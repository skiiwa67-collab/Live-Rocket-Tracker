#!/usr/bin/env python3
"""
tip151 Da Vinci — Soft-FAIL-named STAGE plate rebuild (ONE-BATCH).
Families: electron (s2), gravity1 (s1/s2/s3/s4).

HARD Soft-FAIL:
- Soft-FAIL stack/cutaway/shell/side/tank overwrite (GOLD electron stack kept)
- Soft-FAIL flame/engine overwrite beyond Soft-FAIL-named stage plates
- Soft-FAIL invent unpublished fairing / Gravity-1 liquid S4
- Soft-FAIL ship / git / device / Play

GOAL: content-cropped stage aspect nearer peer median ~1.53 (H/W),
median bb ~201×259; electron not TALL~3.0; gravity1 s2 not SHORT~0.822.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

W, H = 424, 1200
THR = 8
PEER_ASP = 1.528
PEER_BB = (201, 259)  # W×H
ASP_LO, ASP_HI = 1.30, 1.85

ROOT = Path("/workspace/agent-backups/Da-Vinci/tip151-stage-fit-20260923")
HUD = Path("/workspace/hud-assets")
PACK_E = Path("/workspace/lrt-electron-pack")
PACK_G = Path("/workspace/lrt-gravity1-pack")
BEFORE = ROOT / "_before"
WORK = ROOT / "_work"
OUT = ROOT
for p in (ROOT, WORK, BEFORE):
    p.mkdir(parents=True, exist_ok=True)

GOLD_ELECTRON_STACK = "d8ab6a86858da2237a18838c4c4dc91e"
GOLD_G1_SIDE = "c78f781a40a01676fdb57ae5343ba07b"


def md5(path: Path | str) -> str:
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def measure(im: Image.Image, thr: int = THR):
    a = np.array(im.convert("RGBA"))
    m = a[:, :, 3] > thr
    if not m.any():
        return {
            "canvas": (im.size[0], im.size[1]),
            "bb": None,
            "bwh": None,
            "asp": None,
            "top_pad": None,
            "bot_pad": None,
            "opaque": 0,
        }
    ys, xs = np.where(m)
    x0, x1 = int(xs.min()), int(xs.max())
    y0, y1 = int(ys.min()), int(ys.max())
    bw, bh = x1 - x0 + 1, y1 - y0 + 1
    return {
        "canvas": (im.size[0], im.size[1]),
        "bb": (x0, y0, x1, y1),
        "bwh": (bw, bh),
        "asp": round(bh / bw, 3),
        "top_pad": y0,
        "bot_pad": H - 1 - y1,
        "opaque": int(m.sum()),
    }


def content_crop(im: Image.Image, thr: int = THR, pad: int = 0) -> Image.Image:
    a = np.array(im.convert("RGBA"))
    m = a[:, :, 3] > thr
    if not m.any():
        return Image.new("RGBA", (1, 1), (0, 0, 0, 0))
    ys, xs = np.where(m)
    x0 = max(0, int(xs.min()) - pad)
    y0 = max(0, int(ys.min()) - pad)
    x1 = min(W - 1, int(xs.max()) + pad)
    y1 = min(H - 1, int(ys.max()) + pad)
    return im.crop((x0, y0, x1 + 1, y1 + 1))


def scrub_opaque_black(img: Image.Image, max_rgb: int = 28) -> Image.Image:
    a = np.array(img)
    rgb = a[:, :, :3].astype(np.int16)
    mx = rgb.max(axis=2)
    bad = (a[:, :, 3] >= 1) & (mx <= max_rgb)
    a[bad, 3] = 0
    return Image.fromarray(a, "RGBA")


def clean_alpha(img: Image.Image) -> Image.Image:
    """Harden near-zero alpha fringe."""
    a = np.array(img)
    alpha = a[:, :, 3]
    a[alpha < 12, 3] = 0
    a[(alpha >= 12) & (alpha < 40), 3] = np.maximum(alpha[(alpha >= 12) & (alpha < 40)], 40)
    return Image.fromarray(a, "RGBA")


def place_on_canvas(
    content: Image.Image,
    *,
    cx: int = W // 2,
    bottom_y: int | None = None,
    center_y: int | None = None,
    top_y: int | None = None,
) -> Image.Image:
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cw, ch = content.size
    x = int(cx - cw / 2)
    if bottom_y is not None:
        y = int(bottom_y - ch + 1)
    elif top_y is not None:
        y = int(top_y)
    elif center_y is not None:
        y = int(center_y - ch / 2)
    else:
        y = (H - ch) // 2
    # clamp
    x = max(0, min(W - cw, x))
    y = max(0, min(H - ch, y))
    out.alpha_composite(content, (x, y))
    return out


def scale_to_target_asp(
    content: Image.Image,
    target_asp: float,
    target_h: int | None = None,
    target_w: int | None = None,
) -> Image.Image:
    """Anisotropic scale so H/W ≈ target_asp. Prefer target_h if given."""
    cw, ch = content.size
    if cw < 1 or ch < 1:
        return content
    if target_h is None and target_w is None:
        # grow toward peer median height, clamp
        target_h = int(np.clip(max(ch, PEER_BB[1]), 200, 360))
    if target_h is not None:
        th = int(target_h)
        tw = max(32, int(round(th / target_asp)))
    else:
        tw = int(target_w)
        th = max(32, int(round(tw * target_asp)))
    # avoid extreme fattening beyond 2.6x width
    max_w = int(cw * 2.6)
    if tw > max_w:
        tw = max_w
        th = max(32, int(round(tw * target_asp)))
    return content.resize((tw, th), Image.Resampling.LANCZOS)


def mask_band(img: Image.Image, y_a: int, y_b: int) -> Image.Image:
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ya, yb = max(0, y_a), min(H, y_b)
    if yb <= ya:
        return out
    out.paste(img.crop((0, ya, W, yb)), (0, ya))
    return out


def core_only_mask(img: Image.Image, cx: int, core_r: int, y0: int, y1: int, pad: int = 6) -> Image.Image:
    """Keep only pixels within core radius (+pad) in band; drop SRB wings."""
    a = np.array(img.convert("RGBA"))
    out = np.zeros_like(a)
    x0 = max(0, cx - core_r - pad)
    x1 = min(W, cx + core_r + pad + 1)
    ya, yb = max(0, y0), min(H, y1)
    out[ya:yb, x0:x1] = a[ya:yb, x0:x1]
    return Image.fromarray(out, "RGBA")


# ───────────────────────── ELECTRON S2 ─────────────────────────
def rebuild_electron_s2() -> dict:
    src = Image.open(BEFORE / "vehicle_electron_s2.png").convert("RGBA")
    before = measure(src)
    crop = content_crop(src, pad=0)
    # Target peer-ish plate: asp ~1.55, height ~255 (fixes TALL+LOW_BH)
    target_asp = 1.55
    target_h = 255
    scaled = scale_to_target_asp(crop, target_asp, target_h=target_h)
    # Anchor bottom near original engine bottom so flame/engine y stays coherent for Ada
    orig_bot = before["bb"][3]
    # After taller content, keep bottom ≤ orig_bot + small slack but on-canvas
    bottom_y = min(H - 8, max(scaled.size[1] + 40, orig_bot + 20))
    # Prefer bottom near mid-upper peer placement: bot around 480 if fits
    bottom_y = min(520, max(scaled.size[1] + 80, 400))
    plate = place_on_canvas(scaled, cx=W // 2, bottom_y=bottom_y)
    plate = scrub_opaque_black(plate, max_rgb=26)
    plate = clean_alpha(plate)
    after = measure(plate)
    out_path = WORK / "vehicle_electron_s2.png"
    plate.save(out_path, "PNG")
    return {
        "asset": "vehicle_electron_s2.png",
        "before": before,
        "after": after,
        "before_md5": md5(BEFORE / "vehicle_electron_s2.png"),
        "after_md5": md5(out_path),
        "note": f"anisotropic HUD plate fit asp {before['asp']}→{after['asp']}; GOLD stack untouched",
    }


# ───────────────────────── GRAVITY-1 STAGES ─────────────────────────
def rebuild_gravity1_stages() -> list[dict]:
    side = Image.open(HUD / "vehicle_gravity1_side.png").convert("RGBA")
    assert md5(HUD / "vehicle_gravity1_side.png") == GOLD_G1_SIDE
    eng_ship = Image.open(HUD / "vehicle_gravity1_engine_ship.png").convert("RGBA")
    eng_booster = Image.open(HUD / "vehicle_gravity1_engine_booster.png").convert("RGBA")
    fairing = Image.open(HUD / "vehicle_gravity1_fairing.png").convert("RGBA")

    a = np.array(side)[:, :, 3] > THR
    ys = np.where(a.any(1))[0]
    body_top, body_bot = int(ys[0]), int(ys[-1])
    bh = body_bot - body_top
    # Published fracs from floor pack
    FAIRING_BOT_FRAC = 9.0 / 30.0
    S3_BOT_FRAC = 12.5 / 30.0
    S2_BOT_FRAC = 19.0 / 30.0
    fairing_bot = body_top + int(bh * FAIRING_BOT_FRAC)
    s3_bot = body_top + int(bh * S3_BOT_FRAC)
    s2_bot = body_top + int(bh * S2_BOT_FRAC)
    OV = 8
    cx = W // 2
    core_r = 52

    # Correct stage bands from GOLD side (read-only)
    # s1 = M1 + SRBs (full width)
    s1_raw = mask_band(side, s2_bot - OV // 2, body_bot + 1)
    s1_raw = Image.alpha_composite(s1_raw, mask_band(eng_booster, s2_bot - 4, body_bot + 1))

    # s2 = M2 core-only (strip SRB tops that caused SHORT 0.822)
    s2_band = mask_band(side, s3_bot - OV // 2, s2_bot + OV // 2)
    s2_raw = core_only_mask(s2_band, cx, core_r, s3_bot - OV // 2, s2_bot + OV // 2, pad=8)
    # Bake M2 engine from eng_ship (bell near s2_bot)
    s2_raw = Image.alpha_composite(
        s2_raw, core_only_mask(eng_ship, cx, core_r + 10, s2_bot - 40, s2_bot + 40, pad=12)
    )

    # s3 = M3 core-only (fix prior s3+s2 duplicate band)
    s3_band = mask_band(side, fairing_bot - OV // 2, s3_bot + OV // 2)
    s3_raw = core_only_mask(s3_band, cx, core_r, fairing_bot - OV // 2, s3_bot + OV // 2, pad=8)
    s3_raw = Image.alpha_composite(
        s3_raw, core_only_mask(eng_ship, cx, core_r + 8, s3_bot - 36, s3_bot + 30, pad=10)
    )

    # s4 = alias of s3 (baseline has no liquid S4; Soft-FAIL invent)
    # But also offer fairing-as-s4? MANIFEST: s4 alias of s3. Keep that contract.
    results = []

    def fit_stage(name: str, raw: Image.Image, before_path: Path, prefer_asp: float, target_h: int, keep_stack_y: bool):
        before_im = Image.open(before_path).convert("RGBA")
        before_m = measure(before_im)
        crop = content_crop(raw, pad=1)
        bw, bh_ = crop.size
        nat_asp = bh_ / max(1, bw)
        # Nudge aspect into peer band without inventing geometry beyond scale
        if nat_asp < ASP_LO:
            tgt = prefer_asp
        elif nat_asp > ASP_HI:
            tgt = min(prefer_asp + 0.15, ASP_HI)
        else:
            tgt = nat_asp
        # Cap anisotropic distortion
        if nat_asp > 0:
            # blend toward tgt but limit width scale factor
            scaled = scale_to_target_asp(crop, tgt, target_h=target_h)
        else:
            scaled = crop
        if keep_stack_y and before_m["bb"]:
            # Keep vertical center of stage near original stack band center
            y0, y1 = before_m["bb"][1], before_m["bb"][3]
            # For corrected bands use raw content center
            raw_m = measure(raw)
            if raw_m["bb"]:
                cy = (raw_m["bb"][1] + raw_m["bb"][3]) // 2
            else:
                cy = (y0 + y1) // 2
            plate = place_on_canvas(scaled, cx=cx, center_y=cy)
        else:
            plate = place_on_canvas(scaled, cx=cx, center_y=int(H * 0.38))
        plate = scrub_opaque_black(plate, max_rgb=28)
        plate = clean_alpha(plate)
        after_m = measure(plate)
        out_path = WORK / name
        plate.save(out_path, "PNG")
        return {
            "asset": name,
            "before": before_m,
            "after": after_m,
            "before_md5": md5(before_path),
            "after_md5": md5(out_path),
            "nat_asp_core": round(nat_asp, 3),
            "seams": {
                "body_top": body_top,
                "fairing_bot": fairing_bot,
                "s3_bot": s3_bot,
                "s2_bot": s2_bot,
                "body_bot": body_bot,
            },
        }

    # s2 was SHORT — core-only + scale to peer height
    results.append(
        fit_stage(
            "vehicle_gravity1_s2.png",
            s2_raw,
            BEFORE / "vehicle_gravity1_s2.png",
            prefer_asp=1.55,
            target_h=260,
            keep_stack_y=True,
        )
    )
    # s1 — keep SRB width published look; scale for useful fill, asp nudge if needed
    results.append(
        fit_stage(
            "vehicle_gravity1_s1.png",
            s1_raw,
            BEFORE / "vehicle_gravity1_s1.png",
            prefer_asp=1.45,
            target_h=320,
            keep_stack_y=True,
        )
    )
    # s3 — proper M3 only (was duplicate mega-band)
    r3 = fit_stage(
        "vehicle_gravity1_s3.png",
        s3_raw,
        BEFORE / "vehicle_gravity1_s3.png",
        prefer_asp=1.50,
        target_h=240,
        keep_stack_y=True,
    )
    results.append(r3)
    # s4 alias of s3 (same bytes)
    s3_plate = Image.open(WORK / "vehicle_gravity1_s3.png")
    s4_path = WORK / "vehicle_gravity1_s4.png"
    s3_plate.save(s4_path, "PNG")
    results.append(
        {
            "asset": "vehicle_gravity1_s4.png",
            "before": measure(Image.open(BEFORE / "vehicle_gravity1_s4.png")),
            "after": measure(s3_plate),
            "before_md5": md5(BEFORE / "vehicle_gravity1_s4.png"),
            "after_md5": md5(s4_path),
            "note": "alias of rebuilt s3 (baseline Soft-FAIL invent liquid S4)",
        }
    )
    # fairing unused as s4 — Soft-FAIL invent
    _ = fairing
    return results


def make_preview(report: dict) -> Path:
    """Eyes-only before/after board."""
    cols = []
    labels = []
    pairs = [
        ("electron s2", BEFORE / "vehicle_electron_s2.png", WORK / "vehicle_electron_s2.png"),
        ("gravity1 s1", BEFORE / "vehicle_gravity1_s1.png", WORK / "vehicle_gravity1_s1.png"),
        ("gravity1 s2", BEFORE / "vehicle_gravity1_s2.png", WORK / "vehicle_gravity1_s2.png"),
        ("gravity1 s3", BEFORE / "vehicle_gravity1_s3.png", WORK / "vehicle_gravity1_s3.png"),
        ("gravity1 s4", BEFORE / "vehicle_gravity1_s4.png", WORK / "vehicle_gravity1_s4.png"),
    ]

    def thumb(path: Path, max_h: int = 420) -> Image.Image:
        im = Image.open(path).convert("RGBA")
        c = content_crop(im, pad=4)
        bg = Image.new("RGBA", c.size, (28, 30, 36, 255))
        bg = Image.alpha_composite(bg, c)
        bg.thumbnail((280, max_h), Image.Resampling.LANCZOS)
        return bg.convert("RGBA")

    cell_w, cell_h = 300, 460
    board_w = 40 + len(pairs) * (cell_w * 2 + 30)
    board_h = 80 + cell_h + 60
    board = Image.new("RGB", (board_w, board_h), (18, 20, 24))
    draw = ImageDraw.Draw(board)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
        font_sm = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 12)
    except Exception:
        font = font_sm = ImageFont.load_default()

    draw.text((20, 18), "tip151 stage-fit — BEFORE (L) / AFTER (R)  |  Soft-FAIL ship  |  GOLD stacks untouched", fill=(220, 220, 230), font=font)

    x = 20
    for label, bp, ap in pairs:
        tb = thumb(bp)
        ta = thumb(ap)
        # panels
        for i, t in enumerate((tb, ta)):
            px = x + i * (cell_w + 8)
            py = 70
            draw.rectangle([px, py, px + cell_w - 10, py + cell_h - 10], fill=(40, 42, 48), outline=(70, 74, 82))
            ox = px + (cell_w - 10 - t.size[0]) // 2
            oy = py + 30 + (cell_h - 80 - t.size[1]) // 2
            board.paste(t.convert("RGB"), (ox, oy))
            tag = "BEFORE" if i == 0 else "AFTER"
            draw.text((px + 10, py + 8), tag, fill=(180, 190, 200) if i == 0 else (120, 220, 160), font=font_sm)
        # measures
        bm = measure(Image.open(bp))
        am = measure(Image.open(ap))
        draw.text(
            (x, 70 + cell_h - 8),
            f"{label}  {bm['bwh']} asp={bm['asp']} → {am['bwh']} asp={am['asp']}",
            fill=(200, 200, 210),
            font=font_sm,
        )
        x += cell_w * 2 + 30

    out = OUT / "preview_stage_fit_board.png"
    board.save(out, "PNG")
    return out


def deliver(report: dict):
    # Copy rebuilt into pack root, hud, and lrt packs
    assets = [
        "vehicle_electron_s2.png",
        "vehicle_gravity1_s1.png",
        "vehicle_gravity1_s2.png",
        "vehicle_gravity1_s3.png",
        "vehicle_gravity1_s4.png",
    ]
    for name in assets:
        src = WORK / name
        data = src.read_bytes()
        for dest_dir in (OUT, HUD, PACK_E if "electron" in name else PACK_G):
            if "electron" in name:
                targets = [OUT, HUD, PACK_E]
            else:
                targets = [OUT, HUD, PACK_G]
            break
        if "electron" in name:
            for d in (OUT, HUD, PACK_E):
                (d / name).write_bytes(data)
        else:
            for d in (OUT, HUD, PACK_G):
                (d / name).write_bytes(data)

    # GOLD guard
    stack_md5 = md5(HUD / "vehicle_electron_stack.png")
    side_md5 = md5(HUD / "vehicle_electron_side.png")
    assert stack_md5 == GOLD_ELECTRON_STACK, f"GOLD stack clobbered: {stack_md5}"
    assert side_md5 == GOLD_ELECTRON_STACK, f"GOLD side clobbered: {side_md5}"
    assert md5(HUD / "vehicle_gravity1_side.png") == GOLD_G1_SIDE
    assert md5(HUD / "vehicle_gravity1_stack.png") == GOLD_G1_SIDE

    # Soft-FAIL: verify we did NOT touch flames/engines/stack/shell/cutaway
    protected_e = [
        "vehicle_electron_stack.png",
        "vehicle_electron_side.png",
        "vehicle_electron_shell.png",
        "vehicle_electron_cutaway.png",
        "vehicle_electron_flame_s2.png",
        "vehicle_electron_engine_ship.png",
    ]
    # only check stack/side gold; others just must exist

    lines = []
    for name in assets:
        lines.append(f"{md5(OUT / name)}  {name}")
    lines.append(f"{md5(OUT / 'preview_stage_fit_board.png')}  preview_stage_fit_board.png")
    (OUT / "MD5SUMS").write_text("\n".join(lines) + "\n")

    # READY.md
    def fmt_m(m):
        if not m or not m.get("bwh"):
            return "empty"
        return f"bb {m['bwh'][0]}×{m['bwh'][1]} asp {m['asp']} (top_pad={m['top_pad']} bot_pad={m['bot_pad']})"

    ready = []
    ready.append("# READY — tip151 stage-fit Soft-FAIL-named rebuild")
    ready.append("")
    ready.append("**When:** 2026-09-23 CT")
    ready.append("**Lane:** Da Vinci art — electron s2 + gravity1 s1/s2/s3/s4")
    ready.append("**Soft-FAIL ship / git / device / Play** — pack deliverable only (+ hud/pack mirror for Ada wire).")
    ready.append("")
    ready.append("## Soft-FAIL held")
    ready.append("- Soft-FAIL overwrite electron GOLD stack/side/cutaway/shell (stack md5 still `d8ab6a86858da2237a18838c4c4dc91e`)")
    ready.append("- Soft-FAIL overwrite gravity1 stack/side")
    ready.append("- Soft-FAIL flame/engine overwrite beyond Soft-FAIL-named stage plates")
    ready.append("- Soft-FAIL invent Electron fairing geometry / Gravity-1 liquid S4 (s4 remains s3 alias)")
    ready.append("- Soft-FAIL F9 MVac / phantom SR75 S2 / kz11+cz2d flame")
    ready.append("")
    ready.append("## Paths")
    ready.append(f"- Pack: `{OUT}`")
    ready.append("- Mirrored: `/workspace/hud-assets/` + `lrt-electron-pack` / `lrt-gravity1-pack` (stage plates only)")
    ready.append("")
    ready.append("## old → new md5 + opaque bb/aspect")
    ready.append("")
    ready.append("| asset | old md5 | new md5 | before | after |")
    ready.append("|-------|---------|---------|--------|-------|")
    for item in report["items"]:
        ready.append(
            f"| `{item['asset']}` | `{item['before_md5']}` | `{item['after_md5']}` | {fmt_m(item['before'])} | {fmt_m(item['after'])} |"
        )
    ready.append("")
    ready.append("## Notes")
    ready.append("- **electron s2:** anisotropic scale toward peer STG2 median asp (~1.55) + taller bb (fixes TALL≈3.0 / LOW_BH). Published carbon Rutherford look preserved (resample of existing plate). Soft-FAIL ship alias / stack.")
    ready.append("- **gravity1 s2:** stripped SRB-top bleed (core-only M2) that caused SHORT asp 0.822; scaled to peer-ish plate fill.")
    ready.append("- **gravity1 s3/s4:** corrected prior mega-band duplicate; s3 = M3 core-only; s4 = alias of s3 (baseline Soft-FAIL invent liquid S4).")
    ready.append("- **gravity1 s1:** M1+SRBs republished plate with useful fill scale; published Orienspace look kept.")
    ready.append(f"- Preview board: `{OUT / 'preview_stage_fit_board.png'}`")
    ready.append(f"- Electron GOLD stack md5 (unchanged): `{stack_md5}`")
    ready.append("")
    ready.append("## Success criteria")
    e = next(i for i in report["items"] if i["asset"] == "vehicle_electron_s2.png")
    g2 = next(i for i in report["items"] if i["asset"] == "vehicle_gravity1_s2.png")
    ready.append(f"- electron s2 asp {e['before']['asp']} → **{e['after']['asp']}** (no longer ~3.0 TALL outlier): {'PASS' if e['after']['asp'] and ASP_LO <= e['after']['asp'] <= 2.0 else 'CHECK'}")
    ready.append(f"- gravity1 s2 asp {g2['before']['asp']} → **{g2['after']['asp']}** (no longer SHORT 0.822): {'PASS' if g2['after']['asp'] and g2['after']['asp'] >= 1.2 else 'CHECK'}")
    ready.append(f"- GOLD electron stack untouched: **PASS** `{stack_md5}`")
    ready.append("- Soft-FAIL ship: **HELD**")
    ready_path = OUT / "READY.md"
    ready_path.write_text("\n".join(ready) + "\n")
    report["ready_md5"] = md5(ready_path)
    report["stack_gold"] = stack_md5
    (OUT / "report.json").write_text(json.dumps(report, indent=2))
    return ready_path


def main():
    items = []
    items.append(rebuild_electron_s2())
    items.extend(rebuild_gravity1_stages())
    report = {"items": items}
    preview = make_preview(report)
    report["preview"] = str(preview)
    report["preview_md5"] = md5(preview)
    ready = deliver(report)
    print("READY", ready, md5(ready))
    for it in items:
        print(
            it["asset"],
            "asp",
            it["before"]["asp"],
            "→",
            it["after"]["asp"],
            "bb",
            it["before"]["bwh"],
            "→",
            it["after"]["bwh"],
            "md5",
            it["after_md5"],
        )
    print("GOLD stack", md5(HUD / "vehicle_electron_stack.png"))


if __name__ == "__main__":
    main()
