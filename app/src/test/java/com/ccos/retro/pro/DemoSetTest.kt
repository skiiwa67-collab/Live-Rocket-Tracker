package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSetTest {
    @Test fun allowlistIsF13PlusCountries() = assertEquals(
        setOf("demo-starship-f13", "demo-nasa-sls", "demo-casc-lm", "demo-soyuz", "demo-electron-inflight",
            "demo-esa-ariane", "demo-jaxa-h3", "demo-isro-lvm3"),
        DemoSet.FREE_DEMO_IDS
    )

    @Test fun electronCountsAsNz() =
        assertEquals("demo-electron-inflight", DemoSet.COUNTRY_DEMOS.first { it.country == "NZ" }.demoId)

    @Test fun europeJapanIndiaUseExistingDemos() {
        assertEquals("demo-esa-ariane", DemoSet.COUNTRY_DEMOS.first { it.country == "EU" }.demoId)
        assertEquals("demo-jaxa-h3", DemoSet.COUNTRY_DEMOS.first { it.country == "JP" }.demoId)
        assertEquals("demo-isro-lvm3", DemoSet.COUNTRY_DEMOS.first { it.country == "IN" }.demoId)
        for (id in listOf("demo-esa-ariane", "demo-jaxa-h3", "demo-isro-lvm3")) assertTrue(id, DemoSet.isFreeDemo(snap(id = id)))
    }

    @Test fun oneDemoPerCountryNoTodosLeft() {
        assertEquals(listOf("US", "CN", "RU", "NZ", "EU", "JP", "IN"), DemoSet.COUNTRY_DEMOS.map { it.country })
        for (c in DemoSet.COUNTRY_DEMOS) assertNotNull(c.country, c.demoId)
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
