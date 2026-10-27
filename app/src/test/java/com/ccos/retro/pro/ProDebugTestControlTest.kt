package com.ccos.retro.pro

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Chris 13:21 CT: DEBUG-only Settings "Test Pro unlock" / "Test Pro lock". Release neither shows nor honors it. */
class ProDebugTestControlTest {
    private fun src(rel: String): String =
        listOf(File("src/main/$rel"), File("app/src/main/$rel")).first { it.exists() }.readText().replace("\r\n", "\n")

    @After fun reset() { ProConfig.debugForceGates = false }

    @Test fun releaseRefusesEveryEntryPoint() {
        val store = MemProStore()
        store.putBoolean(ProAccess.KEY_PURCHASED, true)
        store.putBoolean(ProGoldMoment.KEY_SHOWN, true)
        assertFalse(ProDebugTest.available(debugBuild = false))
        assertFalse(ProDebugTest.armUnlock(store, debugBuild = false))
        assertFalse(ProDebugTest.lock(store, debugBuild = false))
        // Nothing written, gates untouched.
        assertTrue(store.getBoolean(ProAccess.KEY_PURCHASED, false))
        assertTrue(store.getBoolean(ProGoldMoment.KEY_SHOWN, false))
        assertFalse(store.map.containsKey(ProConfig.KEY_DEBUG_FORCE_GATES))
        assertFalse(ProConfig.debugForceGates)
        // And a release build ignores forced gates anyway.
        assertFalse(ProConfig.gatesEnabled(debugBuild = false, debugForce = true))
    }

    @Test fun unlockGoesThroughTheRealPurchasePathAndPlaysTheMomentOnce() {
        val store = MemProStore()
        store.putBoolean(ProGoldMoment.KEY_SHOWN, true) // moment already seen once
        assertTrue(ProDebugTest.armUnlock(store, debugBuild = true))
        assertFalse(ProAccess(store).isPro)
        assertFalse(store.getBoolean(ProGoldMoment.KEY_SHOWN, true))
        assertTrue(store.getBoolean(ProConfig.KEY_DEBUG_FORCE_GATES, false) && ProConfig.debugForceGates)
        var outcome: PurchaseOutcome? = null
        ProDebugTest.TestOwnedBilling.purchase(null, ProConfig.PLAY_PRODUCT_ID) { outcome = it }
        assertEquals(SheetState.OWNED, UpsellSheetModel.afterPurchase(outcome!!))
        assertEquals(ProGoldMoment.Source.PURCHASE, ProGoldMoment.sourceForPurchase(false))
        assertEquals(ProGoldMoment.Mode.ANIMATED,
            ProGoldMoment.claim(ProGoldMoment.Source.PURCHASE, store, false, true, false))
        // Once only: a second claim does not replay it.
        assertEquals(ProGoldMoment.Mode.NONE,
            ProGoldMoment.claim(ProGoldMoment.Source.PURCHASE, store, false, true, false))
    }

    @Test fun lockReturnsToNotOwnedWithGatesOn() {
        val store = MemProStore()
        store.putBoolean(ProAccess.KEY_PURCHASED, true)
        assertTrue(ProDebugTest.lock(store, debugBuild = true))
        assertFalse(ProAccess(store).isPro)
        assertTrue(ProConfig.gatesEnabled(debugBuild = true, debugForce = ProConfig.debugForceGates))
        assertEquals(ProDebugTest.LABEL_UNLOCK, ProDebugTest.label(isPro = false, gatesOn = true))
        assertEquals(ProDebugTest.LABEL_LOCK, ProDebugTest.label(isPro = true, gatesOn = true))
        assertEquals(ProDebugTest.LABEL_UNLOCK, ProDebugTest.label(isPro = true, gatesOn = false))
    }

    @Test fun everyCallerIsBehindBuildConfigDebug() {
        val main = src("java/com/ccos/retro/ui/MainActivity.kt")
        assertTrue(main.contains("if (com.ccos.retro.BuildConfig.DEBUG) bindProTestButton()"))
        for (fn in listOf("private fun bindProTestButton() {", "private fun onProTestTapped() {")) {
            assertTrue(main.substringAfter(fn).trimStart().startsWith("if (!com.ccos.retro.BuildConfig.DEBUG) return"))
        }
        // The only place the sheet is asked for a test purchase is the debug handler.
        assertEquals(1, Regex("debugTestPurchase = true").findAll(main).count())
        val ui = src("java/com/ccos/retro/ui/ProUi.kt")
        assertTrue(ui.contains("if (debugTestPurchase && !com.ccos.retro.BuildConfig.DEBUG) return"))
        assertTrue(ui.contains("if (com.ccos.retro.BuildConfig.DEBUG && debugTestPurchase) com.ccos.retro.pro.ProDebugTest.TestOwnedBilling else billing"))
        // Same Get Pro handler: the test taps primary; the real handler does the OWNED write.
        assertTrue(ui.contains("primary.performClick()"))
        assertTrue(ui.contains("sheetBilling.purchase(activity, ProConfig.PLAY_PRODUCT_ID)"))
        // Built in code: no layout / string resource ships in release.
        assertFalse(src("res/layout/activity_main.xml").contains("Test Pro"))
        // R8 strips the DEBUG-dead callers in release.
        val gradle = listOf(File("build.gradle.kts"), File("app/build.gradle.kts")).first { it.exists() }.readText()
        assertTrue(gradle.contains("isMinifyEnabled = true"))
        for (s in listOf(ProDebugTest.LABEL_UNLOCK, ProDebugTest.LABEL_LOCK)) assertFalse(s.contains("demo", ignoreCase = true))
    }
}
