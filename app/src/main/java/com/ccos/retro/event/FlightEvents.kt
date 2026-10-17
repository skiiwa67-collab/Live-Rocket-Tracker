package com.ccos.retro.event

import com.ccos.retro.data.LaunchSnapshot
import com.ccos.retro.geo.GeoDraw
import com.ccos.retro.geo.PadBook
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class EventSeverity { INFO, WATCH, FAIL }

data class FlightEvent(
    val id: String,
    val tSec: Float,
    val title: String,
    val detail: String,
    val severity: EventSeverity,
    val autoVideo: Boolean = false,
    val failedSystem: String? = null
)

/**
 * Modular timeline. Add a family function when a new vehicle shows up.
 * One-way expendables share [oneWay]. Starship / Falcon add recovery events.
 */
object FlightEventCatalog {

    fun family(launch: LaunchSnapshot?): String = VehicleCatalog.family(launch)

    fun timeline(launch: LaunchSnapshot?): List<FlightEvent> {
        val base = when {
            VehicleCatalog.needsUpdate(launch) -> unknownVehicle()
            else -> when (family(launch)) {
                "starship" -> if (MissionFacts.isFlight13(launch)) starshipFlight13() else starship()
                "f9", "fh" -> falcon(launch)
                "sls" -> sls()
                "soyuz" -> soyuz()
                "electron" -> electron()
                "zq" -> zhuque(launch)
                "generic" -> unknownVehicle()
                else -> oneWay()
            }
        }
        // Only the demo has a scripted FAIL time. Live never invents T.
        if (launch?.id == "demo-spacex-fail") {
            return (base + demoFail(launch)).sortedBy { it.tSec }
        }
        return base
    }

    /**
     * FAIL is a live observation, not a planned T.
     * Demo: scripted T+70 so we can test the path.
     * Real launch: only when LL2 status already says it failed, and never before liftoff.
     */
    fun failureFromStatus(launch: LaunchSnapshot?, tSec: Float = 0f): FlightEvent? {
        if (launch == null || tSec < 0f) return null
        if (launch.id == "demo-spacex-fail") {
            return if (tSec >= 70f) demoFail(launch) else null
        }
        if (!isFailBlob(failBlob(launch))) return null
        return liveFail(launch, tSec)
    }

    private fun failBlob(launch: LaunchSnapshot): String =
        "${launch.statusName} ${launch.statusAbbrev} ${launch.holdReason.orEmpty()}".lowercase()

    private fun isFailBlob(blob: String): Boolean =
        "fail" in blob || "rud" in blob || "anomaly" in blob ||
            "exploded" in blob || "lost" in blob || blob.contains("partial")

    private fun systemFromBlob(blob: String): String = when {
        "engine" in blob -> "S1 ENGINES"
        "stage" in blob -> "STAGE"
        "rud" in blob || "explod" in blob -> "VEHICLE (RUD)"
        else -> "VEHICLE"
    }

    private fun demoFail(launch: LaunchSnapshot) = FlightEvent(
        id = "fail-${launch.id}",
        tSec = 70f,
        title = "MISSION FAILURE",
        detail = "VEHICLE — telemetry lost after first-stage anomaly",
        severity = EventSeverity.FAIL,
        autoVideo = true,
        failedSystem = "VEHICLE"
    )

    private fun liveFail(launch: LaunchSnapshot, tSec: Float): FlightEvent {
        val blob = failBlob(launch)
        val system = systemFromBlob(blob)
        return FlightEvent(
            id = "fail-${launch.id}",
            tSec = tSec,
            title = "MISSION FAILURE",
            detail = system + " — " + (launch.holdReason ?: launch.statusName).take(48),
            severity = EventSeverity.FAIL,
            autoVideo = true,
            failedSystem = system
        )
    }

    private fun ev(
        id: String,
        t: Float,
        title: String,
        detail: String,
        sev: EventSeverity = EventSeverity.INFO
    ) = FlightEvent(id, t, title, detail, sev)

    private fun oneWay() = listOf(
        ev("ow_lift", 0f, "LIFTOFF", "Vehicle cleared the pad"),
        ev("ow_maxq", 70f, "MAX-Q", "Peak aerodynamic pressure"),
        ev("ow_meco", 155f, "MECO", "Main engine cutoff"),
        ev("ow_sep", 160f, "STAGE SEP", "Upper stage is free"),
        ev("ow_seco", 540f, "SECO", "Orbital insertion burn complete"),
        ev("ow_dep_app", 620f, "APPROACHING PAYLOAD DEPLOYMENT", "Fairing / payload sequence", EventSeverity.WATCH),
        ev("ow_dep", 640f, "PAYLOAD DEPLOYMENT", "Payload is away")
    )

    private fun falcon(launch: LaunchSnapshot?) : List<FlightEvent> {
        val starlink = MissionFacts.blob(launch).contains("starlink")
        val deploy = if (starlink) 3690f else 620f
        val depApp = deploy - 90f
        return listOf(
            ev("f9_lift", 0f, "LIFTOFF", "Falcon cleared the pad"),
            ev("f9_maxq", 66f, "MAX-Q", "Peak aerodynamic pressure"),
            ev("f9_meco", 150f, "MECO", "First-stage main engine cutoff"),
            ev("f9_sep", 154f, "STAGE SEP", "Booster is free"),
            ev("f9_ses1", 165f, "SES-1", "Second-stage ignition"),
            ev("f9_entry", 400f, "ENTRY BURN", "Booster entry burn"),
            ev("f9_land_app", 440f, "APPROACHING BOOSTER LANDING", "Droneship or LZ", EventSeverity.WATCH),
            ev("f9_lb", 458f, "LANDING BURN", "Center Merlin. EST family tape", EventSeverity.WATCH),
            ev("f9_land", 480f, "BOOSTER TOUCHDOWN", "First stage at the droneship or LZ"),
            ev("f9_seco", 510f, "SECO-1", "Second-stage cutoff"),
            ev("f9_dep_app", depApp, "APPROACHING PAYLOAD DEPLOYMENT", if (starlink) "Starlink stack" else "Deployment sequence", EventSeverity.WATCH),
            ev("f9_dep", deploy, "PAYLOAD DEPLOYMENT", if (starlink) "Starlinks are away" else "Payload is away")
        )
    }

    private fun unknownVehicle() = listOf(
        ev("unk_lift", 0f, "LIFTOFF", "Vehicle left the pad"),
        ev(
            "unk_book",
            8f,
            VehicleCatalog.UPDATE_HEAD,
            VehicleCatalog.UPDATE_BODY,
            EventSeverity.WATCH
        )
    )

    /** Published Y2: sep 137s, Minqin landing ~T+8. Landing burn is T+8 minus catalog 22s. */
    private fun zhuque(launch: LaunchSnapshot?): List<FlightEvent> {
        return listOf(
            ev("zq_lift", 0f, "LIFTOFF", "Zhuque-3 cleared Jiuquan"),
            ev("zq_sep", 137f, "STAGE SEP", "First stage is free"),
            ev("zq_land_app", 440f, "APPROACHING BOOSTER LANDING", "Minqin pad, Gansu", EventSeverity.WATCH),
            ev("zq_lb", 458f, "LANDING BURN", "Center TQ-12A. Minqin", EventSeverity.WATCH),
            ev("zq_land", 480f, "BOOSTER TOUCHDOWN", "Landing legs. Minqin County, ~390 km downrange")
        )
    }

    private fun starshipFlight13() = listOf(
        ev("ss13_lift", 0f, "LIFTOFF", "Super Heavy / Starship stack"),
        ev("ss13_maxq", 60f, "MAX-Q", "Peak aerodynamic pressure"),
        ev("ss13_meco", 165f, "BOOSTER MECO", "Super Heavy cutoff"),
        ev("ss13_hot", 170f, "HOT-STAGE", "Ship engines lighting on the stack"),
        ev("ss13_sep", 175f, "STAGE SEP", "Ship is free"),
        ev("ss13_bb", 210f, "BOOSTBACK", "Inner 13 Raptors for the flip"),
        ev("ss13_ent", 340f, "BOOSTER ENTRY", "Super Heavy into the Gulf corridor"),
        ev("ss13_lb", 388f, "LANDING BURN", "Only a subset of 13 relight", EventSeverity.WATCH),
        ev("ss13_splash", 410f, "BOOSTER SPLASH", "Gulf of Mexico — vehicle destroyed"),
        ev("ss13_seco", 540f, "SHIP CUTOFF", "Ascent burn complete"),
        ev("ss13_door", 720f, "PAYLOAD DOOR", "Pez bay opening", EventSeverity.WATCH),
        ev("ss13_dep", 780f, "STARLINK V3 DEPLOY", "20 next-gen sats away"),
        ev("ss13_relight", 1680f, "SHIP RELIGHT", "In-space Raptor restart"),
        ev("ss13_entry", 2700f, "REENTRY", "Entry interface. Tiles still dark"),
        ev("ss13_plasma", 2820f, "PLASMA", "Heatshield going incandescent", EventSeverity.WATCH),
        ev("ss13_flip", 3300f, "LANDING FLIP", "Three Raptors, then two, then one"),
        ev("ss13_ship", 3340f, "SHIP SPLASH", "Intact in the Indian Ocean")
    )

    private fun starship() = listOf(
        ev("ss_lift", 0f, "LIFTOFF", "Super Heavy / Starship stack"),
        ev("ss_maxq", 60f, "MAX-Q", "Peak aerodynamic pressure"),
        ev("ss_meco", 165f, "BOOSTER MECO", "Super Heavy cutoff"),
        ev("ss_hot", 170f, "HOT-STAGE", "Ship engines lighting on the stack"),
        ev("ss_sep", 175f, "STAGE SEP", "Ship is free"),
        ev("ss_boostback", 210f, "BOOSTBACK", "Super Heavy flipping for home"),
        ev("ss_entry_b", 340f, "BOOSTER ENTRY", "Super Heavy plasma / grid fins"),
        ev("ss_land_burn", 388f, "BOOSTER LANDING BURN", "Thirteen Raptors for the tower", EventSeverity.WATCH),
        ev("ss_catch", 410f, "BOOSTER TOUCHDOWN", "Super Heavy at the chopsticks"),
        ev("ss_seco", 540f, "SHIP CUTOFF", "Insertion complete"),
        ev("ss_dep_app", 1200f, "APPROACHING PAYLOAD DEPLOYMENT", "Door / deploy", EventSeverity.WATCH),
        ev("ss_dep", 1260f, "PAYLOAD DEPLOYMENT", "Payload is away"),
        ev("ss_entry", 2700f, "REENTRY", "Entry interface. Tiles still dark", EventSeverity.WATCH),
        ev("ss_plasma", 2820f, "PLASMA", "Heatshield going incandescent", EventSeverity.WATCH),
        ev("ss_flip", 3300f, "LANDING FLIP", "Ship rotating to engines-down", EventSeverity.WATCH),
        ev("ss_land", 3340f, "SHIP LAND", "Starship on the pad")
    )

    private fun sls() = listOf(
        ev("sls_lift", 0f, "LIFTOFF", "SLS cleared the pad"),
        ev("sls_maxq", 70f, "MAX-Q", "Peak aerodynamic pressure"),
        ev("sls_srb", 126f, "SRB SEP", "Solid boosters jettison"),
        ev("sls_meco", 480f, "MECO", "Core stage cutoff"),
        ev("sls_sep", 490f, "CORE SEP", "ICPS / upper stage is free"),
        ev("sls_seco", 900f, "SECO", "Upper-stage cutoff"),
        ev("sls_dep", 980f, "PAYLOAD DEPLOYMENT", "Orion / payload is away")
    )

    private fun soyuz() = listOf(
        ev("sz_lift", 0f, "ПУСК / LIFTOFF", "Soyuz cleared the pad"),
        ev("sz_maxq", 70f, "MAX-Q", "Peak aerodynamic pressure"),
        ev("sz_sep", 118f, "BOOSTER SEP", "Strap-ons jettison"),
        ev("sz_core", 287f, "CORE CUTOFF", "Core stage done"),
        ev("sz_orbit", 530f, "ORBIT", "Insertion")
    )

    private fun electron() = listOf(
        ev("el_lift", 0f, "LIFTOFF", "Electron cleared the pad"),
        ev("el_maxq", 70f, "MAX-Q", "Peak aerodynamic pressure"),
        ev("el_meco", 155f, "MECO", "First-stage cutoff"),
        ev("el_sep", 162f, "STAGE SEP", "Kick stage / upper is free"),
        ev("el_seco", 540f, "SECO", "Insertion"),
        ev("el_dep", 600f, "PAYLOAD DEPLOYMENT", "Payload is away")
    )
}

class FlightEventMonitor {
    private val fired = HashSet<String>()
    private var lastLaunchId: String? = null
    /** First T we observed a live LL2 fail. Null until status actually flips. */
    private var liveFailT: Float? = null

    fun reset() {
        fired.clear()
        lastLaunchId = null
        liveFailT = null
    }

    fun occurred(launch: LaunchSnapshot?, tSec: Float): List<FlightEvent> {
        val tape = FlightEventCatalog.timeline(launch)
            .filter { it.tSec <= tSec }
            .toMutableList()
        FlightEventCatalog.failureFromStatus(launch, tSec)?.let { fail ->
            val stamped = fail.copy(tSec = liveFailT ?: fail.tSec)
            if (tape.none { it.id == stamped.id }) tape.add(stamped)
        }
        return tape.sortedBy { it.tSec }
    }

    fun poll(launch: LaunchSnapshot?, tSec: Float): List<FlightEvent> {
        val id = launch?.id
        if (id != lastLaunchId) {
            fired.clear()
            lastLaunchId = id
            liveFailT = null
            for (e in FlightEventCatalog.timeline(launch)) {
                if (tSec > e.tSec + 4f) fired.add(e.id)
            }
        }
        val out = ArrayList<FlightEvent>()
        val live = FlightEventCatalog.failureFromStatus(launch, tSec)
        if (live != null) {
            if (liveFailT == null) {
                liveFailT = if (launch?.id == "demo-spacex-fail") 70f else tSec
            }
            val stamped = live.copy(tSec = liveFailT!!)
            if (fired.add(stamped.id)) out.add(stamped)
        } else {
            liveFailT = null
        }
        if (tSec < -2f) return out
        for (e in FlightEventCatalog.timeline(launch)) {
            if (tSec + 0.05f >= e.tSec && fired.add(e.id)) out.add(e)
        }
        return out
    }
}

data class MissionBrief(
    val title: String,
    val vehicle: String,
    val payloadName: String,
    val payloadKind: String,
    val payloadCount: String,
    val payloadState: String,
    val orbit: String,
    val objective: String,
    val classified: Boolean,
    val note: String,
    val booster: String?,
    val ship: String?,
    val site: String,
    val status: String
)

object MissionFacts {
    fun blob(launch: LaunchSnapshot?): String =
        "${launch?.name.orEmpty()} ${launch?.missionName.orEmpty()} ${launch?.rocketName.orEmpty()}".lowercase()

    fun isTestFlight(launch: LaunchSnapshot?): Boolean {
        val n = blob(launch)
        if ("starlink" in n && "starship" !in n) return false
        return "test flight" in n || "flight test" in n || "ift-" in n || "ift " in n ||
            "new shepard" in n || "hopper" in n || "qualification" in n ||
            ("starship" in n && "starlink" !in n)
    }

    fun goalLine(launch: LaunchSnapshot?): String {
        val n = blob(launch)
        return when {
            launch == null -> "GOAL  NO LOCK"
            VehicleCatalog.needsUpdate(launch) -> "GOAL  DATA UPDATE REQUIRED"
            isZhuque3(launch) -> "GOAL  ORBIT + BOOSTER LANDING"
            isTestFlight(launch) || isFlight13(launch) -> "GOAL  PERFORMANCE TEST"
            "starlink" in n -> "GOAL  ORBIT + SAT DEPLOY"
            "crew" in n || "astronaut" in n -> "GOAL  CREW TO ORBIT"
            "crs" in n || "cargo" in n -> "GOAL  CARGO TO ORBIT"
            "nrol" in n || "classified" in n -> "GOAL  CLASSIFIED"
            else -> "GOAL  ORBITAL INSERTION"
        }
    }

    fun isFlight13(launch: LaunchSnapshot?): Boolean {
        val n = blob(launch)
        return "flight 13" in n || "flight-13" in n || "ift-13" in n || "ift 13" in n
    }

    fun isZhuque3(launch: LaunchSnapshot?): Boolean {
        val n = blob(launch) + " " + VehicleCatalog.blob(launch)
        return "zhuque-3" in n || "zhuque 3" in n || "zq-3" in n || "zq3" in n
    }

    fun isSatMission(launch: LaunchSnapshot?): Boolean = true

    fun payloadLine(launch: LaunchSnapshot?): String? = brief(launch, 0f).payloadName

    fun boosterOutcome(launch: LaunchSnapshot?): String? = brief(launch, 0f).booster

    fun shipOutcome(launch: LaunchSnapshot?): String? = brief(launch, 0f).ship

    fun brief(launch: LaunchSnapshot?, tSec: Float = 0f): MissionBrief {
        if (launch == null) {
            return MissionBrief(
                "NO MISSION", "—", "NONE", "VOID", "0", "NO LOCK",
                "—", "Wait for a tracked vehicle.", false,
                "Even the sharks are on hold.", null, null, "—", "IDLE"
            )
        }
        val n = blob(launch)
        val sep = FlightProfiles.sepTime(launch)
        val deploy = FlightProfiles.events(launch).firstOrNull { e ->
            val t = e.second.uppercase()
            "DEPLOY" in t || "STARLINK" in t
        }?.first ?: (sep + 400f)
        val known = classify(n, launch)
        val state = payloadState(tSec, sep, deploy, launch, known.kind)
        val bookGap = VehicleCatalog.needsUpdate(launch)
        val note = when {
            bookGap -> VehicleCatalog.UPDATE_BODY
            known.unknown -> unknownNote(launch)
            else -> known.note
        }
        return MissionBrief(
            title = launch.missionName.ifBlank { launch.name }.uppercase(),
            vehicle = "${launch.rocketName}  ·  ${launch.provider}",
            payloadName = known.name,
            payloadKind = known.kind,
            payloadCount = known.count,
            payloadState = state,
            orbit = known.orbit,
            objective = known.objective,
            classified = !bookGap && (known.unknown || "nrol" in n || "classified" in n),
            note = note,
            booster = when {
                isFlight13(launch) && tSec >= 410f -> "HARD SPLASH  GULF"
                isFlight13(launch) -> "GULF RETURN  NO CATCH"
                isZhuque3(launch) && tSec >= 480f -> "MINQIN LANDING  LEGS"
                isZhuque3(launch) -> "DOWNRANGE  MINQIN  LEGS"
                else -> null
            },
            ship = when {
                isFlight13(launch) && tSec >= 3340f -> "SOFT SPLASH  INDIAN OCEAN"
                isFlight13(launch) && tSec >= 780f -> "20 STARLINK V3  OUT"
                isFlight13(launch) -> "PEZ  STARLINK V3  STOWED"
                else -> null
            },
            site = launch.pad.ifBlank { launch.location }.ifBlank { "SITE UNKNOWN" },
            status = launch.statusName.uppercase()
        )
    }

    private data class Classified(
        val name: String,
        val kind: String,
        val count: String,
        val orbit: String,
        val objective: String,
        val note: String,
        val unknown: Boolean
    )

    private fun classify(n: String, launch: LaunchSnapshot): Classified {
        if (isZhuque3(launch)) {
            return if ("honghu" in n) Classified(
                "HONGHU-03", "SATELLITE", "1",
                "LEO",
                "Orbit. Recover the first stage at Minqin.",
                "Y2. Stainless methalox. Landing legs. First Chinese land recovery with legs.",
                false
            ) else Classified(
                launch.missionName.ifBlank { "UNLISTED" }.uppercase().take(22),
                "SATELLITE", "?",
                "LEO",
                "Orbital insertion. Recover the booster.",
                "Payload name from the manifest only. We will not invent a sat.",
                launch.missionName.isBlank()
            )
        }
        if ("zhuque" in n || "landspace" in n) return Classified(
            "UNLISTED", "UNKNOWN", "?",
            "—",
            VehicleCatalog.UPDATE_HEAD,
            VehicleCatalog.UPDATE_BODY,
            true
        )
        if (isFlight13(launch)) return Classified(
            "STARLINK V3", "BROADBAND SAT", "20",
            "SUBORBITAL  DEMISE",
            "V3 drop. Pez. RF + laser. Then demise.",
            "Not three sats. V3. Twenty of them. First time Starship hauled the big ones.",
            false
        )
        if (com.ccos.retro.data.PublishedLaunchFacts.isOwl(launch) ||
            ("owl" in n && ("strix" in n || "synspective" in n))
        ) {
            return Classified(
                "STRIX", "SAR SAT", "1",
                "575 km LEO  38°",
                "Deliver 1x StriX (Synspective) to 575 km LEO at 38 deg.",
                "Rocket Lab Electron. Pad Launch Complex 1 Māhia, this flight LC-1B.",
                false
            )
        }
        return when {
            "starlink" in n -> Classified("STARLINK", "BROADBAND SAT", "STACK", "LEO  ·  SHELL", "Orbital insertion. Deploy the stack. Grow the constellation.", "Same joke every time: internet from a flying trash can. It works.", false)
            "crew" in n || "astronaut" in n || "dragon" in n && "cargo" !in n ->
                Classified("CREW", "HUMAN", "2–7", "LEO  ·  STATION / FREE FLY", "Get humans up and home.", "The payload complains if the Wi-Fi is bad.", false)
            "crs" in n || "cargo" in n || "cygnus" in n || "progress" in n ->
                Classified("CARGO", "LOGISTICS", "1", "LEO  ·  STATION", "Food, science, spare socks.", "If it is late, someone in orbit is eating the backup tortillas.", false)
            "mtg" in n || "meteosat" in n || "weather" in n || "goes" in n ->
                Classified(launch.missionName.ifBlank { "WEATHER" }.uppercase(), "EARTH SCIENCE", "1", "GEO / GTO", "Watch the storms before they watch us.", "Pixels of clouds. Civilization runs on this.", false)
            "gps" in n || "galileo" in n || "glonass" in n || "beidou" in n || "navstar" in n ->
                Classified("NAV SAT", "PNT", "1", "MEO", "Tell everyone where they are.", "Without this, your maps app is interpretive dance.", false)
            "nrol" in n || "classified" in n || "national reconnaissance" in n ->
                Classified("CLASSIFIED", "NATIONAL", "UNK", "UNDISCLOSED", "If we knew, it would not be this flight.", "May or may not include space sharks with laser beams. The briefing is redacted either way.", true)
            "jwst" in n || "hubble" in n || "roman" in n || "telescope" in n ->
                Classified("OBSERVATORY", "SCIENCE", "1", "SEL2 / LEO", "Look farther back in time.", "A very expensive camera that hates fingerprints.", false)
            "mars" in n || "perseverance" in n || "ingenuity" in n ->
                Classified("MARS STACK", "PLANETARY", "1", "TMI / MARS", "Go to Mars. Stay for science.", "The good timeline.", false)
            "oneweb" in n || "kuiper" in n || "iridium" in n || "intelsat" in n || "eutelsat" in n || "viasat" in n ->
                Classified(launch.missionName.ifBlank { "COMMS" }.uppercase(), "COMMS SAT", "1+", "LEO / GEO", "Talk to the planet.", "Bandwidth is the new oil. Worse jokes, better latency.", false)
            "unknown" in n || n.contains("unk") || launch.missionName.isBlank() || launch.missionName.equals("mission", true) ->
                Classified("UNKNOWN", "UNSPECIFIED", "?", "NOT IN THE PRESS KIT", "Do the thing. Probably.", unknownNote(launch), true)
            else -> {
                val named = launch.missionName.ifBlank { launch.name }.uppercase().take(28)
                val vague = named.length < 4 || named in setOf("TBD", "TBC", "UNK", "UNKNOWN", "MISSION")
                Classified(named, if (vague) "UNSPECIFIED" else "PAYLOAD", if (vague) "?" else "1",
                    inferredOrbit(n), if (vague) "Not in the press kit." else "Deliver $named to the intended energy.",
                    if (vague) unknownNote(launch) else "Public sheet. No sharks declared. Disappointing.",
                    vague)
            }
        }
    }

    private fun inferredOrbit(n: String): String = when {
        "gto" in n || "geo" in n || "geostationary" in n -> "GTO / GEO"
        "sso" in n || "sun-sync" in n || "sun sync" in n -> "SSO"
        "tli" in n || "moon" in n || "lunar" in n -> "TLI / LUNAR"
        "tmi" in n || "mars" in n -> "TMI"
        "suborbital" in n -> "SUBORBITAL"
        else -> "LEO (ASSUMED)"
    }

    private fun payloadState(tSec: Float, sep: Float, deploy: Float, launch: LaunchSnapshot, kind: String): String {
        val fail = launch.statusName.lowercase().let { "fail" in it || "partial" in it }
        return when {
            tSec < 0f -> "INTEGRATED  ·  STOWED"
            tSec < sep -> "RIDING THE STACK"
            tSec < deploy -> "COAST  ·  BAY CLOSED"
            isFlight13(launch) && tSec >= deploy && tSec < deploy + 1200f -> "DEPLOYED  ·  RF + LASER LOCK"
            isFlight13(launch) && tSec >= deploy + 1200f -> "DEMISED  ·  PLANNED REENTRY"
            fail && tSec > 0f -> "NOMINAL LOST  ·  CHECK STATUS"
            tSec >= deploy -> "DEPLOYED  ·  ON THE ENERGY"
            else -> "EN ROUTE"
        }
    }

    private fun unknownNote(launch: LaunchSnapshot): String {
        val jokes = when {
            launch.isRussian() -> arrayOf(
                "Manifest says scientific equipment. The equipment requested a vodka ration and a hat.",
                "Possibly a tea-cosy for the station. Possibly a Kosmos with a new name. Both classified.",
                "One bear. It has been briefed. It has not been convinced.",
                "Agricultural tractor. It asked for a Molniya orbit. We did not ask why.",
                "A crate stenciled НЕ СМОТРЕТЬ. Range safety shrugged. Tradition.",
                "Gagarin's lost sandwich. Mass properties: heroic.",
                "Two bags of sunflower seeds and a radio that only plays Ваенга.",
                "If we told you, you would already be on the Progress."
            )
            launch.isChinese() -> arrayOf(
                "Listed as weather. The weather has a very good memory.",
                "A jade rabbit with extra batteries. It will not explain the extra batteries.",
                "Long March cargo: one crate, many stamps, zero press kit.",
                "Possibly a lantern for Mid-Autumn. The lantern has a kick stage.",
                "A very quiet box. The box requested SSO and no questions.",
                "Agricultural satellite. The crops are classified.",
                "One (1) dragon. Paper. We think. Do not poke it.",
                "If we told you, the press release would still say meteorological."
            )
            launch.isEsa() -> arrayOf(
                "A baguette with a reaction wheel. Kourou signed off. Twice.",
                "Possibly cheese. The cheese has a clean-room cert and a COSPAR ID.",
                "One (1) very polite satellite. It filed the paperwork in four languages.",
                "Wine, but in a vacuum-rated bottle. The sommelier is in Darmstadt.",
                "A crate marked 'science.' The science requested a coffee break at T-10.",
                "Galileo spare. Or a very expensive bicycle. Manifest in French and German.",
                "The missing sock of Europe. If it returns, we nailed insertion.",
                "Classified by committee. The committee has not yet scheduled the joke."
            )
            launch.isIsro() -> arrayOf(
                "A tiffin for the Moon. Idli is stowed. Chutney is the kick stage.",
                "Possibly a cricket ball with a star tracker. Do not ask the score.",
                "One crate of mangoes and a very serious spectrometer.",
                "Listed as Earth observation. The Earth is being observed, respectfully.",
                "A yoga mat rated for vacuum. The asana is sun-sync.",
                "PSLV rideshare: 40 friends and one secret. The secret brought ladoo.",
                "If we told you, it would still launch on time from Sriharikota.",
                "A lamp for Diwali. The lamp has RCS. The RCS is festive."
            )
            launch.isJaxa() -> arrayOf(
                "A very polite box. It bowed at the pad. Tanegashima approved.",
                "Possibly a cat. The cat has a delta-v budget and a nametag.",
                "One (1) origami crane with a transponder. Do not unfold it.",
                "A thermos of tea and a spectrometer. Both are flight-rated.",
                "Listed as technology demo. The demo is how to be on time.",
                "A lucky cat for the transfer orbit. The paw is the solar array.",
                "If we told you, it would still be extremely well documented.",
                "A bento for the Moon. The pickle is classified."
            )
            launch.isBlueOrigin() -> arrayOf(
                "Jeff's other hat. It has a kick stage. Do not sit on it.",
                "A very expensive crate that goes to space for fun, then comes home.",
                "Listed as New Glenn cargo. The cargo requested a window seat.",
                "One (1) blue origin mystery. The mystery has FEATHER recovery.",
                "Possibly a club membership card. Mass: classified. Vibes: orbital.",
                "If we told you, it would still cost more than the last one."
            )
            launch.isRocketLab() -> arrayOf(
                "A very small crate with a very large attitude.",
                "Listed as Photon cargo. The Photon has opinions.",
                "One (1) Rutherford souvenir. It still smells like electric pump.",
                "Mahia special: classified, but the sheep next door know.",
                "A lunchbox for the Moon. The sandwich is 3D-printed.",
                "If we told you, Peter would already have tweeted a hint."
            )
            else -> arrayOf(
                "This launch may or may not contain space sharks with laser beams. Manifest: REDACTED.",
                "Cargo listed as agricultural equipment. The tractor has RCS thrusters.",
                "One (1) Boltzmann brain, very polite, asked not to be photographed.",
                "Possibly nothing. Possibly everything. Schrödinger's rideshare.",
                "A fridge. It contains only mustard. The mustard has a COSPAR ID.",
                "400 kg of misc. The range safety officer has questions. We have shrugs.",
                "A mixtape for Proxima Centauri. Track 1 is just engine noise.",
                "Bees. Why bees. Do not ask. They have a payload adapter.",
                "An IKEA bag labeled moon stuff. Allen key not included. Delta-v is.",
                "The missing-sock dimension. If your laundry comes back, we nailed insertion.",
                "Classified: if we told you, we would have to put you in a free-return trajectory.",
                "May include the concept of Tuesday. Mass properties: vibey."
            )
        }
        val h = launch.id.hashCode()
        val i = (h and 0x7fffffff) % jokes.size
        return jokes[i]
    }
}

/** Alt / speed / tape times. Stack until sep, then booster and ship split. */
object FlightProfiles {

    const val NO_LAND_T = 1_000_000f

    fun hasLandTime(t: Float): Boolean = t < 50_000f

    fun events(launch: LaunchSnapshot?): List<Pair<Float, String>> =
        FlightEventCatalog.timeline(launch).map { it.tSec to it.title }

    fun sepTime(launch: LaunchSnapshot?): Float {
        val hit = events(launch).firstOrNull { "SEP" in it.second.uppercase() && "SRB" !in it.second.uppercase() }?.first
        if (hit != null) return hit
        // Unknown / book-gap tape has no SEP. Do not invent a 154s split.
        if (VehicleCatalog.needsUpdate(launch)) return NO_LAND_T
        return 154f
    }

    fun boosterLandTime(launch: LaunchSnapshot?): Float {
        val hit = events(launch).firstOrNull { e ->
            val t = e.second.uppercase()
            if ("SHIP" in t || "STARSHIP" in t) return@firstOrNull false
            "HARD SPLASH" in t ||
                (("BOOSTER" in t) && ("LAND" in t || "CATCH" in t || "TOUCH" in t || "SPLASH" in t))
        }
        return hit?.first ?: NO_LAND_T
    }

    fun tapeBlob(launch: LaunchSnapshot?): String =
        FlightEventCatalog.timeline(launch).joinToString(" ") { "${it.title} ${it.detail}" }.uppercase()

    /** Published F13: Gulf hard-splash, not a pad catch. */
    fun boosterEndsGulf(launch: LaunchSnapshot?): Boolean = "GULF" in tapeBlob(launch)

    fun boosterReturnsToPad(launch: LaunchSnapshot?): Boolean {
        val t = tapeBlob(launch)
        if (boosterEndsGulf(launch)) return false
        if ("SPLASH" in t && "CATCH" !in t && "CHOPSTICK" !in t) return false
        return "CATCH" in t || "CHOPSTICK" in t || ("TOUCHDOWN" in t && "BOOSTER" in t)
    }

    /** Published downrange km if the tape states it. Null = do not invent. */
    fun boosterDownrangeKm(launch: LaunchSnapshot?): Float? {
        val t = tapeBlob(launch)
        if ("MINQIN" in t || "390 KM" in t) return 390f
        return null
    }

    fun eventIsBooster(title: String): Boolean {
        val t = title.uppercase()
        if ("BOOSTER" in t || "HOT-STAGE" in t || "BOOSTBACK" in t) return true
        if ("SHIP" in t || "STARLINK" in t || "PAYLOAD" in t || "PEZ" in t) return false
        if ("REENTRY" in t || "PLASMA" in t || "FLIP" in t || "DOOR" in t || "DEPLOY" in t || "RELIGHT" in t) return false
        return true
    }

    fun eventIsShip(title: String): Boolean {
        val t = title.uppercase()
        if ("BOOSTER" in t || "BOOSTBACK" in t) return false
        if ("SHIP" in t || "STARLINK" in t || "PAYLOAD" in t || "PEZ" in t) return true
        if ("REENTRY" in t || "PLASMA" in t || "FLIP" in t || "DOOR" in t || "DEPLOY" in t || "RELIGHT" in t) return true
        return "SEP" in t || "LIFTOFF" in t || "MAX-Q" in t || "MECO" in t
    }

    fun shipLandTime(launch: LaunchSnapshot?): Float {
        val hit = events(launch).lastOrNull { e ->
            val t = e.second.uppercase()
            if ("BOOSTER" in t || "HARD SPLASH" in t) return@lastOrNull false
            if ("BURN" in t || "FLIP" in t || "APPROACH" in t) return@lastOrNull false
            "SPLASHDOWN" in t || "TOUCHDOWN" in t || "SHIP SPLASH" in t || "SHIP LAND" in t ||
                (("SHIP" in t || "STARSHIP" in t) && ("LAND" in t || "TOUCH" in t || "SPLASH" in t))
        }
        // No published ship land: keep the 1e6 sentinel. Do not invent 3340s.
        return hit?.first ?: NO_LAND_T
    }

    fun replayEndSec(launch: LaunchSnapshot?): Float =
        (events(launch).maxOfOrNull { it.first } ?: 600f) + 20f

    /** EST downrange km on ascent. Caps so a booster is not 1600 km inland. */
    fun ascentDownKm(tSec: Float): Float {
        val t = tSec.coerceAtLeast(0f)
        return (0.00235f * t * t).coerceAtMost(110f)
    }

    /**
     * EST ground track. Same function for MCC and wallpaper.
     * Stage 1: pad → sep → published booster splash/land.
     * Stage 2: ascent, then a coast orbit. After REENTRY, lerp to the
     * orbital position at published ship land (Indian Ocean for F13).
     */
    fun vehicleLonLat(
        launch: LaunchSnapshot?,
        tSec: Float,
        stage: Int,
        padLon: Float,
        padLat: Float,
        az: Float,
        west: Boolean
    ): Pair<Float, Float> {
        if (tSec <= 0f) return padLon to padLat
        val sep = sepTime(launch)
        fun along(t: Float): Pair<Float, Float> {
            val down = if (stage >= 2 && t > sep) {
                ascentDownKm(sep) + (t - sep) * 3.6f
            } else {
                ascentDownKm(min(t, sep))
            }
            return GeoDraw.destFrom(padLon, padLat, az, down)
        }
        fun orbitAt(t: Float): Pair<Float, Float> {
            val (aLon, aLat) = along(min(t, 540f))
            if (t <= 540f) return aLon to aLat
            val period = 5520f
            val inc = max(abs(padLat) + 3f, 28.5f).coerceAtMost(if (west) 98f else 57f)
            val phase = asin((padLat / inc).coerceIn(-1f, 1f).toDouble()).toFloat()
            val u = (t / period) * (2f * PI.toFloat()) + phase
            val lat = inc * sin(u)
            val earthRot = 360f * (t / 86164f)
            val nodeRate = 360f / period
            val lon = GeoDraw.wrapLon(aLon + (if (west) -1f else 1f) * nodeRate * (t - 540f) - earthRot)
            return lon to lat
        }
        if (stage <= 1) {
            val (sepLon, sepLat) = along(sep)
            if (tSec <= sep) return along(tSec)
            val land = boosterLandTime(launch)
            val dest = when {
                boosterEndsGulf(launch) -> GeoDraw.destFrom(padLon, padLat, az, 22f)
                boosterDownrangeKm(launch) != null -> GeoDraw.destFrom(padLon, padLat, az, boosterDownrangeKm(launch)!!)
                boosterReturnsToPad(launch) -> padLon to padLat
                else -> sepLon to sepLat
            }
            if (!hasLandTime(land)) return dest
            val u = ((tSec - sep) / (land - sep).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val lon = sepLon + (dest.first - sepLon) * u
            val lat = sepLat + (dest.second - sepLat) * u
            return lon to lat
        }
        val entry = events(launch).firstOrNull { "REENTRY" in it.second.uppercase() }?.first
        val land = shipLandTime(launch)
        if (entry != null && hasLandTime(land) && tSec >= entry) {
            val (eLon, eLat) = orbitAt(entry)
            val (sLon, sLat) = orbitAt(land)
            val u = ((tSec - entry) / (land - entry).coerceAtLeast(1f)).coerceIn(0f, 1f)
            var dLon = sLon - eLon
            if (dLon > 180f) dLon -= 360f
            if (dLon < -180f) dLon += 360f
            return GeoDraw.wrapLon(eLon + dLon * u) to (eLat + (sLat - eLat) * u)
        }
        return orbitAt(tSec)
    }

    data class TrajCam(val cLon: Float, val cLat: Float, val halfLon: Float, val halfLat: Float)

    fun rangeKm(padLon: Float, padLat: Float, vehLon: Float, vehLat: Float): Float =
        GeoDraw.haversineKm(padLon, padLat, vehLon, vehLat)

    fun padAzimuth(launch: LaunchSnapshot?): Float {
        val site = PadBook.find(launch)
        return when {
            site == null -> 90f
            site.sea && site.waterAz < 360f -> site.waterAz
            !site.inland && site.waterAz <= 360f -> site.waterAz
            else -> 90f
        }
    }

    /**
     * Camera follows the vehicle after it leaves the pad.
     * Low altitude after a long downrange is a landing site, not a snap back to the pad.
     * Same function for MCC and wallpaper. Every vehicle in the book.
     */
    fun trajCam(
        padLon: Float,
        padLat: Float,
        vehLon: Float,
        vehLat: Float,
        altKm: Float,
        tSec: Float
    ): TrajCam {
        val vehLonU = GeoDraw.unwrapLon(vehLon, padLon)
        val sepKm = GeoDraw.haversineKm(padLon, padLat, GeoDraw.wrapLon(vehLonU), vehLat)
        val leftPad = sepKm > 80f && tSec > 90f
        val world = sepKm > 2500f && altKm >= 120f
        if (world) return TrajCam(0f, 8f, 180f, 90f)
        if (!leftPad) {
            val halfLon = when {
                tSec < 0f -> 0.95f
                altKm < 25f -> 0.95f
                altKm < 80f -> 2.2f
                altKm < 180f -> 8f
                else -> 38f
            }.coerceIn(0.55f, 180f)
            val latScale = max(cos(Math.toRadians(padLat.toDouble())).toFloat(), 0.38f)
            val halfLat = (halfLon * latScale).coerceIn(0.35f, 55f)
            return TrajCam(padLon, padLat, halfLon, halfLat)
        }
        val halfLon = when {
            altKm < 25f -> 4.5f
            altKm < 80f -> 10f
            altKm < 180f -> 22f
            else -> 45f
        }.coerceIn(2.5f, 80f)
        val latScale = max(cos(Math.toRadians(vehLat.toDouble())).toFloat(), 0.38f)
        val halfLat = (halfLon * latScale).coerceIn(1.8f, 55f)
        return TrajCam(
            GeoDraw.wrapLon(vehLonU),
            vehLat.coerceIn(-68f, 70f),
            halfLon,
            halfLat
        )
    }

    /** EST convective envelope. 0 at interface, peak near 0.38, then falls. */
    fun entryHeatFrac(u: Float): Float {
        val x = u.coerceIn(0f, 1f)
        val rise = (x / 0.38f).coerceIn(0f, 1f)
        val smooth = rise * rise * (3f - 2f * rise)
        val fall = ((x - 0.38f) / 0.62f).coerceIn(0f, 1f)
        return (smooth * (1f - 0.70f * fall)).coerceIn(0f, 1f)
    }

    /**
     * Plasma 0..1 for a stage. Book events only. No invented window.
     * Stage 1: ENTRY / ENTRY BURN / BOOSTER ENTRY / approaching booster land
     *          → published booster splash/land. Known recoverable with a land
     *          time and no entry mark uses land-80s EST.
     * Stage 2: REENTRY → FLIP or published ship land.
     * Unknown / book-gap vehicles stay dark.
     */
    fun plasmaHeat(launch: LaunchSnapshot?, tSec: Float, stage: Int): Float {
        if (VehicleCatalog.needsUpdate(launch)) return 0f
        val ev = events(launch)
        if (stage <= 1) {
            val marked = ev.firstOrNull { e ->
                val n = e.second.uppercase()
                "REENTRY" !in n && "SHIP" !in n && (
                    "ENTRY" in n || ("APPROACHING" in n && "BOOSTER" in n && "LAND" in n)
                )
            }?.first
            val land = boosterLandTime(launch)
            val start = marked ?: run {
                if (!VehicleCatalog.isKnownRecoverable(launch) || !hasLandTime(land)) return 0f
                land - 80f
            }
            // Booster land only. LANDING FLIP is a ship mark — never keep cooking until T+55.
            if (!hasLandTime(land)) return 0f
            val end = land
            if (end <= start) return 0f
            if (tSec < start || tSec >= end) return 0f
            return entryHeatFrac((tSec - start) / (end - start).coerceAtLeast(1f))
        }
        val start = ev.firstOrNull { "REENTRY" in it.second.uppercase() }?.first ?: return 0f
        val end = ev.firstOrNull { "FLIP" in it.second.uppercase() }?.first
            ?: shipLandTime(launch).let { if (hasLandTime(it)) it else return 0f }
        if (tSec < start || tSec >= end) return 0f
        return entryHeatFrac((tSec - start) / (end - start).coerceAtLeast(1f))
    }


    /** Lit engines for a stage. Super Heavy never lights 33 after sep. */
    fun enginesLit(tSec: Float, launch: LaunchSnapshot?, stage: Int, total: Int): Int {
        if (tSec < 0f || total <= 0) return 0
        val sep = sepTime(launch)
        if (stage >= 2) {
            val seco = secoTime(launch)
            val land = shipLandTime(launch)
            val relight = events(launch).firstOrNull { "RELIGHT" in it.second.uppercase() }?.first
            val flip = events(launch).firstOrNull { "FLIP" in it.second.uppercase() }?.first
                ?: if (hasLandTime(land)) land - 40f else NO_LAND_T
            return when {
                tSec >= sep && tSec < seco -> total
                relight != null && tSec >= relight && tSec < relight + 40f -> total.coerceAtMost(2)
                tSec >= flip && tSec < land -> {
                    val u = ((tSec - flip) / (land - flip).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    when {
                        u < 0.35f -> 3.coerceAtMost(total)
                        u < 0.70f -> 2.coerceAtMost(total)
                        else -> 1.coerceAtMost(total)
                    }
                }
                else -> 0
            }
        }
        if (tSec < sep) return total
        return boosterReturnLit(tSec, launch, total)
    }

    /**
     * After sep: ~32s of inner-cluster boostback, coast dark, then landing subset.
     * Flight 13 landing burn is 10 of the center 13. Never 33 on the way home.
     */
    fun boosterReturnLit(tSec: Float, launch: LaunchSnapshot?, total: Int): Int {
        val spec = VehicleCatalog.spec(launch)
        val sep = sepTime(launch)
        val land = boosterLandTime(launch)
        if (tSec < sep || tSec >= land) return 0
        val boostEnd = sep + spec.boostbackSec
        val landStart = (land - spec.landingBurnSec).coerceAtLeast(boostEnd + 8f)
        val inner = if (spec.boostbackLit > 0) spec.boostbackLit else 0
        val landingN = VehicleCatalog.landingLitFor(launch)
        return when {
            tSec < boostEnd -> inner.coerceAtMost(total)
            tSec >= landStart -> landingN.coerceAtMost(total)
            else -> 0
        }
    }


    /** Legs down at the book TOUCHDOWN / CATCH / LAND mark. Not a delayed T+498. */
    fun legsDeployed(launch: LaunchSnapshot?, tSec: Float, stage: Int = 1): Boolean {
        val land = if (stage >= 2) shipLandTime(launch) else boosterLandTime(launch)
        return hasLandTime(land) && tSec >= land
    }

    fun secoTime(launch: LaunchSnapshot?): Float {
        val sep = sepTime(launch)
        return events(launch).firstOrNull { e ->
            val t = e.second.uppercase()
            "CUTOFF" in t || "SECO" in t
        }?.first ?: (sep + 380f)
    }


    /**
     * First-principles EST, not TM. Liftoff TWR ~1.5 so net ~5 m/s².
     * Gravity turn after ~20s: speed grows, height does not go 0.8 km per second.
     * T+14 is hundreds of meters, not 7 miles. 7 miles is closer to T+60.
     */
    fun stackProfile(tSec: Float): Triple<Float, Float, String> {
        if (tSec < 0f) return Triple(0f, 0f, "PRE-LAUNCH")
        val t = tSec
        val pts = arrayOf(
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(15f, 0.55f, 380f),
            floatArrayOf(30f, 2.1f, 900f),
            floatArrayOf(60f, 11.5f, 2000f),
            floatArrayOf(90f, 28f, 3600f),
            floatArrayOf(120f, 52f, 5200f),
            floatArrayOf(150f, 78f, 6800f),
            floatArrayOf(165f, 88f, 7500f)
        )
        var alt = pts.last()[1]
        var spd = pts.last()[2]
        if (t <= pts.last()[0]) {
            for (i in 0 until pts.lastIndex) {
                val a = pts[i]
                val b = pts[i + 1]
                if (t <= b[0]) {
                    val u = ((t - a[0]) / (b[0] - a[0]).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                    alt = a[1] + (b[1] - a[1]) * u
                    spd = a[2] + (b[2] - a[2]) * u
                    break
                }
            }
        } else {
            alt = 88f + (t - 165f) * 0.35f
            spd = 7500f + (t - 165f) * 18f
        }
        val phase = when {
            t < 12f -> "ASCENT"
            t < 70f -> "ASCENT"
            t < 155f -> "MAX-Q / ASCENT"
            else -> "MECO"
        }
        return Triple(alt, spd, phase)
    }

    /** Sensed g including gravity. Pad = 1. Liftoff TWR ~1.5. */
    fun accelG(tSec: Float, launch: LaunchSnapshot?): Float {
        if (tSec < 0f) return 1.0f
        val sep = sepTime(launch)
        val seco = secoTime(launch)
        return when {
            tSec < 2f -> 1.48f
            tSec < sep -> (1.5f + 1.7f * (tSec / sep.coerceAtLeast(1f))).coerceAtMost(3.5f)
            tSec < seco -> 0.95f + 0.7f * ((tSec - sep) / (seco - sep).coerceAtLeast(1f))
            else -> 0.0f
        }
    }

    fun profile(tSec: Float, launch: LaunchSnapshot?, stage: Int): Triple<Float, Float, String> {
        if (tSec < 0f) return Triple(0f, 0f, "PRE-LAUNCH")
        val sep = sepTime(launch)
        if (launch == null || tSec < sep) return stackProfile(tSec)
        val atSep = stackProfile((sep - 0.2f).coerceAtLeast(0f))
        return if (stage <= 1) {
            boosterReturn(tSec, sep, boosterLandTime(launch), atSep.first, atSep.second, launch)
        } else {
            val land = shipLandTime(launch)
            val entry = events(launch).firstOrNull { "REENTRY" in it.second.uppercase() }?.first
                ?: if (hasLandTime(land)) land - 640f else NO_LAND_T
            val flip = events(launch).firstOrNull { "FLIP" in it.second.uppercase() }?.first
                ?: if (hasLandTime(land)) land - 40f else NO_LAND_T
            shipAfterSep(tSec, sep, land, entry, flip, atSep.first, atSep.second, secoTime(launch), launch)
        }
    }

    private fun boosterReturn(
        tSec: Float,
        sep: Float,
        land: Float,
        altSep: Float,
        spdSep: Float,
        launch: LaunchSnapshot?
    ): Triple<Float, Float, String> {
        val spec = VehicleCatalog.spec(launch)
        val altPeak = altSep * 1.08f
        // No published land time: EST coast. Never teleport to 0.
        if (!hasLandTime(land)) {
            val boostEnd = sep + spec.boostbackSec
            val returning = VehicleCatalog.isKnownRecoverable(launch)
            val alt = if (returning && tSec < boostEnd) {
                val uB = ((tSec - sep) / (boostEnd - sep).coerceAtLeast(1f)).coerceIn(0f, 1f)
                altSep + (altPeak - altSep) * uB
            } else if (returning) {
                altPeak
            } else {
                altSep
            }
            val spd = (spdSep * 0.85f).coerceAtLeast(2000f)
            val phase = when {
                returning && tSec < boostEnd -> "BOOSTBACK"
                VehicleCatalog.isKnownExpendable(launch) -> "COAST EST"
                else -> "NO RECOVERY TIME IN BOOK"
            }
            return Triple(alt, spd, phase)
        }
        if (tSec >= land) return Triple(0f, 0f, "TOUCHDOWN")
        val burnDur = spec.landingBurnSec.coerceAtLeast(8f)
        val burnStart = (land - burnDur).coerceAtLeast(sep + 8f)
        val boostEnd = (sep + spec.boostbackSec).coerceAtMost(burnStart - 4f)
        val alt = when {
            tSec < boostEnd -> {
                val uB = ((tSec - sep) / (boostEnd - sep).coerceAtLeast(1f)).coerceIn(0f, 1f)
                altSep + (altPeak - altSep) * uB
            }
            tSec < burnStart -> {
                val uC = ((tSec - boostEnd) / (burnStart - boostEnd).coerceAtLeast(1f)).coerceIn(0f, 1f)
                (altPeak + (12f - altPeak) * uC).coerceAtLeast(12f)
            }
            else -> {
                val uL = ((tSec - burnStart) / (land - burnStart).coerceAtLeast(1f)).coerceIn(0f, 1f)
                (12f * (1f - uL)).coerceAtLeast(0f)
            }
        }
        val spd = when {
            tSec < boostEnd -> (spdSep * 0.90f).coerceAtLeast(4000f)
            tSec < burnStart -> {
                val uC = ((tSec - boostEnd) / (burnStart - boostEnd).coerceAtLeast(1f)).coerceIn(0f, 1f)
                (spdSep * 0.70f * (1f - 0.45f * uC)).coerceAtLeast(1800f)
            }
            else -> {
                val uL = ((tSec - burnStart) / (land - burnStart).coerceAtLeast(1f)).coerceIn(0f, 1f)
                (1600f * (1f - uL)).coerceAtLeast(0f)
            }
        }
        val phase = when {
            tSec < boostEnd -> "BOOSTBACK"
            tSec < burnStart -> "ENTRY"
            else -> "LANDING BURN"
        }
        return Triple(alt, spd, phase)
    }

    /** Circular vis-viva km/h at altitude. EST, not TM. */
    private fun visVivaCircKmh(altKm: Float): Float {
        val mu = 3.986004418e14
        val r = 6_371_000.0 + altKm.toDouble() * 1000.0
        return (sqrt(mu / r) * 3.6).toFloat()
    }

    private fun shipAfterSep(
        tSec: Float,
        sep: Float,
        land: Float,
        entryIn: Float,
        flipIn: Float,
        altSep: Float,
        spdSep: Float,
        seco: Float,
        launch: LaunchSnapshot?
    ): Triple<Float, Float, String> {
        val cutoff = seco.coerceAtLeast(sep + 10f)
        val burn = (cutoff - sep).coerceAtLeast(1f)
        val uBurn = ((tSec.coerceAtMost(cutoff) - sep).coerceAtLeast(0f) / burn).coerceIn(0f, 1f)
        // F13 TRAJ lock: orbital ~275 km. Cutoff is vis-viva, not leftover 18 km/h-s ascent.
        val f13 = MissionFacts.isFlight13(launch)
        val altTarget = if (f13) 275f else altSep + burn * 0.85f
        val spdTarget = if (f13) visVivaCircKmh(275f) else (spdSep + burn * 18f).coerceAtMost(27500f)
        val altAscent = altSep + (altTarget - altSep) * uBurn
        val spdAscent = spdSep + (spdTarget - spdSep) * uBurn
        if (tSec < cutoff) return Triple(altAscent, spdAscent, "SHIP ASCENT")
        val altAtSeco = altTarget
        val spdAtSeco = spdTarget
        // No published ship land: stay in orbit/coast. Never drop to 0.
        if (!hasLandTime(land)) {
            return Triple(altAtSeco + (tSec - cutoff) * 0.02f, spdAtSeco, "ORBIT / COAST")
        }
        if (tSec >= land) return Triple(0f, 0f, "TOUCHDOWN")
        val entry = entryIn.coerceAtLeast(cutoff + 60f)
        val flip = flipIn.coerceAtLeast(entry)
        val altAtEntry = altAtSeco + (entry - cutoff).coerceAtLeast(0f) * 0.02f
        return when {
            tSec < entry -> Triple(
                altAtSeco + (tSec - cutoff) * 0.02f,
                spdAtSeco,
                "ORBIT / COAST"
            )
            tSec < flip -> {
                val u = ((tSec - entry) / (flip - entry).coerceAtLeast(1f)).coerceIn(0f, 1f)
                Triple(
                    (altAtEntry + (8f - altAtEntry) * u).coerceAtLeast(8f),
                    spdAtSeco * (1f - 0.85f * u),
                    "REENTRY"
                )
            }
            else -> {
                val u = ((tSec - flip) / (land - flip).coerceAtLeast(1f)).coerceIn(0f, 1f)
                Triple((8f * (1f - u)).coerceAtLeast(0f), (4000f * (1f - u)).coerceAtLeast(0f), "LANDING FLIP")
            }
        }
    }

    /**
     * EST remaining propellant, 0..1. Residual film is NOT a drawable load.
     * Spent stage returns 0. Historic and live share this.
     */
    fun isStageSpent(tSec: Float, launch: LaunchSnapshot?, stage: Int): Boolean {
        if (tSec < 0f) return false
        if (stage >= 2) return tSec >= secoTime(launch)
        val land = boosterLandTime(launch)
        val sep = sepTime(launch)
        return if (hasLandTime(land)) tSec >= land else tSec >= sep
    }

    fun fuelRemain(tSec: Float, launch: LaunchSnapshot?, stage: Int): Float {
        if (tSec < 0f) return 1f
        if (isStageSpent(tSec, launch, stage)) return 0f
        val sep = sepTime(launch)
        if (stage >= 2) {
            if (tSec < sep) return 1f
            val seco = secoTime(launch)
            val u = ((tSec - sep) / (seco - sep).coerceAtLeast(1f)).coerceIn(0f, 1f)
            return (1f - u).coerceAtLeast(0f)
        }
        if (!hasLandTime(sep) && VehicleCatalog.needsUpdate(launch)) {
            return (1f - tSec / 180f).coerceIn(0f, 1f)
        }
        if (tSec >= sep) return 0f
        val u = (tSec / sep.coerceAtLeast(1f)).coerceIn(0f, 1f)
        return (1f - u).coerceAtLeast(0f)
    }
}
