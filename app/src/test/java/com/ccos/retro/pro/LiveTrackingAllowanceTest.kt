package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chris 07:51/07:53 CT: live launches never lock; free users get one hour of tracking from actual liftoff. */
class LiveTrackingAllowanceTest {
    // B4: gates are off by default until billing is real; these tests exercise the gated (forced-on) state.
    @org.junit.Before fun gatesOn() { ProConfig.debugForceGates = true }
    @org.junit.After fun gatesReset() { ProConfig.debugForceGates = false }

    private fun inFlight(liftoffAgo: Long) = snap(net = NOW - liftoffAgo, abbrev = "In Flight", status = "Launch in Flight")
    private val launchAndJump = ProHook.values().filter { it.kind != ProHook.Kind.THEME }

    @Test fun freeAtTPlus59AfterLiftoffNoCardNotLocked() {
        val l = inFlight(59 * MIN); val store = MemProStore()
        assertFalse(LiveTrackingAllowance.isUsedUp(l, NOW))
        assertEquals(MIN, LiveTrackingAllowance.remainingMs(l, NOW))
        assertFalse(LiveTrackingAllowance.shouldShowCard(l, false, store, NOW))
        assertFalse(ProGate.isLocked(l, false, NOW))
    }

    @Test fun atTPlus61CardShowsOncePerLaunchAndNothingLocks() {
        val l = inFlight(61 * MIN); val store = MemProStore()
        assertTrue(LiveTrackingAllowance.isUsedUp(l, NOW))
        assertTrue(LiveTrackingAllowance.shouldShowCard(l, false, store, NOW))
        LiveTrackingAllowance.markShown(store, l)
        assertFalse("once per launch", LiveTrackingAllowance.shouldShowCard(l, false, store, NOW + 10 * MIN))
        assertTrue("next launch gets its own card", LiveTrackingAllowance.shouldShowCard(inFlight(61 * MIN).copy(id = "ll2-real-2"), false, store, NOW))
        assertFalse(ProGate.isLocked(l, false, NOW))
        for (h in launchAndJump) assertTrue(h.name, ProPolicy.allows(h, false, launch = l, now = NOW))
    }

    @Test fun proAndGatesOffNeverSeeTheCard() {
        val l = inFlight(2 * HOUR)
        assertFalse(LiveTrackingAllowance.shouldShowCard(l, true, MemProStore(), NOW))
        assertFalse(LiveTrackingAllowance.shouldShowCard(l, false, MemProStore(), NOW, gatesOn = false))
    }

    @Test fun holdBeforeLiftoffDoesNotStartTheClock() {
        val held = snap(net = NOW - 3 * HOUR, abbrev = "Hold", status = "On Hold")
        assertNull(LiveTrackingAllowance.liftoffMs(held))
        assertEquals(LiveTrackingAllowance.FREE_MS, LiveTrackingAllowance.remainingMs(held, NOW))
        assertFalse(LiveTrackingAllowance.shouldShowCard(held, false, MemProStore(), NOW))
        // Lifts off later: LL2 moves net to the real T-0, so the full hour is still there.
        assertFalse(LiveTrackingAllowance.isUsedUp(inFlight(5 * MIN), NOW))
    }

    @Test fun slipPastNetWithoutLiftoffDoesNotStartTheClock() {
        val slipped = snap(net = NOW - 2 * HOUR, abbrev = "Go", status = "Go for Launch", webcastLive = true)
        assertNull(LiveTrackingAllowance.liftoffMs(slipped))
        assertFalse(LiveTrackingAllowance.isUsedUp(slipped, NOW))
        assertFalse(LiveTrackingAllowance.shouldShowCard(slipped, false, MemProStore(), NOW))
    }

    @Test fun launchWithNoLiftoffIsNeverLocked() {
        for (st in listOf("Go" to "Go for Launch", "Hold" to "On Hold", "TBD" to "To Be Determined")) {
            for (ago in listOf(0L, 61 * MIN, 5 * HOUR)) for (cast in listOf(false, true)) {
                val l = snap(net = NOW - ago, abbrev = st.first, status = st.second, webcastLive = cast)
                assertFalse("${st.first} T+${ago / MIN}m", ProGate.isLocked(l, false, NOW))
                assertTrue(PickerTag.proTag(l, true, NOW) == null || l.isHold())
            }
        }
    }

    @Test fun liveLaunchNeverLockedAtAnyTPlusEvenAfterAllowanceRunsOut() {
        for (ago in listOf(0L, 30 * MIN, 59 * MIN, 61 * MIN, 3 * HOUR, 12 * HOUR, 47 * HOUR)) {
            val l = inFlight(ago)
            assertFalse("T+${ago / MIN}m", ProGate.isLocked(l, false, NOW))
            assertTrue(LiveTrackingAllowance.isUsedUp(l, NOW) == (ago >= LiveTrackingAllowance.FREE_MS))
            assertTrue(PickerTag.proTag(l, true, NOW) == null)
        }
    }

    @Test fun finishedLaunchIsAReplayNotALiveSession() {
        val done = snap(net = NOW - 61 * MIN) // Success
        assertFalse(LiveTrackingAllowance.shouldShowCard(done, false, MemProStore(), NOW))
        assertTrue(ProGate.isLocked(done, false, NOW))
    }

    @Test fun cardCopyNeverSaysLocked() {
        val xml = listOf("src/main/res/values/strings_pro.xml", "app/src/main/res/values/strings_pro.xml")
            .map { java.io.File(it) }.first { it.exists() }.readText()
        val lines = xml.lines().filter { "pro_allowance_" in it && "<string" in it }
        assertEquals(5, lines.size) // B6: + note_next / note_none / note_short
        for (ln in lines) assertFalse(ln, ln.lowercase().contains("lock"))
    }
}
