package com.soundfusion.core.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.soundfusion.core.audio.dsp.DspPipeline
import com.soundfusion.core.audio.session.MediaSessionManager
import com.soundfusion.core.audio.focus.AudioFocusHandler
import com.soundfusion.core.audio.model.PlaybackState
import com.soundfusion.core.audio.model.QueueItem
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dspPipeline: DspPipeline,
    private val mediaSessionManager: MediaSessionManager,
    private val audioFocusHandler: AudioFocusHandler,
    private val scope: CoroutineScope,
) {
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                addListener(playerListener)
            }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = when (state) {
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> if (exoPlayer.playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                Player.STATE_ENDED -> PlaybackState.ENDED
                else -> PlaybackState.IDLE
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = exoPlayer.currentMediaItemIndex
            _queue.update { queue ->
                queue.mapIndexed { i, item -> item.copy(isPlaying = i == index) }
            }
            _currentTrack.value = _queue.value.getOrNull(index)?.track
            _currentTrack.value?.let { mediaSessionManager.updateSession(it) }
        }
    }

    fun play(track: Track) {
        scope.launch {
            _currentTrack.value = track
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.streamUrl)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            audioFocusHandler.requestFocus()
            dspPipeline.attachTo(exoPlayer)
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        val items = tracks.map { track ->
            MediaItem.Builder().setMediaId(track.id).setUri(track.streamUrl).build()
        }
        _queue.value = tracks.mapIndexed { i, t ->
            QueueItem(track = t, position = i, isPlaying = i == startIndex)
        }
        exoPlayer.setMediaItems(items, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        audioFocusHandler.requestFocus()
        dspPipeline.attachTo(exoPlayer)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)
    fun skipNext() = exoPlayer.seekToNextMediaItem()
    fun skipPrevious() = exoPlayer.seekToPreviousMediaItem()

    fun setShuffleEnabled(enabled: Boolean) { exoPlayer.shuffleModeEnabled = enabled }
    fun setRepeatMode(mode: Int) { exoPlayer.repeatMode = mode }

    fun release() {
        exoPlayer.release()
        dspPipeline.release()
        audioFocusHandler.abandonFocus()
    }
}
