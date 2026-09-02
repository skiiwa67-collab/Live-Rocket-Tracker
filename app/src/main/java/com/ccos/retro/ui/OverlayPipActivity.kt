package com.ccos.retro.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Color
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
 * HUD VID path. Chrome-less WebView only — no SignIN YT − + X bar, no resize handle.
 * Opaque window so Motorola Razr can enter system PiP. YouTube keeps audio.
 * Never requests AUDIOFOCUS_GAIN. If enter fails, finish() — never a fullscreen tap-sink.
 */
class OverlayPipActivity : AppCompatActivity() {

    private var pipWindow: FloatingVideoWindow? = null
    private var lastUrl: String = ""
    private var pipEnterAttempted = false
    private var pipStuck = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.black)
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
        }
        super.onCreate(savedInstanceState)
        val host = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(host)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LIVE"
        val win = FloatingVideoWindow(
            this, "overlay", title, url, true,
            onClosed = { finish() },
            onActivated = { },
            onSignInStarted = { },
            onSignInFinished = { },
            chromeLess = true
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
        pipWindow = win
        lastUrl = url
        win.hideChrome()
        win.lowerVolume()
        scheduleEnterPipAfterLayout(win)
    }

    override fun onResume() {
        super.onResume()
        pipWindow?.consumeSignInIfNeeded()
        pipWindow?.hideChrome()
        val win = pipWindow ?: return
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
            pipWindow?.setTitle(title)
            pipWindow?.load(url)
            pipWindow?.lowerVolume()
        }
        pipWindow?.hideChrome()
        if (!isInPictureInPictureMode) {
            pipEnterAttempted = false
            pipWindow?.let { scheduleEnterPipAfterLayout(it) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            pipStuck = true
            pipWindow?.hideChrome()
            return
        }
        if (!isInPictureInPictureMode && isFinishing) return
        // OEM bounced PiP without ever sticking — do not leave a fullscreen tap-sink.
        if (pipEnterAttempted && !pipStuck && !isFinishing) {
            finish()
        }
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
                return
            }
            // Motorola can return true then never stick. If we stay fullscreen, leave.
            win.postDelayed({
                if (!isFinishing && !isInPictureInPictureMode && !pipStuck) {
                    finish()
                }
            }, 1200)
        } catch (_: Exception) {
            finish()
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
