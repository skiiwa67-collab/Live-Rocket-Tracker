package com.ccos.retro.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.ccos.retro.R

/**
 * Chrome-less video window we size via WindowManager.LayoutParams.
 * Not OverlayPipActivity. Not FloatingVideoWindow. No SignIN / YT title bar.
 * Drag the video surface itself. Chrome is size-dependent icon chips only.
 */
class SizedVidWindow(
    private val host: CommandCenterActivity,
    private val onMute: () -> Unit,
    private val onCc: () -> Unit,
    private val onGear: () -> Unit,
    private val onExpandFull: () -> Unit,
    private val onClosed: () -> Unit,
    private val muteOn: () -> Boolean,
    private val ccOn: () -> Boolean
) {
    enum class Size { SMALLEST, MEDIUM }

    var size: Size = Size.MEDIUM
        private set
    var onSizeChanged: ((Size) -> Unit)? = null
    val isShowing: Boolean get() = root != null

    private val wm = host.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: FrameLayout? = null
    private var chrome: LinearLayout? = null
    private var lp: WindowManager.LayoutParams? = null
    private var web: WebView? = null
    private var webHome: ViewGroup? = null
    private var webHomeIndex: Int = 0

    fun show(webView: WebView, start: Size = Size.MEDIUM) {
        if (!canDraw(host)) return
        if (root != null && web === webView) {
            applySize(start)
            return
        }
        dismiss(reparentHome = false)
        web = webView
        val parent = webView.parent as? ViewGroup
        webHome = parent
        webHomeIndex = parent?.indexOfChild(webView) ?: 0
        parent?.removeView(webView)

        val density = host.resources.displayMetrics.density
        val frame = DragRoot(host).apply {
            setBackgroundColor(Color.BLACK)
            clipToPadding = false
            clipChildren = false
        }
        frame.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        val strip = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val pad = (6f * density).toInt()
            setPadding(pad, pad, pad, pad)
            isClickable = true
            isFocusable = true
        }
        frame.addView(
            strip,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            )
        )
        chrome = strip
        frame.chrome = strip
        frame.onDrag = { dx, dy ->
            val params = lp
            if (params != null) {
                params.x += dx
                params.y += dy
                val dm = host.resources.displayMetrics
                clamp(params, dm.widthPixels, dm.heightPixels)
                try {
                    wm.updateViewLayout(frame, params)
                } catch (_: Exception) {
                }
            }
        }
        val params = newParams()
        try {
            wm.addView(frame, params)
        } catch (_: Exception) {
            reparentHome(webView)
            return
        }
        root = frame
        lp = params
        applySize(start)
    }

    fun applySize(next: Size) {
        size = next
        onSizeChanged?.invoke(next)
        val params = lp ?: return
        val rootView = root ?: return
        val dm = host.resources.displayMetrics
        val (w, h) = targetPx(next)
        params.width = w
        params.height = h
        clamp(params, dm.widthPixels, dm.heightPixels)
        try {
            wm.updateViewLayout(rootView, params)
        } catch (_: Exception) {
        }
        renderChrome()
    }

    fun toggleSize() {
        applySize(if (size == Size.SMALLEST) Size.MEDIUM else Size.SMALLEST)
    }

    fun dismiss(reparentHome: Boolean = true) {
        val frame = root
        val webView = web
        root = null
        chrome = null
        lp = null
        web = null
        if (frame != null) {
            if (webView != null) {
                (webView.parent as? ViewGroup)?.removeView(webView)
            }
            try {
                wm.removeView(frame)
            } catch (_: Exception) {
            }
        }
        if (reparentHome && webView != null) {
            reparentHome(webView)
        }
        webHome = null
    }

    private fun reparentHome(webView: WebView) {
        val home = webHome ?: return
        if (webView.parent === home) return
        (webView.parent as? ViewGroup)?.removeView(webView)
        val idx = webHomeIndex.coerceIn(0, home.childCount)
        try {
            home.addView(
                webView,
                idx,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        } catch (_: Exception) {
            home.addView(webView)
        }
    }

    private fun renderChrome() {
        val strip = chrome ?: return
        strip.removeAllViews()
        val density = host.resources.displayMetrics.density
        val tap = if (size == Size.SMALLEST) (36f * density).toInt() else (44f * density).toInt()
        fun chip(icon: Int, desc: String, onClick: () -> Unit): ImageButton {
            val btn = ImageButton(host)
            btn.setImageResource(icon)
            btn.contentDescription = desc
            btn.background = chipBg(density)
            btn.scaleType = ImageView.ScaleType.CENTER_INSIDE
            btn.setPadding((8f * density).toInt(), (8f * density).toInt(), (8f * density).toInt(), (8f * density).toInt())
            btn.setOnClickListener { onClick() }
            val mlp = LinearLayout.LayoutParams(tap, tap)
            mlp.marginStart = (4f * density).toInt()
            btn.layoutParams = mlp
            return btn
        }
        // Never a title bar. Icon chips only. Smallest = grow + X.
        if (size == Size.SMALLEST) {
            strip.addView(chip(R.drawable.ic_pip_size_out, "Grow", { applySize(Size.MEDIUM) }))
            strip.addView(chip(R.drawable.ic_sized_close, "Close", { dismiss(); onClosed() }))
            return
        }
        val muteIcon = if (muteOn()) R.drawable.ic_pip_mute else R.drawable.ic_pip_volume
        strip.addView(chip(muteIcon, if (muteOn()) "Unmute" else "Mute", onMute))
        strip.addView(chip(R.drawable.ic_pip_cc, "Captions", onCc))
        strip.addView(chip(R.drawable.ic_sized_gear, "Settings", onGear))
        strip.addView(chip(R.drawable.ic_pip_size_in, "Shrink", { applySize(Size.SMALLEST) }))
        strip.addView(chip(R.drawable.ic_pip_size_out, "Grow", { applySize(Size.MEDIUM) }))
        strip.addView(chip(R.drawable.ic_sized_expand, "Full MCC", onExpandFull))
    }

    fun refreshChrome() {
        if (root != null) renderChrome()
    }

    /**
     * Drag the video surface. Chrome chips stay clickable — no title-bar grab.
     */
    private class DragRoot(context: Context) : FrameLayout(context) {
        var chrome: View? = null
        var onDrag: ((Int, Int) -> Unit)? = null
        private var lastX = 0f
        private var lastY = 0f
        private var dragging = false
        private val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (hitChrome(ev)) return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.rawX
                    lastY = ev.rawY
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - lastX
                    val dy = ev.rawY - lastY
                    if (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop) {
                        dragging = true
                        return true
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - lastX).toInt()
                    val dy = (ev.rawY - lastY).toInt()
                    lastX = ev.rawX
                    lastY = ev.rawY
                    if (dx != 0 || dy != 0) onDrag?.invoke(dx, dy)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    return true
                }
            }
            return super.onTouchEvent(ev)
        }

        private fun hitChrome(ev: MotionEvent): Boolean {
            val c = chrome ?: return false
            val loc = IntArray(2)
            c.getLocationOnScreen(loc)
            val x = ev.rawX
            val y = ev.rawY
            return x >= loc[0] && x < loc[0] + c.width && y >= loc[1] && y < loc[1] + c.height
        }
    }

    private fun newParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val flags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        val (w, h) = targetPx(size)
        val dm = host.resources.displayMetrics
        val p = WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT)
        p.gravity = Gravity.TOP or Gravity.START
        p.x = (dm.widthPixels - w - (12f * dm.density).toInt()).coerceAtLeast(0)
        p.y = (dm.heightPixels - h - (72f * dm.density).toInt()).coerceAtLeast((48f * dm.density).toInt())
        p.title = ""
        return p
    }

    private fun targetPx(sz: Size): Pair<Int, Int> {
        val dm = host.resources.displayMetrics
        return if (sz == Size.SMALLEST) {
            Pair(SMALLEST_W, SMALLEST_H)
        } else {
            val maxW = (dm.widthPixels * 0.96f).toInt().coerceAtLeast(SMALLEST_W)
            val maxH = (dm.heightPixels * 0.72f).toInt().coerceAtLeast(SMALLEST_H)
            val w = MEDIUM_W.coerceAtMost(maxW)
            val h = (w * 9f / 16f).toInt().coerceAtMost(maxH).coerceAtLeast(1)
            Pair(w, h)
        }
    }

    private fun clamp(params: WindowManager.LayoutParams, screenW: Int, screenH: Int) {
        params.x = params.x.coerceIn(-params.width + 48, screenW - 48)
        params.y = params.y.coerceIn(0, screenH - 48)
    }

    private fun chipBg(density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xCC0A1016.toInt())
            setStroke((1.2f * density).toInt().coerceAtLeast(1), 0xFF3CFF7A.toInt())
        }
    }

    companion object {
        const val SMALLEST_W = 280
        const val SMALLEST_H = 168
        const val MEDIUM_W = 1120
        const val MEDIUM_H = 630

        fun canDraw(ctx: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= 23) Settings.canDrawOverlays(ctx) else true
        }
    }
}
