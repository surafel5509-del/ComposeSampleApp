package com.example.composeapp.core.project

import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.model.Track
import com.example.composeapp.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectDocumentCodecTest {
    private val codec = KotlinxProjectDocumentCodec()

    @Test
    fun roundTrip_preservesProjectStructure() {
        val project = Project(
            projectId = "project-1",
            revision = 4,
            name = "Travel",
            tracks = listOf(
                Track(
                    trackId = "track-1",
                    type = TrackType.VIDEO,
                    clips = listOf(
                        Clip(
                            clipId = "clip-1",
                            assetId = "asset-1",
                            startMs = 0,
                            durationMs = 5_000,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(project, codec.decode(codec.encode(project)))
    }
}
