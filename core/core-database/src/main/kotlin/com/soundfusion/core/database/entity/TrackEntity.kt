package com.soundfusion.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.soundfusion.core.database.model.MusicSource

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["source", "source_id"], unique = true),
        Index(value = ["artist"]),
        Index(value = ["album_id"]),
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "album") val albumName: String? = null,
    @ColumnInfo(name = "album_id") val albumId: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String? = null,
    val source: MusicSource,
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "stream_url") val streamUrl: String? = null,
    @ColumnInfo(name = "is_offline") val isOffline: Boolean = false,
    @ColumnInfo(name = "replay_gain") val replayGain: Float? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "play_count") val playCount: Int = 0,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long? = null,
    @ColumnInfo(name = "is_liked") val isLiked: Boolean = false,
)
