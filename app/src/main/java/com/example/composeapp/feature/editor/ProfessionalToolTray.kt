package com.example.composeapp.feature.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composeapp.core.model.EditorTool
import com.example.composeapp.ui.theme.StudioBorder
import com.example.composeapp.ui.theme.StudioBorderSubtle
import com.example.composeapp.ui.theme.StudioCyan
import com.example.composeapp.ui.theme.StudioPink

@Composable
fun ProfessionalToolTray(
    hasSelection: Boolean,
    isExporting: Boolean,
    onTool: (EditorTool) -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onSplitAtPlayhead: (() -> Unit)? = null,
    onDeleteSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var infoTool by remember { mutableStateOf<EditorTool?>(null) }
    val scrollState = rememberScrollState()

    // Ordered sequence exactly like CapCut: Primary editing tools first, creative tools next
    val toolList = remember(hasSelection) {
        if (hasSelection) {
            listOf(
                EditorTool.CUT,
                EditorTool.SPEED,
                EditorTool.VOLUME,
                EditorTool.FILTERS,
                EditorTool.EFFECTS,
                EditorTool.ADJUSTMENT,
                EditorTool.CROP,
                EditorTool.ROTATE,
                EditorTool.FLIP,
                EditorTool.KEYFRAME,
                EditorTool.FREEZE,
                EditorTool.FADE,
                EditorTool.REVERSE,
                EditorTool.REVERB,
                EditorTool.DUPLICATE,
                EditorTool.TEXT,
                EditorTool.STICKERS,
                EditorTool.OVERLAY,
                EditorTool.TRANSITION,
                EditorTool.CANVAS,
                EditorTool.AUDIO,
                EditorTool.RECORD,
                EditorTool.CAPTIONS,
                EditorTool.GRAPH,
                EditorTool.EXPORT,
            )
        } else {
            listOf(
                EditorTool.AUDIO,
                EditorTool.TEXT,
                EditorTool.STICKERS,
                EditorTool.EFFECTS,
                EditorTool.FILTERS,
                EditorTool.OVERLAY,
                EditorTool.CANVAS,
                EditorTool.ADJUSTMENT,
                EditorTool.CAPTIONS,
                EditorTool.RECORD,
                EditorTool.CUT,
                EditorTool.SPEED,
                EditorTool.VOLUME,
                EditorTool.CROP,
                EditorTool.ROTATE,
                EditorTool.EXPORT,
            )
        }
    }

    Surface(
        color = Color(0xFF0F1016),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle top highlight border for depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                StudioBorderSubtle,
                                StudioBorder,
                                StudioBorderSubtle,
                            )
                        )
                    )
            )

            // Horizontal scrolling row of tools (CapCut bottom dock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                toolList.forEach { tool ->
                    val isActionActive = when (tool) {
                        EditorTool.EXPORT -> isExporting
                        EditorTool.CUT -> hasSelection
                        else -> hasSelection
                    }

                    CapCutToolItem(
                        tool = tool,
                        enabled = when (tool) {
                            EditorTool.EXPORT -> true
                            EditorTool.AUDIO, EditorTool.RECORD, EditorTool.TEXT,
                            EditorTool.STICKERS, EditorTool.OVERLAY, EditorTool.CANVAS,
                            EditorTool.CAPTIONS -> true
                            else -> hasSelection
                        },
                        onClick = {
                            when (tool) {
                                EditorTool.CUT -> {
                                    if (onSplitAtPlayhead != null) onSplitAtPlayhead()
                                    else onTool(tool)
                                }
                                EditorTool.DUPLICATE -> onDuplicate()
                                EditorTool.EXPORT -> onExport()
                                EditorTool.AUDIO, EditorTool.RECORD -> {
                                    infoTool = tool
                                }
                                else -> onTool(tool)
                            }
                        }
                    )
                }
            }
        }
    }

    infoTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { infoTool = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorToolIcon(tool, tint = StudioCyan, modifier = Modifier.size(24.dp))
                    Text(tool.label, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    text = when (tool) {
                        EditorTool.AUDIO -> "Audio Studio: Import background music tracks, apply sound effects, audio reverb, and automated fade-in/fade-out curves directly on the timeline."
                        EditorTool.RECORD -> "Voiceover Studio: Record high-fidelity narration directly to an isolated audio track synchronized with your video timeline."
                        else -> "${tool.label} settings and controls are ready to apply to your clips."
                    },
                    color = Color(0xFFD3D7E3),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { infoTool = null }) {
                    Text("Done", color = StudioCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF171A24),
        )
    }
}

@Composable
private fun CapCutToolItem(
    tool: EditorTool,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconBgColor by animateColorAsState(
        targetValue = if (enabled) Color(0xFF1A1D27) else Color(0xFF13151D),
        animationSpec = tween(150),
        label = "iconBg",
    )

    val iconTint by animateColorAsState(
        targetValue = if (!enabled) Color(0xFF535868)
        else when (tool) {
            EditorTool.CUT -> StudioCyan
            EditorTool.EXPORT -> StudioCyan
            EditorTool.RECORD -> StudioPink
            EditorTool.EFFECTS -> Color(0xFFBA8CFF)
            EditorTool.FILTERS -> Color(0xFFFFB74D)
            else -> Color.White
        },
        animationSpec = tween(150),
        label = "iconTint",
    )

    val textColor by animateColorAsState(
        targetValue = if (enabled) Color(0xFFE2E6F2) else Color(0xFF5D6377),
        animationSpec = tween(150),
        label = "textTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = StudioCyan),
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
    ) {
        // Icon rounded container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor)
                .border(
                    width = 1.dp,
                    color = if (enabled) StudioBorderSubtle else Color(0xFF171922),
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            EditorToolIcon(
                tool = tool,
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
        }

        Spacer(Modifier.height(5.dp))

        // Tool text label
        Text(
            text = tool.label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
