"""
1.5.0 item art (Small Wonders): small 3D models, lit and animated like every other item of the mod (helpers from
items3d_13.py and items3d_14.py; the model faces the camera on its -z side).

Run:  python3 tools/items3d_15.py            (writes the textures)
      python3 tools/items3d_15.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
import items3d_14 as P14
from render3d import Part, Material, sd_sphere, sd_box, sd_torus, sd_cyl, sd_capsule, sd_gem, rotx, roty, rotz
from items3d_new import V, sd_ellipsoid, tube, capsule_chain, em_all, em_bands, shade_noise, em_spots, glass
from items3d_13 import stripes, weave, mottle

TAU = math.tau
B = {}
MOTIONS = {}


def reg(iid, motions=(('rock',), ('glint',))):
    def deco(fn):
        B[iid] = fn
        MOTIONS[iid] = motions
        return fn
    return deco


@reg('resonant_shard', (('rock',), ('glint',)))
def b_shard(c1, c2, t):
    """A cut amethyst shard, its faces brightening and dimming as it rings."""
    k = 0.6 + 0.4 * math.sin(TAU * t * 2)
    mats = [Material('#b080e0', '#ffffff', emissive=tuple(np.array([0.6, 0.4, 0.9]) * 0.4 * k), pattern=shade_noise(5, 0.15, 3), metal=0.9)]
    parts = [Part(lambda p: sd_gem(p, 0.28, 0.75), 0, rot=rotz(0.35))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5, zoom=1.1)


@reg('faraday_mesh', (('rock',), ('glint',)))
def b_mesh(c1, c2, t):
    """A square of woven copper mesh on a frame."""
    mats = [Material('#c87838', '#ffd0a0', pattern=weave(18, 0.4), metal=0.95), Material('#6a4020', '#a07050', metal=0.6)]
    parts = [Part(lambda p: sd_box(p, (0.5, 0.5, 0.02)), 0, rot=rotx(-0.25) @ roty(0.3)),
             Part(lambda p: np.maximum(sd_box(p, (0.56, 0.56, 0.05)), -sd_box(p, (0.48, 0.48, 0.1))), 1, rot=rotx(-0.25) @ roty(0.3))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.4, zoom=1.05)


@reg('strange_attractor', (('rock',), ('glint',)))
def b_attractor(c1, c2, t):
    """A glass sphere with the butterfly of the Lorenz attractor traced in light inside, a bead running round it."""
    mats = [glass('#c0d8f0'), Material('#ff7070', '#ffffff', emissive='#ff6060', pattern=em_all(), metal=0.3),
            Material('#ffffff', '#ffffff', emissive='#ffffff', pattern=em_all(), metal=0.3)]

    def path(u):
        a = u * TAU * 2
        side = 1 if u < 0.5 else -1
        return V(side * 0.18 + 0.16 * math.sin(a) * side, 0.17 * math.cos(a), -0.42)
    parts = [Part(lambda p: sd_sphere(p, 0.5), 0)]
    parts += tube(path, 30, 0.022, 1)
    b = path((t * 1.0) % 1.0)
    parts.append(Part(lambda p: sd_sphere(p, 0.05), 2, pos=tuple(b + V(0, 0, -0.03))))
    return parts, mats, dict(view_pitch=0.15, view_yaw=0.2, zoom=1.05)


def trefoil(u, s=0.22):
    a = u * TAU
    return V(s * (math.sin(a) + 2 * math.sin(2 * a)), s * (math.cos(a) - 2 * math.cos(2 * a)), -s * math.sin(3 * a))


@reg('molecular_knot', (('spin',), ('glint',)))
def b_knot(c1, c2, t):
    """A trefoil knot in a single strand, beaded with atoms."""
    mats = [Material('#d0a050', '#fff0c0', metal=0.6), Material('#60c8e0', '#ffffff', metal=0.7)]
    parts = tube(lambda u: trefoil(u, 0.17), 40, 0.05, 0)
    for k in range(0, 40, 5):
        parts.append(Part(lambda p: sd_sphere(p, 0.065), 1, pos=tuple(trefoil(k / 40, 0.17))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.3, zoom=1.05)


@reg('knotted_polymer', (('rock',), ('glint',)))
def b_knotted(c1, c2, t):
    """A short rope of fibre, its strands knotted through each other."""
    mats = [Material('#e0c890', '#fff8e0', pattern=stripes(40, 2, 0.2, 0.15), metal=0.3), Material('#b08840', '#e0c080', metal=0.3)]
    parts = []
    for k in range(3):
        ph = k * TAU / 3
        parts += tube(lambda u, ph=ph: V(-0.7 + 1.4 * u, 0.07 * math.sin(u * TAU * 3 + ph), 0.07 * math.cos(u * TAU * 3 + ph)), 24, 0.05, k % 2)
    parts += tube(lambda u: V(0, 0, 0) + trefoil(u, 0.1), 30, 0.045, 1)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.05)


@reg('von_neumann_constructor', (('rock',), ()))
def b_vn(c1, c2, t):
    """A slab of lattice with a machine of lit cells on it, its tape of cells marching out as it reads."""
    mats = [Material('#1a2a1e', '#3a5a40', metal=0.6), Material('#70f070', '#ffffff', emissive='#60ff60', pattern=em_all(), metal=0.3),
            Material('#305838', '#507858', metal=0.5)]
    parts = [Part(lambda p: sd_box(p, (0.55, 0.55, 0.06), 0.02), 0, rot=rotx(-0.4))]
    rng = np.random.default_rng(15)
    cells = [(x, y) for x in range(-3, 2) for y in range(-2, 3) if rng.random() < 0.55]
    shift = int(t * 6) % 6
    cells += [(2 + k, 0) for k in range(shift)]
    for (x, y) in cells:
        parts.append(Part(lambda p: sd_box(p, (0.045, 0.045, 0.03)), 1 if (x, y) in cells[:-shift or None] else 2,
                          pos=tuple(rotx(-0.4) @ V(x * 0.11, y * 0.11, -0.07))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.25, zoom=1.0)


@reg('zero_knowledge_proof', (('rock',), ('glint',)))
def b_zkp(c1, c2, t):
    """A sealed scroll whose wax seal glows: a proof that shows nothing of what it proves."""
    mats = [Material('#e8dcc0', '#ffffff', metal=0.05), Material('#7070f0', '#ffffff', emissive='#8080ff', pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_cyl(p, 0.16, 0.55, 0.02), 0, rot=rotz(math.pi / 2) @ rotx(0.3)),
             Part(lambda p: sd_cyl(p, 0.14, 0.04, 0.02), 1, pos=(0, 0, -0.17), rot=rotx(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.3, zoom=1.05)


@reg('grover_oracle', (('rock',), ('glint',)))
def b_oracle(c1, c2, t):
    """A gold chip with a row of qubits along it, one of them lit: the marked answer."""
    mats = [Material('#c89a40', '#fff0c0', metal=0.9), Material('#3a3c44', '#7a7c84', metal=0.5),
            Material('#80e0ff', '#ffffff', emissive='#80e0ff', pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.5, 0.36, 0.04), 0.02), 0, rot=rotx(-0.3)),
             Part(lambda p: sd_box(p, (0.36, 0.2, 0.02)), 1, pos=tuple(rotx(-0.3) @ V(0, 0, -0.04)), rot=rotx(-0.3))]
    lit = int(t * 5) % 5
    for k in range(5):
        parts.append(Part(lambda p: sd_box(p, (0.035, 0.035, 0.02)), 2 if k == lit else 0, pos=tuple(rotx(-0.3) @ V(-0.28 + k * 0.14, 0, -0.065))))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.25, zoom=1.0)


def _vial(liquid, glow):
    mats = [glass('#e8f0f8'), Material(liquid, '#ffffff', pattern=shade_noise(4, 0.08, 2), metal=0.75),
            Material('#3a3c44', '#7a7c84', metal=0.6)]
    parts = [Part(lambda p: sd_cyl(p, 0.2, 0.3, 0.06), 1, pos=(0, -0.15, 0)),
             Part(lambda p: sd_cyl(p, 0.12, 0.14, 0.03), 0, pos=(0, 0.25, 0)),
             Part(lambda p: sd_cyl(p, 0.13, 0.07, 0.02), 2, pos=(0, 0.45, 0))]
    return parts, mats


@reg('vestibular_suppressant', (('rock',), ('glint',)))
def b_vest(c1, c2, t):
    """A capped vial of pale blue suspension."""
    parts, mats = _vial('#70b8f0', None)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.4, zoom=1.15)


@reg('anti_inflammatory', (('rock',), ('glint',)))
def b_anti(c1, c2, t):
    """A capped vial of amber tincture."""
    parts, mats = _vial('#e0a040', None)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.4, zoom=1.15)


@reg('tomogram', (('rock',), ('glint',)))
def b_tomo(c1, c2, t):
    """A stack of thin glass slides, each a slice of a scanned fossil, the bright bone moving from slide to slide."""
    mats = [glass('#c8d8e8'), Material('#e8eef4', '#ffffff', emissive='#a0c0e0', pattern=em_all(), metal=0.3)]
    parts = []
    for k in range(4):
        z = -0.25 + k * 0.17
        parts.append(Part(lambda p: sd_box(p, (0.45, 0.32, 0.015)), 0, pos=(k * 0.05, k * 0.04, z)))
        parts.append(Part(lambda p, k=k: sd_ellipsoid(p, (0.14 + 0.03 * math.sin(k + t * TAU), 0.07, 0.018)), 1, pos=(k * 0.05, k * 0.04, z - 0.01)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.05)


ITEMS = sorted(B)
TEX = P13.TEX


def frames(iid, n=40):
    import anim, items3d as I
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


if __name__ == '__main__':
    import items3d as I
    from PIL import Image, ImageDraw
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        cols, S = 6, 100
        W = Image.new('RGBA', (cols * S, ((len(ITEMS) + cols - 1) // cols) * (S + 12)), (58, 58, 64, 255)); d = ImageDraw.Draw(W)
        for i, iid in enumerate(ITEMS):
            parts, mats, view = B[iid]('#ffffff', '#ffffff', 0.15)
            img = I.render(parts, mats, **view)
            im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((96, 96), Image.NEAREST)
            x, y = (i % cols) * S, (i // cols) * (S + 12)
            W.alpha_composite(im, (x + 2, y + 2)); d.text((x + 2, y + 98), iid[:16], fill=(255, 255, 255, 255))
        W.save(sys.argv[2])
        print('preview', sys.argv[2])
    else:
        only = sys.argv[1:]
        for iid in ITEMS:
            if only and iid not in only:
                continue
            P13.save_strip(frames(iid), os.path.join(TEX, iid + '.png'))
        print('rendered', len(only) or len(ITEMS), 'items')
