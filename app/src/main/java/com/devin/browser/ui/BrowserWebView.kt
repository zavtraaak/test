package com.devin.browser.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.devin.browser.model.Tab

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: Tab,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onWebView: (WebView) -> Unit
) {
    val context = LocalContext.current

    val webView = remember(tab.id) { createWebView(context, tab, viewModel) }

    val js by viewModel.javaScriptEnabled.collectAsState()
    val popups by viewModel.blockPopups.collectAsState()
    val dnt by viewModel.doNotTrack.collectAsState()
    val forceDark by viewModel.forceDarkSites.collectAsState()
    val pendingLoad by viewModel.pendingLoad.collectAsState()

    LaunchedEffect(js, popups, dnt, forceDark, tab.id) {
        webView.settings.javaScriptEnabled = js
        webView.settings.javaScriptCanOpenWindowsAutomatically = !popups
        webView.settings.setSupportMultipleWindows(!popups)

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

    DisposableEffect(webView) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { webView },
        update = { /* updates handled via LaunchedEffect */ }
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    tab: Tab,
    viewModel: BrowserViewModel
): WebView {
    val wv = WebView(context)
    wv.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    with(wv.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        useWideViewPort = true
        loadWithOverviewMode = true
        mediaPlaybackRequiresUserGesture = true
        cacheMode = WebSettings.LOAD_DEFAULT
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        userAgentString = userAgentString // keep default mobile UA
        allowContentAccess = true
        allowFileAccess = false
    }

    // Cookies on, except in incognito
    val cm = CookieManager.getInstance()
    cm.setAcceptCookie(!tab.isIncognito)
    cm.setAcceptThirdPartyCookies(wv, !tab.isIncognito)

    if (tab.isIncognito) {
        wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        wv.settings.databaseEnabled = false
        wv.settings.domStorageEnabled = false
    }

    wv.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            viewModel.onPageStarted(tab.id, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            viewModel.onPageFinished(
                tab.id,
                url,
                view.title,
                view.canGoBack(),
                view.canGoForward()
            )
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val url = request.url
            // Custom schemes -> hand off to system
            val scheme = url.scheme
            if (scheme != null && scheme !in setOf("http", "https", "about", "data", "javascript", "file")) {
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, url).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                } catch (_: Throwable) {
                    true
                }
            }
            return false
        }
    }

    wv.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            viewModel.setProgress(tab.id, newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            viewModel.onTitleReceived(tab.id, title)
        }

        override fun onReceivedIcon(view: WebView, icon: Bitmap) {
            viewModel.onFaviconReceived(tab.id, icon)
        }
    }

    wv.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                setDescription("Загрузка из Devin Browser")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                allowScanningByMediaScanner()
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Скачивание: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    })

    return wv
}

fun WebView.applyDesktopMode(enabled: Boolean) {
    settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else null
    settings.useWideViewPort = true
    reload()
}
