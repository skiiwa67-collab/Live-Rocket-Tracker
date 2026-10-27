package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * B3 regression (Skippy vc159). The module can't be built on the plain JVM (AppPrefs needs a Context), so this
 * scans its source: internal theater jumps (page switch, bird change in resolve, select, historic roll) are
 * ungated, and only user entry points can notify onProBlocked.
 */
class ProInternalJumpTest {
    private val m: String = listOf("src/main/java", "app/src/main/java")
        .map { File(it, "com/ccos/retro/module/RocketTelemetryModule.kt") }.first { it.isFile }.readText()

    /** Body of the first `fun <name>(` up to the next member declaration at the same indent. */
    private fun body(name: String): String {
        val start = Regex("""\n    (override |private |internal )?fun $name\(""").find(m) ?: error("fun $name not found")
        val next = Regex("""\n    (override |private |internal )?(fun|val|var) """).find(m, start.range.last) ?: return m.substring(start.range.first)
        return m.substring(start.range.first, next.range.first)
    }

    private fun enclosingFun(at: Int): String =
        Regex("""\n    (?:override |private |internal )?fun (\w+)\(""").findAll(m.substring(0, at)).last().groupValues[1]

    @Test fun userJumpIsGatedThenDelegates() {
        val j = body("jumpTo")
        assertTrue(j.contains("proAllows(ProHook.JUMP"))
        assertTrue(j.contains("jumpToInternal("))
        assertFalse(body("jumpToInternal").contains("proAllows"))
    }

    @Test fun pageSwitchNeverGatesOrNotifies() {
        val b = body("onModuleButton")
        assertTrue("page switch jumps theater", b.contains("jumpToInternal(-30f)"))
        assertFalse(Regex("""(?<![\w.])jumpTo\(""").containsMatchIn(b))
        assertFalse(b.contains("proAllows") || b.contains("onProBlocked"))
    }

    @Test fun birdChangeInResolveUsesInternalJump() {
        val i = m.indexOf("prevId != null && t.id != prevId && simSecondsFromNet == null")
        assertTrue(i > 0)
        val after = m.substring(i, i + 300)
        assertTrue(after.contains("jumpToInternal(-30f)"))
        assertFalse(Regex("""(?<![\w.])jumpTo\(""").containsMatchIn(after))
    }

    @Test fun noInternalCallerUsesTheGatedJump() {
        val calls = Regex("""(?<![\w.])jumpTo\(""").findAll(m).map { it.range.first }
            .filterNot { m.substring(maxOf(0, it - 4), it) == "fun " }.toList()
        assertEquals("module must not call the user-gated jumpTo itself", emptyList<Int>(), calls)
    }

    @Test fun onlyUserEntryPointsNotify() {
        val userEntry = setOf("jumpTo", "skipEvent", "selectLaunch")
        val gateDef = Regex("""fun proAllows\(""").find(m)!!.range.first
        for (hit in Regex("""proAllows\(""").findAll(m)) {
            if (hit.range.first in gateDef..gateDef + 20) continue
            val call = m.substring(hit.range.first, m.indexOf(')', hit.range.first) + 1)
            val fn = enclosingFun(hit.range.first)
            if (call.contains("notify = false")) continue
            assertTrue("$fn notifies onProBlocked but is not a user entry point", fn in userEntry)
        }
        assertEquals(1, Regex("""onProBlocked\?\.invoke""").findAll(m).count())
    }
}
