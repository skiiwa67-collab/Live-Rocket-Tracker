package com.ccos.retro.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
        private const val DEV_UPCOMING = "https://lldev.thespacedevs.com/2.2.0/launch/upcoming/?limit=50&mode=detailed"
        private const val DEV_PREVIOUS = "https://lldev.thespacedevs.com/2.2.0/launch/previous/?limit=25&mode=detailed"
        private const val PROD_UPCOMING = "https://ll.thespacedevs.com/2.2.0/launch/upcoming/?limit=50&mode=detailed"
        private const val PROD_PREVIOUS = "https://ll.thespacedevs.com/2.2.0/launch/previous/?limit=25&mode=detailed"

        private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Shared by every LaunchDataProvider instance (Settings + Wallpaper)
        private val executor = Executors.newSingleThreadExecutor()
        private val cache = AtomicReference<LaunchListResult?>(null)
        private val pastCache = AtomicReference<LaunchListResult?>(null)
        @Volatile private var lastFetchMs = 0L
        private const val minIntervalMs = 2 * 60 * 1000L
        @Volatile var sharedStatus: String = "Idle  -  not fetched yet"
        @Volatile var sharedFetching: Boolean = false
        /** tip138: do not drop refresh while a fetch is in flight (fast Engine recreate / adb). */
        @Volatile private var refreshQueued: Boolean = false
        @Volatile private var refreshQueuedForce: Boolean = false
        private val pendingRefreshOnDone = AtomicReference<((LaunchListResult?) -> Unit)?>(null)
        @Volatile var sharedError: String? = null
        @Volatile var sharedSource: String = " - "
        @Volatile var sharedCount: Int = 0

        /** Stamp 106: last historic name-search hits (1yr window). */
        private val historicSearchCache = AtomicReference<List<LaunchSnapshot>>(emptyList())
        @Volatile private var lastHistoricSearchQ: String = ""
        @Volatile private var lastHistoricSearchMs: Long = 0L
        /** Stamp 107: search in-flight — separate from catalog sharedFetching. */
        @Volatile private var historicSearching: Boolean = false
        /** Stamp 110: latest query while a search is in flight. */
        @Volatile private var queuedHistoricSearchQ: String? = null
        /** Stamp 111: UI/search interest for AUTO/LCK scope (any catalog string). */
        @Volatile private var historicSearchInterestQ: String = ""
        private const val historicSearchMinIntervalMs = 8_000L
        private const val HISTORIC_DEFAULT_N = 20
        private const val HISTORIC_SEARCH_YEAR_MS = 365L * 24L * 3600L * 1000L
        /** Tip 121: Starship Flight 14 LL2 id — always prod-NET authority. */
        const val F14_LL2_ID = "7d1afb26-6f9c-429b-9ccf-29012fd1e519"
    }

    /** Tip 121: bind durable prod-NET vault (call from Activity/Wallpaper onCreate). */
    fun bindAppContext(context: Context) {
        ProdNetVault.bind(context)
    }

    // Instance mirrors of shared cache state (Activity + Wallpaper share one pool)
    val lastStatus: String get() = sharedStatus
    val isFetching: Boolean get() = sharedFetching
    /** Stamp 110: LL2 historic name-search in flight (not catalog refresh). */
    val isHistoricSearching: Boolean get() = historicSearching
    /** Stamp 111: non-blank search interest (Starship / Falcon / Electron / ...). */
    val historicSearchInterest: String get() = historicSearchInterestQ
    fun hasActiveHistoricSearch(): Boolean = historicSearchInterestQ.isNotBlank()
    val lastError: String? get() = sharedError
    val lastSource: String get() = sharedSource
    val lastCount: Int get() = sharedCount


    /** Always available offline demos for skin / button testing. */
    val demoCatalog: List<LaunchSnapshot> by lazy { buildDemoCatalog() }

    fun getCached(): LaunchListResult? = cache.get()
    fun catalogStale(now: Long = System.currentTimeMillis()): Boolean =
        lastFetchMs <= 0L || now - lastFetchMs > 15 * 60 * 1000L

    fun getPast(): LaunchListResult? = pastCache.get()

    /** Full book (settings / find). Prefer historicPool / pickerPool for UI lists. */
    fun allSelectable(): List<LaunchSnapshot> {
        val live = cache.get()?.launches.orEmpty().filter { !it.id.startsWith("demo-") }
        val past = pastCache.get()?.launches.orEmpty().filter { !it.id.startsWith("demo-") }
        val seen = linkedSetOf<String>()
        val out = mutableListOf<LaunchSnapshot>()
        // Stamp 93: demos first for HISTORICAL tooling, then past, then live (callers filter).
        for (l in demoCatalog + past + live) {
            if (l.id in seen) continue
            seen += l.id
            out += l
        }
        return out
    }

    fun getNextSpaceX(now: Long = System.currentTimeMillis()): LaunchSnapshot? =
        cache.get()?.launches
            ?.filter { it.isSpaceX() && it.isUpcoming(now) && !it.id.startsWith("demo-") }
            ?.minByOrNull { it.netMs }

    /** Stamp 93: live upcoming cache ONLY — never pastCache, never demos. */
    fun livePool(): List<LaunchSnapshot> {
        val seen = linkedSetOf<String>()
        val out = mutableListOf<LaunchSnapshot>()
        for (l in cache.get()?.launches.orEmpty()) {
            if (l.id.startsWith("demo-") || l.id in seen) continue
            seen += l.id
            out += l
        }
        return out
    }

    /**
     * Stamp 106: DEFAULT = real past only, newest-first, ~20. Never prepend demos.
     * Demos KEPT in build (demoCatalog). Show demos when:
     *  (a) search contains "demo", or
     *  (b) pastCache empty / no real past (fresh install / offline fallback — never blank).
     * When real past exists, demos never block Gravity-1 / newest.
     */
    fun historicPool(now: Long = System.currentTimeMillis(), query: String = ""): List<LaunchSnapshot> {
        val q = query.trim()
        val qLower = q.lowercase()
        val wantDemo = "demo" in qLower
        val tokens = qLower.split(Regex("\\s+")).filter { it.isNotBlank() && it != "demo" }
        val seen = linkedSetOf<String>()
        val out = mutableListOf<LaunchSnapshot>()

        fun blob(l: LaunchSnapshot): String =
            "${l.name} ${l.rocketName} ${l.provider} ${l.pad} ${l.location} ${l.statusName} ${l.missionName} ${l.holdReason.orEmpty()}".lowercase()

        fun matchTokens(l: LaunchSnapshot): Boolean {
            if (tokens.isEmpty()) return true
            val b = blob(l)
            return tokens.all { it in b }
        }

        fun add(l: LaunchSnapshot) {
            if (l.id in seen) return
            seen += l.id
            out += l
        }

        val pastSorted = pastCache.get()?.launches.orEmpty()
            .filter { !it.id.startsWith("demo-") }
            .filter { it.secondsToNet(now) <= 0 || it.isReplayable(now) }
            .sortedByDescending { it.netMs }

        if (q.isBlank()) {
            pastSorted.take(HISTORIC_DEFAULT_N).forEach { add(it) }
            // Fallback only: no real past yet (fetch fail / fresh install).
            if (out.isEmpty()) {
                for (l in demoCatalog) add(l)
            }
            return out
        }

        // Real past / search hits first so demos never bury Gravity-1 when past exists.
        for (l in pastSorted) {
            if (matchTokens(l)) add(l)
        }
        for (l in historicSearchCache.get().sortedByDescending { it.netMs }) {
            if (matchTokens(l)) add(l)
        }
        if (wantDemo) {
            for (l in demoCatalog) {
                if (tokens.isEmpty() || matchTokens(l)) add(l)
            }
        }
        return out
    }

    fun historicSearchCacheSize(): Int = historicSearchCache.get().size

    /** Stamp 111: sync CMD search box interest (does not wipe cache). */
    fun setHistoricSearchInterest(query: String) {
        historicSearchInterestQ = query.trim()
    }

    /** Stamp 111: release search interest for AUTO leave-to-live. */
    fun clearHistoricSearchInterest() {
        historicSearchInterestQ = ""
        lastHistoricSearchQ = ""
        queuedHistoricSearchQ = null
        historicSearchCache.set(emptyList())
    }

    /**
     * Stamp 111: cycle pool for HISTORIC search + LCK + AUTO.
     * Uses interest query hits (cache + past token match) — any agency string.
     */
    fun searchScopedHistoricPool(now: Long = System.currentTimeMillis()): List<LaunchSnapshot> {
        val q = historicSearchInterestQ.trim()
        if (q.isBlank()) return emptyList()
        return historicPool(now, q)
            .filter { !it.id.startsWith("demo-") || "demo" in q.lowercase() }
            .filter { it.id.startsWith("demo-") || it.isReplayable(now) || it.secondsToNet(now) < -60 }
            .sortedBy { it.netMs }
    }

    /**
     * Stamp 106/107: LL2 previous/search lookback ~1 year. Throttled. No 2yr.
     * Stamp 107: uses historicSearching (NOT sharedFetching) so catalog refresh is untouched.
     * Exceptions → sharedStatus; never crash the process.
     */
    fun requestHistoricSearch(query: String, onDone: (() -> Unit)? = null) {
        val q = query.trim()
        val qLower = q.lowercase()
        if (q.isBlank() || ("demo" in qLower && qLower.replace("demo", "").trim().isEmpty())) {
            onDone?.invoke()
            return
        }
        val now = System.currentTimeMillis()
        if (q.equals(lastHistoricSearchQ, ignoreCase = true) &&
            now - lastHistoricSearchMs < historicSearchMinIntervalMs &&
            historicSearchCache.get().isNotEmpty()
        ) {
            onDone?.invoke()
            return
        }
        if (historicSearching) {
            // Stamp 110: queue latest query — do NOT onDone (avoids empty "No historic match" wipe).
            queuedHistoricSearchQ = q
            sharedStatus = "Historic search queued | $q"
            return
        }
        historicSearchInterestQ = q
        historicSearching = true
        sharedStatus = "Historic search | $q | 1yr..."
        executor.execute {
            try {
                try {
                    val gte = isoFormat.format(java.util.Date(now - HISTORIC_SEARCH_YEAR_MS))
                    val enc = URLEncoder.encode(q, "UTF-8")
                    val prod = "https://ll.thespacedevs.com/2.2.0/launch/previous/?limit=40&mode=detailed&search=$enc&net__gte=$gte"
                    val dev = "https://lldev.thespacedevs.com/2.2.0/launch/previous/?limit=40&mode=detailed&search=$enc&net__gte=$gte"
                    var hit = fetchList(prod, "ll2-search")
                    var src = "ll2-search"
                    if (hit == null) {
                        hit = fetchList(dev, "lldev-search")
                        if (hit != null) src = "lldev-search"
                    }
                    val next = hit?.nextUrl?.trim()?.takeIf { it.startsWith("http") }
                    if (hit != null && hit.launches.size < 12 && next != null) {
                        val more = fetchList(next, src)
                        if (more != null) {
                            val merged = (hit.launches + more.launches).distinctBy { it.id }
                            hit = hit.copy(launches = merged)
                        }
                    }
                    if (hit != null) {
                        historicSearchCache.set(hit.launches.filter { !it.id.startsWith("demo-") })
                        lastHistoricSearchQ = q
                        lastHistoricSearchMs = System.currentTimeMillis()
                        sharedStatus = "SEARCH OK | q=$q | n=${historicSearchCache.get().size} | $src"
                        sharedError = null
                    } else {
                        sharedStatus = "SEARCH FAIL | q=$q | ${sharedError ?: "no data"}"
                    }
                } catch (e: Exception) {
                    sharedError = e.message ?: "search exception"
                    sharedStatus = "SEARCH EX | ${e.javaClass.simpleName}: ${e.message}"
                    Log.e(TAG, "historic search failed: ${e.message}", e)
                }
            } finally {
                historicSearching = false
                try { onDone?.invoke() } catch (e: Exception) {
                    Log.e(TAG, "historic search onDone: ${e.message}", e)
                }
                // Stamp 110: run newest queued query (keep prior cache until SEARCH OK replaces).
                val queued = queuedHistoricSearchQ
                queuedHistoricSearchQ = null
                if (!queued.isNullOrBlank() && !queued.equals(q, ignoreCase = true)) {
                    requestHistoricSearch(queued, onDone)
                }
            }
        }
    }


    /**
     * CURRENT / CMD live picker (stamp 55): upcoming within horizon +
     * HOLD / in-flight / webcast / T+ gates only. Past Spectrum -> HISTORIC.
     */
    fun pickerPool(now: Long = System.currentTimeMillis(), horizonDays: Int = 14): List<LaunchSnapshot> {
        val upcomingHorizonSec = maxOf(
            horizonDays.toLong() * 24L * 3600L,
            LaunchWindow.UPCOMING_MIN_SEC
        )
        return livePool()
            .filter {
                if (it.isActiveWatch(now)) return@filter true // stamp 56: no bare isHold — past Spectrum ? HISTORIC
                val t = it.secondsToNet(now)
                t > 0 && t <= upcomingHorizonSec
            }
            .sortedBy { it.netMs }
    }

    /**
     * AUTO: HOLD / Go / in-flight / webcast-live / T+ watch FIRST, closest to now.
     * Only if none of those exist, soonest future NET.
     */
    fun getNextAny(now: Long = System.currentTimeMillis()): LaunchSnapshot? {
        val live = livePool()
        // Stamp 57: isActiveWatch Hold gate must NOT empty this path for upcoming birds.
        // Watch bucket first; else soonest future NET (t>0) — day-out Starlink still wins.
        val watch = live.filter { it.isActiveWatch(now) }
            .minByOrNull { kotlin.math.abs(it.secondsToNet(now)) }
        if (watch != null) return watch
        val upcoming = live.filter { it.secondsToNet(now) > 0 }.minByOrNull { it.netMs }
        if (upcoming != null) return upcoming
        // Stamp 93: CURRENT/AUTO never falls back to demos — await live fetch.
        return null
    }

    fun findById(id: String): LaunchSnapshot? =
        cache.get()?.launches?.firstOrNull { it.id == id }
            ?: pastCache.get()?.launches?.firstOrNull { it.id == id }
            // Stamp 108: historic name-search hits must resolve (Flight 13 / USSF-259).
            ?: historicSearchCache.get().firstOrNull { it.id == id }
            ?: demoCatalog.firstOrNull { it.id == id }

    /**
     * Stamp 108: upsert a HISTORIC search/spinner pick into pastCache so wallpaper
     * Engine findById never misses and never falls through to live Soyuz.
     * demoCatalog unchanged; demos stay offline-only.
     */
    fun rememberHistoricSelection(s: LaunchSnapshot) {
        if (s.id.isBlank() || s.id.startsWith("demo-")) return
        val prior = pastCache.get()
        val merged = LinkedHashMap<String, LaunchSnapshot>()
        // Selected bird first, then prior past (newest order rebuilt below).
        merged[s.id] = s
        for (l in prior?.launches.orEmpty()) {
            if (l.id !in merged) merged[l.id] = l
        }
        val sorted = merged.values.sortedByDescending { it.netMs }
        pastCache.set(
            LaunchListResult(
                sorted,
                System.currentTimeMillis(),
                prior?.source ?: "sticky-historic"
            )
        )
        // Stamp 112 light: never push CURRENT upcoming selection into historic search cache (F14 bleed).
        if (s.secondsToNet() <= 0 || s.isReplayable()) {
            val sc = historicSearchCache.get().toMutableList()
            if (sc.none { it.id == s.id }) {
                sc.add(0, s)
                historicSearchCache.set(sc)
            }
        }
    }

    /**
     * Stamp 72: once a bird is in the live catalog, NEVER drop on refresh unless
     * scrubbed / flew / terminal. Upsert fetched fields by id onto PRIOR cache;
     * do not full-replace with one LL2 upcoming page.
     */
    private fun isScrubbedOrTerminal(s: LaunchSnapshot): Boolean {
        if (s.isTerminal()) return true
        val blob = "${s.statusAbbrev} ${s.statusName}".lowercase()
        return "scrub" in blob
    }


    /**
     * Tip 121: prod NET always beats lldev NET for the same id.
     * Later-vs-earlier only among the same trust tier (both prod, or both unknown).
     * lldev must never own display net/window/status.
     */
    private fun isProdSourceTag(tag: String?): Boolean {
        if (tag.isNullOrBlank()) return false
        val s = tag.lowercase()
        // Tip 123 ROOT: "lldev+prod-net" must NOT count as prod — that poisoned vault with Sep18.
        if ("lldev" in s) return false
        return s == "ll2" || s.startsWith("ll2") || s == "ll2-id" || s.startsWith("ll2+")
    }

    private fun isLldevSourceTag(tag: String?): Boolean {
        if (tag.isNullOrBlank()) return false
        return "lldev" in tag.lowercase()
    }

    private fun overlayNetFields(base: LaunchSnapshot, from: LaunchSnapshot): LaunchSnapshot {
        return base.copy(
            netMs = from.netMs,
            windowStartMs = from.windowStartMs,
            windowEndMs = from.windowEndMs,
            statusName = from.statusName,
            statusAbbrev = from.statusAbbrev
        )
    }

    private fun overlayVault(base: LaunchSnapshot): LaunchSnapshot = ProdNetVault.applyTo(base)

    /**
     * Tip 121 preferProdNet: if [prod] present, use its NET fields + remember vault.
     * Else vault. Else prior only if not lldev-poisoned. Never keep bare lldev NET when better exists.
     */
    private fun preferProdNet(
        base: LaunchSnapshot,
        prod: LaunchSnapshot?,
        prior: LaunchSnapshot?,
        incomingSource: String
    ): LaunchSnapshot {
        if (prod != null) {
            ProdNetVault.rememberProd(prod)
            val out = overlayNetFields(base, prod)
            logF14Net(out, "prod-by-id")
            return out
        }
        val vaulted = overlayVault(base)
        if (vaulted.netMs != base.netMs || vaulted.windowStartMs != base.windowStartMs) {
            logF14Net(vaulted, "vault")
            return vaulted
        }
        // Prior from in-memory cache: only if later than lldev base (poison guard) OR incoming is prod
        if (prior != null && prior.id == base.id) {
            if (isLldevSourceTag(incomingSource) && prior.netMs > base.netMs) {
                val out = overlayNetFields(base, prior)
                logF14Net(out, "prior-over-lldev")
                return out
            }
            if (isProdSourceTag(incomingSource)) {
                return preferLaterAmongProd(prior, base)
            }
        }
        if (isLldevSourceTag(incomingSource)) {
            // Last resort: still try vault again (no-op) — do NOT invent; keep structure but log
            Log.e(TAG, "NET FAIL lldev kept — no vault/prod id=${base.id} netMs=${base.netMs} ${base.name}")
            logF14Net(base, "lldev-UNTRUSTED-kept-no-vault")
        }
        return base
    }

    private fun preferLaterAmongProd(a: LaunchSnapshot, b: LaunchSnapshot): LaunchSnapshot {
        val now = System.currentTimeMillis()
        return if (a.netMs > now && (b.netMs <= now || a.netMs > b.netMs)) {
            overlayNetFields(b, a)
        } else b
    }

    private fun logF14Net(s: LaunchSnapshot, src: String) {
        if (s.id == F14_LL2_ID || isStarshipFlight14(s)) {
            val days = (s.netMs - System.currentTimeMillis()) / (24.0 * 3600_000.0)
            Log.i(TAG, "F14 NET src=$src id=${s.id} netMs=${s.netMs} ~[${String.format(Locale.US, "%.1f", days)}d] status=${s.statusAbbrev}")
        }
    }

    private fun isStarshipFlight14(s: LaunchSnapshot): Boolean {
        if (s.id == F14_LL2_ID) return true
        val blob = "${s.name} ${s.rocketName} ${s.missionName}".lowercase()
        if ("starship" !in blob) return false
        // Tip 121: NO bare "14" — only flight/ift 14 forms
        return Regex("""(?i)(flight\s*[-#]?\s*14|ift\s*[-#]?\s*14|flight14|ift14)""")
            .containsMatchIn(blob)
    }

    /**
     * Tip 124 ROOT (Chris soft-FAIL: TRACKING == lldev body exactly):
     * - Pure prod catalog: vault stamp + overlay only (no mass by-id).
     * - Forced lldev: F14/critical prod-by-id FIRST AND ONLY (no other fetches → no 429 kill).
     * - Caller must not cache.set until sealCriticalNets returns true for F14.
     */
    private fun enforceProdNetAuthority(
        list: LaunchListResult,
        incomingSource: String
    ): LaunchListResult {
        val now = System.currentTimeMillis()
        val priorById = cache.get()?.launches?.associateBy { it.id } ?: emptyMap()
        val byId = LinkedHashMap<String, LaunchSnapshot>()
        for (l in list.launches) byId[l.id] = l

        val upcoming = list.launches.filter { !it.id.startsWith("demo-") && it.secondsToNet(now) > -3600 }

        if (isProdSourceTag(incomingSource)) {
            ProdNetVault.rememberProdList(upcoming)
            var fromVault = 0
            for (l in upcoming) {
                val before = byId[l.id] ?: l
                val after = overlayVault(before)
                if (after.netMs != before.netMs) fromVault++
                byId[l.id] = after
            }
            // Still pin F14 by-id once when missing from vault (edge)
            val f14 = byId[F14_LL2_ID] ?: upcoming.firstOrNull { isStarshipFlight14(it) }
            if (f14 != null && ProdNetVault.get(f14.id) == null) {
                val prod = fetchLaunchById(f14.id)
                if (prod != null) {
                    byId[f14.id] = preferProdNet(byId[f14.id] ?: f14, prod, priorById[f14.id], incomingSource)
                }
            }
            for (l in upcoming) byId[l.id] = overlayVault(byId[l.id] ?: l)
            val src = "$incomingSource+vault"
            byId[F14_LL2_ID]?.let { logF14Net(it, "FINAL-$src") }
            sharedStatus = "prod catalog | vault=$fromVault | $src"
            Log.i(TAG, lastStatus)
            return LaunchListResult(byId.values.sortedBy { it.netMs }, System.currentTimeMillis(), src)
        }

        // ---- Forced lldev body: critical ONLY ----
        var fromVault = 0
        for (l in upcoming) {
            val before = byId[l.id] ?: l
            val after = overlayVault(before)
            if (after.netMs != before.netMs) fromVault++
            byId[l.id] = after
        }

        val critical = upcoming.filter { l ->
            l.id == F14_LL2_ID || isStarshipFlight14(byId[l.id] ?: l)
        }.distinctBy { it.id }
        // Tip 124: FIRST AND ONLY — never fetch other birds (429 killed F14 on tip122/123).
        var fromProd = 0
        for (l in critical) {
            val prior = priorById[l.id]
            var prod = fetchLaunchById(l.id)
            if (prod == null) {
                Log.w(TAG, "prod-by-id miss critical ${l.id.take(8)} — retry")
                try { Thread.sleep(2500L) } catch (_: InterruptedException) {}
                prod = fetchLaunchById(l.id)
            }
            if (prod == null) {
                try { Thread.sleep(3500L) } catch (_: InterruptedException) {}
                prod = fetchLaunchById(l.id)
            }
            val before = byId[l.id] ?: l
            val after = preferProdNet(before, prod, prior, incomingSource)
            if (prod != null && after.netMs == prod.netMs) fromProd++
            byId[l.id] = after
            logF14Net(after, if (prod != null) "prod-by-id" else "lldev-critical-miss")
        }
        for (l in upcoming) byId[l.id] = overlayVault(byId[l.id] ?: l)

        val src = when {
            fromProd > 0 -> "lldev+prod-net-auth"
            fromVault > 0 -> "lldev+vault-net"
            else -> incomingSource
        }
        byId[F14_LL2_ID]?.let { logF14Net(it, "FINAL-$src") }
        sharedStatus = "lldev body | critical-only fetch=$fromProd vault=$fromVault | $src"
        Log.i(TAG, lastStatus)
        return LaunchListResult(byId.values.sortedBy { it.netMs }, System.currentTimeMillis(), src)
    }

    /** Tip 124: F14 sealed only when vault holds matching NET (prod-by-id or prod list wrote it). */
    private fun isCriticalNetSealed(list: LaunchListResult): Boolean {
        val f14 = list.launches.firstOrNull { it.id == F14_LL2_ID || isStarshipFlight14(it) }
            ?: return true
        if (isProdSourceTag(list.source)) return true
        val vault = ProdNetVault.get(f14.id) ?: return false
        return f14.netMs == vault.netMs
    }

    private fun formatNetIso(netMs: Long): String {
        return try {
            java.time.Instant.ofEpochMilli(netMs).toString()
        } catch (_: Exception) {
            netMs.toString()
        }
    }

    private fun upsertMergeLive(
        prior: List<LaunchSnapshot>,
        fetchedUpcoming: List<LaunchSnapshot>?,
        previous: List<LaunchSnapshot>?
    ): List<LaunchSnapshot> {
        val now = System.currentTimeMillis()
        val byId = LinkedHashMap<String, LaunchSnapshot>()
        for (p in prior) {
            if (!isScrubbedOrTerminal(p)) byId[p.id] = p
        }
        for (f in fetchedUpcoming.orEmpty()) {
            if (isScrubbedOrTerminal(f)) {
                byId.remove(f.id)
            } else {
                // Tip 121: merge with vault/prod authority — never let lldev NET wipe good prod.
                val prior = byId[f.id]
                var merged = f
                if (prior != null) {
                    val vaulted = overlayVault(prior)
                    if (vaulted.netMs >= f.netMs || ProdNetVault.get(f.id) != null) {
                        merged = overlayNetFields(f, if (ProdNetVault.get(f.id) != null) overlayVault(f) else vaulted)
                    }
                } else {
                    merged = overlayVault(f)
                }
                byId[f.id] = merged
            }
        }
        for (p in previous.orEmpty()) {
            if (isScrubbedOrTerminal(p)) {
                byId.remove(p.id)
                continue
            }
            if (p.id in byId) continue
            // Active watch from previous page can re-enter live.
            if (p.isActiveWatch(now)) byId[p.id] = p
        }
        return byId.values.sortedBy { it.netMs }
    }

    fun refreshIfNeeded(force: Boolean = false, onDone: ((LaunchListResult?) -> Unit)? = null) {
        val now = System.currentTimeMillis()
        if (!force && cache.get() != null && (now - lastFetchMs) < minIntervalMs) {
            sharedStatus = "Cached | ${lastCount} launches | ${lastSource}"
            onDone?.invoke(cache.get())
            return
        }
        if (isFetching) {
            // tip138: queue instead of drop — fast adb/Engine recreate was starving live paint
            sharedStatus = "Fetching..."
            refreshQueued = true
            refreshQueuedForce = refreshQueuedForce || force
            if (onDone != null) pendingRefreshOnDone.set(onDone)
            onDone?.invoke(cache.get())
            return
        }
        sharedFetching = true
        sharedStatus = "Fetching Launch Library 2..."
        sharedError = null
        executor.execute {
            try {
                var source = "ll2"
                var upcoming: LaunchListResult? = null
                // Tip 124: prod upcoming must win — never prefer lldev when prod 200.
                for (attempt in 1..3) {
                    sharedStatus = "Prod upcoming attempt $attempt/3..."
                    upcoming = fetchList(PROD_UPCOMING, "ll2")
                    if (upcoming != null) {
                        source = "ll2"
                        Log.i(TAG, "PROD upcoming OK attempt=$attempt n=${upcoming.launches.size}")
                        break
                    }
                    Log.w(TAG, "PROD upcoming miss attempt=$attempt — backoff")
                    try { Thread.sleep(3500L) } catch (_: InterruptedException) {}
                }
                if (upcoming != null) {
                    // Prod catalog body — never fall through to lldev for display.
                    upcoming = enforceProdNetAuthority(upcoming, source)
                    source = upcoming.source
                } else {
                    // Keep prior PROD catalog if we have one — do not replace with lldev body.
                    val priorCache = cache.get()
                    if (priorCache != null && isProdSourceTag(priorCache.source) && priorCache.launches.isNotEmpty()) {
                        Log.w(TAG, "PROD miss — KEEP prior prod catalog n=${priorCache.launches.size} (no lldev replace)")
                        upcoming = enforceProdNetAuthority(priorCache, priorCache.source)
                        source = upcoming.source
                    } else {
                        sharedStatus = "Prod throttled — lldev body + F14 seal only"
                        upcoming = fetchList(DEV_UPCOMING, "lldev")
                        if (upcoming != null) {
                            source = "lldev"
                            // Tip 124: critical-only; never rememberProdList lldev rows (enforce handles).
                            upcoming = enforceProdNetAuthority(upcoming, source)
                            source = upcoming.source
                            // Seal F14 before allowing cache.set — retry critical until sealed or give up.
                            var sealTries = 0
                            while (!isCriticalNetSealed(upcoming!!) && sealTries < 4) {
                                sealTries++
                                Log.w(TAG, "F14 not sealed — retry seal $sealTries/4")
                                try { Thread.sleep(3000L) } catch (_: InterruptedException) {}
                                upcoming = enforceProdNetAuthority(upcoming!!, "lldev")
                                source = upcoming.source
                            }
                            if (!isCriticalNetSealed(upcoming!!)) {
                                Log.e(TAG, "F14 STILL UNSEALED after retries — refusing lldev cache.set for live NET")
                            }
                        }
                    }
                }
                var previous = fetchList(PROD_PREVIOUS, "ll2")
                if (previous == null) {
                    previous = fetchList(DEV_PREVIOUS, "lldev")
                }
                previous?.let { fetchedPast ->
                    // Stamp 112: while historic search active, merge past — never wipe sticky/search picks mid-type.
                    if (historicSearchInterestQ.isNotBlank()) {
                        val merged = LinkedHashMap<String, LaunchSnapshot>()
                        for (l in fetchedPast.launches) merged[l.id] = l
                        for (l in pastCache.get()?.launches.orEmpty()) {
                            if (l.id !in merged) merged[l.id] = l
                        }
                        pastCache.set(
                            fetchedPast.copy(
                                launches = merged.values.sortedByDescending { it.netMs }
                            )
                        )
                    } else {
                        pastCache.set(fetchedPast)
                    }
                }
                val prior = cache.get()
                val priorList = prior?.launches.orEmpty()
                // Stamp 72: upcoming fetch fail/null — KEEP prior cache; never cache.set(empty).
                if (upcoming == null) {
                    sharedError = lastError ?: "upcoming fetch failed"
                    val pastN = previous?.launches?.size ?: pastCache.get()?.launches?.size ?: 0
                    // Stamp 106: past already replaced when previous != null; surface HTTP error.
                    val pastNote = if (previous != null) "past=$pastN OK" else "past KEEP/FAIL"
                    val kept = upsertMergeLive(priorList, null, pastCache.get()?.launches)
                    if (kept.isNotEmpty()) {
                        val keptResult = LaunchListResult(
                            kept,
                            System.currentTimeMillis(),
                            prior?.source ?: source
                        )
                        cache.set(keptResult)
                        sharedCount = kept.size
                        sharedSource = keptResult.source
                        sharedStatus = "KEEP | prior=${kept.size} | upcoming FAIL | $pastNote | ${sharedError} | $source"
                        Log.w(TAG, lastStatus)
                        onDone?.invoke(keptResult)
                    } else {
                        sharedStatus = "KEEP EMPTY | upcoming FAIL | $pastNote | ${sharedError} | $source"
                        Log.w(TAG, lastStatus)
                        onDone?.invoke(cache.get())
                    }
                    return@execute
                }
                // Tip 124: refuse cache.set of unsealed F14 on lldev body (customer miss).
                val mergedLaunches = upsertMergeLive(priorList, upcoming.launches, pastCache.get()?.launches)
                val sealedList = mergedLaunches.map { overlayVault(it) }
                var merged = upcoming.copy(launches = sealedList, source = source)
                if (isLldevSourceTag(source) && !isCriticalNetSealed(merged)) {
                    Log.e(TAG, "CACHE-SET BLOCKED — F14 unsealed src=$source")
                    // Keep prior cache if any; do not publish lldev Sep18 as TRACKING truth.
                    val kept = cache.get()
                    if (kept != null) {
                        sharedStatus = "BLOCKED lldev F14 unsealed — kept prior ${kept.source}"
                        onDone?.invoke(kept)
                        return@execute
                    }
                    // Fresh install: still set structure but force one more F14 fetch into list
                    val force = fetchLaunchById(F14_LL2_ID)
                    if (force != null) {
                        ProdNetVault.rememberProd(force)
                        val patched = sealedList.map { if (it.id == F14_LL2_ID || isStarshipFlight14(it)) overlayNetFields(it, force) else it }
                        merged = merged.copy(launches = patched, source = "lldev+prod-net-auth")
                        source = merged.source
                    }
                }
                cache.set(merged)
                lastFetchMs = System.currentTimeMillis()
                sharedCount = merged.launches.size
                sharedSource = source
                val f14 = merged.launches.firstOrNull { it.id == F14_LL2_ID || isStarshipFlight14(it) }
                if (f14 != null) {
                    Log.i(TAG, "CACHE-SET src=$source F14 netMs=${f14.netMs} ISO=${formatNetIso(f14.netMs)} ~[${String.format(java.util.Locale.US, "%.1f", f14.secondsToNet() / 86400.0)}d] status=${f14.statusAbbrev} n=${merged.launches.size}")
                    logF14Net(f14, "CACHE-SET-$source")
                } else {
                    Log.i(TAG, "CACHE-SET src=$source F14=absent n=${merged.launches.size}")
                }
                sharedError = null
                val pastN = previous?.launches?.size ?: 0
                sharedStatus = "OK | upsert=${merged.launches.size} fetch=${upcoming.launches.size} past=$pastN | $source"
                Log.i(TAG, lastStatus)
                onDone?.invoke(merged)
            } finally {
                sharedFetching = false
                val need = refreshQueued
                val forceAgain = refreshQueuedForce
                refreshQueued = false
                refreshQueuedForce = false
                val cb = pendingRefreshOnDone.getAndSet(null)
                if (need) {
                    refreshIfNeeded(force = forceAgain, onDone = cb)
                }
            }
        }
    }

    private fun fetchList(urlStr: String, sourceTag: String): LaunchListResult? {
        return try {
            fun once(): Pair<Int, String?> {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 12_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "LiveRocketTracker/1.0.114 (Android; tip124-prod-catalog)")
                }
                val code = conn.responseCode
                if (code != 200) {
                    val errBody = try { conn.errorStream?.bufferedReader()?.use { it.readText() } } catch (_: Exception) { null }
                    return code to errBody
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                return 200 to body
            }
            var (code, bodyOrErr) = once()
            // Stamp 106: one 429 backoff — do not hammer LL2.
            if (code == 429) {
                sharedError = "HTTP 429 throttle"
                Log.w(TAG, "HTTP 429 from $urlStr — backoff 3.5s")
                try { Thread.sleep(3500L) } catch (_: InterruptedException) {}
                val retry = once()
                code = retry.first
                bodyOrErr = retry.second
            }
            if (code != 200) {
                sharedError = "HTTP $code${bodyOrErr?.let { " | ${it.take(80)}" } ?: ""}"
                Log.w(TAG, "HTTP $code from $urlStr | $bodyOrErr")
                return null
            }
            parseList(bodyOrErr ?: return null, sourceTag)
        } catch (e: Exception) {
            sharedError = e.message ?: "network error"
            Log.e(TAG, "Fetch failed: ${e.message}")
            null
        }
    }



    /**
     * Stamp 113: lldev catalog can carry stale NET. For soonest birds, try prod by id
     * and adopt prod net/window when available. Cap to avoid hammering LL2.
     */
    /**
     * Stamp 115: F14 was outside soonest-12 enrich window → stale lldev Sep18 [1d] stuck.
     * Always prod-refresh Starship Flight 14 (and other named critical) NETs.
     */
    /** Tip 121: kept name for call sites — delegates to enforceProdNetAuthority. */
    private fun ensureCriticalNetsFromProd(list: LaunchListResult): LaunchListResult {
        return enforceProdNetAuthority(list, list.source)
    }

    private fun enrichNetsFromProd(lldev: LaunchListResult): LaunchListResult {
        val now = System.currentTimeMillis()
        val soon = lldev.launches
            .filter { !it.id.startsWith("demo-") && it.secondsToNet(now) > 0 }
            .sortedBy { it.netMs }
            .take(12)
        // Stamp 115: also force-include Starship Flight 14 even if not in soonest 12.
        val must = lldev.launches.filter { isStarshipFlight14(it) }
        val ranked = (soon + must).distinctBy { it.id }
        if (ranked.isEmpty()) return lldev
        val byId = LinkedHashMap<String, LaunchSnapshot>()
        for (l in lldev.launches) byId[l.id] = l
        var patched = 0
        val priorById = cache.get()?.launches?.associateBy { it.id } ?: emptyMap()
        for (l in ranked) {
            val prod = fetchLaunchById(l.id)
            val prior = priorById[l.id]
            val before = byId[l.id] ?: l
            val after = preferProdNet(before, prod, prior, "lldev")
            if (prod != null && after.netMs != l.netMs) patched++
            byId[l.id] = after
        }
        val src = if (patched > 0) "lldev+prod-net" else lldev.source
        sharedStatus = "lldev enrich | patched=$patched / ${ranked.size} | $src"
        return LaunchListResult(byId.values.sortedBy { it.netMs }, System.currentTimeMillis(), src)
    }

    /** Stamp 113: GET one launch from prod LL2 (single-object JSON). */
    private fun fetchLaunchById(id: String): LaunchSnapshot? {
        if (id.isBlank() || id.startsWith("demo-")) return null
        return try {
            val urlStr = "https://ll.thespacedevs.com/2.2.0/launch/$id/?mode=detailed"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LiveRocketTracker/1.0.114 (Android; tip124-prod-catalog)")
            }
            val code = conn.responseCode
            if (code != 200) {
                Log.w(TAG, "fetchLaunchById HTTP $code id=$id")
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseLaunchObject(JSONObject(body), "ll2-id")
        } catch (e: Exception) {
            Log.w(TAG, "fetchLaunchById $id: ${e.message}")
            null
        }
    }

    private fun parseList(json: String, source: String): LaunchListResult {
        val root = JSONObject(json)
        val results = root.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<LaunchSnapshot>()
        for (i in 0 until results.length()) {
            val snap = parseLaunchObject(results.getJSONObject(i), source) ?: continue
            list.add(snap)
        }
        return LaunchListResult(list, System.currentTimeMillis(), source)
    }

    /** Stamp 113: one LL2 launch object → snapshot (results[] or GET /launch/{id}/). */
    private fun parseLaunchObject(o: JSONObject, source: String): LaunchSnapshot? {
        val status = o.optJSONObject("status")
        val netStr = strOrNull(o, "net") ?: return null
        val netMs = parseIso(netStr) ?: return null
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
        val id = strOrNull(o, "id") ?: return null
        return LaunchSnapshot(
            id = id,
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

    /** Safe JSON string extract  -  avoids Kotlin Nothing? vs String mismatch on optString(key, null). */
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
            // SpaceX  -  upcoming-style (T- ~1h for testing countdown)
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
            // SpaceX  -  already in flight (for metrics / attitude)
            LaunchSnapshot(
                id = "demo-spacex-inflight",
                name = "Falcon 9 Block 5 | In-Flight Test",
                statusName = "In Flight",
                statusAbbrev = "In Flight",
                netMs = now - 95 * 1000L,   // T+95s -> mid ascent
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

