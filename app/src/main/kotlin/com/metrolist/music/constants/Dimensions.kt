/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val CONTENT_TYPE_HEADER = 0
const val CONTENT_TYPE_LIST = 1
const val CONTENT_TYPE_SONG = 2
const val CONTENT_TYPE_ARTIST = 3
const val CONTENT_TYPE_ALBUM = 4
const val CONTENT_TYPE_PLAYLIST = 5

val NavigationBarHeight = 80.dp
val SlimNavBarHeight = 64.dp
val MiniPlayerHeight = 64.dp
val MinMiniPlayerHeight = 16.dp
val MiniPlayerBottomSpacing = 8.dp // Space between MiniPlayer and NavigationBar
val QueuePeekHeight = 64.dp
val AppBarHeight = 64.dp

val ListItemHeight = 64.dp
val SuggestionItemHeight = 56.dp
val SearchFilterHeight = 48.dp
val ListThumbnailSize = 48.dp
val SmallGridThumbnailHeight = 104.dp
val GridThumbnailHeight = 128.dp
val SpeedDialTargetItemSize = 160.dp

data class SpeedDialLayout(
    val columns: Int,
    val rows: Int,
) {
    val itemsPerPage: Int get() = columns * rows
}

/**
 * Port of Metrolist's responsive speedDialLayout.
 * Keep the same minimum column count and thumbnail-size scaling so BIG/SMALL
 * home-grid preferences produce the same page density as the upstream screen.
 */
fun speedDialLayout(
    availableWidth: Dp,
    size: GridItemSize,
): SpeedDialLayout {
    val thumbnailHeight = if (size == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val targetItemSize = SpeedDialTargetItemSize * (thumbnailHeight / GridThumbnailHeight)
    val minColumns = if (size == GridItemSize.BIG) 3 else 4
    val columns = (availableWidth / targetItemSize).toInt().coerceAtLeast(minColumns)
    val rows = when {
        columns >= 6 -> 1
        columns >= 4 -> 2
        else -> 3
    }
    return SpeedDialLayout(columns, rows)
}
val AlbumThumbnailSize = 144.dp

val ThumbnailCornerRadius = 3.dp

val PlayerHorizontalPadding = 32.dp

val NavigationBarAnimationSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

val BottomSheetAnimationSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val BottomSheetSoftAnimationSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)
