#!/usr/bin/env python3
"""Seamless-loop fix for three containment-cache textures drawn by round14.py (which is not edited here).

round14's emblems for these caches do not loop:
  tesseract (bulk_cache)    the inner square grows 1.5 -> 3.0 then snaps back to 1.5
  bubbles   (foam_cache)    bubbles rise 2 px then snap back down
  rooms     (hilbert_locker) the lit door hops one room every 4 frames (a hard step at every 4th frame)
This script re-draws only those caches' strips with periodic versions of the three emblems and leaves every
other cache untouched.  Run:  python3 tools/anim_fix_caches.py
(If round14.py's 'tex' command is ever re-run, run this afterwards, or port these emblems into round14.)
"""
import json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import round14 as R

_orig = R._emblem


def _emblem(kind, x, y, f):
    cx, cy = 7.5, 10.5
    dx, dy = x - cx, y - cy
    ph = f / 16 * math.tau
    if kind == 'tesseract':
        # the inner cube unfolds outward from the centre until it becomes the outer cube; a new one is born
        m = max(abs(dx), abs(dy))
        s = 0.5 + 3.0 * f / 16                       # 0.5 (centre) .. 3.5 (outer frame) over one loop
        inner = abs(m - s) < 0.6
        return 1.0 if (abs(m - 3.5) < 0.6 or inner or (abs(abs(dx) - abs(dy)) < 0.6 and 1.5 < m < 3.5)) else 0.0
    if kind == 'bubbles':
        # bubbles bob up and down (each with its own phase) instead of teleporting back to the bottom
        v = 0.0
        for bx, by, br, sp in ((5.5, 11.5, 1.8, 0), (9.5, 9.5, 2.2, 5), (8, 13, 1.0, 9)):
            yy = by - 1.0 + math.cos(ph + sp / 16 * math.tau)
            d = math.hypot(x - bx, y - yy)
            if abs(d - br) < 0.55: v = 1.0
        return v
    if kind == 'rooms':
        # the light walks from room to room, fading across instead of hopping
        if 7 <= y <= 14 and 2 <= x <= 13:
            col = (x - 2) // 3
            door = (x - 2) % 3 == 1 and y >= 10
            pos = f / 4.0                                  # 0..4 rooms per loop
            dist = min((col - pos) % 4, (pos - col) % 4)   # circular distance to the light
            lit = max(0.0, 1.0 - dist)
            return (0.45 + 0.55 * lit) if door else 0.0
        return 0.0
    return _orig(kind, x, y, f)


R._emblem = _emblem


def main():
    from PIL import Image
    out = os.path.join(R.ASSETS, 'textures', 'block')
    want = {'tesseract', 'bubbles', 'rooms'}
    n = 0
    for cid, _, body, glow, emblem, _ in R.CACHES:
        if emblem not in want:
            continue
        for part, name in (('side', cid), ('top', cid + '_top'), ('top_open', cid + '_top_open'), ('bottom', cid + '_bottom')):
            strip = Image.new('RGBA', (16, 256))
            for f in range(16):
                strip.paste(R._frame(cid, part, body, glow, emblem, f), (0, f * 16))
            strip.save(os.path.join(out, name + '.png'))
            with open(os.path.join(out, name + '.png.mcmeta'), 'w') as fh:
                json.dump({'animation': {'frametime': 3, 'interpolate': True}}, fh)
            n += 1
    print('anim_fix_caches: rewrote', n, 'cache textures')


if __name__ == '__main__':
    main()
