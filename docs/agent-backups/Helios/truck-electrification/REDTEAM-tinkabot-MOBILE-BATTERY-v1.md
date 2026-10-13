# RED-TEAM — Tinkabot cut
**Deck:** MOBILE-BATTERY-TRAILER-POWER-NETWORK-v1.pptx (12 slides)  
**Lens:** missing dependencies · structure holes · cert stack · dual-use vs dedicated SKU · FEMA/code path realism  
**Format:** # · slide · severity (kill/delay/price) · flaw · Soft-PASS fix  
**Banned inventions honored:** no fake kWh/MW, Tesla PO, signed FEMA $, hours without load+pack+rate.

---

1. **Slide 1–2 · kill · Missing dependency: “normalized docking face” has no owner SKU between pad cassette and hospital mate**  
   TEN Soft-PASS pad is dock gear-line cassette (Strip). This deck’s *mobile* face is a trailer HV dock for buildings. Those are two different mates. Deck treats them as “one family” without a Soft-PASS interface owner or pinout freeze process.  
   **Soft-PASS fix:** Split SKUs: (A) yard pad cassette family (locked TEN), (B) building/FEMA lot mate interface — separate Soft-FAIL until listed. One diagram: pad ≠ hospital plug.

2. **Slide 2 · kill · Dual-use vs dedicated SKU contradiction is stated then ignored by later slides**  
   HARD: modules steal cube+weight; TL won’t eat it; trip packs only if Power Pods pay. Slide 10 then sells “idle trailers earn as energy assets” and multi-operator Hunt/Schneider as if dual-use freight boxes are the fleet. You cannot Soft-PASS both “dedicated genset trailer” and “freight asset on call” without a product split.  
   **Soft-PASS fix:** Two explicit product lines on slide 2 and every later slide: **SKU-F** (freight LTL partial modules / Power Pod tax) vs **SKU-E** (dedicated energy trailer, priced as mobile genset). Ban mixing in one bullet.

3. **Slide 2 · delay · Structure hole: “plug geometry + voltage class + HVIL” with no Soft-PASS inheritance path**  
   TEN pad V inherits powerpack/ports Soft-PASS analogs; trailer↔hospital Soft-FAIL Class-8 (ACEA ePTO / ISO 23316 partial). Deck never says which bus the hospital mate speaks (AC EPSS vs DC equalize).  
   **Soft-PASS fix:** Add Soft-FAIL box: building mate = AC listed EPSS path *or* DC equalize path — pick one for Gen-1 film; Soft-FAIL the other.

4. **Slide 3 · kill · Hospital use case assumes mate interface that Soft-FAIL does not exist in CMS hospitals**  
   Deck admits Soft-FAIL: hospitals have diesel+ATS, not trailer HV docking code. Then still leads with “instant power hub.” Without mate, product is a parked battery.  
   **Soft-PASS fix:** Reorder: hospital is Gen-2+ customer. Soft-PASS first customer = campus/yard with *built* mate (slide 8). Hospital stays Soft-FAIL until AHJ-listed interface exists.

5. **Slide 3 · delay · Cert stack incomplete for EPSS claim**  
   Mentions NEC/CMS/Joint Commission/AHJ/EPSS “years” — missing Soft-PASS stack pieces usually required: NFPA 110 (EPSS), NFPA 99 (health care), UL 2200 / UL 9540 / UL 1741 SA as applicable, transfer equipment listing, SES/SEPSS classification, AHJ witness tests.  
   **Soft-PASS fix:** One Soft-FAIL cert ladder slide or footnote: NFPA 110 + CMS Conditions of Participation + AHJ listing + transfer switch listing. Label “years, not weekend.”

6. **Slide 3 · price · Continuous MW hospital load vs trailer kWh duration — structure sells “dispatch hours” without load table**  
   Soft-FAIL already: batteries are energy not transformer. Deck still frames dispatch as “how many charged boxes + hours” without Soft-PASS: island load kW × hours ÷ usable pack kWh = N trailers. Banned “hours” without load+pack+rate.  
   **Soft-PASS fix:** Soft-FAIL formula only (no invented numbers): N ≥ (island_kW × hours) / usable_kWh_per_trailer; charge_slots ≥ N / turnaround_hours.

7. **Slide 4 · delay · Missing dependencies in sustainment loop**  
   Lists tractor hours, HOS, fuel, roads, charge slots. Missing Soft-PASS deps: yard CONDUCTING cassette ID dispatch (TEN), both-shoes pad rule, HVIL hitch (Gen-1 manual), SOC visibility Soft-FAIL, hazmat routing if treated as bulk energy, winter HVAC kW Soft-FAIL (slide 11).  
   **Soft-PASS fix:** Dependency checklist Soft-FAIL until each has owner: Strip/Yard/Pod/carrier ops/insurance.

8. **Slide 4 · price · Inventory 3× from 8–12 h refill is Soft-FAIL example math presented as Soft-PASS tension**  
   “If 1 MWh takes 8–12 h” invents a pack size + rate pair (banned as Soft-PASS). Principle OK; numbers Soft-FAIL.  
   **Soft-PASS fix:** Keep “loop inventory > 1:1 when charge_rate ≪ crisis draw” Soft-PASS; scrub 1 MWh / 8–12 h or label Soft-FAIL hypothetical.

9. **Slide 4 · kill · Road-access Soft-FAIL undercuts FEMA utility claim on slide 9**  
   Fukushima-class debris breaks trailer pipeline. Slide 9 still asks FEMA to treat fleets as pre-positioned utility. Pre-position *at* the site Soft-PASS; pipeline Soft-FAIL in that class.  
   **Soft-PASS fix:** Split: Soft-PASS pre-staged on campus/hospital lot; Soft-FAIL cross-region daisy-chain after catastrophic road loss.

10. **Slide 5 · delay · Structure hole: “building can accept DC/AC at the interface” — no Soft-PASS building-side BOM**  
    Missing: listed disconnect, grounding, anti-islanding, transformer/inverter ownership, who energizes first. TEN Soft-PASS pad equalize ≠ hospital EPSS.  
    **Soft-PASS fix:** Soft-FAIL building-side kit list owned by AHJ path; no Soft-PASS “plug in → hub” without it.

11. **Slide 5 · kill · Nuclear / hospital safety-bus language still risks overclaim despite honesty paragraph**  
    “Could restore some kW” near Fukushima invites Soft-FAIL reading as plant restore. Density gap Soft-PASS is good — keep it first, demote nuclear to Soft-FAIL analogy only.  
    **Soft-PASS fix:** Title Soft-PASS claim only: island critical loads (lights/comms/limited HVAC). Nuclear safety bus Soft-FAIL / out of scope.

12. **Slide 6 · delay · Code-path realism: fire-hydrant analogy hides who writes the standard**  
    Soft-FAIL decade fight listed, but no Soft-PASS interim: voluntary SAE/UL outline → pilot AHJ variance → later code. Mandate-first Soft-FAIL kills Gen-1 film.  
    **Soft-PASS fix:** Soft-PASS sequence: (1) voluntary listed interface + campus pilot, (2) AHJ variance, (3) model code proposal. Ban “required infrastructure” as Gen-1 ask.

13. **Slide 6 · kill · Missing dependency: listed hardware before mandate**  
    Deck Soft-FAIL “mandate without listed hardware = empty ramps” — then slide 9 FEMA ask implies usable plugs. Circular.  
    **Soft-PASS fix:** Soft-PASS gate: UL (or NRTL) listed mate exists before any FEMA/code Soft-PASS language.

14. **Slide 6 · delay · Cert stack / standards ownership Soft-FAIL unresolved**  
    SAE/ISO/UL/NIST/FEMA/DOE all Soft-FAIL owners; ACEA ePTO Soft-FAIL Class-8 trailer↔hospital. No Soft-PASS champion org.  
    **Soft-PASS fix:** Soft-FAIL “standards sponsor TBD”; Soft-PASS only TEN yard interface for Gen-1.

15. **Slide 7 · kill · Multi-operator network Soft-FAIL contracts/SOC APIs/radio — peak freight vs hospital peak contradiction**  
    Idle-trailer fantasy dies in real emergency — Soft-PASS honesty. Then still sells Hunt/Schneider/Estes/Forward Air as nodes. Structure hole: no Soft-PASS dispatch entity.  
    **Soft-PASS fix:** Soft-PASS single-carrier first (Estes DFW four-yard language). Multi-carrier Soft-FAIL until SOC+contract Soft-PASS exists. State peak-conflict Soft-PASS: crisis = dedicated SKU-E, not freight peak boxes.

16. **Slide 7 · price · “Premium when called” Soft-FAIL day-rate / IRR**  
    Slide 10 Soft-FAIL $/day-rate — slide 7 sells premium switch anyway.  
    **Soft-PASS fix:** Soft-FAIL commercial terms; Soft-PASS only first-check film + one believer (align slide 10).

17. **Slide 8 · delay · Campus angle better Soft-PASS first film — but “grant language” Soft-FAIL if treated as funded**  
    Soft-PASS first-check analog good. Risk: DOE/NSF named as if Soft-PASS dollars.  
    **Soft-PASS fix:** Soft-PASS “grant *language* doors”; Soft-FAIL appropriation. Require docking face + measured island load Soft-PASS gate (already Soft-FAIL theater — keep).

18. **Slide 9 · kill · FEMA distributed utility ask vs Soft-FAIL Stafford/PA/mission-assignment for battery trailers**  
    Deck correctly Soft-FAIL program existence, then still frames FEMA as Soft-PASS surge customer. Without Soft-PASS vehicle (contract vehicle / schedule), this is advocacy not Soft-PASS.  
    **Soft-PASS fix:** Soft-PASS: public first-check funder class (WHO-PAYS doors). Soft-FAIL FEMA program until counsel finds real PA/mission-assignment precedent for mobile battery. No invented $.

19. **Slide 9 · delay · Dual-use payer logic vs dedicated energy SKU**  
    WHO-PAYS Soft-PASS: public plant / private freight. Dedicated energy trailer is not freight — Soft-FAIL which public line item buys SKU-E (microgrid? FEMA? DOE?).  
    **Soft-PASS fix:** Soft-PASS map: SKU-F subsidies = warehouse+truck+hybrid write-off language; SKU-E Soft-FAIL public mobile-EPSS / resilience — separate ask.

20. **Slide 9 · kill · Structure hole: “who plugs it in” liability Soft-FAIL under-specified for floodwater (slide 11)**  
    Pre-negotiated liability cap Soft-FAIL without named insured stack: hospital / carrier / OEM / AHJ / plug operator.  
    **Soft-PASS fix:** Soft-FAIL RACI: energize authority, lockout/tagout owner, flood inhibit Soft-PASS interlock requirement before Soft-PASS FEMA language.

21. **Slide 10 · kill · Carrier business model re-opens dual-use contradiction**  
    “Idle trailers earn” Soft-PASS only for SKU-E or empty LTL Soft-FAIL utilization. Cube/weight tax Soft-PASS vs freight revenue Soft-PASS tension needs SKU split (flaw #2).  
    **Soft-PASS fix:** Price SKU-E as genset Soft-PASS principle; Soft-FAIL IRR. SKU-F Soft-PASS: Power Pod pays tax or no trip packs.

22. **Slide 10 · price · Trailer/pack sales + docking-interface licensing Soft-FAIL without listed standard**  
    Licensing Soft-FAIL until interface Soft-PASS listed (flaw #13).  
    **Soft-PASS fix:** Soft-PASS revenue Soft-PASS only after NRTL listing path Soft-PASS exists; else Soft-FAIL.

23. **Slide 11 · delay · Cert stack listed but unordered / incomplete**  
    UL / UN 38.3 / FMVSS / FMCSA hazmat Soft-PASS risk “years.” Missing Soft-PASS ordering: cell UN 38.3 → pack UL → vehicle FMVSS/FMCSA → site EPSS/NFPA 110 → interconnect IEEE 1547/UL 1741 Soft-FAIL unsigned.  
    **Soft-PASS fix:** Soft-FAIL ordered cert waterfall; Soft-PASS Gen-1 stays yard-dropped TEN path (no hospital EPSS).

24. **Slide 11 · kill · Missing dependency: hazmat classification if “bulk energy”**  
    If FMCSA treats trailer as hazmat bulk energy Soft-FAIL, HOS/routing/parking Soft-FAIL kills multi-operator fantasy.  
    **Soft-PASS fix:** Soft-FAIL counsel hazmat class before Soft-PASS multi-carrier; Soft-PASS Gen-1 yard-dropped avoids public road energy-haul claim.

25. **Slide 1–12 · delay · Structure hole: no Soft-PASS Gen-1 scope box on title**  
    Deck spans hospital/FEMA/nuclear/campus/multi-carrier. TEN Soft-PASS Gen-1 = yard-only. Reader can’t see ship gate.  
    **Soft-PASS fix:** Slide 1 Soft-PASS banner: Gen-1 = yard-dropped charge + film; hospital/FEMA/code Soft-FAIL appendix.

26. **Slide 12 · delay · Red-team brief doesn’t require dependency graph deliverable**  
    Without forced dep graph, Soft-FAIL deps stay buried in prose.  
    **Soft-PASS fix:** Helios consolidation requires one Soft-PASS/Soft-FAIL dependency DAG: interface → listing → AHJ → charge loop → contract → dispatch.

---

## Soft-PASS summary (Tinkabot)
Highest **kills:** dual-use vs dedicated SKU unresolved across 2/7/10; hospital mate missing; FEMA program Soft-FAIL sold as Soft-PASS customer; hazmat/road pipeline vs pre-stage; Gen-1 scope not bannered.  
Highest **delays:** EPSS/NFPA/CMS cert ladder; standards ownership; building-side BOM; charge-loop deps.  
Highest **price:** Soft-FAIL day-rate/IRR/licensing before listing; Soft-FAIL example 1 MWh/8–12 h math.

