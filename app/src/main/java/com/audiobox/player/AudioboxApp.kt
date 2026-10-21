package com.audiobox.player

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.audiobox.player.ui.components.AudioOutputSheet
import com.audiobox.player.ui.components.MiniPlayer
import com.audiobox.player.ui.navigation.Screen
import com.audiobox.player.ui.screens.HomeScreen
import com.audiobox.player.ui.screens.LibraryScreen
import com.audiobox.player.ui.screens.NowPlayingScreen
import com.audiobox.player.ui.screens.SearchScreen
import com.audiobox.player.ui.screens.SettingsScreen
import com.audiobox.player.ui.components.JellyfinSettingsSheet
import com.audiobox.player.model.MusicSource
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.DarkBackground
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurface
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextSecondary
import com.audiobox.player.ui.viewmodel.NowPlayingViewModel
import kotlin.math.abs

@Composable
fun AudioboxApp(
    viewModel: NowPlayingViewModel
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Library scanner StateFlows
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()
    val sortedSongs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val musicSource by viewModel.musicSource.collectAsStateWithLifecycle()
    val jfConnection by viewModel.jellyfinConnectionState.collectAsStateWithLifecycle()
    val jfSession by viewModel.jellyfinSession.collectAsStateWithLifecycle()
    val jfSavedServerUrl by viewModel.jellyfinSavedServerUrl.collectAsStateWithLifecycle()
    val jfSavedUsername by viewModel.jellyfinSavedUsername.collectAsStateWithLifecycle()
    val jfSongs by viewModel.jellyfinSongs.collectAsStateWithLifecycle()
    val jfAlbums by viewModel.jellyfinAlbums.collectAsStateWithLifecycle()
    val jfArtists by viewModel.jellyfinArtists.collectAsStateWithLifecycle()
    val jfPlaylists by viewModel.jellyfinPlaylists.collectAsStateWithLifecycle()
    val jfLoading by viewModel.isJellyfinLoading.collectAsStateWithLifecycle()
    val jfError by viewModel.jellyfinErrorMessage.collectAsStateWithLifecycle()
    val appTheme by viewModel.preferences.theme.collectAsStateWithLifecycle()
    val appLanguage by viewModel.preferences.language.collectAsStateWithLifecycle()
    val minSizeMiB by viewModel.preferences.minimumSizeMiB.collectAsStateWithLifecycle()
    val onlineLyrics by viewModel.preferences.onlineLyrics.collectAsStateWithLifecycle()
    val dynamicColors by viewModel.preferences.dynamicColors.collectAsStateWithLifecycle()
    val jellyfinEnabled by viewModel.preferences.jellyfinEnabled.collectAsStateWithLifecycle()
    val localPlaylists by viewModel.preferences.playlists.collectAsStateWithLifecycle()

    var showHomeOutputSheet by remember { mutableStateOf(false) }
    var showJellyfinSettings by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Apple Music Inspired 3-tab Bottom Navigation
    val navigationItems = listOf(Screen.Home, Screen.Library, Screen.Search, Screen.Settings)
    val effectiveSongs = sortedSongs.ifEmpty { uiState.queue }
    val density = LocalDensity.current

    fun navigateToTab(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 0.75.dp
                    )
                    NavigationBar(
                        containerColor = DarkSurface.copy(alpha = 0.95f),
                        tonalElevation = 0.dp
                    ) {
                        navigationItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(screen) },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AudioboxAccent,
                                    selectedTextColor = AudioboxAccent,
                                    indicatorColor = Color.Transparent, // Clean Apple Music style without pill bubble
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.pointerInput(currentRoute) {
                        var horizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                horizontalDrag += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                val threshold = with(density) { 96.dp.toPx() }
                                val currentIndex = navigationItems.indexOfFirst { it.route == currentRoute }
                                if (abs(horizontalDrag) >= threshold && currentIndex >= 0) {
                                    val nextIndex = if (horizontalDrag < 0f) currentIndex + 1 else currentIndex - 1
                                    navigationItems.getOrNull(nextIndex)?.let(::navigateToTab)
                                }
                                horizontalDrag = 0f
                            },
                            onDragCancel = { horizontalDrag = 0f }
                        )
                    }
                ) {
                    // 1. Apple Music Inspired Home Screen
                    composable(Screen.Home.route) {
                        HomeScreen(
                            allSongs = effectiveSongs,
                            albums = albums,
                            favoriteSongIds = uiState.favoriteSongIds,
                            audioOutputState = uiState.audioOutputState,
                            currentSongId = uiState.currentSong?.id,
                            isPlaying = uiState.isPlaying,
                            onSongClick = { viewModel.playSong(it) },
                            onAlbumClick = { album ->
                                if (album.id.startsWith("jellyfin_album_")) {
                                    viewModel.playJellyfinAlbum(album)
                                } else {
                                    album.songs.firstOrNull()?.let { viewModel.playSong(it) }
                                }
                            },
                            onOpenAudioSettings = { showHomeOutputSheet = true },
                            onNavigateToLibrary = {
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // 2. Apple Music Inspired Library Screen
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            songs = effectiveSongs,
                            albums = albums,
                            artists = artists,
                            folders = folders,
                            musicSource = if (jellyfinEnabled) musicSource else MusicSource.LOCAL,
                            jellyfinConnectionState = jfConnection,
                            jellyfinSession = jfSession,
                            jellyfinSongs = jfSongs,
                            jellyfinAlbums = jfAlbums,
                            jellyfinArtists = jfArtists,
                            jellyfinPlaylists = jfPlaylists,
                            localPlaylists = localPlaylists,
                            isJellyfinLoading = jfLoading,
                            jellyfinError = jfError,
                            currentSongId = uiState.currentSong?.id,
                            isPlaying = uiState.isPlaying,
                            isScanning = isScanning,
                            hasPermission = hasPermission,
                            selectedTab = selectedTab,
                            selectedSort = selectedSort,
                            onTabSelect = { viewModel.selectLibraryTab(it) },
                            onSortSelect = { viewModel.selectSortOrder(it) },
                            onSongClick = { viewModel.playSong(it) },
                            onAlbumClick = { album ->
                                album.songs.firstOrNull()?.let { viewModel.playSong(it) }
                            },
                            onArtistClick = { artist ->
                                artist.songs.firstOrNull()?.let { viewModel.playSong(it) }
                            },
                            onFolderClick = { folder ->
                                folder.songs.firstOrNull()?.let { viewModel.playSong(it) }
                            },
                            onSelectMusicSource = { source ->
                                if (source == MusicSource.LOCAL) viewModel.selectMusicSource(source)
                                else if (jellyfinEnabled && jfSession != null) viewModel.selectMusicSource(source)
                                else if (jellyfinEnabled) showJellyfinSettings = true
                            },
                            onOpenJellyfinSettings = { showJellyfinSettings = true },
                            onRefreshJellyfin = { viewModel.refreshJellyfinMedia() },
                            onPlaylistClick = { playlist ->
                                if (playlist.songs.isNotEmpty()) viewModel.playJellyfinQueue(playlist.songs)
                            },
                            onLocalPlaylistClick = viewModel::playPlaylist,
                            onPlayNext = viewModel::playNext,
                            onAddToQueue = viewModel::addToQueue,
                            onAddToPlaylist = { id, song -> viewModel.addSongToPlaylist(id, song) },
                            onCreatePlaylist = { name, song -> viewModel.createPlaylist(name, song.id) },
                            onRescan = { viewModel.rescanLocalMedia() },
                            onRequestPermission = {
                                permissionLauncher.launch(permissionToRequest)
                            },
                            onOpenLocalAudio = { uri, fileName ->
                                viewModel.playLocalAudio(uri, fileName)
                            }
                        )
                    }

                    // 3. Apple Music Inspired Search Screen
                    composable(Screen.Search.route) {
                        SearchScreen(
                            allSongs = effectiveSongs,
                            onSongClick = { viewModel.playSong(it) },
                            playlists = localPlaylists,
                            onPlayNext = viewModel::playNext,
                            onAddToQueue = viewModel::addToQueue,
                            onAddToPlaylist = { id, song -> viewModel.addSongToPlaylist(id, song) },
                            onCreatePlaylist = { name, song -> viewModel.createPlaylist(name, song.id) }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            theme = appTheme, language = appLanguage, minimumSizeMiB = minSizeMiB,
                            onlineLyrics = onlineLyrics, dynamicColors = dynamicColors, jellyfinEnabled = jellyfinEnabled,
                            jellyfinStatus = jfConnection.name,
                            jellyfinError = jfError,
                            playlists = localPlaylists, songs = (effectiveSongs + jfSongs).distinctBy { it.id },
                            trackCount = sortedSongs.size, albumCount = albums.size, artistCount = artists.size,
                            totalBytes = sortedSongs.sumOf { it.sizeBytes }, isScanning = isScanning, scanError = scanMessage,
                            onTheme = viewModel::setAppTheme, onLanguage = { language ->
                                viewModel.setAppLanguage(language)
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) (context as? Activity)?.recreate()
                            },
                            onMinimumSize = viewModel::setMinimumSizeMiB, onOnlineLyrics = viewModel::setOnlineLyrics,
                            onDynamicColors = viewModel::setDynamicColors,
                            onJellyfinEnabled = { enabled ->
                                viewModel.setJellyfinEnabled(enabled)
                                if (!enabled) {
                                    viewModel.selectMusicSource(MusicSource.LOCAL)
                                } else if (jfSession != null) {
                                    viewModel.selectMusicSource(MusicSource.JELLYFIN)
                                } else {
                                    viewModel.selectMusicSource(MusicSource.LOCAL)
                                    showJellyfinSettings = true
                                }
                            },
                            onRescan = viewModel::rescanLocalMedia,
                            onJellyfinSettings = { showJellyfinSettings = true },
                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onRemoveFromPlaylist = viewModel::removeSongFromPlaylist,
                            onPlayPlaylist = viewModel::playPlaylist,
                            onPlayNow = viewModel::playSong,
                            onPlayNext = viewModel::playNext,
                            onAddToQueue = viewModel::addToQueue,
                            onAddToPlaylist = { id, song -> viewModel.addSongToPlaylist(id, song) },
                            onCreatePlaylistWithSong = { name, song -> viewModel.createPlaylist(name, song.id) },
                            onClearLyricsCache = viewModel::clearLyricsCache
                        )
                    }
                }

                // Apple Music Floating MiniPlayer Capsule
                AnimatedVisibility(
                    visible = uiState.currentSong != null && !uiState.isNowPlayingExpanded,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    MiniPlayer(
                        song = uiState.currentSong,
                        isPlaying = uiState.isPlaying,
                        progress = uiState.progress,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onExpand = { viewModel.setNowPlayingExpanded(true) }
                    )
                }
            }
        }

        // Now Playing Layar Penuh Modal
        AnimatedVisibility(
            visible = uiState.isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                state = uiState,
                dynamicColors = dynamicColors,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSeek = { viewModel.seekToProgress(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onLyricClick = { viewModel.seekToPosition(it) },
                onLyricsModeChange = { viewModel.setLyricsMode(it) },
                onToggleBitPerfect = { viewModel.setBitPerfectRequested(it) },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onSongClick = { viewModel.playSong(it) },
                onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onRemoveQueueItem = { index -> viewModel.removeQueueItem(index) },
                onRetryLyrics = { viewModel.retryFetchLyrics() },
                onCollapse = { viewModel.setNowPlayingExpanded(false) }
            )
        }

        // Audio Output Settings Sheet from Home Screen Quick Access
        if (showHomeOutputSheet) {
            AudioOutputSheet(
                outputState = uiState.audioOutputState,
                streamInfo = uiState.audioStreamInfo,
                onToggleBitPerfect = { viewModel.setBitPerfectRequested(it) },
                onDismiss = { showHomeOutputSheet = false }
            )
        }
        if (showJellyfinSettings) {
            JellyfinSettingsSheet(
                connectionState = jfConnection,
                currentSession = jfSession,
                savedServerUrl = jfSavedServerUrl,
                savedUsername = jfSavedUsername,
                isLoading = jfLoading,
                errorMessage = jfError,
                onConnect = { url, user, password -> viewModel.connectJellyfin(url, user, password) { success, _ ->
                    if (success && jellyfinEnabled) viewModel.selectMusicSource(MusicSource.JELLYFIN)
                } },
                onRefresh = viewModel::refreshJellyfinMedia,
                onDisconnect = viewModel::disconnectJellyfin,
                onDismiss = { showJellyfinSettings = false }
            )
        }
    }
}
