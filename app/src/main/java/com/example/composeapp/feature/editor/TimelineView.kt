package com.example.composeapp.feature.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.composeapp.core.model.Clip
import com.example.composeapp.core.model.Track
import com.example.composeapp.core.model.TrackType
import com.example.composeapp.ui.theme.StudioBorder
import com.example.composeapp.ui.theme.StudioBorderSubtle
import com.example.composeapp.ui.theme.StudioCyan
import com.example.composeapp.ui.theme.StudioPink
import kotlin.math.roundToInt

private const val MS_PER_DP = 22L // Scale: 22ms per dp (responsive zoom)

@Composable
fun TimelineView(
    tracks: List<Track>,
    selectedClipId: String?,
    playheadMs: Long,
    totalDurationMs: Long,
    onSelectClip: (String?) -> Unit,
    onSeekTo: (Long) -> Unit,
    onReorderClip: (trackId: String, clipId: String, targetIndex: Int) -> Unit,
    onTrimClip: (trackId: String, clipId: String, newStartMs: Long, newDurationMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val totalWidthDp = remember(totalDurationMs) {
        maxOf((totalDurationMs / MS_PER_DP).toInt(), 340) + 120
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E13)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Timeline Header: Status and Quick Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StudioCyan)
                    )
                    Text(
                        text = "TIMELINE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${formatDuration(playheadMs)} / ${formatDuration(totalDurationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioCyan,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${tracks.sumOf { it.clips.size }} clips",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888E9E),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Interactive Scrollable Track Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color(0xFF090A0E))
            ) {
                // Scrollable content (Time Ruler + Multi-track Clips)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                ) {
                    Column(
                        modifier = Modifier
                            .width(totalWidthDp.dp)
                            .fillMaxHeight()
                    ) {
                        // 1. Time Ruler
                        TimelineRuler(
                            totalDurationMs = totalDurationMs,
                            totalWidthDp = totalWidthDp,
                            onSeekTo = onSeekTo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        )

                        Spacer(Modifier.height(6.dp))

                        // 2. Tracks Stack
                        tracks.forEach { track ->
                            TrackRow(
                                track = track,
                                selectedClipId = selectedClipId,
                                onSelectClip = onSelectClip,
                                onReorderClip = { clipId, targetIndex ->
                                    onReorderClip(track.trackId, clipId, targetIndex)
                                },
                                onTrimClip = { clipId, startMs, durMs ->
                                    onTrimClip(track.trackId, clipId, startMs, durMs)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            )
                        }
                    }

                    // 3. Playhead Needle (synchronous with playheadMs)
                    val playheadX = (playheadMs / MS_PER_DP).toFloat()
                    PlayheadNeedle(
                        positionXDp = playheadX,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    totalDurationMs: Long,
    totalWidthDp: Int,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF13151D))
            .pointerInput(totalDurationMs) {
                detectTapGestures { offset ->
                    val seekMs = (offset.x / density * MS_PER_DP).toLong().coerceIn(0L, totalDurationMs)
                    onSeekTo(seekMs)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalSeconds = (totalDurationMs / 1000L).toInt() + 10
            val lineY = size.height

            // Baseline
            drawLine(
                color = Color(0xFF262A38),
                start = Offset(0f, lineY),
                end = Offset(size.width, lineY),
                strokeWidth = 1.dp.toPx(),
            )

            // Second tick marks
            for (sec in 0..totalSeconds) {
                val secMs = sec * 1000L
                val xDp = (secMs / MS_PER_DP).toInt()
                val xPx = xDp.dp.toPx()
                val isMajor = sec % 5 == 0

                val tickHeight = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                val tickColor = if (isMajor) Color(0xFF888E9E) else Color(0xFF383D4E)

                drawLine(
                    color = tickColor,
                    start = Offset(xPx, lineY - tickHeight),
                    end = Offset(xPx, lineY),
                    strokeWidth = (if (isMajor) 1.5.dp else 1.dp).toPx(),
                )
            }
        }

        // Second text labels at major intervals
        val totalSeconds = (totalDurationMs / 1000L).toInt() + 6
        for (sec in 0..totalSeconds step 5) {
            val xDp = ((sec * 1000L) / MS_PER_DP).toInt()
            Text(
                text = "%02d:%02d".format(sec / 60, sec % 60),
                color = Color(0xFF767C8E),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                modifier = Modifier
                    .offset(x = (xDp + 4).dp, y = 2.dp)
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    selectedClipId: String?,
    onSelectClip: (String?) -> Unit,
    onReorderClip: (clipId: String, targetIndex: Int) -> Unit,
    onTrimClip: (clipId: String, startMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Track Header badge
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(56.dp)
                .background(Color(0xFF161822), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .border(1.dp, StudioBorderSubtle, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (track.type == TrackType.VIDEO) "VID" else "AUD",
                    color = if (track.type == TrackType.VIDEO) StudioCyan else StudioPink,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
                Text(
                    text = "${track.clips.size}",
                    color = Color(0xFF6E7485),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Clips in this track
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            track.clips.forEachIndexed { index, clip ->
                val isSelected = clip.clipId == selectedClipId
                val clipWidthDp = ((clip.durationMs / MS_PER_DP).toInt()).coerceIn(80, 480)

                DraggableTimelineClip(
                    clip = clip,
                    index = index,
                    totalClipsInTrack = track.clips.size,
                    isSelected = isSelected,
                    widthDp = clipWidthDp,
                    trackType = track.type,
                    onSelect = { onSelectClip(clip.clipId) },
                    onReorder = { targetIdx -> onReorderClip(clip.clipId, targetIdx) },
                    onTrim = { startMs, durMs -> onTrimClip(clip.clipId, startMs, durMs) },
                )
            }
        }
    }
}

@Composable
private fun DraggableTimelineClip(
    clip: Clip,
    index: Int,
    totalClipsInTrack: Int,
    isSelected: Boolean,
    widthDp: Int,
    trackType: TrackType,
    onSelect: () -> Unit,
    onReorder: (Int) -> Unit,
    onTrim: (Long, Long) -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragAccumulatorPx by remember { mutableFloatStateOf(0f) }

    val animatedElevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else if (isSelected) 6f else 1f,
        animationSpec = spring(),
        label = "clipElev",
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f,
        animationSpec = spring(),
        label = "clipScale",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) StudioCyan else Color(0xFF262B3B),
        animationSpec = tween(150),
        label = "clipBorder",
    )

    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(58.dp)
            .zIndex(if (isDragging) 10f else if (isSelected) 5f else 1f)
            .graphicsLayer {
                translationX = if (isDragging) dragAccumulatorPx else 0f
                scaleX = animatedScale
                scaleY = animatedScale
                shadowElevation = animatedElevation
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    if (trackType == TrackType.VIDEO) {
                        listOf(Color(0xFF1E2433), Color(0xFF252D40))
                    } else {
                        listOf(Color(0xFF281C26), Color(0xFF33202F))
                    }
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .pointerInput(clip.clipId, index, totalClipsInTrack) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragAccumulatorPx = 0f
                        onSelect()
                    },
                    onDragEnd = {
                        isDragging = false
                        // Calculate how many clip positions the drag shifted
                        val averageClipPx = widthDp * density
                        val shift = (dragAccumulatorPx / (averageClipPx * 0.7f)).roundToInt()
                        if (shift != 0) {
                            val newIndex = (index + shift).coerceIn(0, totalClipsInTrack - 1)
                            if (newIndex != index) {
                                onReorder(newIndex)
                            }
                        }
                        dragAccumulatorPx = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccumulatorPx = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulatorPx += dragAmount
                    }
                )
            }
            .clickable { onSelect() }
    ) {
        // Visual filmstrip notches pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val notchW = 4.dp.toPx()
            val notchH = 3.dp.toPx()
            val spacing = 12.dp.toPx()
            var x = 6.dp.toPx()
            while (x < size.width - 6.dp.toPx()) {
                // Top film notch
                drawRect(Color(0x33000000), topLeft = Offset(x, 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(notchW, notchH))
                // Bottom film notch
                drawRect(Color(0x33000000), topLeft = Offset(x, size.height - 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(notchW, notchH))
                x += spacing
            }
        }

        // Left trim handle (visible when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(StudioCyan, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .pointerInput(clip.clipId) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val deltaMs = (dragAmount / density * MS_PER_DP).toLong()
                            val newStart = (clip.startMs + deltaMs).coerceAtLeast(0L)
                            val newDur = (clip.durationMs - deltaMs).coerceAtLeast(500L)
                            onTrim(newStart, newDur)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(2.dp).height(18.dp).background(Color.Black))
            }
        }

        // Clip Info (Content in center)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (isSelected) 18.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (trackType == TrackType.VIDEO) "VIDEO CLIP" else "AUDIO CLIP",
                color = if (isSelected) StudioCyan else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Text(
                text = formatDuration(clip.durationMs),
                color = if (isSelected) Color(0xFFCBD5E1) else Color(0xFF888E9E),
                fontSize = 10.sp,
            )
        }

        // Right trim handle (visible when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(StudioCyan, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .pointerInput(clip.clipId) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val deltaMs = (dragAmount / density * MS_PER_DP).toLong()
                            val newDur = (clip.durationMs + deltaMs).coerceAtLeast(500L)
                            onTrim(clip.startMs, newDur)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(2.dp).height(18.dp).background(Color.Black))
            }
        }

        // Quick Move Buttons (if selected)
        if (isSelected) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF090A0E))
                            .clickable { onReorder(index - 1) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("◀", color = StudioCyan, fontSize = 8.sp)
                    }
                }
                if (index < totalClipsInTrack - 1) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF090A0E))
                            .clickable { onReorder(index + 1) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶", color = StudioCyan, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayheadNeedle(
    positionXDp: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = positionXDp.dp)
            .width(2.dp)
    ) {
        // Red / White Playhead Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White)
        )
        // Cursor Handle at top
        Box(
            modifier = Modifier
                .offset(x = (-5).dp, y = (-2).dp)
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
                .border(1.dp, StudioCyan, RoundedCornerShape(3.dp))
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    val millis = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(minutes, seconds, millis)
}
