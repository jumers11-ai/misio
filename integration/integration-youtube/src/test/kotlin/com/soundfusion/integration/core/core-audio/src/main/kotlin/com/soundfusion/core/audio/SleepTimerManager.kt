package com.soundfusion.core.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor(
    private val audioEngine: AudioEngine,
    private val scope: CoroutineScope,
) {
    private var timerJob: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    var fadeOutDurationMs: Long = 30_000L
    var finishCurrentTrack: Boolean = true

    fun start(durationMs: Long) {
        cancel()
        _isActive.value = true
        _remainingMs.value = durationMs

        timerJob = scope.launch {
            val tickInterval = 1000L
            var remaining = durationMs

            while (remaining > 0) {
                delay(tickInterval)
                remaining -= tickInterval
                _remainingMs.value = remaining.coerceAtLeast(0)

                // Start fade out
                if (remaining <= fadeOutDurationMs && remaining > 0) {
                    val volume = remaining.toFloat() / fadeOutDurationMs
                    audioEngine.exoPlayer.volume = volume.coerceIn(0f, 1f)
                }
            }

            // Timer expired
            if (finishCurrentTrack) {
                // Wait for current track to end, but don't start next
                audioEngine.exoPlayer.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
            } else {
                audioEngine.exoPlayer.pause()
            }

            audioEngine.exoPlayer.volume = 1f
            _isActive.value = false
            _remainingMs.value = 0
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _isActive.value = false
        _remainingMs.value = 0
        audioEngine.exoPlayer.volume = 1f
    }

    fun addTime(extraMs: Long) {
        _remainingMs.value += extraMs
    }
}
