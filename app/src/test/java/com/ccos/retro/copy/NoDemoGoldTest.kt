package com.ccos.retro.copy

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** vc160: never the word "Demo" in the Pro Gold resources or code. */
class NoDemoGoldTest {
    private val files = listOf(
        "res/values/colors_pro_gold.xml", "res/drawable/ic_pro_lock_gold.xml", "res/drawable/ic_pro_check_gold.xml",
        "res/drawable/bg_pro_cta_gold.xml", "res/drawable/bg_pro_pill_gold.xml", "res/drawable/bg_pro_row_owned_gold.xml",
        "res/drawable/ic_pro_sparkle.xml", "res/layout/view_pro_unlock.xml", "res/layout/sheet_pro_upsell.xml",
        "java/com/ccos/retro/ui/ProUnlockView.kt", "java/com/ccos/retro/ui/ProGold.kt",
        "java/com/ccos/retro/pro/ProGoldMoment.kt", "java/com/ccos/retro/pro/ProSparkle.kt"
    )

    @Test fun noDemoInProGold() {
        for (rel in files) {
            val f = listOf(File("src/main/$rel"), File("app/src/main/$rel")).firstOrNull { it.exists() }
                ?: throw AssertionError("missing $rel")
            assertTrue("'demo' in $rel", !Regex("demo", RegexOption.IGNORE_CASE).containsMatchIn(f.readText()))
        }
    }

    @Test fun unlockPanelUsesExistingCopyAndIsDecorativeArt() {
        val f = listOf(File("src/main/res/layout/view_pro_unlock.xml"), File("app/src/main/res/layout/view_pro_unlock.xml")).first { it.exists() }
        val t = f.readText()
        assertTrue(t.contains("@string/pro_settings_owned") && t.contains("@string/pro_toast_on"))
        assertTrue(t.contains("android:importantForAccessibility=\"no\""))
    }
}
