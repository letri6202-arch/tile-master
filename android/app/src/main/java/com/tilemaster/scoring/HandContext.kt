package com.tilemaster.scoring

import com.tilemaster.model.Suit
import com.tilemaster.model.Tile

enum class Wind(val code: String, val label: String, val tile: Tile) {
    EAST("E", "East", Tile.EAST),
    SOUTH("S", "South", Tile.SOUTH),
    WEST("W", "West", Tile.WEST),
    NORTH("N", "North", Tile.NORTH),
}

enum class MeldType(val label: String, val isKan: Boolean) {
    CHI("Chi", false),
    PON("Pon", false),
    OPEN_KAN("Open kan", true),
    CLOSED_KAN("Closed kan", true),
}

/**
 * A called set or kan, as positions in the scanned hand. A kan's face-down tiles can't be scanned,
 * so a kan may list fewer than 4 positions; [hiddenCopies] are added back when scoring.
 */
data class Meld(val type: MeldType, val indices: List<Int>) {
    val hiddenCopies: Int get() = if (type.isKan) 4 - indices.size else 0

    companion object {
        /** Why [tiles] can't form a [type], or null if they can. */
        fun problem(type: MeldType, tiles: List<Tile>): String? {
            val ranks = tiles.map { it.suit to it.rank }
            return when (type) {
                MeldType.PON ->
                    if (tiles.size != 3 || ranks.distinct().size != 1) "A pon is 3 identical tiles." else null
                MeldType.CHI -> {
                    val sorted = tiles.sortedBy { it.rank }
                    val isRun = tiles.size == 3 &&
                        tiles.all { it.suit == tiles[0].suit && it.suit != Suit.HONOR } &&
                        sorted.zipWithNext().all { (a, b) -> b.rank == a.rank + 1 }
                    if (isRun) null else "A chi is 3 tiles in a row of one suit, like 3-4-5 bamboo."
                }
                MeldType.OPEN_KAN, MeldType.CLOSED_KAN ->
                    if (tiles.size !in 1..4 || ranks.distinct().size != 1) {
                        "Select the visible tiles of one kan (they must match)."
                    } else {
                        null
                    }
            }
        }
    }
}

enum class Riichi(val label: String) {
    NONE("No riichi"),
    RIICHI("Riichi"),
    DOUBLE("Double riichi"),
}

/** Most dora indicators a table can show: the first one plus one per kan. */
const val MAX_DORA_INDICATORS = 5

/** Everything about one winning hand besides its tiles, as answered in the question flow. */
data class HandContext(
    val melds: List<Meld> = emptyList(),
    val winIndex: Int? = null,
    val isTsumo: Boolean? = null,
    val seatWind: Wind? = null,
    val roundWind: Wind = Wind.EAST,
    val riichi: Riichi = Riichi.NONE,
    val isIppatsu: Boolean = false,
    val doraIndicators: List<Tile> = emptyList(),
    val uraDoraIndicators: List<Tile> = emptyList(),
    val isRinshan: Boolean = false,
    val isChankan: Boolean = false,
    val isHaitei: Boolean = false,
    val isHoutei: Boolean = false,
    val isTenhou: Boolean = false,
    val isChiihou: Boolean = false,
    val isRenhou: Boolean = false,
    val honba: Int = 0,
    val riichiSticks: Int = 0,
) {
    /** Called melds open the hand; a closed kan doesn't. */
    val isOpen: Boolean get() = melds.any { it.type != MeldType.CLOSED_KAN }

    val isDealer: Boolean get() = seatWind == Wind.EAST

    val kanCount: Int get() = melds.count { it.type.isKan }

    val meldIndices: Set<Int> get() = melds.flatMapTo(mutableSetOf()) { it.indices }

    /** Only the special-situation flags that fit the other answers. */
    val canRinshan: Boolean get() = isTsumo == true && kanCount > 0
    val canHaitei: Boolean get() = isTsumo == true
    val canChankan: Boolean get() = isTsumo == false
    val canHoutei: Boolean get() = isTsumo == false
    val canTenhou: Boolean get() = isTsumo == true && isDealer && melds.isEmpty()
    val canChiihou: Boolean get() = isTsumo == true && seatWind != null && !isDealer && melds.isEmpty()
    val canRenhou: Boolean get() = isTsumo == false && seatWind != null && !isDealer && melds.isEmpty()

    /**
     * Clears answers that an earlier change made impossible, e.g. riichi after calling a pon, or
     * rinshan after switching to ron. Call after every edit so the state stays scoreable.
     */
    fun sanitized(): HandContext {
        val riichi = if (isOpen) Riichi.NONE else riichi
        val hasRiichi = riichi != Riichi.NONE
        val winIndex = winIndex?.takeUnless { it in meldIndices }
        val next = copy(
            winIndex = winIndex,
            riichi = riichi,
            isIppatsu = isIppatsu && hasRiichi,
            uraDoraIndicators = if (hasRiichi) uraDoraIndicators else emptyList(),
            honba = honba.coerceAtLeast(0),
            riichiSticks = riichiSticks.coerceAtLeast(0),
        )
        val rinshan = isRinshan && next.canRinshan
        val chankan = isChankan && next.canChankan
        return next.copy(
            isRinshan = rinshan,
            isHaitei = isHaitei && next.canHaitei && !rinshan,
            isChankan = chankan,
            isHoutei = isHoutei && next.canHoutei && !chankan,
            isTenhou = isTenhou && next.canTenhou,
            isChiihou = isChiihou && next.canChiihou,
            isRenhou = isRenhou && next.canRenhou,
        )
    }

    /**
     * Problems with the hand once melds are known: tile count (14, plus one per kan) and copies
     * (at most 4 of a tile and 1 red five per suit, counting dora indicators on the table).
     */
    fun problems(tiles: List<Tile>): List<String> {
        val problems = mutableListOf<String>()
        val hidden = melds.sumOf { it.hiddenCopies }
        val total = tiles.size + hidden
        val expected = 14 + kanCount
        if (total != expected) {
            problems += "The hand has $total tiles; with ${kanCount.kans()} it needs $expected."
        }
        val hiddenTiles = melds.flatMap { meld ->
            List(meld.hiddenCopies) { plainTile(tiles[meld.indices.first()]) }
        }
        val allTiles = tiles + hiddenTiles + doraIndicators + uraDoraIndicators
        allTiles.groupBy { it.suit to it.rank }.forEach { (key, group) ->
            if (group.size > 4) {
                problems += "Too many ${key.second}${key.first.code} between the hand and dora indicators: ${group.size} (max 4)."
            }
        }
        allTiles.filter { it.isRedFive }.groupBy { it }.forEach { (tile, group) ->
            if (group.size > 1) problems += "More than one ${tile.displayName}."
        }
        return problems
    }

    companion object {
        /** A plain tile of the same kind: red fives become normal fives. */
        fun plainTile(tile: Tile): Tile =
            if (tile.isRedFive) Tile.fromId("5${tile.suit.code}")!! else tile

        /** The dora an indicator points to: the next tile, wrapping 9→1, North→East, Chun→Haku. */
        fun doraOf(indicator: Tile): Tile {
            val rank = indicator.rank
            val next = when {
                indicator.suit != Suit.HONOR -> if (rank == 9) 1 else rank + 1
                rank <= 4 -> if (rank == 4) 1 else rank + 1
                else -> if (rank == 7) 5 else rank + 1
            }
            return Tile.fromId("$next${indicator.suit.code}")!!
        }

        private fun Int.kans() = when (this) {
            0 -> "no kans"
            1 -> "1 kan"
            else -> "$this kans"
        }
    }
}
