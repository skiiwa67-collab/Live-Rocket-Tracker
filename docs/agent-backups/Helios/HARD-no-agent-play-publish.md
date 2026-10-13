# HARD ABSOLUTE — No agent Play Store publish (ever)

**Effective:** 2026-09-23 ~07:33 CT
**Owner:** Chris Olsen (via Helios)
**Scope:** ENTIRE fleet — no exceptions

## Absolute rule
NO agent ever submits / publishes / rolls out directly to the Play Store. Ever. That breaks everything.
Chris himself is the ONLY one who publishes to the Play Store.

## Agents Soft-PASS may
- Build and stage in Android Studio (Studio-first).
- Upload / stage drafts in Google Play Console for Chris review (draft / internal / staged — Soft-FAIL production publish).
- Soft-PASS Helios when staging is ready for Chris review.

## Agents Soft-FAIL must not
- Click Publish / Rollout / Production / promote-to-production.
- Auto-submit review for release without Chris.
- Direct adb device installs (existing HARD_DEVICE_PUSH_GO).

## Extends HARD_DEVICE_PUSH_GO
Same principle: announce → Chris go → Studio-first → Chris confirms → Chris decides device sync and Play publish.

## Canonical
- `/workspace/agent-backups/Helios/HARD-no-agent-play-publish.md`
- Team: `/workspace/agent-backups/team/HARD-no-agent-play-publish.md`
- Prior gate: `/workspace/agent-backups/Helios/HARD-play-store-publish-gate.md`


## Ada standing LRT ship instruction (Chris voice Soft-PASS 2026-09-23 ~08:06 CT)

Once Ada receives staged fix files (examples: garbled labels tip134, wallpaper disclaimer tip136, and any other Helios/Ada Soft-PASS staged fixes) and has them wired properly:

1. Push the build to **Android Studio only**.
2. Soft-FAIL Play Store publish forever.
3. Soft-FAIL production / rollout / promote-to-production forever.
4. Soft-PASS stage in Android Studio and/or Play Console draft for Chris review only.
5. Chris syncs the device and hits Play himself. Chris alone publishes.

This is standing LRT ship law for Ada — not tip-specific. Soft-FAIL exceptions.

Dual-write: Notion Focus + Live Rocket Tracker GitHub issue #58 alongside this canonical.
