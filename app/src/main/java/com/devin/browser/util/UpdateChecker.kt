package com.devin.browser.util

import com.devin.browser.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val downloadUrl: String?,
    val releaseUrl: String,
    val isNewer: Boolean
)

object UpdateChecker {

    private const val GITHUB_REPO = "zavtraaak/test"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val RELEASES_PAGE =
        "https://github.com/$GITHUB_REPO/releases"

    suspend fun check(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DevinBrowser/${BuildConfig.VERSION_NAME}")
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            try {
                if (conn.responseCode == 404) {
                    // No releases yet — treat current version as latest.
                    return@runCatching UpdateInfo(
                        latestVersion = BuildConfig.VERSION_NAME,
                        currentVersion = BuildConfig.VERSION_NAME,
                        downloadUrl = null,
                        releaseUrl = RELEASES_PAGE,
                        isNewer = false
                    )
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name").trimStart('v')
                val htmlUrl = json.optString("html_url", RELEASES_PAGE)
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val name = a.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                UpdateInfo(
                    latestVersion = tag.ifBlank { BuildConfig.VERSION_NAME },
                    currentVersion = BuildConfig.VERSION_NAME,
                    downloadUrl = apkUrl,
                    releaseUrl = htmlUrl,
                    isNewer = isNewer(tag, BuildConfig.VERSION_NAME)
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split('.', '-').mapNotNull { it.toIntOrNull() }
        val c = current.split('.', '-').mapNotNull { it.toIntOrNull() }
        val n = maxOf(r.size, c.size)
        for (i in 0 until n) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
