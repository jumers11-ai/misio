package com.soundfusion.feature.playlists

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

class MoodPlaylistTest {

    private val trackDao = mockk<TrackDao>()
    private lateinit var generator: MoodPlaylistGenerator

    private val testTracks = (1..50).map { i ->
        TrackEntity(
            id = "track-$i",
            title = "Track $i",
            artist = "Artist ${i % 5}",
            durationMs = (60_000L + (i * 30_000L)) % 600_000L,
            source = MusicSource.entries[i % 3],
            sourceId = "src-$i",
            playCount = i % 10,
            isLiked = i % 3 == 0,
        )
    }

    @Before
    fun setup() {
        every { trackDao.observeAll() } returns flowOf(testTracks)
        generator = MoodPlaylistGenerator(trackDao)
    }

    @Test
    fun `generate returns correct mood`() = runTest {
        generator.generate(Mood.CHILL, limit = 10).test {
            val playlist = awaitItem()
            assertEquals(Mood.CHILL, playlist.mood)
            awaitComplete()
        }
    }

    @Test
    fun `generate respects limit`() = runTest {
        generator.generate(Mood.ENERGETIC, limit = 15).test {
            val playlist = awaitItem()
            assertTrue(playlist.tracks.size <= 15)
            awaitComplete()
        }
    }

    @Test
    fun `generate calculates total duration`() = runTest {
        generator.generate(Mood.FOCUS, limit = 5).test {
            val playlist = awaitItem()
            val expectedMs = playlist.tracks.sumOf { it.durationMs }
            assertEquals(expectedMs, playlist.estimatedDurationMs)
            awaitComplete()
        }
    }

    @Test
    fun `getAvailableMoods returns all moods`() {
        val moods = generator.getAvailableMoods()
        assertEquals(8, moods.size)
        assertTrue(moods.contains(Mood.CHILL))
        assertTrue(moods.contains(Mood.PARTY))
        assertTrue(moods.contains(Mood.SLEEP))
        assertTrue(moods.contains(Mood.WORKOUT))
    }

    @Test
    fun `each mood has emoji and label`() {
        Mood.entries.forEach { mood ->
            assertTrue(mood.label.isNotBlank())
            assertTrue(mood.emoji.isNotBlank())
        }
    }
}
