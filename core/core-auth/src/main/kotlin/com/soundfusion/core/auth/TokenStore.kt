package com.soundfusion.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getAccessToken(source: String): String? =
        prefs.getString("${source}_access_token", null)

    fun getRefreshToken(source: String): String? =
        prefs.getString("${source}_refresh_token", null)

    fun saveTokens(source: String, accessToken: String, refreshToken: String?) {
        prefs.edit()
            .putString("${source}_access_token", accessToken)
            .apply {
                refreshToken?.let { putString("${source}_refresh_token", it) }
            }
            .apply()
    }

    fun clearTokens(source: String) {
        prefs.edit()
            .remove("${source}_access_token")
            .remove("${source}_refresh_token")
            .apply()
    }
}
