# Gravity-1 Stage 1 silhouette rebuild

## Problem
Chris on tip 153: Stage 1 looks like a solid white background behind the rocket.
Helios audit: canvas outside the hull is already transparent (corner alpha 0).
The white is the hull plate itself — a filled white rectangle with three dark fuel windows (center clear about 24%).
Stages 2/3/4 already have cutout windows (about 48% / 82% / 82%). tip 152 bad was about 0% on those.

## Fix
1. Rebuild vehicle_gravity1_s1.png as a true Orienspace Gravity-1 Stage 1 silhouette rim (core + two SRBs). Not a filled white rectangle.
2. Write matching vehicle_gravity1_booster.png (same bytes alias).
3. Keep the three fuel windows (alpha clear).
4. Do not overwrite stack or side (GOLD side must stay md5 c78f781a40a01676fdb57ae5343ba07b).
5. Do not invent unpublished geometry.
6. Hold Ada wire. Hold phone. Hold Play. Do not copy into hud-assets.

## Sources
- GOLD silhouette reference: /workspace/hud-assets/vehicle_gravity1_side.png
- Current slab: /workspace/agent-backups/Da-Vinci/tip152-gravity1-identity-20260923/vehicle_gravity1_s1.png (md5 6962af66b28c6a41fbbe571145e4d6ed)
- Tank masks: /workspace/hud-assets/vehicle_gravity1_tank_s1_fuel.png and vehicle_gravity1_booster_tank_fills.png

## Deliver pack under EXACTLY
/workspace/agent-backups/Da-Vinci/tip153-gravity1-s1-silhouette/
- vehicle_gravity1_s1.png
- vehicle_gravity1_booster.png
- MD5SUMS
- READY.md
- preview_before_after.png

## Success
No white rectangular slab. Core + SRB rim readable. Three fuel windows still clear. stack/side untouched. hud not updated.
