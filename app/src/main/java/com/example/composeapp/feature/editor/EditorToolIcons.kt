package com.example.composeapp.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.composeapp.core.model.EditorTool

@Composable
fun EditorToolIcon(
    tool: EditorTool,
    modifier: Modifier = Modifier.size(22.dp),
    tint: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = (w * 0.085f).coerceAtLeast(1.5f)

        when (tool) {
            EditorTool.CUT -> drawCutIcon(w, h, tint, strokeWidth)
            EditorTool.AUDIO -> drawAudioIcon(w, h, tint, strokeWidth)
            EditorTool.TEXT -> drawTextIcon(w, h, tint, strokeWidth)
            EditorTool.STICKERS -> drawStickerIcon(w, h, tint, strokeWidth)
            EditorTool.EFFECTS -> drawEffectsIcon(w, h, tint, strokeWidth)
            EditorTool.FILTERS -> drawFiltersIcon(w, h, tint, strokeWidth)
            EditorTool.OVERLAY -> drawOverlayIcon(w, h, tint, strokeWidth)
            EditorTool.SPEED -> drawSpeedIcon(w, h, tint, strokeWidth)
            EditorTool.VOLUME -> drawVolumeIcon(w, h, tint, strokeWidth)
            EditorTool.CROP -> drawCropIcon(w, h, tint, strokeWidth)
            EditorTool.ROTATE -> drawRotateIcon(w, h, tint, strokeWidth)
            EditorTool.FLIP -> drawFlipIcon(w, h, tint, strokeWidth)
            EditorTool.FREEZE -> drawFreezeIcon(w, h, tint, strokeWidth)
            EditorTool.REVERSE -> drawReverseIcon(w, h, tint, strokeWidth)
            EditorTool.FADE -> drawFadeIcon(w, h, tint, strokeWidth)
            EditorTool.KEYFRAME -> drawKeyframeIcon(w, h, tint, strokeWidth)
            EditorTool.ADJUSTMENT -> drawAdjustmentIcon(w, h, tint, strokeWidth)
            EditorTool.TRANSITION -> drawTransitionIcon(w, h, tint, strokeWidth)
            EditorTool.CANVAS -> drawCanvasIcon(w, h, tint, strokeWidth)
            EditorTool.DUPLICATE -> drawDuplicateIcon(w, h, tint, strokeWidth)
            EditorTool.EXPORT -> drawExportIcon(w, h, tint, strokeWidth)
            EditorTool.RECORD -> drawRecordIcon(w, h, tint, strokeWidth)
            EditorTool.REVERB -> drawReverbIcon(w, h, tint, strokeWidth)
            EditorTool.CAPTIONS -> drawCaptionsIcon(w, h, tint, strokeWidth)
            EditorTool.GRAPH -> drawGraphIcon(w, h, tint, strokeWidth)
        }
    }
}

private fun DrawScope.drawCutIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Scissors cutting blades and loops
    val loopRadius = w * 0.16f
    // Top-left loop
    drawCircle(
        color = tint,
        radius = loopRadius,
        center = Offset(w * 0.28f, h * 0.32f),
        style = Stroke(strokeWidth),
    )
    // Bottom-left loop
    drawCircle(
        color = tint,
        radius = loopRadius,
        center = Offset(w * 0.28f, h * 0.68f),
        style = Stroke(strokeWidth),
    )
    // Crossed blade lines
    drawLine(
        color = tint,
        start = Offset(w * 0.38f, h * 0.38f),
        end = Offset(w * 0.82f, h * 0.72f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.38f, h * 0.62f),
        end = Offset(w * 0.82f, h * 0.28f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawAudioIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Modern music note with beam
    drawCircle(color = tint, radius = w * 0.16f, center = Offset(w * 0.34f, h * 0.74f))
    drawCircle(color = tint, radius = w * 0.16f, center = Offset(w * 0.74f, h * 0.62f))
    drawLine(color = tint, start = Offset(w * 0.44f, h * 0.74f), end = Offset(w * 0.44f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = tint, start = Offset(w * 0.84f, h * 0.62f), end = Offset(w * 0.84f, h * 0.15f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = tint, start = Offset(w * 0.44f, h * 0.25f), end = Offset(w * 0.84f, h * 0.15f), strokeWidth = strokeWidth * 1.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawTextIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Bold CapCut style "T"
    val barWidth = strokeWidth * 1.4f
    // Top crossbar
    drawLine(color = tint, start = Offset(w * 0.18f, h * 0.22f), end = Offset(w * 0.82f, h * 0.22f), strokeWidth = barWidth, cap = StrokeCap.Round)
    // Vertical stem
    drawLine(color = tint, start = Offset(w * 0.5f, h * 0.22f), end = Offset(w * 0.5f, h * 0.78f), strokeWidth = barWidth, cap = StrokeCap.Round)
    // Small bottom serif/base
    drawLine(color = tint, start = Offset(w * 0.36f, h * 0.78f), end = Offset(w * 0.64f, h * 0.78f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawStickerIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // 5-point sticker star with rounded center
    val path = Path()
    val cx = w * 0.5f
    val cy = h * 0.5f
    val outerR = w * 0.42f
    val innerR = w * 0.20f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = Math.toRadians((i * 36 - 90).toDouble())
        val x = (cx + r * kotlin.math.cos(angle)).toFloat()
        val y = (cy + r * kotlin.math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = tint, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}

private fun DrawScope.drawEffectsIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Magic Sparkles / 4-point stars (CapCut special effects)
    fun drawSparkle(cx: Float, cy: Float, radius: Float) {
        val path = Path().apply {
            moveTo(cx, cy - radius)
            quadraticTo(cx, cy, cx + radius, cy)
            quadraticTo(cx, cy, cx, cy + radius)
            quadraticTo(cx, cy, cx - radius, cy)
            quadraticTo(cx, cy, cx, cy - radius)
            close()
        }
        drawPath(path, color = tint, style = Fill)
    }
    drawSparkle(w * 0.42f, h * 0.42f, w * 0.32f)
    drawSparkle(w * 0.76f, h * 0.26f, w * 0.16f)
    drawSparkle(w * 0.25f, h * 0.74f, w * 0.14f)
}

private fun DrawScope.drawFiltersIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Overlapping filter rings
    val r = w * 0.26f
    drawCircle(color = tint, radius = r, center = Offset(w * 0.38f, h * 0.46f), style = Stroke(strokeWidth))
    drawCircle(color = tint, radius = r, center = Offset(w * 0.62f, h * 0.46f), style = Stroke(strokeWidth))
    drawCircle(color = tint, radius = r * 0.9f, center = Offset(w * 0.5f, h * 0.64f), style = Stroke(strokeWidth))
}

private fun DrawScope.drawOverlayIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Picture-in-picture overlapping frames
    drawRoundRect(
        color = tint.copy(alpha = 0.5f),
        topLeft = Offset(w * 0.14f, h * 0.16f),
        size = Size(w * 0.58f, h * 0.52f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(strokeWidth),
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.32f, h * 0.36f),
        size = Size(w * 0.54f, h * 0.48f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Fill,
    )
}

private fun DrawScope.drawSpeedIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Speedometer dial with indicator needle
    drawArc(
        color = tint,
        startAngle = 140f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(w * 0.15f, h * 0.18f),
        size = Size(w * 0.7f, h * 0.7f),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
    // Needle
    drawLine(
        color = tint,
        start = Offset(w * 0.5f, h * 0.55f),
        end = Offset(w * 0.72f, h * 0.35f),
        strokeWidth = strokeWidth * 1.2f,
        cap = StrokeCap.Round,
    )
    drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.55f))
}

private fun DrawScope.drawVolumeIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Speaker cone + acoustic waves
    val path = Path().apply {
        moveTo(w * 0.16f, h * 0.4f)
        lineTo(w * 0.32f, h * 0.4f)
        lineTo(w * 0.54f, h * 0.22f)
        lineTo(w * 0.54f, h * 0.78f)
        lineTo(w * 0.32f, h * 0.6f)
        lineTo(w * 0.16f, h * 0.6f)
        close()
    }
    drawPath(path, color = tint, style = Fill)
    // Sound arcs
    drawArc(
        color = tint,
        startAngle = -45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(w * 0.45f, h * 0.3f),
        size = Size(w * 0.34f, h * 0.4f),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
    drawArc(
        color = tint,
        startAngle = -45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(w * 0.48f, h * 0.18f),
        size = Size(w * 0.46f, h * 0.64f),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawCropIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Two overlapping L-shaped crop borders
    val path = Path().apply {
        // Top-left L
        moveTo(w * 0.15f, h * 0.3f)
        lineTo(w * 0.7f, h * 0.3f)
        moveTo(w * 0.3f, h * 0.15f)
        lineTo(w * 0.3f, h * 0.7f)
        // Bottom-right L
        moveTo(w * 0.85f, h * 0.7f)
        lineTo(w * 0.3f, h * 0.7f)
        moveTo(w * 0.7f, h * 0.85f)
        lineTo(w * 0.7f, h * 0.3f)
    }
    drawPath(path, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawRotateIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Curved circular arrow
    drawArc(
        color = tint,
        startAngle = 40f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(w * 0.18f, h * 0.18f),
        size = Size(w * 0.64f, h * 0.64f),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
    // Arrow head
    val arrow = Path().apply {
        moveTo(w * 0.72f, h * 0.46f)
        lineTo(w * 0.86f, h * 0.36f)
        lineTo(w * 0.76f, h * 0.22f)
    }
    drawPath(arrow, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawFlipIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Dual opposing horizontal arrows
    // Top arrow pointing right
    drawLine(color = tint, start = Offset(w * 0.18f, h * 0.38f), end = Offset(w * 0.78f, h * 0.38f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    val rightHead = Path().apply {
        moveTo(w * 0.64f, h * 0.26f)
        lineTo(w * 0.82f, h * 0.38f)
        lineTo(w * 0.64f, h * 0.50f)
    }
    drawPath(rightHead, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))

    // Bottom arrow pointing left
    drawLine(color = tint, start = Offset(w * 0.82f, h * 0.64f), end = Offset(w * 0.22f, h * 0.64f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    val leftHead = Path().apply {
        moveTo(w * 0.36f, h * 0.52f)
        lineTo(w * 0.18f, h * 0.64f)
        lineTo(w * 0.36f, h * 0.76f)
    }
    drawPath(leftHead, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawFreezeIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Intricate 6-arm snowflake
    val cx = w * 0.5f
    val cy = h * 0.5f
    for (i in 0 until 3) {
        val angle = Math.toRadians((i * 60).toDouble())
        val dx = (kotlin.math.cos(angle) * w * 0.36f).toFloat()
        val dy = (kotlin.math.sin(angle) * h * 0.36f).toFloat()
        drawLine(color = tint, start = Offset(cx - dx, cy - dy), end = Offset(cx + dx, cy + dy), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
    drawCircle(color = tint, radius = w * 0.08f, center = Offset(cx, cy))
}

private fun DrawScope.drawReverseIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Rewind dual triangles pointing left
    fun drawTriangle(offsetRight: Float) {
        val path = Path().apply {
            moveTo(offsetRight, h * 0.25f)
            lineTo(offsetRight - w * 0.32f, h * 0.5f)
            lineTo(offsetRight, h * 0.75f)
            close()
        }
        drawPath(path, color = tint, style = Fill)
    }
    drawTriangle(w * 0.88f)
    drawTriangle(w * 0.54f)
}

private fun DrawScope.drawFadeIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Ramp curve from bottom-left to top-right
    val path = Path().apply {
        moveTo(w * 0.18f, h * 0.78f)
        cubicTo(w * 0.45f, h * 0.78f, w * 0.55f, h * 0.22f, w * 0.82f, h * 0.22f)
    }
    drawPath(path, color = tint, style = Stroke(strokeWidth * 1.2f, cap = StrokeCap.Round))
    // Arrow head at top-right
    val head = Path().apply {
        moveTo(w * 0.68f, h * 0.22f)
        lineTo(w * 0.84f, h * 0.22f)
        lineTo(w * 0.84f, h * 0.38f)
    }
    drawPath(head, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawKeyframeIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Distinctive keyframe diamond with central glow
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.14f)
        lineTo(w * 0.86f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.86f)
        lineTo(w * 0.14f, h * 0.5f)
        close()
    }
    drawPath(path, color = tint, style = Fill)
    drawCircle(color = Color(0xFF090A0E), radius = w * 0.11f, center = Offset(w * 0.5f, h * 0.5f))
}

private fun DrawScope.drawAdjustmentIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Sliders / Adjustment controls
    // Top slider
    drawLine(color = tint, start = Offset(w * 0.18f, h * 0.34f), end = Offset(w * 0.82f, h * 0.34f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.38f, h * 0.34f))
    // Bottom slider
    drawLine(color = tint, start = Offset(w * 0.18f, h * 0.66f), end = Offset(w * 0.82f, h * 0.66f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.64f, h * 0.66f))
}

private fun DrawScope.drawTransitionIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Intersecting transition squares
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.18f, h * 0.25f),
        size = Size(w * 0.44f, h * 0.44f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(strokeWidth),
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.38f, h * 0.35f),
        size = Size(w * 0.44f, h * 0.44f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Fill,
    )
}

private fun DrawScope.drawCanvasIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Canvas / 9:16 aspect ratio box
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.28f, h * 0.15f),
        size = Size(w * 0.44f, h * 0.7f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(strokeWidth),
    )
    // Horizontal format indicator bar
    drawLine(color = tint, start = Offset(w * 0.38f, h * 0.5f), end = Offset(w * 0.62f, h * 0.5f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawDuplicateIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Dual stacked cards (copy/duplicate)
    drawRoundRect(
        color = tint.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.16f, h * 0.16f),
        size = Size(w * 0.52f, h * 0.52f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(strokeWidth),
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.32f, h * 0.32f),
        size = Size(w * 0.52f, h * 0.52f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Fill,
    )
}

private fun DrawScope.drawExportIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Tray + upward export arrow
    // Tray
    val tray = Path().apply {
        moveTo(w * 0.2f, h * 0.52f)
        lineTo(w * 0.2f, h * 0.78f)
        lineTo(w * 0.8f, h * 0.78f)
        lineTo(w * 0.8f, h * 0.52f)
    }
    drawPath(tray, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Arrow
    drawLine(color = tint, start = Offset(w * 0.5f, h * 0.64f), end = Offset(w * 0.5f, h * 0.22f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    val head = Path().apply {
        moveTo(w * 0.34f, h * 0.34f)
        lineTo(w * 0.5f, h * 0.18f)
        lineTo(w * 0.66f, h * 0.34f)
    }
    drawPath(head, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawRecordIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Record button: concentric ring and filled red/accent circle
    drawCircle(color = tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(strokeWidth))
    drawCircle(color = Color(0xFFFF2A55), radius = w * 0.24f, center = Offset(w * 0.5f, h * 0.5f), style = Fill)
}

private fun DrawScope.drawReverbIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Audio waveform lines
    val heights = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f, 0.2f)
    val step = (w * 0.72f) / (heights.size - 1)
    val startX = w * 0.14f
    heights.forEachIndexed { i, factor ->
        val x = startX + i * step
        val barH = h * 0.6f * factor
        drawLine(
            color = tint,
            start = Offset(x, h * 0.5f - barH / 2),
            end = Offset(x, h * 0.5f + barH / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCaptionsIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Closed captions speech frame
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.16f, h * 0.22f),
        size = Size(w * 0.68f, h * 0.56f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(strokeWidth),
    )
    // Two C's inside
    drawArc(
        color = tint,
        startAngle = 50f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(w * 0.28f, h * 0.36f),
        size = Size(w * 0.18f, h * 0.28f),
        style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round),
    )
    drawArc(
        color = tint,
        startAngle = 50f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(w * 0.52f, h * 0.36f),
        size = Size(w * 0.18f, h * 0.28f),
        style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawGraphIcon(w: Float, h: Float, tint: Color, strokeWidth: Float) {
    // Keyframe curve / speed ramp graph
    val path = Path().apply {
        moveTo(w * 0.16f, h * 0.78f)
        cubicTo(w * 0.35f, h * 0.78f, w * 0.55f, h * 0.22f, w * 0.84f, h * 0.22f)
    }
    drawPath(path, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.16f, h * 0.78f))
    drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.84f, h * 0.22f))
}
