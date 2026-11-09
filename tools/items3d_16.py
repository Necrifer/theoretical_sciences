"""
1.6.0 item art (Archaeology): small 3D models, lit and animated like every other item of the mod (helpers from
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



@reg('ancient_potsherd', (('rock',), ()))
def b_sherd(c1, c2, t):
    """A curved piece of a fired pot, burnished red-brown outside with a painted band, the broken edges paler."""
    mats = [Material('#b0603a', '#e8a070', pattern=stripes(14, 1, 0.18, 0.35), metal=0.1),
            Material('#d8a880', '#f0d0b0', pattern=mottle(11, 0.15, 2), metal=0.05)]
    shell = lambda p: np.maximum(np.maximum(np.abs(sd_sphere(p, 0.62)) - 0.06, sd_box(p - V(0, 0, -0.5), (0.42, 0.34, 0.3))),
                                 -sd_box(p - V(0.32, 0.28, -0.5), (0.2, 0.18, 0.4)))
    parts = [Part(shell, 0, rot=rotz(0.3)), Part(lambda p: np.maximum(np.abs(sd_sphere(p, 0.6)) - 0.02, sd_box(p - V(0, 0, -0.4), (0.38, 0.3, 0.1))), 1, rot=rotz(0.3))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.35, zoom=1.15)


@reg('burnt_flint', (('rock',), ('glint',)))
def b_flint(c1, c2, t):
    """A flint blade crazed and reddened by a hearth's heat."""
    mats = [Material('#6a4a48', '#c08070', pattern=mottle(16, 0.3, 4), metal=0.4), Material('#a04030', '#ff9060', pattern=mottle(9, 0.25, 7), metal=0.3)]
    parts = [Part(lambda p: sd_gem(p, 0.2, 0.7, 0.2), 0, rot=rotz(0.7) @ rotx(0.3)),
             Part(lambda p: sd_ellipsoid(p, (0.12, 0.2, 0.07)), 1, pos=(0.08, -0.05, -0.12), rot=rotz(0.7))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.4, zoom=1.15)


@reg('corroded_artefact', (('rock',), ()))
def b_corroded(c1, c2, t):
    """The rusted shape of a blade, blistered with corrosion, a glint of metal where a blister broke."""
    mats = [Material('#8a4a22', '#d08848', pattern=mottle(14, 0.4, 3), metal=0.2), Material('#a8aab0', '#ffffff', metal=1.0),
            Material('#5a3a20', '#9a6a40', pattern=mottle(20, 0.3, 9), metal=0.2)]
    parts = [Part(lambda p: sd_box(p, (0.62, 0.19, 0.06), 0.04), 0, rot=rotz(0.75)),
             Part(lambda p: sd_box(p, (0.16, 0.08, 0.07), 0.02), 2, pos=(-0.45, -0.42, 0), rot=rotz(0.75))]
    for k, (x, y) in enumerate(((0.1, 0.12), (-0.15, -0.1), (0.3, 0.32), (-0.05, 0.02))):
        parts.append(Part(lambda p: sd_sphere(p, 0.07), 0, pos=(x, y, -0.04)))
    parts.append(Part(lambda p: sd_sphere(p, 0.045), 1, pos=(0.18, 0.2, -0.07)))
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.05)


@reg('charred_beam', (('rock',), ()))
def b_beam(c1, c2, t):
    """A charred end of timber, its cut face showing the rings, the outer wood cracked into black blocks."""
    ring = lambda p, n: (np.where(np.sin(np.hypot(p[..., 0], p[..., 1]) * 60) > 0.4, -0.3, 0.05), np.zeros(p.shape[:-1]))
    mats = [Material('#1c1614', '#4a3a30', pattern=weave(26, 0.2), metal=0.1, keep_dark=True), Material('#8a5a34', '#c89060', pattern=ring, metal=0.05)]
    parts = [Part(lambda p: sd_cyl(p, 0.38, 0.5), 0, rot=rotx(math.pi / 2)),
             Part(lambda p: sd_cyl(p, 0.33, 0.02), 1, pos=(0, 0, -0.51), rot=rotx(math.pi / 2))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.55, zoom=1.0)


@reg('slag_cake', (('rock',), ('glint',)))
def b_slag(c1, c2, t):
    """A heavy bun of run iron slag, glassy black and rusty, pocked with bubbles."""
    mats = [Material('#2a2828', '#7a6a60', pattern=mottle(18, 0.35, 5), metal=0.7, keep_dark=True), Material('#8a4a22', '#c07040', pattern=mottle(12, 0.3, 1), metal=0.2)]
    parts = [Part(lambda p: sd_ellipsoid(p, (0.6, 0.32, 0.55)), 0, rot=rotx(0.4)),
             Part(lambda p: sd_ellipsoid(p, (0.3, 0.14, 0.28)), 1, pos=(0.2, 0.15, -0.22), rot=rotx(0.4))]
    for k, (x, y) in enumerate(((-0.2, 0.1), (0.05, -0.05), (0.3, -0.12))):
        parts.append(Part(lambda p: -sd_sphere(p, 0.06) + 0.0 * k, 0, pos=(x, y, -0.6)))
    parts = parts[:2]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.4, zoom=1.1)


@reg('dated_potsherd', (('rock',), ('glint',)))
def b_dated(c1, c2, t):
    """A sherd with a sample core drilled from it, a paper date tag tied to it, the grains glowing blue."""
    k = 0.6 + 0.4 * math.sin(TAU * t)
    mats = [Material('#b0603a', '#e8a070', pattern=stripes(14, 1, 0.18, 0.35), metal=0.1), Material('#f0ead8', '#ffffff', metal=0.05),
            Material('#6090ff', '#ffffff', emissive=tuple(np.array([0.4, 0.6, 1.0]) * k), pattern=em_all(), metal=0.3)]
    shell = lambda p: np.maximum(np.abs(sd_sphere(p, 0.6)) - 0.06, sd_box(p - V(0, 0, -0.5), (0.4, 0.32, 0.3)))
    parts = [Part(shell, 0, rot=rotz(0.3)), Part(lambda p: sd_box(p, (0.17, 0.11, 0.015)), 1, pos=(0.3, -0.35, -0.6), rot=rotz(-0.3)),
             Part(lambda p: sd_sphere(p, 0.07), 2, pos=(-0.1, 0.05, -0.62))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.35, zoom=1.1)


@reg('tree_ring_chronology', (('rock',), ('glint',)))
def b_chrono(c1, c2, t):
    """A polished disc of wood, its rings sharp, pinned with a brass marker at the dated ring."""
    ring = lambda p, n: (np.where(np.sin(np.hypot(p[..., 0], p[..., 1]) * 70) > 0.5, -0.32, 0.08), np.zeros(p.shape[:-1]))
    mats = [Material('#c89a60', '#f0d0a0', pattern=ring, metal=0.15), Material('#e0b840', '#fff0a0', metal=1.0), Material('#5a3a20', '#8a6040', metal=0.1)]
    parts = [Part(lambda p: sd_cyl(p, 0.62, 0.06), 0, rot=rotx(math.pi / 2 + 0.3)),
             Part(lambda p: sd_cyl(p, 0.64, 0.05), 2, pos=(0, 0, 0.03), rot=rotx(math.pi / 2 + 0.3)),
             Part(lambda p: sd_sphere(p, 0.07), 1, pos=(0.33, 0.2, -0.12))]
    return parts, mats, dict(view_pitch=0.2, view_yaw=0.3, zoom=1.05)


@reg('stabilised_artefact', (('rock',), ('glint',)))
def b_stab(c1, c2, t):
    """The conserved blade: rust cleaned back to a dark, waxed surface, a numbered label tied to it."""
    mats = [Material('#4a3a30', '#a08070', pattern=mottle(12, 0.15, 3), metal=0.6, gloss=1.4), Material('#f0ead8', '#ffffff', metal=0.05),
            Material('#5a3a20', '#9a6a40', metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.62, 0.19, 0.055), 0.04), 0, rot=rotz(0.75)),
             Part(lambda p: sd_box(p, (0.16, 0.08, 0.07), 0.02), 2, pos=(-0.45, -0.42, 0), rot=rotz(0.75)),
             Part(lambda p: sd_box(p, (0.15, 0.1, 0.012)), 1, pos=(0.32, -0.1, -0.08), rot=rotz(-0.2))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.05)


@reg('provenanced_artefact', (('rock',), ('glint',)))
def b_prov(c1, c2, t):
    """The conserved blade on a card stand, a violet isotope signature glowing on its tag."""
    k = 0.6 + 0.4 * math.sin(TAU * t)
    mats = [Material('#4a3a30', '#a08070', pattern=mottle(12, 0.15, 3), metal=0.6, gloss=1.4), Material('#f0ead8', '#ffffff', metal=0.05),
            Material('#d080f0', '#ffffff', emissive=tuple(np.array([0.8, 0.5, 0.95]) * k), pattern=em_all(), metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.58, 0.18, 0.055), 0.04), 0, rot=rotz(0.75)),
             Part(lambda p: sd_box(p, (0.22, 0.14, 0.012)), 1, pos=(0.3, -0.25, -0.08), rot=rotz(-0.2)),
             Part(lambda p: sd_box(p, (0.14, 0.03, 0.01)), 2, pos=(0.3, -0.25, -0.1), rot=rotz(-0.2))]
    return parts, mats, dict(view_pitch=0.25, view_yaw=0.3, zoom=1.05)


@reg('bloomery_steel_ingot', (('rock',), ('glint',)))
def b_bsteel(c1, c2, t):
    """A forged bar showing the wavy grain of its folds."""
    grain = lambda p, n: (np.sin(p[..., 0] * 30 + np.sin(p[..., 2] * 14) * 2.5) * 0.18, np.zeros(p.shape[:-1]))
    mats = [Material('#7a7e86', '#e0e4ea', pattern=grain, metal=1.0)]
    parts = [Part(lambda p: sd_box(p, (0.66, 0.2, 0.3), 0.06), 0, rot=rotx(0.5) @ roty(0.5))]
    return parts, mats, dict(view_pitch=0.35, view_yaw=0.5, zoom=1.2)


@reg('excavation_record', (('rock',), ('glint',)))
def b_record(c1, c2, t):
    """A cut block of the dig, its layers in bands of soil and ash, the finds glowing where they lay, on a brass base."""
    mats = [Material('#8a6a48', '#c0a070', pattern=mottle(14, 0.15, 2), metal=0.1), Material('#5a5a5a', '#a0a0a0', pattern=mottle(18, 0.2, 5), metal=0.1),
            Material('#fff0c0', '#ffffff', emissive=tuple(np.array([1.0, 0.85, 0.5]) * (0.6 + 0.4 * math.sin(TAU * t))), pattern=em_all(), metal=0.3),
            Material('#d0a040', '#fff0a0', metal=1.0)]
    parts = [Part(lambda p: sd_box(p, (0.5, 0.06, 0.5), 0.02), 3, pos=(0, -0.5, 0))]
    for k in range(4):
        parts.append(Part(lambda p: sd_box(p, (0.44, 0.1, 0.44), 0.01), 0 if k % 2 == 0 else 1, pos=(0, -0.33 + k * 0.21, 0)))
    for (x, y) in ((0.18, -0.3), (-0.22, -0.08), (0.06, 0.15), (-0.1, 0.3)):
        parts.append(Part(lambda p: sd_sphere(p, 0.07), 2, pos=(x, y, -0.43)))
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.55, zoom=1.0)


@reg('survey_radar', (('rock',), ('glint',)))
def b_radar(c1, c2, t):
    """A radar sled on a handle, a little screen on the handle showing the echo bands."""
    k = 0.5 + 0.5 * math.sin(TAU * t * 2)
    mats = [Material('#d8c020', '#fff080', metal=0.4), Material('#30343a', '#70747a', metal=0.6),
            Material('#70f0a0', '#ffffff', emissive=tuple(np.array([0.4, 1.0, 0.6]) * (0.4 + 0.6 * k)), pattern=em_bands(14, t, 1, 0.4), metal=0.3)]
    parts = [Part(lambda p: sd_box(p, (0.45, 0.12, 0.3), 0.05), 0, pos=(0.1, -0.42, 0)),
             Part(lambda p: sd_capsule(p, V(0, 0, 0), V(-0.45, 0.75, 0), 0.04), 1, pos=(0.0, -0.3, 0)),
             Part(lambda p: sd_box(p, (0.14, 0.11, 0.04), 0.02), 1, pos=(-0.28, 0.22, -0.03)),
             Part(lambda p: sd_box(p, (0.11, 0.08, 0.01)), 2, pos=(-0.28, 0.22, -0.075))]
    return parts, mats, dict(view_pitch=0.3, view_yaw=0.45, zoom=1.0)


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
