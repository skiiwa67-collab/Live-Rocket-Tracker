# Live and historic panels

Art and layout only. Not wired into Studio. Phone stays on 154. No Play.

The entertainment disclaimer stays in `box_disclaimer_main`, directly under the app title. Do not put the historic note there. Do not cover Follow, Units, or Home screens.

## Live panel
New box inside `section_telemetry`. Suggested id `panel_live`.
Holds the live controls, in this order:
- `btn_list_current` (label CURRENT). Same click, same colors. Only its position changes.
- `row_horizon` (Upcoming window: 1 WK, 1 MO, 6 MO). Still hidden while Historic is selected.
- `spinner_launch` while Current is selected. It is still one spinner. It sits in this panel only in Current mode.

## Historic panel
Suggested id `panel_historic`.
Holds the historic controls, in this order:
- `btn_list_historical` (label HISTORIC). Same click, same colors. Only its position changes.
- `row_search` (Search historic). Still hidden while Current is selected.
- `spinner_launch` while Historic is selected.
- At the very bottom of this panel, always visible: the historic data disclaimer.

## Shared, outside both panels
`btn_refresh_launches` and `txt_launch_status` stay under the two panels. Both modes already use them.
Follow, Units, and Home screens stay below that, untouched.

## Historic disclaimer
Same treatment as `bg_disclaimer_main`: fill #2A2208, gold stroke #F2C14E at 2dp, 8dp corners, 12dp padding.
Title #F2C14E, 16sp, bold: HISTORIC DATA
Body #FFE8A0, 13sp, bold:
Historic profiles use public reports. We add anomalies when we can find them, without digging into private systems. A monthly data push updates past launches as better public data turns up. A real-time failure or anomaly may not show correctly until that data push.

The monthly update is a data push. Do not call it a patch.

Boards: layout-current.png and layout-historic.png. Card: historic-data-disclaimer.png.
