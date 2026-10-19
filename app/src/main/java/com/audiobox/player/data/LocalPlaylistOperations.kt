package com.audiobox.player.data

import com.audiobox.player.model.LocalPlaylist

/** Immutable playlist edits shared by persistence code and unit tests. */
internal object LocalPlaylistOperations {
    fun create(playlists: List<LocalPlaylist>, name: String, id: String): List<LocalPlaylist>? {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || playlists.any { it.name.equals(cleanName, ignoreCase = true) }) return null
        return playlists + LocalPlaylist(id, cleanName, emptyList())
    }

    fun rename(playlists: List<LocalPlaylist>, id: String, name: String): List<LocalPlaylist>? {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || playlists.none { it.id == id } ||
            playlists.any { it.id != id && it.name.equals(cleanName, ignoreCase = true) }
        ) return null
        return playlists.map { if (it.id == id) it.copy(name = cleanName) else it }
    }

    fun delete(playlists: List<LocalPlaylist>, id: String): List<LocalPlaylist> =
        playlists.filterNot { it.id == id }

    fun addSong(playlists: List<LocalPlaylist>, playlistId: String, songId: String): List<LocalPlaylist> =
        playlists.map { playlist ->
            if (playlist.id == playlistId && songId !in playlist.songIds) {
                playlist.copy(songIds = playlist.songIds + songId)
            } else playlist
        }

    fun removeSong(playlists: List<LocalPlaylist>, playlistId: String, songId: String): List<LocalPlaylist> =
        playlists.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(songIds = playlist.songIds - songId) else playlist
        }
}
