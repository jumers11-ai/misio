package com.soundfusion.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val audioEngine: AudioEngine,
    private val lyricsManager: LyricsManager,
) : ViewModel() {

    val currentTrack: StateFlow<Track?> = audioEngine.currentTrack

    private val _lyrics = MutableStateFlow<SyncedLyrics?>(null)
    val lyrics: StateFlow<SyncedLyrics?> = _lyrics.asStateFlow()

    private val _activeLineIndex = MutableStateFlow(-1)
    val activeLineIndex: StateFlow<Int> = _activeLineIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeTrackChanges()
        observePosition()
    }

    private fun observeTrackChanges() {
        viewModelScope.launch {
            audioEngine.currentTrack.collect { track ->
                if (track != null) {
                    loadLyrics(track)
                } else {
                    _lyrics.value = null
                    _activeLineIndex.value = -1
                }
            }
        }
    }

    private fun loadLyrics(track: Track) {
        viewModelScope.launch {
            _isLoading.value = true
            _lyrics.value = null
            _activeLineIndex.value = -1

            lyricsManager.getLyrics(track).collect { synced ->
                _lyrics.value = synced
                _isLoading.value = false
            }
        }
    }

    private fun observePosition() {
        viewModelScope.launch {
            while (true) {
                val posMs = audioEngine.positionMs.value
                val lines = _lyrics.value?.lines
                if (lines != null) {
                    _activeLineIndex.value = lyricsManager.getActiveLine(lines, posMs)
                }
                delay(100)
            }
        }
    }

    fun seekToLine(lineIndex: Int) {
        val lines = _lyrics.value?.lines ?: return
        val line = lines.getOrNull(lineIndex) ?: return
        audioEngine.seekTo(line.timeMs)
    }
}
