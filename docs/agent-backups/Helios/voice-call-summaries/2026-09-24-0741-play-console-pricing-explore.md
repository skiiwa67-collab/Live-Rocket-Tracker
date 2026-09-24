# Helios voice continuity — 2026-09-24 — play-console-pricing-explore

- **Call:** `call-c2c02404-be2f-48dd-a001-b78c2a73e2fd`
- **Time:** 2026-09-24 07:34:32–07:41:45 CT
- **Tags:** LRT, Play Console, pricing, deep-links, tip154

## Decisions
- Play Console deep-link patches are routing changes, not rocket-catalog updates. Do not use them for the catalog; use a remote/server-side catalog later.
- Keep LRT paid. Target price is about **$1.99**, not $9.99.
- Skip pricing experiments with only about three installs.

## Product / status
- `tip154` / `1.0.144` is **Production Active**.
- Price path: **App pricing → Set pricing**. No new APK is needed; new buys receive the changed price.
- Chris also corrected the Play Console phone number during this exploration.

## Bugs / operations
- **LRT price mismatch (open):** Console showed $9.99 versus the remembered ~$1.99 target. Set it to $1.99 if it is still $9.99, while keeping the app paid.
- **Samsung One UI widget drop:** skin/permission quirk, not a crash. Crash history is mostly Chris's own builds.

## Open actions
- Watch Play Console vitals for several days.
- Later stage the next tip rollout and move the rocket catalog server-side when ready.

## Continuity status
- Local session JSON and this summary: **Soft-PASS**.
- No message sent to Chris.
- Full audio STT was not run; extract is from the held call context.
