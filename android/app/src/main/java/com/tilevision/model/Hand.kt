package com.tilevision.model

import com.tilevision.detection.Detection

object Hand {
    /** A closed winning hand without kans. Each kan adds one tile. */
    const val STANDARD_SIZE = 14

    /** Tiles laid out in a single row, read left to right. */
    fun orderLeftToRight(detections: List<Detection>): List<Detection> =
        detections.sortedBy { it.box.centerX }

    /**
     * Compact Riichi notation, e.g. `123m406p789s11122z`. Tiles are grouped by suit and sorted;
     * a red five is written as `0` in the position of its 5.
     */
    fun toNotation(tiles: List<Tile>): String = buildString {
        for (suit in Suit.entries) {
            val inSuit = tiles
                .filter { it.suit == suit }
                .sortedWith(compareBy<Tile> { it.rank }.thenBy { it.isRedFive })
            if (inSuit.isNotEmpty()) {
                inSuit.forEach { append(it.id[0]) }
                append(suit.code)
            }
        }
    }

    /** Human-readable problems that suggest the detection missed or invented tiles. */
    fun problems(tiles: List<Tile>): List<String> {
        val problems = mutableListOf<String>()
        if (tiles.size != STANDARD_SIZE) {
            problems += "Found ${tiles.size} tiles; a winning hand has $STANDARD_SIZE (plus one per kan)."
        }
        tiles.groupBy { it.suit to it.rank }.forEach { (key, group) ->
            if (group.size > 4) {
                problems += "Too many ${key.second}${key.first.code}: ${group.size} (max 4)."
            }
        }
        tiles.filter { it.isRedFive }.groupBy { it }.forEach { (tile, group) ->
            if (group.size > 1) problems += "More than one ${tile.displayName}."
        }
        return problems
    }
}
