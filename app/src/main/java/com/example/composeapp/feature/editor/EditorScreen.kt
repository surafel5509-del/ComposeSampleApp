package com.example.composeapp.feature.editor

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.composeapp.core.model.TrackType

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
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importVideo) }

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
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editorState.project?.name ?: "Editor") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { picker.launch(arrayOf("video/*")) }) { Text("Import video") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (player != null) {
                AndroidView(
                    factory = { PlayerView(it).apply { useController = true } },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            } else {
                Card(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Preview", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Import a local video to preview it here and add it to the timeline.")
                    }
                }
            }

            editorState.project?.let { project ->
                Text(
                    "${project.canvas.width}×${project.canvas.height} • ${project.canvas.frameRate} fps • ${project.durationMs} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Timeline", style = MaterialTheme.typography.titleLarge)
                Column(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    project.tracks.forEach { track ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                track.type.name,
                                modifier = Modifier.width(76.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            track.clips.forEach { clip ->
                                Card(
                                    modifier = Modifier
                                        .width((clip.durationMs / 30L).coerceIn(96L, 360L).dp)
                                        .height(64.dp),
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Clip", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "${clip.startMs}–${clip.startMs + clip.durationMs} ms",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (project.tracks.none { it.type == TrackType.VIDEO && it.clips.isNotEmpty() }) {
                        Text("No video clips yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.assetName?.let { Text("Imported: $it", style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("video/*")) }) { Text("Add video") }
                Text(
                    if (editorState.isDirty) "Unsaved changes" else "Saved locally",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
