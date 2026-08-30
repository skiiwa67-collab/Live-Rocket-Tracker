package com.ccos.retro.geo

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Shared TRAJ land fill. Same Natural Earth book on MCC and wallpaper.
 * Clip in lon/lat, never Path.op a continent, never hatch the planet every frame.
 */
object GeoDraw {
    private class Buf {
        val a = FloatArray(8192)
        val b = FloatArray(8192)
        val path = Path()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val tl = ThreadLocal.withInitial { Buf() }

    fun wrapLon(lon: Float): Float {
        var x = lon
        while (x > 180f) x -= 360f
        while (x < -180f) x += 360f
        return x
    }

    fun unwrapLon(lon: Float, ref: Float): Float {
        var d = lon - ref
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return ref + d
    }

    fun destFrom(lon: Float, lat: Float, bearingDeg: Float, distKm: Float): Pair<Float, Float> {
        val r = 6371.0
        val d = distKm.toDouble() / r
        val br = Math.toRadians(bearingDeg.toDouble())
        val lat1 = Math.toRadians(lat.toDouble())
        val lon1 = Math.toRadians(lon.toDouble())
        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(br))
        val lon2 = lon1 + atan2(sin(br) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))
        return wrapLon(Math.toDegrees(lon2).toFloat()) to Math.toDegrees(lat2).toFloat()
    }

    fun haversineKm(lon1: Float, lat1: Float, lon2: Float, lat2: Float): Float {
        val r = 6371.0
        val p1 = Math.toRadians(lat1.toDouble())
        val p2 = Math.toRadians(lat2.toDouble())
        val dphi = Math.toRadians((lat2 - lat1).toDouble())
        val dl = Math.toRadians((lon2 - lon1).toDouble())
        val a = sin(dphi / 2) * sin(dphi / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return (2.0 * r * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))).toFloat()
    }

    fun mapXY(lon: Float, lat: Float, dest: RectF, cLon: Float, cLat: Float, halfLon: Float, halfLat: Float): Pair<Float, Float> {
        val lonU = unwrapLon(lon, cLon)
        val x = dest.left + ((lonU - (cLon - halfLon)) / (halfLon * 2f)) * dest.width()
        val y = dest.top + (((cLat + halfLat) - lat) / (halfLat * 2f)) * dest.height()
        return x to y
    }

    fun drawLand(
        canvas: Canvas,
        dest: RectF,
        cLon: Float,
        cLat: Float,
        halfLon: Float,
        halfLat: Float,
        landColor: Int,
        edgeColor: Int,
        holeColor: Int
    ) {
        if (!GeoAtlas.ready) return
        val buf = tl.get()
        val west = cLon - halfLon * 1.25f
        val east = cLon + halfLon * 1.25f
        val south = cLat - halfLat * 1.25f
        val north = cLat + halfLat * 1.25f
        val holes = ArrayList<FloatArray>(4)
        GeoAtlas.forRings(west, south, east, north) { ring ->
            if (ring.hole) {
                holes.add(ring.pts)
            } else {
                fillRing(canvas, dest, cLon, cLat, halfLon, halfLat, west, east, south, north, ring.pts, landColor, edgeColor, buf)
            }
        }
        for (pts in holes) {
            fillRing(canvas, dest, cLon, cLat, halfLon, halfLat, west, east, south, north, pts, holeColor, edgeColor, buf)
        }
    }

    private fun fillRing(
        canvas: Canvas,
        dest: RectF,
        cLon: Float,
        cLat: Float,
        halfLon: Float,
        halfLat: Float,
        west: Float,
        east: Float,
        south: Float,
        north: Float,
        pts: FloatArray,
        fill: Int,
        rim: Int,
        buf: Buf
    ) {
        if (pts.size > 12000) {
            // Giant leftover continent. Tile cache should have clipped this.
            return
        }
        val n0 = sample(pts, cLon, west, east, south, north, halfLon, buf.a)
        if (n0 < 3) return
        var n = clipRect(buf.a, n0, west, east, south, north, buf.b)
        if (n < 3) return
        buf.path.reset()
        var i = 0
        while (i < n) {
            val (x, y) = mapXY(buf.b[i * 2], buf.b[i * 2 + 1], dest, cLon, cLat, halfLon, halfLat)
            if (i == 0) buf.path.moveTo(x, y) else buf.path.lineTo(x, y)
            i++
        }
        buf.path.close()
        buf.fill.color = fill
        canvas.drawPath(buf.path, buf.fill)
        buf.stroke.color = rim
        buf.stroke.strokeWidth = 1.4f
        canvas.drawPath(buf.path, buf.stroke)
    }

    private fun sample(
        pts: FloatArray,
        cLon: Float,
        west: Float,
        east: Float,
        south: Float,
        north: Float,
        halfLon: Float,
        dest: FloatArray
    ): Int {
        val n = pts.size / 2
        if (n < 3) return 0
        val stride = ((n + 399) / 400).coerceAtLeast(1)
        val eps = (halfLon / 90f).coerceAtLeast(0.002f)
        var w = 0
        var lastLon = 1e9f
        var lastLat = 1e9f
        var i = 0
        val cap = dest.size / 2 - 2
        while (i < n && w < cap) {
            val lon = unwrapLon(pts[i * 2], cLon)
            val lat = pts[i * 2 + 1]
            val inside = lon >= west && lon <= east && lat >= south && lat <= north
            val keep = inside || i % stride == 0 || i == 0 || i == n - 1
            if (keep) {
                if (w == 0 || inside || abs(lon - lastLon) >= eps || abs(lat - lastLat) >= eps) {
                    dest[w * 2] = lon
                    dest[w * 2 + 1] = lat
                    w++
                    lastLon = lon
                    lastLat = lat
                }
            }
            i++
        }
        return w
    }

    private fun clipRect(
        src: FloatArray,
        n: Int,
        west: Float,
        east: Float,
        south: Float,
        north: Float,
        dst: FloatArray
    ): Int {
        val tmp = FloatArray(min(src.size, dst.size))
        var m = copy(src, n, tmp)
        m = clipPlane(tmp, m, dst, 0, west, +1f, 0f)
        m = clipPlane(dst, m, tmp, 0, east, -1f, 0f)
        m = clipPlane(tmp, m, dst, 1, south, 0f, +1f)
        m = clipPlane(dst, m, tmp, 1, north, 0f, -1f)
        copy(tmp, m, dst)
        return m
    }

    /** axis: 0=lon 1=lat. sign +1 means keep >= edge, -1 keep <= edge. */
    private fun clipPlane(src: FloatArray, n: Int, dst: FloatArray, axis: Int, edge: Float, sx: Float, sy: Float): Int {
        if (n < 3) return 0
        fun inside(i: Int): Boolean {
            val v = src[i * 2 + axis]
            return if (axis == 0) {
                if (sx > 0f) v >= edge else v <= edge
            } else {
                if (sy > 0f) v >= edge else v <= edge
            }
        }
        fun hit(a: Int, b: Int, outI: Int) {
            val ax = src[a * 2]
            val ay = src[a * 2 + 1]
            val bx = src[b * 2]
            val by = src[b * 2 + 1]
            val av = if (axis == 0) ax else ay
            val bv = if (axis == 0) bx else by
            val d = bv - av
            val t = if (abs(d) < 1e-6f) 0f else ((edge - av) / d).coerceIn(0f, 1f)
            dst[outI * 2] = ax + (bx - ax) * t
            dst[outI * 2 + 1] = ay + (by - ay) * t
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
                    hit(prev, i, w)
                    w++
                    if (w >= cap) break
                }
                dst[w * 2] = src[i * 2]
                dst[w * 2 + 1] = src[i * 2 + 1]
                w++
            } else if (pin) {
                hit(prev, i, w)
                w++
            }
            prev = i
            i++
        }
        return w
    }

    private fun copy(src: FloatArray, n: Int, dst: FloatArray): Int {
        val m = min(n, dst.size / 2)
        System.arraycopy(src, 0, dst, 0, m * 2)
        return m
    }
}
