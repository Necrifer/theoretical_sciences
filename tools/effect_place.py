"""1.3.8: keep every multiblock's working animation in view.

The reworked late/endgame shapes (1.3.6) grew to 31-45 blocks across, and several of them buried their effect (a black
hole, a tesseract...) inside solid walls, or left it a speck in the middle of a giant. For each multiblock this casts
rays out from the effect and counts how many reach open air (glass counts as open: it is drawn over the effect). If too
few do, it looks for a better spot - inside an open cavity or the machine's open crown, as near the old spot as it can
- and lets big machines draw their effect bigger, as far as the open space around it allows."""
import math
import numpy as np

SEE = set('. G8')          # air, "anything" and the two glasses
RADIUS = {'BLACK_HOLE': 1.3, 'BLACK_HOLE_DISK': 2.7, 'WHITE_HOLE': 2.6, 'DARK_LENS': 2.4, 'TESSERACT': 1.6, 'COLLIDER': 3, 'RIFT': 2,
          'POCKET_UNIVERSE': 1.5, 'MATRIOSHKA': 2.2, 'DYSON': 2.5, 'ANNIHILATION': 1.6, 'PULSAR': 2, 'WORMHOLE': 1.3, 'TIME_LOOP': 2,
          'EXPANSION': 2, 'MATRIX': 2, 'ASSEMBLY': 2, 'PLASMA': 1.8, 'FRACTIONATION': 2, 'HEARTH': 1.5, 'SYNCHROTRON': 6.5,
          'CONCEPT': 3.2, 'STAR_FORGE': 3.5, 'EYE': 7, 'BREACH': 2, 'OMNILEX': 2.5, 'FOAM': 2,
          'SALT_GLOW': 1.4, 'CZOCHRALSKI': 1.3, 'HEAT_DEATH': 3.5, 'EINSTEIN_RING': 1.3, 'HAWKING_SPARK': 1.0, 'QUASAR_JETS': 2.5}
FIXED = {'COLLIDER', 'SYNCHROTRON', 'ASSEMBLY', 'STARLIGHT', 'SOLAR', 'RADIO', 'DIFFRACTION', 'FRINGES', 'LASER_ARMS', 'GAMMA_RISE', 'BROWNIAN', 'MONTE_CARLO', 'CYCLOTRON', 'TESLA_ARCS', 'NEUTRON_PATHS', 'SONIC_HORIZON', 'TOKAMAK_PLASMA', 'PULSED_FIELD', 'RAILGUN', 'CLIMATE', 'DRILL_STRING', 'CENTRIFUGE', 'MHD_FLOW', 'GAMMA_BURST', 'MARKOV_HOPS', 'DILATION_RINGS', 'WARP_BUBBLE', 'UNRUH_BATH', 'ER_BRIDGE'}      # beams that follow the machine's own ring or line: left where they are
GOOD = 0.55
CAP = {'EYE': 1.3}     # the Eye's rings must stay inside its own inner ring

_rng = np.random.default_rng(7)
_u = _rng.uniform(-1, 1, 160)
_t = _rng.uniform(0, 2 * math.pi, 160)
DIRS = np.stack([np.sqrt(1 - _u ** 2) * np.cos(_t), _u, np.sqrt(1 - _u ** 2) * np.sin(_t)], 1)


def solid_grid(layers, glass_open=True):
    H = len(layers)
    D = max(len(l) for l in layers)
    W = max(len(r) for l in layers for r in l)
    g = np.zeros((W, H, D), bool)
    for y, l in enumerate(layers):
        for z, r in enumerate(l):
            for x, ch in enumerate(r):
                g[x, y, z] = ch not in SEE if glass_open else ch not in '. ' 
    return g


def visibility(g, c, r):
    """Share of rays, leaving the effect's surface, that get out of the structure without hitting a block."""
    W, H, D = g.shape
    p = np.asarray(c, float)[None, :] + DIRS * r * 0.6
    alive = np.ones(len(DIRS), bool)
    out = np.zeros(len(DIRS), bool)
    for st in range(int(2.2 * max(W, H, D) * 4)):
        q = np.rint(p + DIRS * st * 0.25).astype(int)
        inside = (q[:, 0] >= 0) & (q[:, 0] < W) & (q[:, 1] >= 0) & (q[:, 1] < H) & (q[:, 2] >= 0) & (q[:, 2] < D)
        out |= alive & ~inside
        alive &= inside
        if not alive.any():
            break
        idx = np.where(alive)[0]
        hit = g[q[idx, 0], q[idx, 1], q[idx, 2]]
        alive[idx[hit]] = False
    return out.mean()


def occupancy(g, c, r):
    """Share of the effect's volume taken up by blocks (a thin rod through it is fine; a wall is not)."""
    W, H, D = g.shape
    x0, x1 = max(0, int(c[0] - r)), min(W - 1, int(c[0] + r + 1))
    y0, y1 = max(0, int(c[1] - r)), min(H - 1, int(c[1] + r + 1))
    z0, z1 = max(0, int(c[2] - r)), min(D - 1, int(c[2] + r + 1))
    n = s = 0
    for x in range(int(c[0] - r), int(c[0] + r + 2)):
        for y in range(int(c[1] - r), int(c[1] + r + 2)):
            for z in range(int(c[2] - r), int(c[2] + r + 2)):
                if (x - c[0]) ** 2 + (y - c[1]) ** 2 + (z - c[2]) ** 2 > r * r:
                    continue
                n += 1
                if abs(x - c[0]) < 0.6 and abs(z - c[2]) < 0.6:
                    continue          # a spindle up the middle is fine: the effect turns about it
                if 0 <= x < W and 0 <= y < H and 0 <= z < D and g[x, y, z]:
                    s += 1
    return s / max(1, n)


SEALED = 0.12        # below this, the effect cannot be seen at all: move it
LOOSE = 0.10         # the most of an effect's (inner) volume blocks may fill


_SH = DIRS[:120]


def shell(g, c, r):
    """Share of points on a sphere of radius r that sit in a block: a ring or wall at that radius shows up here,
    where the volume test above would miss a thin one."""
    W, H, D = g.shape
    q = np.rint(np.asarray(c, float)[None, :] + _SH * r).astype(int)
    ok = (q[:, 0] >= 0) & (q[:, 0] < W) & (q[:, 1] >= 0) & (q[:, 1] < H) & (q[:, 2] >= 0) & (q[:, 2] < D)
    ok &= ~((np.abs(q[:, 0] - c[0]) < 0.6) & (np.abs(q[:, 2] - c[2]) < 0.6))      # (the spindle again)
    hit = np.zeros(len(q), bool)
    hit[ok] = g[q[ok, 0], q[ok, 1], q[ok, 2]]
    return hit.mean()


def fit(g, c, R, smax):
    """The biggest scale (down to 0.8) at which the effect stays clear of blocks - inside and at its rim - or None."""
    s = smax
    while s >= 0.8:
        r = R * s
        if occupancy(g, c, r * 0.75) <= LOOSE and max(shell(g, c, r * k) for k in (0.5, 0.7, 0.85, 1.0)) <= 0.08:
            return round(s, 2)
        s -= 0.1
    return None


def place(layers, center, effect):
    """(centre, scale) for the effect: kept in its chamber when that can be seen into, and grown to fill it."""
    R = RADIUS.get(effect)
    if R is None or effect in FIXED:
        return center, 1.0
    g = solid_grid(layers)
    walls = solid_grid(layers, glass_open=False)
    W, H, D = g.shape
    smax = max(1.0, min(CAP.get(effect, 3.0), max(W, H, D) / 13.0))
    if visibility(g, center, R) >= SEALED:
        s = fit(walls, center, R, smax)
        return center, max(1.0, s or 1.0)
    # sealed in: the nearest spot on the machine's own axis (up or down it) from which at least half of it shows
    ox, oy, oz = center
    cands = {(ox, y2 / 2, oz) for y2 in range(0, 2 * (H + 12))}
    for c in sorted(cands, key=lambda c: math.dist(c, center)):
        s = fit(walls, c, R, smax)
        if s is None or c[1] > H + R * s * 0.7:          # sits on the crown, not floating above it
            continue
        if s is None or visibility(g, c, R * s) < 0.5:
            continue
        return [float(x) for x in c], s
    return center, 1.0
