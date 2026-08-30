package com.ccos.retro.engine

import android.graphics.*
import com.ccos.retro.data.SystemMetricsProvider
import com.ccos.retro.skin.SystemSkin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure-vector System Metrics HUD.
 * Visual target: Flutter System Metrics screen + technical-data wallpapers + McIntosh VU feel.
 */
class SystemMetricsRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val arcRect = RectF()
    private val timeFmt12 = SimpleDateFormat("hh:mm", Locale.US)
    private val timeFmt24 = SimpleDateFormat("HH:mm", Locale.US)
    private val secFmt = SimpleDateFormat("ss", Locale.US)
    private val ampmFmt = SimpleDateFormat("a", Locale.US)
    private val dateFmt = SimpleDateFormat("EEE. MM/dd", Locale.US)

    /** Live text scale from Settings (shared by all layout helpers) */
    private var textScaleMul = 1.15f
    private fun ts(base: Float): Float = base * textScaleMul

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        snap: SystemMetricsProvider.Snapshot?,
        theme: Int,
        analog: Boolean,
        lamp: Float,
        pageHint: String,
        textScale: Float = 1.15f,
        is24Hour: Boolean = true
    ) {
        textScaleMul = textScale

        val p = SystemSkin.palette(theme, lamp)
        val now = Date()

        // Full opaque fill — exclusive system module surface
        canvas.drawColor(Color.parseColor("#05070A"))

        // Dense technical grid
        stroke.color = p.grid
        stroke.strokeWidth = 1f
        val step = 32f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), stroke)
            x += step
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, stroke)
            y += step
        }
        // Radar-style arcs (HUD elements set)
        stroke.color = Color.argb(40, Color.red(p.primary), Color.green(p.primary), Color.blue(p.primary))
        stroke.strokeWidth = 1.2f
        for (r in listOf(80f, 140f, 220f, 320f)) {
            canvas.drawCircle(width * 0.5f, height * 0.55f, r, stroke)
        }
        // Sweep line
        stroke.color = Color.argb(55, Color.red(p.accent), Color.green(p.accent), Color.blue(p.accent))
        canvas.drawLine(width * 0.5f, height * 0.55f, width * 0.85f, height * 0.35f, stroke)
        // Side waveform strip
        stroke.color = Color.argb(50, Color.red(p.primary), Color.green(p.primary), Color.blue(p.primary))
        val wx = width * 0.08f
        var prevY = height * 0.7f
        for (i in 0..24) {
            val nx = wx + i * 6f
            val ny = height * 0.7f + kotlin.math.sin(i * 0.55f).toFloat() * 18f
            canvas.drawLine(wx + (i - 1).coerceAtLeast(0) * 6f, prevY, nx, ny, stroke)
            prevY = ny
        }
        // Corner brackets
        stroke.color = p.primary
        stroke.strokeWidth = 4.5f
        val m = 14f
        val b = 28f
        canvas.drawLine(m, m, m + b, m, stroke)
        canvas.drawLine(m, m, m, m + b, stroke)
        canvas.drawLine(width - m, m, width - m - b, m, stroke)
        canvas.drawLine(width - m, m, width - m, m + b, stroke)
        canvas.drawLine(m, height - m, m + b, height - m, stroke)
        canvas.drawLine(m, height - m, m, height - m - b, stroke)
        canvas.drawLine(width - m, height - m, width - m - b, height - m, stroke)
        canvas.drawLine(width - m, height - m, width - m, height - m - b, stroke)

        val cx = width / 2f
        // Layout priority: CLOCK first (large) → date → charge → mode → gauges
        // Title "SYSTEM METRICS" moved to bottom to free top space
        // Clear status bar / notch — TIME + DATE only (mode/charge live in CMD)
        var top = height * 0.12f

        // CLOCK — primary
        text.color = Color.argb(255, Color.red(p.primary), Color.green(p.primary), Color.blue(p.primary))
        text.textAlign = Paint.Align.CENTER
        val timeStr = if (is24Hour) timeFmt24.format(now) else timeFmt12.format(now)
        val clockSize = ts(100f)
        text.textSize = clockSize
        // Baseline for clock; advance by ~1.15× glyph height so date never collides at max scale
        val clockBaseline = top + clockSize * 0.78f
        canvas.drawText(timeStr, cx, clockBaseline, text)
        top = clockBaseline + clockSize * 0.42f

        // Date under clock
        val dateSize = ts(28f)
        text.textSize = dateSize
        text.color = p.text
        canvas.drawText(dateFmt.format(now).uppercase(Locale.US), cx, top + dateSize * 0.85f, text)
        top += dateSize * 1.55f

        // Optional subtle seconds / am-pm only (no charge, no mode chips)
        val secSize = ts(16f)
        text.textSize = secSize
        text.color = p.dim
        val sub = if (is24Hour) secFmt.format(now) else "${ampmFmt.format(now)}  ${secFmt.format(now)}"
        canvas.drawText(sub, cx, top + secSize * 0.85f, text)
        top += secSize * 1.6f

        val contentTop = top
        // More vertical room for gauges
        val contentH = height * 0.93f - contentTop - height * 0.05f

        if (analog) {
            drawAnalogLayout(canvas, width, contentTop, contentH, snap, p)
        } else {
            drawDigitalLayout(canvas, width, contentTop, contentH, snap, p)
        }

        // Small title at bottom + page hint
        text.textSize = ts(18f)
        text.color = p.dim
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("SYSTEM METRICS", cx, height * 0.935f, text)
        text.textSize = ts(16f)
        canvas.drawText(pageHint, cx, height * 0.965f, text)
    }

    private fun drawChip(canvas: Canvas, cx: Float, cy: Float, label: String, on: Boolean, p: SystemSkin.Palette) {
        val w = 100f
        val h = 36f
        val r = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        stroke.color = if (on) p.primary else p.dim
        stroke.strokeWidth = if (on) 4.5f else 3f
        canvas.drawRoundRect(r, 18f, 18f, stroke)
        if (on) {
            fill.color = Color.argb(40, Color.red(p.primary), Color.green(p.primary), Color.blue(p.primary))
            canvas.drawRoundRect(r, 18f, 18f, fill)
        }
        text.color = if (on) p.primary else p.text
        text.textSize = ts(18f)
        canvas.drawText(label, cx, cy + 6f, text)
    }

    private fun drawAnalogLayout(
        canvas: Canvas,
        width: Int,
        top: Float,
        h: Float,
        snap: SystemMetricsProvider.Snapshot?,
        p: SystemSkin.Palette
    ) {
        val bat = (snap?.batteryPercent ?: 50) / 100f
        val cpu = (snap?.cpuPercent ?: 0) / 100f
        val memFrac = if (snap != null && snap.maxMemMb > 0)
            snap.usedMemMb.toFloat() / snap.maxMemMb else 0.4f
        val intFrac = snap?.internalStorageFrac ?: 0.25f
        val extFrac = snap?.externalStorageFrac ?: 0f
        val cx = width / 2f

        // Dynamic regions: top 42% battery, mid 32% CPU/RAM pair, bottom 26% storage
        val batRegionH = h * 0.38f
        val midRegionH = h * 0.34f
        val botRegionH = h * 0.26f
        val batCy = top + batRegionH * 0.50f
        val dens = 1.5f // relative; width/height already encode resolution
        val batR = min(width * 0.30f, batRegionH * 0.42f).coerceIn(70f, 140f)
        drawVuGauge(canvas, cx, batCy, batR, bat, "BATTERY", snap?.isCharging == true, p)

        val midTop = top + batRegionH
        val midCy = midTop + midRegionH * 0.48f
        val midR = min(width * 0.20f, midRegionH * 0.48f).coerceIn(55f, 100f)
        drawVuGauge(canvas, width * 0.30f, midCy, midR, cpu, "CPU", false, p)
        drawVuGauge(canvas, width * 0.70f, midCy, midR, memFrac, "RAM", false, p)

        val botTop = midTop + midRegionH
        val storCy = botTop + botRegionH * 0.55f
        val storR = min(width * 0.10f, botRegionH * 0.35f).coerceIn(28f, 48f)
        text.color = p.primary
        text.textSize = ts(18f)
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("STORAGE", cx, botTop + botRegionH * 0.12f, text)
        drawStorageRing(canvas, width * 0.32f, storCy, storR, intFrac, "INTERNAL", p)
        drawStorageRing(canvas, width * 0.68f, storCy, storR, extFrac, "EXTERNAL", p)
    }

    private fun drawDigitalLayout(
        canvas: Canvas,
        width: Int,
        top: Float,
        h: Float,
        snap: SystemMetricsProvider.Snapshot?,
        p: SystemSkin.Palette
    ) {
        val bat = snap?.batteryPercent ?: 50
        val cpu = snap?.cpuPercent ?: 0
        val used = snap?.usedMemMb ?: 0
        val max = snap?.maxMemMb ?: 1
        val cx = width / 2f
        // Content inset from side buttons (~18% each side)
        val left = width * 0.20f
        val usable = width * 0.60f
        // Spread 4 slots across full content height
        val slotH = h * 0.24f

        text.typeface = Typeface.MONOSPACE
        text.textAlign = Paint.Align.LEFT

        fun digRow(slot: Int, label: String, value: String, unit: String, bar: Float?) {
            val y0 = top + slot * slotH
            text.color = p.dim
            text.textSize = ts(16f)
            canvas.drawText(label, left, y0 + slotH * 0.18f, text)
            text.color = Color.argb(255, Color.red(p.primary), Color.green(p.primary), Color.blue(p.primary))
            text.textSize = ts((slotH * 0.48f).coerceIn(32f, 64f))
            canvas.drawText(value, left, y0 + slotH * 0.55f, text)
            text.textSize = ts(16f)
            text.color = p.dim
            canvas.drawText(unit, left + usable * 0.62f, y0 + slotH * 0.40f, text)
            if (bar != null) {
                val ty = y0 + slotH * 0.70f
                drawSegmentBar(canvas, left, ty, usable * 0.92f, bar, p)
            }
        }

        digRow(0, "BATTERY", "%03d".format(bat), if (snap?.isCharging == true) "CHG" else "BAT", bat / 100f)
        digRow(1, "CPU", "%03d".format(cpu), "PCT", cpu / 100f)
        val memFrac = if (max > 0) used.toFloat() / max else 0f
        digRow(2, "MEMORY", "$used/$max", "MB", memFrac)

        // Meta + storage in bottom region
        val yMeta = top + 3 * slotH
        text.textSize = ts(13f)
        text.color = p.dim
        canvas.drawText("FRM ${"%.0f".format(snap?.frameTimeMs ?: 0f)}ms  THR ${snap?.activeThreads ?: 0}",
            left, yMeta + slotH * 0.25f, text)

        val intFrac = snap?.internalStorageFrac ?: 0.25f
        val usedGb = snap?.internalUsedGb ?: 0f
        val totalGb = snap?.internalTotalGb ?: 0f
        text.textAlign = Paint.Align.CENTER
        text.color = p.primary
        text.textSize = ts(14f)
        canvas.drawText("STORAGE", cx, yMeta + slotH * 0.42f, text)
        text.textSize = ts(22f)
        text.color = p.good
        val gbLabel = if (totalGb > 0f) "%.0f / %.0f GB".format(usedGb, totalGb) else "%d%%".format((intFrac * 100).toInt())
        canvas.drawText(gbLabel, cx, yMeta + slotH * 0.68f, text)
        // Segmented used bar
        val barW = usable * 0.90f
        val barL = left + usable * 0.05f
        drawSegmentBar(canvas, barL, yMeta + slotH * 0.78f, barW, intFrac, p)
    }

    private fun drawSegmentBar(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        value: Float,
        p: SystemSkin.Palette
    ) {
        val segs = 16
        val gap = 3f
        val segW = (width - gap * (segs - 1)) / segs
        val h = 16f
        val v = value.coerceIn(0f, 1f)
        val lit = (v * segs).toInt()
        for (i in 0 until segs) {
            val x = left + i * (segW + gap)
            fill.color = when {
                i < lit && i >= segs * 0.8f -> p.warn
                i < lit && i >= segs * 0.55f -> p.accent
                i < lit -> p.good
                else -> p.dim
            }
            canvas.drawRoundRect(x, top, x + segW, top + h, 2f, 2f, fill)
        }
    }

    private fun drawVuGauge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        value: Float,
        label: String,
        charging: Boolean,
        p: SystemSkin.Palette
    ) {
        val v = value.coerceIn(0f, 1f)
        val strokeW = 18f

        stroke.strokeCap = Paint.Cap.BUTT
        stroke.strokeWidth = (radius * 0.045f).coerceIn(3f, 6f)
        stroke.color = p.primary
        canvas.drawCircle(cx, cy, radius + 10f, stroke)
        stroke.strokeWidth = 2.2f
        stroke.color = p.dim
        canvas.drawCircle(cx, cy, radius - strokeW * 0.55f, stroke)

        stroke.strokeWidth = strokeW
        stroke.color = p.dim
        stroke.strokeCap = Paint.Cap.ROUND
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, 140f, 260f, false, stroke)

        stroke.color = when {
            v > 0.7f -> p.good
            v > 0.3f -> p.accent
            else -> p.warn
        }
        canvas.drawArc(arcRect, 140f, 260f * v, false, stroke)
        stroke.strokeCap = Paint.Cap.BUTT

        for (i in 0..20) {
            val ang = Math.toRadians((140 + 13 * i).toDouble())
            val major = i % 2 == 0
            stroke.strokeWidth = if (major) 3.6f else 2.2f
            stroke.color = if (major) p.text else p.dim
            val r0 = radius - if (major) 22f else 14f
            val r1 = radius - 6f
            canvas.drawLine(
                (cx + cos(ang) * r0).toFloat(), (cy + sin(ang) * r0).toFloat(),
                (cx + cos(ang) * r1).toFloat(), (cy + sin(ang) * r1).toFloat(),
                stroke
            )
        }

        val needleAng = Math.toRadians((140 + 260 * v).toDouble())
        stroke.color = Color.argb(80, 255, 255, 255)
        stroke.strokeWidth = 2f
        canvas.drawLine(
            cx, cy,
            (cx - cos(needleAng) * 16f).toFloat(),
            (cy - sin(needleAng) * 16f).toFloat(),
            stroke
        )
        stroke.color = p.primary
        stroke.strokeWidth = 6.5f
        canvas.drawLine(
            cx, cy,
            (cx + cos(needleAng) * (radius - 22f)).toFloat(),
            (cy + sin(needleAng) * (radius - 22f)).toFloat(),
            stroke
        )
        fill.color = Color.parseColor("#1A1A1A")
        canvas.drawCircle(cx, cy, 11f, fill)
        fill.color = p.primary
        canvas.drawCircle(cx, cy, 6f, fill)

        text.color = p.good
        text.textSize = ts(34f)
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("${(v * 100).toInt()}%", cx, cy + radius + ts(22f), text)
        text.textSize = ts(20f)
        text.color = p.text
        canvas.drawText(label, cx, cy + radius + ts(44f), text)
        if (charging) {
            text.color = p.good
            text.textSize = ts(18f)
            canvas.drawText("CHARGING", cx, cy + radius + ts(64f), text)
        }
    }

    private fun drawBarGauge(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        value: Float,
        label: String,
        p: SystemSkin.Palette
    ) {
        val h = 28f
        val v = value.coerceIn(0f, 1f)
        // Trough
        fill.color = p.dim
        canvas.drawRoundRect(left, top, left + width, top + h, 6f, 6f, fill)
        // Fill gradient-ish solid
        fill.color = when {
            v > 0.75f -> p.warn
            v > 0.4f -> p.accent
            else -> p.good
        }
        canvas.drawRoundRect(left, top, left + width * v, top + h, 6f, 6f, fill)
        // Needle mark
        stroke.color = Color.WHITE
        stroke.strokeWidth = 3f
        val nx = left + width * v
        canvas.drawLine(nx, top - 4f, nx, top + h + 4f, stroke)
        text.color = p.text
        text.textSize = ts(18f)
        text.textAlign = Paint.Align.LEFT
        canvas.drawText("$label  ${(v * 100).toInt()}%", left, top - 8f, text)
    }

    private fun drawStorageRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        used: Float,
        label: String,
        p: SystemSkin.Palette
    ) {
        stroke.strokeWidth = 14f
        stroke.color = p.dim
        canvas.drawCircle(cx, cy, radius, stroke)
        stroke.color = p.accent
        stroke.strokeCap = Paint.Cap.ROUND
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, -90f, 360f * used.coerceIn(0f, 1f), false, stroke)
        stroke.strokeCap = Paint.Cap.BUTT
        text.color = p.primary
        text.textSize = ts(22f)
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("${(used * 100).toInt()}%", cx, cy + 8f, text)
        text.textSize = ts(18f)
        text.color = p.text
        canvas.drawText(label, cx, cy + radius + 26f, text)
    }

    private fun drawCpuHistory(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        level: Float,
        p: SystemSkin.Palette
    ) {
        val bars = 16
        val gap = 4f
        val bw = (width - gap * (bars - 1)) / bars
        fill.color = p.dim
        for (i in 0 until bars) {
            // Pseudo history tapering toward current
            val t = (i + 1f) / bars
            val hFrac = (0.15f + 0.85f * level * t * (0.6f + 0.4f * kotlin.math.sin(i * 1.7f)))
                .coerceIn(0.08f, 1f)
            val bh = height * hFrac
            val x = left + i * (bw + gap)
            fill.color = if (hFrac > 0.7f) p.warn else p.primary
            canvas.drawRect(x, top + height - bh, x + bw, top + height, fill)
        }
        text.color = p.text
        text.textSize = ts(14f)
        text.textAlign = Paint.Align.LEFT
        canvas.drawText("CPU HISTORY", left, top - 6f, text)
    }
}
