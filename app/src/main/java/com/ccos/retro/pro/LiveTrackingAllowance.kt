package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * Chris 10-27 07:51/07:53 CT: "Live launches never lock. It's just the amount of time that you can track."
 * A live launch is never locked (ProGate.isLocked stays false: no lock icon, no [PRO] tag, always selectable).
 * Free users get a tracking-time allowance of [FREE_MS] counted from actual liftoff, not NET, so holds and
 * slips before liftoff never use it up. When a free user's tracking session of a still-live launch runs past
 * the allowance, the app shows one brief fading note per launch and moves to the next upcoming launch (B6).
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
        return !idsContain(store, KEY_CARD_SHOWN_ID, l.id)
    }

    /** B6: keeps a short per-launch list (an old single-id value reads as a one-item list), so A -> B -> A never re-shows. */
    fun markShown(store: ProStore, l: LaunchSnapshot) = addId(store, KEY_CARD_SHOWN_ID, l.id)

    // ---- B6 (Chris 08:11 / 08:13 CT, Da Vinci final 08:12 / 08:13; rules R-LRT-FREE-PRO-x-live-1h-2, R-NO-BADGER) ----
    // At T+60 a free user gets ONE brief fading note (no card, no buttons, no Pro mention), then the tracker moves to the
    // next upcoming launch. Pro, grandfathered and switch-off users never see it and never switch. Once per launch.

    const val NOTE_FADE_IN_MS = 300L
    const val NOTE_HOLD_MS = 5_000L
    const val NOTE_FADE_OUT_MS = 600L
    const val NOTE_TOTAL_MS = NOTE_FADE_IN_MS + NOTE_HOLD_MS + NOTE_FADE_OUT_MS
    /** Idle guard: a touch on the tracker in the last 5 s defers the switch until 5 s pass with no touches. */
    const val IDLE_GUARD_MS = 5_000L
    const val KEY_HANDOFF_DONE_IDS = "pro_allowance_handoff_done_ids"
    private const val MAX_IDS = 24

    private fun ids(store: ProStore, key: String): List<String> =
        store.getString(key).orEmpty().split(',').filter { it.isNotBlank() }

    fun idsContain(store: ProStore, key: String, id: String): Boolean = id in ids(store, key)

    fun addId(store: ProStore, key: String, id: String) {
        if (id.isBlank() || idsContain(store, key, id)) return
        store.putString(key, (ids(store, key) + id).takeLast(MAX_IDS).joinToString(","))
    }

    /** Free tier, gates on, a real launch still live whose free hour after liftoff is used up. */
    fun isFreeHourOver(l: LaunchSnapshot?, isPro: Boolean, now: Long, gatesOn: Boolean): Boolean =
        l != null && !isPro && gatesOn && ProGate.isStillLive(l) && isUsedUp(l, now)

    fun isHandoffDone(store: ProStore, id: String): Boolean = idsContain(store, KEY_HANDOFF_DONE_IDS, id)
    fun markHandoffDone(store: ProStore, id: String) = addId(store, KEY_HANDOFF_DONE_IDS, id)

    /** Note opacity at [elapsedMs] since it started: 300 ms in, 5 s hold, 600 ms out, then 0 (gone by itself). */
    fun noteAlpha(elapsedMs: Long): Float = when {
        elapsedMs < 0L -> 0f
        elapsedMs < NOTE_FADE_IN_MS -> elapsedMs.toFloat() / NOTE_FADE_IN_MS
        elapsedMs < NOTE_FADE_IN_MS + NOTE_HOLD_MS -> 1f
        elapsedMs < NOTE_TOTAL_MS -> 1f - (elapsedMs - NOTE_FADE_IN_MS - NOTE_HOLD_MS).toFloat() / NOTE_FADE_OUT_MS
        else -> 0f
    }

    fun isNoteVisible(elapsedMs: Long): Boolean = noteAlpha(elapsedMs) > 0f

    /**
     * Soonest real launch with NET in the future that is not [currentId]. Built-in catalog entries (sample / featured,
     * ids "demo-") never count, so a sample never shows as if it were real. null = go to the default launches view.
     */
    fun nextUpcoming(launches: List<LaunchSnapshot>, currentId: String?, now: Long): LaunchSnapshot? =
        launches.filter { it.id != currentId && !it.id.startsWith("demo-") && it.netMs > now && !it.isTerminal() }
            .minByOrNull { it.netMs }

    fun nextName(l: LaunchSnapshot): String = l.missionName.ifBlank { l.name }

    /**
     * Note copy. [fits] = the text fits 2 lines at 16sp without breaking a word. If the "Up next" form does not fit,
     * drop that clause (never ellipsize): [shortText] is the first sentence alone.
     */
    fun noteText(nextName: String?, nextFormat: String, noneText: String, shortText: String, fits: (String) -> Boolean): String {
        if (nextName.isNullOrBlank()) return noneText
        val full = String.format(nextFormat, nextName)
        return if (fits(full)) full else shortText
    }

    enum class Handoff { WAIT, SWITCH, CANCEL }

    /**
     * When to move a free user off [launchId] after its note. CANCEL: Pro / grandfathered / switch off, already done,
     * or the user is no longer viewing that launch (never yank them). WAIT: note still fading or a touch in the last 5 s.
     */
    fun handoffDecision(
        launchId: String,
        viewingId: String?,
        noteStartMs: Long,
        lastTouchMs: Long,
        now: Long,
        isPro: Boolean,
        gatesOn: Boolean,
        store: ProStore
    ): Handoff {
        if (isPro || !gatesOn) return Handoff.CANCEL
        if (isHandoffDone(store, launchId)) return Handoff.CANCEL
        if (viewingId != launchId) return Handoff.CANCEL
        if (now < noteStartMs + NOTE_TOTAL_MS) return Handoff.WAIT
        if (lastTouchMs > 0L && now - lastTouchMs < IDLE_GUARD_MS) return Handoff.WAIT
        return Handoff.SWITCH
    }
}
