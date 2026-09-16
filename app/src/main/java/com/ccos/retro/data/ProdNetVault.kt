package com.ccos.retro.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Tip 121: durable last-known-good **prod** NET/window/status per launch id.
 * lldev must never own display NET — vault + live prod-by-id win.
 */
object ProdNetVault {
    private const val TAG = "CCOS.ProdNetVault"
    private const val FILE = "lrt_prod_net_vault.json"

    data class Entry(
        val netMs: Long,
        val windowStartMs: Long,
        val windowEndMs: Long,
        val statusName: String,
        val statusAbbrev: String,
        val savedAtMs: Long
    )

    private val map = ConcurrentHashMap<String, Entry>()
    @Volatile private var file: File? = null

    fun bind(context: Context) {
        val f = File(context.applicationContext.filesDir, FILE)
        // Tip 123: wipe once — tip 122 could poison vault via isProdSourceTag("lldev+prod-net").
        val prefs = context.applicationContext.getSharedPreferences("ccos_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("prod_net_vault_v123", false)) {
            map.clear()
            if (f.exists()) {
                f.delete()
                Log.i(TAG, "tip123 wiped poisoned vault file")
            }
            prefs.edit().putBoolean("prod_net_vault_v123", true).apply()
        }
        file = f
        load(f)
    }

    fun get(id: String): Entry? = map[id]

    fun rememberProd(s: LaunchSnapshot) {
        if (s.id.isBlank() || s.id.startsWith("demo-")) return
        map[s.id] = Entry(
            netMs = s.netMs,
            windowStartMs = s.windowStartMs,
            windowEndMs = s.windowEndMs,
            statusName = s.statusName,
            statusAbbrev = s.statusAbbrev,
            savedAtMs = System.currentTimeMillis()
        )
        persist()
    }

    fun rememberProdList(launches: List<LaunchSnapshot>) {
        var n = 0
        for (s in launches) {
            if (s.id.startsWith("demo-")) continue
            map[s.id] = Entry(
                netMs = s.netMs,
                windowStartMs = s.windowStartMs,
                windowEndMs = s.windowEndMs,
                statusName = s.statusName,
                statusAbbrev = s.statusAbbrev,
                savedAtMs = System.currentTimeMillis()
            )
            n++
        }
        if (n > 0) persist()
    }

    /** Overlay vault NET/window/status onto [base] (structure from any source). */
    fun applyTo(base: LaunchSnapshot): LaunchSnapshot {
        val e = map[base.id] ?: return base
        return base.copy(
            netMs = e.netMs,
            windowStartMs = e.windowStartMs,
            windowEndMs = e.windowEndMs,
            statusName = e.statusName.ifBlank { base.statusName },
            statusAbbrev = e.statusAbbrev.ifBlank { base.statusAbbrev }
        )
    }

    private fun load(f: File) {
        try {
            if (!f.exists()) return
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("entries") ?: return
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isBlank()) continue
                map[id] = Entry(
                    netMs = o.optLong("netMs"),
                    windowStartMs = o.optLong("windowStartMs"),
                    windowEndMs = o.optLong("windowEndMs"),
                    statusName = o.optString("statusName"),
                    statusAbbrev = o.optString("statusAbbrev"),
                    savedAtMs = o.optLong("savedAtMs")
                )
            }
            Log.i(TAG, "loaded ${map.size} prod NET entries")
        } catch (e: Exception) {
            Log.w(TAG, "load: ${e.message}")
        }
    }

    @Synchronized
    private fun persist() {
        val f = file ?: return
        try {
            val arr = JSONArray()
            for ((id, e) in map) {
                arr.put(
                    JSONObject()
                        .put("id", id)
                        .put("netMs", e.netMs)
                        .put("windowStartMs", e.windowStartMs)
                        .put("windowEndMs", e.windowEndMs)
                        .put("statusName", e.statusName)
                        .put("statusAbbrev", e.statusAbbrev)
                        .put("savedAtMs", e.savedAtMs)
                )
            }
            f.writeText(JSONObject().put("entries", arr).toString())
        } catch (e: Exception) {
            Log.w(TAG, "persist: ${e.message}")
        }
    }
}
