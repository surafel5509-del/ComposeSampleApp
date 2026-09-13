package com.example.composeapp.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.EditorTool
import com.example.composeapp.ui.theme.StudioBackground
import com.example.composeapp.ui.theme.StudioBorder
import com.example.composeapp.ui.theme.StudioBorderSubtle
import com.example.composeapp.ui.theme.StudioCyan
import com.example.composeapp.ui.theme.StudioPink
import com.example.composeapp.ui.theme.StudioSurface
import com.example.composeapp.ui.theme.StudioSurfaceVariant

@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val editor by viewModel.editorState.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importVideo)
    }

    var lastPosition by remember { mutableLongStateOf(-1L) }

    val selectedClip = remember(editor.project, editor.selectedClipId) {
        editor.project?.tracks?.asSequence()?.flatMap { it.clips.asSequence() }?.firstOrNull { it.clipId == editor.selectedClipId }
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

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    LaunchedEffect(projectId) {
        viewModel.open(projectId)
    }

    LaunchedEffect(editor.playheadMs, player) {
        if (player != null && lastPosition != editor.playheadMs) {
            player.seekTo(editor.playheadMs)
            lastPosition = editor.playheadMs
        }
    }

    LaunchedEffect(editor.isPlaying, player) {
        player?.playWhenReady = editor.isPlaying
    }

    Surface(
        color = StudioBackground,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Studio TopBar (Clean CapCut navigation & state)
            StudioTopBar(
                title = editor.project?.name ?: "MoreCut Studio",
                isDirty = editor.isDirty,
                canUndo = editor.canUndo,
                canRedo = editor.canRedo,
                isExporting = state.isExporting,
                onBack = onBack,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onSave = viewModel::save,
                onExport = viewModel::exportSelected,
            )

            // 2. Middle Editor Workspace (Preview + Timeline + Context Bar)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Video Preview Monitor
                VideoPreviewMonitor(
                    player = player,
                    playheadMs = editor.playheadMs,
                    durationMs = editor.project?.durationMs ?: 0L,
                    isPlaying = editor.isPlaying,
                    onTogglePlay = { viewModel.setPlaying(!editor.isPlaying) },
                    onImportMedia = { picker.launch(arrayOf("video/*", "image/*")) },
                )

                // High Precision Scrubber Bar
                editor.project?.let { project ->
                    PlayheadScrubberBar(
                        playheadMs = editor.playheadMs,
                        durationMs = project.durationMs,
                        onSeek = viewModel::seekTo,
                    )

                    // Interactive Drag & Drop Timeline
                    TimelineView(
                        tracks = project.tracks,
                        selectedClipId = editor.selectedClipId,
                        playheadMs = editor.playheadMs,
                        totalDurationMs = project.durationMs,
                        onSelectClip = viewModel::selectClip,
                        onSeekTo = viewModel::seekTo,
                        onReorderClip = viewModel::reorderClip,
                        onTrimClip = viewModel::trimClip,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }

                // Contextual Action Strip
                ContextualActionStrip(
                    selectedClip = selectedClip,
                    assetName = state.assetName,
                    onSplit = viewModel::splitSelectedAtPlayhead,
                    onDuplicate = viewModel::duplicateSelected,
                    onDelete = viewModel::deleteSelected,
                    onAddMedia = { picker.launch(arrayOf("video/*", "image/*")) },
                    onDeselect = { viewModel.selectClip(null) },
                )

                // Status notifications (Error / Export)
                state.error?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                state.exportUri?.let { exportUri ->
                    Text(
                        text = "✓ Export rendered: $exportUri",
                        color = Color(0xFF00E676),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            // 3. Fixed Bottom Toolbar (Arranged in clean horizontal row at bottom of screen, exactly like CapCut)
            ProfessionalToolTray(
                hasSelection = selectedClip != null,
                isExporting = state.isExporting,
                onTool = viewModel::applyTool,
                onDuplicate = viewModel::duplicateSelected,
                onExport = viewModel::exportSelected,
                onSplitAtPlayhead = viewModel::splitSelectedAtPlayhead,
                onDeleteSelected = viewModel::deleteSelected,
            )
        }
    }

    // Export progress overlay dialog
    if (state.isExporting) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = Color(0xFF161924),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = StudioCyan, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text("Rendering Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text("Exporting high-definition timeline using FFmpeg media pipeline...", color = Color(0xFFB0B7C9), style = MaterialTheme.typography.bodyMedium)
            }
        )
    }
}

@Composable
private fun StudioTopBar(
    title: String,
    isDirty: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isExporting: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1015))
            .border(1.dp, StudioBorderSubtle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF181B24))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
        }

        Spacer(Modifier.width(10.dp))

        // Title and Project Status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isDirty) StudioPink else Color(0xFF00E676))
                )
                Text(
                    text = if (isDirty) "Unsaved edits" else "Synced offline",
                    color = if (isDirty) Color(0xFFFFA4B8) else Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                )
            }
        }

        // Undo & Redo Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (canUndo) Color(0xFF1B1E29) else Color(0xFF12141C))
                    .clickable(enabled = canUndo, onClick = onUndo),
                contentAlignment = Alignment.Center,
            ) {
                Text("↶", color = if (canUndo) Color.White else Color(0xFF4A5164), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (canRedo) Color(0xFF1B1E29) else Color(0xFF12141C))
                    .clickable(enabled = canRedo, onClick = onRedo),
                contentAlignment = Alignment.Center,
            ) {
                Text("↷", color = if (canRedo) Color.White else Color(0xFF4A5164), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Save Button
        TextButton(
            onClick = onSave,
            modifier = Modifier.height(34.dp),
        ) {
            Text("Save", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelMedium)
        }

        // Vibrant Export Button (CapCut signature style)
        Button(
            onClick = onExport,
            enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioCyan,
                contentColor = Color(0xFF090A0E),
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .height(34.dp)
                .shadow(4.dp, RoundedCornerShape(10.dp)),
        ) {
            Text("Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun VideoPreviewMonitor(
    player: ExoPlayer?,
    playheadMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onImportMedia: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050508)),
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (player != null) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { useController = false } },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onImportMedia)
                        .padding(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171A24)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶", color = StudioCyan, fontSize = 20.sp)
                    }
                    Text("MORECUT STUDIO", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, letterSpacing = 1.5.sp)
                    Text("Tap '+ Media' to import video clip", color = Color(0xFF7A8296), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Play / Pause Floating Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC11131B))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .clickable(onClick = onTogglePlay)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isPlaying) "❚❚ Pause" else "▶ Play",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }

            // Duration badge at bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${formatDurationClock(playheadMs)} / ${formatDurationClock(durationMs)}",
                    color = Color(0xFFE2E8F0),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun PlayheadScrubberBar(
    playheadMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatDurationClock(playheadMs),
            color = StudioCyan,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
        )

        Slider(
            value = playheadMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = StudioCyan,
                activeTrackColor = StudioCyan,
                inactiveTrackColor = Color(0xFF222635),
            ),
            modifier = Modifier.weight(1f),
        )

        Text(
            text = formatDurationClock(durationMs),
            color = Color(0xFF888E9E),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ContextualActionStrip(
    selectedClip: Clip?,
    assetName: String?,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddMedia: () -> Unit,
    onDeselect: () -> Unit,
) {
    Surface(
        color = Color(0xFF12141C),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Selection Status or Clip Indicator
            if (selectedClip != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2435))
                            .clickable(onClick = onDeselect),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = Color.LightGray, fontSize = 9.sp)
                    }
                    Column {
                        Text(
                            text = assetName ?: "Clip Selected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatDurationClock(selectedClip.durationMs)} duration",
                            color = StudioCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                        )
                    }
                }
            } else {
                Text(
                    text = "Tap a clip to trim, drag, or split",
                    color = Color(0xFF858C9F),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Right: Quick Action Buttons (Split, Duplicate, Delete, + Media)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (selectedClip != null) {
                    // Split at playhead
                    QuickActionButton(label = "Split", icon = "✂", color = StudioCyan, onClick = onSplit)
                    // Duplicate
                    QuickActionButton(label = "Copy", icon = "⧉", color = Color.White, onClick = onDuplicate)
                    // Delete
                    QuickActionButton(label = "Del", icon = "🗑", color = StudioPink, onClick = onDelete)
                }

                // Add Media Button (+ Media)
                Button(
                    onClick = onAddMedia,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedClip == null) StudioCyan else Color(0xFF1D2230),
                        contentColor = if (selectedClip == null) Color(0xFF090A0E) else Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("+ Media", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B1E2A))
            .border(1.dp, StudioBorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(icon, color = color, fontSize = 11.sp)
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatDurationClock(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "%02d:%02d".format(minutes, seconds)
}
