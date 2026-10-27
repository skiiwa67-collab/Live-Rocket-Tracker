package com.ccos.retro.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** vc160 Da Vinci LRT Pro Gold v1 4.4: one-time unlock moment rules. */
class ProGoldMomentTest {
    private class RecordingStore : ProStore {
        val m = MemProStore()
        val log = ArrayList<String>()
        override fun getString(key: String) = m.getString(key)
        override fun putString(key: String, value: String) = m.putString(key, value)
        override fun getBoolean(key: String, default: Boolean) = m.getBoolean(key, default)
        override fun putBoolean(key: String, value: Boolean) { log += "put:$key"; m.putBoolean(key, value) }
        override fun commitBoolean(key: String, value: Boolean) { log += "commit:$key"; m.putBoolean(key, value) }
    }

    private fun claim(src: ProGoldMoment.Source, s: ProStore, anim: Boolean = true, lowRam: Boolean = false, finishing: Boolean = false) =
        ProGoldMoment.claim(src, s, finishing, anim, lowRam)

    @Test fun flagIsCommittedBeforeTheMomentPlays() {
        val s = RecordingStore()
        val mode = claim(ProGoldMoment.Source.PURCHASE, s)
        assertEquals(ProGoldMoment.Mode.ANIMATED, mode)
        // The flag is already durable (sync commit) when claim() hands back a playable mode.
        assertTrue(s.getBoolean(ProGoldMoment.KEY_SHOWN, false))
        assertEquals(listOf("commit:${ProGoldMoment.KEY_SHOWN}"), s.log)
    }

    @Test fun playsOnlyOnce() {
        val s = MemProStore()
        assertEquals(ProGoldMoment.Mode.ANIMATED, claim(ProGoldMoment.Source.PURCHASE, s))
        assertEquals(ProGoldMoment.Mode.NONE, claim(ProGoldMoment.Source.PURCHASE, s))
        assertEquals(ProGoldMoment.Mode.NONE, claim(ProGoldMoment.Source.PURCHASE, s, anim = false))
    }

    @Test fun restorePendingAndRefreshNeverPlay() {
        for (src in listOf(ProGoldMoment.Source.RESTORE, ProGoldMoment.Source.PENDING_CONFIRMED, ProGoldMoment.Source.REFRESH)) {
            val s = RecordingStore()
            assertEquals(src.name, ProGoldMoment.Mode.NONE, claim(src, s))
            assertFalse("no flag written for $src", s.getBoolean(ProGoldMoment.KEY_SHOWN, false))
            assertTrue(s.log.isEmpty())
        }
        assertEquals(ProGoldMoment.Source.PENDING_CONFIRMED, ProGoldMoment.sourceForPurchase(true))
        assertEquals(ProGoldMoment.Source.PURCHASE, ProGoldMoment.sourceForPurchase(false))
    }

    @Test fun finishingActivityNeverPlaysOrBurnsTheFlag() {
        val s = MemProStore()
        assertEquals(ProGoldMoment.Mode.NONE, claim(ProGoldMoment.Source.PURCHASE, s, finishing = true))
        assertFalse(s.getBoolean(ProGoldMoment.KEY_SHOWN, false))
    }

    @Test fun reducedMotionAndLowRamGetTheStaticFrame() {
        assertEquals(ProGoldMoment.Mode.STATIC, claim(ProGoldMoment.Source.PURCHASE, MemProStore(), anim = false))
        assertEquals(ProGoldMoment.Mode.STATIC, claim(ProGoldMoment.Source.PURCHASE, MemProStore(), lowRam = true))
    }

    @Test fun timingIsTwoPointTwoSecondsAndTalkBackHoldsLonger() {
        assertEquals(2_200L, ProGoldMoment.DURATION_MS)
        assertEquals(2_200L, ProGoldMoment.holdMs(ProGoldMoment.Mode.ANIMATED, false))
        assertEquals(2_000L, ProGoldMoment.holdMs(ProGoldMoment.Mode.STATIC, false))
        assertEquals(4_000L, ProGoldMoment.holdMs(ProGoldMoment.Mode.ANIMATED, true))
        assertEquals(0L, ProGoldMoment.holdMs(ProGoldMoment.Mode.NONE, false))
    }

    private fun src(rel: String): String {
        val f = listOf(File("src/main/$rel"), File("app/src/main/$rel")).first { it.exists() }
        return f.readText().replace("\r\n", "\n")
    }

    @Test fun sheetClaimsBeforePlayingAndOnlyFromThePurchaseCallback() {
        val ui = src("java/com/ccos/retro/ui/ProUi.kt")
        val claimAt = ui.indexOf("ProGoldMoment.claim(")
        val playAt = ui.indexOf("playMoment(mode)")
        assertTrue(claimAt > 0 && playAt > claimAt)
        // Only the purchase callback passes a source; restore keeps the plain dismiss + toast.
        assertEquals(1, Regex("""apply\(next, ProGoldMoment\.sourceForPurchase\(sawPending\)\)""").findAll(ui).count())
        assertTrue("restore path must not pass a source", "apply(SheetState.OWNED)\n" in ui)
        // The DEBUG preview never writes the flag or grants Pro.
        val preview = ui.substringAfter("when (goldPreview) {").substringBefore("enum class GoldPreview")
        assertFalse(preview.contains("KEY_SHOWN") || preview.contains("KEY_PURCHASED") || preview.contains("claim("))
        assertTrue(ui.contains("goldPreview != null && !com.ccos.retro.BuildConfig.DEBUG"))
    }

    @Test fun prefsStoreCommitsSynchronously() {
        val p = src("java/com/ccos/retro/pro/ProStore.kt")
        assertTrue(Regex("""override fun commitBoolean[\s\S]{0,200}\.commit\(\)""").containsMatchIn(p))
    }
}
