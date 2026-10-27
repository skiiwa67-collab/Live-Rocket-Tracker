package com.ccos.retro.pro

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B4 regression (Skippy vc159): one release switch; all gates OFF while billing or the legacy-buyer check is a stub. */
class ProReleaseSwitchTest {
    private object RealBilling : ProBilling {
        override fun loadProduct(productId: String, onResult: (ProProduct?) -> Unit) = onResult(ProProduct("$1"))
        override fun purchase(host: Any?, productId: String, onResult: (PurchaseOutcome) -> Unit) = onResult(PurchaseOutcome.Owned)
        override fun restore(onResult: (EntitlementResult) -> Unit) = onResult(EntitlementResult.NotOwned)
    }
    private object RealGrandfather : GrandfatherCheck { override fun isGrandfathered(): Boolean? = false }

    private val oldReplay = snap(net = NOW - 30L * 24 * HOUR)
    private val proSkin = com.ccos.retro.model.AppPrefs.CONSOLE_SKIN_JAXA

    @After fun reset() { ProConfig.debugForceGates = false }

    @Test fun shippedConfigHasGatesOff() {
        assertFalse(ProConfig.gatesEnabled(debugBuild = false))
        assertFalse("debug build without the force flag is off too", ProConfig.gatesEnabled(debugBuild = true, debugForce = false))
    }

    @Test fun everyStubKeepsGatesOff() {
        assertFalse(ProConfig.gatesEnabled("PLACEHOLDER_SET_BY_CHRIS", RealBilling, RealGrandfather, debugBuild = false))
        assertFalse(ProConfig.gatesEnabled("lrt_pro", StubProBilling, RealGrandfather, debugBuild = false))
        assertFalse(ProConfig.gatesEnabled("lrt_pro", RealBilling, NoGrandfatherCheck, debugBuild = false))
        assertTrue(ProConfig.gatesEnabled("lrt_pro", RealBilling, RealGrandfather, debugBuild = false))
    }

    @Test fun debugForceOnlyWorksInDebugBuilds() {
        assertTrue(ProConfig.gatesEnabled(debugBuild = true, debugForce = true))
        assertFalse(ProConfig.gatesEnabled(debugBuild = false, debugForce = true))
    }

    @Test fun gatesOffAllowsEveryHookForAFreeUser() {
        for (h in ProHook.values()) {
            assertTrue(h.name, ProPolicy.allows(h, false, launch = oldReplay, skinId = proSkin, store = MemProStore(), now = NOW, gatesOn = false))
        }
        ProConfig.debugForceGates = false
        assertTrue("default reads the switch", ProPolicy.allows(ProHook.SELECT_LAUNCH, false, launch = oldReplay, now = NOW))
        assertFalse(LiveTrackingAllowance.shouldShowCard(snap(net = NOW - 2 * HOUR, abbrev = "In Flight", status = "Launch in Flight"), false, MemProStore(), NOW, gatesOn = false))
    }

    @Test fun gatesOnStillGateAPastReplay() {
        assertFalse(ProPolicy.allows(ProHook.SELECT_LAUNCH, false, launch = oldReplay, now = NOW, gatesOn = true))
        assertFalse(ProPolicy.allows(ProHook.THEME_PICK_SETTINGS, false, skinId = proSkin, store = MemProStore(), now = NOW, gatesOn = true))
        ProConfig.debugForceGates = true
        assertFalse("forced on via the debug switch", ProPolicy.allows(ProHook.SELECT_LAUNCH, false, launch = oldReplay, now = NOW))
    }
}
