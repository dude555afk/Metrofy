/**
 * Metrofy navigation adapted from Metrolist's floating expressive navigation.
 * Keeps Metrofy's destinations, route matching and Search long-press recognition action.
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val FloatingBarHeight = 64.dp
private val FloatingItemHeight = 48.dp

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } && currentRoute.startsWith("$screenRoute/")) return true

    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")
    ) {
        return true
    }

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    androidx.compose.material3.NavigationRail(
        modifier = modifier,
        containerColor = containerColor,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }
            val currentOnSearchLongClick by rememberUpdatedState(onSearchLongClick)

            if (isSearchItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) onItemClick(screen, currentIsSelected)
                            }
                            is PressInteraction.Cancel -> isLongClick = false
                        }
                    }
                }
            }

            androidx.compose.material3.NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem) onItemClick(screen, currentIsSelected)
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId),
                    )
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val searchItem = navigationItems.firstOrNull { it == Screens.Search }
    val mainItems = navigationItems.filterNot { it == Screens.Search }
    val currentOnSearchLongClick by rememberUpdatedState(onSearchLongClick)

    val searchSelected = searchItem?.let {
        remember(currentRoute, it.route) { isRouteSelected(currentRoute, it.route, navigationItems) }
    } ?: false

    val searchInteractionSource = remember { MutableInteractionSource() }
    if (searchItem != null && onSearchLongClick != null) {
        val currentSearchSelected by rememberUpdatedState(searchSelected)
        LaunchedEffect(searchInteractionSource) {
            var isLongClick = false
            searchInteractionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongClick = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnSearchLongClick?.invoke()
                    }
                    is PressInteraction.Release -> {
                        if (!isLongClick) onItemClick(searchItem, currentSearchSelected)
                    }
                    is PressInteraction.Cancel -> isLongClick = false
                }
            }
        }
    }

    val navHeight = if (slimNav) 48.dp else FloatingBarHeight
    val itemHeight = if (slimNav) 40.dp else FloatingItemHeight
    val baseContainer = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.wrapContentWidth().height(navHeight),
                shape = CircleShape,
                color = baseContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    mainItems.forEach { screen ->
                        val selected = remember(currentRoute, screen.route) {
                            isRouteSelected(currentRoute, screen.route, navigationItems)
                        }
                        val currentSelected by rememberUpdatedState(selected)
                        val indicator by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                            label = "navIndicator",
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                            label = "navContent",
                        )
                        val interactionSource = remember { MutableInteractionSource() }

                        Row(
                            modifier = Modifier
                                .height(itemHeight)
                                .clip(CircleShape)
                                .background(indicator)
                                .selectable(
                                    selected = selected,
                                    onClick = { onItemClick(screen, currentSelected) },
                                    role = Role.Tab,
                                    interactionSource = interactionSource
                                )
                                .padding(horizontal = if (selected) 16.dp else 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn() + expandHorizontally(spring(dampingRatio = 0.9f, stiffness = 700f)),
                                exit = fadeOut() + shrinkHorizontally(spring(dampingRatio = 0.9f, stiffness = 700f)),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(screen.iconIdActive),
                                        contentDescription = null,
                                        tint = contentColor,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                            }

                            Text(
                                text = stringResource(screen.titleId),
                                color = contentColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            searchItem?.let { screen ->
                val container by animateColorAsState(
                    targetValue = if (searchSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        baseContainer
                    },
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                    label = "floatingSearchContainer",
                )
                val content by animateColorAsState(
                    targetValue = if (searchSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                    label = "floatingSearchContent",
                )

                Surface(
                    modifier = Modifier.size(navHeight),
                    shape = CircleShape,
                    color = container,
                    contentColor = content,
                    shadowElevation = 6.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .selectable(
                                selected = searchSelected,
                                onClick = {
                                    if (onSearchLongClick == null) {
                                        onItemClick(screen, searchSelected)
                                    }
                                },
                                role = Role.Tab,
                                interactionSource = searchInteractionSource
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (searchSelected) screen.iconIdActive else screen.iconIdInactive,
                            ),
                            contentDescription = stringResource(screen.titleId),
                        )
                    }
                }
            }
        }
    }
}
