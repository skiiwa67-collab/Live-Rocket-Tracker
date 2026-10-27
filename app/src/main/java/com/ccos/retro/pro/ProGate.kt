package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * Gate predicate. locked = past NET+60 min (LaunchSnapshot.isReplayable) AND not a free demo AND not Pro.
 * Live / upcoming / active-watch launches are never locked. Not wired into UI yet.
 */
object ProGate {
    fun isLocked(l: LaunchSnapshot?, isPro: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        if (l == null || isPro) return false
        if (!l.isReplayable(now)) return false // live + upcoming always free
        return !DemoSet.isFreeDemo(l)
    }

    /** CDT jump / event skip. Free on live; only blocked on a locked replay. */
    fun canJumpOrSkip(l: LaunchSnapshot?, isPro: Boolean, now: Long = System.currentTimeMillis()): Boolean =
        !isLocked(l, isPro, now)

    // TODO(wire): MainActivity historic select (~:194) + historic search rows, RocketTelemetryModule.selectLaunch
    //   (defense in depth), maybeAdvanceHistoricRoll (free demo set only), jumpTo/skipEvent, wallpaper replay path.
    // TODO(davinci): locked historic row style, lock badge on HISTORIC chip, upsell sheet, toast. See pro-ui-brief.md.
}
