package com.soundfusion.integration.spotify

import android.app.Activity
import android.content.Intent
import com.soundfusion.core.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SpotifyUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val imageUrl: String?,
    val product: String,
)

@Singleton
class SpotifyAuthManager @Inject constructor(
    private val authManager: AuthManager,
) {
    companion object {
        const val SOURCE = "SPOTIFY"
        const val CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID"
        const val REDIRECT_URI = "soundfusion://spotify-callback"
        const val AUTH_URL = "https://accounts.spotify.com/authorize"
        const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        val SCOPES = listOf(
            "user-read-private",
            "user-read-email",
            "user-library-read",
            "user-library-modify",
            "playlist-read-private",
            "playlist-read-collaborative",
            "playlist-modify-public",
            "playlist-modify-private",
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "user-read-recently-played",
            "user-top-read",
        )
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _user = MutableStateFlow<SpotifyUser?>(null)
    val user: StateFlow<SpotifyUser?> = _user.asStateFlow()

    init {
        _isConnected.value = authManager.isAuthenticated(SOURCE)
    }

    fun buildAuthUrl(): String {
        val scope = SCOPES.joinToString(" ")
        return "$AUTH_URL?client_id=$CLIENT_ID&response_type=code&redirect_uri=$REDIRECT_URI&scope=$scope&show_dialog=true"
    }

    suspend fun handleAuthCallback(code: String) {
        // In production: exchange code for tokens via TOKEN_URL
        // val response = tokenApi.exchangeCode(code, REDIRECT_URI, CLIENT_ID, clientSecret)
        val mockAccessToken = "mock_access_token_$code"
        val mockRefreshToken = "mock_refresh_token_$code"

        authManager.connectAccount(SOURCE, mockAccessToken, mockRefreshToken)
        _isConnected.value = true
    }

    fun getAccessToken(): String? = authManager.getAccessToken(SOURCE)

    suspend fun refreshTokenIfNeeded(): String? {
        val current = getAccessToken() ?: return null
        // In production: check expiry and refresh
        return current
    }

    suspend fun disconnect() {
        authManager.disconnectAccount(SOURCE)
        _isConnected.value = false
        _user.value = null
    }
}
