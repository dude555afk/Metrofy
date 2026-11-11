package com.metrolist.music.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherSyncRulesTest {
    @Test
    fun trackTransitionRemainsEligibleUntilMetadataIsReady() {
        assertFalse(shouldBroadcastTrackTransition("new-track", "old-track", false, false))
        assertTrue(shouldBroadcastTrackTransition("new-track", "old-track", true, false))
    }

    @Test
    fun alreadySyncedAndInternalTracksAreNotBroadcastAgain() {
        assertFalse(shouldBroadcastTrackTransition("same", "same", true, false))
        assertFalse(shouldBroadcastTrackTransition("new", "old", true, true))
        assertFalse(shouldBroadcastTrackTransition(null, null, true, false))
    }

    @Test
    fun guestReconcilesWhenHostTrackDiffersOrLocalTrackIsMissing() {
        assertTrue(guestNeedsTrackReconcile("host-track", "guest-track"))
        assertTrue(guestNeedsTrackReconcile("host-track", null))
        assertFalse(guestNeedsTrackReconcile("host-track", "host-track"))
        assertFalse(guestNeedsTrackReconcile(null, "guest-track"))
    }

    @Test
    fun zeroPlaybackPositionsArePreservedForPlaybackActions() {
        assertEquals(0L, playbackPositionFromProto(0L, PlaybackActions.PLAY)!!)
        assertEquals(0L, playbackPositionFromProto(0L, PlaybackActions.PAUSE)!!)
        assertEquals(0L, playbackPositionFromProto(0L, PlaybackActions.SEEK)!!)
        assertEquals(null, playbackPositionFromProto(0L, PlaybackActions.CHANGE_TRACK))
    }

    @Test
    fun zeroVolumeIsPreservedOnlyForSetVolume() {
        assertEquals(0f, playbackVolumeFromProto(0f, PlaybackActions.SET_VOLUME)!!, 0f)
        assertEquals(null, playbackVolumeFromProto(0f, PlaybackActions.PLAY))
    }
}
