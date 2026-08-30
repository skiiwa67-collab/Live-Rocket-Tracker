package com.ccos.retro.geo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

/**
 * Natural Earth coasts, towns, roads. Public domain.
 * Worker thread unpacks the planet, then clips land into 2° tiles around
 * real pads. TRAJ never walks a 66k-point continent on the UI thread.
 */
object GeoAtlas {
    class Ring(
        val hole: Boolean,
        val minLon: Float,
        val minLat: Float,
        val maxLon: Float,
        val maxLat: Float,
        val pts: FloatArray
    )

    class Place(
        val lon: Float,
        val lat: Float,
        val rank: Int,
        val landmark: Boolean,
        val name: String
    )

    class Road(
        val rank: Int,
        val minLon: Float,
        val minLat: Float,
        val maxLon: Float,
        val maxLat: Float,
        val pts: FloatArray
    )

    @Volatile
    var places: Array<Place> = emptyArray()
        private set

    @Volatile
    var roads: Array<Road> = emptyArray()
        private set

    @Volatile
    var ready: Boolean = false
        private set

    @Volatile
    private var loading = false

    private const val CELL = 2
    private var ringGrid: HashMap<Long, IntArray> = HashMap()
    private var roadGrid: HashMap<Long, IntArray> = HashMap()
    private var smallRings: Array<Ring> = emptyArray()
    private var padTiles: HashMap<Long, Array<Ring>> = HashMap()
    private var worldLod: Array<Ring> = emptyArray()
    private var bigRoads: IntArray = IntArray(0)

    fun ensure(ctx: Context, onReady: (() -> Unit)? = null) {
        if (ready) return
        val app = ctx.applicationContext
        synchronized(this) {
            if (ready) return
            if (loading) return
            loading = true
        }
        Thread({
            val t0 = System.nanoTime()
            try {
                PadBook.ensure(app)
                val land = loadLand(app)
                val pl = loadPlaces(app)
                val rd = loadRoads(app)
                val small = ArrayList<Ring>(land.size)
                val giant = ArrayList<Ring>()
                for (ring in land) {
                    val sl = ring.maxLon - ring.minLon
                    val sa = ring.maxLat - ring.minLat
                    if (sl >= 8f || sa >= 8f) giant.add(ring) else small.add(ring)
                }
                val rg = HashMap<Long, ArrayList<Int>>()
                small.forEachIndexed { i, ring ->
                    indexBBox(ring.minLon, ring.minLat, ring.maxLon, ring.maxLat, i, rg, null)
                }
                val rdg = HashMap<Long, ArrayList<Int>>()
                val bigRd = ArrayList<Int>()
                rd.forEachIndexed { i, road ->
                    indexBBox(road.minLon, road.minLat, road.maxLon, road.maxLat, i, rdg, bigRd)
                }
                smallRings = small.toTypedArray()
                places = pl
                roads = rd
                ringGrid = freeze(rg)
                roadGrid = freeze(rdg)
                bigRoads = bigRd.toIntArray()
                worldLod = giant.map { decimate(it, 160) }.toTypedArray()
                padTiles = buildPadTiles(giant)
                ready = true
                Log.i(
                    "CCOS.TRAJ",
                    "geo ready ${(System.nanoTime() - t0) / 1_000_000}ms small=${small.size} tiles=${padTiles.size} pads=${PadBook.sites.size}"
                )
            } catch (e: Exception) {
                Log.e("CCOS.TRAJ", "geo load failed", e)
                smallRings = emptyArray()
                places = emptyArray()
                roads = emptyArray()
                padTiles = HashMap()
                worldLod = emptyArray()
                ready = true
            }
            Handler(Looper.getMainLooper()).post { onReady?.invoke() }
        }, "ccos-geo").start()
    }

    fun overlaps(
        minLon: Float,
        minLat: Float,
        maxLon: Float,
        maxLat: Float,
        west: Float,
        south: Float,
        east: Float,
        north: Float
    ): Boolean {
        return maxLon >= west && minLon <= east && maxLat >= south && minLat <= north
    }

    fun forRings(west: Float, south: Float, east: Float, north: Float, block: (Ring) -> Unit) {
        if (!ready) return
        if (east - west > 24f || north - south > 18f) {
            for (r in worldLod) {
                if (overlaps(r.minLon, r.minLat, r.maxLon, r.maxLat, west, south, east, north)) block(r)
            }
            val seen = HashSet<Int>()
            visit(ringGrid, west, south, east, north) { i ->
                if (seen.add(i)) {
                    val r = smallRings[i]
                    if (overlaps(r.minLon, r.minLat, r.maxLon, r.maxLat, west, south, east, north)) block(r)
                }
            }
            return
        }
        val seen = HashSet<Int>()
        visit(ringGrid, west, south, east, north) { i ->
            if (seen.add(i)) {
                val r = smallRings[i]
                if (overlaps(r.minLon, r.minLat, r.maxLon, r.maxLat, west, south, east, north)) block(r)
            }
        }
        var x = floorCell(west)
        val x1 = floorCell(east)
        val y0 = floorCell(south)
        val y1 = floorCell(north)
        while (x <= x1) {
            var y = y0
            while (y <= y1) {
                val tile = padTiles[pack(x, y)]
                if (tile != null) {
                    for (r in tile) {
                        if (overlaps(r.minLon, r.minLat, r.maxLon, r.maxLat, west, south, east, north)) {
                            block(r)
                        }
                    }
                }
                y++
            }
            x++
        }
    }

    fun forRoads(west: Float, south: Float, east: Float, north: Float, maxRank: Int, block: (Road) -> Unit) {
        if (!ready) return
        val seen = HashSet<Int>()
        visit(roadGrid, west, south, east, north) { i ->
            if (seen.add(i)) {
                val r = roads[i]
                if (r.rank <= maxRank &&
                    overlaps(r.minLon, r.minLat, r.maxLon, r.maxLat, west, south, east, north)
                ) {
                    block(r)
                }
            }
        }
        for (i in bigRoads) {
            if (!seen.add(i)) continue
            val r = roads[i]
            if (r.rank <= maxRank &&
                overlaps(r.minLon, r.minLat, r.maxLon, r.maxLat, west, south, east, north)
            ) {
                block(r)
            }
        }
    }

    private fun buildPadTiles(giant: List<Ring>): HashMap<Long, Array<Ring>> {
        val cells = LinkedHashSet<Long>()
        for (s in PadBook.sites) {
            val cx = floorCell(s.lon)
            val cy = floorCell(s.lat)
            for (dx in -1..1) {
                for (dy in -1..1) cells.add(pack(cx + dx, cy + dy))
            }
        }
        val acc = HashMap<Long, ArrayList<Ring>>(cells.size)
        val bufA = FloatArray(160000)
        val bufB = FloatArray(160000)
        for (key in cells) {
            val x = (key shr 32).toInt()
            val y = key.toInt()
            val west = x * CELL.toFloat() - 0.12f
            val east = (x + 1) * CELL.toFloat() + 0.12f
            val south = y * CELL.toFloat() - 0.12f
            val north = (y + 1) * CELL.toFloat() + 0.12f
            val out = ArrayList<Ring>(4)
            for (ring in giant) {
                if (!overlaps(ring.minLon, ring.minLat, ring.maxLon, ring.maxLat, west, south, east, north)) continue
                val clipped = clipRing(ring.pts, west, east, south, north, bufA, bufB) ?: continue
                out.add(makeRing(ring.hole, clipped))
            }
            if (out.isNotEmpty()) acc[key] = out
        }
        val frozen = HashMap<Long, Array<Ring>>(acc.size)
        for ((k, v) in acc) frozen[k] = v.toTypedArray()
        return frozen
    }

    private fun makeRing(hole: Boolean, pts: FloatArray): Ring {
        var mnlo = Float.POSITIVE_INFINITY
        var mnla = Float.POSITIVE_INFINITY
        var mxlo = Float.NEGATIVE_INFINITY
        var mxla = Float.NEGATIVE_INFINITY
        var i = 0
        while (i + 1 < pts.size) {
            val lo = pts[i]
            val la = pts[i + 1]
            if (lo < mnlo) mnlo = lo
            if (la < mnla) mnla = la
            if (lo > mxlo) mxlo = lo
            if (la > mxla) mxla = la
            i += 2
        }
        return Ring(hole, mnlo, mnla, mxlo, mxla, pts)
    }

    private fun decimate(ring: Ring, maxPts: Int): Ring {
        val n = ring.pts.size / 2
        if (n <= maxPts) return ring
        val step = (n / maxPts).coerceAtLeast(2)
        val out = FloatArray(((n / step) + 2) * 2)
        var w = 0
        var i = 0
        while (i < n) {
            out[w * 2] = ring.pts[i * 2]
            out[w * 2 + 1] = ring.pts[i * 2 + 1]
            w++
            i += step
        }
        out[w * 2] = ring.pts[0]
        out[w * 2 + 1] = ring.pts[1]
        w++
        return makeRing(ring.hole, out.copyOf(w * 2))
    }

    private fun unwrap(lon: Float, ref: Float): Float {
        var d = lon - ref
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return ref + d
    }

    private fun clipRing(
        pts: FloatArray,
        west: Float,
        east: Float,
        south: Float,
        north: Float,
        a: FloatArray,
        b: FloatArray
    ): FloatArray? {
        val ref = (west + east) * 0.5f
        val cap = min(pts.size / 2, a.size / 2 - 4)
        var n = 0
        var i = 0
        val total = pts.size / 2
        while (i < total && n < cap) {
            a[n * 2] = unwrap(pts[i * 2], ref)
            a[n * 2 + 1] = pts[i * 2 + 1]
            n++
            i++
        }
        if (n < 3) return null
        n = clipPlane(a, n, b, 0, west, true)
        n = clipPlane(b, n, a, 0, east, false)
        n = clipPlane(a, n, b, 1, south, true)
        n = clipPlane(b, n, a, 1, north, false)
        if (n < 3) return null
        return a.copyOf(n * 2)
    }

    private fun clipPlane(src: FloatArray, n: Int, dst: FloatArray, axis: Int, edge: Float, keepGte: Boolean): Int {
        if (n < 3) return 0
        fun inside(i: Int): Boolean {
            val v = src[i * 2 + axis]
            return if (keepGte) v >= edge else v <= edge
        }
        var w = 0
        val cap = dst.size / 2 - 2
        var prev = n - 1
        var i = 0
        while (i < n && w < cap) {
            val cin = inside(i)
            val pin = inside(prev)
            if (cin) {
                if (!pin) {
                    hit(src, prev, i, axis, edge, dst, w)
                    w++
                    if (w >= cap) break
                }
                dst[w * 2] = src[i * 2]
                dst[w * 2 + 1] = src[i * 2 + 1]
                w++
            } else if (pin) {
                hit(src, prev, i, axis, edge, dst, w)
                w++
            }
            prev = i
            i++
        }
        return w
    }

    private fun hit(src: FloatArray, a: Int, b: Int, axis: Int, edge: Float, dst: FloatArray, w: Int) {
        val ax = src[a * 2]
        val ay = src[a * 2 + 1]
        val bx = src[b * 2]
        val by = src[b * 2 + 1]
        val av = if (axis == 0) ax else ay
        val bv = if (axis == 0) bx else by
        val d = bv - av
        val t = if (abs(d) < 1e-6f) 0f else ((edge - av) / d).coerceIn(0f, 1f)
        dst[w * 2] = ax + (bx - ax) * t
        dst[w * 2 + 1] = ay + (by - ay) * t
    }

    private fun visit(
        grid: HashMap<Long, IntArray>,
        west: Float,
        south: Float,
        east: Float,
        north: Float,
        block: (Int) -> Unit
    ) {
        var x = floorCell(west)
        val x1 = floorCell(east)
        val y0 = floorCell(south)
        val y1 = floorCell(north)
        while (x <= x1) {
            var y = y0
            while (y <= y1) {
                val hit = grid[pack(x, y)]
                if (hit != null) {
                    for (i in hit) block(i)
                }
                y++
            }
            x++
        }
    }

    private fun indexBBox(
        minLon: Float,
        minLat: Float,
        maxLon: Float,
        maxLat: Float,
        i: Int,
        grid: HashMap<Long, ArrayList<Int>>,
        big: ArrayList<Int>?
    ) {
        val x0 = floorCell(minLon)
        val x1 = floorCell(maxLon)
        val y0 = floorCell(minLat)
        val y1 = floorCell(maxLat)
        if (x1 - x0 > 8 || y1 - y0 > 8) {
            big?.add(i)
            return
        }
        var x = x0
        while (x <= x1) {
            var y = y0
            while (y <= y1) {
                grid.getOrPut(pack(x, y)) { ArrayList() }.add(i)
                y++
            }
            x++
        }
    }

    private fun floorCell(v: Float): Int = kotlin.math.floor((v / CELL).toDouble()).toInt()

    private fun pack(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)

    private fun freeze(src: HashMap<Long, ArrayList<Int>>): HashMap<Long, IntArray> {
        val out = HashMap<Long, IntArray>(src.size)
        for ((k, v) in src) out[k] = v.toIntArray()
        return out
    }

    private fun bytes(ctx: Context, name: String): ByteBuffer {
        ctx.assets.open(name).use { ins ->
            val buf = ins.readBytes()
            return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        }
    }

    private fun loadLand(ctx: Context): Array<Ring> {
        val b = bytes(ctx, "ne_land.bin")
        val mag = ByteArray(4)
        b.get(mag)
        if (String(mag, Charsets.US_ASCII) != "NEL1") return emptyArray()
        val n = b.int
        return Array(n) {
            val hole = b.get().toInt() != 0
            val np = b.int
            val mnlo = b.float
            val mnla = b.float
            val mxlo = b.float
            val mxla = b.float
            val pts = FloatArray(np * 2)
            var i = 0
            while (i < pts.size) {
                pts[i] = b.float
                pts[i + 1] = b.float
                i += 2
            }
            Ring(hole, mnlo, mnla, mxlo, mxla, pts)
        }
    }

    private fun loadPlaces(ctx: Context): Array<Place> {
        val b = bytes(ctx, "ne_places.bin")
        val mag = ByteArray(4)
        b.get(mag)
        if (String(mag, Charsets.US_ASCII) != "NEP1") return emptyArray()
        val n = b.int
        return Array(n) {
            val lon = b.float
            val lat = b.float
            val rank = b.get().toInt() and 0xFF
            val landmark = b.get().toInt() != 0
            val nlen = b.short.toInt() and 0xFFFF
            val raw = ByteArray(nlen)
            b.get(raw)
            Place(lon, lat, rank, landmark, String(raw, Charsets.UTF_8))
        }
    }

    private fun loadRoads(ctx: Context): Array<Road> {
        val b = bytes(ctx, "ne_roads.bin")
        val mag = ByteArray(4)
        b.get(mag)
        if (String(mag, Charsets.US_ASCII) != "NER1") return emptyArray()
        val n = b.int
        return Array(n) {
            val rank = b.get().toInt() and 0xFF
            val np = b.int
            val mnlo = b.float
            val mnla = b.float
            val mxlo = b.float
            val mxla = b.float
            val pts = FloatArray(np * 2)
            var i = 0
            while (i < pts.size) {
                pts[i] = b.float
                pts[i + 1] = b.float
                i += 2
            }
            Road(rank, mnlo, mnla, mxlo, mxla, pts)
        }
    }
}
