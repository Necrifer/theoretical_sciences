"""
1.7.2 item art: the Consilience Core (helpers from items3d_171.py).

Run:  python3 tools/items3d_172.py            (writes the texture)
      python3 tools/items3d_172.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
import items3d_171 as P
from render3d import Part, Material, sd_sphere, sd_torus, rotx, roty, rotz
from items3d_new import em_all

TAU = math.tau
SEVEN = ['#7fb0ff', '#70e0a0', '#c0a0ff', '#ff90b0', '#ffd070', '#80e0e0', '#d0a070']     # the seven fields
SIX = '#fff4d8'                                                                             # the six last materials


def build(c1, c2, t):
    """A white heart, seven beads of the seven fields circling it, and a tilted hoop of six round those."""
    mats = [Material(c, '#ffffff', emissive=c, pattern=em_all(), metal=0.3) for c in SEVEN]
    mats.append(Material(SIX, '#ffffff', emissive=SIX, pattern=em_all(), metal=0.2))
    mats.append(Material('#8a7ab8', '#ffffff', metal=0.85))
    parts = [Part(lambda p: sd_sphere(p, 0.2 + 0.015 * math.sin(TAU * t)), 7)]
    for i in range(7):
        a = i * TAU / 7 + t * TAU / 7
        parts.append(Part(lambda p: sd_sphere(p, 0.085), i, pos=(math.cos(a) * 0.4, math.sin(a) * 0.4, 0)))
    R = rotx(1.15) @ rotz(0.35)
    parts.append(Part(lambda p: sd_torus(p, 0.6, 0.035), 8, rot=R))
    for i in range(6):
        a = i * TAU / 6 - t * TAU / 6
        v = R @ np.array([math.cos(a) * 0.6, 0.0, math.sin(a) * 0.6])
        parts.append(Part(lambda p: sd_sphere(p, 0.075), 7, pos=tuple(v)))
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.0, zoom=1.05)


P.B.clear()
P.MOTIONS.clear()
P.B['consilience_core'] = build
P.MOTIONS['consilience_core'] = (('rock',), ('glint',))

if __name__ == '__main__':
    import items3d as I
    from PIL import Image
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        parts, mats, view = build('#ffffff', '#ffffff', 0.15)
        img = I.render(parts, mats, **view)
        Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((256, 256), Image.NEAREST).save(sys.argv[2])
    else:
        P13.save_strip(P.frames('consilience_core'), os.path.join(P13.TEX, 'consilience_core.png'))
        print('rendered consilience_core')
