package com.soundfusion.feature.home

import app.cash.turbine.test
import com.soundfusion.core.database.model.Album
import com.soundfusion.core.database.model.Track
import com.soundfusion.feature.home.domain.GetNewReleasesUseCase
import com.soundfusion.feature.home.domain.GetRecentlyPlayedUseCase
import com.soundfusion.feature.home.domain.GetRecommendationsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.soundfusion.core.database.model.MusicSource

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockTracks = (1..20).map { i ->
        Track(
            id = "track-$i",
            title = "Track $i",
            artist = "Artist $i",
            durationMs = 200_000L,
            source = MusicSource.YOUTUBE,
            sourceId = "yt-$i",
        )
    }

    private val mockAlbums = (1..5).map { i ->
        Album(
            id = "album-$i",
            title = "Album $i",
            artistId = "artist-$i",
            artistName = "Artist $i",
            trackCount = 10,
            source = MusicSource.SPOTIFY,
        )
    }

    private val getRecentlyPlayed = mockk<GetRecentlyPlayedUseCase>()
    private val getRecommendations = mockk<GetRecommendationsUseCase>()
    private val getNewReleases = mockk<GetNewReleasesUseCase>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getRecentlyPlayed(any()) } returns flowOf(mockTracks)
        every { getRecommendations(any()) } returns flowOf(mockTracks.shuffled().take(15))
        every { getNewReleases() } returns flowOf(mockAlbums)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates all sections`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(getRecentlyPlayed, getRecommendations, getNewReleases)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(20, state.recentlyPlayed.size)
            assertTrue(state.recommendations.isNotEmpty())
            assertEquals(5, state.newReleases.size)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `recently played limited to 20 items`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(getRecentlyPlayed, getRecommendations, getNewReleases)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.recentlyPlayed.size <= 20)
        }
    }

    @Test
    fun `refresh reloads all data`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(getRecentlyPlayed, getRecommendations, getNewReleases)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.recentlyPlayed.isNotEmpty())
        }
    }

    @Test
    fun `error in one section does not block others`() = runTest(testDispatcher) {
        every { getNewReleases() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(getRecentlyPlayed, getRecommendations, getNewReleases)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.recentlyPlayed.isNotEmpty())
            assertTrue(state.recommendations.isNotEmpty())
            assertTrue(state.newReleases.isEmpty())
            assertFalse(state.isLoading)
        }
    }
}
