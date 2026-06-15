package com.soundfusion.core.audio.cast

import android.content.Context
import com.soundfusion.core.database.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class DlnaDevice(
    val id: String,
    val name: String,
    val manufacturer: String,
    val isConnected: Boolean = false,
)

@Singleton
class DlnaManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _devices = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val devices: StateFlow<List<DlnaDevice>> = _devices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DlnaDevice?>(null)
    val connectedDevice: StateFlow<DlnaDevice?> = _connectedDevice.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun startDiscovery() {
        _isScanning.value = true
        scope.launch {
            // In production: use UPnP/DLNA library (Cling or dlna-dmr)
            // val registry = UpnpServiceImpl.getRegistry()
            // registry.addListener(object : RegistryListener { ... })
            // controlPoint.search(STAllHeader(), 10)

            _devices.value = listOf(
                DlnaDevice("dlna-1", "Samsung TV", "Samsung"),
                DlnaDevice("dlna-2", "Sonos One", "Sonos"),
            )
            _isScanning.value = false
        }
    }

    fun stopDiscovery() {
        _isScanning.value = false
    }

    fun connect(deviceId: String) {
        val device = _devices.value.find { it.id == deviceId } ?: return
        _connectedDevice.value = device.copy(isConnected = true)
        _devices.value = _devices.value.map { it.copy(isConnected = it.id == deviceId) }
    }

    fun disconnect() {
        _connectedDevice.value = null
        _devices.value = _devices.value.map { it.copy(isConnected = false) }
    }

    fun play(track: Track) {
        if (_connectedDevice.value == null) return
        // In production:
        // val service = device.findService(UDAServiceId("AVTransport"))
        // val action = SetAVTransportURI(service, track.streamUrl, track.title)
        // controlPoint.execute(action)
        // controlPoint.execute(Play(service))
    }

    fun pause() { /* controlPoint.execute(Pause(service)) */ }
    fun stop() { /* controlPoint.execute(Stop(service)) */ }
    fun seek(positionMs: Long) { /* controlPoint.execute(Seek(service, ...)) */ }
    fun setVolume(volume: Int) { /* controlPoint.execute(SetVolume(service, volume)) */ }
}
