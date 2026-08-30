package com.ccos.retro.engine

/**
 * Nano-assembler particle — streams toward build targets, crawls/orbits when complete.
 */
class NanoParticle {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var targetX = 0f
    var targetY = 0f
    var life = 1f
    var size = 1.5f
    var active = false
    var depositing = false
    var orbit = false
    var orbitAngle = 0f
    var orbitRadius = 20f
    var orbitCx = 0f
    var orbitCy = 0f

    fun reset(sx: Float, sy: Float, tx: Float, ty: Float, orbitMode: Boolean = false) {
        x = sx; y = sy
        targetX = tx; targetY = ty
        orbit = orbitMode
        orbitCx = tx; orbitCy = ty
        orbitAngle = (Math.random() * Math.PI * 2).toFloat()
        orbitRadius = 12f + (Math.random() * 36f).toFloat()
        val dx = tx - sx
        val dy = ty - sy
        val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val speed = if (orbitMode) 35f + (Math.random() * 25f).toFloat()
                    else 90f + (Math.random() * 70f).toFloat()
        vx = (dx / dist) * speed
        vy = (dy / dist) * speed
        life = 1f
        size = 1.0f + (Math.random() * 2.0f).toFloat()
        active = true
        depositing = false
    }

    fun update(dt: Float): Boolean {
        if (!active) return false
        if (orbit) {
            // Spiral in then orbit
            val dx = orbitCx - x
            val dy = orbitCy - y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist > orbitRadius + 8f) {
                x += vx * dt * 0.6f
                y += vy * dt * 0.6f
            } else {
                orbitAngle += dt * 1.8f
                x = orbitCx + kotlin.math.cos(orbitAngle) * orbitRadius
                y = orbitCy + kotlin.math.sin(orbitAngle) * orbitRadius * 0.55f
            }
            life -= dt * 0.22f
        } else {
            x += vx * dt
            y += vy * dt
            val dx = targetX - x
            val dy = targetY - y
            if (dx * dx + dy * dy < 80f) {
                depositing = true
                life -= dt * 2.2f
            } else {
                life -= dt * 0.35f
            }
        }
        if (life <= 0f) {
            active = false
            return false
        }
        return true
    }
}
