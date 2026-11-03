"""
Round 43 (1.3.6): the Eschaton Foundry, the Forge's global wait, tooltip corrections, and the larger late/endgame
multiblocks (round43_shapes).

* The reborn Core's Forge now waits three minutes after ANY forging (ForgeDrift.ANY), not only after its six drifting
  recipes. The Foundry is the way to make the Forge's last materials in bulk:
* the Eschaton Foundry makes the Absolute Singularity, the Echo's replay and the Eschaton Cells (moved here from the
  Pleroma Reconstitutor, which keeps the Omnipotent Ingot, the Crystal of Transcendence and the Shards) and the Eschaton
  Core - at a far heavier cost than the Forge, and each only for an owner who has forged that item by hand at least once
  (ForgeAttainment).
* Its physics is the Bekenstein bound: an object that holds S bits within radius R needs energy E ≥ S·ħc·ln2/(2πR) -
  and if R falls inside the Schwarzschild radius 2GE/c⁴ it is a black hole, not a forging.
"""
import json
import math
import os
import re

import shapes
import round43_shapes  # noqa: F401 - registers the reworked shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))

ITEMS, BLOCKS, FLUIDS = [], [], []
RESEARCH = []

# =================================================================================== THE ESCHATON FOUNDRY
MACHINE_TEXT = {
    'eschaton_foundry': ('Eschaton Foundry', 'A sphere of Pleroma glass on a waist of Omnipotence casing, ringed in Logos coil and watched by four '
                                             'towers. It packs as much as the laws allow into as little space as they allow - right up to the '
                                             'edge of becoming a black hole - and repeats what you once forged by hand.'),
}
MACHINE_CAT = {'eschaton_foundry': 'endgame'}
MACHINE_RESEARCH = {'eschaton_foundry': 'the_rekindling'}
TIER = {'eschaton_foundry': 5}
MC = {'eschaton_foundry': (['LEL', 'APA', 'LOL'], {'L': 'logos_coil_block', 'E': 'transcendental_echo', 'A': 'absolute_frame_casing',
                                                    'P': 'pleroma_glass', 'O': 'omnipotence_casing'})}
EXTRA = {'eschaton_foundry': 'Makes the $(item)Absolute Singularity$(), the $(item)Echo of the Transcendental$(), $(item)Eschaton Cells$() and the '
                             '$(item)Eschaton Core$() - but only what its owner has forged by hand in the Core\'s Forge at least once. It costs far '
                             'more than the Forge, and it never has to wait.'}

_r12.model('eschaton_foundry', 'Eschaton Foundry',
           'Information takes room. Bekenstein\'s bound says an object of radius R and energy E holds at most S = 2πRE/(ħc·ln 2) bits - so the '
           'densest things need the most energy in the least space. But squeeze energy E inside its Schwarzschild radius 2GE/c⁴ and it becomes a '
           'black hole. The richest forgings sit just outside that edge.',
           'The vacuum fluctuates: the energy actually held varies by ±2%·d.',
           [_P('lgE', 'Field energy (log₁₀ J)', '', 30, 48, 0.01, 40, 'The energy poured into the forging volume, as a power of ten joules.'),
            _P('lgR', 'Forging radius (log₁₀ m)', '', -6, 4, 0.01, 1, 'The radius it is held in, as a power of ten metres.')],
           [_D('E', 'Energy held', 'J', '10^lgE*(1 + 0.02*d)', 'E·(1 + 0.02·d)', 'The energy actually in the volume.'),
            _D('R', 'Radius', 'm', '10^lgR', 'R', 'The radius of the forging volume.'),
            _D('Rs', 'Schwarzschild radius', 'm', '2*6.674e-11*E/8.0776e33', 'R_s = 2GE/c⁴', 'Squeezed inside this, the energy is a black hole.'),
            _D('x', 'R / R_s', '', 'R/Rs', 'R/R_s', 'How far outside its own horizon the forging is. It must stay above 1.'),
            _D('lgS', 'Bekenstein bound (log₁₀ bits)', '', 'log10(R) + log10(E) + 26.458', 'S = 2πRE/(ħc·ln 2)',
               'The most information the volume can hold, as a power of ten bits.')],
           [_C('collapse', 'damage', 'x - 1.05', 'The forging volume is collapsing behind its own horizon', 'R > 1.05·R_s'),
            _C('overheat', 'damage', '1200 - T', 'The Foundry is overheating', 'T < 1200 K')],
           rate='clamp(1 + 0.5*(lgS - Sq), 0, 3)', rate_formula='rate = 1 + ½(log S − log S_needed)',
           rate_desc='The more room the bound leaves above what the forging needs, the faster it goes.',
           heat='10 + 3*(lgE - 30)', idle_heat='4', rvars=[_V('Sq', 'Information needed (log₁₀ bits)', 70)], kappa=80, heat_capacity=120)

EF = 'eschaton_founding'
# (name, item inputs, fluid inputs, output, energy FE/t, ticks, log10 bits needed). Inputs are final amounts (unscaled): the
# first three are the Pleroma Reconstitutor's recipes as they were; the Eschaton Core costs two to four times the Forge.
FOUNDRY = [
    ('absolute_singularity', [('omnipotent_ingot', 512), ('micro_singularity', 512), ('absolute_infinity', 128), ('transcendence_crystal', 128),
                              ('axiom_of_choice', 16), ('heat_death_remnant', 64), ('spacetime_singularity', 16), ('white_dwarf_core', 64),
                              ('epsilon_naught', 32), ('morphogenic_serum', 64), ('mathematics_singularity', 16)],
     [('pleroma', 2048000), ('reheating_plasma', 64000), ('ionic_liquid', 64000), ('hypernova_plasma', 64000)], ('absolute_singularity', 1),
     2000000000, 2400, 76),
    ('transcendental_echo', [('omnipotence_shard', 192), ('pleroma_crystal', 768), ('logos_thread', 768), ('absolute_singularity', 32)],
     [('pleroma', 2048000)], ('transcendental_echo', 1), 2000000000, 3200, 78),
    ('eschaton_cells', [('eschaton_core', 16), ('omnipotent_ingot', 384), ('transcendence_crystal', 96), ('absolute_singularity', 64),
                        ('omnipotence_shard', 128)],
     [('pleroma', 2048000)], ('eschaton_cell', 4), 2000000000, 2400, 74),
    ('eschaton_core', [('absolute_singularity', 48), ('transcendence_crystal', 48), ('omnipotent_ingot', 192), ('grand_unified_core', 24),
                       ('resolved_anomaly', 12)],
     [('pleroma', 2048000), ('hypernova_plasma', 128000)], ('eschaton_core', 1), 2000000000, 3600, 77),
]
MOVED = ['reconstitute_absolute_singularity', 'replay_the_echo', 'reconstitute_eschaton_cells']   # old Reconstitutor recipe files

_r12.WINDOWS['eschaton_foundry'] = {'r43_' + n: ({'Sq': s}, [('lgS', s, s + 4)]) for n, *_, s in FOUNDRY}


def eschaton_foundry():
    """31 x 30 x 31: a stepped pedestal, a waist of Omnipotence casing, a banded sphere of Pleroma glass and Absolute frame
    (hollow), an equatorial Logos coil ring joined to it by eight spokes, and four diagonal towers with glass crowns."""
    n, c = 31, 15
    s = shapes.Shape(n, 30, n)
    s.disc(c, c, 0, 15, '2', r_in=12.5)                    # pedestal: two rings and a solid heart (the sphere stands on it)
    s.disc(c, c, 0, 7.5, '2')
    s.disc(c, c, 1, 12.5, '2', r_in=10.5)
    s.disc(c, c, 1, 6.5, '2')
    for k in range(8):                                     # the pedestal's eight ribs
        a = math.radians(22.5 + 45 * k)
        for r in range(7, 15):
            s.put(c + math.cos(a) * r, 0, c + math.sin(a) * r, '2')
        for r in range(6, 12):
            s.put(c + math.cos(a) * r, 1, c + math.sin(a) * r, '2')
    for y, r in ((2, 5.5), (3, 4.5), (4, 3.5), (5, 3.5), (6, 4.5)):
        s.disc(c, c, y, r, '*')                            # the waist
    cy, R = 16, 9.0
    for y in range(cy - 9, cy + 10):                       # banded sphere: frame, glass, frame...
        ch = '2' if (y - cy) % 3 == 0 else '8'
        s.sphere(c, cy, c, R, ch, shell=0.55, filter=lambda x, yy, z, y=y: yy == y)
    s.sphere_air(c, cy, c, R - 0.6)
    s.great_circle(c, cy, c, 12.5, '&', 'y')               # equatorial Logos ring
    s.great_circle(c, cy, c, 12.0, '&', 'y')
    for k in range(8):                                     # spokes
        a = math.radians(45 * k)
        for r in range(9, 12):
            s.put(c + math.cos(a) * r, cy, c + math.sin(a) * r, '2')
    for sx in (-1, 1):                                     # four towers on the diagonals
        for sz in (-1, 1):
            tx, tz = c + sx * 11, c + sz * 11
            s.column(tx, tz, 2, 24, '2')
            for y in range(2, 25, 4):
                s.put(tx - sx, y, tz, '2'); s.put(tx, y, tz - sz, '2')
            s.put(tx, 25, tz, '8'); s.put(tx, 26, tz, '8'); s.put(tx, 27, tz, '*')
    s.put(c, cy + 10, c, '*')                              # crown of the sphere
    s.column(c, c, cy + 11, 29, '&')
    s.center = (c, cy, c)
    s.ports('2', '7', 8, y=0)
    s.v[(c, 1, c - 12)] = 'C'
    s.ctrl = (c, 1, c - 12)
    return s


shapes.BUILDERS['eschaton_foundry'] = eschaton_foundry

# =================================================================================== TOOLTIP CORRECTIONS
LORE_FIX = {
    'block.theoreticalsciences.omnipotence_casing.lore':
        'Material that very nearly refuses to be broken: only a diamond pick will do it, and slowly. Cast from Omnium in the Transcendent '
        'Plasma Forge; the last structures are framed in it.',
    'item.theoreticalsciences.omniscience_catalyst.lore':
        'Every singularity there is, fused around a Theory of Everything. Holding it, you know everything - briefly.',
    'block.theoreticalsciences.chronosteel_block.lore':
        'Forty chronosteel ingots fused into one block around an empty set: a metal whose atoms keep slightly different time from their '
        'surroundings.',
    'block.theoreticalsciences.spacetime_alloy_block.lore':
        'Forty spacetime alloy ingots pressed into one block. The alloy bends spacetime around itself slightly, so the block\'s edges never '
        'look quite square.',
    'block.theoreticalsciences.cosmic_neutronium_block.lore':
        'Forty ingots of cosmic neutronium fused into a block denser than the inside of a neutron star. It casts a faint, cold shadow even in '
        'full light.',
    'block.theoreticalsciences.cherenkov_quartz_block.lore':
        'Forty pieces of Cherenkov quartz fused into a single block that glows the soft blue of particles outrunning light in a medium.',
    'block.theoreticalsciences.core_magnetite_ore.lore':
        'Core Magnetite Ore. Found in the Planetary Interior, between Y 1 and 48. Gives Magnetite when mined. Needs a Moissanite Drill.',
    'item.theoreticalsciences.higgs_catalyst.lore':
        'A pinned Higgs-field bubble. Spent in making spin foam, pulsar crystals and wormhole throats.',
    'item.theoreticalsciences.spacetime_catalyst.lore':
        'A self-sustaining knot of curved spacetime. Required (not consumed) to grow pocket universes and the highest processors.',
    'item.theoreticalsciences.transcendental_crystal.lore':
        'π and e, which no polynomial can reach. Mined in the Platonic Realm, or extracted from axiom stone in an Analytic Continuator.',
    'block.theoreticalsciences.cosmogenic_casing.lore':
        'A casing built to contain the birth of a universe: spin foam lattice and multiverse shards folded around a rift frame casing.',
    'block.theoreticalsciences.transfinite_casing.lore':
        'A casing built to hold infinities: aleph-null shards over reality lattice casing, with a Russell set and a Banach-Tarski sphere '
        'worked into the seams.',
    'block.theoreticalsciences.omnilex_casing.lore':
        'The casing of the final machine: crystallised law and arbor heartwood bound over a transfinite casing. It looks like wood and glass '
        'and law at the same time.',
    'block.theoreticalsciences.pleroma_confluence.lore':
        'Fills with a million mB of water, lava and every fluid of the mod, plus 64,000 mB of every mod\'s Essence, and condenses them into '
        'Pleroma.',
    'block.theoreticalsciences.convergence_engine.lore':
        'Does in a box what the Convergence Altar does with pedestals and patience: every ritual but the Breached Anomaly Engine, fed by pipes '
        'like any machine.',
    'block.theoreticalsciences.breached_anomaly_engine.lore':
        'Ruins of a machine that pulled creatures out of nothing. Built anew at the Convergence Altar, it grows any creature whose genome it is '
        'shown.',
    'item.theoreticalsciences.photonic_lens.lore':
        'Focuses light onto wafers: never used up in lithography, though many finer optics are built from it.',
    'item.theoreticalsciences.eternity_catalyst.lore':
        'A crystal that outlasts the universe. Some of the final processes only need it present; others use it up.',
    'block.theoreticalsciences.zero_point_extractor.lore':
        'Burns Casimir plates, exotic matter, metamaterial sheets, spin foam and mirror processors to pull energy out of the vacuum.',
    'item.theoreticalsciences.transfinite_weave.lore':
        'A weave with more threads than any finite count. Aleph-one and Absolute Infinity are stitched on it.',
    'block.theoreticalsciences.lead_hazard_panel.lore':
        'A lead panel striped for danger: decoration for shielded rooms, and it stops gamma rays like any lead.',
    'item.theoreticalsciences.assembly_matrix.lore':
        'The frame femtoscale parts are mounted on: the Femtoscale Assembly Line and the White Hole Emitter build with it.',
    'item.theoreticalsciences.eschaton_core.lore':
        'The heart of every last thing. Forged by hand in the reborn Core - and once you have, the Eschaton Foundry can make more.',
}


def _numwords(n):
    return f'{n:,}'


def _singularity_lore(g):
    """Each singularity's tooltip says how many go into it - read from its own compaction recipe, so it can never drift again."""
    out = {}
    for f in __import__('glob').glob(os.path.join(g['REC'], 'compaction', '*.json')):
        r = json.load(open(f))
        res = r.get('result', {}).get('id', '').split(':')[-1]
        key = f'item.{NS}.{res}.lore'
        old = g['LANG'].get(key)
        if not old or 'count' not in r:
            continue
        m = re.match(r'^[\d,]+ ', old)
        if m:
            out[key] = _numwords(r['count']) + ' ' + old[m.end():]
    return out


LANG = {
    'status.theoreticalsciences.not_yet_forged': 'Forge it once by hand first',
    'message.theoreticalsciences.forge_cooldown_all': 'The Core re-seeds after every forging: %s:%s until it can compile again.',
    'jei.theoreticalsciences.forge_wait': 'After any forging the Forge waits 3 minutes before it compiles again.',
}
LANG_LATE = {
    'jei.theoreticalsciences.forge_laws': 'Compile at the rebuilt Core: tune its laws. 3 min re-seed after any forging.',
    'jei.theoreticalsciences.forge_laws_drift': 'Compile at the rebuilt Core. Laws drift after each forging; 3 min re-seed. A Reconstitutor or Eschaton Foundry makes it too.',
}


def _recipes(g):
    REC = g['REC']
    for old in MOVED:
        p = os.path.join(REC, 'pleroma_reconstitution', old + '.json')
        if os.path.exists(p):
            os.remove(p)
    for name, ins, fins, out, fe, t, _s in FOUNDRY:
        g['mr']('r43_' + name, EF, ins, fins, [out], [], energy=fe, duration=t, research='the_rekindling', catalysts=['rekindled_spark'])
        p = os.path.join(REC, EF, 'r43_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        g['w'](p, r)


def apply(g):
    g['LANG'].update(LANG)
    _recipes(g)
    g['tag']('item', NS, 'beyond_omnipotence', [g['q']('eschaton_foundry')])


def apply_late(g):
    pass


def finish(g):
    g['LANG'].update(LORE_FIX)
    g['LANG'].update(LANG_LATE)
    g['LANG'].update(_singularity_lore(g))
    te = g['text_entry']
    te('endgame', 'r43_foundry', 'The Eschaton Foundry', 'eschaton_foundry', [
        'The reborn Core\'s Forge makes the last materials by hand, and after every forging it must re-seed its universe: three minutes before '
        'it compiles anything again. The $(item)Eschaton Foundry$() makes four of them in bulk - the $(item)Absolute Singularity$(), the '
        '$(item)Echo of the Transcendental$(), $(item)Eschaton Cells$() and the $(item)Eschaton Core$() - with a Rekindled Spark resting in it.',
        'It costs far more than the Forge, and it only repeats: each thing it makes, its owner must have forged by hand at least once. Until '
        'then it reports $(thing)Forge it once by hand first$(). (The Pleroma Reconstitutor still makes Omnipotent Ingots, Crystals of '
        'Transcendence and Omnipotence Shards.)',
        'Its physics is $(thing)Bekenstein\'s bound$(): a volume of radius R holding energy E can store at most S = 2πRE/(ħc ln 2) bits. Each '
        'forging needs a certain amount of information packed in - more energy, or more room. But the radius must stay outside the '
        'Schwarzschild radius 2GE/c⁴, or the energy is a black hole. The best forgings sit just outside that edge.'],
        adv='the_rekindling', sort=63, spot=['eschaton_foundry', 'eschaton_core'])
