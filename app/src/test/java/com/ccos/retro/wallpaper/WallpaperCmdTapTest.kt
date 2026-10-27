package com.ccos.retro.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** CMD double-tap opens the app; single tap must still fall through to the flyout. */
class WallpaperCmdTapTest {
    private val window = WallpaperCmdTap.DOUBLE_TAP_MS

    @Test fun firstTapTogglesFlyoutAndArmsTimer() {
        val r = WallpaperCmdTap.decide(lastTapMs = 0L, nowMs = 10_000L, windowMs = window)
        assertEquals(WallpaperCmdTap.Decision.TOGGLE_FLYOUT, r.decision)
        assertFalse(r.opensApp)
        assertEquals(10_000L, r.nextLastTapMs)
    }

    @Test fun secondTapInsideWindowOpensAppAndClearsTimer() {
        val first = WallpaperCmdTap.decide(0L, 10_000L, window)
        val second = WallpaperCmdTap.decide(first.nextLastTapMs, 10_000L + window, window)
        assertTrue(second.opensApp)
        assertEquals(WallpaperCmdTap.Decision.OPEN_APP, second.decision)
        assertEquals(0L, second.nextLastTapMs)
    }

    @Test fun tapJustOutsideWindowIsAnotherSingleTap() {
        val r = WallpaperCmdTap.decide(lastTapMs = 10_000L, nowMs = 10_000L + window + 1L, windowMs = window)
        assertEquals(WallpaperCmdTap.Decision.TOGGLE_FLYOUT, r.decision)
        assertEquals(10_000L + window + 1L, r.nextLastTapMs)
    }

    @Test fun tapAfterASuccessfulDoubleTapIsASingleTapAgain() {
        val afterOpen = WallpaperCmdTap.decide(lastTapMs = 0L, nowMs = 20_000L, windowMs = window)
        assertEquals(WallpaperCmdTap.Decision.TOGGLE_FLYOUT, afterOpen.decision)
        assertFalse(afterOpen.opensApp)
    }

    @Test fun sameTimestampCountsAsDoubleTap() {
        val r = WallpaperCmdTap.decide(lastTapMs = 10_000L, nowMs = 10_000L, windowMs = window)
        assertTrue(r.opensApp)
        assertEquals(0L, r.nextLastTapMs)
    }

    @Test fun balOptInMatchesPlatformApisFrom12Through16() {
        assertEquals(WallpaperCmdTap.BalOptIn.NONE, WallpaperCmdTap.balOptIn(24))
        assertEquals(WallpaperCmdTap.BalOptIn.NONE, WallpaperCmdTap.balOptIn(29))
        assertEquals(WallpaperCmdTap.BalOptIn.BOOLEAN_ALLOWED, WallpaperCmdTap.balOptIn(30))
        assertEquals(WallpaperCmdTap.BalOptIn.BOOLEAN_ALLOWED, WallpaperCmdTap.balOptIn(31))
        assertEquals(WallpaperCmdTap.BalOptIn.BOOLEAN_ALLOWED, WallpaperCmdTap.balOptIn(33))
        assertEquals(WallpaperCmdTap.BalOptIn.MODE_ALLOWED, WallpaperCmdTap.balOptIn(34))
        assertEquals(WallpaperCmdTap.BalOptIn.MODE_ALLOWED, WallpaperCmdTap.balOptIn(35))
        assertEquals(WallpaperCmdTap.BalOptIn.MODE_ALLOW_ALWAYS, WallpaperCmdTap.balOptIn(36))
        assertEquals(WallpaperCmdTap.BalOptIn.MODE_ALLOW_ALWAYS, WallpaperCmdTap.balOptIn(37))
    }

    @Test fun wallpaperWiresDecideAndLogsAFailedLaunch() {
        val src = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
            .resolve("com/ccos/retro/wallpaper/RetroCommandWallpaperService.kt")
            .readText().replace("\r\n", "\n")
        val handle = src.substringAfter("private fun handleButtonTap(i: Int) {").substringBefore("private fun openSettings()")
        assertTrue(handle.contains("WallpaperCmdTap.decide(lastCmdTapTime, now, doubleTapMs)"))
        assertTrue(handle.contains("if (tap.opensApp)"))
        assertTrue(handle.contains("showPanel = false"))
        assertTrue(handle.contains("openSettings()"))
        val open = src.substringAfter("private fun openSettings() {").substringBefore("private fun openPadEarth()")
        assertTrue(open.contains("WallpaperOwnActivityLaunch.start"))
        assertFalse(open.contains("catch (_: Exception)"))
        assertFalse(open.contains("startActivity("))
        val launch = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
            .resolve("com/ccos/retro/wallpaper/WallpaperOwnActivityLaunch.kt")
            .readText()
        assertTrue(launch.contains("Log.w("))
        assertTrue(launch.contains("PendingIntent.getActivity"))
        assertFalse(launch.contains("SYSTEM_ALERT_WINDOW"))
    }
}
