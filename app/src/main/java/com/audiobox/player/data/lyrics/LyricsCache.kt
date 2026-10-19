package com.audiobox.player.data.lyrics

import android.content.Context
import com.audiobox.player.model.LyricLine
import com.audiobox.player.model.LyricsData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Pengelolaan cache lirik dua lapis (In-Memory & Disk).
 * Menghindari permintaan jaringan berulang untuk lagu yang sama di sesi saat ini maupun lintas restart aplikasi.
 */
class LyricsCache(context: Context) {

    private val memoryCache = ConcurrentHashMap<String, LyricsData>()
    private val diskCacheDir = File(context.cacheDir, "lyrics_cache").apply {
        if (!exists()) mkdirs()
    }

    private fun generateKey(trackName: String, artistName: String): String {
        val raw = "${trackName.trim().lowercase()}_${artistName.trim().lowercase()}"
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }

    fun get(trackName: String, artistName: String): LyricsData? {
        val key = generateKey(trackName, artistName)

        // 1. Periksa memory cache
        memoryCache[key]?.let { return it }

        // 2. Periksa disk cache
        val file = File(diskCacheDir, "$key.json")
        if (file.exists() && file.length() > 0) {
            try {
                val jsonStr = file.readText(Charsets.UTF_8)
                val json = JSONObject(jsonStr)

                val isSynced = json.optBoolean("isSynced", false)
                val isInstrumental = json.optBoolean("isInstrumental", false)
                val plainText = if (json.has("plainText") && !json.isNull("plainText")) {
                    json.getString("plainText")
                } else null

                val syncedLines = mutableListOf<LyricLine>()
                if (json.has("lines")) {
                    val array = json.getJSONArray("lines")
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        syncedLines.add(
                            LyricLine(
                                timestampMs = item.getLong("ts"),
                                text = item.getString("txt")
                            )
                        )
                    }
                }

                val data = LyricsData(
                    isSynced = isSynced,
                    syncedLines = syncedLines,
                    plainText = plainText,
                    isInstrumental = isInstrumental,
                    source = json.optString("source", "LRCLIB (Cache)")
                )

                // Simpan kembali ke memory cache untuk akses kilat berikutnya
                memoryCache[key] = data
                return data
            } catch (e: Exception) {
                file.delete()
            }
        }

        return null
    }

    fun put(trackName: String, artistName: String, data: LyricsData) {
        val key = generateKey(trackName, artistName)
        memoryCache[key] = data

        try {
            val json = JSONObject().apply {
                put("isSynced", data.isSynced)
                put("isInstrumental", data.isInstrumental)
                put("plainText", data.plainText ?: JSONObject.NULL)
                put("source", data.source)

                val array = JSONArray()
                for (line in data.syncedLines) {
                    val item = JSONObject().apply {
                        put("ts", line.timestampMs)
                        put("txt", line.text)
                    }
                    array.put(item)
                }
                put("lines", array)
            }

            val file = File(diskCacheDir, "$key.json")
            file.writeText(json.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clear() {
        memoryCache.clear()
        try {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
