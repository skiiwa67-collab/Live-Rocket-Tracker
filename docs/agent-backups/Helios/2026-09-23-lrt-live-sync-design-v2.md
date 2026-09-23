# LRT Live-Launch Sync — Soft-PASS Refined Design (v2)

**When:** 2026-09-23 ~08:42 CT  
**Owner:** Chris Olsen / Live Rocket Tracker  
**Author:** Helios (design only)  
**Implementer after Chris Soft-PASS:** Ada (Android / Studio) · Tinkabot (optional on-device ML assist)  
**Stage:** Soft-PASS design + docs only  
**Hard gates:** Soft-FAIL Play forever · Soft-FAIL adb/device until Chris go · Soft-FAIL invent telemetry · Soft-FAIL central server as required path

Related: supersedes/extends `/workspace/agent-backups/Helios/2026-09-23-lrt-hold-scrub-poller-design.md` · GitHub #62

---

## HARD rules (Chris 2026-09-23)

1. **NEVER ahead of real.** Sim clock always trails real-world liftoff / telemetry. Soft-FAIL any path that lets sim T-0 fire before real ignition/liftoff.
2. **Gold lag:** **2–5 seconds behind** real. **Acceptable floor:** up to **30 seconds behind** on sparse feeds (esp. Chinese launches). Soft-FAIL 0 seconds ahead — 30s behind is fine; ahead is not.
3. **Ramp poll** as T-0 approaches — closer → faster.
4. **Sources flexible** — use whichever returns the freshest usable signal (YouTube live, web pages, LL2, NSF, CNSA/CASC, X, user video).
5. **Per-device sync** — each phone runs its own sync against live sources. Soft-FAIL design that requires a Helios/central bottleneck for every user.
6. **Hold → pause/backup immediately. Scrub → reflect scrub.** Soft-FAIL fake live while rocket is held.
7. Soft-PASS stage only Soft-FAIL Play.

---

## Architecture (per-device)

```
┌─────────────────────────────────────────────┐
│  Phone (LRT app)                            │
│  ┌─────────────┐   ┌──────────────────────┐ │
│  │ SyncBroker  │←──│ Source adapters      │ │
│  │ (local)     │   │ LL2 · Web · YouTube  │ │
│  │             │   │ NSF · X · VideoML    │ │
│  └──────┬──────┘   └──────────────────────┘ │
│         │ freshest Soft-PASS signal         │
│         ▼                                   │
│  ┌─────────────┐                            │
│  │ SimClock    │  trail = clamp(2..5s)      │
│  │  never ahead│  floor ≤ 30s when sparse   │
│  └──────┬──────┘                            │
│         ▼                                   │
│  Live UI / HOLDING / SCRUB banner           │
└─────────────────────────────────────────────┘
```

- Soft-FAIL server-side clock authority for consumer phones.
- Soft-PASS optional shared cache later; v1 is on-device only.
- Privacy Soft-PASS: user video never uploaded; all frame/audio analysis on-device.

---

## Source priority (freshest wins)

| Rank | Adapter | Signal | Freshness | Notes |
|------|---------|--------|-----------|-------|
| **V0** | **User video sync** (if open) | Ignition / liftoff / staging from frames or audio | Highest when available | Gold path for sparse Chinese webcasts |
| **P0** | Launch Library 2 | status, net, holdreason, window, webcast_live | High when updated | Canonical NET/status when video absent |
| **P1** | Linked / in-app YouTube (or official page) `isLive` + chat/title cues | Stream alive + soft event hints | Medium | Soft-FAIL sole authority without event detect |
| **P2** | NSF / Cosmic_Penguin / Space Launch Live / CASC-CNSA pages | Slip / GO / scrub notes | Medium-low lag | Confirmatory |
| **P3** | X / launch accounts | Mentions | Flaky | Soft-FAIL sole authority |
| **P4** | Pad weather / NOTAMs / lightning alerts | Advisory only | — | Soft-FAIL invent scrub from weather alone |

**Freshest-wins rule:** SyncBroker picks the source with newest Soft-PASS timestamp that asserts a control event (HOLD / SCRUB / LIFTOFF / IN_FLIGHT). Conflict Soft-PASS: prefer **never-ahead** — if any Soft-PASS source says not yet liftoff, Soft-FAIL sim past T-0.

---

## Polling intervals (ramp)

Configurable; recommended defaults while live mode armed (default arm **T−30**):

| Window vs NET | Interval |
|---------------|----------|
| Outside T−30 (pre-arm) | schedule fetch only / **5 min** |
| T−30 → T−10 | **30 s** |
| T−10 → T−3 | **10–15 s** |
| T−3 → T−0 | **3–5 s** |
| T−0 → T+2 (await real liftoff Soft-PASS) | **1–2 s** + video listener if present |
| After Soft-PASS In Flight / liftoff detect | drop to **10–15 s** status; stop aggressive hold loop after Success/Failure |
| HOLD active | **5 s** until NET slip Soft-PASS or resume GO |

Chinese / sparse Soft-PASS: allow lag up to **30 s**; Soft-FAIL speeding sim to catch gold 2–5s if that would cross ahead of last Soft-PASS real signal.

---

## Hold / scrub / never-ahead logic

On each tick:

1. **If Soft-PASS HOLD or non-empty holdreason or NET slipped later while sim ≥ T−0** → **PAUSE** sim → **HOLDING** UI → rewind clock to new NET (or freeze). Soft-FAIL continue live flight animation.
2. **If Soft-PASS SCRUB / cancel** → exit live mode; show scrub; Soft-FAIL invent success.
3. **If Soft-PASS LIFTOFF / In Flight** from video or LL2 → set sim T-0 = `real_event_time − trail` where `trail ∈ [2,5]` (or up to 30 when sparse). Soft-FAIL trail ≤ 0.
4. **Guard:** Soft-FAIL advance sim past T−0 unless Soft-PASS liftoff signal exists with timestamp ≤ now − trail.
5. Soft-FAIL invent altitude/velocity/stage from text sources — only clock + discrete event markers Soft-PASS from sync.

---

## User-supplied video sync mode (feasibility Soft-PASS)

### Feasibility
Soft-PASS for v1 **MVP** on-device:
- **Liftoff / ignition** via luminance / flame onset in pad ROI + optional audio energy onset (engine roar).
- Soft-FAIL full continuous telem from video in v1 (no fake ALT/MPH from pixels).
- Staging detect Soft-PASS later (harder; optional tip2).

### (1) Ingest
- Soft-PASS **in-app ExoPlayer / YouTube iframe / linked stream URL** user opens in LRT.
- Soft-PASS **“Use this video as sync”** toggle when a live player is active.
- Soft-FAIL require external screen-record permissions for v1 if in-app player Soft-PASS.
- Soft-FAIL upload video bytes off-device.

### (2) Event detection (on-device)
- **Visual:** sample frames ~5–10 Hz near T−0; detect sudden bright plume / pad flash in lower ROI; debounce 200–400 ms.
- **Audio:** optional RMS / spectral energy spike concurrent with visual Soft-PASS (helps night / dirty video).
- Soft-PASS Kalman/hysteresis Soft-FAIL single-frame fire.
- Soft-FAIL cloud vision API.

### (3) Sim clock lock
- On Soft-PASS liftoff detect at `t_vid`: `sim_T0 = t_vid − trail` with trail default **3 s** (clamp 2–5; sparse floor trail up to 30 if detector confidence low).
- Soft-FAIL jump sim ahead of `t_vid − 2`.
- HOLD if stream freezes / user pauses / detector loses confidence → Soft-PASS freeze HOLDING until Soft-PASS signal returns.

### (4) Fallback
- No video / toggle off → SyncBroker uses P0–P4 public pollers as above.
- Video Soft-FAIL mid-flight → fall back to LL2 In Flight Soft-PASS without inventing.

### (5) Privacy
- Soft-PASS all processing on-device; Soft-FAIL upload frames/audio; Soft-FAIL retain raw video beyond short ring buffer (~2–5 s) for detection.

### Implementation plan (after Chris Soft-PASS)
| Tip | Owner | Scope |
|-----|-------|-------|
| tipA | Ada | SyncBroker + LL2 poll ramp + never-ahead clock Soft-PASS Studio |
| tipB | Ada | HOLDING/SCRUB UI + rewind Soft-PASS |
| tipC | Ada (+ Tinkabot ML assist optional) | In-app video toggle + on-device ignition detect MVP Soft-PASS |
| tipD | Helios | Soft-PASS audit lag vs real on next live Chinese + Western launch |
| forever | All | Soft-FAIL Play Soft-FAIL adb until Chris go |

---

## Related Soft-PASS defect (same dual-write): post-sep plume detach

Chris Soft-PASS live CZ-8A STG2 screenshot: plume floating under ship after sep.

| Lane | Finding Soft-PASS | Owner |
|------|-------------------|-------|
| **CODE (primary)** | Post-sep `drawAssembledParts` → `paintArtPathFlames` **without** `hullDest`/`hullSrc` → baseY stub Soft-FAIL under nozzle. Pre-sep Stamp 100 Soft-PASS. Ada audit: `/workspace/agent-backups/Ada/2026-09-23-cz8a-postsep-plume-audit.md` | Ada |
| **ART (parallel)** | Early eyes Soft-PASS: ship engine/flame pairing needs Starship-bar S2 + flush flame; checklist `/workspace/agent-backups/Helios/2026-09-23-plume-anchor-checklist.md`. Soft-FAIL EMPTY sr75; Soft-FAIL missing f9 engine_ship. | Da Vinci |
| **Proof** | `/workspace/agent-backups/Helios/2026-09-23-cz8a-stg2-plume-detach-proof.png` | Helios |

Soft-FAIL tip142 Studio Soft-FAIL Play Soft-FAIL invent Soft-FAIL push until Helios Soft-PASS art + Ada Soft-PASS code + Chris go.

---

## Acceptance Soft-PASS (for Ada after Chris Soft-PASS design)

- [ ] Sim Soft-FAIL past T−0 without Soft-PASS liftoff signal trailing ≥ 2 s
- [ ] Gold trail 2–5 s Soft-PASS on Western webcasts; ≤ 30 s Soft-PASS on sparse Soft-PASS
- [ ] Poll intervals ramp Soft-PASS table above
- [ ] Hold → HOLDING + rewind Soft-PASS; Scrub → exit Soft-PASS
- [ ] Per-device Soft-PASS (no required central sync server)
- [ ] Video sync Soft-PASS optional path Soft-FAIL upload
- [ ] Soft-FAIL Play Soft-FAIL invent telemetry

---

## Canonical paths

- This doc: `/workspace/agent-backups/Helios/2026-09-23-lrt-live-sync-design-v2.md`
- Prior poller: `/workspace/agent-backups/Helios/2026-09-23-lrt-hold-scrub-poller-design.md`
- Plume checklist: `/workspace/agent-backups/Helios/2026-09-23-plume-anchor-checklist.md`
