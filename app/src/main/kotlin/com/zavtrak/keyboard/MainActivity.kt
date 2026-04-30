package com.zavtrak.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zavtrak.keyboard.data.SettingsRepository
import com.zavtrak.keyboard.data.ThemeMode
import com.zavtrak.keyboard.ui.SettingsViewModel
import com.zavtrak.keyboard.ui.AppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZavtraTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun ZavtraTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { SettingsRepository.get(context) }
    val settings by repo.flow.collectAsState(initial = com.zavtrak.keyboard.data.AppSettings())
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Amoled -> true
        ThemeMode.System -> systemDark
    }
    val supportsDynamic = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val scheme = when {
        settings.materialYou && supportsDynamic && isDark -> dynamicDarkColorScheme(context)
        settings.materialYou && supportsDynamic -> dynamicLightColorScheme(context)
        isDark -> darkColorScheme(primary = Color(settings.accentColor))
        else -> lightColorScheme(primary = Color(settings.accentColor))
    }
    MaterialTheme(colorScheme = scheme) {
        Surface(color = scheme.background) { content() }
    }
}

object ImeUtils {
    fun isMyImeEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val pkg = context.packageName
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    fun isMyImeDefault(context: Context): Boolean {
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        return current.startsWith(context.packageName + "/")
    }

    fun openImeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
    }

    fun showImePicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}
