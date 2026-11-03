"""Round-11 item art, group 0: AI cores, analyzers, anchors, artifacts, qubits, brains, branes, capacitors,
chips / processors / dies / dossiers and exotic chunks. Every item has its own model and animation."""
import math
import numpy as np
from items3d import *
import anim

_bl = anim._blend


# ----------------------------------------------------------------------------------------- helpers
def _px(img, x, y, rgb, a=1.0):
    """Blend one pixel on the native 32x32 grid."""
    _bl(img, int(round(y)), int(round(x)), rgb, a)


def _sig(seed, k):
    """Deterministic pseudo-random in [0,1)."""
    v = math.sin(seed * 12.9898 + k * 78.233) * 43758.5453
    return v - math.floor(v)


def _frame(t, n=40):
    return int(round(t * n)) % n


def sd_frame(p, b, e):
    """Wireframe box (edges only)."""
    p = np.abs(p) - np.asarray(b)
    q = np.abs(p + e) - e
    def k(a, b_, c):
        v = np.stack([a, b_, c], -1)
        return length(np.maximum(v, 0)) + np.minimum(np.maximum(a, np.maximum(b_, c)), 0)
    return np.minimum(np.minimum(k(p[..., 0], q[..., 1], q[..., 2]), k(q[..., 0], p[..., 1], q[..., 2])), k(q[..., 0], q[..., 1], p[..., 2]))


def sd_ellip(p, r):
    r = np.asarray(r, float)
    return sd_sphere(p / r, 1.0) * r.min()


def sd_pins(n=2, sp=0.22, off=0.72, w=0.05, ln=0.11, h=0.03, y=-0.02, four=True):
    """All the gold legs of a package in a single SDF (domain repetition along each side)."""
    def f(p):
        x, yy, z = p[..., 0], p[..., 1] - y, p[..., 2]
        qx = x - sp * np.clip(np.round(x / sp), -n, n)
        a = sd_box(np.stack([qx, yy, np.abs(z) - off], -1), (w, h, ln))
        if not four:
            return a
        qz = z - sp * np.clip(np.round(z / sp), -n, n)
        b = sd_box(np.stack([np.abs(x) - off, yy, qz], -1), (ln, h, w))
        return np.minimum(a, b)
    return f


def _pkg(c1, c2, t, pat=None, size=0.62, four=True, pins=True, emissive=None):
    """Processor package: substrate + legs. Returns parts, mats (0 substrate, 1 gold)."""
    parts = [Part(lambda p: sd_box(p, (size, 0.08, size), 0.03), 0)]
    mats = [Material(c1, c2, metal=0.45, pattern=pat, emissive=emissive or (c2 if pat else None)),
            Material('#c8a040', '#fff0c0', metal=1.0)]
    if pins:
        parts.append(Part(sd_pins(2, 0.22, size + 0.1, four=four), 1))
    return parts, mats


def _bare_die(c1, c2, t, pat, shape='square', s=0.55):
    """A naked silicon die: a thin slab with a bevelled silver rim and a patterned active area."""
    if shape == 'hex':
        rim = lambda p: sd_hexprism(p, s, 0.06)
        top = lambda p: sd_hexprism(p - np.array([0, 0.05, 0]), s * 0.82, 0.03)
    elif shape == 'oct':
        def rim(p):
            q = np.abs(p)
            return np.maximum(sd_box(p, (s, 0.06, s), 0.01), (q[..., 0] + q[..., 2]) * 0.7071 - s * 0.98)
        def top(p):
            p = p - np.array([0, 0.05, 0])
            q = np.abs(p)
            return np.maximum(sd_box(p, (s * 0.84, 0.03, s * 0.84)), (q[..., 0] + q[..., 2]) * 0.7071 - s * 0.82)
    elif shape == 'rect':
        rim = lambda p: sd_box(p, (s * 1.25, 0.06, s * 0.72), 0.01)
        top = lambda p: sd_box(p - np.array([0, 0.05, 0]), (s * 1.08, 0.03, s * 0.56), 0.01)
    else:
        rim = lambda p: sd_box(p, (s, 0.06, s), 0.01)
        top = lambda p: sd_box(p - np.array([0, 0.05, 0]), (s * 0.84, 0.03, s * 0.84), 0.01)
    parts = [Part(rim, 0), Part(top, 1)]
    mats = [Material('#9aa4b4', '#ffffff', metal=1.0), Material(c1, c2, metal=0.8, emissive=c2, pattern=pat)]
    return parts, mats


def _glyph(img, rows, x0, y0, rgb, a=1.0):
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch == '#':
                _px(img, x0 + i, y0 + j, rgb, a)


def _zero(p):
    return np.zeros(p.shape[:-1])


# ============================================================================ AI cores
def autonomous_calibrator(c1, c2, t):
    """A hexagonal controller puck with a tuning dial whose needle drifts and is pulled back on target."""
    ang = 0.55 * math.sin(TAU * t) * math.exp(-0) * 0.6 + 0.22 * math.sin(3 * TAU * t)
    def dial(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        ticks = np.abs(np.mod(a * 8 / TAU, 1) - 0.5) > 0.25
        return _zero(p), (ticks & (n[..., 1] > 0.3)).astype(float)
    def needle(p):
        return sd_box(p - np.array([0.2, 0, 0]), (0.3, 0.04, 0.055), 0.015)
    def heal(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        front = np.abs(np.mod(a - TAU * t, TAU) - 0.3) < 0.35
        return _zero(p), (front & (np.abs(p[..., 1]) < 0.08)).astype(float)
    parts = [Part(lambda p: sd_hexprism(p, 0.66, 0.13), 0, rot=roty(0.26)),
             Part(lambda p: sd_torus(p, 0.42, 0.04), 1, pos=(0, 0.15, 0)),
             Part(needle, 3, pos=(0, 0.19, 0), rot=roty(ang)),
             Part(lambda p: sd_cyl(p, 0.08, 0.06, 0.02), 2, pos=(0, 0.2, 0))]
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=heal),
            Material('#304048', c2, metal=0.8, emissive=c2, pattern=dial),
            Material('#e8f0f0', c2, metal=1.0),
            Material('#ffffff', c2, emissive=mix(H(c2), WHITE, 0.5), pattern=pat_all())]
    def ov(img):
        f = int(t * 4)
        ph = (t * 4) % 1
        if ph < 0.4:
            pts = ((6, 22), (25, 21), (26, 12), (7, 12))
            x, y = pts[f]
            _px(img, x, y, WHITE, 1)
            for d in (1, 2) if ph < 0.2 else (1,):
                for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
                    _px(img, x + dx, y + dy, H(c2), 0.7)
    return parts, mats, dict(view_pitch=0.62, view_yaw=0.5, zoom=1.22), ov


def general_ai_core(c1, c2, t):
    """A tetrahedral mind: four thought-nodes linked to a pulsing core. Now and then it asks a question."""
    a = TAU * t
    R = roty(a) @ rotx(0.3)
    nodes = [np.array(v) * 0.62 / math.sqrt(3) for v in ((1, 1, 1), (-1, -1, 1), (-1, 1, -1), (1, -1, -1))]
    nodes = [R @ v for v in nodes]
    pulse = lambda p, n: (_zero(p), (np.sin(length(p) * 14 - TAU * t * 3) > 0.55).astype(float))
    parts = [Part(lambda p: sd_sphere(p, 0.24), 1)]
    for v in nodes:
        parts.append(Part(lambda p, v=v: sd_capsule(p, np.zeros(3), v, 0.045), 0))
        parts.append(Part(lambda p, v=v: sd_sphere(p - v, 0.15), 2))
    mats = [Material('#7a9a88', c2, metal=0.9, emissive=c2, pattern=pulse),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material(c1, c2, metal=0.7, emissive=c2, pattern=pat_spots(t, 0.6, 3))]
    q = ['.##.', '#..#', '...#', '..#.', '..#.', '....', '..#.']
    def ov(img):
        if 0.5 <= t < 0.9:
            k = min(1.0, (t - 0.5) / 0.1) * min(1.0, (0.9 - t) / 0.1)
            _glyph(img, q, 25, 1, H(c2), k)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.1), ov


def narrow_ai_core(c1, c2, t):
    """A blade-thin monolith with a single scan line: superhuman at exactly one thing."""
    y0 = 0.62 * math.sin(TAU * t)
    def scan(p, n):
        return _zero(p) - 0.05 * (np.abs(p[..., 2]) < 0.02), (np.abs(p[..., 1] - y0) < 0.07).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.1, 0.8, 0.42), 0.04), 0, rot=roty(0.0)),
             Part(lambda p: sd_box(p, (0.22, 0.06, 0.5), 0.02), 1, pos=(0, -0.84, 0))]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=scan), Material('#506068', c2, metal=1.0)]
    def ov(img):
        # the single thing it is superhuman at: one needle-thin beam
        yy = 15.5 - y0 * 15.5 * 0.9
        for x in range(0, 32):
            if img[int(round(yy)), x, 3] < 0.5:
                _px(img, x, yy, H(c2), 0.55 if (x + int(t * 40)) % 4 else 0.9)
    return parts, mats, dict(view_pitch=0.15, view_yaw=1.25, zoom=1.0), ov


def superintelligence_core(c1, c2, t):
    """A Matrioshka brain: a sun inside nested cut-away computronium shells turning against each other."""
    def shell(R, th, side):
        return lambda p: np.maximum(np.abs(length(p) - R) - th, side * p[..., 0] - 0.12)
    parts = [Part(lambda p: sd_sphere(p, 0.24), 0),
             Part(shell(0.46, 0.035, 1), 1, rot=roty(TAU * t) @ rotz(0.5)),
             Part(shell(0.72, 0.04, -1), 2, rot=roty(-TAU * t) @ rotx(0.35))]
    mats = [Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#806a20', c2, metal=0.9, emissive=c2, pattern=pat_spots(t, 0.55, 4)),
            Material('#4a4420', c2, metal=0.9, emissive=c2, pattern=pat_grid(5, t, 0.05))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6, zoom=1.0), None


# ============================================================================ analyzers
def field_calibrator(c1, c2, t):
    """A tuning fork on a dial: struck, it rings out arcs of sound and settles on the ideal value."""
    hum = 0.03 * math.sin(TAU * t * 8) * (1 - t) ** 1.5
    parts = [Part(lambda p: sd_capsule(p, np.array([0, -0.85, 0]), np.array([0, -0.2, 0]), 0.1), 0),
             Part(lambda p: sd_torus(p, 0.24, 0.075), 1, pos=(0, -0.12, 0), rot=rotx(TAU / 4)),
             Part(lambda p: sd_capsule(p, np.array([-0.24 - hum, -0.1, 0]), np.array([-0.24 - hum, 0.8, 0]), 0.075), 1),
             Part(lambda p: sd_capsule(p, np.array([0.24 + hum, -0.1, 0]), np.array([0.24 + hum, 0.8, 0]), 0.075), 1),
             Part(lambda p: sd_cyl(p, 0.17, 0.05, 0.02), 2, pos=(0, -0.42, -0.08), rot=rotx(TAU / 4))]
    gauge = lambda p, n: (_zero(p), (np.abs(np.arctan2(p[..., 0], p[..., 2]) - 0.9 * (1 - t) ** 2 * math.sin(TAU * t * 3)) < 0.3).astype(float))
    mats = [Material(c1, '#a0a8b8', metal=0.5), Material('#c8d0dc', c2, metal=1.0),
            Material('#202020', c2, emissive=c2, pattern=gauge)]
    def ov(img):
        amp = (1 - t)
        for k in range(3):
            r = 3 + ((t * 6 + k) % 3) * 2.5
            al = amp * (1 - ((t * 6 + k) % 3) / 3)
            for s in (-1, 1):
                cx = 15.5 + s * 5
                for dy in range(-2, 3):
                    x = cx + s * (r - abs(dy) * 0.4)
                    if 0 <= x < 32 and img[int(7 + dy), int(round(x)), 3] < 0.5:
                        _px(img, x, 7 + dy, H(c2), al)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.35, zoom=1.05), ov


def platonic_compass(c1, c2, t):
    """A white compass whose needle points up at a floating ideal octahedron."""
    na = 0.45 * math.sin(TAU * t) + 0.12 * math.sin(2 * TAU * t)
    needle = lambda p: sd_octa(p / np.array([1.0, 0.25, 0.22]), 0.5) * 0.22
    def rose(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        star = r < 0.48 * (0.45 + 0.55 * np.abs(np.cos(2 * a)) ** 6)
        return -0.15 * star, ((np.abs(r - 0.5) < 0.03) & (n[..., 1] > 0.5)).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.62, 0.07, 0.03), 0),
             Part(lambda p: sd_torus(p, 0.62, 0.06), 1),
             Part(needle, 2, pos=(0, 0.1, 0), rot=roty(na)),
             Part(lambda p: sd_octa(p, 0.3), 3, pos=(0, 0.6 + 0.05 * math.sin(TAU * t * 2), 0), rot=roty(TAU * t) @ rotx(0.6))]
    mats = [Material(c1, c2, metal=0.5, emissive=c2, pattern=rose), Material('#b0b8d0', '#ffffff', metal=1.0),
            Material(c2, '#ffffff', metal=1.0, emissive=c2, pattern=lambda p, n: (_zero(p), (p[..., 0] > 0.05).astype(float))),
            Material(c2, '#ffffff', metal=0.9, emissive=c2, pattern=lambda p, n: (_zero(p), (np.abs(p).sum(-1) > 0.0).astype(float) * (n[..., 0] + n[..., 2] > 0.3)))]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6, zoom=1.0), None


def resonance_probe(c1, c2, t):
    """A probe wand whose glass tip is circled by four beads, one per virtual-particle resonance."""
    tip = np.array([0.24, 0.3, 0])
    parts = [Part(lambda p: sd_capsule(p, np.array([-0.62, -0.75, 0]), np.array([0.05, 0.08, 0]), 0.09), 0),
             Part(lambda p: sd_torus(p, 0.14, 0.05), 1, pos=(0.05, 0.08, 0), rot=rotz(-0.78)),
             Part(lambda p: sd_sphere(p - tip, 0.2), 2)]
    cols = [c2, '#ff70c0', '#ffe060', '#80ff70']
    mats = [Material(c1, '#80a0c0', metal=0.6), Material('#b0c0d0', '#ffffff', metal=1.0),
            Material('#103040', c2, emissive=c2, pattern=pat_pulse_core(t))]
    for k in range(4):
        a = TAU * (t * (k + 1)) + k * 1.57
        tilt = rotx(0.4 + k * 0.7) @ rotz(k * 0.9)
        pos = tip + tilt @ np.array([math.cos(a) * 0.36, 0, math.sin(a) * 0.36])
        mats.append(Material(cols[k], '#ffffff', emissive=cols[k], pattern=pat_all()))
        parts.append(Part(lambda p: sd_sphere(p, 0.065), 3 + k, pos=pos))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.2), None


def spectral_analyzer(c1, c2, t):
    """A handheld scanner with a glass prism on top that splits a white beam into a rainbow."""
    def prism(p):
        q = np.abs(p)
        return np.maximum(q[..., 2] - 0.18, np.maximum(q[..., 0] * 0.866 + p[..., 1] * 0.5, -p[..., 1]) - 0.26)
    def screen(p, n):
        bar = np.mod(np.floor((p[..., 0] + 1) * 14), 1)
        hgt = 0.5 + 0.5 * np.sin(np.floor((p[..., 0] + 1) * 14) * 1.7 + TAU * t * 2)
        return _zero(p), ((p[..., 1] + 0.2) < hgt * 0.3).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.48, 0.42, 0.14), 0.07), 0, pos=(0, -0.3, 0)),
             Part(lambda p: sd_box(p, (0.36, 0.2, 0.03), 0.02), 1, pos=(0, -0.22, -0.14)),
             Part(prism, 2, pos=(0, 0.28, 0)),
             Part(lambda p: sd_capsule(p, np.array([-0.3, -0.72, 0]), np.array([-0.3, -0.92, 0]), 0.07), 0)]
    mats = [Material(c1, '#a0a8b8', metal=0.6), Material('#101418', c2, emissive=c2, pattern=screen),
            Material('#d8f0ff', '#ffffff', metal=1.0, gloss=1.8, emissive='#ffffff', pattern=pat_bands(8, 2, t, 0.12, axis=0))]
    rainbow = [H('#ff4040'), H('#ffa030'), H('#fff040'), H('#50ff60'), H('#40a0ff'), H('#a060ff')]
    def ov(img):
        f = (t * 2) % 1
        for x in range(0, 14):
            _px(img, x, 12.5, WHITE, 0.4 + 0.5 * ((x / 14 + f) % 1 < 0.5))
        for k, col in enumerate(rainbow):
            for s in range(1, 12):
                x = 19 + s
                y = 11.5 - s * (0.45 - k * 0.16)
                if img[int(round(y)), min(31, x), 3] < 0.5:
                    _px(img, x, y, col, 0.9 * (1 - s / 13))
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.15, zoom=1.0), ov


def structure_assembler(c1, c2, t):
    """A projector puck under a holographic wireframe; real blocks drop into its corners one by one.
    Loop: four drops (t < 0.6), hold, then the blocks shrink back into the hologram (t > 0.8) so it ends empty."""
    nshow = min(4, int(t / 0.15) + 1) if t < 0.6 else 4
    gone = max(0.0, (t - 0.8) / 0.2)
    gone = gone * gone * (3 - 2 * gone)
    def holo(p, n):
        scan = np.abs(np.mod(p[..., 1] * 2.5 - t * 3, 1) - 0.5) < 0.1
        return _zero(p), np.ones(p.shape[:-1]) * (0.6 + 0.4 * scan)
    parts = [Part(lambda p: sd_hexprism(p, 0.5, 0.08), 0, pos=(0, -0.72, 0)),
             Part(lambda p: sd_cyl(p, 0.16, 0.04), 2, pos=(0, -0.62, 0)),
             Part(lambda p: sd_frame(p, (0.46, 0.46, 0.46), 0.035), 1, pos=(0, 0.02, 0))]
    mats = [Material(c1, c2, metal=0.7), Material('#203040', c2, emissive=c2, pattern=holo),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_all()), Material('#8a90a0', '#ffffff', metal=0.6)]
    corners = [(-1, -1), (1, 1), (1, -1), (-1, 1)]
    sz = 0.21 * (1 - gone)
    for k in range(nshow if sz > 0.01 else 0):
        cx, cz = corners[k]
        drop = max(0.0, 1.45 - ((t / 0.15) % 1) * 5.0) if (k == nshow - 1 and t < 0.6) else 0.0   # falls in from above the frame
        parts.append(Part(lambda p, sz=sz: sd_box(p, (sz, sz, sz), 0.03 * sz / 0.21), 3, pos=(cx * 0.23, -0.22 + drop, cz * 0.23)))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.05), None


# ============================================================================ anchors
def anchor_core(c1, c2, t):
    """Perfectly still: a black negative-mass core in three fixed alloy rings, while the spacetime grid behind it warps."""
    parts = [Part(lambda p: sd_sphere(p, 0.3), 0)]
    for k in range(3):
        parts.append(Part(lambda p: sd_torus(p, 0.5 + 0.06 * k, 0.045), 1, rot=rotx(k * 1.05 + 0.3) @ rotz(k * 0.9)))
    mats = [Material('#000000', c2, metal=0.0, gloss=0.2, emissive=c2, keep_dark=True,
                     pattern=lambda p, n: (_zero(p), (np.abs(n[..., 1]) < -1).astype(float))),
            Material('#606878', c2, metal=1.0, emissive=c2, pattern=pat_bands(10, 0, t, 0.06, axis=0))]
    mats[0].ramp[:] = np.array([[0.02, 0.02, 0.06], [0.04, 0.04, 0.1], [0.06, 0.07, 0.16], [0.1, 0.14, 0.3], [0.3, 0.45, 0.8], [0.7, 0.85, 1.0]])
    col = H(c2)
    def ov(img):
        n = img.shape[0]
        ys, xs = np.mgrid[0:n, 0:n]
        dx, dy = xs - 15.5, ys - 15.5
        r = np.sqrt(dx * dx + dy * dy) + 1e-6
        w = np.clip((r - 11) / 4, 0, 1) * 1.6 * np.sin(TAU * t - r * 0.35)
        gx, gy = xs + w * dx / r, ys + w * dy / r
        line = (np.abs(np.mod(gx - 0.5, 7) - 3.5) > 3.0) | (np.abs(np.mod(gy - 0.5, 7) - 3.5) > 3.0)
        m = line & (img[..., 3] < 0.5) & (r > 9) & (r < 16)
        a = 0.75 * np.clip((r - 9) / 3, 0, 1) * np.clip((16 - r) / 3, 0, 1)
        img[m, :3] = col * 0.8
        img[m, 3] = a[m]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.15), ov


def brane_tether(c1, c2, t):
    """A glowing cord ties a hovering weight (your mass) down to a rippling purple brane."""
    sw = 0.16 * math.sin(TAU * t)
    base = np.array([0, -0.62, 0])
    mid = np.array([0.12 + sw * 0.5, 0.0, 0])
    top = np.array([sw * 1.6, 0.52 + 0.04 * math.sin(TAU * t * 2), 0])
    def brane(p, n):
        return 0.15 * np.sin(p[..., 0] * 5 + TAU * t), pat_grid(4, t, 0.1)(p, n)[1]
    cord = lambda p, n: (_zero(p), (np.mod(p[..., 1] * 4 - t * 4, 1) < 0.5).astype(float))
    parts = [Part(lambda p: sd_box(p, (0.8, 0.035, 0.58), 0.02), 0, pos=(0, -0.7, 0)),
             Part(lambda p: sd_cyl(p, 0.13, 0.05, 0.02), 1, pos=(0, -0.64, 0)),
             Part(lambda p: sd_capsule(p, base, mid, 0.045), 3),
             Part(lambda p: sd_capsule(p, mid, top, 0.045), 3),
             Part(lambda p: sd_torus(p, 0.11, 0.04), 1, pos=mid, rot=rotz(TAU / 4)),
             Part(lambda p: sd_gem(p, 0.24, 0.3, 0.5), 2, pos=top + np.array([0, 0.22, 0]), rot=roty(TAU * t))]
    mats = [Material(c1, c2, metal=0.5, emissive=c2, pattern=brane), Material('#c8c0e8', '#ffffff', metal=1.0),
            Material(c2, '#ffffff', metal=0.8, emissive=c2, pattern=pat_pulse_core(t)),
            Material(c2, '#ffffff', emissive=mix(H(c2), WHITE, 0.4), pattern=cord)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6, zoom=1.0), None


def geodesic_anchor(c1, c2, t):
    """A ship's anchor with a glowing worldline running up its shank."""
    def flow(p, n):
        return _zero(p), (np.mod(p[..., 1] * 3 - t * 2, 1) < 0.25).astype(float)
    arms = lambda p: np.maximum(sd_torus(p, 0.5, 0.075), p[..., 2] - 0.0)
    parts = [Part(lambda p: sd_capsule(p, np.array([0, -0.62, 0]), np.array([0, 0.55, 0]), 0.075), 1),
             Part(lambda p: sd_torus(p, 0.15, 0.05), 0, pos=(0, 0.73, 0), rot=rotx(TAU / 4)),
             Part(lambda p: sd_capsule(p, np.array([-0.3, 0.4, 0]), np.array([0.3, 0.4, 0]), 0.06), 0),
             Part(arms, 0, pos=(0, -0.12, 0), rot=rotx(-TAU / 4)),
             Part(lambda p: sd_octa(p / np.array([1, 1.4, 0.6]), 0.16) * 0.6, 0, pos=(-0.5, -0.06, 0), rot=rotz(0.5)),
             Part(lambda p: sd_octa(p / np.array([1, 1.4, 0.6]), 0.16) * 0.6, 0, pos=(0.5, -0.06, 0), rot=rotz(-0.5))]
    mats = [Material(c1, c2, metal=0.9), Material('#504060', c2, metal=0.9, emissive=c2, pattern=flow)]
    col = H(c2)
    def ov(img):
        # the worldline continues above and below the anchor as a faint dotted line
        for y in range(32):
            if img[y, 16, 3] < 0.5 and (y - int(t * 16)) % 4 < 2:
                _px(img, 16, y, col, 0.5)
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.25, zoom=1.0), ov


def rift_anchor(c1, c2, t):
    """A flickering vertical tear between universes, held open by four pins driven through its lips."""
    wdt = 0.15 + 0.04 * math.sin(TAU * t * 2)
    def tear(p, n):
        v = np.sin(p[..., 1] * 13 + TAU * t * 3) * np.sin(p[..., 1] * 7 - TAU * t)
        core = np.abs(p[..., 0] / wdt + 0.35 * v) < 0.55
        return 0.6 * core, np.where(core, 1.0, 0.0)
    parts = [Part(lambda p: sd_ellip(p, (wdt, 0.82, 0.1)), 1)]
    mats = [Material(c1, c2, metal=0.95), Material('#1a0830', c2, emissive=mix(H(c2), WHITE, 0.3) * 1.3, keep_dark=True, pattern=tear),
            Material('#d0c0e0', '#ffffff', metal=1.0)]
    for sx in (-1, 1):
        for sy in (-1, 1):
            a = np.array([sx * 0.06, sy * 0.36, 0])
            b = np.array([sx * (0.5 + wdt), sy * 0.5, 0])
            parts.append(Part(lambda p, a=a, b=b: sd_capsule(p, a, b, 0.05), 2))
            parts.append(Part(lambda p, b=b: sd_sphere(p - b, 0.1), 0))
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.35, zoom=1.0), None


# ============================================================================ artifacts
def bootstrap_artifact(c1, c2, t):
    """A golden medallion that nobody made: its future copy drifts in from the side and merges with it."""
    def engr(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        sp = np.mod(a / TAU * 2 + r * 4 + t * 2, 1) < 0.2
        return -0.08 * sp, (sp & (n[..., 1] > 0.5) & (r > 0.2)).astype(float)
    parts = [Part(lambda p: sd_cyl(p, 0.5, 0.06, 0.02), 0, rot=rotx(1.2)),
             Part(lambda p: sd_torus(p, 0.52, 0.06), 1, rot=rotx(1.2)),
             Part(lambda p: sd_torus(p, 0.12, 0.04), 1, pos=(0, 0.62, 0.05)),
             Part(lambda p: sd_gem(p, 0.13, 0.12, 0.5), 2, pos=(0, 0.04, -0.12), rot=rotx(1.2))]
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=engr), Material('#c09040', '#fff0c0', metal=1.0),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    col = H(c2)
    def ov(img):
        a = math.sin(math.pi * t) ** 2 * 0.55
        dx = int(round(11 * (1 - t)))
        if a < 0.02 or dx == 0:
            return
        src = img.copy()
        ghost = np.roll(src, dx, axis=1)
        if dx > 0:
            ghost[:, :dx, 3] = 0
        m = ghost[..., 3] > 0.5
        g = ghost[..., :3] * 0.35 + col * 0.65
        op = (img[..., 3] > 0.5)[..., None]
        new = np.where(op, img[..., :3] * (1 - a) + g * a, g)
        img[m, :3] = new[m]
        img[m, 3] = np.where(op[..., 0], 1.0, a + 0.15)[m]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.3, zoom=1.05), ov


def form_of_the_self(c1, c2, t):
    """An idealised crystal figure with a halo - your Platonic form."""
    parts = [Part(lambda p: sd_octa(p, 0.17), 0, pos=(0, 0.6, 0)),
             Part(lambda p: sd_gem(p, 0.2, 0.34, 0.45), 0, pos=(0, 0.12, 0)),
             Part(lambda p: sd_capsule(p, np.array([-0.14, 0.32, 0]), np.array([-0.42, -0.02, 0]), 0.055), 0),
             Part(lambda p: sd_capsule(p, np.array([0.14, 0.32, 0]), np.array([0.42, -0.02, 0]), 0.055), 0),
             Part(lambda p: sd_capsule(p, np.array([-0.08, -0.2, 0]), np.array([-0.16, -0.82, 0]), 0.06), 0),
             Part(lambda p: sd_capsule(p, np.array([0.08, -0.2, 0]), np.array([0.16, -0.82, 0]), 0.06), 0),
             Part(lambda p: sd_torus(p, 0.24, 0.025), 1, pos=(0, 0.66, 0.05), rot=rotx(1.3))]
    mats = [Material(c1, c2, metal=0.95, gloss=1.6, emissive='#ffffff', pattern=pat_bands(7, 1, t, 0.08, axis=1)),
            Material('#ffffff', '#ffffff', emissive='#fff8d0', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.4, zoom=1.0), None


# ============================================================================ bloch
def gauge_boson_cluster(c1, c2, t):
    """Photon, gluon, W and Z pressed together; each flares in turn and throws out a wavy force line of its colour."""
    cols = [c1, '#ff5a50', '#60a0ff', '#c070ff']
    dirs = [np.array(v) / math.sqrt(3) for v in ((1, 1, -1), (-1, 1, 1), (1, -1, 1), (-1, -1, -1))]
    R = roty(TAU * t) @ rotx(0.4)
    parts, mats = [], []
    for k in range(4):
        beat = math.exp(-(((t * 4 - k) % 4) / 0.35) ** 2) + math.exp(-(((t * 4 - k) % 4 - 4) / 0.35) ** 2)
        mats.append(Material(cols[k], '#ffffff', metal=0.5, emissive=cols[k], pattern=pat_pulse_core(t + k / 4)))
        parts.append(Part(lambda p, r=0.28 + 0.06 * beat: sd_sphere(p, r), k, pos=R @ dirs[k] * 0.3))
    mats.append(Material('#ffffff', '#ffffff', emissive=c2, pattern=pat_all()))
    parts.append(Part(lambda p: sd_sphere(p, 0.16), 4))
    rgb = [H(c) for c in cols]
    ang = [0.6, 2.2, 3.8, 5.4]
    def ov(img):
        k = int(t * 4) % 4
        ph = (t * 4) % 1
        a0 = ang[k] + TAU * t * 0.5
        for s in range(9):
            r = 9 + s + ph * 4
            if r > 16:
                continue
            w = math.sin(s * 1.6 - ph * TAU) * 1.2
            x = 15.5 + math.cos(a0) * r - math.sin(a0) * w
            y = 15.5 + math.sin(a0) * r + math.cos(a0) * w
            yy, xx = int(round(y)), int(round(x))
            if 0 <= yy < 32 and 0 <= xx < 32 and img[yy, xx, 3] < 0.5:
                _px(img, x, y, rgb[k], (1 - ph) * (1 - s / 10))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.05), ov


def qubit(c1, c2, t):
    """A Bloch sphere: equator and meridian rings, with the state arrow precessing - until it is measured and snaps to a pole."""
    look = math.exp(-((t - 0.8) / 0.06) ** 2)
    th = 1.15 * (1 - look) + 0.0
    ph = TAU * t * 2
    v = np.array([math.sin(th) * math.cos(ph), math.cos(th), math.sin(th) * math.sin(ph)]) * 0.58
    parts = [Part(lambda p: sd_torus(p, 0.66, 0.03), 0),
             Part(lambda p: sd_torus(p, 0.66, 0.03), 0, rot=rotx(TAU / 4)),
             Part(lambda p: sd_capsule(p, np.zeros(3), v, 0.05), 1),
             Part(lambda p: sd_sphere(p - v, 0.12), 2),
             Part(lambda p: sd_sphere(p, 0.1), 3),
             Part(lambda p: sd_sphere(p, 0.07), 3, pos=(0, 0.66, 0)),
             Part(lambda p: sd_sphere(p, 0.07), 3, pos=(0, -0.66, 0))]
    mats = [Material('#406878', c1, metal=0.9, emissive=c1, pattern=pat_bands(10, 1, t, 0.1, axis=0)),
            Material(c2, '#ffffff', emissive=c1, pattern=pat_all()),
            Material(c2, '#ffffff', emissive=mix(H(c1), WHITE, 0.5 + 0.5 * look), pattern=pat_all()),
            Material('#e0f8ff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.05), None


# ============================================================================ brains
def _brain_sdf(scale=1.0, seed=0.0):
    def f(p):
        p = p / scale
        d = sd_ellip(p, (0.66, 0.46, 0.52))
        d = d + 0.035 * np.sin(p[..., 0] * 19 + seed) * np.sin(p[..., 1] * 15 + p[..., 2] * 13 + seed)
        d = d + 0.07 * np.exp(-(p[..., 2] / 0.06) ** 2) * (p[..., 1] > -0.05)
        return d * scale * 0.8
    return f


def boltzmann_brain(c1, c2, t):
    """A brain that condenses out of static, holds together for a moment, and dissolves back into noise."""
    f = 0.5 + 0.5 * math.cos(TAU * t)       # 1 = pure noise, 0 = fully formed
    parts = [Part(_brain_sdf(0.95, 1.0), 0, rot=roty(0.3))]
    mats = [Material(c1, c2, metal=0.25, emissive=c2, pattern=pat_combine(pat_noise_shade(14, 0.25, 2), pat_spots(t, 0.8, 7)))]
    col = H(c2)
    def ov(img):
        rnd = np.random.default_rng(_frame(t))
        n = img.shape[0]
        ys, xs = np.mgrid[0:n, 0:n]
        r = np.hypot(xs - 15.5, ys - 15.5)
        op = img[..., 3] > 0.5
        drop = op & (rnd.random((n, n)) < 0.75 * f ** 1.5)
        img[drop, 3] = 0
        noise = (~op | drop) & (rnd.random((n, n)) < 0.3 * f) & (r < 15)
        g = rnd.random((n, n))
        img[noise, :3] = (mix(H(c1), col, 0.5) * 0.4)[None] + g[noise, None] * 0.6 * col
        img[noise, 3] = 0.85
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.4, zoom=1.0), ov


def neural_organoid(c1, c2, t):
    """A pea-sized pink brain in a glass petri dish, firing as it dreams; dream bubbles rise from it."""
    def dish(p):
        outer = sd_cyl(p, 0.72, 0.12, 0.03)
        inner = sd_cyl(p - np.array([0, 0.06, 0]), 0.64, 0.12)
        return np.maximum(outer, -inner)
    def medium(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        return _zero(p), (np.abs(np.mod(r * 6 - t * 2, 1) - 0.5) < 0.12).astype(float) * (r > 0.3)
    parts = [Part(dish, 0, pos=(0, -0.3, 0)),
             Part(lambda p: sd_cyl(p, 0.62, 0.03), 1, pos=(0, -0.3, 0)),
             Part(_brain_sdf(0.42, 3.0), 2, pos=(0, -0.08, 0), rot=roty(0.8))]
    mats = [Material('#a8c8d8', '#ffffff', metal=1.0, gloss=1.8), Material('#402838', c2, emissive=mix(H(c2), H(c1), 0.5), pattern=medium),
            Material('#e890b0', '#ffffff', metal=0.2, emissive=c2, pattern=pat_spots(t * 2 % 1, 0.75, 5))]
    col = H(c2)
    def ov(img):
        for k in range(3):
            life = (t + k / 3) % 1
            y = 12 - life * 11
            x = 15.5 + (k - 1) * 3 + math.sin(TAU * (life * 1.5 + k / 3)) * 1.5
            a = math.sin(math.pi * life)
            r = 1 if life < 0.5 else 1.5
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                _px(img, x + dx * r, y + dy * r, col, a * 0.85)
            if life > 0.5:
                _px(img, x, y, WHITE, a * 0.3)
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5, zoom=1.15), ov


# ============================================================================ branes
def d_brane_sheet(c1, c2, t):
    """A dark sheet with three glowing open strings whose ends are stuck to it, sliding and vibrating."""
    def sheet(p, n):
        g = (np.abs(np.mod(p[..., 0] * 4, 1) - 0.5) > 0.44) | (np.abs(np.mod(p[..., 2] * 4, 1) - 0.5) > 0.44)
        return 0.25 * (g & (n[..., 1] > 0.5)), _zero(p)
    parts = [Part(lambda p: sd_box(p, (0.8, 0.04, 0.62), 0.02), 0, pos=(0, -0.3, 0))]
    for k in range(3):
        x = 0.42 * math.sin(TAU * t + k * 2.1)
        z = (k - 1) * 0.36
        R = 0.17 + 0.05 * k + 0.03 * math.sin(TAU * t * 3 + k)
        arc = lambda p, R=R: np.maximum(sd_torus(p, R, 0.035), p[..., 2])
        parts.append(Part(arc, 1, pos=(x, -0.26, z), rot=rotx(TAU / 4) @ roty(0.0) @ rotz(0.3 * math.sin(TAU * t + k))))
    mats = [Material('#2a1c48', '#6040a0', metal=0.6, pattern=sheet, keep_dark=True),
            Material(c2, '#ffffff', emissive=mix(H(c2), WHITE, 0.65), pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.05), None


def false_vacuum_membrane(c1, c2, t):
    """A curved bubble wall between two vacua: dark on one side, ripples running outward across it."""
    def cap(p):
        return np.maximum(np.abs(sd_sphere(p - np.array([0, -0.6, 0]), 1.0)) - 0.04, -(p[..., 1] + 0.12))
    def ripple(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        ring = np.abs(np.mod(r * 2 - t * 2, 1) - 0.5) < 0.17
        return 0.12 * np.sin(r * 12 - TAU * t * 2), ring.astype(float)
    wob = 1 + 0.04 * math.sin(TAU * t * 2)
    parts = [Part(lambda p: cap(p / np.array([wob, 1 / wob, wob])) * min(wob, 1 / wob), 0, rot=rotx(0.95) @ rotz(0.25))]
    mats = [Material(c1, c2, metal=0.8, gloss=1.6, emissive=mix(H(c2), WHITE, 0.5), keep_dark=False, pattern=ripple)]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5, zoom=1.05), None


def m_brane(c1, c2, t):
    """An undulating membrane with tiny bright universes riding its waves."""
    def h(p):
        return 0.14 * np.sin(p[..., 0] * 4 + TAU * t) * np.cos(p[..., 2] * 3 - TAU * t) + 0.06 * np.sin((p[..., 0] + p[..., 2]) * 6 - TAU * t * 2)
    def sheet(p):
        return np.maximum((np.abs(p[..., 1] - h(p)) - 0.045) * 0.55, sd_box(p, (0.74, 1.0, 0.74), 0.15))
    def unis(p, n):
        cell = np.floor(p[..., 0] * 5) * 7 + np.floor(p[..., 2] * 5) * 13
        rnd = np.mod(np.sin(cell * 12.9898) * 43758.5, 1)
        cx = np.mod(p[..., 0] * 5, 1) - 0.5
        cz = np.mod(p[..., 2] * 5, 1) - 0.5
        dot = (cx * cx + cz * cz < 0.05) & (rnd > 0.55)
        tw = np.sin(rnd * 30 + TAU * t) > -0.3
        return 0.3 * (p[..., 1] - 0.0), (dot & tw).astype(float)
    parts = [Part(sheet, 0, rot=roty(0.3))]
    mats = [Material(c1, c2, metal=0.7, emissive='#ffffff', pattern=unis)]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6, zoom=1.0, steps=90), None


def mobius_strip(c1, c2, t):
    """A real one-sided strip: a band twisting half a turn, with a glowing walker going round it (twice per loop)."""
    R, w, h = 0.56, 0.26, 0.04
    def strip(p):
        th = np.arctan2(p[..., 2], p[..., 0])
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        u, v = r - R, p[..., 1]
        c, s = np.cos(th / 2), np.sin(th / 2)
        a, b = c * u + s * v, -s * u + c * v
        q = np.stack([np.abs(a) - w, np.abs(b) - h], -1)
        return (length(np.maximum(q, 0)) + np.minimum(np.maximum(q[..., 0], q[..., 1]), 0)) * 0.75
    def stripe(p, n):
        th = np.arctan2(p[..., 2], p[..., 0])
        return 0.15 * np.sin(th * 6), (np.abs(np.mod(th * 6 / TAU, 1) - 0.5) < 0.08).astype(float)
    ph = TAU * t * 2
    c = np.array([R * math.cos(ph), 0, R * math.sin(ph)])
    radial = np.array([math.cos(ph), 0, math.sin(ph)])
    nrm = -math.sin(ph / 2) * radial + math.cos(ph / 2) * np.array([0, 1.0, 0])
    walk = rotx(-0.3) @ (c + nrm * (h + 0.07))
    parts = [Part(strip, 0, rot=rotx(-0.3)), Part(lambda p: sd_sphere(p, 0.09), 1, pos=walk)]
    mats = [Material('#2a4058', c2, metal=0.6, emissive=c2, pattern=stripe, keep_dark=True),
            Material('#ffffff', '#ffffff', emissive=mix(H(c2), WHITE, 0.6), pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.3, zoom=1.1, steps=80), None


# ============================================================================ capacitors
def atto_capacitor(c1, c2, t):
    """A multilayer chip capacitor: silver end caps, a stack of Higgs-charged layers that fill up and flash."""
    lvl = 0.5 - 0.5 * math.cos(TAU * t)
    def layers(p, n):
        y = p[..., 1]
        band = np.abs(np.mod(y * 9, 1) - 0.5) < 0.22
        filled = (y + 0.26) / 0.52 < lvl
        side = np.abs(n[..., 1]) < 0.5
        return -0.12 * (~band), (band & filled & side).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.42, 0.28, 0.32), 0.02), 0),
             Part(lambda p: sd_box(p, (0.14, 0.31, 0.35), 0.06), 1, pos=(-0.55, 0, 0)),
             Part(lambda p: sd_box(p, (0.14, 0.31, 0.35), 0.06), 1, pos=(0.55, 0, 0))]
    mats = [Material(c1, c2, metal=0.4, emissive=c2, pattern=layers), Material('#c8ccd8', '#ffffff', metal=1.0)]
    col = H(c2)
    def ov(img):
        if lvl > 0.92:
            k = int(t * 40) % 3
            pts = [(13, 7), (15, 6), (17, 7), (19, 6)]
            for i, (x, y) in enumerate(pts):
                _px(img, x + (k if i % 2 else 0) * 0, y - (1 if (i + k) % 2 else 0), WHITE if i == k else col, 0.9)
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.45, zoom=1.12), ov


def femto_capacitor(c1, c2, t):
    """Two Casimir plates a hair apart; virtual pairs wink in and out of the gap between them."""
    gap = 0.11
    def vac(p, n):
        cell = np.floor(np.arctan2(p[..., 2], p[..., 0]) * 3) * 3 + np.floor(p[..., 1] * 12) * 17
        rnd = np.mod(np.sin(cell * 12.9898 + np.floor(t * 8) * 3.1) * 43758.5, 1)
        return _zero(p), (rnd > 0.6).astype(float)
    disc = lambda p: sd_cyl(p, 0.58, 0.035, 0.015)
    gap = 0.16
    parts = [Part(disc, 0, pos=(0, gap, 0)),
             Part(disc, 0, pos=(0, -gap, 0)),
             Part(lambda p: sd_cyl(p, 0.5, 0.11), 1),
             Part(lambda p: sd_capsule(p, np.array([0.3, gap, 0]), np.array([0.3, 0.75, 0]), 0.04), 2),
             Part(lambda p: sd_capsule(p, np.array([-0.3, -gap, 0]), np.array([-0.3, -0.75, 0]), 0.04), 2)]
    mats = [Material(c1, c2, metal=1.0, gloss=1.5), Material('#101828', c2, emissive=mix(H(c2), WHITE, 0.4), keep_dark=True, pattern=vac),
            Material('#c8a040', '#fff0c0', metal=1.0)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6 + 0.25 * math.sin(TAU * t), zoom=1.1), None


def higgs_field_capacitor(c1, c2, t):
    """The Mexican-hat potential itself, cast in dark metal: a glowing ball rolls round its trough."""
    def hat(p):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        f = 3.2 * (r * r - 0.25) ** 2 - 0.1
        fp = 12.8 * r * (r * r - 0.25)
        d = (np.abs(p[..., 1] - f) - 0.05) / np.sqrt(1 + fp * fp)
        return np.maximum(d, r - 0.76)
    def contours(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        ring = np.abs(np.mod(r * 3.5 - t, 1) - 0.5) < 0.13
        return 0.22 * ring, _zero(p)
    a = TAU * t
    parts = [Part(hat, 0),
             Part(lambda p: sd_cyl(p, 0.18, 0.2, 0.04), 1, pos=(0, -0.38, 0)),
             Part(lambda p: sd_cyl(p, 0.4, 0.05, 0.02), 1, pos=(0, -0.58, 0)),
             Part(lambda p: sd_sphere(p, 0.14), 2, pos=(math.cos(a) * 0.5, 0.06, math.sin(a) * 0.5))]
    mats = [Material('#484848', c2, metal=0.9, pattern=contours), Material('#505050', '#a0a0a0', metal=1.0),
            Material('#ffffff', '#ffffff', emissive=mix(H(c2), WHITE, 0.5), pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.62, view_yaw=0.5, zoom=1.1, steps=90), None


def rf_resonator_cell(c1, c2, t):
    """A three-cell copper cavity on a beam pipe; the standing wave inside swaps cells as it kicks bunches through."""
    def wave(p, n):
        s = np.cos(p[..., 0] / 0.42 * math.pi) * math.cos(TAU * t * 2)
        return 0.1 * s, (s > 0.55).astype(float)
    parts = [Part(lambda p: sd_capsule(p, np.array([-0.9, 0, 0]), np.array([0.9, 0, 0]), 0.1), 1)]
    for k in (-1, 0, 1):
        parts.append(Part(lambda p: sd_ellip(p, (0.2, 0.42, 0.42)), 0, pos=(k * 0.4, 0, 0)))
    for s in (-1, 1):
        parts.append(Part(lambda p: sd_cyl(p, 0.17, 0.035, 0.01), 1, pos=(s * 0.82, 0, 0), rot=rotz(TAU / 4)))
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=wave), Material('#a0a8b0', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.45, zoom=1.0), None


# ============================================================================ chips: dossiers
def _smin(a, b, k=0.1):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def absolute_dossier(c1, c2, t):
    """A thick black tome with glowing page edges; the seal on its cover is a ring that is almost - not quite - closed."""
    gap_a = TAU * t
    def cover(p, n):
        x, y = p[..., 0] + 0.03, p[..., 1]
        r = np.sqrt(x * x + y * y)
        a = np.arctan2(y, x)
        d = np.abs(np.mod(a - gap_a + math.pi, TAU) - math.pi)
        ring = (np.abs(r - 0.26) < 0.05) & (d > 0.35) & (p[..., 2] < -0.12)
        dot = (r < 0.07) & (p[..., 2] < -0.12)
        return _zero(p), (ring | dot).astype(float)
    def pages(p, n):
        lines = np.abs(np.mod(p[..., 2] * 22, 1) - 0.5) < 0.3
        return -0.2 * lines, (lines & (np.abs(n[..., 1]) < 0.9)).astype(float) * 0.0 + 0.0
    parts = [Part(lambda p: sd_box(p, (0.5, 0.66, 0.17), 0.04), 0),
             Part(lambda p: sd_box(p, (0.47, 0.62, 0.13), 0.01), 1, pos=(0.06, 0, 0)),
             Part(lambda p: sd_box(p, (0.07, 0.68, 0.19), 0.03), 2, pos=(-0.48, 0, 0))]
    mats = [Material('#121212', c2, metal=0.3, gloss=0.6, emissive=c2, pattern=cover, keep_dark=True),
            Material('#e8e8f0', '#ffffff', metal=0.1, emissive='#ffffff', pattern=lambda p, n: (_zero(p), (np.abs(np.mod(p[..., 2] * 16 + p[..., 1] * 2 - t * 2, 1) - 0.5) < 0.2).astype(float))),
            Material('#282830', c2, metal=0.8, keep_dark=True)]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.55, zoom=1.12), None


def empirical_dossier(c1, c2, t):
    """A clipboard of measurements: data points plot themselves one by one, then the fit line draws through them.
    Loop: points (t < 0.7), line (0.72-0.85), then a wipe clears the sheet downward (0.88-1) so it ends blank."""
    pts = [(-0.3, -0.32), (-0.2, -0.18), (-0.1, -0.2), (0.0, -0.02), (0.1, 0.02), (0.2, 0.18), (0.28, 0.24)]
    nshow = min(len(pts), int(t * 10))
    wipe = 0.34 - min(1.0, max(0.0, (t - 0.86) / 0.1)) * 0.8   # everything above this height has been erased
    def paper(p, n):
        x, y = p[..., 0], p[..., 1]
        front = p[..., 2] < -0.004
        em = np.zeros(x.shape, bool)
        for k in range(nshow):
            em |= (np.abs(x - pts[k][0]) < 0.045) & (np.abs(y - pts[k][1]) < 0.045)
        if t > 0.72:
            reach = -0.35 + min(1.0, (t - 0.72) / 0.13) * 0.7
            em |= (np.abs(y - (x * 0.95 - 0.04)) < 0.025) & (x < reach) & (x > -0.36)
        em &= y < wipe
        axes = ((np.abs(x + 0.38) < 0.015) & (y > -0.4) & (y < 0.32)) | ((np.abs(y + 0.4) < 0.015) & (x > -0.38) & (x < 0.36))
        rule = np.abs(np.mod(y * 10, 1) - 0.5) < 0.05
        return -0.25 * axes - 0.06 * rule, (em & front).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.52, 0.7, 0.04), 0.03), 0, pos=(0, -0.05, 0.04)),
             Part(lambda p: sd_box(p, (0.44, 0.56, 0.01)), 1, pos=(0, -0.1, -0.01)),
             Part(lambda p: sd_box(p, (0.22, 0.08, 0.05), 0.03), 2, pos=(0, 0.6, -0.01)),
             Part(lambda p: sd_torus(p, 0.08, 0.03), 2, pos=(0, 0.72, -0.01), rot=rotx(TAU / 4))]
    mats = [Material(c1, c2, metal=0.4), Material('#dce6f0', '#ffffff', metal=0.0, gloss=0.3, emissive=mix(H(c2), H('#1040c0'), 0.6), pattern=paper),
            Material('#b8c0cc', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.3, zoom=1.08), None


def theoretical_dossier(c1, c2, t):
    """A scroll of theory hanging between two rods; lines of equations write themselves down the sheet."""
    sway = 0.06 * math.sin(TAU * t)
    def sheet(p, n):
        x, y = p[..., 0], p[..., 1]
        row = np.floor((0.5 - y) * 9)
        prog = t * 6 - row
        on = (row >= 0) & (row < 6) & (np.abs(np.mod(y * 9, 1) - 0.5) < 0.2)
        word = np.mod(x * 7 + row * 1.7, 1) < 0.72
        wrote = (x + 0.4) / 0.8 < np.clip(prog, 0, 1)
        return -0.1 * (on & word), (on & word & wrote & (p[..., 2] < 0)).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.48, 0.6, 0.012)), 1, pos=(0, -0.02, 0)),
             Part(lambda p: sd_cyl(p, 0.09, 0.6, 0.03), 0, pos=(0, 0.62, 0), rot=rotz(TAU / 4)),
             Part(lambda p: sd_cyl(p, 0.09, 0.6, 0.03), 0, pos=(0, -0.66, 0), rot=rotz(TAU / 4)),
             Part(lambda p: sd_sphere(p, 0.1), 2, pos=(0.68, 0.62, 0)), Part(lambda p: sd_sphere(p, 0.1), 2, pos=(-0.68, 0.62, 0)),
             Part(lambda p: sd_sphere(p, 0.1), 2, pos=(0.68, -0.66, 0)), Part(lambda p: sd_sphere(p, 0.1), 2, pos=(-0.68, -0.66, 0))]
    for q in parts:
        q.rot = rotz(sway) @ q.rot
        q.pos = rotz(sway) @ q.pos
    mats = [Material(c1, c2, metal=0.5), Material('#e8dcc8', '#ffffff', metal=0.0, gloss=0.3, emissive=c2, pattern=sheet),
            Material(c2, '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.1, view_yaw=0.25, zoom=1.05), None


def unified_dossier(c1, c2, t):
    """An open gold-bound book; the lines on both pages flow into one point of light above the spine."""
    def page(p, n):
        lines = np.abs(np.mod(p[..., 2] * 8, 1) - 0.5) < 0.12
        flow = np.mod(np.abs(p[..., 0]) * 3 + t * 2, 1) < 0.35
        return -0.12 * lines, (lines & flow & (n[..., 1] > 0.3)).astype(float)
    L = lambda p: sd_box(p, (0.36, 0.035, 0.46), 0.01)
    parts = [Part(L, 0, pos=(-0.36, 0, 0), rot=rotz(-0.28)), Part(L, 0, pos=(0.36, 0, 0), rot=rotz(0.28)),
             Part(lambda p: sd_box(p, (0.4, 0.03, 0.5), 0.02), 1, pos=(-0.38, -0.08, 0), rot=rotz(-0.28)),
             Part(lambda p: sd_box(p, (0.4, 0.03, 0.5), 0.02), 1, pos=(0.38, -0.08, 0), rot=rotz(0.28)),
             Part(lambda p: sd_sphere(p, 0.13 + 0.03 * math.sin(TAU * t * 2)), 2, pos=(0, 0.42, 0)),
             Part(lambda p: sd_torus(p, 0.24, 0.025), 2, pos=(0, 0.42, 0), rot=rotx(1.2) @ rotz(TAU * t))]
    mats = [Material('#f0e4c0', c2, metal=0.0, gloss=0.4, emissive=c2, pattern=page), Material(c1, c2, metal=1.0),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.15, zoom=1.08), None


# ============================================================================ chips: components
def assembly_matrix(c1, c2, t):
    """A 3x3 femtoscale jig; parts snap into its cells one by one, each cell flashing as it locks.
    Loop: nine parts lock in (t < 0.6), the full jig holds, then the finished set sinks out (t > 0.8) so the loop
    ends empty, exactly like it starts."""
    nfill = int(t / 0.6 * 9) + 1 if t < 0.6 else 10
    out = max(0.0, (t - 0.8) / 0.2)
    out = out * out * (3 - 2 * out)            # 0 -> 1: parts shrink down into the jig
    order = [4, 0, 8, 2, 6, 1, 7, 3, 5]
    filled = set(order[:min(9, nfill)])
    sp = 0.4
    def lattice(p):
        outer = sd_box(p, (0.62, 0.07, 0.62), 0.02)
        qx = p[..., 0] - sp * np.clip(np.round(p[..., 0] / sp), -1, 1)
        qz = p[..., 2] - sp * np.clip(np.round(p[..., 2] / sp), -1, 1)
        hole = sd_box(np.stack([qx, p[..., 1], qz], -1), (0.15, 0.2, 0.15))
        return np.maximum(outer, -hole)
    def blocks(p):
        ix = np.clip(np.round(p[..., 0] / sp), -1, 1)
        iz = np.clip(np.round(p[..., 2] / sp), -1, 1)
        idx = (ix + 1) + (iz + 1) * 3
        ok = np.isin(idx, list(filled))
        q = np.stack([p[..., 0] - sp * ix, p[..., 1] - 0.06 + 0.12 * out, p[..., 2] - sp * iz], -1)
        k = 1 - out
        d = sd_box(q, (0.12 * k, 0.12 * k, 0.12 * k), 0.03 * k)
        return np.where(ok, d, np.maximum(d, 0.2))
    last = order[min(8, max(0, nfill - 1))]
    lx, lz = (last % 3 - 1) * sp, (last // 3 - 1) * sp
    flash = 1.0 if (t / 0.6 * 9) % 1 < 0.5 and t < 0.6 else 0.0
    def glow(p, n):
        near = (np.abs(p[..., 0] - lx) < 0.2) & (np.abs(p[..., 2] - lz) < 0.2)
        return _zero(p), (near * flash + 0 * (np.abs(np.mod(p[..., 0] / sp + 0.5, 1) - 0.5) > 0.4)).astype(float)
    parts = [Part(lattice, 0)]
    if filled and out < 0.97:
        parts.append(Part(blocks, 1))
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=glow), Material('#c8d4e0', c2, metal=1.0, pattern=pat_noise_shade(20, 0.1))]
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.6, zoom=1.1, steps=70), None


def atto_transistor(c1, c2, t):
    """A tiny three-legged transistor that flips state a moment *before* the gate pulse climbing its leg arrives."""
    state = 1 if (0.38 <= t < 0.88) else 0
    def body_pat(p, n):
        top = p[..., 1] > 0.02
        on = top if state else ~top
        return _zero(p), (on & (p[..., 2] < -0.05)).astype(float)
    def gate(p, n):
        y = p[..., 1]
        ph = (t + 0.0) % 0.5 / 0.5
        return _zero(p), (np.abs(y - (-0.9 + ph * 0.6)) < 0.07).astype(float)
    body = lambda p: np.maximum(sd_cyl(p, 0.36, 0.34, 0.06), p[..., 2] - 0.14)
    parts = [Part(body, 0, pos=(0, 0.15, 0), rot=rotx(0)),
             Part(lambda p: sd_box(p, (0.035, 0.34, 0.035)), 1, pos=(-0.2, -0.52, 0)),
             Part(lambda p: sd_box(p, (0.035, 0.34, 0.035)), 2, pos=(0, -0.52, 0)),
             Part(lambda p: sd_box(p, (0.035, 0.34, 0.035)), 1, pos=(0.2, -0.52, 0))]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=body_pat), Material('#c8c8d0', '#ffffff', metal=1.0),
            Material('#c8c8d0', c2, metal=1.0, emissive=c2, pattern=lambda p, n: gate(p + np.array([0, -0.52, 0]), n))]
    def ov(img):
        if abs(t - 0.38) < 0.03 or abs(t - 0.88) < 0.03:
            _px(img, 22, 7, WHITE, 0.9); _px(img, 23, 6, H(c2), 0.7); _px(img, 21, 8, H(c2), 0.7)
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.3, zoom=1.15), ov


def femto_transistor(c1, c2, t):
    """A gull-wing package carrying one qubit in a ring; the qubit's glowing hemisphere flips from up to down."""
    flip = 0.5 - 0.5 * math.cos(TAU * t)
    ax = rotx(math.pi * flip)
    def spin(p, n):
        return _zero(p), (p[..., 1] > 0.0).astype(float)
    legs = lambda p: sd_box(np.stack([p[..., 0] - 0.2 * np.clip(np.round(p[..., 0] / 0.2), -1, 1), p[..., 1], np.abs(p[..., 2]) - 0.42], -1), (0.04, 0.025, 0.1))
    parts = [Part(lambda p: sd_box(p, (0.38, 0.12, 0.32), 0.03), 0),
             Part(legs, 1, pos=(0, -0.08, 0)),
             Part(lambda p: sd_torus(p, 0.22, 0.03), 2, pos=(0, 0.2, 0)),
             Part(lambda p: sd_sphere(p, 0.17), 3, pos=(0, 0.34, 0), rot=ax)]
    mats = [Material(c1, c2, metal=0.5, pattern=pat_noise_shade(12, 0.08)), Material('#c8a040', '#fff0c0', metal=1.0),
            Material('#c0d0d8', c2, metal=1.0, emissive=c2, pattern=pat_bands(10, 1, t, 0.15, axis=0)),
            Material('#103830', c2, emissive=mix(H(c2), WHITE, 0.3), pattern=spin)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.55, zoom=1.25), None


def engineered_enzyme(c1, c2, t):
    """A globular protein with an active-site pocket: a substrate drifts in, glows, and leaves cut in two."""
    lobes = [(-0.2, 0.05, 0.05, 0.36), (0.18, -0.15, 0.0, 0.32), (0.08, 0.3, -0.05, 0.25), (-0.25, -0.3, -0.05, 0.25), (0.3, 0.18, 0.12, 0.2)]
    pocket = np.array([0.22, 0.24, -0.25])
    def prot(p):
        d = None
        for (x, y, z, r) in lobes:
            di = sd_sphere(p - np.array([x, y, z]), r)
            d = di if d is None else _smin(d, di, 0.12)
        return np.maximum(d, -sd_sphere(p - pocket, 0.2))
    def helix(p, n):
        v = np.sin(p[..., 0] * 9 + p[..., 1] * 12 + np.sin(p[..., 2] * 8) * 2)
        return 0.18 * v, (np.abs(length(p - pocket) - 0.22) < 0.05).astype(float)
    parts = [Part(prot, 0, rot=roty(0.0))]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=helix), Material(c2, '#ffffff', emissive=c2, pattern=pat_all()),
            Material('#ffffff', c2, emissive=mix(H(c2), WHITE, 0.6), pattern=pat_all())]
    target = pocket + np.array([0, 0, 0.02])
    if t < 0.4:
        k = (t / 0.4) ** 0.7
        parts.append(Part(lambda p: sd_sphere(p, 0.1), 1, pos=target + (1 - k) * np.array([1.4, 1.5, -0.4])))
    elif t < 0.6:
        parts.append(Part(lambda p: sd_sphere(p, 0.11), 2, pos=target))
    else:
        k = ((t - 0.6) / 0.4) ** 1.3
        # both halves leave the picture (the old paths stopped in view, then vanished on the loop seam)
        parts.append(Part(lambda p: sd_sphere(p, 0.075), 1, pos=target + k * np.array([1.7, 0.75, -0.4])))
        parts.append(Part(lambda p: sd_sphere(p, 0.075), 1, pos=target + k * np.array([0.4, 1.9, -0.4])))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.12), None


def hotel_key(c1, c2, t):
    """A brass hotel key on a big red fob whose four lamps count room numbers upward in binary, forever: n -> n+1."""
    n = int(t * 16)
    def fob(p, n_):
        x = p[..., 0]
        cell = np.floor((x + 0.4) / 0.2)
        lit = np.zeros(x.shape, bool)
        for b in range(4):
            if (n >> (3 - b)) & 1:
                lit |= (cell == b)
        dot = (np.abs(np.mod((x + 0.4) / 0.2, 1) - 0.5) < 0.32) & (np.abs(p[..., 2]) < 0.1) & (np.abs(x) < 0.4) & (p[..., 1] > 0.05)
        return -0.25 * (dot & ~lit), (dot & lit).astype(float)
    K = rotz(0.5)
    def key(p):
        d = sd_torus(p - np.array([-0.5, 0, 0]), 0.17, 0.055)
        d = np.minimum(d, sd_capsule(p, np.array([-0.33, 0, 0]), np.array([0.45, 0, 0]), 0.055))
        d = np.minimum(d, sd_box(p - np.array([0.36, -0.1, 0]), (0.045, 0.1, 0.04)))
        d = np.minimum(d, sd_box(p - np.array([0.22, -0.08, 0]), (0.045, 0.08, 0.04)))
        return d
    parts = [Part(key, 1, pos=(0.08, 0.4, 0), rot=rotz(-0.35) @ rotx(TAU / 4)),
             Part(lambda p: sd_torus(p, 0.09, 0.03), 1, pos=(-0.4, 0.38, 0.0), rot=rotz(TAU / 4)),
             Part(lambda p: sd_box(p, (0.42, 0.09, 0.2), 0.08), 0, pos=(-0.08, -0.3, 0.05), rot=rotz(-0.2))]
    mats = [Material(c1, c2, metal=0.4, emissive=c2, pattern=fob), Material(c2, '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.65, view_yaw=0.0, zoom=1.1), None


def pixel_tracker_module(c1, c2, t):
    """A pixel sensor: a charged particle's curved track lights up across its cells and fades - a new track each half-loop."""
    k = int(t * 2)
    ph = (t * 2) % 1
    Rc = (0.9, -1.3)[k]
    cz = (0.3, -0.2)[k]
    def sens(p, n):
        x, z = p[..., 0], p[..., 2]
        gx = np.floor(x * 9) / 9 + 1 / 18
        gz = np.floor(z * 9) / 9 + 1 / 18
        cell = (np.abs(np.mod(x * 9, 1) - 0.5) < 0.36) & (np.abs(np.mod(z * 9, 1) - 0.5) < 0.36)
        dist = np.abs(np.sqrt((gx - Rc * 0 + 0.0 - 0) ** 2 * 0 + (gx) ** 2 + (gz - cz - Rc) ** 2) - abs(Rc))
        reach = (gx + 0.62) / 1.24 < ph * 1.6
        hit = (dist < 0.085) & reach & (ph < 0.85) & cell & (n[..., 1] > 0.5)
        return -0.1 * cell, hit.astype(float)
    parts = [Part(lambda p: sd_box(p, (0.62, 0.05, 0.62), 0.01), 0),
             Part(lambda p: np.maximum(sd_box(p, (0.72, 0.08, 0.72), 0.02), -sd_box(p, (0.63, 0.2, 0.63))), 1),
             Part(lambda p: sd_box(p, (0.72, 0.03, 0.12), 0.01), 2, pos=(0, -0.02, 0.8))]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=sens), Material('#8a90a0', '#ffffff', metal=0.9),
            Material('#c8a040', '#fff0c0', metal=1.0, pattern=lambda p, n: (-0.2 * (np.mod(p[..., 0] * 12, 1) < 0.4), _zero(p)))]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.4, zoom=1.05), None


# ============================================================================ chips: dies
def photonic_die(c1, c2, t):
    """A rectangular die laced with waveguides; short pulses of light race along them in alternating directions."""
    def guides(p, n):
        z, x = p[..., 2], p[..., 0]
        lane = np.floor(z * 8)
        on = np.abs(np.mod(z * 8, 1) - 0.5) < 0.18
        dirn = np.where(np.mod(lane, 2) == 0, 1, -1)
        pulse = np.mod(x * 2 * dirn - t * 4 + lane * 0.37, 1) < 0.22
        return -0.15 * on, (on & pulse).astype(float)
    parts, mats = _bare_die(c1, c2, t, guides, 'rect', 0.55)
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.35, zoom=1.15), None


def qubit_die(c1, c2, t):
    """A square die carrying two entangled qubit bumps; a pulse ping-pongs between them along their coupler."""
    ph = 0.5 - 0.5 * math.cos(TAU * t * 2)
    a, b = np.array([-0.26, 0.1, -0.12]), np.array([0.26, 0.1, 0.12])
    def coupler(p, n):
        s = np.clip(((p - a) @ (b - a)) / ((b - a) @ (b - a)), 0, 1)
        return _zero(p), (np.abs(s - ph) < 0.16).astype(float)
    parts, mats = _bare_die(c1, c2, t, pat_grid(5, t, 0.04), 'square', 0.52)
    mats[1] = Material(c1, c2, metal=0.8, pattern=pat_noise_shade(14, 0.08))
    mats += [Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)), Material('#80a0a8', c2, metal=1.0, emissive=mix(H(c2), WHITE, 0.5), pattern=coupler)]
    parts += [Part(lambda p: sd_sphere(p - a, 0.13 + 0.03 * (1 - ph)), 2), Part(lambda p: sd_sphere(p - b, 0.13 + 0.03 * ph), 2),
              Part(lambda p: sd_capsule(p, a, b, 0.035), 3)]
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.6, zoom=1.15), None


def topological_die(c1, c2, t):
    """A hexagonal die of topological insulator: dark inside, with a current chasing round its edge only."""
    def edge(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        q = np.abs(p)
        hexr = np.maximum(q[..., 0] * 0.866 + q[..., 2] * 0.5, q[..., 2])
        rim = np.abs(hexr - 0.38) < 0.06
        chase = np.mod(a / TAU * 3 - t * 3, 1) < 0.5
        return -0.2 * (hexr < 0.32), (rim & chase).astype(float)
    parts, mats = _bare_die(c1, c2, t, edge, 'hex', 0.55)
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.5, zoom=1.15), None


def hyperdimensional_die(c1, c2, t):
    """A die that is larger on the inside: its face is a square tunnel zooming endlessly inward."""
    def tunnel(p, n):
        r = np.maximum(np.abs(p[..., 0]), np.abs(p[..., 2])) + 1e-4
        lv = np.log2(r / 0.45)
        band = np.mod(lv * 2 + t * 2, 1) < 0.35
        return 0.5 * np.clip(lv, -3, 0) / 3, (band & (r < 0.44)).astype(float)
    parts, mats = _bare_die(c1, c2, t, tunnel, 'square', 0.55)
    return parts, mats, dict(view_pitch=0.95, view_yaw=0.785, zoom=1.12), None


def cosmic_die(c1, c2, t):
    """An octagonal die whose face holds a slowly turning spiral galaxy."""
    def gal(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z) + 1e-6
        arms = np.sin(2 * np.arctan2(z, x) - np.log(r) * 3.5 - TAU * t) > 0.35
        return 0.2 * (r < 0.12), ((arms & (r < 0.42) & (r > 0.06)) | (r < 0.08)).astype(float)
    parts, mats = _bare_die(c1, c2, t, gal, 'oct', 0.56)
    mats[1] = Material('#141c38', c2, metal=0.7, emissive=c2, pattern=gal, keep_dark=True)
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.4, zoom=1.12), None


# ============================================================================ chips: processors
def photonic_processor(c1, c2, t):
    """Tier I: a package under a glass lens that flashes - light is how it computes."""
    def lensp(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        ring = np.abs(np.mod(r * 6 - t * 4, 1) - 0.5) < 0.18
        return 0.3 * np.cos(r * 8), ring.astype(float)
    parts, mats = _pkg(c1, c2, t, pat_traces(t, 1))
    parts += [Part(lambda p: np.maximum(sd_sphere(p - np.array([0, -0.12, 0]), 0.4), -p[..., 1] + 0.0), 2, pos=(0, 0.1, 0)),
              Part(lambda p: sd_torus(p, 0.3, 0.035), 3, pos=(0, 0.1, 0))]
    mats += [Material('#a0c8e8', '#ffffff', metal=0.3, gloss=1.8, emissive=c2, pattern=lensp), Material('#c0c8d0', '#ffffff', metal=1.0)]
    col = H(c2)
    def ov(img):
        ph = (t * 2) % 1
        if ph < 0.35:
            a = math.sin(ph / 0.35 * math.pi)
            for k in range(4):
                ang = TAU * k / 4 + 0.785
                for r in range(5, 9):
                    _px(img, 15.5 + math.cos(ang) * r * 1.1, 11 + math.sin(ang) * r * 0.6, col, a * (1 - (r - 5) / 5))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.785, zoom=1.0), ov


def quantum_processor(c1, c2, t):
    """A package with a 3x3 array of logical-qubit domes; X and Z stabilisers blink in alternation (surface code)."""
    phase = int(t * 4) % 2
    def domes(p):
        q = np.stack([p[..., 0] - 0.3 * np.clip(np.round(p[..., 0] / 0.3), -1, 1), p[..., 1],
                      p[..., 2] - 0.3 * np.clip(np.round(p[..., 2] / 0.3), -1, 1)], -1)
        return np.maximum(sd_sphere(q, 0.12), -p[..., 1])
    def blink(p, n):
        ix = np.round(p[..., 0] / 0.3)
        iz = np.round(p[..., 2] / 0.3)
        return _zero(p), (np.mod(ix + iz, 2) == phase).astype(float)
    parts, mats = _pkg(c1, c2, t, None)
    parts += [Part(lambda p: sd_box(p, (0.46, 0.03, 0.46), 0.02), 2, pos=(0, 0.1, 0)), Part(domes, 3, pos=(0, 0.12, 0))]
    mats += [Material('#1a3040', c2, metal=0.9, pattern=pat_grid(7, t, 0.05), emissive=c2),
             Material('#2a6070', c2, emissive=mix(H(c2), WHITE, 0.3), pattern=blink)]
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.785, zoom=0.98), None


def topological_processor(c1, c2, t):
    """Tier III: a two-row package with a Hopf link - two rings that cannot be pulled apart - turning above it."""
    parts, mats = _pkg(c1, c2, t, None, four=False)
    mats[0] = Material(c1, c2, metal=0.5, pattern=pat_noise_shade(10, 0.1))
    R = roty(TAU * t)
    parts += [Part(lambda p: sd_torus(p, 0.3, 0.06), 2, pos=R @ np.array([-0.15, 0.42, 0]), rot=R @ rotx(TAU / 4)),
              Part(lambda p: sd_torus(p, 0.3, 0.06), 3, pos=R @ np.array([0.15, 0.42, 0]), rot=R)]
    mats += [Material(c2, '#ffffff', metal=0.9, emissive=c2, pattern=pat_bands(8, 1, t, 0.2, axis=0)),
             Material('#e0b060', '#ffffff', metal=1.0, emissive='#ffe0a0', pattern=pat_bands(8, -1, t, 0.2, axis=2))]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6, zoom=1.0), None


def hyperdimensional_processor(c1, c2, t):
    """Tier IV: a tesseract hovers over the package - an inner cube turning through an outer one."""
    parts, mats = _pkg(c1, c2, t, pat_grid(5, t, 0.06), four=False)
    a = TAU * t
    parts += [Part(lambda p: sd_frame(p, (0.3, 0.3, 0.3), 0.03), 2, pos=(0, 0.5, 0), rot=roty(a / 2 + 0.3)),
              Part(lambda p: sd_frame(p, (0.15, 0.15, 0.15), 0.025), 3, pos=(0, 0.5, 0), rot=roty(-a) @ rotx(a))]
    mats += [Material('#60a8b0', c2, metal=1.0, emissive=c2, pattern=pat_bands(6, 1, t, 0.15, axis=1)),
             Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6, zoom=1.0), None


def cosmic_processor(c1, c2, t):
    """Tier V: a galaxy turning above a deep-blue package, its rim ring orbiting."""
    def gal(p, n):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z) + 1e-6
        arms = np.sin(2 * np.arctan2(z, x) - np.log(r) * 4 - TAU * t) > 0.3
        return _zero(p), ((arms & (r < 0.4)) | (r < 0.1)).astype(float)
    parts, mats = _pkg(c1, c2, t, None)
    mats[0] = Material('#203060', c2, metal=0.6, pattern=pat_spots(t, 0.85, 2), emissive='#ffffff')
    parts += [Part(lambda p: sd_ellip(p, (0.44, 0.06, 0.44)), 2, pos=(0, 0.42, 0), rot=rotx(0.25)),
              Part(lambda p: sd_torus(p, 0.54, 0.03), 3, pos=(0, 0.42, 0), rot=rotx(0.25) @ rotz(0.15))]
    mats += [Material('#182040', c2, metal=0.8, emissive=c2, pattern=gal, keep_dark=True),
             Material(c2, '#ffffff', metal=1.0, emissive=c2, pattern=pat_bands(10, 2, t, 0.15, axis=0))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.0), None


def ontological_processor(c1, c2, t):
    """Tier VI: a black-green package raining code, with a monolith on it that writes what is true - line by line."""
    def rain(p, n):
        col = np.floor(p[..., 0] * 10)
        sp = 1 + np.mod(col * 7, 3)
        head = np.mod(-p[..., 2] * 1.2 + t * sp + col * 0.31, 1)
        lane = np.abs(np.mod(p[..., 0] * 10, 1) - 0.5) < 0.25
        return _zero(p), (lane & (head < 0.3) & (n[..., 1] > 0.5)).astype(float)
    def scan(p, n):
        return _zero(p), (np.abs(np.mod(p[..., 1] * 2 - t * 2, 1) - 0.5) < 0.12).astype(float)
    parts, mats = _pkg(c1, c2, t, rain, pins=False)
    mats[0] = Material(c1, c2, metal=0.4, emissive=c2, pattern=rain, keep_dark=True)
    parts += [Part(lambda p: sd_box(p, (0.13, 0.42, 0.08), 0.01), 2, pos=(0, 0.5, 0)),
              Part(lambda p: sd_box(p, (0.44, 0.03, 0.44), 0.02), 3, pos=(0, -0.11, 0))]
    mats += [Material('#081a10', c2, metal=0.9, emissive=c2, pattern=scan, keep_dark=True), Material('#c8a040', '#fff0c0', metal=1.0)]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=0.98), None


def transfinite_processor(c1, c2, t):
    """Tier VII: a ziggurat of ever-thinner layers; a pulse climbs it, each step in half the time of the last."""
    k = -math.log2(max(1e-6, 1 - t * 0.999))       # Zeno: level reached at t = 1 - 2^-k
    lvl = min(k, 6.0)
    parts, mats = _pkg(c1, c2, t, None)
    mats[0] = Material('#2a2440', c2, metal=0.6)
    y = 0.1
    mats.append(Material('#ffffff', c2, metal=0.9, emissive=mix(H(c2), WHITE, 0.5), pattern=pat_all()))
    mats.append(Material('#3a3458', c2, metal=0.9, emissive=c2, pattern=lambda p, n: (_zero(p), (np.abs(n[..., 1]) < 0.3).astype(float) * 0.0)))
    for i in range(6):
        s = 0.5 * 0.72 ** i
        hgt = 0.085 * 0.8 ** i
        lit = 2 if int(lvl) == i else 3
        parts.append(Part(lambda p, s=s, hgt=hgt: sd_box(p, (s, hgt, s), 0.005), lit, pos=(0, y + hgt, 0)))
        y += hgt * 2
    def ov(img):
        if t > 0.9:
            a = math.sin((t - 0.9) / 0.1 * math.pi)
            sparkle(img, 8, 3, H(c2), 2 if a > 0.6 else 1)
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.785, zoom=1.0), ov


# ============================================================================ chunks
def arbor_heartwood_sample(c1, c2, t):
    """A round slice of the World Tree: violet bark, growth rings glowing outward, drifting as if it weighed nothing."""
    def rings(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        bark = r > 0.52
        ring = np.abs(np.mod(r * 3.2 - t * 2, 1) - 0.5) < 0.2
        top = n[..., 1] > 0.0
        off = np.where(bark, -0.3 + 0.25 * np.sin(np.arctan2(p[..., 2], p[..., 0]) * 14 + p[..., 1] * 10), 0.12 * np.sin(r * 26))
        return off, (ring & ~bark & (r > 0.05)).astype(float)
    def log(p):
        rr = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        return np.maximum(rr - 0.6 - 0.025 * np.sin(a * 14), np.abs(p[..., 1]) - 0.2) * 0.9
    tilt = 0.25 * math.sin(TAU * t)
    parts = [Part(log, 0, pos=(0, 0.07 * math.sin(TAU * t), 0), rot=rotx(0.35 + tilt * 0.4) @ rotz(tilt))]
    mats = [Material(c1, c2, metal=0.2, emissive=c2, pattern=rings)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5, zoom=1.08), None


def degenerate_matter_chunk(c1, c2, t):
    """A tiny, impossibly dense crystal block that can only hop a little before slamming back down, squashing on impact."""
    ph = (t * 2) % 1
    hop = 0.24 * 4 * ph * (1 - ph)
    squash = math.exp(-((min(ph, 1 - ph)) / 0.05) ** 2) * 0.18
    S = np.array([1 + squash, 1 - squash, 1 + squash])
    def body(p):
        q = p / S
        return np.maximum(sd_box(q, (0.38, 0.38, 0.38), 0.03), sd_octa(q, 0.86)) * (1 - squash)
    def lattice(p, n):
        g = (np.abs(np.mod(p * 5, 1) - 0.5) < 0.13).sum(-1) >= 1
        return -0.1 * g, (g & (np.mod(np.floor(p[..., 0] * 5) + np.floor(p[..., 1] * 5) + np.floor(p[..., 2] * 5) - t * 10, 5) < 1)).astype(float)
    parts = [Part(body, 0, pos=(0, -0.3 + hop + 0.38 * squash * 0, 0), rot=roty(0.4)),
             Part(lambda p: sd_cyl(p, 0.55, 0.03, 0.01), 1, pos=(0, -0.7, 0))]
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=lattice), Material('#302838', '#605070', metal=0.3, keep_dark=True,
            pattern=lambda p, n: (-0.4 * (np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) < 0.45 - hop), _zero(p)))]
    col = H(c2)
    def ov(img):
        if min(ph, 1 - ph) < 0.08:
            a = 1 - min(ph, 1 - ph) / 0.08
            for s in (-1, 1):
                for k in range(3):
                    _px(img, 15.5 + s * (9 + k + (ph if ph < 0.5 else 0) * 10), 25 - k % 2, col * 0.6 + 0.3, a * (1 - k / 3))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6, zoom=1.0), ov


def fractured_essence(c1, c2, t):
    """One splinter of reality in three places at once: the solid copy and two afterimages swap who is real."""
    def shard(p):
        q = p / np.array([0.5, 1.0, 0.4])
        return np.maximum(sd_octa(q, 0.62) * 0.4, -p[..., 1] - 0.45 + 0.2 * p[..., 0])
    def cracks(p, n):
        v = np.abs(np.sin(p[..., 0] * 13 + p[..., 1] * 7) + np.sin(p[..., 1] * 11 - p[..., 2] * 9))
        return 0.1 * np.sin(p[..., 1] * 9), (v < 0.25).astype(float)
    real = int(t * 3) % 3
    parts, mats = [], [Material(c1, c2, metal=0.8, emissive=c2, pattern=cracks),
                       Material('#101820', c2, emissive=c2, keep_dark=True, pattern=lambda p, n: (_zero(p), (np.abs(n[..., 0] * n[..., 2]) > 0.1).astype(float)))]
    for k in range(3):
        a = TAU * (k / 3 + t / 3)
        pos = (math.cos(a) * 0.36, 0.08 * math.sin(a * 2), math.sin(a) * 0.36)
        parts.append(Part(shard, 0 if k == real else 1, pos=pos, rot=rotz(0.4 * math.sin(a - TAU / 3)) @ roty(a)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0), None


def paradox_residue(c1, c2, t):
    """An impossible triangle. Once per loop it turns and the contradiction shows - three bars that never meet - then it closes again."""
    L, w = 0.95, 0.12
    o = np.array([-L / 2, -L / 2, -L / 2])
    P = [o, o + [L, 0, 0], o + [L, L, 0], o + [L, L, L]]
    parts = []
    for k in range(3):
        a, b = P[k], P[k + 1]
        c = (a + b) / 2
        half = np.abs(b - a) / 2 + w
        parts.append(Part(lambda p, c=c, half=half: sd_box(p - c, half, 0.015), k))
    mats = [Material(c1, c2, metal=0.6, pattern=pat_noise_shade(12, 0.06)), Material(mix(H(c1), H(c2), 0.35), c2, metal=0.6),
            Material(mix(H(c1), H(c2), 0.65), c2, metal=0.6, emissive=c2, pattern=pat_spots(t, 0.85, 3))]
    turn = 0.55 * math.sin(math.pi * t) ** 4
    return parts, mats, dict(view_pitch=0.6155, view_yaw=2.3562 + turn, zoom=1.1), None


def primordial_ejecta(c1, c2, t):
    """A white-hot teardrop of matter flung from a white hole, shedding bright droplets and streaks."""
    def drop(p):
        return _smin(sd_sphere(p, 0.4), sd_capsule(p, np.zeros(3), np.array([-0.75, -0.55, 0.1]), 0.07), 0.3)
    def hot(p, n):
        v = np.sin(p[..., 0] * 14 + TAU * t * 2) * np.sin(p[..., 1] * 12 - TAU * t) * np.sin(p[..., 2] * 10)
        return 0.25 * v, (v > -0.2).astype(float)
    jit = 0.02 * math.sin(TAU * t * 5)
    parts = [Part(drop, 0, pos=(0.22 + jit, 0.2, 0))]
    mats = [Material(c1, c2, metal=0.2, emissive='#fffaf0', pattern=hot)]
    def ov(img):
        for k in range(5):
            life = (t * 2 + k / 5) % 1
            x = 20 - life * 18 + k * 0.7
            y = 12 + life * 12 + (k - 2) * 1.6
            for j in range(3):
                _px(img, x + j, y - j * 0.66, WHITE if j == 0 else H(c1), (1 - life) * (1 - j / 3))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.05), ov


def self_healing_polymer(c1, c2, t):
    """A cube of green gel cut clean in two; the halves drift apart and glowing chains pull them back together."""
    g = 0.2 * (0.5 - 0.5 * math.cos(TAU * t))
    nrm = np.array([0.7071, 0, 0.7071])
    def half(sign):
        return lambda p: np.maximum(sd_box(p - sign * g * nrm, (0.42, 0.42, 0.42), 0.12), -sign * ((p - sign * g * nrm) @ nrm) + 0.0)
    parts = [Part(half(1), 0), Part(half(-1), 0)]
    for k, z in enumerate((-0.25, 0.0, 0.25)):
        a = -g * nrm + np.array([0.0, z, 0.0])
        b = g * nrm + np.array([0.0, z, 0.0])
        if g > 0.01:
            parts.append(Part(lambda p, a=a, b=b: sd_capsule(p, a, b, 0.035 + 0.02 * (1 - g / 0.2)), 1))
    mats = [Material(c1, c2, metal=0.6, gloss=1.7, emissive=c2, pattern=pat_combine(pat_noise_shade(7, 0.15), pat_spots(t, 0.9, 6))),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(20, 2, t, 0.3, axis=2))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6, zoom=1.1), None


def spacetime_slag(c1, c2, t):
    """A crumpled ball of spent geometry with its coordinate grid still printed on it; a demon plucks off a useful shard."""
    def crumple(p):
        d = sd_sphere(p, 0.56)
        f = np.abs(np.sin(p[..., 0] * 7 + 1)) + np.abs(np.sin(p[..., 1] * 6 - p[..., 2] * 4)) + np.abs(np.sin(p[..., 2] * 8 + p[..., 0] * 3))
        return (d + 0.07 * f - 0.08) * 0.7
    def grid(p, n):
        w = p + 0.12 * np.sin(p[..., [1, 2, 0]] * 6)
        g = (np.abs(np.mod(w * 4, 1) - 0.5) < 0.09).sum(-1) >= 1
        flow = np.mod(w[..., 0] * 2 + w[..., 1] - t * 2, 1) < 0.5
        return -0.1 * g, (g & flow).astype(float)
    parts = [Part(crumple, 0, rot=roty(0.4))]
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=grid)]
    col = H(c2)
    def ov(img):
        if t > 0.5:
            k = (t - 0.5) / 0.5
            x, y = 21 + k * 9, 11 - k * 7 + 10 * k * k
            _px(img, x, y, WHITE, 1 - k * 0.5)
            _px(img, x - 1, y, col, 0.7 * (1 - k))
            _px(img, x - 2, y + 1, col, 0.4 * (1 - k))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.05, steps=72), ov


def supernova_remnant(c1, c2, t):
    """A torn shell of glowing filaments with a pulsar at its heart, its two beams sweeping round."""
    def shell(p):
        d = np.abs(sd_sphere(p, 0.64)) - 0.045
        hol = np.sin(p[..., 0] * 8 + 1) * np.sin(p[..., 1] * 7) * np.sin(p[..., 2] * 9 + 2)
        return np.maximum(d, (hol - 0.05) * 0.3) * 0.9
    def fil(p, n):
        r = length(p)
        return 0.2 * np.sin(r * 30), (np.sin(p[..., 0] * 17 + p[..., 1] * 11 - TAU * t) > 0.6).astype(float)
    parts = [Part(shell, 0, rot=roty(TAU * t)),          # whole turns only: half a turn did not loop
             Part(lambda p: sd_sphere(p, 0.11), 1)]
    mats = [Material(c1, c2, metal=0.4, emissive=c2, pattern=fil), Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    col = H(c2)
    def ov(img):
        a = TAU * t * 2
        for s in (1, -1):
            for r in range(2, 15):
                x = 15.5 + s * math.cos(a) * r
                y = 15.5 + s * math.sin(a) * r * 0.45
                _px(img, x, y, mix(WHITE, col, r / 15), 0.85 * (1 - r / 16))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.05, steps=72), ov


def tidal_alloy_scrap(c1, c2, t):
    """A bar of alloy being pulled apart by tides: it necks down to a red-hot thread in the middle, then relaxes."""
    st = 0.5 - 0.5 * math.cos(TAU * t)
    def bar(p):
        x = p[..., 0]
        r = 0.2 - (0.08 + 0.08 * st) * np.exp(-(x / (0.16 + 0.1 * st)) ** 2)
        rr = np.sqrt(p[..., 1] ** 2 + (p[..., 2] * 1.4) ** 2)
        d = np.maximum(rr - r, np.abs(x) - (0.7 + 0.08 * st))
        rough = 0.03 * np.sin(p[..., 1] * 25 + p[..., 2] * 20) * (np.abs(x) > 0.6)
        return (d + rough) * 0.7
    def heat(p, n):
        x = p[..., 0]
        return 0.15 * np.sin(x * 30), (np.abs(x) < 0.12 + 0.12 * st).astype(float)
    parts = [Part(bar, 0, rot=rotz(0.62) @ rotx(TAU * t * 0.0 + 0.4))]
    mats = [Material(c1, c2, metal=0.9, emissive=mix(H(c2), WHITE, 0.15 * st), pattern=heat)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.0, steps=72), None


ITEMS = {
    'autonomous_calibrator': autonomous_calibrator, 'general_ai_core': general_ai_core, 'narrow_ai_core': narrow_ai_core,
    'superintelligence_core': superintelligence_core,
    'field_calibrator': field_calibrator, 'platonic_compass': platonic_compass, 'resonance_probe': resonance_probe,
    'spectral_analyzer': spectral_analyzer, 'structure_assembler': structure_assembler,
    'anchor_core': anchor_core, 'brane_tether': brane_tether, 'geodesic_anchor': geodesic_anchor, 'rift_anchor': rift_anchor,
    'bootstrap_artifact': bootstrap_artifact, 'form_of_the_self': form_of_the_self,
    'gauge_boson_cluster': gauge_boson_cluster, 'qubit': qubit, 'boltzmann_brain': boltzmann_brain, 'neural_organoid': neural_organoid,
    'd_brane_sheet': d_brane_sheet, 'false_vacuum_membrane': false_vacuum_membrane, 'm_brane': m_brane, 'mobius_strip': mobius_strip,
    'atto_capacitor': atto_capacitor, 'femto_capacitor': femto_capacitor, 'higgs_field_capacitor': higgs_field_capacitor,
    'rf_resonator_cell': rf_resonator_cell,
    'absolute_dossier': absolute_dossier,
    'empirical_dossier': empirical_dossier,
    'theoretical_dossier': theoretical_dossier,
    'unified_dossier': unified_dossier,
    'assembly_matrix': assembly_matrix,
    'atto_transistor': atto_transistor,
    'femto_transistor': femto_transistor,
    'engineered_enzyme': engineered_enzyme,
    'hotel_key': hotel_key,
    'pixel_tracker_module': pixel_tracker_module,
    'photonic_die': photonic_die,
    'qubit_die': qubit_die,
    'topological_die': topological_die,
    'hyperdimensional_die': hyperdimensional_die,
    'cosmic_die': cosmic_die,
    'photonic_processor': photonic_processor,
    'quantum_processor': quantum_processor,
    'topological_processor': topological_processor,
    'hyperdimensional_processor': hyperdimensional_processor,
    'cosmic_processor': cosmic_processor,
    'ontological_processor': ontological_processor,
    'transfinite_processor': transfinite_processor,
    'arbor_heartwood_sample': arbor_heartwood_sample,
    'degenerate_matter_chunk': degenerate_matter_chunk,
    'fractured_essence': fractured_essence,
    'paradox_residue': paradox_residue,
    'primordial_ejecta': primordial_ejecta,
    'self_healing_polymer': self_healing_polymer,
    'spacetime_slag': spacetime_slag,
    'supernova_remnant': supernova_remnant,
    'tidal_alloy_scrap': tidal_alloy_scrap,
}

RECIPES = {
    'autonomous_calibrator': ((), ()),
    'general_ai_core': (('breathe',), ()),
    'narrow_ai_core': ((), ()),
    'superintelligence_core': (('pulse',), ('sparkles',)),
    'field_calibrator': ((), ()),
    'platonic_compass': (('rock',), ('sparkles',)),
    'resonance_probe': (('bob',), ()),
    'spectral_analyzer': ((), ('glint',)),
    'structure_assembler': ((), ()),
    'anchor_core': ((), ()),
    'brane_tether': ((), ()),
    'geodesic_anchor': (('rock',), ()),
    'rift_anchor': (('breathe',), ('glitch',)),
    'bootstrap_artifact': ((), ('glint',)),
    'form_of_the_self': (('spin',), ('sparkles',)),
    'gauge_boson_cluster': ((), ()),
    'qubit': ((), ()),
    'boltzmann_brain': ((), ()),
    'neural_organoid': (('heartbeat',), ()),
    'd_brane_sheet': (('rock',), ()),
    'false_vacuum_membrane': (('tumble',), ()),
    'm_brane': (('hue_soft',), ('sparkles',)),
    'mobius_strip': (('rock',), ()),
    'atto_capacitor': ((), ()),
    'femto_capacitor': (('bob',), ()),
    'higgs_field_capacitor': ((), ('glint',)),
    'rf_resonator_cell': (('rock',), ('embers',)),
    'absolute_dossier': ((), ('glint',)),
    'empirical_dossier': (('rock',), ()),
    'theoretical_dossier': ((), ()),
    'unified_dossier': (('bob',), ('sparkles',)),
    'assembly_matrix': ((), ()),
    'atto_transistor': ((), ('glitch',)),
    'femto_transistor': (('rock',), ()),
    'engineered_enzyme': (('rock',), ()),
    'hotel_key': (('rock',), ('glint',)),
    'pixel_tracker_module': ((), ()),
    'photonic_die': ((), ('glint',)),
    'qubit_die': (('rock',), ()),
    'topological_die': (('spin',), ()),
    'hyperdimensional_die': ((), ()),
    'cosmic_die': (('rock',), ('sparkles',)),
    'photonic_processor': ((), ()),
    'quantum_processor': (('breathe',), ()),
    'topological_processor': ((), ('glint',)),
    'hyperdimensional_processor': (('bob',), ()),
    'cosmic_processor': (('spin',), ('sparkles',)),
    'ontological_processor': (('rock',), ()),
    'transfinite_processor': (('spin',), ()),
    'arbor_heartwood_sample': ((), ('embers',)),
    'degenerate_matter_chunk': ((), ()),
    'fractured_essence': (('hue_soft',), ('glitch',)),
    'paradox_residue': ((), ('embers',)),
    'primordial_ejecta': ((), ('sparkles',)),
    'self_healing_polymer': (('rock',), ()),
    'spacetime_slag': (('tumble',), ()),
    'supernova_remnant': ((), ()),
    'tidal_alloy_scrap': (('rock',), ('embers',)),
}

NARRATIVES = {}
FRAMES = {}
