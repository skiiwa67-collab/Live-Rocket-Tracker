# Bug-Out Notion hub re-tier (Skippy, 2026-10-13)
Repo one-to-many @ 0df3eab (app 0.6.6: 05860d0 = 0.6.4, 5a610fd/b0d3fc0 = 0.6.5, af3608b = 0.6.6, 0df3eab = docs). The split was changed mid-task: Skippy took pages 5-7, Open decisions and Doesn't fit cleanly; tinkabot took 1-4. /workspace/agent-backups/Team/bugout-fixed-today.md does not exist.

Hub 3f8a94933faa81f7a719c4df0de44ff1: 1) Security 3f8a94933faa817190c5ca50d399cf3c · 2) Family handling 3f8a94933faa813c84a4fabce968a2ae · 3) Routing 3f8a94933faa8117949aeb647d2b3717 · 4) Logic layer 3f8a94933faa811dbd2bfdbe79bd23e6 · 5) Data and display 3f8a94933faa81228e68ffbac884f74b · 6) Messaging 3f8a94933faa81a4ad0bd3b768540f31 · 7) Hardware and infrastructure 3f8a94933faa81c99c27d111348d6aac · Open decisions 3f8a94933faa81d58b4fd8d6ceef5ea2 · Doesn't fit cleanly 3f8a94933faa8169adbae04be1bd784c

## Pages edited by Skippy (checklist sections only)
- **5) Data and display**: 11 done / 1 designed / 6 open (category toggles moved to designed, decided today; low-battery item cross-refs the 0.6.6 battery warning). New done item: units toggle, default miles (af3608b). Open: map fonts and sprites still load from protomaps.github.io (dfw-style-dark.json:1), so the map is not fully offline.
- **6) Messaging**: 6 done / 1 designed / 5 open. Done: ack, DELIVERED, in-reply-to (Family.kt:76-117). Open: relay dedup/TTL/hop/cap (#7), no READ status, no E2E encryption. Designed: signed envelopes + hash chain (PR #1, not merged).
- **7) Hardware and infrastructure**: 3 done / 5 designed / 3 open (Starlink mesh + dark spots moved to designed; H-6 marked done in af3608b in Audit holes). The battery final card (FinalCard.kt, af3608b) is now done. Starlink is still demo-only (MainActivity.kt:525).
- **Open decisions**: 7 open, all for Chris / 2 resolved (the safety-first merge is coded in 0.6.4, FeatureMenu.kt:69-70; authority route as a default with TTL is decided in 0df3eab). Added the five-tier mapping question, since 0.6.6 says the rework waits on Chris.
- **Doesn't fit cleanly**: notes added to 4 rows. G2-6 and G8-3 are STILL OPEN, slated for Ada's 0.6.7. Low-battery/final card and drill separation are done in code.

## Audit re-check (#1, #2, #9): Eve is right, these are DESIGNED BUT NOT CODED
- #1 tier model: the five-tier model is in SECURITY-NOTES.md (0df3eab); the code still has 3 tiers (Authority.kt:9-11).
- #2 verification: the "claimed at A0" design is in SECURITY-NOTES §3 and Five-tier §8. There is no verifier in the code: authorityVerified (Routing.kt:252) defaults to false and nothing sets it.
- #9 stale TTL: designed in PROGRAM-LOGIC.md "Authority route default" (0df3eab: 30 min near a hazard front / 2 h otherwise). No TTL in the code.

## Cross-check notes for tinkabot (pages 1-4, not edited)
- Page 4 Logic layer has not been re-tiered (still the old "Requirements" list). It marks "[x] Records carry UUID, origin signature, HLC, version, TTL, tombstones", which is false in the app: SyncDelta has no signature/HLC/TTL/version (Authority.kt:31-32), and page 1 says PR #1 is not merged. #9 should be DESIGNED, not open.
- Page 3 Routing: the DONE list still contains two unchecked leftovers, "Offline road routing engine (Valhalla DFW ready, not wired)", which contradicts the done Valhalla item, and "Navigation without GPS". There are no DESIGNED or STILL OPEN headings.
- Page 2: "Kids' live-location privacy (Skippy #12)" is mostly #12's privacy tail; the main part of #12 was straight-line routing (now fixed in 0.6.5).
- Pages 5-7 audit-hole sections (not rewritten, only the checklist was): page 7 still says "Tinkabot H-6: no final card (open)", which is done in af3608b. Page 5 Summary still reads fine.
