package com.soundfusion.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AuthAccount(
    val source: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isConnected: Boolean = true,
)

@Singleton
class AuthManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val _accounts = MutableStateFlow<List<AuthAccount>>(emptyList())
    val accounts: StateFlow<List<AuthAccount>> = _accounts.asStateFlow()

    fun isAuthenticated(source: String): Boolean =
        tokenStore.getAccessToken(source) != null

    suspend fun connectAccount(source: String, accessToken: String, refreshToken: String?) {
        tokenStore.saveTokens(source, accessToken, refreshToken)
        _accounts.value = _accounts.value + AuthAccount(source = source, userId = "", displayName = source)
    }

    suspend fun disconnectAccount(source: String) {
        tokenStore.clearTokens(source)
        _accounts.value = _accounts.value.filter { it.source != source }
    }

    fun getAccessToken(source: String): String? = tokenStore.getAccessToken(source)

    suspend fun refreshToken(source: String): String? {
        val refresh = tokenStore.getRefreshToken(source) ?: return null
        // In production: call OAuth refresh endpoint
        return refresh
    }
}
