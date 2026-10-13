# Play Stage Pipeline feasibility Soft-PASS — 2026-09-23 07:37 CT

## Verdict
**Feasible Soft-PASS.** Agents Soft-PASS stage (signed AAB + Play Console **draft** via Android Publisher API). Soft-FAIL Publish/Rollout/Production/`completed`/`inProgress` forever. Chris alone hits Publish in Console.

## 1) Google Play Developer API Soft-PASS
- API: Android Publisher API v3 (`androidpublisher.googleapis.com`)
- Auth Soft-PASS: **service account** (recommended) with scope `https://www.googleapis.com/auth/androidpublisher`, linked in Play Console Users & permissions. OAuth user client Soft-PASS alternate (acts as Chris account — Soft-FAIL for agent automation).
- Soft-PASS agent may: `edits.insert` → `edits.bundles.upload` (AAB) → `edits.tracks.update` with release **`status: draft`** → `edits.commit` (optionally `changesNotSentForReview=true`).
- Soft-FAIL agent: track status `completed` / `inProgress` / promote / rollout fraction / production go-live.
- Docs Soft-PASS: draft releases exist specifically so API can upload + create release later deployed via Console UI.

## 2) Signing Soft-PASS
- Current Soft-FAIL: no `signingConfigs` in `app/build.gradle.kts`; `lrt-upload.jks` on disk only.
- Cleanest Soft-PASS (Chris HARD today): **Chris Studio Generate Signed Bundle** (path A Soft-PASS).
- Cleanest Soft-PASS for automation later: wire `signingConfigs` reading passwords from **box env / secret card only** (never chat/memory/Git). Soft-PASS `bundleRelease` after JAVA_HOME/JBR Soft-PASS.
- Soft-FAIL store keystore passwords in transcripts, Notion, or agent profile.

## 3) Owner recommendation Soft-PASS
**Do not spin a new bot week-one.** Soft-PASS split:
- **Ada** — owns LRT Git/Studio, signed AAB path, runs stage upload when tooling Soft-PASS.
- **Tinkabot** — owns Play Developer API skill/MCP wrapper (draft-only Soft-FAIL publish methods).
- **Helios** — owns HARD Soft-FAIL Publish gate + Soft-PASS pre-commit audit (status must be draft).

New specialized bot Soft-PASS only if Chris wants a dedicated Play Stage persona after the skill Soft-PASS exists.

## 4) Persona / HARD lines Soft-PASS
- HARD ABSOLUTE: Soft-FAIL Publish/Rollout/Production/promote/`inProgress`/`completed` on production.
- Soft-PASS only: upload AAB + create **draft** track release + Soft-PASS Helios when draft ready for Chris.
- Soft-PASS dual-write Notion + LRT GitHub with exact CT timestamp on every stage.
- Soft-FAIL invent Console state; Soft-PASS blocker with exact step if 2FA/login.

## 5) Effort Soft-PASS
**~2–4 days**, not weeks:
1. Chris: Cloud project + enable API + SA JSON + Play Console link + minimal permissions (~1–2h Chris time).
2. Tinkabot: draft-only upload skill + Soft-FAIL publish methods (~1 day).
3. Ada: AAB Soft-PASS path (Studio handoff or wired signingConfigs via secrets) + first soft stage (~1 day).
4. Helios: gate audit + dual-write receipt + Soft-PASS Chris "draft ready" (~0.5 day).

## 6) Risks Soft-PASS (API access without Publish button)
- Bug sets `status: completed` → users get build Soft-FAIL.
- SA granted "Release to production" → Soft-FAIL blast radius.
- Orphaned Play edits after crash.
- Leaked SA JSON / keystore passwords.
**Mitigations Soft-PASS:** Play Console permissions Soft-PASS minimal (create/edit drafts / manage testing Soft-PASS; Soft-FAIL production release if Console allows); code Soft-FAIL any non-`draft` status; Helios Soft-PASS audit before `edits.commit`; secrets only via secret card; Soft-PASS internal/closed testing draft first forever before production draft.

## Chris Soft-PASS checklist
1. Enable Google Play Android Developer API on a Cloud project.
2. Create service account + download JSON → secret card (not chat).
3. Play Console → Users and permissions → invite SA with **minimal** rights (draft/upload Soft-PASS; Soft-FAIL production release).
4. Keep generating tip139 signed AAB via Studio path A until automation Soft-PASS.
5. You alone click Publish / Rollout in Console.
