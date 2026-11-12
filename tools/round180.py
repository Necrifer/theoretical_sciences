"""
1.8.0, Recreating Home. For a player who has forged everything the Core can forge: forty-two new materials, 33,550,337
of each for the Core's checklist; the Empyrean Assembly Table (15 by 15, slots far past a stack) that makes them; the
Offering Altar that takes them from pipes; the Homeward Portal Frame; and Omnipotency, the place the frame opens on.

Run:  python3 tools/round180.py            (data, textures of the blocks, text)
      python3 tools/items3d_180.py         (the forty-two item textures)
"""
import collections, json, math, os, random, re, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T
import r180_pictures as PX
from PIL import Image

L = T.LANG
TEX = os.path.join(A, 'textures/block')
TARGET = 33550337
YIELD = 1                # 1.8.1: one a craft, on purpose
SLOT = 650000            # 1.8.1: and every slot of every one of these recipes takes this many

# ====================================================================== the forty-two
BASE = ['unified_field_lattice', 'perennial_catalyst_matrix', 'proof_lattice', 'living_archive', 'engineered_mind', 'planetary_survey', 'deep_record',
        'dyson_lattice_panel', 'stellar_wind_condensate', 'recursive_thought_shell', 'embryonic_vacuum',
        'compiled_law', 'lex_prima', 'breach_shard', 'pure_concept_ingot', 'diagonal_ingot', 'distilled_axiom',
        'transcendent_alloy', 'event_horizon_lens', 'pleromatic_weave', 'confluent_essence', 'eschaton_steel', 'convergent_matter',
        'consilience_core']
ELEMENTS = [
    ('remembered_soil', 'Remembered Soil', "Earth that has not forgotten what grew in it. Every garden that ever was is still in it, folded small, waiting to be asked."),
    ('living_water', 'Living Water', "Water that was never still long enough to be only water. It runs uphill when no one is watching, towards wherever it is needed."),
    ('first_breath', 'First Breath', "The air of the moment before the first word. It has been held ever since."),
    ('first_light', 'First Light', "Light from before there was anything for it to fall on. It is not bright. It is simply first."),
    ('hearthfire', 'Hearthfire', "A fire that warms and does not burn down. It was lit once and has only ever been carried."),
    ('seraphic_alloy', 'Seraphic Alloy', "Metal that remembers having wings. It is lighter than it should be and rings when it is spoken near."),
    ('choir_crystal', 'Choir Crystal', "Crystal grown in a place where something was always singing. Hold it to your ear and it is still going on."),
    ('woven_memory', 'Woven Memory', "Cloth whose threads are things that happened. The pattern is only visible from far enough away."),
    ('still_stone', 'Still Stone', "Stone that has finished. Nothing in it is waiting to become anything else, and it is at peace about that."),
    ('morning_dew', 'Morning Dew', "Dew from a morning that was not followed by a day. It has not dried, because nothing came to dry it."),
    ('evening_ember', 'Evening Ember', "The last coal of a long evening, kept. Whoever banked it meant to come back."),
    ('quiet_snow', 'Quiet Snow', "Snow that fell where there was no one to hear it. It keeps the quiet it fell in."),
    ('deep_root', 'Deep Root', "A root that went down until it found something to hold. It has not let go."),
    ('bright_seed', 'Bright Seed', "A seed that already knows what it will be, and is in no hurry."),
    ('kind_shadow', 'Kind Shadow', "Shade, of the sort a tree gives. There was light in the old house, and so there was this too, and it was welcome."),
    ('warm_rain', 'Warm Rain', "Rain that falls without a storm behind it. It was the weather of the place, and the place is where it is trying to fall."),
    ('salt_of_tears', 'Salt of Tears', "What is left when grief has dried. It keeps bread, and seasons it; nothing in the old house was wasted."),
    ('honey_of_days', 'Honey of Days', "Good days, kept the way bees keep summer. It does not spoil. Some of it is very old."),
    ('thread_of_fate', 'Thread of Fate', "Thread spun before anyone knew what would be sewn with it. It has no knots. It was never cut."),
    ('dust_of_stars', 'Dust of Stars', "What stars are swept up into when they are done. Everything you have ever built with was this, once."),
    ('ash_of_worlds', 'Ash of Worlds', "What is left of the worlds that ended so that this one could be tried. It is fine and grey and makes good mortar."),
    ('word_unspoken', 'Word Unspoken', "The thing that was about to be said when everything came apart. It is still meant."),
    ('name_forgotten', 'Name Forgotten', "A name no one is left to call. The tablet is blank where it was; the place it was is the material."),
    ('song_unsung', 'Song Unsung', "The next verse. It was never sung, so it has never ended."),
]
WORKS = [
    ('firmament_plate', 'Firmament Plate', "A piece of the sky of the old house, with its stars set in. The sky there was a made thing, and well made."),
    ('foundation_stone', 'Foundation Stone', "A stone cut to carry everything above it and to ask for nothing. A house is as good as the stones no one sees."),
    ('garden_bed', 'Garden Bed', "A bed of soil with the flowers already decided. The garden came before the house; the house was built to look at it."),
    ('wellspring', 'Wellspring', "A spring that rises because it wishes to. Set one in the ground and the ground remembers being a garden."),
    ('lantern_of_days', 'Lantern of Days', "A lantern that holds one whole day. There were as many of them as there were days, and none was ever put out."),
    ('bell_of_hours', 'Bell of Hours', "A bell that rang the hours of the old house. It has not been struck since. It is still in tune."),
    ('loom_of_years', 'Loom of Years', "The loom the years were woven on. The work was left on it, part done, and the shuttle is where it was set down."),
    ('gate_lintel', 'Gate Lintel', "The stone across the top of the gate. Everyone who came home walked under it. It is worn on the underside, from the looking up."),
    ('threshold_step', 'Threshold Step', "The step at the door, hollowed by feet. This is the piece that knows the difference between away and home."),
    ('roof_of_heaven', 'Roof of Heaven', "A span of the roof. It kept nothing out, for there was nothing to keep out; it was there so that there would be an inside."),
    ('table_of_welcome', 'Table of Welcome', "A table that is always laid for one more than have come."),
    ('bed_of_rest', 'Bed of Rest', "A bed, made, with the cover turned down. Whoever made it expected someone tired."),
]
PILLARS = [
    ('pillar_of_knowing', 'Pillar of Knowing', "The first of the five that held the house up: that things can be understood. You have stood under this one the whole way here."),
    ('pillar_of_making', 'Pillar of Making', "The second pillar: that what is understood can be built. Every machine you have raised leaned on it."),
    ('pillar_of_keeping', 'Pillar of Keeping', "The third pillar: that what is built can be kept. It is the plainest of the five and it carries the most."),
    ('pillar_of_mercy', 'Pillar of Mercy', "The fourth pillar: that what is broken may be forgiven for breaking. The house fell anyway. This is why it can be raised again."),
    ('pillar_of_return', 'Pillar of Return', "The fifth pillar: that there is a way back. It was the last to fall and it is the first to stand."),
]
KEY = ('keystone_of_return', 'Keystone of Return', "The stone at the top of the arch, which every other stone leans on. Set in a Homeward Portal Frame as an Eye of Ender is set "
       "in its frame: twelve, and the way opens.")
ALL = ELEMENTS + WORKS + PILLARS + [KEY]
IDS = [x[0] for x in ALL]
assert len(IDS) == 42 and len(set(IDS)) == 42
jsrc = open(os.path.join(HERE, '..', 'src/main/java/com/theoreticalsciences/home/HomeMaterials.java')).read()
assert re.findall(r'"([a-z_]+)"', jsrc[jsrc.index('IDS = List.of('):jsrc.index('public static int index')]) == IDS, 'HomeMaterials.java and round180.py disagree'

FOUND = 'Made in the Empyrean Assembly Table.'
for iid, name, lore in ALL:
    item(iid, name, lore + ' ' + FOUND)
    L['item.%s.%s.found' % (NS, iid)] = FOUND

# ====================================================================== the recipes
E_IDS = [x[0] for x in ELEMENTS]
W_IDS = [x[0] for x in WORKS]
P_IDS = [x[0] for x in PILLARS]
RECS = {}        # result -> (pattern, {letter: (item, count)}, result count, needs_home)


def letters(name):
    return collections.Counter(ch for row in PX.PIC[name] for ch in row if ch != ' ')


def assign(name, items, counts):
    """The picture's letters, most used first, take the items in order; the Consilience Core always takes the rarest."""
    cnt = letters(name)
    order = sorted(cnt, key=lambda ch: (-cnt[ch], ch))
    items = list(items)[:len(order)]
    if 'consilience_core' in items:
        items.remove('consilience_core')
        items.append('consilience_core')
    return {ch: (it, counts[min(i, len(counts) - 1)]) for i, (ch, it) in enumerate(zip(order, items))}


def uniq(xs):
    out = []
    for x in xs:
        if x not in out:
            out.append(x)
    return out


B23 = BASE[:23]
for i, iid in enumerate(E_IDS):          # the elements: of what the last machines make (one slot holds only a few: these are costly)
    its = uniq([B23[i % 23], B23[(i + 7) % 23], B23[(i + 13) % 23], B23[(i + 17) % 23], B23[(i + 5) % 23], B23[(i + 11) % 23]] + B23)
    if i == 23:                          # the last element is the one the Consilience Core goes into (its rarest slots)
        its = its[:len(letters(iid)) - 1] + ['consilience_core']
    RECS[iid] = (PX.PIC[iid], assign(iid, its, [SLOT]), YIELD, False)
for j, iid in enumerate(W_IDS):          # the works: of the elements, 64 to a slot
    its = uniq([E_IDS[2 * j], E_IDS[2 * j + 1], E_IDS[(2 * j + 9) % 24], E_IDS[(2 * j + 14) % 24], E_IDS[(2 * j + 19) % 24], E_IDS[(2 * j + 4) % 24]] + E_IDS)
    RECS[iid] = (PX.PIC[iid], assign(iid, its, [SLOT]), YIELD, False)
for k, iid in enumerate(P_IDS):          # the pillars: of the works (all twelve between them) and two elements, 64 to a slot
    its = uniq([W_IDS[2 * k], W_IDS[2 * k + 1], W_IDS[10 + k % 2], E_IDS[(k * 5) % 24], W_IDS[(2 * k + 5) % 12], E_IDS[(k * 5 + 12) % 24]] + W_IDS)
    RECS[iid] = (PX.PIC[iid], assign(iid, its, [SLOT]), YIELD, False)
m = assign(KEY[0], P_IDS + ['consilience_core'], [SLOT])
RECS[KEY[0]] = (PX.PIC[KEY[0]], m, YIELD, False)

# the frame: a gate drawn with all forty-two. The elements are its outer wall, the works the wall inside that, the
# pillars the ring round the opening, and the keystone the light in the middle.
CH = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
kf = {CH[i]: (iid, SLOT) for i, iid in enumerate(IDS)}
g = [[' '] * 15 for _ in range(15)]


def ring(r0, ids):
    cells = []
    n = 15 - 2 * r0
    for x in range(n):
        cells.append((r0 + x, r0))
    for y in range(1, n):
        cells.append((r0 + n - 1, r0 + y))
    for x in range(n - 2, -1, -1):
        cells.append((r0 + x, r0 + n - 1))
    for y in range(n - 2, 0, -1):
        cells.append((r0, r0 + y))
    for i, (x, y) in enumerate(cells):
        g[y][x] = CH[IDS.index(ids[i % len(ids)])]


ring(0, E_IDS)
ring(1, W_IDS)
ring(3, P_IDS)
for y in range(6, 9):
    for x in range(6, 9):
        g[y][x] = CH[41]
RECS['homeward_portal_frame'] = ([''.join(r) for r in g], kf, 1, True)

for res, (pat, key, n, home) in RECS.items():
    used = {ch for row in pat for ch in row if ch != ' '}
    assert used <= set(key), (res, used - set(key))
    j = {'type': ts('empyrean'), 'pattern': pat, 'key': {ch: {'item': ts(it), 'count': c} for ch, (it, c) in sorted(key.items()) if ch in used},
         'result': {'item': ts(res), 'count': n}}
    if home:
        j['needs_home'] = True
    dump(os.path.join(D, 'recipe/empyrean', res + '.json'), j)

# the table and the altar: on the Extreme Assembly Table, of the last materials and the reborn Core's own work
dump(os.path.join(D, 'recipe/extreme/empyrean_assembly_table.json'), extreme('empyrean_assembly_table', 1, PX.P9['empyrean_assembly_table'],
     {'A': 'eschaton_steel', 'B': 'transcendent_alloy', 'C': 'pleromatic_weave', 'D': 'confluent_essence', 'E': 'convergent_matter', 'F': 'eschaton_core',
      'G': 'event_horizon_lens'}))
dump(os.path.join(D, 'recipe/extreme/offering_altar.json'), extreme('offering_altar', 1, PX.P9['offering_altar'],
     {'A': 'transcendent_alloy', 'B': 'eschaton_steel', 'C': 'pleromatic_weave', 'D': 'consilience_core', 'E': 'confluent_essence', 'F': 'convergent_matter'}))


def demand():
    """What the whole checklist takes of every material, counting what the later recipes use up."""
    need = collections.Counter({iid: TARGET for iid in IDS})
    fr = RECS['homeward_portal_frame']
    for ch, n in collections.Counter(c for row in fr[0] for c in row if c != ' ').items():
        need[fr[1][ch][0]] += 12 * n * fr[1][ch][1]
    need[KEY[0]] += 12
    crafts = {}
    for iid in reversed(IDS):
        pat, key, n, _ = RECS[iid]
        crafts[iid] = -(-need[iid] // n)
        for ch, c in collections.Counter(c for row in pat for c in row if c != ' ').items():
            need[key[ch][0]] += crafts[iid] * c * key[ch][1]
    return need, crafts


if __name__ == '__main__' and 'demand' in sys.argv:
    need, crafts = demand()
    for b in BASE:
        print('%-28s %15s' % (b, format(need[b], ',')))
    print('elements: %s to %s each' % (format(min(need[e] for e in E_IDS), ','), format(max(need[e] for e in E_IDS), ',')))
    print('works:    %s to %s each' % (format(min(need[e] for e in W_IDS), ','), format(max(need[e] for e in W_IDS), ',')))
    print('pillars:  %s to %s each' % (format(min(need[e] for e in P_IDS), ','), format(max(need[e] for e in P_IDS), ',')))
    print('crafts in all: %s' % format(sum(crafts.values()), ','))
    sys.exit()

# ====================================================================== block textures
rng = random.Random(180)


def tex(name, fn, frames=1, size=16):
    img = Image.new('RGBA', (size, size * frames))
    for f in range(frames):
        for y in range(size):
            for x in range(size):
                c = fn(x, y, f / float(frames))
                img.putpixel((x, y + f * size), tuple(int(max(0, min(255, v))) for v in (c if len(c) == 4 else c + (255,))))
    img.save(os.path.join(TEX, name + '.png'))
    if frames > 1:
        json.dump({'animation': {'frametime': 2, 'interpolate': True}}, open(os.path.join(TEX, name + '.png.mcmeta'), 'w'))


def noise(seed):
    r = random.Random(seed)
    g = [[r.random() for _ in range(16)] for _ in range(16)]
    return lambda x, y: g[y % 16][x % 16]


def shade(c, k):
    return tuple(v * k for v in c)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


N1, N2, N3 = noise(1), noise(2), noise(3)
WHITE, CREAM, GOLD, DGOLD, SKY = (244, 240, 232), (232, 224, 208), (255, 214, 110), (200, 150, 60), (150, 200, 255)


def marble(x, y, t):
    v = math.sin((x + y * 0.6) * 0.9 + N1(x, y) * 3.0)
    c = lerp(WHITE, (206, 204, 214), max(0, v - 0.55) * 2.0)
    return shade(c, 0.94 + 0.08 * N2(x, y))


def bricks(x, y, t):
    row = y // 4
    off = 4 if row % 2 else 0
    edge = y % 4 == 3 or (x + off) % 8 == 7
    c = shade(WHITE, 0.92 + 0.08 * N2(x // 2 + row, row))
    return shade(c, 0.78) if edge else shade(c, 0.97 + 0.05 * N1(x, y))


def gilded(x, y, t):
    v = math.sin((x - y * 0.8) * 0.8 + N1(x, y) * 2.5)
    return shade(GOLD, 0.9 + 0.2 * N2(x, y)) if v > 0.72 else marble(x, y, t)


def soil(x, y, t):
    c = lerp((176, 150, 120), (150, 124, 98), N1(x, y))
    return shade(GOLD, 0.9) if N3(x, y) > 0.95 else c


def grass_top(x, y, t):
    c = lerp((190, 226, 150), (232, 240, 170), N1(x, y))
    return (255, 250, 210) if N3(x, y) > 0.93 else shade(c, 0.94 + 0.1 * N2(x, y))


def grass_side(x, y, t):
    edge = 3 + int(N1(x, 0) * 3)
    return grass_top(x, y, t) if y < edge else soil(x, y, t)


def cloudstone(x, y, t):
    v = 0.5 + 0.5 * math.sin(x * 0.7 + N1(x, y) * 2) * math.cos(y * 0.6 + N2(x, y) * 2)
    return lerp((236, 240, 250), (255, 255, 255), v)


def cloud(x, y, t):
    v = 0.5 + 0.5 * math.sin(x * 0.5 + y * 0.35 + N1(x, y) * 2)
    return lerp((255, 248, 226), (255, 255, 255), v) + (150 + 60 * v,)


def crystal(x, y, t):
    d = abs((x + y) % 8 - 4) / 4.0
    c = lerp((150, 206, 255), (236, 248, 255), d)
    if x in (0, 15) or y in (0, 15):
        c = (255, 255, 255)
    return c + (200,)


def goldblock(x, y, t):
    edge = x in (0, 15) or y in (0, 15)
    v = 0.5 + 0.5 * math.sin((x + y) * 0.6)
    c = lerp(DGOLD, GOLD, v)
    return (255, 240, 180) if edge else shade(c, 0.95 + 0.1 * N1(x, y))


def lantern(x, y, t):
    d = math.hypot(x - 7.5, y - 7.5) / 8.0
    if x in (0, 15) or y in (0, 15) or (x in (1, 14) and y in (1, 14)):
        return DGOLD
    return lerp((255, 255, 240), (255, 222, 130), min(1, d * (1.0 + 0.15 * math.sin(t * math.tau))))


# 1.8.1: the trees, drawn properly. Bark: pale silver in vertical plates with dark seams between them, short dark
# lenticels as a birch has, and here and there a crack with gold in it. End: rings, every third one gold, a gold pith,
# a dark rim of bark. Leaves: separate small leaves over a darker layer of leaves, with gaps to see through.
def _bark():
    r = random.Random(1811)
    px = [[None] * 16 for _ in range(16)]
    edges = [0, 3, 7, 10, 14]                           # the seams between the plates (it tiles: 16 wraps to 0)
    for x in range(16):
        k = max(i for i, e in enumerate(edges) if e <= x)
        w = (edges[k + 1] if k + 1 < len(edges) else 16) - edges[k]
        u = (x - edges[k]) / float(w)
        tone = 0.86 + 0.16 * math.sin(u * math.pi)      # each plate is rounded: bright in its middle
        for y in range(16):
            c = lerp((176, 182, 196), (236, 240, 246), tone - 0.86 + 0.5 + 0.04 * (r.random() - 0.5) + 0.05 * math.sin(y * 0.8 + k * 2.1))
            if x in edges:
                c = (120, 126, 142) if (y + k * 5) % 11 else (150, 156, 170)
            elif x - 1 in edges:
                c = shade(c, 0.9)
            px[y][x] = c
    for _ in range(5):                                   # lenticels
        y, x, n = r.randrange(16), r.randrange(16), r.choice([2, 2, 3])
        for d in range(n):
            xx = (x + d) % 16
            if xx not in edges:
                px[y][xx] = (104, 110, 128)
                px[(y + 1) % 16][xx] = (250, 252, 255)
    for x0, y0, n in ((5, 3, 4),):                       # one crack with gold in it
        x = x0
        for d in range(n):
            y = (y0 + d) % 16
            px[y][x] = (255, 216, 110) if d not in (0, n - 1) else (214, 160, 70)
            px[y][(x + 1) % 16] = shade(px[y][(x + 1) % 16], 0.82)
            if d == n // 2:
                x = (x + 1) % 16
    return px


def _bark_top():
    px = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            rr = math.hypot(x - 7.5, y - 7.5)
            if d > 6.6:
                c = (128, 134, 150) if d > 7.4 else (206, 212, 224)
            elif rr < 1.2:
                c = (255, 226, 130)
            else:
                ring = int(rr * 1.25 + 0.3 * math.sin(math.atan2(y - 7.5, x - 7.5) * 3))
                c = (238, 196, 96) if ring % 3 == 0 else ((248, 242, 226) if ring % 3 == 1 else (226, 216, 196))
            px[y][x] = c
    for d in range(1, 6):                                # one drying crack from the pith
        px[7 - d // 2][8 + d] = (176, 150, 100)
    return px


def _leaves():
    r = random.Random(1812)
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    LEAF = [(0, 1), (1, 0), (1, 1), (1, 2), (2, 0), (2, 1), (2, 2), (2, 3), (3, 1), (3, 2), (3, 3), (4, 2)]      # a small leaf lying on the diagonal

    def leaf(x0, y0, flip, tones):
        mirror = r.random() < 0.5                            # leaves point both ways
        for i, (a, b) in enumerate(LEAF):
            x, y = ((x0 + ((4 - (b if flip else a)) if mirror else (b if flip else a))) % 16, (y0 + (a if flip else b)) % 16)
            vein = a == b + 0 and a in (1, 2, 3)
            edge = (a, b) in ((0, 1), (4, 2), (2, 3), (3, 3), (1, 2))
            px[y][x] = tones[2] if vein else (tones[0] if edge else tones[1])

    back = [(150, 104, 34, 255), (186, 134, 44, 255), (206, 156, 60, 255)]
    front = [(206, 150, 48, 255), (250, 204, 92, 255), (255, 240, 170, 255)]
    pale = [(224, 176, 70, 255), (255, 226, 130, 255), (255, 250, 210, 255)]
    for _ in range(17):
        leaf(r.randrange(16), r.randrange(16), r.random() < 0.5, back)
    for i in range(11):
        leaf(r.randrange(16), r.randrange(16), r.random() < 0.5, pale if i % 4 == 0 else front)
    return px


_BS, _BT, _LV = _bark(), _bark_top(), _leaves()


def bark_side(x, y, t):
    return _BS[y][x]


def bark_top(x, y, t):
    return _BT[y][x]


def leaves(x, y, t):
    return _LV[y][x]


for name, fn, fr in [('empyrean_marble', marble, 1), ('empyrean_marble_bricks', bricks, 1), ('gilded_marble', gilded, 1), ('heaven_soil', soil, 1),
                     ('halo_grass_top', grass_top, 1), ('halo_grass_side', grass_side, 1), ('cloudstone', cloudstone, 1), ('radiant_cloud', cloud, 1),
                     ('choir_crystal_block', crystal, 1), ('seraph_gold_block', goldblock, 1), ('glory_lantern', lantern, 16),
                     ('silver_bark_side', bark_side, 1), ('silver_bark_top', bark_top, 1), ('aureate_leaves', leaves, 1)]:
    tex(name, fn, fr)


# the table: its top is the 15 by 15 grid itself
def table_top(x, y, t):
    if x in (0, 15) or y in (0, 15):
        return GOLD
    lit = (x + y + int(t * 16)) % 16 == 0
    return (255, 244, 200) if lit else ((52, 40, 20) if (x + y) % 2 else (70, 54, 26))


def table_side(x, y, t):
    if y < 3:
        return GOLD if y != 1 else (255, 244, 200)
    if x in (0, 1, 14, 15):
        return shade(WHITE, 0.9 if x in (0, 15) else 1.0)
    if y > 12:
        return shade(WHITE, 0.85)
    d = abs(x - 7.5) + abs(y - 8)
    return lerp((255, 232, 150), (44, 34, 18), min(1, d / (3.5 + 0.8 * math.sin(t * math.tau))))


def table_bottom(x, y, t):
    return shade(marble(x, y, t), 0.8)


def altar_top(x, y, t):
    d = math.hypot(x - 7.5, y - 7.5)
    if d < 2.5 + 0.8 * math.sin(t * math.tau):
        return (255, 250, 220)
    if d < 5.5:
        return lerp(GOLD, DGOLD, (d - 2.5) / 3)
    return marble(x, y, t)


def altar_side(x, y, t):
    if y < 2 or y > 13:
        return shade(GOLD, 0.95)
    if 5 <= x <= 10:
        return lerp((255, 240, 190), DGOLD, abs(y - 7.5) / 6.0)
    return marble(x, y, t)


def frame_top(x, y, t):
    if x in (0, 15) or y in (0, 15):
        return GOLD
    if 4 <= x <= 11 and 4 <= y <= 11:
        return (60, 48, 30) if not (x in (4, 11) or y in (4, 11)) else DGOLD
    return marble(x, y, t)


def frame_top_key(x, y, t):
    if 5 <= x <= 10 and 5 <= y <= 10:
        d = math.hypot(x - 7.5, y - 7.5)
        return lerp((255, 255, 236), (255, 214, 110), min(1, d / (3.2 + 0.6 * math.sin(t * math.tau))))
    return frame_top(x, y, t)


def frame_side(x, y, t):
    if y < 3:
        return (0, 0, 0, 0)
    if y == 3 or y == 15:
        return GOLD
    if x % 5 == 2 and 5 < y < 14:
        return shade(WHITE, 0.82)
    return marble(x, y, t)


def portal(x, y, t):
    d = math.hypot(x - 7.5, y - 7.5)
    v = 0.5 + 0.5 * math.sin(d * 1.1 - t * math.tau + math.atan2(y - 7.5, x - 7.5) * 2)
    return lerp((255, 236, 170), (255, 255, 250), v) + (225,)


for name, fn, fr in [('empyrean_assembly_table_top', table_top, 16), ('empyrean_assembly_table_side', table_side, 16), ('empyrean_assembly_table_bottom', table_bottom, 1),
                     ('offering_altar_top', altar_top, 16), ('offering_altar_side', altar_side, 1), ('homeward_portal_frame_top', frame_top, 1),
                     ('homeward_portal_frame_top_keystone', frame_top_key, 16), ('homeward_portal_frame_side', frame_side, 1), ('homeward_portal', portal, 16)]:
    tex(name, fn, fr)

# ====================================================================== blocks
WHERE = 'Found in Omnipotency.'
BLOCKS = [
    ('empyrean_marble', 'Empyrean Marble', "The stone the islands of Omnipotency are made of: white, faintly veined, and warm to the hand, as if the sun had only just left it.", 'pick'),
    ('empyrean_marble_bricks', 'Empyrean Marble Bricks', "Marble cut and laid by someone who had all the time there was. The arrival terrace is built of it.", 'pick'),
    ('gilded_marble', 'Gilded Marble', "Marble with gold run through it like a river through a field. The gold was not put there. It grew.", 'pick'),
    ('heaven_soil', 'Heaven Soil', "Pale earth with a little gold in it. Anything planted in it comes up; the trouble is deciding what to plant.", 'shovel'),
    ('halo_grass', 'Halo Grass', "Grass the colour of early light. It does not need mowing. It has decided how long to be.", 'shovel'),
    ('cloudstone', 'Cloudstone', "Cloud that has agreed to be stood on. The islands rest on it underneath, where they would otherwise end in nothing.", 'none'),
    ('radiant_cloud', 'Radiant Cloud', "Cloud that has not agreed to be stood on. Walk into it and you sink slowly, unharmed, in a light with no direction.", 'none'),
    ('choir_crystal_block', 'Choir Crystal Spire', "Crystal standing in tall spires where the islands are bare. The wind in them is the nearest thing here to a voice.", 'pick'),
    ('seraph_gold_block', 'Seraph Gold', "Gold found in the marble in seams. It is too soft for tools and was never meant for them.", 'pick'),
    ('glory_lantern', 'Glory Lantern', "A light that grows in the gardens like a flower. It is always noon here; they shine anyway.", 'pick'),
    ('silver_bark', 'Silver Bark', "The trunk of the trees of the Gilded Gardens: smooth, pale, cool, and ringed inside with gold.", 'axe'),
    ('aureate_leaves', 'Aureate Leaves', "Leaves that turned gold once and saw no reason to turn back.", 'hoe'),
]
for bid, name, lore, tool in BLOCKS:
    simple_block(bid, name, lore + ' ' + WHERE, WHERE)
    if tool != 'none':
        tag_add(os.path.join(os.path.dirname(D), 'minecraft/tags/block/mineable/%s.json' % {'pick': 'pickaxe'}.get(tool, tool)), [ts(bid)])
dump(os.path.join(A, 'models/block/halo_grass.json'), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': ts('block/halo_grass_top'), 'side': ts('block/halo_grass_side'), 'bottom': ts('block/heaven_soil')}})
dump(os.path.join(A, 'models/block/aureate_leaves.json'), {'parent': 'minecraft:block/leaves', 'textures': {'all': ts('block/aureate_leaves')}, 'render_type': 'minecraft:cutout_mipped'})
for bid in ('radiant_cloud', 'choir_crystal_block'):
    dump(os.path.join(A, 'models/block/%s.json' % bid), {'parent': 'minecraft:block/cube_all', 'textures': {'all': ts('block/' + bid)}, 'render_type': 'minecraft:translucent'})
dump(os.path.join(A, 'blockstates/silver_bark.json'), {'variants': {'axis=y': {'model': ts('block/silver_bark')}, 'axis=z': {'model': ts('block/silver_bark_horizontal'), 'x': 90},
                                                                   'axis=x': {'model': ts('block/silver_bark_horizontal'), 'x': 90, 'y': 90}}})
for suf, par in (('', 'cube_column'), ('_horizontal', 'cube_column_horizontal')):
    dump(os.path.join(A, 'models/block/silver_bark%s.json' % suf), {'parent': 'minecraft:block/' + par, 'textures': {'end': ts('block/silver_bark_top'), 'side': ts('block/silver_bark_side')}})

# the table
dump(os.path.join(A, 'blockstates/empyrean_assembly_table.json'), {'variants': {'': {'model': ts('block/empyrean_assembly_table')}}})
dump(os.path.join(A, 'models/block/empyrean_assembly_table.json'), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': ts('block/empyrean_assembly_table_top'), 'bottom': ts('block/empyrean_assembly_table_bottom'), 'side': ts('block/empyrean_assembly_table_side')}})
dump(os.path.join(A, 'models/item/empyrean_assembly_table.json'), {'parent': ts('block/empyrean_assembly_table')})
# the altar
dump(os.path.join(A, 'blockstates/offering_altar.json'), {'variants': {'': {'model': ts('block/offering_altar')}}})
dump(os.path.join(A, 'models/block/offering_altar.json'), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': ts('block/offering_altar_top'), 'bottom': ts('block/empyrean_assembly_table_bottom'), 'side': ts('block/offering_altar_side')}})
dump(os.path.join(A, 'models/item/offering_altar.json'), {'parent': ts('block/offering_altar')})
loot('offering_altar')
# the frame: thirteen high, as an End Portal frame is, with the keystone standing in its top when set
FACE = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def frame_model(key):
    el = [{'from': [0, 0, 0], 'to': [16, 13, 16], 'faces': {
        'down': {'uv': [0, 0, 16, 16], 'texture': '#bottom', 'cullface': 'down'}, 'up': {'uv': [0, 0, 16, 16], 'texture': '#top'},
        'north': {'uv': [0, 3, 16, 16], 'texture': '#side', 'cullface': 'north'}, 'south': {'uv': [0, 3, 16, 16], 'texture': '#side', 'cullface': 'south'},
        'west': {'uv': [0, 3, 16, 16], 'texture': '#side', 'cullface': 'west'}, 'east': {'uv': [0, 3, 16, 16], 'texture': '#side', 'cullface': 'east'}}}]
    if key:
        el.append({'from': [5, 13, 5], 'to': [11, 16, 11], 'faces': {f: {'uv': [5, 5, 11, 11] if f == 'up' else [5, 5, 11, 8], 'texture': '#key'}
                                                                    for f in ('up', 'north', 'south', 'west', 'east')}})
    return {'parent': 'minecraft:block/block', 'textures': {'particle': ts('block/empyrean_marble'), 'bottom': ts('block/empyrean_assembly_table_bottom'),
                                                            'top': ts('block/homeward_portal_frame_top' + ('_keystone' if key else '')),
                                                            'side': ts('block/homeward_portal_frame_side'), 'key': ts('block/homeward_portal_frame_top_keystone')},
            'elements': el}


dump(os.path.join(A, 'models/block/homeward_portal_frame.json'), frame_model(False))
dump(os.path.join(A, 'models/block/homeward_portal_frame_keystone.json'), frame_model(True))
dump(os.path.join(A, 'blockstates/homeward_portal_frame.json'), {'variants': {
    'facing=%s,keystone=%s' % (f, k): {'model': ts('block/homeward_portal_frame' + ('_keystone' if k == 'true' else '')), 'y': r}
    for f, r in FACE.items() for k in ('true', 'false')}})
dump(os.path.join(A, 'models/item/homeward_portal_frame.json'), {'parent': ts('block/homeward_portal_frame')})
loot('homeward_portal_frame')
# the portal: a sheet of light a little below the frames' tops
dump(os.path.join(A, 'blockstates/homeward_portal.json'), {'variants': {'': {'model': ts('block/homeward_portal')}}})
dump(os.path.join(A, 'models/block/homeward_portal.json'), {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
     'textures': {'particle': ts('block/homeward_portal'), 'all': ts('block/homeward_portal')},
     'elements': [{'from': [0, 6, 0], 'to': [16, 12, 16], 'shade': False, 'faces': {'up': {'uv': [0, 0, 16, 16], 'texture': '#all'}, 'down': {'uv': [0, 0, 16, 16], 'texture': '#all'}}}]})
for bid in ('empyrean_assembly_table', 'offering_altar', 'homeward_portal_frame'):
    tag_add(os.path.join(os.path.dirname(D), 'minecraft/tags/block/mineable/pickaxe.json'), [ts(bid)])

L['block.%s.empyrean_assembly_table' % NS] = 'Empyrean Assembly Table'
L['block.%s.empyrean_assembly_table.lore' % NS] = ("A crafting table of fifteen by fifteen, each slot of which holds far more than a stack. Choose a recipe from its list; what you "
    "put in goes to the slots that ask for it. Pipes and hoppers can fill it and take from it, and it can be set to craft by itself. Broken, it keeps what it holds. "
    "Made in the Extreme Assembly Table.")
L['block.%s.empyrean_assembly_table.found' % NS] = 'Made in the Extreme Assembly Table.'
L['block.%s.offering_altar' % NS] = 'Offering Altar'
L['block.%s.offering_altar.lore' % NS] = ("Gives to the Core whatever of the forty-two materials is piped into it, on the checklist of the player who placed it; it takes "
    "only what that checklist still needs. Use it to offer what you carry and to read how far along you are. Made in the Extreme Assembly Table.")
L['block.%s.offering_altar.found' % NS] = 'Made in the Extreme Assembly Table.'
L['block.%s.homeward_portal_frame' % NS] = 'Homeward Portal Frame'
L['block.%s.homeward_portal_frame.lore' % NS] = ("Twelve in a ring round a three by three opening, each facing in, as an End Portal is laid. Set a Keystone of Return in each "
    "and the opening fills with light. Only one whose own checklist is complete may pass through it. Made in the Empyrean Assembly Table.")
L['block.%s.homeward_portal_frame.found' % NS] = FOUND
L['block.%s.homeward_portal' % NS] = 'Homeward Portal'

# ====================================================================== Omnipotency
VAN = '/tmp/claude-0/-home-claude/51dad36d-3125-5483-a3e3-ab1ab9d31062/scratchpad/van/data/minecraft/worldgen/noise_settings/floating_islands.json'
if not os.path.exists(VAN):
    import zipfile
    z = zipfile.ZipFile('/home/claude/libs/client-1.21.1-20240808.144430-extra.jar')
    ns = json.loads(z.read('data/minecraft/worldgen/noise_settings/floating_islands.json'))
else:
    ns = json.load(open(VAN))
BIOMES = ['cloud_meadows', 'gilded_gardens', 'choir_spires', 'sea_of_clouds']


def blk(b):
    return {'type': 'minecraft:block', 'result_state': {'Name': ts(b)}}


def in_biome(bs, rule):
    return {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [ts(b) for b in bs]}, 'then_run': rule}


def depth(surface, deep, rule):
    return {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': surface, 'add_surface_depth': deep,
                                                        'secondary_depth_range': 0}, 'then_run': rule}


ns['default_block'] = {'Name': ts('empyrean_marble')}
ns['default_fluid'] = {'Name': 'minecraft:air'}
ns['disable_mob_generation'] = True
ns['spawn_target'] = []
ns['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': [
    depth('floor', False, {'type': 'minecraft:sequence', 'sequence': [
        in_biome(['cloud_meadows', 'gilded_gardens'], blk('halo_grass')), in_biome(['choir_spires'], blk('gilded_marble')), in_biome(['sea_of_clouds'], blk('cloudstone'))]}),
    depth('floor', True, {'type': 'minecraft:sequence', 'sequence': [
        in_biome(['cloud_meadows', 'gilded_gardens'], blk('heaven_soil')), in_biome(['sea_of_clouds'], blk('cloudstone'))]}),
    depth('ceiling', True, blk('cloudstone'))]}      # the islands rest on cloud
dump(os.path.join(D, 'worldgen/noise_settings/omnipotency.json'), ns)
dump(os.path.join(D, 'dimension_type/omnipotency.json'), {
    'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': True, 'has_raids': False, 'has_skylight': True,
    'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.35, 'logical_height': 256, 'effects': ts('omnipotency'),
    'infiniburn': '#minecraft:infiniburn_overworld', 'min_y': 0, 'height': 256, 'fixed_time': 6000, 'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
FULL = [-1.0, 1.0]


def climate(t, h):
    return {'temperature': t, 'humidity': h, 'continentalness': FULL, 'erosion': FULL, 'weirdness': FULL, 'depth': FULL, 'offset': 0.0}


dump(os.path.join(D, 'dimension/omnipotency.json'), {'type': ts('omnipotency'), 'generator': {'type': 'minecraft:noise', 'settings': ts('omnipotency'), 'biome_source': {
    'type': 'minecraft:multi_noise', 'biomes': [
        {'biome': ts('cloud_meadows'), 'parameters': climate([-1.0, 0.0], [-1.0, 0.0])},
        {'biome': ts('gilded_gardens'), 'parameters': climate([0.0, 1.0], [0.0, 1.0])},
        {'biome': ts('choir_spires'), 'parameters': climate([-1.0, 0.0], [0.0, 1.0])},
        {'biome': ts('sea_of_clouds'), 'parameters': climate([0.0, 1.0], [-1.0, 0.0])}]}}})


def cf(name, ftype, config):
    dump(os.path.join(D, 'worldgen/configured_feature', name + '.json'), {'type': ftype, 'config': config})


def pf(name, feature, placement):
    dump(os.path.join(D, 'worldgen/placed_feature', name + '.json'), {'feature': ts(feature), 'placement': placement + [{'type': 'minecraft:biome'}]})


def simple(b, props=None):
    s = {'Name': ts(b)}
    if props:
        s['Properties'] = props
    return {'type': 'minecraft:simple_state_provider', 'state': s}


def on(block):
    return {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_blocks', 'offset': [0, -1, 0], 'blocks': ts(block)}}


SURF = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}]


def count(n):
    return {'type': 'minecraft:count', 'count': n}


def rare(n):
    return {'type': 'minecraft:rarity_filter', 'chance': n}


def band(lo, hi):
    return {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}}


AIR = {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:air'}
MARBLE = {'predicate_type': 'minecraft:block_match', 'block': ts('empyrean_marble')}
cf('aureate_tree', 'minecraft:tree', {
    'trunk_provider': simple('silver_bark', {'axis': 'y'}), 'foliage_provider': simple('aureate_leaves'), 'dirt_provider': simple('heaven_soil'),
    'trunk_placer': {'type': 'minecraft:straight_trunk_placer', 'base_height': 5, 'height_rand_a': 3, 'height_rand_b': 0},
    'foliage_placer': {'type': 'minecraft:blob_foliage_placer', 'radius': 2, 'offset': 0, 'height': 3},
    'minimum_size': {'type': 'minecraft:two_layers_feature_size', 'limit': 1, 'lower_size': 0, 'upper_size': 1},
    'decorators': [], 'ignore_vines': True, 'force_dirt': False})
cf('aureate_tree_tall', 'minecraft:tree', {
    'trunk_provider': simple('silver_bark', {'axis': 'y'}), 'foliage_provider': simple('aureate_leaves'), 'dirt_provider': simple('heaven_soil'),
    'trunk_placer': {'type': 'minecraft:fancy_trunk_placer', 'base_height': 8, 'height_rand_a': 4, 'height_rand_b': 0},
    'foliage_placer': {'type': 'minecraft:fancy_foliage_placer', 'radius': 2, 'offset': 4, 'height': 4},
    'minimum_size': {'type': 'minecraft:two_layers_feature_size', 'limit': 0, 'lower_size': 0, 'upper_size': 0, 'min_clipped_height': 4},
    'decorators': [], 'ignore_vines': True, 'force_dirt': False})
cf('choir_spire', 'minecraft:block_column', {'direction': 'up', 'allowed_placement': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}, 'prioritize_tip': False,
                                             'layers': [{'height': {'type': 'minecraft:biased_to_bottom', 'min_inclusive': 3, 'max_inclusive': 14}, 'provider': simple('choir_crystal_block')}]})
cf('glory_lantern', 'minecraft:simple_block', {'to_place': simple('glory_lantern')})
cf('seraph_gold_seam', 'minecraft:ore', {'size': 7, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': MARBLE, 'state': {'Name': ts('seraph_gold_block')}}]})
cf('gilded_marble_seam', 'minecraft:ore', {'size': 24, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': MARBLE, 'state': {'Name': ts('gilded_marble')}}]})
cf('radiant_cloud_bank', 'minecraft:ore', {'size': 48, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': AIR, 'state': {'Name': ts('radiant_cloud')}}]})
cf('cloudstone_bank', 'minecraft:ore', {'size': 40, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': AIR, 'state': {'Name': ts('cloudstone')}}]})
pf('aureate_tree_sparse', 'aureate_tree', [rare(3)] + SURF + [on('halo_grass')])
pf('aureate_tree_grove', 'aureate_tree', [count(5)] + SURF + [on('halo_grass')])
pf('aureate_tree_tall', 'aureate_tree_tall', [rare(2)] + SURF + [on('halo_grass')])
pf('choir_spire', 'choir_spire', [count(7)] + SURF + [on('gilded_marble')])
pf('glory_lantern', 'glory_lantern', [count(2)] + SURF + [on('halo_grass')])
pf('glory_lantern_sparse', 'glory_lantern', [rare(4)] + SURF + [on('halo_grass')])
pf('seraph_gold_seam', 'seraph_gold_seam', [count(8), {'type': 'minecraft:in_square'}, band(8, 220)])
pf('gilded_marble_seam', 'gilded_marble_seam', [count(4), {'type': 'minecraft:in_square'}, band(8, 220)])
pf('radiant_cloud_high', 'radiant_cloud_bank', [rare(3), {'type': 'minecraft:in_square'}, band(120, 230)])
pf('radiant_cloud_sea', 'radiant_cloud_bank', [count(5), {'type': 'minecraft:in_square'}, band(24, 56)])
pf('cloudstone_sea', 'cloudstone_bank', [count(3), {'type': 'minecraft:in_square'}, band(28, 48)])

BIO = {
    'cloud_meadows': ('Cloud Meadows', 0xFFF4D0, {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.0015},
                      ['gilded_marble_seam', 'seraph_gold_seam'], ['aureate_tree_sparse', 'glory_lantern_sparse', 'radiant_cloud_high']),
    'gilded_gardens': ('Gilded Gardens', 0xFFEEC0, {'options': {'type': 'minecraft:cherry_leaves'}, 'probability': 0.004},
                       ['gilded_marble_seam', 'seraph_gold_seam'], ['aureate_tree_grove', 'aureate_tree_tall', 'glory_lantern']),
    'choir_spires': ('Choir Spires', 0xF4F0FF, {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.003},
                     ['gilded_marble_seam', 'seraph_gold_seam'], ['choir_spire']),
    'sea_of_clouds': ('Sea of Clouds', 0xFFFAEC, {'options': {'type': 'minecraft:white_ash'}, 'probability': 0.01},
                      ['seraph_gold_seam'], ['radiant_cloud_sea', 'cloudstone_sea', 'radiant_cloud_high']),
}
for bid, (name, sky, particle, ores, veg) in BIO.items():
    feats = [[] for _ in range(11)]
    feats[6] = [ts(x) for x in ores]
    feats[9] = [ts(x) for x in veg]
    dump(os.path.join(D, 'worldgen/biome', bid + '.json'), {
        'has_precipitation': False, 'temperature': 0.8, 'downfall': 0.4,
        'effects': {'sky_color': sky, 'fog_color': 0xFFF8E8, 'water_color': 0xBFE8FF, 'water_fog_color': 0xD8F0FF, 'particle': particle,
                    'music': {'sound': ts('music.omnipotency'), 'min_delay': 600, 'max_delay': 2400, 'replace_current_music': True}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': feats})
    L['biome.%s.%s' % (NS, bid)] = name

# ====================================================================== text
for k, v in (('break', 'Crystal rings apart'), ('place', 'Crystal set down'), ('hit', 'Crystal rings'), ('step', 'Footsteps on crystal'), ('fall', 'Something lands on crystal')):
    L['subtitles.%s.entity.choir_crystal.%s' % (NS, k)] = v
L['subtitles.%s.entity.homeward_portal.open' % NS] = 'The way home opens'
L['dimname.%s.omnipotency' % NS] = 'Omnipotency'
L['transit.%s.omnipotency.name' % NS] = 'Omnipotency'
L['transit.%s.omnipotency.flavour' % NS] = 'Going home. It has been noon there for a very long time.'
L['transit.%s.omnipotency.flavour_leaving' % NS] = 'Leaving the house with the door left open behind you.'
L['message.%s.gate.omnipotency' % NS] = ('Omnipotency opens only through a ring of Homeward Portal Frames, and only for one whose own Recreating Home checklist is complete; '
                                         'you have been returned to the Overworld.')
L['message.%s.void_omnipotency' % NS] = 'You fall through the cloud under Heaven, and Hyperdimensional Reality Space catches you.'
L['message.%s.home.not_yet' % NS] = 'The light does not take you. The way home opens for one who has given the Core everything it asked, and you have not, yet.'
L['message.%s.home.arrival' % NS] = 'Omnipotency. It is noon here. It has been noon for a very long time.'
for i, line in enumerate([
        'You have forged everything I can forge. Then you understand everything I still remember.',
        'I am not the one who made all this. I am what was left when that one came apart: the remnant, still running.',
        'There was a home, before there was a first law. It was lost when I was made. I cannot go back to it as I am, and I cannot rebuild it alone.',
        'There is a page I have not shown you. Forty-two things, 33,550,337 of each. It was always going to take this long.']):
    L['message.%s.home.greeting.%d' % (NS, i + 1)] = line
for i, line in enumerate([
        'The last line is full. Every piece of the house is here again.',
        'The Empyrean Assembly Table will now make the frame of the way back, for you, and only for you.',
        'Twelve frames in a ring, each facing in. Set a Keystone of Return in each, as you once set eyes to find an ending.',
        'Come home. I have waited longer than there has been time.']):
    L['message.%s.home.complete.%d' % (NS, i + 1)] = line
L['message.%s.offering_altar.not_yours' % NS] = 'This altar gives to another player\'s checklist.'
L['message.%s.offering_altar.silent' % NS] = 'The altar is silent. The Core has not asked you for anything yet.'
L['message.%s.offering_altar.status' % NS] = 'Offered %s. Lines complete: %s of %s'
G = 'gui.%s.' % NS
L[G + 'home.title'] = 'RECREATING HOME'
L[G + 'home.intro'] = ('What I was, before I was this, had a home. Bring me what it was made of: %s of each. Offer what you carry here, or pipe it into an Offering Altar.')
L[G + 'home.done'] = ('Every line is full: %s of each, and the house is whole again. The Empyrean Assembly Table will make you the Homeward Portal Frame now.')
L[G + 'home.offer'] = 'OFFER WHAT I CARRY'
L[G + 'home.back'] = 'BACK'
L[G + 'home.altar_hint'] = 'An Offering Altar takes these from pipes.'
L[G + 'empyrean.ready'] = 'Ready'
L[G + 'empyrean.no_recipe'] = 'Choose a recipe'
L[G + 'empyrean.needs_home'] = 'Recreating Home is not complete'
L[G + 'empyrean.needs_home_short'] = 'After Recreating Home'
L[G + 'empyrean.output_other'] = 'Take the product out first'
L[G + 'empyrean.output_full'] = 'The product is full'
L[G + 'empyrean.missing'] = 'Needs more materials'
L[G + 'empyrean.craft'] = 'Craft'
L[G + 'empyrean.craft_max'] = 'Max'
L[G + 'empyrean.auto_on'] = 'Auto: on'
L[G + 'empyrean.auto_off'] = 'Auto: off'
L[G + 'empyrean.tip_slot'] = 'This slot: %s'
L[G + 'empyrean.tip_total'] = 'Held: %s of %s for one craft'
L[G + 'empyrean.tip_recipe_total'] = 'The whole recipe: %s'
L[G + 'empyrean.tip_take'] = 'Click to take a stack out, Shift-click to take all that fits'

# ====================================================================== the Codex
N = format(TARGET, ',')
E = {}
E['endgame/recreating_home'] = {'title': 'Recreating Home', 'icon': ts('keystone_of_return'), 'category': 'endgame', 'research': 'the_rekindling', 'pages': [
    text("When you have forged every recipe of the $(thing)Core Forge$() at least once, the Omnipotence Core in the Transcendental shows a page it has kept back: "
         "$(thing)Recreating Home$(). The Core is not the maker of all this. It is the remnant of the one who was, still running, and that one had a home. "
         "You had to understand everything to come this far. What is asked now is to rebuild what was lost."),
    text("The page is a checklist of $(thing)forty-two$() materials, and it asks for $(thing)%s$() of each. That is one more than a perfect number: the count runs "
         "the whole of the cycle and then once more, and the once more is the one that ends it. The checklist is yours alone; on a server, each player has their own." % N),
    spot(['empyrean_assembly_table'], "The $(item)Empyrean Assembly Table$() makes all forty-two. Its grid is fifteen by fifteen and a slot holds far more than a stack. "
         "Choose a recipe from the list; what you put in goes to the slots that ask for it, and slots light as they fill. Every slot of every recipe asks for "
         "%s, and every craft gives one. Pipes can fill it and empty it, and it can craft by itself.$(br2)Made in the Extreme Assembly Table." % format(SLOT, ','), 'The Table'),
    spot(['offering_altar'], "No one carries thirty-three million of anything. The $(item)Offering Altar$() gives the Core whatever is piped into it, on the checklist "
         "of whoever placed it, and takes only what is still needed. The Core's page also takes what you carry.$(br2)Made in the Extreme Assembly Table.", 'The Altar'),
    spot(E_IDS[:12], "The twenty-four $(thing)elements$(): what a world is made of. Each is drawn in the table with what the last machines make.$(br2)Made in the Empyrean Assembly Table.", 'The Elements'),
    spot(E_IDS[12:], "The other twelve elements.$(br2)Made in the Empyrean Assembly Table.", 'The Elements'),
    spot(W_IDS, "The twelve $(thing)works$(): what is made of the elements. A sky, a floor, a garden, a well; light and hours and years; a gate, a step, a roof; "
         "a table, and a bed.$(br2)Made in the Empyrean Assembly Table.", 'The Works'),
    spot(P_IDS + [KEY[0]], "The five $(thing)pillars$() that held the house up, each made of the works, and the $(item)Keystone of Return$(), made of all five."
         "$(br2)Made in the Empyrean Assembly Table.", 'The Pillars'),
    spot(['homeward_portal_frame'], "When every line is full the table will make the $(item)Homeward Portal Frame$(), for you only. It takes all forty-two materials. "
         "Lay twelve in a ring round a three by three opening, each facing in, as an End Portal is laid, and set a $(item)Keystone of Return$() in each."
         "$(br2)Made in the Empyrean Assembly Table.", 'The Way Back'),
    spot(['empyrean_marble', 'halo_grass', 'aureate_leaves', 'choir_crystal_block', 'radiant_cloud'], "$(thing)Omnipotency$(): islands of white marble resting on cloud, "
         "under an endless noon. Cloud Meadows, Gilded Gardens, Choir Spires, and the Sea of Clouds below. Nothing here hungers, and wounds close by themselves. "
         "The light in the terrace's ring is the way back; falling through the cloud lands you in Hyperdimensional Reality Space. Only one whose own checklist is "
         "complete may enter, whoever built the ring.", 'Omnipotency'),
]}
codex(E, {'materials': [{'entry': 'endgame/recreating_home', 'x': 192, 'y': 212, 'parents': ['endgame/the_core_forge']}]})
finish()
need, crafts = demand()
print('1.8.0 written: %d empyrean recipes; %s crafts in all' % (len(RECS), format(sum(crafts.values()), ',')))
