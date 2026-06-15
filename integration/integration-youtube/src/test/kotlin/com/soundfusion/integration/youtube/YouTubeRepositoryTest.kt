package com.soundfusion.integration.youtube

import app.cash.turbine.test
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.network.CacheManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeRepositoryTest {

    private lateinit var repository: YouTubeRepository
    private val extractor = mockk<YouTubeExtractorWrapper>()
    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val cacheManager = mockk<CacheManager>(relaxed = true)
    private val mapper = YouTubeMapper()
    private val testDispatcher = StandardTestDispatcher()

    private val mockResults = listOf(
        YouTubeSearchResult("yt-1", "Midnight Drive", "Synthwave", 234_000L, "https://img.com/1.jpg"),
        YouTubeSearchResult("yt-2", "Neon City", "RetroWave", 252_000L, "https://img.com/2.jpg"),
        YouTubeSearchResult("yt-3", "Digital Dreams", "Chillstep", 208_000L, "https://img.com/3.jpg"),
    )

    @Before
    fun setup() {
        every { cacheManager.getSearchResults(any(), any()) } returns null
        coEvery { extractor.search(any()) } returns mockResults

        repository = YouTubeRepository(
            extractor = extractor,
            trackDao = trackDao,
            cacheManager = cacheManager,
            mapper = mapper,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `search returns mapped tracks from extractor`() = runTest(testDispatcher) {
        repository.search("synthwave").test {
            val tracks = awaitItem()
            assertEquals(3, tracks.size)
            assertEquals("Midnight Drive", tracks[0].title)
            assertEquals("Synthwave", tracks[0].artist)
            assertEquals(MusicSource.YOUTUBE, tracks[0].source)
            assertEquals(234_000L, tracks[0].durationMs)
            awaitComplete()
        }
    }

    @Test
    fun `search uses cache when available`() = runTest(testDispatcher) {
        val cachedTracks = mockResults.map { mapper.toTrack(it) }
        every { cacheManager.getSearchResults(MusicSource.YOUTUBE, "cached") } returns cachedTracks

        repository.search("cached").test {
            // First emission from cache
            val cached = awaitItem()
            assertEquals(3, cached.size)

            // Second emission from network
            val fresh = awaitItem()
            assertEquals(3, fresh.size)

            awaitComplete()
        }
    }

    @Test
    fun `search persists results to database`() = runTest(testDispatcher) {
        repository.search("test").test {
            awaitItem()
            awaitComplete()
        }
        coVerify { trackDao.insertAll(any()) }
    }

    @Test
    fun `search caches results`() = runTest(testDispatcher) {
        repository.search("test").test {
            awaitItem()
            awaitComplete()
        }
        coVerify { cacheManager.cacheSearchResults(MusicSource.YOUTUBE, "test", any(), any()) }
    }

    @Test
    fun `getStreamUrl returns URL from extractor`() = runTest(testDispatcher) {
        coEvery { trackDao.getById(any()) } returns null
        coEvery { extractor.getStreamUrl("yt-1") } returns "https://stream.example.com/audio.m4a"

        repository.getStreamUrl("yt-1").test {
            val url = awaitItem()
            assertTrue(url.startsWith("https://"))
            awaitComplete()
        }
    }

    @Test
    fun `getStreamUrl uses cached URL from database`() = runTest(testDispatcher) {
        val cachedEntity = mockk<com.soundfusion.core.database.entity.TrackEntity> {
            every { streamUrl } returns "https://cached.stream/audio.m4a"
        }
        coEvery { trackDao.getById("yt-1") } returns cachedEntity

        repository.getStreamUrl("yt-1").test {
            val url = awaitItem()
            assertEquals("https://cached.stream/audio.m4a", url)
            awaitComplete()
        }
    }

    @Test
    fun `getTrending returns mapped trending tracks`() = runTest(testDispatcher) {
        coEvery { extractor.getTrending() } returns mockResults.take(2)

        repository.getTrending().test {
            val tracks = awaitItem()
            assertEquals(2, tracks.size)
            awaitComplete()
        }
    }
}
