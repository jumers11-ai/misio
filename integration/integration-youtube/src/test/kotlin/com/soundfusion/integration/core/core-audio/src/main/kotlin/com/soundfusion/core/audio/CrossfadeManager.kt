package com.soundfusion.core.audio

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.soundfusion.core.storage.AudioPreferences
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
class CrossfadeManager @Inject constructor(
    private val preferences: AudioPreferences,
    private val scope: CoroutineScope,
) {
    private var fadeJob: Job? = null
    private var secondaryPlayer: ExoPlayer? = null

    private val _crossfadeDurationMs = MutableStateFlow(0L)
    val crossfadeDurationMs: StateFlow<Long> = _crossfadeDurationMs.asStateFlow()

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    init {
        _crossfadeDurationMs.value = preferences.crossfadeDurationMs
    }

    fun setCrossfadeDuration(durationMs: Long) {
        _crossfadeDurationMs.value = durationMs
        preferences.crossfadeDurationMs = durationMs
    }

    fun shouldCrossfade(): Boolean = _crossfadeDurationMs.value > 0

    fun getPrerollMs(): Long = _crossfadeDurationMs.value

    fun performCrossfade(outgoing: ExoPlayer, incoming: ExoPlayer) {
        val duration = _crossfadeDurationMs.value
        if (duration <= 0) return

        fadeJob?.cancel()
        _isTransitioning.value = true

        fadeJob = scope.launch {
            val steps = (duration / 50).toInt().coerceAtLeast(1)
            val stepDelay = duration / steps

            incoming.volume = 0f
            incoming.play()

            for (step in 0..steps) {
                val progress = step.toFloat() / steps
                val fadeOutVolume = (1f - progress).coerceIn(0f, 1f)
                val fadeInVolume = progress.coerceIn(0f, 1f)

                outgoing.volume = fadeOutVolume * fadeOutCurve(progress)
                incoming.volume = fadeInVolume * fadeInCurve(progress)

                delay(stepDelay)
            }

            outgoing.volume = 0f
            outgoing.pause()
            incoming.volume = 1f
            _isTransitioning.value = false
        }
    }

    fun cancelTransition() {
        fadeJob?.cancel()
        _isTransitioning.value = false
    }

    private fun fadeOutCurve(t: Float): Float {
        // Equal-power crossfade curve
        return kotlin.math.cos(t * Math.PI.toFloat() / 2f)
    }

    private fun fadeInCurve(t: Float): Float {
        return kotlin.math.sin(t * Math.PI.toFloat() / 2f)
    }

    fun attachGaplessListener(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    // Gapless transition happened automatically
                }
            }
        })
    }
}
