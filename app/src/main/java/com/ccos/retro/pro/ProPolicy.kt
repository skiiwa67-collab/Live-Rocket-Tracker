package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * vc159 (R-NO-DEMO-NAMING steering): every Pro gate in the app goes through one of these hooks so the
 * gated surface is a closed, testable list. Gated for a free user ONLY:
 *  - starting a replay of a past launch that is not in the Included set (select, historic roll, picker row),
 *  - jump / event skip while such a locked replay is on screen,
 *  - manually picking a non-free console theme; Chris 8:22: Auto theming applies only free themes too.
 * Everything else (live + upcoming launches, live jump/skip, Starship F13, the free themes,
 * HISTORIC list + search, settings) never consults a gate.
 */
enum class ProHook(val kind: Kind) {
    /** MainActivity spinner pick and RocketTelemetryModule.selectLaunch (defense in depth). */
    SELECT_LAUNCH(Kind.LAUNCH),
    /** RocketTelemetryModule.maybeAdvanceHistoricRoll: rolls only land on unlocked launches. */
    HISTORIC_ROLL(Kind.LAUNCH),
    /** Historic picker row style (lock end zone vs Included pill). */
    PICKER_ROW(Kind.LAUNCH),
    /** RocketTelemetryModule.jumpTo + wallpaper CDT jump chips. */
    JUMP(Kind.JUMP_SKIP),
    /** RocketTelemetryModule.skipEvent + wallpaper/console event skip. */
    SKIP_EVENT(Kind.JUMP_SKIP),
    /** Settings console chips (MainActivity). */
    THEME_PICK_SETTINGS(Kind.THEME),
    /** Wallpaper Command flyout console chips. */
    THEME_PICK_WALLPAPER_FLYOUT(Kind.THEME),
    /** Wallpaper CONSOLES board chips. */
    THEME_PICK_WALLPAPER_BOARD(Kind.THEME);

    enum class Kind { LAUNCH, JUMP_SKIP, THEME }
}

object ProPolicy {
    /** true = allowed. [launch] is used by LAUNCH / JUMP_SKIP hooks, [skinId] + [store] by THEME hooks. */
    fun allows(
        hook: ProHook,
        isPro: Boolean,
        launch: LaunchSnapshot? = null,
        skinId: String? = null,
        store: ProStore? = null,
        now: Long = System.currentTimeMillis(),
        gatesOn: Boolean = ProConfig.gatesEnabled()
    ): Boolean {
        if (isPro || !gatesOn) return true // B4: release switch off = everything allowed
        return when (hook.kind) {
            ProHook.Kind.LAUNCH -> !ProGate.isLocked(launch, false, now)
            ProHook.Kind.JUMP_SKIP -> ProGate.canJumpOrSkip(launch, false, now)
            ProHook.Kind.THEME -> skinId == null || store == null || ThemeAccess.canPickManually(skinId, false, store)
        }
    }

    fun allowsLaunch(hook: ProHook, isPro: Boolean, launch: LaunchSnapshot?, now: Long = System.currentTimeMillis()) =
        allows(hook, isPro, launch = launch, now = now)

    fun allowsTheme(hook: ProHook, isPro: Boolean, skinId: String, store: ProStore) =
        allows(hook, isPro, skinId = skinId, store = store)
}
