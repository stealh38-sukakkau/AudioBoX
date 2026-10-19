package com.audiobox.player.data.jellyfin

import java.net.URI

/** Validates the user supplied server address without weakening normal TLS validation. */
object JellyfinServerUrl {
    fun normalize(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "Enter a Jellyfin server address." }
        val withScheme = if (value.contains("://")) value else "https://$value"
        val uri = try { URI(withScheme) } catch (_: Exception) {
            throw IllegalArgumentException("Enter a valid Jellyfin server address.")
        }
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            "Jellyfin server addresses must use HTTP or HTTPS."
        }
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Enter a server address without credentials, query, or fragment."
        }
        return URI(uri.scheme.lowercase(), null, uri.host, uri.port, uri.path?.trimEnd('/').orEmpty(), null, null)
            .toASCIIString().trimEnd('/')
    }

    fun audioStreamUrl(serverUrl: String, itemId: String): String {
        require(itemId.isNotBlank() && !itemId.contains('/') && !itemId.contains('?'))
        return "${normalize(serverUrl)}/Audio/$itemId/stream?static=true"
    }

    fun playbackUri(itemId: String): String {
        require(itemId.isNotBlank() && !itemId.contains('/') && !itemId.contains('?'))
        return "audiobox-jellyfin://audio/$itemId"
    }
}
