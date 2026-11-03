"""Round 30 (round30_renew): every material of the mod can be made renewably.

The rule: anything this mod uses as an ingredient must have at least one route that, once built, runs on renewable
inputs only - farmable / generator-made vanilla things, energy, and the mod's own machine products - so nobody has to
keep mining ores or killing creatures. Before this round, ores (overworld and dimension ores), the dimensions' own
stones and landmark blocks, the natives' drops, containment-failure fallout, genome samples and some seas were the only
sources of 47 materials (and so, through them, of almost everything else).

Each route here is dearer than going and getting the thing, and needs the research that the item's world needs.
The dimension routes need that dimension's key in the machine as a catalyst (never used up): the machine reaches into
the Bulk / the Foam / the Hierarchy / the hole / the Antiverse / the Realm through it. That also keeps every route from
arriving earlier in the progression than the dimension itself (the Realm's and the Hierarchy's stones also want one
block of themselves as the seed).
"""
LANG = {}

BULK, FOAM, CANTOR, PLATO, ANTI, AXIOM = 'brane_shifter', 'planck_diver', 'cantor_key', 'platonic_compass', 'cpt_mirror', 'axiom_key'
HOLE = ('horizon_diver', 'geodesic_anchor')

# (name, machine, item inputs, fluid inputs, item outputs, fluid outputs, energy FE/t, ticks, research, catalysts)
RECIPES = [
    # ---------------------------------------------------------------- the overworld's ores
    ('cherenkov_quartz_growth', 'crystal_growth', [('minecraft:quartz', 4), ('minecraft:amethyst_shard', 2)], [('minecraft:water', 1000)],
     [('cherenkov_quartz', 2)], [], 512, 400, 'precision_lithography', ['cherenkov_quartz_block']),
    ('dilithium_growth', 'crystal_growth', [('minecraft:netherrack', 16), ('minecraft:redstone', 4)], [('minecraft:lava', 1000)],
     [('dilithium_crystal', 1)], [], 1024, 600, 'precision_lithography', ['dilithium_block']),
    ('degenerate_matter_pressing', 'casimir_pressing', [('minecraft:cobbled_deepslate', 32), ('minecraft:obsidian', 4)], [],
     [('degenerate_matter_chunk', 1)], [], 2048, 600, 'casimir_effect', []),
    ('void_lattice_sorting', 'entropy_sorting', [('minecraft:obsidian', 8), ('minecraft:chorus_fruit', 16), ('minecraft:amethyst_shard', 2)], [],
     [('void_lattice_shard', 1)], [], 4096, 600, 'thermodynamics_of_information', []),
    # ---------------------------------------------------------------- containment-failure fallout, on purpose
    ('controlled_fracture', 'chrono_inversion', [('spacetime_casing', 1), ('quantum_dot', 2)], [('superfluid_helium', 1000)],
     [('fractured_essence', 1)], [], 8192, 600, 'chronodynamics', []),
    ('void_condensation', 'halo_collection', [('void_lattice_shard', 4), ('casimir_plate', 2)], [('superfluid_helium', 1000)],
     [('void_essence', 2)], [], 65536, 600, 'dark_sector', []),
    # ---------------------------------------------------------------- genomes without a creature to prick
    ('genome_assembly', 'gene_sequencing', [('minecraft:egg', 4), ('minecraft:bone_meal', 8), ('minecraft:kelp', 4)], [('minecraft:water', 1000)],
     [('genome_sample', 2)], [], 4096, 400, 'genomics', []),
    ('xenogenome_recoding', 'gene_sequencing', [('genome_sample', 2), ('fractured_essence', 1), ('void_lattice_shard', 1)], [],
     [('xenogenome_sample', 1)], [], 16384, 600, 'xenobiology', []),
    # ---------------------------------------------------------------- the Platonic Realm (through a Platonic Compass)
    ('axiom_stone_construction', 'arithmetic', [('minecraft:stone', 16), ('natural_number_lattice', 1)], [],
     [('axiom_stone', 16)], [], 2048, 400, 'platonic_realm', [PLATO, 'axiom_stone']),
    ('transcendental_extraction', 'analysis', [('axiom_stone', 16), ('rational_field', 2)], [],
     [('transcendental_crystal', 1)], [], 8192, 600, 'platonic_realm', [PLATO]),
    ('koch_iteration', 'analysis', [('axiom_stone', 8), ('natural_number_lattice', 2)], [],
     [('fractal_lattice_block', 4), ('koch_crystal', 1)], [], 8192, 600, 'platonic_realm', [PLATO]),
    ('fractal_core_recursion', 'analysis', [('fractal_dust', 8), ('transcendental_crystal', 1)], [],
     [('fractal_core', 1)], [], 16384, 800, 'platonic_realm', [PLATO]),
    ('proof_fragment_derivation', 'formal_proof', [('axiom_stone', 8), ('formal_proof_tree', 1)], [],
     [('proof_fragment', 2)], [], 8192, 600, 'platonic_realm', [PLATO]),
    # ---------------------------------------------------------------- the Bulk (through a Brane Shifter)
    ('bulk_condensation', 'graviton_condensing', [('minecraft:cobbled_deepslate', 16), ('graviton_crystal', 1)], [],
     [('bulk_stone', 16), ('bulk_crystal', 1)], [], 65536, 600, 'brane_cosmology', [BULK]),
    ('bulk_moduli_sorting', 'entropy_sorting', [('bulk_stone', 32), ('quantum_dot', 2)], [('superfluid_helium', 2000)],
     [('dilaton_crystal', 1), ('moduli_crystal', 1)], [], 65536, 600, 'brane_cosmology', []),
    ('brane_tension_weaving', 'string_weaving', [('quantum_string', 2), ('bulk_crystal', 1)], [],
     [('brane_tension_fiber', 1)], [], 65536, 600, 'brane_cosmology', [BULK]),
    ('graviton_wisp_condensation', 'graviton_condensing', [('graviton_crystal', 4), ('bulk_crystal', 2)], [],
     [('graviton_wisp_core', 1)], [], 131072, 800, 'brane_cosmology', [BULK]),
    # ---------------------------------------------------------------- the Quantum Foam (through a Planck Diver)
    ('foam_boundary_harvest', 'vacuum_harvesting', [('casimir_plate', 8)], [('superfluid_helium', 64000), ('false_vacuum', 4000)],
     [('foam_substrate', 16), ('false_vacuum_membrane', 1)], [('schrodinger_limit', 1000), ('relativity_breakdown', 1000)],
     1048576, 400, 'quantum_foam', [FOAM]),
    # ---------------------------------------------------------------- the Transfinite Hierarchy (through a Cantor Key)
    ('ordinal_strata', 'transfinite_computation', [('minecraft:stone', 32), ('power_set', 1)], [],
     [('ordinal_stone', 32), ('hypersphere_fragment', 1)], [], 65536, 800, 'transfinite_hierarchy', [CANTOR, 'ordinal_stone']),
    ('hilbert_construction', 'formal_proof', [('ordinal_stone', 8), ('minecraft:glass', 8), ('aleph_null_shard', 1)], [],
     [('hilbert_brick', 8), ('aleph_glass', 8)], [], 65536, 600, 'transfinite_hierarchy', [CANTOR]),
    ('cardinal_ascent', 'ordinal_recursion', [('ordinal_stone', 16), ('omega_ordinal', 1)], [],
     [('cardinal_shard', 1)], [], 131072, 800, 'transfinite_hierarchy', [CANTOR]),
    ('hotel_key_cutting', 'transfinite_computation', [('hilbert_brick', 4), ('aleph_null_shard', 2)], [],
     [('hotel_key', 1)], [], 65536, 600, 'transfinite_hierarchy', [CANTOR]),
    # ---------------------------------------------------------------- inside the holes (through a Horizon Diver on a Geodesic Anchor)
    ('accretion_infall', 'kugelblitz', [('minecraft:cobbled_deepslate', 64), ('minecraft:glass', 8), ('micro_singularity', 1)], [],
     [('infall_rock', 32), ('horizon_glass', 4), ('tidal_alloy_scrap', 2)], [], 1048576, 800, 'black_hole_interior', list(HOLE)),
    ('ejecta_emission', 'white_hole', [('minecraft:basalt', 64), ('white_hole_fragment', 1)], [],
     [('ejecta_rock', 32), ('primordial_ejecta', 2), ('retrocausal_crystal', 1)], [], 1048576, 800, 'black_hole_interior', list(HOLE)),
    # ---------------------------------------------------------------- the Antiverse (through a CPT Mirror)
    ('cpt_reflect_stone', 'mirror_synthesis', [('minecraft:stone', 32), ('antimatter_pellet', 1)], [('minecraft:water', 8000)],
     [('antistone', 32)], [('antiwater', 8000)], 1048576, 600, 'cpt_symmetry', [ANTI]),
    ('cpt_reflect_ores', 'mirror_synthesis', [('minecraft:iron_ingot', 8), ('minecraft:gold_ingot', 8), ('minecraft:charcoal', 16), ('minecraft:quartz', 8),
                                              ('antimatter_pellet', 4)], [],
     [('raw_antiiron', 8), ('raw_antigold', 8), ('anticarbon_crystal', 4), ('antisilicon_shard', 4)], [], 1048576, 800, 'cpt_symmetry', [ANTI]),
    ('cpt_reflect_quarks', 'mirror_synthesis', [('up_down_quarks', 4), ('antimatter_pellet', 2)], [],
     [('antiquark_shard', 4)], [], 1048576, 600, 'cpt_symmetry', [ANTI]),
    ('antinuclei_freezing', 'antiproton_deceleration', [('antiproton_bundle', 2), ('antimatter_pellet', 1)], [('superfluid_helium', 1000)],
     [('antihydrogen_crystal', 1), ('antihelium_nucleus', 1)], [], 65536, 600, 'cpt_symmetry', [ANTI]),
    # ---------------------------------------------------------------- Hyperdimensional Reality (through an Axiom Key)
    ('primordial_law_distillation', 'axiom_distillation', [('axiom_fragment', 4)], [('dark_energy', 8000)],
     [], [('primordial_law', 16000)], 33554432, 2400, 'hyperdimensional_reality', [AXIOM]),
    ('bubble_nucleation', 'universe_forging', [('pocket_universe', 1), ('universe_seed', 2)], [('primordial_law', 250)],
     [('bubble_universe_sample', 2)], [], 67108864, 1200, 'reality_breach', [AXIOM]),
    ('world_tree_cultivation', 'directed_evolution', [('minecraft:oak_log', 32), ('minecraft:bone_meal', 16)], [('primordial_law', 250)],
     [('arbor_heartwood', 8)], [], 16777216, 1200, 'reality_breach', ['arbor_heartwood_sample', AXIOM]),
]


def apply(g):
    """The recipes are costed here, at the machine's tier, like scaling.py does for the rest - and marked unscaled so
    round 16/21 do not pull further materials into them: a pulled-in material can itself need the very thing the route
    makes (cherenkov quartz <-> dilithium <-> degenerate matter...), and the route would stop being renewable."""
    import json, os
    g['LANG'].update(LANG)
    T = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'recipe_tuning.json')))
    cost = {int(k): v for k, v in T['cost_factor'].items()}
    fcost = {int(k): v for k, v in T.get('fluid_factor', T['cost_factor']).items()}
    dur = {int(k): v for k, v in T.get('duration_factor', {}).items()}
    item_cap = int(T.get('item_cap', 1024))
    by = {m['recipe']: m for m in g['M']}
    for name, typ, ins, fins, outs, fouts, fe, t, res, cats in RECIPES:
        m = by[typ]
        tier = g['TIER'].get(m['id'], 1)
        tank = int(T.get('fluid_cap_multiblock' if m['multiblock'] else 'fluid_cap_single', 2_048_000 if m['multiblock'] else 256_000))
        ins_ = [(i, min(item_cap, n * cost.get(tier, 1))) for i, n in ins]
        fins_ = [(f, min(tank, n * fcost.get(tier, 1))) for f, n in fins]
        g['mr'](name, typ, ins_, fins_, outs, fouts, energy=fe, duration=int(t * dur.get(tier, 1.0)), research=res, catalysts=tuple(cats))
        p = os.path.join(g['REC'], typ, name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        g['w'](p, r)


def finish(g):
    pass
