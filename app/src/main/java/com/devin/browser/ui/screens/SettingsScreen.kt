package com.devin.browser.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devin.browser.BuildConfig
import com.devin.browser.data.FontSize
import com.devin.browser.data.ThemeMode
import com.devin.browser.data.ToolbarPosition
import com.devin.browser.model.SearchEngine
import com.devin.browser.ui.BrowserViewModel
import com.devin.browser.ui.Screen
import com.devin.browser.util.UpdateChecker
import com.devin.browser.util.UpdateInfo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: BrowserViewModel) {
    val engine by viewModel.searchEngine.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val js by viewModel.javaScriptEnabled.collectAsState()
    val popups by viewModel.blockPopups.collectAsState()
    val dnt by viewModel.doNotTrack.collectAsState()
    val forceDark by viewModel.forceDarkSites.collectAsState()
    val home by viewModel.homePage.collectAsState()
    val toolbarPos by viewModel.toolbarPosition.collectAsState()
    val font by viewModel.fontSize.collectAsState()

    var engineMenu by remember { mutableStateOf(false) }
    var fontMenu by remember { mutableStateOf(false) }
    var homeDialog by remember { mutableStateOf(false) }
    var clearConfirm by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigate(Screen.BROWSER) }) {
                Icon(Icons.Default.Close, "Закрыть")
            }
            Text("Настройки", style = MaterialTheme.typography.titleMedium)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            SectionHeader("Поисковая система")
            Box {
                SettingRow(
                    title = "Поисковик по умолчанию",
                    subtitle = engine.displayName,
                    onClick = { engineMenu = true }
                ) { Icon(Icons.Default.ExpandMore, null) }
                DropdownMenu(expanded = engineMenu, onDismissRequest = { engineMenu = false }) {
                    SearchEngine.entries.forEach { e ->
                        DropdownMenuItem(
                            text = { Text(e.displayName) },
                            onClick = {
                                viewModel.setSearchEngine(e)
                                engineMenu = false
                            },
                            leadingIcon = { RadioButton(selected = e == engine, onClick = null) }
                        )
                    }
                }
            }
            HorizontalDivider()

            SectionHeader("Внешний вид")
            ThemeRow(theme = theme, onSelect = { viewModel.setThemeMode(it) })
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ToolbarPositionRow(position = toolbarPos, onSelect = { viewModel.setToolbarPosition(it) })
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Box {
                SettingRow(
                    title = "Размер шрифта",
                    subtitle = when (font) {
                        FontSize.SMALL -> "Мелкий (85%)"
                        FontSize.NORMAL -> "Обычный (100%)"
                        FontSize.LARGE -> "Крупный (125%)"
                        FontSize.HUGE -> "Огромный (150%)"
                    },
                    onClick = { fontMenu = true }
                ) { Icon(Icons.Default.ExpandMore, null) }
                DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                    FontSize.entries.forEach { f ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (f) {
                                        FontSize.SMALL -> "Мелкий"
                                        FontSize.NORMAL -> "Обычный"
                                        FontSize.LARGE -> "Крупный"
                                        FontSize.HUGE -> "Огромный"
                                    }
                                )
                            },
                            leadingIcon = { RadioButton(selected = f == font, onClick = null) },
                            onClick = { viewModel.setFontSize(f); fontMenu = false }
                        )
                    }
                }
            }
            HorizontalDivider()

            SectionHeader("Главная")
            SettingRow(
                title = "Стартовая страница",
                subtitle = home ?: "Стартовая (плитки + поиск)",
                onClick = { homeDialog = true }
            )
            HorizontalDivider()

            SectionHeader("Конфиденциальность")
            ToggleRow(
                title = "Запрос «Не отслеживать»",
                subtitle = "Отправлять заголовок DNT: 1",
                checked = dnt,
                onCheckedChange = { viewModel.setDoNotTrack(it) }
            )
            ToggleRow(
                title = "Блокировать всплывающие окна",
                subtitle = null,
                checked = popups,
                onCheckedChange = { viewModel.setBlockPopups(it) }
            )
            SettingRow(
                title = "Очистить историю и кэш",
                subtitle = "История, cookies, кэш WebView",
                onClick = { clearConfirm = true }
            )
            HorizontalDivider()

            SectionHeader("Контент")
            ToggleRow(
                title = "JavaScript",
                subtitle = "Включить выполнение JavaScript на сайтах",
                checked = js,
                onCheckedChange = { viewModel.setJavaScriptEnabled(it) }
            )
            ToggleRow(
                title = "Тёмный режим сайтов",
                subtitle = "Применять алгоритмическое затемнение к страницам",
                checked = forceDark,
                onCheckedChange = { viewModel.setForceDarkSites(it) }
            )
            HorizontalDivider()

            SectionHeader("Обновления")
            SettingRow(
                title = if (checkingUpdate) "Проверка…" else "Проверить обновление",
                subtitle = updateResult?.let {
                    if (it.isNewer) "Доступна версия ${it.latestVersion}"
                    else "Установлена последняя версия (${it.currentVersion})"
                } ?: updateError ?: "Текущая версия: ${BuildConfig.VERSION_NAME}",
                onClick = {
                    if (!checkingUpdate) {
                        checkingUpdate = true
                        updateError = null
                        scope.launch {
                            UpdateChecker.check()
                                .onSuccess { updateResult = it }
                                .onFailure { updateError = "Не удалось проверить: ${it.message}" }
                            checkingUpdate = false
                        }
                    }
                }
            )
            updateResult?.let { info ->
                if (info.isNewer) {
                    SettingRow(
                        title = "Скачать обновление",
                        subtitle = info.downloadUrl ?: info.releaseUrl,
                        onClick = {
                            val target = info.downloadUrl ?: info.releaseUrl
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    )
                }
            }
            HorizontalDivider()

            SectionHeader("О приложении")
            SettingRow(
                title = "Devin Browser",
                subtitle = "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = {}
            )

            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (homeDialog) {
        var v by remember { mutableStateOf(home ?: "") }
        AlertDialog(
            onDismissRequest = { homeDialog = false },
            title = { Text("Стартовая страница") },
            text = {
                OutlinedTextField(
                    value = v,
                    onValueChange = { v = it },
                    label = { Text("URL (пусто = стартовая)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setHomePage(v.trim().ifBlank { null })
                    homeDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { homeDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (clearConfirm) {
        AlertDialog(
            onDismissRequest = { clearConfirm = false },
            title = { Text("Очистить данные?") },
            text = { Text("Будут удалены: история, cookies, кэш и формы во всех вкладках.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllBrowsingData()
                    clearConfirm = false
                }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirm = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeRow(theme: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = mode == theme, onClick = { onSelect(mode) })
                Spacer(modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM -> "Тема: как в системе"
                        ThemeMode.LIGHT -> "Тема: светлая"
                        ThemeMode.DARK -> "Тема: тёмная"
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolbarPositionRow(
    position: ToolbarPosition,
    onSelect: (ToolbarPosition) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        ToolbarPosition.entries.forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(p) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = p == position, onClick = { onSelect(p) })
                Spacer(modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = when (p) {
                        ToolbarPosition.TOP -> "Тулбар сверху"
                        ToolbarPosition.BOTTOM -> "Тулбар снизу"
                    }
                )
            }
        }
    }
}
