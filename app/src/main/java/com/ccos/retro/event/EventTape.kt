package com.ccos.retro.event

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.abs

/**
 * Shared TEL event tape for wallpaper and MCC.
 * The tape is the selected launch's event list — not a now-centered time clip.
 * Labels sit ABOVE and BELOW the line so they do not stack.
 * Acronyms on the tape. Full words stay on the catalog / detail.
 * Upcoming marks grow to full size ~2 minutes out. Past marks stay after they fly.
 */
object EventTape {

    const val FULL_READ_SEC = 120f
    /** Past events stay this readable after they have left the 2-minute peak. */
    const val PAST_READ = 0.78f

    fun mark(title: String): String {
        val t = title.uppercase()
        return when {
            "FAIL" in t -> "FAIL"
            "FAIRING" in t || t == "FAIR" -> "FAIR"
            "APPROACHING PAYLOAD" in t || "PAYLOAD DEPLOY" in t ||
                (t.endsWith("DEPLOY") && "DOOR" !in t) -> "DEPLOY"
            "APPROACHING BOOSTER" in t -> "LAND"
            "LANDING BURN" in t || t == "LBURN" -> "LBURN"
            "TOUCHDOWN" in t -> "TDWN"
            "HOT-STAGE" in t || t == "HOT" -> "HOT"
            "BOOSTBACK" in t -> "BB"
            "ENTRY BURN" in t || t == "BOOSTER ENTRY" -> "ENTRY"
            "BOOSTER MECO" in t || t.endsWith("MECO") || t == "MECO" -> "MECO"
            "SECO" in t -> "SECO"
            "SES-1" in t || t == "SES1" -> "SES1"
            ("SEP" in t) && "SECO" !in t -> "SEP"
            "LIFTOFF" in t || "ПУСК" in t -> "L/O"
            "MAX-Q" in t || t == "MAXQ" -> "MAXQ"
            "SHIP CUTOFF" in t -> "SECO"
            "SHIP RELIGHT" in t -> "RESTART"
            "PAYLOAD DOOR" in t -> "DOOR"
            "REENTRY" in t -> "ENTRY"
            "PLASMA" in t -> "PLASMA"
            "FLIP" in t -> "FLIP"
            "SHIP SPLASH" in t || "SHIP LAND" in t || "BOOSTER SPLASH" in t -> "SPLASH"
            "CORE CUTOFF" in t -> "CUT"
            "ORBIT" in t -> "ORBIT"
            else -> t.take(8)
        }
    }

    fun read01(tSec: Float, eventT: Float): Float {
        val until = eventT - tSec
        return when {
            until <= -60f -> PAST_READ
            until <= FULL_READ_SEC -> 1f
            until >= 600f -> 0.32f
            else -> {
                val u = ((until - FULL_READ_SEC) / 480f).coerceIn(0f, 1f)
                1f - 0.68f * u
            }
        }
    }

    /**
     * View of the selected launch's event list. Independent of now.
     * Scroll/zoom (if any) is a slice of this span, never a now-centered 80s gate.
     */
    fun launchSpan(events: List<Pair<Float, String>>): Pair<Float, Float> {
        if (events.isEmpty()) return 0f to 90f
        val first = events.minOf { it.first }
        val last = events.maxOf { it.first }
        var a = first - 30f
        var b = last + 40f
        if (b - a < 90f) b = a + 90f
        return a to b
    }

    fun draw(
        canvas: Canvas,
        events: List<Pair<Float, String>>,
        tSec: Float,
        left: Float,
        top: Float,
        right: Float,
        bot: Float,
        accent: Int,
        go: Int,
        muted: Int,
        text: Int,
        hold: Int,
        danger: Int,
        failed: Boolean,
        textPaint: Paint,
        strokePaint: Paint,
        fillPaint: Paint
    ) {
        if (right - left < 24f || bot - top < 28f || events.isEmpty()) return
        val (win0, win1) = launchSpan(events)
        val span = (win1 - win0).coerceAtLeast(30f)
        val h = bot - top
        val lineY = top + h * 0.50f
        val aboveH = (lineY - top - 4f).coerceAtLeast(10f)
        val belowH = (bot - lineY - 4f).coerceAtLeast(10f)

        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 5f
        strokePaint.color = muted
        canvas.drawLine(left, lineY, right, lineY, strokePaint)
        val nowX = left + ((tSec - win0).coerceIn(0f, span) / span) * (right - left)
        strokePaint.color = go
        canvas.drawLine(left, lineY, nowX, lineY, strokePaint)
        fillPaint.style = Paint.Style.FILL
        fillPaint.shader = null
        fillPaint.color = go
        canvas.drawCircle(nowX, lineY, 7f, fillPaint)

        val visible = events.sortedBy { it.first }
        val minGap = (right - left) * minOf(0.12f, 0.70f / maxOf(visible.size, 2).toFloat())
        val placed = ArrayList<Pair<Float, Boolean>>()
        val clip = canvas.save()
        canvas.clipRect(left, top, right, bot)
        visible.forEachIndexed { i, (et, title) ->
            val x = (left + ((et - win0) / span) * (right - left)).coerceIn(left + 10f, right - 10f)
            val above = i % 2 == 0
            if (placed.any { it.second == above && abs(it.first - x) < minGap }) return@forEachIndexed
            placed.add(x to above)
            val mark = mark(title)
            val read = read01(tSec, et)
            val done = tSec >= et
            val close = abs(et - tSec) <= FULL_READ_SEC
            fillPaint.color = when {
                failed && close -> danger
                close -> hold
                done -> go
                else -> accent
            }
            canvas.drawCircle(x, lineY, if (close) 8f else 5.5f, fillPaint)
            val slot = ((right - left) * (0.14f + 0.16f * read)).coerceAtLeast(28f)
            val size = (if (above) aboveH else belowH) * (0.42f + 0.58f * read)
            textPaint.style = Paint.Style.FILL
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.isFakeBoldText = true
            textPaint.textSize = size.coerceAtLeast(8f)
            val tw = textPaint.measureText(mark)
            if (tw > slot && textPaint.textSize > 8f) {
                textPaint.textSize = (textPaint.textSize * slot / tw).coerceAtLeast(8f)
            }
            val a = (80f + 175f * read).toInt().coerceIn(70, 255)
            val col = when {
                failed && close -> danger
                close -> hold
                else -> text
            }
            textPaint.color = (col and 0x00FFFFFF) or (a shl 24)
            val tx = x.coerceIn(left + slot * 0.45f, right - slot * 0.45f)
            val baseline = if (above) {
                lineY - 8f - textPaint.textSize * 0.15f
            } else {
                lineY + 8f + textPaint.textSize * 0.85f
            }
            canvas.drawText(mark, tx, baseline, textPaint)
        }
        canvas.restoreToCount(clip)
    }
}
