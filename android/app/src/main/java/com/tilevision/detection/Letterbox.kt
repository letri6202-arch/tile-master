package com.tilevision.detection

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Geometry for fitting a source image into a square model input without distortion, padding the
 * remaining space (the same "letterbox" preprocessing Ultralytics uses during training).
 */
class Letterbox(val srcWidth: Int, val srcHeight: Int, val inputSize: Int) {
    val scale: Float = min(inputSize / srcWidth.toFloat(), inputSize / srcHeight.toFloat())
    val scaledWidth: Int = (srcWidth * scale).roundToInt()
    val scaledHeight: Int = (srcHeight * scale).roundToInt()
    val padX: Int = (inputSize - scaledWidth) / 2
    val padY: Int = (inputSize - scaledHeight) / 2

    /** Maps an x coordinate in model-input pixels to a 0..1 fraction of the source width. */
    fun toSourceX(x: Float): Float = ((x - padX) / scale / srcWidth).coerceIn(0f, 1f)

    /** Maps a y coordinate in model-input pixels to a 0..1 fraction of the source height. */
    fun toSourceY(y: Float): Float = ((y - padY) / scale / srcHeight).coerceIn(0f, 1f)
}
