package com.soundfusion.feature.player

import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.audio.model.PlaybackState
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioEngine: AudioEngine,
) : ViewModel() {

    val currentTrack: StateFlow<Track?> = audioEngine.currentTrack
    val playbackState: StateFlow<PlaybackState> = audioEngine.playbackState
    val positionMs: StateFlow<Long> = audioEngine.positionMs
    val durationMs: StateFlow<Long> = audioEngine.durationMs

    fun togglePlayPause() = audioEngine.togglePlayPause()
    fun skipNext() = audioEngine.skipNext()
    fun skipPrevious() = audioEngine.skipPrevious()
    fun seekTo(positionMs: Long) = audioEngine.seekTo(positionMs)

    fun toggleShuffle() {
        val current = audioEngine.exoPlayer.shuffleModeEnabled
        audioEngine.setShuffleEnabled(!current)
    }

    fun toggleRepeat() {
        val current = audioEngine.exoPlayer.repeatMode
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        audioEngine.setRepeatMode(next)
    }
}
