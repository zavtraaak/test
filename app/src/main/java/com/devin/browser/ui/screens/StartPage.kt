package com.devin.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devin.browser.model.SearchEngine
import com.devin.browser.ui.BrowserViewModel
import com.devin.browser.util.UrlUtils

private data class QuickLink(val title: String, val url: String, val color: Color, val initial: String)

private val DefaultQuickLinks = listOf(
    QuickLink("YouTube", "https://m.youtube.com", Color(0xFFFF0000), "Y"),
    QuickLink("Wikipedia", "https://m.wikipedia.org", Color(0xFF000000), "W"),
    QuickLink("Reddit", "https://www.reddit.com", Color(0xFFFF4500), "R"),
    QuickLink("GitHub", "https://github.com", Color(0xFF24292E), "G"),
    QuickLink("Twitter", "https://twitter.com", Color(0xFF1DA1F2), "X"),
    QuickLink("Telegram", "https://web.telegram.org", Color(0xFF0088CC), "T"),
    QuickLink("HN", "https://news.ycombinator.com", Color(0xFFFF6600), "H"),
    QuickLink("Maps", "https://maps.google.com", Color(0xFF4285F4), "M"),
)

@Composable
fun StartPage(viewModel: BrowserViewModel) {
    val engine by viewModel.searchEngine.collectAsState()
    val history by viewModel.history.collectAsState()
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }

    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        SearchEngineLogo(engine)

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = {
                    Text(
                        "Поиск в ${engine.displayName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        viewModel.loadInActiveTab(query)
                        focusManager.clearFocus()
                        query = ""
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Часто посещаемые",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Combine bookmarks and most-frequent history into a quick-links grid.
        val frequent = remember(history) {
            history.groupBy { UrlUtils.host(it.url) }
                .toList()
                .sortedByDescending { it.second.size }
                .take(8)
                .map {
                    QuickLink(
                        title = it.second.first().title.take(20).ifBlank { it.first },
                        url = it.second.first().url,
                        color = colorFromString(it.first),
                        initial = it.first.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
                    )
                }
        }
        val quickLinks = (frequent + DefaultQuickLinks).distinctBy { it.url }.take(8)

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(quickLinks) { link ->
                QuickLinkTile(link) { viewModel.loadInActiveTab(link.url) }
            }
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Недавно",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                history.take(5).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.loadInActiveTab(entry.url) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    colorFromString(UrlUtils.host(entry.url)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                UrlUtils.host(entry.url).firstOrNull()?.uppercaseChar()?.toString() ?: "•",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.title.ifBlank { UrlUtils.host(entry.url) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                UrlUtils.host(entry.url),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun QuickLinkTile(link: QuickLink, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(link.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                link.initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            link.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SearchEngineLogo(engine: SearchEngine) {
    val (label, color) = when (engine) {
        SearchEngine.GOOGLE -> "G" to Color(0xFF4285F4)
        SearchEngine.DUCKDUCKGO -> "D" to Color(0xFFDE5833)
        SearchEngine.BING -> "b" to Color(0xFF008373)
        SearchEngine.YANDEX -> "Я" to Color(0xFFFF0000)
        SearchEngine.BRAVE -> "B" to Color(0xFFFB542B)
        SearchEngine.ECOSIA -> "E" to Color(0xFF2E8B57)
    }
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 56.sp
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        engine.displayName,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private fun colorFromString(s: String): Color {
    val palette = listOf(
        Color(0xFF1A73E8), Color(0xFFE5363B), Color(0xFFFB8C00),
        Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFF00838F),
        Color(0xFFC2185B), Color(0xFF455A64)
    )
    val idx = (s.hashCode().rem(palette.size) + palette.size) % palette.size
    return palette[idx]
}
