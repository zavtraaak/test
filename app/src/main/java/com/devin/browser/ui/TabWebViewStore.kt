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
import com.devin.browser.model.Tab

/**
 * Keeps live [WebView] instances per tab id so that switching screens / tabs
 * doesn't destroy and recreate the WebView (which caused black-screen flashes
 * and forced reloads).
 *
 * Owned by the [BrowserViewModel] for the lifetime of the activity.
 */
class TabWebViewStore(
    private val appContext: Context,
    private val viewModel: BrowserViewModel
) {
    private val views = mutableMapOf<Long, WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreate(tab: Tab): WebView {
        views[tab.id]?.let { return it }
        val wv = createWebView(appContext, tab)
        views[tab.id] = wv
        return wv
    }

    fun release(id: Long) {
        val wv = views.remove(id) ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.stopLoading()
        wv.destroy()
    }

    fun forEach(action: (WebView) -> Unit) {
        views.values.forEach(action)
    }

    fun destroyAll() {
        views.values.forEach { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.destroy()
        }
        views.clear()
    }

    fun clearAllBrowsingData() {
        views.values.forEach { wv ->
            wv.clearHistory()
            wv.clearCache(true)
            wv.clearFormData()
            wv.clearMatches()
            wv.clearSslPreferences()
        }
        WebView(appContext).also {
            it.clearCache(true)
            it.clearHistory()
            it.destroy()
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context, tab: Tab): WebView {
        val wv = WebView(context)
        wv.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        with(wv.settings) {
            javaScriptEnabled = true
            domStorageEnabled = !tab.isIncognito
            databaseEnabled = !tab.isIncognito
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = true
            cacheMode = if (tab.isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowContentAccess = true
            allowFileAccess = false
        }

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(!tab.isIncognito)
        cm.setAcceptThirdPartyCookies(wv, !tab.isIncognito)

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
                val scheme = url.scheme
                if (scheme != null && scheme !in setOf("http", "https", "about", "data", "javascript", "file")) {
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, url).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        true
                    } catch (_: Throwable) { true }
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
}
