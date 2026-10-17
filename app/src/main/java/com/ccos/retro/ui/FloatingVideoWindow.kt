package com.ccos.retro.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Draggable / resizable MCC video window.
 * Cookies persist in this app's WebView. Sign in once (YT / Google) and Premium
 * / subs apply here. Chrome does not share its login with WebView.
 * Do not call WebView.pauseTimers() from a single pane — that is process-wide.
 */
@SuppressLint("SetJavaScriptEnabled")
class FloatingVideoWindow(
    context: Context,
    val feedId: String,
    title: String,
    embedUrl: String,
    private val autoplay: Boolean,
    private val onClosed: (FloatingVideoWindow) -> Unit,
    private val onActivated: (FloatingVideoWindow) -> Unit,
    private val onSignInStarted: (FloatingVideoWindow) -> Unit,
    private val onSignInFinished: (FloatingVideoWindow) -> Unit
) : LinearLayout(context) {

    private val web: WebView
    private val titleView: TextView
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var resizing = false
    private var currentUrl = embedUrl
    private var parkedUrl: String? = null
    private var signingIn = false
    private var signedHint = false

    init {
        CookieManager.getInstance().setAcceptCookie(true)
        orientation = VERTICAL
        val d = resources.displayMetrics.density
        val chrome = GradientDrawable().apply {
            setColor(0xF00C141C.toInt())
            setStroke((1.5f * d).toInt().coerceAtLeast(1), 0xFF00D4FF.toInt())
        }
        background = chrome
        elevation = 16f * d

        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xE00A2030.toInt())
            setPadding((4 * d).toInt(), (2 * d).toInt(), (3 * d).toInt(), (2 * d).toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (28 * d).toInt())
        }
        titleView = TextView(context).apply {
            text = title
            setTextColor(0xFF00D4FF.toInt())
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val dragListener = View.OnTouchListener { _, ev -> onDragTouch(ev) }
        val grip = TextView(context).apply {
            text = "≡"
            setTextColor(0xFF8AA0B0.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((18 * d).toInt(), LayoutParams.MATCH_PARENT)
            setOnTouchListener(dragListener)
        }
        titleView.setOnTouchListener(dragListener)
        bar.addView(grip)
        bar.addView(titleView)
        bar.addView(chromeBtn("SignIN") { startSignIn() })
        bar.addView(chromeBtn("YT") { openInYoutube() })
        bar.addView(chromeBtn("−") { scale(1f / 1.22f) })
        bar.addView(chromeBtn("+") { scale(1.22f) })
        bar.addView(chromeBtn("X") { close() })
        addView(bar)

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)

        web = WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            setLayerType(LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = !autoplay
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = chromeMobileUa(settings.userAgentString)
            cookies.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    cookies.flush()
                    val u = url.orEmpty().lowercase()
                    if (u.contains("accounts.google") || u.contains("/signin")) {
                        signedHint = true
                        signingIn = true
                    }
                    if (signingIn && u.contains("youtube.com") && !u.contains("accounts.google") && !u.contains("/signin")) {
                        signingIn = false
                        cookies.flush()
                        onSignInFinished(this@FloatingVideoWindow)
                    }
                }
            }
            webChromeClient = WebChromeClient()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            setOnTouchListener { _, ev ->
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) activate()
                false
            }
        }
        addView(web)

        val handle = TextView(context).apply {
            text = "◢"
            setTextColor(0xFF8AA0B0.toInt())
            textSize = 14f
            gravity = Gravity.END or Gravity.BOTTOM
            setPadding(0, 0, (6 * d).toInt(), (2 * d).toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (22 * d).toInt())
            setOnTouchListener { _, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        resizing = true
                        lastX = ev.rawX
                        lastY = ev.rawY
                        parent?.requestDisallowInterceptTouchEvent(true)
                        activate()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!resizing) return@setOnTouchListener false
                        val lp = this@FloatingVideoWindow.layoutParams as FrameLayout.LayoutParams
                        lp.width = (lp.width + (ev.rawX - lastX)).toInt()
                        lp.height = (lp.height + (ev.rawY - lastY)).toInt()
                        clamp(lp)
                        this@FloatingVideoWindow.layoutParams = lp
                        lastX = ev.rawX
                        lastY = ev.rawY
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        resizing = false
                        true
                    }
                    else -> false
                }
            }
        }
        addView(handle)

        load(embedUrl)
    }

    fun load(url: String) {
        currentUrl = url
        web.loadUrl(url)
    }

    /** Pause HTML video only. Do NOT WebView.onPause here — that freezes a sibling during Google login. */
    fun pausePlayback() {
        try {
            web.evaluateJavascript(
                "try{document.querySelectorAll('video').forEach(function(v){v.pause()})}catch(e){}",
                null
            )
        } catch (_: Exception) {}
    }

    fun pause() {
        CookieManager.getInstance().flush()
        pausePlayback()
    }

    fun resume() {
        web.onResume()
    }

    fun freezeRenderer() {
        CookieManager.getInstance().flush()
        web.onPause()
    }

    fun hibernate() {
        if (parkedUrl == null) parkedUrl = currentUrl
        try {
            web.stopLoading()
            web.loadUrl("about:blank")
        } catch (_: Exception) {}
    }

    fun wake() {
        val u = parkedUrl ?: currentUrl
        parkedUrl = null
        web.onResume()
        if (u.isNotBlank() && u != "about:blank") load(u)
    }

    fun destroyFeed() {
        web.stopLoading()
        web.loadUrl("about:blank")
        web.onPause()
        web.removeAllViews()
        web.destroy()
    }

    fun close() {
        CookieManager.getInstance().flush()
        destroyFeed()
        onClosed(this)
    }

    private fun activate() {
        bringToFront()
        web.onResume()
        onActivated(this)
    }

    private fun startSignIn() {
        signingIn = true
        onSignInStarted(this)
        web.settings.mediaPlaybackRequiresUserGesture = true
        web.loadUrl(
            "https://accounts.google.com/ServiceLogin?service=youtube&continue=" +
                Uri.encode("https://m.youtube.com")
        )
    }

    private fun openInYoutube() {
        val uri = Uri.parse(currentUrl.ifBlank { "https://m.youtube.com" })
        val yt = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(yt)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun chromeMobileUa(current: String?): String {
        val raw = current.orEmpty()
        val stripped = raw.replace("; wv", "").replace(" Version/4.0", "")
        return if (stripped.contains("Chrome/")) stripped
        else "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    }

    private fun scale(factor: Float) {
        val parent = parent as? View ?: return
        val lp = layoutParams as FrameLayout.LayoutParams
        lp.width = (lp.width * factor).toInt()
        lp.height = (lp.height * factor).toInt()
        clamp(lp, parent)
        layoutParams = lp
    }

    private fun onDragTouch(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                lastX = ev.rawX
                lastY = ev.rawY
                parent?.requestDisallowInterceptTouchEvent(true)
                activate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val lp = layoutParams as FrameLayout.LayoutParams
                lp.gravity = Gravity.TOP or Gravity.START
                lp.leftMargin = (lp.leftMargin + (ev.rawX - lastX)).toInt()
                lp.topMargin = (lp.topMargin + (ev.rawY - lastY)).toInt()
                clamp(lp, clampSize = false)
                layoutParams = lp
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

    private fun clamp(lp: FrameLayout.LayoutParams, host: View? = parent as? View, clampSize: Boolean = true) {
        val d = resources.displayMetrics.density
        val minW = (168 * d).toInt()
        val minH = (128 * d).toInt()
        lp.gravity = Gravity.TOP or Gravity.START
        val pw = host?.width ?: 0
        val ph = host?.height ?: 0
        if (pw <= 0 || ph <= 0) {
            if (clampSize) {
                lp.width = lp.width.coerceAtLeast(minW)
                lp.height = lp.height.coerceAtLeast(minH)
            }
            return
        }
        if (clampSize) {
            val maxW = (pw * 0.92f).toInt().coerceAtLeast(minW)
            val maxH = (ph * 0.72f).toInt().coerceAtLeast(minH)
            lp.width = lp.width.coerceAtLeast(minW).coerceAtMost(maxW)
            lp.height = lp.height.coerceAtLeast(minH).coerceAtMost(maxH)
        }
        lp.leftMargin = lp.leftMargin.coerceIn(0, (pw - lp.width).coerceAtLeast(0))
        lp.topMargin = lp.topMargin.coerceIn(0, (ph - lp.height).coerceAtLeast(0))
    }

    private fun chromeBtn(label: String, click: () -> Unit): TextView {
        val d = resources.displayMetrics.density
        return TextView(context).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val pad = (3 * d).toInt()
            setPadding(pad, 0, pad, 0)
            minWidth = if (label.length > 3) (36 * d).toInt() else (22 * d).toInt()
            minHeight = (22 * d).toInt()
            setOnClickListener { click() }
        }
    }
}
