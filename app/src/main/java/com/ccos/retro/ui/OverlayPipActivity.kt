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
 * Never requests AUDIOFOCUS_GAIN.
 *
 * Razr hop Chris uses: Home / leave. setAutoEnterEnabled makes
 * supportsEnterPipOnTaskSwitch=true. onUserLeaveHint enters if the immediate
 * enterPictureInPictureMode did not stick. If we cannot PiP at all, finish()
 * so HUD plates stay tappable.
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
        // Arm task-switch PiP BEFORE any leave. Razr dumpsys was
        // supportsEnterPipOnTaskSwitch=false without this.
        armAutoEnter(win)
        scheduleEnterPipAfterLayout(win)
    }

    override fun onResume() {
        super.onResume()
        pipWindow?.hideChrome()
        val win = pipWindow ?: return
        armAutoEnter(win)
        if (win.width > 0 && win.height > 0) {
            enterPipFromLaidOutWindow(win, fromLeave = false)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isFinishing || isInPictureInPictureMode) return
        val win = pipWindow
        if (win == null) {
            if (Build.VERSION.SDK_INT < 26) finish()
            return
        }
        // Home / recents — Chris's Razr hop. Immediate enter often returns false.
        enterPipFromLaidOutWindow(win, fromLeave = true)
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
            pipWindow?.let {
                armAutoEnter(it)
                scheduleEnterPipAfterLayout(it)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            pipStuck = true
            pipWindow?.hideChrome()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
    }

    private fun scheduleEnterPipAfterLayout(win: View) {
        val afterLayout = Runnable {
            if (isFinishing) return@Runnable
            if (win.width > 0 && win.height > 0) {
                armAutoEnter(win)
                enterPipFromLaidOutWindow(win, fromLeave = false)
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
                    armAutoEnter(win)
                    enterPipFromLaidOutWindow(win, fromLeave = false)
                }
            })
        }
        win.post(afterLayout)
    }

    /** Keeps supportsEnterPipOnTaskSwitch true so Home/leave can hop like MCC. */
    private fun armAutoEnter(win: View?) {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            setPictureInPictureParams(buildPipParams(win))
        } catch (_: Exception) {
        }
    }

    private fun buildPipParams(win: View?): PictureInPictureParams {
        val b = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (win != null && win.width > 1 && win.height > 1) {
            val loc = IntArray(2)
            win.getLocationOnScreen(loc)
            val hint = Rect(loc[0], loc[1], loc[0] + win.width, loc[1] + win.height)
            if (hint.width() >= 2 && hint.height() >= 2) {
                b.setSourceRectHint(hint)
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            b.setAutoEnterEnabled(true)
            b.setSeamlessResizeEnabled(true)
        }
        return b.build()
    }

    private fun enterPipFromLaidOutWindow(win: View, fromLeave: Boolean) {
        if (isFinishing || isInPictureInPictureMode) return
        if (Build.VERSION.SDK_INT < 26) {
            finish()
            return
        }
        if (!fromLeave && pipEnterAttempted) return
        if (!fromLeave && !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (!fromLeave) pipEnterAttempted = true
        val loc = IntArray(2)
        win.getLocationOnScreen(loc)
        val hint = Rect(loc[0], loc[1], loc[0] + win.width, loc[1] + win.height)
        if (hint.width() < 2 || hint.height() < 2) {
            armAutoEnter(win)
            if (fromLeave) finish()
            return
        }
        try {
            val params = buildPipParams(win)
            setPictureInPictureParams(params)
            val entered = enterPictureInPictureMode(params)
            if (entered) {
                pipStuck = true
                return
            }
            // Immediate enter failed (Razr). Keep auto-enter armed for Home/leave.
            // Only finish on the leave path if that hop also failed — no tap-sink task.
            if (fromLeave && !pipStuck) {
                finish()
            }
        } catch (_: Exception) {
            if (fromLeave || Build.VERSION.SDK_INT < 26) finish()
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
