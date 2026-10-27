package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSetTest {
    // Chris 8:22: samples are ALWAYS real launches. Only Starship F13 has a complete real tape bundled.
    @Test fun allowlistIsRealF13Only() = assertEquals(setOf("demo-starship-f13"), DemoSet.FREE_DEMO_IDS)

    @Test fun madeUpCountrySamplesAreGoneAndPro() {
        for (id in listOf("demo-nasa-sls", "demo-casc-lm", "demo-soyuz", "demo-electron-inflight",
            "demo-esa-ariane", "demo-jaxa-h3", "demo-isro-lvm3")) assertFalse(id, DemoSet.isFreeDemo(snap(id = id)))
    }

    @Test fun countriesWithoutRealDataAreSkippedNotFaked() {
        assertEquals(listOf("US", "CN", "RU", "NZ", "EU", "JP", "IN"), DemoSet.COUNTRY_DEMOS.map { it.country })
        for (c in DemoSet.COUNTRY_DEMOS) {
            assertNull(c.country, c.demoId)
            assertTrue(c.country, c.note.isNotBlank())
        }
    }

    @Test fun otherSyntheticDemosAreNotFree() {
        for (id in listOf("demo-spacex-f9", "demo-spacex-inflight", "demo-spacex-crew", "demo-spacex-fail", "demo-soyuz-inflight", "demo-generic", "demo-blueorigin"))
            assertFalse(id, DemoSet.isFreeDemo(snap(id = id)))
    }

    @Test fun realStarshipF13Detected() {
        val real = snap(id = "ll2-f13", rocket = "Starship", mission = "Flight 13", name = "Starship | Flight 13")
        assertTrue(DemoSet.isRealStarshipF13(real)); assertTrue(DemoSet.isFreeDemo(real))
        assertFalse(DemoSet.isRealStarshipF13(snap(id = "ll2-f12", rocket = "Starship", mission = "Flight 12", name = "Starship | Flight 12")))
        assertFalse(DemoSet.isRealStarshipF13(snap(id = "ll2-f130", rocket = "Starship", mission = "Flight 130", name = "Starship | Flight 130")))
        assertFalse(DemoSet.isRealStarshipF13(snap(id = "demo-starship-f13", rocket = "Starship", mission = "Flight 13")))
    }

    @Test fun f13PrefersRealReplayElseBuiltIn() {
        val builtIn = snap(id = "demo-starship-f13", rocket = "Starship", mission = "Flight 13")
        val real = snap(id = "ll2-f13", rocket = "Starship", mission = "Flight 13")
        assertEquals("ll2-f13", DemoSet.starshipF13(listOf(snap(), real), builtIn)?.id)
        assertEquals("demo-starship-f13", DemoSet.starshipF13(listOf(snap()), builtIn)?.id)
    }

    @Test fun ordinaryLaunchNotDemo() = assertFalse(DemoSet.isFreeDemo(snap()))
}
