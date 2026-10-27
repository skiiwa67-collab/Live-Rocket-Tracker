package com.ccos.retro.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JokeLinesKeyTest {
    private val newEn = "Listed as a technology test. The test is how to be on time."

    @Test fun renamedJaxaKeyStillResolvesJa() =
        assertEquals("技術実証と書いてある。実証しているのは時間を守ること。 / $newEn", JokeLines.beside("ja", newEn))

    @Test fun oldKeyIsGone() {
        val old = "Listed as technology demo. The demo is how to be on time."
        assertEquals(old, JokeLines.beside("ja", old))
    }

    @Test fun flightEventsLineMatchesTheJokeKey() {
        val f = listOf(File("src/main/java/com/ccos/retro/event/FlightEvents.kt"), File("app/src/main/java/com/ccos/retro/event/FlightEvents.kt")).first { it.exists() }
        assertTrue("FlightEvents must use the exact JokeLines key", f.readText().replace("\r\n", "\n").contains("\"$newEn\""))
        assertTrue(JokeLines.beside("ja", newEn) != newEn)
    }
}
