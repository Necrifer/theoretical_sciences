"""
1.7.0 item art (the Field Syntheses and their chains): small 3D models, lit and animated like every other item of the mod (helpers from
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




def _crystal(col, glow, t, n=6, r=0.55, h=0.85):
    """A faceted crystal with a glowing core, turning: the plain look of a Field Synthesis."""
    k = 0.6 + 0.4 * math.sin(TAU * t)
    return [Material(col, '#ffffff', emissive=tuple(np.array(hexrgb(glow)) * 0.35 * k), pattern=shade_noise(5, 0.12, 2), metal=0.85),
            Material(glow, '#ffffff', emissive=glow, pattern=em_all(), metal=0.3)]


def hexrgb(h):
    h = h.lstrip('#')
    return [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]


@reg('unified_field_lattice', (('spin',), ('glint',)))
def b_ufl(c1, c2, t):
    """Four interlocking rings, one per force, round a white-hot point."""
    mats = [Material('#a0c0ff', '#ffffff', metal=0.9), Material('#ffffff', '#ffffff', emissive='#fff8e0', pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_sphere(p, 0.12), 1)]
    for k in range(4):
        parts.append(Part(lambda p: sd_torus(p, 0.5, 0.045), 0, rot=rotx(k * math.pi / 4 + t * TAU / 4) @ roty(k * 0.7)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.05)


@reg('perennial_catalyst_matrix', (('rock',), ('glint',)))
def b_pcm(c1, c2, t):
    """A cube of framework, every node a green catalyst site, glowing in turn."""
    mats = [Material('#c0c8b0', '#ffffff', metal=0.8), Material('#60ff90', '#ffffff', emissive='#40ff80', pattern=em_all(), metal=0.3)]
    parts = []
    for x in (-0.35, 0.35):
        for y in (-0.35, 0.35):
            parts.append(Part(lambda p: sd_capsule(p, V(0, 0, -0.35), V(0, 0, 0.35), 0.035), 0, pos=(x, y, 0)))
            parts.append(Part(lambda p: sd_capsule(p, V(-0.35, 0, 0), V(0.35, 0, 0), 0.035), 0, pos=(0, x, y)))
            parts.append(Part(lambda p: sd_capsule(p, V(0, -0.35, 0), V(0, 0.35, 0), 0.035), 0, pos=(x, 0, y)))
            for z in (-0.35, 0.35):
                parts.append(Part(lambda p: sd_sphere(p, 0.09), 1, pos=(x, y, z)))
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.6, zoom=1.0)


@reg('proof_lattice', (('rock',), ('glint',)))
def b_pl(c1, c2, t):
    """A slab of cells, the live ones lit, a check mark burnt into its face."""
    mats = [Material('#3a3a60', '#8080c0', pattern=weave(14, 0.2), metal=0.5), Material('#80ff80', '#ffffff', emissive='#60ff60', pattern=em_spots(t, 0.75, 4, 10), metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.55, 0.55, 0.08), 0.04), 0, rot=rotx(-0.3) @ roty(0.3)),
             ] + [Part(lambda p: sd_box(p, (0.07, 0.07, 0.03)), 1, pos=(x * 0.18, y * 0.18, -0.1), rot=rotx(-0.3) @ roty(0.3)) for x, y in ((-2, -2), (0, -2), (2, 0), (-1, 1), (1, 1), (0, 2), (-2, 2))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.05)


@reg('living_archive', (('rock',), ('glint',)))
def b_la(c1, c2, t):
    """A glass ampoule of green culture with a helix coiled through it."""
    mats = [glass('#c8f0d0'), Material('#40c070', '#c0ffd0', emissive='#208040', pattern=em_bands(10, t, 1, 0.5), metal=0.2),
            Material('#f0e080', '#ffffff', emissive='#f0d060', pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_capsule(p, V(0, -0.5, 0), V(0, 0.5, 0), 0.3), 0), Part(lambda p: sd_capsule(p, V(0, -0.42, 0), V(0, 0.3, 0), 0.31), 1)]
    parts += tube(lambda u: V(0.15 * math.cos(u * TAU * 2 + t * TAU), -0.4 + 0.8 * u, -0.26 + 0.15 * math.sin(u * TAU * 2 + t * TAU)), 24, 0.03, 2)
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.4, zoom=1.0)


@reg('engineered_mind', (('rock',), ('glint',)))
def b_em(c1, c2, t):
    """A pink cortex held in a frame of steel girders, sparks running over it."""
    mats = [Material('#e090b0', '#ffd0e0', pattern=mottle(16, 0.3, 3), metal=0.2), Material('#8a8e96', '#e0e4ea', metal=1.0),
            Material('#ffffff', '#ffffff', emissive='#a0e0ff', pattern=em_spots(t, 0.9, 6, 12), metal=0.3)]
    parts = [Part(lambda p: sd_ellipsoid(p, (0.42, 0.3, 0.34)), 0)] + [Part(lambda p: sd_sphere(p, 0.05), 2, pos=(x, y, -0.3)) for x, y in ((-0.2, 0.05), (0.05, 0.15), (0.2, -0.05))]
    for x in (-0.5, 0.5):
        parts.append(Part(lambda p: sd_box(p, (0.04, 0.5, 0.04)), 1, pos=(x, 0, -0.3)))
    parts.append(Part(lambda p: sd_box(p, (0.54, 0.04, 0.04)), 1, pos=(0, -0.45, -0.3)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.0)


@reg('planetary_survey', (('spin',), ('glint',)))
def b_ps(c1, c2, t):
    """A small world in a cage of orbits, a probe riding one of them."""
    mats = [Material('#4070c0', '#a0d0ff', pattern=mottle(7, 0.35, 5), metal=0.3), Material('#c0c8d0', '#ffffff', metal=0.9),
            Material('#ffe080', '#ffffff', emissive='#ffd060', pattern=em_all(), metal=0.3)]
    a = t * TAU
    parts = [Part(lambda p: sd_sphere(p, 0.36), 0), Part(lambda p: sd_torus(p, 0.55, 0.02), 1, rot=rotx(1.2)),
             Part(lambda p: sd_torus(p, 0.6, 0.02), 1, rot=rotz(0.6) @ rotx(1.4)),
             Part(lambda p: sd_sphere(p, 0.06), 2, pos=(0.55 * math.cos(a), 0.55 * math.sin(a) * 0.36, 0.55 * math.sin(a) * 0.93))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.05)


@reg('deep_record', (('rock',), ('glint',)))
def b_dr(c1, c2, t):
    """A tall core of layers, from soil at the top to a burning point at the bottom: time read end to end."""
    cols = ['#8a6a48', '#6a6a6a', '#a08040', '#504060', '#302040']
    mats = [Material(c, '#ffffff', metal=0.2) for c in cols] + [Material('#ffd080', '#ffffff', emissive='#ffb040', pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_cyl(p, 0.24, 0.09), k, pos=(0, 0.48 - k * 0.19, 0)) for k in range(5)]
    parts.append(Part(lambda p: sd_sphere(p, 0.13), 5, pos=(0, -0.5, 0)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.0)


def _token(base, glow, shape, t):
    """A small part of a chain: a shaped token in the field's colours."""
    mats = [Material(base, '#ffffff', pattern=shade_noise(6, 0.12, 4), metal=0.7), Material(glow, '#ffffff', emissive=glow, pattern=em_all(), metal=0.3)]
    if shape == 'gem':
        parts = [Part(lambda p: sd_gem(p, 0.3, 0.7), 0, rot=rotz(0.4)), Part(lambda p: sd_sphere(p, 0.1), 1, pos=(0, 0.1, -0.25))]
    elif shape == 'ring':
        parts = [Part(lambda p: sd_torus(p, 0.42, 0.1), 0, rot=rotx(1.2)), Part(lambda p: sd_torus(p, 0.42, 0.04), 1, rot=rotx(1.2), pos=(0, 0, -0.05))]
    elif shape == 'slab':
        parts = [Part(lambda p: sd_box(p, (0.5, 0.36, 0.08), 0.04), 0, rot=rotx(-0.3) @ roty(0.3)),
                 Part(lambda p: sd_box(p, (0.3, 0.05, 0.02)), 1, pos=(0, 0.1, -0.1), rot=rotx(-0.3) @ roty(0.3)),
                 Part(lambda p: sd_box(p, (0.3, 0.05, 0.02)), 1, pos=(0, -0.1, -0.1), rot=rotx(-0.3) @ roty(0.3))]
    elif shape == 'vial':
        parts = [Part(lambda p: sd_capsule(p, V(0, 0.15, 0), V(0, 0.45, 0), 0.2), 0), Part(lambda p: sd_capsule(p, V(0, -0.45, 0), V(0, 0.1, 0), 0.25), 1)]
    elif shape == 'blob':
        parts = [Part(lambda p: sd_ellipsoid(p, (0.45, 0.32, 0.36)), 0), Part(lambda p: sd_ellipsoid(p, (0.2, 0.12, 0.1)), 1, pos=(0.1, 0.12, -0.3))]
    else:   # cylinder
        parts = [Part(lambda p: sd_cyl(p, 0.3, 0.42), 0, rot=rotz(0.4)), Part(lambda p: sd_cyl(p, 0.31, 0.06), 1, rot=rotz(0.4))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.45, zoom=1.1)


TOKENS = {'charged_lattice_seed': ('#8070c0', '#c0a0ff', 'gem'), 'coherent_field_beam': ('#606a80', '#80c0ff', 'ring'),
          'doped_catalyst_bed': ('#8a6040', '#ffb060', 'slab'), 'framework_catalyst': ('#a0a8b0', '#60ff90', 'slab'),
          'chelated_catalyst': ('#407060', '#80ffd0', 'gem'), 'verified_lemma': ('#e8e4d8', '#4080ff', 'slab'),
          'certified_theorem': ('#e8e4d8', '#60d060', 'slab'), 'genome_library': ('#c8d8c8', '#60e0a0', 'vial'),
          'reconstructed_genome': ('#d0c0a0', '#e0c060', 'gem'), 'cultured_cortex': ('#e0a0b8', '#ffd0e0', 'blob'),
          'trained_cortex': ('#d080a0', '#a0e0ff', 'blob'), 'mapped_mind': ('#c06090', '#ffffff', 'blob'),
          'survey_probe': ('#d0d4dc', '#ff8040', 'cyl'), 'orbital_survey': ('#d0d4dc', '#80c0ff', 'slab'),
          'stratigraphic_timeline': ('#8a6a48', '#ffd080', 'cyl')}
for _iid, (_b, _g, _s) in TOKENS.items():
    reg(_iid, (('rock',), ('glint',)))(lambda c1, c2, t, b=_b, g=_g, s=_s: _token(b, g, s, t))


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
