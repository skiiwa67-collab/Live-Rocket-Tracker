package com.ccos.retro.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** B1 regression (Skippy vc159): an unfinished launch in flight or streaming is never locked, at any T+. */
class ProGateLiveTest {
    // B4: gates are off by default until billing is real; these tests exercise the gated (forced-on) state.
    @org.junit.Before fun gatesOn() { ProConfig.debugForceGates = true }
    @org.junit.After fun gatesReset() { ProConfig.debugForceGates = false }

    private val launchAndJump = ProHook.values().filter { it.kind != ProHook.Kind.THEME }

    private fun assertOpenOnAllHooks(l: com.ccos.retro.data.LaunchSnapshot) {
        assertFalse(ProGate.isLocked(l, false, NOW))
        for (h in launchAndJump) assertTrue(h.name, ProPolicy.allows(h, false, launch = l, now = NOW))
    }

    @Test fun inFlightAtTPlus61MinNotLocked() =
        assertOpenOnAllHooks(snap(net = NOW - 61 * MIN, abbrev = "In Flight", status = "Launch in Flight"))

    @Test fun inFlightAtTPlus3hNotLocked() =
        assertOpenOnAllHooks(snap(net = NOW - 3 * HOUR, abbrev = "In Flight", status = "Launch in Flight"))

    @Test fun webcastLiveAtTPlus75MinNotLocked() =
        assertOpenOnAllHooks(snap(net = NOW - 75 * MIN, abbrev = "Go", status = "Go for Launch", webcastLive = true))

    @Test fun successAtTPlus61MinLockedUnlessGranted() {
        val l = snap(net = NOW - 61 * MIN, webcastLive = true) // Success + stale webcast flag: finished
        assertTrue(ProGate.isLocked(l, false, NOW))
        for (h in launchAndJump) assertFalse(h.name, ProPolicy.allows(h, false, launch = l, now = NOW))
        assertFalse(ProGate.isLocked(l, true, NOW))
    }

    @Test fun inFlightPastNetPlus60ReadsNoProTag() =
        assertNull(PickerTag.proTag(snap(net = NOW - 2 * HOUR, abbrev = "In Flight", status = "Launch in Flight"), true, NOW))

    @Test fun catalogEntriesKeepTheirTag() =
        assertTrue(ProGate.isLocked(snap(id = "demo-soyuz-inflight", net = NOW - 2 * HOUR, abbrev = "In Flight", status = "Launch in Flight"), false, NOW))
}
