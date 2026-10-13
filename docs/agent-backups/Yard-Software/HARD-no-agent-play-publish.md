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
