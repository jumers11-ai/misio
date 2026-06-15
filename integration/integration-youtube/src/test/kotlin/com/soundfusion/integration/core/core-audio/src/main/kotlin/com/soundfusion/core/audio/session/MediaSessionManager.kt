package com.soundfusion.core.audio.session

import com.soundfusion.core.database.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionManager @Inject constructor() {
    fun updateSession(track: Track) {
        // Update MediaSession metadata with track info
    }
}
