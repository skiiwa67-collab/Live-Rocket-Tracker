package com.ccos.retro.pro

/** Result of asking the store (Play Billing later) whether the Pro unlock is owned. */
sealed class EntitlementResult {
    object Owned : EntitlementResult()
    object NotOwned : EntitlementResult()
    /** Billing missing, offline, or errored. Never revokes a cached Pro flag. */
    data class Unavailable(val reason: String) : EntitlementResult()
}

/** Seam for Play Billing. No billing library yet; see NoBillingSource. */
interface EntitlementSource {
    fun query(onResult: (EntitlementResult) -> Unit)
}

/**
 * Stub until Chris picks product id + price (R-PLAY-STORE). Port of Alarm Pro
 * ProBillingManager goes here later (one INAPP product, queryPurchasesAsync on resume).
 */
object NoBillingSource : EntitlementSource {
    override fun query(onResult: (EntitlementResult) -> Unit) =
        onResult(EntitlementResult.Unavailable("billing not wired"))
}

/**
 * Grandfathered buyers of the paid app get Pro. Real detection needs Play
 * (original purchase / paid-app ownership) and comes later. null = unknown.
 */
interface GrandfatherCheck {
    fun isGrandfathered(): Boolean?
}

object NoGrandfatherCheck : GrandfatherCheck {
    override fun isGrandfathered(): Boolean? = null
}

/**
 * Cached Pro entitlement. Default false. Rules:
 * - Owned -> Pro on; NotOwned -> purchase flag off; Unavailable -> keep cache (never clear on transport failure).
 * - Grandfathered is sticky: once granted it is never revoked by NotOwned or unknown.
 */
class ProAccess(
    private val store: ProStore,
    private val source: EntitlementSource = NoBillingSource,
    private val grandfather: GrandfatherCheck = NoGrandfatherCheck
) {
    val isPro: Boolean
        get() = isPurchased || isGrandfathered

    val isPurchased: Boolean get() = store.getBoolean(KEY_PURCHASED, false)
    val isGrandfathered: Boolean get() = store.getBoolean(KEY_GRANDFATHERED, false)

    /** Call on app start / onResume (TODO(wire): MainActivity.onResume, wallpaper onVisibilityChanged). */
    fun refresh(onDone: ((Boolean) -> Unit)? = null) {
        if (grandfather.isGrandfathered() == true) markGrandfathered()
        source.query { r ->
            when (r) {
                EntitlementResult.Owned -> store.putBoolean(KEY_PURCHASED, true)
                EntitlementResult.NotOwned -> store.putBoolean(KEY_PURCHASED, false)
                is EntitlementResult.Unavailable -> Unit
            }
            onDone?.invoke(isPro)
        }
    }

    /** Stubbed grandfathered path: grants Pro permanently. */
    fun markGrandfathered() = store.putBoolean(KEY_GRANDFATHERED, true)

    companion object {
        const val KEY_PURCHASED = "pro_purchased_cache"
        const val KEY_GRANDFATHERED = "pro_grandfathered"
    }
}
