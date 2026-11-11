package com.metrolist.music.listentogether

/**
 * Small deterministic rules shared by Listen Together playback and regression tests.
 */
internal fun shouldBroadcastTrackTransition(
    trackId: String?,
    lastSyncedTrackId: String?,
    metadataReady: Boolean,
    internalSync: Boolean,
): Boolean =
    !internalSync &&
        metadataReady &&
        !trackId.isNullOrBlank() &&
        trackId != lastSyncedTrackId

internal fun guestNeedsTrackReconcile(
    actionTrackId: String?,
    localMediaId: String?,
): Boolean {
    val expected = actionTrackId?.takeIf { it.isNotEmpty() } ?: return false
    return localMediaId == null || localMediaId != expected
}

internal fun playbackPositionFromProto(
    position: Long,
    action: String,
): Long? =
    position.takeIf {
        it != 0L ||
            action == PlaybackActions.PLAY ||
            action == PlaybackActions.PAUSE ||
            action == PlaybackActions.SEEK
    }

internal fun playbackVolumeFromProto(
    volume: Float,
    action: String,
): Float? = volume.takeIf { action == PlaybackActions.SET_VOLUME }
