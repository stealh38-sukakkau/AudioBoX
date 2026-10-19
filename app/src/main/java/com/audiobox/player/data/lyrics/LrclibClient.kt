package com.audiobox.player.data.lyrics

import com.audiobox.player.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Client API untuk LRCLIB (https://lrclib.net).
 * Mendukung GET /api/get dan fallback GET /api/search dengan validasi durasi dan metadata,
 * penanganan rate limiting (HTTP 429 Retry-After), timeout, dan User-Agent kustom.
 */
class LrclibClient {

    companion object {
        private const val BASE_URL = "https://lrclib.net"
        private const val USER_AGENT = "AudioBoX/1.0.0 (https://github.com/audiobox)"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000

        @Volatile
        private var rateLimitedUntilMs: Long = 0L
    }

    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        durationMs: Long = 0L
    ): LyricsData? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now < rateLimitedUntilMs) {
            val waitSec = ((rateLimitedUntilMs - now) / 1000).coerceAtLeast(1)
            throw RateLimitedException("Server rate limited. Retry in $waitSec seconds.")
        }

        val durationSec = if (durationMs > 0) (durationMs / 1000).toInt() else null

        // 1. Coba endpoint utama: GET /api/get
        val directResult = tryGetLyrics(
            trackName = trackName,
            artistName = artistName,
            albumName = albumName,
            durationSec = durationSec
        )

        if (directResult != null) {
            return@withContext directResult
        }

        // 2. Jika 404 / tidak cocok, gunakan alternatif: GET /api/search
        return@withContext trySearchLyrics(
            trackName = trackName,
            artistName = artistName,
            albumName = albumName,
            durationSec = durationSec
        )
    }

    private fun tryGetLyrics(
        trackName: String,
        artistName: String,
        albumName: String?,
        durationSec: Int?
    ): LyricsData? {
        val params = mutableListOf<String>()
        params.add("track_name=${urlEncode(trackName)}")
        params.add("artist_name=${urlEncode(artistName)}")
        if (!albumName.isNullOrBlank()) {
            params.add("album_name=${urlEncode(albumName)}")
        }
        if (durationSec != null && durationSec > 0) {
            params.add("duration=$durationSec")
        }

        val urlString = "$BASE_URL/api/get?${params.joinToString("&")}"
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val body = readResponseBody(connection)
                    parseLyricsJson(JSONObject(body))
                }
                404 -> null
                429 -> {
                    handleRateLimit(connection)
                    null
                }
                else -> null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun trySearchLyrics(
        trackName: String,
        artistName: String,
        albumName: String?,
        durationSec: Int?
    ): LyricsData? {
        val params = mutableListOf<String>()
        params.add("track_name=${urlEncode(trackName)}")
        params.add("artist_name=${urlEncode(artistName)}")
        if (!albumName.isNullOrBlank()) {
            params.add("album_name=${urlEncode(albumName)}")
        }

        val urlString = "$BASE_URL/api/search?${params.joinToString("&")}"
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val body = readResponseBody(connection)
                    val array = JSONArray(body)
                    findBestMatch(array, trackName, artistName, durationSec)
                }
                429 -> {
                    handleRateLimit(connection)
                    null
                }
                else -> null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun findBestMatch(
        array: JSONArray,
        trackName: String,
        artistName: String,
        durationSec: Int?
    ): LyricsData? {
        if (array.length() == 0) return null

        var bestCandidate: JSONObject? = null
        var bestScore = -1

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val candidateTrack = item.optString("trackName").ifBlank { item.optString("name") }
            val candidateArtist = item.optString("artistName")
            val candidateDuration = item.optInt("duration", 0)
            val hasSynced = item.optString("syncedLyrics").isNotBlank()
            val hasPlain = item.optString("plainLyrics").isNotBlank()

            if (!hasSynced && !hasPlain) continue

            var score = 0
            if (candidateTrack.equals(trackName, ignoreCase = true)) score += 40
            else if (candidateTrack.contains(trackName, ignoreCase = true) || trackName.contains(candidateTrack, ignoreCase = true)) score += 20

            if (candidateArtist.equals(artistName, ignoreCase = true)) score += 30
            else if (candidateArtist.contains(artistName, ignoreCase = true) || artistName.contains(candidateArtist, ignoreCase = true)) score += 15

            if (durationSec != null && candidateDuration > 0) {
                val diff = abs(candidateDuration - durationSec)
                if (diff <= 3) score += 20
                else if (diff <= 8) score += 10
            }

            if (hasSynced) score += 15

            if (score > bestScore && score >= 35) {
                bestScore = score
                bestCandidate = item
            }
        }

        return bestCandidate?.let { parseLyricsJson(it) }
    }

    private fun parseLyricsJson(json: JSONObject): LyricsData {
        val syncedRaw = json.optString("syncedLyrics", "")
        val plainRaw = json.optString("plainLyrics", "")
        val isInstrumental = json.optBoolean("instrumental", false)

        val syncedLines = if (syncedRaw.isNotBlank()) {
            LrcParser.parse(syncedRaw)
        } else {
            emptyList()
        }

        val isSynced = syncedLines.isNotEmpty()

        return LyricsData(
            isSynced = isSynced,
            syncedLines = syncedLines,
            plainText = plainRaw.ifBlank { null },
            isInstrumental = isInstrumental,
            source = "LRCLIB"
        )
    }

    private fun handleRateLimit(connection: HttpURLConnection) {
        val retryAfterHeader = connection.getHeaderField("Retry-After")
        val retrySeconds = retryAfterHeader?.toLongOrNull() ?: 15L
        rateLimitedUntilMs = System.currentTimeMillis() + (retrySeconds * 1000L)
    }

    private fun readResponseBody(connection: HttpURLConnection): String {
        BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
            }
            return sb.toString()
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value.trim(), "UTF-8")
    }
}

class RateLimitedException(message: String) : Exception(message)
