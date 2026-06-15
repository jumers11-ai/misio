package com.soundfusion.integration.youtube

import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Playlist
import com.soundfusion.core.database.model.Track
import com.soundfusion.core.network.CacheManager
import com.soundfusion.core.network.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class YouTubeRepository @Inject constructor(
    private val extractor: YouTubeExtractorWrapper,
    private val trackDao: TrackDao,
    private val cacheManager: CacheManager,
    private val mapper: YouTubeMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun search(query: String): Flow<List<Track>> = flow {
        // L1: Check cache
        val cached = cacheManager.getSearchResults(MusicSource.YOUTUBE, query)
        if (cached != null) emit(cached)

        // L2: Network fetch
        val results = withContext(ioDispatcher) {
            extractor.search(query)
        }
        val tracks = results.map { mapper.toTrack(it) }
        emit(tracks)

        // Save to cache
        cacheManager.cacheSearchResults(MusicSource.YOUTUBE, query, tracks, ttl = 15.minutes)

        // Persist to DB
        trackDao.insertAll(tracks.map { mapper.toEntity(it) })
    }.flowOn(ioDispatcher)

    fun getStreamUrl(trackId: String): Flow<String> = flow {
        val cached = trackDao.getById(trackId)?.streamUrl
        if (cached != null) {
            emit(cached)
            return@flow
        }

        val url = withContext(ioDispatcher) {
            extractor.getStreamUrl(trackId)
        }
        trackDao.updateStreamUrl(trackId, url)
        emit(url)
    }.flowOn(ioDispatcher)

    fun getTrending(): Flow<List<Track>> = flow {
        val results = withContext(ioDispatcher) {
            extractor.getTrending()
        }
        emit(results.map { mapper.toTrack(it) })
    }.flowOn(ioDispatcher)

    fun getPlaylists(): Flow<List<Playlist>> = flow {
        val results = withContext(ioDispatcher) {
            extractor.getUserPlaylists()
        }
        emit(results.map { mapper.toPlaylist(it) })
    }.flowOn(ioDispatcher)
}
