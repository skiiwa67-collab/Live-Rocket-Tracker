# FLEET_TANK_FIT_AUDIT — tip154

**When:** 2026-09-23 CT
**Pack:** `/workspace/agent-backups/Da-Vinci/tip154-tank-fit-20260923/`
**Metric:** mid-row `fill_width / core_window_width` vs cutaway; outside% fill opaque outside cutaway opaque
**PASS bar:** center tanks 80–88%, outside%=0, Soft-FAIL <70%

## Soft-FAIL held
- Soft-FAIL stack/side/cutaway/shell/engines/flames overwrite
- Soft-FAIL Ada/hud/phone/Play/ship copy
- Electron GOLD `d8ab6a86…` Soft-PASS held; G1 GOLD `c78f781a…` Soft-PASS held

## Family rollup

| family | action | status | notes |
|--------|--------|--------|-------|
| `electron` | REBUILT | **PASS** | inset to shell gap; uniform 0.803 |
| `gravity1` | REBUILT | **PASS** | center 0.505→0.838; sides KEEP 0.848 |
| `cz8a` | KEEP | **KEEP_PASS** | core-window PASS (raw seg 0.37 is merged span) |
| `cz6a` | KEEP | **KEEP_PASS** |  |
| `cz2d` | REBUILT | **PASS** |  |
| `f9` | NO_CUTAWAY | **NO_CUTAWAY** |  |
| `sr75` | KEEP | **KEEP_PASS** |  |
| `nuri` | KEEP | **KEEP_PASS** |  |
| `h3` | KEEP | **KEEP_PASS** |  |
| `kz11` | REBUILT | **PASS** |  |
| `cz12` | REBUILT | **PASS** |  |
| `fh` | KEEP | **KEEP_PASS** |  |
| `kinetica1` | REBUILT | **PASS** |  |
| `lm5` | REBUILT | **PASS** |  |
| `pallas1` | KEEP | **KEEP_PASS** |  |
| `zq3` | REBUILT | **PASS** |  |

## Per-island detail (rebuilt)

### `electron` (ref_core 61→61)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 247 | center | 51/61=0.836 | 49/61=0.803 | 0.00 |
| 309 | center | 51/61=0.836 | 49/61=0.803 | 0.00 |
| 434 | center | 51/61=0.836 | 49/61=0.803 | 0.00 |
| 632 | center | 51/61=0.836 | 49/61=0.803 | 0.00 |

### `gravity1` (ref_core 105→105)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 384 | center | 53/105=0.505 | 88/105=0.838 | 0.00 |
| 520 | center | 53/105=0.505 | 88/105=0.838 | 0.00 |
| 757 | side | 39/46=0.848 | 39/46=0.848 | 0.00 |
| 774 | center | 53/105=0.505 | 88/105=0.838 | 0.00 |

### `cz2d` (ref_core 210→210)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 218 | center | 112/210=0.533 | 176/210=0.838 | 0.00 |
| 284 | center | 112/210=0.533 | 176/210=0.838 | 0.00 |
| 436 | center | 112/210=0.533 | 176/210=0.838 | 0.00 |
| 642 | center | 112/210=0.533 | 176/210=0.838 | 0.00 |

### `kz11` (ref_core 201→201)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 191 | center | 103/201=0.512 | 169/201=0.841 | 0.00 |
| 227 | center | 103/201=0.512 | 169/201=0.841 | 0.00 |
| 317 | center | 103/201=0.512 | 169/201=0.841 | 0.00 |
| 470 | center | 103/201=0.512 | 169/201=0.841 | 0.00 |
| 733 | center | 103/201=0.512 | 169/201=0.841 | 0.00 |

### `cz12` (ref_core 216→216)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 258 | center | 73/216=0.338 | 181/216=0.838 | 0.00 |
| 342 | center | 73/216=0.338 | 181/216=0.838 | 0.00 |
| 459 | center | 176/216=0.815 | 176/216=0.815 | 0.00 |
| 725 | center | 176/216=0.815 | 176/216=0.815 | 0.00 |

### `kinetica1` (ref_core 211→211)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 239 | center | 51/211=0.242 | 177/211=0.839 | 0.00 |
| 333 | center | 67/211=0.318 | 177/211=0.839 | 0.00 |
| 466 | center | 77/211=0.365 | 177/211=0.839 | 0.00 |
| 648 | center | 121/211=0.573 | 177/211=0.839 | 0.00 |

### `lm5` (ref_core 169→169)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 268 | center | 85/169=0.503 | 142/169=0.840 | 0.00 |
| 354 | center | 85/169=0.503 | 142/169=0.840 | 0.00 |
| 510 | center | 85/169=0.503 | 142/169=0.840 | 0.00 |
| 625 | side | 41/49=0.837 | 41/49=0.837 | 0.00 |
| 673 | center | 85/169=0.503 | 142/169=0.840 | 0.00 |
| 772 | side | 41/49=0.837 | 41/49=0.837 | 0.00 |

### `zq3` (ref_core 206→206)

| y | role | before fw/core | after fw/core | outside% |
|---|------|----------------|---------------|----------|
| 227 | center | 112/208=0.538 | 175/208=0.841 | 0.00 |
| 319 | center | 110/206=0.534 | 173/206=0.840 | 0.00 |
| 485 | center | 136/256=0.531 | 215/256=0.840 | 0.00 |
| 667 | center | 113/209=0.541 | 176/209=0.842 | 0.00 |

