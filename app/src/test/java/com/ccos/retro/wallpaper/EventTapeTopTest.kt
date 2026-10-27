package com.ccos.retro.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

/** #79 regression: event tape top row must not go empty between events. */
class EventTapeTopTest {
    // PROP LOAD, LIVE FEED, COUNTDOWN, LIFTOFF, MAX-Q, MECO, STAGE SEP
    private val times = listOf(-1800f, -1500f, -600f, 0f, 72f, 150f, 154f)
    private val lead = 5f

    @Test fun latestPastEventHeldBetweenEvents() {
        assertEquals(3, EventTapeTop.heldIndex(times, 30f, lead))   // LIFTOFF held until MAX-Q is soon
        assertEquals(4, EventTapeTop.heldIndex(times, 100f, lead))  // MAX-Q held
        assertEquals(2, EventTapeTop.heldIndex(times, -300f, lead)) // COUNTDOWN held
        assertEquals(6, EventTapeTop.heldIndex(times, 2000f, lead)) // last event stays up
    }

    @Test fun soonOrNowEventOwnsTop() {
        assertEquals(-1, EventTapeTop.heldIndex(times, 68f, lead))   // MAX-Q soon
        assertEquals(-1, EventTapeTop.heldIndex(times, 72.3f, lead)) // MAX-Q now
        assertEquals(-1, EventTapeTop.heldIndex(times, -1800f, lead))
    }

    @Test fun topNeverEmptyAcrossFlight() {
        var t = -1800f
        while (t <= 3600f) {
            val held = EventTapeTop.heldIndex(times, t, lead)
            val ownsTop = times.any { it - t >= -EventTapeTop.NOW_AFTER_SEC && it - t <= lead }
            assertEquals("t=$t", true, held >= 0 || ownsTop)
            t += 0.25f
        }
    }
}
