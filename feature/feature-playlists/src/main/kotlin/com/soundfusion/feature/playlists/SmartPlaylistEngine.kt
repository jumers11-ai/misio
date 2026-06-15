package com.soundfusion.feature.playlists

import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
sealed class SmartRule {
    abstract fun matches(track: TrackEntity): Boolean

    @Serializable
    data class SourceRule(val source: MusicSource) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean = track.source == source
    }

    @Serializable
    data class LikedRule(val isLiked: Boolean = true) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean = track.isLiked == isLiked
    }

    @Serializable
    data class PlayCountRule(val minPlays: Int = 0, val maxPlays: Int = Int.MAX_VALUE) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean =
            track.playCount in minPlays..maxPlays
    }

    @Serializable
    data class RecentlyAddedRule(val withinDays: Int = 30) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean {
            val cutoff = System.currentTimeMillis() - (withinDays.toLong() * 24 * 60 * 60 * 1000)
            return track.addedAt >= cutoff
        }
    }

    @Serializable
    data class RecentlyPlayedRule(val withinDays: Int = 7) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean {
            val cutoff = System.currentTimeMillis() - (withinDays.toLong() * 24 * 60 * 60 * 1000)
            return (track.lastPlayedAt ?: 0L) >= cutoff
        }
    }

    @Serializable
    data class NeverPlayedRule(val unused: Boolean = true) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean = track.playCount == 0
    }

    @Serializable
    data class OfflineOnlyRule(val unused: Boolean = true) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean = track.isOffline
    }

    @Serializable
    data class DurationRule(val minMs: Long = 0, val maxMs: Long = Long.MAX_VALUE) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean =
            track.durationMs in minMs..maxMs
    }

    @Serializable
    data class ArtistContainsRule(val query: String) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean =
            track.artist.contains(query, ignoreCase = true)
    }

    @Serializable
    data class TitleContainsRule(val query: String) : SmartRule() {
        override fun matches(track: TrackEntity): Boolean =
            track.title.contains(query, ignoreCase = true)
    }
}

@Serializable
data class SmartPlaylistDefinition(
    val name: String,
    val rules: List<SmartRule>,
    val matchAll: Boolean = true,
    val sortBy: SortOption = SortOption.ADDED_DESC,
    val limit: Int = 100,
)

@Serializable
enum class SortOption {
    TITLE_ASC, TITLE_DESC,
    ARTIST_ASC, ARTIST_DESC,
    ADDED_ASC, ADDED_DESC,
    PLAY_COUNT_ASC, PLAY_COUNT_DESC,
    DURATION_ASC, DURATION_DESC,
    LAST_PLAYED_DESC,
    RANDOM,
}

@Singleton
class SmartPlaylistEngine @Inject constructor(
    private val trackDao: TrackDao,
    private val json: Json,
) {
    fun resolve(definition: SmartPlaylistDefinition): Flow<List<Track>> {
        return trackDao.observeAll().map { allTracks ->
            val filtered = allTracks.filter { track ->
                if (definition.matchAll) {
                    definition.rules.all { it.matches(track) }
                } else {
                    definition.rules.any { it.matches(track) }
                }
            }

            val sorted = when (definition.sortBy) {
                SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
                SortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
                SortOption.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
                SortOption.ARTIST_DESC -> filtered.sortedByDescending { it.artist.lowercase() }
                SortOption.ADDED_ASC -> filtered.sortedBy { it.addedAt }
                SortOption.ADDED_DESC -> filtered.sortedByDescending { it.addedAt }
                SortOption.PLAY_COUNT_ASC -> filtered.sortedBy { it.playCount }
                SortOption.PLAY_COUNT_DESC -> filtered.sortedByDescending { it.playCount }
                SortOption.DURATION_ASC -> filtered.sortedBy { it.durationMs }
                SortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
                SortOption.LAST_PLAYED_DESC -> filtered.sortedByDescending { it.lastPlayedAt ?: 0L }
                SortOption.RANDOM -> filtered.shuffled()
            }

            sorted.take(definition.limit).map { entity ->
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
        }
    }

    fun serializeDefinition(definition: SmartPlaylistDefinition): String =
        json.encodeToString(SmartPlaylistDefinition.serializer(), definition)

    fun deserializeDefinition(jsonString: String): SmartPlaylistDefinition =
        json.decodeFromString(SmartPlaylistDefinition.serializer(), jsonString)

    fun createPresets(): List<SmartPlaylistDefinition> = listOf(
        SmartPlaylistDefinition(
            name = "Ulubione",
            rules = listOf(SmartRule.LikedRule()),
            sortBy = SortOption.ADDED_DESC,
        ),
        SmartPlaylistDefinition(
            name = "Ostatnio dodane",
            rules = listOf(SmartRule.RecentlyAddedRule(withinDays = 14)),
            sortBy = SortOption.ADDED_DESC,
            limit = 50,
        ),
        SmartPlaylistDefinition(
            name = "Most Played",
            rules = listOf(SmartRule.PlayCountRule(minPlays = 5)),
            sortBy = SortOption.PLAY_COUNT_DESC,
            limit = 100,
        ),
        SmartPlaylistDefinition(
            name = "Discover",
            rules = listOf(SmartRule.NeverPlayedRule()),
            sortBy = SortOption.RANDOM,
            limit = 50,
        ),
        SmartPlaylistDefinition(
            name = "Offline Ready",
            rules = listOf(SmartRule.OfflineOnlyRule()),
            sortBy = SortOption.TITLE_ASC,
        ),
        SmartPlaylistDefinition(
            name = "Krótkie (<3min)",
            rules = listOf(SmartRule.DurationRule(maxMs = 180_000L)),
            sortBy = SortOption.DURATION_ASC,
        ),
        SmartPlaylistDefinition(
            name = "YouTube Only",
            rules = listOf(SmartRule.SourceRule(MusicSource.YOUTUBE)),
            sortBy = SortOption.ADDED_DESC,
        ),
        SmartPlaylistDefinition(
            name = "Spotify Only",
            rules = listOf(SmartRule.SourceRule(MusicSource.SPOTIFY)),
            sortBy = SortOption.ADDED_DESC,
        ),
    )
}
