package com.tilemaster.scoring

/** Search over the yaku list. See [FuzzySearch] for how matches are ranked. */
object YakuSearch {

    fun search(entries: List<YakuEntry>, query: String): List<YakuEntry> =
        FuzzySearch.search(entries, query) { SearchText(listOf(it.name), it.shownMeaning, it.shownDescription) }
}
