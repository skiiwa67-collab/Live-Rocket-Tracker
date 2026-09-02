package com.ccos.retro.ui

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.ccos.retro.data.WebcastResolver

/**
 * Small HUD overlay player. The *window* is the PiP rect — not a full-screen
 * touch sink. Touches outside this window go to the wallpaper plates.
 * Never enter Android system Picture-in-Picture (that collapsed to a title bar).
 * MCC is the big YouTube pane. This is embed + thin chrome only.
 */
class OverlayPipActivity : AppCompatActivity() {

    private var player: OverlayPipView? = null
    private var lastUrl: String = ""
    private var placed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySmallWindow(forceDefault = true)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        val play = OverlayPipView(
            this, title, url,
            onClosed = { finish() },
            onMove = { dx, dy -> moveBy(dx, dy) },
            onResize = { w, h -> resizeTo(w, h) }
        )
        player = play
        lastUrl = url
        setContentView(play)
        applySmallWindow(forceDefault = false)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        if (url.isNotBlank() && WebcastResolver.overlayPlayUrl(url) != WebcastResolver.overlayPlayUrl(lastUrl)) {
            lastUrl = url
            player?.setTitle(title)
            player?.load(url)
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
        player?.destroyPlayer()
        player = null
        super.onDestroy()
    }

    private fun applySmallWindow(forceDefault: Boolean) {
        val d = resources.displayMetrics.density
        val lp = window.attributes
        if (forceDefault || !placed) {
            val w = OverlayPipView.defaultWidth(this)
            val h = OverlayPipView.defaultHeight(this, w)
            lp.width = w
            lp.height = h
            lp.gravity = Gravity.TOP or Gravity.END
            lp.x = (8 * d).toInt()
            lp.y = (72 * d).toInt()
            placed = true
        }
        lp.flags = lp.flags or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        window.attributes = lp
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
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
        val lp = window.attributes
        lp.width = width
        lp.height = height
        lp.x = lp.x.coerceIn(0, (resources.displayMetrics.widthPixels - width).coerceAtLeast(0))
        lp.y = lp.y.coerceIn(0, (resources.displayMetrics.heightPixels - height).coerceAtLeast(0))
        window.attributes = lp
        window.setLayout(width, height)
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
