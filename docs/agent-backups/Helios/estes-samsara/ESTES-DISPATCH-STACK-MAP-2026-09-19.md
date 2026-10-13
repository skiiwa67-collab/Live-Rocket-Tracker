# Estes Express Lines — Dispatch Software Stack Map
**Chris Olsen · Soft-PASS public research · 2026-09-19 CDT**  
**Status:** Soft-PASS on cited public sources only. Soft-FAIL where not documented. No fabrication.

**Career note (Chris voice, call):** Prefer stay at Estes as consultant / driver link to software; UI skill from decades of games + 30 years driving; push suggestions/fixes to Samsara from Estes seat (Samsara job OK if needed, Estes preferred).

**Live pain example:** Route Departure Form 2 LH35083527 — Trailer1 van 516335 + Trailer2 /MTY illegal. Dispatch must not allow second trailer on a van.

---

## End-to-end flow (public Soft-PASS)

```
Customer / rating (Pega)
        ↓
Linehaul / P&D planning (Optym HaulPlan, DriverPlan, RouteMax…)
        ↓
Core ops / equipment / legacy dispatch (IBM i / “AS400”)
        ↓
[★ VALIDATION GATE — best insert here]
        ↓
Samsara cloud (routes, trailers, messages, forms, HOS)
        ↓
Samsara Driver App (tablet) ← Chris today
```

Trailer location side-channel: Spireon FleetLocate (2021) → expanding Samsara Asset Gateways 43k+ trailers (late 2025–end 2026).

---

## 1) Samsara

| Item | Soft-PASS / Soft-FAIL |
|------|------------------------|
| Role | Driver tablet ELD/HOS, workflows, messaging, routes, forms; vehicle telematics; AI dash cams; MEM |
| Estes start | ~2023 Soft-PASS (Estes + Samsara award/customer pages) |
| Modules named publicly | Vehicle Telematics, Apps & Driver Workflows, Video-Based Safety; Driver App auto dep/arr +40%; idle savings ~$3M; ETA predictability +40%; safety scores +25% with AI cams + MEM |
| Trailer expansion | Soft-PASS late 2025: Asset Gateways on 43k+ trailers + Asset Tags (dollies, pallet jacks, ramps); target complete end 2026 |
| Exact “version” string | **Soft-FAIL** — SaaS; no public Estes build/version published |
| Estes↔Samsara private wiring | **Soft-FAIL** — not published. Public pattern only (below) |
| Asset-type / LTL rule enforcement at Samsara | **Soft-FAIL** — public APIs do **not** document van-vs-pups / “no Trailer2 on van” rules. That logic lives in Estes systems unless Estes builds it |

### Public integration surface (any TMS ↔ Samsara)

Docs: [TMS integration](https://developers.samsara.com/docs/tms-integration)

- Routes (loads): `GET/POST/PATCH/DELETE /fleet/routes` — driver **or** vehicle assign, not both
- Trailers/vehicles: `/assets` (+ driver-trailer assignments API)
- Messages: `POST/GET /v1/fleet/messages`
- Forms, HOS clocks/logs, GPS/stats, webhooks, Kafka for high-freq GPS/HOS
- SDKs: TypeScript, Java, C#, Python

**Pitfall Soft-PASS:** Samsara will happily sync a bad route if the TMS posts it. Trailer assignment is separate from route create. No public “equipment compatibility” validator. Route arrive/depart webhooks only for vehicle-assigned routes.

Driver App UI stack (general Samsara, not Estes-specific): React Native Soft-PASS from Samsara engineering post (prior brief).

---

## 2) Estes enterprise backend

| Item | Soft-PASS / Soft-FAIL |
|------|------------------------|
| IBM i / AS400 still live | **Soft-PASS** — LinkedIn IBM i Architect / Systems Engineer roles; RPG ILE, CL, SQL, Java, Rational Developer for i; EDI on System i; service desk AS400 account/printer support |
| “Built own dispatch + handheld for years” | **Soft-PASS** — Optym blog quote (Rob Webb / Estes tech history) |
| Exact product name “S400” | **Soft-FAIL** — almost certainly driver shorthand for AS/400; no public product named S400 |
| OS = “Linux base on Windows” | **Soft-FAIL as stated** — public Soft-PASS is a **mixed** estate: IBM i + Windows/AD/SCCM + Java/Spring + AWS/Azure Kubernetes (employee profiles). Not one documented “Linux-on-Windows” dispatch OS |
| Dispatch language | **Soft-PASS on IBM i side:** RPG ILE, CL, SQL, Java. **Soft-FAIL** which exact programs own trailer1/trailer2/dolly fields |
| Equipment fields (trailer, dolly, asset type) schema | **Soft-FAIL** — not public |
| How tablet talks to backend | Soft-PASS pattern: Samsara cloud ↔ Estes systems via API/integration; Pega historically for handheld P&D visibility. Exact Estes middleware Soft-FAIL |

Pega Soft-PASS: freight rating (20M+ rules off mainframe constraints), mobile pickup/delivery app, real-time dispatcher visibility (~50k P&D/day scale claimed in Pega materials). Estes site lists PEGA for handheld/shipment status.

---

## 3) Other systems in the chain

| System | Role | Soft-PASS |
|--------|------|-----------|
| **Optym HaulPlan** | Middle-mile linehaul load plans/schedules | Yes |
| **Optym DriverPlan** | Linehaul driver schedules | Yes |
| **Optym RouteMax** | P&D inbound planning, routing, dispatch, driver mgmt; mobile module exists in product family; Estes inbound rollout (85/222 terminals Oct 2024 → target Q1 2025 complete per Optym) | Yes |
| **Optym LiveHaul / DockAI** | Dynamic linehaul / dock AI — licensed/planned | Soft-PASS as partnership intent |
| **Pega** | Rating engine, mobile P&D, visibility, low-code ops apps | Yes |
| **Samsara** | Telematics, Driver App, cams, expanding trailer gateways | Yes |
| **Spireon FleetLocate** | Trailer tracking since 2021 (~40k assets) | Yes |
| Classic commercial LTL/400 or TL2000 as *the* core | **Soft-FAIL** — no public Soft-PASS that Estes’s core is those packages |

RouteMax integration (vendor docs, not Estes-specific Soft-PASS): Azure Event Hubs (AMQP) or REST — shipment, equipment, dispatch, route, driver, completion, image, ePOD.

---

## 4) Where to put the van + /MTY validation gate

**Illegal pattern to catch:** Trailer1 = full van + Trailer2 = /MTY (or any second trailer) when asset type cannot pull doubles. Same class: bobtail with trailer/dolly required flags still set.

### Ranked insert points

1. **Best — dispatch / TMS write path (Optym RouteMax UI or IBM i equipment screen)**  
   Block *save* of illegal equipment combo. Dispatcher never creates the bad plan. Highest leverage; matches Chris: “dispatcher should never have been able to add a second trailer.”

2. **Strong — middleware between IBM i (or Optym) and Samsara**  
   Before `POST/PATCH /fleet/routes` and before driver-trailer assignment calls: reject or strip Trailer2, return error to dispatch, do not publish to Driver App. Soft-PASS that this pattern is how TMS↔Samsara is designed; Soft-FAIL whether Estes already has this layer.

3. **Secondary — Samsara org Workflow / Forms / custom Marketplace app**  
   Possible for warnings after sync; Soft-FAIL that Samsara natively enforces Estes LTL asset rules. Partner/overlay can add checks on assignment events.

4. **Last line — Driver App Routes RED + concrete error (Chris UI spec)**  
   If bad plan still arrives: Routes goes red (“van + empty — cannot dispatch; contact dispatch”) instead of burying in Messages. Does not replace #1/#2.

**Do not rely on:** driver phone call after the fact (today’s parking-lot path).

---

## 5) Public APIs / docs / pitfalls (short)

- Samsara developers: tms-integration, routes, assets, driver-trailer-assignments, messaging, forms, webhooks, Kafka  
- Estes public tech pages: Optym + PEGA named; Samsara customer/award pages; Spireon 2021; Samsara trailer expansion 2025–26  
- Pitfalls: SaaS version Soft-FAIL; private Estes integration Soft-FAIL; Samsara will accept bad TMS data; “S400” Soft-FAIL name; Linux-on-Windows Soft-FAIL; Qualcomm Soft-FAIL as current trailer tracker (Spireon → Samsara Soft-PASS)

---

## 6) Consultant path Soft-PASS framing (for Chris)

Stay inside Estes → own driver UX + rule gaps → file concrete tickets with screenshots (this van/MTY case) → propose gate at dispatch write + Driver App red path → partner with IT (IBM i / Optym / Samsara admins). Samsara feature requests ride better with Estes CIO sponsorship (Todd Florence named publicly on Samsara partnership) than as a lone external ask.

