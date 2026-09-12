package com.example.composeapp.core.project

import com.example.composeapp.core.database.ProjectDao
import com.example.composeapp.core.database.ProjectEntity
import com.example.composeapp.core.model.Canvas
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.storage.ProjectFileStore
import com.example.composeapp.core.sync.SyncState
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineProjectRepository(
    private val projectDao: ProjectDao,
    private val fileStore: ProjectFileStore,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<Project>> =
        projectDao.observeProjects().map { entities ->
            entities.mapNotNull { fileStore.load(it.projectId) }
        }

    override suspend fun create(name: String): Project {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Project name must not be blank" }
        val project = Project(
            projectId = UUID.randomUUID().toString(),
            name = normalized,
            canvas = Canvas(),
        )
        save(project)
        return project
    }

    override suspend fun get(projectId: String): Project? = fileStore.load(projectId)

    override suspend fun save(project: Project): Project {
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
        return project
    }

    override suspend fun delete(projectId: String) {
        fileStore.delete(projectId)
        projectDao.delete(projectId)
    }
}
