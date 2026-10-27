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
import com.ccos.retro.data.SearchAliases
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.module.RocketTelemetryModule
import com.ccos.retro.pro.PickerTag
import com.ccos.retro.pro.ProHook
import com.ccos.retro.pro.ProPolicy
import com.ccos.retro.pro.ProSettingsRowModel
import com.ccos.retro.pro.RestoreState
import com.ccos.retro.pro.ThemeAccess
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
    /** vc159: Settings LRT Pro row restore state (v1 section 6). */
    private var proRestoreState = RestoreState.IDLE

    /** B4: with the release switch off every user is treated as unlocked (no locks, badges or Pro rows: vc158 UI). */
    private fun isPro(): Boolean = ProUi.access(prefs).isPro || !com.ccos.retro.pro.ProConfig.gatesEnabled()

    /** vc159: Pro turned on (purchase / restore) or entitlement refreshed: unlock in place. */
    private fun onProChanged() {
        if (isFinishing || isDestroyed) return
        applyConsoleSkin()
        populateLaunchSpinner()
        refreshTrackingUi()
        bindProRow()
    }

    /** vc160 DEBUG only: PRO_GOLD_PREVIEW extra (sheet | moment | static | row). Never grants Pro or writes a flag. */
    private var goldPreviewPending: String? = null
    private var goldRowPreview = false
    /** Da Vinci v2 section 2: ambient glints play once per opened Settings screen (armed in onResume). */
    private var rowGlint: GoldGlintDrawable? = null
    private var rowGlintArmed = false
    private var lockGlintArmed = false

    /** Chris 13:21 CT: DEBUG-only Settings "Test Pro unlock" / "Test Pro lock". Stock Button built in code, so
     *  release has no view, no string and (R8 strips BuildConfig.DEBUG-dead code) no handler. */
    private var proTestButton: Button? = null

    private fun bindProTestButton() {
        if (!com.ccos.retro.BuildConfig.DEBUG) return
        val row = findViewById<View>(R.id.row_lrt_pro) ?: return
        val parent = row.parent as? LinearLayout ?: return
        val b = proTestButton ?: Button(this).also { btn ->
            btn.isAllCaps = false
            btn.setOnClickListener { onProTestTapped() }
            parent.addView(btn, parent.indexOfChild(row) + 1,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            proTestButton = btn
        }
        b.text = com.ccos.retro.pro.ProDebugTest.label(ProUi.access(prefs).isPro, com.ccos.retro.pro.ProConfig.gatesEnabled())
    }

    private fun onProTestTapped() {
        if (!com.ccos.retro.BuildConfig.DEBUG) return
        val store = prefs.proStore()
        val owned = ProUi.access(prefs).isPro && com.ccos.retro.pro.ProConfig.gatesEnabled()
        if (owned) {
            // Tap 2: not owned + debug gates on, so the padlocks show again.
            com.ccos.retro.pro.ProDebugTest.lock(store)
            onProChanged()
        } else {
            // Tap 1: not owned + gates on + moment flag cleared, then the real sheet's real Get Pro handler.
            com.ccos.retro.pro.ProDebugTest.armUnlock(store)
            onProChanged()
            ProUi.showUpsell(this, prefs, debugTestPurchase = true) { onProChanged() }
        }
    }

    private fun bindProRow() {
        if (com.ccos.retro.BuildConfig.DEBUG) bindProTestButton()
        findViewById<View>(R.id.row_lrt_pro)?.visibility =
            if (com.ccos.retro.pro.ProConfig.gatesEnabled()) View.VISIBLE else View.GONE // B4
        val r = ProSettingsRowModel.render(isPro() || goldRowPreview, proRestoreState)
        // vc160 Da Vinci Pro Gold v1 4.3: owned row = gold plate + metal border + PRO pill + sparkle. Free row unchanged.
        val ownedGold = goldRowPreview || (ProUi.access(prefs).isPro && com.ccos.retro.pro.ProConfig.gatesEnabled())
        findViewById<View>(R.id.row_lrt_pro)?.background =
            if (ownedGold) ProGold.ownedRowBackground(this) else android.graphics.drawable.ColorDrawable(0xFF0F1620.toInt())
        findViewById<View>(R.id.txt_lrt_pro_pill)?.visibility = if (ownedGold) View.VISIBLE else View.GONE
        // Static sparkle = the rest state the v2 row glints settle on.
        findViewById<View>(R.id.img_lrt_pro_sparkle)?.visibility = if (ownedGold) View.VISIBLE else View.GONE
        findViewById<View>(R.id.row_lrt_pro)?.let { row ->
            if (!ownedGold) {
                rowGlint?.cancel(); rowGlint = null; row.foreground = null
            } else if (rowGlintArmed) {
                rowGlintArmed = false
                val gates = goldRowPreview || com.ccos.retro.pro.ProConfig.gatesEnabled()
                if (ProGold.idleGlintAllowed(this, gates)) {
                    val g = GoldGlintDrawable(ProGold.ROW_GLINT_POINTS, ProGold.ROW_GLINT_POINTS.indices.toList(),
                        ProGold.GOLD_PALE, null, 3f * resources.displayMetrics.density)
                    rowGlint?.cancel(); rowGlint = g
                    row.foreground = g
                    ProGold.startIdleGlint(row, g, gates)
                }
            }
        }
        findViewById<TextView>(R.id.txt_lrt_pro_title)?.setText(r.titleRes)
        findViewById<TextView>(R.id.txt_lrt_pro_status)?.apply {
            if (r.statusRes != null) { setText(r.statusRes); visibility = View.VISIBLE } else visibility = View.GONE
        }
        findViewById<Button>(R.id.btn_lrt_pro_restore)?.apply {
            visibility = if (r.showRestoreButton) View.VISIBLE else View.GONE
            isEnabled = r.restoreEnabled
        }
        findViewById<View>(R.id.row_lrt_pro)?.apply {
            // Free: the row opens the sheet (user tap only). Owned: no CTA.
            isClickable = r.showRestoreButton
            setOnClickListener(if (r.showRestoreButton) View.OnClickListener { ProUi.showUpsell(this@MainActivity, prefs) { onProChanged() } } else null)
        }
    }

    private fun wireProRow() {
        findViewById<Button>(R.id.btn_lrt_pro_restore)?.setOnClickListener {
            proRestoreState = RestoreState.CHECKING
            bindProRow()
            ProUi.billing.restore { res ->
                runOnUiThread {
                    proRestoreState = ProSettingsRowModel.afterRestore(res)
                    if (proRestoreState == RestoreState.RESTORED) {
                        prefs.proStore().putBoolean(com.ccos.retro.pro.ProAccess.KEY_PURCHASED, true)
                        android.widget.Toast.makeText(this, R.string.pro_restored, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onProChanged()
                }
            }
        }
        bindProRow()
    }

    /** v1 section 1: 18dp lock badge, top-end corner of a chip. */
    private fun chipLockForeground(glint: GoldGlintDrawable? = null): android.graphics.drawable.Drawable? {
        // vc160 Da Vinci Pro Gold v1 4.2: gold padlock (ic_pro_lock kept, not overwritten).
        val d = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_pro_lock_gold) ?: return null
        val px = (com.ccos.retro.pro.ProChip.LOCK_BADGE_DP * resources.displayMetrics.density).toInt()
        val layers = if (glint != null) arrayOf(d, glint) else arrayOf(d)
        return android.graphics.drawable.LayerDrawable(layers).apply {
            for (i in layers.indices) {
                setLayerGravity(i, Gravity.TOP or Gravity.END)
                setLayerSize(i, px, px)
            }
        }
    }

    /** DEBUG only. adb shell am start -S -n com.liverockettracker/com.ccos.retro.ui.MainActivity --es com.ccos.retro.extra.PRO_GOLD_PREVIEW moment */
    private fun runGoldPreview(what: String) {
        if (!com.ccos.retro.BuildConfig.DEBUG) return
        val root = window.decorView
        when (what) {
            "row" -> {
                goldRowPreview = true
                bindProRow()
                root.postDelayed({
                    val row = findViewById<View>(R.id.row_lrt_pro) ?: return@postDelayed
                    (findViewById<android.view.ViewGroup>(android.R.id.content)?.getChildAt(0) as? android.widget.ScrollView)
                        ?.smoothScrollTo(0, (row.top - 200).coerceAtLeast(0))
                }, 400L)
            }
            "sheet", "moment", "static" -> root.postDelayed({
                val p = when (what) { "sheet" -> ProUi.GoldPreview.SHEET; "moment" -> ProUi.GoldPreview.MOMENT; else -> ProUi.GoldPreview.STATIC }
                ProUi.showUpsell(this, prefs, goldPreview = p)
            }, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        // vc159 B4: debug builds only. adb shell am start -n com.liverockettracker/com.ccos.retro.ui.MainActivity --ez <EXTRA> true|false
        val forceExtra = com.ccos.retro.pro.ProConfig.EXTRA_DEBUG_FORCE_GATES
        if (com.ccos.retro.BuildConfig.DEBUG && intent?.hasExtra(forceExtra) == true) {
            val on = intent.getBooleanExtra(forceExtra, false)
            prefs.proStore().putBoolean(com.ccos.retro.pro.ProConfig.KEY_DEBUG_FORCE_GATES, on)
            com.ccos.retro.pro.ProConfig.debugForceGates = on
        }
        MissionTapeStore.ensure(this)
        prefs.restoreCommandPageHud()
        prefs.ensurePaidAppDefaults()
        if (!prefs.setupComplete) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        if (com.ccos.retro.BuildConfig.DEBUG) goldPreviewPending = intent?.getStringExtra(ProGold.EXTRA_PRO_GOLD_PREVIEW)
        launchProvider = LaunchDataProvider()
        launchProvider.bindAppContext(applicationContext)
                telemetryModule = RocketTelemetryModule(prefs, launchProvider)
        // vc159: a refused Pro gate (user tap on a locked replay / jump / skip) opens the sheet here.
        telemetryModule.onProBlocked = { hook, l ->
            runOnUiThread {
                ProUi.showUpsell(this, prefs, l.takeIf { hook.kind == ProHook.Kind.LAUNCH }) { onProChanged() }
            }
        }
        // B6 (Chris 08:11/08:13, R-NO-BADGER): free hour after liftoff is up -> one brief fading note, never the sheet.
        telemetryModule.onLiveAllowanceUsed = { _, next ->
            runOnUiThread { ProUi.showFreeHourNote(this, null, next) }
        }
        // Stamp 63 A: onCreate ensureData -> forceRefresh if null -> resolveTracked -> keepTrackedOrLastGood
        telemetryModule.ensureData()
        if (telemetryModule.tracked == null) {
            telemetryModule.forceRefresh {
                runOnUiThread {
                    telemetryModule.syncLeaveTheaterFromPeer()
                    telemetryModule.resolveTracked()
                    telemetryModule.keepTrackedOrLastGood()
                    populateLaunchSpinner()
                    refreshTrackingUi()
                }
            }
        } else {
            telemetryModule.syncLeaveTheaterFromPeer()
            telemetryModule.resolveTracked()
            telemetryModule.keepTrackedOrLastGood()
        }



        wireConsoleSkin()
        wireProRow()

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
            telemetryModule.syncLeaveTheaterFromPeer()
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
            telemetryModule.syncLeaveTheaterFromPeer()
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
                historicSearchDebouncePending = historicQuery.isNotBlank() && !SearchAliases.wantsCatalog(historicQuery)
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
                    // vc159 (v1 section 3): locked row -> do not start it, restore the previous pick, open the sheet.
                    if (!ProPolicy.allowsLaunch(ProHook.SELECT_LAUNCH, isPro(), launch)) {
                        val sp = parent as? Spinner
                        val prev = launchList.indexOfFirst { it.id == prefs.telemetryLaunchId }
                            .takeIf { it >= 0 }
                            ?: launchList.indexOfFirst { ProPolicy.allowsLaunch(ProHook.PICKER_ROW, false, it) }
                                .coerceAtLeast(0)
                        if (sp != null) {
                            suppressLaunchSelect = true
                            sp.setSelection(prev, false)
                            sp.post { sp.post { suppressLaunchSelect = false } }
                        }
                        ProUi.showUpsell(this@MainActivity, prefs, launch) { onProChanged() }
                        return
                    }
                    // Stamp 60: real USER id change always selectLaunch (AUTO off + stick THAT bird).
                    // Programmatic populate still blocked by suppressLaunchSelect.
                    val alreadyTracking = launch.id == prefs.telemetryLaunchId && launch.id == telemetryModule.tracked?.id
                    // Historic tap of the bird already on screen must still enter replay (Flight 14).
                    val historicReplay = prefs.telemetryListMode == "historical" && launch.isReplayable()
                    if (alreadyTracking && !historicReplay) return
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
                telemetryModule.syncLeaveTheaterFromPeer()
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
        rowGlintArmed = true
        lockGlintArmed = true
        // Stamp 57: never force AUTO on resume  -  MANUAL pick must stick (Soyuz stay Soyuz).
        // tip150: re-tint settings chrome from prefs.consoleSkin (tip149 forLaunch may change it off-screen).
        // Chris 8:22: Pro theme gate first, so a free user never resumes on a Pro theme.
        prefs.enforceThemeGate(isPro())
        applyConsoleSkin()
        refreshTrackingUi()
        // vc159: entitlement refresh (stub billing = Unavailable, cache kept).
        ProUi.access(prefs).refresh { runOnUiThread { bindProRow() } }
        bindProRow()
        goldPreviewPending?.let { runGoldPreview(it) }
        goldPreviewPending = null
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
        if (q.isBlank() || SearchAliases.wantsCatalog(q)) return
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
                // Stamp 112: sync box so auto-refresh never invents blank mid-type.
                syncHistoricQueryFromBox()
                // Stamp 106/107/110: local pool only. LL2 search debounced separately.
                val q = historicQuery
                var pool = if (q.isNotBlank()) {
                    launchProvider.historicPool(now, q)
                } else {
                    launchProvider.historicPool(now, q).filter {
                        it.id.startsWith("demo-") || it.isReplayable(now) || it.secondsToNet(now) <= 0
                    }
                }
                // Stamp 112 light: do not prepend CURRENT live selection into SEARCH HISTORIC.
                if (q.isNotBlank()) {
                    val liveId = telemetryModule.tracked?.takeIf { it.secondsToNet(now) > 0 }?.id
                    if (liveId != null) pool = pool.filter { it.id != liveId }
                }
                if (q.isNotBlank()) pool.take(80) else pool.take(20)
            } else {
                val keepId = prefs.telemetryLaunchId
                // Stamp 55: CURRENT keep only HOLD/in-flight/webcast/T+/upcoming  -  past -> HISTORIC.
                // Built-in fabricated / sample rows never appear as a real countdown (even in debug).
                val keep = liveOnly.firstOrNull { it.id == keepId }
                    ?: launchProvider.findById(keepId)?.takeIf {
                        LaunchDataProvider.keepInLiveUpcomingList(it, now)
                    }
                    ?: telemetryModule.tracked?.takeIf {
                        it.id == keepId && LaunchDataProvider.keepInLiveUpcomingList(it, now)
                    }
                val base = liveOnly.toMutableList()
                if (keep != null && base.none { it.id == keep.id }) base.add(0, keep)
                base.sortedBy { it.netMs }.take(20)
            }

            val tags = launchList.map { l ->
                val secs = l.secondsToNet(now)
                // v1.1 copy (Da Vinci + erratum): [INCLUDED] / [PRO]; never "demo" on screen.
                PickerTag.proTag(l, historic, now).takeIf { com.ccos.retro.pro.ProConfig.gatesEnabled() } ?: when {
                    l.isHold() -> "[HOLD] "
                    l.isActiveWatch(now) && secs <= 0 -> "[LIVE] "
                    // A finished flight stays Past inside the 48h window (Flight 14). T+ is only a non-terminal clock.
                    l.isTerminal() || secs < -LaunchWindow.PICKER_LOOKBACK_SEC -> "[PAST] "
                    secs < 0 -> "[T+] "
                    secs < 86400 -> "[${secs / 3600}h] "
                    else -> "[${secs / 86400}d] "
                }
            }
            val labels = launchList.mapIndexed { i, l -> "${tags[i]}${l.name.take(36)} | ${l.provider.take(12)}" }
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
            spinner.adapter = if (historic && labels.isNotEmpty() && !isPro()) {
                // vc159 (v1 section 3): free users see lock / Included rows in HISTORIC only.
                ProLaunchRowAdapter(this, launchList, labels, tags, now)
            } else {
                ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    if (labels.isEmpty()) listOf(emptyMsg) else labels
                )
            }
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
        findViewById<View>(R.id.box_historic_data)?.visibility = if (historic) View.VISIBLE else View.GONE

        styleChip(R.id.btn_list_current, !historic)
        styleChip(R.id.btn_list_historical, historic)
        // vc159 (v1 section 2): HISTORIC is never locked; free users see a 20dp badge + description.
        findViewById<Button>(R.id.btn_list_historical)?.let { b ->
            val free = !isPro()
            val badge = if (free) androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_pro_lock)?.apply {
                val px = (20 * resources.displayMetrics.density).toInt()
                setBounds(0, 0, px, px)
            } else null
            b.setCompoundDrawablesRelative(null, null, badge, null)
            b.compoundDrawablePadding = (12 * resources.displayMetrics.density).toInt()
            b.contentDescription = if (free) getString(R.string.pro_cd_historic_button) else null
        }
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
            b.textSize = 11f
        }

    private fun applyConsoleSkin() {
        val section = findViewById<LinearLayout>(R.id.section_console_skin) ?: return
        val title = findViewById<TextView>(R.id.txt_console_title)
        val help = findViewById<TextView>(R.id.txt_console_help)
        val buttons = listOf(
            findViewById<Button>(R.id.btn_console_mcc),
            findViewById<Button>(R.id.btn_console_ros),
            findViewById<Button>(R.id.btn_console_spacex),
            findViewById<Button>(R.id.btn_console_nasa),
            findViewById<Button>(R.id.btn_console_cnsa),
            findViewById<Button>(R.id.btn_console_ariane),
            findViewById<Button>(R.id.btn_console_rlab),
            findViewById<Button>(R.id.btn_console_ula),
            findViewById<Button>(R.id.btn_console_jaxa),
            findViewById<Button>(R.id.btn_console_isro)
        )
        val labels = AppPrefs.ROCKER_LABELS_CONSOLE
        val selectedBg = intArrayOf(
            R.drawable.chip_console_selected_mcc,
            R.drawable.chip_console_selected_ros,
            R.drawable.chip_console_selected_spacex,
            R.drawable.chip_console_selected_nasa,
            R.drawable.chip_console_selected_cnsa,
            R.drawable.chip_console_selected_arianespace,
            R.drawable.chip_console_selected_rocketlab,
            R.drawable.chip_console_selected_ula,
            R.drawable.chip_console_selected_jaxa,
            R.drawable.chip_console_selected_isro
        )
        val idleBg = intArrayOf(
            R.drawable.chip_console_idle_mcc,
            R.drawable.chip_console_idle_ros,
            R.drawable.chip_console_idle_spacex,
            R.drawable.chip_console_idle_nasa,
            R.drawable.chip_console_idle_cnsa,
            R.drawable.chip_console_idle_arianespace,
            R.drawable.chip_console_idle_rocketlab,
            R.drawable.chip_console_idle_ula,
            R.drawable.chip_console_idle_jaxa,
            R.drawable.chip_console_idle_isro
        )
        // selected text / idle text per pack tokens
        val selText = intArrayOf(
            0xFF1A1200.toInt(), 0xFFE8E0D0.toInt(), 0xFFF0F0F0.toInt(), 0xFFE8EEF8.toInt(),
            0xFFF5E6C8.toInt(), 0xFFFFFFFF.toInt(), 0xFFF2F2F2.toInt(), 0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(), 0xFFFFF4E0.toInt()
        )
        val idleText = intArrayOf(
            0xFF8A7050.toInt(), 0xFF6A7850.toInt(), 0xFF6A6A6A.toInt(), 0xFF6A7A90.toInt(),
            0xFF8A6060.toInt(), 0xFF6A7090.toInt(), 0xFF6A6A6A.toInt(), 0xFF5A6A88.toInt(),
            0xFF8A6068.toInt(), 0xFF8A6848.toInt()
        )
        val skin = prefs.consoleSkin
        val sel = AppPrefs.consoleSkinIndex(skin)
        title?.visibility = View.VISIBLE
        title?.text = "CONSOLE"
        help?.text = "Skins page past eight when needed · JAXA ISRO on the next row · same on CMD flyout"
        // tip148: each of 8 skins gets distinct section bg + title/help + root/content tint
        // accents match wallpaper consoleChipAccent / consoleFrameAccent
        when (skin) {
            AppPrefs.CONSOLE_SKIN_ROS -> {
                section.setBackgroundResource(R.drawable.bg_console_ros)
                title?.setTextColor(0xFFE8F0D8.toInt())
                help?.setTextColor(0xFFB8C890.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF0A0C08.toInt())
            }
            AppPrefs.CONSOLE_SKIN_SPACEX -> {
                section.setBackgroundResource(R.drawable.bg_console_spacex)
                title?.setTextColor(0xFF00D26A.toInt())
                help?.setTextColor(0xFFA0A0A0.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF050505.toInt())
            }
            AppPrefs.CONSOLE_SKIN_NASA -> {
                section.setBackgroundResource(R.drawable.bg_console_nasa)
                title?.setTextColor(0xFF6A9AD4.toInt())
                help?.setTextColor(0xFF8AA0B8.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF080C14.toInt())
            }
            AppPrefs.CONSOLE_SKIN_CNSA -> {
                section.setBackgroundResource(R.drawable.bg_console_cnsa)
                title?.setTextColor(0xFFDE2910.toInt())
                help?.setTextColor(0xFFB08070.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF0C0606.toInt())
            }
            AppPrefs.CONSOLE_SKIN_ARIANE -> {
                section.setBackgroundResource(R.drawable.bg_console_ariane)
                title?.setTextColor(0xFFFFD100.toInt())
                help?.setTextColor(0xFFA0A888.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF060A14.toInt())
            }
            AppPrefs.CONSOLE_SKIN_RLAB -> {
                section.setBackgroundResource(R.drawable.bg_console_rlab)
                title?.setTextColor(0xFFFF5A1F.toInt())
                help?.setTextColor(0xFFA88878.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF080606.toInt())
            }
            AppPrefs.CONSOLE_SKIN_ULA -> {
                section.setBackgroundResource(R.drawable.bg_console_ula)
                title?.setTextColor(0xFFF6A800.toInt())
                help?.setTextColor(0xFFA89870.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF060E1C.toInt())
            }
            AppPrefs.CONSOLE_SKIN_JAXA -> {
                section.setBackgroundResource(R.drawable.bg_console_jaxa)
                title?.setTextColor(0xFFE10600.toInt())
                help?.setTextColor(0xFFB08080.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF100608.toInt())
            }
            AppPrefs.CONSOLE_SKIN_ISRO -> {
                section.setBackgroundResource(R.drawable.bg_console_isro)
                title?.setTextColor(0xFFFF671F.toInt())
                help?.setTextColor(0xFFC09878.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF100C08.toInt())
            }
            else -> {
                // MCC amber
                section.setBackgroundResource(R.drawable.bg_console_mcc)
                title?.setTextColor(0xFFFFB000.toInt())
                help?.setTextColor(0xFF8AA0B0.toInt())
                findViewById<View>(android.R.id.content)?.setBackgroundColor(0xFF0A0E14.toInt())
            }
        }
        val pro = isPro()
        val store = prefs.proStore()
        // v2 section 2: one #FFFACD glint at each gold padlock's ring, staggered over the six slots, once per open.
        val glintLocks = lockGlintArmed && ProGold.idleGlintAllowed(this, com.ccos.retro.pro.ProConfig.gatesEnabled())
        lockGlintArmed = false
        var lockN = 0
        for (i in buttons.indices) {
            val b = buttons[i]
            b?.text = labels[i]
            styleConsoleChip(b, i == sel, selectedBg[i], idleBg[i], selText[i], idleText[i])
            // vc159 (v1 sections 1, 8): lock badge on Pro themes for free users; an Auto-applied
            // Pro theme (the selected chip) shows no badge.
            val skinId = AppPrefs.CONSOLE_SKIN_IDS.getOrNull(i) ?: continue
            val locked = !ProPolicy.allowsTheme(ProHook.THEME_PICK_SETTINGS, pro, skinId, store)
            val badged = locked && i != sel
            // Da Vinci 13:18 CT art gate (2): the label never sits under the padlock badge. Inset its end by the badge.
            b?.let { it.setPaddingRelative(it.paddingStart, it.paddingTop, com.ccos.retro.pro.ProChip.endPadPx(badged, resources.displayMetrics.density), it.paddingBottom) }
            b?.foreground = if (badged) {
                val g = if (glintLocks && b != null) GoldGlintDrawable(listOf(ProGold.LOCK_GLINT_POINT), listOf(lockN++ % 6),
                    ProGold.GOLD_PALE, null, 2.5f * resources.displayMetrics.density) else null
                chipLockForeground(g).also { if (g != null && b != null) ProGold.startIdleGlint(b, g, true) }
            } else null
            b?.contentDescription = getString(
                if (locked) R.string.pro_cd_theme_locked else R.string.pro_cd_theme_free, labels[i]
            )
        }
        // Da Vinci 13:18 CT art gate (1): row 3 (JAXA, ISRO) uses the same 40dp / 11sp / 2dp-margin chips as rows 1-2
        // in activity_main.xml. The old runtime resize (efe00ee4, 2026-09-29: height = 10% of width, text 32%) is gone.
        findViewById<View>(R.id.section_telemetry)?.let { tel ->
            when (skin) {
                AppPrefs.CONSOLE_SKIN_ROS -> tel.setBackgroundResource(R.drawable.bg_console_ros)
                AppPrefs.CONSOLE_SKIN_SPACEX -> tel.setBackgroundResource(R.drawable.bg_console_spacex)
                AppPrefs.CONSOLE_SKIN_NASA -> tel.setBackgroundResource(R.drawable.bg_console_nasa)
                AppPrefs.CONSOLE_SKIN_CNSA -> tel.setBackgroundResource(R.drawable.bg_console_cnsa)
                AppPrefs.CONSOLE_SKIN_ARIANE -> tel.setBackgroundResource(R.drawable.bg_console_ariane)
                AppPrefs.CONSOLE_SKIN_RLAB -> tel.setBackgroundResource(R.drawable.bg_console_rlab)
                AppPrefs.CONSOLE_SKIN_ULA -> tel.setBackgroundResource(R.drawable.bg_console_ula)
                AppPrefs.CONSOLE_SKIN_JAXA -> tel.setBackgroundResource(R.drawable.bg_console_jaxa)
                AppPrefs.CONSOLE_SKIN_ISRO -> tel.setBackgroundResource(R.drawable.bg_console_isro)
                AppPrefs.CONSOLE_SKIN_MCC -> tel.setBackgroundResource(R.drawable.panel_console)
                else -> tel.setBackgroundResource(R.drawable.bg_console_mcc)
            }
        }
        (findViewById<View>(android.R.id.content) as? android.view.ViewGroup)?.getChildAt(0)?.setBackgroundColor(
            when (skin) {
                AppPrefs.CONSOLE_SKIN_ROS -> 0xFF1A2010.toInt()
                AppPrefs.CONSOLE_SKIN_SPACEX -> 0xFF0C0C0C.toInt()
                AppPrefs.CONSOLE_SKIN_NASA -> 0xFF0A1220.toInt()
                AppPrefs.CONSOLE_SKIN_CNSA -> 0xFF140808.toInt()
                AppPrefs.CONSOLE_SKIN_ARIANE -> 0xFF0A1020.toInt()
                AppPrefs.CONSOLE_SKIN_RLAB -> 0xFF101010.toInt()
                AppPrefs.CONSOLE_SKIN_ULA -> 0xFF0A1830.toInt()
                AppPrefs.CONSOLE_SKIN_JAXA -> 0xFF18080C.toInt()
                AppPrefs.CONSOLE_SKIN_ISRO -> 0xFF181008.toInt()
                else -> 0xFF0A0E14.toInt() // MCC
            }
        )
        applySettingsBoard()
    }

    // vc159 (v1 section 8): locked chip opens the sheet and saves nothing; free picks save via recordManualPick.
    private fun wireConsoleSkin() {
        fun pick(id: String) {
            val store = prefs.proStore()
            val pro = isPro()
            if (!ProPolicy.allowsTheme(ProHook.THEME_PICK_SETTINGS, pro, id, store)) {
                ProUi.showUpsell(this, prefs) { onProChanged() }
                return
            }
            ThemeAccess.recordManualPick(store, id, pro)
            prefs.consoleSkin = id
            applyConsoleSkin()
        }
        val ids = AppPrefs.CONSOLE_SKIN_IDS
        val btnIds = intArrayOf(
            R.id.btn_console_mcc, R.id.btn_console_ros, R.id.btn_console_spacex, R.id.btn_console_nasa,
            R.id.btn_console_cnsa, R.id.btn_console_ariane, R.id.btn_console_rlab, R.id.btn_console_ula,
            R.id.btn_console_jaxa, R.id.btn_console_isro
        )
        for (i in btnIds.indices) {
            val skinId = ids[i]
            findViewById<Button>(btnIds[i])?.setOnClickListener { pick(skinId) }
        }
        applyConsoleSkin()
    }

    private data class SettingsBoard(
        val header: String,
        val accent: Int,
        val tracking: String,
        val live: String,
        val current: String,
        val historic: String,
        val historicButton: String,
        val search: String,
        val refresh: String,
        val follow: String,
        val auto: String,
        val manual: String,
        val units: String,
        val home: String
    )

    private fun settingsBoard(skin: String): SettingsBoard {
        fun line(en: String, local: String) = if (local.isEmpty()) en else "$en $local"
        fun btn(en: String, local: String) = if (local.isEmpty()) en else "$en\n$local"
        val local = when (skin) {
            AppPrefs.CONSOLE_SKIN_ROS -> listOf(
                "РОСКОСМОС", "СЛЕЖЕНИЕ", "ПРЯМОЙ", "ТЕКУЩИЙ", "ИСТОРИЯ", "ИСТОРИЯ",
                "ПОИСК", "ОБНОВИТЬ", "СЛЕЖЕНИЕ", "АВТО", "РУЧНОЙ", "ЕДИНИЦЫ", "ЭКРАНЫ"
            )
            AppPrefs.CONSOLE_SKIN_CNSA -> listOf(
                "CASC 中国航天", "跟踪", "实时", "当前", "历史", "历史",
                "搜索历史", "刷新", "跟踪", "自动", "手动", "单位", "主屏"
            )
            AppPrefs.CONSOLE_SKIN_ARIANE -> listOf(
                "ESA / ARIANESPACE", "SUIVI", "DIRECT", "ACTUEL", "HISTORIQUE", "HISTORIQUE",
                "RECHERCHE", "ACTUALISER", "SUIVI", "AUTO", "MANUEL", "UNITÉS", "ÉCRANS"
            )
            AppPrefs.CONSOLE_SKIN_JAXA -> listOf(
                "JAXA 宇宙航空", "追跡", "ライブ", "現在", "履歴", "履歴",
                "検索", "更新", "追尾", "自動", "手動", "単位", "画面"
            )
            AppPrefs.CONSOLE_SKIN_ISRO -> listOf(
                "ISRO इसरो", "ट्रैक", "लाइव", "वर्तमान", "इतिहास", "इतिहास",
                "खोज", "ताज़ा", "अनुसरण", "स्वतः", "मैनुअल", "इकाई", "स्क्रीन"
            )
            AppPrefs.CONSOLE_SKIN_SPACEX -> listOf("SPACEX") + List(12) { "" }
            AppPrefs.CONSOLE_SKIN_NASA -> listOf("NASA") + List(12) { "" }
            AppPrefs.CONSOLE_SKIN_RLAB -> listOf("ROCKET LAB") + List(12) { "" }
            AppPrefs.CONSOLE_SKIN_ULA -> listOf("ULA") + List(12) { "" }
            else -> listOf("MCC") + List(12) { "" }
        }
        val accent = when (skin) {
            AppPrefs.CONSOLE_SKIN_ROS -> 0xFFFFD100.toInt()
            AppPrefs.CONSOLE_SKIN_SPACEX -> 0xFF00D26A.toInt()
            AppPrefs.CONSOLE_SKIN_NASA -> 0xFF6A9AD4.toInt()
            AppPrefs.CONSOLE_SKIN_CNSA -> 0xFFF2C14E.toInt()
            AppPrefs.CONSOLE_SKIN_ARIANE -> 0xFFFFD100.toInt()
            AppPrefs.CONSOLE_SKIN_RLAB -> 0xFFFF5A1F.toInt()
            AppPrefs.CONSOLE_SKIN_ULA -> 0xFF6A9AD4.toInt()
            AppPrefs.CONSOLE_SKIN_JAXA -> 0xFFE10600.toInt()
            AppPrefs.CONSOLE_SKIN_ISRO -> 0xFFFF671F.toInt()
            else -> 0xFFFFB000.toInt()
        }
        return SettingsBoard(
            header = local[0],
            accent = accent,
            tracking = line("TRACKING", local[1]),
            live = line("LIVE", local[2]),
            current = btn("CURRENT", local[3]),
            // vc159: HISTORIC badge + locked rows are wired in refreshTrackingUi / ProLaunchRowAdapter.
            historic = line("HISTORIC", local[4]),
            historicButton = btn("HISTORIC", local[5]),
            search = line("SEARCH HISTORIC", local[6]),
            refresh = btn("REFRESH", local[7]),
            follow = line("FOLLOW", local[8]),
            auto = btn("AUTO ON", local[9]),
            manual = btn("MANUAL", local[10]),
            units = line("UNITS", local[11]),
            home = line("HOME SCREENS", local[12])
        )
    }

    private fun applySettingsBoard() {
        val b = settingsBoard(prefs.consoleSkin)
        fun label(id: Int, text: String) {
            findViewById<TextView>(id)?.apply {
                this.text = text
                setTextColor(b.accent)
            }
        }
        fun button(id: Int, text: String) {
            findViewById<Button>(id)?.apply {
                this.text = text
                isAllCaps = false
            }
        }
        label(R.id.txt_agency_header, b.header)
        label(R.id.txt_tracking_label, b.tracking)
        label(R.id.txt_live_label, b.live)
        label(R.id.txt_historic_label, b.historic)
        label(R.id.txt_search_historic, b.search)
        label(R.id.txt_follow_label, b.follow)
        label(R.id.txt_units_label, b.units)
        label(R.id.txt_home_screens, b.home)
        button(R.id.btn_list_current, b.current)
        button(R.id.btn_list_historical, b.historicButton)
        button(R.id.btn_refresh_launches, b.refresh)
        button(R.id.btn_auto_on, b.auto)
        button(R.id.btn_auto_off, b.manual)
    }


}
