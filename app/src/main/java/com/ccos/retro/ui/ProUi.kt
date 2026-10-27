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
import com.ccos.retro.pro.ProGoldMoment
import com.ccos.retro.pro.PurchaseOutcome
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
    /** Billing seam, owned by ProConfig (B4 release switch reads it). */
    @JvmStatic var billing: ProBilling
        get() = com.ccos.retro.pro.ProConfig.billing
        set(v) { com.ccos.retro.pro.ProConfig.billing = v }

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
    /** Helios no-badger: shared by every screen so two activities can't stack sheets either. */
    val sheetDebounce = com.ccos.retro.pro.UpsellDebounce()

    fun showUpsell(
        activity: Activity,
        prefs: AppPrefs,
        context: LaunchSnapshot? = null,
        liveAllowance: Boolean = false,
        /** DEBUG builds only (MainActivity extra): show the gold look / moment without buying or writing any flag. */
        goldPreview: GoldPreview? = null,
        // Keep onOwned LAST: every caller passes it as a trailing lambda (B2 appended liveAllowance after it,
        // which bound those lambdas to the Boolean and broke compileDebugKotlin).
        onOwned: (() -> Unit)? = null
    ) {
        if (activity.isFinishing) return
        if (!com.ccos.retro.pro.ProConfig.gatesEnabled()) return // B4: no sheet while billing is a stub
        val access = access(prefs)
        if (goldPreview != null && !com.ccos.retro.BuildConfig.DEBUG) return
        if (access.isPro) return
        // Helios no-badger: one sheet per tap; held / repeated presses never stack or reopen it.
        if (!sheetDebounce.tryOpen(android.os.SystemClock.uptimeMillis())) return
        val dialog = BottomSheetDialog(activity)
        val momentHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var unlockArt: ProUnlockView? = null
        var glintCancel: (() -> Unit)? = null
        dialog.setOnDismissListener { sheetDebounce.onDismissed(android.os.SystemClock.uptimeMillis())
            momentHandler.removeCallbacksAndMessages(null)
            unlockArt?.stop()
            glintCancel?.invoke()
        }
        val v = LayoutInflater.from(activity).inflate(R.layout.sheet_pro_upsell, null, false)
        dialog.setContentView(v)
        dialog.behavior.maxWidth = (560 * activity.resources.displayMetrics.density).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        // Material draws its own sheet background; ours has the 20dp corners.
        (v.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        // vc160 Da Vinci Pro Gold v1 4.1: gold for every skin (rule, hairline, CTA); the skin accent no longer drives them.
        val buy = com.ccos.retro.pro.ProContrast.buyButtonGold()
        v.background = ProGold.sheetBackground(activity)
        v.findViewById<View>(R.id.pro_sheet_rule).background = ProGold.metalRect()
        if (liveAllowance) v.findViewById<TextView>(R.id.pro_sheet_title).setText(R.string.pro_allowance_title)
        v.findViewById<TextView>(R.id.pro_sheet_context).apply {
            if (context != null) {
                val shortName = context.name.substringAfter(" | ").ifBlank { context.name }
                text = activity.getString(if (liveAllowance) R.string.pro_allowance_context else R.string.pro_sheet_context, shortName)
                visibility = View.VISIBLE
            }
        }
        // B6: unlimited live tracking item; short form only if the full one wraps past 2 lines.
        v.findViewById<TextView>(R.id.pro_sheet_item_live)?.let { item ->
            item.post { if (item.lineCount > 2) item.setText(R.string.pro_sheet_item_live_short) }
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
        var sawPending = false
        // Da Vinci v2 section 2: once-per-sheet ambient glint on the gold CTA (off with animations removed / low RAM).
        val dp = activity.resources.displayMetrics.density
        val ctaGlint = GoldGlintDrawable(ProGold.CTA_GLINT_POINTS, ProGold.CTA_GLINT_POINTS.indices.toList(),
            Color.WHITE, ProGold.GOLD_DEEP, 3.5f * dp)
        var ctaGlinted = false
        fun maybeCtaGlint() {
            if (ctaGlinted || !primary.isEnabled) return
            ctaGlinted = true
            primary.post {
                if (!dialog.isShowing || !primary.isEnabled) return@post
                if (ProGold.idleGlintAllowed(activity, gatesOn = true)) {
                    primary.foreground = ctaGlint
                    ProGold.startIdleGlint(primary, ctaGlint, gatesOn = true)
                }
            }
        }

        /** v1 4.4: the one-time unlock moment inside this sheet. [preview] = DEBUG only, no flag, no grant. */
        fun playMoment(mode: ProGoldMoment.Mode) {
            val content = v.findViewById<View>(R.id.pro_sheet_content)
            val panel = v.findViewById<View>(R.id.pro_unlock_panel)
            val art = v.findViewById<ProUnlockView>(R.id.pro_unlock_art).also { unlockArt = it }
            val textBlock = v.findViewById<View>(R.id.pro_unlock_text)
            val title = v.findViewById<TextView>(R.id.pro_unlock_title)
            val body = v.findViewById<TextView>(R.id.pro_unlock_body)
            val animated = mode == ProGoldMoment.Mode.ANIMATED
            ctaGlint.cancel()
            // Skip: tap anywhere on the sheet, Back, swipe-down, or an outside tap. Pro is already on.
            val skip = View.OnClickListener { if (dialog.isShowing) dialog.dismiss() }
            panel.setOnClickListener(skip)
            v.setOnClickListener(skip)
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            panel.visibility = View.VISIBLE
            if (animated) {
                content.animate().alpha(0f).setDuration(ProGoldMoment.CONTENT_FADE_MS)
                    .withEndAction { content.visibility = View.INVISIBLE }.start()
                panel.alpha = 0f
                panel.animate().alpha(1f).setDuration(ProGoldMoment.PLATE_FADE_MS)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)).start()
                textBlock.alpha = 0f
                textBlock.animate().alpha(1f).setStartDelay(ProGoldMoment.TEXT_IN_START_MS)
                    .setDuration(ProGoldMoment.TEXT_IN_END_MS - ProGoldMoment.TEXT_IN_START_MS).start()
                momentHandler.postDelayed({ shimmerOnce(title) }, ProGoldMoment.SHIMMER_START_MS)
            } else {
                content.visibility = View.INVISIBLE // keeps the sheet height; no fade, burst, rays or shimmer
                panel.alpha = 1f
                textBlock.alpha = 1f
            }
            art.setKeepOutViews(title, body) // v2: sparkles never over the text
            art.play(mode)
            panel.announceForAccessibility("${title.text}. ${body.text}")
            val hold = ProGoldMoment.holdMs(mode, ProGold.talkBackOn(activity))
            momentHandler.postDelayed({
                if (!dialog.isShowing) return@postDelayed
                if (!animated) dialog.window?.setWindowAnimations(0) // static: no slide
                dialog.dismiss()
            }, hold)
        }

        fun apply(state: SheetState, source: ProGoldMoment.Source? = null) {
            if (activity.isFinishing) return
            val r = UpsellSheetModel.render(state, product)
            if (r.dismiss) {
                // vc160: plays once, only for a fresh purchase; the flag is committed before it plays.
                // Restore / pending-confirmed / refresh keep the old dismiss + toast (O2/O3 default).
                val mode = if (source == null) ProGoldMoment.Mode.NONE else ProGoldMoment.claim(
                    source, prefs.proStore(), activity.isFinishing,
                    ProGold.animatorsEnabled(activity), ProGold.isLowRam(activity)
                )
                if (mode != ProGoldMoment.Mode.NONE) {
                    onOwned?.invoke()
                    playMoment(mode)
                    return
                }
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
            // Gold gradient comes from bg_pro_cta_gold (selector); a tint would flatten it.
            primary.backgroundTintList = null
            if (state == SheetState.READY) maybeCtaGlint() else ctaGlint.cancel()
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
                    if (o is PurchaseOutcome.Pending) sawPending = true
                    val next = UpsellSheetModel.afterPurchase(o)
                    if (next == SheetState.OWNED) {
                        prefs.proStore().putBoolean(ProAccess.KEY_PURCHASED, true)
                        apply(next, ProGoldMoment.sourceForPurchase(sawPending))
                    } else apply(next)
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
        glintCancel = { ctaGlint.cancel() }
        dialog.show()
        when (goldPreview) {
            null -> load()
            GoldPreview.SHEET -> {
                // Enabled gold CTA look only; tapping it does nothing (stub billing, no grant).
                primary.setOnClickListener(null)
                primary.isEnabled = true
                primary.setText(R.string.pro_get)
                primary.setTextColor(buy.label)
                primary.backgroundTintList = null
                message.setText(R.string.pro_confirm_one_time)
                for ((k, b) in buttons) b.visibility = if (k == SheetSecondary.NOT_NOW || k == SheetSecondary.RESTORE) View.VISIBLE else View.GONE
                maybeCtaGlint()
            }
            GoldPreview.MOMENT, GoldPreview.STATIC -> {
                apply(SheetState.READY)
                momentHandler.postDelayed({
                    if (dialog.isShowing) playMoment(if (goldPreview == GoldPreview.MOMENT) ProGoldMoment.Mode.ANIMATED else ProGoldMoment.Mode.STATIC)
                }, 700L)
            }
        }
    }

    /** DEBUG-only previews of the gold sheet / unlock moment (no purchase, no flag, Pro locks stay on). */
    enum class GoldPreview { SHEET, MOMENT, STATIC }

    /** v1 4.4 900-1500 ms: one pale-gold band across the title, once; never loops. */
    private fun shimmerOnce(title: TextView) {
        val w = title.width.toFloat().takeIf { it > 0f } ?: return
        val anim = android.animation.ValueAnimator.ofFloat(-0.3f, 1.3f)
        anim.duration = ProGoldMoment.SHIMMER_END_MS - ProGoldMoment.SHIMMER_START_MS
        anim.addUpdateListener { a ->
            val x = (a.animatedValue as Float) * w
            title.paint.shader = android.graphics.LinearGradient(x - w * 0.15f, 0f, x + w * 0.15f, 0f,
                intArrayOf(ProGold.GOLD, ProGold.GOLD_PALE, ProGold.GOLD), floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP)
            title.invalidate()
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) { title.paint.shader = null; title.invalidate() }
        })
        anim.start()
    }

    /** B6: true when [text] fits [maxLines] lines at [textSizePx] in [widthPx] (StaticLayout never splits a word unless it is wider than a line). */
    fun fitsLines(text: String, widthPx: Int, textSizePx: Float, maxLines: Int = 2): Boolean {
        if (widthPx <= 0) return true
        val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = textSizePx }
        @Suppress("DEPRECATION")
        val layout = if (android.os.Build.VERSION.SDK_INT >= 23)
            android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
                .setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE)
                .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE).build()
        else android.text.StaticLayout(text, paint, widthPx, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        if (layout.lineCount > maxLines) return false
        // A single word wider than the line would be split mid-word: treat as not fitting.
        val words = text.split(' ')
        return words.all { paint.measureText(it) <= widthPx }
    }

    /**
     * B6 (Chris 08:11/08:13, Da Vinci final 08:13, R-NO-BADGER): ONE brief non-modal note at T+60 for free users.
     * 16sp, 1-2 lines, no buttons, no tap target; fades in 300 ms, holds 5 s, fades out 600 ms, then goes away by itself.
     * Polite live region so TalkBack announces it once. [slot] sits above the command row (Command Center); null =
     * a bottom overlay on the activity content (Settings screen, which has no vehicle or telemetry).
     */
    fun showFreeHourNote(activity: Activity, slot: TextView?, next: LaunchSnapshot?) {
        if (activity.isFinishing) return
        val dm = activity.resources.displayMetrics
        val tv = slot ?: TextView(activity).also { v ->
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            val lp = android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM)
            val m = (16 * dm.density).toInt()
            lp.setMargins(m, m, m, m)
            v.setBackgroundColor(0xE6101C28.toInt())
            v.setTextColor(0xFFE8EEF4.toInt())
            val pad = (12 * dm.density).toInt()
            v.setPadding(pad, pad, pad, pad)
            root.addView(v, lp)
        }
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
        tv.isClickable = false
        tv.isFocusable = false
        tv.isLongClickable = false
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            tv.breakStrategy = android.text.Layout.BREAK_STRATEGY_SIMPLE
            tv.hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NONE
        }
        val parentW = (tv.parent as? View)?.width?.takeIf { it > 0 } ?: dm.widthPixels
        val avail = parentW - tv.paddingLeft - tv.paddingRight - (32 * dm.density).toInt()
        val sizePx = 16f * dm.scaledDensity
        val text = com.ccos.retro.pro.LiveTrackingAllowance.noteText(
            next?.let { com.ccos.retro.pro.LiveTrackingAllowance.nextName(it) },
            activity.getString(R.string.pro_allowance_note_next),
            activity.getString(R.string.pro_allowance_note_none),
            activity.getString(R.string.pro_allowance_note_short)
        ) { fitsLines(it, avail, sizePx, 2) }
        tv.animate().cancel()
        tv.alpha = 0f
        tv.visibility = View.VISIBLE
        androidx.core.view.ViewCompat.setAccessibilityLiveRegion(tv, androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        tv.text = text
        tv.animate().alpha(1f).setDuration(com.ccos.retro.pro.LiveTrackingAllowance.NOTE_FADE_IN_MS).withEndAction {
            tv.animate().alpha(0f).setStartDelay(com.ccos.retro.pro.LiveTrackingAllowance.NOTE_HOLD_MS)
                .setDuration(com.ccos.retro.pro.LiveTrackingAllowance.NOTE_FADE_OUT_MS).withEndAction {
                    androidx.core.view.ViewCompat.setAccessibilityLiveRegion(tv, androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE)
                    tv.visibility = View.GONE
                    if (slot == null) (tv.parent as? ViewGroup)?.removeView(tv)
                }.start()
        }.start()
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
