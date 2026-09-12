package com.example.composeapp.core.timeline

import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class TimelineState(
    val durationMs: Long = 0,
    val tracks: List<Track> = emptyList(),
    val playheadMs: Long = 0,
    val selectedClipIds: Set<String> = emptySet(),
    val markers: List<TimelineMarker> = emptyList(),
)

@Serializable
data class TimelineMarker(
    val markerId: String,
    val positionMs: Long,
    val label: String? = null,
)

object TimelineOperations {
    fun splitClip(clip: Clip, positionMs: Long): Pair<Clip, Clip> {
        require(positionMs > clip.startMs) { "Split position must be after clip start" }
        require(positionMs < clip.startMs + clip.durationMs) { "Split position must be before clip end" }
        val firstDuration = positionMs - clip.startMs
        val second = clip.copy(
            clipId = "${clip.clipId}-split",
            startMs = positionMs,
            durationMs = clip.durationMs - firstDuration,
            sourceStartMs = clip.sourceStartMs + firstDuration,
        )
        return clip.copy(durationMs = firstDuration) to second
    }
}
