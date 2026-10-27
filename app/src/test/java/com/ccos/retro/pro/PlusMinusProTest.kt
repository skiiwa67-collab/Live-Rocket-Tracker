package com.ccos.retro.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Chris 8:22 (4): the +/- event controls are Pro on past launches (where they do their work). Verifies the existing gate. */
class PlusMinusProTest {
    private fun src(rel: String) = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        .resolve("com/ccos/retro/$rel").readText()

    private val pastReplay = snap(id = "ll2-past", net = NOW - 30L * 24 * HOUR)

    @Test fun plusMinusOnAPastReplayIsPro() {
        for (h in listOf(ProHook.SKIP_EVENT, ProHook.JUMP)) {
            assertFalse("$h free on a past replay", ProPolicy.allows(h, false, launch = pastReplay, now = NOW, gatesOn = true))
            assertTrue("$h for Pro", ProPolicy.allows(h, true, launch = pastReplay, now = NOW, gatesOn = true))
            assertTrue("$h with the release switch off", ProPolicy.allows(h, false, launch = pastReplay, now = NOW, gatesOn = false))
        }
    }

    @Test fun everyPlusMinusButtonGoesThroughTheGate() {
        val cc = src("ui/CommandCenterActivity.kt")
        assertTrue(cc.contains("R.id.btn_event_prev).setOnClickListener {\n            telemetryModule.skipEvent(-1)"))
        assertTrue(cc.contains("R.id.btn_event_next).setOnClickListener {\n            telemetryModule.skipEvent(1)"))
        assertTrue(src("wallpaper/RetroCommandWallpaperService.kt").contains("telemetryModule.skipEvent(if (y >= eventSkipHit.centerY()) -1 else 1)"))
        val m = src("module/RocketTelemetryModule.kt")
        val skip = m.substringAfter("fun skipEvent(dir: Int) {").take(200)
        assertTrue(skip.contains("if (!proAllows(ProHook.SKIP_EVENT, launch)) return"))
        val jump = m.substringAfter("fun jumpTo(secFromNet: Float) {").take(120)
        assertTrue(jump.contains("if (!proAllows(ProHook.JUMP, tracked)) return"))
    }
}
