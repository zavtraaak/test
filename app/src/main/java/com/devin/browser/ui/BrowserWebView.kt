package com.devin.browser.ui

import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.devin.browser.data.FontSize
import com.devin.browser.model.Tab

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@Composable
fun BrowserWebView(
    tab: Tab,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onWebView: (WebView) -> Unit
) {
    val webView = viewModel.webViewStore.getOrCreate(tab)

    val js by viewModel.javaScriptEnabled.collectAsState()
    val popups by viewModel.blockPopups.collectAsState()
    val dnt by viewModel.doNotTrack.collectAsState()
    val forceDark by viewModel.forceDarkSites.collectAsState()
    val font by viewModel.fontSize.collectAsState()
    val pendingLoad by viewModel.pendingLoad.collectAsState()

    LaunchedEffect(js, popups, dnt, forceDark, font, tab.id) {
        webView.settings.javaScriptEnabled = js
        webView.settings.javaScriptCanOpenWindowsAutomatically = !popups
        webView.settings.setSupportMultipleWindows(!popups)
        webView.settings.textZoom = when (font) {
            FontSize.SMALL -> 85
            FontSize.NORMAL -> 100
            FontSize.LARGE -> 125
            FontSize.HUGE -> 150
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, forceDark)
        }
    }

    LaunchedEffect(pendingLoad?.first, pendingLoad?.second) {
        val p = pendingLoad ?: return@LaunchedEffect
        if (p.first == tab.id) {
            val headers = if (dnt) mapOf("DNT" to "1") else emptyMap()
            webView.loadUrl(p.second, headers)
            viewModel.consumePendingLoad()
        }
    }

    LaunchedEffect(webView) { onWebView(webView) }

    AndroidView(
        modifier = modifier,
        factory = {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView
        },
        update = { /* settings handled in LaunchedEffect */ }
    )
}

fun WebView.applyDesktopMode(enabled: Boolean) {
    settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else null
    settings.useWideViewPort = true
    reload()
}
