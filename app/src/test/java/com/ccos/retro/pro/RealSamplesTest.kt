package com.ccos.retro.pro

import com.ccos.retro.data.LaunchDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** Chris 8:22: built-in samples are real past launches, never upcoming countdowns, never made up. */
class RealSamplesTest {
    private val catalog by lazy {
        val src = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
            .resolve("com/ccos/retro/data/LaunchDataProvider.kt").readText().replace("\r\n", "\n")
        src.substringAfter("fun buildDemoCatalog(").substringBefore("\n    }\n")
    }

    @Test fun f13UsesRealFixedT0() {
        assertEquals(Instant.parse("2026-07-24T22:51:00Z").toEpochMilli(), LaunchDataProvider.STARSHIP_F13_T0_MS)
        val f13 = catalog.substringAfter("id = \"demo-starship-f13\"").substringBefore("LaunchSnapshot(")
        assertTrue(f13.contains("netMs = STARSHIP_F13_T0_MS"))
        assertFalse("never now-relative", f13.contains("now"))
        assertTrue(f13.contains("statusAbbrev = \"Success\""))
    }

    @Test fun f13IsPastAndNeverCountsDown() {
        val f13 = snap(id = DemoSet.STARSHIP_F13_DEMO_ID, net = LaunchDataProvider.STARSHIP_F13_T0_MS,
            rocket = "Starship", mission = "Flight 13")
        val appStart = System.currentTimeMillis()
        assertTrue(f13.secondsToNet(appStart) < 0)
        assertTrue(f13.isReplayable(appStart))
    }

    @Test fun madeUpCountrySamplesRemoved() {
        for (id in listOf("demo-nasa-sls", "demo-casc-lm", "demo-soyuz\"", "demo-electron-inflight",
            "demo-esa-ariane", "demo-jaxa-h3", "demo-isro-lvm3")) assertFalse(id, catalog.contains("id = \"$id"))
        assertTrue(catalog.contains("id = \"demo-starship-f13\""))
    }

    @Test fun everyFreeSampleIsInTheCatalog() {
        for (id in DemoSet.FREE_DEMO_IDS) assertTrue(id, catalog.contains("id = \"$id\""))
    }
}
