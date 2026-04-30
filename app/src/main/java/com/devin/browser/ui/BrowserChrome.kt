package com.devin.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.devin.browser.data.ToolbarPosition
import com.devin.browser.model.Tab
import com.devin.browser.util.UrlUtils

@Composable
fun BrowserChrome(
    viewModel: BrowserViewModel,
    tab: Tab?,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    findQuery: String?,
    onFindChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onFindClose: () -> Unit
) {
    val position by viewModel.toolbarPosition.collectAsState()
    val isTop = position == ToolbarPosition.TOP

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isTop) Modifier.statusBarsPadding() else Modifier)
    ) {
        if (isTop) {
            // Top: address bar first, then progress, then bottom-bar of actions
            if (findQuery != null) {
                FindBar(findQuery, onFindChange, onFindNext, onFindPrev, onFindClose)
            } else {
                AddressBar(viewModel, tab, onReload, onStop)
            }
            ProgressLine(tab)
            ActionRow(viewModel, tab, onBack, onForward, addNavBarPadding = false)
        } else {
            // Bottom: actions row, then progress (above), then address bar at the very bottom
            ProgressLine(tab)
            ActionRow(viewModel, tab, onBack, onForward, addNavBarPadding = false)
            if (findQuery != null) {
                FindBar(findQuery, onFindChange, onFindNext, onFindPrev, onFindClose)
            } else {
                AddressBar(viewModel, tab, onReload, onStop)
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun ProgressLine(tab: Tab?) {
    if (tab?.isLoading == true && tab.progress in 1..99) {
        LinearProgressIndicator(
            progress = { tab.progress / 100f },
            modifier = Modifier.fillMaxWidth().height(2.dp)
        )
    } else {
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun AddressBar(
    viewModel: BrowserViewModel,
    tab: Tab?,
    onReload: () -> Unit,
    onStop: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var editing by remember { mutableStateOf(false) }
    var fieldValue by remember(tab?.url, editing) {
        mutableStateOf(if (editing) tab?.url ?: "" else "")
    }

    val secure = tab?.url?.startsWith("https://") == true
    val isStart = tab?.url?.let { viewModel.isStartPage(it) } == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable {
                    editing = true
                    fieldValue = if (isStart) "" else tab?.url ?: ""
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (editing) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    singleLine = true,
                    placeholder = {
                        Text("Поиск или адрес", style = MaterialTheme.typography.bodyMedium)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = {
                        viewModel.loadInActiveTab(fieldValue)
                        editing = false
                        focusManager.clearFocus()
                    }),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = when {
                            isStart -> Icons.Default.Search
                            secure -> Icons.Default.Lock
                            else -> Icons.Default.LockOpen
                        },
                        contentDescription = null,
                        tint = if (secure || isStart) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tab?.let {
                            when {
                                isStart -> "Поиск или адрес"
                                else -> UrlUtils.host(it.url).ifBlank { it.url }
                            }
                        } ?: "Поиск или адрес",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (tab?.isIncognito == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Инкогнито",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (!isStart) {
            IconButton(onClick = { if (tab?.isLoading == true) onStop() else onReload() }) {
                Icon(
                    imageVector = if (tab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (tab?.isLoading == true) "Остановить" else "Обновить"
                )
            }
        }
    }
}

@Composable
private fun FindBar(
    query: String,
    onChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Закрыть")
        }
        TextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Поиск на странице") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Вперёд") }
    }
}

@Composable
private fun ActionRow(
    viewModel: BrowserViewModel,
    tab: Tab?,
    onBack: () -> Unit,
    onForward: () -> Unit,
    addNavBarPadding: Boolean
) {
    val tabs by viewModel.tabs.collectAsState()
    val tabCount = tabs.size
    var menuOpen by remember { mutableStateOf(false) }
    var bookmarked by remember(tab?.url) { mutableStateOf(false) }
    val context = LocalContext.current
    val isStart = tab?.url?.let { viewModel.isStartPage(it) } == true

    LaunchedEffect(tab?.url) {
        bookmarked = tab?.let { if (isStart) false else viewModel.isBookmarked(it.url) } ?: false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (addNavBarPadding) Modifier.navigationBarsPadding() else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, enabled = tab?.canGoBack == true) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
        }
        IconButton(onClick = onForward, enabled = tab?.canGoForward == true) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Вперёд")
        }
        IconButton(
            enabled = !isStart,
            onClick = {
                tab?.let {
                    viewModel.toggleBookmarkActive()
                    bookmarked = !bookmarked
                }
            }
        ) {
            Icon(
                imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Закладка"
            )
        }
        TabSwitcher(count = tabCount) { viewModel.navigate(Screen.TABS) }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, "Меню")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Новая вкладка") },
                    leadingIcon = { Icon(Icons.Outlined.Tab, null) },
                    onClick = { menuOpen = false; viewModel.newTab(false) }
                )
                DropdownMenuItem(
                    text = { Text("Новая вкладка инкогнито") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    onClick = { menuOpen = false; viewModel.newTab(true) }
                )
                DropdownMenuItem(
                    text = { Text("История") },
                    leadingIcon = { Icon(Icons.Default.History, null) },
                    onClick = { menuOpen = false; viewModel.navigate(Screen.HISTORY) }
                )
                DropdownMenuItem(
                    text = { Text("Закладки") },
                    leadingIcon = { Icon(Icons.Default.Bookmark, null) },
                    onClick = { menuOpen = false; viewModel.navigate(Screen.BOOKMARKS) }
                )
                DropdownMenuItem(
                    text = { Text("Пароли") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                    onClick = { menuOpen = false; viewModel.navigate(Screen.PASSWORDS) }
                )
                DropdownMenuItem(
                    text = { Text("Поиск на странице") },
                    leadingIcon = { Icon(Icons.Outlined.FindInPage, null) },
                    onClick = { menuOpen = false; viewModel.startFind() },
                    enabled = !isStart
                )
                DropdownMenuItem(
                    text = { Text("Поделиться") },
                    leadingIcon = { Icon(Icons.Default.Share, null) },
                    enabled = !isStart && tab != null,
                    onClick = {
                        menuOpen = false
                        tab?.let {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, it.title)
                                putExtra(Intent.EXTRA_TEXT, it.url)
                            }
                            context.startActivity(Intent.createChooser(send, "Поделиться ссылкой"))
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Главная") },
                    leadingIcon = { Icon(Icons.Default.Home, null) },
                    onClick = { menuOpen = false; viewModel.goHome() }
                )
                DropdownMenuItem(
                    text = { Text("Настройки") },
                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                    onClick = { menuOpen = false; viewModel.navigate(Screen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun TabSwitcher(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
