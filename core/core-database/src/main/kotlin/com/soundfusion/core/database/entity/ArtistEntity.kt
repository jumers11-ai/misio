package com.soundfusion.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.soundfusion.core.database.model.MusicSource

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    val source: MusicSource,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val genres: String = "",
    val followers: Int? = null,
)
