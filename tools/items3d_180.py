"""
1.8.0 item art, Recreating Home: the forty-two materials (helpers from items3d_171.py). Heaven's colours: white marble,
gold, pale blue, rose; every one glows a little.

Run:  python3 tools/items3d_180.py [ids...]     (writes the textures)
      python3 tools/items3d_180.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
import items3d_171 as P
from render3d import Part, Material, sd_sphere, sd_box, sd_torus, sd_cyl, sd_capsule, sd_gem, sd_octa, sd_hexprism, rotx, roty, rotz
from items3d_new import sd_ellipsoid, em_all, shade_noise

TAU = math.tau
P.B.clear()
P.MOTIONS.clear()
ISO = dict(view_pitch=0.45, view_yaw=0.6, zoom=1.1)
FRONT = dict(view_pitch=0.12, view_yaw=0.0, zoom=1.1)


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


# ---------------------------------------------------------------- the elements
@reg('remembered_soil')
def _(c1, c2, t):
    k = 0.02 * math.sin(TAU * t)
    return [E((0.6, 0.3, 0.5), 0, (0, -0.25, 0)), C((0, 0, 0), (0, 0.3 + k, 0), 0.04, 1), S(0.1, 1, (0.1, 0.38 + k, 0)), S(0.08, 1, (-0.1, 0.3 + k, 0))], mats('#8a6a48', '#c8f0a0'), ISO


@reg('living_water', (('bob',), ('glint',)))
def _(c1, c2, t):
    k = 0.03 * math.sin(TAU * t)
    return [S(0.38, 0, (0, -0.15, 0)), E((0.2 - k, 0.42, 0.2 - k), 0, (0, 0.2, 0)), S(0.1, 1, (-0.12, -0.1, -0.3))], mats('#58a8f8', '#e8f8ff'), FRONT


@reg('first_breath', (('spin',), ('glint',)))
def _(c1, c2, t):
    return [T(0.58, 0.06, 0, rot=rotx(0.4)), T(0.4, 0.06, 1, rot=rotx(-0.5) @ rotz(0.6)), T(0.22, 0.06, 0, rot=rotz(1.1)), S(0.09, 1)], mats('#d8f0ff', '#ffffff'), ISO


@reg('first_light', (('pulse',), ('glint',)))
def _(c1, c2, t):
    parts = [S(0.26, 1)]
    for i in range(8):
        a = i * TAU / 8 + t * TAU / 8
        r0, r1 = 0.36, (0.62 if i % 2 == 0 else 0.5)
        parts.append(C((math.cos(a) * r0, math.sin(a) * r0, 0), (math.cos(a) * r1, math.sin(a) * r1, 0), 0.035, 3))
    return parts, mats('#ffffff', '#fffbe0', '#ffffff', '#ffd870'), FRONT


@reg('hearthfire', (('rock',), ()))
def _(c1, c2, t):
    k = 0.05 * math.sin(TAU * t)
    return [Bx((0.5, 0.08, 0.14), 0, (0, -0.5, 0), rotz(0.12)), Bx((0.5, 0.08, 0.14), 0, (0, -0.5, 0.05), roty(1.2) @ rotz(-0.1)),
            E((0.3, 0.5 + k, 0.3), 1, (0, -0.02, 0)), E((0.15, 0.3 + k, 0.15), 3, (0.03, -0.12, -0.2))], mats('#6a4428', '#ff8a30', '#ffffff', '#ffe890'), FRONT


@reg('seraphic_alloy')
def _(c1, c2, t):
    R = rotx(-0.45) @ roty(0.55)
    parts = [Bx((0.5, 0.15, 0.25), 0, rot=R, r=0.05)]
    for s in (-1, 1):
        for j in range(3):
            parts.append(C((s * 0.3, 0.1, 0), (s * (0.62 - j * 0.06), 0.3 + j * 0.14 + 0.03 * math.sin(TAU * t + j), 0.1), 0.035, 1))
    return parts, mats('#f0dca0', '#ffffff'), FRONT


@reg('choir_crystal', (('spin',), ('glint',)))
def _(c1, c2, t):
    return [Part(lambda p: sd_gem(p, 0.26, 0.62), 0), Part(lambda p: sd_gem(p, 0.12, 0.3), 1, pos=(0.3, -0.3, 0), rot=rotz(-0.5)),
            Part(lambda p: sd_gem(p, 0.1, 0.26), 1, pos=(-0.3, -0.32, 0), rot=rotz(0.5))], mats('#a8d8ff', '#e8f8ff'), ISO


@reg('woven_memory')
def _(c1, c2, t):
    R = rotx(-0.5) @ roty(0.4)
    parts = [Bx((0.5, 0.5, 0.04), 0, rot=R)]
    for i in range(-2, 3):
        m = 1 if (i + int(t * 5)) % 5 == 0 else 2
        parts.append(Bx((0.5, 0.03, 0.02), m, tuple(R @ np.array([0, i * 0.2, -0.06])), R))
        parts.append(Bx((0.03, 0.5, 0.02), 2, tuple(R @ np.array([i * 0.2, 0, -0.05])), R))
    return parts, mats('#e8dcc8', '#ffffff', '#c89858'), FRONT


@reg('still_stone')
def _(c1, c2, t):
    return [E((0.5, 0.2, 0.4), 0, (0, -0.4, 0)), E((0.36, 0.17, 0.3), 2, (0.04, -0.07, 0)), E((0.24, 0.14, 0.2), 0, (-0.03, 0.2, 0)), S(0.08, 1, (0, 0.42, 0))], mats('#c8c8d0', '#ffffff', '#a8a8b8'), ISO


@reg('morning_dew')
def _(c1, c2, t):
    k = 0.02 * math.sin(TAU * t)
    return [E((0.62, 0.05, 0.2), 0, rot=rotz(0.5)), S(0.13 + k, 1, (-0.25, -0.02, -0.12)), S(0.1, 1, (0.08, 0.15, -0.12)), S(0.07 - k, 1, (0.33, 0.3, -0.1))], mats('#78c070', '#e0f8ff'), FRONT


@reg('evening_ember', (('pulse',), ()))
def _(c1, c2, t):
    k = 0.5 + 0.5 * math.sin(TAU * t)
    return [Part(lambda p: sd_octa(p, 0.55), 0), Part(lambda p: sd_octa(p, 0.3 + 0.08 * k), 1, pos=(0, 0, -0.3)), S(0.06, 1, (0.4, 0.45, 0)), S(0.04, 1, (-0.42, 0.3, 0))], mats('#4a2c30', '#ff7038'), FRONT


@reg('quiet_snow', (('spin',), ('glint',)))
def _(c1, c2, t):
    parts = [S(0.1, 1)]
    for i in range(6):
        a = i * TAU / 6
        d = np.array([math.cos(a), math.sin(a), 0])
        parts.append(C(tuple(d * 0.1), tuple(d * 0.62), 0.04, 0))
        for s in (-1, 1):
            b = a + s * 0.9
            parts.append(C(tuple(d * 0.4), tuple(d * 0.4 + np.array([math.cos(b), math.sin(b), 0]) * 0.16), 0.03, 0))
    return parts, mats('#f0f8ff', '#ffffff'), FRONT


@reg('deep_root')
def _(c1, c2, t):
    parts = [C((0, 0.6, 0), (0, 0.1, 0), 0.1, 0)]
    for i, (x, y) in enumerate([(-0.5, -0.5), (-0.2, -0.62), (0.15, -0.6), (0.5, -0.45)]):
        parts.append(C((0, 0.1, 0), (x, y, 0), 0.06, 0))
        parts.append(S(0.06 + 0.015 * math.sin(TAU * t + i), 1, (x, y, 0)))
    return parts, mats('#7a5838', '#ffe090'), FRONT


@reg('bright_seed', (('bob',), ('glint',)))
def _(c1, c2, t):
    return [E((0.3, 0.46, 0.24), 0, (0, -0.1, 0), rotz(0.25)), C((0.1, 0.3, 0), (0.2, 0.6, 0), 0.035, 1), S(0.09, 1, (0.24, 0.62, 0)), S(0.08, 1, (-0.05, -0.1, -0.24))], mats('#e0b050', '#f8ffd0'), FRONT


@reg('kind_shadow')
def _(c1, c2, t):
    return [S(0.55, 0), Part(lambda p: sd_sphere(p, 0.5), 0, pos=(0.3, 0.14, -0.4), sub=True), S(0.06, 1, (0.3, 0.2, 0)), S(0.04, 1, (0.45, -0.1, 0))], mats('#6068b0', '#e0e0ff'), FRONT


@reg('warm_rain')
def _(c1, c2, t):
    parts = [S(0.26, 0, (-0.25, 0.3, 0)), S(0.32, 0, (0.05, 0.38, 0)), S(0.24, 0, (0.34, 0.28, 0))]
    for i, x in enumerate((-0.3, 0.0, 0.3)):
        y = -0.1 - ((t + i / 3) % 1) * 0.5
        parts.append(E((0.05, 0.1, 0.05), 1, (x - 0.05, y, 0)))
    return parts, mats('#e8e8f0', '#ffc878'), FRONT


@reg('salt_of_tears', (('spin',), ('glint',)))
def _(c1, c2, t):
    R = rotx(0.6) @ roty(0.78)
    return [Bx((0.36, 0.36, 0.36), 0, rot=R), Bx((0.16, 0.16, 0.16), 1, (0.3, 0.34, -0.2), R), Bx((0.1, 0.1, 0.1), 0, (-0.4, -0.38, -0.1), R)], mats('#f4f4fa', '#c0e0ff'), FRONT


@reg('honey_of_days')
def _(c1, c2, t):
    R = rotx(1.2)
    parts = []
    for i, (x, y) in enumerate([(0, 0), (0.42, 0.24), (-0.42, 0.24), (0, -0.48), (0.42, -0.24), (-0.42, -0.24), (0, 0.48)]):
        lit = (i + int(t * 7)) % 7 == 0
        parts.append(Part(lambda p: sd_hexprism(p, 0.21, 0.08), 1 if lit or i == 0 else 0, pos=(x * 0.95, y * 0.95, 0), rot=R))
    return parts, mats('#d89028', '#ffd860'), FRONT


@reg('thread_of_fate', (('spin',), ()))
def _(c1, c2, t):
    parts = [Cy(0.12, 0.5, 0), Cy(0.3, 0.05, 0, (0, 0.5, 0)), Cy(0.3, 0.05, 0, (0, -0.5, 0))]
    for i in range(5):
        parts.append(T(0.2, 0.05, 1 if i % 2 == 0 else 3, (0, -0.32 + i * 0.16, 0)))
    return parts, mats('#c8a878', '#ff7090', '#ffffff', '#ffd870'), ISO


@reg('dust_of_stars', (('rock',), ('glint',)))
def _(c1, c2, t):
    rng = np.random.RandomState(42)
    parts = []
    for i in range(16):
        v = rng.uniform(-0.55, 0.55, 3)
        v[2] *= 0.4
        parts.append(S(0.035 + 0.05 * rng.rand() * (0.6 + 0.4 * math.sin(TAU * t + i)), 1 if i % 3 else 3, tuple(v)))
    return parts, mats('#ffffff', '#fff8d8', '#ffffff', '#b8d8ff'), FRONT


@reg('ash_of_worlds')
def _(c1, c2, t):
    return [S(0.36, 0), Part(lambda p: sd_sphere(p, 0.22), 0, pos=(0.26, 0.2, -0.24), sub=True), T(0.56, 0.04, 2, rot=rotx(1.25) @ rotz(0.3)),
            S(0.07, 1, (0.2, 0.16, -0.16)), S(0.04, 1, (0.5, 0.42, 0))], mats('#8a8890', '#ff9050', '#c0c0c8'), FRONT


@reg('word_unspoken')
def _(c1, c2, t):
    parts = [Bx((0.55, 0.36, 0.1), 0, (0, 0.1, 0), r=0.16), C((-0.2, -0.2, 0), (-0.4, -0.55, 0), 0.07, 0)]
    for i, x in enumerate((-0.26, 0.0, 0.26)):
        parts.append(S(0.07 + (0.03 if int(t * 3) == i else 0), 1, (x, 0.1, -0.14)))
    return parts, mats('#f0f0f8', '#a8b8e0'), FRONT


@reg('name_forgotten')
def _(c1, c2, t):
    R = rotz(0.12)
    return [Bx((0.4, 0.56, 0.07), 0, rot=R, r=0.05), Bx((0.26, 0.03, 0.02), 2, (0.02, 0.28, -0.09), R), Bx((0.26, 0.03, 0.02), 2, (0, 0.08, -0.09), R),
            Bx((0.18, 0.03, 0.02), 1, (-0.06, -0.14, -0.09), R), T(0.07, 0.03, 2, (0.05, 0.62, 0), rotx(1.57))], mats('#e0d0b0', '#ffffff', '#a89878'), FRONT


@reg('song_unsung', (('bob',), ('glint',)))
def _(c1, c2, t):
    return [E((0.17, 0.13, 0.12), 0, (-0.3, -0.4, 0), rotz(0.4)), E((0.17, 0.13, 0.12), 0, (0.3, -0.3, 0), rotz(0.4)), C((-0.16, -0.38, 0), (-0.16, 0.42, 0), 0.04, 0),
            C((0.44, -0.28, 0), (0.44, 0.52, 0), 0.04, 0), C((-0.16, 0.42, 0), (0.44, 0.52, 0), 0.07, 1)], mats('#d8c8ff', '#ffffff'), FRONT


# ---------------------------------------------------------------- the works
@reg('firmament_plate')
def _(c1, c2, t):
    R = rotx(-0.5) @ roty(0.4)
    parts = [Bx((0.56, 0.4, 0.06), 0, rot=R, r=0.04)]
    for i, (x, y) in enumerate([(-0.3, 0.15), (0.1, 0.22), (0.35, -0.1), (-0.1, -0.2), (0.2, 0.0)]):
        parts.append(S(0.045 + 0.02 * math.sin(TAU * t + i * 1.3), 1, tuple(R @ np.array([x, y, -0.08]))))
    return parts, mats('#5078c8', '#ffffff'), FRONT


@reg('foundation_stone')
def _(c1, c2, t):
    R = roty(0.6)
    return [Bx((0.5, 0.34, 0.4), 0, (0, -0.1, 0), R, 0.04), Bx((0.52, 0.05, 0.42), 1, (0, 0.02, 0), R), Bx((0.3, 0.1, 0.24), 2, (0, 0.34, 0), R)], mats('#e8e4dc', '#ffd870', '#d0c8b8'), dict(view_pitch=0.4, view_yaw=0.0, zoom=1.1)


@reg('garden_bed')
def _(c1, c2, t):
    parts = [Bx((0.6, 0.14, 0.3), 0, (0, -0.42, 0), r=0.05)]
    for i, (x, c) in enumerate([(-0.36, 1), (0.0, 3), (0.36, 1)]):
        h = 0.18 + 0.03 * math.sin(TAU * t + i * 2)
        parts.append(C((x, -0.3, 0), (x, h, 0), 0.03, 4))
        parts.append(S(0.13, c, (x, h + 0.1, 0)))
    return parts, mats('#7a5c40', '#ff98c0', '#ffffff', '#ffe070', '#70b868'), FRONT


@reg('wellspring')
def _(c1, c2, t):
    k = 0.03 * math.sin(TAU * t)
    return [T(0.46, 0.13, 0, (0, -0.2, 0)), E((0.36, 0.06, 0.36), 1, (0, -0.16, 0)), C((0, -0.1, 0), (0, 0.36 + k, 0), 0.05, 1), S(0.1, 1, (0, 0.44 + k, 0)),
            S(0.05, 1, (0.2, 0.2 - k, 0)), S(0.05, 1, (-0.2, 0.2 - k, 0))], mats('#d8d4cc', '#80d0ff'), ISO


@reg('lantern_of_days')
def _(c1, c2, t):
    k = 0.02 * math.sin(TAU * t)
    parts = [Cy(0.3, 0.05, 0, (0, -0.42, 0)), Cy(0.3, 0.05, 0, (0, 0.3, 0)), Cy(0.14, 0.06, 0, (0, 0.42, 0)), T(0.1, 0.03, 0, (0, 0.6, 0), rotx(1.57)), S(0.19 + k, 1, (0, -0.06, 0))]
    for i in range(4):
        a = i * TAU / 4 + 0.78
        parts.append(C((math.cos(a) * 0.27, -0.4, math.sin(a) * 0.27), (math.cos(a) * 0.27, 0.3, math.sin(a) * 0.27), 0.03, 0))
    return parts, mats('#c8a050', '#fff0b0'), ISO


@reg('bell_of_hours', (('rock',), ('glint',)))
def _(c1, c2, t):
    sw = 0.25 * math.sin(TAU * t)
    return [S(0.3, 0, (0, 0.12, 0)), Cy(0.3, 0.22, 0, (0, -0.1, 0)), T(0.36, 0.07, 0, (0, -0.32, 0)), S(0.08, 0, (0, 0.46, 0)), S(0.1, 1, (sw, -0.46, 0))], mats('#e0b040', '#fff4c0'), FRONT


@reg('loom_of_years')
def _(c1, c2, t):
    parts = [C((-0.5, -0.55, 0), (-0.5, 0.55, 0), 0.06, 0), C((0.5, -0.55, 0), (0.5, 0.55, 0), 0.06, 0), C((-0.5, 0.5, 0), (0.5, 0.5, 0), 0.06, 0), C((-0.5, -0.5, 0), (0.5, -0.5, 0), 0.06, 0)]
    for i in range(5):
        x = -0.32 + i * 0.16
        parts.append(C((x, -0.45, 0), (x, 0.45, 0), 0.02, 2))
    y = -0.3 + 0.6 * (0.5 + 0.5 * math.sin(TAU * t))
    parts.append(C((-0.4, y, -0.05), (0.4, y, -0.05), 0.035, 1))
    return parts, mats('#a07848', '#ffd870', '#f0e8d8'), FRONT


@reg('gate_lintel')
def _(c1, c2, t):
    return [Cy(0.11, 0.42, 0, (-0.4, -0.16, 0)), Cy(0.11, 0.42, 0, (0.4, -0.16, 0)), Bx((0.62, 0.1, 0.16), 0, (0, 0.36, 0)), Bx((0.2, 0.1, 0.18), 1, (0, 0.36, -0.02)),
            Bx((0.16, 0.05, 0.16), 2, (-0.4, -0.58, 0)), Bx((0.16, 0.05, 0.16), 2, (0.4, -0.58, 0))], mats('#f0ece4', '#ffd870', '#d0c8b8'), FRONT


@reg('threshold_step')
def _(c1, c2, t):
    R = roty(0.5)
    return [Bx((0.6, 0.1, 0.42), 0, (0, -0.42, 0), R), Bx((0.46, 0.1, 0.32), 2, (0, -0.2, 0.06), R), Bx((0.32, 0.1, 0.22), 0, (0, 0.02, 0.12), R),
            Bx((0.3, 0.02, 0.2), 1, (0, 0.14, 0.12), R)], mats('#ece8e0', '#ffe9a8', '#d8d0c0'), dict(view_pitch=0.45, view_yaw=0.0, zoom=1.15)


@reg('roof_of_heaven')
def _(c1, c2, t):
    return [Bx((0.42, 0.06, 0.3), 0, (-0.3, 0.1, 0), rotz(0.6)), Bx((0.42, 0.06, 0.3), 0, (0.3, 0.1, 0), rotz(-0.6)), Bx((0.6, 0.05, 0.3), 2, (0, -0.2, 0)),
            S(0.1 + 0.02 * math.sin(TAU * t), 1, (0, 0.44, 0)), Cy(0.05, 0.14, 2, (0, -0.02, -0.2))], mats('#6890d8', '#ffe9a8', '#f0ece4'), FRONT


@reg('table_of_welcome')
def _(c1, c2, t):
    parts = [Bx((0.6, 0.05, 0.36), 0, (0, 0.08, 0))]
    for x in (-0.48, 0.48):
        for z in (-0.26, 0.26):
            parts.append(C((x, 0.05, z), (x, -0.5, z), 0.05, 0))
    parts += [Cy(0.1, 0.07, 1, (-0.2, 0.2, 0)), E((0.16, 0.03, 0.16), 2, (0.24, 0.15, 0)), S(0.07, 3, (0.24, 0.22, 0))]
    return parts, mats('#b08858', '#ffd870', '#f4f0e8', '#ff9070'), ISO


@reg('bed_of_rest')
def _(c1, c2, t):
    R = roty(0.5)
    return [Bx((0.62, 0.1, 0.36), 0, (0, -0.2, 0), R, 0.06), Bx((0.06, 0.34, 0.36), 2, tuple(R @ np.array([-0.6, 0.02, 0])), R), Bx((0.16, 0.07, 0.24), 1, tuple(R @ np.array([-0.36, -0.04, 0])), R, 0.06),
            Bx((0.34, 0.05, 0.36), 3, tuple(R @ np.array([0.24, -0.08, 0])), R, 0.04)], mats('#f0ece4', '#ffffff', '#b08858', '#b8d0ff'), dict(view_pitch=0.45, view_yaw=0.0, zoom=1.15)


# ---------------------------------------------------------------- the pillars
def pillar(shaft, glow, top):
    parts = [Bx((0.3, 0.06, 0.3), 0, (0, -0.6, 0)), Cy(0.17, 0.42, 0, (0, -0.14, 0)), Bx((0.26, 0.05, 0.26), 0, (0, 0.3, 0))]
    for i in range(3):
        parts.append(T(0.175, 0.025, 1, (0, -0.42 + i * 0.28, 0)))
    return parts + top, mats(shaft, glow), ISO


@reg('pillar_of_knowing')
def _(c1, c2, t):          # an eye
    return pillar('#f0ece4', '#80c8ff', [E((0.22, 0.12, 0.1), 0, (0, 0.52, 0)), S(0.08 + 0.015 * math.sin(TAU * t), 1, (0, 0.52, -0.08))])


@reg('pillar_of_making')
def _(c1, c2, t):          # a hammer
    return pillar('#f0e0c0', '#ffb050', [C((0, 0.36, 0), (0, 0.66, 0), 0.035, 0), Bx((0.18, 0.07, 0.08), 1, (0, 0.66, 0), rotz(0.3 * math.sin(TAU * t)))])


@reg('pillar_of_keeping')
def _(c1, c2, t):          # a ring, closed
    return pillar('#e8e8f0', '#b0ffc0', [T(0.14, 0.045, 1, (0, 0.54, 0), rotx(1.57)), Bx((0.1, 0.08, 0.06), 0, (0, 0.42, 0))])


@reg('pillar_of_mercy')
def _(c1, c2, t):          # a heart
    k = 0.012 * math.sin(2 * TAU * t)
    return pillar('#f8e8e8', '#ff90b0', [S(0.11 + k, 1, (-0.08, 0.58, 0)), S(0.11 + k, 1, (0.08, 0.58, 0)), Part(lambda p: sd_octa(p, 0.2 + k), 1, pos=(0, 0.47, 0))])


@reg('pillar_of_return')
def _(c1, c2, t):          # an arch with a light going round
    a = TAU * t
    return pillar('#f0ece4', '#ffe070', [T(0.16, 0.035, 0, (0, 0.54, 0), rotx(1.57)), S(0.06, 1, (math.cos(a) * 0.16, 0.54 + math.sin(a) * 0.16, 0))])


@reg('keystone_of_return', (('pulse',), ('glint',)))
def _(c1, c2, t):
    k = 0.02 * math.sin(TAU * t)
    return [Bx((0.4, 0.44, 0.16), 0, (0, 0.05, 0), r=0.04), Bx((0.26, 0.2, 0.16), 0, (0, -0.42, 0), r=0.04), Bx((0.5, 0.08, 0.18), 2, (0, 0.5, 0)),
            Part(lambda p: sd_gem(p, 0.14 + k, 0.3), 1, pos=(0, 0.05, -0.16)), S(0.05, 1, (-0.26, 0.3, -0.17)), S(0.05, 1, (0.26, 0.3, -0.17))], mats('#f4f0e4', '#ffe070', '#e0c070'), FRONT


ITEMS = list(P.B)

if __name__ == '__main__':
    import items3d as I
    from PIL import Image, ImageDraw
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        cols, S_ = 9, 100
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
