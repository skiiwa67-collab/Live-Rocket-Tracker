package com.ccos.retro.module

import com.ccos.retro.data.LaunchSnapshot

/**
 * Live wall-clock gate for CURRENT/AUTO birds (pure, JVM-testable).
 * tip146 hold/scrub + never-ahead rules, lifted out of [RocketTelemetryModule] unchanged,
 * plus 158: a firm LL2 Go with NET reached counts as liftoff (no hold/scrub/slip signal).
 */
object LiveClockGate {

    /**
     * Lagged live seconds from NET after hold/scrub pause and the liftoff gate.
     * [tLagged] = (now - pinnedNet) / 1000 - trail.
     */
    fun liveSeconds(launch: LaunchSnapshot, now: Long, tLagged: Float, liftoffSeen: Boolean = false): Float {
        var t = tLagged
        if (shouldPauseForHoldScrub(launch, now, t)) {
            // Hold / Scrub / TBD+slip: pause at pad; never fake live past a real hold.
            t = minOf(t, -0.5f)
        }
        // Never past T-0 without a liftoff signal.
        if (t >= 0f && !liftoffConfirmed(launch, now, liftoffSeen)) {
            t = -0.5f
        }
        // Helios P1-A: a plain Failure with NET reached and no liftoff evidence is a pad abort: hold at -0.5 s
        // immediately (no lagged countdown inside the 5/30 s trail implying a liftoff is still coming).
        if (launch.isPlainFailure() && launch.secondsToNet(now) <= 0L && !liftoffConfirmed(launch, now, liftoffSeen)) {
            t = -0.5f
        }
        return t
    }

    /**
     * Liftoff confirmed past T-0: LL2 In Flight, or NET reached with webcast_live,
     * or (158) NET reached with LL2 Go and no hold/scrub. Before 158 a stale Go
     * snapshot (LL2 429 near T-0 kept webcast_live=false) pinned T-00:00 for the
     * whole flight (Falcon 9 Starlink 15-16, 2026-10-27).
     * 159 (Helios P1-1): a terminal LL2 status (Success / Failure / Partial Failure) once NET has
     * passed also counts. Sparse / 429 polling often jumps Go -> Success without ever showing
     * In Flight; isGo() is false for those, so the clock froze at T-00:00 for the 60 min live window.
     * Terminal before NET does NOT lift off early (NET check comes first).
     * Helios P1-A (pad abort): Success and Partial Failure imply liftoff. A plain Failure counts only with evidence:
     * [liftoffSeen] (sticky per-launch flag: this app saw LL2 In Flight; webcast_live is not evidence, Skippy N2) or an LL2
     * failreason that describes a post-liftoff failure. Otherwise the clock holds at -0.5 s; the status still says Failure.
     */
    fun liftoffConfirmed(launch: LaunchSnapshot, now: Long, liftoffSeen: Boolean = false): Boolean {
        if (launch.isInFlightStatus()) return true
        if (launch.secondsToNet(now) > 0L) return false
        if (launch.isPlainFailure()) return liftoffSeen || observedLiftoff(launch, now) || failReasonShowsLiftoff(launch.failReason)
        if (launch.isWebcastLive()) return true
        if (launch.isTerminal()) return true
        return launch.isGo() && !launch.isHold() && !launch.isScrubStatus()
    }

    /**
     * P1-A: the observation that sets the sticky liftoffSeen flag: LL2 In Flight only.
     * Skippy N2 (vc159 re-review): webcast_live with NET reached is NOT liftoff evidence. Webcasts are live at T-0 on
     * nearly every streamed launch, including pad aborts, so it marked "liftoff seen" and a later plain Failure ran the
     * flight clock. Trade-off: a real in-flight Failure never seen as In Flight, with an empty failreason, holds at -0.5 s.
     */
    fun observedLiftoff(launch: LaunchSnapshot, @Suppress("UNUSED_PARAMETER") now: Long): Boolean =
        !launch.id.startsWith("demo-") && launch.isInFlightStatus()

    private val PAD_WORDS = listOf("pad abort", "on the pad", "before liftoff", "before lift-off", "prior to liftoff",
        "prior to lift-off", "did not lift off", "didn't lift off", "never left the pad", "never lifted off", "abort at t-0",
        "aborted at t-0", "aborted at t-", "hold-down", "rsrs", "engine cutoff at ignition", "shutdown at ignition")
    private val FLIGHT_WORDS = listOf("after liftoff", "after lift-off", "after launch", "in flight", "during flight",
        "during ascent", "ascent", "max q", "max-q", "meco", "seco", "stage separation", "second stage", "upper stage",
        "third stage", "fairing", "orbit", "trajectory", "flight termination", "self-destruct", "exploded", "t+")

    /** P1-A: an LL2 failreason that clearly describes a failure after liftoff (pad-abort wording always wins). */
    fun failReasonShowsLiftoff(reason: String?): Boolean {
        val r = reason?.lowercase()?.trim().orEmpty()
        if (r.isEmpty()) return false
        if (PAD_WORDS.any { it in r }) return false
        return FLIGHT_WORDS.any { it in r }
    }

    /**
     * tip146 hold-scrub: Pause/backup when LL2 Hold, Scrub/cancel, or TBD with slipped NET,
     * or when NET is still meaningfully in the future while the lagged clock would be past T-0.
     */
    fun shouldPauseForHoldScrub(launch: LaunchSnapshot, now: Long, tLagged: Float): Boolean {
        if (launch.isHold()) return true
        if (launch.isScrubStatus()) return true
        val netAhead = launch.secondsToNet(now)
        if (launch.isTbd() && netAhead > RocketTelemetryModule.NET_SLIP_HOLD_SEC) return true
        // NET slipped later while lagged sim already at/past T-0 (CZ-8A class).
        if (tLagged >= 0f && netAhead >= RocketTelemetryModule.NET_SLIP_HOLD_SEC) return true
        return false
    }

    /** Trail behind wall clock: gold 5 s with webcast_live / In Flight, sparse 30 s otherwise. */
    fun trailSec(launch: LaunchSnapshot): Float =
        if (launch.isWebcastLive() || launch.isInFlightStatus()) RocketTelemetryModule.LIVE_WALL_LAG_SEC
        else RocketTelemetryModule.SPARSE_WALL_LAG_SEC

    /**
     * Which birds run on the live wall clock (vs sim/replay cursor). Built-in catalog missions never do;
     * historical list mode never does; otherwise active watch or not yet past the live window.
     */
    fun isLiveWallClock(launch: LaunchSnapshot, now: Long, historicalMode: Boolean): Boolean {
        if (launch.id.startsWith("demo-")) return false
        if (historicalMode) return false
        return launch.isActiveWatch(now) || !launch.isReplayable(now)
    }

    /** Live branch of RocketTelemetryModule.effectiveSecondsFromNet (pure; P2-7 wiring test target). */
    fun liveWallSecondsFromNet(launch: LaunchSnapshot, now: Long, pinnedNetMs: Long, liftoffSeen: Boolean = false): Float =
        liveSeconds(launch, now, (now - pinnedNetMs) / 1000f - trailSec(launch), liftoffSeen)
}
