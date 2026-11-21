/**
 * Ported from Metrolist's shared UI theme.
 * Adapted to Metrofy's preference and constant system.
 */
package com.metrolist.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MetrolistShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object ComponentShapes {
    val CardShape = RoundedCornerShape(20.dp)
    val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val SmallThumbnailShape = RoundedCornerShape(8.dp)
    val ThumbnailShape = RoundedCornerShape(12.dp)
    val AlbumArtShape = RoundedCornerShape(32.dp)
    val MenuExteriorShape = RoundedCornerShape(28.dp)
    val MenuGroupShape = RoundedCornerShape(20.dp)
    val MenuItemShape = RoundedCornerShape(8.dp)
    val SearchExteriorCornerRadius = 24.dp
    val SearchItemCornerRadius = 6.dp
}
