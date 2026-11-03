"""
Round 48 (1.7.0, "Chance & Order"): Probability, Tier III - where chance becomes statistical mechanics.

* Brownian Microscope (Perrin's experiment): clay particles in water jitter as molecules strike them. Their mean square
  displacement grows as ⟨x²⟩ = 2Dt with D = kT/(6πηr) - so watching them weighs atoms: it gives Avogadro's number. The
  water's viscosity changes steeply with the temperature the coolant holds; the statistical error falls as √(2/n) with the
  number of tracks; seeing them needs the objective's resolution to be well under their wandering. The displacements
  fall on a bell curve: Normal Distribution Crystals.
* Monte Carlo Engine: random-number chips estimate what cannot be computed exactly, with an error σ/√N. Importance
  sampling cuts σ when its proposal fits, and makes it worse when it does not. Every bit erased costs at least kT·ln 2
  (Landauer): the engine's heat falls with its temperature. Its Markov chains turn Priors into Posteriors.
* The Random Walker: a creature that moves by an honest random walk, drifting toward prey. Its touch evens out your luck
  (Variance).
* Maxwell's demon has to forget: a running Maxwell's Demon Sorter leaves those near it with Information Loss.
"""
import json
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C, V as _V

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


def _B(i, n, art, c1, c2, kind='casing', light=2, hardness=5.0, tool='iron', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=light, hardness=hardness, tool=tool)
    d.update(kw)
    return d


# =================================================================================== CONTENT
ITEMS = [
    _I('avogadro_record', 'Avogadro Record', 'codex', '#28241c', '#f0e0b0',
       'A thousand tracks of a clay grain in water, each one a staggering random walk, and from how far they wander: how many molecules make a '
       'mole. Perrin won the Nobel Prize for this.'),
]
BLOCKS = [
    _B('microscope_column', 'Microscope Column', 'beampipe', '#5a5e62', '#c0d8ff', light=2),
    _B('colloid_cell', 'Colloid Cell', 'cryo', '#8a9098', '#d8c8b0', light=2),
    _B('stochastic_core', 'Stochastic Core', 'circuit', '#103020', '#40ff80', light=5),
    _B('entropy_sink', 'Entropy Sink', 'vent', '#6a4028', '#ffa060', light=0),
]
FLUIDS = []

KEY_ADD = {
    'ϑ': ['microscope_column'], 'ϕ': ['colloid_cell'],
    'ϖ': ['stochastic_core'], 'ϱ': ['entropy_sink'], 'ϰ': ['stochastic_core', 'interface_port'],
}

CREATURES = {
    'random_walker': ('Random Walker', [('normal_distribution_crystal', 0, 1, 0.35)]),
}
SPAWN_WEIGHT = (12, 1, 2)            # weight, min, max - at night anywhere in the Overworld


# =================================================================================== SHAPES
def brownian_microscope():
    """An upright microscope on the optical bench: the stage with its colloid cell, the long column above it, and the
    camera at the top recording every track."""
    s = shapes.Shape(7, 7, 5)
    s.box(0, 0, 0, 6, 0, 4, 'Ξ')
    s.box(1, 1, 1, 5, 1, 3, 'Ξ')                                          # the bench top
    s.put(3, 2, 2, 'ϕ')                                                   # the colloid cell on the stage
    s.put(1, 2, 2, 'Ξ'); s.put(5, 2, 2, 'Ξ')                              # stage clips
    for y in range(3, 6):
        s.put(3, y, 2, 'ϑ')                                               # the column and objective
    s.put(3, 6, 2, 'ϑ')
    s.put(2, 6, 2, 'ϑ')                                                   # the camera arm
    s.column(5, 3, 2, 5, 'Ξ')                                             # the focusing pillar
    s.put(4, 5, 3, 'Ξ')
    s.center = (3.0, 2.0, 2.0)
    s.ports('Ξ', 'Π', 4, y=0)
    s.v[(3, 1, 0)] = 'C'
    s.ctrl = (3, 1, 0)
    return s


def monte_carlo_engine():
    """A machine hall: four rows of racks of stochastic cores with an aisle down the middle, and the entropy sinks on the
    roof that carry off the heat of every bit it erases."""
    s = shapes.Shape(9, 5, 9)
    s.box(0, 0, 0, 8, 0, 8, 'ϖ')
    for x in (1, 2, 6, 7):
        for z in range(1, 8):
            for y in (1, 2, 3):
                s.put(x, y, z, 'ϖ')                                       # the racks
    for z in range(1, 8):
        for y in (1, 2, 3):
            s.put(4, y, z, '.')                                           # the aisle
    for x in (1, 2, 6, 7):
        for z in range(1, 8, 2):
            s.put(x, 4, z, 'ϱ')                                           # the radiators on the racks' roofs
    s.center = (4.0, 2.0, 4.0)
    s.ports('ϖ', 'ϰ', 4, y=0)
    s.v[(4, 1, 0)] = 'C'
    s.ctrl = (4, 1, 0)
    return s


shapes.BUILDERS.update({'brownian_microscope': brownian_microscope, 'monte_carlo_engine': monte_carlo_engine})

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'brownian_microscope': ('Brownian Microscope',
                            'A microscope over a drop of water with clay in it. The grains never stop jittering - molecules strike them from every '
                            'side - and how far they wander weighs the molecules.'),
    'monte_carlo_engine': ('Monte Carlo Engine',
                           'Halls of random-number cores that answer by throwing dice: sample enough and the error falls as one over the square '
                           'root of the count.'),
}
MACHINE_CAT = {k: 'probability' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'brownian_microscope': 'brownian_motion', 'monte_carlo_engine': 'monte_carlo_methods'}
TIER = {k: 2 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'brownian_microscope': (['LCL', 'BSB', 'BBB'], {'L': 'fused_silica_lens', 'C': 'microscope_column', 'B': 'optical_bench', 'S': _S}),
    'monte_carlo_engine': (['RRR', 'CSC', 'EEE'], {'R': 'random_number_chip', 'C': 'stochastic_core', 'S': _S, 'E': 'entropy_sink'}),
}
EXTRA = {
    'brownian_microscope': 'Hold the cell\'s temperature with the coolant (water stays liquid between 273 and 370 K; its viscosity falls steeply '
                           'as it warms). Choose how long between frames and how many tracks to follow: the error in Avogadro\'s number falls as '
                           '√(2/n), and the objective\'s resolution must be fine against how far a grain wanders in that time.',
    'monte_carlo_engine': 'Choose how many samples (10³ to 10¹²) and how strongly to importance-sample: the error is σ/√N, and the right proposal '
                          'cuts σ while a wrong one inflates it. Each erased bit gives off kT·ln 2 at least: cool the cores and the heat falls.',
}

# =================================================================================== PHYSICS
_r12.model('brownian_microscope', 'Brownian Microscope',
           'A grain of radius r in water is struck by molecules from every side and wanders: its mean square displacement grows as ⟨x²⟩ = 2Dt, '
           'with D = kT/(6πηr) (Einstein). Measure D and you have Boltzmann\'s constant, and from it Avogadro\'s number. The water\'s viscosity '
           'η falls steeply as it warms. n tracks measure ⟨x²⟩ to √(2/n); each position is found to about a tenth of the objective\'s '
           'resolution, 0.61λ/NA, and that error adds to the wandering.',
           'Convection in the cell: the drift biases the tracks by ±1%·d.',
           [_P('dt', 'Frame interval t', 's', 0.1, 100, 0.1, 10, 'Time between the frames the displacement is measured over.'),
            _P('n', 'Tracks followed', '', 10, 5000, 1, 200, 'How many grains are followed. More tracks, a better average - and a slower count.'),
            _P('NA', 'Numerical aperture', '', 0.1, 1.4, 0.01, 0.5, 'How wide a cone of light the objective takes in. Higher resolves finer.')],
           [_D('eta', 'Water viscosity', 'mPa·s', '0.02414*10^(247.8/max(T - 140, 1))', 'η = A·10^(B/(T − C))', 'How thick the water is at this '
                                                                                                         'temperature.'),
            _D('Dc', 'Diffusion coefficient', 'µm²/s', '1.381e-23*T/(6*3.1416*eta*0.001*r*1e-6)*1e12', 'D = kT/(6πηr)', 'How fast a grain '
                                                                                                                       'spreads out.'),
            _D('msd', 'Mean square displacement', 'µm²', '2*Dc*dt*(1 + 0.02*d)', '⟨x²⟩ = 2Dt', 'How far a grain wanders, squared, between frames.'),
            _D('res', 'Resolution', 'µm', '0.61*0.55/NA', 'δ = 0.61λ/NA', 'The finest detail the objective resolves.'),
            _D('bias', 'Position error', '%', '100*2*(res/10)^2/msd', '2σ²/⟨x²⟩', 'How much the uncertainty in each position inflates the result.'),
            _D('stat', 'Statistical error', '%', '100*sqrt(2/n)', '√(2/n)', 'The scatter of an average over n tracks.'),
            _D('err', 'Error in Avogadro\'s number', '%', 'sqrt(stat^2 + bias^2 + d^2)', 'all added in quadrature', 'How well the count of '
                                                                                                                      'molecules is known.')],
           [_C('frozen', 'stop', 'T - 273', 'The water in the cell has frozen', 'T > 273 K'),
            _C('boiling', 'stop', '370 - T', 'The water in the cell is boiling', 'T < 370 K')],
           rate='clamp(300/(n + 100)*(1 + 2/(dt + 1)), 0.1, 2)', rate_formula='rate = 300/(n + 100) · (1 + 2/(t + 1 s))',
           rate_desc='Fewer tracks and shorter intervals are faster.',
           heat='0.02', idle_heat='0', rvars=[_V('r', 'Grain radius (µm)', 0.5)], kappa=40, heat_capacity=40)

_r12.model('monte_carlo_engine', 'Monte Carlo Engine',
           'Draw N random samples of a quantity of spread σ and their average is off by σ/√N: four times the work halves the error. Importance '
           'sampling draws more where it matters and cuts σ - if its proposal fits the problem; a poor one makes σ worse. Each sample erases '
           'bits, and erasing a bit gives off at least kT·ln 2 (Landauer\'s principle): a cold engine computes for less heat.',
           'Correlations in the random stream: the error grows by up to 10%·d.',
           [_P('lgN', 'Samples (log₁₀ N)', '', 3, 12, 0.01, 6, 'How many samples are drawn: 10 to this power.'),
            _P('s', 'Importance sampling', '', 0, 1, 0.001, 0, 'How strongly the samples are steered. Best near the problem\'s own optimum.')],
           [_D('N', 'Samples', '', '10^lgN', 'N', 'Samples drawn per run.'),
            _D('sv', 'Effective spread', '', 'sg*(1 - 0.9*s)*(1 + 6*s*(s - opt)^2)', 'σ_eff', 'The spread of the steered samples.'),
            _D('err', 'Error', 'ppm', '1e6*sv/sqrt(N)*(1 + 0.1*abs(d))', 'ε = σ/√N', 'How far the answer may be off, in parts per million.'),
            _D('Pl', 'Heat of erasure', 'kW', '2e-12*N*max(T, 1)/300', 'P ∝ N·kT·ln 2', 'The heat of the bits erased, at the cores\' temperature.')],
           [_C('overheat', 'damage', '380 - T', 'The cores are overheating', 'T < 380 K')],
           rate='clamp(2/(1 + N/1e11), 0.1, 2)', rate_formula='rate = 2 / (1 + N/10¹¹)', rate_desc='More samples take longer.',
           heat='Pl', idle_heat='0', rvars=[_V('sg', 'Spread σ of a sample', 1), _V('opt', 'Best importance setting', 0.5)], kappa=40, heat_capacity=60)

_r12.WINDOWS['brownian_microscope'] = {'r48_perrin': ({'r': 0.5}, [('err', 0, 3)])}
_r12.WINDOWS['monte_carlo_engine'] = {
    'r48_central_limit': ({'sg': 1, 'opt': 0.5}, [('err', 0, 100)]),
    'r48_mcmc_posterior': ({'sg': 30, 'opt': 0.62}, [('err', 0, 50)]),
}

# =================================================================================== RESEARCH
RESEARCH = [
    ('brownian_motion', 'Brownian Motion',
     'Pollen grains in water never keep still. In 1905 Einstein showed why - molecules too small to see, striking them at random - and how far they '
     'wander says how big the molecules are. Perrin watched, and atoms were real. Unlocks the Brownian Microscope.',
     'brownian_microscope', 5, 11, 70, ['distributions', 'geometric_optics'], [('normal_distribution_crystal', 2), ('fused_silica_lens', 1)]),
    ('monte_carlo_methods', 'Monte Carlo Methods',
     'When a sum is too hard to do, guess it at random - many times. The scatter of the guesses falls as one over the square root of how many; '
     'steer them well and it falls faster. Unlocks the Monte Carlo Engine.',
     'monte_carlo_engine', 3, 11, 70, ['bayesian_inference', 'combinatorics'], [('random_number_chip', 4), ('posterior_tablet', 2)]),
]

PUZZLES = {
    'brownian_motion': dict(
        goal='A sphere of radius r drifts through a liquid of viscosity η at temperature T. The drag of the liquid and the random kicks of its '
             'molecules are two sides of one thing; write the sphere\'s diffusion coefficient D from the thermal energy scale k _ B T.',
        name='Stokes-Einstein relation', lhs='D',
        form='k _ B * T / ( 6 * π * η * r )',
        expr='k * T / ( 6 * 3.14159 * n * r ) * 1000000000000',
        ask='Find D in µm²/s for a clay grain of r = 5e-7 m in water at T = 293 K (n = 0.001 Pa·s, k = 1.381e-23 J/K).',
        vars={'D': 'diffusion coefficient (µm²/s)', 'k': 'Boltzmann constant', 'B': 'Boltzmann', 'T': 'temperature (K)', 'π': 'pi',
              'η': 'viscosity', 'r': 'radius (m)', 'n': 'viscosity (Pa·s)'},
        given={'k': 1.381e-23, 'T': 293, 'n': 0.001, 'r': 5e-7}, unit='µm²/s',
        method='A force F drags the sphere at F/(6πηr) (Stokes); in equilibrium the drift down a concentration gradient balances the diffusion '
               'up it, so D = kT × that mobility.',
        distractors=['2', 'c', 'ln']),
    'monte_carlo_methods': dict(
        goal='A Markov chain gives N samples of a quantity with spread σ, but each sample remembers the last: they stay correlated for about τ '
             'steps. Averaged, they are worth fewer independent samples. Write how far their average typically is from the truth, ε.',
        name='Standard error of a correlated chain', lhs='ε',
        form='σ * sqrt ( ( 1 + 2 * τ ) / N )',
        expr='s * ( ( 1 + 2 * t ) / N ) ^ 0.5',
        ask='Find ε for a chain of spread s = 2, correlation time t = 4.5 steps, and N = 1000000 samples.',
        vars={'ε': 'error of the average', 'σ': 'spread of one sample', 'τ': 'correlation time (steps)', 'N': 'number of samples',
              's': 'spread of one sample', 't': 'correlation time (steps)'},
        given={'s': 2, 't': 4.5, 'N': 1000000}, unit='',
        method='Independent samples average to σ/√N. Correlated ones are worth only N/(1 + 2τ) independent ones - the effective sample size.',
        distractors=['2', 'π', 'ln']),
}

# =================================================================================== RECIPES
BT, MCE, BA = 'brownian_tracking', 'monte_carlo', 'bayesian_inference'
RECIPES = [
    # (name, type, item inputs, item outputs, FE/t, ticks, research, insight, catalysts)
    ('perrin', BT, [('minecraft:clay_ball', 1), ('photographic_plate', 1)], [('avogadro_record', 1), ('normal_distribution_crystal', 1)], 128, 800,
     'brownian_motion', 5, []),
    # the central limit theorem: sums of uniform random numbers fall on a bell curve
    ('central_limit', MCE, [('minecraft:quartz', 1)], [('normal_distribution_crystal', 2)], 512, 400, 'monte_carlo_methods', 2,
     ['random_number_chip']),
    # Markov chain Monte Carlo: the posterior sampled rather than computed
    ('mcmc_posterior', MCE, [('prior_tablet', 2)], [('posterior_tablet', 4)], 1024, 800, 'monte_carlo_methods', 4, ['random_number_chip']),
]
CRAFTING = [
    ('microscope_column', ['ILI', 'I I', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'fused_silica_lens'}, 'microscope_column', 2),
    ('colloid_cell', ['GCG', 'GWG', 'GCG'], {'G': 'minecraft:glass_pane', 'C': 'minecraft:clay_ball', 'W': 'minecraft:water_bucket'}, 'colloid_cell', 1),
    ('stochastic_core', ['RIR', 'ICI', 'RIR'], {'R': 'random_number_chip', 'I': 'minecraft:iron_ingot', 'C': 'minecraft:comparator'}, 'stochastic_core', 4),
    ('entropy_sink', ['CCC', 'C C', 'CCC'], {'C': 'minecraft:copper_ingot'}, 'entropy_sink', 4),
]
LANG = {}


# =================================================================================== WRITERS
def _write_research(g):
    import round42
    saved = round42.RESEARCH
    round42.RESEARCH = RESEARCH
    try:
        round42._write_research(g)
    finally:
        round42.RESEARCH = saved


def _write_puzzles():
    p = os.path.join(HERE, 'puzzles2', 'group_r48.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    REC, w = g['REC'], g['w']
    for name, typ, ins, outs, fe, t, res, insight, cats in RECIPES:
        g['mr']('r48_' + name, typ, ins, [], outs, [], energy=fe, duration=t, research=res, catalysts=cats)
        p = os.path.join(REC, typ, 'r48_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        if insight: r['insight_per_run'] = insight
        w(p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r48_' + name, pat, key, res, n)


def _spawns(g):
    wt, lo, hi = SPAWN_WEIGHT
    g['w'](os.path.join(g['D'], 'neoforge', 'biome_modifier', 'random_walker.json'), {
        'type': 'neoforge:add_spawns', 'biomes': '#minecraft:is_overworld',
        'spawners': [{'type': f'{NS}:random_walker', 'weight': wt, 'minCount': lo, 'maxCount': hi}]})


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)


def apply_late(g):
    import round42
    round42._extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'),
                        [f'{NS}:{b["id"]}' for b in BLOCKS])
    _spawns(g)


def finish(g):
    te = g['text_entry']
    te('probability', 'r48_brownian', 'Brownian Motion', 'brownian_microscope', [
        'Grains of clay in water never keep still: molecules too small to see strike them from every side, unevenly, and they wander. Einstein '
        'worked out how far: the mean square displacement grows as $(thing)⟨x²⟩ = 2Dt$(), with D = kT/(6πηr).',
        'Measure D and you have Boltzmann\'s constant - and so how many molecules make a mole. The $(item)Brownian Microscope$() follows the '
        'grains in a $(item)Colloid Cell$(). The coolant sets the water\'s temperature, and with it the viscosity: water is three times thinner '
        'near boiling than near freezing. Keep it liquid.',
        'Two errors. Averaging n tracks scatters by √(2/n): follow more grains. Each position is found to about a tenth of the objective\'s '
        'resolution, 0.61λ/NA, and that inflates ⟨x²⟩: open the aperture, or wait longer between frames so they wander further. Get Avogadro\'s '
        'number within 3% for an $(item)Avogadro Record$() - and the displacements, a perfect bell curve, give a Normal Distribution Crystal.'],
        adv='brownian_motion', sort=8, spot=['brownian_microscope', 'colloid_cell', 'avogadro_record'])
    te('probability', 'r48_monte_carlo', 'Monte Carlo', 'monte_carlo_engine', [
        'Some sums are too hard to work out. Throw dice instead: average N random samples and the answer is off by only $(thing)σ/√N$(). To '
        'halve the error, sample four times as much. The $(item)Monte Carlo Engine$() draws up to a trillion samples a run.',
        '$(thing)Importance sampling$() draws more samples where they matter. Set near the problem\'s own optimum it cuts σ several times; set '
        'wrong, it makes σ worse than plain sampling.',
        'Every sample erases bits, and $(thing)Landauer\'s principle$() says erasing a bit gives off at least kT·ln 2: the engine\'s heat is '
        'proportional to its own temperature. Cool the cores and a trillion samples cost far less.',
        'Sums of random numbers fall on a bell curve - the central limit theorem - so the engine grows Normal Distribution Crystals; and its '
        'Markov chains sample a posterior without ever computing it, turning two Prior Tablets into four Posteriors.'],
        adv='monte_carlo_methods', sort=9, spot=['monte_carlo_engine', 'stochastic_core', 'entropy_sink'])
    te('probability', 'r48_walker', 'The Random Walker', 'random_walker_spawn_egg', [
        'At night, in the open, something drifts that has no will of its own: a cloud of particles taking an honest random walk. Near prey its '
        'steps lean toward it - a drift on top of the diffusion, the way a charged grain wanders in a field.',
        'Its touch brings $(thing)Variance$(): luck evens out, and every good effect on you runs out twice as fast. Killed, it may leave a Normal '
        'Distribution Crystal: where it wandered, summed up.',
        'And Maxwell\'s demon must forget what it sorted. Stand by a running $(item)Maxwell\'s Demon Sorter$() and its erasures spill into you: '
        '$(thing)Information Loss$() - the things you hold forget their places.'],
        adv='brownian_motion', sort=10, spot=['random_walker_spawn_egg'])
