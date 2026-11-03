"""
Round-11 item art: unique 3D models and animations per item, written in group files art11/g*.py.
Each group module may define:
  ITEMS      {item_id: builder(c1, c2, t) -> (parts, mats, view, overlay_or_None)}   (see items3d.py builders)
  RECIPES    {item_id: (motions, overlays)}   motions from anim.apply_motions, overlays: glint sparkles orbit embers glitch
  NARRATIVES {item_id: fn(c1, c2, t) -> (parts, mats, view, overlay_or_None)}   80-frame story loop, used as-is
  FRAMES     {item_id: n}   optional loop length (default 40, narratives 80)
install() merges them into items3d / anim. Later groups override earlier ones.
"""
import glob
import importlib
import os

_done = [False]


def install():
    if _done[0]:
        return
    _done[0] = True
    import anim
    import items3d
    here = os.path.dirname(os.path.abspath(__file__))
    for f in sorted(glob.glob(os.path.join(here, 'g*.py'))):
        name = os.path.basename(f)[:-3]
        try:
            m = importlib.import_module('art11.' + name)
        except Exception as e:                  # a broken group must not break the whole texture run
            print('art11: could not load', name, '-', e)
            continue
        items3d.ITEMS.update(getattr(m, 'ITEMS', {}))
        anim.RECIPE_OVERRIDE.update(getattr(m, 'RECIPES', {}))
        anim.NARRATIVES.update(getattr(m, 'NARRATIVES', {}))
        anim.FRAMES_OVERRIDE.update(getattr(m, 'FRAMES', {}))
