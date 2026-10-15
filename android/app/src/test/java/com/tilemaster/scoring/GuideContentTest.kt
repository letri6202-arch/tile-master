package com.tilemaster.scoring

import org.junit.Assert.assertTrue
import org.junit.Test

class GuideContentTest {

    private val guides = mapOf("rule book" to RuleBook.sections, "point guide" to PointGuide.sections)

    @Test
    fun everyTileExampleParses() {
        // parseTiles throws on an unknown tile, which would crash the page when it's opened.
        guides.values.flatten().flatMap { it.cards }.flatMap { it.examples }.forEach { parseTiles(it.tiles) }
    }

    @Test
    fun everyCardHasContentAndTablesAreRectangular() {
        for ((name, sections) in guides) for (card in sections.flatMap { it.cards }) {
            val hasContent = card.body != null || card.table != null || card.bullets.isNotEmpty() || card.examples.isNotEmpty()
            assertTrue("$name card '${card.title}' is empty", hasContent)
            card.table?.let { t ->
                val width = t.header?.size ?: t.rows.first().size
                assertTrue("$name card '${card.title}' has ragged rows", t.rows.all { it.size == width })
            }
        }
    }

    @Test
    fun cardKeysAreUniqueWithinEachGuide() {
        // The pages use "section/card" as list keys, and duplicate keys crash a LazyColumn.
        for ((name, sections) in guides) {
            val keys = sections.flatMap { s -> s.cards.map { "${s.title}/${it.title}" } }
            assertTrue("$name has duplicate card keys", keys.size == keys.toSet().size)
            assertTrue("$name has duplicate section titles", sections.size == sections.map { it.title }.toSet().size)
        }
    }
}
