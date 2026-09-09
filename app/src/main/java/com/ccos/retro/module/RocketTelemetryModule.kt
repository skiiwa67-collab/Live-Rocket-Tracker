package com.ccos.retro.module

import android.util.Log

import com.ccos.retro.data.LaunchDataProvider
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.data.LaunchWindow
import com.ccos.retro.event.FlightEventCatalog
import com.ccos.retro.event.FlightProfiles
import com.ccos.retro.model.AppPrefs

/**
 * Live Rocket Telemetry Module — mission-control surface for any tracked launch.
 *
 * Buttons (on command page):
 *  0 CMD  → settings / launch picker panel
 *  1 CDT  → big countdown + T- clock
 *  2 TEL  → altitude / speed / trajectory gauges (profile or live)
 *  3 STS  → status board (Go / Hold / In Flight + reasons)
 *  4 PAD  → pad + location + weather placeholder
 *  5 VID  → webcast hint / open stream action
 *  6 MSK  → mission overview + agency
 *  7 AUTO → browse auto-next; double-tap pin/unpin (lock lamp)
 *
 * Selecting a launch (or Auto) reskins the entire surface via agency tokens.
 */
class RocketTelemetryModule(
    private val prefs: AppPrefs,
    private val provider: LaunchDataProvider
) : Module {

    override val id = AppPrefs.MODULE_TELEMETRY
    override val displayName = "Live Rocket Telemetry"
    override val isFree = false
    override val playProductId = ModuleCatalog.PRODUCT_LIVE_TELEMETRY
    override val buttonLabels = arrayOf("CMD", "CDT", "TEL", "STS", "PAD", "VID", "MSK", "AUTO")

    /** Which telemetry page is active (0–7). */
    var activePage: Int = 0   // CMD is the home view
        private set

    /** Currently tracked launch (null = none / waiting for data). */
    var tracked: LaunchSnapshot? = null
        private set

    /** Stamp 57: log once on null or tracked-id change (no per-frame spam). */
    private var lrt57LogKey: String? = null

    /**
     * Simulated seconds relative to NET for scrubbing historical/demo flights.
     * null = live wall-clock. Negative = before T-0, positive = after liftoff.
     * When set, countdown + metrics + attitude use this instead of real time.
     */
    var simSecondsFromNet: Float? = null

    /** When true, after reaching late phase, loop demo back to T-120 (wallpaper theater). */
    var loopReplay: Boolean = false

    private var lastCursorWriteMs: Long = 0L

    /** Force status overlay e.g. "Scrubbed" for testing. */
    var forceStatus: String? = null

    /** Last snapshot that matched the pin id. Survives a catalog miss of findById. */
    private var pinnedSnapshot: LaunchSnapshot? = null

    /** Last snapshot under HOLD. Survives findById miss so tracked is never null for hours. */
    private var holdSnapshot: LaunchSnapshot? = null
    /** Stamp 58: never lose HUD bird while AUTO/id set. */
    private var lastGoodSnapshot: LaunchSnapshot? = null

    companion object {
        /** Stamp 59: Activity + wallpaper Engine share last HUD bird. */
        @Volatile var sharedLastGood: LaunchSnapshot? = null
    }

    /**
     * Auto browse: follow live AUTO window.
     * Pin (LCK) gates browse off, but prefs.telemetryAuto keeps FOLLOW chip intent —
     * do not force MANUAL label merely because LCK is on.
     */
    var autoMode: Boolean
        get() = prefs.telemetryAuto && !prefs.telemetryPinned
        set(v) {
            if (v && prefs.telemetryPinned) {
                // Pin wins browse; keep FOLLOW intent bit unless caller clears pin first.
                return
            }
            prefs.telemetryAuto = v
        }

    override fun onModuleButton(index: Int): Boolean {
        if (index == 7) {
            if (prefs.telemetryPinned) {
                // Pin wins. Do not steal the pin; keep FOLLOW intent (chip stays AUTO when pinned).
                if (activePage == 7) activePage = 2
                return true
            }
            autoMode = !autoMode
            if (autoMode) {
                // Always drop leftover HOLD so AUTO can browse; resolveTracked
                // may re-arm a short wall-clock hold if something is actively watched.
                releaseHold()
                // Stamp 79: historic/demo LCK or HISTORICAL — do NOT clearSim / restart theater.
                val hist = prefs.telemetryListMode == "historical" ||
                    (tracked?.let { it.id.startsWith("demo-") || it.isReplayable() } == true)
                if (!hist) clearSim()
                resolveTracked()
            }
            // AUTO is a lamp, not a page — never open the old overlay screen.
            if (activePage == 7) activePage = 2
            return true
        }
        activePage = index.coerceIn(0, 6)
        val launch = tracked
        if (index == 1 && launch != null && launch.isReplayable() && simSecondsFromNet == null) {
            jumpTo(-30f)
        }
        return true
    }

    /** Jump simulation to a key mission timestamp (seconds from NET). */
    fun jumpTo(secFromNet: Float) {
        simSecondsFromNet = secFromNet
        forceStatus = null
        loopReplay = true
        // Stamp 79: keep FOLLOW bit if historic LCK; scrub must not drop pin theater.
        if (!(prefs.telemetryPinned && isHistoricOrDemoPin(tracked))) {
            autoMode = false
        }
        persistEventCursor(secFromNet)
    }

    /** Jump to the next (+) or previous (-) catalog event. For testing, not a 2-hour sit. */
    fun skipEvent(dir: Int) {
        val launch = tracked ?: return
        val t = effectiveSecondsFromNet()
        val marks = FlightEventCatalog.timeline(launch).map { it.tSec }.toMutableList()
        for (p in floatArrayOf(-60f, -30f, -10f)) {
            if (marks.none { kotlin.math.abs(it - p) < 0.5f }) marks.add(p)
        }
        marks.sort()
        val target = if (dir > 0) {
            marks.firstOrNull { it > t + 0.25f }
                ?: (t + 25f).coerceAtMost(com.ccos.retro.event.FlightProfiles.replayEndSec(launch))
        } else {
            marks.lastOrNull { it < t - 0.25f } ?: (t - 30f).coerceAtLeast(-120f)
        }
        jumpTo(target)
    }


    fun markScrubbed() {
        forceStatus = "Scrubbed"
        simSecondsFromNet = -300f
        autoMode = false
        persistEventCursor(-300f)
    }

    fun clearSim(forId: String? = tracked?.id) {
        // Stamp 77/79: historic/demo LCK or HISTORICAL theater — keep sim/cursor (scrub must not blank).
        val pinKeep = prefs.telemetryPinned && isHistoricOrDemoPin(tracked)
        val histKeep = prefs.telemetryListMode == "historical" &&
            tracked != null && (tracked!!.id.startsWith("demo-") || tracked!!.isReplayable())
        if (pinKeep || histKeep) {
            loopReplay = true
            return
        }
        simSecondsFromNet = null
        forceStatus = null
        loopReplay = false
        // Stamp 55 smoking gun: null memory AND remove persisted cursor for this id.
        val id = forId
        if (!id.isNullOrBlank()) {
            prefs.setEventCursorSec(id, null)
            lastCursorWriteMs = System.currentTimeMillis()
        }
    }

    /** Effective seconds from NET for display/metrics (sim or real). */
    fun effectiveSecondsFromNet(now: Long = System.currentTimeMillis()): Float {
        val launch = tracked ?: return 0f
        // Memory sim first. Prefs cursor only for historic/demo replay — never theater a live bird.
        val mem = simSecondsFromNet
        if (mem != null) return mem
        if (launch.isReplayable(now)) {
            val cur = prefs.eventCursorSec(launch.id)
            if (cur != null) return cur
        }
        val t0 = prefs.pinnedNetMs(launch.id, launch.netMs)
        return (now - t0) / 1000f
    }

    fun clockIsEst(): Boolean {
        val launch = tracked ?: return false
        if (simSecondsFromNet != null) return true
        if (launch.isReplayable() && prefs.eventCursorSec(launch.id) != null) return true
        if (prefs.usingFallbackT0(launch.id, launch.netMs)) return true
        return provider.catalogStale()
    }

    /**
     * Advance sim at 1× realtime for wallpaper theater.
     * Loops back to T-30 after ~10 min post-liftoff so demos keep running.
     */
    fun tickSim(deltaSec: Float) {
        val s = simSecondsFromNet ?: return
        if (!loopReplay) return
        var next = s + deltaSec.coerceIn(0f, 0.25f)
        val end = FlightProfiles.replayEndSec(tracked)
        val chipSec = (prefs.telemetryHoldDurationMs / 1000L).toFloat()
        val loopAt = minOf(end, chipSec)
        if (next > loopAt) {
            // Stamp 80: LCK = same-bird chip loop; unlocked HISTORICAL advances via maybeAdvanceHistoricRoll.
            next = if (prefs.telemetryPinned) -30f else loopAt
        }
        simSecondsFromNet = next
        val now = System.currentTimeMillis()
        if (now - lastCursorWriteMs >= 500L) {
            lastCursorWriteMs = now
            persistEventCursor(next)
        }
    }



    override fun slidersFor(activeIndex: Int): List<SliderDef> = when (activeIndex) {
        0 -> listOf(
            // text scale already handled globally on CMD; keep module-specific empty or future
        )
        else -> emptyList()
    }

    /** Call from engine tick / visibility to keep tracked launch current. */


    private var lastBoundTrackedId: String? = null

    /** Stamp 63: bumps when tracked id changes so HUD/VID/agency caches can drop prior bird chrome. */
    var trackedGeneration: Long = 0L
        private set

    private fun rememberTracked(s: LaunchSnapshot?) {
        if (s == null) return
        if (s.id != lastBoundTrackedId) {
            lastBoundTrackedId = s.id
            trackedGeneration++
            // Do not keep prior-flight theater/cursor on a new bird (agency mix FAIL).
            if (!s.id.startsWith("demo-") && simSecondsFromNet != null) {
                clearSim()
            }
        }
        tracked = s
        lastGoodSnapshot = s
        sharedLastGood = s
    }

    /**
     * Stamp 63 HISTORIC rolling: without LCK, advance to next historic after NET+chip into the flight.
     * LCK pins one historic (no auto-advance).
     */
    /** Stamp 77: historic/demo LCK is pin-session — never expire on wall NET+duration. */
    private fun isHistoricOrDemoPin(launch: LaunchSnapshot?, now: Long = System.currentTimeMillis()): Boolean {
        if (launch == null) return false
        if (launch.id.startsWith("demo-")) return true
        // Historical list mode + any in-theater sim: never wall-clock kill LCK (pastCeiling is always true for past NET).
        if (prefs.telemetryListMode == "historical") return true
        if (loopReplay || simSecondsFromNet != null) return true
        return launch.isReplayable(now)
    }

    /** Stamp 78: HISTORICAL same-launch chip loop — NEVER advance to next historic/Soyuz. */
    private fun maybeAdvanceHistoricRoll(now: Long) {
        if (prefs.telemetryListMode != "historical") return
        // Stamp 80: LCK pins one bird. Unlocked AUTO cycles historic list.
        if (prefs.telemetryPinned) return
        val cur = tracked ?: return
        if (!cur.isReplayable(now) && !cur.id.startsWith("demo-")) return
        val tSec = effectiveSecondsFromNet(now)
        val limitSec = (prefs.telemetryHoldDurationMs / 1000L).toFloat()
        if (tSec < limitSec) return
        val pool = provider.allSelectable().filter {
            it.id.startsWith("demo-") || it.isReplayable(now) || it.secondsToNet(now) < -60
        }.sortedBy { it.netMs }
        if (pool.isEmpty()) return
        val idx = pool.indexOfFirst { it.id == cur.id }
        val next = if (idx < 0) pool.first() else pool[(idx + 1) % pool.size]
        if (next.id == cur.id) return
        clearSim()
        rememberTracked(next)
        prefs.telemetryLaunchId = next.id
        if (next.isReplayable(now) || next.id.startsWith("demo-")) {
            jumpTo(-30f)
        }
    }

    /** Stamp 59: NEVER leave tracked null while AUTO or launch id/pin set. */
    fun keepTrackedOrLastGood(now: Long = System.currentTimeMillis()) {
        if (tracked != null) {
            lastGoodSnapshot = tracked
            sharedLastGood = tracked
            return
        }
        val id = prefs.telemetryLaunchId
        val keep = lastGoodSnapshot
            ?: sharedLastGood
            ?: holdSnapshot
            ?: pinnedSnapshot
            ?: if (id.isNotBlank()) provider.findById(id) else null
            ?: provider.getNextAny(now)
            ?: provider.getNextSpaceX(now)
            ?: provider.demoCatalog.firstOrNull()
        if (keep != null) {
            tracked = keep
            lastGoodSnapshot = keep
            sharedLastGood = keep
            if (id.isBlank()) prefs.telemetryLaunchId = keep.id
        }
    }

    fun resolveTracked(now: Long = System.currentTimeMillis()) {
        // Stamp 59: restore before resolve
        if (tracked == null) keepTrackedOrLastGood(now)
        val prevId = tracked?.id
        // Stamp 63: LCK lifetime = NET + telemetryHoldDurationMs (1H|2H|6H). Never forever-pin.
        if (prefs.telemetryPinned) {
            val expireId = prefs.telemetryLaunchId.ifBlank { tracked?.id ?: pinnedSnapshot?.id ?: "" }
            val expireLaunch = when {
                expireId.isBlank() -> null
                tracked?.id == expireId -> tracked
                pinnedSnapshot?.id == expireId -> pinnedSnapshot
                else -> provider.findById(expireId)
            }
            // Stamp 77: live birds only — historic/demo LCK stays until user unpins.
            if (expireLaunch != null
                && !isHistoricOrDemoPin(expireLaunch, now)
                && expireLaunch.netMs + prefs.telemetryHoldDurationMs <= now
            ) {
                prefs.telemetryPinned = false
                pinnedSnapshot = null
                releaseHold()
                // Fall through to AUTO / HOLD resolve below.
            }
        }
        if (prefs.telemetryPinned) {
            // Pin gates browse (autoMode getter). Keep prefs.telemetryAuto for FOLLOW chip intent.
            val pinId = prefs.telemetryLaunchId.ifBlank { tracked?.id ?: pinnedSnapshot?.id ?: "" }
            if (pinId.isNotBlank() && prefs.telemetryLaunchId.isBlank()) {
                prefs.telemetryLaunchId = pinId
            }
            val found = if (pinId.isNotBlank()) provider.findById(pinId) else null
            val pinBird = found
                ?: when {
                    tracked?.id == pinId -> tracked
                    pinnedSnapshot?.id == pinId -> pinnedSnapshot
                    else -> null
                }
            if (pinBird != null) {
                // Stamp 77: historic/demo — pin until togglePin; live keeps NET+duration / ceiling.
                val historicPin = isHistoricOrDemoPin(pinBird, now)
                val expiredByDur = !historicPin && pinBird.netMs + prefs.telemetryHoldDurationMs <= now
                val tPin = pinBird.secondsToNet(now)
                val pastCeiling = !historicPin && tPin <= -LaunchWindow.PIN_HARD_CEILING_SEC
                if (expiredByDur || pastCeiling) {
                    prefs.telemetryPinned = false
                    pinnedSnapshot = null
                    releaseHold()
                } else {
                    tracked = pinBird
                    pinnedSnapshot = pinBird
                    if (found == null && prefs.telemetryLaunchId.isBlank()) {
                        prefs.telemetryLaunchId = pinBird.id
                    }
                }
            }
            if (prefs.telemetryPinned) {
                if (pinBird == null) {
                    // Stamp 72: sticky pin/lastGood across one catalog miss — no LCK retarget.
                    fun stickyOk(s: LaunchSnapshot) =
                        !s.isTerminal() && (s.id == pinId || s.isActiveWatch(now) || s.secondsToNet(now) > 0)
                    val sticky = listOfNotNull(
                        tracked?.takeIf { it.id == pinId },
                        pinnedSnapshot?.takeIf { it.id == pinId },
                        lastGoodSnapshot?.takeIf { it.id == pinId },
                        sharedLastGood?.takeIf { it.id == pinId },
                        lastGoodSnapshot,
                        sharedLastGood,
                        tracked
                    ).firstOrNull { stickyOk(it) }
                    if (sticky != null) {
                        tracked = sticky
                        if (sticky.id == pinId) pinnedSnapshot = sticky
                    }
                    if (pinId.isNotBlank() && !provider.isFetching) {
                        val missingPin = pinId
                        provider.refreshIfNeeded(force = true) {
                            val again = provider.findById(missingPin)
                            if (again != null) {
                                tracked = again
                                pinnedSnapshot = again
                                prefs.telemetryLaunchId = missingPin
                            }
                            // Still missing: keep sticky — do not retarget pin id.
                            resolveTracked()
                        }
                    }
                }
            } else {
                // Unpinned by ceiling — fall through into AUTO resolve on this same call
                pinnedSnapshot = null
                val live = provider.livePool()
                val watch = live.filter { it.isActiveWatch(now) }
                    .minByOrNull { kotlin.math.abs(it.secondsToNet(now)) }
                val liveNext = watch ?: provider.getNextAny(now)
                fun lastGoodOk(s: LaunchSnapshot) =
                    s.isActiveWatch(now) || s.secondsToNet(now) > 0
                val next = liveNext
                    ?: lastGoodSnapshot?.takeIf { lastGoodOk(it) }
                    ?: sharedLastGood?.takeIf { lastGoodOk(it) }
                    ?: provider.getNextSpaceX(now)
                    ?: provider.getCached()?.launches
                        ?.filter { it.isUpcoming(now) }
                        ?.minByOrNull { it.netMs }
                    ?: provider.demoCatalog.firstOrNull { it.secondsToNet(now) > 0 }
                    ?: provider.demoCatalog.firstOrNull()
                if (next != null) {
                    rememberTracked(next)
                    if (prefs.telemetryLaunchId != next.id) {
                        prefs.telemetryLaunchId = next.id
                    }
                    if (watch != null) {
                        val latch = watch.isHold() || watch.isInFlight(now)
                        if (latch) {
                            val dur = prefs.telemetryHoldDurationMs
                            val until = now + dur
                            if (prefs.telemetryHoldUntilMs < until) prefs.telemetryHoldUntilMs = until
                        }
                        clearSim()
                    } else {
                        clearSim()
                    }
                }
            }
        } else if (isHolding(now)) {
            // HOLD beats AUTO for live/in-flight. Stamp 55: historic HOLD must not trap AUTO+CURRENT.
            pinnedSnapshot = null
            val holdId = prefs.telemetryLaunchId
            val found = if (holdId.isNotBlank()) provider.findById(holdId) else null
            val held = found ?: holdSnapshot?.takeIf { it.id == holdId } ?: tracked?.takeIf { it.id == holdId }
            // Stamp 78: HISTORICAL — never release historic HOLD into live AUTO (Soyuz steal).
            if (autoMode
                && prefs.telemetryListMode != "historical"
                && (held == null || held.isReplayable(now))
            ) {
                releaseHold()
                holdSnapshot = null
                resolveTracked(now)
                return
            }
            if (found != null) {
                tracked = found
                holdSnapshot = found
            } else {
                // Stamp 52 D: never leave tracked null under HOLD after a spinner switch.
                // Wallpaper cache can miss the new id briefly — keep last good snapshot,
                // force refresh, then resolve. Do NOT releaseHold / blank PAD+TEL.
                val keep = when {
                    holdSnapshot?.id == holdId -> holdSnapshot
                    tracked?.id == holdId -> tracked
                    else -> tracked ?: holdSnapshot
                }
                if (keep != null) {
                    tracked = keep
                    if (keep.id == holdId) holdSnapshot = keep
                }
                if (!provider.isFetching) {
                    provider.refreshIfNeeded(force = true) {
                        val again = if (holdId.isNotBlank()) provider.findById(holdId) else null
                        if (again != null) {
                            tracked = again
                            holdSnapshot = again
                        }
                        resolveTracked()
                    }
                }
            }
        } else if (autoMode) {
            pinnedSnapshot = null
            holdSnapshot = null
            // Stamp 78: HISTORICAL + AUTO — stay on user historic/demo; NEVER jump to live.
            if (prefs.telemetryListMode == "historical") {
                val id = prefs.telemetryLaunchId
                val keep = (if (id.isNotBlank()) provider.findById(id) else null)
                    ?: tracked?.takeIf { it.isReplayable(now) || it.id.startsWith("demo-") }
                    ?: lastGoodSnapshot?.takeIf { it.isReplayable(now) || it.id.startsWith("demo-") }
                    ?: sharedLastGood?.takeIf { it.isReplayable(now) || it.id.startsWith("demo-") }
                    ?: provider.allSelectable().filter { it.isReplayable(now) || it.id.startsWith("demo-") }
                        .maxByOrNull { it.netMs }
                    ?: provider.demoCatalog.firstOrNull()
                if (keep != null) {
                    rememberTracked(keep)
                    if (prefs.telemetryLaunchId != keep.id) prefs.telemetryLaunchId = keep.id
                }
            } else {
            // Stamp 55: CURRENT+AUTO locks live/next (HOLD/in-flight/webcast/T+ gates).
            // Never stay on a historic pick; never null tracked on a brief cache gap.
            // Stamp 59: do not force listMode every AUTO tick (CURRENT button still can).
            val live = provider.livePool()
            val watch = live.filter { it.isActiveWatch(now) }
                .minByOrNull { kotlin.math.abs(it.secondsToNet(now)) }
            // Stamp 72: sticky lastGood across one refresh miss before getNextAny retarget.
            fun lastGoodOk(s: LaunchSnapshot) =
                !s.isTerminal() && (s.isActiveWatch(now) || s.secondsToNet(now) > 0)
            val stickyMiss = listOfNotNull(tracked, lastGoodSnapshot, sharedLastGood)
                .firstOrNull { lastGoodOk(it) && provider.findById(it.id) == null }
            val liveNext = watch ?: stickyMiss ?: provider.getNextAny(now)
            val next = liveNext
                ?: lastGoodSnapshot?.takeIf { lastGoodOk(it) }
                ?: sharedLastGood?.takeIf { lastGoodOk(it) }
                ?: provider.getNextSpaceX(now)
                ?: provider.getCached()?.launches
                    ?.filter { it.isUpcoming(now) }
                    ?.minByOrNull { it.netMs }
                ?: provider.demoCatalog.firstOrNull { it.secondsToNet(now) > 0 }
                ?: provider.demoCatalog.firstOrNull()
            if (next != null) {
                rememberTracked(next)
                if (prefs.telemetryLaunchId != next.id) {
                    prefs.telemetryLaunchId = next.id
                }
                if (watch != null) {
                    // Stamp 52 A: latch HOLD only for in-flight / T+ / LL2 Hold — NOT upcoming Go.
                    val latch = watch.isHold() || watch.isInFlight(now)
                    if (latch) {
                        val dur = prefs.telemetryHoldDurationMs
                        val until = now + dur
                        if (prefs.telemetryHoldUntilMs < until) prefs.telemetryHoldUntilMs = until
                    }
                    clearSim()
                } else {
                    clearSim()
                }
            } else {
                // Stamp 63: NEVER null on cache gap — but dead lastGood must not stick AUTO.
                val fallback = listOfNotNull(tracked, lastGoodSnapshot, sharedLastGood)
                    .firstOrNull { it.isActiveWatch(now) || it.secondsToNet(now) > 0 }
                rememberTracked(fallback)
                if (tracked == null) {
                    rememberTracked(
                        provider.getNextSpaceX(now)
                            ?: provider.demoCatalog.firstOrNull()
                    )
                }
                if (tracked == null && !provider.isFetching) {
                    provider.refreshIfNeeded(force = true) { resolveTracked() }
                }
            }
            } // end CURRENT+AUTO live browse
        } else {
            // MANUAL (stamp 57): keep snapshot on miss + forceRefresh; never empty if catalog exists.
            pinnedSnapshot = null
            val id = prefs.telemetryLaunchId
            if (id.isNotBlank()) {
                val found = provider.findById(id)
                when {
                    found != null -> {
                        holdSnapshot = found
                        rememberTracked(found)
                    }
                    tracked?.id == id -> { /* keep snapshot */ }
                    holdSnapshot?.id == id -> tracked = holdSnapshot
                    else -> {
                        tracked = tracked ?: holdSnapshot
                        if (!provider.isFetching) {
                            provider.refreshIfNeeded(force = true) {
                                val again = provider.findById(id)
                                if (again != null) {
                                    holdSnapshot = again
                                    rememberTracked(again)
                                } else if (tracked == null) {
                                    val now2 = System.currentTimeMillis()
                                    rememberTracked(
                                        provider.getNextAny(now2)
                                            ?: provider.getNextSpaceX(now2)
                                            ?: provider.getCached()?.launches
                                                ?.filter { it.isUpcoming(now2) }
                                                ?.minByOrNull { it.netMs }
                                            ?: provider.demoCatalog.firstOrNull()
                                    )
                                }
                                resolveTracked()
                            }
                        }
                    }
                }
                if (tracked == null) {
                    rememberTracked(
                        provider.getNextAny(now)
                            ?: provider.getNextSpaceX(now)
                            ?: provider.getCached()?.launches
                                ?.filter { it.isUpcoming(now) }
                                ?.minByOrNull { it.netMs }
                            ?: provider.demoCatalog.firstOrNull()
                    )
                }
            } else {
                rememberTracked(
                    provider.getNextAny(now)
                        ?: provider.getNextSpaceX(now)
                        ?: provider.getCached()?.launches
                            ?.filter { it.isUpcoming(now) }
                            ?.minByOrNull { it.netMs }
                        ?: provider.demoCatalog.firstOrNull()
                )
            }
        }
        // Stamp 57: once when still null or tracked id changes.
        run {
            val key = tracked?.id ?: "NULL"
            if (key != lrt57LogKey) {
                lrt57LogKey = key
                Log.i(
                    "LRT57",
                    "livePool=${provider.livePool().size} getNextAny=${provider.getNextAny(now)?.id} " +
                        "launchId=${prefs.telemetryLaunchId} hold=${isHolding(now)} " +
                        "listMode=${prefs.telemetryListMode} tracked=${tracked?.id}"
                )
            }
        }
        // Stamp 57/58/63: never blank AUTO/pin/hold while we still have a last-good snapshot.
        keepTrackedOrLastGood(now)
        maybeAdvanceHistoricRoll(now)
        val t = tracked ?: return
        // Stamp 55: CURRENT live / AUTO — clearSim + wipe cursor so upcoming Starlink kills theater.
        // Never restore prefs.eventCursorSec onto a live wall-clock bird.
        if (autoMode || !t.isReplayable(now)) {
            if (prevId != null && prevId != t.id) {
                prefs.setEventCursorSec(prevId, null)
            }
            if (simSecondsFromNet != null || prefs.eventCursorSec(t.id) != null) {
                clearSim(t.id)
            } else {
                simSecondsFromNet = null
                forceStatus = null
                loopReplay = false
            }
        } else if (prevId != null && t.id != prevId) {
            simSecondsFromNet = prefs.eventCursorSec(t.id)
            if (simSecondsFromNet == null) {
                forceStatus = null
                loopReplay = false
            } else {
                loopReplay = true
            }
        } else {
            restoreEventCursor()
        }
        if (t.isReplayable(now)) {
            // Only jump theater when THIS module already had a different flight.
            // Fresh MCC / wallpaper process: prevId is null — keep wall-clock, do not T-30 a LIVE launch.
            // Stamp 57: never jumpTo while AUTO/FOLLOW — jumpTo clears tel_auto and blanks live.
            if (prevId != null && t.id != prevId && simSecondsFromNet == null
                && !prefs.telemetryPinned && !prefs.telemetryAuto) {
                jumpTo(-30f)
            }
        }
    }

    fun togglePin() {
        val t = tracked
        if (prefs.telemetryPinned) {
            prefs.telemetryPinned = false
            releaseHold()
            return
        }
        if (t == null) return
        prefs.telemetryLaunchId = t.id
        prefs.telemetryPinned = true
        // Keep prefs.telemetryAuto (FOLLOW intent). autoMode getter gates browse while LCK on.
        pinnedSnapshot = t
        // Stamp 85: chip (1H|2H|48H) picks hold duration; live pin expires via NET+chip.
        holdFor(prefs.telemetryHoldDurationMs)
    }

    fun stepCatalog(dir: Int) {
        // Stamp 55: allow CMD step while pinned — selectLaunch moves the pin.
        val list = selectableLaunches()
        if (list.isEmpty()) return
        val cur = tracked?.id
        val i = list.indexOfFirst { it.id == cur }.let { if (it < 0) 0 else it }
        val n = list.size
        val next = list[((i + dir) % n + n) % n]
        selectLaunch(next.id)
    }

    /**
     * How often LL2 should be re-fetched while AUTO is on.
     * Closer to NET → more frequent so webcast URLs / NET slips stay fresh.
     */
    fun autoRefreshIntervalMs(now: Long = System.currentTimeMillis()): Long {
        val t = tracked ?: return 5 * 60 * 1000L
        if (t.id.startsWith("demo-")) return 15 * 60 * 1000L
        val secs = t.secondsToNet(now)
        return when {
            secs in -LaunchWindow.WATCH_AFTER_NET_SEC..2 * 3600L -> 60_000L // T-2h … T+6h
            secs in 2 * 3600L..12 * 3600L -> 3 * 60_000L // T-12h → every 3 min
            secs in 12 * 3600L..48 * 3600L -> 5 * 60_000L
            else -> 10 * 60_000L
        }
    }

    fun selectLaunch(id: String) {
        if (id.isBlank()) return
        val found = provider.findById(id)

        if (prefs.telemetryPinned) {
            // Stamp 55: MOVE pin to the newly selected launch (never keep stale LCK id).
            prefs.telemetryLaunchId = id
            releaseHold()
            if (found != null) {
                // Stamp 71: rememberTracked bumps generation so agency chrome follows select.
                rememberTracked(found)
                pinnedSnapshot = found
            } else {
                pinnedSnapshot = pinnedSnapshot?.takeIf { it.id == id }
                provider.refreshIfNeeded(force = true) {
                    val again = provider.findById(id)
                    if (again != null) {
                        rememberTracked(again)
                        pinnedSnapshot = again
                    }
                    prefs.telemetryLaunchId = id
                    resolveTracked()
                }
            }
            // Keep prefs.telemetryAuto (FOLLOW intent). Pin gates browse via autoMode.
            val t = found ?: tracked?.takeIf { it.id == id }
            if (t != null && (t.id.startsWith("demo-") || t.isReplayable())) {
            // Stamp 80: theater without auto-LCK so unlocked HISTORICAL can cycle.
            jumpTo(-30f)
        } else {
            clearSim()
        }
            return
        }

        // Stamp 57: MANUAL pick clearSim + stick THIS bird — never flash prior live then blank.
        clearSim(id)
        // Unpinned manual pick: AUTO off + wall-clock HOLD on this id.
        prefs.telemetryAuto = false
        prefs.telemetryLaunchId = id
        if (found != null) {
            // Stamp 60: remember after MANUAL so wallpaper sharedLastGood matches Activity.
            rememberTracked(found)
        } else {
            // Do not null tracked on a cache miss — keep last good snapshot until
            // cache has the id (force refresh, then resolve).
            provider.refreshIfNeeded(force = true) {
                val again = provider.findById(id)
                // Stamp 71: generation bump on late resolve after MANUAL select.
                if (again != null) rememberTracked(again)
                prefs.telemetryLaunchId = id
                resolveTracked()
            }
        }
        // Re-bind HOLD to the selected id on a wall-clock timer (not netMs+dur).
        val now = System.currentTimeMillis()
        val dur = prefs.telemetryHoldDurationMs
        val holdTarget = found ?: tracked?.takeIf { it.id == id }
        prefs.telemetryHoldUntilMs = now + dur
        prefs.telemetryLaunchId = id
        if (holdTarget != null) holdSnapshot = holdTarget
        val t = holdTarget
        if (t != null && (t.id.startsWith("demo-") || t.isReplayable())) {
            // Stamp 80: theater without auto-LCK so unlocked HISTORICAL can cycle.
            jumpTo(-30f)
        } else {
            clearSim()
        }
    }

    fun holdFor(durationMs: Long, now: Long = System.currentTimeMillis()) {
        val t = tracked
        if (t != null) {
            prefs.telemetryLaunchId = t.id
            holdSnapshot = t
        }
        prefs.telemetryHoldDurationMs = durationMs
        // Wall-clock only — netMs latch stuck day-out Go birds for NET+dur.
        prefs.telemetryHoldUntilMs = now + durationMs
        autoMode = false
    }

    fun releaseHold() {
        prefs.telemetryHoldUntilMs = 0L
    }

    private fun isHolding(now: Long = System.currentTimeMillis()): Boolean =
        prefs.telemetryHoldUntilMs > now && prefs.telemetryLaunchId.isNotBlank()



    private fun persistEventCursor(sec: Float?) {
        val id = tracked?.id ?: return
        prefs.setEventCursorSec(id, sec)
        lastCursorWriteMs = System.currentTimeMillis()
    }

    private fun restoreEventCursor() {
        if (simSecondsFromNet != null) return
        val id = tracked?.id ?: return
        val cur = prefs.eventCursorSec(id) ?: return
        simSecondsFromNet = cur
        loopReplay = true
    }

    fun selectableLaunches(): List<LaunchSnapshot> {
        val now = System.currentTimeMillis()
        val live = provider.pickerPool(now, prefs.telemetryHorizonDays)
        val keep = provider.findById(prefs.telemetryLaunchId) ?: tracked
        // Stamp 55: keep only HOLD / active-watch / upcoming — past Spectrum stays HISTORIC.
        if (keep == null) return live
        if (live.any { it.id == keep.id }) return live
        val tSec = keep.secondsToNet(now)
        if (keep.isActiveWatch(now) || keep.isHold() || tSec > 0) {
            return listOf(keep) + live
        }
        return live
    }

    fun forceRefresh(onDone: (() -> Unit)? = null) {
        provider.refreshIfNeeded(force = true) {
            resolveTracked()
            onDone?.invoke()
        }
    }

    fun ensureData() {
        provider.refreshIfNeeded(force = false) {
            resolveTracked()
        }
        if (tracked == null) resolveTracked()
    }

    /** True while LL2 is in-flight or cache has never landed (cold wallpaper). */
    fun isFetchingData(): Boolean =
        provider.isFetching || (tracked == null && provider.getCached() == null)

    /**
     * Blank-well copy. Stamp 57: AUTO + tracked==null never silent —
     * FETCHING while cold/mid-pull, else AWAITING until getNextAny lands.
     */
    fun noTrackLabel(idle: String = "NO LAUNCH TRACKED"): String {
        if (tracked != null) return idle
        if (autoMode || prefs.telemetryAuto) {
            return if (isFetchingData() || provider.getCached() == null) "FETCHING" else "AWAITING"
        }
        return if (isFetchingData()) "FETCHING" else idle
    }
}


