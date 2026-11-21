/**
 * Navigation adapted from Metrolist's shared FloatingNavigationBar.
 * Preserves Metrofy destinations, long-press Search recognition, rail navigation and nav preferences.
 */
package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
private val FloatingBarShadow = 6.dp
private val NavSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 700f)

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } && currentRoute.startsWith("$screenRoute/")) return true
    if (screenRoute == "search_input" && (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true
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
    androidx.compose.material3.NavigationRail(modifier = modifier, containerColor = containerColor) {
        Spacer(Modifier.weight(1f))
        navigationItems.forEach { screen ->
            val selected = remember(currentRoute, screen.route) { isRouteSelected(currentRoute, screen.route, navigationItems) }
            val currentSelected by rememberUpdatedState(selected)
            val iconRes = if (selected) screen.iconIdActive else screen.iconIdInactive
            val isSearch = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }
            val currentLongClick by rememberUpdatedState(onSearchLongClick)
            if (isSearch) {
                LaunchedEffect(interactionSource) {
                    var longPressed = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                longPressed = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                longPressed = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentLongClick?.invoke()
                            }
                            is PressInteraction.Release -> if (!longPressed) onItemClick(screen, currentSelected)
                            is PressInteraction.Cancel -> longPressed = false
                        }
                    }
                }
            }
            androidx.compose.material3.NavigationRailItem(
                selected = selected,
                onClick = { if (!isSearch) onItemClick(screen, currentSelected) },
                interactionSource = interactionSource,
                icon = { Icon(painterResource(iconRes), contentDescription = stringResource(screen.titleId)) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val baseContainer = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val selectedSearch = searchItem?.let { isRouteSelected(currentRoute, it.route, navigationItems) } ?: false
    val searchInteraction = remember { MutableInteractionSource() }
    val currentSearchLongClick by rememberUpdatedState(onSearchLongClick)
    val currentSearchSelected by rememberUpdatedState(selectedSearch)

    if (searchItem != null && onSearchLongClick != null) {
        LaunchedEffect(searchInteraction) {
            var longPressed = false
            searchInteraction.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        longPressed = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        longPressed = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentSearchLongClick?.invoke()
                    }
                    is PressInteraction.Release -> if (!longPressed) onItemClick(searchItem, currentSearchSelected)
                    is PressInteraction.Cancel -> longPressed = false
                }
            }
        }
    }

    val barHeight = if (slimNav) 48.dp else FloatingBarHeight
    val itemHeight = if (slimNav) 40.dp else FloatingItemHeight

    AnimatedVisibility(
        visible = true,
        modifier = modifier.fillMaxWidth(),
        enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = 700f), expandFrom = Alignment.Bottom),
        exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 700f), shrinkTowards = Alignment.Bottom),
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.wrapContentWidth().height(barHeight),
                    shape = CircleShape,
                    color = baseContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = FloatingBarShadow,
                ) {
                    Row(
                        Modifier.wrapContentWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        mainItems.forEach { screen ->
                            val selected = remember(currentRoute, screen.route) { isRouteSelected(currentRoute, screen.route, navigationItems) }
                            val currentSelected by rememberUpdatedState(selected)
                            val indicator by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                animationSpec = NavSpring,
                                label = "floatingItemIndicator",
                            )
                            val content by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = NavSpring,
                                label = "floatingItemContent",
                            )
                            Row(
                                Modifier.height(itemHeight).clip(CircleShape).background(indicator)
                                    .selectable(
                                        selected = selected,
                                        onClick = { onItemClick(screen, currentSelected) },
                                        role = Role.Tab,
                                        interactionSource = remember { MutableInteractionSource() },
                                    )
                                    .padding(horizontal = if (selected && !slimNav) 16.dp else 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                                    contentDescription = if (slimNav) stringResource(screen.titleId) else null,
                                    tint = content,
                                )
                                if (!slimNav) {
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn() + expandHorizontally(NavSpring),
                                        exit = fadeOut() + shrinkHorizontally(NavSpring),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(screen.titleId),
                                                color = content,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                searchItem?.let { screen ->
                    val container by animateColorAsState(
                        if (selectedSearch) MaterialTheme.colorScheme.secondaryContainer else baseContainer,
                        animationSpec = NavSpring,
                        label = "floatingSearchContainer",
                    )
                    val content by animateColorAsState(
                        if (selectedSearch) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                        animationSpec = NavSpring,
                        label = "floatingSearchContent",
                    )
                    Surface(
                        modifier = Modifier.size(barHeight),
                        shape = CircleShape,
                        color = container,
                        contentColor = content,
                        shadowElevation = FloatingBarShadow,
                    ) {
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape).selectable(
                                selected = selectedSearch,
                                onClick = { if (onSearchLongClick == null) onItemClick(screen, selectedSearch) },
                                role = Role.Tab,
                                interactionSource = searchInteraction,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(if (selectedSearch) screen.iconIdActive else screen.iconIdInactive),
                                contentDescription = stringResource(screen.titleId),
                            )
                        }
                    }
                }
            }
        }
    }
}
