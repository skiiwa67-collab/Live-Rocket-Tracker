package com.ccos.retro.module

import com.ccos.retro.data.LaunchSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Proof for the sticky AUTO / LCK targeting rules: no retarget on a single miss.
 */
class TelemetryTargetingTest {

    private val NOW = 1_000_000_000_000L

    private fun snap(
        id: String,
        netFromNowMs: Long,
        status: String = "Go for Launch",
        abbrev: String = "Go",
        rocket: String = "Rocket"
    ): LaunchSnapshot = LaunchSnapshot(
        id = id,
        name = "$rocket | $id",
        statusName = status,
        statusAbbrev = abbrev,
        netMs = NOW + netFromNowMs,
        windowStartMs = NOW + netFromNowMs,
        windowEndMs = NOW + netFromNowMs,
        provider = "LSP",
        rocketName = rocket,
        missionName = id,
        pad = "LC-1",
        location = "Somewhere"
    )

    // ---- AUTO stickiness ----

    /** Scenario 2: a transient getNextAny miss keeps the last good bird (no blank). */
    @Test
    fun auto_keeps_lastgood_on_transient_miss() {
        val current = snap("long-march", 23L * 3600_000L, rocket = "Long March 2D")
        val chosen = TelemetryTargeting.chooseAutoTarget(current, candidate = null, candidateInFlight = false, now = NOW)
        assertEquals("long-march", chosen?.id)
    }

    /** Scenario 2: a window reshuffle (different id, both future) must NOT flip to Soyuz. */
    @Test
    fun auto_does_not_flip_to_soyuz_on_window_shuffle() {
        val current = snap("long-march", 23L * 3600_000L, rocket = "Long March 2D")
        val soyuz = snap("soyuz", 1L * 3600_000L, rocket = "Soyuz-2.1a")
        val chosen = TelemetryTargeting.chooseAutoTarget(current, candidate = soyuz, candidateInFlight = false, now = NOW)
        assertEquals("must stay on the Long March, not flip to Soyuz", "long-march", chosen?.id)
    }

    /** Same id → take the fresh candidate snapshot (field refresh). */
    @Test
    fun auto_refreshes_same_id() {
        val current = snap("f9", 2L * 3600_000L, status = "Go for Launch")
        val fresh = snap("f9", 2L * 3600_000L, status = "In Flight", abbrev = "In Flight")
        val chosen = TelemetryTargeting.chooseAutoTarget(current, candidate = fresh, candidateInFlight = false, now = NOW)
        assertEquals("f9", chosen?.id)
        assertEquals("In Flight", chosen?.statusName)
    }

    /** A live in-flight bird supersedes a mere countdown. */
    @Test
    fun auto_retargets_to_inflight_bird() {
        val current = snap("countdown", 2L * 3600_000L)
        val inflight = snap("live", -60_000L, status = "In Flight", abbrev = "In Flight")
        val chosen = TelemetryTargeting.chooseAutoTarget(current, candidate = inflight, candidateInFlight = true, now = NOW)
        assertEquals("live", chosen?.id)
    }

    /** When the current bird is genuinely gone/terminal, AUTO moves on. */
    @Test
    fun auto_retargets_when_current_terminal() {
        val current = snap("done", 1L * 3600_000L, status = "Launch Successful", abbrev = "Success")
        val next = snap("next", 4L * 3600_000L)
        val chosen = TelemetryTargeting.chooseAutoTarget(current, candidate = next, candidateInFlight = false, now = NOW)
        assertEquals("next", chosen?.id)
    }

    /** No current yet → adopt the candidate. */
    @Test
    fun auto_adopts_candidate_when_no_current() {
        val next = snap("next", 4L * 3600_000L)
        val chosen = TelemetryTargeting.chooseAutoTarget(current = null, candidate = next, candidateInFlight = false, now = NOW)
        assertEquals("next", chosen?.id)
    }

    // ---- LCK stickiness ----

    /** Scenario 3: LCK on a Chinese launch does not blank on a findById miss. */
    @Test
    fun lck_holds_pinned_snapshot_on_miss() {
        val pinned = snap("cz-lck", 10L * 3600_000L, rocket = "Long March 3B")
        val kept = TelemetryTargeting.choosePinnedTarget(found = null, tracked = pinned, pinnedSnapshot = pinned)
        assertEquals("cz-lck", kept?.id)
    }

    /** Scenario 3: LCK recovers the moment findById returns the pinned id again. */
    @Test
    fun lck_recovers_when_findById_returns() {
        val stale = snap("cz-lck", 10L * 3600_000L, status = "Go for Launch")
        val fresh = snap("cz-lck", 10L * 3600_000L, status = "In Flight", abbrev = "In Flight")
        val kept = TelemetryTargeting.choosePinnedTarget(found = fresh, tracked = stale, pinnedSnapshot = stale)
        assertEquals("cz-lck", kept?.id)
        assertEquals("In Flight", kept?.statusName)
    }

    /** LCK never returns null while any snapshot is available. */
    @Test
    fun lck_never_blanks_when_snapshot_available() {
        val pinned = snap("cz-lck", 10L * 3600_000L)
        assertEquals("cz-lck", TelemetryTargeting.choosePinnedTarget(null, null, pinned)?.id)
        assertEquals("cz-lck", TelemetryTargeting.choosePinnedTarget(null, pinned, null)?.id)
        assertNull(TelemetryTargeting.choosePinnedTarget(null, null, null))
    }
}
