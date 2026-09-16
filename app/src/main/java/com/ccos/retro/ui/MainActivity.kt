package com.ccos.retro.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ccos.retro.R
import com.ccos.retro.data.LaunchDataProvider
import com.ccos.retro.data.MissionTapeStore
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.data.autoDwellHint
import com.ccos.retro.data.LaunchWindow
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.module.RocketTelemetryModule
import com.ccos.retro.wallpaper.RetroCommandWallpaperService

/**
 * CCOS settings. Single paid app. Current + Auto on first install.
 * Historic search for past flights. No modules, no rocket game.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var launchProvider: LaunchDataProvider
    private lateinit var telemetryModule: RocketTelemetryModule
    private var launchList: List<LaunchSnapshot> = emptyList()
    private var suppressLaunchSelect = false
    private var historicQuery: String = ""
    /** Stamp 107: debounce LL2 historic search — never fire every keystroke. */
    private val uiHandler = Handler(Looper.getMainLooper())
    private val historicSearchDebounceMs = 450L
    /** Stamp 112: true while debounce runnable is scheduled (mid-type protect). */
    @Volatile private var historicSearchDebouncePending: Boolean = false
    private var populateDepth = 0
    private val historicSearchRunnable = Runnable { fireHistoricSearchDebounced() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        MissionTapeStore.ensure(this)
        prefs.restoreCommandPageHud()
        prefs.ensurePaidAppDefaults()
        if (!prefs.setupComplete) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        launchProvider = LaunchDataProvider()
                telemetryModule = RocketTelemetryModule(prefs, launchProvider)
        // Stamp 63 A: onCreate ensureData -> forceRefresh if null -> resolveTracked -> keepTrackedOrLastGood
        telemetryModule.ensureData()
        if (telemetryModule.tracked == null) {
            telemetryModule.forceRefresh {
                runOnUiThread {
                    telemetryModule.resolveTracked()
                    telemetryModule.keepTrackedOrLastGood()
                    populateLaunchSpinner()
                    refreshTrackingUi()
                }
            }
        } else {
            telemetryModule.resolveTracked()
            telemetryModule.keepTrackedOrLastGood()
        }



        wireConsoleSkin()

        findViewById<Button>(R.id.btn_set_wallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, RetroCommandWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_open_console).setOnClickListener {
            startActivity(Intent(this, CommandCenterActivity::class.java))
        }

        findViewById<Button>(R.id.btn_auto_on).setOnClickListener {
            // Stamp 55: AUTO ON re-locks live CURRENT window (drop HOLD/historic trap).
            prefs.telemetryAuto = true
            prefs.telemetryListMode = "current"
            prefs.telemetryPinned = false
            telemetryModule.releaseHold()
            telemetryModule.clearSim()
            launchProvider.clearHistoricSearchInterest()
            historicQuery = ""
            telemetryModule.resolveTracked()
            populateLaunchSpinner()
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_auto_off).setOnClickListener {
            prefs.telemetryAuto = false
            refreshTrackingUi()
        }

        findViewById<Button>(R.id.btn_list_current).setOnClickListener {
            // Stamp 55: CURRENT tab forces follow=AUTO and re-locks live window.
            prefs.telemetryListMode = "current"
            prefs.telemetryAuto = true
            prefs.telemetryPinned = false
            telemetryModule.releaseHold()
            telemetryModule.clearSim()
            telemetryModule.resolveTracked()
            populateLaunchSpinner()
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_list_historical).setOnClickListener {
            prefs.telemetryListMode = "historical"
            prefs.telemetryAuto = false
            populateLaunchSpinner()
            refreshTrackingUi()
        }

        findViewById<Button>(R.id.btn_horizon_week).setOnClickListener {
            prefs.telemetryHorizonDays = 7
            populateLaunchSpinner()
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_horizon_month).setOnClickListener {
            prefs.telemetryHorizonDays = 30
            populateLaunchSpinner()
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_horizon_6mo).setOnClickListener {
            prefs.telemetryHorizonDays = 180
            populateLaunchSpinner()
            refreshTrackingUi()
        }

        findViewById<Button>(R.id.btn_units_mph).setOnClickListener {
            prefs.useImperial = true
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_units_kmh).setOnClickListener {
            prefs.useImperial = false
            refreshTrackingUi()
        }

        findViewById<EditText>(R.id.et_historic_search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Stamp 107: local filter every key; network search debounced >=400ms.
                historicQuery = s?.toString()?.trim().orEmpty()
                launchProvider.setHistoricSearchInterest(historicQuery)
                populateLaunchSpinner()
                uiHandler.removeCallbacks(historicSearchRunnable)
                historicSearchDebouncePending = historicQuery.isNotBlank() && "demo" !in historicQuery.lowercase()
                if (historicSearchDebouncePending) {
                    uiHandler.postDelayed(historicSearchRunnable, historicSearchDebounceMs)
                }
            }
        })

        val launchSpinner = findViewById<Spinner>(R.id.spinner_launch)
        launchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressLaunchSelect) return
                if (position in launchList.indices) {
                    val launch = launchList[position]
                    // Stamp 60: real USER id change always selectLaunch (AUTO off + stick THAT bird).
                    // Programmatic populate still blocked by suppressLaunchSelect.
                    if (launch.id == prefs.telemetryLaunchId && launch.id == telemetryModule.tracked?.id) return
                    // Stamp 108: pass full snapshot so historic search picks (F13/USSF) stick.
                    // Keep HISTORICAL listMode — never flip to CURRENT on select.
                    telemetryModule.selectLaunch(launch.id, launch)
                    refreshTrackingUi()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btn_lamp_dim).setOnClickListener { prefs.setLampStep(0); refreshTrackingUi() }
        findViewById<Button>(R.id.btn_lamp_norm).setOnClickListener { prefs.setLampStep(1); refreshTrackingUi() }
        findViewById<Button>(R.id.btn_lamp_bright).setOnClickListener { prefs.setLampStep(2); refreshTrackingUi() }
        findViewById<Button>(R.id.btn_text_sm).setOnClickListener { prefs.setTextStep(0); refreshTrackingUi() }
        findViewById<Button>(R.id.btn_text_md).setOnClickListener { prefs.setTextStep(1); refreshTrackingUi() }
        findViewById<Button>(R.id.btn_text_lg).setOnClickListener { prefs.setTextStep(2); refreshTrackingUi() }

        val pageLabel = findViewById<TextView>(R.id.txt_page_index)
        val screensLabel = findViewById<TextView>(R.id.txt_screen_count)
        fun refreshPageLabels() {
            pageLabel.text = "COMMAND PAGE ${prefs.commandPageIndex}"
            screensLabel.text = "${prefs.launcherPageCount} SCREENS"
            rebuildPageFills()
        }
        findViewById<Button>(R.id.btn_page_minus).setOnClickListener {
            prefs.commandPageIndex = (prefs.commandPageIndex - 1).coerceAtLeast(0)
            refreshPageLabels()
        }
        findViewById<Button>(R.id.btn_page_plus).setOnClickListener {
            prefs.commandPageIndex = (prefs.commandPageIndex + 1)
                .coerceAtMost((prefs.launcherPageCount - 1).coerceAtLeast(0))
                .coerceAtMost(AppPrefs.MAX_COMMAND_PAGE_INDEX)
            refreshPageLabels()
        }
        findViewById<Button>(R.id.btn_screens_minus).setOnClickListener {
            prefs.launcherPageCount = (prefs.launcherPageCount - 1).coerceAtLeast(1)
            if (prefs.commandPageIndex > prefs.launcherPageCount - 1) {
                prefs.commandPageIndex = prefs.launcherPageCount - 1
            }
            refreshPageLabels()
        }
        findViewById<Button>(R.id.btn_screens_plus).setOnClickListener {
            prefs.launcherPageCount = (prefs.launcherPageCount + 1).coerceAtMost(12)
            refreshPageLabels()
        }
        refreshPageLabels()

        findViewById<Button>(R.id.btn_refresh_launches).setOnClickListener {
            updateLaunchStatus("Fetching...")
            syncHistoricQueryFromBox()
            val keepQ = historicQuery
            launchProvider.refreshIfNeeded(force = true) {
                runOnUiThread {
                    // Stamp 112: manual REFRESH keeps text; re-run SAME historic query if any.
                    syncHistoricQueryFromBox()
                    if (prefs.telemetryListMode == "historical" && keepQ.isNotBlank()) {
                        historicQuery = keepQ
                        launchProvider.setHistoricSearchInterest(keepQ)
                        val et = findViewById<EditText>(R.id.et_historic_search)
                        if (et != null && et.text?.toString()?.trim().orEmpty() != keepQ) {
                            et.setText(keepQ)
                            et.setSelection(keepQ.length)
                        }
                        launchProvider.requestHistoricSearch(keepQ) {
                            runOnUiThread {
                                populateLaunchSpinner()
                                updateLaunchStatus(launchProvider.lastStatus)
                            }
                        }
                    } else {
                        populateLaunchSpinner()
                        updateLaunchStatus(launchProvider.lastStatus)
                    }
                }
            }
        }

        populateLaunchSpinner()
        updateLaunchStatus("Fetching...")
        launchProvider.refreshIfNeeded(force = true) {
            runOnUiThread {
                // Stamp 57: catalog landed - resolve AUTO now (onCreate resolve was pre-fetch null).
                telemetryModule.resolveTracked()
                // Stamp 112: never discard mid-type query on catalog land.
                syncHistoricQueryFromBox()
                populateLaunchSpinner()
                updateLaunchStatus(launchProvider.lastStatus)
                refreshStatusLine()
                refreshTrackingUi()
            }
        }

        refreshTrackingUi()
    }

    private fun updateLaunchStatus(msg: String) {
        val pending = MissionTapeStore.pendingNote(
            if (::telemetryModule.isInitialized) telemetryModule.tracked else null
        ) ?: MissionTapeStore.sharedHudNote
        @Suppress("NAME_SHADOWING")
        val msg = if (!pending.isNullOrBlank() && pending !in msg) "$msg | $pending" else msg
        val tv = findViewById<TextView>(R.id.txt_launch_status) ?: return
        tv.text = msg
        tv.setTextColor(
            when {
                msg.contains("PENDING REAL TELEMETRY") -> 0xFFFFCC66.toInt()
                msg.startsWith("OK") || msg.startsWith("SEARCH OK") -> 0xFF90FFB0.toInt()
                msg.startsWith("FAILED") || msg.contains("throttle", ignoreCase = true) ->
                    0xFFFF8080.toInt()
                else -> 0xFF668899.toInt()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // Stamp 57: never force AUTO on resume  -  MANUAL pick must stick (Soyuz stay Soyuz).
        refreshTrackingUi()
        syncHistoricQueryFromBox()
        populateLaunchSpinner()
        updateLaunchStatus(launchProvider.lastStatus)
        refreshStatusLine()
    }

    private fun historicBlob(l: LaunchSnapshot): String =
        "${l.name} ${l.rocketName} ${l.provider} ${l.pad} ${l.location} ${l.statusName} ${l.missionName} ${l.holdReason.orEmpty()}".lowercase()

    private fun matchesHistoric(l: LaunchSnapshot, q: String): Boolean {
        if (q.isBlank()) return true
        val blob = historicBlob(l)
        return q.lowercase().split(Regex("\\s+")).all { it in blob }
    }

    /** Stamp 107: debounced historic LL2 search — not every keystroke. */
    private fun fireHistoricSearchDebounced() {
        historicSearchDebouncePending = false
        if (isFinishing || isDestroyed) return
        if (prefs.telemetryListMode != "historical") return
        val q = historicQuery
        if (q.isBlank() || "demo" in q.lowercase()) return
        try {
            launchProvider.requestHistoricSearch(q) {
                uiHandler.post {
                    try {
                        if (isFinishing || isDestroyed) return@post
                        if (prefs.telemetryListMode != "historical") return@post
                        // Refresh from search cache only — never re-enter requestHistoricSearch here.
                        populateLaunchSpinner()
                        updateLaunchStatus(launchProvider.lastStatus)
                    } catch (e: Exception) {
                        Log.e("CCOS.Main", "search UI refresh: ${e.message}", e)
                        try { updateLaunchStatus("SEARCH UI EX | ${e.message}") } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CCOS.Main", "fireHistoricSearch: ${e.message}", e)
            try { updateLaunchStatus("SEARCH FIRE EX | ${e.message}") } catch (_: Exception) {}
        }
    }


    /** Stamp 112: mid-type / focus / in-flight search — protect query + spinner. */
    private fun isHistoricSearchUiActive(): Boolean {
        if (prefs.telemetryListMode != "historical") return false
        val et = findViewById<EditText>(R.id.et_historic_search)
        val typed = et?.text?.toString()?.orEmpty() ?: ""
        if (et?.hasFocus() == true) return true
        if (typed.isNotBlank()) return true
        if (historicQuery.isNotBlank()) return true
        if (launchProvider.isHistoricSearching) return true
        if (historicSearchDebouncePending) return true
        return launchProvider.hasActiveHistoricSearch()
    }

    /** Stamp 112: keep historicQuery + interest synced from EditText (never invent blank). */
    private fun syncHistoricQueryFromBox() {
        val et = findViewById<EditText>(R.id.et_historic_search) ?: return
        val typed = et.text?.toString()?.trim().orEmpty()
        if (typed.isNotBlank() || et.hasFocus()) {
            historicQuery = typed
            launchProvider.setHistoricSearchInterest(typed)
        }
    }

    private fun populateLaunchSpinner() {
        // Stamp 107: re-entry / concurrent spinner updates must not kill the process.
        if (populateDepth > 0) return
        populateDepth++
        try {
            val now = System.currentTimeMillis()
            // Stamp 93: CURRENT = pickerPool (live only); HISTORICAL = historicPool (no live upcoming).
            val liveOnly = launchProvider.pickerPool(now, prefs.telemetryHorizonDays)
            val historic = prefs.telemetryListMode == "historical"

            launchList = if (historic) {
                // Stamp 106/107/110: local pool only. LL2 search debounced separately.
                // Search hits: do NOT re-filter with isReplayable (drops valid CDN/LL2 past).
                val q = historicQuery
                val pool = if (q.isNotBlank()) {
                    launchProvider.historicPool(now, q)
                } else {
                    launchProvider.historicPool(now, q).filter {
                        it.id.startsWith("demo-") || it.isReplayable(now) || it.secondsToNet(now) <= 0
                    }
                }
                if (q.isNotBlank()) pool.take(80) else pool.take(20)
            } else {
                val keepId = prefs.telemetryLaunchId
                // Stamp 55: CURRENT keep only HOLD/in-flight/webcast/T+/upcoming  -  past -> HISTORIC.
                val keep = liveOnly.firstOrNull { it.id == keepId }
                    ?: launchProvider.findById(keepId)?.takeIf {
                        it.isActiveWatch(now) || it.isHold() || it.secondsToNet(now) > 0
                    }
                    ?: telemetryModule.tracked?.takeIf {
                        it.id == keepId && (it.isActiveWatch(now) || it.isHold() || it.secondsToNet(now) > 0)
                    }
                val base = liveOnly.toMutableList()
                if (keep != null && base.none { it.id == keep.id }) base.add(0, keep)
                base.sortedBy { it.netMs }.take(20)
            }

            val labels = launchList.map { l ->
                val secs = l.secondsToNet(now)
                val tag = when {
                    l.id.startsWith("demo-") -> "[DEMO] "
                    l.isHold() -> "[HOLD] "
                    l.isActiveWatch(now) && secs <= 0 -> "[LIVE] "
                    secs < -LaunchWindow.PICKER_LOOKBACK_SEC -> "[PAST] "
                    secs < 0 -> "[T+] "
                    secs < 86400 -> "[${secs / 3600}h] "
                    else -> "[${secs / 86400}d] "
                }
                "$tag${l.name.take(36)} | ${l.provider.take(12)}"
            }
            val spinner = findViewById<Spinner>(R.id.spinner_launch)
            suppressLaunchSelect = true
            val emptyMsg = when {
                // Stamp 110: never "No historic match" while search/catalog fetch in flight.
                historic && historicQuery.isNotBlank() &&
                    (launchProvider.isHistoricSearching || launchProvider.isFetching) ->
                    "Historic search…"
                launchProvider.isFetching -> "Fetching..."
                historic && historicQuery.isNotBlank() -> "No historic match: " + historicQuery.take(18)
                historic -> "No historic entries yet"
                liveOnly.isEmpty() && launchProvider.lastError != null ->
                    "0 live | ${launchProvider.lastError}"
                liveOnly.isEmpty() ->
                    "0 live in cache | ${launchProvider.lastStatus}"
                else -> "0 in window (live cache=${liveOnly.size}) | try 1 MO / 6 MO"
            }
            spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                if (labels.isEmpty()) listOf(emptyMsg) else labels
            )
            val idx = launchList.indexOfFirst { it.id == prefs.telemetryLaunchId }
            if (idx >= 0) spinner.setSelection(idx, false)
            // Stamp 55: onItemSelected often fires after this frame  -  hold suppress until post.
            spinner.post { spinner.post { suppressLaunchSelect = false } }

            val extra = if (!historic) {
                " | ${launchList.size} shown | ${liveOnly.size} in window"
            } else {
                " | ${launchList.size} historic"
            }
            updateLaunchStatus(launchProvider.lastStatus + extra)
            refreshStatusLine()
        } catch (e: Exception) {
            Log.e("CCOS.Main", "populateLaunchSpinner: ${e.message}", e)
            try {
                updateLaunchStatus("LIST EX | ${e.message}")
            } catch (_: Exception) {}
        } finally {
            populateDepth--
        }
    }

    private fun refreshStatusLine() {
        val tv = findViewById<TextView>(R.id.txt_status) ?: return
        val now = System.currentTimeMillis()
        val tracked = telemetryModule.tracked
        if ((prefs.telemetryAuto || prefs.telemetryPinned) && tracked != null) {
            tv.text = "${tracked.autoDwellHint(now, pinned = prefs.telemetryPinned, holdDurationMs = prefs.telemetryHoldDurationMs)} | ${tracked.name.take(22)}"
            tv.setTextColor(0xFF90FFB0.toInt())
            return
        }
        val next = launchProvider.getNextAny(now)
        tv.text = when {
            next != null && next.secondsToNet(now) > -1800 -> {
                val s = next.secondsToNet(now)
                val whenStr = when {
                    s < 0 -> "LIVE"
                    s < 3600 -> "T-${s / 60}m"
                    s < 86400 -> "T-${s / 3600}h"
                    else -> "T-${s / 86400}d"
                }
                "Next | ${next.name.take(28)} | $whenStr"
            }
            else -> "Command Center - set wallpaper, then you are on the next launch"
        }
        tv.setTextColor(0xFFFFFFFF.toInt())
    }
    private fun refreshTrackingUi() {
        val historic = prefs.telemetryListMode == "historical"
        findViewById<View>(R.id.row_horizon)?.visibility = if (historic) View.GONE else View.VISIBLE
        findViewById<View>(R.id.row_search)?.visibility = if (historic) View.VISIBLE else View.GONE

        styleChip(R.id.btn_list_current, !historic)
        styleChip(R.id.btn_list_historical, historic)
        styleChip(R.id.btn_horizon_week, prefs.telemetryHorizonDays == 7)
        styleChip(R.id.btn_horizon_month, prefs.telemetryHorizonDays == 30)
        styleChip(R.id.btn_horizon_6mo, prefs.telemetryHorizonDays == 180)
        styleChip(R.id.btn_auto_on, prefs.telemetryAuto)
        styleChip(R.id.btn_auto_off, !prefs.telemetryAuto)
        styleChip(R.id.btn_units_mph, prefs.useImperial)
        styleChip(R.id.btn_units_kmh, !prefs.useImperial)

        val lampStep = prefs.lampStepIndex()
        styleChip(R.id.btn_lamp_dim, lampStep == 0)
        styleChip(R.id.btn_lamp_norm, lampStep == 1)
        styleChip(R.id.btn_lamp_bright, lampStep == 2)
        val textStep = prefs.textStepIndex()
        styleChip(R.id.btn_text_sm, textStep == 0)
        styleChip(R.id.btn_text_md, textStep == 1)
        styleChip(R.id.btn_text_lg, textStep == 2)
        rebuildPageFills()
    }

    private fun rebuildPageFills() {
        val host = findViewById<LinearLayout>(R.id.page_fill_list) ?: return
        host.removeAllViews()
        val dp = resources.displayMetrics.density
        fun dp(v: Int) = (v * dp).toInt()
        val modes = listOf(
            AppPrefs.DATA_AUTO to "AUTO",
            AppPrefs.DATA_TELEMETRY to "LAUNCH",
            AppPrefs.DATA_SYSTEM to "PHONE"
        )
        val count = prefs.launcherPageCount
        for (page in 0 until count) {
            val isCommand = page == prefs.commandPageIndex
            val title = TextView(this).apply {
                text = if (isCommand) {
                    "PAGE $page  |  COMMAND (HUD lives here)"
                } else {
                    "PAGE $page  |  look-only fill"
                }
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 15f
                setPadding(0, dp(10), 0, dp(4))
            }
            host.addView(title)
            val note = TextView(this).apply {
                text = if (isCommand) {
                    "Live HUD stays on this page."
                } else {
                    "AUTO follows the HUD. LAUNCH or PHONE locks this page."
                }
                setTextColor(0xFFC8D8E8.toInt())
                textSize = 13f
                setPadding(0, 0, 0, dp(6))
            }
            host.addView(note)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val selected = prefs.offPageData(page)
            for ((mode, label) in modes) {
                val b = Button(this).apply {
                    text = label
                    textSize = 13f
                    isAllCaps = true
                    setOnClickListener {
                        prefs.setOffPageData(page, mode)
                        rebuildPageFills()
                    }
                }
                val lp = LinearLayout.LayoutParams(0, dp(48), 1f)
                lp.marginEnd = dp(3)
                row.addView(b, lp)
                val on = selected == mode
                if (on) {
                    b.setBackgroundColor(0xFF1A3040.toInt())
                    b.setTextColor(0xFF90FFB0.toInt())
                } else {
                    b.setBackgroundColor(0xFF1A1A22.toInt())
                    b.setTextColor(0xFFD0DCE8.toInt())
                }
            }
            host.addView(row)
        }
    }

    private fun styleChip(id: Int, active: Boolean) {
        val b = findViewById<Button>(id) ?: return
        if (active) {
            b.setBackgroundColor(0xFF1A3040.toInt())
            b.setTextColor(0xFF90FFB0.toInt())
        } else {
            b.setBackgroundColor(0xFF1A1A22.toInt())
            b.setTextColor(0xFFD0DCE8.toInt())
        }
    }


        private fun styleConsoleChip(b: Button?, selected: Boolean, selectedBg: Int, idleBg: Int, selectedText: Int, idleText: Int) {
            if (b == null) return
            try {
                b.backgroundTintList = null
            } catch (_: Throwable) {}
            try {
                // AppCompat / Material support tint
                val m = b.javaClass.methods.firstOrNull { it.name == "setSupportBackgroundTintList" && it.parameterTypes.size == 1 }
                m?.invoke(b, null)
            } catch (_: Throwable) {}
            b.setBackgroundResource(if (selected) selectedBg else idleBg)
            b.setTextColor(if (selected) selectedText else idleText)
            b.isAllCaps = false
            b.textSize = 15f
        }

        private fun applyConsoleSkin() {
        val section = findViewById<LinearLayout>(R.id.section_console_skin) ?: return
        val title = findViewById<TextView>(R.id.txt_console_title)
        val help = findViewById<TextView>(R.id.txt_console_help)
        val mcc = findViewById<Button>(R.id.btn_console_mcc)
        val ros = findViewById<Button>(R.id.btn_console_ros)
        val clear = findViewById<Button>(R.id.btn_console_clear)
        val skin = prefs.consoleSkin
        // Stamp 70: selected = BRIGHT fill + BRIGHT text; idle = dark + dim. Clear Material tints.
        mcc?.text = "MCC"
        ros?.text = "ROS"
        clear?.text = "CLEAR"
        when (skin) {
            AppPrefs.CONSOLE_SKIN_ROS -> {
                section.setBackgroundResource(R.drawable.bg_console_ros)
                title?.visibility = View.VISIBLE
                title?.text = "\u041D\u0410\u0421\u0422\u0420\u041E\u0419\u041A\u0418 / SETTINGS"
                title?.setTextColor(0xFFE8F0D8.toInt())
                help?.text = "Selected LIT bright text | idle dim | shared CMD flyout"
                help?.setTextColor(0xFFB8C890.toInt())
                styleConsoleChip(mcc, false, R.drawable.chip_console_selected_ros, R.drawable.chip_console_ros_idle, 0xFFFFFFF0.toInt(), 0xFF6A7A58.toInt())
                styleConsoleChip(ros, true, R.drawable.chip_console_selected_ros, R.drawable.chip_console_ros_idle, 0xFFFFFFF0.toInt(), 0xFF6A7A58.toInt())
                styleConsoleChip(clear, false, R.drawable.chip_console_selected_ros, R.drawable.chip_console_ros_idle, 0xFFFFFFF0.toInt(), 0xFF6A7A58.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF0A0C08.toInt())
            }
            AppPrefs.CONSOLE_SKIN_CLEAR -> {
                title?.visibility = View.VISIBLE
                section.setBackgroundResource(R.drawable.bg_console_clear)
                title?.text = "CONSOLE"
                title?.setTextColor(0xFF00E5FF.toInt())
                help?.text = "Selected LIT bright text | idle dim | shared CMD flyout"
                help?.setTextColor(0xFF8AA0B0.toInt())
                styleConsoleChip(mcc, false, R.drawable.chip_console_selected_clear, R.drawable.chip_console_idle, 0xFFFFFFF0.toInt(), 0xFF5A6878.toInt())
                styleConsoleChip(ros, false, R.drawable.chip_console_selected_clear, R.drawable.chip_console_idle, 0xFFFFFFF0.toInt(), 0xFF5A6878.toInt())
                styleConsoleChip(clear, true, R.drawable.chip_console_selected_clear, R.drawable.chip_console_idle, 0xFFFFFFF0.toInt(), 0xFF5A6878.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF060C12.toInt())
            }
            else -> {
                title?.visibility = View.VISIBLE
                section.setBackgroundResource(R.drawable.bg_console_mcc)
                title?.text = "CONSOLE"
                title?.setTextColor(0xFFFFB000.toInt())
                help?.text = "Selected LIT bright text | idle dim | shared CMD flyout"
                help?.setTextColor(0xFF8AA0B0.toInt())
                // HARD: never 0xFF1A1000 on MCC selected — bright cream/white on lit amber
                styleConsoleChip(mcc, true, R.drawable.chip_console_selected_mcc, R.drawable.chip_console_idle, 0xFFFFFFF0.toInt(), 0xFF5A6878.toInt())
                styleConsoleChip(ros, false, R.drawable.chip_console_selected_mcc, R.drawable.chip_console_idle, 0xFFFFFFF0.toInt(), 0xFF5A6878.toInt())
                styleConsoleChip(clear, false, R.drawable.chip_console_selected_mcc, R.drawable.chip_console_idle, 0xFFFFFFF0.toInt(), 0xFF5A6878.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF0A0E14.toInt())
            }
        }
        findViewById<View>(R.id.section_telemetry)?.let { tel ->
            when (skin) {
                AppPrefs.CONSOLE_SKIN_ROS -> tel.setBackgroundResource(R.drawable.bg_console_ros)
                AppPrefs.CONSOLE_SKIN_CLEAR -> tel.setBackgroundResource(R.drawable.bg_console_clear)
                else -> tel.setBackgroundResource(R.drawable.panel_console)
            }
        }
        (findViewById<View>(android.R.id.content) as? android.view.ViewGroup)?.getChildAt(0)?.setBackgroundColor(
            when (skin) {
                AppPrefs.CONSOLE_SKIN_ROS -> 0xFF1A2010.toInt()
                AppPrefs.CONSOLE_SKIN_CLEAR -> 0xFF060C12.toInt()
                else -> 0xFF0A0E14.toInt()
            }
        )
    }

private fun wireConsoleSkin() {
        fun pick(id: String) {
            prefs.consoleSkin = id
            applyConsoleSkin()
        }
        findViewById<Button>(R.id.btn_console_mcc)?.setOnClickListener { pick(AppPrefs.CONSOLE_SKIN_MCC) }
        findViewById<Button>(R.id.btn_console_ros)?.setOnClickListener { pick(AppPrefs.CONSOLE_SKIN_ROS) }
        findViewById<Button>(R.id.btn_console_clear)?.setOnClickListener { pick(AppPrefs.CONSOLE_SKIN_CLEAR) }
        applyConsoleSkin()
    }


}
