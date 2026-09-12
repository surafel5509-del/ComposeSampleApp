package com.example.composeapp.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importVideo) }
    var lastPlayerPosition by remember { mutableLongStateOf(0L) }

    LaunchedEffect(projectId) { viewModel.open(projectId) }

    val previewUri = state.previewUri?.let(Uri::parse)
    val player = remember(previewUri) {
        previewUri?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it))
                prepare()
            }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }

    LaunchedEffect(editorState.playheadMs, player) {
        player?.seekTo(editorState.playheadMs)
        lastPlayerPosition = editorState.playheadMs
    }
    LaunchedEffect(editorState.isPlaying, player) { player?.playWhenReady = editorState.isPlaying }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editorState.project?.name ?: "Editor") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(enabled = editorState.canUndo, onClick = { viewModel.undo() }) { Text("Undo") }
                    TextButton(enabled = editorState.canRedo, onClick = { viewModel.redo() }) { Text("Redo") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (player != null) {
                AndroidView(
                    factory = { PlayerView(it).apply { useController = true } },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            } else {
                Card(Modifier.fillMaxWidth().height(220.dp)) {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                        Text("Preview", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Import a local video to begin editing.")
                    }
                }
            }

            editorState.project?.let { project ->
                Text("${project.canvas.width}×${project.canvas.height} • ${project.canvas.frameRate} fps • ${project.durationMs} ms")
                if (project.durationMs > 0) {
                    Slider(
                        value = editorState.playheadMs.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..project.durationMs.toFloat(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.setPlaying(!editorState.isPlaying) }) {
                        Text(if (editorState.isPlaying) "Pause" else "Play")
                    }
                    OutlinedButton(onClick = { viewModel.splitSelected(editorState.playheadMs) }, enabled = editorState.selectedClipId != null) { Text("Split") }
                    OutlinedButton(onClick = { viewModel.deleteSelected() }, enabled = editorState.selectedClipId != null) { Text("Delete") }
                }

                Text("Timeline", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    project.tracks.forEach { track ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(track.type.name, Modifier.width(76.dp), style = MaterialTheme.typography.labelMedium)
                            track.clips.forEach { clip ->
                                Card(onClick = { viewModel.selectClip(clip.clipId) }, Modifier.width((clip.durationMs / 30L).coerceIn(96L, 360L).toInt().dp).height(64.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(if (clip.clipId == editorState.selectedClipId) "Selected clip" else "Clip")
                                        Text("${clip.startMs}–${clip.startMs + clip.durationMs} ms", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.assetName?.let { Text("Imported: $it", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("video/*")) }) { Text("Add video") }
                Text(if (editorState.isDirty) "Unsaved changes" else "Saved locally")
            }
        }
    }
}
