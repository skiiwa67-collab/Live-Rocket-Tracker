package com.ccos.retro.geo

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.event.VehicleCatalog

/**
 * Published pad silhouettes for the PAD plate / PAD page.
 * Starship at Starbase = chopsticks. Other sites get the tower that is actually there.
 * Unknown = trench + stick. Do not invent hardware.
 */
object PadGlyph {

    enum class Kind {
        CHOPSTICKS,
        FSS_39A,
        FALCON_WEST,
        SLS_ML,
        SOYUZ_TOWER,
        ARIANE_GANTRY,
        CASC_TOWER,
        SDSC_TOWER,
        ELECTRON,
        GLENN,
        UNKNOWN
    }

    fun kind(launch: LaunchSnapshot?): Kind {
        if (launch == null) return Kind.UNKNOWN
        val blob = "${launch.pad} ${launch.location} ${launch.rocketName} ${launch.name}".lowercase()
        val family = VehicleCatalog.family(launch)
        val starbase = "starbase" in blob || "boca" in blob || "cameron" in blob ||
            "olm" in blob || "orbital launch" in blob || "launch pad 2" in blob ||
            "pad 2" in blob || "pad 1" in blob
        val starship = family == "starship" || "starship" in blob || "super heavy" in blob
        return when {
            starship && starbase -> Kind.CHOPSTICKS
            starbase -> Kind.CHOPSTICKS
            "39a" in blob || "lc-39a" in blob || "lc 39a" in blob -> Kind.FSS_39A
            "39b" in blob || family == "sls" || "sls" in blob -> Kind.SLS_ML
            "vandenberg" in blob || "vafb" in blob || "slc-4" in blob || "slc 4" in blob ||
                "slc-40" in blob || "slc 40" in blob -> Kind.FALCON_WEST
            family == "soyuz" || "baikonur" in blob || "plesetsk" in blob || "vostochny" in blob ||
                "gagarin" in blob -> Kind.SOYUZ_TOWER
            family == "ariane" || "kourou" in blob || "guiana" in blob || "ela" in blob -> Kind.ARIANE_GANTRY
            family == "electron" || "mahia" in blob || "rocket lab" in blob -> Kind.ELECTRON
            family == "glenn" || "new glenn" in blob || "lc-36" in blob || "slc-36" in blob -> Kind.GLENN
            "sriharikota" in blob || "satish" in blob || "sdsc" in blob ||
                family == "lvm3" || family == "isro" -> Kind.SDSC_TOWER
            "tanegashima" in blob || family == "h3" -> Kind.CASC_TOWER
            launch.isChinese() || family == "lm" || family == "lm5" || family == "zq" ||
                "jiuquan" in blob || "wenchang" in blob || "taiyuan" in blob ||
                "xichang" in blob -> Kind.CASC_TOWER
            else -> Kind.UNKNOWN
        }
    }

    fun label(kind: Kind): String = when (kind) {
        Kind.CHOPSTICKS -> "MECHAZILLA"
        Kind.FSS_39A -> "FSS / CREW ARM"
        Kind.FALCON_WEST -> "TEL / UMBILICAL"
        Kind.SLS_ML -> "MOBILE LAUNCHER"
        Kind.SOYUZ_TOWER -> "SERVICE TOWERS"
        Kind.ARIANE_GANTRY -> "MOBILE GANTRY"
        Kind.CASC_TOWER -> "UMBILICAL TOWER"
        Kind.SDSC_TOWER -> "UMBILICAL TOWER"
        Kind.ELECTRON -> "LC-1"
        Kind.GLENN -> "GLENN TOWER"
        Kind.UNKNOWN -> "NEW PAD"
    }

    fun earthUri(lat: Float, lon: Float): android.net.Uri {
        return android.net.Uri.parse(
            java.lang.String.format(
                java.util.Locale.US,
                "https://earth.google.com/web/@%.6f,%.6f,80a,700d,35y,0h,45t,0r",
                lat, lon
            )
        )
    }

    fun draw(canvas: Canvas, box: RectF, color: Int, kind: Kind, stroke: Paint) {
        if (box.width() < 8f || box.height() < 8f) return
        val oldStyle = stroke.style
        val oldW = stroke.strokeWidth
        val oldColor = stroke.color
        val oldPath = stroke.strokeJoin
        stroke.style = Paint.Style.STROKE
        stroke.color = color
        stroke.strokeJoin = Paint.Join.ROUND
        stroke.strokeWidth = minOf(box.width(), box.height()) * 0.028f
        when (kind) {
            Kind.CHOPSTICKS -> chopsticks(canvas, box, stroke)
            Kind.FSS_39A -> fss39a(canvas, box, stroke)
            Kind.FALCON_WEST -> falconWest(canvas, box, stroke)
            Kind.SLS_ML -> slsMl(canvas, box, stroke)
            Kind.SOYUZ_TOWER -> soyuz(canvas, box, stroke)
            Kind.ARIANE_GANTRY -> ariane(canvas, box, stroke)
            Kind.CASC_TOWER, Kind.SDSC_TOWER -> casc(canvas, box, stroke)
            Kind.ELECTRON -> electron(canvas, box, stroke)
            Kind.GLENN -> glenn(canvas, box, stroke)
            Kind.UNKNOWN -> unknown(canvas, box, stroke)
        }
        stroke.style = oldStyle
        stroke.strokeWidth = oldW
        stroke.color = oldColor
        stroke.strokeJoin = oldPath
    }

    private fun chopsticks(canvas: Canvas, b: RectF, p: Paint) {
        val cx = b.centerX()
        val ground = b.bottom - b.height() * 0.06f
        val olmH = b.height() * 0.10f
        val olmW = b.width() * 0.42f
        canvas.drawRect(cx - olmW * 0.5f, ground - olmH, cx + olmW * 0.5f, ground, p)
        val towerW = b.width() * 0.07f
        val towerH = b.height() * 0.78f
        val gap = b.width() * 0.22f
        val left = cx - gap * 0.5f - towerW
        val right = cx + gap * 0.5f
        val top = ground - olmH - towerH
        canvas.drawRect(left, top, left + towerW, ground - olmH, p)
        canvas.drawRect(right, top, right + towerW, ground - olmH, p)
        val armY = top + towerH * 0.28f
        val armH = b.height() * 0.045f
        canvas.drawRect(left + towerW, armY, cx - b.width() * 0.015f, armY + armH, p)
        canvas.drawRect(cx + b.width() * 0.015f, armY, right, armY + armH, p)
        val qdX = right + towerW + b.width() * 0.04f
        canvas.drawLine(right + towerW, armY + armH * 2f, qdX, armY + armH * 2f, p)
        canvas.drawLine(qdX, armY + armH * 2f, qdX, ground - olmH, p)
        canvas.drawLine(b.left + b.width() * 0.08f, ground, b.right - b.width() * 0.08f, ground, p)
    }

    private fun fss39a(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.1f, ground, b.right - b.width() * 0.1f, ground, p)
        val tw = b.width() * 0.16f
        val th = b.height() * 0.72f
        val tx = cx + b.width() * 0.08f
        canvas.drawRect(tx, ground - th, tx + tw, ground, p)
        val armY = ground - th * 0.55f
        canvas.drawRect(cx - b.width() * 0.18f, armY, tx, armY + b.height() * 0.06f, p)
        canvas.drawLine(tx + tw * 0.5f, ground - th, tx + tw * 0.5f, ground - th - b.height() * 0.08f, p)
    }

    private fun falconWest(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.12f, ground, b.right - b.width() * 0.12f, ground, p)
        canvas.drawRect(cx - b.width() * 0.12f, ground - b.height() * 0.08f, cx + b.width() * 0.12f, ground, p)
        val tx = cx + b.width() * 0.18f
        canvas.drawRect(tx, ground - b.height() * 0.62f, tx + b.width() * 0.10f, ground, p)
        canvas.drawLine(tx, ground - b.height() * 0.38f, cx + b.width() * 0.04f, ground - b.height() * 0.38f, p)
    }

    private fun slsMl(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawRect(cx - b.width() * 0.28f, ground - b.height() * 0.10f, cx + b.width() * 0.28f, ground, p)
        canvas.drawRect(cx + b.width() * 0.04f, ground - b.height() * 0.78f, cx + b.width() * 0.22f, ground - b.height() * 0.10f, p)
        canvas.drawLine(cx + b.width() * 0.04f, ground - b.height() * 0.50f, cx - b.width() * 0.06f, ground - b.height() * 0.50f, p)
        canvas.drawLine(cx + b.width() * 0.13f, ground - b.height() * 0.78f, cx + b.width() * 0.13f, ground - b.height() * 0.86f, p)
    }

    private fun soyuz(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.08f, ground, b.right - b.width() * 0.08f, ground, p)
        val path = Path()
        fun wing(dir: Float) {
            val x = cx + dir * b.width() * 0.08f
            path.reset()
            path.moveTo(x, ground)
            path.lineTo(x + dir * b.width() * 0.22f, ground - b.height() * 0.18f)
            path.lineTo(x + dir * b.width() * 0.18f, ground - b.height() * 0.62f)
            path.lineTo(x + dir * b.width() * 0.04f, ground - b.height() * 0.55f)
            path.close()
            canvas.drawPath(path, p)
        }
        wing(-1f)
        wing(1f)
        canvas.drawRect(cx - b.width() * 0.05f, ground - b.height() * 0.12f, cx + b.width() * 0.05f, ground, p)
    }

    private fun ariane(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.1f, ground, b.right - b.width() * 0.1f, ground, p)
        canvas.drawRect(cx - b.width() * 0.20f, ground - b.height() * 0.70f, cx + b.width() * 0.20f, ground, p)
        canvas.drawLine(cx - b.width() * 0.20f, ground - b.height() * 0.35f, cx - b.width() * 0.32f, ground - b.height() * 0.35f, p)
        canvas.drawLine(cx - b.width() * 0.20f, ground - b.height() * 0.55f, cx - b.width() * 0.32f, ground - b.height() * 0.55f, p)
    }

    private fun casc(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.1f, ground, b.right - b.width() * 0.1f, ground, p)
        canvas.drawRect(cx + b.width() * 0.06f, ground - b.height() * 0.72f, cx + b.width() * 0.20f, ground, p)
        canvas.drawLine(cx + b.width() * 0.06f, ground - b.height() * 0.42f, cx - b.width() * 0.02f, ground - b.height() * 0.42f, p)
        canvas.drawLine(cx - b.width() * 0.28f, ground, cx - b.width() * 0.28f, ground - b.height() * 0.80f, p)
        canvas.drawLine(cx + b.width() * 0.34f, ground, cx + b.width() * 0.34f, ground - b.height() * 0.80f, p)
    }

    private fun electron(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.10f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.18f, ground, b.right - b.width() * 0.18f, ground, p)
        canvas.drawRect(cx - b.width() * 0.10f, ground - b.height() * 0.08f, cx + b.width() * 0.10f, ground, p)
        canvas.drawRect(cx + b.width() * 0.12f, ground - b.height() * 0.42f, cx + b.width() * 0.20f, ground, p)
    }

    private fun glenn(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.08f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.1f, ground, b.right - b.width() * 0.1f, ground, p)
        canvas.drawRect(cx - b.width() * 0.16f, ground - b.height() * 0.10f, cx + b.width() * 0.16f, ground, p)
        canvas.drawRect(cx + b.width() * 0.10f, ground - b.height() * 0.70f, cx + b.width() * 0.24f, ground, p)
        canvas.drawLine(cx + b.width() * 0.10f, ground - b.height() * 0.40f, cx, ground - b.height() * 0.40f, p)
    }

    private fun unknown(canvas: Canvas, b: RectF, p: Paint) {
        val ground = b.bottom - b.height() * 0.10f
        val cx = b.centerX()
        canvas.drawLine(b.left + b.width() * 0.12f, ground, b.right - b.width() * 0.12f, ground, p)
        canvas.drawRect(cx - b.width() * 0.22f, ground - b.height() * 0.08f, cx + b.width() * 0.22f, ground, p)
        canvas.drawLine(cx + b.width() * 0.18f, ground, cx + b.width() * 0.18f, ground - b.height() * 0.55f, p)
        canvas.drawLine(cx + b.width() * 0.18f, ground - b.height() * 0.55f, cx + b.width() * 0.28f, ground - b.height() * 0.55f, p)
    }
}
