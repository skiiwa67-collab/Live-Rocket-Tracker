package com.ccos.retro.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ccos.retro.R
import com.ccos.retro.data.LaunchDataProvider
import com.ccos.retro.data.WebcastResolver
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.event.EventSeverity
import com.ccos.retro.event.FlightEventMonitor
import com.ccos.retro.event.KineticFx
import com.ccos.retro.module.RocketTelemetryModule
import com.ccos.retro.geo.GeoAtlas
import com.ccos.retro.geo.PadBook

/**
 * Full-screen Mission Control Center. Wallpaper stays the home HUD.
 * Status / camera cutout are padded — never drawn under the punch-hole.
 * VID is this activity's full-screen WebView. PiP is this same WebView
 * via enterPictureInPictureMode — not OverlayPip, not a second player.
 */
class CommandCenterActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var telemetryModule: RocketTelemetryModule
    private lateinit var console: CommandConsoleView
    private lateinit var analogBtn: Button
    private lateinit var vidBtn: Button
    private lateinit var chrome: View
    private lateinit var vidWeb: WebView
    private lateinit var vidPipBar: View
    private lateinit var pipBtn: Button
    private var vidShowing = false
    private var lastVidUrl: String = ""
    private var pipAfterResume = false
    private var pipMuted = false
    private var pipCcOn = false
    /** true = try stamp-26 280x168 compact; false = current big Razr ~1120x630. */
    private var pipCompact = true
    private var pipReceiverRegistered = false
    private lateinit var eventBanner: EventBannerView
    private val eventMonitor = FlightEventMonitor()
    private lateinit var kinetic: KineticFx
    private val handler = Handler(Looper.getMainLooper())
    private var lastSimTickMs = 0L
    private var running = false

    private val tabIds = intArrayOf(
        R.id.tab_tel, R.id.tab_traj, R.id.tab_stg1, R.id.tab_stg2,
        R.id.tab_eng, R.id.tab_prop, R.id.tab_pad
    )

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.currentTimeMillis()
            if (telemetryModule.simSecondsFromNet != null) {
                val dt = (now - lastSimTickMs).coerceIn(0L, 50L) / 1000f
                lastSimTickMs = now
                telemetryModule.tickSim(dt)
            } else {
                lastSimTickMs = now
            }
            pushFlightEvents()
            console.invalidate()
            handler.postDelayed(this, 100L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_command_center)
        applyImmersive()

        GeoAtlas.ensure(this)
        PadBook.ensure(this)
        kinetic = KineticFx(this)
        prefs = AppPrefs(this)
        telemetryModule = RocketTelemetryModule(prefs, LaunchDataProvider())

        console = findViewById(R.id.console_view)
        console.bind(telemetryModule, prefs)
        console.onScreenChanged = { highlightTabs(it) }

        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            if (vidShowing) enterVidPip() else finish()
        }
        analogBtn = findViewById(R.id.btn_analog)
        analogBtn.setOnClickListener {
            prefs.telemetryAnalog = !prefs.telemetryAnalog
            updateAnalogButton()
            console.invalidate()
        }
        console.onAnalogChanged = { updateAnalogButton() }
        updateAnalogButton()

        eventBanner = findViewById(R.id.event_banner)
        chrome = findViewById(R.id.chrome)
        vidWeb = findViewById(R.id.vid_web)
        vidPipBar = findViewById(R.id.vid_pip_bar)
        pipBtn = findViewById(R.id.btn_pip)
        bindVidWeb()
        pipBtn.setOnClickListener { enterVidPip() }
        vidWeb.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (vidShowing) updatePipParams()
        }
        findViewById<Button>(R.id.btn_event_prev).setOnClickListener {
            telemetryModule.skipEvent(-1)
            pushFlightEvents()
            console.invalidate()
        }
        findViewById<Button>(R.id.btn_event_next).setOnClickListener {
            telemetryModule.skipEvent(1)
            pushFlightEvents()
            console.invalidate()
        }
        vidBtn = findViewById(R.id.btn_vid)
        vidBtn.setOnClickListener {
            if (vidShowing) {
                enterVidPip()
            } else {
                val launch = telemetryModule.tracked
                val feeds = WebcastResolver.panes(launch)
                openVid(
                    feeds.official.url,
                    if (feeds.official.isWatch) "LIVE" else feeds.official.title
                )
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vidShowing) {
                    enterVidPip()
                } else {
                    finish()
                }
            }
        })
        registerPipActions()
        consumeVidIntent(intent)
        updateVidButton()

        tabIds.forEachIndexed { index, id ->
            findViewById<TextView>(id).setOnClickListener {
                console.screen = index
                highlightTabs(index)
                console.invalidate()
            }
        }
        applyHardwareChrome()
        updateVidButton()
        updateAnalogButton()
        highlightTabs(console.screen)
    }

    private fun applyHardwareChrome() {
        val d = resources.displayMetrics.density
        fun steel(top: Int, bot: Int, stroke: Int, radiusDp: Float = 5f): GradientDrawable {
            val g = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bot))
            g.cornerRadius = radiusDp * d
            g.setStroke((1.4f * d).toInt().coerceAtLeast(1), stroke)
            return g
        }
        fun metalBtn(btn: Button, top: Int, bot: Int, stroke: Int, text: Int) {
            btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            btn.background = steel(top, bot, stroke)
            btn.setTextColor(text)
            btn.elevation = 5f * d
        }
        metalBtn(
            findViewById(R.id.btn_exit),
            0xFF7A141C.toInt(), 0xFF3A080C.toInt(), 0xFFD05058.toInt(), Color.WHITE
        )
        metalBtn(
            findViewById(R.id.btn_event_prev),
            0xFF1A3040.toInt(), 0xFF0C1820.toInt(), 0xFF3A5A70.toInt(), Color.WHITE
        )
        metalBtn(
            findViewById(R.id.btn_event_next),
            0xFF1A3040.toInt(), 0xFF0C1820.toInt(), 0xFF3A5A70.toInt(), Color.WHITE
        )
        if (this::pipBtn.isInitialized) {
            metalBtn(
                pipBtn,
                0xFF145A28.toInt(), 0xFF063014.toInt(), 0xFF3CFF7A.toInt(), 0xFFB6FFD0.toInt()
            )
        }
        val rail = steel(0xFF121820.toInt(), 0xFF070A0E.toInt(), 0xFF1E2A34.toInt(), 0f)
        findViewById<View>(R.id.top_chrome).background = rail
        findViewById<LinearLayout>(R.id.chrome).setBackgroundColor(0xFF070A0E.toInt())
        findViewById<View>(R.id.mcc_root).setBackgroundColor(0xFF070A0E.toInt())
    }

    override fun onResume() {
        super.onResume()
        applyImmersive()
        telemetryModule.ensureData()
        telemetryModule.resolveTracked()
        lastSimTickMs = System.currentTimeMillis()
        running = true
        handler.removeCallbacks(tick)
        handler.post(tick)
        if (this::vidWeb.isInitialized) vidWeb.onResume()
        updatePipParams()
        if (pipAfterResume && vidShowing) {
            pipAfterResume = false
            vidWeb.post { enterVidPip() }
        }
    }

    override fun onPause() {
        running = false
        handler.removeCallbacks(tick)
        CookieManager.getInstance().flush()
        super.onPause()
        if (inPip() && this::vidWeb.isInitialized) vidWeb.onResume()
    }

    override fun onStop() {
        super.onStop()
        if (inPip() && this::vidWeb.isInitialized) vidWeb.onResume()
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        if (this::vidWeb.isInitialized) {
            vidWeb.stopLoading()
            vidWeb.destroy()
        }
        if (this::kinetic.isInitialized) kinetic.release()
        unregisterPipActions()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeVidIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vidShowing) enterVidPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        applyVidSurface()
        if (isInPictureInPictureMode && this::vidWeb.isInitialized) vidWeb.onResume()
        if (isInPictureInPictureMode) {
            applyPipMuteCc()
            vidWeb.post { maybeSyncPipSizeFromWindow() }
        }
        updatePipParams()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (inPip()) {
            applyVidSurface()
            maybeSyncPipSizeFromWindow()
            if (this::vidWeb.isInitialized) vidWeb.onResume()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (vidShowing) {
            enterVidPip()
            return
        }
        finish()
    }

    private fun updateAnalogButton() {
        if (!this::analogBtn.isInitialized) return
        paintAnalogSwitch(prefs.telemetryAnalog)
    }

    /** Hardware lamp like VID. ANLG lit cyan, DIG dim. Testers learn it by clicking. */
    private fun paintAnalogSwitch(analog: Boolean) {
        val d = resources.displayMetrics.density
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (analog) intArrayOf(0xFF145A70.toInt(), 0xFF063040.toInt())
            else intArrayOf(0xFF163044.toInt(), 0xFF0A1822.toInt())
        )
        bg.cornerRadius = 6f * d
        bg.setStroke(
            (2.2f * d).toInt().coerceAtLeast(2),
            if (analog) 0xFF00D4FF.toInt() else 0xFF1A88AA.toInt()
        )
        analogBtn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        analogBtn.background = bg
        analogBtn.setTextColor(if (analog) 0xFFB8F4FF.toInt() else 0xFF8AA0B0.toInt())
        analogBtn.text = if (analog) "ANLG" else "DIG"
        analogBtn.textSize = 14f
        analogBtn.elevation = if (analog) 10f * d else 5f * d
        val lamp = GradientDrawable()
        lamp.shape = GradientDrawable.OVAL
        val ls = (11f * d).toInt().coerceAtLeast(10)
        lamp.setSize(ls, ls)
        if (analog) {
            lamp.setColor(0xFF00D4FF.toInt())
            lamp.setStroke((1.4f * d).toInt().coerceAtLeast(1), 0xFFD0F6FF.toInt())
        } else {
            lamp.setColor(0xFF1A2228.toInt())
            lamp.setStroke((1.2f * d).toInt().coerceAtLeast(1), 0xFF2A333C.toInt())
        }
        analogBtn.setCompoundDrawablesWithIntrinsicBounds(lamp, null, null, null)
        analogBtn.compoundDrawablePadding = (6f * d).toInt()
        analogBtn.gravity = Gravity.CENTER
        val pad = (8f * d).toInt()
        analogBtn.setPadding(pad, 0, pad, 0)
    }

    private fun updateVidButton() {
        if (!this::vidBtn.isInitialized) return
        paintVidSwitch(vidShowing)
    }

    /** Hardware lamp switch. Green + VID ON when feeds are up. Dark + VID when cold. */
    private fun paintVidSwitch(on: Boolean) {
        val d = resources.displayMetrics.density
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (on) intArrayOf(0xFF145A28.toInt(), 0xFF063014.toInt())
            else intArrayOf(0xFF163044.toInt(), 0xFF0A1822.toInt())
        )
        bg.cornerRadius = 6f * d
        bg.setStroke(
            (2.2f * d).toInt().coerceAtLeast(2),
            if (on) 0xFF3CFF7A.toInt() else 0xFF1A88AA.toInt()
        )
        vidBtn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        vidBtn.background = bg
        vidBtn.setTextColor(if (on) 0xFFB6FFD0.toInt() else 0xFF8AA0B0.toInt())
        vidBtn.text = if (on) "VID  ON" else "VID"
        vidBtn.textSize = 15f
        vidBtn.elevation = if (on) 10f * d else 5f * d
        val lamp = GradientDrawable()
        lamp.shape = GradientDrawable.OVAL
        val ls = (11f * d).toInt().coerceAtLeast(10)
        lamp.setSize(ls, ls)
        if (on) {
            lamp.setColor(0xFF3CFF7A.toInt())
            lamp.setStroke((1.4f * d).toInt().coerceAtLeast(1), 0xFFD0FFE0.toInt())
        } else {
            lamp.setColor(0xFF1A2228.toInt())
            lamp.setStroke((1.2f * d).toInt().coerceAtLeast(1), 0xFF2A333C.toInt())
        }
        vidBtn.setCompoundDrawablesWithIntrinsicBounds(lamp, null, null, null)
        vidBtn.compoundDrawablePadding = (8f * d).toInt()
        vidBtn.gravity = Gravity.CENTER
        val pad = (10f * d).toInt()
        vidBtn.setPadding(pad, 0, pad, 0)
    }

    private fun pushFlightEvents() {
        if (!this::console.isInitialized) return
        val launch = telemetryModule.tracked
        val tSec = telemetryModule.effectiveSecondsFromNet()
        console.eventTape = eventMonitor.occurred(launch, tSec)
        val fresh = eventMonitor.poll(launch, tSec)
        val liveWatch = if (this::kinetic.isInitialized) {
            kinetic.shouldAlertOnWallpaper(launch, tSec, telemetryModule.simSecondsFromNet)
        } else false
        for (e in fresh) {
            if (this::eventBanner.isInitialized) eventBanner.push(e)
            if (this::kinetic.isInitialized && liveWatch) kinetic.play(e)
            if (e.severity == EventSeverity.FAIL) {
                console.failedSystem = e.failedSystem
                if (this::vidWeb.isInitialized) {
                    val feeds = WebcastResolver.panes(launch)
                    openVid(feeds.official.url, feeds.official.title)
                }
            }
        }
        val failNow = com.ccos.retro.event.FlightEventCatalog.failureFromStatus(launch, tSec)
        console.failedSystem = failNow?.failedSystem
    }

    private fun applyImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the nav bar for an MCC look, but NEVER consume the status bar /
        // display cutout — chrome pads so STG2 is never under the camera.
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let { c ->
                c.hide(WindowInsets.Type.navigationBars())
                c.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
        val chrome = findViewById<View>(R.id.chrome)
        if (chrome != null) {
            ViewCompat.setOnApplyWindowInsetsListener(chrome) { v, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.navigationBars()
                )
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(chrome)
        }
        applyVidPipBarInsets()
    }

    /** PIP chip sits below statusBars / cutout. Not y=0 under signal/battery. */
    private fun applyVidPipBarInsets() {
        val bar = if (this::vidPipBar.isInitialized) vidPipBar
            else findViewById(R.id.vid_pip_bar) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(bar) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            val lp = v.layoutParams
            if (lp is FrameLayout.LayoutParams) {
                lp.topMargin = bars.top
                lp.marginEnd = bars.right
                lp.marginStart = bars.left
                v.layoutParams = lp
            } else {
                val pad = (8f * resources.displayMetrics.density).toInt()
                v.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad)
            }
            insets
        }
        ViewCompat.requestApplyInsets(bar)
    }

    private fun highlightTabs(screen: Int) {
        val d = resources.displayMetrics.density
        tabIds.forEachIndexed { i, id ->
            val tv = findViewById<TextView>(id)
            val on = i == screen
            val bg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                if (on) intArrayOf(0xFF16344A.toInt(), 0xFF0A1824.toInt())
                else intArrayOf(0xFF141A22.toInt(), 0xFF080C10.toInt())
            )
            bg.cornerRadius = 5f * d
            if (on) {
                bg.setStroke((2f * d).toInt().coerceAtLeast(2), 0xFF00D4FF.toInt())
                tv.setTextColor(0xFF00D4FF.toInt())
            } else {
                bg.setStroke((1f * d).toInt().coerceAtLeast(1), 0xFF243038.toInt())
                tv.setTextColor(0xFF8AA0B0.toInt())
            }
            tv.background = bg
            tv.gravity = Gravity.CENTER
            // Lit lamp sits ABOVE the label so it never covers TEL/TRAJ/…
            val lamp = GradientDrawable()
            lamp.shape = GradientDrawable.OVAL
            val ls = (7f * d).toInt().coerceAtLeast(6)
            lamp.setSize(ls, ls)
            if (on) {
                lamp.setColor(0xFF2ECC71.toInt())
                lamp.setStroke((1.1f * d).toInt().coerceAtLeast(1), 0xFF88FFBB.toInt())
            } else {
                lamp.setColor(0xFF1A2228.toInt())
                lamp.setStroke((1f * d).toInt().coerceAtLeast(1), 0xFF2A333C.toInt())
            }
            tv.setCompoundDrawablesWithIntrinsicBounds(null, lamp, null, null)
            tv.compoundDrawablePadding = (2f * d).toInt()
            tv.setPadding(0, (3f * d).toInt(), 0, (2f * d).toInt())
        }
        findViewById<TextView>(R.id.tab_pad).text = "MISS"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun bindVidWeb() {
        CookieManager.getInstance().setAcceptCookie(true)
        vidWeb.setBackgroundColor(Color.BLACK)
        vidWeb.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT >= 26) {
            vidWeb.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }
        vidWeb.settings.javaScriptEnabled = true
        vidWeb.settings.domStorageEnabled = true
        vidWeb.settings.databaseEnabled = true
        vidWeb.settings.mediaPlaybackRequiresUserGesture = false
        vidWeb.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        vidWeb.settings.loadWithOverviewMode = true
        vidWeb.settings.useWideViewPort = true
        vidWeb.settings.javaScriptCanOpenWindowsAutomatically = true
        vidWeb.settings.setSupportMultipleWindows(false)
        vidWeb.settings.cacheMode = WebSettings.LOAD_DEFAULT
        vidWeb.settings.userAgentString = chromeMobileUa(vidWeb.settings.userAgentString)
        CookieManager.getInstance().setAcceptThirdPartyCookies(vidWeb, true)
        vidWeb.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                if (inPip()) applyPipVideoFill(true)
                applyPipMuteCc()
            }
        }
        vidWeb.webChromeClient = WebChromeClient()
    }

    private fun consumeVidIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_VID, false) != true) return
        if (vidShowing && lastVidUrl.isNotBlank()) {
            applyVidSurface()
            updateVidButton()
            updatePipParams()
            if (!inPip()) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    vidWeb.post { enterVidPip() }
                } else {
                    pipAfterResume = true
                }
            }
            return
        }
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val launch = telemetryModule.tracked
        val feeds = WebcastResolver.panes(launch)
        val target = url.ifBlank { feeds.official.url }
        val label = title.ifBlank { if (feeds.official.isWatch) "LIVE" else feeds.official.title }
        openVid(target, label)
    }

    private fun openVid(url: String, title: String) {
        if (!this::vidWeb.isInitialized) return
        val target = url.ifBlank { return }
        vidShowing = true
        vidWeb.contentDescription = title.ifBlank { "VID" }
        if (target != lastVidUrl) {
            lastVidUrl = target
            vidWeb.loadUrl(target)
        }
        applyVidSurface()
        updateVidButton()
        updatePipParams()
        CookieManager.getInstance().flush()
    }

    private fun closeVid() {
        vidShowing = false
        CookieManager.getInstance().flush()
        applyVidSurface()
        updateVidButton()
        updatePipParams()
    }

    private fun inPip(): Boolean =
        Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode

    private fun applyVidSurface() {
        if (!this::vidWeb.isInitialized || !this::chrome.isInitialized) return
        val pip = inPip()
        title = if (pip) "" else getString(R.string.app_name)
        if (vidShowing) {
            vidWeb.visibility = View.VISIBLE
            chrome.visibility = View.GONE
            if (this::vidPipBar.isInitialized) {
                vidPipBar.visibility = if (pip) View.GONE else View.VISIBLE
            }
            applyPipVideoFill(pip)
            applyPipMuteCc()
        } else {
            vidWeb.visibility = View.GONE
            chrome.visibility = View.VISIBLE
            if (this::vidPipBar.isInitialized) vidPipBar.visibility = View.GONE
            applyPipVideoFill(false)
        }
    }

    /**
     * In system PiP the WebView is the whole window. Hide YouTube page chrome
     * (masthead, comments, related) and make the video element cover the
     * surface. Restore the watch page when leaving PiP. Does not load a
     * different URL or create a second player.
     */
    private fun applyPipVideoFill(fill: Boolean) {
        if (!this::vidWeb.isInitialized) return
        vidWeb.setPadding(0, 0, 0, 0)
        vidWeb.setBackgroundColor(Color.BLACK)
        try {
            vidWeb.evaluateJavascript(if (fill) PIP_FILL_JS else PIP_CLEAR_JS, null)
        } catch (_: Exception) {
        }
    }

    private fun enterVidPip() {
        if (Build.VERSION.SDK_INT < 26 || !vidShowing) return
        if (inPip()) {
            applyVidSurface()
            if (this::vidWeb.isInitialized) vidWeb.onResume()
            return
        }
        updatePipParams()
        try {
            val entered = enterPictureInPictureMode(pipParams())
            if (!entered) {
                applyVidSurface()
                if (this::vidWeb.isInitialized) vidWeb.onResume()
            }
        } catch (_: Exception) {
            applyVidSurface()
            if (this::vidWeb.isInitialized) vidWeb.onResume()
        }
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            setPictureInPictureParams(pipParams())
        } catch (_: Exception) {
        }
    }

    private fun pipParams(): PictureInPictureParams {
        val b = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        // Compact = stamp-26 emu ~280x168. Expanded = current Razr pin ~1120x630.
        // OEM may ignore size; never invent OverlayPip to fake it.
        b.setSourceRectHint(pipSizeHint())
        if (Build.VERSION.SDK_INT >= 26) {
            b.setActions(pipRemoteActions())
        }
        if (Build.VERSION.SDK_INT >= 31) {
            b.setAutoEnterEnabled(vidShowing)
            b.setSeamlessResizeEnabled(true)
        }
        return b.build()
    }

    /**
     * Compact sourceRectHint is 280x168 px (stamp 26 emu look).
     * Expanded is 1120x630 (Motorola Razr current pin Rect 52,1606-1172,2236).
     */
    private fun pipSizeHint(): Rect {
        val dm = resources.displayMetrics
        val targetW = if (pipCompact) COMPACT_PIP_W else EXPANDED_PIP_W
        val targetH = if (pipCompact) COMPACT_PIP_H else EXPANDED_PIP_H
        val pad = (12f * dm.density).toInt()
        val left = (dm.widthPixels - targetW - pad).coerceAtLeast(0)
        val top = (dm.heightPixels - targetH - pad * 6).coerceAtLeast(0)
        if (this::vidWeb.isInitialized && vidWeb.width > 0 && vidWeb.height > 0 && !inPip()) {
            val loc = IntArray(2)
            vidWeb.getLocationOnScreen(loc)
            val w = vidWeb.width
            val h = (w * 9f / 16f).toInt().coerceAtMost(vidWeb.height)
            // Enter animation still starts from the playing video, then the
            // target size rect is the compact/expanded hint above.
            if (!pipCompact) {
                return Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
            }
        }
        return Rect(left, top, left + targetW, top + targetH)
    }

    private fun pipRemoteActions(): ArrayList<RemoteAction> {
        val actions = ArrayList<RemoteAction>(3)
        val muteIcon = if (pipMuted) R.drawable.ic_pip_mute else R.drawable.ic_pip_volume
        val muteLabel = if (pipMuted) "Unmute" else "Mute"
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, muteIcon),
                muteLabel,
                muteLabel,
                pipActionIntent(ACTION_PIP_MUTE, REQ_PIP_MUTE)
            )
        )
        val ccLabel = if (pipCcOn) "Captions on" else "Captions"
        val cc = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_pip_cc),
            ccLabel,
            "Closed captions",
            pipActionIntent(ACTION_PIP_CC, REQ_PIP_CC)
        )
        cc.setEnabled(true)
        actions.add(cc)
        val sizeIcon = if (pipCompact) R.drawable.ic_pip_size_out else R.drawable.ic_pip_size_in
        val sizeLabel = if (pipCompact) "Larger" else "Smaller"
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, sizeIcon),
                sizeLabel,
                if (pipCompact) "Expand PiP toward Razr size" else "Shrink PiP to 280x168",
                pipActionIntent(ACTION_PIP_SIZE, REQ_PIP_SIZE)
            )
        )
        return actions
    }

    private fun pipActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).setPackage(packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(this, requestCode, intent, flags)
    }

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_MUTE -> togglePipMute()
                ACTION_PIP_CC -> togglePipCc()
                ACTION_PIP_SIZE -> togglePipSize()
            }
        }
    }

    private fun registerPipActions() {
        if (pipReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_MUTE)
            addAction(ACTION_PIP_CC)
            addAction(ACTION_PIP_SIZE)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(pipActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(pipActionReceiver, filter)
        }
        pipReceiverRegistered = true
    }

    private fun unregisterPipActions() {
        if (!pipReceiverRegistered) return
        try {
            unregisterReceiver(pipActionReceiver)
        } catch (_: Exception) {
        }
        pipReceiverRegistered = false
    }

    private fun togglePipMute() {
        pipMuted = !pipMuted
        applyPipMuteCc()
        updatePipParams()
    }

    private fun togglePipCc() {
        pipCcOn = !pipCcOn
        applyPipMuteCc()
        updatePipParams()
    }

    /** Inward = 280x168 compact. Outward = current big Razr size. System chrome only. */
    private fun togglePipSize() {
        pipCompact = !pipCompact
        updatePipParams()
    }

    /**
     * If the user drags the system corner arrows, snap our compact flag to the
     * new window. Motorola may ignore the hint and stay pinned at ~1120x630.
     */
    private fun maybeSyncPipSizeFromWindow() {
        if (!inPip() || !this::vidWeb.isInitialized) return
        val w = vidWeb.width
        if (w <= 0) return
        val nextCompact = w <= (COMPACT_PIP_W + 80)
        if (nextCompact != pipCompact && w < EXPANDED_PIP_W - 80) {
            pipCompact = nextCompact
            updatePipParams()
        }
    }

    /** Mute = video.muted. CC clicks YouTube captions / textTracks. Same WebView. */
    private fun applyPipMuteCc() {
        if (!this::vidWeb.isInitialized) return
        try {
            vidWeb.evaluateJavascript(muteJs(pipMuted), null)
            vidWeb.evaluateJavascript(ccJs(pipCcOn), null)
        } catch (_: Exception) {
        }
    }

    private fun muteJs(muted: Boolean): String {
        val flag = if (muted) "true" else "false"
        return """
(function(){
  var want=$flag;
  var v=document.querySelector('video');
  if(v){
    v.muted=want;
    try{v.defaultMuted=want;}catch(e){}
    if(want){try{if(v.volume>0)v.dataset.lrtVol=v.volume; v.volume=0;}catch(e){}}
    else {try{v.volume=parseFloat(v.dataset.lrtVol||'1')||1;}catch(e){}}
  }
  var p=document.querySelector('#movie_player,.html5-video-player');
  try{
    if(p){
      if(want && p.mute) p.mute();
      if(!want && p.unMute) p.unMute();
    }
  }catch(e){}
  return want?'1':'0';
})();"""
    }

    private fun ccJs(on: Boolean): String {
        val flag = if (on) "true" else "false"
        return """
(function(){
  var want=$flag;
  var v=document.querySelector('video');
  if(v && v.textTracks){
    for(var i=0;i<v.textTracks.length;i++){
      try{v.textTracks[i].mode=want?'showing':'hidden';}catch(e){}
    }
  }
  var p=document.querySelector('#movie_player,.html5-video-player');
  try{
    if(p){
      if(want){
        try{if(p.loadModule)p.loadModule('captions');}catch(e){}
        try{if(p.setOption)p.setOption('captions','track',{languageCode:'en'});}catch(e){}
      } else {
        try{if(p.setOption)p.setOption('captions','track',{});}catch(e){}
        try{if(p.unloadModule)p.unloadModule('captions');}catch(e){}
      }
    }
  }catch(e){}
  var sel=[
    '.ytp-subtitles-button','button.ytp-subtitles-button',
    'button[aria-label*="captions" i]','button[aria-label*="Subtitles" i]',
    'button[aria-label*="Closed caption" i]','button[aria-label*="subtitle" i]',
    '.ytm-subtitle-button','ytm-subtitle-button'
  ];
  for(var s=0;s<sel.length;s++){
    var btn=document.querySelector(sel[s]);
    if(!btn) continue;
    var pressed=btn.getAttribute('aria-pressed')==='true' ||
      (btn.className||'').indexOf('ytp-button-toggled')>=0;
    if(pressed!==want){try{btn.click();}catch(e){}}
    break;
  }
  return want?'1':'0';
})();"""
    }

    private fun chromeMobileUa(current: String?): String {
        val raw = current.orEmpty()
        val stripped = raw.replace("; wv", "").replace(" Version/4.0", "")
        return if (stripped.contains("Chrome/")) stripped
        else "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    }

    companion object {
        const val EXTRA_OPEN_VID = "open_vid"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"

        const val ACTION_PIP_MUTE = "com.liverockettracker.action.PIP_MUTE"
        const val ACTION_PIP_CC = "com.liverockettracker.action.PIP_CC"
        const val ACTION_PIP_SIZE = "com.liverockettracker.action.PIP_SIZE"
        private const val REQ_PIP_MUTE = 3901
        private const val REQ_PIP_CC = 3902
        private const val REQ_PIP_SIZE = 3903
        private const val COMPACT_PIP_W = 280
        private const val COMPACT_PIP_H = 168
        private const val EXPANDED_PIP_W = 1120
        private const val EXPANDED_PIP_H = 630

        private const val PIP_FILL_JS = """
(function(){
  var id='lrt-pip-fill';
  var s=document.getElementById(id);
  if(!s){s=document.createElement('style');s.id=id;document.documentElement.appendChild(s);}
  s.textContent=[
    'html,body,ytd-app,ytm-app,#content,#page-manager,ytd-watch-flexy,ytm-watch{background:#000!important;margin:0!important;padding:0!important;overflow:hidden!important;width:100%!important;height:100%!important;}',
    '#masthead-container,ytd-masthead,ytm-mobile-topbar-renderer,ytm-header-bar,#header,header,ytm-pivot-bar-renderer,#guide,#secondary,#related,#comments,ytd-comments,ytm-comment-section-renderer,#below,#meta,#info,#chat,ytd-live-chat-frame,ytd-watch-metadata,ytm-slim-video-metadata-section-renderer,ytm-slim-owner-renderer,ytm-item-section-renderer,ytm-engagement-panel,ytd-engagement-panel-section-list-renderer,#player-ads,.ytp-ce-element,.ytp-pause-overlay,.ytp-endscreen-content,.ytp-chrome-top,.ytp-chrome-bottom,.ytp-gradient-top,.ytp-gradient-bottom,.ytp-title,ytm-chip-cloud-renderer{display:none!important;visibility:hidden!important;height:0!important;}',
    '#player,#player-container,#player-container-inner,#player-container-outer,#player-theater-container,ytd-player,#ytd-player,#movie_player,.html5-video-player,ytm-player,.player-container,#player-container-id{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;z-index:2147483647!important;background:#000!important;}',
    '.html5-video-container,video,video.html5-main-video,.html5-main-video{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;object-fit:cover!important;object-position:center!important;background:#000!important;}'
  ].join('');
  var v=document.querySelector('video');
  if(v){v.style.objectFit='cover';v.style.width='100vw';v.style.height='100vh';try{v.play();}catch(e){}}
})();"""

        private const val PIP_CLEAR_JS = """
(function(){
  var s=document.getElementById('lrt-pip-fill');
  if(s)s.remove();
})();"""
    }
}
