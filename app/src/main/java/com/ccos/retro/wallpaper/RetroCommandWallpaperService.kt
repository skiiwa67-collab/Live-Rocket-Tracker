package com.ccos.retro.wallpaper

import android.content.Intent
import android.text.format.DateFormat
import android.graphics.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.wallpaper.WallpaperService
import android.os.Build
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowInsets
import com.ccos.retro.BuildConfig
import com.ccos.retro.data.LaunchDataProvider
import com.ccos.retro.data.SystemMetricsProvider
import com.ccos.retro.data.WebcastResolver
import com.ccos.retro.engine.NanoParticle
import com.ccos.retro.engine.SystemMetricsRenderer
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.model.BuildState
import com.ccos.retro.module.ModuleCatalog
import com.ccos.retro.module.ModuleLicense
import com.ccos.retro.module.ModuleRegistry
import com.ccos.retro.event.EngineDraw
import com.ccos.retro.event.EventClock
import com.ccos.retro.event.VehicleDraw
import com.ccos.retro.event.VehicleOutline
import com.ccos.retro.event.FlightEventCatalog
import com.ccos.retro.event.FlightEventMonitor
import com.ccos.retro.event.KineticFx
import com.ccos.retro.geo.PadBook
import com.ccos.retro.geo.PadGlyph
import com.ccos.retro.event.MissionFacts
import com.ccos.retro.geo.GeoAtlas
import com.ccos.retro.geo.GeoDraw
import com.ccos.retro.module.RocketTelemetryModule
import com.ccos.retro.module.SliderDef
import com.ccos.retro.module.SystemMetricsModule
import com.ccos.retro.module.VectorRocketModule
import com.ccos.retro.skin.RetroSkin
import com.ccos.retro.skin.TelemetrySkin
import com.ccos.retro.ui.MainActivity
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


/**
 * CCOS Live Wallpaper – Retro Command Center surface.
 * Renders the active module (Vector Rocket) using pure vector graphics.
 * Skin tokens come from RetroSkin. Telemetry from SystemMetricsProvider.
 * Engine stays thin; module owns its own interaction model.
 */
class RetroCommandWallpaperService : WallpaperService() {

    // Moved outside the inner class – Kotlin does not allow data classes inside inner classes
    private data class LiveSlider(
        val def: SliderDef,
        val rect: RectF,
        var value: Float
    )

    override fun onCreateEngine(): Engine = RetroEngine()

    inner class RetroEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private lateinit var state: BuildState
        private lateinit var prefs: AppPrefs
        private lateinit var metrics: SystemMetricsProvider
        private lateinit var rocketModule: VectorRocketModule
        private lateinit var systemModule: SystemMetricsModule
        private lateinit var telemetryModule: RocketTelemetryModule
        private lateinit var launchProvider: LaunchDataProvider
        private lateinit var license: ModuleLicense
        private val systemRenderer = SystemMetricsRenderer()
        private var lastLaunchRefresh = 0L
        private var trajLandBmp: Bitmap? = null
        private var trajLandKey = Long.MIN_VALUE
        @Volatile private var trajLandBusy = false

        private fun trajLandKeyOf(cLon: Float, cLat: Float, hLon: Float, w: Int, h: Int): Long {
            val a = (cLon * 20f).toInt()
            val b = (cLat * 20f).toInt()
            val c = (hLon * 8f).toInt()
            return (a.toLong() shl 32) xor ((b.toLong() and 0xffff) shl 16) xor
                (c.toLong() and 0xffff) xor (w.toLong() shl 8) xor h.toLong()
        }

        private fun requestTrajLand(w: Int, h: Int, cLon: Float, cLat: Float, hLon: Float, hLat: Float) {
            GeoAtlas.ensure(this@RetroCommandWallpaperService)
            if (!GeoAtlas.ready) return
            val ww = w.coerceIn(8, 1200)
            val hh = h.coerceIn(8, 1200)
            val key = trajLandKeyOf(cLon, cLat, hLon, ww, hh)
            if (key == trajLandKey || trajLandBusy) return
            trajLandBusy = true
            Thread({
                val bmp = Bitmap.createBitmap(ww, hh, Bitmap.Config.RGB_565)
                val c = Canvas(bmp)
                val local = RectF(0f, 0f, ww.toFloat(), hh.toFloat())
                c.drawColor(Color.parseColor("#0A3A58"))
                GeoDraw.drawLand(
                    c, local, cLon, cLat, hLon, hLat,
                    Color.parseColor("#1C4A32"),
                    Color.parseColor("#8FD4A8"),
                    Color.parseColor("#0C4A6E")
                )
                handler.post {
                    val old = trajLandBmp
                    trajLandBmp = bmp
                    trajLandKey = key
                    trajLandBusy = false
                    old?.recycle()
                }
            }, "ccos-wp-traj").start()
        }



        private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RetroSkin.cyan
            style = Paint.Style.FILL
            textSize = 32f
            typeface = Typeface.MONOSPACE
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RetroSkin.labelMuted
            style = Paint.Style.FILL
            textSize = 20f
            typeface = Typeface.MONOSPACE
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RetroSkin.particle
            style = Paint.Style.FILL
        }
        private val gridPaint = Paint().apply {
            color = RetroSkin.grid
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        private val particles = Array(120) { NanoParticle() }
        private var lastSpawn = 0L
        private var width = 0
        private var height = 0
        private val buttonRects = Array(8) { RectF() }

        private var lastTele: SystemMetricsProvider.Snapshot? = null
        private var lastTeleTime = 0L
        private var lastTickTime = 0L
        private var lastSimTickMs = 0L
        private val eventMonitor = FlightEventMonitor()
        private val kinetic = KineticFx(this@RetroCommandWallpaperService)

        private var avgFrameMs = 16f

        private var rocketCx = 0f
        private var rocketBaseY = 0f
        private var rocketScale = 1f
        private val bodyPath = Path()
        private val fairingPath = Path()
        private val finPath = Path()
        private val legPath = Path()
        private val padPath = Path()

        private val activeSliders = mutableListOf<LiveSlider>()
        private var draggingSlider: LiveSlider? = null
        private var showPanel = false
        private val panelBounds = RectF()
        private var panelTouchLocked = false
        private val modeChipAnalog = RectF()
        private val modeChipDigital = RectF()
        private val themeChipRects = mutableListOf<Pair<Int, RectF>>()
        private val resetChipRect = RectF()

        // CMD double-tap (within 400ms) opens Settings / MainActivity
        private var lastCmdTapTime = 0L
        private val doubleTapMs = 400L

        // Launcher page tracking — buttons only on command page (left of home)
        private var onCommandPage = true
        private val pagesSeen = HashSet<Int>()
        private var currentLauncherPage = 0
        private var pageSettled = true
        private var trackZoomSmoothed = 0f
        private var telHudBottom = 0f
        private var insetNavBottom = 0
        private var insetTappableBottom = 0
        private var touchStartX = 0f
        private var touchStartY = 0f
        private var trackingSwipe = false

        // Larger touch targets for leftover rocket allocation sliders
        private val sliderThumbRadius = 28f
        private val sliderTrackHeight = 14f
        private val lampRockerRects = arrayOf(RectF(), RectF(), RectF())
        private val textRockerRects = arrayOf(RectF(), RectF(), RectF())
        private val holdRockerRects = arrayOf(RectF(), RectF(), RectF())
        private val extraRockerHits = mutableListOf<Pair<RectF, () -> Unit>>()

        /** Screen-relative size so panels stay readable on phones and tablets. */
        private fun su(frac: Float): Float = min(width, height) * frac
        /** Razr / phone / cover. The home screen IS the desk. */
        private fun isPhoneDesk(): Boolean {
            if (width < 8 || height < 8) return true
            val sw = min(width, height) / resources.displayMetrics.density
            return sw < 600f
        }
        /** Preferred telemetry size in px. Layout code must still fit-to-cell. */
        private fun telSp(sp: Float): Float {
            val d = this@RetroCommandWallpaperService.resources.displayMetrics.scaledDensity
            return sp * d
        }
        private fun telBold() {
            hudPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            hudPaint.isFakeBoldText = true
        }
        /** Largest bold size that fits the cell — never overflow. */
        private fun telFit(text: String, maxW: Float, maxH: Float, preferSp: Float = 18f): Float {
            telBold()
            val capW = maxW.coerceAtLeast(8f)
            var size = min(telSp(preferSp), maxH).coerceAtLeast(10f)
            hudPaint.textSize = size
            var guard = 0
            while (guard++ < 60 && size > 10f && hudPaint.measureText(text) * 1.06f > capW) {
                size -= 1.5f
                hudPaint.textSize = size
            }
            return size
        }

        /** Inner edges of the CMD / PAD columns. HUD and gauges stay inside this lane. */
        private fun laneLeft(): Float {
            val b = buttonRects[0]
            return if (b.width() > 4f) b.right + su(0.012f) else width * 0.195f
        }
        private fun laneRight(): Float {
            val b = buttonRects[4]
            return if (b.width() > 4f) b.left - su(0.012f) else width * 0.805f
        }
        private fun fullLeft(): Float = width * 0.012f
        private fun fullRight(): Float = width * 0.988f
        private fun belowButtons(): Float {
            val b = buttonRects[3]
            return if (b.bottom > 1f) b.bottom + su(0.010f) else height * 0.42f
        }
        private fun panelTitleSize(): Float = su(0.042f).coerceIn(28f, 48f)
        private fun panelLabelSize(): Float = su(0.032f).coerceIn(22f, 36f)
        private fun panelRockerH(): Float = (height * 0.078f).coerceIn(56f, 96f)
        private fun panelToggleH(): Float = (height * 0.070f).coerceIn(52f, 88f)

        /** Scaled text size from Settings slider */
        private fun ts(base: Float): Float = base * prefs.textScale
        /**
         * Desired button label size from HUD slider (tracks full range).
         * Callers must pass through fitBtnText() so it clamps at the plate edge.
         */
        private fun btnTs(base: Float): Float {
            val s = if (prefs.isTelemetry()) {
                // 2.8→9.0 maps to ~1.2× → ~2.8× base
                val t = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
                1.2f + t * 1.6f
            } else {
                prefs.textScale.coerceIn(0.9f, 2.4f)
            }
            return base * s
        }

        /** Shrink label until it fits inside the button plate. */
        private fun fitBtnText(label: String, maxW: Float, maxH: Float, desired: Float): Float {
            var size = desired.coerceAtMost(maxH)
            hudPaint.textSize = size
            var guard = 0
            while (guard++ < 40 && size > 11f && hudPaint.measureText(label) > maxW) {
                size -= 1.5f
                hudPaint.textSize = size
            }
            return size
        }

        /** Body copy on PAD/STS/VID/MSK — tracks HUD slider but hard-capped. */
        private fun pageBodyTs(base: Float): Float {
            val t = if (prefs.isTelemetry()) {
                ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            } else 0.5f
            // 1.0× → 1.85× max — never the "stupid large" clock scale
            return base * (1.0f + t * 0.85f)
        }

        /** Draw centered text, wrapping to max width. Returns next baseline Y. */
        private fun drawWrappedCenter(
            canvas: Canvas, text: String, cx: Float, startY: Float,
            maxW: Float, size: Float, color: Int, lineGap: Float = 1.25f
        ): Float {
            hudPaint.textSize = size
            hudPaint.color = color
            hudPaint.textAlign = Paint.Align.CENTER
            val words = text.split(' ')
            var line = StringBuilder()
            var y = startY
            fun flush() {
                if (line.isNotEmpty()) {
                    canvas.drawText(line.toString(), cx, y, hudPaint)
                    y += size * lineGap
                    line = StringBuilder()
                }
            }
            for (w in words) {
                val trial = if (line.isEmpty()) w else "$line $w"
                if (hudPaint.measureText(trial) > maxW && line.isNotEmpty()) flush()
                if (line.isEmpty()) line.append(w) else line.append(' ').append(w)
            }
            flush()
            return y
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!visible) return
                val start = System.nanoTime()
                // Refresh prefs each frame so Settings changes apply live
                drawFrame()
                avgFrameMs = avgFrameMs * 0.9f + ((System.nanoTime() - start) / 1_000_000f) * 0.1f
                handler.postDelayed(this, 55)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            state = BuildState(this@RetroCommandWallpaperService)
            prefs = AppPrefs(this@RetroCommandWallpaperService)
            metrics = SystemMetricsProvider(this@RetroCommandWallpaperService)
            launchProvider = LaunchDataProvider()
            rocketModule = VectorRocketModule(state)
            systemModule = SystemMetricsModule()
            telemetryModule = RocketTelemetryModule(prefs, launchProvider)
            license = ModuleLicense(this@RetroCommandWallpaperService)
            ModuleRegistry.register(rocketModule)
            ModuleRegistry.register(systemModule)
            ModuleRegistry.register(telemetryModule)
            ModuleRegistry.setActive(prefs.activeModuleId)
            lastTickTime = System.currentTimeMillis()
            // Kick off first launch schedule fetch
            telemetryModule.ensureData()
            prefs.restoreCommandPageHud()
            GeoAtlas.ensure(this@RetroCommandWallpaperService)
            PadBook.ensure(this@RetroCommandWallpaperService)
            setTouchEventsEnabled(true)
        }


        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, w: Int, h: Int) {
            width = w
            height = h
            layoutButtons()
            rebuildRocketGeometry()
            rebuildSliders()
        }

        override fun onApplyWindowInsets(insets: WindowInsets?) {
            if (insets != null) {
                insetNavBottom = insets.systemWindowInsetBottom
                if (Build.VERSION.SDK_INT >= 30) {
                    insetNavBottom = maxOf(
                        insetNavBottom,
                        insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                    )
                    insetTappableBottom = insets.getInsets(WindowInsets.Type.tappableElement()).bottom
                } else if (Build.VERSION.SDK_INT >= 29) {
                    insetTappableBottom = insets.systemGestureInsets.bottom
                }
            }
            super.onApplyWindowInsets(insets)
        }

        /** Full launcher band: hotseat (Phone/Messages/drawer) + nav. Never nav-only. */
        private fun dockFloor(): Float {
            val icon = resources.getDimension(android.R.dimen.app_icon_size)
            val navRes = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            val navFallback = if (navRes != 0) resources.getDimension(navRes) else icon
            val nav = if (insetNavBottom > 0) insetNavBottom.toFloat() else navFallback
            val tap = insetTappableBottom.toFloat()
            val hotseatInInsets = tap > nav + icon * 0.5f
            val row = icon * 2f
            val gap = if (hotseatInInsets) tap else nav + row
            return height - gap
        }

        /**
         * HUD lives on the command page. Fresh Nova has one screen — that IS page 0.
         * We never wait for a page that does not exist. Mid-swipe hides the buttons
         * so they do not bleed onto the next screen.
         */
        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            if (xOffsetStep <= 0.001f) {
                // Cannot see pages. Do not paint plates always-on. Treat as page 0.
                currentLauncherPage = 0
                pageSettled = true
                pagesSeen.add(0)
                onCommandPage = prefs.commandPageIndex == 0
            } else {
                val pageFloat = xOffset / xOffsetStep
                val page = Math.round(pageFloat)
                pageSettled = abs(pageFloat - page) < 0.2f
                currentLauncherPage = page.coerceIn(0, 11)
                val pages = ((1f / xOffsetStep).roundToInt() + 1).coerceIn(1, 12)
                if (pages != prefs.launcherPageCount) prefs.launcherPageCount = pages
                if (pageSettled) pagesSeen.add(page)
                val cmd = prefs.commandPageIndex
                onCommandPage = pageSettled && page == cmd
            }
            if (!onCommandPage) showPanel = false
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastTickTime = System.currentTimeMillis()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawRunnable)
            kinetic.release()
            super.onDestroy()
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (event == null) return super.onTouchEvent(event)
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = x
                    touchStartY = y
                    trackingSwipe = true
                    panelTouchLocked = false

                    // Entire open CMD panel captures touches so Nova cannot page-flip
                    if (showPanel && panelBounds.contains(x, y)) {
                        panelTouchLocked = true
                        trackingSwipe = false
                        // Hard MODE switch chips
                        if (modeChipAnalog.contains(x, y)) {
                            prefs.systemAnalog = true
                            state.registerInteraction()
                            rebuildSliders()
                            return
                        }
                        if (modeChipDigital.contains(x, y)) {
                            prefs.systemAnalog = false
                            state.registerInteraction()
                            rebuildSliders()
                            return
                        }
                        // Theme color chips
                        for ((tid, r) in themeChipRects) {
                            if (r.contains(x, y)) {
                                prefs.systemTheme = tid
                                state.registerInteraction()
                                rebuildSliders()
                                return
                            }
                        }
                        if (resetChipRect.contains(x, y) && !prefs.isSystem()) {
                            state.resetProgress()
                            // clear nanos
                            for (p in particles) p.active = false
                            state.registerInteraction()
                            rebuildSliders()
                            return
                        }
                        val lampHit = hitRocker(lampRockerRects, x, y)
                        if (lampHit >= 0) {
                            prefs.setLampStep(lampHit)
                            state.registerInteraction()
                            return
                        }
                        val textHit = hitRocker(textRockerRects, x, y)
                        if (textHit >= 0) {
                            prefs.setTextStep(textHit)
                            state.registerInteraction()
                            return
                        }
                        for ((r, action) in extraRockerHits) {
                            if (r.contains(x, y)) {
                                action()
                                state.registerInteraction()
                                return
                            }
                        }
                        // Telemetry panel toggles
                        if (prefs.isTelemetry()) {
                            if (panelToggleAnalog.contains(x, y)) {
                                prefs.telemetryAnalog = !prefs.telemetryAnalog
                                state.registerInteraction()
                                return
                            }
                            if (panelToggleUnits.contains(x, y)) {
                                prefs.useImperial = !prefs.useImperial
                                state.registerInteraction()
                                return
                            }
                            if (panelToggleExtra.contains(x, y)) {
                                prefs.extraScreens = !prefs.extraScreens
                                applyExtraScreens()
                                state.registerInteraction()
                                return
                            }
                            if (panelToggleConsole.contains(x, y)) {
                                openCommandCenter()
                                showPanel = false
                                return
                            }
                            for ((r, action) in extraChipHits) {
                                if (r.contains(x, y)) {
                                    action()
                                    applyExtraScreens()
                                    state.registerInteraction()
                                    return
                                }
                            }
                        }
                        for (s in activeSliders) {
                            val pad = height * 0.03f
                            val hit = RectF(s.rect.left - 16f, s.rect.top - pad, s.rect.right + 16f, s.rect.bottom + pad)
                            if (hit.contains(x, y)) {
                                draggingSlider = s
                                updateSliderFromTouch(s, x)
                                state.registerInteraction()
                            }
                        }
                        return  // always consume panel touches
                    }

                    // Buttons on command page
                    if (onCommandPage && prefs.showButtons) {
                        for (i in buttonRects.indices) {
                            if (buttonRects[i].contains(x, y)) {
                                trackingSwipe = false
                                handleButtonTap(i)
                                return
                            }
                        }
                    }
                    if (onCommandPage && prefs.isTelemetry() && telemetryModule.activePage == 4 &&
                        !geoHit.isEmpty && geoHit.contains(x, y)
                    ) {
                        trackingSwipe = false
                        openPadEarth()
                        state.registerInteraction()
                        return
                    }
                    // CDT jump chips (time scrub for demos / historical)
                    if (onCommandPage && prefs.isTelemetry() && telemetryModule.activePage == 1) {
                        for (i in jumpChipRects.indices) {
                            if (jumpChipRects[i].contains(x, y)) {
                                trackingSwipe = false
                                if (jumpChipValues[i] <= -900f) telemetryModule.markScrubbed()
                                else telemetryModule.jumpTo(jumpChipValues[i])
                                state.registerInteraction()
                                return
                            }
                        }
                    }
                    // TEL page bottom control strip
                    if (onCommandPage && prefs.isTelemetry() &&
                        (telemetryModule.activePage == 0 || telemetryModule.activePage == 2)
                    ) {
                        if (telStripAnalog.contains(x, y)) {
                            prefs.telemetryAnalog = y >= telStripAnalog.centerY()
                            state.registerInteraction()
                            return
                        }
                        if (lockHit.contains(x, y)) {
                            telemetryModule.togglePin()
                            state.registerInteraction()
                            return
                        }
                        if (eventSkipHit.contains(x, y)) {
                            telemetryModule.skipEvent(if (y >= eventSkipHit.centerY()) -1 else 1)
                            state.registerInteraction()
                            return
                        }
                        if (stage1Hit.contains(x, y)) {
                            prefs.trackedStage = 1
                            state.registerInteraction()
                            return
                        }
                        if (stage2Hit.contains(x, y)) {
                            prefs.trackedStage = 2
                            state.registerInteraction()
                            return
                        }
                    }

                    // Tap empty space while panel open → dismiss
                    if (showPanel) {
                        showPanel = false
                        return
                    }

                }
                MotionEvent.ACTION_MOVE -> {
                    if (panelTouchLocked || draggingSlider != null) {
                        draggingSlider?.let {
                            updateSliderFromTouch(it, x)
                            state.registerInteraction()
                        }
                        return  // consume — prevent launcher page flip
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val locked = panelTouchLocked || draggingSlider != null
                    draggingSlider = null
                    panelTouchLocked = false
                    trackingSwipe = false
                    if (locked) return
                }
            }
            super.onTouchEvent(event)
        }

        private fun handleButtonTap(i: Int) {
            val onSystem = prefs.isSystem()
            val onTel = prefs.isTelemetry()
            if (i == 0) {
                // CMD: double-tap → Settings; single → panel
                val now = System.currentTimeMillis()
                if (now - lastCmdTapTime <= doubleTapMs) {
                    lastCmdTapTime = 0L
                    showPanel = false
                    openSettings()
                    return
                }
                lastCmdTapTime = now
            }

            when {
                onSystem -> {
                    systemModule.onModuleButton(i)
                    when (i) {
                        0 -> {
                            if (showPanel && state.activeModule == 0) {
                                showPanel = false
                            } else {
                                state.activeModule = 0
                                rebuildSliders()
                                showPanel = true
                            }
                        }
                        4 -> {
                            showPanel = false
                            openStorageSettings()
                        }
                        7 -> {
                            showPanel = false
                            openHomeWallpaperSettings()
                        }
                        else -> {
                            if (showPanel && state.activeModule == i) {
                                showPanel = false
                            } else {
                                state.activeModule = i
                                activeSliders.clear()
                                showPanel = true
                            }
                        }
                    }
                }
                onTel -> {
                    val wasTelPage = telemetryModule.activePage == 2
                    telemetryModule.onModuleButton(i)
                    when (i) {
                        0 -> { // CMD
                            showPanel = !showPanel
                            if (showPanel) rebuildTelemetrySliders()
                        }
                        2 -> { // TEL — first tap is gauges only. Second tap opens options.
                            if (wasTelPage) {
                                showPanel = !showPanel
                                if (showPanel) rebuildTelemetrySliders()
                            } else {
                                showPanel = false
                            }
                        }
                        5 -> { // VID
                            showPanel = false
                            openWebcast()
                        }
                        else -> showPanel = false
                    }
                }


                else -> {
                    // Rocket: re-tap active button closes panel; other button switches panel
                    if (showPanel && state.activeModule == i) {
                        showPanel = false
                    } else {
                        rocketModule.onModuleButton(i)
                        rebuildSliders()
                        showPanel = true
                    }
                }
            }
            state.registerInteraction()
        }


        private fun openSettings() {
            try {
                val intent = Intent(this@RetroCommandWallpaperService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) { }
        }

        /** Open official webcast / mission search for the *currently tracked* launch only. */
        private fun openPadEarth() {
            telemetryModule.resolveTracked()
            val launch = telemetryModule.tracked ?: return
            val ll = PadBook.lonLat(launch) ?: return
            val (lon, lat) = ll
            try {
                val intent = Intent(Intent.ACTION_VIEW, PadGlyph.earthUri(lat, lon)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) { }
        }

        private fun openWebcast() {
            telemetryModule.resolveTracked()
            val launch = telemetryModule.tracked
            // Prefer fresh cache entry (webcast often appears only close to NET)
            val fresh = launch?.id?.let { telemetryModule.selectableLaunches().firstOrNull { s -> s.id == it } } ?: launch
            val url = WebcastResolver.panes(fresh).official.url
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) { }
        }


        // Hit targets for analog / units toggles in CMD panel
        private val panelToggleAnalog = RectF()
        private val panelToggleUnits = RectF()
        private val panelToggleExtra = RectF()
        private val panelToggleConsole = RectF()
        private val extraChipHits = mutableListOf<Pair<RectF, () -> Unit>>()
        private val stage1Hit = RectF()
        private val stage2Hit = RectF()
        // TEL page bottom control strip
        private val telStripAnalog = RectF()
        private val eventSkipHit = RectF()
        private val lockHit = RectF()
        private val geoHit = RectF()
        private val telStripUnits = RectF()
        private val telStripText = RectF()
        private val telStripColorRects = mutableListOf<Pair<Int, RectF>>()

        private fun rebuildTelemetrySliders() {
            activeSliders.clear()
            // Telemetry textScale range 2.8 → 9.0
            val textVal = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            activeSliders.add(
                LiveSlider(
                    SliderDef("HUD TEXT SIZE", textVal) { v ->
                        prefs.textScale = 2.8f + v * 6.2f
                    },
                    RectF(),
                    textVal
                )
            )


            val lampVal = prefs.lampBrightness.coerceIn(0.2f, 1f)
            activeSliders.add(
                LiveSlider(
                    SliderDef("LAMP BRIGHTNESS", lampVal) { v ->
                        prefs.lampBrightness = v.coerceIn(0.2f, 1f)
                    },
                    RectF(),
                    lampVal
                )
            )
        }



        private fun openStorageSettings() {
            try {
                val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MEMORY_CARD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) { }
            }
        }

        private fun openHomeWallpaperSettings() {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) { }
            }
        }

        private fun updateSliderFromTouch(s: LiveSlider, x: Float) {
            val t = ((x - s.rect.left) / s.rect.width()).coerceIn(0f, 1f)
            s.value = t
            s.def.onChange(t)
        }

        private fun nearestAllocStep(value: Float): Int {
            val steps = floatArrayOf(0.20f, 0.55f, 1.00f)
            var best = 1
            var bestD = Float.MAX_VALUE
            for (i in steps.indices) {
                val d = abs(steps[i] - value.coerceIn(0f, 1f))
                if (d < bestD) {
                    bestD = d
                    best = i
                }
            }
            return best
        }

        private fun hitRocker(rects: Array<RectF>, x: Float, y: Float): Int {
            for (i in rects.indices) if (rects[i].contains(x, y)) return i
            return -1
        }

        private fun layoutRockerRow(rects: Array<RectF>, left: Float, top: Float, right: Float, h: Float) {
            val gap = 10f
            val n = rects.size
            val w = (right - left - gap * (n - 1)) / n
            for (i in 0 until n) {
                val x0 = left + i * (w + gap)
                rects[i].set(x0, top, x0 + w, top + h)
            }
        }

        private fun drawRockerRow(
            canvas: Canvas,
            rects: Array<RectF>,
            labels: Array<String>,
            selected: Int,
            accent: Int,
            textColor: Int,
            muted: Int
        ) {
            for (i in rects.indices) {
                val r = rects[i]
                val on = i == selected
                fillPaint.color = if (on) Color.argb(230, 18, 28, 38) else Color.argb(210, 8, 12, 16)
                canvas.drawRoundRect(r, 8f, 8f, fillPaint)
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = if (on) 4.5f else 3.2f
                strokePaint.color = if (on) accent else muted
                canvas.drawRoundRect(r, 8f, 8f, strokePaint)
                val lampX = r.left + 16f
                if (on) {
                    fillPaint.color = accent
                    canvas.drawCircle(lampX, r.centerY(), 7f, fillPaint)
                } else {
                    strokePaint.strokeWidth = 3f
                    canvas.drawCircle(lampX, r.centerY(), 7f, strokePaint)
                }
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.textSize = r.height() * 0.36f
                hudPaint.color = if (on) textColor else muted
                canvas.drawText(labels[i], r.centerX() + 8f, r.centerY() + r.height() * 0.14f, hudPaint)
            }
        }

        private fun layoutButtons() {
            val btnW = width * 0.175f
            val btnH = height * 0.078f
            val gap = height * 0.014f
            val topY = height * 0.055f
            val leftX = 8f
            val rightX = width - btnW - 8f
            for (i in 0 until 4) {
                val y = topY + i * (btnH + gap)
                buttonRects[i].set(leftX, y, leftX + btnW, y + btnH)
                buttonRects[i + 4].set(rightX, y, rightX + btnW, y + btnH)
            }
        }

        /** −/+ by STG1, DIG/ANLG by STG2. Vertical pair just inside each stack. */
        private fun layoutActionChipsBesideStacks() {
            if (stage1Hit.height() < 16f || stage2Hit.height() < 16f) {
                eventSkipHit.setEmpty()
                lockHit.setEmpty()
                telStripAnalog.setEmpty()
                return
            }
            val cellW = min(stage1Hit.width() * 0.42f, telSp(32f)).coerceAtLeast(36f)
            val cellH = min(stage1Hit.height() * 0.18f, telSp(22f)).coerceAtLeast(32f)
            val pairH = cellH * 2f + 3f
            val y1 = stage1Hit.centerY() - pairH * 0.5f
            eventSkipHit.set(stage1Hit.right + 3f, y1, stage1Hit.right + 3f + cellW, y1 + pairH)
            lockHit.set(eventSkipHit.left, eventSkipHit.bottom + 3f, eventSkipHit.right, eventSkipHit.bottom + 3f + cellH)
            val y2 = stage2Hit.centerY() - pairH * 0.5f
            telStripAnalog.set(stage2Hit.left - 3f - cellW, y2, stage2Hit.left - 3f, y2 + pairH)
        }

        private fun rebuildSliders() {
            activeSliders.clear()
            val panelLeft = width * 0.18f
            val panelRight = width * 0.82f
            val panelTop = height * 0.14f
            val rowH = height * 0.075f
            val gap = height * 0.018f

            val defs = mutableListOf<SliderDef>()
            // Lamp and text are console rockers, not sliders.
            if (!prefs.isSystem() && state.activeModule != 0) {
                defs.addAll(rocketModule.slidersFor(state.activeModule))
            }
            defs.forEachIndexed { row, def ->
                val y = panelTop + row * (rowH + gap)
                val r = RectF(panelLeft, y, panelRight, y + rowH * 0.55f)
                activeSliders.add(LiveSlider(def, r, def.value))
            }
        }

        private fun rebuildRocketGeometry() {
            rocketCx = width / 2f
            rocketBaseY = height * 0.74f
            rocketScale = min(width, height) * 0.00215f
        }

        /** Dense technical / blueprint background shared by modules */
        private fun drawTechnicalBackground(canvas: Canvas, accent: Int) {
            canvas.drawColor(RetroSkin.bg)
            // Fine grid
            strokePaint.color = RetroSkin.grid
            strokePaint.strokeWidth = 1f
            val step = 36f
            var x = 0f
            while (x < width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), strokePaint)
                x += step
            }
            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, strokePaint)
                y += step
            }
            // Corner brackets
            strokePaint.color = accent
            strokePaint.strokeWidth = 2f
            val b = 28f
            val m = 18f
            // TL
            canvas.drawLine(m, m, m + b, m, strokePaint)
            canvas.drawLine(m, m, m, m + b, strokePaint)
            // TR
            canvas.drawLine(width - m, m, width - m - b, m, strokePaint)
            canvas.drawLine(width - m, m, width - m, m + b, strokePaint)
            // BL
            canvas.drawLine(m, height - m, m + b, height - m, strokePaint)
            canvas.drawLine(m, height - m, m, height - m - b, strokePaint)
            // BR
            canvas.drawLine(width - m, height - m, width - m - b, height - m, strokePaint)
            canvas.drawLine(width - m, height - m, width - m, height - m - b, strokePaint)
            // Top title bar tick marks
            strokePaint.strokeWidth = 1.2f
            strokePaint.color = Color.argb(80, Color.red(accent), Color.green(accent), Color.blue(accent))
            for (i in 0..20) {
                val tx = width * 0.15f + i * (width * 0.7f / 20f)
                val th = if (i % 5 == 0) 12f else 6f
                canvas.drawLine(tx, 8f, tx, 8f + th, strokePaint)
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return

                val now = System.currentTimeMillis()
                if (now - lastTickTime > 2400) {
                    state.tick((now - lastTickTime) / 1000f)
                    lastTickTime = now
                }
                if (now - lastTeleTime > 2500) {
                    lastTele = metrics.sample(avgFrameMs)
                    lastTeleTime = now
                }

                // Exclusive modules — never both on screen. Paid modules require a license.
                val telUnlocked = license.isUnlocked(ModuleCatalog.telemetry)
                when {
                    prefs.isSystem() -> {
                        val page = when (systemModule.activePage) {
                            1 -> "BAT focus"
                            2 -> "CPU focus"
                            3 -> "RAM focus"
                            4 -> "DIS → Storage settings"
                            5 -> "NET"
                            6 -> "SEN"
                            7 -> "HOME → Launcher settings"
                            else -> "CMD double-tap = Settings · text size on CMD"
                        }
                        systemRenderer.draw(
                            canvas, width, height, lastTele,
                            prefs.systemTheme, prefs.systemAnalog, prefs.lampBrightness, page,
                            prefs.textScale,
                            DateFormat.is24HourFormat(this@RetroCommandWallpaperService)
                        )
                        if (showPanel && onCommandPage) drawInteractivePanel(canvas)
                    }
                    prefs.isTelemetry() && telUnlocked -> {
                        val telCanvas = canvas!!
                        val clipMark = telCanvas.save()
                        try {
                            val interval = if (telemetryModule.autoMode)
                                telemetryModule.autoRefreshIntervalMs(now)
                            else
                                5 * 60 * 1000L
                            if (now - lastLaunchRefresh > interval) {
                                lastLaunchRefresh = now
                                val near = telemetryModule.tracked?.let {
                                    val s = it.secondsToNet(now)
                                    s in -300L..(2 * 3600L) && !it.id.startsWith("demo-")
                                } == true
                                if (near || telemetryModule.autoMode) {
                                    telemetryModule.forceRefresh()
                                } else {
                                    telemetryModule.ensureData()
                                }
                            }
                            telemetryModule.resolveTracked(now)
                            if (telemetryModule.simSecondsFromNet != null) {
                                val dt = (now - lastSimTickMs).coerceIn(0L, 50L) / 1000f
                                lastSimTickMs = now
                                telemetryModule.tickSim(dt)
                            } else {
                                lastSimTickMs = now
                            }
                            val tNow = telemetryModule.effectiveSecondsFromNet(now)
                            val live = telemetryModule.tracked
                            val alert = kinetic.shouldAlertOnWallpaper(live, tNow, telemetryModule.simSecondsFromNet)
                            for (e in eventMonitor.poll(live, tNow)) {
                                if (alert) kinetic.play(e)
                            }
                            drawTelemetrySurface(telCanvas, now)
                            if (showPanel && onCommandPage) drawTelemetryPanel(telCanvas)
                        } catch (_: Exception) {
                            // never let a gauge/map crash eat the side buttons
                        } finally {
                            try { telCanvas.restoreToCount(clipMark) } catch (_: Exception) { }
                        }
                    }

                    else -> {
                        drawTechnicalBackground(canvas, RetroSkin.steel)
                        drawMercuryRocket(canvas)
                        updateParticles(canvas)
                        drawNearRocketInfo(canvas)
                        if (showPanel && onCommandPage) drawInteractivePanel(canvas)
                    }
                }

                if (onCommandPage && prefs.showButtons) drawButtons(canvas)
                if (!onCommandPage && pageSettled) {
                    drawOffPageDataWall(canvas, now)
                }

            } finally {
                if (canvas != null) try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                }
            }
        }

        /**
         * Mercury-Atlas launch vehicle — vector reconstruction from Project Mercury blueprints.
         * Escape tower, capsule, adapter, UNITED STATES stages, engine skirt, fins.
         */
        private fun drawMercuryRocket(canvas: Canvas) {
            val cx = rocketCx
            val by = rocketBaseY
            val s = rocketScale

            val engH = 58f * s
            val stage1H = 195f * s
            val stage2H = 90f * s
            val adapterH = 26f * s
            val capsuleH = 48f * s
            val towerH = 68f * s
            val bodyW = 30f * s
            val engW = 40f * s

            val engTop = by - engH
            val s1Bottom = engTop
            val s1Top = s1Bottom - stage1H
            val s2Bottom = s1Top
            val s2Top = s2Bottom - stage2H
            val adBottom = s2Top
            val adTop = adBottom - adapterH
            val capTop = adTop - capsuleH
            val towerBase = capTop

            // --- LAUNCH PAD + SERVICE TOWER (blueprint assembly) ---
            val pp = state.padProgress.coerceIn(0f, 1f)
            val padW = 78f * s
            // Flame trench (always ghost)
            strokePaint.strokeWidth = 1.4f
            strokePaint.color = Color.argb(70, 154, 163, 173)
            canvas.drawRect(cx - padW * 0.35f, by + 18f, cx + padW * 0.35f, by + 34f, strokePaint)
            if (pp > 0.08f) {
                strokePaint.color = RetroSkin.dim
                strokePaint.strokeWidth = 2f
                canvas.drawRect(cx - padW * 0.35f, by + 18f, cx + padW * 0.35f, by + 34f, strokePaint)
                // trench hatch lines
                strokePaint.strokeWidth = 1f
                var hx = cx - padW * 0.3f
                while (hx < cx + padW * 0.3f) {
                    canvas.drawLine(hx, by + 18f, hx + 6f * s, by + 34f, strokePaint)
                    hx += 8f * s
                }
            }
            // Blast deck plates
            strokePaint.strokeWidth = 2.2f
            strokePaint.color = if (pp > 0.15f) RetroSkin.steel else Color.argb(50, 154, 163, 173)
            canvas.drawLine(cx - padW, by + 8f, cx + padW, by + 8f, strokePaint)
            if (pp > 0.2f) {
                strokePaint.strokeWidth = 1.5f
                canvas.drawLine(cx - padW * 0.9f, by + 12f, cx + padW * 0.9f, by + 12f, strokePaint)
                // plate seams
                for (i in 1..4) {
                    val px = cx - padW + i * (padW * 2f / 5f)
                    canvas.drawLine(px, by + 8f, px, by + 16f, strokePaint)
                }
            }
            // Hold-down arms
            if (pp > 0.3f) {
                strokePaint.color = RetroSkin.orange
                strokePaint.strokeWidth = 2.4f
                for (side in listOf(-1f, 1f)) {
                    canvas.drawLine(cx + side * engW * 0.9f, by - 4f * s, cx + side * (padW * 0.55f), by + 8f, strokePaint)
                    canvas.drawLine(cx + side * (padW * 0.55f), by + 8f, cx + side * (padW * 0.55f), by + 16f, strokePaint)
                }
            }
            // Service structure — FULL rocket height (pad to capsule), not just engines
            if (pp > 0.2f) {
                val twX = cx + padW * 0.78f
                val fullH = engH + stage1H + stage2H + adapterH + capsuleH * 0.5f
                val twH = fullH * pp.coerceAtLeast(0.25f)
                val twTop = by + 8f - twH
                val colW = 16f * s
                // Twin columns
                strokePaint.color = RetroSkin.steel
                strokePaint.strokeWidth = 2.8f
                canvas.drawLine(twX, by + 8f, twX, twTop, strokePaint)
                canvas.drawLine(twX + colW, by + 8f, twX + colW, twTop, strokePaint)
                // Top platform
                strokePaint.strokeWidth = 2f
                canvas.drawLine(twX - 4f * s, twTop, twX + colW + 4f * s, twTop, strokePaint)
                // Lattice
                strokePaint.strokeWidth = 1.2f
                strokePaint.color = RetroSkin.cyan
                var ly = by + 8f - 10f * s
                while (ly > twTop + 8f * s) {
                    canvas.drawLine(twX, ly, twX + colW, ly - 14f * s, strokePaint)
                    canvas.drawLine(twX + colW, ly, twX, ly - 14f * s, strokePaint)
                    canvas.drawLine(twX, ly - 14f * s, twX + colW, ly - 14f * s, strokePaint)
                    ly -= 20f * s
                }
                // Umbilical / cabling arms at multiple heights
                strokePaint.color = RetroSkin.orange
                strokePaint.strokeWidth = 1.8f
                val cableYs = listOf(
                    by - engH * 0.4f,
                    by - engH - stage1H * 0.25f,
                    by - engH - stage1H * 0.7f,
                    by - engH - stage1H - stage2H * 0.5f,
                    by - engH - stage1H - stage2H - adapterH * 0.5f
                )
                for ((idx, cyArm) in cableYs.withIndex()) {
                    if (pp < 0.3f + idx * 0.12f) continue
                    if (cyArm < twTop) continue
                    canvas.drawLine(twX, cyArm, cx + bodyW + 2f * s, cyArm, strokePaint)
                    // small connector stub on vehicle
                    canvas.drawLine(cx + bodyW + 2f * s, cyArm - 3f * s, cx + bodyW + 2f * s, cyArm + 3f * s, strokePaint)
                }
            }
            // Left support pylon
            if (pp > 0.35f) {
                strokePaint.color = RetroSkin.steel
                strokePaint.strokeWidth = 2f
                val lx = cx - padW * 0.75f
                canvas.drawLine(lx, by + 8f, lx, by - 30f * s * pp, strokePaint)
                canvas.drawLine(lx, by - 30f * s * pp, lx + 10f * s, by - 30f * s * pp, strokePaint)
            }

            // --- Ghost full stack outline (always, so structure is readable) ---
            // --- Ghost full stack outline (always, so structure is readable) ---
            strokePaint.color = Color.argb(55, 154, 163, 173)
            strokePaint.strokeWidth = 1.4f
            canvas.drawRect(cx - bodyW, s1Top, cx + bodyW, s1Bottom, strokePaint)
            canvas.drawRect(cx - bodyW * 0.92f, s2Top, cx + bodyW * 0.92f, s2Bottom, strokePaint)
            // Ghost capsule
            val gCap = Path()
            gCap.moveTo(cx - bodyW * 0.55f, adTop)
            gCap.lineTo(cx - bodyW * 0.55f, adBottom)
            gCap.lineTo(cx + bodyW * 0.55f, adBottom)
            gCap.lineTo(cx + bodyW * 0.55f, adTop)
            gCap.quadTo(cx, capTop - 4f * s, cx - bodyW * 0.55f, adTop)
            canvas.drawPath(gCap, strokePaint)
            // Ghost escape tower
            canvas.drawLine(cx - 10f * s, towerBase, cx, towerBase - towerH, strokePaint)
            canvas.drawLine(cx + 10f * s, towerBase, cx, towerBase - towerH, strokePaint)
            canvas.drawLine(cx, towerBase, cx, towerBase - towerH, strokePaint)
            canvas.drawCircle(cx, towerBase - towerH, 5f * s, strokePaint)

            // --- ENGINES (always drawn; light up with progress) ---
            strokePaint.strokeWidth = 2.4f
            strokePaint.color = if (state.engineProgress > 0.15f) RetroSkin.steel else RetroSkin.dim
            val skirt = Path()
            skirt.moveTo(cx - bodyW * 0.9f, engTop)
            skirt.lineTo(cx - engW, by - 2f * s)
            skirt.lineTo(cx + engW, by - 2f * s)
            skirt.lineTo(cx + bodyW * 0.9f, engTop)
            skirt.close()
            canvas.drawPath(skirt, strokePaint)
            // 3 Atlas-style nozzles — always visible
            val lit = (state.engineProgress * 3).toInt().coerceIn(0, 3)
            for ((idx, nx) in listOf(-16f * s, 0f, 16f * s).withIndex()) {
                val on = idx < lit
                strokePaint.color = if (on) RetroSkin.orange else RetroSkin.dim
                strokePaint.strokeWidth = if (on) 2.6f else 1.8f
                canvas.drawOval(cx + nx - 9f * s, by - 14f * s, cx + nx + 9f * s, by + 2f * s, strokePaint)
                if (on) {
                    fillPaint.color = Color.argb(160, 232, 120, 20)
                    canvas.drawOval(cx + nx - 5f * s, by - 10f * s, cx + nx + 5f * s, by - 1f * s, fillPaint)
                }
            }
            // Fins always
            strokePaint.color = RetroSkin.steel
            strokePaint.strokeWidth = 2.2f
            for (side in listOf(-1f, 1f)) {
                val fin = Path()
                fin.moveTo(cx + side * engW * 0.85f, by - engH * 0.4f)
                fin.lineTo(cx + side * (engW + 20f * s), by - 4f * s)
                fin.lineTo(cx + side * engW * 0.8f, by - 4f * s)
                fin.close()
                canvas.drawPath(fin, strokePaint)
            }

            // --- STAGE 1 body (builds upward with progress) ---
            val bodyProg = state.rocketBodyProgress.coerceIn(0f, 1f)
            if (bodyProg > 0.02f) {
                val builtTop = s1Bottom - stage1H * bodyProg
                strokePaint.color = RetroSkin.steel
                strokePaint.strokeWidth = 2.6f
                canvas.drawRect(cx - bodyW, builtTop, cx + bodyW, s1Bottom, strokePaint)
                // Station rings
                val rings = (8 * bodyProg).toInt().coerceAtLeast(1)
                for (i in 1..rings) {
                    val ry = s1Bottom - stage1H * bodyProg * i / 8f
                    strokePaint.strokeWidth = 1.3f
                    strokePaint.color = RetroSkin.dim
                    canvas.drawLine(cx - bodyW, ry, cx + bodyW, ry, strokePaint)
                }
                if (bodyProg > 0.3f) {
                    hudPaint.color = Color.argb(200, 216, 224, 232)
                    hudPaint.textSize = ts(12f)
                    hudPaint.textAlign = Paint.Align.LEFT
                    val us = "USA"
                    for ((idx, ch) in us.withIndex()) {
                        canvas.drawText(ch.toString(), cx + bodyW + 10f * s, builtTop + 30f * s + idx * 18f * s, hudPaint)
                    }
                }
            }

            // --- STAGE 2 ---
            if (bodyProg > 0.45f) {
                val p2 = ((bodyProg - 0.45f) / 0.55f).coerceIn(0f, 1f)
                val built2Top = s2Bottom - stage2H * p2
                strokePaint.color = RetroSkin.steel
                strokePaint.strokeWidth = 2.4f
                canvas.drawRect(cx - bodyW * 0.92f, built2Top, cx + bodyW * 0.92f, s2Bottom, strokePaint)
            }

            // --- Adapter + Capsule ---
            val sub = state.subsystemsProgress.coerceIn(0f, 1f)
            if (sub > 0.1f || bodyProg > 0.6f) {
                strokePaint.color = RetroSkin.steel
                strokePaint.strokeWidth = 2.2f
                val ad = Path()
                ad.moveTo(cx - bodyW * 0.92f, adBottom)
                ad.lineTo(cx - bodyW * 0.55f, adTop)
                ad.lineTo(cx + bodyW * 0.55f, adTop)
                ad.lineTo(cx + bodyW * 0.92f, adBottom)
                ad.close()
                canvas.drawPath(ad, strokePaint)

                val cap = Path()
                cap.moveTo(cx - bodyW * 0.55f, adTop)
                cap.quadTo(cx - bodyW * 0.7f, adTop - capsuleH * 0.35f, cx - bodyW * 0.22f, capTop + 6f * s)
                cap.lineTo(cx + bodyW * 0.22f, capTop + 6f * s)
                cap.quadTo(cx + bodyW * 0.7f, adTop - capsuleH * 0.35f, cx + bodyW * 0.55f, adTop)
                cap.close()
                strokePaint.color = if (sub > 0.35f) RetroSkin.cyan else RetroSkin.steel
                canvas.drawPath(cap, strokePaint)
                // Window
                strokePaint.strokeWidth = 1.8f
                canvas.drawCircle(cx, adTop - capsuleH * 0.4f, 5.5f * s, strokePaint)
                if (sub > 0.4f) {
                    fillPaint.color = Color.argb(80, 0, 200, 255)
                    canvas.drawCircle(cx, adTop - capsuleH * 0.4f, 3.5f * s, fillPaint)
                }
            }

            // --- Escape tower (builds with subsystems) ---
            if (sub > 0.2f) {
                val tp = ((sub - 0.2f) / 0.8f).coerceIn(0.15f, 1f)
                strokePaint.color = RetroSkin.orange
                strokePaint.strokeWidth = 2.2f
                val th = towerH * tp
                canvas.drawLine(cx - 11f * s, towerBase + 4f * s, cx, towerBase - th, strokePaint)
                canvas.drawLine(cx + 11f * s, towerBase + 4f * s, cx, towerBase - th, strokePaint)
                canvas.drawLine(cx, towerBase + 2f * s, cx, towerBase - th, strokePaint)
                canvas.drawCircle(cx, towerBase - th, 6f * s, strokePaint)
                if (tp > 0.6f) {
                    // Tower motors
                    canvas.drawLine(cx - 10f * s, towerBase - th + 3f * s, cx + 10f * s, towerBase - th + 3f * s, strokePaint)
                    strokePaint.color = RetroSkin.red
                    canvas.drawCircle(cx - 8f * s, towerBase - th, 3f * s, strokePaint)
                    canvas.drawCircle(cx + 8f * s, towerBase - th, 3f * s, strokePaint)
                }
            }

            // Title UNDER the launch pad / flame trench
            hudPaint.color = RetroSkin.cyan
            hudPaint.textSize = ts(22f)
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "MERCURY-ATLAS  ${(state.overallProgress() * 100).toInt()}%",
                cx, by + 48f, hudPaint
            )
            hudPaint.textAlign = Paint.Align.LEFT
        }

        private fun updateParticles(canvas: Canvas) {
            val now = System.currentTimeMillis()
            val dt = 0.048f
            val cx = rocketCx
            val by = rocketBaseY
            val s = rocketScale
            val engH = 58f * s
            val stage1H = 195f * s
            val stage2H = 90f * s

            // Flow scales with engagement; crawl when vehicle is complete
            val eng = state.engagement.coerceIn(0.08f, 1.6f)
            val sliderDrive = (
                state.engineRndAlloc + state.bodyRndAlloc +
                state.padRndAlloc + state.subRndAlloc
            ).coerceIn(0.2f, 4f) / 4f
            val complete = state.overallProgress() >= 0.98f
            val baseAct = (0.45f + eng * 0.55f + sliderDrive * 0.45f).coerceIn(0.45f, 1.5f)
            val activity = if (complete) 0.18f else baseAct  // maintenance trickle only
            val spawnMs = (if (complete) 480L else (200f / activity).toLong()).coerceIn(60L, 520L)
            val spawnCount = when {
                complete -> 1
                activity > 1.1f -> 6
                activity > 0.7f -> 4
                else -> 3
            }

            if (now - lastSpawn > spawnMs) {
                repeat(spawnCount) {
                    val free = particles.firstOrNull { !it.active } ?: return@repeat
                    val fromLeft = Math.random() < 0.5
                    val sx = if (fromLeft) width * 0.06f + (Math.random() * 50).toFloat()
                             else width * 0.76f + (Math.random() * 50).toFloat()
                    val sy = height * 0.18f + (Math.random() * height * 0.45f).toFloat()
                    val targets = mutableListOf<Pair<Float, Float>>()
                    // Always stream toward vehicle stages (even when complete — "maintenance nanos")
                    targets.add(cx + (Math.random() * 16 - 8).toFloat() to by + 4f)
                    targets.add(cx + (Math.random() * 20 - 10).toFloat() to by - engH * 0.5f)
                    targets.add(cx to by - engH - stage1H * (0.2f + Math.random().toFloat() * 0.6f))
                    targets.add(cx to by - engH - stage1H - stage2H * 0.5f)
                    targets.add(cx to by - engH - stage1H - stage2H - 40f * s)
                    val t = targets.random()
                    free.reset(sx, sy, t.first, t.second, orbitMode = complete)
                    if (!complete) {
                        val spdBoost = 0.85f + activity * 0.9f
                        free.vx *= spdBoost
                        free.vy *= spdBoost
                    }
                }
                lastSpawn = now
            }
            // Recycle if pool saturated
            if (particles.count { it.active } > 70) {
                particles.filter { it.active }.minByOrNull { it.life }?.active = false
            }
            particlePaint.color = Color.parseColor("#B8DCF0")
            for (p in particles) {
                if (p.active) {
                    p.update(dt)
                    particlePaint.alpha = (p.life * 255).toInt().coerceIn(110, 255)
                    canvas.drawCircle(p.x, p.y, p.size * 1.15f, particlePaint)  // tiny nanos
                }
            }
        }

        private fun drawNearRocketInfo(canvas: Canvas) {
            val cx = rocketCx
            val by = rocketBaseY
            val s = rocketScale
            val t = lastTele
            val analog = prefs.systemAnalog

            // Side columns — maximize usable vertical space under buttons
            val colW = width * 0.34f
            val leftCx = colW * 0.48f
            val rightCx = width - colW * 0.48f
            val colTop = height * 0.36f  // clear of side button labels
            val colBottom = by - height * 0.08f
            val colH = (colBottom - colTop).coerceAtLeast(height * 0.42f)

            if (analog) {
                val leftItems = 3
                val rightItems = 4
                val leftSlot = colH / leftItems
                val rightSlot = colH / rightItems
                // Density-aware radius: large on tall screens, always leave ~28% slot for text under dial
                val dens = resources.displayMetrics.density
                val leftR = (min(colW * 0.55f, leftSlot * 0.36f) * (0.92f + dens * 0.03f)).coerceIn(52f, 115f)
                val rightR = (min(colW * 0.52f, rightSlot * 0.34f) * (0.92f + dens * 0.03f)).coerceIn(48f, 105f)

                labelPaint.textSize = ts(15f)
                labelPaint.color = RetroSkin.cyan
                labelPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("SYSTEM", leftCx, colTop - 8f, labelPaint)
                labelPaint.color = RetroSkin.orange
                canvas.drawText("BUILD", rightCx, colTop - 8f, labelPaint)

                // Centers high in each slot so value+label sit under the arc inside the slot
                if (t != null) {
                    val bat = t.batteryPercent / 100f
                    val cpu = t.cpuPercent / 100f
                    val mem = if (t.maxMemMb > 0) t.usedMemMb.toFloat() / t.maxMemMb else 0f
                    drawMiniVu(canvas, leftCx, colTop + leftSlot * 0.36f, leftR, bat, "BAT")
                    drawMiniVu(canvas, leftCx, colTop + leftSlot * 1.36f, leftR, cpu, "CPU")
                    drawMiniVu(canvas, leftCx, colTop + leftSlot * 2.36f, leftR, mem, "MEM")
                }
                drawMiniVu(canvas, rightCx, colTop + rightSlot * 0.34f, rightR,
                    (state.budget / 1200f).coerceIn(0f, 1f), "BUD")
                drawMiniVu(canvas, rightCx, colTop + rightSlot * 1.34f, rightR,
                    state.overallProgress(), "RDY")
                drawMiniVu(canvas, rightCx, colTop + rightSlot * 2.34f, rightR,
                    state.engineProgress, "ENG")
                drawMiniVu(canvas, rightCx, colTop + rightSlot * 3.34f, rightR,
                    state.padProgress, "PAD")

                // Leader lines ONLY from BUILD gauges (right) → vehicle parts
                strokePaint.color = Color.argb(200, 0, 212, 255)
                strokePaint.strokeWidth = 2.6f
                val bodyX = rocketCx
                canvas.drawLine(rightCx - rightR * 0.95f, colTop + rightSlot * 0.34f,
                    bodyX + 18f * s, by - (58f + 195f + 90f) * s, strokePaint)
                canvas.drawLine(rightCx - rightR * 0.95f, colTop + rightSlot * 1.34f,
                    bodyX + 22f * s, by - (58f + 120f) * s, strokePaint)
                canvas.drawLine(rightCx - rightR * 0.95f, colTop + rightSlot * 2.34f,
                    bodyX + 24f * s, by - 28f * s, strokePaint)
                canvas.drawLine(rightCx - rightR * 0.95f, colTop + rightSlot * 3.34f,
                    bodyX + 32f * s, by + 8f, strokePaint)
            } else {
                // DIGITAL: LED numbers with even vertical slots — no overlap
                val leftCount = 3
                val rightCount = 4
                val leftSlot = colH / leftCount
                val rightSlot = colH / rightCount
                val barW = colW * 0.85f

                val dPal = com.ccos.retro.skin.SystemSkin.palette(prefs.systemTheme, prefs.lampBrightness)
                fun ledSlot(cx: Float, slotTop: Float, slotH: Float, label: String, value: Float, color: Int) {
                    val v = value.coerceIn(0f, 1f)
                    labelPaint.textSize = ts(13f)
                    labelPaint.color = dPal.text
                    labelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(label, cx, slotTop + slotH * 0.18f, labelPaint)
                    hudPaint.textSize = ts((slotH * 0.40f).coerceIn(20f, 34f))
                    hudPaint.color = color
                    hudPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("%03d".format((v * 100).toInt()), cx, slotTop + slotH * 0.55f, hudPaint)
                    val trackL = cx - barW / 2f
                    val trackR = cx + barW / 2f
                    val ty = slotTop + slotH * 0.72f
                    strokePaint.strokeWidth = 4f
                    strokePaint.color = dPal.dim
                    canvas.drawLine(trackL, ty, trackR, ty, strokePaint)
                    strokePaint.color = color
                    canvas.drawLine(trackL, ty, trackL + barW * v, ty, strokePaint)
                }

                labelPaint.textSize = ts(15f)
                labelPaint.color = dPal.primary
                labelPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("SYSTEM", leftCx, colTop - 8f, labelPaint)
                labelPaint.color = dPal.accent
                canvas.drawText("BUILD", rightCx, colTop - 8f, labelPaint)

                if (t != null) {
                    val mem = if (t.maxMemMb > 0) t.usedMemMb.toFloat() / t.maxMemMb else 0f
                    ledSlot(leftCx, colTop, leftSlot, "BATTERY", t.batteryPercent / 100f, dPal.good)
                    ledSlot(leftCx, colTop + leftSlot, leftSlot, "CPU", t.cpuPercent / 100f, dPal.warn)
                    ledSlot(leftCx, colTop + leftSlot * 2, leftSlot, "MEMORY", mem, dPal.primary)
                }
                ledSlot(rightCx, colTop, rightSlot, "BUDGET",
                    (state.budget / 1200f).coerceIn(0f, 1f), dPal.accent)
                ledSlot(rightCx, colTop + rightSlot, rightSlot, "READY",
                    state.overallProgress(), dPal.good)
                ledSlot(rightCx, colTop + rightSlot * 2, rightSlot, "ENGINE",
                    state.engineProgress, dPal.primary)
                ledSlot(rightCx, colTop + rightSlot * 3, rightSlot, "PAD",
                    state.padProgress, dPal.warn)
            }

            // ENGAGE under pad
            val eng = state.engagement
            val barW = 110f * s
            val barX = cx - barW / 2f
            val barY = by + 58f
            strokePaint.strokeWidth = 5f
            strokePaint.color = RetroSkin.dim
            canvas.drawLine(barX, barY, barX + barW, barY, strokePaint)
            strokePaint.color = when {
                eng > 0.7f -> RetroSkin.green
                eng > 0.35f -> RetroSkin.cyan
                else -> RetroSkin.orange
            }
            canvas.drawLine(barX, barY, barX + barW * (eng / 1.6f).coerceIn(0f, 1f), barY, strokePaint)
            labelPaint.textSize = ts(13f)
            labelPaint.color = RetroSkin.labelMuted
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("ENGAGE ${(eng * 100).roundToInt()}%", cx, barY + 16f, labelPaint)
            labelPaint.textAlign = Paint.Align.LEFT
            hudPaint.textAlign = Paint.Align.LEFT
        }

        private fun drawButtons(canvas: Canvas) {
            val onSys = prefs.isSystem()
            val onTel = prefs.isTelemetry()
            val labels = when {
                onSys -> systemModule.buttonLabels
                onTel -> telemetryModule.buttonLabels
                else -> rocketModule.buttonLabels
            }
            val activeIdx = when {
                onSys -> systemModule.activePage
                onTel -> telemetryModule.activePage
                else -> state.activeModule
            }
            val pal = if (onSys) com.ccos.retro.skin.SystemSkin.palette(prefs.systemTheme, prefs.lampBrightness) else null

            if (onTel) {
                drawTelemetryButtons(canvas, labels, activeIdx)
                return
            }

            for (i in 0 until 8) {
                val r = buttonRects[i]
                val active = activeIdx == i
                if (onSys) {
                    // SYSTEM MODULE — visible chips, lamp-scaled
                    val radius = 14f
                    val lamp = prefs.lampBrightness.coerceIn(0.35f, 1f)
                    val fillA = (if (active) 90 else 55) + (40 * lamp).toInt()
                    fillPaint.color = Color.argb(fillA.coerceIn(50, 160), 18, 28, 40)
                    canvas.drawRoundRect(r, radius, radius, fillPaint)
                    strokePaint.style = Paint.Style.STROKE
                    strokePaint.strokeWidth = if (active) 4.5f else 3.2f
                    strokePaint.color = if (pal != null) {
                        val a = (if (active) 255 else 160 + (60 * lamp).toInt()).coerceIn(120, 255)
                        Color.argb(a, Color.red(pal.primary), Color.green(pal.primary), Color.blue(pal.primary))
                    } else Color.argb(180, 100, 140, 180)
                    canvas.drawRoundRect(r, radius, radius, strokePaint)
                    if (active && pal != null) {
                        strokePaint.strokeWidth = 3.5f
                        strokePaint.color = pal.primary
                        canvas.drawLine(r.left + 10f, r.top + 5f, r.right - 10f, r.top + 5f, strokePaint)
                    }
                    hudPaint.textSize = btnTs(22f)
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = if (pal != null) {
                        val a = (if (active) 255 else 200 + (40 * lamp).toInt()).coerceIn(180, 255)
                        Color.argb(a, Color.red(pal.primary), Color.green(pal.primary), Color.blue(pal.primary))
                    } else Color.argb(230, 180, 200, 220)
                    canvas.drawText(labels[i], r.centerX(), r.centerY() + 8f, hudPaint)
                    if (labels[i] == "PAD") drawPadVersionMark(canvas, r, r.centerY() + 8f)
                } else {
                    // ROCKET MODULE — Mercury switch panel: beveled plate + flip switch
                    fillPaint.color = Color.parseColor("#2C3036")
                    canvas.drawRoundRect(r, 4f, 4f, fillPaint)
                    strokePaint.style = Paint.Style.STROKE
                    strokePaint.strokeWidth = 3.2f
                    strokePaint.color = Color.parseColor("#6A727A")
                    canvas.drawRoundRect(r, 4f, 4f, strokePaint)
                    val inset = 5f
                    fillPaint.color = Color.parseColor("#14181C")
                    canvas.drawRoundRect(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset * 2.2f, 3f, 3f, fillPaint)
                    val swCx = r.centerX()
                    val swCy = r.top + r.height() * 0.38f
                    val swW = r.width() * 0.42f
                    val swH = r.height() * 0.22f
                    fillPaint.color = if (active) Color.parseColor("#3A2010") else Color.parseColor("#1A1E22")
                    canvas.drawRoundRect(swCx - swW / 2, swCy - swH / 2, swCx + swW / 2, swCy + swH / 2, 3f, 3f, fillPaint)
                    strokePaint.color = if (active) RetroSkin.red else Color.parseColor("#5A6068")
                    strokePaint.strokeWidth = 1.5f
                    canvas.drawRoundRect(swCx - swW / 2, swCy - swH / 2, swCx + swW / 2, swCy + swH / 2, 3f, 3f, strokePaint)
                    val leverX = if (active) swCx + swW * 0.22f else swCx - swW * 0.22f
                    fillPaint.color = if (active) RetroSkin.orange else Color.parseColor("#8A9098")
                    canvas.drawCircle(leverX, swCy, swH * 0.45f, fillPaint)
                    fillPaint.color = if (active) RetroSkin.green else Color.parseColor("#2A3038")
                    canvas.drawCircle(r.left + 11f, r.top + 11f, 3.5f, fillPaint)
                    hudPaint.textSize = fitBtnText(labels[i], r.width() * 0.88f, r.height() * 0.40f, btnTs(18f))
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = if (active) RetroSkin.cyan else Color.parseColor("#8A929A")
                    canvas.drawText(labels[i], r.centerX(), r.bottom - 9f, hudPaint)
                    if (labels[i] == "PAD") drawPadVersionMark(canvas, r, r.bottom - 9f)
                }
            }
            hudPaint.textAlign = Paint.Align.LEFT
        }

        /** Lamp-modulated color (shared across modules). */
        private fun withLamp(c: Int, lamp: Float = prefs.lampBrightness): Int {
            val L = lamp.coerceIn(0.2f, 1f)
            val a = (Color.alpha(c).coerceAtLeast(180) * L).toInt().coerceIn(25, 255)
            // Slight desaturation when dim
            val r = (Color.red(c) * (0.55f + 0.45f * L)).toInt().coerceIn(0, 255)
            val g = (Color.green(c) * (0.55f + 0.45f * L)).toInt().coerceIn(0, 255)
            val b = (Color.blue(c) * (0.55f + 0.45f * L)).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }

        /** Agency-aware side buttons + column chrome. */
        private fun drawTelemetryButtons(canvas: Canvas, labels: Array<String>, activeIdx: Int) {
            val skin = TelemetrySkin.forLaunch(telemetryModule.tracked)
            val lamp = prefs.lampBrightness.coerceIn(0.25f, 1f)
            // Vertical rails connecting the button columns
            drawAgencyButtonChrome(canvas, skin, lamp)
            for (i in 0 until 8) {
                val r = buttonRects[i]
                val active = if (i == 7) telemetryModule.autoMode else activeIdx == i
                when (skin.btnStyle) {
                    TelemetrySkin.ButtonStyle.SPACEX -> drawSpacexBtn(canvas, r, labels[i], active, skin, lamp)
                    TelemetrySkin.ButtonStyle.NASA -> drawNasaBtn(canvas, r, labels[i], active, skin, lamp)
                    TelemetrySkin.ButtonStyle.CASC -> drawCascBtn(canvas, r, labels[i], active, skin, lamp)
                    TelemetrySkin.ButtonStyle.ROSCOSMOS -> drawRoscosmosBtn(canvas, r, labels[i], active, skin, lamp)
                    TelemetrySkin.ButtonStyle.ESA -> drawEsaBtn(canvas, r, labels[i], active, skin, lamp)
                    TelemetrySkin.ButtonStyle.GENERIC -> drawGenericTelBtn(canvas, r, labels[i], active, skin, lamp)
                }

            }
            hudPaint.textAlign = Paint.Align.LEFT
        }

        private fun drawAgencyButtonChrome(canvas: Canvas, skin: TelemetrySkin.Tokens, lamp: Float) {
            val leftX = buttonRects[0].centerX()
            val rightX = buttonRects[4].centerX()
            val topY = buttonRects[0].top - 8f
            val botY = buttonRects[3].bottom + 8f
            strokePaint.style = Paint.Style.STROKE
            when (skin.btnStyle) {
                TelemetrySkin.ButtonStyle.SPACEX -> {
                    // Thin vertical guide rails + tick marks
                    strokePaint.strokeWidth = 1.2f
                    strokePaint.color = withLamp(Color.parseColor("#3A3A3A"), lamp)
                    canvas.drawLine(leftX, topY, leftX, botY, strokePaint)
                    canvas.drawLine(rightX, topY, rightX, botY, strokePaint)
                    for (i in 0 until 4) {
                        val y = buttonRects[i].centerY()
                        canvas.drawLine(leftX - 6f, y, leftX + 6f, y, strokePaint)
                        canvas.drawLine(rightX - 6f, y, rightX + 6f, y, strokePaint)
                    }
                }
                TelemetrySkin.ButtonStyle.NASA -> {
                    // Double blue channel rails
                    strokePaint.strokeWidth = 2.5f
                    strokePaint.color = withLamp(skin.accent, lamp * 0.7f)
                    canvas.drawLine(leftX - 18f, topY, leftX - 18f, botY, strokePaint)
                    canvas.drawLine(rightX + 18f, topY, rightX + 18f, botY, strokePaint)
                    strokePaint.strokeWidth = 1f
                    strokePaint.color = withLamp(skin.danger, lamp * 0.5f)
                    canvas.drawLine(leftX - 22f, topY, leftX - 22f, botY, strokePaint)
                    canvas.drawLine(rightX + 22f, topY, rightX + 22f, botY, strokePaint)
                }
                TelemetrySkin.ButtonStyle.CASC -> {
                    strokePaint.strokeWidth = 2f
                    strokePaint.color = withLamp(skin.accent, lamp)
                    val inset = 14f
                    canvas.drawRoundRect(
                        buttonRects[0].left - inset, topY,
                        buttonRects[0].right + inset, botY, 4f, 4f, strokePaint
                    )
                    canvas.drawRoundRect(
                        buttonRects[4].left - inset, topY,
                        buttonRects[4].right + inset, botY, 4f, 4f, strokePaint
                    )
                    strokePaint.color = withLamp(skin.btnLampOn, lamp)
                    strokePaint.strokeWidth = 1.5f
                    for (i in 0 until 4) {
                        val y = buttonRects[i].centerY()
                        canvas.drawLine(buttonRects[0].left - inset - 4f, y, buttonRects[0].left - inset, y, strokePaint)
                        canvas.drawLine(buttonRects[4].right + inset, y, buttonRects[4].right + inset + 4f, y, strokePaint)
                    }
                }
                TelemetrySkin.ButtonStyle.ROSCOSMOS -> {
                    strokePaint.strokeWidth = 2.2f
                    strokePaint.color = withLamp(skin.accent, lamp)
                    canvas.drawLine(leftX - 16f, topY, leftX - 16f, botY, strokePaint)
                    canvas.drawLine(rightX + 16f, topY, rightX + 16f, botY, strokePaint)
                    strokePaint.strokeWidth = 1.5f
                    strokePaint.color = withLamp(skin.danger, lamp)
                    canvas.drawLine(leftX - 20f, topY, leftX - 20f, botY, strokePaint)
                    canvas.drawLine(rightX + 20f, topY, rightX + 20f, botY, strokePaint)
                }
                TelemetrySkin.ButtonStyle.ESA -> {
                    strokePaint.strokeWidth = 2.4f
                    strokePaint.color = withLamp(Color.parseColor("#003399"), lamp)
                    canvas.drawLine(leftX - 16f, topY, leftX - 16f, botY, strokePaint)
                    canvas.drawLine(rightX + 16f, topY, rightX + 16f, botY, strokePaint)
                    strokePaint.strokeWidth = 1.4f
                    strokePaint.color = withLamp(Color.parseColor("#FFD100"), lamp)
                    canvas.drawLine(leftX - 20f, topY, leftX - 20f, botY, strokePaint)
                    canvas.drawLine(rightX + 20f, topY, rightX + 20f, botY, strokePaint)
                }
                else -> {
                    strokePaint.strokeWidth = 1.5f
                    strokePaint.color = withLamp(skin.accentDim, lamp)
                    canvas.drawLine(leftX, topY, leftX, botY, strokePaint)
                    canvas.drawLine(rightX, topY, rightX, botY, strokePaint)
                }
            }
        }


        private fun zhFor(label: String): String = when (label) {
            "CMD" -> "指令"; "CDT" -> "倒计时"; "TEL" -> "遥测"; "STS" -> "状态"
            "PAD" -> "工位"; "VID" -> "视频"; "MSK" -> "任务"; "AUTO" -> "自动"; else -> ""
        }

        private fun ruFor(label: String): String = when (label) {
            "CMD" -> "КМД"; "CDT" -> "ОТСЧ"; "TEL" -> "ТЕЛ"; "STS" -> "СТАТ"
            "PAD" -> "СТАРТ"; "VID" -> "ВИДЕО"; "MSK" -> "МИСС"; "AUTO" -> "АВТО"; else -> ""
        }

        private fun frFor(label: String): String = when (label) {
            "CMD" -> "CMD"; "CDT" -> "COMPTE"; "TEL" -> "TÉL"; "STS" -> "ÉTAT"
            "PAD" -> "PAS"; "VID" -> "VIDÉO"; "MSK" -> "MISSION"; "AUTO" -> "AUTO"; else -> ""
        }

        private fun drawBevelPlate(
            canvas: Canvas,
            r: RectF,
            fill: Int,
            lamp: Float,
            rad: Float,
            glow: Int?,
            active: Boolean
        ) {
            if (active && glow != null) {
                fillPaint.shader = null
                fillPaint.color = Color.argb(
                    (88 * lamp).toInt().coerceIn(0, 120),
                    Color.red(glow), Color.green(glow), Color.blue(glow)
                )
                canvas.drawRoundRect(r.left - 4f, r.top - 4f, r.right + 4f, r.bottom + 4f, rad + 3f, rad + 3f, fillPaint)
            }
            fillPaint.shader = null
            fillPaint.color = Color.argb(160, 0, 0, 0)
            canvas.drawRoundRect(r.left + 1.5f, r.top + 3.5f, r.right + 1.5f, r.bottom + 3.5f, rad, rad, fillPaint)
            fillPaint.color = withLamp(fill, lamp)
            canvas.drawRoundRect(r, rad, rad, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.3f
            strokePaint.color = Color.argb(if (active) 130 else 55, 255, 255, 255)
            canvas.drawLine(r.left + 6f, r.top + 3f, r.right - 6f, r.top + 3f, strokePaint)
            strokePaint.color = Color.argb(90, 0, 0, 0)
            canvas.drawLine(r.left + 6f, r.bottom - 3f, r.right - 6f, r.bottom - 3f, strokePaint)
        }


        private fun drawPadVersionMark(canvas: Canvas, r: RectF, padBaselineY: Float) {
            val oldTf = hudPaint.typeface
            val oldAlign = hudPaint.textAlign
            val oldSize = hudPaint.textSize
            val oldColor = hudPaint.color
            val mark = BuildConfig.VERSION_CODE.toString()
            hudPaint.typeface = Typeface.MONOSPACE
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.textSize = (oldSize * 0.82f).coerceIn(16f, 22f)
            hudPaint.color = Color.argb(255, 230, 240, 255)
            val y = (padBaselineY - oldSize * 0.95f).coerceAtLeast(r.top + hudPaint.textSize + 2f)
            canvas.drawText(mark, r.centerX(), y, hudPaint)
            hudPaint.typeface = oldTf
            hudPaint.textAlign = oldAlign
            hudPaint.textSize = oldSize
            hudPaint.color = oldColor
        }

        private fun drawFullFaceLabel(
            canvas: Canvas,
            r: RectF,
            en: String,
            local: String,
            enColor: Int,
            locColor: Int,
            lamp: Float
        ) {
            hudPaint.textAlign = Paint.Align.CENTER
            if (en == "PAD") {
                val g = RectF(r.left + 8f, r.top + 6f, r.right - 8f, r.bottom - 6f)
                PadGlyph.draw(
                    canvas, g, withLamp(enColor, lamp * 0.38f),
                    PadGlyph.kind(telemetryModule.tracked), strokePaint
                )
            }
            val hasLocal = local.isNotEmpty() && local != en
            val enBoxH = r.height() * if (hasLocal) 0.48f else 0.62f
            val locBoxH = r.height() * 0.28f
            val enY = if (hasLocal) r.centerY() + enBoxH * 0.08f else r.centerY() + enBoxH * 0.22f
            hudPaint.color = withLamp(enColor, lamp)
            hudPaint.textSize = fitBtnText(en, r.width() * 0.90f, enBoxH, btnTs(22f))
            canvas.drawText(en, r.centerX(), enY, hudPaint)
            if (en == "PAD") drawPadVersionMark(canvas, r, enY)
            if (hasLocal) {
                hudPaint.color = withLamp(locColor, lamp)
                hudPaint.textSize = fitBtnText(local, r.width() * 0.90f, locBoxH, btnTs(15f))
                canvas.drawText(local, r.centerX(), r.bottom - r.height() * 0.10f, hudPaint)
            }
        }

        private fun drawLedBar(canvas: Canvas, r: RectF, on: Boolean, onColor: Int, lamp: Float) {
            val y = r.top + r.height() * 0.10f
            val h = (r.height() * 0.07f).coerceIn(3.5f, 7f)
            fillPaint.shader = null
            fillPaint.color = withLamp(if (on) onColor else Color.parseColor("#1A1A1A"), lamp)
            canvas.drawRoundRect(r.left + 8f, y - h * 0.5f, r.right - 8f, y + h * 0.5f, 2f, 2f, fillPaint)
            if (on) {
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = 1.2f
                strokePaint.color = withLamp(onColor, lamp * 0.7f)
                canvas.drawRoundRect(r.left + 6f, y - h * 0.7f, r.right - 6f, y + h * 0.7f, 3f, 3f, strokePaint)
            }
        }

        private fun drawSpacexBtn(canvas: Canvas, r: RectF, label: String, active: Boolean, skin: TelemetrySkin.Tokens, lamp: Float) {
            val fill = if (active) Color.parseColor("#1A1A1A") else Color.parseColor("#0A0A0A")
            drawBevelPlate(canvas, r, fill, lamp, 3f, if (active) Color.WHITE else null, active)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (active) 2.6f else 1.5f
            strokePaint.color = withLamp(if (active) Color.WHITE else Color.parseColor("#4A4A4A"), lamp)
            canvas.drawRect(r, strokePaint)
            fillPaint.color = withLamp(if (active) Color.parseColor("#BBBBBB") else Color.parseColor("#444444"), lamp)
            val rr = 2.4f
            canvas.drawCircle(r.left + 7f, r.top + 7f, rr, fillPaint)
            canvas.drawCircle(r.right - 7f, r.top + 7f, rr, fillPaint)
            canvas.drawCircle(r.left + 7f, r.bottom - 7f, rr, fillPaint)
            canvas.drawCircle(r.right - 7f, r.bottom - 7f, rr, fillPaint)
            drawLedBar(canvas, r, active, skin.btnLampOn, lamp)
            drawFullFaceLabel(
                canvas, r, label, "",
                if (active) Color.WHITE else Color.parseColor("#8A8A8A"),
                Color.parseColor("#666666"), lamp
            )
        }

        private fun drawNasaBtn(canvas: Canvas, r: RectF, label: String, active: Boolean, skin: TelemetrySkin.Tokens, lamp: Float) {
            val fill = if (active) Color.parseColor("#163056") else Color.parseColor("#0C1424")
            drawBevelPlate(canvas, r, fill, lamp, 5f, if (active) Color.parseColor("#0032A0") else null, active)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (active) 2.8f else 1.6f
            strokePaint.color = withLamp(if (active) Color.parseColor("#5AA0FF") else Color.parseColor("#2A3A55"), lamp)
            canvas.drawRoundRect(r, 5f, 5f, strokePaint)
            if (active) {
                strokePaint.strokeWidth = 2f
                strokePaint.color = withLamp(Color.parseColor("#C8102E"), lamp)
                canvas.drawLine(r.left + 8f, r.bottom - 5f, r.right - 8f, r.bottom - 5f, strokePaint)
            }
            drawLedBar(canvas, r, active, Color.parseColor("#C8102E"), lamp)
            drawFullFaceLabel(
                canvas, r, label, "",
                if (active) Color.parseColor("#F0F6FF") else Color.parseColor("#7A8898"),
                Color.parseColor("#5A6A80"), lamp
            )
        }

        private fun drawCascBtn(canvas: Canvas, r: RectF, label: String, active: Boolean, skin: TelemetrySkin.Tokens, lamp: Float) {
            val fill = if (active) Color.parseColor("#2A1010") else Color.parseColor("#140A0A")
            drawBevelPlate(canvas, r, fill, lamp, 3f, if (active) Color.parseColor("#DE2910") else null, active)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (active) 2.8f else 1.6f
            strokePaint.color = withLamp(if (active) Color.parseColor("#DE2910") else Color.parseColor("#5A2020"), lamp)
            canvas.drawRoundRect(r, 3f, 3f, strokePaint)
            strokePaint.strokeWidth = if (active) 2.4f else 1.4f
            strokePaint.color = withLamp(Color.parseColor("#FFD700"), if (active) lamp else lamp * 0.5f)
            val m = 9f
            canvas.drawLine(r.left + 2f, r.top + m, r.left + 2f, r.top + 2f, strokePaint)
            canvas.drawLine(r.left + 2f, r.top + 2f, r.left + m, r.top + 2f, strokePaint)
            canvas.drawLine(r.right - 2f, r.top + m, r.right - 2f, r.top + 2f, strokePaint)
            canvas.drawLine(r.right - 2f, r.top + 2f, r.right - m, r.top + 2f, strokePaint)
            canvas.drawLine(r.left + 2f, r.bottom - m, r.left + 2f, r.bottom - 2f, strokePaint)
            canvas.drawLine(r.left + 2f, r.bottom - 2f, r.left + m, r.bottom - 2f, strokePaint)
            canvas.drawLine(r.right - 2f, r.bottom - m, r.right - 2f, r.bottom - 2f, strokePaint)
            canvas.drawLine(r.right - 2f, r.bottom - 2f, r.right - m, r.bottom - 2f, strokePaint)
            drawLedBar(canvas, r, active, Color.parseColor("#FFD700"), lamp)
            drawFullFaceLabel(
                canvas, r, label, zhFor(label),
                if (active) Color.parseColor("#F5E6C8") else Color.parseColor("#A08060"),
                if (active) Color.parseColor("#FFD700") else Color.parseColor("#7A6040"),
                lamp
            )
        }

        private fun drawRoscosmosBtn(canvas: Canvas, r: RectF, label: String, active: Boolean, skin: TelemetrySkin.Tokens, lamp: Float) {
            val fill = if (active) Color.parseColor("#1A1810") else Color.parseColor("#0C1014")
            drawBevelPlate(canvas, r, fill, lamp, 4f, if (active) Color.parseColor("#E8C547") else null, active)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (active) 2.8f else 1.6f
            strokePaint.color = withLamp(if (active) Color.parseColor("#E8C547") else Color.parseColor("#6A5840"), lamp)
            canvas.drawRoundRect(r, 4f, 4f, strokePaint)
            fillPaint.shader = null
            fillPaint.color = withLamp(if (active) Color.parseColor("#C41E3A") else Color.parseColor("#2A1818"), lamp)
            canvas.drawCircle(r.left + 11f, r.top + 11f, 4.2f, fillPaint)
            drawLedBar(canvas, r, active, Color.parseColor("#C41E3A"), lamp)
            drawFullFaceLabel(
                canvas, r, label, ruFor(label),
                if (active) Color.parseColor("#F0E8D0") else Color.parseColor("#8A8070"),
                if (active) Color.parseColor("#E8C547") else Color.parseColor("#6A6558"),
                lamp
            )
        }

        private fun drawEsaBtn(canvas: Canvas, r: RectF, label: String, active: Boolean, skin: TelemetrySkin.Tokens, lamp: Float) {
            val fill = if (active) Color.parseColor("#183060") else Color.parseColor("#0C1428")
            drawBevelPlate(canvas, r, fill, lamp, 6f, if (active) Color.parseColor("#FFD100") else null, active)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (active) 2.8f else 1.7f
            strokePaint.color = withLamp(if (active) Color.parseColor("#FFD100") else Color.parseColor("#003399"), lamp)
            canvas.drawRoundRect(r, 6f, 6f, strokePaint)
            if (active) {
                strokePaint.strokeWidth = 2f
                strokePaint.color = withLamp(Color.parseColor("#003399"), lamp)
                canvas.drawRoundRect(r.left + 4f, r.top + 4f, r.right - 4f, r.bottom - 4f, 4f, 4f, strokePaint)
            }
            drawLedBar(canvas, r, active, Color.parseColor("#FFD100"), lamp)
            drawFullFaceLabel(
                canvas, r, label, frFor(label),
                if (active) Color.WHITE else Color.parseColor("#8A98C0"),
                if (active) Color.parseColor("#FFD100") else Color.parseColor("#5A6A90"),
                lamp
            )
        }

        private fun drawGenericTelBtn(canvas: Canvas, r: RectF, label: String, active: Boolean, skin: TelemetrySkin.Tokens, lamp: Float) {
            val fill = if (active) skin.btnActiveFill else skin.btnIdleFill
            drawBevelPlate(canvas, r, fill, lamp, 5f, if (active) skin.btnLampOn else null, active)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = if (active) 2.6f else 1.5f
            strokePaint.color = withLamp(if (active) skin.btnActiveStroke else skin.btnIdleStroke, lamp)
            canvas.drawRoundRect(r, 5f, 5f, strokePaint)
            drawLedBar(canvas, r, active, skin.btnLampOn, lamp)
            drawFullFaceLabel(
                canvas, r, label, "",
                if (active) skin.btnTextActive else skin.btnTextIdle,
                skin.muted, lamp
            )
        }

        private fun drawInteractivePanel(canvas: Canvas) {
            val panelLeft = width * 0.14f
            val panelRight = width * 0.86f
            val panelTop = height * 0.12f
            val onSys = prefs.isSystem()
            val analog = prefs.systemAnalog

            // System focus pages BAT/CPU/RAM/NET/SEN — gauge detail panels
            val sysFocus = onSys && state.activeModule in listOf(1, 2, 3, 5, 6)
            if (sysFocus) {
                val panelBottom = height * 0.72f
                panelBounds.set(panelLeft - 12f, panelTop - 16f, panelRight + 12f, panelBottom + 8f)
                // Fully opaque so wallpaper gauges do not bleed through
                fillPaint.color = Color.parseColor("#F20A1018")
                canvas.drawRoundRect(panelLeft - 10f, panelTop - 14f, panelRight + 10f, panelBottom, 14f, 14f, fillPaint)
                fillPaint.color = Color.parseColor("#E6080C12")
                canvas.drawRoundRect(panelLeft - 6f, panelTop - 10f, panelRight + 6f, panelBottom - 4f, 10f, 10f, fillPaint)
                val palF = com.ccos.retro.skin.SystemSkin.palette(prefs.systemTheme, prefs.lampBrightness)
                strokePaint.color = palF.primary
                strokePaint.strokeWidth = 2.2f
                canvas.drawRoundRect(panelLeft - 10f, panelTop - 14f, panelRight + 10f, panelBottom, 14f, 14f, strokePaint)
                val t = lastTele
                val title = when (state.activeModule) {
                    1 -> "BATTERY"
                    2 -> "CPU"
                    3 -> "MEMORY"
                    5 -> "NETWORK"
                    6 -> "SENSORS"
                    else -> "DETAIL"
                }
                hudPaint.color = RetroSkin.cyan
                hudPaint.textSize = ts(32f)
                hudPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(title, width / 2f, panelTop + 28f, hudPaint)
                val cx = width / 2f
                val panelH = panelBottom - panelTop
                val bigR = min(width * 0.28f, panelH * 0.28f)
                if (analog) {
                    when (state.activeModule) {
                        1 -> {
                            val bat = (t?.batteryPercent ?: 0) / 100f
                            // Large gauge — value drawn under by drawMiniVu
                            drawMiniVu(canvas, cx, panelTop + panelH * 0.32f, bigR * 1.25f, bat,
                                if (t?.isCharging == true) "CHARGING" else "BATTERY")
                            // Battery body silhouette at bottom
                            val bw = width * 0.28f
                            val bh = panelH * 0.12f
                            val bx = cx - bw / 2f
                            val by = panelBottom - panelH * 0.18f
                            strokePaint.color = RetroSkin.cyan
                            strokePaint.strokeWidth = 3f
                            canvas.drawRoundRect(bx, by, bx + bw, by + bh, 8f, 8f, strokePaint)
                            // terminal
                            canvas.drawRect(bx + bw, by + bh * 0.3f, bx + bw + 10f, by + bh * 0.7f, strokePaint)
                            fillPaint.color = if (t?.isCharging == true) RetroSkin.green else RetroSkin.orange
                            val fillW = (bw - 8f) * bat
                            canvas.drawRoundRect(bx + 4f, by + 4f, bx + 4f + fillW, by + bh - 4f, 4f, 4f, fillPaint)
                            // power flow lines when charging
                            if (t?.isCharging == true) {
                                strokePaint.color = RetroSkin.green
                                strokePaint.strokeWidth = 1.5f
                                val pulse = ((System.currentTimeMillis() / 80) % 20).toInt()
                                for (i in 0 until 5) {
                                    val lx = panelLeft + 20f + i * 18f + pulse
                                    canvas.drawLine(lx, by + bh / 2f, lx + 12f, by + bh / 2f, strokePaint)
                                }
                            }
                        }
                        2 -> {
                            val cpu = (t?.cpuPercent ?: 0) / 100f
                            drawMiniVu(canvas, cx, panelTop + panelH * 0.42f, bigR, cpu, "CPU")
                            drawSparkline(canvas, panelLeft + 24f, panelBottom - panelH * 0.22f,
                                panelRight - panelLeft - 48f, panelH * 0.14f, cpu)
                        }
                        3 -> {
                            val mem = if (t != null && t.maxMemMb > 0) t.usedMemMb.toFloat() / t.maxMemMb else 0f
                            drawMiniVu(canvas, cx, panelTop + panelH * 0.42f, bigR, mem, "RAM")
                            labelPaint.textAlign = Paint.Align.CENTER
                            labelPaint.textSize = ts(22f)
                            labelPaint.color = RetroSkin.cyan
                            canvas.drawText("${t?.usedMemMb ?: 0} / ${t?.maxMemMb ?: 0} MB",
                                cx, panelBottom - panelH * 0.12f, labelPaint)
                        }
                        5 -> {
                            drawSparkline(canvas, panelLeft + 24f, panelTop + panelH * 0.25f,
                                panelRight - panelLeft - 48f, panelH * 0.35f, 0.4f)
                            labelPaint.textSize = ts(20f)
                            labelPaint.color = RetroSkin.labelMuted
                            labelPaint.textAlign = Paint.Align.CENTER
                            canvas.drawText("RX / TX  ·  live link phase 2", cx, panelBottom - 40f, labelPaint)
                        }
                        6 -> {
                            labelPaint.textSize = ts(24f)
                            labelPaint.color = RetroSkin.cyan
                            labelPaint.textAlign = Paint.Align.CENTER
                            canvas.drawText("SENSORS  ·  accel / gyro phase 2", cx, panelTop + panelH * 0.45f, labelPaint)
                        }
                    }
                } else {
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = RetroSkin.green
                    when (state.activeModule) {
                        1 -> {
                            hudPaint.textSize = ts(110f)
                            canvas.drawText("%03d".format(t?.batteryPercent ?: 0), cx, panelTop + panelH * 0.40f, hudPaint)
                            hudPaint.textSize = ts(26f)
                            canvas.drawText("%", cx + ts(90f), panelTop + panelH * 0.28f, hudPaint)
                            hudPaint.textSize = ts(24f)
                            canvas.drawText(if (t?.isCharging == true) "CHARGING" else "DISCHARGING", cx, panelTop + panelH * 0.52f, hudPaint)
                            // Segmented charge bar
                            val segs = 20
                            val barW = (panelRight - panelLeft) * 0.7f
                            val segW = barW / segs
                            val barL = cx - barW / 2f
                            val barY = panelTop + panelH * 0.62f
                            val lit = ((t?.batteryPercent ?: 0) / 100f * segs).toInt()
                            for (i in 0 until segs) {
                                fillPaint.color = if (i < lit) RetroSkin.green else RetroSkin.dim
                                canvas.drawRoundRect(barL + i * segW + 1f, barY, barL + (i + 1) * segW - 1f, barY + 18f, 2f, 2f, fillPaint)
                            }
                        }
                        2 -> {
                            hudPaint.textSize = ts(96f)
                            canvas.drawText("%03d".format(t?.cpuPercent ?: 0), cx, panelTop + panelH * 0.45f, hudPaint)
                            hudPaint.textSize = ts(24f)
                            canvas.drawText("CPU  ·  ${t?.availableProcessors ?: 0} CORES", cx, panelTop + panelH * 0.58f, hudPaint)
                        }
                        3 -> {
                            hudPaint.textSize = ts(56f)
                            canvas.drawText("${t?.usedMemMb ?: 0}/${t?.maxMemMb ?: 0}", cx, panelTop + panelH * 0.42f, hudPaint)
                            hudPaint.textSize = ts(24f)
                            canvas.drawText("MB HEAP", cx, panelTop + panelH * 0.55f, hudPaint)
                        }
                        5 -> {
                            hudPaint.textSize = ts(36f)
                            canvas.drawText("NET  RX —  TX —", cx, panelTop + panelH * 0.42f, hudPaint)
                            hudPaint.textSize = ts(20f)
                            canvas.drawText("TRAFFIC  ·  PHASE 2", cx, panelTop + panelH * 0.55f, hudPaint)
                        }
                        6 -> {
                            hudPaint.textSize = ts(28f)
                            canvas.drawText("SENSOR FEED  ·  PHASE 2", cx, panelTop + panelH * 0.45f, hudPaint)
                        }
                    }
                }
                hudPaint.textAlign = Paint.Align.LEFT
                return
            }

            if (activeSliders.isEmpty() && state.activeModule != 0) return

            val rockerH = panelRockerH()
            val labelSz = panelLabelSize()
            val titleSz = panelTitleSize()
            val sectionGap = su(0.028f)
            extraRockerHits.clear()

            // Layout every control first, then paint the shell around the last row.
            // Previous builds drew the border first, so LAMP / analog sat outside the box.
            var y = panelTop + titleSz + su(0.022f)
            if (state.activeModule == 0) {
                y += labelSz + 8f
                layoutRockerRow(textRockerRects, panelLeft, y, panelRight, rockerH)
                y = textRockerRects[0].bottom + sectionGap
                y += labelSz + 8f
                layoutRockerRow(lampRockerRects, panelLeft, y, panelRight, rockerH)
                y = lampRockerRects[0].bottom + sectionGap
            }

            val sliderLayouts = mutableListOf<Pair<LiveSlider, Array<RectF>>>()
            for (s in activeSliders) {
                y += labelSz + 8f
                val rects = arrayOf(RectF(), RectF(), RectF())
                layoutRockerRow(rects, panelLeft, y, panelRight, rockerH)
                sliderLayouts.add(s to rects)
                val onChange = s.def.onChange
                extraRockerHits.add(rects[0] to { onChange(0.20f); s.value = 0.20f })
                extraRockerHits.add(rects[1] to { onChange(0.55f); s.value = 0.55f })
                extraRockerHits.add(rects[2] to { onChange(1.00f); s.value = 1.00f })
                y = rects[0].bottom + su(0.04f)
            }

            if (state.activeModule == 0) {
                val chipH = panelToggleH()
                val mid = (panelLeft + panelRight) / 2f
                modeChipAnalog.set(panelLeft + 8f, y, mid - 8f, y + chipH)
                modeChipDigital.set(mid + 8f, y, panelRight - 8f, y + chipH)
                y += chipH + 16f
                if (!onSys) {
                    resetChipRect.set(panelLeft + 8f, y, panelRight - 8f, y + height * 0.055f)
                    y = resetChipRect.bottom + 16f
                } else {
                    resetChipRect.setEmpty()
                }
            }

            val panelBottom = y + su(0.018f)
            panelBounds.set(panelLeft - 16f, panelTop - 28f, panelRight + 16f, panelBottom + 8f)
            drawConsoleShell(
                canvas,
                panelLeft - 12f, panelTop - 20f, panelRight + 12f, panelBottom,
                RetroSkin.cyan
            )

            hudPaint.color = RetroSkin.cyan
            hudPaint.textSize = titleSz
            hudPaint.textAlign = Paint.Align.CENTER
            val title = when (state.activeModule) {
                0 -> "COMMAND"
                1 -> "ENGINES"
                2 -> "ROCKET BODY"
                3 -> "PAD / TOWER"
                4 -> "TELEMETRY"
                5 -> "SUBSYSTEMS"
                6 -> "PERSONNEL"
                7 -> "BUDGET / WORK"
                else -> "MODULE"
            }
            canvas.drawText(title, (panelLeft + panelRight) / 2f, panelTop + titleSz * 0.82f, hudPaint)
            hudPaint.textAlign = Paint.Align.LEFT

            if (state.activeModule == 0) {
                labelPaint.color = Color.parseColor("#F0F6FC")
                labelPaint.textSize = labelSz
                labelPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("HUD TEXT", panelLeft, textRockerRects[0].top - 10f, labelPaint)
                drawRockerRow(
                    canvas, textRockerRects, AppPrefs.ROCKER_LABELS_TEXT, prefs.textStepIndex(),
                    RetroSkin.cyan, Color.WHITE, RetroSkin.labelMuted
                )
                canvas.drawText("LAMP", panelLeft, lampRockerRects[0].top - 10f, labelPaint)
                drawRockerRow(
                    canvas, lampRockerRects, AppPrefs.ROCKER_LABELS_LAMP, prefs.lampStepIndex(),
                    RetroSkin.orange, Color.WHITE, RetroSkin.labelMuted
                )
            }

            if (state.activeModule == 4 && activeSliders.isEmpty()) {
                labelPaint.color = RetroSkin.labelMuted
                labelPaint.textSize = 24f
                canvas.drawText("Live device data only", panelLeft, panelTop + 50f, labelPaint)
                canvas.drawText("No simulated numbers", panelLeft, panelTop + 80f, labelPaint)
            }

            for ((s, rects) in sliderLayouts) {
                labelPaint.color = Color.parseColor("#E8F0F8")
                labelPaint.textSize = labelSz
                labelPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(s.def.label, panelLeft, rects[0].top - 10f, labelPaint)
                drawRockerRow(
                    canvas, rects, arrayOf("LOW", "MED", "HIGH"), nearestAllocStep(s.value),
                    RetroSkin.orange, Color.WHITE, RetroSkin.labelMuted
                )
            }

            if (state.activeModule == 0) {
                fun drawChip(r: RectF, label: String, on: Boolean) {
                    fillPaint.color = if (on) Color.parseColor("#C8102E") else Color.parseColor("#1A2228")
                    canvas.drawRoundRect(r, 10f, 10f, fillPaint)
                    strokePaint.color = if (on) RetroSkin.orange else RetroSkin.dim
                    strokePaint.strokeWidth = if (on) 4.5f else 3.2f
                    canvas.drawRoundRect(r, 10f, 10f, strokePaint)
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.textSize = r.height() * 0.38f
                    hudPaint.color = if (on) Color.WHITE else Color.parseColor("#D0DCE8")
                    canvas.drawText(label, r.centerX(), r.centerY() + r.height() * 0.14f, hudPaint)
                }
                drawChip(modeChipAnalog, "ANALOG", prefs.systemAnalog)
                drawChip(modeChipDigital, "DIGITAL", !prefs.systemAnalog)

                if (!onSys && !resetChipRect.isEmpty) {
                    fillPaint.color = Color.parseColor("#C62828")
                    canvas.drawRoundRect(resetChipRect, 8f, 8f, fillPaint)
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.textSize = labelSz
                    hudPaint.color = Color.WHITE
                    canvas.drawText("RESET BUILD", resetChipRect.centerX(), resetChipRect.centerY() + 8f, hudPaint)
                }
                themeChipRects.clear()
                hudPaint.textAlign = Paint.Align.LEFT
            }
        }

        private fun drawConsoleShell(
            canvas: Canvas,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            stroke: Int
        ) {
            fillPaint.color = Color.parseColor("#F20A1018")
            canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, fillPaint)
            fillPaint.color = Color.parseColor("#E6080C12")
            canvas.drawRoundRect(left + 5f, top + 5f, right - 5f, bottom - 5f, 12f, 12f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 4.5f
            strokePaint.color = stroke
            canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, strokePaint)
        }

        private fun drawMiniVu(canvas: Canvas, cx: Float, cy: Float, radius: Float, value: Float, label: String) {
            val v = value.coerceIn(0f, 1f)
            val strokeW = (radius * 0.16f).coerceIn(6f, 16f)
            val lamp = prefs.lampBrightness.coerceIn(0.25f, 1f)
            val pal = com.ccos.retro.skin.SystemSkin.palette(prefs.systemTheme, lamp)
            fun la(c: Int): Int {
                val a = (Color.alpha(c).coerceAtLeast(200) * lamp).toInt().coerceIn(50, 255)
                return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
            }
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = strokeW
            // Outer ring
            strokePaint.color = la(pal.dim)
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 140f, 260f, false, strokePaint)
            // Value arc — theme-aware bands
            strokePaint.color = la(when {
                v > 0.85f -> pal.good
                v > 0.55f -> pal.accent
                v > 0.30f -> pal.warn
                else -> Color.parseColor("#FF5252")
            })
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 140f, 260f * v, false, strokePaint)
            // Inner thin ring
            strokePaint.strokeWidth = (radius * 0.04f).coerceIn(1.5f, 3f)
            strokePaint.color = la(pal.grid)
            canvas.drawArc(cx - radius * 0.72f, cy - radius * 0.72f, cx + radius * 0.72f, cy + radius * 0.72f,
                140f, 260f, false, strokePaint)
            strokePaint.strokeWidth = strokeW
            // Needle
            val ang = Math.toRadians((140.0 + 260.0 * v))
            strokePaint.color = la(pal.primary)
            strokePaint.strokeWidth = (radius * 0.08f).coerceIn(3f, 6f)
            val needleLen = radius * 0.80f
            canvas.drawLine(cx, cy, cx + needleLen * cos(ang).toFloat(), cy + needleLen * sin(ang).toFloat(), strokePaint)
            // Hub
            fillPaint.color = la(pal.warn)
            canvas.drawCircle(cx, cy, (radius * 0.13f).coerceIn(5f, 10f), fillPaint)
            fillPaint.color = la(pal.primary)
            canvas.drawCircle(cx, cy, (radius * 0.06f).coerceIn(2.5f, 5f), fillPaint)
            // Number + label OUTSIDE the dial — below the lowest point of the arc
            val numSize = (radius * 0.34f).coerceIn(16f, 30f)
            val baseY = cy + radius + numSize * 0.55f   // clear of arc body
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.textSize = numSize
            hudPaint.color = la(when {
                v > 0.7f -> pal.good
                v > 0.35f -> pal.warn
                else -> Color.parseColor("#FF5252")
            })
            canvas.drawText("${(v * 100).toInt()}", cx, baseY, hudPaint)
            labelPaint.textAlign = Paint.Align.CENTER
            labelPaint.textSize = (radius * 0.26f).coerceIn(12f, 18f)
            labelPaint.color = la(pal.text)
            canvas.drawText(label, cx, baseY + labelPaint.textSize * 1.25f, labelPaint)
            labelPaint.textAlign = Paint.Align.LEFT
            hudPaint.textAlign = Paint.Align.LEFT
        }

        private fun drawSparkline(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, value: Float) {
            strokePaint.color = RetroSkin.dim
            strokePaint.strokeWidth = 1f
            canvas.drawRect(x, y, x + w, y + h, strokePaint)
            strokePaint.color = RetroSkin.cyan
            strokePaint.strokeWidth = 2.5f
            val v = value.coerceIn(0.05f, 1f)
            var prevX = x
            var prevY = y + h * (1f - v * 0.5f)
            for (i in 1..24) {
                val nx = x + w * i / 24f
                val wave = (0.4f + 0.6f * v) * (0.5f + 0.5f * sin(i * 0.7f + value * 8f).toFloat())
                val ny = y + h * (1f - wave)
                canvas.drawLine(prevX, prevY, nx, ny, strokePaint)
                prevX = nx; prevY = ny
            }
        }

        // ------------------------------------------------------------------
        // LIVE ROCKET TELEMETRY SURFACE
        // ------------------------------------------------------------------

        private fun drawTelemetrySurface(canvas: Canvas, now: Long) {
            val launch = telemetryModule.tracked
            val skin = TelemetrySkin.forLaunch(launch)
            // Full per-module text scale — do NOT clamp (was killing slider effect)
            val ts = prefs.textScale
            val lamp = prefs.lampBrightness.coerceIn(0.35f, 1f)


            canvas.drawColor(skin.bg)
            val trackingPage = telemetryModule.activePage == 0 || telemetryModule.activePage == 2
            drawAgencyBackground(canvas, skin, lamp, showMark = !trackingPage && telemetryModule.activePage != 4)

            // === ALWAYS-ON TOP HUD (clock / countdown / agency) ===
            drawTelemetryTopBar(canvas, launch, skin, ts, lamp, now)

            when (telemetryModule.activePage) {
                0, 2, 7 -> drawTelMetrics(canvas, launch, skin, ts, now)
                1 -> drawTelCountdown(canvas, launch, skin, ts, now)
                3 -> drawTelStatus(canvas, launch, skin, ts)
                4 -> drawTelPad(canvas, launch, skin, ts)
                5 -> drawTelVideo(canvas, launch, skin, ts)
                6 -> drawTelMission(canvas, launch, skin, ts)
                else -> drawTelCountdown(canvas, launch, skin, ts, now)
            }
        }

        /** Large permanent top stack: local time, T- clock, agency/mission. */
        private fun drawTelemetryTopBar(
            canvas: Canvas,
            launch: com.ccos.retro.data.LaunchSnapshot?,
            skin: TelemetrySkin.Tokens,
            ts: Float,
            lamp: Float,
            now: Long
        ) {
            telBold()
            layoutButtons()
            val gapL = laneLeft()
            val gapR = laneRight()
            val gapW = (gapR - gapL).coerceAtLeast(8f)
            val cx = (gapL + gapR) / 2f
            val topInset = height * 0.062f

            val cal = java.util.Calendar.getInstance()
            val is24 = DateFormat.is24HourFormat(this@RetroCommandWallpaperService)
            val timeStr = if (is24) {
                String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY),
                    cal.get(java.util.Calendar.MINUTE))
            } else {
                val h = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
                String.format("%d:%02d", h, cal.get(java.util.Calendar.MINUTE))
            }
            val tSec = telemetryModule.effectiveSecondsFromNet(now)
            val absSecs = kotlin.math.abs(tSec).toLong()
            val sign = if (tSec <= 0f) "T-" else "T+"
            val hh = absSecs / 3600
            val mm = (absSecs % 3600) / 60
            val ss = absSecs % 60
            val cdtBase = if (hh > 0) String.format("%s%02d:%02d:%02d", sign, hh, mm, ss)
            else String.format("%s%02d:%02d", sign, mm, ss)
            val cdt = if (telemetryModule.clockIsEst()) "$cdtBase EST" else cdtBase
            val failed = hudFailed(launch, tSec)
            val line3 = when {
                failed -> "FAIL · ${launch?.name?.take(22) ?: "VEHICLE"}"
                telemetryModule.forceStatus != null -> "SCRUBBED · ${launch?.name?.take(22) ?: ""}"
                telemetryModule.simSecondsFromNet != null -> "SIM · ${launch?.name?.take(26) ?: skin.label}"
                launch != null -> "${launch.provider.take(12)} · ${launch.name.take(20)}"
                else -> "NO LAUNCH TRACKED"
            }

            val phone = isPhoneDesk()
            val clockH = height * if (phone) 0.088f else 0.130f
            val cdtH = height * if (phone) 0.062f else 0.088f
            val metaH = height * if (phone) 0.022f else 0.026f
            val clockSize = telFit("00:00", gapW, clockH, if (phone) 72f else 160f)
            val cdtSize = telFit(if (telemetryModule.clockIsEst()) "T-00:00:00 EST" else "T-00:00:00", gapW, cdtH, if (phone) 44f else 96f)
            val metaSize = telFit(line3, gapW, metaH, 16f)
            val clockBaseline = topInset + clockSize * 0.86f
            val cdtBaseline = clockBaseline + cdtSize * 1.02f
            val metaBaseline = cdtBaseline + metaSize * 1.15f
            val barH = metaBaseline + 8f
            telHudBottom = barH

            canvas.save()
            canvas.clipRect(gapL, 0f, gapR, barH + 4f)

            fillPaint.color = withLamp(skin.panel, lamp)
            canvas.drawRect(gapL, topInset - 6f, gapR, barH, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = su(0.004f).coerceIn(2.5f, 4f)
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawLine(gapL, barH, gapR, barH, strokePaint)

            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(Color.WHITE, lamp)
            hudPaint.textSize = clockSize
            canvas.drawText(timeStr, cx, clockBaseline, hudPaint)

            hudPaint.color = withLamp(
                when {
                    failed -> skin.danger
                    telemetryModule.forceStatus != null -> skin.hold
                    tSec in -15f..30f -> skin.go
                    else -> skin.accent
                }, lamp
            )
            hudPaint.textSize = cdtSize
            canvas.drawText(cdt, cx, cdtBaseline, hudPaint)
            hudPaint.color = withLamp(if (failed) skin.danger else skin.text, lamp)
            hudPaint.textSize = metaSize
            canvas.drawText(line3, cx, metaBaseline, hudPaint)
            canvas.restore()
        }






        /**
         * SpaceX X — sharp tapered strokes with pointed tips + signature swoosh.
         * Matches the real wordmark geometry (clean X, not a blob).
         */
        private fun drawSpaceXLogo(canvas: Canvas, cx: Float, cy: Float, size: Float, L: Float) {
            val a = size * 0.50f
            val halfW = size * 0.048f
            val tip = size * 0.012f
            val col = Color.argb((170 * L).toInt(), 255, 255, 255)

            fun sharpBar(x1: Float, y1: Float, x2: Float, y2: Float) {
                val dx = x2 - x1
                val dy = y2 - y1
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                if (len < 1f) return
                val nx = -dy / len
                val ny = dx / len
                val p = Path()
                p.moveTo(x1, y1) // tip
                p.lineTo(x1 + nx * tip + dx * 0.04f, y1 + ny * tip + dy * 0.04f)
                p.lineTo((x1 + x2) / 2f + nx * halfW, (y1 + y2) / 2f + ny * halfW)
                p.lineTo(x2 + nx * tip - dx * 0.04f, y2 + ny * tip - dy * 0.04f)
                p.lineTo(x2, y2) // tip
                p.lineTo(x2 - nx * tip - dx * 0.04f, y2 - ny * tip - dy * 0.04f)
                p.lineTo((x1 + x2) / 2f - nx * halfW, (y1 + y2) / 2f - ny * halfW)
                p.lineTo(x1 - nx * tip + dx * 0.04f, y1 - ny * tip + dy * 0.04f)
                p.close()
                fillPaint.color = col
                canvas.drawPath(p, fillPaint)
            }

            // Two sharp X arms
            sharpBar(cx - a, cy - a * 0.90f, cx + a, cy + a * 0.90f)
            sharpBar(cx + a, cy - a * 0.90f, cx - a, cy + a * 0.90f)

            // Signature upward swoosh cutting the X
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = size * 0.085f
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.color = Color.argb((190 * L).toInt(), 255, 255, 255)
            val swoosh = Path()
            swoosh.moveTo(cx - a * 1.15f, cy + a * 0.28f)
            swoosh.cubicTo(
                cx - a * 0.25f, cy - a * 0.05f,
                cx + a * 0.40f, cy - a * 0.55f,
                cx + a * 1.20f, cy - a * 0.78f
            )
            canvas.drawPath(swoosh, strokePaint)
            strokePaint.strokeCap = Paint.Cap.ROUND

            hudPaint.color = Color.argb((50 * L).toInt(), 220, 220, 220)
            hudPaint.textSize = size * 0.15f
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("SPACEX", cx, cy + a * 1.40f, hudPaint)
        }




        /** NASA "meatball" — blue disk, white NASA word, red vector, stars, orbit. */
        private fun drawNasaMeatball(canvas: Canvas, cx: Float, cy: Float, R: Float, L: Float) {
            // Blue disk fill
            fillPaint.color = Color.argb((210 * L).toInt(), 0, 50, 160)
            canvas.drawCircle(cx, cy, R, fillPaint)
            // White rim
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = R * 0.04f
            strokePaint.color = Color.argb((180 * L).toInt(), 240, 245, 255)
            canvas.drawCircle(cx, cy, R, strokePaint)
            // Orbit ellipse (white)
            strokePaint.strokeWidth = R * 0.05f
            strokePaint.color = Color.argb((200 * L).toInt(), 255, 255, 255)
            canvas.drawOval(cx - R * 0.95f, cy - R * 0.28f, cx + R * 0.95f, cy + R * 0.28f, strokePaint)
            // Red chevron / vector wing
            val wing = Path()
            wing.moveTo(cx - R * 0.95f, cy - R * 0.08f)
            wing.lineTo(cx + R * 0.15f, cy - R * 0.55f)
            wing.lineTo(cx + R * 0.95f, cy - R * 0.72f)
            wing.lineTo(cx + R * 0.05f, cy - R * 0.12f)
            wing.close()
            fillPaint.color = Color.argb((200 * L).toInt(), 200, 16, 46)
            canvas.drawPath(wing, fillPaint)
            // Stars
            fillPaint.color = Color.argb((210 * L).toInt(), 255, 255, 255)
            val starR = R * 0.035f
            canvas.drawCircle(cx - R * 0.35f, cy + R * 0.25f, starR, fillPaint)
            canvas.drawCircle(cx - R * 0.15f, cy + R * 0.45f, starR * 0.8f, fillPaint)
            canvas.drawCircle(cx + R * 0.25f, cy + R * 0.35f, starR * 0.7f, fillPaint)
            canvas.drawCircle(cx + R * 0.45f, cy + R * 0.15f, starR, fillPaint)
            canvas.drawCircle(cx - R * 0.55f, cy - R * 0.15f, starR * 0.6f, fillPaint)
            // NASA word
            hudPaint.color = Color.argb((220 * L).toInt(), 255, 255, 255)
            hudPaint.textSize = R * 0.42f
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("NASA", cx, cy + R * 0.12f, hudPaint)
        }

        /** CASC mark approximation: upward arrow + three orbital rings (group identity). */
        private fun drawCascLogo(canvas: Canvas, cx: Float, cy: Float, size: Float, L: Float) {
            strokePaint.style = Paint.Style.STROKE
            // Three concentric rings (three cosmic velocities motif)
            for (i in 1..3) {
                strokePaint.strokeWidth = size * (0.04f - i * 0.005f)
                strokePaint.color = Color.argb(((100 - i * 15) * L).toInt(), 222, 41, 16)
                val rr = size * (0.35f + i * 0.22f)
                canvas.drawCircle(cx, cy, rr, strokePaint)
            }
            // Upward arrow (spacecraft launch)
            fillPaint.color = Color.argb((170 * L).toInt(), 222, 41, 16)
            val arrow = Path()
            arrow.moveTo(cx, cy - size * 0.85f)
            arrow.lineTo(cx + size * 0.22f, cy - size * 0.35f)
            arrow.lineTo(cx + size * 0.08f, cy - size * 0.35f)
            arrow.lineTo(cx + size * 0.08f, cy + size * 0.55f)
            arrow.lineTo(cx - size * 0.08f, cy + size * 0.55f)
            arrow.lineTo(cx - size * 0.08f, cy - size * 0.35f)
            arrow.lineTo(cx - size * 0.22f, cy - size * 0.35f)
            arrow.close()
            canvas.drawPath(arrow, fillPaint)
            // Gold accent tip
            fillPaint.color = Color.argb((180 * L).toInt(), 255, 215, 0)
            val tip = Path()
            tip.moveTo(cx, cy - size * 0.85f)
            tip.lineTo(cx + size * 0.12f, cy - size * 0.55f)
            tip.lineTo(cx - size * 0.12f, cy - size * 0.55f)
            tip.close()
            canvas.drawPath(tip, fillPaint)
        }

        /** ESA roundel approximation: Earth disk, lowercase e, orbit parallels, satellite dot. */
        private fun drawEsaLogo(canvas: Canvas, cx: Float, cy: Float, R: Float, L: Float) {
            fillPaint.color = Color.argb((150 * L).toInt(), 0, 50, 120)
            canvas.drawCircle(cx, cy, R, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = R * 0.04f
            strokePaint.color = Color.argb((180 * L).toInt(), 200, 220, 255)
            canvas.drawCircle(cx, cy, R, strokePaint)
            // Parallel arcs (fingerprint / orbits)
            strokePaint.strokeWidth = R * 0.03f
            for (i in 0..4) {
                val inset = R * (0.25f + i * 0.12f)
                canvas.drawArc(cx - inset, cy - R * 0.55f, cx + inset * 0.6f, cy + R * 0.55f,
                    70f, 140f, false, strokePaint)
            }
            // Satellite dot
            fillPaint.color = Color.argb((200 * L).toInt(), 255, 255, 255)
            canvas.drawCircle(cx - R * 0.45f, cy - R * 0.15f, R * 0.06f, fillPaint)
            // e letter
            hudPaint.color = Color.argb((200 * L).toInt(), 255, 255, 255)
            hudPaint.textSize = R * 0.7f
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("e", cx + R * 0.15f, cy + R * 0.25f, hudPaint)
            hudPaint.textSize = R * 0.28f
            canvas.drawText("ESA", cx, cy + R * 1.35f, hudPaint)
        }

        /**
         * Agency vector backgrounds — intentionally strong so lamp can brighten them.
         * Base alpha is higher; lamp multiplies up from there.
         */
        private fun drawAgencyBackground(
            canvas: Canvas,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            showMark: Boolean = true
        ) {

            strokePaint.style = Paint.Style.STROKE
            // Boost: lamp 0.35→ visible, 1.0→ bold
            val L = (0.45f + 0.55f * lamp).coerceIn(0.4f, 1f)
            when (skin.btnStyle) {
                TelemetrySkin.ButtonStyle.SPACEX -> {
                    strokePaint.strokeWidth = 1f
                    strokePaint.color = Color.argb((55 * L).toInt(), 60, 60, 60)
                    val step = 44f
                    var x = 0f
                    while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), strokePaint); x += step }
                    var y = 0f
                    while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, strokePaint); y += step }
                    if (showMark) drawSpaceXLogo(canvas, width / 2f, height * 0.26f, width * 0.18f, L)
                }





                TelemetrySkin.ButtonStyle.NASA -> {
                    strokePaint.strokeWidth = 1.2f
                    strokePaint.color = Color.argb((70 * L).toInt(), 30, 50, 90)
                    val step = 48f
                    var x = 0f
                    while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), strokePaint); x += step }
                    var y = 0f
                    while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, strokePaint); y += step }
                    if (showMark) drawNasaMeatball(canvas, width / 2f, height * 0.26f, width * 0.15f, L)
                }



                TelemetrySkin.ButtonStyle.CASC -> {
                    strokePaint.strokeWidth = 1.1f
                    strokePaint.color = Color.argb((80 * L).toInt(), 80, 30, 25)
                    val step = 42f
                    var x = 0f
                    while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), strokePaint); x += step }
                    var y = 0f
                    while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, strokePaint); y += step }
                    if (showMark) {
                        drawCascLogo(canvas, width / 2f, height * 0.26f, width * 0.14f, L)
                        hudPaint.color = Color.argb((40 * L).toInt(), 245, 230, 200)
                        hudPaint.textSize = width * 0.045f
                        hudPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText("中国航天", width / 2f, height * 0.36f, hudPaint)
                    }
                }




                TelemetrySkin.ButtonStyle.ROSCOSMOS -> {
                    strokePaint.strokeWidth = 1.2f
                    strokePaint.color = Color.argb((90 * L).toInt(), 50, 55, 45)
                    val step = 44f
                    var x = 0f
                    while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), strokePaint); x += step }
                    var y = 0f
                    while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, strokePaint); y += step }
                    // Gold frame
                    strokePaint.strokeWidth = 3f
                    strokePaint.color = Color.argb((150 * L).toInt(), 232, 197, 71)
                    val m = 24f
                    canvas.drawRoundRect(m, m, width - m, height - m, 4f, 4f, strokePaint)
                    if (showMark) {
                        val scx = width / 2f
                        val scy = height * 0.26f
                        val sr = width * 0.08f
                        val star = Path()
                        for (i in 0 until 5) {
                            val a = Math.toRadians(-90.0 + i * 72.0)
                            val px = scx + (sr * cos(a)).toFloat()
                            val py = scy + (sr * sin(a)).toFloat()
                            if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
                            val a2 = Math.toRadians(-90.0 + i * 72.0 + 36.0)
                            star.lineTo(scx + (sr * 0.4f * cos(a2)).toFloat(), scy + (sr * 0.4f * sin(a2)).toFloat())
                        }
                        star.close()
                        fillPaint.color = Color.argb((170 * L).toInt(), 196, 30, 58)
                        canvas.drawPath(star, fillPaint)
                        strokePaint.strokeWidth = 2f
                        strokePaint.color = Color.argb((180 * L).toInt(), 232, 197, 71)
                        canvas.drawPath(star, strokePaint)
                        hudPaint.color = Color.argb((55 * L).toInt(), 232, 197, 71)
                        hudPaint.textSize = width * 0.05f
                        hudPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText("РОСКОСМОС", width / 2f, height * 0.36f, hudPaint)
                    }
                }
                else -> {
                    strokePaint.strokeWidth = 1.1f
                    strokePaint.color = Color.argb((70 * L).toInt(), 0, 60, 100)
                    val step = 44f
                    var x = 0f
                    while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), strokePaint); x += step }
                    var y = 0f
                    while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, strokePaint); y += step }
                    if (showMark) drawEsaLogo(canvas, width / 2f, height * 0.26f, width * 0.12f, L)
                }



            }
        }



        private fun drawTelCmdHint(canvas: Canvas, skin: TelemetrySkin.Tokens, ts: Float, lamp: Float = prefs.lampBrightness) {
            // Cap hint text — full HUD scale is for clock/metrics, not this overlay
            val hintScale = ts.coerceIn(1.2f, 2.4f)
            hudPaint.color = withLamp(skin.muted, lamp)
            hudPaint.textSize = 16f * hintScale
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("CMD · PANEL OPEN", width / 2f, height * 0.28f, hudPaint)
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = 13f * hintScale
            canvas.drawText("Adjust LAMP & TEXT SIZE in panel below", width / 2f, height * 0.34f, hudPaint)
            canvas.drawText("Settings → launch picker / units", width / 2f, height * 0.39f, hudPaint)
        }



        // Touch targets for time-jump chips on CDT page
        private val jumpChipRects = Array(6) { RectF() }
        // T-30 so you can reach TEL before liftoff; T+0 for historical live watch
        private val jumpChipLabels = arrayOf("T-60", "T-30", "T-10", "T+0", "MECO", "SCRUB")
        private val jumpChipValues = floatArrayOf(-60f, -30f, -10f, 0f, 150f, -999f)


        private fun hudFailed(launch: com.ccos.retro.data.LaunchSnapshot?, tSec: Float): Boolean =
            FlightEventCatalog.failureFromStatus(launch, tSec) != null

        private fun drawTelCountdown(canvas: Canvas, launch: com.ccos.retro.data.LaunchSnapshot?, skin: TelemetrySkin.Tokens, ts: Float, now: Long) {
            val lamp = prefs.lampBrightness
            if (launch == null) {
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = 22f * ts
                hudPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("NO LAUNCH DATA", width / 2f, height * 0.4f, hudPaint)
                return
            }
            // Mission block sits high under the top HUD — above the logo zone
            // so names never write through the agency mark
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = (16f * ts).coerceIn(14f, 28f)
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(launch.name.take(36), width / 2f, height * 0.22f, hudPaint)

            val statusColor = when {
                telemetryModule.forceStatus != null -> skin.hold
                launch.statusAbbrev.equals("Go", true) || launch.statusName.contains("Go", true) -> skin.go
                launch.statusName.contains("Hold", true) || launch.statusName.contains("TBC", true) -> skin.hold
                launch.statusName.contains("Flight", true) -> skin.accent
                else -> skin.muted
            }

            val statusLabel = telemetryModule.forceStatus ?: launch.statusAbbrev
            fillPaint.color = Color.argb(60, Color.red(statusColor), Color.green(statusColor), Color.blue(statusColor))
            val pillW = (160f * ts).coerceIn(140f, 240f)
            val pillH = (32f * ts).coerceIn(28f, 48f)
            val px = width / 2f - pillW / 2f
            val py = height * 0.25f
            canvas.drawRoundRect(px, py, px + pillW, py + pillH, 8f, 8f, fillPaint)
            strokePaint.color = statusColor
            strokePaint.strokeWidth = 2f
            canvas.drawRoundRect(px, py, px + pillW, py + pillH, 8f, 8f, strokePaint)
            hudPaint.color = statusColor
            hudPaint.textSize = (14f * ts).coerceIn(12f, 22f)
            canvas.drawText(statusLabel.uppercase(), width / 2f, py + pillH * 0.68f, hudPaint)

            hudPaint.color = withLamp(skin.muted, lamp)
            hudPaint.textSize = (12f * ts).coerceIn(11f, 18f)
            canvas.drawText("${launch.pad} · ${launch.location}".take(42), width / 2f, height * 0.32f, hudPaint)


            // Time-jump chips — scale with text slider, clear of bottom nav
            val chipScale = ts.coerceIn(0.9f, 1.6f)
            hudPaint.color = withLamp(skin.accent, lamp)
            hudPaint.textSize = 16f * chipScale
            canvas.drawText("JUMP TO EVENT", width / 2f, height * 0.50f, hudPaint)
            val chipW = width * 0.30f
            val chipH = 52f * chipScale
            val gap = 12f * chipScale
            val startY = height * 0.54f
            for (i in 0 until 6) {
                val col = i % 3
                val row = i / 3
                val left = width * 0.05f + col * (chipW + gap)
                val top = startY + row * (chipH + gap)
                jumpChipRects[i].set(left, top, left + chipW, top + chipH)
                fillPaint.color = withLamp(skin.btnIdleFill, lamp)
                canvas.drawRoundRect(jumpChipRects[i], 12f, 12f, fillPaint)
                strokePaint.color = withLamp(skin.accent, lamp)
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(jumpChipRects[i], 12f, 12f, strokePaint)
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = 17f * chipScale
                canvas.drawText(jumpChipLabels[i], jumpChipRects[i].centerX(), jumpChipRects[i].centerY() + 6f * chipScale, hudPaint)
            }
            hudPaint.color = withLamp(skin.muted, lamp)
            hudPaint.textSize = 12f * chipScale
            canvas.drawText("Sim loops for wallpaper theater", width / 2f, height * 0.80f, hudPaint)
        }




        private fun drawTelMetrics(canvas: Canvas, launch: com.ccos.retro.data.LaunchSnapshot?, skin: TelemetrySkin.Tokens, ts: Float, now: Long) {
            val lamp = prefs.lampBrightness
            telBold()
            if (launch == null) {
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = telFit("NO LAUNCH TRACKED", width * 0.7f, height * 0.05f, 20f)
                hudPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("NO LAUNCH TRACKED", width / 2f, height * 0.45f, hudPaint)
                if (eventSkipHit.width() > 4f) drawEventSkipRocker(canvas, eventSkipHit, skin, lamp)
                if (lockHit.width() > 4f) drawLockChip(canvas, lockHit, skin, lamp)
                if (telStripAnalog.width() > 4f) drawAnalogDigitalRocker(canvas, telStripAnalog, skin, lamp)
                return
            }

            val t = telemetryModule.effectiveSecondsFromNet(now)
            val (altKm, speedKmh, phase) = approximateProfile(t, launch, hudStage(launch, t))
            val pitchDeg = approximatePitch(t)
            val targetZoom = trackingZoom(t, altKm)
            trackZoomSmoothed += (targetZoom - trackZoomSmoothed) * 0.07f

            val analog = prefs.telemetryAnalog
            val gapL = laneLeft()
            val gapR = laneRight()
            val pad = su(0.008f).coerceIn(4f, 10f)
            val dock = min(height * 0.838f, dockFloor())
            val packTop = max(belowButtons(), telHudBottom + pad)
            val packH = (dock - packTop).coerceAtLeast(80f)
            // Intrinsic rows: readout and tape take what they need. Leftover is the flex gap.
            val readH = min(telSp(26f), packH * 0.10f).coerceAtLeast(20f)
            val timeH = min(telSp(34f), packH * 0.12f).coerceAtLeast(24f)
            val readBot = dock
            val readTop = readBot - readH
            val timeBot = readTop - pad
            val timeTop = timeBot - timeH
            val flexTop = packTop + pad
            val flexBot = timeTop - pad
            val flexH = (flexBot - flexTop).coerceAtLeast(40f)

            val mapLeft: Float
            val mapRight: Float
            val mapTop: Float
            val mapBot: Float
            val mapR: Float
            val cx: Float
            val cy: Float
            val gaugeTop: Float
            val gaugeBot: Float
            if (analog) {
                mapLeft = gapL
                mapRight = gapR
                mapTop = telHudBottom + pad
                mapBot = buttonRects[3].bottom.coerceAtLeast(mapTop + 28f)
                val stampH = (mapBot - mapTop).coerceAtLeast(28f)
                cx = (mapLeft + mapRight) * 0.50f
                cy = (mapTop + mapBot) * 0.50f
                mapR = min((mapRight - mapLeft) * 0.40f, stampH * 0.40f)
                val stackNeed = min(max(flexH * 0.32f, telSp(88f)), flexH * 0.42f)
                gaugeTop = flexTop + stackNeed + pad
                gaugeBot = flexBot
            } else {
                val gaugeNeed = min(telSp(64f), flexH * 0.20f).coerceAtLeast(36f)
                gaugeBot = flexBot
                gaugeTop = gaugeBot - gaugeNeed
                mapLeft = fullLeft()
                mapRight = fullRight()
                mapTop = flexTop
                mapBot = gaugeTop - pad
                cx = width / 2f
                cy = (mapTop + mapBot) * 0.50f
                mapR = min(width * 0.44f, (mapBot - mapTop) * 0.46f)
            }

            canvas.save()
            canvas.clipRect(mapLeft, mapTop, mapRight, mapBot)
            drawAgencyOrbitView(
                canvas, cx, cy, mapR,
                t, altKm, speedKmh, pitchDeg, launch, skin, lamp
            )
            canvas.restore()

            resetHudPaints()
            drawSideStageRockets(canvas, launch, t, skin, lamp, if (analog) gaugeTop else mapBot)
            layoutActionChipsBesideStacks()
            val gL = fullLeft()
            val gR = fullRight()
            drawFlightGauges(canvas, launch, t, altKm, speedKmh, skin, lamp, gaugeTop, gaugeBot, gL, gR)
            if (eventSkipHit.width() > 4f) drawEventSkipRocker(canvas, eventSkipHit, skin, lamp)
            if (lockHit.width() > 4f) drawLockChip(canvas, lockHit, skin, lamp)
            if (telStripAnalog.width() > 4f) drawAnalogDigitalRocker(canvas, telStripAnalog, skin, lamp)
            drawEventTimeline(canvas, t, launch, skin, lamp, timeTop, timeBot)
            drawTelReadoutRow(canvas, launch, t, altKm, speedKmh, phase, skin, lamp, readTop, readBot)
        }

        private fun drawTelEventClockStrip(
            canvas: Canvas,
            launch: com.ccos.retro.data.LaunchSnapshot?,
            tSec: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            top: Float,
            bot: Float
        ) {
            val left = fullLeft()
            val right = fullRight()
            val h = (bot - top).coerceAtLeast(24f)
            fillPaint.shader = null
            fillPaint.color = withLamp(Color.parseColor("#141A22"), lamp)
            canvas.drawRoundRect(left, top, right, bot, 6f, 6f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.4f
            strokePaint.color = withLamp(skin.accent, lamp * 0.55f)
            canvas.drawRoundRect(left, top, right, bot, 6f, 6f, strokePaint)
            val (last, next) = EventClock.lastNext(launch, tSec)
            val on = when {
                last != null && abs(last.tSec - tSec) <= 0.5f -> last
                next != null && abs(next.tSec - tSec) <= 0.5f -> next
                else -> null
            }
            val clock = EventClock.fmt(tSec)
            val lastStr = last?.let { "LAST  ${it.title.take(14)}  ${EventClock.fmt(it.tSec)}" } ?: "LAST  —"
            val nowStr = if (on != null) "NOW  ${on.title.take(14)}  $clock" else "NOW  $clock"
            val nextStr = next?.let { "NEXT  ${it.title.take(14)}" } ?: "NEXT  —"
            val inStr = if (next != null) EventClock.remain(tSec, next.tSec) else ""
            telBold()
            val row1 = top + h * 0.42f
            val row2 = top + h * 0.86f
            hudPaint.textAlign = Paint.Align.LEFT
            hudPaint.color = withLamp(skin.hold, lamp)
            hudPaint.textSize = telFit(lastStr, (right - left) * 0.52f, h * 0.38f, 13f)
            canvas.drawText(lastStr, left + 10f, row1, hudPaint)
            hudPaint.textAlign = Paint.Align.RIGHT
            hudPaint.color = withLamp(if (on != null) skin.hold else if (tSec >= 0f) skin.go else skin.hold, lamp)
            hudPaint.textSize = telFit(nowStr, (right - left) * 0.44f, h * 0.40f, 14f)
            canvas.drawText(nowStr, right - 10f, row1, hudPaint)
            hudPaint.textAlign = Paint.Align.LEFT
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = telFit(nextStr, (right - left) * 0.62f, h * 0.38f, 13f)
            canvas.drawText(nextStr, left + 10f, row2, hudPaint)
            if (inStr.isNotEmpty()) {
                hudPaint.textAlign = Paint.Align.RIGHT
                hudPaint.color = withLamp(skin.go, lamp)
                hudPaint.textSize = telFit(inStr, (right - left) * 0.28f, h * 0.38f, 14f)
                canvas.drawText(inStr, right - 10f, row2, hudPaint)
            }
            hudPaint.textAlign = Paint.Align.CENTER
        }

        private fun drawTelReadoutRow(
            canvas: Canvas,
            launch: com.ccos.retro.data.LaunchSnapshot,
            tSec: Float,
            altKm: Float,
            speedKmh: Float,
            phase: String,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            top: Float,
            bot: Float
        ) {
            telBold()
            val imperial = prefs.useImperial
            val altStr = if (imperial) String.format("%.1f mi", altKm * 0.621371f)
            else String.format("%.1f km", altKm)
            val spdVal = if (imperial) speedKmh * 0.621371f else speedKmh
            val spdStr = String.format("%,.0f", spdVal)
            val padLL = PadBook.lonLat(launch)
            val downStr = if (padLL == null) {
                "—"
            } else {
                val (padLon, padLat) = padLL
                val az = com.ccos.retro.event.FlightProfiles.padAzimuth(launch)
                val west = az > 180f
                val stage = hudStage(launch, tSec)
                val (vehLon, vehLat) = com.ccos.retro.event.FlightProfiles.vehicleLonLat(
                    launch, tSec, stage, padLon, padLat, az, west
                )
                val downKm = com.ccos.retro.event.FlightProfiles.rangeKm(padLon, padLat, vehLon, vehLat)
                if (imperial) String.format("%.0f mi", downKm * 0.621371f)
                else String.format("%.0f km", downKm)
            }
            val h = (bot - top).coerceAtLeast(20f)
            val labH = h * 0.36f
            val valH = h * 0.50f
            val scale01 = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            val cols = arrayOf(
                Triple("ALT", altStr, skin.accent),
                Triple(if (imperial) "MPH" else "KM/H", spdStr, skin.hold),
                Triple("RANGE", downStr, skin.text)
            )
            val left = fullLeft()
            val right = fullRight()
            val cellW = (right - left) / 3f
            hudPaint.textAlign = Paint.Align.CENTER
            cols.forEachIndexed { i, (lab, value, col) ->
                val x = left + cellW * (i + 0.5f)
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = telFit(lab, cellW * 0.96f, labH, 11f)
                canvas.drawText(lab, x, top + labH, hudPaint)
                hudPaint.color = withLamp(col, lamp)
                hudPaint.textSize = telFit(value, cellW * 0.96f, valH, 16f)
                canvas.drawText(value, x, top + labH + valH, hudPaint)
            }
        }

        private fun trackingZoom(tSec: Float, altKm: Float): Float {
            val fromTime = when {
                tSec < -20f -> 0f
                tSec < 0f -> ((tSec + 20f) / 20f) * 0.08f
                tSec < 28f -> 0.08f + (tSec / 28f) * 0.30f
                tSec < 90f -> 0.38f + ((tSec - 28f) / 62f) * 0.32f
                tSec < 220f -> 0.70f + ((tSec - 90f) / 130f) * 0.30f
                else -> 1f
            }
            val fromAlt = (altKm / 200f).coerceIn(0f, 1f)
            return (fromTime * 0.72f + fromAlt * 0.28f).coerceIn(0f, 1f)
        }

        private fun currentMissionEvent(tSec: Float, launch: com.ccos.retro.data.LaunchSnapshot): String {
            val ev = missionEvents(launch)
            val passed = ev.lastOrNull { tSec >= it.first }
            return passed?.second ?: "HOLDING / T-COUNT"
        }

        private fun missionEvents(launch: com.ccos.retro.data.LaunchSnapshot): List<Pair<Float, String>> =
            com.ccos.retro.event.FlightProfiles.events(launch)

        private fun drawEventTimeline(
            canvas: Canvas,
            tSec: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            top: Float,
            bot: Float
        ) {
            telBold()
            val events = missionEvents(launch)
            val nearest = events.minByOrNull { abs(it.first - tSec) } ?: return
            val close = abs(nearest.first - tSec) < 32f && tSec > -8f
            val lastT = events.last().first
            val win0 = if (close) nearest.first - 48f else 0f
            val win1 = if (close) nearest.first + 48f else lastT
            val span = (win1 - win0).coerceAtLeast(30f)
            val left = fullLeft()
            val right = fullRight()
            val h = (bot - top).coerceAtLeast(24f)
            val failed = hudFailed(launch, tSec)
            val title = when {
                failed -> "FAIL"
                close -> nearest.second
                else -> currentMissionEvent(tSec, launch)
            }
            val titleH = h * 0.38f
            val lineY = top + titleH + h * 0.18f
            val labelH = (bot - lineY - 4f).coerceAtLeast(14f)

            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(when {
                failed -> skin.danger
                close -> skin.hold
                else -> skin.text
            }, lamp)
            val scale01 = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            hudPaint.textSize = telFit(title, (right - left) * 0.96f, titleH, 18f + scale01 * 28f)
            canvas.drawText(title, width / 2f, top + titleH, hudPaint)

            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 6f
            strokePaint.color = withLamp(skin.muted, lamp)
            canvas.drawLine(left, lineY, right, lineY, strokePaint)
            val nowX = left + ((tSec - win0).coerceIn(0f, span) / span) * (right - left)
            strokePaint.color = withLamp(skin.go, lamp)
            canvas.drawLine(left, lineY, nowX, lineY, strokePaint)
            fillPaint.color = withLamp(skin.go, lamp)
            canvas.drawCircle(nowX, lineY, 8f, fillPaint)

            val inWindow = events.filter { it.first in (win0 - 4f)..(win1 + 4f) }
            val visible = if (close) inWindow else {
                val cur = inWindow.minByOrNull { abs(it.first - tSec) }
                listOfNotNull(inWindow.firstOrNull(), cur, inWindow.getOrNull(inWindow.size / 2), inWindow.lastOrNull())
                    .distinct()
            }
            val minGap = (right - left) * 0.22f
            val labeled = mutableListOf<Pair<Float, String>>()
            val mark = canvas.save()
            canvas.clipRect(left, top, right, bot)
            for ((et, label) in visible) {
                val x = (left + ((et - win0) / span) * (right - left)).coerceIn(left + 8f, right - 8f)
                val done = tSec >= et
                fillPaint.color = withLamp(if (done) skin.go else skin.accent, lamp)
                canvas.drawCircle(x, lineY, 7f, fillPaint)
                if (label == title) continue
                val clash = labeled.any { abs(it.first - x) < minGap }
                if (clash) continue
                labeled.add(x to label)
            }
            val slotW = ((right - left) / max(3, labeled.size + 1)).coerceAtLeast(36f)
            for ((x, label) in labeled) {
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = telFit(label, slotW, labelH, 16f)
                val tx = x.coerceIn(left + slotW * 0.5f, right - slotW * 0.5f)
                canvas.drawText(label, tx, lineY + labelH * 0.85f, hudPaint)
            }
            canvas.restoreToCount(mark)
        }

        private fun drawAgencyOrbitView(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSec: Float,
            altKm: Float,
            speedKmh: Float,
            pitchDeg: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            drawUnifiedGroundTrack(canvas, cx, cy, radius, tSec, altKm, launch, skin, lamp)
        }

        private fun drawUnifiedGroundTrack(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSec: Float,
            altKm: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val left = cx - radius * 1.28f
            val top = cy - radius * 0.82f
            val right = cx + radius * 1.28f
            val bot = cy + radius * 0.82f
            val destW = (right - left).coerceAtLeast(8f)
            val destH = (bot - top).coerceAtLeast(8f)
            canvas.save()
            canvas.clipRect(left, top, right, bot)
            fillPaint.color = withLamp(Color.parseColor("#06101C"), lamp)
            canvas.drawRect(left, top, right, bot, fillPaint)
            PadBook.ensure(this@RetroCommandWallpaperService)
            GeoAtlas.ensure(this@RetroCommandWallpaperService)
            val (padLon, padLat) = PadBook.lonLat(launch) ?: (0f to 0f)
            val site = PadBook.find(launch)
            val az = when {
                site == null -> 90f
                site.sea && site.waterAz < 360f -> site.waterAz
                !site.inland && site.waterAz <= 360f -> site.waterAz
                else -> 90f
            }
            val west = az > 180f
            val stage = hudStage(launch, tSec)
            val (vehLon, vehLat) = com.ccos.retro.event.FlightProfiles.vehicleLonLat(
                launch, tSec, stage, padLon, padLat, az, west
            )
            val cam = com.ccos.retro.event.FlightProfiles.trajCam(
                padLon, padLat, vehLon, vehLat, altKm, tSec
            )
            val cLon = cam.cLon
            val cLat = cam.cLat
            val hLon = cam.halfLon
            val hLat = cam.halfLat
            val dest = RectF(left, top, right, bot)
            fillPaint.color = withLamp(Color.parseColor("#0A3A58"), lamp)
            canvas.drawRect(dest, fillPaint)
            requestTrajLand(destW.toInt(), destH.toInt(), cLon, cLat, hLon, hLat)
            val baked = trajLandBmp
            if (baked != null && !baked.isRecycled) {
                canvas.drawBitmap(baked, null, dest, null)
            }
            fun xy(lon: Float, lat: Float): Pair<Float, Float> {
                return GeoDraw.mapXY(lon, lat, dest, cLon, cLat, hLon, hLat)
            }
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.0f
            strokePaint.color = withLamp(Color.parseColor("#2A5A70"), lamp)
            for (i in 1..6) {
                val y = top + destH * i / 7f
                canvas.drawLine(left, y, right, y, strokePaint)
            }
            val (px, py) = xy(padLon, padLat)
            fillPaint.color = withLamp(Color.parseColor("#F2C14E"), lamp)
            val ps = min(destW, destH) * 0.018f
            canvas.drawRect(px - ps, py - ps, px + ps, py + ps, fillPaint)
            val endT = com.ccos.retro.event.FlightProfiles.replayEndSec(launch)
            val nowT = tSec.coerceAtLeast(0f)
            val track = Path()
            val samples = 32
            for (i in 0..samples) {
                val tt = nowT * i / samples
                val (lon, lat) = com.ccos.retro.event.FlightProfiles.vehicleLonLat(
                    launch, tt, stage, padLon, padLat, az, west
                )
                val (x, y) = xy(lon, lat)
                if (i == 0) track.moveTo(x, y) else track.lineTo(x, y)
            }
            strokePaint.strokeWidth = 3.0f
            strokePaint.color = withLamp(Color.parseColor("#00D4FF"), lamp)
            canvas.drawPath(track, strokePaint)
            if (nowT < endT) {
                val future = Path()
                for (i in 0..12) {
                    val tt = nowT + (endT - nowT) * i / 12f
                    val (lon, lat) = com.ccos.retro.event.FlightProfiles.vehicleLonLat(
                        launch, tt, stage, padLon, padLat, az, west
                    )
                    val (x, y) = xy(lon, lat)
                    if (i == 0) future.moveTo(x, y) else future.lineTo(x, y)
                }
                strokePaint.strokeWidth = 1.6f
                strokePaint.color = withLamp(Color.parseColor("#3A6A80"), lamp)
                canvas.drawPath(future, strokePaint)
            }
            val (vx, vy) = xy(vehLon, vehLat)
            fillPaint.color = withLamp(Color.parseColor("#3D9BFF"), lamp)
            canvas.drawCircle(vx, vy, 7f, fillPaint)
            canvas.restore()
            strokePaint.strokeWidth = 2.6f
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawRect(left, top, right, bot, strokePaint)
        }

        private fun drawNasaPetalMap(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSec: Float,
            altKm: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val left = cx - radius * 1.35f
            val top = cy - radius * 0.78f
            val right = cx + radius * 1.35f
            val bot = cy + radius * 0.78f
            val w = (right - left).coerceAtLeast(8f)
            val h = (bot - top).coerceAtLeast(8f)
            canvas.save()
            canvas.clipRect(left, top, right, bot)

            fillPaint.color = withLamp(Color.parseColor("#071428"), lamp)
            canvas.drawRect(left, top, right, bot, fillPaint)

            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.05f
            strokePaint.color = withLamp(Color.parseColor("#3A88A8"), lamp)
            for (i in 1..5) {
                val y = top + h * i / 6f
                canvas.drawLine(left, y, right, y, strokePaint)
            }
            for (i in 1..7) {
                val x = left + w * i / 8f
                canvas.drawLine(x, top, x, bot, strokePaint)
            }

            val land = withLamp(Color.parseColor("#2E5A3C"), lamp)
            val landEdge = withLamp(Color.parseColor("#4A7A58"), lamp)
            fun blob(l: Float, t: Float, r: Float, b: Float) {
                fillPaint.color = land
                canvas.drawOval(l, t, r, b, fillPaint)
                strokePaint.strokeWidth = 1.4f
                strokePaint.color = landEdge
                canvas.drawOval(l, t, r, b, strokePaint)
            }
            // N America left-of-center
            blob(cx - w * 0.42f, cy - h * 0.34f, cx - w * 0.02f, cy + h * 0.06f)
            blob(cx - w * 0.38f, cy - h * 0.22f, cx - w * 0.14f, cy + h * 0.02f)
            // S America
            blob(cx - w * 0.22f, cy + h * 0.04f, cx - w * 0.04f, cy + h * 0.42f)
            // Europe / Africa
            blob(cx + w * 0.00f, cy - h * 0.30f, cx + w * 0.18f, cy - h * 0.06f)
            blob(cx + w * 0.02f, cy - h * 0.08f, cx + w * 0.22f, cy + h * 0.34f)
            // Asia strip
            blob(cx + w * 0.16f, cy - h * 0.28f, cx + w * 0.52f, cy + h * 0.04f)
            blob(cx + w * 0.28f, cy - h * 0.08f, cx + w * 0.48f, cy + h * 0.18f)

            val loc = "${launch.pad} ${launch.location}".lowercase()
            val florida = "kennedy" in loc || "cape" in loc || "39" in loc || "florida" in loc
            val padX = if (florida) cx - w * 0.08f else cx - w * 0.18f
            val padY = if (florida) cy + h * 0.02f else cy - h * 0.01f
            val padS = min(w, h) * 0.016f
            fillPaint.color = withLamp(Color.parseColor("#F2C14E"), lamp)
            canvas.drawRect(padX - padS, padY - padS, padX + padS, padY + padS, fillPaint)

            val track = Path()
            val prog = ((tSec + 10f) / 540f).coerceIn(0.02f, 1f)
            for (i in 0..48) {
                val u = i / 48f
                val x = padX + w * 0.58f * u
                val y = padY - h * 0.30f * u - h * 0.07f * sin(u * Math.PI).toFloat()
                if (i == 0) track.moveTo(x, y) else track.lineTo(x, y)
            }
            strokePaint.strokeWidth = 3.4f
            strokePaint.color = withLamp(Color.parseColor("#00D4FF"), lamp)
            canvas.drawPath(track, strokePaint)
            val vx = padX + w * 0.58f * prog
            val vy = padY - h * 0.30f * prog - h * 0.07f * sin(prog * Math.PI).toFloat()
            drawVesselChevron(canvas, vx, vy, 0.55f, skin, lamp)

            val labelH = (h * 0.10f).coerceIn(10f, 15f)
            val labelY = top + labelH + 3f
            val padName = launch.pad.ifBlank { "LC-39B" }.take(16)
            telBold()
            hudPaint.color = withLamp(Color.parseColor("#F2C14E"), lamp)
            hudPaint.textAlign = Paint.Align.LEFT
            hudPaint.textSize = telFit(padName, w * 0.28f, labelH, 11f)
            canvas.drawText(padName, left + 8f, labelY, hudPaint)
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.textSize = telFit("GROUND TRACK", w * 0.34f, labelH, 11f)
            canvas.drawText("GROUND TRACK", cx, labelY, hudPaint)
            hudPaint.textAlign = Paint.Align.RIGHT
            hudPaint.textSize = telFit("DOWNRANGE", w * 0.28f, labelH, 11f)
            canvas.drawText("DOWNRANGE", right - 8f, labelY, hudPaint)

            canvas.restore()
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3.2f
            strokePaint.color = withLamp(Color.parseColor("#F2C14E"), lamp)
            canvas.drawRect(left, top, right, bot, strokePaint)
            telBold()
            hudPaint.textAlign = Paint.Align.LEFT
            hudPaint.color = withLamp(Color.parseColor("#F2C14E"), lamp)
            hudPaint.textSize = telFit("NASA", w * 0.10f, h * 0.07f, 8f)
            canvas.drawText("NASA", left + 6f, bot - 5f, hudPaint)
        }

        private fun drawSpacexDarkGlobe(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSec: Float,
            altKm: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            fillPaint.color = Color.argb(255, 4, 6, 10)
            canvas.drawCircle(cx, cy, radius, fillPaint)
            fillPaint.color = Color.argb(255, 8, 14, 22)
            canvas.drawCircle(cx, cy, radius * 0.96f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 10f
            strokePaint.color = Color.argb(160, 40, 90, 140)
            canvas.drawCircle(cx, cy, radius * 0.99f, strokePaint)
            strokePaint.strokeWidth = 4f
            strokePaint.color = Color.argb(90, 180, 200, 220)
            canvas.drawCircle(cx, cy, radius * 0.70f, strokePaint)
            canvas.drawCircle(cx, cy, radius * 0.40f, strokePaint)

            val land = Color.argb(230, 28, 32, 36)
            val outline = Color.argb(230, 210, 215, 220)
            fun blob(ox: Float, oy: Float, rx: Float, ry: Float) {
                fillPaint.color = land
                canvas.drawOval(cx + ox - rx, cy + oy - ry, cx + ox + rx, cy + oy + ry, fillPaint)
                strokePaint.strokeWidth = 3.5f
                strokePaint.color = outline
                canvas.drawOval(cx + ox - rx, cy + oy - ry, cx + ox + rx, cy + oy + ry, strokePaint)
            }
            blob(-radius * 0.18f, -radius * 0.06f, radius * 0.34f, radius * 0.22f)
            blob(-radius * 0.02f, radius * 0.16f, radius * 0.16f, radius * 0.20f)
            blob(radius * 0.28f, -radius * 0.08f, radius * 0.18f, radius * 0.12f)
            telBold()
            hudPaint.color = Color.argb(200, 220, 220, 220)
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.textSize = telFit("FLORIDA", radius * 0.55f, radius * 0.12f, 13f)
            canvas.drawText("FLORIDA", cx - radius * 0.08f, cy + radius * 0.02f, hudPaint)
            hudPaint.textSize = telFit("MEXICO", radius * 0.45f, radius * 0.12f, 13f)
            canvas.drawText("MEXICO", cx - radius * 0.02f, cy + radius * 0.28f, hudPaint)

            val west = "vandenberg" in launch.location.lowercase()
            val padX = cx + if (west) -radius * 0.22f else radius * 0.02f
            val padY = cy + radius * 0.10f
            val flown = Path()
            val predict = Path()
            val steps = 48
            val prog = ((tSec + 4f) / 540f).coerceIn(0.02f, 1f)
            for (i in 0..steps) {
                val u = i / steps.toFloat()
                val x = padX + (if (west) -1f else 1f) * radius * 0.85f * u
                val y = padY - radius * 0.55f * sin((u * Math.PI * 0.85).toFloat())
                if (u <= prog) {
                    if (i == 0) flown.moveTo(x, y) else flown.lineTo(x, y)
                } else {
                    if (predict.isEmpty) predict.moveTo(x, y) else predict.lineTo(x, y)
                }
            }
            strokePaint.strokeWidth = 9f
            strokePaint.color = Color.parseColor("#FF6A1A")
            canvas.drawPath(flown, strokePaint)
            strokePaint.strokeWidth = 6f
            strokePaint.color = Color.parseColor("#8A9098")
            strokePaint.pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
            canvas.drawPath(predict, strokePaint)
            strokePaint.pathEffect = null
            val vx = padX + (if (west) -1f else 1f) * radius * 0.85f * prog
            val vy = padY - radius * 0.55f * sin((prog * Math.PI * 0.85).toFloat())
            fillPaint.color = Color.parseColor("#3D9BFF")
            canvas.drawCircle(vx, vy, 12f, fillPaint)
            fillPaint.color = Color.WHITE
            canvas.drawCircle(vx, vy, 5f, fillPaint)
            hudPaint.color = Color.parseColor("#FF8A3A")
            val cap = if (altKm < 120f) "ASCENT" else "ORBIT"
            hudPaint.textSize = telFit(cap, radius * 0.7f, radius * 0.14f, 14f)
            canvas.drawText(cap, cx, cy + radius * 0.82f, hudPaint)
        }

        private fun drawThemedGlobe(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSec: Float,
            altKm: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val ocean = when (skin.mapStyle) {
                TelemetrySkin.MapStyle.CASC -> Color.parseColor("#1A1010")
                TelemetrySkin.MapStyle.ROSCOSMOS -> Color.parseColor("#101820")
                TelemetrySkin.MapStyle.ESA -> Color.parseColor("#0A1430")
                TelemetrySkin.MapStyle.ISRO -> Color.parseColor("#14100A")
                TelemetrySkin.MapStyle.JAXA -> Color.parseColor("#180C0C")
                TelemetrySkin.MapStyle.BLUE -> Color.parseColor("#061828")
                else -> Color.parseColor("#0A1828")
            }
            fillPaint.color = ocean
            canvas.drawCircle(cx, cy, radius, fillPaint)
            fillPaint.color = Color.argb(160, Color.red(skin.accent), Color.green(skin.accent), Color.blue(skin.accent))
            canvas.drawCircle(cx - radius * 0.16f, cy - radius * 0.08f, radius * 0.28f, fillPaint)
            canvas.drawCircle(cx + radius * 0.18f, cy + radius * 0.10f, radius * 0.16f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 8f
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawCircle(cx, cy, radius, strokePaint)
            strokePaint.strokeWidth = 3.5f
            strokePaint.color = withLamp(skin.accentDim, lamp)
            canvas.drawCircle(cx, cy, radius * 0.66f, strokePaint)
            canvas.drawCircle(cx, cy, radius * 0.33f, strokePaint)
            val track = Path()
            val prog = ((tSec + 4f) / 540f).coerceIn(0.02f, 1f)
            for (i in 0..40) {
                val u = i / 40f
                val ang = Math.toRadians(-30.0 + u * 200.0)
                val rr = radius * (0.55f + 0.12f * (altKm / 200f).coerceIn(0f, 1f))
                val x = cx + (rr * cos(ang)).toFloat()
                val y = cy + (rr * 0.62f * sin(ang)).toFloat()
                if (i == 0) track.moveTo(x, y) else track.lineTo(x, y)
            }
            strokePaint.strokeWidth = 8f
            strokePaint.color = withLamp(skin.hold, lamp)
            canvas.drawPath(track, strokePaint)
            val ang = Math.toRadians(-30.0 + prog * 200.0)
            val rr = radius * (0.55f + 0.12f * (altKm / 200f).coerceIn(0f, 1f))
            drawVesselChevron(
                canvas, cx + (rr * cos(ang)).toFloat(), cy + (rr * 0.62f * sin(ang)).toFloat(),
                prog, skin, lamp
            )
            telBold()
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = telFit(skin.label, radius * 1.2f, radius * 0.16f, 14f)
            canvas.drawText(skin.label, cx, cy + radius * 0.18f, hudPaint)
        }

        private fun drawFlightGauges(
            canvas: Canvas,
            launch: com.ccos.retro.data.LaunchSnapshot,
            tSec: Float,
            altKm: Float,
            speedKmh: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            top: Float,
            bot: Float,
            left: Float,
            right: Float
        ) {
            telBold()
            val stg = hudStage(launch, tSec)
            val engines = engineCountForStage(launch, stg)
            val sep = sepTime(launch)
            val lit = com.ccos.retro.event.FlightProfiles.enginesLit(
                tSec, launch, stg, engines
            )
            val accel = com.ccos.retro.event.FlightProfiles.accelG(tSec, launch)
            val analog = prefs.telemetryAnalog
            val scale01 = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            val imperial = prefs.useImperial
            val altVal = if (imperial) altKm * 0.621371f else altKm
            val spdVal = if (imperial) speedKmh * 0.621371f else speedKmh
            val fuel = fuelRemain(tSec, stg, launch)
            val altStr = String.format("%.1f", altVal)
            val spdStr = String.format("%,.0f", spdVal)
            val gStr = String.format("%.2fg", accel)
            val engStr = if (analog) "$lit/$engines" else "$lit/$engines LIT"
            val fuelStr = String.format("%.0f%%", fuel * 100f)
            val live = tSec >= 0f
            val items = arrayOf(
                Triple("ALT EST", altStr, (altKm / 200f).coerceIn(0f, 1f)),
                Triple("SPD EST", spdStr, (speedKmh / 28000f).coerceIn(0f, 1f)),
                Triple("ACCEL", gStr, (accel / 4f).coerceIn(0f, 1f)),
                Triple("ENG", engStr, (lit.toFloat() / engines.coerceAtLeast(1)).coerceIn(0f, 1f))
            )
            val cellW = (right - left) / items.size
            val gH = (bot - top).coerceAtLeast(24f)
            val gaugeMark = canvas.save()
            try {
            if (right > left && bot > top) canvas.clipRect(left, top, right, bot)

            val failed = hudFailed(launch, tSec)
            if (!analog) {
                val labH = gH * 0.36f
                val valH = gH * 0.52f
                items.forEachIndexed { i, (lab, value, _) ->
                    val x = left + cellW * (i + 0.5f)
                    telBold()
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = withLamp(
                        when {
                            lab == "ENG" && failed -> skin.danger
                            live || lab == "ACCEL" -> skin.text
                            else -> skin.muted
                        }, lamp
                    )
                    hudPaint.textSize = telFit(value, cellW * 0.94f, valH, 18f + scale01 * 32f)
                    canvas.drawText(value, x, top + valH, hudPaint)
                    hudPaint.color = withLamp(skin.muted, lamp)
                    hudPaint.textSize = telFit(lab, cellW * 0.94f, labH, 13f + scale01 * 18f)
                    canvas.drawText(lab, x, bot - 2f, hudPaint)
                }
                return
            }

            // Phone density used to eat the whole can (caption mins > gH). Face first.
            val topTextH = min(gH * 0.16f, telSp(14f)).coerceAtLeast(min(gH * 0.12f, 11f))
            val rangeH = min(gH * 0.18f, telSp(18f)).coerceAtLeast(min(gH * 0.10f, 10f))
            val botTextH = min(gH * 0.12f, telSp(12f)).coerceAtLeast(min(gH * 0.08f, 8f))
            var faceTop = top + topTextH
            var faceBot = bot - rangeH - botTextH
            if (faceBot - faceTop < gH * 0.34f) {
                faceTop = top + gH * 0.14f
                faceBot = bot - gH * 0.24f
            }
            items.forEachIndexed { i, (lab, value, frac) ->
                val x = left + cellW * (i + 0.5f)
                val can = RectF(
                    left + cellW * i + 3f, faceTop,
                    left + cellW * (i + 1) - 3f, faceBot
                )
                if (can.height() < 28f || can.width() < 28f) {
                    telBold()
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = withLamp(skin.text, lamp)
                    hudPaint.textSize = telFit(value, cellW * 0.94f, gH * 0.55f, 16f)
                    canvas.drawText(value, x, top + gH * 0.55f, hudPaint)
                    hudPaint.color = withLamp(skin.muted, lamp)
                    hudPaint.textSize = telFit(lab, cellW * 0.94f, gH * 0.22f, 11f)
                    canvas.drawText(lab, x, bot - 2f, hudPaint)
                    return@forEachIndexed
                }
                telBold()
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(if (live || lab == "ACCEL") skin.text else skin.muted, lamp)
                hudPaint.textSize = telFit(value, cellW * 0.94f, topTextH * 0.95f, 16f + scale01 * 28f)
                canvas.drawText(value, x, top + topTextH * 0.88f, hudPaint)
                try {
                drawInstrumentCan(canvas, can, skin, lamp)
                val r = min(can.width(), can.height()) * 0.46f
                when {
                    lab.startsWith("ALT") -> drawAltitudeTape(canvas, can, altVal, imperial, skin, lamp, live)
                    lab.startsWith("SPD") -> drawSpeedTape(canvas, can, spdVal, imperial, skin, lamp, live)
                    lab.startsWith("ACCEL") -> drawGMeter(canvas, can, accel, skin, lamp, live)
                    lab.startsWith("ENG") -> {
                        drawEngineGauge(canvas, can, r, engines, lit, true, skin, lamp, live, scale01, launch, stg)
                        if (hudFailed(launch, tSec)) {
                            fillPaint.color = Color.argb(140, 180, 16, 16)
                            canvas.drawRect(can, fillPaint)
                            telBold()
                            hudPaint.textAlign = Paint.Align.CENTER
                            hudPaint.color = Color.WHITE
                            hudPaint.textSize = telFit("FAIL", can.width() * 0.86f, can.height() * 0.36f, 18f + scale01 * 22f)
                            canvas.drawText("FAIL", can.centerX(), can.centerY() + can.height() * 0.10f, hudPaint)
                        }
                    }
                    lab.startsWith("FUEL") -> drawFuelTanks(canvas, can, fuel, fuel, skin, lamp)
                    else -> drawSpacecraftGauge(canvas, can.centerX(), can.centerY(), r, frac, lab, skin, lamp, live || lab == "ACCEL")
                }
                val range = analogCaption(lab, altVal, spdVal, imperial)
                if (range.isNotEmpty()) {
                    telBold()
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = withLamp(skin.hold, lamp)
                    drawRangeCaption(canvas, range, x, faceBot + rangeH * 0.78f, cellW * 0.98f, rangeH)
                }
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = telFit(lab, cellW * 0.94f, botTextH * 0.92f, 13f + scale01 * 20f)
                canvas.drawText(lab, x, bot - 2f, hudPaint)
                } catch (_: Exception) {
                    telBold()
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = withLamp(skin.text, lamp)
                    hudPaint.textSize = telFit(value, cellW * 0.9f, gH * 0.4f, 16f)
                    canvas.drawText(value, x, (top + bot) * 0.55f, hudPaint)
                }
            }
            } finally {
                try { canvas.restoreToCount(gaugeMark) } catch (_: Exception) { }
            }
        }

        private fun fuelRemain(tSec: Float, stage: Int = 1, launch: com.ccos.retro.data.LaunchSnapshot? = telemetryModule.tracked): Float =
            com.ccos.retro.event.FlightProfiles.fuelRemain(tSec, launch, stage)

        private fun analogBand(value: Float, edges: FloatArray): Pair<Float, Float> {
            val v = value.coerceAtLeast(0f)
            var i = 1
            while (i < edges.lastIndex && v >= edges[i]) i++
            return edges[i - 1] to edges[i]
        }

        private fun analogSpeedWindow(spdVal: Float, imperial: Boolean): Pair<Float, Float> =
            analogBand(
                spdVal,
                if (imperial) floatArrayOf(0f, 300f, 1000f, 3000f, 10000f, 18000f)
                else floatArrayOf(0f, 500f, 1600f, 5000f, 16000f, 28000f)
            )

        private fun analogAltWindow(altVal: Float, imperial: Boolean): Pair<Float, Float> =
            analogBand(
                altVal,
                if (imperial) floatArrayOf(0f, 5f, 20f, 50f, 120f, 250f)
                else floatArrayOf(0f, 8f, 30f, 80f, 200f)
            )

        private fun analogRangeText(lo: Float, hi: Float, unit: String): String {
            fun n(x: Float): String =
                if (x >= 100f) String.format("%,.0f", x)
                else if (abs(x - x.toInt()) < 0.05f) String.format("%.0f", x)
                else String.format("%.1f", x)
            val span = if (lo <= 0.01f) "0–${n(hi)}" else "${n(lo)}–${n(hi)}"
            return if (unit.isBlank()) span else "$span $unit"
        }

        private fun analogCaption(lab: String, altVal: Float, spdVal: Float, imperial: Boolean): String {
            return when {
                lab.startsWith("ALT") -> {
                    val (lo, hi) = analogAltWindow(altVal, imperial)
                    analogRangeText(lo, hi, if (imperial) "mi" else "km")
                }
                lab.startsWith("SPD") -> {
                    val (lo, hi) = analogSpeedWindow(spdVal, imperial)
                    analogRangeText(lo, hi, if (imperial) "mph" else "km/h")
                }
                else -> ""
            }
        }

        private fun drawRangeCaption(canvas: Canvas, text: String, cx: Float, y: Float, maxW: Float, maxH: Float) {
            telBold()
            hudPaint.textAlign = Paint.Align.CENTER
            var size = telSp(18f).coerceIn(telSp(15f), maxH * 0.72f)
            hudPaint.textSize = size
            if (hudPaint.measureText(text) * 1.04f <= maxW) {
                canvas.drawText(text, cx, y, hudPaint)
                return
            }
            val parts = text.split(" ")
            val main = parts.dropLast(1).joinToString(" ").ifBlank { text }
            val unit = if (parts.size > 1) parts.last() else ""
            size = telSp(16f).coerceIn(telSp(14f), maxH * 0.46f)
            hudPaint.textSize = size
            canvas.drawText(main, cx, y - size * 0.55f, hudPaint)
            if (unit.isNotEmpty()) {
                canvas.drawText(unit, cx, y + size * 0.65f, hudPaint)
            }
        }

        private fun drawAltitudeTape(
            canvas: Canvas,
            can: RectF,
            altVal: Float,
            imperial: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            live: Boolean
        ) {
            val (lo, hi) = analogAltWindow(altVal, imperial)
            val span = (hi - lo).coerceAtLeast(0.01f)
            val frac = ((altVal.coerceAtLeast(0f) - lo) / span).coerceIn(0f, 1f)
            val tape = RectF(
                can.left + can.width() * 0.18f,
                can.top + 6f,
                can.right - can.width() * 0.40f,
                can.bottom - 6f
            )
            fillPaint.shader = null
            fillPaint.color = Color.parseColor("#05070A")
            canvas.drawRoundRect(tape, 5f, 5f, fillPaint)
            val inset = 3.5f
            val fillH = (tape.height() - inset * 2f) * frac
            fillPaint.color = withLamp(if (live) skin.go else skin.muted, lamp)
            canvas.drawRect(
                tape.left + inset,
                tape.bottom - inset - fillH,
                tape.right - inset,
                tape.bottom - inset,
                fillPaint
            )
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2.8f
            strokePaint.color = withLamp(if (live) skin.accent else skin.muted, lamp)
            canvas.drawRoundRect(tape, 5f, 5f, strokePaint)
            val step = when {
                span <= 5.01f -> 1f
                span <= 20.01f -> 5f
                span <= 50.01f -> 10f
                else -> 25f
            }
            var mark = lo
            telBold()
            hudPaint.textAlign = Paint.Align.LEFT
            while (mark <= hi + 0.001f) {
                val u = ((mark - lo) / span).coerceIn(0f, 1f)
                val y = tape.bottom - tape.height() * u
                val isMajor = abs((mark - lo) % step) < 0.01f
                strokePaint.strokeWidth = if (abs(mark - lo) < 0.01f || abs(mark - hi) < 0.01f) 2.4f else 1.4f
                strokePaint.color = withLamp(Color.WHITE, lamp)
                canvas.drawLine(tape.right, y, tape.right + can.width() * 0.06f, y, strokePaint)
                if (isMajor) {
                    hudPaint.color = withLamp(skin.text, lamp)
                    hudPaint.textSize = telSp(13f).coerceAtMost(can.height() * 0.14f)
                    val label = if (span <= 8f) String.format("%.0f", mark) else String.format("%.0f", mark)
                    canvas.drawText(label, tape.right + can.width() * 0.08f, y + hudPaint.textSize * 0.35f, hudPaint)
                }
                mark += step
            }
            val py = tape.bottom - inset - fillH
            fillPaint.color = withLamp(skin.hold, lamp)
            val chev = Path()
            chev.moveTo(tape.right + 1f, py)
            chev.lineTo(tape.right + can.width() * 0.12f, py - 8f)
            chev.lineTo(tape.right + can.width() * 0.12f, py + 8f)
            chev.close()
            canvas.drawPath(chev, fillPaint)
        }

        private fun drawSpeedTape(
            canvas: Canvas,
            can: RectF,
            spdVal: Float,
            imperial: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            live: Boolean
        ) {
            val (lo, hi) = analogSpeedWindow(spdVal, imperial)
            val frac = ((spdVal.coerceAtLeast(0f) - lo) / (hi - lo).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val pad = can.width() * 0.08f
            val barH = (can.height() * 0.22f).coerceIn(10f, 22f)
            val bar = RectF(
                can.left + pad,
                can.centerY() - barH * 0.55f,
                can.right - pad,
                can.centerY() + barH * 0.55f
            )
            val n = 10
            val gap = 3f
            val segW = (bar.width() - gap * (n - 1)) / n
            fillPaint.shader = null
            val litN = kotlin.math.round(frac * n).toInt().coerceIn(0, n)
            for (i in 0 until n) {
                val l = bar.left + i * (segW + gap)
                val on = i < litN
                fillPaint.color = if (on) {
                    val u = i / (n - 1).toFloat()
                    Color.rgb((30 + u * 40).toInt(), (210 - u * 70).toInt(), (90 + u * 80).toInt())
                } else {
                    Color.parseColor("#182028")
                }
                canvas.drawRoundRect(l, bar.top, l + segW, bar.bottom, 3f, 3f, fillPaint)
            }
            val nx = bar.left + bar.width() * frac
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3f
            strokePaint.color = Color.parseColor("#F2C14E")
            canvas.drawLine(nx, bar.top - 8f, nx, bar.bottom + 8f, strokePaint)
            fillPaint.color = Color.parseColor("#F2C14E")
            canvas.drawCircle(nx, bar.top - 8f, 3.4f, fillPaint)
            telBold()
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = telSp(13f).coerceAtMost(can.height() * 0.16f)
            val loTxt = if (lo <= 0.01f) "0" else String.format(if (lo >= 100f) "%,.0f" else "%.0f", lo)
            val hiTxt = String.format(if (hi >= 100f) "%,.0f" else "%.0f", hi)
            hudPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(loTxt, bar.left, bar.bottom + hudPaint.textSize + 4f, hudPaint)
            hudPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(hiTxt, bar.right, bar.bottom + hudPaint.textSize + 4f, hudPaint)
        }

        private fun drawGMeter(
            canvas: Canvas,
            can: RectF,
            accel: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            live: Boolean
        ) {
            val padX = can.width() * 0.32f
            val col = RectF(can.left + padX, can.top + can.height() * 0.08f, can.right - padX, can.bottom - can.height() * 0.08f)
            fillPaint.color = Color.argb(240, 4, 6, 8)
            canvas.drawRoundRect(col, 5f, 5f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3.5f
            strokePaint.color = withLamp(if (live) skin.accent else skin.muted, lamp)
            canvas.drawRoundRect(col, 5f, 5f, strokePaint)
            val maxG = 4f
            val oneG = col.bottom - (1f / maxG) * col.height()
            strokePaint.strokeWidth = 3f
            strokePaint.color = withLamp(skin.go, lamp)
            canvas.drawLine(col.left, oneG, col.right, oneG, strokePaint)
            val g = accel.coerceIn(0f, maxG)
            val y = col.bottom - (g / maxG) * col.height()
            fillPaint.color = withLamp(if (g > 3f) skin.danger else skin.hold, lamp)
            canvas.drawRect(col.left + 4f, y, col.right - 4f, col.bottom - 4f, fillPaint)
            fillPaint.color = Color.WHITE
            canvas.drawCircle(col.centerX(), y, (col.width() * 0.16f).coerceAtLeast(4f), fillPaint)
        }

        private fun drawFuelTanks(
            canvas: Canvas,
            can: RectF,
            lox: Float,
            rp: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val pad = can.width() * 0.12f
            val gap = can.width() * 0.08f
            val tankW = (can.width() - pad * 2f - gap) / 2f
            val top = can.top + can.height() * 0.10f
            val bot = can.bottom - can.height() * 0.18f
            val h = (bot - top).coerceAtLeast(8f)
            fun tank(x: Float, frac: Float, fill: Int, label: String) {
                val r = RectF(x, top, x + tankW, bot)
                fillPaint.color = Color.argb(220, 8, 10, 12)
                canvas.drawRoundRect(r, 5f, 5f, fillPaint)
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = 3.5f
                strokePaint.color = withLamp(skin.accent, lamp)
                canvas.drawRoundRect(r, 5f, 5f, strokePaint)
                val fh = h * frac.coerceIn(0f, 1f)
                fillPaint.color = withLamp(fill, lamp)
                canvas.drawRect(r.left + 3f, r.bottom - 3f - fh, r.right - 3f, r.bottom - 3f, fillPaint)
                telBold()
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = telFit(label, tankW * 0.95f, (can.bottom - bot).coerceAtLeast(10f) * 0.9f, 11f)
                canvas.save()
                canvas.clipRect(can)
                canvas.drawText(label, r.centerX(), (bot + can.bottom) * 0.5f + hudPaint.textSize * 0.35f, hudPaint)
                canvas.restore()
            }
            tank(can.left + pad, lox, Color.parseColor("#3AA0FF"), "LOX")
            tank(can.left + pad + tankW + gap, rp, Color.parseColor("#E07A2F"), "RP")
        }

        private fun drawInstrumentCan(
            canvas: Canvas,
            can: RectF,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            fillPaint.color = Color.argb((230 * lamp).toInt().coerceIn(90, 235), 14, 16, 20)
            canvas.drawRoundRect(can, 6f, 6f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = su(0.007f).coerceIn(3.5f, 6.5f)
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawRoundRect(can, 6f, 6f, strokePaint)
            strokePaint.strokeWidth = su(0.003f).coerceIn(1.6f, 3f)
            strokePaint.color = withLamp(skin.muted, lamp)
            val inset = su(0.006f).coerceIn(3f, 6f)
            canvas.drawRoundRect(
                can.left + inset, can.top + inset, can.right - inset, can.bottom - inset,
                4f, 4f, strokePaint
            )
            val screw = su(0.008f).coerceIn(3.2f, 5.5f)
            val pad = inset + screw
            fillPaint.color = withLamp(skin.muted, lamp)
            val screws = arrayOf(
                can.left + pad to can.top + pad,
                can.right - pad to can.top + pad,
                can.left + pad to can.bottom - pad,
                can.right - pad to can.bottom - pad
            )
            for ((sx, sy) in screws) {
                canvas.drawCircle(sx, sy, screw, fillPaint)
                strokePaint.strokeWidth = 1.6f
                strokePaint.color = withLamp(skin.bg, lamp)
                canvas.drawLine(sx - screw * 0.55f, sy, sx + screw * 0.55f, sy, strokePaint)
                canvas.drawLine(sx, sy - screw * 0.55f, sx, sy + screw * 0.55f, strokePaint)
            }
        }

        private fun resetHudPaints() {
            fillPaint.shader = null
            fillPaint.maskFilter = null
            fillPaint.style = Paint.Style.FILL
            fillPaint.strokeWidth = 0f
            fillPaint.alpha = 255
            strokePaint.shader = null
            strokePaint.maskFilter = null
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f
            strokePaint.strokeCap = Paint.Cap.BUTT
        }

        private fun drawAnalogDigitalRocker(
            canvas: Canvas,
            r: RectF,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            if (r.width() < 8f || r.height() < 8f) return
            resetHudPaints()
            val mid = r.centerY()
            val analog = prefs.telemetryAnalog
            fillPaint.color = withLamp(skin.panel, lamp)
            canvas.drawRoundRect(r, 4f, 4f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawRoundRect(r, 4f, 4f, strokePaint)
            val left = RectF(r.left + 1.5f, r.top + 1.5f, r.right - 1.5f, mid - 1f)
            val right = RectF(r.left + 1.5f, mid + 1f, r.right - 1.5f, r.bottom - 1.5f)
            fun half(box: RectF, on: Boolean, label: String) {
                if (box.width() < 4f || box.height() < 4f) return
                fillPaint.shader = null
                fillPaint.style = Paint.Style.FILL
                fillPaint.color = withLamp(if (on) skin.btnActiveFill else skin.btnIdleFill, lamp)
                canvas.drawRoundRect(box, 4f, 4f, fillPaint)
                if (on) {
                    strokePaint.style = Paint.Style.STROKE
                    strokePaint.strokeWidth = 2f
                    strokePaint.color = withLamp(skin.accent, lamp)
                    canvas.drawRoundRect(box, 4f, 4f, strokePaint)
                }
                telBold()
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(if (on) Color.WHITE else skin.muted, lamp)
                hudPaint.textSize = telFit(label, box.width() * 0.90f, box.height() * 0.62f, 14f)
                canvas.drawText(label, box.centerX(), box.centerY() + box.height() * 0.22f, hudPaint)
            }
            half(left, !analog, "DIG")
            half(right, analog, "ANLG")
        }

        private fun drawEventSkipRocker(
            canvas: Canvas,
            r: RectF,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            if (r.width() < 8f || r.height() < 8f) return
            resetHudPaints()
            val mid = r.centerY()
            fillPaint.color = withLamp(skin.panel, lamp)
            canvas.drawRoundRect(r, 4f, 4f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawRoundRect(r, 4f, 4f, strokePaint)
            val left = RectF(r.left + 1.5f, r.top + 1.5f, r.right - 1.5f, mid - 1f)
            val right = RectF(r.left + 1.5f, mid + 1f, r.right - 1.5f, r.bottom - 1.5f)
            fun half(box: RectF, label: String) {
                if (box.width() < 4f || box.height() < 4f) return
                fillPaint.shader = null
                fillPaint.style = Paint.Style.FILL
                fillPaint.color = withLamp(skin.btnIdleFill, lamp)
                canvas.drawRoundRect(box, 4f, 4f, fillPaint)
                telBold()
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = telFit(label, box.width() * 0.90f, box.height() * 0.72f, 16f)
                canvas.drawText(label, box.centerX(), box.centerY() + box.height() * 0.24f, hudPaint)
            }
            half(left, "+")
            half(right, "-")
        }

        private fun drawLockChip(
            canvas: Canvas,
            r: RectF,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            if (r.width() < 8f || r.height() < 8f) return
            resetHudPaints()
            val on = prefs.telemetryPinned
            fillPaint.shader = null
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = withLamp(if (on) skin.btnActiveFill else skin.btnIdleFill, lamp)
            canvas.drawRoundRect(r, 4f, 4f, fillPaint)
            if (on) {
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = 2f
                strokePaint.color = withLamp(skin.accent, lamp)
                canvas.drawRoundRect(r, 4f, 4f, strokePaint)
            }
            telBold()
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(if (on) Color.WHITE else skin.muted, lamp)
            hudPaint.textSize = telFit("LCK", r.width() * 0.90f, r.height() * 0.62f, 14f)
            canvas.drawText("LCK", r.centerX(), r.centerY() + r.height() * 0.22f, hudPaint)
        }

        private fun drawSpacecraftGauge(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            r: Float,
            value01: Float,
            kind: String,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            live: Boolean
        ) {
            val v = value01.coerceIn(0f, 1f)
            val start = 140f
            val sweep = 260f
            val face = r * 0.98f

            fillPaint.color = Color.argb(255, 6, 8, 10)
            canvas.drawCircle(cx, cy, face, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.strokeWidth = (r * 0.16f).coerceIn(6f, 12f)
            strokePaint.color = withLamp(if (live) skin.accent else skin.muted, lamp)
            canvas.drawCircle(cx, cy, face, strokePaint)
            strokePaint.strokeWidth = (r * 0.04f).coerceIn(2f, 4f)
            strokePaint.color = Color.argb(180, 30, 34, 40)
            canvas.drawCircle(cx, cy, face * 0.88f, strokePaint)

            val warn0 = when (kind) {
                "ACCEL" -> 0.62f
                "SPD" -> 0.72f
                else -> 0.80f
            }
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.strokeWidth = (r * 0.10f).coerceIn(4f, 8f)
            strokePaint.color = withLamp(skin.hold, lamp)
            canvas.drawArc(cx - face * 0.80f, cy - face * 0.80f, cx + face * 0.80f, cy + face * 0.80f,
                start + sweep * warn0, sweep * (0.90f - warn0), false, strokePaint)
            strokePaint.color = withLamp(skin.danger, lamp)
            canvas.drawArc(cx - face * 0.80f, cy - face * 0.80f, cx + face * 0.80f, cy + face * 0.80f,
                start + sweep * 0.90f, sweep * 0.10f, false, strokePaint)

            if (kind == "ACCEL") {
                val g1 = start + sweep * 0.25f
                strokePaint.color = withLamp(skin.go, lamp)
                strokePaint.strokeWidth = (r * 0.08f).coerceIn(3.5f, 6f)
                val a = Math.toRadians(g1.toDouble())
                canvas.drawLine(
                    cx + (face * 0.70f * cos(a)).toFloat(),
                    cy + (face * 0.70f * sin(a)).toFloat(),
                    cx + (face * 0.86f * cos(a)).toFloat(),
                    cy + (face * 0.86f * sin(a)).toFloat(),
                    strokePaint
                )
            }

            strokePaint.strokeCap = Paint.Cap.SQUARE
            val majors = 5
            val minors = 20
            for (i in 0..minors) {
                val a = Math.toRadians((start + sweep * (i / minors.toFloat())).toDouble())
                val major = i % (minors / majors) == 0
                val inner = face * if (major) 0.58f else 0.70f
                strokePaint.strokeWidth = if (major) (r * 0.075f).coerceIn(3.5f, 6.5f) else (r * 0.035f).coerceIn(2f, 3.4f)
                strokePaint.color = withLamp(if (major) Color.WHITE else skin.muted, lamp)
                canvas.drawLine(
                    cx + (inner * cos(a)).toFloat(),
                    cy + (inner * sin(a)).toFloat(),
                    cx + (face * 0.84f * cos(a)).toFloat(),
                    cy + (face * 0.84f * sin(a)).toFloat(),
                    strokePaint
                )
            }

            if (r > 28f) {
                telBold()
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(Color.WHITE, lamp)
                val numH = (r * 0.16f).coerceIn(9f, 16f)
                hudPaint.textSize = numH
                val nums = when (kind) {
                    "SPD" -> arrayOf("0", "7", "14", "21", "28")
                    "ACCEL" -> arrayOf("0", "1", "2", "3", "4")
                    else -> arrayOf("0", "5", "10", "15", "20")
                }
                for (i in nums.indices) {
                    val a = Math.toRadians((start + sweep * (i / 4f)).toDouble())
                    canvas.drawText(
                        nums[i],
                        cx + (face * 0.46f * cos(a)).toFloat(),
                        cy + (face * 0.46f * sin(a)).toFloat() + numH * 0.35f,
                        hudPaint
                    )
                }
            }

            val na = Math.toRadians((start + sweep * v).toDouble())
            val tipR = face * 0.80f
            val tailR = face * 0.28f
            val halfW = (r * 0.085f).coerceIn(4f, 8f)
            val tx = cos(na).toFloat()
            val ty = sin(na).toFloat()
            val px = -ty
            val py = tx
            val needle = Path()
            needle.moveTo(cx + tipR * tx, cy + tipR * ty)
            needle.lineTo(cx + px * halfW * 0.45f + tx * face * 0.55f, cy + py * halfW * 0.45f + ty * face * 0.55f)
            needle.lineTo(cx + px * halfW, cy + py * halfW)
            needle.lineTo(cx - tailR * tx, cy - tailR * ty)
            needle.lineTo(cx - px * halfW, cy - py * halfW)
            needle.close()
            fillPaint.color = withLamp(if (live) Color.parseColor("#F2C14E") else skin.muted, lamp)
            canvas.drawPath(needle, fillPaint)
            fillPaint.color = withLamp(Color.WHITE, lamp)
            canvas.drawCircle(cx, cy, (r * 0.15f).coerceIn(6f, 13f), fillPaint)
            fillPaint.color = Color.argb(255, 8, 8, 10)
            canvas.drawCircle(cx, cy, (r * 0.06f).coerceIn(2.8f, 5.5f), fillPaint)

            strokePaint.strokeWidth = (r * 0.035f).coerceIn(1.8f, 3.2f)
            strokePaint.color = Color.argb(70, 255, 255, 255)
            canvas.drawArc(cx - face * 0.72f, cy - face * 0.72f, cx + face * 0.72f, cy + face * 0.72f,
                220f, 70f, false, strokePaint)
        }

        private fun drawEngineGauge(
            canvas: Canvas,
            can: RectF,
            r: Float,
            total: Int,
            lit: Int,
            analog: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            live: Boolean,
            scale01: Float,
            launch: com.ccos.retro.data.LaunchSnapshot?,
            stage: Int
        ) {
            val cx = can.centerX()
            val cy = can.centerY()
            val on = live && lit > 0
            if (analog) {
                val face = r * 0.98f
                fillPaint.color = Color.argb(255, 6, 8, 10)
                canvas.drawCircle(cx, cy, face, fillPaint)
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = (r * 0.10f).coerceIn(3f, 8f)
                strokePaint.color = withLamp(if (on) skin.accent else skin.muted, lamp)
                canvas.drawCircle(cx, cy, face, strokePaint)
                EngineDraw.drawEnginePattern(canvas, cx, cy, face * 1.72f, launch, stage, lit, skin)
            } else {
                val count = "$lit/$total"
                telBold()
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(if (on) skin.go else skin.text, lamp)
                hudPaint.textSize = telFit(count, can.width() * 0.86f, can.height() * 0.42f, 28f + scale01 * 36f)
                canvas.drawText(count, cx, cy - can.height() * 0.02f, hudPaint)
                hudPaint.color = withLamp(if (on) skin.hold else skin.muted, lamp)
                hudPaint.textSize = telFit("LIT", can.width() * 0.70f, can.height() * 0.22f, 16f + scale01 * 20f)
                canvas.drawText("LIT", cx, cy + can.height() * 0.28f, hudPaint)
            }
        }

        /** Home HUD follows the stack. MCC STG2 must not turn Super Heavy into 6 ship bells. */
        private fun hudStage(launch: com.ccos.retro.data.LaunchSnapshot?, tSec: Float): Int {
            if (launch == null) return 1
            val sep = sepTime(launch)
            if (tSec < sep) return 1
            return prefs.trackedStage.coerceIn(1, 2)
        }

        private fun engineCount(launch: com.ccos.retro.data.LaunchSnapshot, tSec: Float = 0f): Int =
            engineCountForStage(launch, hudStage(launch, tSec))

        private fun engineCountForStage(launch: com.ccos.retro.data.LaunchSnapshot, stage: Int): Int =
            com.ccos.retro.event.VehicleCatalog.engines(launch, stage)

        private fun drawEngineBank(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            total: Int,
            lit: Int,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            radius: Float = width * 0.12f,
            launch: com.ccos.retro.data.LaunchSnapshot? = null,
            stage: Int = 1
        ) {
            EngineDraw.drawEnginePattern(canvas, cx, cy, radius * 2.05f, launch, stage, lit, skin)
            telBold()
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = telSp(18f)
            canvas.drawText("ENG $lit/$total", cx, cy + radius + telSp(22f), hudPaint)
        }

        private fun drawArcGauge(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            r: Float,
            value01: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            live: Boolean
        ) {
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeCap = Paint.Cap.ROUND
            strokePaint.strokeWidth = (r * 0.16f).coerceIn(5f, 10f)
            strokePaint.color = withLamp(skin.grid, lamp)
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 150f, 240f, false, strokePaint)
            strokePaint.color = withLamp(if (live) skin.accent else skin.muted, lamp)
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 150f, 240f * value01.coerceIn(0f, 1f), false, strokePaint)
        }

        private fun drawTrackingRocket(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            scale: Float,
            tSec: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val launch = telemetryModule.tracked ?: return
            val h = min(width, height) * 0.22f * scale
            drawVehicle(canvas, cx, baseY, h, launch, tSec, 1, tSec >= sepTime(launch), skin, lamp, alpha)
        }

        private fun vehicleFamily(launch: com.ccos.retro.data.LaunchSnapshot): String =
            com.ccos.retro.event.VehicleCatalog.family(launch)

        private fun sepTime(launch: com.ccos.retro.data.LaunchSnapshot?): Float =
            com.ccos.retro.event.FlightProfiles.sepTime(launch)

        private fun drawSideStageRockets(
            canvas: Canvas,
            launch: com.ccos.retro.data.LaunchSnapshot,
            tSec: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            mapBot: Float
        ) {
            if (buttonRects[3].width() <= 4f || buttonRects[7].width() <= 4f) {
                stage1Hit.setEmpty()
                stage2Hit.setEmpty()
                return
            }
            val minTop = buttonRects[3].bottom + 10f
            val analogCeil = if (mapBot > minTop + 24f) mapBot else minTop + height * 0.16f
            val slot = (analogCeil - minTop).coerceAtLeast(height * 0.14f)
            val rocketH = min(height * 0.16f, slot * 0.90f)
            val baseY = minTop + rocketH
            val leftCx = buttonRects[3].centerX()
            val rightCx = buttonRects[7].centerX()
            val separated = tSec >= sepTime(launch)
            val boxW = buttonRects[3].width()
            val top = minTop
            val bot = (baseY + telSp(16f)).coerceAtMost(analogCeil + telSp(8f))
            stage1Hit.set(leftCx - boxW * 0.5f, top, leftCx + boxW * 0.5f, bot)
            stage2Hit.set(rightCx - boxW * 0.5f, top, rightCx + boxW * 0.5f, bot)

            fun frame(hit: RectF, selected: Boolean) {
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = if (selected) 3f else 1.4f
                strokePaint.color = withLamp(if (selected) skin.accent else skin.muted, lamp * if (selected) 1f else 0.55f)
                canvas.drawRoundRect(hit.left, hit.top, hit.right, hit.bottom - telSp(14f), 6f, 6f, strokePaint)
            }
            val leftSel = prefs.trackedStage == 1
            val rightSel = prefs.trackedStage == 2
            frame(stage1Hit, leftSel)
            frame(stage2Hit, rightSel)

            canvas.save()
            canvas.clipRect(stage1Hit)
            if (!separated) {
                drawVehicle(canvas, leftCx, baseY, rocketH, launch, tSec, 1, false, skin, lamp, 1f)
            } else {
                drawVehicle(canvas, leftCx, baseY, rocketH * 0.88f, launch, tSec, 1, true, skin, lamp, 1f)
            }
            canvas.restore()
            canvas.save()
            canvas.clipRect(stage2Hit)
            if (!separated) {
                drawVehicle(canvas, rightCx, baseY, rocketH, launch, tSec, 1, false, skin, lamp, 0.42f)
            } else {
                drawVehicle(canvas, rightCx, baseY, rocketH * 0.88f, launch, tSec, 2, true, skin, lamp, 1f)
            }
            canvas.restore()

            telBold()
            hudPaint.textAlign = Paint.Align.CENTER
            val labH = telSp(11f)
            val leftLab = if (separated) "STG1" else "STACK"
            val rightLab = if (separated) "STG2" else "STACK"
            hudPaint.color = withLamp(if (leftSel) skin.accent else skin.muted, lamp)
            hudPaint.textSize = telFit(leftLab, stage1Hit.width() * 0.9f, labH, 10f)
            canvas.drawText(leftLab, leftCx, bot - 2f, hudPaint)
            hudPaint.color = withLamp(if (rightSel) skin.accent else skin.muted, lamp)
            hudPaint.textSize = telFit(rightLab, stage2Hit.width() * 0.9f, labH, 10f)
            canvas.drawText(rightLab, rightCx, bot - 2f, hudPaint)
        }

        private fun lampAlpha(c: Int, lamp: Float, alpha: Float): Int {
            val base = withLamp(c, lamp)
            val a = (Color.alpha(base) * alpha.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
            return Color.argb(a, Color.red(base), Color.green(base), Color.blue(base))
        }

        private fun stageBurning(tSec: Float, stage: Int, separated: Boolean, sep: Float): Boolean {
            if (tSec < 0f) return false
            val tot = if (stage >= 2) 6 else 33
            return com.ccos.retro.event.FlightProfiles.enginesLit(
                tSec, telemetryModule.tracked, stage, tot
            ) > 0
        }

        private fun drawVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            VehicleDraw.draw(canvas, cx, baseY, h, launch, tSec, stage, separated, skin, lamp, alpha)
        }

        private fun drawBookGap(
            canvas: Canvas,
            cx: Float,
            y: Float,
            h: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            fillPaint.color = Color.argb(190, 12, 10, 4)
            val w = h * 1.15f
            canvas.drawRoundRect(cx - w, y - h * 0.22f, cx + w, y + h * 0.28f, 8f, 8f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2.4f
            strokePaint.color = withLamp(skin.hold, lamp)
            canvas.drawRoundRect(cx - w, y - h * 0.22f, cx + w, y + h * 0.28f, 8f, 8f, strokePaint)
            telBold()
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(skin.hold, lamp)
            hudPaint.textSize = telFit(com.ccos.retro.event.VehicleCatalog.UPDATE_HEAD, w * 1.9f, h * 0.16f, 13f)
            canvas.drawText(com.ccos.retro.event.VehicleCatalog.UPDATE_HEAD, cx, y - h * 0.02f, hudPaint)
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = telFit("WILL NOT INVENT DRAWING OR NUMBERS", w * 1.9f, h * 0.12f, 11f)
            canvas.drawText("WILL NOT INVENT DRAWING OR NUMBERS", cx, y + h * 0.16f, hudPaint)
        }

        private fun drawVehicleBell(
            canvas: Canvas,
            cx: Float,
            y: Float,
            halfW: Float,
            hBell: Float,
            lamp: Float,
            alpha: Float,
            copper: Boolean
        ) {
            if (halfW < 1.4f || hBell < 2f) return
            val throat = halfW * 0.40f
            val p = Path()
            p.moveTo(cx - throat, y)
            p.lineTo(cx + throat, y)
            p.lineTo(cx + halfW, y + hBell)
            p.lineTo(cx - halfW, y + hBell)
            p.close()
            fillPaint.shader = null
            fillPaint.color = lampAlpha(
                if (copper) Color.parseColor("#C46A28") else Color.parseColor("#3A4048"),
                lamp, alpha
            )
            canvas.drawPath(p, fillPaint)
            fillPaint.color = lampAlpha(
                if (copper) Color.parseColor("#6A3010") else Color.parseColor("#101214"),
                lamp, alpha
            )
            canvas.drawOval(cx - halfW * 0.92f, y + hBell * 0.78f, cx + halfW * 0.92f, y + hBell * 1.12f, fillPaint)
            fillPaint.color = Color.argb((210 * alpha).toInt().coerceIn(0, 255), 6, 6, 8)
            canvas.drawOval(cx - halfW * 0.50f, y + hBell * 0.88f, cx + halfW * 0.50f, y + hBell * 1.08f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.15f
            strokePaint.color = lampAlpha(Color.parseColor("#D8C8B0"), lamp, alpha * 0.75f)
            canvas.drawPath(p, strokePaint)
        }

        private fun drawFlame(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            w: Float,
            h: Float,
            alpha: Float,
            tSec: Float,
            kind: String = "merlin"
        ) {
            val flick = 0.80f + 0.20f * sin((tSec * 33f + cx * 0.07f).toDouble()).toFloat()
            val len = h * flick
            val (c0, c1, c2) = when (kind) {
                "raptor" -> Triple(Color.rgb(70, 160, 255), Color.rgb(170, 220, 255), Color.rgb(240, 250, 255))
                "rs25" -> Triple(Color.rgb(110, 175, 255), Color.rgb(200, 230, 255), Color.rgb(255, 255, 255))
                "srb" -> Triple(Color.rgb(255, 78, 12), Color.rgb(255, 160, 50), Color.rgb(255, 235, 190))
                "rd107" -> Triple(Color.rgb(255, 70, 16), Color.rgb(255, 140, 36), Color.rgb(255, 215, 110))
                "mvac" -> Triple(Color.rgb(255, 96, 24), Color.rgb(255, 175, 64), Color.rgb(255, 236, 160))
                else -> Triple(Color.rgb(255, 64, 8), Color.rgb(255, 128, 28), Color.rgb(255, 214, 110))
            }
            fun wash(c: Int, a: Float): Int = Color.argb(
                (a * alpha * 255f).toInt().coerceIn(0, 255),
                Color.red(c), Color.green(c), Color.blue(c)
            )
            val plume = Path()
            plume.moveTo(cx - w * 0.70f, baseY)
            plume.quadTo(cx - w * 0.18f, baseY + len * 0.42f, cx, baseY + len)
            plume.quadTo(cx + w * 0.18f, baseY + len * 0.42f, cx + w * 0.70f, baseY)
            plume.close()
            fillPaint.shader = null
            fillPaint.color = wash(c0, if (kind == "srb") 0.70f else 0.52f)
            canvas.drawPath(plume, fillPaint)
            val core = Path()
            core.moveTo(cx - w * 0.30f, baseY)
            core.quadTo(cx, baseY + len * 0.58f, cx + w * 0.30f, baseY)
            core.close()
            fillPaint.color = wash(c1, 0.88f)
            canvas.drawPath(core, fillPaint)
            fillPaint.color = wash(c2, 0.96f)
            canvas.drawCircle(cx, baseY + len * 0.16f, w * 0.15f, fillPaint)
        }

        private fun drawStageTanks(
            canvas: Canvas,
            x: Float,
            top: Float,
            bot: Float,
            halfW: Float,
            fuel: Float,
            methalox: Boolean,
            lamp: Float,
            alpha: Float
        ) {
            if (bot - top < 6f || halfW < 2.2f) return
            val rim = (halfW * 0.08f).coerceAtLeast(1.1f)
            val innerL = x - halfW + rim
            val innerR = x + halfW - rim
            val pad = ((bot - top) * 0.028f).coerceAtLeast(1.4f)
            val tankTop = top + pad
            val tankBot = bot - pad
            val bulk = tankTop + (tankBot - tankTop) * 0.52f
            fillPaint.shader = null
            fillPaint.color = Color.argb((240 * alpha).toInt().coerceIn(0, 255), 8, 10, 14)
            canvas.drawRect(innerL, tankTop, innerR, tankBot, fillPaint)
            val lox = Color.argb((255 * alpha).toInt().coerceIn(0, 255), 28, 110, 220)
            val fuelC = if (methalox)
                Color.argb((255 * alpha).toInt().coerceIn(0, 255), 16, 205, 190)
            else
                Color.argb((255 * alpha).toInt().coerceIn(0, 255), 230, 105, 18)
            val f = fuel.coerceIn(0f, 1f)
            if (f > 0f) {
                fillPaint.color = lox
                canvas.drawRect(innerL, bulk - (bulk - tankTop) * f, innerR, bulk, fillPaint)
                fillPaint.color = fuelC
                canvas.drawRect(innerL, tankBot - (tankBot - bulk) * f, innerR, tankBot, fillPaint)
            }
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.4f
            strokePaint.color = Color.argb((210 * alpha).toInt().coerceIn(0, 255), 210, 225, 240)
            canvas.drawLine(innerL, bulk, innerR, bulk, strokePaint)
        }


        private fun drawOgive(
            canvas: Canvas,
            cx: Float,
            base: Float,
            tip: Float,
            halfW: Float,
            fill: Int,
            stroke: Int
        ) {
            VehicleOutline.ogive(canvas, cx, base, tip, halfW, fillPaint, strokePaint, fill, stroke)
        }

        private fun drawFalconVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            cores: Int,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
            val black = lampAlpha(Color.parseColor("#1A1A1A"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = 154f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val s1H = if (drawS2 && drawS1) h * 0.54f else if (drawS1) h * 0.92f else 0f
            val s2H = if (drawS1 && drawS2) h * 0.26f else if (drawS2) h * 0.62f else 0f
            val fairH = if (drawS2) (if (drawS1) h * 0.20f else h * 0.38f) else 0f
            val coreW = h * 0.070f
            val offsets = if (cores >= 3) floatArrayOf(-coreW * 2.15f, 0f, coreW * 2.15f) else floatArrayOf(0f)
            val s1Top = baseY - s1H
            val burning = stageBurning(tSec, stage, separated, sep)

            fun core(x: Float, sideCore: Boolean) {
                val top = if (sideCore && drawS2) s1Top else if (drawS1) s1Top else baseY - s2H - fairH
                val bot = baseY
                fillPaint.color = white
                canvas.drawRoundRect(x - coreW, top, x + coreW, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, x, top, bot, coreW, fuelRemain(tSec), methalox = false, lamp, alpha)
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = 1.5f
                strokePaint.color = stroke
                canvas.drawRoundRect(x - coreW, top, x + coreW, bot, 3f, 3f, strokePaint)
                // black interstage
                fillPaint.color = black
                canvas.drawRect(x - coreW, top, x + coreW, top + h * 0.018f, fillPaint)
                // octaweb
                fillPaint.color = black
                canvas.drawRect(x - coreW * 1.05f, bot - h * 0.018f, x + coreW * 1.05f, bot, fillPaint)
                val bellR = coreW * 0.16f
                for (i in 0 until 9) {
                    val a = Math.toRadians(-90.0 + i * 40.0)
                    val rr = if (i == 0) 0f else coreW * 0.55f
                    fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
                    canvas.drawCircle(x + (rr * cos(a)).toFloat(), bot - h * 0.006f, bellR, fillPaint)
                }
                strokePaint.strokeWidth = (h * 0.010f).coerceIn(1.4f, 3.2f)
                strokePaint.color = lampAlpha(Color.parseColor("#C8CCD0"), lamp, alpha)
                val attach = bot - h * 0.012f
                val legsOut = separated && com.ccos.retro.event.FlightProfiles.legsDeployed(telemetryModule.tracked, tSec, 1)
                val finsOut = separated && tSec >= 158f
                if (legsOut) {
                    val legLen = h * 0.13f
                    for (side in floatArrayOf(-1f, 1f)) {
                        for (d in floatArrayOf(0.85f, 1.15f)) {
                            val x0 = x + side * coreW * 0.9f
                            val x1 = x + side * coreW * (2.4f + d * 0.35f)
                            val y1 = attach + legLen
                            canvas.drawLine(x0, attach, x1, y1, strokePaint)
                            fillPaint.color = lampAlpha(Color.parseColor("#D0D4D8"), lamp, alpha)
                            canvas.drawRect(x1 - coreW * 0.22f, y1, x1 + coreW * 0.22f, y1 + h * 0.012f, fillPaint)
                        }
                    }
                } else {
                    val legLen = s1H.coerceAtLeast(h * 0.4f) * 0.34f
                    for (side in floatArrayOf(-1f, 1f)) {
                        for (d in floatArrayOf(0.95f, 1.18f)) {
                            val x0 = x + side * coreW * d
                            val x1 = x + side * coreW * (d + 0.28f)
                            val y1 = attach - legLen
                            canvas.drawLine(x0, attach, x1, y1, strokePaint)
                            canvas.drawLine(x1, y1, x1 + side * coreW * 0.14f, y1 - h * 0.016f, strokePaint)
                        }
                    }
                }
                if (drawS1) {
                    fillPaint.color = lampAlpha(Color.parseColor("#4A4A4A"), lamp, alpha)
                    val fin = coreW * (if (finsOut) 0.70f else 0.42f)
                    val fy = top + s1H * 0.10f
                    canvas.save()
                    canvas.rotate(if (finsOut) 0f else -18f, x - coreW, fy)
                    canvas.drawRect(x - coreW - fin, fy - fin * 0.45f, x - coreW, fy + fin * 0.45f, fillPaint)
                    canvas.restore()
                    canvas.save()
                    canvas.rotate(18f, x + coreW, fy)
                    canvas.drawRect(x + coreW, fy - fin * 0.45f, x + coreW + fin, fy + fin * 0.45f, fillPaint)
                    canvas.restore()
                }
                if (burning && drawS1) {
                    drawFlame(canvas, x, bot, coreW * 1.8f, h * 0.20f, alpha, tSec, "merlin")
                }
            }

            if (drawS1) {
                for (i in offsets.indices) core(cx + offsets[i], cores >= 3 && i != 1)
            }
            if (drawS2) {
                val s2Bot = if (drawS1) s1Top else baseY
                val s2Top = s2Bot - s2H
                val s2W = coreW * 0.78f
                fillPaint.color = white
                canvas.drawRoundRect(cx - s2W, s2Top, cx + s2W, s2Bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, s2Top, s2Bot, s2W, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.strokeWidth = 1.5f
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - s2W, s2Top, cx + s2W, s2Bot, 3f, 3f, strokePaint)
                fillPaint.color = black
                canvas.drawRect(cx - s2W * 0.7f, s2Bot - h * 0.03f, cx + s2W * 0.7f, s2Bot, fillPaint)
                val vac = Path()
                vac.moveTo(cx - s2W * 0.42f, s2Bot)
                vac.lineTo(cx - s2W * 0.62f, s2Bot + h * 0.045f)
                vac.lineTo(cx + s2W * 0.62f, s2Bot + h * 0.045f)
                vac.lineTo(cx + s2W * 0.42f, s2Bot)
                vac.close()
                fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
                canvas.drawPath(vac, fillPaint)
                drawOgive(canvas, cx, s2Top, s2Top - fairH, s2W, white, stroke)
                if (burning && !drawS1) drawFlame(canvas, cx, s2Bot + h * 0.045f, s2W * 1.6f, h * 0.16f, alpha, tSec, "mvac")
            }
        }

        private fun drawSlsVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val orange = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
            val white = lampAlpha(Color.parseColor("#E8E8E8"), lamp, alpha)
            val dark = lampAlpha(Color.parseColor("#2A3038"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = sepTime(telemetryModule.tracked ?: return)
            val drawCore = stage == 1
            val drawUpper = stage == 2 || (stage == 1 && !separated)
            val coreW = h * 0.085f
            val srbW = h * 0.055f
            val coreH = if (drawUpper && drawCore) h * 0.62f else if (drawCore) h * 0.94f else 0f
            val coreTop = baseY - coreH
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawCore) {
                VehicleOutline.capsule(canvas, cx, coreTop, baseY, coreW, fillPaint, strokePaint, orange, stroke)
                drawStageTanks(canvas, cx, coreTop, baseY, coreW, fuelRemain(tSec), methalox = false, lamp, alpha)
                if (!separated) {
                    for (side in floatArrayOf(-1f, 1f)) {
                        val x = cx + side * (coreW + srbW + h * 0.008f)
                        val srbTop = coreTop + h * 0.04f
                        fillPaint.color = white
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, fillPaint)
                        strokePaint.color = stroke
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, strokePaint)
                        VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                        if (tSec >= 0f && tSec < sep) {
                            drawFlame(canvas, x, baseY, srbW * 1.8f, h * 0.22f, alpha, tSec, "srb")
                        }
                    }
                }
                if (stageBurning(tSec, 1, separated, sep) && drawCore) {
                    drawFlame(canvas, cx, baseY, coreW * 1.5f, h * 0.18f, alpha, tSec, "rs25")
                }
            }
            if (drawUpper) {
                val uBot = if (drawCore) coreTop else baseY
                val uH = if (drawCore) h * 0.18f else h * 0.42f
                val uTop = uBot - uH
                fillPaint.color = dark
                canvas.drawRoundRect(cx - coreW * 0.78f, uTop, cx + coreW * 0.78f, uBot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, uTop, uBot, coreW * 0.78f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW * 0.78f, uTop, cx + coreW * 0.78f, uBot, 3f, 3f, strokePaint)
                val capH = if (drawCore) h * 0.12f else h * 0.28f
                val capTop = uTop - capH
                VehicleOutline.ogive(canvas, cx, uTop, capTop, coreW * 0.62f, fillPaint, strokePaint, white, stroke, 0.48f)
                strokePaint.strokeWidth = 2.2f
                strokePaint.color = stroke
                canvas.drawLine(cx, capTop, cx, capTop - h * 0.08f, strokePaint)
                canvas.drawLine(cx - coreW * 0.18f, capTop - h * 0.02f, cx, capTop - h * 0.08f, strokePaint)
                canvas.drawLine(cx + coreW * 0.18f, capTop - h * 0.02f, cx, capTop - h * 0.08f, strokePaint)
                if (stageBurning(tSec, 2, true, sep) && !drawCore) {
                    drawFlame(canvas, cx, uBot, coreW, h * 0.10f, alpha, tSec)
                }
            }
        }

        private fun drawSoyuzVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val gray = lampAlpha(Color.parseColor("#D2CDBE"), lamp, alpha)
            val rust = lampAlpha(Color.parseColor("#B86A28"), lamp, alpha)
            val dark = lampAlpha(Color.parseColor("#3A3A38"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.9f)
            val sep = 118f
            val coreW = h * 0.072f
            val drawBoost = stage == 1 && !separated
            val drawCore = stage == 1
            val drawUpper = stage == 2 || (stage == 1 && !separated)
            val coreH = if (drawUpper && drawCore) h * 0.50f else if (drawCore) h * 0.90f else 0f
            val coreTop = baseY - coreH
            val fuel = fuelRemain(tSec)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawBoost) {
                val bW = coreW * 0.70f
                for (side in floatArrayOf(-1f, 1f)) {
                    for (d in floatArrayOf(1.55f, 2.15f)) {
                        val x = cx + side * coreW * d
                        val tip = coreTop + coreH * 0.16f
                        val p = Path()
                        p.moveTo(x - bW, baseY)
                        p.lineTo(x - bW * 0.42f, tip)
                        p.lineTo(x + bW * 0.42f, tip)
                        p.lineTo(x + bW, baseY)
                        p.close()
                        fillPaint.color = gray
                        canvas.drawPath(p, fillPaint)
                        canvas.save()
                        canvas.clipPath(p)
                        drawStageTanks(canvas, x, tip, baseY, bW * 0.78f, fuel, methalox = false, lamp, alpha)
                        canvas.restore()
                        strokePaint.color = stroke
                        canvas.drawPath(p, strokePaint)
                        fillPaint.color = rust
                        canvas.drawRect(x - bW * 0.22f, tip, x + bW * 0.22f, tip + h * 0.018f, fillPaint)
                        if (stageBurning(tSec, 1, separated, sep)) {
                            drawFlame(canvas, x, baseY, bW * 1.15f, h * 0.10f, alpha, tSec, "rd107")
                        }
                    }
                }
            }
            if (drawCore) {
                fillPaint.color = gray
                canvas.drawRoundRect(cx - coreW, coreTop, cx + coreW, baseY, 2f, 2f, fillPaint)
                drawStageTanks(canvas, cx, coreTop, baseY, coreW, fuel, methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW, coreTop, cx + coreW, baseY, 2f, 2f, strokePaint)
                strokePaint.strokeWidth = 2.2f
                strokePaint.color = dark
                canvas.drawLine(cx - coreW * 1.35f, baseY, cx + coreW * 1.35f, baseY, strokePaint)
                canvas.drawLine(cx, baseY - coreW * 0.18f, cx, baseY + coreW * 0.55f, strokePaint)
                if (stageBurning(tSec, 1, separated, sep)) {
                    drawFlame(canvas, cx, baseY, coreW * 1.6f, h * 0.12f, alpha, tSec, "rd107")
                }
            }
            if (drawUpper) {
                val uBot = if (drawCore) coreTop else baseY
                val shroudH = if (drawCore) h * 0.28f else h * 0.62f
                val sW = coreW * 1.05f
                fillPaint.color = gray
                canvas.drawRoundRect(cx - sW * 0.72f, uBot - shroudH * 0.42f, cx + sW * 0.72f, uBot, 2f, 2f, fillPaint)
                drawStageTanks(canvas, cx, uBot - shroudH * 0.42f, uBot, sW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                strokePaint.strokeWidth = 1.5f
                canvas.drawRoundRect(cx - sW * 0.72f, uBot - shroudH * 0.42f, cx + sW * 0.72f, uBot, 2f, 2f, strokePaint)
                drawOgive(canvas, cx, uBot - shroudH * 0.42f, uBot - shroudH, sW * 0.72f, gray, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawCore) {
                    drawFlame(canvas, cx, uBot, coreW * 1.3f, h * 0.12f, alpha, tSec, "rd107")
                }
            }
        }


        private fun drawSuperHeavyGridFins(
            canvas: Canvas,
            cx: Float,
            y: Float,
            bW: Float,
            h: Float,
            deployed: Boolean,
            steel: Int,
            dark: Int,
            stroke: Int
        ) {
            val finW = if (deployed) bW * 1.15f else bW * 0.28f
            val finH = h * 0.055f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.1f
            strokePaint.color = stroke
            for (side in floatArrayOf(-1f, 1f)) {
                val left = if (side < 0f) cx - bW - finW else cx + bW
                val right = left + finW
                val top = y - finH * 0.5f
                val bot = y + finH * 0.5f
                fillPaint.color = dark
                canvas.drawRect(left, top, right, bot, fillPaint)
                canvas.drawRect(left, top, right, bot, strokePaint)
                val cols = if (deployed) 4 else 2
                val rows = 3
                for (c in 1 until cols) {
                    val x = left + finW * c / cols
                    canvas.drawLine(x, top, x, bot, strokePaint)
                }
                for (r in 1 until rows) {
                    val yy = top + finH * r / rows
                    canvas.drawLine(left, yy, right, yy, strokePaint)
                }
            }
        }

        private fun drawPlasmaSheath(
            canvas: Canvas,
            cx: Float,
            top: Float,
            bot: Float,
            halfW: Float,
            heat: Float,
            alpha: Float,
            fins: Boolean
        ) {
            if (heat < 0.04f || alpha < 0.05f || halfW < 2f) return
            val now = SystemClock.uptimeMillis() * 0.001f
            val flick = 0.82f + 0.18f * sin((now * 17.3f).toDouble()).toFloat()
            val a = (40 + 170 * heat * flick * alpha).toInt().coerceIn(0, 220)
            val midY = (top + bot) * 0.5f
            val h = (bot - top).coerceAtLeast(8f)
            fillPaint.shader = RadialGradient(
                cx, midY, halfW * 3.6f,
                intArrayOf(
                    Color.argb(a, 255, 240, 255),
                    Color.argb((a * 0.75f).toInt(), 255, 60, 180),
                    Color.argb((a * 0.45f).toInt(), 90, 50, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.35f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(cx - halfW * 3.0f, top - h * 0.08f, cx + halfW * 3.0f, bot + h * 0.16f, fillPaint)
            fillPaint.shader = null
            val wake = h * (0.28f + 0.55f * heat)
            fillPaint.shader = LinearGradient(
                cx, bot, cx, bot + wake,
                intArrayOf(
                    Color.argb((a * 0.85f).toInt(), 255, 220, 255),
                    Color.argb((a * 0.40f).toInt(), 100, 70, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(cx - halfW * 2.3f, bot - h * 0.05f, cx + halfW * 2.3f, bot + wake, fillPaint)
            fillPaint.shader = null
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeCap = Paint.Cap.ROUND
            for (i in 0 until 5) {
                val wob = sin((now * (5.1f + i * 0.4f) + i).toDouble()).toFloat()
                val x0 = cx + (i - 2f) * halfW * 0.55f
                strokePaint.strokeWidth = halfW * (0.18f + 0.12f * heat)
                strokePaint.color = if (i % 2 == 0)
                    Color.argb((a * 0.55f).toInt(), 120, 90, 255)
                else
                    Color.argb((a * 0.55f).toInt(), 255, 80, 210)
                canvas.drawLine(x0, bot, x0 + wob * halfW * 0.8f, bot + wake * (0.55f + 0.12f * i), strokePaint)
            }
            if (fins) {
                val g = (70 + 140 * heat * flick * alpha).toInt().coerceIn(0, 210)
                val fy = top + h * 0.16f
                fillPaint.shader = RadialGradient(
                    cx, fy, halfW * 2.8f,
                    intArrayOf(
                        Color.argb(g, 180, 210, 255),
                        Color.argb((g * 0.65f).toInt(), 80, 40, 255),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.48f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, fy, halfW * 2.6f, fillPaint)
                fillPaint.shader = null
            }
        }

        private fun drawStarshipVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float,
            launch: com.ccos.retro.data.LaunchSnapshot? = null
        ) {
            val steel = lampAlpha(Color.parseColor("#C4C8CC"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val dark = lampAlpha(Color.parseColor("#3A3C40"), lamp, alpha)
            val hot = lampAlpha(Color.parseColor("#FFB020"), lamp, alpha)
            val core = lampAlpha(Color.parseColor("#FFE060"), lamp, alpha)
            val sep = com.ccos.retro.event.FlightProfiles.sepTime(launch)
            val drawB = stage == 1
            val drawS = stage == 2 || (stage == 1 && !separated)
            val bH = if (drawS && drawB) h * 0.58f else if (drawB) h * 0.94f else 0f
            val sH = if (drawB && drawS) h * 0.42f else if (drawS) h * 0.94f else 0f
            val bW = h * 0.11f
            val sW = h * 0.095f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.6f
            if (drawB) {
                val top = baseY - bH
                fillPaint.color = steel
                canvas.drawRoundRect(cx - bW, top, cx + bW, baseY, 6f, 6f, fillPaint)
                drawStageTanks(canvas, cx, top, baseY, bW, fuelRemain(tSec), methalox = true, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - bW, top, cx + bW, baseY, 6f, 6f, strokePaint)
                drawSuperHeavyGridFins(canvas, cx, top + bH * 0.16f, bW, h, separated, steel, dark, stroke)
                val lit = com.ccos.retro.event.FlightProfiles.enginesLit(tSec, launch, 1, 33)
                var idx = 0
                fun ring(n: Int, rad: Float, rDot: Float) {
                    for (i in 0 until n) {
                        val a = Math.toRadians(-90.0 + i * (360.0 / n))
                        val x = cx + (rad * cos(a)).toFloat()
                        val y = baseY - h * 0.012f + (rad * 0.18f * sin(a)).toFloat()
                        val on = idx < lit
                        fillPaint.color = if (on) hot else dark
                        canvas.drawCircle(x, y, rDot, fillPaint)
                        if (on) {
                            fillPaint.color = core
                            canvas.drawCircle(x, y, rDot * 0.42f, fillPaint)
                        }
                        idx++
                    }
                }
                ring(3, bW * 0.22f, h * 0.0072f)
                ring(10, bW * 0.48f, h * 0.0062f)
                ring(20, bW * 0.72f, h * 0.0054f)
                if (stageBurning(tSec, 1, separated, sep)) {
                    drawFlame(canvas, cx, baseY, bW * 1.6f, h * 0.18f, alpha, tSec, "raptor")
                }
            }
            if (drawS) {
                val bot = if (drawB) baseY - bH else baseY
                val top = bot - sH
                val sHeat = com.ccos.retro.event.FlightProfiles.plasmaHeat(launch, tSec, 2)
                if (sHeat > 0.04f) {
                    drawPlasmaSheath(canvas, cx, top, bot, sW, sHeat, alpha, false)
                }
                fillPaint.color = steel
                canvas.drawRoundRect(cx - sW, top + sH * 0.12f, cx + sW, bot, 5f, 5f, fillPaint)
                drawStageTanks(canvas, cx, top + sH * 0.12f, bot, sW, fuelRemain(tSec, 2), methalox = true, lamp, alpha)
                if (sHeat > 0.08f) {
                    val tileA = (70 + 140 * sHeat * alpha).toInt().coerceIn(0, 210)
                    fillPaint.color = Color.argb(tileA, 255, (70 + 150 * sHeat).toInt().coerceIn(0, 255), 36)
                    canvas.drawRect(cx - sW, top + sH * 0.14f, cx - sW * 0.08f, bot, fillPaint)
                }
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - sW, top + sH * 0.12f, cx + sW, bot, 5f, 5f, strokePaint)
                drawOgive(canvas, cx, top + sH * 0.12f, top, sW, steel, stroke)
                val now = SystemClock.uptimeMillis() * 0.001f
                val hunt = if (sHeat > 0.04f) 0.12f * sin((now * 1.7f).toDouble()).toFloat() else 0f
                fillPaint.color = steel
                canvas.drawRect(cx - sW * (1.55f + hunt), bot - sH * 0.22f, cx - sW, bot - sH * 0.08f, fillPaint)
                canvas.drawRect(cx + sW, bot - sH * 0.22f, cx + sW * (1.55f + hunt), bot - sH * 0.08f, fillPaint)
                canvas.drawRect(cx - sW * (1.35f + hunt * 0.6f), top + sH * 0.18f, cx - sW, top + sH * 0.32f, fillPaint)
                canvas.drawRect(cx + sW, top + sH * 0.18f, cx + sW * (1.35f + hunt * 0.6f), top + sH * 0.32f, fillPaint)
                if (sHeat > 0.10f) {
                    val g = (50 + 130 * sHeat * alpha).toInt().coerceIn(0, 190)
                    fillPaint.color = Color.argb(g, 140, 90, 255)
                    canvas.drawCircle(cx - sW * 1.15f, bot - sH * 0.15f, sW * 0.55f, fillPaint)
                    canvas.drawCircle(cx + sW * 1.15f, bot - sH * 0.15f, sW * 0.55f, fillPaint)
                    canvas.drawCircle(cx - sW * 1.05f, top + sH * 0.25f, sW * 0.42f, fillPaint)
                    canvas.drawCircle(cx + sW * 1.05f, top + sH * 0.25f, sW * 0.42f, fillPaint)
                }
                if (stageBurning(tSec, 2, true, sep) && !drawB) {
                    drawFlame(canvas, cx, bot, sW * 1.2f, h * 0.14f, alpha, tSec, "raptor")
                }
            }
        }

        private fun drawElectronVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val carbon = lampAlpha(Color.parseColor("#1C1C1C"), lamp, alpha)
            val stroke = lampAlpha(Color.parseColor("#8A8A8A"), lamp, alpha)
            val sep = 162f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val w = h * 0.038f
            val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.3f
            if (drawS1) {
                fillPaint.color = carbon
                canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 2f, 2f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, w, fuelRemain(tSec), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 2f, 2f, strokePaint)
                fillPaint.color = lampAlpha(Color.parseColor("#2A2A2A"), lamp, alpha)
                for (i in 0 until 9) {
                    val a = Math.toRadians(-90.0 + i * 40.0)
                    val rr = if (i == 0) 0f else w * 0.7f
                    canvas.drawCircle(cx + (rr * cos(a)).toFloat(), baseY, w * 0.22f, fillPaint)
                }
                if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY, w * 2.2f, h * 0.12f, alpha, tSec)
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.26f else h * 0.62f
                fillPaint.color = carbon
                canvas.drawRoundRect(cx - w * 0.78f, bot - s2H, cx + w * 0.78f, bot, 2f, 2f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, w * 0.78f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - w * 0.78f, bot - s2H, cx + w * 0.78f, bot, 2f, 2f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.16f else h * 0.32f), w * 0.78f, carbon, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, w * 1.6f, h * 0.10f, alpha, tSec)
            }
        }

        private fun drawGlennVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val navy = lampAlpha(Color.parseColor("#101820"), lamp, alpha)
            val stroke = lampAlpha(Color.parseColor("#6A88AA"), lamp, alpha)
            val sep = 180f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val w = h * 0.10f
            val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawS1) {
                fillPaint.color = navy
                canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, w, fuelRemain(tSec), methalox = true, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, strokePaint)
                fillPaint.color = lampAlpha(Color.parseColor("#2A3038"), lamp, alpha)
                canvas.drawCircle(cx, baseY - h * 0.01f, w * 0.16f, fillPaint)
                for (i in 0 until 6) {
                    val a = Math.toRadians(-90.0 + i * 60.0)
                    val rr = w * 0.52f
                    canvas.drawCircle(cx + (rr * cos(a)).toFloat(), baseY - h * 0.01f, w * 0.14f, fillPaint)
                }
                if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY, w * 1.3f, h * 0.13f, alpha, tSec)
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.22f else h * 0.50f
                fillPaint.color = navy
                canvas.drawRoundRect(cx - w * 0.85f, bot - s2H, cx + w * 0.85f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, w * 0.85f, fuelRemain(tSec, 2), methalox = true, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - w * 0.85f, bot - s2H, cx + w * 0.85f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.44f), w * 0.85f, navy, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, w, h * 0.10f, alpha, tSec)
            }
        }

        private fun drawArianeVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
            val black = lampAlpha(Color.parseColor("#222222"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = 137f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val coreW = h * 0.07f
            val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawS1) {
                fillPaint.color = black
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuelRemain(tSec), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
                if (!separated) {
                    for (side in floatArrayOf(-1f, 1f)) {
                        val x = cx + side * (coreW + h * 0.042f)
                        val bw = h * 0.032f
                        fillPaint.color = white
                        canvas.drawRoundRect(x - bw, baseY - s1H * 0.82f, x + bw, baseY, 3f, 3f, fillPaint)
                        drawStageTanks(canvas, x, baseY - s1H * 0.82f, baseY, bw, fuelRemain(tSec), methalox = false, lamp, alpha)
                        strokePaint.color = stroke
                        canvas.drawRoundRect(x - bw, baseY - s1H * 0.82f, x + bw, baseY, 3f, 3f, strokePaint)
                        drawVehicleBell(canvas, x, baseY, bw * 0.95f, h * 0.042f, lamp, alpha, copper = false)
                        if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY + h * 0.04f, bw * 1.8f, h * 0.12f, alpha, tSec, "srb")
                    }
                }
                drawVehicleBell(canvas, cx, baseY, coreW * 0.62f, h * 0.050f, lamp, alpha, copper = true)
                if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY + h * 0.048f, coreW * 1.5f, h * 0.14f, alpha, tSec, "rs25")
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.22f else h * 0.50f
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.44f), coreW * 0.72f, white, stroke)
                drawVehicleBell(canvas, cx, bot, coreW * 0.48f, h * 0.055f, lamp, alpha, copper = true)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot + h * 0.05f, coreW * 1.2f, h * 0.12f, alpha, tSec, "rs25")
            }
        }

        private fun drawLmVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            wide: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val white = lampAlpha(Color.parseColor("#D8DCE0"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = 155f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val coreW = h * if (wide) 0.10f else 0.07f
            val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.92f else 0f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawS1) {
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuelRemain(tSec), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
                if (!separated) {
                    val nBoost = if (wide) 4 else 2
                    for (i in 0 until nBoost) {
                        val side = if (i < nBoost / 2) -1f else 1f
                        val k = if (nBoost == 4) (0.55f + (i % 2) * 0.55f) else 1.05f
                        val x = cx + side * coreW * (1.15f + k * 0.15f)
                        val bw = coreW * 0.38f
                        fillPaint.color = white
                        canvas.drawRoundRect(x - bw, baseY - s1H * 0.78f, x + bw, baseY, 3f, 3f, fillPaint)
                        drawStageTanks(canvas, x, baseY - s1H * 0.78f, baseY, bw, fuelRemain(tSec), methalox = false, lamp, alpha)
                        strokePaint.color = stroke
                        canvas.drawRoundRect(x - bw, baseY - s1H * 0.78f, x + bw, baseY, 3f, 3f, strokePaint)
                        drawVehicleBell(canvas, x - bw * 0.42f, baseY, bw * 0.48f, h * 0.036f, lamp, alpha, copper = false)
                        drawVehicleBell(canvas, x + bw * 0.42f, baseY, bw * 0.48f, h * 0.036f, lamp, alpha, copper = false)
                        if (tSec >= 0f && tSec < sep) {
                            drawFlame(canvas, x - bw * 0.42f, baseY + h * 0.034f, bw * 0.9f, h * 0.09f, alpha, tSec, "merlin")
                            drawFlame(canvas, x + bw * 0.42f, baseY + h * 0.034f, bw * 0.9f, h * 0.09f, alpha, tSec, "merlin")
                        }
                    }
                }
                drawVehicleBell(canvas, cx - coreW * 0.36f, baseY, coreW * 0.28f, h * 0.040f, lamp, alpha, copper = true)
                drawVehicleBell(canvas, cx + coreW * 0.36f, baseY, coreW * 0.28f, h * 0.040f, lamp, alpha, copper = true)
                if (stageBurning(tSec, 1, separated, sep)) {
                    drawFlame(canvas, cx - coreW * 0.36f, baseY + h * 0.038f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
                    drawFlame(canvas, cx + coreW * 0.36f, baseY + h * 0.038f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
                }
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.24f else h * 0.55f
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.40f), coreW * 0.72f, white, stroke)
                drawVehicleBell(canvas, cx, bot, coreW * 0.40f, h * 0.048f, lamp, alpha, copper = true)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot + h * 0.046f, coreW, h * 0.11f, alpha, tSec, "rs25")
            }
        }


        private fun drawH3Vehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val white = lampAlpha(Color.parseColor("#E8ECF0"), lamp, alpha)
            val orange = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = 130f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val coreW = h * 0.078f
            val srbW = h * 0.042f
            val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
            val fuel = fuelRemain(tSec)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawS1) {
                VehicleOutline.capsule(canvas, cx, baseY - s1H, baseY, coreW, fillPaint, strokePaint, white, stroke)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = false, lamp, alpha)
                if (!separated) {
                    for (side in floatArrayOf(-1f, 1f)) {
                        val x = cx + side * (coreW + srbW + h * 0.008f)
                        val srbTop = baseY - s1H * 0.78f
                        fillPaint.color = white
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, fillPaint)
                        strokePaint.color = stroke
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, strokePaint)
                        fillPaint.color = orange
                        canvas.drawRect(x - srbW, srbTop + (baseY - srbTop) * 0.35f, x + srbW, srbTop + (baseY - srbTop) * 0.42f, fillPaint)
                        VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                        if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY, srbW * 1.7f, h * 0.12f, alpha, tSec, "srb")
                    }
                }
                drawVehicleBell(canvas, cx - coreW * 0.38f, baseY, coreW * 0.32f, h * 0.042f, lamp, alpha, copper = true)
                drawVehicleBell(canvas, cx + coreW * 0.38f, baseY, coreW * 0.32f, h * 0.042f, lamp, alpha, copper = true)
                if (stageBurning(tSec, 1, separated, sep)) {
                    drawFlame(canvas, cx - coreW * 0.38f, baseY + h * 0.04f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
                    drawFlame(canvas, cx + coreW * 0.38f, baseY + h * 0.04f, coreW * 0.7f, h * 0.11f, alpha, tSec, "rs25")
                }
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.22f else h * 0.50f
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW * 0.72f, bot - s2H, cx + coreW * 0.72f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.44f), coreW * 0.72f, white, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, coreW, h * 0.11f, alpha, tSec, "rs25")
            }
        }

        private fun drawIsroVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val white = lampAlpha(Color.parseColor("#F0EDE4"), lamp, alpha)
            val band = lampAlpha(Color.parseColor("#C45C26"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = 150f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val coreW = h * 0.070f
            val srbW = h * 0.058f
            val s1H = if (drawS2 && drawS1) h * 0.56f else if (drawS1) h * 0.92f else 0f
            val fuel = fuelRemain(tSec)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawS1) {
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
                if (!separated) {
                    for (side in floatArrayOf(-1f, 1f)) {
                        val x = cx + side * (coreW + srbW + h * 0.006f)
                        val srbTop = baseY - s1H * 0.82f
                        fillPaint.color = white
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, fillPaint)
                        strokePaint.color = stroke
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 4f, 4f, strokePaint)
                        fillPaint.color = band
                        canvas.drawRect(x - srbW, srbTop + (baseY - srbTop) * 0.40f, x + srbW, srbTop + (baseY - srbTop) * 0.48f, fillPaint)
                        VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                        if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY, srbW * 1.6f, h * 0.14f, alpha, tSec, "srb")
                    }
                }
                drawVehicleBell(canvas, cx - coreW * 0.36f, baseY, coreW * 0.30f, h * 0.038f, lamp, alpha, copper = false)
                drawVehicleBell(canvas, cx + coreW * 0.36f, baseY, coreW * 0.30f, h * 0.038f, lamp, alpha, copper = false)
                if (stageBurning(tSec, 1, separated, sep)) {
                    drawFlame(canvas, cx - coreW * 0.36f, baseY + h * 0.036f, coreW * 0.65f, h * 0.10f, alpha, tSec, "merlin")
                    drawFlame(canvas, cx + coreW * 0.36f, baseY + h * 0.036f, coreW * 0.65f, h * 0.10f, alpha, tSec, "merlin")
                }
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.24f else h * 0.55f
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW * 0.78f, bot - s2H, cx + coreW * 0.78f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.78f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW * 0.78f, bot - s2H, cx + coreW * 0.78f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), coreW * 0.78f, white, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, coreW, h * 0.11f, alpha, tSec, "rs25")
            }
        }

        private fun drawVulcanVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val dark = lampAlpha(Color.parseColor("#2A3038"), lamp, alpha)
            val white = lampAlpha(Color.parseColor("#E6E6E8"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.85f)
            val sep = 140f
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val coreW = h * 0.080f
            val srbW = h * 0.036f
            val s1H = if (drawS2 && drawS1) h * 0.58f else if (drawS1) h * 0.92f else 0f
            val fuel = fuelRemain(tSec)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.5f
            if (drawS1) {
                fillPaint.color = dark
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, coreW, fuel, methalox = true, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW, baseY - s1H, cx + coreW, baseY, 3f, 3f, strokePaint)
                if (!separated) {
                    for (side in floatArrayOf(-1f, 1f)) {
                        val x = cx + side * (coreW + srbW + h * 0.006f)
                        val srbTop = baseY - s1H * 0.72f
                        fillPaint.color = white
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, fillPaint)
                        strokePaint.color = stroke
                        canvas.drawRoundRect(x - srbW, srbTop, x + srbW, baseY, 3f, 3f, strokePaint)
                        VehicleOutline.srbForward(canvas, x, srbTop, srbW, fillPaint, strokePaint, white, stroke)
                        if (tSec >= 0f && tSec < sep) drawFlame(canvas, x, baseY, srbW * 1.6f, h * 0.10f, alpha, tSec, "srb")
                    }
                }
                drawVehicleBell(canvas, cx - coreW * 0.40f, baseY, coreW * 0.34f, h * 0.048f, lamp, alpha, copper = false)
                drawVehicleBell(canvas, cx + coreW * 0.40f, baseY, coreW * 0.34f, h * 0.048f, lamp, alpha, copper = false)
                if (stageBurning(tSec, 1, separated, sep)) {
                    drawFlame(canvas, cx - coreW * 0.40f, baseY + h * 0.046f, coreW * 0.75f, h * 0.12f, alpha, tSec, "raptor")
                    drawFlame(canvas, cx + coreW * 0.40f, baseY + h * 0.046f, coreW * 0.75f, h * 0.12f, alpha, tSec, "raptor")
                }
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.22f else h * 0.52f
                fillPaint.color = white
                canvas.drawRoundRect(cx - coreW * 0.70f, bot - s2H, cx + coreW * 0.70f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, coreW * 0.70f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - coreW * 0.70f, bot - s2H, cx + coreW * 0.70f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), coreW * 0.70f, white, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, coreW, h * 0.11f, alpha, tSec, "rs25")
            }
        }

        private fun drawGenericVehicle(
            canvas: Canvas,
            cx: Float,
            baseY: Float,
            h: Float,
            tSec: Float,
            stage: Int,
            separated: Boolean,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val body = lampAlpha(Color.parseColor("#C8D0D6"), lamp, alpha)
            val stroke = lampAlpha(skin.accent, lamp, alpha * 0.9f)
            val sep = sepTime(telemetryModule.tracked ?: return)
            val drawS1 = stage == 1
            val drawS2 = stage == 2 || (stage == 1 && !separated)
            val w = h * 0.075f
            val s1H = if (drawS2 && drawS1) h * 0.55f else if (drawS1) h * 0.92f else 0f
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.6f
            if (drawS1) {
                fillPaint.color = body
                canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, fillPaint)
                drawStageTanks(canvas, cx, baseY - s1H, baseY, w, fuelRemain(tSec), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - w, baseY - s1H, cx + w, baseY, 4f, 4f, strokePaint)
                val fin = Path()
                fin.moveTo(cx - w, baseY - s1H * 0.22f)
                fin.lineTo(cx - w * 1.7f, baseY)
                fin.lineTo(cx - w, baseY)
                fin.close()
                canvas.drawPath(fin, fillPaint)
                canvas.drawPath(fin, strokePaint)
                fin.reset()
                fin.moveTo(cx + w, baseY - s1H * 0.22f)
                fin.lineTo(cx + w * 1.7f, baseY)
                fin.lineTo(cx + w, baseY)
                fin.close()
                canvas.drawPath(fin, fillPaint)
                canvas.drawPath(fin, strokePaint)
                if (stageBurning(tSec, 1, separated, sep)) drawFlame(canvas, cx, baseY, w * 1.5f, h * 0.13f, alpha, tSec)
            }
            if (drawS2) {
                val bot = if (drawS1) baseY - s1H else baseY
                val s2H = if (drawS1) h * 0.25f else h * 0.58f
                fillPaint.color = body
                canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, fillPaint)
                drawStageTanks(canvas, cx, bot - s2H, bot, w * 0.72f, fuelRemain(tSec, 2), methalox = false, lamp, alpha)
                strokePaint.color = stroke
                canvas.drawRoundRect(cx - w * 0.72f, bot - s2H, cx + w * 0.72f, bot, 3f, 3f, strokePaint)
                drawOgive(canvas, cx, bot - s2H, bot - s2H - (if (drawS1) h * 0.20f else h * 0.42f), w * 0.72f, body, stroke)
                if (stageBurning(tSec, 2, true, sep) && !drawS1) drawFlame(canvas, cx, bot, w, h * 0.10f, alpha, tSec)
            }
        }

        private fun drawLaunchLandmass(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            size: Float,
            launch: com.ccos.retro.data.LaunchSnapshot,
            tSec: Float,
            altKm: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float,
            alpha: Float
        ) {
            val west = "vandenberg" in launch.location.lowercase() || "vafb" in launch.pad.lowercase()
            val ocean = Color.argb((210 * alpha).toInt(), 10, 28, 52)
            val land = Color.argb((220 * alpha).toInt(), 28, 52, 32)
            val left = cx - size
            val top = cy - size * 0.72f
            val right = cx + size
            val bot = cy + size * 0.72f
            fillPaint.color = ocean
            canvas.drawRoundRect(left, top, right, bot, 16f, 16f, fillPaint)
            val coast = Path()
            if (west) {
                coast.moveTo(cx + size * 0.05f, bot)
                coast.lineTo(cx + size * 0.18f, cy + size * 0.2f)
                coast.lineTo(cx + size * 0.08f, cy - size * 0.15f)
                coast.lineTo(cx + size * 0.35f, top)
                coast.lineTo(right, top)
                coast.lineTo(right, bot)
                coast.close()
            } else {
                coast.moveTo(left, bot)
                coast.lineTo(left, top)
                coast.lineTo(cx - size * 0.15f, top)
                coast.lineTo(cx - size * 0.05f, cy - size * 0.05f)
                coast.lineTo(cx + size * 0.12f, cy + size * 0.18f)
                coast.lineTo(cx - size * 0.02f, bot)
                coast.close()
            }
            fillPaint.color = land
            canvas.drawPath(coast, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 6f
            strokePaint.color = withLamp(skin.accent, lamp * alpha)
            canvas.drawRoundRect(left, top, right, bot, 16f, 16f, strokePaint)
            strokePaint.strokeWidth = 5f
            canvas.drawPath(coast, strokePaint)

            val padX = if (west) cx + size * 0.02f else cx + size * 0.02f
            val padY = cy + size * 0.28f
            fillPaint.color = withLamp(skin.go, lamp * alpha)
            canvas.drawRect(padX - 10f, padY - 10f, padX + 10f, padY + 10f, fillPaint)
            val padName = launch.pad.take(12).ifBlank { "PAD" }
            hudPaint.color = withLamp(skin.text, lamp * alpha)
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.textSize = telFit(padName, size * 1.2f, size * 0.14f, 14f)
            canvas.drawText(padName, cx, top + size * 0.16f, hudPaint)

            val trail = Path()
            val steps = 40
            val range = (altKm.coerceAtLeast(0.5f) / 140f).coerceIn(0.04f, 1f)
            for (i in 0..steps) {
                val u = i / steps.toFloat()
                val x = padX + (if (west) -1f else 1f) * size * 0.72f * u
                val y = padY - size * 0.85f * (4f * u * (1f - u) * 0.85f + u * 0.2f) * range
                if (i == 0) trail.moveTo(x, y) else trail.lineTo(x, y)
            }
            strokePaint.strokeWidth = 7f
            strokePaint.color = withLamp(skin.accent, lamp * alpha)
            canvas.drawPath(trail, strokePaint)
            val prog = ((tSec + 2f) / 180f).coerceIn(0.02f, 0.95f) * range
            val vx = padX + (if (west) -1f else 1f) * size * 0.72f * prog
            val vy = padY - size * 0.85f * (4f * prog * (1f - prog) * 0.85f + prog * 0.2f)
            drawVesselChevron(canvas, vx, vy, if (west) -0.7f else 0.7f, skin, lamp * alpha)
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(skin.text, lamp * alpha)
            hudPaint.textSize = telFit("DOWNRANGE", size * 1.1f, size * 0.14f, 14f)
            canvas.drawText("DOWNRANGE", cx, bot - size * 0.08f, hudPaint)
        }


        /**
         * Bottom strip on TEL only — analog/digital and units.
         * Lamp, text, and agency skin live on CMD. No color picker here.
         */
        private fun drawTelControlStrip(canvas: Canvas, skin: TelemetrySkin.Tokens, lamp: Float) {
            val top = height * 0.88f
            val bottom = height - 16f
            val left = width * 0.06f
            val right = width * 0.94f
            fillPaint.color = withLamp(skin.panel, lamp)
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3.5f
            strokePaint.color = withLamp(skin.accent, lamp * 0.7f)
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, strokePaint)

            val pad = 12f
            val chipH = (bottom - top - pad * 2f).coerceIn(40f, 72f)
            val row1 = top + (bottom - top - chipH) / 2f
            val half = (right - left - 28f) / 2f
            telStripAnalog.set(left + 10f, row1, left + 10f + half - 4f, row1 + chipH)
            telStripUnits.set(left + 10f + half + 4f, row1, right - 10f, row1 + chipH)
            telStripColorRects.clear()
            fun chip(r: RectF, on: Boolean, label: String) {
                fillPaint.color = withLamp(if (on) skin.btnActiveFill else skin.btnIdleFill, lamp)
                canvas.drawRoundRect(r, 8f, 8f, fillPaint)
                strokePaint.strokeWidth = 3.5f
                strokePaint.color = withLamp(if (on) skin.accent else skin.muted, lamp)
                canvas.drawRoundRect(r, 8f, 8f, strokePaint)
                hudPaint.color = Color.WHITE
                hudPaint.textSize = r.height() * 0.38f
                hudPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(label, r.centerX(), r.centerY() + r.height() * 0.14f, hudPaint)
            }
            chip(telStripAnalog, prefs.telemetryAnalog, if (prefs.telemetryAnalog) "ANALOG" else "DIGITAL")
            chip(telStripUnits, prefs.useImperial, if (prefs.useImperial) "MPH / MI" else "KM/H")
        }

        /**
         * Phone system metrics (BAT / CPU / RAM / STOR) flanking the ADI on TEL.
         * Same live data as System Metrics module — makes TEL feel busy without
         * colliding with flight readouts.
         */
        private fun drawPhoneMetricsAroundAdi(
            canvas: Canvas,
            attCx: Float,
            attCy: Float,
            attR: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val snap = try { metrics.sample(avgFrameMs) } catch (_: Exception) { return }
            // Scale gauges with HUD text so max slider = max readable size in the gap
            val scale01 = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            val r = (width * (0.115f + scale01 * 0.08f)).coerceIn(68f, 150f)
            val gap = attR + r + width * 0.035f
            val leftCx = (attCx - gap).coerceAtLeast(r + 8f)
            val rightCx = (attCx + gap).coerceAtMost(width - r - 8f)
            // BAT / RAM on the ADI mid-line; CPU / STOR stacked directly underneath
            val topCy = attCy - r * 0.15f
            val botCy = topCy + r * 1.65f
            val strokeW = 8f + scale01 * 6f
            val labelSz = 14f + scale01 * 14f
            val valueSz = 18f + scale01 * 18f
            val style = skin.btnStyle
            val themePal = try {
                com.ccos.retro.skin.SystemSkin.palette(prefs.systemTheme, lamp)
            } catch (_: Exception) { null }

            fun accentFor(kind: String, fallback: Int): Int {
                // Color picker always drives phone gauge accents (including red / theme 1)
                if (themePal != null) {
                    return when (kind) {
                        "bat" -> themePal.primary
                        "cpu" -> themePal.accent
                        "ram" -> themePal.primary
                        else -> themePal.accent
                    }
                }
                return fallback
            }

            val batFrac = (snap.batteryPercent / 100f).coerceIn(0f, 1f)
            val cpuFrac = (snap.cpuPercent / 100f).coerceIn(0f, 1f)
            val memFrac = if (snap.maxMemMb > 0) (snap.usedMemMb.toFloat() / snap.maxMemMb).coerceIn(0.05f, 1f) else 0.3f
            val storFrac = snap.internalStorageFrac.coerceIn(0f, 1f)
            val batAccent = accentFor("bat", when {
                snap.isCharging -> skin.go
                snap.batteryPercent < 20 -> skin.danger
                else -> skin.accent
            })

            // DIGITAL mode — boxed numeric readouts at the same four stations (all agencies)
            if (!prefs.telemetryAnalog) {
                fun digi(cx: Float, cy: Float, label: String, value: String, accent: Int) {
                    val bw = r * 1.55f
                    val bh = r * 0.95f
                    fillPaint.color = Color.argb(200, 8, 10, 14)
                    canvas.drawRoundRect(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2, 10f, 10f, fillPaint)
                    strokePaint.style = Paint.Style.STROKE
                    strokePaint.strokeWidth = 2.5f
                    strokePaint.color = withLamp(accent, lamp)
                    canvas.drawRoundRect(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2, 10f, 10f, strokePaint)
                    // Top accent bar proportional to value
                    val barFrac = when {
                        label.contains("CPU") || label.contains("ЦП") -> cpuFrac
                        label.contains("STOR") || label.contains("存储") || label.contains("ДИСК") -> storFrac
                        label.contains("RAM") || label.contains("内存") || label.contains("ОЗУ") -> memFrac
                        else -> batFrac
                    }
                    fillPaint.color = withLamp(accent, lamp)
                    val barH = 5f
                    val barW = (bw - 16f) * barFrac
                    canvas.drawRoundRect(cx - bw / 2 + 8f, cy - bh / 2 + 8f, cx - bw / 2 + 8f + barW, cy - bh / 2 + 8f + barH, 2f, 2f, fillPaint)
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.color = withLamp(skin.muted, lamp)
                    hudPaint.textSize = labelSz * 0.85f
                    canvas.drawText(label, cx, cy - bh * 0.12f, hudPaint)
                    hudPaint.color = withLamp(skin.text, lamp)
                    hudPaint.textSize = valueSz * 1.15f
                    canvas.drawText(value, cx, cy + bh * 0.28f, hudPaint)
                }
                when (style) {
                    TelemetrySkin.ButtonStyle.ROSCOSMOS -> {
                        digi(leftCx, topCy, if (snap.isCharging) "БАТ ⚡" else "БАТ", "${snap.batteryPercent}%", batAccent)
                        digi(leftCx, botCy, "ЦП", "${snap.cpuPercent}%", accentFor("cpu", skin.hold))
                        digi(rightCx, topCy, "ОЗУ", "${snap.usedMemMb}M", accentFor("ram", skin.accent))
                        digi(rightCx, botCy, "ДИСК", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go))
                    }
                    TelemetrySkin.ButtonStyle.CASC -> {
                        digi(leftCx, topCy, if (snap.isCharging) "电 ⚡" else "电", "${snap.batteryPercent}%", batAccent)
                        digi(leftCx, botCy, "CPU", "${snap.cpuPercent}%", accentFor("cpu", skin.hold))
                        digi(rightCx, topCy, "内存", "${snap.usedMemMb}M", accentFor("ram", skin.accent))
                        digi(rightCx, botCy, "存储", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go))
                    }
                    else -> {
                        digi(leftCx, topCy, if (snap.isCharging) "BAT ⚡" else "BAT", "${snap.batteryPercent}%", batAccent)
                        digi(leftCx, botCy, "CPU", "${snap.cpuPercent}%", accentFor("cpu", skin.hold))
                        digi(rightCx, topCy, "RAM", "${snap.usedMemMb}M", accentFor("ram", skin.accent))
                        digi(rightCx, botCy, "STOR", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go))
                    }
                }
                return
            }

            fun arcPhone(cx: Float, cy: Float, value01: Float, label: String, valueText: String, accent: Int, startAng: Float, sweepMax: Float) {
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = strokeW
                strokePaint.strokeCap = Paint.Cap.ROUND
                strokePaint.color = withLamp(skin.grid, lamp)
                canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAng, sweepMax, false, strokePaint)
                strokePaint.color = withLamp(accent, lamp)
                val sweep = sweepMax * value01.coerceIn(0f, 1f)
                canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAng, sweep, false, strokePaint)
                val ang = Math.toRadians((startAng + sweep).toDouble())
                val nx = cx + (r * 0.78f * cos(ang)).toFloat()
                val ny = cy + (r * 0.78f * sin(ang)).toFloat()
                strokePaint.strokeWidth = 2.5f
                canvas.drawLine(cx, cy, nx, ny, strokePaint)
                fillPaint.color = withLamp(accent, lamp)
                canvas.drawCircle(cx, cy, 4f, fillPaint)
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = valueSz
                canvas.drawText(valueText, cx, cy + r * 0.12f, hudPaint)
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = labelSz
                canvas.drawText(label, cx, cy + r * 0.52f, hudPaint)
            }

            fun barPhone(cx: Float, cy: Float, value01: Float, label: String, valueText: String, accent: Int) {
                val bw = r * 1.6f
                val bh = r * 0.28f
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = 2f
                strokePaint.color = withLamp(skin.grid, lamp)
                canvas.drawRoundRect(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2, 4f, 4f, strokePaint)
                fillPaint.color = withLamp(accent, lamp)
                val fw = bw * value01.coerceIn(0f, 1f)
                canvas.drawRoundRect(cx - bw / 2, cy - bh / 2, cx - bw / 2 + fw, cy + bh / 2, 4f, 4f, fillPaint)
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = valueSz
                canvas.drawText(valueText, cx, cy - bh - 6f, hudPaint)
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = labelSz
                canvas.drawText(label, cx, cy + bh + 14f, hudPaint)
            }

            when (style) {
                TelemetrySkin.ButtonStyle.ROSCOSMOS -> {
                    // Vertical gold bars (Soviet panel strip)
                    barPhone(leftCx, topCy, batFrac, if (snap.isCharging) "БАТ ⚡" else "БАТ", "${snap.batteryPercent}%", batAccent)
                    barPhone(leftCx, botCy, cpuFrac, "ЦП", "${snap.cpuPercent}%", accentFor("cpu", skin.hold))
                    barPhone(rightCx, topCy, memFrac, "ОЗУ", "${snap.usedMemMb}M", accentFor("ram", skin.accent))
                    barPhone(rightCx, botCy, storFrac, "ДИСК", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go))
                }
                TelemetrySkin.ButtonStyle.NASA -> {
                    // Full 360° rings (mission control)
                    arcPhone(leftCx, topCy, batFrac, "BAT", "${snap.batteryPercent}%", batAccent, -90f, 360f)
                    arcPhone(leftCx, botCy, cpuFrac, "CPU", "${snap.cpuPercent}%", accentFor("cpu", skin.hold), -90f, 360f)
                    arcPhone(rightCx, topCy, memFrac, "RAM", "${snap.usedMemMb}M", accentFor("ram", skin.accent), -90f, 360f)
                    arcPhone(rightCx, botCy, storFrac, "STOR", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go), -90f, 360f)
                }
                TelemetrySkin.ButtonStyle.CASC -> {
                    // Square frames with partial arc
                    fun square(cx: Float, cy: Float, v: Float, lab: String, vt: String, acc: Int) {
                        strokePaint.style = Paint.Style.STROKE
                        strokePaint.strokeWidth = 2f
                        strokePaint.color = withLamp(skin.accent, lamp)
                        canvas.drawRect(cx - r, cy - r, cx + r, cy + r, strokePaint)
                        arcPhone(cx, cy, v, lab, vt, acc, 180f, 180f)
                    }
                    square(leftCx, topCy, batFrac, "电", "${snap.batteryPercent}%", batAccent)
                    square(leftCx, botCy, cpuFrac, "CPU", "${snap.cpuPercent}%", accentFor("cpu", skin.hold))
                    square(rightCx, topCy, memFrac, "内存", "${snap.usedMemMb}M", accentFor("ram", skin.accent))
                    square(rightCx, botCy, storFrac, "存储", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go))
                }
                else -> {
                    // SpaceX / default — open arcs 260°
                    arcPhone(leftCx, topCy, batFrac, if (snap.isCharging) "BAT ⚡" else "BAT", "${snap.batteryPercent}%", batAccent, 140f, 260f)
                    arcPhone(leftCx, botCy, cpuFrac, "CPU", "${snap.cpuPercent}%", accentFor("cpu", skin.hold), 140f, 260f)
                    arcPhone(rightCx, topCy, memFrac, "RAM", "${snap.usedMemMb}M", accentFor("ram", skin.accent), 140f, 260f)
                    arcPhone(rightCx, botCy, storFrac, "STOR", "${(storFrac * 100).toInt()}%", accentFor("stor", skin.go), 140f, 260f)
                }
            }
            strokePaint.strokeCap = Paint.Cap.ROUND
        }

        /** Optional small flight analog arcs under trajectory when ANALOG is on. */
        private fun drawAnalogTelemetryGauges(
            canvas: Canvas,
            altKm: Float,
            speedKmh: Float,
            pitchDeg: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val scale01 = ((prefs.textScale - 2.8f) / 6.2f).coerceIn(0f, 1f)
            val r = width * (0.045f + scale01 * 0.02f)
            val cy = height * 0.695f
            val strokeW = 4f + scale01 * 3f
            fun arcGauge(cx: Float, value01: Float, label: String, accent: Int) {
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = strokeW
                strokePaint.strokeCap = Paint.Cap.ROUND
                strokePaint.color = withLamp(skin.grid, lamp)
                canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 180f, 180f, false, strokePaint)
                strokePaint.color = withLamp(accent, lamp)
                canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 180f, 180f * value01.coerceIn(0f, 1f), false, strokePaint)
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = 10f + scale01 * 4f
                hudPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(label, cx, cy + 4f, hudPaint)
            }
            val (altLo, altHi) = analogAltWindow(if (prefs.useImperial) altKm * 0.621371f else altKm, prefs.useImperial)
            val spdDisp = if (prefs.useImperial) speedKmh * 0.621371f else speedKmh
            val (spdLo, spdHi) = analogSpeedWindow(spdDisp, prefs.useImperial)
            val altFrac = (( (if (prefs.useImperial) altKm * 0.621371f else altKm) - altLo) / (altHi - altLo).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
            val spdFrac = ((spdDisp - spdLo) / (spdHi - spdLo).coerceAtLeast(1f)).coerceIn(0f, 1f)
            arcGauge(width * 0.22f, altFrac, analogRangeText(altLo, altHi, if (prefs.useImperial) "mi" else "km"), skin.accent)
            arcGauge(width * 0.50f, spdFrac, analogRangeText(spdLo, spdHi, if (prefs.useImperial) "mph" else "km/h"), skin.go)
            arcGauge(width * 0.78f, ((90f - pitchDeg) / 90f).coerceIn(0f, 1f), "PITCH", skin.hold)
        }





        /** Smooth pitch (90 = vertical). Gravity-turn style. */
        private fun approximatePitch(tSec: Float): Float {
            if (tSec < 0f) return 90f
            val turnStart = 12f
            val turnTau = 95f
            return if (tSec < turnStart) {
                90f
            } else {
                val u = ((tSec - turnStart) / turnTau).coerceIn(0f, 1f)
                val eased = 1f - (1f - u) * (1f - u) * (1f - u)
                (90f - 72f * eased).coerceIn(12f, 90f)
            }
        }

        /**
         * Gentle roll during ascent (degrees). Peaks near max-Q then damps.
         * Purely visual / interpolated so the ball feels alive.
         */
        private fun approximateRoll(tSec: Float): Float {
            if (tSec < 0f) return 0f
            // Small sinusoidal bank that grows then settles
            val peak = 8f
            val phase = (tSec / 45f) * Math.PI.toFloat()
            val envelope = when {
                tSec < 20f -> tSec / 20f
                tSec < 120f -> 1f
                tSec < 200f -> 1f - (tSec - 120f) / 80f
                else -> 0.15f
            }.coerceIn(0f, 1f)
            return peak * envelope * sin(phase.toDouble()).toFloat()
        }

        private fun approximateProfile(
            tSec: Float,
            launch: com.ccos.retro.data.LaunchSnapshot? = null,
            stage: Int = 1
        ): Triple<Float, Float, String> = com.ccos.retro.event.FlightProfiles.profile(tSec, launch, stage)

        /** Artificial horizon with pitch offset + roll bank. */
        /**
         * Agency-flavored ADI / attitude ball.
         * NASA ≈ Shuttle/CSS cyan-magenta instrument; SpaceX minimal white;
         * CASC red/gold; Roscosmos gold/red Soviet panel language.
         */
        private fun drawAttitudeIndicator(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            pitchDeg: Float,
            rollDeg: Float,
            skin: TelemetrySkin.Tokens
        ) {
            val style = skin.btnStyle
            val skyColor = when (style) {
                TelemetrySkin.ButtonStyle.NASA -> Color.argb(235, 30, 50, 90)
                TelemetrySkin.ButtonStyle.SPACEX -> Color.argb(230, 18, 22, 28)
                TelemetrySkin.ButtonStyle.CASC -> Color.argb(230, 40, 20, 18)
                TelemetrySkin.ButtonStyle.ROSCOSMOS -> Color.argb(230, 25, 35, 50)
                else -> Color.argb(230, 20, 40, 60)
            }
            val groundColor = when (style) {
                TelemetrySkin.ButtonStyle.NASA -> Color.argb(235, 55, 45, 30)
                TelemetrySkin.ButtonStyle.SPACEX -> Color.argb(230, 35, 32, 28)
                TelemetrySkin.ButtonStyle.CASC -> Color.argb(230, 50, 30, 20)
                TelemetrySkin.ButtonStyle.ROSCOSMOS -> Color.argb(230, 45, 40, 28)
                else -> Color.argb(230, 40, 28, 18)
            }
            val ladderColor = when (style) {
                TelemetrySkin.ButtonStyle.NASA -> Color.parseColor("#00E8FF") // cyan
                TelemetrySkin.ButtonStyle.SPACEX -> Color.WHITE
                TelemetrySkin.ButtonStyle.CASC -> Color.parseColor("#FFD700")
                TelemetrySkin.ButtonStyle.ROSCOSMOS -> Color.parseColor("#E8C547")
                else -> skin.accent
            }
            val symbolColor = when (style) {
                TelemetrySkin.ButtonStyle.NASA -> Color.parseColor("#FF00AA") // magenta like CSS
                else -> skin.accent
            }

            canvas.save()
            val path = Path()
            path.addCircle(cx, cy, radius, Path.Direction.CW)
            canvas.clipPath(path)

            val pitchOffset = (90f - pitchDeg) / 90f * radius * 1.55f
            canvas.rotate(rollDeg, cx, cy)

            fillPaint.color = skyColor
            canvas.drawRect(cx - radius * 1.6f, cy - radius * 2.2f - pitchOffset, cx + radius * 1.6f, cy - pitchOffset, fillPaint)
            fillPaint.color = groundColor
            canvas.drawRect(cx - radius * 1.6f, cy - pitchOffset, cx + radius * 1.6f, cy + radius * 2.2f - pitchOffset, fillPaint)

            // Horizon
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3f
            strokePaint.color = ladderColor
            canvas.drawLine(cx - radius * 1.5f, cy - pitchOffset, cx + radius * 1.5f, cy - pitchOffset, strokePaint)

            // Pitch ladder with numbers (NASA/Shuttle style)
            strokePaint.strokeWidth = 1.4f
            strokePaint.color = Color.argb(200, Color.red(ladderColor), Color.green(ladderColor), Color.blue(ladderColor))
            hudPaint.textSize = 10f
            hudPaint.color = ladderColor
            hudPaint.textAlign = Paint.Align.CENTER
            for (d in listOf(-40, -30, -20, -10, 10, 20, 30, 40)) {
                val dy = -pitchOffset + (d / 90f) * radius * 1.55f
                val half = if (kotlin.math.abs(d) % 20 == 0) radius * 0.42f else radius * 0.28f
                canvas.drawLine(cx - half, cy + dy, cx + half, cy + dy, strokePaint)
                if (kotlin.math.abs(d) % 20 == 0) {
                    canvas.drawText("$d", cx - half - 12f, cy + dy + 4f, hudPaint)
                    canvas.drawText("$d", cx + half + 12f, cy + dy + 4f, hudPaint)
                }
            }
            // Bank tick marks around rim (inside clip)
            strokePaint.strokeWidth = 1.5f
            for (a in -60..60 step 10) {
                val rad = Math.toRadians(a.toDouble())
                val x1 = cx + (radius * 0.88f * sin(rad)).toFloat()
                val y1 = cy - (radius * 0.88f * cos(rad)).toFloat()
                val x2 = cx + (radius * 0.98f * sin(rad)).toFloat()
                val y2 = cy - (radius * 0.98f * cos(rad)).toFloat()
                canvas.drawLine(x1, y1, x2, y2, strokePaint)
            }
            canvas.restore()

            // Outer bezel
            strokePaint.strokeWidth = 4f
            strokePaint.color = withLamp(skin.accentDim, prefs.lampBrightness)
            canvas.drawCircle(cx, cy, radius, strokePaint)
            strokePaint.strokeWidth = 2f
            strokePaint.color = withLamp(ladderColor, prefs.lampBrightness)
            canvas.drawCircle(cx, cy, radius - 5f, strokePaint)

            // Fixed waterline / aircraft symbol
            strokePaint.strokeWidth = 3f
            strokePaint.color = symbolColor
            canvas.drawLine(cx - radius * 0.5f, cy, cx - 10f, cy, strokePaint)
            canvas.drawLine(cx + 10f, cy, cx + radius * 0.5f, cy, strokePaint)
            // Center diamond / nose
            val nose = Path()
            nose.moveTo(cx, cy - 14f)
            nose.lineTo(cx - 8f, cy + 4f)
            nose.lineTo(cx + 8f, cy + 4f)
            nose.close()
            fillPaint.color = symbolColor
            canvas.drawPath(nose, fillPaint)
            canvas.drawLine(cx, cy, cx, cy + radius * 0.2f, strokePaint)

            // Roll pointer at top of bezel
            fillPaint.color = ladderColor
            val tip = Path()
            tip.moveTo(cx, cy - radius + 2f)
            tip.lineTo(cx - 7f, cy - radius + 16f)
            tip.lineTo(cx + 7f, cy - radius + 16f)
            tip.close()
            canvas.drawPath(tip, fillPaint)

            hudPaint.color = withLamp(skin.text, prefs.lampBrightness)
            hudPaint.textSize = 15f
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(String.format("P %.0f°  R %.0f°", pitchDeg, rollDeg), cx, cy + radius + 24f, hudPaint)
        }

        /**
         * Kerbal-style map view: Earth at one focus, Pe/Ap markers, vessel on the
         * vis-viva ellipse (or a suborbital ballistic if energy is not bound).
         */
        private fun drawOrbitMap(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSec: Float,
            altKm: Float,
            speedKmh: Float,
            pitchDeg: Float,
            launch: com.ccos.retro.data.LaunchSnapshot?,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            val gm = 398600.4418f
            val re = 6371f
            val rKm = re + altKm.coerceAtLeast(0.1f)
            val v = (speedKmh / 3.6f).coerceAtLeast(0.05f)
            val pitchRad = Math.toRadians(pitchDeg.toDouble())
            val vt = (v * cos(pitchRad)).toFloat().coerceAtLeast(0.02f)
            val vr = (v * sin(pitchRad)).toFloat()
            val hMom = rKm * vt
            val energy = 0.5f * (vr * vr + vt * vt) - gm / rKm
            val bound = energy < -0.05f
            val a = if (bound) -gm / (2f * energy) else 0f
            val e2 = 1.0 + (2.0 * energy * hMom * hMom) / (gm * gm)
            val e = sqrt(e2.coerceAtLeast(0.0)).toFloat().coerceIn(0f, 0.98f)
            val peR = if (bound) a * (1f - e) else rKm
            val apR = if (bound) a * (1f + e) else rKm
            val peAlt = peR - re
            val apAlt = apR - re
            val suborbital = !bound || peAlt < 0f
            val periodSec = if (bound && a > 0f) {
                (2.0 * Math.PI * sqrt((a * a * a / gm).toDouble())).toFloat()
            } else 0f
            val inc = approxInclination(launch?.location ?: "", launch?.pad ?: "")
            val p = (hMom * hMom / gm).coerceAtLeast(1f)
            val cosNu = if (e < 0.002f) 1f else (((p / rKm) - 1f) / e).coerceIn(-1f, 1f)
            var nu = acos(cosNu)
            if (vr < 0f) nu = (2.0 * Math.PI).toFloat() - nu

            val bezel = radius + 12f
            fillPaint.color = Color.argb(220, 4, 8, 14)
            canvas.drawCircle(cx, cy, bezel, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 7f
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawCircle(cx, cy, bezel, strokePaint)

            val maxR = if (suborbital) max(re + 200f, rKm * 1.15f) else max(apR * 1.12f, re * 1.8f)
            val scale = (radius * 0.86f) / maxR
            val earthR = re * scale

            fillPaint.color = Color.argb(235, 18, 42, 78)
            canvas.drawCircle(cx, cy, earthR, fillPaint)
            fillPaint.color = Color.argb(90, 40, 90, 50)
            canvas.drawCircle(cx - earthR * 0.18f, cy - earthR * 0.1f, earthR * 0.55f, fillPaint)
            strokePaint.strokeWidth = 5.5f
            strokePaint.color = withLamp(skin.accent, lamp)
            canvas.drawCircle(cx, cy, earthR, strokePaint)
            strokePaint.strokeWidth = 4f
            strokePaint.color = withLamp(Color.argb(180, 80, 160, 220), lamp)
            canvas.drawCircle(cx, cy, (re + 100f) * scale, strokePaint)

            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(skin.muted, lamp)
            hudPaint.textSize = 18f
            canvas.drawText("EARTH", cx, cy + 6f, hudPaint)

            if (suborbital) {
                strokePaint.strokeWidth = 8f
                strokePaint.color = withLamp(skin.hold, lamp)
                val path = Path()
                val steps = 36
                for (i in 0..steps) {
                    val u = i / steps.toFloat()
                    val ang = Math.toRadians((200.0 - u * 140.0))
                    val rr = earthR + (rKm - re) * scale * (4f * u * (1f - u) + 0.08f)
                    val x = cx + (rr * cos(ang)).toFloat()
                    val y = cy + (rr * sin(ang)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, strokePaint)
                val prog = ((tSec + 5f) / 180f).coerceIn(0.02f, 0.92f)
                val ang = Math.toRadians((200.0 - prog * 140.0))
                val rr = earthR + (rKm - re) * scale * (4f * prog * (1f - prog) + 0.08f)
                val vx = cx + (rr * cos(ang)).toFloat()
                val vy = cy + (rr * sin(ang)).toFloat()
                drawVesselChevron(canvas, vx, vy, ang.toFloat() + Math.PI.toFloat() / 2f, skin, lamp)
                val splashAng = Math.toRadians(60.0)
                val sx = cx + ((earthR + 8f) * cos(splashAng)).toFloat()
                val sy = cy + ((earthR + 8f) * sin(splashAng)).toFloat()
                fillPaint.color = withLamp(skin.danger, lamp)
                canvas.drawCircle(sx, sy, 8f, fillPaint)
                hudPaint.color = withLamp(skin.hold, lamp)
                hudPaint.textSize = 20f
                canvas.drawText("SUBORBITAL  ·  SPLASH PREDICT", cx, cy + radius + 8f, hudPaint)
            } else {
                val b = a * sqrt((1f - e * e).toDouble()).toFloat()
                val cOff = a * e * scale
                strokePaint.strokeWidth = 8f
                strokePaint.color = withLamp(skin.accent, lamp)
                canvas.drawOval(
                    cx - cOff - a * scale, cy - b * scale,
                    cx - cOff + a * scale, cy + b * scale,
                    strokePaint
                )
                fun node(nuRad: Float): Pair<Float, Float> {
                    val rr = (p / (1f + e * cos(nuRad.toDouble()).toFloat())) * scale
                    return (cx + rr * cos(nuRad.toDouble()).toFloat()) to
                        (cy + rr * sin(nuRad.toDouble()).toFloat())
                }
                val (pex, pey) = node(0f)
                val (apx, apy) = node(Math.PI.toFloat())
                fillPaint.color = withLamp(skin.go, lamp)
                canvas.drawCircle(pex, pey, 7f, fillPaint)
                fillPaint.color = withLamp(skin.hold, lamp)
                canvas.drawCircle(apx, apy, 7f, fillPaint)
                val (vx, vy) = node(nu)
                drawVesselChevron(canvas, vx, vy, nu + Math.PI.toFloat() / 2f, skin, lamp)

                val dist = { v: Float ->
                    if (prefs.useImperial) String.format("%.0f mi", v * 0.621371f)
                    else String.format("%.0f km", v)
                }
                val perTxt = when {
                    periodSec <= 0f -> "—"
                    periodSec >= 3600f -> String.format("T %dh %02dm", (periodSec / 3600).toInt(), ((periodSec % 3600) / 60).toInt())
                    else -> String.format("T %dm %02ds", (periodSec / 60).toInt(), (periodSec % 60).toInt())
                }
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = 18f
                canvas.drawText(
                    "Pe ${dist(peAlt)}   Ap ${dist(apAlt)}",
                    cx, cy + radius + 4f, hudPaint
                )
                canvas.drawText(
                    "INC ${String.format("%.1f", inc)}°   $perTxt   INSERT",
                    cx, cy + radius + 26f, hudPaint
                )
            }
        }

        private fun drawVesselChevron(
            canvas: Canvas,
            x: Float,
            y: Float,
            heading: Float,
            skin: TelemetrySkin.Tokens,
            lamp: Float
        ) {
            canvas.save()
            canvas.rotate(Math.toDegrees(heading.toDouble()).toFloat(), x, y)
            val path = Path()
            path.moveTo(x, y - 12f)
            path.lineTo(x - 8f, y + 10f)
            path.lineTo(x, y + 4f)
            path.lineTo(x + 8f, y + 10f)
            path.close()
            fillPaint.color = withLamp(skin.go, lamp)
            canvas.drawPath(path, fillPaint)
            canvas.restore()
        }

        private fun approxInclination(location: String, pad: String): Float {
            val s = "$location $pad".lowercase()
            return when {
                "vandenberg" in s || "vafb" in s -> 70f
                "baikonur" in s -> 51.6f
                "plesetsk" in s -> 62.8f
                "kourou" in s || "guiana" in s -> 5.2f
                "mahia" in s || "new zealand" in s -> 39f
                "satish" in s || "sriharikota" in s -> 13.7f
                "tanegashima" in s -> 30.4f
                "wenchang" in s -> 19.3f
                "jiuquan" in s -> 40.9f
                "wallops" in s -> 37.8f
                "starbase" in s || "boca chica" in s -> 26.0f
                "kennedy" in s || "cape" in s || "canaveral" in s -> 28.5f
                else -> 28.5f
            }
        }

        private fun openCommandCenter() {
            try {
                val intent = Intent(this@RetroCommandWallpaperService, com.ccos.retro.ui.CommandCenterActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            } catch (_: Exception) { }
        }

        private fun drawTelemetryPanel(canvas: Canvas) {
            val skin = TelemetrySkin.forLaunch(telemetryModule.tracked)
            val lamp = prefs.lampBrightness
            val btnW = width * 0.175f
            val btnH = height * 0.078f
            val btnGap = height * 0.014f
            val btnTop = height * 0.055f
            val btnBottom = btnTop + 4f * btnH + 3f * btnGap
            val panelLeft = btnW + 18f
            val panelRight = width - btnW - 18f
            val top = btnBottom + height * 0.012f
            extraRockerHits.clear()
            extraChipHits.clear()
            themeChipRects.clear()
            if (prefs.extraScreens) applyExtraScreens()

            val titleSize = panelTitleSize()
            val labelSz = panelLabelSize()
            var rockerH = panelRockerH()
            if (prefs.extraScreens) rockerH *= 0.86f
            val inset = 16f
            var y = top + titleSize + su(0.04f)
            y += labelSz + 8f
            layoutRockerRow(textRockerRects, panelLeft + inset, y, panelRight - inset, rockerH)
            y = textRockerRects[0].bottom + su(0.04f)
            y += labelSz + 8f
            layoutRockerRow(lampRockerRects, panelLeft + inset, y, panelRight - inset, rockerH)

            val toggleH = panelToggleH() * (if (prefs.extraScreens) 0.90f else 1f)
            val toggleY = lampRockerRects[0].bottom + su(0.028f)
            val half = (panelRight - panelLeft - 36f) / 2f
            panelToggleAnalog.set(panelLeft + inset, toggleY, panelLeft + inset + half - 6f, toggleY + toggleH)
            panelToggleUnits.set(panelLeft + inset + half + 6f, toggleY, panelRight - inset, toggleY + toggleH)

            val extraY = toggleY + toggleH + su(0.016f)
            panelToggleExtra.set(panelLeft + inset, extraY, panelLeft + inset + half - 6f, extraY + toggleH)
            panelToggleConsole.set(panelLeft + inset + half + 6f, extraY, panelRight - inset, extraY + toggleH)
            var afterExtra = extraY + toggleH
            if (prefs.extraScreens) {
                val chipY = extraY + toggleH + 6f
                val chipH = toggleH * 0.70f
                val gap = 8f
                val chipW = (panelRight - panelLeft - inset * 2f - gap * 3f) / 4f
                val chipDefs = listOf(
                    "TRAJ" to { prefs.extraScreenTraj = !prefs.extraScreenTraj },
                    "STG" to { prefs.extraScreenStg = !prefs.extraScreenStg },
                    "ENG" to { prefs.extraScreenEng = !prefs.extraScreenEng },
                    "PROP" to { prefs.extraScreenProp = !prefs.extraScreenProp }
                )
                chipDefs.forEachIndexed { i, (lab, action) ->
                    val r = RectF(
                        panelLeft + inset + i * (chipW + gap),
                        chipY,
                        panelLeft + inset + i * (chipW + gap) + chipW,
                        chipY + chipH
                    )
                    extraChipHits.add(r to action)
                }
                afterExtra = chipY + chipH
} else {
                /* extra chips hidden */
            }

            val holdLabelY = afterExtra + su(0.028f)
            val holdTop = holdLabelY + labelSz + 8f
            layoutRockerRow(holdRockerRects, panelLeft + inset, holdTop, panelRight - inset, rockerH)
            extraRockerHits.clear()
            val holdMs = longArrayOf(2L * 3600_000L, 6L * 3600_000L, 2L * 86400_000L)
            for (i in holdRockerRects.indices) {
                val dur = holdMs[i]
                extraRockerHits.add(holdRockerRects[i] to {
                    telemetryModule.holdFor(dur)
                })
            }

            val footerTop = holdRockerRects[0].bottom + su(0.03f)
            val bottom = min(footerTop + labelSz * 2.4f + su(0.04f), height - 8f)
            panelBounds.set(panelLeft - 6f, top - 8f, panelRight + 6f, bottom + 6f)
            drawConsoleShell(canvas, panelLeft, top, panelRight, bottom, withLamp(skin.accent, lamp))
            canvas.save()
            canvas.clipRect(panelBounds)

            hudPaint.color = withLamp(skin.accent, lamp)
            hudPaint.textSize = titleSize
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("COMMAND", width / 2f, top + titleSize * 0.85f, hudPaint)

            fun rowLabel(text: String, row: Array<RectF>) {
                hudPaint.color = withLamp(skin.text, lamp)
                hudPaint.textSize = labelSz
                hudPaint.textAlign = Paint.Align.LEFT
                val ly = row[0].top - 8f
                if (ly > panelBounds.top + labelSz * 0.8f) {
                    canvas.drawText(text, row[0].left, ly, hudPaint)
                }
            }
            rowLabel("HUD TEXT", textRockerRects)
            drawRockerRow(
                canvas, textRockerRects, AppPrefs.ROCKER_LABELS_TEXT, prefs.textStepIndex(),
                withLamp(skin.accent, lamp), withLamp(skin.text, lamp), withLamp(skin.muted, lamp)
            )
            rowLabel("LAMP", lampRockerRects)
            drawRockerRow(
                canvas, lampRockerRects, AppPrefs.ROCKER_LABELS_LAMP, prefs.lampStepIndex(),
                withLamp(skin.accent, lamp), withLamp(skin.text, lamp), withLamp(skin.muted, lamp)
            )
            val holdSel = when (prefs.telemetryHoldDurationMs) {
                6L * 3600_000L -> 1
                2L * 86400_000L -> 2
                else -> 0
            }
            rowLabel("HOLD", holdRockerRects)
            drawRockerRow(
                canvas, holdRockerRects, arrayOf("2H", "6H", "2D"), holdSel,
                withLamp(skin.accent, lamp), withLamp(skin.text, lamp), withLamp(skin.muted, lamp)
            )

            fun drawToggle(r: RectF, on: Boolean, onLabel: String, offLabel: String) {
                fillPaint.color = withLamp(if (on) skin.btnActiveFill else skin.btnIdleFill, lamp)
                canvas.drawRoundRect(r, 10f, 10f, fillPaint)
                strokePaint.strokeWidth = 4.5f
                strokePaint.color = withLamp(if (on) skin.accent else skin.muted, lamp)
                canvas.drawRoundRect(r, 10f, 10f, strokePaint)
                val lab = if (on) onLabel else offLabel
                hudPaint.color = withLamp(if (on) skin.text else Color.WHITE, lamp)
                hudPaint.textAlign = Paint.Align.CENTER
                hudPaint.textSize = telFit(lab, r.width() * 0.92f, r.height() * 0.50f, 14f)
                canvas.drawText(lab, r.centerX(), r.centerY() + hudPaint.textSize * 0.35f, hudPaint)
            }
            drawToggle(panelToggleAnalog, prefs.telemetryAnalog, "ANALOG", "DIGITAL")
            drawToggle(panelToggleUnits, prefs.useImperial, "MPH / MI", "KM/H")
            drawToggle(panelToggleExtra, prefs.extraScreens, "EXTRA PAGES ON", "EXTRA PAGES OFF")
            drawToggle(panelToggleConsole, true, "ENTER CONSOLE", "ENTER CONSOLE")
            if (prefs.extraScreens) {
                val chipOn = booleanArrayOf(
                    prefs.extraScreenTraj, prefs.extraScreenStg,
                    prefs.extraScreenEng, prefs.extraScreenProp
                )
                extraChipHits.forEachIndexed { i, (r, _) ->
                    val on = chipOn.getOrElse(i) { false }
                    fillPaint.color = withLamp(if (on) skin.btnActiveFill else skin.btnIdleFill, lamp)
                    canvas.drawRoundRect(r, 8f, 8f, fillPaint)
                    strokePaint.strokeWidth = if (on) 3.5f else 2f
                    strokePaint.color = withLamp(if (on) skin.accent else skin.muted, lamp)
                    canvas.drawRoundRect(r, 8f, 8f, strokePaint)
                    val lab = arrayOf("TRAJ", "STG", "ENG", "PROP").getOrElse(i) { "" }
                    hudPaint.color = withLamp(if (on) skin.text else skin.muted, lamp)
                    hudPaint.textAlign = Paint.Align.CENTER
                    hudPaint.textSize = telFit(lab, r.width() * 0.92f, r.height() * 0.55f, 12f)
                    canvas.drawText(lab, r.centerX(), r.centerY() + hudPaint.textSize * 0.35f, hudPaint)
                }
            }

            val launch = telemetryModule.tracked
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = withLamp(skin.accent, lamp)
            hudPaint.textSize = labelSz
            canvas.drawText(skin.label, width / 2f, footerTop + labelSz, hudPaint)
            hudPaint.color = Color.WHITE
            hudPaint.textSize = (labelSz * 0.92f)
            canvas.drawText((launch?.name ?: "NO TRACKED LAUNCH").take(28), width / 2f, footerTop + labelSz * 2.05f, hudPaint)
            hudPaint.color = withLamp(skin.muted, lamp)
            hudPaint.textSize = labelSz * 0.72f
            canvas.drawText("CCOS HUD  ·  CONSOLE = MCC  ·  CMD x2 = SETTINGS", width / 2f, bottom - su(0.018f), hudPaint)
            canvas.restore()
        }

        private fun applyExtraScreens() {
            if (!prefs.extraScreens) return
            val modes = mutableListOf<String>()
            if (prefs.extraScreenTraj) modes.add(AppPrefs.DATA_TRAJ)
            if (prefs.extraScreenStg) {
                modes.add(AppPrefs.DATA_STG1)
                modes.add(AppPrefs.DATA_STG2)
            }
            if (prefs.extraScreenEng) modes.add(AppPrefs.DATA_ENG)
            if (prefs.extraScreenProp) modes.add(AppPrefs.DATA_PROP)
            if (modes.isEmpty()) modes.add(AppPrefs.DATA_TELEMETRY)
            var mi = 0
            val pages = prefs.launcherPageCount.coerceIn(2, 12)
            for (page in 0 until pages) {
                if (page == prefs.commandPageIndex) continue
                val mode = modes[mi % modes.size]
                mi++
                prefs.setOffPageData(page, mode)
            }
        }

        private fun drawTelStatus(canvas: Canvas, launch: com.ccos.retro.data.LaunchSnapshot?, skin: TelemetrySkin.Tokens, ts: Float) {
            val lamp = prefs.lampBrightness
            val cx = width / 2f
            val maxW = width * 0.72f
            val titleSz = pageBodyTs(18f)
            val bodySz = pageBodyTs(22f)
            val subSz = pageBodyTs(14f)
            val top = (telHudBottom + height * 0.03f).coerceAtLeast(height * 0.26f)
            hudPaint.textAlign = Paint.Align.CENTER
            var y = top
            y = drawWrappedCenter(canvas, "STATUS BOARD", cx, y, maxW, titleSz, withLamp(skin.accent, lamp))
            if (launch == null) {
                drawWrappedCenter(canvas, "AWAITING DATA", cx, y, maxW, bodySz, withLamp(skin.muted, lamp))
                return
            }
            val tSec = telemetryModule.effectiveSecondsFromNet()
            val goCol = if (hudFailed(launch, tSec)) skin.danger else if (!launch.holdReason.isNullOrBlank()) skin.hold else skin.go
            y = drawWrappedCenter(canvas, launch.statusName.uppercase(), cx, y, maxW, bodySz, withLamp(goCol, lamp))
            y = drawWrappedCenter(canvas, launch.name, cx, y, maxW, subSz, withLamp(skin.text, lamp))
            y = drawWrappedCenter(canvas, "${launch.rocketName}  ·  ${launch.provider}", cx, y, maxW, subSz, withLamp(skin.muted, lamp))
            if (!launch.holdReason.isNullOrBlank()) {
                y = drawWrappedCenter(canvas, "HOLD  ${launch.holdReason}", cx, y, maxW, subSz, withLamp(skin.hold, lamp))
            }
            launch.probability?.let {
                y = drawWrappedCenter(canvas, "WEATHER  $it%", cx, y, maxW, subSz, withLamp(skin.text, lamp))
            }
            val absSecs = kotlin.math.abs(tSec).toLong()
            val sign = if (tSec <= 0f) "T-" else "T+"
            val clock = String.format("%s%02d:%02d:%02d", sign, absSecs / 3600, (absSecs % 3600) / 60, absSecs % 60)
            drawWrappedCenter(canvas, clock, cx, y, maxW, bodySz, withLamp(skin.accent, lamp))
        }

        private fun drawTelPad(canvas: Canvas, launch: com.ccos.retro.data.LaunchSnapshot?, skin: TelemetrySkin.Tokens, ts: Float) {
            geoHit.setEmpty()
            layoutButtons()
            val lamp = prefs.lampBrightness
            val titleSz = pageBodyTs(22f)
            val bodySz = pageBodyTs(26f)
            val geoSz = pageBodyTs(28f)
            val left = laneLeft()
            val right = laneRight()
            val cx = (left + right) * 0.5f
            val maxW = (right - left) * 0.94f
            val gap = su(0.008f)
            hudPaint.textAlign = Paint.Align.CENTER
            var y = telHudBottom + gap
            y = drawWrappedCenter(canvas, "PAD / SITE", cx, y, maxW, titleSz, withLamp(skin.accent, lamp))
            val kind = PadGlyph.kind(launch)
            y = drawWrappedCenter(canvas, PadGlyph.label(kind), cx, y, maxW, bodySz, Color.WHITE)

            val padLine = launch?.pad?.ifBlank { "—" } ?: "NO LOCK"
            val locLine = launch?.location?.ifBlank { "—" } ?: ""
            val ll = PadBook.lonLat(launch)
            val geoLine = if (ll != null) {
                val (lon, lat) = ll
                val hemiNS = if (lat >= 0f) "N" else "S"
                val hemiEW = if (lon >= 0f) "E" else "W"
                String.format("%.2f°%s  %.2f°%s", abs(lat), hemiNS, abs(lon), hemiEW)
            } else ""
            val seaLine = if (launch == null) "" else if (PadBook.isSea(launch)) "SEA / BARGE" else "LAND"
            val vehLine = if (launch == null) "" else "${launch.rocketName}  ·  ${launch.provider}"

            fun lineH(sz: Float) = sz * 1.28f
            var foot = gap
            foot += lineH(bodySz)
            if (locLine.isNotBlank()) foot += lineH(bodySz)
            if (geoLine.isNotBlank()) foot += lineH(geoSz)
            if (seaLine.isNotBlank()) foot += lineH(bodySz)
            if (vehLine.isNotBlank()) foot += lineH(bodySz)

            val floor = dockFloor()
            val glyphTop = y + gap
            val glyphBot = (floor - foot).coerceAtLeast(glyphTop + su(0.12f))
            val leftoverH = (glyphBot - glyphTop).coerceAtLeast(8f)
            val plate = buttonRects[4]
            val aspect = if (plate.height() > 4f) plate.width() / plate.height() else 0.90f
            var gw = leftoverH * aspect
            val glyphMaxW = (right - left) * 1.08f
            if (gw > glyphMaxW) gw = glyphMaxW
            val gx = cx - gw * 0.5f
            val glyph = RectF(gx, glyphTop, gx + gw, glyphBot)
            PadGlyph.draw(canvas, glyph, Color.WHITE, kind, strokePaint)
            y = glyph.bottom + gap

            if (launch == null) {
                drawWrappedCenter(canvas, "NO LOCK", cx, y, maxW, bodySz, withLamp(skin.muted, lamp))
                return
            }
            y = drawWrappedCenter(canvas, padLine, cx, y, maxW, bodySz, withLamp(skin.text, lamp))
            if (locLine.isNotBlank()) {
                y = drawWrappedCenter(canvas, locLine, cx, y, maxW, bodySz, withLamp(skin.muted, lamp))
            }
            if (geoLine.isNotBlank()) {
                val y0 = y
                y = drawWrappedCenter(canvas, geoLine, cx, y, maxW, geoSz, Color.WHITE)
                geoHit.set(left, y0 - geoSz, right, y)
            }
            if (seaLine.isNotBlank()) {
                y = drawWrappedCenter(canvas, seaLine, cx, y, maxW, bodySz, withLamp(skin.muted, lamp))
            }
            if (vehLine.isNotBlank()) {
                drawWrappedCenter(canvas, vehLine, cx, y, maxW, bodySz, withLamp(skin.text, lamp))
            }
        }

        private fun drawTelVideo(canvas: Canvas, launch: com.ccos.retro.data.LaunchSnapshot?, skin: TelemetrySkin.Tokens, ts: Float) {
            val lamp = prefs.lampBrightness
            hudPaint.color = withLamp(skin.accent, lamp)
            hudPaint.textSize = pageBodyTs(18f)
            hudPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("WEBCAST", width / 2f, height * 0.28f, hudPaint)
            // Fake embed frame
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f
            strokePaint.color = withLamp(skin.accent, lamp * 0.7f)
            val fx = width * 0.18f
            val fy = height * 0.34f
            val fw = width * 0.64f
            val fh = height * 0.22f
            canvas.drawRoundRect(fx, fy, fx + fw, fy + fh, 10f, 10f, strokePaint)
            fillPaint.color = withLamp(Color.parseColor("#18000000"), lamp)
            canvas.drawRoundRect(fx, fy, fx + fw, fy + fh, 10f, 10f, fillPaint)
            // Play triangle
            fillPaint.color = withLamp(skin.accent, lamp)
            val path = Path()
            val pcx = width / 2f
            val pcy = fy + fh / 2f
            path.moveTo(pcx - 18f, pcy - 22f)
            path.lineTo(pcx - 18f, pcy + 22f)
            path.lineTo(pcx + 26f, pcy)
            path.close()
            canvas.drawPath(path, fillPaint)
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = 14f * ts
            hudPaint.color = withLamp(skin.text, lamp)
            hudPaint.textSize = pageBodyTs(14f)
            canvas.drawText("TAP TO OPEN LIVE FOR THIS LAUNCH", width / 2f, fy + fh + 28f, hudPaint)
            if (launch != null) {
                drawWrappedCenter(
                    canvas, launch.name, width / 2f, fy + fh + 52f,
                    width * 0.72f, pageBodyTs(16f), withLamp(skin.accent, lamp)
                )
                hudPaint.color = withLamp(skin.muted, lamp)
                hudPaint.textSize = pageBodyTs(13f)
                canvas.drawText(launch.provider.take(24), width / 2f, fy + fh + 100f, hudPaint)
            }
        }



        private fun drawTelMission(canvas: Canvas, launch: com.ccos.retro.data.LaunchSnapshot?, skin: TelemetrySkin.Tokens, ts: Float) {
            val lamp = prefs.lampBrightness
            val cx = width / 2f
            val maxW = width * 0.72f
            val titleSz = pageBodyTs(18f)
            val bodySz = pageBodyTs(16f)
            val subSz = pageBodyTs(13f)
            val top = (telHudBottom + height * 0.03f).coerceAtLeast(height * 0.26f)
            hudPaint.textAlign = Paint.Align.CENTER
            var y = top
            y = drawWrappedCenter(canvas, "MISSION", cx, y, maxW, titleSz, withLamp(skin.accent, lamp))
            if (launch == null) {
                drawWrappedCenter(canvas, "NO LOCK", cx, y, maxW, bodySz, withLamp(skin.muted, lamp))
                return
            }
            val m = MissionFacts.brief(launch, telemetryModule.effectiveSecondsFromNet())
            y = drawWrappedCenter(canvas, m.title, cx, y, maxW, bodySz, withLamp(skin.text, lamp))
            y = drawWrappedCenter(canvas, MissionFacts.goalLine(launch), cx, y, maxW, subSz, withLamp(skin.hold, lamp))
            y = drawWrappedCenter(canvas, m.vehicle, cx, y, maxW, subSz, withLamp(skin.muted, lamp))
            y = drawWrappedCenter(canvas, m.objective, cx, y, maxW, bodySz, withLamp(skin.text, lamp))
            if (m.note.isNotBlank()) {
                y = drawWrappedCenter(canvas, m.note, cx, y, maxW, subSz, withLamp(if (m.classified) skin.hold else skin.muted, lamp))
            }
            drawWrappedCenter(canvas, "${m.payloadName}  ·  ${m.payloadState}", cx, y, maxW, subSz, withLamp(skin.go, lamp))
        }

        private fun drawOffPageDataWall(canvas: Canvas, now: Long) {
            val raw = prefs.offPageData(currentLauncherPage)
            val mode = when (raw) {
                AppPrefs.DATA_TELEMETRY -> AppPrefs.DATA_TELEMETRY
                AppPrefs.DATA_SYSTEM -> AppPrefs.DATA_SYSTEM
                AppPrefs.DATA_ROCKET -> AppPrefs.DATA_ROCKET
                AppPrefs.DATA_TRAJ -> AppPrefs.DATA_TRAJ
                AppPrefs.DATA_STG1 -> AppPrefs.DATA_STG1
                AppPrefs.DATA_STG2 -> AppPrefs.DATA_STG2
                AppPrefs.DATA_ENG -> AppPrefs.DATA_ENG
                AppPrefs.DATA_PROP -> AppPrefs.DATA_PROP
                else -> when {
                    prefs.isTelemetry() -> AppPrefs.DATA_TELEMETRY
                    prefs.isSystem() -> AppPrefs.DATA_SYSTEM
                    else -> AppPrefs.DATA_ROCKET
                }
            }
            if (mode != AppPrefs.DATA_SYSTEM && mode != AppPrefs.DATA_ROCKET) {
                telemetryModule.resolveTracked(now)
            }

            val left = width * 0.06f
            val right = width * 0.94f
            val top = height * 0.42f
            val bottom = height * 0.97f
            val skin = TelemetrySkin.forLaunch(telemetryModule.tracked)
            val pal = com.ccos.retro.skin.SystemSkin.palette(prefs.systemTheme, prefs.lampBrightness)
            val accent = when (mode) {
                AppPrefs.DATA_SYSTEM -> pal.primary
                AppPrefs.DATA_ROCKET -> RetroSkin.cyan
                else -> skin.accent
            }

            fillPaint.color = Color.argb(235, 4, 6, 10)
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, fillPaint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1.8f
            strokePaint.color = Color.argb(50, Color.red(accent), Color.green(accent), Color.blue(accent))
            var gx = left + 18f
            while (gx < right - 10f) {
                canvas.drawLine(gx, top + 10f, gx, bottom - 10f, strokePaint)
                gx += su(0.04f)
            }
            var gy = top + 18f
            while (gy < bottom - 10f) {
                canvas.drawLine(left + 10f, gy, right - 10f, gy, strokePaint)
                gy += su(0.035f)
            }
            strokePaint.strokeWidth = 4.5f
            strokePaint.color = accent
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, strokePaint)
            val br = su(0.035f)
            canvas.drawLine(left + 10f, top + 10f, left + 10f + br, top + 10f, strokePaint)
            canvas.drawLine(left + 10f, top + 10f, left + 10f, top + 10f + br, strokePaint)
            canvas.drawLine(right - 10f, top + 10f, right - 10f - br, top + 10f, strokePaint)
            canvas.drawLine(right - 10f, top + 10f, right - 10f, top + 10f + br, strokePaint)

            val titleSz = panelTitleSize()
            val bodySz = panelLabelSize()
            val bigSz = su(0.072f).coerceIn(40f, 72f)
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = accent
            hudPaint.textSize = titleSz
            val modeLabel = when (mode) {
                AppPrefs.DATA_TELEMETRY -> "LAUNCH"
                AppPrefs.DATA_SYSTEM -> "PHONE"
                AppPrefs.DATA_TRAJ -> "GROUND TRACK"
                AppPrefs.DATA_STG1 -> "STAGE 1"
                AppPrefs.DATA_STG2 -> "STAGE 2"
                AppPrefs.DATA_ENG -> "ENGINES"
                AppPrefs.DATA_PROP -> "PROP"
                else -> "ROCKET"
            }
            canvas.drawText("READ ONLY  ·  $modeLabel", width / 2f, top + titleSz + 12f, hudPaint)

            when (mode) {
                AppPrefs.DATA_TELEMETRY -> {
                    val launch = telemetryModule.tracked
                    val secs = launch?.secondsToNet(now) ?: 0L
                    val absSecs = kotlin.math.abs(secs)
                    val sign = if (secs >= 0) "T-" else "T+"
                    val clock = String.format(
                        "%s%02d:%02d:%02d",
                        sign, absSecs / 3600, (absSecs % 3600) / 60, absSecs % 60
                    )
                    hudPaint.textSize = bigSz
                    canvas.drawText(clock, width / 2f, top + titleSz + bigSz + 24f, hudPaint)
                    hudPaint.color = Color.WHITE
                    hudPaint.textSize = bodySz
                    val y0 = top + titleSz + bigSz + su(0.06f)
                    canvas.drawText((launch?.name ?: "NO TRACKED LAUNCH").take(32), width / 2f, y0, hudPaint)
                    hudPaint.color = accent
                    canvas.drawText("${skin.label}  ·  ${launch?.statusAbbrev ?: "--"}", width / 2f, y0 + bodySz * 1.4f, hudPaint)
                    hudPaint.color = Color.parseColor("#E0E8F0")
                    canvas.drawText((launch?.rocketName ?: "").take(28), width / 2f, y0 + bodySz * 2.8f, hudPaint)
                    canvas.drawText((launch?.pad ?: "PAD --").take(28), width / 2f, y0 + bodySz * 4.2f, hudPaint)
                    canvas.drawText((launch?.location ?: "").take(32), width / 2f, y0 + bodySz * 5.6f, hudPaint)
                    val t = lastTele
                    canvas.drawText(
                        "BAT ${t?.batteryPercent ?: 0}%  CPU ${t?.cpuPercent ?: 0}%  RAM ${t?.usedMemMb ?: 0}MB",
                        width / 2f, bottom - bodySz * 1.6f, hudPaint
                    )
                }
                AppPrefs.DATA_TRAJ -> {
                    val launch = telemetryModule.tracked
                    val lamp = prefs.lampBrightness
                    val tSec = telemetryModule.effectiveSecondsFromNet(now)
                    val (altKm, speedKmh, _) = approximateProfile(tSec, launch, hudStage(launch, tSec))
                    val pitchDeg = approximatePitch(tSec)
                    hudPaint.color = Color.WHITE
                    hudPaint.textSize = bodySz
                    canvas.drawText((launch?.name ?: "NO TRACKED LAUNCH").take(32), width / 2f, top + titleSz + bodySz + 20f, hudPaint)
                    if (launch != null) {
                        val mapR = min((right - left) * 0.40f, (bottom - top) * 0.30f)
                        drawAgencyOrbitView(
                            canvas, width / 2f, (top + bottom) * 0.58f, mapR,
                            tSec, altKm, speedKmh, pitchDeg, launch, skin, lamp
                        )
                    }
                }
                AppPrefs.DATA_STG1, AppPrefs.DATA_STG2 -> {
                    val launch = telemetryModule.tracked
                    val lamp = prefs.lampBrightness
                    val tSec = telemetryModule.effectiveSecondsFromNet(now)
                    val stg = if (mode == AppPrefs.DATA_STG2) 2 else 1
                    val (altKm, speedKmh, _) = approximateProfile(tSec, launch, stg)
                    hudPaint.color = Color.WHITE
                    hudPaint.textSize = bodySz
                    if (launch != null) {
                        val vh = (bottom - top) * 0.48f
                        val baseY = top + titleSz + 28f + vh
                        drawVehicle(canvas, width / 2f, baseY, vh, launch, tSec, stg, true, skin, lamp, 1f)
                        val n = engineCountForStage(launch, stg)
                        hudPaint.textSize = bodySz
                        canvas.drawText("ENG $n", width / 2f, baseY + bodySz * 1.4f, hudPaint)
                        val imperial = prefs.useImperial
                        val altStr = if (imperial) String.format("%.1f mi", altKm * 0.621371f) else String.format("%.1f km", altKm)
                        val spdStr = if (imperial) String.format("%,.0f mph", speedKmh * 0.621371f) else String.format("%,.0f km/h", speedKmh)
                        canvas.drawText("$altStr   $spdStr", width / 2f, baseY + bodySz * 2.8f, hudPaint)
                    } else {
                        canvas.drawText("NO TRACKED LAUNCH", width / 2f, (top + bottom) * 0.55f, hudPaint)
                    }
                }
                AppPrefs.DATA_ENG -> {
                    val launch = telemetryModule.tracked
                    val lamp = prefs.lampBrightness
                    val tSec = telemetryModule.effectiveSecondsFromNet(now)
                    if (launch != null) {
                        val stg = hudStage(launch, tSec)
                        val engines = engineCountForStage(launch, stg)
                        val lit = com.ccos.retro.event.FlightProfiles.enginesLit(
                            tSec, launch, stg, engines
                        )
                        drawEngineBank(
                            canvas, width / 2f, (top + bottom) * 0.58f,
                            engines, lit, skin, lamp, min(right - left, bottom - top) * 0.18f,
                            launch, stg
                        )
                    } else {
                        hudPaint.color = Color.WHITE
                        hudPaint.textSize = bodySz
                        canvas.drawText("NO TRACKED LAUNCH", width / 2f, (top + bottom) * 0.55f, hudPaint)
                    }
                }
                AppPrefs.DATA_PROP -> {
                    val lamp = prefs.lampBrightness
                    val tSec = telemetryModule.effectiveSecondsFromNet(now)
                    val fuel = fuelRemain(tSec, hudStage(telemetryModule.tracked, tSec))
                    val can = RectF(left + 48f, top + titleSz + 36f, right - 48f, bottom - 80f)
                    drawFuelTanks(canvas, can, fuel, fuel, skin, lamp)
                }
                AppPrefs.DATA_SYSTEM -> {
                    val t = lastTele
                    hudPaint.textSize = bigSz
                    hudPaint.color = pal.good
                    val chg = if (t?.isCharging == true) " CHG" else ""
                    canvas.drawText("BAT ${t?.batteryPercent ?: 0}%$chg", width / 2f, top + titleSz + bigSz + 20f, hudPaint)
                    hudPaint.textSize = bodySz
                    hudPaint.color = Color.WHITE
                    val y0 = top + titleSz + bigSz + su(0.07f)
                    canvas.drawText("CPU ${t?.cpuPercent ?: 0}%   CORES ${t?.availableProcessors ?: 0}", width / 2f, y0, hudPaint)
                    canvas.drawText("RAM ${t?.usedMemMb ?: 0} / ${t?.maxMemMb ?: 0} MB", width / 2f, y0 + bodySz * 1.5f, hudPaint)
                    val usedPct = ((t?.internalStorageFrac ?: 0f) * 100f).toInt()
                    canvas.drawText("STORAGE  $usedPct%   THR ${t?.activeThreads ?: 0}", width / 2f, y0 + bodySz * 3.0f, hudPaint)
                    canvas.drawText("FRAME ${"%.0f".format(t?.frameTimeMs ?: 0f)} ms", width / 2f, y0 + bodySz * 4.5f, hudPaint)
                    hudPaint.color = pal.text
                    canvas.drawText("LOOK ONLY  ·  NO CONTROLS", width / 2f, bottom - bodySz * 1.4f, hudPaint)
                }
                else -> {
                    hudPaint.textSize = bodySz * 1.15f
                    hudPaint.color = Color.WHITE
                    val y0 = top + titleSz + su(0.07f)
                    val gap = bodySz * 1.55f
                    canvas.drawText("ENGINES     ${(state.engineProgress * 100).toInt()}%", width / 2f, y0, hudPaint)
                    canvas.drawText("PAD / TOWER ${(state.padProgress * 100).toInt()}%", width / 2f, y0 + gap, hudPaint)
                    canvas.drawText("BODY        ${(state.rocketBodyProgress * 100).toInt()}%", width / 2f, y0 + gap * 2f, hudPaint)
                    canvas.drawText("SUBSYSTEMS  ${(state.subsystemsProgress * 100).toInt()}%", width / 2f, y0 + gap * 3f, hudPaint)
                    canvas.drawText("BUDGET      ${state.budget.toInt()}", width / 2f, y0 + gap * 4f, hudPaint)
                    hudPaint.color = RetroSkin.cyan
                    canvas.drawText("LOOK ONLY  ·  NO CONTROLS", width / 2f, bottom - bodySz * 1.4f, hudPaint)
                }
            }
        }
    }
}

