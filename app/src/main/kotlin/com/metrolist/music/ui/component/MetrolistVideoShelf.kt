/**
 * Android adapter of Metrolist shared UI components.VideoShelf.
 * Its item renderer is supplied by Metrofy so click, menu, offline and playback logic stay local.
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
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.YTItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetrolistVideoShelf(
    items: List<YTItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    itemContent: @Composable (YTItem) -> Unit,
) {
    if (items.isEmpty()) return

    val itemSize = currentGridThumbnailHeight() + 24.dp
    val carouselState = rememberCarouselState { items.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = itemSize,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .clip(RoundedCornerShape(24.dp)),
    ) { index ->
        itemContent(items[index])
    }
}
