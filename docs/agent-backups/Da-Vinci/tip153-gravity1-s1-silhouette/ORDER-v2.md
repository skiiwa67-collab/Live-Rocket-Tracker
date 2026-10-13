# Gravity-1 Stage 1 silhouette rebuild v2

## Problem
Chris phone tip 153 at T+02:43 STAGE SEP: STG1 still looks like a solid white box.
Shot: /home/box/agent-data/agents/fe0b6c3f-adcf-483f-a7f9-335890abd80d/attachments/578edc6c9db0d0e30d7757e9406348aa71c235fa523bd7b09bf70694a14404bb.png
Prior pack tip153-gravity1-s1-silhouette s1 md5 f5dc3cbc still measures bbox fill about 88 percent (one white slab). SRBs are not separate.

## Required look
True Orienspace Gravity-1 Stage 1 after sep:
- Central core silhouette PLUS two SRB silhouettes
- Transparent gaps between core and each SRB (Chris must see three bodies, not one white rectangle)
- Keep three fuel windows (center core + one per SRB) alpha clear
- GOLD side reference: /workspace/hud-assets/vehicle_gravity1_side.png md5 c78f781a40a01676fdb57ae5343ba07b
  Crop the Stage-1 / SRB band (about y=669 to 1034) for the rim. Do not flatten SRBs into the core.

## Do not
- Do not overwrite stack or side
- Do not invent unpublished geometry (no grid fins; Gravity-1 has none)
- Do not copy into hud-assets
- Hold Ada wire, phone, and Play until Helios clears the pack

## Deliver
Overwrite pack /workspace/agent-backups/Da-Vinci/tip153-gravity1-s1-silhouette/
- vehicle_gravity1_s1.png
- vehicle_gravity1_booster.png (same bytes)
- MD5SUMS
- READY.md
- preview_before_after.png (prior slab vs new three-body silhouette on dark bg)

## Success
bbox fill of opaque pixels inside the bounding box should drop well below 88 percent because of gaps between SRBs.
Three-body silhouette readable at HUD STG1 plate size. Fuel windows still clear.
