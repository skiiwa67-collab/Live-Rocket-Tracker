package com.ccos.retro.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Helios P3: no stale "UNWIRED" header on wired Pro copy; no orphan tip146 doc above selectLaunch. */
class StaleCommentsTest {
    private val main = listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }

    @Test fun stringsProHeaderIsCurrent() {
        val xml = main.resolve("res/values/strings_pro.xml").readText().replace("\r\n", "\n")
        assertFalse(xml.contains("UNWIRED"))
        assertTrue(xml.contains("<!-- LRT Pro copy. Source:"))
    }

    @Test fun selectLaunchHasNoOrphanDoc() {
        val m = main.resolve("java/com/ccos/retro/module/RocketTelemetryModule.kt").readText().replace("\r\n", "\n")
        val before = m.substringBefore("    fun selectLaunch(id: String").takeLast(300)
        assertFalse(before.contains("gold 5s when LL2"))
        assertTrue(before.trimEnd().endsWith("// vc159: defense-in-depth Pro gate. A locked replay is not selected; UI shows the sheet / toast."))
    }
}
