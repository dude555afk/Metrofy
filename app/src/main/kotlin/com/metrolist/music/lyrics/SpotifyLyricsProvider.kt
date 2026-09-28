/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.constants.EnableSpotifyLyricsKey
import com.metrolist.music.playback.SpotifyTrackMatcher
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.spotify.Spotify
import java.util.Locale

object SpotifyLyricsProvider : LyricsProvider {
    override val name = "Spotify"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableSpotifyLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> =
        runCatching {
            // Synced lyrics must target the same recording. Canvas matching can tolerate
            // music-video length differences, but those differences visibly desync lyrics.
            val spotifyTrack = SpotifyTrackMatcher.resolve(
                id,
                title,
                artist,
                duration,
                maxDurationDifferenceSeconds = 5,
            )
                ?: error("No matching Spotify track was found")
            val lyrics = Spotify.lyrics(spotifyTrack.id).getOrThrow()
            lyrics.lines.joinToString("\n") { line ->
                if (lyrics.synced) "[${formatTimestamp(line.startTimeMs)}]${line.words}" else line.words
            }
        }

    private fun formatTimestamp(milliseconds: Long): String {
        val minutes = milliseconds / 60_000
        val seconds = (milliseconds % 60_000) / 1_000
        val hundredths = (milliseconds % 1_000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}
