/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.viewmodels.LyricsViewModel

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    // Always use the word-timing-aware renderer. Older installs may have a saved
    // ExperimentalLyricsKey=false value, so changing a default is not sufficient.
    // ExperimentalLyrics still falls back to ordinary line highlighting when the
    // selected provider supplies only line timestamps.
    ExperimentalLyrics(
        sliderPositionProvider = sliderPositionProvider,
        modifier = modifier,
        showLyrics = showLyrics,
        lyricsViewModel = lyricsViewModel
    )
}
