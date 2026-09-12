package com.example.composeapp.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.composeapp.core.model.EditorTool

@Composable
fun EditorScreen(projectId: String, onBack: () -> Unit, viewModel: EditorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val editor by viewModel.editorState.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importVideo) }
    var lastPosition by remember { mutableLongStateOf(-1L) }
    val selected = editor.project?.tracks?.asSequence()?.flatMap { it.clips.asSequence() }?.firstOrNull { it.clipId == editor.selectedClipId }
    val previewUri = state.previewUri?.let(Uri::parse)
    val player = remember(previewUri) { previewUri?.let { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(it)); prepare() } } }
    DisposableEffect(player) { onDispose { player?.release() } }
    LaunchedEffect(projectId) { viewModel.open(projectId) }
    LaunchedEffect(editor.playheadMs, player) { if (player != null && lastPosition != editor.playheadMs) { player.seekTo(editor.playheadMs); lastPosition = editor.playheadMs } }
    LaunchedEffect(editor.isPlaying, player) { player?.playWhenReady = editor.isPlaying }

    Surface(color = Color(0xFF050506), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TopBar(editor.project?.name ?: "MoreCut Studio", editor.isDirty, editor.canUndo, editor.canRedo, onBack, viewModel::undo, viewModel::redo, viewModel::save)
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Preview(player, editor.playheadMs, editor.project?.durationMs ?: 0L, editor.isPlaying) { viewModel.setPlaying(!editor.isPlaying) }
                editor.project?.let { project ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(editor.playheadMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Slider(value = editor.playheadMs.toFloat(), onValueChange = { viewModel.seekTo(it.toLong()) }, valueRange = 0f..project.durationMs.coerceAtLeast(1L).toFloat(), modifier = Modifier.weight(1f))
                        Text(formatTime(project.durationMs), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    Timeline(project.tracks, editor.selectedClipId, editor.playheadMs) { viewModel.selectClip(it) }
                }
                ProfessionalToolTray(
                    hasSelection = selected != null,
                    isExporting = state.isExporting,
                    onTool = viewModel::applyTool,
                    onDuplicate = viewModel::duplicateSelected,
                    onExport = viewModel::exportSelected,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (selected == null) "Select a clip to unlock tools" else "Selected: ${state.assetName ?: "clip"}", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    Button(onClick = { picker.launch(arrayOf("video/*")) }) { Text("＋ Media") }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.isExporting) Text("Exporting…", color = Color.White, fontWeight = FontWeight.Bold)
                state.exportUri?.let { Text("Export complete: $it", color = Color(0xFFB8FFCF), style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun TopBar(title: String, dirty: Boolean, canUndo: Boolean, canRedo: Boolean, onBack: () -> Unit, undo: () -> Unit, redo: () -> Unit, save: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0A0A0C)).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (dirty) "Unsaved changes" else "Saved", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        TextButton(enabled = canUndo, onClick = undo) { Text("↶", color = Color.White) }
        TextButton(enabled = canRedo, onClick = redo) { Text("↷", color = Color.White) }
        TextButton(onClick = save) { Text("Save", color = Color.White) }
    }
}

@Composable
private fun Preview(player: ExoPlayer?, position: Long, duration: Long, playing: Boolean, toggle: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(22.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
        if (player != null) AndroidView(factory = { PlayerView(it).apply { useController = false } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
        else Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("MORECUT", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium); Text("Import media to begin", color = Color.Gray) }
        TextButton(onClick = toggle, modifier = Modifier.align(Alignment.BottomCenter)) { Text(if (playing) "❚❚" else "▶", color = Color.White, style = MaterialTheme.typography.titleLarge) }
        Text("${formatTime(position)} / ${formatTime(duration)}", color = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Timeline(tracks: List<com.example.composeapp.core.model.Track>, selectedId: String?, playhead: Long, select: (String?) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D10)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("TIMELINE", color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${tracks.sumOf { it.clips.size }} clips", color = Color.Gray, style = MaterialTheme.typography.labelSmall) }
            HorizontalDivider(color = Color(0xFF25252A))
            Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks.forEach { track ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(track.type.name.take(5), color = Color.Gray, modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall)
                        track.clips.forEach { clip ->
                            val selected = clip.clipId == selectedId
                            val width = (clip.durationMs / 25L).coerceIn(90L, 260L).toInt()
                            Card(onClick = { select(clip.clipId) }, colors = CardDefaults.cardColors(containerColor = if (selected) Color.White else Color(0xFF1B1B20)), shape = RoundedCornerShape(9.dp), modifier = Modifier.width(width.dp).height(58.dp)) {
                                Column(Modifier.padding(8.dp)) { Text(if (selected) "SELECTED" else "CLIP", color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall); Text(formatTime(clip.durationMs), color = if (selected) Color.DarkGray else Color.Gray, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
            Text("Playhead ${formatTime(playhead)}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms.coerceAtLeast(0L) / 1000L; return "%02d:%02d".format(s / 60L, s % 60L) }
