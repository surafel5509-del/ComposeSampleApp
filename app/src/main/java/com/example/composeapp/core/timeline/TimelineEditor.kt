package com.example.composeapp.core.timeline

import com.example.composeapp.core.model.Project

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
        return project.copy(
            revision = project.revision + 1,
            tracks = project.tracks.map { current ->
                if (current.trackId == trackId) {
                    current.copy(clips = current.clips.flatMap { if (it.clipId == clipId) listOf(first, second) else listOf(it) })
                } else current
            },
        )
    }

    fun trim(project: Project, trackId: String, clipId: String, newStartMs: Long, newDurationMs: Long): Project {
        if (newStartMs < 0L || newDurationMs <= 0L) return project
        val track = project.tracks.firstOrNull { it.trackId == trackId } ?: return project
        val clip = track.clips.firstOrNull { it.clipId == clipId } ?: return project
        val timelineDelta = newStartMs - clip.startMs
        val newSourceStart = (clip.sourceStartMs + timelineDelta).coerceAtLeast(0L)
        return project.copy(
            revision = project.revision + 1,
            durationMs = maxDuration(project, trackId, clipId, newStartMs, newDurationMs),
            tracks = project.tracks.map { current ->
                if (current.trackId != trackId) current
                else current.copy(clips = current.clips.map { currentClip ->
                    if (currentClip.clipId != clipId) currentClip
                    else currentClip.copy(
                        startMs = newStartMs,
                        durationMs = newDurationMs,
                        sourceStartMs = newSourceStart,
                    )
                })
            },
        )
    }

    fun delete(project: Project, trackId: String, clipId: String): Project {
        val track = project.tracks.firstOrNull { it.trackId == trackId } ?: return project
        if (track.clips.none { it.clipId == clipId }) return project
        val tracks = project.tracks.map { current ->
            if (current.trackId == trackId) current.copy(clips = current.clips.filterNot { it.clipId == clipId }) else current
        }
        return project.copy(
            revision = project.revision + 1,
            durationMs = tracks.maxOfOrNull { current -> current.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L } ?: 0L,
            tracks = tracks,
        )
    }

    fun reorder(project: Project, trackId: String, clipId: String, targetIndex: Int): Project {
        val track = project.tracks.firstOrNull { it.trackId == trackId } ?: return project
        val currentIndex = track.clips.indexOfFirst { it.clipId == clipId }
        if (currentIndex < 0 || targetIndex !in track.clips.indices || currentIndex == targetIndex) return project
        val reordered = track.clips.toMutableList().apply {
            add(targetIndex, removeAt(currentIndex))
        }
        return project.copy(
            revision = project.revision + 1,
            tracks = project.tracks.map { current -> if (current.trackId == trackId) current.copy(clips = reordered) else current },
        )
    }

    private fun maxDuration(project: Project, trackId: String, clipId: String, startMs: Long, durationMs: Long): Long =
        project.tracks.maxOfOrNull { track ->
            track.clips.maxOfOrNull { clip ->
                if (track.trackId == trackId && clip.clipId == clipId) startMs + durationMs else clip.startMs + clip.durationMs
            } ?: 0L
        } ?: 0L
}
