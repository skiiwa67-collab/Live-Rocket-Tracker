package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * R-LRT-FREE-PRO free demo list: Starship Flight 13 + one replay per country that has data.
 * Everything else that is past/missed is Pro.
 */
object DemoSet {
    const val STARSHIP_F13_DEMO_ID = "demo-starship-f13"

    data class CountryDemo(val country: String, val demoId: String?, val note: String = "")

    val COUNTRY_DEMOS: List<CountryDemo> = listOf(
        CountryDemo("US", "demo-nasa-sls"),
        CountryDemo("CN", "demo-casc-lm"),
        CountryDemo("RU", "demo-soyuz"),
        CountryDemo("NZ", "demo-electron-inflight", "Electron counts as NZ (Chris 10-27)"),
        // Chris 10-27: one demo per country; existing synthetic demos from LaunchDataProvider.buildDemoCatalog.
        CountryDemo("EU", "demo-esa-ariane"),
        CountryDemo("JP", "demo-jaxa-h3"),
        CountryDemo("IN", "demo-isro-lvm3")
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
