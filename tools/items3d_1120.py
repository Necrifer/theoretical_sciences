"""
1.12.0 item art, Patch C1 (getting there): engine parts, Transfer Stages, the Sample Return Probe and its samples
(helpers from items3d_180.py).

Run:  python3 tools/items3d_1120.py [ids...]     (writes the textures)
      python3 tools/items3d_1120.py preview <png>
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


@reg('hydrolox_transfer_stage', (('rock',), ('glint',)))
def _(c1, c2, t):
    return stage('#e8e4dc', '#c87a3a', '#bfe4ff', t)


@reg('methalox_transfer_stage', (('rock',), ('glint',)))
def _(c1, c2, t):
    return stage('#c8ccd4', '#5a5e68', '#a8c8ff', t)


@reg('nuclear_transfer_stage', (('rock',), ('glint',)))
def _(c1, c2, t):
    return stage('#9aa8b4', '#e0c040', '#ff9a50', t)


@reg('ion_transfer_stage', (('rock',), ('glint',)))
def _(c1, c2, t):
    return stage('#d8dce4', '#8a90a0', '#78c8ff', t, 'grid')


@reg('antimatter_transfer_stage', (('rock',), ('glint',)))
def _(c1, c2, t):
    return stage('#34303c', '#b080ff', '#ff70f0', t, 'ring')


@reg('nuclear_thermal_core', (('rock',), ('pulse',)))
def _(c1, c2, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Cy(0.3, 0.32, 0, (0, 0.12, 0))]
    for i in range(6):
        a = i * TAU / 6
        parts.append(Cy(0.05, 0.34, 1, (0.17 * math.cos(a), 0.14, 0.17 * math.sin(a))))
    parts += nozzle(-0.24, 0.12, 0.3, 0.42, 2)
    parts.append(S(0.06 + 0.03 * k, 1, (0, -0.62, 0)))
    return parts, mats('#4a4e58', '#60e0ff', '#9aa0aa'), ISO


@reg('ion_thruster_grid', (('rock',), ('glint',)))
def _(c1, c2, t):
    R = rotx(-0.6)
    parts = [Part(lambda p: sd_cyl(p, 0.52, 0.05, 0.02), 0, rot=R), Part(lambda p: sd_torus(p, 0.52, 0.05), 2, rot=R)]
    for i in range(-2, 3):
        for j in range(-2, 3):
            if i * i + j * j <= 5:
                parts.append(S(0.05, 1, tuple(R @ np.array([i * 0.17, 0.06, j * 0.17]))))
    return parts, mats('#2a2c32', '#5aa8ff', '#9aa0aa'), ISO


@reg('antimatter_injector', (('spin',), ('glint',)))
def _(c1, c2, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    return ([Cy(0.14, 0.5, 0), T(0.32, 0.07, 2, (0, 0.25, 0)), T(0.32, 0.07, 2, (0, -0.05, 0)), T(0.36, 0.05, 2, (0, -0.35, 0)),
             S(0.08 + 0.04 * k, 1, (0, 0.05, 0)), S(0.05, 3, (0, -0.55, 0))], mats('#3a3640', '#ff60e0', '#c0b070', '#a0e0ff'), ISO)


@reg('sample_return_probe', (('rock',), ('glint',)))
def _(c1, c2, t):
    return ([Part(lambda p: sd_cyl(p, 0.36, 0.1, 0.03), 2, pos=(0, -0.18, 0)), E((0.3, 0.22, 0.3), 0, (0, 0.02, 0)),
             C((0, 0.1, 0), (0.42, 0.5, 0), 0.04, 0), S(0.06, 1, (0.44, 0.52, 0)),
             Bx((0.3, 0.02, 0.14), 3, (-0.5, 0.05, 0), r=0.01), Bx((0.3, 0.02, 0.14), 3, (0.5, 0.0, 0.05), r=0.01)],
            mats('#d8d8dc', '#ffd060', '#7a4a2a', '#3a68c8'), ISO)


def regolith(col, glow, seed):
    rng = np.random.default_rng(seed)
    parts = [Bx((0.32, 0.36, 0.32), 2, (0, -0.04, 0), r=0.12)]                                  # the capsule's sample jar
    for i in range(6):
        a = rng.uniform(0, TAU)
        parts.append(S(rng.uniform(0.08, 0.14), 0, (math.cos(a) * 0.18, 0.36 + rng.uniform(-0.03, 0.06), math.sin(a) * 0.18)))
    parts.append(S(0.05, 1, (0.1, 0.45, -0.1)))
    return parts, mats(col, glow, '#c8dce8'), ISO


@reg('asteroid_regolith')
def _(c1, c2, t):
    return regolith('#3a3632', '#c8b890', 1)


@reg('lunar_regolith')
def _(c1, c2, t):
    return regolith('#8c8c90', '#e8e8f0', 2)


@reg('martian_regolith')
def _(c1, c2, t):
    return regolith('#b0522a', '#f0a070', 3)


@reg('mercurian_regolith')
def _(c1, c2, t):
    return regolith('#3e3c3a', '#9a9aa0', 4)


@reg('venusian_aerosol', (('bob',), ('glint',)))
def _(c1, c2, t):
    k = 0.03 * math.sin(TAU * t)
    return ([Bx((0.3, 0.42, 0.3), 2, (0, -0.05, 0), r=0.12), E((0.22, 0.2 + k, 0.22), 0, (0, -0.15, 0)), S(0.07, 1, (0.08, 0.1 + k, -0.15)),
             S(0.05, 1, (-0.1, -0.02, -0.15)), Cy(0.2, 0.06, 0, (0, 0.42, 0))], mats('#e8d870', '#fff8c0', '#c8dce8'), ISO)


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
