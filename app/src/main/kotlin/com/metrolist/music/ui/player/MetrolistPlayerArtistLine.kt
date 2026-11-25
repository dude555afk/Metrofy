/**
 * Android adapter of Metrolist's shared PlayerArtistLine.
 * Artist links still navigate through Metrofy's NavController; long-press keeps
 * Metrofy's copy-all-artists affordance.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Layout
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class PlayerArtistMarqueeCycle(
    val distance: Int,
    val durationMillis: Int,
)

private fun playerArtistMarqueeCycle(
    contentWidth: Int,
    containerWidth: Int,
    pixelsPerSecond: Float,
): PlayerArtistMarqueeCycle? {
    if (contentWidth <= containerWidth || pixelsPerSecond <= 0f) return null
    val distance = contentWidth + (containerWidth / 3f).roundToInt()
    return PlayerArtistMarqueeCycle(
        distance = distance,
        durationMillis = ceil(distance / pixelsPerSecond * 1000f).toInt().coerceAtLeast(1),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetrolistPlayerArtistLine(
    artists: List<MediaMetadata.Artist>,
    onNavigateToArtist: (String) -> Unit,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    iterations: Int = 1,
    initialDelayMillis: Int = 3000,
    pixelsPerSecond: Dp = 30.dp,
) {
    if (artists.isEmpty()) return

    var contentWidth = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var containerWidth = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val offset = remember { Animatable(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val speed = with(density) { pixelsPerSecond.toPx() }

    LaunchedEffect(contentWidth.intValue, containerWidth.intValue, iterations, initialDelayMillis, speed) {
        offset.snapTo(0f)
        val cycle = playerArtistMarqueeCycle(contentWidth.intValue, containerWidth.intValue, speed) ?: return@LaunchedEffect
        if (iterations <= 0) return@LaunchedEffect
        repeat(iterations) { index ->
            delay(if (index == 0) initialDelayMillis.toLong() else 1200L)
            offset.animateTo(
                targetValue = cycle.distance.toFloat(),
                animationSpec = tween(durationMillis = cycle.durationMillis, easing = LinearEasing),
            )
            offset.snapTo(0f)
        }
    }

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            repeat(2) { copy ->
                Row(
                    modifier = if (copy == 0) Modifier else Modifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    artists.forEachIndexed { index, artist ->
                        if (index > 0) {
                            Text(text = ", ", style = style, color = color, maxLines = 1, softWrap = false)
                        }
                        Text(
                            text = artist.name,
                            style = style,
                            color = color,
                            maxLines = 1,
                            softWrap = false,
                            modifier = if (artist.id.isNullOrBlank()) {
                                Modifier
                            } else {
                                Modifier.combinedClickable(
                                    onClick = { onNavigateToArtist(artist.id) },
                                    onLongClick = onLongClick,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val copies = measurables.map { it.measure(constraints.copy(maxWidth = Constraints.Infinity)) }
        contentWidth.intValue = copies.first().width
        containerWidth.intValue = constraints.maxWidth
        val cycle = playerArtistMarqueeCycle(contentWidth.intValue, containerWidth.intValue, speed)
        val distance = cycle?.distance ?: contentWidth.intValue
        layout(constraints.constrainWidth(contentWidth.intValue), copies.first().height) {
            val translation = -offset.value.roundToInt()
            copies[0].placeWithLayer(translation, 0)
            if (cycle != null) {
                copies[1].placeWithLayer(distance + translation, 0)
            }
        }
    }
}
