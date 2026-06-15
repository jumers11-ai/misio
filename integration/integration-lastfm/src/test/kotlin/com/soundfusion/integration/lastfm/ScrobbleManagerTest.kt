package com.soundfusion.integration.lastfm

import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.audio.model.PlaybackState
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScrobbleManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val audioEngine = mockk<AudioEngine>(relaxed = true)
    private val lastFmRepo = mockk<LastFmRepository>(relaxed = true)
    private val lastFmAuth = mockk<LastFmAuthManager>(relaxed = true)

    private val currentTrackFlow = MutableStateFlow<Track?>(null)
    private val playbackStateFlow = MutableStateFlow(PlaybackState.IDLE)
    private val positionFlow = MutableStateFlow(0L)

    private val testTrack = Track(
        id = "test-1",
        title = "Test Track",
        artist = "Test Artist",
        durationMs = 200_000L,
        source = MusicSource.YOUTUBE,
        sourceId = "yt-1",
    )

    @Before
    fun setup() {
        every { audioEngine.currentTrack } returns currentTrackFlow
        every { audioEngine.playbackState } returns playbackStateFlow
        every { audioEngine.positionMs } returns positionFlow
        every { lastFmAuth.isConnected } returns MutableStateFlow(true)
    }

    @Test
    fun `scrobble not called before threshold`() = testScope.runTest {
        val manager = ScrobbleManager(audioEngine, lastFmRepo, lastFmAuth, testScope)
        manager.startObserving()

        currentTrackFlow.value = testTrack
        playbackStateFlow.value = PlaybackState.PLAYING

        advanceTimeBy(30_000L)

        coVerify(exactly = 0) { lastFmRepo.scrobble(any(), any(), any(), any()) }
    }

    @Test
    fun `scrobble called after 50 percent of duration`() = testScope.runTest {
        val manager = ScrobbleManager(audioEngine, lastFmRepo, lastFmAuth, testScope)
        manager.startObserving()

        currentTrackFlow.value = testTrack
        playbackStateFlow.value = PlaybackState.PLAYING

        advanceTimeBy(101_000L)

        coVerify(atLeast = 1) { lastFmRepo.scrobble("Test Track", "Test Artist", 200_000L, "test-1") }
    }

    @Test
    fun `scrobble cancelled on pause`() = testScope.runTest {
        val manager = ScrobbleManager(audioEngine, lastFmRepo, lastFmAuth, testScope)
        manager.startObserving()

        currentTrackFlow.value = testTrack
        playbackStateFlow.value = PlaybackState.PLAYING
        advanceTimeBy(30_000L)

        playbackStateFlow.value = PlaybackState.PAUSED
        advanceTimeBy(200_000L)

        coVerify(exactly = 0) { lastFmRepo.scrobble(any(), any(), any(), any()) }
    }

    @Test
    fun `no scrobble when not connected`() = testScope.runTest {
        every { lastFmAuth.isConnected } returns MutableStateFlow(false)

        val manager = ScrobbleManager(audioEngine, lastFmRepo, lastFmAuth, testScope)
        manager.startObserving()

        currentTrackFlow.value = testTrack
        playbackStateFlow.value = PlaybackState.PLAYING
        advanceTimeBy(200_000L)

        coVerify(exactly = 0) { lastFmRepo.scrobble(any(), any(), any(), any()) }
    }
}
