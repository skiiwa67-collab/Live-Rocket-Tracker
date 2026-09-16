package com.ccos.retro.data

import com.ccos.retro.model.AppPrefs
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Lightweight models for live launch telemetry module.
 * Sourced from The Space Devs Launch Library 2.
 */
data class WebcastRef(
    val url: String,
    val publisher: String? = null,
    val title: String? = null
)

data class WebcastPane(
    val title: String,
    val url: String,
    /** True only when [url] is a specific watch/embed video, not a search. */
    val isWatch: Boolean
)

data class WebcastPanes(
    val official: WebcastPane,
    val nsf: WebcastPane
)

data class LaunchSnapshot(
    val id: String,
    val name: String,
    val statusName: String,
    val statusAbbrev: String,
    val netMs: Long,                 // epoch ms of NET (T-0)
    val windowStartMs: Long,
    val windowEndMs: Long,
    val provider: String,            // SpaceX, NASA, CASC, etc.
    val rocketName: String,
    val missionName: String,
    val pad: String,
    val location: String,
    val padLat: Float? = null,
    val padLon: Float? = null,
    val imageUrl: String? = null,
    val webcastUrl: String? = null,
    val webcasts: List<WebcastRef> = emptyList(),
    val webcastLive: Boolean = false,
    val probability: Int? = null,    // 0-100 or null
    val holdReason: String? = null,
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    fun isSpaceX(): Boolean = provider.contains("SpaceX", ignoreCase = true)
    fun isNasa(): Boolean = provider.contains("NASA", ignoreCase = true) ||
            provider.contains("United Launch Alliance", ignoreCase = true)
    fun isChinese(): Boolean = provider.contains("CASC", ignoreCase = true) ||
            provider.contains("China", ignoreCase = true) ||
            provider.contains("LandSpace", ignoreCase = true) ||
            rocketName.contains("Long March", ignoreCase = true) ||
            rocketName.contains("Zhuque", ignoreCase = true)
    fun isRussian(): Boolean = provider.contains("Roscosmos", ignoreCase = true) ||
            provider.contains("Russia", ignoreCase = true) ||
            provider.contains("RKA", ignoreCase = true) ||
            rocketName.contains("Soyuz", ignoreCase = true) ||
            rocketName.contains("Angara", ignoreCase = true) ||
            rocketName.contains("Proton", ignoreCase = true)

    fun isBlueOrigin(): Boolean = provider.contains("Blue Origin", ignoreCase = true) ||
            rocketName.contains("New Glenn", ignoreCase = true) ||
            rocketName.contains("New Shepard", ignoreCase = true)

    fun isEsa(): Boolean = provider.contains("Arianespace", ignoreCase = true) ||
            provider.contains("ESA", ignoreCase = true) ||
            provider.contains("ArianeGroup", ignoreCase = true) ||
            provider.contains("CNES", ignoreCase = true) ||
            rocketName.contains("Ariane", ignoreCase = true) ||
            rocketName.contains("Vega", ignoreCase = true)

    fun isRocketLab(): Boolean = provider.contains("Rocket Lab", ignoreCase = true) ||
            rocketName.contains("Electron", ignoreCase = true) ||
            rocketName.contains("Neutron", ignoreCase = true)

    fun isIsro(): Boolean = provider.contains("ISRO", ignoreCase = true) ||
            provider.contains("Indian Space", ignoreCase = true) ||
            rocketName.contains("PSLV", ignoreCase = true) ||
            rocketName.contains("GSLV", ignoreCase = true) ||
            rocketName.contains("LVM3", ignoreCase = true)

    fun isJaxa(): Boolean = provider.contains("JAXA", ignoreCase = true) ||
            provider.contains("Mitsubishi Heavy", ignoreCase = true) ||
            rocketName.contains("H-II", ignoreCase = true) ||
            rocketName.contains("H3", ignoreCase = true) ||
            rocketName.contains("Epsilon", ignoreCase = true)

    fun isUla(): Boolean = provider.contains("United Launch Alliance", ignoreCase = true) ||
            provider.contains("ULA", ignoreCase = true) ||
            rocketName.contains("Atlas V", ignoreCase = true) ||
            rocketName.contains("Vulcan", ignoreCase = true) ||
            rocketName.contains("Delta IV", ignoreCase = true)

    fun isLandSpace(): Boolean = provider.contains("LandSpace", ignoreCase = true) ||
            rocketName.contains("Zhuque", ignoreCase = true)

    fun allWebcasts(): List<WebcastRef> {
        if (webcasts.isNotEmpty()) return webcasts
        val u = webcastUrl
        return if (!u.isNullOrBlank()) listOf(WebcastRef(u)) else emptyList()
    }

    /** Seconds until NET. Negative = past T-0. */
    fun secondsToNet(now: Long = System.currentTimeMillis()): Long =
        (netMs - now) / 1000L

    fun isHold(): Boolean {
        if (!holdReason.isNullOrBlank()) return true
        val a = statusAbbrev
        val n = statusName
        return a.equals("Hold", ignoreCase = true) ||
            a.equals("In Hold", ignoreCase = true) ||
            n.contains("Hold", ignoreCase = true)
    }

    fun isTerminal(): Boolean {
        val blob = "$statusAbbrev $statusName".lowercase()
        // Stamp 63: Success / Completed / Failure / Partial Failure eject AUTO immediately.
        return "success" in blob || "completed" in blob || "failure" in blob ||
            "partial" in blob || ("fail" in blob && "fairing" !in blob)
    }


    /** Stamp 103: our catalog tape has no upcoming marks left (independent of 60m WATCH_AFTER). */
    fun isEventTapeExhausted(now: Long = System.currentTimeMillis()): Boolean {
        if (secondsToNet(now) > 0) return false // pre-NET always has upcoming tape
        val tSec = -secondsToNet(now).toFloat()
        val tape = com.ccos.retro.event.FlightEventCatalog.timeline(this)
        if (tape.isEmpty()) return false // unknown empty tape — do not false-eject; keep 60m string
        return tape.none { it.tSec > tSec + 0.5f }
    }

    fun isGo(): Boolean {
        val a = statusAbbrev.trim()
        val n = statusName.trim()
        return a.equals("Go", ignoreCase = true) ||
            n.equals("Go", ignoreCase = true) ||
            n.equals("Go for Launch", ignoreCase = true)
    }

    fun isWebcastLive(): Boolean {
        // Trust LL2 webcast_live only. Most upcoming birds already have a YouTube
        // URL; treating URL+Go as live made every Go look LIVE and stuck AUTO.
        return webcastLive
    }

    /**
     * AUTO first bucket: HOLD / Go / in-flight / webcast-live / T+ watch window.
     * Stamp 63: terminal Success/Completed/Failure eject immediately; In Flight is time-gated.
     */
    fun isActiveWatch(now: Long = System.currentTimeMillis()): Boolean {
        if (isTerminal()) return false
        val t = secondsToNet(now)
        val nearNet = t <= LaunchWindow.GO_WATCH_BEFORE_NET_SEC && t > -LaunchWindow.WATCH_AFTER_NET_SEC
        val holdLive = isHold() && nearNet
        val flightLive = isInFlightStatus() && nearNet
        // Webcast only while near NET / early T+ — not forever after Success.
        val webcastLive = isWebcastLive() && nearNet
        if (holdLive || flightLive || webcastLive || isTPlusWatch(now)) return true
        if (!isGo()) return false
        return nearNet
    }

    fun isTPlusWatch(now: Long = System.currentTimeMillis()): Boolean {
        val t = secondsToNet(now)
        return t <= 0 && t > -LaunchWindow.WATCH_AFTER_NET_SEC
    }

    /** Bare LL2 In Flight / In-Flight status (no time gate). */
    fun isInFlightStatus(): Boolean {
        val a = statusAbbrev
        val n = statusName
        return a.equals("In Flight", ignoreCase = true) ||
            n.contains("In Flight", ignoreCase = true)
    }

    /**
     * Stamp 63: In Flight is time-gated like Hold (near-NET / WATCH_AFTER window).
     * Bare statusAbbrev must NOT keep AUTO forever.
     */
    fun isInFlight(now: Long = System.currentTimeMillis()): Boolean {
        if (isInFlightStatus()) {
            val t = secondsToNet(now)
            return t <= LaunchWindow.GO_WATCH_BEFORE_NET_SEC && t > -LaunchWindow.WATCH_AFTER_NET_SEC
        }
        return isTPlusWatch(now)
    }

    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean {
        if (isActiveWatch(now)) return true
        return secondsToNet(now) > 0
    }

    /** CMD picker: upcoming + last 48h + anything AUTO is still watching. */
    fun inPickerWindow(now: Long = System.currentTimeMillis(), upcomingHorizonSec: Long = LaunchWindow.UPCOMING_MIN_SEC): Boolean {
        if (isActiveWatch(now)) return true
        val t = secondsToNet(now)
        if (t > 0) return t <= upcomingHorizonSec
        return t > -LaunchWindow.PICKER_LOOKBACK_SEC
    }

    /** Past flights and demos: CDT jump chips drive a replay clock. Live stays wall-clock. */
    fun isReplayable(now: Long = System.currentTimeMillis()): Boolean {
        if (id.startsWith("demo-")) return true
        if (isActiveWatch(now)) return false
        return secondsToNet(now) <= -LaunchWindow.WATCH_AFTER_NET_SEC
    }

    /**
     * Stamp 112: HISTORIC list/search only — never upcoming / Go / Hold preflight / live watch.
     * Flight 14 and any future-NET bird stay CURRENT-only.
     */
    fun isHistoricPastEntry(now: Long = System.currentTimeMillis()): Boolean {
        if (id.startsWith("demo-")) return true
        if (secondsToNet(now) > 0) return false
        if (isActiveWatch(now)) return false
        // Past NET or terminal Success/Failure/Partial.
        return isTerminal() || isReplayable(now) || secondsToNet(now) <= 0
    }
}

/**
 * Shared AUTO / CMD window constants. LL2 is truth — no mission-name special cases.
 */
object LaunchWindow {
    /** Go counts as active watch only inside this pre-NET window. */
    const val GO_WATCH_BEFORE_NET_SEC = 2L * 3600L
    /** T+ watch after NET so AUTO does not jump to the next bird mid-flight. */
    /** Stamp 63: AUTO post-NET active-watch = 60 minutes. */
    const val WATCH_AFTER_NET_SEC = 60L * 60L
    // Stamp 85: no fixed pin ceiling — live LCK lifetime is NET + telemetryHoldDurationMs (1H|2H|48H).
    /** Recent previous that must stay pickable after AUTO leaves. */
    const val PICKER_LOOKBACK_SEC = 48L * 3600L
    /** Fine-tooth upcoming compare: next ~14 days of LL2. */
    const val UPCOMING_MIN_SEC = 14L * 24L * 3600L

    /** Stamp 63: human dwell remain (matches WATCH_AFTER_NET_SEC). */
    fun formatDwellRemain(sec: Long): String {
        val s = sec.coerceAtLeast(0L)
        val h = s / 3600L
        val m = (s % 3600L) / 60L
        return when {
            h >= 1L -> "${h}h ${m}m"
            m >= 1L -> "${m}m"
            else -> "${s}s"
        }
    }
}

/**
 * Stamp 85 dwell UX — AUTO (T+60m) or LCK (NET + selected chip 1H|2H|48H).
 */
fun LaunchSnapshot.autoDwellHint(
    now: Long = System.currentTimeMillis(),
    pinned: Boolean = false,
    holdDurationMs: Long = 0L,
): String {
    // One source of truth: selected hold duration (normalized to 1H|2H|48H).
    if (pinned && holdDurationMs > 0L) {
        val dur = AppPrefs.normalizeHoldDurationMs(holdDurationMs)
        val chip = when (dur) {
            AppPrefs.HOLD_DUR_1H_MS -> "1H"
            AppPrefs.HOLD_DUR_48H_MS -> "48H"
            else -> "2H"
        }
        val remainSec = ((netMs + dur) - now) / 1000L
        return if (remainSec > 0L) {
            "LCK $chip | REMAIN ${LaunchWindow.formatDwellRemain(remainSec)}"
        } else {
            "LCK $chip | ENDING"
        }
    }
    if (!isActiveWatch(now)) return "AUTO | NEXT UPCOMING"
    val t = secondsToNet(now)
    if (t > 0L) return "AUTO | HOLD TO T+60m"
    val remain = LaunchWindow.WATCH_AFTER_NET_SEC + t
    return if (remain > 0L) {
        "AUTO | NEXT IN ${LaunchWindow.formatDwellRemain(remain)}"
    } else {
        "AUTO | LIVE HOLD"
    }
}

/**
 * Resolves MCC video panes for a launch.
 *
 * Never invents a YouTube video id. A channel home, /videos, or /live tab is not
 * "this launch". Those fall back to a labeled SEARCH URL for the mission name.
 */
object WebcastResolver {

    fun panes(launch: LaunchSnapshot?): WebcastPanes {
        if (launch == null) {
            val q = "rocket launch"
            return WebcastPanes(
                official = WebcastPane("UNKNOWN · SEARCH", resultsSearch(q), false),
                nsf = nsfSearch(q)
            )
        }
        val query = missionQuery(launch)
        val watch = launch.allWebcasts().filter { youtubeVideoId(it.url) != null }
        val officialWatch = watch.firstOrNull { hint(it).let { h -> h != null && h != HINT_NSF } }
            ?: watch.firstOrNull { hint(it) == null }
        val nsfWatch = watch.firstOrNull { hint(it) == HINT_NSF }

        val official = if (officialWatch != null) {
            val id = youtubeVideoId(officialWatch.url)!!
            val title = hint(officialWatch) ?: officialTitle(launch)
            WebcastPane(title, watchUrl(id), true)
        } else {
            officialSearch(launch, query)
        }
        val nsf = if (nsfWatch != null) {
            val id = youtubeVideoId(nsfWatch.url)!!
            WebcastPane("NASASPACEFLIGHT", watchUrl(id), true)
        } else {
            nsfSearch(query)
        }
        return WebcastPanes(official, nsf)
    }

    fun missionQuery(launch: LaunchSnapshot): String {
        val rocket = scrub(launch.rocketName)
        val mission = scrub(launch.missionName)
        val name = scrub(launch.name)
        val raw = when {
            rocket.isNotEmpty() && mission.isNotEmpty() &&
                !mission.contains(rocket, ignoreCase = true) -> "$rocket $mission"
            mission.isNotEmpty() -> mission
            rocket.isNotEmpty() -> rocket
            else -> name
        }
        return scrub(raw).ifBlank { name.ifBlank { "rocket launch" } }
    }

    fun youtubeVideoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val markers = listOf("v=", "youtu.be/", "/live/", "/embed/", "/shorts/")
        for (m in markers) {
            val i = url.indexOf(m, ignoreCase = true)
            if (i < 0) continue
            val rest = url.substring(i + m.length)
            val id = rest.takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
            if (id.length == 11) return id
        }
        return null
    }

    internal fun officialTitle(launch: LaunchSnapshot): String = when {
        launch.isSpaceX() -> "SPACEX"
        launch.isUla() -> "ULA"
        launch.isLandSpace() -> "LANDSPACE"
        launch.isRocketLab() -> "ROCKET LAB"
        launch.isBlueOrigin() -> "BLUE ORIGIN"
        launch.isEsa() && (launch.provider.contains("Ariane", ignoreCase = true) ||
            launch.rocketName.contains("Ariane", ignoreCase = true)) -> "ARIANESPACE"
        launch.isEsa() -> "ESA"
        launch.isIsro() -> "ISRO"
        launch.isJaxa() -> "JAXA"
        launch.isRussian() -> "ROSCOSMOS"
        launch.isChinese() -> "CNSA"
        launch.provider.contains("Firefly", ignoreCase = true) -> "FIREFLY"
        launch.provider.contains("Relativity", ignoreCase = true) -> "RELATIVITY"
        launch.isNasa() -> "NASA"
        else -> "UNKNOWN"
    }

    /** Known official YouTube handles only. Null → generic results search. */
    internal fun officialHandle(launch: LaunchSnapshot): Pair<String, String>? = when {
        launch.isSpaceX() -> "@SpaceX" to "SPACEX"
        launch.isUla() -> "@ulalaunch" to "ULA"
        launch.isRocketLab() -> "@RocketLab" to "ROCKET LAB"
        launch.isBlueOrigin() -> "@blueorigin" to "BLUE ORIGIN"
        launch.isEsa() && (launch.provider.contains("Ariane", ignoreCase = true) ||
            launch.rocketName.contains("Ariane", ignoreCase = true)) -> "@Arianespace" to "ARIANESPACE"
        launch.isEsa() -> "@ESA" to "ESA"
        launch.provider.contains("Firefly", ignoreCase = true) -> "@FireflyAerospace" to "FIREFLY"
        launch.isNasa() && !launch.isUla() -> "@NASA" to "NASA"
        else -> null
    }

    private const val HINT_NSF = "NASASPACEFLIGHT"

    private fun hint(ref: WebcastRef): String? {
        val hay = listOfNotNull(ref.url, ref.publisher, ref.title).joinToString(" ").lowercase()
        return when {
            "nasaspaceflight" in hay || "nasa spaceflight" in hay -> HINT_NSF
            "spacex" in hay -> "SPACEX"
            "ulalaunch" in hay || "united launch" in hay -> "ULA"
            "rocketlab" in hay || "rocket-lab" in hay || "rocket lab" in hay -> "ROCKET LAB"
            "blueorigin" in hay || "blue origin" in hay -> "BLUE ORIGIN"
            "landspace" in hay || "zhuque" in hay -> "LANDSPACE"
            "arianespace" in hay -> "ARIANESPACE"
            "firefly" in hay -> "FIREFLY"
            "relativity" in hay -> "RELATIVITY"
            hay.contains("@esa") || hay.contains("esa.org") ||
                hay.contains("youtube.com/esa") -> "ESA"
            "isro" in hay -> "ISRO"
            "jaxa" in hay -> "JAXA"
            "roscosmos" in hay -> "ROSCOSMOS"
            "nasa" in hay -> "NASA"
            else -> null
        }
    }

    private fun officialSearch(launch: LaunchSnapshot, query: String): WebcastPane {
        val handle = officialHandle(launch)
        return if (handle != null) {
            WebcastPane("${handle.second} · SEARCH", channelSearch(handle.first, query), false)
        } else {
            val title = officialTitle(launch)
            val q = if (title != "UNKNOWN") "$title $query" else query
            val label = if (title == "UNKNOWN") "UNKNOWN · SEARCH" else "$title · SEARCH"
            WebcastPane(label, resultsSearch(q), false)
        }
    }

    private fun nsfSearch(query: String): WebcastPane =
        WebcastPane("NASASPACEFLIGHT · SEARCH", channelSearch("@NASASpaceflight", query), false)

    private fun watchUrl(id: String): String = "https://www.youtube.com/watch?v=$id"

    private fun channelSearch(handle: String, query: String): String {
        val q = encode(query)
        return "https://www.youtube.com/$handle/search?query=$q"
    }

    private fun resultsSearch(query: String): String {
        val q = encode(query)
        return "https://www.youtube.com/results?search_query=$q"
    }

    private fun encode(query: String): String =
        URLEncoder.encode(query, StandardCharsets.UTF_8.name())

    private fun scrub(s: String): String =
        s.replace('|', ' ')
            .replace('/', ' ')
            .replace(Regex("[\\[\\](){}]"), " ")
            .replace(Regex("\\bTBD\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\bTBC\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

data class LaunchListResult(
    val launches: List<LaunchSnapshot>,
    val fetchedAtMs: Long,
    val source: String,
    /** Stamp 106: LL2 pagination next URL when present. */
    val nextUrl: String? = null
)
