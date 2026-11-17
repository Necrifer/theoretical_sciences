"""
1.3.1: pixel-identical savings on animated textures (performance review C1, steps 1 and 2).

1. An animation whose frames are all the same is no animation: the strip is cropped to one frame and its .mcmeta
   removed (the game then never re-uploads it).
2. A strip with repeated frames stores each different frame once, and lists the order in "frames"; a frame shown
   several steps in a row becomes one longer step (one upload instead of several). What is seen is unchanged, step for
   step.

Strips with interpolate: true are left alone (interpolation blends neighbouring frames, which this would change).

Run:  python3 tools/dedupe_frames.py [--dry]
"""
import glob
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/textures')


def main(dry=False):
    stat = dict(files=0, static=0, deduped=0, frames_before=0, frames_after=0, bytes_before=0, bytes_after=0)
    for meta in sorted(glob.glob(os.path.join(TEX, '**/*.png.mcmeta'), recursive=True)):
        png = meta[:-len('.mcmeta')]
        if not os.path.exists(png):
            continue
        m = json.load(open(meta))
        a = m.get('animation', {})
        if a.get('interpolate') or 'width' in a or 'height' in a:
            continue
        img = Image.open(png)
        if img.mode not in ('RGBA', 'RGB', 'LA', 'L'):
            img = img.convert('RGBA')         # (a palette image is worked on as plain colours, which look the same)
        mode = img.mode
        arr = np.array(img)
        w = arr.shape[1]
        h = w
        if arr.shape[0] % h:
            continue
        n = arr.shape[0] // h
        frames = [arr[i * h:(i + 1) * h] for i in range(n)]
        ft = int(a.get('frametime', 1))
        # the order shown, as (frame index, time)
        if 'frames' in a:
            seq = []
            for f in a['frames']:
                if isinstance(f, int):
                    seq.append((f, ft))
                else:
                    seq.append((int(f['index']), int(f.get('time', ft))))
        else:
            seq = [(i, ft) for i in range(n)]
        stat['files'] += 1
        stat['frames_before'] += n
        # map each frame to the first identical one
        uniq, first = [], {}
        remap = []
        for f in frames:
            key = f.tobytes()
            if key not in first:
                first[key] = len(uniq)
                uniq.append(f)
            remap.append(first[key])
        if len(uniq) == n and 'frames' not in a:
            stat['frames_after'] += n
            continue
        seq = [(remap[i], t) for i, t in seq]
        # merge consecutive repeats
        merged = []
        for i, t in seq:
            if merged and merged[-1][0] == i:
                merged[-1] = (i, merged[-1][1] + t)
            else:
                merged.append((i, t))
        if len(merged) > 1 and merged[0][0] == merged[-1][0] and len(set(i for i, _ in merged)) > 1:
            pass  # (the loop wraps: kept as two entries, so the cycle's start stays where it was)
        stat['bytes_before'] += os.path.getsize(png)
        if len(uniq) == 1:
            stat['static'] += 1
            stat['frames_after'] += 1
            if not dry:
                Image.fromarray(uniq[0], mode).save(png, optimize=True)
                os.remove(meta)
            stat['bytes_after'] += os.path.getsize(png) if not dry else 0
            continue
        stat['deduped'] += 1
        stat['frames_after'] += len(uniq)
        if not dry:
            Image.fromarray(np.concatenate(uniq, axis=0), mode).save(png, optimize=True)
            na = {k: v for k, v in a.items() if k != 'frames'}
            na['frametime'] = ft
            na['frames'] = [i if t == ft else {'index': i, 'time': t} for i, t in merged]
            m['animation'] = na
            with open(meta, 'w') as f:
                json.dump(m, f, separators=(', ', ': '))
            stat['bytes_after'] += os.path.getsize(png)
    print(stat)


if __name__ == '__main__':
    main('--dry' in sys.argv)
