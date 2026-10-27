package com.ccos.retro.pro

import com.ccos.retro.model.AppPrefs
import kotlin.math.pow

/**
 * vc159 (Da Vinci call on cb1f491): the upsell buy button uses the theme accent only when
 * label-on-fill >= 4.5:1 AND fill-vs-sheet >= 3:1 (WCAG 2.x relative luminance); otherwise the
 * fixed fallback fill #90E0FF with label #1A1A22. Computed per theme, never hand-picked.
 * Pure ARGB ints so it runs in JVM tests.
 */
object ProContrast {
    const val SHEET_BG = 0xFF0F1620.toInt()
    const val ACCENT_LABEL = 0xFF0B0F14.toInt()
    const val FALLBACK_FILL = 0xFF90E0FF.toInt()
    const val FALLBACK_LABEL = 0xFF1A1A22.toInt()
    const val MIN_LABEL = 4.5
    const val MIN_FILL = 3.0

    /** Theme accent (Settings title colour, matches MainActivity.applyConsoleSkin). */
    fun accentFor(skin: String): Int = when (skin) {
        AppPrefs.CONSOLE_SKIN_ROS -> 0xFFE8F0D8.toInt()
        AppPrefs.CONSOLE_SKIN_SPACEX -> 0xFF00D26A.toInt()
        AppPrefs.CONSOLE_SKIN_NASA -> 0xFF6A9AD4.toInt()
        AppPrefs.CONSOLE_SKIN_CNSA -> 0xFFDE2910.toInt()
        AppPrefs.CONSOLE_SKIN_ARIANE -> 0xFFFFD100.toInt()
        AppPrefs.CONSOLE_SKIN_RLAB -> 0xFFFF5A1F.toInt()
        AppPrefs.CONSOLE_SKIN_ULA -> 0xFFF6A800.toInt()
        AppPrefs.CONSOLE_SKIN_JAXA -> 0xFFE10600.toInt()
        AppPrefs.CONSOLE_SKIN_ISRO -> 0xFFFF671F.toInt()
        else -> 0xFFFFB000.toInt()
    }

    private fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** WCAG relative luminance of an opaque ARGB colour (alpha ignored). */
    fun luminance(argb: Int): Double =
        0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)

    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a); val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    data class ButtonColors(val fill: Int, val label: Int, val usesAccent: Boolean, val labelRatio: Double, val fillRatio: Double)

    fun buyButton(accent: Int, sheetBg: Int = SHEET_BG, accentLabel: Int = ACCENT_LABEL): ButtonColors {
        val lr = ratio(accentLabel, accent)
        val fr = ratio(accent, sheetBg)
        return if (lr >= MIN_LABEL && fr >= MIN_FILL) ButtonColors(accent, accentLabel, true, lr, fr)
        else ButtonColors(FALLBACK_FILL, FALLBACK_LABEL, false, ratio(FALLBACK_LABEL, FALLBACK_FILL), ratio(FALLBACK_FILL, sheetBg))
    }

    fun buyButtonForSkin(skin: String): ButtonColors = buyButton(accentFor(skin))

    /** vc160 Da Vinci Pro Gold v1 4.1: gold CTA for every skin; label #1A1200 (the guard still runs, gold passes). */
    const val GOLD = 0xFFFFD700.toInt()
    const val GOLD_LABEL = 0xFF1A1200.toInt()
    fun buyButtonGold(): ButtonColors = buyButton(GOLD, accentLabel = GOLD_LABEL)
}
