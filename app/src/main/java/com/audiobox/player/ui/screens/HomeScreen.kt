package com.audiobox.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.audiobox.player.ui.components.jellyfinArtworkRequest
import com.audiobox.player.model.Album
import com.audiobox.player.model.AudioOutputState
import com.audiobox.player.model.BitPerfectStatus
import com.audiobox.player.model.Song
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.AudioboxGold
import com.audiobox.player.ui.theme.DarkBackground
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextPrimary
import com.audiobox.player.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    albums: List<Album>,
    favoriteSongIds: Set<String>,
    audioOutputState: AudioOutputState,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onOpenAudioSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featuredSong = allSongs.firstOrNull()
    val favoriteSongs = allSongs.filter { favoriteSongIds.contains(it.id) }
    val recentlyAddedSongs = allSongs.sortedByDescending { it.dateAddedSec }.take(8)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // ─── Apple Music Header: Listen Now & Audio Output Pill ─────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Listen Now",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "AudioBoX Hi-Fi Player",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                // Quick Audio Output Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.clickable { onOpenAudioSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBitPerfect = audioOutputState.bitPerfectStatus == BitPerfectStatus.REQUESTED
                        Icon(
                            imageVector = if (audioOutputState.isUsbDac) Icons.Rounded.Usb else Icons.Rounded.Tune,
                            contentDescription = "Audio Output",
                            tint = if (isBitPerfect) Color(0xFF00E676) else AudioboxAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (audioOutputState.isUsbDac) "USB DAC" else "Audio Output",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBitPerfect) Color(0xFF00E676) else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ─── 1. Featured Top Pick Card ──────────────────────────────────────
        if (featuredSong != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
                        .clickable { onSongClick(featuredSong) }
                ) {
                    if (featuredSong.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = jellyfinArtworkRequest(featuredSong.artworkUrl),
                            contentDescription = featuredSong.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            AudioboxAccent.copy(alpha = 0.45f),
                                            Color(0xFF1C1C1E)
                                        )
                                    )
                                )
                        )
                    }

                    // Dark gradient scrim overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    // Content on top of Scrim
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FEATURED TRACK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AudioboxAccent,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = featuredSong.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${featuredSong.artist} • ${featuredSong.album}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Large Coral Play Button
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(AudioboxAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── 2. Recently Played Carousel ────────────────────────────────────
        if (allSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                HomeSectionHeader(
                    title = "Recently Played",
                    actionText = "See All",
                    onActionClick = onNavigateToLibrary
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(allSongs.take(10), key = { "recent_${it.id}" }) { song ->
                        val isCurrent = song.id == currentSongId

                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onSongClick(song) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) AudioboxAccent else DarkCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(DarkSurfaceElevated)
                            ) {
                                if (song.artworkUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = jellyfinArtworkRequest(song.artworkUrl),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(AudioboxAccent.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = AudioboxAccent,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                if (isCurrent && isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(AudioboxAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = "Playing",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) AudioboxAccent else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ─── 3. Albums Section ──────────────────────────────────────────────
        if (albums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HomeSectionHeader(
                    title = "Recent Albums",
                    actionText = "Library",
                    onActionClick = onNavigateToLibrary
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(albums.take(8), key = { "home_album_${it.id}" }) { album ->
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onAlbumClick(album) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                if (album.artworkUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = jellyfinArtworkRequest(album.artworkUrl),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(AudioboxAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Album,
                                            contentDescription = null,
                                            tint = AudioboxAccent,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${album.trackCount} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Favorites Playlist Section (if any songs favorited) ─────────
        if (favoriteSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HomeSectionHeader(
                    title = "Favorite Songs",
                    actionText = "${favoriteSongs.size} tracks",
                    onActionClick = {}
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    favoriteSongs.take(4).forEach { song ->
                        val isCurrent = song.id == currentSongId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSongClick(song) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (song.artworkUrl.isNotBlank()) {
                                AsyncImage(
                                    model = jellyfinArtworkRequest(song.artworkUrl),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AudioboxAccent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = AudioboxAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) AudioboxAccent else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favorite",
                                tint = AudioboxAccent,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = actionText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AudioboxAccent,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}
