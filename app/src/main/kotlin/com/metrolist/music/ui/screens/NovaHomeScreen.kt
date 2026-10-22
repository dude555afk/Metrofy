@file:OptIn(ExperimentalFoundationApi::class)

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHostState
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

private val novaHomeTabs = listOf(
    NovaHomeTab("home", "Home"),
    NovaHomeTab("songs", "Tracks"),
    NovaHomeTab("albums", "Albums"),
    NovaHomeTab("artists", "Artists"),
    NovaHomeTab("playlists", "Playlists"),
    NovaHomeTab("folders", "Folders"),
    NovaHomeTab("favorites", "Favorites"),
)

/**
 * SoundPod-style top-level library tabs over Metrofy's existing music/feed/playback stack.
 * This is only installed by the separate Metrofy Nova preview flavor.
 */
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
            .padding(top = 2.dp),
    ) {
        NovaFilterTabs(
            chips = novaHomeTabs.map { it.id to it.title },
            currentValue = selectedTab,
            onValueUpdate = { tabId ->
                selectedTab = tabId
                val index = novaHomeTabs.indexOfFirst { it.id == tabId }.coerceAtLeast(0)
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
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
                    "songs" -> LibrarySongsScreen(navController = navController, onDeselect = {})
                    "artists" -> LibraryArtistsScreen(navController = navController, onDeselect = {})
                    "albums" -> LibraryAlbumsScreen(navController = navController, onDeselect = {})
                    "playlists" -> LibraryPlaylistsScreen(navController = navController, filterContent = {})
                    "folders" -> LocalFilesScreen(navController = navController, filterContent = {})
                }
            }
        }
    }
}

@Composable
private fun NovaFavoritesTab(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Your favorites",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "The music you have saved, all in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        NovaFavoriteAction(
            title = "Liked songs",
            subtitle = "All tracks you've liked",
            onClick = { navController.navigate("auto_playlist/liked") },
        )
        NovaFavoriteAction(
            title = "Offline music",
            subtitle = "Tracks saved for listening offline",
            onClick = { navController.navigate("auto_playlist/downloaded") },
        )
        NovaFavoriteAction(
            title = "Spotify liked songs",
            subtitle = "Your synced Spotify favorites",
            onClick = { navController.navigate("spotify_liked_songs") },
        )
        NovaFavoriteAction(
            title = "Listen together",
            subtitle = "Share a listening room with friends",
            onClick = { navController.navigate("listen_together") },
        )
    }
}

@Composable
private fun NovaFavoriteAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
