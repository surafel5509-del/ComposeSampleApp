package com.example.composeapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val projectId: String,
    val schemaVersion: Int = 1,
    val revision: Long = 0,
    val name: String,
    val canvas: Canvas = Canvas(),
    val durationMs: Long = 0,
    val tracks: List<Track> = emptyList(),
)

@Serializable
data class Canvas(
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
)

@Serializable
data class Track(
    val trackId: String,
    val type: TrackType,
    val clips: List<Clip> = emptyList(),
)

@Serializable
enum class TrackType { VIDEO, AUDIO, TEXT, OVERLAY, EFFECT }

@Serializable
data class Clip(
    val clipId: String,
    val assetId: String,
    val startMs: Long,
    val durationMs: Long,
    val sourceStartMs: Long = 0,
    val speed: Float = 1f,
    val edit: ClipEditState = ClipEditState(),
)
