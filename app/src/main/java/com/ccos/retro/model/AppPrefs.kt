package com.ccos.retro.model

import android.content.Context
import android.content.SharedPreferences

class AppPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ccos_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MODULE_ROCKET = "vector_rocket"
        const val MODULE_SYSTEM = "system_metrics"
        const val MODULE_TELEMETRY = "rocket_telemetry"

        /** Max launcher page index for side-button placement (matches Settings UI). */
        const val MAX_COMMAND_PAGE_INDEX = 12

        const val DATA_AUTO = "auto"
        const val DATA_TELEMETRY = "telemetry"
        const val DATA_SYSTEM = "system"
        const val DATA_ROCKET = "rocket"
        const val DATA_TRAJ = "traj"
        const val DATA_STG1 = "stg1"
        const val DATA_STG2 = "stg2"
        const val DATA_ENG = "eng"
        const val DATA_PROP = "prop"

        val LAMP_STEPS = floatArrayOf(0.45f, 0.75f, 1.0f)
        val TEXT_STEPS_SYS = floatArrayOf(1.05f, 1.70f, 2.40f)
        val TEXT_STEPS_TEL = floatArrayOf(3.2f, 5.6f, 8.4f)
        val ROCKER_LABELS_LAMP = arrayOf("DIM", "NORM", "BRIGHT")
        val ROCKER_LABELS_TEXT = arrayOf("SM", "MD", "LG")
    }

    var activeModuleId: String
        get() = prefs.getString("module_id", MODULE_TELEMETRY) ?: MODULE_TELEMETRY
        set(v) = prefs.edit().putString("module_id", v).apply()

    var showButtons: Boolean
        get() = prefs.getBoolean("show_buttons", true)
        set(v) = prefs.edit().putBoolean("show_buttons", v).apply()

    var systemAnalog: Boolean
        get() = prefs.getBoolean("sys_analog", true)
        set(v) = prefs.edit().putBoolean("sys_analog", v).apply()

    var systemTheme: Int
        get() = prefs.getInt("sys_theme", 1)
        set(v) = prefs.edit().putInt("sys_theme", v.coerceIn(0, 7)).apply()

    var lampBrightness: Float
        get() = prefs.getFloat("lamp", 1.0f)
        set(v) = prefs.edit().putFloat("lamp", v.coerceIn(0.2f, 1f)).apply()

    fun lampStepIndex(): Int = nearestStep(lampBrightness, LAMP_STEPS)

    fun setLampStep(index: Int) {
        lampBrightness = LAMP_STEPS[index.coerceIn(0, LAMP_STEPS.lastIndex)]
    }

    fun textSteps(): FloatArray =
        if (isTelemetry()) TEXT_STEPS_TEL else TEXT_STEPS_SYS

    fun textStepIndex(): Int = nearestStep(textScale, textSteps())

    fun setTextStep(index: Int) {
        val steps = textSteps()
        textScale = steps[index.coerceIn(0, steps.lastIndex)]
    }

    private fun nearestStep(value: Float, steps: FloatArray): Int {
        var best = 0
        var bestDist = Float.MAX_VALUE
        for (i in steps.indices) {
            val d = kotlin.math.abs(steps[i] - value)
            if (d < bestDist) {
                bestDist = d
                best = i
            }
        }
        return best
    }

    /**
     * Per-module HUD text scale — each module remembers its last setting so
     * switching Live Tele ↔ System Metrics never inherits a "stupid large" value.
     */
    var textScale: Float
        get() = when (activeModuleId) {
            MODULE_TELEMETRY -> {
                bumpTelOffSm()
                prefs.getFloat("text_scale_tel", 5.6f).coerceIn(2.8f, 9.0f)
            }
            MODULE_SYSTEM -> prefs.getFloat("text_scale_sys", 1.8f).coerceIn(0.9f, 2.6f)
            else -> prefs.getFloat("text_scale_rkt", 1.8f).coerceIn(0.9f, 2.6f)
        }
        set(v) {
            when (activeModuleId) {
                MODULE_TELEMETRY ->
                    prefs.edit().putFloat("text_scale_tel", v.coerceIn(2.8f, 9.0f)).apply()
                MODULE_SYSTEM ->
                    prefs.edit().putFloat("text_scale_sys", v.coerceIn(0.9f, 2.6f)).apply()
                else ->
                    prefs.edit().putFloat("text_scale_rkt", v.coerceIn(0.9f, 2.6f)).apply()
            }
        }

    /** Old factory default 3.6 snapped to SM. One bump to MD. Do not guess LG. */
    private fun bumpTelOffSm() {
        if (prefs.getBoolean("text_scale_tel_off_sm_v1", false)) return
        val e = prefs.edit()
        if (prefs.contains("text_scale_tel")) {
            val v = prefs.getFloat("text_scale_tel", 5.6f)
            if (v <= 4.0f) e.putFloat("text_scale_tel", 5.6f)
        }
        e.putBoolean("text_scale_tel_off_sm_v1", true).apply()
    }



    /** Launcher page index where side buttons appear (0 = leftmost). */
    var commandPageIndex: Int
        get() = prefs.getInt("cmd_page", 0).coerceIn(0, MAX_COMMAND_PAGE_INDEX)
        set(v) = prefs.edit().putInt("cmd_page", v.coerceIn(0, MAX_COMMAND_PAGE_INDEX)).apply()

    fun restoreCommandPageHud() {
        // no-op. First-run SetupActivity owns page placement.
    }

    var setupComplete: Boolean
        get() = prefs.getBoolean("ccos_setup_v1", false)
        set(v) = prefs.edit().putBoolean("ccos_setup_v1", v).apply()

    // --- Live Rocket Telemetry ---
    /** Selected launch id from LL2, or blank for auto/default. */
    var telemetryLaunchId: String
        get() = prefs.getString("tel_launch_id", "") ?: ""
        set(v) = prefs.edit().putString("tel_launch_id", v).apply()

    /** AUTO double-tap pin: current flight across wallpaper + MCC. Does not expire. */
    /**
     * One UTC T0 per launch.
     * Live: the book's netMs. Persist so a later empty fetch does not invent a local clock.
     * Demo: catalog used to bake now±offset per process — pin the first UTC so this device
     * does not start a second T0 when the wallpaper reopens.
     */
    fun pinnedNetMs(launchId: String, incoming: Long): Long {
        if (launchId.isBlank()) return incoming
        val key = "t0_$launchId"
        val have = prefs.getLong(key, 0L)
        val demo = launchId.startsWith("demo-")
        if (demo) {
            if (have > 0L) return have
            if (incoming > 0L) prefs.edit().putLong(key, incoming).apply()
            return incoming
        }
        if (incoming > 0L) {
            if (have != incoming) prefs.edit().putLong(key, incoming).apply()
            return incoming
        }
        return if (have > 0L) have else incoming
    }

    fun usingFallbackT0(launchId: String, incoming: Long): Boolean {
        if (launchId.isBlank()) return incoming <= 0L
        if (launchId.startsWith("demo-")) return true
        return incoming <= 0L && prefs.getLong("t0_$launchId", 0L) > 0L
    }

    /** Shared event-walk cursor so wallpaper +/− and MCC PREV/NEXT stay on the same mark. */
    fun eventCursorSec(launchId: String): Float? {
        if (launchId.isBlank()) return null
        val key = "evt_cur_$launchId"
        if (!prefs.contains(key)) return null
        return prefs.getFloat(key, 0f)
    }

    fun setEventCursorSec(launchId: String, sec: Float?) {
        if (launchId.isBlank()) return
        val key = "evt_cur_$launchId"
        if (sec == null) prefs.edit().remove(key).apply()
        else prefs.edit().putFloat(key, sec).apply()
    }

    var telemetryPinned: Boolean
        get() = prefs.getBoolean("tel_pinned", false)
        set(v) = prefs.edit().putBoolean("tel_pinned", v).apply()

    /** Auto-switch to next upcoming launch. Browse mode. HOLD beats this. */
    var telemetryAuto: Boolean
        get() = prefs.getBoolean("tel_auto", true)
        set(v) = prefs.edit().putBoolean("tel_auto", v).apply()

    /** Epoch ms when HOLD expires. 0 = not holding. */
    var telemetryHoldUntilMs: Long
        get() = prefs.getLong("tel_hold_until", 0L)
        set(v) = prefs.edit().putLong("tel_hold_until", v).apply()

    /** HOLD length: 2h default, also 6h / 2d. */
    var telemetryHoldDurationMs: Long
        get() = prefs.getLong("tel_hold_dur", 2L * 3600_000L).coerceIn(60_000L, 3L * 86400_000L)
        set(v) = prefs.edit().putLong("tel_hold_dur", v.coerceIn(60_000L, 3L * 86400_000L)).apply()

    /** true = mph (and mi); false = km/h (and km). Default imperial for US. */
    var useImperial: Boolean
        get() = prefs.getBoolean("use_imperial", true)
        set(v) = prefs.edit().putBoolean("use_imperial", v).apply()

    /** true = analog gauges on TEL; false = digital-only readouts. */
    var telemetryAnalog: Boolean
        get() = prefs.getBoolean("tel_analog", true)
        set(v) = prefs.edit().putBoolean("tel_analog", v).apply()

    var extraScreens: Boolean
        get() = prefs.getBoolean("tel_extra_screens", false)
        set(v) = prefs.edit().putBoolean("tel_extra_screens", v).apply()

    var extraScreenTraj: Boolean
        get() = prefs.getBoolean("tel_x_traj", true)
        set(v) = prefs.edit().putBoolean("tel_x_traj", v).apply()

    var extraScreenStg: Boolean
        get() = prefs.getBoolean("tel_x_stg", true)
        set(v) = prefs.edit().putBoolean("tel_x_stg", v).apply()

    var extraScreenEng: Boolean
        get() = prefs.getBoolean("tel_x_eng", false)
        set(v) = prefs.edit().putBoolean("tel_x_eng", v).apply()

    var extraScreenProp: Boolean
        get() = prefs.getBoolean("tel_x_prop", false)
        set(v) = prefs.edit().putBoolean("tel_x_prop", v).apply()

    /** 1 = booster / first, 2 = upper. Tap side rockets. */
    var trackedStage: Int
        get() = prefs.getInt("tel_stage", 1).coerceIn(1, 2)
        set(v) = prefs.edit().putInt("tel_stage", v.coerceIn(1, 2)).apply()

    /** Upcoming window: 7 / 30 / 180 days. */
    var telemetryHorizonDays: Int
        get() = prefs.getInt("tel_horizon_days", 30).coerceIn(7, 180)
        set(v) = prefs.edit().putInt("tel_horizon_days", v.coerceIn(7, 180)).apply()

    /** "current" or "historical" picker tab. */
    var telemetryListMode: String
        get() = prefs.getString("tel_list_mode", "current") ?: "current"
        set(v) = prefs.edit().putString("tel_list_mode", v).apply()

    /** true = 8 buttons on every settled screen. First-install default. Samsung-safe. */
    var hudEveryScreen: Boolean
        get() = prefs.getBoolean("hud_every_screen", true)
        set(v) = prefs.edit().putBoolean("hud_every_screen", v).apply()

    var launcherPageCount: Int
        get() = prefs.getInt("launcher_pages", 1).coerceIn(1, 12)
        set(v) = prefs.edit().putInt("launcher_pages", v.coerceIn(1, 12)).apply()

    /**
     * HUD bottom gap. Auto = this page: search row only on first home
     * (Pixel glued bar). Extra pages get dock + nav. Dock-only / Dock+search
     * are overrides.
     */
    var bottomGapMode: String
        get() = when (prefs.getString("bottom_gap_mode", "auto")) {
            "dock", "dock_search" -> prefs.getString("bottom_gap_mode", "auto")!!
            else -> "auto"
        }
        set(v) = prefs.edit().putString(
            "bottom_gap_mode",
            when (v) { "dock", "dock_search" -> v; else -> "auto" }
        ).apply()

    /**
     * Look-only data for a specific launcher page.
     * auto = follow the active module.
     */
    fun offPageData(page: Int): String {
        return prefs.getString("off_page_$page", null)
            ?: prefs.getString("off_page_data", DATA_AUTO)
            ?: DATA_AUTO
    }

    fun setOffPageData(page: Int, mode: String) {
        prefs.edit().putString("off_page_$page", mode).apply()
    }

    fun isRocket(): Boolean = activeModuleId == MODULE_ROCKET
    fun isSystem(): Boolean = activeModuleId == MODULE_SYSTEM
    fun isTelemetry(): Boolean = activeModuleId == MODULE_TELEMETRY

    /** Fresh install and this rebuild: live telemetry, Current + Auto. */
    fun ensurePaidAppDefaults() {
        if (prefs.getBoolean("ccos_first_run_v3", false)) return
        prefs.edit()
            .putString("module_id", MODULE_TELEMETRY)
            .putString("tel_list_mode", "current")
            .putBoolean("tel_auto", true)
            .putString("tel_launch_id", "")
            .putBoolean("tel_pinned", false)
            .putLong("tel_hold_until", 0L)
            .putInt("cmd_page", 0)
            .putBoolean("hud_every_screen", true)
            .putBoolean("ccos_first_run_v3", true)
            .apply()
    }
}




