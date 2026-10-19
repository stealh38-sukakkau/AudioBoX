package com.audiobox.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.audiobox.player.model.Song
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.DarkBackground
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextPrimary
import com.audiobox.player.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveItem: (index: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Playing Next",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${queue.size} songs in queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Queue",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Currently Playing Card
            if (currentSong != null) {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AudioboxAccent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AudioboxAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QueueSongArtwork(song = currentSong, size = 46)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = currentSong.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AudioboxAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Playing",
                                tint = AudioboxAccent,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Up Next List
            Text(
                text = "UP NEXT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    itemsIndexed(queue, key = { index, song -> "${song.id}_$index" }) { index, song ->
                        val isCurrent = song.id == currentSong?.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) AudioboxAccent.copy(alpha = 0.1f) else DarkSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isCurrent) AudioboxAccent.copy(alpha = 0.25f) else DarkCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSongClick(song) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QueueSongArtwork(song = song, size = 42)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) AudioboxAccent else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${formatDuration(song.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Reorder Controls: Move Up & Move Down
                            if (index > 0) {
                                IconButton(
                                    onClick = { onMoveItem(index, index - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (index < queue.lastIndex) {
                                IconButton(
                                    onClick = { onMoveItem(index, index + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Remove from queue
                            IconButton(
                                onClick = { onRemoveItem(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove from Queue",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongArtwork(song: Song, size: Int) {
    if (song.artworkUrl.isNotBlank()) {
        AsyncImage(
            model = jellyfinArtworkRequest(song.artworkUrl),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AudioboxAccent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = AudioboxAccent,
                modifier = Modifier.size((size * 0.5f).dp)
            )
        }
    }
}
