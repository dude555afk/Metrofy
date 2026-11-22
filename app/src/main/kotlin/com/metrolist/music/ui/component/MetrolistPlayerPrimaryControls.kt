/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Android/Media3 adapter for Metrolist's expressive PlayerPrimaryControls.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MetrolistPlayerPrimaryControls(
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    centerIcon: Int,
    centerLabel: String,
    onPrevious: () -> Unit,
    onCenterClick: () -> Unit,
    onNext: () -> Unit,
    sideContainerColor: androidx.compose.ui.graphics.Color,
    sideContentColor: androidx.compose.ui.graphics.Color,
    centerContainerColor: androidx.compose.ui.graphics.Color,
    centerContentColor: androidx.compose.ui.graphics.Color,
    isPlaying: Boolean,
    useRoundedRect: Boolean,
    modifier: Modifier = Modifier,
    buttonHeight: androidx.compose.ui.unit.Dp = 68.dp,
) {
    val previousInteraction = remember { MutableInteractionSource() }
    val centerInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }

    val previousPressed by previousInteraction.collectIsPressedAsState()
    val centerPressed by centerInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()

    val compact = buttonHeight < 52.dp
    val centerWeight by animateFloatAsState(
        targetValue =
            when {
                centerPressed -> 2.2f
                previousPressed || nextPressed -> 1.3f
                else -> 1.9f
            },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "metrolistPlayPauseWeight",
    )
    val previousWeight by animateFloatAsState(
        targetValue =
            when {
                previousPressed -> 0.9f
                centerPressed -> 0.4f
                nextPressed -> 0.5f
                else -> 0.6f
            },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "metrolistPreviousWeight",
    )
    val nextWeight by animateFloatAsState(
        targetValue =
            when {
                nextPressed -> 0.9f
                centerPressed -> 0.4f
                previousPressed -> 0.5f
                else -> 0.6f
            },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "metrolistNextWeight",
    )

    val sideShape = RoundedCornerShape(buttonHeight / 2)
    val centerShape =
        if (useRoundedRect) {
            RoundedCornerShape(16.dp)
        } else if (isPlaying) {
            RoundedCornerShape(20.dp)
        } else {
            RoundedCornerShape(48.dp)
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onPrevious,
            enabled = previousEnabled,
            interactionSource = previousInteraction,
            shapes = ButtonDefaults.shapes(shape = sideShape),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = sideContainerColor.copy(alpha = 0.8f),
                    contentColor = sideContentColor,
                ),
            contentPadding = PaddingValues(0.dp),
            modifier =
                Modifier
                    .height(buttonHeight)
                    .weight(previousWeight),
        ) {
            Icon(
                painter = painterResource(com.metrolist.music.R.drawable.skip_previous),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 20.dp else 28.dp),
            )
        }

        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))

        FilledTonalButton(
            onClick = onCenterClick,
            interactionSource = centerInteraction,
            shapes = ButtonDefaults.shapes(shape = centerShape),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = centerContainerColor,
                    contentColor = centerContentColor,
                ),
            contentPadding = PaddingValues(horizontal = if (compact) 4.dp else 12.dp, vertical = 0.dp),
            modifier =
                Modifier
                    .height(buttonHeight)
                    .weight(centerWeight),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 0.dp else 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(centerIcon),
                    contentDescription = centerLabel,
                    modifier = Modifier.size(if (compact) 20.dp else 28.dp),
                )
                Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
                Text(
                    text = centerLabel,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))

        FilledTonalButton(
            onClick = onNext,
            enabled = nextEnabled,
            interactionSource = nextInteraction,
            shapes = ButtonDefaults.shapes(shape = sideShape),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = sideContainerColor.copy(alpha = 0.85f),
                    contentColor = sideContentColor,
                ),
            contentPadding = PaddingValues(0.dp),
            modifier =
                Modifier
                    .height(buttonHeight)
                    .weight(nextWeight),
        ) {
            Icon(
                painter = painterResource(com.metrolist.music.R.drawable.skip_next),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 20.dp else 28.dp),
            )
        }
    }
}
