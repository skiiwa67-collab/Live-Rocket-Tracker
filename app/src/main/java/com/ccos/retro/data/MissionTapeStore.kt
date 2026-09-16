package com.ccos.retro.data

import android.content.Context
import android.util.Log
import com.ccos.retro.BuildConfig
import com.ccos.retro.event.EventSeverity
import com.ccos.retro.event.FlightEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Stamp 109: Mission tape CDN pull (Starship gold).
 * HARD: never present family templates as historic truth without PENDING REAL TELEMETRY.
 */
data class MissionTape(
    val id: String,
    val slug: String?,
    val family: String?,
    val name: String?,
    val truth: String,
    val pending: Boolean,
    val events: List<MissionTapeEvent>,
    val etag: String? = null
) {
    fun hasUsableTruth(): Boolean {
        val t = truth.lowercase()
        return !pending && events.isNotEmpty() && (t == "observed" || t == "published")
    }

    fun toFlightEvents(): List<FlightEvent> =
        events.map {
            FlightEvent(
                id = it.id,
                tSec = it.tSec,
                title = it.title,
                detail = it.detail,
                severity = it.severity
            )
        }.sortedBy { it.tSec }
}

data class MissionTapeEvent(
    val id: String,
    val tSec: Float,
    val title: String,
    val detail: String,
    val severity: EventSeverity
)

object MissionTapeStore {
    private const val TAG = "CCOS.MissionTape"
    private const val PENDING_LABEL = "PENDING REAL TELEMETRY"
    private const val LIVE_POLL_MS = 20_000L

    private val executor = Executors.newSingleThreadExecutor()
    private val memory = ConcurrentHashMap<String, MissionTape>()
    private val appCtx = AtomicReference<Context?>(null)
    private val fetchingIds = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var lastLivePollMs: Long = 0L
    @Volatile var sharedHudNote: String? = null
        private set

    fun ensure(context: Context) {
        val app = context.applicationContext
        appCtx.compareAndSet(null, app)
        cacheDir(app)?.mkdirs()
    }

    private fun cacheDir(ctx: Context? = appCtx.get()): File? {
        val c = ctx ?: return null
        return File(c.filesDir, "mission_tapes")
    }

    fun baseUrl(): String =
        BuildConfig.MISSION_TAPE_BASE_URL.trimEnd('/')

    fun get(id: String?): MissionTape? {
        if (id.isNullOrBlank()) return null
        memory[id]?.let { return it }
        // Also try by scanning memory for slug matches is N/A; disk load:
        return loadDisk(id)
    }

    fun hasUsableTruth(id: String?): Boolean = get(id)?.hasUsableTruth() == true

    /** True when historic/live should show PENDING (no usable CDN truth yet). */
    fun needsPendingBanner(launch: LaunchSnapshot?): Boolean {
        if (launch == null) return false
        if (launch.id.startsWith("demo-")) return false
        val tape = get(launch.id)
        if (tape?.hasUsableTruth() == true) return false
        // CDN returned pending/empty placeholder — always honest.
        if (tape != null) return true
        // No tape yet: Starship gold + historic replay must not look like locked truth.
        val blob = "${launch.rocketName} ${launch.name} ${launch.missionName}".lowercase()
        val starship = "starship" in blob || "super heavy" in blob || "ift-" in blob || "ift " in blob
        if (starship) return true
        if (launch.isReplayable()) return true
        return false
    }


    fun pendingNote(launch: LaunchSnapshot?): String? =
        if (needsPendingBanner(launch)) PENDING_LABEL else null

    fun refreshHudNote(launch: LaunchSnapshot?) {
        sharedHudNote = pendingNote(launch)
    }

    /**
     * HISTORIC select / LIVE Starship poll.
     * UUID primary; Starship Flight N slug fallback when uuid miss.
     */
    fun requestFor(launch: LaunchSnapshot, force: Boolean = false, onDone: (() -> Unit)? = null) {
        val ctx = appCtx.get()
        if (ctx == null) {
            onDone?.invoke()
            return
        }
        val id = launch.id
        if (id.isBlank() || id.startsWith("demo-")) {
            onDone?.invoke()
            return
        }
        if (!force) {
            val have = get(id)
            if (have?.hasUsableTruth() == true) {
                refreshHudNote(launch)
                onDone?.invoke()
                return
            }
        }
        if (!fetchingIds.add(id)) {
            onDone?.invoke()
            return
        }
        executor.execute {
            try {
                var tape = httpGetJson("${baseUrl()}/$id.json", id)
                if (tape == null) {
                    val slugPath = starshipSlugPath(launch)
                    if (slugPath != null) {
                        tape = httpGetJson("${baseUrl()}/$slugPath", id)
                    }
                }
                if (tape != null) {
                    memory[id] = tape
                    if (tape.id.isNotBlank() && tape.id != id && tape.id != "PENDING_LL2_UUID") {
                        memory[tape.id] = tape
                    }
                    saveDisk(id, tape)
                }
                refreshHudNote(launch)
            } catch (e: Exception) {
                Log.e(TAG, "requestFor ${launch.id}: ${e.message}", e)
                sharedHudNote = PENDING_LABEL
            } finally {
                fetchingIds.remove(id)
                try {
                    onDone?.invoke()
                } catch (_: Exception) {
                }
            }
        }
    }

    /** LIVE CURRENT Starship: poll CDN every ~20s while watching. */
    fun maybePollLive(launch: LaunchSnapshot?, now: Long = System.currentTimeMillis()) {
        if (launch == null || launch.id.startsWith("demo-")) return
        val fam = launch.rocketName.lowercase() + " " + launch.name.lowercase()
        if ("starship" !in fam && "super heavy" !in fam) return
        // Live-ish: in flight / webcast / near NET
        val t = launch.secondsToNet(now)
        val liveish = launch.isInFlightStatus() || launch.isWebcastLive() ||
            (t in -6 * 3600L..2 * 3600L)
        if (!liveish) return
        if (now - lastLivePollMs < LIVE_POLL_MS) return
        lastLivePollMs = now
        requestFor(launch, force = true)
    }

    private fun starshipSlugPath(launch: LaunchSnapshot): String? {
        val blob = "${launch.name} ${launch.missionName} ${launch.rocketName}".lowercase()
        if ("starship" !in blob && "super heavy" !in blob && "ift" !in blob) return null
        val m = Regex("""(?:flight|ift)[-\s]?(\d{1,3})""").find(blob) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        return "starship/flight-$n.json"
    }

    private fun httpGetJson(urlStr: String, cacheKey: String): MissionTape? {
        return try {
            fun once(): Pair<Int, String?> {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "LiveRocketTracker/1.0.99 (mission-tape)")
                    val etagFile = etagFile(cacheKey)
                    if (etagFile?.exists() == true) {
                        val et = etagFile.readText().trim()
                        if (et.isNotEmpty()) setRequestProperty("If-None-Match", et)
                    }
                }
                val code = conn.responseCode
                if (code == 304) {
                    return 304 to null
                }
                if (code != 200) {
                    val err = try {
                        conn.errorStream?.bufferedReader()?.use { it.readText() }
                    } catch (_: Exception) {
                        null
                    }
                    return code to err
                }
                val etag = conn.getHeaderField("ETag")
                if (!etag.isNullOrBlank()) {
                    etagFile(cacheKey)?.writeText(etag)
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                return 200 to body
            }

            var (code, body) = once()
            if (code == 429) {
                Log.w(TAG, "HTTP 429 $urlStr — backoff 3.5s")
                try {
                    Thread.sleep(3500L)
                } catch (_: InterruptedException) {
                }
                val retry = once()
                code = retry.first
                body = retry.second
            }
            when (code) {
                200 -> if (body != null) parseTape(body) else null
                304 -> loadDisk(cacheKey)
                else -> {
                    Log.w(TAG, "HTTP $code $urlStr")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $urlStr: ${e.message}")
            null
        }
    }

    private fun parseTape(json: String): MissionTape? {
        return try {
            val o = JSONObject(json)
            val schema = o.optString("schema", "")
            if (schema.isNotBlank() && schema != "lrt.mission_tape.v1") {
                Log.w(TAG, "unexpected schema $schema")
            }
            val eventsArr = o.optJSONArray("events") ?: JSONArray()
            val events = mutableListOf<MissionTapeEvent>()
            for (i in 0 until eventsArr.length()) {
                val e = eventsArr.optJSONObject(i) ?: continue
                val id = e.optString("id", "ev$i").ifBlank { "ev$i" }
                val title = e.optString("title", "")
                if (title.isBlank()) continue
                val detail = e.optString("detail", "")
                val tSec = e.optDouble("t_sec", Double.NaN)
                if (tSec.isNaN()) continue
                val sev = when (e.optString("severity", "INFO").uppercase()) {
                    "WATCH" -> EventSeverity.WATCH
                    "FAIL" -> EventSeverity.FAIL
                    else -> EventSeverity.INFO
                }
                events += MissionTapeEvent(id, tSec.toFloat(), title, detail, sev)
            }
            MissionTape(
                id = o.optString("id", ""),
                slug = o.optString("slug", "").ifBlank { null },
                family = o.optString("family", "").ifBlank { null },
                name = o.optString("name", "").ifBlank { null },
                truth = o.optString("truth", "pending").ifBlank { "pending" },
                pending = o.optBoolean("pending", true),
                events = events
            )
        } catch (e: Exception) {
            Log.e(TAG, "parse: ${e.message}")
            null
        }
    }

    private fun diskFile(id: String): File? {
        val dir = cacheDir() ?: return null
        val safe = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safe.json")
    }

    private fun etagFile(id: String): File? {
        val dir = cacheDir() ?: return null
        val safe = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safe.etag")
    }

    private fun saveDisk(id: String, tape: MissionTape) {
        try {
            val f = diskFile(id) ?: return
            // Persist raw-ish reconstruct for offline
            val o = JSONObject()
            o.put("schema", "lrt.mission_tape.v1")
            o.put("id", tape.id)
            o.put("slug", tape.slug)
            o.put("family", tape.family)
            o.put("name", tape.name)
            o.put("truth", tape.truth)
            o.put("pending", tape.pending)
            val arr = JSONArray()
            for (e in tape.events) {
                val je = JSONObject()
                je.put("id", e.id)
                je.put("t_sec", e.tSec.toDouble())
                je.put("title", e.title)
                je.put("detail", e.detail)
                je.put(
                    "severity",
                    when (e.severity) {
                        EventSeverity.WATCH -> "WATCH"
                        EventSeverity.FAIL -> "FAIL"
                        else -> "INFO"
                    }
                )
                arr.put(je)
            }
            o.put("events", arr)
            f.writeText(o.toString())
        } catch (e: Exception) {
            Log.w(TAG, "saveDisk: ${e.message}")
        }
    }

    private fun loadDisk(id: String): MissionTape? {
        return try {
            val f = diskFile(id) ?: return null
            if (!f.exists()) return null
            val tape = parseTape(f.readText()) ?: return null
            memory[id] = tape
            tape
        } catch (_: Exception) {
            null
        }
    }
}
