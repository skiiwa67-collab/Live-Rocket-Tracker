package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * Gate predicate. locked = past NET+60 min (LaunchSnapshot.isReplayable) AND not a free demo AND not Pro.
 * Live / upcoming / active-watch launches are never locked. Wired via ProPolicy (vc159).
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

    // vc159: wired only through ProPolicy (see ProHook for every call site); UI callers never call this directly.
}
