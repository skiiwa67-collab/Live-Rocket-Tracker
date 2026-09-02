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

    /**
     * Pixel glues the Google search bar to the FIRST home page only.
     * Extra pages (the HUD product page) do not have that bar.
     *
     * auto = insets of THIS page: search row only when [page] is 0.
     * dock = hotseat + nav override.
     * dock_search = force search + hotseat + nav (page 0 look) as override.
     */
    fun bottomGap(
        ctx: Context,
        engineInsets: WindowInsets?,
        mode: String = "auto",
        page: Int = 0
    ): Float {
        val chrome = max(packEngine(engineInsets), packWindowMetrics(ctx))
        val nav = navBand(ctx, engineInsets)
        val hotseat = hotseatRow(ctx)
        val search = searchRow(ctx)
        val dockGap = when {
            hotseat > 0f && chrome + 1f >= nav + hotseat -> chrome
            else -> chrome + hotseat
        }
        val searchGap = max(dockGap, nav + hotseat + search)
        val gap = when (mode) {
            "dock" -> dockGap
            "dock_search" -> searchGap
            else -> if (page <= 0) searchGap else dockGap
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
        val raw = compat.toWindowInsets()
        if (raw != null && Build.VERSION.SDK_INT >= 31) {
            val ov = WindowInsets.Type.systemOverlays()
            b = max(
                b,
                max(raw.getInsets(ov).bottom, raw.getInsetsIgnoringVisibility(ov).bottom)
            )
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

    private fun hotseatRow(ctx: Context): Float {
        val fromHome = homeDims(ctx, HOTSEAT_DIMENS)
        val icon = ctx.resources.getDimension(android.R.dimen.app_icon_size)
        return if (fromHome > 0f) fromHome else icon
    }

    private fun searchRow(ctx: Context): Float {
        val fromHome = homeDims(ctx, QSB_DIMENS) + homeDims(ctx, SPACE_DIMENS)
        return max(fromHome, publishedSearchHeight(ctx))
    }

    private fun homeDims(ctx: Context, names: Array<String>): Float {
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
            names.maxOf { name ->
                val id = res.getIdentifier(name, "dimen", pkg)
                if (id == 0) 0f else try {
                    res.getDimension(id)
                } catch (_: Exception) {
                    0f
                }
            }
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
