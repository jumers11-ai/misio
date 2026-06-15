package com.soundfusion.feature.player

import com.soundfusion.core.database.model.Track
import com.soundfusion.core.network.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class LrcLine(
    val timeMs: Long,
    val text: String,
)

data class SyncedLyrics(
    val trackId: String,
    val lines: List<LrcLine>,
    val source: String,
    val hasTranslation: Boolean = false,
    val translationLines: List<LrcLine> = emptyList(),
)

@Singleton
class LyricsManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val LRCLIB_BASE = "https://lrclib.net/api"
    }

    fun getLyrics(track: Track): Flow<SyncedLyrics?> = flow {
        // Try LrcLib first (free, no API key)
        val lrcLib = fetchFromLrcLib(track.title, track.artist, track.durationMs)
        if (lrcLib != null) {
            emit(lrcLib)
            return@flow
        }

        // Fallback: try other sources
        // Musixmatch, Genius, etc.
        emit(null)
    }.flowOn(ioDispatcher)

    private suspend fun fetchFromLrcLib(title: String, artist: String, durationSec: Long): SyncedLyrics? {
        return withContext(ioDispatcher) {
            try {
                val url = "$LRCLIB_BASE/search?track_name=${encode(title)}&artist_name=${encode(artist)}"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                // Parse synced lyrics from response
                val syncedLrc = extractSyncedLyrics(body) ?: return@withContext null
                val lines = parseLrc(syncedLrc)

                if (lines.isEmpty()) return@withContext null

                SyncedLyrics(
                    trackId = "$title-$artist",
                    lines = lines,
                    source = "LrcLib",
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun parseLrc(raw: String): List<LrcLine> {
        val regex = Regex("\\[(\\d+):(\\d+\\.\\d+)](.*)")
        return raw.lines().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val min = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val sec = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            val text = match.groupValues[3].trim()
            LrcLine(
                timeMs = (min * 60_000 + (sec * 1000)).toLong(),
                text = text,
            )
        }.sortedBy { it.timeMs }
    }

    fun getActiveLine(lines: List<LrcLine>, positionMs: Long): Int {
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }

    private fun extractSyncedLyrics(json: String): String? {
        // Simple extraction — in production use kotlinx.serialization
        val syncedKey = "\"syncedLyrics\""
        val idx = json.indexOf(syncedKey)
        if (idx == -1) return null
        val start = json.indexOf('"', idx + syncedKey.length + 1)
        if (start == -1) return null
        val end = json.indexOf('"', start + 1)
        if (end == -1) return null
        return json.substring(start + 1, end)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
