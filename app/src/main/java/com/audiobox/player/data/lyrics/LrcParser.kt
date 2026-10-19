package com.audiobox.player.data.lyrics

import com.audiobox.player.model.LyricLine
import java.util.regex.Pattern

object LrcParser {

    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")

    /**
     * Parse raw string LRC format menjadi daftar LyricLine terurut berdasarkan timestamp (milidetik).
     * Mendukung multi-tag per baris (misal [00:10.00][00:20.00] text)
     * dan mengabaikan metadata tag seperti [ti:Title], [ar:Artist], dll.
     */
    fun parse(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank()) return emptyList()

        val parsedLines = mutableListOf<LyricLine>()
        val lines = lrcContent.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Abaikan metadata tag seperti [ar:Artist], [ti:Title], [al:Album], [by:...]
            if (line.startsWith("[ar:") || line.startsWith("[ti:") ||
                line.startsWith("[al:") || line.startsWith("[by:") ||
                line.startsWith("[offset:") || line.startsWith("[length:") ||
                line.startsWith("[re:") || line.startsWith("[ve:")
            ) {
                continue
            }

            val matcher = TIME_TAG_PATTERN.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisPart = matcher.group(3)

                val millis = when (millisPart?.length) {
                    null, 0 -> 0L
                    1 -> (millisPart.toLongOrNull() ?: 0L) * 100L
                    2 -> (millisPart.toLongOrNull() ?: 0L) * 10L
                    else -> (millisPart.take(3).toLongOrNull() ?: 0L)
                }

                val totalMs = (minutes * 60L * 1000L) + (seconds * 1000L) + millis
                timestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = line.substring(lastMatchEnd).trim()
                // Masukkan teks ke setiap timestamp yang ditemukan pada baris tersebut
                for (ts in timestamps) {
                    parsedLines.add(LyricLine(timestampMs = ts, text = text))
                }
            }
        }

        // Urutkan berdasarkan waktu mulai lagu
        return parsedLines.sortedBy { it.timestampMs }
    }
}
