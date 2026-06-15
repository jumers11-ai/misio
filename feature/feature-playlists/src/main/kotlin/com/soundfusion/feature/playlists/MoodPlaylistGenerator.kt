package com.soundfusion.feature.playlists

import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

enum class Mood(
    val label: String,
    val emoji: String,
    val tempoRange: LongRange,
    val preferLiked: Boolean,
    val preferShort: Boolean,
) {
    CHILL("Relaks", "😌", 60_000L..240_000L, preferLiked = true, preferShort = false),
    ENERGETIC("Energia", "⚡", 120_000L..300_000L, preferLiked = false, preferShort = true),
    FOCUS("Skupienie", "🎯", 180_000L..600_000L, preferLiked = false, preferShort = false),
    HAPPY("Radość", "😊", 120_000L..240_000L, preferLiked = true, preferShort = true),
    MELANCHOLIC("Melancholia", "🌧️", 180_000L..360_000L, preferLiked = true, preferShort = false),
    PARTY("Impreza", "🎉", 120_000L..240_000L, preferLiked = false, preferShort = true),
    SLEEP("Sen", "🌙", 180_000L..480_000L, preferLiked = false, preferShort = false),
    WORKOUT("Trening", "💪", 60_000L..240_000L, preferLiked = false, preferShort = true),
}

data class MoodPlaylist(
    val mood: Mood,
    val tracks: List<Track>,
    val estimatedDurationMs: Long,
)

@Singleton
class MoodPlaylistGenerator @Inject constructor(
    private val trackDao: TrackDao,
) {
    fun generate(mood: Mood, limit: Int = 30): Flow<MoodPlaylist> = flow {
        val allTracks = trackDao.observeAll().first()

        val scored = allTracks.map { track ->
            var score = 0f

            // Duration preference
            if (track.durationMs in mood.tempoRange) {
                score += 20f
            } else {
                val distance = when {
                    track.durationMs < mood.tempoRange.first -> mood.tempoRange.first - track.durationMs
                    track.durationMs > mood.tempoRange.last -> track.durationMs - mood.tempoRange.last
                    else -> 0L
                }
                score -= (distance.toFloat() / 60_000f).coerceAtMost(10f)
            }

            // Like preference
            if (mood.preferLiked && track.isLiked) score += 15f

            // Short preference
            if (mood.preferShort && track.durationMs < 180_000L) score += 10f

            // Play count factor — mix familiar with new
            if (track.playCount > 0) score += 5f
            if (track.playCount == 0) score += 8f // Discovery bonus

            // Randomization factor
            score += (Math.random() * 10).toFloat()

            Pair(track, score)
        }

        val selected = scored
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .let { tracks -> entityListToTracks(tracks) }

        val totalMs = selected.sumOf { it.durationMs }

        emit(MoodPlaylist(mood = mood, tracks = selected, estimatedDurationMs = totalMs))
    }

    fun getAvailableMoods(): List<Mood> = Mood.entries.toList()

    private fun entityListToTracks(entities: List<TrackEntity>): List<Track> =
        entities.map { e ->
            Track(
                id = e.id, title = e.title, artist = e.artist, albumName = e.albumName,
                durationMs = e.durationMs, artworkUrl = e.artworkUrl, source = e.source,
                sourceId = e.sourceId, streamUrl = e.streamUrl, isOffline = e.isOffline,
                isLiked = e.isLiked, playCount = e.playCount,
            )
        }
}
