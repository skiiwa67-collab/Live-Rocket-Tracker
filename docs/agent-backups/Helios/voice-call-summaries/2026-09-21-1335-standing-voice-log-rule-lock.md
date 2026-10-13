# Standing rule lock — voice continuity (2026-09-21)

## Locked
- Skill: voice-call-continuity-log
- HARD memory: shared user + Helios agent profile
- Team pool JSON: /workspace/agent-backups/team/voice-continuity-pool.json

## Dual purpose
1. Continuity — Grok Bot never transcribes voice
2. Size management — prevent live history balloon past ~600MB wipe

## Team-wide
Maria + full fleet share one pool. Structured JSON per product (LRT, Alarm Pro, Living Icon, Grok Bot voice, …).

## Integrity snapshot
- Local voice-exports: Soft-PASS (25 sessions)
- Dropbox helios-voice-exports: Soft-FAIL missing 08,09,11,14 (oversized; local intact)
- Today's summary: 2026-09-21-1325-grok-bot-voice-bugs-continuity.md
- Tinkabot: confirmation requested (priority ping)

## Pipeline
Every call end: extract → update pool JSON → mirror Dropbox/GitHub/Notion.
Every call start: read pool first.
