/**
 * Metrofy Project
 *
 * Learns broad Spotify -> YouTube variant preferences from manual match choices.
 * The model is intentionally tiny and local: only category counters are persisted,
 * never track titles, Spotify IDs, YouTube IDs, or listening history.
 */
package com.metrolist.music.playback

import android.content.Context
import android.content.SharedPreferences

object SpotifyMatchPreferenceLearner {
    private const val PREFS_NAME = "spotify_match_preferences"
    private const val MIN_EVIDENCE = 3
    private const val MAX_ADJUSTMENT = 0.12

    private enum class Variant {
        STUDIO,
        LIVE,
        VIDEO,
        SLOWED,
        SPED_UP,
        COVER,
        KARAOKE,
        INSTRUMENTAL,
        REMIX,
    }

    @Volatile
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences =
            context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )
    }

    /**
     * Records only the selected variant category. No song metadata is persisted.
     */
    fun recordManualChoice(
        spotifyTitle: String?,
        selectedYouTubeTitle: String,
    ) {
        val prefs = preferences ?: return
        val selected = classify(selectedYouTubeTitle)

        // If Spotify itself is explicitly a variant, that manual choice is not useful
        // as a general preference signal (e.g. a live album should resolve to live).
        val spotifyVariant = spotifyTitle?.let(::classify)
        if (spotifyVariant != null && spotifyVariant != Variant.STUDIO) return

        val key = key(selected)
        val next = (prefs.getInt(key, 0) + 1).coerceAtMost(10_000)
        prefs.edit().putInt(key, next).apply()
    }

    /**
     * Returns a small score adjustment applied on top of Metrofy's fuzzy match score.
     * Positive values favor a category the user repeatedly chooses; negative values
     * reinforce the default studio-audio preference.
     */
    fun scoreAdjustment(
        spotifyTitle: String,
        candidateTitle: String,
    ): Double {
        val prefs = preferences ?: return 0.0
        if (classify(spotifyTitle) != Variant.STUDIO) return 0.0

        val variant = classify(candidateTitle)
        if (variant == Variant.STUDIO) return 0.0

        val variantCount = prefs.getInt(key(variant), 0)
        val studioCount = prefs.getInt(key(Variant.STUDIO), 0)
        val evidence = variantCount + studioCount
        if (evidence < MIN_EVIDENCE) return 0.0

        // Laplace smoothing avoids wild swings after only a few corrections.
        val preferenceShare = (variantCount + 1.0) / (evidence + 2.0)
        val neutralShare = 0.25
        return ((preferenceShare - neutralShare) * 0.24)
            .coerceIn(-MAX_ADJUSTMENT, MAX_ADJUSTMENT)
    }

    fun reset() {
        preferences?.edit()?.clear()?.apply()
    }

    fun learnedChoices(): Int {
        val prefs = preferences ?: return 0
        return Variant.entries.sumOf { prefs.getInt(key(it), 0) }
    }

    private fun key(variant: Variant) = "variant_${variant.name.lowercase()}"

    private fun classify(title: String): Variant {
        val value = title.lowercase()
        return when {
            Regex("\\b(live|en vivo|en directo|ao vivo)\\b").containsMatchIn(value) -> Variant.LIVE
            Regex("\\b(music video|official video|lyric video|mv)\\b").containsMatchIn(value) -> Variant.VIDEO
            Regex("\\b(slowed|slowed down)\\b").containsMatchIn(value) -> Variant.SLOWED
            Regex("\\b(sped up|spedup|nightcore)\\b").containsMatchIn(value) -> Variant.SPED_UP
            Regex("\\bcover\\b").containsMatchIn(value) -> Variant.COVER
            Regex("\\bkaraoke\\b").containsMatchIn(value) -> Variant.KARAOKE
            Regex("\\binstrumental\\b").containsMatchIn(value) -> Variant.INSTRUMENTAL
            Regex("\\b(remix|mix|edit|version)\\b").containsMatchIn(value) -> Variant.REMIX
            else -> Variant.STUDIO
        }
    }
}
