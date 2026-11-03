"""
Round 43 (1.3.6): the late and endgame multiblocks, rebuilt larger and each with a silhouette of its own.

The design language comes from GregTech: New Horizons' and MeatballCraft's endgame structures:
  * sparse lines at large scale - rings, spokes, lattices and frames rather than filled volumes;
  * varied symmetry and orientation - horizontal rings, upright wheels, armillary spheres, a tilted ring, a plus-sign of
    spire and arm, interlocked links, mirrored halves, a tree;
  * combined axes - a spire through a disc, a barrel into a ring, a sphere on a waisted pedestal;
  * a hollow focus - the effect plays in empty space the structure frames;
  * one main casing with a few accent blocks, and regular repetition (ribs, bands) along anything long.

Size budgets: late machines (256M FE) up to 25 wide, 512M-1B up to ~33-41, the 2B endgame up to 45.

No player loses anything: every rebuilt structure uses AT LEAST as many of each block as its 1.3.5 form did
(`_keep_counts` grows the shape until it does), so an old build's blocks all go into the new one. The 1.3.5 layouts
ship as "legacy" patterns, so the Structure Assembler's disassemble mode can still take an old build apart.
"""
import json
import math
import os

import shapes

HERE = os.path.dirname(os.path.abspath(__file__))
LEGACY = json.load(open(os.path.join(HERE, 'legacy_shapes_135.json')))
TAU = 2 * math.pi


# ------------------------------------------------------------------------------------------------ primitives
def ring(s, cx, cy, cz, r, ch, axis='y', thick=0.5, force=True, tilt=0.0):
    """A closed ring of radius r around (cx, cy, cz), normal to `axis`; `tilt` (radians) leans a horizontal ring about x."""
    n = max(24, int(TAU * r * 4))
    for i in range(n):
        a = TAU * i / n
        u, v = math.cos(a) * r, math.sin(a) * r
        if axis == 'y':
            p = (u, -v * math.sin(tilt), v * math.cos(tilt))
        elif axis == 'x':
            p = (0, u, v)
        else:
            p = (u, v, 0)
        for d in ((0, 0, 0),) if thick < 0.75 else ((0, 0, 0), (0, 1, 0), (0, -1, 0)):
            s.put(cx + p[0], cy + p[1] + d[1], cz + p[2], ch, force)


def torus(s, cx, cy, cz, R, r, ch, axis='y', force=True):
    """A thick ring: every cell within r of the circle of radius R."""
    for x in range(s.w):
        for y in range(s.h):
            for z in range(s.d):
                dx, dy, dz = x - cx, y - cy, z - cz
                if axis == 'y':
                    q = math.hypot(math.hypot(dx, dz) - R, dy)
                elif axis == 'x':
                    q = math.hypot(math.hypot(dy, dz) - R, dx)
                else:
                    q = math.hypot(math.hypot(dx, dy) - R, dz)
                if q <= r:
                    s.put(x, y, z, ch, force)


def shell(s, cx, cy, cz, r, ch, keep=None, thick=0.55):
    s.sphere(cx, cy, cz, r, ch, shell=thick, filter=keep)


def frustum(s, cx, cz, y0, y1, r0, r1, ch, hollow=0.0):
    for y in range(y0, y1 + 1):
        t = (y - y0) / max(1, y1 - y0)
        r = r0 + (r1 - r0) * t
        s.disc(cx, cz, y, r, ch, r_in=(r - hollow) if hollow else -1.0)


def beam(s, a, b, ch, force=True):
    n = int(max(abs(b[i] - a[i]) for i in range(3)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        s.put(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t, ch, force)


def counts(v):
    c = {}
    for ch in v.values():
        if ch not in ' .':
            c[ch] = c.get(ch, 0) + 1
    return c


def _keep_counts(s, name, base_ch, port_ch):
    """Grow the shape until it holds at least as many of every block as the 1.3.5 build did. New cells are added next to
    blocks of the same kind (lowest first, nearest the middle), so the extra material thickens what is already there."""
    old = LEGACY[name]['layers']
    want = {}
    for layer in old:
        for row in layer:
            for ch in row:
                if ch not in ' .C':
                    want[ch] = want.get(ch, 0) + 1
    # ports: the old number of port positions, as port-capable casing
    have = counts(s.v)
    if port_ch in want and have.get(port_ch, 0) < want[port_ch]:
        extra = want[port_ch] - have.get(port_ch, 0)
        base = sorted((p for p, c in s.v.items() if c == base_ch and p[1] == 0), key=lambda p: (abs(p[0] - s.center[0]) + abs(p[2] - s.center[2])))
        for p in base[-extra:]:
            s.v[p] = port_ch
    cx, _, cz = s.center
    for ch, n in want.items():
        if ch == port_ch:
            continue
        have = counts(s.v)
        need = n - have.get(ch, 0)
        guard = 0
        while need > 0 and guard < 40:
            guard += 1
            front = []
            for (x, y, z), c in list(s.v.items()):
                if c != ch:
                    continue
                for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
                    q = (x + d[0], y + d[1], z + d[2])
                    if q in s.v or not (0 <= q[0] < s.w and 0 <= q[1] < s.h and 0 <= q[2] < s.d):
                        continue
                    if (q[0], q[1], q[2] - 1) == s.ctrl or (q[0], q[1], q[2]) == (s.ctrl[0], s.ctrl[1], s.ctrl[2] - 1):
                        continue
                    front.append(q)
            if not front:
                # nothing of this kind has room beside it (a lone emitter in an empty core): grow it on the outside of the
                # structure's base instead
                front = []
                for (x, y, z), c in list(s.v.items()):
                    if c in ' .':
                        continue
                    for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)):
                        q = (x + d[0], y + d[1], z + d[2])
                        if q in s.v or not (0 <= q[0] < s.w and 0 <= q[1] < s.h and 0 <= q[2] < s.d):
                            continue
                        if q[2] <= s.ctrl[2] and abs(q[0] - s.ctrl[0]) <= 1 and abs(q[1] - s.ctrl[1]) <= 1:
                            continue
                        front.append(q)
                if not front:
                    break
            front = sorted(set(front), key=lambda q: (q[1], math.hypot(q[0] - cx, q[2] - cz)))
            for q in front[:need]:
                s.v[q] = ch
            need = n - counts(s.v).get(ch, 0)
    return s


def _finish(s, name, base_ch, port_ch, ctrl, nports=4, port_y=0):
    s.ports(base_ch, port_ch, nports, y=port_y)
    s.v[ctrl] = 'C'
    s.ctrl = ctrl
    _keep_counts(s, name, base_ch, port_ch)
    return s


# ------------------------------------------------------------------------------------------------ late machines (256M)
def abiogenesis_crucible():
    """A primordial tide-pool: three terraces round a shallow glass sea, a glass retort on a stem in the middle, and four
    spark spires leaning over it - Miller and Urey's electrodes - each ending in an injector aimed at the retort."""
    s = shapes.Shape(23, 15, 23)
    c = 11
    s.disc(c, c, 0, 11.3, 'c')
    s.disc(c, c, 1, 11.3, 'c', r_in=9.2)
    s.disc(c, c, 2, 9.2, 'c', r_in=7.6)
    s.disc(c, c, 1, 9.0, 'G')                               # the sea
    s.column(c, c, 2, 4, 'c')
    s.put(c, 2, c, 'y')
    shell(s, c, 7, c, 3.4, 'G')
    s.sphere_air(c, 7, c, 2.9)
    for sx in (-1, 1):
        for sz in (-1, 1):
            x0, z0 = c + sx * 9, c + sz * 9
            beam(s, (x0, 2, z0), (c + sx * 6, 12, c + sz * 6), 'c')
            s.put(c + sx * 5, 12, c + sz * 5, 'j')
    s.put(c, 12, c, 'F')
    s.center = (c, 7, c)
    return _finish(s, 'abiogenesis_crucible', 'c', 'd', (c, 1, c - 10))


def annihilation_reactor():
    """The antimatter cross: a spire through two nested horizontal rings (confinement casing outside, superconducting coil
    inside) and an upright coil ring, crossed by a cryogenic arm - a plus sign from the front - round a glass core."""
    s = shapes.Shape(25, 25, 25)
    c, cy = 12, 12
    s.disc(c, c, 0, 5.5, 'M')
    s.disc(c, c, 1, 4.5, 'M')
    for y in list(range(2, 8)) + list(range(17, 24)):
        s.disc(c, c, y, 1.2, 'M')                           # the spire, above and below the core
    torus(s, c, cy, c, 11, 0.8, 'M')
    ring(s, c, cy, c, 9, 'K')
    ring(s, c, cy, c, 9, 'K', axis='z')
    shell(s, c, cy, c, 3.5, 'G')
    s.sphere_air(c, cy, c, 3.0)
    for x in list(range(0, 8)) + list(range(17, 25)):
        s.put(x, cy, c, 'i')                                # the cryogenic arm
    s.put(0, cy + 1, c, 'v'); s.put(24, cy + 1, c, 'v'); s.put(c, 24, c, 'v')
    s.put(c, cy + 5, c, 'F'); s.put(c, cy - 5, c, 'F')
    s.center = (c, cy, c)
    return _finish(s, 'annihilation_reactor', 'M', 'N', (c, 1, c - 4))


def anomaly_containment_ward():
    """A quarantine complex on a cross plan: the specimen cell in the middle, and four airlock wings - windowed tunnels -
    each closed by a causal-shield door. Stabilisers stand on the cell's corner pillars."""
    s = shapes.Shape(25, 12, 25)
    c = 12
    s.box(7, 0, 7, 17, 0, 17, 'O')
    s.box(8, 1, 8, 16, 1, 16, 'z')
    for y, ch in ((2, 'O'), (3, 'G'), (4, 'K'), (5, 'G'), (6, 'O'), (7, 'O')):
        for x in range(8, 17):
            for z in range(8, 17):
                if x in (8, 16) or z in (8, 16):
                    s.put(x, y, z, ch)
    s.box(8, 8, 8, 16, 8, 16, 'O')
    s.air_box(9, 2, 9, 15, 7, 15)
    for x, z in ((7, 7), (17, 7), (7, 17), (17, 17)):
        s.column(x, z, 1, 8, 'O')
        s.put(x, 9, z, 's')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):        # airlock wings
        for k in range(9, 13):
            for a in range(-2, 3):
                for y in range(0, 6):
                    x = c + dx * k + (a if dz else 0)
                    z = c + dz * k + (a if dx else 0)
                    edge = abs(a) == 2 or y in (0, 5)
                    if edge:
                        s.put(x, y, z, 'G' if (y == 3 and abs(a) == 2) else 'O')
                    else:
                        s.put(x, y, z, '.', force=False)
        for a in range(-2, 3):
            for y in range(1, 5):
                s.put(c + dx * 12 + (a if dz else 0), y, c + dz * 12 + (a if dx else 0), 'z')
    for x, z in ((10, 10), (14, 10), (10, 14), (14, 14)):
        s.put(x, 7, z, 'F')
    s.put(c, 8, c, 'F'); s.put(c, 2, c, 'F')
    s.center = (c, 4, c)
    return _finish(s, 'anomaly_containment_ward', 'O', 'J', (c, 3, 8 - 0), port_y=0)


def anomaly_resolution_engine():
    """A cathedral of dosing: a tapering tower of reality-lattice rings banded with coils, four flying buttresses, and at
    the top a ring of graviton lenses focusing the dose down an open shaft onto the anomaly's pedestal."""
    s = shapes.Shape(17, 25, 17)
    c = 8
    s.disc(c, c, 0, 8.3, 'A')
    for y in range(1, 21):
        r = 5.6 - 0.12 * y
        s.disc(c, c, y, r, 'K' if y % 5 == 2 else ('G' if y % 5 == 4 else 'A'), r_in=r - 1.0)
    for y in range(1, 22):
        s.disc(c, c, y, 1.6 if y > 3 else 2.4, '.', force=False)
    for k in range(4):
        a = math.radians(45 + 90 * k)
        beam(s, (c + math.cos(a) * 8, 0, c + math.sin(a) * 8), (c + math.cos(a) * 4, 14, c + math.sin(a) * 4), 'A')
    s.disc(c, c, 21, 3.4, 'A', r_in=1.5)
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        s.put(c + dx, 22, c + dz, 'l')
        s.put(c + dx, 1, c + dz, 'n')
    s.put(c, 23, c, 'F'); s.put(c, 24, c, 'A')
    s.put(c, 1, c, 'F')
    s.center = (c, 6, c)
    return _finish(s, 'anomaly_resolution_engine', 'A', 'D', (c, 1, c - 5))


def causality_loop_engine():
    """Two upright rings linked like a chain - one in each vertical plane - each a chronoplastic torus lined with coil and
    glazed, so a signal can go round one and through the other: a closed timelike curve. Shield plinths carry them."""
    s = shapes.Shape(25, 21, 19)
    cy, cz = 10, 9
    torus(s, 8, cy, cz, 7.5, 0.7, 'O', axis='z')
    ring(s, 8, cy, cz, 6.6, 'K', axis='z')
    torus(s, 16, cy, cz, 7.5, 0.7, 'O', axis='x')
    ring(s, 16, cy, cz, 6.6, 'G', axis='x')
    for x, z in ((8, cz), (16, cz)):
        s.box(x - 2, 0, z - 2, x + 2, 1, z + 2, 'O')
        s.box(x - 1, 2, z - 1, x + 1, 2, z + 1, 'z')
    s.box(4, 0, cz - 1, 20, 0, cz + 1, 'O')
    s.put(8, cy, cz, 'F'); s.put(16, cy, cz, 'F')
    s.center = (12, cy, cz)
    return _finish(s, 'causality_loop_engine', 'O', 'J', (12, 0, cz - 2 + 1 - 1))


def entropy_horizon():
    """The heat death in layers: three domes one inside another, the outer two only lattices - open to the cold - each
    ringed at its foot by coil, with Hawking collectors on the crown."""
    s = shapes.Shape(25, 14, 25)
    c = 12
    s.disc(c, c, 0, 12.3, 'E', r_in=10.6)
    s.disc(c, c, 0, 6, 'E')
    shell(s, c, 0, c, 11.5, 'E', keep=lambda x, y, z: y >= 1 and ((x + z) % 3 == 0 or y % 4 == 0))
    shell(s, c, 0, c, 8.0, 'G', keep=lambda x, y, z: y >= 1 and (x + y + z) % 2 == 0)
    shell(s, c, 0, c, 5.0, 'E', keep=lambda x, y, z: y >= 1)
    s.sphere_air(c, 0, c, 4.4)
    ring(s, c, 1, c, 11.5, 'K')
    ring(s, c, 1, c, 8.0, 'O')
    for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1)):
        s.put(c + dx, 12 if (dx, dz) == (0, 0) else 11, c + dz, 'w')
    s.put(c, 1, c, 'F'); s.put(c, 0, c, 'z')
    s.center = (c, 3, c)
    return _finish(s, 'entropy_horizon', 'E', 'V', (c, 1, 1))


def essence_distillery():
    """A distillation train: a main column and two shorter side columns, each a stack of glazed trays banded by condenser
    coils, joined by headers - one tray per fraction."""
    s = shapes.Shape(21, 22, 11)
    cz = 5
    for cx, top, r in ((10, 21, 2.6), (4, 15, 2.0), (16, 11, 2.0)):
        s.disc(cx, cz, 0, r + 1.2, 'f')
        for y in range(1, top + 1):
            ch = 'k' if y % 4 == 0 else ('G' if y % 2 else 'f')
            s.disc(cx, cz, y, r, ch, r_in=r - 1.0)
            s.disc(cx, cz, y, r - 1.0, '.', force=False)
        s.disc(cx, cz, top + 1 if top + 1 < 22 else top, r - 0.6, 'f')
    for y, x0, x1 in ((8, 6, 8), (11, 12, 14), (13, 6, 8)):
        for x in range(x0, x1 + 1):
            s.put(x, y, cz, 'f')
    s.center = (10, 10, cz)
    return _finish(s, 'essence_distillery', 'f', 'g', (10, 1, cz - 2))


def inflation_chamber():
    """A balloon of spacetime: a glass-and-cosmogenic sphere on a slim stem, ringed by coil at its equator and held down by
    six cables to stabiliser anchors on the ground."""
    s = shapes.Shape(25, 22, 25)
    c, cy = 12, 12
    s.disc(c, c, 0, 4.5, 'X')
    s.column(c, c, 1, 3, 'X')
    shell(s, c, cy, c, 8.0, 'X', keep=lambda x, y, z: (y - cy) % 3 == 0)
    shell(s, c, cy, c, 8.0, 'G', keep=lambda x, y, z: (y - cy) % 3 != 0)
    s.sphere_air(c, cy, c, 7.4)
    ring(s, c, cy, c, 9.0, 'K')
    for k in range(6):
        a = TAU * k / 6
        gx, gz = c + math.cos(a) * 11, c + math.sin(a) * 11
        beam(s, (gx, 1, gz), (c + math.cos(a) * 7, cy + 3, c + math.sin(a) * 7), 'X')
        s.put(gx, 0, gz, 's' if k < 4 else 'X')
    s.put(c, cy + 9, c, 'l'); s.put(c, cy, c, 'F'); s.put(c, cy + 10, c, 'F')
    s.center = (c, cy, c)
    return _finish(s, 'inflation_chamber', 'X', 'Z', (c, 1, c - 4))


def mirror_foundry():
    """Two identical stepped forges facing each other across a wall of glass - the mirror - with a bridge over it and the
    coil core set in the mirror's middle."""
    s = shapes.Shape(27, 13, 15)
    cz = 7
    for y in range(0, 9):                                     # the left forge, then its mirror image
        w = 6 - y // 2
        for x in range(0, 12):
            for z in range(cz - w, cz + w + 1):
                inside = x >= 12 - 2 * w - 1
                if not inside:
                    continue
                if y == 0 or y == 8 or x == 12 - 2 * w - 1 or abs(z - cz) == w:
                    s.put(x, y, z, 'M')
                    s.put(26 - x, y, z, 'M')
    for y in range(0, 11):
        for z in range(cz - 6, cz + 7):
            if abs(z - cz) + y <= 15:
                s.put(13, y, z, 'G')
    for x in range(7, 20):
        s.put(x, 11, cz, 'M')
    s.put(13, 5, cz, 'K'); s.put(13, 12, cz, 'F')
    s.center = (13, 5, cz)
    return _finish(s, 'mirror_foundry', 'M', 'N', (3, 1, cz - 5))


def universe_forge():
    """A cosmic egg held up by three curved claws over a broad pedestal, girdled by two coil rings at its tropics, with
    anchor pylons where the claws grip."""
    s = shapes.Shape(25, 25, 25)
    c, cy = 12, 13
    s.disc(c, c, 0, 10.5, 'X')
    s.disc(c, c, 1, 8.5, 'X', r_in=6.5)
    for x in range(s.w):
        for y in range(s.h):
            for z in range(s.d):
                q = ((x - c) / 6.0) ** 2 + ((y - cy) / 8.5) ** 2 + ((z - c) / 6.0) ** 2
                if 0.8 <= q <= 1.0:
                    s.put(x, y, z, 'G' if abs(y - cy) <= 1 else 'X')
    for k in range(3):
        a = TAU * k / 3 + 0.3
        pts = []
        for i in range(25):
            t = i / 24
            r = 10 - 3.5 * math.sin(t * math.pi * 0.8)
            pts.append((c + math.cos(a) * r, 1 + t * 19, c + math.sin(a) * r))
        for p0, p1 in zip(pts, pts[1:]):
            beam(s, p0, p1, 'X')
        s.put(*pts[18], 'y')
    ring(s, c, cy - 4, c, 6.4, 'K')
    ring(s, c, cy + 4, c, 6.4, 'K')
    s.put(c, cy + 9, c, 's'); s.put(c, 2, c, 's'); s.put(c, cy, c, 'F'); s.put(c, cy + 10, c, 'F')
    s.center = (c, cy, c)
    return _finish(s, 'universe_forge', 'X', 'Z', (c, 1, c - 7))


def vacuum_harvester():
    """A Casimir stack: broad plates of Planck casing and containment glass, each a gap apart, on four corner columns, with a
    coil ring round the middle gap and lenses at the corners."""
    s = shapes.Shape(19, 19, 19)
    c = 9
    for i, y in enumerate(range(0, 19, 3)):                   # round plates, alternately Planck casing and glass
        ch = '5' if i % 2 == 0 else 'G'
        s.disc(c, c, y, 7.6, ch)
        if y + 1 < 19:
            for yy in (y + 1, y + 2):
                s.disc(c, c, yy, 6.6, '.', force=False)
    for k in range(4):
        a = TAU * k / 4 + math.pi / 4
        x, z = round(c + math.cos(a) * 8.4), round(c + math.sin(a) * 8.4)
        s.column(x, z, 0, 18, '5')
        s.put(x, 9, z, 'l')
    ring(s, c, 10, c, 9.5, 'K')
    s.put(c, 10, c, 'F')
    s.center = (c, 10, c)
    return _finish(s, 'vacuum_harvester', '5', '6', (c, 1, 1))


# ------------------------------------------------------------------------------------------------ 512M - 1B
def matrioshka_brain():
    """Three shells, one inside another: a solid computronium core, an error-correcting lattice round it, and an outer
    stellar frame in latitude bands with glass between - open at the poles, cooled by a ring at the equator."""
    s = shapes.Shape(29, 29, 29)
    c = 14
    shell(s, c, c, c, 4.2, 'Q')
    shell(s, c, c, c, 8.0, 'n', keep=lambda x, y, z: (x + y + z) % 3 == 0)
    shell(s, c, c, c, 8.0, 'Q', keep=lambda x, y, z: (x + y + z) % 3 == 1)
    shell(s, c, c, c, 12.6, 'T', keep=lambda x, y, z: abs(y - c) < 11 and (y - c) % 4 == 0)
    shell(s, c, c, c, 12.6, 'G', keep=lambda x, y, z: abs(y - c) < 11 and (y - c) % 4 == 2 and (x + z) % 2 == 0)
    ring(s, c, c, c, 13.6, 'i')
    for k in range(8):                                        # struts tying the shells
        a = TAU * k / 8
        beam(s, (c + math.cos(a) * 4.5, c, c + math.sin(a) * 4.5), (c + math.cos(a) * 12.5, c, c + math.sin(a) * 12.5), 'T')
    s.disc(c, c, 0, 5.5, 'T')
    s.column(c, c, 1, 9, 'T')
    s.put(c, c, c, 'F')
    s.center = (c, c, c)
    return _finish(s, 'matrioshka_brain', 'T', 'W', (c, 1, c - 2))


def dyson_array():
    """A Dyson swarm in plan: nested rings joined by eight spokes round a glass star, and one great ring tilted out of the
    plane - the swarm's other orbit. Low and sparse, 41 across."""
    s = shapes.Shape(41, 21, 41)
    c, cy = 20, 9
    shell(s, c, cy, c, 3.4, 'G')
    shell(s, c, cy, c, 3.4, '1', keep=lambda x, y, z: (x + y + z) % 2 == 0)
    s.sphere_air(c, cy, c, 2.8)
    ring(s, c, cy, c, 8, '1')
    ring(s, c, cy, c, 14, 'T')
    ring(s, c, cy, c, 19.5, 'T')
    ring(s, c, cy, c, 14, 'T', tilt=math.radians(35))
    for k in range(8):
        a = TAU * k / 8
        beam(s, (c + math.cos(a) * 3.5, cy, c + math.sin(a) * 3.5), (c + math.cos(a) * 19.5, cy, c + math.sin(a) * 19.5), 'T')
    for k in range(4):                                        # four legs down to the ground
        a = TAU * k / 4 + math.pi / 4
        beam(s, (c + math.cos(a) * 14, cy, c + math.sin(a) * 14), (c + math.cos(a) * 16, 0, c + math.sin(a) * 16), 'T')
    s.disc(c, c, 0, 3.5, 'T')
    s.column(c, c, 1, cy - 4, 'T')
    s.put(c, cy, c, 'F'); s.put(c, cy + 4, c, 'F')
    s.center = (c, cy, c)
    return _finish(s, 'dyson_array', 'T', 'W', (c, 1, c - 2))


def reality_compiler():
    """A ziggurat of reality lattice: five terraces, error-correction cores at the corners of each, a hollow chamber holding
    a stack of computronium, and a spire with a field emitter on top."""
    s = shapes.Shape(31, 23, 31)
    c = 15
    for k in range(5):
        h0, w = k * 3, 15 - k * 3
        for y in range(h0, h0 + 3):
            for x in range(c - w, c + w + 1):
                for z in range(c - w, c + w + 1):
                    terrace = max(abs(x - c), abs(z - c)) > w - 3       # the floor only where the next tier does not cover it
                    if (y == h0 and terrace) or abs(x - c) == w or abs(z - c) == w:
                        s.put(x, y, z, 'A')
        for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            s.put(c + dx * w, h0 + 3, c + dz * w, 'n')
    s.air_box(c - 5, 1, c - 5, c + 5, 11, c + 5)
    for y in range(1, 11):
        s.disc(c, c, y, 2.2, 'Q', r_in=1.0)
    s.column(c, c, 15, 21, 'A')
    s.put(c, 22, c, 'F')
    for dx in (-1, 1):
        s.put(c + dx, 13, c, 'G'); s.put(c, 13, c + dx, 'G')
    s.center = (c, 6, c)
    return _finish(s, 'reality_compiler', 'A', 'D', (c, 1, 0))


# ------------------------------------------------------------------------------------------------ 2B endgame
def axiomatic_breach_engine():
    """A breach in the axioms: a wireframe cube of transfinite casing with a great tilted ring of coil and lattice crashing
    through it, anchored at the cube's corners."""
    s = shapes.Shape(33, 33, 33)
    c = 16
    lo, hi = 6, 26
    for a in range(lo, hi + 1):
        for (y, z) in ((lo, lo), (lo, hi), (hi, lo), (hi, hi)):
            s.put(a, y, z, 't'); s.put(y, a, z, 't'); s.put(y, z, a, 't')
    ring(s, c, c, c, 15, 'K', axis='z')
    ring(s, c, c, c, 15, 'K', tilt=math.radians(55))
    ring(s, c, c, c, 14, 'A', tilt=math.radians(55))
    for x in (lo, hi):
        for z in (lo, hi):
            s.column(x, z, 0, lo - 1, 't')
            s.put(x, hi + 1, z, 'y')
    s.box(lo - 1, 0, lo - 1, hi + 1, 0, hi + 1, 't', hollow=False)
    s.air_box(lo + 1, 1, lo + 1, hi - 1, hi - 1, hi - 1)
    s.put(c, c, c, 's'); s.put(c, c + 3, c, 'F')
    s.center = (c, c, c)
    return _finish(s, 'axiomatic_breach_engine', 't', 'u', (c, 1, lo - 1))


def concept_compressor():
    """An armillary of concepts: a sphere of conceptual casing inside three crossed coil rings, nine injector spokes of
    lenses converging on its centre, stabilisers where the spokes meet the shell, on a stepped pedestal."""
    s = shapes.Shape(31, 31, 31)
    c = 15
    s.disc(c, c, 0, 9.5, '0')
    s.disc(c, c, 1, 7.5, '0')
    s.disc(c, c, 2, 5.5, '0')
    shell(s, c, c, c, 8.0, '0')
    shell(s, c, c, c, 8.0, 'G', keep=lambda x, y, z: abs(y - c) <= 1)
    s.sphere_air(c, c, c, 7.4)
    for axis in ('x', 'y', 'z'):
        ring(s, c, c, c, 12, '3', axis=axis)
        ring(s, c, c, c, 12.8, '0', axis=axis)
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                for k in range(9, 15):
                    s.put(c + sx * k * 0.6, c + sy * k * 0.6, c + sz * k * 0.6, 'F' if k == 14 else 'l')
                s.put(c + sx * 4.7, c + sy * 4.7, c + sz * 4.7, 's')
    for y in range(c + 9, 31):
        s.put(c, y, c, 'F' if y == 30 else 'l')
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        s.put(c + dx * 7, 3, c + dz * 7, 'y')
    s.center = (c, c, c)
    return _finish(s, 'concept_compressor', '0', '9', (c, 1, c - 7), nports=8)


def eye_of_the_horizon():
    """A captive black hole in an armillary sphere: five great rings of hyperframe with coil linings (three on the axes,
    two on the diagonals), a lattice of lenses and glass round the empty heart, a flat accretion ring at the equator,
    anchor pylons on the axes and a stepped event-horizon pedestal. 41 across."""
    s = shapes.Shape(41, 41, 41)
    c = 20
    for y, r in ((0, 15.5), (1, 12.5), (2, 9.5)):
        s.disc(c, c, y, r, 'E')
    s.disc(c, c, 1, 15.5, 'i', r_in=13.6)
    shell(s, c, c, c, 9.0, 'l', keep=lambda x, y, z: (x + y + z) % 3 == 0)
    shell(s, c, c, c, 9.0, 'G', keep=lambda x, y, z: (x + y + z) % 3 == 1)
    s.sphere_air(c, c, c, 8.3)
    for axis in ('x', 'y', 'z'):
        ring(s, c, c, c, 18.0, 'H', axis=axis)
        ring(s, c, c, c, 17.0, '4', axis=axis)
    for t in (math.radians(45), math.radians(-45)):
        ring(s, c, c, c, 18.0, 'H', tilt=t)
    s.disc(c, c, c, 14.5, 'E', r_in=12.2)                       # accretion ring
    for p in ((c, c + 18, c), (c, c - 18, c), (c + 18, c, c), (c - 18, c, c), (c, c, c + 18), (c, c, c - 18)):
        s.put(*p, 's')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for k in range(9, 18):
            s.put(c + dx * k, c, c + dz * k, 'y' if k in (9, 17) else 'H')
        s.column(c + dx * 15, c + dz * 15, 3, c - 1, 'H')
    s.column(c, c, 3, c - 10, 'y')
    s.put(c, c + 10, c, 'w'); s.put(c, c - 10, c, 'w')
    for sx in (-1, 1):
        for sz in (-1, 1):
            for sy in (-1, 1):
                d = 10.4 / math.sqrt(3)
                s.put(c + sx * d, c + sy * d, c + sz * d, 'F')
    s.center = (c, c, c)
    return _finish(s, 'eye_of_the_horizon', 'E', 'V', (c, 2, c - 9), nports=8)


def omnilex_engine():
    """The World Tree: spreading roots of omnilex casing, a trunk of arbor heartwood, branches climbing out to stabilisers,
    a canopy of glass leaves in three tiers, and a ring of anchor pylons round its foot."""
    s = shapes.Shape(31, 31, 31)
    c = 15
    s.disc(c, c, 0, 4.5, 'o')
    for k in range(8):                                        # roots
        a = TAU * k / 8 + 0.2
        beam(s, (c + math.cos(a) * 3, 1, c + math.sin(a) * 3), (c + math.cos(a) * 14, 0, c + math.sin(a) * 14), 'o')
    for y in range(1, 21):
        s.disc(c, c, y, 1.6 if y < 16 else 1.1, 'h')
    for k in range(6):                                        # branches
        a = TAU * k / 6
        y0 = 12 + (k % 3) * 2
        tip = (c + math.cos(a) * 11, y0 + 9, c + math.sin(a) * 11)
        beam(s, (c + math.cos(a) * 1.5, y0, c + math.sin(a) * 1.5), tip, 'o')
        s.put(*tip, 's')
    for y, r in ((21, 11), (24, 9), (27, 6)):                  # canopy
        s.disc(c, c, y, r, 'G', r_in=r - 1.2)
        s.disc(c, c, y + 1, r - 2, 'G', r_in=r - 3.2)
    s.column(c, c, 21, 29, 'h')
    s.put(c, 30, c, 'F')
    for k in range(12):
        a = TAU * k / 12
        s.put(c + math.cos(a) * 9, 1, c + math.sin(a) * 9, 'y')
    s.disc(c, c, 1, 3.6, 'A', r_in=2.0)
    s.center = (c, 12, c)
    return _finish(s, 'omnilex_engine', 'o', 'p', (c, 0, c - 4))


def pleroma_reconstitutor():
    """A cathedral: a broad stepped dais, twelve Absolute Frame buttresses leaning to a crown, a throat of Logos coil
    rising through the nave, a rose window of Pleroma glass on the front, and four corner spires."""
    n, c = 33, 16
    s = shapes.Shape(n, 30, n)
    s.disc(c, c, 0, 16.3, '2', r_in=13.5)
    s.disc(c, c, 0, 8.5, '2')
    s.disc(c, c, 1, 13.5, '2', r_in=11.5)
    s.disc(c, c, 1, 8.5, '*')
    for y in range(2, 25):
        s.disc(c, c, y, 3.2, '&' if y % 3 != 1 else '8', r_in=2.3)
        s.disc(c, c, y, 2.2, '.')
    for k in range(12):
        a = TAU * k / 12 + TAU / 24
        beam(s, (c + math.cos(a) * 13, 1, c + math.sin(a) * 13), (c + math.cos(a) * 5, 25, c + math.sin(a) * 5), '2')
    for y in (8, 15):
        ring(s, c, y, c, 11.5 - (y - 2) * 0.33, '8', force=False)
    s.disc(c, c, 25, 5.4, '2', r_in=4.2)
    s.disc(c, c, 26, 4.2, '*', r_in=3.0)
    ring(s, c, 16, c - 9, 5.5, '8', axis='z')                  # rose window, facing the front
    ring(s, c, 16, c - 9, 3.0, '*', axis='z')
    for x, z in ((3, 3), (29, 3), (3, 29), (29, 29)):
        s.column(x, z, 0, 18, '2')
        s.put(x, 19, z, '*'); s.put(x, 20, z, '8')
    s.center = (c, 13, c)
    return _finish(s, 'pleroma_reconstitutor', '2', '7', (c, 1, c - 11), nports=8)


def transcendent_plasma_forge():
    """After the Forge of the Gods: a great horizontal ring of crucible casing, 45 across, lined with spacetime-alloy coil
    and stellar plates, held on eight pylons round a plasma sphere on its pillar - and a ribbed injector barrel running from
    the front of the ring straight at the sphere."""
    n, c, cy = 45, 22, 11
    s = shapes.Shape(n, 22, n)
    torus(s, c, cy, c, 20, 1.0, 'c')
    ring(s, c, cy, c, 18.6, '3')
    ring(s, c, cy, c, 18.6, '1', thick=1.0, force=False)
    for k in range(8):
        a = TAU * k / 8
        px, pz = c + math.cos(a) * 20, c + math.sin(a) * 20
        s.column(round(px), round(pz), 0, cy - 2, 'c')
        s.put(px, 0, pz, 'i')
    s.disc(c, c, 0, 4.5, 'c')
    s.column(c, c, 1, cy - 6, 'c')
    shell(s, c, cy, c, 5.0, 'G')
    shell(s, c, cy, c, 5.0, '1', keep=lambda x, y, z: (x + y + z) % 2 == 0)
    s.sphere_air(c, cy, c, 4.4)
    ring(s, c, cy, c, 6.5, '3', axis='x')
    ring(s, c, cy, c, 6.5, '3', axis='z')
    for z in range(3, c - 6):                                  # the barrel: injectors along the axis, ribs every third block
        s.put(c, cy, z, 'j')
        if z % 3 == 0:
            ring(s, c, cy, z, 2.0, 'c', axis='z')
    for z in range(3, c - 6, 6):
        s.column(c, z, 1, cy - 3, 'c')
    s.put(c, cy, 2, 'F'); s.put(c, cy + 6, c, 'F'); s.put(c, cy - 6, c, 'F')
    s.center = (c, cy, c)
    return _finish(s, 'transcendent_plasma_forge', 'c', 'd', (c, 1, c - 4), nports=8)


REWORKED = {
    'abiogenesis_crucible': abiogenesis_crucible, 'annihilation_reactor': annihilation_reactor,
    'anomaly_containment_ward': anomaly_containment_ward, 'anomaly_resolution_engine': anomaly_resolution_engine,
    'causality_loop_engine': causality_loop_engine, 'entropy_horizon': entropy_horizon, 'essence_distillery': essence_distillery,
    'inflation_chamber': inflation_chamber, 'mirror_foundry': mirror_foundry, 'universe_forge': universe_forge,
    'vacuum_harvester': vacuum_harvester, 'matrioshka_brain': matrioshka_brain, 'dyson_array': dyson_array,
    'reality_compiler': reality_compiler, 'axiomatic_breach_engine': axiomatic_breach_engine, 'concept_compressor': concept_compressor,
    'eye_of_the_horizon': eye_of_the_horizon, 'omnilex_engine': omnilex_engine, 'pleroma_reconstitutor': pleroma_reconstitutor,
    'transcendent_plasma_forge': transcendent_plasma_forge,
}
shapes.BUILDERS.update(REWORKED)


def legacy(name):
    """The 1.3.5 layout of a reworked machine (for taking an old build apart), or None."""
    return LEGACY[name]['layers'] if name in REWORKED and name in LEGACY else None
