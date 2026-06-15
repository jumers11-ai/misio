package com.soundfusion.wearable

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SoundFusionWearableListener : WearableListenerService() {

    @Inject lateinit var wearableDataSync: WearableDataSync

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearableDataSync.PATH_PLAYBACK_COMMAND -> {
                val command = String(messageEvent.data)
                wearableDataSync.handleCommand(command)
            }
        }
    }
}
