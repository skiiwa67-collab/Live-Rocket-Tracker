package com.ccos.retro.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** vc160 Da Vinci Pro Gold v2 wiring (source checks; the view code is Android-only). */
class GoldV2WiringTest {
    private fun src(rel: String): String =
        listOf(File("src/main/$rel"), File("app/src/main/$rel")).first { it.exists() }.readText().replace("\r\n", "\n")

    @Test fun unlockViewKeepsSparklesOffTheText() {
        val v = src("java/com/ccos/retro/ui/ProUnlockView.kt")
        assertTrue(v.contains("clipOutRect"))
        assertTrue(v.contains("Region.Op.DIFFERENCE"))
        assertTrue(src("java/com/ccos/retro/ui/ProUi.kt").contains("art.setKeepOutViews(title, body)"))
    }

    @Test fun unlockViewStopsAtTheCap() {
        val v = src("java/com/ccos/retro/ui/ProUnlockView.kt")
        assertTrue(v.contains("coerceAtMost(params.totalMs())"))
        assertTrue(v.contains("override fun onDetachedFromWindow()"))
    }

    @Test fun idleGlintNeverLoopsAndIsWallClockDriven() {
        val g = src("java/com/ccos/retro/ui/ProGold.kt")
        assertFalse(g.contains("INFINITE"))
        assertFalse(g.contains("repeatCount"))
        assertTrue(g.contains("ProIdleTwinkle.allowed(params, animatorsEnabled(ctx), isLowRam(ctx), gatesOn)"))
        assertTrue(g.contains("SystemClock.uptimeMillis()"))
    }

    @Test fun idleGlintOnlyOnOpenedSurfacesNotTheWallpaper() {
        val main = src("java/com/ccos/retro/ui/MainActivity.kt")
        assertTrue(main.contains("rowGlintArmed = true"))
        assertTrue(main.contains("lockGlintArmed = true"))
        assertTrue(src("java/com/ccos/retro/ui/ProUi.kt").contains("maybeCtaGlint()"))
        val wp = File(if (File("src/main").exists()) "src/main/java/com/ccos/retro/wallpaper" else "app/src/main/java/com/ccos/retro/wallpaper")
        wp.walkTopDown().filter { it.isFile }.forEach {
            val t = it.readText()
            assertFalse(it.name, t.contains("GoldGlint") || t.contains("ProGold") || t.contains("ProSparkle"))
        }
    }
}
