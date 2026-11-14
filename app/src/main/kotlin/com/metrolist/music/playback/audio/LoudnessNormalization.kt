package com.metrolist.music.playback.audio

import com.metrolist.music.constants.LoudnessLevel

/**
 * Calculates the PCM normalization gain in millibels for one media item.
 *
 * Perceptual loudness is already expressed as LUFS. The legacy loudness field is
 * converted to an approximate LUFS value using the same fallback convention as
 * the upstream Metrolist normalization implementation.
 */
internal fun calculateNormalizationGainMb(
    loudnessDb: Double?,
    perceptualLoudnessDb: Double?,
    level: LoudnessLevel,
): Int? {
    val measuredLufs = perceptualLoudnessDb?.takeIf { it.isFinite() }
        ?: loudnessDb?.takeIf { it.isFinite() }?.plus(LoudnessLevel.AGGRESSIVE.targetLufs)
        ?: return null

    return ((level.targetLufs.toDouble() - measuredLufs) * 100.0)
        .toInt()
        .coerceIn(-1500, 300)
}
