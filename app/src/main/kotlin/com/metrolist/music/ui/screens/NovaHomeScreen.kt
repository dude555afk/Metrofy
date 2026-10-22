@file:OptIn(ExperimentalFoundationApi::class)

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.ui.component.NovaFilterTabs
import com.metrolist.music.ui.screens.library.LibraryAlbumsScreen
import com.metrolist.music.ui.screens.library.LibraryArtistsScreen
import com.metrolist.music.ui.screens.library.LibraryPlaylistsScreen
import com.metrolist.music.ui.screens.library.LibrarySongsScreen
import com.metrolist.music.ui.screens.library.local.LocalFilesScreen
import kotlinx.coroutines.launch

private data class NovaHomeTab(
    val id: String,
    val title: String,
)

/**
 * Nova borrows SoundPod's home hierarchy: Home first, a compact pill tab strip,
 * and one rounded content surface. Every page still calls Metrofy's own screens,
 * so this is a presentation-only layer over the existing data and playback stack.
 */
private val novaHomeTabs = listOf(
    NovaHomeTab("home", "Home"),
    NovaHomeTab("favorites", "Favorites"),
    NovaHomeTab("playlists", "Playlists"),
    NovaHomeTab("songs", "Tracks"),
    NovaHomeTab("albums", "Albums"),
    NovaHomeTab("artists", "Artists"),
    NovaHomeTab("folders", "Folders"),
)

@Composable
fun NovaHomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { novaHomeTabs.size })
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(novaHomeTabs.first().id) }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = novaHomeTabs[pagerState.currentPage].id
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 6.dp),
    ) {
        NovaFilterTabs(
            chips = novaHomeTabs.map { it.id to it.title },
            currentValue = selectedTab,
            onValueUpdate = { tabId ->
                selectedTab = tabId
                val index = novaHomeTabs.indexOfFirst { it.id == tabId }.coerceAtLeast(0)
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.padding(bottom = 6.dp),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> novaHomeTabs[page].id },
            ) { page ->
                when (novaHomeTabs[page].id) {
                    "home" -> HomeScreen(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                    )
                    "favorites" -> NovaFavoritesTab(navController)
                    "songs" -> LibrarySongsScreen(navController = navController, onDeselect = {}, showDeselectChip = false)
                    "artists" -> LibraryArtistsScreen(navController = navController, onDeselect = {}, showDeselectChip = false)
                    "albums" -> LibraryAlbumsScreen(navController = navController, onDeselect = {}, showDeselectChip = false)
                    "playlists" -> LibraryPlaylistsScreen(navController = navController, filterContent = {})
                    "folders" -> LocalFilesScreen(navController = navController, filterContent = {})
                }
            }
        }
    }
}

@Composable
private fun NovaFavoritesTab(navController: NavController) {
    val actions = listOf(
        Triple("Liked songs", "All tracks you've liked", "auto_playlist/liked"),
        Triple("Downloads", "Music saved for offline listening", "auto_playlist/downloaded"),
        Triple("Spotify favorites", "Your synced Spotify favorites", "spotify_liked_songs"),
        Triple("Listen together", "Share a listening room with friends", "listen_together"),
    )

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(actions.size) { index ->
            val (title, subtitle, route) = actions[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clickable { navController.navigate(route) },
                shape = RoundedCornerShape(22.dp),
                color = when (index) {
                    0 -> MaterialTheme.colorScheme.primaryContainer
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    2 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
