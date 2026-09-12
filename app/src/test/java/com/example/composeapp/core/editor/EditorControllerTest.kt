package com.example.composeapp.core.editor

import com.example.composeapp.core.database.ProjectEntity
import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.model.Track
import com.example.composeapp.core.model.TrackType
import com.example.composeapp.core.project.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorControllerTest {
    @Test
    fun deleteUndoRedoRestoresAndReappliesProject() = runBlocking {
        val original = Project(
            projectId = "p",
            name = "Test",
            durationMs = 2_000,
            tracks = listOf(Track("t", TrackType.VIDEO, listOf(Clip("c", "a", 0, 2_000))),),
        )
        val repository = InMemoryProjectRepository(original)
        val controller = EditorController(repository)

        assertTrue(controller.open("p"))
        controller.selectClip("c")
        assertTrue(controller.deleteSelected())
        assertTrue(controller.state.value.canUndo)
        assertEquals(0, controller.state.value.project!!.tracks.single().clips.size)

        assertTrue(controller.undo())
        assertEquals(1, controller.state.value.project!!.tracks.single().clips.size)
        assertTrue(controller.state.value.canRedo)

        assertTrue(controller.redo())
        assertEquals(0, controller.state.value.project!!.tracks.single().clips.size)
        assertFalse(controller.state.value.canRedo)
    }

    private class InMemoryProjectRepository(initial: Project) : ProjectRepository {
        private var project = initial

        override fun observeProjects(): Flow<List<ProjectEntity>> = emptyFlow()
        override suspend fun createProject(name: String, canvas: com.example.composeapp.core.model.Canvas): Project = project
        override suspend fun getProject(projectId: String): Project? = project.takeIf { it.projectId == projectId }
        override suspend fun saveProject(project: Project) { this.project = project }
        override suspend fun deleteProject(projectId: String) { if (project.projectId == projectId) project = project.copy(name = "deleted") }
    }
}
