"""
Round 39 (1.2.6, "Field Sites"): the first structures of the mod - six Overworld sites from the 1.1 expansion design.

* Abandoned Observatory (mountains)      - astrophysics: mirror blanks, spectral plates, star charts.
* Survey Benchmark (anywhere)            - geography: a trig pillar and a barrel of survey gear.
* Crashed Satellite (plains, deserts)    - geography / electromagnetism: laminations, chips, solar cells.
* Magnetic Anomaly Site (rare)           - electromagnetism: a ring of lodestones round an iron knot.
* Fallout Shelter (underground)          - nuclear: lead-lined rooms, Geiger tubes, Prussian Blue.
* Probability Hall (inside villages)     - probability: a Galton Board to study, dice and tablets.

The templates are built here block by block and written as structure NBT files (gzip, written by a small NBT writer
below, so no extra Python package is needed). The first five are their own jigsaw structures; the Probability Hall is
added to the village house pools at server start (world/VillageHalls.java).
"""
import gzip
import io
import json
import math
import os
import random
import struct

NS = 'theoreticalsciences'
DATA_VERSION = 3955          # 1.21.1


# =================================================================================== blocks (decoration for the sites)
def _B(i, n, art, c1, c2, kind='deco', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=kw.pop('light', 0), hardness=kw.pop('hardness', 5.0), tool=kw.pop('tool', 'stone'))
    d.update(kw)
    return d


BLOCKS = [
    _B('magnetite_block', 'Block of Magnetite', 'crystalblock', '#1c1c20', '#686878', kind='simple'),
    _B('magnetite_bricks', 'Magnetite Bricks', 'bricks', '#222228', '#5a5a6a', deco=True, material='magnetite_block'),
    _B('lead_bricks', 'Lead Bricks', 'bricks', '#3a404c', '#8890a0', deco=True, material='lead_shielding_block'),
    _B('lead_hazard_panel', 'Lead Hazard Panel', 'hazard', '#3a404c', '#ffd040', deco=True, material='lead_shielding_block'),
    _B('radium_lamp', 'Radium Lamp', 'lamp', '#183018', '#80ff90', light=12, deco=True, material='lead_shielding_block'),
    _B('chance_tiles', 'Chance Tiles', 'tiles', '#202024', '#f0f0f0', deco=True, material='cherenkov_quartz_block'),
    _B('decorative_telescope', 'Decorative Telescope', 'plating', '#505868', '#c0c8d8', kind='deco_model', hardness=3.0),
    _B('satellite_wreck', 'Satellite Wreck', 'plating', '#606870', '#4060c0', kind='deco_model', hardness=3.0),
]
ITEMS, FLUIDS = [], []
MAGNETIC_EXTRA = [f'{NS}:magnetite_block', f'{NS}:magnetite_bricks']
SHIELDING_EXTRA = {f'{NS}:lead_bricks': (2.77, 0.1), f'{NS}:lead_hazard_panel': (2.77, 0.1), f'{NS}:radium_lamp': (1.5, 0.1),
                   f'{NS}:magnetite_block': (1.9, 0.3), f'{NS}:magnetite_bricks': (1.9, 0.3)}
CRAFTING = [
    ('magnetite_block', ['MMM', 'MMM', 'MMM'], {'M': 'magnetite'}, 'magnetite_block', 1),
    ('decorative_telescope', [' M ', 'ISI', 'I I'], {'M': 'telescope_mirror_blank', 'S': 'minecraft:spyglass', 'I': 'minecraft:iron_ingot'},
     'decorative_telescope', 1),
    ('satellite_wreck', ['LCL', 'EBE'], {'L': 'electrical_steel_lamination', 'C': 'random_number_chip', 'E': 'minecraft:copper_ingot',
                                         'B': 'minecraft:iron_block'}, 'satellite_wreck', 1),
]
SHAPELESS = [('magnetite_from_block', ['magnetite_block'], 'magnetite', 9)]


# =================================================================================== a minimal NBT writer
class _L(list):
    """An NBT list with an explicit element type id (for empty or typed lists)."""
    def __init__(self, tid, items=()):
        super().__init__(items)
        self.tid = tid


class Int(int):
    pass


def _tid(v):
    if isinstance(v, bool): return 1
    if isinstance(v, Int): return 3
    if isinstance(v, int): return 3
    if isinstance(v, float): return 6
    if isinstance(v, str): return 8
    if isinstance(v, _L): return 9
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(type(v))


def _w(out, v, tid):
    if tid == 1: out.write(struct.pack('>b', 1 if v else 0))
    elif tid == 3: out.write(struct.pack('>i', int(v)))
    elif tid == 6: out.write(struct.pack('>d', float(v)))
    elif tid == 8:
        b = v.encode('utf-8')
        out.write(struct.pack('>H', len(b)))
        out.write(b)
    elif tid == 9:
        et = v.tid if isinstance(v, _L) else (_tid(v[0]) if v else 0)
        out.write(struct.pack('>bi', et, len(v)))
        for x in v: _w(out, x, et)
    elif tid == 10:
        for k, x in v.items():
            t = _tid(x)
            out.write(struct.pack('>b', t))
            _w(out, k, 8)
            _w(out, x, t)
        out.write(b'\x00')


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(b'\x0a')
    _w(buf, '', 8)
    _w(buf, root, 10)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = gzip.compress(buf.getvalue(), mtime=0)
    if not os.path.exists(path) or open(path, 'rb').read() != data:
        open(path, 'wb').write(data)


# =================================================================================== templates
class T:
    def __init__(self, sx, sy, sz, seed):
        self.size = (sx, sy, sz)
        self.b = {}
        self.r = random.Random(seed)

    def set(self, x, y, z, name, props=None, nbt=None):
        if not (0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]):
            return
        self.b[(x, y, z)] = (name if ':' in name else 'minecraft:' + name, props or {}, nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, name, props)

    def chest(self, x, y, z, loot, facing='north', block='chest'):
        props = {'facing': facing, 'type': 'single', 'waterlogged': 'false'} if block == 'chest' else {'facing': facing, 'open': 'false'}
        self.set(x, y, z, block, props, {'id': 'minecraft:' + block, 'LootTable': f'{NS}:chests/{loot}'})

    def cache(self, x, y, z, loot, style='observatory_cache'):
        """A Containment Cache, like the 1.0 ruins', filled from a loot table when first opened."""
        self.set(x, y, z, f'{NS}:{style}', {'open': 'false'}, {'id': f'{NS}:containment_cache', 'LootTable': f'{NS}:chests/{loot}'})

    def save(self, path):
        palette, index, blocks = [], {}, []
        for (x, y, z), (name, props, nbt) in sorted(self.b.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                e = {'Name': name}
                if props: e['Properties'] = dict(props)
                palette.append(e)
            blk = {'pos': _L(3, [Int(x), Int(y), Int(z)]), 'state': Int(index[key])}
            if nbt: blk['nbt'] = nbt
            blocks.append(blk)
        write_nbt(path, {'DataVersion': Int(DATA_VERSION), 'size': _L(3, [Int(s) for s in self.size]), 'palette': _L(10, palette),
                         'blocks': _L(10, blocks), 'entities': _L(10, [])})


M = NS + ':'
PILLAR = {'axis': 'y'}


def observatory():
    """A research observatory gone quiet: spacetime-brick walls, a cracked field-glass dome with its slit, a telescope."""
    t = T(11, 12, 11, 1)
    c, R = 5, 5
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - c, z - c)
            if d > R + 0.4:
                continue
            t.set(x, 0, z, M + ('neutronium_tiles' if d < R - 1 else 'spacetime_bricks'))
            for y in range(1, 6):
                if d >= R - 0.6:
                    if y > 3 and t.r.random() < 0.2: t.set(x, y, z, 'air')          # weathered gaps near the top
                    else: t.set(x, y, z, M + t.r.choice(['spacetime_bricks', 'spacetime_bricks', 'neutronium_bricks']))
                else:
                    t.set(x, y, z, 'air')
            for y in range(6, 12):
                h = y - 5
                r_ = math.sqrt(max(0, R * R - h * h))
                if d <= r_ + 0.5:
                    shell = d >= r_ - 0.7 or y == 11
                    if shell and x != c and t.r.random() > 0.18:
                        t.set(x, y, z, M + 'containment_field_glass', {'formed': 'false'})
                    else:
                        t.set(x, y, z, 'air')
    for y in (1, 2): t.set(c, y, 10, 'air')
    t.set(c, 1, c, M + 'neutronium_pillar', PILLAR)
    t.set(c, 2, c, M + 'decorative_telescope', {'facing': 'north'})
    for x, z in ((1, c), (9, c), (c, 1)): t.set(x, 4, z, M + 'cherenkov_lamp')
    t.cache(2, 1, 3, 'observatory')
    t.cache(8, 1, 7, 'observatory_ruin')
    t.set(8, 1, 3, M + 'cherenkov_bricks')
    return t


def benchmark():
    """A survey trig point: a neutronium pillar on a tiled plinth, its brass mark a Cherenkov lamp, and a cache of gear."""
    t = T(5, 5, 5, 2)
    t.fill(0, 0, 0, 4, 0, 4, M + 'neutronium_tiles')
    t.fill(1, 1, 1, 3, 1, 3, M + 'neutronium_plating')
    for y in (2, 3): t.set(2, y, 2, M + 'neutronium_pillar', PILLAR)
    t.set(2, 4, 2, M + 'cherenkov_lamp')
    t.cache(0, 1, 2, 'survey_benchmark')
    return t


def satellite():
    """A survey satellite come down hard: the wreck, its torn panels and spars, in a scorched ring."""
    t = T(11, 4, 11, 3)
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - 5, z - 5)
            if d <= 5.2:
                if d < 3.2 or t.r.random() < 0.35:
                    t.set(x, 0, z, M + t.r.choice(['chronosteel_vent', 'cosmic_plating', 'chronosteel_plating']))
                for y in range(1, 4): t.set(x, y, z, 'air')
    t.set(5, 1, 5, M + 'satellite_wreck', {'facing': 'east'})
    t.set(4, 1, 5, M + 'satellite_wreck', {'facing': 'west'})
    for x in range(0, 3): t.set(x, 1, 5, M + 'computronium_panel')       # one solar wing still attached...
    for x, z in ((8, 3), (9, 7)): t.set(x, 1, z, M + 'computronium_panel')  # ...the other in pieces
    t.set(6, 1, 7, M + 'chronosteel_pillar', {'axis': 'x'})
    t.set(7, 1, 7, M + 'chronosteel_pillar', {'axis': 'x'})
    t.set(5, 2, 5, M + 'chronosteel_hazard_panel')
    t.cache(5, 1, 3, 'crashed_satellite')
    return t


def magnetic_anomaly():
    """A buried knot of magnetite ringed by standing magnetite blocks: the Gauss Meter reads it from well away."""
    t = T(9, 4, 9, 4)
    for x in range(9):
        for z in range(9):
            d = math.hypot(x - 4, z - 4)
            if d <= 4.3:
                t.set(x, 0, z, M + ('magnetite_bricks' if d < 2 else 'magnetite_block' if t.r.random() < 0.25 else 'neutronium_tiles'))
                for y in range(1, 4): t.set(x, y, z, 'air')
    for k in range(8):
        a = k * math.pi / 4
        x, z = int(round(4 + 3 * math.cos(a))), int(round(4 + 3 * math.sin(a)))
        t.set(x, 1, z, M + 'magnetite_block')
        if k % 2 == 0: t.set(x, 2, z, M + 'magnetite_block')
    t.fill(3, 1, 3, 5, 1, 5, M + 'magnetite_bricks')
    t.set(4, 2, 4, M + 'magnetite_block')
    t.cache(4, 1, 6, 'magnetic_anomaly')
    return t


def fallout_shelter():
    """Two lead-lined rooms deep underground: hazard panels, radium lamps, and caches of supplies."""
    t = T(11, 6, 13, 5)
    t.fill(0, 0, 0, 10, 5, 12, M + 'lead_bricks')
    t.fill(1, 5, 1, 9, 5, 11, M + 'lead_shielding_block')
    t.fill(1, 1, 1, 9, 4, 11, 'air')
    t.fill(1, 1, 6, 9, 4, 6, M + 'lead_bricks')                   # two rooms, a doorway between
    t.fill(4, 1, 6, 6, 3, 6, 'air')
    for z in (0, 12):
        for x in (2, 5, 8): t.set(x, 2, z, M + 'lead_hazard_panel')
    for x, z in ((5, 3), (5, 9)): t.set(x, 5, z, M + 'radium_lamp')
    t.fill(1, 1, 1, 3, 1, 1, M + 'lead_shielding_block')           # bunks: shielding slabs along the wall
    t.fill(1, 1, 3, 3, 1, 3, M + 'lead_shielding_block')
    t.cache(8, 1, 1, 'fallout_shelter')
    t.cache(9, 1, 10, 'fallout_shelter_stores')
    t.cache(9, 1, 9, 'fallout_shelter_stores')
    t.set(1, 1, 11, M + 'chronosteel_vent')
    return t


def probability_hall():
    """A village's hall of chance: chance-tiled floor, a Galton Board to study, and a cache of dice and tablets."""
    t = T(9, 7, 9, 6)
    t.fill(0, 0, 0, 8, 0, 8, M + 'chance_tiles')
    for y in range(1, 5):
        for x in range(9):
            for z in range(9):
                edge = x in (0, 8) or z in (0, 8)
                corner = x in (0, 8) and z in (0, 8)
                if corner: t.set(x, y, z, M + 'cherenkov_pillar', PILLAR)
                elif edge: t.set(x, y, z, M + 'hex_glass' if y in (2, 3) and (x + z) % 2 == 1 else M + 'neutronium_plating',
                                 {'formed': 'false'} if y in (2, 3) and (x + z) % 2 == 1 else None)
                else: t.set(x, y, z, 'air')
    t.fill(0, 5, 0, 8, 5, 8, M + 'neutronium_tiles')
    t.fill(2, 5, 2, 6, 5, 6, M + 'cherenkov_lamp')
    # the door on the south side, and the jigsaw that joins it to the village street
    t.set(4, 1, 8, 'air')
    t.set(4, 2, 8, 'air')
    t.set(4, 0, 8, 'jigsaw', {'orientation': 'south_up'},
          {'id': 'minecraft:jigsaw', 'name': 'minecraft:building_entrance', 'target': 'minecraft:empty', 'pool': 'minecraft:empty',
           'joint': 'rigid', 'final_state': M + 'chance_tiles', 'selection_priority': Int(0), 'placement_priority': Int(0)})
    t.set(4, 1, 2, M + 'galton_board', {'facing': 'south', 'active': 'false'})
    t.cache(2, 1, 1, 'probability_hall')
    return t


# =================================================================================== worldgen
# id: (builder, biome tag values, spacing, separation, salt, step, start height, terrain adaptation)
SITES = {
    'abandoned_observatory': (observatory, ['#minecraft:is_mountain'], 40, 14, 38_101, 'surface_structures', None, 'beard_thin'),
    'survey_benchmark': (benchmark, ['#minecraft:is_overworld'], 28, 10, 38_102, 'surface_structures', None, 'beard_thin'),
    'crashed_satellite': (satellite, ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:desert', 'minecraft:savanna',
                                      'minecraft:savanna_plateau', 'minecraft:badlands'], 48, 16, 38_103, 'surface_structures', None, 'beard_thin'),
    'magnetic_anomaly_site': (magnetic_anomaly, ['#minecraft:is_overworld'], 64, 24, 38_104, 'surface_structures', None, 'beard_thin'),
    'fallout_shelter': (fallout_shelter, ['#minecraft:is_overworld'], 40, 14, 38_105, 'underground_structures', (-20, 30), 'none'),
}
LOOT = {
    # (item, min, max, weight) - this mod's own materials only, as in the 1.0 ruins
    'observatory': [('telescope_mirror_blank', 1, 2, 10), ('spectral_plate', 1, 3, 10), ('theodolite_lens', 1, 1, 5), ('cherenkov_quartz', 2, 6, 10),
                    ('quantum_dot', 1, 3, 6), ('survey_map', 1, 2, 6), ('decorative_telescope', 1, 1, 2), ('spectral_analyzer', 1, 1, 2)],
    'survey_benchmark': [('survey_map', 1, 2, 10), ('theodolite_lens', 1, 1, 5), ('sediment_core', 1, 3, 8), ('geoid_model', 1, 1, 3),
                         ('geodetic_surveyor', 1, 1, 1), ('cherenkov_quartz', 1, 4, 8), ('seismic_recorder_drum', 1, 2, 4)],
    'crashed_satellite': [('electrical_steel_lamination', 2, 6, 10), ('random_number_chip', 1, 2, 6), ('ferrite_magnet', 1, 3, 6),
                          ('quantum_dot', 1, 4, 8), ('neutronium_ingot', 1, 3, 6), ('geiger_tube', 1, 1, 4), ('satellite_wreck', 1, 1, 2),
                          ('spectral_plate', 1, 2, 5)],
    'magnetic_anomaly': [('magnetite', 2, 6, 12), ('ferrite_magnet', 1, 3, 8), ('magnetite_block', 1, 2, 4), ('neodymium_blank', 1, 1, 3),
                         ('rare_earth_oxide', 1, 3, 5), ('gauss_meter', 1, 1, 2), ('samarium_cobalt_blank', 1, 1, 1)],
    'fallout_shelter': [('lead_shielding_plate', 2, 6, 10), ('geiger_tube', 1, 2, 8), ('geiger_counter', 1, 1, 2), ('prussian_blue_tablet', 1, 4, 10),
                        ('radium_salt', 1, 1, 2), ('raw_pitchblende', 1, 4, 6), ('radiogenic_lead', 1, 4, 6)],
    'fallout_shelter_stores': [('prussian_blue_tablet', 1, 3, 8), ('lead_shielding_plate', 1, 3, 6), ('radiogenic_lead', 1, 3, 6),
                               ('neutronium_ingot', 1, 2, 4), ('cherenkov_quartz', 2, 6, 8), ('radium_lamp', 1, 2, 3)],
    'probability_hall': [('dice_set', 1, 1, 8), ('random_number_chip', 1, 2, 8), ('prior_tablet', 1, 3, 8), ('posterior_tablet', 1, 1, 3),
                         ('normal_distribution_crystal', 1, 1, 2), ('quantum_dot', 1, 3, 6), ('chance_tiles', 4, 12, 5)],
}
VILLAGE_POOLS = ['plains', 'desert', 'savanna', 'snowy', 'taiga']

LANG = {
    'message.theoreticalsciences.variance_hall': 'The odds in this hall feel thinner than they should.',
}


def _extend(p, ids, w):
    t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    for i in ids:
        if i not in t['values']: t['values'].append(i)
    w(p, t)


def _box(fr, to, tex, rot=None):
    e = {'from': fr, 'to': to, 'faces': {f: {'texture': tex} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if rot: e['rotation'] = rot
    return e


def _deco_models(g):
    """The two decorative instruments' models, overriding the plain cube gen_data gives every block."""
    A, w = g['A'], g['w']
    tel = {'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:block/decorative_telescope', 'tube': f'{NS}:block/decorative_telescope',
                                                           'leg': f'{NS}:block/neutronium_plating', 'lens': f'{NS}:block/containment_field_glass'},
           'elements': [_box([7, 0, 3], [8, 8, 4], '#leg'), _box([8, 0, 3], [9, 8, 4], '#leg'), _box([7.5, 0, 12], [8.5, 8, 13], '#leg'),
                        _box([3, 0, 7.5], [4, 8, 8.5], '#leg'), _box([12, 0, 7.5], [13, 8, 8.5], '#leg'),
                        _box([6, 7, 6], [10, 9, 10], '#leg'),
                        _box([6.5, 9, 1], [9.5, 12, 15], '#tube', {'origin': [8, 10, 8], 'axis': 'x', 'angle': -22.5}),
                        _box([6.8, 9.3, 0.8], [9.2, 11.7, 1], '#lens', {'origin': [8, 10, 8], 'axis': 'x', 'angle': -22.5}),
                        _box([7.2, 12, 10], [8.8, 13.5, 13], '#leg', {'origin': [8, 10, 8], 'axis': 'x', 'angle': -22.5})]}
    sat = {'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:block/satellite_wreck', 'bus': f'{NS}:block/satellite_wreck',
                                                           'panel': f'{NS}:block/computronium_panel', 'rod': f'{NS}:block/chronosteel_plating'},
           'elements': [_box([4, 0, 4], [12, 7, 12], '#bus', {'origin': [8, 4, 8], 'axis': 'z', 'angle': 22.5}),
                        _box([0, 3, 6], [4, 4, 10], '#panel', {'origin': [8, 4, 8], 'axis': 'z', 'angle': 22.5}),
                        _box([12, 3, 6], [16, 4, 10], '#panel', {'origin': [8, 4, 8], 'axis': 'z', 'angle': -22.5}),
                        _box([7.5, 7, 7.5], [8.5, 15, 8.5], '#rod', {'origin': [8, 7, 8], 'axis': 'x', 'angle': 22.5}),
                        _box([6, 7, 6], [10, 8, 10], '#rod')]}
    for bid, m in (('decorative_telescope', tel), ('satellite_wreck', sat)):
        w(os.path.join(A, 'models', 'block', bid + '.json'), m)
        w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {f'facing={f}': ({'model': f'{NS}:block/{bid}', 'y': y} if y else
                                                                                      {'model': f'{NS}:block/{bid}'})
                                                                       for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
        w(os.path.join(A, 'models', 'item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})


def _q(x):
    return x if ':' in x else f'{NS}:{x}'


def apply(g):
    D, w = g['D'], g['w']
    for sid, (fn, biomes, spacing, sep, salt, step, height, adapt) in SITES.items():
        fn().save(os.path.join(D, 'structure', sid + '.nbt'))
        w(os.path.join(D, 'tags', 'worldgen', 'biome', 'has_structure', sid + '.json'), {'replace': False, 'values': biomes})
        w(os.path.join(D, 'worldgen', 'template_pool', sid, 'start.json'), {
            'fallback': 'minecraft:empty',
            'elements': [{'weight': 1, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{sid}',
                                                   'projection': 'rigid', 'processors': 'minecraft:empty'}}]})
        s = {'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{sid}', 'step': step, 'spawn_overrides': {},
             'terrain_adaptation': adapt, 'start_pool': f'{NS}:{sid}/start', 'size': 1, 'max_distance_from_center': 80,
             'use_expansion_hack': False}
        if height:
            s['start_height'] = {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': height[0]}, 'max_inclusive': {'absolute': height[1]}}
        else:
            s['start_height'] = {'absolute': 0}
            s['project_start_to_heightmap'] = 'WORLD_SURFACE_WG'
        w(os.path.join(D, 'worldgen', 'structure', sid + '.json'), s)
        w(os.path.join(D, 'worldgen', 'structure_set', sid + '.json'), {
            'structures': [{'structure': f'{NS}:{sid}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': sep, 'salt': salt}})
    # 1.1.0 (audit): the hall rebuilt in 1.10.0 (round51, structure/probability_hall.nbt) is the one villages use; this first
    # version is no longer saved, so villages don't get two different halls.
    old = os.path.join(D, 'structure', 'village', 'probability_hall.nbt')
    if os.path.exists(old):
        os.remove(old)
    # the decoration blocks: recipes, the magnet tag, shielding values, pickaxe
    for name, pat, key, res, n in CRAFTING: g['shaped']('r39_' + name, pat, key, res, n)
    for name, items, res, n in SHAPELESS: g['shapeless']('r39_' + name, items, res, n)
    import round38
    _extend(os.path.join(D, 'tags', 'block', 'magnetic.json'), MAGNETIC_EXTRA, w)
    sh = dict(round38.SHIELDING)
    sh.update(SHIELDING_EXTRA)
    w(os.path.join(D, 'data_maps', 'block', 'radiation_shielding.json'), {'values': {k: {'gamma': a_, 'neutron': b_} for k, (a_, b_) in sh.items()}})
    _extend(os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'), [f'{NS}:{b_["id"]}' for b_ in BLOCKS], w)
    for name, entries in LOOT.items():
        w(os.path.join(D, 'loot_table', 'chests', name + '.json'), {
            'type': 'minecraft:chest',
            'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6},
                       'entries': [{'type': 'minecraft:item', 'name': _q(i), 'weight': wt,
                                    'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
                                   for i, lo, hi, wt in entries]}]})
    g['LANG'].update(LANG)


def finish(g):
    _deco_models(g)
    te = g['text_entry']
    te('geography', 'r39_sites', 'Field Sites', 'decorative_telescope', [
        'Scientists worked across the Overworld before you, and their field sites still stand - built, like the old observatory ruins, from '
        'the mod\'s own materials, with Containment Caches of what they left behind.',
        '$(thing)Survey Benchmarks$() - a neutronium pillar topped with a Cherenkov lamp - stand in every kind of land. $(thing)Observatories$() '
        'keep their cracked field-glass domes high in the mountains, a telescope still aimed through the slit.',
        '$(thing)Crashed Satellites$() lie in scorched rings on plains, savannas, deserts and badlands. $(thing)Magnetic Anomaly Sites$() are '
        'rare: standing stones of magnetite round a buried knot of it, which the Gauss Meter reads from well away.',
        '$(thing)Fallout Shelters$() are sealed, lead-lined rooms deep underground, lit by radium lamps. They have no door to the surface: a '
        'Seismograph, which reports caves below it, is the way to find one.',
        'Some villages keep a $(thing)Probability Hall$(): a floor of Chance Tiles, a Galton Board to study and a cache of dice and tablets.',
        'The sites\' blocks can be made too: $(item)Magnetite$() and $(item)Lead$() bricks, Lead Hazard Panels, Radium Lamps, Chance Tiles, and a '
        '$(item)Decorative Telescope$() and $(item)Satellite Wreck$() - decoration only; working instruments come later.'],
        adv='cartography', sort=5, spot=['decorative_telescope', 'satellite_wreck'])
