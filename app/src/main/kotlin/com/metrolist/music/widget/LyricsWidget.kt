/**
 * Pear Music lyrics widget, adapted to Metrofy's existing MusicService and lyrics cache.
 * The widget itself retains Pear's two-page lyric animation and artwork treatment.
 */
package com.metrolist.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.toBitmap
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.ceil

class LyricsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        LyricsWidgetState.invalidateInstalledCache()
        LyricsWidgetState.push(context)
        if (MusicService.isRunning) {
            runCatching {
                context.startService(Intent(context, MusicService::class.java).setAction(ACTION_UPDATE))
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_PLAY_PAUSE) {
            runCatching {
                context.startService(Intent(context, MusicService::class.java).setAction(ACTION_PLAY_PAUSE))
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        LyricsWidgetState.invalidateInstalledCache()
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.metrolist.music.widget.LYRICS_PLAY_PAUSE"
        const val ACTION_UPDATE = "com.metrolist.music.widget.LYRICS_UPDATE"
    }
}

object LyricsWidgetState {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile private var installedCache: Boolean? = null
    private var checkedAt = 0L

    private var songId: String? = null
    private var title: String? = null
    private var artist: String? = null
    private var playing = false
    private var artUrl: String? = null
    private var art: Bitmap? = null
    private var backdrop: Bitmap? = null

    private var lineKey: Any? = null
    private var previous = ""
    private var current = "Play a song to see its lyrics here"
    private var next = ""
    private var sungWords = -1
    private var page = 0
    private var needsFull = true
    private var lastWordPush = 0L

    fun invalidateInstalledCache() {
        installedCache = null
        checkedAt = 0L
    }

    fun installed(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val cached = installedCache
        if (cached != null && now - checkedAt < 30_000L) return cached
        checkedAt = now
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
        return ids.isNotEmpty().also { installedCache = it }
    }

    fun update(
        context: Context,
        songId: String?,
        title: String?,
        artist: String?,
        artUrl: String?,
        lineKey: Any?,
        previous: String,
        current: String,
        next: String,
        progress: Float?,
        sungWordsOverride: Int?,
        playing: Boolean,
    ) {
        val app = context.applicationContext
        val wordCount = WORD_REGEX.findAll(current).count()
        val sung = (sungWordsOverride
            ?: if (progress == null) wordCount
            else ceil(progress * wordCount).toInt()).coerceIn(0, wordCount)

        val newSong = songId != this.songId ||
            title != this.title || artist != this.artist || artUrl != this.artUrl
        val newLine = lineKey != this.lineKey || current != this.current

        this.songId = songId
        this.title = title
        this.artist = artist
        this.playing = playing.also { if (it != this.playing) needsFull = true }

        if (artUrl != this.artUrl) {
            this.artUrl = artUrl
            art = null
            backdrop = null
            if (artUrl != null) {
                scope.launch {
                    val loader = SingletonImageLoader.get(app)
                    val small = loader.execute(
                        ImageRequest.Builder(app)
                            .data(artUrl)
                            .size(160)
                            .allowHardware(false)
                            .build(),
                    )
                    val blurred = loader.execute(
                        ImageRequest.Builder(app)
                            .data(artUrl)
                            .size(256)
                            .allowHardware(false)
                            .transformations(SoftBlurTransformation(width = 48, radius = 5))
                            .build(),
                    )
                    if (this@LyricsWidgetState.artUrl == artUrl) {
                        art = (small as? SuccessResult)?.image?.toBitmap()
                        backdrop = (blurred as? SuccessResult)?.image?.toBitmap()
                        push(app)
                    }
                }
            }
        }

        when {
            newSong || needsFull -> {
                this.lineKey = lineKey
                this.previous = previous
                this.current = current
                this.next = next
                sungWords = sung
                needsFull = false
                push(app)
            }
            newLine -> {
                this.lineKey = lineKey
                this.previous = previous
                this.current = current
                this.next = next
                sungWords = sung
                page = 1 - page
                lastWordPush = System.currentTimeMillis() + 300L
                partial(app) { views ->
                    fillPage(views, page)
                    views.setDisplayedChild(R.id.lw_flipper, page)
                }
            }
            sung != sungWords && System.currentTimeMillis() - lastWordPush > 450L -> {
                lastWordPush = System.currentTimeMillis()
                sungWords = sung
                push(app)
            }
        }
    }

    private fun lineId(page: Int) = if (page == 0) R.id.lw_line_a else R.id.lw_line_b

    private fun fillPage(views: RemoteViews, page: Int) {
        views.setTextViewText(if (page == 0) R.id.lw_prev_a else R.id.lw_prev_b, previous)
        views.setTextViewText(lineId(page), karaoke(current, sungWords))
        views.setTextViewText(if (page == 0) R.id.lw_next_a else R.id.lw_next_b, next)
    }

    private fun karaoke(text: String, sung: Int): CharSequence {
        if (sung <= 0 && text.isNotEmpty()) {
            return android.text.SpannableString(text).apply {
                setSpan(android.text.style.ForegroundColorSpan(DIM), 0, text.length, 0)
            }
        }
        var seen = 0
        var cut = text.length
        for (match in WORD_REGEX.findAll(text)) {
            seen++
            if (seen == sung) {
                cut = match.range.last + 1
                break
            }
        }
        return android.text.SpannableString(text).apply {
            if (cut < text.length) {
                setSpan(android.text.style.ForegroundColorSpan(DIM), cut, text.length, 0)
            }
        }
    }

    private fun partial(context: Context, build: (RemoteViews) -> Unit) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_lyrics)
        build(views)
        manager.partiallyUpdateAppWidget(ids, views)
    }

    fun push(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_lyrics).apply {
            setTextViewText(R.id.lw_title, title ?: "Not Playing")
            setTextViewText(R.id.lw_artist, artist ?: "Metrofy")
            fillPage(this, 0)
            fillPage(this, 1)
            art?.let { setImageViewBitmap(R.id.lw_art, it) }
                ?: setImageViewResource(R.id.lw_art, android.R.color.transparent)
            backdrop?.let { setImageViewBitmap(R.id.lw_backdrop, it) }
                ?: setImageViewResource(R.id.lw_backdrop, android.R.color.transparent)
            setImageViewResource(R.id.lw_play, if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)
            setOnClickPendingIntent(
                R.id.lw_play,
                PendingIntent.getBroadcast(
                    context,
                    21,
                    Intent(context, LyricsWidget::class.java).setAction(LyricsWidget.ACTION_PLAY_PAUSE),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            setOnClickPendingIntent(
                R.id.lw_root,
                PendingIntent.getActivity(
                    context,
                    22,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        manager.updateAppWidget(ids, views)
    }

    private const val DIM = 0x73FFFFFF
    private val WORD_REGEX = Regex("""\S+""")
}
