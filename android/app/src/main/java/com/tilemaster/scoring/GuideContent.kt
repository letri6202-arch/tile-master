package com.tilemaster.scoring

/**
 * A small table: an optional [header] row, then [rows] whose first column is the label.
 * Numeric tables right-align their value columns; set [textual] for columns of sentences, which
 * read better left-aligned with the second column given the most room.
 */
data class GuideTable(
    val header: List<String>?,
    val rows: List<List<String>>,
    val textual: Boolean = false,
)

/** Tiles shown on a card, in standard notation (see [parseTiles]), with a caption above them. */
data class TileExample(val caption: String, val tiles: String)

/** One card on a guide page. Every part is optional except the title. */
data class GuideCard(
    val title: String,
    val body: String? = null,
    /** A line of math shown as a highlighted chip, like `fu × 2^(han + 2)`. */
    val formula: String? = null,
    val table: GuideTable? = null,
    val notes: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val examples: List<TileExample> = emptyList(),
)

data class GuideSection(val title: String, val blurb: String, val cards: List<GuideCard>)
