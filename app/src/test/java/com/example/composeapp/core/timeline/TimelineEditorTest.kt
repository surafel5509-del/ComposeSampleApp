package com.example.composeapp.core.timeline

import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.model.Track
import com.example.composeapp.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineEditorTest {
    @Test
    fun splitCreatesTwoContiguousClipsAndAdvancesRevision() {
        val project = Project(
            projectId = "p",
            name = "Test",
            durationMs = 10_000,
            tracks = listOf(
                Track("t", TrackType.VIDEO, listOf(Clip("c", "a", 0, 10_000))),
            ),
        )
        val result = TimelineEditor.split(project, "t", "c", 4_000)
        val clips = result.tracks.single().clips
        assertEquals(2, clips.size)
        assertEquals(4_000, clips[0].durationMs)
        assertEquals(4_000, clips[1].startMs)
        assertEquals(6_000, clips[1].durationMs)
        assertEquals(4_000, clips[1].sourceStartMs)
        assertEquals(1, result.revision)
    }

    @Test
    fun trimRejectsInvalidDuration() {
        val project = Project(
            projectId = "p",
            name = "Test",
            tracks = listOf(Track("t", TrackType.VIDEO, listOf(Clip("c", "a", 0, 1000)))),
        )
        assertEquals(project, TimelineEditor.trim(project, "t", "c", 0, 0))
    }

    @Test
    fun trimMovesSourceStartWhenLeftEdgeMoves() {
        val project = Project(
            projectId = "p",
            name = "Test",
            durationMs = 5_000,
            tracks = listOf(Track("t", TrackType.VIDEO, listOf(Clip("c", "a", 1_000, 2_000, sourceStartMs = 500)))),
        )
        val result = TimelineEditor.trim(project, "t", "c", 1_500, 1_000)
        val clip = result.tracks.single().clips.single()
        assertEquals(1_500, clip.startMs)
        assertEquals(1_000, clip.durationMs)
        assertEquals(1_000, clip.sourceStartMs)
        assertEquals(2_500, result.durationMs)
    }

    @Test
    fun deleteRemovesClipAndRecomputesDuration() {
        val project = Project(
            projectId = "p",
            name = "Test",
            durationMs = 5_000,
            tracks = listOf(
                Track("t", TrackType.VIDEO, listOf(Clip("a", "asset", 0, 2_000), Clip("b", "asset", 2_000, 3_000))),
            ),
        )
        val result = TimelineEditor.delete(project, "t", "b")
        assertEquals(1, result.tracks.single().clips.size)
        assertEquals("a", result.tracks.single().clips.single().clipId)
        assertEquals(2_000, result.durationMs)
        assertEquals(1, result.revision)
    }

    @Test
    fun reorderMovesClipAndRetimesTrackContiguously() {
        val project = Project(
            projectId = "p",
            name = "Test",
            durationMs = 4_500,
            tracks = listOf(
                Track("t", TrackType.VIDEO, listOf(
                    Clip("a", "asset", 0, 1_000),
                    Clip("b", "asset", 1_000, 1_500),
                    Clip("c", "asset", 2_500, 2_000),
                )),
            ),
        )
        val result = TimelineEditor.reorder(project, "t", "c", 0)
        val clips = result.tracks.single().clips
        assertEquals(listOf("c", "a", "b"), clips.map { it.clipId })
        assertEquals(listOf(0L, 2_000L, 3_000L), clips.map { it.startMs })
        assertEquals(4_500, result.durationMs)
        assertEquals(1, result.revision)
    }
}
