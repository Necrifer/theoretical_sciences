"""
Round 16 - GTNH-style recipe cost.

* Every multiblock has 16 item and 16 fluid inputs (and 4 + 4 outputs); single-block machines a few more
  (machine/MachineType.java, mirrored in gen_java.py).
* Every machine recipe gets far more demanding: quantities scale steeply with the machine's tier (scaling.py reads
  recipe_tuning.json), durations grow, and each recipe pulls in many more of the mod's own materials and fluids - all
  of them ones that are already obtainable before the recipe (checked with the progression waves of chaincheck.py, so
  nothing becomes unreachable and no loops appear).
* Optional ingredients from other mods (Mekanism, AE2, GregTech, Draconic Evolution, Botania, Malum, Astral
  Sorcery...): written as "optional_inputs"; the game uses each only if that item exists, so the mod works alone too.
* Eight new tiered components ("Engineering Components") are the common currency that fills the new slots.

Everything is tunable in tools/recipe_tuning.json, and in game with the common config recipeIngredientMultiplier.
"""
import glob
import hashlib
import importlib
import json
import os
import sys

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
TUNING = json.load(open(os.path.join(HERE, 'recipe_tuning.json')))


def _t(key, tier):
    d = TUNING[key]
    return d.get(str(tier), d[str(max(int(k) for k in d))])


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


# one component per machine tier: the standard parts every recipe of that tier and above draws on
COMPONENTS = [
    _I('coherent_wiring_harness', 'Coherent Wiring Harness', 'coil', '#30506a', '#80e0ff',
       'Superconducting wire braided so its electrons keep their phase from end to end. Every quantum machine is threaded with it.'),
    _I('entangled_bus_bar', 'Entangled Bus Bar', 'capacitor', '#402060', '#c080ff',
       'Two bars that are one: a current at one end is already at the other.'),
    _I('chronoplastic_gasket', 'Chronoplastic Gasket', 'damper', '#504020', '#ffd080',
       'Seals a joint against time itself: what is inside ages at the machine\'s pace, not the world\'s.'),
    _I('spacetime_bearing', 'Spacetime Bearing', 'spindle', '#203048', '#a0c0ff',
       'Turns on a frictionless geodesic. It will outlast the machine around it.'),
    _I('cosmic_flux_coupler', 'Cosmic Flux Coupler', 'lens', '#201030', '#ff80d0',
       'Couples a machine to the flux of a whole cosmos without being consumed by it.'),
    _I('ontological_junction', 'Ontological Junction', 'chip', '#202020', '#ffffff',
       'Where a machine\'s idea of itself meets its matter. Without one, big machines forget what they are for.'),
    _I('transfinite_lattice_bolt', 'Transfinite Lattice Bolt', 'lattice', '#302010', '#ffb060',
       'Threaded with ℵ₀ turns. It never quite finishes screwing in, and it never comes loose.'),
    _I('omnilex_seal', 'Omnilex Seal', 'codex', '#101010', '#ffe080',
       'A wax seal pressed with a law. Machines that bear one are permitted to work.'),
]
ITEMS = COMPONENTS + [
    _I('laplaces_demon', "Laplace's Demon", 'probe', '#200830', '#e080ff',
       'An intellect that knows every force and every position. Shown a machine, it knows exactly how the machine must be run.',
       special='laplace', stack=1),
]
COMP = [c['id'] for c in COMPONENTS]

LANG = {
    'tooltip.theoreticalsciences.creative_only': 'Creative mode only',
    'tooltip.theoreticalsciences.laplaces_demon': 'Use on a multiblock controller: tunes it to the recipe its inputs match (or hold a recipe\'s product in your off hand to choose it) - every control, the coolant, containment and drift.',
    'message.theoreticalsciences.demon_creative': "Laplace's Demon answers only in creative mode.",
    'message.theoreticalsciences.demon_nothing': 'Use it on a multiblock controller.',
    'message.theoreticalsciences.demon_generic': 'any recipe',
    'message.theoreticalsciences.demon_tuned_simple': "Laplace's Demon: tuned exactly, containment restored.",
    'message.theoreticalsciences.demon_tuned': "Laplace's Demon: tuned for %s - every limit and window met, rate %sx, coolant %s.",
    'message.theoreticalsciences.demon_partial': "Laplace's Demon: no point meets everything for %s with this structure (coil tier too low?) - set to the closest it found.",
    'message.theoreticalsciences.demon_failed': "Laplace's Demon could not finish (the machine changed or was removed).",
    'message.theoreticalsciences.demon_busy': "Laplace's Demon is still thinking about this machine.",
    'message.theoreticalsciences.demon_thinking': "Laplace's Demon considers %s...",
    'message.theoreticalsciences.demon_environment': 'This recipe also needs a field around the controller (gravity, dimensions, time or vacuum): that is not something tuning can change.',
}


def apply(g):
    """Component recipes, each in a machine of its own tier."""
    mr, TIER, M, PROC, TIER_PLATE = g['mr'], g['TIER'], g['M'], g['PROC'], g['TIER_PLATE']
    by_tier = {}
    import round42 as _r42, round43 as _r43, round45 as _r45, round46 as _r46, round47 as _r47, round48 as _r48, round49 as _r49, round50 as _r50, round51 as _r51, round52 as _r52, round53 as _r53, round54 as _r54, round55 as _r55, round56 as _r56, round57 as _r57   # 1.3.5+ machines never take over the older components' recipes
    for m in M:
        t = TIER.get(m['id'])
        if not t or m['generator'] or m['id'] in ('research_nexus', 'vacuum_harvester', 'omnilex_engine', 'pleroma_reconstitutor') \
                or m['id'] in _r42.MACHINE_TEXT or m['id'] in _r43.MACHINE_TEXT or m['id'] in _r45.MACHINE_TEXT or m['id'] in _r46.MACHINE_TEXT or m['id'] in _r47.MACHINE_TEXT or m['id'] in _r48.MACHINE_TEXT or m['id'] in _r49.MACHINE_TEXT or m['id'] in _r50.MACHINE_TEXT or m['id'] in _r51.MACHINE_TEXT or m['id'] in _r52.MACHINE_TEXT or m['id'] in _r53.MACHINE_TEXT or m['id'] in _r54.MACHINE_TEXT or m['id'] in _r55.MACHINE_TEXT or m['id'] in _r56.MACHINE_TEXT or m['id'] in _r57.MACHINE_TEXT:
            continue
        by_tier.setdefault(t, []).append(m)
    for k, cid in enumerate(COMP):
        t = k + 1
        tt = t
        while tt > 1 and not by_tier.get(tt):
            tt -= 1                       # no machine of this exact tier: the nearest one below
        ms = sorted(by_tier.get(tt, []), key=lambda m: (-m['inputs'], m['id']))
        m = ms[0]
        ins = [(TIER_PLATE[t], 4), (PROC.get(t, PROC[max(PROC)]) if t > 1 else PROC[1], 1)]
        if t > 1: ins.append((COMP[k - 1], 2))
        mr(cid, m['recipe'], ins, [], [(cid, 4)], [], min(16_777_216, 256 * 8 ** t), 200 + 100 * t)
    g['LANG'].update(LANG)


# ------------------------------------------------------------------------------- the expansion (after everything)
def _h(*parts):
    return int(hashlib.sha256('|'.join(map(str, parts)).encode()).hexdigest()[:12], 16)


def expand(g):
    REC, TIER, M = g['REC'], g['TIER'], g['M']
    by_type = {m['recipe']: m for m in M}
    # progression waves of the recipes as they stand (before expansion)
    sys.argv = ['chaincheck']
    os.environ['TS_WAVES_BASE'] = '1'
    try:
        if 'chaincheck' in sys.modules:
            cc = importlib.reload(sys.modules['chaincheck'])
        else:
            cc = importlib.import_module('chaincheck')
    finally:
        os.environ.pop('TS_WAVES_BASE', None)
    W, FW = cc.WAVE, cc.FWAVE
    C = json.load(open(os.path.join(HERE, 'content.json')))
    beyond = set()
    try:
        import round15
        beyond = {f'{NS}:{x}' for x in round15.BEYOND}
    except Exception:
        pass
    # 1.2: content from later releases is never pulled into older recipes (they stay as they were)
    try:
        import round38
        beyond |= {f'{NS}:{x["id"]}' for x in round38.ITEMS + round38.BLOCKS + round38.FLUIDS}
        import round39
        beyond |= {f'{NS}:{x["id"]}' for x in round39.BLOCKS}
        import round40
        beyond |= {f'{NS}:{x["id"]}' for x in round40.ITEMS + round40.BLOCKS + round40.FLUIDS}
        import round41
        beyond |= {f'{NS}:{x["id"]}' for x in round41.ITEMS + round41.BLOCKS}
        import round42
        beyond |= {f'{NS}:{x["id"]}' for x in round42.ITEMS + round42.BLOCKS + round42.FLUIDS}
        import round43, round44, round45
        beyond |= {f'{NS}:{x["id"]}' for x in round44.ITEMS}
        beyond |= {f'{NS}:{x["id"]}' for x in round45.ITEMS + round45.BLOCKS}
        import round46
        beyond |= {f'{NS}:{x["id"]}' for x in round46.ITEMS + round46.BLOCKS}
        import round47
        beyond |= {f'{NS}:{x["id"]}' for x in round47.ITEMS + round47.BLOCKS}
        import round48
        beyond |= {f'{NS}:{x["id"]}' for x in round48.ITEMS + round48.BLOCKS}
        import round49
        beyond |= {f'{NS}:{x["id"]}' for x in round49.ITEMS + round49.BLOCKS}
        import round50
        beyond |= {f'{NS}:{x["id"]}' for x in round50.ITEMS + round50.BLOCKS}
        import round51
        beyond |= {f'{NS}:{x["id"]}' for x in round51.ITEMS + round51.BLOCKS}
        import round52
        beyond |= {f'{NS}:{x["id"]}' for x in round52.ITEMS + round52.BLOCKS}
        import round53
        beyond |= {f'{NS}:{x["id"]}' for x in round53.ITEMS + round53.BLOCKS}
        import round54
        beyond |= {f'{NS}:{x["id"]}' for x in round54.ITEMS + round54.BLOCKS}
        import round55
        beyond |= {f'{NS}:{x["id"]}' for x in round55.ITEMS + round55.BLOCKS}
        import round56
        beyond |= {f'{NS}:{x["id"]}' for x in round56.ITEMS + round56.BLOCKS}
        import round57
        beyond |= {f'{NS}:{x["id"]}' for x in round57.ITEMS + round57.BLOCKS}
        beyond |= {f'{NS}:{x["id"]}' for x in round43.ITEMS + round43.BLOCKS + round43.FLUIDS}
    except Exception:
        pass
    # what may be pulled in: materials (stackable, no special behaviour), and fluids
    cand = []
    for it in C['items']:
        i = f'{NS}:{it["id"]}'
        if it.get('stack', 64) == 1 or it.get('special') or it['id'].endswith('_cell') or i in beyond or i not in W:
            continue
        cand.append(i)
    fcand = [f'{NS}:{f["id"]}' for f in C['fluids'] if f'{NS}:{f["id"]}' in FW and f'{NS}:{f["id"]}' not in beyond]
    comps = [f'{NS}:{c}' for c in COMP]
    pools = {int(k): v for k, v in TUNING['optional_pools'].items()}
    fpools = {int(k): v for k, v in TUNING.get('optional_fluid_pools', {}).items()}
    n = 0
    for f in sorted(glob.glob(os.path.join(REC, '*', '*.json'))):
        typ = os.path.basename(os.path.dirname(f))
        m = by_type.get(typ)
        if not m or m['generator'] or m['id'] in ('research_nexus', 'vacuum_harvester'):
            continue
        r = json.load(open(f))
        if r.get('secret') or r.get('unscaled'):
            continue
        rid = os.path.basename(f)[:-5]
        ins = [x.get('item') for x in r.get('item_inputs', []) if 'item' in x]
        outs = [o['id'] for o in r.get('item_outputs', [])] + [o['id'] for o in r.get('fluid_outputs', [])]
        fins = [x['fluid'] for x in r.get('fluid_inputs', [])]
        if set(ins) & set(outs) or set(fins) & set(outs):
            continue                   # loops (seeds, catalytic and self-replicating recipes) stay as they are
        tier = TIER.get(m['id'], 1)
        big = bool(m['multiblock'])
        # the wave at which this recipe could first run: its machine, its inputs
        rw = W.get(f'{NS}:{m["id"]}', 0)
        for x in r.get('item_inputs', []):
            if 'item' in x: rw = max(rw, W.get(x['item'], 0))
        for x in r.get('catalysts', []):
            if 'item' in x: rw = max(rw, W.get(x['item'], 0))
        for x in fins: rw = max(rw, FW.get(x, 0))
        is_beyond = any(o in beyond for o in outs)
        cap_i = m['inputs'] - len(r.get('catalysts', []))
        n_opt = _t('optional_multiblock' if big else 'optional_single', tier)
        want_i = len(r.get('item_inputs', [])) + _t('extra_items_multiblock' if big else 'extra_items_single', tier)
        room = max(0, cap_i - n_opt)
        want_i = min(room, want_i)
        have = set(ins) | set(outs)
        added = []
        # 1) this tier's standard component (and the previous one on big machines), if obtainable before the recipe
        for k in ([tier - 1, tier - 2] if big else [tier - 1]):
            if 0 <= k < len(comps) and comps[k] not in have and W.get(comps[k], 1 << 30) < rw and len(ins) + len(added) < want_i:
                added.append(comps[k])
                have.add(comps[k])
        # 2) the mod's own materials, preferring recent ones (close to the recipe's place in the progression)
        pool = [c for c in cand if c not in have and W.get(c, 1 << 30) < rw]
        pool.sort(key=lambda c: (-(W[c]), _h(rid, c)))
        recent, older = pool[:24], pool[24:]
        older.sort(key=lambda c: _h(rid, 'o', c))
        recent.sort(key=lambda c: _h(rid, 'r', c))
        mix = []
        for a, b in zip(recent, older + [None] * len(recent)):
            mix.append(a)
            if b: mix.append(b)
        for c in mix:
            if len(ins) + len(added) >= want_i: break
            if c in have: continue
            added.append(c)
            have.add(c)
        f_ = _t('cost_factor', tier)
        for c in added:
            dist = rw - W[c]
            base = 1 if dist <= 2 else 2 if dist <= 5 else 4
            if c in comps: base = 2 if big else 1
            r.setdefault('item_inputs', []).append({'item': c, 'count': int(min(TUNING.get('item_cap', 1024), base * f_))})
        # 3) fluids
        n_optf = _t('optional_fluid_multiblock' if big else 'optional_fluid_single', tier) if 'optional_fluid_multiblock' in TUNING else 0
        n_optf = 0 if is_beyond else max(0, min(n_optf, m['fluid_inputs'] - len(fins)))
        want_f = min(m['fluid_inputs'] - n_optf, len(fins) + _t('extra_fluids_multiblock' if big else 'extra_fluids_single', tier))
        fpool = [x for x in fcand if x not in fins and x not in outs and FW.get(x, 1 << 30) < rw]
        fpool.sort(key=lambda x: _h(rid, 'f', x))
        cap_f = int(TUNING.get('fluid_cap_multiblock', 2_048_000)) if big else int(TUNING.get('fluid_cap_single', 256_000))
        for x in fpool[:max(0, want_f - len(fins))]:
            r.setdefault('fluid_inputs', []).append({'fluid': x, 'amount': int(min(cap_f // 2, 250 * _t('fluid_factor', tier)))})
        # 4) other mods' materials, if installed
        if n_opt and not is_beyond:
            ot = _t('optional_tier', tier)
            opts = sorted(pools.get(ot, []), key=lambda x: _h(rid, 'm', x))[:n_opt]
            if opts:
                # as many as the recipe's tier calls for (round 17), not one each
                cnt = int(max(1, min(TUNING.get('item_cap', 1024), _t('optional_count', ot) * f_ / 2)))
                r['optional_inputs'] = [{'item': x, 'count': cnt} for x in opts]
        if n_optf:
            ot = _t('optional_tier', tier)
            fo = sorted(fpools.get(ot, []), key=lambda x: _h(rid, 'mf', x))[:n_optf]
            if fo:
                amt = int(max(1, min(cap_f // 4, _t('optional_fluid_amount', ot) * f_ / 2)))
                r['optional_fluid_inputs'] = [{'fluid': x, 'amount': amt} for x in fo]
        # 5) time
        r['duration'] = int(max(1, round(r.get('duration', 200) * _t('duration_factor', tier))))
        r['expanded'] = True        # informational; ignored by the game
        json.dump(r, open(f, 'w'), indent=2)
        n += 1
    print('round16: recipes expanded:', n)


def finish(g):
    te = g['text_entry']
    te('basics', 'recipe_cost', 'Heavy Industry', 'coherent_wiring_harness', [
        'Every multiblock (except generators) takes $(thing)16$() kinds of item and $(thing)16$() fluids at once, and gives up to 4 of each; the smaller machines take a few more than they used to. Recipes are built to fill them: each needs many materials from all over the mod, in quantities that grow steeply with the machine\'s tier.',
        'The $(item)Engineering Components$() - from the Coherent Wiring Harness (tier 1) to the Omnilex Seal (tier 8) - are the standard parts: each tier\'s recipes use its own and the one before. Each is made in a machine of its tier from that tier\'s plate and processor.',
        'Some recipes also ask for materials from other mods in your pack (Mekanism, AE2, GregTech, Draconic Evolution, Botania, Malum, Astral Sorcery...). These appear only if that mod is installed. JEI always shows exactly what is needed.',
        'Pack makers: the common config $(thing)recipeIngredientMultiplier$() scales every machine recipe\'s inputs (then /reload); KubeJS or a datapack can edit or replace any recipe.'],
        sort=4, spot=COMP)
