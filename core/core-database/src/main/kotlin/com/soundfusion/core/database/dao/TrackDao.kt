package com.soundfusion.core.database.dao

import androidx.room.*
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY added_at DESC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE source = :source ORDER BY title ASC")
    fun observeBySource(source: MusicSource): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_liked = 1 ORDER BY added_at DESC")
    fun observeLiked(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_offline = 1 ORDER BY title ASC")
    fun observeOffline(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY last_played_at DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY play_count DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 50): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE source = :source AND source_id = :sourceId")
    suspend fun getBySourceId(source: MusicSource, sourceId: String): TrackEntity?

    @Query("""
        SELECT * FROM tracks 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
        ORDER BY play_count DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Update
    suspend fun update(track: TrackEntity)

    @Query("UPDATE tracks SET is_liked = :liked WHERE id = :trackId")
    suspend fun setLiked(trackId: String, liked: Boolean)

    @Query("UPDATE tracks SET play_count = play_count + 1, last_played_at = :timestamp WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET stream_url = :url WHERE id = :trackId")
    suspend fun updateStreamUrl(trackId: String, url: String)

    @Delete
    suspend fun delete(track: TrackEntity)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM tracks WHERE source = :source")
    suspend fun countBySource(source: MusicSource): Int
}
