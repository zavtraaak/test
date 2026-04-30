package com.devin.browser.util

import com.devin.browser.model.SearchEngine

object UrlUtils {

    private val urlRegex = Regex(
        "^(https?://)?([\\w-]+\\.)+[\\w-]+(:\\d+)?(/.*)?$",
        RegexOption.IGNORE_CASE
    )

    fun normalizeOrSearch(input: String, engine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return engine.homeUrl

        // already a full URL
        if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            return trimmed
        }
        // about: / file: / data:
        val schemes = listOf("about:", "file:", "data:", "javascript:", "chrome:")
        if (schemes.any { trimmed.startsWith(it, true) }) return trimmed

        // localhost or IP
        if (trimmed.startsWith("localhost", true) ||
            trimmed.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$"))
        ) {
            return "http://$trimmed"
        }

        // looks like a host (contains dot, no spaces)
        if (!trimmed.contains(' ') && trimmed.contains('.') && urlRegex.matches(trimmed)) {
            return "https://$trimmed"
        }

        return engine.searchUrl(trimmed)
    }

    fun host(url: String): String =
        try {
            java.net.URI(url).host ?: url
        } catch (_: Throwable) {
            url
        }

    fun faviconUrl(pageUrl: String): String? {
        return try {
            val uri = java.net.URI(pageUrl)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: return null
            "$scheme://$host/favicon.ico"
        } catch (_: Throwable) {
            null
        }
    }
}
