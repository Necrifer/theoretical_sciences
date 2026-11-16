"""
1.14.0 item art, Patch D1 (the Sun and small bodies): sails, solar records, samples
(helpers from items3d_180.py).

Run:  python3 tools/items3d_1140.py [ids...]     (writes the textures)
      python3 tools/items3d_1140.py preview <png>
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




def record(paper, ink, glow, t, kind):
    """A data record: a plate with a chart on it."""
    R = rotx(-0.75)
    parts = [Part(lambda p: sd_box(p, (0.42, 0.025, 0.34), 0.015), 0, rot=R)]
    k = 0.5 + 0.5 * math.sin(TAU * t)
    if kind == 'disk':                                                   # the Sun's disc with spot pairs
        parts.append(Part(lambda p: sd_cyl(p, 0.26, 0.02, 0.005), 1, pos=tuple(R @ np.array([0, 0.03, 0])), rot=R))
        for x, z, c in ((-0.08, -0.05, 2), (0.0, -0.05, 3), (0.08, 0.08, 2), (0.15, 0.08, 3)):
            parts.append(S(0.04, c, tuple(R @ np.array([x, 0.05, z]))))
    elif kind == 'curve':                                                # a boundary pushed in
        for i in range(9):
            a = -1.1 + i * 0.275
            parts.append(S(0.03, 1, tuple(R @ np.array([0.28 * math.cos(a) - 0.1, 0.045, 0.28 * math.sin(a)]))))
        parts.append(S(0.06, 2, tuple(R @ np.array([-0.2, 0.05, 0]))))
    elif kind == 'belts':                                                # two belts round a planet
        parts.append(S(0.07, 2, tuple(R @ np.array([0, 0.05, 0]))))
        for rr in (0.16, 0.27):
            parts.append(Part(lambda p, rr=rr: sd_torus(p, rr, 0.022), 1, pos=tuple(R @ np.array([0, 0.03, 0])), rot=R))
    elif kind == 'dots':                                                 # an ephemeris: a dotted track
        for i in range(7):
            parts.append(S(0.025 + 0.01 * (i == 6) * k, 1, tuple(R @ np.array([-0.3 + i * 0.1, 0.045, -0.15 + 0.05 * i]))))
        for i in range(4):
            parts.append(Bx((0.3, 0.004, 0.006), 2, tuple(R @ np.array([0.0, 0.04, -0.22 + i * 0.12])), R, r=0.002))
    return parts, mats(paper, glow, ink, '#202024'), ISO


@reg('sunspot_magnetogram', (('rock',), ('glint',)))
def _(c1, c2, t):
    return record('#d8d4c8', '#202024', '#9a9a9a', t, 'disk')


@reg('magnetopause_record', (('rock',), ('glint',)))
def _(c1, c2, t):
    return record('#d8d4c8', '#3a68c8', '#60d0ff', t, 'curve')


@reg('trapped_proton_flux', (('rock',), ('glint',)))
def _(c1, c2, t):
    return record('#d8d4c8', '#3a68c8', '#ff7050', t, 'belts')


@reg('asteroid_ephemeris', (('rock',), ('glint',)))
def _(c1, c2, t):
    return record('#e8e4d8', '#9aa0aa', '#ffd060', t, 'dots')


@reg('laser_sail', (('spin',), ('glint',)))
def _(c1, c2, t):
    R = rotx(-0.6)
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Part(lambda p: np.maximum(sd_cyl(p, 0.48, 0.006, 0.002), -sd_cyl(p, 0.0, 0.02, 0.0)), 0, rot=R),
             Part(lambda p: sd_torus(p, 0.48, 0.012), 2, rot=R), S(0.05 + 0.02 * k, 1, tuple(R @ np.array([0, 0.03, 0])))]
    for a in range(6):
        ang = a * TAU / 6
        parts.append(Part(lambda p: sd_box(p, (0.24, 0.004, 0.004), 0.002), 2, pos=tuple(R @ np.array([0.24 * math.cos(ang), 0.01, 0.24 * math.sin(ang)])),
                          rot=R @ roty(-ang)))
    return parts, mats('#c8b0e8', '#ffb0f0', '#e0e0e8'), ISO


@reg('coronal_loop_plasma', (('bob',), ('pulse',)))
def _(c1, c2, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Cy(0.3, 0.05, 2, (0, -0.38, 0)), Cy(0.05, 0.4, 2, (-0.28, 0, 0)), Cy(0.05, 0.4, 2, (0.28, 0, 0))]
    for i in range(11):
        a = math.pi * i / 10
        parts.append(S(0.045 + 0.015 * k, 1, (0.18 * math.cos(a), -0.3 + 0.5 * math.sin(a), 0)))
    return parts, mats('#9aa0aa', '#ffb040', '#c0c4cc', '#40304a'), ISO


def rock(col, glow, seed, n=7, r=(0.12, 0.2), shine=0):
    rng = np.random.default_rng(seed)
    parts = [E((0.3, 0.24, 0.27), 0, (0, -0.05, 0))]
    for i in range(n):
        a = rng.uniform(0, TAU)
        b = rng.uniform(-0.6, 0.9)
        rr = rng.uniform(*r)
        parts.append(S(rr, 0, (0.26 * math.cos(a) * math.cos(b), 0.22 * math.sin(b) - 0.05, 0.24 * math.sin(a) * math.cos(b))))
    for i in range(shine):
        a = rng.uniform(0, TAU)
        parts.append(S(0.04, 1, (0.3 * math.cos(a), rng.uniform(-0.1, 0.15), 0.28 * math.sin(a))))
    return parts, mats(col, glow), ISO


@reg('carbonaceous_regolith')
def _(c1, c2, t):
    return rock('#26221f', '#8a7a60', 11, shine=2)


@reg('platinum_group_nugget')
def _(c1, c2, t):
    p, m, v = rock('#b8bcc4', '#f0f4ff', 12, n=5, r=(0.1, 0.16), shine=3)
    return p, [Material('#b8bcc4', '#ffffff', pattern=shade_noise(6, 0.1, 4), metal=0.95), m[1]], v


@reg('cometary_ice', (('rock',), ('glint',)))
def _(c1, c2, t):
    return rock('#c8d8e0', '#ffffff', 13, shine=3)


@reg('cometary_dust', (('bob',), ('glint',)))
def _(c1, c2, t):
    rng = np.random.default_rng(14)
    parts = [Bx((0.3, 0.06, 0.3), 2, (0, -0.25, 0), r=0.02), Bx((0.26, 0.2, 0.26), 3, (0, -0.02, 0), r=0.04)]      # aerogel in its tray
    for i in range(9):
        parts.append(S(rng.uniform(0.02, 0.04), 0 if i % 3 else 1, (rng.uniform(-0.2, 0.2), rng.uniform(-0.15, 0.15), rng.uniform(-0.2, 0.2))))
    return parts, [Material('#4a3a2a', '#ffffff', pattern=shade_noise(6, 0.1, 4), metal=0.2), Material('#ffd090', '#ffffff', emissive='#ffd090', pattern=em_all()),
                   Material('#9aa0aa', '#ffffff', metal=0.6), Material('#a8c8e8', '#ffffff', metal=0.1)], ISO


@reg('jovian_atmosphere_sample', (('bob',), ('glint',)))
def _(c1, c2, t):
    parts = [Cy(0.22, 0.34, 3, (0, 0, 0)), Cy(0.24, 0.05, 2, (0, 0.38, 0)), Cy(0.24, 0.05, 2, (0, -0.38, 0))]
    for i, y in enumerate((-0.2, -0.07, 0.06, 0.19)):
        parts.append(Cy(0.235, 0.045, 0 if i % 2 else 1, (0, y, 0)))
    return parts, mats('#d8a070', '#f0e0c0', '#9aa0aa', '#c0d0e0'), ISO


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
