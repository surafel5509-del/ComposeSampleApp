package com.example.composeapp.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeapp.core.database.DatabaseFactory
import com.example.composeapp.core.database.ProjectEntity
import com.example.composeapp.core.model.Canvas
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.storage.ProjectFileStore
import com.example.composeapp.core.sync.SyncState
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val projectCount: Int = 0,
    val hasRecentProjects: Boolean = false,
    val projects: List<ProjectEntity> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseFactory.create(application)
    private val projectDao = database.projectDao()
    private val fileStore = ProjectFileStore(application)

    val uiState: StateFlow<HomeUiState> = projectDao.observeProjects()
        .map { projects ->
            HomeUiState(
                projectCount = projects.size,
                hasRecentProjects = projects.isNotEmpty(),
                projects = projects,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createProject(name: String): String? {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return null
        val project = Project(projectId = UUID.randomUUID().toString(), name = normalizedName, canvas = Canvas())
        viewModelScope.launch {
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
        return project.projectId
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch { projectDao.delete(projectId) }
    }

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}
