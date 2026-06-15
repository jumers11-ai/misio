package com.soundfusion.core.database.entity

import androidx.room.*

@Entity(
    tableName = "queue_items",
    foreignKeys = [
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("track_id")],
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: String,
    val position: Int,
    @ColumnInfo(name = "is_playing") val isPlaying: Boolean = false,
    @ColumnInfo(name = "added_by") val addedBy: String = "USER",
)
