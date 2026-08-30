package com.ccos.retro.event

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * One shared nose/body primitive for wallpaper and MCC.
 * Real public outline pieces only. No circle noses. No dual-peak noses.
 * Ogive control Y is a lerp of base to tip, never (base+tip)*k.
 */
object VehicleOutline {

    fun ogive(
        canvas: Canvas,
        cx: Float,
        base: Float,
        tip: Float,
        halfW: Float,
        fillPaint: Paint,
        strokePaint: Paint,
        fill: Int,
        stroke: Int,
        fullness: Float = 0.42f
    ) {
        if (halfW < 0.8f) return
        val join = maxOf(base, tip)
        val nose = minOf(base, tip)
        val ctrl = join + (nose - join) * fullness.coerceIn(0.18f, 0.72f)
        val p = Path()
        p.moveTo(cx - halfW, join)
        p.quadTo(cx - halfW * 0.55f, ctrl, cx, nose)
        p.quadTo(cx + halfW * 0.55f, ctrl, cx + halfW, join)
        p.close()
        fillPaint.shader = null
        fillPaint.color = fill
        canvas.drawPath(p, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.6f
        strokePaint.color = stroke
        canvas.drawPath(p, strokePaint)
    }

    fun frustum(
        canvas: Canvas,
        cx: Float,
        base: Float,
        top: Float,
        halfWBase: Float,
        halfWTop: Float,
        fillPaint: Paint,
        strokePaint: Paint,
        fill: Int,
        stroke: Int
    ) {
        if (halfWBase < 0.8f) return
        val p = Path()
        p.moveTo(cx - halfWBase, base)
        p.lineTo(cx + halfWBase, base)
        p.lineTo(cx + halfWTop, top)
        p.lineTo(cx - halfWTop, top)
        p.close()
        fillPaint.shader = null
        fillPaint.color = fill
        canvas.drawPath(p, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        strokePaint.color = stroke
        canvas.drawPath(p, strokePaint)
    }

    /** Capsule / oval-ended tank. SLS core is a cylinder with domed ends, not a box. */
    fun capsule(
        canvas: Canvas,
        cx: Float,
        top: Float,
        bot: Float,
        halfW: Float,
        fillPaint: Paint,
        strokePaint: Paint,
        fill: Int,
        stroke: Int
    ) {
        if (halfW < 0.8f || bot - top < 2f) return
        val rr = RectF(cx - halfW, top, cx + halfW, bot)
        fillPaint.shader = null
        fillPaint.color = fill
        canvas.drawRoundRect(rr, halfW, halfW, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f
        strokePaint.color = stroke
        canvas.drawRoundRect(rr, halfW, halfW, strokePaint)
    }

    /**
     * Shuttle-derived five-segment SRB forward end: frustum then ogive cap.
     * Not a needle. Not a pin. Used by SLS and other published solid noses.
     */
    fun srbForward(
        canvas: Canvas,
        cx: Float,
        caseTop: Float,
        caseHalfW: Float,
        fillPaint: Paint,
        strokePaint: Paint,
        fill: Int,
        stroke: Int
    ) {
        val frustumH = caseHalfW * 1.05f
        val ogiveH = caseHalfW * 0.78f
        val frustumTop = caseTop - frustumH
        val tipHalf = caseHalfW * 0.58f
        frustum(canvas, cx, caseTop, frustumTop, caseHalfW, tipHalf, fillPaint, strokePaint, fill, stroke)
        ogive(canvas, cx, frustumTop, frustumTop - ogiveH, tipHalf, fillPaint, strokePaint, fill, stroke, 0.38f)
    }
}
