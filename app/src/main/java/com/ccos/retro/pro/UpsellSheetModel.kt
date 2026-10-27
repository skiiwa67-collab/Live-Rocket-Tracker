package com.ccos.retro.pro

import com.ccos.retro.R

/** Da Vinci v1 section 5 / v1.2: pure state -> render model for the Pro sheet. */
enum class SheetState { LOADING, READY, UNAVAILABLE, PURCHASING, PENDING, CANCELLED, ERROR, OWNED }

enum class SheetSecondary { NOT_NOW, RESTORE, TRY_AGAIN, CLOSE }

data class SheetRender(
    val primaryTextRes: Int,
    /** Non-null only when [primaryTextRes] is pro_get_with_price. */
    val priceArg: String?,
    val primaryEnabled: Boolean,
    val showProgress: Boolean,
    val messageRes: Int,
    val secondary: List<SheetSecondary>,
    val dismiss: Boolean
)

object UpsellSheetModel {
    fun render(
        state: SheetState,
        product: ProProduct?,
        productIdConfigured: Boolean = ProConfig.isProductIdConfigured()
    ): SheetRender {
        val price = product?.formattedPrice
        val buyable = price != null && productIdConfigured
        val primaryRes = if (price != null) R.string.pro_get_with_price else R.string.pro_get
        fun r(enabled: Boolean, progress: Boolean, msg: Int, sec: List<SheetSecondary>, dismiss: Boolean = false) =
            SheetRender(primaryRes, price, enabled, progress, msg, sec, dismiss)
        val normal = listOf(SheetSecondary.NOT_NOW, SheetSecondary.RESTORE)
        return when (state) {
            SheetState.LOADING -> r(false, true, R.string.pro_state_checking, normal)
            SheetState.READY ->
                if (buyable) r(true, false, R.string.pro_confirm_one_time, normal)
                else r(false, false, R.string.pro_state_unavailable, listOf(SheetSecondary.NOT_NOW, SheetSecondary.TRY_AGAIN))
            SheetState.UNAVAILABLE ->
                r(false, false, R.string.pro_state_unavailable, listOf(SheetSecondary.NOT_NOW, SheetSecondary.TRY_AGAIN))
            SheetState.PURCHASING -> r(false, true, R.string.pro_state_finishing, emptyList())
            SheetState.PENDING -> r(false, false, R.string.pro_state_pending, listOf(SheetSecondary.CLOSE))
            SheetState.CANCELLED -> r(buyable, false, R.string.pro_state_user_cancelled, normal)
            SheetState.ERROR -> r(buyable, false, R.string.pro_state_error, normal)
            SheetState.OWNED -> r(false, false, R.string.pro_toast_on, emptyList(), dismiss = true)
        }
    }

    /** Load result -> next state. No price (or unavailable) is never "ready to buy". */
    fun afterLoad(product: ProProduct?): SheetState =
        if (product?.formattedPrice != null) SheetState.READY else SheetState.UNAVAILABLE

    fun afterPurchase(o: PurchaseOutcome): SheetState = when (o) {
        PurchaseOutcome.Owned -> SheetState.OWNED
        PurchaseOutcome.Pending -> SheetState.PENDING
        PurchaseOutcome.UserCancelled -> SheetState.CANCELLED
        PurchaseOutcome.Unavailable -> SheetState.UNAVAILABLE
        is PurchaseOutcome.Error -> SheetState.ERROR
    }
}

/** Da Vinci v1 section 6: Settings "LRT Pro" row. */
enum class RestoreState { IDLE, CHECKING, RESTORED, NONE, UNAVAILABLE }

data class SettingsRowRender(val titleRes: Int, val statusRes: Int?, val showRestoreButton: Boolean, val restoreEnabled: Boolean)

object ProSettingsRowModel {
    fun render(isPro: Boolean, restore: RestoreState): SettingsRowRender {
        if (isPro) return SettingsRowRender(R.string.pro_settings_owned, null, showRestoreButton = false, restoreEnabled = false)
        val status = when (restore) {
            RestoreState.IDLE -> null
            RestoreState.CHECKING -> R.string.pro_state_checking
            RestoreState.RESTORED -> R.string.pro_restored
            RestoreState.NONE -> R.string.pro_restore_none
            RestoreState.UNAVAILABLE -> R.string.pro_restore_unavailable
        }
        return SettingsRowRender(R.string.pro_settings_row_title, status, showRestoreButton = true, restoreEnabled = restore != RestoreState.CHECKING)
    }

    fun afterRestore(r: EntitlementResult): RestoreState = when (r) {
        EntitlementResult.Owned -> RestoreState.RESTORED
        EntitlementResult.NotOwned -> RestoreState.NONE
        is EntitlementResult.Unavailable -> RestoreState.UNAVAILABLE
    }
}

/** Da Vinci v1 section 7: at most one Pro toast per 4 s. */
class ProToastLimiter(private val minIntervalMs: Long = ProConfig.TOAST_MIN_INTERVAL_MS) {
    private var last = Long.MIN_VALUE
    @Synchronized fun tryAcquire(now: Long): Boolean {
        if (last != Long.MIN_VALUE && now - last < minIntervalMs) return false
        last = now
        return true
    }
}
