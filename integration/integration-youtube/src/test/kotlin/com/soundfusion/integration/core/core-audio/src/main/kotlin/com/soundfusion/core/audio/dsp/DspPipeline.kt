package com.soundfusion.core.audio.dsp

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.exoplayer.ExoPlayer
import com.soundfusion.core.storage.AudioPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class EqBand(
    val index: Int,
    val frequencyHz: Int,
    val minLevel: Short,
    val maxLevel: Short,
    val currentLevel: Short,
)

@Singleton
class DspPipeline @Inject constructor(
    private val preferences: AudioPreferences,
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _eqBands = MutableStateFlow<List<EqBand>>(emptyList())
    val eqBands: StateFlow<List<EqBand>> = _eqBands.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0)
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    fun attachTo(player: ExoPlayer) {
        val sessionId = player.audioSessionId
        initEqualizer(sessionId)
        initBassBoost(sessionId)
        initLoudnessEnhancer(sessionId)
    }

    private fun initEqualizer(sessionId: Int) {
        equalizer?.release()
        equalizer = Equalizer(0, sessionId).apply {
            enabled = preferences.eqEnabled
            val savedPreset = preferences.eqBandLevels
            val bands = (0 until numberOfBands).map { band ->
                val level = savedPreset.getOrNull(band.toInt()) ?: getBandLevel(band)
                setBandLevel(band, level)
                EqBand(
                    index = band.toInt(),
                    frequencyHz = getCenterFreq(band) / 1000,
                    minLevel = bandLevelRange[0],
                    maxLevel = bandLevelRange[1],
                    currentLevel = level,
                )
            }
            _eqBands.value = bands
        }
    }

    private fun initBassBoost(sessionId: Int) {
        bassBoost?.release()
        bassBoost = BassBoost(0, sessionId).apply {
            enabled = preferences.bassBoostEnabled
            val strength = preferences.bassBoostStrength.toShort()
            setStrength(strength)
            _bassBoostStrength.value = strength.toInt()
        }
    }

    private fun initLoudnessEnhancer(sessionId: Int) {
        loudnessEnhancer?.release()
        loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
            enabled = preferences.loudnessNormEnabled
            setTargetGain(preferences.targetGainMb)
        }
    }

    fun setEqBandLevel(bandIndex: Int, level: Short) {
        equalizer?.setBandLevel(bandIndex.toShort(), level)
        _eqBands.update { bands ->
            bands.map { if (it.index == bandIndex) it.copy(currentLevel = level) else it }
        }
        preferences.eqBandLevels = _eqBands.value.map { it.currentLevel }
    }

    fun setEqEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        preferences.eqEnabled = enabled
    }

    fun setBassBoost(strength: Int) {
        bassBoost?.setStrength(strength.toShort())
        _bassBoostStrength.value = strength
        preferences.bassBoostStrength = strength
    }

    fun setLoudnessNorm(enabled: Boolean, targetMb: Int = -1400) {
        loudnessEnhancer?.enabled = enabled
        if (enabled) loudnessEnhancer?.setTargetGain(targetMb)
        preferences.loudnessNormEnabled = enabled
        preferences.targetGainMb = targetMb
    }

    fun release() {
        equalizer?.release(); equalizer = null
        bassBoost?.release(); bassBoost = null
        loudnessEnhancer?.release(); loudnessEnhancer = null
    }
}
