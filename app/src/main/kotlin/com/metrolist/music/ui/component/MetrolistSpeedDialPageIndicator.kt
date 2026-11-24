/**
 * Android port of Metrolist's SpeedDialPageIndicator.
 * The active worm follows pager drag progress instead of snapping between static dots.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.PagerState

@Composable
fun MetrolistSpeedDialPageIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val pageCount = pagerState.pageCount
    if (pageCount <= 1) return

    Row(
        modifier = modifier.height(22.dp).fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val inactiveColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        val activeColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
        Canvas(Modifier.size(width = 12.dp * pageCount - 6.dp, height = 6.dp)) {
            val dot = size.height
            val pitch = dot * 2
            val radius = CornerRadius(dot / 2)
            repeat(pageCount) { index ->
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(index * pitch, 0f),
                    size = Size(dot, dot),
                    cornerRadius = radius,
                )
            }
            val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (pageCount - 1).toFloat())
            val page = kotlin.math.floor(position)
            val fraction = position - page
            val left = (page + (fraction * 2 - 1).coerceAtLeast(0f)) * pitch
            val right = (page + (fraction * 2).coerceAtMost(1f)) * pitch + dot
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(left, 0f),
                size = Size(right - left, dot),
                cornerRadius = radius,
            )
        }
    }
}
