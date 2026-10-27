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
    fun liveSeconds(launch: LaunchSnapshot, now: Long, tLagged: Float): Float {
        var t = tLagged
        if (shouldPauseForHoldScrub(launch, now, t)) {
            // Hold / Scrub / TBD+slip: pause at pad; never fake live past a real hold.
            t = minOf(t, -0.5f)
        }
        // Never past T-0 without a liftoff signal.
        if (t >= 0f && !liftoffConfirmed(launch, now)) {
            t = -0.5f
        }
        return t
    }

    /**
     * Liftoff confirmed past T-0: LL2 In Flight, or NET reached with webcast_live,
     * or (158) NET reached with LL2 Go and no hold/scrub. Before 158 a stale Go
     * snapshot (LL2 429 near T-0 kept webcast_live=false) pinned T-00:00 for the
     * whole flight (Falcon 9 Starlink 15-16, 2026-10-27).
     */
    fun liftoffConfirmed(launch: LaunchSnapshot, now: Long): Boolean {
        if (launch.isInFlightStatus()) return true
        if (launch.secondsToNet(now) > 0L) return false
        if (launch.isWebcastLive()) return true
        return launch.isGo() && !launch.isHold() && !launch.isScrubStatus()
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
}
