package com.ccos.retro.event

import com.ccos.retro.data.LaunchSnapshot

/**
 * Aft-view engine cluster. One enum, used by wallpaper AND MCC.
 * merlin27 is three octawebs. Never a concentric 27-ring.
 * Unknown: do not invent a ring.
 */
enum class EnginePattern {
    MERLIN9,
    MERLIN27,
    MERLIN_VAC,
    RAPTOR33,
    RAPTOR6,
    RS25_4,
    RL10,
    BE4_7,
    BE4_2,
    KOROLEV,
    KOROLEV_UPPER,
    ELECTRON9,
    ARIANE,
    LM5,
    LE9_2,
    GLENN7,
    CHAMBER4,
    H3_2,
    VACUUM1,
    SOLIDS,
    REAVER4,
    PROTON6,
    RD180_2,
    VIKAS2,
    VIKAS4,
    UNKNOWN
}

/**
 * Data-driven vehicle book. Add a [VehicleSpec] when a new rocket shows up.
 * Drawing still keys off [VehicleSpec.family]. Change numbers here, not in the views.
 * Starship 14 is a new row (or the same row until the hardware actually changes).
 */
data class VehicleSpec(
    val id: String,
    val family: String,
    val tokens: List<String>,
    val s1Engines: Int,
    val s2Engines: Int,
    val recoverable: Boolean,
    val methalox: Boolean,
    val fuelName: String,
    val oxName: String = "LOX",
    val boostbackLit: Int,
    val landingLit: Int,
    val boostbackSec: Float = 32f,
    val landingBurnSec: Float = 22f,
    val residual: Float = 0.08f,
    val engineName: String,
    val s1Thrust: String,
    val s2Thrust: String,
    val ispVac: String,
    val mixRatio: String,
    val chamberBar: String,
    val s1Dry: String,
    val s1Prop: String,
    val nerdNote: String,
    /** False: do not invent a drawing or numbers. Show UPDATE REQUIRED. */
    val verified: Boolean = true,
    /** Silhouette key. Zhuque-3 is Falcon-shaped, not Long March. */
    val drawFamily: String = family,
    val s1Pattern: EnginePattern = EnginePattern.UNKNOWN,
    val s2Pattern: EnginePattern = EnginePattern.UNKNOWN,
    val s2Dry: String = "—",
    val s2Prop: String = "—",
    val s1Isp: String = "—",
    val s2Isp: String = "",
    val s1Name: String = "",
    val s2Name: String = "",
    val s2EngineName: String = ""
) {
    fun pattern(stage: Int): EnginePattern = if (stage >= 2) s2Pattern else s1Pattern
    fun stageName(stage: Int, fallback: String): String {
        val named = if (stage >= 2) s2Name else s1Name
        return named.ifBlank { fallback }
    }
    fun dry(stage: Int): String = if (stage >= 2) s2Dry else s1Dry
    fun prop(stage: Int): String = if (stage >= 2) s2Prop else s1Prop
    fun isp(stage: Int): String {
        val own = if (stage >= 2) s2Isp else s1Isp
        return own.ifBlank { ispVac }
    }
    fun engineLabel(stage: Int): String =
        if (stage >= 2) s2EngineName.ifBlank { engineName } else engineName
}

object VehicleCatalog {

    private val GENERIC = VehicleSpec(
        id = "generic",
        family = "generic",
        tokens = emptyList(),
        s1Engines = 9,
        s2Engines = 1,
        recoverable = false,
        methalox = false,
        fuelName = "RP",
        boostbackLit = 0,
        landingLit = 0,
        engineName = "UNKNOWN",
        s1Thrust = "—",
        s2Thrust = "—",
        ispVac = "—",
        mixRatio = "—",
        chamberBar = "—",
        s1Dry = "—",
        s1Prop = "—",
        nerdNote = "Not in the vehicle book. Drawing and numbers ship in a later update. We will not invent them.",
        verified = false,
        drawFamily = "generic",
        s1Pattern = EnginePattern.UNKNOWN,
        s2Pattern = EnginePattern.UNKNOWN
    )

    /**
     * First match wins. Put more specific tokens (Falcon Heavy, Long March 5) first.
     */
    val all: List<VehicleSpec> = listOf(
        VehicleSpec(
            id = "starship",
            family = "starship",
            tokens = listOf("starship", "super heavy"),
            s1Engines = 33,
            s2Engines = 6,
            recoverable = true,
            methalox = true,
            fuelName = "CH4",
            boostbackLit = 13,
            landingLit = 13,
            boostbackSec = 32f,
            landingBurnSec = 22f,
            engineName = "RAPTOR SL",
            s1Thrust = "74 MN SL",
            s2Thrust = "15 MN VAC",
            ispVac = "~350–380 s VAC",
            mixRatio = "3.6 O/F EST",
            chamberBar = "300 BAR EST",
            s1Dry = "~275 t EST",
            s1Prop = "~3400 t",
            nerdNote = "Full-flow staged combustion. Best part is no part. Update this row when Raptor changes.",
            s1Pattern = EnginePattern.RAPTOR33,
            s2Pattern = EnginePattern.RAPTOR6,
            s2Dry = "~85–126 t EST",
            s2Prop = "~1200–1500 t EST",
            s1Isp = "~327 s SL EST",
            s2Isp = "~350–380 s VAC",
            s1Name = "SUPER HEAVY",
            s2Name = "STARSHIP",
            s2EngineName = "RAPTOR 3 SL + 3 VAC"
        ),
        VehicleSpec(
            id = "fh",
            family = "fh",
            tokens = listOf("falcon heavy"),
            s1Engines = 27,
            s2Engines = 1,
            recoverable = true,
            methalox = false,
            fuelName = "RP",
            boostbackLit = 3,
            landingLit = 3,
            engineName = "MERLIN 1D",
            s1Thrust = "22.8 MN SL",
            s2Thrust = "934 kN VAC",
            ispVac = "348 s",
            mixRatio = "2.36 O/F",
            chamberBar = "97 BAR",
            s1Dry = "3× ~26 t",
            s1Prop = "3× ~400 t",
            nerdNote = "Three cores, one vacuum Merlin. Side boosters go home. Center core has a worse day.",
            s1Pattern = EnginePattern.MERLIN27,
            s2Pattern = EnginePattern.MERLIN_VAC
        ),
        VehicleSpec(
            id = "f9",
            family = "f9",
            tokens = listOf("falcon"),
            s1Engines = 9,
            s2Engines = 1,
            recoverable = true,
            methalox = false,
            fuelName = "RP",
            boostbackLit = 3,
            landingLit = 3,
            engineName = "MERLIN 1D",
            s1Thrust = "7.6 MN SL",
            s2Thrust = "934 kN VAC",
            ispVac = "348 s",
            mixRatio = "2.36 O/F",
            chamberBar = "97 BAR",
            s1Dry = "~26 t",
            s1Prop = "~400 t",
            nerdNote = "Octaweb. Center engine for landing. The workhorse that paid for the steel one.",
            s1Pattern = EnginePattern.MERLIN9,
            s2Pattern = EnginePattern.MERLIN_VAC
        ),
        VehicleSpec(
            id = "glenn",
            family = "glenn",
            tokens = listOf("new glenn"),
            s1Engines = 7,
            s2Engines = 2,
            recoverable = true,
            methalox = true,
            fuelName = "CH4",
            boostbackLit = 7,
            landingLit = 7,
            engineName = "BE-4 / BE-3U",
            s1Thrust = "17 MN SL",
            s2Thrust = "1.6 MN VAC",
            ispVac = "445 s",
            mixRatio = "3.5 O/F",
            chamberBar = "134 BAR",
            s1Dry = "~70 t",
            s1Prop = "~1000 t",
            nerdNote = "Oxygen-rich staged combustion. The hat has a kick stage. Do not sit on it.",
            s1Pattern = EnginePattern.BE4_7,
            s2Pattern = EnginePattern.BE4_2
        ),
        VehicleSpec(
            id = "vulcan",
            family = "vulcan",
            tokens = listOf("vulcan centaur", "vulcan"),
            s1Engines = 2,
            s2Engines = 2,
            recoverable = true,
            methalox = true,
            fuelName = "CH4",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "BE-4 / RL10",
            s1Thrust = "4.9 MN SL",
            s2Thrust = "200 kN VAC",
            ispVac = "460 s",
            mixRatio = "—",
            chamberBar = "—",
            s1Dry = "—",
            s1Prop = "—",
            nerdNote = "Two BE-4. Centaur V. Solids are solids: no liquid bells on the ENG plate.",
            verified = true,
            drawFamily = "vulcan",
            s1Pattern = EnginePattern.BE4_2,
            s2Pattern = EnginePattern.RL10
        ),
        VehicleSpec(
            id = "sls",
            family = "sls",
            tokens = listOf("sls", "space launch system", "artemis"),
            s1Engines = 4,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "LH2",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "RS-25 / RL10",
            s1Thrust = "7.4 MN + SRB",
            s2Thrust = "110 kN VAC",
            ispVac = "452 s",
            mixRatio = "6.0 O/F",
            chamberBar = "206 BAR",
            s1Dry = "~85 t core",
            s1Prop = "~980 t",
            nerdNote = "Shuttle leftovers at a Saturn price. The solids do the talking.",
            s1Pattern = EnginePattern.RS25_4,
            s2Pattern = EnginePattern.RL10
        ),
        VehicleSpec(
            id = "soyuz",
            family = "soyuz",
            tokens = listOf("soyuz"),
            s1Engines = 20,
            s2Engines = 4,
            recoverable = false,
            methalox = false,
            fuelName = "RP",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "RD-107 / RD-0110",
            s1Thrust = "4.1 MN SL",
            s2Thrust = "298 kN VAC",
            ispVac = "326 s",
            mixRatio = "2.6 O/F",
            chamberBar = "60 BAR",
            s1Dry = "~30 t",
            s1Prop = "~120 t",
            nerdNote = "Korolev cross. Four boosters, one core. If it looks 1960s, that is because it works.",
            s1Pattern = EnginePattern.KOROLEV,
            s2Pattern = EnginePattern.KOROLEV_UPPER
        ),
        VehicleSpec(
            id = "electron",
            family = "electron",
            tokens = listOf("electron"),
            s1Engines = 9,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "RP",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "RUTHERFORD SL",
            s1Thrust = "9× SL",
            s2Thrust = "5800 lbf VAC",
            ispVac = "343 s",
            mixRatio = "2.4 O/F",
            chamberBar = "—",
            s1Dry = "~1.2 t",
            s1Prop = "~9 t",
            nerdNote = "9 Rutherford sea-level + 1 Rutherford vacuum. LOX/RP-1 electric-pump-fed. Smithsonian vac ~5800 lbf, Isp 343 s.",
            s1Pattern = EnginePattern.ELECTRON9,
            s2Pattern = EnginePattern.VACUUM1,
            s1Isp = "—",
            s2Isp = "343 s",
            s1Name = "STAGE 1",
            s2Name = "STAGE 2",
            s2EngineName = "RUTHERFORD VAC"
        ),
        VehicleSpec(
            id = "ariane",
            family = "ariane",
            tokens = listOf("ariane"),
            s1Engines = 1,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "LH2",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "VULCAIN / VINCI",
            s1Thrust = "1.4 MN + SRB",
            s2Thrust = "180 kN VAC",
            ispVac = "457 s",
            mixRatio = "6.0 O/F",
            chamberBar = "118 BAR",
            s1Dry = "~55 t",
            s1Prop = "~180 t",
            nerdNote = "Kourou. Solids have no liquid bells on the ENG plate. One Vulcain.",
            s1Pattern = EnginePattern.ARIANE,
            s2Pattern = EnginePattern.RL10
        ),
        VehicleSpec(
            id = "lm5",
            family = "lm5",
            tokens = listOf("long march 5", "cz-5", "cz5"),
            s1Engines = 10,
            s2Engines = 2,
            recoverable = false,
            methalox = false,
            fuelName = "LH2",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "YF-77 / YF-75D",
            s1Thrust = "10.6 MN SL",
            s2Thrust = "176 kN VAC",
            ispVac = "442 s",
            mixRatio = "6.0 O/F",
            chamberBar = "—",
            s1Dry = "~18 t core",
            s1Prop = "~170 t",
            nerdNote = "China's heavy. Four boosters, hydrolox core. The weather sat has a good memory.",
            s1Pattern = EnginePattern.LM5,
            s2Pattern = EnginePattern.BE4_2
        ),
        VehicleSpec(
            id = "lm",
            family = "lm",
            tokens = listOf("long march", "cz-"),
            s1Engines = 4,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "UDMH",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "YF-100 / YF-20",
            s1Thrust = "—",
            s2Thrust = "—",
            ispVac = "—",
            mixRatio = "—",
            chamberBar = "—",
            s1Dry = "—",
            s1Prop = "—",
            nerdNote = "Long March catch-all. Specific CZ variants get their own row. We will not invent a drawing.",
            verified = false,
            drawFamily = "generic",
            s1Pattern = EnginePattern.UNKNOWN,
            s2Pattern = EnginePattern.UNKNOWN
        ),
        VehicleSpec(
            id = "isro",
            family = "isro",
            tokens = listOf("pslv"),
            s1Engines = 0,
            s2Engines = 0,
            recoverable = false,
            methalox = false,
            fuelName = "HTPB",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "UNKNOWN",
            s1Thrust = "—",
            s2Thrust = "—",
            ispVac = "—",
            mixRatio = "—",
            chamberBar = "—",
            s1Dry = "—",
            s1Prop = "—",
            nerdNote = "PSLV is not in the book yet. We will not invent a drawing.",
            verified = false,
            drawFamily = "generic",
            s1Pattern = EnginePattern.UNKNOWN,
            s2Pattern = EnginePattern.UNKNOWN
        ),
        VehicleSpec(
            id = "zq3",
            family = "zq",
            tokens = listOf("zhuque-3", "zhuque 3", "zq-3", "zq3"),
            s1Engines = 9,
            s2Engines = 1,
            recoverable = true,
            methalox = true,
            fuelName = "CH4",
            boostbackLit = 3,
            landingLit = 1,
            engineName = "TQ-12A / TQ-15A",
            s1Thrust = "7.2 MN SL",
            s2Thrust = "944 kN VAC",
            ispVac = "—",
            mixRatio = "—",
            chamberBar = "—",
            s1Dry = "—",
            s1Prop = "~550 t GROSS",
            nerdNote = "Stainless methalox. 9×TQ-12A + 1×TQ-15A. Grid fins and legs. Looks like Falcon 9 because the physics is the same. Y2 recovered Minqin ~T+8.",
            verified = true,
            drawFamily = "f9",
            s1Pattern = EnginePattern.MERLIN9,
            s2Pattern = EnginePattern.MERLIN_VAC
        ),
        VehicleSpec(
            id = "h3",
            family = "h3",
            tokens = listOf("h3", "h-3"),
            s1Engines = 2,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "LH2",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "LE-9 / LE-5B",
            s1Thrust = "2.9 MN + SRB",
            s2Thrust = "137 kN VAC",
            ispVac = "448 s",
            mixRatio = "5.9 O/F",
            chamberBar = "—",
            s1Dry = "—",
            s1Prop = "—",
            nerdNote = "Tanegashima. Two LE-9. Solids have no liquid bells on the ENG plate.",
            s1Pattern = EnginePattern.LE9_2,
            s2Pattern = EnginePattern.RL10
        ),
        VehicleSpec(
            id = "lvm3",
            family = "lvm3",
            tokens = listOf("lvm3", "lvm-3", "gslv mk iii", "gslv mk-iii", "gslv-mk3", "gslv mk3"),
            s1Engines = 2,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "UH25",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "VIKAS / CE-20",
            s1Thrust = "1.6 MN + S200",
            s2Thrust = "186 kN VAC",
            ispVac = "443 s",
            mixRatio = "-",
            chamberBar = "-",
            s1Dry = "-",
            s1Prop = "-",
            nerdNote = "LVM3. Two S200 solids, L110 with 2 Vikas, C25 with CE-20. Solids are not liquid bells.",
            verified = true,
            drawFamily = "lvm3",
            s1Pattern = EnginePattern.VIKAS2,
            s2Pattern = EnginePattern.VACUUM1
        ),
        VehicleSpec(
            id = "gslv2",
            family = "gslv2",
            tokens = listOf(
                "gslv mk ii", "gslv mk-ii", "gslv-mk2", "gslv mk2",
                "gslv-f", "gisat", "gslv"
            ),
            s1Engines = 4,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "HTPB",
            oxName = "N2O4",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "VIKAS L40",
            s1Thrust = "4800 kN S139",
            s2Thrust = "846 kN",
            ispVac = "—",
            mixRatio = "—",
            chamberBar = "—",
            s1Dry = "—",
            s1Prop = "420 t GLOW",
            nerdNote = "ISRO GSLV Mk II (isro.gov.in/GSLV_CON). 3 stages, 51.73 m ogive PLF, 420 t. GS1 S139 HTPB solid + 4 L40 Vikas liquid strap-ons. GS2 1 Vikas UH25/N2O4 846 kN. CUS CE-7.5 LOX/LH2 75 kN. Solid core is not a liquid bell.",
            verified = true,
            drawFamily = "gslv2",
            s1Pattern = EnginePattern.VIKAS4,
            s2Pattern = EnginePattern.VACUUM1,
            s1Name = "GS1 S139 + L40",
            s2Name = "GS2 / CUS",
            s2EngineName = "VIKAS / CE-7.5"
        ),
        VehicleSpec(
            id = "atlas",
            family = "atlas",
            tokens = listOf("atlas v", "atlas-v", "atlas 5"),
            s1Engines = 2,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "RP",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "RD-180 / RL10",
            s1Thrust = "3.8 MN SL",
            s2Thrust = "99 kN VAC",
            ispVac = "450 s",
            mixRatio = "2.72 O/F",
            chamberBar = "257 BAR",
            s1Dry = "-",
            s1Prop = "-",
            nerdNote = "Atlas V. RD-180 is two chambers. SRBs are not liquid bells.",
            verified = true,
            drawFamily = "atlas",
            s1Pattern = EnginePattern.RD180_2,
            s2Pattern = EnginePattern.VACUUM1
        ),
        VehicleSpec(
            id = "firefly",
            family = "firefly",
            tokens = listOf("firefly alpha", "firefly"),
            s1Engines = 4,
            s2Engines = 1,
            recoverable = false,
            methalox = false,
            fuelName = "RP",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "REAVER / LIGHTNING",
            s1Thrust = "736 kN SL",
            s2Thrust = "70 kN VAC",
            ispVac = "322 s",
            mixRatio = "-",
            chamberBar = "-",
            s1Dry = "-",
            s1Prop = "-",
            nerdNote = "Firefly Alpha. Four Reaver in a 2x2, one Lightning vacuum.",
            verified = true,
            drawFamily = "firefly",
            s1Pattern = EnginePattern.REAVER4,
            s2Pattern = EnginePattern.VACUUM1
        ),
        VehicleSpec(
            id = "proton",
            family = "proton",
            tokens = listOf("proton"),
            s1Engines = 6,
            s2Engines = 4,
            recoverable = false,
            methalox = false,
            fuelName = "UDMH",
            boostbackLit = 0,
            landingLit = 0,
            engineName = "RD-275 / RD-0210",
            s1Thrust = "10 MN SL",
            s2Thrust = "2.4 MN VAC",
            ispVac = "327 s",
            mixRatio = "-",
            chamberBar = "-",
            s1Dry = "-",
            s1Prop = "-",
            nerdNote = "Proton-M. Six RD-275 around the core. Stage 2 is four RD-0210.",
            verified = true,
            drawFamily = "proton",
            s1Pattern = EnginePattern.PROTON6,
            s2Pattern = EnginePattern.CHAMBER4
        )
    )

    fun blob(launch: LaunchSnapshot?): String =
        "${launch?.rocketName.orEmpty()} ${launch?.name.orEmpty()} ${launch?.missionName.orEmpty()} ${launch?.provider.orEmpty()}".lowercase()

    fun spec(launch: LaunchSnapshot?): VehicleSpec {
        val n = blob(launch)
        if (n.isBlank()) return GENERIC
        return all.firstOrNull { s -> s.tokens.any { it in n } } ?: GENERIC
    }

    fun family(launch: LaunchSnapshot?): String = spec(launch).family

    fun drawFamily(launch: LaunchSnapshot?): String = spec(launch).drawFamily

    /** Short rocket on the HUD map. Not the mission string. */
    fun hudRocket(launch: LaunchSnapshot?): String {
        val s = spec(launch)
        return when (s.family) {
            "gslv2" -> "GSLV Mk II"
            "lvm3" -> "LVM3"
            "electron" -> "Electron"
            else -> launch?.rocketName?.trim()?.take(16).orEmpty().ifBlank { "—" }
        }
    }

    fun isVerified(launch: LaunchSnapshot?): Boolean = spec(launch).verified

    fun needsUpdate(launch: LaunchSnapshot?): Boolean = !isVerified(launch)

    const val UPDATE_HEAD = "NEW VEHICLE · DATA UPDATE REQUIRED"
    const val UPDATE_BODY = "This rocket is not in the vehicle book yet. Drawing and numbers ship in a later update. We will not invent them."

    fun engines(launch: LaunchSnapshot?, stage: Int): Int {
        val s = spec(launch)
        return if (stage >= 2) s.s2Engines else s.s1Engines
    }

    fun enginePattern(launch: LaunchSnapshot?, stage: Int): EnginePattern {
        val s = spec(launch)
        if (!s.verified) return EnginePattern.UNKNOWN
        return if (stage >= 2) s.s2Pattern else s.s1Pattern
    }

    fun landingLitFor(launch: LaunchSnapshot?): Int {
        val s = spec(launch)
        return if (MissionFacts.isFlight13(launch)) 10 else s.landingLit
    }

    /** Verified recoverable. GENERIC (verified=false) is unknown, not expended. */
    fun isKnownRecoverable(launch: LaunchSnapshot?): Boolean {
        val s = spec(launch)
        return s.verified && s.recoverable
    }

    /** Verified expendable. recoverable=false + verified=false is unknown. */
    fun isKnownExpendable(launch: LaunchSnapshot?): Boolean {
        val s = spec(launch)
        return s.verified && !s.recoverable
    }

    const val UNKNOWN_RECOVERY = "UNKNOWN · DATA UPDATE REQUIRED"
}
