package com.soundfusion.feature.player

import androidx.lifecycle.ViewModel
import com.soundfusion.core.audio.dsp.DspPipeline
import com.soundfusion.core.audio.dsp.EqBand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val dspPipeline: DspPipeline,
) : ViewModel() {

    val eqBands: StateFlow<List<EqBand>> = dspPipeline.eqBands
    val bassBoostStrength: StateFlow<Int> = dspPipeline.bassBoostStrength

    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    fun setBandLevel(bandIndex: Int, level: Short) {
        dspPipeline.setEqBandLevel(bandIndex, level)
    }

    fun setBassBoost(strength: Int) {
        dspPipeline.setBassBoost(strength)
    }

    fun setEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        dspPipeline.setEqEnabled(enabled)
    }
}
