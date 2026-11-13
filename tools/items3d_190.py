"""
1.9.0 item art: the twelve Remembrance Tablets (helpers from items3d_180.py). A slab of marble with a gold rim; each
carries as many lit marks as its number, so they can be told apart at a glance.

Run:  python3 tools/items3d_190.py [ids...]
      python3 tools/items3d_190.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
import items3d_171 as P
import items3d_180 as H
from render3d import rotx, roty, rotz

P.B.clear()
P.MOTIONS.clear()
TAU = math.tau


def tablet(n):
    @P.reg('remembrance_tablet_%d' % n, (('rock',), ('glint',)))
    def _(c1, c2, t, n=n):
        R = rotz(0.1) @ rotx(-0.15)
        parts = [H.Bx((0.42, 0.56, 0.07), 0, rot=R, r=0.06), H.Bx((0.46, 0.05, 0.09), 2, tuple(R @ np.array([0, 0.56, 0])), R), H.Bx((0.46, 0.05, 0.09), 2, tuple(R @ np.array([0, -0.56, 0])), R)]
        for i in range(n):
            row, col = divmod(i, 3)
            x, y = -0.24 + col * 0.24, 0.36 - row * 0.24
            lit = 1 if (i + int(t * n)) % n == 0 else 3
            parts.append(H.Bx((0.07, 0.07, 0.03), lit, tuple(R @ np.array([x, y, -0.09])), R, 0.02))
        return parts, H.mats('#ece6da', '#ffffff', '#e0b860', '#ffd870'), H.FRONT


for i in range(1, 13):
    tablet(i)
ITEMS = list(P.B)

if __name__ == '__main__':
    import items3d as I
    from PIL import Image
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        W = Image.new('RGBA', (12 * 100, 100), (58, 58, 64, 255))
        for i, iid in enumerate(ITEMS):
            parts, mm, view = P.B[iid]('#ffffff', '#ffffff', 0.15)
            img = I.render(parts, mm, **view)
            W.alpha_composite(Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((96, 96), Image.NEAREST), (i * 100 + 2, 2))
        W.save(sys.argv[2])
    else:
        for iid in ITEMS:
            if sys.argv[1:] and iid not in sys.argv[1:]:
                continue
            P13.save_strip(P.frames(iid), os.path.join(P13.TEX, iid + '.png'))
        print('rendered', len(ITEMS))
