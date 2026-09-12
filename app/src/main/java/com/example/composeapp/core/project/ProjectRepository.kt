package com.example.composeapp.core.project

import com.example.composeapp.core.database.ProjectDao
import com.example.composeapp.core.database.ProjectEntity
import com.example.composeapp.core.model.Canvas
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.sync.SyncState
import com.example.composeapp.core.storage.ProjectFileStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ProjectRepository {
    fun observeProjects(): Flow<List<ProjectEntity>>
    suspend fun createProject(name: String, canvas: Canvas = Canvas()): Project
    suspend fun getProject(projectId: String): Project?
    suspend fun saveProject(project: Project)
    suspend fun deleteProject(projectId: String)
}

class OfflineProjectRepository(
    private val projectDao: ProjectDao,
    private val fileStore: ProjectFileStore,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeProjects()

    override suspend fun createProject(name: String, canvas: Canvas): Project {
        val project = Project(
            projectId = UUID.randomUUID().toString(),
            name = name.trim(),
            canvas = canvas,
        )
        saveProject(project)
        return project
    }

    override suspend fun getProject(projectId: String): Project? = fileStore.load(projectId)

    override suspend fun saveProject(project: Project) {
        fileStore.save(project)
        projectDao.upsert(
            ProjectEntity(
                projectId = project.projectId,
                name = project.name,
                schemaVersion = project.schemaVersion,
                revision = project.revision,
                durationMs = project.durationMs,
                canvasWidth = project.canvas.width,
                canvasHeight = project.canvas.height,
                frameRate = project.canvas.frameRate,
                documentPath = "projects/${project.projectId}/project.json",
                syncState = SyncState.LOCAL_ONLY.name,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteProject(projectId: String) {
        fileStore.delete(projectId)
        projectDao.delete(projectId)
    }
}
