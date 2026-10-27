package com.ccos.retro.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.ccos.retro.pro.KeepOut
import com.ccos.retro.pro.ProFrameGuard
import com.ccos.retro.pro.ProGoldMoment
import com.ccos.retro.pro.ProSparkleParams
import com.ccos.retro.pro.ProSparkleSim
import com.ccos.retro.pro.Resident
import com.ccos.retro.pro.SparkleLayer
import com.ccos.retro.pro.StaticSparkle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * vc160 Da Vinci LRT Pro Gold v2: the one-time unlock art inside the Pro sheet (plate, padlock, glow, #FFD27A rays,
 * two shock rings, a four-layer gold burst, 64 lingering twinkles, five lens glints). Hard-capped at 2.2 s
 * ([ProSparkleParams.totalMs]); clipped to this view so it never covers the rocket or readouts; sparkles are kept
 * out of the title/body boxes (fade 560-600 ms, clipped out from 600 ms). Sprites are pre-rendered bitmaps tinted
 * with cached PorterDuff filters: no per-frame paths for particles. Stops postOnAnimation at 2.2 s and on detach.
 */
class ProUnlockView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sprite = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val dst = RectF()
    private val filters = HashMap<Int, PorterDuffColorFilter>()

    private var mode = ProGoldMoment.Mode.NONE
    private var startMs = 0L
    private var params = ProSparkleParams.current
    private var sim: ProSparkleSim? = null
    private var running = false
    private var guard = ProFrameGuard()
    /** O9: degrade level reached in the last moment (0 = full field). Read it on the Razr. */
    val frameGuardLevel: Int get() = guard.level

    private var keepOutViews: List<View> = emptyList()
    private val keepRects = ArrayList<RectF>()
    private var residents: List<Resident>? = null
    private var staticField: List<StaticSparkle>? = null
    private val myLoc = IntArray(2)
    private val otherLoc = IntArray(2)

    private val starBmp: Bitmap by lazy { renderStar(64) }
    private val dotBmp: Bitmap by lazy { renderDot(32) }
    private val streakBmp: Bitmap by lazy { renderStreak(128, 8) }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    /** Text boxes sparkles must never cover (v2: "never over text"). */
    fun setKeepOutViews(vararg views: View) { keepOutViews = views.toList() }

    fun play(m: ProGoldMoment.Mode, p: ProSparkleParams = ProSparkleParams.current) {
        mode = m
        params = p
        sim = if (m == ProGoldMoment.Mode.ANIMATED) ProSparkleSim(p) else null
        residents = null; staticField = null
        guard = ProFrameGuard()
        startMs = SystemClock.uptimeMillis()
        running = m == ProGoldMoment.Mode.ANIMATED
        invalidate()
        if (running) postOnAnimation(tick)
    }

    fun stop() {
        running = false
        removeCallbacks(tick)
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            if (elapsed() < params.totalMs()) postOnAnimation(this) else running = false
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    private fun elapsed(): Long = (SystemClock.uptimeMillis() - startMs).coerceAtMost(params.totalMs())

    private fun easeOutCubic(x: Float): Float { val c = 1f - x.coerceIn(0f, 1f); return 1f - c * c * c }
    private fun span(t: Long, a: Long, b: Long): Float = ((t - a).toFloat() / (b - a).toFloat()).coerceIn(0f, 1f)

    private fun updateKeepRects() {
        keepRects.clear()
        if (keepOutViews.isEmpty()) return
        getLocationInWindow(myLoc)
        val pad = params.keepOutPadDp * d
        for (v in keepOutViews) {
            if (v.width <= 0 || v.visibility != VISIBLE) continue
            v.getLocationInWindow(otherLoc)
            val l = (otherLoc[0] - myLoc[0]).toFloat(); val t = (otherLoc[1] - myLoc[1]).toFloat()
            keepRects.add(RectF(l - pad, t - pad, l + v.width + pad, t + v.height + pad))
        }
    }

    private fun keepOutsDp(cx: Float, cy: Float): List<KeepOut> =
        keepRects.map { KeepOut((it.left - cx) / d, (it.top - cy) / d, (it.right - cx) / d, (it.bottom - cy) / d) }

    private fun inKeepOut(x: Float, y: Float): Boolean = keepRects.any { it.contains(x, y) }

    private fun clipOutText(c: Canvas) {
        for (r in keepRects) {
            if (Build.VERSION.SDK_INT >= 26) c.clipOutRect(r)
            else @Suppress("DEPRECATION") c.clipRect(r, Region.Op.DIFFERENCE)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mode == ProGoldMoment.Mode.NONE) return
        val t0 = System.nanoTime()
        drawMoment(canvas)
        if (mode == ProGoldMoment.Mode.ANIMATED && running) {
            val before = guard.level
            if (guard.record(System.nanoTime() - t0) != before && com.ccos.retro.BuildConfig.DEBUG)
                android.util.Log.d("ProUnlockView", "O9 frame guard -> level ${guard.level}")
        }
    }

    private fun drawMoment(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val static = mode == ProGoldMoment.Mode.STATIC
        val t = if (static) ProGoldMoment.HOLD_START_MS else elapsed()
        updateKeepRects()

        canvas.save()
        canvas.clipRect(0f, 0f, w, h) // plateRect: never past the sheet

        // Plate (#322A18 @ 88%) + 2dp metal border, fade-in 0-220 ease-out cubic.
        val plateA = if (static) 1f else easeOutCubic(span(t, 0, ProGoldMoment.PLATE_FADE_MS))
        val inset = 8f * d
        rect.set(inset, inset, w - inset, h - inset)
        paint.shader = null; paint.style = Paint.Style.FILL; paint.colorFilter = null
        paint.color = ProGold.PLATE; paint.alpha = (0xE0 * plateA).toInt()
        canvas.drawRoundRect(rect, 16f * d, 16f * d, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f * d
        paint.shader = ProGold.metalShader(w); paint.alpha = (255 * plateA).toInt()
        canvas.drawRoundRect(rect, 16f * d, 16f * d, paint)
        paint.shader = null; paint.style = Paint.Style.FILL
        canvas.clipRect(rect) // everything else stays on the plate

        val cx = w / 2f
        val cy = max(rect.top + 64f * d, h * 0.34f)
        val halfWdp = (rect.width() / 2f) / d
        val halfHdp = (rect.height() / 2f) / d

        // Glow + rays: rise to 450, fade to 1800. Static frame: frozen glow, rays at 22/255.
        val g = if (static) 0.6f else if (t < ProGoldMoment.SHACKLE_OPEN_END_MS) span(t, 0, ProGoldMoment.SHACKLE_OPEN_END_MS)
            else 1f - span(t, ProGoldMoment.SHACKLE_OPEN_END_MS, ProGoldMoment.GLOW_FADE_END_MS)
        if (g > 0f) {
            val radius = params.glowRadiusDp * d
            paint.shader = RadialGradient(cx, cy, radius,
                intArrayOf(Color.argb((160 * g).toInt(), 255, 235, 180), Color.argb((60 * g).toInt(), 255, 190, 90), Color.argb(0, 255, 150, 60)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
            val rot = if (static) 0f else max(0L, t - ProGoldMoment.SHACKLE_OPEN_END_MS) * params.rayRotateDegPerMs
            paint.color = params.rayColor
            paint.alpha = if (static) params.staticRayAlpha else (params.rayPeakAlpha * g).toInt()
            val len = radius * 1.4f
            for (i in 0 until params.rayCount) {
                val a0 = Math.toRadians((rot + i * 360f / params.rayCount - params.rayHalfAngleDeg).toDouble())
                val a1 = Math.toRadians((rot + i * 360f / params.rayCount + params.rayHalfAngleDeg).toDouble())
                path.reset(); path.moveTo(cx, cy)
                path.lineTo(cx + (cos(a0) * len).toFloat(), cy + (sin(a0) * len).toFloat())
                path.lineTo(cx + (cos(a1) * len).toFloat(), cy + (sin(a1) * len).toFloat())
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        // Padlock: 88dp, appears closed 0-130, shackle opens 130-450.
        val appear = if (static) 1f else easeOutCubic(span(t, 0, ProGoldMoment.LOCK_APPEAR_MS))
        val open = if (static) 1f else easeOutCubic(span(t, ProGoldMoment.LOCK_APPEAR_MS, ProGoldMoment.SHACKLE_OPEN_END_MS))
        drawPadlock(canvas, cx, cy, 44f * d * (0.85f + 0.15f * appear), appear, open)

        // FX layer: hard keep-out of the text boxes from 600 ms (and always in the static frame).
        canvas.save()
        if (static || t >= params.textKeepOutMs) clipOutText(canvas)
        val textFade = if (!static && t >= params.textFadeStartMs && t < params.textKeepOutMs)
            (params.textKeepOutMs - t).toFloat() / (params.textKeepOutMs - params.textFadeStartMs).toFloat() else 1f

        if (static) drawStatic(canvas, cx, cy, halfWdp, halfHdp)
        else {
            // Shock rings 130-600.
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * d; paint.color = ProGold.GOLD_HI
            for (i in 0 until params.ringCount) {
                val pr = params.ringProgress(i, t)
                if (pr < 0f) continue
                paint.alpha = (200 * (1f - pr)).toInt()
                canvas.drawCircle(cx, cy, (44f + (params.ringMaxRadiusDp - 44f) * easeOutCubic(pr)) * d, paint)
            }
            paint.style = Paint.Style.FILL

            val sm = sim
            val fade = params.fieldFade(t)
            if (sm != null && t >= params.burstStartMs) {
                val targetFrame = ((t - params.burstStartMs) / 16L).toInt()
                var guard = 0
                while (sm.frame < targetFrame && guard++ < 200) sm.step()
            }
            val res = residents ?: ProSparkleSim.residents(params, halfWdp, halfHdp, keepOutsDp(cx, cy)).also { residents = it }
            // Order: back -> dust -> mid -> residents -> front -> glints.
            for (li in params.layers.indices) {
                if (params.layers[li].name == "front") drawResidents(canvas, cx, cy, res, t)
                val L = params.layers[li]
                var n = 0
                sm?.live?.forEach { p ->
                    if (p.layer != li) return@forEach
                    if (!guard.keep(L.name, n++)) return@forEach
                    val x = cx + p.x * d; val y = cy + p.y * d
                    var a = p.alpha(L.alpha) * fade
                    if (textFade < 1f && inKeepOut(x, y)) a *= textFade
                    drawParticle(canvas, L, x, y, p.size * d, p.color, a.toInt())
                }
            }
            if (params.layers.none { it.name == "front" }) drawResidents(canvas, cx, cy, res, t)
            for (i in params.glintPeaksMs.indices) {
                val a = params.glintAlpha(i, t)
                if (a > 0f) params.glintOffsetsDp.getOrNull(i)?.let { (gx, gy) -> drawGlint(canvas, cx + gx * d, cy + gy * d, a) }
            }
        }
        canvas.restore()
        canvas.restore()
    }

    private fun drawStatic(c: Canvas, cx: Float, cy: Float, halfWdp: Float, halfHdp: Float) {
        val field = staticField ?: ProSparkleSim.staticField(params, halfWdp, halfHdp, keepOutsDp(cx, cy)).also { staticField = it }
        for (s in field) {
            val L = params.layers[s.layer]
            drawParticle(c, L, cx + s.x * d, cy + s.y * d, s.size * d, s.color, (255 * s.alpha).toInt())
        }
        for (i in 0 until minOf(params.staticGlints, params.glintOffsetsDp.size)) {
            val (gx, gy) = params.glintOffsetsDp[i]
            drawGlint(c, cx + gx * d, cy + gy * d, 0.85f)
        }
    }

    private fun drawResidents(c: Canvas, cx: Float, cy: Float, res: List<Resident>, t: Long) {
        for (r in res) {
            val a = r.alpha(t)
            if (a <= 0f) continue
            val x = cx + r.x * d; val y = cy + r.y * d; val s = r.size * d
            drawStar(c, x, y, s, r.color, (255 * a).toInt())
            core(c, x, y, s, (255 * a).toInt())
        }
    }

    private fun filter(color: Int): PorterDuffColorFilter =
        filters.getOrPut(color) { PorterDuffColorFilter(color or 0xFF000000.toInt(), PorterDuff.Mode.SRC_IN) }

    private fun blit(c: Canvas, bmp: Bitmap, x: Float, y: Float, hw: Float, hh: Float, color: Int, alpha: Int) {
        if (alpha <= 0) return
        sprite.colorFilter = filter(color); sprite.alpha = alpha.coerceIn(0, 255)
        dst.set(x - hw, y - hh, x + hw, y + hh)
        c.drawBitmap(bmp, null, dst, sprite)
    }

    /** Concave four-point astroid star, vertical arm 1.25x. */
    private fun drawStar(c: Canvas, x: Float, y: Float, size: Float, color: Int, alpha: Int) =
        blit(c, starBmp, x, y, size * 1.8f, size * 1.8f * 1.25f, color, alpha)

    private fun core(c: Canvas, x: Float, y: Float, size: Float, alpha: Int) {
        paint.colorFilter = null; paint.shader = null; paint.color = Color.WHITE; paint.alpha = alpha.coerceIn(0, 255)
        c.drawCircle(x, y, size * 0.35f, paint)
    }

    private fun drawParticle(c: Canvas, L: SparkleLayer, x: Float, y: Float, size: Float, color: Int, alpha: Int) {
        if (alpha <= 0) return
        val sDp = size / d
        when {
            L.blurred -> blit(c, dotBmp, x, y, size * 1.6f, size * 1.6f, color, alpha)
            sDp <= L.starAboveDp -> {
                paint.colorFilter = null; paint.shader = null; paint.color = color; paint.alpha = alpha
                c.drawCircle(x, y, size, paint)
            }
            else -> {
                if (L.glow) blit(c, dotBmp, x, y, size * 3f, size * 3f, color, alpha / 3)
                if (sDp > L.flareAboveDp) blit(c, streakBmp, x, y, size * 5f, size * 0.35f, color, alpha / 2)
                drawStar(c, x, y, size, color, alpha)
                if (sDp > L.subStarAboveDp) {
                    c.save(); c.rotate(45f, x, y)
                    drawStar(c, x, y, size * 0.5f, color, (alpha * 0.8f).toInt())
                    c.restore()
                }
                if (sDp > L.whiteCoreAboveDp) core(c, x, y, size, alpha)
            }
        }
    }

    /** Lens-flare glint: four-point star plus a soft horizontal streak. */
    private fun drawGlint(c: Canvas, x: Float, y: Float, a: Float) {
        val al = (255 * a).toInt()
        blit(c, dotBmp, x, y, 14f * d, 14f * d, ProGold.GOLD_HI, al / 3)
        blit(c, streakBmp, x, y, 36f * d, 1.6f * d, ProGold.GOLD_PALE, (al * 0.7f).toInt())
        drawStar(c, x, y, 5f * d, ProGold.GOLD_PALE, al)
        core(c, x, y, 5f * d, al)
    }

    private fun drawPadlock(c: Canvas, cx: Float, cy: Float, r: Float, alpha: Float, open: Float) {
        val a = (255 * alpha).toInt()
        paint.shader = null; paint.colorFilter = null
        paint.style = Paint.Style.FILL; paint.color = 0xFF0B0F14.toInt(); paint.alpha = a
        c.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d; paint.color = ProGold.GOLD_INK; paint.alpha = a
        c.drawCircle(cx, cy, r + 0.5f * d, paint)
        paint.strokeWidth = 4f * d; paint.color = ProGold.GOLD; paint.alpha = a
        c.drawCircle(cx, cy, r - 2.5f * d, paint)

        // Glyph (#FFD27A): body + shackle; the shackle lifts and swings free on its left leg.
        val u = r / 44f
        paint.color = ProGold.GOLD_HI; paint.alpha = a
        paint.style = Paint.Style.FILL
        rect.set(cx - 18f * u, cy - 2f * u, cx + 18f * u, cy + 20f * u)
        c.drawRoundRect(rect, 3f * u, 3f * u, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f * u; paint.strokeCap = Paint.Cap.ROUND
        c.save()
        val pivotX = cx - 11f * u; val pivotY = cy - 2f * u
        c.translate(0f, -7f * u * open)
        c.rotate(-28f * open, pivotX, pivotY)
        path.reset()
        path.moveTo(cx - 11f * u, cy - 2f * u)
        path.lineTo(cx - 11f * u, cy - 12f * u)
        rect.set(cx - 11f * u, cy - 23f * u, cx + 11f * u, cy - 1f * u)
        path.arcTo(rect, 180f, 180f)
        path.lineTo(cx + 11f * u, cy - 2f * u)
        c.drawPath(path, paint)
        c.restore()
        paint.strokeCap = Paint.Cap.BUTT
        paint.style = Paint.Style.FILL
    }

    companion object {
        /** White astroid (exponent 3), drawn once; tinted per draw. Width w, height w * 1.25. */
        fun renderStar(w: Int): Bitmap {
            val h = (w * 1.25f).toInt()
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
            val c = Canvas(b)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val path = Path()
            val rx = w / 2f; val ry = h / 2f
            val n = 96
            for (i in 0..n) {
                val th = 2.0 * Math.PI * i / n
                val cs = cos(th); val sn = sin(th)
                val x = rx + rx * (Math.signum(cs) * Math.abs(cs).pow(3.0)).toFloat()
                val y = ry + ry * (Math.signum(sn) * Math.abs(sn).pow(3.0)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            c.drawPath(path, p)
            return b
        }

        fun renderDot(s: Int): Bitmap {
            val b = Bitmap.createBitmap(s, s, Bitmap.Config.ALPHA_8)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(s / 2f, s / 2f, s / 2f, intArrayOf(Color.WHITE, Color.argb(0, 255, 255, 255)), null, Shader.TileMode.CLAMP)
            }
            Canvas(b).drawCircle(s / 2f, s / 2f, s / 2f, p)
            return b
        }

        fun renderStreak(w: Int, h: Int): Bitmap {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
            val clear = Color.argb(0, 255, 255, 255)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(clear, Color.WHITE, clear), null, Shader.TileMode.CLAMP)
            }
            Canvas(b).drawRect(0f, h * 0.25f, w.toFloat(), h * 0.75f, p)
            return b
        }
    }
}
