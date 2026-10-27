package com.ccos.retro.pro

import com.ccos.retro.BuildConfig

/**
 * Chris 13:21 CT 10-27: "Next time just add a test unlock pro button so I can make sure it works."
 * DEBUG builds only: a plain Settings button.
 *  - "Test Pro unlock": not owned + debug gates on + moment flag cleared, then the real Pro sheet runs its real
 *    Get Pro handler against [TestOwnedBilling] (purchase -> Owned), so OWNED, the cache write, the one-time
 *    claim and the gold moment all go through the same code a Play purchase uses.
 *  - "Test Pro lock": back to not owned with the debug gates on, so the padlocks show again.
 * Release: every entry point refuses (debugBuild = false) and the callers sit behind BuildConfig.DEBUG,
 * which R8 strips (release isMinifyEnabled = true). Pure JVM so tests cover it.
 */
object ProDebugTest {
    const val LABEL_UNLOCK = "Test Pro unlock"
    const val LABEL_LOCK = "Test Pro lock"

    fun available(debugBuild: Boolean = BuildConfig.DEBUG): Boolean = debugBuild

    /** Tap 2 label once Pro is owned with the gates on; otherwise tap 1. */
    fun label(isPro: Boolean, gatesOn: Boolean): String = if (isPro && gatesOn) LABEL_LOCK else LABEL_UNLOCK

    /** Tap 1 setup. Only for this test: clears the one-time moment flag so the moment can replay. */
    fun armUnlock(store: ProStore, debugBuild: Boolean = BuildConfig.DEBUG): Boolean {
        if (!debugBuild) return false
        store.commitBoolean(ProAccess.KEY_PURCHASED, false)
        store.commitBoolean(ProGoldMoment.KEY_SHOWN, false)
        forceGates(store)
        return true
    }

    /** Tap 2: not owned, gates on, locks show again. */
    fun lock(store: ProStore, debugBuild: Boolean = BuildConfig.DEBUG): Boolean {
        if (!debugBuild) return false
        store.commitBoolean(ProAccess.KEY_PURCHASED, false)
        forceGates(store)
        return true
    }

    private fun forceGates(store: ProStore) {
        store.putBoolean(ProConfig.KEY_DEBUG_FORCE_GATES, true)
        ProConfig.debugForceGates = true
    }

    /** Billing for the test sheet only: purchase answers Owned, like Play does after a real payment. */
    object TestOwnedBilling : ProBilling {
        override fun loadProduct(productId: String, onResult: (ProProduct?) -> Unit) = onResult(null)
        override fun purchase(host: Any?, productId: String, onResult: (PurchaseOutcome) -> Unit) =
            onResult(PurchaseOutcome.Owned)
        override fun restore(onResult: (EntitlementResult) -> Unit) = onResult(EntitlementResult.NotOwned)
    }
}
