package com.tilevision.model

import com.tilevision.detection.BoundingBox
import com.tilevision.detection.Detection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandTest {

    private fun at(tile: Tile, x: Float) = Detection(tile, 0.9f, BoundingBox(x, 0.4f, x + 0.05f, 0.6f))

    @Test
    fun ordersDetectionsLeftToRight() {
        val shuffled = listOf(at(Tile.M3, 0.5f), at(Tile.M1, 0.1f), at(Tile.M2, 0.3f))

        assertEquals(listOf(Tile.M1, Tile.M2, Tile.M3), Hand.orderLeftToRight(shuffled).map { it.tile })
    }

    @Test
    fun notationGroupsBySuitAndWritesRedFiveAsZero() {
        val tiles = listOf(
            Tile.HAKU, Tile.P6, Tile.M1, Tile.P0, Tile.M3, Tile.EAST, Tile.P4, Tile.M2,
        )

        assertEquals("123m406p15z", Hand.toNotation(tiles))
    }

    @Test
    fun completeHandHasNoProblems() {
        val tiles = listOf(
            Tile.M1, Tile.M2, Tile.M3, Tile.P4, Tile.P0, Tile.P6, Tile.S7,
            Tile.S8, Tile.S9, Tile.EAST, Tile.EAST, Tile.EAST, Tile.HAKU, Tile.HAKU,
        )

        assertTrue(Hand.problems(tiles).isEmpty())
    }

    @Test
    fun flagsWrongCountTooManyCopiesAndDuplicateRedFive() {
        val tiles = listOf(Tile.M5, Tile.M5, Tile.M5, Tile.M0, Tile.M0)

        val problems = Hand.problems(tiles)

        assertEquals(3, problems.size)
        assertTrue(problems.any { "Found 5 tiles" in it })
        assertTrue(problems.any { "Too many 5m" in it })
        assertTrue(problems.any { "Red 5 Man" in it })
    }

    @Test
    fun tileIdsMatchClassIndexOrder() {
        assertEquals("1m", Tile.fromClassIndex(0)?.id)
        assertEquals("7z", Tile.fromClassIndex(33)?.id)
        assertEquals("0s", Tile.fromClassIndex(36)?.id)
        assertEquals(37, Tile.entries.size)
    }
}
