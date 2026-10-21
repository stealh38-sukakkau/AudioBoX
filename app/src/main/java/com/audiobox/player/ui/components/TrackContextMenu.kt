package com.audiobox.player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.audiobox.player.model.LocalPlaylist
import com.audiobox.player.model.Song

@Composable
fun TrackContextMenu(
    song: Song,
    playlists: List<LocalPlaylist>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPlayNow: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onCreatePlaylist: (String, Song) -> Boolean
) {
    var showPlaylists by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Track actions")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(text = { Text("Play Now") }, onClick = { onExpandedChange(false); onPlayNow(song) })
            DropdownMenuItem(text = { Text("Play Next") }, onClick = { onExpandedChange(false); onPlayNext(song) })
            DropdownMenuItem(text = { Text("Add to Queue") }, onClick = { onExpandedChange(false); onAddToQueue(song) })
            DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { onExpandedChange(false); showPlaylists = true })
            DropdownMenuItem(text = { Text("Track Details") }, onClick = { onExpandedChange(false); showDetails = true })
        }
    }
    if (showPlaylists) AlertDialog(
        onDismissRequest = { showPlaylists = false },
        title = { Text("Add to playlist") },
        text = { androidx.compose.foundation.layout.Column {
            if (playlists.isEmpty()) Text("No local playlists yet.")
            playlists.forEach { playlist -> TextButton(onClick = { onAddToPlaylist(playlist.id, song); showPlaylists = false }) { Text(playlist.name) } }
            TextButton(onClick = { showPlaylists = false; newPlaylistName = ""; showCreate = true }) { Text("Create playlist and add track") }
        } },
        confirmButton = { TextButton(onClick = { showPlaylists = false }) { Text("Close") } }
    )
    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false },
        title = { Text("New playlist") },
        text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (onCreatePlaylist(newPlaylistName, song)) showCreate = false }) { Text("Create") } },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
    )
    if (showDetails) AlertDialog(
        onDismissRequest = { showDetails = false },
        title = { Text(song.title, fontWeight = FontWeight.Bold) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrackDetailRow("Artist", song.artist.ifBlank { "Unknown" })
                TrackDetailRow("Album", song.album.ifBlank { "Unknown" })
                TrackDetailRow("Duration", if (song.durationMs > 0) formatDuration(song.durationMs) else "Unknown")
                TrackDetailRow("Format", song.mimeType.ifBlank { "Unknown" })
                TrackDetailRow("File size", if (song.sizeBytes > 0) "%.2f MiB".format(song.sizeBytes / 1048576.0) else "Unavailable")
                TrackDetailRow("Source", song.source)
                if (song.folderPath.isNotBlank()) TrackDetailRow("Path", song.folderPath)
            }
        },
        confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Done") } }
    )
}

@Composable
private fun TrackDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
