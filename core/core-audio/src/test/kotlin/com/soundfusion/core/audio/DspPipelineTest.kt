package com.soundfusion.core.audio

import com.soundfusion.core.audio.dsp.DspPipeline
import com.soundfusion.core.audio.dsp.EqBand
import com.soundfusion.core.storage.AudioPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DspPipelineTest {

    private val preferences = mockk<AudioPreferences>(relaxed = true)
    private lateinit var pipeline: DspPipeline

    @Before
    fun setup() {
        every { preferences.eqEnabled } returns false
        every { preferences.bassBoostEnabled } returns false
        every { preferences.bassBoostStrength } returns 0
        every { preferences.loudnessNormEnabled } returns false
        every { preferences.targetGainMb } returns -1400
        every { preferences.eqBandLevels } returns (0..9).map { 0.toShort() }

        pipeline = DspPipeline(preferences)
    }

    @Test
    fun `initial eq bands are empty before attach`() {
        val bands = pipeline.eqBands.value
        assertEquals(0, bands.size)
    }

    @Test
    fun `initial bass boost is zero`() {
        val strength = pipeline.bassBoostStrength.value
        assertEquals(0, strength)
    }

    @Test
    fun `setEqEnabled updates preferences`() {
        pipeline.setEqEnabled(true)
        verify { preferences.eqEnabled = true }
    }

    @Test
    fun `setBassBoost updates preferences`() {
        pipeline.setBassBoost(500)
        verify { preferences.bassBoostStrength = 500 }
    }

    @Test
    fun `setLoudnessNorm updates preferences`() {
        pipeline.setLoudnessNorm(true, -1400)
        verify { preferences.loudnessNormEnabled = true }
        verify { preferences.targetGainMb = -1400 }
    }

    @Test
    fun `release cleans up without crash`() {
        pipeline.release()
        // Should not throw
    }

    @Test
    fun `EqBand data class works correctly`() {
        val band = EqBand(
            index = 3,
            frequencyHz = 1000,
            minLevel = -1500,
            maxLevel = 1500,
            currentLevel = 300,
        )
        assertEquals(3, band.index)
        assertEquals(1000, band.frequencyHz)
        assertEquals(300, band.currentLevel.toInt())
    }
}
