#!/usr/bin/env python3
"""tip154 — rebuild tank fill masks to ~80–88% of cutaway core window width.
HARD FAIL: do not touch stack/side/cutaway/shell/engines/flames/hud/Ada.
"""
from __future__ import annotations
import json, hashlib, shutil
from pathlib import Path
import numpy as np
from PIL import Image
from scipy import ndimage

DRAW = Path('/workspace/Live-Rocket-Tracker/app/src/main/res/drawable')
OUT = Path('/workspace/agent-backups/Da-Vinci/tip154-tank-fit-20260923')
FILLS_OUT = OUT / 'fills'
BEFORE = OUT / '_before'
QA = OUT / '_qa'
TARGET = 0.84
TARGET_LO, TARGET_HI = 0.80, 0.88
TINY = 0.70
OVER = 0.92

NEXT30 = ['gravity1', 'cz8a', 'cz6a', 'electron', 'f9', 'sr75', 'nuri', 'h3', 'cz2d', 'kz11']
CATALOG = ['cz12', 'fh', 'kinetica1', 'lm5', 'pallas1', 'zq3']
PRIORITY = ['electron', 'gravity1', 'cz8a', 'cz2d'] + [
    v for v in NEXT30 + CATALOG if v not in ('electron', 'gravity1', 'cz8a', 'cz2d')
]


def md5(p: Path) -> str:
    h = hashlib.md5()
    with open(p, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def load(p: Path) -> np.ndarray:
    return np.array(Image.open(p).convert('RGBA'))


def save(arr: np.ndarray, p: Path) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(arr).save(p)


def contiguous_segs(row_a: np.ndarray):
    xs = np.where(row_a > 8)[0]
    if len(xs) == 0:
        return []
    out = []
    s = int(xs[0])
    for i in range(1, len(xs)):
        if xs[i] != xs[i - 1] + 1:
            out.append((s, int(xs[i - 1])))
            s = int(xs[i])
    out.append((s, int(xs[-1])))
    return out


def seg_containing(segs, cx):
    for a, b in segs:
        if a <= cx <= b:
            return a, b
    if not segs:
        return None
    return min(segs, key=lambda ab: min(abs(ab[0] - cx), abs(ab[1] - cx)))


def shell_gap(shell_a, y, cx):
    if shell_a is None:
        return None
    xs_op = np.where(shell_a[y] > 8)[0]
    if len(xs_op) == 0:
        return None
    left = xs_op[xs_op < cx]
    right = xs_op[xs_op > cx]
    if len(left) == 0 or len(right) == 0:
        return None
    x0 = int(left[-1] + 1)
    x1 = int(right[0] - 1)
    if x1 < x0:
        return None
    return x0, x1, x1 - x0 + 1


def reference_core_width(cut_a: np.ndarray, fill_a: np.ndarray) -> int:
    """Narrow-biased core: 25th percentile of single-segment cutaway widths on fill rows.
    Prefers upper-stage body diameter over booster-merged spans.
    """
    widths = []
    ys = np.where(fill_a.any(axis=1))[0]
    if len(ys) == 0:
        ys = np.arange(cut_a.shape[0] // 5, 4 * cut_a.shape[0] // 5, 8)
    # prefer upper half of fill span
    if len(ys):
        y_split = ys[len(ys) // 2]
        ys_pref = [y for y in ys[::2] if y <= y_split] or list(ys[::3])
    else:
        ys_pref = []
    for y in ys_pref:
        segs = contiguous_segs(cut_a[y])
        if len(segs) == 1:
            a, b = segs[0]
            widths.append(b - a + 1)
    if len(widths) < 5:
        for y in ys[::3]:
            segs = contiguous_segs(cut_a[y])
            if len(segs) == 1:
                widths.append(segs[0][1] - segs[0][0] + 1)
    if not widths:
        cx = cut_a.shape[1] // 2
        for y in range(0, cut_a.shape[0], 8):
            segs = contiguous_segs(cut_a[y])
            seg = seg_containing(segs, cx)
            if seg:
                widths.append(seg[1] - seg[0] + 1)
    if not widths:
        return 0
    # 25th percentile biases toward slender core (not booster-merged)
    return int(np.percentile(widths, 25))


def classify_islands(lab, n, img_cx):
    """Return dict i -> 'side'|'center' using x position clusters at similar y."""
    metas = {}
    for i in range(1, n + 1):
        ys, xs = np.where(lab == i)
        if len(ys) < 80:
            continue
        metas[i] = {
            'y0': int(ys.min()), 'y1': int(ys.max()),
            'x0': int(xs.min()), 'x1': int(xs.max()),
            'cx': int((xs.min() + xs.max()) // 2),
            'mid': int((ys.min() + ys.max()) // 2),
        }
    roles = {}
    for i, m in metas.items():
        # side if does not contain img_cx and another island at overlapping y does
        contains_cx = m['x0'] - 2 <= img_cx <= m['x1'] + 2
        peers = [j for j, o in metas.items() if j != i and not (o['y1'] < m['y0'] or o['y0'] > m['y1'])]
        peer_center = any(o['x0'] - 2 <= img_cx <= o['x1'] + 2 for j, o in metas.items() if j in peers)
        if (not contains_cx) and (peer_center or abs(m['cx'] - img_cx) > 60):
            roles[i] = 'side'
        else:
            roles[i] = 'center'
    return roles, metas


def island_metrics(fill_a, cut_a, shell_a, lab, i, ref_core, img_cx, role='center'):
    ys, xs = np.where(lab == i)
    y0, y1 = int(ys.min()), int(ys.max())
    x0, x1 = int(xs.min()), int(xs.max())
    mid = (y0 + y1) // 2
    cx = (x0 + x1) // 2
    segs = contiguous_segs(cut_a[mid])
    seg = seg_containing(segs, cx)
    if seg is None:
        return None
    seg_w = seg[1] - seg[0] + 1
    gap = shell_gap(shell_a, mid, cx)
    gap_w = gap[2] if gap else None

    if role == 'side':
        # booster pod body ≈ local shell gap / 0.80, else seg clipped
        if gap_w:
            core = max(gap_w + 4, int(round(gap_w / 0.80)))
        else:
            core = min(seg_w, max(ref_core // 2, x1 - x0 + 8))
    else:
        # center stack: prefer ref_core when cutaway has merged boosters
        if ref_core and seg_w > int(1.5 * ref_core):
            core = ref_core
        else:
            core = seg_w
        # If shell gap exists and is the true tank window on a thin rocket,
        # prefer max(gap-derived body, ref) carefully:
        # Electron: gap=49, seg=61, ref=61 → use seg (body) as core per cutaway rule.
        # Do not replace body core with gap.

    ifill = np.where(lab[mid] == i)[0]
    fw = int(ifill[-1] - ifill[0] + 1) if len(ifill) else (x1 - x0 + 1)
    ratio = fw / core if core else 0.0
    island = lab == i
    outside = int((island & (cut_a <= 8)).sum())
    out_pct = 100.0 * outside / int(island.sum()) if island.sum() else 0.0
    return {
        'id': i, 'y0': y0, 'y1': y1, 'x0': x0, 'x1': x1, 'mid': mid, 'cx': cx,
        'fw': fw, 'core': int(core), 'seg_w': seg_w, 'gap_w': gap_w, 'ratio': ratio,
        'out_pct': out_pct, 'role': role, 'px': int(island.sum()),
    }


def scale_island(src: np.ndarray, lab: np.ndarray, i: int, target_w: int, cut_a: np.ndarray) -> np.ndarray:
    ys, xs = np.where(lab == i)
    y0, y1 = int(ys.min()), int(ys.max())
    x0, x1 = int(xs.min()), int(xs.max())
    crop = src[y0:y1 + 1, x0:x1 + 1].copy()
    mask = lab[y0:y1 + 1, x0:x1 + 1] == i
    crop[~mask] = 0
    h, w = crop.shape[:2]
    if w <= 0 or h <= 0 or target_w <= 1:
        return src
    new = np.array(Image.fromarray(crop).resize((target_w, h), Image.Resampling.LANCZOS))
    cx = (x0 + x1) / 2.0
    new_x0 = int(round(cx - (target_w - 1) / 2.0))
    new_x1 = new_x0 + target_w
    H, W = src.shape[:2]
    out = src.copy()
    out[lab == i] = 0
    dst_x0 = max(0, new_x0)
    dst_x1 = min(W, new_x1)
    src_x0 = dst_x0 - new_x0
    src_x1 = src_x0 + (dst_x1 - dst_x0)
    if dst_x1 <= dst_x0:
        return out
    tile = new[:, src_x0:src_x1]
    region = out[y0:y1 + 1, dst_x0:dst_x1].copy()
    # Overwrite where tile has coverage (no double-alpha premultiply)
    cov = tile[:, :, 3] > 8
    region[cov] = tile[cov]
    ca = cut_a[y0:y1 + 1, dst_x0:dst_x1] <= 8
    region[ca] = 0
    out[y0:y1 + 1, dst_x0:dst_x1] = region
    return out


def list_tank_assets(vid: str):
    return [p.name for p in sorted(DRAW.glob(f'vehicle_{vid}_*tank*.png'))]


def audit_vid(vid: str, fills_dir: Path | None = None):
    cut_p = DRAW / f'vehicle_{vid}_cutaway.png'
    fills_p = (fills_dir or DRAW) / f'vehicle_{vid}_tank_fills.png'
    if not cut_p.exists() or not fills_p.exists():
        return None
    cut = load(cut_p)
    fills = load(fills_p)
    shell_p = DRAW / f'vehicle_{vid}_shell.png'
    shell = load(shell_p) if shell_p.exists() else None
    cut_a = cut[:, :, 3]
    fill_a = fills[:, :, 3]
    shell_a = shell[:, :, 3] if shell is not None else None
    ref = reference_core_width(cut_a, fill_a)
    lab, n = ndimage.label(fill_a > 8)
    img_cx = fills.shape[1] // 2
    roles, _ = classify_islands(lab, n, img_cx)
    islands = []
    for i in range(1, n + 1):
        if (lab == i).sum() < 80:
            continue
        m = island_metrics(fill_a, cut_a, shell_a, lab, i, ref, img_cx, roles.get(i, 'center'))
        if m:
            islands.append(m)
    return {'vid': vid, 'ref_core': ref, 'islands': islands}


def needs_rebuild(m, force_all=False):
    r = m['ratio']
    if m['out_pct'] > 0.05:
        return True, 'spill'
    if r < TINY:
        return True, 'tiny'
    if r > OVER:
        return True, 'flush'
    if force_all and not (TARGET_LO <= r <= TARGET_HI):
        return True, 'even'
    return False, 'ok'


def rebuild_array(src: np.ndarray, cut_a: np.ndarray, shell_a, ref_core: int, force_all=False, electron=False):
    lab, n = ndimage.label(src[:, :, 3] > 8)
    img_cx = src.shape[1] // 2
    roles, _ = classify_islands(lab, n, img_cx)
    out = src.copy()
    reports = []

    for i in range(1, n + 1):
        if (lab == i).sum() < 80:
            continue
        role = roles.get(i, 'center')
        m = island_metrics(src[:, :, 3], cut_a, shell_a, lab, i, ref_core, img_cx, role)
        if not m:
            continue
        need, reason = needs_rebuild(m, force_all=force_all)
        # Side boosters already in-band: keep (avoid merge/loss)
        if m['role'] == 'side' and not need and not electron:
            reports.append({'before': m, 'target_w': m['fw'], 'reason': 'keep_side', 'after': m})
            continue

        # Electron special: fills currently ~flush to shell gap (gap≈49, fw=51).
        # Core remains cutaway body (61). Target 84% body (=51) but CLIP into shell
        # gap with ~6% margin so fills sit INSIDE window, not over rim.
        # Result ~46–47 px → ~76–77% body but INSIDE window; nudge target to 0.80*body=49
        # which equals gap → still flush. Use 0.84 * gap as window-fit when gap/body > 0.75.
        if electron and m['gap_w'] and m['gap_w'] >= 0.70 * m['core']:
            # Core = cutaway body; target 84% but never wider than shell gap
            # so fills sit INSIDE the window (no rim spill). 0.84*61=51, gap=49 → 49 = 80.3%.
            target_w = max(3, int(round(TARGET * m['core'])))
            target_w = min(target_w, int(m['gap_w']))
            lo = max(3, int(np.floor(TARGET_LO * m['core'])))
            hi = max(lo, int(np.ceil(TARGET_HI * m['core'])))
            # allow equal-to-gap even if slightly below TARGET mid
            target_w = int(np.clip(target_w, min(lo, m['gap_w']), max(hi, min(target_w, m['gap_w']))))
            target_w = min(target_w, int(m['gap_w']))
            need, reason = True, 'electron_inside_gap'
        elif force_all or need:
            target_w = max(3, int(round(TARGET * m['core'])))
            lo = max(3, int(np.floor(TARGET_LO * m['core'])))
            hi = max(lo, int(np.ceil(TARGET_HI * m['core'])))
            target_w = int(np.clip(target_w, lo, hi))
        else:
            reports.append({'before': m, 'target_w': m['fw'], 'reason': 'keep', 'after': m})
            continue

        # Clamp: never exceed local cutaway segment - 2, and for center using ref_core
        # don't grow past 0.90*ref into booster zone beyond center column estimate
        segs = contiguous_segs(cut_a[m['mid']])
        seg = seg_containing(segs, m['cx'])
        if seg:
            max_w = max(3, (seg[1] - seg[0] + 1) - 2)
            if role == 'center' and ref_core and (seg[1] - seg[0] + 1) > 1.5 * ref_core:
                # keep within center column: ref_core with small pad
                max_w = min(max_w, int(round(TARGET_HI * ref_core)) + 2)
            if role == 'side' and m['gap_w']:
                max_w = min(max_w, m['gap_w'] + 2)
            target_w = min(target_w, max_w)

        # Avoid no-op resize jitter
        if abs(target_w - m['fw']) <= 1 and reason == 'keep':
            reports.append({'before': m, 'target_w': m['fw'], 'reason': 'keep', 'after': m})
            continue

        out = scale_island(out, lab, i, target_w, cut_a)
        # scrub dust from LANCZOS
        lab_scrub, n_scrub = ndimage.label(out[:, :, 3] > 8)
        for j in range(1, n_scrub + 1):
            if (lab_scrub == j).sum() < 80:
                out[lab_scrub == j] = 0
        lab2, _ = ndimage.label(out[:, :, 3] > 8)
        roles2, _ = classify_islands(lab2, int(lab2.max()), img_cx)
        best_j, best_d = None, 1e9
        for j in range(1, int(lab2.max()) + 1):
            ys, xs = np.where(lab2 == j)
            if len(ys) < 40:
                continue
            d = abs(((ys.min() + ys.max()) // 2) - m['mid']) + abs(((xs.min() + xs.max()) // 2) - m['cx'])
            if d < best_d:
                best_d, best_j = d, j
        after = None
        if best_j is not None:
            after = island_metrics(out[:, :, 3], cut_a, shell_a, lab2, best_j, ref_core, img_cx, roles2.get(best_j, role))
        reports.append({'before': m, 'target_w': target_w, 'reason': reason, 'after': after})
    return out, reports


def y_mask_from_before(before_arr, new_fills):
    ys = np.where(before_arr[:, :, 3] > 8)[0]
    if len(ys) == 0:
        return np.zeros_like(new_fills)
    y0, y1 = int(ys.min()), int(ys.max())
    xs = np.where((before_arr[:, :, 3] > 8).any(axis=0))[0]
    x0 = max(0, int(xs.min()) - 12)
    x1 = min(new_fills.shape[1] - 1, int(xs.max()) + 12)
    out = np.zeros_like(new_fills)
    out[y0:y1 + 1, x0:x1 + 1] = new_fills[y0:y1 + 1, x0:x1 + 1]
    # Keep only pixels that fall near original island columns OR new scaled content in band
    # Zero rows outside before opaque rows tightly? keep y band.
    return out


def process_vehicle(vid: str, force_all=False):
    cut_p = DRAW / f'vehicle_{vid}_cutaway.png'
    if not cut_p.exists():
        return None
    cut = load(cut_p)
    cut_a = cut[:, :, 3]
    shell_p = DRAW / f'vehicle_{vid}_shell.png'
    shell = load(shell_p) if shell_p.exists() else None
    shell_a = shell[:, :, 3] if shell is not None else None

    assets = list_tank_assets(vid)
    if not assets:
        return None

    fills_name = f'vehicle_{vid}_tank_fills.png'
    ref_src = load(BEFORE / fills_name) if (BEFORE / fills_name).exists() else load(DRAW / assets[0])
    ref_core = reference_core_width(cut_a, ref_src[:, :, 3])

    results = {'vid': vid, 'ref_core': ref_core, 'assets': {}, 'force_all': force_all}
    new_arrays = {}

    for name in assets:
        src = load(BEFORE / name)
        old_md5 = md5(BEFORE / name)
        if (src[:, :, 3] > 8).sum() < 80:
            new_arrays[name] = src
            results['assets'][name] = {
                'old_md5': old_md5, 'new_md5': old_md5, 'changed': False,
                'reports': [], 'note': 'empty_or_tiny',
            }
            continue
        new_arr, reports = rebuild_array(
            src, cut_a, shell_a, ref_core,
            force_all=force_all, electron=(vid == 'electron'),
        )
        new_arrays[name] = new_arr
        results['assets'][name] = {
            'old_md5': old_md5,
            'changed': not np.array_equal(src, new_arr),
            'reports': [
                {
                    'before_ratio': float(r['before']['ratio']),
                    'before_fw': r['before']['fw'],
                    'core': r['before']['core'],
                    'gap_w': r['before']['gap_w'],
                    'role': r['before']['role'],
                    'target_w': r['target_w'],
                    'reason': r['reason'],
                    'after_ratio': (float(r['after']['ratio']) if r['after'] else None),
                    'after_fw': (r['after']['fw'] if r['after'] else None),
                    'after_out_pct': (float(r['after']['out_pct']) if r['after'] else None),
                    'y': f"{r['before']['y0']}-{r['before']['y1']}",
                }
                for r in reports
            ],
        }

    # Recompose tank_fills: parts first, then UNION any fills-rebuild islands
    # not covered by parts (preserves G1/lm5 side boosters etc.)
    part_keys = [k for k in new_arrays if '_tank_s' in k]
    if part_keys and fills_name in new_arrays:
        comb = np.zeros_like(new_arrays[fills_name])
        for k in sorted(part_keys):
            p = new_arrays[k]
            m = p[:, :, 3] > 0
            comb[m] = p[m]
        fills_rebuilt = new_arrays[fills_name]
        # add fills pixels where comb is empty
        add = (fills_rebuilt[:, :, 3] > 8) & (comb[:, :, 3] <= 8)
        comb[add] = fills_rebuilt[add]
        new_arrays[fills_name] = comb

    fills_new = new_arrays.get(fills_name)
    if fills_new is not None:
        for extra in [f'vehicle_{vid}_booster_tank_fills.png', f'vehicle_{vid}_ship_tank_fills.png']:
            if extra not in new_arrays:
                continue
            before_e = load(BEFORE / extra)
            if (before_e[:, :, 3] > 8).sum() < 40:
                continue
            rebuilt_e = y_mask_from_before(before_e, fills_new)
            rebuilt_e[cut_a <= 8] = 0
            # For booster/ship, also intersect with scaled content that overlaps before's island roles
            new_arrays[extra] = rebuilt_e

    for name, arr in new_arrays.items():
        dest = FILLS_OUT / name
        save(arr, dest)
        results['assets'][name]['new_md5'] = md5(dest)
        results['assets'][name]['changed'] = results['assets'][name]['old_md5'] != results['assets'][name]['new_md5']

    results['changed_any'] = any(a['changed'] for a in results['assets'].values())
    return results


def make_preview(before_fills, after_fills, cut):
    H, W = cut.shape[:2]

    def under(fills):
        out = cut.copy()
        m = fills[:, :, 3] > 8
        out[m, :3] = (0.35 * out[m, :3].astype(np.float32) + 0.65 * fills[m, :3].astype(np.float32)).astype(np.uint8)
        out[m, 3] = 255
        return out

    b, a = under(before_fills), under(after_fills)
    ys = np.where((before_fills[:, :, 3] > 8) | (after_fills[:, :, 3] > 8) | (cut[:, :, 3] > 8))[0]
    y0, y1 = max(0, int(ys.min()) - 20), min(H, int(ys.max()) + 20)
    board = np.zeros((y1 - y0, W * 2 + 20, 4), dtype=np.uint8)
    board[:, :W] = b[y0:y1]
    board[:, W + 20:] = a[y0:y1]
    return board


def main():
    FILLS_OUT.mkdir(parents=True, exist_ok=True)
    QA.mkdir(parents=True, exist_ok=True)
    # clear previous fills
    for p in FILLS_OUT.glob('*.png'):
        p.unlink()

    audits = {}
    to_process = []
    for vid in PRIORITY:
        au = audit_vid(vid, BEFORE)
        if not au:
            continue
        audits[vid] = au
        force = vid == 'electron'
        tiny = any(isl['ratio'] < TINY or isl['ratio'] > OVER or isl['out_pct'] > 0.05 for isl in au['islands'])
        if force or tiny:
            to_process.append((vid, force))
        elif vid in ('cz8a', 'cz6a', 'h3', 'nuri', 'pallas1', 'fh', 'sr75'):
            # already in-band under core-window metric — record keep, no rewrite
            print(f'KEEP {vid} (in-band under core-window metric)')

    all_results = []
    for vid, force in to_process:
        print(f'Processing {vid} force_all={force} ref_core={audits[vid]["ref_core"]} ...')
        for isl in audits[vid]['islands']:
            print(f"  before y={isl['y0']}-{isl['y1']} role={isl['role']} fw={isl['fw']} core={isl['core']} ratio={isl['ratio']:.3f}")
        r = process_vehicle(vid, force_all=force)
        if r:
            all_results.append(r)
            au2 = audit_vid(vid, FILLS_OUT)
            if au2:
                for isl in au2['islands']:
                    flag = 'OK' if TARGET_LO <= isl['ratio'] <= TARGET_HI and isl['out_pct'] <= 0.05 else 'CHECK'
                    print(f"  after  y={isl['y0']}-{isl['y1']} role={isl['role']} fw={isl['fw']} core={isl['core']} ratio={isl['ratio']:.3f} out%={isl['out_pct']:.2f} {flag}")

    for vid in ['electron', 'gravity1', 'cz8a', 'cz2d']:
        bf = BEFORE / f'vehicle_{vid}_tank_fills.png'
        af = FILLS_OUT / f'vehicle_{vid}_tank_fills.png'
        cp = DRAW / f'vehicle_{vid}_cutaway.png'
        if bf.exists() and af.exists() and cp.exists():
            board = make_preview(load(bf), load(af), load(cp))
            save(board, QA / f'preview_{vid}_before_after.png')
            shutil.copy(QA / f'preview_{vid}_before_after.png', OUT / f'preview_{vid}_before_after.png')

    def conv(o):
        if isinstance(o, (np.floating, np.integer)):
            return float(o) if isinstance(o, np.floating) else int(o)
        if isinstance(o, dict):
            return {k: conv(v) for k, v in o.items()}
        if isinstance(o, list):
            return [conv(v) for v in o]
        return o

    report = {
        'audits_before': conv({k: {'ref_core': v['ref_core'], 'islands': v['islands']} for k, v in audits.items()}),
        'results': conv(all_results),
        'processed': [v for v, _ in to_process],
    }
    (OUT / 'report.json').write_text(json.dumps(report, indent=2))
    print('Done:', [v for v, _ in to_process])


if __name__ == '__main__':
    main()
