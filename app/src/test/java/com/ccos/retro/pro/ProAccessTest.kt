package com.ccos.retro.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProAccessTest {
    private fun src(r: EntitlementResult) = object : EntitlementSource {
        override fun query(onResult: (EntitlementResult) -> Unit) = onResult(r)
    }

    @Test fun defaultIsFree() = assertFalse(ProAccess(MemProStore()).isPro)

    @Test fun stubBillingNeverGrants() {
        val s = MemProStore(); val p = ProAccess(s); p.refresh(); assertFalse(p.isPro)
    }

    @Test fun ownedGrantsAndIsCached() {
        val s = MemProStore(); ProAccess(s, src(EntitlementResult.Owned)).refresh()
        assertTrue(ProAccess(s).isPro)
    }

    @Test fun unavailableNeverClearsCache() {
        val s = MemProStore(); ProAccess(s, src(EntitlementResult.Owned)).refresh()
        ProAccess(s, src(EntitlementResult.Unavailable("offline"))).refresh()
        assertTrue(ProAccess(s).isPro)
    }

    @Test fun notOwnedRevokesPurchase() {
        val s = MemProStore(); ProAccess(s, src(EntitlementResult.Owned)).refresh()
        ProAccess(s, src(EntitlementResult.NotOwned)).refresh()
        assertFalse(ProAccess(s).isPro)
    }

    @Test fun grandfatheredGrantsAndIsSticky() {
        val s = MemProStore()
        val gf = object : GrandfatherCheck { override fun isGrandfathered(): Boolean? = true }
        ProAccess(s, src(EntitlementResult.NotOwned), gf).refresh()
        assertTrue(ProAccess(s).isPro)
        ProAccess(s, src(EntitlementResult.NotOwned)).refresh()
        assertTrue(ProAccess(s).isPro)
        assertTrue(ProAccess(s).isGrandfathered)
        assertFalse(ProAccess(s).isPurchased)
    }

    @Test fun stubGrandfatherCheckGrantsNothing() {
        val s = MemProStore(); ProAccess(s, grandfather = NoGrandfatherCheck).refresh(); assertFalse(ProAccess(s).isPro)
    }

    @Test fun markGrandfatheredGrants() {
        val s = MemProStore(); ProAccess(s).markGrandfathered(); assertTrue(ProAccess(s).isPro)
    }
}
