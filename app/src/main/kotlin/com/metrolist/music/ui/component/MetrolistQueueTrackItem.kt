/**
 * Android adapter of Metrolist QueueScreen.TrackItem, backed by Metrofy's
 * MediaMetadata and existing thumbnail/download components.
 *
 * Metrofy continues to own queue actions, selection, cast, menus, and ordering.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.makeTimeString

@Composable
fun MetrolistQueueTrackItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showSelectionCheckbox: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val motionScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    val motionEnabled = motionScale > 0f
    val highlightProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = if (motionEnabled) 320 else 0),
        label = "queueTrackShape",
    )
    val shape = RoundedCornerShape(12.dp + 38.dp * highlightProgress)
    val artworkCorner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isActive) 22.dp else 6.dp,
        animationSpec = tween(durationMillis = if (motionEnabled) 420 else 0),
        label = "queueArtworkShape",
    )
    val artworkShape = RoundedCornerShape(
        topStart = artworkCorner,
        topEnd = 6.dp,
        bottomEnd = artworkCorner,
        bottomStart = 6.dp,
    )
    val artworkRotationTransition = rememberInfiniteTransition(label = "queueArtworkRotation")
    val artworkRotation by artworkRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive && isPlaying && motionEnabled) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (14_000 / motionScale.coerceAtLeast(0.1f)).toInt().coerceAtLeast(1),
            ),
        ),
        label = "queueArtworkRotation",
    )

    val targetContainer = when {
        isSelected -> colorScheme.primaryContainer
        isActive -> colorScheme.primaryContainer
        else -> colorScheme.surfaceContainer
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(durationMillis = 220),
        label = "queueTrackContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive || isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        animationSpec = tween(durationMillis = 220),
        label = "queueTrackContent",
    )
    val subtitleColor by animateColorAsState(
        targetValue = if (isActive || isSelected) colorScheme.onPrimaryContainer else colorScheme.secondary,
        animationSpec = tween(durationMillis = 220),
        label = "queueTrackSubtitle",
    )

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)
    val hasMetadata = mediaMetadata.liked || mediaMetadata.explicit ||
        mediaMetadata.inLibrary != null || download != null
    val subtitle = listOf(
        mediaMetadata.artists.joinToString { it.name },
        makeTimeString(mediaMetadata.duration * 1000L),
    ).filter { it.isNotBlank() }.joinToString(" · ")

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { rotationZ = artworkRotation }
                    .clip(artworkShape)
                    .background(colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                ItemThumbnail(
                    thumbnailUrl = mediaMetadata.thumbnailUrl,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = artworkShape,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        letterSpacing = 0.15.sp,
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        if (mediaMetadata.liked) Icon.Favorite()
                        if (mediaMetadata.explicit) Icon.Explicit()
                        if (mediaMetadata.inLibrary != null) Icon.Library()
                        if (download != null) Icon.Download(download?.state)
                    }
                    if (hasMetadata) Spacer(Modifier.width(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showSelectionCheckbox || isSelected) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                }
            } else {
                trailingContent()
            }
        }
    }
}
