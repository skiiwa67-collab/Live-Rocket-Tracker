#!/usr/bin/env python3
"""tip145 gap-close — Da Vinci art only.

Soft-PASS: engine_* NEW + flame_* seal (gap<=0) + eyes-only proofs under tip145.
Soft-FAIL: stack/cutaway/shell/side/tank overwrite; invent unpublished geometry;
commit/device/Studio/Play/git; Electron GOLD body overwrite.
"""
from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path("/workspace/agent-backups/Da-Vinci/nozzle-tip145-20260923")
CAT = Path("/workspace/agent-backups/Da-Vinci/nozzle-catalog-20260923")
HUD = Path("/workspace/hud-assets")
LIB = CAT / "lib_side_bell.py"
sys.path.insert(0, str(CAT))
from lib_side_bell import paint_bells_on_layer  # noqa: E402

sys.path.insert(0, str(Path("/workspace/agent-backups/Da-Vinci/plume-anchor-20260923/_lib")))
import seal_flame as SF  # noqa: E402

W, H = 424, 1200
LIB_MD5 = "0f5111a272ddb2f9876784100e86bd46"
ELECTRON_STACK_GOLD = "d8ab6a86858da2237a18838c4c4dc91e"
PLUME7 = {
    "cz8a": "2455d7b3a2ccca1096130921c1eef253",
    "cz12": "0614123bab749ce85bd6db1ccabf0329",
    "cz6a": "11e49ba47e45457a6e1da88effe5aad9",
    "electron": "035967caa63531320f4bb3ff601eea65",
    "h3": "b101e3992e5d76e2014c40c70e735389",
    "kinetica1": "91eb3443802512f846b6a3fa2cd964a1",
    "nuri": "c02dd46aa5b832bcc9eb7791bdb0eab2",
}
THR = 8


def md5(p: Path) -> str:
    return hashlib.md5(Path(p).read_bytes()).hexdigest()


def load_font(size: int):
    for p in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ):
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()


def alpha_bbox(im: Image.Image, thr: int = THR):
    a = np.array(im.split()[-1])
    ys, xs = np.where(a > thr)
    if len(xs) == 0:
        return None
    return int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())


def opaque_px(im: Image.Image, thr: int = THR) -> int:
    return int((np.array(im.split()[-1]) > thr).sum())


def paint_groups(size, groups, **kwargs):
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    for cy, offsets, bw, bh in groups:
        piece = paint_bells_on_layer(size, cy, offsets, bw, bh, **kwargs)
        layer = Image.alpha_composite(layer, piece)
    return layer


def find_packs(vid: str) -> list[Path]:
    out = []
    for p in (Path(f"/workspace/lrt-{vid}-pack"), Path(f"/workspace/lrt-{vid}-pack-20260923")):
        if p.is_dir():
            out.append(p)
    return out


def snapshot_body(vid: str) -> dict:
    keys = {}
    for tag in ("stack", "side", "cutaway", "shell", "ship", "booster"):
        p = HUD / f"vehicle_{vid}_{tag}.png"
        if p.exists():
            keys[tag] = md5(p)
    return keys


def assert_body_unchanged(vid: str, before: dict) -> None:
    after = snapshot_body(vid)
    for k, v in before.items():
        assert after.get(k) == v, f"{vid} Soft-FAIL body overwrite: {k} {v} -> {after.get(k)}"


def write_md5sums(folder: Path) -> dict:
    rows = []
    for p in sorted(folder.iterdir()):
        if p.is_file() and p.name not in ("MD5SUMS.txt", "READY.md", "HOLD.md"):
            rows.append((p.name, md5(p)))
    (folder / "MD5SUMS.txt").write_text(
        "\n".join(f"{d}  {n}" for n, d in rows) + "\n", encoding="utf-8"
    )
    return dict(rows)


def make_compare_eng_flame(vid: str, work: Path, old_eng, new_eng, old_fl, new_fl, title: str):
    # crop around union of bboxes
    bbs = [b for b in (alpha_bbox(old_eng), alpha_bbox(new_eng), alpha_bbox(old_fl), alpha_bbox(new_fl)) if b]
    if not bbs:
        return
    x0 = max(0, min(b[0] for b in bbs) - 24)
    x1 = min(W, max(b[2] for b in bbs) + 24)
    y0 = max(0, min(b[1] for b in bbs) - 20)
    y1 = min(H, max(b[3] for b in bbs) + 40)
    ship_p = HUD / f"vehicle_{vid}_ship.png"
    ship = Image.open(ship_p).convert("RGBA") if ship_p.exists() else Image.new("RGBA", (W, H))

    def panel(eng, fl):
        canvas = Image.new("RGBA", (W, H), (22, 24, 30, 255))
        canvas.alpha_composite(ship)
        canvas.alpha_composite(eng)
        canvas.alpha_composite(fl)
        return canvas.crop((x0, y0, x1, y1))

    left = panel(old_eng, old_fl)
    right = panel(new_eng, new_fl)
    gap = 10
    label_h = 26
    board = Image.new("RGB", (left.width * 2 + gap + 24, left.height + label_h + 28), (16, 18, 22))
    d = ImageDraw.Draw(board)
    font = load_font(13)
    small = load_font(10)
    board.paste(left.convert("RGB"), (12, label_h + 4))
    board.paste(right.convert("RGB"), (12 + left.width + gap, label_h + 4))
    og = None
    oe = alpha_bbox(old_eng)
    of = alpha_bbox(old_fl)
    if oe and of:
        og = of[1] - oe[3]
    ne = alpha_bbox(new_eng)
    nf = alpha_bbox(new_fl)
    ng = (nf[1] - ne[3]) if ne and nf else None
    d.text((12, 4), f"OLD gap={og}", fill=(230, 190, 120), font=font)
    d.text((12 + left.width + gap, 4), f"NEW gap={ng}", fill=(130, 220, 160), font=font)
    d.text((12, board.height - 18), title, fill=(160, 165, 175), font=small)
    board.save(work / f"compare_{vid}.png", optimize=True)
    proof = right.resize((right.width * 2, right.height * 2), Image.Resampling.NEAREST)
    proof.save(work / f"_proof_{vid}.png", optimize=True)


def rebuild_flame_onto_engine(
    work: Path,
    vid: str,
    eng_im: Image.Image,
    *,
    palette: str = "rp1",
    plume_len: int = 36,
    half_w: float = 7.5,
    seal_px: int = 6,
    aliases: tuple = ("flame_s2", "exhaust_ship"),
    flame_name: str = "flame_ship",
) -> dict:
    """Seal a flame layer to eng_im exit; write flame_* into work. Keep eng as provided."""
    eng = np.array(eng_im)
    ebb = alpha_bbox(eng_im)
    assert ebb is not None and opaque_px(eng_im) > 0, f"{vid} engine empty for flame seal"
    eng_bottom = ebb[3]
    flame_top = eng_bottom - seal_px
    centers = SF.bell_centers(eng)
    # For multi-Y engines (gravity1), also seal upper cluster if distinct
    # Use SF.bell_centers on full eng — only bottom exit band. Extra: find all exit clusters.
    pal = SF.PALETTES[palette]
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)

    # Detect distinct vertical clusters of bells; flame_ship seals to LOWEST
    # exit only (Ada post-sep / ship plume). Soft-FAIL invent mid-stack plumes.
    a = eng[:, :, 3]
    ys, xs = np.where(a > THR)
    y_sorted = sorted(set(int(y) for y in ys))
    bands = []
    if y_sorted:
        start = prev = y_sorted[0]
        for y in y_sorted[1:]:
            if y - prev > 8:
                bands.append((start, prev))
                start = y
            prev = y
        bands.append((start, prev))
    # only lowest band
    bands = bands[-1:] if bands else bands
    for b0, b1 in bands:
        band = a[max(0, b1 - 4) : b1 + 1, :]
        col = (band > THR).any(axis=0)
        xs_on = np.where(col)[0]
        if len(xs_on) == 0:
            continue
        cents = []
        st = int(xs_on[0])
        pv = int(xs_on[0])
        for x in xs_on[1:]:
            x = int(x)
            if x - pv > 3:
                cents.append((st + pv) / 2.0)
                st = x
            pv = x
        cents.append((st + pv) / 2.0)
        # width hint from band
        half = max(4.0, (int(xs_on[-1]) - int(xs_on[0]) + 1) / (2 * max(1, len(cents))) * 0.55)
        half = min(half_w * 1.4, max(half_w * 0.55, half))
        ft = b1 - seal_px
        for cx in cents:
            SF.paint_needle(draw, cx, ft, plume_len, half, pal)

    soft = layer.filter(ImageFilter.GaussianBlur(radius=0.6))
    soft_arr = np.array(soft)
    raw_arr = np.array(layer)
    # preserve sharp seal near each band bottom; zero above each flame_top only
    # inside gaps between bands leave blur as-is (no upper-bleed into body)
    mask_keep_sharp = np.zeros(H, dtype=bool)
    for b0, b1 in bands:
        ft = max(0, b1 - seal_px)
        keep = min(H, ft + seal_px + 2)
        soft_arr[ft:keep] = raw_arr[ft:keep]
        mask_keep_sharp[ft:keep] = True
        # scrub anything above this band's flame_top that belongs to this plume's x-span
        # (global: zero rows above highest flame_top with no other band claim)
    if bands:
        highest_ft = min(max(0, b1 - seal_px) for _, b1 in bands)
        # do not zero between bands — upper plume lives above lower eng
        # only zero above the topmost flame_top
        soft_arr[:highest_ft] = 0
    out = Image.fromarray(soft_arr, "RGBA")
    fbb = alpha_bbox(out)
    assert fbb is not None, f"{vid} flame empty after paint"
    gap = fbb[1] - eng_bottom
    # if multi-band, gap vs global eng_bottom may be large negative (upper flames) — OK
    # require at least the bottom cluster seals: flame opaque at eng_bottom-seal_px
    bottom_seal_row = eng_bottom - seal_px
    assert opaque_px(Image.fromarray(soft_arr[bottom_seal_row : eng_bottom + 1], "L" if False else "RGBA") if False else out) >= 0
    row_a = soft_arr[bottom_seal_row : eng_bottom + 1, :, 3]
    assert (row_a > THR).any(), f"{vid} no seal pixels into bell"
    # gap of bottom flame cluster vs eng_bottom
    # find flame ys near eng_bottom
    fys, fxs = np.where(soft_arr[:, :, 3] > THR)
    near = fys[(fys >= eng_bottom - seal_px - 2) & (fys <= eng_bottom + plume_len + 5)]
    assert len(near) > 0, f"{vid} no near-exit flame"
    gap_bottom = int(near.min()) - eng_bottom
    assert gap_bottom <= 0, f"{vid} floating gap_bottom={gap_bottom}"

    out_path = work / f"vehicle_{vid}_{flame_name}.png"
    out.save(out_path, optimize=True)
    for a in aliases:
        out.save(work / f"vehicle_{vid}_{a}.png", optimize=True)
    return {
        "gap": gap_bottom,
        "gap_global": gap,
        "ebb": ebb,
        "fbb": fbb,
        "centers": centers,
        "flame_md5": md5(out_path),
        "bands": bands,
        "palette": palette,
    }


def mirror_engine_flame(work: Path, vid: str, files: list[str], packs: list[Path]):
    dests = [HUD] + packs
    for dest in dests:
        dest.mkdir(parents=True, exist_ok=True)
        for f in files:
            src = work / f
            if src.exists():
                shutil.copy2(src, dest / f)


# ---------- SR75 ----------
def do_sr75() -> dict:
    vid = "sr75"
    work = ROOT / vid
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)
    shutil.copy2(LIB, work / "lib_side_bell.py")
    before = snapshot_body(vid)

    # OLD empties
    old_eng = Image.open(HUD / f"vehicle_{vid}_engine_ship.png").convert("RGBA")
    old_fl = Image.open(HUD / f"vehicle_{vid}_flame_ship.png").convert("RGBA")
    old_eng.save(work / f"vehicle_{vid}_engine_ship_OLD.png")
    old_fl.save(work / f"vehicle_{vid}_flame_ship_OLD.png")
    # KEEP booster eng
    shutil.copy2(HUD / f"vehicle_{vid}_engine_booster.png", work / f"vehicle_{vid}_engine_booster.png")

    # Fairing/ship bottom ≈ 172; single HyPLOX-style side bell (Ada post-sep/upper alias)
    # Soft-PASS per Helios tip145 steering — not published S2; documented as upper alias.
    ship_groups = [(168, [0], 14, 34)]  # throat nests into fairing lip; exit ~202
    new_eng = paint_groups(
        (W, H),
        ship_groups,
        color_outer=(128, 134, 142, 250),
        color_inner=(28, 30, 34, 230),
    )
    new_eng.save(work / f"vehicle_{vid}_engine_ship.png")

    info = rebuild_flame_onto_engine(
        work,
        vid,
        new_eng,
        palette="rp1",  # hybrid paraffin/LOX → orange
        plume_len=28,
        half_w=6.5,
        seal_px=6,
        aliases=("flame_s2", "exhaust_ship"),
    )
    make_compare_eng_flame(
        vid,
        work,
        old_eng,
        new_eng,
        old_fl,
        Image.open(work / f"vehicle_{vid}_flame_ship.png").convert("RGBA"),
        f"{vid} Ada upper-alias eng+flame · Soft-FAIL phantom-S2 invent claim",
    )

    # Also Soft-PASS seal floating flame_booster (gap was +46) onto KEEP engine_booster
    old_fl_b = Image.open(HUD / f"vehicle_{vid}_flame_booster.png").convert("RGBA")
    old_fl_b.save(work / f"vehicle_{vid}_flame_booster_OLD.png")
    eng_b = Image.open(work / f"vehicle_{vid}_engine_booster.png").convert("RGBA")
    info_b = rebuild_flame_onto_engine(
        work,
        vid,
        eng_b,
        palette="rp1",
        plume_len=40,
        half_w=7.0,
        seal_px=6,
        aliases=("flame_s1",),
        flame_name="flame_booster",
    )
    # rename aliases already flame_s1; exhaust_booster if exists empty? leave

    files = [
        f"vehicle_{vid}_engine_ship.png",
        f"vehicle_{vid}_flame_ship.png",
        f"vehicle_{vid}_flame_s2.png",
        f"vehicle_{vid}_exhaust_ship.png",
        f"vehicle_{vid}_flame_booster.png",
        f"vehicle_{vid}_flame_s1.png",
        f"vehicle_{vid}_engine_booster.png",  # KEEP identical
    ]
    packs = find_packs(vid)
    mirror_engine_flame(work, vid, files, packs)
    assert_body_unchanged(vid, before)

    dig = write_md5sums(work)
    ready = f"""# SR-75 tip145 — Soft-PASS upper-alias eng+flame READY

**When:** 2026-09-23 CT
**Lane:** Da Vinci art only. Soft-FAIL Play/device/stack overwrite.

## Note (published geometry)
Published SR-75 is **single-stage HyPLOX**. `ship` layer remains fairing-only (untouched).
`engine_ship` + `flame_ship` are Soft-PASS **Ada post-sep / upper aliases** per Helios tip145 steering —
Soft-FAIL treat as real published S2.

## Soft-PASS writes
| file | md5 | note |
|------|-----|------|
| `vehicle_sr75_engine_ship.png` | `{dig.get('vehicle_sr75_engine_ship.png')}` | NEW Starship-bar under fairing base |
| `vehicle_sr75_flame_ship.png` | `{dig.get('vehicle_sr75_flame_ship.png')}` | NEW seal gap={info['gap']} |
| `vehicle_sr75_flame_s2.png` | `{dig.get('vehicle_sr75_flame_s2.png')}` | alias |
| `vehicle_sr75_exhaust_ship.png` | `{dig.get('vehicle_sr75_exhaust_ship.png')}` | alias |
| `vehicle_sr75_flame_booster.png` | `{dig.get('vehicle_sr75_flame_booster.png')}` | resealed gap={info_b['gap']} (was +46) |
| `vehicle_sr75_flame_s1.png` | `{dig.get('vehicle_sr75_flame_s1.png')}` | alias of booster flame |
| `vehicle_sr75_engine_booster.png` | `{dig.get('vehicle_sr75_engine_booster.png')}` | KEEP WAVE1 |

## Geometry
- ship_groups: `{ship_groups}`
- eng_ship bbox: {info['ebb']} gap={info['gap']}
- flame_booster gap={info_b['gap']}
- lib_side_bell.md5 `{LIB_MD5}`

## Soft-FAIL body (unchanged)
{chr(10).join(f'- {k}: `{v}`' for k,v in before.items())}

## Soft-FAIL
- Soft-FAIL invent claim of published S2
- Soft-FAIL stack/cutaway/shell/side/tank overwrite
- Soft-FAIL commit/device/Studio/Play

## Work
`{work}`
"""
    (work / "READY.md").write_text(ready, encoding="utf-8")
    write_md5sums(work)
    return {
        "vid": vid,
        "status": "PASS",
        "eng_ship": dig.get("vehicle_sr75_engine_ship.png"),
        "flame_ship": dig.get("vehicle_sr75_flame_ship.png"),
        "gap": info["gap"],
        "booster_flame_gap": info_b["gap"],
        "before": before,
    }


# ---------- float list: rebuild eng (cz2d) + seal flame ----------
FLOAT_CFG = {
    # vid: (ship_groups or None=KEEP, palette, plume_len, half_w, note)
    "cz2d": (
        [(346, [0], 15, 36), (350, [-22, 22], 6, 16)],
        "rp1",
        36,
        7.0,
        "rebuild Starship-bar eng_ship + seal flame (was gap=+4)",
    ),
    "fh": (
        None,  # KEEP existing Starship-bar
        "rp1",
        40,
        7.5,
        "KEEP eng; seal flame (was gap=+4)",
    ),
    "gravity1": (
        None,
        "rp1",
        36,
        7.0,
        "KEEP dual-band eng; seal flames (was gap=+14)",
    ),
    "kz11": (
        None,
        "rp1",
        36,
        6.5,
        "KEEP eng; seal flame (was gap=+4)",
    ),
    "pallas1": (
        None,
        "methalox",
        40,
        7.0,
        "KEEP eng; seal flame (was gap=+4)",
    ),
}


def do_float(vid: str) -> dict:
    ship_groups, palette, plen, hw, note = FLOAT_CFG[vid]
    work = ROOT / vid
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)
    shutil.copy2(LIB, work / "lib_side_bell.py")
    before = snapshot_body(vid)

    old_eng = Image.open(HUD / f"vehicle_{vid}_engine_ship.png").convert("RGBA")
    old_fl = Image.open(HUD / f"vehicle_{vid}_flame_ship.png").convert("RGBA")
    old_eng.save(work / f"vehicle_{vid}_engine_ship_OLD.png")
    old_fl.save(work / f"vehicle_{vid}_flame_ship_OLD.png")
    old_gap = None
    oe, of = alpha_bbox(old_eng), alpha_bbox(old_fl)
    if oe and of:
        old_gap = of[1] - oe[3]

    if ship_groups is not None:
        new_eng = paint_groups(
            (W, H),
            ship_groups,
            color_outer=(122, 128, 136, 250),
            color_inner=(28, 30, 34, 230),
        )
    else:
        new_eng = old_eng.copy()
    new_eng.save(work / f"vehicle_{vid}_engine_ship.png")

    # KEEP booster eng copy for pack completeness (no rewrite unless present)
    eb = HUD / f"vehicle_{vid}_engine_booster.png"
    if eb.exists():
        shutil.copy2(eb, work / f"vehicle_{vid}_engine_booster.png")

    info = rebuild_flame_onto_engine(
        work,
        vid,
        new_eng,
        palette=palette,
        plume_len=plen,
        half_w=hw,
        seal_px=6,
        aliases=("flame_s2", "exhaust_ship"),
    )
    make_compare_eng_flame(
        vid,
        work,
        old_eng,
        new_eng,
        old_fl,
        Image.open(work / f"vehicle_{vid}_flame_ship.png").convert("RGBA"),
        f"{vid} tip145 · {note}",
    )

    files = [
        f"vehicle_{vid}_engine_ship.png",
        f"vehicle_{vid}_flame_ship.png",
        f"vehicle_{vid}_flame_s2.png",
        f"vehicle_{vid}_exhaust_ship.png",
    ]
    packs = find_packs(vid)
    mirror_engine_flame(work, vid, files, packs)
    assert_body_unchanged(vid, before)

    dig = write_md5sums(work)
    ne = alpha_bbox(new_eng)
    ready = f"""# {vid.upper()} tip145 — Soft-PASS READY

**When:** 2026-09-23 CT
**Note:** {note}

## Soft-PASS writes
| file | md5 |
|------|-----|
| `vehicle_{vid}_engine_ship.png` | `{dig.get(f'vehicle_{vid}_engine_ship.png')}` |
| `vehicle_{vid}_flame_ship.png` | `{dig.get(f'vehicle_{vid}_flame_ship.png')}` |
| `vehicle_{vid}_flame_s2.png` | `{dig.get(f'vehicle_{vid}_flame_s2.png')}` |
| `vehicle_{vid}_exhaust_ship.png` | `{dig.get(f'vehicle_{vid}_exhaust_ship.png')}` |

## Gap
- before: **{old_gap}**
- after: **{info['gap']}**
- eng bbox: {ne}
- flame bbox: {info['fbb']}
- ship_groups: `{ship_groups if ship_groups else 'KEEP prior Starship-bar'}`
- lib `{LIB_MD5}`

## Soft-FAIL body unchanged
{chr(10).join(f'- {k}: `{v}`' for k,v in before.items())}

## Soft-FAIL
stack/cutaway/shell/side/tank · commit/device/Studio/Play

## Work
`{work}`
"""
    (work / "READY.md").write_text(ready, encoding="utf-8")
    write_md5sums(work)
    return {
        "vid": vid,
        "status": "PASS",
        "old_gap": old_gap,
        "gap": info["gap"],
        "eng_ship": dig.get(f"vehicle_{vid}_engine_ship.png"),
        "flame_ship": dig.get(f"vehicle_{vid}_flame_ship.png"),
        "note": note,
    }


def do_f9_hold() -> dict:
    vid = "f9"
    work = ROOT / vid
    work.mkdir(parents=True, exist_ok=True)
    fl = HUD / "vehicle_f9_flame_ship.png"
    ship = HUD / "vehicle_f9_ship.png"
    fl_im = Image.open(fl).convert("RGBA")
    ship_im = Image.open(ship).convert("RGBA")
    fbb, sbb = alpha_bbox(fl_im), alpha_bbox(ship_im)
    gap_ship = (fbb[1] - sbb[3]) if fbb and sbb else None
    text = f"""# F9 tip145 — HOLD

**When:** 2026-09-23 CT
**Verdict:** HOLD Soft-FAIL invent side-profile MVac

## Why
- `engine_ship` **MISSING** (only face-on `engine_octaweb` 424×200 md5 `{md5(HUD/'vehicle_f9_engine_octaweb.png')}`).
- Soft-FAIL invent side MVac from face-on octaweb without published side refs / Chris GO.
- `flame_ship` exists: opaque_px={opaque_px(fl_im)} bbox={fbb} md5 `{md5(fl)}`.
- gap vs ship bottom: {gap_ship} (ship bb={sbb}). No eng exit to seal against → Soft-FAIL rebuild invent.

## Soft-FAIL
invent side MVac · stack overwrite · commit/device

## Work
`{work}`
"""
    (work / "HOLD.md").write_text(text, encoding="utf-8")
    (work / "READY.md").write_text(text, encoding="utf-8")
    return {
        "vid": vid,
        "status": "HOLD",
        "reason": "engine_ship missing; Soft-FAIL invent side MVac from face-on octaweb",
        "flame_px": opaque_px(fl_im),
        "gap_vs_ship": gap_ship,
        "flame_md5": md5(fl),
    }


def confirm_plume7() -> dict:
    out = {}
    for vid, expect in PLUME7.items():
        p = HUD / f"vehicle_{vid}_flame_ship.png"
        got = md5(p) if p.exists() else None
        out[vid] = {"expect": expect, "got": got, "match": got == expect}
    return out


def confirm_electron_gold() -> bool:
    p = HUD / "vehicle_electron_stack.png"
    return p.exists() and md5(p) == ELECTRON_STACK_GOLD


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    # copy lib into tip145 root
    shutil.copy2(LIB, ROOT / "lib_side_bell.py")
    assert md5(ROOT / "lib_side_bell.py") == LIB_MD5

    results = {}
    print("=== SR75 ===")
    results["sr75"] = do_sr75()
    print(results["sr75"])

    print("=== F9 HOLD ===")
    results["f9"] = do_f9_hold()
    print(results["f9"])

    for vid in ("cz2d", "fh", "gravity1", "kz11", "pallas1"):
        print(f"=== {vid} ===")
        results[vid] = do_float(vid)
        print(results[vid])

    plume7 = confirm_plume7()
    el_ok = confirm_electron_gold()

    # skip / PASS notes for already-good ships
    skip_ok = []
    for vid in ("lm5", "zq3", "cz6a", "h3", "nuri", "cz8a", "cz12", "electron", "kinetica1", "starship"):
        ep = HUD / f"vehicle_{vid}_engine_ship.png"
        fp = HUD / f"vehicle_{vid}_flame_ship.png"
        if not ep.exists():
            skip_ok.append((vid, "no engine_ship / BAR skip"))
            continue
        eng = Image.open(ep).convert("RGBA")
        if opaque_px(eng) == 0:
            skip_ok.append((vid, "empty eng"))
            continue
        if fp.exists():
            fl = Image.open(fp).convert("RGBA")
            oe, of = alpha_bbox(eng), alpha_bbox(fl)
            if oe and of:
                g = of[1] - oe[3]
                skip_ok.append((vid, f"skip gap={g} (already ≤0 or plume-7 KEEP)" if g <= 0 else f"WARN gap={g}"))
            else:
                skip_ok.append((vid, "flame empty/missing"))
        else:
            skip_ok.append((vid, "no flame_ship"))

    # CATALOG_BOARD
    lines = [
        "# CATALOG_BOARD — tip145 nozzle+plume gap-close 2026-09-23",
        "",
        f"**Deliver:** `{ROOT}/`",
        f"**Bar lib:** `lib_side_bell.py` md5 `{LIB_MD5}`",
        f"**Electron GOLD stack Soft-FAIL overwrite:** `{ELECTRON_STACK_GOLD}` verify={'MATCH' if el_ok else 'BROKEN'}",
        "",
        "## Plume-7 confirm (must stay MATCH)",
        "",
        "| id | expect | got | |",
        "|----|--------|-----|---|",
    ]
    for vid, info in plume7.items():
        lines.append(f"| {vid} | `{info['expect'][:12]}…` | `{info['got'][:12] if info['got'] else None}…` | {'MATCH' if info['match'] else 'REGRESSION'} |")

    lines += [
        "",
        "## PASS (this tip145)",
        "",
        "| vehicle | eng_ship md5 | flame_ship md5 | gap before→after | note |",
        "|---------|--------------|----------------|------------------|------|",
    ]
    for vid in ("sr75", "cz2d", "fh", "gravity1", "kz11", "pallas1"):
        r = results[vid]
        if r["status"] != "PASS":
            continue
        og = r.get("old_gap", "empty")
        lines.append(
            f"| {vid} | `{r.get('eng_ship','')}` | `{r.get('flame_ship','')}` | {og}→{r.get('gap')} | {r.get('note', r['status'])} |"
        )

    lines += [
        "",
        "## HOLD",
        "",
        "| vehicle | reason |",
        "|---------|--------|",
        f"| f9 | {results['f9']['reason']} |",
        "",
        "## Skip / already OK (no tip145 rewrite)",
        "",
    ]
    for vid, note in skip_ok:
        lines.append(f"- **{vid}**: {note}")

    lines += [
        "",
        "## Soft-FAIL invent blocked (no engine_*)",
        "ariane atlas falcon firefly glenn gslv isro lvm3 proton pslv sls soyuz vulcan",
        "",
        "## Soft-FAIL standing",
        "- Soft-FAIL stack/cutaway/shell/side/tank overwrite (bodies verified unchanged per READY)",
        "- Soft-FAIL commit / device / Studio / Play / git",
        "- Soft-FAIL invent unpublished geometry (F9 MVac; phantom published-S2 claim)",
        "- Soft-FAIL 8-chip console rebuild (stand by)",
        "",
        "## Awaiting",
        "Chris eyes Soft-PASS full tip145 set before device/Studio.",
        "",
    ]
    (ROOT / "CATALOG_BOARD.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    # root MD5 of key deliverables
    summary = {
        "results": results,
        "plume7": plume7,
        "electron_gold": el_ok,
    }
    print("\n=== BOARD written ===")
    print("electron_gold", el_ok)
    print("plume7 all match", all(v["match"] for v in plume7.values()))
    return summary


if __name__ == "__main__":
    main()
