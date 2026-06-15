package com.soundfusion.core.network

import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

data class CachedResult<T>(val data: T, val expiresAt: Long)

@Singleton
class CacheManager @Inject constructor() {
    private val searchCache = ConcurrentHashMap<String, CachedResult<List<Track>>>()

    fun getSearchResults(source: MusicSource, query: String): List<Track>? {
        val key = "${source.name}:$query"
        val cached = searchCache[key] ?: return null
        if (System.currentTimeMillis() > cached.expiresAt) {
            searchCache.remove(key)
            return null
        }
        return cached.data
    }

    fun cacheSearchResults(source: MusicSource, query: String, results: List<Track>, ttl: Duration) {
        val key = "${source.name}:$query"
        searchCache[key] = CachedResult(results, System.currentTimeMillis() + ttl.inWholeMilliseconds)
    }

    fun clearAll() = searchCache.clear()
}
