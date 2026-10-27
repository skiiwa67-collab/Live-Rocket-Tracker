package com.ccos.retro.pro

import com.ccos.retro.data.LaunchWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gate + the existing NET+60 / 48h window systems it leans on (R-REGRESSION-TEST). */
class ProGateTest {
    @Test fun upcomingNeverLocked() =
        assertFalse(ProGate.isLocked(snap(net = NOW + 64 * HOUR, abbrev = "Go", status = "Go for Launch"), false, NOW))

    @Test fun liveGoAtTPlus30NeverLocked() =
        assertFalse(ProGate.isLocked(snap(net = NOW - 30 * MIN, abbrev = "Go", status = "Go for Launch"), false, NOW))

    @Test fun liveInFlightNeverLocked() =
        assertFalse(ProGate.isLocked(snap(net = NOW - 20 * MIN, abbrev = "In Flight", status = "Launch in Flight"), false, NOW))

    @Test fun justBeforeNetPlus60NotLocked() =
        assertFalse(ProGate.isLocked(snap(net = NOW - 59 * MIN), false, NOW))

    @Test fun atNetPlus60LockedForFree() =
        assertTrue(ProGate.isLocked(snap(net = NOW - LaunchWindow.WATCH_AFTER_NET_SEC * 1000L), false, NOW))

    @Test fun recentPastInside48hPickerIsLockedForFree() {
        val l = snap(net = NOW - 47 * HOUR)
        assertTrue(l.inPickerWindow(NOW)) // existing 48h picker unchanged
        assertTrue(ProGate.isLocked(l, false, NOW))
    }

    @Test fun oldHistoricLockedForFreeOpenForPro() {
        val l = snap(net = NOW - 30L * 24 * HOUR)
        assertTrue(ProGate.isLocked(l, false, NOW)); assertFalse(ProGate.isLocked(l, true, NOW))
    }

    @Test fun freeDemosNeverLocked() {
        for (id in DemoSet.FREE_DEMO_IDS) assertFalse(id, ProGate.isLocked(snap(id = id, net = NOW - 26L * 24 * HOUR), false, NOW))
        val realF13 = snap(id = "ll2-f13", rocket = "Starship", mission = "Flight 13", net = NOW - 26L * 24 * HOUR)
        assertFalse(ProGate.isLocked(realF13, false, NOW))
    }

    @Test fun nonListedSyntheticDemoLockedForFree() =
        assertTrue(ProGate.isLocked(snap(id = "demo-spacex-fail", net = NOW + HOUR), false, NOW))

    @Test fun jumpSkipFreeOnLiveBlockedOnLockedReplay() {
        assertTrue(ProGate.canJumpOrSkip(snap(net = NOW - 10 * MIN, abbrev = "Go", status = "Go for Launch"), false, NOW))
        assertFalse(ProGate.canJumpOrSkip(snap(net = NOW - 5 * HOUR), false, NOW))
        assertTrue(ProGate.canJumpOrSkip(snap(net = NOW - 5 * HOUR), true, NOW))
        assertTrue(ProGate.canJumpOrSkip(snap(id = "demo-soyuz", net = NOW - 5 * HOUR), false, NOW))
        assertTrue(ProGate.canJumpOrSkip(null, false, NOW))
    }

    @Test fun existingReplayableWindowUnchanged() {
        assertFalse(snap(net = NOW - 59 * MIN).isReplayable(NOW))
        assertTrue(snap(net = NOW - 61 * MIN).isReplayable(NOW))
        assertTrue(snap(id = "demo-generic", net = NOW + HOUR).isReplayable(NOW))
        assertFalse(snap(net = NOW - 49 * HOUR).inPickerWindow(NOW))
    }
}
