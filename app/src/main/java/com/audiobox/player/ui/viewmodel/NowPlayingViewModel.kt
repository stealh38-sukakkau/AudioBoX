package com.audiobox.player.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiobox.player.data.MusicRepository
import com.audiobox.player.data.UserPreferences
import com.audiobox.player.data.AppThemeChoice
import com.audiobox.player.data.AppLanguageChoice
import com.audiobox.player.model.Album
import com.audiobox.player.model.AlbumColorPalette
import com.audiobox.player.model.Artist
import com.audiobox.player.model.LibraryTab
import com.audiobox.player.model.LyricsDisplayMode
import com.audiobox.player.model.MusicFolder
import com.audiobox.player.model.NowPlayingUiState
import com.audiobox.player.model.Song
import com.audiobox.player.model.SortOrder
import com.audiobox.player.data.jellyfin.JellyfinConnectionState
import com.audiobox.player.data.jellyfin.JellyfinRepository
import com.audiobox.player.data.jellyfin.JellyfinSession
import com.audiobox.player.model.JellyfinPlaylist
import com.audiobox.player.model.MusicSource
import com.audiobox.player.data.lyrics.LyricsRepository
import com.audiobox.player.model.LyricLine
import com.audiobox.player.player.AudioboxPlayerController
import com.audiobox.player.player.QueueOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NowPlayingViewModel(
    application: Application,
    private val repository: MusicRepository,
    val preferences: UserPreferences = UserPreferences(application.applicationContext)
) : AndroidViewModel(application) {

    private val playerController = AudioboxPlayerController(
        context = application.applicationContext,
        scope = viewModelScope
    )
    val uiState: StateFlow<NowPlayingUiState> = playerController.state

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOrder.TITLE_ASC)
    val selectedSort: StateFlow<SortOrder> = _selectedSort.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(checkPermission())
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private val _sortedSongs = MutableStateFlow<List<Song>>(emptyList())
    val sortedSongs: StateFlow<List<Song>> = _sortedSongs.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var debounceJob: Job? = null
    private var localSongCatalog: List<Song> = emptyList()

    private val lyricsRepository = LyricsRepository(application.applicationContext)
    private var lyricsFetchJob: Job? = null
    private var lastFetchedSongId: String? = null
    private var playbackExpiryHandled = false

    // Sumber Koleksi Musik: Lokal vs Jellyfin Server
    private val _musicSource = MutableStateFlow(MusicSource.LOCAL)
    val musicSource: StateFlow<MusicSource> = _musicSource.asStateFlow()

    // Integrasi Jellyfin
    private val jellyfinRepository = JellyfinRepository(application.applicationContext, viewModelScope)
    val jellyfinConnectionState: StateFlow<JellyfinConnectionState> = jellyfinRepository.connectionState
    val jellyfinSession: StateFlow<JellyfinSession?> = jellyfinRepository.session
    val jellyfinSavedServerUrl: StateFlow<String> = jellyfinRepository.savedServerUrl
    val jellyfinSavedUsername: StateFlow<String> = jellyfinRepository.savedUsername
    val jellyfinSongs: StateFlow<List<Song>> = jellyfinRepository.songs
    val jellyfinAlbums: StateFlow<List<Album>> = jellyfinRepository.albums
    val jellyfinArtists: StateFlow<List<Artist>> = jellyfinRepository.artists
    val jellyfinPlaylists: StateFlow<List<JellyfinPlaylist>> = jellyfinRepository.playlists
    val isJellyfinLoading: StateFlow<Boolean> = jellyfinRepository.isLoading
    val jellyfinErrorMessage: StateFlow<String?> = jellyfinRepository.errorMessage

    init {
        viewModelScope.launch {
            jellyfinSession.collect { session ->
                if (session == null) _musicSource.value = MusicSource.LOCAL
            }
        }

        // Amati antrean lagu dan perbarui kategori
        viewModelScope.launch {
            repository.getAllSongs().collect { songs ->
                localSongCatalog = songs
                updateCategoriesAndSorted(songs, _selectedSort.value)
                playerController.updateSongCatalog(songs + jellyfinSongs.value)
                playerController.initializeQueueIfEmpty(songs, songs + jellyfinSongs.value)
            }
        }

        viewModelScope.launch {
            jellyfinSongs.collect { songs ->
                playerController.updateSongCatalog(localSongCatalog + songs)
            }
        }

        // Amati perubahan lagu untuk otomatis memuat lirik via LRCLIB
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.playbackError == null) playbackExpiryHandled = false
                if (!playbackExpiryHandled && state.playbackError?.contains("session expired", ignoreCase = true) == true) {
                    playbackExpiryHandled = true
                    jellyfinRepository.markSessionExpired()
                }
                val song = state.currentSong
                if (song != null && song.id != lastFetchedSongId) {
                    lastFetchedSongId = song.id
                    fetchLyricsForSong(song, forceRefresh = false)
                }
            }
        }

        // Pindai otomatis jika izin sudah ada
        if (_hasStoragePermission.value) {
            rescanLocalMedia()
        }

        // Pasang ContentObserver untuk deteksi perubahan MediaStore otomatis
        contentObserver = repository.registerObserver {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(1200L) // Debounce 1.2 detik saat banyak file dimodifikasi
                rescanLocalMedia()
            }
        }
    }

    private fun checkPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionResult(isGranted: Boolean) {
        _hasStoragePermission.value = isGranted
        if (isGranted) {
            rescanLocalMedia()
        }
    }

    fun rescanLocalMedia() {
        if (!_hasStoragePermission.value) {
            _scanMessage.value = "Grant audio access in Library before scanning."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanMessage.value = null
            try {
                val updated = repository.refreshLocalMedia()
                updateCategoriesAndSorted(updated, _selectedSort.value)
                _scanMessage.value = "Scan complete: ${updated.size} indexed tracks."
            } catch (e: Exception) {
                _scanMessage.value = e.localizedMessage ?: "Could not scan local music."
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectLibraryTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun selectSortOrder(order: SortOrder) {
        _selectedSort.value = order
        val currentSongs = uiState.value.queue
        updateCategoriesAndSorted(currentSongs, order)
    }

    private fun updateCategoriesAndSorted(songs: List<Song>, sortOrder: SortOrder) {
        _sortedSongs.value = repository.sortSongs(songs, sortOrder)
        _albums.value = repository.getAlbums()
        _artists.value = repository.getArtists()
        _folders.value = repository.getFolders()
    }

    fun playSong(song: Song) {
        if (song.source == "JELLYFIN") {
            playJellyfinSelection(jellyfinSongs.value, song)
        } else if (uiState.value.queue.any { it.source != song.source }) {
            playSourceSelection(sortedSongs.value, song)
        } else {
            playerController.loadSong(song, autoPlay = true)
            playerController.setNowPlayingExpanded(true)
        }
    }

    fun playJellyfinAlbum(album: Album) {
        val existingSongs = album.songs
        if (existingSongs.isNotEmpty()) {
            playJellyfinQueue(existingSongs)
            return
        }

        viewModelScope.launch {
            jellyfinRepository.fetchAlbumSongs(album.id).onSuccess { albumSongs ->
                if (albumSongs.isNotEmpty()) playJellyfinQueue(albumSongs)
            }
        }
    }

    fun playLocalAudio(uri: Uri, fileName: String) {
        val cleanTitle = fileName.substringBeforeLast(".")
        val localSong = Song(
            id = "local_${System.currentTimeMillis()}",
            title = cleanTitle,
            artist = "Local Device Audio",
            album = "On Device",
            durationMs = 0L,
            artworkUrl = "",
            audioUrl = uri.toString(),
            colorPalette = AlbumColorPalette(
                primary = Color(0xFF3B82F6),
                secondary = Color(0xFF8B5CF6),
                background = Color(0xFF0A0F1D)
            ),
            lyrics = emptyList()
        )
        playerController.addSongAndPlay(localSong)
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun seekToProgress(progress: Float) = playerController.seekToProgress(progress)
    fun seekToPosition(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleRepeat() = playerController.toggleRepeat()
    fun setLyricsMode(mode: LyricsDisplayMode) = playerController.setLyricsMode(mode)
    fun setNowPlayingExpanded(expanded: Boolean) = playerController.setNowPlayingExpanded(expanded)
    fun setBitPerfectRequested(enabled: Boolean) = playerController.setBitPerfectRequested(enabled)
    fun toggleFavorite(songId: String? = null) = playerController.toggleFavorite(songId)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playerController.moveQueueItem(fromIndex, toIndex)
    fun removeQueueItem(index: Int) = playerController.removeQueueItem(index)
    fun addToQueue(song: Song) = playerController.addToQueue(song)
    fun playNext(song: Song) = playerController.playNext(song)
    fun setMinimumSizeMiB(value: Int) {
        preferences.setMinimumSizeMiB(value)
        rescanLocalMedia()
    }
    fun setAppTheme(value: AppThemeChoice) = preferences.setTheme(value)
    fun setAppLanguage(value: AppLanguageChoice) {
        preferences.setLanguage(value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tags = when (value) {
                AppLanguageChoice.SYSTEM -> ""
                AppLanguageChoice.INDONESIAN -> "id"
                AppLanguageChoice.ENGLISH -> "en"
            }
            getApplication<Application>().getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales = android.os.LocaleList.forLanguageTags(tags)
        }
    }
    fun setOnlineLyrics(enabled: Boolean) {
        preferences.setOnlineLyrics(enabled)
        if (!enabled) uiState.value.currentSong?.let { fetchLyricsForSong(it, forceRefresh = false) }
    }
    fun setDynamicColors(enabled: Boolean) = preferences.setDynamicColors(enabled)
    fun setJellyfinEnabled(enabled: Boolean) = preferences.setJellyfinEnabled(enabled)
    fun createPlaylist(name: String, songId: String? = null) = preferences.createPlaylist(name, songId)
    fun renamePlaylist(id: String, name: String) = preferences.renamePlaylist(id, name)
    fun deletePlaylist(id: String) = preferences.deletePlaylist(id)
    fun addSongToPlaylist(playlistId: String, song: Song) = preferences.addSong(playlistId, song.id)
    fun removeSongFromPlaylist(playlistId: String, song: Song) = preferences.removeSong(playlistId, song.id)
    fun clearLyricsCache() = lyricsRepository.clearCache()

    fun retryFetchLyrics() {
        val song = uiState.value.currentSong ?: return
        fetchLyricsForSong(song, forceRefresh = true)
    }

    private fun fetchLyricsForSong(song: Song, forceRefresh: Boolean) {
        lyricsFetchJob?.cancel()

        if (!preferences.onlineLyrics.value && song.lyrics.isEmpty()) {
            val cached = lyricsRepository.getCachedLyrics(song)
            playerController.setLyricsState(
                syncedLines = cached?.syncedLines.orEmpty(),
                isSynced = cached?.isSynced == true,
                plainText = cached?.plainText,
                isLoading = false,
                error = null
            )
            return
        }

        // Jika lagu sudah memiliki lirik bawaan dan bukan refresh paksa, gunakan langsung
        if (song.lyrics.isNotEmpty() && !forceRefresh) {
            playerController.setLyricsState(
                syncedLines = song.lyrics,
                isSynced = true,
                plainText = null,
                isLoading = false,
                error = null
            )
            return
        }

        lyricsFetchJob = viewModelScope.launch(Dispatchers.IO) {
            playerController.setLyricsState(
                syncedLines = emptyList(),
                isSynced = false,
                plainText = null,
                isLoading = true,
                error = null
            )

            val result = lyricsRepository.getLyrics(song, forceRefresh = forceRefresh)
            result.onSuccess { data ->
                playerController.setLyricsState(
                    syncedLines = data.syncedLines,
                    isSynced = data.isSynced,
                    plainText = data.plainText,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                playerController.setLyricsState(
                    syncedLines = emptyList(),
                    isSynced = false,
                    plainText = null,
                    isLoading = false,
                    error = exception.message ?: "Lyrics not found on LRCLIB."
                )
            }
        }
    }

    // Jellyfin Actions
    fun selectMusicSource(source: MusicSource) {
        _musicSource.value = source
    }

    fun connectJellyfin(
        serverUrl: String,
        username: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = jellyfinRepository.connect(serverUrl, username, password)
            result.onSuccess {
                onComplete(true, null)
            }.onFailure { error ->
                onComplete(false, error.message ?: "Koneksi gagal")
            }
        }
    }

    fun disconnectJellyfin() {
        viewModelScope.launch {
            jellyfinRepository.disconnect()
            _musicSource.value = MusicSource.LOCAL
        }
    }

    fun refreshJellyfinMedia() {
        viewModelScope.launch {
            jellyfinRepository.refreshAll()
        }
    }

    fun clearJellyfinError() {
        jellyfinRepository.clearError()
    }

    fun playJellyfinQueue(songs: List<Song>, startSongId: String? = null) {
        val selected = if (startSongId == null) {
            songs.firstOrNull()
        } else {
            songs.firstOrNull { it.id == startSongId }
        } ?: return
        playJellyfinSelection(songs, selected)
    }

    private fun playJellyfinSelection(candidates: List<Song>, selected: Song) {
        if (selected.source != "JELLYFIN") return
        playSourceSelection(candidates, selected)
    }

    private fun playSourceSelection(candidates: List<Song>, selected: Song) {
        val selection = QueueOperations.sourceQueueForSelection(
            candidates = candidates,
            selected = selected,
            idOf = { it.id },
            sourceOf = { it.source }
        )
        playerController.setQueue(selection.items, selection.items[selection.index].id, autoPlay = true)
        playerController.setNowPlayingExpanded(true)
    }
    fun playPlaylist(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            playerController.setQueue(songs, songs.first().id, autoPlay = true)
            playerController.setNowPlayingExpanded(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        lyricsFetchJob?.cancel()
        contentObserver?.let { repository.unregisterObserver(it) }
        playerController.release()
    }
}
