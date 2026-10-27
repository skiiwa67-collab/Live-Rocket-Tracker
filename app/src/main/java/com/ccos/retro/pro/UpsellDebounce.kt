package com.ccos.retro.pro

/**
 * Helios no-badger (R-NO-BADGER): a refused Pro gate opens the sheet once per user tap. While the sheet is up, and
 * until [QUIET_MS] pass with no further refused presses, more presses (held key repeat, rapid +/- taps) never stack
 * or reopen it. Every press restarts the quiet window, so a held button cannot reopen the sheet after it is closed.
 */
class UpsellDebounce(private val quietMs: Long = QUIET_MS) {
    var showing: Boolean = false
        private set
    private var lastPressMs: Long = Long.MIN_VALUE / 2

    /** A refused press at [now]. True = open the sheet now. */
    fun tryOpen(now: Long): Boolean {
        val ok = !showing && now - lastPressMs >= quietMs
        lastPressMs = now
        if (ok) showing = true
        return ok
    }

    fun onDismissed(now: Long) {
        showing = false
        lastPressMs = maxOf(lastPressMs, now)
    }

    companion object { const val QUIET_MS = 1_000L }
}
