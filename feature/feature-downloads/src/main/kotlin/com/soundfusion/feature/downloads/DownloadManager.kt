package com.soundfusion.feature.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.soundfusion.core.database.dao.DownloadDao
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.DownloadEntity
import com.soundfusion.core.database.entity.DownloadStatus
import com.soundfusion.core.database.model.Track
import com.soundfusion.core.network.monitor.ConnectivityMonitor
import com.soundfusion.core.storage.AudioPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val trackDao: TrackDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val preferences: AudioPreferences,
) {
    private val workManager = WorkManager.getInstance(context)

    fun observeDownloads(): Flow<List<DownloadEntity>> = downloadDao.observeAll()
    fun observeActive(): Flow<List<DownloadEntity>> = downloadDao.observeActive()

    suspend fun enqueueDownload(track: Track) {
        val streamUrl = track.streamUrl ?: return
        val downloadId = UUID.randomUUID().toString()

        // Create download entity
        val entity = DownloadEntity(
            id = downloadId,
            trackId = track.id,
            filePath = File(context.filesDir, "downloads/${track.id}.m4a").absolutePath,
            fileSize = 0,
            status = DownloadStatus.QUEUED,
            progress = 0f,
        )
        downloadDao.insert(entity)

        // Create work request
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                DownloadWorker.KEY_TRACK_ID to track.id,
                DownloadWorker.KEY_STREAM_URL to streamUrl,
                DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
            ))
            .addTag("download_${track.id}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${track.id}",
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
    }

    suspend fun cancelDownload(downloadId: String) {
        val download = downloadDao.observeAll() // would need getById
        workManager.cancelUniqueWork("download_$downloadId")
    }

    suspend fun removeDownload(downloadId: String, trackId: String) {
        // Delete file
        val file = File(context.filesDir, "downloads/$trackId.m4a")
        if (file.exists()) file.delete()

        // Update database
        val entity = DownloadEntity(id = downloadId, trackId = trackId, filePath = "")
        downloadDao.delete(entity)

        // Unmark offline
        val track = trackDao.getById(trackId)
        if (track != null) {
            trackDao.update(track.copy(isOffline = false))
        }
    }

    fun getStorageUsed(): Long {
        val dir = File(context.filesDir, "downloads")
        return if (dir.exists()) dir.walkTopDown().sumOf { it.length() } else 0L
    }

    fun getStorageAvailable(): Long {
        val stat = android.os.StatFs(context.filesDir.absolutePath)
        return stat.availableBytes
    }
}
