package com.audiobox.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiobox.player.model.LyricLine
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextPrimary
import com.audiobox.player.ui.theme.TextSecondary

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    plainText: String?,
    isSynced: Boolean,
    isLoading: Boolean,
    error: String?,
    currentLyricIndex: Int,
    onLyricClick: (Long) -> Unit,
    onRetry: () -> Unit,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Loading State (Sedang mencari di LRCLIB)
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = AudioboxAccent,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Searching lyrics on LRCLIB…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fetching synchronized metadata",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // 2. Error / Not Found State dengan Tombol Retry
    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lyrics Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Apple Style Retry Pill Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AudioboxAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onRetry() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Retry",
                            tint = AudioboxAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Search",
                            style = MaterialTheme.typography.labelSmall,
                            color = AudioboxAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    // 3. Synced Lyrics State (Auto-scroll & Highlight Baris Aktif)
    if (isSynced && lyrics.isNotEmpty()) {
        val listState = rememberLazyListState()

        LaunchedEffect(currentLyricIndex) {
            if (currentLyricIndex in lyrics.indices) {
                val targetIndex = (currentLyricIndex - 1).coerceAtLeast(0)
                listState.animateScrollToItem(index = targetIndex)
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = if (isFullscreen) 140.dp else 40.dp),
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == currentLyricIndex

                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.58f),
                    animationSpec = tween(durationMillis = 300),
                    label = "lyricTextColor"
                )

                val textScale by animateFloatAsState(
                    targetValue = if (isActive) 1.04f else 1.0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "lyricScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLyricClick(line.timestampMs) }
                        .padding(vertical = if (isFullscreen) 14.dp else 9.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = line.text,
                        color = textColor,
                        fontSize = if (isFullscreen) 28.sp else 21.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        lineHeight = if (isFullscreen) 38.sp else 30.sp,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.scale(textScale)
                    )
                }
            }
        }
        return
    }

    // 4. Plain Lyrics State (Lirik Teks Biasa dari LRCLIB)
    if (!plainText.isNullOrBlank()) {
        val plainLines = plainText.lines()
        LazyColumn(
            contentPadding = PaddingValues(
                vertical = if (isFullscreen) 140.dp else 36.dp,
                horizontal = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PLAIN LYRICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            items(plainLines) { line ->
                Text(
                    text = line.ifBlank { " " },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = if (isFullscreen) 22.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = if (isFullscreen) 32.sp else 26.sp
                )
            }
        }
        return
    }

    // 5. Empty State (Tidak Ada Lirik Ditemukan)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.FormatQuote,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Lyrics not available for this song",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No matched lyrics found on LRCLIB.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.clickable { onRetry() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Search Again",
                        tint = AudioboxAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Search LRCLIB",
                        style = MaterialTheme.typography.labelSmall,
                        color = AudioboxAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
