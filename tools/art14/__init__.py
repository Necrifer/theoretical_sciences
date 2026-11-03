"""
Round-14 item art: transformation animations (grow, collapse, fade, shatter and reform, phase between forms) for the
ingots and the endgame items. Same group-file format as art11; installed after art11, so these override it.
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
            m = importlib.import_module('art14.' + name)
        except Exception as e:
            print('art14: could not load', name, '-', e)
            continue
        for k in getattr(m, 'ITEMS', {}):
            anim.NARRATIVES.pop(k, None)          # a new builder replaces any older story loop
        items3d.ITEMS.update(getattr(m, 'ITEMS', {}))
        anim.RECIPE_OVERRIDE.update(getattr(m, 'RECIPES', {}))
        anim.NARRATIVES.update(getattr(m, 'NARRATIVES', {}))
        anim.FRAMES_OVERRIDE.update(getattr(m, 'FRAMES', {}))
