"""
1.13.0 item art, Patch C2 (the station grows): satellites, the sweeper, and the parts they are built from
(helpers from items3d_180.py).

Run:  python3 tools/items3d_1130.py [ids...]     (writes the textures)
      python3 tools/items3d_1130.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
import items3d_171 as P
from render3d import Part, Material, sd_sphere, sd_box, sd_torus, sd_cyl, sd_capsule, sd_gem, rotx, roty, rotz
from items3d_new import sd_ellipsoid, em_all, shade_noise

TAU = math.tau
P.B.clear()
P.MOTIONS.clear()
ISO = dict(view_pitch=0.45, view_yaw=0.6, zoom=1.1)
TILT = dict(view_pitch=0.3, view_yaw=0.5, zoom=1.15)


def mats(*cols):
    """Odd places glow: (base, glow, base2, glow2...)."""
    out = []
    for i, c in enumerate(cols):
        out.append(Material(c, '#ffffff', emissive=c, pattern=em_all(), metal=0.3) if i % 2 else Material(c, '#ffffff', pattern=shade_noise(6, 0.1, 4), metal=0.55))
    return out


def S(r, m, pos=(0, 0, 0)):
    return Part(lambda p: sd_sphere(p, r), m, pos=pos)


def E(r, m, pos=(0, 0, 0), rot=None):
    return Part(lambda p: sd_ellipsoid(p, r), m, pos=pos, rot=rot)


def Bx(b, m, pos=(0, 0, 0), rot=None, r=0.02):
    return Part(lambda p: sd_box(p, b, r), m, pos=pos, rot=rot)


def C(a, b, r, m):
    return Part(lambda p: sd_capsule(p, a, b, r), m)


def Cy(r, h, m, pos=(0, 0, 0), rot=None):
    return Part(lambda p: sd_cyl(p, r, h, 0.02), m, pos=pos, rot=rot)


def T(R, r, m, pos=(0, 0, 0), rot=None):
    return Part(lambda p: sd_torus(p, R, r), m, pos=pos, rot=rot)


def reg(iid, motions=(('rock',), ('glint',))):
    return P.reg(iid, motions)


def nozzle(y, r0, r1, h, m, n=4):
    """A bell, as stacked rings widening downward."""
    out = []
    for i in range(n):
        f = i / (n - 1)
        out.append(Cy(r0 + (r1 - r0) * f, h / n * 0.6, m, (0, y - h * f, 0)))
    return out


def stage(tank, band, glow, t, engine='bell'):
    """A Transfer Stage: a tank on its side, tilted, with bands, and its engine at the back."""
    R = rotz(-0.95)
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Part(lambda p: sd_capsule(p, (0, -0.32, 0), (0, 0.36, 0), 0.26), 0, rot=R)]
    for y in (-0.15, 0.15):
        parts.append(Part(lambda p, y=y: sd_torus(p - np.array([0, y, 0]), 0.265, 0.03), 2, rot=R))
    back = R @ np.array([0, -0.6, 0])
    if engine == 'bell':
        for i in range(4):
            f = i / 3
            parts.append(Part(lambda p, f=f: sd_cyl(p, 0.09 + 0.1 * f, 0.03, 0.01), 2, pos=tuple(R @ np.array([0, -0.5 - 0.12 * f, 0])), rot=R))
        parts.append(S(0.08 + 0.03 * k, 1, tuple(R @ np.array([0, -0.72, 0]))))
    elif engine == 'grid':
        for s in (-1, 1):
            parts.append(Part(lambda p: sd_cyl(p, 0.11, 0.05, 0.01), 2, pos=tuple(R @ np.array([s * 0.13, -0.6, 0])), rot=R))
            parts.append(S(0.06 + 0.02 * k, 1, tuple(R @ np.array([s * 0.13, -0.7, 0]))))
        for s in (-1, 1):                                                     # solar wings
            parts.append(Bx((0.04, 0.3, 0.2), 3, tuple(R @ np.array([s * 0.44, 0.05, 0])), R, r=0.01))
    elif engine == 'ring':
        parts.append(Part(lambda p: sd_torus(p, 0.2, 0.05), 3, pos=tuple(back), rot=R))
        parts.append(S(0.1 + 0.04 * k, 1, tuple(R @ np.array([0, -0.66, 0]))))
    return parts, mats(tank, glow, band, '#3060c0' if engine == 'grid' else glow), TILT



def wings(n, span, col_i, R=None):
    """Solar wings either side of a bus."""
    out = []
    for s in (-1, 1):
        out.append(Bx((span / 2, 0.015, 0.16), col_i, (s * (0.3 + span / 2), 0, 0), R, r=0.005))
        for k in range(1, n):
            out.append(Bx((0.006, 0.02, 0.16), 2, (s * (0.3 + span * k / n), 0, 0), R, r=0.002))
    return out


def sat(bus, extra, mm, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Bx((0.2, 0.22, 0.2), 0, (0, 0, 0), r=0.03)] + wings(3, 0.5, 3) + extra(k)
    return parts, mm, TILT


@reg('weather_satellite', (('spin',), ('glint',)))
def _(c1, c2, t):
    return sat(0, lambda k: [Cy(0.09, 0.08, 2, (0, -0.28, 0)), S(0.05 + 0.02 * k, 1, (0, -0.36, 0)), C((0, 0.2, 0), (0, 0.42, 0), 0.025, 2)],
               mats('#d8d4c8', '#80d0ff', '#9aa0aa', '#2a4a9a'), t)


@reg('solar_monitor_satellite', (('spin',), ('glint',)))
def _(c1, c2, t):
    return sat(0, lambda k: [C((0, 0.2, 0), (0, 0.5, 0), 0.02, 2), S(0.04, 1, (0, 0.52, 0)), Cy(0.08, 0.14, 2, (0.12, 0.26, 0.12)),
                             S(0.04 + 0.02 * k, 1, (0.12, 0.38, 0.12))], mats('#e0c060', '#ffd070', '#9aa0aa', '#2a4a9a'), t)


@reg('relay_satellite', (('spin',), ('glint',)))
def _(c1, c2, t):
    return sat(0, lambda k: [Part(lambda p: np.maximum(sd_sphere(p, 0.32), -sd_sphere(p - np.array([0, 0.12, 0]), 0.3)),
                                  2, pos=(0, -0.42, 0)), S(0.04 + 0.02 * k, 1, (0, -0.24, 0)), C((0, -0.2, 0), (0, -0.3, 0), 0.02, 2)],
               mats('#c8ccd4', '#80ffd0', '#e8e8ec', '#2a4a9a'), t)


@reg('power_satellite', (('rock',), ('pulse',)))
def _(c1, c2, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Bx((0.62, 0.02, 0.62), 3, (0, 0.18, 0), r=0.01)]
    for i in range(-2, 3):
        parts.append(Bx((0.62, 0.025, 0.006), 2, (0, 0.18, i * 0.25), r=0.002))
    parts += [Cy(0.05, 0.3, 2, (0, -0.05, 0)), Part(lambda p: sd_cyl(p, 0.34, 0.03, 0.01), 0, pos=(0, -0.3, 0)),
              S(0.06 + 0.04 * k, 1, (0, -0.42, 0))]
    return parts, mats('#5a5e68', '#ff9050', '#9aa0aa', '#20306a'), TILT


@reg('debris_sweeper', (('spin',), ('glint',)))
def _(c1, c2, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Bx((0.16, 0.16, 0.16), 0, r=0.03), C((0, 0.1, 0), (0, 0.32, 0.0), 0.04, 2), S(0.04 + 0.02 * k, 1, (0, 0.36, 0))]
    for a in range(4):
        ang = a * TAU / 4 + 0.4
        parts.append(Part(lambda p, ang=ang: sd_box(p, (0.24, 0.008, 0.24), 0.004), 3, pos=(0.34 * math.cos(ang), -0.08, 0.34 * math.sin(ang)),
                          rot=roty(ang) @ rotx(0.3)))
    return parts, mats('#b8bcc4', '#ff5050', '#9aa0aa', '#e8e0c8'), TILT


@reg('zblan_fibre', (('rock',), ('glint',)))
def _(c1, c2, t):
    parts = [T(0.36, 0.05, 2, (0, 0, 0), rotx(1.2)), T(0.28, 0.05, 2, (0, 0, 0), rotx(1.2))]
    for i in range(10):
        a = i * TAU / 10 + TAU * t * 0.1
        parts.append(S(0.035, 1, tuple(rotx(1.2) @ np.array([0.32 * math.cos(a), 0, 0.32 * math.sin(a)]))))
    return parts, mats('#c8e8f0', '#d0f8ff', '#e0e4ea'), ISO


@reg('multijunction_solar_cell', (('rock',), ('glint',)))
def _(c1, c2, t):
    R = rotx(-0.7)
    parts = [Part(lambda p: sd_box(p, (0.4, 0.03, 0.4), 0.01), 0, pos=(0, -0.05, 0), rot=R),
             Part(lambda p: sd_box(p, (0.38, 0.02, 0.38), 0.01), 3, pos=tuple(R @ np.array([0, 0.0, 0])), rot=R),
             Part(lambda p: sd_box(p, (0.36, 0.015, 0.36), 0.01), 1, pos=tuple(R @ np.array([0, 0.04, 0])), rot=R)]
    for i in (-0.2, 0, 0.2):
        parts.append(Part(lambda p: sd_box(p, (0.36, 0.02, 0.006), 0.002), 2, pos=tuple(R @ np.array([0, 0.06, i])), rot=R))
    return parts, mats('#3a2a5a', '#5060c0', '#d8c070', '#a03050'), ISO


@reg('microwave_phased_array', (('rock',), ('pulse',)))
def _(c1, c2, t):
    R = rotx(-0.7)
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Part(lambda p: sd_box(p, (0.44, 0.04, 0.44), 0.02), 0, rot=R)]
    for i in range(-2, 3):
        for j in range(-2, 3):
            parts.append(S(0.045 + (0.01 * k if (i + j) % 2 == 0 else 0), 1, tuple(R @ np.array([i * 0.17, 0.05, j * 0.17]))))
    return parts, mats('#4a4e58', '#ffb060', '#9aa0aa'), ISO


ITEMS = list(P.B)

if __name__ == '__main__':
    import items3d as I
    from PIL import Image, ImageDraw
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        cols, S_ = 8, 100
        W = Image.new('RGBA', (cols * S_, ((len(ITEMS) + cols - 1) // cols) * (S_ + 12)), (58, 58, 64, 255)); d = ImageDraw.Draw(W)
        for i, iid in enumerate(ITEMS):
            parts, mm, view = P.B[iid]('#ffffff', '#ffffff', 0.15)
            img = I.render(parts, mm, **view)
            im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((96, 96), Image.NEAREST)
            x, y = (i % cols) * S_, (i // cols) * (S_ + 12)
            W.alpha_composite(im, (x + 2, y + 2)); d.text((x + 2, y + 98), iid[:16], fill=(255, 255, 255, 255))
        W.save(sys.argv[2])
        print('preview', len(ITEMS), sys.argv[2])
    else:
        only = sys.argv[1:]
        for iid in ITEMS:
            if only and iid not in only:
                continue
            P13.save_strip(P.frames(iid), os.path.join(P13.TEX, iid + '.png'))
        print('rendered', len(only) or len(ITEMS), 'items')
