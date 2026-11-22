/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Android-compatible port of Metrolist's expressive Material3SettingsGroup.
 * Keeps Metrofy's existing Painter-backed settings item API.
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A grouped settings surface using Metrolist's segmented-row treatment.
 * The original Metrofy item model and all trailing/leading content slots remain supported.
 */
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
                modifier = Modifier.padding(start = 16.dp, end = 32.dp, top = 8.dp, bottom = 12.dp),
            )
        }

        AnimatedContent(
            targetState = items,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
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
                            enter = fadeIn(tween(240)) + expandVertically(tween(240)),
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

@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem,
    isFirst: Boolean,
    isLast: Boolean,
    useLowContrast: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val shape =
        when {
            isPressed -> RoundedCornerShape(16.dp)
            isFirst && isLast -> RoundedCornerShape(24.dp)
            isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
            isLast -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            else -> RoundedCornerShape(6.dp)
        }

    val targetContainer =
        when {
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHigh
            useLowContrast -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(140),
        label = "settingsRowContainer",
    )

    LaunchedEffect(isFocused) {
        if (isFocused) bringIntoViewRequester.bringIntoView()
    }

    Row(
        modifier =
            item.modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .clip(shape)
                .background(containerColor)
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = shape,
                )
                .then(
                    if (item.checked != null) {
                        Modifier.toggleable(
                            value = item.checked,
                            enabled = item.enabled && item.onClick != null,
                            role = Role.Switch,
                            interactionSource = interactionSource,
                            indication = null,
                            onValueChange = { item.onClick?.invoke() },
                        )
                    } else {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = item.enabled && item.onClick != null,
                            role = Role.Button,
                            onClickLabel = item.onClickLabel,
                            onClick = { item.onClick?.invoke() },
                        )
                    },
                )
                .semantics {
                    item.badgeDescription?.takeIf { item.showBadge }?.let {
                        stateDescription = it
                    }
                }
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
                val iconTint =
                    when {
                        !item.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        item.iconTint != null -> item.iconTint
                        item.isHighlighted -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                val iconContent: @Composable () -> Unit = {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }

                if (item.showBadge) {
                    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                        iconContent()
                    }
                } else {
                    iconContent()
                }
            }
            Spacer(Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(
                    color =
                        if (item.enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
            ) {
                item.title()
            }

            item.description?.let { description ->
                Spacer(Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                            if (item.enabled) MaterialTheme.colorScheme.onSurfaceVariant
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

data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val badgeDescription: String? = null,
    val iconTint: Color? = null,
    val searchTitle: String? = null,
    val searchDescription: String? = null,
    val modifier: Modifier = Modifier,
    val checked: Boolean? = null,
    val onClickLabel: String? = null,
)
