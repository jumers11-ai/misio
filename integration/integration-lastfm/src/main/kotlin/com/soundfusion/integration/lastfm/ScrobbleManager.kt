package com.soundfusion.integration.lastfm

import com.soundfusion.core.audio.AudioEngine
import com.soundfusion.core.audio.model.PlaybackState
import com.soundfusion.core.database.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrobbleManager @Inject constructor(
    private val audioEngine: AudioEngine,
    private val lastFmRepository: LastFmRepository,
    private val lastFmAuth: LastFmAuthManager,
    private val scope: CoroutineScope,
) {
    private var scrobbleJob: Job? = null
    private var currentTrackStartMs: Long = 0L
    private var hasScrobbled = false

    fun startObserving() {
        scope.launch {
            combine(
                audioEngine.currentTrack,
                audioEngine.playbackState,
            ) { track, state -> Pair(track, state) }
                .distinctUntilChanged()
                .collect { (track, state) ->
                    when {
                        track != null && state == PlaybackState.PLAYING -> onTrackPlaying(track)
                        state == PlaybackState.PAUSED -> onTrackPaused()
                        state == PlaybackState.ENDED -> onTrackEnded()
                        track == null -> resetScrobble()
                    }
                }
        }
    }

    private fun onTrackPlaying(track: Track) {
        if (!lastFmAuth.isConnected.value) return

        val isNewTrack = audioEngine.positionMs.value < 3000
        if (isNewTrack) {
            resetScrobble()
            currentTrackStartMs = System.currentTimeMillis()

            // Update "Now Playing" on Last.fm
            scope.launch {
                try {
                    // api.updateNowPlaying(track.artist, track.title)
                } catch (_: Exception) { }
            }
        }

        // Start scrobble timer
        if (!hasScrobbled) {
            scrobbleJob?.cancel()
            scrobbleJob = scope.launch {
                // Scrobble after 50% of duration or 4 minutes, whichever is less
                val scrobbleThreshold = minOf(track.durationMs / 2, 240_000L)
                val elapsed = System.currentTimeMillis() - currentTrackStartMs
                val remaining = (scrobbleThreshold - elapsed).coerceAtLeast(0)

                delay(remaining)

                lastFmRepository.scrobble(
                    trackTitle = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs,
                    trackId = track.id,
                )
                hasScrobbled = true
            }
        }
    }

    private fun onTrackPaused() {
        scrobbleJob?.cancel()
    }

    private fun onTrackEnded() {
        scrobbleJob?.cancel()
    }

    private fun resetScrobble() {
        scrobbleJob?.cancel()
        hasScrobbled = false
        currentTrackStartMs = 0L
    }
}
