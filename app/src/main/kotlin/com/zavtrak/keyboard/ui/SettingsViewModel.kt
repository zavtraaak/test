package com.zavtrak.keyboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zavtrak.keyboard.data.AppSettings
import com.zavtrak.keyboard.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository.get(app)

    val state: StateFlow<AppSettings> = repo.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun reset() = viewModelScope.launch { repo.reset() }

    fun export(): String = repo.exportJson(state.value)

    fun importJson(json: String): Boolean {
        val parsed = repo.importJson(json) ?: return false
        viewModelScope.launch { repo.replace(parsed) }
        return true
    }
}
