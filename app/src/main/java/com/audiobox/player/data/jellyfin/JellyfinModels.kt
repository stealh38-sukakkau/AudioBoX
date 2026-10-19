package com.audiobox.player.data.jellyfin

enum class JellyfinConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}



data class JellyfinServerInfo(
    val serverName: String,
    val serverVersion: String,
    val serverId: String
)
