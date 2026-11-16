"""
1.15.0 biomes: the Plume Field (the Titan Ocean's third biome) and the Protoplanetary Disk (the Accretion Frontier's sixth).
Run after round1150.py (which defines the blocks' text, drops and items):  python3 tools/round1150_biomes.py
"""
import json, os, random, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
TEX = os.path.join(A, 'textures/block')
JAVA = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/java/com/theoreticalsciences/registry')
FRAMES = 16


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


N1, N2 = noise(51), noise(52)
BEADS = [(3, 4, 1.6), (10, 2, 1.3), (12, 9, 1.8), (5, 11, 1.4), (8, 7, 1.1), (1, 14, 1.0), (14, 14, 1.2)]


def plume_ice(x, y, t):           # granular blue-white ice; silica grains glint as the light moves over them
    base = 205 + 30 * N1[y][x] - 18 * N2[(y + 3) % 16][(x * 3) % 16]
    c = [base * 0.86, base * 0.94, base * 1.02]
    g = N2[y][x]
    if g > 0.86:
        k = max(0.0, 1 - abs(((t * 3 + g * 7) % 1) - 0.5) * 4)
        c = [c[0] + 50 * k, c[1] + 55 * k, c[2] + 40 * k]
    return c


def chondritic_regolith(x, y, t):  # dark grey dust packed with round grey beads; a white inclusion catches the light
    d = 70 + 26 * N1[y][x]
    c = [d, d * 0.97, d * 0.92]
    for bx, by, r in BEADS:
        q = ((x - bx) ** 2 + (y - by) ** 2) ** 0.5
        if q <= r:
            s = 1 - q / (r + 0.6)
            c = [118 + 60 * s, 116 + 58 * s, 110 + 54 * s]
    if (x, y) in ((7, 13), (8, 13), (7, 14)):
        k = 0.5 + 0.5 * __import__('math').sin(t * 6.283)
        c = [215 + 30 * k, 212 + 30 * k, 200 + 25 * k]
    return c


BLOCKS = [('plume_ice', 1, 2.5, 'pickaxe', 'GLASS', plume_ice, 'titan_ocean', 'Plume Field'),
          ('chondritic_regolith', 0, 0.8, 'shovel', 'GRAVEL', chondritic_regolith, 'accretion_frontier', 'Protoplanetary Disk')]


def blocks():
    cl = os.path.join(JAVA, 'QHContentList.java')
    s = open(cl, encoding='utf-8').read()
    snd = os.path.join(JAVA, 'QHBlockSounds.java')
    q = open(snd, encoding='utf-8').read()
    lines = []
    for bid, light, hard, tool, sound, fn, dim, biome in BLOCKS:
        tex(bid, fn)
        if 'new BlockDef("%s"' % bid not in s:
            lines.append('        new BlockDef("%s", "simple", %d, %.1fF, "none", ""),          // 1.15.0' % (bid, light, hard))
        if '"%s"' % bid not in q:
            q = q.replace('            default -> null;', '            case "%s" -> SoundType.%s;          // 1.15.0\n            default -> null;' % (bid, sound), 1)
        T.tag_add(os.path.join(D, 'tags/block/observable.json'), [ts(bid)])
        T.tag_add(os.path.join(D, 'tags/block/field/world.json'), [ts(bid)])
        T.tag_add(os.path.join(D, 'tags/item/field/world.json'), [ts(bid)])
    if lines:
        anchor = '        new BlockDef("antistone_bricks", "deco", 0, 3.0F, "iron", ""),\n'
        assert anchor in s
        s = s.replace(anchor, anchor + '\n'.join(lines) + '\n', 1)
    open(cl, 'w', encoding='utf-8').write(s)
    open(snd, 'w', encoding='utf-8').write(q)


def biome(bid, parent, name, sky, fog, water, particle, extra_feats):
    b = json.loads(json.dumps(json.load(open(os.path.join(D, 'worldgen/biome', parent + '.json'), encoding='utf-8'))))
    b['effects']['sky_color'] = sky
    b['effects']['fog_color'] = fog
    if water:
        b['effects']['water_color'] = water
        b['effects']['water_fog_color'] = water
    if particle:
        b['effects']['particle'] = particle
    for step, f in extra_feats:
        while len(b['features']) <= step:
            b['features'].append([])
        if f not in b['features'][step]:
            b['features'][step].append(f)
    dump(os.path.join(D, 'worldgen/biome', bid + '.json'), b)
    L['biome.%s.%s' % (NS, bid)] = name


def split(dim, parent, new, hum=0.3):
    """The new biome takes the wetter part of its parent's climate."""
    def f(o):
        bl = o['generator']['biome_source']['biomes']
        if any(e['biome'] == ts(new) for e in bl):
            return
        for e in bl:
            if e['biome'] == ts(parent):
                p = json.loads(json.dumps(e['parameters']))
                e['parameters']['humidity'] = [-1.0, hum]
                p['humidity'] = [hum, 1.0]
                bl.append({'biome': ts(new), 'parameters': p})
                return
        raise SystemExit('no %s in %s' % (parent, dim))
    T.edit_json(os.path.join(D, 'dimension', dim + '.json'), f)


def surface(dim, rule):
    def f(ns):
        seq = ns['surface_rule'].get('sequence')
        if json.dumps(rule) in json.dumps(ns['surface_rule']):
            return
        if seq is None:
            ns['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': [rule, ns['surface_rule']]}
        else:
            seq.insert(0, rule)
    T.edit_json(os.path.join(D, 'worldgen/noise_settings', dim + '.json'), f)


blocks()
biome('plume_field', 'hydrothermal_vents', 'Plume Field', 0x2A4A68, 0x1C3A58, 0x3A7AB0,
      {'options': {'type': 'minecraft:bubble'}, 'probability': 0.01}, [])
biome('protoplanetary_disk', 'asteroid_belt', 'Protoplanetary Disk', 0xC89A60, 0x6A4A2E, None,
      {'options': {'type': 'minecraft:white_ash'}, 'probability': 0.012}, [])
split('titan_ocean', 'hydrothermal_vents', 'plume_field')
split('accretion_frontier', 'asteroid_belt', 'protoplanetary_disk')
# the Plume Field's ice shell is Plume Ice, overhead where the plumes break through
surface('titan_ocean', {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [ts('plume_field')]},
                        'then_run': {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:y_above', 'anchor': {'absolute': 150},
                                                                                'surface_depth_multiplier': 0, 'add_stone_depth': False},
                                     'then_run': {'type': 'minecraft:block', 'result_state': {'Name': ts('plume_ice')}}}})
# the disk's ground is chondritic dust, a few blocks deep
surface('accretion_frontier', {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [ts('protoplanetary_disk')]},
                               'then_run': {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 3, 'surface_type': 'floor',
                                                                                       'add_surface_depth': False, 'secondary_depth_range': 0},
                                            'then_run': {'type': 'minecraft:block', 'result_state': {'Name': ts('chondritic_regolith')}}}})
# found lines for the text rule
L['block.%s.plume_ice.found' % NS] = 'Found in the Titan Ocean\'s Plume Field, as its ice shell overhead.'
L['block.%s.chondritic_regolith.found' % NS] = "Found in the Accretion Frontier's Protoplanetary Disk, a few blocks deep on every floor."
# the biomes on the Codex's dimension tab
E = {'world/plume_field': {'title': 'Plume Field', 'icon': ts('plume_ice'), 'category': 'world', 'research': 'tidal_heating', 'pages': [
         spot(['plume_ice'], "In the Titan Ocean, where the vents run hot enough to break the ice: the shell overhead is $(item)Plume Ice$(), grainy with silica from the rock below, and the water is full of bubbles. Mine it for Plume Ice Grains.", 'Plume Field')]},
     'world/protoplanetary_disk': {'title': 'Protoplanetary Disk', 'icon': ts('chondritic_regolith'), 'category': 'world', 'research': 'planet_formation', 'pages': [
         spot(['chondritic_regolith'], "In the Accretion Frontier, a young disk still settling: every floor is $(item)Chondritic Regolith$(), packed with chondrules, and now and then a white Calcium-Aluminium-rich Inclusion, the oldest solid there is.", 'Protoplanetary Disk')]}}
codex(E, {'dimensions': [{'entry': 'world/plume_field', 'x': 20, 'y': 17, 'parents': []},
                         {'entry': 'world/protoplanetary_disk', 'x': 22, 'y': 17, 'parents': []}]})
finish()
print('biomes written')
