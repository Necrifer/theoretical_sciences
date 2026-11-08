"""
1.4.0 item art: the materials of Mind and Language and Engineering of the Large, as small 3D models, lit and animated
like every other item of the mod (see items3d_13.py for the helpers and the renderer).

Run:  python3 tools/items3d_14.py            (writes the textures)
      python3 tools/items3d_14.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
from render3d import Part, Material, sd_sphere, sd_box, sd_torus, sd_cyl, sd_capsule, sd_gem, rotx, roty, rotz
from items3d_new import V, sd_ellipsoid, smin, tube, capsule_chain, em_all, em_bands, shade_noise, em_spots, glass
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


# ======================================================================= Mind and Language
@reg('myelinated_axon', (('rock',), ()))
def b_axon(c1, c2, t):
    """A nerve fibre in its sheath: white myelin segments with the bare nodes between, a spike of light running down it."""
    mats = [Material('#f4ead8', '#ffffff', pattern=stripes(16, 1, 0.12, 0.12), metal=0.35),
            Material('#ff70d0', '#ffffff', emissive='#ff70e0', pattern=em_all(), metal=0.3)]
    path = lambda u: V(-0.15 + 0.3 * math.sin(u * 3), -0.75 + 1.5 * u, 0.1 * math.cos(u * 4))
    parts = tube(path, 14, 0.07, 1)
    for k in range(5):
        u0, u1 = k / 5 + 0.05, (k + 1) / 5 - 0.05
        parts += capsule_chain([path(u0 + (u1 - u0) * i / 3) for i in range(4)], 0.13, 0)
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.5, zoom=1.15)


@reg('synaptic_lattice', (('rock',), ('glint',)))
def b_lattice(c1, c2, t):
    """A small cube of crystal holding a lattice of synapses, its nodes lighting one after another as it learns."""
    mats = [glass('#d8c8f0'), Material('#b080f0', '#ffffff', emissive='#c090ff', pattern=em_spots(t, 0.5, 3, 6), metal=0.5),
            Material('#6a5a80', '#a090c0', metal=0.7)]
    parts = [Part(lambda p: sd_box(p, (0.42, 0.42, 0.42), 0.04), 0)]
    for x in (-0.22, 0.0, 0.22):
        for y in (-0.22, 0.0, 0.22):
            for z in (-0.44,):
                parts.append(Part(lambda p: sd_sphere(p, 0.07), 1, pos=(x, y, z)))
    for x in (-0.22, 0.0, 0.22):
        for z in (-0.22, 0.0, 0.22):
            parts.append(Part(lambda p: sd_sphere(p, 0.07), 1, pos=(x, 0.44, z)))
    for a in (-0.22, 0.22):
        parts.append(Part(lambda p: sd_box(p, (0.3, 0.015, 0.015)), 2, pos=(0, a, -0.44)))
        parts.append(Part(lambda p: sd_box(p, (0.015, 0.3, 0.015)), 2, pos=(a, 0, -0.44)))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.7, zoom=1.05)


@reg('rhodopsin_retina', (('breathe',), ()))
def b_retina(c1, c2, t):
    """A cupped sheet of rod cells, deep purple-red with rhodopsin, bleaching paler and recovering as light falls on it."""
    k = 0.5 + 0.5 * math.sin(TAU * t)
    mats = [Material(tuple(np.array([0.62, 0.22, 0.42]) * (0.7 + 0.3 * k) + np.array([0.3, 0.3, 0.3]) * (1 - k) * 0.4), '#ffc0e0', pattern=em_spots(t, 0.8, 2, 14), metal=0.35),
            Material('#3a2030', '#805060', metal=0.2)]
    parts = [Part(lambda p: np.maximum(sd_sphere(p, 0.55), -sd_sphere(p - V(0, 0, 0.42), 0.58)), 0),
             Part(lambda p: sd_cyl(p, 0.06, 0.25), 1, pos=(0, 0, -0.62), rot=rotx(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.35, zoom=1.15)


@reg('low_light_visor', (('rock',), ('glint',)))
def b_visor(c1, c2, t):
    """Goggles of hull plate with two intensifier tubes, their lenses glowing the anglerfish's green as they amplify."""
    pulse = 0.65 + 0.35 * math.sin(TAU * t)
    mats = [Material('#4a5058', '#9aa4b0', metal=0.8), Material('#90ffd8', '#ffffff', emissive=tuple(np.array([0.4, 1.0, 0.8]) * pulse), pattern=em_all(), metal=0.3),
            Material('#2a2c30', '#606468', metal=0.4)]
    parts = [Part(lambda p: sd_box(p, (0.62, 0.16, 0.1), 0.05), 0)]
    for sx in (-0.3, 0.3):
        parts.append(Part(lambda p: sd_cyl(p, 0.22, 0.2, 0.02), 0, pos=(sx, 0, -0.2), rot=rotx(math.pi / 2)))
        parts.append(Part(lambda p: sd_cyl(p, 0.17, 0.03), 1, pos=(sx, 0, -0.42), rot=rotx(math.pi / 2)))
    parts += tube(lambda u: V(-0.62 - 0.1 * math.sin(u * math.pi), -0.05 * u, 0.4 * u), 6, 0.035, 2)
    parts += tube(lambda u: V(0.62 + 0.1 * math.sin(u * math.pi), -0.05 * u, 0.4 * u), 6, 0.035, 2)
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.0)


def _tablet(stone, glyph_col, t, glow=True):
    mats = [Material(stone, '#a0a0a8', pattern=mottle(18, 0.15, 3), metal=0.15),
            Material(glyph_col, '#ffffff', emissive=glyph_col if glow else None, pattern=em_bands(9, t, 1, 0.25, 1) if glow else None, metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.4, 0.55, 0.07), 0.04), 0, rot=rotz(0.12))]
    rng = np.random.default_rng(5)
    for row in range(4):
        x = -0.28
        while x < 0.28:
            w = rng.uniform(0.05, 0.1)
            parts.append(Part(lambda p, w=w: sd_box(p, (w / 2, 0.035, 0.02)), 1, pos=tuple(rotz(0.12) @ V(x + w / 2, 0.36 - row * 0.22, -0.1))))
            x += w + 0.04
    return parts, mats


@reg('glyph_tablet', (('rock',), ()))
def b_tablet(c1, c2, t):
    """A stone tablet dense with glyphs in rows, their grooves still glowing faintly."""
    parts, mats = _tablet('#5a5c66', '#5ae0d0', t)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.35, zoom=1.1)


@reg('lexicon_fragment', (('rock',), ('glint',)))
def b_lexicon(c1, c2, t):
    """A sheet of paper: glyphs down one side, their meanings written beside them."""
    mats = [Material('#ece4cc', '#ffffff', metal=0.05), Material('#5ae0d0', '#ffffff', emissive='#40c0b0', pattern=em_all(), metal=0.2),
            Material('#3a3028', '#605040', metal=0.1)]
    parts = [Part(lambda p: sd_box(p, (0.42, 0.56, 0.015), 0.01), 0, rot=rotz(-0.1) @ rotx(0.1))]
    for row in range(5):
        y = 0.4 - row * 0.19
        parts.append(Part(lambda p: sd_box(p, (0.06, 0.05, 0.02)), 1, pos=tuple(rotz(-0.1) @ V(-0.27, y, 0.02))))
        parts.append(Part(lambda p: sd_box(p, (0.2, 0.02, 0.02)), 2, pos=tuple(rotz(-0.1) @ V(0.08, y, 0.02))))
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.3, zoom=1.1)


@reg('universal_grammar', (('spin',), ('glint',)))
def b_grammar(c1, c2, t):
    """A tree of glowing nodes: a sentence parsed into its grammar, turning slowly."""
    mats = [Material('#5ae0d0', '#ffffff', emissive='#60f0e0', pattern=em_all(), metal=0.3), Material('#c0f0e8', '#ffffff', metal=0.6)]
    nodes = [V(0, 0.55, 0), V(-0.35, 0.15, 0.1), V(0.35, 0.15, -0.1), V(-0.55, -0.3, 0.1), V(-0.15, -0.3, 0.2), V(0.2, -0.3, -0.2), V(0.5, -0.3, 0.0),
             V(0.05, -0.65, 0.25), V(-0.3, -0.65, 0.2)]
    edges = [(0, 1), (0, 2), (1, 3), (1, 4), (2, 5), (2, 6), (4, 7), (4, 8)]
    parts = [Part(lambda p: sd_sphere(p, 0.09), 0, pos=tuple(n)) for n in nodes]
    for a, b in edges:
        parts.append(Part(lambda p, a=nodes[a], b=nodes[b]: sd_capsule(p, a, b, 0.025), 1))
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.4, zoom=1.0)


@reg('engram', (('rock',), ('glint',)))
def b_engram(c1, c2, t):
    """A memory in a crystal: a tangle of glowing pathways frozen in a violet gem."""
    mats = [Material('#c8a0f0', '#ffffff', pattern=shade_noise(9, 0.2, 2), metal=0.9), Material('#ffe0ff', '#ffffff', emissive='#ff90ff', pattern=em_bands(7, t, 1, 0.2, 2), metal=0.4)]
    parts = [Part(lambda p: sd_gem(p, 0.42, 0.62), 0)]
    parts += tube(lambda u: V(0.18 * math.sin(u * 9), -0.4 + 0.8 * u, 0.18 * math.cos(u * 7)), 14, 0.025, 1)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.6, zoom=1.05)


@reg('dream_polyp', (('breathe',), ()))
def b_polyp(c1, c2, t):
    """A lobe of a Dreaming Colony: a translucent lilac sac with cell bodies glowing inside and a tendril trailing."""
    k = 0.5 + 0.5 * math.sin(TAU * t)
    mats = [Material('#e0d0f4', '#ffffff', pattern=shade_noise(6, 0.12, 4), metal=0.85),
            Material('#ffe8ff', '#ffffff', emissive=tuple(np.array([0.85, 0.55, 1.0]) * (0.5 + 0.5 * k)), pattern=em_all(), metal=0.3),
            Material('#c8b0dc', '#f0e0ff', metal=0.3)]
    parts = [Part(lambda p: sd_ellipsoid(p, (0.42, 0.38, 0.4)), 0, pos=(0, 0.1, 0))]
    for pos in ((0.12, 0.28, -0.33), (-0.22, 0.12, -0.32), (0.2, -0.05, -0.35), (-0.05, 0.42, -0.22)):
        parts.append(Part(lambda p: sd_sphere(p, 0.085), 1, pos=pos))
    parts += tube(lambda u: V(0.05 * math.sin(u * 6 + t * TAU), -0.25 - 0.5 * u, 0.1 * u), 8, 0.04, 2)
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.1)


# ======================================================================= Engineering of the Large
def _ibeam(steel, l=0.75, rot=None):
    rot = np.eye(3) if rot is None else rot
    parts = [Part(lambda p: sd_box(p, (l, 0.035, 0.18), 0.01), 0, pos=tuple(rot @ V(0, 0.2, 0)), rot=rot),
             Part(lambda p: sd_box(p, (l, 0.035, 0.18), 0.01), 0, pos=tuple(rot @ V(0, -0.2, 0)), rot=rot),
             Part(lambda p: sd_box(p, (l, 0.18, 0.03), 0.005), 0, rot=rot)]
    return parts


@reg('rated_i_beam', (('rock',), ('glint',)))
def b_beam(c1, c2, t):
    """A steel I-beam with its test stamp: a yellow tag on the web."""
    mats = [Material('#8a8e94', '#d0d4d8', metal=0.85), Material('#e8c020', '#fff0a0', metal=0.3)]
    rot = rotz(0.55) @ roty(0.3)
    parts = _ibeam(0, rot=rot)
    parts.append(Part(lambda p: sd_box(p, (0.1, 0.07, 0.02)), 1, pos=tuple(rot @ V(0.2, 0, -0.04)), rot=rot))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6, zoom=1.0)


@reg('carbon_fibre_composite', (('rock',), ('glint',)))
def b_cfrp(c1, c2, t):
    """A panel of woven carbon in glossy resin, the weave catching the light."""
    mats = [Material('#2a2c30', '#8a9098', pattern=weave(22, 0.25), metal=0.9, keep_dark=True)]
    parts = [Part(lambda p: sd_box(p, (0.55, 0.55, 0.04), 0.03), 0, rot=rotz(0.2) @ rotx(-0.3))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5, zoom=1.05)


@reg('nanotube_composite', (('rock',), ('glint',)))
def b_cnt(c1, c2, t):
    """A bar of aligned nanotubes in a pale matrix, iridescent along its length."""
    mats = [Material('#3a4048', '#b0c8e0', pattern=stripes(60, 2, 0.2, 0.25), metal=0.95), Material('#80a0ff', '#ffffff', emissive='#6080ff',
                                                                                                    pattern=em_bands(10, t, 2, 0.12, 1), metal=0.6)]
    parts = [Part(lambda p: sd_box(p, (0.16, 0.16, 0.7), 0.04), 0, rot=roty(0.6) @ rotx(0.4)),
             Part(lambda p: sd_box(p, (0.165, 0.02, 0.71)), 1, rot=roty(0.6) @ rotx(0.4))]
    return parts, mats, dict(view_pitch=0.4, view_yaw=0.6, zoom=1.0)


@reg('wear_coating', (('rock',), ('glint',)))
def b_dlc(c1, c2, t):
    """A coated bearing race: a steel ring under a black, mirror-bright film of diamond-like carbon."""
    mats = [Material('#1c1e22', '#a0a8c0', metal=0.98, keep_dark=True), Material('#c8ccd0', '#ffffff', metal=0.9)]
    parts = [Part(lambda p: sd_torus(p, 0.42, 0.12), 0, rot=rotx(1.1)),
             Part(lambda p: sd_torus(p, 0.42, 0.06), 1, pos=(0, 0.02, 0), rot=rotx(1.1))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.5, zoom=1.15)


@reg('passivated_plate', (('rock',), ('glint',)))
def b_passivated(c1, c2, t):
    """Steel plate with a grey-blue phosphated finish, its film catching faint interference colours."""
    mats = [Material('#5a6878', '#b0c0d8', pattern=mottle(20, 0.08, 6), metal=0.8)]
    parts = [Part(lambda p: sd_box(p, (0.55, 0.06, 0.5), 0.03), 0, rot=rotx(0.2))]
    return parts, mats, dict(view_pitch=0.55, view_yaw=0.6, zoom=1.0)


@reg('rust_flakes', (('rock',), ()))
def b_rust(c1, c2, t):
    """Curled flakes of red-brown oxide."""
    mats = [Material('#8a4020', '#d08050', pattern=mottle(26, 0.25, 7), metal=0.2)]
    rng = np.random.default_rng(8)
    parts = []
    for k in range(9):
        pos = V(*rng.uniform(-0.38, 0.38, 3)) * V(1, 0.8, 0.5)
        parts.append(Part(lambda p: np.maximum(sd_torus(p, 0.16, 0.035), -p[..., 2]), 0, pos=tuple(pos), rot=rotx(rng.uniform(0.6, 1.6)) @ rotz(rng.uniform(-1, 1))))
    return parts, mats, dict(view_pitch=0.5, view_yaw=0.6, zoom=1.15)


@reg('tether_spool', (('spin',), ('glint',)))
def b_spool(c1, c2, t):
    """A drum wound with dark nanotube ribbon, a tail of it trailing."""
    mats = [Material('#3a4048', '#90a8c0', pattern=stripes(80, 1, 0.3, 0.2), metal=0.95), Material('#d0a830', '#fff0a0', metal=0.6)]
    parts = [Part(lambda p: sd_cyl(p, 0.36, 0.3, 0.02), 0, rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.5, 0.035, 0.01), 1, pos=(-0.33, 0, 0), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.5, 0.035, 0.01), 1, pos=(0.33, 0, 0), rot=rotz(math.pi / 2)),
             Part(lambda p: sd_box(p, (0.25, 0.01, 0.4)), 0, pos=(0, -0.36, 0.3), rot=rotx(0.4))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.55, zoom=1.0)


@reg('shkadov_mirror_film', (('rock',), ('glint',)))
def b_film(c1, c2, t):
    """A sheet of mirror film so thin it ripples, golden on its sail side."""
    mats = [Material('#e8dca0', '#ffffff', metal=1.0), Material('#c0a040', '#ffe080', metal=0.9)]
    def sheet(p):
        y = p[..., 1] - 0.08 * np.sin(p[..., 0] * 6 + t * TAU)
        q = np.stack([p[..., 0], y, p[..., 2]], -1)
        return sd_box(q, (0.62, 0.025, 0.55))
    parts = [Part(sheet, 0, rot=rotx(1.0)), Part(lambda p: sd_cyl(p, 0.04, 0.58), 1, pos=(-0.64, 0, 0), rot=rotx(1.0) @ rotx(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.45, view_yaw=0.5, zoom=1.05)


# ======================================================================= render and write
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
