# Truck Electrification Network — Technical Brief v1
**Soft-PASS · 2026-09-19 · Consolidated from Strip Electrical, Yard Software, Power Pod Soft-PASS packs via Helios**  
**Status:** SPEC, not live. DFW Node dark. Soft-PASS/Play HOLD.  
**Do not invent:** pad V/A/AWG, SOC%, kit mpg, Tesla PO/logos, signed interconnect MW, SKU prices, lateral spacing, cassette length, insurance %.

**Pitch (messy HTML, live):** https://skiiwa67-collab.github.io/yard-energy-console/  
**Repo:** https://github.com/skiiwa67-collab/yard-energy-console  
**Related HUD:** https://github.com/skiiwa67-collab/carry-hud  
**Windows home:** `C:\Users\skiiw\Projects\Truck-Electrification-Network`  
**Box mirror:** `/workspace/christopher-olsen-projects/04-truck-electrification-network`  
**This Soft-PASS pack:** `/workspace/agent-backups/helios/truck-electrification/`

---

## 1. Vision

Turn LTL / warehouse docks into **dual-use energy nodes**: roofs, trailer skins, building packs, and a flush landing-gear pad move kWh to trailers and hybrid tractors **without melting the building service drop**, and without asking carriers to fund a city power plant from freight pennies.

When the grid is healthy, private freight uses the plant every day so it stays maintained. When the grid dies, **public-funded nodes still move energy**. A related 911 / disaster-comms mesh shares the *payer logic* but is a **different project** (no radios in this trucking HUD).

**Gen-1 ship target = YARD only.** City mesh verbs stay parked until a real interconnect exists.

---

## 2. Architecture (two layers, one product)

### Yard (Gen-1 ship)
Roof / trailer-skin PV → building bank (under slab) → **flush pad at the landing-gear line** (weight switch) → shoe → **HV jumper UP INSIDE landing-gear tubes** → trailer junction → snap-in modules → Power Pod / BEV pack (bidirectional). Skin PV can feed modules with the pad idle. Steel leg ≠ bus. Orange = HV class (not a number).

### City (parked)
Estes DFW four-yard prototype language: **ARL ↔ Dallas ↔ GRP ↔ Lone Star**. Software verbs: **EXPORT / CHARGE / ISLAND** (amber = legal isolated, not pad FAULT). **No signed interconnect. No Soft-PASS MW.** Gen-1 does not ship city.

### Road (Power Pod)
Diesel keeps **axle 1**. **Axle 2** = two Tesla-*class* wheel motors (physics analog only — **not a Tesla PO**). Energy from trailer snap-in modules. Regen on the road is not the dock path.

### Owners
| Owner | Owns |
|-------|------|
| Strip Electrical | Pad / cassette / trench / volts physics |
| Yard Software | Power router + CITY verbs (parked) |
| Power Pod | Axle-2 pods + hybrid software |
| Steve | CARRY-HUD-SEND.html |
| Artist | Stills / layout (stood down on pad volts) |

---

## 3. Gen-1 pad

**What it is:** A flush, full-length run **parallel to dock doors at the gear line** — not the door threshold, not a raised bar, not a 10×10 floor tile. Factory SKU = one snap-out **recycled-tire** cassette; repeat to make the run; replace a chewed bay without tearing out the yard. One cassette face takes **round sand foot and square plate** — no second SKU.

**Site lock:** Chris drew green/red band on real dock photo (bays 66–60, trailer on 62). Strip sits in that band. Band width and wall offset = **SPEC pending tape**. First article: **ONE cassette + ONE lateral + bay 62**.

**What conducts:** Weight is the switch (mechanical plunger / rocker). Rubber does not conduct. Idle pads stay dark. **Both shoes or no watts** (cocked trailer). Seam is dead rubber.

**Cassette stack (top down):** wear face → plunger → sealed wipe contacts → segment tap → snap rails into slab pan / pad bus → status lamps on their own loop (not through HV).

**Laterals:** Fat pipe back to dock wall at intervals set by ampacity / voltage drop / fault zone — **do not invent spacing or AWG**. One dead bay does not kill neighbors; a blown lateral takes its zone, not ~201 doors.

**Lamps (Yard Software input):** CONDUCTING green · IDLE dim · WORN amber · FAULT red. Ice = weather, not a lamp. Island = yard verb, not pad lamp.

**Voltage class:** Inherited from ports (published analogs only — e.g. commercial rooftop DC class; Tesla Powerpack-class published max VDC analogs; Semi Megacharger voltage class). **Pad current is not Megacharger-class.** Per-bay amps Soft-FAIL until shoe patch + in-tube jumper are proven.

**Pad job:** Bidirectional equalize for building powerpacks — not a charger gun, not a Megacharger.

---

## 4. Power Pod

- Diesel keeps one rear drive axle; **two snap-on pods on the second tandem axle** (one motor per wheel).
- Fed from **snap-in trailer modules** (and/or small tractor junction) — **not a wall plug on the cab**.
- **Gen-1 charge money:** charge the **dropped trailer on the pad** (never crosses fifth wheel first).
- **Gen-1 hitch:** manual HV hop + **HVIL** (open = pods offline). Auto-mate = Gen 2.
- Software half: propulsion/regen, per-wheel torque/slip (electric ms vs air brakes 10–50 Hz), event log (insurance discount Soft-FAIL path — no %).
- Fleet stamps equal in HUD: **BEV SEMI · DIESEL DOUBLES · DIESEL+POD**.

**MPG:** No published kit number (**Soft-FAIL**). Closest published analog: NREL Coca-Cola Eaton hybrid **+13.7% in-service**; city EST higher; highway ~0–6%. His DFW day **180–280 mi** is the city case — write-off should hit P&D/shuttle first, not linehaul.

---

## 5. Yard router (software)

- Reads Strip per-cassette lamps; consumes Pod torque/slip/module SOC (**dash until measured**).
- **Dispatch key:** watts → **CONDUCTING cassette ID**, not door number.
- Gen-1 ship = **YARD-only**. City verbs parked.
- Locked HUD chips: **201 doors EST** (Loop 820 class) · **22.0 MWh EST** bank · **SOC dash** · live doors/201 EST.

---

## 6. City mesh (parked)

ARL ↔ Dallas ↔ GRP ↔ Lone Star. Verbs EXPORT / CHARGE / ISLAND. Cell first, sat hop when cell dies. **Not radios. Not 911.** Soft-FAIL until signed interconnect.

---

## 7. Who pays / incentives

**Rule:** Carriers/retailers will not absorb metro PV + packs + strips from freight pennies. **Public pays the plant** (warehouse electrify, truck electrify, hybrid-axle write-off for a percentage of fleet). **Private hauls freight.** Government does **not** buy radios here (911 project).

Do not invent dollar figures or named appropriations. Doors to knock on (language, not cleared money): DOE-class grid resilience, FEMA/disaster dual-use, DoD dual-use microgrid language, mayor/county outage risk, private believer funding a public-use prototype.

HUD must show: **WHO PAYS = public node / private freight**. Outage beat: grid = 0, nodes still move kWh.

---

## 8. First-check sequence (do not skip)

1. Show CARRY-HUD (phone/laptop); flip OUTAGE.  
2. Get **one** believer (money, doors, or both).  
3. Build **one** node — his yard, Loop 820 class (~201 doors EST); a few strip doors first, not 201 day one.  
4. Film it — then earn the right to talk teams.

Do **not** hire OEM / legal / hardware armies before step 3.

First check buys (as public strategic asset, not Estes capex): few-door strip, handful of skin + snap-in packs, one building pack, roof if check big enough (skins + strip first), HUD + film of real dock. It does **not** buy a 200-person company, every DFW roof, Kenworth production pods, or lawyers on retainer.

---

## 9. Open Soft-FAIL gaps

- MiniME offline — Win home not verified this recovery turn  
- No measured SOC / V / A / fault current  
- City interconnect unsigned (no MW)  
- 53 vs 28-ft offset tape pending; frame-width / round-shoe diameter pending  
- Steve cassette-ID coding status UNKNOWN since Aug  
- Live telemetry: none  
- Kit mpg / motor kW / pack kWh / hitch pinout / insurance %: unpublished  
- Auto-mate Gen-2 timeline: on board, not Gen-1 blocker  

---

## 10. Scale context (labeled EST where stated)

DFW industrial / W&D inventory is **~1.1–1.25B SF** depending on broker set (published). Soft-PASS roof energy story is **several GW nameplate / ~dozen–tens GWh/day mid EST** under stated W/SF and capacity-factor assumptions — **not terawatts**. Estes DFW four terminals are the prototype cell; do not spray every industrial roof and call it strategy.

Planning physics from his HUD CONFIG (not invented here): **2.0 kWh/mi** loaded level ground planning; trailer roof ~375 SF in writeup.

---

*End Soft-PASS brief. Anything beyond sources above is Soft-FAIL until measured or signed.*
