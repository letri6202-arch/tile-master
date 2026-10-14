package com.tilemaster.scoring

import com.tilemaster.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionStepTest {

    private val closedHand = HandContext(isTsumo = false, seatWind = Wind.SOUTH)

    @Test
    fun closedHandWithoutRiichiSkipsUraDora() {
        assertEquals(
            listOf(
                QuestionStep.MELDS, QuestionStep.WINNING_TILE, QuestionStep.WIN_TYPE, QuestionStep.WINDS,
                QuestionStep.RIICHI, QuestionStep.DORA, QuestionStep.SPECIAL, QuestionStep.TABLE,
                QuestionStep.SUMMARY,
            ),
            QuestionStep.relevant(closedHand),
        )
    }

    @Test
    fun riichiAddsUraDora() {
        val riichi = closedHand.copy(riichi = Riichi.RIICHI)
        assertEquals(QuestionStep.URA_DORA, QuestionStep.next(QuestionStep.DORA, riichi))
        assertEquals(QuestionStep.SPECIAL, QuestionStep.next(QuestionStep.DORA, closedHand))
    }

    @Test
    fun openHandSkipsRiichi() {
        val open = closedHand.copy(melds = listOf(Meld(MeldType.PON, listOf(0, 1, 2))))
        assertFalse(QuestionStep.RIICHI.isRelevant(open))
        assertEquals(QuestionStep.DORA, QuestionStep.next(QuestionStep.WINDS, open))
        assertEquals(QuestionStep.WINDS, QuestionStep.previous(QuestionStep.DORA, open))
    }

    @Test
    fun firstAndLastStepsHaveNoNeighbours() {
        assertNull(QuestionStep.previous(QuestionStep.MELDS, closedHand))
        assertNull(QuestionStep.next(QuestionStep.SUMMARY, closedHand))
    }

    @Test
    fun requiredAnswersBlockProgress() {
        val tiles = List(14) { Tile.M1 }
        assertNotNull(QuestionStep.WIN_TYPE.blocker(HandContext(), tiles))
        assertNotNull(QuestionStep.WINDS.blocker(HandContext(), tiles))
        assertNotNull(QuestionStep.WINNING_TILE.blocker(HandContext(), tiles))
        assertNull(QuestionStep.WINNING_TILE.blocker(HandContext(winIndex = 13), tiles))
        assertNull(QuestionStep.MELDS.blocker(HandContext(), tiles))
    }
}
