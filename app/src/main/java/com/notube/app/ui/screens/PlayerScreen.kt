package com.notube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.notube.app.data.PipedClient
import com.notube.app.data.PipedStreamResponse
import com.notube.app.ui.theme.YouTubeRed
import com.notube.app.util.formatCount

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(videoId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var stream by remember { mutableStateOf<PipedStreamResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        try {
            stream = PipedClient.withFallback { it.streams(videoId) }
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load video"
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(stream) {
        val s = stream ?: return@LaunchedEffect
        val streamUrl = s.hls
            ?: s.videoStreams.firstOrNull { !it.videoOnly }?.url
            ?: s.videoStreams.firstOrNull()?.url
        if (streamUrl != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                error != null -> Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                stream == null -> CircularProgressIndicator(
                    color = YouTubeRed,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    val s = stream!!
                    Text(
                        text = s.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = buildString {
                            if (s.views > 0) append("${formatCount(s.views)} views")
                            if (!s.uploadDate.isNullOrBlank()) append(" • ${s.uploadDate}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = s.uploaderAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                s.uploader,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            if (s.uploaderSubscriberCount > 0) {
                                Text(
                                    "${formatCount(s.uploaderSubscriberCount)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (s.description.isNotBlank()) {
                        Text(
                            text = stripHtml(s.description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun stripHtml(html: String): String =
    html.replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]+>"), "")
