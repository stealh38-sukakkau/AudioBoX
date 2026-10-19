package com.audiobox.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.audiobox.player.data.jellyfin.JellyfinApiClient
import com.audiobox.player.data.jellyfin.JellyfinAuthManager

/** Creates Coil requests with Jellyfin's supported auth header, keeping tokens out of image URLs. */
@Composable
fun jellyfinArtworkRequest(url: String): ImageRequest {
    val context = LocalContext.current
    val authManager = remember(context) { JellyfinAuthManager(context) }
    val session = authManager.getSession()
    return remember(url, session?.accessToken, session?.serverUrl) {
        ImageRequest.Builder(context)
            .data(url)
            .apply {
                if (session != null && url.startsWith("${session.serverUrl.trimEnd('/')}/")) {
                    val client = JellyfinApiClient(authManager.deviceId)
                    client.authorizationHeaders(session.accessToken).forEach { (name, value) -> addHeader(name, value) }
                }
            }
            .build()
    }
}
