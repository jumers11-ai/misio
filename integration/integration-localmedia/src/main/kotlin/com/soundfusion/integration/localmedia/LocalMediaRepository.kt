package com.soundfusion.integration.localmedia

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track
import com.soundfusion.core.network.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun scanLocalMedia(): Flow<List<Track>> = flow {
        val tracks = mutableListOf<Track>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        withContext(ioDispatcher) {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                    val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

                    val track = Track(
                        id = UUID.nameUUIDFromBytes("local:$mediaId".toByteArray()).toString(),
                        title = cursor.getString(titleCol),
                        artist = cursor.getString(artistCol),
                        albumName = cursor.getString(albumCol),
                        durationMs = cursor.getLong(durationCol),
                        artworkUrl = artworkUri.toString(),
                        source = MusicSource.LOCAL,
                        sourceId = mediaId.toString(),
                        streamUrl = contentUri.toString(),
                        isOffline = true,
                    )
                    tracks.add(track)
                }
            }

            // Persist to database
            trackDao.insertAll(tracks.map { track ->
                TrackEntity(
                    id = track.id, title = track.title, artist = track.artist,
                    albumName = track.albumName, durationMs = track.durationMs,
                    artworkUrl = track.artworkUrl, source = track.source,
                    sourceId = track.sourceId, streamUrl = track.streamUrl,
                    isOffline = true,
                )
            })
        }

        emit(tracks)
    }.flowOn(ioDispatcher)
}
