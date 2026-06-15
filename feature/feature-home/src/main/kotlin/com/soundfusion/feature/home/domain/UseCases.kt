package com.soundfusion.feature.home.domain

import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.Album
import com.soundfusion.core.database.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentlyPlayedUseCase @Inject constructor(
    private val trackDao: TrackDao,
) {
    operator fun invoke(limit: Int = 20): Flow<List<Track>> =
        trackDao.observeRecentlyPlayed(limit).map { entities ->
            entities.map { e ->
                Track(id = e.id, title = e.title, artist = e.artist, durationMs = e.durationMs,
                    artworkUrl = e.artworkUrl, source = e.source, sourceId = e.sourceId,
                    streamUrl = e.streamUrl, isLiked = e.isLiked, playCount = e.playCount)
            }
        }
}

class GetRecommendationsUseCase @Inject constructor(
    private val trackDao: TrackDao,
) {
    operator fun invoke(count: Int = 30): Flow<List<Track>> =
        trackDao.observeMostPlayed(count).map { entities ->
            entities.map { e ->
                Track(id = e.id, title = e.title, artist = e.artist, durationMs = e.durationMs,
                    artworkUrl = e.artworkUrl, source = e.source, sourceId = e.sourceId,
                    streamUrl = e.streamUrl)
            }
        }
}

class GetNewReleasesUseCase @Inject constructor() {
    operator fun invoke(): Flow<List<Album>> = kotlinx.coroutines.flow.flowOf(emptyList())
}
