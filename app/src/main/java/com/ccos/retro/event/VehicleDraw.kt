package com.ccos.retro.event

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.skin.TelemetrySkin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared side-view silhouette. Wallpaper and MCC call [draw].
 * One tangent-ogive nose/body primitive. No circle noses, no dual-peak, no needle SRBs.
 */
object VehicleDraw {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    fun lampAlpha(c: Int, lamp: Float, alpha: Float): Int {
        val a = (Color.alpha(c) * lamp.coerceIn(0.15f, 1f) * alpha.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
    }

    fun draw(
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
        if (h < 8f || alpha < 0.04f) return
        val heat = FlightProfiles.plasmaHeat(launch, tSec, stage)
        if (heat > 0.04f) {
            plasma(canvas, cx, baseY - h, baseY, h * 0.12f, heat, alpha, stage == 1)
        }
        when (VehicleCatalog.drawFamily(launch)) {
            "f9", "zq" -> falcon(canvas, cx, baseY, h, tSec, stage, separated, 1, skin, lamp, alpha, launch)
            "fh" -> falcon(canvas, cx, baseY, h, tSec, stage, separated, 3, skin, lamp, alpha, launch)
            "sls" -> sls(canvas, cx, baseY, h, tSec, stage, separated, launch, skin, lamp, alpha)
            "soyuz" -> soyuz(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "starship" -> starship(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "electron" -> electron(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "glenn" -> glenn(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "ariane" -> ariane(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "lm5" -> lm(canvas, cx, baseY, h, tSec, stage, separated, true, skin, lamp, alpha, launch)
            "lm" -> lm(canvas, cx, baseY, h, tSec, stage, separated, false, skin, lamp, alpha, launch)
            "h3" -> h3(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "lvm3", "isro" -> lvm3(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "vulcan" -> vulcan(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "atlas" -> atlas(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "firefly" -> firefly(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            "proton" -> proton(canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch)
            else -> generic(canvas, cx, baseY, h, tSec, stage, separated, launch, skin, lamp, alpha)
        }
        if (VehicleCatalog.needsUpdate(launch)) {
            bookGap(canvas, cx, baseY - h * 0.55f, h, skin, lamp)
        }
    }

    fun bookGap(
        canvas: Canvas,
        cx: Float,
        y: Float,
        h: Float,
        skin: TelemetrySkin.Tokens,
        lamp: Float
    ) {
        fill.shader = null
        fill.color = Color.argb(190, 12, 10, 4)
        val w = h * 1.15f
        canvas.drawRoundRect(cx - w, y - h * 0.22f, cx + w, y + h * 0.28f, 8f, 8f, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2.4f
        stroke.color = lampAlpha(skin.hold, lamp, 1f)
        canvas.drawRoundRect(cx - w, y - h * 0.22f, cx + w, y + h * 0.28f, 8f, 8f, stroke)
        stroke.style = Paint.Style.FILL
        stroke.color = lampAlpha(skin.hold, lamp, 1f)
        stroke.strokeWidth = 0f
        fill.color = lampAlpha(skin.hold, lamp, 1f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lampAlpha(skin.hold, lamp, 1f)
            textAlign = Paint.Align.CENTER
            textSize = (h * 0.10f).coerceIn(9f, 16f)
            isFakeBoldText = true
        }
        canvas.drawText(VehicleCatalog.UPDATE_HEAD, cx, y - h * 0.02f, textPaint)
        textPaint.color = lampAlpha(skin.text, lamp, 1f)
        textPaint.textSize = (h * 0.08f).coerceIn(8f, 13f)
        canvas.drawText("WILL NOT INVENT DRAWING OR NUMBERS", cx, y + h * 0.16f, textPaint)
    }

    /**
     * Tangent ogive. Single peak. Not a circle, not dual-quad.
     * [base] is the cylinder join (larger y), [tip] is the nose (smaller y).
     */
    fun ogive(
        canvas: Canvas,
        cx: Float,
        base: Float,
        tip: Float,
        halfW: Float,
        fillC: Int,
        strokeC: Int
    ) {
        // ONE nose for the whole catalog. Canvas y grows down.
        // join = engines side (larger y). nose = pointy end UP (smaller y).
        val join = maxOf(base, tip)
        val noseY = minOf(base, tip)
        val L = (join - noseY).coerceAtLeast(1.5f)
        val R = halfW.coerceAtLeast(0.8f)
        val rho = (R * R + L * L) / (2f * R)
        path.reset()
        val steps = 14
        // i=0 at the point (top). i=steps at the cylinder join (full width).
        for (i in 0..steps) {
            val fromNose = L * i / steps
            val yr = (sqrt(max(0f, rho * rho - (L - fromNose) * (L - fromNose))) + R - rho).coerceAtLeast(0f)
            val y = noseY + fromNose
            if (i == 0) path.moveTo(cx - yr, y) else path.lineTo(cx - yr, y)
        }
        for (i in steps downTo 0) {
            val fromNose = L * i / steps
            val yr = (sqrt(max(0f, rho * rho - (L - fromNose) * (L - fromNose))) + R - rho).coerceAtLeast(0f)
            path.lineTo(cx + yr, noseY + fromNose)
        }
        path.close()
        fill.shader = null
        fill.color = fillC
        canvas.drawPath(path, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.6f
        stroke.color = strokeC
        canvas.drawPath(path, stroke)
    }

    /** Shuttle/SLS five-segment SRB: cylinder + frustum + blunt ogive. Not a needle. */
    fun srbFrustumOgive(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        topY: Float,
        halfW: Float,
        fillC: Int,
        strokeC: Int
    ) {
        val bodyH = (baseY - topY).coerceAtLeast(8f)
        val frustumH = bodyH * 0.10f
        val ogiveH = bodyH * 0.08f
        val cylTop = topY + frustumH + ogiveH
        val frustTop = topY + ogiveH
        val tipR = halfW * 0.70f
        fill.shader = null
        fill.color = fillC
        canvas.drawRoundRect(cx - halfW, cylTop, cx + halfW, baseY, 3f, 3f, fill)
        path.reset()
        path.moveTo(cx - halfW, cylTop)
        path.lineTo(cx - tipR, frustTop)
        path.lineTo(cx + tipR, frustTop)
        path.lineTo(cx + halfW, cylTop)
        path.close()
        canvas.drawPath(path, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        stroke.color = strokeC
        canvas.drawRoundRect(cx - halfW, cylTop, cx + halfW, baseY, 3f, 3f, stroke)
        canvas.drawPath(path, stroke)
        ogive(canvas, cx, frustTop, topY, tipR, fillC, strokeC)
    }

    private fun tanks(
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
        fill.shader = null
        fill.color = Color.argb((240 * alpha).toInt().coerceIn(0, 255), 8, 10, 14)
        canvas.drawRect(innerL, tankTop, innerR, tankBot, fill)
        val lox = Color.argb((255 * alpha).toInt().coerceIn(0, 255), 28, 110, 220)
        val fuelC = if (methalox)
            Color.argb((255 * alpha).toInt().coerceIn(0, 255), 16, 205, 190)
        else
            Color.argb((255 * alpha).toInt().coerceIn(0, 255), 230, 105, 18)
        val f = fuel.coerceIn(0f, 1f)
        if (f > 0f) {
            fill.color = lox
            canvas.drawRect(innerL, bulk - (bulk - tankTop) * f, innerR, bulk, fill)
            fill.color = fuelC
            canvas.drawRect(innerL, tankBot - (tankBot - bulk) * f, innerR, tankBot, fill)
        }
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.4f
        stroke.color = Color.argb((210 * alpha).toInt().coerceIn(0, 255), 210, 225, 240)
        canvas.drawLine(innerL, bulk, innerR, bulk, stroke)
    }

    private fun bell(
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
        path.reset()
        path.moveTo(cx - throat, y)
        path.lineTo(cx + throat, y)
        path.lineTo(cx + halfW, y + hBell)
        path.lineTo(cx - halfW, y + hBell)
        path.close()
        fill.shader = null
        fill.color = lampAlpha(
            if (copper) Color.parseColor("#C46A28") else Color.parseColor("#3A4048"),
            lamp, alpha
        )
        canvas.drawPath(path, fill)
        fill.color = lampAlpha(
            if (copper) Color.parseColor("#6A3010") else Color.parseColor("#101214"),
            lamp, alpha
        )
        canvas.drawOval(cx - halfW * 0.92f, y + hBell * 0.78f, cx + halfW * 0.92f, y + hBell * 1.12f, fill)
        fill.color = Color.argb((210 * alpha).toInt().coerceIn(0, 255), 6, 6, 8)
        canvas.drawOval(cx - halfW * 0.50f, y + hBell * 0.88f, cx + halfW * 0.50f, y + hBell * 1.08f, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.15f
        stroke.color = lampAlpha(Color.parseColor("#D8C8B0"), lamp, alpha * 0.75f)
        canvas.drawPath(path, stroke)
    }

    private fun flame(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        w: Float,
        h: Float,
        alpha: Float,
        tSec: Float,
        kind: String
    ) {
        val flick = 0.80f + 0.20f * sin((tSec * 33f + cx * 0.07f).toDouble()).toFloat()
        val len = h * flick
        val (c0, c1, c2) = when (kind) {
            "raptor", "ch4" -> Triple(Color.rgb(70, 160, 255), Color.rgb(170, 220, 255), Color.rgb(240, 250, 255))
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
        fill.shader = null
        fill.color = wash(c0, if (kind == "srb") 0.70f else 0.52f)
        canvas.drawPath(plume, fill)
        val core = Path()
        core.moveTo(cx - w * 0.30f, baseY)
        core.quadTo(cx, baseY + len * 0.58f, cx + w * 0.30f, baseY)
        core.close()
        fill.color = wash(c1, 0.88f)
        canvas.drawPath(core, fill)
        fill.color = wash(c2, 0.96f)
        canvas.drawCircle(cx, baseY + len * 0.16f, w * 0.15f, fill)
    }

    private fun fuelOf(tSec: Float, launch: LaunchSnapshot?, stage: Int) =
        FlightProfiles.fuelRemain(tSec, launch, stage)

    private fun burning(tSec: Float, launch: LaunchSnapshot?, stage: Int, sep: Float): Boolean {
        if (tSec < 0f) return false
        val spec = VehicleCatalog.spec(launch)
        val tot = if (stage >= 2) spec.s2Engines else spec.s1Engines
        return FlightProfiles.enginesLit(tSec, launch, stage, tot) > 0
    }

    private fun falcon(
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
        alpha: Float,
        launch: LaunchSnapshot?
    ) {
        val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
        val black = lampAlpha(Color.parseColor("#1A1A1A"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val s1H = if (drawS2 && drawS1) h * 0.54f else if (drawS1) h * 0.92f else 0f
        val s2H = if (drawS1 && drawS2) h * 0.26f else if (drawS2) h * 0.62f else 0f
        val fairH = if (drawS2) (if (drawS1) h * 0.20f else h * 0.38f) else 0f
        val coreW = h * 0.070f
        val offsets = if (cores >= 3) floatArrayOf(-coreW * 2.15f, 0f, coreW * 2.15f) else floatArrayOf(0f)
        val s1Top = baseY - s1H
        val on = burning(tSec, launch, stage, sep)
        fun core(x: Float, sideCore: Boolean) {
            val top = if (sideCore && drawS2) s1Top else if (drawS1) s1Top else baseY - s2H - fairH
            val bot = baseY
            fill.color = white
            canvas.drawRoundRect(x - coreW, top, x + coreW, bot, 3f, 3f, fill)
            tanks(canvas, x, top, bot, coreW, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.style = Paint.Style.STROKE
            stroke.strokeWidth = 1.5f
            stroke.color = strokeC
            canvas.drawRoundRect(x - coreW, top, x + coreW, bot, 3f, 3f, stroke)
            fill.color = black
            if (sideCore) {
                val noseH = coreW * 1.65f
                ogive(canvas, x, top, top - noseH, coreW, white, strokeC)
            } else {
                canvas.drawRect(x - coreW, top, x + coreW, top + h * 0.018f, fill)
            }
            canvas.drawRect(x - coreW * 1.05f, bot - h * 0.018f, x + coreW * 1.05f, bot, fill)
            bell(canvas, x - coreW * 0.42f, bot, coreW * 0.22f, h * 0.028f, lamp, alpha, false)
            bell(canvas, x, bot, coreW * 0.22f, h * 0.028f, lamp, alpha, false)
            bell(canvas, x + coreW * 0.42f, bot, coreW * 0.22f, h * 0.028f, lamp, alpha, false)
            stroke.strokeWidth = (h * 0.010f).coerceIn(1.4f, 3.2f)
            stroke.color = lampAlpha(Color.parseColor("#C8CCD0"), lamp, alpha)
            val attach = bot - h * 0.012f
            val legsOut = separated && FlightProfiles.legsDeployed(launch, tSec, 1)
            val finsOut = separated && tSec >= 158f
            if (legsOut) {
                val legLen = h * 0.13f
                for (side in floatArrayOf(-1f, 1f)) {
                    for (d in floatArrayOf(0.85f, 1.15f)) {
                        val x0 = x + side * coreW * 0.9f
                        val x1 = x + side * coreW * (2.4f + d * 0.35f)
                        val y1 = attach + legLen
                        canvas.drawLine(x0, attach, x1, y1, stroke)
                        fill.color = lampAlpha(Color.parseColor("#D0D4D8"), lamp, alpha)
                        canvas.drawRect(x1 - coreW * 0.22f, y1, x1 + coreW * 0.22f, y1 + h * 0.012f, fill)
                    }
                }
            } else {
                val legLen = s1H.coerceAtLeast(h * 0.4f) * 0.34f
                for (side in floatArrayOf(-1f, 1f)) {
                    for (d in floatArrayOf(0.95f, 1.18f)) {
                        val x0 = x + side * coreW * d
                        val x1 = x + side * coreW * (d + 0.28f)
                        val y1 = attach - legLen
                        canvas.drawLine(x0, attach, x1, y1, stroke)
                        canvas.drawLine(x1, y1, x1 + side * coreW * 0.14f, y1 - h * 0.016f, stroke)
                    }
                }
            }
            if (drawS1) {
                fill.color = lampAlpha(Color.parseColor("#4A4A4A"), lamp, alpha)
                val fin = coreW * (if (finsOut) 0.70f else 0.42f)
                val fy = top + s1H * 0.10f
                val rot = if (finsOut) 0f else 18f
                canvas.save()
                canvas.rotate(-rot, x - coreW, fy)
                canvas.drawRect(x - coreW - fin, fy - fin * 0.40f, x - coreW, fy + fin * 0.40f, fill)
                canvas.restore()
                canvas.save()
                canvas.rotate(rot, x + coreW, fy)
                canvas.drawRect(x + coreW, fy - fin * 0.40f, x + coreW + fin, fy + fin * 0.40f, fill)
                canvas.restore()
            }
            if (on && drawS1) flame(canvas, x, bot, coreW * 1.8f, h * 0.20f, alpha, tSec, "merlin")
        }
        if (drawS1) {
            for (i in offsets.indices) core(cx + offsets[i], cores >= 3 && i != 1)
        }
        if (drawS2) {
            val s2Bot = if (drawS1) s1Top else baseY
            val s2Top = s2Bot - s2H
            val s2W = coreW * 0.78f
            fill.color = white
            canvas.drawRoundRect(cx - s2W, s2Top, cx + s2W, s2Bot, 3f, 3f, fill)
            tanks(canvas, cx, s2Top, s2Bot, s2W, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.strokeWidth = 1.5f
            stroke.color = strokeC
            canvas.drawRoundRect(cx - s2W, s2Top, cx + s2W, s2Bot, 3f, 3f, stroke)
            if (!drawS1) {
                fill.color = black
                canvas.drawRect(cx - s2W * 0.7f, s2Bot - h * 0.03f, cx + s2W * 0.7f, s2Bot, fill)
                bell(canvas, cx, s2Bot, s2W * 0.55f, h * 0.045f, lamp, alpha, false)
            }
            ogive(canvas, cx, s2Top, s2Top - fairH, s2W, white, strokeC)
            if (on && !drawS1) flame(canvas, cx, s2Bot + h * 0.045f, s2W * 1.6f, h * 0.16f, alpha, tSec, "mvac")
        }
    }

    private fun sls(
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
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawCore = stage == 1
        val drawUpper = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.085f
        val srbW = h * 0.058f
        val coreH = if (drawUpper && drawCore) h * 0.58f else if (drawCore) h * 0.90f else 0f
        val coreTop = baseY - coreH
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawCore) {
            VehicleOutline.capsule(canvas, cx, coreTop, baseY, coreW, fill, stroke, orange, strokeC)
            tanks(canvas, cx, coreTop, baseY, coreW, fuelOf(tSec, launch, 1), false, lamp, alpha)
            bell(canvas, cx - coreW * 0.38f, baseY, coreW * 0.28f, h * 0.032f, lamp, alpha, true)
            bell(canvas, cx + coreW * 0.38f, baseY, coreW * 0.28f, h * 0.032f, lamp, alpha, true)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + srbW + h * 0.006f)
                    val srbTop = coreTop - h * 0.02f
                    srbFrustumOgive(canvas, x, baseY, srbTop, srbW, white, strokeC)
                    if (tSec >= 0f && tSec < sep) {
                        flame(canvas, x, baseY, srbW * 1.8f, h * 0.22f, alpha, tSec, "srb")
                    }
                }
            }
            if (burning(tSec, launch, 1, sep) && drawCore) {
                flame(canvas, cx, baseY, coreW * 1.5f, h * 0.18f, alpha, tSec, "rs25")
            }
        }
        if (drawUpper) {
            val uBot = if (drawCore) coreTop else baseY
            val icpsH = if (drawCore) h * 0.10f else h * 0.22f
            val icpsTop = uBot - icpsH
            fill.color = dark
            canvas.drawRoundRect(cx - coreW * 0.72f, icpsTop, cx + coreW * 0.72f, uBot, 2f, 2f, fill)
            tanks(canvas, cx, icpsTop, uBot, coreW * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW * 0.72f, icpsTop, cx + coreW * 0.72f, uBot, 2f, 2f, stroke)
            val capH = if (drawCore) h * 0.09f else h * 0.20f
            val capTop = icpsTop - capH
            path.reset()
            path.moveTo(cx - coreW * 0.70f, icpsTop)
            path.lineTo(cx - coreW * 0.42f, capTop)
            path.lineTo(cx + coreW * 0.42f, capTop)
            path.lineTo(cx + coreW * 0.70f, icpsTop)
            path.close()
            fill.color = white
            canvas.drawPath(path, fill)
            stroke.color = strokeC
            canvas.drawPath(path, stroke)
            val lasH = if (drawCore) h * 0.11f else h * 0.24f
            val lasBot = capTop
            val lasTop = lasBot - lasH
            fill.color = white
            canvas.drawRect(cx - coreW * 0.10f, lasTop + lasH * 0.18f, cx + coreW * 0.10f, lasBot, fill)
            stroke.color = strokeC
            canvas.drawLine(cx - coreW * 0.28f, lasBot, cx - coreW * 0.08f, lasTop + lasH * 0.28f, stroke)
            canvas.drawLine(cx + coreW * 0.28f, lasBot, cx + coreW * 0.08f, lasTop + lasH * 0.28f, stroke)
            ogive(canvas, cx, lasTop + lasH * 0.22f, lasTop, coreW * 0.16f, white, strokeC)
            if (burning(tSec, launch, 2, sep) && !drawCore) {
                flame(canvas, cx, uBot, coreW * 1.1f, h * 0.13f, alpha, tSec, "rs25")
            }
        }
    }

    private fun soyuz(
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
        launch: LaunchSnapshot?
    ) {
        val gray = lampAlpha(Color.parseColor("#D2CDBE"), lamp, alpha)
        val rust = lampAlpha(Color.parseColor("#B86A28"), lamp, alpha)
        val dark = lampAlpha(Color.parseColor("#3A3A38"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.9f)
        val sep = FlightProfiles.sepTime(launch)
        val coreW = h * 0.072f
        val drawBoost = stage == 1 && !separated
        val drawCore = stage == 1
        val drawUpper = stage == 2 || (stage == 1 && !separated)
        val coreH = if (drawUpper && drawCore) h * 0.50f else if (drawCore) h * 0.90f else 0f
        val coreTop = baseY - coreH
        val fuel = fuelOf(tSec, launch, 1)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawBoost) {
            val bW = coreW * 0.70f
            for (side in floatArrayOf(-1f, 1f)) {
                for (d in floatArrayOf(1.55f, 2.15f)) {
                    val x = cx + side * coreW * d
                    val tip = coreTop + coreH * 0.16f
                    path.reset()
                    path.moveTo(x - bW, baseY)
                    path.lineTo(x - bW * 0.42f, tip)
                    path.lineTo(x + bW * 0.42f, tip)
                    path.lineTo(x + bW, baseY)
                    path.close()
                    fill.color = gray
                    canvas.drawPath(path, fill)
                    canvas.save()
                    canvas.clipPath(path)
                    tanks(canvas, x, tip, baseY, bW * 0.78f, fuel, false, lamp, alpha)
                    canvas.restore()
                    stroke.color = strokeC
                    canvas.drawPath(path, stroke)
                    fill.color = rust
                    canvas.drawRect(x - bW * 0.22f, tip, x + bW * 0.22f, tip + h * 0.018f, fill)
                    if (burning(tSec, launch, 1, sep)) {
                        flame(canvas, x, baseY, bW * 1.15f, h * 0.10f, alpha, tSec, "rd107")
                    }
                }
            }
        }
        if (drawCore) {
            fill.color = gray
            canvas.drawRoundRect(cx - coreW, coreTop, cx + coreW, baseY, 2f, 2f, fill)
            tanks(canvas, cx, coreTop, baseY, coreW, fuel, false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW, coreTop, cx + coreW, baseY, 2f, 2f, stroke)
            stroke.strokeWidth = 2.2f
            stroke.color = dark
            canvas.drawLine(cx - coreW * 1.35f, baseY, cx + coreW * 1.35f, baseY, stroke)
            canvas.drawLine(cx, baseY - coreW * 0.18f, cx, baseY + coreW * 0.55f, stroke)
            if (burning(tSec, launch, 1, sep)) {
                flame(canvas, cx, baseY, coreW * 1.6f, h * 0.12f, alpha, tSec, "rd107")
            }
        }
        if (drawUpper) {
            val uBot = if (drawCore) coreTop else baseY
            val shroudH = if (drawCore) h * 0.28f else h * 0.62f
            val sW = coreW * 1.05f
            fill.color = gray
            canvas.drawRoundRect(cx - sW * 0.72f, uBot - shroudH * 0.42f, cx + sW * 0.72f, uBot, 2f, 2f, fill)
            tanks(canvas, cx, uBot - shroudH * 0.42f, uBot, sW * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            stroke.strokeWidth = 1.5f
            canvas.drawRoundRect(cx - sW * 0.72f, uBot - shroudH * 0.42f, cx + sW * 0.72f, uBot, 2f, 2f, stroke)
            ogive(canvas, cx, uBot - shroudH * 0.42f, uBot - shroudH, sW * 0.72f, gray, strokeC)
            if (burning(tSec, launch, 2, sep) && !drawCore) {
                flame(canvas, cx, uBot, coreW * 1.3f, h * 0.12f, alpha, tSec, "rd107")
            }
        }
    }

    private fun gridFins(
        canvas: Canvas,
        cx: Float,
        y: Float,
        bW: Float,
        h: Float,
        deployed: Boolean,
        dark: Int,
        strokeC: Int
    ) {
        val finW = if (deployed) bW * 1.15f else bW * 0.28f
        val finH = h * 0.055f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.1f
        stroke.color = strokeC
        for (side in floatArrayOf(-1f, 1f)) {
            val left = if (side < 0f) cx - bW - finW else cx + bW
            val right = left + finW
            val top = y - finH * 0.5f
            val bot = y + finH * 0.5f
            fill.color = dark
            canvas.drawRect(left, top, right, bot, fill)
            canvas.drawRect(left, top, right, bot, stroke)
            val cols = if (deployed) 4 else 2
            val rows = 3
            for (c in 1 until cols) {
                val x = left + finW * c / cols
                canvas.drawLine(x, top, x, bot, stroke)
            }
            for (r in 1 until rows) {
                val yy = top + finH * r / rows
                canvas.drawLine(left, yy, right, yy, stroke)
            }
        }
    }

    private fun starship(
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
        launch: LaunchSnapshot?
    ) {
        val steel = lampAlpha(Color.parseColor("#C4C8CC"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val dark = lampAlpha(Color.parseColor("#3A3C40"), lamp, alpha)
        val sep = FlightProfiles.sepTime(launch)
        val drawB = stage == 1
        val drawS = stage == 2 || (stage == 1 && !separated)
        val bH = if (drawS && drawB) h * 0.58f else if (drawB) h * 0.94f else 0f
        val sH = if (drawB && drawS) h * 0.42f else if (drawS) h * 0.94f else 0f
        val bW = h * 0.11f
        val sW = h * 0.095f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.6f
        if (drawB) {
            val top = baseY - bH
            fill.color = steel
            canvas.drawRoundRect(cx - bW, top, cx + bW, baseY, 6f, 6f, fill)
            tanks(canvas, cx, top, baseY, bW, fuelOf(tSec, launch, 1), true, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - bW, top, cx + bW, baseY, 6f, 6f, stroke)
            gridFins(canvas, cx, top + bH * 0.16f, bW, h, separated, dark, strokeC)
            for (k in floatArrayOf(-0.62f, -0.31f, 0f, 0.31f, 0.62f)) {
                bell(canvas, cx + bW * k, baseY, bW * 0.14f, h * 0.022f, lamp, alpha, false)
            }
            if (burning(tSec, launch, 1, sep)) {
                flame(canvas, cx, baseY, bW * 1.6f, h * 0.18f, alpha, tSec, "raptor")
            }
        }
        if (drawS) {
            val bot = if (drawB) baseY - bH else baseY
            val top = bot - sH
            fill.color = steel
            canvas.drawRoundRect(cx - sW, top + sH * 0.12f, cx + sW, bot, 5f, 5f, fill)
            tanks(canvas, cx, top + sH * 0.12f, bot, sW, fuelOf(tSec, launch, 2), true, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - sW, top + sH * 0.12f, cx + sW, bot, 5f, 5f, stroke)
            ogive(canvas, cx, top + sH * 0.12f, top, sW, steel, strokeC)
            fill.color = steel
            canvas.drawRect(cx - sW * 1.55f, bot - sH * 0.22f, cx - sW, bot - sH * 0.08f, fill)
            canvas.drawRect(cx + sW, bot - sH * 0.22f, cx + sW * 1.55f, bot - sH * 0.08f, fill)
            canvas.drawRect(cx - sW * 1.35f, top + sH * 0.18f, cx - sW, top + sH * 0.32f, fill)
            canvas.drawRect(cx + sW, top + sH * 0.18f, cx + sW * 1.35f, top + sH * 0.32f, fill)
            if (!drawB) pez(canvas, cx, bot, sH, sW, tSec, launch, alpha)
            if (burning(tSec, launch, 2, sep) && !drawB) {
                flame(canvas, cx, bot, sW * 1.2f, h * 0.14f, alpha, tSec, "raptor")
            }
        }
    }

    private fun pez(
        canvas: Canvas,
        cx: Float,
        bot: Float,
        sH: Float,
        sW: Float,
        tSec: Float,
        launch: LaunchSnapshot?,
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
            fill.color = Color.argb(a, 70, 210, 130)
            val sl = sW * 0.48f
            val sh = sH * 0.028f
            canvas.drawRoundRect(bayX + drift, bayY + yOff, bayX + drift + sl, bayY + yOff + sh, 2f, 2f, fill)
        }
    }

    private fun electron(
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
        launch: LaunchSnapshot?
    ) {
        val carbon = lampAlpha(Color.parseColor("#1C1C1C"), lamp, alpha)
        val strokeC = lampAlpha(Color.parseColor("#8A8A8A"), lamp, alpha)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.048f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.3f
        if (drawS1) {
            fill.color = carbon
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 2f, 2f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, w, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 2f, 2f, stroke)
            bell(canvas, cx, baseY, w * 0.7f, h * 0.028f, lamp, alpha, false)
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY, w * 2.2f, h * 0.12f, alpha, tSec, "merlin")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.26f else h * 0.62f
            fill.color = carbon
            canvas.drawRoundRect(cx - w * 0.78f, bot - s2H, cx + w * 0.78f, bot, 2f, 2f, fill)
            tanks(canvas, cx, bot - s2H, bot, w * 0.78f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w * 0.78f, bot - s2H, cx + w * 0.78f, bot, 2f, 2f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.16f else h * 0.32f), w * 0.78f, carbon, strokeC)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot, w * 1.6f, h * 0.10f, alpha, tSec, "merlin")
        }
    }

    private fun glenn(
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
        launch: LaunchSnapshot?
    ) {
        val navy = lampAlpha(Color.parseColor("#1A2430"), lamp, alpha)
        val body = lampAlpha(Color.parseColor("#C5CCD3"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.9f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.095f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.6f
        if (drawS1) {
            val top = baseY - s1H
            fill.color = body
            canvas.drawRoundRect(cx - w, top, cx + w, baseY, 5f, 5f, fill)
            tanks(canvas, cx, top, baseY, w, fuelOf(tSec, launch, 1), true, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w, top, cx + w, baseY, 5f, 5f, stroke)
            fill.color = navy
            canvas.drawRect(cx - w, top, cx + w, top + h * 0.03f, fill)
            for (k in floatArrayOf(-0.55f, -0.18f, 0.18f, 0.55f)) {
                bell(canvas, cx + w * k, baseY, w * 0.16f, h * 0.030f, lamp, alpha, false)
            }
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY, w * 1.3f, h * 0.13f, alpha, tSec, "ch4")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.58f
            val sw = w * 0.78f
            fill.color = body
            canvas.drawRoundRect(cx - sw, bot - s2H, cx + sw, bot, 4f, 4f, fill)
            tanks(canvas, cx, bot - s2H, bot, sw, fuelOf(tSec, launch, 2), true, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - sw, bot - s2H, cx + sw, bot, 4f, 4f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.18f else h * 0.38f), sw, body, strokeC)
            if (burning(tSec, launch, 2, sep) && !drawS1) {
                flame(canvas, cx - sw * 0.35f, bot, sw * 0.7f, h * 0.12f, alpha, tSec, "ch4")
                flame(canvas, cx + sw * 0.35f, bot, sw * 0.7f, h * 0.12f, alpha, tSec, "ch4")
            }
        }
    }

    private fun ariane(
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
        launch: LaunchSnapshot?
    ) {
        val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
        val black = lampAlpha(Color.parseColor("#222222"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.07f
        val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = black
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, coreW, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, stroke)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + h * 0.042f)
                    val bw = h * 0.032f
                    srbFrustumOgive(canvas, x, baseY, baseY - s1H * 0.82f, bw, white, strokeC)
                    if (tSec >= 0f && tSec < sep) flame(canvas, x, baseY, bw * 1.8f, h * 0.12f, alpha, tSec, "srb")
                }
            }
            bell(canvas, cx, baseY, coreW * 0.62f, h * 0.050f, lamp, alpha, true)
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY + h * 0.048f, coreW * 1.5f, h * 0.14f, alpha, tSec, "rs25")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.50f
            fill.color = white
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.44f), coreW * 0.72f, white, strokeC)
            bell(canvas, cx, bot, coreW * 0.48f, h * 0.055f, lamp, alpha, true)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot + h * 0.05f, coreW * 1.2f, h * 0.12f, alpha, tSec, "rs25")
        }
    }

    private fun lm(
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
        alpha: Float,
        launch: LaunchSnapshot?
    ) {
        val white = lampAlpha(Color.parseColor("#D8DCE0"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * if (wide) 0.10f else 0.07f
        val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.92f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = white
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, coreW, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, stroke)
            if (!separated) {
                val nBoost = if (wide) 4 else 2
                for (i in 0 until nBoost) {
                    val side = if (i < nBoost / 2) -1f else 1f
                    val k = if (nBoost == 4) (0.55f + (i % 2) * 0.55f) else 1.05f
                    val x = cx + side * coreW * (1.15f + k * 0.15f)
                    val bw = coreW * 0.38f
                    fill.color = white
                    canvas.drawRoundRect(x - bw, baseY - s1H * 0.78f, x + bw, baseY, 3f, 3f, fill)
                    tanks(canvas, x, baseY - s1H * 0.78f, baseY, bw, fuelOf(tSec, launch, 1), false, lamp, alpha)
                    stroke.color = strokeC
                    canvas.drawRoundRect(x - bw, baseY - s1H * 0.78f, x + bw, baseY, 3f, 3f, stroke)
                    ogive(canvas, x, baseY - s1H * 0.78f, baseY - s1H * 0.88f, bw, white, strokeC)
                    bell(canvas, x - bw * 0.42f, baseY, bw * 0.48f, h * 0.036f, lamp, alpha, false)
                    bell(canvas, x + bw * 0.42f, baseY, bw * 0.48f, h * 0.036f, lamp, alpha, false)
                    if (tSec >= 0f && tSec < sep) {
                        flame(canvas, x, baseY + h * 0.034f, bw * 1.2f, h * 0.09f, alpha, tSec, "merlin")
                    }
                }
            }
            bell(canvas, cx - coreW * 0.36f, baseY, coreW * 0.28f, h * 0.040f, lamp, alpha, true)
            bell(canvas, cx + coreW * 0.36f, baseY, coreW * 0.28f, h * 0.040f, lamp, alpha, true)
            if (burning(tSec, launch, 1, sep)) {
                flame(canvas, cx, baseY + h * 0.038f, coreW, h * 0.11f, alpha, tSec, "rs25")
            }
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.24f else h * 0.55f
            fill.color = white
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.40f), coreW * 0.72f, white, strokeC)
            bell(canvas, cx, bot, coreW * 0.40f, h * 0.048f, lamp, alpha, true)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot + h * 0.046f, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }

    private fun h3(
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
        launch: LaunchSnapshot?
    ) {
        val white = lampAlpha(Color.parseColor("#E8E4DC"), lamp, alpha)
        val orange = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.078f
        val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.90f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = white
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, coreW, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, stroke)
            fill.color = orange
            canvas.drawRect(cx - coreW, baseY - s1H * 0.22f, cx + coreW, baseY - s1H * 0.18f, fill)
            if (!separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + h * 0.040f)
                    val bw = h * 0.030f
                    srbFrustumOgive(canvas, x, baseY, baseY - s1H * 0.78f, bw, white, strokeC)
                    if (tSec >= 0f && tSec < sep) flame(canvas, x, baseY, bw * 1.6f, h * 0.12f, alpha, tSec, "srb")
                }
            }
            bell(canvas, cx - coreW * 0.32f, baseY, coreW * 0.28f, h * 0.042f, lamp, alpha, true)
            bell(canvas, cx + coreW * 0.32f, baseY, coreW * 0.28f, h * 0.042f, lamp, alpha, true)
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY + h * 0.04f, coreW * 1.4f, h * 0.13f, alpha, tSec, "rs25")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.52f
            fill.color = white
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), coreW * 0.85f, white, strokeC)
            bell(canvas, cx, bot, coreW * 0.40f, h * 0.048f, lamp, alpha, true)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot + h * 0.046f, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }

    private fun lvm3(
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
        launch: LaunchSnapshot?
    ) {
        val white = lampAlpha(Color.parseColor("#E6E4DC"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.072f
        val s1H = if (drawS2 && drawS1) h * 0.52f else if (drawS1) h * 0.88f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = white
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, coreW, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, stroke)
            bell(canvas, cx - coreW * 0.32f, baseY, coreW * 0.26f, h * 0.036f, lamp, alpha, false)
            bell(canvas, cx + coreW * 0.32f, baseY, coreW * 0.26f, h * 0.036f, lamp, alpha, false)
            if (!separated) {
                val bw = h * 0.052f
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + bw + h * 0.006f)
                    srbFrustumOgive(canvas, x, baseY, baseY - s1H * 1.08f, bw, white, strokeC)
                    if (tSec >= 0f && tSec < sep) flame(canvas, x, baseY, bw * 1.7f, h * 0.18f, alpha, tSec, "srb")
                }
            }
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY, coreW * 1.3f, h * 0.12f, alpha, tSec, "merlin")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.24f else h * 0.55f
            fill.color = white
            canvas.drawRoundRect(cx - coreW * 0.78f, bot - s2H, cx + coreW * 0.78f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, coreW * 0.78f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW * 0.78f, bot - s2H, cx + coreW * 0.78f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.22f else h * 0.42f), coreW * 0.90f, white, strokeC)
            bell(canvas, cx, bot, coreW * 0.42f, h * 0.050f, lamp, alpha, true)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot + h * 0.048f, coreW, h * 0.11f, alpha, tSec, "rs25")
        }
    }

    private fun twoEngineCore(
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
        launch: LaunchSnapshot?,
        bodyC: Int,
        methalox: Boolean,
        srb: Boolean,
        flameKind: String
    ) {
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val coreW = h * 0.082f
        val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.90f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = bodyC
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, coreW, fuelOf(tSec, launch, 1), methalox, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, stroke)
            if (srb && !separated) {
                for (side in floatArrayOf(-1f, 1f)) {
                    val x = cx + side * (coreW + h * 0.036f)
                    val bw = h * 0.028f
                    srbFrustumOgive(canvas, x, baseY, baseY - s1H * 0.72f, bw, white, strokeC)
                    if (tSec >= 0f && tSec < sep) flame(canvas, x, baseY, bw * 1.5f, h * 0.10f, alpha, tSec, "srb")
                }
            }
            bell(canvas, cx - coreW * 0.34f, baseY, coreW * 0.30f, h * 0.040f, lamp, alpha, methalox)
            bell(canvas, cx + coreW * 0.34f, baseY, coreW * 0.30f, h * 0.040f, lamp, alpha, methalox)
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY + h * 0.038f, coreW * 1.4f, h * 0.13f, alpha, tSec, flameKind)
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.22f else h * 0.52f
            fill.color = bodyC
            canvas.drawRoundRect(cx - coreW * 0.70f, bot - s2H, cx + coreW * 0.70f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, coreW * 0.70f, fuelOf(tSec, launch, 2), methalox, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - coreW * 0.70f, bot - s2H, cx + coreW * 0.70f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), coreW * 0.70f, bodyC, strokeC)
            bell(canvas, cx, bot, coreW * 0.40f, h * 0.048f, lamp, alpha, true)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot + h * 0.046f, coreW, h * 0.11f, alpha, tSec, flameKind)
        }
    }

    private fun vulcan(
        canvas: Canvas, cx: Float, baseY: Float, h: Float, tSec: Float, stage: Int,
        separated: Boolean, skin: TelemetrySkin.Tokens, lamp: Float, alpha: Float, launch: LaunchSnapshot?
    ) = twoEngineCore(
        canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch,
        lampAlpha(Color.parseColor("#D8DCE2"), lamp, alpha), true, true, "ch4"
    )

    private fun atlas(
        canvas: Canvas, cx: Float, baseY: Float, h: Float, tSec: Float, stage: Int,
        separated: Boolean, skin: TelemetrySkin.Tokens, lamp: Float, alpha: Float, launch: LaunchSnapshot?
    ) = twoEngineCore(
        canvas, cx, baseY, h, tSec, stage, separated, skin, lamp, alpha, launch,
        lampAlpha(Color.parseColor("#C8B8A0"), lamp, alpha), false, true, "merlin"
    )

    private fun firefly(
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
        launch: LaunchSnapshot?
    ) {
        val body = lampAlpha(Color.parseColor("#C4CCD4"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.070f
        val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.90f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = body
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, w, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 3f, 3f, stroke)
            bell(canvas, cx - w * 0.38f, baseY, w * 0.24f, h * 0.032f, lamp, alpha, false)
            bell(canvas, cx + w * 0.38f, baseY, w * 0.24f, h * 0.032f, lamp, alpha, false)
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY, w * 1.5f, h * 0.12f, alpha, tSec, "merlin")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.24f else h * 0.54f
            fill.color = body
            canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, w * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.18f else h * 0.40f), w * 0.72f, body, strokeC)
            bell(canvas, cx, bot, w * 0.40f, h * 0.044f, lamp, alpha, false)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot + h * 0.042f, w, h * 0.10f, alpha, tSec, "merlin")
        }
    }

    private fun proton(
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
        launch: LaunchSnapshot?
    ) {
        val body = lampAlpha(Color.parseColor("#C8C0B0"), lamp, alpha)
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.85f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.090f
        val s1H = if (drawS2 && drawS1) h * 0.54f else if (drawS1) h * 0.88f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.5f
        if (drawS1) {
            fill.color = body
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 3f, 3f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, w, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 3f, 3f, stroke)
            for (k in floatArrayOf(-0.62f, -0.21f, 0.21f, 0.62f)) {
                bell(canvas, cx + w * k, baseY, w * 0.14f, h * 0.028f, lamp, alpha, false)
            }
            if (burning(tSec, launch, 1, sep)) flame(canvas, cx, baseY, w * 1.5f, h * 0.13f, alpha, tSec, "rd107")
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.24f else h * 0.54f
            fill.color = body
            canvas.drawRoundRect(cx - w * 0.62f, bot - s2H, cx + w * 0.62f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, w * 0.62f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w * 0.62f, bot - s2H, cx + w * 0.62f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), w * 0.62f, body, strokeC)
            if (burning(tSec, launch, 2, sep) && !drawS1) flame(canvas, cx, bot, w, h * 0.11f, alpha, tSec, "rd107")
        }
    }

    private fun generic(
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
        val strokeC = lampAlpha(skin.accent, lamp, alpha * 0.9f)
        val sep = FlightProfiles.sepTime(launch)
        val drawS1 = stage == 1
        val drawS2 = stage == 2 || (stage == 1 && !separated)
        val w = h * 0.075f
        val s1H = if (drawS2 && drawS1) h * 0.55f else if (drawS1) h * 0.92f else 0f
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.6f
        if (drawS1) {
            fill.color = body
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, fill)
            tanks(canvas, cx, baseY - s1H, baseY, w, fuelOf(tSec, launch, 1), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, stroke)
        }
        if (drawS2) {
            val bot = if (drawS1) baseY - s1H else baseY
            val s2H = if (drawS1) h * 0.25f else h * 0.58f
            fill.color = body
            canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, fill)
            tanks(canvas, cx, bot - s2H, bot, w * 0.72f, fuelOf(tSec, launch, 2), false, lamp, alpha)
            stroke.color = strokeC
            canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, stroke)
            ogive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), w * 0.72f, body, strokeC)
        }
    }

    private fun plasma(
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
        val hh = (bot - top).coerceAtLeast(8f)
        fill.shader = RadialGradient(
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
        canvas.drawOval(cx - halfW * 3.0f, top - hh * 0.08f, cx + halfW * 3.0f, bot + hh * 0.16f, fill)
        fill.shader = null
        val wake = hh * (0.28f + 0.55f * heat)
        fill.shader = LinearGradient(
            cx, bot, cx, bot + wake,
            intArrayOf(
                Color.argb((a * 0.85f).toInt(), 255, 220, 255),
                Color.argb((a * 0.40f).toInt(), 100, 70, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(cx - halfW * 2.3f, bot - hh * 0.05f, cx + halfW * 2.3f, bot + wake, fill)
        fill.shader = null
        if (fins) {
            val g = (70 + 140 * heat * flick * alpha).toInt().coerceIn(0, 210)
            val fy = top + hh * 0.16f
            fill.shader = RadialGradient(
                cx, fy, halfW * 2.8f,
                intArrayOf(
                    Color.argb(g, 180, 210, 255),
                    Color.argb((g * 0.65f).toInt(), 80, 40, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, fy, halfW * 2.6f, fill)
            fill.shader = null
        }
    }
}
