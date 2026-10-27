package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** vc160 Da Vinci LRT Pro Gold v2: dense four-layer burst, all inside the 2.2 s cap, < 3 Hz, never over text. */
class ProSparkleTest {
    private val p = ProSparkleParams.V2

    @Test fun v2LayerCountsMatchTheSpec() {
        assertEquals(listOf("back" to 130, "dust" to 380, "mid" to 180, "front" to 64), p.layers.map { it.name to it.count })
        assertEquals(754, p.plannedSpawnTotal())
        assertTrue("live cap ~760", p.maxLive in 754..800)
        assertEquals(64, p.residentCount)
        assertEquals(2, p.ringCount)
        assertEquals(14, p.rayCount)
        assertEquals(0xFFFFD27A.toInt(), p.rayColor)
        assertEquals(22, p.staticRayAlpha)
        assertEquals(0.15f, p.twinkleStep, 0f)
        assertSame(p, ProSparkleParams.current)
    }

    private fun assertSame(a: Any, b: Any) = assertTrue(a === b || a == b)

    @Test fun momentIsHardCappedAt2200() {
        assertEquals(ProGoldMoment.DURATION_MS, p.totalMs())
        assertEquals(2_200L, p.totalMs())
        // Even a params change cannot push the moment past 2.2 s.
        assertEquals(2_200L, p.copy(residentDurMaxMs = 5_000).totalMs())
    }

    @Test fun lingeringTwinklesAllFinishBy2200() {
        val res = ProSparkleSim.residents(p, 150f, 160f, emptyList())
        assertEquals(64, res.size)
        for (r in res) {
            assertTrue(r.t0 >= p.residentStartMs)
            assertTrue("dur ${r.dur}", r.dur in p.residentDurMinMs..p.residentDurMaxMs)
            assertTrue("ends ${r.t0 + r.dur}", r.t0 + r.dur <= 2_200L)
            assertEquals(0f, r.alpha(2_200L), 0f)
        }
    }

    @Test fun residentsAndStaticFieldAvoidTextBoxes() {
        val title = KeepOut(-120f, 60f, 120f, 92f)
        val body = KeepOut(-120f, 96f, 120f, 120f)
        val ko = listOf(title, body)
        for (r in ProSparkleSim.residents(p, 150f, 160f, ko)) assertFalse(ko.any { it.contains(r.x, r.y, p.keepOutPadDp) })
        for (s in ProSparkleSim.staticField(p, 150f, 160f, ko)) assertFalse(ko.any { it.contains(s.x, s.y, p.keepOutPadDp) })
        assertTrue(p.textFadeStartMs == 560L && p.textKeepOutMs == 600L)
    }

    @Test fun glintsPeakAtSpecTimesAndEndInsideTheCap() {
        assertEquals(listOf(450L, 830L, 1_200L, 1_600L, 2_000L), p.glintPeaksMs)
        assertEquals(300L, p.glintDurMs)
        val gaps = p.glintPeaksMs.zipWithNext { a, b -> b - a }
        assertTrue("glints >= 370 ms apart", gaps.all { it >= 370 })
        for (i in p.glintPeaksMs.indices) {
            assertEquals(1f, p.glintAlpha(i, p.glintPeaksMs[i]), 1e-4f)
            assertTrue(p.glintPeaksMs[i] + p.glintDurMs / 2 <= 2_200L)
        }
        assertEquals(0f, p.glintAlpha(4, 2_200L), 0f)
    }

    @Test fun shockRingsLiveOnlyIn130To600() {
        assertTrue(p.ringProgress(0, 129) < 0f)
        assertTrue(p.ringProgress(0, 130) >= 0f)
        assertTrue(p.ringProgress(1, 300) >= 0f)
        assertTrue(p.ringProgress(0, 600) < 0f)
        assertTrue(p.ringProgress(1, 600) < 0f)
    }

    @Test fun burstFieldFadesOut1800To2200() {
        assertEquals(1f, p.fieldFade(1_800), 0f)
        assertEquals(0.5f, p.fieldFade(2_000), 1e-4f)
        assertEquals(0f, p.fieldFade(2_200), 0f)
    }

    @Test fun simSpawnsEveryLayerAndRespectsTheCap() {
        val sim = ProSparkleSim(p, Random(7))
        var peak = 0
        repeat(130) { sim.step(); peak = maxOf(peak, sim.live.size) }
        assertEquals(754, sim.spawned)
        assertEquals(listOf(130, 380, 180, 64), sim.spawnedByLayer.toList())
        assertTrue(peak <= p.maxLive)
        // Spawn window + slowest decay ends before 2200 ms (burst starts at 130).
        val lastSpawnFrame = p.spawnFrames - 1
        assertTrue(p.burstStartMs + (lastSpawnFrame + 1f / p.decayMin) * 16 <= 2_200)
        assertTrue("all dead by 130 frames", sim.live.isEmpty())
    }

    @Test fun denseParamsAreStillCapped() {
        val dense = p.copy(maxLive = 300)
        val sim = ProSparkleSim(dense, Random(3))
        repeat(40) { sim.step(); assertTrue(sim.live.size <= 300) }
    }

    @Test fun staticFieldIsRichAndDeterministic() {
        val a = ProSparkleSim.staticField(p, 150f, 160f, emptyList())
        val b = ProSparkleSim.staticField(p, 150f, 160f, emptyList())
        assertEquals(a, b)
        assertEquals(p.staticTotal(), a.size)
        assertEquals(mapOf("dust" to 150, "back" to 80, "mid" to 70, "front" to 26), p.staticCounts)
        assertEquals(3, p.staticGlints)
    }

    @Test fun sparkleTwinkleAndGlintsStayUnder3Hz() {
        val hz = p.twinkleStep * 60.0 / (2 * Math.PI)
        assertTrue("twinkle $hz Hz", hz < 3.0 && hz > 1.2)
        assertTrue(1000.0 / p.glintDurMs * 0.5 < 3.0 || p.glintDurMs >= 300)
    }

    // --- Ambient idle glint (v2 section 2) ---

    @Test fun idleGlintIsOnAndGated() {
        // Chris 12:08 CT: "I approve! Go for it!"
        assertTrue(p.idleTwinkleEnabled)
        assertTrue(ProIdleTwinkle.allowed(p, animatorsEnabled = true))
        assertFalse("reduced motion", ProIdleTwinkle.allowed(p, animatorsEnabled = false))
        assertFalse("low RAM", ProIdleTwinkle.allowed(p, animatorsEnabled = true, lowRam = true))
        assertFalse("gates off (B4)", ProIdleTwinkle.allowed(p, animatorsEnabled = true, gatesOn = false))
        assertFalse(ProIdleTwinkle.allowed(p.copy(idleTwinkleEnabled = false), animatorsEnabled = true))
    }

    @Test fun idleGlintScheduleIsOnceUnder3HzDoneBy2950() {
        assertEquals(listOf(250L, 630L, 1_010L, 1_390L, 1_770L, 2_150L), p.idleGlintStartsMs)
        assertEquals(800L, p.idleGlintDurMs)
        assertEquals(2_950L, ProIdleTwinkle.totalMs(p))
        assertTrue("peak rate ${ProIdleTwinkle.peakRateHz(p)}", ProIdleTwinkle.peakRateHz(p) < 3.0)
        assertTrue(1000.0 / p.idleGlintDurMs <= 1.7) // each glint ~1.25 Hz
        for (i in 0 until 6) {
            assertEquals(0f, ProIdleTwinkle.alpha(p, i, 2_950L), 0f) // never loops: nothing after the end
            assertEquals(1f, ProIdleTwinkle.alpha(p, i, p.idleGlintStartsMs[i] + 400), 1e-4f)
        }
    }

    @Test fun idleGlintTimesAreStartsWithSin2Shape() {
        // Da Vinci 12:14 CT: 250/630/1010/1390/1770/2150 are STARTS; 800 ms sin^2 each; last ends 2950.
        val d = p.idleGlintDurMs
        for ((i, s0) in p.idleGlintStartsMs.withIndex()) {
            assertEquals("zero before start", 0f, ProIdleTwinkle.alpha(p, i, s0 - 1), 0f)
            assertEquals("zero at start", 0f, ProIdleTwinkle.alpha(p, i, s0), 1e-6f)
            assertEquals("sin^2 quarter", 0.5f, ProIdleTwinkle.alpha(p, i, s0 + d / 4), 1e-4f)
            assertEquals("peak mid-pulse", 1f, ProIdleTwinkle.alpha(p, i, s0 + d / 2), 1e-4f)
            assertEquals("sin^2 three-quarter", 0.5f, ProIdleTwinkle.alpha(p, i, s0 + 3 * d / 4), 1e-4f)
            assertEquals("zero at end", 0f, ProIdleTwinkle.alpha(p, i, s0 + d), 0f)
            for (k in 0..40) {
                val tt = s0 + k * d / 40
                val x = Math.sin(Math.PI * (tt - s0) / d)
                assertEquals(x * x, ProIdleTwinkle.alpha(p, i, tt).toDouble(), 1e-4)
            }
        }
        assertEquals(380L, p.idleGlintStartsMs.zipWithNext { a, b -> b - a }.distinct().single())
        assertEquals(2_150L + 800L, ProIdleTwinkle.totalMs(p))
    }

    // --- O9 frame guard ---

    @Test fun frameGuardHalvesDustFirstOnFirstSlowFrame() {
        // Da Vinci 12:14 CT: a frame over 16 ms halves the dust layer FIRST, before cutting anything else.
        val g = ProFrameGuard()
        assertEquals(0, g.record(16_000_000)) // exactly on budget: not over
        assertEquals(0, g.record(10_000_000))
        assertEquals(1, g.record(16_000_001))
        val dustKept = (0 until 380).count { g.keep("dust", it) }
        assertEquals(190, dustKept)
        for (layer in listOf("back", "mid", "front")) {
            assertEquals(layer, 1000, (0 until 1000).count { g.keep(layer, it) })
        }
    }

    @Test fun frameGuardCutsFurtherOnlyAfterThreeMoreSlowFrames() {
        val g = ProFrameGuard()
        g.record(20_000_000); assertEquals(1, g.level)
        g.record(20_000_000); g.record(20_000_000); assertEquals(1, g.level)
        g.record(5_000_000); g.record(20_000_000); g.record(20_000_000); assertEquals("streak reset", 1, g.level)
        g.record(20_000_000); assertEquals(2, g.level)
        assertFalse(g.keep("dust", 0)); assertFalse(g.keep("back", 1)); assertTrue(g.keep("back", 2))
        assertTrue(g.keep("mid", 1)); assertTrue(g.keep("front", 1))
        repeat(10) { g.record(40_000_000) }
        assertEquals(ProFrameGuard.MAX_LEVEL, g.level)
        repeat(50) { g.record(1_000_000) }
        assertEquals("never steps back up", ProFrameGuard.MAX_LEVEL, g.level)
        assertTrue(ProFrameGuard().keep("dust", 1))
    }

    @Test fun sin2PulseShape() {
        assertEquals(0f, sin2Pulse(99, 100, 200), 0f)
        assertEquals(1f, sin2Pulse(200, 100, 200), 1e-4f)
        assertEquals(0f, sin2Pulse(300, 100, 200), 0f)
    }
}
