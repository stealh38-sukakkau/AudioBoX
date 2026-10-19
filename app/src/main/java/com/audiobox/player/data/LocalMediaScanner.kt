package com.audiobox.player.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import com.audiobox.player.model.Album
import com.audiobox.player.model.AlbumColorPalette
import com.audiobox.player.model.Artist
import com.audiobox.player.model.MusicFolder
import com.audiobox.player.model.Song
import com.audiobox.player.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/**
 * Pemindai musik otomatis via Android MediaStore — ala HiBy Music.
 * Berjalan di background thread (Dispatchers.IO), mengekstrak metadata lengkap,
 * mengelompokkan ke kategori Songs, Albums, Artists, dan Folders, serta mengamati perubahan koleksi.
 */
class LocalMediaScanner(
    private val context: Context,
    private val minimumSizeBytes: () -> Long = {
        AudioFileSizeFilter.minimumBytes(AudioFileSizeFilter.DEFAULT_MINIMUM_MIB)
    }
) {

    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    suspend fun scanLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        // Hanya musik sebenarnya dan durasi minimal 5 detik (mengabaikan suara notifikasi/ringtone pendek)
        val minSize = minimumSizeBytes().coerceAtLeast(0L)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000 AND ${MediaStore.Audio.Media.SIZE} >= ?"
        val sortOrderString = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf(minSize.toString()),
                sortOrderString
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol).takeIf { !it.isNullOrBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = cursor.getString(albumCol).takeIf { !it.isNullOrBlank() && it != "<unknown>" } ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    // Keep the setting enforced in the scanner itself as well as in the
                    // MediaStore query, so the indexed library always reflects the threshold.
                    if (!AudioFileSizeFilter.includes(sizeBytes, minSize)) continue
                    val mimeType = cursor.getString(mimeTypeCol) ?: "audio/mpeg"
                    val dateAddedSec = cursor.getLong(dateAddedCol)
                    val filePath = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                    } else {
                        ""
                    }

                    val folderPath = if (filePath.isNotBlank()) {
                        try {
                            File(filePath).parent ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                    } else {
                        ""
                    }

                    songsList.add(
                        Song(
                            id = "mediastore_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            artworkUrl = albumArtworkUri,
                            audioUrl = contentUri,
                            colorPalette = generatePaletteForText("$title $album $artist"),
                            lyrics = emptyList(),
                            sizeBytes = sizeBytes,
                            mimeType = mimeType,
                            dateAddedSec = dateAddedSec,
                            folderPath = folderPath,
                            albumId = albumId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("MediaStore scan failed: ${e.localizedMessage ?: e.javaClass.simpleName}", e)
        }

        songsList
    }

    fun groupByAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.album.ifBlank { "Unknown Album" } }
            .map { (albumTitle, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = "album_${first.albumId.takeIf { it > 0 } ?: albumTitle.hashCode()}",
                    title = albumTitle,
                    artist = first.artist,
                    trackCount = albumSongs.size,
                    artworkUrl = albumSongs.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl ?: "",
                    songs = albumSongs
                )
            }.sortedBy { it.title.lowercase() }
    }

    fun groupByArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artistName, artistSongs) ->
                val distinctAlbums = artistSongs.map { it.album }.distinct().size
                Artist(
                    name = artistName,
                    trackCount = artistSongs.size,
                    albumCount = distinctAlbums,
                    songs = artistSongs
                )
            }.sortedBy { it.name.lowercase() }
    }

    fun groupByFolders(songs: List<Song>): List<MusicFolder> {
        return songs.groupBy { it.folderPath.ifBlank { "Root Storage" } }
            .map { (folderPath, folderSongs) ->
                val folderName = if (folderPath == "Root Storage") "Device Storage" else File(folderPath).name
                MusicFolder(
                    name = folderName,
                    path = folderPath,
                    trackCount = folderSongs.size,
                    songs = folderSongs
                )
            }.sortedBy { it.name.lowercase() }
    }

    fun sortSongs(songs: List<Song>, sortOrder: SortOrder): List<Song> {
        return when (sortOrder) {
            SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST_ASC -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            SortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAddedSec }
            SortOrder.DURATION_DESC -> songs.sortedByDescending { it.durationMs }
        }
    }

    fun registerContentObserver(onChangeDebounced: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                onChangeDebounced()
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return observer
    }

    fun unregisterContentObserver(observer: ContentObserver) {
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generatePaletteForText(seed: String): AlbumColorPalette {
        val hash = abs(seed.hashCode())
        val hue = (hash % 360).toFloat()
        // Variasi palet warna harmonis gelap
        val primary = Color.hsl(hue, 0.70f, 0.55f)
        val secondary = Color.hsl((hue + 45f) % 360f, 0.80f, 0.60f)
        val background = Color.hsl(hue, 0.50f, 0.08f)
        return AlbumColorPalette(
            primary = primary,
            secondary = secondary,
            background = background
        )
    }
}
