package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * Chris 10-27 07:51/07:53 CT: "Live launches never lock. It's just the amount of time that you can track."
 * A live launch is never locked (ProGate.isLocked stays false: no lock icon, no [PRO] tag, always selectable).
 * Free users get a tracking-time allowance of [FREE_MS] counted from actual liftoff, not NET, so holds and
 * slips before liftoff never use it up. When a free user's tracking session of a still-live launch runs past
 * the allowance, the app shows one "Keep tracking with Pro" card, once per launch.
 */
object LiveTrackingAllowance {
    const val FREE_MS = 60 * 60_000L
    const val KEY_CARD_SHOWN_ID = "pro_allowance_card_launch_id"

    /**
     * Actual liftoff, or null when the launch has not lifted off. LL2 moves `net` to the real T-0 once a
     * launch lifts off (In Flight, then Success / Failure), so net is the liftoff time only in those states.
     * Go / Hold / TBD past NET is a slip or a hold: no liftoff, no clock.
     */
    fun liftoffMs(l: LaunchSnapshot): Long? =
        if (l.isInFlightStatus() || l.isTerminal()) l.netMs else null

    /** Allowance left in ms (FREE_MS before liftoff). Never negative. */
    fun remainingMs(l: LaunchSnapshot, now: Long = System.currentTimeMillis()): Long {
        val t0 = liftoffMs(l) ?: return FREE_MS
        return (t0 + FREE_MS - now).coerceIn(0L, FREE_MS)
    }

    fun isUsedUp(l: LaunchSnapshot, now: Long = System.currentTimeMillis()): Boolean =
        liftoffMs(l) != null && remainingMs(l, now) == 0L

    /**
     * True when the card should show now: a free user, gates on, tracking a still-live real launch whose
     * allowance is used up, and the card has not been shown for this launch yet. Pure; [markShown] records it.
     */
    fun shouldShowCard(
        l: LaunchSnapshot?,
        isPro: Boolean,
        store: ProStore,
        now: Long = System.currentTimeMillis(),
        gatesOn: Boolean = true
    ): Boolean {
        if (l == null || isPro || !gatesOn) return false
        if (!ProGate.isStillLive(l)) return false
        if (!isUsedUp(l, now)) return false
        return store.getString(KEY_CARD_SHOWN_ID) != l.id
    }

    fun markShown(store: ProStore, l: LaunchSnapshot) = store.putString(KEY_CARD_SHOWN_ID, l.id)
}
