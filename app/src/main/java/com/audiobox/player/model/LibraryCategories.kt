package com.audiobox.player.model

enum class MusicSource(val label: String) {
    LOCAL("On This Device"),
    JELLYFIN("Jellyfin Server")
}

enum class LibraryTab(val title: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

enum class SortOrder(val label: String) {
    TITLE_ASC("Title (A-Z)"),
    ARTIST_ASC("Artist (A-Z)"),
    DATE_ADDED_DESC("Recently Added"),
    DURATION_DESC("Longest First")
}

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artworkUrl: String,
    val songs: List<Song> = emptyList()
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val songs: List<Song> = emptyList()
)

data class MusicFolder(
    val name: String,
    val path: String,
    val trackCount: Int,
    val songs: List<Song> = emptyList()
)

data class JellyfinPlaylist(
    val id: String,
    val name: String,
    val trackCount: Int,
    val artworkUrl: String,
    val songs: List<Song> = emptyList()
)
