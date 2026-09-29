package com.ccos.retro.module

import com.ccos.retro.data.LaunchSnapshot

/**
 * Pure, framework-free targeting decisions for [RocketTelemetryModule].
 *
 * CHRIS RULE at the display layer: a single transient miss must never flip the
 * tracked launch. No AUTO/LCK retarget on one bad tick — keep the last good bird
 * and only move when the current one is genuinely gone/terminal (or a live in-flight
 * bird supersedes a countdown). Kept out of the module so it can be unit-tested
 * without AppPrefs / Android.
 */
object TelemetryTargeting {

    /** A launch this long past NET (and not tracked as in-flight) has clearly flown. */
    private const val GONE_PAST_SEC = 1800L

    /** The tracked launch is no longer a valid live target. */
    fun isGone(launch: LaunchSnapshot, now: Long): Boolean =
        launch.isTerminal() || launch.secondsToNet(now) < -GONE_PAST_SEC

    /**
     * AUTO mode target. Sticky by design.
     *
     * @param current the launch tracked last tick (last good).
     * @param candidate the best fresh candidate this tick (in-flight bird, else next-any); may be null on a miss.
     * @param candidateInFlight true when [candidate] is a live in-flight launch.
     *
     * Rules:
     *  - No current yet → adopt the candidate.
     *  - Candidate missing (transient getNextAny miss) → keep current unless it is gone.
     *  - Same id → take the fresh candidate snapshot (field refresh).
     *  - Different id → keep current UNLESS current is gone/terminal, or a live in-flight
     *    bird supersedes a mere countdown. A window reshuffle alone never retargets.
     */
    fun chooseAutoTarget(
        current: LaunchSnapshot?,
        candidate: LaunchSnapshot?,
        candidateInFlight: Boolean,
        now: Long
    ): LaunchSnapshot? {
        if (current == null) return candidate
        if (candidate == null) return if (isGone(current, now)) null else current
        if (candidate.id == current.id) return candidate
        if (isGone(current, now)) return candidate
        if (candidateInFlight && !current.isInFlight(now)) return candidate
        // current is still a valid future/live bird — do not flip on a transient window change.
        return current
    }

    /**
     * LCK (hard pin) target. Never blanks on a single findById miss.
     *
     * @param found fresh catalog hit for the pinned id, or null on a miss.
     * @param tracked what is tracked now.
     * @param pinnedSnapshot the last snapshot that matched the pin.
     *
     * Prefers a fresh hit; otherwise holds the last known pinned snapshot / tracked.
     * Recovers automatically the moment findById returns again.
     */
    fun choosePinnedTarget(
        found: LaunchSnapshot?,
        tracked: LaunchSnapshot?,
        pinnedSnapshot: LaunchSnapshot?
    ): LaunchSnapshot? = found ?: pinnedSnapshot ?: tracked
}
