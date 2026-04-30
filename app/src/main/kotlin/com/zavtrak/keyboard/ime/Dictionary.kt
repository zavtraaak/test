package com.zavtrak.keyboard.ime

import kotlin.math.abs

/**
 * Tiny in-memory dictionary that produces word suggestions from a typed prefix.
 * Built-in word lists live in the source — keeps APK small and avoids assets.
 *
 * For real-world use, a frequency-weighted trie loaded from compressed assets
 * would be better; this is a clean MVP that performs surprisingly well for
 * short prefixes thanks to the small word set.
 */
class Dictionary(private val words: List<Pair<String, Int>>) {

    private val byPrefix: Map<Char, List<Pair<String, Int>>> =
        words.groupBy { it.first.firstOrNull() ?: ' ' }

    private val learned = HashMap<String, Int>()

    fun observeWord(word: String) {
        if (word.length < 2) return
        learned[word] = (learned[word] ?: 0) + 5
    }

    /** Top-N suggestions for the given (case-insensitive) prefix. */
    fun suggest(prefix: String, n: Int = 3): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val first = prefix[0].lowercaseChar()
        val pl = prefix.lowercase()
        val candidates = (byPrefix[first].orEmpty() + learned.entries.filter { it.key.startsWith(pl) }.map { it.key to it.value })
            .filter { it.first.startsWith(pl) }
            .sortedByDescending { (w, freq) ->
                // Boost shorter completions slightly for relevance
                freq - (w.length - prefix.length).coerceAtMost(20)
            }
        val seen = LinkedHashSet<String>()
        for ((w, _) in candidates) {
            if (seen.size >= n) break
            seen += w
        }
        return seen.toList()
    }

    /** Best autocorrect candidate by Levenshtein distance (≤ 2). null if none. */
    fun autocorrect(word: String): String? {
        if (word.length < 3) return null
        val lower = word.lowercase()
        if (contains(lower)) return null
        val first = lower[0]
        val pool = (byPrefix[first].orEmpty() + learned.entries.map { it.key to it.value })
            .map { it.first }
            .filter { abs(it.length - lower.length) <= 2 }
        val maxDistance = if (lower.length <= 4) 1 else 2
        var best: String? = null
        var bestDist = Int.MAX_VALUE
        for (cand in pool) {
            val d = lev(lower, cand, maxDistance)
            if (d in 0..maxDistance && d < bestDist) {
                bestDist = d
                best = cand
            }
        }
        return best
    }

    fun contains(word: String): Boolean {
        val w = word.lowercase()
        if (learned.containsKey(w)) return true
        val list = byPrefix[w.firstOrNull() ?: return false] ?: return false
        return list.any { it.first == w }
    }

    private fun lev(a: String, b: String, max: Int): Int {
        if (abs(a.length - b.length) > max) return max + 1
        val n = a.length; val m = b.length
        val prev = IntArray(m + 1) { it }
        val curr = IntArray(m + 1)
        for (i in 1..n) {
            curr[0] = i
            var rowMin = curr[0]
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
                if (curr[j] < rowMin) rowMin = curr[j]
            }
            if (rowMin > max) return max + 1
            System.arraycopy(curr, 0, prev, 0, m + 1)
        }
        return prev[m]
    }

    companion object {
        fun forLanguage(lang: String): Dictionary = when (lang) {
            "ru" -> Dictionary(WordsRu.list)
            else -> Dictionary(WordsEn.list)
        }
    }
}
