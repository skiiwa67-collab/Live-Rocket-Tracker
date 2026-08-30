package com.ccos.retro.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ccos.retro.event.EventSeverity
import com.ccos.retro.event.FlightEvent

/**
 * Non-intrusive MCC alert. Does not steal the console or zoom video.
 * FAIL stays until tapped. INFO / WATCH auto-clear.
 */
class EventBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val titleView: TextView
    private val detailView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val hide = Runnable { collapse() }
    private var sticky = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        val d = resources.displayMetrics.density
        setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
        visibility = GONE
        titleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        detailView = TextView(context).apply {
            setTextColor(0xFFD0D8E0.toInt())
            textSize = 12f
            maxLines = 2
        }
        addView(titleView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(detailView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        setOnClickListener {
            if (sticky) {
                sticky = false
                collapse()
            }
        }
    }

    fun push(event: FlightEvent) {
        titleView.text = event.title
        detailView.text = event.detail
        val fill = when (event.severity) {
            EventSeverity.FAIL -> 0xE8B01018.toInt()
            EventSeverity.WATCH -> 0xE8A06010.toInt()
            EventSeverity.INFO -> 0xE80A3040.toInt()
        }
        val stroke = when (event.severity) {
            EventSeverity.FAIL -> 0xFFFF3344.toInt()
            EventSeverity.WATCH -> 0xFFF2C14E.toInt()
            EventSeverity.INFO -> 0xFF00D4FF.toInt()
        }
        val bg = GradientDrawable()
        bg.setColor(fill)
        bg.setStroke((2 * resources.displayMetrics.density).toInt().coerceAtLeast(2), stroke)
        background = bg
        sticky = event.severity == EventSeverity.FAIL
        visibility = VISIBLE
        handler.removeCallbacks(hide)
        if (!sticky) handler.postDelayed(hide, 5200L)
    }

    fun collapse() {
        handler.removeCallbacks(hide)
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(hide)
        super.onDetachedFromWindow()
    }
}
