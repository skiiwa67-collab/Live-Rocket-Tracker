package com.ccos.retro.ui

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
import com.ccos.retro.event.MissionFacts
import com.ccos.retro.event.FlightEventMonitor
import com.ccos.retro.event.KineticFx
import com.ccos.retro.module.RocketTelemetryModule
import com.ccos.retro.geo.GeoAtlas
import com.ccos.retro.geo.PadBook

/**
 * Full-screen Mission Control Center. Wallpaper stays the home HUD.
 * Status / camera cutout are padded — never drawn under the punch-hole.
 */
class CommandCenterActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var telemetryModule: RocketTelemetryModule
    private lateinit var console: CommandConsoleView
    private lateinit var analogBtn: Button
    private lateinit var vidBtn: Button
    private lateinit var videoOverlay: VideoFeedOverlay
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
        videoOverlay = findViewById(R.id.video_overlay)
        videoOverlay.onChanged = { updateVidButton() }
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
            val launch = telemetryModule.tracked
            val feeds = WebcastResolver.panes(launch)
            if (videoOverlay.isShowing()) {
                videoOverlay.closeAll()
            } else {
                videoOverlay.switchFeed(
                    feeds.official.url,
                    if (feeds.official.isWatch) "LIVE" else feeds.official.title
                )
            }
            updateVidButton()
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

    override fun onResume() {
        super.onResume()
        applyImmersive()
        telemetryModule.ensureData()
        telemetryModule.resolveTracked()
        lastSimTickMs = System.currentTimeMillis()
        running = true
        handler.removeCallbacks(tick)
        handler.post(tick)
        if (this::videoOverlay.isInitialized) videoOverlay.resumeAll()
    }

    override fun onPause() {
        running = false
        handler.removeCallbacks(tick)
        if (this::videoOverlay.isInitialized) {
            videoOverlay.pauseAll()
            videoOverlay.flushCookies()
        }
        super.onPause()
    }

    /**
     * Chris's Razr path: sign in on this big MCC VID, then Home/leave.
     * Hand the same URL to OverlayPip so wallpaper HUD is already system PiP.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isFinishing) return
        if (!this::videoOverlay.isInitialized || !videoOverlay.isShowing()) return
        val feed = videoOverlay.currentFeed() ?: return
        OverlayPip.switch(this, feed.first, feed.second)
        videoOverlay.closeAll()
        updateVidButton()
    }

    override fun onDestroy() {
        if (this::videoOverlay.isInitialized) videoOverlay.destroyAll()
        if (this::kinetic.isInitialized) kinetic.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
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
        val on = this::videoOverlay.isInitialized && videoOverlay.isShowing()
        paintVidSwitch(on)
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
                if (this::videoOverlay.isInitialized) {
                    val feeds = WebcastResolver.panes(launch)
                    videoOverlay.ensureFeeds(
                        feeds.official.url,
                        feeds.nsf.url,
                        feeds.official.title,
                        feeds.nsf.title
                    )
                    updateVidButton()
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
}
