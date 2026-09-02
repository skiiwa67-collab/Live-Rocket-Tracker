package com.ccos.retro.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Data Provider for rocket launch schedule + status + historical demos.
 * Primary: The Space Devs Launch Library 2.
 * Also ships a curated offline catalog so agency skins and buttons can be
 * tested without network (SpaceX / NASA / CASC / generic).
 */
class LaunchDataProvider {

    companion object {
        private const val TAG = "CCOS.Launch"
        private const val DEV_UPCOMING = "https://lldev.thespacedevs.com/2.2.0/launch/upcoming/?limit=20&mode=detailed"
        private const val DEV_PREVIOUS = "https://lldev.thespacedevs.com/2.2.0/launch/previous/?limit=30&mode=detailed"
        private const val PROD_UPCOMING = "https://ll.thespacedevs.com/2.2.0/launch/upcoming/?limit=20&mode=detailed"
        private const val PROD_PREVIOUS = "https://ll.thespacedevs.com/2.2.0/launch/previous/?limit=30&mode=detailed"

        private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Shared by every LaunchDataProvider instance (Settings + Wallpaper)
        private val executor = Executors.newSingleThreadExecutor()
        private val cache = AtomicReference<LaunchListResult?>(null)
        private val pastCache = AtomicReference<LaunchListResult?>(null)
        @Volatile private var lastFetchMs = 0L
        @Volatile private var lastAttemptMs = 0L
        @Volatile private var backoffUntilMs = 0L
        @Volatile private var throttleHits = 0
        @Volatile private var allowLldev = false
        @Volatile private var lastWasThrottle = false
        private const val minIntervalMs = 5 * 60 * 1000L
        private const val backoffMs = 5 * 60 * 1000L
        private const val backoffMaxMs = 15 * 60 * 1000L
        @Volatile var sharedStatus: String = "Idle — not fetched yet"
        @Volatile var sharedFetching: Boolean = false
        @Volatile var sharedError: String? = null
        @Volatile var sharedSource: String = "—"
        @Volatile var sharedCount: Int = 0
    }

    // Instance mirrors of shared cache state (Activity + Wallpaper share one pool)
    val lastStatus: String get() = sharedStatus
    val isFetching: Boolean get() = sharedFetching
    val lastError: String? get() = sharedError
    val lastSource: String get() = sharedSource
    val lastCount: Int get() = sharedCount


    /** Always available offline demos for skin / button testing. */
    val demoCatalog: List<LaunchSnapshot> by lazy { buildDemoCatalog() }

    fun getCached(): LaunchListResult? = cache.get()
    fun catalogStale(now: Long = System.currentTimeMillis()): Boolean =
        lastFetchMs <= 0L || now - lastFetchMs > 15 * 60 * 1000L

    fun getPast(): LaunchListResult? = pastCache.get()

    fun allSelectable(): List<LaunchSnapshot> {
        val live = cache.get()?.launches.orEmpty()
        val past = pastCache.get()?.launches.orEmpty()
        val demoIds = demoCatalog.map { it.id }.toSet()
        val seen = demoIds.toMutableSet()
        val out = demoCatalog.toMutableList()
        for (l in live + past) {
            if (l.id in seen) continue
            seen += l.id
            out += PublishedLaunchFacts.apply(l)
        }
        return out
    }

    fun getNextSpaceX(now: Long = System.currentTimeMillis()): LaunchSnapshot? =
        cache.get()?.launches
            ?.filter { it.isSpaceX() && it.isUpcoming(now) }
            ?.minByOrNull { it.netMs }
            ?: demoCatalog.firstOrNull { it.isSpaceX() }

    fun livePool(): List<LaunchSnapshot> {
        val seen = linkedSetOf<String>()
        val out = mutableListOf<LaunchSnapshot>()
        for (l in cache.get()?.launches.orEmpty() + pastCache.get()?.launches.orEmpty()) {
            if (l.id.startsWith("demo-") || l.id in seen) continue
            seen += l.id
            out += PublishedLaunchFacts.apply(l)
        }
        return out
    }

    /**
     * Live CMD picker: LL2 upcoming + last 48h previous + active watch.
     * No demo / invented missions. Horizon never shrinks below 14 days.
     */
    fun pickerPool(now: Long = System.currentTimeMillis(), horizonDays: Int = 14): List<LaunchSnapshot> {
        val upcomingHorizonSec = maxOf(
            horizonDays.toLong() * 24L * 3600L,
            LaunchWindow.UPCOMING_MIN_SEC
        )
        return livePool()
            .filter { it.inPickerWindow(now, upcomingHorizonSec) }
            .sortedBy { it.netMs }
    }

    /**
     * AUTO: HOLD / Go / in-flight / webcast-live / T+ watch FIRST, closest to now.
     * Only if none of those exist, soonest future NET.
     */
    fun getNextAny(now: Long = System.currentTimeMillis()): LaunchSnapshot? {
        val live = livePool()
        val watch = live.filter { it.isActiveWatch(now) }
            .minByOrNull { kotlin.math.abs(it.secondsToNet(now)) }
        if (watch != null) return watch
        return live.filter { it.secondsToNet(now) > 0 }.minByOrNull { it.netMs }
    }

    fun findById(id: String): LaunchSnapshot? {
        val raw = cache.get()?.launches?.firstOrNull { it.id == id }
            ?: pastCache.get()?.launches?.firstOrNull { it.id == id }
            ?: demoCatalog.firstOrNull { it.id == id }
        return raw?.let { PublishedLaunchFacts.apply(it) }
    }

    private fun enrich(result: LaunchListResult): LaunchListResult =
        result.copy(launches = result.launches.map { PublishedLaunchFacts.apply(it) })

    /** Previous 48h + active watch stay in the live cache even if LL2 moved them to previous. */
    private fun mergeWatch(upcoming: LaunchListResult, previous: LaunchListResult?): LaunchListResult {
        val now = System.currentTimeMillis()
        val seen = upcoming.launches.associateBy { it.id }.toMutableMap()
        for (p in previous?.launches.orEmpty()) {
            if (p.id in seen) continue
            if (p.isActiveWatch(now) || p.inPickerWindow(now)) {
                seen[p.id] = p
            }
        }
        val merged = seen.values.sortedBy { it.netMs }
        return upcoming.copy(launches = merged)
    }

    fun refreshIfNeeded(force: Boolean = false, onDone: ((LaunchListResult?) -> Unit)? = null) {
        val now = System.currentTimeMillis()
        val cached = cache.get()
        if (isFetching) {
            onDone?.invoke(cached)
            return
        }
        if (now < backoffUntilMs) {
            sharedStatus = throttleStatus(cached)
            onDone?.invoke(cached)
            return
        }
        if (!force && cached != null && lastFetchMs > 0L && (now - lastFetchMs) < minIntervalMs) {
            sharedStatus = "Cached · $lastCount launches · $lastSource"
            onDone?.invoke(cached)
            return
        }
        if (!force && lastAttemptMs > 0L && (now - lastAttemptMs) < minIntervalMs) {
            sharedStatus = if (lastWasThrottle) throttleStatus(cached)
            else "Cached · $lastCount launches · $lastSource"
            onDone?.invoke(cached)
            return
        }
        sharedFetching = true
        lastAttemptMs = now
        sharedStatus = if (cached != null) "Refreshing… · cache $lastCount" else "Fetching Launch Library 2…"
        executor.execute {
            try {
                pullCatalog(cached, onDone)
            } finally {
                sharedFetching = false
            }
        }
    }

    private fun pullCatalog(cached: LaunchListResult?, onDone: ((LaunchListResult?) -> Unit)?) {
        var source = "ll2"
        var upcoming = fetchList(PROD_UPCOMING, "ll2")
        var previous = fetchList(PROD_PREVIOUS, "ll2")
        val prodThrottled = lastWasThrottle && upcoming == null

        if (upcoming == null && allowLldev) {
            val devUp = fetchList(DEV_UPCOMING, "lldev")
            if (devUp != null) {
                upcoming = devUp
                source = "lldev"
            }
            if (previous == null) previous = fetchList(DEV_PREVIOUS, "lldev")
            allowLldev = false
        } else if (upcoming == null) {
            allowLldev = true
            keepCache(cached, previous, throttled = true, onDone)
            return
        }

        previous?.let { pastCache.set(enrich(it)) }
        if (upcoming != null) {
            val merged = mergeWatch(enrich(upcoming), pastCache.get())
            cache.set(merged)
            lastFetchMs = System.currentTimeMillis()
            throttleHits = 0
            backoffUntilMs = 0L
            allowLldev = false
            lastWasThrottle = false
            sharedCount = merged.launches.size
            sharedSource = source
            sharedError = null
            val pastN = pastCache.get()?.launches?.size ?: 0
            sharedStatus = "OK · ${upcoming.launches.size} upcoming · $pastN past · $source"
            Log.i(TAG, lastStatus)
            onDone?.invoke(merged)
            return
        }
        keepCache(cached, previous, prodThrottled, onDone)
    }

    private fun keepCache(
        cached: LaunchListResult?,
        previous: LaunchListResult?,
        throttled: Boolean,
        onDone: ((LaunchListResult?) -> Unit)?
    ) {
        previous?.let { pastCache.set(enrich(it)) }
        val keep = cached ?: cache.get()
        if (throttled) startBackoff()
        if (keep != null) {
            sharedCount = keep.launches.size
            sharedStatus = if (throttled || lastWasThrottle) throttleStatus(keep)
            else "Cached · ${keep.launches.size} launches · $lastSource"
            Log.w(TAG, lastStatus)
            onDone?.invoke(keep)
        } else {
            sharedError = sharedError ?: "No launches from LL2"
            sharedStatus = if (throttled || lastWasThrottle) throttleStatus(null)
            else "NO CATALOG · $sharedError"
            Log.w(TAG, lastStatus)
            onDone?.invoke(null)
        }
    }

    private fun startBackoff() {
        throttleHits += 1
        val wait = (backoffMs * throttleHits).coerceAtMost(backoffMaxMs)
        backoffUntilMs = System.currentTimeMillis() + wait
    }

    private fun throttleStatus(cached: LaunchListResult?): String {
        val waitMs = (backoffUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val waitMin = ((waitMs + 59_999L) / 60_000L).coerceAtLeast(1L)
        return if (cached != null || cache.get() != null) {
            "THROTTLED · using cache (${lastCount}) · retry in ${waitMin}m"
        } else {
            "THROTTLED · retry in ${waitMin}m"
        }
    }

    private fun fetchList(urlStr: String, sourceTag: String): LaunchListResult? {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LiveRocketTracker/1.0.18 (Android; upcoming+previous)")
            }
            val code = conn.responseCode
            if (code != 200) {
                val errBody = try { conn.errorStream?.bufferedReader()?.use { it.readText() } } catch (_: Exception) { null }
                lastWasThrottle = code == 429 ||
                    errBody?.contains("throttl", ignoreCase = true) == true
                sharedError = "HTTP $code${errBody?.let { " · ${it.take(80)}" } ?: ""}"
                Log.w(TAG, "HTTP $code from $urlStr · $errBody")
                return null
            }
            lastWasThrottle = false
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseList(body, sourceTag)
        } catch (e: Exception) {
            lastWasThrottle = false
            sharedError = e.message ?: "network error"
            Log.e(TAG, "Fetch failed: ${e.message}")
            null
        }
    }


    private fun parseList(json: String, source: String): LaunchListResult {
        val root = JSONObject(json)
        val results = root.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<LaunchSnapshot>()
        for (i in 0 until results.length()) {
            val o = results.getJSONObject(i)
            val status = o.optJSONObject("status")
            val netStr = strOrNull(o, "net") ?: continue
            val netMs = parseIso(netStr) ?: continue
            val windowStart = parseIso(strOrNull(o, "window_start")) ?: netMs
            val windowEnd = parseIso(strOrNull(o, "window_end")) ?: netMs
            val provider = strOrNull(o, "lsp_name")
                ?: o.optJSONObject("launch_service_provider")?.let { strOrNull(it, "name") }
                ?: "Unknown"
            val rocket = strOrNull(o, "launcher")
                ?: o.optJSONObject("rocket")?.optJSONObject("configuration")?.let {
                    strOrNull(it, "full_name") ?: strOrNull(it, "name")
                }
                ?: "Rocket"
            val mission = strOrNull(o, "mission")
                ?: o.optJSONObject("mission")?.let { strOrNull(it, "name") }
                ?: ""
            val padObj = o.optJSONObject("pad")
            // pad is an object in LL2. optString(object) dumps JSON. Never show that.
            val pad = humanName(padObj?.let { strOrNull(it, "name") })
                ?: humanName(strOrNull(o, "pad"))
                ?: ""
            val locObj = padObj?.optJSONObject("location") ?: o.optJSONObject("location")
            val location = humanName(locObj?.let { strOrNull(it, "name") })
                ?: humanName(strOrNull(o, "location"))
                ?: ""
            val padLat = floatOrNull(padObj, "latitude")
            val padLon = floatOrNull(padObj, "longitude")
            val refs = extractWebcasts(o)
            list.add(
                LaunchSnapshot(
                    id = strOrNull(o, "id") ?: "unknown-$i",
                    name = strOrNull(o, "name") ?: "Unnamed",
                    statusName = status?.let { strOrNull(it, "name") } ?: "Unknown",
                    statusAbbrev = status?.let { strOrNull(it, "abbrev") }
                        ?: status?.let { strOrNull(it, "name") }?.take(3)
                        ?: "???",
                    netMs = netMs,
                    windowStartMs = windowStart,
                    windowEndMs = windowEnd,
                    provider = provider,
                    rocketName = rocket,
                    missionName = mission.ifBlank { strOrNull(o, "name") ?: "Mission" },
                    pad = pad,
                    location = location,
                    padLat = padLat,
                    padLon = padLon,
                    imageUrl = strOrNull(o, "image"),
                    webcastUrl = refs.firstOrNull()?.url,
                    webcasts = refs,
                    webcastLive = o.optBoolean("webcast_live", false),
                    probability = if (o.has("probability") && !o.isNull("probability"))
                        o.optInt("probability") else null,
                    holdReason = strOrNull(o, "holdreason")?.takeIf { it.isNotBlank() },
                    lastUpdatedMs = System.currentTimeMillis()
                )
            )
        }
        return LaunchListResult(list, System.currentTimeMillis(), source)
    }


    private fun extractWebcasts(o: JSONObject): List<WebcastRef> {
        // LL2 detailed: vidURLs / vid_urls. List mode usually omits them.
        val out = mutableListOf<WebcastRef>()
        val vids = o.optJSONArray("vidURLs") ?: o.optJSONArray("vid_urls")
        if (vids != null) {
            for (i in 0 until vids.length()) {
                val item = vids.opt(i)
                when (item) {
                    is String -> if (item.startsWith("http")) out += WebcastRef(item)
                    is JSONObject -> {
                        val u = strOrNull(item, "url") ?: strOrNull(item, "video_url")
                        if (!u.isNullOrBlank()) {
                            val pubObj = item.optJSONObject("publisher")
                            val pub = pubObj?.let { strOrNull(it, "name") }
                                ?: strOrNull(item, "publisher")
                                ?: strOrNull(item, "source")
                            out += WebcastRef(u, publisher = pub, title = strOrNull(item, "title"))
                        }
                    }
                }
            }
        }
        if (out.isEmpty()) {
            strOrNull(o, "webcast_url")?.let { out += WebcastRef(it) }
            strOrNull(o, "video_url")?.let { out += WebcastRef(it) }
        }
        return out
    }

    private fun parseIso(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return try {
            val clean = when {
                s.contains(".") -> s.substringBefore(".") + "Z"
                s.endsWith("Z") -> s
                else -> s + "Z"
            }
            isoFormat.parse(clean)?.time
                ?: java.time.Instant.parse(s.replace(" ", "T")).toEpochMilli()
        } catch (_: Exception) {
            try { java.time.Instant.parse(s).toEpochMilli() } catch (_: Exception) { null }
        }
    }


    private fun floatOrNull(o: JSONObject?, key: String): Float? {
        if (o == null || !o.has(key) || o.isNull(key)) return null
        val d = o.optDouble(key, Double.NaN)
        if (!d.isNaN()) return d.toFloat()
        val raw = o.optString(key, "")
        return raw.toFloatOrNull()
    }

    /** Safe JSON string extract — avoids Kotlin Nothing? vs String mismatch on optString(key, null). */
    private fun strOrNull(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        if (o.optJSONObject(key) != null || o.optJSONArray(key) != null) return null
        val v = o.optString(key, "")
        return v.takeIf { it.isNotBlank() }
    }

    private fun humanName(s: String?): String? {
        val v = s?.trim() ?: return null
        if (v.startsWith("{") || v.startsWith("[")) return null
        return v.takeIf { it.isNotBlank() }
    }


    /**
     * Curated offline launches for testing every agency skin + button set.
     * NET times are relative so countdown / in-flight states are useful.
     */
    private fun buildDemoCatalog(): List<LaunchSnapshot> {
        val now = System.currentTimeMillis()
        return listOf(
            // SpaceX — upcoming-style (T- ~1h for testing countdown)
            LaunchSnapshot(
                id = "demo-spacex-f9",
                name = "Falcon 9 Block 5 | Starlink Demo",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 55 * 60 * 1000L,
                windowStartMs = now + 55 * 60 * 1000L,
                windowEndMs = now + 70 * 60 * 1000L,
                provider = "SpaceX",
                rocketName = "Falcon 9 Block 5",
                missionName = "Starlink Demo",
                pad = "SLC-40",
                location = "Cape Canaveral SFS, FL, USA"
            ),
            // SpaceX — already in flight (for metrics / attitude)
            LaunchSnapshot(
                id = "demo-spacex-inflight",
                name = "Falcon 9 Block 5 | In-Flight Test",
                statusName = "In Flight",
                statusAbbrev = "In Flight",
                netMs = now - 95 * 1000L,   // T+95s → mid ascent
                windowStartMs = now - 95 * 1000L,
                windowEndMs = now + 10 * 60 * 1000L,
                provider = "SpaceX",
                rocketName = "Falcon 9 Block 5",
                missionName = "Ascent Profile Demo",
                pad = "LC-39A",
                location = "Kennedy Space Center, FL, USA"
            ),
            LaunchSnapshot(
                id = "demo-starship-f13",
                name = "Starship | Flight 13",
                statusName = "Launch Successful",
                statusAbbrev = "Success",
                netMs = now - 26 * 24 * 3600 * 1000L,
                windowStartMs = now - 26 * 24 * 3600 * 1000L,
                windowEndMs = now - 26 * 24 * 3600 * 1000L,
                provider = "SpaceX",
                rocketName = "Starship",
                missionName = "Flight 13",
                pad = "Orbital Launch Pad 2",
                location = "SpaceX Starbase, TX, USA"
            ),
            // SpaceX historical success
            LaunchSnapshot(
                id = "demo-spacex-crew",
                name = "Falcon 9 | Crew Demo (Historical)",
                statusName = "Launch Successful",
                statusAbbrev = "Success",
                netMs = now - 3 * 24 * 3600 * 1000L,
                windowStartMs = now - 3 * 24 * 3600 * 1000L,
                windowEndMs = now - 3 * 24 * 3600 * 1000L,
                provider = "SpaceX",
                rocketName = "Falcon 9 Block 5",
                missionName = "Crew Demo",
                pad = "LC-39A",
                location = "Kennedy Space Center, FL, USA"
            ),
            // NASA / ULA
            LaunchSnapshot(
                id = "demo-nasa-sls",
                name = "SLS Block 1 | Artemis Demo",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 2 * 3600 * 1000L,
                windowStartMs = now + 2 * 3600 * 1000L,
                windowEndMs = now + 4 * 3600 * 1000L,
                provider = "NASA",
                rocketName = "Space Launch System (SLS)",
                missionName = "Artemis Demo",
                pad = "LC-39B",
                location = "Kennedy Space Center, FL, USA"
            ),
            // CASC / Chinese
            LaunchSnapshot(
                id = "demo-casc-lm",
                name = "Long March 5 | Demo Mission",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 90 * 60 * 1000L,
                windowStartMs = now + 90 * 60 * 1000L,
                windowEndMs = now + 120 * 60 * 1000L,
                provider = "China Aerospace Science and Technology Corporation",
                rocketName = "Long March 5",
                missionName = "Demo Mission",
                pad = "LC-1",
                location = "Wenchang, China"
            ),
            // Roscosmos
            LaunchSnapshot(
                id = "demo-soyuz",
                name = "Soyuz-2.1a | ISS Crew Demo",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 3 * 3600 * 1000L,
                windowStartMs = now + 3 * 3600 * 1000L,
                windowEndMs = now + 3 * 3600 * 1000L + 60_000L,
                provider = "Roscosmos",
                rocketName = "Soyuz-2.1a",
                missionName = "ISS Crew Demo",
                pad = "Site 31/6",
                location = "Baikonur Cosmodrome, Kazakhstan"
            ),
            LaunchSnapshot(
                id = "demo-soyuz-inflight",
                name = "Soyuz-2.1a | Ascent Profile",
                statusName = "In Flight",
                statusAbbrev = "In Flight",
                netMs = now - 80 * 1000L,
                windowStartMs = now - 80 * 1000L,
                windowEndMs = now + 20 * 60 * 1000L,
                provider = "Roscosmos",
                rocketName = "Soyuz-2.1a",
                missionName = "Ascent Profile",
                pad = "Site 31/6",
                location = "Baikonur Cosmodrome, Kazakhstan"
            ),
            // Generic / other
            LaunchSnapshot(
                id = "demo-spacex-fail",
                name = "Falcon 9 | Ascent Anomaly Demo",
                statusName = "In Flight",
                statusAbbrev = "In Flight",
                netMs = now - 72 * 1000L,
                windowStartMs = now - 72 * 1000L,
                windowEndMs = now + 10 * 60 * 1000L,
                provider = "SpaceX",
                rocketName = "Falcon 9 Block 5",
                missionName = "Ascent Anomaly Demo",
                pad = "SLC-40",
                location = "Cape Canaveral SFS, FL, USA"
            ),
            LaunchSnapshot(
                id = "demo-electron-inflight",
                name = "Electron | Ascent Profile",
                statusName = "In Flight",
                statusAbbrev = "In Flight",
                netMs = now - 200 * 1000L,
                windowStartMs = now - 200 * 1000L,
                windowEndMs = now + 20 * 60 * 1000L,
                provider = "Rocket Lab",
                rocketName = "Electron",
                missionName = "Ascent Profile",
                pad = "LC-1",
                location = "Mahia Peninsula, New Zealand"
            ),
            LaunchSnapshot(
                id = "demo-generic",
                name = "Electron | Demo Flight",
                statusName = "To Be Confirmed",
                statusAbbrev = "TBC",
                netMs = now + 5 * 3600 * 1000L,
                windowStartMs = now + 5 * 3600 * 1000L,
                windowEndMs = now + 6 * 3600 * 1000L,
                provider = "Rocket Lab",
                rocketName = "Electron",
                missionName = "Demo Flight",
                pad = "LC-1",
                location = "Mahia Peninsula, New Zealand"
            ),
            LaunchSnapshot(
                id = "demo-blueorigin",
                name = "New Glenn | Demo Mission",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 4 * 3600 * 1000L,
                windowStartMs = now + 4 * 3600 * 1000L,
                windowEndMs = now + 5 * 3600 * 1000L,
                provider = "Blue Origin",
                rocketName = "New Glenn",
                missionName = "Demo Mission",
                pad = "LC-36",
                location = "Cape Canaveral SFS, FL, USA"
            ),
            LaunchSnapshot(
                id = "demo-esa-ariane",
                name = "Ariane 6 | Demo Flight",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 6 * 3600 * 1000L,
                windowStartMs = now + 6 * 3600 * 1000L,
                windowEndMs = now + 7 * 3600 * 1000L,
                provider = "Arianespace",
                rocketName = "Ariane 6",
                missionName = "Demo Flight",
                pad = "ELA-4",
                location = "Guiana Space Centre, Kourou"
            ),
            LaunchSnapshot(
                id = "demo-isro-lvm3",
                name = "LVM3 | Demo Mission",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 8 * 3600 * 1000L,
                windowStartMs = now + 8 * 3600 * 1000L,
                windowEndMs = now + 9 * 3600 * 1000L,
                provider = "ISRO",
                rocketName = "LVM3",
                missionName = "Demo Mission",
                pad = "SLP",
                location = "Satish Dhawan Space Centre, India"
            ),
            LaunchSnapshot(
                id = "demo-jaxa-h3",
                name = "H3 | Demo Flight",
                statusName = "Go for Launch",
                statusAbbrev = "Go",
                netMs = now + 10 * 3600 * 1000L,
                windowStartMs = now + 10 * 3600 * 1000L,
                windowEndMs = now + 11 * 3600 * 1000L,
                provider = "JAXA",
                rocketName = "H3",
                missionName = "Demo Flight",
                pad = "Yoshinobu",
                location = "Tanegashima Space Center, Japan"
            )
        )
    }
}

