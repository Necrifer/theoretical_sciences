"""
Round 55 (1.14.0, "Ensembles"): the rest of the 1.1 design's Statistical Probability branch.

Stochastic Processes (Markov chain links, Poisson clocks, Brownian suspension), Statistical Mechanics (the partition
function, Maxwell-Boltzmann gas), Extreme Value Theory (large deviations; the Black Swan, 1.10.0), Ergodic Theory (the
Markov Chain Refinery: the steady state of a tuned three-state chain sets which by-product a feed becomes) and The
Measure Problem (a Boltzmann Brain, made the slow, sure way). The Confidence Interval Gauge is the counter to Variance.
"""
import json
import math
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V
import kit
from kit import I, B, FL

NS = 'theoreticalsciences'
M = NS + ':'

ITEMS = [
    I('poisson_clock', 'Poisson Clock', 'clock', '#303828', '#c0ff80',
      'A clock that ticks at random: each tick a decay in its radium, the waits between them falling off exponentially. It keeps perfect '
      'average time and never says when.'),
    I('markov_chain_link', 'Markov Chain Link', 'loop', '#404048', '#e0e0f0',
      'A link that remembers only the one before it. Chain enough of them and the past stops mattering at all.'),
    I('confidence_interval_gauge', 'Confidence Interval Gauge', 'probe', '#202830', '#80e0c0',
      'A gauge that shows not a value but a range, and how sure it is. Carried in the off hand, it steadies the luck of what you drink '
      '(no Variance).'),
    I('monte_carlo_sample_pack', 'Monte Carlo Sample Pack', 'chip', '#103020', '#40ff80',
      'A billion random points, drawn and sorted: the answer to an integral too hard to do any other way, to a part in thirty thousand.'),
    I('partition_function_matrix', 'Partition Function Matrix', 'lattice', '#281818', '#ffa080',
      'Z = Σ e^(−E/kT), summed over every state a system can be in. Everything else - energy, entropy, pressure - is a derivative of it.'),
    I('large_deviation_shard', 'Large Deviation Shard', 'shard', '#201018', '#ff4060',
      'A record of something that was not supposed to happen - and the exponentially small odds it beat.'),
    I('ergodic_mixer_core', 'Ergodic Mixer Core', 'aicore', '#202838', '#a0c0ff',
      'A mixer that visits every state there is, given time - so its average over time is its average over all of them.'),
]
BLOCKS = [
    B('markov_vat', 'Markov Vat', 'cryo', '#303040', '#c0c0ff', light=3),
    B('transition_valve', 'Transition Valve', 'injector', '#404050', '#e0e0ff', light=4),
    B('refinery_casing', 'Refinery Casing', 'casing', '#303038', '#a0a0c0'),
]
FLUIDS = [
    FL('brownian_suspension', 'Brownian Suspension', '#403830', '#c0b090', temp=300, density=1100, viscosity=1300, art='exotic'),
    FL('maxwell_boltzmann_gas', 'Maxwell-Boltzmann Gas', '#302020', '#ffa080', gas=True, temp=300, density=1, viscosity=200, art='gas'),
]

KEY_ADD = {'ѻ': ['refinery_casing'], 'ѽ': ['refinery_casing', 'interface_port'], 'ѿ': ['markov_vat'], 'ҁ': ['transition_valve']}


def markov_chain_refinery():
    """Three vats at the corners of a triangle, joined in a ring by transition valves - A to B to C and back to A - round a
    central hub, on a triangular slab."""
    s = shapes.Shape(13, 5, 13)
    c = 6
    corners = [(c + 4.5 * math.cos(a), c + 4.5 * math.sin(a)) for a in (math.pi / 2, math.pi / 2 + 2 * math.pi / 3, math.pi / 2 + 4 * math.pi / 3)]
    for x in range(13):
        for z in range(13):
            if math.hypot(x - c, z - c) <= 6.2:
                s.put(x, 0, z, 'ѻ')
    for cx, cz in corners:
        for x in range(13):
            for z in range(13):
                if math.hypot(x - cx, z - cz) <= 1.6:
                    for y in (1, 2, 3):
                        s.put(x, y, z, 'ѿ' if y < 3 or (x + z) % 2 else 'G')
    for i in range(3):
        (ax, az), (bx, bz) = corners[i], corners[(i + 1) % 3]
        for t in range(1, 10):
            f = t / 10
            x, z = ax + (bx - ax) * f, az + (bz - az) * f
            if all(math.hypot(x - qx, z - qz) > 1.6 for qx, qz in corners):
                s.put(x, 2, z, 'ҁ' if t == 5 else 'G')
    s.column(c, c, 1, 3, 'ѻ')
    s.center = (6.0, 3.5, 6.0)
    s.ports('ѻ', 'ѽ', 4, y=0)
    for z in range(13):
        if (c, 0, z) in s.v and s.v[(c, 0, z)] == 'ѻ':
            s.v[(c, 0, z)] = 'C'
            s.ctrl = (c, 0, z)
            break
    return s


shapes.BUILDERS.update({'markov_chain_refinery': markov_chain_refinery})

MACHINE_TEXT = {'markov_chain_refinery': ('Markov Chain Refinery', 'Three vats in a ring, the feed hopping between them by tuned chances. '
                                                                   'Where it spends its time - the chain\'s steady state - decides what it '
                                                                   'becomes.')}
MACHINE_CAT = {'markov_chain_refinery': 'probability'}
MACHINE_RESEARCH = {'markov_chain_refinery': 'ergodic_theory'}
TIER = {'markov_chain_refinery': 4}
MC = {'markov_chain_refinery': (['VTV', 'LSL', 'CCC'], {'V': 'markov_vat', 'T': 'transition_valve', 'L': 'markov_chain_link', 'S': 'spacetime_casing',
                                                        'C': 'refinery_casing'})}
EXTRA = {'markov_chain_refinery': 'Set the three transition chances: A to B, B to C, C to A. In a ring, the feed spends time in each vat in '
                                  'proportion to 1/(its chance of leaving): π ∝ (1/p, 1/q, 1/r). Each product needs its vat to hold at least '
                                  'three fifths of the time; thermal equilibrium needs all three nearly equal. The slowest transition sets how '
                                  'fast the chain mixes.'}

_r12.model('markov_chain_refinery', 'Markov Chain Refinery',
           'The feed hops A → B with chance p each step, B → C with q, C → A with r. However it starts, it settles into the chain\'s steady '
           'state: the share of time in each vat is proportional to 1/(its chance of leaving), π = (1/p, 1/q, 1/r)/Σ. How fast it settles is '
           'set by the slowest hop.',
           'Valve wear: the A → B chance varies by ±5%·d.',
           [_P('p', 'A → B chance', '', 0.01, 1, 0.0001, 0.5, 'Chance per step that feed in vat A moves on to B.'),
            _P('q', 'B → C chance', '', 0.01, 1, 0.0001, 0.5, 'Chance per step that feed in vat B moves on to C.'),
            _P('r', 'C → A chance', '', 0.01, 1, 0.0001, 0.5, 'Chance per step that feed in vat C moves back to A.')],
           [_D('pe', 'A → B (actual)', '', 'p*(1 + 0.05*d)', '', 'The chance the worn valve actually gives.'),
            _D('Z', 'Normaliser', '', '1/pe + 1/q + 1/r', 'Σ 1/p', '', show=False),
            _D('pA', 'Time in A', '', '(1/pe)/Z', 'π_A', 'Share of the time the feed spends in vat A.'),
            _D('pB', 'Time in B', '', '(1/q)/Z', 'π_B', 'Share of the time in vat B.'),
            _D('pC', 'Time in C', '', '(1/r)/Z', 'π_C', 'Share of the time in vat C.')],
           [],
           rate='clamp(4*min(p, min(q, r)), 0.25, 2)', rate_formula='rate = 4·min(p, q, r)', rate_desc='The slowest hop sets how fast it mixes.',
           heat='0', idle_heat='0', kappa=40, heat_capacity=40)

MR = 'markov_refining'
_r12.WINDOWS['markov_chain_refinery'] = {
    'r55_refine_rare_earths': ({}, [('pA', 0.6, 1)]),
    'r55_refine_thorium': ({}, [('pB', 0.6, 1)]),
    'r55_refine_phosphate': ({}, [('pC', 0.6, 1)]),
    'r55_thermalize': ({}, [('pA', 0.3, 0.37), ('pB', 0.3, 0.37), ('pC', 0.3, 0.37)]),
}
_r12.WINDOWS.setdefault('monte_carlo_engine', {}).update({
    'r55_sample_pack': ({'sg': 10, 'opt': 0.5}, [('err', 0, 10)]),
    'r55_partition_function': ({'sg': 20, 'opt': 0.6}, [('err', 0, 30)]),
    'r55_boltzmann_brain': ({'sg': 40, 'opt': 0.7}, [('err', 0, 15)]),
    'r55_large_deviation': ({'sg': 30, 'opt': 0.7}, [('err', 0, 20)]),
    'r55_swan_boltzmann_brain': ({'sg': 10, 'opt': 0.5}, [('err', 0, 50)]),
})
_r12.WINDOWS.setdefault('pulsed_magnet_lab', {}).update({'r55_swan_monopole': ({}, [('Bf', 80, 99)])})

RESEARCH = kit.place([
    ('stochastic_processes', 'Stochastic Processes',
     'Chance that unfolds in time: a walk with random steps, a clock with random ticks, a chain whose next state depends only on this one. '
     'Unlocks Markov Chain Links, Poisson Clocks and Brownian Suspension.',
     'markov_chain_link', None, None, 60, ['distributions'], [('normal_distribution_crystal', 2), ('dice_set', 2)]),
    ('statistical_mechanics', 'Statistical Mechanics',
     'Temperature, pressure and entropy are averages over states too many to count: each weighted by e^(−E/kT). Sum the weights and the '
     'partition function holds every property of the system.',
     'partition_function_matrix', None, None, 140, ['stochastic_processes', 'thermodynamics_of_information'],
     [('markov_chain_link', 4), ('avogadro_record', 1)]),
    ('extreme_value_theory', 'Extreme Value Theory',
     'The largest of many random draws follows its own law - and the rarest events are less rare than a bell curve says. Black swans happen.',
     'large_deviation_shard', None, None, 130, ['distributions'], [('normal_distribution_crystal', 4), ('entropy_die', 1)]),
    ('ergodic_theory', 'Ergodic Theory',
     'A system that, given time, passes through every state open to it: averaging over time is then the same as averaging over all states. '
     'Unlocks the Markov Chain Refinery and the Ergodic Mixer Core.',
     'ergodic_mixer_core', None, None, 220, ['statistical_mechanics', 'topology'], [('partition_function_matrix', 2), ('markov_chain_link', 8)]),
    ('measure_problem', 'The Measure Problem',
     'In a universe that lasts forever, everything that can fluctuate into being does, infinitely often - even a lone brain that only '
     'thinks it remembers a world. How then to say what is likely? Unlocks the deliberate assembly of a Boltzmann Brain.',
     'boltzmann_brain', None, None, 320, ['ergodic_theory', 'simulation_hypothesis'], [('ergodic_mixer_core', 1), ('large_deviation_shard', 2)]),
])

PUZZLES = {
    'stochastic_processes': dict(
        goal='A drunkard takes N steps of length a, each in a direction chosen by chance. Write how far x from the start he typically ends up.',
        name='Random walk spread', lhs='x',
        form='sqrt ( N * a ^ 2 )',
        expr='sqrt ( N * a ^ 2 )',
        ask='Find x in metres for N = 10000 steps of a = 0.7 m.',
        vars={'x': 'distance (m)', 'a': 'step length (m)', 'N': 'steps'},
        given={'a': 0.7, 'N': 10000}, unit='m',
        method='The steps are independent, so their variances add: N·a². The typical distance is the square root.',
        distractors=['2', 'π', 't']),
    'statistical_mechanics': dict(
        goal='Two states differ in energy by E. At temperature T, write the ratio r of how often the upper one is found to how often the lower '
             'one is, with Boltzmann\'s k.',
        name='Population ratio', lhs='r',
        form='1 / exp ( E / ( k * T ) )',
        expr='1 / exp ( E / ( k * T ) )',
        ask='Find r for E = 1.602e-20 J (0.1 eV), k = 1.381e-23 J/K, T = 300 K.',
        vars={'r': 'population ratio', 'E': 'energy gap (J)', 'k': 'Boltzmann constant', 'T': 'temperature (K)'},
        given={'E': 1.602e-20, 'k': 1.381e-23, 'T': 300}, unit='',
        method='Each state is weighted by e^(−E/kT); the ratio of two weights depends only on their difference.',
        distractors=['h', '2', 'N']),
    'extreme_value_theory': dict(
        goal='Draw n numbers from a standard bell curve. Write the size m the largest of them typically has.',
        name='Gumbel scaling', lhs='m',
        form='sqrt ( 2 * ln ( n ) )',
        expr='sqrt ( 2 * ln ( n ) )',
        ask='Find m for n = 1000000 draws.',
        vars={'m': 'typical maximum (σ)', 'n': 'draws'},
        given={'n': 1000000}, unit='σ',
        method='The maximum sits where the tail\'s chance, about e^(−m²/2), times n is about one.',
        distractors=['π', 'e', '3']),
    'ergodic_theory': dict(
        goal='Two copies of a chaotic system start a distance x apart, and the gap grows by a factor e^λ every second. Write the gap X after t '
             'seconds.',
        name='Lyapunov divergence', lhs='X',
        form='x * exp ( λ * t )',
        expr='x * exp ( L * t )',
        ask='Find X for x = 1e-6, L = 1 per second (for λ) and t = 10 s.',
        vars={'X': 'gap after t', 'x': 'starting gap', 'λ': 'Lyapunov exponent', 'L': 'λ (1/s)', 't': 'time (s)'},
        given={'x': 1e-6, 'L': 1, 't': 10}, unit='',
        method='A small gap grows in proportion to itself: dX/dt = λX. That is why a chaotic system forgets where it began - and mixes.',
        distractors=['2', 'π', 'n']),
    'measure_problem': dict(
        goal='Fluctuations out of equilibrium are rarer the more entropy they cost. Write how many times likelier Q a fluctuation that costs '
             's of entropy is than one costing S, with Boltzmann\'s k.',
        name='Einstein\'s inversion', lhs='Q',
        form='exp ( ( S - s ) / k )',
        expr='exp ( ( S - s ) / k )',
        ask='Find Q for S = 30, s = 10 and k = 1 (entropies in units of k).',
        vars={'Q': 'odds', 'S': 'entropy cost of the larger fluctuation', 's': 'entropy cost of the smaller', 'k': 'Boltzmann constant'},
        given={'S': 30, 's': 10, 'k': 1}, unit='',
        method='Einstein: a fluctuation that lowers the entropy by ΔS has probability ∝ e^(−ΔS/k). A brain is far cheaper than a universe.',
        distractors=['2', 'T', 'π']),
}

RECIPES = [
    ('refine_rare_earths', MR, [('monazite_sand', 2)], [], [('rare_earth_oxide', 4)], [], 2048, 600, 'ergodic_theory', 2),
    ('refine_thorium', MR, [('monazite_sand', 2)], [], [('thorium_232_pellet', 2)], [], 2048, 600, 'ergodic_theory', 2),
    ('refine_phosphate', MR, [('monazite_sand', 2)], [], [('minecraft:bone_meal', 6)], [], 2048, 600, 'ergodic_theory', 2),
    ('thermalize', MR, [], [('warm_helium', 500)], [], [('maxwell_boltzmann_gas', 500)], 2048, 800, 'statistical_mechanics', 4),
    ('sample_pack', 'monte_carlo', [('minecraft:paper', 4)], [], [('monte_carlo_sample_pack', 1)], [], 2048, 800, 'monte_carlo_methods', 4,
     ['random_number_chip']),
    ('partition_function', 'monte_carlo', [('entropy_die', 1), ('normal_distribution_crystal', 2)], [], [('partition_function_matrix', 1)], [],
     4096, 1600, 'statistical_mechanics', 12, ['random_number_chip']),
    # the slow, sure way to what the Frontier gives by chance
    ('boltzmann_brain', 'monte_carlo', [('ergodic_mixer_core', 1), ('entropy_die', 8)], [], [('boltzmann_brain', 1)], [], 16384, 6000,
     'measure_problem', 60, ['random_number_chip']),
    ('brownian_suspension', 'chemical_synthesis', [('minecraft:clay_ball', 2)], [('minecraft:water', 1000)], [], [('brownian_suspension', 1000)],
     512, 200, 'stochastic_processes', 0),
    # rare-event sampling: tilt the odds toward the tail, then weigh each sample back - the sure, slow way to a large deviation
    ('large_deviation', 'monte_carlo', [('entropy_die', 4), ('normal_distribution_crystal', 8)], [], [('large_deviation_shard', 1)], [], 8192, 3000,
     'extreme_value_theory', 15, ['random_number_chip']),
    # the Black Swan Feather: proof the improbable happens - a shortcut through the slowest recipes
    ('swan_boltzmann_brain', 'monte_carlo', [('ergodic_mixer_core', 1), ('black_swan_feather', 1)], [], [('boltzmann_brain', 1)], [], 16384, 600,
     'measure_problem', 20, ['random_number_chip']),
    ('swan_monopole', 'pulsed_field', [('magnetar_crust_fragment', 1), ('black_swan_feather', 1)], [], [('magnetic_monopole', 1)], [], 16384, 1600,
     'magnetic_monopoles', 12),
    ('galton_large_deviation', 'galton_sorting', [('entropy_die', 1)], [], [('normal_distribution_crystal', 1), ('large_deviation_shard', 1)], [],
     512, 400, 'extreme_value_theory', 2, [], [1.0, 0.05]),
]
SHAPED = [
    ('poisson_clock', [' R ', 'GCG', ' R '], {'R': 'radium_salt', 'G': 'geiger_tube', 'C': 'minecraft:clock'}, 'poisson_clock', 1),
    ('markov_chain_link', ['C', 'R', 'C'], {'C': 'minecraft:chain', 'R': 'random_number_chip'}, 'markov_chain_link', 4),
    ('confidence_interval_gauge', [' N ', 'PCP', ' N '], {'N': 'normal_distribution_crystal', 'P': 'posterior_tablet', 'C': 'minecraft:compass'},
     'confidence_interval_gauge', 1),
    ('ergodic_mixer_core', ['LDL', 'DPD', 'LDL'], {'L': 'markov_chain_link', 'D': 'entropy_die', 'P': 'partition_function_matrix'}, 'ergodic_mixer_core', 1),
    ('markov_vat', ['IGI', 'G G', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass'}, 'markov_vat', 4),
    ('transition_valve', ['ILI', 'LRL', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'markov_chain_link', 'R': 'random_number_chip'}, 'transition_valve', 1),
    ('refinery_casing', ['III', 'ISI', 'III'], {'I': 'minecraft:iron_ingot', 'S': 'spacetime_casing'}, 'refinery_casing', 8),
]
SHAPELESS = []


def apply(g):
    kit.write_puzzles('r55', PUZZLES)
    kit.write_research(g, RESEARCH)
    kit.recipes(g, 'r55_', RECIPES)
    kit.crafting(g, 'r55_', SHAPED, SHAPELESS)


def apply_late(g):
    kit.pickaxe(g, BLOCKS)


def finish(g):
    te = g['text_entry']
    te('probability', 'r55_stochastic', 'Stochastic Processes', 'markov_chain_link', [
        'A drunkard\'s walk of N steps of length l ends about $(thing)l√N$() from where it began - the reason Brownian grains wander as they do. '
        'A $(item)Markov Chain Link$() remembers only the one before it; a $(item)Poisson Clock$() ticks at random, each tick a decay in its '
        'radium. Clay stirred into water makes a $(fluid)Brownian Suspension$().',
        'The $(item)Confidence Interval Gauge$() shows a range and how sure it is: carried in the off hand, it cancels Variance.'],
        adv='stochastic_processes', sort=20, spot=['markov_chain_link', 'poisson_clock', 'confidence_interval_gauge'])
    te('probability', 'r55_statmech', 'Statistical Mechanics', 'partition_function_matrix', [
        'A state of energy E is found $(thing)e^(−E/kT)$() times as often as one of zero: the Boltzmann factor. Summed over every state, the '
        'factors make the partition function, and everything else - energy, entropy, pressure - is a derivative of it. The Monte Carlo '
        'Engine samples one into a $(item)Partition Function Matrix$(); a billion sorted samples make a $(item)Monte Carlo Sample Pack$().'],
        adv='statistical_mechanics', sort=21, spot=['partition_function_matrix', 'monte_carlo_sample_pack'])
    te('probability', 'r55_extremes', 'Extreme Values', 'large_deviation_shard', [
        'The largest of n draws from a bell curve is about $(thing)√(2 ln n)$() standard deviations: five for a million draws. Rare events '
        'are rarer than average ones - but not as rare as a bell curve alone says. A Galton Board now and then sorts an Entropy Die into a '
        '$(item)Large Deviation Shard$(); the Monte Carlo Engine, tilting its samples toward the tail and weighing them back, makes one every '
        'time - slowly. A $(item)Black Swan Feather$() - proof the improbable happens - is a shortcut: with it the Pulsed Magnet Lab finds a '
        'monopole every time, and an Ergodic Mixer Core becomes a Boltzmann Brain in a tenth of the time.',
        'The $(thing)Black Swan$() itself comes to the Overworld only after a long time - three days or more - in which nothing improbable has '
        'happened to you.'],
        adv='extreme_value_theory', sort=22, spot=['large_deviation_shard', 'black_swan_feather', 'black_swan_spawn_egg'])
    te('probability', 'r55_refinery', 'The Markov Chain Refinery', 'markov_chain_refinery', [
        'Three vats in a ring, A → B → C → A, the feed hopping on by chances p, q and r. However it starts, it settles into the steady state: '
        'time in each vat ∝ $(thing)1/(its chance of leaving)$(). Give a vat three fifths of the time and monazite sand comes out as that '
        'vat\'s product: rare earths, thorium, or phosphate. Make all three equal and warm helium thermalises into $(fluid)Maxwell-Boltzmann '
        'Gas$(). The slowest hop sets how fast it mixes.',
        'An $(item)Ergodic Mixer Core$() visits every state there is: its time average is its average over all of them (Kac: a set of measure '
        'μ is revisited every t/μ on average).'],
        adv='ergodic_theory', sort=23, spot=['markov_chain_refinery', 'ergodic_mixer_core'])
    te('probability', 'r55_measure', 'The Measure Problem', 'boltzmann_brain', [
        'A fluctuation that costs entropy ΔS happens with odds $(thing)e^(−ΔS/k)$(). A lone brain that only thinks it remembers a world is '
        'far cheaper than a whole low-entropy universe - so in an eternal universe, which observers are likely? That is the measure problem.',
        'Boltzmann Brains fluctuate into the Accretion Frontier by chance. The sure way: an Ergodic Mixer Core and eight Entropy Dice, run for '
        'a long time through the Monte Carlo Engine.'],
        adv='measure_problem', sort=24, spot=['boltzmann_brain', 'ergodic_mixer_core'])
