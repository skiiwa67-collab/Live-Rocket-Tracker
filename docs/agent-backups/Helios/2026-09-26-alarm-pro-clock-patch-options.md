# Alarm Pro clock freeze: two patch options (2026-09-26)

## What is broken
Tip46 (`8bd76cb`, Sep 23) turned off the clock's own ticking (`setChronometer(..., started = false)`) and paints the digits once a second from `WallpaperTickService`. When Android pauses or kills that service (screen off, battery saver, Doze), the clock freezes on the last painted second. The Till countdown keeps ticking because it still runs on its own.

Current `main` is still `8bd76cb`; the two commits after it are docs only.

## Option 1 (recommended now): revert the clock to pre-tip46 code from `dd05a67`
- The clock ticks on its own again and no longer depends on the tick service.
- Anchored to midnight in 24-hour mode, or to noon/midnight in 12-hour mode (keeps the 12-o'clock hour fix).
- Small change that touches only the clock path. The Till countdown is untouched.
- Downside: hours before 10 show without a leading zero, e.g. `9:05:12` instead of `09:05:12` (the display you had before Sep 23).

## Option 2 (follow-up): replace the clock line with Android's built-in TextClock
- Uses the `hh:mm:ss` format, and Android keeps it ticking itself.
- Gives real zero-padded six digits.
- Needs a layout change for each font style, so it's more work for Ada and more to test.

## Recommendation
Do Option 1 now so the app is usable. Do Option 2 later if you want padded digits.

## Rules
- Ada makes the change in her Alarm Pro repo.
- Helios reviews it.
- It's staged in Studio only after Chris gives a verbal go.
- It is never published to the Play Store.
