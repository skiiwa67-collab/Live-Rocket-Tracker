package com.ccos.retro.pro

import com.ccos.retro.model.AppPrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** vc160 Da Vinci Pro Gold v1 4.2: free themes never get a padlock; gated Pro themes get the gold one. */
class GoldPadlockTest {
    private fun src(rel: String): String =
        listOf(File("src/main/$rel"), File("app/src/main/$rel")).first { it.exists() }.readText().replace("\r\n", "\n")

    // Gates forced on: debug/stub builds default them off (B4), which would make everything look free.
    private fun locked(id: String, store: ProStore) =
        !ProPolicy.allows(ProHook.THEME_PICK_SETTINGS, false, skinId = id, store = store, gatesOn = true)

    @Test fun freeThemesNeverShowAPadlock() {
        val store = MemProStore()
        for (id in ThemeAccess.freeSet(store)) assertFalse("$id must stay free", locked(id, store))
    }

    @Test fun freeThemeIdsNeverGetAPadlock() {
        // Chris 11:11 / Da Vinci v2 answer 3: Starship, Tesla, Roadster live in FREE_THEME_IDS (TODO O1, empty today,
        // no invented IDs). Whatever lands there is in the free set, never locked, never gated for a free user.
        val store = MemProStore()
        val free = ThemeAccess.freeSet(store)
        assertTrue(free.containsAll(ThemeAccess.FREE_THEME_IDS))
        for (id in ThemeAccess.FREE_THEME_IDS) assertFalse("$id must have no padlock", locked(id, store))
    }

    @Test fun freeThemeIdsIsEmptyWithAnO1Todo() {
        assertTrue("no invented IDs until spec O1 lands", ThemeAccess.FREE_THEME_IDS.isEmpty())
        val src = src("java/com/ccos/retro/pro/ThemeAccess.kt")
        assertTrue(src.contains("TODO(O1"))
        assertTrue(src.contains("FREE_BASE + FREE_THEME_IDS + freeEuroSkin(store)"))
    }

    @Test fun proThemesAreLockedForFreeUsersAndOpenForPro() {
        val store = MemProStore()
        val pro = AppPrefs.CONSOLE_SKIN_IDS.filter { it !in ThemeAccess.freeSet(store) }
        assertTrue("expected some Pro themes", pro.isNotEmpty())
        for (id in pro) {
            assertTrue("$id gated", locked(id, store))
            assertTrue("$id open for Pro", ProPolicy.allowsTheme(ProHook.THEME_PICK_SETTINGS, true, id, store))
        }
    }

    @Test fun lockedChipsUseTheGoldPadlock() {
        val main = src("java/com/ccos/retro/ui/MainActivity.kt")
        val fn = main.substringAfter("private fun chipLockForeground(").substringBefore("override fun")
        assertTrue(fn.contains("R.drawable.ic_pro_lock_gold"))
        assertTrue(main.contains("b?.foreground = if (locked && i != sel) {"))
        assertTrue(main.contains("chipLockForeground(g)"))
        val lock = src("res/drawable/ic_pro_lock_gold.xml")
        for (c in listOf("#FFD700", "#FFD27A", "#1A1200", "#0B0F14")) assertTrue(c, lock.contains(c, ignoreCase = true))
    }
}
