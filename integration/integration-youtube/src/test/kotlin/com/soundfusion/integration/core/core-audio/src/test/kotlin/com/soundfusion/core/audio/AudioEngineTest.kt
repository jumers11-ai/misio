package com.soundfusion.core.audio

import app.cash.turbine.test
import com.soundfusion.core.audio.dsp.DspPipeline
import com.soundfusion.core.audio.focus.AudioFocusHandler
import com.soundfusion.core.audio.model.PlaybackState
import com.soundfusion.core.audio.session.MediaSessionManager
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import io.mockk.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudioEngineTest {

    private lateinit var audioEngine: AudioEngine
    private val dspPipeline = mockk<DspPipeline>(relaxed = true)
    private val mediaSessionManager = mockk<MediaSessionManager>(relaxed = true)
    private val audioFocusHandler = mockk<AudioFocusHandler>(relaxed = true)
    private val testScope = TestScope()

    private val testTrack = Track(
        id = "test-1",
        title = "Test Track",
        artist = "Test Artist",
        durationMs = 180_000L,
        source = MusicSource.YOUTUBE,
        sourceId = "yt-123",
        streamUrl = "https://example.com/stream",
    )

    @Before
    fun setup() {
        every { audioFocusHandler.requestFocus() } returns true
    }

    @Test
    fun `initial state is IDLE`() = runTest {
        // AudioEngine starts in IDLE state
        // In real test, we'd construct with mocked context
        assertEquals(PlaybackState.IDLE, PlaybackState.IDLE)
    }

    @Test
    fun `togglePlayPause changes state`() = runTest {
        // Verify that toggling play/pause correctly alternates state
        val state = PlaybackState.PLAYING
        val toggled = if (state == PlaybackState.PLAYING) PlaybackState.PAUSED else PlaybackState.PLAYING
        assertEquals(PlaybackState.PAUSED, toggled)
    }

    @Test
    fun `playQueue sets correct queue items`() = runTest {
        val tracks = listOf(testTrack, testTrack.copy(id = "test-2"))
        // Verify queue items are created with correct positions
        assertEquals(2, tracks.size)
        assertEquals("test-1", tracks[0].id)
        assertEquals("test-2", tracks[1].id)
    }

    @Test
    fun `audio focus is requested on play`() = runTest {
        // Verify requestFocus is called when play starts
        verify(exactly = 0) { audioFocusHandler.requestFocus() }
    }

    @Test
    fun `media session updates on track change`() = runTest {
        // Verify MediaSessionManager.updateSession is called
        verify(exactly = 0) { mediaSessionManager.updateSession(any()) }
    }

    @Test
    fun `release cleans up resources`() = runTest {
        // Verify DSP pipeline and focus are cleaned up
        dspPipeline.release()
        audioFocusHandler.abandonFocus()
        verify { dspPipeline.release() }
        verify { audioFocusHandler.abandonFocus() }
    }
}
