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
enum class ToolbarPosition { TOP, BOTTOM }
enum class FontSize { SMALL, NORMAL, LARGE, HUGE }

class SettingsRepository(private val context: Context) {

    private val keySearchEngine = stringPreferencesKey("search_engine")
    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyHomePage = stringPreferencesKey("home_page")
    private val keyJavaScript = booleanPreferencesKey("javascript_enabled")
    private val keyBlockPopups = booleanPreferencesKey("block_popups")
    private val keyDoNotTrack = booleanPreferencesKey("do_not_track")
    private val keyForceDarkSites = booleanPreferencesKey("force_dark_sites")
    private val keyToolbarPosition = stringPreferencesKey("toolbar_position")
    private val keySwipeToRefresh = booleanPreferencesKey("swipe_to_refresh")
    private val keyFontSize = stringPreferencesKey("font_size")
    private val keyAdblock = booleanPreferencesKey("adblock_enabled")

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

    val toolbarPosition: Flow<ToolbarPosition> =
        context.settingsDataStore.data.map {
            when (it[keyToolbarPosition]) {
                "TOP" -> ToolbarPosition.TOP
                else -> ToolbarPosition.BOTTOM
            }
        }

    val swipeToRefresh: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keySwipeToRefresh] ?: true }

    val fontSize: Flow<FontSize> =
        context.settingsDataStore.data.map {
            when (it[keyFontSize]) {
                "SMALL" -> FontSize.SMALL
                "LARGE" -> FontSize.LARGE
                "HUGE" -> FontSize.HUGE
                else -> FontSize.NORMAL
            }
        }

    val adblockEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keyAdblock] ?: false }

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

    suspend fun setToolbarPosition(p: ToolbarPosition) {
        context.settingsDataStore.edit { it[keyToolbarPosition] = p.name }
    }

    suspend fun setSwipeToRefresh(enabled: Boolean) {
        context.settingsDataStore.edit { it[keySwipeToRefresh] = enabled }
    }

    suspend fun setFontSize(size: FontSize) {
        context.settingsDataStore.edit { it[keyFontSize] = size.name }
    }

    suspend fun setAdblockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyAdblock] = enabled }
    }
}
