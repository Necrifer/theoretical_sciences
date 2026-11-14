"""
1.11.0 (Patch B): more places in four single-biome dimensions. Two biomes each, every one with its own ground block,
its own features and its own weather of creatures, from the real physics of the place:

  Antiverse            Annihilation Front     where stray matter met antimatter: ground fused to glass by 511 keV gammas
                       Antihydrogen Glacier   firn of frozen antihydrogen, pools of liquid antihelium
  Quantum Foam         Spin Network           space as loop quantum gravity draws it: a graph of nodes and links
                       False Vacuum Shelf     metastable crust about pools of false vacuum
  Black Hole Interior  Spaghettified Reaches  rock drawn out by the tides, pools of tidal plasma
                       Mass Inflation Zone    the inner (Cauchy) horizon, where infalling light piles up blueshifted
  White Hole Interior  Outflow Spires         ejecta driven outward in columns
                       Reheating Fields       a crust like the young universe's, over reheating plasma

Run: python3 tools/round1110_biomes.py   (blocks, textures, Java lists, biomes, worldgen, text, Codex)
"""
import json, math, os, random, re, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T
from PIL import Image

L = T.LANG
TEX = os.path.join(A, 'textures/block')
JAVA = os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences/registry')
FRAMES = 16


# ====================================================================== textures (16 frames, 16 by 16, like the dimensions' own blocks)
def tex(name, fn):
    im = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        for y in range(16):
            for x in range(16):
                c = fn(x, y, f / FRAMES)
                im.putpixel((x, y + 16 * f), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    im.save(os.path.join(TEX, name + '.png'))
    json.dump({'animation': {'frametime': 3, 'interpolate': False}}, open(os.path.join(TEX, name + '.png.mcmeta'), 'w'))


def noise(seed):
    r = random.Random(seed)
    return [[r.random() for _ in range(16)] for _ in range(16)]


def mix(a, b, t):
    return [a[i] + (b[i] - a[i]) * t for i in range(3)]


N1, N2, N3 = noise(11), noise(12), noise(13)


def annihilation_scar(x, y, t):                       # fused dark glass over antistone, with flashes that come and go
    base = mix((52, 30, 26), (24, 18, 22), N1[y][x])
    if N2[y][x] > 0.55:
        base = mix(base, (70, 60, 78), 0.6)               # glassy sheen
    ph = (N3[y][x] * 7 + t) % 1.0
    if N3[y][x] > 0.86 and ph < 0.25:
        return mix(base, (250, 236, 255), 1 - ph * 4)     # a 511 keV flash
    return base


def antihydrogen_firn(x, y, t):                       # granular pale ice with a slow cold glitter
    base = mix((206, 228, 240), (168, 200, 222), N1[y][x])
    if N2[y][x] > 0.8:
        g = 0.5 + 0.5 * math.sin((t + N3[y][x]) * math.tau)
        base = mix(base, (240, 252, 255), g)
    if (x * 7 + y * 3) % 11 == 0:
        base = mix(base, (120, 170, 210), 0.5)
    return base


def spin_foam_mesh(x, y, t):                          # a graph: bright nodes joined by links that pulse
    base = mix((14, 26, 52), (22, 40, 74), N1[y][x])
    nodes = [(2, 3), (11, 2), (6, 8), (13, 10), (3, 13), (9, 14)]
    links = [(0, 2), (1, 2), (2, 3), (2, 4), (3, 5), (4, 5), (1, 3), (0, 4)]
    for i, (a, b) in enumerate(links):
        (x0, y0), (x1, y1) = nodes[a], nodes[b]
        for s in range(24):
            u = s / 23
            if round(x0 + (x1 - x0) * u) == x and round(y0 + (y1 - y0) * u) == y:
                pulse = 0.5 + 0.5 * math.sin((t * 2 - u + i * 0.3) * math.tau)
                return mix((60, 130, 200), (170, 230, 255), pulse)
    for nx, ny in nodes:
        if abs(nx - x) <= 0 and abs(ny - y) <= 0:
            return (240, 250, 255)
        if abs(nx - x) + abs(ny - y) == 1:
            return (150, 210, 255)
    return base


def _cracks():
    r, pts = random.Random(5), {}
    for start, steps in (((0, 4), 22), ((15, 11), 20), ((7, 0), 18)):
        x, y = start
        dx = 1 if x < 8 else -1
        for i in range(steps):
            pts[(x % 16, y % 16)] = i
            if r.random() < 0.55:
                x += dx
            else:
                y += 1 if r.random() < 0.6 else -1
            if r.random() < 0.12:                          # a branch
                bx, by = x, y
                for j in range(5):
                    by += 1
                    bx += r.choice((-1, 0, 1))
                    pts.setdefault((bx % 16, by % 16), i + j)
    return pts


CRACKS = _cracks()


def metastable_crust(x, y, t):                        # dark violet crust; its cracks brighten and fade, never quite letting go
    base = mix((40, 22, 52), (62, 34, 78), N1[y][x])
    if (x, y) in CRACKS:
        g = 0.3 + 0.7 * (0.5 + 0.5 * math.sin((t - CRACKS[(x, y)] / 40) * math.tau))
        return mix((90, 40, 110), (240, 110, 250), g)
    near = any((x + a, y + b) in CRACKS for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    return mix(base, (100, 50, 120), 0.35) if near else base


def stretched_rock(x, y, t):                          # infall rock drawn into long streaks that slide down
    yy = (y + t * 16) % 16
    col = N1[0][x]
    base = mix((44, 16, 20), (28, 10, 14), col)
    band = math.sin((yy / 16 + col * 2) * math.tau * 0.5)
    base = mix(base, (90, 36, 34), max(0, band) * 0.5 * (N2[x][x] > 0.5))
    if N3[0][x] > 0.82:
        base = mix(base, (190, 70, 40), 0.5 + 0.3 * math.sin((yy / 16) * math.tau))
    return base


def blueshift_crust(x, y, t):                         # deep navy under bands of blue-white light that crowd and brighten
    base = mix((10, 14, 40), (18, 24, 62), N1[y][x])
    v = (y + x * 0.3 + t * 16) % 16
    band = max(0.0, math.cos(v / 16 * math.tau * 2))
    g = band ** 6
    return mix(base, (200, 226, 255), g * (0.6 + 0.4 * N2[y][x]))


def outflow_rock(x, y, t):                            # pale ejecta with streaks running upward
    yy = (y - t * 16) % 16
    base = mix((236, 228, 214), (206, 196, 180), N1[y][x])
    if N2[0][x] > 0.6:
        base = mix(base, (255, 250, 236), 0.4 + 0.4 * math.sin(yy / 16 * math.tau))
    if N3[y][x] > 0.9:
        base = mix(base, (180, 160, 140), 0.6)
    return base


def nucleosynthesis_crust(x, y, t):                   # warm crust with specks of fresh nuclei flickering in it
    base = mix((214, 150, 90), (176, 112, 64), N1[y][x])
    if N2[y][x] > 0.85:
        ph = (N3[y][x] * 5 + t) % 1.0
        base = mix(base, (255, 246, 170), 1 - ph)
    return base


# ====================================================================== the blocks
# id, name, light, hardness, tool, sound, mineable, dimension, lore
BLOCKS = [
    ('annihilation_scar', 'Annihilation Scar', 6, 3.0, 'iron', 'GLASS', 'pickaxe', 'antiverse', annihilation_scar,
     "Antistone fused to glass where ordinary matter fell on it. Every proton and electron that arrived met its antiparticle and "
     "went up as a pair of gamma rays of exactly 511 keV, the electron's rest energy, and the heat of it melted the ground. Stray "
     "matter still arrives now and then: the flashes in it are each one more annihilation."),
    ('antihydrogen_firn', 'Antihydrogen Firn', 0, 0.8, 'none', 'SNOW', 'shovel', 'antiverse', antihydrogen_firn,
     "Old snow of frozen antihydrogen, packed but not yet ice, the way firn lies between snow and glacier on Earth. Each grain is "
     "an antiproton with a positron in orbit, the atom CERN's ALPHA experiment first trapped for a quarter of an hour."),
    ('spin_foam_mesh', 'Spin Foam Mesh', 4, 1.5, 'none', 'AMETHYST', 'pickaxe', 'quantum_foam', spin_foam_mesh,
     "Space as loop quantum gravity draws it: not a smooth stage but a graph. Each node is a grain of volume, each link a quantum "
     "of area, and the area of a surface is counted by the links that cross it, about the Planck area each. Here the graph shows."),
    ('metastable_crust', 'Metastable Crust', 3, 2.0, 'iron', 'CALCITE', 'pickaxe', 'quantum_foam', metastable_crust,
     "Crust grown at the edge of a false vacuum, a state that looks stable but is not the lowest. Its cracks brighten and fade as "
     "it nearly tunnels through to the true vacuum and does not, again and again."),
    ('stretched_rock', 'Stretched Rock', 0, 5.0, 'iron', 'DEEPSLATE', 'pickaxe', 'black_hole_interior', stretched_rock,
     "Infall rock drawn out along the line of fall. Near the singularity the pull on one end of anything is far stronger than on "
     "the other, so everything is stretched lengthwise and squeezed sideways: spaghettification, done to stone."),
    ('blueshift_crust', 'Blueshift Crust', 9, 4.0, 'iron', 'AMETHYST', 'pickaxe', 'black_hole_interior', blueshift_crust,
     "Rock at the inner horizon of a spinning black hole. All the light that ever fell in behind you arrives here at once, "
     "blueshifted without limit; the energy piling up is called mass inflation, and it glows in the stone."),
    ('outflow_rock', 'Outflow Rock', 6, 4.0, 'iron', 'BASALT', 'pickaxe', 'white_hole_interior', outflow_rock,
     "Ejecta caught on its way out. A white hole is a black hole run backwards: nothing can enter it, and all it holds is leaving. "
     "This rock set in columns along the way it was thrown."),
    ('nucleosynthesis_crust', 'Nucleosynthesis Crust', 5, 3.0, 'iron', 'TUFF', 'pickaxe', 'white_hole_interior', nucleosynthesis_crust,
     "Crust like the young universe's. In the first minutes after the Big Bang the hot plasma cooled enough for protons and "
     "neutrons to stick: deuterium, then helium, a little lithium. A white hole's outflow repeats it in miniature, and the flecks "
     "are fresh nuclei."),
]
FOUND = {'antiverse': 'the Antiverse', 'quantum_foam': 'the Quantum Foam', 'black_hole_interior': 'the Black Hole Interior',
         'white_hole_interior': 'the White Hole Interior'}
RESEARCH = {'antiverse': 'cpt_symmetry', 'quantum_foam': 'quantum_foam', 'black_hole_interior': 'black_hole_interior',
            'white_hole_interior': 'black_hole_interior'}
CATEGORY = {'antiverse': 'antimatter', 'quantum_foam': 'quantum', 'black_hole_interior': 'spacetime', 'white_hole_interior': 'spacetime'}

# ====================================================================== the biomes
# biome, dimension, name, block, sky/fog colours, particle, extra features, spawners (copied from the dimension's own biome, reweighted), description
BIOMES = [
    ('annihilation_front', 'antiverse', 'Annihilation Front', 'annihilation_scar', 0xD8B8E8, 0x6A3A68,
     {'options': {'type': 'minecraft:flash'}, 'probability': 0.0004}, ['annihilation_scar_vein'],
     {'theoreticalsciences:annihilator': 16},
     "Where ordinary matter strays into the Antiverse it annihilates on the ground, and the ground is fused to $(item)Annihilation Scar$(). Flashes come and go over it. $(thing)Annihilators$() gather here."),
    ('antihydrogen_glacier', 'antiverse', 'Antihydrogen Glacier', 'antihydrogen_firn', 0xC8E4FF, 0x8AB4D8,
     {'options': {'type': 'minecraft:snowflake'}, 'probability': 0.004}, ['liquid_antihelium_lake', 'antihydrogen_ice_rich'],
     {'theoreticalsciences:antihydrogen_wisp': 24},
     "Fields of $(item)Antihydrogen Firn$() over glacier ice, with pools of liquid antihelium. Antihydrogen Ice runs rich under the firn, and the $(thing)Antihydrogen Wisps$() are thick in the air."),
    ('spin_network', 'quantum_foam', 'Spin Network', 'spin_foam_mesh', 0x203A70, 0x10203E,
     {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.002}, ['spin_foam_web'],
     {},
     "Here the foam is drawn as loop quantum gravity draws space: a graph of nodes and links. Webs of $(item)Spin Foam Mesh$() hang between the membranes."),
    ('false_vacuum_shelf', 'quantum_foam', 'False Vacuum Shelf', 'metastable_crust', 0x40204E, 0x1E0E26,
     {'options': {'type': 'minecraft:witch'}, 'probability': 0.003}, ['false_vacuum_lake'],
     {},
     "Shelves of $(item)Metastable Crust$() about pools of false vacuum. The vacuum here is one push from decaying: carry a Coherence Stabiliser, and do not swim."),
    ('spaghettified_reaches', 'black_hole_interior', 'Spaghettified Reaches', 'stretched_rock', 0x3A1418, 0x200A0C,
     {'options': {'type': 'minecraft:crimson_spore'}, 'probability': 0.006}, ['tidal_plasma_lake', 'tidal_ore_rich'],
     {'theoreticalsciences:tidal_horror': 12},
     "Rock drawn out along the fall into $(item)Stretched Rock$(), pools of tidal plasma, and Tidal Ore running rich. The tides are at their worst here, and so are the $(thing)Tidal Horrors$()."),
    ('mass_inflation_zone', 'black_hole_interior', 'Mass Inflation Zone', 'blueshift_crust', 0x1A2A6A, 0x0A1030,
     {'options': {'type': 'minecraft:glow'}, 'probability': 0.004}, ['blueshift_spire'],
     {},
     "The inner horizon, where all the light that ever fell in arrives at once, blueshifted without end. $(item)Blueshift Crust$() glows with it, and spires of it stand up into the dark."),
    ('outflow_spires', 'white_hole_interior', 'Outflow Spires', 'outflow_rock', 0xFFF8EC, 0xF0E8DA,
     {'options': {'type': 'minecraft:white_ash'}, 'probability': 0.01}, ['outflow_spire'],
     {'theoreticalsciences:photon_moth': 14},
     "Columns of $(item)Outflow Rock$(), set where the ejecta was thrown. Everything here is leaving, and the spires point the way it goes."),
    ('reheating_fields', 'white_hole_interior', 'Reheating Fields', 'nucleosynthesis_crust', 0xFFE4C0, 0xE8C090,
     {'options': {'type': 'minecraft:small_flame'}, 'probability': 0.004}, ['reheating_plasma_lake', 'ejecta_ore_rich'],
     {},
     "A crust like the young universe's, $(item)Nucleosynthesis Crust$(), over pools of reheating plasma, and Ejecta Ore in plenty."),
]


def dim_biome(d):
    return json.load(open(os.path.join(D, 'worldgen/biome', d + '.json'), encoding='utf-8'))


# ====================================================================== worldgen features
def cf(name, ftype, config):
    dump(os.path.join(D, 'worldgen/configured_feature', name + '.json'), {'type': ftype, 'config': config})


def pf(name, feature, placement):
    dump(os.path.join(D, 'worldgen/placed_feature', name + '.json'), {'feature': ts(feature), 'placement': placement + [{'type': 'minecraft:biome'}]})


def state(b, props=None):
    s = {'Name': ts(b) if ':' not in b else b}
    if props:
        s['Properties'] = props
    return s


def simple(b, props=None):
    return {'type': 'minecraft:simple_state_provider', 'state': state(b, props)}


def match(b):
    return {'predicate_type': 'minecraft:block_match', 'block': ts(b) if ':' not in b else b}


SURF = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}]


def count(n):
    return {'type': 'minecraft:count', 'count': n}


def rare(n):
    return {'type': 'minecraft:rarity_filter', 'chance': n}


def band(lo, hi):
    return {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}}


def lake(name, fluid, barrier):
    cf(name, 'minecraft:lake', {'fluid': simple(fluid, {'level': '0'}), 'barrier': simple(barrier)})


def column(name, block, lo, hi):
    cf(name, 'minecraft:block_column', {'direction': 'up', 'allowed_placement': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'},
                                         'prioritize_tip': False,
                                         'layers': [{'height': {'type': 'minecraft:biased_to_bottom', 'min_inclusive': lo, 'max_inclusive': hi}, 'provider': simple(block)}]})


def ore(name, block, into, size):
    cf(name, 'minecraft:ore', {'size': size, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': match(into), 'state': state(block)}]})


def features():
    ore('annihilation_scar_vein', 'annihilation_scar', 'antistone', 24)
    pf('annihilation_scar_vein', 'annihilation_scar_vein', [count(6), {'type': 'minecraft:in_square'}, band(20, 200)])
    lake('liquid_antihelium_lake', 'liquid_antihelium', 'antihydrogen_firn')
    pf('liquid_antihelium_lake', 'liquid_antihelium_lake', [rare(6)] + SURF)
    ore('antihydrogen_ice_rich', 'antihydrogen_ice', 'antistone', 10)
    pf('antihydrogen_ice_rich', 'antihydrogen_ice_rich', [count(10), {'type': 'minecraft:in_square'}, band(0, 160)])
    ore('spin_foam_web', 'spin_foam_mesh', 'minecraft:air', 40)
    pf('spin_foam_web', 'spin_foam_web', [count(4), {'type': 'minecraft:in_square'}, band(8, 240)])
    lake('false_vacuum_lake', 'false_vacuum', 'metastable_crust')
    pf('false_vacuum_lake', 'false_vacuum_lake', [rare(4)] + SURF)
    lake('tidal_plasma_lake', 'tidal_plasma', 'stretched_rock')
    pf('tidal_plasma_lake', 'tidal_plasma_lake', [rare(4)] + SURF)
    ore('tidal_ore_rich', 'tidal_ore', 'infall_rock', 9)
    pf('tidal_ore_rich', 'tidal_ore_rich', [count(10), {'type': 'minecraft:in_square'}, band(16, 200)])
    column('blueshift_spire', 'blueshift_crust', 3, 12)
    pf('blueshift_spire', 'blueshift_spire', [count(3)] + SURF)
    column('outflow_spire', 'outflow_rock', 4, 18)
    pf('outflow_spire', 'outflow_spire', [count(5)] + SURF)
    lake('reheating_plasma_lake', 'reheating_plasma', 'nucleosynthesis_crust')
    pf('reheating_plasma_lake', 'reheating_plasma_lake', [rare(4)] + SURF)
    ore('ejecta_ore_rich', 'ejecta_ore', 'ejecta_rock', 9)
    pf('ejecta_ore_rich', 'ejecta_ore_rich', [count(10), {'type': 'minecraft:in_square'}, band(16, 220)])


# ====================================================================== biomes, the dimensions' biome sources, surfaces
def biomes():
    for bid, dim, name, block, sky, fog, particle, feats, weights, desc in BIOMES:
        b = dim_biome(dim)
        b = json.loads(json.dumps(b))
        b['effects']['sky_color'] = sky
        b['effects']['fog_color'] = fog
        b['effects']['particle'] = particle
        for cat, lst in b.get('spawners', {}).items():
            for s in lst:
                if s['type'] in weights:
                    s['weight'] = weights[s['type']]
        for f in feats:
            step = 6 if 'vein' in f or 'rich' in f or 'web' in f else (1 if 'lake' in f else 9)
            while len(b['features']) <= step:
                b['features'].append([])
            if ts(f) not in b['features'][step]:
                b['features'][step].append(ts(f))
        dump(os.path.join(D, 'worldgen/biome', bid + '.json'), b)
        L['biome.%s.%s' % (NS, bid)] = name

    FULL = [-1.0, 1.0]

    def climate(t, h):
        return {'temperature': t, 'humidity': h, 'continentalness': FULL, 'erosion': FULL, 'weirdness': FULL, 'depth': FULL, 'offset': 0.0}
    SHIFT = lambda n: {'type': 'minecraft:shifted_noise', 'noise': n, 'shift_x': 'minecraft:shift_x', 'shift_y': 0.0, 'shift_z': 'minecraft:shift_z',
                       'xz_scale': 0.25, 'y_scale': 0.0}
    for dim in FOUND:
        mine = [b for b in BIOMES if b[1] == dim]
        # the old biome keeps the middle of the climate, the two new ones the colder and the warmer edges (the Antiverse
        # takes the Overworld's broad climate, so its edges start nearer the middle, or they would lie kilometres out)
        k = 0.2 if dim == 'antiverse' else 0.35
        dump(os.path.join(D, 'dimension', dim + '.json'), {'type': ts(dim), 'generator': {'type': 'minecraft:noise', 'settings': ts(dim), 'biome_source': {
            'type': 'minecraft:multi_noise', 'biomes': [
                {'biome': ts(dim), 'parameters': climate([-k, k], FULL)},
                {'biome': ts(mine[0][0]), 'parameters': climate([k, 1.0], FULL)},
                {'biome': ts(mine[1][0]), 'parameters': climate([-1.0, -k], FULL)}]}}})

        def ns_fix(ns, mine=mine, dim=dim):
            r = ns['noise_router']
            if not isinstance(r.get('temperature'), dict):
                r['temperature'] = SHIFT('minecraft:temperature')
            if not isinstance(r.get('vegetation'), dict):
                r['vegetation'] = SHIFT('minecraft:vegetation')
            rule = ns['surface_rule']
            if ts(mine[0][0]) in json.dumps(rule):          # already done (the tool was run before)
                return
            top = {'type': 'minecraft:sequence', 'sequence': [
                {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [ts(b[0])]},
                 'then_run': {'type': 'minecraft:block', 'result_state': state(b[3])}} for b in mine]}
            floor = {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor',
                                                                 'add_surface_depth': True, 'secondary_depth_range': 0}, 'then_run': top}
            if rule.get('type') == 'minecraft:sequence':
                seq = rule['sequence']
                # after the bedrock floor (if any), before the dimension's own surface
                at = 1 if seq and 'vertical_gradient' in json.dumps(seq[0]) else 0
                seq.insert(at, floor)
            else:
                ns['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': [floor, rule]}
        T.edit_json(os.path.join(D, 'worldgen/noise_settings', dim + '.json'), ns_fix)


# ====================================================================== the blocks' data, Java lists, text and Codex
def blocks():
    cl = os.path.join(JAVA, 'QHContentList.java')
    s = open(cl, encoding='utf-8').read()
    snd = os.path.join(JAVA, 'QHBlockSounds.java')
    q = open(snd, encoding='utf-8').read()
    lines = []
    for bid, name, light, hard, tool, sound, mine, dim, fn, lore in BLOCKS:
        tex(bid, fn)
        simple_block(bid, name, lore, 'Found: in %s, on the ground of the %s.' % (FOUND[dim], next(b[2] for b in BIOMES if b[3] == bid)))
        if 'new BlockDef("%s"' % bid not in s:
            lines.append('        new BlockDef("%s", "simple", %d, %.1fF, "%s", ""),          // 1.11.0' % (bid, light, hard, tool))
        if '"%s"' % bid not in q:
            q = q.replace('            default -> null;', '            case "%s" -> SoundType.%s;          // 1.11.0\n            default -> null;' % (bid, sound), 1)
        T.tag_add(os.path.join(MC, 'tags/block/mineable', mine + '.json'), [ts(bid)])
        T.tag_add(os.path.join(D, 'tags/block/observable.json'), [ts(bid)])
        T.tag_add(os.path.join(D, 'tags/block/field/world.json'), [ts(bid)])
        T.tag_add(os.path.join(D, 'tags/item/field/world.json'), [ts(bid)])
    if lines:
        anchor = '        new BlockDef("antistone_bricks", "deco", 0, 3.0F, "iron", ""),\n'
        assert anchor in s
        s = s.replace(anchor, anchor + '\n'.join(lines) + '\n', 1)
    open(cl, 'w', encoding='utf-8').write(s)
    open(snd, 'w', encoding='utf-8').write(q)


def text_and_codex():
    entries, dims_nodes, mats_nodes = {}, [], []
    dims = json.load(open(os.path.join(A, 'ts_codex/dimensions.json'), encoding='utf-8'))
    mats = json.load(open(os.path.join(A, 'ts_codex/materials.json'), encoding='utf-8'))
    at_d = {n['entry']: n for n in dims['nodes']}
    at_m = {n['entry']: n for n in mats['nodes']}
    for i, (bid, name, light, hard, tool, sound, mine, dim, fn, lore) in enumerate(BLOCKS):
        biome = next(b[2] for b in BIOMES if b[3] == bid)
        key = 'world/node_' + bid
        entries[key] = {'title': name, 'icon': ts(bid), 'category': 'world', 'pages': [
            spot([bid], lore + '$(br2)$(thing)Found$(): in %s, on the ground of the %s.' % (FOUND[dim], biome), name)], 'node': True, 'research': RESEARCH[dim]}
        d0, m0 = at_d['world/node_antisoil'], at_m['world/node_antisoil']
        dims_nodes.append({'entry': key, 'x': d0['x'] - 6 + 2 * (i % 4), 'y': d0['y'] + 6 + 2 * (i // 4), 'parents': ['world/decor'], 'node': True})
        mats_nodes.append({'entry': key, 'x': m0['x'] - 6 + 2 * (i % 4), 'y': m0['y'] + 6 + 2 * (i // 4), 'parents': ['world/decor'], 'node': True})
    # one entry per dimension: its regions
    for dim in FOUND:
        mine = [b for b in BIOMES if b[1] == dim]
        key = '%s/%s_regions' % (CATEGORY[dim], dim)
        title = {'antiverse': 'Regions of the Antiverse', 'quantum_foam': 'Regions of the Foam', 'black_hole_interior': 'Regions of the Black Hole',
                 'white_hole_interior': 'Regions of the White Hole'}[dim]
        pages = [text('%s is no longer one place. Besides its old ground there are two more regions, each with its own ground, features and creatures.' % FOUND[dim].capitalize())]
        for bid, _, name, block, *_rest in mine:
            pages.append(spot([block], '$(thing)%s$(). %s' % (name, _rest[-1]), name))
        entries[key] = {'title': title, 'icon': ts(mine[0][3]), 'category': CATEGORY[dim], 'pages': pages, 'research': RESEARCH[dim]}
        parent = {'antiverse': 'antimatter/antiverse_life', 'quantum_foam': 'quantum/quantum_foam', 'black_hole_interior': 'spacetime/black_hole_interior',
                  'white_hole_interior': 'spacetime/black_hole_interior'}[dim]
        p0 = at_d[parent]
        dims_nodes.append({'entry': key, 'x': p0['x'] + 2, 'y': p0['y'] + 2, 'parents': [parent]})
    codex(entries, {'dimensions': dims_nodes, 'materials': mats_nodes})


if __name__ == '__main__':
    features()
    biomes()
    blocks()
    text_and_codex()
    finish()
    print('biomes:', len(BIOMES), 'blocks:', len(BLOCKS))
