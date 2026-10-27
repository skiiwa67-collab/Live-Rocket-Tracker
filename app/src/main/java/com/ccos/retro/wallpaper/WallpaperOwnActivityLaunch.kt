package com.ccos.retro.wallpaper

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Opens this app's own activity from the live wallpaper.
 *
 * Direct startActivity is blocked as a background activity launch on API 31+
 * (BOUND_FOREGROUND_SERVICE, no visible window, originatingPendingIntent null).
 * The platform path is a self-created PendingIntent with creator + sender BAL
 * opt-in: [ActivityOptions.setPendingIntentBackgroundActivityLaunchAllowed]
 * on API 30–33, then [ActivityOptions.setPendingIntentCreatorBackgroundActivityStartMode]
 * / [ActivityOptions.setPendingIntentBackgroundActivityStartMode] on 34+.
 * ALLOW_IF_VISIBLE would fail here — the wallpaper is not an app-visible window —
 * so API 34–35 use MODE_BACKGROUND_ACTIVITY_START_ALLOWED and API 36+ uses
 * MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS.
 */
object WallpaperOwnActivityLaunch {
    private const val TAG = "WallpaperOwnActivity"
    private const val REQ_OPEN_OWN = 1591

    fun start(context: Context, intent: Intent) {
        try {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getActivity(
                context,
                REQ_OPEN_OWN,
                intent,
                flags,
                creatorOptions()?.toBundle(),
            )
            if (pi == null) {
                Log.w(TAG, "CMD double-tap: PendingIntent.getActivity returned null")
                return
            }
            val sender = senderOptions()?.toBundle()
            if (sender != null) {
                pi.send(context, 0, null, null, null, null, sender)
            } else {
                pi.send()
            }
        } catch (e: Exception) {
            Log.w(TAG, "CMD double-tap failed to open ${intent.component}", e)
        }
    }

    private fun creatorOptions(): ActivityOptions? {
        val opts = ActivityOptions.makeBasic()
        return if (applyCreatorOptIn(opts)) opts else null
    }

    private fun senderOptions(): ActivityOptions? {
        val opts = ActivityOptions.makeBasic()
        return if (applySenderOptIn(opts)) opts else null
    }

    private fun applyCreatorOptIn(opts: ActivityOptions): Boolean {
        val sdk = Build.VERSION.SDK_INT
        return when (WallpaperCmdTap.balOptIn(sdk)) {
            WallpaperCmdTap.BalOptIn.NONE -> false
            WallpaperCmdTap.BalOptIn.BOOLEAN_ALLOWED -> {
                @Suppress("DEPRECATION")
                opts.setPendingIntentBackgroundActivityLaunchAllowed(true)
                true
            }
            WallpaperCmdTap.BalOptIn.MODE_ALLOWED,
            WallpaperCmdTap.BalOptIn.MODE_ALLOW_ALWAYS -> {
                opts.setPendingIntentCreatorBackgroundActivityStartMode(backgroundStartMode(sdk))
                true
            }
        }
    }

    private fun applySenderOptIn(opts: ActivityOptions): Boolean {
        val sdk = Build.VERSION.SDK_INT
        return when (WallpaperCmdTap.balOptIn(sdk)) {
            WallpaperCmdTap.BalOptIn.NONE -> false
            WallpaperCmdTap.BalOptIn.BOOLEAN_ALLOWED -> {
                @Suppress("DEPRECATION")
                opts.setPendingIntentBackgroundActivityLaunchAllowed(true)
                true
            }
            WallpaperCmdTap.BalOptIn.MODE_ALLOWED,
            WallpaperCmdTap.BalOptIn.MODE_ALLOW_ALWAYS -> {
                opts.setPendingIntentBackgroundActivityStartMode(backgroundStartMode(sdk))
                true
            }
        }
    }

    private fun backgroundStartMode(sdk: Int): Int {
        return if (sdk >= 36) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            @Suppress("DEPRECATION")
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
    }
}
