/**
 * Android adaptation of Metrolist's current Material3SettingsGroup.
 * Keeps Metrofy's Painter-based item model and custom settings destinations.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp, start = 16.dp, end = 32.dp),
            )
        }

        AnimatedContent(
            targetState = items,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "settingsItemsTransition",
        ) { currentItems ->
            if (currentItems.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    currentItems.forEachIndexed { index, item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
                        ) {
                            Material3SettingsItemRow(
                                item = item,
                                isFirst = index == 0,
                                isLast = index == currentItems.lastIndex,
                                useLowContrast = useLowContrast,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem,
    isFirst: Boolean,
    isLast: Boolean,
    useLowContrast: Boolean,
    horizontalPadding: Dp = 16.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) bringIntoViewRequester.bringIntoView()
    }

    val shape = settingsSegmentShape(isFirst, isLast, isPressed)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            useLowContrast -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "settingsRowBackground",
    )
    val outlineColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = horizontalPadding)
            .clip(shape)
            .background(backgroundColor)
            .border(if (isFocused) 2.dp else 0.dp, outlineColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() },
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(Modifier.width(16.dp))
        } else if (item.icon != null) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showBadge) {
                    BadgedBox(
                        badge = { Badge(containerColor = MaterialTheme.colorScheme.error) },
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = settingsItemIconTint(item),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = settingsItemIconTint(item),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(
                    color = if (item.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
            ) {
                item.title()
            }

            item.description?.let { description ->
                Spacer(Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (item.enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                ) {
                    description()
                }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun settingsItemIconTint(item: Material3SettingsItem): Color =
    when {
        !item.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        item.isHighlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun settingsSegmentShape(
    isFirst: Boolean,
    isLast: Boolean,
    isPressed: Boolean,
): Shape {
    val outer = if (isPressed) 18.dp else 24.dp
    val inner = if (isPressed) 12.dp else 6.dp
    return RoundedCornerShape(
        topStart = if (isFirst) outer else inner,
        topEnd = if (isFirst) outer else inner,
        bottomStart = if (isLast) outer else inner,
        bottomEnd = if (isLast) outer else inner,
    )
}

/** Metrofy settings item model retained for all existing settings pages. */
data class Material3SettingsItem(
    val icon: androidx.compose.ui.graphics.painter.Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
)
