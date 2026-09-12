package com.example.composeapp.core.editor

import com.example.composeapp.core.model.Project
import com.example.composeapp.core.project.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditorController(
    private val repository: ProjectRepository,
) {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    suspend fun open(projectId: String): Boolean {
        val project = repository.getProject(projectId) ?: return false
        _state.value = EditorState(project = project)
        return true
    }

    fun selectClip(clipId: String?) {
        _state.update { it.copy(selectedClipId = clipId) }
    }

    fun seekTo(positionMs: Long) {
        val duration = _state.value.project?.durationMs ?: 0L
        _state.update { it.copy(playheadMs = positionMs.coerceIn(0L, duration)) }
    }

    fun setPlaying(playing: Boolean) {
        _state.update { it.copy(isPlaying = playing) }
    }

    suspend fun save(): Boolean {
        val project = _state.value.project ?: return false
        repository.saveProject(project)
        _state.update { it.copy(isDirty = false) }
        return true
    }

    fun replaceProject(project: Project) {
        _state.update { it.copy(project = project, isDirty = true) }
    }
}
