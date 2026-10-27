package com.ccos.retro.pro

/**
 * vc159: billing seam. No Play Billing library is linked yet; [StubProBilling] answers "unavailable" for
 * everything, which drives the sheet's offline state (Get Pro disabled, Try again).
 */
data class ProProduct(val formattedPrice: String?) {
    companion object {
        /** v1.2: the price comes ONLY from oneTimePurchaseOfferDetails.formattedPrice. Blank means no price. */
        fun fromOneTimeOffer(oneTimePurchaseOfferFormattedPrice: String?): ProProduct =
            ProProduct(oneTimePurchaseOfferFormattedPrice?.trim()?.takeIf { it.isNotEmpty() })
    }
}

sealed class PurchaseOutcome {
    object Owned : PurchaseOutcome()
    object Pending : PurchaseOutcome()
    object UserCancelled : PurchaseOutcome()
    object Unavailable : PurchaseOutcome()
    data class Error(val reason: String) : PurchaseOutcome()
}

interface ProBilling {
    /** null = billing unavailable / product not found. */
    fun loadProduct(productId: String, onResult: (ProProduct?) -> Unit)
    fun purchase(host: Any?, productId: String, onResult: (PurchaseOutcome) -> Unit)
    fun restore(onResult: (EntitlementResult) -> Unit)
}

object StubProBilling : ProBilling {
    override fun loadProduct(productId: String, onResult: (ProProduct?) -> Unit) = onResult(null)
    override fun purchase(host: Any?, productId: String, onResult: (PurchaseOutcome) -> Unit) =
        onResult(PurchaseOutcome.Unavailable)
    override fun restore(onResult: (EntitlementResult) -> Unit) =
        onResult(EntitlementResult.Unavailable("billing not linked"))
}
