"""
1.7.1: the endgame structures. Eight reshaped (each keeps at least the blocks of its old build, whose layers go into
"legacy" so the Structure Assembler can still take an old one apart) and four new ones (the Cantor Diagonalizer, Axiom
Distiller, Pleroma Confluence and Convergence Engine, which were single blocks).

Pattern coordinates: x to the right, y up, z to the back; the controller is in the front face.

Run by round171.py; alone:  python3 tools/r171_shapes.py     (prints each structure's block counts)
"""
import collections, json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tsgen as T
from tsgen import ts, dump, A

HERE = os.path.dirname(os.path.abspath(__file__))
MBD = os.path.join(A, 'multiblocks')
SNAP = os.path.join(HERE, 'r171_legacy')          # the 1.7.0 layouts, kept so this can be run again
TAU = math.tau
EMIT = ['field_emitter', 'graviton_field_emitter', 'axiomatic_field_emitter']
COILS = ['superconducting_coil_block', 'chronosteel_coil_block', 'spacetime_alloy_coil_block', 'cosmic_neutronium_coil_block', 'transfinite_coil_block']


class Vox:
    """A sparse block model: cells[(x, y, z)] = character."""

    def __init__(self):
        self.c = {}

    def put(self, x, y, z, ch, over=True):
        k = (int(round(x)), int(round(y)), int(round(z)))
        if over or k not in self.c:
            self.c[k] = ch

    def clear(self, x, y, z):
        self.c.pop((int(round(x)), int(round(y)), int(round(z))), None)

    def fill(self, x0, x1, y0, y1, z0, z1, fn, over=True):
        """fn(x, y, z) -> character or None, for every cell of the box (ends included)."""
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                for x in range(x0, x1 + 1):
                    ch = fn(x, y, z)
                    if ch:
                        self.put(x, y, z, ch, over)

    def box(self, x0, x1, y0, y1, z0, z1, ch, hollow=False, over=True):
        def f(x, y, z):
            if hollow and x0 < x < x1 and y0 < y < y1 and z0 < z < z1:
                return None
            return ch
        self.fill(x0, x1, y0, y1, z0, z1, f, over)

    def line(self, a, b, ch, over=True):
        n = int(max(abs(b[i] - a[i]) for i in range(3))) + 1
        for i in range(n + 1):
            t = i / max(1, n)
            self.put(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t, ch, over)

    def count(self):
        return collections.Counter(self.c.values())

    def desk(self, cx, y, port, n=3, base=None):
        """The controller in the front face at (cx, y), ports either side of it, on a ledge one block out from the
        frontmost block of that row."""
        zs = [z for (x, yy, z) in self.c if abs(x - cx) <= n + 1 and yy == y]
        z = (min(zs) if zs else 0) - 1
        for dx in range(-n, n + 1):
            self.put(cx + dx, y, z, 'C' if dx == 0 else port)
            if base and y > 0:
                self.put(cx + dx, y - 1, z, base, over=False)
        return z

    def layers(self):
        xs = [k[0] for k in self.c]
        ys = [k[1] for k in self.c]
        zs = [k[2] for k in self.c]
        ox, oy, oz = min(xs), min(ys), min(zs)
        w, h, d = max(xs) - ox + 1, max(ys) - oy + 1, max(zs) - oz + 1
        g = [[[' '] * w for _ in range(d)] for _ in range(h)]
        for (x, y, z), ch in self.c.items():
            g[y - oy][z - oz][x - ox] = ch
        return [[''.join(r) for r in layer] for layer in g], (ox, oy, oz), (w, h, d)


def hsh(*a):
    """A steady pseudo-random number in [0, 1) from integers."""
    n = 0x9E3779B9
    for v in a:
        n = (n ^ (int(v) + 0x7F4A7C15 + (n << 6) + (n >> 2))) & 0xFFFFFFFF
        n = (n * 0x85EBCA6B) & 0xFFFFFFFF
        n ^= n >> 13
    return (n & 0xFFFFFF) / float(0x1000000)


def old(mid):
    """The 1.7.0 layout of a reshaped structure (saved the first time this runs)."""
    os.makedirs(SNAP, exist_ok=True)
    p = os.path.join(SNAP, mid + '.json')
    if not os.path.exists(p):
        json.dump(json.load(open(os.path.join(MBD, mid + '.json'), encoding='utf-8')), open(p, 'w'))
    return json.load(open(p))


COUNTS = {}
SHAPES = {}


def write(mid, v, key, extra, minimum=None, reshaped=True, fx_points=None):
    """Writes a structure. extra's coordinates (effect_center, fx points and 'at') are given in model space and shifted here."""
    layers, (ox, oy, oz), dims = v.layers()
    cnt = collections.Counter(ch for layer in layers for row in layer for ch in row if ch != ' ')

    def sh(p):
        return [round(p[0] - ox, 2), round(p[1] - oy, 2), round(p[2] - oz, 2)]
    d = {'controller_block': ts(mid), 'controller': 'C', 'layers': layers}
    prev = old(mid) if reshaped else None
    k = dict(prev['key']) if prev else {}
    for ch, blocks in key.items():
        k[ch] = [ts(b) for b in blocks]
    d['key'] = k
    d['tiers'] = dict(prev.get('tiers', {})) if prev else {}
    for kk, vv in extra.items():
        if kk == 'effect_center':
            vv = sh(vv)
        elif kk == 'fx':
            out = []
            for e in vv:
                e = dict(e)
                if 'points' in e:
                    e['points'] = [sh(p) for p in e['points']]
                if 'at' in e:
                    e['at'] = sh(e['at'])
                for f in ('start_back', 'end_back'):
                    if f in e:
                        e[f] = round(e[f] - oz, 2)
                if 'up' in e:
                    e['up'] = round(e['up'] - oy, 2)
                if 'floor' in e:
                    e['floor'] = round(e['floor'] - oy, 2)
                out.append(e)
            vv = out
        elif kk == 'tiers':
            d['tiers'].update(vv)
            continue
        d[kk] = vv
    if prev:
        d['legacy'] = prev.get('legacy', []) + [prev['layers']]
        d['scaled_95'] = True
        # at least the blocks of the old build, block by block
        was = collections.Counter()
        for layer in prev['layers']:
            for row in layer:
                for ch in row:
                    if ch in prev['key']:
                        was[prev['key'][ch][0]] += 1
        now = collections.Counter()
        for ch, n in cnt.items():
            if ch in k:
                now[k[ch][0]] += n
        short = {b.split(':')[1]: (now[b], n) for b, n in was.items() if now[b] < n}
        if short:
            raise SystemExit('%s has fewer blocks than its old build: %s' % (mid, short))
    unknown = [ch for ch in cnt if ch not in k and ch != 'C']
    if unknown:
        raise SystemExit('%s: characters without a key: %s' % (mid, unknown))
    # nothing in front of the controller
    for y, layer in enumerate(layers):
        for z, row in enumerate(layer):
            if 'C' in row:
                x = row.index('C')
                if any(layers[y][zz][x] != ' ' for zz in range(z)):
                    raise SystemExit('%s: the controller is not in the front face' % mid)
    dump(os.path.join(MBD, mid + '.json'), d)
    n = sum(cnt.values())
    COUNTS[mid] = n
    T.MB_COUNTS[mid] = n
    by = collections.Counter()
    for ch, c in cnt.items():
        by[k[ch][0].split(':')[1] if ch in k else 'controller'] += c
    SHAPES[mid] = dict(dims=dims, total=n, blocks=dict(by))
    print('%-26s %6d blocks  %3d x %3d x %3d (w h d)  %s' % (mid, n, dims[0], dims[1], dims[2], dict(cnt)))
    return n


def ring(cx, cz, r, n):
    return [(cx + math.cos(i * TAU / n) * r, cz + math.sin(i * TAU / n) * r) for i in range(n)]


# ====================================================================== Stellar Engine
def stellar_engine():
    """A Shkadov thruster: a great mirror dish behind the star, held on ribs from a spine; a ring of Caplan jets ahead
    of the star turns as it burns, and the thrust beam leaves through the ring."""
    v = Vox()
    R = 25                                   # the mirror: a cap of a sphere round the star, at the back
    for x in range(-R - 1, R + 2):
        for y in range(-R - 1, R + 2):
            for z in range(8, R + 2):
                rr = math.sqrt(x * x + y * y + z * z)
                if R - 0.5 <= rr < R + 0.5:
                    v.put(x, y, z, 'S')
                elif R + 0.5 <= rr < R + 1.5 and (abs(x) <= 0 or abs(y) <= 0 or abs(abs(x) - abs(y)) <= 0 or abs(rr * 0 + z - 18) <= 0):
                    v.put(x, y, z, 'D')    # ribs and a hoop behind the mirror
    for z in range(-20, R + 3):              # the spine under the star, from the desk to the mirror's foot
        for x in (-1, 0, 1):
            v.put(x, -R - 2, z, 'D')
        v.put(0, -R - 3, z, 'D')
    for y in range(-R - 2, -R + 6):          # the mirror's foot
        for x in range(-2, 3):
            v.put(x, y, R + 2, 'D', over=False)
    for k, (x, y) in enumerate(ring(0, 0, 9, 24)):   # the jet ring, ahead of the star
        v.put(x, y, -12, 'J')
    for k, (x, y) in enumerate(ring(0, 0, 11, 32)):
        v.put(x, y, -12, 'j')                 # its hull hoop (turns with it)
    for a in range(4):                       # struts from the hoop to a fixed outer ring
        for r in range(12, 15):
            v.put(math.cos(a * TAU / 4 + TAU / 8) * r, math.sin(a * TAU / 4 + TAU / 8) * r, -12, 'D')
    for (x, y) in ring(0, 0, 15, 44):
        v.put(x, y, -12, 'D')
        v.put(x, y, -11, 'D')
    for y in range(-R - 2, -14):             # the pylon that carries the outer ring
        for x in (-1, 0, 1):
            v.put(x, y, -12, 'D')
            v.put(x, y, -11, 'D', over=False)
    v.box(-4, 4, -R - 3, -R - 3, -24, -19, 'D')   # the desk's platform
    v.box(-4, 4, -R - 2, -R - 2, -24, -21, 'D')
    v.desk(0, -R - 2, 'P', n=2)
    write('stellar_engine', v, {'j': ['habitat_hull']},
          {'effect_center': [0, 0, 0], 'effect_scale': 5.0,
           'moving': [{'id': 'jets', 'chars': 'Jj', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 8,
                       'motion': [{'type': 'spin', 'axis': 'back', 'deg_per_sec': 12}]}],
           'fx': [{'type': 'star', 'radius': 3.6, 'color': '#FFD070', 'at': [0, 0, 0]},
                  {'type': 'glow', 'chars': 'J', 'color': '#FF9040', 'hot': '#FFF0C0'},
                  {'type': 'beam', 'start_back': -6, 'end_back': -60, 'up': 0, 'width': 0.9, 'color': '#FFB060'}]})


# ====================================================================== Dyson Array
def dyson_array():
    """A sphere of collector panels round the star, open between its ribs; an equatorial belt; eight arms out from the
    belt to dishes that beam the power in."""
    v = Vox()
    R = 22
    for x in range(-R - 1, R + 2):
        for y in range(-R - 1, R + 2):
            for z in range(-R - 1, R + 2):
                rr = math.sqrt(x * x + y * y + z * z)
                if not (R - 0.5 <= rr < R + 0.5):
                    continue
                lon = math.atan2(z, x)
                lat = math.asin(max(-1, min(1, y / rr)))
                m = (lon / (TAU / 16)) % 1.0
                p = ((lat + math.pi / 2) / (math.pi / 10)) % 1.0
                rib = min(m, 1 - m) * (TAU / 16) * rr * math.cos(lat) < 0.6 or min(p, 1 - p) * (math.pi / 10) * rr < 0.6
                if rib:
                    v.put(x, y, z, 'T')
                elif (int(lon / (TAU / 16) + 16) + int((lat + math.pi / 2) / (math.pi / 10))) % 2 == 0 and abs(lat) < 1.25:
                    v.put(x, y, z, '1')       # panels on every other field: the star shows between them
    for (x, z) in ring(0, 0, R + 4, 200):    # the belt: two hoops with glass between
        for y in (-2, 2):
            v.put(x, y, z, 'T')
        for y in (-1, 0, 1):
            v.put(x, y, z, 'G' if y == 0 else 'T', over=False)
    for (x, z) in ring(0, 0, R + 5, 220):
        for y in (-2, -1, 0, 1, 2):
            v.put(x, y, z, 'T', over=False)
    pts = []
    for a in range(8):                        # arms and dishes
        ang = a * TAU / 8 + TAU / 16
        ca, sa = math.cos(ang), math.sin(ang)
        up = 5 if a % 2 == 0 else -5
        for r in range(R + 1, R + 4):         # spokes from the sphere to the belt
            v.put(ca * r, 0, sa * r, 'T', over=False)
        tip = (ca * 46, up, sa * 46)
        v.line((ca * (R + 6), 0, sa * (R + 6)), tip, 'T')
        v.line((ca * (R + 6), 1, sa * (R + 6)), (tip[0], tip[1] + 1, tip[2]), 'T', over=False)
        for i in range(-4, 5):                # the dish: a square of plates across the arm, its emitter at the middle
            for j in range(-4, 5):
                if i * i + j * j <= 18:
                    v.put(tip[0] - sa * i, tip[1] + j, tip[2] + ca * i, 'd')
        v.put(tip[0], tip[1], tip[2], 'F')
        pts.append([ca * 44, up, sa * 44])
    for y in range(-R - 12, -R + 1):          # the mast under the sphere
        for x in range(-2, 3):
            for z in range(-2, 3):
                if abs(x) == 2 or abs(z) == 2:
                    v.put(x, y, z, 'T', over=False)
    v.box(-7, 7, -R - 13, -R - 13, -9, 7, 'T')
    v.desk(0, -R - 12, 'W', n=2, base='T')
    write('dyson_array', v, {'d': ['stellar_containment_plate']},
          {'effect_center': [0, 0, 0], 'effect_scale': 6.0,
           'moving': [{'id': 'dishes', 'chars': 'd', 'split': 'components', 'pivot': 'self', 'ramp_seconds': 4, 'stagger': 0.125,
                       'motion': [{'type': 'oscillate', 'mode': 'rotate', 'axis': 'up', 'amplitude': 7, 'period_seconds': 18}]}],
           'fx': [{'type': 'dyson', 'points': pts, 'radius': 5, 'star': 4.0}]})


# ====================================================================== Matrioshka Brain
def matrioshka_brain():
    """Four nested rings round the star, each a band of a sphere tilted on its own axis like an armillary: frame
    outside, the thinking layers inside. The innermost turns."""
    v = Vox()
    bands = [(14, 4, (0.0, 1.0, 0.0), 1), (22, 6, (0.5, 0.7, 0.5), 2), (30, 7, (-0.6, 0.6, 0.5), 2), (38, 8, (0.3, 0.5, -0.8), 2)]
    for bi, (R, w, n, thick) in enumerate(bands):
        ln = math.sqrt(sum(c * c for c in n))
        n = tuple(c / ln for c in n)
        for x in range(-R - 2, R + 3):
            for y in range(-R - 2, R + 3):
                for z in range(-R - 2, R + 3):
                    rr = math.sqrt(x * x + y * y + z * z)
                    if abs(x * n[0] + y * n[1] + z * n[2]) > w:
                        continue
                    if R + 0.5 <= rr < R + 1.5 and thick == 2:
                        v.put(x, y, z, 'T')
                    elif R - 0.5 <= rr < R + 0.5:
                        if thick == 1:
                            v.put(x, y, z, 'q' if hsh(x, y, z) < 0.5 else 'g')
                        else:
                            h = hsh(x, y, z, bi)
                            edge = abs(x * n[0] + y * n[1] + z * n[2]) > w - 1
                            v.put(x, y, z, 'T' if edge else 'G' if h < 0.44 else 'Q' if h < 0.72 else 'i' if h < 0.85 else 'n')
    for y in range(-52, -37):                 # the mast under the outer ring
        for x in range(-2, 3):
            for z in range(-2, 3):
                if abs(x) == 2 or abs(z) == 2:
                    v.put(x, y, z, 'T', over=False)
    v.box(-8, 8, -53, -53, -10, 8, 'T')
    for (x, z) in ((-8, -10), (8, -10), (-8, 8), (8, 8)):
        v.put(x, -52, z, 'F')
    v.desk(0, -52, 'W', n=2, base='T')
    write('matrioshka_brain', v, {'q': ['computronium_block'], 'g': ['containment_field_glass']},
          {'effect_center': [0, 0, 0], 'effect_scale': 1.86,
           'moving': [{'id': 'inner_ring', 'chars': 'qg', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 8,
                       'motion': [{'type': 'spin', 'axis': 'up', 'deg_per_sec': 6}]}],
           'fx': [{'type': 'matrioshka', 'radii': [14, 26, 38], 'star': 4.0}]})


# ====================================================================== Universe Forge
def universe_forge():
    """A broad disc under a dome of glass that steps in as it rises, coils round every step, and a spire through the
    crown; the new universe swells inside the dome."""
    v = Vox()
    for x in range(-36, 37):
        for z in range(-36, 37):
            r = math.hypot(x, z)
            if r <= 36.4:
                v.put(x, 0, z, 'X')
            if 34.4 < r <= 36.4:
                v.put(x, 1, z, 'X')
    steps = [(32, 1, 9), (26, 10, 18), (20, 19, 25), (13, 26, 31), (6, 32, 37)]
    for si, (R, y0, y1) in enumerate(steps):
        for x in range(-R - 1, R + 2):
            for z in range(-R - 1, R + 2):
                r = math.hypot(x, z)
                if R - 0.5 <= r < R + 0.5:
                    for y in range(y0, y1 + 1):
                        v.put(x, y, z, 'G')
                    v.put(x, y1 + 1, z, 'K')                       # a coil round the step's shoulder
                nxt = steps[si + 1][0] if si + 1 < len(steps) else -1
                if nxt + 0.5 <= r < R - 0.5:
                    v.put(x, y1 + 1, z, 'G')                       # the step's roof
    for (x, z) in ring(0, 0, 22, 140):        # coils set in the floor, round where the universe forms
        v.put(x, 1, z, 'K')
    for (x, z) in ring(0, 0, 11, 70):
        v.put(x, 1, z, 'K')
    for y in range(38, 56):                   # the spire
        for x in (-1, 0, 1):
            for z in (-1, 0, 1):
                if y < 50 or (x == 0 and z == 0):
                    v.put(x, y, z, 'X')
    for (x, z) in ring(0, 0, 3, 8):
        v.put(x, 47, z, 'r')                  # a ring of stabilisers round the spire: it turns
    for (x, z) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        v.put(x, 40, z, 'F')
    for (x, z) in ring(0, 0, 35, 8):          # anchor pylons round the rim
        for y in range(2, 8):
            v.put(x, y, z, 'X')
        v.put(x, 8, z, 'y')
    v.desk(0, 0, 'Z', n=2)
    write('universe_forge', v, {'r': ['dimensional_stabiliser']},
          {'effect_center': [0, 14, 0], 'effect_scale': 4.193,
           'moving': [{'id': 'crown', 'chars': 'r', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 6,
                       'motion': [{'type': 'spin', 'axis': 'up', 'deg_per_sec': 20}]}],
           'fx': [{'type': 'big_bang', 'radius': 18}]})


# ====================================================================== Concept Compressor
def concept_compressor():
    """A press line: nine bays in a row, one for each idea fed in, each with a lens array over a ram that beats down
    on the bed; the bays' beams meet in the middle bay, where the singularity is pressed."""
    v = Vox()
    BAY, W, H = 12, 10, 16                    # bay length along x, half-width in z, height
    n = 9
    x0 = -(n * BAY) // 2
    pts = []
    for x in range(x0, x0 + n * BAY + 1):
        for z in range(-W, W + 1):
            v.put(x, 0, z, '0')               # the bed
            if abs(z) <= 5:
                v.put(x, 1, z, '0')
    for b in range(n + 1):                    # frames between the bays: pylons up the sides, a beam across
        x = x0 + b * BAY
        for y in range(1, H + 1):
            for z in (-W, W):
                v.put(x, y, z, 'y')
        for z in range(-W, W + 1):
            v.put(x, H + 1, z, '0')
            v.put(x, H, z, '0', over=False)
    for b in range(n):
        xa, xb = x0 + b * BAY + 1, x0 + (b + 1) * BAY - 1
        xm = (xa + xb) // 2
        for x in range(xa, xb + 1):
            for z in (-W, W):                 # side rails and glass
                v.put(x, H + 1, z, '0')
                v.put(x, 1, z, '0')
                for y in range(2, H + 1):
                    v.put(x, y, z, 'G' if 3 <= y <= H - 3 else '0')
            for z in range(-W + 1, W):        # the lens array in the roof
                v.put(x, H + 1, z, 'l' if abs(z) <= 5 else '0')
        for x in range(xm - 2, xm + 3):       # the ram and its coil
            for z in range(-2, 3):
                for y in (9, 10, 11):
                    v.put(x, y, z, 'm')
        for (cx, cz) in ring(xm, 0, 4, 28):
            v.put(cx, 12, cz, '3')
        v.put(xm, 2, 0, 'F')                  # the anvil's emitter
        v.put(xm, 12, 0, 's')
        pts.append([xm, 14, 0])
    v.put(0, 2, 0, 's')
    for (cx, cz) in ring(0, 0, 3, 8):
        v.put(cx, 2, cz, 's')
    v.put(0, 3, 0, 'F')
    v.desk(0, 0, '0', n=1)                    # the bed is the port row here: ports are set in it either side
    zf = min(z for (x, y, z) in v.c if y == 0)
    for dx in (-3, -2, 2, 3):
        v.put(dx, 0, zf, 'p')
    write('concept_compressor', v, {'m': ['conceptual_casing'], 'p': ['conceptual_casing', 'interface_port']},
          {'effect_center': [0, 6, 0], 'effect_scale': 4.683,
           'moving': [{'id': 'rams', 'chars': 'm', 'split': 'components', 'pivot': 'self', 'ramp_seconds': 3, 'stagger': 0.11,
                       'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'up', 'amplitude': 2.6, 'period_seconds': 4}]}],
           'fx': [{'type': 'concept', 'points': pts}]})


# ====================================================================== Omnilex Engine
def omnilex_engine():
    """A field of glass spires standing on a solid base of heartwood sheathed in casing: the tallest in the middle,
    six round it, twelve round those. Worlds rise as bubbles from every tip."""
    v = Vox()
    for x in range(-28, 29):
        for z in range(-28, 29):
            r = math.hypot(x, z)
            if r <= 28.4:
                v.put(x, 0, z, 'o')
                for y in (1, 2):
                    v.put(x, y, z, 'h' if r <= 26.4 else 'o')
                if r > 26.4 or (int(r) % 6 == 0):
                    v.put(x, 3, z, 'o')       # rings of casing on the deck
    sp = [(0, 0, 62, 'A')] + [(x, z, 44, 'A') for (x, z) in ring(0, 0, 11, 6)] + [(x, z, 30, None) for (x, z) in ring(0, 0, 22, 12)]
    tips, drips = [], []
    for i, (sx, sz, hgt, core) in enumerate(sp):
        sx, sz = int(round(sx)), int(round(sz))
        for y in range(3, 3 + hgt):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if dx or dz:
                        v.put(sx + dx, y, sz + dz, 'G')
            if core and (i == 0 or y < 3 + 22):
                v.put(sx, y, sz, core)
        top = 3 + hgt
        if i == 0:
            for y in range(top, top + 4):
                v.put(sx, y, sz, 's')
        elif i <= 6:
            for y in range(top, top + 3):
                v.put(sx, y, sz, 's')
            drips.append([sx, top, sz + 2])
        else:
            for y in range(top, top + 7):
                v.put(sx, y, sz, 'y')
        tips.append([sx, top + 4, sz])
    for (x, z) in ring(0, 0, 4, 6):
        v.put(x, 4, z, 'F')
    v.desk(0, 1, 'p', n=2, base='o')
    write('omnilex_engine', v, {},
          {'effect_center': [0, 70, 0], 'effect_scale': 4.76,
           'fx': [{'type': 'bubbles', 'points': tips, 'color': '#80FFE0', 'rise': 14, 'size': 1.4, 'period': 11},
                  {'type': 'drips', 'points': drips, 'color': '#A0FFF0', 'floor': 3, 'period': 4}]})


# ====================================================================== Reality Compiler
def reality_compiler():
    """A tower whose plan changes at every stage: square, octagon, circle, diamond, then an open lantern of eight
    piers where the code is compiled, and a square cap. Bands of computronium at every joint."""
    v = Vox()
    HS = 11

    def inside(kind, s, x, z):
        if kind == 'square':
            return max(abs(x), abs(z)) <= s
        if kind == 'octagon':
            return max(abs(x), abs(z)) <= s and abs(x) + abs(z) <= s * 1.45
        if kind == 'circle':
            return math.hypot(x, z) <= s + 0.4
        if kind == 'diamond':
            return abs(x) + abs(z) <= s
        return False
    stages = [('square', 17), ('octagon', 15), ('circle', 13), ('diamond', 16), ('lantern', 9), ('square', 5)]
    y = 0
    for si, (kind, s) in enumerate(stages):
        for yy in range(y, y + HS):
            for x in range(-s - 1, s + 2):
                for z in range(-s - 1, s + 2):
                    if kind == 'lantern':
                        if yy in (y, y + HS - 1) and math.hypot(x, z) <= s + 0.4:
                            v.put(x, yy, z, 'A' if math.hypot(x, z) > 6.5 or yy == y else 'A')
                        continue
                    if not inside(kind, s, x, z):
                        continue
                    shell = not (inside(kind, s - 2, x, z))
                    if kind == 'diamond':
                        shell = not inside(kind, s - 3, x, z)
                    floor = yy == y or yy == y + HS - 1
                    if shell or floor:
                        band = shell and yy == y + HS - 1 and not inside(kind, s - 1, x, z) if kind != 'diamond' else shell and yy == y + HS - 1 and not inside(kind, s - 1.5, x, z)
                        v.put(x, yy, z, 'Q' if band else 'A')
        if kind == 'lantern':
            for (px, pz) in ring(0, 0, s, 8):
                for yy in range(y, y + HS):
                    for dx in (0, 1):
                        for dz in (0, 1):
                            v.put(px + dx - 0.5, yy, pz + dz - 0.5, 'A')
                v.put(px, y + HS // 2, pz, 'n')
            for (px, pz) in ring(0, 0, 7, 44):   # a hoop of computronium round the compile: it turns
                v.put(px, y + HS // 2, pz, 'c')
            lant = y + HS // 2
        y += HS
    for (x, z) in ((17, 17), (-17, 17), (17, -17), (-17, -17), (17, 0), (-17, 0), (0, 17)):
        for yy in range(1, HS - 1):
            v.put(x, yy, z, 'n')
    for yy in range(y, y + 6):
        v.put(0, yy, 0, 'A')
    v.put(0, y + 6, 0, 'F')
    for x in (-8, 8):                          # windows in the front of the first stage
        for yy in (4, 5, 6):
            v.put(x, yy, -17, 'G')
    v.desk(0, 0, 'D', n=2)
    write('reality_compiler', v, {'c': ['computronium_block']},
          {'effect_center': [0, lant, 0], 'effect_scale': 2.76,
           'moving': [{'id': 'hoop', 'chars': 'c', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 6,
                       'motion': [{'type': 'spin', 'axis': 'up', 'deg_per_sec': 30}]}],
           'fx': [{'type': 'compiler', 'radius': 5}]})


# ====================================================================== Axiomatic Breach Engine
def axiomatic_breach_engine():
    """Twin towers on one plinth, the breach torn open in the gap between them: lattice below, transfinite casing
    above, anchor pylons up every corner, and a coil strip down each inner face."""
    v = Vox()
    H, HA, S, GAP = 88, 27, 5, 17             # height, the lattice's height, tower half-width, half the gap
    for side in (-1, 1):
        cx = side * (GAP + S)
        for y in range(1, H + 1):
            for x in range(cx - S, cx + S + 1):
                for z in range(-S, S + 1):
                    ex, ez = abs(x - cx) == S, abs(z) == S
                    if not (ex or ez):
                        if y % 11 == 0:
                            v.put(x, y, z, 'A' if y <= HA else 't')      # floors
                        continue
                    if ex and ez and y > HA:
                        v.put(x, y, z, 'y')
                    elif x == cx - side * S and abs(z) <= 1:
                        v.put(x, y, z, 'K')                                 # the coil strip, facing the breach
                    else:
                        v.put(x, y, z, 'A' if y <= HA else 't')
        for (x, z) in ((cx - 2, -2), (cx + 2, -2), (cx - 2, 2), (cx + 2, 2)):   # the crown
            v.put(x, H + 1, z, 'k')
        for x in range(cx - 3, cx + 4):
            for z in range(-3, 4):
                if max(abs(x - cx), abs(z)) == 3:
                    v.put(x, H + 1, z, 't', over=False)
    for x in range(-GAP - 2 * S - 2, GAP + 2 * S + 3):                     # the plinth
        for z in range(-S - 2, S + 3):
            v.put(x, 0, z, 'A')
    for (x, z) in ring(0, 0, 14, 90):                                       # the breach's coil ring, in the plinth
        if abs(z) <= S + 2:
            v.put(x, 1, z, 'K')
    v.put(0, 1, 0, 's')
    v.put(0, 1, -S - 2, 'F')
    v.put(0, 1, S + 2, 'F')
    v.desk(0, 0, 'u', n=2)
    write('axiomatic_breach_engine', v, {'k': ['dimensional_stabiliser']},
          {'effect_center': [0, 46, 0], 'effect_scale': 5.08,
           'moving': [{'id': 'crowns', 'chars': 'k', 'split': 'components', 'pivot': 'self', 'ramp_seconds': 5, 'stagger': 0.25,
                       'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'up', 'amplitude': 1.2, 'period_seconds': 5}]}],
           'fx': [{'type': 'breach', 'radius': 14, 'height': 80}]})


# ====================================================================== Cantor Diagonalizer (new)
def cantor_diagonalizer():
    """A hall over a grid: eight rows of eight digit columns on a square floor, the roof a lattice of glass. The columns
    on the diagonal stand taller, and their heads flip up and down as the machine writes a number the list cannot hold."""
    v = Vox()
    S = 17
    for x in range(-S, S + 1):
        for z in range(-S, S + 1):
            v.put(x, 0, z, 'H')
            if max(abs(x), abs(z)) == S:
                v.put(x, 1, z, 'H')
    for i in range(8):
        for j in range(8):
            x, z = -14 + i * 4, -14 + j * 4
            diag = i == j
            for y in range(1, 11 if diag else 7):
                v.put(x, y, z, 'n' if diag else 'i')
            if diag:
                v.put(x, 11, z, 'e')
                v.put(x, 12, z, 'e')
    for x in range(-S, S + 1):                 # the roof lattice
        for z in range(-S, S + 1):
            if (x + 16) % 4 == 0 or (z + 16) % 4 == 0:
                if max(abs(x), abs(z)) < S:
                    v.put(x, 15, z, 'G')
            if max(abs(x), abs(z)) == S:
                v.put(x, 15, z, 'H')
    for (x, z) in ((-S, -S), (S, -S), (-S, S), (S, S), (0, S), (-S, 0), (S, 0)):
        for y in range(1, 15):
            v.put(x, y, z, 'H')
    for x in range(-S + 1, S):                 # the back wall: glass between rails
        for y in range(2, 15):
            v.put(x, y, S, 'G' if y % 4 else 'H')
    v.desk(0, 0, 'P', n=2)
    write('cantor_diagonalizer', v,
          {'H': ['hyperframe_casing'], 'i': ['cryogenic_cooling_jacket'], 'n': ['quantum_error_correction_core'], 'e': EMIT,
           'G': ['containment_field_glass'], 'P': ['hyperframe_casing', 'interface_port']},
          {'effect_center': [0, 8, 0], 'effect_scale': 2.5,
           'moving': [{'id': 'digits', 'chars': 'e', 'split': 'components', 'pivot': 'self', 'ramp_seconds': 2, 'stagger': 0.125,
                       'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'up', 'amplitude': 1.0, 'period_seconds': 3}]}],
           'fx': [{'type': 'glow', 'chars': 'e', 'color': '#B090FF', 'hot': '#FFFFFF'},
                  {'type': 'inflow', 'radius': 9.0, 'color': '#D0C0FF', 'waves': 4, 'period': 2.5}]}, reshaped=False)


# ====================================================================== Axiom Distiller (new)
def axiom_distiller():
    """A still of seven stages, one above the other: a coil-wound boiler at the foot, a tray and a band of windows in
    every stage, a cooling collar at every joint, and a condenser dome on top. The trays tremble as the laws boil off."""
    v = Vox()
    R, HS = 6, 6
    for x in range(-10, 11):
        for z in range(-10, 11):
            r = math.hypot(x, z)
            if r <= 10.4:
                v.put(x, 0, z, 'H')
            if 8.5 < r <= 10.4:
                v.put(x, 1, z, 'H')
            if 6.5 < r <= 8.5:
                v.put(x, 1, z, 'K')
                v.put(x, 2, z, 'K')
    top = 1 + 7 * HS
    drips = []
    for y in range(1, top + 1):
        st = (y - 1) // HS
        yy = (y - 1) % HS
        for x in range(-R - 2, R + 3):
            for z in range(-R - 2, R + 3):
                r = math.hypot(x, z)
                if R - 0.5 <= r < R + 0.5:
                    v.put(x, y, z, 'G' if yy in (2, 3) and (int(math.atan2(z, x) / (TAU / 12) + 12) % 2 == 0) else 'S')
                elif yy == 0 and R + 0.5 <= r < R + 1.5 and st > 0:
                    v.put(x, y, z, 'i')            # the collar
                elif yy == 1 and r < R - 0.5 and (abs(x) + abs(z)) % 2 == 0:
                    v.put(x, y, z, 't')            # the tray: a sieve plate
        if yy == 1:
            drips.append([2, y, 1])
    for y in range(top + 1, top + 6):          # the condenser dome
        rr = math.sqrt(max(0.0, (R + 0.5) ** 2 - ((y - top) * 1.25) ** 2))
        for x in range(-R - 1, R + 2):
            for z in range(-R - 1, R + 2):
                r = math.hypot(x, z)
                if rr - 1.0 < r <= rr:
                    v.put(x, y, z, 'i')
    for y in range(top + 5, top + 9):
        v.put(0, y, 0, 'S')
    for y in range(3, top + 1):                # the reflux line down the back
        v.put(0, y, R + 2, 'S')
    v.desk(0, 0, 'P', n=2)
    write('axiom_distiller', v,
          {'H': ['hyperframe_casing'], 'S': ['precision_assembly_casing'], 'i': ['cryogenic_cooling_jacket'], 'K': ['superconducting_coil_block'] + COILS[1:],
           't': ['hyperframe_casing'], 'G': ['containment_field_glass'], 'P': ['hyperframe_casing', 'interface_port']},
          {'effect_center': [0, 22, 0], 'effect_scale': 2.5,
           'moving': [{'id': 'trays', 'chars': 't', 'split': 'layers', 'pivot': 'centre', 'ramp_seconds': 3, 'stagger': 0.14,
                       'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'up', 'amplitude': 0.12, 'period_seconds': 2.5}]}],
           'fx': [{'type': 'glow', 'chars': 'K', 'color': '#A060FF', 'hot': '#FFE0FF'},
                  {'type': 'drips', 'points': drips, 'color': '#C8A0FF', 'fall': 5, 'period': 2.5},
                  {'type': 'bubbles', 'points': [[0, 4, 0], [2, 4, -2], [-2, 4, 1]], 'color': '#B080FF', 'rise': 5, 'size': 0.6, 'period': 4}]}, reshaped=False)


# ====================================================================== Pleroma Confluence (new)
FIVE = [('quintessence', '#FFD070'), ('alkahest', '#70E0A0'), ('mathesis', '#80A0FF'), ('panacea', '#FF90B0'), ('anima_mundi', '#C0F0FF')]


def pleroma_confluence():
    """Five channels come down in terraces from five fonts, one for each fluid, and meet in a round basin where the
    Pleroma gathers. Each channel is a stair of lattice with glass banks; pylons stand round the basin."""
    v = Vox()
    fx = []
    for x in range(-13, 14):                   # the basin: a floor, a wall, a lip
        for z in range(-13, 14):
            r = math.hypot(x, z)
            if r <= 13.4:
                v.put(x, 0, z, 't')
            if 9.5 < r <= 11.5:
                v.put(x, 1, z, 't')
                v.put(x, 2, z, 't' if r > 10.5 else 'G')
            if 11.5 < r <= 13.4:
                v.put(x, 1, z, 'A')
    for (x, z) in ring(0, 0, 12.5, 10):
        for y in range(2, 7):
            v.put(x, y, z, 'y')
    for a in range(5):
        ang = a * TAU / 5 + TAU / 4            # the first channel runs straight back, so the front is clear for the desk
        ca, sa = math.cos(ang), math.sin(ang)
        lips = []
        for d in range(11, 36):                # along the channel, out from the basin
            step = min(4, (d - 11) // 5)       # terraces: each five long, three higher than the last
            top = 3 + step * 3
            for w in range(-2, 3):
                x, z = ca * d - sa * w, sa * d + ca * w
                if abs(w) <= 1:
                    pier = (d - 11) % 5 in (0, 4)          # a pier at each end of a terrace; an aqueduct between
                    for y in range(1 if pier and w == 0 else max(1, top - 1), top + 1):
                        v.put(x, y, z, 'A', over=False)
                else:
                    for y in range(max(1, top - 2), top + 2):
                        v.put(x, y, z, 'G' if y == top + 1 else 'A', over=False)
            if (d - 11) % 5 == 0 and d > 11:
                lips.append([ca * (d - 0.5), top + 1, sa * (d - 0.5)])
        fxc, fzc = ca * 38, sa * 38            # the font: a glass tank on pylon legs
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                for y in range(1, 22):
                    edge = max(abs(dx), abs(dz)) == 3
                    corner = abs(dx) == 3 and abs(dz) == 3
                    if corner:
                        v.put(fxc + dx, y, fzc + dz, 'y' if y >= 14 else 'A')
                    elif y in (14, 21) or (edge and 14 < y < 21):
                        v.put(fxc + dx, y, fzc + dz, 'A' if y == 14 else 'G')
                    elif y < 14 and max(abs(dx), abs(dz)) <= 1:
                        v.put(fxc + dx, y, fzc + dz, 'A', over=False)
        lips.append([ca * 34.5, 16, sa * 34.5])
        lips.append([ca * 10.5, 4, sa * 10.5])
        fx.append({'type': 'drips', 'points': lips, 'color': FIVE[a][1], 'fall': 3, 'period': 2.0})
    v.put(0, 1, 0, 's')
    for (x, z) in ring(0, 0, 4, 8):
        v.put(x, 1, z, 'F')
    v.desk(0, 0, 'P', n=2)
    fx.append({'type': 'swirl', 'radius': 7})
    fx.append({'type': 'bubbles', 'points': [[0, 2, 0], [3, 2, 2], [-3, 2, -2], [2, 2, -4], [-2, 2, 4]], 'color': '#FFF0D0', 'rise': 9, 'size': 1.0, 'period': 6})
    write('pleroma_confluence', v,
          {'A': ['reality_lattice_casing'], 't': ['transfinite_casing'], 'G': ['containment_field_glass'], 'y': ['spacetime_anchor_pylon'],
           's': ['dimensional_stabiliser'], 'F': EMIT, 'P': ['transfinite_casing', 'interface_port']},
          {'effect_center': [0, 3, 0], 'effect_scale': 3.0, 'fx': fx}, reshaped=False)


# ====================================================================== Convergence Engine (new)
def convergence_engine():
    """Thirteen altars in a ring on a round floor, a pedestal on each, and a column in the middle where what they hold
    is brought together. A glass arch leaps from every altar to the column's head; thirteen cores circle above the ring."""
    v = Vox()
    for x in range(-22, 23):
        for z in range(-22, 23):
            r = math.hypot(x, z)
            if r <= 20.4:
                v.put(x, 0, z, 'S')
            elif r <= 22.4:
                v.put(x, 0, z, 'H')
            if 19.5 < r <= 20.5:
                v.put(x, 1, z, 'H')
    for y in range(1, 25):                     # the column: a hollow shaft with coil bands
        for x in range(-3, 4):
            for z in range(-3, 4):
                r = math.hypot(x, z)
                if 2.5 <= r < 3.5:
                    v.put(x, y, z, 'K' if y % 6 == 0 else 'H')
    for x in range(-4, 5):
        for z in range(-4, 5):
            if math.hypot(x, z) <= 4.4:
                v.put(x, 25, z, 'H')
    v.put(0, 26, 0, 'e')
    pts = []
    for k, (ax, az) in enumerate(ring(0, 0, 15, 13)):
        ax, az = int(round(ax)), int(round(az))
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for y in (1, 2, 3):
                    if y < 3 or (dx == 0 or dz == 0):
                        v.put(ax + dx, y, az + dz, 'H')
        v.put(ax, 4, az, 'p')
        for i in range(1, 16):                 # the arch: up from the altar and in to the column's head
            t = i / 16.0
            x, z = ax * (1 - t) + ax / 15.0 * 4.5 * t, az * (1 - t) + az / 15.0 * 4.5 * t
            y = 6 + 19 * math.sin(t * math.pi / 2)
            v.put(x, y, z, 'G', over=False)
        v.put(ax * 1.0, 8, az * 1.0, 'q')
        pts.append([ax, 5, az])
    v.desk(0, 0, 'P', n=2)
    write('convergence_engine', v,
          {'S': ['precision_assembly_casing'], 'H': ['hyperframe_casing'], 'K': ['superconducting_coil_block'] + COILS[1:], 'e': EMIT,
           'p': ['resonance_pedestal', 'harmonic_pedestal', 'absolute_pedestal'], 'q': ['quantum_error_correction_core'],
           'G': ['containment_field_glass'], 'P': ['hyperframe_casing', 'interface_port']},
          {'effect_center': [0, 12, 0], 'effect_scale': 2.5,
           'moving': [{'id': 'cores', 'chars': 'q', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 4,
                       'motion': [{'type': 'spin', 'axis': 'up', 'deg_per_sec': 24}]}],
           'fx': [{'type': 'glow', 'chars': 'q', 'color': '#B080FF', 'hot': '#FFFFFF'},
                  {'type': 'inflow', 'radius': 15.0, 'color': '#C0A0FF', 'waves': 5, 'period': 2.0},
                  {'type': 'bubbles', 'points': pts, 'color': '#D0B0FF', 'rise': 3, 'size': 0.5, 'period': 3}]}, reshaped=False)


ALL = [stellar_engine, dyson_array, matrioshka_brain, universe_forge, concept_compressor, omnilex_engine, reality_compiler,
       axiomatic_breach_engine, cantor_diagonalizer, axiom_distiller, pleroma_confluence, convergence_engine]
RESHAPED = ['stellar_engine', 'dyson_array', 'matrioshka_brain', 'universe_forge', 'concept_compressor', 'omnilex_engine', 'reality_compiler',
            'axiomatic_breach_engine']
NEW = ['cantor_diagonalizer', 'axiom_distiller', 'pleroma_confluence', 'convergence_engine']


def build():
    for f in ALL:
        f()
    json.dump(SHAPES, open(os.path.join(HERE, 'r171_shapes.json'), 'w'), indent=1)


if __name__ == '__main__':
    build()
