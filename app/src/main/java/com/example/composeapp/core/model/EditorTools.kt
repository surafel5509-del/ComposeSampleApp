package com.example.composeapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipEditState(
    val filter: String = "Original",
    val effect: String = "None",
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val temperature: Float = 0f,
    val sharpen: Float = 0f,
    val blur: Float = 0f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val volume: Float = 1f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val reverb: Float = 0f,
    val freezeAtMs: Long? = null,
    val freezeDurationMs: Long = 0L,
    val text: String? = null,
    val textSize: Float = 32f,
    val textX: Float = 0.5f,
    val textY: Float = 0.5f,
    val keyframes: List<Keyframe> = emptyList(),
)

@Serializable
data class Keyframe(
    val timeMs: Long,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val opacity: Float = 1f,
)

enum class EditorTool(
    val label: String,
    val icon: String,
) {
    EFFECTS("Effects", "✦"),
    CUT("Cut", "✂"),
    FREEZE("Freeze", "❄"),
    REVERB("Reverb", "≈"),
    AUDIO("Audio", "♫"),
    RECORD("Record", "●"),
    FILTERS("Filters", "◐"),
    CROP("Crop", "□"),
    TEXT("Text", "T"),
    KEYFRAME("Keyframe", "◆"),
    GRAPH("Graph", "⌁"),
    ADJUSTMENT("Adjust", "☼"),
    DUPLICATE("Duplicate", "⧉"),
    SPEED("Speed", "1×"),
    REVERSE("Reverse", "↔"),
    ROTATE("Rotate", "↻"),
    FLIP("Flip", "⇄"),
    VOLUME("Volume", "◖"),
    FADE("Fade", "↗"),
    TRANSITION("Transition", "◇"),
    CAPTIONS("Captions", "CC"),
    STICKERS("Stickers", "★"),
    OVERLAY("Overlay", "▣"),
    CANVAS("Canvas", "▤"),
    EXPORT("Export", "↑"),
}
