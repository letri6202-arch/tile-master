package com.tilevision.detection

import com.tilevision.model.Tile

/**
 * Decodes the raw output of an Ultralytics YOLO (v8/11) detection model exported to TFLite.
 *
 * The output tensor is `[1, 4 + numClasses, numAnchors]` ("channels first"), or the transpose
 * `[1, numAnchors, 4 + numClasses]`. Each anchor holds `cx, cy, w, h` followed by one score per
 * class. Ultralytics' TFLite export normalizes the box values to 0..1 of the model input.
 */
object YoloDecoder {

    fun decode(
        output: FloatArray,
        numAnchors: Int,
        numClasses: Int,
        letterbox: Letterbox,
        classToTile: (Int) -> Tile?,
        channelsFirst: Boolean = true,
        normalizedCoords: Boolean = true,
        confidenceThreshold: Float = 0.4f,
        iouThreshold: Float = 0.5f,
    ): List<Detection> {
        val stride = 4 + numClasses
        require(output.size >= stride * numAnchors) {
            "Output has ${output.size} values, expected ${stride * numAnchors}"
        }
        fun value(channel: Int, anchor: Int): Float =
            if (channelsFirst) output[channel * numAnchors + anchor] else output[anchor * stride + channel]

        val coordScale = if (normalizedCoords) letterbox.inputSize.toFloat() else 1f
        val candidates = ArrayList<Detection>()

        for (anchor in 0 until numAnchors) {
            var bestClass = -1
            var bestScore = confidenceThreshold
            for (c in 0 until numClasses) {
                val score = value(4 + c, anchor)
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }
            if (bestClass < 0) continue
            val tile = classToTile(bestClass) ?: continue

            val cx = value(0, anchor) * coordScale
            val cy = value(1, anchor) * coordScale
            val halfW = value(2, anchor) * coordScale / 2f
            val halfH = value(3, anchor) * coordScale / 2f
            val box = BoundingBox(
                left = letterbox.toSourceX(cx - halfW),
                top = letterbox.toSourceY(cy - halfH),
                right = letterbox.toSourceX(cx + halfW),
                bottom = letterbox.toSourceY(cy + halfH),
            )
            candidates += Detection(tile, bestScore, box)
        }
        return nonMaxSuppression(candidates, iouThreshold)
    }

    /**
     * Class-agnostic NMS: physical tiles never overlap, so two boxes on the same spot are the
     * same tile and only the most confident label is kept.
     */
    fun nonMaxSuppression(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        val kept = ArrayList<Detection>()
        for (candidate in detections.sortedByDescending { it.confidence }) {
            if (kept.none { it.box.iou(candidate.box) > iouThreshold }) kept += candidate
        }
        return kept
    }
}
