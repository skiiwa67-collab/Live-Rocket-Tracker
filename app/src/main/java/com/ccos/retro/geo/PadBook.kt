package com.ccos.retro.geo

import android.content.Context
import com.ccos.retro.data.LaunchSnapshot
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer
import kotlin.math.abs

/**
 * Every public Launch Library 2 pad with published lon/lat.
 * Sea / barge / air-launch pads stay in the water. Land pads sit on real coast.
 * Natural Earth land is the ground truth. Do not default unknown sites to Cape.
 *
 * find() is O(sites) with pre-normalized strings. Never compile Regex in paint.
 */
object PadBook {
    class Site(
        val lon: Float,
        val lat: Float,
        val waterAz: Float,
        val sea: Boolean,
        val inland: Boolean,
        val name: String,
        val location: String,
        val locNorm: String,
        val nameNorm: String
    )

    @Volatile
    var sites: Array<Site> = emptyArray()
        private set

    @Volatile
    private var loaded = false

    private val stop = setOf(
        "pad", "launch", "complex", "site", "center", "centre", "space", "spaceport",
        "unknown", "area", "mobile", "commercial", "satellite", "the", "and", "for",
        "from", "republic", "people", "federative", "state", "usa", "of", "at",
        "sls", "lc", "slc", "sfs"
    )

    private val aliases = listOf(
        "kodiak" to "pacific spaceport",
        "vafb" to "vandenberg",
        "ccsfs" to "cape canaveral",
        "ksc" to "kennedy space",
        "sriharikota" to "satish dhawan",
        "boca chica" to "starbase",
        "guiana" to "kourou"
    )

    private var cacheId: String? = null
    private var cacheSite: Site? = null

    fun ensure(ctx: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            sites = try {
                load(ctx)
            } catch (_: Exception) {
                emptyArray()
            }
            loaded = true
        }
    }

    fun lonLat(launch: LaunchSnapshot?): Pair<Float, Float>? {
        val lon = launch?.padLon
        val lat = launch?.padLat
        if (lon != null && lat != null) {
            if (abs(lon) > 0.01f || abs(lat) > 0.01f || isSea(launch)) {
                return lon to lat
            }
        }
        return find(launch)?.let { it.lon to it.lat }
    }

    fun isSea(launch: LaunchSnapshot?): Boolean {
        find(launch)?.let { return it.sea }
        val blob = norm("${launch?.pad.orEmpty()} ${launch?.location.orEmpty()} ${launch?.name.orEmpty()}")
        return "yellow sea" in blob || "south china sea" in blob || "offshore" in blob ||
            "sea launch" in blob || "odyssey" in blob || "air launch" in blob
    }

    fun find(launch: LaunchSnapshot?): Site? {
        if (launch == null) return null
        val id = launch.id
        if (id == cacheId) return cacheSite
        val hit = findHay("${launch.pad} ${launch.location} ${launch.name}")
        cacheId = id
        cacheSite = hit
        return hit
    }

    fun findHay(raw: String): Site? {
        if (sites.isEmpty()) return null
        var hay = norm(raw)
        if (hay.isBlank()) return null
        for ((a, b) in aliases) {
            if (a in hay) hay = "$hay $b"
        }
        var best: Site? = null
        var bestScore = 0
        for (s in sites) {
            var score = 0
            val loc = s.locNorm
            val name = s.nameNorm
            if (loc.length >= 6 && loc in hay) score += loc.length
            if (name.length >= 8 && "unknown" !in name && name in hay) score += name.length
            var i = 0
            val L = loc.length
            while (i < L) {
                while (i < L && loc[i] == ' ') i++
                val start = i
                while (i < L && loc[i] != ' ') i++
                if (i - start >= 5) {
                    val tok = loc.substring(start, i)
                    if (tok !in stop && tok in hay) score += tok.length
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = s
            }
        }
        return if (bestScore >= 6) best else null
    }

    fun norm(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        val sb = StringBuilder(n.length)
        var i = 0
        val len = n.length
        while (i < len) {
            val c = n[i]
            val lc = if (c in 'A'..'Z') c + 32 else c
            if (lc in 'a'..'z' || lc in '0'..'9') {
                sb.append(lc)
            } else if (sb.isNotEmpty() && sb[sb.length - 1] != ' ') {
                sb.append(' ')
            }
            i++
        }
        if (sb.isNotEmpty() && sb[sb.length - 1] == ' ') sb.setLength(sb.length - 1)
        return sb.toString()
    }

    private fun load(ctx: Context): Array<Site> {
        val raw = ctx.assets.open("ne_pads.bin").use { it.readBytes() }
        val b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val mag = ByteArray(4)
        b.get(mag)
        if (String(mag, Charsets.US_ASCII) != "PAD1") return emptyArray()
        val n = b.int
        return Array(n) {
            val lon = b.float
            val lat = b.float
            val az = b.float
            b.float
            val flags = b.get().toInt() and 0xFF
            val nl = b.short.toInt() and 0xFFFF
            val ll = b.short.toInt() and 0xFFFF
            val nb = ByteArray(nl)
            b.get(nb)
            val lb = ByteArray(ll)
            b.get(lb)
            val nm = String(nb, Charsets.UTF_8)
            val loc = String(lb, Charsets.UTF_8)
            Site(
                lon = lon,
                lat = lat,
                waterAz = az,
                sea = flags and 1 != 0,
                inland = flags and 2 != 0,
                name = nm,
                location = loc,
                locNorm = norm(loc),
                nameNorm = norm(nm)
            )
        }
    }
}
