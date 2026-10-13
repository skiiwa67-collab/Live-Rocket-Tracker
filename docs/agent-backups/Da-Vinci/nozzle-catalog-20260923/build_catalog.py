#!/usr/bin/env python3
"""HARD full-catalog Starship-bar nozzle pass (Da Vinci art executor).

Soft-FAIL invent / below Starship bar / commit / publish / thread / catalog ship /
device / Studio / Play / RetroCommandCenter git.
Soft-FAIL solid burn bars except WAVE1 real solids already done.
Soft-PASS Starship as BAR reference (Soft-FAIL rebuild).
"""
from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path
from typing import Iterable

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path("/workspace/agent-backups/Da-Vinci/nozzle-catalog-20260923")
WAVE1 = Path("/workspace/agent-backups/Da-Vinci/nozzle-wave1-20260923")
HUD = Path("/workspace/hud-assets")
sys.path.insert(0, str(ROOT))
from lib_side_bell import paint_bells_on_layer  # noqa: E402

W, H = 424, 1200
LIB_MD5 = "0f5111a272ddb2f9876784100e86bd46"


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
    if radius <= 0:
        return mask
    return mask.filter(ImageFilter.MaxFilter(radius * 2 + 1))


def replace_engine_layer(base: Image.Image, old_eng: Image.Image, new_eng: Image.Image) -> Image.Image:
    out = base.convert("RGBA").copy()
    old = old_eng.convert("RGBA")
    alpha = old.split()[-1]
    clear = dilate_alpha(alpha.point(lambda a: 255 if a > 28 else 0), radius=2)
    arr = np.array(out)
    m = np.array(clear) > 0
    arr[m, 3] = 0
    out = Image.fromarray(arr, "RGBA")
    return Image.alpha_composite(out, new_eng.convert("RGBA"))


def paint_groups(size, groups, **kwargs):
    """Paint multiple (cy, offsets, w, h) groups onto one layer."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    for cy, offsets, bw, bh in groups:
        piece = paint_bells_on_layer(size, cy, offsets, bw, bh, **kwargs)
        layer = Image.alpha_composite(layer, piece)
    return layer


def stack_bottom_crop(stack: Image.Image, y0: int, y1: int | None = None) -> Image.Image:
    y1 = y1 or min(H, y0 + 220)
    return stack.crop((0, y0, W, y1))


def make_compare(old_crop: Image.Image, new_crop: Image.Image, title: str, out: Path) -> None:
    font = load_font(16)
    small = load_font(12)
    gap = 12
    label_h = 28
    foot_h = 36
    ow, oh = old_crop.size
    board = Image.new("RGB", (ow * 2 + gap + 24, label_h + oh + foot_h + 16), (18, 18, 22))
    d = ImageDraw.Draw(board)
    d.text((12, 6), title, fill=(240, 240, 245), font=font)
    for i, (lab, im) in enumerate((("OLD", old_crop), ("NEW", new_crop))):
        x = 12 + i * (ow + gap)
        panel = Image.new("RGBA", im.size, (0, 0, 0, 255))
        panel = Image.alpha_composite(panel, im.convert("RGBA"))
        board.paste(panel.convert("RGB"), (x, label_h + 4))
        d.rectangle([x, label_h + 4, x + 48, label_h + 22], fill=(0, 0, 0))
        d.text((x + 6, label_h + 6), lab, fill=(255, 220, 120) if lab == "OLD" else (120, 220, 160), font=small)
    d.text((12, board.size[1] - foot_h + 8), "Starship-bar de Laval · Soft-FAIL commit/device · awaiting Chris Soft-PASS full-set", fill=(170, 175, 180), font=small)
    board.save(out, optimize=True)


def make_proof(stack: Image.Image, title: str, foot: str, y0: int, out: Path, y1: int | None = None) -> None:
    crop = stack_bottom_crop(stack, y0, y1)
    panel = Image.new("RGBA", crop.size, (0, 0, 0, 255))
    panel = Image.alpha_composite(panel, crop.convert("RGBA")).convert("RGB")
    font = load_font(16)
    small = load_font(12)
    label_h, foot_h = 34, 44
    board = Image.new("RGB", (W + 16, label_h + panel.size[1] + foot_h + 12), (18, 18, 22))
    d = ImageDraw.Draw(board)
    d.rectangle([8, 6, 8 + W, 6 + label_h - 4], fill=(0, 0, 0))
    bb = d.textbbox((0, 0), title, font=font)
    d.text((8 + (W - (bb[2] - bb[0])) // 2, 10), title, fill=(255, 255, 255), font=font)
    board.paste(panel, (8, 6 + label_h))
    bb = d.textbbox((0, 0), foot, font=small)
    d.text((8 + (W - (bb[2] - bb[0])) // 2, board.size[1] - foot_h + 10), foot, fill=(190, 195, 200), font=small)
    board.save(out, optimize=True)


def write_md5sums(folder: Path) -> list[tuple[Path, str]]:
    rows = []
    for p in sorted(folder.iterdir()):
        if p.is_file() and p.name != "MD5SUMS.txt":
            rows.append((p, md5(p)))
    (folder / "MD5SUMS.txt").write_text(
        "\n".join(f"{digest}  {p.name}" for p, digest in rows) + "\n", encoding="utf-8"
    )
    return rows


def copy_to(dests: Iterable[Path], src: Path, name: str | None = None) -> None:
    name = name or src.name
    for d in dests:
        if d is None:
            continue
        d = Path(d)
        d.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, d / name)


def find_pack(vid: str) -> Path | None:
    for p in (
        Path(f"/workspace/lrt-{vid}-pack"),
        Path(f"/workspace/lrt-{vid}-pack-20260923"),
    ):
        if p.is_dir():
            return p
    return None


# Composite tags that typically carry booster and/or ship engines
BOOSTER_TAGS = ("stack", "side", "cutaway", "full", "booster", "s1", "root")
SHIP_TAGS = ("stack", "side", "cutaway", "full", "ship", "s2", "root")
# filename map for root
TAG_FILE = {
    "root": "",
    "stack": "_stack",
    "side": "_side",
    "cutaway": "_cutaway",
    "full": "_full",
    "booster": "_booster",
    "s1": "_s1",
    "ship": "_ship",
    "s2": "_s2",
    "s3": "_s3",
    "s4": "_s4",
    "shell": "_shell",
}


def vehicle_png(vid: str, tag: str, base: Path) -> Path | None:
    if tag == "root":
        p = base / f"vehicle_{vid}.png"
    else:
        p = base / f"vehicle_{vid}_{tag}.png"
    return p if p.exists() else None


def rebuild_vehicle(
    vid: str,
    *,
    booster_groups: list[tuple],
    ship_groups: list[tuple] | None,
    aliases: dict[str, str] | None = None,
    label: str,
    crop_y0: int,
    color_outer=(122, 128, 136, 250),
    color_inner=(28, 30, 34, 230),
    ship_outer=(125, 130, 140, 248),
    ship_inner=(28, 30, 34, 230),
) -> dict:
    out = ROOT / vid
    out.mkdir(parents=True, exist_ok=True)
    pack = find_pack(vid)
    src_base = pack if pack and (pack / f"vehicle_{vid}_engine_booster.png").exists() else HUD

    old_eng_b = Image.open(src_base / f"vehicle_{vid}_engine_booster.png").convert("RGBA")
    # Keep OLD copy for compare
    old_eng_b.save(out / f"vehicle_{vid}_engine_booster_OLD.png")

    new_eng_b = paint_groups(
        (W, H), booster_groups, color_outer=color_outer, color_inner=color_inner
    )
    new_eng_b.save(out / f"vehicle_{vid}_engine_booster.png")

    new_eng_s = None
    old_eng_s = None
    ship_path = src_base / f"vehicle_{vid}_engine_ship.png"
    if ship_groups and ship_path.exists():
        old_eng_s = Image.open(ship_path).convert("RGBA")
        old_eng_s.save(out / f"vehicle_{vid}_engine_ship_OLD.png")
        new_eng_s = paint_groups(
            (W, H), ship_groups, color_outer=ship_outer, color_inner=ship_inner
        )
        new_eng_s.save(out / f"vehicle_{vid}_engine_ship.png")

    # Prefer stack from pack else HUD
    stack_src = vehicle_png(vid, "stack", src_base) or vehicle_png(vid, "stack", HUD)
    side_src = vehicle_png(vid, "side", src_base) or vehicle_png(vid, "side", HUD)
    old_stack = Image.open(stack_src).convert("RGBA") if stack_src else None

    # Update composites that exist
    updated = {}
    tags_seen = set()
    for base in filter(None, [src_base, HUD]):
        for tag in ("stack", "side", "cutaway", "full", "booster", "s1", "ship", "s2", "s3", "s4", "root"):
            if tag in tags_seen:
                continue
            p = vehicle_png(vid, tag, base)
            if not p:
                continue
            tags_seen.add(tag)
            im = Image.open(p).convert("RGBA")
            # booster eng on booster-ish; ship eng on ship-ish; both on whole-vehicle
            whole = tag in ("stack", "side", "cutaway", "full", "root")
            if tag in ("booster", "s1") or whole:
                im = replace_engine_layer(im, old_eng_b, new_eng_b)
            if new_eng_s is not None and (tag in ("ship", "s2", "s3", "s4") or whole):
                im = replace_engine_layer(im, old_eng_s, new_eng_s)
            updated[tag] = im
            suffix = "" if tag == "root" else f"_{tag}"
            im.save(out / f"vehicle_{vid}{suffix}.png")

    # shell unchanged copy if present
    for base in filter(None, [src_base, HUD]):
        sh = vehicle_png(vid, "shell", base)
        if sh:
            shutil.copy2(sh, out / f"vehicle_{vid}_shell.png")
            break

    # aliases (cz12 yf100k/yf115)
    if aliases:
        for alias, src_key in aliases.items():
            src_im = new_eng_b if src_key == "booster" else new_eng_s
            if src_im is not None:
                src_im.save(out / f"vehicle_{vid}_{alias}.png")

    # Proof + compare
    if old_stack is not None and "stack" in updated:
        old_bottom = replace_engine_layer(old_stack.copy(), old_eng_b, old_eng_b)  # identity
        # true OLD bottom: clear new? use original stack
        old_crop = stack_bottom_crop(old_stack, crop_y0)
        new_crop = stack_bottom_crop(updated["stack"], crop_y0)
        old_crop.save(out / f"{vid}_stack_bottom_OLD.png")
        new_crop.save(out / f"{vid}_stack_bottom_NEW.png")
        make_compare(old_crop, new_crop, f"{label} nozzles OLD | NEW", out / f"compare_{vid}.png")
        make_proof(
            updated["stack"],
            title=f"{label} — Starship-bar nozzles",
            foot="lib_side_bell Soft-PASS bar · Soft-FAIL commit/device · Chris Soft-PASS pending",
            y0=crop_y0,
            out=out / f"_proof_{vid}.png",
        )

    # Mirror NEW engines + updated composites into HUD + pack (art only)
    dests = [HUD]
    if pack:
        dests.append(pack)
    mirror_names = [f"vehicle_{vid}_engine_booster.png"]
    if new_eng_s is not None:
        mirror_names.append(f"vehicle_{vid}_engine_ship.png")
    if aliases:
        for alias in aliases:
            mirror_names.append(f"vehicle_{vid}_{alias}.png")
    for tag in updated:
        suffix = "" if tag == "root" else f"_{tag}"
        mirror_names.append(f"vehicle_{vid}{suffix}.png")
    # also proof into dests
    for name in mirror_names:
        p = out / name
        if p.exists():
            copy_to(dests, p)
    if (out / f"_proof_{vid}.png").exists():
        copy_to(dests, out / f"_proof_{vid}.png")

    rows = write_md5sums(out)
    dig = {p.name: d for p, d in rows}

    ready = [
        f"# {label} — Starship-bar nozzle Soft-PASS READY (catalog)",
        "",
        f"**Bar:** Starship side-profile de Laval (`lib_side_bell.py` md5 `{LIB_MD5}`)",
        "**Scope:** Soft-FAIL commit / publish / thread / catalog ship / device / Studio / Play / RetroCommandCenter git",
        "**Solid burn:** Soft-FAIL invent (liquids / Soft-FAIL non-WAVE1 solids)",
        "",
        "## Key deliverables (absolute paths + md5)",
        "",
    ]
    for name in [
        f"vehicle_{vid}_engine_booster.png",
        f"vehicle_{vid}_engine_ship.png",
        f"vehicle_{vid}_stack.png",
        f"_proof_{vid}.png",
        f"compare_{vid}.png",
        f"{vid}_stack_bottom_NEW.png",
    ]:
        if name in dig:
            ready.append(f"- `{out / name}`  md5 `{dig[name]}`")
    ready += [
        "",
        "## Mirrored (art only, no git)",
        f"- HUD: `{HUD}/vehicle_{vid}_*`",
    ]
    if pack:
        ready.append(f"- Pack: `{pack}/`")
    ready += ["", f"## Geometry (measured Soft-FAIL invent)", f"- booster_groups: `{booster_groups}`"]
    if ship_groups:
        ready.append(f"- ship_groups: `{ship_groups}`")
    ready.append("")
    (out / "READY.md").write_text("\n".join(ready) + "\n", encoding="utf-8")
    write_md5sums(out)
    dig = {p.name: md5(p) for p in out.iterdir() if p.is_file() and p.name != "MD5SUMS.txt"}
    print(f"READY {vid}")
    for k in ("engine_booster", "engine_ship", "stack", "proof"):
        for name, digest in dig.items():
            if k in name and "OLD" not in name:
                print(f"  {name}  {digest}")
    return dig


def copy_wave1(vid: str) -> None:
    src = WAVE1 / vid
    dst = ROOT / vid
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst)
    # ensure READY notes catalog skip
    note = dst / "WAVE1_SKIP.txt"
    note.write_text(
        "Soft-PASS already WAVE1 — catalog Soft-PASS skip rebuild (bar OK).\n"
        f"Source: {src}\n",
        encoding="utf-8",
    )
    write_md5sums(dst)
    print(f"WAVE1 copy {vid}")


def main():
    assert md5(ROOT / "lib_side_bell.py") == LIB_MD5, "lib md5 Soft-FAIL bar mismatch"

    # ---- WAVE1 Soft-PASS skip (copy into catalog for board) ----
    for vid in ("cz6a", "h3", "nuri", "sr75"):
        if (WAVE1 / vid).is_dir():
            copy_wave1(vid)

    # ---- Soft-FAIL rebuilds ----
    # Batch 1
    rebuild_vehicle(
        "cz8a",
        label="CZ-8A",
        crop_y0=940,
        booster_groups=[(973, [-142, -28, 28, 142], 14, 48)],
        ship_groups=[(509, [-20, 20], 11, 36)],
    )
    rebuild_vehicle(
        "cz12",
        label="CZ-12",
        crop_y0=980,
        booster_groups=[(1014, [-51, -17, 17, 51], 15, 42)],
        ship_groups=[(426, [-43, 42], 16, 40)],
        aliases={"engine_yf100k": "booster", "engine_yf115": "ship"},
    )
    rebuild_vehicle(
        "cz2d",
        label="CZ-2D",
        crop_y0=970,
        booster_groups=[(1002, [-34, -11, 11, 34], 13, 34)],
        ship_groups=[
            (346, [0], 15, 36),
            (350, [-22, 22], 6, 16),
        ],
    )
    rebuild_vehicle(
        "electron",
        label="Electron",
        crop_y0=950,
        booster_groups=[(980, [-23, -11, 0, 11, 23], 9, 36)],
        ship_groups=[(373, [0], 11, 48)],
    )
    rebuild_vehicle(
        "fh",
        label="Falcon Heavy",
        crop_y0=950,
        booster_groups=[(977, [-33, -16, 0, 16, 33], 10, 38)],
        ship_groups=[(422, [0], 16, 58)],
    )
    rebuild_vehicle(
        "lm5",
        label="Long March 5",
        crop_y0=940,
        booster_groups=[
            (971, [-24, 24], 15, 52),
            (975, [-144, -124, 124, 144], 11, 42),
        ],
        ship_groups=[(455, [-20, 20], 11, 34)],
    )

    # Batch 2
    rebuild_vehicle(
        "gravity1",
        label="Gravity-1",
        crop_y0=940,
        booster_groups=[
            (971, [0], 22, 48),
            (975, [-96, 96], 18, 42),
        ],
        ship_groups=[
            (446, [0], 16, 30),
            (663, [0], 12, 22),
        ],
    )
    rebuild_vehicle(
        "pallas1",
        label="Pallas-1",
        crop_y0=960,
        booster_groups=[(985, [-39, -19, 0, 19, 39], 11, 40)],
        ship_groups=[(451, [0], 14, 52)],
    )
    rebuild_vehicle(
        "zq3",
        label="ZQ-3",
        crop_y0=940,
        booster_groups=[(967, [-46, -23, 0, 23, 46], 12, 46)],
        ship_groups=[(432, [0], 16, 42)],
    )
    rebuild_vehicle(
        "kz11",
        label="KZ-11",
        crop_y0=970,
        booster_groups=[(1000, [0], 36, 52)],
        ship_groups=[(261, [0], 16, 24)],
    )
    rebuild_vehicle(
        "kinetica1",
        label="Kinetica-1",
        crop_y0=970,
        booster_groups=[(996, [0], 40, 58)],
        ship_groups=[
            (306, [0], 18, 26),
            (436, [0], 22, 32),
            (586, [0], 26, 38),
        ],
    )

    print("CATALOG BUILD DONE")


if __name__ == "__main__":
    main()
