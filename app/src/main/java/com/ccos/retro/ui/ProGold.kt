package com.ccos.retro.ui

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.ccos.retro.R
import com.ccos.retro.pro.ProIdleTwinkle
import com.ccos.retro.pro.ProSparkleParams

/** vc160 Da Vinci LRT Pro Gold v1 sections 2-3: gold drawables + motion/accessibility checks shared by the Pro UI. */
object ProGold {
    /** DEBUG builds only: sheet | moment | static | row. Release ignores it. */
    const val EXTRA_PRO_GOLD_PREVIEW = "com.ccos.retro.extra.PRO_GOLD_PREVIEW"

    const val GOLD = 0xFFFFD700.toInt()
    const val GOLD_HI = 0xFFFFD27A.toInt()
    const val GOLD_PALE = 0xFFFFFACD.toInt()
    const val GOLD_DEEP = 0xFFB8860B.toInt()
    const val GOLD_INK = 0xFF1A1200.toInt()
    const val PLATE = 0xFF322A18.toInt()
    const val SHEET_BG = 0xFF0F1620.toInt()

    /** pro_gold_metal: horizontal 5-stop metallic. */
    val METAL_COLORS = intArrayOf(GOLD_DEEP, GOLD, GOLD_HI, GOLD, GOLD_DEEP)
    val METAL_STOPS = floatArrayOf(0f, 0.22f, 0.5f, 0.78f, 1f)

    fun metalShader(width: Float): Shader =
        LinearGradient(0f, 0f, width.coerceAtLeast(1f), 0f, METAL_COLORS, METAL_STOPS, Shader.TileMode.CLAMP)

    private val metalFactory = object : ShapeDrawable.ShaderFactory() {
        override fun resize(width: Int, height: Int): Shader = metalShader(width.toFloat())
    }

    /** Flat metal bar (sheet rule). */
    fun metalRect(): Drawable = ShapeDrawable(RectShape()).apply { shaderFactory = metalFactory }

    fun metalRoundRect(radiusPx: Float): ShapeDrawable =
        ShapeDrawable(RoundRectShape(FloatArray(8) { radiusPx }, null, null)).apply { shaderFactory = metalFactory }

    /** Sheet background: bg_pro_sheet (#0F1620, 20dp top corners, unchanged) + 1dp metal hairline along the top edge. */
    fun sheetBackground(ctx: Context): Drawable {
        val d = ctx.resources.displayMetrics.density
        val base = ContextCompat.getDrawable(ctx, R.drawable.bg_pro_sheet)!!
        return LayerDrawable(arrayOf(base, GoldHairlineDrawable(20f * d, 1f * d)))
    }

    /** Owned Settings row: 1.5dp metal border (8dp corners) around the bg_pro_row_owned_gold plate. */
    fun ownedRowBackground(ctx: Context): Drawable {
        val d = ctx.resources.displayMetrics.density
        val border = metalRoundRect(8f * d)
        val plate = ContextCompat.getDrawable(ctx, R.drawable.bg_pro_row_owned_gold)!!
        val inset = (1.5f * d).toInt().coerceAtLeast(1)
        return LayerDrawable(arrayOf(border, plate)).apply { setLayerInset(1, inset, inset, inset, inset) }
    }

    /** Remove animations: ANIMATOR_DURATION_SCALE == 0, or animators disabled (API 26+). */
    fun animatorsEnabled(ctx: Context): Boolean {
        // getFloat(cr, name, default) never throws (API 17+); no catch needed.
        val scale = Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) return false
        if (android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) return false
        return true
    }

    fun isLowRam(ctx: Context): Boolean =
        (ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

    fun talkBackOn(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        return am.isEnabled && am.isTouchExplorationEnabled
    }

    /**
     * Da Vinci v2 section 2: ambient, once-per-open glint on a gold surface the user opened (Pro CTA, Settings padlocks,
     * owned row). Six single 800 ms sin^2 pulses, one new peak every 380 ms (< 3 Hz), done by 2.95 s, never loops.
     * Off with animations removed, on low-RAM devices and with gates off (B4); never used by the wallpaper.
     * Any touch on the host skips it. Returns true when it started.
     */
    fun startIdleGlint(host: View, glint: GoldGlintDrawable, gatesOn: Boolean, params: ProSparkleParams = ProSparkleParams.current): Boolean {
        val ctx = host.context
        if (!ProIdleTwinkle.allowed(params, animatorsEnabled(ctx), isLowRam(ctx), gatesOn)) return false
        host.setOnTouchListener { _, _ -> glint.cancel(); false }
        glint.start(host)
        return true
    }

    fun idleGlintAllowed(ctx: Context, gatesOn: Boolean, params: ProSparkleParams = ProSparkleParams.current): Boolean =
        ProIdleTwinkle.allowed(params, animatorsEnabled(ctx), isLowRam(ctx), gatesOn)

    /** CTA: 6 white glints on the rim (fractions of the button), #B8860B shadow. Label stays clear. */
    val CTA_GLINT_POINTS = listOf(0.06f to 0.2f, 0.28f to 0.04f, 0.6f to 0.04f, 0.94f to 0.22f, 0.86f to 0.96f, 0.18f to 0.96f)
    /** Owned row: around the PRO pill (start) and the row's right end. */
    val ROW_GLINT_POINTS = listOf(0.03f to 0.22f, 0.1f to 0.84f, 0.16f to 0.12f, 0.9f to 0.2f, 0.97f to 0.8f, 0.94f to 0.5f)
    /** Padlock: one glint at the 18dp ring's upper-right. */
    val LOCK_GLINT_POINT = 0.84f to 0.16f
}

/** 1dp metal line that follows the sheet's rounded top edge. */
class GoldHairlineDrawable(private val radius: Float, private val stroke: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0) return
        val h = stroke / 2f
        val l = b.left + h; val t = b.top + h; val r = b.right - h
        val rad = radius - h
        path.reset()
        path.moveTo(l, b.top + radius)
        path.arcTo(RectF(l, t, l + 2 * rad, t + 2 * rad), 180f, 90f)
        path.lineTo(r - rad, t)
        path.arcTo(RectF(r - 2 * rad, t, r, t + 2 * rad), 270f, 90f)
        paint.shader = ProGold.metalShader(b.width().toFloat())
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * Foreground overlay for the ambient idle glint (v2 section 2). Wall-clock driven (not scaled by the animator duration
 * scale, so glints never speed past 3 Hz), runs 0..[ProIdleTwinkle.totalMs] once, cancelled on detach or touch.
 * [points] are fractions of the bounds; [schedule] maps each point to one of the six glint start times.
 */
class GoldGlintDrawable(
    private val points: List<Pair<Float, Float>>,
    private val schedule: List<Int>,
    private val color: Int,
    private val shadowColor: Int?,
    private val sizePx: Float,
    private val params: ProSparkleParams = ProSparkleParams.current
) : Drawable() {
    private val total = ProIdleTwinkle.totalMs(params)
    var t: Long = total
        private set
    private var host: View? = null
    private var startMs = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dst = RectF()
    private val tick = object : Runnable {
        override fun run() {
            val h = host ?: return
            t = (android.os.SystemClock.uptimeMillis() - startMs).coerceAtMost(total)
            invalidateSelf(); h.invalidate()
            if (t < total) h.postOnAnimation(this) else host = null
        }
    }
    private val detach = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { cancel() }
    }

    fun start(v: View) {
        cancel()
        host = v; startMs = android.os.SystemClock.uptimeMillis(); t = 0L
        v.addOnAttachStateChangeListener(detach)
        v.postOnAnimation(tick)
    }

    /** Skip: jump to the end (all glints off). */
    fun cancel() {
        host?.let { it.removeCallbacks(tick); it.removeOnAttachStateChangeListener(detach); it.invalidate() }
        host = null; t = total
        invalidateSelf()
    }

    val running: Boolean get() = host != null

    override fun draw(canvas: Canvas) {
        if (t >= total) return
        val b = bounds
        if (b.width() <= 0) return
        val star = STAR
        for (i in points.indices) {
            val a = ProIdleTwinkle.alpha(params, schedule.getOrElse(i) { i }, t)
            if (a <= 0f) continue
            val x = b.left + points[i].first * b.width(); val y = b.top + points[i].second * b.height()
            val hw = sizePx; val hh = sizePx * 1.25f
            shadowColor?.let {
                paint.colorFilter = android.graphics.PorterDuffColorFilter(it, android.graphics.PorterDuff.Mode.SRC_IN)
                paint.alpha = (200 * a).toInt()
                dst.set(x - hw + 1f, y - hh + 1f, x + hw + 1f, y + hh + 1f); canvas.drawBitmap(star, null, dst, paint)
            }
            paint.colorFilter = android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
            paint.alpha = (255 * a).toInt()
            dst.set(x - hw, y - hh, x + hw, y + hh); canvas.drawBitmap(star, null, dst, paint)
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private val STAR by lazy { ProUnlockView.renderStar(32) }
    }
}
