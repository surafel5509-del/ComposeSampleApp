package com.example.composeapp.core.media

import android.net.Uri

interface MediaEngine {
    suspend fun inspect(uri: Uri): MediaMetadata
    suspend fun extractFrame(uri: Uri, positionMs: Long): FrameResult
}

data class MediaMetadata(
    val durationMs: Long,
    val width: Int?,
    val height: Int?,
    val rotationDegrees: Int,
    val mimeType: String?,
    val audioChannels: Int?,
    val sampleRate: Int?,
)

sealed interface FrameResult {
    data class Success(val timestampMs: Long, val bytes: ByteArray) : FrameResult
    data class Failure(val reason: String) : FrameResult
}
