#!/usr/bin/env python3
"""Re-render only the named 3D item textures through the real pipeline (gen_textures._render3d_job), instead of
running the whole texture generator.  Fluid cells are named <fluid>_cell.
Usage: python3 tools/anim_regen.py item_id [item_id ...]
The .mcmeta is (re)written exactly as gen_textures.main() writes it (frametime 1, no interpolation)."""
import json, os, sys
from multiprocessing import Pool

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_textures as G


def _jobs(names):
    import anim
    groups, jobs = {}, {}
    for it in G.C['items']:
        cat = anim.CATEGORY.get(it['art'], 'device')
        jobs[it['id']] = (it['id'], it['art'], it['c1'], it['c2'], groups.get(cat, 0))
        groups[cat] = groups.get(cat, 0) + 1
    for fl in G.C['fluids']:
        jobs[fl['id'] + '_cell'] = ('@cell:' + fl['id'], 'gas' if fl['gas'] else 'liquid', fl['c1'], fl['c2'], 0)
    out = []
    for n in names:
        if n not in jobs:
            raise SystemExit('unknown item ' + n)
        out.append((n, jobs[n]))
    return out


def main(names):
    todo = _jobs(names)
    with Pool() as pool:
        results = pool.map(G._render3d_job, [j for _, j in todo], chunksize=1)
    for (name, _), frames in zip(todo, results):
        if frames is None:
            print('skip (no 3D builder):', name)
            continue
        path = os.path.join(G.TEX, 'item', name + '.png')
        old = open(path + '.mcmeta').read() if os.path.exists(path + '.mcmeta') else None
        G.save_strip(frames, path, frametime=1, interpolate=False)
        if old is not None and json.loads(old) != json.load(open(path + '.mcmeta')):
            open(path + '.mcmeta', 'w').write(old)        # keep a hand-edited animation block
            print('  kept existing mcmeta for', name, old.strip())
        print('wrote', name, len(frames), 'frames')


if __name__ == '__main__':
    main(sys.argv[1:])
