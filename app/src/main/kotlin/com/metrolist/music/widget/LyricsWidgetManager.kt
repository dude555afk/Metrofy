/**
 * Feeds Metrofy's lyrics database and Media3 clock into Pear Music's original widget
 * state machine and lock-screen metadata mechanism.
 */
package com.metrolist.music.widget

import android.content.Context
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.LockScreenLyrics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class LyricsWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val lyricsHelper: LyricsHelper,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var loadedMediaId: String? = null
    private var entries: List<LyricsEntry> = emptyList()
    private var plainLines: List<String> = emptyList()
    private var hasSyncedLyrics = false
    private var loadJob: Job? = null
    private var latest: Snapshot? = null

    private data class Snapshot(
        val metadata: MediaMetadata,
        val title: String?,
        val artist: String?,
        val artworkUri: String?,
        val positionMs: Long,
        val isPlaying: Boolean,
        val showLockScreenLyrics: Boolean,
    )

    fun update(
        mediaMetadata: MediaMetadata?,
        title: String?,
        artist: String?,
        artworkUri: String?,
        positionMs: Long,
        isPlaying: Boolean,
        showLockScreenLyrics: Boolean,
    ) {
        val widgetInstalled = LyricsWidgetState.installed(context)
        if (mediaMetadata == null) {
            if (widgetInstalled) LyricsWidgetState.update(
                context, null, null, null, null, "", "Play a song to see its lyrics here",
                "", null, false,
            )
            LockScreenLyrics.publish(null)
            return
        }
        if (!widgetInstalled && !showLockScreenLyrics) {
            LockScreenLyrics.publish(null)
            return
        }

        val snapshot = Snapshot(
            mediaMetadata, title, artist, artworkUri,
            positionMs.coerceAtLeast(0L), isPlaying, showLockScreenLyrics,
        )
        latest = snapshot
        if (loadedMediaId != mediaMetadata.id) {
            loadedMediaId = mediaMetadata.id
            entries = emptyList()
            plainLines = emptyList()
            hasSyncedLyrics = false
            loadJob?.cancel()
            loadJob = scope.launch {
                val rawLyrics = withContext(Dispatchers.IO) {
                    val cached = database.lyrics(mediaMetadata.id).first()?.lyrics
                    cached?.takeIf { it.isNotBlank() } ?: run {
                        val fetched = lyricsHelper.getLyrics(mediaMetadata)
                        database.query {
                            upsert(
                                LyricsEntity(
                                    id = mediaMetadata.id,
                                    lyrics = fetched.lyrics,
                                    provider = fetched.provider,
                                ),
                            )
                        }
                        fetched.lyrics
                    }
                }
                if (loadedMediaId != mediaMetadata.id) return@launch
                hasSyncedLyrics = rawLyrics.trimStart().startsWith("[")
                plainLines = rawLyrics.lineSequence().map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("[") && !it.startsWith("{") }
                    .toList()
                entries = withContext(Dispatchers.Default) {
                    runCatching { LyricsUtils.parseLyrics(rawLyrics) }.getOrDefault(emptyList())
                }
                latest?.takeIf { it.metadata.id == mediaMetadata.id }?.let(::render)
            }
        }
        render(snapshot)
    }

    private fun render(snapshot: Snapshot) {
        val widgetInstalled = LyricsWidgetState.installed(context)
        val lines = entries
        if (!hasSyncedLyrics || lines.isEmpty()) {
            if (widgetInstalled) LyricsWidgetState.update(
                context, snapshot.title, snapshot.artist, snapshot.artworkUri,
                snapshot.metadata.id, "", plainLines.getOrNull(0) ?: "Synced lyrics unavailable",
                plainLines.getOrNull(1).orEmpty(), null, snapshot.isPlaying,
            )
            LockScreenLyrics.publish(null)
            return
        }

        val position = snapshot.positionMs
        val currentIndex = lines.indexOfLast { it.time <= position && !it.isBackground }
        val current = lines.getOrNull(currentIndex)
        if (current == null) {
            if (widgetInstalled) LyricsWidgetState.update(
                context, snapshot.title, snapshot.artist, snapshot.artworkUri,
                snapshot.metadata.id to -1, "", "♪",
                lines.firstOrNull { !it.isBackground }?.text.orEmpty(), 0f, snapshot.isPlaying,
            )
            LockScreenLyrics.publish(null)
            return
        }

        val previous = lines.subList(0, currentIndex)
            .lastOrNull { !it.isBackground }?.text.orEmpty()
        val next = lines.drop(currentIndex + 1)
            .firstOrNull { !it.isBackground }?.text.orEmpty()
        val progress = current.words?.takeIf { it.isNotEmpty() }?.let { words ->
            val sung = words.count { position >= (it.endTime * 1000.0).toLong() }
            sung.toFloat() / words.size
        } ?: run {
            val nextTime = lines.drop(currentIndex + 1)
                .firstOrNull { !it.isBackground }?.time
            val end = nextTime ?: (current.time + 5_000L)
            val duration = (end - current.time).coerceIn(500L, 8_000L)
            ((position - current.time).toFloat() / (duration * 0.85f)).coerceIn(0f, 1f)
        }

        if (widgetInstalled) LyricsWidgetState.update(
            context, snapshot.title, snapshot.artist, snapshot.artworkUri,
            snapshot.metadata.id to currentIndex, previous,
            current.text.ifBlank { "♪" }, next, progress, snapshot.isPlaying,
        )
        LockScreenLyrics.publish(
            if (snapshot.showLockScreenLyrics && current.text.isNotBlank()) "♪ ${current.text}" else null,
        )
    }
}
