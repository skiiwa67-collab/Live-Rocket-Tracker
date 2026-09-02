package com.ccos.retro.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.webkit.CookieManager
import android.widget.FrameLayout

/**
 * Activity-level overlay so video windows survive Command Center tab swipes.
 * Empty space does not steal touches — gauges and tabs stay live underneath.
 * Only the focused pane stays resumed so two YouTube players do not kill each other.
 */
class VideoFeedOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val windows = LinkedHashMap<String, FloatingVideoWindow>()
    var onChanged: (() -> Unit)? = null

    init {
        CookieManager.getInstance().setAcceptCookie(true)
        isClickable = false
        isFocusable = false
        clipChildren = false
        clipToPadding = false
    }

    /**
     * VID tap cycle. One overlay PiP only. Second tap closes. Links switch the same pane.
     * No extra MCC YouTube panes.
     */
    fun toggleFeeds(
        primaryUrl: String,
        secondaryUrl: String,
        primaryTitle: String = "OFFICIAL",
        secondaryTitle: String = "NASASPACEFLIGHT"
    ) {
        if (windows.isEmpty()) {
            openFeed("official", primaryTitle, primaryUrl, corner = 0, autoplay = true)
            layoutPanes()
        } else {
            closeAll()
        }
        onChanged?.invoke()
    }

    fun switchFeed(url: String, title: String) {
        val existing = windows.values.firstOrNull()
        if (existing != null) {
            existing.setTitle(title)
            existing.load(url)
            existing.lowerVolume()
            existing.bringToFront()
            focus(existing)
        } else {
            openFeed("official", title, url, corner = 0, autoplay = true)
            layoutPanes()
        }
        onChanged?.invoke()
    }

    fun openFeed(id: String, title: String, url: String, corner: Int, autoplay: Boolean = (id == "official")) {
        windows[id]?.let {
            it.load(url)
            it.bringToFront()
            focus(it)
            return
        }
        val win = FloatingVideoWindow(
            context, id, title, url, autoplay,
            onClosed = { closed ->
                windows.remove(closed.feedId)
                removeView(closed)
                CookieManager.getInstance().flush()
                layoutPanes()
                onChanged?.invoke()
            },
            onActivated = { focus(it) },
            onSignInStarted = { hibernateOthers(it) },
            onSignInFinished = {
                CookieManager.getInstance().flush()
                wakeOthers(it)
            }
        )
        val lp = LayoutParams(100, 100).apply { gravity = Gravity.TOP or Gravity.START }
        windows[id] = win
        addView(win, lp)
        win.lowerVolume()
        layoutPanes()
        onChanged?.invoke()
    }

    /** FAIL / auto path: one official pane only. Second pane is a second VID tap. */
    fun ensureFeeds(
        primaryUrl: String,
        secondaryUrl: String,
        primaryTitle: String = "OFFICIAL",
        secondaryTitle: String = "NASASPACEFLIGHT"
    ) {
        if (windows.isEmpty()) {
            openFeed("official", primaryTitle, primaryUrl, corner = 0, autoplay = true)
            layoutPanes()
        }
    }

    private fun layoutPanes() {
        if (windows.isEmpty()) return
        val d = resources.displayMetrics.density
        val pw = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val ph = height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val gutter = (8 * d).toInt()
        val two = windows.size >= 2
        val ww = if (two) {
            (pw * 0.47f).toInt().coerceAtLeast((176 * d).toInt())
        } else {
            (pw * 0.86f).toInt().coerceAtLeast((220 * d).toInt())
        }
        val wh = if (two) {
            (ph * 0.30f).toInt().coerceAtLeast((160 * d).toInt())
        } else {
            (ph * 0.52f).toInt().coerceAtLeast((200 * d).toInt())
        }
        val topBand = (ph - wh - gutter).coerceAtLeast(gutter)
        windows.entries.forEachIndexed { i, (_, win) ->
            val lp = (win.layoutParams as? LayoutParams) ?: LayoutParams(ww, wh)
            lp.width = ww
            lp.height = wh
            lp.gravity = Gravity.TOP or Gravity.START
            if (!two) {
                lp.leftMargin = ((pw - ww) / 2).coerceAtLeast(gutter)
                lp.topMargin = ((ph - wh) / 2).coerceAtLeast(gutter)
            } else if (i == 0) {
                lp.leftMargin = gutter
                lp.topMargin = topBand
            } else {
                lp.leftMargin = (pw - ww - gutter).coerceAtLeast(gutter)
                lp.topMargin = topBand
            }
            win.layoutParams = lp
        }
    }

    private fun focus(active: FloatingVideoWindow) {
        windows.values.forEach { w ->
            if (w !== active) w.pausePlayback() else w.resume()
        }
        active.bringToFront()
    }

    private fun hibernateOthers(active: FloatingVideoWindow) {
        // Google login in one WebView deadlocks a second live YouTube renderer.
        windows.values.forEach { w ->
            if (w !== active) w.hibernate()
        }
        active.bringToFront()
    }

    private fun wakeOthers(active: FloatingVideoWindow) {
        windows.values.forEach { w ->
            if (w !== active) w.wake()
        }
    }

    fun closeAll() {
        CookieManager.getInstance().flush()
        windows.values.toList().forEach { it.close() }
        windows.clear()
        removeAllViews()
    }

    fun pauseAll() {
        CookieManager.getInstance().flush()
        windows.values.forEach { it.freezeRenderer() }
    }

    fun flushCookies() {
        CookieManager.getInstance().flush()
    }

    fun resumeAll() {
        val last = windows.values.lastOrNull() ?: return
        focus(last)
    }

    fun destroyAll() {
        CookieManager.getInstance().flush()
        windows.values.forEach { it.destroyFeed() }
        windows.clear()
        removeAllViews()
    }

    fun isShowing(): Boolean = windows.isNotEmpty()

    fun currentFeed(): Pair<String, String>? {
        val w = windows.values.firstOrNull() ?: return null
        val url = w.feedUrl()
        if (url.isBlank()) return null
        return url to w.feedTitle().ifBlank { "LIVE" }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (windows.isEmpty()) return false
        if (ev.actionMasked == MotionEvent.ACTION_DOWN && !hitWindow(ev)) return false
        return super.dispatchTouchEvent(ev)
    }

    private fun hitWindow(ev: MotionEvent): Boolean {
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        for (i in childCount - 1 downTo 0) {
            val c = getChildAt(i)
            if (c.visibility == VISIBLE &&
                x >= c.left && x < c.right &&
                y >= c.top && y < c.bottom
            ) return true
        }
        return false
    }
}
