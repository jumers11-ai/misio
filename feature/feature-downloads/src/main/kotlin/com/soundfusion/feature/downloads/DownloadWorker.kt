package com.soundfusion.feature.downloads

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.soundfusion.core.database.dao.DownloadDao
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.DownloadEntity
import com.soundfusion.core.database.entity.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao,
    private val trackDao: TrackDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TRACK_ID = "track_id"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: UUID.randomUUID().toString()

        val downloadsDir = File(applicationContext.filesDir, "downloads")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val outputFile = File(downloadsDir, "$trackId.m4a")

        try {
            downloadDao.updateProgress(downloadId, 0f, DownloadStatus.DOWNLOADING)

            val request = Request.Builder().url(streamUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                downloadDao.updateProgress(downloadId, 0f, DownloadStatus.ERROR)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                downloadDao.updateProgress(downloadId, 0f, DownloadStatus.ERROR)
                return@withContext Result.failure()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            FileOutputStream(outputFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            downloadDao.updateProgress(downloadId, downloadedBytes.toFloat() / totalBytes, DownloadStatus.PAUSED)
                            return@withContext Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        downloadDao.updateProgress(downloadId, progress, DownloadStatus.DOWNLOADING)

                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                }
            }

            // Update database
            downloadDao.updateProgress(downloadId, 1f, DownloadStatus.COMPLETED)

            // Mark track as offline
            val track = trackDao.getById(trackId)
            if (track != null) {
                trackDao.update(track.copy(
                    isOffline = true,
                    streamUrl = Uri.fromFile(outputFile).toString(),
                ))
            }

            Result.success(workDataOf(KEY_DOWNLOAD_ID to downloadId))
        } catch (e: Exception) {
            downloadDao.updateProgress(downloadId, 0f, DownloadStatus.ERROR)
            if (outputFile.exists()) outputFile.delete()
            Result.retry()
        }
    }
}
