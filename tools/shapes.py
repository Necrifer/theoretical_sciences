"""
Procedural multiblock silhouettes.

Each builder draws voxels into a Shape (x left->right, y bottom->top, z front->back; the controller sits on
the front face, z small) and returns pattern layers in the format MultiblockDefinition expects:
layers bottom->top, rows front->back, chars left->right.  '.' = must be air, ' ' = anything.

Design rules (learned from GregTech Leisure / Multiblock Madness 2):
  * a recognisable silhouette - rings, dishes, spheres, spires, presses, pylons - not a box
  * a clear "business end" where the effect plays, kept as required air
  * hatches (interface ports) on accessible exterior faces near the base
  * footprints of at most ~15x15 and heights of at most ~17 (the assembly line is long but narrow),
    so structure checks stay cheap and nothing approaches the size of GT's Forge of the Gods.
"""
import math


class Shape:
    def __init__(self, w, h, d):
        self.w, self.h, self.d = w, h, d
        self.v = {}
        self.center = ((w - 1) / 2, (h - 1) / 2, (d - 1) / 2)
        self.ctrl = None
        self.repeat = None      # (from, to, max): layers from..to may be stacked up to max more times (1.3.5)

    def put(self, x, y, z, ch, force=True):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d:
            if force or (x, y, z) not in self.v or self.v[(x, y, z)] == '.':
                self.v[(x, y, z)] = ch

    def get(self, x, y, z):
        return self.v.get((x, y, z))

    # ---------------------------------------------------------------- primitives
    def box(self, x0, y0, z0, x1, y1, z1, ch, hollow=False, force=True):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    edge = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    if hollow and not edge:
                        continue
                    self.put(x, y, z, ch, force)

    def air_box(self, x0, y0, z0, x1, y1, z1):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if (x, y, z) not in self.v:
                        self.put(x, y, z, '.')

    def disc(self, cx, cz, y, r, ch, r_in=-1.0, force=True):
        for x in range(self.w):
            for z in range(self.d):
                dd = math.hypot(x - cx, z - cz)
                if r_in < dd <= r + 0.35:
                    self.put(x, y, z, ch, force)

    def ring_v(self, cx, cy, z, r, ch, thick=0.6, plane='xy', cz=None):
        """Vertical ring (in the xy plane at depth z, or the zy plane at x=cx)."""
        for a in range(self.w if plane == 'xy' else self.d):
            for y in range(self.h):
                dd = math.hypot((a - cx) if plane == 'xy' else (a - cz), y - cy)
                if abs(dd - r) <= thick:
                    if plane == 'xy':
                        self.put(a, y, z, ch)
                    else:
                        self.put(cx, y, a, ch)

    def sphere(self, cx, cy, cz, r, ch, shell=0.8, filter=None):
        for x in range(self.w):
            for y in range(self.h):
                for z in range(self.d):
                    dd = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2)
                    if abs(dd - r) <= shell and (filter is None or filter(x, y, z)):
                        self.put(x, y, z, ch)

    def sphere_air(self, cx, cy, cz, r):
        for x in range(self.w):
            for y in range(self.h):
                for z in range(self.d):
                    if math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) < r and (x, y, z) not in self.v:
                        self.put(x, y, z, '.')

    def great_circle(self, cx, cy, cz, r, ch, axis):
        """Ring of radius r around the centre, normal to `axis` ('x', 'y' or 'z')."""
        n = int(2 * math.pi * r * 3)
        for i in range(n):
            a = 2 * math.pi * i / n
            u, v = math.cos(a) * r, math.sin(a) * r
            if axis == 'y':
                self.put(cx + u, cy, cz + v, ch)
            elif axis == 'x':
                self.put(cx, cy + u, cz + v, ch)
            else:
                self.put(cx + u, cy + v, cz, ch)

    def line(self, a, b, ch):
        n = int(max(abs(b[i] - a[i]) for i in range(3)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            self.put(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t, ch)

    def column(self, x, z, y0, y1, ch):
        for y in range(y0, y1 + 1):
            self.put(x, y, z, ch)

    # ---------------------------------------------------------------- output
    def controller(self, x, y, z=None):
        if z is None:
            z = min(zz for (xx, yy, zz) in self.v if xx == x and yy == y and self.v[(xx, yy, zz)] != '.')
        self.v[(x, y, z)] = 'C'
        self.ctrl = (x, y, z)

    def ports(self, ch_base, ch_port, count=4, y=0):
        """Port-capable positions in a fixed, readable pattern: the middle block of each of the four sides of the base
        layer (the outermost casing on the machine's centre line), and on larger machines (count > 4) also the casing
        two blocks either side of each middle. If a side's middle is taken (by the controller, say), its two
        neighbours one block either side are used instead, so every side stays symmetric."""
        cx, _, cz = self.center
        cx, cz = round(cx), round(cz)

        # only the main body of the base counts (not detached pillars or legs)
        base = {(x, z) for (x, yy, z), c in self.v.items() if yy == y and c not in '. '}
        comps, seen = [], set()
        for st in base:
            if st in seen: continue
            comp, todo = set(), [st]
            seen.add(st)
            while todo:
                a = todo.pop()
                comp.add(a)
                for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = (a[0] + ddx, a[1] + ddz)
                    if n in base and n not in seen:
                        seen.add(n)
                        todo.append(n)
            comps.append(comp)
        body = max(comps, key=len) if comps else set()

        def outer(d, off):
            """The outermost exposed base casing on the line parallel to direction d, offset `off` from the centre line."""
            dx, dz = d
            best = None
            for (x, yy, z), c in self.v.items():
                if yy != y or c != ch_base or (x, z) not in body:
                    continue
                perp = (z - cz) if dx else (x - cx)
                if perp != off:
                    continue
                nx, nz = x + dx, z + dz
                if (nx, yy, nz) in self.v and self.v[(nx, yy, nz)] not in '. ':
                    continue                                     # must face outwards in this direction
                proj = (x - cx) * dx + (z - cz) * dz
                if proj <= 0:
                    continue                                     # never the inner wall of the far side
                if best is None or proj > best[0]:
                    best = (proj, (x, yy, z))
            return best[1] if best else None

        picks = []
        for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            mid = outer(d, 0)
            side = [mid] if mid else [p for p in (outer(d, -1), outer(d, 1)) if p] if (outer(d, -1) and outer(d, 1)) else []
            if not side:
                # no casing on or beside the centre line: the closest one to it, on this side
                for off in range(2, 8):
                    pair = [outer(d, -off), outer(d, off)]
                    if all(pair):
                        side = pair
                        break
            if count > 4 and mid:
                fl = [outer(d, -2), outer(d, 2)]
                proj = lambda p: (p[0] - cx) * d[0] + (p[2] - cz) * d[1]
                if all(fl) and proj(fl[0]) == proj(mid) == proj(fl[1]): side += fl     # only along a flat side
            picks += side
        if not picks:                                          # unusual base: fall back to evenly around the perimeter
            cand = [(x, yy, z) for (x, yy, z), c in self.v.items() if yy == y and c == ch_base]
            cand.sort(key=lambda p: math.atan2(p[2] - cz, p[0] - cx))
            picks = cand[::max(1, len(cand) // max(4, count))][:max(4, count)]
        for p in picks:
            self.v[p] = ch_port

    def layers(self):
        out = []
        for y in range(self.h):
            rows = []
            for z in range(self.d):
                rows.append(''.join(self.v.get((x, y, z), ' ') for x in range(self.w)))
            out.append(rows)
        return out

    def block_count(self):
        return sum(1 for c in self.v.values() if c not in '. ')


# ------------------------------------------------------------------------------- extra primitives
def ring_cells(cx, cz, r):
    """Grid cells on a circle of radius r, ordered by angle (no duplicates)."""
    seen, out = set(), []
    n = int(2 * math.pi * r * 4)
    for i in range(n):
        a = 2 * math.pi * i / n
        p = (int(round(cx + math.cos(a) * r)), int(round(cz + math.sin(a) * r)))
        if p not in seen:
            seen.add(p)
            out.append((p, a))
    return out


def square_ring(c, half):
    """Cells of a square ring (perimeter) with chamfered corners, ordered around it; also returns which are corners."""
    out = []
    lo, hi = c - half, c + half
    for x in range(lo + 1, hi):
        out.append(((x, lo), 'side'))
    for z in range(lo + 1, hi):
        out.append(((hi, z), 'side'))
    for x in range(hi - 1, lo, -1):
        out.append(((x, hi), 'side'))
    for z in range(hi - 1, lo, -1):
        out.append(((lo, z), 'side'))
    return out


# =============================================================================== the machines
# Component characters (see complexity.KEY_ADD): K/k/3/4 coils (tiered), F field emitters (tiered), e beam pipe,
# m dipole, q quadrupole, r RF cavity, i cryo jacket, v vacuum pump, x detector, j plasma injector, l lens array,
# w Hawking collector, n error-correction core, y spacetime anchor, s dimensional stabiliser, z causal shielding,
# 1 stellar containment plate.

def collider():
    """A storage ring: beam pipe in a lattice of alternating bending dipoles and focusing quadrupoles, RF cavities
    on two opposite sides, cryo jackets and vacuum pumps around it, and four detector halls."""
    s = Shape(15, 6, 15)
    c = 7
    s.disc(c, c, 0, 7.2, 'S', r_in=4.3)                   # foundation ring
    ring = ring_cells(c, c, 6)
    for i, ((x, z), a) in enumerate(ring):
        s.put(x, 2, z, 'e')                                # the beam pipe
        mag = 'm' if (i // 2) % 2 == 0 else 'q'            # FODO-like lattice: two dipoles, two quadrupoles...
        s.put(x, 1, z, mag)
        s.put(x, 3, z, 'i' if i % 3 == 0 else mag)         # cryo jackets sit on the magnets
        if i % 6 == 3:
            s.put(x, 4, z, 'K')                            # superconducting coil packs
    for (x, z), a in ring_cells(c, c, 7):
        s.put(x, 2, z, 'G', force=False)                   # outer shielding window
    for (x, z), a in ring_cells(c, c, 5):
        s.put(x, 1, z, 'v' if int(a * 4 / math.pi) % 2 == 0 else 'S', force=False)   # vacuum pumps inside the ring
    for k in (len(ring) // 8, len(ring) // 8 + len(ring) // 2):   # RF cavities on opposite straights, between the halls
        for d in (-1, 0, 1):
            (x, z), _ = ring[(k + d) % len(ring)]
            s.put(x, 2, z, 'r')
    for (dx, dz) in ((0, -1), (0, 1), (-1, 0), (1, 0)):   # four detector halls straddling the ring
        x0, z0 = c + dx * 6, c + dz * 6
        s.box(x0 - 1, 1, z0 - 1, x0 + 1, 3, z0 + 1, 'x', hollow=True)
        s.put(x0, 2, z0, 'e')
        s.box(x0 - 1, 0, z0 - 1, x0 + 1, 0, z0 + 1, 'S')
        s.box(x0 - 1, 4, z0 - 1, x0 + 1, 4, z0 + 1, 'S')
        s.put(x0, 5, z0, 'F')
    s.disc(c, c, 0, 2.5, 'S')                             # central control platform
    s.column(c, c, 1, 3, 'K')
    s.put(c, 4, c, 'F')
    s.ports('S', 'P', 4)
    s.controller(c, 3, 0) if s.get(c, 3, 0) is not None else None
    if s.ctrl is None:
        s.v[(c, 4, 0)] = 'C'
        s.ctrl = (c, 4, 0)
    s.center = (c, 2, c)
    return s


def synchrotron():
    """Hyperion Synchrotron: a racetrack beamline. Dipoles bend the beam at the four corners, quadrupole doublets
    focus it along the straights, an RF section accelerates it, an injector linac feeds it, and a detector hall
    with its own cryo and vacuum plant sits on the far side."""
    s = Shape(19, 6, 19)
    c, half = 9, 7
    lo, hi = c - half, c + half
    # foundation rails under the whole ring
    for (x, z), kind in square_ring(c, half):
        s.box(x, 0, z, x, 0, z, 'S')
    # beamline at y=2
    cells = square_ring(c, half)
    for i, ((x, z), kind) in enumerate(cells):
        corner = (x in (lo + 1, hi - 1) and z in (lo, hi)) or (z in (lo + 1, hi - 1) and x in (lo, hi))
        s.put(x, 2, z, 'e')
        if corner:
            s.put(x, 1, z, 'm')
            s.put(x, 3, z, 'm')
            s.put(x, 4, z, 'K')
        elif i % 3 == 0:
            s.put(x, 1, z, 'q')
            s.put(x, 3, z, 'q')
        else:
            s.put(x, 1, z, 'v' if i % 3 == 1 else 'S')
            s.put(x, 3, z, 'i')
    # chamfered corners: bending dipoles on the diagonal
    for sx in (lo, hi):
        for sz in (lo, hi):
            x, z = sx + (1 if sx == lo else -1), sz + (1 if sz == lo else -1)
            s.box(x, 0, z, x, 0, z, 'S')
            s.put(x, 1, z, 'K')
            s.put(x, 2, z, 'm')
            s.put(x, 3, z, 'K')
    # RF accelerating section on the right straight
    for z in range(c - 2, c + 3):
        s.put(hi, 2, z, 'r')
        s.put(hi + 1, 2, z, 'i')
    # injector linac from the controller (front) into the ring
    for z in range(0, lo + 1):
        s.put(c, 0, z, 'S')
        s.put(c, 1, z, 'r' if z % 2 == 0 else 'q')
        s.put(c, 2, z, 'e' if z > 0 else 'S')
    # detector hall on the far straight: calorimeter shells around the beam, cryo and pumps on top
    zc = hi
    s.box(c - 3, 0, zc - 1, c + 3, 0, zc + 1, 'S')
    s.box(c - 3, 1, zc - 1, c + 3, 4, zc + 1, 'x', hollow=True)
    for x in range(c - 2, c + 3):
        s.put(x, 2, zc, 'e')
    s.box(c - 3, 5, zc - 1, c + 3, 5, zc + 1, 'i')
    s.put(c, 5, zc, 'F')
    s.put(c - 3, 5, zc, 'v')
    s.put(c + 3, 5, zc, 'v')
    s.ports('S', 'P', 6)
    s.v[(c, 3, 0)] = 'C'
    s.ctrl = (c, 3, 0)
    s.put(c, 2, 0, 'S')
    s.center = (c, 2, c)
    return s


def dark_matter_collector():
    s = Shape(11, 9, 11)
    c = 5
    for y in range(1, 6):                                  # parabolic dish opening upward
        r = 1.2 + (y - 1) ** 1.35 * 1.05
        s.disc(c, c, y, r, 'E', r_in=r - 1.0)
    s.disc(c, c, 6, 5.4, 'K', r_in=4.6)                   # superconducting rim
    s.disc(c, c, 0, 3.4, 'S')
    s.disc(c, c, 0, 2.6, 'i', r_in=1.6)                   # cryogenic base ring
    s.disc(c, c, 1, 1.2, 'E')
    s.column(c, c, 1, 3, 'v')                             # vacuum feed line
    s.put(c, 4, c, 'x')                                   # detector at the focus
    s.put(c, 5, c, 'F')
    s.air_box(c - 1, 6, c - 1, c + 1, 8, c + 1)
    s.ports('S', 'P', 3)
    s.controller(c, 2)
    s.center = (c, 7, c)
    return s


def kugelblitz():
    s = Shape(11, 11, 11)
    c = 5
    s.sphere(c, c, c, 4.6, 'G', shell=0.6)
    s.sphere(c, c, c, 4.6, 'E', shell=0.6, filter=lambda x, y, z: abs(y - c) <= 1)
    for p in ((c, c, 0.4), (c, c, 9.6), (0.4, c, c), (9.6, c, c), (c, 0.4, c), (c, 9.6, c)):
        s.put(*p, 'F')                                     # six inward-facing emitters...
    for p in ((c, c, 0.4), (c, c, 9.6), (0.4, c, c), (9.6, c, c)):   # ...each framed by gravitational lens arrays
        x, y, z = (int(round(v)) for v in p)
        for d in (-1, 1):
            if x in (0, 10):
                s.put(x, y + d, z, 'l')
                s.put(x, y, z + d, 'l')
            else:
                s.put(x, y + d, z, 'l')
                s.put(x + d, y, z, 'l')
    s.sphere(c, c, c, 4.6, 'w', shell=0.6, filter=lambda x, y, z: abs(y - c) >= 4)   # Hawking collectors at the poles
    s.sphere_air(c, c, c, 4.0)
    s.box(c - 2, 0, c - 2, c + 2, 0, c + 2, 'S', force=False)
    for dx, dz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        s.column(c + dx, c + dz, 0, 2, 'S')
        s.put(c + dx, 0, c + dz, 'i')
    s.ports('S', 'P', 4)
    s.controller(c, c, 1)
    s.v[(c, c, 0)] = 'E'
    s.center = (c, c, c)
    return s


def penrose():
    s = Shape(13, 8, 13)
    c = 6
    s.disc(c, c, 0, 6, 'S', r_in=2)
    s.disc(c, c, 0, 5, 'i', r_in=4)                       # cryogenic ring in the floor
    for y, r, ch in ((1, 6.2, 'E'), (2, 6.4, 'K'), (3, 6.4, 'E'), (4, 6.0, 'G'), (5, 5.0, 'E')):
        s.disc(c, c, y, r, ch, r_in=r - 1.1)
    s.disc(c, c, 6, 4.2, 'w', r_in=2.6)                   # Hawking collectors ring the opening
    for (x, z), a in ring_cells(c, c, 6.4):
        if int(a / (math.pi / 4)) % 2 == 0:
            s.put(x, 3, z, 'v', force=True)               # vacuum pumps between the field layers
    s.disc(c, c, 0, 1.6, 'K')
    s.column(c, c, 1, 1, 'F')
    for y in range(2, 6):
        s.disc(c, c, y, 4.9, '.', force=False)
    s.put(c, 7, c, 'F')
    s.ports('S', 'P', 4)
    s.controller(c, 2, 0)
    s.center = (c, 3.5, c)
    return s


def white_hole():
    s = Shape(9, 13, 9)
    c = 4
    for y in range(13):
        k = abs(y - 6) / 6
        r = 1.6 + 2.6 * k ** 1.3
        ch = 'K' if y == 6 else ('H' if y in (0, 12) or y % 3 == 0 else 'G')
        s.disc(c, c, y, r, ch, r_in=r - 0.95)
    s.disc(c, c, 0, 4, 'S', r_in=1)
    s.disc(c, c, 12, 4, 'w', r_in=1)                      # the emission end collects what comes out
    s.disc(c, c, 1, 3.6, 'i', r_in=2.8)
    s.put(c, 0, c, 'F')
    s.put(c, 12, c, 'F')
    for y in range(1, 12):
        k = abs(y - 6) / 6
        s.disc(c, c, y, 0.6 + 2.6 * k ** 1.3, '.', force=False)
    s.ports('S', 'P', 4)
    s.controller(c, 2)
    s.center = (c, 6, c)
    return s


def tesseract():
    s = Shape(11, 11, 11)
    o0, o1, i0, i1 = 0, 10, 3, 7
    for (a, b) in ((o0, o1), (i0, i1)):
        ch = 'H' if a == o0 else 'G'
        for x in (a, b):
            for y in (a, b):
                s.line((x, y, a), (x, y, b), ch)
                s.line((x, a, y), (x, b, y), ch)
                s.line((a, x, y), (b, x, y), ch)
    for x in (0, 1):
        for y in (0, 1):
            for z in (0, 1):
                s.line((o0 + x * 10, o0 + y * 10, o0 + z * 10), (i0 + x * 4, i0 + y * 4, i0 + z * 4), 'U' if y == 0 else 'H')
                s.put(o0 + x * 10, o0 + y * 10, o0 + z * 10, 's')        # stabilisers hold the outer corners
                s.put(i0 + x * 4, i0 + y * 4, i0 + z * 4, 'F')            # emitters on the inner corners
    s.air_box(i0 + 1, i0 + 1, i0 + 1, i1 - 1, i1 - 1, i1 - 1)
    s.box(3, 0, 3, 7, 0, 7, 'K', force=False)
    s.put(5, 0, 5, 'i')
    s.controller(5, 0, 0)
    s.center = (5, 5, 5)
    return s


def multiverse_rift():
    s = Shape(13, 13, 7)
    for x0 in (1, 10):
        for y in range(0, 12):
            w = 1 if y > 3 else 2
            s.box(x0 - (w - 1), y, 2, x0 + 1, y, 4, 'R')
        s.box(x0, 1, 2, x0 + 1, 1, 4, 'y')                # spacetime anchors at the pylon feet
        for y in (4, 7, 10):
            s.put(x0, y, 3, 's')                           # stabilisers up the pylons
    for x in range(1, 12):
        y = 11 + round(1.2 * math.sin(math.pi * (x - 1) / 10))
        s.box(x, y - 1, 3, x, y, 3, 'R')
        if x % 3 == 0:
            s.put(x, y - 1, 3, 's')
    s.box(0, 0, 1, 12, 0, 5, 'H', force=False)
    s.put(6, 0, 3, 'F')
    s.put(6, 11, 3, 'F')
    s.air_box(3, 1, 2, 9, 10, 4)
    s.ports('H', 'Y', 4)
    s.controller(1, 2, 2)
    s.center = (6, 5.5, 3)
    return s


def universe_forge():
    s = Shape(13, 15, 13)
    c, cy = 6, 8
    for y, r in ((0, 6), (1, 5), (2, 4)):
        s.disc(c, c, y, r, 'X')
    for (x, z), a in ring_cells(c, c, 5):
        if int(a / (math.pi / 3)) != int((a + 0.2) / (math.pi / 3)):
            s.put(x, 1, z, 'y')                            # six anchors on the pedestal
    for axis in ('x', 'y', 'z'):
        s.great_circle(c, cy, c, 5.6, 'X', axis)
    s.great_circle(c, cy, c, 5.6, 'K', 'y')
    s.great_circle(c, cy, c, 4.2, 'G', 'y')
    for p in ((c, cy + 5.6, c), (c, cy - 5.6, c), (c + 5.6, cy, c), (c - 5.6, cy, c), (c, cy, c + 5.6), (c, cy, c - 5.6)):
        s.put(*p, 's')                                     # stabilisers where the rings cross
    s.column(c, c, 3, 3, 'F')
    s.put(c, 14, c, 'F')
    s.column(c, c, 13, 13, 'X')
    s.sphere_air(c, cy, c, 3.6)
    s.ports('X', 'Z', 4, y=0)
    s.controller(c, 1)
    s.center = (c, cy, c)
    return s


def matrioshka():
    s = Shape(13, 13, 13)
    c = 6
    lattice = lambda x, y, z: (x + y + z) % 2 == 0
    s.sphere(c, c, c, 5.8, 'Q', shell=0.55, filter=lattice)
    s.sphere(c, c, c, 5.8, 'n', shell=0.55, filter=lambda x, y, z: (x + y + z) % 6 == 0)    # error-correction nodes
    s.sphere(c, c, c, 5.8, 'T', shell=0.55, filter=lambda x, y, z: abs(y - c) <= 0)
    s.sphere(c, c, c, 3.6, 'G', shell=0.5, filter=lambda x, y, z: (x + z) % 2 == 1)
    s.sphere(c, c, c, 3.6, 'i', shell=0.5, filter=lambda x, y, z: (x + z) % 4 == 0 and (x + z) % 2 == 0)
    s.put(c, c, c, 'F')
    s.disc(c, c, 0, 3, 'T')
    s.ports('T', 'W', 4)
    s.controller(c, c)
    s.center = (c, c, c)
    return s


def dyson():
    s = Shape(15, 15, 15)
    c = 7
    for axis in ('x', 'y', 'z'):
        s.great_circle(c, c, c, 6.5, 'T', axis)
    s.great_circle(c, c, c, 6.5, '1', 'y')               # equatorial stellar containment belt
    for a in range(4):
        ang = math.pi / 4 + a * math.pi / 2
        for i in range(120):
            t = 2 * math.pi * i / 120
            x = c + math.cos(t) * 5.2 * math.cos(ang)
            z = c + math.cos(t) * 5.2 * math.sin(ang)
            y = c + math.sin(t) * 5.2
            if a % 2 == 0:
                s.put(x, y, z, 'G', force=False)
    s.put(c, c, c, 'F')
    for p in ((c, c + 6.5, c), (c, c - 6.5, c)):
        s.put(*p, 'F')
    for dx, dz in ((-4, -4), (4, -4), (0, 5)):
        s.line((c + dx, 0, c + dz), (c + dx * 0.6, 2.5, c + dz * 0.6), 'T')
    s.box(c - 1, 0, c - 1, c + 1, 0, c + 1, 'T')
    s.ports('T', 'W', 3)
    s.controller(c, 1, c - 1)
    s.center = (c, c, c)
    return s


def annihilation():
    s = Shape(9, 11, 9)
    c = 4
    for y in range(11):
        r = 3.6 if 1 <= y <= 9 else 2.6
        ch = 'K' if y in (2, 5, 8) else ('G' if y in (4, 6) else ('i' if y in (3, 7) else 'M'))
        s.disc(c, c, y, r, ch, r_in=r - 1.05 if 1 <= y <= 9 else -1)
    for y in range(1, 10):
        s.disc(c, c, y, 2.5, '.', force=False)
    s.put(c, 0, c, 'F')
    s.put(c, 10, c, 'F')
    for dx, dz in ((-4, 0), (4, 0), (0, 4)):
        s.box(c + dx * 1 - (1 if dx > 0 else 0) if dx else c, 0, c + dz - (1 if dz > 0 else 0) if dz else c, c + dx if dx else c, 3, c + dz if dz else c, 'M', force=False)
        s.put(c + dx, 3, c + dz, 'v')                      # vacuum pumps on the support legs
    s.ports('M', 'N', 4)
    s.controller(c, 3)
    s.center = (c, 5, c)
    return s


def neutron_star_forge():
    s = Shape(11, 13, 11)
    c = 5
    for y in range(0, 3):
        s.disc(c, c, y, 5 - y * 0.7, 'M')
    for y in range(10, 13):
        s.disc(c, c, y, 5 - (12 - y) * 0.7, 'M')
    s.disc(c, c, 2, 3.3, '1')                             # anvil faces of stellar containment plating
    s.disc(c, c, 10, 3.3, '1')
    for dx, dz in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        s.column(c + dx, c + dz, 0, 12, 'K')
        s.put(c + dx, 6, c + dz, 'i')
    s.disc(c, c, 3, 1.6, 'E')
    s.disc(c, c, 9, 1.6, 'E')
    s.put(c, 3, c, 'F')
    s.put(c, 9, c, 'F')
    s.air_box(c - 2, 4, c - 2, c + 2, 8, c + 2)
    s.ports('M', 'N', 4)
    s.controller(c, 1)
    s.center = (c, 6, c)
    return s


def wormhole():
    s = Shape(13, 14, 5)
    c, cy = 6, 7
    s.ring_v(c, cy, 2, 5.5, 'L', thick=0.7)
    s.ring_v(c, cy, 1, 6.2, 'S', thick=0.5)
    s.ring_v(c, cy, 3, 6.2, 'S', thick=0.5)
    for k in range(8):                                     # eight anchors hold the throat open
        a = k * math.pi / 4 + math.pi / 8
        s.put(c + math.cos(a) * 5.5, cy + math.sin(a) * 5.5, 2, 'y')
    for y in range(14):
        for x in range(13):
            if math.hypot(x - c, y - cy) < 4.8:
                s.put(x, y, 2, '.', force=False)
    s.box(1, 0, 0, 11, 0, 4, 'S', force=False)
    for x0 in (1, 11):
        s.box(x0, 1, 1, x0, 3, 3, 'S', force=False)
        s.put(x0, 2, 2, 's')
    for (x, y) in ((c - 6, cy), (c + 6, cy)):
        s.put(x, y, 2, 'I')
    s.controller(c, 0, 0)
    s.center = (c, cy, 2)
    return s


def causality_loop():
    s = Shape(15, 6, 9)
    cx, cz = 7, 4
    s.box(0, 0, 0, 14, 0, 8, 'O')
    track = []
    for i in range(160):
        t = 2 * math.pi * i / 160
        d = 1 + math.sin(t) ** 2
        x = cx + 6.2 * math.cos(t) / d
        z = cz + 6.2 * math.sin(t) * math.cos(t) / d * 1.1
        s.put(x, 1, z, 'K')
        s.put(x, 2, z, 'G')
        track.append((int(round(x)), int(round(z))))
    for (x, z) in track:                                   # causal shielding walls along the outside of the loop
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, z + dz) not in track and 0 <= x + dx < 15 and 0 <= z + dz < 9 and s.get(x + dx, 1, z + dz) is None:
                if (x + dx + z + dz) % 3 == 0:
                    s.put(x + dx, 1, z + dz, 'z')
    for x in (3, 11):
        s.column(x, cz, 1, 5, 'O')
        s.put(x, 3, cz, 'z')
        s.put(x, 5, cz, 'F')
    s.ports('O', 'J', 4)
    s.controller(cx, 1, 0)
    s.center = (cx, 3, cz)
    return s


def dark_energy_siphon():
    s = Shape(11, 15, 11)
    c = 5
    s.disc(c, c, 0, 4, 'E')
    for dx, dz in ((-3, 0), (3, 0), (0, -3), (0, 3)):
        s.put(c + dx, 1, c + dz, 'y')                     # anchors brace the mast against the expansion
    s.column(c, c, 1, 13, 'E')
    for y, r in ((3, 2.0), (6, 3.2), (9, 4.4), (12, 5.2)):
        s.disc(c, c, y, r, 'F' if y == 12 else 'G', r_in=r - 0.9)
        for a in range(4):
            ang = a * math.pi / 2 + y * 0.2
            s.line((c, y, c), (c + math.cos(ang) * r, y, c + math.sin(ang) * r), 'E')
        s.put(c, y, c, 's')
    s.put(c, 14, c, 'F')
    s.ports('E', 'V', 4)
    s.controller(c, 1)
    s.center = (c, 13.5, c)
    return s


def reality_compiler():
    s = Shape(13, 11, 13)
    c = 6
    for y in range(0, 6):
        r = 6 - y
        s.box(c - r, y, c - r, c + r, y, c + r, 'A' if y % 2 == 0 else 'Q', hollow=y > 0)
    for dx, dz in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        s.column(c + dx, c + dz, 1, 8, 'Q')
        for y in (2, 5, 8):
            s.put(c + dx, y, c + dz, 'n')                 # error-correction nodes up the data pylons
        s.put(c + dx, 9, c + dz, 'G')
    s.box(c - 1, 6, c - 1, c + 1, 6, c + 1, 'A')
    s.put(c, 6, c, 'F')
    s.air_box(c - 2, 7, c - 2, c + 2, 10, c + 2)
    s.ports('A', 'D', 4)
    s.controller(c, 1, 1)
    s.center = (c, 8.5, c)
    return s


def breach():
    s = Shape(11, 10, 11)
    c = 5
    s.disc(c, c, 0, 5, 't')
    s.disc(c, c, 0, 2.5, 'A')
    s.put(c, 0, c, 'F')
    for k, (axis, r) in enumerate((('y', 4.6), ('x', 4.4), ('z', 4.2))):
        n = 90
        for i in range(n):
            if (i // 9) % 3 == 2:
                continue
            a = 2 * math.pi * i / n
            u, v = math.cos(a) * r, math.sin(a) * r
            p = (c + u, 4.5, c + v) if axis == 'y' else ((c, 4.5 + u, c + v) if axis == 'x' else (c + u, 4.5 + v, c))
            if p[1] >= 1:
                s.put(*p, 'K' if k == 0 else ('s' if i % 9 == 4 else 't'), force=False)
    for dx, dz in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        s.column(c + dx, c + dz, 1, 3, 't')
        s.put(c + dx, 1, c + dz, 'y')
    s.air_box(c - 2, 1, 0, c + 2, 3, c + 2)
    s.ports('t', 'u', 4)
    s.controller(c, 5, 0) if s.get(c, 5, 0) is None else None
    if s.ctrl is None:
        s.v[(c, 6, 0)] = 'C'
        s.ctrl = (c, 6, 0)
    s.center = (c, 2, c)
    return s


def omnilex():
    s = Shape(13, 15, 13)
    c = 6
    s.disc(c, c, 0, 6, 'o')
    s.disc(c, c, 1, 6, 'o', r_in=4.6)
    s.disc(c, c, 1, 4.6, 'G')
    for (x, z), a in ring_cells(c, c, 5.4):
        if int(a / (math.pi / 4)) % 2 == 0:
            s.put(x, 1, z, 'y')                            # anchors in the rim of the Ocean pool
    s.column(c, c, 1, 8, 'h')
    for a in range(5):
        ang = a * 2 * math.pi / 5
        tip = (c + math.cos(ang) * 4.5, 12, c + math.sin(ang) * 4.5)
        s.line((c, 8, c), tip, 'h')
        s.put(*tip, 'F')
    s.disc(c, c, 13, 5.6, 'A', r_in=4.4)
    for (x, z), a in ring_cells(c, c, 5):
        if int(a / (math.pi / 5)) % 2 == 0:
            s.put(x, 13, z, 's')                           # stabilisers in the crown ring
    s.disc(c, c, 14, 2, 'o')
    s.ports('o', 'p', 4)
    s.controller(c, 1, 0)
    s.center = (c, 6, c)
    return s


def assembly_line():
    s = Shape(7, 5, 19)
    for z in range(19):
        s.box(0, 0, z, 6, 0, z, 'a')
        s.box(1, 1, z, 5, 1, z, 'K' if z % 2 == 0 else 'a')
        s.put(0, 1, z, 'a')
        s.put(6, 1, z, 'a')
        if z % 3 == 1:
            s.box(0, 2, z, 0, 4, z, 'a')
            s.box(6, 2, z, 6, 4, z, 'a')
            s.box(1, 4, z, 5, 4, z, 'a')
            s.put(3, 4, z, 'n')                            # each gantry has its own control core
            s.put(3, 3, z, 'F')
        else:
            s.put(0, 2, z, 'G')
            s.put(6, 2, z, 'G')
    s.air_box(1, 2, 0, 5, 3, 18)
    for z in (0, 6, 12, 18):
        s.put(0, 0, z, 'b')
        s.put(6, 0, z, 'b')
    s.v[(3, 1, 0)] = 'C'
    s.ctrl = (3, 1, 0)
    s.center = (3, 2.5, 9)
    return s


def plasma_crucible():
    s = Shape(11, 10, 11)
    c = 5
    s.disc(c, c, 0, 4, 'c')
    s.disc(c, c, 0, 3.4, 'i', r_in=2.4)                   # cryo ring under the crucible
    for y in range(1, 8):
        ch = 'k' if y in (2, 3, 5, 6) else 'c'
        s.disc(c, c, y, 3.3, ch, r_in=2.2)
    s.disc(c, c, 8, 3.6, 'c', r_in=1.2)
    s.disc(c, c, 9, 1.4, 'c')
    for y in range(1, 9):
        s.disc(c, c, y, 2.2, '.', force=False)
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):     # buttresses with plasma injectors firing inward
        for k in (4, 5):
            s.column(c + dx * k, c + dz * k, 0, 3, 'c')
        s.put(c + dx * 4, 4, c + dz * 4, 'j')
        s.put(c + dx * 3, 4, c + dz * 3, 'j')
    s.put(c, 0, c, 'F')
    s.ports('c', 'd', 4)
    s.v[(c, 1, c - 3)] = 'C'
    s.ctrl = (c, 1, c - 3)
    s.center = (c, 4.5, c)
    return s


def fractionation():
    s = Shape(9, 17, 9)
    c = 4
    s.disc(c, c, 0, 4, 'f')
    for y in range(1, 17):
        r = 2.4 if y < 15 else 1.5
        s.disc(c, c, y, r, 'f' if y % 3 else 'G', r_in=r - 1.0)
        if y % 4 == 0 and y < 15:
            s.disc(c, c, y, 4, 'i' if y % 8 == 0 else 'f', r_in=2.5)   # alternating cryo condensers on the trays
    s.put(c, 16, c, 'F')
    s.put(c + 2, 15, c, 'v')
    s.put(c - 2, 15, c, 'v')
    for y in range(1, 16):
        s.disc(c, c, y, 1.3, '.', force=False)
    s.ports('f', 'g', 4)
    s.v[(c, 1, c - 2)] = 'C'
    s.ctrl = (c, 1, c - 2)
    s.center = (c, 8, c)
    return s


def plasma_forge():
    """Transcendent Plasma Forge: a spherical chamber of stellar containment plating and field glass, wrapped in two
    crossed toroidal coil packs, fed by six plasma injector lines (four horizontal, two vertical) that each end in a
    field emitter. Cryo manifolds feed the coils; a stepped crucible-casing platform carries it all."""
    s = Shape(17, 17, 17)
    c = 8
    for y, r in ((0, 8), (1, 7), (2, 6)):                 # stepped platform
        s.disc(c, c, y, r, 'c')
    s.disc(c, c, 1, 7, 'i', r_in=6.2)                     # cryo manifold ring
    cy = 9
    s.sphere(c, cy, c, 4.4, 'G', shell=0.55)
    s.sphere(c, cy, c, 4.4, '1', shell=0.55, filter=lambda x, y, z: (x + y + z) % 2 == 0)   # chequered plating
    for axis in ('x', 'z'):                                # two crossed toroidal coil packs
        s.great_circle(c, cy, c, 6.0, '3', axis)
        s.great_circle(c, cy, c, 6.6, '3', axis)
    s.great_circle(c, cy, c, 6.3, '3', 'y')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):     # horizontal injector lines
        for k in range(5, 9):
            s.put(c + dx * k, cy, c + dz * k, 'j')
        s.put(c + dx * 8, cy + 1, c + dz * 8, 'F')
        s.put(c + dx * 8, cy - 1, c + dz * 8, 'c')
        s.column(c + dx * 8, c + dz * 8, 3, cy - 2, 'c')   # injector support pylons
        s.put(c + dx * 8, 3, c + dz * 8, 'i')
    for k in range(5, 8):                                  # vertical injectors
        s.put(c, cy + k, c, 'j')
    s.put(c, 16, c, 'F')
    for k in range(3, 5):
        s.put(c, cy - k - 0, c, 'j')
    s.put(c, 3, c, 'F')
    s.sphere_air(c, cy, c, 3.8)
    s.ports('c', 'd', 6, y=0)
    s.v[(c, 2, c - 6)] = 'C'
    s.ctrl = (c, 2, c - 6)
    s.center = (c, cy, c)
    return s


def eye_of_the_horizon():
    """Eye of the Event Horizon: a captive black hole held in a cage. Graviton lens arrays form an inner lattice
    shell; three orthogonal rings of high-tier coils circle it; six spacetime anchor pylons pin it at the axis
    points; dimensional stabilisers sit where the rings cross, Hawking collectors at the poles, field emitters on
    the diagonals - all on an event-horizon pedestal."""
    s = Shape(21, 21, 21)
    c = 10
    for y, r in ((0, 9), (1, 7), (2, 5)):                 # pedestal
        s.disc(c, c, y, r, 'E')
    s.disc(c, c, 1, 9, 'i', r_in=7.2)
    cy = 11
    s.sphere(c, cy, c, 5.2, 'l', shell=0.5, filter=lambda x, y, z: (x + y + z) % 3 == 0)   # lens lattice
    s.sphere(c, cy, c, 5.2, 'G', shell=0.5, filter=lambda x, y, z: (x + y + z) % 3 == 1)
    for axis, r in (('x', 8.0), ('y', 8.0), ('z', 8.0)):
        s.great_circle(c, cy, c, r, '4', axis)
        s.great_circle(c, cy, c, r + 0.7, 'H', axis)
    for p in ((c, cy + 8, c), (c, cy - 8, c), (c + 8, cy, c), (c - 8, cy, c), (c, cy, c + 8), (c, cy, c - 8)):
        s.put(*p, 's')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):     # four horizontal anchor pylons
        for k in range(9, 11):
            s.put(c + dx * k, cy, c + dz * k, 'y')
        s.column(c + dx * 10, c + dz * 10, 2, cy - 1, 'H')
    s.column(c, c, cy + 9, cy + 9, 'y')
    s.put(c, cy + 6, c, 'w')
    s.put(c, cy - 6, c, 'w')
    s.column(c, c, 3, cy - 7, 'y')
    for sx in (-1, 1):                                     # diagonal emitters
        for sz in (-1, 1):
            for sy in (-1, 1):
                d = 5.6 / math.sqrt(3) * 1.6
                s.put(c + sx * d, cy + sy * d, c + sz * d, 'F')
    s.sphere_air(c, cy, c, 4.4)
    s.ports('E', 'V', 6, y=0)
    s.v[(c, 2, c - 5)] = 'C'
    s.ctrl = (c, 2, c - 5)
    s.center = (c, cy, c)
    return s


def concept_compressor():
    """Conceptual Singularity Compressor: a sphere of conceptual casing pierced by nine converging injector spokes
    (eight from the corners of a cube, one from above), each a line of lens arrays ending in a field emitter, so
    that every field's stream meets at one point. An equatorial coil ring and stabilisers hold the shell together."""
    s = Shape(13, 14, 13)
    c, cy = 6, 7
    s.disc(c, c, 0, 5.5, '0')
    s.disc(c, c, 1, 4.2, '0')
    for dx, dz in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        s.put(c + dx, 1, c + dz, 'y')
    s.sphere(c, cy, c, 5.2, '0', shell=0.55)
    s.sphere(c, cy, c, 5.2, 'G', shell=0.55, filter=lambda x, y, z: abs(y - cy) == 1)
    s.great_circle(c, cy, c, 5.9, '3', 'y')
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                for k in range(3, 7):
                    p = (c + sx * k * 0.72, cy + sy * k * 0.72, c + sz * k * 0.72)
                    s.put(*p, 'F' if k == 6 else 'l')
                s.put(c + sx * 2.9, cy + sy * 2.9, c + sz * 2.9, 's')
    for y in range(cy + 3, 14):
        s.put(c, y, c, 'F' if y == 13 else 'l')
    s.sphere_air(c, cy, c, 2.2)
    s.ports('0', '9', 6, y=0)
    s.v[(c, 1, c - 5)] = 'C'
    s.ctrl = (c, 1, c - 5)
    s.center = (c, cy, c)
    return s


BUILDERS = {
    'quark_gluon_collider': collider, 'dark_matter_collector': dark_matter_collector, 'kugelblitz_chamber': kugelblitz,
    'penrose_ergosphere': penrose, 'white_hole_emitter': white_hole, 'tesseract_assembler': tesseract,
    'multiverse_rift': multiverse_rift, 'universe_forge': universe_forge, 'matrioshka_brain': matrioshka,
    'dyson_array': dyson, 'annihilation_reactor': annihilation, 'neutron_star_forge': neutron_star_forge,
    'wormhole_projector': wormhole, 'causality_loop_engine': causality_loop, 'dark_energy_siphon': dark_energy_siphon,
    'reality_compiler': reality_compiler, 'axiomatic_breach_engine': breach, 'omnilex_engine': omnilex,
    'femtoscale_assembly_line': assembly_line, 'stellar_plasma_crucible': plasma_crucible, 'fractionation_spire': fractionation,
    'synchrotron': synchrotron, 'transcendent_plasma_forge': plasma_forge, 'eye_of_the_horizon': eye_of_the_horizon,
    'concept_compressor': concept_compressor,
}


def _outside(s):
    """Cells reachable from outside the structure through non-solid cells (air or 'anything')."""
    import collections
    solid = {p for p, c in s.v.items() if c not in ' .'}
    start = (-1, -1, -1)
    seen, q = {start}, collections.deque([start])
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in seen or n in solid or not (-1 <= n[0] <= s.w and -1 <= n[1] <= s.h and -1 <= n[2] <= s.d):
                continue
            seen.add(n)
            q.append(n)
    return seen


def reachable_controller(s):
    """A controller must face open air you can walk up to (it faces -Z). If the builder buried it, move it to the
    nearest exposed casing on the structure's front, lowest layers first, and put casing back where it was."""
    out = _outside(s)
    x, y, z = s.ctrl
    if (x, y, z - 1) in out:
        return
    counts = {}
    for p, c in s.v.items():
        if c not in ' .C':
            counts[c] = counts.get(c, 0) + 1
    casing = max(counts, key=counts.get) if counts else 'S'
    neigh = [s.v.get((x + dx, y, z + dz)) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
    neigh = [c for c in neigh if c and c not in ' .C']
    s.v[(x, y, z)] = neigh[0] if neigh else casing
    cx = s.center[0]
    cands = [(c != casing, p[1], abs(p[0] - cx), p[2], p) for p, c in s.v.items()
             if c not in ' .C' and (p[0], p[1], p[2] - 1) in out]
    cands.sort()
    p = cands[0][4]
    s.v[p] = 'C'
    s.ctrl = p


def build_all():
    out = {}
    for k, f in BUILDERS.items():
        s = f()
        assert s.ctrl is not None, k
        reachable_controller(s)
        out[k] = dict(layers=s.layers(), center=list(s.center), blocks=s.block_count(), size=(s.w, s.h, s.d), repeat=s.repeat)
    return out


if __name__ == '__main__':
    for k, v in build_all().items():
        chars = {}
        for layer in v['layers']:
            for row in layer:
                for ch in row:
                    if ch not in ' .':
                        chars[ch] = chars.get(ch, 0) + 1
        print(f"{k:28s} {v['size']}  {v['blocks']:5d} blocks  {len(chars)} kinds  {''.join(sorted(chars))}")
