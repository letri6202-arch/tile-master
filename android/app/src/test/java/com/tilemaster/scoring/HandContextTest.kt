package com.tilemaster.scoring

import com.tilemaster.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandContextTest {

    private fun tiles(notation: String): List<Tile> {
        val result = mutableListOf<Tile>()
        var digits = ""
        for (ch in notation) {
            if (ch.isDigit()) {
                digits += ch
            } else {
                digits.forEach { result += Tile.fromId("$it$ch")!! }
                digits = ""
            }
        }
        return result
    }

    @Test
    fun ponAndChiValidation() {
        assertNull(Meld.problem(MeldType.PON, listOf(Tile.P5, Tile.P0, Tile.P5)))
        assertNotNull(Meld.problem(MeldType.PON, listOf(Tile.P5, Tile.P6, Tile.P5)))
        assertNull(Meld.problem(MeldType.CHI, listOf(Tile.S5, Tile.S3, Tile.S4)))
        assertNull(Meld.problem(MeldType.CHI, listOf(Tile.M4, Tile.M0, Tile.M6)))
        assertNotNull(Meld.problem(MeldType.CHI, listOf(Tile.S3, Tile.S4, Tile.P5)))
        assertNotNull(Meld.problem(MeldType.CHI, listOf(Tile.EAST, Tile.SOUTH, Tile.WEST)))
        assertNotNull(Meld.problem(MeldType.CHI, listOf(Tile.S1, Tile.S3, Tile.S5)))
    }

    @Test
    fun kanAcceptsOnlyTheVisibleTiles() {
        assertNull(Meld.problem(MeldType.CLOSED_KAN, listOf(Tile.CHUN, Tile.CHUN)))
        assertNull(Meld.problem(MeldType.OPEN_KAN, List(4) { Tile.M9 }))
        assertNotNull(Meld.problem(MeldType.CLOSED_KAN, listOf(Tile.CHUN, Tile.HAKU)))
        assertEquals(2, Meld(MeldType.CLOSED_KAN, listOf(0, 1)).hiddenCopies)
        assertEquals(0, Meld(MeldType.PON, listOf(0, 1, 2)).hiddenCopies)
    }

    @Test
    fun closedKanKeepsTheHandClosed() {
        val closed = HandContext(melds = listOf(Meld(MeldType.CLOSED_KAN, listOf(0, 1))))
        assertFalse(closed.isOpen)
        val open = HandContext(melds = listOf(Meld(MeldType.PON, listOf(0, 1, 2))))
        assertTrue(open.isOpen)
    }

    @Test
    fun callingClearsRiichiIppatsuAndUraDora() {
        val context = HandContext(
            riichi = Riichi.RIICHI,
            isIppatsu = true,
            uraDoraIndicators = listOf(Tile.P4),
            melds = listOf(Meld(MeldType.PON, listOf(0, 1, 2))),
        ).sanitized()

        assertEquals(Riichi.NONE, context.riichi)
        assertFalse(context.isIppatsu)
        assertTrue(context.uraDoraIndicators.isEmpty())
    }

    @Test
    fun switchingToRonClearsTsumoOnlyFlags() {
        val tsumo = HandContext(
            isTsumo = true,
            seatWind = Wind.EAST,
            melds = listOf(Meld(MeldType.CLOSED_KAN, listOf(0, 1))),
            isRinshan = true,
        ).sanitized()
        assertTrue(tsumo.isRinshan)

        val ron = tsumo.copy(isTsumo = false).sanitized()
        assertFalse(ron.isRinshan)
    }

    @Test
    fun firstTurnWinsDependOnSeat() {
        val dealer = HandContext(isTsumo = true, seatWind = Wind.EAST)
        assertTrue(dealer.canTenhou)
        assertFalse(dealer.canChiihou)

        val south = HandContext(isTsumo = true, seatWind = Wind.SOUTH)
        assertFalse(south.canTenhou)
        assertTrue(south.canChiihou)
        assertFalse(south.copy(isTsumo = false).canChiihou)
        assertTrue(south.copy(isTsumo = false).canRenhou)

        assertFalse(dealer.copy(isTenhou = true, seatWind = Wind.WEST).sanitized().isTenhou)
    }

    @Test
    fun winningTileInsideAMeldIsCleared() {
        val context = HandContext(winIndex = 1, melds = listOf(Meld(MeldType.PON, listOf(0, 1, 2)))).sanitized()
        assertNull(context.winIndex)
    }

    @Test
    fun countsFaceDownKanTilesTowardTheHand() {
        // A closed Chun kan with two tiles showing: 13 visible + 2 face-down = 15, right for one kan.
        val hand = tiles("77z234m567p88s345s")
        val context = HandContext(melds = listOf(Meld(MeldType.CLOSED_KAN, listOf(0, 1))))
        assertTrue(context.problems(hand).isEmpty())

        val missingKan = HandContext()
        assertEquals(1, missingKan.problems(hand).size)
    }

    @Test
    fun countsDoraIndicatorsAgainstTileLimits() {
        val hand = tiles("111m234m567p345s66s")
        val context = HandContext(doraIndicators = listOf(Tile.M1, Tile.M1))
        assertTrue(context.problems(hand).any { it.startsWith("Too many 1m") })
    }

    @Test
    fun doraIndicatorWrapsAround() {
        assertEquals(Tile.M1, HandContext.doraOf(Tile.M9))
        assertEquals(Tile.P6, HandContext.doraOf(Tile.P0))
        assertEquals(Tile.EAST, HandContext.doraOf(Tile.NORTH))
        assertEquals(Tile.SOUTH, HandContext.doraOf(Tile.EAST))
        assertEquals(Tile.HAKU, HandContext.doraOf(Tile.CHUN))
        assertEquals(Tile.HATSU, HandContext.doraOf(Tile.HAKU))
    }
}
