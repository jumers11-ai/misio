package com.soundfusion.core.audio.multiroom

import android.content.Context
import android.media.MediaRouter2
import android.media.RouteDiscoveryPreference
import android.media.RoutingSessionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class AudioDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isConnected: Boolean = false,
    val volume: Int = 100,
)

enum class DeviceType {
    PHONE_SPEAKER,
    BLUETOOTH,
    CHROMECAST,
    DLNA,
    GROUP,
}

@Singleton
class MultiRoomManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _devices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val devices: StateFlow<List<AudioDevice>> = _devices.asStateFlow()

    private val _activeDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val activeDevices: StateFlow<List<AudioDevice>> = _activeDevices.asStateFlow()

    private val _isMultiRoomActive = MutableStateFlow(false)
    val isMultiRoomActive: StateFlow<Boolean> = _isMultiRoomActive.asStateFlow()

    init {
        _devices.value = listOf(
            AudioDevice("phone", "Ten telefon", DeviceType.PHONE_SPEAKER, isConnected = true),
        )
        _activeDevices.value = listOf(_devices.value.first())
    }

    fun startDiscovery() {
        // In production: use MediaRouter2 for device discovery
        // val router = MediaRouter2.getInstance(context)
        // val preference = RouteDiscoveryPreference.Builder(
        //     listOf(MediaRoute2Info.FEATURE_REMOTE_PLAYBACK), true
        // ).build()
        // router.registerRouteCallback(executor, routeCallback, preference)
    }

    fun stopDiscovery() {
        // router.unregisterRouteCallback(routeCallback)
    }

    fun connectDevice(deviceId: String) {
        _devices.update { devices ->
            devices.map {
                if (it.id == deviceId) it.copy(isConnected = true) else it
            }
        }
        _activeDevices.value = _devices.value.filter { it.isConnected }
        _isMultiRoomActive.value = _activeDevices.value.size > 1
    }

    fun disconnectDevice(deviceId: String) {
        if (deviceId == "phone") return
        _devices.update { devices ->
            devices.map {
                if (it.id == deviceId) it.copy(isConnected = false) else it
            }
        }
        _activeDevices.value = _devices.value.filter { it.isConnected }
        _isMultiRoomActive.value = _activeDevices.value.size > 1
    }

    fun setDeviceVolume(deviceId: String, volume: Int) {
        _devices.update { devices ->
            devices.map {
                if (it.id == deviceId) it.copy(volume = volume.coerceIn(0, 100)) else it
            }
        }
        _activeDevices.update { active ->
            active.map {
                if (it.id == deviceId) it.copy(volume = volume.coerceIn(0, 100)) else it
            }
        }
    }

    fun createGroup(name: String, deviceIds: List<String>) {
        val groupDevice = AudioDevice(
            id = "group-${System.currentTimeMillis()}",
            name = name,
            type = DeviceType.GROUP,
            isConnected = true,
        )
        _devices.update { it + groupDevice }
    }
}
