package com.example.composeapp.feature.editor

import android.app.Application
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
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
import com.example.composeapp.core.timeline.TimelineEditor
import java.io.File
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
            var previewUri: String? = null
            var assetName: String? = null
            if (opened) {
                val proj = controller.state.value.project
                val firstClip = proj?.tracks?.firstOrNull { it.type == TrackType.VIDEO }?.clips?.firstOrNull()
                    ?: proj?.tracks?.asSequence()?.flatMap { it.clips.asSequence() }?.firstOrNull()
                if (firstClip != null) {
                    val asset = withContext(Dispatchers.IO) { database.assetDao().findById(firstClip.assetId) }
                    previewUri = asset?.localPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> Uri.fromFile(f).toString() } }
                        ?: asset?.originalUri
                    assetName = previewUri?.let { withContext(Dispatchers.IO) { queryDisplayName(Uri.parse(it)) } }
                }
            }
            _state.value = _state.value.copy(
                opened = opened,
                isLoading = false,
                previewUri = previewUri,
                assetName = assetName,
                error = if (opened) null else "Project could not be opened."
            )
        }
    }

    fun undo() = controller.undo()
    fun redo() = controller.redo()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun setPlaying(playing: Boolean) = controller.setPlaying(playing)
    fun splitSelected(atTimelineMs: Long) = controller.splitSelected(atTimelineMs)
    fun splitSelectedAtPlayhead(): Boolean = controller.splitSelected(editorState.value.playheadMs)
    fun trimSelected(startMs: Long, durationMs: Long) = controller.trimSelected(startMs, durationMs)
    fun trimClip(trackId: String, clipId: String, newStartMs: Long, newDurationMs: Long): Boolean {
        val project = editorState.value.project ?: return false
        val updated = TimelineEditor.trim(project, trackId, clipId, newStartMs, newDurationMs)
        if (updated == project) return false
        controller.replaceProject(updated)
        return true
    }
    fun reorderSelected(targetIndex: Int) = controller.reorderSelected(targetIndex)
    fun reorderClip(trackId: String, clipId: String, targetIndex: Int): Boolean {
        val project = editorState.value.project ?: return false
        val updated = TimelineEditor.reorder(project, trackId, clipId, targetIndex)
        if (updated == project) return false
        controller.replaceProject(updated)
        return true
    }
    fun deleteSelected() = controller.deleteSelected()
    fun selectClip(clipId: String?) {
        controller.selectClip(clipId)
        if (clipId != null) {
            viewModelScope.launch {
                val project = editorState.value.project ?: return@launch
                val clip = project.tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.clipId == clipId } ?: return@launch
                val asset = withContext(Dispatchers.IO) { database.assetDao().findById(clip.assetId) }
                val uri = asset?.localPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> Uri.fromFile(f).toString() } }
                    ?: asset?.originalUri
                if (uri != null) {
                    val name = withContext(Dispatchers.IO) { queryDisplayName(Uri.parse(uri)) } ?: "Clip"
                    _state.value = _state.value.copy(previewUri = uri, assetName = name)
                }
            }
        }
    }

    fun applyTool(tool: EditorTool) {
        if (tool == EditorTool.CUT) {
            splitSelectedAtPlayhead()
            return
        }
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
            val uri = asset?.localPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> Uri.fromFile(f) } }
                ?: asset?.originalUri?.let(Uri::parse)
                ?: return@launch
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
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            val project = controller.state.value.project ?: return@launch
            _state.value = _state.value.copy(isLoading = true, error = null)

            val assetId = UUID.randomUUID().toString()
            val localFile = copyUriToLocalAsset(uri, project.projectId, assetId)
            val durationMs = resolveMediaDuration(uri, localFile)
            val assetName = withContext(Dispatchers.IO) { queryDisplayName(uri) } ?: "Media Clip"

            val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            val isAudio = mimeType?.startsWith("audio/") == true
            val isImage = mimeType?.startsWith("image/") == true
            val assetKind = when {
                isAudio -> AssetKind.AUDIO.name
                isImage -> AssetKind.IMAGE.name
                else -> AssetKind.VIDEO.name
            }

            withContext(Dispatchers.IO) {
                database.assetDao().upsert(
                    AssetEntity(
                        assetId = assetId,
                        kind = assetKind,
                        originalUri = uri.toString(),
                        localPath = localFile?.absolutePath,
                        proxyPath = null,
                        checksum = null,
                        version = 1,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                )
            }

            val targetTrackType = if (isAudio) TrackType.AUDIO else TrackType.VIDEO
            val track = project.tracks.firstOrNull { it.type == targetTrackType }
                ?: Track(UUID.randomUUID().toString(), targetTrackType)
            val startMs = track.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val clip = Clip(
                clipId = UUID.randomUUID().toString(),
                assetId = assetId,
                startMs = startMs,
                durationMs = durationMs,
            )
            val updatedTrack = track.copy(clips = track.clips + clip)
            val nextTracks = if (project.tracks.any { it.trackId == track.trackId }) {
                project.tracks.map { if (it.trackId == track.trackId) updatedTrack else it }
            } else {
                project.tracks + updatedTrack
            }

            val nextProject = project.copy(
                revision = project.revision + 1,
                durationMs = maxOf(project.durationMs, startMs + durationMs),
                tracks = nextTracks,
            )
            controller.replaceProject(nextProject)
            controller.save()
            controller.selectClip(clip.clipId)

            val resolvedPreview = localFile?.let { Uri.fromFile(it).toString() } ?: uri.toString()
            _state.value = _state.value.copy(
                isLoading = false,
                assetName = assetName,
                previewUri = resolvedPreview,
                error = null,
            )
        }
    }

    private suspend fun copyUriToLocalAsset(uri: Uri, projectId: String, assetId: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val context = getApplication<Application>()
            val assetDir = File(context.filesDir, "projects/$projectId/assets").apply { mkdirs() }
            val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            val ext = when {
                mimeType?.startsWith("image/") == true -> "jpg"
                mimeType?.startsWith("audio/") == true -> "m4a"
                else -> "mp4"
            }
            val targetFile = File(assetDir, "asset_${assetId}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0L) targetFile else null
        }.getOrNull()
    }

    private suspend fun resolveMediaDuration(uri: Uri, localFile: File?): Long = withContext(Dispatchers.IO) {
        var duration = 0L

        // Strategy 1: MediaMetadataRetriever on local cached file
        if (localFile != null && localFile.exists()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(localFile.absolutePath)
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (_: Throwable) {
            } finally {
                runCatching { retriever.release() }
            }
        }

        // Strategy 2: MediaExtractor on local file
        if (duration <= 0L && localFile != null && localFile.exists()) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(localFile.absolutePath)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        val durUs = format.getLong(MediaFormat.KEY_DURATION)
                        if (durUs > 0L) {
                            duration = durUs / 1000L
                            break
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { extractor.release() }
            }
        }

        // Strategy 3: MediaMetadataRetriever directly on URI
        if (duration <= 0L) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(getApplication(), uri)
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (_: Throwable) {
            } finally {
                runCatching { retriever.release() }
            }
        }

        // Strategy 4: Query MediaStore
        if (duration <= 0L) {
            try {
                getApplication<Application>().contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Video.Media.DURATION),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dur = cursor.getLong(0)
                        if (dur > 0L) duration = dur
                    }
                }
            } catch (_: Throwable) {
            }
        }

        // Strategy 5: Safe fallback duration (5000ms default) for photos or media without duration header
        if (duration <= 0L) {
            duration = 5000L
        }

        duration
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
