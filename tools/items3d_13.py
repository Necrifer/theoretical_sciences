"""
1.3.0 art pass: 3D models for the items of Fluids and Flight, the Living Planet, the Green Kingdom and Deep Time.

They had been drawn as flat, still sprites; every other item of the mod is a small 3D model, lit and animated (see
items3d.py, items3d_new.py and anim.py). Each builder takes (c1, c2, t) and returns (parts, materials, view); MOTIONS
gives each its animation (anything not listed rocks gently with a glint).

Run:  python3 tools/items3d_13.py            (writes the textures)
      python3 tools/items3d_13.py preview    (a contact sheet in the scratch folder only)
"""
import math, os, sys, json
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from render3d import Part, Material, sd_sphere, sd_box, sd_torus, sd_cyl, sd_capsule, sd_gem, rotx, roty, rotz
from items3d_new import V, sd_ellipsoid, smin, sd_cone_y, tube, capsule_chain, em_all, em_bands, shade_noise, em_spots, glass
import items3d as I
import anim

TAU = math.tau
B = {}
MOTIONS = {}


def reg(iid, motions=(('rock',), ('glint',))):
    def deco(fn):
        B[iid] = fn
        MOTIONS[iid] = motions
        return fn
    return deco


def stripes(freq, axis=1, width=0.5, amt=0.25, phase=0.0):
    return lambda p, n: (np.where(np.sin(p[..., axis] * freq + phase) > 1 - width * 2, -amt, 0.0), np.zeros(p.shape[:-1]))


def weave(freq=18, amt=0.18):
    return lambda p, n: (np.where((np.sin(p[..., 0] * freq) > 0) ^ (np.sin(p[..., 2] * freq) > 0), -amt, amt * 0.4), np.zeros(p.shape[:-1]))


def mottle(scale=9, amt=0.25, seed=0):
    return lambda p, n: (np.sin(p[..., 0] * scale + seed) * np.sin(p[..., 1] * scale * 1.37 + seed * 2) * np.sin(p[..., 2] * scale * 0.83 + 1) * amt,
                         np.zeros(p.shape[:-1]))


def label_tag(k, pos=(0.38, 0.28, 0.18)):
    """A museum tag on a string: every dated sample carries one."""
    return [Part(lambda p: sd_box(p, (0.17, 0.11, 0.015), 0.01), k, pos=pos, rot=rotz(0.35) @ roty(-0.4)),
            Part(lambda p: sd_capsule(p, (0, 0, 0), (-0.22, -0.14, -0.05), 0.012), k, pos=(pos[0] - 0.12, pos[1] + 0.03, pos[2]))]


TAG = lambda: Material('#f2ead6', '#ffffff', pattern=stripes(40, 0, 0.15, 0.35), metal=0.1)


# ======================================================================= Fluids and Flight
@reg('aerofoil_composite')
def b_aerofoil(c1, c2, t):
    """A cambered wing section in carbon-nylon weave, a pale leading edge."""
    def foil(p):
        x, y = p[..., 0], p[..., 1] - 0.12 * (1 - (p[..., 0] / 0.75) ** 2)        # camber
        th = 0.16 * np.clip(1 - (x + 0.45) / 1.3, 0, 1) ** 0.8 * np.clip((x + 0.75) / 0.3, 0, 1) ** 0.5
        return np.maximum(np.maximum(np.abs(y) - th, np.abs(p[..., 2]) - 0.42), np.abs(x) - 0.78)
    mats = [Material('#3a3e48', '#9aa4b8', pattern=weave(26, 0.2), metal=0.45), Material('#e8ecf0', '#ffffff', metal=0.4)]
    parts = [Part(lambda p: foil(p) - 0.01, 0),
             Part(lambda p: np.maximum(foil(p) - 0.012, p[..., 0] + 0.6), 1)]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.95, zoom=1.1)


@reg('de_laval_nozzle', (('rock',), ('glint',)))
def b_nozzle(c1, c2, t):
    """Converging, then diverging: a copper throat, the bell flaring below, exhaust glowing in it."""
    def shell(p, inner):
        y = p[..., 1]
        r = np.where(y > 0.1, 0.16 + (y - 0.1) * 0.55, 0.16 + (0.1 - y) * 0.62)
        q = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        d = np.abs(q - r) - (0.03 if not inner else 0.0)
        return np.maximum(d * 0.85, np.abs(y + 0.05) - 0.62) if not inner else np.maximum((q - r + 0.035) * 0.85, np.abs(y + 0.05) - 0.6)
    flick = 0.75 + 0.25 * math.sin(TAU * t * 2)
    mats = [Material('#c87848', '#ffd0a0', metal=0.95), Material('#80c8ff', '#ffffff', emissive=tuple(np.array([0.5, 0.8, 1.0]) * flick), pattern=em_bands(14, t, 1, 0.35, 2), metal=0.3),
            Material('#585c66', '#c0c8d8', metal=0.9)]
    parts = [Part(lambda p: shell(p, False), 0), Part(lambda p: shell(p, True), 1),
             Part(lambda p: sd_torus(p, 0.23, 0.035), 2, pos=(0, 0.56, 0)),
             Part(lambda p: sd_torus(p, 0.16, 0.03), 2, pos=(0, 0.1, 0))]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6, zoom=1.0)


@reg('rocket_stage')
def b_stage(c1, c2, t):
    """A white propellant tank with roll-pattern bands, the engine bell beneath, an interstage ring above."""
    mats = [Material('#e8eaee', '#ffffff', pattern=stripes(9, 1, 0.18, 0.55), metal=0.35), Material('#40444c', '#a0a8b8', metal=0.9),
            Material('#c86838', '#ffc090', metal=0.9), Material('#ff9040', '#ffffff', emissive='#ff9040', pattern=em_bands(16, t, 1, 0.3, 2), metal=0.2)]
    parts = [Part(lambda p: sd_cyl(p, 0.27, 0.5, 0.05), 0, pos=(0, 0.08, 0)),
             Part(lambda p: sd_cyl(p, 0.29, 0.05, 0.01), 1, pos=(0, 0.6, 0)),
             Part(lambda p: sd_cyl(p, 0.29, 0.04, 0.01), 1, pos=(0, -0.42, 0)),
             Part(lambda p: np.maximum(sd_cone_y(p, 0.24, 0.1, 0.17), -sd_cone_y(p, 0.2, 0.06, 0.2)), 2, pos=(0, -0.62, 0)),
             Part(lambda p: sd_cyl(p, 0.18, 0.01), 3, pos=(0, -0.79, 0))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6, zoom=0.95)


@reg('microgravity_lab')
def b_mglab(c1, c2, t):
    """A sealed furnace canister: a window onto the crystal growing inside, handles, end caps."""
    mats = [Material('#d0d6e0', '#ffffff', metal=0.8), Material('#3a4050', '#8090a8', metal=0.9),
            Material('#8fd8ff', '#ffffff', emissive='#8fd8ff', pattern=em_all(), metal=0.3), glass()]
    parts = [Part(lambda p: sd_cyl(p, 0.36, 0.42, 0.06), 0, rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.38, 0.06, 0.02), 1, pos=(0.44, 0, 0), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.38, 0.06, 0.02), 1, pos=(-0.44, 0, 0), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.15, 0.05, 0.01), 3, pos=(0, 0.33, 0)),
             Part(lambda p: sd_gem(p, 0.07, 0.11), 2, pos=(0, 0.33, 0), rot=roty(TAU * t)),
             Part(lambda p: sd_torus(p, 0.1, 0.025), 1, pos=(0.22, 0.38, 0), rot=rotx(math.pi / 2)),
             Part(lambda p: sd_torus(p, 0.1, 0.025), 1, pos=(-0.22, 0.38, 0), rot=rotx(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.55, zoom=1.0)


@reg('microgravity_crystal', (('spin',), ('glint',)))
def b_mgcrystal(c1, c2, t):
    """Flawless: a clear, pale-blue crystal with a steady inner glow, turning in free fall."""
    mats = [Material('#b8e4ff', '#ffffff', emissive='#6ab8f0', pattern=em_bands(9, t, 1, 0.15), metal=0.95)]
    return [Part(lambda p: sd_gem(p, 0.3, 0.62, 0.4), 0)], mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.0)


@reg('station_module')
def b_module(c1, c2, t):
    """A pressurised can of hull plating: docking hatches at both ends, a row of lit windows."""
    blink = 1.0 if (t * 4) % 1 < 0.5 else 0.3
    mats = [Material('#d8dce4', '#ffffff', pattern=stripes(16, 0, 0.12, 0.2), metal=0.7), Material('#6a7080', '#c0c8d8', metal=0.9),
            Material('#ffe8a0', '#ffffff', emissive='#ffd870', pattern=em_all(), metal=0.3), Material('#ff4040', '#ffffff', emissive=(blink, 0.15, 0.1), pattern=em_all())]
    parts = [Part(lambda p: sd_cyl(p, 0.32, 0.55, 0.06), 0, rot=rotz(math.pi / 2)),
             Part(lambda p: sd_torus(p, 0.2, 0.05), 1, pos=(0.6, 0, 0), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_torus(p, 0.2, 0.05), 1, pos=(-0.6, 0, 0), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_sphere(p, 0.035), 3, pos=(0.0, 0.33, 0.0))]
    for x in (-0.3, -0.1, 0.1, 0.3):
        parts.append(Part(lambda p: sd_box(p, (0.05, 0.035, 0.02), 0.01), 2, pos=(x, 0.1, 0.31)))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.45, zoom=1.0)


def _capsule_cone(mats_base, extra=()):
    parts = [Part(lambda p: sd_cone_y(p, 0.42, 0.16, 0.32) - 0.02, 0, pos=(0, 0.02, 0)),
             Part(lambda p: sd_cyl(p, 0.44, 0.06, 0.04), 1, pos=(0, -0.33, 0)),                  # the heat shield
             Part(lambda p: sd_cyl(p, 0.14, 0.06, 0.02), 2, pos=(0, 0.38, 0)),
             Part(lambda p: sd_sphere(p, 0.075), 3, pos=(0.18, 0.05, 0.26))]                      # the window
    return parts + list(extra)


@reg('crew_capsule')
def b_capsule(c1, c2, t):
    """A blunt cone with a scorched ceramic heat shield and one window."""
    mats = [Material('#e6e8ec', '#ffffff', pattern=stripes(22, 1, 0.1, 0.18), metal=0.5), Material('#5a3a28', '#b07850', metal=0.3),
            Material('#7a808c', '#d0d8e0', metal=0.9), Material('#203048', '#80c0ff', metal=0.95)]
    return _capsule_cone(mats), mats, dict(view_pitch=0.35, view_yaw=0.7, zoom=1.0)


@reg('fuelled_crew_capsule', (('bob',), ('glint',)))
def b_capsule_fuelled(c1, c2, t):
    """Fuelled and waiting: the window lit, frost venting from the propellant line, a ready light blinking."""
    blink = 1.0 if (t * 2) % 1 < 0.5 else 0.25
    mats = [Material('#e6e8ec', '#ffffff', pattern=stripes(22, 1, 0.1, 0.18), metal=0.5), Material('#5a3a28', '#b07850', metal=0.3),
            Material('#7a808c', '#d0d8e0', metal=0.9), Material('#ffe8a0', '#ffffff', emissive='#ffd870', pattern=em_all()),
            Material('#40ff70', '#ffffff', emissive=(0.2, blink, 0.35), pattern=em_all()),
            Material('#e8f4ff', '#ffffff', emissive='#a8d0ff', pattern=em_spots(t, 0.6, 3, 14), metal=0.2)]
    vent = 0.06 + 0.03 * math.sin(TAU * t)
    extra = [Part(lambda p: sd_sphere(p, 0.04), 4, pos=(-0.12, 0.3, 0.22)),
             Part(lambda p: sd_sphere(p, vent + 0.05), 5, pos=(-0.42, -0.1, 0.1)),
             Part(lambda p: sd_sphere(p, vent), 5, pos=(-0.55, 0.0, 0.05))]
    return _capsule_cone(mats, extra), mats, dict(view_pitch=0.35, view_yaw=0.7, zoom=0.95)


@reg('vortex_filament', (('spin',), ('glint',)))
def b_vortex(c1, c2, t):
    """A thread of spinning air: a pale helix round a brighter core, light running along it."""
    mats = [Material('#d8ecff', '#ffffff', emissive='#a8d8ff', pattern=em_bands(16, t, 1, 0.25, 2), metal=0.3),
            Material('#ffffff', '#ffffff', emissive='#e0f4ff', pattern=em_all(), metal=0.2)]
    helix = lambda u: V(0.24 * math.cos(u * TAU * 2.5), -0.65 + 1.3 * u, 0.24 * math.sin(u * TAU * 2.5))
    parts = tube(helix, 36, 0.07, 0)
    parts.append(Part(lambda p: sd_cyl(p, 0.045, 0.6), 1))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0)


# ======================================================================= the Living Planet
@reg('pressure_hull_plate')
def b_hull_plate(c1, c2, t):
    """A curved plate cut from a sphere: ceramic face, steel back, aramid lacing round its edge."""
    R = 1.4
    def shell(p, r0, r1):
        q = p - V(0, -R, 0)
        rr = np.sqrt(np.sum(q * q, -1))
        return np.maximum(np.maximum(r0 - rr, rr - r1), np.maximum(np.abs(p[..., 0]) - 0.55, np.abs(p[..., 2]) - 0.55))
    mats = [Material('#e2e0da', '#ffffff', pattern=mottle(18, 0.08, 2), metal=0.3), Material('#5c626e', '#b8c0d0', metal=0.9),
            Material('#e0b030', '#fff0a0', pattern=stripes(30, 0, 0.3, 0.3), metal=0.2)]
    parts = [Part(lambda p: shell(p, R + 0.02, R + 0.08) - 0.005, 0), Part(lambda p: shell(p, R - 0.06, R + 0.02), 1),
             Part(lambda p: np.maximum(shell(p, R - 0.04, R + 0.1), -np.maximum(np.abs(p[..., 0]) - 0.48, np.abs(p[..., 2]) - 0.48)), 2)]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6, zoom=1.05)


@reg('bathyscaphe', (('bob',), ('glint',)))
def b_bathyscaphe(c1, c2, t):
    """The way down: a long float of petrol-light buoyancy above, the steel sphere hung under it, its window lit."""
    lamp = 0.75 + 0.25 * math.sin(TAU * t)
    mats = [Material('#e8b830', '#fff0a0', pattern=stripes(24, 0, 0.08, 0.25), metal=0.5), Material('#7a8290', '#d0d8e4', metal=0.9),
            Material('#3a3e48', '#9098a8', metal=0.9), Material('#a8f0ff', '#ffffff', emissive=tuple(np.array([0.6, 0.95, 1.0]) * lamp), pattern=em_all())]
    parts = [Part(lambda p: sd_ellipsoid(p, (0.72, 0.2, 0.22)), 0, pos=(0, 0.28, 0)),
             Part(lambda p: sd_box(p, (0.1, 0.07, 0.06), 0.02), 0, pos=(0.05, 0.5, 0)),                    # the conning tower
             Part(lambda p: sd_sphere(p, 0.26), 1, pos=(0, -0.22, 0)),
             Part(lambda p: sd_torus(p, 0.27, 0.025), 2, pos=(0, -0.22, 0)),
             Part(lambda p: sd_capsule(p, (0.12, 0.14, 0), (0.08, 0.0, 0), 0.03), 2, pos=(0, -0.0, 0)),
             Part(lambda p: sd_capsule(p, (-0.12, 0.14, 0), (-0.08, 0.0, 0), 0.03), 2, pos=(0, -0.0, 0)),
             Part(lambda p: sd_sphere(p, 0.06), 3, pos=(0.1, -0.24, 0.22))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5, zoom=1.05)


@reg('anglerfish_lure', (('rock',), ()))
def b_lure(c1, c2, t):
    """The esca on the end of its rod: a bulb of glowing bacteria, pulsing, its filaments trailing."""
    pulse = 0.6 + 0.4 * math.sin(TAU * t)
    mats = [Material('#4a3e36', '#a08870', metal=0.2), Material('#a0ffe8', '#ffffff', emissive=tuple(np.array([0.55, 1.0, 0.88]) * pulse), pattern=em_all(), metal=0.2),
            Material('#80f0d0', '#ffffff', emissive=tuple(np.array([0.3, 0.8, 0.65]) * pulse), pattern=em_all())]
    rod = lambda u: V(-0.55 + 0.7 * u, -0.6 + 1.05 * math.sin(u * 1.6), 0)
    parts = tube(rod, 12, 0.045, 0)
    end = rod(1.0)
    parts.append(Part(lambda p: sd_sphere(p, 0.2), 1, pos=tuple(end + V(0.05, 0.08, 0))))
    for k in range(3):
        a = -0.6 + k * 0.5
        parts += capsule_chain([end + V(0.05, -0.08, 0), end + V(0.05 + 0.18 * math.cos(a), -0.35 + 0.05 * k, 0.12 * math.sin(a))], 0.018, 2)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.3, zoom=1.2)


@reg('nematocyst_cluster', (('rock',), ('glint',)))
def b_nematocysts(c1, c2, t):
    """Stinging capsules in a bunch, each with its barbed thread coiled inside, glowing faintly."""
    mats = [Material('#e8b0e0', '#ffffff', emissive='#a04890', pattern=em_bands(6, t, 1, 0.12), metal=0.85),
            Material('#ff70c8', '#ffffff', emissive='#ff60c0', pattern=em_all(), metal=0.3)]
    rng = np.random.default_rng(7)
    parts = []
    for k in range(7):
        pos = V(*rng.uniform(-0.35, 0.35, 3)) * V(1, 0.9, 0.6)
        rot = roty(rng.uniform(0, TAU)) @ rotz(rng.uniform(-0.6, 0.6))
        parts.append(Part(lambda p: sd_ellipsoid(p, (0.14, 0.22, 0.14)), 0, pos=tuple(pos), rot=rot))
        parts.append(Part(lambda p: sd_torus(p, 0.06, 0.018), 1, pos=tuple(pos), rot=rot @ rotx(0.3)))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6, zoom=1.35)


@reg('fulgurite')
def b_fulgurite(c1, c2, t):
    """Lightning in sand: a crooked, branching tube, rough and sandy outside, glassy within."""
    mats = [Material('#c8b090', '#f0e0c0', pattern=mottle(30, 0.3, 4), metal=0.15), Material('#e8f4ff', '#ffffff', emissive='#b8d8ff', pattern=em_bands(18, t, 1, 0.1, 2), metal=0.95)]
    main = lambda u: V(0.08 * math.sin(u * 9), -0.7 + 1.4 * u, 0.06 * math.cos(u * 7))
    branch = lambda u: V(0.08 * math.sin(4.5) + 0.32 * u, 0.0 + 0.3 * u, 0.06 * math.cos(3.5) + 0.05 * u)
    parts = tube(main, 16, 0.09, 0) + tube(branch, 6, 0.06, 0)
    parts.append(Part(lambda p: sd_cyl(p, 0.045, 0.02), 1, pos=tuple(main(1.0))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.7, zoom=1.15)


# ======================================================================= the Green Kingdom
@reg('xylem_fibre')
def b_xylem(c1, c2, t):
    """A bundle of hollow, lignified tubes, tied round the middle."""
    mats = [Material('#c8b078', '#f0e0b0', pattern=stripes(40, 1, 0.2, 0.15), metal=0.15), Material('#3a2c1c', '#806040', metal=0.1),
            Material('#6a9a3a', '#c0e080', metal=0.2)]
    parts = []
    for i in range(7):
        a = i / 6 * TAU
        r = 0 if i == 6 else 0.13
        pos = (r * math.cos(a), 0, r * math.sin(a))
        parts.append(Part(lambda p: sd_cyl(p, 0.065, 0.6, 0.01), 0, pos=pos, rot=rotz(0.5)))
        parts.append(Part(lambda p: sd_cyl(p, 0.035, 0.61), 1, pos=pos, rot=rotz(0.5), sub=False))
    parts.append(Part(lambda p: sd_cyl(p, 0.235, 0.05, 0.01), 2, rot=rotz(0.5)))
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6, zoom=1.05)


@reg('engineered_seed', (('rock',), ('glint',)))
def b_seed(c1, c2, t):
    """A plump grain marked down its furrow with the line it was bred for, the mark glowing faintly."""
    mats = [Material('#d8b860', '#fff0b0', pattern=mottle(14, 0.12, 1), metal=0.3),
            Material('#80ffb0', '#ffffff', emissive='#50e080', pattern=em_bands(12, t, 1, 0.3), metal=0.4), Material('#5a9a30', '#b0e070', metal=0.2)]
    parts = []
    for k, (x, y, rz) in enumerate(((-0.22, -0.15, 0.4), (0.2, -0.2, -0.3), (0.0, 0.18, 0.1))):
        parts.append(Part(lambda p: sd_ellipsoid(p, (0.16, 0.27, 0.14)), 0, pos=(x, y, 0), rot=rotz(rz)))
        parts.append(Part(lambda p: sd_box(p, (0.02, 0.2, 0.15)), 1, pos=(x, y, 0.02), rot=rotz(rz)))
    parts.append(Part(lambda p: sd_capsule(p, (0, 0, 0), (0.06, 0.22, 0), 0.025), 2, pos=(0.0, 0.42, 0)))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.4, zoom=1.35)


@reg('biochar')
def b_biochar(c1, c2, t):
    """Wood charred without air: black lumps still showing the grain, riddled with pores."""
    mats = [Material('#2a2624', '#787068', pattern=stripes(34, 1, 0.25, 0.25), metal=0.55, keep_dark=True)]
    parts = [Part(lambda p: sd_box(p, (0.3, 0.38, 0.22), 0.06), 0, pos=(-0.15, 0.05, 0), rot=rotz(0.25)),
             Part(lambda p: sd_box(p, (0.22, 0.25, 0.2), 0.05), 0, pos=(0.27, -0.2, 0.05), rot=rotz(-0.5) @ roty(0.4))]
    rng = np.random.default_rng(3)
    for k in range(9):
        parts.append(Part(lambda p: sd_sphere(p, 0.045), 0, pos=tuple(rng.uniform(-0.4, 0.4, 3) * V(1, 1, 0.5) + V(0, 0, 0.2)), sub=True))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.0)


def _mound(cols, seed, n=14, r=0.5, h=0.32):
    rng = np.random.default_rng(seed)
    parts = []
    for k in range(n):
        a = rng.uniform(0, TAU); rr = rng.uniform(0, r)
        parts.append(Part(lambda p, s=rng.uniform(0.11, 0.17): sd_sphere(p, s), k % cols,
                          pos=(rr * math.cos(a), -0.3 + rng.uniform(0, h) * (1 - rr / r), rr * math.sin(a))))
    return parts


@reg('humus')
def b_humus(c1, c2, t):
    """Leaves rotted past recognition: a dark, crumbly heap with a last scrap of leaf in it."""
    mats = [Material('#5a3c22', '#9a7048', pattern=mottle(20, 0.25, 5), metal=0.15), Material('#6a4a2c', '#a87a50', metal=0.15),
            Material('#8a6a20', '#d0b050', metal=0.2)]
    parts = _mound(2, 11, 16) + [Part(lambda p: sd_ellipsoid(p, (0.16, 0.02, 0.09)), 2, pos=(0.12, 0.04, 0.1), rot=roty(0.6) @ rotz(0.3))]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.6, zoom=1.35)


@reg('living_soil', (('rock',), ('glint',)))
def b_living_soil(c1, c2, t):
    """Humus, char and fungi together: a rich heap threaded with pale glowing hyphae, a seedling coming up out of it."""
    mats = [Material('#5e4228', '#a07850', pattern=mottle(20, 0.2, 6), metal=0.15), Material('#3a3430', '#807870', metal=0.4),
            Material('#f0f0e0', '#ffffff', emissive='#c8e8c0', pattern=em_bands(10, t, 0, 0.2), metal=0.2), Material('#58b030', '#c0f080', metal=0.25)]
    parts = _mound(2, 21, 16)
    for k in range(4):
        a = k * 1.6
        parts += capsule_chain([V(0.4 * math.cos(a), -0.2, 0.3 * math.sin(a)), V(0.15 * math.cos(a + 0.8), -0.05, 0.15 * math.sin(a + 0.8)), V(0, 0.02, 0)], 0.018, 2)
    grow = 0.08 * math.sin(TAU * t)
    parts += capsule_chain([V(0, -0.05, 0.05), V(0.02, 0.3, 0.05)], 0.03, 3)
    parts.append(Part(lambda p: sd_ellipsoid(p, (0.16, 0.025, 0.08)), 3, pos=(0.13, 0.33, 0.05), rot=rotz(0.4 + grow)))
    parts.append(Part(lambda p: sd_ellipsoid(p, (0.14, 0.025, 0.07)), 3, pos=(-0.1, 0.36, 0.05), rot=rotz(-0.5 - grow)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6, zoom=1.3)


@reg('mycorrhizal_inoculant', (('rock',), ('glint',)))
def b_inoculant(c1, c2, t):
    """A dish of fungal culture: a white web of hyphae spreading out from a root, glowing where it trades."""
    mats = [Material('#c8d8e0', '#ffffff', metal=0.9), Material('#f4f0e8', '#ffffff', emissive='#d8f0c8', pattern=em_bands(12, t, 0, 0.2), metal=0.2),
            Material('#a07848', '#e0c090', metal=0.2), Material('#d8c89c', '#f8ecc8', pattern=mottle(18, 0.12, 2), metal=0.2)]
    parts = [Part(lambda p: np.maximum(sd_cyl(p, 0.62, 0.08, 0.02), -sd_cyl(p - V(0, 0.04, 0), 0.57, 0.08)), 0),
             Part(lambda p: sd_cyl(p, 0.57, 0.03), 3, pos=(0, -0.02, 0))]
    root = lambda u: V(-0.4 + 0.8 * u, 0.04, 0.12 * math.sin(u * 5))
    parts += tube(root, 10, 0.05, 2)
    rng = np.random.default_rng(5)
    for k in range(14):
        u = rng.uniform(0.1, 0.9); a = rng.uniform(0, TAU); r = rng.uniform(0.18, 0.48)
        a0 = root(u)
        mid = a0 + V(0.5 * r * math.cos(a) + rng.uniform(-0.05, 0.05), 0.02, 0.5 * r * math.sin(a))
        parts += capsule_chain([a0, mid, a0 + V(r * math.cos(a), 0.01, r * math.sin(a))], 0.016, 1)
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5, zoom=1.05)


@reg('field_ration')
def b_ration(c1, c2, t):
    """A dense sourdough loaf, its crust scored and dark, wrapped round the middle with a printed paper band."""
    score = lambda p, n: (np.where((np.sin((p[..., 0] + p[..., 2] * 0.6) * 12) > 0.85) & (p[..., 1] > 0.05), 0.35, 0.0), np.zeros(p.shape[:-1]))
    mats = [Material('#9a5a28', '#e0a060', pattern=score, metal=0.35), Material('#f0e6cc', '#ffffff', pattern=stripes(50, 0, 0.12, 0.3), metal=0.15)]
    loaf = lambda p: smin(sd_ellipsoid(p, (0.62, 0.3, 0.38)), sd_box(p - V(0, -0.12, 0), (0.55, 0.12, 0.32), 0.1), 0.08)
    parts = [Part(loaf, 0), Part(lambda p: np.maximum(loaf(p) - 0.025, np.abs(p[..., 0]) - 0.12), 1)]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.55, zoom=1.05)


# ======================================================================= Deep Time
@reg('fossil_fragment')
def b_fossil(c1, c2, t):
    """A piece of bone gone to stone, still half in its limestone."""
    mats = [Material('#b8b0a0', '#e8e0d0', pattern=mottle(16, 0.2, 2), metal=0.15), Material('#d8c8a8', '#fff4dc', pattern=stripes(28, 0, 0.2, 0.2), metal=0.25)]
    parts = [Part(lambda p: sd_box(p, (0.45, 0.25, 0.32), 0.12), 0, pos=(0, -0.15, 0), rot=rotz(-0.15)),
             Part(lambda p: sd_capsule(p, (-0.4, 0, 0), (0.35, 0.05, 0), 0.1), 1, pos=(0, 0.1, 0.05)),
             Part(lambda p: sd_sphere(p, 0.15), 1, pos=(0.42, 0.17, 0.05)),
             Part(lambda p: sd_sphere(p, 0.13), 1, pos=(0.38, 0.0, 0.08))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5, zoom=1.0)


def _amber_mats(t, glow=0.35):
    return [Material('#e08a18', '#ffd070', emissive=tuple(np.array([0.85, 0.45, 0.08]) * glow), pattern=em_bands(8, t, 1, 0.12), metal=0.9),
            Material('#2a1a10', '#604030', metal=0.3)]


def _insect(k, pos=(0, 0, 0), s=1.0):
    return [Part(lambda p: sd_capsule(p, (-0.1 * s, 0, 0), (0.1 * s, 0, 0), 0.03 * s), k, pos=pos),
            Part(lambda p: sd_ellipsoid(p, (0.08 * s, 0.01, 0.05 * s)), k, pos=(pos[0] - 0.02, pos[1] + 0.03, pos[2] + 0.06), rot=roty(0.4)),
            Part(lambda p: sd_ellipsoid(p, (0.08 * s, 0.01, 0.05 * s)), k, pos=(pos[0] - 0.02, pos[1] + 0.03, pos[2] - 0.06), rot=roty(-0.4))]


@reg('raw_amber')
def b_raw_amber(c1, c2, t):
    """Rough nuggets of fossil resin, light glowing through them, something small with wings caught inside."""
    mats = _amber_mats(t)
    parts = [Part(lambda p: sd_ellipsoid(p, (0.42, 0.32, 0.3)) + 0.02 * np.sin(p[..., 0] * 18) * np.sin(p[..., 1] * 15), 0, pos=(-0.05, 0.05, 0)),
             Part(lambda p: sd_ellipsoid(p, (0.22, 0.18, 0.18)), 0, pos=(0.38, -0.3, 0.05))] + _insect(1, (-0.05, 0.05, 0.12))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6, zoom=1.25)


@reg('stromatolite_fragment')
def b_stromatolite(c1, c2, t):
    """A domed slice of the oldest rock: layer on layer of bacterial mat, laid down three and a half billion years ago."""
    mats = [Material('#8a8a70', '#d0d0b0', pattern=stripes(26, 1, 0.35, 0.35), metal=0.15), Material('#5a5a48', '#9a9a80', metal=0.1)]
    dome = lambda p: np.maximum(sd_ellipsoid(p - V(0, -0.35, 0), (0.5, 0.65, 0.35)), -p[..., 1] - 0.35)
    return [Part(dome, 0), Part(lambda p: sd_box(p, (0.52, 0.05, 0.36), 0.03), 1, pos=(0, -0.38, 0))], mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0)


@reg('dated_bone')
def b_dated_bone(c1, c2, t):
    """An Ice Age bone, its tag tied on: dated to thirty thousand years."""
    mats = [Material('#e2d6b8', '#fff8e8', pattern=mottle(12, 0.12, 3), metal=0.25), TAG()]
    parts = [Part(lambda p: sd_capsule(p, (-0.45, -0.1, 0), (0.4, 0.15, 0), 0.09), 0),
             Part(lambda p: sd_sphere(p, 0.14), 0, pos=(-0.5, -0.02, 0.05)), Part(lambda p: sd_sphere(p, 0.13), 0, pos=(-0.48, -0.22, -0.04)),
             Part(lambda p: sd_sphere(p, 0.13), 0, pos=(0.46, 0.25, 0.05)), Part(lambda p: sd_sphere(p, 0.12), 0, pos=(0.47, 0.06, -0.04))]
    return parts + label_tag(1, (0.05, -0.28, 0.2)), mats, dict(view_pitch=0.45, view_yaw=0.45, zoom=1.15)


@reg('dated_amber')
def b_dated_amber(c1, c2, t):
    """Amber from the age of giant insects, polished and tagged: 310 million years."""
    mats = _amber_mats(t, 0.45) + [TAG()]
    parts = [Part(lambda p: sd_ellipsoid(p, (0.42, 0.3, 0.26)), 0, pos=(-0.05, 0.08, 0))] + _insect(1, (-0.05, 0.08, 0.1), 1.2)
    return parts + label_tag(2, (0.32, -0.28, 0.2)), mats, dict(view_pitch=0.4, view_yaw=0.55, zoom=1.25)


@reg('dated_stromatolite')
def b_dated_strom(c1, c2, t):
    """A stromatolite cut and tagged: 3.5 billion years, and the cells still in it."""
    mats = [Material('#8a8a70', '#d0d0b0', pattern=stripes(26, 1, 0.35, 0.35), metal=0.15), Material('#5a5a48', '#9a9a80', metal=0.1), TAG()]
    dome = lambda p: np.maximum(sd_ellipsoid(p - V(0, -0.3, 0), (0.42, 0.6, 0.3)), -p[..., 1] - 0.3)
    parts = [Part(dome, 0, pos=(-0.08, 0.05, 0)), Part(lambda p: sd_box(p, (0.45, 0.05, 0.32), 0.03), 1, pos=(-0.08, -0.27, 0))]
    return parts + label_tag(2, (0.35, -0.3, 0.22)), mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0)


def _helix(c_a, c_b, c_rung, t, glow):
    mats = [Material(c_a, '#ffffff', emissive=glow, pattern=em_bands(10, t, 1, 0.12, 1), metal=0.6), Material(c_b, '#ffffff', metal=0.6),
            Material(c_rung, '#ffffff', metal=0.4)]
    parts = []
    for s, k in ((0, 0), (math.pi, 1)):
        parts += tube(lambda u, s=s: V(0.26 * math.cos(u * TAU * 1.5 + s), -0.62 + 1.24 * u, 0.26 * math.sin(u * TAU * 1.5 + s)), 30, 0.06, k)
    for j in range(9):
        u = (j + 0.5) / 9
        a = u * TAU * 1.5
        parts.append(Part(lambda p, a=a: sd_capsule(p, (0.24 * math.cos(a), 0, 0.24 * math.sin(a)), (-0.24 * math.cos(a), 0, -0.24 * math.sin(a)), 0.03), 2,
                          pos=(0, -0.62 + 1.24 * u, 0)))
    return parts, mats


@reg('mammoth_genome', (('spin',), ('glint',)))
def b_mammoth_genome(c1, c2, t):
    """Mammuthus primigenius, read and pieced together: a double helix in ivory and wool-brown."""
    parts, mats = _helix('#e8dcc0', '#8a5a30', '#c8a070', t, '#a07040')
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.4, zoom=1.0)


@reg('meganeura_genome', (('spin',), ('glint',)))
def b_meganeura_genome(c1, c2, t):
    """A griffinfly's genome out of amber: a double helix in metallic green and amber."""
    parts, mats = _helix('#5ac080', '#e09028', '#3a7a98', t, '#40a070')
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.4, zoom=1.0)


@reg('mammoth_wool')
def b_wool(c1, c2, t):
    """A hank of the long outer coat, ginger-brown, curling at the ends over dense underwool."""
    mats = [Material('#7a4a26', '#c88a50', pattern=stripes(60, 0, 0.3, 0.2), metal=0.2), Material('#a87850', '#e0b888', metal=0.2)]
    parts = [Part(lambda p: sd_ellipsoid(p, (0.32, 0.18, 0.22)), 1, pos=(0, -0.25, 0))]
    for k in range(7):
        x0 = -0.3 + k * 0.1
        sway = 0.05 * math.sin(TAU * t + k)
        parts += tube(lambda u, x0=x0, k=k: V(x0 + 0.25 * u * (1 if k % 2 else -1) * 0.6 + sway * u, -0.2 + 0.85 * u, 0.06 * math.sin(k) + 0.12 * u * u), 8, 0.055 - 0.003 * k, 0)
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.15)


@reg('mammoth_ivory')
def b_ivory(c1, c2, t):
    """A spiralling tusk of dentine, its growth rings banding it."""
    mats = [Material('#eadfc4', '#fffaf0', pattern=stripes(40, 0, 0.18, 0.18), metal=0.45), Material('#b89a70', '#e0c8a0', metal=0.3)]
    arc = lambda u: V(-0.55 + 0.9 * u, -0.45 + 1.1 * math.sin(u * 1.9) * 0.75, 0.25 * math.sin(u * 3.2))
    parts = []
    for i in range(14):
        u0, u1 = i / 14, (i + 1) / 14
        r = 0.13 * (1 - 0.8 * u0) + 0.02
        parts.append(Part(lambda p, a=arc(u0), b=arc(u1), r=r: sd_capsule(p, a, b, r), 0))
    parts.append(Part(lambda p: sd_cyl(p, 0.14, 0.03, 0.01), 1, pos=tuple(arc(0)), rot=rotz(-1.0)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.45, zoom=1.1)


@reg('meganeura_wing')
def b_wing(c1, c2, t):
    """A wing of chitin film thirty-five centimetres long: clear, netted with veins, a dark pterostigma near the tip."""
    veins = lambda p, n: (np.where((np.abs(np.sin(p[..., 0] * 22)) < 0.18) | (np.abs(np.sin(p[..., 2] * 30 + p[..., 0] * 3)) < 0.14), -0.55, 0.0),
                          np.zeros(p.shape[:-1]))
    mats = [Material('#c8dce8', '#ffffff', pattern=veins, metal=0.95), Material('#2a2a30', '#707078', metal=0.6), Material('#5a3a20', '#a07850', metal=0.3)]
    wing = lambda p: np.maximum(sd_ellipsoid(p, (0.82, 0.02, 0.2)), p[..., 2] - 0.15 - 0.05 * p[..., 0])
    parts = [Part(wing, 0), Part(lambda p: sd_capsule(p, (-0.8, 0, 0.13), (0.78, 0, 0.1), 0.022), 1),
             Part(lambda p: sd_box(p, (0.07, 0.025, 0.04), 0.01), 1, pos=(0.6, 0, 0.1)), Part(lambda p: sd_sphere(p, 0.07), 2, pos=(-0.8, 0, 0.05))]
    return parts, mats, dict(view_pitch=0.9, view_yaw=0.35, zoom=1.05)


def _capsule_glass(inner, mats_extra, t):
    mats = [glass('#d8ecf4'), Material('#7a828e', '#d0d8e4', metal=0.9),
            Material('#a8f0ff', '#ffffff', emissive='#80d0ff', pattern=em_bands(10, t, 1, 0.2), metal=0.3)] + mats_extra
    window = lambda p: sd_box(p - V(0.3, 0.0, -0.3), (0.3, 0.44, 0.3))
    parts = [Part(lambda p: np.maximum(np.maximum(sd_capsule(p, (0, -0.32, 0), (0, 0.32, 0), 0.34), -sd_capsule(p, (0, -0.3, 0), (0, 0.3, 0), 0.31)), -window(p)), 0),
             Part(lambda p: sd_cyl(p, 0.36, 0.08, 0.03), 1, pos=(0, 0.62, 0)), Part(lambda p: sd_cyl(p, 0.36, 0.08, 0.03), 1, pos=(0, -0.62, 0)),
             Part(lambda p: sd_torus(p, 0.33, 0.02), 2, pos=(0, 0, 0))]
    return parts + inner(3), mats


@reg('mammoth_capsule', (('rock',), ('glint',)))
def b_mammoth_capsule(c1, c2, t):
    """A young mammoth asleep in its capsule, curled up, trunk tucked, under a ring of chill light."""
    breathe = 1 + 0.04 * math.sin(TAU * t)
    def inner(k):
        return [Part(lambda p: sd_ellipsoid(p, (0.22 * breathe, 0.2 * breathe, 0.22)), k, pos=(0, -0.12, 0)),
                Part(lambda p: sd_sphere(p, 0.12), k, pos=(0.0, 0.1, -0.12)),
                Part(lambda p: sd_capsule(p, (0, 0, 0), (0.02, -0.15, -0.05), 0.04), k, pos=(0.0, 0.02, -0.22)),
                Part(lambda p: sd_capsule(p, (0, 0, 0), (0.1, -0.06, -0.08), 0.022), k + 1, pos=(0.06, 0.02, -0.2))]
    parts, mats = _capsule_glass(inner, [Material('#7a4a26', '#c88a50', metal=0.2), Material('#f0e6cc', '#ffffff', metal=0.4)], t)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0)


@reg('meganeura_capsule', (('rock',), ('glint',)))
def b_meganeura_capsule(c1, c2, t):
    """A griffinfly in a capsule of high-oxygen air: its long body upright, wings folded, the air faintly glowing."""
    shiver = 0.04 * math.sin(TAU * t * 3)
    def inner(k):
        parts = [Part(lambda p: sd_capsule(p, (0, -0.42, 0), (0, 0.15, 0), 0.04), k + 1),
                 Part(lambda p: sd_sphere(p, 0.08), k, pos=(0, 0.22, 0)), Part(lambda p: sd_ellipsoid(p, (0.07, 0.1, 0.07)), k, pos=(0, 0.1, 0))]
        for s in (-1, 1):
            parts.append(Part(lambda p: sd_ellipsoid(p, (0.04, 0.32, 0.012)), k + 2, pos=(s * 0.06, -0.12, -0.05), rot=rotz(s * (0.12 + shiver))))
        return parts
    parts, mats = _capsule_glass(inner, [Material('#2e8a50', '#90e0a8', metal=0.8), Material('#2a5a8a', '#80b0e0', metal=0.8),
                                         Material('#d8ecf8', '#ffffff', metal=0.95)], t)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0)


# ======================================================================= render and write
ITEMS = sorted(B)
TEX = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/textures/item')


def frames(iid, n=40):
    out = []
    for f in range(n):
        t = f / n
        parts, mats, view = B[iid]('#ffffff', '#ffffff', t)
        motions, overlays = MOTIONS[iid]
        view = anim.apply_motions(parts, mats, view, t, motions, anim.h(iid))
        img = I.render(parts, mats, **view)
        if 'glint' in overlays:
            anim.ov_glint(img, t, anim.h(iid), start=(anim.h(iid) % 7) / 7)
        out.append(img)
    return out


def save_strip(fr, path):
    from PIL import Image
    import trim_animations
    img = np.concatenate(fr, axis=0)
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').save(path)
    json.dump({"animation": {"frametime": 1, "interpolate": False}}, open(path + '.mcmeta', 'w'))
    trim_animations.trim(path)


if __name__ == '__main__':
    from PIL import Image, ImageDraw
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        out = sys.argv[2]
        cols = 8; S = 100
        W = Image.new('RGBA', (cols * S, ((len(ITEMS) + cols - 1) // cols) * (S + 12)), (58, 58, 64, 255)); d = ImageDraw.Draw(W)
        for i, iid in enumerate(ITEMS):
            parts, mats, view = B[iid]('#ffffff', '#ffffff', 0.15)
            img = I.render(parts, mats, **view)
            im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((96, 96), Image.NEAREST)
            x, y = (i % cols) * S, (i // cols) * (S + 12)
            W.alpha_composite(im, (x + 2, y + 2)); d.text((x + 2, y + 98), iid[:16], fill=(255, 255, 255, 255))
        W.save(out)
        print('preview', out)
    else:
        only = sys.argv[1:]
        for iid in ITEMS:
            if only and iid not in only:
                continue
            save_strip(frames(iid), os.path.join(TEX, iid + '.png'))
        print('rendered', len(only) or len(ITEMS), 'items')
