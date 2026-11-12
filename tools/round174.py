"""
1.7.4: the Mana Port. A block that fits in a structure wherever an Interface Port does, holds Botania mana (as much as
a Mana Pool), and takes it from spreaders' bursts; the structure's recipes that ask for mana draw on it.

Run:  python3 tools/round174.py
"""
import json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T
from PIL import Image

L = T.LANG
TEX = os.path.join(A, 'textures/block')

# ---- the face: the Interface Port's frame, its window a pool of mana that ripples (all of the port's own frames)
src = Image.open(os.path.join(TEX, 'interface_port.png')).convert('RGBA')
w = src.width
n = src.height // w
out = Image.new('RGBA', (w, w * n))
import math
for f in range(n):
    fr = src.crop((0, f * w, w, (f + 1) * w))
    px = fr.load()
    for y in range(w):
        for x in range(w):
            r, g, b, a = px[x, y]
            d = math.hypot(x - (w - 1) / 2.0, y - (w - 1) / 2.0) / (w / 2.0)
            lum = (r * 0.3 + g * 0.6 + b * 0.1) / 255.0
            if d < 0.62:                                   # the window: mana, lighter in rings that move outwards
                ring = 0.5 + 0.5 * math.sin(d * 14.0 - f / float(n) * math.tau)
                k = 0.55 + 0.45 * ring
                px[x, y] = (int(40 + 60 * k), int(150 + 90 * k), int(215 + 40 * k), a)
            elif d < 0.72:                                 # its rim
                px[x, y] = (int(150 + 60 * lum), int(215 + 30 * lum), 255, a)
            else:                                          # the frame, cooled towards blue
                px[x, y] = (int(r * 0.78), int(g * 0.92), min(255, int(b * 1.08 + 10)), a)
    out.paste(fr, (0, f * w))
out.save(os.path.join(TEX, 'mana_port.png'))
mc = os.path.join(TEX, 'interface_port.png.mcmeta')
if os.path.exists(mc):
    open(os.path.join(TEX, 'mana_port.png.mcmeta'), 'w').write(open(mc).read())

dump(os.path.join(A, 'blockstates/mana_port.json'), {'variants': {'': {'model': ts('block/mana_port')}}})
dump(os.path.join(A, 'models/block/mana_port.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': ts('block/mana_port')}})
dump(os.path.join(A, 'models/item/mana_port.json'), {'parent': ts('block/mana_port')})
dump(os.path.join(D, 'loot_table/blocks/mana_port.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ts('mana_port')}],
                                                                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
tag_add(os.path.join(os.path.dirname(D), 'minecraft/tags/block/mineable/pickaxe.json'), [ts('mana_port')])
tag_add(os.path.join(os.path.dirname(D), 'minecraft/tags/block/needs_iron_tool.json'), [ts('mana_port')])
# made from an Interface Port, with Botania's own metal (so only where Botania is)
dump(os.path.join(D, 'recipe/crafting/mana_port.json'), {
    'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'botania'}],
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['MPM', 'PIP', 'MPM'],
    'key': {'M': {'item': 'botania:manasteel_ingot'}, 'P': {'item': 'botania:mana_pearl'}, 'I': {'item': ts('interface_port')}},
    'result': {'id': ts('mana_port'), 'count': 1}})

L['block.%s.mana_port' % NS] = 'Mana Port'
L['block.%s.mana_port.lore' % NS] = ("Takes the place of a structure's block wherever an Interface Port may, and holds as much mana as a Mana Pool. Aim a Mana Spreader "
                                     "at it: the structure's recipes that ask for mana draw on it. Against a single machine it serves that machine. Use it to read "
                                     "what it holds; a comparator reads it too. Made by crafting, with Botania.")
L['message.%s.mana_port' % NS] = 'Mana: %s of %s'
L['status.%s.no_mana' % NS] = 'Needs mana: fill a Mana Port in the structure, or stand a Mana Pool against it'
L['jei.%s.mana' % NS] = 'Mana: %s, from a Mana Port or a Mana Pool'


def upd(Eall):
    e = Eall['materials/magic_routes']
    e['pages'] = [p for p in e['pages'] if not p.get('r174')]
    e['pages'].append(dict(spot(['mana_port'], "A $(item)Mana Port$() takes the place of a structure's block wherever an Interface Port may, and holds as much as a Mana Pool. "
                                "Aim a $(item)Mana Spreader$() at it, and the structure's recipes that ask for mana draw on it. A Mana Pool against the controller still works.$(br2)Made by crafting.", 'The Mana Port'), r174=True))
    for p in e['pages']:
        if p.get('r173'):
            p['text'] = p['text'].replace("Stand a $(item)Mana Pool$() against the Revival Vat's controller.", "Fill a $(item)Mana Port$() in the Revival Vat, or stand a $(item)Mana Pool$() against its controller.")


edit_json(os.path.join(A, 'ts_codex/entries.json'), upd)
finish()
print('1.7.4 written')
