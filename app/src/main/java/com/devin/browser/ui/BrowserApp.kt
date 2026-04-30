package com.devin.browser.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.devin.browser.data.ToolbarPosition
import com.devin.browser.ui.screens.BookmarksScreen
import com.devin.browser.ui.screens.HistoryScreen
import com.devin.browser.ui.screens.PasswordsScreen
import com.devin.browser.ui.screens.SettingsScreen
import com.devin.browser.ui.screens.StartPage
import com.devin.browser.ui.screens.TabsScreen

@Composable
fun BrowserApp(viewModel: BrowserViewModel) {
    val screen by viewModel.screen.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    val findQuery by viewModel.findQuery.collectAsState()
    val toolbarPosition by viewModel.toolbarPosition.collectAsState()

    BackHandler(enabled = true) {
        when (screen) {
            Screen.BROWSER -> {
                val wv = activeWebView
                val tab = activeTab
                when {
                    tab != null && viewModel.isStartPage(tab.url).not() && wv != null && wv.canGoBack() -> wv.goBack()
                    tab != null && tabs.size > 1 -> viewModel.closeTab(tab.id)
                    else -> { /* allow exit */ }
                }
            }
            Screen.FIND -> {
                activeWebView?.clearMatches()
                viewModel.stopFind()
            }
            else -> viewModel.navigate(Screen.BROWSER)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (screen) {
            Screen.BROWSER, Screen.FIND -> {
                val chrome: @Composable () -> Unit = {
                    BrowserChrome(
                        viewModel = viewModel,
                        tab = activeTab,
                        onReload = { activeWebView?.reload() },
                        onStop = { activeWebView?.stopLoading() },
                        onBack = { activeWebView?.goBack() },
                        onForward = { activeWebView?.goForward() },
                        findQuery = findQuery,
                        onFindNext = { activeWebView?.findNext(true) },
                        onFindPrev = { activeWebView?.findNext(false) },
                        onFindChange = { q ->
                            viewModel.setFindQuery(q)
                            activeWebView?.findAllAsync(q)
                        },
                        onFindClose = {
                            activeWebView?.clearMatches()
                            viewModel.stopFind()
                        }
                    )
                }
                val content: @Composable () -> Unit = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (activeTab != null) {
                            if (viewModel.isStartPage(activeTab.url)) {
                                StartPage(viewModel = viewModel)
                            } else {
                                BrowserWebView(
                                    tab = activeTab,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    onWebView = { activeWebView = it }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (toolbarPosition == ToolbarPosition.TOP) {
                        chrome()
                        Box(modifier = Modifier.weight(1f)) { content() }
                    } else {
                        Box(modifier = Modifier.weight(1f)) { content() }
                        chrome()
                    }
                }
            }
            Screen.TABS -> TabsScreen(viewModel = viewModel)
            Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
            Screen.BOOKMARKS -> BookmarksScreen(viewModel = viewModel)
            Screen.PASSWORDS -> PasswordsScreen(viewModel = viewModel)
            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
        }
    }
}
