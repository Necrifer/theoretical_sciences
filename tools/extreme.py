"""
Mass-crafting layer, modelled on how Avaritia (Re:Avaritia) and GregTech: New Horizons make their last ingot expensive.

Re:Avaritia                                    Theoretical Sciences
---------------------------------------------  ------------------------------------------------------------------
Extreme Crafting Table (9x9)                   Extreme Assembly Table (9x9)
Neutronium Compressor (1000 items, timer)      Singularity Compactor (1,000s-16,000s of items, no timer)
27 material singularities                      29 material singularities (14 vanilla, 15 Theoretical Sciences)
Diamond Lattice -> Crystal Matrix Ingot        Cherenkov Lattice -> Crystalline Matrix Ingot
Cosmic Meatballs / Ultimate Stew               Biosphere Compendium / Primordial Harvest
Endest Pearl / Record Fragment                 Hyperdimensional Pearl / Cosmic Record Fragment
Infinity Catalyst (every singularity + ...)    Omniscience Catalyst (every material AND concept singularity + ...)
Infinity Ingot = 24 neutron + 10 matrix        Omnium Ingot = 32 cosmic neutronium + 16 crystalline matrix
                 + 11 catalysts (9x9)                         + 16 omniscience catalysts + Omega Point (own 9x9 layout)

GTNH adds the second lesson: the better your infrastructure, the cheaper (per ingot) the same item gets - the
Dimensionally Transcendent Plasma Forge turns fewer catalysts into more ingots on better coils. Here the Conceptual
Singularity Compressor plays that role: with transfinite coils it forges Omnium from far fewer catalysts.
"""

# (id, name, ingredient, count, core colour, glow colour)
MATERIAL_SINGULARITIES = [
    # vanilla - the same idea as Avaritia's iron / gold / diamond ... singularities, with bigger numbers
    ('iron_singularity', 'Iron Singularity', 'minecraft:iron_ingot', 4096, '#3a3a3a', '#e8e8e8'),
    ('gold_singularity', 'Gold Singularity', 'minecraft:gold_ingot', 4096, '#3a2a08', '#ffd84a'),
    ('copper_singularity', 'Copper Singularity', 'minecraft:copper_ingot', 8192, '#3a1a0a', '#ff9a5a'),
    ('coal_singularity', 'Coal Singularity', 'minecraft:coal', 16384, '#0a0a0a', '#606060'),
    ('redstone_singularity', 'Redstone Singularity', 'minecraft:redstone', 16384, '#300404', '#ff3030'),
    ('lapis_singularity', 'Lapis Singularity', 'minecraft:lapis_lazuli', 8192, '#081840', '#4a7aff'),
    ('quartz_singularity', 'Quartz Singularity', 'minecraft:quartz', 8192, '#3a3430', '#fff4ea'),
    ('glowstone_singularity', 'Glowstone Singularity', 'minecraft:glowstone_dust', 8192, '#3a2a08', '#fff08a'),
    ('amethyst_singularity', 'Amethyst Singularity', 'minecraft:amethyst_shard', 4096, '#24103a', '#c890ff'),
    ('diamond_singularity', 'Diamond Singularity', 'minecraft:diamond', 2048, '#08303a', '#7af8ff'),
    ('emerald_singularity', 'Emerald Singularity', 'minecraft:emerald', 2048, '#04300e', '#40ff80'),
    ('netherite_singularity', 'Netherite Singularity', 'minecraft:netherite_ingot', 512, '#1a1214', '#8a7070'),
    ('obsidian_singularity', 'Obsidian Singularity', 'minecraft:obsidian', 4096, '#0a0614', '#6a40a0'),
    ('blue_ice_singularity', 'Blue Ice Singularity', 'minecraft:blue_ice', 2048, '#08203a', '#a0e0ff'),
    # Theoretical Sciences materials - one from every stage of the mod
    ('neutronium_singularity', 'Neutronium Singularity', 'neutronium_ingot', 8192, '#101418', '#b0c0d0'),
    ('cherenkov_singularity', 'Cherenkov Singularity', 'cherenkov_quartz', 8192, '#04182a', '#40c8ff'),
    ('void_lattice_singularity', 'Void Lattice Singularity', 'void_lattice_shard', 8192, '#06020e', '#7040c0'),
    ('dilithium_singularity', 'Dilithium Singularity', 'dilithium_crystal', 4096, '#2a0418', '#ff60b0'),
    ('chronosteel_singularity', 'Chronosteel Singularity', 'chronosteel_ingot', 4096, '#2a2008', '#ffcc60'),
    ('stellar_iron_singularity', 'Stellar Iron Singularity', 'stellar_iron_ingot', 4096, '#2a1004', '#ff9040'),
    ('prime_singularity', 'Prime Singularity', 'prime_shard', 4096, '#081a30', '#80e0ff'),
    ('kaluza_klein_singularity', 'Kaluza-Klein Singularity', 'kaluza_klein_shard', 4096, '#081428', '#60c0ff'),
    ('tachyon_singularity', 'Tachyon Singularity', 'tachyon_crystal', 2048, '#0a1a2a', '#a0ffff'),
    ('strange_matter_singularity', 'Strange Matter Singularity', 'strange_matter_pellet', 2048, '#2a0a20', '#ff70d0'),
    ('antimatter_singularity', 'Antimatter Singularity', 'antimatter_pellet', 2048, '#1a0a2a', '#e0a0ff'),
    ('exotic_crystal_singularity', 'Exotic Crystal Singularity', 'exotic_matter_crystal', 2048, '#200818', '#ff50a0'),
    ('spacetime_alloy_singularity', 'Spacetime Alloy Singularity', 'spacetime_alloy_ingot', 2048, '#0a0a20', '#9090ff'),
    ('computronium_singularity', 'Computronium Singularity', 'computronium_ingot', 2048, '#04200a', '#60ff90'),
    ('cosmic_neutronium_singularity', 'Cosmic Neutronium Singularity', 'cosmic_neutronium_ingot', 1024, '#020208', '#c0c8ff'),
]

CONCEPT_SINGULARITIES = ['mathematics_singularity', 'standard_model_singularity', 'spacetime_singularity', 'stellar_singularity',
                         'dimensional_singularity', 'temporal_singularity', 'quantum_singularity', 'cognitive_singularity',
                         'exotic_matter_singularity']


def _I(id, name, art, c1, c2, lore, **kw):
    d = dict(id=id, name=name, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [_I(i, n, 'singularity', c1, c2, f'{cnt:,} {src.split(":")[-1].replace("_", " ")}, crushed into one point.', stack=64)
         for i, n, src, cnt, c1, c2 in MATERIAL_SINGULARITIES] + [
    _I('cherenkov_lattice', 'Cherenkov Lattice', 'lattice', '#08283a', '#80f0ff', 'Diamond and Cherenkov quartz woven around netherite: a frame for a crystal that should not exist.'),
    _I('crystalline_matrix_ingot', 'Crystalline Matrix Ingot', 'ingot', '#2a6a8a', '#e0ffff', 'Lattices pinned in place by the light of two dead stars.'),
    _I('biosphere_compendium', 'Biosphere Compendium', 'orb', '#3a1010', '#ff9070', 'One of every creature that can be eaten, pressed into one. Do not eat.', stack=16),
    _I('primordial_harvest', 'Primordial Harvest', 'seed', '#103a10', '#a0ff60', 'Every crop that has ever grown, in one seed.', stack=16),
    _I('hyperdimensional_pearl', 'Hyperdimensional Pearl', 'ghostorb', '#041a1a', '#40ffd0', 'A pearl that teleports to everywhere at once.', stack=16),
    _I('cosmic_record_fragment', 'Cosmic Record Fragment', 'shard', '#1a1a1a', '#ff60ff', 'A sliver of a disc that recorded the Big Bang.'),
]

BLOCK_LANG = {'extreme_assembly_table': 'Extreme Assembly Table', 'singularity_compactor': 'Singularity Compactor'}

LANG = {
    'gui.theoreticalsciences.compactor_progress': '%s / %s absorbed',
    'gui.theoreticalsciences.compactor_idle': 'Insert a material to begin',
    'gui.theoreticalsciences.compactor_needs': 'Needs %s in total - feed it any amount at a time',
    'gui.theoreticalsciences.compactor_instant': 'No timer: fills as fast as you feed it',
}

MEATS = ['beef', 'porkchop', 'chicken', 'mutton', 'rabbit', 'cod', 'salmon', 'tropical_fish', 'pufferfish', 'rotten_flesh', 'spider_eye',
         'cooked_beef', 'cooked_porkchop', 'cooked_chicken', 'cooked_mutton', 'cooked_rabbit', 'cooked_cod', 'cooked_salmon']
CROPS = ['wheat', 'carrot', 'potato', 'beetroot', 'melon_slice', 'pumpkin', 'sweet_berries', 'glow_berries', 'cocoa_beans', 'sugar_cane',
         'cactus', 'bamboo', 'kelp', 'nether_wart', 'chorus_fruit', 'apple', 'brown_mushroom', 'red_mushroom', 'crimson_fungus',
         'warped_fungus', 'sea_pickle', 'torchflower', 'pitcher_plant', 'vine', 'lily_pad', 'poisonous_potato']

# Own layouts (not Avaritia's): nested squares of a tesseract projection.
PEARL = ['EEEEEEEEE',
         'EPPPPPPPE',
         'EPEEEEEPE',
         'EPENNNEPE',
         'EPENSNEPE',
         'EPENNNEPE',
         'EPEEEEEPE',
         'EPPPPPPPE',
         'EEEEEEEEE']

# Omnium: a cross of crystalline matrix through a lattice of catalysts, Omega Point at the heart
# (X = Omniscience Catalyst x16, C = Crystalline Matrix x16, N = Cosmic Neutronium x32, O = Omega Point x1)
OMNIUM = ['XNNNCNNNX',
          'NX  C  XN',
          'N XNCNX N',
          'N NXCXN N',
          'CCCCOCCCC',
          'N NXCXN N',
          'N XNCNX N',
          'NX  C  XN',
          'XNNNCNNNX']

COMPACTION_SCALE = 4        # every material singularity needs 4x the listed count


def _ing(x):
    if x.startswith('#'):
        return {'tag': x[1:]}
    return {'item': x if ':' in x else 'theoreticalsciences:' + x}


def recipes(w, REC, mr, shaped, remove):
    import os
    ext = os.path.join(REC, 'extreme')

    def xshaped(name, pattern, key, result, count=1):
        w(os.path.join(ext, name + '.json'), {'type': 'theoreticalsciences:extreme_shaped', 'pattern': pattern,
                                               'key': {k: _ing(v) for k, v in key.items()},
                                               'result': {'id': _ing(result)['item'], 'count': count}})

    def xshapeless(name, items, result, count=1):
        assert 1 <= len(items) <= 81, (name, len(items))
        w(os.path.join(ext, name + '.json'), {'type': 'theoreticalsciences:extreme_shapeless', 'ingredients': [_ing(v) for v in items],
                                               'result': {'id': _ing(result)['item'], 'count': count}})

    # ---- the two stations (ordinary crafting)
    shaped('extreme_assembly_table', ['SCS', 'ATA', 'SCS'], {'S': 'chronosteel_block', 'C': 'computronium_block', 'A': 'casimir_plate',
                                                             'T': 'minecraft:crafting_table'}, 'extreme_assembly_table')
    shaped('singularity_compactor', ['NHN', 'BKB', 'NHN'], {'N': 'neutronium_block', 'H': 'minecraft:hopper', 'B': 'chronosteel_block',
                                                            'K': 'micro_singularity'}, 'singularity_compactor')

    # ---- material singularities (Avaritia: 1000 of each; here 512 - 16,384 depending on how common it is)
    for sid, _, src, cnt, *_ in MATERIAL_SINGULARITIES:
        w(os.path.join(REC, 'compaction', sid + '.json'), {'type': 'theoreticalsciences:compaction', 'ingredient': _ing(src), 'count': cnt * COMPACTION_SCALE,
                                                          'result': {'id': 'theoreticalsciences:' + sid, 'count': 1}})

    # ---- crystal matrix analogue: lattice in a normal table, ingot in the extreme table (Avaritia: xyx / xyx)
    shaped('cherenkov_lattice', ['QDQ', 'DSD', 'QDQ'], {'Q': 'cherenkov_quartz', 'D': 'minecraft:diamond', 'S': 'minecraft:netherite_scrap'}, 'cherenkov_lattice')
    xshaped('crystalline_matrix_ingot', ['LSL', 'SQS', 'LSL'], {'L': 'cherenkov_lattice', 'S': 'minecraft:nether_star', 'Q': 'superposition_crystal'},
            'crystalline_matrix_ingot')

    # ---- the "everything" items (Avaritia: cosmic meatballs, ultimate stew, endest pearl, record fragment)
    xshapeless('biosphere_compendium', [f'minecraft:{m}' for m in MEATS for _ in range(4)] + ['minecraft:egg'] * 4 + ['cosmic_neutronium_ingot'],
               'biosphere_compendium')
    xshapeless('primordial_harvest', [f'minecraft:{c}' for c in CROPS for _ in range(3)] + ['cosmic_neutronium_ingot'], 'primordial_harvest')
    xshaped('hyperdimensional_pearl', PEARL, {'E': 'bulk_stone', 'P': 'minecraft:ender_pearl', 'N': 'cosmic_neutronium_ingot',
                                              'S': 'minecraft:nether_star'}, 'hyperdimensional_pearl')
    xshaped('cosmic_record_fragment', ['QNQ', 'NDN', 'QNQ'], {'Q': 'cherenkov_quartz', 'N': 'neutronium_ingot', 'D': '#c:music_discs'}, 'cosmic_record_fragment', 2)

    # ---- Omniscience Catalyst: now made like Avaritia's Infinity Catalyst - one of EVERY singularity and everything-item
    remove('concept_compression', 'omniscience_catalyst')
    xshapeless('omniscience_catalyst', ['theory_of_everything', 'mathematics_singularity', 'stellar_singularity', 'temporal_singularity',
                                        'cognitive_singularity', 'exotic_matter_singularity', 'absolute_infinity', 'omega_point']
               + [s[0] for s in MATERIAL_SINGULARITIES]
               + ['biosphere_compendium', 'primordial_harvest', 'hyperdimensional_pearl', 'cosmic_record_fragment',
                  'crystalline_matrix_ingot', 'cosmic_neutronium_ingot', 'transcendent_metal_ingot'], 'omniscience_catalyst')

    # ---- Omnium Ingot: 16 catalysts in our own 9x9 layout
    xshaped('omnium_ingot', OMNIUM, {'N': 'cosmic_neutronium_ingot', 'C': 'crystalline_matrix_ingot', 'X': 'omniscience_catalyst', 'O': 'omega_point'}, 'omnium_ingot')

    # ---- GTNH-style forge routes: better coils in the Conceptual Singularity Compressor = fewer catalysts per ingot
    E = 268_435_456
    mr('omnium_ingot', 'concept_compression', [('omniscience_catalyst', 4), ('crystalline_matrix_ingot', 16), ('cosmic_neutronium_ingot', 64),
                                               ('hypernova_alloy_ingot', 32), ('transcendent_metal_ingot', 16)],
       [('primordial_law', 64000), ('transcendent_plasma', 32000), ('liquid_spacetime', 64000)], [('omnium_ingot', 1)],
       energy=E * 4, duration=6000, research='omnilex', min_tier=4)
    mr('omnium_ingot_transfinite', 'concept_compression', [('omniscience_catalyst', 4), ('crystalline_matrix_ingot', 32), ('cosmic_neutronium_ingot', 64),
                                                           ('hypernova_alloy_ingot', 64), ('transcendent_metal_ingot', 32), ('absolute_infinity', 1)],
       [('primordial_law', 64000), ('transcendent_plasma', 64000), ('hypernova_plasma', 64000)], [('omnium_ingot', 4)],
       energy=E * 8, duration=6000, research='omnilex', min_tier=5)


def total_per_catalyst():
    return sum(s[3] for s in MATERIAL_SINGULARITIES) * COMPACTION_SCALE
