package com.audiobox.player.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.audiobox.player.data.jellyfin.JellyfinApiClient
import com.audiobox.player.data.jellyfin.JellyfinAuthManager
import com.audiobox.player.data.jellyfin.JellyfinServerUrl
import java.io.IOException
import com.audiobox.player.MainActivity

/**
 * Service pemutaran audio latar belakang menggunakan AndroidX Media3 MediaSessionService.
 * Menjaga pemutaran musik tetap aktif saat aplikasi diminimalkan atau layar mati,
 * serta secara otomatis menyediakan Media Notification di system tray dan kontrol headset/Bluetooth.
 */
@UnstableApi
class AudioboxMediaService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val authManager = JellyfinAuthManager(this)
        val apiClient = JellyfinApiClient(authManager.deviceId)
        val upstream = DefaultDataSource.Factory(this)
        val resolvingDataSource = ResolvingDataSource.Factory(upstream) { dataSpec ->
            val uri = dataSpec.uri
            if (uri.scheme != "audiobox-jellyfin") return@Factory dataSpec
            val itemId = uri.lastPathSegment?.takeIf { it.isNotBlank() }
                ?: throw IOException("Jellyfin audio item is unavailable.")
            val session = authManager.getSession()
                ?: throw IOException("Jellyfin session expired. Reconnect to continue playback.")
            val streamUri = android.net.Uri.parse(JellyfinServerUrl.audioStreamUrl(session.serverUrl, itemId))
            dataSpec.buildUpon()
                .setUri(streamUri)
                .setHttpRequestHeaders(apiClient.authorizationHeaders(session.accessToken))
                .build()
        }

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSource))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        player = null
        mediaSession = null
        super.onDestroy()
    }
}
