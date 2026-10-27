package com.ccos.retro.pro

import com.ccos.retro.model.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * vc159 R-NO-DEMO-NAMING steering: walk every feature hook and prove each Included feature stays
 * unlocked for a free user; only past replays outside the Included set, Pro themes, and replay
 * jump/skip on non-live missions are gated.
 */
class ProPolicyTest {
    // B4: gates are off by default until billing is real; these tests exercise the gated (forced-on) state.
    @org.junit.Before fun gatesOn() { ProConfig.debugForceGates = true }
    @org.junit.After fun gatesReset() { ProConfig.debugForceGates = false }


    private val launchHooks = ProHook.values().filter { it.kind != ProHook.Kind.THEME }
    private val themeHooks = ProHook.values().filter { it.kind == ProHook.Kind.THEME }

    /** Included launches: live, upcoming, in-window, holds, every Included replay, real Starship F13. */
    private fun includedLaunches() = listOf(
        "live webcast" to snap(id = "ll2-live", net = NOW - 10 * MIN, abbrev = "In Flight", status = "Launch in Flight", webcastLive = true),
        "upcoming" to snap(id = "ll2-up", net = NOW + 3 * HOUR, abbrev = "Go", status = "Go for Launch"),
        "upcoming far" to snap(id = "ll2-up2", net = NOW + 30L * 24 * HOUR, abbrev = "TBD", status = "To Be Determined"),
        "in-window success" to snap(id = "ll2-win", net = NOW - 30 * MIN),
        "in-window failure" to snap(id = "ll2-winf", net = NOW - 20 * MIN, abbrev = "Failure", status = "Launch Failure"),
        "hold" to snap(id = "ll2-hold", net = NOW + 5 * MIN, abbrev = "Hold", status = "On Hold"),
        "real Starship F13" to snap(id = "ll2-f13", net = NOW - 200L * 24 * HOUR, rocket = "Starship", mission = "Flight 13", name = "Starship | Flight 13"),
    ) + DemoSet.FREE_DEMO_IDS.map { "included $it" to snap(id = it, net = NOW - 400L * 24 * HOUR) }

    private fun lockedReplay() = snap(id = "ll2-old", net = NOW - 3 * HOUR)

    @Test fun everyHookHasAKnownKind() {
        assertEquals(8, ProHook.values().size)
        assertEquals(3, ProHook.Kind.values().size)
    }

    @Test fun everyLaunchHookKeepsIncludedLaunchesOpenForFree() {
        for (hook in launchHooks) for ((why, l) in includedLaunches()) {
            assertTrue("$hook must allow $why for a free user", ProPolicy.allows(hook, isPro = false, launch = l, now = NOW))
        }
    }

    @Test fun everyLaunchHookAllowsNullLaunch() {
        for (hook in launchHooks) assertTrue(hook.name, ProPolicy.allows(hook, false, launch = null, now = NOW))
    }

    @Test fun everyThemeHookKeepsFreeThemesOpenForFree() {
        val store = MemProStore()
        val free = ThemeAccess.freeSet(store)
        assertTrue(free.containsAll(ThemeAccess.FREE_BASE))
        assertTrue(free.any { it in ThemeAccess.EURO_POOL })
        for (hook in themeHooks) for (id in free) {
            assertTrue("$hook must allow free theme $id", ProPolicy.allows(hook, false, skinId = id, store = store, now = NOW))
        }
    }

    @Test fun themeHooksWithoutAThemeAreNotGated() {
        for (hook in themeHooks) assertTrue(ProPolicy.allows(hook, false, skinId = null, store = MemProStore(), now = NOW))
    }

    @Test fun onlyPastReplaysOutsideIncludedAreGated() {
        val old = lockedReplay()
        for (hook in launchHooks) {
            assertFalse("$hook gates a past replay for free", ProPolicy.allows(hook, false, launch = old, now = NOW))
            assertTrue("$hook opens for Pro", ProPolicy.allows(hook, true, launch = old, now = NOW))
        }
    }

    @Test fun replayJumpSkipGatedButLiveJumpSkipFree() {
        val live = snap(id = "ll2-live", net = NOW - 5 * MIN, webcastLive = true)
        for (hook in listOf(ProHook.JUMP, ProHook.SKIP_EVENT)) {
            assertTrue(ProPolicy.allows(hook, false, launch = live, now = NOW))
            assertFalse(ProPolicy.allows(hook, false, launch = lockedReplay(), now = NOW))
        }
    }

    @Test fun proThemesGatedForFreeOpenForPro() {
        val store = MemProStore()
        val pro = AppPrefs.CONSOLE_SKIN_IDS.filter { it !in ThemeAccess.freeSet(store) }
        assertTrue("there are Pro themes", pro.isNotEmpty())
        for (hook in themeHooks) for (id in pro) {
            assertFalse("$hook gates $id", ProPolicy.allows(hook, false, skinId = id, store = store, now = NOW))
            assertTrue("$hook opens $id for Pro", ProPolicy.allows(hook, true, skinId = id, store = store, now = NOW))
        }
    }

    @Test fun autoThemingIsGatedToo() {
        // Chris 8:22: Pro unlocks ALL themes; the gate stands on live launches too. The Auto map stays pure,
        // and the apply step only uses a theme the user may pick.
        val prefs = src("model/AppPrefs.kt")
        val map = Regex("""fun consoleSkinIdForLaunch\b[\s\S]*?\n        }""").find(prefs)!!.value
        for (gate in listOf("ProPolicy", "ThemeAccess", "ProGate", "isPro")) assertFalse("map uses $gate", map.contains(gate))
        val apply = Regex("""fun noteConsoleSkinTrackedLaunch\b[\s\S]*?\n    }""").find(prefs)!!.value
        assertTrue(apply.contains("ThemeAccess.displayedDuringLive(consoleSkinIdForLaunch(launch), isPro, proStore())"))
    }

    @Test fun everyHookIsWiredOutsidePro() {
        val main = mainSourcesOutsidePro()
        for (hook in ProHook.values()) {
            assertTrue("ProHook.${hook.name} used outside pro/", main.any { it.second.contains("ProHook.${hook.name}") })
        }
    }

    @Test fun uiNeverCallsRawGatesDirectly() {
        for ((name, text) in mainSourcesOutsidePro()) {
            for (raw in listOf("ProGate.isLocked", "ProGate.canJumpOrSkip", "ThemeAccess.canPickManually")) {
                assertFalse("$name calls $raw directly (use ProPolicy)", text.contains(raw))
            }
        }
    }

    @Test fun moduleEntryPointsUseProPolicy() {
        val m = src("module/RocketTelemetryModule.kt")
        assertTrue(m.contains("proAllows(ProHook.JUMP"))
        assertTrue(m.contains("proAllows(ProHook.SKIP_EVENT"))
        assertTrue(m.contains("proAllows(ProHook.SELECT_LAUNCH"))
        assertTrue(m.contains("proAllows(ProHook.HISTORIC_ROLL"))
    }

    private fun srcRoot(): File =
        listOf(File("src/main/java/com/ccos/retro"), File("app/src/main/java/com/ccos/retro")).first { it.isDirectory }

    private fun src(rel: String) = File(srcRoot(), rel).readText()

    private fun mainSourcesOutsidePro(): List<Pair<String, String>> =
        srcRoot().walkTopDown().filter { it.isFile && it.extension == "kt" && !it.path.contains("/pro/") }
            .map { it.name to it.readText() }.toList()
}
