package com.soundfusion.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.Track
import com.soundfusion.integration.youtube.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val results: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val query: String = "",
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val youtubeRepo: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _query
                .debounce(300)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { query ->
                    _uiState.update { it.copy(isSearching = true, query = query) }
                    try {
                        // Search local DB first
                        val localResults = trackDao.search(query)
                        val mapped = localResults.map { entity ->
                            Track(
                                id = entity.id,
                                title = entity.title,
                                artist = entity.artist,
                                durationMs = entity.durationMs,
                                artworkUrl = entity.artworkUrl,
                                source = entity.source,
                                sourceId = entity.sourceId,
                                streamUrl = entity.streamUrl,
                            )
                        }
                        _uiState.update { it.copy(results = mapped, isSearching = false) }

                        // Then fetch from YouTube
                        youtubeRepo.search(query).collect { remoteResults ->
                            val combined = (mapped + remoteResults)
                                .distinctBy { "${it.source}:${it.sourceId}" }
                            _uiState.update { it.copy(results = combined, isSearching = false) }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isSearching = false) }
                    }
                }
        }
    }

    fun search(query: String) { _query.value = query }
    fun clear() { _query.value = ""; _uiState.update { SearchUiState() } }
}
