package com.tilemaster.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import kotlin.math.max
import kotlin.math.roundToInt

/** Converts a camera frame to an upright bitmap, downscaled so its long side is at most [maxSide]. */
fun ImageProxy.toUprightBitmap(maxSide: Int = 2048): Bitmap {
    val raw = toBitmap()
    val degrees = imageInfo.rotationDegrees
    val longSide = max(raw.width, raw.height)
    val scale = if (longSide > maxSide) maxSide / longSide.toFloat() else 1f
    if (degrees == 0 && scale == 1f) return raw

    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(degrees.toFloat())
    }
    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
}

/** Size of [width]x[height] content when fitted (letterboxed) inside a [boxWidth]x[boxHeight] area. */
fun fitInside(width: Int, height: Int, boxWidth: Float, boxHeight: Float): FitRect {
    if (width <= 0 || height <= 0) return FitRect(0f, 0f, boxWidth, boxHeight)
    val scale = minOf(boxWidth / width, boxHeight / height)
    val w = (width * scale).roundToInt().toFloat()
    val h = (height * scale).roundToInt().toFloat()
    return FitRect((boxWidth - w) / 2f, (boxHeight - h) / 2f, w, h)
}

data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)
