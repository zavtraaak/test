package com.devin.browser.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devin.browser.BrowserApp
import com.devin.browser.data.BookmarkEntry
import com.devin.browser.data.HistoryEntry
import com.devin.browser.data.PasswordRepository
import com.devin.browser.data.FontSize
import com.devin.browser.data.SavedCredential
import com.devin.browser.data.SettingsRepository
import com.devin.browser.data.ThemeMode
import com.devin.browser.data.ToolbarPosition
import com.devin.browser.model.SearchEngine
import com.devin.browser.model.Tab
import com.devin.browser.util.UrlUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

enum class Screen { BROWSER, TABS, HISTORY, BOOKMARKS, PASSWORDS, SETTINGS, FIND }

class BrowserViewModel(
    private val app: BrowserApp
) : AndroidViewModel(app) {

    private val historyDao = app.database.historyDao()
    private val bookmarkDao = app.database.bookmarkDao()
    val passwords: PasswordRepository = app.passwords
    val settings: SettingsRepository = app.settings
    val webViewStore: TabWebViewStore = TabWebViewStore(app.applicationContext, this)

    private val tabIdGen = AtomicLong(1L)

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<Long?>(null)
    val activeTabId: StateFlow<Long?> = _activeTabId.asStateFlow()

    private val _screen = MutableStateFlow(Screen.BROWSER)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _pendingLoad = MutableStateFlow<Pair<Long, String>?>(null)
    val pendingLoad: StateFlow<Pair<Long, String>?> = _pendingLoad.asStateFlow()

    private val _findQuery = MutableStateFlow<String?>(null)
    val findQuery: StateFlow<String?> = _findQuery.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val searchEngine: StateFlow<SearchEngine> = settings.searchEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine.DEFAULT)

    val javaScriptEnabled: StateFlow<Boolean> = settings.javaScriptEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val blockPopups: StateFlow<Boolean> = settings.blockPopups
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val doNotTrack: StateFlow<Boolean> = settings.doNotTrack
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val forceDarkSites: StateFlow<Boolean> = settings.forceDarkSites
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val homePage: StateFlow<String?> = settings.homePage
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val toolbarPosition: StateFlow<ToolbarPosition> = settings.toolbarPosition
        .stateIn(viewModelScope, SharingStarted.Eagerly, ToolbarPosition.BOTTOM)

    val swipeToRefresh: StateFlow<Boolean> = settings.swipeToRefresh
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val fontSize: StateFlow<FontSize> = settings.fontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontSize.NORMAL)

    val adblockEnabled: StateFlow<Boolean> = settings.adblockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val history = historyDao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedPasswords: StateFlow<List<SavedCredential>> = passwords.credentials

    init {
        if (_tabs.value.isEmpty()) newTab(incognito = false, url = null)
    }

    fun isStartPage(url: String?): Boolean =
        url.isNullOrBlank() || url == NEW_TAB_URL || url == "about:blank"

    fun activeTab(): Tab? = _tabs.value.firstOrNull { it.id == _activeTabId.value }

    fun newTab(incognito: Boolean, url: String? = null): Tab {
        val target = url ?: homePage.value ?: NEW_TAB_URL
        val tab = Tab(
            id = tabIdGen.getAndIncrement(),
            url = target,
            title = "Новая вкладка",
            isIncognito = incognito
        )
        _tabs.value = _tabs.value + tab
        _activeTabId.value = tab.id
        if (target != NEW_TAB_URL) _pendingLoad.value = tab.id to target
        _screen.value = Screen.BROWSER
        return tab
    }

    fun openInNewTab(url: String) {
        val resolved = UrlUtils.normalizeOrSearch(url, searchEngine.value)
        newTab(incognito = false, url = resolved)
    }

    fun selectTab(id: Long) {
        if (_tabs.value.any { it.id == id }) {
            _activeTabId.value = id
            _screen.value = Screen.BROWSER
        }
    }

    fun closeTab(id: Long) {
        val list = _tabs.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list.removeAt(idx)
        _tabs.value = list
        webViewStore.release(id)
        if (_activeTabId.value == id) {
            _activeTabId.value = list.getOrNull(idx.coerceAtMost(list.lastIndex))?.id
                ?: list.firstOrNull()?.id
        }
        if (list.isEmpty()) newTab(incognito = false, url = null)
    }

    fun closeAllTabs() {
        _tabs.value.forEach { webViewStore.release(it.id) }
        _tabs.value = emptyList()
        _activeTabId.value = null
        newTab(incognito = false, url = null)
    }

    override fun onCleared() {
        super.onCleared()
        webViewStore.destroyAll()
    }

    fun loadInActiveTab(input: String) {
        val tab = activeTab() ?: newTab(false)
        if (isStartPage(input)) {
            updateTab(tab.id) {
                it.copy(url = NEW_TAB_URL, title = "Новая вкладка", isLoading = false, progress = 0)
            }
            _screen.value = Screen.BROWSER
            return
        }
        val url = UrlUtils.normalizeOrSearch(input, searchEngine.value)
        updateTab(tab.id) { it.copy(url = url, isLoading = true, progress = 0) }
        _pendingLoad.value = tab.id to url
        _screen.value = Screen.BROWSER
    }

    fun consumePendingLoad() {
        _pendingLoad.value = null
    }

    fun updateTab(id: Long, transform: (Tab) -> Tab) {
        _tabs.value = _tabs.value.map { if (it.id == id) transform(it) else it }
    }

    fun setProgress(id: Long, progress: Int) {
        updateTab(id) { it.copy(progress = progress, isLoading = progress in 1..99) }
    }

    fun onPageStarted(id: Long, url: String, favicon: Bitmap?) {
        updateTab(id) {
            it.copy(url = url, isLoading = true, progress = 0, favicon = favicon ?: it.favicon)
        }
    }

    fun onPageFinished(id: Long, url: String, title: String?, canBack: Boolean, canForward: Boolean) {
        val finalTitle = title?.takeIf { it.isNotBlank() } ?: UrlUtils.host(url)
        updateTab(id) {
            it.copy(
                url = url,
                title = finalTitle,
                isLoading = false,
                progress = 100,
                canGoBack = canBack,
                canGoForward = canForward
            )
        }
        val tab = _tabs.value.firstOrNull { it.id == id } ?: return
        if (!tab.isIncognito && url.startsWith("http") && !isStartPage(url)) {
            viewModelScope.launch {
                historyDao.insert(
                    HistoryEntry(
                        url = url,
                        title = finalTitle,
                        visitedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun onTitleReceived(id: Long, title: String) {
        if (title.isBlank()) return
        updateTab(id) { it.copy(title = title) }
    }

    fun onFaviconReceived(id: Long, favicon: Bitmap) {
        updateTab(id) { it.copy(favicon = favicon) }
    }

    // Bookmarks
    fun toggleBookmarkActive() {
        val tab = activeTab() ?: return
        viewModelScope.launch {
            if (bookmarkDao.exists(tab.url)) {
                bookmarkDao.deleteByUrl(tab.url)
            } else {
                bookmarkDao.insert(
                    BookmarkEntry(
                        url = tab.url,
                        title = tab.title.ifBlank { UrlUtils.host(tab.url) },
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun isBookmarked(url: String): Boolean = bookmarkDao.exists(url)

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { bookmarkDao.delete(id) }
    }

    // History
    fun deleteHistory(id: Long) {
        viewModelScope.launch { historyDao.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyDao.clear() }
    }

    // Passwords
    fun savePassword(site: String, user: String, pass: String) {
        passwords.save(site, user, pass)
    }

    fun deletePassword(id: String) = passwords.delete(id)

    fun clearPasswords() = passwords.clear()

    // Settings
    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { settings.setSearchEngine(engine) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setHomePage(url: String?) {
        viewModelScope.launch { settings.setHomePage(url) }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setJavaScriptEnabled(enabled) }
    }

    fun setBlockPopups(enabled: Boolean) {
        viewModelScope.launch { settings.setBlockPopups(enabled) }
    }

    fun setDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { settings.setDoNotTrack(enabled) }
    }

    fun setForceDarkSites(enabled: Boolean) {
        viewModelScope.launch { settings.setForceDarkSites(enabled) }
    }

    fun setToolbarPosition(p: ToolbarPosition) {
        viewModelScope.launch { settings.setToolbarPosition(p) }
    }

    fun setSwipeToRefresh(enabled: Boolean) {
        viewModelScope.launch { settings.setSwipeToRefresh(enabled) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { settings.setFontSize(size) }
    }

    fun setAdblockEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAdblockEnabled(enabled) }
    }

    fun clearAllBrowsingData() {
        viewModelScope.launch {
            historyDao.clear()
            webViewStore.clearAllBrowsingData()
        }
    }

    fun goHome() {
        loadInActiveTab(homePage.value ?: NEW_TAB_URL)
        // ensure start page renders if no home set
        if (homePage.value == null) {
            val tab = activeTab() ?: return
            updateTab(tab.id) { it.copy(url = NEW_TAB_URL, title = "Новая вкладка", progress = 0, isLoading = false) }
        }
    }

    // Find in page
    fun startFind() {
        _findQuery.value = ""
        _screen.value = Screen.FIND
    }

    fun setFindQuery(q: String) {
        _findQuery.value = q
    }

    fun stopFind() {
        _findQuery.value = null
        _screen.value = Screen.BROWSER
    }

    fun navigate(screen: Screen) {
        _screen.value = screen
    }

    companion object {
        const val NEW_TAB_URL = "devin://newtab"

        fun factory(app: BrowserApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BrowserViewModel(app) as T
        }
    }
}
