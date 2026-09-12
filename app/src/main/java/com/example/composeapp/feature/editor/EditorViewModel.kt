package com.example.composeapp.feature.editor

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeapp.core.database.AssetEntity
import com.example.composeapp.core.database.DatabaseFactory
import com.example.composeapp.core.editor.EditorController
import com.example.composeapp.core.export.VideoExportEngine
import com.example.composeapp.core.model.AssetKind
import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.EditorTool
import com.example.composeapp.core.model.Keyframe
import com.example.composeapp.core.model.Track
import com.example.composeapp.core.model.TrackType
import com.example.composeapp.core.project.OfflineProjectRepository
import com.example.composeapp.core.storage.ProjectFileStore
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseFactory.create(application)
    private val repository = OfflineProjectRepository(
        projectDao = database.projectDao(),
        fileStore = ProjectFileStore(application),
    )
    private val controller = EditorController(repository)
    private val exportEngine = VideoExportEngine(application)
    val editorState: StateFlow<com.example.composeapp.core.editor.EditorState> = controller.state

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    fun open(projectId: String) {
        if (_state.value.projectId == projectId && _state.value.opened) return
        viewModelScope.launch {
            _state.value = _state.value.copy(projectId = projectId, isLoading = true, error = null)
            val opened = controller.open(projectId)
            _state.value = _state.value.copy(opened = opened, isLoading = false, error = if (opened) null else "Project could not be opened.")
        }
    }

    fun undo() = controller.undo()
    fun redo() = controller.redo()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun setPlaying(playing: Boolean) = controller.setPlaying(playing)
    fun splitSelected(atTimelineMs: Long) = controller.splitSelected(atTimelineMs)
    fun trimSelected(startMs: Long, durationMs: Long) = controller.trimSelected(startMs, durationMs)
    fun reorderSelected(targetIndex: Int) = controller.reorderSelected(targetIndex)
    fun deleteSelected() = controller.deleteSelected()
    fun selectClip(clipId: String?) = controller.selectClip(clipId)

    fun applyTool(tool: EditorTool) {
        val project = editorState.value.project ?: return
        val clipId = editorState.value.selectedClipId ?: return
        val selected = project.tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.clipId == clipId } ?: return
        val updatedEdit = when (tool) {
            EditorTool.EFFECTS -> selected.edit.copy(effect = if (selected.edit.effect == "None") "Glow" else "None")
            EditorTool.FILTERS -> selected.edit.copy(filter = when (selected.edit.filter) { "Original" -> "Cinematic"; "Cinematic" -> "Mono"; "Mono" -> "Vintage"; else -> "Original" })
            EditorTool.FREEZE -> selected.edit.copy(freezeAtMs = editorState.value.playheadMs, freezeDurationMs = 1000L)
            EditorTool.REVERB -> selected.edit.copy(reverb = if (selected.edit.reverb > 0f) 0f else 0.35f)
            EditorTool.CROP -> selected.edit.copy(cropLeft = 0.05f, cropTop = 0.05f, cropRight = 0.95f, cropBottom = 0.95f)
            EditorTool.ROTATE -> selected.edit.copy(rotation = (selected.edit.rotation + 90) % 360)
            EditorTool.FLIP -> selected.edit.copy(flipHorizontal = !selected.edit.flipHorizontal)
            EditorTool.VOLUME -> selected.edit.copy(volume = if (selected.edit.volume > 0.5f) 0.5f else 1f)
            EditorTool.ADJUSTMENT -> selected.edit.copy(brightness = 0.08f, contrast = 1.08f, saturation = 1.1f)
            EditorTool.FADE -> selected.edit.copy(fadeInMs = 250L, fadeOutMs = 250L)
            EditorTool.KEYFRAME -> selected.edit.copy(keyframes = selected.edit.keyframes + Keyframe(editorState.value.playheadMs))
            EditorTool.TEXT -> selected.edit.copy(text = selected.edit.text?.let { null } ?: "MoreCut")
            else -> selected.edit
        }
        if (updatedEdit == selected.edit) return
        val updated = selected.copy(edit = updatedEdit)
        val next = project.copy(
            revision = project.revision + 1,
            tracks = project.tracks.map { track -> track.copy(clips = track.clips.map { if (it.clipId == clipId) updated else it }) },
        )
        controller.replaceProject(next)
    }

    fun duplicateSelected() {
        val project = editorState.value.project ?: return
        val clipId = editorState.value.selectedClipId ?: return
        val sourceTrack = project.tracks.firstOrNull { it.clips.any { clip -> clip.clipId == clipId } } ?: return
        val source = sourceTrack.clips.first { it.clipId == clipId }
        val duplicate = source.copy(clipId = UUID.randomUUID().toString(), startMs = source.startMs + source.durationMs)
        val updatedTrack = sourceTrack.copy(clips = sourceTrack.clips + duplicate)
        controller.replaceProject(project.copy(
            revision = project.revision + 1,
            durationMs = maxOf(project.durationMs, duplicate.startMs + duplicate.durationMs),
            tracks = project.tracks.map { if (it.trackId == sourceTrack.trackId) updatedTrack else it },
        ))
        controller.selectClip(duplicate.clipId)
    }

    fun exportSelected() {
        viewModelScope.launch {
            val project = editorState.value.project ?: return@launch
            val clipId = editorState.value.selectedClipId ?: project.tracks.firstOrNull { it.type == TrackType.VIDEO }?.clips?.firstOrNull()?.clipId ?: return@launch
            val clip = project.tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.clipId == clipId } ?: return@launch
            val asset = database.assetDao().findById(clip.assetId)
            val uri = asset?.originalUri?.let(Uri::parse) ?: return@launch
            _state.value = _state.value.copy(isExporting = true, exportUri = null, error = null)
            val result = exportEngine.exportClip(uri, clip.sourceStartMs, clip.durationMs, clip.edit)
            _state.value = _state.value.copy(isExporting = false, exportUri = result.getOrNull()?.toString(), error = result.exceptionOrNull()?.message)
        }
    }

    fun save() {
        viewModelScope.launch {
            if (!controller.save()) _state.value = _state.value.copy(error = "Nothing to save yet.")
        }
    }

    fun importVideo(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            val project = controller.state.value.project ?: return@launch
            val result = withContext(Dispatchers.IO) { runCatching { readDurationMs(uri) } }
            val durationMs = result.getOrElse { _state.value = _state.value.copy(error = "The selected video could not be read."); return@launch }
            if (durationMs <= 0L) { _state.value = _state.value.copy(error = "The selected video has no readable duration."); return@launch }
            val assetId = UUID.randomUUID().toString()
            val assetName = withContext(Dispatchers.IO) { queryDisplayName(uri) }
            database.assetDao().upsert(AssetEntity(assetId, AssetKind.VIDEO.name, uri.toString(), null, null, null, 1, System.currentTimeMillis()))
            val videoTrack = project.tracks.firstOrNull { it.type == TrackType.VIDEO } ?: Track(UUID.randomUUID().toString(), TrackType.VIDEO)
            val startMs = videoTrack.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val clip = Clip(UUID.randomUUID().toString(), assetId, startMs, durationMs)
            val updatedTrack = videoTrack.copy(clips = videoTrack.clips + clip)
            controller.replaceProject(project.copy(revision = project.revision + 1, durationMs = maxOf(project.durationMs, startMs + durationMs), tracks = if (project.tracks.any { it.trackId == videoTrack.trackId }) project.tracks.map { if (it.trackId == videoTrack.trackId) updatedTrack else it } else project.tracks + updatedTrack))
            controller.save()
            controller.selectClip(clip.clipId)
            _state.value = _state.value.copy(assetName = assetName, previewUri = uri.toString(), error = null)
        }
    }

    private fun readDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try { retriever.setDataSource(getApplication(), uri); retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L } finally { retriever.release() }
    }

    private fun queryDisplayName(uri: Uri): String? {
        getApplication<Application>().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) return cursor.getString(0) }
        return uri.lastPathSegment
    }

    override fun onCleared() { database.close(); super.onCleared() }
}

data class EditorUiState(
    val projectId: String? = null,
    val opened: Boolean = false,
    val isLoading: Boolean = false,
    val assetName: String? = null,
    val previewUri: String? = null,
    val isExporting: Boolean = false,
    val exportUri: String? = null,
    val error: String? = null,
)
