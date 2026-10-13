#!/usr/bin/env python3
"""Wave 1 GOLD nozzle + SRB burn-bar finisher (Da Vinci art executor).

A) CZ-6A  — Starship-bar nozzles + FG-112 burn @ 114.5 s NSF
B) H3-24  — Starship-bar nozzles + SRB-3 burn @ 110 s JAXA
C) Nuri / SR-75 — Starship-bar liquid/hybrid nozzles ONLY (Soft-FAIL solid bar)

Soft-FAIL: commit/publish/thread/catalog/device/Studio/Play
Soft-FAIL: invent geometry or burn times
Soft-FAIL: solid burn on CZ-8A / Nuri / SR-75
Soft-FAIL: CZ-8A this wave; Soft-FAIL RetroCommandCenter git
"""
from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path("/workspace/agent-backups/Da-Vinci/nozzle-wave1-20260923")
sys.path.insert(0, str(ROOT))
from lib_side_bell import paint_bells_on_layer  # noqa: E402

W, H = 424, 1200
HUD = Path("/workspace/hud-assets")


def md5(path: Path) -> str:
    h = hashlib.md5()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_font(size: int):
    for p in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf",
    ):
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()


def dilate_alpha(mask: Image.Image, radius: int = 2) -> Image.Image:
    """Dilate an L mask so old-bell fringe is fully cleared."""
    if radius <= 0:
        return mask
    # MaxFilter needs odd size
    k = radius * 2 + 1
    return mask.filter(ImageFilter.MaxFilter(k))


def replace_engine_layer(base: Image.Image, old_eng: Image.Image, new_eng: Image.Image) -> Image.Image:
    """Clear old nozzle footprint (dilated) then composite Starship-bar bells."""
    out = base.convert("RGBA").copy()
    old = old_eng.convert("RGBA")
    # Build clear mask from old eng alpha
    alpha = old.split()[-1]
    clear = dilate_alpha(alpha.point(lambda a: 255 if a > 28 else 0), radius=2)
    # Zero alpha where clear
    arr = np.array(out)
    m = np.array(clear) > 0
    arr[m, 3] = 0
    out = Image.fromarray(arr, "RGBA")
    return Image.alpha_composite(out, new_eng.convert("RGBA"))


def draw_srb_burn_bar(
    img: Image.Image,
    srb: dict,
    *,
    t_s: float,
    burn_s: float,
    bar_w: int = 10,
) -> Image.Image:
    """Simple vertical burn tell along SRB length.

    Soft-FAIL invent propellant color science — dim expended / warm burning only.
    Fraction expended = clamp(t/burn, 0..1). Progress top→down (UI strip).
    """
    out = img.copy()
    d = ImageDraw.Draw(out)
    frac = max(0.0, min(1.0, t_s / burn_s))
    y0, y1 = srb["y0"] + 8, srb["y1"] - 6
    h = max(1, y1 - y0)
    split = y0 + int(round(h * frac))
    cx = srb["cx"]
    if cx < W // 2:
        bx = cx + 18
    else:
        bx = cx - 18
    x0, x1 = bx - bar_w // 2, bx + bar_w // 2
    d.rectangle([x0 - 1, y0 - 1, x1 + 1, y1 + 1], outline=(40, 42, 48, 220), width=1)
    if split > y0:
        d.rectangle([x0, y0, x1, split], fill=(70, 74, 82, 230))
    if split < y1:
        d.rectangle([x0, split, x1, y1], fill=(230, 170, 70, 235))
    d.rectangle([x0 - 2, split - 1, x1 + 2, split + 1], fill=(255, 220, 140, 250))
    return out


def make_burn_layer(srbs: list[dict], t_s: float, burn_s: float) -> Image.Image:
    burn = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for srb in srbs:
        burn = draw_srb_burn_bar(burn, srb, t_s=t_s, burn_s=burn_s)
    return burn


def make_proof(
    stack: Image.Image,
    *,
    title: str,
    foot: str,
    crop_y0: int,
    crop_y1: int,
    out_path: Path,
) -> None:
    crop = stack.crop((0, crop_y0, W, crop_y1))
    panel = Image.new("RGBA", (W, crop_y1 - crop_y0), (0, 0, 0, 255))
    panel.alpha_composite(crop, (0, 0))
    panel = panel.convert("RGB")
    label_h = 34
    foot_h = 44
    board = Image.new("RGB", (W + 16, label_h + panel.size[1] + foot_h + 12), (18, 18, 22))
    d = ImageDraw.Draw(board)
    font = load_font(16)
    small = load_font(12)
    d.rectangle([8, 6, 8 + W, 6 + label_h - 4], fill=(0, 0, 0))
    bb = d.textbbox((0, 0), title, font=font)
    d.text((8 + (W - (bb[2] - bb[0])) // 2, 10), title, fill=(255, 255, 255), font=font)
    board.paste(panel, (8, 6 + label_h))
    bb = d.textbbox((0, 0), foot, font=small)
    d.text(
        (8 + (W - (bb[2] - bb[0])) // 2, board.size[1] - foot_h + 10),
        foot,
        fill=(190, 195, 200),
        font=small,
    )
    board.save(out_path, optimize=True)


def write_md5sums(folder: Path) -> list[tuple[Path, str]]:
    rows = []
    for p in sorted(folder.iterdir()):
        if p.is_file() and p.name != "MD5SUMS.txt":
            rows.append((p, md5(p)))
    (folder / "MD5SUMS.txt").write_text(
        "\n".join(f"{digest}  {p.name}" for p, digest in rows) + "\n",
        encoding="utf-8",
    )
    return rows


def copy_to(dests: list[Path], src: Path, name: str | None = None) -> None:
    name = name or src.name
    for d in dests:
        d.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, d / name)


# ---------------------------------------------------------------------------
# A) CZ-6A
# ---------------------------------------------------------------------------
def build_cz6a() -> dict:
    pack = Path("/workspace/lrt-cz6a-pack-20260923")
    pack_alias = Path("/workspace/lrt-cz6a-pack")
    out = ROOT / "cz6a"
    out.mkdir(parents=True, exist_ok=True)

    # Measured / GOLD placement (Soft-FAIL invent)
    ENGINE_CY = 973
    OFFSETS = [-137, -28, 28, 137]  # L SRB, L core, R core, R SRB
    BELL_W, BELL_H = 13, 44
    SHIP_CY = 432
    SHIP_W, SHIP_H = 11, 36
    FG112_BURN_S = 114.5  # NSF maiden — Soft-FAIL 174 s liquid K2
    SRB_L = dict(x0=24, x1=121, y0=727, y1=974, cx=72)
    SRB_R = dict(x0=303, x1=399, y0=727, y1=974, cx=351)
    DEMO_T = 57.0  # mid-burn demo ~50%

    old_eng_b = Image.open(pack / "vehicle_cz6a_engine_booster.png").convert("RGBA")
    old_eng_s = Image.open(pack / "vehicle_cz6a_engine_ship.png").convert("RGBA")

    new_eng_b = paint_bells_on_layer(
        (W, H), ENGINE_CY, OFFSETS, BELL_W, BELL_H,
        color_outer=(122, 128, 136, 250), color_inner=(28, 30, 34, 230),
    )
    new_eng_s = paint_bells_on_layer(
        (W, H), SHIP_CY, [0], SHIP_W, SHIP_H,
        color_outer=(125, 130, 140, 248), color_inner=(28, 30, 34, 230),
    )

    # Burn layers: Ada mid + empty + full demo frames
    burn_mid = make_burn_layer([SRB_L, SRB_R], DEMO_T, FG112_BURN_S)
    burn_empty = make_burn_layer([SRB_L, SRB_R], 0.0, FG112_BURN_S)
    burn_full = make_burn_layer([SRB_L, SRB_R], FG112_BURN_S, FG112_BURN_S)
    # Primary Ada-wireable name
    burn_primary = burn_mid

    composites = {
        "stack": "vehicle_cz6a_stack.png",
        "side": "vehicle_cz6a_side.png",
        "cutaway": "vehicle_cz6a_cutaway.png",
        "full": "vehicle_cz6a_full.png",
        "booster": "vehicle_cz6a_booster.png",
        "s1": "vehicle_cz6a_s1.png",
        "ship": "vehicle_cz6a_ship.png",
        "s2": "vehicle_cz6a_s2.png",
        "main": "vehicle_cz6a.png",
    }
    updated = {}
    for key, fname in composites.items():
        src = Image.open(pack / fname).convert("RGBA")
        if key in ("ship", "s2"):
            upd = replace_engine_layer(src, old_eng_s, new_eng_s)
        else:
            upd = replace_engine_layer(src, old_eng_b, new_eng_b)
            if key in ("stack", "side", "cutaway", "full", "main"):
                # also swap ship bell on whole-vehicle comps
                upd = replace_engine_layer(upd, old_eng_s, new_eng_s)
        updated[key] = upd

    # Stack with mid-burn for proof / Helios eyes (burn is separate Ada layer;
    # also provide stack_with_burn demo)
    stack_with_burn = Image.alpha_composite(updated["stack"], burn_primary)

    # shell / srb body unchanged (nozzles live on engine layers)
    shell = Image.open(pack / "vehicle_cz6a_shell.png").convert("RGBA")
    srb = Image.open(pack / "vehicle_cz6a_srb.png").convert("RGBA")

    # Write Da Vinci out
    files = {
        "vehicle_cz6a_stack.png": updated["stack"],
        "vehicle_cz6a_side.png": updated["side"],
        "vehicle_cz6a_cutaway.png": updated["cutaway"],
        "vehicle_cz6a_shell.png": shell,
        "vehicle_cz6a_full.png": updated["full"],
        "vehicle_cz6a.png": updated["main"],
        "vehicle_cz6a_booster.png": updated["booster"],
        "vehicle_cz6a_s1.png": updated["s1"],
        "vehicle_cz6a_ship.png": updated["ship"],
        "vehicle_cz6a_s2.png": updated["s2"],
        "vehicle_cz6a_engine_booster.png": new_eng_b,
        "vehicle_cz6a_engine_ship.png": new_eng_s,
        "vehicle_cz6a_srb.png": srb,
        "vehicle_cz6a_srb_burn.png": burn_primary,
        "vehicle_cz6a_srb_burn_mid.png": burn_mid,
        "vehicle_cz6a_srb_burn_empty.png": burn_empty,
        "vehicle_cz6a_srb_burn_full.png": burn_full,
        "vehicle_cz6a_stack_with_burn.png": stack_with_burn,
        "cz6a_engine_and_srb_burn.png": Image.alpha_composite(new_eng_b, burn_primary),
    }
    for name, im in files.items():
        im.save(out / name)

    make_proof(
        stack_with_burn,
        title="CZ-6A WAVE1 — nozzles + FG-112 burn",
        foot=f"Starship-bar de Laval · FG-112 burn {FG112_BURN_S:g}s NSF · demo t≈{DEMO_T:g}s",
        crop_y0=700,
        crop_y1=1040,
        out_path=out / "_proof_cz6a_wave1.png",
    )

    # Update pack (GOLD art only) + alias pack + hud-assets
    pack_names = [
        "vehicle_cz6a.png",
        "vehicle_cz6a_stack.png",
        "vehicle_cz6a_side.png",
        "vehicle_cz6a_cutaway.png",
        "vehicle_cz6a_full.png",
        "vehicle_cz6a_booster.png",
        "vehicle_cz6a_s1.png",
        "vehicle_cz6a_ship.png",
        "vehicle_cz6a_s2.png",
        "vehicle_cz6a_engine_booster.png",
        "vehicle_cz6a_engine_ship.png",
        "vehicle_cz6a_srb_burn.png",
        "vehicle_cz6a_srb_burn_mid.png",
        "vehicle_cz6a_srb_burn_empty.png",
        "vehicle_cz6a_srb_burn_full.png",
    ]
    dests = [pack, pack_alias, HUD]
    for name in pack_names:
        copy_to(dests, out / name)
    # proof into pack + Da Vinci
    shutil.copy2(out / "_proof_cz6a_wave1.png", pack / "_proof_cz6a.png")
    shutil.copy2(out / "_proof_cz6a_wave1.png", pack_alias / "_proof_cz6a.png")
    shutil.copy2(out / "_proof_cz6a_wave1.png", HUD / "_proof_cz6a.png")

    ready_lines = [
        "# CZ-6A WAVE 1 — nozzle + SRB burn Soft-PASS READY",
        "",
        "**Vehicle:** Long March 6A — core 2×YF-100 + 4×FG-112 solid strap-ons",
        "**Bar:** Starship side-profile de Laval (`lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46`)",
        f"**Burn:** FG-112 = **{FG112_BURN_S:g} s** (NSF maiden). Soft-FAIL 174 s liquid K2.",
        "**Scope:** Soft-FAIL commit / publish / device / Studio / Play / RetroCommandCenter git",
        "",
        "## Key deliverables (paths + md5)",
        "",
    ]
    key_names = [
        "vehicle_cz6a_stack.png",
        "vehicle_cz6a_cutaway.png",
        "vehicle_cz6a_shell.png",
        "vehicle_cz6a_engine_booster.png",
        "vehicle_cz6a_engine_ship.png",
        "vehicle_cz6a_srb.png",
        "vehicle_cz6a_srb_burn.png",
        "_proof_cz6a_wave1.png",
    ]
    # write READY first draft then refresh md5 after MD5SUMS
    (out / "READY.md").write_text("\n".join(ready_lines) + "\n", encoding="utf-8")
    rows = write_md5sums(out)
    dig = {p.name: d for p, d in rows}
    ready_lines = [
        "# CZ-6A WAVE 1 — nozzle + SRB burn Soft-PASS READY",
        "",
        "**Vehicle:** Long March 6A — core 2×YF-100 + 4×FG-112 solid strap-ons",
        "**Bar:** Starship side-profile de Laval (`lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46`)",
        f"**Burn:** FG-112 = **{FG112_BURN_S:g} s** (NSF maiden). Soft-FAIL 174 s liquid K2.",
        "**Demo burn frame:** t≈57 s (~50% expended). Also empty/full Ada masks.",
        "**Scope:** Soft-FAIL commit / publish / device / Studio / Play / RetroCommandCenter git",
        "",
        "## Key deliverables (absolute paths + md5)",
        "",
    ]
    for name in key_names:
        p = out / name
        ready_lines.append(f"- `{p}`  md5 `{dig[name]}`")
    ready_lines += [
        "",
        "## Also updated",
        f"- Pack: `{pack}/` (GOLD art PNGs + burn layers + proof)",
        f"- Pack alias: `{pack_alias}/`",
        f"- HUD: `{HUD}/vehicle_cz6a_*` + burn + proof",
        "",
        "## Soft-FAIL this vehicle",
        "- Soft-FAIL invent geometry; placement reused from GOLD/measured",
        "- Soft-FAIL solid burn color science beyond dim/warm tell",
        "",
    ]
    (out / "READY.md").write_text("\n".join(ready_lines) + "\n", encoding="utf-8")
    write_md5sums(out)

    # Refresh pack MD5SUMS for updated art files (append/replace keys)
    pack_md5_path = pack / "MD5SUMS.txt"
    existing = {}
    if pack_md5_path.exists():
        for line in pack_md5_path.read_text().splitlines():
            parts = line.split()
            if len(parts) >= 2:
                existing[parts[-1]] = parts[0]
    for name in pack_names + ["_proof_cz6a.png"]:
        p = pack / name
        if p.exists():
            existing[name] = md5(p)
    pack_md5_path.write_text(
        "\n".join(f"{existing[k]}  {k}" for k in sorted(existing)) + "\n",
        encoding="utf-8",
    )

    print("CZ6A READY")
    for name in key_names:
        print(f"  {out / name}  {dig[name]}")
    return {n: dig[n] for n in key_names}


# ---------------------------------------------------------------------------
# B) H3
# ---------------------------------------------------------------------------
def build_h3() -> dict:
    pack = Path("/workspace/lrt-h3-pack-20260923")
    out = ROOT / "h3"
    out.mkdir(parents=True, exist_ok=True)

    ENGINE_CY = 979  # measured
    OFFSETS = [-143, -28, 28, 143]
    BELL_W, BELL_H = 13, 44
    SHIP_CY = 489
    SHIP_W, SHIP_H = 11, 36
    SRB3_BURN_S = 110.0  # JAXA table / ground-test class
    # measured SRB bodies
    SRB_L = dict(x0=23, x1=111, y0=793, y1=980, cx=67)
    SRB_R = dict(x0=313, x1=400, y0=793, y1=980, cx=356)
    DEMO_T = 55.0  # mid-burn

    old_eng_b = Image.open(pack / "vehicle_h3_engine_booster.png").convert("RGBA")
    old_eng_s = Image.open(pack / "vehicle_h3_engine_ship.png").convert("RGBA")

    new_eng_b = paint_bells_on_layer(
        (W, H), ENGINE_CY, OFFSETS, BELL_W, BELL_H,
        color_outer=(122, 128, 136, 250), color_inner=(28, 30, 34, 230),
    )
    new_eng_s = paint_bells_on_layer(
        (W, H), SHIP_CY, [0], SHIP_W, SHIP_H,
        color_outer=(125, 130, 140, 248), color_inner=(28, 30, 34, 230),
    )

    burn_mid = make_burn_layer([SRB_L, SRB_R], DEMO_T, SRB3_BURN_S)
    burn_empty = make_burn_layer([SRB_L, SRB_R], 0.0, SRB3_BURN_S)
    burn_full = make_burn_layer([SRB_L, SRB_R], SRB3_BURN_S, SRB3_BURN_S)
    burn_primary = burn_mid

    composites = {
        "stack": "vehicle_h3_stack.png",
        "side": "vehicle_h3_side.png",
        "cutaway": "vehicle_h3_cutaway.png",
        "full": "vehicle_h3_full.png",
        "booster": "vehicle_h3_booster.png",
        "s1": "vehicle_h3_s1.png",
        "ship": "vehicle_h3_ship.png",
        "s2": "vehicle_h3_s2.png",
        "main": "vehicle_h3.png",
    }
    updated = {}
    for key, fname in composites.items():
        src = Image.open(pack / fname).convert("RGBA")
        if key in ("ship", "s2"):
            upd = replace_engine_layer(src, old_eng_s, new_eng_s)
        else:
            upd = replace_engine_layer(src, old_eng_b, new_eng_b)
            if key in ("stack", "side", "cutaway", "full", "main"):
                upd = replace_engine_layer(upd, old_eng_s, new_eng_s)
        updated[key] = upd

    stack_with_burn = Image.alpha_composite(updated["stack"], burn_primary)
    shell = Image.open(pack / "vehicle_h3_shell.png").convert("RGBA")
    srb = Image.open(pack / "vehicle_h3_srb.png").convert("RGBA")

    files = {
        "vehicle_h3_stack.png": updated["stack"],
        "vehicle_h3_side.png": updated["side"],
        "vehicle_h3_cutaway.png": updated["cutaway"],
        "vehicle_h3_shell.png": shell,
        "vehicle_h3_full.png": updated["full"],
        "vehicle_h3.png": updated["main"],
        "vehicle_h3_booster.png": updated["booster"],
        "vehicle_h3_s1.png": updated["s1"],
        "vehicle_h3_ship.png": updated["ship"],
        "vehicle_h3_s2.png": updated["s2"],
        "vehicle_h3_engine_booster.png": new_eng_b,
        "vehicle_h3_engine_ship.png": new_eng_s,
        "vehicle_h3_srb.png": srb,
        "vehicle_h3_srb_burn.png": burn_primary,
        "vehicle_h3_srb_burn_mid.png": burn_mid,
        "vehicle_h3_srb_burn_empty.png": burn_empty,
        "vehicle_h3_srb_burn_full.png": burn_full,
        "vehicle_h3_stack_with_burn.png": stack_with_burn,
        "h3_engine_and_srb_burn.png": Image.alpha_composite(new_eng_b, burn_primary),
    }
    for name, im in files.items():
        im.save(out / name)

    make_proof(
        stack_with_burn,
        title="H3-24 WAVE1 — nozzles + SRB-3 burn",
        foot=f"Starship-bar de Laval · SRB-3 burn {SRB3_BURN_S:g}s JAXA · demo t≈{DEMO_T:g}s",
        crop_y0=760,
        crop_y1=1050,
        out_path=out / "_proof_h3_wave1.png",
    )

    pack_names = [
        "vehicle_h3.png",
        "vehicle_h3_stack.png",
        "vehicle_h3_side.png",
        "vehicle_h3_cutaway.png",
        "vehicle_h3_full.png",
        "vehicle_h3_booster.png",
        "vehicle_h3_s1.png",
        "vehicle_h3_ship.png",
        "vehicle_h3_s2.png",
        "vehicle_h3_engine_booster.png",
        "vehicle_h3_engine_ship.png",
        "vehicle_h3_srb_burn.png",
        "vehicle_h3_srb_burn_mid.png",
        "vehicle_h3_srb_burn_empty.png",
        "vehicle_h3_srb_burn_full.png",
    ]
    for name in pack_names:
        copy_to([pack, HUD], out / name)
    shutil.copy2(out / "_proof_h3_wave1.png", pack / "_proof_h3.png")
    shutil.copy2(out / "_proof_h3_wave1.png", HUD / "_proof_h3.png")

    key_names = [
        "vehicle_h3_stack.png",
        "vehicle_h3_cutaway.png",
        "vehicle_h3_shell.png",
        "vehicle_h3_engine_booster.png",
        "vehicle_h3_engine_ship.png",
        "vehicle_h3_srb.png",
        "vehicle_h3_srb_burn.png",
        "_proof_h3_wave1.png",
    ]
    (out / "READY.md").write_text("# H3 WAVE1 WIP\n", encoding="utf-8")
    rows = write_md5sums(out)
    dig = {p.name: d for p, d in rows}
    ready_lines = [
        "# H3-24 WAVE 1 — nozzle + SRB-3 burn Soft-PASS READY",
        "",
        "**Vehicle:** H3-24 MMX config — core LE-9 family + 4×SRB-3 (side shows L/R)",
        "**Bar:** Starship side-profile de Laval (`lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46`)",
        f"**Burn:** SRB-3 = **{SRB3_BURN_S:g} s** (JAXA table / ground-test). Soft-FAIL invent.",
        f"**Demo burn frame:** t≈{DEMO_T:g}s mid-burn. Also empty/full Ada masks.",
        "**Scope:** Soft-FAIL commit / publish / device / Studio / Play / RetroCommandCenter git",
        "",
        "## Key deliverables (absolute paths + md5)",
        "",
    ]
    for name in key_names:
        ready_lines.append(f"- `{out / name}`  md5 `{dig[name]}`")
    ready_lines += [
        "",
        f"## Also updated\n- Pack: `{pack}/`\n- HUD: `{HUD}/vehicle_h3_*` + burn + proof",
        "",
        "## Soft-FAIL this vehicle",
        "- Soft-FAIL invent geometry; placement reused from GOLD/measured",
        "",
    ]
    (out / "READY.md").write_text("\n".join(ready_lines) + "\n", encoding="utf-8")
    write_md5sums(out)

    pack_md5_path = pack / "MD5SUMS.txt"
    existing = {}
    if pack_md5_path.exists():
        for line in pack_md5_path.read_text().splitlines():
            parts = line.split()
            if len(parts) >= 2:
                existing[parts[-1]] = parts[0]
    for name in pack_names + ["_proof_h3.png"]:
        p = pack / name
        if p.exists():
            existing[name] = md5(p)
    pack_md5_path.write_text(
        "\n".join(f"{existing[k]}  {k}" for k in sorted(existing)) + "\n",
        encoding="utf-8",
    )

    print("H3 READY")
    for name in key_names:
        print(f"  {out / name}  {dig[name]}")
    return {n: dig[n] for n in key_names}


# ---------------------------------------------------------------------------
# C1) Nuri — liquid nozzles only
# ---------------------------------------------------------------------------
def build_nuri() -> dict:
    pack = Path("/workspace/lrt-nuri-pack-20260923")
    out = ROOT / "nuri"
    out.mkdir(parents=True, exist_ok=True)

    # Measured / GOLD
    ENGINE_CY = 1034
    OFFSETS = [-36, -12, 12, 36]
    BELL_W, BELL_H = 11, 28
    SHIP_CY = 553
    SHIP_W, SHIP_H = 10, 22

    old_eng_b = Image.open(pack / "vehicle_nuri_engine_booster.png").convert("RGBA")
    old_eng_s = Image.open(pack / "vehicle_nuri_engine_ship.png").convert("RGBA")

    new_eng_b = paint_bells_on_layer(
        (W, H), ENGINE_CY, OFFSETS, BELL_W, BELL_H,
        color_outer=(122, 128, 136, 250), color_inner=(28, 30, 34, 230),
    )
    new_eng_s = paint_bells_on_layer(
        (W, H), SHIP_CY, [0], SHIP_W, SHIP_H,
        color_outer=(125, 130, 140, 248), color_inner=(28, 30, 34, 230),
    )

    composites = {
        "stack": "vehicle_nuri_stack.png",
        "side": "vehicle_nuri_side.png",
        "cutaway": "vehicle_nuri_cutaway.png",
        "full": "vehicle_nuri_full.png",
        "booster": "vehicle_nuri_booster.png",
        "s1": "vehicle_nuri_s1.png",
        "ship": "vehicle_nuri_ship.png",
        "s2": "vehicle_nuri_s2.png",
        "main": "vehicle_nuri.png",
    }
    updated = {}
    for key, fname in composites.items():
        src = Image.open(pack / fname).convert("RGBA")
        if key in ("ship", "s2"):
            upd = replace_engine_layer(src, old_eng_s, new_eng_s)
        else:
            upd = replace_engine_layer(src, old_eng_b, new_eng_b)
            if key in ("stack", "side", "cutaway", "full", "main"):
                upd = replace_engine_layer(upd, old_eng_s, new_eng_s)
        updated[key] = upd

    shell = Image.open(pack / "vehicle_nuri_shell.png").convert("RGBA")
    srb = Image.open(pack / "vehicle_nuri_srb.png").convert("RGBA")  # empty — keep

    files = {
        "vehicle_nuri_stack.png": updated["stack"],
        "vehicle_nuri_side.png": updated["side"],
        "vehicle_nuri_cutaway.png": updated["cutaway"],
        "vehicle_nuri_shell.png": shell,
        "vehicle_nuri_full.png": updated["full"],
        "vehicle_nuri.png": updated["main"],
        "vehicle_nuri_booster.png": updated["booster"],
        "vehicle_nuri_s1.png": updated["s1"],
        "vehicle_nuri_ship.png": updated["ship"],
        "vehicle_nuri_s2.png": updated["s2"],
        "vehicle_nuri_engine_booster.png": new_eng_b,
        "vehicle_nuri_engine_ship.png": new_eng_s,
        "vehicle_nuri_srb.png": srb,
    }
    for name, im in files.items():
        im.save(out / name)

    make_proof(
        updated["stack"],
        title="NURI WAVE1 — liquid nozzles only",
        foot="Starship-bar de Laval · Soft-FAIL solid burn bar (all-liquid KSLV-2)",
        crop_y0=980,
        crop_y1=1100,
        out_path=out / "_proof_nuri_wave1.png",
    )

    pack_names = [
        "vehicle_nuri.png",
        "vehicle_nuri_stack.png",
        "vehicle_nuri_side.png",
        "vehicle_nuri_cutaway.png",
        "vehicle_nuri_full.png",
        "vehicle_nuri_booster.png",
        "vehicle_nuri_s1.png",
        "vehicle_nuri_ship.png",
        "vehicle_nuri_s2.png",
        "vehicle_nuri_engine_booster.png",
        "vehicle_nuri_engine_ship.png",
    ]
    for name in pack_names:
        copy_to([pack, HUD], out / name)
    shutil.copy2(out / "_proof_nuri_wave1.png", pack / "_proof_nuri.png")
    shutil.copy2(out / "_proof_nuri_wave1.png", HUD / "_proof_nuri.png")

    key_names = [
        "vehicle_nuri_stack.png",
        "vehicle_nuri_cutaway.png",
        "vehicle_nuri_shell.png",
        "vehicle_nuri_engine_booster.png",
        "vehicle_nuri_engine_ship.png",
        "_proof_nuri_wave1.png",
    ]
    (out / "READY.md").write_text("# Nuri WAVE1 WIP\n", encoding="utf-8")
    rows = write_md5sums(out)
    dig = {p.name: d for p, d in rows}
    ready_lines = [
        "# Nuri / KSLV-2 WAVE 1 — liquid nozzles Soft-PASS READY",
        "",
        "**Vehicle:** Nuri — all-liquid (4×KRE-075 S1 + upper). Soft-FAIL solid burn bar.",
        "**Bar:** Starship side-profile de Laval (`lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46`)",
        "**Scope:** Soft-FAIL commit / publish / device / Studio / Play / RetroCommandCenter git",
        "",
        "## Key deliverables (absolute paths + md5)",
        "",
    ]
    for name in key_names:
        ready_lines.append(f"- `{out / name}`  md5 `{dig[name]}`")
    ready_lines += [
        "",
        f"## Also updated\n- Pack: `{pack}/`\n- HUD: `{HUD}/vehicle_nuri_*`",
        "",
        "## Soft-FAIL",
        "- Soft-FAIL solid burn bar on Nuri",
        "",
    ]
    (out / "READY.md").write_text("\n".join(ready_lines) + "\n", encoding="utf-8")
    write_md5sums(out)

    pack_md5_path = pack / "MD5SUMS.txt"
    existing = {}
    if pack_md5_path.exists():
        for line in pack_md5_path.read_text().splitlines():
            parts = line.split()
            if len(parts) >= 2:
                existing[parts[-1]] = parts[0]
    for name in pack_names + ["_proof_nuri.png"]:
        p = pack / name
        if p.exists():
            existing[name] = md5(p)
    pack_md5_path.write_text(
        "\n".join(f"{existing[k]}  {k}" for k in sorted(existing)) + "\n",
        encoding="utf-8",
    )

    print("NURI READY")
    for name in key_names:
        print(f"  {out / name}  {dig[name]}")
    return {n: dig[n] for n in key_names}


# ---------------------------------------------------------------------------
# C2) SR-75 — hybrid nozzle only
# ---------------------------------------------------------------------------
def build_sr75() -> dict:
    pack = Path("/workspace/lrt-sr75-pack-20260923")
    out = ROOT / "sr75"
    out.mkdir(parents=True, exist_ok=True)

    ENGINE_CY = 928  # measured top ~928; builder engine_top+2=932 — use measured
    OFFSETS = [0]
    BELL_W, BELL_H = 16, 48

    old_eng = Image.open(pack / "vehicle_sr75_engine_booster.png").convert("RGBA")
    new_eng = paint_bells_on_layer(
        (W, H), ENGINE_CY, OFFSETS, BELL_W, BELL_H,
        color_outer=(118, 124, 132, 250), color_inner=(28, 30, 34, 230),
    )

    composites = {
        "stack": "vehicle_sr75_stack.png",
        "side": "vehicle_sr75_side.png",
        "cutaway": "vehicle_sr75_cutaway.png",
        "full": "vehicle_sr75_full.png",
        "booster": "vehicle_sr75_booster.png",
        "s1": "vehicle_sr75_s1.png",
        "main": "vehicle_sr75.png",
    }
    updated = {}
    for key, fname in composites.items():
        src = Image.open(pack / fname).convert("RGBA")
        updated[key] = replace_engine_layer(src, old_eng, new_eng)

    shell = Image.open(pack / "vehicle_sr75_shell.png").convert("RGBA")
    # ship eng is empty placeholder for single-stage — leave as pack has it
    eng_ship = Image.open(pack / "vehicle_sr75_engine_ship.png").convert("RGBA")

    files = {
        "vehicle_sr75_stack.png": updated["stack"],
        "vehicle_sr75_side.png": updated["side"],
        "vehicle_sr75_cutaway.png": updated["cutaway"],
        "vehicle_sr75_shell.png": shell,
        "vehicle_sr75_full.png": updated["full"],
        "vehicle_sr75.png": updated["main"],
        "vehicle_sr75_booster.png": updated["booster"],
        "vehicle_sr75_s1.png": updated["s1"],
        "vehicle_sr75_engine_booster.png": new_eng,
        "vehicle_sr75_engine_ship.png": eng_ship,
    }
    for name, im in files.items():
        im.save(out / name)

    make_proof(
        updated["stack"],
        title="SR-75 WAVE1 — hybrid nozzle only",
        foot="Starship-bar de Laval · Soft-FAIL solid burn bar (hybrid paraffin/LOX)",
        crop_y0=860,
        crop_y1=1020,
        out_path=out / "_proof_sr75_wave1.png",
    )

    pack_names = [
        "vehicle_sr75.png",
        "vehicle_sr75_stack.png",
        "vehicle_sr75_side.png",
        "vehicle_sr75_cutaway.png",
        "vehicle_sr75_full.png",
        "vehicle_sr75_booster.png",
        "vehicle_sr75_s1.png",
        "vehicle_sr75_engine_booster.png",
    ]
    for name in pack_names:
        copy_to([pack, HUD], out / name)
    shutil.copy2(out / "_proof_sr75_wave1.png", pack / "_proof_sr75.png")
    shutil.copy2(out / "_proof_sr75_wave1.png", HUD / "_proof_sr75.png")

    key_names = [
        "vehicle_sr75_stack.png",
        "vehicle_sr75_cutaway.png",
        "vehicle_sr75_shell.png",
        "vehicle_sr75_engine_booster.png",
        "_proof_sr75_wave1.png",
    ]
    (out / "READY.md").write_text("# SR75 WAVE1 WIP\n", encoding="utf-8")
    rows = write_md5sums(out)
    dig = {p.name: d for p, d in rows}
    ready_lines = [
        "# SR-75 WAVE 1 — hybrid nozzle Soft-PASS READY",
        "",
        "**Vehicle:** SR-75 Flight 2 — single HyPLOX-75 hybrid. Soft-FAIL solid burn bar.",
        "**Bar:** Starship side-profile de Laval (`lib_side_bell.py` md5 `0f5111a272ddb2f9876784100e86bd46`)",
        "**Scope:** Soft-FAIL commit / publish / device / Studio / Play / RetroCommandCenter git",
        "",
        "## Key deliverables (absolute paths + md5)",
        "",
    ]
    for name in key_names:
        ready_lines.append(f"- `{out / name}`  md5 `{dig[name]}`")
    ready_lines += [
        "",
        f"## Also updated\n- Pack: `{pack}/`\n- HUD: `{HUD}/vehicle_sr75_*`",
        "",
        "## Soft-FAIL",
        "- Soft-FAIL solid burn bar on SR-75 (hybrid, not SRB)",
        "",
    ]
    (out / "READY.md").write_text("\n".join(ready_lines) + "\n", encoding="utf-8")
    write_md5sums(out)

    pack_md5_path = pack / "MD5SUMS.txt"
    existing = {}
    if pack_md5_path.exists():
        for line in pack_md5_path.read_text().splitlines():
            parts = line.split()
            if len(parts) >= 2:
                existing[parts[-1]] = parts[0]
    for name in pack_names + ["_proof_sr75.png"]:
        p = pack / name
        if p.exists():
            existing[name] = md5(p)
    pack_md5_path.write_text(
        "\n".join(f"{existing[k]}  {k}" for k in sorted(existing)) + "\n",
        encoding="utf-8",
    )

    print("SR75 READY")
    for name in key_names:
        print(f"  {out / name}  {dig[name]}")
    return {n: dig[n] for n in key_names}


def main():
    only = sys.argv[1] if len(sys.argv) > 1 else "all"
    results = {}
    if only in ("all", "a", "cz6a"):
        results["cz6a"] = build_cz6a()
    if only in ("all", "b", "h3"):
        results["h3"] = build_h3()
    if only in ("all", "c", "nuri"):
        results["nuri"] = build_nuri()
    if only in ("all", "c", "sr75"):
        results["sr75"] = build_sr75()
    return results


if __name__ == "__main__":
    main()
