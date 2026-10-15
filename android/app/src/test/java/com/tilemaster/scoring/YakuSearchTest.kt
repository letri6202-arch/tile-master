package com.tilemaster.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YakuSearchTest {

    private val all = YakuCatalog.entries

    private fun names(query: String) = YakuSearch.search(all, query).map { it.name }

    @Test
    fun blankQueryReturnsEverythingInCatalogOrder() {
        assertEquals(all, YakuSearch.search(all, ""))
        assertEquals(all, YakuSearch.search(all, "   "))
    }

    @Test
    fun partialNameFindsTheYaku() {
        assertEquals("Tanyao", names("anyao").first())
    }

    @Test
    fun caseAndSpacingAreIgnored() {
        assertEquals("Tanyao", names("  TANYAO ").first())
        assertEquals("San Ankou", names("sanankou").first())
        assertEquals("San Ankou", names("san-ankou").first())
    }

    @Test
    fun prefixOutranksSubstring() {
        // "ankou" appears inside San Ankou and Suu Ankou; "tan" starts Tanyao but is only inside others.
        assertEquals("Tanyao", names("tan").first())
    }

    @Test
    fun skippedLettersStillMatch() {
        assertTrue("Tanyao" in names("tnyo"))
    }

    @Test
    fun typosStillMatch() {
        assertEquals("Pinfu", names("pinfoo").first())
        assertEquals("Tanyao", names("tanyou").first())
    }

    @Test
    fun sharedPrefixReturnsEveryMatch() {
        val result = names("sanshoku")
        assertTrue("Sanshoku Doujun" in result)
        assertTrue("Sanshoku Doukou" in result)
    }

    @Test
    fun plainEnglishMeaningMatches() {
        assertEquals("Tanyao", names("all simples").first())
    }

    @Test
    fun gibberishFindsNothing() {
        assertTrue(names("xyzq").isEmpty())
    }

    @Test
    fun shortQueriesDoNotMatchLoosely() {
        // Two letters must be a real prefix or substring, not scattered letters.
        assertTrue(names("zq").isEmpty())
    }
}
