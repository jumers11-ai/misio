package com.soundfusion.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.audio.model.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEngine: AudioEngine,
    private val scope: CoroutineScope,
) {
    fun startObserving() {
        scope.launch {
            combine(
                audioEngine.currentTrack,
                audioEngine.playbackState,
                audioEngine.positionMs,
                audioEngine.durationMs,
            ) { track, state, position, duration ->
                WidgetData(
                    title = track?.title ?: "",
                    artist = track?.artist ?: "",
                    isPlaying = state == PlaybackState.PLAYING,
                    progress = if (duration > 0) position.toFloat() / duration else 0f,
                )
            }.collect { data ->
                updateWidgets(data)
            }
        }
    }

    private suspend fun updateWidgets(data: WidgetData) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NowPlayingWidget::class.java)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
                prefs[NowPlayingWidget.TRACK_TITLE] = data.title
                prefs[NowPlayingWidget.TRACK_ARTIST] = data.artist
                prefs[NowPlayingWidget.IS_PLAYING] = data.isPlaying
                prefs[NowPlayingWidget.PROGRESS] = data.progress
            }
            NowPlayingWidget().update(context, glanceId)
        }
    }

    private data class WidgetData(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val progress: Float,
    )
}
