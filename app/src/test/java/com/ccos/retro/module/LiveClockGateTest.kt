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
}
