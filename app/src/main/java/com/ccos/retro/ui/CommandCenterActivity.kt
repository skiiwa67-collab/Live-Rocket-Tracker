package com.ccos.retro.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
 * Status / camera cutout are padded - never drawn under the punch-hole.
 * VID is one YouTube WebView filling the console well - no dual popout panes,
 * no SignIn/YT/-/+/X chrome, no resize triangles.
 */
class CommandCenterActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var telemetryModule: RocketTelemetryModule
    private lateinit var console: CommandConsoleView
    private lateinit var analogBtn: Button
    private lateinit var vidBtn: Button
    private lateinit var vidWeb: WebView
    private var vidShowing = false
    private var lastVidUrl: String = ""
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
                val dt = (now - lastSimTickMs).coerceIn(0L, 1000L) / 1000f
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

        findViewById<Button>(R.id.btn_exit).setOnClickListener { finish() }
        analogBtn = findViewById(R.id.btn_analog)
        analogBtn.setOnClickListener {
            prefs.telemetryAnalog = !prefs.telemetryAnalog
            updateAnalogButton()
            console.invalidate()
        }
        console.onAnalogChanged = { updateAnalogButton() }
        updateAnalogButton()

        eventBanner = findViewById(R.id.event_banner)
        vidWeb = findViewById(R.id.vid_web)
        bindVidWeb()
        consumeVidIntent(intent)
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
                closeVid()
            } else {
                val launch = telemetryModule.tracked
                val feeds = WebcastResolver.panes(launch)
                openVid(feeds.official.url)
            }
        }
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
        val rail = steel(0xFF121820.toInt(), 0xFF070A0E.toInt(), 0xFF1E2A34.toInt(), 0f)
        findViewById<View>(R.id.top_chrome).background = rail
        findViewById<LinearLayout>(R.id.chrome).setBackgroundColor(0xFF070A0E.toInt())
        findViewById<View>(R.id.mcc_root).setBackgroundColor(0xFF070A0E.toInt())
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
            }
        }
        vidWeb.webChromeClient = WebChromeClient()
    }

    private fun openVid(url: String) {
        if (!this::vidWeb.isInitialized) return
        val target = url.ifBlank { return }
        vidShowing = true
        if (target != lastVidUrl) {
            lastVidUrl = target
            vidWeb.loadUrl(target)
        }
        vidWeb.visibility = View.VISIBLE
        vidWeb.onResume()
        updateVidButton()
        CookieManager.getInstance().flush()
    }

    private fun closeVid() {
        vidShowing = false
        CookieManager.getInstance().flush()
        try {
            vidWeb.evaluateJavascript(
                "try{document.querySelectorAll('video').forEach(function(v){v.pause()})}catch(e){}",
                null
            )
        } catch (_: Exception) {
        }
        vidWeb.onPause()
        vidWeb.visibility = View.GONE
        updateVidButton()
    }

    private fun ensureVid(url: String) {
        // Stamp 53: always load into the existing single well (retarget in place).
        openVid(url)
    }

    private fun chromeMobileUa(current: String?): String {
        val raw = current.orEmpty()
        val stripped = raw.replace("; wv", "").replace(" Version/4.0", "")
        return if (stripped.contains("Chrome/")) stripped
        else "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
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
        if (this::vidWeb.isInitialized && vidShowing) vidWeb.onResume()
    }

    override fun onPause() {
        running = false
        handler.removeCallbacks(tick)
        if (this::vidWeb.isInitialized) {
            vidWeb.onPause()
            CookieManager.getInstance().flush()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (this::vidWeb.isInitialized) {
            vidWeb.stopLoading()
            vidWeb.loadUrl("about:blank")
            vidWeb.onPause()
            vidWeb.removeAllViews()
            vidWeb.destroy()
        }
        if (this::kinetic.isInitialized) kinetic.release()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeVidIntent(intent)
    }

    private fun consumeVidIntent(intent: Intent?) {
        if (intent == null) return
        if (!intent.getBooleanExtra(EXTRA_OPEN_VID, false)) return
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isBlank()) return
        openVid(url)
        // One-shot so rotate / resume does not re-toggle.
        intent.removeExtra(EXTRA_OPEN_VID)
        intent.removeExtra(EXTRA_URL)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (vidShowing) {
            closeVid()
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
                    ensureVid(feeds.official.url)
                }
            }
        }
        val failNow = com.ccos.retro.event.FlightEventCatalog.failureFromStatus(launch, tSec)
        console.failedSystem = failNow?.failedSystem
    }

    private fun applyImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the nav bar for an MCC look, but NEVER consume the status bar /
        // display cutout \u2014 chrome pads so STG2 is never under the camera.
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
        val chrome = findViewById<View>(R.id.chrome) ?: return
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
            // Lit lamp sits ABOVE the label so it never covers TEL/TRAJ/\u2014¦
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
    companion object {
        const val EXTRA_OPEN_VID = "com.ccos.retro.EXTRA_OPEN_VID"
        const val EXTRA_URL = "com.ccos.retro.EXTRA_URL"
    }
}