package com.soundfusion.core.database.converter

import androidx.room.TypeConverter
import com.soundfusion.core.database.entity.DownloadStatus
import com.soundfusion.core.database.model.MusicSource

class Converters {
    @TypeConverter fun fromMusicSource(value: MusicSource): String = value.name
    @TypeConverter fun toMusicSource(value: String): MusicSource = MusicSource.valueOf(value)
    @TypeConverter fun fromDownloadStatus(value: DownloadStatus): String = value.name
    @TypeConverter fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
