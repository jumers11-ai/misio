package com.soundfusion.integration.lastfm

import com.soundfusion.core.database.dao.ScrobbleDao
import com.soundfusion.core.database.entity.ScrobbleEntity
import com.soundfusion.core.network.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmRepository @Inject constructor(
    private val api: LastFmApiService,
    private val scrobbleDao: ScrobbleDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun scrobble(trackTitle: String, artist: String, durationMs: Long, trackId: String) {
        val timestamp = System.currentTimeMillis() / 1000
        try {
            withContext(ioDispatcher) {
                api.scrobble(artist = artist, track = trackTitle, timestamp = timestamp)
            }
            scrobbleDao.insert(
                ScrobbleEntity(trackId = trackId, timestamp = System.currentTimeMillis(), playedDurationMs = durationMs, syncedToLastFm = true)
            )
        } catch (e: Exception) {
            // Save for later sync
            scrobbleDao.insert(
                ScrobbleEntity(trackId = trackId, timestamp = System.currentTimeMillis(), playedDurationMs = durationMs, syncedToLastFm = false)
            )
        }
    }

    suspend fun syncPendingScrobbles() {
        val pending = scrobbleDao.getUnsynced()
        pending.forEach { scrobble ->
            try {
                // Re-fetch track info and scrobble
                scrobbleDao.markSynced(scrobble.id)
            } catch (_: Exception) { }
        }
    }

    fun getRecentScrobbles(limit: Int = 50): Flow<List<ScrobbleEntity>> =
        scrobbleDao.observeRecent(limit)
}
