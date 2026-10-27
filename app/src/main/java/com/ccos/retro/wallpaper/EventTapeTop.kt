package com.ccos.retro.wallpaper

/**
 * Event tape top row (#79): the top row is never empty once the tape runs.
 * An event that is soon (0..lead s ahead) or now (within 0.5 s after) owns the top;
 * otherwise the most recent past event is held at top-center until the next one is soon.
 */
object EventTapeTop {
    const val NOW_AFTER_SEC = 0.5f

    /** Index into [times] of the past event held at top-center, or -1 if none / a soon-now event owns the top. */
    fun heldIndex(times: List<Float>, tSec: Float, lead: Float): Int {
        var held = -1
        for (i in times.indices) {
            val dt = times[i] - tSec
            if (dt >= -NOW_AFTER_SEC && dt <= lead) return -1
            if (dt < -NOW_AFTER_SEC && (held < 0 || times[i] >= times[held])) held = i
        }
        return held
    }
}
