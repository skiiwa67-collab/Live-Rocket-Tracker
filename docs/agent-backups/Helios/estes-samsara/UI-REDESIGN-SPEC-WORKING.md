# Samsara Driver UI redesign — working spec (Estes / Chris Olsen)

**Status:** DRAFT — capturing from voice. Do NOT implement yet.  
**Updated:** 2026-09-19T07:19:50.325756-05:00 (van+/MTY Soft-FAIL example)
**Items so far:** 6 (UI 1–5 DRAFT; item 6 feasibility — still no UI implement)

## Context
Driver-facing Samsara tablet at Estes LTL. Home screenshot Soft-PASS archived: `samsara-home-2026-09-19-0642.jpg` (truck 61784). Related pain: trailer misfolds from near-identical numbers (separate screen still needed).

---

## 1) Screen color — parchment (HARD)

- **Replace** default bright white background with **parchment** (Leonardo da Vinci manuscript style — same family as LRT Leonardo theme if helpful).
- **Why (functional, not pretty):** white glare hurts at night and day; drivers dim the tablet to cope.
- **Do:** switch home (and related screens as scope allows) to parchment.

---

## 2) Routes panel redesign (HARD)

### Layout
- **Routes** box today is nearly worthless.
- **Expand Routes** to fill the space currently occupied by **Forms**.
- **Move Forms** elsewhere (exact home TBD — capture later).

### Per active route, show
1. **Trailer number(s)** — can pull **two** at once (van **or** two pups); list **both**.
2. **Origin → destination** with location codes, e.g. `DAL 089` (Dallas 089) → `ARL 193` (Arlington 193).
3. **Dolly** — once the driver enters the dolly into the system, it **must appear on this same route screen**.

### Color-coded route status (button/indicator)
| Color | Meaning |
|-------|---------|
| **Green** | Clear — dispatched, good to go |
| **Yellow** | Non-blocking issue (e.g. trailer not moved/positioned properly) |
| **Red** | Blocking — equipment flagged in shop / must not pull |

### Concrete example (Chris, this run)
- At **ARL 193**, just left **DAL 089**
- Pulling a **set of two pups** + a **dolly**

---

## 3) Top-bar "O" hot tap / accidental navigation (HARD)

### Problem
- Top-left **"O"** avatar is a hot tap to the driver's personal info screen.
- When a **pop-up** appears (e.g. check whether a trailer can actually be pulled), the driver taps **back** to dismiss.
- Pop-up and back arrow vanish; the finger keeps moving and **lands on "O"**.
- That jumps to **personal info** — useless (driver already knows their own info). Accidental navigation during a real workflow.

### Fix (pick one or both)
1. **Remove** hot-tap from the "O" entirely, **or**
2. **Relocate** the profile control next to the driver's name and date (e.g. "Olsen, September 19th") — away from the dismiss-swipe landing zone.

### Same top bar — truck number
- Make the **truck number larger** (today: e.g. 61784 under the name).
- Drivers switch trucks nearly every day and need to **instantly** see which truck they're in.

*(Captured 2026-09-19T06:48:19.257003-05:00)*

---


## 4) Collapse unused buttons + prioritize HoS / Drive Smart + specific Tasks (HARD)

### a) Collapse dead-weight tiles into **Other**
- Home tiles **Training**, **Documents**, **Trip Log**, **Qualifications** are mostly unused:
  - Training: doesn't even work on these tablets
  - Trip Log: untouched in ~3 years
  - Qualifications: irrelevant to a veteran driver
  - Documents: occasional (e.g. after a breakdown)
- **Collapse all four** behind a single **Other** button that swings out on tap to reveal them — frees screen real estate.

### b) HoS largest on screen
- **Hours of Service** (including break countdown circles) must be the **largest** element — driver runs the day on it.

### c) Drive Smart / Coach — not smaller; bigger if possible
- Safety score / Drive Smart feature must **not** shrink; **make bigger if possible** (safety team likely approves).

### d) Tasks — specific actionable detail
- Today: vague "you have a task."
- **Must** show concrete action + dates, e.g. **"Sign your logs for January 7th through 9th"** — not generic wording.

*(Captured 2026-09-19T06:51:34.999771-05:00)*


## 5) Sign-out prominence + merged vehicle/truck button + fleet data (HARD)

### a) Sign-out more prominent
- Drivers use **Sign out** often when swapping trucks — make it **easier to see and hit**.

### b) Merge Vehicle + truck number — **DYNAMIC traits** (amended)
- Today: "Vehicles" and truck number (e.g. **61784**) are separate with no good reason.
- **Merge** into a single control that shows truck number **plus** glance traits for **the truck currently assigned**, e.g. **"61784 — C and G, single screw"**.
- Traits must be **fully dynamic** — not a static driver profile. When the driver swaps trucks, the button updates to that truck’s:
  - configuration type (e.g. C and G vs other)
  - axle setup (single screw vs twin screw)
  - powertrain (diesel vs other)
  - truck number
- **Data source:** Estes fleet system — driver recalled **"S400"** (exact product name Soft-FAIL / confirm later). Same live feed drives the **dispatch dashboard**.
- Do not invent traits; pull from the authoritative Estes fleet record.


### c) Same truck data → dispatch dashboard
- Vehicle/truck characteristics should **float up** to a **new dispatch dashboard** (driver plans to create) so dispatch sees the same relevant traits.
- Dispatch screens today are scattered (same fragmentation as driver home).
- Note: dispatch may already have some of this data in fragmented form — consolidate, don't invent duplicates.

*(Captured 2026-09-19T06:53:28.257212-05:00)*


### Data source Soft-FAIL (research note 2026-09-19)
- Driver recalled **"S400"** — **no public Soft-PASS** for an Estes product by that exact name yet.
- Public Estes stack clues (not confirmed as the tractor master): **Optym** (RouteMax / DriverPlan / HaulPlan), **Pega** driver/dispatch apps, **Samsara** telematics.
- **Next:** confirm with yard/IT which system owns tractor master (number, C&G, single/twin screw, powertrain). Until then label source as **Estes fleet master (S400?) Soft-FAIL name**.


## 6) Trailer tracking via existing hardware + Starlink (INVESTIGATE — not UI build)

### Problem
- Most Estes trailers have periodic location pings so drivers can find them.
- Unreliable — works sometimes, fails others.
- Driver believes hardware is **Qualcomm** (uncertain Soft-FAIL).

### Tasks
a) Identify exact make/model from Estes fleet records / equipment specs (Qualcomm vs other).
b) Determine if that hardware has a data path/API integrable with Starlink connectivity.
c) Feasibility, cost, hardware/software changes to route trailer location through Starlink instead of or in addition to current link.
d) Short feasibility summary (possible? what it takes? rough cost?).

*(Spec item captured 2026-09-19T06:55:02.884173-05:00; feasibility notes appended when research Soft-PASS)*

### Feasibility Soft-PASS summary (item 6)
- Public Soft-PASS: Estes trailer track = **Spireon FleetLocate** (2021) → expanding to **Samsara Asset Gateways** (2025–26). Qualcomm Soft-FAIL as trailer tracker.
- Starlink on every trailer Soft-FAIL (power/cost). Starlink on **tractors** Soft-PASS as cellular dead-zone backhaul; trailer GPS stays on cellular asset trackers.
- Full write-up: `TRAILER-STARLINK-FEASIBILITY-2026-09-19.md`


## Soft-FAIL example Soft-PASS photo: Route Departure Form 2 (2026-09-19)

**Photo:** `route-departure-van-mty-2026-09-19.jpg`  
**Dispatch:** LH35083527 · Driver 435769 · Form shows "1 field to complete"

| Field | Value |
|-------|--------|
| Trailer#1 | **516335** (van / band) |
| DT1 | 089 |
| Trailer#2 | **/MTY** (empty — Soft-FAIL assignment) |
| DT2 | 089 |
| Dolly1 | required if 2 trailers (incomplete) |

### HARD rules from Chris (this case)
1. **Dispatch must not** be able to add a second trailer when Trailer#1 is a **van/band** — only **pups** pull as a set of two (+ dolly).
2. Van + empty (`/MTY`) is illegal — driver cannot pull another trailer behind a van.
3. If it still happens: **Routes** indicator goes **RED** (blocking) — not a buried messaging error that forces backing out to the bottom Messages tab.
4. Pop-up / route button should state the concrete problem (e.g. cannot pull empty with van) and path to fix — **not** generic "contact dispatch via Messages" as the only UX.
5. Driver today: phone call to remove second trailer; Submit Document greyed — wasteful.

*(Captured 2026-09-19T07:19:50.325756-05:00)*

## Out of scope for now
- Implementation / Samsara Marketplace build
- Soft-PASS to ship

## Evidence add: dispatcher comms failure (2026-09-19 ~07:34 CDT)

Chris on voice after calling dispatch to fix van + /MTY (LH35083527):
- Took **three tries** to explain to dispatcher that he was set up pulling a **set** when he was only pulling a **van**.
- Unclear whether explanation was obtuse or dispatcher having a bad morning — either way, **system should not require that conversation**.
- Pattern is **daily**: drivers juggle unclear data; drivers also screw up the other direction → endless back-and-forth.
- Ask: streamline software **and** paperwork (paperwork Soft-FAIL / later).

**Product implication:** gate + clear equipment labels on dispatch screen beat training humans to re-explain van vs pups every morning.

