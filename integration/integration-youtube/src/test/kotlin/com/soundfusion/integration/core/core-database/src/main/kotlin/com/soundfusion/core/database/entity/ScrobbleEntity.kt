package com.soundfusion.core.database.entity

import androidx.room.*

@Entity(
    tableName = "scrobbles",
    foreignKeys = [
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("track_id"), Index("timestamp")],
)
data class ScrobbleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: String,
    val timestamp: Long,
    @ColumnInfo(name = "played_duration_ms") val playedDurationMs: Long,
    @ColumnInfo(name = "synced_to_lastfm") val syncedToLastFm: Boolean = false,
)
