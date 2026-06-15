package com.soundfusion.core.database.model

enum class MusicSource { YOUTUBE, SPOTIFY, LOCAL, PODCAST, LASTFM }

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val albumName: String? = null,
    val albumId: String? = null,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val source: MusicSource,
    val sourceId: String,
    val streamUrl: String? = null,
    val isOffline: Boolean = false,
    val replayGain: Float? = null,
    val isLiked: Boolean = false,
    val playCount: Int = 0,
)

data class Album(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val artworkUrl: String? = null,
    val releaseYear: Int? = null,
    val trackCount: Int,
    val source: MusicSource,
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val source: MusicSource,
    val genres: List<String> = emptyList(),
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val trackCount: Int,
    val source: MusicSource,
    val isSmartPlaylist: Boolean = false,
)
