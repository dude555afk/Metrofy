package com.metrolist.music.playback.audio

import com.metrolist.music.constants.LoudnessLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoudnessNormalizationTest {

    @Test
    fun `perceptual loudness uses selected target level`() {
        assertEquals(
            -400,
            calculateNormalizationGainMb(
                loudnessDb = -6.0,
                perceptualLoudnessDb = -10.0,
                level = LoudnessLevel.BALANCED,
            ),
        )
        assertEquals(
            -700,
            calculateNormalizationGainMb(
                loudnessDb = -6.0,
                perceptualLoudnessDb = -10.0,
                level = LoudnessLevel.QUIET,
            ),
        )
    }

    @Test
    fun `legacy loudness falls back to aggressive target reference`() {
        assertEquals(
            -100,
            calculateNormalizationGainMb(
                loudnessDb = -6.0,
                perceptualLoudnessDb = null,
                level = LoudnessLevel.BALANCED,
            ),
        )
    }

    @Test
    fun `normalization gain is clamped to safe range`() {
        assertEquals(
            300,
            calculateNormalizationGainMb(
                loudnessDb = null,
                perceptualLoudnessDb = -30.0,
                level = LoudnessLevel.BALANCED,
            ),
        )
        assertEquals(
            -1500,
            calculateNormalizationGainMb(
                loudnessDb = null,
                perceptualLoudnessDb = 10.0,
                level = LoudnessLevel.QUIET,
            ),
        )
    }

    @Test
    fun `missing or invalid loudness returns no gain`() {
        assertNull(
            calculateNormalizationGainMb(
                loudnessDb = null,
                perceptualLoudnessDb = Double.NaN,
                level = LoudnessLevel.BALANCED,
            ),
        )
    }
}
