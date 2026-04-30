package com.devin.browser.model

enum class SearchEngine(
    val displayName: String,
    val queryUrl: String,
    val homeUrl: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com/"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com/"),
    YANDEX("Yandex", "https://yandex.com/search/?text=%s", "https://yandex.com/"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com/"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s", "https://www.ecosia.org/");

    fun searchUrl(query: String): String =
        queryUrl.replace("%s", java.net.URLEncoder.encode(query, "UTF-8"))

    companion object {
        val DEFAULT: SearchEngine = GOOGLE
        fun fromName(name: String?): SearchEngine =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
