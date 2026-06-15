package com.soundfusion.core.audio.cast

import android.content.Context
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class CastDevice(
    val id: String,
    val name: String,
    val modelName: String,
    val isConnected: Boolean = false,
)

enum class CastState { NOT_CONNECTED, CONNECTING, CONNECTED }

@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEngine: AudioEngine,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(CastState.NOT_CONNECTED)
    val state: StateFlow<CastState> = _state.asStateFlow()

    private val _devices = MutableStateFlow<List<CastDevice>>(emptyList())
    val devices: StateFlow<List<CastDevice>> = _devices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<CastDevice?>(null)
    val connectedDevice: StateFlow<CastDevice?> = _connectedDevice.asStateFlow()

    fun startDiscovery() {
        // In production:
        // val castContext = CastContext.getSharedInstance(context)
        // val discoveryManager = castContext.discoveryManager
        // discoveryManager.addDiscoveryManagerListener(listener)

        // For now, simulate discovery
        _devices.value = listOf(
            CastDevice("cast-1", "Living Room TV", "Chromecast"),
            CastDevice("cast-2", "Kitchen Speaker", "Nest Mini"),
            CastDevice("cast-3", "Bedroom Display", "Nest Hub"),
        )
    }

    fun stopDiscovery() {
        // discoveryManager.removeDiscoveryManagerListener(listener)
    }

    fun connectToDevice(deviceId: String) {
        scope.launch {
            _state.value = CastState.CONNECTING
            val device = _devices.value.find { it.id == deviceId } ?: return@launch

            // In production:
            // val castSession = SessionManagerListener { ... }
            // castContext.sessionManager.startSession(castSession)

            _connectedDevice.value = device.copy(isConnected = true)
            _state.value = CastState.CONNECTED
            _devices.value = _devices.value.map {
                it.copy(isConnected = it.id == deviceId)
            }
        }
    }

    fun disconnect() {
        // castContext.sessionManager.endCurrentSession(true)
        _state.value = CastState.NOT_CONNECTED
        _connectedDevice.value = null
        _devices.value = _devices.value.map { it.copy(isConnected = false) }
    }

    fun castTrack(track: Track) {
        if (_state.value != CastState.CONNECTED) return
        scope.launch {
            // In production:
            // val mediaInfo = MediaInfo.Builder(track.streamUrl)
            //     .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            //     .setContentType("audio/mp4")
            //     .setMetadata(MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            //         putString(MediaMetadata.KEY_TITLE, track.title)
            //         putString(MediaMetadata.KEY_ARTIST, track.artist)
            //         addImage(WebImage(Uri.parse(track.artworkUrl)))
            //     })
            //     .build()
            // remoteMediaClient.load(MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).build())
        }
    }

    fun castPlay() { /* remoteMediaClient.play() */ }
    fun castPause() { /* remoteMediaClient.pause() */ }
    fun castSeek(positionMs: Long) { /* remoteMediaClient.seek(MediaSeekOptions.Builder().setPosition(positionMs).build()) */ }
    fun castSetVolume(volume: Float) { /* castSession.setVolume(volume.toDouble()) */ }
}
