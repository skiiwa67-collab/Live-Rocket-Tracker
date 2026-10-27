package com.ccos.retro.pro

import com.ccos.retro.BuildConfig

/**
 * vc159: Play one-time product for LRT Pro. Chris sets the real id in Play Console; this is a placeholder
 * on purpose (never guess a live id). While it is the placeholder the purchase button stays disabled.
 */
object ProConfig {
    const val PLACEHOLDER_PRODUCT_ID = "PLACEHOLDER_SET_BY_CHRIS"
    const val PLAY_PRODUCT_ID: String = PLACEHOLDER_PRODUCT_ID

    fun isProductIdConfigured(id: String = PLAY_PRODUCT_ID): Boolean =
        id.isNotBlank() && id != PLACEHOLDER_PRODUCT_ID

    /** Billing seam (moved from ProUi so the release switch can see it). Stub until Play Billing is linked. */
    @JvmStatic @Volatile var billing: ProBilling = StubProBilling

    /** Legacy paid-app buyer check. Stub until Play's original-purchase detection is wired. */
    @JvmStatic @Volatile var grandfatherCheck: GrandfatherCheck = NoGrandfatherCheck

    /** Debug builds only: reviewers force the gates on (adb extra on MainActivity, see AppPrefs). Release ignores it. */
    @JvmStatic @Volatile var debugForceGates: Boolean = false

    const val KEY_DEBUG_FORCE_GATES = "pro_debug_force_gates"
    const val EXTRA_DEBUG_FORCE_GATES = "com.ccos.retro.extra.PRO_FORCE_GATES"

    /**
     * B4 (Skippy vc159): the one release switch. Every Pro gate, lock, tag, badge, card and sheet is OFF unless
     * a real product id, real billing AND the legacy-buyer check are all wired, so a stub can never take
     * features away from paying users. Debug builds can force it on for design review.
     */
    @JvmStatic
    fun gatesEnabled(
        productId: String = PLAY_PRODUCT_ID,
        billing: ProBilling = this.billing,
        grandfather: GrandfatherCheck = grandfatherCheck,
        debugBuild: Boolean = BuildConfig.DEBUG,
        debugForce: Boolean = debugForceGates
    ): Boolean {
        if (debugBuild && debugForce) return true
        return isProductIdConfigured(productId) && billing !is StubProBilling && grandfather !is NoGrandfatherCheck
    }

    /** Toast rate limit from Da Vinci v1 section 7. */
    const val TOAST_MIN_INTERVAL_MS = 4_000L
}
