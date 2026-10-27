package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * R-LRT-FREE-PRO free sample list: Starship Flight 13 + one real past launch per country that has complete data.
 * Chris 8:22: samples are ALWAYS real launches. A country without a complete, unbroken real tape is skipped,
 * never faked. Everything else that is past/missed is Pro.
 */
object DemoSet {
    const val STARSHIP_F13_DEMO_ID = "demo-starship-f13"

    data class CountryDemo(val country: String, val demoId: String?, val note: String = "")

    val COUNTRY_DEMOS: List<CountryDemo> = listOf(
        CountryDemo("US", null, "Starship Flight 13 is the US sample (published SpaceX tape)"),
        CountryDemo("CN", null, "skipped: no complete real tape bundled (Long March 5 TJSW-25)"),
        CountryDemo("RU", null, "skipped: no complete real tape bundled (Soyuz MS-29)"),
        CountryDemo("NZ", null, "skipped: no complete real tape bundled (Electron)"),
        CountryDemo("EU", null, "skipped: no complete real tape bundled (Ariane 62 MTG-I2)"),
        CountryDemo("JP", null, "skipped: no complete real tape bundled (H3-24 MMX)"),
        CountryDemo("IN", null, "skipped: no complete real tape bundled (LVM3 Chandrayaan-3)")
    )

    val FREE_DEMO_IDS: Set<String> =
        (listOf(STARSHIP_F13_DEMO_ID) + COUNTRY_DEMOS.mapNotNull { it.demoId }).toSet()

    private val FLIGHT_13 = Regex("""\bFlight\s*13\b""", RegexOption.IGNORE_CASE)

    /** Real LL2 Starship Flight 13 (not the synthetic demo). */
    fun isRealStarshipF13(l: LaunchSnapshot): Boolean =
        !l.id.startsWith("demo-") &&
            l.rocketName.contains("Starship", ignoreCase = true) &&
            (FLIGHT_13.containsMatchIn(l.missionName) || FLIGHT_13.containsMatchIn(l.name))

    /** F13 entry: real replay when LL2 data exists in [pool], else the built-in demo. */
    fun starshipF13(pool: List<LaunchSnapshot>, builtIn: LaunchSnapshot?): LaunchSnapshot? =
        pool.firstOrNull { isRealStarshipF13(it) } ?: builtIn

    fun isFreeDemo(l: LaunchSnapshot): Boolean = l.id in FREE_DEMO_IDS || isRealStarshipF13(l)
}
