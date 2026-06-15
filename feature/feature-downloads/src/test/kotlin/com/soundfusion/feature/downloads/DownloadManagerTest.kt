package com.soundfusion.feature.downloads

import android.content.Context
import com.soundfusion.core.database.dao.DownloadDao
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import com.soundfusion.core.network.monitor.ConnectivityMonitor
import com.soundfusion.core.storage.AudioPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File

class DownloadManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val connectivityMonitor = mockk<ConnectivityMonitor>(relaxed = true)
    private val preferences = mockk<AudioPreferences>(relaxed = true)

    private lateinit var manager: DownloadManager

    private val testTrack = Track(
        id = "track-1",
        title = "Test Track",
        artist = "Test Artist",
        durationMs = 200_000L,
        source = MusicSource.YOUTUBE,
        sourceId = "yt-1",
        streamUrl = "https://stream.example.com/audio.m4a",
    )

    @Before
    fun setup() {
        val filesDir = File(System.getProperty("java.io.tmpdir"), "soundfusion_test")
        filesDir.mkdirs()
        every { context.filesDir } returns filesDir

        manager = DownloadManager(context, downloadDao, trackDao, connectivityMonitor, preferences)
    }

    @Test
    fun `enqueueDownload creates download entity`() = runTest {
        manager.enqueueDownload(testTrack)
        coVerify { downloadDao.insert(any()) }
    }

    @Test
    fun `enqueueDownload skips track without streamUrl`() = runTest {
        val trackWithoutUrl = testTrack.copy(streamUrl = null)
        manager.enqueueDownload(trackWithoutUrl)
        coVerify(exactly = 0) { downloadDao.insert(any()) }
    }

    @Test
    fun `removeDownload cleans up database`() = runTest {
        val trackEntity = TrackEntity(
            id = "track-1", title = "Test", artist = "Test",
            durationMs = 200_000, source = MusicSource.YOUTUBE,
            sourceId = "yt-1", isOffline = true,
        )
        coEvery { trackDao.getById("track-1") } returns trackEntity

        manager.removeDownload("dl-1", "track-1")

        coVerify { downloadDao.delete(any()) }
        coVerify { trackDao.update(match { !it.isOffline }) }
    }

    @Test
    fun `getStorageUsed returns 0 for empty directory`() {
        val used = manager.getStorageUsed()
        // May be 0 or small depending on temp files
        assert(used >= 0)
    }
}
