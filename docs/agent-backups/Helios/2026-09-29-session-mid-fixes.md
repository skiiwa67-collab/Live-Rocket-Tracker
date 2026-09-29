# 2026-09-29 session mid — fixes and specs

Logged 9:52 AM CT by Helios. This is the mirror record for work named today. It does not publish anything. Chris alone publishes. No Play Store action was taken from this note.

Latest command-flyout spec wins over every earlier flyout note, including the ten-fix line that said to add an extra panel.

## 1. Alarm Pro v48

Chris is pushing a signed AAB to Play Console from the cold-soaked build. Two days, no problems, no code changes. The word is cold soak, not gold soak. Chris alone publishes.

- Local: `/workspace/agent-backups/Helios/2026-09-29-alarm-pro-v48-ship-lock.md`
- GitHub doc commit: `8297fb4` on Live-Rocket-Tracker, 2026-09-29 6:14 AM CT
- Notion Focus: https://app.notion.com/p/3eaa94933faa81fcadf0f760250513da
- Not verified from here: that the AAB itself reached Play Console. No Alarm-Pro repo commit showed up for today.

## 2. Event tape

Three separate notes. Do not collapse them.

Morning, version 1.5.5: Starship Flight 14 tape must keep advancing when the app is backgrounded. Chris's diagnosis was a stopped view tick, a background service Android 26+ will not restart, and a swallowed Log.w. Chris held the Studio push for a Tinkabot check, then said go. No Play.

Ten-fix review, commit `efe00ee` on branch `tip155-restore`: tape centers only on the event happening now. Tinkabot pass 2 called that pass. That commit was not on GitHub when last checked. Local review: `/workspace/agent-backups/tinkabot/2026-09-29-lrt-1562-pass2.md` and `/workspace/agent-backups/Helios/LRT-1562-fix-09-event-tape-2026-09-29.md`.

Phone request, not in version 157: when an event pops to the top, hold it about five seconds, flex if the next event is close, keep the color, shrink as it slides left, then drop it. Not a hard-coded timer. Filed on issue 58, comments 5891787434 and 5891804394. Not coded in this record.

## 3. Version 157 command flyout (latest)

The flyout comes out of the existing command button, one of the eight chips at the top. Ada only changes where it opens. It must not fly out from the bottom of those eight chips, and it must not be a second panel. It can use the length of the phone.

Everything has to fit. JAXA and ISRO, the two chips just added, have to sit properly and stop taking so much space. Ten company buttons: five across, two rows. Shrink the buttons so the text fits.

Scope is the command panel only. Ada finishes, then Tinkabot reviews. No fuel, tape timing, art, Vega C, or Long March 8A in 157.

Already on GitHub issue 58, comment 5892115580, which replaces the earlier flyout notes. Notion Focus task https://app.notion.com/p/3eaa94933faa81178914e546cd09be48 has the same comment.

## 4. Long March 8A blank booster

Chris posted the art bug: on separation the booster became two plain white bodies, no fuel cutouts, no plume, and open engine holes. Side profile should show the nozzle from the side only, plus a plume. Chinese text ballooning was retracted. The art pass was tagged future, not today, in comment 5891851203.

No art file for a completed blank-booster fix was found in local backups, Dropbox `/Grok-Agent-Backups`, or today's GitHub commits. That asset mirror is open until the posted file is located.

## 5. Other work already on the local backup today

- Ten-fix list and Tinkabot pass 2 on `efe00ee`. Falcon Heavy stack md5 `61fc77c120a7b91e4797691759797ec5`.
- Historic and live views should be separate windows. Filed. Da Vinci panel art is under `/workspace/agent-backups/Da-Vinci/lrt-historic-panels-20260929/`. Not in 157.
- Phone bugs still filed and waiting: catalog fuel (Kinetica One first miss, Gravity One is the control), Vega C still a pill.
- Da Vinci settings-chip handoff for MCC and ULA: `/workspace/agent-backups/Da-Vinci/lrt-historic-panels-20260929/skins-lang/HANDOFF-157.md`.
- Ada local diff: `/workspace/agent-backups/Ada/2026-09-29-tip155-fgs.diff`.

## 6. Helios proxy approvals, 2026-09-29

`/workspace/agent-backups/helios/proxy-approvals-log.json` has no grant dated today. Last entry is 2026-09-21. Through 9:52 AM CT Helios granted nothing by proxy. Chris was on the calls and gave the goes himself. Tonight's review can treat today as none.

## Mirrors this file is meant to hit

- Local: this path
- Dropbox: `/Grok-Agent-Backups/Helios/2026-09-29-session-mid-fixes.md`
- GitHub: `docs/agent-backups/Helios/2026-09-29-session-mid-fixes.md` on Live-Rocket-Tracker
- Notion: comment on the version 157 Focus task
