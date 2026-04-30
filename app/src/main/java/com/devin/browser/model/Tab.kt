package com.devin.browser.model

import android.graphics.Bitmap

data class Tab(
    val id: Long,
    var url: String,
    var title: String = "",
    var favicon: Bitmap? = null,
    var progress: Int = 0,
    var isLoading: Boolean = false,
    val isIncognito: Boolean = false,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false
)
