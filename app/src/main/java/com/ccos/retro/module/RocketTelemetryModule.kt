package com.ccos.retro.module

import com.ccos.retro.data.LaunchDataProvider
import com.ccos.retro.data.LaunchSnapshot
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
 *  7 AUTO → toggle auto-next mode. Double-tap pins / unpins. Lock light when pinned.
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

    /** Last snapshot that matched the AUTO pin. Survives a catalog miss of findById. */
    private var pinnedSnapshot: LaunchSnapshot? = null

    /** Auto mode: always lock to the next upcoming launch. Pin (AUTO double-tap) forces this off. */
    var autoMode: Boolean
        get() = prefs.telemetryAuto && !prefs.telemetryPinned
        set(v) {
            if (v && prefs.telemetryPinned) {
                prefs.telemetryAuto = false
                return
            }
            prefs.telemetryAuto = v
        }

    override fun onModuleButton(index: Int): Boolean {
        if (index == 7) {
            if (prefs.telemetryPinned) {
                // Pin wins. AUTO is browse; do not steal the pin. Lock light stays on AUTO.
                prefs.telemetryAuto = false
                if (activePage == 7) activePage = 2
                return true
            }
            autoMode = !autoMode
            if (autoMode) {
                releaseHold()
                clearSim()
                resolveTracked()
            }
            // AUTO is a lamp, not a page — never open the old overlay screen.
            if (activePage == 7) activePage = 2
            return true
        }
        activePage = index.coerceIn(0, 6)
        val launch = tracked
        if (index == 1 && launch != null && !isLiveWallClock() && simSecondsFromNet == null) {
            jumpTo(-30f)
        }
        return true
    }

    /** Jump simulation to a key mission timestamp (seconds from NET). */
    fun jumpTo(secFromNet: Float) {
        simSecondsFromNet = secFromNet
        forceStatus = null
        loopReplay = true
        autoMode = false
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

    fun clearSim() {
        simSecondsFromNet = null
        forceStatus = null
        loopReplay = false
        persistEventCursor(null)
    }

    /**
     * Live current flights use Launch Library 2 NET only.
     * Theater (demo / historic scrub) may use the event cursor.
     * Never let a leftover T-30 cursor lie about a live Owl-class lift.
     */
    fun isLiveWallClock(now: Long = System.currentTimeMillis()): Boolean {
        val t = tracked ?: return true
        if (t.id.startsWith("demo-")) return false
        if (prefs.telemetryListMode == "historical") return false
        if (simSecondsFromNet != null) return false
        return true
    }

    /** Effective seconds from NET for display/metrics (sim or real). */
    fun effectiveSecondsFromNet(now: Long = System.currentTimeMillis()): Float {
        val launch = tracked ?: return 0f
        if (!isLiveWallClock(now)) {
            val sim = simSecondsFromNet ?: prefs.eventCursorSec(launch.id)
            if (sim != null) return sim
        }
        val t0 = if (launch.id.startsWith("demo-")) {
            prefs.pinnedNetMs(launch.id, launch.netMs)
        } else {
            launch.netMs
        }
        if (t0 <= 0L) return 0f
        return (now - t0) / 1000f
    }

    fun clockIsEst(): Boolean {
        val launch = tracked ?: return false
        if (isLiveWallClock() && launch.netMs > 0L) {
            return provider.catalogStale()
        }
        if (simSecondsFromNet != null) return true
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
        if (next > end) next = -30f
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
    fun resolveTracked(now: Long = System.currentTimeMillis()) {
        val prevId = tracked?.id
        if (prefs.telemetryPinned) {
            // AUTO double-tap pin is hard. Browse must not run, even if findById misses this tick.
            if (prefs.telemetryAuto) prefs.telemetryAuto = false
            val pinId = prefs.telemetryLaunchId.ifBlank { tracked?.id ?: pinnedSnapshot?.id ?: "" }
            if (pinId.isNotBlank() && prefs.telemetryLaunchId.isBlank()) {
                prefs.telemetryLaunchId = pinId
            }
            val found = if (pinId.isNotBlank()) provider.findById(pinId) else null
            if (found != null) {
                tracked = found
                pinnedSnapshot = found
            } else {
                // Catalog refresh / process start can miss findById. Keep the pinned flight.
                // Never fall through to getNextAny.
                val keep = when {
                    tracked?.id == pinId -> tracked
                    pinnedSnapshot?.id == pinId -> pinnedSnapshot
                    else -> tracked ?: pinnedSnapshot
                }
                if (keep != null) {
                    tracked = keep
                    pinnedSnapshot = keep
                    if (prefs.telemetryLaunchId.isBlank()) prefs.telemetryLaunchId = keep.id
                }
            }
        } else if (isHolding(now)) {
            // HOLD beats AUTO. Do not let the next NET on Earth steal an in-flight vehicle.
            pinnedSnapshot = null
            tracked = provider.findById(prefs.telemetryLaunchId) ?: tracked
        } else if (autoMode) {
            pinnedSnapshot = null
            val live = provider.getCached()?.launches.orEmpty()
                .filter { !it.id.startsWith("demo-") }
            val inFlight = live.filter { it.isInFlight(now) }
                .minByOrNull { kotlin.math.abs(it.secondsToNet(now)) }
            val next = inFlight ?: provider.getNextAny(now)
            tracked = next
            if (next != null && prefs.telemetryLaunchId != next.id) {
                prefs.telemetryLaunchId = next.id
            }
            if (inFlight != null) {
                val dur = prefs.telemetryHoldDurationMs
                val until = maxOf(now + dur, inFlight.netMs + dur)
                if (prefs.telemetryHoldUntilMs < until) prefs.telemetryHoldUntilMs = until
                clearSim()
            } else if (next != null) {
                clearSim()
            }
        } else {
            pinnedSnapshot = null
            val id = prefs.telemetryLaunchId
            tracked = when {
                id.isNotBlank() -> provider.findById(id)
                else -> provider.getNextSpaceX(now) ?: provider.demoCatalog.firstOrNull()
            }
        }
        val t = tracked ?: return
        if (t.id.startsWith("demo-") || prefs.telemetryListMode == "historical") {
            if (prevId != null && t.id != prevId) {
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
                if (prevId != null && t.id != prevId && simSecondsFromNet == null && !prefs.telemetryPinned) {
                    jumpTo(-30f)
                }
            } else if (prefs.eventCursorSec(t.id) == null) {
                simSecondsFromNet = null
                forceStatus = null
                loopReplay = false
            }
        } else if (simSecondsFromNet == null) {
            forceStatus = null
            loopReplay = false
        }
    }

    fun togglePin() {
        val t = tracked
        if (prefs.telemetryPinned) {
            prefs.telemetryPinned = false
            return
        }
        if (t == null) return
        prefs.telemetryLaunchId = t.id
        prefs.telemetryPinned = true
        prefs.telemetryAuto = false
        pinnedSnapshot = t
    }

    fun stepCatalog(dir: Int) {
        if (prefs.telemetryPinned) return
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
            secs in -300L..2 * 3600L -> 60_000L          // T-2h … T+5m → every 1 min
            secs in 2 * 3600L..12 * 3600L -> 3 * 60_000L // T-12h → every 3 min
            secs in 12 * 3600L..48 * 3600L -> 5 * 60_000L
            else -> 10 * 60_000L
        }
    }

    fun selectLaunch(id: String) {
        prefs.telemetryLaunchId = id
        autoMode = false
        tracked = provider.findById(id)
        holdFor(prefs.telemetryHoldDurationMs)
        val t = tracked
        if (t != null && (t.id.startsWith("demo-") || prefs.telemetryListMode == "historical")) {
            jumpTo(-30f)
        } else {
            clearSim()
        }
    }

    fun holdFor(durationMs: Long, now: Long = System.currentTimeMillis()) {
        val t = tracked
        if (t != null) prefs.telemetryLaunchId = t.id
        prefs.telemetryHoldDurationMs = durationMs
        val anchor = t?.netMs?.let { maxOf(now, it) } ?: now
        prefs.telemetryHoldUntilMs = anchor + durationMs
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

    fun selectableLaunches(): List<LaunchSnapshot> = provider.allSelectable()

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
}

