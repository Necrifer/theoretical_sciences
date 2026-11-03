"""Round 37: the Empty Containment Cell drawn as the same 3D canister as the filled cells, with nothing inside -
dark glass with a faint containment shimmer - so empty and filled cells read as one item family.
Run: python3 tools/empty_cell.py   (writes textures/item/containment_cell.png; then tools/trim_animations.py)"""
import os, sys, math
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_textures as G
import anim, items3d
from render3d import Material

TAU = 2 * math.pi


def frames(n=40):
    out = []
    for f in range(n):
        t = f / n
        parts, mats, view, _ = items3d.b_cell('#a0c8e0', '#a0c8e0', t, fluid_cell=True)
        # no liquid: the glass shows only a faint field shimmer drifting up the canister
        shimmer = lambda p, nrm, t=t: (np.zeros(p.shape[:-1]),
                                       0.18 * (0.5 + 0.5 * np.sin(p[..., 1] * 9 - t * TAU * 2)) * (np.abs(p[..., 1]) < 0.6))
        mats[1] = Material('#20283a', '#a0c0ff', metal=1.0, emissive=items3d.H('#5fb8e8'), pattern=shimmer)
        view = anim.apply_motions(parts, mats, view, t, ('bob',), anim.h('containment_cell'))
        img = items3d.render(parts, mats, **view)
        anim.ov_glint(img, t, anim.h('containment_cell'), start=0.2)
        out.append(img)
    return out


if __name__ == '__main__':
    G.setres(32)
    path = os.path.join(G.TEX, 'item', 'containment_cell.png')
    G.save_strip(frames(), path, frametime=1, interpolate=False)
    print('wrote', path)
