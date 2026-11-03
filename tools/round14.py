"""Round 14: containment caches (the mod's own loot storage for ruins), the attunement protection hierarchy, and space
distortion inside higher-dimensional structures.

    python3 round14.py tex      -> draws the cache textures (16 animated frames each)

apply()/finish() are called from gen_data.py.
"""
import json
import math
import os

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', NS)

# id, name, body colour, glow colour, emblem, flavour
CACHES = [
    ('observatory_cache', 'Containment Cache', '#3a4250', '#60e0ff', 'lens',
     'A sealed stasis locker. Whoever left it here wanted what is inside to stay exactly as it was.'),
    ('bulk_cache', 'Bulk Cache', '#1c1230', '#b070ff', 'tesseract',
     'Bigger on the inside - it borrows a little room from a fifth dimension.'),
    ('euclid_reliquary', 'Euclid Reliquary', '#d8d0b8', '#ffc840', 'triangle',
     'A reliquary of perfect proportions. Its lid closes on an exact right angle.'),
    ('foam_cache', 'Foam Cache', '#0e1c2a', '#80ffe0', 'bubbles',
     'Held together by a bubble of stable vacuum. Do not leave it open long.'),
    ('hilbert_locker', 'Hilbert Locker', '#2a1e14', '#ffb060', 'rooms',
     'Always full, and always room for one more.'),
    ('horizon_cache', 'Horizon Cache', '#060606', '#ff7a30', 'ring',
     'Its lid is an event horizon held open. What goes in stays in - until you reach for it.'),
    ('ejecta_cache', 'Ejecta Cache', '#e6eaf6', '#70b0ff', 'star',
     'Thrown out of a white hole with everything else. It has never been opened from outside.'),
    ('hyperreal_cache', 'Hyperreal Cache', '#0e2416', '#80ff90', 'wave',
     'Its walls obey whatever law is currently in force - the contents do not.'),
    ('transcendent_cache', 'Transcendent Cache', '#f6efd8', 'rainbow', 'spiral',
     'A box beyond every law, holding what was left when the last law was gone.'),
    ('antiverse_cache', 'Antiverse Cache', '#3a2418', '#ffb070', 'mirror',
     'Made of antimatter and kept levitating in its own trap. Never touch it with your bare hands - the trap does.'),
]

BLOCKS = [dict(id=i, name=n, art='bricks', c1=c1, c2=(c2 if c2 != 'rainbow' else '#ffd0ff'), kind='cache', light=4, hardness=3.0, tool='iron')
          for i, n, c1, c2, _, _ in CACHES]

# (attunement item, the place it is made for), in the order they are earned - must match DimensionLaws.TIERS
TIERS = [('form_of_the_self', 'the Platonic Realm'), ('penning_harness', 'the Antiverse'), ('coherence_stabiliser', 'the Quantum Foam'),
         ('brane_tether', 'the Bulk'), ('geodesic_anchor', 'black and white holes'), ('well_ordering_charm', 'the Transfinite Hierarchy'),
         ('primordial_charter', 'Hyperdimensional Reality'), ('identity_kernel', 'the Transcendental')]

LANG = {
    'message.theoreticalsciences.law_shielded': 'A stronger attunement holds this place\'s laws away from you.',
    'theoreticalsciences.configuration.structureDistortion': 'Structure Space Distortion',
    'puzzle.theoreticalsciences.review_note': 'Researched - your derivation, kept for reference.',
    'gui.theoreticalsciences.review_tip': 'Click to read its derivation again.',
}


def shield_text(i):
    if i == len(TIERS) - 1:
        return 'Also shields you completely in every other dimension.'
    earlier = [p for _, p in TIERS[:i]]
    if not earlier:
        return None
    s = earlier[0] if len(earlier) == 1 else ', '.join(earlier[:-1]) + ' and ' + earlier[-1]
    return 'Also shields you completely in ' + s + '.'


def apply(g):
    q, shaped, LANG_ = g['q'], g['shaped'], g['LANG']
    LANG_.update(LANG)
    for i, n, _, _, _, lore in CACHES:
        LANG_[f'block.{NS}.{i}'] = n
    shaped('observatory_cache', ['CHC', 'S S', 'CHC'], {'C': 'superconducting_coil', 'H': 'hyperframe_casing', 'S': 'minecraft:chest'}, 'observatory_cache')
    # any cache can be re-sealed in a dimension's style with a block from that dimension
    shapeless = g['shapeless']
    for cid, mat in (('bulk_cache', 'bulk_crystal'), ('euclid_reliquary', 'golden_ratio_glass'), ('foam_cache', 'foam_substrate'), ('hilbert_locker', 'hilbert_brick'),
                     ('horizon_cache', 'infall_rock'), ('ejecta_cache', 'ejecta_rock'), ('hyperreal_cache', 'arbor_heartwood'), ('transcendent_cache', 'transcendent_stone'),
                     ('antiverse_cache', 'antistone')):
        shapeless(cid, ['observatory_cache', mat], cid)
    for k, (iid, _) in enumerate(TIERS):
        t = shield_text(k)
        if t:
            LANG_[f'item.{NS}.{iid}.shield'] = t


def finish(g):
    w, q, LANG_, text_entry = g['w'], g['q'], g['LANG'], g['text_entry']
    A = g['A']
    for i, *_ in CACHES:
        for v in ('', '_open'):
            w(os.path.join(A, 'models/block', i + v + '.json'), {
                'parent': 'minecraft:block/cube_bottom_top',
                'textures': {'top': f'{NS}:block/{i}_top{v}', 'bottom': f'{NS}:block/{i}_bottom', 'side': f'{NS}:block/{i}', 'particle': f'{NS}:block/{i}'}})
        w(os.path.join(A, 'blockstates', i + '.json'), {'variants': {'open=false': {'model': f'{NS}:block/{i}'}, 'open=true': {'model': f'{NS}:block/{i}_open'}}})
        w(os.path.join(A, 'models/item', i + '.json'), {'parent': f'{NS}:block/{i}'})
    # ---- codex
    text_entry('world', 'containment_caches', 'Containment Caches', 'observatory_cache', [
        'Ruins and impossible structures keep their treasure in $(item)Containment Caches$(), not chests. Each dimension seals its own kind: the Bulk Cache borrows room from a fifth dimension, the Euclid Reliquary of the Platonic Realm closes on an exact right angle, the Hilbert Locker is always full and always has room for one more.',
        'A cache works like a chest of 27 slots; hoppers and pipes can reach it. Breaking it spills its contents and drops the cache itself. You can make an ordinary $(item)Containment Cache$() of your own.'],
        sort=40, spot=['observatory_cache', 'bulk_cache', 'euclid_reliquary', 'hilbert_locker', 'horizon_cache', 'transcendent_cache'])
    text_entry('world', 'impossible_interiors', 'Impossible Interiors', 'tesseract_core', [
        'Stand inside a structure built in more dimensions than three and your eyes stop agreeing with your feet. The deeper you go, the more space folds to match the object you are in:',
        '$(li)$(thing)Tesseracts$() and Bulk monoliths: nested cubes turning inside one another$(li)$(thing)Fractals$(): the view contains itself, again and again$(li)$(thing)Platonic solids$(): mirrored through their symmetry$(li)$(thing)Hilbert\'s Hotel$() and the Ordinal Spire: an endless corridor$(li)$(thing)Calabi-Yau knots$() and Hopf rings: space wound round itself',
        '$(li)$(thing)Graviton reefs$(): light bent into an Einstein ring$(li)$(thing)Anomaly vaults$(): the picture tears$(li)the heart of the $(thing)Transcendental$(): all of it at once.$(br2)It is harmless, and fades as you walk out. It can be switched off in the client config (structureDistortion).'],
        sort=41)
    lines = []
    for k, (iid, place) in enumerate(TIERS):
        lines.append(f'$(li)$(item){LANG_.get(f"item.{NS}.{iid}", iid)}$(): {place}')
    text_entry('world', 'attunement_hierarchy', 'The Attunement Hierarchy', 'primordial_charter', [
        'Every strange dimension has the item that lets you survive it. Each one was made from what you brought back from the places before it - so it carries their protection too.$(br2)Carry an attunement item and every dimension whose own item comes $(l)earlier$() in this list cannot touch you at all: no debuffs, no strange gravity, no rules.',
        ''.join(lines[:4]),
        ''.join(lines[4:]) + '$(br2)So the $(item)Primordial Charter$() shields you completely in every place before Hyperdimensional Reality, does its usual job inside Hyperdimensional Reality itself, and only the Transcendental still needs the $(item)Identity Kernel$(). In its own dimension, each item works as it always has.'],
        sort=42, spot=['primordial_charter'])


# ======================================================================================= textures
def _hex(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def _mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def _hsv(h, s, v):
    import colorsys
    r, g_, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return int(r * 255), int(g_ * 255), int(b * 255)


def _emblem(kind, x, y, f):
    """Strength 0..1 of the emblem at pixel (x, y) of a 16x16 side (emblem centred on 8, 10.5)."""
    cx, cy = 7.5, 10.5
    dx, dy = x - cx, y - cy
    r = math.hypot(dx, dy)
    a = math.atan2(dy, dx)
    ph = f / 16 * math.tau
    if kind == 'lens':
        return max(1.0 if 2.2 < r < 3.4 else 0.0, 1.0 if r < 1.2 else 0.0)
    if kind == 'tesseract':
        m = max(abs(dx), abs(dy))
        s = (f / 16) * 1.5
        return 1.0 if (abs(m - 3.5) < 0.6 or abs(m - (1.5 + s)) < 0.6 or (abs(abs(dx) - abs(dy)) < 0.6 and 1.5 < m < 3.5)) else 0.0
    if kind == 'triangle':
        if dy > 3 or dy < -3.5: return 0.0
        half = (dy + 3.5) / 6.5 * 3.6
        inside = abs(dx) <= half
        edge = inside and (abs(dx) > half - 1.0 or dy > 2.2)
        return 1.0 if edge else 0.0
    if kind == 'bubbles':
        v = 0.0
        for bx, by, br, sp in ((5.5, 11.5, 1.8, 0), (9.5, 9.5, 2.2, 5), (8, 13, 1.0, 9)):
            yy = by - ((f + sp) % 16) / 16 * 2
            d = math.hypot(x - bx, y - yy)
            if abs(d - br) < 0.55: v = 1.0
        return v
    if kind == 'rooms':
        if 7 <= y <= 14 and 2 <= x <= 13:
            col = (x - 2) // 3
            door = (x - 2) % 3 == 1 and y >= 10
            lit = (col + f // 4) % 4 == 0
            return (1.0 if lit else 0.45) if door else 0.0
        return 0.0
    if kind == 'ring':
        ring = 1.0 if 2.6 < r < 3.6 else 0.0
        bright = 0.5 + 0.5 * math.cos(a - ph)
        return ring * (0.4 + 0.6 * bright) if r >= 1.2 else 0.0
    if kind == 'star':
        rays = abs(math.cos(2 * (a + ph / 4)))
        return 1.0 if r < 1.2 else (rays ** 6 if r < 4 else 0.0)
    if kind == 'wave':
        wy = cy + 1.6 * math.sin(x * 0.9 + ph)
        return 1.0 if abs(y - wy) < 0.7 and 1 <= x <= 14 else 0.0
    if kind == 'spiral':
        if r > 3.8 or r < 0.5: return 1.0 if r < 0.5 else 0.0
        t = (a + ph) / math.tau - r / 2.0
        return 1.0 if abs((t % 0.5) - 0.25) < 0.09 else 0.0
    if kind == 'mirror':
        left = dx < 0
        d = math.hypot(dx + (1.2 if left else -1.2), dy)
        return 1.0 if abs(d - 2.2) < 0.55 else (0.6 if abs(dx) < 0.4 and r < 4 else 0.0)
    return 0.0


def _frame(cid, part, body, glow, emblem, f):
    from PIL import Image
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    pulse = 0.65 + 0.35 * math.sin(f / 16 * math.tau)
    rainbow = glow == 'rainbow'

    def G(x, y):
        if rainbow: return _hsv(f / 16 + (x + y) / 40, 0.55, 1.0)
        return _hex(glow)
    B = _hex(body)
    lum = sum(B) / 3
    dark = _mix(B, (0, 0, 0), 0.45)
    light = _mix(B, (255, 255, 255), 0.25 if lum < 128 else 0.12)
    for y in range(16):
        for x in range(16):
            n = ((x * 7 + y * 13) * 2654435761 >> 7) % 9 / 9 - 0.5   # stable grain
            c = _mix(B, (255, 255, 255) if n > 0 else (0, 0, 0), abs(n) * 0.18)
            edge = x in (0, 15) or y in (0, 15)
            if edge: c = dark
            elif x in (1, 14) or y in (1, 14): c = light if (x == 1 or y == 1) else _mix(B, dark, 0.5)
            if part == 'side':
                corner = (x <= 3 or x >= 12) and (y <= 3 or y >= 12) and not edge
                if corner: c = _mix(c, G(x, y), 0.18)
                if y == 6 and 1 <= x <= 14: c = _mix(dark, G(x, y), pulse)             # the lid seam, glowing
                elif y == 5 and 1 <= x <= 14: c = _mix(c, dark, 0.6)
                e = _emblem(emblem, x, y, f)
                if e > 0 and 2 <= x <= 13 and 7 <= y <= 14: c = _mix(c, G(x, y), e * (0.55 + 0.45 * pulse))
                if 6 <= x <= 9 and 3 <= y <= 4: c = _mix(dark, G(x, y), 0.35 + 0.3 * pulse)  # latch
            elif part == 'top':
                r = math.hypot(x - 7.5, y - 7.5)
                if 4.2 < r < 5.6:
                    ang = (math.atan2(y - 7.5, x - 7.5) / math.tau * 12 + f * 0.75) % 1
                    c = _mix(c, G(x, y), (0.3 + 0.7 * pulse) * (1.0 if ang < 0.45 else 0.25))
                if r < 1.6: c = _mix(c, G(x, y), pulse)
            elif part == 'top_open':
                if 2 <= x <= 13 and 2 <= y <= 13:
                    depth = min(x - 2, 13 - x, y - 2, 13 - y)
                    c = _mix((8, 8, 12), G(x, y), (0.25 + 0.55 * pulse) * (1 - depth / 6))
                    if depth >= 4: c = _mix(c, (255, 255, 255), 0.25 * pulse)
            elif part == 'bottom':
                if 4 <= x <= 11 and 4 <= y <= 11 and (x in (4, 11) or y in (4, 11)): c = _mix(c, G(x, y), 0.3)
            px[x, y] = c + (255,)
    return im


def textures():
    from PIL import Image
    out = os.path.join(ASSETS, 'textures', 'block')
    os.makedirs(out, exist_ok=True)
    for cid, _, body, glow, emblem, _ in CACHES:
        for part, name in (('side', cid), ('top', cid + '_top'), ('top_open', cid + '_top_open'), ('bottom', cid + '_bottom')):
            strip = Image.new('RGBA', (16, 256))
            for f in range(16):
                strip.paste(_frame(cid, part, body, glow, emblem, f), (0, f * 16))
            strip.save(os.path.join(out, name + '.png'))
            with open(os.path.join(out, name + '.png.mcmeta'), 'w') as fh:
                json.dump({'animation': {'frametime': 3, 'interpolate': True}}, fh)
    print('round14 textures:', len(CACHES) * 4)


if __name__ == '__main__':
    import sys
    if 'tex' in sys.argv:
        textures()
