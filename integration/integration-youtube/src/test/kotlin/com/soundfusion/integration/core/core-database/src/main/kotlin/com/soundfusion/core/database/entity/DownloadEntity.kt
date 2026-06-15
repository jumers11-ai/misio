package com.soundfusion.core.database.entity

import androidx.room.*

@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("track_id")],
)
data class DownloadEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
)

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, ERROR }
