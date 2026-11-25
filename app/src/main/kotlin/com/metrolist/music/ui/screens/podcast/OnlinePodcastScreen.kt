package com.metrolist.music.ui.screens.podcast

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PodcastItem
import timber.log.Timber
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.db.entities.PodcastEntity
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.OnlinePodcastViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlinePodcastScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePodcastViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val podcast by viewModel.podcast.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val libraryPodcast by viewModel.libraryPodcast.collectAsState()

    val lazyListState = rememberLazyListState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredEpisodes = remember(episodes, query) {
        if (query.text.isEmpty()) episodes
        else episodes.filter { episode ->
            episode.title.contains(query.text, ignoreCase = true) ||
                episode.author?.name?.contains(query.text, ignoreCase = true) == true
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            if (podcast == null && isLoading) {
                item(key = "loading_placeholder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            } else if (error != null) {
                item(key = "error") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = error ?: stringResource(R.string.error_unknown),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.retry() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            } else {
                podcast?.let { podcastItem ->
                    if (!isSearching) {
                        item(key = "podcast_header") {
                            val context = LocalContext.current
                            PodcastHeader(
                                podcast = podcastItem,
                                episodeCount = episodes.size,
                                inLibrary = libraryPodcast?.inLibrary == true,
                                canPlay = episodes.any { !it.isUnavailable },
                                onPlayClick = {
                                    val playableEpisodes = episodes.filterNot { it.isUnavailable }
                                    if (playableEpisodes.isNotEmpty()) {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = podcastItem.title,
                                                items = playableEpisodes.map { it.toMediaMetadata().toMediaItem() },
                                            ),
                                        )
                                    }
                                },
                                onLibraryClick = { viewModel.toggleLibrary() },
                                onViewChannelClick = {
                                    val channelId = podcastItem.channelId ?: podcastItem.author?.id
                                    if (channelId != null) {
                                        navController.navigate("artist/$channelId?isPodcastChannel=true")
                                    }
                                },
                            )
                        }
                    }

                    itemsIndexed(
                        items = filteredEpisodes,
                        key = { _, episode -> episode.id }
                    ) { index, episode ->
                        YouTubeListItem(
                            item = episode,
                            isActive = mediaMetadata?.id == episode.id,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        if (episode.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            Timber.d("Playing episode: ${episode.title}, index: $index, total episodes: ${filteredEpisodes.size}")
                                            val mediaItems = filteredEpisodes.map { it.toMediaMetadata().toMediaItem() }
                                            Timber.d("Created ${mediaItems.size} media items for queue")
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = podcast?.title,
                                                    items = mediaItems,
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeSongMenu(episode.asSongItem(), navController, menuState::dismiss)
                                        }
                                    }
                                )
                                .animateItem(),
                            trailingContent = {
                                IconButton(onClick = {
                                    menuState.show {
                                        YouTubeSongMenu(episode.asSongItem(), navController, menuState::dismiss)
                                    }
                                }) {
                                    Icon(painterResource(R.drawable.more_vert), null)
                                }
                            }
                        )
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else if (lazyListState.firstVisibleItemIndex > 0) {
                    Text(podcast?.title ?: "")
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching) navController.backToMain()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
private fun PodcastHeader(
    podcast: PodcastItem,
    episodeCount: Int,
    inLibrary: Boolean,
    canPlay: Boolean,
    onPlayClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onViewChannelClick: () -> Unit,
) {
    val context = LocalContext.current
    val canViewChannel = !podcast.channelId.isNullOrBlank() || !podcast.author?.id.isNullOrBlank()
    val artwork: @Composable (Modifier) -> Unit = { artworkModifier ->
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(podcast.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = artworkModifier.clip(RoundedCornerShape(16.dp)),
        )
    }
    val titleAndMeta: @Composable () -> Unit = {
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        podcast.author?.name?.let { authorName ->
            Text(
                text = authorName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = podcast.episodeCountText ?: "$episodeCount episodes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val actions: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onPlayClick,
                enabled = canPlay,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_widget_play),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.play), maxLines = 1)
            }
            OutlinedButton(
                onClick = onLibraryClick,
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (inLibrary) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (inLibrary) R.drawable.library_add_check else R.drawable.library_add,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(if (inLibrary) R.string.remove_from_library else R.string.add_to_library),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (canViewChannel) {
            OutlinedButton(
                onClick = onViewChannelClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.view_channel))
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (maxWidth >= 620.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                artwork(Modifier.size(160.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    titleAndMeta()
                    actions()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    artwork(Modifier.size(96.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        titleAndMeta()
                    }
                }
                actions()
            }
        }
    }
}
