package com.ccos.retro.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.ccos.retro.R
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.pro.EntitlementResult
import com.ccos.retro.pro.ProAccess
import com.ccos.retro.pro.ProBilling
import com.ccos.retro.pro.ProConfig
import com.ccos.retro.pro.ProHook
import com.ccos.retro.pro.ProPolicy
import com.ccos.retro.pro.ProProduct
import com.ccos.retro.pro.ProToastLimiter
import com.ccos.retro.pro.SheetSecondary
import com.ccos.retro.pro.SheetState
import com.ccos.retro.pro.StubProBilling
import com.ccos.retro.pro.UpsellSheetModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.DateFormat
import java.util.Date

/** vc159: Android glue for the Pro UI (Da Vinci v1.1 + erratum, v1.2 one-time). Pure rules live in pro/. */
object ProUi {
    /** Billing seam. Stub until Play Billing is linked; swap here only. */
    @JvmStatic var billing: ProBilling = StubProBilling

    private val toastLimiter = ProToastLimiter()

    fun access(prefs: AppPrefs): ProAccess = ProAccess(prefs.proStore())

    /** Wallpaper only (v1 section 7): short, max one per 4 s. */
    fun toastBlocked(context: Context, hook: ProHook, now: Long = System.currentTimeMillis()) {
        if (!toastLimiter.tryAcquire(now)) return
        val res = when (hook.kind) {
            ProHook.Kind.THEME -> R.string.pro_toast_theme_locked
            ProHook.Kind.JUMP_SKIP -> R.string.pro_toast_jump_locked
            ProHook.Kind.LAUNCH -> R.string.pro_toast_replay_locked
        }
        try { Toast.makeText(context.applicationContext, res, Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
    }

    /** Settings title colour per skin (matches MainActivity.applyConsoleSkin); sheet top rule uses it. */
    fun skinTitleColor(skin: String): Int = com.ccos.retro.pro.ProContrast.accentFor(skin)

    /**
     * v1 section 5 sheet. Opened only by a user tap (locked row, locked theme chip, locked jump/skip, Pro row).
     * Swipe / Back = Not now.
     */
    fun showUpsell(
        activity: Activity,
        prefs: AppPrefs,
        context: LaunchSnapshot? = null,
        onOwned: (() -> Unit)? = null,
        liveAllowance: Boolean = false
    ) {
        if (activity.isFinishing) return
        val access = access(prefs)
        if (access.isPro) return
        val dialog = BottomSheetDialog(activity)
        val v = LayoutInflater.from(activity).inflate(R.layout.sheet_pro_upsell, null, false)
        dialog.setContentView(v)
        dialog.behavior.maxWidth = (560 * activity.resources.displayMetrics.density).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        // Material draws its own sheet background; ours has the 20dp corners.
        (v.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        val accent = skinTitleColor(prefs.consoleSkin)
        val buy = com.ccos.retro.pro.ProContrast.buyButtonForSkin(prefs.consoleSkin)
        v.findViewById<View>(R.id.pro_sheet_rule).setBackgroundColor(accent)
        if (liveAllowance) v.findViewById<TextView>(R.id.pro_sheet_title).setText(R.string.pro_allowance_title)
        v.findViewById<TextView>(R.id.pro_sheet_context).apply {
            if (context != null) {
                val shortName = context.name.substringAfter(" | ").ifBlank { context.name }
                text = activity.getString(if (liveAllowance) R.string.pro_allowance_context else R.string.pro_sheet_context, shortName)
                visibility = View.VISIBLE
            }
        }
        val primary = v.findViewById<Button>(R.id.pro_sheet_primary)
        val progress = v.findViewById<ProgressBar>(R.id.pro_sheet_progress)
        val message = v.findViewById<TextView>(R.id.pro_sheet_message)
        val buttons = mapOf(
            SheetSecondary.NOT_NOW to v.findViewById<Button>(R.id.pro_sheet_not_now),
            SheetSecondary.RESTORE to v.findViewById<Button>(R.id.pro_sheet_restore),
            SheetSecondary.TRY_AGAIN to v.findViewById<Button>(R.id.pro_sheet_try_again),
            SheetSecondary.CLOSE to v.findViewById<Button>(R.id.pro_sheet_close)
        )
        var product: ProProduct? = null

        fun apply(state: SheetState) {
            if (activity.isFinishing) return
            val r = UpsellSheetModel.render(state, product)
            if (r.dismiss) {
                dialog.dismiss()
                Toast.makeText(activity, R.string.pro_toast_on, Toast.LENGTH_SHORT).show()
                onOwned?.invoke()
                return
            }
            primary.text = if (r.priceArg != null) activity.getString(r.primaryTextRes, r.priceArg)
            else activity.getString(r.primaryTextRes)
            primary.isEnabled = r.primaryEnabled
            // vc159 Da Vinci: accent only if WCAG label >= 4.5:1 and fill-vs-sheet >= 3:1, else #90E0FF / #1A1A22.
            primary.setTextColor(if (r.primaryEnabled) buy.label else 0xFF8AA0B0.toInt())
            primary.backgroundTintList = if (r.primaryEnabled) android.content.res.ColorStateList.valueOf(buy.fill) else null
            progress.visibility = if (r.showProgress) View.VISIBLE else View.GONE
            message.setText(r.messageRes)
            for ((k, b) in buttons) b.visibility = if (k in r.secondary) View.VISIBLE else View.GONE
            dialog.setCancelable(state != SheetState.PURCHASING)
        }

        fun load() {
            apply(SheetState.LOADING)
            billing.loadProduct(ProConfig.PLAY_PRODUCT_ID) { p ->
                activity.runOnUiThread {
                    product = p
                    apply(UpsellSheetModel.afterLoad(p))
                }
            }
        }

        primary.setOnClickListener {
            apply(SheetState.PURCHASING)
            billing.purchase(activity, ProConfig.PLAY_PRODUCT_ID) { o ->
                activity.runOnUiThread {
                    val next = UpsellSheetModel.afterPurchase(o)
                    if (next == SheetState.OWNED) prefs.proStore().putBoolean(ProAccess.KEY_PURCHASED, true)
                    apply(next)
                }
            }
        }
        buttons[SheetSecondary.NOT_NOW]?.setOnClickListener { dialog.dismiss() }
        buttons[SheetSecondary.CLOSE]?.setOnClickListener { dialog.dismiss() }
        buttons[SheetSecondary.TRY_AGAIN]?.setOnClickListener { load() }
        buttons[SheetSecondary.RESTORE]?.setOnClickListener {
            apply(SheetState.LOADING)
            billing.restore { res ->
                activity.runOnUiThread {
                    when (res) {
                        EntitlementResult.Owned -> {
                            prefs.proStore().putBoolean(ProAccess.KEY_PURCHASED, true)
                            apply(SheetState.OWNED)
                        }
                        EntitlementResult.NotOwned -> {
                            apply(UpsellSheetModel.afterLoad(product))
                            message.setText(R.string.pro_restore_none)
                        }
                        is EntitlementResult.Unavailable -> {
                            apply(SheetState.UNAVAILABLE)
                            message.setText(R.string.pro_restore_unavailable)
                        }
                    }
                }
            }
        }
        dialog.show()
        load()
    }

    fun rowDate(l: LaunchSnapshot): String =
        if (l.netMs > 0) DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(l.netMs)) else ""

    /** "provider · medium date" (v1 section 3 line 2). */
    fun rowLine2(l: LaunchSnapshot): String =
        listOf(l.provider, rowDate(l)).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * v1 section 3: HISTORIC dropdown rows for free users. Locked rows get the lock + "Pro" end zone,
 * Included rows get the "Included" pill. Pro owners and the CURRENT list keep the stock adapter.
 */
class ProLaunchRowAdapter(
    context: Context,
    private val launches: List<LaunchSnapshot>,
    labels: List<String>,
    private val tags: List<String>,
    private val now: Long
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, labels) {

    init { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    fun isLocked(position: Int): Boolean {
        val l = launches.getOrNull(position) ?: return false
        return !ProPolicy.allowsLaunch(ProHook.PICKER_ROW, false, l, now)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val l = launches.getOrNull(position) ?: return super.getDropDownView(position, null, parent)
        val v = if (convertView?.id == R.id.pro_row_root) convertView
        else LayoutInflater.from(context).inflate(R.layout.item_launch_row, parent, false).also { it.id = R.id.pro_row_root }
        val locked = isLocked(position)
        val tag = tags.getOrElse(position) { "" }
        val included = !locked && tag == com.ccos.retro.pro.PickerTag.INCLUDED
        val line1 = v.findViewById<TextView>(R.id.row_line1)
        line1.text = tag + l.name
        line1.setTextColor(if (locked) 0xFFD8E2EA.toInt() else 0xFFFFFFFF.toInt())
        val line2 = ProUi.rowLine2(l)
        val date = ProUi.rowDate(l)
        v.findViewById<TextView>(R.id.row_line2).text = line2
        v.findViewById<View>(R.id.row_lock_zone).visibility = if (locked) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.row_pill).visibility = if (included) View.VISIBLE else View.GONE
        v.minimumHeight = ((if (included) 94 else 64) * context.resources.displayMetrics.density).toInt()
        val name = l.name
        v.contentDescription = when {
            locked -> context.getString(R.string.pro_cd_row_locked, name, l.provider, date)
            included -> context.getString(R.string.pro_cd_row_included, name, l.provider, date)
            else -> null
        }
        return v
    }

}
