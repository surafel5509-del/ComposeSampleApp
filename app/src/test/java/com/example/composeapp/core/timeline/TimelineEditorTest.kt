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
}
