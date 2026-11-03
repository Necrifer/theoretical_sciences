"""
Round 19.

* The Convergence Altar: an Empowerer / fusion-crafting style ritual. One item on the altar, one item on each Resonance
  Pedestal around it (up to twelve, within four blocks), energy into the altar, then a right-click or a redstone pulse.
  Hard to automate at first (every pedestal must get exactly its one item, the core goes in, the result comes out, and
  something has to give the pulse). The eight Engineering Components are now made this way. Much later the
  Convergence Engine runs the same recipes as an ordinary machine.
* The Omnipotence Core became a terminal; its Forge simulates a universe whose constants the player tunes.
* Costs: steeper item factors, a separate (much steeper) fluid factor, bigger tanks (recipe_tuning.json, scaling.py).
"""
import glob
import hashlib
import importlib
import json
import os
import sys

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))

BLOCKS = [
    dict(id='convergence_altar', name='Convergence Altar', art='emitterblock', c1='#1a1030', c2='#c080ff', kind='conv_altar', light=9, hardness=8.0, tool='iron'),
    dict(id='resonance_pedestal', name='Resonance Pedestal', art='bricks', c1='#28203a', c2='#a080ff', kind='conv_pedestal', light=6, hardness=5.0, tool='iron'),
    dict(id='harmonic_pedestal', name='Harmonic Pedestal', art='bricks', c1='#302018', c2='#ffc060', kind='conv_pedestal', light=8, hardness=8.0, tool='diamond'),
    dict(id='absolute_pedestal', name='Absolute Pedestal', art='bricks', c1='#f0ece0', c2='#ffe090', kind='conv_pedestal', light=12, hardness=12.0, tool='diamond'),
]

MACHINE_TEXT = {'convergence_engine': ('Convergence Engine',
                                       'Does in a box what the Convergence Altar does with pedestals and patience: every convergence ritual, fed by pipes like any machine.')}
MACHINE_CAT = {'convergence_engine': 'fabrication'}
ENGINE_RESEARCH = 'simulation_hypothesis'
MACHINE_RESEARCH = {'convergence_engine': ENGINE_RESEARCH}
TIER = {'convergence_engine': 6}
MC = {'convergence_engine': (['APA', 'CSC', 'APA'], {'A': 'absolute_pedestal', 'P': 'ontological_processor', 'C': 'convergence_altar', 'S': 'reality_lattice_casing'})}
EXTRA = {'convergence_engine': 'Runs every $(item)Convergence Altar$() recipe: the core and every pedestal item go into its input slots together, at four times the energy.'}

LANG = {
    'block.theoreticalsciences.convergence_altar': 'Convergence Altar',
    'block.theoreticalsciences.resonance_pedestal': 'Resonance Pedestal',
    'block.theoreticalsciences.harmonic_pedestal': 'Harmonic Pedestal',
    'block.theoreticalsciences.absolute_pedestal': 'Absolute Pedestal',
    'message.theoreticalsciences.altar_no_core': 'Put the core item on the altar first.',
    'message.theoreticalsciences.altar_no_recipe': 'Nothing converges: no recipe matches the altar and the pedestals around it.',
    'message.theoreticalsciences.altar_weak': 'These pedestals cannot carry it: every pedestal in use must be of grade %s or better.',
    'message.theoreticalsciences.altar_research': 'You do not understand this convergence yet (research needed).',
    'message.theoreticalsciences.altar_full': 'Take the last result off the altar first.',
    'message.theoreticalsciences.altar_status': 'Converging: %s%% - %s FE stored, %s FE per tick',
    'jei.theoreticalsciences.altar_core': 'On the altar',
    'jei.theoreticalsciences.altar_pedestal': 'On a pedestal (one item each)',
    'jei.theoreticalsciences.altar_tier': 'Grade %s+',
    'jei.theoreticalsciences.forge': 'Omnipotence Forge',
    'jei.theoreticalsciences.forge_laws': 'Compile at the rebuilt Core: tune its laws',
    'message.theoreticalsciences.forge_diverges': 'The simulation diverges: those laws cannot hold this thing. Tune the constants until every observable is stable.',
    'gui.theoreticalsciences.law_hint': 'Click the arrows (or the scale), or scroll; shift: x10',
    'gui.theoreticalsciences.law.0': 'G - the strength of gravity',
    'gui.theoreticalsciences.law.1': 'c - the speed of light, the limit of cause',
    'gui.theoreticalsciences.law.2': 'h-bar - the grain of action, the scale of the quantum',
    'gui.theoreticalsciences.law.3': 'alpha - the fine-structure constant, the strength of light on matter',
    'gui.theoreticalsciences.law.4': 'Lambda - the cosmological constant, the push of the vacuum',
    'gui.theoreticalsciences.law.5': 'k_B - Boltzmann\'s constant, the exchange rate of entropy',
    'gui.theoreticalsciences.observable.0': 'Stellar balance',
    'gui.theoreticalsciences.observable.1': 'Causal order',
    'gui.theoreticalsciences.observable.2': 'Quantum coherence',
    'gui.theoreticalsciences.observable.3': 'Atomic binding',
    'gui.theoreticalsciences.observable.4': 'Vacuum stability',
    'gui.theoreticalsciences.observable.5': 'Arrow of time',
}


def _h(*parts):
    return int(hashlib.sha256('|'.join(map(str, parts)).encode()).hexdigest()[:12], 16)


def _waves():
    sys.argv = ['chaincheck']
    os.environ['TS_LENIENT'] = '1'
    cc = importlib.reload(sys.modules['chaincheck']) if 'chaincheck' in sys.modules else importlib.import_module('chaincheck')
    os.environ.pop('TS_LENIENT', None)
    return cc.WAVE


def apply(g):
    import round16
    w, q, shaped, REC, C = g['w'], g['q'], g['shaped'], g['REC'], g['C']
    TIER_PLATE, PROC = g['TIER_PLATE'], g['PROC']
    g['LANG'].update(LANG)
    W = _waves()
    # the altar and its pedestals
    shaped('convergence_altar', ['APA', 'QCQ', 'NNN'], {'A': 'minecraft:amethyst_block', 'P': PROC[1], 'Q': 'cherenkov_quartz', 'C': 'minecraft:crying_obsidian',
                                                         'N': TIER_PLATE[1]}, 'convergence_altar')
    shaped('resonance_pedestal', [' Q ', ' N ', 'NAN'], {'Q': 'cherenkov_quartz', 'N': TIER_PLATE[1], 'A': 'minecraft:amethyst_shard'}, 'resonance_pedestal', 2)
    shaped('harmonic_pedestal', [' P ', 'TRT', 'TTT'], {'P': PROC[3], 'T': TIER_PLATE[3], 'R': 'resonance_pedestal'}, 'harmonic_pedestal', 2)
    shaped('absolute_pedestal', [' P ', 'TRT', 'TTT'], {'P': PROC[5], 'T': TIER_PLATE[5], 'R': 'harmonic_pedestal'}, 'absolute_pedestal', 2)
    # the Engineering Components move from machines to the altar
    beyond = set()
    cand = [f'{NS}:{it["id"]}' for it in C['items'] if it.get('stack', 64) > 1 and not it.get('special') and not it['id'].endswith('_cell')
            and f'{NS}:{it["id"]}' in W and 'notes' not in it['id'] and 'dossier' not in it['id']]
    comps = [f'{NS}:{c}' for c in round16.COMP]
    made = []
    for k, cid in enumerate(round16.COMP):
        t = k + 1
        for f in glob.glob(os.path.join(REC, '*', cid + '.json')):
            os.remove(f)                                          # the old machine recipe
        cw = W.get(f'{NS}:{cid}', 0)
        core = q(TIER_PLATE[t])
        peds = [q(PROC.get(t, PROC[max(PROC)]))]
        if k > 0: peds += [comps[k - 1], comps[k - 1]]
        n = min(12, 4 + t)
        pool = [c for c in cand if c not in peds and c != core and c not in comps and W.get(c, 1 << 30) < cw]
        pool.sort(key=lambda c: (-W[c], _h(cid, c)))
        recent, older = pool[:10], pool[10:]
        recent.sort(key=lambda c: _h(cid, 'r', c))
        older.sort(key=lambda c: _h(cid, 'o', c))
        for c in (recent + older):
            if len(peds) >= n: break
            peds.append(c)
        tier = 1 if t <= 3 else 2 if t <= 6 else 3
        research = None
        r = dict(type=f'{NS}:convergence', core={'item': core}, pedestals=[{'item': p} for p in peds],
                 result={'id': f'{NS}:{cid}', 'count': 8}, energy=min(4_000_000_000_000, 4096 * 8 ** t), duration=200 + 100 * t, tier=tier)
        w(os.path.join(REC, 'convergence', cid + '.json'), r)
        made.append(r)
        # and, much later, the Convergence Engine
        e = dict(type=f'{NS}:convergence_automation', item_inputs=[{'item': core, 'count': 1}] + [{'item': p, 'count': 1} for p in peds],
                 item_outputs=[{'id': f'{NS}:{cid}', 'count': 8}], energy=int(min(2_000_000_000, 4 * r['energy'] // r['duration'])),
                 duration=r['duration'], research=f'{NS}:{ENGINE_RESEARCH}', unscaled=True)
        w(os.path.join(REC, 'convergence_automation', cid + '.json'), e)
    print('round19: convergence recipes:', len(made))


def finish(g):
    w, te, A = g['w'], g['text_entry'], g['A']
    # models: an altar table and pedestal columns
    altar = {'parent': 'minecraft:block/block', 'textures': {'all': f'{NS}:block/convergence_altar', 'particle': f'{NS}:block/convergence_altar'},
             'elements': [{'from': [0, 0, 0], 'to': [16, 4, 16], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}},
                          {'from': [2, 4, 2], 'to': [14, 10, 14], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west')}},
                          {'from': [1, 10, 1], 'to': [15, 13, 15], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
    w(os.path.join(A, 'models/block', 'convergence_altar.json'), altar)
    for pid in ('resonance_pedestal', 'harmonic_pedestal', 'absolute_pedestal'):
        m = {'parent': 'minecraft:block/block', 'textures': {'all': f'{NS}:block/{pid}', 'particle': f'{NS}:block/{pid}'},
             'elements': [{'from': [2, 0, 2], 'to': [14, 3, 14], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}},
                          {'from': [4, 3, 4], 'to': [12, 12, 12], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west')}},
                          {'from': [2, 12, 2], 'to': [14, 14, 14], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
        w(os.path.join(A, 'models/block', pid + '.json'), m)
    te('fabrication', 'convergence_altar', 'Convergence', 'convergence_altar', [
        'Some things are not made in a machine: they are $(thing)converged$(). Place a $(item)Convergence Altar$() and up to twelve $(item)Resonance Pedestals$() around it (within 4 blocks, at most one block higher or lower). Put the core item on the altar and one item on each pedestal - JEI shows which.',
        'Power the altar (it takes energy from any side), then right-click it with an empty hand or give it a $(thing)redstone pulse$(). Beams run from the pedestals; when it finishes, the pedestals are empty and the core has become the result. Take it off with an empty hand (sneak-click takes the core back instead).',
        'Bigger rituals need better pedestals: $(item)Harmonic$() (grade 2) and $(item)Absolute$() (grade 3) pedestals carry what a Resonance Pedestal cannot. Every pedestal in use must be good enough.',
        'Automating it is the trick: pipes may put the core onto the altar and take the result off, and every pedestal takes exactly one item from a pipe (it refuses more, and locks while in use). A comparator reads 15 from the altar when a result waits, 7 while it works. The $(item)Engineering Components$() are all made here.',
        'Much later the $(item)Convergence Engine$() runs every convergence recipe as an ordinary machine, at four times the energy.'],
        sort=6, spot=['convergence_altar', 'resonance_pedestal', 'harmonic_pedestal', 'absolute_pedestal'])
    te('endgame', 'forge_terminal', 'The Core Terminal', 'eschaton_core', [
        'The rebuilt Core does not simply make what lies beyond Omnipotence: it has to $(thing)simulate a universe$() in which the thing can exist. Its $(thing)FORGE$() page lists what it can forge and what that costs; beside it are the six constants of nature and six readings of the simulated universe.',
        'Each recipe needs its own universe. Every constant moves one reading strongly and a few others a little, so tune them patiently: watch the bars, bring each to the middle. When all six are stable the recipe can $(thing)COMPILE$(). The Core remembers your settings while the game runs. JEI shows every Forge recipe.'],
        sort=31)
