package com.audiobox.player.data

import android.content.Context
import android.database.ContentObserver
import com.audiobox.player.model.Album
import com.audiobox.player.model.Artist
import com.audiobox.player.model.MusicFolder
import com.audiobox.player.model.Song
import com.audiobox.player.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getSongById(id: String): Song?
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun refreshLocalMedia(): List<Song>
    fun getAlbums(): List<Album>
    fun getArtists(): List<Artist>
    fun getFolders(): List<MusicFolder>
    fun sortSongs(songs: List<Song>, sortOrder: SortOrder): List<Song>
    fun registerObserver(onChange: () -> Unit): ContentObserver?
    fun unregisterObserver(observer: ContentObserver)
}

class MusicRepositoryImpl(
    private val context: Context? = null,
    private val userPreferences: UserPreferences? = context?.let { UserPreferences(it) }
) : MusicRepository {

    private val scanner = context?.let { LocalMediaScanner(it) {
        AudioFileSizeFilter.minimumBytes(
            userPreferences?.minimumSizeMiB?.value ?: AudioFileSizeFilter.DEFAULT_MINIMUM_MIB
        )
    } }
    private val dummySongs = DummyMusicDataSource.songs

    private val _songsFlow = MutableStateFlow(if (scanner == null) dummySongs else emptyList())

    override fun getAllSongs(): Flow<List<Song>> = _songsFlow.asStateFlow()

    override fun getSongById(id: String): Song? {
        return _songsFlow.value.find { it.id == id }
    }

    override fun searchSongs(query: String): Flow<List<Song>> = flow {
        val current = _songsFlow.value
        if (query.isBlank()) {
            emit(current)
        } else {
            val filtered = current.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
            emit(filtered)
        }
    }

    override suspend fun refreshLocalMedia(): List<Song> {
        val localSongs = scanner?.scanLocalSongs() ?: emptyList()
        val combined = if (scanner == null) dummySongs else localSongs
        _songsFlow.value = combined
        return combined
    }

    override fun getAlbums(): List<Album> {
        return scanner?.groupByAlbums(_songsFlow.value) ?: emptyList()
    }

    override fun getArtists(): List<Artist> {
        return scanner?.groupByArtists(_songsFlow.value) ?: emptyList()
    }

    override fun getFolders(): List<MusicFolder> {
        return scanner?.groupByFolders(_songsFlow.value) ?: emptyList()
    }

    override fun sortSongs(songs: List<Song>, sortOrder: SortOrder): List<Song> {
        return scanner?.sortSongs(songs, sortOrder) ?: songs
    }

    override fun registerObserver(onChange: () -> Unit): ContentObserver? {
        return scanner?.registerContentObserver(onChange)
    }

    override fun unregisterObserver(observer: ContentObserver) {
        scanner?.unregisterContentObserver(observer)
    }
}
