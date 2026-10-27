package com.ccos.retro.pro

import com.ccos.retro.data.LaunchDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** P1: fabricated built-in launches never reach release or any live/upcoming countdown. */
class FabricatedCatalogTest {
    private fun src(rel: String): File {
        val f = listOf(File("src/main/$rel"), File("app/src/main/$rel")).firstOrNull { it.exists() }
        return f ?: throw AssertionError("missing source file $rel")
    }

    private val providerSrc by lazy {
        src("java/com/ccos/retro/data/LaunchDataProvider.kt").readText().replace("\r\n", "\n")
    }

    @Test fun releasePathCatalogContainsNoFabricatedLaunches() {
        val ids = LaunchDataProvider.builtInCatalogIds(debug = false)
        for (id in LaunchDataProvider.FABRICATED_BUILTIN_IDS) {
            assertFalse("$id leaked into release catalog ids", id in ids)
        }
        assertEquals(setOf(DemoSet.STARSHIP_F13_DEMO_ID), ids)

        val release = LaunchDataProvider().buildDemoCatalog(debug = false, now = NOW)
        assertTrue(release.none { LaunchDataProvider.isFabricatedBuiltIn(it.id) })
        assertEquals(listOf(DemoSet.STARSHIP_F13_DEMO_ID), release.map { it.id })
        assertTrue(release.all { it.netMs == LaunchDataProvider.STARSHIP_F13_T0_MS })
    }

    @Test fun debugKeepsFabricatedAsDevDataButNeverAsLiveCountdown() {
        val debugIds = LaunchDataProvider.builtInCatalogIds(debug = true)
        for (id in LaunchDataProvider.FABRICATED_BUILTIN_IDS) assertTrue(id, id in debugIds)
        val debug = LaunchDataProvider().buildDemoCatalog(debug = true, now = NOW)
        val fabricated = debug.filter { LaunchDataProvider.isFabricatedBuiltIn(it.id) }
        assertEquals(LaunchDataProvider.FABRICATED_BUILTIN_IDS.size, fabricated.size)
        for (l in fabricated) {
            assertFalse(l.id, LaunchDataProvider.isEligibleForLiveUpcomingList(l))
            assertFalse(l.id, LaunchDataProvider.keepInLiveUpcomingList(l, NOW))
        }
        val f13 = debug.first { it.id == DemoSet.STARSHIP_F13_DEMO_ID }
        assertFalse("F13 is historic, not a live countdown", LaunchDataProvider.isEligibleForLiveUpcomingList(f13))
        assertTrue(LaunchDataProvider.isEligibleForLiveUpcomingList(snap(id = "ll2-real", net = NOW + HOUR)))
    }

    @Test fun liveUpcomingCallSitesRejectBuiltInCatalog() {
        val module = src("java/com/ccos/retro/module/RocketTelemetryModule.kt").readText().replace("\r\n", "\n")
        val sel = module.substringAfter("fun selectableLaunches(").substringBefore("\n    fun ")
        assertTrue(sel.contains("LaunchDataProvider.isEligibleForLiveUpcomingList"))
        assertTrue(sel.contains("LaunchDataProvider.keepInLiveUpcomingList"))
        val main = src("java/com/ccos/retro/ui/MainActivity.kt").readText().replace("\r\n", "\n")
        assertTrue(main.contains("LaunchDataProvider.keepInLiveUpcomingList"))
        val catalog = providerSrc.substringAfter("fun buildDemoCatalog(").substringBefore("\n    }\n")
        assertTrue("fabricated rows stay behind the debug gate", catalog.contains("if (!debug) return historic"))
        assertTrue(catalog.contains("DEV DATA"))
    }
}
