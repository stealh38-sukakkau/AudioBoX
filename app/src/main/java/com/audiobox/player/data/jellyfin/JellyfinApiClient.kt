package com.audiobox.player.data.jellyfin

import androidx.compose.ui.graphics.Color
import com.audiobox.player.model.Album
import com.audiobox.player.model.AlbumColorPalette
import com.audiobox.player.model.Artist
import com.audiobox.player.model.JellyfinPlaylist
import com.audiobox.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * Klien HTTP resmi untuk Jellyfin REST API.
 * Menggunakan otentikasi resmi MediaBrowser header, mendukung streaming langsung (Direct Play),
 * pagination, pengambilan metadata album, artis, lagu, dan playlist.
 */
class JellyfinApiClient(private val deviceId: String) {

    companion object {
        private const val CLIENT_NAME = "AudioBoX"
        private const val CLIENT_VERSION = "1.0.0"
        private const val DEVICE_NAME = "Android Device"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val LOG_TAG = "AudioboxJellyfin"
    }

    private fun buildAuthHeader(token: String? = null): String {
        val parts = mutableListOf(
            "Client=\"$CLIENT_NAME\"",
            "Device=\"$DEVICE_NAME\"",
            "DeviceId=\"$deviceId\"",
            "Version=\"$CLIENT_VERSION\""
        )
        if (!token.isNullOrBlank()) {
            parts.add("Token=\"$token\"")
        }
        return "MediaBrowser ${parts.joinToString(", ")}"
    }

    fun authorizationHeader(token: String): String = buildAuthHeader(token)

    /** Jellyfin 12 uses the standard Authorization header; legacy auth is disabled by default. */
    fun authorizationHeaders(token: String? = null): Map<String, String> {
        val value = buildAuthHeader(token)
        return mapOf("Authorization" to value)
    }

    private fun applyAuthorization(connection: HttpURLConnection, token: String? = null) {
        authorizationHeaders(token).forEach { (name, value) -> connection.setRequestProperty(name, value) }
    }

    private fun logDiagnostic(endpoint: String, statusCode: Int?, exception: Throwable?) {
        val path = runCatching { URL(endpoint).path }.getOrDefault("<invalid-endpoint>")
        val details = JellyfinHttpErrors.diagnostic(exception)
        Log.w(LOG_TAG, "request_failed endpoint=$path status=${statusCode ?: "unknown"} $details")
    }

    /**
     * Memeriksa keterjangkauan server Jellyfin via GET /System/Info/Public
     */
    suspend fun getPublicServerInfo(serverUrl: String): Result<JellyfinServerInfo> = withContext(Dispatchers.IO) {
        var endpoint = "<invalid-endpoint>"
        var statusCode: Int? = null
        try {
            val cleanUrl = JellyfinServerUrl.normalize(serverUrl)
            endpoint = "$cleanUrl/System/Info/Public"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                applyAuthorization(this)
                setRequestProperty("Accept", "application/json")
            }

            statusCode = connection.responseCode
            if (statusCode == 200) {
                val body = readBody(connection)
                val json = JSONObject(body)
                val serverName = json.optString("ServerName", "Jellyfin Server")
                val version = json.optString("Version", "Unknown")
                val id = json.optString("Id", "")
                connection.disconnect()
                Result.success(JellyfinServerInfo(serverName, version, id))
            } else {
                connection.disconnect()
                val error = JellyfinHttpException(endpoint, statusCode!!, authentication = false)
                logDiagnostic(endpoint, statusCode, error)
                Result.failure(error)
            }
        } catch (e: Exception) {
            logDiagnostic(endpoint, statusCode, e)
            Result.failure(if (e is IllegalArgumentException) e else JellyfinRequestException(endpoint, statusCode, e))
        }
    }

    /**
     * Autentikasi pengguna via POST /Users/AuthenticateByName
     */
    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<JellyfinSession> = withContext(Dispatchers.IO) {
        var endpoint = "<invalid-endpoint>"
        var statusCode: Int? = null
        try {
            val cleanUrl = JellyfinServerUrl.normalize(serverUrl)
            endpoint = "$cleanUrl/Users/AuthenticateByName"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                applyAuthorization(this)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val reqBody = JSONObject().apply {
                put("Username", username)
                put("Pw", password)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(reqBody.toString())
                writer.flush()
            }

            statusCode = connection.responseCode
            if (statusCode == 200) {
                val body = readBody(connection)
                val json = JSONObject(body)

                val token = json.getString("AccessToken")
                val serverId = json.optString("ServerId", "")
                val userObj = json.getJSONObject("User")
                val userId = userObj.getString("Id")
                val resolvedName = userObj.optString("Name", username)

                // Ambil server info untuk display name
                val serverInfo = getPublicServerInfo(cleanUrl).getOrNull()
                val serverName = serverInfo?.serverName ?: "Jellyfin Server"
                val serverVersion = serverInfo?.serverVersion ?: ""

                connection.disconnect()
                Result.success(
                    JellyfinSession(
                        serverUrl = cleanUrl,
                        accessToken = token,
                        userId = userId,
                        serverId = serverId,
                        serverName = serverName,
                        serverVersion = serverVersion,
                        username = resolvedName
                    )
                )
            } else {
                connection.disconnect()
                val error = JellyfinHttpException(endpoint, statusCode!!, authentication = true)
                logDiagnostic(endpoint, statusCode, error)
                Result.failure(error)
            }
        } catch (e: Exception) {
            logDiagnostic(endpoint, statusCode, e)
            Result.failure(if (e is IllegalArgumentException) e else JellyfinRequestException(endpoint, statusCode, e))
        }
    }

    /**
     * Mengambil daftar lagu (IncludeItemTypes=Audio)
     */
    suspend fun fetchSongs(
        session: JellyfinSession,
        albumId: String? = null,
        artistId: String? = null,
        startIndex: Int = 0,
        limit: Int = 200,
        playlistId: String? = null
    ): Result<List<Song>> = withContext(Dispatchers.IO) {
        val queryParams = mutableListOf(
            "IncludeItemTypes=Audio",
            "Recursive=true",
            "SortBy=SortName",
            "SortOrder=Ascending",
            "StartIndex=$startIndex",
            "Limit=$limit",
            "Fields=MediaSources,MediaStreams"
        )
        if (!albumId.isNullOrBlank()) queryParams.add("ParentId=$albumId")
        if (!artistId.isNullOrBlank()) queryParams.add("ArtistIds=$artistId")

        val endpoint = if (!playlistId.isNullOrBlank()) {
            "${session.serverUrl}/Playlists/$playlistId/Items?UserId=${session.userId}&Fields=MediaSources,MediaStreams&StartIndex=$startIndex&Limit=$limit"
        } else {
            "${session.serverUrl}/Users/${session.userId}/Items?${queryParams.joinToString("&")}"
        }

        try {
            val json = getJsonObject(endpoint, session.accessToken)
            val items = json.optJSONArray("Items") ?: JSONArray()
            val songList = mutableListOf<Song>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val itemId = item.getString("Id")
                val title = item.optString("Name", "Unknown Track")
                val album = item.optString("Album", "Unknown Album")

                val artistsArray = item.optJSONArray("Artists")
                val artist = if (artistsArray != null && artistsArray.length() > 0) {
                    artistsArray.getString(0)
                } else {
                    item.optString("AlbumArtist", "Unknown Artist")
                }

                // RunTimeTicks (10.000 ticks = 1 ms)
                val runTimeTicks = item.optLong("RunTimeTicks", 0L)
                val durationMs = if (runTimeTicks > 0) runTimeTicks / 10_000L else 0L

                // Remote artwork and audio use auth headers, keeping access tokens out of URLs.
                val artworkUrl = "${session.serverUrl}/Items/$itemId/Images/Primary?maxWidth=600&quality=90"
                val audioUrl = JellyfinServerUrl.playbackUri(itemId)

                var container = item.optString("Container", "mp3")
                var mimeType = "audio/$container"
                var sizeBytes = 0L

                val mediaSources = item.optJSONArray("MediaSources")
                if (mediaSources != null && mediaSources.length() > 0) {
                    val ms = mediaSources.getJSONObject(0)
                    sizeBytes = ms.optLong("Size", 0L)
                    container = ms.optString("Container", container)
                    mimeType = "audio/$container"
                }

                songList.add(
                    Song(
                        id = "jellyfin_$itemId",
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        artworkUrl = artworkUrl,
                        audioUrl = audioUrl,
                        colorPalette = generatePalette("$title $album $artist"),
                        lyrics = emptyList(),
                        sizeBytes = sizeBytes,
                        mimeType = mimeType,
                        source = "JELLYFIN",
                        jellyfinItemId = itemId
                    )
                )
            }

            Result.success(songList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil daftar Album (IncludeItemTypes=MusicAlbum)
     */
    suspend fun fetchAlbums(
        session: JellyfinSession,
        startIndex: Int = 0,
        limit: Int = 100
    ): Result<List<Album>> = withContext(Dispatchers.IO) {
        val endpoint = "${session.serverUrl}/Users/${session.userId}/Items?IncludeItemTypes=MusicAlbum&Recursive=true&SortBy=SortName&SortOrder=Ascending&StartIndex=$startIndex&Limit=$limit"

        try {
            val json = getJsonObject(endpoint, session.accessToken)
            val items = json.optJSONArray("Items") ?: JSONArray()
            val albums = mutableListOf<Album>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val itemId = item.getString("Id")
                val title = item.optString("Name", "Unknown Album")
                val artist = item.optString("AlbumArtist", "Various Artists")
                val trackCount = item.optInt("ChildCount", 0)
                val artworkUrl = "${session.serverUrl}/Items/$itemId/Images/Primary?maxWidth=600&quality=90"

                albums.add(
                    Album(
                        id = "jellyfin_album_$itemId",
                        title = title,
                        artist = artist,
                        trackCount = trackCount,
                        artworkUrl = artworkUrl
                    )
                )
            }

            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllSongs(session: JellyfinSession, pageSize: Int = 200): Result<List<Song>> {
        val all = mutableListOf<Song>()
        var offset = 0
        while (true) {
            val page = fetchSongs(session, startIndex = offset, limit = pageSize).getOrElse { return Result.failure(it) }
            all += page
            if (page.size < pageSize) break
            offset += page.size
        }
        return Result.success(all)
    }

    suspend fun fetchAllAlbums(session: JellyfinSession, pageSize: Int = 100): Result<List<Album>> {
        val all = mutableListOf<Album>()
        var offset = 0
        while (true) {
            val page = fetchAlbums(session, startIndex = offset, limit = pageSize).getOrElse { return Result.failure(it) }
            all += page
            if (page.size < pageSize) break
            offset += page.size
        }
        return Result.success(all)
    }

    suspend fun fetchPlaylistSongs(session: JellyfinSession, playlistId: String, pageSize: Int = 200): Result<List<Song>> {
        val all = mutableListOf<Song>()
        var offset = 0
        while (true) {
            val page = fetchSongs(session, startIndex = offset, limit = pageSize, playlistId = playlistId)
                .getOrElse { return Result.failure(it) }
            all += page
            if (page.size < pageSize) break
            offset += page.size
        }
        return Result.success(all)
    }

    /**
     * Mengambil daftar Artis musik
     */
    suspend fun fetchArtists(session: JellyfinSession): Result<List<Artist>> = withContext(Dispatchers.IO) {
        val endpoint = "${session.serverUrl}/Artists?userId=${session.userId}&SortBy=SortName&SortOrder=Ascending"

        try {
            val json = getJsonObject(endpoint, session.accessToken)
            val items = json.optJSONArray("Items") ?: JSONArray()
            val artists = mutableListOf<Artist>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val itemId = item.getString("Id")
                val name = item.optString("Name", "Unknown Artist")
                val trackCount = item.optInt("SongCount", 0)
                val albumCount = item.optInt("AlbumCount", 0)

                artists.add(
                    Artist(
                        name = name,
                        trackCount = trackCount,
                        albumCount = albumCount
                    )
                )
            }

            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil daftar Playlist
     */
    suspend fun fetchPlaylists(session: JellyfinSession): Result<List<JellyfinPlaylist>> = withContext(Dispatchers.IO) {
        val endpoint = "${session.serverUrl}/Users/${session.userId}/Items?IncludeItemTypes=Playlist&Recursive=true&SortBy=SortName&SortOrder=Ascending"

        try {
            val json = getJsonObject(endpoint, session.accessToken)
            val items = json.optJSONArray("Items") ?: JSONArray()
            val playlists = mutableListOf<JellyfinPlaylist>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val itemId = item.getString("Id")
                val name = item.optString("Name", "Playlist")
                val trackCount = item.optInt("ChildCount", 0)
                val artworkUrl = "${session.serverUrl}/Items/$itemId/Images/Primary?maxWidth=600&quality=90"

                playlists.add(
                    JellyfinPlaylist(
                        id = itemId,
                        name = name,
                        trackCount = trackCount,
                        artworkUrl = artworkUrl
                    )
                )
            }

            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getJsonObject(endpoint: String, token: String): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            applyAuthorization(this, token)
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            if (code == 200) {
                val body = readBody(connection)
                return JSONObject(body)
            } else {
                val error = JellyfinHttpException(endpoint, code, authentication = false)
                logDiagnostic(endpoint, code, error)
                throw error
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection): String {
        BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
            }
            return sb.toString()
        }
    }

    private fun generatePalette(seed: String): AlbumColorPalette {
        val hash = abs(seed.hashCode())
        val hue = (hash % 360).toFloat()
        return AlbumColorPalette(
            primary = Color.hsl(hue, 0.65f, 0.50f),
            secondary = Color.hsl((hue + 40f) % 360f, 0.75f, 0.55f),
            background = Color.hsl(hue, 0.40f, 0.08f)
        )
    }
}
