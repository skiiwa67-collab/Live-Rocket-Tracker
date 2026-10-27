package com.ccos.retro.wallpaper

/**
 * CMD tap on the live wallpaper: one tap toggles the flyout, two taps within
 * [DOUBLE_TAP_MS] open the app. Kept free of Android types so unit tests can
 * cover the decision without Robolectric.
 *
 * Launching the activity is [WallpaperOwnActivityLaunch] — a wallpaper
 * [android.content.Context.startActivity] is a background start on API 31+
 * (no app-visible window, input token already finished).
 * [android.app.WallpaperManager.COMMAND_TAP] is not a BAL exemption and is
 * not guaranteed for a consumed CMD hit.
 */
object WallpaperCmdTap {
    const val DOUBLE_TAP_MS = 400L

    enum class Decision { OPEN_APP, TOGGLE_FLYOUT }

    data class Result(val decision: Decision, val nextLastTapMs: Long) {
        val opensApp: Boolean get() = decision == Decision.OPEN_APP
    }

    /** Which ActivityOptions BAL opt-in the PendingIntent creator/sender must use. */
    enum class BalOptIn {
        NONE,
        BOOLEAN_ALLOWED,
        MODE_ALLOWED,
        MODE_ALLOW_ALWAYS,
    }

    fun decide(lastTapMs: Long, nowMs: Long, windowMs: Long = DOUBLE_TAP_MS): Result {
        if (lastTapMs > 0L && nowMs - lastTapMs <= windowMs) {
            return Result(Decision.OPEN_APP, nextLastTapMs = 0L)
        }
        return Result(Decision.TOGGLE_FLYOUT, nextLastTapMs = nowMs)
    }

    fun balOptIn(sdk: Int): BalOptIn = when {
        sdk >= 36 -> BalOptIn.MODE_ALLOW_ALWAYS
        sdk >= 34 -> BalOptIn.MODE_ALLOWED
        sdk >= 30 -> BalOptIn.BOOLEAN_ALLOWED
        else -> BalOptIn.NONE
    }
}
