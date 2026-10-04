package com.tilemaster.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.tilemaster.detection.Detection
import com.tilemaster.detection.TileDetector

data class LiveDetections(val detections: List<Detection>, val frameWidth: Int, val frameHeight: Int) {
    companion object {
        val EMPTY = LiveDetections(emptyList(), 0, 0)
    }
}

/** Runs the detector on preview frames, at most once per [minIntervalMs], for the live overlay. */
class TileAnalyzer(
    private val detector: TileDetector,
    private val minIntervalMs: Long = 500,
    private val onResult: (LiveDetections) -> Unit,
) : ImageAnalysis.Analyzer {

    private var lastRunMs = 0L

    override fun analyze(image: ImageProxy) {
        image.use {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRunMs < minIntervalMs) return
            lastRunMs = now

            val bitmap = it.toUprightBitmap(maxSide = 1024)
            onResult(LiveDetections(detector.detect(bitmap), bitmap.width, bitmap.height))
        }
    }
}
