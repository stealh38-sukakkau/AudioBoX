package com.audiobox.player.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.audiobox.player.data.jellyfin.JellyfinConnectionState
import com.audiobox.player.data.jellyfin.JellyfinSession
import com.audiobox.player.model.Album
import com.audiobox.player.model.Artist
import com.audiobox.player.model.JellyfinPlaylist
import com.audiobox.player.model.LibraryTab
import com.audiobox.player.model.LocalPlaylist
import com.audiobox.player.model.MusicFolder
import com.audiobox.player.model.MusicSource
import com.audiobox.player.model.Song
import com.audiobox.player.model.SortOrder
import com.audiobox.player.ui.components.formatDuration
import com.audiobox.player.ui.components.TrackContextMenu
import com.audiobox.player.ui.components.jellyfinArtworkRequest
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.AudioboxSecondary
import com.audiobox.player.ui.theme.DarkBackground
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextPrimary
import com.audiobox.player.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    folders: List<MusicFolder>,
    musicSource: MusicSource = MusicSource.LOCAL,
    jellyfinConnectionState: JellyfinConnectionState = JellyfinConnectionState.DISCONNECTED,
    jellyfinSession: JellyfinSession? = null,
    jellyfinSongs: List<Song> = emptyList(),
    jellyfinAlbums: List<Album> = emptyList(),
    jellyfinArtists: List<Artist> = emptyList(),
    jellyfinPlaylists: List<JellyfinPlaylist> = emptyList(),
    localPlaylists: List<LocalPlaylist> = emptyList(),
    isJellyfinLoading: Boolean = false,
    jellyfinError: String? = null,
    currentSongId: String?,
    isPlaying: Boolean,
    isScanning: Boolean,
    hasPermission: Boolean,
    selectedTab: LibraryTab,
    selectedSort: SortOrder,
    onTabSelect: (LibraryTab) -> Unit,
    onSortSelect: (SortOrder) -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onFolderClick: (MusicFolder) -> Unit,
    onPlaylistClick: (JellyfinPlaylist) -> Unit = {},
    onLocalPlaylistClick: (List<Song>) -> Unit = {},
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (String, Song) -> Unit = { _, _ -> },
    onCreatePlaylist: (String, Song) -> Boolean = { _, _ -> false },
    onSelectMusicSource: (MusicSource) -> Unit = {},
    onOpenJellyfinSettings: () -> Unit = {},
    onRescan: () -> Unit,
    onRefreshJellyfin: () -> Unit = {},
    onRequestPermission: () -> Unit,
    onOpenLocalAudio: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Local Audio"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                fileName = uri.lastPathSegment ?: "Local Audio"
            }
            onOpenLocalAudio(uri, fileName)
        }
    }

    val isJellyfin = musicSource == MusicSource.JELLYFIN
    val isJellyfinConnected = jellyfinConnectionState == JellyfinConnectionState.CONNECTED
    val activeSongs = if (isJellyfin) jellyfinSongs else songs
    val activeAlbums = if (isJellyfin) jellyfinAlbums else albums
    val activeArtists = if (isJellyfin) jellyfinArtists else artists

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // ─── Apple Music Large Title & Actions ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val countText = if (isJellyfin) {
                    if (!isJellyfinConnected) {
                        "Jellyfin Server • Disconnected"
                    } else if (isJellyfinLoading) {
                        "Syncing with Jellyfin..."
                    } else {
                        when (selectedTab) {
                            LibraryTab.SONGS -> "${jellyfinSongs.size} songs on server"
                            LibraryTab.ALBUMS -> "${jellyfinAlbums.size} albums"
                            LibraryTab.ARTISTS -> "${jellyfinArtists.size} artists"
                            LibraryTab.PLAYLISTS -> "${jellyfinPlaylists.size} playlists"
                            else -> "${jellyfinSongs.size} items"
                        }
                    }
                } else {
                    when (selectedTab) {
                        LibraryTab.SONGS -> "${songs.size} songs"
                        LibraryTab.ALBUMS -> "${albums.size} albums"
                        LibraryTab.ARTISTS -> "${artists.size} artists"
                        LibraryTab.FOLDERS -> "${folders.size} folders"
                        LibraryTab.PLAYLISTS -> "${localPlaylists.size} playlists"
                    }
                }
                Text(
                    text = if (isScanning) "Scanning local music..." else countText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sort Dropdown Menu
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort Options",
                            tint = AudioboxAccent
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = DarkSurfaceElevated
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        color = if (order == selectedSort) AudioboxAccent else TextPrimary,
                                        fontWeight = if (order == selectedSort) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortSelect(order)
                                    showSortMenu = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = TextPrimary
                                )
                            )
                        }
                    }
                }

                if (isJellyfin) {
                    // Jellyfin Refresh / Loading
                    if (isJellyfinLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AudioboxAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefreshJellyfin) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh Jellyfin Media",
                                tint = AudioboxAccent
                            )
                        }
                    }

                    // Jellyfin Server Settings
                    IconButton(onClick = onOpenJellyfinSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Jellyfin Server Settings",
                            tint = AudioboxAccent
                        )
                    }
                } else {
                    // Rescan Local Button
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AudioboxAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRescan) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Rescan Library",
                                tint = AudioboxAccent
                            )
                        }
                    }
                }
            }
        }

        // ─── Music Source Segmented Control (Local vs Jellyfin) ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isLocalSelected = musicSource == MusicSource.LOCAL
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLocalSelected) AudioboxAccent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isLocalSelected) AudioboxAccent.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectMusicSource(MusicSource.LOCAL) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = if (isLocalSelected) AudioboxAccent else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "On Device",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isLocalSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLocalSelected) TextPrimary else TextSecondary
                )
            }

            val isJellyfinSelected = musicSource == MusicSource.JELLYFIN
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isJellyfinSelected) AudioboxAccent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isJellyfinSelected) AudioboxAccent.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectMusicSource(MusicSource.JELLYFIN) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isJellyfinConnected) Icons.Rounded.CloudDone else Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = if (isJellyfinSelected) AudioboxAccent else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Jellyfin Server",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isJellyfinSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isJellyfinSelected) TextPrimary else TextSecondary
                )
            }
        }

        // ─── Jellyfin Status or Local Permission Banner ─────────────────────
        if (isJellyfin) {
            if (isJellyfinConnected) {
                // Jellyfin Connected Status Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, AudioboxAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = AudioboxAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = jellyfinSession?.serverName ?: "Jellyfin Server",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Connected as ${jellyfinSession?.userName ?: "User"} • Direct Play",
                            style = MaterialTheme.typography.bodySmall,
                            color = AudioboxAccent
                        )
                    }
                    IconButton(
                        onClick = onOpenJellyfinSettings,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Server Settings",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (jellyfinError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B1219))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = jellyfinError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Local Permission Banner
            if (!hasPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, AudioboxAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onRequestPermission() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AudioboxAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AudioboxAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Access Local Music Library",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to grant permission and scan songs on your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // ─── Disconnected Jellyfin Prompt ───────────────────────────────────
        if (isJellyfin && !isJellyfinConnected) {
            JellyfinDisconnectedCard(onConnectClick = onOpenJellyfinSettings)
        } else {
            // ─── Apple Music Scrollable Category Tabs ───────────────────────────
            val availableTabs = if (isJellyfin) {
                listOf(LibraryTab.SONGS, LibraryTab.ALBUMS, LibraryTab.ARTISTS, LibraryTab.PLAYLISTS)
            } else {
                listOf(LibraryTab.SONGS, LibraryTab.ALBUMS, LibraryTab.ARTISTS, LibraryTab.FOLDERS)
            }

            val currentTab = if (selectedTab in availableTabs) selectedTab else availableTabs.first()

            ScrollableTabRow(
                selectedTabIndex = availableTabs.indexOf(currentTab).coerceAtLeast(0),
                containerColor = DarkBackground,
                contentColor = AudioboxAccent,
                edgePadding = 20.dp,
                indicator = { tabPositions ->
                    val idx = availableTabs.indexOf(currentTab).coerceAtLeast(0)
                    if (idx in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                            color = AudioboxAccent
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)
                }
            ) {
                availableTabs.forEach { tab ->
                    Tab(
                        selected = tab == currentTab,
                        onClick = { onTabSelect(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (tab == currentTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (tab == currentTab) AudioboxAccent else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── SAF File Picker Pill (Local only) ──────────────────────────────
            if (!isJellyfin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .clickable { openDocumentLauncher.launch(arrayOf("audio/*")) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = AudioboxAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pick Local Audio File",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Browse storage via Android document picker",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Tab View Content ───────────────────────────────────────────────
            when (currentTab) {
                LibraryTab.SONGS -> SongsTab(
                    songs = activeSongs,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongClick = onSongClick,
                    playlists = localPlaylists,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onCreatePlaylist = onCreatePlaylist
                )
                LibraryTab.ALBUMS -> AlbumsTab(
                    albums = activeAlbums,
                    onAlbumClick = onAlbumClick
                )
                LibraryTab.ARTISTS -> ArtistsTab(
                    artists = activeArtists,
                    onArtistClick = onArtistClick
                )
                LibraryTab.FOLDERS -> FoldersTab(
                    folders = folders,
                    onFolderClick = onFolderClick
                )
                LibraryTab.PLAYLISTS -> if (isJellyfin) {
                    PlaylistsTab(playlists = jellyfinPlaylists, onPlaylistClick = onPlaylistClick)
                } else {
                    LocalPlaylistsTab(
                        playlists = localPlaylists,
                        songs = (songs + jellyfinSongs).distinctBy { it.id },
                        onPlay = onLocalPlaylistClick,
                        onPlayNow = onSongClick,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onCreatePlaylist = onCreatePlaylist
                    )
                }
            }
        }
    }
}

// ─── Songs Tab ────────────────────────────────────────────────────────────────
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SongsTab(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    playlists: List<LocalPlaylist>,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onCreatePlaylist: (String, Song) -> Boolean
) {
    if (songs.isEmpty()) {
        EmptyLibraryState(message = "No songs found in library.\nGrant audio access or tap Rescan.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            val isCurrentSong = song.id == currentSongId
            var showTrackMenu by remember(song.id) { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrentSong) AudioboxAccent.copy(alpha = 0.12f) else Color.Transparent)
                    .combinedClickable(onClick = { onSongClick(song) }, onLongClick = { showTrackMenu = true })
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(artworkUrl = song.artworkUrl, title = song.title, size = 48)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCurrentSong) AudioboxAccent else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isCurrentSong && isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Playing",
                        tint = AudioboxAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = if (song.durationMs > 0) formatDuration(song.durationMs) else "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                TrackContextMenu(
                    song = song, playlists = playlists, expanded = showTrackMenu,
                    onExpandedChange = { showTrackMenu = it }, onPlayNow = onSongClick,
                    onPlayNext = onPlayNext, onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist, onCreatePlaylist = onCreatePlaylist
                )
            }
        }
    }
}

// ─── Albums Tab (Apple Music 2-Column Grid) ───────────────────────────────────
@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyLibraryState(message = "No albums found.\nScan local storage to discover albums.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
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
                                .background(AudioboxAccent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = AudioboxAccent,
                                modifier = Modifier.size(48.dp)
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

// ─── Artists Tab ──────────────────────────────────────────────────────────────
@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyLibraryState(message = "No artists found.\nScan local storage to discover artists.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onArtistClick(artist) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AudioboxAccent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = AudioboxAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.trackCount} songs • ${artist.albumCount} albums",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Folders Tab ──────────────────────────────────────────────────────────────
@Composable
private fun FoldersTab(
    folders: List<MusicFolder>,
    onFolderClick: (MusicFolder) -> Unit
) {
    if (folders.isEmpty()) {
        EmptyLibraryState(message = "No folders found.\nScan local storage to discover folders.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(folders, key = { it.path }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onFolderClick(folder) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AudioboxAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = AudioboxAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.trackCount} tracks • ${folder.path}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SongArtwork(
    artworkUrl: String,
    title: String,
    size: Int
) {
    if (artworkUrl.isNotBlank()) {
        AsyncImage(
            model = jellyfinArtworkRequest(artworkUrl),
            contentDescription = title,
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

@Composable
private fun EmptyLibraryState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Playlists Tab (Jellyfin Playlists) ─────────────────────────────────────────
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LocalPlaylistsTab(
    playlists: List<LocalPlaylist>,
    songs: List<Song>,
    onPlay: (List<Song>) -> Unit,
    onPlayNow: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onCreatePlaylist: (String, Song) -> Boolean
) {
    if (playlists.isEmpty()) {
        EmptyLibraryState(message = "No local playlists yet. Create one in Settings or add a track from its actions menu.")
        return
    }
    var expandedId by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            val tracks = playlist.songIds.mapNotNull { id -> songs.find { it.id == id } }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkSurfaceElevated).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { expandedId = if (expandedId == playlist.id) null else playlist.id }) {
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${tracks.size} tracks${if (tracks.size < playlist.songIds.size) " • ${playlist.songIds.size - tracks.size} unavailable" else ""}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    IconButton(onClick = { onPlay(tracks) }, enabled = tracks.isNotEmpty()) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Play playlist", tint = AudioboxAccent)
                    }
                }
                if (expandedId == playlist.id) {
                    tracks.forEach { song ->
                        var showTrackMenu by remember(playlist.id, song.id) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onPlay(listOf(song)) },
                                    onLongClick = { showTrackMenu = true }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${song.title}  •  ${song.artist}",
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TrackContextMenu(
                                song = song,
                                playlists = playlists,
                                expanded = showTrackMenu,
                                onExpandedChange = { showTrackMenu = it },
                                onPlayNow = onPlayNow,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                                onAddToPlaylist = onAddToPlaylist,
                                onCreatePlaylist = onCreatePlaylist
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<JellyfinPlaylist>,
    onPlaylistClick: (JellyfinPlaylist) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyLibraryState(message = "No playlists found on Jellyfin server.\nCreate playlists on your server to see them here.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaylistClick(playlist) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                ) {
                    if (playlist.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = jellyfinArtworkRequest(playlist.artworkUrl),
                            contentDescription = playlist.name,
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
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = AudioboxAccent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.trackCount} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Jellyfin Disconnected Card ───────────────────────────────────────────────
@Composable
private fun JellyfinDisconnectedCard(
    onConnectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AudioboxAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = AudioboxAccent,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connect Jellyfin Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stream your lossless music library, albums, artists, and playlists directly from your personal Jellyfin media server.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AudioboxAccent)
                    .clickable { onConnectClick() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Server & Login",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
