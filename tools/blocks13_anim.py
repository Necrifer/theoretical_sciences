"""
1.3.0 art pass for blocks: the casings, parts and consoles of 1.3.0 were drawn as still pictures, where the mod's own
blocks move (casings catch a passing sheen, lit parts pulse, formed casings carry the energy conduits that join a
whole structure into one surface, running consoles flicker with their readouts). This keeps each 1.3.0 drawing and
animates it the mod's way. 16 frames, two ticks each.

Also redraws Amber Ore in the style of every other ore of the mod (its mineral set into its host rock, catching the
light in turn) in place of the plus-shaped marks it had.
"""
import json, math, os, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/textures/block')
N = 16
TAU = math.tau

CASINGS = ['amniotic_tank', 'cold_plate', 'convection_wall', 'flow_straightener', 'funnel_coil', 'grow_light', 'grow_tray', 'heater_plate',
           'hydroponic_frame', 'ice_drill_casing', 'ion_collector', 'launch_pad', 'life_support_pump', 'nutrient_mast', 'ramscoop_frame',
           'rocket_engine', 'rocket_hull', 'service_tower', 'storm_electrode', 'turning_vane', 'vat_frame', 'wind_tunnel_casing',
           'wind_tunnel_fan']
SINGLES = ['orbital_transfer_console', 'solar_array']
MACHINES = ['ancient_dna_lab', 'breeding_bench', 'bussard_ramscoop', 'dating_spectrometer', 'geothermal_plant', 'hydroponic_tower', 'ice_core_drill',
            'phytotron', 'revival_vat', 'rocket_assembly', 'storm_chamber', 'wave_energy_converter', 'wind_tunnel']
# parts whose lit pixels pulse, and the speed of the pulse (beats per loop)
GLOW = {'grow_light': 1, 'heater_plate': 1, 'storm_electrode': 4, 'ion_collector': 2, 'funnel_coil': 2, 'amniotic_tank': 1, 'life_support_pump': 4,
        'rocket_engine': 3, 'nutrient_mast': 2, 'orbital_transfer_console': 2, 'laminar_flow_conduit_in': 2, 'laminar_flow_conduit_out': 2}


def load(path):
    im = np.asarray(Image.open(path).convert('RGBA'), float) / 255
    return im[:im.shape[1]]                                       # first frame if it already had several


def lit_mask(img):
    rgb = img[..., :3]
    mx, mn = rgb.max(-1), rgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1e-3)
    return np.clip((mx - 0.55) * 3, 0, 1) * np.clip((sat - 0.25) * 3, 0, 1)


def sheen(img, t, strength=0.22, width=0.08):
    n = img.shape[0]
    y, x = np.mgrid[0:n, 0:n] / n
    d = (x + y) / 2 - (t * 1.6 - 0.3)
    s = np.exp(-(d / width) ** 2) * strength
    out = img.copy()
    out[..., :3] = np.clip(out[..., :3] + s[..., None] * (1 - out[..., :3]), 0, 1)
    return out


def pulse(img, t, beats=1, depth=0.35):
    m = lit_mask(img)[..., None]
    k = 1 - depth + depth * (0.5 + 0.5 * math.cos(TAU * t * beats))
    out = img.copy()
    out[..., :3] = np.clip(img[..., :3] * (1 - m) + img[..., :3] * m * (0.65 + 0.55 * k), 0, 1)
    return out


def conduits(img, t):
    """The formed look shared by every multiblock casing of the mod: a pulse running along the centre lines, edge to edge,
    a whole tile per loop, so across a structure the conduits join into one flowing network."""
    out = img.copy()
    n = img.shape[0]
    rgb = img[..., :3]
    m = lit_mask(img)
    glow = rgb[m > 0.3].mean(0) if (m > 0.3).sum() >= 3 else np.clip(rgb.mean((0, 1)) * 1.6 + 0.15, 0, 1)
    glow = np.clip(glow * 1.1 + 0.08, 0, 1)
    mid = n // 2
    for i in range(n):
        p = 0.35 + 0.65 * max(0.0, math.cos(TAU * (i / n - t))) ** 6
        for (y, x) in ((mid, i), (mid - 1, i), (i, mid), (i, mid - 1)):
            a = p * 0.8
            out[y, x, :3] = out[y, x, :3] * (1 - a) + glow * a
    return out


def readout(img, t):
    """A running console: its lit display pixels flicker and a scan line runs down the screen."""
    m = lit_mask(img)
    out = img.copy()
    n = img.shape[0]
    row = int(t * n) % n
    flick = 0.85 + 0.15 * math.sin(TAU * t * 4)
    out[..., :3] = np.clip(img[..., :3] * (1 - m[..., None]) + img[..., :3] * m[..., None] * flick, 0, 1)
    band = np.zeros((n, n)); band[row] = 1; band[(row + 1) % n] = 0.4
    out[..., :3] = np.clip(out[..., :3] + (band * m)[..., None] * 0.35, 0, 1)
    return out


def write(frames, path):
    strip = np.concatenate(frames, 0)
    Image.fromarray((np.clip(strip, 0, 1) * 255).astype(np.uint8), 'RGBA').save(path)
    json.dump({'animation': {'frametime': 2, 'interpolate': False}}, open(path + '.mcmeta', 'w'))


def animate(name, fn):
    p = os.path.join(TEX, name + '.png')
    if not os.path.exists(p):
        print('missing', name); return
    if os.path.exists(p + '.mcmeta'):
        return                                                  # already animated by its author: kept as it is
    base = load(p)
    write([fn(base, f / N) for f in range(N)], p)


def amber_ore():
    sys.path.insert(0, HERE)
    import ore_textures
    ore_textures.TEX = TEX
    ore_textures.make('amber_ore', 'deepslate', '#a8500c', '#ffc040')


if __name__ == '__main__':
    for c in CASINGS:
        g = GLOW.get(c)
        animate(c, (lambda b, t, g=g: pulse(sheen(b, t), t, g)) if g else (lambda b, t: sheen(b, t)))
        animate(c + '_formed', lambda b, t, g=g: conduits(pulse(b, t, g) if g else b, t))
    for s in SINGLES:
        g = GLOW.get(s)
        animate(s, (lambda b, t, g=g: pulse(sheen(b, t, 0.15), t, g)) if g else (lambda b, t: sheen(b, t, 0.3, 0.06)))
    for k in ('laminar_flow_conduit_in', 'laminar_flow_conduit_out'):
        animate(k, lambda b, t, k=k: pulse(b, t, GLOW[k], 0.5))
    animate('laminar_flow_conduit_side', lambda b, t: sheen(b, t, 0.18))
    # machine faces, sides and tops are drawn and animated by tools/machine_art.py
    amber_ore()
    print('animated', len(CASINGS) * 2 + len(SINGLES) + 3 + len(MACHINES) * 3, 'block textures; amber ore redrawn')
