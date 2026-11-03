"""
Round 10:
  * Oracle's Lemma - a creative-only tool that solves the Alpha Omega trial chamber you are in
  * no vanilla spawns in our biomes (Java also blocks natural spawns of foreign mobs there)
  * ores generate as winding veins, fewer of them
  * Anomaly Vaults: half-sunk ruins in every dimension with Breached Anomaly Engines that materialise the local mobs
"""
import glob
import json
import os

NS = 'theoreticalsciences'

ITEMS = [
    dict(id='oracle_lemma', name="Oracle's Lemma", art='omega', c1='#301040', c2='#ffd0ff', special='oracle', stack=1,
         lore='Creative mode only. Use inside one of Alpha Omega\'s trial chambers to have it count as solved.'),
]
BLOCKS = [
    dict(id='breached_anomaly_engine', name='Breached Anomaly Engine', art='reality', c1='#1a0c2a', c2='#c060ff', kind='anomaly',
         light=7, hardness=30.0, tool='diamond'),
]
FLUIDS = []

LANG = {
    'puzzle.theoreticalsciences.drop_here': 'Drag tiles here',
    'message.theoreticalsciences.oracle_creative': "Oracle's Lemma only works in creative mode.",
    'message.theoreticalsciences.oracle_solved': 'Q.E.D. - the Oracle vouches for this chamber.',
    'message.theoreticalsciences.oracle_nothing': 'There is no unsolved trial chamber here.',
    'tooltip.theoreticalsciences.oracle_lemma': 'Creative only: solves the Alpha Omega trial chamber you stand in',
}

# where vaults appear (biome -> rarity: one per N chunks)
VAULT_BIOMES = {'the_bulk': 40, 'moduli_wastes': 40, 'graviton_reef': 48, 'platonic_realm': 36, 'quantum_foam': 40, 'transfinite_strata': 36,
                'black_hole_interior': 32, 'white_hole_interior': 32, 'hyperreal_expanse': 60, 'transcendental_expanse': 48, 'antiverse': 36}


def apply(g):
    g['LANG'].update(LANG)
    loot_chest = g['loot_chest']
    loot_chest('anomaly_vault', [('quantum_processor', 12, 1, 3), ('superconducting_coil', 12, 2, 6), ('exotic_matter_crystal', 8, 1, 3),
                                 ('photonic_processor', 10, 1, 3), ('metamaterial_sheet', 10, 2, 5), ('chronon_capacitor', 6, 1, 2),
                                 ('graviton_crystal', 3, 1, 1), ('empirical_dossier', 3, 1, 1), ('containment_cell', 8, 2, 6),
                                 ('neutronium_ingot', 14, 2, 8), ('field_emitter', 4, 1, 1)])


def finish(g):
    w, q, D, LT = g['w'], g['q'], g['D'], g['LT']
    WG = os.path.join(D, 'worldgen')
    # ---- the engine drops salvage, not itself
    w(os.path.join(LT, 'blocks', 'breached_anomaly_engine.json'), {'type': 'minecraft:block', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': q('quantum_processor'),
                                  'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': q('superconducting_coil'),
                                  'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}}]}]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}, 'entries': [{'type': 'minecraft:item', 'name': q('exotic_matter_crystal')},
                                                                              {'type': 'minecraft:item', 'name': q('field_emitter')}]}]})

    # ---- our biomes host only our creatures
    stripped = 0
    for f in glob.glob(os.path.join(WG, 'biome', '*.json')):
        b = json.load(open(f))
        ch = False
        for cat, lst in b.get('spawners', {}).items():
            keep = [s for s in lst if s['type'].startswith(NS + ':')]
            if len(keep) != len(lst):
                stripped += len(lst) - len(keep)
                b['spawners'][cat] = keep
                ch = True
        if ch: w(f, b)
    print('vanilla spawn entries removed from our biomes:', stripped)

    INTERIOR_HOSTS = {f'{NS}:' + b for b in ('crustal_rock', 'basalt_column', 'peridotite', 'core_iron', 'oklo_rock')}
    # ---- ores become veins: fewer, but each one a seam worth following
    veins = 0
    for f in glob.glob(os.path.join(WG, 'configured_feature', 'ore_*.json')):
        name = os.path.basename(f)[:-5]
        c = json.load(open(f))
        if c.get('type') != 'minecraft:ore': continue
        size = c['config']['size']
        t = c['config']['targets'][0]
        L = 18 + 3 * size                                   # bigger old blobs -> longer veins
        rad = 1.3 + min(0.7, size / 20)
        pf = os.path.join(WG, 'placed_feature', name + '.json')
        p = json.load(open(pf))
        host = t['target'].get('block', '')
        if host in INTERIOR_HOSTS:
            # deep in the planet ore runs in great branching lodes: thick, long, forked, and in most chunks
            old = next((pl['count'] for pl in p['placement'] if pl['type'] == 'minecraft:count'), 4)
            rad = min(3.0, 2.0 + size / 14)
            w(f, {'type': f'{NS}:ore_vein', 'config': {'target': t['target'], 'state': t['state'], 'length': 64,
                                                      'radius': round(rad, 2), 'density': 0.5, 'branches': 3 if size >= 8 else 2}})
            p['placement'] = [x for x in p['placement'] if x['type'] != 'minecraft:count']
            ch = 2 if old >= 10 else 3 if old >= 7 else 4
            p['placement'].insert(0, {'type': 'minecraft:rarity_filter', 'chance': ch})
            w(pf, p)
            veins += 1
            continue
        w(f, {'type': f'{NS}:ore_vein', 'config': {'target': t['target'], 'state': t['state'], 'length': min(64, L), 'radius': round(rad, 2), 'density': 0.55}})
        old = 1
        for pl in p['placement']:
            if pl['type'] == 'minecraft:count': old = pl['count'] if isinstance(pl['count'], int) else 8
        per_vein = 0.75 * L * 3.14 * rad * rad * 0.55 * 0.7          # expected ore blocks in one vein
        per_chunk_before = old * size * 0.6
        chance = max(2, round(per_vein / (per_chunk_before * 0.5)))   # about half as much ore overall
        p['placement'] = [x for x in p['placement'] if x['type'] != 'minecraft:count']
        p['placement'].insert(0, {'type': 'minecraft:rarity_filter', 'chance': chance})
        w(pf, p)
        veins += 1
    print('ore features turned into veins:', veins)

    # ---- anomaly vaults
    w(os.path.join(WG, 'configured_feature', 'anomaly_vault.json'), {'type': f'{NS}:anomaly_vault', 'config': {}})
    for biome, chance in VAULT_BIOMES.items():
        bf = os.path.join(WG, 'biome', biome + '.json')
        if not os.path.exists(bf): continue
        pid = f'anomaly_vault_{biome}'
        w(os.path.join(WG, 'placed_feature', pid + '.json'), {'feature': q('anomaly_vault'), 'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'}, {'type': 'minecraft:biome'}]})
        b = json.load(open(bf))
        feats = b.setdefault('features', [])
        while len(feats) < 5: feats.append([])
        if q(pid) not in feats[4]: feats[4].append(q(pid))
        w(bf, b)
    w(os.path.join(WG, 'placed_feature', 'anomaly_vault_overworld.json'), {'feature': q('anomaly_vault'), 'placement': [
        {'type': 'minecraft:rarity_filter', 'chance': 420}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'}, {'type': 'minecraft:biome'}]})
    w(os.path.join(D, 'neoforge', 'biome_modifier', 'anomaly_vaults.json'), {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld',
                                                                            'features': q('anomaly_vault_overworld'), 'step': 'surface_structures'})
