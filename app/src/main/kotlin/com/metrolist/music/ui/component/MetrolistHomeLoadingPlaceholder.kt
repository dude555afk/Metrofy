/**
 * Android adaptation of Metrolist shared UI HomeLoadingPlaceholder.
 * Preserves Metrofy's grid-size preference and existing shimmer implementation.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder

private val ChipWidths = listOf(72.dp, 96.dp, 64.dp, 88.dp, 76.dp, 104.dp)

@Composable
fun MetrolistHomeLoadingPlaceholder(
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    includeChipShelf: Boolean = false,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Loading home content"
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth > 840.dp
        val horizontalPadding = if (isWide) 24.dp else 16.dp
        val tile = currentGridThumbnailHeight()
        val availableWidth = (maxWidth - horizontalPadding * 2).coerceAtLeast(0.dp)
        val speedDialColumns = ((availableWidth / (tile + 8.dp)).toInt()).coerceIn(1, 6)
        val speedDialRows = if (isLandscape) 2 else 3
        val carouselCount = ((maxWidth - horizontalPadding) / (tile + 12.dp))
            .toInt().coerceAtLeast(2) + 1
        val cardShape = RoundedCornerShape(12.dp)

        ShimmerHost(modifier = Modifier, showGradient = false) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isWide) 24.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isWide) 16.dp else 8.dp),
            ) {
                if (includeChipShelf) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(start = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChipWidths.forEach { width ->
                            SkeletonBlock(Modifier.width(width).height(32.dp), CircleShape)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                ) {
                    ShelfTitlePlaceholder(isWide)
                    repeat(speedDialRows) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (row == 0) 0.dp else 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            repeat(speedDialColumns) {
                                SkeletonBlock(
                                    Modifier.weight(1f).aspectRatio(1f),
                                    cardShape,
                                )
                            }
                        }
                    }
                    if (!isLandscape) Spacer(Modifier.height(16.dp))
                }

                repeat(3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = horizontalPadding),
                    ) {
                        ShelfTitlePlaceholder(isWide)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isWide) 12.dp else 8.dp),
                        ) {
                            repeat(carouselCount) {
                                CarouselItemPlaceholder(tile, cardShape)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfTitlePlaceholder(isWide: Boolean) {
    Column(
        modifier = Modifier.height(if (isWide) 60.dp else 50.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        SkeletonBlock(Modifier.width(184.dp).height(22.dp), RoundedCornerShape(8.dp))
    }
}

@Composable
private fun CarouselItemPlaceholder(
    size: Dp,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Column(
        modifier = Modifier.width(size),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(1f), shape)
        Spacer(Modifier.height(2.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.85f).height(14.dp), CircleShape)
        SkeletonBlock(Modifier.fillMaxWidth(0.55f).height(12.dp), CircleShape)
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Spacer(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
