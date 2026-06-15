package com.soundfusion.core.database.entity

import androidx.room.*
import com.soundfusion.core.database.model.MusicSource

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String? = null,
    @ColumnInfo(name = "is_smart") val isSmartPlaylist: Boolean = false,
    val source: MusicSource,
    @ColumnInfo(name = "source_id") val sourceId: String? = null,
    @ColumnInfo(name = "track_count") val trackCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlist_id", "track_id"],
    foreignKeys = [
        ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlist_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class PlaylistTrackCrossRef(
    @ColumnInfo(name = "playlist_id") val playlistId: String,
    @ColumnInfo(name = "track_id") val trackId: String,
    val position: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
)
