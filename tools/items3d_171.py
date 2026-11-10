"""
1.7.1 item art (the sixteen signature materials): small 3D models, lit and animated like every other item of the mod
(helpers from items3d_17.py; the model faces the camera on its -z side).

Run:  python3 tools/items3d_171.py            (writes the textures)
      python3 tools/items3d_171.py preview <png>
"""
import math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items3d_13 as P13
import items3d_17 as P17
from render3d import Part, Material, sd_sphere, sd_box, sd_torus, sd_cyl, sd_capsule, sd_gem, rotx, roty, rotz
from items3d_new import V, sd_ellipsoid, em_all, shade_noise

TAU = math.tau
B = {}
MOTIONS = {}


def reg(iid, motions=(('rock',), ('glint',))):
    def deco(fn):
        B[iid] = fn
        MOTIONS[iid] = motions
        return fn
    return deco


def M(base, glow, metal=0.8):
    return [Material(base, '#ffffff', pattern=shade_noise(6, 0.12, 4), metal=metal), Material(glow, '#ffffff', emissive=glow, pattern=em_all(), metal=0.3)]


def ingot(base, glow, extra=()):
    """A bar, seen three-quarter on, with something of its own set in the top face."""
    R = rotx(-0.45) @ roty(0.55)
    return [Part(lambda p: sd_box(p, (0.52, 0.16, 0.26), 0.05), 0, rot=R)] + list(extra), M(base, glow), dict(view_pitch=0.25, view_yaw=0.0, zoom=1.1)


@reg('dyson_lattice_panel')
def b_panel(c1, c2, t):
    """A hexagon-ish collector panel, its cells lighting in turn."""
    R = rotx(-0.5) @ roty(0.4)
    parts = [Part(lambda p: sd_box(p, (0.5, 0.5, 0.05), 0.03), 0, rot=R)]
    for i in range(3):
        for j in range(3):
            if (i + j + int(t * 9)) % 3 == 0:
                parts.append(Part(lambda p: sd_box(p, (0.11, 0.11, 0.02)), 1, rot=R, pos=tuple(R @ np.array([(i - 1) * 0.3, (j - 1) * 0.3, -0.07]))))
    return parts, M('#303a60', '#ffc860'), dict(view_pitch=0.2, view_yaw=0.0, zoom=1.1)


@reg('stellar_wind_condensate')
def b_wind(c1, c2, t):
    k = 0.03 * math.sin(TAU * t)
    return ingot('#8a5a30', '#ffb050', [Part(lambda p: sd_sphere(p, 0.09 + k), 1, pos=(-0.15, 0.12, -0.3)), Part(lambda p: sd_sphere(p, 0.06), 1, pos=(0.2, 0.05, -0.33))])


@reg('recursive_thought_shell', (('spin',), ('glint',)))
def b_shell(c1, c2, t):
    """Rings inside rings, each tilted against the last, round a bright point."""
    parts = [Part(lambda p: sd_torus(p, 0.55, 0.06), 0, rot=rotx(1.2)),
             Part(lambda p: sd_torus(p, 0.38, 0.055), 0, rot=rotx(0.4) @ roty(0.9)),
             Part(lambda p: sd_torus(p, 0.22, 0.05), 0, rot=rotx(-0.7) @ roty(-0.5)),
             Part(lambda p: sd_sphere(p, 0.1), 1)]
    return parts, M('#50c890', '#c0ffe0'), dict(view_pitch=0.25, view_yaw=0.3, zoom=1.1)


@reg('embryonic_vacuum')
def b_vac(c1, c2, t):
    """A dark bead with a seam of first light round it."""
    k = 0.02 * math.sin(TAU * t)
    parts = [Part(lambda p: sd_sphere(p, 0.42), 0), Part(lambda p: sd_torus(p, 0.44 + k, 0.035), 1, rot=rotx(1.25) @ rotz(0.3)),
             Part(lambda p: sd_sphere(p, 0.07), 1, pos=(-0.14, 0.16, -0.36))]
    return parts, M('#181830', '#ffe8a0', metal=0.9), dict(view_pitch=0.25, view_yaw=0.3, zoom=1.1)


@reg('compiled_law')
def b_law(c1, c2, t):
    """A tablet with lines of code lit one after another."""
    R = rotx(-0.3) @ roty(0.3)
    parts = [Part(lambda p: sd_box(p, (0.4, 0.55, 0.07), 0.04), 0, rot=R)]
    for i in range(5):
        w = 0.28 if (i + int(t * 5)) % 5 else 0.3
        on = (i == int(t * 5) % 5)
        parts.append(Part(lambda p, w=w: sd_box(p, (w * (0.6 + 0.4 * ((i * 7) % 3) / 2), 0.035, 0.02)), 1 if on or i % 2 == 0 else 0, rot=R,
                          pos=tuple(R @ np.array([-0.03, 0.36 - i * 0.18, -0.09]))))
    return parts, M('#285838', '#70ff9a'), dict(view_pitch=0.2, view_yaw=0.0, zoom=1.1)


@reg('lex_prima')
def b_lex(c1, c2, t):
    """A scroll: two rods and the sheet between."""
    R = rotz(0.25)
    parts = [Part(lambda p: sd_cyl(p, 0.09, 0.55), 0, rot=R @ rotz(math.pi / 2), pos=(0, 0.38, 0)),
             Part(lambda p: sd_cyl(p, 0.09, 0.55), 0, rot=R @ rotz(math.pi / 2), pos=(0, -0.38, 0)),
             Part(lambda p: sd_box(p, (0.42, 0.34, 0.02)), 1, rot=R)]
    return parts, M('#3a9088', '#b0fff0'), dict(view_pitch=0.2, view_yaw=0.3, zoom=1.1)


@reg('breach_shard')
def b_breach(c1, c2, t):
    parts = [Part(lambda p: sd_gem(p, 0.26, 0.85), 0, rot=rotz(0.5)), Part(lambda p: sd_box(p, (0.03, 0.6, 0.03)), 1, rot=rotz(0.5), pos=(0.02, 0, -0.24))]
    return parts, M('#b8a0e0', '#fff0c0'), dict(view_pitch=0.25, view_yaw=0.4, zoom=1.1)


@reg('pure_concept_ingot')
def b_concept(c1, c2, t):
    return ingot('#e0e0f0', '#ffffff', [Part(lambda p: sd_torus(p, 0.12, 0.03), 1, rot=rotx(1.1), pos=(0, 0.12, -0.3))])


@reg('diagonal_ingot')
def b_diag(c1, c2, t):
    R = rotx(-0.45) @ roty(0.55)
    ex = [Part(lambda p: sd_sphere(p, 0.05), 1, pos=tuple(R @ np.array([-0.36 + i * 0.18, 0.19, -0.16 + i * 0.08]))) for i in range(5)]
    return ingot('#8878c8', '#e0d0ff', ex)


@reg('distilled_axiom')
def b_axiom(c1, c2, t):
    parts = [Part(lambda p: sd_capsule(p, V(0, 0.2, 0), V(0, 0.5, 0), 0.13), 0), Part(lambda p: sd_sphere(p, 0.36), 0, pos=(0, -0.15, 0)),
             Part(lambda p: sd_sphere(p, 0.24 + 0.02 * math.sin(TAU * t)), 1, pos=(0, -0.18, -0.16))]
    return parts, M('#c8c0e0', '#a060ff', metal=0.3), dict(view_pitch=0.2, view_yaw=0.3, zoom=1.1)


@reg('transcendent_alloy')
def b_talloy(c1, c2, t):
    return ingot('#d04878', '#ffd0e0', [Part(lambda p: sd_gem(p, 0.09, 0.2), 1, pos=(0.0, 0.15, -0.3))])


@reg('event_horizon_lens', (('spin',), ('glint',)))
def b_lens(c1, c2, t):
    parts = [Part(lambda p: sd_torus(p, 0.46, 0.08), 0, rot=rotx(1.35)), Part(lambda p: sd_ellipsoid(p, (0.4, 0.4, 0.08)), 2), Part(lambda p: sd_torus(p, 0.3, 0.03), 1, rot=rotx(1.35), pos=(0, 0, -0.07))]
    return parts, M('#c08030', '#ffc060') + [Material('#08080c', '#404050', metal=0.9)], dict(view_pitch=0.2, view_yaw=0.35, zoom=1.1)


@reg('pleromatic_weave')
def b_weave(c1, c2, t):
    R = rotx(-0.35) @ roty(0.35)
    parts = [Part(lambda p: sd_box(p, (0.5, 0.5, 0.04), 0.02), 0, rot=R)]
    for i in range(4):
        parts.append(Part(lambda p: sd_box(p, (0.5, 0.035, 0.02)), 1, rot=R, pos=tuple(R @ np.array([0, -0.33 + i * 0.22, -0.06]))))
        parts.append(Part(lambda p: sd_box(p, (0.035, 0.5, 0.02)), 1, rot=R, pos=tuple(R @ np.array([-0.33 + i * 0.22, 0, -0.07]))))
    return parts, M('#e8e0d0', '#fff8e0', metal=0.3), dict(view_pitch=0.2, view_yaw=0.0, zoom=1.1)


@reg('confluent_essence', (('spin',), ('glint',)))
def b_ess(c1, c2, t):
    """Five drops round a white one."""
    mats = [Material(c, '#ffffff', emissive=c, pattern=em_all(), metal=0.3) for c in ('#ffd070', '#70e0a0', '#80a0ff', '#ff90b0', '#c0f0ff', '#ffffff')]
    parts = [Part(lambda p: sd_sphere(p, 0.2), 5)]
    for i in range(5):
        a = i * TAU / 5 + t * TAU / 5
        parts.append(Part(lambda p: sd_sphere(p, 0.13), i, pos=(math.cos(a) * 0.42, math.sin(a) * 0.42, 0)))
    return parts, mats, dict(view_pitch=0.0, view_yaw=0.0, zoom=1.1)


@reg('eschaton_steel')
def b_esteel(c1, c2, t):
    return ingot('#383038', '#ffd0a0', [Part(lambda p: sd_box(p, (0.4, 0.02, 0.02)), 1, rot=rotx(-0.45) @ roty(0.55), pos=(0.0, 0.14, -0.26))])


@reg('convergent_matter', (('spin',), ('glint',)))
def b_conv(c1, c2, t):
    """Thirteen points on a ring, drawn in to one."""
    k = 0.5 + 0.5 * math.sin(TAU * t)
    parts = [Part(lambda p: sd_sphere(p, 0.2), 1)]
    for i in range(13):
        a = i * TAU / 13
        r = 0.5 - 0.12 * k
        parts.append(Part(lambda p: sd_sphere(p, 0.07), 0, pos=(math.cos(a) * r, math.sin(a) * r, 0)))
    return parts, M('#9070d0', '#e0c8ff'), dict(view_pitch=0.0, view_yaw=0.0, zoom=1.1)


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
        cols, S = 8, 100
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
