package com.soundfusion.integration.lastfm

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LastFmApiService {
    @POST("2.0/")
    suspend fun scrobble(
        @Query("method") method: String = "track.scrobble",
        @Query("artist") artist: String,
        @Query("track") track: String,
        @Query("timestamp") timestamp: Long,
        @Query("api_key") apiKey: String = "",
        @Query("sk") sessionKey: String = "",
        @Query("format") format: String = "json",
    )

    @GET("2.0/")
    suspend fun getRecommendations(
        @Query("method") method: String = "user.getRecommendedArtists",
        @Query("api_key") apiKey: String = "",
        @Query("sk") sessionKey: String = "",
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json",
    ): LastFmRecommendationsResponse

    @GET("2.0/")
    suspend fun getSimilarTracks(
        @Query("method") method: String = "track.getSimilar",
        @Query("artist") artist: String,
        @Query("track") track: String,
        @Query("api_key") apiKey: String = "",
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json",
    ): LastFmSimilarResponse
}

@Serializable data class LastFmRecommendationsResponse(val recommendations: LastFmArtistList? = null)
@Serializable data class LastFmArtistList(val artist: List<LastFmArtist> = emptyList())
@Serializable data class LastFmArtist(val name: String, val url: String? = null)
@Serializable data class LastFmSimilarResponse(val similartracks: LastFmTrackList? = null)
@Serializable data class LastFmTrackList(val track: List<LastFmTrack> = emptyList())
@Serializable data class LastFmTrack(val name: String, val artist: LastFmArtist? = null)
