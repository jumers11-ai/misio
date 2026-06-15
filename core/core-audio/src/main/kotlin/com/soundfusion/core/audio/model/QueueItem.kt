package com.soundfusion.core.audio.model

import com.soundfusion.core.database.model.Track

data class QueueItem(
    val track: Track,
    val position: Int,
    val isPlaying: Boolean = false,
    val addedBy: QueueSource = QueueSource.USER,
)

enum class QueueSource {
    USER,
    AUTO_PLAY,
    SMART_SHUFFLE,
    RECOMMENDATION,
}
