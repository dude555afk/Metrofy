/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.spotify.Spotify
import com.metrolist.spotify.SpotifyMapper
import com.metrolist.spotify.models.SpotifyTrack
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resolves ordinary YouTube-backed media to the matching Spotify track. */
object SpotifyTrackMatcher {
    private const val MIN_MATCH_SCORE = 0.72
    private val searchMutex = Mutex()

    suspend fun resolve(
        mediaId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        maxDurationDifferenceSeconds: Int? = null,
    ): SpotifyTrack? {
        SpotifyMetadataRegistry.get(mediaId)
            ?.takeIf { it.hasCompatibleDuration(durationSeconds, maxDurationDifferenceSeconds) }
            ?.let { return it }
        if (!Spotify.isAuthenticated() || title.isBlank()) return null

        return searchMutex.withLock {
            SpotifyMetadataRegistry.get(mediaId)
                ?.takeIf { it.hasCompatibleDuration(durationSeconds, maxDurationDifferenceSeconds) }
                ?.let { return@withLock it }
            val candidates = Spotify.search(
                query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" "),
                types = listOf("track"),
                limit = 10,
            ).getOrThrow().tracks?.items.orEmpty()

            candidates
                .map { candidate ->
                    candidate to SpotifyMapper.matchScore(
                        spotifyTitle = title,
                        spotifyArtist = artist,
                        spotifyDurationMs = durationSeconds * 1000,
                        candidateTitle = candidate.name,
                        candidateArtist = candidate.artists.firstOrNull()?.name.orEmpty(),
                        candidateDurationSec = candidate.durationMs / 1000,
                    )
                }
                .maxByOrNull { it.second }
                ?.takeIf {
                    it.second >= MIN_MATCH_SCORE &&
                        it.first.hasCompatibleDuration(durationSeconds, maxDurationDifferenceSeconds)
                }
                ?.first
                ?.also { SpotifyMetadataRegistry.register(mediaId, it) }
        }
    }

    private fun SpotifyTrack.hasCompatibleDuration(
        expectedDurationSeconds: Int,
        maxDifferenceSeconds: Int?,
    ): Boolean {
        if (maxDifferenceSeconds == null || expectedDurationSeconds <= 0 || durationMs <= 0) return true
        return kotlin.math.abs(durationMs / 1000 - expectedDurationSeconds) <= maxDifferenceSeconds
    }
}
