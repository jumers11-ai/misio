package com.soundfusion.wearable

import android.content.Context
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.audio.model.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearableDataSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEngine: AudioEngine,
    private val scope: CoroutineScope,
) {
    companion object {
        const val PATH_PLAYBACK_STATE = "/playback/state"
        const val PATH_PLAYBACK_COMMAND = "/playback/command"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ARTWORK = "artwork"
        const val KEY_IS_PLAYING = "is_playing"
        const val KEY_POSITION = "position"
        const val KEY_DURATION = "duration"
        const val CMD_PLAY_PAUSE = "play_pause"
        const val CMD_NEXT = "next"
        const val CMD_PREVIOUS = "previous"
        const val CMD_SEEK = "seek"
    }

    fun startSync() {
        scope.launch {
            combine(
                audioEngine.currentTrack,
                audioEngine.playbackState,
                audioEngine.positionMs,
                audioEngine.durationMs,
            ) { track, state, position, duration ->
                WearPlaybackData(
                    title = track?.title ?: "",
                    artist = track?.artist ?: "",
                    artworkUrl = track?.artworkUrl,
                    isPlaying = state == PlaybackState.PLAYING,
                    positionMs = position,
                    durationMs = duration,
                )
            }.collect { data ->
                sendToWearable(data)
            }
        }
    }

    fun handleCommand(command: String, extras: Map<String, Any> = emptyMap()) {
        when (command) {
            CMD_PLAY_PAUSE -> audioEngine.togglePlayPause()
            CMD_NEXT -> audioEngine.skipNext()
            CMD_PREVIOUS -> audioEngine.skipPrevious()
            CMD_SEEK -> {
                val position = (extras["position"] as? Long) ?: return
                audioEngine.seekTo(position)
            }
        }
    }

    private fun sendToWearable(data: WearPlaybackData) {
        // In production: use Wearable.DataApi or MessageApi
        // val dataMap = PutDataMapRequest.create(PATH_PLAYBACK_STATE).run {
        //     dataMap.putString(KEY_TITLE, data.title)
        //     dataMap.putString(KEY_ARTIST, data.artist)
        //     dataMap.putBoolean(KEY_IS_PLAYING, data.isPlaying)
        //     dataMap.putLong(KEY_POSITION, data.positionMs)
        //     dataMap.putLong(KEY_DURATION, data.durationMs)
        //     asPutDataRequest().setUrgent()
        // }
        // Wearable.getDataClient(context).putDataItem(dataMap)
    }

    data class WearPlaybackData(
        val title: String,
        val artist: String,
        val artworkUrl: String?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
    )
}
