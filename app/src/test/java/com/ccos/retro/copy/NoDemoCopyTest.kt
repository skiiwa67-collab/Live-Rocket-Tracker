package com.ccos.retro.copy

import com.ccos.retro.pro.PickerTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Chris 10-27 7:10 CT: no "demo" anywhere users can see. Scans source so a regression fails the build's tests. */
class NoDemoCopyTest {
    private fun src(rel: String): File {
        val f = listOf(File("src/main/$rel"), File("app/src/main/$rel")).firstOrNull { it.exists() }
        return f ?: throw AssertionError("missing source file $rel")
    }

    private val demo = Regex("demo", RegexOption.IGNORE_CASE)
    private val provider by lazy { src("java/com/ccos/retro/data/LaunchDataProvider.kt").readText() }

    private fun literals(text: String, field: String): List<String> =
        Regex("""\b$field\s*=\s*"([^"]*)"""").findAll(text).map { it.groupValues[1] }.toList()

    @Test fun builtInMissionDisplayNamesHaveNoDemo() {
        val names = literals(provider, "name") + literals(provider, "missionName")
        // Chris 8:22 (313d8b8) removed the made-up per-country samples, so the catalog is ~16 literals now.
        assertTrue("expected the built-in catalog names", names.size >= 12)
        val bad = names.filter { demo.containsMatchIn(it) }
        assertTrue("visible 'demo' in names: $bad", bad.isEmpty())
    }

    @Test fun v11NamesArePresent() {
        // v1.1 names that survive Chris 8:22 (samples are real launches; made-up per-country ones removed in 313d8b8).
        for (n in listOf("Falcon 9 Block 5 | Starlink Mission", "Falcon 9 | Crew Mission (Historical)",
            "Falcon 9 | Ascent Anomaly Scenario", "New Glenn | Featured Mission", "Electron | Featured Flight",
            "Starship | Flight 13")) assertTrue(n, "name = \"$n\"" in provider)
        for (m in listOf("Starlink Mission", "Ascent Profile", "Crew Mission", "Ascent Anomaly Scenario", "Flight 13"))
            assertTrue(m, "missionName = \"$m\"" in provider)
        // ...and the removed made-up samples stay gone.
        for (n in listOf("SLS Block 1 | Artemis Mission", "Soyuz-2.1a | ISS Crew Mission", "Long March 5 | Featured Mission",
            "LVM3 | Featured Mission", "Ariane 6 | Featured Flight", "H3 | Featured Flight")) assertFalse(n, "name = \"$n\"" in provider)
    }

    @Test fun layoutVisibleAttributesHaveNoDemo() {
        val dir = src("res/layout")
        val attr = Regex("""android:(text|hint|contentDescription|title)\s*=\s*"([^"]*)"""")
        val bad = dir.listFiles()!!.filter { it.extension == "xml" }.flatMap { f ->
            attr.findAll(f.readText()).map { f.name + ": " + it.groupValues[2] }.filter { demo.containsMatchIn(it.substringAfter(": ")) }.toList()
        }
        assertTrue("visible 'demo' in layouts: $bad", bad.isEmpty())
    }

    @Test fun helpLineIsV11() = assertTrue(
        src("res/layout/activity_main.xml").readText().contains("Historic is featured missions and past flights.")
    )

    @Test fun stringResourcesHaveNoDemo() {
        val values = src("res").listFiles()!!.filter { it.name.startsWith("values") }
        val body = Regex("""<string[^>]*>([^<]*)</string>""")
        val bad = values.flatMap { d -> d.listFiles()!!.filter { it.extension == "xml" } }.flatMap { f ->
            body.findAll(f.readText()).map { it.groupValues[1] }.filter { demo.containsMatchIn(it) }.toList()
        }
        assertTrue("visible 'demo' in strings: $bad", bad.isEmpty())
    }

    @Test fun pickerTagsHaveNoDemo() {
        assertEquals("[INCLUDED] ", PickerTag.INCLUDED)
        assertEquals("[PRO] ", PickerTag.PRO)
        val main = src("java/com/ccos/retro/ui/MainActivity.kt").readText()
        assertFalse("[DEMO] tag still in MainActivity", main.contains("\"[DEMO]", ignoreCase = true))
    }

    @Test fun jaxaJokeHasNoDemo() {
        val fe = src("java/com/ccos/retro/event/FlightEvents.kt").readText()
        assertFalse(fe.contains("technology demo", ignoreCase = true))
        assertTrue(fe.contains("\"Listed as a technology test. The test is how to be on time.\""))
    }

    @Test fun oneTimePurchaseCopyHasNoSubscriptionWords() {
        val pro = src("res/values/strings_pro.xml").readText()
        val body = Regex("""<string[^>]*>([^<]*)</string>""").findAll(pro).map { it.groupValues[1] }.toList()
        assertTrue(body.contains("One-time purchase. You confirm in Google Play."))
        assertTrue(body.contains("LRT Pro · Unlocked"))
        assertFalse(body.contains("LRT Pro · Active"))
        val sub = Regex("renew|monthly|yearly|trial|subscri|cancel anytime|cancel-anytime|per month|/mo", RegexOption.IGNORE_CASE)
        val bad = body.filter { sub.containsMatchIn(it) }
        assertTrue("subscription wording: $bad", bad.isEmpty())
    }
}
