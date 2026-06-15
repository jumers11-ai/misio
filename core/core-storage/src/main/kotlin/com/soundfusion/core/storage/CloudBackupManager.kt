package com.soundfusion.core.storage

import android.content.Context
import com.soundfusion.core.database.AppDatabase
import com.soundfusion.core.database.dao.PlaylistDao
import com.soundfusion.core.database.dao.TrackDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val likedTrackIds: List<String>,
    val playlists: List<BackupPlaylist>,
    val settings: Map<String, String>,
)

@Serializable
data class BackupPlaylist(
    val name: String,
    val description: String? = null,
    val trackIds: List<String>,
)

enum class BackupState { IDLE, BACKING_UP, RESTORING, SUCCESS, ERROR }

@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val preferences: AudioPreferences,
    private val json: Json,
) {
    private val _state = MutableStateFlow(BackupState.IDLE)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    suspend fun createBackup(): File? = withContext(Dispatchers.IO) {
        try {
            _state.value = BackupState.BACKING_UP

            // Gather liked tracks
            val likedTracks = trackDao.observeLiked().first()
            val likedIds = likedTracks.map { it.id }

            // Gather playlists
            val allPlaylists = playlistDao.observeAll().first()
            val backupPlaylists = allPlaylists.map { playlist ->
                val tracks = playlistDao.observeTracksForPlaylist(playlist.id).first()
                BackupPlaylist(
                    name = playlist.name,
                    description = playlist.description,
                    trackIds = tracks.map { it.id },
                )
            }

            // Gather settings
            val settings = mapOf(
                "eq_enabled" to preferences.eqEnabled.toString(),
                "bass_boost_strength" to preferences.bassBoostStrength.toString(),
                "crossfade_duration_ms" to preferences.crossfadeDurationMs.toString(),
                "loudness_norm_enabled" to preferences.loudnessNormEnabled.toString(),
            )

            val backup = BackupData(
                likedTrackIds = likedIds,
                playlists = backupPlaylists,
                settings = settings,
            )

            // Write to file
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "soundfusion_backup_$dateStr.json")
            file.writeText(json.encodeToString(backup))

            _lastBackupTime.value = System.currentTimeMillis()
            _state.value = BackupState.SUCCESS

            file
        } catch (e: Exception) {
            _state.value = BackupState.ERROR
            null
        }
    }

    suspend fun restoreBackup(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = BackupState.RESTORING

            val content = file.readText()
            val backup = json.decodeFromString<BackupData>(content)

            // Restore liked tracks
            backup.likedTrackIds.forEach { trackId ->
                trackDao.setLiked(trackId, true)
            }

            // Restore settings
            backup.settings["eq_enabled"]?.toBooleanStrictOrNull()?.let { preferences.eqEnabled = it }
            backup.settings["bass_boost_strength"]?.toIntOrNull()?.let { preferences.bassBoostStrength = it }
            backup.settings["crossfade_duration_ms"]?.toLongOrNull()?.let { preferences.crossfadeDurationMs = it }
            backup.settings["loudness_norm_enabled"]?.toBooleanStrictOrNull()?.let { preferences.loudnessNormEnabled = it }

            _state.value = BackupState.SUCCESS
            true
        } catch (e: Exception) {
            _state.value = BackupState.ERROR
            false
        }
    }

    fun getBackupFiles(): List<File> {
        val dir = File(context.filesDir, "backups")
        return if (dir.exists()) {
            dir.listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun deleteBackup(file: File): Boolean = file.delete()

    fun getBackupSize(): Long {
        val dir = File(context.filesDir, "backups")
        return if (dir.exists()) dir.walkTopDown().sumOf { it.length() } else 0L
    }
}
