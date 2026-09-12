package com.example.composeapp.feature.editor

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeapp.core.database.DatabaseFactory
import com.example.composeapp.core.database.AssetEntity
import com.example.composeapp.core.editor.EditorController
import com.example.composeapp.core.model.AssetKind
import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.model.Track
import com.example.composeapp.core.model.TrackType
import com.example.composeapp.core.project.OfflineProjectRepository
import com.example.composeapp.core.storage.ProjectFileStore
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseFactory.create(application)
    private val repository = OfflineProjectRepository(
        projectDao = database.projectDao(),
        fileStore = ProjectFileStore(application),
    )
    private val controller = EditorController(repository)
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    fun open(projectId: String) {
        if (_state.value.projectId == projectId && _state.value.opened) return
        viewModelScope.launch {
            _state.value = _state.value.copy(projectId = projectId, isLoading = true, error = null)
            val opened = controller.open(projectId)
            _state.value = _state.value.copy(
                opened = opened,
                isLoading = false,
                error = if (opened) null else "Project could not be opened.",
            )
        }
    }

    fun importVideo(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Some providers do not expose persistable permissions; the import still works now.
            }

            val project = controller.state.value.project ?: return@launch
            val durationMs = readDurationMs(uri)
            if (durationMs <= 0L) {
                _state.value = _state.value.copy(error = "The selected video has no readable duration.")
                return@launch
            }

            val assetId = UUID.randomUUID().toString()
            val assetName = queryDisplayName(uri)
            database.assetDao().upsert(
                AssetEntity(
                    assetId = assetId,
                    kind = AssetKind.VIDEO.name,
                    originalUri = uri.toString(),
                    localPath = null,
                    proxyPath = null,
                    checksum = null,
                    version = 1,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )

            val videoTrack = project.tracks.firstOrNull { it.type == TrackType.VIDEO }
                ?: Track(trackId = UUID.randomUUID().toString(), type = TrackType.VIDEO)
            val startMs = videoTrack.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val clip = Clip(
                clipId = UUID.randomUUID().toString(),
                assetId = assetId,
                startMs = startMs,
                durationMs = durationMs,
            )
            val updatedTrack = videoTrack.copy(clips = videoTrack.clips + clip)
            val updatedProject = project.copy(
                revision = project.revision + 1,
                durationMs = maxOf(project.durationMs, startMs + durationMs),
                tracks = if (project.tracks.any { it.trackId == videoTrack.trackId }) {
                    project.tracks.map { if (it.trackId == videoTrack.trackId) updatedTrack else it }
                } else {
                    project.tracks + updatedTrack
                },
            )
            controller.replaceProject(updatedProject)
            controller.save()
            _state.value = _state.value.copy(
                assetName = assetName,
                error = null,
            )
        }
    }

    private fun readDurationMs(uri: Uri): Long =
        android.media.MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(getApplication(), uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        }

    private fun queryDisplayName(uri: Uri): String? {
        getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment
    }

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}

data class EditorUiState(
    val projectId: String? = null,
    val opened: Boolean = false,
    val isLoading: Boolean = false,
    val assetName: String? = null,
    val error: String? = null,
)
