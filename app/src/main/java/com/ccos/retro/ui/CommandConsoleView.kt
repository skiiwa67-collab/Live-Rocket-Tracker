package com.ccos.retro.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ccos.retro.BuildConfig
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.geo.GeoAtlas
import com.ccos.retro.geo.PadBook
import com.ccos.retro.geo.PadGlyph
import com.ccos.retro.geo.GeoDraw
import com.ccos.retro.event.FlightProfiles
import com.ccos.retro.event.MissionBrief
import com.ccos.retro.event.MissionFacts
import com.ccos.retro.event.VehicleCatalog
import com.ccos.retro.event.EngineDraw
import com.ccos.retro.event.VehicleOutline
import com.ccos.retro.event.VehicleDraw
import com.ccos.retro.event.EventClock
import com.ccos.retro.event.FlightEvent
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.module.RocketTelemetryModule
import com.ccos.retro.skin.TelemetrySkin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Full-screen MCC console. Dark grid + agency skin.
 * Screens: TEL, TRAJ, STG1, STG2, ENG, PROP, MISS.
 */
class CommandConsoleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var screen: Int = 0
        set(value) {
            field = value.coerceIn(0, 6)
            invalidate()
        }

    var onScreenChanged: ((Int) -> Unit)? = null
    var onAnalogChanged: (() -> Unit)? = null
    var failedSystem: String? = null
    var eventTape: List<FlightEvent> = emptyList()

    private var module: RocketTelemetryModule? = null
    private var prefs: AppPrefs? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val tmpPath = Path()
    private val viewClip = Path()
    private val landClip = Path()
    private val tmpRect = RectF()
    private val propStg1Hit = RectF()
    private val propStg2Hit = RectF()
    private val analogHit = RectF()
    private val geoHit = RectF()
    private val trajHandler = Handler(Looper.getMainLooper())
    private var trajMapBmp: Bitmap? = null
    private var trajMapKey = Long.MIN_VALUE
    @Volatile private var trajMapBusy = false
    private var padCacheId: String? = null
    private var padCacheLon = 0f
    private var padCacheLat = 0f
    private var padCacheAz = 90f
    private var padCacheWest = false
    private val trajPast = Path()
    private val trajFuture = Path()
    private val colWater = Color.parseColor("#0A3A58")
    private val colTrack = Color.parseColor("#00D4FF")


    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val p = prefs ?: return false
            if (screen == 0 && analogHit.contains(e.x, e.y)) {
                p.telemetryAnalog = e.x >= analogHit.centerX()
                onAnalogChanged?.invoke()
                invalidate()
                return true
            }
            if (!geoHit.isEmpty && geoHit.contains(e.x, e.y)) {
                val launch = module?.tracked
                val ll = PadBook.lonLat(launch)
                if (ll != null) {
                    val (lon, lat) = ll
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, PadGlyph.earthUri(lat, lon))
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                    return true
                }
            }
            // TEL / TRAJ / ENG / PROP all share the STG1·STG2 picker.
            if (screen !in intArrayOf(0, 1, 4, 5)) return false
            when {
                propStg1Hit.contains(e.x, e.y) -> p.trackedStage = 1
                propStg2Hit.contains(e.x, e.y) -> p.trackedStage = 2
                else -> return false
            }
            invalidate()
            return true
        }
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (abs(velocityX) < 650f || abs(velocityX) < abs(velocityY)) return false
            screen = if (velocityX < 0) (screen + 1) % 7 else (screen + 6) % 7
            onScreenChanged?.invoke(screen)
            invalidate()
            return true
        }
    })

    init {
        isClickable = true
        isFocusable = true
    }

    fun bind(module: RocketTelemetryModule, prefs: AppPrefs) {
        this.module = module
        this.prefs = prefs
        GeoAtlas.ensure(context) { postInvalidateOnAnimation() }
        PadBook.ensure(context)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        trajMapBmp?.recycle()
        trajMapBmp = null
        trajMapKey = Long.MIN_VALUE
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gesture.onTouchEvent(event)
        return handled || super.onTouchEvent(event)
    }

    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
    private fun minSp(): Float = sp(10f)
    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun lamp(): Float = prefs?.lampBrightness ?: 1f

    private fun withLamp(c: Int, lamp: Float = lamp()): Int {
        val k = lamp.coerceIn(0.2f, 1f)
        return Color.argb(
            Color.alpha(c),
            (Color.red(c) * k).toInt().coerceIn(0, 255),
            (Color.green(c) * k).toInt().coerceIn(0, 255),
            (Color.blue(c) * k).toInt().coerceIn(0, 255)
        )
    }

    private fun lampAlpha(c: Int, lamp: Float, alpha: Float): Int {
        val base = withLamp(c, lamp)
        val a = (Color.alpha(base) * alpha.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(base), Color.green(base), Color.blue(base))
    }

    private fun fitText(text: String, maxW: Float, maxH: Float, wish: Float): Float {
        val min = minSp()
        var size = min(wish, maxH * 0.92f).coerceAtLeast(min)
        textPaint.textSize = size
        if (maxW > 4f) {
            while (size > min && textPaint.measureText(text) > maxW) {
                size -= 0.5f
                textPaint.textSize = size
            }
        }
        return size
    }


    private fun drawPadVersionMark(canvas: Canvas, x: Float, y: Float, color: Int) {
        val oldTf = textPaint.typeface
        val oldAlign = textPaint.textAlign
        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        textPaint.typeface = Typeface.MONOSPACE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = sp(18f)
        textPaint.color = color
        canvas.drawText(BuildConfig.VERSION_CODE.toString(), x, y, textPaint)
        textPaint.typeface = oldTf
        textPaint.textAlign = oldAlign
        textPaint.textSize = oldSize
        textPaint.color = oldColor
    }

    private fun drawLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        color: Int,
        maxW: Float,
        maxH: Float,
        wish: Float,
        align: Paint.Align = Paint.Align.CENTER
    ) {
        var shown = text
        val size = fitText(shown, maxW, maxH, wish)
        textPaint.textSize = size
        if (maxW > 8f && textPaint.measureText(shown) > maxW) {
            val tp = TextPaint(textPaint)
            shown = TextUtils.ellipsize(shown, tp, maxW, TextUtils.TruncateAt.END).toString()
        }
        textPaint.color = color
        textPaint.textAlign = align
        textPaint.textSize = size
        canvas.save()
        val left = when (align) {
            Paint.Align.LEFT -> x
            Paint.Align.RIGHT -> x - maxW
            else -> x - maxW * 0.5f
        }
        canvas.clipRect(left, y - maxH, left + maxW, y + maxH * 0.4f)
        canvas.drawText(shown, x, y, textPaint)
        canvas.restore()
    }

    /** Word-wrap. No ellipsis. Returns height consumed. */
    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        topY: Float,
        color: Int,
        maxW: Float,
        wish: Float,
        align: Paint.Align = Paint.Align.CENTER,
        maxLines: Int = 6
    ): Float {
        if (text.isBlank() || maxW < 8f) return 0f
        val tp = TextPaint(textPaint)
        tp.textSize = wish
        tp.color = color
        tp.textAlign = Paint.Align.LEFT
        val alignment = when (align) {
            Paint.Align.CENTER -> Layout.Alignment.ALIGN_CENTER
            Paint.Align.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, maxW.toInt().coerceAtLeast(1))
            .setAlignment(alignment)
            .setMaxLines(maxLines.coerceAtLeast(1))
            .setEllipsize(null)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.05f)
            .build()
        val left = when (align) {
            Paint.Align.LEFT -> x
            Paint.Align.RIGHT -> x - maxW
            else -> x - maxW * 0.5f
        }
        canvas.save()
        canvas.translate(left, topY)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t0 = SystemClock.uptimeMillis()
        val w = width.toFloat()
        val h = height.toFloat()
        if (w < 8f || h < 8f) return
        val mod = module
        val p = prefs
        val launch = mod?.tracked
        val skin = TelemetrySkin.forLaunch(launch)
        val tSec = mod?.effectiveSecondsFromNet() ?: 0f
        canvas.save()
        canvas.clipRect(0f, 0f, w, h)
        canvas.drawColor(skin.bg)
        if (screen != 1) drawGrid(canvas, w, h, skin)
        val stripH = drawEventClockStrip(canvas, w, launch, tSec, skin)
        if (mod == null || p == null) {
            drawLabel(canvas, "NO MODULE", w * 0.5f, h * 0.5f, Color.WHITE, w * 0.8f, sp(22f), sp(18f))
            canvas.restore()
            return
        }
        canvas.save()
        canvas.translate(0f, stripH)
        val bh = (h - stripH).coerceAtLeast(8f)
        when (screen) {
            0 -> drawTel(canvas, w, bh, launch, tSec, skin, p)
            1 -> drawTraj(canvas, w, bh, launch, tSec, skin)
            2 -> drawStage(canvas, w, bh, launch, tSec, skin, p, stage = 1)
            3 -> drawStage(canvas, w, bh, launch, tSec, skin, p, stage = 2)
            4 -> drawEng(canvas, w, bh, launch, tSec, skin, p)
            5 -> drawProp(canvas, w, bh, launch, tSec, skin)
            else -> drawMiss(canvas, w, bh, launch, tSec, skin, p)
        }
        canvas.restore()
        if (stripH > 0f && screen in intArrayOf(0, 1, 4, 5)) {
            propStg1Hit.offset(0f, stripH)
            propStg2Hit.offset(0f, stripH)
        }
        if (stripH > 0f && !geoHit.isEmpty) geoHit.offset(0f, stripH)
        canvas.restore()
        val dt = SystemClock.uptimeMillis() - t0
        if (dt > 32) Log.w("CCOS.TRAJ", "onDraw ${dt}ms screen=$screen")
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float, skin: TelemetrySkin.Tokens) {
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1f
        strokePaint.color = withLamp(skin.grid)
        val step = dp(28f)
        var x = 0f
        while (x <= w) {
            canvas.drawLine(x, 0f, x, h, strokePaint)
            x += step
        }
        var y = 0f
        while (y <= h) {
            canvas.drawLine(0f, y, w, y, strokePaint)
            y += step
        }
        // Faint lamp wash — hardware overhead, not a wash-out of telemetry.
        val ac = withLamp(skin.accentDim, lamp() * 0.55f)
        fillPaint.shader = RadialGradient(
            w * 0.50f, 0f, h * 0.62f,
            Color.argb(22, Color.red(ac), Color.green(ac), Color.blue(ac)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, fillPaint)
        fillPaint.shader = null
        // 1px panel seams behind the pages.
        strokePaint.strokeWidth = 1f
        strokePaint.color = Color.argb(42, 200, 210, 220)
        canvas.drawLine(0f, h * 0.28f, w, h * 0.28f, strokePaint)
        canvas.drawLine(0f, h * 0.72f, w, h * 0.72f, strokePaint)
        canvas.drawLine(w * 0.07f, 0f, w * 0.07f, h, strokePaint)
        canvas.drawLine(w * 0.93f, 0f, w * 0.93f, h, strokePaint)
        // Agency bezel ticks along the unused top/bottom rails.
        strokePaint.color = withLamp(skin.accent, lamp() * 0.38f)
        val tickN = 24
        for (i in 0..tickN) {
            val tx = w * (0.10f + 0.80f * i / tickN)
            val th = if (i % 4 == 0) dp(9f) else dp(4.5f)
            canvas.drawLine(tx, dp(3f), tx, dp(3f) + th, strokePaint)
            canvas.drawLine(tx, h - dp(3f), tx, h - dp(3f) - th, strokePaint)
        }
        strokePaint.strokeWidth = 2.4f
        strokePaint.color = withLamp(skin.accent, lamp() * 0.7f)
        val m = dp(10f)
        val b = dp(18f)
        canvas.drawLine(m, m, m + b, m, strokePaint)
        canvas.drawLine(m, m, m, m + b, strokePaint)
        canvas.drawLine(w - m, m, w - m - b, m, strokePaint)
        canvas.drawLine(w - m, m, w - m, m + b, strokePaint)
        canvas.drawLine(m, h - m, m + b, h - m, strokePaint)
        canvas.drawLine(m, h - m, m, h - m - b, strokePaint)
        canvas.drawLine(w - m, h - m, w - m - b, h - m, strokePaint)
        canvas.drawLine(w - m, h - m, w - m, h - m - b, strokePaint)
        val screw = dp(4.2f)
        val inset = m + dp(5f)
        drawScrew(canvas, inset, inset, screw, skin)
        drawScrew(canvas, w - inset, inset, screw, skin)
        drawScrew(canvas, inset, h - inset, screw, skin)
        drawScrew(canvas, w - inset, h - inset, screw, skin)
    }

    private fun drawScrew(canvas: Canvas, x: Float, y: Float, r: Float, skin: TelemetrySkin.Tokens) {
        fillPaint.shader = RadialGradient(
            x - r * 0.25f, y - r * 0.25f, r * 1.4f,
            Color.parseColor("#6A727C"), Color.parseColor("#1A1E24"), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, r, fillPaint)
        fillPaint.shader = null
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.1f
        strokePaint.color = Color.argb(160, 12, 14, 16)
        canvas.drawCircle(x, y, r, strokePaint)
        strokePaint.strokeWidth = 1.4f
        strokePaint.color = withLamp(skin.bg, 0.85f)
        canvas.drawLine(x - r * 0.55f, y, x + r * 0.55f, y, strokePaint)
        canvas.drawLine(x, y - r * 0.55f, x, y + r * 0.55f, strokePaint)
    }

    /**
     * LAST / NOW / NEXT on every MCC page. Catalog marks only.
     * On a mark (~0.5s) NOW is the event name, not "NEXT <name> T+00:00".
     */
    private fun drawEventClockStrip(
        canvas: Canvas,
        w: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens
    ): Float {
        val rowH = sp(13f)
        val pad = dp(5f)
        val stripH = rowH * 2f + pad * 2f + dp(4f)
        val left = dp(8f)
        val right = w - dp(8f)
        val top = dp(2f)
        val bot = top + stripH
        fillPaint.shader = LinearGradient(
            0f, top, 0f, bot,
            Color.parseColor("#1A222C"), Color.parseColor("#0A1016"),
            Shader.TileMode.CLAMP
        )
        tmpRect.set(left, top, right, bot)
        canvas.drawRoundRect(tmpRect, 6f, 6f, fillPaint)
        fillPaint.shader = null
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.4f
        strokePaint.color = withLamp(skin.accent, lamp() * 0.55f)
        canvas.drawRoundRect(tmpRect, 6f, 6f, strokePaint)
        val sc = dp(3.2f)
        drawScrew(canvas, left + dp(8f), (top + bot) * 0.5f, sc, skin)
        drawScrew(canvas, right - dp(8f), (top + bot) * 0.5f, sc, skin)
        drawPadVersionMark(canvas, w * 0.5f, top + dp(10f), withLamp(skin.muted))
        val (last, next) = EventClock.lastNext(launch, tSec)
        val on = when {
            last != null && abs(last.tSec - tSec) <= 0.5f -> last
            next != null && abs(next.tSec - tSec) <= 0.5f -> next
            else -> null
        }
        val lastStr = last?.let { "LAST  ${it.title.take(18)}  ${EventClock.fmt(it.tSec)}" } ?: "LAST  —"
        val nowStr = if (on != null) "NOW  ${on.title.take(16)}  ${formatClock(tSec)}"
        else "NOW  ${formatClock(tSec)}"
        val nextStr = next?.let { "NEXT  ${it.title.take(18)}" } ?: "NEXT  —"
        val inStr = if (next != null) EventClock.remain(tSec, next.tSec) else ""
        val innerL = left + dp(16f)
        val innerR = right - dp(16f)
        val innerW = innerR - innerL
        val y1 = top + pad + rowH * 0.85f
        val y2 = y1 + rowH + dp(2f)
        drawLabel(canvas, lastStr, innerL, y1, withLamp(skin.hold), innerW * 0.54f, rowH, sp(11f), Paint.Align.LEFT)
        drawLabel(
            canvas, nowStr, innerR, y1,
            withLamp(if (on != null) skin.hold else if (tSec >= 0f) skin.go else skin.hold),
            innerW * 0.44f, rowH * 1.05f, sp(12f), Paint.Align.RIGHT
        )
        drawLabel(canvas, nextStr, innerL, y2, withLamp(skin.text), innerW * 0.62f, rowH, sp(11f), Paint.Align.LEFT)
        if (inStr.isNotEmpty()) {
            drawLabel(canvas, inStr, innerR, y2, withLamp(skin.go), innerW * 0.34f, rowH, sp(12f), Paint.Align.RIGHT)
        }
        return stripH + dp(4f)
    }

    private fun formatClock(tSec: Float): String {
        val sign = if (tSec < 0f) "T-" else "T+"
        val absSec = abs(tSec).toInt()
        val hh = absSec / 3600
        val mm = (absSec % 3600) / 60
        val ss = absSec % 60
        val base = if (hh > 0) String.format("%s%02d:%02d:%02d", sign, hh, mm, ss)
        else String.format("%s%02d:%02d", sign, mm, ss)
        return if (module?.clockIsEst() == true) "$base EST" else base
    }

    private fun isMethalox(launch: LaunchSnapshot?): Boolean = VehicleCatalog.spec(launch).methalox

    private fun fuelColor(launch: LaunchSnapshot?): Int =
        if (isMethalox(launch)) Color.parseColor("#C8E6F5") else Color.parseColor("#E07A2F")

    private fun fuelName(launch: LaunchSnapshot?): String = VehicleCatalog.spec(launch).fuelName

    // ------------------------------------------------------------------ TEL
    private fun drawTel(
        canvas: Canvas,
        w: Float,
        h: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs
    ) {
        val pad = dp(10f)
        val subH = sp(18f)
        var y = pad + subH
        val name = (launch?.name ?: "NO TRACKED LAUNCH").uppercase().take(36)
        drawLabel(canvas, name, w * 0.5f, y, withLamp(skin.text), w * 0.92f, subH, sp(16f))
        y += subH + dp(2f)
        val vehPad = buildString {
            append((launch?.rocketName ?: "—").take(16))
            append(" · ")
            append((launch?.pad ?: "PAD").take(12))
        }
        drawLabel(canvas, vehPad, w * 0.5f, y, withLamp(skin.muted), w * 0.92f, subH, sp(14f))
        drawPadVersionMark(canvas, w * 0.5f, y - subH * 0.72f, withLamp(skin.muted))

        val (altKm, speedKmh, phase) = approximateProfile(tSec, launch, prefs.trackedStage)
        y += dp(8f)
        val pillH = dp(28f)
        drawStagePills(canvas, w, y, pillH, skin, prefs.trackedStage, analogChip = true)
        y += pillH + dp(8f)
        val gaugeTop = y
        val gaugeBot = h * 0.50f
        if (prefs.telemetryAnalog) {
            drawFlightGauges(canvas, launch, tSec, altKm, speedKmh, skin, prefs, pad, gaugeTop, w - pad, gaugeBot)
        } else {
            drawDigitalGauges(canvas, launch, tSec, altKm, speedKmh, skin, prefs, pad, gaugeTop, w - pad, gaugeBot)
        }

        val readTop = gaugeBot + dp(10f)
        val tapeBot = h - pad
        drawEventTape(canvas, tSec, altKm, speedKmh, phase, skin, prefs, pad, readTop, w - pad, tapeBot)
    }

    private data class GaugeItem(val lab: String, val value: String, val frac: Float)

    private fun gaugeItems(
        launch: LaunchSnapshot?,
        tSec: Float,
        altKm: Float,
        speedKmh: Float,
        prefs: AppPrefs
    ): Pair<Array<GaugeItem>, Int> {
        val stage = hudStage(launch, tSec)
        val engines = if (launch != null) engineCountForStage(launch, stage) else 9
        val sep = if (launch != null) sepTime(launch) else 154f
        val lit = FlightProfiles.enginesLit(tSec, launch, stage, engines)
        val accel = FlightProfiles.accelG(tSec, launch)
        val imperial = prefs.useImperial
        val altVal = if (imperial) altKm * 0.621371f else altKm
        val spdVal = if (imperial) speedKmh * 0.621371f else speedKmh
        val fuel = fuelRemain(tSec, stage, launch)
        val items = arrayOf(
            GaugeItem("ALT EST", String.format("%.1f", altVal), (altKm / 200f).coerceIn(0f, 1f)),
            GaugeItem("SPD EST", String.format("%,.0f", spdVal), (speedKmh / 28000f).coerceIn(0f, 1f)),
            GaugeItem("ACCEL", String.format("%.2f", accel), (accel / 4f).coerceIn(0f, 1f)),
            GaugeItem("ENG", "$lit/$engines", (lit.toFloat() / engines.coerceAtLeast(1)).coerceIn(0f, 1f))
        )
        return items to lit
    }

    private fun currentAccel(tSec: Float, launch: LaunchSnapshot? = null): Float =
        FlightProfiles.accelG(tSec, launch)

    /** Operator-selected stage. Default 1. Never silently flip to STG2 after sep. */
    private fun trackedOr(prefs: AppPrefs?, fallback: Int = 1): Int =
        (prefs?.trackedStage ?: fallback).coerceIn(1, 2)

    /** Home HUD follows the flying stage. STG tab still uses trackedOr. */
    private fun hudStage(launch: LaunchSnapshot?, tSec: Float): Int {
        if (launch == null) return 1
        val sep = sepTime(launch)
        if (tSec < sep) return 1
        return (prefs?.trackedStage ?: 1).coerceIn(1, 2)
    }

    private fun drawDigitalGauges(
        canvas: Canvas,
        launch: LaunchSnapshot?,
        tSec: Float,
        altKm: Float,
        speedKmh: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs,
        left: Float,
        top: Float,
        right: Float,
        bot: Float
    ) {
        val (items, _) = gaugeItems(launch, tSec, altKm, speedKmh, prefs)
        val live = tSec >= 0f
        val cellW = (right - left) / items.size
        val gH = (bot - top).coerceAtLeast(sp(48f))
        val labH = (gH * 0.28f).coerceAtLeast(minSp())
        val valH = gH * 0.55f
        items.forEachIndexed { i, item ->
            val x = left + cellW * (i + 0.5f)
            val cell = RectF(
                left + cellW * i + dp(4f), top,
                left + cellW * (i + 1) - dp(4f), bot
            )
            fillPaint.shader = null
            fillPaint.color = Color.argb(160, 8, 10, 14)
            canvas.drawRoundRect(cell, 6f, 6f, fillPaint)
            drawLabel(
                canvas, item.value, x, top + valH * 0.92f,
                withLamp(if (live || item.lab == "ACCEL") skin.text else skin.muted),
                cellW * 0.90f, valH, sp(28f)
            )
            drawLabel(
                canvas, item.lab, x, bot - dp(8f),
                withLamp(skin.muted), cellW * 0.90f, labH, sp(14f)
            )
        }
    }

    private fun drawFlightGauges(
        canvas: Canvas,
        launch: LaunchSnapshot?,
        tSec: Float,
        altKm: Float,
        speedKmh: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs,
        left: Float,
        top: Float,
        right: Float,
        bot: Float
    ) {
        val (items, lit) = gaugeItems(launch, tSec, altKm, speedKmh, prefs)
        val cellW = (right - left) / items.size
        val gH = (bot - top).coerceAtLeast(24f)
        val topBand = gH * 0.14f
        val botBand = gH * 0.14f
        val live = tSec >= 0f
        val imperial = prefs.useImperial
        val altVal = if (imperial) altKm * 0.621371f else altKm
        val accel = currentAccel(tSec, launch)
        val stage = hudStage(launch, tSec)
        val fuel = fuelRemain(tSec, stage, launch)
        items.forEachIndexed { i, item ->
            val x = left + cellW * (i + 0.5f)
            val cellL = left + cellW * i + dp(3f)
            val cellR = left + cellW * (i + 1) - dp(3f)
            drawLabel(
                canvas, item.value, x, top + topBand * 0.88f,
                withLamp(if (live || item.lab == "ACCEL") skin.text else skin.muted),
                cellW * 0.92f, topBand, sp(15f)
            )
            val face = RectF(cellL, top + topBand, cellR, bot - botBand)
            drawInstrumentCan(canvas, face, skin)
            canvas.save()
            canvas.clipRect(face)
            when {
                item.lab.startsWith("ALT") -> drawAltTape(canvas, face, altVal, imperial, skin)
                item.lab.startsWith("SPD") -> drawSpdBar(canvas, face, speedKmh, imperial, skin)
                item.lab.startsWith("ACCEL") -> drawAccelColumn(canvas, face, accel, skin)
                item.lab.startsWith("ENG") -> {
                    val sz = min(face.width(), face.height()) * 0.90f
                    drawEngineHardware(canvas, face.centerX(), face.centerY(), sz, launch, stage, lit, skin)
                    if (failedSystem != null) {
                        fillPaint.shader = null
                        fillPaint.color = Color.argb(120, 180, 16, 16)
                        canvas.drawRect(face, fillPaint)
                        drawLabel(
                            canvas, "FAIL", face.centerX(), face.centerY() + sp(5f),
                            Color.WHITE, face.width() * 0.9f, sp(16f), sp(14f)
                        )
                    }
                }
                item.lab.startsWith("FUEL") -> {
                    drawFuelTanks(canvas, face, fuel, fuel, skin, launch, compact = true)
                }
            }
            canvas.restore()
            drawLabel(
                canvas, item.lab, x, bot - dp(2f),
                withLamp(skin.muted), cellW * 0.92f, botBand, sp(14f)
            )
        }
    }

    private fun drawInstrumentCan(canvas: Canvas, can: RectF, skin: TelemetrySkin.Tokens) {
        fillPaint.shader = null
        fillPaint.color = Color.argb(235, 10, 12, 16)
        canvas.drawRoundRect(can, 7f, 7f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 3.2f
        strokePaint.color = withLamp(skin.accent)
        canvas.drawRoundRect(can, 7f, 7f, strokePaint)
        strokePaint.strokeWidth = 1.1f
        strokePaint.color = Color.argb(80, 255, 255, 255)
        tmpRect.set(can.left + 3f, can.top + 3f, can.right - 3f, can.bottom - 3f)
        canvas.drawRoundRect(tmpRect, 5f, 5f, strokePaint)
        val screw = dp(3.4f)
        val pad = dp(6f)
        if (can.width() > dp(36f) && can.height() > dp(36f)) {
            drawScrew(canvas, can.left + pad, can.top + pad, screw, skin)
            drawScrew(canvas, can.right - pad, can.top + pad, screw, skin)
            drawScrew(canvas, can.left + pad, can.bottom - pad, screw, skin)
            drawScrew(canvas, can.right - pad, can.bottom - pad, screw, skin)
        }
    }

    private fun drawAltTape(canvas: Canvas, face: RectF, altVal: Float, imperial: Boolean, skin: TelemetrySkin.Tokens) {
        val well = RectF(
            face.left + face.width() * 0.16f,
            face.top + dp(5f),
            face.right - face.width() * 0.28f,
            face.bottom - dp(5f)
        )
        fillPaint.shader = null
        fillPaint.color = Color.parseColor("#07090C")
        canvas.drawRoundRect(well, 4f, 4f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        strokePaint.color = withLamp(skin.accent, lamp() * 0.5f)
        canvas.drawRoundRect(well, 4f, 4f, strokePaint)

        val (lo, hi) = analogAltWindow(altVal, imperial)
        val span = (hi - lo).coerceAtLeast(1f)
        val v = altVal.coerceIn(lo, hi)
        fun yFor(a: Float) = well.bottom - ((a - lo) / span) * well.height()

        canvas.save()
        canvas.clipRect(well.left + 1f, well.top + 1f, well.right - 1f, well.bottom - 1f)
        val step = if (span <= 8f) 1 else if (span <= 25f) 5 else 10
        val majorEvery = if (span <= 8f) 5 else if (span <= 25f) 5 else if (span <= 60f) 10 else 50
        val t0 = lo.toInt()
        val t1 = hi.toInt()
        var t = t0
        while (t <= t1) {
            if (true) {
                val y = yFor(t.toFloat())
                val major = t == t0 || t == t1 || t % majorEvery == 0
                strokePaint.strokeCap = Paint.Cap.SQUARE
                strokePaint.strokeWidth = if (major) 2.2f else 1.1f
                strokePaint.color = withLamp(if (major) skin.text else skin.muted)
                val tickW = if (major) well.width() * 0.48f else well.width() * 0.24f
                canvas.drawLine(well.right - tickW, y, well.right - dp(3f), y, strokePaint)
                if (major && t in 0..200) {
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.color = withLamp(skin.text)
                    textPaint.textSize = minSp()
                    canvas.drawText(t.toString(), well.right - tickW - dp(3f), y + sp(5f), textPaint)
                }
            }
            t += step
        }
        canvas.restore()

        val py = yFor(v)
        val winH = (well.height() * 0.14f).coerceAtLeast(sp(16f))
        val win = RectF(well.left + dp(2f), py - winH * 0.5f, well.right - dp(2f), py + winH * 0.5f)
        strokePaint.strokeWidth = 2.4f
        strokePaint.color = withLamp(skin.hold)
        canvas.drawRoundRect(win, 3f, 3f, strokePaint)
        tmpPath.reset()
        tmpPath.moveTo(well.right + dp(2f), py)
        tmpPath.lineTo(face.right - dp(3f), py - dp(7f))
        tmpPath.lineTo(face.right - dp(3f), py + dp(7f))
        tmpPath.close()
        fillPaint.color = withLamp(skin.hold)
        canvas.drawPath(tmpPath, fillPaint)
        val unit = if (imperial) "mi" else "km"
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = sp(14f)
        textPaint.color = withLamp(skin.hold)
        canvas.drawText(analogRangeText(lo, hi, unit), face.centerX(), face.bottom - dp(3f), textPaint)
    }

    private fun analogBand(value: Float, edges: FloatArray): Pair<Float, Float> {
        val v = value.coerceAtLeast(0f)
        var i = 1
        while (i < edges.lastIndex && v >= edges[i]) i++
        return edges[i - 1] to edges[i]
    }

    private fun analogSpeedWindow(speedKmh: Float, imperial: Boolean): Pair<Float, Float> {
        val v = if (imperial) speedKmh * 0.621371f else speedKmh
        return analogBand(
            v,
            if (imperial) floatArrayOf(0f, 300f, 1000f, 3000f, 10000f, 18000f)
            else floatArrayOf(0f, 500f, 1600f, 5000f, 16000f, 28000f)
        )
    }

    private fun analogAltWindow(altVal: Float, imperial: Boolean): Pair<Float, Float> =
        analogBand(
            altVal,
            if (imperial) floatArrayOf(0f, 5f, 20f, 50f, 120f, 250f)
            else floatArrayOf(0f, 8f, 30f, 80f, 200f)
        )

    private fun analogRangeText(lo: Float, hi: Float, unit: String): String {
        fun n(x: Float): String =
            if (x >= 100f) String.format("%,.0f", x)
            else if (abs(x - x.toInt()) < 0.05f) String.format("%.0f", x)
            else String.format("%.1f", x)
        return if (lo <= 0.01f) "0–${n(hi)} $unit" else "${n(lo)}–${n(hi)} $unit"
    }

    private fun drawSpdBar(canvas: Canvas, face: RectF, speedKmh: Float, imperial: Boolean, skin: TelemetrySkin.Tokens) {
        val (lo, hi) = analogSpeedWindow(speedKmh, imperial)
        val v = if (imperial) speedKmh * 0.621371f else speedKmh
        val frac = ((v.coerceAtLeast(0f) - lo) / (hi - lo).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val n = 12
        val pad = dp(6f)
        val barH = (face.height() * 0.28f).coerceIn(dp(14f), dp(28f))
        val inner = RectF(
            face.left + pad,
            face.centerY() - barH * 0.5f,
            face.right - pad,
            face.centerY() + barH * 0.5f
        )
        val gap = dp(2.4f)
        val segW = (inner.width() - gap * (n - 1)) / n
        val litN = kotlin.math.round(frac * n).toInt().coerceIn(0, n)
        fillPaint.shader = null
        for (i in 0 until n) {
            val l = inner.left + i * (segW + gap)
            val r = l + segW
            val on = i < litN
            fillPaint.color = if (on) {
                val u = i / (n - 1).toFloat()
                Color.rgb(
                    (30 + u * 40).toInt(),
                    (210 - u * 70).toInt(),
                    (90 + u * 80).toInt()
                )
            } else {
                Color.parseColor("#182028")
            }
            canvas.drawRoundRect(l, inner.top, r, inner.bottom, 3f, 3f, fillPaint)
        }
        val nx = inner.left + inner.width() * frac.coerceIn(0f, 1f)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 3.2f
        strokePaint.color = Color.parseColor("#F2C14E")
        canvas.drawLine(nx, inner.top - dp(10f), nx, inner.bottom + dp(10f), strokePaint)
        fillPaint.color = Color.parseColor("#F2C14E")
        canvas.drawCircle(nx, inner.top - dp(10f), dp(3.2f), fillPaint)
        strokePaint.strokeWidth = 1.2f
        strokePaint.color = withLamp(skin.muted)
        canvas.drawLine(inner.left, inner.bottom + dp(12f), inner.right, inner.bottom + dp(12f), strokePaint)
        val unit = if (imperial) "mph" else "km/h"
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = sp(14f)
        textPaint.color = withLamp(skin.hold)
        canvas.drawText(
            analogRangeText(lo, hi, unit),
            inner.centerX(),
            inner.bottom + dp(22f),
            textPaint
        )
    }

    private fun drawAccelColumn(canvas: Canvas, face: RectF, accel: Float, skin: TelemetrySkin.Tokens) {
        val col = RectF(
            face.centerX() - face.width() * 0.16f,
            face.top + dp(8f),
            face.centerX() + face.width() * 0.16f,
            face.bottom - dp(8f)
        )
        fillPaint.shader = null
        fillPaint.color = Color.parseColor("#07090C")
        canvas.drawRoundRect(col, 5f, 5f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.6f
        strokePaint.color = withLamp(skin.accent, lamp() * 0.45f)
        canvas.drawRoundRect(col, 5f, 5f, strokePaint)
        val y3 = col.bottom - col.height() * (3f / 4f)
        fillPaint.color = Color.argb(110, 200, 28, 28)
        canvas.drawRect(col.left + 2f, col.top + 2f, col.right - 2f, y3, fillPaint)
        val y1 = col.bottom - col.height() * (1f / 4f)
        strokePaint.strokeWidth = 2.6f
        strokePaint.color = Color.WHITE
        canvas.drawLine(col.left - dp(5f), y1, col.right + dp(5f), y1, strokePaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = minSp()
        textPaint.color = withLamp(skin.muted)
        canvas.drawText("4g", col.left - dp(6f), col.top + sp(12f), textPaint)
        canvas.drawText("1g", col.left - dp(6f), y1 + sp(4f), textPaint)
        canvas.drawText("0", col.left - dp(6f), col.bottom - dp(2f), textPaint)
        val g = accel.coerceIn(0f, 4f)
        val by = col.bottom - col.height() * (g / 4f)
        fillPaint.color = withLamp(if (g > 3f) skin.danger else skin.go, 0.85f)
        canvas.drawRect(col.left + 3f, by, col.right - 3f, col.bottom - 3f, fillPaint)
        val br = (col.width() * 0.22f).coerceIn(dp(3.5f), dp(7f))
        fillPaint.color = withLamp(if (g > 3f) skin.danger else skin.go)
        canvas.drawCircle(col.centerX(), by, br, fillPaint)
        strokePaint.strokeWidth = 1.6f
        strokePaint.color = Color.WHITE
        canvas.drawCircle(col.centerX(), by, br, strokePaint)
    }

    private fun drawReadouts(
        canvas: Canvas,
        tSec: Float,
        altKm: Float,
        speedKmh: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs,
        left: Float,
        top: Float,
        right: Float,
        bot: Float
    ) {
        val imperial = prefs.useImperial
        val altStr = if (imperial) String.format("%.1f mi", altKm * 0.621371f)
        else String.format("%.1f km", altKm)
        val spdVal = if (imperial) speedKmh * 0.621371f else speedKmh
        val spdStr = String.format("%,.0f %s", spdVal, if (imperial) "mph" else "km/h")
        val downKm = (tSec.coerceAtLeast(0f) * (speedKmh / 3600f) * 0.55f).coerceAtLeast(0f)
        val downStr = if (imperial) String.format("%.0f mi", downKm * 0.621371f)
        else String.format("%.0f km", downKm)
        val cols = arrayOf(
            Triple("ALT", altStr, skin.accent),
            Triple("SPD", spdStr, skin.hold),
            Triple("RANGE", downStr, skin.text)
        )
        val h = (bot - top).coerceAtLeast(sp(36f))
        val labH = h * 0.32f
        val valH = h * 0.58f
        val cellW = (right - left) / 3f
        cols.forEachIndexed { i, (lab, value, col) ->
            val x = left + cellW * (i + 0.5f)
            drawLabel(canvas, lab, x, top + labH, withLamp(skin.muted), cellW * 0.94f, labH, sp(14f))
            drawLabel(canvas, value, x, top + labH + valH * 0.85f, withLamp(col), cellW * 0.94f, valH, sp(22f))
        }
    }

    // ----------------------------------------------------------------- TRAJ
    private data class MapCam(
        val cLon: Float,
        val cLat: Float,
        val halfLon: Float,
        val halfLat: Float
    )

    private fun trajBezel(skin: TelemetrySkin.Tokens): Int {
        return if (skin.mapStyle == TelemetrySkin.MapStyle.NASA_PETAL || skin.label == "NASA") {
            Color.parseColor("#F2C14E")
        } else {
            skin.accent
        }
    }

    private fun wrapLon(lon: Float): Float {
        var x = lon
        while (x > 180f) x -= 360f
        while (x < -180f) x += 360f
        return x
    }

    private fun unwrapLon(lon: Float, ref: Float): Float {
        var d = lon - ref
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return ref + d
    }

    /** Clip a lon/lat ring to the camera so continent paths cannot flood the view. */
    private fun clipRingToCam(pts: FloatArray, cam: MapCam): ArrayList<Pair<Float, Float>> {
        val pad = 1.25f
        val west = cam.cLon - cam.halfLon * pad
        val east = cam.cLon + cam.halfLon * pad
        val south = cam.cLat - cam.halfLat * pad
        val north = cam.cLat + cam.halfLat * pad
        var ring = ArrayList<Pair<Float, Float>>(pts.size / 2 + 2)
        var i = 0
        while (i + 1 < pts.size) {
            ring.add(unwrapLon(pts[i], cam.cLon) to pts[i + 1])
            i += 2
        }
        if (ring.size < 3) return ring
        fun lerp(a: Pair<Float, Float>, b: Pair<Float, Float>, t: Float): Pair<Float, Float> {
            val u = t.coerceIn(0f, 1f)
            return (a.first + (b.first - a.first) * u) to (a.second + (b.second - a.second) * u)
        }
        fun clip(
            input: ArrayList<Pair<Float, Float>>,
            inside: (Pair<Float, Float>) -> Boolean,
            hit: (Pair<Float, Float>, Pair<Float, Float>) -> Pair<Float, Float>
        ): ArrayList<Pair<Float, Float>> {
            if (input.isEmpty()) return input
            val out = ArrayList<Pair<Float, Float>>(input.size + 4)
            var prev = input.last()
            for (cur in input) {
                val cin = inside(cur)
                val pin = inside(prev)
                if (cin) {
                    if (!pin) out.add(hit(prev, cur))
                    out.add(cur)
                } else if (pin) {
                    out.add(hit(prev, cur))
                }
                prev = cur
            }
            return out
        }
        ring = clip(ring, { it.first >= west }) { a, b ->
            val d = b.first - a.first
            lerp(a, b, if (abs(d) < 1e-6f) 0f else (west - a.first) / d)
        }
        ring = clip(ring, { it.first <= east }) { a, b ->
            val d = b.first - a.first
            lerp(a, b, if (abs(d) < 1e-6f) 0f else (east - a.first) / d)
        }
        ring = clip(ring, { it.second >= south }) { a, b ->
            val d = b.second - a.second
            lerp(a, b, if (abs(d) < 1e-6f) 0f else (south - a.second) / d)
        }
        ring = clip(ring, { it.second <= north }) { a, b ->
            val d = b.second - a.second
            lerp(a, b, if (abs(d) < 1e-6f) 0f else (north - a.second) / d)
        }
        return ring
    }

    private fun refreshPadCache(launch: LaunchSnapshot?) {
        val id = launch?.id
        if (id != null && id == padCacheId) return
        PadBook.ensure(context)
        val site = PadBook.find(launch)
        val ll = PadBook.lonLat(launch)
        padCacheLon = ll?.first ?: 0f
        padCacheLat = ll?.second ?: 0f
        padCacheWest = if (site != null) {
            !site.sea && !site.inland && site.waterAz in 170f..260f
        } else {
            val loc = "${launch?.pad.orEmpty()} ${launch?.location.orEmpty()}".lowercase()
            "vandenberg" in loc || "vafb" in loc
        }
        padCacheAz = when {
            site != null && site.sea -> if (site.waterAz < 360f) site.waterAz else 90f
            site != null && !site.inland && site.waterAz <= 360f -> site.waterAz
            padCacheWest -> 186f
            else -> {
                val loc = "${launch?.pad.orEmpty()} ${launch?.location.orEmpty()}".lowercase()
                when {
                    "baikonur" in loc -> 51f
                    "jiuquan" in loc || "taiyuan" in loc || "xichang" in loc -> 145f
                    "vostochny" in loc || "plesetsk" in loc -> 90f
                    else -> 90f
                }
            }
        }
        padCacheId = id
    }

    private fun padLonLat(launch: LaunchSnapshot?): Pair<Float, Float> {
        refreshPadCache(launch)
        return padCacheLon to padCacheLat
    }

    private fun destAzimuth(launch: LaunchSnapshot?, west: Boolean): Float {
        refreshPadCache(launch)
        return padCacheAz
    }

    private fun destFrom(lon: Float, lat: Float, bearingDeg: Float, distKm: Float): Pair<Float, Float> {
        val r = 6371.0
        val d = distKm.toDouble() / r
        val br = Math.toRadians(bearingDeg.toDouble())
        val lat1 = Math.toRadians(lat.toDouble())
        val lon1 = Math.toRadians(lon.toDouble())
        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(br))
        val lon2 = lon1 + atan2(sin(br) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))
        return wrapLon(Math.toDegrees(lon2).toFloat()) to Math.toDegrees(lat2).toFloat()
    }

    private fun isWestPad(launch: LaunchSnapshot?): Boolean {
        refreshPadCache(launch)
        return padCacheWest
    }

    /** EST gravity-turn downrange km. Not TM. Caps so a booster is not 1600 km inland. */
    private fun ascentDownKm(tSec: Float): Float {
        val t = tSec.coerceAtLeast(0f)
        return (0.00235f * t * t).coerceAtMost(110f)
    }

    private fun boosterSplashDest(
        launch: LaunchSnapshot?,
        padLon: Float,
        padLat: Float,
        az: Float,
        sepLon: Float,
        sepLat: Float
    ): Pair<Float, Float> {
        if (FlightProfiles.boosterEndsGulf(launch)) {
            // F13: Gulf hard-splash just off the coast. No public lat/lon. EST ~22 km along azimuth.
            return destFrom(padLon, padLat, az, 22f)
        }
        FlightProfiles.boosterDownrangeKm(launch)?.let { km ->
            return destFrom(padLon, padLat, az, km)
        }
        if (FlightProfiles.boosterReturnsToPad(launch)) return padLon to padLat
        return sepLon to sepLat
    }

    private fun vehicleLonLat(launch: LaunchSnapshot?, tSec: Float, stage: Int = 1): Pair<Float, Float> {
        val (padLon, padLat) = padLonLat(launch)
        val west = isWestPad(launch)
        val az = destAzimuth(launch, west)
        return FlightProfiles.vehicleLonLat(launch, tSec, stage, padLon, padLat, az, west)
    }

    private fun haversineKm(lon1: Float, lat1: Float, lon2: Float, lat2: Float): Float {
        val r = 6371.0
        val p1 = Math.toRadians(lat1.toDouble())
        val p2 = Math.toRadians(lat2.toDouble())
        val dphi = Math.toRadians((lat2 - lat1).toDouble())
        val dl = Math.toRadians((lon2 - lon1).toDouble())
        val a = sin(dphi / 2) * sin(dphi / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return (2.0 * r * atan2(sqrt(a), sqrt(1.0 - a))).toFloat()
    }

    private fun flightCamera(
        padLon: Float,
        padLat: Float,
        vehLon: Float,
        vehLat: Float,
        altKm: Float,
        tSec: Float
    ): MapCam {
        val orbital = tSec >= 520f && altKm >= 180f
        if (orbital) return MapCam(0f, 8f, 180f, 90f)
        val vehLonU = unwrapLon(vehLon, padLon)
        val sepKm = haversineKm(padLon, padLat, wrapLon(vehLonU), vehLat)
        val padFrameKm = when {
            sepKm < 50f -> 70f
            sepKm < 140f -> 150f
            else -> 420f
        }
        val altWiden = when {
            tSec < 0f -> 0f
            altKm < 20f -> 0f
            altKm < 80f -> altKm * 0.5f
            altKm < 160f -> 40f + (altKm - 80f) * 2.4f
            else -> 232f + (altKm - 160f) * 8f
        }
        val frameKm = max(padFrameKm, sepKm * 2.2f) + altWiden
        var halfLat = (frameKm / 2f / 111f).coerceIn(0.32f, 48f)
        val latScale = max(cos(Math.toRadians(padLat.toDouble())).toFloat(), 0.38f)
        var halfLon = (halfLat / latScale).coerceIn(0.40f, 70f)
        val cLon = padLon * 0.55f + vehLonU * 0.45f
        val cLat = (padLat * 0.62f + vehLat * 0.38f).coerceIn(-68f, 70f)
        halfLon = max(halfLon, abs(vehLonU - cLon) + 0.35f).coerceIn(0.40f, 80f)
        halfLat = max(halfLat, abs(vehLat - cLat) + 0.22f).coerceIn(0.32f, 55f)
        return MapCam(cLon, cLat, halfLon, halfLat)
    }

    private fun mapXY(lon: Float, lat: Float, dest: RectF, cam: MapCam): Pair<Float, Float> {
        val lonU = unwrapLon(lon, cam.cLon)
        val x = dest.left + ((lonU - (cam.cLon - cam.halfLon)) / (cam.halfLon * 2f)) * dest.width()
        val y = dest.top + (((cam.cLat + cam.halfLat) - lat) / (cam.halfLat * 2f)) * dest.height()
        return x to y
    }

    private fun drawTraj(
        canvas: Canvas,
        w: Float,
        h: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens
    ) {
        val left = dp(10f)
        val top = dp(10f)
        val right = w - dp(10f)
        val bot = h - dp(10f)
        val mw = (right - left).coerceAtLeast(8f)
        val tight = mw < dp(300f) || (bot - top) < dp(220f)
        val textH = if (tight) dp(26f) else dp(38f)
        val pillH = dp(26f)
        val headerH = textH + pillH + dp(4f)
        val footerH = dp(24f)
        val bodyTop = top + headerH
        val dest = RectF(left, bodyTop, right, bot - footerH)
        val gold = trajBezel(skin)
        val stgNow = prefs?.trackedStage ?: 1
        val (altKm, _, _) = approximateProfile(tSec, launch, stgNow)
        val (padLon, padLat) = padLonLat(launch)
        val (vehLon, vehLat) = vehicleLonLat(launch, tSec, stgNow)
        val cam = flightCamera(padLon, padLat, vehLon, vehLat, altKm, tSec)

        fillPaint.shader = null
        fillPaint.color = Color.parseColor("#06101C")
        canvas.drawRect(left, top, right, bot, fillPaint)

        fillPaint.color = Color.argb(230, 8, 12, 20)
        canvas.drawRect(left, top, right, bodyTop, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.2f
        strokePaint.color = Color.argb(80, 255, 255, 255)
        canvas.drawLine(left, bodyTop, right, bodyTop, strokePaint)

        val sep = if (launch != null) sepTime(launch) else 154f
        val stg = prefs?.trackedStage ?: 1
        val camLabel = when {
            failedSystem != null -> "CONTINGENCY"
            tSec < 0f -> "ON PAD"
            tSec < sep -> "ASCENT"
            stg <= 1 && VehicleCatalog.needsUpdate(launch) -> "UNKNOWN"
            stg <= 1 && FlightProfiles.boosterEndsGulf(launch) && FlightProfiles.hasLandTime(FlightProfiles.boosterLandTime(launch)) &&
                tSec >= FlightProfiles.boosterLandTime(launch) -> "GULF SPLASH"
            stg <= 1 && FlightProfiles.boosterEndsGulf(launch) -> "GULF CORRIDOR"
            stg <= 1 && VehicleCatalog.isKnownRecoverable(launch) && FlightProfiles.hasLandTime(FlightProfiles.boosterLandTime(launch)) &&
                tSec >= FlightProfiles.boosterLandTime(launch) -> "BOOSTER DOWN"
            stg <= 1 && VehicleCatalog.isKnownRecoverable(launch) && FlightProfiles.hasLandTime(FlightProfiles.boosterLandTime(launch)) -> "BOOSTER RETURN"
            stg <= 1 && VehicleCatalog.isKnownRecoverable(launch) -> "NO RECOVERY TIME"
            stg <= 1 && VehicleCatalog.isKnownExpendable(launch) -> "COAST EST"
            stg <= 1 -> "UNKNOWN"
            tSec < 520f -> "UPPER BURN"
            cam.halfLon >= 160f -> "ORBITAL"
            else -> "WIDE"
        }
        val doing = when {
            failedSystem != null -> "CONTINGENCY"
            tSec < 0f -> "ON THE PAD"
            tSec < sep -> "STAGE 1 ASCENT"
            stg <= 1 && VehicleCatalog.needsUpdate(launch) -> VehicleCatalog.UNKNOWN_RECOVERY
            stg <= 1 && FlightProfiles.boosterEndsGulf(launch) && FlightProfiles.hasLandTime(FlightProfiles.boosterLandTime(launch)) &&
                tSec >= FlightProfiles.boosterLandTime(launch) -> "STAGE 1 · GULF SPLASH"
            stg <= 1 && FlightProfiles.boosterEndsGulf(launch) -> "STAGE 1 · GULF EST"
            stg <= 1 && VehicleCatalog.isKnownRecoverable(launch) && FlightProfiles.hasLandTime(FlightProfiles.boosterLandTime(launch)) &&
                tSec >= FlightProfiles.boosterLandTime(launch) -> "STAGE 1 · DOWN"
            stg <= 1 && VehicleCatalog.isKnownRecoverable(launch) && FlightProfiles.hasLandTime(FlightProfiles.boosterLandTime(launch)) -> "STAGE 1 · RETURNING"
            stg <= 1 && VehicleCatalog.isKnownRecoverable(launch) -> "NO RECOVERY TIME IN BOOK"
            stg <= 1 && VehicleCatalog.isKnownExpendable(launch) -> "STAGE 1 · COAST EST"
            stg <= 1 -> VehicleCatalog.UNKNOWN_RECOVERY
            tSec < 520f -> "UPPER STAGE · BURN TO ORBIT"
            else -> "ORBIT / COAST"
        }
        val altLab = String.format("ALT %.0f KM", altKm)
        if (tight) {
            drawLabel(
                canvas, "TRACK EST  ·  $camLabel  ·  $altLab", left + mw * 0.50f, top + textH * 0.68f,
                gold, mw * 0.94f, textH * 0.78f, sp(12f)
            )
        } else {
            drawLabel(
                canvas, "GROUND TRACK EST", left + mw * 0.50f, top + textH * 0.38f,
                gold, mw * 0.90f, textH * 0.42f, sp(13f)
            )
            drawLabel(
                canvas, camLabel, left + dp(8f), top + textH * 0.86f,
                withLamp(skin.muted), mw * 0.42f, textH * 0.36f, sp(11f), Paint.Align.LEFT
            )
            drawLabel(
                canvas, altLab, right - dp(8f), top + textH * 0.86f,
                withLamp(skin.muted), mw * 0.42f, textH * 0.36f, sp(11f), Paint.Align.RIGHT
            )
        }
        drawStagePills(canvas, w, top + textH, pillH, skin, prefs?.trackedStage ?: 1)

        fillPaint.shader = null
        fillPaint.color = colWater
        canvas.drawRect(dest, fillPaint)
        requestTrajMap(dest, cam)
        val baked = trajMapBmp
        if (baked != null && !baked.isRecycled) {
            canvas.drawBitmap(baked, null, dest, null)
        }

        val (padX, padY) = mapXY(padLon, padLat, dest, cam)
        val ringBase = min(dest.width(), dest.height())
        val ringScale = (8f / cam.halfLon).coerceIn(0.35f, 2.4f)
        val padS = (ringBase * 0.012f * ringScale).coerceAtLeast(2.5f)
        fillPaint.shader = null
        fillPaint.color = gold
        canvas.drawRect(padX - padS, padY - padS, padX + padS, padY + padS, fillPaint)

        val past = trajPast
        val future = trajFuture
        past.reset()
        future.reset()
        val period = 5520f
        val landT = FlightProfiles.boosterLandTime(launch)
        val tEnd = when {
            stg <= 1 && FlightProfiles.hasLandTime(landT) -> max(tSec, landT)
            stg <= 1 -> max(tSec, if (launch != null) sepTime(launch) else tSec)
            tSec >= 480f -> tSec + period
            else -> max(tSec, 0f)
        }
        val samples = 96
        var lastLon = padLon
        var pastOpen = false
        var futureOpen = false
        for (i in 0..samples) {
            val u = i / samples.toFloat()
            val t = 0f + (tEnd - 0f) * u
            val (lon, lat) = vehicleLonLat(launch, t, stg)
            if (abs(lon - lastLon) > 180f) {
                pastOpen = false
                futureOpen = false
            }
            lastLon = lon
            val (x, y) = mapXY(lon, lat, dest, cam)
            if (t <= max(tSec, 0f)) {
                if (!pastOpen) {
                    past.moveTo(x, y)
                    pastOpen = true
                } else past.lineTo(x, y)
            } else {
                if (!futureOpen) {
                    future.moveTo(x, y)
                    futureOpen = true
                } else future.lineTo(x, y)
            }
        }
        strokePaint.strokeWidth = 2.2f
        strokePaint.color = Color.argb(90, 0, 212, 255)
        canvas.drawPath(future, strokePaint)
        strokePaint.strokeWidth = 3.4f
        strokePaint.color = colTrack
        canvas.drawPath(past, strokePaint)

        val (vx, vy) = mapXY(vehLon, vehLat, dest, cam)
        drawVesselTick(canvas, vx, vy, skin)

        // event marks baked later — never on the UI thread

        val padLab = padCallout(launch)
        drawMapCallout(canvas, padX + padS + dp(6f), padY - dp(6f), padLab, gold, dest)
        val shipName = (launch?.rocketName ?: "VEHICLE").uppercase().take(14)
        val stgLab = if (stg <= 1) "STG1" else "STG2"
        val kind = trackVehicleKind(launch, stg)
        drawMapCallout(
            canvas, vx + dp(10f), vy + dp(14f),
            "$kind · $stgLab · $shipName", withLamp(skin.go), dest
        )
        drawLabel(
            canvas, doing, left + dp(8f), bot - footerH * 0.38f,
            gold, mw * 0.94f, footerH * 0.85f, sp(12f), Paint.Align.LEFT
        )

        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 3.2f
        strokePaint.color = withLamp(gold)
        canvas.drawRect(left, top, right, bot, strokePaint)
    }

    private fun drawGeoGrid(canvas: Canvas, dest: RectF, cam: MapCam, skin: TelemetrySkin.Tokens) {
        val step = when {
            cam.halfLon < 12f -> 2f
            cam.halfLon < 30f -> 5f
            cam.halfLon < 80f -> 15f
            else -> 30f
        }
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.0f
        strokePaint.color = Color.parseColor("#2A5A70")
        var lon = ((cam.cLon - cam.halfLon) / step).toInt() * step - step
        while (lon <= cam.cLon + cam.halfLon + step) {
            val (x, _) = mapXY(lon, cam.cLat, dest, cam)
            canvas.drawLine(x, dest.top, x, dest.bottom, strokePaint)
            lon += step
        }
        var lat = ((cam.cLat - cam.halfLat) / step).toInt() * step - step
        while (lat <= cam.cLat + cam.halfLat + step) {
            val (_, y) = mapXY(cam.cLon, lat, dest, cam)
            canvas.drawLine(dest.left, y, dest.right, y, strokePaint)
            lat += step
        }
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = withLamp(skin.muted)
        textPaint.textSize = minSp()
        val north = (cam.cLat + cam.halfLat * 0.82f).coerceIn(-90f, 90f)
        val south = (cam.cLat - cam.halfLat * 0.82f).coerceIn(-90f, 90f)
        canvas.drawText(latLabel(north), dest.left + dp(6f), dest.top + dp(14f), textPaint)
        canvas.drawText(latLabel(cam.cLat), dest.left + dp(6f), dest.centerY() + sp(4f), textPaint)
        canvas.drawText(latLabel(south), dest.left + dp(6f), dest.bottom - dp(8f), textPaint)
    }

    private fun latLabel(lat: Float): String {
        val a = abs(lat)
        return when {
            a < 0.8f -> "EQ"
            lat >= 0f -> String.format("%.0fN", a)
            else -> String.format("%.0fS", a)
        }
    }


    private fun trajMapKeyOf(cam: MapCam, dest: RectF): Long {
        val w = dest.width().toInt()
        val h = dest.height().toInt()
        val a = (cam.cLon * 40f).toInt()
        val b = (cam.cLat * 40f).toInt()
        val c = (cam.halfLon * 20f).toInt()
        return (a.toLong() shl 32) xor ((b.toLong() and 0xffff) shl 16) xor
            (c.toLong() and 0xffff) xor (w.toLong() shl 8) xor h.toLong()
    }

    private fun requestTrajMap(dest: RectF, cam: MapCam) {
        GeoAtlas.ensure(context) { postInvalidateOnAnimation() }
        if (!GeoAtlas.ready) return
        val key = trajMapKeyOf(cam, dest)
        if (key == trajMapKey || trajMapBusy) return
        val w = dest.width().toInt().coerceIn(8, 1600)
        val h = dest.height().toInt().coerceIn(8, 1600)
        val camCopy = cam
        trajMapBusy = true
        Thread({
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val local = RectF(0f, 0f, w.toFloat(), h.toFloat())
            c.drawColor(Color.parseColor("#0A3A58"))
            GeoDraw.drawLand(
                c, local, camCopy.cLon, camCopy.cLat, camCopy.halfLon, camCopy.halfLat,
                Color.parseColor("#1C4A32"),
                Color.parseColor("#8FD4A8"),
                Color.parseColor("#0C4A6E")
            )
            trajHandler.post {
                val old = trajMapBmp
                trajMapBmp = bmp
                trajMapKey = key
                trajMapBusy = false
                old?.recycle()
                invalidate()
            }
        }, "ccos-traj-map").start()
    }

    private fun drawLandmasses(canvas: Canvas, dest: RectF, cam: MapCam) {
        return
        GeoAtlas.ensure(context) { postInvalidateOnAnimation() }
        if (!GeoAtlas.ready) return
        val land = Color.parseColor("#1C4A32")
        val edge = Color.parseColor("#8FD4A8")
        val water = Color.parseColor("#0C4A6E")
        GeoDraw.drawLand(canvas, dest, cam.cLon, cam.cLat, cam.halfLon, cam.halfLat, land, edge, water)
        if (cam.halfLon >= 8f) return
        val west = cam.cLon - cam.halfLon * 1.15f
        val east = cam.cLon + cam.halfLon * 1.15f
        val south = cam.cLat - cam.halfLat * 1.15f
        val north = cam.cLat + cam.halfLat * 1.15f
        val maxRank = when {
            cam.halfLon > 3.5f -> 4
            cam.halfLon > 1.4f -> 6
            else -> 8
        }
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND
        GeoAtlas.forRoads(west, south, east, north, maxRank) { road ->
            tmpPath.reset()
            var started = false
            var i = 0
            val pts = road.pts
            while (i + 1 < pts.size) {
                val lonU = unwrapLon(pts[i], cam.cLon)
                val lat = pts[i + 1]
                if (lonU < west || lonU > east || lat < south || lat > north) {
                    started = false
                    i += 2
                    continue
                }
                val (x, y) = mapXY(pts[i], lat, dest, cam)
                if (!started) {
                    tmpPath.moveTo(x, y)
                    started = true
                } else {
                    tmpPath.lineTo(x, y)
                }
                i += 2
            }
            strokePaint.strokeWidth = when {
                road.rank <= 3 -> 2.0f
                road.rank <= 5 -> 1.4f
                else -> 1.1f
            }
            strokePaint.color = Color.argb(if (road.rank <= 4) 170 else 120, 214, 196, 150)
            canvas.drawPath(tmpPath, strokePaint)
        }
    }

    private fun drawMapCulture(canvas: Canvas, dest: RectF, cam: MapCam) {
        if (!GeoAtlas.ready) return
        val maxRank = when {
            cam.halfLon < 0.70f -> 12
            cam.halfLon < 1.20f -> 10
            cam.halfLon < 2.20f -> 8
            cam.halfLon < 4.50f -> 6
            cam.halfLon < 8.00f -> 4
            cam.halfLon < 16f -> 2
            else -> 1
        }
        val west = cam.cLon - cam.halfLon
        val east = cam.cLon + cam.halfLon
        val south = cam.cLat - cam.halfLat
        val north = cam.cLat + cam.halfLat
        val taken = ArrayList<Pair<Float, Float>>()
        val minGap = dp(26f)
        for (p in GeoAtlas.places) {
            val show = when {
                p.landmark && cam.halfLon < 8f -> true
                p.rank <= maxRank -> true
                else -> false
            }
            if (!show) continue
            val lonU = unwrapLon(p.lon, cam.cLon)
            if (lonU < west || lonU > east || p.lat < south || p.lat > north) continue
            val (x, y) = mapXY(p.lon, p.lat, dest, cam)
            if (x < dest.left + dp(6f) || x > dest.right - dp(6f) || y < dest.top + dp(10f) || y > dest.bottom - dp(8f)) continue
            var clash = false
            for ((tx, ty) in taken) {
                val dx = x - tx
                val dy = y - ty
                if (dx * dx + dy * dy < minGap * minGap) {
                    clash = true
                    break
                }
            }
            if (clash) continue
            taken.add(x to y)
            fillPaint.shader = null
            fillPaint.color = if (p.landmark) Color.argb(230, 242, 193, 78) else Color.argb(210, 232, 222, 196)
            canvas.drawCircle(x, y, if (p.landmark) dp(2.6f) else dp(2.0f), fillPaint)
            val lab = p.name.uppercase()
            drawLabel(
                canvas, lab, x + dp(5f), y - dp(3f),
                if (p.landmark) Color.argb(230, 242, 193, 78) else Color.argb(210, 236, 228, 204),
                dest.width() * 0.28f, sp(11f), sp(9f), Paint.Align.LEFT
            )
        }
    }

    private fun drawEventTape(
        canvas: Canvas,
        tSec: Float,
        altKm: Float,
        speedKmh: Float,
        phase: String,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs,
        left: Float,
        top: Float,
        right: Float,
        bot: Float
    ) {
        val imperial = prefs.useImperial
        val altStr = if (imperial) String.format("%.1f mi", altKm * 0.621371f) else String.format("%.1f km", altKm)
        val spdVal = if (imperial) speedKmh * 0.621371f else speedKmh
        val spdStr = String.format("%,.0f %s", spdVal, if (imperial) "mph" else "km/h")
        val band = right - left
        val y0 = top + sp(13f)
        drawLabel(
            canvas, altStr, left + dp(4f), y0,
            withLamp(skin.text), band * 0.30f, sp(16f), sp(13f), Paint.Align.LEFT
        )
        drawLabel(
            canvas, spdStr, (left + right) * 0.5f, y0,
            withLamp(skin.text), band * 0.34f, sp(16f), sp(13f)
        )
        drawLabel(
            canvas, phase.take(12), right - dp(4f), y0,
            withLamp(skin.muted), band * 0.30f, sp(16f), sp(13f), Paint.Align.RIGHT
        )
        val fail = failedSystem
        var row0 = top + sp(30f)
        if (fail != null) {
            drawLabel(
                canvas, "FAIL  $fail", (left + right) * 0.5f, top + sp(34f),
                withLamp(skin.danger), band * 0.94f, sp(16f), sp(14f)
            )
            row0 = top + sp(52f)
        }
        val tape = eventTape.takeLast(5)
        if (tape.isEmpty()) {
            drawLabel(
                canvas, "NO FLIGHT EVENTS YET", (left + right) * 0.5f, (top + bot) * 0.62f,
                withLamp(skin.muted), (right - left) * 0.9f, sp(14f), sp(12f)
            )
            return
        }
        val rowH = ((bot - row0) / 5f).coerceIn(sp(16f), sp(22f))
        tape.forEachIndexed { i, e ->
            val y = row0 + rowH * (i + 0.78f)
            val clock = formatClock(e.tSec)
            val col = when {
                e.severity.name == "FAIL" -> skin.danger
                e.severity.name == "WATCH" -> skin.hold
                else -> skin.accent
            }
            drawLabel(
                canvas, clock, left + dp(4f), y,
                withLamp(skin.muted), (right - left) * 0.22f, rowH * 0.9f, sp(12f), Paint.Align.LEFT
            )
            drawLabel(
                canvas, e.title.take(28), left + (right - left) * 0.26f, y,
                withLamp(col), (right - left) * 0.72f, rowH * 0.9f, sp(13f), Paint.Align.LEFT
            )
        }
    }

    private fun drawMapCallout(canvas: Canvas, x: Float, y: Float, text: String, col: Int, dest: RectF) {
        val lx = x.coerceIn(dest.left + dp(4f), dest.right - dp(8f))
        val ly = y.coerceIn(dest.top + dp(12f), dest.bottom - dp(6f))
        textPaint.textSize = sp(10f)
        val tw = (textPaint.measureText(text) + dp(10f)).coerceAtMost(dest.width() * 0.46f)
        val th = sp(13f)
        tmpRect.set(lx - dp(4f), ly - th + dp(3f), lx + tw, ly + dp(4f))
        fillPaint.shader = null
        fillPaint.color = Color.argb(190, 6, 10, 16)
        canvas.drawRoundRect(tmpRect, 4f, 4f, fillPaint)
        drawLabel(canvas, text, lx, ly, col, tw, th, sp(10f), Paint.Align.LEFT)
    }

    private fun padSiteKnown(launch: LaunchSnapshot?): Boolean {
        refreshPadCache(launch)
        return PadBook.lonLat(launch) != null
    }

    private fun padCallout(launch: LaunchSnapshot?): String {
        refreshPadCache(launch)
        val site = PadBook.find(launch)
        if (site != null) {
            val loc = site.location.substringBefore(',').uppercase().trim()
            val nm = site.name.uppercase().trim()
            val head = when {
                loc.isNotBlank() && "UNKNOWN" !in loc -> loc.take(16)
                nm.isNotBlank() && "UNKNOWN" !in nm -> nm.take(16)
                else -> ""
            }
            return if (head.isBlank()) "PAD EST" else "PAD · $head"
        }
        val raw = launch?.pad?.uppercase()?.trim().orEmpty()
        if (raw.isBlank() || raw == "PAD" || "UNKNOWN" in raw || raw.startsWith("{")) return "PAD EST"
        return "PAD EST · ${raw.take(16)}"
    }

    private fun trackVehicleKind(launch: LaunchSnapshot?, stg: Int): String {
        if (stg <= 1) return "BOOSTER"
        val fam = VehicleCatalog.family(launch).lowercase()
        return if ("starship" in fam || "super heavy" in fam || "superheavy" in fam) "SHIP" else "UPPER"
    }

    private fun drawTrajEvents(
        canvas: Canvas,
        dest: RectF,
        cam: MapCam,
        launch: LaunchSnapshot?,
        tSec: Float,
        gold: Int,
        stage: Int
    ) {
        data class Mk(val e: com.ccos.retro.event.FlightEvent, val x: Float, val y: Float)
        val marks = eventTape.filter { it.tSec >= 0f && it.severity.name != "FAIL" }
            .filter { e ->
                if (stage <= 1) FlightProfiles.eventIsBooster(e.title) else FlightProfiles.eventIsShip(e.title)
            }
            .takeLast(8)
        val pts = ArrayList<Mk>()
        for (e in marks) {
            val (lon, lat) = vehicleLonLat(launch, e.tSec, stage)
            val (x, y) = mapXY(lon, lat, dest, cam)
            if (x < dest.left - dp(4f) || x > dest.right + dp(4f) || y < dest.top - dp(4f) || y > dest.bottom + dp(4f)) continue
            fillPaint.shader = null
            fillPaint.color = gold
            canvas.drawCircle(x, y, dp(3.2f), fillPaint)
            pts += Mk(e, x, y)
        }
        val used = BooleanArray(pts.size)
        val clusterR = dp(40f)
        for (i in pts.indices) {
            if (used[i]) continue
            val group = ArrayList<Int>()
            group += i
            used[i] = true
            for (j in i + 1 until pts.size) {
                if (used[j]) continue
                val dx = pts[j].x - pts[i].x
                val dy = pts[j].y - pts[i].y
                if (dx * dx + dy * dy <= clusterR * clusterR) {
                    group += j
                    used[j] = true
                }
            }
            val titles = group.map { pts[it].e.title.replace("BOOSTER ", "").take(16) }
            val live = group.any { pts[it].e.tSec <= tSec + 2f }
            val cx = group.map { pts[it].x }.average().toFloat()
            val cy = group.map { pts[it].y }.average().toFloat()
            val col = if (live) gold else Color.argb(200, 200, 210, 180)
            val right = cx < dest.centerX()
            var ly = cy - (titles.size - 1) * sp(11f) * 0.5f
            val lx = if (right) cx + dp(10f) else cx - dp(10f)
            for (title in titles) {
                if (title.contains("LIFTOFF")) continue
                drawMapCallout(canvas, lx, ly, title, col, dest)
                ly += sp(12f)
            }
        }
    }

    private fun drawVesselTick(canvas: Canvas, x: Float, y: Float, skin: TelemetrySkin.Tokens) {
        tmpPath.reset()
        tmpPath.moveTo(x, y - 14f)
        tmpPath.lineTo(x - 9f, y + 11f)
        tmpPath.lineTo(x, y + 5f)
        tmpPath.lineTo(x + 9f, y + 11f)
        tmpPath.close()
        fillPaint.shader = null
        fillPaint.color = withLamp(skin.go)
        canvas.drawPath(tmpPath, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.4f
        strokePaint.color = Color.WHITE
        canvas.drawPath(tmpPath, strokePaint)
        fillPaint.color = Color.WHITE
        canvas.drawCircle(x, y, 3.2f, fillPaint)
    }

    // ---------------------------------------------------------- STG1 / STG2
    private fun drawStage(
        canvas: Canvas,
        w: Float,
        h: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs,
        stage: Int
    ) {
        val title = if (stage == 1) "STAGE 1" else "STAGE 2"
        drawLabel(
            canvas, title, w * 0.5f, dp(26f),
            withLamp(skin.accent), w * 0.9f, sp(20f), sp(18f)
        )
        if (launch == null) {
            drawLabel(canvas, "NO TRACKED LAUNCH", w * 0.5f, h * 0.5f, withLamp(skin.muted), w * 0.8f, sp(18f), sp(16f))
            return
        }
        val (altKm, speedKmh, _) = approximateProfile(tSec, launch, stage)
        val imperial = prefs.useImperial
        val altStr = if (imperial) String.format("%.1f mi", altKm * 0.621371f) else String.format("%.1f km", altKm)
        val spdVal = if (imperial) speedKmh * 0.621371f else speedKmh
        val spdStr = String.format("%,.0f %s", spdVal, if (imperial) "mph" else "km/h")
        val engines = engineCountForStage(launch, stage)
        val sep = sepTime(launch)
        val separated = tSec >= sep
        val lit = FlightProfiles.enginesLit(tSec, launch, stage, engines)
        val stripTop = dp(36f)
        val stripH = sp(36f)
        val colW = w / 3f
        fun metric(label: String, value: String, x: Float, col: Int) {
            drawLabel(canvas, label, x, stripTop + sp(12f), withLamp(skin.muted), colW * 0.92f, sp(13f), sp(11f))
            drawLabel(canvas, value, x, stripTop + stripH - dp(2f), withLamp(col), colW * 0.92f, sp(16f), sp(14f))
        }
        metric("ALT EST", altStr, colW * 0.5f, skin.text)
        metric("SPD EST", spdStr, w * 0.5f, skin.text)
        metric("ENG", "$lit/$engines", w - colW * 0.5f, skin.go)

        val rocketTop = stripTop + stripH + dp(10f)
        val footerY = h - dp(22f)
        val rocketH = (footerY - rocketTop - dp(24f)).coerceAtLeast(h * 0.38f)
        val baseY = footerY - dp(22f)
        val cx = w * 0.5f
        if (stage == 1) {
            drawVehicle(canvas, cx, baseY, rocketH, launch, tSec, 1, separated, skin, lamp(), 1f)
            val recovers = VehicleCatalog.isKnownRecoverable(launch)
            val land = FlightProfiles.boosterLandTime(launch)
            val hasLand = FlightProfiles.hasLandTime(land)
            val outcome = MissionFacts.boosterOutcome(launch)
            val lab = when {
                !separated -> "FULL STACK"
                VehicleCatalog.needsUpdate(launch) -> VehicleCatalog.UNKNOWN_RECOVERY
                tSec >= land && hasLand && !outcome.isNullOrBlank() -> outcome
                recovers && hasLand && tSec < land -> outcome ?: "BOOSTER · RETURNING"
                recovers && hasLand -> outcome ?: "BOOSTER · DOWN"
                recovers -> outcome ?: "NO RECOVERY TIME IN BOOK"
                VehicleCatalog.isKnownExpendable(launch) -> "EXPENDED · NO RECOVERY"
                else -> VehicleCatalog.UNKNOWN_RECOVERY
            }
            drawLabel(canvas, lab, cx, footerY, withLamp(if (!separated) skin.text else if (recovers) skin.hold else skin.muted), w * 0.92f, sp(16f), sp(13f))
        } else {
            if (!separated) {
                drawVehicle(canvas, cx, baseY, rocketH, launch, tSec, 1, false, skin, lamp(), 0.32f)
                drawLabel(
                    canvas, "AWAITING SEP", cx, rocketTop + sp(18f),
                    withLamp(skin.hold), w * 0.9f, sp(18f), sp(16f)
                )
            } else {
                val entry = FlightProfiles.events(launch).firstOrNull { "REENTRY" in it.second.uppercase() }?.first
                val flip = FlightProfiles.events(launch).firstOrNull { "FLIP" in it.second.uppercase() }?.first
                if (entry != null && flip != null && tSec >= entry && tSec < flip && vehicleFamily(launch) == "starship") {
                    drawReentryCard(canvas, w, rocketTop, footerY - dp(8f), launch, tSec, skin)
                } else {
                    drawVehicle(canvas, cx, baseY, rocketH * 0.92f, launch, tSec, 2, true, skin, lamp(), 1f)
                }
            }
            val entryT = FlightProfiles.events(launch).firstOrNull { "REENTRY" in it.second.uppercase() }?.first
            val landT = FlightProfiles.shipLandTime(launch)
            val flipT = FlightProfiles.events(launch).firstOrNull { "FLIP" in it.second.uppercase() }?.first
            val lab = when {
                !separated -> "STACK GHOST"
                VehicleCatalog.needsUpdate(launch) -> VehicleCatalog.UNKNOWN_RECOVERY
                FlightProfiles.hasLandTime(landT) && tSec >= landT -> "SHIP  ·  SPLASH"
                flipT != null && tSec >= flipT -> "SHIP  ·  LANDING BURN"
                entryT != null && tSec >= entryT -> "SHIP  ·  REENTRY"
                else -> "UPPER STAGE"
            }
            drawLabel(canvas, lab, cx, footerY, withLamp(if ("REENTRY" in lab || "BURN" in lab) skin.hold else skin.text), w * 0.8f, sp(14f), sp(12f))
        }
    }

    // ------------------------------------------------------------------- ENG
    private fun engineTitle(launch: LaunchSnapshot?, stage: Int): String {
        val spec = VehicleCatalog.spec(launch)
        if (!spec.verified) return "ENGINES · STG $stage"
        return spec.engineName
    }

    private fun drawEng(
        canvas: Canvas,
        w: Float,
        h: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs
    ) {
        val stage = trackedOr(prefs)
        drawLabel(
            canvas, engineTitle(launch, stage), w * 0.5f, dp(28f),
            withLamp(skin.accent), w * 0.9f, sp(20f), sp(18f)
        )
        drawStagePills(canvas, w, dp(48f), dp(28f), skin, stage)
        if (launch == null) {
            drawLabel(canvas, "NO TRACKED LAUNCH", w * 0.5f, h * 0.5f, withLamp(skin.muted), w * 0.8f, sp(18f), sp(16f))
            return
        }
        val total = engineCountForStage(launch, stage)
        val lit = FlightProfiles.enginesLit(tSec, launch, stage, total)
        val size = min(w, h) * 0.62f
        drawEngineHardware(canvas, w * 0.5f, h * 0.50f, size, launch, stage, lit, skin)
        if (VehicleCatalog.needsUpdate(launch)) {
            drawBookGap(canvas, w * 0.5f, h * 0.50f, size, skin, lamp())
        } else {
            drawLabel(
                canvas, "$lit / $total LIT", w * 0.5f, h * 0.50f + size * 0.52f + sp(26f),
                withLamp(skin.text), w * 0.9f, sp(22f), sp(20f)
            )
        }
        drawLabel(
            canvas, launch.rocketName.take(28), w * 0.5f, h - dp(18f),
            withLamp(skin.muted), w * 0.9f, sp(16f), sp(14f)
        )
    }

    private fun drawEngineHardware(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        launch: LaunchSnapshot?,
        stage: Int,
        lit: Int,
        skin: TelemetrySkin.Tokens
    ) {
        EngineDraw.drawEnginePattern(canvas, cx, cy, size, launch, stage, lit, skin)
    }

    // ------------------------------------------------------------------ PROP
    private fun drawProp(
        canvas: Canvas,
        w: Float,
        h: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens
    ) {
        val stage = prefs?.trackedStage ?: 1
        val recover = VehicleCatalog.isKnownRecoverable(launch)
        val sep = if (launch != null) sepTime(launch) else 154f
        val fuelEarly = fuelRemain(tSec, stage, launch)
        val expendedEarly = FlightProfiles.isStageSpent(tSec, launch, stage) || fuelEarly <= 0f
        drawLabel(
            canvas, if (expendedEarly) "FLIGHT CARD" else "PROPELLANT", w * 0.5f, dp(22f),
            withLamp(skin.accent), w * 0.9f, sp(18f), sp(16f)
        )
        val pillY = dp(40f)
        val pillH = dp(28f)
        drawStagePills(canvas, w, pillY, pillH, skin, stage)
        if (!expendedEarly) {
            val sub = when {
                stage == 1 && recover && tSec >= sep -> "STAGE 1 · RETURNING"
                stage == 1 -> "STAGE 1 · LOX / ${fuelName(launch)}"
                else -> "STAGE 2 · LOX / ${fuelName(launch)}"
            }
            drawLabel(
                canvas, sub, w * 0.5f, pillY + pillH + sp(16f),
                withLamp(skin.muted), w * 0.9f, sp(14f), sp(12f)
            )
        }
        val fuel = fuelEarly
        val expended = expendedEarly
        val bodyTop = if (expended) pillY + pillH + dp(6f) else pillY + pillH + sp(28f)
        if (expended) {
            drawFlightCard(canvas, w, h, bodyTop, launch, tSec, stage, skin, fuel)
        } else {
            val can = RectF(w * 0.10f, bodyTop, w * 0.90f, h * 0.92f)
            drawFuelTanks(canvas, can, fuel, fuel, skin, launch, compact = false)
        }
    }

    /**
     * After burnout nobody cares about 8% leftovers.
     * PROP becomes a nerd flight card: thrust, Isp, mix, chamber P, peak Q, MECO.
     */
    private fun drawFlightCard(
        canvas: Canvas,
        w: Float,
        h: Float,
        top: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        stage: Int,
        skin: TelemetrySkin.Tokens,
        fuel: Float
    ) {
        val spec = VehicleCatalog.spec(launch)
        val left = dp(10f)
        val right = w - dp(10f)
        val bot = h - dp(8f)
        fillPaint.shader = null
        fillPaint.color = Color.argb(236, 8, 12, 18)
        canvas.drawRoundRect(left, top, right, bot, 10f, 10f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2.2f
        strokePaint.color = withLamp(skin.accent)
        canvas.drawRoundRect(left, top, right, bot, 10f, 10f, strokePaint)

        val cx = w * 0.5f
        val maxW = right - left - dp(16f)
        val floor = bot - dp(10f)
        var lineH = ((floor - (top + dp(14f))) / 13f).coerceIn(sp(16f), sp(26f))
        var y = top + dp(16f)
        fun can(n: Int = 1) = y + lineH * n <= floor + 0.5f
        fun head(text: String, col: Int, wish: Float) {
            if (!can()) return
            drawLabel(canvas, text, cx, y, withLamp(col), maxW, lineH * 0.92f, wish)
            y += lineH
        }
        fun row(lab: String, value: String, col: Int = skin.text) {
            if (!can()) return
            drawLabel(canvas, lab, left + dp(12f), y, withLamp(skin.muted), maxW * 0.30f, lineH * 0.9f, sp(10f), Paint.Align.LEFT)
            drawLabel(canvas, value, left + dp(12f) + maxW * 0.32f, y, withLamp(col), maxW * 0.64f, lineH * 0.9f, sp(11f), Paint.Align.LEFT)
            y += lineH
        }

        val (mecoAlt, mecoSpd, _) = approximateProfile(
            if (stage == 1) FlightProfiles.sepTime(launch) - 1f else FlightProfiles.secoTime(launch) - 1f,
            launch, stage
        )
        val peakG = FlightProfiles.accelG(
            if (stage == 1) FlightProfiles.sepTime(launch) - 1f else FlightProfiles.secoTime(launch) - 1f,
            launch
        )
        val imperial = prefs?.useImperial == true
        val altStr = if (imperial) String.format("%.0f mi", mecoAlt * 0.621371f) else String.format("%.0f km", mecoAlt)
        val spdStr = if (imperial) String.format("%,.0f mph", mecoSpd * 0.621371f) else String.format("%,.0f km/h", mecoSpd)

        val veh = spec.stageName(stage, launch?.rocketName ?: spec.id).uppercase().take(22)
        head(if (stage == 1) "STAGE 1  ·  DRY" else "STAGE 2  ·  DRY", skin.hold, sp(12f))
        row("VEHICLE", veh, skin.text)
        row("ENGINE", spec.engineLabel(stage), skin.go)
        row("COUNT", "${if (stage == 1) spec.s1Engines else spec.s2Engines}  LIT @ PEAK", skin.go)
        row("THRUST", if (stage == 1) spec.s1Thrust else spec.s2Thrust, skin.text)
        row("ISP", spec.isp(stage), skin.text)
        row("MIX", spec.mixRatio, skin.text)
        row("PC", spec.chamberBar, skin.text)
        row("DRY / PROP", "${spec.dry(stage)}  /  ${spec.prop(stage)}", skin.muted)
        if (stage == 1) row("MAX-Q", "~40 kPa EST  ·  T+60", skin.hold)
        row("CUTOFF", "EST  $altStr   $spdStr", skin.text)
        row("PEAK G", String.format("EST  %.1f g", peakG), skin.text)
        row("RESIDUAL PROP", String.format("%.0f%%", fuel * 100f), skin.muted)
    }

    /** EST convective envelope. Heat builds after EI as density rises, peaks mid-entry, then falls. */
    private fun entryHeatFrac(u: Float): Float {
        val x = u.coerceIn(0f, 1f)
        val rise = (x / 0.38f).coerceIn(0f, 1f)
        val smooth = rise * rise * (3f - 2f * rise)
        val fall = ((x - 0.38f) / 0.62f).coerceIn(0f, 1f)
        return (smooth * (1f - 0.70f * fall)).coerceIn(0f, 1f)
    }

    /** Blackbody of a silica tile. Not plasma. Charcoal → cherry → orange → white. */
    private fun tileBlackbody(tempC: Float): Int {
        val t = tempC.coerceIn(280f, 1750f)
        val r: Int
        val g: Int
        val b: Int
        when {
            t < 520f -> {
                val k = (t - 280f) / 240f
                r = (28 + 40 * k).toInt()
                g = (20 + 8 * k).toInt()
                b = (16 + 2 * k).toInt()
            }
            t < 780f -> {
                val k = (t - 520f) / 260f
                r = (68 + 140 * k).toInt()
                g = (28 + 18 * k).toInt()
                b = (18 + 4 * k).toInt()
            }
            t < 1100f -> {
                val k = (t - 780f) / 320f
                r = (208 + 47 * k).toInt()
                g = (46 + 90 * k).toInt()
                b = (22 + 10 * k).toInt()
            }
            t < 1400f -> {
                val k = (t - 1100f) / 300f
                r = 255
                g = (136 + 90 * k).toInt()
                b = (32 + 90 * k).toInt()
            }
            else -> {
                val k = ((t - 1400f) / 350f).coerceIn(0f, 1f)
                r = 255
                g = (226 + 24 * k).toInt()
                b = (122 + 110 * k).toInt()
            }
        }
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun drawReentryCard(
        canvas: Canvas,
        w: Float,
        top: Float,
        bot: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens
    ) {
        val land = FlightProfiles.shipLandTime(launch)
        val entry = FlightProfiles.events(launch).firstOrNull { "REENTRY" in it.second.uppercase() }?.first
            ?: (land - 640f)
        val flip = FlightProfiles.events(launch).firstOrNull { "FLIP" in it.second.uppercase() }?.first
            ?: (land - 40f)
        val u = ((tSec - entry) / (flip - entry).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val heat = entryHeatFrac(u)
        val tps = 380f + 1220f * heat
        val noseT = tps * 1.12f
        val flapT = tps * 0.70f
        val now = SystemClock.uptimeMillis() * 0.001f
        val cx = w * 0.5f
        val midY = (top + bot) * 0.50f
        val sW = w * 0.26f
        val sH = (bot - top) * 0.11f
        val noseX = cx - sW * 1.05f
        val aftX = cx + sW * 0.95f
        val backY = midY - sH * 0.55f
        val bellyY = midY + sH * 0.62f
        val flick = 0.82f + 0.18f * sin((now * 17.3f).toDouble()).toFloat()

        // Ionized wake. Miles of trail. White-magenta core, blue-violet fringe, orange fade.
        val wakeLen = w * (0.18f + 0.46f * heat)
        if (heat > 0.04f) {
            val a0 = (210 * heat * flick).toInt().coerceIn(0, 230)
            fillPaint.shader = LinearGradient(
                aftX, midY, aftX + wakeLen, midY,
                intArrayOf(
                    Color.argb(a0, 255, 230, 255),
                    Color.argb((a0 * 0.70f).toInt(), 255, 70, 200),
                    Color.argb((a0 * 0.45f).toInt(), 90, 70, 255),
                    Color.argb((a0 * 0.18f).toInt(), 255, 110, 40),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.18f, 0.40f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(aftX - sW * 0.08f, midY - sH * 1.35f, aftX + wakeLen, midY + sH * 1.55f, fillPaint)
            fillPaint.shader = null
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeCap = Paint.Cap.ROUND
            for (i in 0 until 9) {
                val wob = sin((now * (4.2f + i * 0.37f) + i * 1.7f).toDouble()).toFloat()
                val y = midY + (i - 4f) * sH * 0.28f + wob * sH * 0.18f
                val len = wakeLen * (0.55f + 0.45f * (1f - abs(i - 4f) / 5f)) *
                    (0.78f + 0.22f * sin((now * 6.1f + i).toDouble()).toFloat())
                val a = (150 * heat * flick * (1f - i * 0.06f)).toInt().coerceIn(0, 200)
                strokePaint.strokeWidth = sH * (0.10f + 0.08f * heat) * (1f - i * 0.05f)
                strokePaint.color = if (i % 3 == 0)
                    Color.argb(a, 120, 90, 255)
                else if (i % 3 == 1)
                    Color.argb(a, 255, 80, 210)
                else
                    Color.argb(a, 255, 200, 240)
                canvas.drawLine(aftX, y, aftX + len, y + wob * sH * 0.35f, strokePaint)
            }
        }

        // Windward plasma sheath. Magenta-white, not a campfire.
        if (heat > 0.03f) {
            val a = (40 + 150 * heat * flick).toInt().coerceIn(0, 210)
            fillPaint.shader = RadialGradient(
                cx, bellyY + sH * 0.15f, w * 0.42f,
                intArrayOf(
                    Color.argb(a, 255, 240, 255),
                    Color.argb((a * 0.75f).toInt(), 255, 60, 170),
                    Color.argb((a * 0.35f).toInt(), 80, 40, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.35f, 0.70f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(noseX - sW * 0.15f, bellyY - sH * 0.25f, aftX + sW * 0.20f, bellyY + sH * 2.1f, fillPaint)
            fillPaint.shader = null
        }

        // Bow shock ahead of the nose. Thin blue-white cap.
        if (heat > 0.12f) {
            val a = (90 * heat * flick).toInt().coerceIn(0, 160)
            fillPaint.shader = RadialGradient(
                noseX - sW * 0.08f, midY, sW * 0.55f,
                intArrayOf(Color.argb(a, 200, 230, 255), Color.argb((a * 0.4f).toInt(), 80, 120, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(noseX - sW * 0.55f, midY - sH * 1.1f, noseX + sW * 0.10f, midY + sH * 1.2f, fillPaint)
            fillPaint.shader = null
        }

        // Hull: nose left, aft right, belly down
        tmpPath.reset()
        tmpPath.moveTo(noseX, midY)
        tmpPath.quadTo(noseX + sW * 0.22f, backY, cx - sW * 0.15f, backY)
        tmpPath.lineTo(aftX - sW * 0.16f, backY + sH * 0.10f)
        tmpPath.lineTo(aftX, midY + sH * 0.05f)
        tmpPath.lineTo(aftX - sW * 0.10f, bellyY)
        tmpPath.lineTo(cx - sW * 0.10f, bellyY)
        tmpPath.quadTo(noseX + sW * 0.28f, bellyY - sH * 0.05f, noseX, midY)
        tmpPath.close()
        fillPaint.shader = null
        fillPaint.color = Color.parseColor("#C6CACF")
        canvas.drawPath(tmpPath, fillPaint)

        // Windward tiles. Local temp: nose hotter, aft cooler, tile-to-tile scatter.
        canvas.save()
        canvas.clipPath(tmpPath)
        fillPaint.color = Color.parseColor("#1A1410")
        canvas.drawRect(noseX, midY + sH * 0.06f, aftX, bellyY + 2f, fillPaint)
        val tile = sW * 0.10f
        var tx = cx - sW + tile
        var col = 0
        while (tx < aftX - tile * 0.4f) {
            var ty = midY + sH * 0.12f
            var row = 0
            val along = ((tx - noseX) / (aftX - noseX).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val spanHot = 1.10f - 0.28f * along
            while (ty < bellyY - tile * 0.25f) {
                val seed = ((col * 17 + row * 31) % 11) / 11f
                val local = tps * spanHot * (0.90f + 0.18f * seed) * (1f + 0.04f * sin((now * 3.1f + col + row).toDouble()).toFloat())
                fillPaint.color = tileBlackbody(local)
                canvas.drawRoundRect(tx, ty, tx + tile * 0.78f, ty + tile * 0.48f, 1.2f, 1.2f, fillPaint)
                if (local > 1250f) {
                    fillPaint.color = Color.argb(((local - 1250f) / 400f * 90f).toInt().coerceIn(0, 110), 255, 255, 255)
                    canvas.drawRoundRect(tx + tile * 0.12f, ty + tile * 0.08f, tx + tile * 0.55f, ty + tile * 0.28f, 1f, 1f, fillPaint)
                }
                ty += tile * 0.60f
                row++
            }
            tx += tile
            col++
        }
        canvas.restore()

        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2.0f
        strokePaint.color = Color.parseColor("#8A9098")
        canvas.drawPath(tmpPath, strokePaint)

        // Flaps hunt. Ionized N2 around the fins is blue-violet.
        fun flap(i: Int, x0: Float, y0: Float, out0: Float, drop0: Float, windward: Boolean) {
            val hunt = sin((now * (1.55f + i * 0.21f) + i * 1.9f).toDouble()).toFloat()
            val hunt2 = sin((now * 0.47f + i * 2.4f).toDouble()).toFloat()
            val k = 1f + 0.16f * hunt + 0.08f * hunt2
            val out = out0 * k
            val drop = drop0 * (0.92f + 0.16f * (1f - hunt * 0.5f))
            val hx = x0 + out * 0.55f
            val hy = y0 + drop * 0.45f
            if (heat > 0.08f) {
                val glow = (50 + 140 * heat * flick).toInt().coerceIn(0, 200)
                fillPaint.shader = RadialGradient(
                    hx, hy, sW * 0.55f,
                    intArrayOf(
                        Color.argb(glow, 180, 210, 255),
                        Color.argb((glow * 0.75f).toInt(), 90, 40, 255),
                        Color.argb((glow * 0.35f).toInt(), 200, 40, 220),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.40f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(hx, hy, sW * 0.48f, fillPaint)
                fillPaint.shader = null
            }
            tmpPath.reset()
            tmpPath.moveTo(x0, y0)
            tmpPath.lineTo(x0 + out, y0 + drop * 0.35f)
            tmpPath.lineTo(x0 + out * 0.15f, y0 + drop)
            tmpPath.close()
            fillPaint.color = if (windward) tileBlackbody(flapT * (0.88f + 0.10f * hunt)) else Color.parseColor("#8B9198")
            canvas.drawPath(tmpPath, fillPaint)
            strokePaint.strokeWidth = 1.4f
            strokePaint.color = Color.argb((80 + 120 * heat).toInt().coerceIn(0, 220), 140, 160, 255)
            canvas.drawPath(tmpPath, strokePaint)
        }
        flap(0, cx - sW * 0.55f, backY + sH * 0.04f, -sW * 0.38f, sH * 0.55f, false)
        flap(1, cx - sW * 0.55f, bellyY - sH * 0.04f, -sW * 0.38f, -sH * 0.55f, true)
        flap(2, cx + sW * 0.35f, backY + sH * 0.12f, sW * 0.42f, sH * 0.70f, false)
        flap(3, cx + sW * 0.35f, bellyY - sH * 0.08f, sW * 0.42f, -sH * 0.70f, true)

        fillPaint.shader = null
        fillPaint.color = Color.parseColor("#4A3220")
        val bellR = sH * 0.18f
        canvas.drawCircle(aftX + dp(2f), midY - sH * 0.22f, bellR, fillPaint)
        canvas.drawCircle(aftX + dp(2f), midY, bellR, fillPaint)
        canvas.drawCircle(aftX + dp(2f), midY + sH * 0.22f, bellR, fillPaint)

        drawLabel(canvas, "NOSE", noseX, backY - sp(8f), withLamp(skin.muted), w * 0.18f, sp(11f), sp(10f))
        drawLabel(canvas, "AFT", aftX, backY - sp(8f), withLamp(skin.muted), w * 0.18f, sp(11f), sp(10f))
        val banner = when {
            heat < 0.12f -> "BELLY FLOP  ·  ENTRY INTERFACE"
            heat < 0.45f -> "BELLY FLOP  ·  PLASMA BUILDING"
            else -> "BELLY FLOP  ·  HEAT SHIELD LIVE"
        }
        drawLabel(canvas, banner, cx, top + sp(16f), withLamp(skin.hold), w * 0.92f, sp(13f), sp(12f))

        val colW = w * 0.28f
        val yLab = bot - sp(28f)
        val yVal = bot - sp(10f)
        fun tempCol(lab: String, value: Float, x: Float, col: Int) {
            drawLabel(canvas, lab, x, yLab, withLamp(skin.muted), colW, sp(12f), sp(10f))
            drawLabel(canvas, String.format("%.0f C", value), x, yVal, withLamp(col), colW, sp(16f), sp(13f))
        }
        val hotCol = if (heat > 0.35f) skin.hold else skin.muted
        tempCol("TPS EST", tps, w * 0.20f, hotCol)
        tempCol("NOSE EST", noseT, cx, if (heat > 0.25f) skin.text else skin.muted)
        tempCol("FLAP EST", flapT, w * 0.80f, if (heat > 0.40f) skin.hold else skin.muted)
    }

        private fun drawFuelTanks(
        canvas: Canvas,
        can: RectF,
        lox: Float,
        rp: Float,
        skin: TelemetrySkin.Tokens,
        launch: LaunchSnapshot?,
        compact: Boolean
    ) {
        val labelBand = if (compact) 0f else can.height() * 0.18f
        val tankBot = can.bottom - labelBand
        val tankTop = can.top + can.height() * 0.02f
        val gap = can.width() * 0.10f
        val pad = can.width() * 0.06f
        val tankW = (can.width() - pad * 2f - gap) / 2f
        val tankH = (tankBot - tankTop).coerceAtLeast(8f)
        val fName = fuelName(launch)
        val fCol = fuelColor(launch)
        fun tank(x: Float, frac: Float, fill: Int, label: String) {
            val r = RectF(x, tankTop, x + tankW, tankBot)
            fillPaint.shader = null
            fillPaint.color = Color.argb(230, 8, 10, 12)
            canvas.drawRoundRect(r, 8f, 8f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3.2f
            strokePaint.color = withLamp(skin.accent)
            canvas.drawRoundRect(r, 8f, 8f, strokePaint)
            val fh = (tankH - 8f) * frac.coerceIn(0f, 1f)
            fillPaint.color = withLamp(fill)
            canvas.save()
            canvas.clipRect(r.left + 4f, r.top + 4f, r.right - 4f, r.bottom - 4f)
            canvas.drawRect(r.left + 4f, r.bottom - 4f - fh, r.right - 4f, r.bottom - 4f, fillPaint)
            canvas.restore()
            val pct = String.format("%.0f%%", frac * 100f)
            val room = fh > sp(22f) && tankW > sp(28f)
            if (room) {
                drawLabel(
                    canvas, pct, r.centerX(), r.bottom - 4f - fh * 0.45f,
                    Color.WHITE, tankW * 0.86f, sp(20f), if (compact) sp(14f) else sp(20f)
                )
            }
            if (!compact) {
                val labY = tankBot + labelBand * 0.68f
                drawLabel(
                    canvas, label, r.centerX(), labY,
                    withLamp(skin.text), tankW * 0.95f, labelBand * 0.8f, sp(14f)
                )
            }
        }
        tank(can.left + pad, lox, Color.parseColor("#3AA0FF"), "LOX")
        tank(can.left + pad + tankW + gap, rp, fCol, fName)
    }

    // ------------------------------------------------------------------- MISS
    private fun drawMiss(
        canvas: Canvas,
        w: Float,
        h: Float,
        launch: LaunchSnapshot?,
        tSec: Float,
        skin: TelemetrySkin.Tokens,
        prefs: AppPrefs
    ) {
        val m = MissionFacts.brief(launch, tSec)
        val left = dp(10f)
        val right = w - dp(10f)
        val top = dp(8f)
        val bot = h - dp(8f)
        fillPaint.shader = null
        fillPaint.color = Color.argb(236, 8, 12, 18)
        canvas.drawRoundRect(left, top, right, bot, 12f, 12f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2.4f
        strokePaint.color = withLamp(skin.accent)
        canvas.drawRoundRect(left, top, right, bot, 12f, 12f, strokePaint)
        val tick = dp(10f)
        strokePaint.strokeWidth = 2.8f
        canvas.drawLine(left, top, left + tick, top, strokePaint)
        canvas.drawLine(left, top, left, top + tick, strokePaint)
        canvas.drawLine(right, top, right - tick, top, strokePaint)
        canvas.drawLine(right, top, right, top + tick, strokePaint)
        canvas.drawLine(left, bot, left + tick, bot, strokePaint)
        canvas.drawLine(left, bot, left, bot - tick, strokePaint)
        canvas.drawLine(right, bot, right - tick, bot, strokePaint)
        canvas.drawLine(right, bot, right, bot - tick, strokePaint)

        val cx = w * 0.5f
        val maxW = right - left - dp(16f)
        val floor = bot - dp(12f)
        var lineH = sp(16f)
        var y = top + dp(16f)

        fun can(n: Int = 1) = y + lineH * n <= floor + 0.5f

        fun headline(text: String, col: Int, wish: Float) {
            if (!can()) return
            drawLabel(canvas, text, cx, y, withLamp(col), maxW, lineH * 0.92f, wish)
            y += lineH
        }
        fun wrapHead(text: String, col: Int, wish: Float, maxLines: Int = 4) {
            val room = floor - y
            if (room < wish * 0.55f) return
            val fit = (room / (wish * 1.12f)).toInt().coerceIn(1, maxLines)
            val used = drawWrapped(
                canvas, text, cx, y - wish * 0.85f, withLamp(col),
                maxW, wish, Paint.Align.CENTER, fit
            )
            y += max(used, lineH * 0.85f)
        }
        fun row(lab: String, value: String, col: Int = skin.text) {
            if (!can()) return
            val labW = maxW * 0.24f
            val valW = maxW * 0.70f
            val valX = left + dp(12f) + maxW * 0.26f
            drawLabel(
                canvas, lab, left + dp(12f), y, withLamp(skin.muted),
                labW, lineH * 0.90f, sp(10f), Paint.Align.LEFT
            )
            val room = floor - y
            val fit = (room / (sp(11f) * 1.12f)).toInt().coerceIn(1, 4)
            val used = drawWrapped(
                canvas, value, valX, y - sp(11f) * 0.85f, withLamp(col),
                valW, sp(11f), Paint.Align.LEFT, fit
            )
            y += max(used, lineH)
        }
        fun siteRow(lab: String, value: String) {
            if (!can()) return
            val labW = maxW * 0.20f
            val valW = maxW * 0.76f
            val valX = left + dp(12f) + maxW * 0.22f
            val wish = sp(16f)
            drawLabel(
                canvas, lab, left + dp(12f), y, withLamp(skin.text),
                labW, lineH * 0.92f, sp(14f), Paint.Align.LEFT
            )
            val room = floor - y
            val fit = (room / (wish * 1.12f)).toInt().coerceIn(1, 4)
            val used = drawWrapped(
                canvas, value, valX, y - wish * 0.85f, Color.WHITE,
                valW, wish, Paint.Align.LEFT, fit
            )
            y += max(used, lineH)
        }
        fun geoLinkRow(geo: String) {
            if (!can()) return
            val link = Color.parseColor("#5EB8FF")
            val click = "Click Me"
            val labW = maxW * 0.20f
            val wish = sp(16f)
            drawLabel(
                canvas, "GEO", left + dp(12f), y, withLamp(skin.text),
                labW, lineH * 0.92f, sp(14f), Paint.Align.LEFT
            )
            val oldTf = textPaint.typeface
            val oldAlign = textPaint.textAlign
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = wish
            textPaint.color = withLamp(link)
            textPaint.textAlign = Paint.Align.LEFT
            val gap = textPaint.measureText("   ")
            val geoW = textPaint.measureText(geo)
            val clickW = textPaint.measureText(click)
            val pair = geoW + gap + clickW
            val valX = left + dp(12f) + maxW * 0.22f
            val roomW = (right - dp(8f) - valX).coerceAtLeast(pair)
            val drawX = if (pair <= roomW) valX else (right - dp(8f) - pair).coerceAtLeast(left)
            val fm = textPaint.fontMetrics
            canvas.drawText(geo, drawX, y, textPaint)
            val clickX = drawX + geoW + gap
            canvas.drawText(click, clickX, y, textPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = (wish * 0.055f).coerceAtLeast(1.6f)
            strokePaint.color = withLamp(link)
            val ulY = y + fm.descent * 0.28f
            canvas.drawLine(drawX, ulY, drawX + geoW, ulY, strokePaint)
            canvas.drawLine(clickX, ulY, clickX + clickW, ulY, strokePaint)
            val box = (fm.descent - fm.ascent) * 1.12f
            geoHit.set(drawX, y + fm.ascent, clickX + clickW, y + fm.descent)
            textPaint.typeface = oldTf
            textPaint.textAlign = oldAlign
            y += max(box, lineH)
        }

        headline("MISSION", skin.accent, sp(12f))
        wrapHead(m.title, skin.text, sp(15f), 3)
        if (can()) headline(MissionFacts.goalLine(launch), skin.hold, sp(16f))
        if (can()) wrapHead(m.vehicle, skin.muted, sp(11f), 2)
        if (can()) {
            strokePaint.strokeWidth = 1.2f
            strokePaint.color = withLamp(skin.grid)
            canvas.drawLine(left + dp(12f), y, right - dp(12f), y, strokePaint)
            y += dp(6f)
        }

        val clock = if (tSec >= 0f) String.format("T+%02d:%02d", (tSec / 60).toInt(), (tSec % 60).toInt())
        else String.format("T-%02d:%02d", (-tSec / 60).toInt(), ((-tSec) % 60).toInt())

        data class Slot(val pri: Int, val lines: Int, val draw: () -> Unit)
        val slots = ArrayList<Slot>()
        slots += Slot(0, 3) {
            headline("PAYLOAD", skin.accent, sp(11f))
            row("NAME", m.payloadName, if (m.classified) skin.hold else skin.go)
            row("STATE", m.payloadState, skin.go)
        }
        slots += Slot(1, 4) {
            headline("OBJECTIVE", skin.accent, sp(11f))
            val room = floor - y
            val fit = (room / (sp(11f) * 1.12f)).toInt().coerceIn(1, 4)
            val used = drawWrapped(
                canvas, m.objective, cx, y - sp(11f) * 0.85f, withLamp(skin.text),
                maxW, sp(11f), Paint.Align.CENTER, fit
            )
            y += max(used, lineH)
        }
        if (m.classified || m.note.isNotBlank()) {
            slots += Slot(2, 3) {
                val room = floor - y
                val fit = (room / (sp(10f) * 1.12f)).toInt().coerceIn(1, 5)
                val used = drawWrapped(
                    canvas, m.note, cx, y - sp(10f) * 0.85f,
                    withLamp(if (m.classified) skin.hold else skin.muted),
                    maxW, sp(10f), Paint.Align.CENTER, fit
                )
                y += max(used, lineH)
            }
        }
        slots += Slot(3, 1) { row("STATUS", m.status, skin.go) }
        slots += Slot(4, 2) {
            row("KIND", m.payloadKind)
            row("COUNT", m.payloadCount)
        }
        slots += Slot(5, 1) { row("ENERGY", m.orbit, skin.muted) }
        m.booster?.let { b -> slots += Slot(6, 1) { row("BOOSTER", b, skin.hold) } }
        m.ship?.let { sh -> slots += Slot(6, 1) { row("SHIP", sh, skin.go) } }
        geoHit.setEmpty()
        slots += Slot(7, 2) {
            siteRow("SITE", PadBook.padShoreLine(launch))
            val ll = PadBook.lonLat(launch)
            if (ll != null) {
                val (lon, lat) = ll
                val hemiNS = if (lat >= 0f) "N" else "S"
                val hemiEW = if (lon >= 0f) "E" else "W"
                val geo = String.format("%.2f°%s  %.2f°%s", abs(lat), hemiNS, abs(lon), hemiEW)
                geoLinkRow(geo)
            }
        }
        if (!launch?.holdReason.isNullOrBlank()) {
            slots += Slot(7, 1) { row("HOLD", launch!!.holdReason!!.uppercase(), skin.hold) }
        }
        if (prefs.telemetryHoldUntilMs > System.currentTimeMillis()) {
            slots += Slot(7, 1) { row("OP HOLD", "ACTIVE", skin.hold) }
        }
        slots += Slot(0, 1) { headline(clock, skin.accent, sp(13f)) }

        val remain = ((floor - y) / sp(16f)).toInt().coerceAtLeast(0)
        val keep = slots.toMutableList()
        while (keep.sumOf { it.lines } > remain && keep.any { it.pri > 0 }) {
            val worst = keep.maxOf { it.pri }
            val drop = keep.indexOfLast { it.pri == worst }
            if (drop >= 0) keep.removeAt(drop) else break
        }
        val need = keep.sumOf { it.lines }.coerceAtLeast(1)
        lineH = ((floor - y) / need).coerceIn(sp(18f), sp(40f))
        for (slot in keep) slot.draw()
    }

    // --- flight helpers (ported from wallpaper Engine; not imported) ---

    private fun approximateProfile(tSec: Float, launch: LaunchSnapshot? = null, stage: Int = 1): Triple<Float, Float, String> =
        FlightProfiles.profile(tSec, launch, stage)

    private fun fuelRemain(tSec: Float, stage: Int = 1, launch: LaunchSnapshot? = module?.tracked): Float =
        FlightProfiles.fuelRemain(tSec, launch, stage)

    private fun drawAnalogRocker(canvas: Canvas, r: RectF, skin: TelemetrySkin.Tokens) {
        val analog = prefs?.telemetryAnalog == true
        val mid = r.centerX()
        fillPaint.shader = null
        fillPaint.color = Color.argb(230, 8, 12, 18)
        canvas.drawRoundRect(r, 6f, 6f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.6f
        strokePaint.color = withLamp(skin.accent)
        canvas.drawRoundRect(r, 6f, 6f, strokePaint)
        val left = RectF(r.left + 2f, r.top + 2f, mid - 1f, r.bottom - 2f)
        val right = RectF(mid + 1f, r.top + 2f, r.right - 2f, r.bottom - 2f)
        fun half(box: RectF, on: Boolean, label: String) {
            fillPaint.color = if (on) Color.argb(230, 16, 52, 78) else Color.argb(150, 8, 10, 14)
            canvas.drawRoundRect(box, 5f, 5f, fillPaint)
            if (on) {
                strokePaint.strokeWidth = 2.4f
                strokePaint.color = withLamp(skin.accent)
                canvas.drawRoundRect(box, 5f, 5f, strokePaint)
            }
            drawLabel(
                canvas, label, box.centerX(), box.centerY() + sp(4f),
                withLamp(if (on) Color.WHITE else skin.muted),
                box.width() * 0.94f, box.height() * 0.72f, sp(11f)
            )
        }
        half(left, !analog, "DIG")
        half(right, analog, "ANLG")
    }

    private fun drawStagePills(
        canvas: Canvas,
        w: Float,
        y: Float,
        h: Float,
        skin: TelemetrySkin.Tokens,
        stage: Int,
        analogChip: Boolean = false
    ) {
        analogHit.setEmpty()
        val rightReserve = if (analogChip) dp(96f) else 0f
        val usable = (w - rightReserve).coerceAtLeast(dp(120f))
        propStg1Hit.set(usable * 0.12f, y, usable * 0.46f, y + h)
        propStg2Hit.set(usable * 0.50f, y, usable * 0.84f, y + h)
        fun pill(r: RectF, lab: String, on: Boolean) {
            fillPaint.shader = null
            fillPaint.color = if (on) Color.argb(200, 12, 28, 48) else Color.argb(160, 8, 10, 14)
            canvas.drawRoundRect(r, 8f, 8f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (on) 3.0f else 1.4f
            strokePaint.color = withLamp(if (on) skin.accent else skin.muted)
            canvas.drawRoundRect(r, 8f, 8f, strokePaint)
            drawLabel(
                canvas, lab, r.centerX(), r.centerY() + sp(5f),
                withLamp(if (on) skin.text else skin.muted), r.width() * 0.9f, r.height() * 0.7f, sp(14f)
            )
        }
        pill(propStg1Hit, "STG1", stage == 1)
        pill(propStg2Hit, "STG2", stage == 2)
        if (analogChip) {
            analogHit.set(w - dp(10f) - dp(88f), y, w - dp(10f), y + h)
            drawAnalogRocker(canvas, analogHit, skin)
        }
    }

    private fun isRecoverable(launch: LaunchSnapshot?): Boolean = VehicleCatalog.isKnownRecoverable(launch)

    private fun landingTime(launch: LaunchSnapshot?): Float = FlightProfiles.boosterLandTime(launch)

    private fun engineCount(launch: LaunchSnapshot): Int =
        engineCountForStage(launch, prefs?.trackedStage ?: 1)

    private fun engineCountForStage(launch: LaunchSnapshot, stage: Int): Int =
        VehicleCatalog.engines(launch, stage)

    private fun vehicleFamily(launch: LaunchSnapshot): String = VehicleCatalog.family(launch)

    private fun missionEvents(launch: LaunchSnapshot): List<Pair<Float, String>> =
        FlightProfiles.events(launch)

    private fun sepTime(launch: LaunchSnapshot): Float = FlightProfiles.sepTime(launch)

    private fun stageBurning(tSec: Float, stage: Int, separated: Boolean, sep: Float, launch: LaunchSnapshot? = null): Boolean {
        if (tSec < 0f) return false
        if (launch != null) {
            val tot = if (stage >= 2) 6 else 33
            return FlightProfiles.enginesLit(tSec, launch, stage, tot) > 0
        }
        if (stage >= 2) return tSec >= sep && tSec < sep + 380f
        if (tSec < sep) return true
        if (tSec >= sep && tSec < sep + 32f) return true
        val land = FlightProfiles.boosterLandTime(null)
        if (tSec >= land - 22f && tSec < land) return true
        return false
    }

    private fun drawPlasmaSheath(
        canvas: Canvas,
        cx: Float,
        top: Float,
        bot: Float,
        halfW: Float,
        heat: Float,
        alpha: Float,
        fins: Boolean
    ) {
        if (heat < 0.04f || alpha < 0.05f || halfW < 2f) return
        val now = SystemClock.uptimeMillis() * 0.001f
        val flick = 0.82f + 0.18f * sin((now * 17.3f).toDouble()).toFloat()
        val a = (40 + 170 * heat * flick * alpha).toInt().coerceIn(0, 220)
        val midY = (top + bot) * 0.5f
        val h = (bot - top).coerceAtLeast(8f)
        fillPaint.shader = RadialGradient(
            cx, midY, halfW * 3.6f,
            intArrayOf(
                Color.argb(a, 255, 240, 255),
                Color.argb((a * 0.75f).toInt(), 255, 60, 180),
                Color.argb((a * 0.45f).toInt(), 90, 50, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.35f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(cx - halfW * 3.0f, top - h * 0.08f, cx + halfW * 3.0f, bot + h * 0.16f, fillPaint)
        fillPaint.shader = null
        val wake = h * (0.28f + 0.55f * heat)
        fillPaint.shader = LinearGradient(
            cx, bot, cx, bot + wake,
            intArrayOf(
                Color.argb((a * 0.85f).toInt(), 255, 220, 255),
                Color.argb((a * 0.40f).toInt(), 100, 70, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(cx - halfW * 2.3f, bot - h * 0.05f, cx + halfW * 2.3f, bot + wake, fillPaint)
        fillPaint.shader = null
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 5) {
            val wob = sin((now * (5.1f + i * 0.4f) + i).toDouble()).toFloat()
            val x0 = cx + (i - 2f) * halfW * 0.55f
            strokePaint.strokeWidth = halfW * (0.18f + 0.12f * heat)
            strokePaint.color = if (i % 2 == 0)
                Color.argb((a * 0.55f).toInt(), 120, 90, 255)
            else
                Color.argb((a * 0.55f).toInt(), 255, 80, 210)
            canvas.drawLine(x0, bot, x0 + wob * halfW * 0.8f, bot + wake * (0.55f + 0.12f * i), strokePaint)
        }
        if (fins) {
            val g = (70 + 140 * heat * flick * alpha).toInt().coerceIn(0, 210)
            val fy = top + h * 0.16f
            fillPaint.shader = RadialGradient(
                cx, fy, halfW * 2.8f,
                intArrayOf(
                    Color.argb(g, 180, 210, 255),
                    Color.argb((g * 0.65f).toInt(), 80, 40, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, fy, halfW * 2.6f, fillPaint)
            fillPaint.shader = null
        }
    }

    private fun drawVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        launch: LaunchSnapshot,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        VehicleDraw.draw(canvas, cx, baseY, h, launch, tSec, stage, separated, skin, lamp, alpha)
    }

    private fun drawBookGap(
        canvas: Canvas,
        cx: Float,
        y: Float,
        h: Float,
        skin: TelemetrySkin.Tokens,
        lamp: Float
    ) {
        fillPaint.shader = null
        fillPaint.color = Color.argb(180, 12, 10, 4)
        val w = h * 1.15f
        canvas.drawRoundRect(cx - w, y - h * 0.22f, cx + w, y + h * 0.28f, 8f, 8f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2.4f
        strokePaint.color = withLamp(skin.hold, lamp)
        canvas.drawRoundRect(cx - w, y - h * 0.22f, cx + w, y + h * 0.28f, 8f, 8f, strokePaint)
        drawLabel(
            canvas, VehicleCatalog.UPDATE_HEAD, cx, y - h * 0.04f,
            withLamp(skin.hold), w * 1.9f, h * 0.16f, sp(13f)
        )
        drawLabel(
            canvas, "WILL NOT INVENT DRAWING OR NUMBERS", cx, y + h * 0.14f,
            withLamp(skin.text), w * 1.9f, h * 0.12f, sp(11f)
        )
    }

    private fun drawVehicleBell(
        canvas: Canvas,
        cx: Float,
        y: Float,
        halfW: Float,
        hBell: Float,
        lamp: Float,
        alpha: Float,
        copper: Boolean
    ) {
        if (halfW < 1.4f || hBell < 2f) return
        val throat = halfW * 0.40f
        val p = Path()
        p.moveTo(cx - throat, y)
        p.lineTo(cx + throat, y)
        p.lineTo(cx + halfW, y + hBell)
        p.lineTo(cx - halfW, y + hBell)
        p.close()
        fillPaint.shader = null
        fillPaint.color = lampAlpha(
            if (copper) Color.parseColor("#C46A28") else Color.parseColor("#3A4048"),
            lamp, alpha
        )
        canvas.drawPath(p, fillPaint)
        fillPaint.color = lampAlpha(
            if (copper) Color.parseColor("#6A3010") else Color.parseColor("#101214"),
            lamp, alpha
        )
        canvas.drawOval(cx - halfW * 0.92f, y + hBell * 0.78f, cx + halfW * 0.92f, y + hBell * 1.12f, fillPaint)
        fillPaint.color = Color.argb((210 * alpha).toInt().coerceIn(0, 255), 6, 6, 8)
        canvas.drawOval(cx - halfW * 0.50f, y + hBell * 0.88f, cx + halfW * 0.50f, y + hBell * 1.08f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.15f
        strokePaint.color = lampAlpha(Color.parseColor("#D8C8B0"), lamp, alpha * 0.75f)
        canvas.drawPath(p, strokePaint)
    }

    private fun drawFlame(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        w: Float,
        h: Float,
        alpha: Float,
        tSec: Float,
        kind: String = "merlin"
    ) {
        val flick = 0.80f + 0.20f * sin((tSec * 33f + cx * 0.07f).toDouble()).toFloat()
        val len = h * flick
        val (c0, c1, c2) = when (kind) {
            "raptor" -> Triple(Color.rgb(70, 160, 255), Color.rgb(170, 220, 255), Color.rgb(240, 250, 255))
            "rs25" -> Triple(Color.rgb(110, 175, 255), Color.rgb(200, 230, 255), Color.rgb(255, 255, 255))
            "srb" -> Triple(Color.rgb(255, 78, 12), Color.rgb(255, 160, 50), Color.rgb(255, 235, 190))
            "rd107" -> Triple(Color.rgb(255, 70, 16), Color.rgb(255, 140, 36), Color.rgb(255, 215, 110))
            "mvac" -> Triple(Color.rgb(255, 96, 24), Color.rgb(255, 175, 64), Color.rgb(255, 236, 160))
            else -> Triple(Color.rgb(255, 64, 8), Color.rgb(255, 128, 28), Color.rgb(255, 214, 110))
        }
        fun wash(c: Int, a: Float): Int = Color.argb(
            (a * alpha * 255f).toInt().coerceIn(0, 255),
            Color.red(c), Color.green(c), Color.blue(c)
        )
        val plume = Path()
        plume.moveTo(cx - w * 0.70f, baseY)
        plume.quadTo(cx - w * 0.18f, baseY + len * 0.42f, cx, baseY + len)
        plume.quadTo(cx + w * 0.18f, baseY + len * 0.42f, cx + w * 0.70f, baseY)
        plume.close()
        fillPaint.shader = null
        fillPaint.color = wash(c0, if (kind == "srb") 0.70f else 0.52f)
        canvas.drawPath(plume, fillPaint)
        val core = Path()
        core.moveTo(cx - w * 0.30f, baseY)
        core.quadTo(cx, baseY + len * 0.58f, cx + w * 0.30f, baseY)
        core.close()
        fillPaint.color = wash(c1, 0.88f)
        canvas.drawPath(core, fillPaint)
        fillPaint.color = wash(c2, 0.96f)
        canvas.drawCircle(cx, baseY + len * 0.16f, w * 0.15f, fillPaint)
        if (kind == "raptor" || kind == "rs25" || kind == "mvac") {
            fillPaint.color = wash(c2, 0.50f)
            canvas.drawCircle(cx, baseY + len * 0.40f, w * 0.07f, fillPaint)
            canvas.drawCircle(cx, baseY + len * 0.60f, w * 0.045f, fillPaint)
        }
    }

    private fun drawStageTanks(
        canvas: Canvas,
        x: Float,
        top: Float,
        bot: Float,
        halfW: Float,
        fuel: Float,
        methalox: Boolean,
        lamp: Float,
        alpha: Float
    ) {
        if (bot - top < 6f || halfW < 2.2f) return
        val rim = (halfW * 0.08f).coerceAtLeast(1.1f)
        val innerL = x - halfW + rim
        val innerR = x + halfW - rim
        val pad = ((bot - top) * 0.028f).coerceAtLeast(1.4f)
        val tankTop = top + pad
        val tankBot = bot - pad
        val bulk = tankTop + (tankBot - tankTop) * 0.52f
        fillPaint.shader = null
        fillPaint.color = Color.argb((240 * alpha).toInt().coerceIn(0, 255), 8, 10, 14)
        canvas.drawRect(innerL, tankTop, innerR, tankBot, fillPaint)
        val lox = Color.argb((255 * alpha).toInt().coerceIn(0, 255), 28, 110, 220)
        val fuelC = if (methalox)
            Color.argb((255 * alpha).toInt().coerceIn(0, 255), 16, 205, 190)
        else
            Color.argb((255 * alpha).toInt().coerceIn(0, 255), 230, 105, 18)
        val f = fuel.coerceIn(0f, 1f)
        if (f > 0f) {
            fillPaint.color = lox
            canvas.drawRect(innerL, bulk - (bulk - tankTop) * f, innerR, bulk, fillPaint)
            fillPaint.color = fuelC
            canvas.drawRect(innerL, tankBot - (tankBot - bulk) * f, innerR, tankBot, fillPaint)
        }
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.4f
        strokePaint.color = Color.argb((210 * alpha).toInt().coerceIn(0, 255), 210, 225, 240)
        canvas.drawLine(innerL, bulk, innerR, bulk, strokePaint)
    }


    private fun drawOgive(
        canvas: Canvas,
        cx: Float,
        base: Float,
        tip: Float,
        halfW: Float,
        fill: Int,
        stroke: Int
    ) {
        VehicleOutline.ogive(canvas, cx, base, tip, halfW, fillPaint, strokePaint, fill, stroke)
    }

    private fun drawFalconVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        cores: Int,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
        val black = lampAlpha(Color.parseColor("#1A1A1A"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = 154f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val s1H = if (drawS2 && drawS1) h * 0.54f else if (drawS1) h * 0.92f else 0f
        val s2H = if (drawS1 && drawS2) h * 0.26f else if (drawS2) h * 0.62f else 0f
        val fairH = if (drawS2) (if (drawS1) h * 0.20f else h * 0.38f) else 0f
        val coreW = h * 0.070f
        val offsets = if (cores >= 3) floatArrayOf(-coreW * 2.15f, 0f, coreW * 2.15f) else floatArrayOf(0f)
        val s1Top = baseY - s1H
        val burning = stageBurning(tSec, stage, separated, sep)

        fun core(x: Float, sideCore: Boolean) {
            val top = if (sideCore && drawS2) s1Top else if (drawS1) s1Top else baseY - s2H - fairH
            val bot = baseY
            fillPaint.color = white
            canvas.drawRoundRect(x - coreW, top, x + coreW, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, x, top, bot, coreW, fuelRemain(tSec, 1), methalox = false, lamp, alpha)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            strokePaint.color = stroke
            canvas.drawRoundRect(x - coreW, top, x + coreW, bot, 3f, 3f, strokePaint)
            fillPaint.color = black
            canvas.drawRect(x - coreW, top, x + coreW, top + h * 0.018f, fillPaint)
            fillPaint.color = black
            canvas.drawRect(x - coreW * 1.05f, bot - h * 0.018f, x + coreW * 1.05f, bot, fillPaint)
            val bellR = coreW * 0.16f
            for (i in 0 until 9) {
                val a = Math.toRadians(-90.0 + i * 40.0)
                val rr = if (i == 0) 0f else coreW * 0.55f
                fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
                canvas.drawCircle(x + (rr * cos(a)).toFloat(), bot - h * 0.006f, bellR, fillPaint)
            }
            strokePaint.strokeWidth = (h * 0.010f).coerceIn(1.4f, 3.2f)
            strokePaint.color = lampAlpha(Color.parseColor("#C8CCD0"), lamp, alpha)
            val attach = bot - h * 0.012f
            val legsOut = separated && FlightProfiles.legsDeployed(module?.tracked, tSec, 1)
            val finsOut = separated && tSec >= 158f
            if (legsOut) {
                val legLen = h * 0.13f
                for (side in floatArrayOf(-1f, 1f)) {
                    for (d in floatArrayOf(0.85f, 1.15f)) {
                        val x0 = x + side * coreW * 0.9f
                        val x1 = x + side * coreW * (2.4f + d * 0.35f)
                        val y1 = attach + legLen
                        canvas.drawLine(x0, attach, x1, y1, strokePaint)
                        fillPaint.color = lampAlpha(Color.parseColor("#D0D4D8"), lamp, alpha)
                        canvas.drawRect(x1 - coreW * 0.22f, y1, x1 + coreW * 0.22f, y1 + h * 0.012f, fillPaint)
                    }
                }
            } else {
                val legLen = s1H.coerceAtLeast(h * 0.4f) * 0.34f
                for (side in floatArrayOf(-1f, 1f)) {
                    for (d in floatArrayOf(0.95f, 1.18f)) {
                        val x0 = x + side * coreW * d
                        val x1 = x + side * coreW * (d + 0.28f)
                        val y1 = attach - legLen
                        canvas.drawLine(x0, attach, x1, y1, strokePaint)
                        canvas.drawLine(x1, y1, x1 + side * coreW * 0.14f, y1 - h * 0.016f, strokePaint)
                    }
                }
            }
            if (drawS1) {
                fillPaint.color = lampAlpha(Color.parseColor("#4A4A4A"), lamp, alpha)
                val fin = coreW * (if (finsOut) 0.70f else 0.42f)
                val fy = top + s1H * 0.10f
                val rot = if (finsOut) 0f else 18f
                canvas.save()
                canvas.rotate(-rot, x - coreW, fy)
                canvas.drawRect(x - coreW - fin, fy - fin * 0.40f, x - coreW, fy + fin * 0.40f, fillPaint)
                canvas.restore()
                canvas.save()
                canvas.rotate(rot, x + coreW, fy)
                canvas.drawRect(x + coreW, fy - fin * 0.40f, x + coreW + fin, fy + fin * 0.40f, fillPaint)
                canvas.restore()
            }
            if (burning && drawS1) {
                drawFlame(canvas, x, bot, coreW * 1.8f, h * 0.20f, alpha, tSec, "merlin")
            }
        }

        if (drawS1) {
            for (i in offsets.indices) core(cx + offsets[i], cores >= 3 && i != 1)
        }
        if (drawS2) {
            val s2Bot = if (drawS1) s1Top else baseY
            val s2Top = s2Bot - s2H
            val s2W = coreW * 0.78f
            fillPaint.color = white
            canvas.drawRoundRect(cx - s2W, s2Top, cx + s2W, s2Bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, s2Top, s2Bot, s2W, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.strokeWidth = 1.5f
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - s2W, s2Top, cx + s2W, s2Bot, 3f, 3f, strokePaint)
            fillPaint.color = black
            canvas.drawRect(cx - s2W * 0.7f, s2Bot - h * 0.03f, cx + s2W * 0.7f, s2Bot, fillPaint)
            val vac = Path()
            vac.moveTo(cx - s2W * 0.42f, s2Bot)
            vac.lineTo(cx - s2W * 0.62f, s2Bot + h * 0.045f)
            vac.lineTo(cx + s2W * 0.62f, s2Bot + h * 0.045f)
            vac.lineTo(cx + s2W * 0.42f, s2Bot)
            vac.close()
            fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
            canvas.drawPath(vac, fillPaint)
            drawOgive(canvas, cx, s2Top, s2Top - fairH, s2W, white, stroke)
            if (burning && !drawS1) drawFlame(canvas, cx, s2Bot + h * 0.045f, s2W * 1.6f, h * 0.16f, alpha, tSec, "mvac")
        }
    }

    private fun drawSlsVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        launch: LaunchSnapshot,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val orange = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
        val white = lampAlpha(Color.parseColor("#E8E8E8"), lamp, alpha)
        val dark = lampAlpha(Color.parseColor("#2A3038"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = sepTime(launch)
        val drawCore = stage == 1
        val drawUpper = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.085f
        val srbW = h * 0.055f
        val coreH = if (drawUpper && drawCore) h * 0.62f else if (drawCore) h * 0.94f else 0f
        val coreTop = baseY - coreH
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawCore) {
            VehicleOutline.capsule(canvas, cx, coreTop, baseY, coreW, fillPaint, strokePaint, orange, stroke)
            drawStageTanks(canvas, cx, coreTop, baseY, coreW, fuelRemain(tSec), methalox = false, lamp, alpha)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + srbW + h * 0.008f)
                    val srbTop = coreTop + h * 0.04f
                    fillPaint.color = white
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, fillPaint)
                    strokePaint.color = stroke
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, strokePaint)
                    // Shuttle-derived five-segment SRB: frustum + ogive, not a needle.
                    VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                    if (tSec >= 0f && tSec < sep) {
                        drawFlame(canvas, x, baseY, srbW * 1.8f, h * 0.22f, alpha, tSec, "srb")
                    }
                }
            }
            if (stageBurning(tSec, 1, separated, sep) && drawCore) {
                drawFlame(canvas, cx, baseY, coreW * 1.5f, h * 0.18f, alpha, tSec, "rs25")
            }
        }
        if (drawUpper) {
            val uBot = if (drawCore) coreTop else baseY
            val uH = if (drawCore) h * 0.18f else h * 0.42f
            val uTop = uBot - uH
            fillPaint.color = dark
            canvas.drawRoundRect(cx - coreW * 0.78f, uTop, cx + coreW * 0.78f, uBot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, uTop, uBot, coreW * 0.78f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW * 0.78f, uTop, cx + coreW * 0.78f, uBot, 3f, 3f, strokePaint)
            val capH = if (drawCore) h * 0.12f else h * 0.28f
            val capTop = uTop - capH
            // Orion on the core. Pointy bit is the LAS tower, not the booster tips.
            VehicleOutline.ogive(canvas, cx, uTop, capTop, coreW * 0.62f, fillPaint, strokePaint, white, stroke, 0.48f)
            strokePaint.strokeWidth = 2.2f
            strokePaint.color = stroke
            canvas.drawLine(cx, capTop, cx, capTop - h * 0.08f, strokePaint)
            canvas.drawLine(cx - coreW * 0.18f, capTop - h * 0.02f, cx, capTop - h * 0.08f, strokePaint)
            canvas.drawLine(cx + coreW * 0.18f, capTop - h * 0.02f, cx, capTop - h * 0.08f, strokePaint)
            if (stageBurning(tSec, 2, true, sep) && !drawCore) {
                drawFlame(canvas, cx, uBot, coreW * 1.1f, h * 0.13f, alpha, tSec, "rs25")
            }
        }
    }

    private fun drawSoyuzVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val gray = lampAlpha(Color.parseColor("#D2CDBE"), lamp, alpha)
        val rust = lampAlpha(Color.parseColor("#B86A28"), lamp, alpha)
        val dark = lampAlpha(Color.parseColor("#3A3A38"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.9f)
        val sep = 118f
        val coreW = h * 0.072f
        val drawBoost = stage == 1 && !separated
        val drawCore = stage == 1
        val drawUpper = stage == 2 || (stage == 1 && !separated)
        val coreH = if (drawUpper && drawCore) h * 0.50f else if (drawCore) h * 0.90f else 0f
        val coreTop = baseY - coreH
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawBoost) {
            val bW = coreW * 0.70f
            for (side in floatArrayOf(-1f, 1f)) {
                for (d in floatArrayOf(1.55f, 2.15f)) {
                    val x = cx + side * coreW * d
                    val tip = coreTop + coreH * 0.16f
                    val p = Path()
                    p.moveTo(x - bW, baseY)
                    p.lineTo(x - bW * 0.42f, tip)
                    p.lineTo(x + bW * 0.42f, tip)
                    p.lineTo(x + bW, baseY)
                    p.close()
                    fillPaint.color = gray
                    canvas.drawPath(p, fillPaint)
                    canvas.save()
                    canvas.clipPath(p)
                    drawStageTanks(canvas, x, tip, baseY, bW * 0.78f, fuel, methalox = false, lamp, alpha)
                    canvas.restore()
                    strokePaint.color = stroke
                    canvas.drawPath(p, strokePaint)
                    fillPaint.color = rust
                    canvas.drawRect(x - bW * 0.22f, tip, x + bW * 0.22f, tip + h * 0.018f, fillPaint)
                    if (stageBurning(tSec, 1, separated, sep)) {
                        drawFlame(canvas, x, baseY, bW * 1.15f, h * 0.10f, alpha, tSec, "rd107")
                    }
                }
            }
        }
        if (drawCore) {
            fillPaint.color = gray
            canvas.drawRoundRect(cx - coreW, coreTop, cx + coreW, baseY, 2f, 2f, fillPaint)
            drawStageTanks(canvas, cx, coreTop, baseY, coreW, fuel, methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW, coreTop, cx + coreW, baseY, 2f, 2f, strokePaint)
            strokePaint.strokeWidth = 2.2f
            strokePaint.color = dark
            canvas.drawLine(cx - coreW * 1.35f, baseY, cx + coreW * 1.35f, baseY, strokePaint)
            canvas.drawLine(cx, baseY - coreW * 0.18f, cx, baseY + coreW * 0.55f, strokePaint)
            if (stageBurning(tSec, 1, separated, sep)) {
                drawFlame(canvas, cx, baseY, coreW * 1.6f, h * 0.12f, alpha, tSec, "rd107")
            }
        }
        if (drawUpper) {
            val uBot = if (drawCore) coreTop else baseY
            val shroudH = if (drawCore) h * 0.28f else h * 0.62f
            val sW = coreW * 1.05f
            fillPaint.color = gray
            canvas.drawRoundRect(cx - sW * 0.72f, uBot - shroudH * 0.42f, cx + sW * 0.72f, uBot, 2f, 2f, fillPaint)
            drawStageTanks(canvas, cx, uBot - shroudH * 0.42f, uBot, sW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            strokePaint.strokeWidth = 1.5f
            canvas.drawRoundRect(cx - sW * 0.72f, uBot - shroudH * 0.42f, cx + sW * 0.72f, uBot, 2f, 2f, strokePaint)
            drawOgive(canvas, cx, uBot - shroudH * 0.42f, uBot - shroudH, sW * 0.72f, gray, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawCore) {
                drawFlame(canvas, cx, uBot, coreW * 1.3f, h * 0.12f, alpha, tSec, "rd107")
            }
        }
    }


    private fun drawSuperHeavyGridFins(
        canvas: Canvas,
        cx: Float,
        y: Float,
        bW: Float,
        h: Float,
        deployed: Boolean,
        steel: Int,
        dark: Int,
        stroke: Int
    ) {
        val finW = if (deployed) bW * 1.15f else bW * 0.28f
        val finH = h * 0.055f
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.1f
        strokePaint.color = stroke
        for (side in floatArrayOf(-1f, 1f)) {
            val left = if (side < 0f) cx - bW - finW else cx + bW
            val right = left + finW
            val top = y - finH * 0.5f
            val bot = y + finH * 0.5f
            fillPaint.color = dark
            canvas.drawRect(left, top, right, bot, fillPaint)
            canvas.drawRect(left, top, right, bot, strokePaint)
            val cols = if (deployed) 4 else 2
            val rows = 3
            for (c in 1 until cols) {
                val x = left + finW * c / cols
                canvas.drawLine(x, top, x, bot, strokePaint)
            }
            for (r in 1 until rows) {
                val yy = top + finH * r / rows
                canvas.drawLine(left, yy, right, yy, strokePaint)
            }
        }
    }

    private fun drawStarshipVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float,
        launch: LaunchSnapshot? = null
    ) {
        val steel = lampAlpha(Color.parseColor("#C4C8CC"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val dark = lampAlpha(Color.parseColor("#3A3C40"), lamp, alpha)
        val hot = lampAlpha(Color.parseColor("#FFB020"), lamp, alpha)
        val core = lampAlpha(Color.parseColor("#FFE060"), lamp, alpha)
        val sep = FlightProfiles.sepTime(launch)
        val drawB = stage == 1
        val drawS = stage == 2 || (stage == 1 && !separated)
        val bH = if (drawS && drawB) h * 0.58f else if (drawB) h * 0.94f else 0f
        val sH = if (drawB && drawS) h * 0.42f else if (drawS) h * 0.94f else 0f
        val bW = h * 0.11f
        val sW = h * 0.095f
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.6f
        if (drawB) {
            val top = baseY - bH
            fillPaint.color = steel
            canvas.drawRoundRect(cx - bW, top, cx + bW, baseY, 6f, 6f, fillPaint)
            drawStageTanks(canvas, cx, top, baseY, bW, fuelRemain(tSec), methalox = true, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - bW, top, cx + bW, baseY, 6f, 6f, strokePaint)
            drawSuperHeavyGridFins(canvas, cx, top + bH * 0.16f, bW, h, separated, steel, dark, stroke)
            val lit = FlightProfiles.enginesLit(tSec, launch, 1, 33)
            var idx = 0
            fun ring(n: Int, rad: Float, rDot: Float) {
                for (i in 0 until n) {
                    val a = Math.toRadians(-90.0 + i * (360.0 / n))
                    val x = cx + (rad * cos(a)).toFloat()
                    val y = baseY - h * 0.012f + (rad * 0.18f * sin(a)).toFloat()
                    val on = idx < lit
                    fillPaint.color = if (on) hot else dark
                    canvas.drawCircle(x, y, rDot, fillPaint)
                    if (on) {
                        fillPaint.color = core
                        canvas.drawCircle(x, y, rDot * 0.42f, fillPaint)
                    }
                    idx++
                }
            }
            ring(3, bW * 0.22f, h * 0.0072f)
            ring(10, bW * 0.48f, h * 0.0062f)
            ring(20, bW * 0.72f, h * 0.0054f)
            if (stageBurning(tSec, 1, separated, sep, launch)) {
                drawFlame(canvas, cx, baseY, bW * 1.6f, h * 0.18f, alpha, tSec, "raptor")
            }
        }
        if (drawS) {
            val bot = if (drawB) baseY - bH else baseY
            val top = bot - sH
            fillPaint.color = steel
            canvas.drawRoundRect(cx - sW, top + sH * 0.12f, cx + sW, bot, 5f, 5f, fillPaint)
            drawStageTanks(canvas, cx, top + sH * 0.12f, bot, sW, fuelRemain(tSec, 2), methalox = true, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - sW, top + sH * 0.12f, cx + sW, bot, 5f, 5f, strokePaint)
            drawOgive(canvas, cx, top + sH * 0.12f, top, sW, steel, stroke)
            fillPaint.color = steel
            canvas.drawRect(cx - sW * 1.55f, bot - sH * 0.22f, cx - sW, bot - sH * 0.08f, fillPaint)
            canvas.drawRect(cx + sW, bot - sH * 0.22f, cx + sW * 1.55f, bot - sH * 0.08f, fillPaint)
            canvas.drawRect(cx - sW * 1.35f, top + sH * 0.18f, cx - sW, top + sH * 0.32f, fillPaint)
            canvas.drawRect(cx + sW, top + sH * 0.18f, cx + sW * 1.35f, top + sH * 0.32f, fillPaint)
            if (!drawB) drawPezRelease(canvas, cx, bot, sH, sW, tSec, launch, lamp, alpha)
            if (stageBurning(tSec, 2, true, sep, launch) && !drawB) {
                drawFlame(canvas, cx, bot, sW * 1.2f, h * 0.14f, alpha, tSec, "raptor")
            }
        }
    }

    private fun drawPezRelease(
        canvas: Canvas,
        cx: Float,
        bot: Float,
        sH: Float,
        sW: Float,
        tSec: Float,
        launch: LaunchSnapshot?,
        lamp: Float,
        alpha: Float
    ) {
        val deploy = FlightProfiles.events(launch).firstOrNull { "DEPLOY" in it.second.uppercase() }?.first ?: return
        if (tSec < deploy || tSec > deploy + 110f) return
        val u = ((tSec - deploy) / 110f).coerceIn(0f, 1f)
        val count = if (MissionFacts.isFlight13(launch)) 20 else 8
        val shown = (1 + (u * (count - 1)).toInt()).coerceIn(1, count)
        val bayX = cx + sW * 1.05f
        val bayY = bot - sH * 0.30f
        for (i in 0 until shown) {
            val drift = (i + 1) * sW * 0.42f * (0.25f + 0.75f * u)
            val yOff = ((i % 5) - 2) * sH * 0.028f
            val a = (210f * alpha * (1f - u * 0.35f)).toInt().coerceIn(40, 230)
            fillPaint.color = Color.argb(a, 70, 210, 130)
            val sl = sW * 0.48f
            val sh = sH * 0.028f
            canvas.drawRoundRect(bayX + drift, bayY + yOff, bayX + drift + sl, bayY + yOff + sh, 2f, 2f, fillPaint)
        }
    }


    private fun drawGlennVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val navy = lampAlpha(Color.parseColor("#1A2430"), lamp, alpha)
        val body = lampAlpha(Color.parseColor("#C5CCD3"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.9f)
        val sep = 180f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.095f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.6f
        if (drawS1) {
            val top = baseY - s1H
            fillPaint.color = body
            canvas.drawRoundRect(cx - w, top, cx + w, baseY, 5f, 5f, fillPaint)
            drawStageTanks(canvas, cx, top, baseY, w, fuelRemain(tSec), methalox = true, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - w, top, cx + w, baseY, 5f, 5f, strokePaint)
            fillPaint.color = navy
            canvas.drawRect(cx - w, top, cx + w, top + h * 0.03f, fillPaint)
            val fin = Path()
            for (side in floatArrayOf(-1f, 1f)) {
                fin.reset()
                fin.moveTo(cx + side * w, baseY - s1H * 0.18f)
                fin.lineTo(cx + side * w * 1.85f, baseY)
                fin.lineTo(cx + side * w, baseY)
                fin.close()
                fillPaint.color = navy
                canvas.drawPath(fin, fillPaint)
                canvas.drawPath(fin, strokePaint)
            }
            val burning = stageBurning(tSec, 1, separated, sep)
            val layout = ArrayList<Pair<Float, Float>>(7)
            layout.add(0f to 0f)
            for (i in 0 until 6) {
                val a = Math.toRadians(-90.0 + i * 60.0)
                layout.add(cos(a).toFloat() to sin(a).toFloat())
            }
            for ((dx, dy) in layout) {
                val ex = cx + dx * w * 0.62f
                val ey = baseY + dy * w * 0.10f
                fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
                canvas.drawCircle(ex, ey, w * 0.16f, fillPaint)
                if (burning) {
                    drawFlame(canvas, ex, ey + w * 0.08f, w * 0.55f, h * 0.11f, alpha, tSec, "raptor")
                }
            }
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.58f
            val sw = w * 0.78f
            fillPaint.color = body
            canvas.drawRoundRect(cx - sw, bot - s2H, cx + sw, bot, 4f, 4f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, sw, fuelRemain(tSec, 2), methalox = true, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - sw, bot - s2H, cx + sw, bot, 4f, 4f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.18f else h * 0.38f), sw, body, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) {
                drawFlame(canvas, cx - sw * 0.35f, bot, sw * 0.7f, h * 0.12f, alpha, tSec, "raptor")
                drawFlame(canvas, cx + sw * 0.35f, bot, sw * 0.7f, h * 0.12f, alpha, tSec, "raptor")
            }
        }
    }


    private fun drawElectronVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val carbon = lampAlpha(Color.parseColor("#1C1C1C"), lamp, alpha)
        val stroke = lampAlpha(Color.parseColor("#8A8A8A"), lamp, alpha)
        val sep = 162f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.048f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.3f
        if (drawS1) {
            fillPaint.color = carbon
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 2f, 2f, fillPaint)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, w, fuel, methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 2f, 2f, strokePaint)
            fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
            for (i in 0 until 9) {
                val a = Math.toRadians(-90.0 + i * 40.0)
                val rr = if (i == 0) 0f else w * 0.7f
                canvas.drawCircle(cx + (rr * cos(a)).toFloat(), baseY, w * 0.22f, fillPaint)
            }
            if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY, w * 2.2f, h * 0.12f, alpha, tSec, "merlin")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.26f else h * 0.62f
            fillPaint.color = carbon
            canvas.drawRoundRect(cx - w * 0.78f, bot - s2H, cx + w * 0.78f, bot, 2f, 2f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, w * 0.78f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - w * 0.78f, bot - s2H, cx + w * 0.78f, bot, 2f, 2f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.16f else h * 0.32f), w * 0.78f, carbon, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, w * 1.6f, h * 0.10f, alpha, tSec, "merlin")
        }
    }

    private fun drawArianeVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
        val black = lampAlpha(Color.parseColor("#222222"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = 137f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.07f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawS1) {
            fillPaint.color = black
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + h * 0.042f)
                    val bw = h * 0.032f
                    fillPaint.color = white
                    canvas.drawRoundRect(x - bw, baseY - s1H * 0.82f, x + bw, baseY, 3f, 3f, fillPaint)
                    drawStageTanks(canvas, x, baseY - s1H * 0.82f, baseY, bw, fuel, methalox = false, lamp, alpha)
                    strokePaint.color = stroke
                    canvas.drawRoundRect(x - bw, baseY - s1H * 0.82f, x + bw, baseY, 3f, 3f, strokePaint)
                    drawVehicleBell(canvas, x, baseY, bw * 0.95f, h * 0.042f, lamp, alpha, copper = false)
                    if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY + h * 0.04f, bw * 1.8f, h * 0.12f, alpha, tSec, "srb")
                }
            }
            drawVehicleBell(canvas, cx, baseY, coreW * 0.62f, h * 0.050f, lamp, alpha, copper = true)
            if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY + h * 0.048f, coreW * 1.5f, h * 0.14f, alpha, tSec, "rs25")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.50f
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.44f), coreW * 0.72f, white, stroke)
            drawVehicleBell(canvas, cx, bot, coreW * 0.48f, h * 0.055f, lamp, alpha, copper = true)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot + h * 0.05f, coreW * 1.2f, h * 0.12f, alpha, tSec, "rs25")
        }
    }

    private fun drawLmVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        wide: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val white = lampAlpha(Color.parseColor("#D8DCE0"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = 155f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * if (wide) 0.10f else 0.07f
        val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.92f else 0f
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawS1) {
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
            if (!separated) {
                val nBoost = if (wide) 4 else 2
                for (i in 0 until nBoost) {
                    val side = if (i < nBoost / 2) -1f else 1f
                    val k = if (nBoost == 4) (0.55f + (i % 2) * 0.55f) else 1.05f
                    val x = cx + side * coreW * (1.15f + k * 0.15f)
                    val bw = coreW * 0.38f
                    fillPaint.color = white
                    canvas.drawRoundRect(x - bw, baseY - s1H * 0.78f, x + bw, baseY, 3f, 3f, fillPaint)
                    drawStageTanks(canvas, x, baseY - s1H * 0.78f, baseY, bw, fuel, methalox = false, lamp, alpha)
                    strokePaint.color = stroke
                    canvas.drawRoundRect(x - bw, baseY - s1H * 0.78f, x + bw, baseY, 3f, 3f, strokePaint)
                    drawVehicleBell(canvas, x - bw * 0.42f, baseY, bw * 0.48f, h * 0.036f, lamp, alpha, copper = false)
                    drawVehicleBell(canvas, x + bw * 0.42f, baseY, bw * 0.48f, h * 0.036f, lamp, alpha, copper = false)
                    if (tSec >= 0f && tSec < sep) {
                        drawFlame(canvas, x - bw * 0.42f, baseY + h * 0.034f, bw * 0.9f, h * 0.09f, alpha, tSec, "merlin")
                        drawFlame(canvas, x + bw * 0.42f, baseY + h * 0.034f, bw * 0.9f, h * 0.09f, alpha, tSec, "merlin")
                    }
                }
            }
            drawVehicleBell(canvas, cx - coreW * 0.36f, baseY, coreW * 0.28f, h * 0.040f, lamp, alpha, copper = true)
            drawVehicleBell(canvas, cx + coreW * 0.36f, baseY, coreW * 0.28f, h * 0.040f, lamp, alpha, copper = true)
            if (stageBurning(tSec, 1, separated, sep)) {
                drawFlame(canvas, cx - coreW * 0.36f, baseY + h * 0.038f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
                drawFlame(canvas, cx + coreW * 0.36f, baseY + h * 0.038f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
            }
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.24f else h * 0.55f
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.40f), coreW * 0.72f, white, stroke)
            drawVehicleBell(canvas, cx, bot, coreW * 0.40f, h * 0.048f, lamp, alpha, copper = true)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot + h * 0.046f, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }


    private fun drawH3Vehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val white = lampAlpha(Color.parseColor("#E8ECF0"), lamp, alpha)
        val orange = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = 130f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.078f
        val srbW = h * 0.042f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawS1) {
            VehicleOutline.capsule(canvas, cx, baseY - s1H, baseY, coreW, fillPaint, strokePaint, white, stroke)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = false, lamp, alpha)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + srbW + h * 0.008f)
                    val srbTop = baseY - s1H * 0.78f
                    fillPaint.color = white
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, fillPaint)
                    strokePaint.color = stroke
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, strokePaint)
                    fillPaint.color = orange
                    canvas.drawRect(x - srbW, srbTop + (baseY - srbTop) * 0.35f, x + srbW, srbTop + (baseY - srbTop) * 0.42f, fillPaint)
                    VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                    if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY, srbW * 1.7f, h * 0.12f, alpha, tSec, "srb")
                }
            }
            drawVehicleBell(canvas, cx - coreW * 0.38f, baseY, coreW * 0.32f, h * 0.042f, lamp, alpha, copper = true)
            drawVehicleBell(canvas, cx + coreW * 0.38f, baseY, coreW * 0.32f, h * 0.042f, lamp, alpha, copper = true)
            if (stageBurning(tSec, 1, separated, sep)) {
                drawFlame(canvas, cx - coreW * 0.38f, baseY + h * 0.04f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
                drawFlame(canvas, cx + coreW * 0.38f, baseY + h * 0.04f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
            }
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.50f
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.44f), coreW * 0.72f, white, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }

    private fun drawIsroVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val white = lampAlpha(Color.parseColor("#F0EDE4"), lamp, alpha)
        val band = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = 150f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.070f
        val srbW = h * 0.058f
        val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.92f else 0f
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawS1) {
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + srbW + h * 0.006f)
                    val srbTop = baseY - s1H * 0.82f
                    fillPaint.color = white
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, fillPaint)
                    strokePaint.color = stroke
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, strokePaint)
                    fillPaint.color = band
                    canvas.drawRect(x - srbW, srbTop + (baseY - srbTop) * 0.40f, x + srbW, srbTop + (baseY - srbTop) * 0.48f, fillPaint)
                    VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                    if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY, srbW * 1.6f, h * 0.14f, alpha, tSec, "srb")
                }
            }
            drawVehicleBell(canvas, cx - coreW * 0.36f, baseY, coreW * 0.30f, h * 0.038f, lamp, alpha, copper = false)
            drawVehicleBell(canvas, cx + coreW * 0.36f, baseY, coreW * 0.30f, h * 0.038f, lamp, alpha, copper = false)
            if (stageBurning(tSec, 1, separated, sep)) {
                drawFlame(canvas, cx - coreW * 0.36f, baseY + h * 0.036f, coreW * 0.65f, h * 0.10f, alpha, tSec, "merlin")
                drawFlame(canvas, cx + coreW * 0.36f, baseY + h * 0.036f, coreW * 0.65f, h * 0.10f, alpha, tSec, "merlin")
            }
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.24f else h * 0.55f
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW * 0.78f, bot - s2H, cx + coreW * 0.78f, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.78f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW * 0.78f, bot - s2H, cx + coreW * 0.78f, bot, 3f, 3f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), coreW * 0.78f, white, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }

    private fun drawVulcanVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val dark = lampAlpha(Color.parseColor("#2A3038"), lamp, alpha)
        val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = 140f
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.080f
        val srbW = h * 0.036f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        val fuel = fuelRemain(tSec)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        if (drawS1) {
            fillPaint.color = dark
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = true, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + srbW + h * 0.006f)
                    val srbTop = baseY - s1H * 0.72f
                    fillPaint.color = white
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, fillPaint)
                    strokePaint.color = stroke
                    canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, strokePaint)
                    VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                    if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY, srbW * 1.6f, h * 0.10f, alpha, tSec, "srb")
                }
            }
            drawVehicleBell(canvas, cx - coreW * 0.40f, baseY, coreW * 0.34f, h * 0.048f, lamp, alpha, copper = false)
            drawVehicleBell(canvas, cx + coreW * 0.40f, baseY, coreW * 0.34f, h * 0.048f, lamp, alpha, copper = false)
            if (stageBurning(tSec, 1, separated, sep)) {
                drawFlame(canvas, cx - coreW * 0.40f, baseY + h * 0.046f, coreW * 0.75f, h * 0.12f, alpha, tSec, "raptor")
                drawFlame(canvas, cx + coreW * 0.40f, baseY + h * 0.046f, coreW * 0.75f, h * 0.12f, alpha, tSec, "raptor")
            }
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.52f
            fillPaint.color = white
            canvas.drawRoundRect(cx - coreW * 0.70f, bot - s2H, cx + coreW * 0.70f, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.70f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - coreW * 0.70f, bot - s2H, cx + coreW * 0.70f, bot, 3f, 3f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), coreW * 0.70f, white, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }

    private fun drawGenericVehicle(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        h: Float,
        tSec: Float,
        stage: Int,
        separated: Boolean,
        launch: LaunchSnapshot,
        skin: TelemetrySkin.Tokens,
        lamp: Float,
        alpha: Float
    ) {
        val body = lampAlpha(Color.parseColor("#C8D0D6"), lamp, alpha)
        val stroke = lampAlpha(skin.accent, lamp, alpha * 0.9f)
        val sep = sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.075f
        val s1H = if (drawS2 && drawS1) h * 0.55f else if (drawS1) h * 0.92f else 0f
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.6f
        if (drawS1) {
            fillPaint.color = body
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, fillPaint)
            drawStageTanks(canvas, cx, baseY - s1H, baseY, w, fuelRemain(tSec), launch.isBlueOrigin() || "glenn" in launch.rocketName.lowercase(), lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, strokePaint)
            val fin = Path()
            fin.moveTo(cx - w, baseY - s1H * 0.22f)
            fin.lineTo(cx - w * 1.7f, baseY)
            fin.lineTo(cx - w, baseY)
            fin.close()
            canvas.drawPath(fin, fillPaint)
            canvas.drawPath(fin, strokePaint)
            fin.reset()
            fin.moveTo(cx + w, baseY - s1H * 0.22f)
            fin.lineTo(cx + w * 1.7f, baseY)
            fin.lineTo(cx + w, baseY)
            fin.close()
            canvas.drawPath(fin, fillPaint)
            canvas.drawPath(fin, strokePaint)
            if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY, w * 1.6f, h * 0.16f, alpha, tSec, "merlin")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.25f else h * 0.58f
            fillPaint.color = body
            canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, fillPaint)
            drawStageTanks(canvas, cx, bot - s2H, bot, w * 0.72f, fuelRemain(tSec, 2), launch.isBlueOrigin() || "glenn" in launch.rocketName.lowercase(), lamp, alpha)
            strokePaint.color = stroke
            canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, strokePaint)
            drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), w * 0.72f, body, stroke)
            if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, w * 1.15f, h * 0.13f, alpha, tSec, "merlin")
        }
    }
}
