package com.audiobox.player.data.jellyfin

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.UUID

/**
 * Pengelolaan sesi dan kredensial Jellyfin yang aman.
 * Hanya menyimpan token akses (AccessToken) dan metadata sesi, TIDAK PERNAH menyimpan kata sandi mentah.
 */
class JellyfinAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("jellyfin_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_URL = "jf_server_url"
        private const val KEY_ACCESS_TOKEN = "jf_access_token"
        private const val KEY_ENCRYPTED_ACCESS_TOKEN = "jf_access_token_encrypted"
        private const val KEY_TOKEN_IV = "jf_access_token_iv"
        private const val KEY_ALIAS = "audiobox_jellyfin_token"
        private const val KEY_USER_ID = "jf_user_id"
        private const val KEY_SERVER_ID = "jf_server_id"
        private const val KEY_SERVER_NAME = "jf_server_name"
        private const val KEY_SERVER_VERSION = "jf_server_version"
        private const val KEY_USERNAME = "jf_username"
        private const val KEY_DEVICE_ID = "jf_device_id"
    }

    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    fun getSession(): JellyfinSession? {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val token = readEncryptedToken() ?: prefs.getString(KEY_ACCESS_TOKEN, null)?.also {
            // One-time migration from the earlier plaintext preference.
            try {
                writeEncryptedToken(it)
                prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
            } catch (_: Exception) {
                return null
            }
        } ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val serverId = prefs.getString(KEY_SERVER_ID, "") ?: ""
        val serverName = prefs.getString(KEY_SERVER_NAME, "Jellyfin Server") ?: "Jellyfin Server"
        val serverVersion = prefs.getString(KEY_SERVER_VERSION, "") ?: ""
        val username = prefs.getString(KEY_USERNAME, "") ?: ""

        return JellyfinSession(
            serverUrl = serverUrl,
            accessToken = token,
            userId = userId,
            serverId = serverId,
            serverName = serverName,
            serverVersion = serverVersion,
            username = username
        )
    }

    fun saveSession(session: JellyfinSession) {
        writeEncryptedToken(session.accessToken)
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .putString(KEY_SERVER_URL, session.serverUrl.trimEnd('/'))
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_SERVER_ID, session.serverId)
            .putString(KEY_SERVER_NAME, session.serverName)
            .putString(KEY_SERVER_VERSION, session.serverVersion)
            .putString(KEY_USERNAME, session.username)
            .apply()
    }

    fun clearSession(preserveServerConfig: Boolean = false) {
        val editor = prefs.edit()
        if (!preserveServerConfig) editor.remove(KEY_SERVER_URL).remove(KEY_USERNAME)
        editor
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ENCRYPTED_ACCESS_TOKEN)
            .remove(KEY_TOKEN_IV)
            .remove(KEY_USER_ID)
            .remove(KEY_SERVER_ID)
            .remove(KEY_SERVER_NAME)
            .remove(KEY_SERVER_VERSION)
            .apply()
    }

    fun isConnected(): Boolean {
        return getSession() != null
    }

    fun savedServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)
    fun savedUsername(): String? = prefs.getString(KEY_USERNAME, null)

    private fun readEncryptedToken(): String? {
        val encrypted = prefs.getString(KEY_ENCRYPTED_ACCESS_TOKEN, null) ?: return null
        val iv = prefs.getString(KEY_TOKEN_IV, null) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            prefs.edit().remove(KEY_ENCRYPTED_ACCESS_TOKEN).remove(KEY_TOKEN_IV).apply()
            null
        }
    }

    private fun writeEncryptedToken(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = Base64.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        prefs.edit().putString(KEY_ENCRYPTED_ACCESS_TOKEN, encrypted).putString(KEY_TOKEN_IV, iv).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }
}
