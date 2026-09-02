package com.ccos.retro.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * Stamp 26 HUD overlay PiP. One FloatingVideoWindow. Enter PiP once.
 * Do not re-enter. Do not spawn a second chrome window.
 * YouTube keeps audio. Never AUDIOFOCUS_GAIN.
 */
class OverlayPipActivity : AppCompatActivity() {

    private var window: FloatingVideoWindow? = null
    private var lastUrl: String = ""

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
        enterPip()
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
        enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode && isFinishing) return
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setSourceRectHint(Rect(0, 0, 16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
