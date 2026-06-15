package com.soundfusion.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.soundfusion.core.database.model.MusicSource

@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artist_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "artist_id") val artistId: String,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String? = null,
    @ColumnInfo(name = "release_year") val releaseYear: Int? = null,
    @ColumnInfo(name = "track_count") val trackCount: Int = 0,
    val source: MusicSource,
    @ColumnInfo(name = "source_id") val sourceId: String,
)
