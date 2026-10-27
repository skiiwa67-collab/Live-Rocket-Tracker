package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** B6: T+60 free-hour fading note + auto-switch (Chris 08:11/08:13, Da Vinci 08:12/08:13, R-NO-BADGER). */
class FreeHourNoteTest {
    @Before fun gatesOn() { ProConfig.debugForceGates = true }
    @After fun gatesReset() { ProConfig.debugForceGates = false }

    private fun inFlight(liftoffAgo: Long, id: String = "ll2-live") =
        snap(id = id, net = NOW - liftoffAgo, abbrev = "In Flight", status = "Launch in Flight")
    private fun upcoming(id: String, inMs: Long, mission: String = "Mission $id") =
        snap(id = id, net = NOW + inMs, abbrev = "Go", status = "Go for Launch", mission = mission)

    private fun file(rel: String) = listOf(File(rel), File("app/$rel")).first { it.exists() }.readText()
    private val stringsXml by lazy { file("src/main/res/values/strings_pro.xml") }
    private fun str(name: String): String =
        Regex("""<string name="$name"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).find(stringsXml)!!.groupValues[1]

    // ---- note: once, timing, who sees it ----

    @Test fun noteShowsOncePerLaunchEvenAfterVisitingAnother() {
        val store = MemProStore()
        val a = inFlight(61 * MIN, "A"); val b = inFlight(61 * MIN, "B")
        assertTrue(LiveTrackingAllowance.shouldShowCard(a, false, store, NOW))
        LiveTrackingAllowance.markShown(store, a)
        assertFalse(LiveTrackingAllowance.shouldShowCard(a, false, store, NOW))
        LiveTrackingAllowance.markShown(store, b)
        assertFalse("A -> B -> A never re-shows", LiveTrackingAllowance.shouldShowCard(a, false, store, NOW + HOUR))
    }

    @Test fun oldSingleIdValueStillCounts() {
        val store = MemProStore().apply { putString(LiveTrackingAllowance.KEY_CARD_SHOWN_ID, "A") }
        assertFalse(LiveTrackingAllowance.shouldShowCard(inFlight(61 * MIN, "A"), false, store, NOW))
    }

    @Test fun timingConstantsAndAutoDismiss() {
        assertEquals(300L, LiveTrackingAllowance.NOTE_FADE_IN_MS)
        assertEquals(5_000L, LiveTrackingAllowance.NOTE_HOLD_MS)
        assertEquals(600L, LiveTrackingAllowance.NOTE_FADE_OUT_MS)
        assertEquals(5_900L, LiveTrackingAllowance.NOTE_TOTAL_MS)
        assertEquals(0f, LiveTrackingAllowance.noteAlpha(0), 0.001f)
        assertEquals(0.5f, LiveTrackingAllowance.noteAlpha(150), 0.01f)
        assertEquals(1f, LiveTrackingAllowance.noteAlpha(300), 0.001f)
        assertEquals(1f, LiveTrackingAllowance.noteAlpha(5_299), 0.001f)
        assertEquals(0.5f, LiveTrackingAllowance.noteAlpha(5_600), 0.01f)
        assertFalse("gone by itself", LiveTrackingAllowance.isNoteVisible(5_900))
        assertFalse(LiveTrackingAllowance.isNoteVisible(60_000))
    }

    @Test fun neverForProOrSwitchOff() {
        val l = inFlight(2 * HOUR)
        assertFalse(LiveTrackingAllowance.isFreeHourOver(l, true, NOW, true))
        // Chris 8:22: no grandfathering. A stale legacy flag is a free user.
        val gf = MemProStore().apply { putBoolean("pro_grandfathered", true) }
        assertTrue(LiveTrackingAllowance.isFreeHourOver(l, ProAccess(gf).isPro, NOW, true))
        assertFalse(LiveTrackingAllowance.isFreeHourOver(l, false, NOW, gatesOn = false))
        assertFalse(LiveTrackingAllowance.shouldShowCard(l, false, MemProStore(), NOW, gatesOn = false))
        assertTrue(LiveTrackingAllowance.isFreeHourOver(l, false, NOW, true))
        assertFalse("T+59 nothing yet", LiveTrackingAllowance.isFreeHourOver(inFlight(59 * MIN), false, NOW, true))
    }

    // ---- copy ----

    @Test fun exactStringsExist() {
        assertEquals("Free live tracking covers the first hour after liftoff. Up next: %1\$s", str("pro_allowance_note_next"))
        assertEquals("Free live tracking covers the first hour after liftoff. Back to launches.", str("pro_allowance_note_none"))
        assertEquals("Free live tracking covers the first hour after liftoff.", str("pro_allowance_note_short"))
        assertEquals("Unlimited live tracking for the whole flight (free covers the first hour after liftoff)", str("pro_sheet_item_live"))
        assertEquals("Unlimited live tracking (free: first hour after liftoff)", str("pro_sheet_item_live_short"))
        assertEquals("Included free: live and upcoming launches (first hour of live tracking), Auto theming, Starship Flight 13, " +
            "a featured mission from each country, and the free themes.", str("pro_sheet_included"))
    }

    @Test fun noteCopyHasNoLockDemoOrPro() {
        for (n in listOf("pro_allowance_note_next", "pro_allowance_note_none", "pro_allowance_note_short",
                "pro_sheet_item_live", "pro_sheet_item_live_short", "pro_sheet_included")) {
            val v = str(n).lowercase()
            assertFalse(n, Regex("""\block(ed)?\b""").containsMatchIn(v))
            assertFalse(n, v.contains("demo"))
        }
        for (n in listOf("pro_allowance_note_next", "pro_allowance_note_none", "pro_allowance_note_short"))
            assertFalse("$n mentions Pro", Regex("""\bpro\b""", RegexOption.IGNORE_CASE).containsMatchIn(str(n)))
    }

    @Test fun proSheetListsUnlimitedLiveTracking() {
        val sheet = file("src/main/res/layout/sheet_pro_upsell.xml")
        assertTrue(sheet.contains("@+id/pro_sheet_item_live"))
        assertTrue(sheet.contains("@string/pro_sheet_item_live"))
        assertTrue(file("src/main/java/com/ccos/retro/ui/ProUi.kt").contains("R.string.pro_sheet_item_live_short"))
    }

    private val fmt = "Free live tracking covers the first hour after liftoff. Up next: %1\$s"
    private val none = "Free live tracking covers the first hour after liftoff. Back to launches."
    private val short = "Free live tracking covers the first hour after liftoff."

    @Test fun upNextNameIsFilled() {
        val t = LiveTrackingAllowance.noteText("Starlink Group 10-5", fmt, none, short) { true }
        assertEquals("Free live tracking covers the first hour after liftoff. Up next: Starlink Group 10-5", t)
        assertEquals("Crew-12", LiveTrackingAllowance.nextName(upcoming("x", HOUR, mission = "Crew-12")))
        assertEquals("Falcon 9 | ", LiveTrackingAllowance.nextName(upcoming("x", HOUR, mission = "").copy(name = "Falcon 9 | ")))
    }

    @Test fun noNextVariant() {
        assertEquals(none, LiveTrackingAllowance.noteText(null, fmt, none, short) { true })
        assertEquals(none, LiveTrackingAllowance.noteText("  ", fmt, none, short) { true })
    }

    @Test fun dropClauseWhenItDoesNotFitNeverEllipsize() {
        val t = LiveTrackingAllowance.noteText("A Very Long Mission Name That Wraps", fmt, none, short) { false }
        assertEquals(short, t)
        assertFalse(t.contains("…") || t.contains("..."))
    }

    // ---- auto-switch ----

    private fun decide(store: ProStore = MemProStore(), viewing: String? = "L", start: Long = NOW, touch: Long = 0L,
                       now: Long = NOW + 5_900, pro: Boolean = false, gates: Boolean = true) =
        LiveTrackingAllowance.handoffDecision("L", viewing, start, touch, now, pro, gates, store)

    @Test fun switchesAfterTheFadeForFree() {
        assertEquals(LiveTrackingAllowance.Handoff.WAIT, decide(now = NOW + 5_899))
        assertEquals(LiveTrackingAllowance.Handoff.SWITCH, decide(now = NOW + 5_900))
    }

    @Test fun noSwitchForProOrSwitchOff() {
        assertEquals(LiveTrackingAllowance.Handoff.CANCEL, decide(pro = true))
        assertEquals(LiveTrackingAllowance.Handoff.CANCEL, decide(gates = false))
    }

    @Test fun noSwitchIfUserLeft() {
        assertEquals(LiveTrackingAllowance.Handoff.CANCEL, decide(viewing = "OTHER"))
        assertEquals(LiveTrackingAllowance.Handoff.CANCEL, decide(viewing = null))
    }

    @Test fun reopeningDoesNotRetrigger() {
        val store = MemProStore()
        LiveTrackingAllowance.markHandoffDone(store, "L")
        assertEquals(LiveTrackingAllowance.Handoff.CANCEL, decide(store = store, now = NOW + HOUR))
        assertTrue(LiveTrackingAllowance.isHandoffDone(store, "L"))
    }

    @Test fun recentTouchDefersUntilFiveSecondsIdle() {
        val touch = NOW + 5_000
        assertEquals(LiveTrackingAllowance.Handoff.WAIT, decide(touch = touch, now = NOW + 6_000))
        assertEquals(LiveTrackingAllowance.Handoff.WAIT, decide(touch = touch, now = touch + 4_999))
        assertEquals(LiveTrackingAllowance.Handoff.SWITCH, decide(touch = touch, now = touch + 5_000))
    }

    @Test fun picksSoonestRealFutureLaunchSkippingSamples() {
        val list = listOf(
            inFlight(2 * HOUR, "L"),
            upcoming("demo-starship-f13", 10 * MIN),
            upcoming("far", 5 * HOUR),
            upcoming("soon", 2 * HOUR),
            snap(id = "past", net = NOW - 3 * HOUR),
            upcoming("done", HOUR).copy(statusAbbrev = "Success", statusName = "Launch Successful")
        )
        assertEquals("soon", LiveTrackingAllowance.nextUpcoming(list, "L", NOW)?.id)
    }

    @Test fun fallsBackHomeWhenNoNext() {
        assertNull(LiveTrackingAllowance.nextUpcoming(listOf(inFlight(2 * HOUR, "L"), upcoming("demo-x", HOUR)), "L", NOW))
        assertNull(LiveTrackingAllowance.nextUpcoming(emptyList<LaunchSnapshot>(), "L", NOW))
    }

    // ---- wiring (source) ----

    private val module by lazy { file("src/main/java/com/ccos/retro/module/RocketTelemetryModule.kt") }
    private fun body(name: String): String = module.substringAfter("fun $name(").substringBefore("\n    fun ")

    @Test fun wallpaperShowsNoNoteButStillMoves() {
        val wp = file("src/main/java/com/ccos/retro/wallpaper/RetroCommandWallpaperService.kt")
        assertFalse(wp.contains("onLiveAllowanceUsed"))
        assertFalse(wp.contains("pro_allowance_note"))
        assertFalse(wp.contains("showFreeHourNote"))
        // The handoff wait starts before the (activity-only) handler check, so a module with no handler still moves.
        val check = body("checkLiveTrackingAllowance")
        assertTrue(check.indexOf("freeHourPendingId = l.id") in 0 until check.indexOf("onLiveAllowanceUsed ?: return"))
        assertTrue(module.substringAfter("fun resolveTracked(").take(700).contains("freeHourTick(now)"))
    }

    @Test fun switchIsInternalNeverGatedNeverSheet() {
        val sw = body("freeHourTick") + module.substringAfter("private fun handoffAfterFreeHour(").substringBefore("\n    fun ")
        assertFalse(sw.contains("selectLaunch(") || sw.contains("proAllows") || sw.contains("onProBlocked"))
        assertTrue(sw.contains("rememberTracked(next)"))
        assertTrue(sw.contains("LiveTrackingAllowance.nextUpcoming") || module.contains("LiveTrackingAllowance.nextUpcoming(provider.getCached()"))
    }

    @Test fun activitiesNeverAutoOpenTheSheetOnAllowance() {
        for (a in listOf("CommandCenterActivity", "MainActivity")) {
            val s = file("src/main/java/com/ccos/retro/ui/$a.kt")
            assertFalse(a, s.contains("liveAllowance = true"))
            val wire = s.substringAfter("onLiveAllowanceUsed = ").take(200)
            assertTrue(a, wire.contains("showFreeHourNote"))
            assertFalse(a, wire.contains("showUpsell"))
        }
        val cc = file("src/main/java/com/ccos/retro/ui/CommandCenterActivity.kt")
        assertTrue("idle guard wired", cc.contains("telemetryModule.noteUserTouch()"))
        assertTrue("1 Hz tick", cc.contains("telemetryModule.freeHourTick(now)"))
    }

    @Test fun noteSlotIsAboveTheCommandRowAndNotATapTarget() {
        val lay = file("src/main/res/layout/activity_command_center.xml")
        val note = lay.indexOf("@+id/free_hour_note")
        assertTrue(note in 0 until lay.indexOf("@+id/top_chrome"))
        assertTrue(note < lay.indexOf("@+id/vid_well"))
        val tag = lay.substring(note, lay.indexOf("/>", note))
        assertTrue(tag.contains("""android:textSize="16sp""""))
        assertTrue(tag.contains("""android:clickable="false""""))
        assertTrue(tag.contains("""android:accessibilityLiveRegion="polite""""))
        assertFalse(tag.contains("ellipsize"))
    }
}
