package com.tilemaster.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossarySearchTest {

    private val all = GlossaryCatalog.terms

    private fun names(query: String) = GlossarySearch.search(all, query).map { it.term }

    @Test
    fun termsAreUniqueAndExamplesParse() {
        // Terms are used as list keys, and a duplicate would crash the page.
        assertEquals(all.size, all.map { it.term }.toSet().size)
        all.forEach { it.exampleGroups }
    }

    @Test
    fun blankQueryReturnsEverything() {
        assertEquals(all, GlossarySearch.search(all, " "))
    }

    @Test
    fun japaneseNamesFindTheTerm() {
        assertEquals("Closed wait", names("kanchan").first())
        assertEquals("Two-sided wait", names("ryanmen").first())
        assertEquals("Dealer", names("oya").first())
    }

    @Test
    fun partialAndMisspelledTermsMatch() {
        assertEquals("Furiten", names("uriten").first())
        assertEquals("Tenpai", names("tenpei").first())
        assertTrue("Dora indicator" in names("indicater"))
    }

    @Test
    fun meaningsMatch() {
        assertTrue("Tsumo" in names("own draw"))
    }

    @Test
    fun gibberishFindsNothing() {
        assertTrue(names("xyzq").isEmpty())
    }
}
