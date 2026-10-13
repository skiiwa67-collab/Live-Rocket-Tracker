# 1.5.6.2 fix 9 — event tape centers on the event happening now

**Effective:** 2026-09-29 ~08:11 CT
**Owner:** Chris Olsen (via Helios)
**Status:** told to Ada. Not coded. Not Studio. No Play.

The tape centers on the event happening now.

Left of center is past events. Before launch those are prop load, the countdown, and live feeds from about 30 minutes out. The tape does not start running until prop load begins.

Right of center is upcoming events. Their text is shrunk so it does not override the current event.

An event that is happening is on the top of the tape. An event that is coming up is on the bottom. As it gets closer, its text grows until it pops to the top and is highlighted. When it moves past its time, it pops back to the bottom, and the next upcoming event pops to the top. No event hits the center until it actually happens.

Example: when liftoff rolls past prop load, prop load drops to the bottom. Liftoff pops to the top but sits right of center. A few seconds before liftoff it is at the top as it reaches center. After it passes center it drops to the bottom as a past event, and the next event pops to the top.

Starship rollout, booster test, and roll-back are deferred.

This does not replace the 154 pause and resume restore. The other eight fixes on this list were not written here, because they were not enumerated in one place.

## Screen size

Dynamic, not pixel-based. Center lock, top and bottom layering, text growth, and shrink scale with the display. No fixed pixels or hard-coded positions. The same rule applies to all nine 1.5.6.2 fixes. Do not break the tape.
