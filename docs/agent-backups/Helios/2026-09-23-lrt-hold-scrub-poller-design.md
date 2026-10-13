# LRT Hold/Scrub Poller — Soft-PASS Design

**Where we are:** 2026-09-23 ~08:38 CDT (13:38 UTC)  
**Owner:** Chris Olsen / Live Rocket Tracker  
**Author:** Helios (design only)  
**Implementer (after Soft-PASS):** Ada (LRT Git / Android)  
**Stage:** Soft-PASS design + docs only  
**Hard gates:** Soft-FAIL Play publish · Soft-FAIL device/adb push · Soft-FAIL invent telemetry · Soft-FAIL implement Android code in this task

---

## 1) Problem statement

Today's **CZ-8A Unknown Payload** (Launch Library 2 id `63063b9d-ade9-4448-8717-6f910aa81188`) was **In Flight** with:

- NET slipped **13:30 → 13:33 UTC** (~3 minutes)
- `holdreason` empty
- `webcast_live` true

Chris's LRT live sim ran on the **published T-0** without checking real-world slips, so the sim launched **~3 minutes ahead of actual liftoff**.

**Chris HARD:** Do **not** display a live launch when the real rocket is not launching (hold / scrub / weather / any reason). The live sim must stay synchronized with reality — never fake a countdown past a real hold or scrub.

---

## 2) Polling sources (priority order)

| Priority | Source | What to read | Notes |
|----------|--------|--------------|-------|
| **P0** | **Launch Library 2 API** | `status` (Go / Hold / In Flight / Success / Failure / TBD), `holdreason`, `net`, `window_start` / `window_end`, `webcast_live` | Canonical truth for NET + status. Poll by launch id when live mode is active. |
| **P1** | **Pad weather** | Open-Meteo (nearest pad coords) **or** official MSA / maritime NOTAMs for China pads; lightning / thunderstorm alerts | Advisory input; alone must not invent a scrub — prefer correlating with LL2 Hold/TBD + holdreason. |
| **P2 (optional)** | **NSF forum Cosmic_Penguin** updates, **Space Launch Live**, unofficial webcast `isLive` | Human/community slip notes; stream-alive signals | Confirmatory only when LL2 lags. |
| **Soft-FAIL** | **X API alone** | Mentions / launch accounts | Access flaky — **do not** rely on X as sole hold/scrub authority. |

**Rule:** LL2 status + NET drive sim control. Weather and community sources may raise confidence or surface a reason string for UI, but must not override a clear LL2 **In Flight / Success / Failure**.

---

## 3) Check interval

Configurable; recommended defaults:

| Window relative to NET | Interval |
|------------------------|----------|
| Outside **T−30** (and inside configurable pre-NET window, default 30 min) | **every 60 s** |
| Inside **T−30** | **every 15–20 s** |
| Inside **T−5** | **every 5–10 s** |
| After **In Flight** / **Success** / **Failure** | **stop** hold/scrub polling (optional one-shot sync then idle) |

Outside the configurable pre-NET window: do not enter aggressive live poll mode.

---

## 4) Hold / scrub detection logic

On each poll tick while live mode is armed:

1. **PAUSE + rewind** → enter **HOLDING** (or freeze T-COUNT UI Chris already has) when **any** of:
   - LL2 `status` is **Hold** or **TBD** **and** NET has slipped later than the sim's current T-0
   - `holdreason` is non-empty
   - Published **NET > now + threshold** while the sim clock is already past T−0 (sim launched “early” relative to reality — today's CZ-8A case)

2. **Exit live mode** when:
   - Scrub / cancel signaled (status path to scrub / Failure pre-flight cancel as defined by LL2 mapping Ada implements)
   - Window closed with no liftoff and status no longer Go/Hold toward flight

3. **Sync and leave running** when:
   - Status becomes **In Flight** or **Success** → snap/sync sim to real timeline and keep running (no invented telemetry)

4. **Go / NET advanced earlier:** if NET moves earlier and status remains Go, rewind/advance sim clock to the new NET without inventing vehicle state.

**Threshold (default):** treat “NET still meaningfully in the future while sim past T-0” as hold/slip — e.g. NET − now ≥ **30–60 s** while sim ≥ T-0. Exact seconds: Ada + Chris Soft-PASS before merge.

---

## 5) Sim clock adjust

- Prefer Chris's existing **HOLDING / T-COUNT** UI state — **freeze** the clock; do not invent progress.
- On slip: **PAUSE** sim and **rewind** displayed T-0 / countdown to the new NET.
- On scrub/cancel: exit live mode cleanly (banner / reason from `holdreason` or status when available).
- On In Flight / Success: sync once to real, continue; **never invent telemetry** (no fabricated altitude, speed, or stage events).
- Soft-FAIL inventing vehicle state from weather or forum text.

---

## 6) Configurable window

- **Default:** arm hold/scrub poller **30 minutes before NET**.
- User/config override: minutes-before-NET to start polling (and optionally max post-NET grace before exit).
- Outside that window: normal schedule fetch only; no high-frequency hold loop.

---

## 7) Soft-PASS stage plan

| Step | Owner | Action |
|------|-------|--------|
| 1 | **Helios** (this doc) | Soft-PASS design locked; dual-write Notion Focus + GitHub issue |
| 2 | **Chris** | Soft-PASS design (or request edits) |
| 3 | **Ada** | Implement poller in LRT Android **after** Chris Soft-PASS — Studio/git only |
| 4 | **All agents** | Soft-FAIL **Play** forever per **HARD-no-agent-play-publish** until Chris explicit go |
| 5 | **All agents** | Soft-FAIL device/adb push until Chris explicit go |

**Out of scope this task:** Android code, Play AAB, device install, invented telemetry, X-only detection.

---

## Acceptance criteria (for Ada after Chris Soft-PASS)

- [ ] Live mode polls LL2 at the intervals above inside the configurable window
- [ ] Hold / slipped NET / non-empty holdreason → HOLDING + rewind; no live “launching” UI while rocket is held
- [ ] Scrub/cancel → exit live mode
- [ ] In Flight / Success → sync and continue; no invented telemetry
- [ ] Weather / NSF / SLL optional; X not sole authority
- [ ] No Play publish from agents

---

## Canonical local path

`/workspace/agent-backups/Helios/2026-09-23-lrt-hold-scrub-poller-design.md`

## Dual-write targets

- GitHub: `skiiwa67-collab/Live-Rocket-Tracker` issue (Soft-PASS design)
- Notion: Focus / Eve board note linking this Soft-PASS design


---
**Superseded Soft-PASS:** see refined v2 `/workspace/agent-backups/Helios/2026-09-23-lrt-live-sync-design-v2.md` (never-ahead, video sync, per-device, 2–5s gold / 30s floor).
