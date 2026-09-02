package com.ccos.retro.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle

/**
 * Draggable overlay PiP for HUD VID links. One pane. YouTube keeps audio.
 * Never requests AUDIOFOCUS_GAIN. Volume is lowered in the WebView, not muted.
 * System PiP is the product on HUD. If enter fails, finish() — never a fullscreen tap-sink.
 */
class OverlayPipActivity : AppCompatActivity() {

    private var window: FloatingVideoWindow? = null
    private var lastUrl: String = ""
    private var pipEnterAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val host = FrameLayout(this).apply {
            setBackgroundColor(0x00000000)
        }
        setContentView(host)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        val win = FloatingVideoWindow(
            this, "overlay", title, url, true,
            onClosed = { finish() },
            onActivated = { },
            onSignInStarted = { },
            onSignInFinished = { }
        )
        val d = resources.displayMetrics.density
        val lp = FrameLayout.LayoutParams(
            (280 * d).toInt(),
            (168 * d).toInt()
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = (72 * d).toInt()
            marginEnd = (12 * d).toInt()
        }
        host.addView(win, lp)
        window = win
        lastUrl = url
        win.lowerVolume()
        scheduleEnterPipAfterLayout(win)
    }

    override fun onResume() {
        super.onResume()
        val win = window ?: return
        if (win.width > 0 && win.height > 0) {
            enterPipFromLaidOutWindow(win)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        if (url.isNotBlank() && url != lastUrl) {
            lastUrl = url
            window?.setTitle(title)
            window?.load(url)
            window?.lowerVolume()
        }
        if (!isInPictureInPictureMode) {
            pipEnterAttempted = false
            window?.let { scheduleEnterPipAfterLayout(it) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode && isFinishing) return
    }

    private fun scheduleEnterPipAfterLayout(win: View) {
        val afterLayout = Runnable {
            if (isFinishing) return@Runnable
            if (win.width > 0 && win.height > 0) {
                enterPipFromLaidOutWindow(win)
                return@Runnable
            }
            val observer = win.viewTreeObserver
            if (!observer.isAlive) {
                finish()
                return@Runnable
            }
            observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (win.width <= 0 || win.height <= 0) return
                    val live = win.viewTreeObserver
                    if (live.isAlive) {
                        live.removeOnGlobalLayoutListener(this)
                    }
                    enterPipFromLaidOutWindow(win)
                }
            })
        }
        win.post(afterLayout)
    }

    private fun enterPipFromLaidOutWindow(win: View) {
        if (pipEnterAttempted || isFinishing || isInPictureInPictureMode) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (Build.VERSION.SDK_INT < 26) {
            finish()
            return
        }
        pipEnterAttempted = true
        val loc = IntArray(2)
        win.getLocationOnScreen(loc)
        val hint = Rect(loc[0], loc[1], loc[0] + win.width, loc[1] + win.height)
        if (hint.width() < 2 || hint.height() < 2) {
            finish()
            return
        }
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setSourceRectHint(hint)
                .build()
            val entered = enterPictureInPictureMode(params)
            if (!entered) {
                finish()
            }
        } catch (_: Exception) {
            finish()
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
