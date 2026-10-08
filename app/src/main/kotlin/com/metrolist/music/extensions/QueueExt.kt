/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.extensions

import android.content.Context
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.PersistQueue
import com.metrolist.music.models.QueueData
import com.metrolist.music.models.QueueType
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.playback.SpotifyYouTubeMapper
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.playback.queues.SpotifyQueue
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.spotify.Spotify

fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long
): PersistQueue {
    return when (this) {
        is ListQueue -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
        is YouTubeQueue -> {
            // Since endpoint is private, we'll store a simplified version
            val endpoint = "youtube_queue"
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE,
                queueData = QueueData.YouTubeData(endpoint = endpoint)
            )
        }
        is YouTubeAlbumRadio -> {
            // Since playlistId is private, we'll store a simplified version
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE_ALBUM_RADIO,
                queueData = QueueData.YouTubeAlbumRadioData(
                    playlistId = "youtube_album_radio"
                )
            )
        }
        is LocalAlbumRadio -> {
            // Since albumWithSongs and startIndex are private, we'll store a simplified version
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LOCAL_ALBUM_RADIO,
                queueData = QueueData.LocalAlbumRadioData(
                    albumId = "local_album_radio",
                    startIndex = 0
                )
            )
        }
        is SpotifyQueue -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.SPOTIFY,
            queueData = QueueData.SpotifyData(seedTrackId = this.seedTrackId),
        )
        else -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
    }
}

fun PersistQueue.toQueue(): Queue {
    return when (queueType) {
        is QueueType.LIST -> ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = mediaItemIndex,
            position = position
        )
        is QueueType.YOUTUBE -> {
            // For now, fallback to ListQueue since we can't reconstruct YouTubeQueue properly
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position
            )
        }
        is QueueType.YOUTUBE_ALBUM_RADIO -> {
            // For now, fallback to ListQueue since we can't reconstruct YouTubeAlbumRadio properly
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position
            )
        }
        is QueueType.LOCAL_ALBUM_RADIO -> {
            // For now, fallback to ListQueue since we can't reconstruct LocalAlbumRadio properly
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position
            )
        }
        is QueueType.SPOTIFY -> ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = mediaItemIndex,
            position = position,
        )
    }
}

suspend fun PersistQueue.toRuntimeQueue(
    context: Context,
    database: MusicDatabase,
): Queue {
    val spotifyData = queueData as? QueueData.SpotifyData
    if (queueType !is QueueType.SPOTIFY || spotifyData == null) return toQueue()

    val seedTrack = Spotify.track(spotifyData.seedTrackId).getOrNull() ?: return toQueue()
    return SpotifyQueue(
        initialTrack = seedTrack,
        mapper = SpotifyYouTubeMapper(database),
        context = context,
        database = database,
        preloadItem = null,
        restoredItems = items,
        restoredStartIndex = mediaItemIndex,
        restoredPosition = position,
    )
}
