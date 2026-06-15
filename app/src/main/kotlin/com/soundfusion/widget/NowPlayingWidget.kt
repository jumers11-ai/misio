package com.soundfusion.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.soundfusion.R
import com.soundfusion.core.audio.PlaybackService

class NowPlayingWidget : GlanceAppWidget() {

    companion object {
        val TRACK_TITLE = stringPreferencesKey("track_title")
        val TRACK_ARTIST = stringPreferencesKey("track_artist")
        val IS_PLAYING = booleanPreferencesKey("is_playing")
        val PROGRESS = floatPreferencesKey("progress")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                NowPlayingContent()
            }
        }
    }
}

@Composable
private fun NowPlayingContent() {
    val prefs = currentState<Preferences>()
    val title = prefs[NowPlayingWidget.TRACK_TITLE] ?: "Nie odtwarzasz"
    val artist = prefs[NowPlayingWidget.TRACK_ARTIST] ?: "SoundFusion"
    val isPlaying = prefs[NowPlayingWidget.IS_PLAYING] ?: false
    val progress = prefs[NowPlayingWidget.PROGRESS] ?: 0f

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Track info
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurface,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = artist,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.secondary,
                        ),
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Progress bar
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(GlanceTheme.colors.surfaceVariant),
            ) {
                Box(
                    modifier = GlanceModifier
                        .height(3.dp)
                        .width((progress * 200).dp)
                        .background(GlanceTheme.colors.primary),
                ) {}
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Controls
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(36.dp)
                        .clickable(actionRunCallback<PreviousAction>()),
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                Image(
                    provider = ImageProvider(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = "Play/Pause",
                    modifier = GlanceModifier
                        .size(44.dp)
                        .clickable(actionRunCallback<PlayPauseAction>()),
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(36.dp)
                        .clickable(actionRunCallback<NextAction>()),
                )
            }
        }
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "TOGGLE_PLAY_PAUSE"
        }
        context.startService(intent)
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "SKIP_PREVIOUS"
        }
        context.startService(intent)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "SKIP_NEXT"
        }
        context.startService(intent)
    }
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}
