package com.audiobox.player.data.lyrics

import android.content.Context
import com.audiobox.player.model.LyricsData
import com.audiobox.player.model.Song
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repositori lirik yang menggabungkan cache lokal (memori + disk) dan LRCLIB API client.
 */
class LyricsRepository(context: Context) {

    private val cache = LyricsCache(context)
    private val client = LrclibClient()

    fun getCachedLyrics(song: Song): LyricsData? = cache.get(cleanTitle(song.title), cleanArtist(song.artist))
    fun clearCache() = cache.clear()

    suspend fun getLyrics(song: Song, forceRefresh: Boolean = false): Result<LyricsData> {
        val title = cleanTitle(song.title)
        val artist = cleanArtist(song.artist)

        // 1. Cek cache jika tidak dipaksa refresh
        if (!forceRefresh) {
            val cached = cache.get(title, artist)
            if (cached != null) {
                return Result.success(cached)
            }
        }

        // 2. Ambil dari LRCLIB
        return try {
            val lyrics = client.fetchLyrics(
                trackName = title,
                artistName = artist,
                albumName = song.album.takeIf { it.isNotBlank() && it != "Unknown Album" },
                durationMs = song.durationMs
            )

            if (lyrics != null) {
                cache.put(title, artist, lyrics)
                Result.success(lyrics)
            } else {
                Result.failure(NoSuchElementException("No lyrics found for \"$title\" by \"$artist\" on LRCLIB."))
            }
        } catch (e: RateLimitedException) {
            Result.failure(e)
        } catch (e: UnknownHostException) {
            Result.failure(IOException("No internet connection. Please check your network and try again.", e))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("Connection timed out while contacting LRCLIB.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("(?i)\\.(mp3|flac|wav|m4a|aac|ogg)$"), "")
            .replace(Regex("(?i)\\s*\\(feat\\..*?\\)"), "")
            .replace(Regex("(?i)\\s*\\[feat\\..*?\\]"), "")
            .trim()
    }

    private fun cleanArtist(raw: String): String {
        return if (raw.isBlank() || raw == "<unknown>" || raw == "Unknown Artist") {
            ""
        } else {
            raw.trim()
        }
    }
}
