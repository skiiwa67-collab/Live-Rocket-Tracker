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

    fun isWebcastLive(): Boolean {
        if (webcastLive) return true
        val watch = allWebcasts().any { WebcastResolver.youtubeVideoId(it.url) != null }
        if (!watch || isTerminal()) return false
        return isInFlight() || isHold() || secondsToNet() > -6 * 3600L
    }

    /** HOLD / in-flight / live webcast stay selectable. AUTO must not drop them. */
    fun isActiveWatch(now: Long = System.currentTimeMillis()): Boolean =
        isInFlight(now) || isHold() || isWebcastLive()

    fun isInFlight(now: Long = System.currentTimeMillis()): Boolean {
        if (isTerminal()) return false
        val flying = statusAbbrev.equals("In Flight", ignoreCase = true) ||
            statusName.contains("In Flight", ignoreCase = true)
        if (flying) return true
        val t = secondsToNet(now)
        return t < 0 && t > -1800
    }

    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean {
        if (isInFlight(now) || isHold() || webcastLive) return true
        return secondsToNet(now) > -300
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
 * Published-only fill for Owl Around The World. Does not invent NET.
 * Vehicle / pad / orbit come from Rocket Lab + LL2 public sheets.
 */
object PublishedLaunchFacts {
    const val OWL_ID = "9f5a4cb6-63f9-47e1-9512-b468bae2a8e6"
    const val OWL_PAD_LAT = -39.26085f
    const val OWL_PAD_LON = 177.86586f
    const val OWL_PAD = "Launch Complex 1B"
    const val OWL_LOCATION = "Rocket Lab LC-1 / Mahia"

    fun isOwl(launch: LaunchSnapshot?): Boolean {
        if (launch == null) return false
        if (launch.id == OWL_ID) return true
        val blob = "${launch.name} ${launch.missionName} ${launch.rocketName}".lowercase()
        return "owl around" in blob || ("strix" in blob && launch.isRocketLab())
    }

    fun apply(launch: LaunchSnapshot): LaunchSnapshot {
        if (!isOwl(launch)) return launch
        val pad = when {
            launch.pad.contains("1B", ignoreCase = true) -> launch.pad
            launch.pad.contains("Launch Complex 1", ignoreCase = true) -> "Launch Complex 1B"
            launch.pad.isBlank() -> OWL_PAD
            else -> launch.pad
        }
        val loc = when {
            launch.location.contains("Mahia", ignoreCase = true) ||
                launch.location.contains("Māhia", ignoreCase = true) -> OWL_LOCATION
            launch.location.isBlank() -> OWL_LOCATION
            else -> OWL_LOCATION
        }
        val mission = launch.missionName.ifBlank { "Owl Around The World" }.let { m ->
            if ("owl" in m.lowercase() || "strix" in m.lowercase()) m else "Owl Around The World"
        }
        return launch.copy(
            pad = pad,
            location = loc,
            padLat = OWL_PAD_LAT,
            padLon = OWL_PAD_LON,
            missionName = mission
        )
    }
}
