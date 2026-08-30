package com.ccos.retro.skin

import android.graphics.Color

/**
 * Color tokens for System Metrics skin.
 * Themes: 0 Cool, 1 Warm, 2 Neon, 3 Amber — match Flutter Command Center.
 */
object SystemSkin {

    data class Palette(
        val primary: Int,
        val accent: Int,
        val good: Int,
        val warn: Int,
        val dim: Int,
        val grid: Int,
        val text: Int,
        val panel: Int
    )

    fun palette(theme: Int, lamp: Float): Palette {
        val a = (lamp.coerceIn(0.2f, 1f) * 255).toInt()
        fun c(hex: String): Int {
            val base = Color.parseColor(hex)
            return Color.argb(a, Color.red(base), Color.green(base), Color.blue(base))
        }
        return when (theme) {
            0 -> Palette( // Cool cyan
                primary = c("#00E5FF"), accent = c("#4FC3F7"), good = c("#00FF88"),
                warn = c("#FF6B00"), dim = c("#1A3040"), grid = c("#0D1A22"),
                text = c("#B0E0FF"), panel = Color.parseColor("#CC05080C")
            )
            1 -> Palette( // Red / warm
                primary = c("#FF3B3B"), accent = c("#FF6B00"), good = c("#00FF88"),
                warn = c("#FFB000"), dim = c("#2A1010"), grid = c("#1A0808"),
                text = c("#FFB0B0"), panel = Color.parseColor("#CC0A0505")
            )
            2 -> Palette( // Neon
                primary = c("#39FF14"), accent = c("#FF00FF"), good = c("#39FF14"),
                warn = c("#FFFF00"), dim = c("#1A2A1A"), grid = c("#0A1A0A"),
                text = c("#C8FFC8"), panel = Color.parseColor("#CC050805")
            )
            3 -> Palette( // Amber
                primary = c("#FFB000"), accent = c("#FF6B00"), good = c("#FFD54F"),
                warn = c("#FF3D00"), dim = c("#2A1A0A"), grid = c("#1A1008"),
                text = c("#FFE0A0"), panel = Color.parseColor("#CC0A0805")
            )
            4 -> Palette( // Violet
                primary = c("#B388FF"), accent = c("#7C4DFF"), good = c("#69F0AE"),
                warn = c("#FF6E40"), dim = c("#1A1030"), grid = c("#100820"),
                text = c("#E0D0FF"), panel = Color.parseColor("#CC080510")
            )
            5 -> Palette( // Ice
                primary = c("#18FFFF"), accent = c("#00B8D4"), good = c("#00E676"),
                warn = c("#FFAB40"), dim = c("#0A2030"), grid = c("#061018"),
                text = c("#B2EBF2"), panel = Color.parseColor("#CC040A10")
            )
            6 -> Palette( // Pink
                primary = c("#FF80AB"), accent = c("#FF4081"), good = c("#69F0AE"),
                warn = c("#FFD740"), dim = c("#2A1020"), grid = c("#180810"),
                text = c("#FFD0E0"), panel = Color.parseColor("#CC100508")
            )
            7 -> Palette( // White / mono
                primary = c("#FFFFFF"), accent = c("#B0BEC5"), good = c("#A5D6A7"),
                warn = c("#FFE082"), dim = c("#263238"), grid = c("#1A1A1A"),
                text = c("#ECEFF1"), panel = Color.parseColor("#CC0A0A0A")
            )
            else -> Palette( // Warm
                primary = c("#FF3B3B"), accent = c("#FF6B00"), good = c("#00FF88"),
                warn = c("#FFB000"), dim = c("#2A1010"), grid = c("#1A0808"),
                text = c("#FFB0B0"), panel = Color.parseColor("#CC0A0505")
            )
        }
    }
}
