package com.soundfusion.feature.library

import androidx.lifecycle.ViewModel
import com.soundfusion.core.database.dao.PlaylistDao
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
) : ViewModel() {

    val playlists: Flow<List<Playlist>> = playlistDao.observeAll().map { entities ->
        entities.map { e ->
            Playlist(
                id = e.id,
                name = e.name,
                description = e.description,
                artworkUrl = e.artworkUrl,
                trackCount = e.trackCount,
                source = e.source,
                isSmartPlaylist = e.isSmartPlaylist,
            )
        }
    }

    val likedCount: StateFlow<Int> = MutableStateFlow(0)
}
