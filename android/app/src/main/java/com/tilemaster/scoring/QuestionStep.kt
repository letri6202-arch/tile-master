package com.tilemaster.scoring

import com.tilemaster.model.Tile

/** The questions asked after a scan, in order. Steps that can't apply to the hand are skipped. */
enum class QuestionStep(val title: String) {
    MELDS("Did you call any tiles?"),
    WINNING_TILE("Which tile did you win on?"),
    WIN_TYPE("How did you win?"),
    WINDS("Seat and round wind"),
    RIICHI("Did you riichi?"),
    DORA("Dora indicators"),
    URA_DORA("Ura dora indicators"),
    SPECIAL("Anything special?"),
    TABLE("Honba and riichi sticks"),
    SUMMARY("Check and score"),
    ;

    fun isRelevant(context: HandContext): Boolean = when (this) {
        RIICHI -> !context.isOpen
        URA_DORA -> context.riichi != Riichi.NONE
        SPECIAL -> context.isTsumo != null
        else -> true
    }

    /** Why the player can't move on yet, or null if they can. */
    fun blocker(context: HandContext, tiles: List<Tile>): String? = when (this) {
        WINNING_TILE -> when (val index = context.winIndex) {
            null -> "Tap the tile you won on."
            in context.meldIndices -> "The winning tile can't be in a called meld."
            else -> if (index !in tiles.indices) "Tap the tile you won on." else null
        }
        WIN_TYPE -> if (context.isTsumo == null) "Pick tsumo or ron." else null
        WINDS -> if (context.seatWind == null) "Pick your seat wind." else null
        DORA, URA_DORA -> {
            val indicators = if (this == DORA) context.doraIndicators else context.uraDoraIndicators
            if (indicators.size > MAX_DORA_INDICATORS) "A table shows at most $MAX_DORA_INDICATORS." else null
        }
        SUMMARY -> context.problems(tiles).firstOrNull()
        else -> null
    }

    companion object {
        fun relevant(context: HandContext): List<QuestionStep> = entries.filter { it.isRelevant(context) }

        fun next(step: QuestionStep, context: HandContext): QuestionStep? =
            entries.drop(step.ordinal + 1).firstOrNull { it.isRelevant(context) }

        fun previous(step: QuestionStep, context: HandContext): QuestionStep? =
            entries.take(step.ordinal).lastOrNull { it.isRelevant(context) }
    }
}
