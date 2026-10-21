package com.audiobox.player.data.jellyfin

import android.content.Context
import com.audiobox.player.model.Album
import com.audiobox.player.model.Artist
import com.audiobox.player.model.JellyfinPlaylist
import com.audiobox.player.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repositori utama untuk mengelola sesi Jellyfin, sinkronisasi koleksi jarak jauh,
 * dan penyediaan data lagu, album, artis, dan playlist.
 */
class JellyfinRepository(
    context: Context,
    private val scope: CoroutineScope
) {
    private val authManager = JellyfinAuthManager(context)
    private val apiClient = JellyfinApiClient(authManager.deviceId)
    private var refreshJob: Job? = null
    @Volatile private var generation: Long = 0L

    private val _connectionState = MutableStateFlow(
        if (authManager.isConnected()) JellyfinConnectionState.CONNECTED else JellyfinConnectionState.DISCONNECTED
    )
    val connectionState: StateFlow<JellyfinConnectionState> = _connectionState.asStateFlow()

    private val _session = MutableStateFlow(authManager.getSession())
    val session: StateFlow<JellyfinSession?> = _session.asStateFlow()
    private val _savedServerUrl = MutableStateFlow(authManager.savedServerUrl().orEmpty())
    val savedServerUrl: StateFlow<String> = _savedServerUrl.asStateFlow()
    private val _savedUsername = MutableStateFlow(authManager.savedUsername().orEmpty())
    val savedUsername: StateFlow<String> = _savedUsername.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _playlists = MutableStateFlow<List<JellyfinPlaylist>>(emptyList())
    val playlists: StateFlow<List<JellyfinPlaylist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Jika sudah tersambung sebelumnya, muat koleksi awal
        if (authManager.isConnected()) {
            refreshLibrary()
        }
    }

    suspend fun connect(
        serverUrl: String,
        username: String,
        password: String
    ): Result<JellyfinSession> = withContext(Dispatchers.IO) {
        generation++
        refreshJob?.cancel()
        _isLoading.value = true
        _errorMessage.value = null
        _connectionState.value = JellyfinConnectionState.CONNECTING

        val result = apiClient.authenticate(serverUrl, username, password)
        result.onSuccess { newSession ->
            try {
                authManager.saveSession(newSession)
                _session.value = newSession
                _savedServerUrl.value = newSession.serverUrl
                _savedUsername.value = newSession.username
                _connectionState.value = JellyfinConnectionState.CONNECTED
                _isLoading.value = false
                refreshLibrary()
            } catch (_: Exception) {
                _connectionState.value = JellyfinConnectionState.ERROR
                _errorMessage.value = "Could not securely save the Jellyfin session. Please reconnect."
                _isLoading.value = false
            }
        }.onFailure { e ->
            _connectionState.value = JellyfinConnectionState.ERROR
            _errorMessage.value = e.message ?: "Authentication failed."
            _isLoading.value = false
        }

        if (_connectionState.value == JellyfinConnectionState.ERROR && result.isSuccess) {
            Result.failure(IllegalStateException(_errorMessage.value))
        } else result
    }

    fun disconnect() {
        generation++
        refreshJob?.cancel()
        refreshJob = null
        authManager.clearSession(preserveServerConfig = true)
        _session.value = null
        _savedServerUrl.value = authManager.savedServerUrl().orEmpty()
        _savedUsername.value = authManager.savedUsername().orEmpty()
        _connectionState.value = JellyfinConnectionState.DISCONNECTED
        _songs.value = emptyList()
        _albums.value = emptyList()
        _artists.value = emptyList()
        _playlists.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
    }

    fun markSessionExpired() {
        generation++
        refreshJob?.cancel()
        authManager.clearSession(preserveServerConfig = true)
        _session.value = null
        _savedServerUrl.value = authManager.savedServerUrl().orEmpty()
        _savedUsername.value = authManager.savedUsername().orEmpty()
        _connectionState.value = JellyfinConnectionState.ERROR
        _errorMessage.value = "Jellyfin session expired. Reconnect to continue."
        _isLoading.value = false
        _songs.value = emptyList()
        _albums.value = emptyList()
        _artists.value = emptyList()
        _playlists.value = emptyList()
    }

    fun refreshLibrary() {
        val currentSession = _session.value ?: return

        refreshJob?.cancel()
        val requestGeneration = generation
        refreshJob = scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null
            _connectionState.value = JellyfinConnectionState.CONNECTING

            // 1. Ambil lagu
            val songsResult = apiClient.fetchAllSongs(currentSession)
            currentCoroutineContext().ensureActive()
            if (requestGeneration != generation) return@launch
            songsResult.onSuccess { songList ->
                _songs.value = songList
                _connectionState.value = JellyfinConnectionState.CONNECTED
            }.onFailure { e ->
                val message = e.message.orEmpty()
                _errorMessage.value = when {
                    message.contains("session expired", ignoreCase = true) -> message
                    message.contains("denied access", ignoreCase = true) -> message
                    message.contains("no longer available", ignoreCase = true) -> message
                    else -> "Failed to load Jellyfin songs. Check your connection and try again."
                }
                if (message.contains("session expired", ignoreCase = true)) {
                    authManager.clearSession(preserveServerConfig = true)
                    _session.value = null
                    _savedServerUrl.value = authManager.savedServerUrl().orEmpty()
                    _savedUsername.value = authManager.savedUsername().orEmpty()
                }
                _connectionState.value = JellyfinConnectionState.ERROR
            }
            if (_session.value == null) {
                _isLoading.value = false
                return@launch
            }

            // 2. Ambil album
            val albumsResult = apiClient.fetchAllAlbums(currentSession)
            currentCoroutineContext().ensureActive()
            if (requestGeneration != generation) return@launch
            albumsResult.onSuccess { albumList ->
                _albums.value = albumList
            }

            // 3. Ambil artis
            val artistsResult = apiClient.fetchArtists(currentSession)
            currentCoroutineContext().ensureActive()
            if (requestGeneration != generation) return@launch
            artistsResult.onSuccess { artistList ->
                _artists.value = artistList
            }

            // 4. Ambil playlist
            val playlistsResult = apiClient.fetchPlaylists(currentSession)
            currentCoroutineContext().ensureActive()
            if (requestGeneration != generation) return@launch
            playlistsResult.onSuccess { playlistList ->
                _playlists.value = playlistList
                val withSongs = playlistList.map { playlist ->
                    val items = apiClient.fetchPlaylistSongs(currentSession, playlist.id).getOrDefault(emptyList())
                    if (requestGeneration != generation) return@launch
                    playlist.copy(songs = items)
                }
                _playlists.value = withSongs
            }

            if (requestGeneration == generation) _isLoading.value = false
        }
    }

    fun searchSongs(query: String): List<Song> {
        val current = _songs.value
        if (query.isBlank()) return current
        return current.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    suspend fun fetchAlbumSongs(albumId: String): Result<List<Song>> {
        val currentSession = _session.value
            ?: return Result.failure(IllegalStateException("Connect to Jellyfin before opening an album."))
        return apiClient.fetchAlbumSongs(currentSession, albumId.removePrefix("jellyfin_album_")).onFailure { error ->
            _errorMessage.value = error.message ?: "Failed to load tracks for this Jellyfin album."
        }
    }

    // Compatibility helpers for ViewModel
    fun refreshAll() {
        refreshLibrary()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
