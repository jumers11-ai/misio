package com.soundfusion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.database.model.Album
import com.soundfusion.core.database.model.Track
import com.soundfusion.feature.home.domain.GetNewReleasesUseCase
import com.soundfusion.feature.home.domain.GetRecentlyPlayedUseCase
import com.soundfusion.feature.home.domain.GetRecommendationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentlyPlayed: List<Track> = emptyList(),
    val recommendations: List<Track> = emptyList(),
    val newReleases: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentlyPlayed: GetRecentlyPlayedUseCase,
    private val getRecommendations: GetRecommendationsUseCase,
    private val getNewReleases: GetNewReleasesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val recentDef = async { getRecentlyPlayed(limit = 20).first() }
                val recsDef = async { getRecommendations(count = 30).first() }
                val releasesDef = async { getNewReleases().first() }

                _uiState.update {
                    it.copy(
                        recentlyPlayed = recentDef.await(),
                        recommendations = recsDef.await(),
                        newReleases = releasesDef.await(),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refresh() = loadData()
}
