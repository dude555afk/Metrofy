package com.metrolist.spotify.models

data class SpotifyLyrics(
    val synced: Boolean,
    val lines: List<SpotifyLyricLine>,
)

data class SpotifyLyricLine(
    val startTimeMs: Long,
    val words: String,
)
