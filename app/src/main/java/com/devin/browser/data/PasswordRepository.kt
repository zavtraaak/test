package com.devin.browser.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavedCredential(
    val id: String,
    val site: String,
    val username: String,
    val password: String,
    val savedAt: Long
)

/**
 * Stores saved login credentials encrypted on-device using AES256_GCM via Jetpack Security.
 * The master key is hardware-backed when available.
 *
 * Credentials are kept in a single JSON blob under one preferences key for simplicity.
 */
class PasswordRepository(context: Context) {

    private val prefs: SharedPreferences

    private val _credentials = MutableStateFlow<List<SavedCredential>>(emptyList())
    val credentials: StateFlow<List<SavedCredential>> = _credentials

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "passwords",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        reload()
    }

    private fun reload() {
        _credentials.value = readAll()
    }

    private fun readAll(): List<SavedCredential> {
        val raw = prefs.getString(KEY_BLOB, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SavedCredential(
                    id = o.getString("id"),
                    site = o.getString("site"),
                    username = o.getString("username"),
                    password = o.getString("password"),
                    savedAt = o.optLong("savedAt", System.currentTimeMillis())
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun persist(list: List<SavedCredential>) {
        val arr = JSONArray()
        for (c in list) {
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("site", c.site)
                    .put("username", c.username)
                    .put("password", c.password)
                    .put("savedAt", c.savedAt)
            )
        }
        prefs.edit().putString(KEY_BLOB, arr.toString()).apply()
        _credentials.value = list
    }

    fun save(site: String, username: String, password: String): SavedCredential {
        val now = System.currentTimeMillis()
        val list = readAll().toMutableList()
        val existingIndex = list.indexOfFirst {
            it.site.equals(site, ignoreCase = true) && it.username == username
        }
        val cred = SavedCredential(
            id = if (existingIndex >= 0) list[existingIndex].id else java.util.UUID.randomUUID().toString(),
            site = site,
            username = username,
            password = password,
            savedAt = now
        )
        if (existingIndex >= 0) list[existingIndex] = cred else list.add(cred)
        persist(list)
        return cred
    }

    fun delete(id: String) {
        persist(readAll().filterNot { it.id == id })
    }

    fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
        _credentials.value = emptyList()
    }

    fun forSite(site: String): List<SavedCredential> =
        readAll().filter { it.site.equals(site, ignoreCase = true) }

    companion object {
        private const val KEY_BLOB = "credentials_v1"
    }
}
