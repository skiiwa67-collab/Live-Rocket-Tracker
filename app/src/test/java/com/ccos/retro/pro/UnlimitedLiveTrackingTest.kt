package com.ccos.retro.pro

import com.ccos.retro.data.LaunchWindow
import com.ccos.retro.module.LiveClockGate
import com.ccos.retro.module.RocketTelemetryModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Chris 10-27 07:59 CT: Pro users get unlimited live tracking past one hour; the old 60-minute stop started as a bug.
 * Root cause: stamp 63 capped In Flight / webcast_live active watch at NET + 60 min for everyone, so at T+60 a launch
 * still flying flipped to replayable, left the live wall clock and AUTO moved on. Fixed in LaunchSnapshot.isExtendedLive.
 */
class UnlimitedLiveTrackingTest {
    private fun inFlight(ago: Long) = snap(net = NOW - ago, abbrev = "In Flight", status = "Launch in Flight")
    private val t0 = NOW // pinned NET = liftoff

    @Before fun gatesOn() { ProConfig.debugForceGates = true }
    @After fun reset() { ProConfig.debugForceGates = false; LaunchWindow.unlimitedLive = true }

    private fun assertLiveFor(l: com.ccos.retro.data.LaunchSnapshot, label: String) {
        val tNow = -l.secondsToNet(NOW).toFloat()
        assertTrue("$label active watch", l.isActiveWatch(NOW))
        assertTrue("$label status live", l.isStatusLive(NOW))
        assertFalse("$label not a replay", l.isReplayable(NOW))
        assertTrue("$label on the live wall clock", LiveClockGate.isLiveWallClock(l, NOW, historicalMode = false))
        val a = LiveClockGate.liveWallSecondsFromNet(l, NOW, l.netMs)
        val b = LiveClockGate.liveWallSecondsFromNet(l, NOW + 10_000L, l.netMs)
        assertEquals("$label clock = wall minus trail", tNow - LiveClockGate.trailSec(l), a, 0.01f)
        assertEquals("$label clock advances, never stuck", 10f, b - a, 0.01f)
        assertTrue("$label jump", ProPolicy.allows(ProHook.JUMP, true, launch = l, now = NOW, gatesOn = true))
        assertTrue("$label skip", ProPolicy.allows(ProHook.SKIP_EVENT, true, launch = l, now = NOW, gatesOn = true))
        assertFalse("$label never locked", ProGate.isLocked(l, true, NOW))
    }

    @Test fun proKeepsLiveTrackingAtT61T120T240() {
        LaunchWindow.unlimitedLive = true // Pro
        for (m in listOf(61L, 120L, 240L)) {
            assertLiveFor(inFlight(m * MIN), "In Flight T+$m")
            assertLiveFor(snap(net = NOW - m * MIN, abbrev = "Go", status = "Go for Launch", webcastLive = true), "webcast T+$m")
            assertFalse("Pro never sees the card at T+$m", LiveTrackingAllowance.shouldShowCard(inFlight(m * MIN), true, MemProStore(), NOW))
        }
    }

    @Test fun grandfatheredCountsAsPro() {
        val store = MemProStore().apply { putBoolean(ProAccess.KEY_GRANDFATHERED, true) }
        assertTrue(ProAccess(store).isPro)
        assertFalse(LiveTrackingAllowance.shouldShowCard(inFlight(2 * HOUR), ProAccess(store).isPro, store, NOW))
    }

    @Test fun freeAtT61GetsTheCardAndIsNeverLocked() {
        LaunchWindow.unlimitedLive = false // free, gates on
        val l = inFlight(61 * MIN)
        assertFalse("free: live window ends at T+60 as before", l.isActiveWatch(NOW))
        assertFalse("free: never locked", ProGate.isLocked(l, false, NOW))
        assertTrue(LiveTrackingAllowance.shouldShowCard(l, false, MemProStore(), NOW))
        assertTrue("free before T+60 is live", inFlight(59 * MIN).isActiveWatch(NOW))
    }

    @Test fun releaseSwitchOffFreeBehavesLikePro() {
        assertFalse(ProConfig.gatesEnabled(debugBuild = false))
        LaunchWindow.unlimitedLive = true // what the module sets when gates are off (see wiring test)
        for (m in listOf(61L, 120L, 240L)) {
            val l = inFlight(m * MIN)
            assertTrue(l.isActiveWatch(NOW))
            assertTrue(LiveClockGate.isLiveWallClock(l, NOW, false))
            assertFalse(LiveTrackingAllowance.shouldShowCard(l, false, MemProStore(), NOW, gatesOn = false))
            assertTrue(ProPolicy.allows(ProHook.JUMP, false, launch = l, now = NOW, gatesOn = false))
        }
    }

    @Test fun finishedStaleAndBuiltInNeverExtend() {
        LaunchWindow.unlimitedLive = true
        assertFalse("Success at T+2h is a replay", snap(net = NOW - 2 * HOUR, webcastLive = true).isActiveWatch(NOW))
        assertFalse("stale In Flight past the ceiling", inFlight(LaunchWindow.LIVE_STATUS_MAX_SEC * 1000L + MIN).isActiveWatch(NOW))
        assertFalse("built-in mission", inFlight(2 * HOUR).copy(id = "demo-soyuz-inflight").isExtendedLive(NOW))
        assertFalse("Go without stream past T+60", snap(net = NOW - 2 * HOUR, abbrev = "Go", status = "Go for Launch").isActiveWatch(NOW))
    }

    private fun src(rel: String) = listOf(File(rel), File("app/$rel")).first { it.exists() }.readText()

    @Test fun moduleWiresTheSwitchAndKeepsStatusLiveBirdsPastTapeEnd() {
        val m = src("src/main/java/com/ccos/retro/module/RocketTelemetryModule.kt")
        val resolve = m.substringAfter("fun resolveTracked(").take(400)
        assertTrue(resolve.contains("LaunchWindow.unlimitedLive = isProProvider()"))
        assertTrue(m.substringAfter("var isProProvider").substringBefore("\n").contains("!com.ccos.retro.pro.ProConfig.gatesEnabled()"))
        assertTrue("AUTO keeps a still-flying bird after the event tape ends",
            m.contains("it.isActiveWatch(now) && (!it.isEventTapeExhausted(now) || it.isStatusLive(now))"))
        assertEquals(RocketTelemetryModule.LIVE_WALL_LAG_SEC, LiveClockGate.trailSec(inFlight(2 * HOUR)), 0f)
    }
}
