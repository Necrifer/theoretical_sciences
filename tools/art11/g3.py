"""
Round-11 item art, group 3: lenses, loops, manifolds, negentropy devices, omega-tier catalysts, orbs, particle
pairs, pellets and plates. Every item gets its own model, surface and motion that says what it *is*.
"""
import colorsys
import math
import numpy as np
from items3d import *
import anim

# tree_simulacrum and omnilex used to share one generic "growing tree" story loop; they now have their own
# models below, so drop the shared narrative (it would otherwise take precedence over ITEMS).
for _k in ('tree_simulacrum', 'omnilex'):
    anim.NARRATIVES.pop(_k, None)


# ============================================================================== helpers
def _n(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def _frame(nrm):
    """Rotation whose local y axis points along nrm (Part.rot convention: columns = world images of local axes)."""
    nrm = _n(nrm)
    a = np.array([1.0, 0, 0]) if abs(nrm[0]) < 0.9 else np.array([0, 0, 1.0])
    e1 = _n(np.cross(a, nrm))
    e2 = np.cross(e1, nrm)
    return np.column_stack([e1, nrm, e2])


def _segs(A, B, r):
    """Union of many capsules in one SDF (vectorised)."""
    A = np.asarray(A, float)
    B = np.asarray(B, float)
    BA = B - A
    L2 = np.maximum((BA * BA).sum(-1), 1e-9)
    r = np.broadcast_to(np.asarray(r, float), (len(A),)).copy()

    def f(p):
        pa = p[..., None, :] - A
        h = np.clip((pa * BA).sum(-1) / L2, 0, 1)
        return (np.sqrt(((pa - BA * h[..., None]) ** 2).sum(-1)) - r).min(-1)
    return f


def _poly(pts, r, closed=False):
    pts = np.asarray(pts, float)
    if closed:
        return _segs(pts, np.roll(pts, -1, 0), r)
    return _segs(pts[:-1], pts[1:], r)


def _balls(C, R):
    C = np.asarray(C, float)
    R = np.broadcast_to(np.asarray(R, float), (len(C),)).copy()

    def f(p):
        return (np.sqrt(((p[..., None, :] - C) ** 2).sum(-1)) - R).min(-1)
    return f


def _boxframe(b, e):
    def f(p):
        q0 = np.abs(p) - b
        q = np.abs(q0 + e) - e

        def L(x, y, z):
            return np.sqrt(np.maximum(x, 0) ** 2 + np.maximum(y, 0) ** 2 + np.maximum(z, 0) ** 2) + np.minimum(np.maximum(x, np.maximum(y, z)), 0)
        return np.minimum(np.minimum(L(q0[..., 0], q[..., 1], q[..., 2]), L(q[..., 0], q0[..., 1], q[..., 2])), L(q[..., 0], q[..., 1], q0[..., 2]))
    return f


def _hsv(h, s=1.0, v=1.0):
    return np.array(colorsys.hsv_to_rgb(h % 1.0, s, v))


def _dot(img, x, y, rgb, a=1.0, behind=False):
    n = img.shape[0]
    x, y = int(round(x)), int(round(y))
    if 0 <= x < n and 0 <= y < n:
        if behind and img[y, x, 3] > 0.5:
            return
        anim._blend(img, y, x, np.asarray(rgb, float), min(1.0, a))


def _star(img, x, y, rgb, s):
    """Four-point flash of size s (0..1) at 32-px coords."""
    if s <= 0:
        return
    arm = int(round(s * 3))
    for d in range(1, arm + 1):
        a = s * (1 - d / (arm + 1))
        for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
            _dot(img, x + dx, y + dy, rgb, a)
    _dot(img, x, y, WHITE, s)


def _zero(p):
    return np.zeros(p.shape[:-1])


def _wafer_disk(p, r=0.78, h=0.05):
    return np.maximum(sd_cyl(p, r, h, 0.02), -(p[..., 2] + r - 0.05))


def _cyl_pr(p):
    return np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2), np.arctan2(p[..., 2], p[..., 0])


# ============================================================================== lenses
def dark_matter_lens(c1, c2, t):
    # a lens of dark glass that is barely there: only its rim and the light it bends from *behind* give it away
    lens = lambda p: sd_sphere(p / np.array([0.5, 0.14, 0.5]), 1.0) * 0.14
    parts = [Part(lens, 0), Part(lambda p: sd_torus(p, 0.52, 0.035), 1)]
    mats = [Material('#05030c', '#2a2050', metal=1.0, gloss=0.6, keep_dark=True),
            Material(c1, c2, emissive=c2, pattern=pat_bands(6, 1, t, 0.12, axis=0))]

    def ov(img):
        # an Einstein ring of background starlight, two bright arcs sliding around it
        n = img.shape[0]
        c = (n - 1) / 2
        ys, xs = np.mgrid[0:n, 0:n]
        dx, dy = (xs - c) / 12.5, (ys - c - 0.5) / 10.0
        r = np.sqrt(dx * dx + dy * dy)
        a = np.arctan2(dy, dx)
        ring = (np.abs(r - 1.0) < 0.09) & (img[..., 3] < 0.5)
        k = np.clip(np.cos(2 * (a - t * TAU)), 0, 1) ** 2
        col = H(c2) * 0.6 + 0.4
        sel = ring & (k > 0.12)
        img[sel, :3] = col * (0.35 + 0.65 * k[sel][:, None])
        img[sel, 3] = 0.4 + 0.6 * k[sel]
    return parts, mats, dict(view_pitch=0.95, view_yaw=0.4, zoom=1.05), ov


def gravitic_lens_element(c1, c2, t):
    # a square-trimmed dark lens in a hexagonal mount; its Einstein ring breathes in and locks onto the crosshair
    lens = lambda p: np.maximum(sd_sphere(p / np.array([0.62, 0.16, 0.62]), 1.0) * 0.16, sd_box(p, (0.42, 0.3, 0.42)))
    mount = lambda p: np.maximum(sd_hexprism(p, 0.72, 0.07), -sd_hexprism(p, 0.52, 0.3))
    R = 0.3 + 0.07 * math.cos(t * TAU) ** 2

    def ring(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        cross = (np.abs(p[..., 0]) < 0.025) | (np.abs(p[..., 2]) < 0.025)
        return -0.25 * cross, (np.abs(r - R) < 0.045).astype(float)

    def ticks(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return _zero(p), (np.abs(np.angle(np.exp(1j * (a * 6 + 0.5)))) < 0.35).astype(float) * (n[..., 1] > 0.5)
    parts = [Part(lens, 0, rot=roty(0.785)), Part(mount, 1, rot=roty(0.0))]
    mats = [Material('#181030', c2, metal=0.9, gloss=1.5, emissive=c2, pattern=ring),
            Material('#6a6088', '#ffffff', metal=1.0, emissive=c2, pattern=ticks)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.3), None


def photonic_lens(c1, c2, t):
    # a clear lithography lens in a silver barrel; parallel light enters and converges to a burning focus
    barrel = lambda p: np.maximum(sd_cyl(p, 0.64, 0.13, 0.02), -sd_cyl(p, 0.53, 0.5))
    glass = lambda p: sd_sphere(p / np.array([0.53, 0.15, 0.53]), 1.0) * 0.15

    def sheen(p, n):
        return 0.3 * np.sin(p[..., 0] * 4 + p[..., 2] * 3 - t * TAU), (np.abs(p[..., 0] - p[..., 2] - (0.2 + 0.1 * math.sin(t * TAU))) < 0.07).astype(float)
    rot = rotz(1.5708) @ rotx(0.0)
    parts = [Part(barrel, 0, rot=rot), Part(glass, 1, rot=rot)]
    mats = [Material('#a8b0c0', '#ffffff', metal=1.0), Material(c1, c2, metal=0.2, gloss=1.8, emissive='#ffffff', pattern=sheen)]

    def ov(img):
        cx, cy = 14, 15.5
        fx = 29
        for k, y0 in enumerate((8, 15.5, 23)):
            for x in range(0, 31):
                if x < cx - 3:
                    y = y0
                else:
                    y = y0 + (cy - y0) * (x - (cx - 3)) / (fx - (cx - 3))
                if x > fx:
                    continue
                on = ((x - t * 40) % 8) < 4
                inside = img[int(round(y)), x, 3] > 0.5
                _dot(img, x, y, H('#e0f8ff') if on else H(c1), (0.85 if on else 0.35) * (0.5 if inside else 1.0))
        _star(img, fx, cy, H(c1), 0.6 + 0.4 * math.sin(t * TAU * 2))
    return parts, mats, dict(view_pitch=0.3, view_yaw=-0.75, zoom=1.0), ov


def tesseract_lens(c1, c2, t):
    # a lens held inside a rotating hypercube frame; the glass shows squares within squares falling inward
    lens = lambda p: sd_sphere(p / np.array([0.5, 0.12, 0.5]), 1.0) * 0.12

    def squares(p, n):
        m = np.maximum(np.abs(p[..., 0]), np.abs(p[..., 2]))
        return _zero(p), (np.sin(m * 16 + t * TAU * 2) > 0.3).astype(float)
    parts = [Part(_boxframe(np.array([0.6, 0.6, 0.6]), 0.045), 0, rot=rotx(0.25) @ roty(t * TAU / 4)),
             Part(_boxframe(np.array([0.3, 0.3, 0.3]), 0.03), 2, rot=rotx(0.25) @ roty(-t * TAU / 4) @ rotx(0.0)),
             Part(lens, 1, rot=rotx(0.25))]
    mats = [Material(c1, '#ffffff', metal=1.0, emissive=c1, pattern=pat_bands(8, 1, t, 0.1, axis=1)),
            Material('#402060', c2, metal=0.4, gloss=1.6, emissive=c2, pattern=squares),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.6), None


# ============================================================================== loops
def _ctc_scene(c1, c2, t):
    """Gold worldline ring with a bead; when the bead passes 'now' it meets its own future copy."""
    def arrows(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return _zero(p), (np.sin(a * 8 - t * TAU * 4) > 0.55).astype(float)
    parts = [Part(lambda p: sd_torus(p, 0.62, 0.075), 0, rot=rotz(0.3)),
             Part(lambda p: sd_cyl(p, 0.11, 0.1, 0.02), 2, pos=(0, 0, 0), rot=rotx(0.5) @ rotz(0.3) @ rotz(0.0))]
    # the 'now' marker: a small light-cone (two cones point to point) sitting on the ring
    R = rotz(0.3)
    now = R @ np.array([0.62, 0, 0])
    cone = lambda p: np.maximum(np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) * 0.8 - np.abs(p[..., 1]) * 0.6, np.abs(p[..., 1]) - 0.2)
    parts[1] = Part(cone, 2, pos=now, rot=R)
    mats = [Material(c1, c2, metal=0.95, emissive=c2, pattern=arrows),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()),
            Material('#806020', '#fff0c0', metal=0.6, emissive='#fff0c0', pattern=pat_bands(10, 2, t, 0.2, axis=1))]
    a = TAU * 2 * t
    pos_a = R @ np.array([math.cos(a) * 0.62, 0, math.sin(a) * 0.62])
    parts.append(Part(lambda p: sd_sphere(p, 0.12), 1, pos=pos_a))
    # after the first lap (t=0.5) a second copy exists, half a lap behind, fading out by the loop's end
    if t >= 0.5:
        b = a + math.pi
        s = 0.12 * (1 - (t - 0.5) / 0.5) ** 0.5
        if s > 0.03:
            parts.append(Part(lambda p, s=s: sd_sphere(p, s), 1, pos=R @ np.array([math.cos(b) * 0.62, 0, math.sin(b) * 0.62])))
    return parts, mats, R


def nar_closed_timelike_curve(c1, c2, t):
    parts, mats, R = _ctc_scene(c1, c2, t)

    def ov(img):
        if abs(t - 0.5) < 0.05:
            _star(img, 15.5, 15.5, H(c2), 1 - abs(t - 0.5) / 0.05)
        anim.ov_sparkles(img, t, 21, H(c2), 3, 2)
    return parts, mats, dict(view_pitch=0.95, view_yaw=0.5), ov


def closed_timelike_curve(c1, c2, t):
    parts, mats, _ = _ctc_scene(c1, c2, t)
    return parts, mats, dict(view_pitch=0.95, view_yaw=0.5), None


def compactified_circle(c1, c2, t):
    # a long hollow tube: one dimension is the length, the other is rolled into a circle you can see down
    tube = lambda p: np.maximum(np.maximum(sd_cyl(p, 0.24, 0.82, 0.02), -sd_cyl(p, 0.17, 1.0)), 0)

    def wind(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        v = np.sin(a * 2 + p[..., 1] * 9 - t * TAU * 2)
        return 0.15 * np.cos(a * 2), (v > 0.6).astype(float)
    R = rotz(-1.0) @ rotx(0.35)
    parts = [Part(tube, 0, rot=R),
             Part(lambda p: sd_torus(p, 0.24, 0.035), 1, pos=R @ np.array([0, 0.82, 0]), rot=R),
             Part(lambda p: sd_torus(p, 0.24, 0.035), 1, pos=R @ np.array([0, -0.82, 0]), rot=R)]
    # a string wound once round the circle, sliding around the circumference
    a = t * TAU
    parts.append(Part(lambda p: sd_sphere(p, 0.08), 1, pos=R @ np.array([math.cos(a) * 0.27, 0.2 * math.sin(a * 2), math.sin(a) * 0.27])))
    mats = [Material(c1, c2, metal=0.8, emissive=c2, pattern=wind), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.4), None


def hopf_fibration(c1, c2, t):
    # Villarceau circles of a torus: every ring links every other; the family slides round its axis
    Rb, rs = 0.5, 0.25
    th = math.asin(rs / Rb)
    e1 = np.array([math.cos(th), 0, math.sin(th)])
    e2 = np.array([0, 1.0, 0])
    nn = np.cross(e1, e2)
    base = np.column_stack([e1, nn, e2])
    tilt = rotx(0.55)
    parts = []
    cols = [H(c2), WHITE, mix(H(c2), H('#ff80ff'), 0.6)]
    mats = [Material(mix(col, H(c1), 0.45), col, metal=0.7, emissive=col, pattern=pat_bands(6, 1, t, 0.25, axis=0)) for col in cols]
    nring = 6
    for k in range(nring):
        phi = (k + 3 * t) * TAU / nring
        Rz = np.array([[math.cos(phi), -math.sin(phi), 0], [math.sin(phi), math.cos(phi), 0], [0, 0, 1]])
        parts.append(Part(lambda p: sd_torus(p, Rb, 0.04), k % 3, pos=tilt @ Rz @ np.array([0, rs, 0]), rot=tilt @ Rz @ base))
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.2, zoom=1.1), None


def recombinant_dna(c1, c2, t):
    # a double helix turning in place; one stretch of it is spliced in from another creature (orange)
    rr, k = 0.3, 4.6
    ph = t * TAU
    lim = 0.82
    fix = 1 / math.sqrt(1 + (k * rr) ** 2)

    def strand(off):
        def f(p):
            a = k * p[..., 1] + ph + off
            d = np.sqrt((p[..., 0] - rr * np.cos(a)) ** 2 + (p[..., 2] - rr * np.sin(a)) ** 2) * fix - 0.075
            return np.maximum(d, np.abs(p[..., 1]) - lim)
        return f

    def rungs(p):
        s = 0.16
        yq = np.clip(np.round(p[..., 1] / s) * s, -lim + 0.05, lim - 0.05)
        a = k * yq + ph
        A = np.stack([rr * np.cos(a), yq, rr * np.sin(a)], -1)
        pa = p - A
        ba = -2 * np.stack([rr * np.cos(a), np.zeros_like(a), rr * np.sin(a)], -1)
        h = np.clip((pa * ba).sum(-1) / (ba * ba).sum(-1), 0, 1)
        return np.sqrt(((pa - ba * h[..., None]) ** 2).sum(-1)) - 0.035

    def splice(p, n):
        y = p[..., 1]
        return _zero(p), ((y > -0.15) & (y < 0.3)).astype(float)

    def pairs(p, n):
        yq = np.round(p[..., 1] / 0.16)
        return 0.2 * (np.mod(yq, 2) - 0.5), (np.mod(yq, 4) == 0).astype(float)
    R = rotz(0.35)
    parts = [Part(strand(0.0), 0, rot=R), Part(strand(math.pi), 0, rot=R), Part(rungs, 1, rot=R)]
    mats = [Material(c2, '#ffffff', metal=0.5, emissive='#ffa030', pattern=splice),
            Material('#a0c0a8', c2, metal=0.3, emissive=c2, pattern=pairs)]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.2), None


def temporal_knot(c1, c2, t):
    # a trefoil worldline: two sparks chase each other round it (untie it and the event happens twice)
    def kp(u):
        return 0.19 * np.array([math.sin(u) + 2 * math.sin(2 * u), math.cos(u) - 2 * math.cos(2 * u), -1.2 * math.sin(3 * u)])
    pts = [kp(i / 60 * TAU) for i in range(60)]
    R = np.eye(3)

    def flow(p, n):
        a = np.arctan2(p[..., 1], p[..., 0])
        return _zero(p), (np.sin(a * 4 - t * TAU * 4 / 3 * 3) > 0.7).astype(float)
    parts = [Part(_poly(pts, 0.075, closed=True), 0, rot=R)]
    mats = [Material(mix(H(c2), H(c1), 0.55), c2, metal=0.9, emissive=c2, pattern=flow), Material('#ffffff', '#ffffff', emissive='#fff4d0', pattern=pat_all())]
    for k in range(2):
        u = t * TAU + k * math.pi
        parts.append(Part(lambda p: sd_sphere(p, 0.11), 1, pos=R @ kp(u)))
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.15, zoom=1.05), None


def well_ordering_charm(c1, c2, t):
    # a charm of beads, each smaller than the last; a light runs down them and always stops at the least one
    n = 7
    pos, rad = [], []
    for i in range(n):
        a = 2.6 - i * 0.62
        pos.append(np.array([math.cos(a) * 0.5 + 0.05, math.sin(a) * 0.5 - 0.05, 0.0]))
        rad.append(0.24 * 0.78 ** i)
    lit = int(t * n) % n
    parts = [Part(lambda p: sd_torus(p, 0.12, 0.04), 2, pos=pos[0] + np.array([0, 0.36, 0]), rot=rotx(1.5708)),
             Part(_poly([pos[0] + np.array([0, 0.25, 0])] + pos, 0.03), 2)]
    for i in range(n):
        m = 1 if (i == lit or i == n - 1) else 0
        parts.append(Part(lambda p, r=rad[i]: sd_sphere(p, r), m, pos=pos[i]))
    mats = [Material('#4048a0', '#a0a0ff', metal=0.8, gloss=1.3),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_all()),
            Material('#c0a060', '#fff0c0', metal=1.0)]

    def ov(img):
        if t > 0.86:
            pass
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.1, zoom=1.05), None


# ============================================================================== manifolds
def calabi_yau_manifold(c1, c2, t):
    # a petalled, self-folding surface - five lobes that each fold back through the centre
    def f(p):
        r = np.sqrt((p * p).sum(-1)) + 1e-6
        ny = p[..., 1] / r
        phi = np.arctan2(p[..., 2], p[..., 0])
        lobes = 0.18 * np.cos(5 * phi + 2.0 * ny) * (1 - ny * ny) + 0.1 * np.cos(3 * ny * math.pi) * ny
        return (r - 0.46 - lobes) * 0.55

    def pat(p, n):
        phi = np.arctan2(p[..., 2], p[..., 0])
        v = np.sin(phi * 5 + p[..., 1] * 9 - t * TAU * 2)
        return 0.25 * n[..., 1], (v > 0.55).astype(float)
    parts = [Part(f, 0, rot=rotx(0.35) @ roty(t * TAU / 5))]
    mats = [Material(c1, c2, metal=0.7, gloss=1.3, emissive=c2, pattern=pat)]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.4, zoom=1.0), None


def differential_manifold(c1, c2, t):
    # a curved saddle sheet with its coordinate grid; a small flat tangent plane slides over it
    k = 0.42
    fz = lambda x, z: k * (x * x - z * z)

    def sheet(p):
        x, z = p[..., 0], p[..., 2]
        g = np.sqrt(1 + (2 * k * x) ** 2 + (2 * k * z) ** 2)
        d = np.abs(p[..., 1] - fz(x, z)) / g - 0.07
        return np.maximum(d, np.sqrt(x * x + z * z) - 0.74)

    def grid(p, n):
        gx = np.abs(np.mod(p[..., 0] * 2.0 + 0.5, 1) - 0.5) < 0.12
        gz = np.abs(np.mod(p[..., 2] * 2.0 + 0.5, 1) - 0.5) < 0.12
        return _zero(p), (gx | gz).astype(float)
    a = t * TAU
    x0, z0 = math.cos(a) * 0.42, math.sin(a) * 0.42
    nrm = np.array([-2 * k * x0, 1.0, 2 * k * z0])
    nrm = _n(nrm)
    parts = [Part(sheet, 0), Part(lambda p: sd_box(p, (0.2, 0.015, 0.2), 0.01), 1, pos=np.array([x0, fz(x0, z0), z0]) + nrm * 0.06, rot=_frame(nrm))]
    mats = [Material('#284060', c2, metal=0.6, gloss=0.6, emissive=c2, pattern=grid),
            Material('#e0f0ff', '#ffffff', metal=0.3, emissive='#ffffff', pattern=lambda p, n: (_zero(p), (np.maximum(np.abs(p[..., 0]), np.abs(p[..., 2])) > 0.15).astype(float)))]
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.5, zoom=1.02), None


def g2_manifold(c1, c2, t):
    # the Fano plane - seven points, seven lines - whose symmetry is G2's: one line lights up at a time
    V = [np.array([math.cos(a), math.sin(a), 0]) * 0.72 for a in (math.pi / 2, math.pi / 2 + TAU / 3, math.pi / 2 + 2 * TAU / 3)]
    M = [(V[1] + V[2]) / 2, (V[0] + V[2]) / 2, (V[0] + V[1]) / 2]
    C = np.zeros(3)
    lines = [(V[0], V[1]), (V[1], V[2]), (V[2], V[0]), (V[0], M[0]), (V[1], M[1]), (V[2], M[2])]
    A = [l[0] for l in lines]
    B = [l[1] for l in lines]
    rin = 0.36
    lit = int(t * 7) % 7
    R = roty(0.35 * math.sin(t * TAU)) @ rotx(0.15)
    parts = [Part(_segs(A, B, 0.035), 0, rot=R), Part(lambda p: sd_torus(p, rin, 0.035), 0, rot=R @ rotx(1.5708)),
             Part(_balls(V + M + [C], [0.1] * 3 + [0.08] * 3 + [0.09]), 2, rot=R)]
    if lit < 6:
        parts.append(Part(_segs([A[lit]], [B[lit]], 0.06), 1, rot=R))
    else:
        parts.append(Part(lambda p: sd_torus(p, rin, 0.06), 1, rot=R @ rotx(1.5708)))
    mats = [Material(c1, c2, metal=0.8), Material(c2, '#ffffff', emissive=c2, pattern=pat_all()),
            Material('#ffffff', c2, emissive='#ffe0ff', pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.0, zoom=1.0), None


def klein_bottle(c1, c2, t):
    # bulb + a neck that loops over and plunges back in through its own side; a spark rides the one surface
    body = lambda p: sd_sphere(p / np.array([0.42, 0.46, 0.42]), 1.0) * 0.42
    ctrl = [(0.0, 0.2), (0.0, 0.55), (0.1, 0.8), (0.42, 0.82), (0.62, 0.55), (0.62, 0.05), (0.5, -0.35), (0.22, -0.42), (0.0, -0.3)]
    ctrl = np.array([[x, y, 0.0] for x, y in ctrl])
    # Catmull-Rom style smoothing by simple subdivision
    pts = []
    for i in range(len(ctrl) - 1):
        for s in np.linspace(0, 1, 4, endpoint=False):
            pts.append(ctrl[i] * (1 - s) + ctrl[i + 1] * s)
    pts.append(ctrl[-1])
    pts = np.array(pts)
    for _ in range(2):
        pts[1:-1] = (pts[:-2] + pts[1:-1] * 2 + pts[2:]) / 4
    r = np.linspace(0.11, 0.07, len(pts) - 1)
    r[-6:] = np.linspace(0.07, 0.12, 6)
    neck = _segs(pts[:-1], pts[1:], r)
    off = np.array([-0.18, -0.1, 0])

    def flow(p, n):
        return _zero(p), (np.sin((p[..., 1] * 1.0 - p[..., 0] * 0.6) * 14 + t * TAU * 2) > 0.65).astype(float)
    parts = [Part(body, 0, pos=off), Part(neck, 0, pos=off)]
    # the spark: along the neck, then (hidden) through the bulb back to the top
    L = len(pts)
    u = (t * 1.35) % 1.35
    if u < 1:
        q = pts[min(L - 1, int(u * (L - 1)))]
        parts.append(Part(lambda p: sd_sphere(p, 0.08), 1, pos=q + off + np.array([0, 0, -0.1])))
    mats = [Material(c1, c2, metal=0.5, gloss=1.4, emissive=c2, pattern=flow), Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.25, zoom=1.0), None


def omnicellular_genome(c1, c2, t):
    # an X-shaped chromosome; its bands flicker through every colour - one genome for every creature
    arms = [((0, 0.02, 0), (0.3, 0.62, 0)), ((0, 0.02, 0), (-0.3, 0.62, 0)), ((0, -0.02, 0), (0.34, -0.62, 0)), ((0, -0.02, 0), (-0.34, -0.62, 0))]
    f = _segs([a for a, b in arms], [b for a, b in arms], 0.17)
    pinch = lambda p: f(p) + 0.05 * np.exp(-(p[..., 1] / 0.08) ** 2)
    col = _hsv(t, 0.55, 1.0)

    def bands(p, n):
        y = np.abs(p[..., 1])
        b = np.sin(y * 24 - t * TAU * 3)
        return 0.18 * np.sign(np.sin(y * 12)), ((b > 0.55) & (y > 0.1)).astype(float)
    parts = [Part(pinch, 0, rot=roty(0.3)), Part(lambda p: sd_sphere(p, 0.1), 1)]
    mats = [Material('#2a2a2a', c2, metal=0.3, emissive=col, pattern=bands),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.2, zoom=1.05), None


def stabiliser_matrix(c1, c2, t):
    # three rings of quantum string around a rolled-up core, each ringing with a standing wave
    w = math.cos(t * TAU * 2)

    def ring(p):
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        R = 0.66 + 0.07 * np.sin(8 * a) * w
        return (np.sqrt((rho - R) ** 2 + p[..., 1] ** 2) - 0.04) * 0.75

    def anti(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return _zero(p), (np.abs(np.sin(8 * a)) * abs(w) > 0.55).astype(float)
    parts = [Part(ring, 0), Part(ring, 0, rot=rotx(1.5708)), Part(ring, 0, rot=rotz(1.5708)),
             Part(lambda p: sd_sphere(p, 0.3), 1)]
    mats = [Material(c2, '#ffffff', metal=0.7, emissive='#ffffff', pattern=anti),
            Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_grid(7, t, 0.1))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5), None


def transfinite_weave(c1, c2, t):
    # a swatch of gold cloth whose threads get finer and finer toward one edge - more threads than numbers
    def cloth(p):
        x, z = p[..., 0], p[..., 2]
        y0 = 0.07 * np.sin(x * 3 + t * TAU) * np.cos(z * 2.2)
        return np.maximum((np.abs(p[..., 1] - y0) - 0.03) * 0.8, np.maximum(np.abs(x), np.abs(z)) - 0.7)

    def weave(p, n):
        x, z = np.clip(p[..., 0] + 0.7, 0, 1.4), np.clip(p[..., 2] + 0.7, 0, 1.4)
        u = (x / 1.4) ** 1.8 * 11
        v = (z / 1.4) ** 1.8 * 11
        over = np.mod(np.floor(u) + np.floor(v), 2)
        gap = (np.abs(np.mod(u, 1) - 0.5) > 0.4) | (np.abs(np.mod(v, 1) - 0.5) > 0.4)
        glint = np.sin(u * 0.9 + v * 0.9 - t * TAU * 2) > 0.8
        return 0.25 * (over - 0.5) - 0.3 * gap, (glint & ~gap).astype(float)
    parts = [Part(cloth, 0, rot=roty(0.4))]
    mats = [Material(c1, c2, metal=0.7, emissive=c2, pattern=weave)]
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.3, zoom=1.0), None


def tree_simulacrum(c1, c2, t):
    # a hologram of the World Tree standing on its projector; a scan ring climbs it, each leaf a universe
    A, B, Rr, leaves = [], [], [], []
    sway = 0.06 * math.sin(t * TAU)

    def br(a, d, ln, depth, yaw):
        b = a + d * ln
        A.append(a)
        B.append(b)
        Rr.append(0.035 + 0.03 * depth)
        if depth == 0:
            leaves.append(b)
            return
        for k in (-1, 1):
            ang = 0.6 + sway * k
            y2 = yaw + (1.57 if depth % 2 == 0 else 0)
            side = np.array([math.cos(y2), 0, math.sin(y2)])
            nd = d * math.cos(ang) + side * k * math.sin(ang)
            br(b, nd / np.linalg.norm(nd), ln * 0.68, depth - 1, y2)
    br(np.array([0, -0.52, 0.0]), np.array([0, 1.0, 0]), 0.5, 3, 0.4)
    hy = -0.5 + 1.25 * (0.5 - 0.5 * math.cos(TAU * t))   # the scan climbs and descends (it used to snap back down)

    def holo(p, n):
        scan = np.abs(p[..., 1] - hy) < 0.08
        lines = np.abs(np.mod(p[..., 1] * 10, 1) - 0.5) < 0.12
        return -0.15 * lines, (scan | lines).astype(float)
    parts = [Part(_segs(A, B, Rr), 0), Part(_balls(leaves, 0.12), 1),
             Part(lambda p: sd_cyl(p, 0.5, 0.06, 0.02), 2, pos=(0, -0.6, 0)),
             Part(lambda p: sd_torus(p, 0.55 - 0.2 * max(0, hy), 0.025), 3, pos=(0, hy, 0))]
    mats = [Material(c1, '#ffffff', metal=0.3, emissive='#ffffff', pattern=holo),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_spots(t, 0.4, 3)),
            Material('#30384a', c1, metal=1.0, emissive=c1, pattern=lambda p, n: (_zero(p), ((np.abs(np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - 0.35) < 0.05) & (n[..., 1] > 0.5)).astype(float))),
            Material(c1, '#ffffff', emissive=c1, pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=1.02), None


# ============================================================================== negentropy
def cryocooler_unit(c1, c2, t):
    # a pulse-tube cold head: motor housing, a stack of fins, and a frosted cold tip shedding sinking vapour
    def fins(p):
        y = p[..., 1]
        q = np.mod(y + 0.05, 0.13) - 0.065
        d = np.maximum(sd_cyl(np.stack([p[..., 0], q, p[..., 2]], -1), 0.36 - 0.25 * (0.35 - y), 0.025, 0.01), np.abs(y + 0.1) - 0.42)
        return d
    parts = [Part(lambda p: sd_cyl(p, 0.36, 0.17, 0.05), 0, pos=(0, 0.6, 0)),
             Part(lambda p: sd_cyl(p, 0.09, 0.55), 2, pos=(0, 0.0, 0)),
             Part(fins, 1),
             Part(lambda p: sd_cyl(p, 0.15, 0.12, 0.05), 3, pos=(0, -0.66, 0)),
             Part(lambda p: sd_capsule(p, (0.36, 0.6, 0), (0.55, 0.2, 0.0), 0.05), 2)]

    def frost(p, n):
        return 0.2 * np.sin(p[..., 0] * 40) * np.sin(p[..., 2] * 37), (np.sin(p[..., 1] * 30 + t * TAU * 2) > 0.3).astype(float)
    mats = [Material(c1, '#ffffff', metal=0.9, emissive='#80ff80', pattern=lambda p, n: (_zero(p), ((np.abs(p[..., 1]) < 0.04) & (np.abs(np.arctan2(p[..., 2], p[..., 0]) + 1.9) < 0.25)).astype(float))),
            Material('#98b8d0', c2, metal=1.0, emissive=c2, pattern=lambda p, n: (_zero(p), (p[..., 1] < -0.35 + 0.1 * math.sin(t * TAU)).astype(float) * (np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) > 0.2))),
            Material('#707888', '#ffffff', metal=1.0),
            Material('#80e0ff', '#ffffff', emissive='#e0ffff', pattern=frost)]

    def ov(img):
        rng = np.random.default_rng(77)
        for i in range(6):
            ph = rng.random()
            life = (t * 2 + ph) % 1.0
            x = 15.5 + rng.uniform(-4, 4) + math.sin(TAU * (life + ph)) * 1.0
            y = 26 + life * 6
            _dot(img, x, y, H(c2) if i % 2 else WHITE, math.sin(life * math.pi) * 0.9, behind=True)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=1.0), ov


def negentropy_cell(c1, c2, t):
    # Maxwell's demon in a bottle: hot (red) left, cold (blue) right, a gate in the middle sorting molecules
    left = lambda p: sd_capsule(p, (-0.6, 0, 0), (0, 0, 0), 0.33)
    right = lambda p: sd_capsule(p, (0, 0, 0), (0.6, 0, 0), 0.33)

    def gas(seed, speed):
        def f(p, n):
            q = p * 7
            v = np.sin(q[..., 0] + np.sin(q[..., 1] * 1.3 + t * TAU * speed + seed) * 2) * np.sin(q[..., 2] * 1.1 + q[..., 1] - t * TAU * speed)
            return 0.15 * v, (v > 0.35).astype(float)
        return f
    parts = [Part(left, 0), Part(right, 1),
             Part(lambda p: np.maximum(sd_cyl(p, 0.4, 0.05, 0.02), -sd_box(p - np.array([0, 0.0, -0.4]), (0.2, 0.12, 0.2))), 2, rot=rotz(1.5708)),
             Part(lambda p: sd_cyl(p, 0.2, 0.07, 0.03), 2, pos=(-0.92, 0, 0), rot=rotz(1.5708)),
             Part(lambda p: sd_cyl(p, 0.2, 0.07, 0.03), 2, pos=(0.92, 0, 0), rot=rotz(1.5708))]
    mats = [Material('#802010', c1, metal=0.3, gloss=1.4, emissive=c1, pattern=gas(0, 3)),
            Material('#103060', c2, metal=0.3, gloss=1.4, emissive=c2, pattern=gas(2, 1)),
            Material('#9098a8', '#ffffff', metal=1.0)]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.35, zoom=1.0), None


# ============================================================================== omega tier
def _rot_between(a, b):
    """Smallest rotation taking unit vector a onto unit vector b (Rodrigues)."""
    v = np.cross(a, b)
    c = float(a @ b)
    if np.linalg.norm(v) < 1e-9:
        return np.eye(3)
    vx = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + vx + vx @ vx / (1 + c)


def _abs_inf_scene(c1, c2, t, u):
    """Rings of ever-larger infinities streaming out from a small white lemniscate."""
    parts, mats = [], [Material(c1, c2, metal=0.8, emissive=c2, pattern=pat_bands(10, 2, t, 0.2, axis=0)),
                       Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]

    def lem(s):
        a = s * TAU
        d = 1 + math.sin(a) ** 2
        return np.array([0.36 * math.cos(a) / d, 0.06 * math.sin(2 * a), 0.36 * math.sin(a) * math.cos(a) / d])
    pts = [lem(i / 32) for i in range(32)]
    parts.append(Part(_poly(pts, 0.065, closed=True), 1, rot=roty(t * TAU) @ rotx(-1.2)))
    # Seamless loop: each ring takes over its outer neighbour's place when u wraps 1 -> 0.  Rings are born inside the
    # bright lemniscate; on the way out they turn to face the camera and grow past the frame (r > 1.6), so they leave
    # the picture instead of popping out of existence on the loop seam (which is what made the old loop jerk).
    cam = rotx(-0.3) @ roty(0.5)                      # same view as nar_absolute_infinity
    fwd = np.array([0, 0, 1.0]) @ cam                 # world-space view direction
    for k in range(-1, 7):
        s = k + u
        r = 0.16 * 1.55 ** s
        if r < 0.1 or r > 1.6:
            continue
        w = 0.035 * min(1.0, (r - 0.1) / 0.1)
        if w < 0.008:
            continue
        rot = roty(s * 1.1) @ rotx(1.0 + 0.6 * math.sin(s * 2.0))
        face = min(1.0, max(0.0, (s - 3.0) / 2.0))
        face = face * face * (3 - 2 * face)
        if face > 0:
            a = rot[:, 1]
            d = fwd if a @ fwd >= 0 else -fwd
            n = a * (1 - face) + d * face
            n /= np.linalg.norm(n)
            rot = _rot_between(a, n) @ rot
        parts.append(Part(lambda p, r=r, w=w: sd_torus(p, r, w), 0, rot=rot))
    return parts, mats


def nar_absolute_infinity(c1, c2, t):
    u = (t * 2) % 1.0
    parts, mats = _abs_inf_scene(c1, c2, t, u)

    def ov(img):
        anim.ov_sparkles(img, t, 41, H(c2), 5, 4)
        anim.ov_rays(img, t, 3, H(c2) * 0.6 + 0.4, 2, fade_in=0.25)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0), ov


def absolute_infinity(c1, c2, t):
    parts, mats = _abs_inf_scene(c1, c2, t, t % 1.0)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0), None


def eternity_catalyst(c1, c2, t):
    # an hourglass whose sand runs forever but neither bulb ever empties or fills
    bulbs = lambda p: np.minimum(sd_sphere((p - np.array([0, 0.36, 0])) / np.array([1, 1.05, 1]), 0.34), sd_sphere((p + np.array([0, 0.36, 0])) / np.array([1, 1.05, 1]), 0.34))
    neck = lambda p: sd_cyl(p, 0.07, 0.12)

    def sand(p, n):
        y = p[..., 1]
        filled = ((y > 0.06) & (y < 0.3)) | (y < -0.5)
        return 0.15 * filled, filled.astype(float)
    parts = [Part(bulbs, 0), Part(neck, 0),
             Part(lambda p: sd_cyl(p, 0.44, 0.045, 0.02), 1, pos=(0, 0.78, 0)),
             Part(lambda p: sd_cyl(p, 0.44, 0.045, 0.02), 1, pos=(0, -0.78, 0)),
             Part(_segs([np.array([math.cos(a) * 0.4, -0.76, math.sin(a) * 0.4]) for a in (0.3, 0.3 + TAU / 3, 0.3 + 2 * TAU / 3)],
                        [np.array([math.cos(a) * 0.4, 0.76, math.sin(a) * 0.4]) for a in (0.3, 0.3 + TAU / 3, 0.3 + 2 * TAU / 3)], 0.04), 1)]
    mats = [Material('#d8e0f0', '#ffffff', metal=0.3, gloss=1.8, emissive=c2, pattern=sand),
            Material('#c0a050', c2, metal=1.0, emissive=c2, pattern=pat_bands(6, 1, t, 0.08, axis=0))]

    def ov(img):
        # the falling stream through the neck, grains sliding down without end
        for y in range(16, 26):
            on = ((y - t * 30) % 3) < 1.5
            _dot(img, 15.5, y, H(c2) if on else WHITE, 0.9 if on else 0.5)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.5, zoom=1.0), ov


def higgs_catalyst(c1, c2, t):
    # the Mexican-hat potential: a ball rolls forever round the trough; the unstable peak glows
    a3, v = 7.0, 0.5
    hat = lambda r2: a3 * (r2 - v * v) ** 2 - 0.25

    def surf(p):
        x, z = p[..., 0], p[..., 2]
        r2 = x * x + z * z
        g = np.sqrt(1 + (4 * a3 * (r2 - v * v)) ** 2 * r2)
        return np.maximum(np.maximum((p[..., 1] - hat(r2)) / g, -0.42 - p[..., 1]), np.sqrt(r2) - 0.66) * 0.6

    def rings(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        lines = (np.abs(np.mod(r * 7, 1) - 0.5) < 0.1) | (np.abs(np.mod(a * 12 / TAU, 1) - 0.5) < 0.06)
        peak = r < 0.14 + 0.05 * math.sin(t * TAU * 2)
        return -0.1 * lines, (peak | (lines & (np.abs(r - v) < 0.12))).astype(float)
    a = t * TAU
    parts = [Part(surf, 0), Part(lambda p: sd_sphere(p, 0.1), 1, pos=(math.cos(a) * v, hat(v * v) + 0.12, math.sin(a) * v))]
    mats = [Material('#8a6c18', c2, metal=0.9, emissive=c2, pattern=rings), Material('#ffffff', c2, emissive='#ffffff', pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.32, view_yaw=0.6, zoom=1.1), None


def _omega_scene(c1, c2, t, conv):
    """A light-cone wireframe opening upward; rings of light slide down it and converge on the apex."""
    ya, yt, rt = -0.62, 0.55, 0.72
    ribs_a = [k * TAU / 8 + conv * TAU / 8 for k in range(8)]
    A = [np.array([0, ya, 0])] * 8
    B = [np.array([math.cos(a) * rt, yt, math.sin(a) * rt]) for a in ribs_a]
    parts = [Part(_segs(A, B, 0.025), 0), Part(lambda p: sd_torus(p, rt, 0.04), 2, pos=(0, yt, 0))]
    mats = [Material('#7a5a20', c1, metal=0.9),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all()),
            Material('#c0a060', c1, metal=1.0, emissive=c1, pattern=pat_bands(8, 1, t, 0.15, axis=0)),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    for k in range(4):
        f = (k / 4 - conv) % 1.0
        f = f ** 1.3
        if f < 0.06:
            continue
        parts.append(Part(lambda p, r=rt * f, w=0.02 + 0.025 * f: sd_torus(p, r, w), 3, pos=(0, ya + (yt - ya) * f, 0)))
    return parts, mats, ya


def nar_omega_point(c1, c2, t):
    parts, mats, ya = _omega_scene(c1, c2, t, t * 2)
    grow = min(1.0, t / 0.85)
    if t < 0.85:
        rs = 0.05 + 0.1 * grow
    else:
        rs = 0.15 * (1 - (t - 0.85) / 0.15) + 0.05 * ((t - 0.85) / 0.15)
    parts.append(Part(lambda p: sd_sphere(p, rs), 1, pos=(0, ya, 0)))

    def ov(img):
        anim.ov_infall(img, t * 2 % 1.0, 5, H(c1), 10)
        _star(img, 15.5, 25, H(c1), 0.35 + 0.5 * grow if t < 0.85 else 0.85 - 0.5 * (t - 0.85) / 0.15)
        if t > 0.85:
            k = math.sin((t - 0.85) / 0.15 * math.pi)
            anim.ov_rays(img, (t - 0.85) / 0.15 * 0.99, 1, WHITE, 1)
            for dy in range(-4, 5):
                for dx in range(-4, 5):
                    _dot(img, 15.5 + dx, 25 + dy, WHITE, max(0, k * (1 - math.hypot(dx, dy) / 5)))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5, zoom=1.0), ov


def omega_point(c1, c2, t):
    parts, mats, ya = _omega_scene(c1, c2, t, t)
    parts.append(Part(lambda p: sd_sphere(p, 0.1), 1, pos=(0, ya, 0)))
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5, zoom=1.0), None


def omnilex(c1, c2, t):
    # the book of every law, open and floating; loose blocks drift *upward* around it - gravity is optional
    open_a = 0.42
    page = lambda p: sd_box(p, (0.36, 0.035, 0.48), 0.02)
    parts = [Part(page, 0, pos=(-0.34, 0.0, 0), rot=rotz(-open_a)), Part(page, 0, pos=(0.34, 0.0, 0), rot=rotz(open_a)),
             Part(lambda p: sd_box(p, (0.39, 0.02, 0.5), 0.01), 2, pos=(-0.35, -0.05, 0), rot=rotz(-open_a)),
             Part(lambda p: sd_box(p, (0.39, 0.02, 0.5), 0.01), 2, pos=(0.35, -0.05, 0), rot=rotz(open_a)),
             Part(lambda p: sd_torus(p, 0.86, 0.022), 1, pos=(0, 0.05, 0), rot=rotx(0.3) @ roty(t * TAU))]

    def text(p, n):
        line = (np.abs(np.mod(p[..., 2] * 7, 1) - 0.5) < 0.18) & (np.abs(p[..., 0]) < 0.28) & (n[..., 1] > 0.3)
        wave = np.sin(p[..., 0] * 10 + p[..., 2] * 3 - t * TAU * 2) > 0.4
        return -0.3 * line, (line & wave).astype(float)
    mats = [Material('#f0ece0', c2, metal=0.2, emissive='#20d0b0', pattern=text),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(10, 2, t, 0.25, axis=0)),
            Material('#1a3048', c2, metal=0.9, keep_dark=True)]
    for k in range(4):
        life = (t + k / 4) % 1.0
        s = 0.07 * min(1.0, life / 0.2, (1 - life) / 0.2)
        if s < 0.015:
            continue
        a = k * TAU / 4 + 0.6
        parts.append(Part(lambda p, s=s: sd_box(p, (s, s, s), 0.01), 1, pos=(math.cos(a) * 0.62, -0.6 + life * 1.3, math.sin(a) * 0.45), rot=roty(life * 3) @ rotx(life * 2)))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.2, zoom=1.0), None


def omniscience_catalyst(c1, c2, t):
    # an eye; its iris is every colour at once, nine singularity-motes orbit it, and once a loop it blinks
    blink = 1.0
    if 0.46 < t < 0.56:
        blink = max(0.08, abs(t - 0.51) / 0.05)
    almond = lambda p: np.maximum(sd_sphere((p - np.array([0, -0.5, 0])) / np.array([1, 1, 0.5]), 0.88),
                                  sd_sphere((p + np.array([0, -0.5, 0])) / np.array([1, 1, 0.5]), 0.88)) * 0.5
    sc = np.array([1.0, blink, 1.0])
    parts = [Part(lambda p: almond(p / sc) * min(1.0, blink + 0.2), 0),
             Part(lambda p: almond(p / (sc * np.array([1.12, 1.25, 1.0]))) * 0.9, 1, pos=(0, 0, 0.08))]
    mats = [Material('#f0f0f8', '#ffffff', metal=0.2, gloss=1.4), Material('#0a0a10', '#806080', metal=0.3, keep_dark=True)]

    def ov(img):
        n = img.shape[0]
        c = (n - 1) / 2
        ys, xs = np.mgrid[0:n, 0:n]
        dx, dy = xs - c, ys - c
        r = np.sqrt(dx * dx + dy * dy)
        a = np.arctan2(dy, dx)
        lid = img[..., 3] > 0.5
        # only paint the iris where the white of the eye is showing
        white = lid & (img[..., :3].mean(-1) > 0.45)
        iris = white & (r < 6.6)
        hue = (a / TAU + t) % 1.0
        for y, x in zip(*np.nonzero(iris)):
            if r[y, x] < 2.6:
                img[y, x, :3] = 0.02
            else:
                img[y, x, :3] = _hsv(hue[y, x] + r[y, x] * 0.04, 0.85, 1.0)
        if blink > 0.9:
            _dot(img, c - 1.5, c - 2, WHITE, 1.0)
        for k in range(9):
            ang = TAU * (t + k / 9)
            x = c + math.cos(ang) * 14
            y = c + math.sin(ang) * 5 + math.cos(ang) * 3
            front = math.sin(ang) > 0
            _dot(img, x, y, _hsv(k / 9, 0.8, 1.0), 1.0, behind=not front)
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.0, zoom=1.0), ov


def oracle_lemma(c1, c2, t):
    # a scroll of proof; a check-mark writes itself across it, holds, and fades - 'counts as solved'
    sheet = lambda p: sd_box(p, (0.5, 0.6, 0.02), 0.01) + 0.0
    parts = [Part(sheet, 0, rot=rotx(0.1)),
             Part(lambda p: sd_cyl(p, 0.09, 0.62, 0.03), 1, pos=(0, 0.64, 0.02), rot=rotz(1.5708)),
             Part(lambda p: sd_cyl(p, 0.09, 0.62, 0.03), 1, pos=(0, -0.64, -0.02), rot=rotz(1.5708)),
             Part(_balls([(-0.7, 0.64, 0.02), (0.7, 0.64, 0.02), (-0.7, -0.64, -0.02), (0.7, -0.64, -0.02)], 0.09), 2)]

    def lines(p, n):
        ln = (np.abs(np.mod(p[..., 1] * 6, 1) - 0.5) < 0.15) & (np.abs(p[..., 0]) < 0.38)
        return -0.18 * ln, _zero(p)
    mats = [Material('#6a4888', c2, metal=0.1, pattern=lines), Material('#2a1838', c2, metal=0.6, keep_dark=True), Material(c2, '#ffffff', emissive=c2, pattern=pat_all())]
    tick = [(9, 16), (10, 17), (11, 18), (12, 19), (13, 20), (14, 19), (15, 18), (16, 17), (17, 16), (18, 15), (19, 14), (20, 13), (21, 12), (22, 11)]

    def ov(img):
        if t < 0.05:
            return
        if t < 0.5:
            nshow = int(len(tick) * (t - 0.05) / 0.45) + 1
            a = 1.0
        else:
            nshow = len(tick)
            a = 1.0 if t < 0.8 else max(0.0, 1 - (t - 0.8) / 0.2)
        col = WHITE
        for i, (x, y) in enumerate(tick[:nshow]):
            _dot(img, x, y, col, a)
            _dot(img, x, y + 1, H(c2), a * 0.8)
        if t < 0.5:
            x, y = tick[nshow - 1]
            _star(img, x, y, H(c2), 0.7)
    return parts, mats, dict(view_pitch=0.1, view_yaw=0.25, zoom=1.0), ov


def proof_of_everything(c1, c2, t):
    # the end-of-proof tombstone, a black block with gold-lit edges on a little plinth; it radiates 'Q.E.D.'
    def edges(p, n):
        q = np.abs(p) / np.array([0.36, 0.48, 0.2])
        e = ((q > 0.86).sum(-1) >= 2)
        sweep = np.sin((p[..., 0] + p[..., 1]) * 6 - t * TAU * 2) > -0.3
        inset = (np.abs(np.abs(p[..., 0]) - 0.24) < 0.03) & (np.abs(p[..., 1]) < 0.36) | (np.abs(np.abs(p[..., 1]) - 0.36) < 0.03) & (np.abs(p[..., 0]) < 0.24)
        inset = inset & (n[..., 2] < -0.5)
        return -0.1 * inset, ((e & sweep) | inset).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.36, 0.48, 0.2), 0.02), 0, pos=(0, 0.08, 0)),
             Part(lambda p: sd_box(p, (0.5, 0.07, 0.3), 0.02), 1, pos=(0, -0.48, 0)),
             Part(lambda p: sd_box(p, (0.62, 0.06, 0.38), 0.02), 1, pos=(0, -0.6, 0))]
    mats = [Material('#14101c', c2, metal=0.9, gloss=1.5, emissive=c2, pattern=edges, keep_dark=True),
            Material('#3a3040', c2, metal=0.8, emissive=c2, pattern=pat_bands(6, 1, t, 0.08, axis=0))]

    def ov(img):
        anim.ov_embers(img, t, 55, H(c2), 6, 0.7)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.45, zoom=1.05), ov


def spacetime_catalyst(c1, c2, t):
    # a gravity well in a rubber-sheet grid, holding a knot of curved spacetime in orbit round its throat
    def well(p):
        x, z = p[..., 0], p[..., 2]
        r = np.sqrt(x * x + z * z)
        h = 0.3 - 0.8 / (r / 0.15 + 1)
        g = np.sqrt(1 + (0.8 / 0.15 / (r / 0.15 + 1) ** 2) ** 2)
        return np.maximum(np.abs(p[..., 1] - h) / g - 0.03, r - 0.8)

    def grid(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        circ = np.abs(np.mod(r * 4 + t * 2, 1) - 0.5) < 0.12
        spoke = (np.abs(np.mod((a - t * TAU / 6) * 6 / TAU, 1) - 0.5) < 0.06) & (r > 0.2)
        return 0.0 * r, (circ | spoke).astype(float)
    a = t * TAU
    parts = [Part(well, 0), Part(lambda p: sd_sphere(p, 0.15), 1, pos=(0, -0.2, 0)),
             Part(lambda p: sd_torus(p, 0.3, 0.035), 2, pos=(0, -0.2, 0), rot=rotx(0.6 * math.cos(a)) @ rotz(0.6 * math.sin(a)))]
    mats = [Material('#203060', c2, metal=0.7, gloss=0.6, emissive=c2, pattern=grid), Material('#ffffff', c2, emissive='#ffffff', pattern=pat_pulse_core(t)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(8, 2, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.5, zoom=1.0), None


def theory_of_everything(c1, c2, t):
    # four force-rings (EM, weak, strong, gravity) swing into one plane and fuse into a single white ring
    s = 0.5 - 0.5 * math.cos(t * TAU)
    s = s * s * (3 - 2 * s)
    cols = ['#ffe040', '#40a0ff', '#ff4040', '#c080ff']
    parts, mats = [], []
    for k, col in enumerate(cols):
        cc = mix(H(col), WHITE, s ** 2 * 0.9)
        mats.append(Material(cc, '#ffffff', emissive=cc, pattern=pat_bands(8, 1 + k % 2, t, 0.35, axis=0)))
        tilt = (0.55 + 0.3 * k) * (1 - s)
        R = roty(k * 0.785 + t * TAU) @ rotx(tilt)
        parts.append(Part(lambda p, rr=0.62 - 0.05 * k * (1 - s): sd_torus(p, rr, 0.045), k, rot=R))
    mats.append(Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_pulse_core(t)))
    parts.append(Part(lambda p: sd_sphere(p, 0.08 + 0.12 * s), 4))
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.4, zoom=1.0), None


# ============================================================================== orbs
def biosphere_compendium(c1, c2, t):
    # one of every edible creature pressed into a single lumpy meatball, flecked with herbs and a bone
    def ball(p):
        return sd_sphere(p, 0.55) + 0.05 * np.sin(p[..., 0] * 11) * np.sin(p[..., 1] * 13 + 1) * np.sin(p[..., 2] * 12 + 2) + 0.025 * np.sin(p[..., 0] * 27 + p[..., 2] * 23)
    parts = [Part(ball, 0),
             Part(lambda p: sd_capsule(p, (0.35, 0.35, -0.2), (0.62, 0.62, -0.25), 0.05), 1),
             Part(_balls([(0.62, 0.62, -0.25), (0.66, 0.56, -0.25)], 0.06), 1),
             Part(_balls([(-0.3, 0.38, -0.32), (0.1, 0.5, -0.32), (-0.45, -0.1, -0.35), (0.25, -0.25, -0.48), (-0.1, 0.05, -0.56)], 0.07), 2)]
    mats = [Material(c1, c2, metal=0.2, pattern=pat_combine(pat_noise_shade(14, 0.35, 3), pat_spots(t, 0.85, 5)), emissive=c2),
            Material('#e8e0d0', '#ffffff', metal=0.3),
            Material('#40a030', '#c0ff80', metal=0.2)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.02), None


_PHI = (1 + 5 ** 0.5) / 2


def _bucky():
    import itertools
    base = [(0, 1, 3 * _PHI), (1, 2 + _PHI, 2 * _PHI), (_PHI, 2, _PHI ** 3)]
    pts = set()
    for b in base:
        for perm in ((0, 1, 2), (1, 2, 0), (2, 0, 1)):
            v = [b[perm[0]], b[perm[1]], b[perm[2]]]
            for sx in (-1, 1):
                for sy in (-1, 1):
                    for sz in (-1, 1):
                        pts.add((round(v[0] * sx, 6), round(v[1] * sy, 6), round(v[2] * sz, 6)))
    P = np.array(sorted(pts))
    E = [(i, j) for i in range(len(P)) for j in range(i + 1, len(P)) if abs(np.linalg.norm(P[i] - P[j]) - 2) < 1e-3]
    sc = 0.6 / np.linalg.norm(P[0])
    return P * sc, E


_BV, _BE = _bucky()


def buckyball_cage(c1, c2, t):
    # a truncated-icosahedron cage of sixty carbons with one guest atom glowing inside
    A = _BV[[e[0] for e in _BE]]
    B = _BV[[e[1] for e in _BE]]
    R = rotx(0.3) @ roty(t * math.pi)
    parts = [Part(_segs(A, B, 0.025), 0, rot=R), Part(_balls(_BV, 0.055), 1, rot=R), Part(lambda p: sd_sphere(p, 0.16), 2)]
    mats = [Material('#404448', '#a0a0a0', metal=0.6), Material(c2, '#ffffff', metal=0.9, emissive=c2, pattern=pat_bands(6, 1, t, 0.15, axis=1)),
            Material('#80ffd0', '#ffffff', emissive='#80ffd0', pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.02), None


def einstein_field_core(c1, c2, t):
    # a bright matter core inside a lattice sphere of spacetime grid; curvature ripples run out along the grid
    def lattice(p):
        r = np.sqrt((p * p).sum(-1)) + 1e-6
        shell = np.abs(r - 0.62) - 0.035
        phi = np.arctan2(p[..., 2], p[..., 0])
        th = np.arccos(np.clip(p[..., 1] / r, -1, 1))
        dl = np.abs(np.mod(phi * 8 / TAU, 1) - 0.5) * (TAU / 8) * r * np.sin(th)
        dt = np.abs(np.mod(th * 6 / math.pi, 1) - 0.5) * (math.pi / 6) * r
        return np.maximum(shell, np.minimum(dl, dt) - 0.035)

    def ripple(p, n):
        r = np.sqrt((p * p).sum(-1))
        th = np.arccos(np.clip(p[..., 1] / np.maximum(r, 1e-6), -1, 1))
        return _zero(p), (np.sin(th * 6 - t * TAU * 2) > 0.6).astype(float)
    parts = [Part(lattice, 0, rot=roty(t * TAU / 8)), Part(lambda p: sd_sphere(p, 0.3), 1)]
    mats = [Material(c1, c2, metal=0.9, emissive=c2, pattern=ripple), Material('#ffe0a0', '#ffffff', emissive='#fff0d0', pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0), None


def graviton_wisp_core(c1, c2, t):
    # a closed, twisting ribbon of gravity; stray sparks are pulled in toward it
    def ribbon(p):
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        qx, qy = rho - 0.5, p[..., 1]
        tw = a * 1.5 + t * TAU
        c, s = np.cos(tw), np.sin(tw)
        u, v = qx * c + qy * s, -qx * s + qy * c
        return (np.sqrt((u / 0.2) ** 2 + (v / 0.07) ** 2) - 1) * 0.06

    def wisp(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return 0.2 * np.sin(a * 6), (np.sin(a * 3 - t * TAU * 3) > 0.5).astype(float)
    parts = [Part(ribbon, 0, rot=rotx(0.2)), Part(lambda p: sd_sphere(p, 0.12), 1)]
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=wisp), Material(c2, '#ffffff', emissive='#ffffff', pattern=pat_pulse_core(t))]

    def ov(img):
        anim.ov_infall(img, t, 31, H(c2), 8)
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.4, zoom=1.05), ov


def hypersphere_fragment(c1, c2, t):
    # a curved shard of shell from a sphere one dimension up; light-cells slide across its surface
    def shard(p):
        d = np.abs(sd_sphere(p - np.array([0, -0.85, 0]), 1.15)) - 0.06
        a = np.arctan2(p[..., 2], p[..., 0])
        rim = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) - (0.62 + 0.12 * np.sin(a * 5 + 1) + 0.06 * np.sin(a * 11))
        return np.maximum(np.maximum(d, rim), -0.1 - p[..., 1])

    def cells(p, n):
        q = p * 7
        v = np.sin(q[..., 0] + t * TAU) + np.sin(q[..., 2] * 1.1 - t * TAU) + np.sin((q[..., 0] + q[..., 2]) * 0.8 + q[..., 1] * 2)
        return 0.12 * v, (np.abs(v) < 0.35).astype(float)
    parts = [Part(shard, 0, rot=rotz(0.35) @ rotx(-0.2))]
    mats = [Material(c1, c2, metal=0.8, gloss=1.4, emissive=c2, pattern=cells)]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.05), None


def protostar_core(c1, c2, t):
    # a spinning disk of nebular gas with a glowing, not-yet-fusing knot at its heart and twin jets
    def disk(p):
        return sd_sphere(p / np.array([0.8, 0.1, 0.8]), 1.0) * 0.1

    def arms(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2) + 1e-6
        a = np.arctan2(p[..., 2], p[..., 0])
        v = np.sin(2 * a + np.log(r) * 5 + t * TAU)
        return 0.25 * v, (v > 0.6).astype(float)
    parts = [Part(disk, 0, rot=rotx(0.25)), Part(lambda p: sd_sphere(p, 0.25 + 0.02 * math.sin(t * TAU * 2)), 1),
             Part(lambda p: sd_capsule(p, (0, 0.25, 0), (0, 0.85, 0), 0.04), 2, rot=rotx(0.25)),
             Part(lambda p: sd_capsule(p, (0, -0.25, 0), (0, -0.85, 0), 0.04), 2, rot=rotx(0.25))]
    mats = [Material(c1, c2, metal=0.2, emissive=c2, pattern=arms), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#ffd0a0', '#ffffff', emissive='#ffe0c0', pattern=pat_bands(16, 3, t, 0.3, axis=1))]

    def ov(img):
        anim.ov_infall(img, t, 17, H(c2), 6)
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.5, zoom=1.0), ov


def red_giant_envelope(c1, c2, t):
    # a swollen, convecting envelope filling the frame; granules churn and the star slowly pulsates
    def gran(p, n):
        q = p * 9
        v = np.sin(q[..., 0] + np.sin(q[..., 1] * 1.2 + t * TAU) * 1.5) * np.sin(q[..., 1] + np.sin(q[..., 2] * 1.1 - t * TAU) * 1.5) * np.sin(q[..., 2] + np.sin(q[..., 0] * 0.9 + t * TAU) * 1.5)
        return 0.35 * v, (v > 0.25).astype(float)
    parts = [Part(lambda p: sd_sphere(p, 0.8), 0)]
    mats = [Material(c1, c2, metal=0.1, gloss=0.3, emissive=c2, pattern=gran)]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.5, zoom=1.0), None


def synthetic_cell(c1, c2, t):
    # a cut-away membrane bubble: inside, one tiny circular genome being read, and a few ribosomes
    def membrane(p):
        sh = np.abs(sd_sphere(p, 0.66)) - 0.04
        cut = sd_box(p - np.array([0.5, 0.5, -0.5]), (0.5, 0.5, 0.5))
        return np.maximum(sh, -cut)
    parts = [Part(membrane, 0, rot=roty(0.0)),
             Part(lambda p: sd_torus(p, 0.2, 0.04), 1, pos=(0.08, 0.05, -0.08), rot=rotx(1.0) @ rotz(t * TAU)),
             Part(_balls([(-0.15, -0.25, -0.1), (0.28, -0.15, 0.1), (-0.2, 0.25, 0.15)], 0.07), 2),
             Part(lambda p: sd_sphere(p, 0.05), 3, pos=np.array([0.08, 0.05, -0.08]) + rotx(1.0) @ np.array([math.cos(t * TAU) * 0.2, 0, math.sin(t * TAU) * 0.2]))]

    def bilayer(p, n):
        r = np.sqrt((p * p).sum(-1))
        return 0.2 * np.sin(r * 160), (np.sin(p[..., 0] * 8 + p[..., 1] * 6 - t * TAU) > 0.85).astype(float)

    def gene(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return _zero(p), (np.sin(a * 6) > 0.2).astype(float)
    mats = [Material(c1, c2, metal=0.2, gloss=1.4, emissive=c2, pattern=bilayer),
            Material(c2, '#ffffff', emissive=c2, pattern=gene),
            Material('#c0a0ff', '#ffffff', metal=0.3),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.75, zoom=1.02), None


def white_dwarf_core(c1, c2, t):
    # small and blinding: a crystallised carbon ball, faceted, with diffraction spikes
    def facets(p, n):
        q = np.abs(n)
        f = np.maximum(np.maximum(q[..., 0], q[..., 1]), q[..., 2])
        return 0.25 * (f > 0.82), (np.sin((np.abs(p[..., 0]) + np.abs(p[..., 2]) + p[..., 1]) * 18 - t * TAU * 2) > 0.7).astype(float)
    shape = lambda p: np.maximum(sd_sphere(p, 0.36), sd_octa(p, 0.55) * 1.0)
    parts = [Part(shape, 0, rot=rotx(0.4) @ roty(t * TAU / 4))]
    mats = [Material('#d8e0ec', '#ffffff', metal=1.0, gloss=1.8, emissive='#ffffff', pattern=facets)]

    def ov(img):
        s = 0.75 + 0.25 * math.sin(t * TAU * 2)
        L = int(round(6 + 5 * s))
        for d in range(7, L + 1):
            a = max(0.0, 1 - (d - 6) / (L - 5)) * 0.9
            for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
                _dot(img, 15.5 + dx, 15.5 + dy, H('#c0e0ff'), a)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0), ov


# ============================================================================== pairs
def entangled_photon_pair(c1, c2, t):
    # two photons flying apart along one axis; their polarisation rings turn in perfect, opposite lockstep
    a = t * math.pi
    parts = [Part(lambda p: sd_sphere(p, 0.2), 0, pos=(-0.55, 0, 0)), Part(lambda p: sd_sphere(p, 0.2), 1, pos=(0.55, 0, 0)),
             Part(lambda p: sd_torus(p, 0.33, 0.045), 0, pos=(-0.55, 0, 0), rot=rotz(1.5708) @ roty(0) @ rotx(0) if False else rotx(a) @ rotz(1.5708)),
             Part(lambda p: sd_torus(p, 0.33, 0.045), 1, pos=(0.55, 0, 0), rot=rotx(-a + math.pi / 2) @ rotz(1.5708))]
    wave = [np.array([x, 0.09 * math.sin(x * 14 - t * TAU * 2), 0.09 * math.cos(x * 14 - t * TAU * 2)]) for x in np.linspace(-0.3, 0.3, 16)]
    parts.append(Part(_poly(wave, 0.035), 2))
    mats = [Material(c1, '#ffffff', emissive=c1, pattern=pat_pulse_core(t)), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t)),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_bands(20, 2, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.2, zoom=1.1), None


def lepton_triplet(c1, c2, t):
    # electron, muon and tau (growing heavier) circling, each trailed by its own ghostly neutrino
    parts = []
    mats = [Material(c1, '#ffffff', emissive=c1, pattern=pat_pulse_core(t)), Material('#405060', c2, metal=0.3),
            Material(c2, '#ffffff', metal=0.6, emissive=c1, pattern=pat_bands(8, 1, t, 0.2, axis=0))]
    for k, r in enumerate((0.13, 0.19, 0.26)):
        a = t * TAU + k * TAU / 3
        parts.append(Part(lambda p, r=r: sd_sphere(p, r), 0, pos=(math.cos(a) * 0.56, 0.06 * math.sin(a * 2), math.sin(a) * 0.56)))
        b = a - 0.55
        parts.append(Part(lambda p: sd_sphere(p, 0.08), 1, pos=(math.cos(b) * 0.56, 0.06 * math.sin(b * 2), math.sin(b) * 0.56)))
    parts.append(Part(lambda p: sd_torus(p, 0.56, 0.02), 2))
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.3, zoom=1.0), None


# ============================================================================== pellets
def glueball_core(c1, c2, t):
    # a gluon flux tube wound into a closed spring, colour charge chasing round it
    def spring(p):
        rho = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        qx, qy = rho - 0.5, p[..., 1]
        psi = a * 6 + t * TAU
        return (np.sqrt((qx - 0.13 * np.cos(psi)) ** 2 + (qy - 0.13 * np.sin(psi)) ** 2) - 0.075) * 0.5
    parts = [Part(spring, 0), Part(lambda p: sd_sphere(p, 0.2), 1)]

    def charge(p, n):
        a = np.arctan2(p[..., 2], p[..., 0])
        return _zero(p), (np.sin(a * 3 - t * TAU * 3) > 0.4).astype(float)
    mats = [Material(c1, c2, metal=0.6, emissive=c2, pattern=charge), Material(c2, '#ffffff', emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.65, view_yaw=0.4, zoom=1.0), None


def muonic_catalyst(c1, c2, t):
    # a muon's orbit, 200x tighter than the faint electron orbit around it, squeezes two nuclei until they fuse
    gap = 0.12 * (1 - 0.8 * math.sin(t * math.pi))
    parts = [Part(lambda p: sd_torus(p, 0.74, 0.02), 0, rot=rotx(0.4)),
             Part(lambda p: sd_torus(p, 0.26, 0.04), 1, rot=rotx(0.9) @ roty(t * TAU)),
             Part(lambda p: sd_sphere(p, 0.12), 2, pos=(-gap, 0, 0)), Part(lambda p: sd_sphere(p, 0.12), 2, pos=(gap, 0, 0))]
    a = t * TAU * 3
    parts.append(Part(lambda p: sd_sphere(p, 0.06), 3, pos=rotx(0.9) @ np.array([math.cos(a) * 0.24, 0, math.sin(a) * 0.24])))
    mats = [Material('#405060', c2, metal=0.5, emissive=c2, pattern=lambda p, n: (_zero(p), (np.sin(np.arctan2(p[..., 2], p[..., 0]) * 12) > 0.6).astype(float) * 0.0)),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_bands(8, 2, t, 0.3, axis=0)),
            Material('#ff8060', '#ffe0c0', metal=0.4),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=pat_all())]

    def ov(img):
        k = 1 - abs(t - 0.5) / 0.08
        if k > 0:
            _star(img, 15.5, 15.5, H(c2), k)
        # dashed outer electron orbit dots twinkle in turn
        for i in range(12):
            ang = TAU * i / 12
            if (i + int(t * 12)) % 3 == 0:
                _dot(img, 15.5 + math.cos(ang) * 11.8, 15.5 + math.sin(ang) * 9.3, H(c2), 0.6)
    return parts, mats, dict(view_pitch=0.9, view_yaw=0.4, zoom=1.0), ov


def positronium_hydride(c1, c2, t):
    # inside a Penning trap: a proton, with an electron and a positron waltzing round each other beside it
    # domed end-cap electrodes above and below, a ring electrode round the middle
    parts = [Part(lambda p: np.maximum(sd_sphere(p, 0.4), 0.25 - p[..., 1]), 0, pos=(0, 0.32, 0)),
             Part(lambda p: np.maximum(sd_sphere(p, 0.4), 0.25 + p[..., 1]), 0, pos=(0, -0.32, 0)),
             Part(lambda p: sd_torus(p, 0.66, 0.06), 0),
             Part(lambda p: sd_sphere(p, 0.17), 1, pos=(-0.22, 0, 0))]
    a = t * TAU * 2
    cx = np.array([0.22, 0.05, 0])
    off = np.array([math.cos(a) * 0.14, math.sin(a) * 0.1, math.sin(a) * 0.14])
    parts += [Part(lambda p: sd_sphere(p, 0.1), 2, pos=cx + off), Part(lambda p: sd_sphere(p, 0.1), 3, pos=cx - off),
              Part(_poly([np.array([-0.2, 0, 0]), cx], 0.015), 4)]
    mats = [Material('#7a7890', c2, metal=1.0, emissive=c2, pattern=pat_bands(10, 1, t, 0.12, axis=0)),
            Material('#ff9060', '#ffffff', emissive='#ff9060', pattern=pat_pulse_core(t)),
            Material('#60c0ff', '#ffffff', emissive='#80d0ff', pattern=pat_all()),
            Material(c2, '#ffffff', emissive=c2, pattern=pat_all()),
            Material('#ffffff', '#ffffff', emissive=c2, pattern=pat_bands(20, 3, t, 0.3, axis=0))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.0), None


def strange_matter_pellet(c1, c2, t):
    # a lumpy strangelet of packed quarks; ordinary nuclei drift in, turn green on contact, and are swallowed
    rng = np.random.default_rng(9)
    C = rng.normal(size=(14, 3))
    C = C / np.linalg.norm(C, axis=1, keepdims=True) * rng.uniform(0.12, 0.38, (14, 1))
    parts = [Part(_balls(C, 0.21), 0, rot=roty(t * TAU))]
    mats = [Material(c1, c2, metal=0.4, emissive=c2, pattern=pat_spots(t, 0.6, 7)),
            Material('#909aa8', '#ffffff', metal=0.5),
            Material(c1, '#ffffff', emissive=c1, pattern=pat_all())]
    for k in range(3):
        life = (t + k / 3) % 1.0
        d = 0.82 - 0.36 * life
        s = 0.15 * min(1.0, life / 0.15, (1 - life) / 0.15)
        if s < 0.02:
            continue
        a = k * TAU / 3 + 0.5
        parts.append(Part(lambda p, s=s: sd_sphere(p, s), 2 if life > 0.6 else 1, pos=(math.cos(a) * d, 0.2 * math.sin(a * 2), math.sin(a) * d)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.0), None


# ============================================================================== plates
def cherenkov_wafer(c1, c2, t):
    # a glossy blue wafer; rings of Cherenkov light expand across it like ripples
    def ring(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        rr = [((t + k / 2) % 1.0) * 0.85 for k in range(2)]
        e = np.zeros(r.shape, bool)
        for R in rr:
            e |= np.abs(r - R) < 0.04
        return 0.25 * np.sin(p[..., 0] * 4 + p[..., 2] * 3 - t * TAU) * (n[..., 1] > 0.5), (e & (n[..., 1] > 0.5)).astype(float)
    parts = [Part(_wafer_disk, 0)]
    mats = [Material(c1, c2, metal=1.0, gloss=1.6, emissive=c2, pattern=ring)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5), None


def circuit_substrate(c1, c2, t):
    # a blank metamaterial tile: an array of split-ring resonators, shimmering as a wave passes through them
    def srr(p, n):
        cx = np.mod(p[..., 0] * 1.4, 1) - 0.5
        cz = np.mod(p[..., 2] * 1.4, 1) - 0.5
        r = np.sqrt(cx * cx + cz * cz)
        a = np.arctan2(cz, cx)
        ringo = (np.abs(r - 0.34) < 0.07) & (np.abs(a) > 0.4)
        ringi = (np.abs(r - 0.17) < 0.06) & (np.abs(np.abs(a) - math.pi) > 0.6)
        m = (ringo | ringi) & (n[..., 1] > 0.5)
        wave = np.sin((p[..., 0] + p[..., 2]) * 3 - t * TAU * 2) > -0.4
        side = (np.abs(n[..., 1]) < 0.5) & (np.sin(p[..., 1] * 60) > 0)
        return -0.12 * m + 0.15 * side, (m & wave).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.72, 0.09, 0.72), 0.03), 0)]
    mats = [Material('#1c6050', c2, metal=0.5, emissive=c2, pattern=srr)]
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.785, zoom=1.0), None


def cosmic_board(c1, c2, t):
    # a circuit board laid over a rippling Calabi-Yau surface: the whole board is warped, gold traces and all
    wf = lambda x, z: 0.06 * np.sin(x * 4.4 + 0.5) * np.cos(z * 4.0)

    def board(p):
        x, z = p[..., 0], p[..., 2]
        return np.maximum((np.abs(p[..., 1] - wf(x, z)) - 0.045) * 0.75, np.maximum(np.abs(x) - 0.76, np.abs(z) - 0.6))
    tr = pat_traces(t, 5)
    parts = [Part(board, 0)]
    for (x, z) in ((-0.3, -0.18), (0.32, 0.2)):
        parts.append(Part(lambda p: sd_box(p, (0.14, 0.05, 0.14), 0.02), 1, pos=(x, float(wf(x, z)) + 0.07, z), rot=rotz(0.26 * math.cos(x * 4.4 + 0.5) * math.cos(z * 4))))
    mats = [Material('#283870', c2, metal=0.4, emissive=c2, pattern=tr), Material('#202030', c2, metal=0.9, emissive=c2, pattern=pat_pulse_core(t))]

    def ov(img):
        anim.ov_sparkles(img, t, 61, H('#c0d0ff'), 3, 1)
    return parts, mats, dict(view_pitch=0.75, view_yaw=0.55, zoom=1.0), ov


def cosmic_logic_wafer(c1, c2, t):
    # a navy wafer etched with one immense trace folded into a spiral; a light pulse runs the whole length
    def spiral(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        s = r * 9 - a / TAU
        line = (np.abs(np.mod(s, 1) - 0.5) < 0.22) & (r < 0.7) & (n[..., 1] > 0.5)
        along = np.floor(s) + a / TAU
        pulse = np.mod(along * 0.5 - t * 2, 1.0) < 0.3
        return 0.12 * line, (line & pulse).astype(float)
    parts = [Part(_wafer_disk, 0), Part(lambda p: sd_box(p, (0.07, 0.07, 0.07), 0.01), 1, pos=(0, 0.06, 0), rot=roty(0.785))]
    mats = [Material('#304890', c2, metal=0.9, gloss=1.4, emissive=c2, pattern=spiral), Material('#ffffff', c2, emissive=c2, pattern=pat_pulse_core(t))]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.5), None


def degenerate_armour_plate(c1, c2, t):
    # a thick hexagonal plate of neutronium in a glowing spacetime-alloy frame; starlight just bounces off
    frame = lambda p: np.maximum(sd_hexprism(p, 0.74, 0.15), -sd_hexprism(p, 0.56, 0.3))
    core = lambda p: np.maximum(sd_hexprism(p, 0.58, 0.12), sd_sphere(p + np.array([0, 1.4, 0]), 1.6))
    bolts = [np.array([math.cos(a) * 0.66, 0.16, math.sin(a) * 0.66]) for a in np.arange(6) * TAU / 6 + math.pi / 6]
    parts = [Part(frame, 0), Part(core, 1), Part(_balls(bolts, 0.05), 2)]
    mats = [Material('#8a94a8', c2, metal=1.0, emissive=c2, pattern=lambda p, n: (_zero(p), (np.abs(np.sin(np.arctan2(p[..., 2], p[..., 0]) * 3 - t * TAU)) > 0.92).astype(float) * (np.abs(n[..., 1]) < 0.6))),
            Material(c1, '#e0f0ff', metal=1.0, gloss=1.8, pattern=pat_noise_shade(20, 0.08, 2), keep_dark=True),
            Material('#c0c8d8', '#ffffff', metal=1.0)]

    def ov(img):
        ph = (t * 2) % 1.0
        if ph < 0.3:
            x0, y0 = 4 + ph / 0.3 * 10, 2 + ph / 0.3 * 12
            _dot(img, x0, y0, WHITE, 1.0)
            _dot(img, x0 - 1, y0 - 1.2, H(c2), 0.6)
        elif ph < 0.55:
            k = (ph - 0.3) / 0.25
            _star(img, 14, 14, H(c2), 1 - k)
            _dot(img, 14 + k * 12, 14 - k * 12, WHITE, 1 - k)
    return parts, mats, dict(view_pitch=0.65, view_yaw=0.5), ov


def graphene_sheet(c1, c2, t):
    # a rippling one-atom honeycomb with real holes; electrons run along the bonds
    sc = 4.2

    def hexd(x, z):
        sx, sz = 1.0, 1.7320508
        hx1 = np.floor(x / sx) + 0.5
        hz1 = np.floor(z / sz) + 0.5
        hx2 = np.floor((x - 0.5) / sx) + 0.5
        hz2 = np.floor((z - 0.866) / sz) + 0.5
        ax, az = x - hx1 * sx, z - hz1 * sz
        bx, bz = x - (hx2 + 0.5) * sx, z - (hz2 + 0.5) * sz
        use_a = (ax * ax + az * az) < (bx * bx + bz * bz)
        px = np.abs(np.where(use_a, ax, bx))
        pz = np.abs(np.where(use_a, az, bz))
        return 0.5 - np.maximum(px * 0.5 + pz * 0.8660254, px)

    def sheet(p):
        x, z = p[..., 0], p[..., 2]
        y0 = 0.06 * np.sin(x * 3 + t * TAU) * np.cos(z * 2.5)
        e = hexd(x * sc, z * sc) / sc
        return np.maximum(np.maximum(np.abs(p[..., 1] - y0) - 0.025, 0.035 - e), np.maximum(np.abs(x), np.abs(z)) - 0.72)

    def flow(p, n):
        return _zero(p), (np.sin(p[..., 0] * 5 - p[..., 2] * 3 - t * TAU * 2) > 0.8).astype(float)
    parts = [Part(lambda p: sheet(p) * 0.8, 0, rot=roty(0.3))]
    mats = [Material('#3a3a3e', c2, metal=0.8, emissive='#d0d0d8', pattern=flow)]
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.4, zoom=1.0), None


def hawking_absorber_plate(c1, c2, t):
    # a pitch-black anechoic plate of tiny pyramids; Hawking glow rains onto it and is swallowed at the tips
    cell = 0.3

    def pyr(p):
        lx = np.mod(p[..., 0] + 0.75, cell) - cell / 2
        lz = np.mod(p[..., 2] + 0.75, cell) - cell / 2
        m = np.maximum(np.abs(lx), np.abs(lz))
        top = (p[..., 1] - 0.06 - 0.18 * (1 - m / (cell / 2))) * 0.6
        return np.maximum(np.maximum(top, -p[..., 1]), np.maximum(np.abs(p[..., 0]), np.abs(p[..., 2])) - 0.75)

    def tips(p, n):
        hot = p[..., 1] > 0.15 + 0.06 * math.sin(t * TAU * 2)
        return _zero(p), hot.astype(float)
    parts = [Part(pyr, 0), Part(lambda p: sd_box(p, (0.78, 0.06, 0.78), 0.02), 1, pos=(0, -0.04, 0))]
    mats = [Material(c1, c2, metal=0.2, gloss=0.3, emissive=c2, pattern=tips, keep_dark=True), Material('#30343c', c2, metal=0.9)]

    def ov(img):
        rng = np.random.default_rng(91)
        for i in range(7):
            ph = rng.random()
            life = (t * 2 + ph) % 1.0
            x = rng.uniform(6, 26) - life * 3
            y = -1 + life * 13
            _dot(img, x, y, H(c2), math.sin(life * math.pi) * 0.9)
    return parts, mats, dict(view_pitch=0.65, view_yaw=0.785), ov


def hyperdimensional_logic_wafer(c1, c2, t):
    # circuits folded down from 4D: a tesseract's shadow etched on the wafer, inner cube breathing in and out
    def tess(p, n):
        x, z = p[..., 0], p[..., 2]
        so, si = 0.5, 0.2 + 0.08 * math.sin(t * TAU)
        m = np.maximum(np.abs(x), np.abs(z))
        sq = (np.abs(m - so) < 0.03) | (np.abs(m - si) < 0.03)
        diag = (np.abs(np.abs(x) - np.abs(z)) < 0.035) & (m > si) & (m < so)
        line = (sq | diag) & (n[..., 1] > 0.5)
        pulse = np.sin(m * 20 - t * TAU * 3) > 0.3
        dies = (np.abs(np.mod(x * 6, 1) - 0.5) < 0.45) & (np.abs(np.mod(z * 6, 1) - 0.5) < 0.45)
        return 0.15 * line - 0.06 * (~dies), (line & pulse).astype(float)
    parts = [Part(_wafer_disk, 0)]
    mats = [Material(c1, c2, metal=0.9, gloss=1.5, emissive=c2, pattern=tess)]
    return parts, mats, dict(view_pitch=0.9, view_yaw=0.785), None


def hyperdimensional_wafer(c1, c2, t):
    # a wafer with more surface than its area: its rim ruffles like a hyperbolic sheet, ripples running round
    def ruffle(p):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        a = np.arctan2(p[..., 2], p[..., 0])
        y0 = 0.22 * r * r * np.sin(7 * a + t * TAU)
        return np.maximum((np.abs(p[..., 1] - y0) - 0.035) * 0.55, r - 0.76)

    def sheen(p, n):
        r = np.sqrt(p[..., 0] ** 2 + p[..., 2] ** 2)
        return 0.3 * n[..., 0], ((np.sin(r * 18 - t * TAU * 2) > 0.7) & (r > 0.2)).astype(float)
    parts = [Part(ruffle, 0)]
    mats = [Material(c1, c2, metal=1.0, gloss=1.6, emissive=c2, pattern=sheen)]
    return parts, mats, dict(view_pitch=0.7, view_yaw=0.5), None


def neutronium_plate(c1, c2, t):
    # a plate rolled femtometres thin and impossibly heavy: it sags, and a light glint bends round it
    def plate(p):
        x, z = p[..., 0], p[..., 2]
        y0 = -0.1 * (1 - (x * x + z * z) / 1.1)
        return np.maximum(np.abs(p[..., 1] - y0) - 0.025, np.maximum(np.abs(x), np.abs(z)) - 0.72)

    def packed(p, n):
        q = p * 26
        v = np.sin(q[..., 0]) * np.sin(q[..., 2] * 0.866 + 0.5 * q[..., 0])
        return 0.12 * v + 0.15 * np.sin(p[..., 0] * 3 - p[..., 2] * 2 - t * TAU), _zero(p)
    parts = [Part(lambda p: plate(p) * 0.9, 0, rot=roty(0.2)), Part(_balls([(0.68, 0.0, 0.68), (-0.68, 0.0, 0.68), (0.68, 0.0, -0.68), (-0.68, 0.0, -0.68)], 0.06), 1, rot=roty(0.2))]
    mats = [Material(c1, c2, metal=1.0, gloss=1.4, pattern=packed), Material('#c8d0e0', '#ffffff', metal=1.0)]

    def ov(img):
        n = img.shape[0]
        # a faint lensing halo: light hugging the silhouette, crawling round it
        op = img[..., 3] > 0.5
        halo = np.zeros_like(op)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            halo |= np.roll(op, (dy, dx), (0, 1))
        halo &= ~op
        ys, xs = np.mgrid[0:n, 0:n]
        crawl = np.sin(np.arctan2(ys - n / 2, xs - n / 2) * 3 - t * TAU) > 0.2
        sel = halo & crawl
        img[sel, :3] = mix(H(c2), WHITE, 0.3) * 0.8
        img[sel, 3] = 0.5
        local = t % 1.0
        if local > 0.5:
            return
        pos = -6 + local / 0.5 * (2 * n + 12)
        ys, xs = np.mgrid[0:n, 0:n]
        bend = 5 * np.sin((ys / n) * math.pi)
        band = np.clip(1 - np.abs((xs + ys + bend) - pos) / 2.5, 0, 1) * (img[..., 3] > 0.5)
        img[..., :3] = img[..., :3] * (1 - band[..., None] * 0.6) + band[..., None] * 0.6 * mix(H(c2), WHITE, 0.5)
    return parts, mats, dict(view_pitch=0.6, view_yaw=0.6), ov


def photonic_board(c1, c2, t):
    # a board of curving light-guides; packets of light run along them between a laser and a prism
    def guides(p, n):
        x, z = p[..., 0], p[..., 2]
        top = n[..., 1] > 0.5
        e = np.zeros(x.shape, bool)
        lit = np.zeros(x.shape, bool)
        for k, z0 in enumerate((-0.3, 0.0, 0.3)):
            zc = z0 + 0.12 * np.sin(x * 4 + k * 1.3)
            g = np.abs(z - zc) < 0.035
            e |= g
            lit |= g & (np.mod(x * 1.5 - t * 2 + k / 3, 1) < 0.25)
        return -0.12 * (e & top), (lit & top).astype(float)
    parts = [Part(lambda p: sd_box(p, (0.76, 0.05, 0.56), 0.02), 0),
             Part(lambda p: sd_box(p, (0.1, 0.07, 0.42), 0.02), 1, pos=(-0.62, 0.1, 0)),
             Part(lambda p: sd_octa(p, 0.17), 2, pos=(0.58, 0.14, 0.0), rot=roty(t * TAU / 4)),
             Part(_segs([(i * 0.15, 0.0, 0.6) for i in range(-4, 5)], [(i * 0.15, 0.0, 0.66) for i in range(-4, 5)], 0.03), 3)]
    mats = [Material(c1, c2, metal=0.3, emissive=c2, pattern=guides), Material('#303440', c2, metal=0.8, emissive=c2, pattern=lambda p, n: (_zero(p), (p[..., 0] > 0.07).astype(float))),
            Material('#e0f0ff', c2, metal=0.2, gloss=1.8, emissive=c2, pattern=pat_pulse_core(t)), Material('#c8a040', '#fff0c0', metal=1.0)]
    return parts, mats, dict(view_pitch=0.8, view_yaw=0.6), None


def photonic_logic_wafer(c1, c2, t):
    # a wafer of Mach-Zehnder interferometers: waveguides split into diamonds and rejoin, light pulsing through
    def mzi(p, n):
        x, z = p[..., 0], p[..., 2]
        top = n[..., 1] > 0.5
        e = np.zeros(x.shape, bool)
        for z0, x0 in ((-0.34, -0.1), (0.0, 0.15), (0.34, -0.2)):
            sp = 0.1 * np.clip(1 - np.abs(x - x0) / 0.32, 0, 1)
            e |= (np.abs(np.abs(z - z0) - sp) < 0.03) & (np.abs(x) < 0.7)
        lit = e & (np.mod(x * 1.2 - t * 2, 1) < 0.3)
        r = np.sqrt(x * x + z * z)
        return -0.1 * (e & top) + 0.1 * np.sin(r * 30) * 0.3, (lit & top).astype(float)
    parts = [Part(_wafer_disk, 0)]
    mats = [Material(c1, c2, metal=0.95, gloss=1.5, emissive=c2, pattern=mzi)]
    return parts, mats, dict(view_pitch=0.85, view_yaw=0.35), None


def quadrupole_pole_tip(c1, c2, t):
    # a steel pole piece with a hyperbolic tip and a gold winding; above it, the beam it squeezes to a point
    def pole(p):
        x = p[..., 0]
        box = sd_box(p, (0.36, 0.62, 0.28), 0.02)
        hyp = (p[..., 1] - (0.62 - 1.1 * (np.sqrt(0.03 + x * x) - 0.173))) * 0.6
        return np.maximum(box, hyp)
    coil = lambda p: np.maximum(sd_box(p, (0.46, 0.14, 0.38), 0.04), -sd_box(p, (0.37, 0.3, 0.29)))
    parts = [Part(pole, 0, pos=(0, -0.15, 0)), Part(coil, 1, pos=(0, -0.2, 0)),
             Part(lambda p: sd_sphere(p, 0.07 + 0.03 * math.sin(t * TAU * 2)), 2, pos=(0, 0.72, 0))]

    def ground(p, n):
        return 0.3 * np.sin(p[..., 1] * 30) * (n[..., 1] > 0.6), ((n[..., 1] > 0.6) & (np.abs(p[..., 0] - math.sin(t * TAU) * 0.25) < 0.05)).astype(float)
    mats = [Material(c1, '#ffffff', metal=1.0, gloss=1.6, emissive='#ffffff', pattern=ground),
            Material('#c08030', c2, metal=0.9, emissive=c2, pattern=pat_bands(14, 1, t, 0.12, axis=1)),
            Material(c2, '#ffffff', emissive='#ffffff', pattern=pat_all())]

    def ov(img):
        # the focused beam spot above the tip flickers like a struck spark
        _star(img, 15.5, 4.5, H(c2), 0.4 + 0.3 * math.sin(t * TAU * 4))
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.25, zoom=1.0), ov


# ============================================================================== registry
ITEMS = {
    'dark_matter_lens': dark_matter_lens,
    'gravitic_lens_element': gravitic_lens_element,
    'photonic_lens': photonic_lens,
    'tesseract_lens': tesseract_lens,
    'closed_timelike_curve': closed_timelike_curve,
    'compactified_circle': compactified_circle,
    'hopf_fibration': hopf_fibration,
    'recombinant_dna': recombinant_dna,
    'temporal_knot': temporal_knot,
    'well_ordering_charm': well_ordering_charm,
    'calabi_yau_manifold': calabi_yau_manifold,
    'differential_manifold': differential_manifold,
    'g2_manifold': g2_manifold,
    'klein_bottle': klein_bottle,
    'omnicellular_genome': omnicellular_genome,
    'stabiliser_matrix': stabiliser_matrix,
    'transfinite_weave': transfinite_weave,
    'tree_simulacrum': tree_simulacrum,
    'cryocooler_unit': cryocooler_unit,
    'negentropy_cell': negentropy_cell,
    'absolute_infinity': absolute_infinity,
    'eternity_catalyst': eternity_catalyst,
    'higgs_catalyst': higgs_catalyst,
    'omega_point': omega_point,
    'omnilex': omnilex,
    'omniscience_catalyst': omniscience_catalyst,
    'oracle_lemma': oracle_lemma,
    'proof_of_everything': proof_of_everything,
    'spacetime_catalyst': spacetime_catalyst,
    'theory_of_everything': theory_of_everything,
    'biosphere_compendium': biosphere_compendium,
    'buckyball_cage': buckyball_cage,
    'einstein_field_core': einstein_field_core,
    'graviton_wisp_core': graviton_wisp_core,
    'hypersphere_fragment': hypersphere_fragment,
    'protostar_core': protostar_core,
    'red_giant_envelope': red_giant_envelope,
    'synthetic_cell': synthetic_cell,
    'white_dwarf_core': white_dwarf_core,
    'entangled_photon_pair': entangled_photon_pair,
    'lepton_triplet': lepton_triplet,
    'glueball_core': glueball_core,
    'muonic_catalyst': muonic_catalyst,
    'positronium_hydride': positronium_hydride,
    'strange_matter_pellet': strange_matter_pellet,
    'cherenkov_wafer': cherenkov_wafer,
    'circuit_substrate': circuit_substrate,
    'cosmic_board': cosmic_board,
    'cosmic_logic_wafer': cosmic_logic_wafer,
    'degenerate_armour_plate': degenerate_armour_plate,
    'graphene_sheet': graphene_sheet,
    'hawking_absorber_plate': hawking_absorber_plate,
    'hyperdimensional_logic_wafer': hyperdimensional_logic_wafer,
    'hyperdimensional_wafer': hyperdimensional_wafer,
    'neutronium_plate': neutronium_plate,
    'photonic_board': photonic_board,
    'photonic_logic_wafer': photonic_logic_wafer,
    'quadrupole_pole_tip': quadrupole_pole_tip,
}

RECIPES = {
    'dark_matter_lens': (('rock',), ()),
    'gravitic_lens_element': (('tumble',), ('sparkles',)),
    'photonic_lens': ((), ('glint',)),
    'tesseract_lens': (('hue_soft',), ('sparkles',)),
    'closed_timelike_curve': ((), ()),
    'compactified_circle': (('rock',), ('glint',)),
    'hopf_fibration': (('breathe',), ()),
    'recombinant_dna': (('bob',), ()),
    'temporal_knot': (('rock', 'breathe'), ('sparkles',)),
    'well_ordering_charm': (('rock',), ()),
    'calabi_yau_manifold': (('hue_soft',), ('sparkles',)),
    'differential_manifold': (('rock',), ()),
    'g2_manifold': ((), ('sparkles',)),
    'klein_bottle': (('rock',), ('glint',)),
    'omnicellular_genome': (('bob',), ()),
    'stabiliser_matrix': (('spin',), ()),
    'transfinite_weave': ((), ('sparkles',)),
    'tree_simulacrum': (('rock',), ()),
    'cryocooler_unit': ((), ('glint',)),
    'negentropy_cell': (('rock',), ()),
    'absolute_infinity': ((), ('sparkles',)),
    'eternity_catalyst': (('rock',), ('glint',)),
    'higgs_catalyst': ((), ()),
    'omega_point': ((), ()),
    'omnilex': (('bob',), ('sparkles',)),
    'omniscience_catalyst': ((), ()),
    'oracle_lemma': (('bob',), ()),
    'proof_of_everything': (('rock',), ()),
    'spacetime_catalyst': (('rock',), ('sparkles',)),
    'theory_of_everything': ((), ('sparkles',)),
    'biosphere_compendium': (('tumble',), ('embers',)),
    'buckyball_cage': ((), ('glint',)),
    'einstein_field_core': (('heartbeat',), ()),
    'graviton_wisp_core': ((), ()),
    'hypersphere_fragment': (('tumble',), ('sparkles',)),
    'protostar_core': ((), ()),
    'red_giant_envelope': (('pulse',), ('embers',)),
    'synthetic_cell': (('rock',), ()),
    'white_dwarf_core': ((), ()),
    'entangled_photon_pair': ((), ()),
    'lepton_triplet': ((), ()),
    'glueball_core': (('pulse',), ()),
    'muonic_catalyst': ((), ()),
    'positronium_hydride': (('rock',), ()),
    'strange_matter_pellet': (('breathe',), ()),
    'cherenkov_wafer': (('rock',), ()),
    'circuit_substrate': (('tumble',), ()),
    'cosmic_board': (('rock',), ()),
    'cosmic_logic_wafer': (('spin',), ()),
    'degenerate_armour_plate': ((), ()),
    'graphene_sheet': ((), ()),
    'hawking_absorber_plate': (('breathe',), ()),
    'hyperdimensional_logic_wafer': (('bob',), ('glint',)),
    'hyperdimensional_wafer': (('hue_soft',), ()),
    'neutronium_plate': ((), ()),
    'photonic_board': (('rock',), ()),
    'photonic_logic_wafer': ((), ('glint',)),
    'quadrupole_pole_tip': (('rock',), ()),
}

NARRATIVES = {
    'closed_timelike_curve': nar_closed_timelike_curve,
    'absolute_infinity': nar_absolute_infinity,
    'omega_point': nar_omega_point,
}

FRAMES = {}
