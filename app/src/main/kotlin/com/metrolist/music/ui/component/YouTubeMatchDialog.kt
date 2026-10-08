/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.R
import com.metrolist.spotify.Spotify
import com.metrolist.spotify.SpotifyMapper
import com.metrolist.spotify.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class YouTubeMatchResult(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
)

private data class SuggestedYouTubeMatch(
    val song: SongItem,
    val confidence: Int,
)

@Composable
fun YouTubeMatchDialog(
    spotifyTrack: SpotifyTrack? = null,
    spotifyTrackId: String? = null,
    currentYouTubeId: String?,
    onConfirm: (YouTubeMatchResult) -> Unit,
    onRestoreAutomatic: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<SongItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentMatchInfo by remember { mutableStateOf<SongItem?>(null) }
    var isLoadingCurrent by remember { mutableStateOf(currentYouTubeId != null) }
    var suggestions by remember { mutableStateOf<List<SuggestedYouTubeMatch>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(spotifyTrack != null || spotifyTrackId != null) }
    var resolvedSpotifyTrack by remember(spotifyTrack, spotifyTrackId) { mutableStateOf(spotifyTrack) }

    LaunchedEffect(spotifyTrack, spotifyTrackId) {
        resolvedSpotifyTrack = spotifyTrack
        if (resolvedSpotifyTrack == null && !spotifyTrackId.isNullOrBlank()) {
            resolvedSpotifyTrack =
                withContext(Dispatchers.IO) {
                    Spotify.track(spotifyTrackId).getOrNull()
                }
        }
    }

    LaunchedEffect(currentYouTubeId) {
        if (currentYouTubeId == null) {
            isLoadingCurrent = false
            return@LaunchedEffect
        }
        isLoadingCurrent = true
        try {
            val result = withContext(Dispatchers.IO) {
                YouTube.queue(listOf(currentYouTubeId)).getOrNull()
            }
            currentMatchInfo = result?.firstOrNull()
        } catch (_: Exception) { }
        isLoadingCurrent = false
    }

    LaunchedEffect(resolvedSpotifyTrack?.id) {
        val track = resolvedSpotifyTrack
        if (track == null) {
            suggestions = emptyList()
            isLoadingSuggestions = false
            return@LaunchedEffect
        }
        isLoadingSuggestions = true
        suggestions =
            withContext(Dispatchers.IO) {
                val query = SpotifyMapper.buildSearchQuery(track)
                val result = YouTube.searchSummary(query, incognito = true).getOrNull()
                result
                    ?.summaries
                    .orEmpty()
                    .flatMap { it.items }
                    .filterIsInstance<SongItem>()
                    .map { song ->
                        val score =
                            SpotifyMapper.matchScore(
                                spotifyTitle = track.name,
                                spotifyArtist = track.artists.firstOrNull()?.name.orEmpty(),
                                spotifyDurationMs = track.durationMs,
                                candidateTitle = song.title,
                                candidateArtist = song.artists.firstOrNull()?.name.orEmpty(),
                                candidateDurationSec = song.duration ?: 0,
                            )
                        SuggestedYouTubeMatch(
                            song = song,
                            confidence = (score * 100.0).roundToInt().coerceIn(0, 100),
                        )
                    }
                    .sortedByDescending { it.confidence }
                    .distinctBy { it.song.id }
                    .take(4)
            }
        isLoadingSuggestions = false
    }

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return trimmed
        return try {
            val uri = Uri.parse(trimmed)
            when {
                uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                uri.host?.contains("youtube.com") == true && uri.path == "/watch" ->
                    uri.getQueryParameter("v")
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(url) {
        val videoId = extractVideoId(url)
        if (videoId == null) {
            preview = null
            errorMessage = null
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        preview = null
        try {
            val result = withContext(Dispatchers.IO) {
                YouTube.queue(listOf(videoId)).getOrNull()
            }
            val song = result?.firstOrNull()
            if (song != null) {
                preview = song
            } else {
                errorMessage = "Video not found"
            }
        } catch (_: Exception) {
            errorMessage = "Failed to fetch video info"
        }
        isLoading = false
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = {
            Text(text = stringResource(R.string.change_youtube_version))
        },
        buttons = {
            if (currentYouTubeId != null && onRestoreAutomatic != null) {
                TextButton(
                    onClick = {
                        onRestoreAutomatic()
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.restore_automatic_matching))
                }
            }
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
            TextButton(
                enabled = preview != null && preview?.id != currentYouTubeId,
                onClick = {
                    preview?.let { song ->
                        onConfirm(
                            YouTubeMatchResult(
                                videoId = song.id,
                                title = song.title,
                                artist = song.artists.joinToString(", ") { it.name },
                                thumbnail = song.thumbnail,
                            )
                        )
                    }
                    onDismiss()
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (currentYouTubeId != null) {
                Text(
                    text = stringResource(R.string.current_match),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingCurrent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.searching),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    SongPreviewRow(song = currentMatchInfo, youtubeId = currentYouTubeId)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = stringResource(R.string.suggested_matches),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingSuggestions) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.searching),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                suggestions.forEach { suggestion ->
                    val song = suggestion.song
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    preview = song
                                    errorMessage = null
                                }
                                .padding(vertical = 6.dp),
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest.Builder(LocalContext.current)
                                    .data(song.thumbnail)
                                    .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = stringResource(R.string.match_confidence, suggestion.confidence),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (song.id == currentYouTubeId) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.paste_youtube_url)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Uri,
                ),
                keyboardActions = KeyboardActions(onDone = {}),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.searching),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                preview != null -> {
                    val song = preview!!
                    val isCurrentMatch = song.id == currentYouTubeId

                    Text(
                        text = stringResource(R.string.new_match),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrentMatch) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.thumbnail)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isCurrentMatch) {
                                Text(
                                    text = stringResource(R.string.current_match),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongPreviewRow(
    song: SongItem?,
    youtubeId: String,
) {
    if (song != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.thumbnail)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "music.youtube.com/watch?v=$youtubeId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        Text(
            text = "music.youtube.com/watch?v=$youtubeId",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
