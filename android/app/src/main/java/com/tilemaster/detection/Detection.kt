package com.tilemaster.detection

import com.tilemaster.model.Tile
import kotlin.math.max
import kotlin.math.min

/** Axis-aligned box in coordinates normalized to 0..1 of the source image. */
data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = max(0f, width) * max(0f, height)

    fun iou(other: BoundingBox): Float {
        val interW = max(0f, min(right, other.right) - max(left, other.left))
        val interH = max(0f, min(bottom, other.bottom) - max(top, other.top))
        val inter = interW * interH
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }
}

data class Detection(val tile: Tile, val confidence: Float, val box: BoundingBox)
