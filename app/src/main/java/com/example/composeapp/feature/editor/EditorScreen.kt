package com.example.composeapp.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.composeapp.core.model.Clip

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
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(projectId) { viewModel.open(projectId) }

    val selectedClip = editorState.project?.tracks?.asSequence()?.flatMap { it.clips.asSequence() }
        ?.firstOrNull { it.clipId == editorState.selectedClipId }
    LaunchedEffect(selectedClip?.clipId) {
        selectedClip?.let {
            val duration = editorState.project?.durationMs?.coerceAtLeast(1L) ?: it.durationMs
            trimStart = it.startMs.toFloat() / duration
            trimEnd = (it.startMs + it.durationMs).toFloat() / duration
        }
    }

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
        if (player != null && lastPlayerPosition != editorState.playheadMs) {
            player.seekTo(editorState.playheadMs)
            lastPlayerPosition = editorState.playheadMs
        }
    }
    LaunchedEffect(editorState.isPlaying, player) { player?.playWhenReady = editorState.isPlaying }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            EditorHeader(
                title = editorState.project?.name ?: "Untitled project",
                dirty = editorState.isDirty,
                canUndo = editorState.canUndo,
                canRedo = editorState.canRedo,
                onBack = onBack,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onSave = viewModel::save,
            )

            Column(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PreviewPanel(
                    player = player,
                    projectDurationMs = editorState.project?.durationMs ?: 0L,
                    playheadMs = editorState.playheadMs,
                    isPlaying = editorState.isPlaying,
                    onPlayPause = { viewModel.setPlaying(!editorState.isPlaying) },
                )

                editorState.project?.let { project ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(formatTime(editorState.playheadMs), fontWeight = FontWeight.Bold)
                        Slider(
                            value = editorState.playheadMs.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..project.durationMs.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatTime(project.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    TimelinePanel(
                        project = project,
                        selectedClipId = editorState.selectedClipId,
                        playheadMs = editorState.playheadMs,
                        onSelect = viewModel::selectClip,
                    )

                    selectedClip?.let { clip ->
                        TrimPanel(
                            clip = clip,
                            projectDurationMs = project.durationMs,
                            startFraction = trimStart,
                            endFraction = trimEnd,
                            onStartChange = { trimStart = it.coerceIn(0f, trimEnd - 0.01f) },
                            onEndChange = { trimEnd = it.coerceIn(trimStart + 0.01f, 1f) },
                            onApply = {
                                val total = project.durationMs.coerceAtLeast(1L)
                                val start = (trimStart * total).toLong()
                                val end = (trimEnd * total).toLong()
                                viewModel.trimSelected(start, (end - start).coerceAtLeast(1L))
                            },
                        )
                    }
                }

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp))
                }

                Spacer(Modifier.weight(1f))
                EditorToolbar(
                    hasSelection = selectedClip != null,
                    onAdd = { picker.launch(arrayOf("video/*")) },
                    onSplit = { viewModel.splitSelected(editorState.playheadMs) },
                    onDelete = viewModel::deleteSelected,
                    onMoveLeft = {
                        val index = selectedClipIndex(editorState.project?.tracks.orEmpty(), selectedClip)
                        if (index > 0) viewModel.reorderSelected(index - 1)
                    },
                    onMoveRight = {
                        val index = selectedClipIndex(editorState.project?.tracks.orEmpty(), selectedClip)
                        if (index >= 0) viewModel.reorderSelected(index + 1)
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorHeader(
    title: String,
    dirty: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("‹  Projects") }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (dirty) "Unsaved changes" else "Saved locally", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(enabled = canUndo, onClick = onUndo) { Text("↶") }
        TextButton(enabled = canRedo, onClick = onRedo) { Text("↷") }
        Button(onClick = onSave) { Text("Save") }
    }
}

@Composable
private fun PreviewPanel(
    player: ExoPlayer?,
    projectDurationMs: Long,
    playheadMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (player != null) {
            AndroidView(
                factory = { PlayerView(it).apply { useController = false; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) } },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("YOUR VIDEO", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Import a clip to start editing", color = Color(0xFF9E9E9E))
            }
        }
        TextButton(
            onClick = onPlayPause,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        ) {
            Text(if (isPlaying) "❚❚" else "▶", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            "${formatTime(playheadMs)} / ${formatTime(projectDurationMs)}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        )
    }
}

@Composable
private fun TimelinePanel(
    project: com.example.composeapp.core.model.Project,
    selectedClipId: String?,
    playheadMs: Long,
    onSelect: (String?) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TIMELINE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${project.tracks.sumOf { it.clips.size }} clips", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                project.tracks.forEach { track ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.width(52.dp), contentAlignment = Alignment.CenterStart) {
                            Text(track.type.name.take(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        track.clips.forEach { clip ->
                            val selected = clip.clipId == selectedClipId
                            val width = (clip.durationMs / 28L).coerceIn(88L, 260L).toInt()
                            Card(
                                onClick = { onSelect(clip.clipId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                modifier = Modifier.width(width.dp).height(62.dp).border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                ),
                            ) {
                                Column(Modifier.padding(9.dp)) {
                                    Text(if (selected) "SELECTED" else "CLIP", color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text(formatTime(clip.durationMs), color = if (selected) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            if (project.durationMs > 0) {
                Text("Playhead  ${formatTime(playheadMs)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrimPanel(
    clip: Clip,
    projectDurationMs: Long,
    startFraction: Float,
    endFraction: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    onApply: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TRIM", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${formatTime((startFraction * projectDurationMs).toLong())} — ${formatTime((endFraction * projectDurationMs).toLong())}", style = MaterialTheme.typography.labelSmall)
            }
            Slider(value = startFraction, onValueChange = onStartChange, valueRange = 0f..(endFraction - 0.01f).coerceAtLeast(0.01f))
            Slider(value = endFraction, onValueChange = onEndChange, valueRange = (startFraction + 0.01f).coerceAtMost(0.99f)..1f)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onApply) { Text("Apply trim") }
            }
        }
    }
}

@Composable
private fun EditorToolbar(
    hasSelection: Boolean,
    onAdd: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton("＋", "Media", onAdd)
            ToolButton("✂", "Split", onSplit, !hasSelection)
            ToolButton("‹", "Move", onMoveLeft, !hasSelection)
            ToolButton("›", "Move", onMoveRight, !hasSelection)
            ToolButton("⌫", "Delete", onDelete, !hasSelection)
        }
    }
}

@Composable
private fun ToolButton(label: String, caption: String, onClick: () -> Unit, disabled: Boolean = false) {
    TextButton(enabled = !disabled, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun selectedClipIndex(
    tracks: List<com.example.composeapp.core.model.Track>,
    selectedClip: Clip?,
): Int {
    if (selectedClip == null) return -1
    return tracks.firstOrNull { track -> track.clips.any { it.clipId == selectedClip.clipId } }
        ?.clips?.indexOfFirst { it.clipId == selectedClip.clipId } ?: -1
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
