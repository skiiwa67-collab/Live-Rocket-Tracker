# HARD — Play Store publish gate (extends HARD_DEVICE_PUSH_GO)

**Effective:** 2026-09-23 ~07:33 CT (ABSOLUTE update same minute)
**Owner:** Chris Olsen (via Helios)
**Scope:** ENTIRE fleet — no exceptions

## Absolute
NO agent ever submits directly to the Play Store. Ever. Chris is the only publisher.
Agents Soft-PASS stage in Android Studio / Play Console for Chris review only.

## Flow Soft-PASS
1. Studio-first
2. Stage in Play Console for Chris review
3. Chris reviews
4. Chris publishes (agents Soft-FAIL auto-publish)

## Canonical
- `/workspace/agent-backups/Helios/HARD-no-agent-play-publish.md`
