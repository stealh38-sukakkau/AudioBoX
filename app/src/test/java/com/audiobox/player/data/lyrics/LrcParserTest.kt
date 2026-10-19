package com.audiobox.player.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseStandardLrcFormat() {
        val lrc = """
            [ti:Test Song]
            [ar:Test Artist]
            [00:05.20]First line of lyrics
            [00:12.80]Second line of lyrics
            [01:05.50]Third line after one minute
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)

        // Line 1: 5 seconds + 20 centiseconds (200ms) = 5200ms
        assertEquals(5200L, result[0].timestampMs)
        assertEquals("First line of lyrics", result[0].text)

        // Line 2: 12 seconds + 800ms = 12800ms
        assertEquals(12800L, result[1].timestampMs)
        assertEquals("Second line of lyrics", result[1].text)

        // Line 3: 65 seconds + 500ms = 65500ms
        assertEquals(65500L, result[2].timestampMs)
        assertEquals("Third line after one minute", result[2].text)
    }

    @Test
    fun parseMillisecondPrecisionFormat() {
        val lrc = """
            [00:01.234]High precision line
            [00:02.5]Single digit precision line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(2, result.size)
        assertEquals(1234L, result[0].timestampMs)
        assertEquals("High precision line", result[0].text)

        // 2s + 500ms = 2500ms
        assertEquals(2500L, result[1].timestampMs)
        assertEquals("Single digit precision line", result[1].text)
    }

    @Test
    fun parseMultiTimestampLine() {
        val lrc = """
            [00:10.00][00:30.00]Repeated chorus phrase
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(2, result.size)
        assertEquals(10000L, result[0].timestampMs)
        assertEquals("Repeated chorus phrase", result[0].text)

        assertEquals(30000L, result[1].timestampMs)
        assertEquals("Repeated chorus phrase", result[1].text)
    }

    @Test
    fun parseEmptyOrInvalidReturnsEmpty() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n\n  ").isEmpty())
        assertTrue(LrcParser.parse("[ar:Artist Only]\n[ti:Title Only]").isEmpty())
    }
}
