package com.audiobox.player.data.jellyfin

/**
 * Data class representing an authenticated Jellyfin session.
 *
 * Fields are persisted by `JellyfinAuthManager` and used throughout the UI and repository.
 */
data class JellyfinSession(
    val serverUrl: String,
    val accessToken: String,
    val userId: String,
    val serverId: String,
    val serverName: String,
    val serverVersion: String,
    val username: String
) {
    /** Alias for backward‑compatible UI code that expects `userName`. */
    val userName: String get() = username
}
