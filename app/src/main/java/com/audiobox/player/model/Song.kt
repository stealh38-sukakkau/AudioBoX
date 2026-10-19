package com.audiobox.player.model

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class AlbumColorPalette(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val onBackground: Color = Color.White
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String,
    val audioUrl: String,
    val colorPalette: AlbumColorPalette,
    val lyrics: List<LyricLine>,
    val sizeBytes: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val dateAddedSec: Long = 0L,
    val folderPath: String = "",
    val albumId: Long = -1L,
    val source: String = "LOCAL",
    val jellyfinItemId: String? = null
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(if (artworkUrl.isNotBlank()) Uri.parse(artworkUrl) else null)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(audioUrl))
            .setMediaMetadata(metadata)
            .build()
    }
}
