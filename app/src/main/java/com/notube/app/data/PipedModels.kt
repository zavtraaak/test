package com.notube.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PipedFeedItem(
    val url: String? = null,
    val type: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val shortDescription: String? = null,
    val duration: Long = 0,
    val views: Long = 0,
    val uploaded: Long = 0,
    val uploaderVerified: Boolean = false,
    val isShort: Boolean = false,
)

@Serializable
data class PipedSearchResponse(
    val items: List<PipedFeedItem> = emptyList(),
    val nextpage: String? = null,
    val suggestion: String? = null,
    val corrected: Boolean = false,
)

@Serializable
data class PipedStreamResponse(
    val title: String = "",
    val description: String = "",
    val uploader: String = "",
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploaderVerified: Boolean = false,
    val uploaderSubscriberCount: Long = 0,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    val duration: Long = 0,
    val uploadDate: String? = null,
    val thumbnailUrl: String? = null,
    val hls: String? = null,
    val dash: String? = null,
    val livestream: Boolean = false,
    @SerialName("videoStreams") val videoStreams: List<PipedStream> = emptyList(),
    @SerialName("audioStreams") val audioStreams: List<PipedStream> = emptyList(),
)

@Serializable
data class PipedStream(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    val mimeType: String = "",
    val codec: String? = null,
    val videoOnly: Boolean = false,
    val bitrate: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 0,
)
