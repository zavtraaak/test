package com.notube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notube.app.data.PipedClient
import com.notube.app.data.PipedFeedItem
import com.notube.app.ui.components.VideoCard
import com.notube.app.ui.theme.YouTubeRed

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onVideoClick: (videoId: String) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<PipedFeedItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            items = PipedClient.withFallback { it.trending(region = "US") }
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load trending"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(onSearchClick = onSearchClick)
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(
                    color = YouTubeRed,
                    modifier = Modifier.align(Alignment.Center),
                )
                error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Couldn't load trending.",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { reloadKey++ }) { Text("Retry", color = YouTubeRed) }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                ) {
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

@Composable
private fun TopBar(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // YouTube-like wordmark: red play badge + "NoTube".
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 22.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .background(YouTubeRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = "NoTube",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 6.dp).weight(1f),
        )
        IconButton(onClick = onSearchClick) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

internal fun videoIdFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val q = url.substringAfter("v=", "")
    if (q.isNotBlank()) return q.substringBefore('&')
    // youtu.be/<id>
    return url.substringAfterLast('/').takeIf { it.isNotBlank() }
}
