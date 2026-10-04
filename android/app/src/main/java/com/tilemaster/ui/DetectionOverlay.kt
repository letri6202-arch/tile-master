package com.tilemaster.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tilemaster.camera.fitInside
import com.tilemaster.detection.Detection

/** Detections below this confidence are drawn in a warning color, as tiles worth double-checking. */
private const val CONFIDENT = 0.7f

/**
 * Draws detection boxes over content of size [frameWidth]x[frameHeight] that is displayed
 * fit-centered within this composable's bounds.
 */
@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    frameWidth: Int,
    frameHeight: Int,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color(0xFF231700), fontSize = 11.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier) {
        if (frameWidth <= 0 || frameHeight <= 0) return@Canvas
        val fit = fitInside(frameWidth, frameHeight, size.width, size.height)
        val corner = CornerRadius(4.dp.toPx())
        val padX = 4.dp.toPx()
        val padY = 1.dp.toPx()

        for (d in detections) {
            val color = if (d.confidence >= CONFIDENT) TableColors.Gold else TableColors.Coral
            val left = fit.left + d.box.left * fit.width
            val top = fit.top + d.box.top * fit.height
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(d.box.width * fit.width, d.box.height * fit.height),
                cornerRadius = corner,
                style = Stroke(width = 2.dp.toPx()),
            )

            val label = textMeasurer.measure(d.tile.id, labelStyle)
            val pillWidth = label.size.width + padX * 2
            val pillHeight = label.size.height + padY * 2
            // Above the box, or tucked inside its top edge when there's no room above.
            val pillTop = if (top - pillHeight - 2f >= 0f) top - pillHeight - 2f else top + 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(left, pillTop),
                size = Size(pillWidth, pillHeight),
                cornerRadius = corner,
            )
            drawText(label, topLeft = Offset(left + padX, pillTop + padY))
        }
    }
}
