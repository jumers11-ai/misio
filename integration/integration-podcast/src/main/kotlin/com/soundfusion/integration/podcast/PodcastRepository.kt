package com.soundfusion.integration.podcast

import com.soundfusion.core.database.model.MusicSource
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
class PodcastRepository @Inject constructor(
    private val rssParser: RssParser,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun getEpisodes(feedUrl: String): Flow<List<Track>> = flow {
        val episodes = withContext(ioDispatcher) { rssParser.parse(feedUrl) }
        val tracks = episodes.map { episode ->
            Track(
                id = "pod:${episode.guid}",
                title = episode.title,
                artist = episode.author,
                durationMs = episode.durationMs,
                artworkUrl = episode.imageUrl,
                source = MusicSource.PODCAST,
                sourceId = episode.guid,
                streamUrl = episode.audioUrl,
            )
        }
        emit(tracks)
    }.flowOn(ioDispatcher)
}

data class PodcastEpisode(
    val guid: String,
    val title: String,
    val author: String,
    val durationMs: Long,
    val imageUrl: String?,
    val audioUrl: String,
)

class RssParser @Inject constructor() {
    suspend fun parse(feedUrl: String): List<PodcastEpisode> {
        // In production: XML parser for RSS/Atom feeds
        return emptyList()
    }
}
