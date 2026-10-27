package com.ccos.retro.pro

/**
 * vc160 Da Vinci LRT Pro Gold v1 section 4.4 (Chris hearted 11:42 CT 10-27): the one-time "unlocked" moment.
 *
 * Exactly one trigger: the Pro sheet's own billing.purchase callback returns OWNED (ProUi). Restore, a PENDING
 * purchase confirmed later, entitlement refresh, reinstall, the wallpaper, timers and launches never play it
 * (O2/O3 open with Chris; default NO burst). R-NO-BADGER: nothing here can open a sheet on its own.
 *
 * The persistent flag is written BEFORE anything plays, so a crash or rotation mid-animation can never replay it.
 * Pure JVM so tests cover it.
 */
object ProGoldMoment {
    const val KEY_SHOWN = "pro_gold_moment_shown"

    /** Timeline (ms). Total 2.2 s, self-dismiss at [DURATION_MS]. */
    const val DURATION_MS = 2_200L
    const val CONTENT_FADE_MS = 180L
    const val PLATE_FADE_MS = 220L
    const val LOCK_APPEAR_MS = 130L
    const val SHACKLE_OPEN_END_MS = 450L
    const val GLOW_FADE_END_MS = 1_800L
    const val TEXT_IN_START_MS = 600L
    const val TEXT_IN_END_MS = 900L
    const val SHIMMER_START_MS = 900L
    const val SHIMMER_END_MS = 1_500L
    const val HOLD_START_MS = 2_000L

    /** Remove-animations static frame: shown at once, held, then dismissed without slide. */
    const val STATIC_HOLD_MS = 2_000L
    /** TalkBack on: hold long enough for the announcement (tap still skips). */
    const val TALKBACK_HOLD_MS = 4_000L

    enum class Source {
        /** billing.purchase callback said Owned, with no PENDING seen in this sheet. The only source that plays. */
        PURCHASE,
        /** Restore purchases (sheet or Settings row). */
        RESTORE,
        /** A purchase that went PENDING first and was confirmed later. */
        PENDING_CONFIRMED,
        /** Entitlement refresh / reinstall / new device. */
        REFRESH
    }

    enum class Mode { NONE, ANIMATED, STATIC }

    /** Pure decision; writes nothing. */
    fun decide(
        source: Source,
        store: ProStore,
        activityFinishing: Boolean,
        animatorsEnabled: Boolean,
        lowRamDevice: Boolean
    ): Mode {
        if (source != Source.PURCHASE) return Mode.NONE
        if (activityFinishing) return Mode.NONE
        if (store.getBoolean(KEY_SHOWN, false)) return Mode.NONE
        return if (!animatorsEnabled || lowRamDevice) Mode.STATIC else Mode.ANIMATED
    }

    /**
     * Decide and, when it will play, persist [KEY_SHOWN] = true first (synchronous commit). The caller only
     * starts drawing after this returns. Never cleared by code, not even on restore.
     */
    fun claim(
        source: Source,
        store: ProStore,
        activityFinishing: Boolean,
        animatorsEnabled: Boolean,
        lowRamDevice: Boolean
    ): Mode {
        val m = decide(source, store, activityFinishing, animatorsEnabled, lowRamDevice)
        if (m != Mode.NONE) store.commitBoolean(KEY_SHOWN, true)
        return m
    }

    /** How long the end frame stays before the sheet dismisses itself. */
    fun holdMs(mode: Mode, talkBackOn: Boolean): Long = when {
        mode == Mode.NONE -> 0L
        talkBackOn -> TALKBACK_HOLD_MS
        mode == Mode.STATIC -> STATIC_HOLD_MS
        else -> DURATION_MS
    }

    /** Source for a purchase callback result: Owned after a Pending in the same sheet counts as PENDING_CONFIRMED. */
    fun sourceForPurchase(sawPendingThisSheet: Boolean): Source =
        if (sawPendingThisSheet) Source.PENDING_CONFIRMED else Source.PURCHASE
}
