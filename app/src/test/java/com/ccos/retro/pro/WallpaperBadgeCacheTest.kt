package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Helios P2-C: theme lock badges read a cache refreshed on visibility change; nothing allocated or written per frame. */
class WallpaperBadgeCacheTest {
    private val wp = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        .resolve("com/ccos/retro/wallpaper/RetroCommandWallpaperService.kt").readText().replace("\r\n", "\n")

    @Test fun drawPathAllocatesAndWritesNothing() {
        val draw = wp.substringAfter("private fun drawThemeLockBadges(").substringBefore("\n        private fun ")
        for (bad in listOf("proStore()", "ProUi.access", "ProAccess(", "SharedPrefsProStore", "Color.parseColor", "allowsTheme", "freeEuroSkin", "freeSet", "canPickManually")) {
            assertFalse("draw path uses $bad", draw.contains(bad))
        }
        assertTrue(draw.contains("val proThemes = badgeProThemes ?: return"))
    }

    @Test fun cacheRefreshedOnVisibility() {
        val vis = wp.substringAfter("override fun onVisibilityChanged(").substringBefore("\n        override fun ")
        assertTrue(vis.contains("refreshThemeBadgeCache()"))
        assertTrue(vis.indexOf("refresh()") < vis.indexOf("refreshThemeBadgeCache()"))
    }

    @Test fun euroPickIsWrittenOncePerStoreOffTheDrawPath() {
        val s = MemProStore()
        val first = ThemeAccess.freeEuroSkin(s)
        assertEquals(first, ThemeAccess.freeEuroSkin(s))
        val badges = com.ccos.retro.model.AppPrefs.CONSOLE_SKIN_IDS.filterNot {
            ProPolicy.allows(ProHook.THEME_PICK_WALLPAPER_FLYOUT, false, skinId = it, store = s, gatesOn = true)
        }
        assertFalse(first in badges)
        assertFalse("MCC" in badges)
        assertTrue(badges.isNotEmpty())
    }
}
