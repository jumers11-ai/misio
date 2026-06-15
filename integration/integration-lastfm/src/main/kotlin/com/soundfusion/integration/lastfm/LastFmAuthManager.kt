package com.soundfusion.integration.lastfm

import com.soundfusion.core.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmAuthManager @Inject constructor(
    private val authManager: AuthManager,
) {
    companion object {
        const val SOURCE = "LASTFM"
        const val API_KEY = "YOUR_LASTFM_API_KEY"
        const val API_SECRET = "YOUR_LASTFM_API_SECRET"
        const val AUTH_URL = "https://www.last.fm/api/auth/"
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        _isConnected.value = authManager.isAuthenticated(SOURCE)
    }

    fun buildAuthUrl(): String =
        "$AUTH_URL?api_key=$API_KEY&cb=soundfusion://lastfm-callback"

    suspend fun handleAuthCallback(token: String) {
        // In production: call auth.getSession with token + api_sig
        val mockSessionKey = "session_$token"
        authManager.connectAccount(SOURCE, mockSessionKey, null)
        _isConnected.value = true
    }

    fun getSessionKey(): String? = authManager.getAccessToken(SOURCE)

    suspend fun disconnect() {
        authManager.disconnectAccount(SOURCE)
        _isConnected.value = false
    }
}
