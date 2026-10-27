package com.ccos.retro.pro

import com.ccos.retro.model.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ThemeAccessTest {
    @Test fun freeSetIsBasePlusOneEuro() {
        val s = MemProStore()
        assertEquals(setOf("NASA", "SPACEX", "ROS", "CNSA", "MCC", "ARIANE"), ThemeAccess.freeSet(s))
    }

    @Test fun euroPickIsStickyAcrossReadsAndRng() {
        val s = MemProStore()
        val first = ThemeAccess.freeEuroSkin(s, Random(1))
        repeat(5) { assertEquals(first, ThemeAccess.freeEuroSkin(s, Random(it.toLong() + 99))) }
        assertEquals(first, s.getString(ThemeAccess.KEY_FREE_EURO))
        assertTrue(first in ThemeAccess.EURO_POOL)
    }

    @Test fun invalidStoredEuroIsRepaired() {
        val s = MemProStore(); s.putString(ThemeAccess.KEY_FREE_EURO, "JAXA")
        assertTrue(ThemeAccess.freeEuroSkin(s) in ThemeAccess.EURO_POOL)
    }

    @Test fun manualPickerFiltersForFree() {
        val s = MemProStore()
        for (id in listOf("R.LAB", "ULA", "JAXA", "ISRO")) {
            assertFalse(id, ThemeAccess.canPickManually(id, false, s))
            assertFalse(ThemeAccess.recordManualPick(s, id, false))
        }
        for (id in ThemeAccess.freeSet(s)) assertTrue(id, ThemeAccess.recordManualPick(s, id, false))
    }

    @Test fun proPicksAny() {
        val s = MemProStore()
        for (id in AppPrefs.CONSOLE_SKIN_IDS) assertTrue(id, ThemeAccess.recordManualPick(s, id, true))
        assertEquals("ISRO", ThemeAccess.savedManualPick(s, true))
    }

    @Test fun autoProThemeIsGatedOnLiveToo() {
        // Chris 8:22: free users never see a Pro theme, live launches included.
        val s = MemProStore()
        ThemeAccess.recordManualPick(s, "NASA", false)
        assertEquals("NASA", ThemeAccess.displayedDuringLive("ULA", false, s))
        assertEquals("ULA", ThemeAccess.displayedDuringLive("ULA", true, s))
        assertEquals("SPACEX", ThemeAccess.displayedDuringLive("SPACEX", false, s))
        assertEquals("NASA", ThemeAccess.skinAfterLiveEnds("ULA", false, s))
        assertEquals("NASA", ThemeAccess.savedManualPick(s, false))
        assertEquals("CNSA", ThemeAccess.skinAfterLiveEnds("CNSA", false, s))
        assertEquals("ULA", ThemeAccess.skinAfterLiveEnds("ULA", true, s))
    }

    @Test fun lapsedProFallsBackToMcc() {
        val s = MemProStore()
        ThemeAccess.recordManualPick(s, "JAXA", true)
        assertEquals("MCC", ThemeAccess.savedManualPick(s, false))
        assertEquals("MCC", ThemeAccess.savedManualPick(MemProStore(), false))
    }

    @Test fun autoThemingMapUnchanged() {
        // AppPrefs.consoleSkinIdForLaunch takes no Pro input, so Auto is identical for free and Pro.
        // Only the pre-TelemetrySkin branches are JVM-testable (TelemetrySkin uses android.graphics.Color).
        assertEquals("ULA", AppPrefs.consoleSkinIdForLaunch(snap(provider = "United Launch Alliance", rocket = "Vulcan VC2S")))
        assertEquals("JAXA", AppPrefs.consoleSkinIdForLaunch(snap(provider = "JAXA", rocket = "H3")))
        assertEquals("ISRO", AppPrefs.consoleSkinIdForLaunch(snap(provider = "ISRO", rocket = "LVM3")))
        assertEquals("MCC", AppPrefs.consoleSkinIdForLaunch(null))
        assertEquals("ULA", ThemeAccess.displayedDuringLive(AppPrefs.consoleSkinIdForLaunch(snap(provider = "United Launch Alliance")), true, MemProStore()))
        assertEquals("MCC", ThemeAccess.displayedDuringLive(AppPrefs.consoleSkinIdForLaunch(snap(provider = "United Launch Alliance")), false, MemProStore()))
    }
}
