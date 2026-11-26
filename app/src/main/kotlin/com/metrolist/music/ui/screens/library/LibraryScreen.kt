/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.ChipSortTypeKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.ui.component.ChipShelf
import com.metrolist.music.ui.screens.library.local.LocalFilesScreen
import com.metrolist.music.utils.rememberEnumPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val libraryFilters = listOf(
        LibraryFilter.PLAYLISTS,
        LibraryFilter.SONGS,
        LibraryFilter.ALBUMS,
        LibraryFilter.ARTISTS,
        LibraryFilter.PODCASTS,
        LibraryFilter.LOCAL_FILES,
    )

    val filterContent = @Composable {
        ChipShelf(
            options = libraryFilters,
            selectedOption = filterType.takeUnless { it == LibraryFilter.LIBRARY },
            onSelectionChange = { filterType = it ?: LibraryFilter.LIBRARY },
            label = { filter ->
                stringResource(
                    when (filter) {
                        LibraryFilter.PLAYLISTS -> R.string.filter_playlists
                        LibraryFilter.SONGS -> R.string.filter_songs
                        LibraryFilter.ALBUMS -> R.string.filter_albums
                        LibraryFilter.ARTISTS -> R.string.filter_artists
                        LibraryFilter.PODCASTS -> R.string.filter_podcasts
                        LibraryFilter.LOCAL_FILES -> R.string.filter_local_files
                        LibraryFilter.LIBRARY -> R.string.filter_library
                    },
                )
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.PODCASTS -> LibraryPodcastsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )

            LibraryFilter.LOCAL_FILES -> LocalFilesScreen(
                navController,
                filterContent,
            )
        }
    }
}
