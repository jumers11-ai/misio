package com.soundfusion.feature.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.database.dao.PlaylistDao
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlistId: String = "",
    val playlistName: String = "",
    val description: String? = null,
    val artworkUrl: String? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val currentlyPlayingId: String? = null,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
    private val audioEngine: AudioEngine,
) : ViewModel() {

    private val playlistId: String = savedStateHandle["playlistId"] ?: ""

    private val _uiState = MutableStateFlow(PlaylistUiState(playlistId = playlistId))
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
        observeCurrentTrack()
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            val playlist = playlistDao.getById(playlistId)
            if (playlist != null) {
                _uiState.update {
                    it.copy(
                        playlistName = playlist.name,
                        description = playlist.description,
                        artworkUrl = playlist.artworkUrl,
                    )
                }
            }

            playlistDao.observeTracksForPlaylist(playlistId).collect { trackEntities ->
                val tracks = trackEntities.map { entity ->
                    Track(
                        id = entity.id,
                        title = entity.title,
                        artist = entity.artist,
                        albumName = entity.albumName,
                        durationMs = entity.durationMs,
                        artworkUrl = entity.artworkUrl,
                        source = entity.source,
                        sourceId = entity.sourceId,
                        streamUrl = entity.streamUrl,
                        isOffline = entity.isOffline,
                        isLiked = entity.isLiked,
                        playCount = entity.playCount,
                    )
                }
                _uiState.update { it.copy(tracks = tracks, isLoading = false) }
            }
        }
    }

    private fun observeCurrentTrack() {
        viewModelScope.launch {
            audioEngine.currentTrack.collect { track ->
                _uiState.update { it.copy(currentlyPlayingId = track?.id) }
            }
        }
    }

    fun playAll() {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            audioEngine.playQueue(tracks, startIndex = 0)
        }
    }

    fun shuffleAll() {
        val tracks = _uiState.value.tracks.shuffled()
        if (tracks.isNotEmpty()) {
            audioEngine.playQueue(tracks, startIndex = 0)
        }
    }

    fun removeTrack(trackId: String) {
        viewModelScope.launch {
            playlistDao.removeTrackFromPlaylist(playlistId, trackId)
            playlistDao.refreshTrackCount(playlistId)
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            val playlist = playlistDao.getById(playlistId) ?: return@launch
            playlistDao.delete(playlist)
        }
    }
}
