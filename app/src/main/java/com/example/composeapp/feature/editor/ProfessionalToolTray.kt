package com.example.composeapp.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composeapp.core.model.EditorTool

@Composable
fun ProfessionalToolTray(
    hasSelection: Boolean,
    isExporting: Boolean,
    onTool: (EditorTool) -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
) {
    var infoTool by remember { mutableStateOf<EditorTool?>(null) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0B0D)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorTool.entries.filter { it.ordinal < 8 }.forEach { tool ->
                    ToolChip(tool, enabled = hasSelection) { if (tool == EditorTool.AUDIO || tool == EditorTool.RECORD) infoTool = tool else onTool(tool) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorTool.entries.drop(8).take(8).forEach { tool ->
                    ToolChip(tool, enabled = hasSelection) { onTool(tool) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorTool.entries.drop(16).forEach { tool ->
                    ToolChip(tool, enabled = hasSelection || tool == EditorTool.EXPORT) {
                        when (tool) {
                            EditorTool.DUPLICATE -> onDuplicate()
                            EditorTool.EXPORT -> onExport()
                            else -> onTool(tool)
                        }
                    }
                }
            }
        }
    }
    infoTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { infoTool = null },
            title = { Text(tool.label) },
            text = { Text(if (tool == EditorTool.AUDIO) "Audio workspace: add music, sound effects and volume automation. Recording uses the device microphone when the recording flow is enabled." else "Recording workspace: microphone capture is isolated from the video timeline and can be added as an audio asset.") },
            confirmButton = { TextButton(onClick = { infoTool = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun ToolChip(tool: EditorTool, enabled: Boolean, onClick: () -> Unit) {
    TextButton(enabled = enabled, onClick = onClick, modifier = Modifier.size(width = 74.dp, height = 64.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tool.icon, fontWeight = FontWeight.Bold)
            Text(tool.label, maxLines = 1, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }
}
