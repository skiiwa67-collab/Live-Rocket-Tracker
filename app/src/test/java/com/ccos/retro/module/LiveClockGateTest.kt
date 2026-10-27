package com.ccos.retro.module

import com.ccos.retro.data.LaunchSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 158 regression: Falcon 9 Starlink 15-16 (LL2 e2886a96…, NET 2026-10-27T07:25:51Z, SLC-4E)
 * froze at T-00:00 for the whole flight because the cached LL2 snapshot stayed Go / webcast_live=false.
 */
class LiveClockGateTest {

    private val netMs = 1793085951000L // 2026-10-27T07:25:51Z

    private fun snap(
        abbrev: String = "Go",
        name: String = "Go for Launch",
        webcastLive: Boolean = false,
        holdReason: String? = null,
        net: Long = netMs
    ) = LaunchSnapshot(
        id = "e2886a96-5ce9-4f45-872f-4e25a00be837",
        name = "Falcon 9 Block 5 | Starlink Group 15-16",
        statusName = name,
        statusAbbrev = abbrev,
        netMs = net,
        windowStartMs = net,
        windowEndMs = net,
        provider = "SpaceX",
        rocketName = "Falcon 9 Block 5",
        missionName = "Starlink Group 15-16",
        pad = "Space Launch Complex 4E",
        location = "Vandenberg SFB, CA, USA",
        webcastLive = webcastLive,
        holdReason = holdReason
    )

    /** Mirrors RocketTelemetryModule.effectiveSecondsFromNet live branch: (now - pinnedNet)/1000 - trail. */
    private fun tick(launch: LaunchSnapshot, realSecFromNet: Int, pinnedNet: Long = netMs): Float {
        val now = pinnedNet + realSecFromNet * 1000L
        val trail = if (launch.isWebcastLive() || launch.isInFlightStatus())
            RocketTelemetryModule.LIVE_WALL_LAG_SEC else RocketTelemetryModule.SPARSE_WALL_LAG_SEC
        return LiveClockGate.liveSeconds(launch, now, (now - pinnedNet) / 1000f - trail)
    }

    @Test fun staleGoSnapshotLiftsOffAtTPlus451() {
        // Screenshot 0230: webcast PiP read T+00:04:51 while LRT sat at T-00:00.
        val t = tick(snap(), 291)
        assertEquals(291f - RocketTelemetryModule.SPARSE_WALL_LAG_SEC, t, 0.001f)
    }

    @Test fun wallpaperTickSimRunsMonotonicAndNeverAhead() {
        val go = snap()
        var last = Float.NEGATIVE_INFINITY
        for (s in -120..600) {
            val t = tick(go, s)
            assertTrue("tick $s went backwards", t >= last)
            assertTrue("tick $s ahead of real", t <= s.toFloat())
            last = t
        }
        assertTrue("never left the pad", last > 0f)
    }

    @Test fun beforeNetNeverPastTZero() {
        // Lagged clock can never be >= 0 before NET; gate still refuses liftoff pre-NET.
        assertFalse(LiveClockGate.liftoffConfirmed(snap(), netMs - 1000L))
        assertTrue(tick(snap(), -1) < 0f)
    }

    @Test fun holdStaysPausedPastNet() {
        assertEquals(-0.5f, tick(snap(abbrev = "Hold", name = "On Hold"), 120), 0.001f)
        assertEquals(-0.5f, tick(snap(holdReason = "Range violation"), 120), 0.001f)
    }

    @Test fun scrubStaysPaused() {
        assertEquals(-0.5f, tick(snap(abbrev = "Scrubbed", name = "Launch Scrubbed"), 120), 0.001f)
    }

    @Test fun netSlipRewindsToPad() {
        // LL2 moved NET +180s (CZ-8A class) while pinned clock already past old T-0.
        val slipped = snap(net = netMs + 180_000L)
        assertTrue(tick(slipped, 60, pinnedNet = netMs) <= -0.5f)
    }

    @Test fun tbdStillNeedsLiftoffSignal() {
        assertEquals(-0.5f, tick(snap(abbrev = "TBC", name = "To Be Confirmed"), 120), 0.001f)
    }

    @Test fun inFlightAndWebcastUseGoldTrail() {
        assertEquals(115f, tick(snap(abbrev = "In Flight", name = "Launch in Flight"), 120), 0.001f)
        assertEquals(115f, tick(snap(webcastLive = true), 120), 0.001f)
    }

    // ---- 159: Helios P1-1, terminal status without ever seeing In Flight ----

    @Test fun terminalWithoutInFlightRunsAtTPlus600() {
        for ((a, n) in listOf("Success" to "Launch Successful", "Failure" to "Launch Failure", "Partial Failure" to "Launch was a Partial Failure")) {
            val t = tick(snap(abbrev = a, name = n), 600)
            assertEquals("$a froze", 600f - RocketTelemetryModule.SPARSE_WALL_LAG_SEC, t, 0.001f)
            assertTrue(LiveClockGate.liftoffConfirmed(snap(abbrev = a, name = n), netMs + 600_000L))
        }
    }

    @Test fun terminalBeforeNetDoesNotLiftOffEarly() {
        val ok = snap(abbrev = "Success", name = "Launch Successful")
        assertFalse(LiveClockGate.liftoffConfirmed(ok, netMs - 1000L))
        assertFalse(LiveClockGate.liftoffConfirmed(ok, netMs - 600_000L))
        for (s in listOf(-600, -60, -1, 0, 10, 29)) assertTrue("tick $s", tick(ok, s) < 0f)
    }

    @Test fun terminalMonotonicAndNeverAhead() {
        val ok = snap(abbrev = "Success", name = "Launch Successful")
        var last = Float.NEGATIVE_INFINITY
        for (s in -120..1200) {
            val t = tick(ok, s)
            assertTrue(t >= last); assertTrue(t <= s.toFloat()); last = t
        }
    }

    @Test fun terminalStillNotGoAndHoldStillWins() {
        // A hold reason on a terminal snapshot keeps the tip146 pad pause.
        assertEquals(-0.5f, tick(snap(abbrev = "Success", name = "Launch Successful", holdReason = "Range"), 600), 0.001f)
    }

    // ---- 159: Helios P2-7, wiring through the module's pure live path ----

    @Test fun liveWallSecondsFromNetMatchesTickForEveryStatus() {
        for ((a, n) in listOf("Go" to "Go for Launch", "In Flight" to "Launch in Flight", "Success" to "Launch Successful",
            "Hold" to "On Hold", "TBC" to "To Be Confirmed")) {
            val l = snap(abbrev = a, name = n)
            for (s in listOf(-60, 0, 120, 600)) {
                assertEquals("$a @ $s", tick(l, s), LiveClockGate.liveWallSecondsFromNet(l, netMs + s * 1000L, netMs), 0.001f)
            }
        }
        assertEquals(RocketTelemetryModule.LIVE_WALL_LAG_SEC, LiveClockGate.trailSec(snap(webcastLive = true)), 0f)
        assertEquals(RocketTelemetryModule.SPARSE_WALL_LAG_SEC, LiveClockGate.trailSec(snap(abbrev = "Success", name = "Launch Successful")), 0f)
    }

    @Test fun terminalInsideLiveWindowStaysOnWallClockBranch() {
        val ok = snap(abbrev = "Success", name = "Launch Successful")
        assertTrue(LiveClockGate.isLiveWallClock(ok, netMs + 600_000L, historicalMode = false))
        assertFalse(LiveClockGate.isLiveWallClock(ok, netMs + 600_000L, historicalMode = true))
        assertFalse(LiveClockGate.isLiveWallClock(ok.copy(id = "demo-spacex-f9"), netMs + 600_000L, historicalMode = false))
    }

    @Test fun moduleDelegatesToGate() {
        val f = listOf(java.io.File("src/main/java/com/ccos/retro/module/RocketTelemetryModule.kt"),
            java.io.File("app/src/main/java/com/ccos/retro/module/RocketTelemetryModule.kt")).first { it.exists() }
        val body = f.readText().substringAfter("fun effectiveSecondsFromNet(").substringBefore("fun clockIsEst(")
        assertTrue(body.contains("isLiveWallClockBird(launch, now)"))
        assertTrue(body.contains("LiveClockGate.liveWallSecondsFromNet(launch, now, t0)"))
        assertTrue(f.readText().contains("LiveClockGate.isLiveWallClock(launch, now, prefs.telemetryListMode == \"historical\")"))
    }
}
