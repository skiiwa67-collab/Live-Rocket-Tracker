package com.ccos.retro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CHRIS RULE proof: LaunchDataProvider.mergeWatch is an upsert MERGE BY ID that
 * never erases a live bird on refresh.
 */
class LaunchMergeTest {

    private val NOW = 1_000_000_000_000L

    private fun snap(
        id: String,
        netFromNowMs: Long,
        status: String = "Go for Launch",
        abbrev: String = "Go",
        provider: String = "Test LSP",
        rocket: String = "Test Rocket",
        mission: String = "Test Mission"
    ): LaunchSnapshot = LaunchSnapshot(
        id = id,
        name = "$rocket | $mission",
        statusName = status,
        statusAbbrev = abbrev,
        netMs = NOW + netFromNowMs,
        windowStartMs = NOW + netFromNowMs,
        windowEndMs = NOW + netFromNowMs,
        provider = provider,
        rocketName = rocket,
        missionName = mission,
        pad = "LC-1",
        location = "Somewhere"
    )

    /** The headline invariant: a T-23h CZ-2D survives a refresh whose window omits it. */
    @Test
    fun czlm_survives_refresh_that_drops_it_from_window() {
        val cz2d = snap(
            id = "cz-2d-23h",
            netFromNowMs = 23L * 3600_000L,
            provider = "China Aerospace Science and Technology Corporation",
            rocket = "Long March 2D",
            mission = "Yaogan"
        )
        val existing = listOf(
            snap("falcon-1", 1L * 3600_000L),
            cz2d,
            snap("soyuz-1", 3L * 3600_000L, provider = "Roscosmos", rocket = "Soyuz-2.1a")
        )
        // Fresh top-20 window does NOT contain the CZ-2D.
        val incoming = listOf(
            snap("falcon-1", 1L * 3600_000L),
            snap("soyuz-1", 3L * 3600_000L, provider = "Roscosmos", rocket = "Soyuz-2.1a"),
            snap("new-electron", 4L * 3600_000L)
        )

        val merged = LaunchDataProvider.mergeWatch(existing, incoming, NOW)
        val ids = merged.map { it.id }

        assertTrue("CZ-2D must be kept even though it fell out of the window", "cz-2d-23h" in ids)
        assertTrue("new launch must be added", "new-electron" in ids)
        assertNotNull(merged.firstOrNull { it.id == "cz-2d-23h" })
    }

    /** Upsert updates fields on an existing id (fresh snapshot wins). */
    @Test
    fun upsert_updates_existing_fields() {
        val existing = listOf(snap("f9", 2L * 3600_000L, status = "Go for Launch", abbrev = "Go"))
        val incoming = listOf(snap("f9", 2L * 3600_000L, status = "In Flight", abbrev = "In Flight"))

        val merged = LaunchDataProvider.mergeWatch(existing, incoming, NOW)
        assertEquals(1, merged.size)
        assertEquals("In Flight", merged.first().statusName)
    }

    /** Terminal birds (success / failure / scrub) age out and are dropped on merge. */
    @Test
    fun terminal_launches_are_dropped() {
        val existing = listOf(
            snap("done-success", 1L * 3600_000L, status = "Launch Successful", abbrev = "Success"),
            snap("done-fail", 1L * 3600_000L, status = "Launch Failure", abbrev = "Failure"),
            snap("done-scrub", 1L * 3600_000L, status = "Scrubbed", abbrev = "Scrub"),
            snap("live-go", 5L * 3600_000L)
        )
        val incoming = listOf(snap("brand-new", 6L * 3600_000L))

        val merged = LaunchDataProvider.mergeWatch(existing, incoming, NOW)
        val ids = merged.map { it.id }

        assertFalse("success must age out", "done-success" in ids)
        assertFalse("failure must age out", "done-fail" in ids)
        assertFalse("scrub must age out", "done-scrub" in ids)
        assertTrue("still-future live bird must stay", "live-go" in ids)
        assertTrue("new bird must be added", "brand-new" in ids)
    }

    /** A launch that flew well in the past (and fell out of the window) ages out. */
    @Test
    fun long_flown_launch_ages_out() {
        val existing = listOf(
            snap("flew-yesterday", -8L * 3600_000L, status = "Go for Launch"),
            snap("future", 2L * 3600_000L)
        )
        val incoming = listOf(snap("future", 2L * 3600_000L))

        val merged = LaunchDataProvider.mergeWatch(existing, incoming, NOW)
        val ids = merged.map { it.id }
        assertFalse("a launch 8h past NET ages out", "flew-yesterday" in ids)
        assertTrue("future launch stays", "future" in ids)
    }

    /** Merge output is ordered by NET ascending. */
    @Test
    fun merge_is_sorted_by_net() {
        val existing = listOf(snap("c", 5L * 3600_000L))
        val incoming = listOf(snap("a", 1L * 3600_000L), snap("b", 3L * 3600_000L))
        val merged = LaunchDataProvider.mergeWatch(existing, incoming, NOW)
        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
    }
}
