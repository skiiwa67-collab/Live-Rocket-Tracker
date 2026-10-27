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
 * Cached Pro entitlement. Default false. Rules:
 * - Owned -> Pro on; NotOwned -> purchase flag off; Unavailable -> keep cache (never clear on transport failure).
 * - Chris 8:22: no grandfathering (nobody bought the paid app). Pro = purchased, nothing else.
 */
class ProAccess(
    private val store: ProStore,
    private val source: EntitlementSource = NoBillingSource
) {
    val isPro: Boolean
        get() = isPurchased

    val isPurchased: Boolean get() = store.getBoolean(KEY_PURCHASED, false)

    /** Call on app start / onResume (vc159: MainActivity.onResume, wallpaper onVisibilityChanged). */
    fun refresh(onDone: ((Boolean) -> Unit)? = null) {
        source.query { r ->
            when (r) {
                EntitlementResult.Owned -> store.putBoolean(KEY_PURCHASED, true)
                EntitlementResult.NotOwned -> store.putBoolean(KEY_PURCHASED, false)
                is EntitlementResult.Unavailable -> Unit
            }
            onDone?.invoke(isPro)
        }
    }

    companion object {
        const val KEY_PURCHASED = "pro_purchased_cache"
    }
}
