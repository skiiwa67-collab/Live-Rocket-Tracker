package com.ccos.retro.data

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
        return "success" in blob || "failure" in blob || "fail" in blob || "partial" in blob
    }

    fun isGo(): Boolean {
        val a = statusAbbrev.trim()
        val n = statusName.trim()
        return a.equals("Go", ignoreCase = true) ||
            n.equals("Go", ignoreCase = true) ||
            n.equals("Go for Launch", ignoreCase = true)
    }

    fun isWebcastLive(): Boolean {
        if (webcastLive) return true
        val watch = allWebcasts().any { WebcastResolver.youtubeVideoId(it.url) != null }
        if (!watch || isTerminal()) return false
        return isInFlight() || isHold() || isGo() || isTPlusWatch()
    }

    /**
     * AUTO first bucket: HOLD / Go / in-flight / webcast-live / T+ watch window.
     * Success does not eject a bird during [LaunchWindow.WATCH_AFTER_NET_SEC].
     */
    fun isActiveWatch(now: Long = System.currentTimeMillis()): Boolean {
        if (isHold() || isInFlight(now) || isWebcastLive() || isTPlusWatch(now)) return true
        // Go counts while still upcoming or inside the T+ window. A day-old
        // previous still marked Go must not steal AUTO from the next NET.
        return isGo() && secondsToNet(now) > -LaunchWindow.WATCH_AFTER_NET_SEC
    }

    fun isTPlusWatch(now: Long = System.currentTimeMillis()): Boolean {
        val t = secondsToNet(now)
        return t <= 0 && t > -LaunchWindow.WATCH_AFTER_NET_SEC
    }

    /**
     * In Flight status, or T+ inside the watch window.
     * Launch Successful is not a delete — Electron deploy is ~56 min; customers
     * in other timezones still need the bird. 30 minutes was the AUTO skip bug.
     */
    fun isInFlight(now: Long = System.currentTimeMillis()): Boolean {
        val flying = statusAbbrev.equals("In Flight", ignoreCase = true) ||
            statusName.contains("In Flight", ignoreCase = true)
        if (flying) return true
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
        return secondsToNet(now) <= -600
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

    /**
     * HUD overlay URL. Embed when we have a video id so YouTube home/search
     * chrome never lands on the wallpaper. Search URLs stay for MCC only.
     */
    fun overlayPlayUrl(url: String): String {
        val id = youtubeVideoId(url) ?: return url
        return "https://www.youtube.com/embed/$id?autoplay=1&playsinline=1&rel=0&modestbranding=1&fs=0"
    }

    /**
     * Channel /@handle/search 404s in mobile WebView (PrimeTestLab M-01, historic Electron).
     * Results search with the handle in the query actually loads. Never invent a video id.
     */
    private fun channelSearch(handle: String, query: String): String {
        val joined = listOf(handle, query).filter { it.isNotBlank() }.joinToString(" ")
        return resultsSearch(joined)
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
    val source: String
)

/**
 * Catalog is a live LL2 window. These constants are the AUTO / picker rules.
 * Never key AUTO off a mission nickname.
 */
object LaunchWindow {
    /** Electron deploy ~56 min; other timezones. 30 minutes was the skip bug. */
    const val WATCH_AFTER_NET_SEC = 6L * 3600L
    /** Recent previous that must stay pickable after AUTO leaves. */
    const val PICKER_LOOKBACK_SEC = 48L * 3600L
    /** Fine-tooth upcoming compare: next ~14 days of LL2. */
    const val UPCOMING_MIN_SEC = 14L * 24L * 3600L
}

/**
 * Published pad fill for Rocket Lab Electron / Mahia LC-1.
 * Does not invent NET. AUTO must never key off a mission name.
 */
object PublishedLaunchFacts {
    const val MAHIA_PAD_LAT = -39.26085f
    const val MAHIA_PAD_LON = 177.86586f
    const val MAHIA_PAD = "Launch Complex 1B"
    const val MAHIA_LOCATION = "Rocket Lab LC-1 / Mahia"

    fun apply(launch: LaunchSnapshot): LaunchSnapshot {
        if (!isMahiaElectron(launch)) return launch
        val pad = when {
            launch.pad.contains("1B", ignoreCase = true) -> launch.pad
            launch.pad.contains("1A", ignoreCase = true) -> launch.pad
            launch.pad.contains("Launch Complex 1", ignoreCase = true) -> launch.pad
            launch.pad.isBlank() -> MAHIA_PAD
            else -> launch.pad
        }
        val loc = when {
            launch.location.contains("Mahia", ignoreCase = true) ||
                launch.location.contains("Māhia", ignoreCase = true) ->
                if (launch.location.contains("LC-1", ignoreCase = true)) launch.location else MAHIA_LOCATION
            launch.location.isBlank() -> MAHIA_LOCATION
            else -> launch.location
        }
        return launch.copy(
            pad = pad,
            location = loc,
            padLat = launch.padLat ?: MAHIA_PAD_LAT,
            padLon = launch.padLon ?: MAHIA_PAD_LON
        )
    }

    private fun isMahiaElectron(launch: LaunchSnapshot): Boolean {
        if (!launch.isRocketLab()) return false
        val blob = "${launch.pad} ${launch.location}".lowercase()
        return "mahia" in blob || "māhia" in blob ||
            "launch complex 1" in blob || "lc-1" in blob || "lc 1" in blob
    }
}
