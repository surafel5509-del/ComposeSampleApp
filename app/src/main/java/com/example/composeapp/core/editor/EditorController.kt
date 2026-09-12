package com.example.composeapp.core.editor

import com.example.composeapp.core.model.Project
import com.example.composeapp.core.project.ProjectRepository
import com.example.composeapp.core.timeline.TimelineEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditorController(
    private val repository: ProjectRepository,
) {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    suspend fun open(projectId: String): Boolean {
        val project = repository.getProject(projectId) ?: return false
        undoStack.clear()
        redoStack.clear()
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

    fun splitSelected(atTimelineMs: Long): Boolean {
        val state = _state.value
        val project = state.project ?: return false
        val clipId = state.selectedClipId ?: return false
        val trackId = project.tracks.firstOrNull { track -> track.clips.any { it.clipId == clipId } }?.trackId ?: return false
        return applyEdit(TimelineEditor.split(project, trackId, clipId, atTimelineMs))
    }

    fun trimSelected(newStartMs: Long, newDurationMs: Long): Boolean {
        val state = _state.value
        val project = state.project ?: return false
        val clipId = state.selectedClipId ?: return false
        val trackId = project.tracks.firstOrNull { track -> track.clips.any { it.clipId == clipId } }?.trackId ?: return false
        return applyEdit(TimelineEditor.trim(project, trackId, clipId, newStartMs, newDurationMs))
    }

    fun deleteSelected(): Boolean {
        val state = _state.value
        val project = state.project ?: return false
        val clipId = state.selectedClipId ?: return false
        val trackId = project.tracks.firstOrNull { track -> track.clips.any { it.clipId == clipId } }?.trackId ?: return false
        val changed = applyEdit(TimelineEditor.delete(project, trackId, clipId))
        if (changed) selectClip(null)
        return changed
    }

    fun reorderSelected(targetIndex: Int): Boolean {
        val state = _state.value
        val project = state.project ?: return false
        val clipId = state.selectedClipId ?: return false
        val trackId = project.tracks.firstOrNull { track -> track.clips.any { it.clipId == clipId } }?.trackId ?: return false
        return applyEdit(TimelineEditor.reorder(project, trackId, clipId, targetIndex))
    }

    fun undo(): Boolean {
        val current = _state.value.project ?: return false
        val previous = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(current)
        updateProject(previous)
        return true
    }

    fun redo(): Boolean {
        val current = _state.value.project ?: return false
        val next = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(current)
        updateProject(next)
        return true
    }

    suspend fun save(): Boolean {
        val project = _state.value.project ?: return false
        repository.saveProject(project)
        _state.update { it.copy(isDirty = false) }
        return true
    }

    fun replaceProject(project: Project) {
        val current = _state.value.project
        if (current == project) return
        if (current != null) undoStack.addLast(current)
        redoStack.clear()
        updateProject(project)
    }

    private fun applyEdit(updated: Project): Boolean {
        if (updated == _state.value.project) return false
        replaceProject(updated)
        return true
    }

    private fun updateProject(project: Project) {
        val duration = project.durationMs
        _state.update {
            it.copy(
                project = project,
                playheadMs = it.playheadMs.coerceIn(0L, duration),
                isDirty = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
    }
}
