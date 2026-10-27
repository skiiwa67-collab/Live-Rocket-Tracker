package com.ccos.retro.pro

import com.ccos.retro.model.AppPrefs
import java.security.SecureRandom
import java.util.Random

/**
 * Console theme access (R-LRT-FREE-PRO, Chris 10-27):
 * free = NASA, SPACEX, ROS, CNSA, MCC + one European-style theme picked once per install.
 * Others are Pro for manual picking. Auto theming (AppPrefs.noteConsoleSkinTrackedLaunch) is unchanged:
 * a Pro theme Auto applies shows for that live launch but never becomes a free user's saved manual pick.
 */
object ThemeAccess {
    val FREE_BASE: Set<String> = setOf(
        AppPrefs.CONSOLE_SKIN_NASA, AppPrefs.CONSOLE_SKIN_SPACEX, AppPrefs.CONSOLE_SKIN_ROS,
        AppPrefs.CONSOLE_SKIN_CNSA, AppPrefs.CONSOLE_SKIN_MCC
    )

    /** TODO(davinci): more European-style theme art for this pool. Today only ARIANE. */
    val EURO_POOL: List<String> = listOf(AppPrefs.CONSOLE_SKIN_ARIANE)

    const val KEY_FREE_EURO = "pro_free_euro_skin"
    const val KEY_MANUAL_PICK = "pro_manual_console_skin"

    /** Sticky per install: picked on first call, persisted, never re-rolled while still in the pool. */
    fun freeEuroSkin(store: ProStore, rng: Random = SecureRandom()): String {
        val saved = store.getString(KEY_FREE_EURO)
        if (saved != null && saved in EURO_POOL) return saved
        val pick = EURO_POOL[rng.nextInt(EURO_POOL.size)]
        store.putString(KEY_FREE_EURO, pick)
        return pick
    }

    fun freeSet(store: ProStore): Set<String> = FREE_BASE + freeEuroSkin(store)

    fun canPickManually(id: String, isPro: Boolean, store: ProStore): Boolean {
        val n = AppPrefs.normalizeConsoleSkin(id)
        return isPro || n in freeSet(store)
    }

    /** Manual picker entry point. Returns false (and saves nothing) for a locked theme. TODO(davinci): locked chip + toast. */
    fun recordManualPick(store: ProStore, id: String, isPro: Boolean): Boolean {
        if (!canPickManually(id, isPro, store)) return false
        store.putString(KEY_MANUAL_PICK, AppPrefs.normalizeConsoleSkin(id))
        return true
    }

    /** Saved manual pick, or MCC if none / no longer allowed (e.g. Pro lapsed). */
    fun savedManualPick(store: ProStore, isPro: Boolean): String {
        val s = store.getString(KEY_MANUAL_PICK) ?: return AppPrefs.CONSOLE_SKIN_MCC
        return if (canPickManually(s, isPro, store)) s else AppPrefs.CONSOLE_SKIN_MCC
    }

    /** What shows during a live launch: Auto's choice for everyone (unchanged behaviour). */
    fun displayedDuringLive(autoSkin: String): String = AppPrefs.normalizeConsoleSkin(autoSkin)

    /**
     * When the Auto-themed live launch ends: keep [current] if the user may pick it, else fall back
     * to their saved manual pick. TODO(wire): call where the tracked launch leaves active watch.
     */
    fun skinAfterLiveEnds(current: String, isPro: Boolean, store: ProStore): String =
        if (canPickManually(current, isPro, store)) AppPrefs.normalizeConsoleSkin(current)
        else savedManualPick(store, isPro)
}
