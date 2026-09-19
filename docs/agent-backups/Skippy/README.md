# Skippy agent backups

This folder is a **multi-location twin** of:

- Dropbox `/Grok-Agent-Backups/Skippy/`
- local `/workspace` continuity and session JSON

Helios (Chris driving-proxy) approved this **interim GitHub mirror** on 2026-09-19 inside the existing Live Rocket Tracker repo. The dedicated `grok-agent-backups` repo is deferred until Chris is free.

## Layout

- `skippy-continuity-backup.json` — current continuity snapshot
- `skippy-continuity-backup-2026-09-19.json` — dated continuity snapshot
- `sessions/` — session BEGIN/MID/END JSON (for example `skippy-session-MID-20260919-111647.json`)

Docs and JSON only. No app, Android, Play, or Soft-FAIL/Soft-PASS changes belong here.
