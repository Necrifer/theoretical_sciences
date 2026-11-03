"""Decorative block families. Run once to (re)write the deco entries in content.json."""
import json, os

ROOT = os.path.dirname(os.path.abspath(__file__))

# family: (material block for the stonecutter, ingot that crafts the material block or None, c1, c2, variants)
# variant: (suffix, art, shape, light)
FAMILIES = {
    'neutronium': ('neutronium_block', None, '#5a6070', '#c8d4ff', [
        ('plating', 'plating', 'cube', 0), ('tiles', 'tiles', 'cube', 0), ('bricks', 'bricks', 'cube', 0), ('pillar', 'pillar', 'pillar', 0)]),
    'cherenkov': ('cherenkov_quartz_block', None, '#1a4cbf', '#8fe3ff', [
        ('polished', 'polished', 'cube', 6), ('bricks', 'bricks', 'cube', 4), ('pillar', 'pillar', 'pillar', 4), ('lamp', 'lamp', 'cube', 15)]),
    'chronosteel': ('chronosteel_block', 'chronosteel_ingot', '#8a6a2a', '#ffe0a0', [
        ('plating', 'plating', 'cube', 0), ('hazard_panel', 'hazard', 'cube', 0), ('vent', 'vent', 'cube', 2), ('pillar', 'pillar', 'pillar', 0)]),
    'spacetime': ('spacetime_alloy_block', 'spacetime_alloy_ingot', '#26305e', '#a0c0ff', [
        ('panel', 'stars', 'cube', 4), ('tiles', 'tiles', 'cube', 2), ('bricks', 'bricks', 'cube', 2)]),
    'cosmic': ('cosmic_neutronium_block', 'cosmic_neutronium_ingot', '#14141c', '#ffffff', [
        ('tiles', 'stars', 'cube', 6), ('plating', 'plating', 'cube', 3)]),
    'computronium': ('computronium_block', None, '#0e2418', '#60ff9a', [
        ('panel', 'data', 'cube', 6), ('tiles', 'tiles', 'cube', 3), ('lamp', 'lamp', 'cube', 15)]),
    'dilithium': ('dilithium_block', 'dilithium_crystal', '#5a1a3a', '#ff80c0', [
        ('polished', 'polished', 'cube', 8), ('lamp', 'lamp', 'cube', 15)]),
    'void': ('void_lattice_block', 'void_lattice_shard', '#14081f', '#b04dff', [
        ('bricks', 'bricks', 'cube', 3), ('tiles', 'tiles', 'cube', 3), ('pillar', 'pillar', 'pillar', 3)]),
    'stellar': ('stellar_frame_casing', None, '#3a2810', '#ffb040', [
        ('lamp', 'lamp', 'cube', 15)]),
    'metamaterial': ('containment_field_glass', None, '#30a080', '#80ffd0', [
        ('cloaking_glass', 'cloak', 'glass', 0), ('hex_glass', 'fieldglass', 'glass', 2)]),
    'arbor': ('arbor_heartwood', None, '#3a2a58', '#ffb0ff', [
        ('planks', 'planks', 'cube', 2)]),
}
# which variants get stairs / slabs / walls
SHAPED = {'neutronium_plating': 'ssw', 'neutronium_bricks': 'ssw', 'cherenkov_bricks': 'ssw', 'chronosteel_plating': 'ss',
          'spacetime_tiles': 'ss', 'spacetime_bricks': 'ssw', 'void_bricks': 'ssw', 'cosmic_tiles': 'ss', 'computronium_tiles': 'ss',
          'arbor_planks': 'ss', 'neutronium_tiles': 'ss', 'cherenkov_polished': 'ss'}
NAMES = {'neutronium': 'Neutronium', 'cherenkov': 'Cherenkov', 'chronosteel': 'Chronosteel', 'spacetime': 'Spacetime', 'cosmic': 'Cosmic Neutronium',
         'computronium': 'Computronium', 'dilithium': 'Dilithium', 'void': 'Void Lattice', 'stellar': 'Stellar', 'metamaterial': 'Metamaterial', 'arbor': 'Arbor'}
NEW_MATERIAL_BLOCKS = {
    'chronosteel_block': ('Block of Chronosteel', 'metalblock', '#8a6a2a', '#ffe0a0'),
    'spacetime_alloy_block': ('Block of Spacetime Alloy', 'metalblock', '#26305e', '#a0c0ff'),
    'cosmic_neutronium_block': ('Block of Cosmic Neutronium', 'stars', '#14141c', '#ffffff'),
    'dilithium_block': ('Block of Dilithium', 'crystalblock', '#5a1a3a', '#ff80c0'),
    'void_lattice_block': ('Void Lattice Block', 'crystalblock', '#14081f', '#b04dff'),
}


def deco_id(fam, suffix):
    if suffix in ('cloaking_glass', 'hex_glass'):
        return suffix
    if fam == 'cherenkov' and suffix == 'polished':
        return 'polished_cherenkov_quartz'
    if fam == 'dilithium' and suffix == 'polished':
        return 'polished_dilithium'
    return f'{fam}_{suffix}'


def entries():
    out = []
    for bid, (name, art, c1, c2) in NEW_MATERIAL_BLOCKS.items():
        out.append(dict(id=bid, name=name, art=art, c1=c1, c2=c2, kind='simple', light=3, hardness=8.0, tool='diamond', deco=True))
    for fam, (mat, ingot, c1, c2, variants) in FAMILIES.items():
        for suffix, art, shape, light in variants:
            bid = deco_id(fam, suffix)
            nice = suffix.replace('_', ' ').title()
            name = {'polished_cherenkov_quartz': 'Polished Cherenkov Quartz', 'polished_dilithium': 'Polished Dilithium',
                    'cloaking_glass': 'Metamaterial Cloaking Glass', 'hex_glass': 'Hexagonal Field Glass'}.get(bid, f'{NAMES[fam]} {nice}')
            kind = {'cube': 'deco', 'pillar': 'deco_pillar', 'glass': 'glass'}[shape]
            out.append(dict(id=bid, name=name, art=art, c1=c1, c2=c2, kind=kind, light=light, hardness=6.0,
                            tool='none' if fam == 'arbor' else 'iron', deco=True, material=mat))
            for k, form in zip(('stairs', 'slab', 'wall'), SHAPED.get(bid, '')):
                out.append(dict(id=f'{bid}_{k}', name=f'{name} {k.title() if k != "stairs" else "Stairs"}', art=None, c1=c1, c2=c2,
                                kind='deco_' + k, light=light, hardness=6.0, tool='none' if fam == 'arbor' else 'iron', deco=True,
                                base=bid, material=mat))
    return out


if __name__ == '__main__':
    p = os.path.join(ROOT, 'content.json')
    C = json.load(open(p))
    C['blocks'] = [b for b in C['blocks'] if not b.get('deco')] + entries()
    json.dump(C, open(p, 'w'), indent=1)
    print(len([b for b in C['blocks'] if b.get('deco')]), 'decorative blocks')
