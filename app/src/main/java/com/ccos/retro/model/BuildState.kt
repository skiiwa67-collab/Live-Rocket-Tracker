package com.ccos.retro.model

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max
import kotlin.math.min

/**
 * Persistent state for the free Vector Rocket module.
 * Lives in the model layer; engine and UI only read/write through this.
 */
class BuildState(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("retro_command_state", Context.MODE_PRIVATE)

    var padProgress: Float
        get() = prefs.getFloat("pad", 0f)
        set(v) = prefs.edit().putFloat("pad", v.coerceIn(0f, 1f)).apply()

    var engineProgress: Float
        get() = prefs.getFloat("engine", 0f)
        set(v) = prefs.edit().putFloat("engine", v.coerceIn(0f, 1f)).apply()

    var rocketBodyProgress: Float
        get() = prefs.getFloat("body", 0f)
        set(v) = prefs.edit().putFloat("body", v.coerceIn(0f, 1f)).apply()

    var subsystemsProgress: Float
        get() = prefs.getFloat("sub", 0f)
        set(v) = prefs.edit().putFloat("sub", v.coerceIn(0f, 1f)).apply()

    var engineRndAlloc: Float
        get() = prefs.getFloat("eng_rnd", 0.30f)
        set(v) = prefs.edit().putFloat("eng_rnd", v.coerceIn(0f, 1f)).apply()

    var padRndAlloc: Float
        get() = prefs.getFloat("pad_rnd", 0.25f)
        set(v) = prefs.edit().putFloat("pad_rnd", v.coerceIn(0f, 1f)).apply()

    var bodyRndAlloc: Float
        get() = prefs.getFloat("body_rnd", 0.30f)
        set(v) = prefs.edit().putFloat("body_rnd", v.coerceIn(0f, 1f)).apply()

    var subRndAlloc: Float
        get() = prefs.getFloat("sub_rnd", 0.15f)
        set(v) = prefs.edit().putFloat("sub_rnd", v.coerceIn(0f, 1f)).apply()

    var personnelEngine: Float
        get() = prefs.getFloat("per_eng", 0.25f)
        set(v) = prefs.edit().putFloat("per_eng", v.coerceIn(0f, 1f)).apply()

    var personnelPad: Float
        get() = prefs.getFloat("per_pad", 0.25f)
        set(v) = prefs.edit().putFloat("per_pad", v.coerceIn(0f, 1f)).apply()

    var personnelBody: Float
        get() = prefs.getFloat("per_body", 0.25f)
        set(v) = prefs.edit().putFloat("per_body", v.coerceIn(0f, 1f)).apply()

    var personnelSub: Float
        get() = prefs.getFloat("per_sub", 0.25f)
        set(v) = prefs.edit().putFloat("per_sub", v.coerceIn(0f, 1f)).apply()

    var budget: Float
        get() = prefs.getFloat("budget", 1000f)
        set(v) = prefs.edit().putFloat("budget", max(0f, v)).apply()

    var activeModule: Int
        get() = prefs.getInt("active_mod", 0)
        set(v) = prefs.edit().putInt("active_mod", v.coerceIn(0, 7)).apply()

    var engagement: Float
        get() = prefs.getFloat("engage", 0.35f)
        set(v) = prefs.edit().putFloat("engage", v.coerceIn(0.08f, 1.6f)).apply()

    var lastInteractTime: Long
        get() = prefs.getLong("last_interact", 0L)
        set(v) = prefs.edit().putLong("last_interact", v).apply()

    fun overallProgress(): Float =
        padProgress * 0.25f + engineProgress * 0.25f +
                rocketBodyProgress * 0.30f + subsystemsProgress * 0.20f

    fun isBuilding(): Boolean =
        padProgress < 1f || engineProgress < 1f ||
                rocketBodyProgress < 1f || subsystemsProgress < 1f

    fun registerInteraction() {
        val now = System.currentTimeMillis()
        lastInteractTime = now
        engagement = min(1.6f, engagement + 0.12f)
    }

    fun tick(deltaSeconds: Float) {
        val idleMs = System.currentTimeMillis() - lastInteractTime
        if (idleMs > 12_000) {
            engagement = max(0.08f, engagement - 0.015f * deltaSeconds)
        }

        if (budget <= 0f && engagement < 0.2f) return

        val engageBoost = 0.45f + engagement
        val efficiency = 0.0011f * deltaSeconds * engageBoost
        val cost = 0.28f * deltaSeconds
        val regen = (0.18f + engagement * 0.55f) * deltaSeconds

        if (padProgress < 1f && padRndAlloc > 0.01f) {
            padProgress = min(1f, padProgress + efficiency * padRndAlloc * (0.5f + personnelPad))
            budget -= cost * padRndAlloc
        }
        if (engineProgress < 1f && engineRndAlloc > 0.01f) {
            engineProgress = min(1f, engineProgress + efficiency * engineRndAlloc * (0.5f + personnelEngine))
            budget -= cost * engineRndAlloc
        }
        if (rocketBodyProgress < 1f && bodyRndAlloc > 0.01f) {
            rocketBodyProgress = min(1f, rocketBodyProgress + efficiency * bodyRndAlloc * (0.5f + personnelBody))
            budget -= cost * bodyRndAlloc
        }
        if (subsystemsProgress < 1f && subRndAlloc > 0.01f) {
            subsystemsProgress = min(1f, subsystemsProgress + efficiency * subRndAlloc * (0.5f + personnelSub))
            budget -= cost * subRndAlloc
        }

        if (engagement > 0.6f) {
            personnelEngine = min(1f, personnelEngine + 0.0004f * deltaSeconds * engagement)
            personnelPad = min(1f, personnelPad + 0.0004f * deltaSeconds * engagement)
            personnelBody = min(1f, personnelBody + 0.0004f * deltaSeconds * engagement)
            personnelSub = min(1f, personnelSub + 0.0004f * deltaSeconds * engagement)
        }

        budget += regen
        if (budget > 2500f) budget = 2500f
    }

    fun resetProgress() {
        padProgress = 0f
        engineProgress = 0f
        rocketBodyProgress = 0f
        subsystemsProgress = 0f
        budget = 1000f
        engagement = 0.35f
    }
}
