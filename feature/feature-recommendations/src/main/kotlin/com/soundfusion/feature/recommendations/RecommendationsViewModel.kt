package com.soundfusion.feature.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Mix(
    val id: String,
    val title: String,
    val trackCount: Int,
    val artworkUrl: String?,
    val trackIds: List<String> = emptyList(),
)

data class RecommendationsUiState(
    val mixes: List<Mix> = emptyList(),
    val recommendations: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val audioEngine: AudioEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val mostPlayed = trackDao.observeMostPlayed(50).first()
                val recentlyPlayed = trackDao.observeRecentlyPlayed(30).first()

                val tracks = (mostPlayed + recentlyPlayed)
                    .distinctBy { it.id }
                    .map { entity ->
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
                            isLiked = entity.isLiked,
                            playCount = entity.playCount,
                        )
                    }

                val mixes = listOf(
                    Mix(id = "daily-1", title = "Daily Mix #1", trackCount = tracks.take(25).size, artworkUrl = tracks.firstOrNull()?.artworkUrl),
                    Mix(id = "discover", title = "Discover Weekly", trackCount = tracks.drop(10).take(30).size, artworkUrl = tracks.getOrNull(5)?.artworkUrl),
                )

                _uiState.update {
                    it.copy(
                        mixes = mixes,
                        recommendations = tracks.take(20),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun playMix(mixId: String) {
        viewModelScope.launch {
            val tracks = _uiState.value.recommendations.shuffled()
            if (tracks.isNotEmpty()) {
                audioEngine.playQueue(tracks)
            }
        }
    }

    fun toggleLike(trackId: String) {
        viewModelScope.launch {
            val track = _uiState.value.recommendations.find { it.id == trackId } ?: return@launch
            trackDao.setLiked(trackId, !track.isLiked)
            _uiState.update { state ->
                state.copy(
                    recommendations = state.recommendations.map {
                        if (it.id == trackId) it.copy(isLiked = !it.isLiked) else it
                    }
                )
            }
        }
    }
}
