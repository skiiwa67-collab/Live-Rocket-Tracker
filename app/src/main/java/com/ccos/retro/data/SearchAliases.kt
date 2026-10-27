package com.ccos.retro.data

/**
 * v1.1 copy (Da Vinci + erratum, Chris 10-27): words that list the built-in missions in historic search.
 * "demo" stays a silent alias for old habits and is never shown; featured / included / pro are the visible words.
 * Whole-word match, so "proton" or "demolition" do not trigger it.
 */
object SearchAliases {
    val CATALOG_WORDS: Set<String> = setOf("demo", "featured", "included", "pro")

    private val SPLIT = Regex("\\s+")

    private fun words(q: String): List<String> =
        q.trim().lowercase().split(SPLIT).filter { it.isNotBlank() }

    /** True when the query asks for the built-in missions. */
    fun wantsCatalog(q: String): Boolean = words(q).any { it in CATALOG_WORDS }

    /** True when the query is only alias words (no real search terms to send to LL2). */
    fun isAliasOnly(q: String): Boolean {
        val w = words(q)
        return w.isNotEmpty() && w.all { it in CATALOG_WORDS }
    }

    /** Query words with the alias words removed, for token matching. */
    fun searchTokens(q: String): List<String> = words(q).filter { it !in CATALOG_WORDS }
}
