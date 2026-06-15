package com.soundfusion.feature.recommendations

import app.cash.turbine.test
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TasteEngineTest {

    private val trackDao = mockk<TrackDao>()
    private lateinit var engine: TasteEngine

    private val testTracks = listOf(
        createTrack("1", "Song A", "Artist X", MusicSource.YOUTUBE, playCount = 10, isLiked = true),
        createTrack("2", "Song B", "Artist X", MusicSource.YOUTUBE, playCount = 8),
        createTrack("3", "Song C", "Artist Y", MusicSource.SPOTIFY, playCount = 5),
        createTrack("4", "Song D", "Artist Z", MusicSource.LOCAL, playCount = 0),
        createTrack("5", "Song E", "Artist X", MusicSource.YOUTUBE, playCount = 3),
    )

    @Before
    fun setup() {
        every { trackDao.observeAll() } returns flowOf(testTracks)
        engine = TasteEngine(trackDao)
    }

    @Test
    fun `buildProfile identifies top artists`() = runTest {
        val profile = engine.buildProfile()
        assertTrue(profile.topArtists.containsKey("Artist X"))
        assertTrue(profile.topArtists["Artist X"]!! > profile.topArtists["Artist Y"] ?: 0f)
    }

    @Test
    fun `buildProfile calculates source distribution`() = runTest {
        val profile = engine.buildProfile()
        assertTrue(profile.topSources.containsKey(MusicSource.YOUTUBE))
        assertTrue(profile.topSources[MusicSource.YOUTUBE]!! > 0.5f)
    }

    @Test
    fun `buildProfile calculates diversity score`() = runTest {
        val profile = engine.buildProfile()
        assertTrue(profile.diversityScore > 0f)
        assertTrue(profile.diversityScore <= 1f)
    }

    @Test
    fun `generateRecommendations returns scored tracks`() = runTest {
        val profile = engine.buildProfile()
        engine.generateRecommendations(profile, limit = 5).test {
            val scored = awaitItem()
            assertEquals(5, scored.size)
            assertTrue(scored[0].score >= scored[1].score)
            assertTrue(scored.all { it.reason.isNotBlank() })
            awaitComplete()
        }
    }

    @Test
    fun `unplayed tracks get discovery bonus`() = runTest {
        val profile = engine.buildProfile()
        engine.generateRecommendations(profile, limit = 10).test {
            val scored = awaitItem()
            val unplayedScore = scored.find { it.track.id == "4" }
            assertTrue(unplayedScore != null)
            assertTrue(unplayedScore!!.reason.contains("słuchane") || unplayedScore.reason.contains("gustu"))
            awaitComplete()
        }
    }

    @Test
    fun `generateMixes creates at least 3 mixes`() = runTest {
        val profile = engine.buildProfile()
        val mixes = engine.generateMixes(profile)
        assertTrue(mixes.size >= 3)
        assertTrue(mixes.any { it.title.contains("Discover") })
    }

    @Test
    fun `cosineSimilarity identical profiles return 1`() {
        val a = mapOf("rock" to 0.5f, "pop" to 0.3f, "jazz" to 0.2f)
        val sim = engine.cosineSimilarity(a, a)
        assertTrue(sim > 0.99f)
    }

    @Test
    fun `cosineSimilarity orthogonal profiles return 0`() {
        val a = mapOf("rock" to 1f)
        val b = mapOf("jazz" to 1f)
        val sim = engine.cosineSimilarity(a, b)
        assertEquals(0f, sim, 0.001f)
    }

    private fun createTrack(id: String, title: String, artist: String, source: MusicSource, playCount: Int = 0, isLiked: Boolean = false) =
        TrackEntity(id = id, title = title, artist = artist, durationMs = 200_000, source = source, sourceId = "$source-$id", playCount = playCount, isLiked = isLiked)
}
