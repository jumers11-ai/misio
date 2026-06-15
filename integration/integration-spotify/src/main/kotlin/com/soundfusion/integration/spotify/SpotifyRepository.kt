package com.soundfusion.integration.spotify

import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Playlist
import com.soundfusion.core.database.model.Track
import com.soundfusion.core.network.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepository @Inject constructor(
    private val api: SpotifyApiService,
    private val trackDao: TrackDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun search(query: String): Flow<List<Track>> = flow {
        val response = withContext(ioDispatcher) { api.search(query) }
        val tracks = response.tracks.items.map { item ->
            Track(
                id = "sp:${item.id}",
                title = item.name,
                artist = item.artists.firstOrNull()?.name ?: "Unknown",
                albumName = item.album?.name,
                durationMs = item.durationMs,
                artworkUrl = item.album?.images?.firstOrNull()?.url,
                source = MusicSource.SPOTIFY,
                sourceId = item.id,
            )
        }
        emit(tracks)
    }.flowOn(ioDispatcher)

    fun getSavedTracks(): Flow<List<Track>> = flow {
        val response = withContext(ioDispatcher) { api.getSavedTracks() }
        val tracks = response.items.map { saved ->
            Track(
                id = "sp:${saved.track.id}",
                title = saved.track.name,
                artist = saved.track.artists.firstOrNull()?.name ?: "Unknown",
                durationMs = saved.track.durationMs,
                artworkUrl = saved.track.album?.images?.firstOrNull()?.url,
                source = MusicSource.SPOTIFY,
                sourceId = saved.track.id,
            )
        }
        emit(tracks)
    }.flowOn(ioDispatcher)

    fun getPlaylists(): Flow<List<Playlist>> = flow {
        val response = withContext(ioDispatcher) { api.getPlaylists() }
        val playlists = response.items.map { item ->
            Playlist(
                id = "sp:pl:${item.id}",
                name = item.name,
                description = item.description,
                artworkUrl = item.images.firstOrNull()?.url,
                trackCount = item.tracks.total,
                source = MusicSource.SPOTIFY,
            )
        }
        emit(playlists)
    }.flowOn(ioDispatcher)
}
