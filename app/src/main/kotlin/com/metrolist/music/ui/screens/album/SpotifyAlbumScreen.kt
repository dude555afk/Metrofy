/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.playback.queues.SpotifyPlaylistQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.MetrolistCollectionHeader
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.viewmodels.SpotifyAlbumViewModel
import com.metrolist.spotify.SpotifyMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyAlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SpotifyAlbumViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current

    val album by viewModel.album.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSpotifyId by produceState<String?>(initialValue = null, mediaMetadata?.id) {
        val ytId = mediaMetadata?.id
        value = if (ytId != null) {
            withContext(Dispatchers.IO) { database.getSpotifyMatchByYouTubeId(ytId)?.spotifyId }
        } else {
            null
        }
    }

    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "header") {
                val thumbnailUrl = album?.images
                    ?.firstOrNull { (it.width ?: 0) >= 300 }?.url
                    ?: album?.images?.firstOrNull()?.url

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 960.dp
                    val isMedium = maxWidth >= 600.dp && !isWide
                    val mediumArtworkSize = minOf(256.dp, (maxWidth - 56.dp).coerceAtLeast(0.dp) * 0.4f)

                    MetrolistCollectionHeader(
                        isWide = isWide,
                        isMedium = isMedium,
                        mediumArtworkSize = mediumArtworkSize,
                        artwork = { artworkModifier, cornerRadius ->
                            if (thumbnailUrl != null) {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = album?.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = artworkModifier.clip(RoundedCornerShape(cornerRadius)),
                                )
                            } else {
                                Box(
                                    modifier = artworkModifier
                                        .clip(RoundedCornerShape(cornerRadius))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                )
                            }
                        },
                        actions = {
                            if (!isLoading && tracks.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    androidx.compose.material3.FilledTonalIconButton(
                                        onClick = {
                                            val albumId = album?.id ?: return@FilledTonalIconButton
                                            val shuffled = tracks.shuffled()
                                            if (shuffled.isNotEmpty()) {
                                                playerConnection.playQueue(
                                                    SpotifyPlaylistQueue(
                                                        playlistId = "album_$albumId",
                                                        initialTracks = shuffled,
                                                        startIndex = 0,
                                                        mapper = viewModel.mapper,
                                                    ),
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.shuffle),
                                            contentDescription = stringResource(R.string.shuffle),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    androidx.compose.material3.FilledIconButton(
                                        onClick = {
                                            val albumId = album?.id ?: return@FilledIconButton
                                            if (tracks.isNotEmpty()) {
                                                playerConnection.playQueue(
                                                    SpotifyPlaylistQueue(
                                                        playlistId = "album_$albumId",
                                                        initialTracks = tracks,
                                                        startIndex = 0,
                                                        mapper = viewModel.mapper,
                                                    ),
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(64.dp),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.play),
                                            contentDescription = stringResource(R.string.play),
                                            modifier = Modifier.size(30.dp),
                                        )
                                    }
                                }
                            }
                        },
                    ) {
                        Text(
                            text = album?.name.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = album?.artists?.joinToString { it.name }.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val metaInfo = buildList {
                            album?.albumType?.replaceFirstChar { it.uppercase() }?.let(::add)
                            album?.releaseDate?.take(4)?.let(::add)
                            if (tracks.isNotEmpty()) {
                                add(pluralStringResource(R.plurals.n_song, tracks.size, tracks.size))
                            } else {
                                album?.totalTracks?.takeIf { it > 0 }?.let {
                                    add(pluralStringResource(R.plurals.n_song, it, it))
                                }
                            }
                        }
                        if (metaInfo.isNotEmpty()) {
                            Text(
                                text = metaInfo.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (error != null) {
                item(key = "error") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.retry() },
                        ) {
                            Text(stringResource(R.string.retry_button))
                        }
                    }
                }
            }

            if (!isLoading && error == null && tracks.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.spotify_no_tracks),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            itemsIndexed(
                items = tracks,
                key = { index, track -> "track_${track.id}_$index" },
            ) { index, track ->
                val thumbnailUrl = SpotifyMapper.getTrackThumbnail(track)

                val isActive = currentSpotifyId != null && currentSpotifyId == track.id
                ListItem(
                    title = track.name,
                    subtitle = joinByBullet(
                        track.artists.joinToString { it.name },
                        makeTimeString(track.durationMs.toLong()),
                    ),
                    isActive = isActive,
                    thumbnailContent = {
                        ItemThumbnail(
                            thumbnailUrl = thumbnailUrl,
                            isActive = isActive,
                            isPlaying = isPlaying,
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val albumId = album?.id ?: return@clickable
                            playerConnection.playQueue(
                                SpotifyPlaylistQueue(
                                    playlistId = "album_$albumId",
                                    initialTracks = tracks,
                                    startIndex = index,
                                    mapper = viewModel.mapper,
                                )
                            )
                        }
                        .animateItem(),
                )
            }
        }

        TopAppBar(
            title = { Text(album?.name ?: stringResource(R.string.albums)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )
    }
}
