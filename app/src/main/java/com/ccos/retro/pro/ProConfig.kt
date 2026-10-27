package com.ccos.retro.pro

/**
 * vc159: Play one-time product for LRT Pro. Chris sets the real id in Play Console; this is a placeholder
 * on purpose (never guess a live id). While it is the placeholder the purchase button stays disabled.
 */
object ProConfig {
    const val PLACEHOLDER_PRODUCT_ID = "PLACEHOLDER_SET_BY_CHRIS"
    const val PLAY_PRODUCT_ID: String = PLACEHOLDER_PRODUCT_ID

    fun isProductIdConfigured(id: String = PLAY_PRODUCT_ID): Boolean =
        id.isNotBlank() && id != PLACEHOLDER_PRODUCT_ID

    /** Toast rate limit from Da Vinci v1 section 7. */
    const val TOAST_MIN_INTERVAL_MS = 4_000L
}
