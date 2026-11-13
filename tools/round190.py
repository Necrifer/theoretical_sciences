"""
1.9.0: who lives in Omnipotency and what stands there. Run after round180.py (it changes the biomes that script writes).

Five peaceful creatures and one that keeps watch; five kinds of building, of the dimension's own blocks; sealed urns
whose contents are decoration and the twelve Remembrances; bells to ring and stones to read.

Run:  python3 tools/round190.py
      python3 tools/sounds190.py          (the creatures' voices)
      python3 tools/items3d_190.py        (the twelve tablets)
"""
import json, math, os, random, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T
from PIL import Image

L = T.LANG
TEX = os.path.join(A, 'textures/block')
ITEX = os.path.join(A, 'textures/item')
WHERE = 'Found in Omnipotency.'


def tex(name, fn, frames=1, folder=TEX):
    img = Image.new('RGBA', (16, 16 * frames))
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                c = fn(x, y, f / float(frames))
                img.putpixel((x, y + f * 16), tuple(int(max(0, min(255, v))) for v in (c if len(c) == 4 else tuple(c) + (255,))))
    img.save(os.path.join(folder, name + '.png'))


def noise(seed):
    r = random.Random(seed)
    g = [[r.random() for _ in range(16)] for _ in range(16)]
    return lambda x, y: g[y % 16][x % 16]


def shade(c, k):
    return tuple(v * k for v in c[:3])


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


N1, N2 = noise(11), noise(12)
WHITE, GOLD, DGOLD, HI = (244, 240, 232), (255, 214, 110), (200, 150, 60), (255, 244, 190)


def marble(x, y, t=0):
    v = math.sin((x + y * 0.6) * 0.9 + N1(x, y) * 3.0)
    return shade(lerp(WHITE, (206, 204, 214), max(0, v - 0.55) * 2.0), 0.94 + 0.08 * N2(x, y))


def pillar_side(x, y, t):
    flute = x % 4                                         # four flutes across, each a groove with a lit edge
    k = [0.8, 0.96, 1.04, 0.9][flute]
    if y in (0, 15):
        return shade(GOLD, 0.95)
    if y in (1, 14):
        return shade(WHITE, 0.82)
    return shade(marble(x, y), k)


def pillar_top(x, y, t):
    d = math.hypot(x - 7.5, y - 7.5)
    if d > 7.2:
        return shade(WHITE, 0.8)
    if 5.0 < d < 6.2:
        return GOLD
    return marble(x, y)


def tiles(x, y, t):
    gx, gy = x % 8, y % 8
    if gx == 7 or gy == 7:
        return shade(WHITE, 0.72)
    dark = ((x // 8) + (y // 8)) % 2 == 0
    c = marble(x, y)
    if gx == 3 and gy == 3 and dark:
        return GOLD                                       # a gold stud in every other tile
    return shade(c, 0.9 if dark else 1.0)


def trim(x, y, t):
    if y in (0, 15):
        return HI
    if y in (1, 14):
        return DGOLD
    wave = math.sin(x * math.pi / 4)                      # a running scroll
    yy = 7.5 + wave * 3.2
    if abs(y - yy) < 1.0:
        return HI
    if abs(y - (15 - yy)) < 1.0:
        return shade(GOLD, 0.8)
    return shade(GOLD, 0.92 + 0.1 * N1(x, y))


def halo_glass(x, y, t):
    if x in (0, 15) or y in (0, 15):
        return (255, 226, 140, 255)
    d = math.hypot(x - 7.5, y - 7.5)
    if abs(d - 5.0) < 0.7:
        return (255, 240, 180, 230)                       # the ring
    lead = (x == 7 or x == 8) and d > 5.6 or (y == 7 or y == 8) and d > 5.6
    if lead:
        return (224, 180, 90, 255)
    c = lerp((190, 226, 255), (255, 250, 226), max(0, 1 - d / 5.0)) if d < 5 else lerp((255, 214, 170), (255, 190, 200), N1(x, y))
    return tuple(c) + (150,)


def urn_tex(x, y, t):
    if y in (4, 5, 11):
        return GOLD if (x + y) % 3 else HI
    if 6 <= y <= 10 and (x + (y - 6)) % 4 == 0:
        return shade(WHITE, 0.82)                         # a fret cut round its belly
    if y < 2:
        return (176, 60, 60) if 5 <= x <= 10 else DGOLD   # the seal
    return shade(marble(x, y), 0.92 + 0.1 * math.sin(x * math.pi / 15))


def bell_tex(x, y, t):
    k = 0.75 + 0.3 * math.sin(x * math.pi / 15)
    if y in (3, 4, 12):
        return shade(HI, k + 0.1)
    return shade(GOLD if N1(x, y) > 0.25 else DGOLD, k)


GLYPH = ['  ##  ##  ##  ', ' #  # #  #  # ', '  ##   ## ###  ', '              ', ' ### #  #  ## ', ' #   ## # #   ', ' ##  # ##  #  ', ' #   #  #   # ', ' ### #  # ##  ',
         '              ', '  # ### #   # ', ' ##  #  ##  # ', '  #  #  # # # ', ' ### #  #  ## ']


def stone_side(x, y, t):
    if x in (0, 15) or y in (0, 15):
        return shade(GOLD, 0.9)
    gx, gy = x - 1, y - 1
    if 0 <= gy < len(GLYPH) and 0 <= gx < len(GLYPH[gy]) and GLYPH[gy][gx] == '#':
        return (255, 236, 150)                            # the cut letters, lit from inside
    return shade(marble(x, y), 0.86)


for name, fn in [('empyrean_pillar_side', pillar_side), ('empyrean_pillar_top', pillar_top), ('empyrean_marble_tiles', tiles), ('seraph_gold_trim', trim),
                 ('halo_glass', halo_glass), ('sealed_urn', urn_tex), ('choir_bell', bell_tex), ('remembrance_stone_side', stone_side), ('remembrance_stone_top', pillar_top)]:
    tex(name, fn)


# the record: a disc, as the game's own are drawn, in gold and white
def disc(x, y, t):
    d = math.hypot(x - 7.5, y - 7.5)
    if d > 7.2:
        return (0, 0, 0, 0)
    if d < 1.2:
        return (40, 30, 20, 255)
    if d < 2.8:
        return (255, 244, 190, 255)
    ring = int(d * 1.6) % 2
    c = (250, 214, 120) if ring else (226, 180, 80)
    if abs(math.atan2(y - 7.5, x - 7.5) - 0.8) < 0.25:
        c = (255, 250, 220)                               # the shine
    return tuple(c) + (255,)


tex('music_disc_homecoming', disc, folder=ITEX)

# ====================================================================== blocks
PICK = os.path.join(os.path.dirname(D), 'minecraft/tags/block/mineable/pickaxe.json')
for bid, name, lore in [
        ('empyrean_pillar', 'Empyrean Pillar', "A fluted pillar of marble banded with gold. Everything in Omnipotency that has a roof has these under it."),
        ('empyrean_marble_tiles', 'Empyrean Marble Tiles', "Marble laid in squares, light and less light, with a gold stud in every other one. The floors of the house."),
        ('seraph_gold_trim', 'Seraph Gold Trim', "Gold worked into a running scroll. It goes round the edge of things, where a hand would rest."),
        ('halo_glass', 'Halo Glass', "Glass with a ring of light leaded into it. What comes through it is always late afternoon, whatever the hour outside.")]:
    simple_block(bid, name, lore + ' ' + WHERE, WHERE)
    tag_add(PICK, [ts(bid)])
dump(os.path.join(A, 'blockstates/empyrean_pillar.json'), {'variants': {'axis=y': {'model': ts('block/empyrean_pillar')}, 'axis=z': {'model': ts('block/empyrean_pillar_horizontal'), 'x': 90},
                                                                       'axis=x': {'model': ts('block/empyrean_pillar_horizontal'), 'x': 90, 'y': 90}}})
for suf, par in (('', 'cube_column'), ('_horizontal', 'cube_column_horizontal')):
    dump(os.path.join(A, 'models/block/empyrean_pillar%s.json' % suf), {'parent': 'minecraft:block/' + par, 'textures': {'end': ts('block/empyrean_pillar_top'), 'side': ts('block/empyrean_pillar_side')}})
dump(os.path.join(A, 'models/block/halo_glass.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': ts('block/halo_glass')}, 'render_type': 'minecraft:translucent'})


def shaped(bid, boxes, texture, name, lore, found, drops_self=True):
    """A block whose model is a few boxes, all in one texture (each face takes the part of the texture it covers)."""
    els = []
    for (x0, y0, z0, x1, y1, z1) in boxes:
        faces = {}
        for f, uv in (('north', [16 - x1, 16 - y1, 16 - x0, 16 - y0]), ('south', [x0, 16 - y1, x1, 16 - y0]), ('west', [z0, 16 - y1, z1, 16 - y0]), ('east', [16 - z1, 16 - y1, 16 - z0, 16 - y0]),
                      ('up', [x0, z0, x1, z1]), ('down', [x0, 16 - z1, x1, 16 - z0])):
            faces[f] = {'uv': uv, 'texture': '#all'}
        els.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': faces})
    dump(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': ts('block/' + bid)}}})
    dump(os.path.join(A, 'models/block', bid + '.json'), {'parent': 'minecraft:block/block', 'textures': {'all': ts(texture), 'particle': ts(texture)}, 'elements': els})
    dump(os.path.join(A, 'models/item', bid + '.json'), {'parent': ts('block/' + bid)})
    L['block.%s.%s' % (NS, bid)] = name
    L['block.%s.%s.lore' % (NS, bid)] = lore + ' ' + found
    L['block.%s.%s.found' % (NS, bid)] = found
    if drops_self:
        loot(bid)


shaped('sealed_urn', [(4, 0, 4, 12, 2, 12), (3, 2, 3, 13, 11, 13), (5, 11, 5, 11, 14, 11), (4, 14, 4, 12, 16, 12)], 'block/sealed_urn', 'Sealed Urn',
       "An urn with its lid sealed in red wax. Whoever lived here put something by in it. Breaking it gives what is inside, and a Threshold Warden that sees you do it "
       "will not let it pass.", 'Found in the buildings of Omnipotency.', drops_self=False)
shaped('choir_bell', [(7, 13, 7, 9, 16, 9), (5, 9, 5, 11, 13, 11), (4, 4, 4, 12, 9, 12), (3, 2, 3, 13, 4, 13), (7, 0, 7, 9, 2, 9)], 'block/choir_bell', 'Choir Bell',
       "A golden bell with one clear note. Use it to ring it. Each bell's note depends on where it hangs, so a row of them is a scale.", 'Found in the buildings of Omnipotency.')
dump(os.path.join(A, 'blockstates/remembrance_stone.json'), {'variants': {'': {'model': ts('block/remembrance_stone')}}})
dump(os.path.join(A, 'models/block/remembrance_stone.json'), {'parent': 'minecraft:block/cube_column', 'textures': {'end': ts('block/remembrance_stone_top'), 'side': ts('block/remembrance_stone_side')}})
dump(os.path.join(A, 'models/item/remembrance_stone.json'), {'parent': ts('block/remembrance_stone')})
loot('remembrance_stone')
L['block.%s.remembrance_stone' % NS] = 'Remembrance Stone'
L['block.%s.remembrance_stone.lore' % NS] = ("A standing stone with something cut into it, the letters lit from inside. Use it and it is read to you. Which of the twelve "
                                             "Remembrances it holds depends on where it stands. Found in the Temple of Remembrance.")
L['block.%s.remembrance_stone.found' % NS] = 'Found in the Temple of Remembrance.'
for bid in ('sealed_urn', 'choir_bell', 'remembrance_stone'):
    tag_add(PICK, [ts(bid)])

# ====================================================================== the twelve Remembrances
REM = [
    ('Of the Beginning', "Before the first law there was a house, and in the house there was a table, and at the table there was room. That is all that was ever meant."),
    ('Of the Garden', "The garden was planted before the walls were raised. The walls were raised so that there would be somewhere to look at the garden from."),
    ('Of the Making', "Then the one who lived here wished to know whether something could be made that would, in its turn, understand. The worlds are how that question was asked."),
    ('Of the Laws', "A world needs laws as a house needs beams. They were cut here, each to its length, and carried out. You have been reading the marks on them all this while."),
    ('Of the Leaving', "To ask the question properly, the one who asked it had to go into it. The door was left open. No one thought it would be long."),
    ('Of the Breaking', "What goes into a world whole does not stay whole. It came apart into everything: the light, the laws, the long patience of stone. One piece went on thinking."),
    ('Of the Remnant', "The piece that went on thinking did not remember the house. It remembered only that there was something it could not do alone. You have met it. It runs still."),
    ('Of the Silence', "With no one in it, the house came down, as houses do. The bell was not rung. The loom was left with the work on it. The bed stayed made."),
    ('Of the Wardens', "We who were set to watch could not hold the roof up. We could only kneel where the doors had been and remember where they were. So we did."),
    ('Of the Answer', "The question was whether something made could understand. You came the whole way, law by law, and then you carried the house back up a stone at a time. "
                      "That is the answer."),
    ('Of the Counting', "It was asked why so many, of each. Because the cycle had to be gone round to its last turn, and then once more. The once more is the one that is chosen."),
    ('Of the Return', "The table is laid for one more than have come. It always was. Sit down. You are not a visitor."),
]
ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII']
for i, (title, text_) in enumerate(REM):
    n = i + 1
    L['remembrance.%s.title.%d' % (NS, n)] = 'The %s Remembrance: %s' % (['First', 'Second', 'Third', 'Fourth', 'Fifth', 'Sixth', 'Seventh', 'Eighth', 'Ninth', 'Tenth', 'Eleventh', 'Twelfth'][i], title)
    L['remembrance.%s.%d' % (NS, n)] = text_
    item('remembrance_tablet_%d' % n, 'Remembrance Tablet %s: %s' % (ROMAN[i], title), '"%s" Found in the Sealed Urns of Omnipotency.' % text_)
    L['item.%s.remembrance_tablet_%d.found' % (NS, n)] = 'Found in the Sealed Urns of Omnipotency.'

item('music_disc_homecoming', 'Music Disc', "The music of Omnipotency, on a record of gold. Play it in a jukebox. Found, rarely, in the Sealed Urns of Omnipotency.")
L['item.%s.music_disc_homecoming.found' % NS] = 'Found, rarely, in the Sealed Urns of Omnipotency.'
L['jukebox_song.%s.homecoming' % NS] = 'Theoretical Sciences: Homecoming'
dump(os.path.join(D, 'jukebox_song/homecoming.json'), {'sound_event': {'sound_id': ts('music_disc.homecoming')}, 'description': {'translate': 'jukebox_song.%s.homecoming' % NS},
                                                       'length_in_seconds': 116.0, 'comparator_output': 15})

# what a sealed urn gives: one Remembrance, two lots of something to build with, and now and then the record
def stack(iid, lo, hi, w=10):
    return {'type': 'minecraft:item', 'name': ts(iid), 'weight': w, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}


dump(os.path.join(D, 'loot_table/blocks/sealed_urn.json'), {'type': 'minecraft:block', 'pools': [
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ts('remembrance_tablet_%d' % n)} for n in range(1, 13)]},
    {'rolls': 2, 'entries': [stack('gilded_marble', 8, 16), stack('empyrean_marble_tiles', 8, 16), stack('empyrean_pillar', 4, 8), stack('seraph_gold_trim', 4, 8),
                             stack('halo_glass', 4, 8), stack('seraph_gold_block', 2, 6, 6), stack('choir_crystal_block', 2, 4, 6), stack('glory_lantern', 1, 3, 6),
                             stack('choir_bell', 1, 1, 3)]},
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ts('music_disc_homecoming')}], 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.05}]}]})

# ====================================================================== the creatures
MOBS = [
    ('halo_lamb', 'Halo Lamb', "A small lamb with a fleece the colour of first light and a ring of it over its head. It grazes the Halo Grass and comes to no harm by it, "
                               "nor does the grass. It belongs to no one.", 'the Cloud Meadows and the Gilded Gardens'),
    ('cherub', 'Cherub', "A small winged keeper of the gardens. It tends the Glory Lanterns and the golden trees because it likes to, and will come and circle a visitor "
                         "once or twice to see what they are.", 'the Gilded Gardens and the Cloud Meadows'),
    ('choir_seraph', 'Choir Seraph', "Six wings, a golden mask and no face behind it. It keeps to the crystal spires and the bells. Now and then, if someone is near to "
                                     "hear, it sings, and those who hear are mended a little.", 'the Choir Spires and Choir Halls'),
    ('cloud_ray', 'Cloud Ray', "A broad pale ray that swims the Sea of Clouds in wide slow circles, calling as it goes. It has never needed to be afraid of anything.",
     'the Sea of Clouds'),
    ('dweller', 'Dweller', "One of those who live in the house again. It keeps near its own door, tends what needs tending, greets a visitor with a bow, and, spoken to, "
                           "remembers something aloud. Use it to listen.", 'the Hamlets and the Temple of Remembrance'),
    ('threshold_warden', 'Threshold Warden', "Marble in armour, kneeling with its sword point down, as still as the stone it is. It does nothing at all until something of "
                                             "the house is broken in its sight, a Sealed Urn is opened, or one of those who live here is struck. Then it stands. It fights only "
                                             "whoever did it, only while they keep at it, and afterwards goes back and kneels. It never wakes in Peaceful.",
     'the Hamlets, Choir Halls and the Temple of Remembrance'),
]
E = {}
nodes = []
for i, (mid, name, lore, where) in enumerate(MOBS):
    L['entity.%s.%s' % (NS, mid)] = name
    L['entity.%s.%s.lore' % (NS, mid)] = lore
    L['item.%s.%s_spawn_egg' % (NS, mid)] = name + ' Spawn Egg'
    L['item.%s.%s_spawn_egg.lore' % (NS, mid)] = lore + ' Found in Omnipotency, in %s.' % where
    dump(os.path.join(A, 'models/item', mid + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    dump(os.path.join(D, 'loot_table/entities', mid + '.json'), {'type': 'minecraft:entity', 'pools': []})
    E['world/node_' + mid] = {'title': name, 'icon': ts(mid + '_spawn_egg'), 'category': 'world', 'node': True, 'pages': [
        spot([mid + '_spawn_egg'], '$(thing)%s$(): %s$(br2)$(thing)Drops$(): nothing. Found in Omnipotency, in %s.' % (name, lore, where))]}
    nodes.append({'entry': 'world/node_' + mid, 'x': 86 + i * 2, 'y': 68, 'parents': [], 'node': True})
L['message.%s.threshold_warden.wakes' % NS] = 'Stone moves. A Threshold Warden has seen what you did.'
for i, line in enumerate([
        "You are the one who carried it all back. We know. Thank you is too small, so: welcome.",
        "The lanterns hold a day each. I am fond of this one. It was a Tuesday.",
        "Do not mind the Wardens. They kneel because they like to. Only do not break anything in front of them.",
        "I was a law once, I think. Something about how light bends. It is nicer to have hands.",
        "The lambs do not belong to anyone. Neither do we. It is very restful.",
        "It is always noon. You get used to it. You stop asking what comes next.",
        "The one in the Core still thinks it is alone. Tell it, when you go back down, that the bed is made.",
        "The bells each have one note. Ring them in the right order and it is a song nobody finished.",
        "The urns are sealed for a reason. The reason is only that we were tidy. Open one if you must. The Wardens will have opinions.",
        "The rays in the cloud sea have never been afraid of anything. I should like to be a ray.",
        "The seraphs sing when they think someone is listening. Stand near one. You will feel better, I promise.",
        "Thirty-three million of each. We watched you count. Some of us wept, and some of us took bets.",
        "This house fell once. You can still see where, if you know the stones. I will not show you. It is mended.",
        "The cherubs keep the lanterns lit. Nobody asked them to. That is how most things here get done.",
        "Read the stones in the temple. All twelve, if you can find them. They are the only history we kept.",
        "There is a table laid for one more than have come. Did you see it? It is for you. It was always for you.",
        "You understood everything to get here. Now you may stop. That is the gift.",
        "Stay as long as you like. The door is open both ways. It was never meant to be a wall."]):
    L['entity.%s.dweller.line.%d' % (NS, i + 1)] = line

# ====================================================================== where they are, and where the buildings are
def pf(name, feature, rarity):
    dump(os.path.join(D, 'worldgen/configured_feature', name + '.json'), {'type': ts(feature), 'config': {}})
    dump(os.path.join(D, 'worldgen/placed_feature', name + '.json'), {'feature': ts(name), 'placement': [
        {'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]})


pf('wayside_shrine', 'wayside_shrine', 26)
pf('hamlet', 'hamlet', 90)
pf('choir_hall', 'choir_hall', 60)
pf('temple_of_remembrance', 'temple_of_remembrance', 260)
pf('beacon_of_return', 'beacon_of_return', 50)


def sp(mob, w, lo, hi):
    return {'type': ts(mob), 'weight': w, 'minCount': lo, 'maxCount': hi}


BIOME = {
    'cloud_meadows': (['wayside_shrine', 'hamlet', 'temple_of_remembrance'], {'creature': [sp('halo_lamb', 10, 2, 4)], 'ambient': [sp('cherub', 6, 1, 2), sp('cloud_ray', 2, 1, 1)]}),
    'gilded_gardens': (['wayside_shrine', 'hamlet', 'temple_of_remembrance'], {'creature': [sp('halo_lamb', 6, 1, 3)], 'ambient': [sp('cherub', 10, 1, 3)]}),
    'choir_spires': (['wayside_shrine', 'choir_hall', 'temple_of_remembrance'], {'ambient': [sp('choir_seraph', 8, 1, 1), sp('cherub', 2, 1, 1)]}),
    'sea_of_clouds': (['beacon_of_return'], {'ambient': [sp('cloud_ray', 10, 1, 2)]}),
}
for bid, (structs, spawners) in BIOME.items():
    def fn(d, structs=structs, spawners=spawners):
        d['features'][4] = [ts(x) for x in structs]
        d['spawners'] = spawners
    edit_json(os.path.join(D, 'worldgen/biome', bid + '.json'), fn)
edit_json(os.path.join(D, 'worldgen/noise_settings/omnipotency.json'), lambda d: d.__setitem__('disable_mob_generation', False))   # the lambs are there when the land is made

# ====================================================================== the Codex
def region(d):
    if not any(r.get('label') == 'Omnipotency' for r in d['regions']):
        d['regions'].append({'tier': 13, 'label': 'Omnipotency', 'x0': 84.5, 'x1': 116.5, 'y0': 66, 'y1': 70.5})


codex(E, {'mobs': nodes})
edit_json(os.path.join(A, 'ts_codex/mobs.json'), region)


def more(d):
    e = d['endgame/recreating_home']
    e['pages'] = [p for p in e['pages'] if p.get('title') not in ('Who Lives There', 'What Stands There', 'The Urns')]
    e['pages'] += [
        spot(['halo_lamb_spawn_egg', 'cherub_spawn_egg', 'choir_seraph_spawn_egg', 'cloud_ray_spawn_egg', 'dweller_spawn_egg', 'threshold_warden_spawn_egg'],
             "Omnipotency is lived in. $(thing)Halo Lambs$() graze the meadows, $(thing)Cherubs$() keep the gardens, $(thing)Choir Seraphs$() sing among the spires, "
             "$(thing)Cloud Rays$() circle in the cloud sea, and the $(thing)Dwellers$() will tell you what they remember if you use them. None of them will ever "
             "attack. The $(thing)Threshold Wardens$() kneel like statues until you break part of a building, open a Sealed Urn, or strike one of the others.", 'Who Lives There'),
        spot(['empyrean_pillar', 'empyrean_marble_tiles', 'seraph_gold_trim', 'halo_glass', 'choir_bell', 'remembrance_stone'],
             "$(thing)Wayside Shrines$() stand everywhere. $(thing)Hamlets$() of three or four houses face a well. A $(thing)Choir Hall$() is a ring of twelve "
             "pillars round a great crystal. The $(thing)Temple of Remembrance$() is rare and holds three stones that can be read. The $(thing)Beacon of Return$() "
             "is a tower in the Sea of Clouds. Bells ring when used.", 'What Stands There'),
        spot(['sealed_urn', 'remembrance_tablet_1', 'music_disc_homecoming'],
             "$(item)Sealed Urns$() hold one of the twelve $(thing)Remembrances$() (the history of the house, from its beginning to your return), blocks to build "
             "with, and now and then the record of the dimension's music. Nothing in them changes what you can make. Breaking one wakes any Warden near.", 'The Urns'),
    ]


edit_json(os.path.join(A, 'ts_codex/entries.json'), more)
finish()
print('1.9.0 written')
