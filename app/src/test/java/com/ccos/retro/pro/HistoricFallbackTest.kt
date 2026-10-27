package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Helios P2-A: the Historic fallback is an Included launch (Starship F13), never a Pro catalog entry. */
class HistoricFallbackTest {
    private val m by lazy {
        listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
            .resolve("com/ccos/retro/module/RocketTelemetryModule.kt").readText().replace("\r\n", "\n")
    }

    @Test fun noFallbackPicksTheFirstCatalogEntry() {
        assertFalse(m.contains("provider.demoCatalog.firstOrNull()"))
        assertTrue(Regex("""historicFallback\(\)""").findAll(m).count() >= 8)
    }

    @Test fun fallbackIsF13RealFirstThenBuiltIn() {
        val body = m.substringAfter("internal fun historicFallback()").substringBefore("\n    }\n")
        assertTrue(body.contains("DemoSet.starshipF13("))
        assertTrue(body.contains("DemoSet.STARSHIP_F13_DEMO_ID"))
        assertTrue(body.contains("DemoSet.isFreeDemo(it)"))
        assertFalse(body.contains("proAllows") || body.contains("onProBlocked"))
    }

    @Test fun f13IsIncludedAndReachableForFree() {
        val builtIn = snap(id = DemoSet.STARSHIP_F13_DEMO_ID, rocket = "Starship", mission = "Flight 13")
        val pick = DemoSet.starshipF13(listOf(snap(id = "ll2-other")), builtIn)!!
        assertEquals(DemoSet.STARSHIP_F13_DEMO_ID, pick.id)
        assertFalse(ProGate.isLocked(pick, false, NOW))
        val real = snap(id = "ll2-f13", rocket = "Starship", mission = "Flight 13", net = NOW - 90 * 24 * HOUR)
        assertEquals("ll2-f13", DemoSet.starshipF13(listOf(real), builtIn)?.id)
        assertFalse(ProGate.isLocked(real, false, NOW))
        assertTrue("the old fallback was Pro", ProGate.isLocked(snap(id = "demo-spacex-f9", net = NOW - HOUR * 3), false, NOW))
    }
}
