package com.audiobox.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audiobox.player.data.AppLanguageChoice
import com.audiobox.player.data.AppThemeChoice
import com.audiobox.player.model.LocalPlaylist
import com.audiobox.player.model.Song
import com.audiobox.player.ui.components.TrackContextMenu
import com.audiobox.player.ui.theme.AudioboxAccent

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SettingsScreen(
    theme: AppThemeChoice,
    language: AppLanguageChoice,
    minimumSizeMiB: Int,
    onlineLyrics: Boolean,
    dynamicColors: Boolean,
    jellyfinEnabled: Boolean,
    jellyfinStatus: String,
    jellyfinError: String?,
    playlists: List<LocalPlaylist>,
    songs: List<Song>,
    trackCount: Int,
    albumCount: Int,
    artistCount: Int,
    totalBytes: Long,
    isScanning: Boolean,
    scanError: String?,
    onTheme: (AppThemeChoice) -> Unit,
    onLanguage: (AppLanguageChoice) -> Unit,
    onMinimumSize: (Int) -> Unit,
    onOnlineLyrics: (Boolean) -> Unit,
    onDynamicColors: (Boolean) -> Unit,
    onJellyfinEnabled: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onJellyfinSettings: () -> Unit,
    onCreatePlaylist: (String) -> Boolean,
    onRenamePlaylist: (String, String) -> Boolean,
    onDeletePlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (String, Song) -> Unit,
    onPlayPlaylist: (List<Song>) -> Unit,
    onPlayNow: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onCreatePlaylistWithSong: (String, Song) -> Boolean,
    onClearLyricsCache: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var confirmCacheClear by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val installedVersion = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            "${info.versionName} ($code)"
        }.getOrDefault("Unavailable")
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 120.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item { SectionTitle("Appearance") }
        item { ChoiceRow("Theme", AppThemeChoice.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, theme.ordinal) { onTheme(AppThemeChoice.entries[it]) } }
        item { ChoiceRow("Language preference", listOf("System default", "Bahasa Indonesia", "English"), language.ordinal) { onLanguage(AppLanguageChoice.entries[it]) } }
        item { SettingSwitch("Dynamic Now Playing colors", dynamicColors, onDynamicColors) }
        item { Text("Language is saved and applied as the app locale. Most existing interface text still needs translation.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp)) }

        item { SectionTitle("Local music library") }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Minimum audio file size", style = MaterialTheme.typography.titleMedium)
                Text(if (minimumSizeMiB == 0) "No size filter" else "$minimumSizeMiB MiB (1 MiB = 1,048,576 bytes)", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onMinimumSize((minimumSizeMiB - 1).coerceAtLeast(0)) }) { Text("−") }
                    Text(
                        if (minimumSizeMiB == 0) "  Off  " else "  ${minimumSizeMiB} MiB  ",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    OutlinedButton(onClick = { onMinimumSize(minimumSizeMiB + 1) }) { Text("+") }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Library statistics", style = MaterialTheme.typography.titleMedium)
                Text("$trackCount tracks • $albumCount albums • $artistCount artists", style = MaterialTheme.typography.bodySmall)
                Text(if (totalBytes > 0) "Indexed size: %.2f GiB".format(totalBytes / 1073741824.0) else "Indexed size unavailable", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRescan, enabled = !isScanning) { if (isScanning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Refresh, "Rescan library", tint = AudioboxAccent) }
        } }
        if (scanError != null) item { Text(scanError, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { Text("Playback formats depend on Android Media3 decoders and the device. The library scans MediaStore audio entries.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp)) }

        item { SectionTitle("Jellyfin") }
        item { Text("Connection: ${jellyfinStatus.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)}", style = MaterialTheme.typography.bodyMedium) }
        if (jellyfinError != null) item { Text(jellyfinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        item { SettingSwitch("Include Jellyfin music source", jellyfinEnabled, onJellyfinEnabled) }
        item { OutlinedButton(onClick = onJellyfinSettings, modifier = Modifier.fillMaxWidth()) { Text("Manage Jellyfin connection") } }

        item { SectionTitle("Lyrics and data") }
        item { SettingSwitch("Fetch lyrics online", onlineLyrics, onOnlineLyrics) }
        item { OutlinedButton(onClick = { confirmCacheClear = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear lyrics cache") } }

        item { SectionTitle("Local playlists") }
        item { OutlinedButton(onClick = { text = ""; dialog = "create" }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Add, null); Text("  Create playlist") } }
        items(playlists, key = { it.id }) { playlist ->
            val tracks = playlist.songIds.mapNotNull { id -> songs.find { it.id == id } }
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                        Text("${tracks.size} tracks${if (tracks.size != playlist.songIds.size) " • ${playlist.songIds.size - tracks.size} unavailable" else ""}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { selectedPlaylist = playlist; text = playlist.name; dialog = "rename" }) { Icon(Icons.Rounded.Edit, "Rename") }
                    IconButton(onClick = { onPlayPlaylist(tracks) }, enabled = tracks.isNotEmpty()) { Icon(Icons.Rounded.PlayArrow, "Play playlist") }
                    IconButton(onClick = { selectedPlaylist = playlist; dialog = "delete" }) { Icon(Icons.Rounded.Delete, "Delete") }
                }
                tracks.forEach { song ->
                    var showTrackMenu by remember(playlist.id, song.id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp).combinedClickable(
                        onClick = { onPlayNow(song) }, onLongClick = { showTrackMenu = true }
                    ), verticalAlignment = Alignment.CenterVertically) {
                        Text("${song.title} • ${song.artist}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        TextButton(onClick = { onRemoveFromPlaylist(playlist.id, song) }) { Text("Remove") }
                        TrackContextMenu(
                            song = song, playlists = playlists, expanded = showTrackMenu,
                            onExpandedChange = { showTrackMenu = it }, onPlayNow = onPlayNow,
                            onPlayNext = onPlayNext, onAddToQueue = onAddToQueue,
                            onAddToPlaylist = onAddToPlaylist, onCreatePlaylist = onCreatePlaylistWithSong
                        )
                    }
                }
            }
        }
        item { SectionTitle("About AudioBoX") }
        item { Text("AudioBoX • version $installedVersion", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
    }

    if (dialog != null) {
        val kind = dialog
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(when (kind) { "create" -> "Create playlist"; "rename" -> "Rename playlist"; else -> "Delete playlist?" }) },
            text = { if (kind == "delete") Text("The playlist and its references will be removed. Audio files will not be changed.") else OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Playlist name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                when (kind) {
                    "create" -> if (onCreatePlaylist(text)) dialog = null
                    "rename" -> if (selectedPlaylist?.let { onRenamePlaylist(it.id, text) } == true) dialog = null
                    "delete" -> { selectedPlaylist?.let { onDeletePlaylist(it.id) }; dialog = null }
                }
            }) { Text(if (kind == "delete") "Delete" else "Save") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
    }
    if (confirmCacheClear) AlertDialog(
        onDismissRequest = { confirmCacheClear = false },
        title = { Text("Clear cached lyrics?") },
        text = { Text("Cached LRCLIB results will be removed. Music files, playlists, and Jellyfin settings are unaffected.") },
        confirmButton = { TextButton(onClick = { onClearLyricsCache(); confirmCacheClear = false }) { Text("Clear cache") } },
        dismissButton = { TextButton(onClick = { confirmCacheClear = false }) { Text("Cancel") } }
    )
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, color = AudioboxAccent, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) }
@Composable private fun ChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { options.forEachIndexed { index, option ->
            FilterChip(selected == index, onClick = { onSelect(index) }, label = { Text(option) })
        } }
    }
}
@Composable private fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked, onCheckedChange = onChecked)
    }
}
