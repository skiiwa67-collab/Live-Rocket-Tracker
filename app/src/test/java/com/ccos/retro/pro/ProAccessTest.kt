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

    @Test fun noGrandfatheringProIsPurchaseOnly() {
        // Chris 8:22 (Helios P2-B): no grandfathering. A stale legacy flag never grants Pro.
        val s = MemProStore().apply { putBoolean("pro_grandfathered", true) }
        ProAccess(s, src(EntitlementResult.NotOwned)).refresh()
        assertFalse(ProAccess(s).isPro)
        ProAccess(s, src(EntitlementResult.Owned)).refresh()
        assertTrue(ProAccess(s).isPro)
    }

    @Test fun grandfatherPathIsGone() {
        val src = listOf(java.io.File("src/main/java"), java.io.File("app/src/main/java")).first { it.isDirectory }
            .resolve("com/ccos/retro/pro")
        for (f in listOf("ProAccess.kt", "ProConfig.kt")) {
            val t = src.resolve(f).readText()
            for (w in listOf("GrandfatherCheck", "markGrandfathered", "KEY_GRANDFATHERED", "isGrandfathered")) assertFalse("$f $w", t.contains(w))
        }
    }
}
