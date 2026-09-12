package com.example.composeapp.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeapp.core.database.DatabaseFactory
import com.example.composeapp.core.database.ProjectEntity
import com.example.composeapp.core.project.OfflineProjectRepository
import com.example.composeapp.core.storage.ProjectFileStore
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
    private val repository = OfflineProjectRepository(
        projectDao = database.projectDao(),
        fileStore = ProjectFileStore(application),
    )

    val uiState: StateFlow<HomeUiState> = repository.observeProjects()
        .map { projects ->
            HomeUiState(
                projectCount = projects.size,
                hasRecentProjects = projects.isNotEmpty(),
                projects = projects,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createProject(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return
        viewModelScope.launch { repository.createProject(normalizedName) }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch { repository.deleteProject(projectId) }
    }

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}
