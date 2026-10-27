package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickerTagTest {
    @Test fun includedMissionsReadIncluded() {
        for (id in DemoSet.FREE_DEMO_IDS) {
            assertEquals(id, PickerTag.INCLUDED, PickerTag.proTag(snap(id = id, net = NOW - 30 * 24 * HOUR), historic = true, now = NOW))
            assertEquals(id, PickerTag.INCLUDED, PickerTag.proTag(snap(id = id), historic = false, now = NOW))
        }
    }

    @Test fun realStarshipFlight13ReadsIncluded() = assertEquals(
        PickerTag.INCLUDED,
        PickerTag.proTag(snap(id = "ll2-f13", provider = "SpaceX", rocket = "Starship", mission = "Flight 13", net = NOW - 10 * 24 * HOUR), true, NOW)
    )

    @Test fun otherBuiltInMissionsReadPro() {
        for (id in listOf("demo-spacex-f9", "demo-spacex-crew", "demo-spacex-fail", "demo-generic", "demo-blueorigin", "demo-soyuz-inflight"))
            assertEquals(id, PickerTag.PRO, PickerTag.proTag(snap(id = id), historic = true, now = NOW))
    }

    @Test fun realPastHistoricRowReadsProEvenForProOwners() =
        assertEquals(PickerTag.PRO, PickerTag.proTag(snap(net = NOW - 5 * 24 * HOUR), historic = true, now = NOW))

    @Test fun currentListKeepsNormalTags() {
        assertNull(PickerTag.proTag(snap(net = NOW + 2 * HOUR, abbrev = "Go", status = "Go for Launch"), historic = false, now = NOW))
        assertNull(PickerTag.proTag(snap(net = NOW - 5 * 24 * HOUR), historic = false, now = NOW))
    }

    @Test fun holdInHistoricKeepsHoldTag() =
        assertNull(PickerTag.proTag(snap(net = NOW - 10 * MIN, abbrev = "Hold", status = "On Hold"), historic = true, now = NOW))

    @Test fun launchInsideLiveWindowIsNeverAProRow() {
        assertNull(PickerTag.proTag(snap(net = NOW - 30 * MIN), historic = true, now = NOW))
        assertNull(PickerTag.proTag(snap(net = NOW - 20 * MIN, abbrev = "Failure", status = "Launch Failure"), historic = true, now = NOW))
    }
}
