package com.audiobox.player.model

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class LyricsDisplayMode {
    COLLAPSED, PANEL, FULLSCREEN
}

data class NowPlayingUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val lyricsMode: LyricsDisplayMode = LyricsDisplayMode.COLLAPSED,
    val currentLyricIndex: Int = -1,
    val queue: List<Song> = emptyList(),
    val isNowPlayingExpanded: Boolean = false,
    val isFavorite: Boolean = false,
    val favoriteSongIds: Set<String> = emptySet(),
    val audioStreamInfo: AudioStreamInfo = AudioStreamInfo(),
    val audioOutputState: AudioOutputState = AudioOutputState(),
    val isLyricsLoading: Boolean = false,
    val lyricsError: String? = null,
    val isLyricsSynced: Boolean = false,
    val plainLyricsText: String? = null,
    val isBuffering: Boolean = false,
    val playbackError: String? = null
)
