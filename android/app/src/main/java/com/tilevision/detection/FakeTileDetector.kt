package com.tilevision.detection

import android.graphics.Bitmap
import com.tilevision.model.Tile

/**
 * Stand-in used until a trained model is placed in assets. Ignores the image and returns a
 * fixed 14-tile hand laid out across the middle of the frame, so the camera overlay and review
 * flow can be exercised end to end.
 */
class FakeTileDetector : TileDetector {
    override val isDemo: Boolean = true

    private val hand = listOf(
        Tile.M1, Tile.M2, Tile.M3,
        Tile.P4, Tile.P0, Tile.P6,
        Tile.S7, Tile.S8, Tile.S9,
        Tile.EAST, Tile.EAST, Tile.EAST,
        Tile.HAKU, Tile.HAKU,
    )

    override fun detect(bitmap: Bitmap): List<Detection> {
        val margin = 0.05f
        val slot = (1f - 2 * margin) / hand.size
        return hand.mapIndexed { i, tile ->
            val left = margin + i * slot
            Detection(
                tile = tile,
                confidence = 0.99f,
                box = BoundingBox(left + slot * 0.05f, 0.4f, left + slot * 0.95f, 0.6f),
            )
        }
    }
}
