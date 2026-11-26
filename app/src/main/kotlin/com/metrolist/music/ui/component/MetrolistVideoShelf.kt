/**
 * Android adapter of Metrolist shared UI components.VideoShelf.
 *
 * Keeps Metrolist's responsive multi-browse card sizing and horizontal carousel
 * container while delegating each card to Metrofy's own item renderer. That
 * preserves Metrofy's menus, navigation, offline rules and playback queue.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.YTItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetrolistVideoShelf(
    items: List<YTItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    itemContent: @Composable (YTItem, Modifier) -> Unit,
) {
    if (items.isEmpty()) return

    val carouselState = rememberCarouselState { items.size }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // These are the source VideoShelf's Android card widths.
        val preferredItemWidth = if (maxWidth > 840.dp) 320.dp else 280.dp

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = preferredItemWidth,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .clip(RoundedCornerShape(24.dp)),
        ) { index ->
            itemContent(items[index], Modifier.fillMaxWidth())
        }
    }
}
