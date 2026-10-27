package com.ccos.retro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAliasesTest {
    @Test fun everyAliasListsTheCatalog() {
        for (w in listOf("demo", "featured", "included", "pro", "Featured", " PRO ", "INCLUDED")) {
            assertTrue(w, SearchAliases.wantsCatalog(w))
            assertTrue(w, SearchAliases.isAliasOnly(w))
        }
    }

    @Test fun aliasesMatchWholeWordsOnly() {
        for (w in listOf("proton", "demolition", "featuredness", "Progress", "Falcon")) assertFalse(w, SearchAliases.wantsCatalog(w))
    }

    @Test fun aliasPlusTermsKeepsTheTerms() {
        assertTrue(SearchAliases.wantsCatalog("featured ariane"))
        assertFalse(SearchAliases.isAliasOnly("featured ariane"))
        assertEquals(listOf("ariane"), SearchAliases.searchTokens("Featured  Ariane"))
        assertEquals(listOf("h3"), SearchAliases.searchTokens("pro h3 demo"))
    }

    @Test fun blankIsNotAnAlias() {
        assertFalse(SearchAliases.wantsCatalog(""))
        assertFalse(SearchAliases.isAliasOnly("   "))
        assertTrue(SearchAliases.searchTokens("").isEmpty())
    }
}
