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

    @Test fun unlockDrawOrderMatchesDaVinci() {
        // Da Vinci 12:16 CT: glow -> rays -> rings -> back -> dust -> mid -> residents -> PADLOCK -> front -> glints -> text.
        // Glow and rays under the text (not clipped); rings, sparkles, residents and glints clipped off the text.
        val v = src("java/com/ccos/retro/ui/ProUnlockView.kt")
        val body = v.substring(v.indexOf("private fun drawMoment("), v.indexOf("private fun drawStatic("))
        fun at(s: String): Int = body.indexOf(s).also { assertTrue("missing: $s", it >= 0) }
        val glow = at("RadialGradient(")
        val rays = at("paint.color = params.rayColor")
        val clip = at("clipOutText(canvas)")
        val rings = at("params.ringProgress(i, t)")
        val layers = at("for (li in params.layers.indices)")
        val residents = at("drawResidents(canvas, cx, cy, res, t)")
        val glints = at("params.glintAlpha(i, t)")
        assertTrue(glow < rays && rays < clip && clip < rings && rings < layers && layers < glints)
        assertTrue("residents then padlock just before front",
            body.contains("if (params.layers[li].name == \"front\") { drawResidents(canvas, cx, cy, res, t); drawLock() }"))
        assertTrue(residents in layers..glints)
        // the padlock is drawn only via drawLock (not before the rings any more)
        val lockDef = at("val drawLock: () -> Unit = {")
        assertTrue(lockDef < clip)
        assertTrue("padlock drawn once, inside drawLock", Regex("drawPadlock\\(").findAll(body).count() == 1)
        val lockBody = body.substring(lockDef, body.indexOf("}", body.indexOf("if (keepOutOn) clipOutText(canvas)", lockDef)))
        // padlock keeps its own clipping: pops the text keep-out clip, draws, restores it
        assertTrue(lockBody.indexOf("canvas.restore()") < lockBody.indexOf("drawPadlock(") &&
            lockBody.indexOf("drawPadlock(") < lockBody.indexOf("canvas.save()") &&
            lockBody.indexOf("canvas.save()") < lockBody.indexOf("clipOutText(canvas)"))
        // static frame keeps the same order: padlock before the front layer, glints last
        val st = v.substring(v.indexOf("private fun drawStatic("), v.indexOf("private fun drawResidents("))
        assertTrue(st.contains("if (!lockDrawn && s.layer == frontIdx) { drawLock(); lockDrawn = true }"))
        assertTrue(st.indexOf("sortedBy { it.layer }") in 0 until st.indexOf("drawGlint("))
        assertTrue(st.indexOf("if (!lockDrawn) drawLock()") in 0 until st.indexOf("drawGlint("))
        val names = ProSparkleParams.V2.layers.map { it.name }
        assertTrue(names.toString(), names == listOf("back", "dust", "mid", "front"))
        // keep-out begins exactly when the text starts to appear; rings are over by then
        assertTrue(ProSparkleParams.V2.textKeepOutMs <= ProGoldMoment.TEXT_IN_START_MS)
        assertTrue(ProSparkleParams.V2.ringEndMs <= ProSparkleParams.V2.textKeepOutMs)
        // text block is a later sibling of the art view, so it draws on top
        val xml = listOf(File("src/main/res/layout/view_pro_unlock.xml"), File("app/src/main/res/layout/view_pro_unlock.xml")).first { it.exists() }.readText()
        assertTrue(xml.indexOf("@+id/pro_unlock_art") < xml.indexOf("@+id/pro_unlock_text"))
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
