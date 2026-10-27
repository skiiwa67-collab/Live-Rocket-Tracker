package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** Da Vinci 13:18 CT art gate on vc160: equal chips, padlock off the label, tapered + feathered rays. */
class ArtGate1318Test {
    private fun src(rel: String): String =
        listOf(File("src/main/$rel"), File("app/src/main/$rel")).first { it.exists() }.readText().replace("\r\n", "\n")

    private val chips = listOf("mcc", "ros", "spacex", "nasa", "cnsa", "ariane", "rlab", "ula", "jaxa", "isro")

    private fun tagFor(xml: String, id: String): String {
        val at = xml.indexOf("android:id=\"@+id/$id\"")
        assertTrue("$id missing", at >= 0)
        return xml.substring(xml.lastIndexOf('<', at), xml.indexOf("/>", at))
    }

    // MUST 1: every console chip has the same size, text size and margin (JAXA / ISRO were resized at runtime).
    @Test fun allConsoleChipsShareSizeAndTextSize() {
        val xml = src("res/layout/activity_main.xml")
        for (c in chips) {
            val t = tagFor(xml, "btn_console_$c")
            assertTrue("$c height", t.contains("android:layout_height=\"40dp\""))
            assertTrue("$c textSize", t.contains("android:textSize=\"11sp\""))
            assertTrue("$c margin", t.contains("android:layout_margin=\"2dp\""))
            assertTrue("$c weight", t.contains("android:layout_weight=\"1\""))
        }
        // Row 3 spacers carry the same margin, so JAXA / ISRO get the same width as rows 1-2.
        val row3 = xml.substringAfter("@+id/row_console_skin_3").substringBefore("</LinearLayout>")
        val spacers = Regex("<View[^>]*/>").findAll(row3).map { it.value }.toList()
        assertEquals(2, spacers.size)
        for (s in spacers) assertTrue(s.contains("android:layout_margin=\"2dp\"") && s.contains("android:layout_weight=\"1\""))
    }

    @Test fun noRuntimeResizeOfTheThirdChipRow() {
        val main = src("java/com/ccos/retro/ui/MainActivity.kt")
        assertFalse(main.contains("row_console_skin_3.post"))
        assertFalse(main.contains("h * 0.32f"))
    }

    // MUST 2: the badge sits in the top-end corner; a badged chip's label ends before the badge starts.
    @Test fun padlockBadgeStaysOutsideTheLabel() {
        var w = 60f
        while (w <= 110f) {
            var lw = 10f
            while (lw <= w - ProChip.LABEL_PAD_DP - ProChip.endPadDp(true)) {
                val (l, r) = ProChip.labelSpanDp(w, lw, badged = true)
                assertTrue("chip $w label $lw: label right $r overlaps badge", r <= w - ProChip.LOCK_BADGE_DP + 1e-3f)
                assertTrue(l >= ProChip.LABEL_PAD_DP - 1e-3f)
                lw += 2.5f
            }
            w += 5f
        }
        assertEquals(ProChip.LABEL_PAD_DP, ProChip.endPadDp(false), 0f)
        assertEquals(53, ProChip.endPadPx(true, 2.625f)) // 20dp at 420dpi
        val main = src("java/com/ccos/retro/ui/MainActivity.kt")
        assertTrue(main.contains("com.ccos.retro.pro.ProChip.endPadPx(badged, resources.displayMetrics.density)"))
        assertTrue(main.contains("com.ccos.retro.pro.ProChip.LOCK_BADGE_DP"))
    }

    // SHOULD 3: rays taper to a point and feather out; same draw order.
    @Test fun raysTaperToAPointAndFeather() {
        val k = ProRay.kite(30f, 6f, 100f)
        assertEquals(0f, k[0], 0f); assertEquals(0f, k[1], 0f)
        assertEquals(100.0, hypot(k[4].toDouble(), k[5].toDouble()), 1e-3)
        assertEquals(30.0, Math.toDegrees(atan2(k[5].toDouble(), k[4].toDouble())), 1e-3)
        for (j in listOf(2, 6)) assertEquals(ProRay.WIDE_AT * 100.0, hypot(k[j].toDouble(), k[j + 1].toDouble()), 1e-3)
        assertTrue(ProRay.WIDE_AT > 0f && ProRay.WIDE_AT < 1f)
        val (halo, core) = ProRay.PASSES
        assertTrue("core narrower than halo", core.first < halo.first)
        assertTrue("core brighter than halo", core.second > halo.second)
        assertEquals(0f, ProRay.FALLOFF_ALPHA.last(), 0f)
        assertEquals(ProRay.FALLOFF_STOPS.size, ProRay.FALLOFF_ALPHA.size)
        assertTrue(abs(ProRay.FALLOFF_STOPS.last() - 1f) < 1e-6f)
        val view = src("java/com/ccos/retro/ui/ProUnlockView.kt")
        assertTrue(view.contains("ProRay.kite("))
        assertTrue(view.contains("for ((halfScale, alphaScale) in ProRay.PASSES)"))
        // Draw order unchanged: glow, rays, then the padlock.
        val glow = view.indexOf("canvas.drawCircle(cx, cy, radius, paint)")
        val rays = view.indexOf("ProRay.kite(")
        val lock = view.indexOf("drawPadlock(canvas")
        assertTrue(glow in 0 until rays && rays < lock)
    }
}
