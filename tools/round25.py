"""
Round 25.

* The Autonomous Extreme Assembler (extreme/AutoAssembler*): an Extreme Assembly Table without a player, for AE2
  (Pattern Provider + processing pattern), pipes or hoppers. Items go in, it finds the recipe and lays them out,
  the result waits to be pulled out.
* Multiblock stabilisers (machine/tuning/StabiliserItem): three engineered tiers that cut drift (how far and how
  often), containment wear and damage; the fourth, the Equilibrium Lattice, only from the rebuilt Core's Forge, makes
  a multiblock perfect: no drift, no wear, no quench, no fault - completely hands off.
"""
import json
import os

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('phase_locked_stabiliser', 'Phase-Locked Stabiliser', 'chip', '#102438', '#60c0ff',
       'Locks a multiblock\'s fields to a reference oscillator. Drift comes slower and smaller.', special='stabiliser1', stack=16),
    _I('adaptive_feedback_core', 'Adaptive Feedback Core', 'orb', '#142a20', '#70ffb0',
       'Learns how its machine wanders and leans against it before it does.', special='stabiliser2', stack=16),
    _I('predictive_lattice_governor', 'Predictive Lattice Governor', 'lattice', '#261440', '#c080ff',
       'Simulates the machine a minute ahead and corrects what has not happened yet.', special='stabiliser3', stack=16),
    _I('equilibrium_lattice', 'Equilibrium Lattice', 'crystal', '#2a2410', '#fff0a0',
       'Forged with Omnipotence: a law that says this machine does not change. It does not.', special='stabiliser4', stack=1),
]
BEYOND = ['equilibrium_lattice']

LANG = {
    'block.theoreticalsciences.autonomous_extreme_assembler': 'Autonomous Extreme Assembler',
    'message.theoreticalsciences.auto_assembler.made': 'Assembling: %s',
    'message.theoreticalsciences.auto_assembler.no_recipe': 'Waiting: what it holds makes no Extreme recipe (yet)',
    'message.theoreticalsciences.auto_assembler.full': 'Output full: pull the result out',
    'message.theoreticalsciences.auto_assembler.idle': 'Idle: push the ingredients of one craft in',
    'message.theoreticalsciences.stabiliser_present': 'This machine already has a %s (or better).',
    'message.theoreticalsciences.stabiliser_installed': '%s installed: %s%% less drift, %s%% less wear and damage.',
    'message.theoreticalsciences.stabiliser_perfect': '%s installed: this machine no longer drifts, wears, quenches or faults.',
    'message.theoreticalsciences.stabiliser_report': 'Stabiliser: %s',
    'tooltip.theoreticalsciences.stabiliser': '%s%% less drift, %sx longer between drift steps, %s%% less wear and damage',
    'tooltip.theoreticalsciences.stabiliser_perfect': 'Perfect: no drift, no wear, no quench, no fault. Completely hands off.',
    'tooltip.theoreticalsciences.stabiliser_use': 'Use on a multiblock controller to install. A better one replaces it (and hands it back).',
}


def apply(g):
    w, mr, REC, D, A, shaped = g['w'], g['mr'], g['REC'], g['D'], g['A'], g['shaped']
    g['LANG'].update(LANG)
    # ---- the Autonomous Extreme Assembler: block files and its recipe (the 9x9 rule blows it up to the big grid)
    bid = 'autonomous_extreme_assembler'
    w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': f'{NS}:block/{bid}'}}})
    w(os.path.join(A, 'models', 'block', bid + '.json'), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': f'{NS}:block/{bid}_top', 'bottom': f'{NS}:block/{bid}_bottom', 'side': f'{NS}:block/{bid}_side'}})
    w(os.path.join(A, 'models', 'item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})
    w(os.path.join(D, 'loot_table', 'blocks', bid + '.json'), {'type': 'minecraft:block', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{bid}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    mp = os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json')
    t = json.load(open(mp))
    if f'{NS}:{bid}' not in t['values']:
        t['values'].append(f'{NS}:{bid}')
    w(mp, t)
    shaped(bid, ['PQP', 'QTQ', 'PQP'], {'P': 'superconducting_coil', 'Q': 'quantum_processor', 'T': 'extreme_assembly_table'}, bid)
    # ---- stabilisers 1-3 (4 is forged at the Core: transcend/CoreForge)
    mr('phase_locked_stabiliser', 'nanofabrication', [('field_calibrator', 2), ('superconducting_coil', 8), ('quantum_processor', 4),
                                                      ('chronon_capacitor', 4)], [], [('phase_locked_stabiliser', 1)], [],
       65536, 600, 'field_engineering')
    mr('adaptive_feedback_core', 'femtofabrication', [('phase_locked_stabiliser', 2), ('general_ai_core', 4), ('topological_processor', 8),
                                                      ('error_correction_lattice', 8), ('stabiliser_matrix', 4)], [], [('adaptive_feedback_core', 1)], [],
       1048576, 1200, 'general_intelligence')
    mr('predictive_lattice_governor', 'femto_assembly', [('adaptive_feedback_core', 2), ('superintelligence_core', 4), ('hyperdimensional_processor', 8),
                                                         ('closed_timelike_curve', 4), ('autonomous_calibrator', 1)], [], [('predictive_lattice_governor', 1)], [],
       16777216, 2400, 'superintelligence')
    tf = os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')
    t = json.load(open(tf))
    for b in BEYOND:
        if f'{NS}:{b}' not in t['values']:
            t['values'].append(f'{NS}:{b}')
    w(tf, t)


def finish(g):
    g['text_entry']('basics', 'stabilisers', 'Stabilisers', 'phase_locked_stabiliser', [
        'Every multiblock drifts: its ideal setting (or, for the physics machines, the disturbance it fights) wanders while it runs, and its containment wears. A $(item)Stabiliser$() used on the controller fights that for good.',
        '$(li)$(item)Phase-Locked Stabiliser$(): 40%% less drift, 1.5x longer between drift steps, 30%% less wear and damage.$(li)$(item)Adaptive Feedback Core$(): 65%% less drift, 2.5x longer between steps, 55%% less wear.$(li)$(item)Predictive Lattice Governor$(): 88%% less drift, 4x longer between steps, 80%% less wear.'.replace('%%', '%'),
        'Only one perfect stabiliser exists: the $(item)Equilibrium Lattice$(), forged at the rebuilt Omnipotence Core. A machine holding it never drifts, never wears, never quenches and never faults - set it once and leave it. A better stabiliser replaces a lesser one, and the old one is handed back.'],
        sort=4, spot=['phase_locked_stabiliser', 'adaptive_feedback_core', 'predictive_lattice_governor', 'equilibrium_lattice'])
    g['text_entry']('basics', 'auto_assembler', 'Autonomous Extreme Assembler', 'autonomous_extreme_assembler', [
        'An Extreme Assembly Table that needs nobody. Push the ingredients of one craft in from any side - an AE2 $(thing)Pattern Provider$() with a processing pattern (inputs: everything the recipe takes; output: the result), pipes, hoppers - and it finds the Extreme recipe they make, lays them out itself and crafts. The result (and anything a recipe leaves behind) waits in its output; pull it out from any side.',
        'Every Extreme recipe works, including the ones that follow the game (the Omniscience Catalyst, singularity clusters, specimen bundles, the Compendium and the Harvest). Those are matched by exactly what was delivered, so give their patterns blocking mode. Use the block to see what it is doing.'],
        sort=5, spot=['autonomous_extreme_assembler'])
