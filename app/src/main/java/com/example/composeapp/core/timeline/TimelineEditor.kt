package com.example.composeapp.core.timeline

import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.model.Track

object TimelineEditor {
    fun split(project: Project, trackId: String, clipId: String, atTimelineMs: Long): Project {
        val track = project.tracks.firstOrNull { it.trackId == trackId } ?: return project
        val clip = track.clips.firstOrNull { it.clipId == clipId } ?: return project
        val offset = atTimelineMs - clip.startMs
        if (offset <= 0L || offset >= clip.durationMs) return project

        val first = clip.copy(durationMs = offset)
        val second = clip.copy(
            clipId = "${clip.clipId}-split-${atTimelineMs}",
            startMs = atTimelineMs,
            durationMs = clip.durationMs - offset,
            sourceStartMs = clip.sourceStartMs + offset,
        )
        val updatedTrack = track.copy(
            clips = track.clips.flatMap { if (it.clipId == clipId) listOf(first, second) else listOf(it) },
        )
        return project.copy(
            revision = project.revision + 1,
            tracks = project.tracks.map { if (it.trackId == trackId) updatedTrack else it },
        )
    }

    fun trim(project: Project, trackId: String, clipId: String, newStartMs: Long, newDurationMs: Long): Project {
        if (newStartMs < 0L || newDurationMs <= 0L) return project
        return project.copy(
            revision = project.revision + 1,
            tracks = project.tracks.map { track ->
                if (track.trackId != trackId) track
                else track.copy(clips = track.clips.map { clip ->
                    if (clip.clipId != clipId) clip
                    else clip.copy(startMs = newStartMs, durationMs = newDurationMs)
                })
            },
        )
    }
}
