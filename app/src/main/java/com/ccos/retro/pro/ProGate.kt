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
        // B1 (Skippy vc159): isReplayable flips at NET+60 whatever the status, so a long coast or a
        // still-streaming webcast used to lock mid-flight. An unfinished real launch is never locked.
        if (isStillLive(l)) return false
        // Chris 07:53: a real launch that has not lifted off (hold / slip past NET) is never locked.
        if (!l.id.startsWith("demo-") && LiveTrackingAllowance.liftoffMs(l) == null) return false
        return !DemoSet.isFreeDemo(l)
    }

    /** Real (non-catalog) launch that LL2 still reports In Flight or webcast_live, at any T+. */
    fun isStillLive(l: LaunchSnapshot): Boolean =
        !l.id.startsWith("demo-") && !l.isTerminal() && (l.isInFlightStatus() || l.isWebcastLive())

    /**
     * CDT jump / event skip. Free on live / upcoming / still-in-flight. Pro on every historic and
     * replay, including Included samples (Starship Flight 13). Viewing F13 stays free; +/- does not.
     */
    fun canJumpOrSkip(l: LaunchSnapshot?, isPro: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        if (isPro || l == null) return true
        if (!l.isReplayable(now)) return true
        if (isStillLive(l)) return true
        if (!l.id.startsWith("demo-") && LiveTrackingAllowance.liftoffMs(l) == null) return true
        return false
    }

    // vc159: wired only through ProPolicy (see ProHook for every call site); UI callers never call this directly.
}
