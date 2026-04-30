package com.notube.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.notube.app.data.PipedClient
import com.notube.app.data.PipedFeedItem
import com.notube.app.ui.components.VideoCard
import com.notube.app.ui.theme.YouTubeRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onVideoClick: (videoId: String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<PipedFeedItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted) {
        if (submitted.isBlank()) return@LaunchedEffect
        loading = true
        error = null
        try {
            items = PipedClient.withFallback { it.search(submitted).items }
        } catch (t: Throwable) {
            error = t.message ?: "Search failed"
        }
        loading = false
    }
    LaunchedEffect(Unit) {
        delay(50)
        focus.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search NoTube") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YouTubeRed,
                    cursorColor = YouTubeRed,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitted = query }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(
                    color = YouTubeRed,
                    modifier = Modifier.align(Alignment.Center),
                )
                error != null -> Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                submitted.isBlank() -> Text(
                    "Tap a query and press search",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                items.isEmpty() -> Text(
                    "No results",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)) {
                    items(items, key = { it.url ?: it.title.orEmpty() }) { item ->
                        VideoCard(item = item, onClick = {
                            val id = videoIdFromUrl(item.url) ?: return@VideoCard
                            onVideoClick(id)
                        })
                    }
                }
            }
        }
    }
}
