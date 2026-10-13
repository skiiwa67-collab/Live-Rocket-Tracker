# Fuel / Diesel Availability API Research
**Chris Olsen · Sales conversation research · Soft-PASS public facts only · 2026-09-19 (America/Chicago, CDT)**

**Goal:** Map real-time diesel *availability* data (tank levels / outages / in-stock by location) for major truck fuel chains, partnership/API access paths, aggregator APIs, and large-fleet evidence.

**Identity note:** Caller said **"Sincera"** — **Soft-FAIL** exact legal identity. Public search does not map “Sincera” to a documented trucking telematics / fuel-data company (hits are unrelated, e.g. tire line). May be a client name, misspeak for **Samsara**, or another party. Treat as placeholder for the consumer of any feed. Do not assert who “Sincera” is.

**Hard distinction used throughout:**
- **Price / location / amenities** ≠ **diesel availability** (tank level, “out of diesel,” pump lanes down).
- Soft-PASS means documented in public sources cited below. Soft-FAIL means not found in public documentation — do not invent.

---

## Executive summary (Soft-PASS)

1. **No major truck-stop chain publishes a Soft-PASS public API for real-time diesel tank levels / inventory.** Documented chain APIs focus on **locations, fuel prices, loyalty, parking, showers, truck service, payments**.
2. **Love’s Soft-PASS:** Partner Store & Fuel **Prices** API on `developer.loves.com` (OAuth via sales rep / Request Access). Exposes store + fuel **prices** comparable to the public location/fuel search page — **not** Soft-PASS tank availability.
3. **Pilot Company Soft-PASS:** Partner API / SDK program (MuleSoft case: APIs/SDKs to ~10 transport partners; partner portal Soft-PASS via product-manager account mentioning GasBuddy, Garmin, RTS Pro). myPilot Soft-PASS: mobile fueling, **IoT real-time parking**, shower reservations. **Soft-FAIL** public diesel tank/outage API. Operational Updates page Soft-PASS exists for store status / emergency alerts (human page, not documented inventory API).
4. **TA / Petro Soft-PASS:** Developer portal lists Location & Amenities, **Parking Availability**, **Shower Availability**, Truck Service — **Soft-FAIL** diesel tank levels in the listed suite. Public **Network Updates** page Soft-PASS posts diesel *pump down* notices (manual ops page, not an API).
5. **QuikTrip Soft-FAIL:** No Soft-PASS public developer fuel/availability API. App Soft-PASS shows fuel prices / store finder; location pages Soft-PASS list Auto Diesel / Truck Diesel as amenities (product offered, not live stock).
6. **Aggregators Soft-PASS prices; Soft-FAIL authoritative chain tank feeds:** GasBuddy Soft-PASS crowdsourced **Fuel Availability Tracker** (crisis/outage reporting, including NO DIESEL) — not a Soft-PASS official public developer API. Trucker Path Soft-PASS partner SDK method for diesel **prices**. Mudflap Soft-FAIL public fuel-availability API docs. WEX/Comdata/EFS Soft-PASS card/transaction/account APIs — Soft-FAIL station diesel-inventory APIs. OPIS Soft-PASS commercial **price** products/APIs — Soft-FAIL outage inventory.
7. **Large fleets Soft-PASS on discounts / loyalty / payment rails** (e.g. J.B. Hunt 360 Perks + Pilot; Schneider + Relay; Relay at Pilot and Love’s). **Soft-FAIL** public evidence they receive chain **real-time diesel stock** APIs.
8. **Sales implication:** If the product needs true “diesel available at site X now,” price APIs Soft-FAIL that requirement — open **BD / partnership** with chains (or define a proxy: crowdsource + recent successful diesel transactions + ops bulletins).

---

## 1) Per chain — Soft-PASS / Soft-FAIL matrix

| Chain | Public / partner API Soft-PASS? | Documented data | Real-time diesel *availability* / tank level | Access model |
|-------|----------------------------------|-----------------|-----------------------------------------------|--------------|
| **Love’s Travel Stops** | Soft-PASS developer portal + Store & Fuel Prices REST API | Store locations + fuel **prices** (`/stores`, `/fuelPrices` with OLA product codes); comparable to https://www.loves.com/en/location-and-fuel-price-search | **Soft-FAIL** — docs do not describe tank inventory / outage status | OAuth; contact Love’s sales representative or “Request Access” on portal |
| **Pilot / Flying J (Pilot Company)** | Soft-PASS Partner API Portal (non-self-serve public OpenAPI Soft-FAIL); Soft-PASS MuleSoft case (~10 transport partners + SDKs) | Partner narrative Soft-PASS: fuel **pricing**, loyalty, mobile fueling; myPilot Soft-PASS: parking IoT, showers, offers | **Soft-FAIL** documented diesel tank/outage API. Soft-PASS: Operational Updates human page for store status / emergency alerts | Partnership / fleet specialist / Partner API program — Soft-FAIL open public Swagger |
| **QuikTrip (QT)** | **Soft-FAIL** public fuel developer API | Soft-PASS consumer app: fuel prices, store finder; Soft-PASS location pages list Auto Diesel / Truck Diesel as offered products | Soft-FAIL live stock/outage API | Soft-FAIL — commercial / BD contact path not Soft-PASS documented as API onboarding |
| **TA / Petro (TravelCenters of America)** | Soft-PASS `ta-petro.com/developers` + request-access form; Soft-PASS experience pricing API docs (TA diesel + competitor **prices**, OPIS rack, ProMiles — internal/customer pricing use) | Soft-PASS listed: Location & Amenities Lookup, **Parking Availability**, **Shower Availability**, Truck Service / work orders | Soft-FAIL tank levels in listed APIs. Soft-PASS: Network Updates page posts diesel pump-down notices (ops web page) | Request API Access form Soft-PASS |
| **Other majors** | Soft-FAIL exhaustive survey of every regional chain | Soft-FAIL | Soft-FAIL | Soft-FAIL |

### Love’s — Soft-PASS detail

- Portal: https://developer.loves.com/ (Anypoint Exchange assets include Store & Fuel Prices, Truck Care Service, Loyalty, Factoring, Vendors, etc.)
- Store & Fuel Prices docs: https://developer.loves.com/ad47e8c2-fb2b-4eaf-9d61-099061f1cfaa/loves-store-fuel-price-exp-api/
- Production base Soft-PASS: `https://apis.loves.com/locations/api/`
- Access Soft-PASS: OAuth; “contact your Love’s sales representative” or Request Access.
- **Soft-FAIL:** No Soft-PASS documentation that `/fuelPrices` or `/stores` returns tank levels, gallons remaining, or “out of diesel.”

### Pilot Company — Soft-PASS detail

- MuleSoft case Soft-PASS: unlocked product/inventory/loyalty/customer data for **myPilot**; mobile diesel fueling; IoT parking with micro-radar; shower reservations; APIs/SDKs released to ~10 transportation partners; integration examples include GasBuddy, Garmin, RTS Pro (partner-portal Soft-PASS via public portfolio write-up).
  - https://blogs.mulesoft.com/digital-transformation/business/pilot-flying-j-omnichannel-case-study/
  - https://www.mulesoft.com/case-studies/api/omnichannel-pilot-flying-j
  - https://www.leslittell.com/general-8 (Pilot Partner API Portal Soft-PASS — secondary source)
- Soft-PASS public sites: fuel prices page, locations, customer portal, fleet contact form (credit/factoring/maintenance — not Soft-PASS “inventory API signup”).
  - https://pilotcompany.com/fuel-prices
  - https://pilotcompany.com/fleet-contact-us
  - https://www.pilotcompany.com/operational-updates
- Soft-PASS historical: Pilot + Sensys Networks real-time **parking** availability in myPilot (press Soft-PASS).
- **Soft-FAIL:** Public OpenAPI for diesel tank levels / outages; Soft-FAIL confirmation that partner APIs expose fuel *inventory* (partner Soft-PASS language emphasizes pricing, loyalty, parking, mobile fueling auth).

### TA / Petro — Soft-PASS detail

- https://www.ta-petro.com/developers/ — Soft-PASS suite: Truck Service (work orders, docs), Retail (locations/amenities, parking availability, shower availability).
- Soft-PASS Network Updates example (ops bulletin, not API): diesel pumps down at a named site — https://www.ta-petro.com/network-updates/
- Soft-PASS pricing Experience API (MuleSoft Exchange Soft-PASS): TA + competitor diesel **prices**, OPIS rack, ProMiles — OAuth/client credentials; oriented to price optimization / customers, not Soft-PASS tank gauges.
- **Soft-FAIL:** Soft-PASS list does not include “diesel availability” / ATG tank-level endpoint.

### QuikTrip — Soft-FAIL / Soft-PASS boundary

- Soft-PASS: QT Mobile App markets up-to-date fuel prices and store finder — https://www.quiktrip.com/qt-mobile-app/
- Soft-PASS: location pages list Auto Diesel / Truck Diesel as amenity/product types.
- **Soft-FAIL:** Public developer portal / partner fuel availability API documentation.

---

## 2) Partnership path (Soft-PASS only)

### What a fleet / software company would Soft-PASS need to negotiate

| Path | Soft-PASS public | Soft-FAIL |
|------|------------------|-----------|
| **Chain partner API (prices / amenities)** | Love’s: sales rep + OAuth Request Access. TA: Launch API Request Form. Pilot: Partner API / transport partner program Soft-PASS (~10 partners historically). | Exact commercial terms, SLAs, data fields for tank levels Soft-FAIL public |
| **Fleet credit / card / factoring** | Love’s Fleet Solutions contact form Soft-PASS: https://www.loves.com/fleetsolutions . Pilot Fleet Solutions contact Soft-PASS: https://pilotcompany.com/fleet-contact-us (credit team phone Soft-PASS for existing accounts: 865-474-2953). | Soft-FAIL that fleet card enrollment alone grants tank-level feeds |
| **Digital payment networks** | Relay Payments Soft-PASS accepted at Pilot and Love’s for digital diesel payment; Schneider Soft-PASS named Relay customer; coverage Soft-PASS claims after Love’s deal (FreightWaves Soft-PASS) | Soft-FAIL that Relay exposes chain ATG / availability APIs to fleets |
| **Telematics / preferred-station import** | Samsara Soft-PASS Preferred Stations + Fuel Purchase APIs (fleet can *store* diesel prices they supply — Soft-PASS docs) — https://developers.samsara.com/reference/listpreferredstations ; https://developers.samsara.com/reference/postfuelpurchase | Soft-FAIL native Soft-PASS Love’s/Pilot/OPIS tank-availability connector in Samsara public docs |
| **Commercial price data vendors** | OPIS Soft-PASS retail/rack diesel **price** products and APIs (subscription) — Soft-PASS Spot API swagger exists for permitted datasets | Soft-FAIL OPIS as Soft-PASS source of station outages / tank levels |

### Practical Soft-PASS ask list for corporate BD

1. Confirm **legal entity** requesting data (“Sincera” Soft-FAIL identity).
2. Specify required payload: **price** vs **diesel in stock / pump lanes open / outage** vs **gallons remaining (ATG)**.
3. Ask Love’s Fleet Solutions + API owner: Can Store & Fuel Prices (or a separate feed) include **fuel status / outage**? What volume / preferred-fueling commitment unlocks partner access?
4. Ask Pilot Partner / Fleet Specialist: Which Soft-PASS partner APIs (pricing, loyalty, parking) are available to telematics / fleet apps today, and is **diesel availability** in any partner contract?
5. Ask TA: Beyond parking/shower availability APIs, is there a Soft-PASS or private **fuel status** feed? Can Network Updates be consumed programmatically under agreement?
6. Ask QT Commercial Fuels / BD: Soft-FAIL public API — is there a private partner data program?
7. Clarify whether **payment** partners (WEX, Comdata/Corpay, EFS, Relay, Mudflap) can share **authorized transaction success at lane** as a proxy Soft-PASS signal (recent successful diesel sale ≠ tank level, but Soft-PASS usable heuristic if licensed).

**Soft-FAIL:** Exact NDA templates, price lists for inventory feeds, or guaranteed ATG sharing programs Soft-FAIL in public documentation.

---

## 3) Competitor / large fleet evidence

| Party | Soft-PASS public evidence | Soft-FAIL |
|-------|---------------------------|-----------|
| **J.B. Hunt** | Soft-PASS Aug 2018: Carrier 360 Perks with Pilot Flying J — ~18¢/gal diesel discount at participating Pilot/Flying J with J.B. Hunt 360 fuel card; Pilot myRewards points; Truck Care discounts; incentives integrated in Carrier 360. https://www.jbhunt.com/our-company/newsroom/2018/08/jbh-teams-up-with-industry-leaders-to-reward-carriers | Soft-FAIL public diesel **availability** data-share API |
| **Schneider** | Soft-PASS Relay Payments customer (lumper then fuel; network Soft-PASS includes Pilot; Love’s Soft-PASS Relay acceptance). Soft-PASS Relay press / FreightWaves coverage of Love’s + Relay market coverage. | Soft-FAIL tank-level / outage API with chains |
| **Walmart** | Soft-FAIL public Pilot (or Love’s) diesel-availability partnership. Caller note Soft-PASS as caveat: may not fuel at Pilot. Soft-PASS unrelated telematics (e.g. Platform Science) does not Soft-PASS equal fuel inventory sharing. | Soft-FAIL |
| **Mudflap** | Soft-PASS fleet fuel card / discount product marketing. Soft-PASS partner API host Soft-PASS at api.mudflapinc.com with OAuth scopes Soft-PASS limited to openid/profile/email in public catalog scrapes. | Soft-FAIL public diesel availability / tank API docs |
| **GasBuddy** | Soft-PASS crowdsourced Fuel Availability Tracker (Has Fuel / Limited / No Fuel; app Soft-PASS NO DIESEL reporting in activated crisis areas) — https://tracker.gasbuddy.com/ ; https://www.gasbuddy.com/go/fuel-availability-tracker . Soft-PASS historical WEX alliance (2017). Soft-PASS Business Pages for retailers. Soft-PASS third-party GraphQL wrappers exist (unofficial). | Soft-FAIL official public developer portal documenting licensed availability API for fleets; Soft-FAIL as authoritative chain ATG |
| **Trucker Path** | Soft-PASS partner SDK Soft-PASS method `getDieselPrices(withPlaceIDs:)` (CocoaPods Soft-PASS). Soft-PASS WEX fuel-discount partnership press. Soft-PASS truckloads API docs Soft-PASS for loads (separate product). | Soft-FAIL Soft-PASS REST diesel *availability* / tank API |
| **Fleetcor / Comdata (Corpay)** | Soft-PASS APIs & Web Services for **card/account/vehicle** management, transaction history Soft-PASS — https://resourcecenter.comdata.com/apis-and-web-services/ | Soft-FAIL Soft-PASS station diesel inventory / outage API |
| **EFS** | Soft-PASS card/transaction web service materials Soft-PASS in industry circulation | Soft-FAIL Soft-PASS diesel availability feed |
| **WEX** | Soft-PASS payment/developer products Soft-PASS; Soft-PASS WEX Connect app Soft-PASS markets locations/prices; Soft-PASS telematics APIs Soft-PASS for fleet/telematics data | Soft-FAIL Soft-PASS public station diesel-availability API documentation |
| **OPIS** | Soft-PASS commercial diesel **price** APIs/products (rack/retail Soft-PASS) | Soft-FAIL Soft-PASS outage / tank inventory product Soft-PASS in cited materials |
| **Trimble Maps** | Soft-PASS Places facility info: fuel *options* (ulsd, def, etc.), amenities, Soft-PASS `availableTruckParkingSpaces` real-time parking where available — https://developer.trimblemaps.com/restful-apis/places/facility-information/ | Soft-FAIL diesel tank levels / outages Soft-PASS as Soft-PASS field |
| **Samsara** | Soft-PASS Preferred Stations (fleet-supplied prices Soft-PASS) + Fuel Purchase Soft-PASS write APIs | Soft-FAIL Soft-PASS native chain ATG connector Soft-PASS |

**Pattern Soft-PASS:** Large fleets Soft-PASS negotiate **price discounts, loyalty points, payment rails, parking**. Soft-FAIL public evidence of **real-time diesel stock APIs** from chains to fleets.

---

## 4) Practical recommendation for sales conversation

### What to Soft-PASS claim today

- Love’s Soft-PASS partner **price + location** API path exists (sales-rep gated).
- Pilot Soft-PASS partner ecosystem + Soft-PASS real-time **parking** (not diesel tank) Soft-PASS in myPilot / partner narrative.
- TA Soft-PASS partner APIs for locations, parking, showers Soft-PASS; Soft-PASS human Network Updates for some diesel pump outages.
- Aggregators Soft-PASS **prices**; GasBuddy Soft-PASS **crowdsourced availability** in crisis mode Soft-PASS.
- Fleet Soft-PASS deals Soft-PASS are primarily commercial (cents/gal, rewards, payments).

### What Soft-FAIL (do not claim)

- Any Soft-PASS “public real-time diesel tank-level API” from Love’s, Pilot, QT, or TA.
- That J.B. Hunt / Schneider / Walmart Soft-PASS receive chain ATG feeds.
- Exact identity / rights of “Sincera.”
- That Mudflap / WEX / Comdata / EFS Soft-PASS expose diesel availability APIs Soft-PASS.

### Questions for corporate (script Soft-PASS)

1. Is the buyer need **price routing** or **avoid dry tanks / closed diesel lanes**?
2. Which chains are in scope (Love’s / Pilot / TA / QT / independents)?
3. Acceptable Soft-PASS proxies until BD closes: (a) GasBuddy-style crowdsource Soft-PASS, (b) recent fleet-card diesel authorization Soft-PASS if licensed, (c) scrape/subscribe Soft-PASS ops bulletins (TA Network Updates Soft-PASS), (d) driver check-in Soft-PASS?
4. Who owns BD: fleet account (volume commitment Soft-PASS path) vs software partnership Soft-PASS path?
5. For telematics stack: will Preferred Stations Soft-PASS (Samsara Soft-PASS) be populated from Love’s price API Soft-PASS once partner access Soft-PASS granted?

### Bold practical framing Soft-PASS

Do not wait on Soft-FAIL tank telemetry Soft-PASS product. Soft-PASS combine: chain **price** APIs (Love’s Soft-PASS) + partner amenities Soft-PASS + crowdsourced outage Soft-PASS (GasBuddy Soft-PASS) + Soft-PASS payment-success heuristics Soft-PASS under agreement Soft-PASS — while BD Soft-PASS asks chains for Soft-FAIL inventory Soft-PASS licensing Soft-PASS.

---

## Soft-FAIL register (explicit)

| Item | Status |
|------|--------|
| Exact identity of “Sincera” | Soft-FAIL |
| Love’s real-time diesel tank / outage API Soft-PASS | Soft-FAIL (prices Soft-PASS only) |
| Pilot public OpenAPI for diesel availability Soft-PASS | Soft-FAIL |
| QT public developer fuel/availability API Soft-PASS | Soft-FAIL |
| TA diesel tank-level API Soft-PASS in listed suite Soft-PASS | Soft-FAIL |
| Walmart Soft-PASS Pilot diesel-availability partnership Soft-PASS | Soft-FAIL |
| J.B. Hunt Soft-PASS tank-level data share Soft-PASS | Soft-FAIL |
| Schneider Soft-PASS tank-level data share Soft-PASS | Soft-FAIL |
| Mudflap Soft-PASS public diesel availability API Soft-PASS | Soft-FAIL |
| GasBuddy Soft-PASS official public developer availability API Soft-PASS | Soft-FAIL (crowdsource tracker Soft-PASS; commercial licensing Soft-PASS partial / contact Soft-PASS) |
| WEX Soft-PASS public diesel availability API Soft-PASS | Soft-FAIL |
| Comdata/Corpay Soft-PASS station inventory API Soft-PASS | Soft-FAIL |
| EFS Soft-PASS diesel availability Soft-PASS | Soft-FAIL |
| OPIS Soft-PASS station outage Soft-PASS | Soft-FAIL (prices Soft-PASS) |
| Relay Soft-PASS as Soft-PASS ATG data provider Soft-PASS | Soft-FAIL (payments Soft-PASS) |
| Contract terms / pricing for inventory Soft-PASS feeds Soft-PASS | Soft-FAIL |

---

## Sources (Soft-PASS URLs)

**Chains**
- https://developer.loves.com/
- https://developer.loves.com/ad47e8c2-fb2b-4eaf-9d61-099061f1cfaa/loves-store-fuel-price-exp-api/
- https://www.loves.com/fleetsolutions
- https://blogs.mulesoft.com/digital-transformation/business/pilot-flying-j-omnichannel-case-study/
- https://www.mulesoft.com/case-studies/api/omnichannel-pilot-flying-j
- https://www.leslittell.com/general-8
- https://pilotcompany.com/fuel-prices
- https://pilotcompany.com/fleet-contact-us
- https://www.pilotcompany.com/operational-updates
- https://www.ta-petro.com/developers/
- https://www.ta-petro.com/network-updates/
- https://www.quiktrip.com/qt-mobile-app/

**Fleets / payments**
- https://www.jbhunt.com/our-company/newsroom/2018/08/jbh-teams-up-with-industry-leaders-to-reward-carriers
- https://www.relaypayments.com/newsroom/relay-payments-supports-schneider-in-boosting-efficiency-increasing-billing-accuracy-and-reducing-fraud
- https://www.freightwaves.com/news/ceo-says-relay-covers-75-of-diesel-payment-market-after-loves-deal
- https://www.ttnews.com/articles/loves-payment-relay

**Aggregators / data / telematics**
- https://tracker.gasbuddy.com/
- https://www.gasbuddy.com/go/fuel-availability-tracker
- https://resourcecenter.comdata.com/apis-and-web-services/
- https://developer.trimblemaps.com/restful-apis/places/facility-information/
- https://developers.samsara.com/reference/listpreferredstations
- https://developers.samsara.com/reference/postfuelpurchase
- https://www.opis.com/product/pricing/retail-fuel-prices/
- https://cocoapods.org/pods/TruckerPathKit

---

*End of Soft-PASS / Soft-FAIL research report. Generated 2026-09-19 CDT for Chris Olsen sales conversation. Do not fabricate beyond Soft-PASS citations above.*
