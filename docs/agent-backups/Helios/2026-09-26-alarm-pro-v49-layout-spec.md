# Alarm Pro v49 spec — clock block layout + auto-fit fix (Chris, voice 2026-09-26 17:26 CT)

Base: GitHub main 870b657 (v48, vc48/0.5.24). v48 stays as-is. v49 = vc49/0.5.25 on top. Studio-only after Chris go. Never Play.

1. **Date on one line.** e.g. `September 26` on a single row, never wrapping across two slots. Mil (24h) time line also one solid line.
2. **Stack under the clock, in order:**
   - 12h mode: date line, then AM/PM badge line (right-aligned under the last digit of the time), then mil-time line.
   - 24h mode: date line, then mil-time line (no AM/PM).
3. **Bigger, readable sub-lines.** Scale date / AM-PM / mil lines up to match the clock's visual weight. Date may shrink slightly so a long mil line fits, but everything sits cleanly under the time block.
4. **Auto-fit cap.** `computeFitScales` lets the clock scale up to `MAX_SCALE = 5f` with no 1x cap (since stamp 21, 6466f04), so the size slider can't shrink the clock. Cap clock scale at 1 (`minOf(heightScale, widthScale, 1f)`) and the same 1x cap for tier-2 lines. Slider = max size; auto-fit only shrinks.
5. **No digit clipping.** Last digit sometimes cut off (`05:22:4`). Measure the sample with the real typeface/style used for the TextClock (not default Paint), use widest-digit sample (e.g. `44:44:44` / `47:47:47` in chosen font), and keep a horizontal safety margin so 4/7 never clip.

Keep: TextClock CLOCK, 12h default + 24h toggle both format slots, Till unchanged, noon badge boundary alarm still separate (needs its own go).
Acceptance: slider moves clock size both ways; no clip at any time 00:00:00–23:59:59 in either mode; sub-lines stack as above; assembleDebug + unit tests green; tinkabot verify.

6. **Till line won't grow (Chris, end of call).** Till slider maxed and it stays small. Likely cause: tier 1 lets the clock claim the full height at up to 5x, so tier 2 (Till/alarm/weather) gets the leftovers. The 1x clock cap in item 4 should free that space; verify Till reaches its slider size once the cap is in.

Timing: v48 soaks first (time fix). v49 maybe Tuesday, or Sunday if Chris has time. Go widget waiting in Ada's chat.
