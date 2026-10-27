package com.ccos.retro.pro

import com.ccos.retro.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsellSheetModelTest {
    private val price = ProProduct("$4.99")

    @Test fun placeholderProductIdIsNotConfigured() {
        assertEquals(ProConfig.PLACEHOLDER_PRODUCT_ID, ProConfig.PLAY_PRODUCT_ID)
        assertFalse(ProConfig.isProductIdConfigured())
        assertFalse(ProConfig.isProductIdConfigured(""))
        assertTrue(ProConfig.isProductIdConfigured("some_real_id"))
    }

    @Test fun priceOnlyFromOneTimeOffer() {
        assertNull(ProProduct.fromOneTimeOffer(null).formattedPrice)
        assertNull(ProProduct.fromOneTimeOffer("   ").formattedPrice)
        assertEquals("$4.99", ProProduct.fromOneTimeOffer(" $4.99 ").formattedPrice)
    }

    @Test fun noPriceShowsGetProDisabled() {
        val r = UpsellSheetModel.render(SheetState.READY, null, productIdConfigured = true)
        assertEquals(R.string.pro_get, r.primaryTextRes)
        assertNull(r.priceArg)
        assertFalse(r.primaryEnabled)
        assertEquals(R.string.pro_state_unavailable, r.messageRes)
        assertEquals(listOf(SheetSecondary.NOT_NOW, SheetSecondary.TRY_AGAIN), r.secondary)
    }

    @Test fun placeholderIdKeepsBuyDisabledEvenWithPrice() {
        val r = UpsellSheetModel.render(SheetState.READY, price)
        assertEquals(R.string.pro_get_with_price, r.primaryTextRes)
        assertFalse(r.primaryEnabled)
    }

    @Test fun readyWithPriceAndIdIsBuyable() {
        val r = UpsellSheetModel.render(SheetState.READY, price, productIdConfigured = true)
        assertEquals(R.string.pro_get_with_price, r.primaryTextRes)
        assertEquals("$4.99", r.priceArg)
        assertTrue(r.primaryEnabled)
        assertEquals(R.string.pro_confirm_one_time, r.messageRes)
        assertEquals(listOf(SheetSecondary.NOT_NOW, SheetSecondary.RESTORE), r.secondary)
    }

    @Test fun loadingAndPurchasingShowProgress() {
        val l = UpsellSheetModel.render(SheetState.LOADING, null, true)
        assertTrue(l.showProgress); assertFalse(l.primaryEnabled)
        val p = UpsellSheetModel.render(SheetState.PURCHASING, price, true)
        assertTrue(p.showProgress); assertFalse(p.primaryEnabled); assertTrue(p.secondary.isEmpty())
    }

    @Test fun stateMapping() {
        assertEquals(SheetState.READY, UpsellSheetModel.afterLoad(price))
        assertEquals(SheetState.UNAVAILABLE, UpsellSheetModel.afterLoad(null))
        assertEquals(SheetState.UNAVAILABLE, UpsellSheetModel.afterLoad(ProProduct(null)))
        assertEquals(SheetState.OWNED, UpsellSheetModel.afterPurchase(PurchaseOutcome.Owned))
        assertEquals(SheetState.PENDING, UpsellSheetModel.afterPurchase(PurchaseOutcome.Pending))
        assertEquals(SheetState.CANCELLED, UpsellSheetModel.afterPurchase(PurchaseOutcome.UserCancelled))
        assertEquals(SheetState.UNAVAILABLE, UpsellSheetModel.afterPurchase(PurchaseOutcome.Unavailable))
        assertEquals(SheetState.ERROR, UpsellSheetModel.afterPurchase(PurchaseOutcome.Error("x")))
        assertTrue(UpsellSheetModel.render(SheetState.OWNED, price, true).dismiss)
        assertEquals(listOf(SheetSecondary.CLOSE), UpsellSheetModel.render(SheetState.PENDING, price, true).secondary)
    }

    @Test fun stubBillingNeverGrantsPro() {
        var product: ProProduct? = ProProduct("x")
        StubProBilling.loadProduct(ProConfig.PLAY_PRODUCT_ID) { product = it }
        assertNull(product)
        var out: PurchaseOutcome? = null
        StubProBilling.purchase(null, ProConfig.PLAY_PRODUCT_ID) { out = it }
        assertEquals(PurchaseOutcome.Unavailable, out)
        var res: EntitlementResult? = null
        StubProBilling.restore { res = it }
        assertTrue(res is EntitlementResult.Unavailable)
    }

    @Test fun settingsRow() {
        val owned = ProSettingsRowModel.render(true, RestoreState.IDLE)
        assertEquals(R.string.pro_settings_owned, owned.titleRes)
        assertFalse(owned.showRestoreButton)
        val free = ProSettingsRowModel.render(false, RestoreState.IDLE)
        assertEquals(R.string.pro_settings_row_title, free.titleRes)
        assertTrue(free.showRestoreButton); assertTrue(free.restoreEnabled)
        assertFalse(ProSettingsRowModel.render(false, RestoreState.CHECKING).restoreEnabled)
        assertEquals(RestoreState.RESTORED, ProSettingsRowModel.afterRestore(EntitlementResult.Owned))
        assertEquals(RestoreState.NONE, ProSettingsRowModel.afterRestore(EntitlementResult.NotOwned))
    }

    @Test fun toastLimiterFourSeconds() {
        val t = ProToastLimiter()
        assertTrue(t.tryAcquire(1_000))
        assertFalse(t.tryAcquire(4_999))
        assertTrue(t.tryAcquire(5_000))
    }
}
