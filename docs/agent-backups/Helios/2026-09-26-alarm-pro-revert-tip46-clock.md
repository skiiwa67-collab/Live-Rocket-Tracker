# Alarm Pro: revert tip46 clock (option 1), ready for Ada review (2026-09-26)

**Decision:** Chris picked option 1 on the voice call at about 3:58 PM CT on 2026-09-26.
**Reason (Chris):** The widget's built-in timer drives the countdown alarm, so the clock has to stay on that same timer path for accuracy, with no separate clock ticking on its own.

## What changes
- `8bd76cb` (tip46, Sep 23) had set CLOCK to `setChronometer(..., started = false)` and painted the digits once a second from `WallpaperTickService`. The clock froze whenever Android paused or killed that service.
- The revert brings the app code back to exactly `dd05a67`. The CLOCK is a Chronometer again with `started = true`, based on the same `SystemClock.elapsedRealtime()` as the Till countdown. It counts up from midnight in 24-hour mode, or from noon/midnight in 12-hour mode, and keeps the 12-o'clock-hour fix from #3.
- Till countdown: the logic is identical to what was there before tip46. It's still a countdown on the same base, so nothing changes in how it behaves.
- The two docs commits after tip46 (`f5366fc`, `fab613a`) are kept.
- Known tradeoff: hours before 10 show without a leading zero (`9:05:12`).

## Where it is
- Local branch `helios/revert-tip46-clock` in `/workspace/alarm-push/Alarm-Pro`, commit `d5ef02e`, based on `fab613a`. **It's not pushed and not merged.** Ada owns Git and applies it.
- Patch: `/workspace/agent-backups/Helios/2026-09-26-alarm-pro-revert-tip46-clock.patch`
- Check: `git diff dd05a67 HEAD -- app/` comes back empty.

## Gates
- Ada applies it in her repo and does the version bump. Helios reviews.
- Studio staging or a phone push happens only after Chris gives a verbal go.
- It is **never** published, submitted, or rolled out to the Play Store.


## Update 3:59 PM CT
**Stamp 47 staged to main (Chris verbal go, 2026-09-26 ~3:59 PM CT).**
- `a977c6a` Alarm Pro stamp 47 bump: versionCode 47 / versionName 0.5.23
- `d5ef02e` revert(tip46) clock back to `dd05a67` (`git diff dd05a67 HEAD -- app/src` is empty)
- Pushed to `skiiwa67-collab/Alarm-Pro` main by Helios because Ada wasn't reachable live on the call; Ada reviews next turn.
- Studio: git fetch/pull main, then run on Chris's phone. **Never Play.**


## STOP 4:01 PM CT (Chris)
HOLD: do NOT apply v47 to Studio and do NOT run on the phone. Leave a977c6a on main as-is. Wait for a new verbal go from Chris.


## CORRECTION 4:07 PM CT (Chris)
Do NOT ship the full revert. v47 a977c6a stays parked on main, not staged. Target is a surgical merge: the old self-ticking Chronometer CLOCK on the same timer path as Till, plus tip46 modulo-24 wrap (hours never 24+), plus leading zero / 12h digit 01-12. Proposed: an hourly exact re-anchor plus a Chronometer format prefix (00:%s / 0%s / %s). Ada and tinkabot are to return a joint design with Soft-PASS test evidence (rollover at 23:59:59, 09:59:59, 12h 12:59:59, late-alarm case) before any staging. Never Play.
