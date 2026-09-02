package com.ccos.retro.geo

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/**
 * Live wallpaper is fullscreen. The Google search widget and Phone/Messages
 * hotseat are launcher chrome, not the nav pill. System insets alone are
 * nav-sized. This packer reads WindowInsetsCompat (gestures, tappable, stable,
 * cutout, overlays) plus the default HOME launcher's own dimen resources and
 * the published QSB widget minHeight. No invented pixel constants.
 */
object LauncherChrome {

    private val HOTSEAT_DIMENS = arrayOf(
        "hotseat_bar_size", "hotseat_height", "hotseat_size",
        "dynamic_grid_hotseat_size", "hotseat_bar_height", "hotseat_layout_height"
    )
    private val QSB_DIMENS = arrayOf(
        "qsb_widget_height", "qsb_height", "search_widget_hotseat_height",
        "hotseat_qsb_height", "qsb_container_height", "qsb_focus_height"
    )
    private val SPACE_DIMENS = arrayOf(
        "hotseat_qsb_space", "workspace_bottom_padding",
        "hotseat_bar_bottom_space", "hotseat_extra_vertical_size"
    )

    fun bottomGap(ctx: Context, engineInsets: WindowInsets?): Float {
        val chrome = max(packEngine(engineInsets), packWindowMetrics(ctx))
        val nav = navBand(ctx, engineInsets)
        val workspace = workspaceAboveNav(ctx)
        val gap = when {
            workspace > 0f && chrome + 1f >= nav + workspace -> chrome
            workspace > 0f -> chrome + workspace
            else -> chrome
        }
        return gap.coerceAtLeast(nav)
    }

    private fun packEngine(insets: WindowInsets?): Float {
        if (insets == null) return 0f
        return packCompat(WindowInsetsCompat.toWindowInsetsCompat(insets)).toFloat()
    }

    private fun packWindowMetrics(ctx: Context): Float {
        if (Build.VERSION.SDK_INT < 30) return 0f
        return try {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            packCompat(WindowInsetsCompat.toWindowInsetsCompat(wm.currentWindowMetrics.windowInsets)).toFloat()
        } catch (_: Exception) {
            0f
        }
    }

    private fun packCompat(compat: WindowInsetsCompat): Int {
        val mask = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.navigationBars() or
            WindowInsetsCompat.Type.systemGestures() or
            WindowInsetsCompat.Type.mandatorySystemGestures() or
            WindowInsetsCompat.Type.tappableElement() or
            WindowInsetsCompat.Type.displayCutout()
        var b = max(
            max(compat.getInsets(mask).bottom, compat.getInsetsIgnoringVisibility(mask).bottom),
            compat.stableInsetBottom
        )
        if (Build.VERSION.SDK_INT >= 31) {
            val ov = WindowInsetsCompat.Type.systemOverlays()
            b = max(b, max(compat.getInsets(ov).bottom, compat.getInsetsIgnoringVisibility(ov).bottom))
        }
        val cut = compat.displayCutout?.safeInsetBottom ?: 0
        return max(b, cut)
    }

    private fun navBand(ctx: Context, engineInsets: WindowInsets?): Float {
        var nav = 0f
        if (engineInsets != null) {
            nav = engineInsets.systemWindowInsetBottom.toFloat()
            if (Build.VERSION.SDK_INT >= 30) {
                nav = max(
                    nav,
                    engineInsets.getInsets(WindowInsets.Type.navigationBars()).bottom.toFloat()
                )
            }
        }
        val id = ctx.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id != 0) nav = max(nav, ctx.resources.getDimension(id))
        return nav
    }

    /**
     * Hotseat + Google search row from the default HOME package and the
     * published QSB widget. Those are the launcher's sizes, not ours.
     */
    private fun workspaceAboveNav(ctx: Context): Float {
        val fromHome = homeLauncherWorkspace(ctx)
        val qsb = publishedSearchHeight(ctx)
        val icon = ctx.resources.getDimension(android.R.dimen.app_icon_size)
        return when {
            fromHome > 0f && qsb > 0f -> max(fromHome, qsb + icon)
            fromHome > 0f -> fromHome
            qsb > 0f -> qsb + icon
            else -> icon
        }
    }

    private fun homeLauncherWorkspace(ctx: Context): Float {
        val pm = ctx.packageManager
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = try {
            pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (_: Exception) {
            null
        } ?: return 0f
        val pkg = ri.activityInfo?.packageName ?: return 0f
        if (pkg == ctx.packageName) return 0f
        return try {
            val res = pm.getResourcesForApplication(pkg)
            fun dim(name: String): Float {
                val id = res.getIdentifier(name, "dimen", pkg)
                if (id == 0) return 0f
                return try {
                    res.getDimension(id)
                } catch (_: Exception) {
                    0f
                }
            }
            val hotseat = HOTSEAT_DIMENS.maxOf { dim(it) }
            val qsb = QSB_DIMENS.maxOf { dim(it) }
            val space = SPACE_DIMENS.maxOf { dim(it) }
            if (hotseat <= 0f && qsb <= 0f) 0f else hotseat + qsb + space
        } catch (_: Exception) {
            0f
        }
    }

    private fun publishedSearchHeight(ctx: Context): Float {
        return try {
            val mm = AppWidgetManager.getInstance(ctx)
            var best = 0
            for (info in mm.installedProviders) {
                val p = info.provider.packageName.lowercase()
                val c = info.provider.className.lowercase()
                val search = "googlequicksearchbox" in p ||
                    "searchwidget" in c ||
                    ("qsb" in c && "google" in p)
                if (search) best = max(best, info.minHeight)
            }
            best.toFloat()
        } catch (_: Exception) {
            0f
        }
    }
}
