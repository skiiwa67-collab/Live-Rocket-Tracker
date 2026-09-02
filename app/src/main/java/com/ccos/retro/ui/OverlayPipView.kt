package com.ccos.retro.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ccos.retro.data.WebcastResolver

/**
 * HUD overlay player. Thin chrome + a real video well.
 * Not the MCC YouTube pane. Not YouTube home/search chrome.
 */
@SuppressLint("SetJavaScriptEnabled")
class OverlayPipView(
    context: Context,
    title: String,
    startUrl: String,
    private val onClosed: () -> Unit,
    private val onMove: (dx: Int, dy: Int) -> Unit,
    private val onResize: (width: Int, height: Int) -> Unit
) : LinearLayout(context) {

    private val web: WebView
    private val titleView: TextView
    private val muteBtn: TextView
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var resizing = false
    private var muted = false
    var pageUrl: String = startUrl
        private set

    init {
        orientation = VERTICAL
        val d = resources.displayMetrics.density
        val chrome = GradientDrawable().apply {
            setColor(0xF00C141C.toInt())
            setStroke((1.5f * d).toInt().coerceAtLeast(1), 0xFF00D4FF.toInt())
        }
        background = chrome
        elevation = 18f * d
        CookieManager.getInstance().setAcceptCookie(true)

        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xE00A2030.toInt())
            val pad = (4 * d).toInt()
            setPadding(pad, 0, pad, 0)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, chromeH())
        }
        val grip = TextView(context).apply {
            text = "≡"
            setTextColor(0xFF8AA0B0.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((22 * d).toInt(), LayoutParams.MATCH_PARENT)
            setOnTouchListener { _, ev -> onDrag(ev) }
        }
        titleView = TextView(context).apply {
            text = title
            setTextColor(0xFF00D4FF.toInt())
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            setOnTouchListener { _, ev -> onDrag(ev) }
        }
        muteBtn = chromeBtn("♪") { toggleMute() }
        bar.addView(grip)
        bar.addView(titleView)
        bar.addView(muteBtn)
        bar.addView(chromeBtn("−") { scale(1f / 1.22f) })
        bar.addView(chromeBtn("+") { scale(1.22f) })
        bar.addView(chromeBtn("X") { close() })
        addView(bar)

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        web = WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            setLayerType(LAYER_TYPE_HARDWARE, null)
            minimumHeight = minVideoH()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = chromeMobileUa(settings.userAgentString)
            cookies.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    cookies.flush()
                    if (!url.isNullOrBlank()) pageUrl = url
                    applyVolume()
                }
            }
            webChromeClient = WebChromeClient()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        addView(web)

        val handle = TextView(context).apply {
            text = "◢"
            setTextColor(0xFF8AA0B0.toInt())
            textSize = 12f
            gravity = Gravity.END or Gravity.BOTTOM
            setPadding(0, 0, (6 * d).toInt(), (2 * d).toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, handleH())
            setOnTouchListener { _, ev -> onResizeTouch(ev) }
        }
        addView(handle)
        load(startUrl)
    }

    fun load(url: String) {
        pageUrl = url
        web.loadUrl(WebcastResolver.overlayPlayUrl(url))
    }

    fun setTitle(text: String) {
        titleView.text = text
    }

    fun applyVolume() {
        val js = if (muted) {
            "try{document.querySelectorAll('video').forEach(function(v){v.muted=true})}catch(e){}"
        } else {
            "try{document.querySelectorAll('video').forEach(function(v){v.muted=false;v.volume=0.35})}catch(e){}"
        }
        try { web.evaluateJavascript(js, null) } catch (_: Exception) {}
    }

    fun destroyPlayer() {
        CookieManager.getInstance().flush()
        try {
            web.stopLoading()
            web.loadUrl("about:blank")
            web.onPause()
            web.removeAllViews()
            web.destroy()
        } catch (_: Exception) {}
    }

    private fun close() {
        destroyPlayer()
        onClosed()
    }

    private fun toggleMute() {
        muted = !muted
        muteBtn.text = if (muted) "🔇" else "♪"
        applyVolume()
    }

    private fun scale(factor: Float) {
        val w = (width * factor).toInt()
        val h = (height * factor).toInt()
        onResize(clampW(w), clampH(h, w))
    }

    private fun onDrag(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                lastX = ev.rawX
                lastY = ev.rawY
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                onMove((ev.rawX - lastX).toInt(), (ev.rawY - lastY).toInt())
                lastX = ev.rawX
                lastY = ev.rawY
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
            else -> return false
        }
    }

    private fun onResizeTouch(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizing = true
                lastX = ev.rawX
                lastY = ev.rawY
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!resizing) return false
                val w = clampW(width + (ev.rawX - lastX).toInt())
                val h = clampH(height + (ev.rawY - lastY).toInt(), w)
                lastX = ev.rawX
                lastY = ev.rawY
                onResize(w, h)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resizing = false
                return true
            }
            else -> return false
        }
    }

    private fun chromeBtn(label: String, click: () -> Unit): TextView {
        val d = resources.displayMetrics.density
        return TextView(context).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val pad = (5 * d).toInt()
            setPadding(pad, 0, pad, 0)
            minWidth = (26 * d).toInt()
            minHeight = (26 * d).toInt()
            setOnClickListener { click() }
        }
    }

    private fun chromeMobileUa(current: String?): String {
        val raw = current.orEmpty()
        val stripped = raw.replace("; wv", "").replace(" Version/4.0", "")
        return if (stripped.contains("Chrome/")) stripped
        else "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    }

    private fun chromeH(): Int = (32 * resources.displayMetrics.density).toInt()
    private fun handleH(): Int = (20 * resources.displayMetrics.density).toInt()
    private fun minVideoH(): Int = (120 * resources.displayMetrics.density).toInt()
    private fun minW(): Int = (168 * resources.displayMetrics.density).toInt()
    private fun minH(): Int = chromeH() + minVideoH() + handleH()

    private fun clampW(w: Int): Int {
        val max = (resources.displayMetrics.widthPixels * 0.72f).toInt()
        return w.coerceIn(minW(), max.coerceAtLeast(minW()))
    }

    private fun clampH(h: Int, w: Int): Int {
        val max = (resources.displayMetrics.heightPixels * 0.48f).toInt()
        val sixteenNine = chromeH() + handleH() + (w * 9 / 16)
        return h.coerceIn(minH(), max.coerceAtLeast(minH())).coerceAtLeast(sixteenNine.coerceAtMost(max))
    }

    companion object {
        fun defaultWidth(ctx: Context): Int {
            val d = ctx.resources.displayMetrics.density
            val screen = ctx.resources.displayMetrics.widthPixels
            return (200 * d).toInt().coerceAtMost((screen * 0.46f).toInt()).coerceAtLeast((168 * d).toInt())
        }

        fun defaultHeight(ctx: Context, width: Int): Int {
            val d = ctx.resources.displayMetrics.density
            val chrome = (32 * d).toInt()
            val handle = (20 * d).toInt()
            val video = (width * 9 / 16).coerceAtLeast((120 * d).toInt())
            return chrome + video + handle
        }
    }
}
