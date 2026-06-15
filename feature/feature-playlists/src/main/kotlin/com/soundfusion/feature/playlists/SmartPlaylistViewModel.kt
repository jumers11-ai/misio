package com.soundfusion.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartPlaylistUiState(
    val activeRules: List<SmartRule> = emptyList(),
    val matchedTracks: List<Track> = emptyList(),
    val presetNames: List<String> = emptyList(),
    val activePreset: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class SmartPlaylistViewModel @Inject constructor(
    private val smartEngine: SmartPlaylistEngine,
    private val audioEngine: AudioEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartPlaylistUiState())
    val uiState: StateFlow<SmartPlaylistUiState> = _uiState.asStateFlow()

    private var currentDefinition = SmartPlaylistDefinition(
        name = "Custom",
        rules = emptyList(),
        sortBy = SortOption.ADDED_DESC,
        limit = 200,
    )

    init {
        val presets = smartEngine.createPresets()
        _uiState.update { it.copy(presetNames = presets.map { p -> p.name }) }
        resolvePlaylist()
    }

    fun addRule(rule: SmartRule) {
        currentDefinition = currentDefinition.copy(
            rules = currentDefinition.rules + rule,
        )
        _uiState.update {
            it.copy(activeRules = currentDefinition.rules, activePreset = null)
        }
        resolvePlaylist()
    }

    fun removeRule(rule: SmartRule) {
        currentDefinition = currentDefinition.copy(
            rules = currentDefinition.rules - rule,
        )
        _uiState.update {
            it.copy(activeRules = currentDefinition.rules, activePreset = null)
        }
        resolvePlaylist()
    }

    fun applyPreset(name: String) {
        val preset = smartEngine.createPresets().find { it.name == name } ?: return
        currentDefinition = preset
        _uiState.update {
            it.copy(activeRules = preset.rules, activePreset = name)
        }
        resolvePlaylist()
    }

    fun playAll() {
        val tracks = _uiState.value.matchedTracks
        if (tracks.isNotEmpty()) {
            audioEngine.playQueue(tracks)
        }
    }

    private fun resolvePlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            smartEngine.resolve(currentDefinition).collect { tracks ->
                _uiState.update { it.copy(matchedTracks = tracks, isLoading = false) }
            }
        }
    }
}
