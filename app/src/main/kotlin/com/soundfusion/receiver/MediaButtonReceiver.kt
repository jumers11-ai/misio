package com.soundfusion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.soundfusion.core.audio.AudioEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaButtonReceiver : BroadcastReceiver() {

    @Inject lateinit var audioEngine: AudioEngine

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return

        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        if (event.action != KeyEvent.ACTION_DOWN) return

        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> audioEngine.togglePlayPause()
            KeyEvent.KEYCODE_MEDIA_PAUSE -> audioEngine.togglePlayPause()
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> audioEngine.togglePlayPause()
            KeyEvent.KEYCODE_MEDIA_NEXT -> audioEngine.skipNext()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> audioEngine.skipPrevious()
            KeyEvent.KEYCODE_HEADSETHOOK -> audioEngine.togglePlayPause()
        }
    }
}
