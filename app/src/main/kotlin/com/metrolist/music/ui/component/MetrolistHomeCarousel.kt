/**
 * Android source port of Metrolist HomeContent.HomeCarouselShelf.
 *
 * The carousel owns horizontal browsing and clipping; Metrofy provides the
 * item renderer so its local database, playback and menu actions remain intact.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MetrolistHomeCarousel(
    items: List<T>,
    preferredItemWidth: Dp,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    itemSpacing: Dp = 12.dp,
    content: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    if (items.isEmpty()) return

    val carouselState = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = preferredItemWidth,
        itemSpacing = itemSpacing,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .clip(RoundedCornerShape(24.dp)),
    ) { index ->
        content(items[index], Modifier.fillMaxWidth())
    }
}
