package com.soundfusion.feature.recommendations

import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class TasteProfile(
    val topArtists: Map<String, Float>,
    val topSources: Map<MusicSource, Float>,
    val avgDurationMs: Long,
    val preferredTimeOfDay: Int,
    val totalListeningMs: Long,
    val diversityScore: Float,
)

data class TrackScore(
    val track: Track,
    val score: Float,
    val reason: String,
)

@Singleton
class TasteEngine @Inject constructor(
    private val trackDao: TrackDao,
) {
    suspend fun buildProfile(): TasteProfile {
        val tracks = trackDao.observeAll().first()
        val played = tracks.filter { it.playCount > 0 }

        val artistCounts = played.groupBy { it.artist }
            .mapValues { (_, tracks) -> tracks.sumOf { it.playCount }.toFloat() }
        val totalPlays = artistCounts.values.sum().coerceAtLeast(1f)
        val topArtists = artistCounts.mapValues { it.value / totalPlays }
            .entries.sortedByDescending { it.value }
            .take(20)
            .associate { it.key to it.value }

        val sourceCounts = played.groupBy { it.source }
            .mapValues { (_, tracks) -> tracks.size.toFloat() }
        val totalSourceTracks = sourceCounts.values.sum().coerceAtLeast(1f)
        val topSources = sourceCounts.mapValues { it.value / totalSourceTracks }

        val avgDuration = if (played.isNotEmpty()) played.map { it.durationMs }.average().toLong() else 200_000L
        val totalListening = played.sumOf { it.durationMs * it.playCount }

        val uniqueArtists = played.map { it.artist }.distinct().size
        val diversity = if (played.isNotEmpty()) uniqueArtists.toFloat() / played.size else 0f

        return TasteProfile(
            topArtists = topArtists,
            topSources = topSources,
            avgDurationMs = avgDuration,
            preferredTimeOfDay = 20,
            totalListeningMs = totalListening,
            diversityScore = diversity,
        )
    }

    fun generateRecommendations(profile: TasteProfile, limit: Int = 30): Flow<List<TrackScore>> = flow {
        val allTracks = trackDao.observeAll().first()

        val scored = allTracks.map { track ->
            var score = 0f
            var reason = ""

            val artistWeight = profile.topArtists[track.artist] ?: 0f
            if (artistWeight > 0) {
                score += artistWeight * 40f
                reason = "Lubisz ${track.artist}"
            }

            val sourceWeight = profile.topSources[track.source] ?: 0f
            score += sourceWeight * 10f

            val durationDiff = kotlin.math.abs(track.durationMs - profile.avgDurationMs).toFloat()
            val durationScore = (1f - (durationDiff / 600_000f).coerceAtMost(1f)) * 5f
            score += durationScore

            if (track.playCount == 0) {
                score += 15f
                if (reason.isEmpty()) reason = "Jeszcze nie słuchane"
            }

            if (track.isLiked) {
                score += 10f
            }

            val recencyBonus = if (track.addedAt > System.currentTimeMillis() - 7 * 24 * 3600_000L) 8f else 0f
            score += recencyBonus
            if (recencyBonus > 0 && reason.isEmpty()) reason = "Niedawno dodane"

            if (reason.isEmpty()) reason = "Na podstawie Twojego gustu"

            TrackScore(
                track = entityToTrack(track),
                score = score,
                reason = reason,
            )
        }

        val sorted = scored.sortedByDescending { it.score }.take(limit)
        emit(sorted)
    }

    fun generateMixes(profile: TasteProfile): List<Mix> {
        val mixes = mutableListOf<Mix>()

        if (profile.topArtists.isNotEmpty()) {
            val topArtist = profile.topArtists.entries.first().key
            mixes.add(Mix(
                id = "mix-artist-${topArtist.hashCode()}",
                title = "Mix: $topArtist",
                trackCount = 25,
                artworkUrl = null,
            ))
        }

        mixes.add(Mix(id = "mix-discover", title = "Discover Weekly", trackCount = 30, artworkUrl = null))
        mixes.add(Mix(id = "mix-chill", title = "Chill Vibes", trackCount = 40, artworkUrl = null))
        mixes.add(Mix(id = "mix-energy", title = "Energy Boost", trackCount = 35, artworkUrl = null))

        return mixes
    }

    fun cosineSimilarity(a: Map<String, Float>, b: Map<String, Float>): Float {
        val allKeys = a.keys + b.keys
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        allKeys.forEach { key ->
            val va = a[key] ?: 0f
            val vb = b[key] ?: 0f
            dotProduct += va * vb
            normA += va * va
            normB += vb * vb
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0) dotProduct / denom else 0f
    }

    private fun entityToTrack(e: TrackEntity) = Track(
        id = e.id, title = e.title, artist = e.artist, albumName = e.albumName,
        durationMs = e.durationMs, artworkUrl = e.artworkUrl, source = e.source,
        sourceId = e.sourceId, streamUrl = e.streamUrl, isOffline = e.isOffline,
        isLiked = e.isLiked, playCount = e.playCount,
    )
}
