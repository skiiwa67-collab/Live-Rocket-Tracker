package com.ccos.retro.ui

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.ccos.retro.data.WebcastResolver

/**
 * HUD overlay player. The *window* is the 280×168 dp player — not a full-screen
 * touch sink and not Android system Picture-in-Picture.
 *
 * enterPictureInPictureMode + setSourceRectHint(Rect(0,0,16,9)) collapsed the
 * well to a SignIN YT − + X sliver whenever the WebView failed to paint.
 * Do not call enterPictureInPictureMode.
 *
 * FLAG_NOT_TOUCH_MODAL: taps around this window hit CMD CDT TEL STS PAD VID MSK AUTO.
 * MCC SignIN stays in MCC. This overlay is one FloatingVideoWindow.
 */
class OverlayPipActivity : AppCompatActivity() {

    private var windowView: FloatingVideoWindow? = null
    private var lastUrl: String = ""
    private var placed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySmallWindow(forceDefault = true)
        val raw = intent.getStringExtra(EXTRA_URL).orEmpty()
        val url = WebcastResolver.watchPlayUrl(raw)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        val win = FloatingVideoWindow(
            this, "overlay", title, url, true,
            onClosed = { finish() },
            onActivated = { },
            onSignInStarted = { },
            onSignInFinished = { },
            showSignIn = false,
            onWindowMove = { dx, dy -> moveBy(dx, dy) },
            onWindowResize = { w, h -> resizeTo(w, h) }
        )
        windowView = win
        lastUrl = url
        setContentView(win)
        applySmallWindow(forceDefault = false)
        win.lowerVolume()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val raw = intent.getStringExtra(EXTRA_URL).orEmpty()
        val url = WebcastResolver.watchPlayUrl(raw)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        if (url.isNotBlank() && url != lastUrl) {
            lastUrl = url
            windowView?.setTitle(title)
            windowView?.load(url)
            windowView?.lowerVolume()
        }
        applySmallWindow(forceDefault = false)
    }

    override fun onResume() {
        super.onResume()
        applySmallWindow(forceDefault = false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applySmallWindow(forceDefault = false)
    }

    override fun onDestroy() {
        windowView?.destroyFeed()
        windowView = null
        super.onDestroy()
    }

    private fun applySmallWindow(forceDefault: Boolean) {
        val d = resources.displayMetrics.density
        val lp = window.attributes
        if (forceDefault || !placed) {
            lp.width = (WIN_W_DP * d).toInt()
            lp.height = (WIN_H_DP * d).toInt()
            lp.gravity = Gravity.TOP or Gravity.END
            lp.x = (12 * d).toInt()
            lp.y = (72 * d).toInt()
            placed = true
        }
        lp.flags = lp.flags or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        window.attributes = lp
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (lp.width > 0 && lp.height > 0) window.setLayout(lp.width, lp.height)
    }

    private fun moveBy(dx: Int, dy: Int) {
        val lp = window.attributes
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (lp.x + dx).coerceIn(0, (resources.displayMetrics.widthPixels - lp.width).coerceAtLeast(0))
        lp.y = (lp.y + dy).coerceIn(0, (resources.displayMetrics.heightPixels - lp.height).coerceAtLeast(0))
        window.attributes = lp
    }

    private fun resizeTo(width: Int, height: Int) {
        val dm = resources.displayMetrics
        val d = dm.density
        val minW = (168 * d).toInt()
        val minH = (128 * d).toInt()
        val maxW = (dm.widthPixels * 0.92f).toInt().coerceAtLeast(minW)
        val maxH = (dm.heightPixels * 0.72f).toInt().coerceAtLeast(minH)
        val w = width.coerceIn(minW, maxW)
        val h = height.coerceIn(minH, maxH)
        val lp = window.attributes
        lp.width = w
        lp.height = h
        lp.x = lp.x.coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
        lp.y = lp.y.coerceIn(0, (dm.heightPixels - h).coerceAtLeast(0))
        window.attributes = lp
        window.setLayout(w, h)
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        private const val WIN_W_DP = 280f
        private const val WIN_H_DP = 168f
    }
}
