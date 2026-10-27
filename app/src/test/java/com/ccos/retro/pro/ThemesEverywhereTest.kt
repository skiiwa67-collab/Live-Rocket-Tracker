package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Chris 8:22: Pro unlocks ALL themes; the theme gate stands everywhere, live launches included. */
class ThemesEverywhereTest {
    private fun src(rel: String) = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        .resolve("com/ccos/retro/$rel").readText()

    @Test fun freeUserNeverGetsAProThemeFromAuto() {
        val s = MemProStore()
        val pro = com.ccos.retro.model.AppPrefs.CONSOLE_SKIN_IDS.filterNot { ThemeAccess.canPickManually(it, false, s) }
        assertTrue(pro.isNotEmpty())
        for (id in pro) {
            assertFalse(id, ThemeAccess.canPickManually(ThemeAccess.displayedDuringLive(id, false, s), false, s))
            assertEquals(com.ccos.retro.model.AppPrefs.normalizeConsoleSkin(id), ThemeAccess.displayedDuringLive(id, true, s))
        }
    }

    @Test fun gateRunsOnAutoApplyResumeAndWallpaperVisibility() {
        assertTrue(src("module/RocketTelemetryModule.kt").contains("prefs.noteConsoleSkinTrackedLaunch(s.id, s, isProProvider())"))
        val prefs = src("model/AppPrefs.kt")
        val enforce = prefs.substringAfter("fun enforceThemeGate(isPro: Boolean)").substringBefore("\n    }\n")
        assertTrue(enforce.contains("if (isPro) return"))
        assertTrue(enforce.contains("ThemeAccess.skinAfterLiveEnds(cur, false, proStore())"))
        val resume = src("ui/MainActivity.kt").substringAfter("override fun onResume()").take(900)
        assertTrue(resume.contains("prefs.enforceThemeGate(isPro())"))
        val vis = src("wallpaper/RetroCommandWallpaperService.kt").substringAfter("override fun onVisibilityChanged(").take(900)
        assertTrue(vis.contains("prefs.enforceThemeGate("))
        assertTrue(vis.contains("ProConfig.gatesEnabled()"))
    }

    @Test fun liveTrackingItselfIsNeverGated() {
        val live = snap(net = NOW - 10 * MIN, abbrev = "In Flight", status = "Launch in Flight")
        assertFalse(ProGate.isLocked(live, false, NOW))
        assertTrue(ProPolicy.allows(ProHook.SELECT_LAUNCH, false, launch = live, now = NOW, gatesOn = true))
    }
}
