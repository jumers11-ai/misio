package com.soundfusion.integration.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SpotifyApiService {
    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 20,
        @Header("Authorization") auth: String = "",
    ): SpotifySearchResponse

    @GET("v1/me/tracks")
    suspend fun getSavedTracks(
        @Query("limit") limit: Int = 50,
        @Header("Authorization") auth: String = "",
    ): SpotifySavedTracksResponse

    @GET("v1/me/playlists")
    suspend fun getPlaylists(
        @Query("limit") limit: Int = 50,
        @Header("Authorization") auth: String = "",
    ): SpotifyPlaylistsResponse
}

@Serializable data class SpotifySearchResponse(val tracks: SpotifyTrackList)
@Serializable data class SpotifyTrackList(val items: List<SpotifyTrack>)
@Serializable data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum? = null,
    @SerialName("duration_ms") val durationMs: Long,
)
@Serializable data class SpotifyArtist(val id: String, val name: String)
@Serializable data class SpotifyAlbum(val name: String, val images: List<SpotifyImage> = emptyList())
@Serializable data class SpotifyImage(val url: String, val width: Int? = null, val height: Int? = null)
@Serializable data class SpotifySavedTracksResponse(val items: List<SpotifySavedTrack>)
@Serializable data class SpotifySavedTrack(val track: SpotifyTrack)
@Serializable data class SpotifyPlaylistsResponse(val items: List<SpotifyPlaylistItem>)
@Serializable data class SpotifyPlaylistItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val tracks: SpotifyPlaylistTracksRef,
)
@Serializable data class SpotifyPlaylistTracksRef(val total: Int)
