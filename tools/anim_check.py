#!/usr/bin/env python3
"""Check that every animated texture loops seamlessly.

For each PNG with a .png.mcmeta animation and height > width, the displayed frame sequence (respecting an explicit
"frames" list) is compared:
  seam    = mean abs diff (premultiplied RGB + alpha) between the last and the first displayed frame
  median  = median of the consecutive-frame diffs inside the sequence (identical "hold" frames ignored)
  local   = median of the 4 steps before and 4 steps after the seam
Flags:
  SEAM  seam > floor, seam > ratio * median and seam > 1.8 * local   (a visible jump back to the start)
  DUP   last frame identical to the first in an animation that never holds a frame (first frame shown twice)
  step  a SEAM/DUP hit that was reviewed and is a deliberate discrete-step rhythm (see STEP_OK); not a failure
Exit status 1 when anything is flagged SEAM/DUP.
Usage: python3 tools/anim_check.py [--all] [--ratio 2.5] [--floor 1.5] [name-substring ...]
"""
import json, os, sys, glob
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..',
                    'src/main/resources/assets/theoreticalsciences/textures')


LOCAL_RATIO = 1.8

# Animations built from deliberate discrete steps (a light hopping between cells, a particle teleporting between
# places, ...).  Their seam is exactly as big as every other step of the rhythm, so they loop correctly; they are
# reported as 'step' instead of 'SEAM'.
STEP_OK = {
    'item/hyperdimensional_pearl.png': 'teleports between four places, four equal jumps per loop',
    'item/quantum_processor.png': 'X/Z stabilisers blink in alternation, four flips per loop',
    'item/holographic_matrix.png': 'shell bits re-randomise eight times per loop',
    'item/g2_manifold.png': 'one Fano line lit at a time, seven hops per loop',
    'item/thought_matrix.png': 'copy-pulse hops cell to cell, eight hops per loop',
    'item/well_ordering_charm.png': 'light hops bead to bead, seven hops per loop',
    'item/containment_cell.png': 'three coils pulse in turn, six switches per loop',
    'item/error_correction_lattice.png': 'a random qubit flips and is voted out six times per loop',
    'item/reality_anchor.png': 'glitch specks re-randomise every two frames',
    'item/antihydrogen_crystal.png': 'Balmer line colour steps four times per loop',
    'item/cosmic_lens.png': 'focus sparkle flashes on twice per loop',
    'item/autonomous_calibrator.png': 'calibration spark flashes on four times per loop',
    'item/antihelium_nucleus.png': 'annihilation spark flashes on twice per loop',
    'item/transfinite_processor.png': 'Zeno pulse: lit layer steps up 0,1,2..5 and restarts at the base (one of its steps)',
    'item/degenerate_matter_chunk.png': 'periodic hop that slams down on the seam (squash is symmetric about the impact)',
    'item/inertia_damper.png': 'violent periodic shake: every step is large, the seam is ordinary',
    'item/natural_number_lattice.png': 'glyph tablet: fast but periodic glint/breathe across the seam',
    'item/path_integral.png': 'glyph tablet: fast but periodic glint/rock across the seam',
    'item/empirical_dossier.png': 'sheet ruling dithers while the clipboard rocks; the plot now wipes to blank before the seam',
    'block/transfinite_casing.png': 'clamped glow max(0, sin): frames 15 and 0 both fall in the dark part of one periodic cycle',
}


def frames_of(path):
    meta = json.load(open(path + '.mcmeta')).get('animation', {})
    im = np.asarray(Image.open(path).convert('RGBA'), dtype=np.float32)
    h, w = im.shape[:2]
    fw = meta.get('width', w)
    fh = meta.get('height', fw)
    n = h // fh
    raw = [im[i * fh:(i + 1) * fh, :fw] for i in range(n)]
    order = []
    for f in meta.get('frames', range(n)):
        idx = f['index'] if isinstance(f, dict) else f
        t = f.get('time', meta.get('frametime', 1)) if isinstance(f, dict) else meta.get('frametime', 1)
        order.append((idx, t))
    return raw, order, meta, (fw, fh, n)


def diff(a, b):
    # premultiply so invisible pixels don't count
    pa = a[..., :3] * (a[..., 3:] / 255.0)
    pb = b[..., :3] * (b[..., 3:] / 255.0)
    return float(np.mean(np.abs(pa - pb)) + 0.5 * np.mean(np.abs(a[..., 3] - b[..., 3])))


def check(path, ratio, floor):
    raw, order, meta, (fw, fh, n) = frames_of(path)
    seq = [raw[i] for i, _ in order if i < n]
    if len(seq) < 2:
        return None
    cons = [diff(seq[i], seq[i + 1]) for i in range(len(seq) - 1)]
    seam = diff(seq[-1], seq[0])
    moving = [c for c in cons if c > 0.05]          # ignore deliberate holds (identical frames)
    med = float(np.median(moving)) if moving else 0.0
    holds = len(cons) - len(moving)
    flags = []
    near = cons[-4:] + cons[:4]
    loc = float(np.median(near)) if near else med
    if seam <= 0.05 and med > 0.5 and holds == 0:
        # the animation never holds a frame, so a last frame equal to the first is a double-shown frame
        flags.append('DUP')
    elif seam > floor and seam > ratio * max(med, 1e-6) and seam > LOCAL_RATIO * max(loc, 1e-6):
        # the seam is compared with the global typical step AND with the steps right around it (4 before, 4 after),
        # so a fast but continuous motion that crosses the seam (a glint sweep, a spin) is not mistaken for a jump
        flags.append('SEAM')
    if flags and os.path.relpath(path, ROOT) in STEP_OK:
        flags = ['step']              # reviewed: deliberate discrete-step animation, the seam is one of its steps
    return dict(path=path, n=n, seq=len(seq), fw=fw, seam=seam, med=med,
                score=seam / max(med, 1e-3), flags=flags, holds=holds, loc=seam / max(loc, 1e-3),
                interp=meta.get('interpolate', False), ft=meta.get('frametime', 1))


def main():
    args = sys.argv[1:]
    ratio, floor, show_all = 2.0, 1.5, False
    pats = []
    while args:
        a = args.pop(0)
        if a == '--ratio': ratio = float(args.pop(0))
        elif a == '--floor': floor = float(args.pop(0))
        elif a == '--all': show_all = True
        else: pats.append(a)
    res = []
    for p in sorted(glob.glob(os.path.join(ROOT, '*', '*.png'))):
        if not os.path.exists(p + '.mcmeta'):
            continue
        w, h = Image.open(p).size
        if h <= w:
            continue
        name = os.path.relpath(p, ROOT)
        if pats and not any(s in name for s in pats):
            continue
        r = check(p, ratio, floor)
        if r:
            r['name'] = name
            res.append(r)
    res.sort(key=lambda r: -r['score'])
    bad = [r for r in res if r['flags'] and r['flags'] != ['step']]
    for r in (res if show_all else [r for r in res if r['flags']]):
        print(f"{','.join(r['flags']) or 'ok':5s} {r['name']:60s} frames={r['n']:3d} "
              f"holds={r['holds']:2d} w={r['fw']:3d} seam={r['seam']:7.2f} med={r['med']:6.2f} x{r['score']:6.1f} local x{r['loc']:5.1f}")
    print(f"\n{len(res)} animated textures checked, {len(bad)} flagged "
          f"(ratio>{ratio}, floor>{floor})")
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
