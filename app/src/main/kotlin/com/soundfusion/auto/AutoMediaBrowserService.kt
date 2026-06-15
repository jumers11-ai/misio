package com.soundfusion.auto

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.database.dao.PlaylistDao
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.MusicSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import javax.inject.Inject

@AndroidEntryPoint
class AutoMediaBrowserService : MediaLibraryService() {

    @Inject lateinit var audioEngine: AudioEngine
    @Inject lateinit var trackDao: TrackDao
    @Inject lateinit var playlistDao: PlaylistDao

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val ROOT_ID = "root"
        private const val RECENT_ID = "recent"
        private const val PLAYLISTS_ID = "playlists"
        private const val FAVORITES_ID = "favorites"
        private const val YOUTUBE_ID = "youtube"
        private const val SPOTIFY_ID = "spotify"
        private const val LOCAL_ID = "local"
    }

    override fun onCreate() {
        super.onCreate()
        val player = audioEngine.exoPlayer
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ) = serviceScope.future {
            LibraryResult.ofItem(
                MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("SoundFusion")
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build(),
                params,
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ) = serviceScope.future {
            val children = when (parentId) {
                ROOT_ID -> buildRootChildren()
                RECENT_ID -> buildRecentTracks()
                FAVORITES_ID -> buildFavoriteTracks()
                PLAYLISTS_ID -> buildPlaylists()
                YOUTUBE_ID -> buildSourceTracks(MusicSource.YOUTUBE)
                SPOTIFY_ID -> buildSourceTracks(MusicSource.SPOTIFY)
                LOCAL_ID -> buildSourceTracks(MusicSource.LOCAL)
                else -> {
                    // Check if it's a playlist ID
                    if (parentId.startsWith("playlist:")) {
                        buildPlaylistTracks(parentId.removePrefix("playlist:"))
                    } else {
                        emptyList()
                    }
                }
            }
            LibraryResult.ofItemList(children, params)
        }
    }

    private fun buildRootChildren(): List<MediaItem> = listOf(
        buildBrowsableItem(RECENT_ID, "Ostatnio grane", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        buildBrowsableItem(FAVORITES_ID, "Ulubione", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        buildBrowsableItem(PLAYLISTS_ID, "Playlisty", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
        buildBrowsableItem(YOUTUBE_ID, "YouTube Music", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        buildBrowsableItem(SPOTIFY_ID, "Spotify", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        buildBrowsableItem(LOCAL_ID, "Lokalne pliki", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
    )

    private suspend fun buildRecentTracks(): List<MediaItem> {
        val tracks = trackDao.observeRecentlyPlayed(20).first()
        return tracks.map { entity ->
            buildPlayableItem(
                mediaId = entity.id,
                title = entity.title,
                artist = entity.artist,
                artworkUrl = entity.artworkUrl,
                streamUrl = entity.streamUrl,
            )
        }
    }

    private suspend fun buildFavoriteTracks(): List<MediaItem> {
        val tracks = trackDao.observeLiked().first()
        return tracks.map { entity ->
            buildPlayableItem(
                mediaId = entity.id,
                title = entity.title,
                artist = entity.artist,
                artworkUrl = entity.artworkUrl,
                streamUrl = entity.streamUrl,
            )
        }
    }

    private suspend fun buildPlaylists(): List<MediaItem> {
        val playlists = playlistDao.observeAll().first()
        return playlists.map { playlist ->
            buildBrowsableItem(
                mediaId = "playlist:${playlist.id}",
                title = playlist.name,
                mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
            )
        }
    }

    private suspend fun buildPlaylistTracks(playlistId: String): List<MediaItem> {
        val tracks = playlistDao.observeTracksForPlaylist(playlistId).first()
        return tracks.map { entity ->
            buildPlayableItem(
                mediaId = entity.id,
                title = entity.title,
                artist = entity.artist,
                artworkUrl = entity.artworkUrl,
                streamUrl = entity.streamUrl,
            )
        }
    }

    private suspend fun buildSourceTracks(source: MusicSource): List<MediaItem> {
        val tracks = trackDao.observeBySource(source).first()
        return tracks.take(100).map { entity ->
            buildPlayableItem(
                mediaId = entity.id,
                title = entity.title,
                artist = entity.artist,
                artworkUrl = entity.artworkUrl,
                streamUrl = entity.streamUrl,
            )
        }
    }

    private fun buildBrowsableItem(mediaId: String, title: String, mediaType: Int): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(mediaType)
                    .build()
            )
            .build()

    private fun buildPlayableItem(
        mediaId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        streamUrl: String?,
    ): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
}
