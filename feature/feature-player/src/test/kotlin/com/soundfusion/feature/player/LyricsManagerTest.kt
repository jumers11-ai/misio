package com.soundfusion.feature.player

import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LyricsManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var manager: LyricsManager

    @Before
    fun setup() {
        manager = LyricsManager(okHttpClient = mockk(relaxed = true), ioDispatcher = testDispatcher)
    }

    @Test
    fun `parseLrc parses standard LRC format`() {
        val lrc = """
            [00:00.00]Intro
            [00:04.50]First line of the song
            [00:08.20]Second line here
            [00:12.00]Third line too
        """.trimIndent()

        val lines = manager.parseLrc(lrc)
        assertEquals(4, lines.size)
        assertEquals("Intro", lines[0].text)
        assertEquals(0L, lines[0].timeMs)
        assertEquals("First line of the song", lines[1].text)
        assertEquals(4500L, lines[1].timeMs)
        assertEquals("Second line here", lines[2].text)
        assertEquals(8200L, lines[2].timeMs)
        assertEquals("Third line too", lines[3].text)
        assertEquals(12000L, lines[3].timeMs)
    }

    @Test
    fun `parseLrc handles empty lines`() {
        val lrc = "[00:00.00]Line 1\n[00:05.00]\n[00:10.00]Line 3"
        val lines = manager.parseLrc(lrc)
        assertEquals(3, lines.size)
        assertEquals("", lines[1].text)
    }

    @Test
    fun `parseLrc returns empty for invalid input`() {
        val lines = manager.parseLrc("not a valid lrc")
        assertEquals(0, lines.size)
    }

    @Test
    fun `parseLrc sorts by time`() {
        val lrc = "[00:10.00]Later\n[00:00.00]First\n[00:05.00]Middle"
        val lines = manager.parseLrc(lrc)
        assertEquals("First", lines[0].text)
        assertEquals("Middle", lines[1].text)
        assertEquals("Later", lines[2].text)
    }

    @Test
    fun `getActiveLine returns correct index at start`() {
        val lines = listOf(
            LrcLine(0, "A"), LrcLine(5000, "B"), LrcLine(10000, "C"),
        )
        assertEquals(0, manager.getActiveLine(lines, 2000))
    }

    @Test
    fun `getActiveLine returns correct index in middle`() {
        val lines = listOf(
            LrcLine(0, "A"), LrcLine(5000, "B"), LrcLine(10000, "C"),
        )
        assertEquals(1, manager.getActiveLine(lines, 7000))
    }

    @Test
    fun `getActiveLine returns last index at end`() {
        val lines = listOf(
            LrcLine(0, "A"), LrcLine(5000, "B"), LrcLine(10000, "C"),
        )
        assertEquals(2, manager.getActiveLine(lines, 15000))
    }

    @Test
    fun `getActiveLine returns -1 before first line`() {
        val lines = listOf(LrcLine(5000, "A"))
        assertEquals(-1, manager.getActiveLine(lines, 3000))
    }

    @Test
    fun `getActiveLine handles empty list`() {
        assertEquals(-1, manager.getActiveLine(emptyList(), 5000))
    }
}
