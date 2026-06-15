package com.soundfusion.integration.youtube

import com.soundfusion.core.database.entity.TrackEntity
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Playlist
import com.soundfusion.core.database.model.Track
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMapper @Inject constructor() {

    fun toTrack(result: YouTubeSearchResult): Track = Track(
        id = UUID.nameUUIDFromBytes("yt:${result.id}".toByteArray()).toString(),
        title = result.title,
        artist = result.artist,
        durationMs = result.durationMs,
        artworkUrl = result.thumbnailUrl,
        source = MusicSource.YOUTUBE,
        sourceId = result.id,
    )

    fun toEntity(track: Track): TrackEntity = TrackEntity(
        id = track.id,
        title = track.title,
        artist = track.artist,
        durationMs = track.durationMs,
        artworkUrl = track.artworkUrl,
        source = track.source,
        sourceId = track.sourceId,
        streamUrl = track.streamUrl,
    )

    fun toPlaylist(result: YouTubePlaylistResult): Playlist = Playlist(
        id = UUID.nameUUIDFromBytes("yt:pl:${result.id}".toByteArray()).toString(),
        name = result.title,
        artworkUrl = result.thumbnailUrl,
        trackCount = result.trackCount,
        source = MusicSource.YOUTUBE,
    )
}
