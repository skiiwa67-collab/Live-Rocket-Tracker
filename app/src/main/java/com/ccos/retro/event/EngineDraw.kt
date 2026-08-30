package com.ccos.retro.event

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Shared engine-bell plate. Wallpaper analog ENG and MCC both call [draw]
 * so Falcon Heavy cannot become one 27-ring and Super Heavy cannot become Merlin.
 */
object EngineDraw {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun drawEnginePattern(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        launch: com.ccos.retro.data.LaunchSnapshot?,
        stage: Int,
        lit: Int,
        skin: com.ccos.retro.skin.TelemetrySkin.Tokens
    ) {
        val pattern = VehicleCatalog.enginePattern(launch, stage)
        draw(canvas, cx, cy, size * 0.5f, pattern, lit)
    }

    fun draw(canvas: Canvas, cx: Float, cy: Float, half: Float, pattern: EnginePattern, lit: Int) {
        if (half < 4f) return
        fill.shader = null
        stroke.shader = null
        canvas.save()
        canvas.clipRect(cx - half, cy - half, cx + half, cy + half)
        when (pattern) {
            EnginePattern.MERLIN9 -> octaweb(canvas, cx, cy, half * 0.92f, lit, faceted = false)
            EnginePattern.ELECTRON9 -> octaweb(canvas, cx, cy, half * 0.88f, lit, faceted = true)
            EnginePattern.MERLIN27 -> {
                val r = half * 0.30f
                val dx = half * 0.64f
                octaweb(canvas, cx - dx, cy, r, lit.coerceAtMost(9), faceted = false)
                octaweb(canvas, cx, cy, r, (lit - 9).coerceIn(0, 9), faceted = false)
                octaweb(canvas, cx + dx, cy, r, (lit - 18).coerceIn(0, 9), faceted = false)
            }
            EnginePattern.RAPTOR33 -> raptor33(canvas, cx, cy, half, lit)
            EnginePattern.RAPTOR6 -> raptor6(canvas, cx, cy, half, lit)
            EnginePattern.RS25_4 -> rs25(canvas, cx, cy, half, lit)
            EnginePattern.BE4_7, EnginePattern.GLENN7 -> glenn(canvas, cx, cy, half * 0.88f, lit)
            EnginePattern.KOROLEV -> korolev(canvas, cx, cy, half, lit)
            EnginePattern.KOROLEV_UPPER, EnginePattern.CHAMBER4 -> {
                plateCircle(canvas, cx, cy, half * 0.72f)
                chamber4(canvas, cx, cy, half * 0.28f, half * 0.22f, 0, lit)
            }
            EnginePattern.ARIANE -> ariane(canvas, cx, cy, half, lit)
            EnginePattern.LM5 -> lm5(canvas, cx, cy, half, lit)
            EnginePattern.LE9_2, EnginePattern.BE4_2, EnginePattern.H3_2, EnginePattern.VIKAS2, EnginePattern.RD180_2 -> h3(canvas, cx, cy, half, lit)
            EnginePattern.MERLIN_VAC, EnginePattern.RL10, EnginePattern.VACUUM1 -> {
                plateCircle(canvas, cx, cy, half * 0.72f)
                nozzle(canvas, cx, cy, half * 0.62f, lit > 0, vacuum = true, faceted = false)
            }
            EnginePattern.REAVER4 -> rs25(canvas, cx, cy, half, lit)
            EnginePattern.PROTON6, EnginePattern.SOLIDS, EnginePattern.UNKNOWN -> plateCircle(canvas, cx, cy, half * 0.72f)
        }
        canvas.restore()
        fill.shader = null
    }

    private fun octaweb(canvas: Canvas, cx: Float, cy: Float, r: Float, lit: Int, faceted: Boolean) {
        octagonPlate(canvas, cx, cy, r * 1.02f)
        val bell = (r * 0.22f).coerceAtLeast(2f)
        val ring = r * 0.62f
        nozzle(canvas, cx, cy, bell, 0 < lit, faceted = faceted)
        for (i in 0 until 8) {
            val a = Math.toRadians(-90.0 + i * 45.0)
            nozzle(
                canvas,
                cx + (ring * cos(a)).toFloat(),
                cy + (ring * sin(a)).toFloat(),
                bell,
                (i + 1) < lit,
                faceted = faceted
            )
        }
    }

    private fun raptor33(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        plateCircle(canvas, cx, cy, half * 0.96f)
        var idx = 0
        fun ring(n: Int, rad: Float, bell: Float) {
            for (i in 0 until n) {
                val a = Math.toRadians(-90.0 + i * (360.0 / n))
                nozzle(
                    canvas,
                    cx + (rad * cos(a)).toFloat(),
                    cy + (rad * sin(a)).toFloat(),
                    bell,
                    idx < lit
                )
                idx++
            }
        }
        ring(3, half * 0.18f, (half * 0.085f).coerceAtLeast(2f))
        ring(10, half * 0.46f, (half * 0.072f).coerceAtLeast(2f))
        ring(20, half * 0.78f, (half * 0.062f).coerceAtLeast(1.8f))
    }

    private fun raptor6(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        plateCircle(canvas, cx, cy, half * 0.78f)
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            nozzle(
                canvas,
                cx + (half * 0.22f * cos(a)).toFloat(),
                cy + (half * 0.22f * sin(a)).toFloat(),
                half * 0.16f,
                i < lit
            )
            nozzle(
                canvas,
                cx + (half * 0.52f * cos(a + Math.PI / 3.0)).toFloat(),
                cy + (half * 0.52f * sin(a + Math.PI / 3.0)).toFloat(),
                half * 0.24f,
                (i + 3) < lit,
                vacuum = true
            )
        }
    }

    private fun rs25(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        val plate = half * 0.78f
        fill.shader = null
        fill.color = Color.parseColor("#14161C")
        fill.style = Paint.Style.FILL
        canvas.drawRoundRect(cx - plate, cy - plate, cx + plate, cy + plate, 8f, 8f, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2f
        stroke.color = Color.parseColor("#3A4048")
        canvas.drawRoundRect(cx - plate, cy - plate, cx + plate, cy + plate, 8f, 8f, stroke)
        val d = half * 0.34f
        val bell = half * 0.22f
        val pts = arrayOf(-d to -d, d to -d, -d to d, d to d)
        pts.forEachIndexed { i, (ox, oy) ->
            nozzle(canvas, cx + ox, cy + oy, bell, i < lit)
        }
    }

    private fun glenn(canvas: Canvas, cx: Float, cy: Float, r: Float, lit: Int) {
        plateCircle(canvas, cx, cy, r * 1.02f)
        val bell = r * 0.20f
        val ring = r * 0.60f
        nozzle(canvas, cx, cy, bell * 1.08f, 0 < lit)
        for (i in 0 until 6) {
            val a = Math.toRadians(-90.0 + i * 60.0)
            nozzle(
                canvas,
                cx + (ring * cos(a)).toFloat(),
                cy + (ring * sin(a)).toFloat(),
                bell,
                (i + 1) < lit
            )
        }
    }

    private fun korolev(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        val arm = half * 0.22f
        val len = half * 0.92f
        fill.shader = null
        fill.style = Paint.Style.FILL
        fill.color = Color.parseColor("#16181E")
        canvas.drawRoundRect(cx - arm, cy - len, cx + arm, cy + len, 6f, 6f, fill)
        canvas.drawRoundRect(cx - len, cy - arm, cx + len, cy + arm, 6f, 6f, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2f
        stroke.color = Color.parseColor("#3A4048")
        canvas.drawRoundRect(cx - arm, cy - len, cx + arm, cy + len, 6f, 6f, stroke)
        canvas.drawRoundRect(cx - len, cy - arm, cx + len, cy + arm, 6f, 6f, stroke)
        val spread = half * 0.12f
        val bell = half * 0.10f
        val reach = half * 0.62f
        chamber4(canvas, cx, cy, spread, bell, 0, lit)
        chamber4(canvas, cx, cy - reach, spread, bell, 4, lit)
        chamber4(canvas, cx + reach, cy, spread, bell, 8, lit)
        chamber4(canvas, cx, cy + reach, spread, bell, 12, lit)
        chamber4(canvas, cx - reach, cy, spread, bell, 16, lit)
    }

    private fun chamber4(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        spread: Float,
        bell: Float,
        litStart: Int,
        lit: Int
    ) {
        val offs = arrayOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)
        offs.forEachIndexed { i, (ox, oy) ->
            nozzle(canvas, cx + ox * spread, cy + oy * spread, bell, (litStart + i) < lit)
        }
    }

    private fun ariane(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        plateCircle(canvas, cx, cy, half * 0.42f)
        nozzle(canvas, cx, cy, half * 0.28f, 0 < lit)
        nozzle(canvas, cx - half * 0.62f, cy, half * 0.22f, 1 < lit)
        nozzle(canvas, cx + half * 0.62f, cy, half * 0.22f, 2 < lit)
    }

    private fun lm5(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        plateCircle(canvas, cx, cy, half * 0.88f)
        nozzle(canvas, cx - half * 0.16f, cy, half * 0.20f, 0 < lit)
        nozzle(canvas, cx + half * 0.16f, cy, half * 0.20f, 1 < lit)
        val br = half * 0.62f
        val bell = half * 0.14f
        for (i in 0 until 4) {
            val a = Math.toRadians(-45.0 + i * 90.0)
            nozzle(
                canvas,
                cx + (br * cos(a)).toFloat(),
                cy + (br * sin(a)).toFloat(),
                bell,
                (i + 2) < lit
            )
        }
    }

    private fun h3(canvas: Canvas, cx: Float, cy: Float, half: Float, lit: Int) {
        plateCircle(canvas, cx, cy, half * 0.72f)
        val d = half * 0.28f
        nozzle(canvas, cx - d, cy, half * 0.26f, 0 < lit)
        nozzle(canvas, cx + d, cy, half * 0.26f, 1 < lit)
    }

    private fun plateCircle(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        fill.shader = null
        fill.style = Paint.Style.FILL
        fill.color = Color.parseColor("#12141A")
        canvas.drawCircle(cx, cy, r, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = (r * 0.03f).coerceIn(1.2f, 3f)
        stroke.color = Color.parseColor("#2A3038")
        canvas.drawCircle(cx, cy, r, stroke)
    }

    private fun octagonPlate(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        for (i in 0..7) {
            val a = Math.toRadians(-22.5 + i * 45.0)
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fill.shader = null
        fill.style = Paint.Style.FILL
        fill.color = Color.parseColor("#101218")
        canvas.drawPath(path, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2f
        stroke.color = Color.parseColor("#2C323A")
        canvas.drawPath(path, stroke)
    }

    private fun nozzle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        bell: Float,
        lit: Boolean,
        vacuum: Boolean = false,
        faceted: Boolean = false
    ) {
        if (bell < 0.8f) return
        val rx = if (vacuum) bell * 1.18f else bell
        val ry = if (vacuum) bell * 1.38f else bell * 0.92f
        fill.shader = null
        fill.style = Paint.Style.FILL
        if (lit) {
            fill.color = Color.argb(75, 255, 130, 30)
            canvas.drawOval(cx - rx * 1.55f, cy - ry * 1.55f, cx + rx * 1.55f, cy + ry * 1.55f, fill)
        }
        val metalLight = Color.parseColor("#7A8088")
        val metalDark = Color.parseColor("#1A1C22")
        fill.shader = RadialGradient(
            cx - rx * 0.25f, cy - ry * 0.30f, max(rx, ry) * 1.45f,
            metalLight, metalDark, Shader.TileMode.CLAMP
        )
        if (faceted) {
            path.reset()
            val sides = 6
            for (i in 0 until sides) {
                val a = Math.toRadians(-90.0 + i * (360.0 / sides))
                val x = cx + (rx * cos(a)).toFloat()
                val y = cy + (ry * sin(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, fill)
        } else {
            canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, fill)
        }
        fill.shader = null
        fill.color = Color.parseColor("#08090C")
        canvas.drawOval(cx - rx * 0.40f, cy - ry * 0.40f, cx + rx * 0.40f, cy + ry * 0.40f, fill)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = (bell * 0.10f).coerceIn(1f, 3.4f)
        stroke.color = Color.parseColor("#C0C6CE")
        canvas.drawArc(cx - rx, cy - ry, cx + rx, cy + ry, 205f, 75f, false, stroke)
        stroke.strokeWidth = 1.1f
        stroke.color = Color.parseColor("#3A4048")
        if (faceted) canvas.drawPath(path, stroke)
        else canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, stroke)
        if (lit) {
            fill.color = Color.argb(230, 255, 150, 40)
            canvas.drawOval(cx - rx * 0.30f, cy - ry * 0.30f, cx + rx * 0.30f, cy + ry * 0.30f, fill)
            fill.color = Color.argb(245, 255, 240, 200)
            canvas.drawCircle(cx, cy, bell * 0.13f, fill)
        }
    }
}
