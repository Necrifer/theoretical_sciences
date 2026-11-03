"""
Round 21.

* The Breached Anomaly Engine becomes the mod's mob spawner (dungeon/AnomalyEngineBlockEntity): a genome sample says
  which creature, Morphogenic Serum is used for each, four conditions must be tuned to the creature; opened only with
  Synthetic Biology. Crafted on the Convergence Altar.
* The Akashic Record (creative only): every research and symbol at once.
* The Censer of Entropy (endgame, Core Forge): any harmful effect on a creature or as a lingering cloud.
* Singularities anyone can add, automatic ones for every c:ingots item, and Singularity Clusters when there are more
  than the Omniscience Catalyst has room for (extreme/Singularities).
* The Biosphere Compendium and the Primordial Harvest take the meats, crops and mob drops of every installed mod.
* Every material has more than one use (the single-use ones are worked into later recipes).
"""
import glob
import hashlib
import importlib
import json
import os
import sys

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('morphogenic_serum', 'Morphogenic Serum', 'contained', '#102a18', '#80ffb0',
       'Every signal an embryo listens to, in one vial. A Breached Anomaly Engine spends one on every creature it grows.'),
    _I('akashic_record', 'Akashic Record', 'codex', '#1a1030', '#ffe0ff',
       'Everything that has ever been known, written once. Creative mode only.', special='akashic', stack=1),
    _I('entropy_censer', 'Censer of Entropy', 'orb', '#120818', '#b060ff',
       'Swing it and the world remembers how to decay. Any affliction, on one creature or on a place.', special='censer', stack=1),
    _I('dynamic_singularity', 'Singularity', 'singularity', '#202020', '#ffffff',
       'A singularity of something from another mod (or added by your pack).', special='dynsing'),
    _I('singularity_cluster', 'Singularity Cluster', 'singularity', '#100818', '#ffffff',
       'Nine singularities condensed into one, when there are too many to hold.', special='dyncluster'),
]
CREATIVE_ONLY = ['akashic_record']
BEYOND = ['akashic_record', 'entropy_censer']

LANG = {
    'message.theoreticalsciences.akashic_creative': 'The Akashic Record opens only in creative mode.',
    'message.theoreticalsciences.akashic_done': 'The Akashic Record: %s research entries learned, and every symbol.',
    'tooltip.theoreticalsciences.censer_set': '%s %s, %s s, cloud radius %s',
    'tooltip.theoreticalsciences.censer_use': 'Use on a creature: afflict it. Use: a cloud where you look. Sneak-use: choose.',
    'gui.theoreticalsciences.censer_seconds': 'Lasts: %s s',
    'gui.theoreticalsciences.censer_level': 'Level: %s',
    'gui.theoreticalsciences.censer_radius': 'Cloud radius: %s',
    'tooltip.theoreticalsciences.species': 'From: %s',
    'message.theoreticalsciences.engine_research': 'You do not understand this machine yet (research Synthetic Biology).',
    'gui.theoreticalsciences.engine_param.0': 'Morphogen',
    'gui.theoreticalsciences.engine_param.1': 'Membrane',
    'gui.theoreticalsciences.engine_param.2': 'Tide',
    'gui.theoreticalsciences.engine_param.3': 'Coherence',
    'gui.theoreticalsciences.engine_no_template': 'No template: a genome sample',
    'gui.theoreticalsciences.engine_status.0': 'Idle',
    'gui.theoreticalsciences.engine_status.1': 'Growing',
    'gui.theoreticalsciences.engine_status.2': 'Needs a genome sample',
    'gui.theoreticalsciences.engine_status.3': 'Not tuned to it',
    'gui.theoreticalsciences.engine_status.4': 'Needs Morphogenic Serum',
    'gui.theoreticalsciences.engine_status.5': 'Needs energy',
    'gui.theoreticalsciences.engine_status.6': 'Crowded / no room',
    'gui.theoreticalsciences.engine_status.7': 'Cannot grow that',
    'item.theoreticalsciences.dynamic_singularity.of': '%s Singularity',
    'item.theoreticalsciences.singularity_cluster.level1': 'Singularity Cluster %s',
    'item.theoreticalsciences.singularity_cluster.level2': 'Singularity Hypercluster %s',
    'item.theoreticalsciences.singularity_cluster.level3': 'Singularity Ultracluster %s',
    'tooltip.theoreticalsciences.cluster': 'Condensed singularities. The Omniscience Catalyst asks for these when there are too many singularities to hold.',
    'tooltip.theoreticalsciences.dynamic_singularity': '%s x %s',
}


def _h(*p):
    return int(hashlib.sha256('|'.join(map(str, p)).encode()).hexdigest()[:12], 16)


def apply(g):
    w, mr, REC, D = g['w'], g['mr'], g['REC'], g['D']
    g['LANG'].update(LANG)
    # ---- the spawner: its serum, and the engine itself (Convergence Altar)
    mr('morphogenic_serum', 'bioculture', [('recombinant_dna', 1), ('engineered_enzyme', 2), ('metal_organic_framework', 1), ('folded_protein_crystal', 1)],
       [('nutrient_broth', 1000)], [('morphogenic_serum', 4)], [], 65536, 400, 'synthetic_biology')
    w(os.path.join(REC, 'convergence', 'breached_anomaly_engine.json'), {
        'type': f'{NS}:convergence', 'core': {'item': f'{NS}:exotic_matter_crystal'},
        'pedestals': [{'item': f'{NS}:{x}'} for x in ('superconducting_coil', 'quantum_processor', 'recombinant_dna', 'engineered_enzyme', 'folded_protein_crystal',
                                                     'telomerase_complex', 'metal_organic_framework', 'electride_crystal')],
        'result': {'id': f'{NS}:breached_anomaly_engine', 'count': 1}, 'energy': 2_000_000_000, 'duration': 1200, 'tier': 2,
        'research': f'{NS}:synthetic_biology'})
    # creatures it will not grow
    w(os.path.join(D, 'tags', 'entity_type', 'not_spawnable.json'), {'replace': False, 'values': [
        'minecraft:ender_dragon', 'minecraft:wither', 'minecraft:warden', 'minecraft:elder_guardian', 'minecraft:player',
        f'{NS}:alpha_omega', f'{NS}:alpha_omega_genesis', f'{NS}:hyperobject', f'{NS}:containment_rupture'] +
        # (not every one of these is an entity: optional, so a missing one does not void the whole tag)
        [{'id': f'{NS}:{x}', 'required': False} for x in ('bubble_universe', 'fractured_reality', 'ex_nihilo_engine')] + [{'id': '#c:bosses', 'required': False}]})
    # ---- the Omniscience Catalyst follows the game: every singularity there is, or their clusters
    of = os.path.join(REC, 'extreme', 'omniscience_catalyst.json')
    old = json.load(open(of))
    w(of, {'type': f'{NS}:extreme_dynamic', 'ingredients': old['ingredients'], 'singularities': True, 'result': old['result']})
    w(os.path.join(REC, 'extreme', 'singularity_clusters.json'), {'type': f'{NS}:extreme_cluster', 'extra': {'item': f'{NS}:micro_singularity'}})
    # ---- the "everything" items take every installed mod's meats, crops and drops
    w(os.path.join(REC, 'extreme', 'biosphere_compendium.json'), {
        'type': f'{NS}:extreme_dynamic', 'ingredients': [{'item': f'{NS}:cosmic_neutronium_ingot'}, {'item': 'minecraft:egg'}],
        'tags': ['c:foods/raw_meat', 'c:foods/cooked_meat', 'c:foods/raw_fish', 'c:foods/cooked_fish', 'c:foods/food_poisoning',
                 'c:bones', 'c:strings', 'c:leathers', 'c:feathers', 'c:gunpowders', 'c:slime_balls', 'c:ender_pearls', 'c:rods/blaze',
                 'c:foods/raw_meats', 'c:foods/cooked_meats', 'c:mob_drops', 'c:drops'],
        'result': {'id': f'{NS}:biosphere_compendium', 'count': 1}})
    w(os.path.join(REC, 'extreme', 'primordial_harvest.json'), {
        'type': f'{NS}:extreme_dynamic', 'ingredients': [{'item': f'{NS}:cosmic_neutronium_ingot'}],
        'tags': ['c:crops', 'c:seeds', 'c:foods/fruit', 'c:foods/vegetable', 'c:foods/fruits', 'c:foods/vegetables', 'c:mushrooms', 'c:foods/berry',
                 'minecraft:saplings', 'minecraft:flowers', 'minecraft:small_flowers'],
        'result': {'id': f'{NS}:primordial_harvest', 'count': 1}})
    # ---- beyond Omnipotence: never from the Core
    tf = os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')
    t = json.load(open(tf))
    for b in BEYOND:
        if f'{NS}:{b}' not in t['values']:
            t['values'].append(f'{NS}:{b}')
    w(tf, t)


def reuse(g):
    """Every material should have more than one use: the ones with a single use go into two later recipes too."""
    w, REC, D, M, TIER, C = g['w'], g['REC'], g['D'], g['M'], g['TIER'], g['C']
    sys.argv = ['chaincheck']
    os.environ['TS_LENIENT'] = '1'
    os.environ['TS_WAVES_PRE135'] = '1'
    cc = importlib.reload(sys.modules['chaincheck']) if 'chaincheck' in sys.modules else importlib.import_module('chaincheck')
    os.environ.pop('TS_LENIENT', None)
    os.environ.pop('TS_WAVES_PRE135', None)
    W = cc.WAVE
    tuning = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'recipe_tuning.json')))
    cost = {int(k): v for k, v in tuning['cost_factor'].items()}
    use = {}
    files = sorted(glob.glob(os.path.join(REC, '**', '*.json'), recursive=True))
    # 1.3.5: content from 1.3.5 on is neither counted nor reused here, so older recipes stay exactly as they were
    import round42 as _r42, round43 as _r43, round44 as _r44, round45 as _r45, round46 as _r46, round47 as _r47, round48 as _r48, round49 as _r49, round50 as _r50, round51 as _r51, round52 as _r52, round53 as _r53, round54 as _r54, round55 as _r55, round56 as _r56, round57 as _r57
    late_ids, late_names = set(), set()
    for _q in (_r42, _r43, _r44, _r45, _r46, _r47, _r48, _r49, _r50, _r51, _r52, _r53, _r54, _r55, _r56, _r57):
        late_ids |= {f'{NS}:{x["id"]}' for x in _q.ITEMS + _q.BLOCKS + _q.FLUIDS}
        late_names |= set(_q.MACHINE_TEXT) | {r_[0] for r_ in _q.RESEARCH}

    def _late(path):
        b = os.path.basename(path)[:-5]
        return b.startswith(('r42_', 'r43_', 'r44_', 'r45_', 'r46_', 'r47_', 'r48_', 'r49_', 'r50_', 'r51_', 'r52_', 'r53_', 'r54_', 'r55_', 'r56_', 'r57_')) or b in late_names
    files = [f for f in files if not _late(f)]
    for f in files:
        r = json.load(open(f))
        ins = []
        for k in ('item_inputs', 'catalysts', 'pedestals', 'ingredients'):
            for x in r.get(k, []) or []:
                if isinstance(x, dict) and 'item' in x: ins.append(x['item'])
        for v in (r.get('key') or {}).values():
            if isinstance(v, dict) and 'item' in v: ins.append(v['item'])
        for k in ('ingredient', 'core'):
            if isinstance(r.get(k), dict) and 'item' in r[k]: ins.append(r[k]['item'])
        for i in set(ins): use.setdefault(i, set()).add(f)
    for rf in glob.glob(os.path.join(D, 'ts_research', '*.json')):
        if _late(rf): continue
        for c in json.load(open(rf)).get('cost', []): use.setdefault(c.get('item'), set()).add(rf)
    for mf in glob.glob(os.path.join(g['A'], 'multiblocks', '*.json')):
        if _late(mf): continue
        for v in json.load(open(mf))['key'].values():
            for i in v: use.setdefault(i if ':' in i else f'{NS}:{i}', set()).add(mf)
    beyond = set(json.load(open(os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')))['values'])
    by_type = {m['recipe']: m for m in M}
    single = []
    for it in C['items']:
        i = f'{NS}:{it["id"]}'
        if i in beyond or i in late_ids or it.get('special') or i not in W or 'notes_' in it['id'] or 'dossier' in it['id']:
            continue
        if it.get('stack', 64) == 1 and it['id'] != 'omnium_singularity':
            continue                          # tools and charms are used again and again already
        if len(use.get(i, ())) <= 1:
            single.append(i)
    # candidate recipes: machine recipes, not loops / secrets / one-offs, with an input slot to spare
    cands = []
    for f in files:
        typ = os.path.basename(os.path.dirname(f))
        m = by_type.get(typ)
        if not m or m['generator'] or m['id'] in ('research_nexus', 'vacuum_harvester', 'pleroma_reconstitutor'):
            continue
        r = json.load(open(f))
        if r.get('secret') or r.get('unscaled') or r.get('insight_per_run'):
            continue
        n_in = len(r.get('item_inputs', [])) + len(r.get('catalysts', [])) + len(r.get('optional_inputs', []))
        if n_in >= m['inputs']:
            continue
        outs = [o['id'] for o in r.get('item_outputs', [])]
        rw = W.get(f'{NS}:{m["id"]}', 0)
        for x in r.get('item_inputs', []):
            if 'item' in x: rw = max(rw, W.get(x['item'], 0))
        if any(o in beyond for o in outs):
            continue
        cands.append((f, m, rw, outs))
    added = []
    for i in single:
        wi = W[i]
        pool = [c for c in cands if c[2] > wi and i not in c[3]]
        pool.sort(key=lambda c: (c[2] - wi, _h(i, c[0])))
        took = 0
        for f, m, rw, outs in pool[:12]:
            r = json.load(open(f))
            if any(x.get('item') == i for x in r.get('item_inputs', [])):
                continue
            n_in = len(r.get('item_inputs', [])) + len(r.get('catalysts', [])) + len(r.get('optional_inputs', []))
            if n_in >= m['inputs']:
                continue
            stack1 = any(x['id'] == i.split(':')[1] and x.get('stack', 64) == 1 for x in C['items'])
            n = 1 if stack1 else int(min(tuning.get('item_cap', 1024), 2 * cost.get(TIER.get(m['id'], 1), 2)))
            r.setdefault('item_inputs', []).append({'item': i, 'count': n})
            w(f, r)
            added.append((i.split(':')[1], os.path.basename(f)[:-5]))
            took += 1
            if took >= 2:
                break
    print('round21 reuse:', len(single), 'single-use materials;', added)


def finish(g):
    w, te, A = g['w'], g['text_entry'], g['A']
    for i in ('dynamic_singularity', 'singularity_cluster'):
        w(os.path.join(A, 'models', 'item', i + '.json'), {'parent': 'minecraft:item/generated',
                                                            'textures': {'layer0': f'{NS}:item/{i}', 'layer1': f'{NS}:item/{i}_glow'}})
    te('biology', 'anomaly_spawner', 'The Breached Anomaly Engine', 'breached_anomaly_engine', [
        'The engines found in ruins pull creatures out of nothing at random. One you build yourself (on the $(item)Convergence Altar$()) grows the creature you choose - once you have studied $(thing)Synthetic Biology$(); before that it will not open for you.',
        'Its template is a $(item)Genome Sample$() from the Gene Sampler: every sample now remembers whose it is. Each creature costs one $(item)Morphogenic Serum$() and 16 million FE.',
        'And it must be tuned: four controls - morphogen, membrane, tide and coherence - and four readings of how close the breach is to the creature\'s form. Every creature has its own settings; bring all four readings to the middle and it starts to grow them. It stops when eight are around it. It will not grow bosses (tag theoreticalsciences:not_spawnable).'],
        sort=30, spot=['breached_anomaly_engine', 'morphogenic_serum'])
    te('endgame', 'entropy_censer', 'The Censer of Entropy', 'entropy_censer', [
        'Forged at the rebuilt Core. Sneak-use to choose any harmful effect of any mod, its level (up to 255), how long it lasts (up to an hour) and the radius of its cloud.',
        'Use it on a creature to lay the effect on it; use it in the air to leave a lingering cloud of it where you look, up to 48 blocks away.'],
        sort=33, spot=['entropy_censer'])
    te('fabrication', 'more_singularities', 'Singularities of Everything', 'omniscience_catalyst', [
        'Every ingot of every installed mod (anything tagged c:ingots) has its own singularity, made in the $(item)Singularity Compactor$() - and the $(item)Omniscience Catalyst$() needs one of each, on top of its usual ingredients. Packs can add more (data/<namespace>/ts_singularities/*.json), with their own names, counts and colours; colours not given come from the item\'s texture.',
        'When there are more singularities than the Extreme Assembly Table can hold, nine at a time are condensed (with a Micro Singularity) into a $(item)Singularity Cluster$() - and clusters into Hyperclusters - and the catalyst asks for those instead. JEI shows what this game needs. Clusters can be renamed with a resource pack or in ts_singularities/_clusters.json.',
        'The $(item)Biosphere Compendium$() and the $(item)Primordial Harvest$() likewise take one of every meat, catch and mob drop - every crop, seed, fruit and flower - of every installed mod, as many as fit.'],
        sort=12, spot=['dynamic_singularity', 'singularity_cluster'])
