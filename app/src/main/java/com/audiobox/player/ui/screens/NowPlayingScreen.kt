package com.audiobox.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiobox.player.model.BitPerfectStatus
import com.audiobox.player.model.LyricsDisplayMode
import com.audiobox.player.model.NowPlayingUiState
import com.audiobox.player.model.MusicSource
import com.audiobox.player.model.Song
import com.audiobox.player.ui.components.AlbumArtCard
import com.audiobox.player.ui.components.AudioOutputSheet
import com.audiobox.player.ui.components.DynamicBackground
import com.audiobox.player.ui.components.LyricsView
import com.audiobox.player.ui.components.PlaybackControls
import com.audiobox.player.ui.components.QueueSheet
import com.audiobox.player.ui.components.SongProgressBar
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.AudioboxGold
import com.audiobox.player.ui.theme.DarkCardBorder

@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    dynamicColors: Boolean = true,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onLyricClick: (Long) -> Unit,
    onLyricsModeChange: (LyricsDisplayMode) -> Unit,
    onToggleBitPerfect: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onSongClick: (Song) -> Unit,
    onMoveQueueItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveQueueItem: (index: Int) -> Unit,
    onRetryLyrics: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = state.currentSong ?: return
    val isBitPerfect = state.audioOutputState.bitPerfectStatus == BitPerfectStatus.REQUESTED
    val isHiRes = state.audioStreamInfo.isHiRes
    var showOutputSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    DynamicBackground(
        palette = song.colorPalette.takeIf { dynamicColors },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─── Apple Music Top Bar: Down Grabber, Audio Badge, Favorite ───────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse Now Playing",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Apple Music Style Lossless / Hi-Res / Bit-Perfect Badge Pill
                val badgeText = when {
                    isBitPerfect -> "BIT-PERFECT"
                    isHiRes -> "HI-RES LOSSLESS"
                    state.audioStreamInfo.codecName.contains("FLAC", ignoreCase = true) -> "LOSSLESS"
                    state.audioOutputState.isUsbDac -> "USB DAC"
                    state.currentSong?.source == "JELLYFIN" -> "JELLYFIN STREAM"
                    else -> "AUDIOBOX"
                }
                val badgeColor = when {
                    isBitPerfect -> Color(0xFF00E676)
                    isHiRes -> AudioboxGold
                    state.currentSong?.source == "JELLYFIN" -> Color(0xFF4CAF50) // Green accent for Jellyfin
                    else -> Color.White.copy(alpha = 0.75f)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .clickable { showOutputSheet = true }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.audioOutputState.isUsbDac) Icons.Rounded.Usb else Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Favorite Toggle (Apple Music Heart)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (state.isFavorite) "Favorited" else "Favorite",
                        tint = if (state.isFavorite) AudioboxAccent else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // ─── Center Visual Area: Artwork / Fullscreen Lyrics / Panel Lyrics ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (state.lyricsMode) {
                    LyricsDisplayMode.COLLAPSED -> {
                        AlbumArtCard(
                            artworkUrl = song.artworkUrl,
                            isPlaying = state.isPlaying,
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .padding(vertical = 12.dp)
                        )
                    }

                    LyricsDisplayMode.FULLSCREEN -> {
                        LyricsView(
                            lyrics = song.lyrics,
                            plainText = state.plainLyricsText,
                            isSynced = state.isLyricsSynced,
                            isLoading = state.isLyricsLoading,
                            error = state.lyricsError,
                            currentLyricIndex = state.currentLyricIndex,
                            onLyricClick = onLyricClick,
                            onRetry = onRetryLyrics,
                            isFullscreen = true
                        )
                    }

                    LyricsDisplayMode.PANEL -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AlbumArtCard(
                                artworkUrl = song.artworkUrl,
                                isPlaying = state.isPlaying,
                                modifier = Modifier
                                    .size(90.dp)
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.80f)
                                    .align(Alignment.BottomCenter)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp))
                                    .padding(6.dp)
                            ) {
                                LyricsView(
                                    lyrics = song.lyrics,
                                    plainText = state.plainLyricsText,
                                    isSynced = state.isLyricsSynced,
                                    isLoading = state.isLyricsLoading,
                                    error = state.lyricsError,
                                    currentLyricIndex = state.currentLyricIndex,
                                    onLyricClick = onLyricClick,
                                    onRetry = onRetryLyrics,
                                    isFullscreen = false
                                )
                            }
                        }
                    }
                }
            }

            // ─── Track Info & Clean Audio Details Pill ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tapable Audio Stream Summary Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { showOutputSheet = true }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.audioStreamInfo.formattedSummary(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.audioOutputState.isUsbDac) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${state.audioOutputState.activeDeviceName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBitPerfect) Color(0xFF00E676) else AudioboxAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── Scrubber Bar ──────────────────────────────────────────────────
            if (state.isBuffering || state.playbackError != null) {
                Text(
                    text = state.playbackError ?: "Buffering…",
                    color = if (state.playbackError != null) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            SongProgressBar(
                currentPositionMs = state.currentPositionMs,
                durationMs = state.durationMs,
                onSeek = onSeek,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // ─── Apple Music Transport Controls ────────────────────────────────
            PlaybackControls(
                isPlaying = state.isPlaying,
                isShuffle = state.isShuffle,
                repeatMode = state.repeatMode,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ─── Apple Music Bottom Utility Bar: Lyrics | Output | Queue ────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Lyrics Toggle
                IconButton(
                    onClick = {
                        val nextMode = when (state.lyricsMode) {
                            LyricsDisplayMode.COLLAPSED -> LyricsDisplayMode.PANEL
                            LyricsDisplayMode.PANEL -> LyricsDisplayMode.FULLSCREEN
                            LyricsDisplayMode.FULLSCREEN -> LyricsDisplayMode.COLLAPSED
                        }
                        onLyricsModeChange(nextMode)
                    }
                ) {
                    val isLyricsActive = state.lyricsMode != LyricsDisplayMode.COLLAPSED
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = "Toggle Lyrics",
                        tint = if (isLyricsActive) AudioboxAccent else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Audio Output / AirPlay Button
                IconButton(onClick = { showOutputSheet = true }) {
                    Icon(
                        imageVector = if (state.audioOutputState.isUsbDac) Icons.Rounded.Usb else Icons.Rounded.GraphicEq,
                        contentDescription = "Audio Output",
                        tint = if (isBitPerfect) Color(0xFF00E676) else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Up Next Queue Button
                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Playing Next Queue",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Modal Sheet Audio Output & Bit-Perfect Mode
        if (showOutputSheet) {
            AudioOutputSheet(
                outputState = state.audioOutputState,
                streamInfo = state.audioStreamInfo,
                onToggleBitPerfect = onToggleBitPerfect,
                onDismiss = { showOutputSheet = false }
            )
        }

        // Modal Sheet Playing Next Queue
        if (showQueueSheet) {
            QueueSheet(
                queue = state.queue,
                currentSong = state.currentSong,
                isPlaying = state.isPlaying,
                onSongClick = { song ->
                    onSongClick(song)
                    showQueueSheet = false
                },
                onMoveItem = onMoveQueueItem,
                onRemoveItem = onRemoveQueueItem,
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}
