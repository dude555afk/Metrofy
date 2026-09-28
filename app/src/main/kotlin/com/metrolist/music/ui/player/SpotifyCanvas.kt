/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.metrolist.music.playback.SpotifyCanvasCache
import com.metrolist.music.playback.SpotifyTrackMatcher
import timber.log.Timber

@Composable
internal fun SpotifyCanvasBackground(
    mediaId: String,
    title: String,
    artist: String,
    durationSeconds: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onAvailabilityChanged: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var canvasUrl by remember(mediaId) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaId, title, artist, durationSeconds, enabled) {
        onAvailabilityChanged(mediaId, false)
        canvasUrl = null
        if (enabled) {
            canvasUrl =
                runCatching {
                    val spotifyTrack = SpotifyTrackMatcher.resolve(mediaId, title, artist, durationSeconds)
                        ?: return@runCatching null
                    val trackUri = spotifyTrack.uri
                        ?: spotifyTrack.id.takeIf { it.isNotBlank() }?.let { "spotify:track:$it" }
                        ?: return@runCatching null
                    SpotifyCanvasCache.getUrl(trackUri)?.also {
                        SpotifyCanvasCache.prepare(context)
                    }
                }
                    .onFailure { Timber.tag("SpotifyCanvas").w(it, "Canvas lookup failed for $mediaId") }
                    .getOrNull()
        }
    }

    DisposableEffect(mediaId, enabled) {
        onDispose { onAvailabilityChanged(mediaId, false) }
    }

    canvasUrl?.let { url ->
        CanvasVideo(
            mediaId = mediaId,
            url = url,
            modifier = modifier.fillMaxSize(),
            onAvailabilityChanged = onAvailabilityChanged,
        )
    }
}

@Composable
@UnstableApi
private fun CanvasVideo(
    mediaId: String,
    url: String,
    modifier: Modifier = Modifier,
    onAvailabilityChanged: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val availabilityCallback = rememberUpdatedState(onAvailabilityChanged)
    var firstFrameRendered by remember(mediaId, url) { mutableStateOf(false) }
    var playbackFailed by remember(mediaId, url) { mutableStateOf(false) }
    val canvasPlayer =
        remember(mediaId, url) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(SpotifyCanvasCache.dataSourceFactory(context)),
                ).setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            3_000,
                            10_000,
                            500,
                            1_000,
                        ).build(),
                ).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                trackSelectionParameters =
                    trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
                addListener(
                    object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            firstFrameRendered = true
                            availabilityCallback.value(mediaId, true)
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            playbackFailed = true
                            availabilityCallback.value(mediaId, false)
                            Timber.tag("SpotifyCanvas").w(error, "Canvas video playback failed")
                        }
                    },
                )
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
        }

    DisposableEffect(canvasPlayer, lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> canvasPlayer.play()
                    Lifecycle.Event.ON_STOP -> canvasPlayer.pause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            availabilityCallback.value(mediaId, false)
            lifecycleOwner.lifecycle.removeObserver(observer)
            canvasPlayer.release()
        }
    }

    if (!playbackFailed) {
        AndroidView(
            factory = { viewContext ->
                TextureView(viewContext).apply {
                    alpha = 0f
                    canvasPlayer.setVideoTextureView(this)
                }
            },
            update = { textureView ->
                textureView.alpha = if (firstFrameRendered) 1f else 0f
            },
            modifier = modifier,
        )
    }
}
