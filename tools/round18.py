"""
Round 18 - no dead ends.

* The progression checker (chaincheck.py) now also follows research: a machine recipe needs its research, research needs
  its prerequisites, its items, the symbols of its derivation and Insight. It also checks the world after the Shutdown
  Sequence (TS_PHASE=sundered): the Transcendental must be rebuildable from what is left.
* Insight was finite until the Matrioshka Brain (observations give about a thousand; research costs about nineteen
  thousand). Thought experiments - one per machine tier - now turn materials into Insight for the machine's owner.
* The Transcendental's own blocks can be remade after the Sundering (Omnipotence Casing, Transcendent Glass and Stone).
* (Beyond Omnipotence cannot be copied: a watch over other mods' creative duplicators. Removed in 1.1.0 - it did not hold
  in testing, and the design leaves scarcity to the Core never making those things.)
"""
import json
import os

NS = 'theoreticalsciences'

# tier: (machine, insight per run, extra inputs)
THOUGHT = {
    1: ('quantum_entangler', 10, [('minecraft:paper', 16), ('entangled_photon_pair', 4)]),
    2: ('neural_synthesizer', 25, [('minecraft:paper', 32), ('qubit', 4)]),
    3: ('holographic_encoder', 60, [('minecraft:book', 8)]),
    4: ('graviton_condenser', 120, [('minecraft:book', 16)]),
    6: ('cantor_diagonalizer', 500, [('minecraft:book', 32), ('qubit', 64)]),
    7: ('axiom_distiller', 800, [('minecraft:book', 64), ('qubit', 64)]),
}
CONTEMPLATE_INSIGHT = 300
# symbol -> the research that teaches it (the item that used to teach it is only made after the research that needs it)
GLYPH_FIX = {'Tc': 'casimir_effect', 'gs': 'string_theory', '☉': 'dark_sector'}          # the Matrioshka Brain (tier 5)

LANG = {
    'jei.theoreticalsciences.insight': '+%s Insight',
}


def apply(g):
    mr, LANG_, M, TIER_PLATE, PROC, w = g['mr'], g['LANG'], g['M'], g['TIER_PLATE'], g['PROC'], g['w']
    LANG_.update(LANG)
    by_id = {m['id']: m for m in M}
    for t, (mid, ins, extra) in THOUGHT.items():
        m = by_id[mid]
        name = f'thought_experiment_t{t}'
        mr(name, m['recipe'], [(TIER_PLATE[t], 2), (PROC.get(t, PROC[1]), 1)] + extra, [], [], [],
           min(2_000_000_000, 512 * 6 ** t), 600 + 200 * t, g['MACHINE_RESEARCH'].get(mid) if 'MACHINE_RESEARCH' in g else None)
        f = os.path.join(g['REC'], m['recipe'], name + '.json')
        r = json.load(open(f))
        r['insight_per_run'] = ins
        w(f, r)
    f = os.path.join(g['REC'], 'matrioshka', 'contemplate.json')
    r = json.load(open(f))
    r['insight_per_run'] = CONTEMPLATE_INSIGHT
    w(f, r)


def finish(g):
    te = g['text_entry']
    te('basics', 'thought_experiments', 'Thought Experiments', 'minecraft:book', [
        'Research costs $(thing)Insight$(). Observing new machines, blocks, creatures and skies with the Spectral Analyzer gives some, once each - not nearly enough for everything.',
        'Every tier has a $(item)thought experiment$(): a recipe in one of its machines that uses up materials and gives the machine\'s owner Insight each time it finishes - 10 in the Quantum Entangler, 25 in the Neural Synthesizer, 60 in the Holographic Encoder, 120 in the Graviton Condenser, 300 in the Matrioshka Brain, 500 in the Cantor Diagonalizer and 800 in the Axiom Distiller. The owner gets it even when away. JEI shows the Insight beside the recipe.'],
        sort=5)


def _research_graph(D):
    import glob
    R = {}
    for f in glob.glob(os.path.join(D, 'ts_research', '*.json')):
        R[os.path.basename(f)[:-5]] = json.load(open(f))
    kids = {}
    for rid, d in R.items():
        for q in d.get('requires', []):
            kids.setdefault(q.split(':')[-1], set()).add(rid)

    def below(rid):
        out, todo = {rid}, [rid]
        while todo:
            for k in kids.get(todo.pop(), ()):
                if k not in out:
                    out.add(k)
                    todo.append(k)
        return out
    return R, below


def untangle(g):
    """A research that costs an item whose every recipe needs that same research (or one after it) can never be done.
    Such a recipe is moved one step earlier: to the research's own prerequisite."""
    import glob
    w, REC, D = g['w'], g['REC'], g['D']
    R, below = _research_graph(D)
    files = {}
    for f in glob.glob(os.path.join(REC, '*', '*.json')):
        r = json.load(open(f))
        outs = [o['id'] for o in r.get('item_outputs', [])] + ([r['result']['id']] if isinstance(r.get('result'), dict) and 'id' in r['result'] else [])
        for o in outs:
            files.setdefault(o, []).append((f, r))
    fixed = []
    for rid, d in sorted(R.items()):
        late = below(rid)
        for c in d.get('cost', []):
            it = c.get('item')
            makers = files.get(it, [])
            if not makers:
                continue
            gates = [(r.get('research') or '').split(':')[-1] for _, r in makers]
            if all(x and x in late for x in gates):
                f, r = min(makers, key=lambda fr: len(fr[1].get('item_inputs', [])))
                req = [q.split(':')[-1] for q in d.get('requires', [])]
                new = req[-1] if req else None
                if new: r['research'] = f'{NS}:{new}'
                else: r.pop('research', None)
                w(f, r)
                fixed.append(f'{os.path.basename(f)[:-5]}: {gates[0]} -> {new}')
    # symbols that were only learned from an item made after the research whose derivation needs them
    gp = os.path.join(D, 'ts_glyphs', 'glyphs.json')
    G = json.load(open(gp))
    for tok, rid in GLYPH_FIX.items():
        if tok in G:
            G[tok]['unlock'] = {'research': rid}
            fixed.append(f'symbol {tok}: learned from research {rid}')
    w(gp, G)
    print('round18 untangled:', fixed)
