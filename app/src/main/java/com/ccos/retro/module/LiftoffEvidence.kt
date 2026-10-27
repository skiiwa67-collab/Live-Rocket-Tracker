package com.ccos.retro.module

import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.pro.ProStore

/**
 * Helios P1-A: sticky per-launch "liftoff seen" flag, persisted in prefs so it survives a process restart.
 * Set when this app observed LL2 In Flight ([LiveClockGate.observedLiftoff]); webcast_live is not evidence (Skippy N2).
 * A plain LL2 Failure runs the clock only with this flag or a post-liftoff failreason.
 */
object LiftoffEvidence {
    const val KEY_SEEN_IDS = "liftoff_seen_ids"
    private const val MAX_IDS = 48

    private fun ids(store: ProStore): List<String> =
        store.getString(KEY_SEEN_IDS).orEmpty().split(',').filter { it.isNotBlank() }

    fun isSeen(store: ProStore, id: String): Boolean = id in ids(store)

    fun mark(store: ProStore, id: String) {
        if (id.isBlank() || isSeen(store, id)) return
        store.putString(KEY_SEEN_IDS, (ids(store) + id).takeLast(MAX_IDS).joinToString(","))
    }

    fun observedLiftoff(l: LaunchSnapshot, now: Long): Boolean = LiveClockGate.observedLiftoff(l, now)
}
