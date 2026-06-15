package com.soundfusion.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.audioPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "audio_prefs")

@Singleton
class AudioPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.audioPrefsStore

    companion object {
        private val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        private val BASS_BOOST_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        private val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        private val LOUDNESS_NORM_ENABLED = booleanPreferencesKey("loudness_norm_enabled")
        private val TARGET_GAIN_MB = intPreferencesKey("target_gain_mb")
        private val CROSSFADE_DURATION_MS = longPreferencesKey("crossfade_duration_ms")
        private val REPLAY_GAIN_PREAMP = floatPreferencesKey("replay_gain_preamp")
    }

    var eqEnabled: Boolean
        get() = runBlocking { store.data.map { it[EQ_ENABLED] ?: false }.first() }
        set(value) = runBlocking { store.edit { it[EQ_ENABLED] = value } }

    var eqBandLevels: List<Short>
        get() = runBlocking {
            store.data.map { prefs ->
                (0..9).map { i -> (prefs[intPreferencesKey("eq_band_$i")] ?: 0).toShort() }
            }.first()
        }
        set(value) = runBlocking {
            store.edit { prefs ->
                value.forEachIndexed { i, level -> prefs[intPreferencesKey("eq_band_$i")] = level.toInt() }
            }
        }

    var bassBoostEnabled: Boolean
        get() = runBlocking { store.data.map { it[BASS_BOOST_ENABLED] ?: false }.first() }
        set(value) = runBlocking { store.edit { it[BASS_BOOST_ENABLED] = value } }

    var bassBoostStrength: Int
        get() = runBlocking { store.data.map { it[BASS_BOOST_STRENGTH] ?: 0 }.first() }
        set(value) = runBlocking { store.edit { it[BASS_BOOST_STRENGTH] = value } }

    var loudnessNormEnabled: Boolean
        get() = runBlocking { store.data.map { it[LOUDNESS_NORM_ENABLED] ?: false }.first() }
        set(value) = runBlocking { store.edit { it[LOUDNESS_NORM_ENABLED] = value } }

    var targetGainMb: Int
        get() = runBlocking { store.data.map { it[TARGET_GAIN_MB] ?: -1400 }.first() }
        set(value) = runBlocking { store.edit { it[TARGET_GAIN_MB] = value } }

    var crossfadeDurationMs: Long
        get() = runBlocking { store.data.map { it[CROSSFADE_DURATION_MS] ?: 0L }.first() }
        set(value) = runBlocking { store.edit { it[CROSSFADE_DURATION_MS] = value } }
}
