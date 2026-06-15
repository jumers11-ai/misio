package com.soundfusion.core.database

import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDaoTest {

    private fun createTrackEntity(
        id: String = "test-1",
        title: String = "Test Track",
        artist: String = "Test Artist",
        source: MusicSource = MusicSource.YOUTUBE,
        sourceId: String = "yt-123",
        durationMs: Long = 180_000L,
        playCount: Int = 0,
        isLiked: Boolean = false,
    ) = TrackEntity(
        id = id,
        title = title,
        artist = artist,
        durationMs = durationMs,
        source = source,
        sourceId = sourceId,
        playCount = playCount,
        isLiked = isLiked,
    )

    @Test
    fun `track entity creates with correct defaults`() {
        val entity = createTrackEntity()
        assertEquals("test-1", entity.id)
        assertEquals("Test Track", entity.title)
        assertEquals("Test Artist", entity.artist)
        assertEquals(MusicSource.YOUTUBE, entity.source)
        assertEquals(180_000L, entity.durationMs)
        assertEquals(0, entity.playCount)
        assertEquals(false, entity.isLiked)
        assertEquals(false, entity.isOffline)
        assertNull(entity.replayGain)
        assertNull(entity.albumName)
        assertNull(entity.streamUrl)
        assertNotNull(entity.addedAt)
    }

    @Test
    fun `track entity copy works correctly`() {
        val original = createTrackEntity()
        val liked = original.copy(isLiked = true, playCount = 5)

        assertEquals(original.id, liked.id)
        assertEquals(original.title, liked.title)
        assertTrue(liked.isLiked)
        assertEquals(5, liked.playCount)
    }

    @Test
    fun `different sources create distinct entities`() {
        val ytTrack = createTrackEntity(id = "1", source = MusicSource.YOUTUBE, sourceId = "yt-1")
        val spTrack = createTrackEntity(id = "2", source = MusicSource.SPOTIFY, sourceId = "sp-1")
        val localTrack = createTrackEntity(id = "3", source = MusicSource.LOCAL, sourceId = "local-1")

        assertEquals(MusicSource.YOUTUBE, ytTrack.source)
        assertEquals(MusicSource.SPOTIFY, spTrack.source)
        assertEquals(MusicSource.LOCAL, localTrack.source)
        assertTrue(ytTrack.id != spTrack.id)
    }

    @Test
    fun `music source enum has all expected values`() {
        val sources = MusicSource.entries
        assertEquals(5, sources.size)
        assertTrue(sources.contains(MusicSource.YOUTUBE))
        assertTrue(sources.contains(MusicSource.SPOTIFY))
        assertTrue(sources.contains(MusicSource.LOCAL))
        assertTrue(sources.contains(MusicSource.PODCAST))
        assertTrue(sources.contains(MusicSource.LASTFM))
    }
}
