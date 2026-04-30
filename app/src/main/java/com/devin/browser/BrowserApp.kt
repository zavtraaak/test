package com.devin.browser

import android.app.Application
import com.devin.browser.data.BrowserDatabase
import com.devin.browser.data.SettingsRepository
import com.devin.browser.data.PasswordRepository

class BrowserApp : Application() {
    val database: BrowserDatabase by lazy { BrowserDatabase.create(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val passwords: PasswordRepository by lazy { PasswordRepository(this) }
}
