# LRT Live-Sync Soft-PASS Amendment v2.1 — Post-NET gate + RUD MCC mode

**When:** 2026-09-26 ~09:17 CT (Helios voice call)  
**Parent:** `2026-09-23-lrt-live-sync-design-v2.md`  
**Stage:** Soft-PASS design only · Soft-FAIL Ada code / Studio / phone / Play until Chris go

## HARD addenda (Chris 2026-09-26)

### A. Post-NET start gate (ascent kinematics)
- Soft-FAIL start of flight telemetry / sim ascent narration at wall-clock NET alone.
- Soft-PASS wait about **NET + 5 seconds to NET + 10 seconds** (or Soft-PASS liftoff detect + gold trail 2–5s, whichever is later / never-ahead).
- Intent: real rocket on Chris's video should already have lifted and be accelerating before LRT shows that speed/altitude story.
- Gold lag from v2 (**2–5 s behind**, floor **≤30 s** sparse) still Soft-PASS. Soft-FAIL any lead of real.

### B. Never predict the future
- Soft-FAIL showing successful flight while pad anomaly / RUD is still possible.
- Lag / slight behind Soft-PASS preferred over ahead Soft-FAIL.

### C. RUD / anomaly UI (control center, not fake flight)
- Soft-FAIL continue a nominal success trajectory after Soft-PASS anomaly/RUD/loss signals.
- Soft-PASS posture like sitting in MCC: anomaly, not sure what's wrong, possible rocket loss; display only what Soft-PASS sources can pick up at that point.
- Soft-FAIL invent recovery or continuing boost numbers without Soft-PASS source.

### D. Exact feed Soft-FAIL accepted
- Cannot get perfect real-life telemetry Soft-PASS. Narrating behind real is the design target.

## Open Soft-PASS verify
- Chris to drop live landing screenshots when available (phone not linked to Helios this morning).
- Observed goofy speed (~2000 mph memory) near landing Soft-FAIL exact without photos.

## Implementer
Ada after Chris go. Soft-FAIL agent Play forever.
