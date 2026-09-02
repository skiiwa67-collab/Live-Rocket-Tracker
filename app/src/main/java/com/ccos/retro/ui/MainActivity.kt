package com.ccos.retro.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
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
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.wallpaper.RetroCommandWallpaperService

/**
 * CCOS settings. Single paid app. Current + Auto on first install.
 * Historic search for past flights. No modules, no rocket game.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var launchProvider: LaunchDataProvider
    private var launchList: List<LaunchSnapshot> = emptyList()
    private var suppressLaunchSelect = false
    private var historicQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        prefs.restoreCommandPageHud()
        prefs.ensurePaidAppDefaults()
        if (!prefs.setupComplete) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        launchProvider = LaunchDataProvider()

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
            prefs.telemetryAuto = true
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_auto_off).setOnClickListener {
            prefs.telemetryAuto = false
            refreshTrackingUi()
        }

        findViewById<Button>(R.id.btn_list_current).setOnClickListener {
            prefs.telemetryListMode = "current"
            prefs.telemetryAuto = true
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
                historicQuery = s?.toString()?.trim().orEmpty()
                populateLaunchSpinner()
            }
        })

        val launchSpinner = findViewById<Spinner>(R.id.spinner_launch)
        launchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressLaunchSelect) return
                if (position in launchList.indices) {
                    val launch = launchList[position]
                    prefs.telemetryLaunchId = launch.id
                    if (prefs.telemetryListMode == "historical") {
                        prefs.telemetryAuto = false
                    }
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
        findViewById<Button>(R.id.btn_gap_auto).setOnClickListener {
            prefs.bottomGapMode = "auto"
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_gap_dock).setOnClickListener {
            prefs.bottomGapMode = "dock"
            refreshTrackingUi()
        }
        findViewById<Button>(R.id.btn_gap_search).setOnClickListener {
            prefs.bottomGapMode = "dock_search"
            refreshTrackingUi()
        }
        refreshPageLabels()

        findViewById<Button>(R.id.btn_refresh_launches).setOnClickListener {
            updateLaunchStatus("Fetching…")
            launchProvider.refreshIfNeeded(force = true) {
                runOnUiThread {
                    populateLaunchSpinner()
                    updateLaunchStatus(launchProvider.lastStatus)
                }
            }
        }

        populateLaunchSpinner()
        updateLaunchStatus("Fetching…")
        launchProvider.refreshIfNeeded(force = true) {
            runOnUiThread {
                populateLaunchSpinner()
                updateLaunchStatus(launchProvider.lastStatus)
                refreshStatusLine()
            }
        }

        refreshTrackingUi()
    }

    private fun updateLaunchStatus(msg: String) {
        val tv = findViewById<TextView>(R.id.txt_launch_status) ?: return
        tv.text = msg
        tv.setTextColor(
            when {
                msg.startsWith("OK") -> 0xFF90FFB0.toInt()
                msg.startsWith("FAILED") || msg.contains("throttle", ignoreCase = true) ->
                    0xFFFF8080.toInt()
                else -> 0xFF668899.toInt()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        refreshTrackingUi()
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

    private fun populateLaunchSpinner() {
        val now = System.currentTimeMillis()
        val all = launchProvider.allSelectable()
        val liveOnly = launchProvider.getCached()?.launches.orEmpty()
            .filter { !it.id.startsWith("demo-") && it.isUpcoming(now) }
        val horizonMs = prefs.telemetryHorizonDays * 24L * 3600L * 1000L
        val historic = prefs.telemetryListMode == "historical"

        launchList = if (historic) {
            val pool = all.filter {
                it.id.startsWith("demo-") || it.isReplayable(now) || it.secondsToNet(now) < -60
            }
            val filtered = pool.filter { matchesHistoric(it, historicQuery) }
            if (historicQuery.isNotBlank()) filtered.take(80) else filtered.take(40)
        } else {
            liveOnly.filter { it.netMs <= now + horizonMs }
                .sortedBy { it.netMs }
                .take(10)
        }

        val labels = launchList.map { l ->
            val secs = l.secondsToNet(now)
            val tag = when {
                l.id.startsWith("demo-") -> "[DEMO] "
                secs < -300 -> "[PAST] "
                secs < 0 -> "[LIVE] "
                secs < 86400 -> "[${secs / 3600}h] "
                else -> "[${secs / 86400}d] "
            }
            "$tag${l.name.take(36)} · ${l.provider.take(12)}"
        }
        val spinner = findViewById<Spinner>(R.id.spinner_launch)
        suppressLaunchSelect = true
        val emptyMsg = when {
            launchProvider.isFetching -> "Fetching launches…"
            historic && historicQuery.isNotBlank() -> "No historic match for “${historicQuery.take(18)}”"
            historic -> "No historic entries yet"
            liveOnly.isEmpty() && launchProvider.lastError != null ->
                "0 live · ${launchProvider.lastError}"
            liveOnly.isEmpty() ->
                "0 live in cache · ${launchProvider.lastStatus}"
            else -> "0 in window (live cache=${liveOnly.size}) · try 1 MO / 6 MO"
        }
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            if (labels.isEmpty()) listOf(emptyMsg) else labels
        )
        val idx = launchList.indexOfFirst { it.id == prefs.telemetryLaunchId }
        if (idx >= 0) spinner.setSelection(idx, false)
        suppressLaunchSelect = false

        val extra = if (!historic) {
            " · ${launchList.size} shown · ${liveOnly.size} upcoming"
        } else {
            " · ${launchList.size} historic"
        }
        updateLaunchStatus(launchProvider.lastStatus + extra)
        refreshStatusLine()
    }

    private fun refreshStatusLine() {
        val tv = findViewById<TextView>(R.id.txt_status) ?: return
        val now = System.currentTimeMillis()
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
                "Next  ·  ${next.name.take(28)}  ·  $whenStr"
            }
            else -> "Command Center · set wallpaper, then you’re on the next launch"
        }
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
        styleChip(R.id.btn_gap_auto, prefs.bottomGapMode == "auto")
        styleChip(R.id.btn_gap_dock, prefs.bottomGapMode == "dock")
        styleChip(R.id.btn_gap_search, prefs.bottomGapMode == "dock_search")

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
                    "PAGE $page  ·  COMMAND (HUD lives here)"
                } else {
                    "PAGE $page  ·  look-only fill"
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
}
