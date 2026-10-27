package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Helios no-badger: allowance never auto-opens the sheet; a refused gate opens it once per tap, debounced. */
class NoBadgerTest {
    private fun src(rel: String) = listOf(File(rel), File("app/$rel")).first { it.exists() }.readText()

    @Test fun onePressOpensOnce() {
        val d = UpsellDebounce()
        assertTrue(d.tryOpen(0))
        assertTrue(d.showing)
    }

    @Test fun heldOrRepeatedPressesNeverStack() {
        val d = UpsellDebounce()
        assertTrue(d.tryOpen(0))
        val opened = (1..100).count { d.tryOpen(it * 50L) } // held key repeat at 20 Hz while the sheet is up
        assertEquals(0, opened)
    }

    @Test fun heldPressAfterCloseDoesNotReopenUntilQuiet() {
        val d = UpsellDebounce()
        assertTrue(d.tryOpen(0))
        d.onDismissed(2_000)
        for (t in 2_050L..4_000L step 50L) assertFalse("repeat at $t", d.tryOpen(t)) // still held
        assertTrue("new tap after 1 s quiet", d.tryOpen(4_000L + UpsellDebounce.QUIET_MS))
    }

    @Test fun separateTapsEachOpenOnceAfterClose() {
        val d = UpsellDebounce()
        assertTrue(d.tryOpen(0)); d.onDismissed(500)
        assertTrue(d.tryOpen(2_000)); d.onDismissed(2_500)
        assertTrue(d.tryOpen(5_000))
    }

    @Test fun showUpsellIsDebouncedAndDismissReleases() {
        val ui = src("src/main/java/com/ccos/retro/ui/ProUi.kt")
        val body = ui.substringAfter("fun showUpsell(")
        assertTrue(body.indexOf("sheetDebounce.tryOpen(") in 0 until body.indexOf("BottomSheetDialog(activity)"))
        assertTrue(body.contains("setOnDismissListener { sheetDebounce.onDismissed("))
    }

    @Test fun allowanceNeverAutoOpensTheSheet() {
        for (a in listOf("CommandCenterActivity", "MainActivity")) {
            val s = src("src/main/java/com/ccos/retro/ui/$a.kt")
            assertFalse(a, s.contains("liveAllowance = true"))
            assertFalse(a, s.substringAfter("onLiveAllowanceUsed = ").take(200).contains("showUpsell"))
        }
    }
}
