package com.ccos.retro.pro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * vc160 gold unlock sparkles. Da Vinci LRT Pro Gold v2 ("The more sparkles the better!", Chris 11:42 CT) on top
 * of the v1 FAIRY_DUST port. ALL sparkle art is driven by one tunable [ProSparkleParams] (swap [ProSparkleParams.current]
 * and nothing else changes). Pure JVM (no android.*), so counts, timings and flash rates are unit tested.
 * Units: dp, ms and 16 ms frames. Coordinates are dp offsets from the padlock centre.
 *
 * Hard rules (v2 decision a): the moment is capped at [ProGoldMoment.DURATION_MS]; every lingering twinkle starts and
 * ends inside it. The ambient idle glint ([ProIdleTwinkle]) is separate, once per opened surface, < 3 Hz, never loops,
 * off with animations removed / low RAM / gates off.
 */
data class SparkleLayer(
    val name: String,
    val count: Int,
    val sizeMinDp: Float,
    val sizeMaxDp: Float,
    val alpha: Float,
    val speedFactor: Float,
    /** Four-point astroid star above this size, else a soft dot. */
    val starAboveDp: Float,
    val buoyancyDp: Float = 0f,
    val colors: List<Int>,
    val blurred: Boolean = false,
    val glow: Boolean = false,
    val subStarAboveDp: Float = Float.MAX_VALUE,
    val flareAboveDp: Float = Float.MAX_VALUE,
    val whiteCoreAboveDp: Float = Float.MAX_VALUE
)

/** Pre-computed keep-out box (dp from the lock centre). */
data class KeepOut(val l: Float, val t: Float, val r: Float, val b: Float) {
    fun contains(x: Float, y: Float, pad: Float = 0f) = x >= l - pad && x <= r + pad && y >= t - pad && y <= b + pad
}

data class ProSparkleParams(
    // Burst (FAIRY_DUST port, v1 physics).
    val burstStartMs: Long = 130L,
    val spawnFrames: Int = 12,
    val earlyFrames: Int = 10,
    val startRadiusMinDp: Float = 20f,
    val startRadiusMaxDp: Float = 34f,
    val speedMinDp: Float = 3.0f,
    val speedMaxDp: Float = 8.5f,
    val lateSpeedFactor: Float = 0.55f,
    val initialVyDp: Float = -0.3f,
    val drag: Float = 0.95f,
    val gravityDp: Float = 0.02f,
    /** Per-frame life decay; 1/decayMin frames + spawn window must end before 2200 ms. */
    val decayMin: Float = 0.009f,
    val decayMax: Float = 0.018f,
    val twinkleStep: Float = 0.15f, // rad/frame -> ~1.43 Hz at 60 fps
    val layers: List<SparkleLayer> = V2_LAYERS,
    val maxLive: Int = 760,
    val fieldFadeStartMs: Long = 1_800L,
    // Lingering twinkles ("residents"), all inside the 2.2 s cap.
    val residentCount: Int = 64,
    val residentStartMs: Long = 520L,
    val residentDurMinMs: Long = 600L,
    val residentDurMaxMs: Long = 900L,
    val residentSizeMinDp: Float = 1.6f,
    val residentSizeMaxDp: Float = 3.6f,
    // Lens-flare glints: single sin^2 pulses PEAKING at these times (start = peak - dur/2; last ends 2150 < 2200).
    val glintPeaksMs: List<Long> = listOf(450L, 830L, 1_200L, 1_600L, 2_000L),
    val glintDurMs: Long = 300L,
    val glintOffsetsDp: List<Pair<Float, Float>> = listOf(-74f to -38f, 82f to -18f, -46f to 58f, 64f to 50f, 6f to -86f),
    // Shock rings.
    val ringCount: Int = 2,
    val ringStartMs: Long = 130L,
    val ringEndMs: Long = 600L,
    val ringStaggerMs: Long = 110L,
    val ringMaxRadiusDp: Float = 150f,
    // Text keep-out: sparkles over the arriving text fade 560-600, hard keep-out from 600.
    val textFadeStartMs: Long = 560L,
    val textKeepOutMs: Long = 600L,
    val keepOutPadDp: Float = 6f,
    // Glow + rays.
    val rayCount: Int = 14,
    val rayHalfAngleDeg: Float = 6f,
    val rayColor: Int = 0xFFFFD27A.toInt(),
    val rayPeakAlpha: Int = 46,
    val staticRayAlpha: Int = 22,
    val rayRotateDegPerMs: Float = 0.06f,
    val glowRadiusDp: Float = 150f,
    // Static (remove animations / low RAM) frozen field.
    val staticCounts: Map<String, Int> = mapOf("dust" to 150, "back" to 80, "mid" to 70, "front" to 26),
    val staticGlints: Int = 3,
    // Ambient idle glint on gold elements (v2 section 2). Chris 12:08 CT: "I approve! Go for it!" -> ON.
    // Still off with animations removed, low RAM, gates off; once per open, < 3 Hz, touch skips.
    val idleTwinkleEnabled: Boolean = true,
    val idleGlintStartsMs: List<Long> = listOf(250L, 630L, 1_010L, 1_390L, 1_770L, 2_150L),
    val idleGlintDurMs: Long = 800L
) {
    fun plannedSpawnTotal(): Int = layers.sumOf { it.count }
    fun staticTotal(): Int = staticCounts.values.sum()
    /** Hard cap: the moment never runs past 2.2 s, whatever the params. */
    fun totalMs(): Long = ProGoldMoment.DURATION_MS
    fun glintAlpha(index: Int, t: Long): Float =
        glintPeaksMs.getOrNull(index)?.let { sin2Pulse(t, it - glintDurMs / 2, glintDurMs) } ?: 0f
    /** Shock ring i progress 0..1 inside its window, or -1 when not showing. */
    fun ringProgress(i: Int, t: Long): Float {
        val s = ringStartMs + i * ringStaggerMs
        val e = ringEndMs
        if (t < s || t >= e) return -1f
        return (t - s).toFloat() / (e - s).toFloat()
    }
    /** Whole burst field multiplier: 1 until [fieldFadeStartMs], linear to 0 at the cap. */
    fun fieldFade(t: Long): Float = when {
        t <= fieldFadeStartMs -> 1f
        t >= ProGoldMoment.DURATION_MS -> 0f
        else -> 1f - (t - fieldFadeStartMs).toFloat() / (ProGoldMoment.DURATION_MS - fieldFadeStartMs).toFloat()
    }

    companion object {
        private val PALE = 0xFFFFFACD.toInt()
        private val HI = 0xFFFFD27A.toInt()
        private val GOLD = 0xFFFFD700.toInt()
        private val DEEP = 0xFFB8860B.toInt()
        val V2_LAYERS = listOf(
            SparkleLayer("back", 130, 0.8f, 1.7f, 0.55f, 0.6f, Float.MAX_VALUE, colors = listOf(HI, GOLD, DEEP), blurred = true),
            SparkleLayer("dust", 380, 0.3f, 0.85f, 0.9f, 0.8f, Float.MAX_VALUE, buoyancyDp = -0.03f, colors = listOf(PALE, HI, GOLD)),
            SparkleLayer("mid", 180, 1.4f, 3.0f, 1f, 1f, 1.9f, colors = listOf(PALE, HI, GOLD), whiteCoreAboveDp = 2.6f),
            SparkleLayer("front", 64, 2.8f, 5.6f, 1f, 1.1f, 0f, colors = listOf(PALE, HI, GOLD), glow = true,
                subStarAboveDp = 4f, flareAboveDp = 5f, whiteCoreAboveDp = 2.8f)
        )
        val V2 = ProSparkleParams()
        @JvmStatic @Volatile var current: ProSparkleParams = V2
    }
}

/**
 * O9 (Da Vinci v2): the moment peaks at ~760 sprites; check frame time on the Razr. Cheap guard: after [strikes]
 * consecutive draws over [budgetNs], degrade one level (1 = halve the dust layer, the least visible; 2 = drop dust and
 * halve the back layer). Never steps back up mid-moment, so there is no flicker between levels.
 */
/**
 * O9 degrade guard (Da Vinci 12:14 CT): the FIRST frame over 16 ms halves the dust layer and
 * nothing else. Only if [strikes] more consecutive frames are still over budget does it cut
 * further (drop dust, halve back). Never steps back up within one moment.
 */
class ProFrameGuard(private val budgetNs: Long = 16_000_000L, private val strikes: Int = 3) {
    var level = 0
        private set
    private var over = 0
    fun record(drawNs: Long): Int {
        if (drawNs > budgetNs) {
            over++
            val needed = if (level == 0) 1 else strikes
            if (over >= needed && level < MAX_LEVEL) { level++; over = 0 }
        } else over = 0
        return level
    }
    /** Whether the [index]-th particle of [layer] is drawn at the current level. */
    fun keep(layer: String, index: Int): Boolean = when {
        level == 0 -> true
        layer == "dust" -> level == 1 && index % 2 == 0
        layer == "back" && level >= 2 -> index % 2 == 0
        else -> true
    }
    companion object { const val MAX_LEVEL = 2 }
}

/** sin^2 pulse over [start, start+dur): 0 outside, peak 1 at the middle. */
fun sin2Pulse(t: Long, start: Long, dur: Long): Float {
    if (t < start || t >= start + dur || dur <= 0) return 0f
    val s = sin(PI * (t - start).toDouble() / dur.toDouble()).toFloat()
    return s * s
}

/** Ambient, once-per-open glint on gold elements (CTA, padlocks, owned row). */
object ProIdleTwinkle {
    fun allowed(params: ProSparkleParams, animatorsEnabled: Boolean, lowRam: Boolean = false, gatesOn: Boolean = true): Boolean =
        params.idleTwinkleEnabled && animatorsEnabled && !lowRam && gatesOn
    fun totalMs(params: ProSparkleParams): Long = (params.idleGlintStartsMs.maxOrNull() ?: 0L) + params.idleGlintDurMs
    fun alpha(params: ProSparkleParams, index: Int, t: Long): Float =
        params.idleGlintStartsMs.getOrNull(index)?.let { sin2Pulse(t, it, params.idleGlintDurMs) } ?: 0f
    /** New glint peaks per second across the element (must stay < 3). */
    fun peakRateHz(params: ProSparkleParams): Double {
        val s = params.idleGlintStartsMs.sorted()
        val minGap = s.zipWithNext { a, b -> b - a }.minOrNull() ?: return 0.0
        return 1000.0 / minGap
    }
}

class ProSparkle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val size: Float, val decay: Float, val color: Int, var phase: Float, val layer: Int
) {
    var life = 1f
    fun twinkle(): Float = 0.4f + 0.6f * ((sin(phase) + 1f) / 2f)
    fun alpha(layerAlpha: Float = 1f): Int = (twinkle() * life.coerceIn(0f, 1f) * layerAlpha * 255f).toInt().coerceIn(0, 255)
}

data class Resident(val x: Float, val y: Float, val size: Float, val color: Int, val t0: Long, val dur: Long) {
    fun alpha(t: Long): Float = sin2Pulse(t, t0, dur)
}

data class StaticSparkle(val x: Float, val y: Float, val size: Float, val layer: Int, val color: Int, val alpha: Float)

class ProSparkleSim(private val p: ProSparkleParams = ProSparkleParams.current, private val rng: Random = Random.Default) {
    val live = ArrayList<ProSparkle>(p.maxLive)
    var frame = 0
        private set
    var spawned = 0
        private set
    val spawnedByLayer = IntArray(p.layers.size)

    private fun r(a: Float, b: Float) = a + rng.nextFloat() * (b - a)

    /** Even split of each layer's count over [ProSparkleParams.spawnFrames]; remainder goes to the first frames. */
    fun spawnCount(layer: Int, f: Int): Int {
        if (f >= p.spawnFrames) return 0
        val c = p.layers[layer].count
        return c / p.spawnFrames + if (f < c % p.spawnFrames) 1 else 0
    }

    fun step() {
        for ((li, L) in p.layers.withIndex()) {
            repeat(spawnCount(li, frame)) {
                if (live.size >= p.maxLive) return@repeat
                val a = r(0f, (PI * 2).toFloat())
                val rad = r(p.startRadiusMinDp, p.startRadiusMaxDp)
                val sp = r(p.speedMinDp, p.speedMaxDp) * L.speedFactor * (if (frame >= p.earlyFrames) p.lateSpeedFactor else 1f)
                live.add(ProSparkle(
                    x = cos(a) * rad, y = sin(a) * rad, vx = cos(a) * sp, vy = sin(a) * sp + p.initialVyDp,
                    size = r(L.sizeMinDp, L.sizeMaxDp), decay = r(p.decayMin, p.decayMax),
                    color = L.colors[rng.nextInt(L.colors.size)], phase = r(0f, 6.28f), layer = li
                ))
                spawned++; spawnedByLayer[li]++
            }
        }
        val it = live.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.x += s.vx; s.y += s.vy
            s.vx *= p.drag; s.vy = s.vy * p.drag + p.gravityDp + p.layers[s.layer].buoyancyDp
            s.life -= s.decay
            s.phase += p.twinkleStep
            if (s.life <= 0f) it.remove()
        }
        frame++
    }

    companion object {
        /** Lingering twinkles at fixed positions over the plate, outside the text boxes, all done by 2200 ms. */
        fun residents(p: ProSparkleParams, halfWdp: Float, halfHdp: Float, keepOuts: List<KeepOut>, rng: Random = Random(160)): List<Resident> {
            val out = ArrayList<Resident>(p.residentCount)
            var guard = 0
            while (out.size < p.residentCount && guard++ < p.residentCount * 50) {
                val x = (rng.nextFloat() * 2f - 1f) * halfWdp
                val y = (rng.nextFloat() * 2f - 1f) * halfHdp
                if (keepOuts.any { it.contains(x, y, p.keepOutPadDp) }) continue
                val dur = p.residentDurMinMs + (rng.nextFloat() * (p.residentDurMaxMs - p.residentDurMinMs)).toLong()
                val latest = ProGoldMoment.DURATION_MS - dur
                val t0 = p.residentStartMs + (rng.nextFloat() * (latest - p.residentStartMs).coerceAtLeast(0L)).toLong()
                val L = p.layers.first { it.name == "mid" }
                out.add(Resident(x, y, p.residentSizeMinDp + rng.nextFloat() * (p.residentSizeMaxDp - p.residentSizeMinDp),
                    L.colors[rng.nextInt(L.colors.size)], t0, dur))
            }
            return out
        }

        /** Reduced-motion frozen field (v2 section 1): deterministic, outside the text boxes. */
        fun staticField(p: ProSparkleParams, halfWdp: Float, halfHdp: Float, keepOuts: List<KeepOut>, rng: Random = Random(1600)): List<StaticSparkle> {
            val out = ArrayList<StaticSparkle>(p.staticTotal())
            for ((li, L) in p.layers.withIndex()) {
                val n = p.staticCounts[L.name] ?: 0
                var made = 0; var guard = 0
                while (made < n && guard++ < n * 50 + 50) {
                    // Denser near the lock: radius biased toward the centre.
                    val a = rng.nextFloat() * 2f * PI.toFloat()
                    val rr = (rng.nextFloat() * rng.nextFloat() * 0.85f + 0.15f)
                    val x = cos(a) * rr * halfWdp
                    val y = sin(a) * rr * halfHdp
                    if (keepOuts.any { it.contains(x, y, p.keepOutPadDp) }) continue
                    out.add(StaticSparkle(x, y, L.sizeMinDp + rng.nextFloat() * (L.sizeMaxDp - L.sizeMinDp), li,
                        L.colors[rng.nextInt(L.colors.size)], L.alpha * (0.55f + 0.45f * rng.nextFloat())))
                    made++
                }
            }
            return out
        }
    }
}
