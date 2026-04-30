package com.devin.browser.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devin.browser.model.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val context: Context) {

    private val keySearchEngine = stringPreferencesKey("search_engine")
    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyHomePage = stringPreferencesKey("home_page")
    private val keyJavaScript = booleanPreferencesKey("javascript_enabled")
    private val keyBlockPopups = booleanPreferencesKey("block_popups")
    private val keyDoNotTrack = booleanPreferencesKey("do_not_track")
    private val keyForceDarkSites = booleanPreferencesKey("force_dark_sites")

    val searchEngine: Flow<SearchEngine> =
        context.settingsDataStore.data.map { prefs: Preferences ->
            SearchEngine.fromName(prefs[keySearchEngine])
        }

    val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            when (prefs[keyTheme]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    val homePage: Flow<String?> =
        context.settingsDataStore.data.map { it[keyHomePage] }

    val javaScriptEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keyJavaScript] ?: true }

    val blockPopups: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keyBlockPopups] ?: true }

    val doNotTrack: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keyDoNotTrack] ?: true }

    val forceDarkSites: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keyForceDarkSites] ?: false }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.settingsDataStore.edit { it[keySearchEngine] = engine.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[keyTheme] = mode.name }
    }

    suspend fun setHomePage(url: String?) {
        context.settingsDataStore.edit {
            if (url.isNullOrBlank()) it.remove(keyHomePage) else it[keyHomePage] = url
        }
    }

    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyJavaScript] = enabled }
    }

    suspend fun setBlockPopups(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyBlockPopups] = enabled }
    }

    suspend fun setDoNotTrack(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyDoNotTrack] = enabled }
    }

    suspend fun setForceDarkSites(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyForceDarkSites] = enabled }
    }
}
