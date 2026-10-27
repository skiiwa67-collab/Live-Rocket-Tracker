package com.ccos.retro.module

import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.pro.MemProStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Helios P1-A: an LL2 "Failure" on a vehicle that never left the pad must not run the clock. */
class PadAbortFailureTest {
    private val netMs = 1793085951000L

    private fun snap(abbrev: String, name: String, failReason: String? = null, webcastLive: Boolean = false, id: String = "ll2-x") =
        LaunchSnapshot(id = id, name = "R | M", statusName = name, statusAbbrev = abbrev, netMs = netMs,
            windowStartMs = netMs, windowEndMs = netMs, provider = "P", rocketName = "R", missionName = "M",
            pad = "Pad", location = "Loc", webcastLive = webcastLive, failReason = failReason)

    private val fail = snap("Failure", "Launch Failure")
    private fun t(l: LaunchSnapshot, sec: Int, seen: Boolean = false) =
        LiveClockGate.liveWallSecondsFromNet(l, netMs + sec * 1000L, netMs, seen)

    @Test fun failureWithNoEvidenceHoldsAtMinusHalf() {
        for (s in listOf(0, 30, 600, 3600)) assertEquals("T+$s", -0.5f, t(fail, s), 0.001f)
        assertFalse(LiveClockGate.liftoffConfirmed(fail, netMs + 600_000L))
        assertTrue("status text still says Failure", fail.statusName.contains("Failure") && fail.isTerminal())
    }

    @Test fun failureWithLiftoffSeenRuns() {
        assertTrue(LiveClockGate.liftoffConfirmed(fail, netMs + 600_000L, liftoffSeen = true))
        assertEquals(600f - RocketTelemetryModule.SPARSE_WALL_LAG_SEC, t(fail, 600, seen = true), 0.001f)
    }

    @Test fun failureWithPostLiftoffFailReasonRuns() {
        val l = snap("Failure", "Launch Failure", failReason = "Second stage engine shut down early; payload did not reach orbit.")
        assertTrue(LiveClockGate.liftoffConfirmed(l, netMs + 600_000L))
        assertTrue(t(l, 600) > 0f)
    }

    @Test fun padAbortFailReasonStillHolds() {
        for (r in listOf("Engine shutdown before liftoff; vehicle remained on the pad.", "Pad abort at T-0", "")) {
            val l = snap("Failure", "Launch Failure", failReason = r)
            assertFalse(r, LiveClockGate.liftoffConfirmed(l, netMs + 600_000L))
            assertEquals(r, -0.5f, t(l, 600), 0.001f)
        }
    }

    @Test fun partialFailureRuns() {
        val l = snap("Partial Failure", "Launch was a Partial Failure")
        assertFalse(l.isPlainFailure())
        assertTrue(LiveClockGate.liftoffConfirmed(l, netMs + 600_000L))
        assertTrue(t(l, 600) > 0f)
    }

    @Test fun successRuns() {
        val l = snap("Success", "Launch Successful")
        assertFalse(l.isPlainFailure())
        assertTrue(t(l, 600) > 0f)
    }

    @Test fun failureBeforeNetNeverLiftsOff() {
        assertFalse(LiveClockGate.liftoffConfirmed(fail, netMs - 1000L, liftoffSeen = true))
    }

    @Test fun flagIsSetByInFlightOrLiveWebcastAfterNet() {
        val now = netMs + 10_000L
        assertTrue(LiveClockGate.observedLiftoff(snap("In Flight", "Launch in Flight"), now))
        assertTrue(LiveClockGate.observedLiftoff(snap("Go", "Go for Launch", webcastLive = true), now))
        assertFalse("webcast before NET", LiveClockGate.observedLiftoff(snap("Go", "Go for Launch", webcastLive = true), netMs - 10_000L))
        assertFalse(LiveClockGate.observedLiftoff(snap("Go", "Go for Launch"), now))
        assertFalse("built-in sample", LiveClockGate.observedLiftoff(snap("In Flight", "Launch in Flight", id = "demo-x"), now))
    }

    @Test fun flagSurvivesProcessRestart() {
        val prefs = MemProStore()
        LiftoffEvidence.mark(prefs, "ll2-x")
        // "Restart": a fresh store object over the same persisted values.
        val restarted = MemProStore().apply { map.putAll(prefs.map) }
        assertTrue(LiftoffEvidence.isSeen(restarted, "ll2-x"))
        assertFalse(LiftoffEvidence.isSeen(restarted, "ll2-y"))
        LiftoffEvidence.mark(restarted, "ll2-y")
        assertEquals("ll2-x,ll2-y", restarted.getString(LiftoffEvidence.KEY_SEEN_IDS))
    }

    @Test fun moduleWiresTheStickyFlagAndParsesFailReason() {
        fun src(rel: String) = listOf(File(rel), File("app/$rel")).first { it.exists() }.readText()
        val m = src("src/main/java/com/ccos/retro/module/RocketTelemetryModule.kt")
        assertTrue(m.contains("LiveClockGate.liveWallSecondsFromNet(launch, now, t0, liftoffSeen(launch, now))"))
        assertTrue(m.contains("LiftoffEvidence.mark(prefs.proStore(), launch.id)"))
        assertTrue(src("src/main/java/com/ccos/retro/data/LaunchDataProvider.kt").contains("strOrNull(o, \"failreason\")"))
    }
}
