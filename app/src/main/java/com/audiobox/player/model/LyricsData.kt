package com.audiobox.player.model

data class LyricsData(
    val isSynced: Boolean,
    val syncedLines: List<LyricLine> = emptyList(),
    val plainText: String? = null,
    val isInstrumental: Boolean = false,
    val source: String = "LRCLIB"
)
