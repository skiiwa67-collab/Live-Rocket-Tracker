package com.ccos.retro.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Helios P2-B (Chris 8:22: no grandfathering): a saved launch that is now Pro falls back to F13 for a free user. */
class SavedProLaunchFallbackTest {
    private val m by lazy {
        listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
            .resolve("com/ccos/retro/module/RocketTelemetryModule.kt").readText()
    }

    @Test fun restoreRunsTheGate() {
        val resolve = m.substringAfter("fun resolveTracked(").take(700)
        assertTrue(resolve.contains("keepTrackedOrLastGood(now)\n            gateRestoredLaunch(now)"))
    }

    @Test fun gateFallsBackToF13Silently() {
        val body = m.substringAfter("internal fun gateRestoredLaunch(").substringBefore("\n    }\n")
        assertTrue(body.contains("ProGate.isLocked(saved, isProProvider(), now)"))
        assertTrue(body.contains("historicFallback()"))
        assertTrue(body.contains("prefs.telemetryLaunchId = f13.id"))
        assertFalse("no sheet on restore (no-badger)", body.contains("onProBlocked") || body.contains("proAllows("))
    }

    @Test fun whichSavedLaunchesFallBack() {
        val oldReplay = snap(id = "ll2-old", net = NOW - 30L * 24 * HOUR)
        assertTrue("saved past replay is Pro for free", ProGate.isLocked(oldReplay, false, NOW))
        assertFalse("Pro keeps it", ProGate.isLocked(oldReplay, true, NOW))
        assertFalse("upcoming is never gated", ProGate.isLocked(snap(net = NOW + HOUR, abbrev = "Go", status = "Go for Launch"), false, NOW))
        assertFalse("F13 itself never falls back",
            ProGate.isLocked(snap(id = DemoSet.STARSHIP_F13_DEMO_ID, net = NOW - 90L * 24 * HOUR), false, NOW))
    }
}
