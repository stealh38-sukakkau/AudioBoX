package com.audiobox.player.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.audiobox.player.model.LocalPlaylist
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeChoice { SYSTEM, LIGHT, DARK }
enum class AppLanguageChoice { SYSTEM, INDONESIAN, ENGLISH }

/** Small persistent preference store matching the app's existing SharedPreferences use. */
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("audiobox_preferences", Context.MODE_PRIVATE)
    val theme = MutableStateFlow(enumValue(prefs.getString("theme", null), AppThemeChoice.SYSTEM))
    val language = MutableStateFlow(enumValue(prefs.getString("language", null), AppLanguageChoice.SYSTEM))
    val minimumSizeMiB = MutableStateFlow(
        prefs.getInt("minimum_size_mib", AudioFileSizeFilter.DEFAULT_MINIMUM_MIB).coerceAtLeast(0)
    )
    val onlineLyrics = MutableStateFlow(prefs.getBoolean("online_lyrics", true))
    val dynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors", true))
    val jellyfinEnabled = MutableStateFlow(prefs.getBoolean("jellyfin_enabled", true))
    val playlists = MutableStateFlow(readPlaylists())

    fun setTheme(value: AppThemeChoice) { theme.value = value; prefs.edit().putString("theme", value.name).apply() }
    fun setLanguage(value: AppLanguageChoice) { language.value = value; prefs.edit().putString("language", value.name).apply() }
    fun setMinimumSizeMiB(value: Int) { minimumSizeMiB.value = value.coerceAtLeast(0); prefs.edit().putInt("minimum_size_mib", minimumSizeMiB.value).apply() }
    fun setOnlineLyrics(value: Boolean) { onlineLyrics.value = value; prefs.edit().putBoolean("online_lyrics", value).apply() }
    fun setDynamicColors(value: Boolean) { dynamicColors.value = value; prefs.edit().putBoolean("dynamic_colors", value).apply() }
    fun setJellyfinEnabled(value: Boolean) { jellyfinEnabled.value = value; prefs.edit().putBoolean("jellyfin_enabled", value).apply() }

    fun createPlaylist(name: String, initialSongId: String? = null): Boolean {
        val current = playlists.value
        val created = LocalPlaylistOperations.create(current, name, java.util.UUID.randomUUID().toString()) ?: return false
        val entry = created.last().let { it.copy(songIds = listOfNotNull(initialSongId)) }
        savePlaylists(created.dropLast(1) + entry)
        return true
    }
    fun renamePlaylist(id: String, name: String): Boolean {
        val renamed = LocalPlaylistOperations.rename(playlists.value, id, name) ?: return false
        savePlaylists(renamed)
        return true
    }
    fun deletePlaylist(id: String) = savePlaylists(LocalPlaylistOperations.delete(playlists.value, id))
    fun addSong(playlistId: String, songId: String) = savePlaylists(
        LocalPlaylistOperations.addSong(playlists.value, playlistId, songId)
    )
    fun removeSong(playlistId: String, songId: String) = savePlaylists(
        LocalPlaylistOperations.removeSong(playlists.value, playlistId, songId)
    )
    fun clearLyricsCache(context: Context) {
        context.cacheDir.resolve("lyrics_cache").listFiles()?.forEach { it.delete() }
    }

    private fun savePlaylists(value: List<LocalPlaylist>) {
        playlists.value = value
        val array = JSONArray()
        value.forEach { playlist -> array.put(JSONObject().put("id", playlist.id).put("name", playlist.name).put("songs", JSONArray(playlist.songIds))) }
        prefs.edit().putString("local_playlists", array.toString()).apply()
    }
    private fun readPlaylists(): List<LocalPlaylist> = runCatching {
        val array = JSONArray(prefs.getString("local_playlists", "[]"))
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val songs = item.optJSONArray("songs") ?: JSONArray()
            LocalPlaylist(item.getString("id"), item.getString("name"), (0 until songs.length()).map(songs::getString))
        }
    }.getOrDefault(emptyList())

    private inline fun <reified T : Enum<T>> enumValue(value: String?, default: T) =
        runCatching { value?.let { enumValueOf<T>(it) } ?: default }.getOrDefault(default)
}