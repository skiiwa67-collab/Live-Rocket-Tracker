# LRT Starship Flight 14: why the event tape was empty (Helios RCA, 2026-09-28 8:3x AM CT)
Source: /workspace/RetroCommandCenter branch stamp-85 @ e2392de (vc154 / 1.0.144). Read-only. Nothing staged or published.

## Root cause (high confidence, from code)
1. Mission tape CDN `lrt-mission-tapes/v1/starship/flight-14.json` is still `truth: pending`, `events: []` (last touched 2026-09-17). So `MissionTape.hasUsableTruth()` returns false and `FlightEventCatalog.timeline()` falls back to templates.
2. The template fallback matches `MissionFacts.isFlight14()` and returns `starshipFlight14()` (event/FlightEvents.kt:225), which has only two events: LIFTOFF at T+0 and SHIP SPLASHDOWN at T+36000s (10h). The generic `starship()` template has 16 events (Max-Q, MECO, hot-stage, sep, boostback, catch, SECO, reentry...), but Flight 14 skips it.
   Introduced by a5226f5 (2026-09-16, "stamp 105 soft: ... F14 profile"), which was an intentional "no invented mid-flight events" choice.
3. `FlightEventMonitor.poll()` marks any event more than 4s in the past as already fired when it first sees a launch. If the app attached after T+4s, even the LIFTOFF banner never pops. The result is that nothing plays until T+10h.
Other paths were checked and ruled out: the fail-cut only applies to Failure/Partial, and `isEventTapeExhausted` is false because the splash is at T+10h.

## Not checked
No phone logcat from the launch window is on my computer, so I couldn't confirm this against device logs.

## Fix options (need Chris go; Ada owns Git/Studio; LRT currently parked)
A. Remove the F14 special case so it uses the full starship() template, labeled as the nominal profile (matches the old honest-banner rule).
B. Publish the real Flight 14 tape to the lrt-mission-tapes CDN (observed events). This fixes replays with no app build.
C. For the monitor, fire the most recent missed event on a late join.
Nothing staged. No Play.

## Update 8:2x AM CT: altitude/speed during F14 live (Chris saw 429 km / ~13,000 vs actual ~277 km / ~26,000)
- LRT has no live telemetry. Alt/speed = FlightProfiles.profile() time model (event/FlightEvents.kt ~L1059).
- F14 tape has no SEP/SECO, so sepTime=154s default and secoTime=sep+380=534s.
- stackProfile at sep is about 79 km / 6,870 km/h. shipAfterSep (starship, not F13): orbitalCoast=false, so altTarget=altSep+burn*0.85=402 km and spdTarget=spdSep+burn*18=13,710 km/h. After cutoff, alt creeps +0.02 km/s. This matches Chris's 429 km and ~13k km/h.
- F13 has a hard lock (275 km + vis-viva, about 27.8k km/h at 277 km). F14 never got it. Fix: extend the lock to F14/all Starship orbital flights (vis-viva at a published target altitude).
- Engine-out: LL2 has no per-engine data. MissionTapeStore.maybePollLive re-fetches the CDN tape every 20s during Starship flights, so an observed-events push to lrt-mission-tapes (with an ENGINE OUT line) reaches devices in about 20s with no build. The long-term option is OCR of the SpaceX webcast engine-status overlay.
- Chris screenshots not found on my computer; numbers are from code math.
