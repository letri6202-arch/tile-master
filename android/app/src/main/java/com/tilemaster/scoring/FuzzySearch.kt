package com.tilemaster.scoring

/** The text a search can match for one item. [names] are the strongest; the rest rank below them. */
class SearchText(val names: List<String>, val meaning: String, val description: String)

/**
 * Forgiving search shared by the guide pages. Matches don't have to start at the beginning or be
 * spelled perfectly: "anyao" finds Tanyao, "tnyo" finds Tanyao and "pinfoo" finds Pinfu.
 */
object FuzzySearch {

    /** Items matching [query], best match first; ties keep their original order. A blank query returns everything. */
    fun <T> search(items: List<T>, query: String, textOf: (T) -> SearchText): List<T> {
        val q = normalize(query)
        if (q.isEmpty()) return items
        return items
            .mapNotNull { item -> score(textOf(item), q)?.let { item to it } }
            .sortedBy { it.second } // stable, so equal scores stay in original order
            .map { it.first }
    }

    /** Lower is better; null means no match. */
    private fun score(text: SearchText, q: String): Int? {
        val names = text.names.map(::normalize)
        val meaning = normalize(text.meaning)
        val fuzzy = q.length >= MIN_FUZZY
        val tolerance = typoTolerance(q)
        // Tiers are spaced 10 apart so the typo tier can rank closer spellings ahead of looser ones.
        if (names.any { it.startsWith(q) }) return 0
        if (names.any { q in it }) return 10
        if (fuzzy && names.any { isSubsequence(q, it) }) return 20
        if (tolerance > 0) {
            val distance = names.minOf { approximateDistance(q, it) }
            if (distance <= tolerance) return 30 + distance
        }
        return when {
            q in meaning -> 40
            fuzzy && isSubsequence(q, meaning) -> 50
            // Descriptions are long, so only exact substrings count there.
            fuzzy && q in normalize(text.description) -> 60
            else -> null
        }
    }

    /** Lowercase letters and digits only, so "San Ankou", "san-ankou" and "sanankou" agree. */
    private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    /** Shorter queries match too much once typos or gaps are allowed. */
    private const val MIN_FUZZY = 3

    private fun typoTolerance(q: String): Int = when {
        q.length >= 6 -> 2
        q.length >= 4 -> 1
        else -> 0
    }

    private fun isSubsequence(q: String, text: String): Boolean {
        var next = 0
        for (c in text) if (next < q.length && c == q[next]) next++
        return next == q.length
    }

    /** Smallest edit distance between [q] and any substring of [text] (Sellers' algorithm). */
    private fun approximateDistance(q: String, text: String): Int {
        var previous = IntArray(text.length + 1) // matching an empty query anywhere costs nothing
        for (i in 1..q.length) {
            val current = IntArray(text.length + 1)
            current[0] = i
            for (j in 1..text.length) {
                val substitute = previous[j - 1] + if (q[i - 1] == text[j - 1]) 0 else 1
                current[j] = minOf(substitute, previous[j] + 1, current[j - 1] + 1)
            }
            previous = current
        }
        return previous.min()
    }
}
