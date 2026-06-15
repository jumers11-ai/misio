package com.soundfusion.core.audio

import com.soundfusion.core.storage.AudioPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CrossfadeManagerTest {

    private val preferences = mockk<AudioPreferences>(relaxed = true)
    private val testScope = TestScope()
    private lateinit var manager: CrossfadeManager

    @Before
    fun setup() {
        every { preferences.crossfadeDurationMs } returns 6000L
        manager = CrossfadeManager(preferences, testScope)
    }

    @Test
    fun `initial duration from preferences`() {
        assertEquals(6000L, manager.crossfadeDurationMs.value)
    }

    @Test
    fun `setCrossfadeDuration updates state and preferences`() {
        manager.setCrossfadeDuration(8000L)
        assertEquals(8000L, manager.crossfadeDurationMs.value)
        verify { preferences.crossfadeDurationMs = 8000L }
    }

    @Test
    fun `shouldCrossfade true when duration greater than 0`() {
        manager.setCrossfadeDuration(3000L)
        assertTrue(manager.shouldCrossfade())
    }

    @Test
    fun `shouldCrossfade false when duration is 0`() {
        manager.setCrossfadeDuration(0L)
        assertFalse(manager.shouldCrossfade())
    }

    @Test
    fun `getPrerollMs returns crossfade duration`() {
        manager.setCrossfadeDuration(5000L)
        assertEquals(5000L, manager.getPrerollMs())
    }

    @Test
    fun `initial isTransitioning is false`() {
        assertFalse(manager.isTransitioning.value)
    }

    @Test
    fun `cancelTransition resets state`() {
        manager.cancelTransition()
        assertFalse(manager.isTransitioning.value)
    }
}
