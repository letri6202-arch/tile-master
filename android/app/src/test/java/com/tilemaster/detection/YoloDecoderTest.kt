package com.tilemaster.detection

import com.tilemaster.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloDecoderTest {

    private val numClasses = Tile.entries.size

    /** Builds a channels-first output tensor `[4 + classes, anchors]` from per-anchor rows. */
    private fun channelsFirst(anchors: List<FloatArray>): FloatArray {
        val stride = 4 + numClasses
        val out = FloatArray(stride * anchors.size)
        anchors.forEachIndexed { a, row -> for (c in 0 until stride) out[c * anchors.size + a] = row[c] }
        return out
    }

    private fun anchor(cx: Float, cy: Float, w: Float, h: Float, cls: Int, score: Float): FloatArray =
        FloatArray(4 + numClasses).also {
            it[0] = cx; it[1] = cy; it[2] = w; it[3] = h
            it[4 + cls] = score
        }

    private val squareLetterbox = Letterbox(640, 640, 640)

    @Test
    fun decodesNormalizedBoxOnSquareImage() {
        val output = channelsFirst(listOf(anchor(0.5f, 0.5f, 0.1f, 0.2f, Tile.P0.ordinal, 0.9f)))

        val result = YoloDecoder.decode(output, 1, numClasses, squareLetterbox, Tile.Companion::fromClassIndex)

        assertEquals(1, result.size)
        val d = result.single()
        assertEquals(Tile.P0, d.tile)
        assertEquals(0.45f, d.box.left, 1e-4f)
        assertEquals(0.55f, d.box.right, 1e-4f)
        assertEquals(0.4f, d.box.top, 1e-4f)
        assertEquals(0.6f, d.box.bottom, 1e-4f)
    }

    @Test
    fun dropsAnchorsBelowThreshold() {
        val output = channelsFirst(listOf(anchor(0.5f, 0.5f, 0.1f, 0.1f, 0, 0.2f)))

        val result = YoloDecoder.decode(output, 1, numClasses, squareLetterbox, Tile.Companion::fromClassIndex)

        assertTrue(result.isEmpty())
    }

    @Test
    fun overlappingBoxesKeepOnlyMostConfidentLabel() {
        val output = channelsFirst(
            listOf(
                anchor(0.5f, 0.5f, 0.1f, 0.2f, Tile.M5.ordinal, 0.6f),
                anchor(0.505f, 0.5f, 0.1f, 0.2f, Tile.M0.ordinal, 0.8f),
                anchor(0.8f, 0.5f, 0.1f, 0.2f, Tile.EAST.ordinal, 0.7f),
            ),
        )

        val result = YoloDecoder.decode(output, 3, numClasses, squareLetterbox, Tile.Companion::fromClassIndex)

        assertEquals(listOf(Tile.M0, Tile.EAST), result.map { it.tile })
    }

    @Test
    fun transposedOutputDecodesTheSame() {
        val row = anchor(0.25f, 0.5f, 0.1f, 0.1f, Tile.S3.ordinal, 0.9f)

        val result = YoloDecoder.decode(
            row, 1, numClasses, squareLetterbox, Tile.Companion::fromClassIndex, channelsFirst = false,
        )

        assertEquals(Tile.S3, result.single().tile)
        assertEquals(0.25f, result.single().box.centerX, 1e-4f)
    }

    @Test
    fun undoesLetterboxPaddingForWideImage() {
        // 1280x720 into 640: scale 0.5, content 640x360, 140px bars top and bottom.
        val letterbox = Letterbox(1280, 720, 640)
        val cyInInput = (140f + 360f / 2f) / 640f
        val output = channelsFirst(listOf(anchor(0.5f, cyInInput, 0.1f, 0.1f, 0, 0.9f)))

        val box = YoloDecoder.decode(output, 1, numClasses, letterbox, Tile.Companion::fromClassIndex).single().box

        assertEquals(0.5f, box.centerY, 1e-3f)
        assertEquals(0.5f, box.centerX, 1e-3f)
        // 64px tall in input = 128px in source = 128/720 of the height.
        assertEquals(128f / 720f, box.height, 1e-3f)
    }
}
