package com.tilemaster.scoring

/** Search over the glossary. Aliases such as the Japanese names count as strongly as the term itself. */
object GlossarySearch {

    fun search(terms: List<GlossaryTerm>, query: String): List<GlossaryTerm> =
        FuzzySearch.search(terms, query) { SearchText(listOf(it.term) + it.aliases, it.meaning, it.description) }
}
