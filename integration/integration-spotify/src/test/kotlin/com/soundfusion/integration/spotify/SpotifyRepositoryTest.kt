package com.soundfusion.integration.spotify

import app.cash.turbine.test
import com.soundfusion.core.database.dao.TrackDao
import com.soundfusion.core.database.model.MusicSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpotifyRepositoryTest {

    private lateinit var repository: SpotifyRepository
    private val api = mockk<SpotifyApiService>()
    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val mockSearchResponse = SpotifySearchResponse(
        tracks = SpotifyTrackList(
            items = listOf(
                SpotifyTrack(
                    id = "sp-1",
                    name = "Blinding Lights",
                    artists = listOf(SpotifyArtist(id = "a-1", name = "The Weeknd")),
                    album = SpotifyAlbum(name = "After Hours", images = listOf(SpotifyImage(url = "https://img.com/1.jpg"))),
                    durationMs = 200_000L,
                ),
                SpotifyTrack(
                    id = "sp-2",
                    name = "Save Your Tears",
                    artists = listOf(SpotifyArtist(id = "a-1", name = "The Weeknd")),
                    album = SpotifyAlbum(name = "After Hours"),
                    durationMs = 215_000L,
                ),
            )
        )
    )

    private val mockSavedResponse = SpotifySavedTracksResponse(
        items = listOf(
            SpotifySavedTrack(
                track = SpotifyTrack(
                    id = "sp-3",
                    name = "Starboy",
                    artists = listOf(SpotifyArtist(id = "a-1", name = "The Weeknd")),
                    durationMs = 230_000L,
                )
            )
        )
    )

    private val mockPlaylistsResponse = SpotifyPlaylistsResponse(
        items = listOf(
            SpotifyPlaylistItem(
                id = "pl-1",
                name = "My Playlist",
                description = "Test playlist",
                images = listOf(SpotifyImage(url = "https://img.com/pl.jpg")),
                tracks = SpotifyPlaylistTracksRef(total = 25),
            )
        )
    )

    @Before
    fun setup() {
        coEvery { api.search(any(), any(), any(), any()) } returns mockSearchResponse
        coEvery { api.getSavedTracks(any(), any()) } returns mockSavedResponse
        coEvery { api.getPlaylists(any(), any()) } returns mockPlaylistsResponse

        repository = SpotifyRepository(
            api = api,
            trackDao = trackDao,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `search returns mapped tracks with SPOTIFY source`() = runTest(testDispatcher) {
        repository.search("weeknd").test {
            val tracks = awaitItem()
            assertEquals(2, tracks.size)
            assertEquals("Blinding Lights", tracks[0].title)
            assertEquals("The Weeknd", tracks[0].artist)
            assertEquals(MusicSource.SPOTIFY, tracks[0].source)
            assertEquals("sp-1", tracks[0].sourceId)
            assertEquals(200_000L, tracks[0].durationMs)
            assertTrue(tracks[0].id.startsWith("sp:"))
            awaitComplete()
        }
    }

    @Test
    fun `search maps album artwork correctly`() = runTest(testDispatcher) {
        repository.search("weeknd").test {
            val tracks = awaitItem()
            assertEquals("https://img.com/1.jpg", tracks[0].artworkUrl)
            awaitComplete()
        }
    }

    @Test
    fun `getSavedTracks returns user library`() = runTest(testDispatcher) {
        repository.getSavedTracks().test {
            val tracks = awaitItem()
            assertEquals(1, tracks.size)
            assertEquals("Starboy", tracks[0].title)
            assertEquals(MusicSource.SPOTIFY, tracks[0].source)
            awaitComplete()
        }
    }

    @Test
    fun `getPlaylists returns user playlists`() = runTest(testDispatcher) {
        repository.getPlaylists().test {
            val playlists = awaitItem()
            assertEquals(1, playlists.size)
            assertEquals("My Playlist", playlists[0].name)
            assertEquals(25, playlists[0].trackCount)
            assertEquals(MusicSource.SPOTIFY, playlists[0].source)
            assertTrue(playlists[0].id.startsWith("sp:pl:"))
            awaitComplete()
        }
    }
}
