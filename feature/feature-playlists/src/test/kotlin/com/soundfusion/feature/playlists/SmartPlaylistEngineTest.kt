package com.soundfusion.feature.playlists

import app.cash.turbine.test
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartPlaylistEngineTest {

    private val trackDao = mockk<TrackDao>()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var engine: SmartPlaylistEngine

    private val testTracks = listOf(
        createTrack("1", "Alpha Song", "Artist A", MusicSource.YOUTUBE, playCount = 10, isLiked = true, durationMs = 120_000),
        createTrack("2", "Beta Song", "Artist B", MusicSource.SPOTIFY, playCount = 3, isLiked = false, durationMs = 240_000),
        createTrack("3", "Gamma Song", "Artist A", MusicSource.LOCAL, playCount = 0, isLiked = true, durationMs = 60_000),
        createTrack("4", "Delta Song", "Artist C", MusicSource.YOUTUBE, playCount = 20, isLiked = false, durationMs = 350_000),
        createTrack("5", "Epsilon Song", "Artist B", MusicSource.SPOTIFY, playCount = 0, isLiked = true, durationMs = 180_000),
    )

    @Before
    fun setup() {
        every { trackDao.observeAll() } returns flowOf(testTracks)
        engine = SmartPlaylistEngine(trackDao, json)
    }

    @Test
    fun `source rule filters correctly`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(SmartRule.SourceRule(MusicSource.YOUTUBE)),
            sortBy = SortOption.TITLE_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(2, tracks.size)
            assertTrue(tracks.all { it.source == MusicSource.YOUTUBE })
            awaitComplete()
        }
    }

    @Test
    fun `liked rule filters correctly`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(SmartRule.LikedRule()),
            sortBy = SortOption.TITLE_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(3, tracks.size)
            assertTrue(tracks.all { it.isLiked })
            awaitComplete()
        }
    }

    @Test
    fun `play count rule filters correctly`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(SmartRule.PlayCountRule(minPlays = 5)),
            sortBy = SortOption.PLAY_COUNT_DESC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(2, tracks.size)
            assertEquals("Delta Song", tracks[0].title)
            assertEquals("Alpha Song", tracks[1].title)
            awaitComplete()
        }
    }

    @Test
    fun `never played rule filters correctly`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(SmartRule.NeverPlayedRule()),
            sortBy = SortOption.TITLE_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(2, tracks.size)
            assertTrue(tracks.all { it.playCount == 0 })
            awaitComplete()
        }
    }

    @Test
    fun `duration rule filters short tracks`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(SmartRule.DurationRule(maxMs = 180_000)),
            sortBy = SortOption.DURATION_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(3, tracks.size)
            assertTrue(tracks.all { it.durationMs <= 180_000 })
            assertEquals("Gamma Song", tracks[0].title) // 60s
            awaitComplete()
        }
    }

    @Test
    fun `multiple rules combine with AND`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(
                SmartRule.LikedRule(),
                SmartRule.SourceRule(MusicSource.YOUTUBE),
            ),
            matchAll = true,
            sortBy = SortOption.TITLE_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(1, tracks.size)
            assertEquals("Alpha Song", tracks[0].title)
            awaitComplete()
        }
    }

    @Test
    fun `multiple rules combine with OR`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(
                SmartRule.SourceRule(MusicSource.LOCAL),
                SmartRule.PlayCountRule(minPlays = 10),
            ),
            matchAll = false,
            sortBy = SortOption.TITLE_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(3, tracks.size) // Local(1) + PlayCount>=10(2), one overlaps
            awaitComplete()
        }
    }

    @Test
    fun `limit caps results`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = emptyList(),
            sortBy = SortOption.TITLE_ASC,
            limit = 3,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals(3, tracks.size)
            awaitComplete()
        }
    }

    @Test
    fun `sort by title ascending works`() = runTest {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = emptyList(),
            sortBy = SortOption.TITLE_ASC,
        )
        engine.resolve(def).test {
            val tracks = awaitItem()
            assertEquals("Alpha Song", tracks[0].title)
            assertEquals("Epsilon Song", tracks[4].title)
            awaitComplete()
        }
    }

    @Test
    fun `serialization round-trip works`() {
        val def = SmartPlaylistDefinition(
            name = "Test",
            rules = listOf(SmartRule.LikedRule(), SmartRule.SourceRule(MusicSource.YOUTUBE)),
            sortBy = SortOption.PLAY_COUNT_DESC,
            limit = 50,
        )
        val serialized = engine.serializeDefinition(def)
        val deserialized = engine.deserializeDefinition(serialized)
        assertEquals(def.name, deserialized.name)
        assertEquals(def.rules.size, deserialized.rules.size)
        assertEquals(def.sortBy, deserialized.sortBy)
        assertEquals(def.limit, deserialized.limit)
    }

    @Test
    fun `presets are generated correctly`() {
        val presets = engine.createPresets()
        assertTrue(presets.size >= 8)
        assertTrue(presets.any { it.name == "Ulubione" })
        assertTrue(presets.any { it.name == "Most Played" })
        assertTrue(presets.any { it.name == "Discover" })
        assertTrue(presets.any { it.name == "Offline Ready" })
    }

    private fun createTrack(
        id: String,
        title: String,
        artist: String,
        source: MusicSource,
        playCount: Int = 0,
        isLiked: Boolean = false,
        durationMs: Long = 200_000,
    ) = TrackEntity(
        id = id,
        title = title,
        artist = artist,
        durationMs = durationMs,
        source = source,
        sourceId = "${source.name.lowercase()}-$id",
        playCount = playCount,
        isLiked = isLiked,
    )
}
