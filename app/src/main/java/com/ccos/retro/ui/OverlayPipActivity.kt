package com.ccos.retro.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Stamp 25/26 HUD overlay: system Picture-in-Picture over the wallpaper.
 * Same FloatingVideoWindow watch URL that already played. Not an /embed
 * (Error 153). Not the MCC SignIN/YT chrome glued onto the HUD.
 * If PiP does not take, the *window* shrinks to the player rect so plate
 * taps around it still reach the wallpaper. Never a full-screen tap sink.
 */
class OverlayPipActivity : Activity() {

    private var player: FloatingVideoWindow? = null
    private var lastUrl: String = ""
    private var closing = false
    private var placed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        val host = FrameLayout(this).apply {
            setBackgroundColor(0x00000000)
        }
        setContentView(host)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        val win = FloatingVideoWindow(
            this, "overlay", title, url, true,
            onClosed = {
                closing = true
                finish()
            },
            onActivated = { },
            onSignInStarted = { },
            onSignInFinished = { },
            onMove = { dx, dy -> moveBy(dx, dy) },
            onResize = { w, h -> resizeTo(w, h) }
        )
        host.addView(
            win,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        player = win
        lastUrl = url
        win.lowerVolume()
        shrinkToPlayerRect()
        enterPip()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        if (url.isNotBlank() && url != lastUrl) {
            lastUrl = url
            player?.setTitle(title)
            player?.load(url)
            player?.lowerVolume()
        }
        enterPip()
    }

    override fun onStart() {
        super.onStart()
        enterPip()
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post {
            if (closing) return@post
            enterPip()
            if (!inPip()) shrinkToPlayerRect()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        player?.setPipChrome(!isInPictureInPictureMode)
        if (!isInPictureInPictureMode && !closing && !isFinishing) {
            shrinkToPlayerRect()
            enterPip()
        }
    }

    override fun onDestroy() {
        player?.destroyFeed()
        player = null
        super.onDestroy()
    }

    private fun enterPip() {
        if (closing || Build.VERSION.SDK_INT < 26) return
        try {
            val hint = Rect()
            window.decorView.getGlobalVisibleRect(hint)
            val b = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
            if (hint.width() > 16 && hint.height() > 9) b.setSourceRectHint(hint)
            if (Build.VERSION.SDK_INT >= 31) {
                b.setAutoEnterEnabled(true)
                b.setSeamlessResizeEnabled(true)
            }
            val params = b.build()
            setPictureInPictureParams(params)
            if (!isInPictureInPictureMode) enterPictureInPictureMode(params)
        } catch (_: Exception) {
        }
    }

    /**
     * Hit-test is this window. Keep it the visible player, never MATCH_PARENT.
     */
    private fun shrinkToPlayerRect() {
        val w = playerWidth()
        val h = playerHeight(w)
        val d = resources.displayMetrics.density
        val lp = window.attributes
        lp.width = w
        lp.height = h
        if (!placed) {
            lp.gravity = Gravity.TOP or Gravity.END
            lp.x = (8 * d).toInt()
            lp.y = (72 * d).toInt()
            placed = true
        }
        lp.flags = (lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) and
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH.inv()
        window.attributes = lp
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        window.setLayout(w, h)
        player?.setPipChrome(true)
    }

    private fun moveBy(dx: Int, dy: Int) {
        if (inPip()) return
        val lp = window.attributes
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (lp.x + dx).coerceIn(0, (resources.displayMetrics.widthPixels - lp.width).coerceAtLeast(0))
        lp.y = (lp.y + dy).coerceIn(0, (resources.displayMetrics.heightPixels - lp.height).coerceAtLeast(0))
        window.attributes = lp
    }

    private fun resizeTo(width: Int, height: Int) {
        if (inPip()) return
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val d = resources.displayMetrics.density
        val minW = (screenW * 0.28f).toInt().coerceAtLeast((168 * d).toInt())
        val minH = playerHeight(minW)
        val w = width.coerceIn(minW, (screenW * 0.72f).toInt().coerceAtLeast(minW))
        val h = height.coerceIn(minH, (screenH * 0.48f).toInt().coerceAtLeast(minH))
        val lp = window.attributes
        lp.width = w
        lp.height = h
        lp.x = lp.x.coerceIn(0, (screenW - w).coerceAtLeast(0))
        lp.y = lp.y.coerceIn(0, (screenH - h).coerceAtLeast(0))
        window.attributes = lp
        window.setLayout(w, h)
    }

    private fun inPip(): Boolean =
        Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode

    private fun playerWidth(): Int {
        val screen = resources.displayMetrics.widthPixels
        return (screen * 0.46f).toInt().coerceAtLeast((resources.displayMetrics.heightPixels * 0.28f).toInt())
    }

    private fun playerHeight(width: Int): Int {
        val d = resources.displayMetrics.density
        val chrome = (28 * d).toInt()
        val handle = (22 * d).toInt()
        val well = (width * 9 / 16).coerceAtLeast((120 * d).toInt())
        return chrome + well + handle
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
