"""
Round 38 (1.2, Tier II): the instruments of the new sciences, and radiation that follows the physics.

* EARTH:        Seismology (the Seismograph).
* NUCLEAR:      Radiochemistry (the Decay Chamber, radon), Decay Chains (radiogenic lead, Lead Shielding).
* MAGNETISM:    the Magnetizer, Neodymium and Samarium-Cobalt magnets (Ferromagnetism), the Induction Generator
                (Electromagnetic Induction).
* PROBABILITY:  the Galton Board (Combinatorics), Distributions, Bayesian Inference (the Bayesian Analyzer).
* RELATIVITY:   the Equivalence Principle, Gravitational Time Dilation (the Atomic Clock).

Radiation as data (see RadiationData.java): data maps for what items and blocks give off and what blocks stop.
"""
import json
import os

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


# =================================================================================== CONTENT
ITEMS = [
    _I('seismic_recorder_drum', 'Seismic Recorder Drum', 'spindle', '#d8d0b8', '#402820',
       'A paper drum turned by clockwork under a hanging pen. The ground shakes, the pen does not, and the difference is written down.'),
    _I('radiogenic_lead', 'Radiogenic Lead', 'chunk', '#505660', '#9098a8',
       'Lead that was once uranium, radium and radon: the stable end of a decay chain, made one nucleus at a time.'),
    _I('lead_shielding_plate', 'Lead Shielding Plate', 'plate', '#4a5060', '#a0a8b8',
       'Dense, soft and cheap. Each centimetre of lead halves a gamma ray\'s chance of getting through.'),
    _I('monazite_sand', 'Monazite Sand', 'dots', '#b08850', '#f0d090',
       'Heavy golden grains sorted out of beach sand: a phosphate of the rare earths, with a little thorium that keeps it faintly radioactive.'),
    _I('rare_earth_oxide', 'Rare Earth Oxide', 'dots', '#d0c8d8', '#a080c0',
       'Neodymium, samarium and their cousins, so alike chemically that it took a century and a spectroscope to tell them apart.'),
    _I('neodymium_blank', 'Neodymium Alloy Blank', 'ingot', '#808890', '#c0c8d0',
       'Nd₂Fe₁₄B, cast and pressed, not yet magnetised. Its crystals all point the same way, waiting for a field strong enough to set them.'),
    _I('samarium_cobalt_blank', 'Samarium-Cobalt Blank', 'ingot', '#706060', '#c0a0a0',
       'Sm₂(Co,Fe,Cu)₁₇: harder to magnetise than neodymium, and it keeps its magnetism far hotter.'),
    _I('neodymium_magnet', 'Neodymium Magnet', 'plate', '#a0a8b0', '#ff4040',
       'The strongest permanent magnet known: a thumb-sized piece can hold a kilogram of steel. Loses its strength above about 80 °C.'),
    _I('samarium_cobalt_magnet', 'Samarium-Cobalt Magnet', 'plate', '#806868', '#ff8040',
       'A little weaker than neodymium, but it keeps its field up to 300 °C - the magnet for hot machines.'),
    _I('proof_mass', 'Proof Mass', 'orb', '#e0c060', '#fff0c0',
       'A polished cube of gold and platinum that touches nothing. Whatever path it follows is the path of free fall itself.'),
    _I('time_dilated_sample', 'Time-Dilated Sample', 'crystal', '#806040', '#ffd080',
       'A crystal grown deep in a gravity well. By its own clock it took longer to form than the outside world saw.'),
    _I('normal_distribution_crystal', 'Normal Distribution Crystal', 'crystal', '#c0d8ff', '#ffffff',
       'Faceted where the bell curve says it should be: most of it near the middle, less and less towards the edges, none past the tails.'),
    _I('prior_tablet', 'Prior Tablet', 'plate', '#605040', '#d0c0a0',
       'What you believe before you look, written down so that the evidence can change it.'),
    _I('posterior_tablet', 'Posterior Tablet', 'plate', '#304060', '#80c0ff',
       'Belief, updated by evidence. Carry one while solving a research derivation and it pays for a hint instead of Insight.'),
]
BLOCKS = [
    dict(id='lead_shielding_block', name='Lead Shielding Block', art='metalblock', c1='#3a404c', c2='#8890a0', kind='simple', hardness=5.0,
         tool='stone'),
]
FLUIDS = [
    dict(id='radon', name='Radon-222', c1='#103020', c2='#60ff90', gas=True, light=6, temp=300, density=-50, viscosity=100, art='darkgas'),
]

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'seismograph': ('Seismograph', 'A heavy pendulum that the ground moves under. It times two kinds of wave from below - fast pressure waves and slow '
                                   'shear waves - and the gap between them says how deep their source lies.'),
    'decay_chamber': ('Decay Chamber', 'A sealed, shielded vessel where short-lived isotopes live out their half-lives. Radon in its tanks decays on '
                                       'a real clock - a day in a minute - into lead.'),
    'magnetizer': ('Magnetizer', 'A coil around an iron core that pulses a field through a magnet blank. Only a field stronger than the blank\'s '
                                 'coercivity sets it.'),
    'induction_generator': ('Induction Generator', 'A coil spun inside a permanent magnet\'s field by a small turbine. Stronger magnets give more '
                                                   'power; flowing water beside it spins it faster.'),
    'galton_board': ('Galton Board', 'Twelve rows of pegs. Every piece bounces left or right at each, and where it lands sorts it: most near the '
                                     'middle, a few far out in the tails.'),
    'bayesian_analyzer': ('Bayesian Analyzer', 'Weighs a prior against a piece of evidence and writes down the updated belief.'),
    'atomic_clock': ('Atomic Clock', 'Counts the oscillations of a caesium atom. The deeper it stands in a gravity well, the slower it ticks.'),
}
MACHINE_CAT = {'seismograph': 'geography', 'decay_chamber': 'nuclear', 'magnetizer': 'electromagnetism', 'induction_generator': 'electromagnetism',
               'galton_board': 'probability', 'bayesian_analyzer': 'probability', 'atomic_clock': 'astrophysics'}
MACHINE_RESEARCH = {'seismograph': 'seismology', 'decay_chamber': 'radiochemistry', 'magnetizer': 'ferromagnetism',
                    'induction_generator': 'electromagnetic_induction', 'galton_board': 'combinatorics', 'bayesian_analyzer': 'bayesian_inference',
                    'atomic_clock': 'gravitational_time_dilation'}
TIER = {k: 1 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'seismograph': (['ISI', 'TCT', 'OOO'], {'I': 'minecraft:iron_ingot', 'S': 'sediment_core', 'T': 'theodolite_lens', 'C': _S, 'O': 'minecraft:obsidian'}),
    'decay_chamber': (['IGI', 'TCT', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'T': 'geiger_tube', 'C': _S}),
    'magnetizer': (['WFW', 'ESE', 'WRW'], {'W': 'minecraft:copper_ingot', 'F': 'ferrite_magnet', 'E': 'electrical_steel_lamination', 'S': _S,
                                           'R': 'minecraft:redstone'}),
    'induction_generator': (['WEW', 'FSF', 'WEW'], {'W': 'minecraft:copper_ingot', 'E': 'electrical_steel_lamination', 'F': 'ferrite_magnet', 'S': _S}),
    'galton_board': (['GGG', 'NSN', 'PDP'], {'G': 'minecraft:glass', 'N': 'minecraft:iron_nugget', 'S': _S, 'P': '#minecraft:planks', 'D': 'dice_set'}),
    'bayesian_analyzer': (['RQR', 'CSC', 'RBR'], {'R': 'minecraft:redstone', 'Q': 'random_number_chip', 'C': 'minecraft:comparator', 'S': _S,
                                                  'B': 'minecraft:book'}),
    'atomic_clock': (['GCG', 'QSQ', 'GRG'], {'G': 'minecraft:gold_ingot', 'C': 'minecraft:clock', 'Q': 'cherenkov_quartz', 'S': _S,
                                             'R': 'random_number_chip'}),
}
EXTRA = {
    'seismograph': 'Makes $(item)Seismic Recorder Drums$(). It must stand on a full solid block, or it hears only a fifth as fast. Each finished run '
                   'reports, to everyone within 16 blocks, how deep the nearest ore or cave beneath it lies.',
    'decay_chamber': 'Draws $(item)Radon-222$() from $(item)Radium Salt$() (which is never used up). Radon in its tanks decays with its real 3.8-day '
                     'half-life, sped up 1440 times, and every litre that decays leaves one $(item)Radiogenic Lead$(). It gives off gamma rays while '
                     'radioactive things are inside - shield it.',
    'magnetizer': 'Its coil gives 0.6 T; each iron block or magnetic block touching it adds 0.35 T. A Ferrite Magnet needs 0.35 T, a '
                  '$(item)Neodymium Magnet$() 1.2 T (two touching blocks), a $(item)Samarium-Cobalt Magnet$() 2.0 T (four).',
    'induction_generator': 'Burns coal, charcoal or blaze rods to spin its coil, with a magnet in its second slot (never used up). The stronger '
                           'the magnet the more power - it rises with the square of the field. Flowing water beside it adds half again per side.',
    'galton_board': 'Each piece falls through 12 rows of pegs and lands in one of three grades: common (61%), fine (35%) or exceptional (4%) - the '
                    'binomial distribution, exactly.',
    'bayesian_analyzer': 'Combines a $(item)Prior Tablet$() with evidence - a Spectral Plate, a Seismic Recorder Drum, a Survey Map or a Normal '
                         'Distribution Crystal - into $(item)Posterior Tablets$().',
    'atomic_clock': 'Grows $(item)Time-Dilated Samples$(). It runs at √(1 − 2Φ/c²) of its full speed: fastest at the top of the world, about '
                    'three quarters as fast at its bottom.',
}

# =================================================================================== RESEARCH
RESEARCH = [
    ('seismology', 'Seismology',
     'Earthquakes send two kinds of wave through rock: fast pressure waves and slower shear waves. Time the gap between them and you know how far '
     'away the source is; time it at three stations and you know where. Unlocks the Seismograph.',
     'seismograph', 4, 15, 30, ['geodesy', 'geomorphology'], [('sediment_core', 4), ('theodolite_lens', 1)]),
    ('radiochemistry', 'Radiochemistry',
     'Chemistry done by following radioactivity: a few atoms of a new element are found because they glow. Radium gives off a gas, radon, that '
     'decays in days. Unlocks the Decay Chamber.',
     'decay_chamber', 4, 12, 35, ['radioactivity', 'electrochemistry'], [('radium_salt', 2), ('geiger_tube', 1)]),
    ('decay_chains', 'Decay Chains',
     'Uranium becomes thorium, then radium, then radon, then polonium and at last lead - fourteen steps, each with its own half-life. Wait long '
     'enough and every step runs as fast as the slowest feeds it. Unlocks Radiogenic Lead and Lead Shielding.',
     'radiogenic_lead', 5, 13, 40, ['radiochemistry'], [('radium_salt', 4)]),
    ('distributions', 'Distributions',
     'Add up enough small independent chances and their total always spreads into the same bell-shaped curve, whatever the chances were. '
     'Unlocks the Normal Distribution Crystal.',
     'normal_distribution_crystal', 4, 9, 30, ['combinatorics', 'calculus'], [('dice_set', 2)]),
    ('bayesian_inference', 'Bayesian Inference',
     'Belief is a probability, and evidence should change it by a fixed rule: how likely the evidence was if you are right, against how likely it '
     'was anyway. Unlocks the Bayesian Analyzer and the Prior and Posterior Tablets.',
     'bayesian_analyzer', 5, 10, 40, ['distributions'], [('random_number_chip', 2)]),
    ('equivalence_principle', 'The Equivalence Principle',
     'Drop a hammer and a feather on the Moon and they land together. Gravity pulls everything with the same acceleration, so in free fall '
     'gravity vanishes - Einstein\'s happiest thought. Unlocks the Proof Mass.',
     'proof_mass', 5, 6, 40, ['special_relativity'], [('minecraft:gold_ingot', 4)]),
    ('gravitational_time_dilation', 'Gravitational Time Dilation',
     'A clock lower in a gravity well ticks slower than one above it. Satellite navigation has to correct for it every day, or it would drift '
     'kilometres. Unlocks the Atomic Clock.',
     'atomic_clock', 6, 7, 50, ['equivalence_principle'], [('proof_mass', 1), ('random_number_chip', 1)]),
]

# =================================================================================== PUZZLES (tools/puzzles2/group_r38.json)
PUZZLES = {
    'seismology': dict(
        goal='An earthquake sends compressional waves through rock at speed v _ p and slower shear waves at v _ s. A seismometer records the second kind '
             'arriving a time T after the first. Write how far away, L, the quake happened.',
        name='Hypocentre from two arrivals', lhs='L',
        form='T * v _ p * v _ s / ( v _ p - v _ s )',
        expr='D * P * S / ( P - S )',
        ask='Find L in kilometres for a 2 second gap, with P = 6.0 km/s and S = 3.5 km/s.',
        vars={'L': 'how far away (km)', 'T': 'gap between the arrivals (s)', 'v': 'wave speed', 'p': 'compressional', 's': 'shear',
              'D': 'the gap (s)', 'P': 'compressional speed (km/s)', 'S': 'shear speed (km/s)'},
        given={'D': 2, 'P': 6.0, 'S': 3.5}, unit='km',
        method='The slow wave takes d/v_s, the fast one d/v_p; their difference is Δt. Solve for d.',
        distractors=['t', '+', '2']),
    'radiochemistry': dict(
        goal='Every nucleus in a collection of N has the same small chance of decaying each second, set by the half-life T. Write how many decays happen '
             'per second, A.',
        name='Activity of a radioactive source', lhs='A',
        form='ln ( 2 ) * N / T',
        expr='0.693147 * N / T',
        ask='Find A in becquerels for one gram of radium-226: N = 2.66e21 nuclei and T = 5.05e10 seconds.',
        vars={'A': 'decays per second (Bq)', 'N': 'nuclei present', 'T': 'half-life (s)'},
        given={'N': 2.66e21, 'T': 5.05e10}, unit='Bq',
        method='The decay constant is λ = ln 2 / T, and each second λN of the N nuclei decay.',
        distractors=['exp', 't', 'e']),
    'decay_chains': dict(
        goal='A long-lived parent (half-life T _ 1) feeds a short-lived daughter (half-life T _ 2). After a while the daughter is made as fast as it '
             'decays and its amount stops changing. Write how many daughter nuclei M sit alongside N parent nuclei.',
        name='Secular equilibrium', lhs='M',
        form='N * T _ 2 / T _ 1',
        expr='N * R / T',
        ask='Find M for a speck of radium holding N = 1e12 nuclei (T = 5.05e10 s), with its radon (R = 3.30e5 s).',
        vars={'M': 'daughter nuclei', 'N': 'parent nuclei', 'T': 'half-life (s)', 'R': 'the daughter\'s half-life (s)'},
        given={'N': 1e12, 'T': 5.05e10, 'R': 3.30e5}, unit='nuclei',
        method='When nothing changes, the daughter decays as fast as it forms: λ₂M = λ₁N, and each λ is ln 2 over its half-life.',
        distractors=['ln', '+', 'λ']),
    'distributions': dict(
        goal='Add up many small independent random pushes and the total spreads into a bell-shaped curve with centre μ and width σ. Write the height f of '
             'that curve at the value x.',
        name='Gaussian density', lhs='f',
        form='exp ( - ( x - μ ) ^ 2 / ( 2 * σ ^ 2 ) ) / ( σ * sqrt ( 2 * π ) )',
        expr='exp ( - ( x - μ ) ^ 2 / ( 2 * σ ^ 2 ) ) / ( σ * 2.506628 )',
        ask='Find f at one width from the centre: x = 1, μ = 0, σ = 1.',
        vars={'f': 'height of the curve', 'x': 'the value', 'μ': 'centre', 'σ': 'width', 'π': 'pi'},
        given={'x': 1, 'μ': 0, 'σ': 1}, unit='',
        method='The central limit theorem: sums of many independent pieces tend to this curve; the factor in front makes its total area 1.',
        distractors=['ln', '3', 'n']),
    'bayesian_inference': dict(
        goal='A condition affects a fraction p of people. A check comes back positive with probability L when it is present, and wrongly with '
             'probability F when it is not. Write the chance Q that someone who checks positive really has it.',
        name="Bayes' rule", lhs='Q',
        form='L * p / ( L * p + F * ( 1 - p ) )',
        expr='L * p / ( L * p + F * ( 1 - p ) )',
        ask='Find Q for a condition in 1% of people, a check that finds it 99% of the time and gives false alarms 5% of the time.',
        vars={'Q': 'chance it is really there', 'p': 'how common it is', 'L': 'chance of a positive when present', 'F': 'chance of a false alarm'},
        given={'p': 0.01, 'L': 0.99, 'F': 0.05}, unit='',
        method='Of all the positives, the true ones are L·p and the false ones F·(1 − p). Q is the true share.',
        distractors=['q', '2', 'N']),
    'equivalence_principle': dict(
        goal='A hammer and a feather are dropped together from height h in a vacuum. They land at the same moment, because a body\'s inertia and its '
             'weight are always in proportion. Write how long t they take to reach the ground under acceleration g.',
        name='Universality of free fall', lhs='t',
        form='sqrt ( 2 * h / g )',
        expr='sqrt ( 2 * h / g )',
        ask='Find t in seconds for the drop made on the Moon in 1971: h = 1.6 m, g = 1.62 m/s².',
        vars={'t': 'seconds to land', 'h': 'height (m)', 'g': 'acceleration (m/s²)'},
        given={'h': 1.6, 'g': 1.62}, unit='s',
        method='Starting from rest with constant acceleration, h = ½gt². The mass of the body appears nowhere.',
        distractors=['m', 'v', '4']),
    'gravitational_time_dilation': dict(
        goal='A clock sits a distance r from the centre of a mass M. Compared with a clock far away it ticks slower. Write how many seconds τ it counts '
             'while the faraway clock counts t.',
        name='Gravitational time dilation', lhs='τ',
        form='t * sqrt ( 1 - 2 * G * M / ( r * c ^ 2 ) )',
        expr='t * sqrt ( 1 - 2 * G * M / ( r * c ^ 2 ) )',
        ask='Find τ in seconds for one hour on the surface of a neutron star: M = 2.8e30 kg, r = 12 km (G and c in SI units).',
        vars={'τ': 'seconds on the near clock', 't': 'seconds on the far clock', 'G': 'gravitational constant', 'M': 'mass (kg)', 'r': 'distance (m)',
              'c': 'speed of light (m/s)'},
        given={'t': 3600, 'G': 6.674e-11, 'M': 2.8e30, 'r': 12000, 'c': 2.998e8}, unit='s',
        method='The Schwarzschild metric: for a clock at rest at radius r, dτ² = (1 − 2GM/rc²) dt².',
        distractors=['v', 'ln', 'π']),
}

# =================================================================================== RECIPES
SE, DC, MG, IG, GB, BA, AC, SP = ('seismography', 'radioactive_decay', 'magnetizing', 'induction', 'galton_sorting', 'bayesian_inference',
                                  'clock_comparison', 'spectroscopy')
GALTON = [0.6123, 0.3491, 0.0386]          # 12 rows of pegs: |k − 6| ≤ 1, 2-3, ≥ 4 (2508, 1430 and 158 of 4096)
RECIPES = [
    # (name, type, item inputs, fluid inputs, item outputs, fluid outputs, energy, ticks, research, extra)
    ('seismic_recorder_drum', SE, [('minecraft:paper', 4), ('minecraft:iron_ingot', 1), ('minecraft:black_dye', 1)], [], [('seismic_recorder_drum', 2)], [],
     20000, 300, 'seismology', {}),
    ('geoid_from_seismics', SE, [('survey_map', 2), ('seismic_recorder_drum', 1)], [], [('geoid_model', 1)], [], 30000, 400, 'seismology', {}),
    ('radon_emanation', DC, [], [], [], [('radon', 100)], 20000, 400, 'radiochemistry', {'catalysts': ['radium_salt']}),
    ('lead_from_radon', DC, [], [('radon', 1000)], [('radiogenic_lead', 1)], [], 10000, 600, 'decay_chains', {}),
    ('ferrite_magnetizing', MG, [('magnetite', 1), ('minecraft:clay_ball', 1)], [], [('ferrite_magnet', 2)], [], 20000, 200, 'ferromagnetism', {}),
    ('neodymium_magnetizing', MG, [('neodymium_blank', 1)], [], [('neodymium_magnet', 1)], [], 60000, 200, 'ferromagnetism', {}),
    ('samarium_cobalt_magnetizing', MG, [('samarium_cobalt_blank', 1)], [], [('samarium_cobalt_magnet', 1)], [], 120000, 300, 'ferromagnetism', {}),
    ('galton_iron', GB, [('minecraft:raw_iron', 1)], [], [('minecraft:iron_ingot', 1), ('minecraft:iron_ingot', 2), ('minecraft:iron_ingot', 3)], [],
     8000, 100, 'combinatorics', {'chances': GALTON}),
    ('galton_copper', GB, [('minecraft:raw_copper', 1)], [], [('minecraft:copper_ingot', 1), ('minecraft:copper_ingot', 2), ('minecraft:copper_ingot', 3)],
     [], 8000, 100, 'combinatorics', {'chances': GALTON}),
    ('galton_gold', GB, [('minecraft:raw_gold', 1)], [], [('minecraft:gold_ingot', 1), ('minecraft:gold_ingot', 2), ('minecraft:gold_ingot', 3)], [],
     8000, 100, 'combinatorics', {'chances': GALTON}),
    ('galton_quartz', GB, [('minecraft:quartz', 1)], [], [('minecraft:quartz', 1), ('cherenkov_quartz', 1), ('normal_distribution_crystal', 1)], [],
     12000, 120, 'distributions', {'chances': GALTON}),
    ('posterior_spectral', BA, [('prior_tablet', 1), ('spectral_plate', 1)], [], [('posterior_tablet', 1)], [], 20000, 300, 'bayesian_inference', {}),
    ('posterior_seismic', BA, [('prior_tablet', 1), ('seismic_recorder_drum', 1)], [], [('posterior_tablet', 1)], [], 20000, 300, 'bayesian_inference', {}),
    ('posterior_survey', BA, [('prior_tablet', 1), ('survey_map', 1)], [], [('posterior_tablet', 1)], [], 20000, 300, 'bayesian_inference', {}),
    ('posterior_normal', BA, [('prior_tablet', 1), ('normal_distribution_crystal', 1)], [], [('posterior_tablet', 2)], [], 30000, 300, 'bayesian_inference', {}),
    ('time_dilated_sample', AC, [('proof_mass', 1), ('minecraft:amethyst_shard', 2)], [], [('time_dilated_sample', 1)], [], 30000, 600,
     'gravitational_time_dilation', {}),
    ('monazite_sand', SP, [('minecraft:sand', 8), ('spectral_plate', 1)], [], [('monazite_sand', 2)], [], 30000, 300, 'ferromagnetism', {}),
    ('rare_earth_oxide', SP, [('monazite_sand', 2), ('spectral_plate', 1)], [], [('rare_earth_oxide', 2)], [], 40000, 400, 'ferromagnetism', {}),
]
# the Induction Generator's fuel: (fuel, magnet, FE/t, ticks) - power rises with the square of the magnet's field
GENERATOR = []
for fuel, ticks in (('minecraft:coal', 1600), ('minecraft:charcoal', 1600), ('minecraft:blaze_rod', 2400)):
    for magnet, fe in (('ferrite_magnet', 24), ('samarium_cobalt_magnet', 180), ('neodymium_magnet', 250)):
        GENERATOR.append((fuel.split(':')[1] + '_' + magnet, fuel, magnet, fe, ticks))
CRAFTING = [
    ('lead_shielding_plate', ['LL', 'LL'], {'L': 'radiogenic_lead'}, 'lead_shielding_plate', 2),
    ('lead_shielding_block', ['PPP', 'PPP', 'PPP'], {'P': 'lead_shielding_plate'}, 'lead_shielding_block', 1),
    ('neodymium_blank', ['RIR', 'IRI', 'RIR'], {'R': 'rare_earth_oxide', 'I': 'minecraft:iron_ingot'}, 'neodymium_blank', 2),
    ('samarium_cobalt_blank', ['RCR', 'IRI', 'RCR'], {'R': 'rare_earth_oxide', 'C': 'minecraft:copper_ingot', 'I': 'minecraft:iron_ingot'},
     'samarium_cobalt_blank', 2),
    ('proof_mass', ['GIG', 'IGI', 'GIG'], {'G': 'minecraft:gold_ingot', 'I': 'minecraft:iron_ingot'}, 'proof_mass', 1),
]
SHAPELESS = [
    ('lead_shielding_plate_from_lead', ['#c:ingots/lead', '#c:ingots/lead'], 'lead_shielding_plate', 1),
    ('lead_shielding_plates_from_block', ['lead_shielding_block'], 'lead_shielding_plate', 9),
    ('prior_tablet', ['minecraft:paper', 'minecraft:paper', 'random_number_chip'], 'prior_tablet', 2),
    ('normal_distribution_crystal', ['cherenkov_quartz', 'cherenkov_quartz', 'cherenkov_quartz', 'cherenkov_quartz', 'dice_set'],
     'normal_distribution_crystal', 1),
]

# =================================================================================== RADIATION DATA (data maps)
ITEM_SOURCES = {
    f'{NS}:raw_pitchblende': ('u238_chain', 0.02),
    f'{NS}:radium_salt': ('ra226', 0.25),
    f'{NS}:monazite_sand': ('th232', 0.004),
    f'{NS}:radiogenic_lead': ('pb210', 0.002),
}
BLOCK_SOURCES = {
    f'{NS}:pitchblende_ore': ('u238_chain', 0.004),
    f'{NS}:deepslate_pitchblende_ore': ('u238_chain', 0.004),
}
# μx per block (each block is counted as a 5 cm slab): lead stops ~94% of gamma rays, rock ~43%, water ~30%
SHIELDING = {
    f'{NS}:lead_shielding_block': (2.77, 0.1),
    '#c:storage_blocks/lead': (2.77, 0.1),
    'minecraft:iron_block': (2.2, 0.3),
    'minecraft:gold_block': (3.6, 0.2),
    'minecraft:netherite_block': (3.0, 0.4),
    'minecraft:copper_block': (2.4, 0.3),
    '#minecraft:base_stone_overworld': (0.55, 0.4),
    '#minecraft:base_stone_nether': (0.5, 0.35),
    'minecraft:obsidian': (0.65, 0.45),
    'minecraft:crying_obsidian': (0.65, 0.45),
    '#minecraft:logs': (0.14, 0.6),
    '#minecraft:planks': (0.12, 0.55),
    '#minecraft:wool': (0.03, 0.3),
    '#c:glass_blocks': (0.6, 0.3),
    'minecraft:ice': (0.3, 1.1),
    'minecraft:packed_ice': (0.3, 1.1),
    'minecraft:blue_ice': (0.3, 1.1),
    'minecraft:snow_block': (0.1, 0.4),
    'minecraft:clay': (0.5, 0.8),
}
for col in ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green',
            'red', 'black'):
    SHIELDING[f'minecraft:{col}_concrete'] = (0.6, 0.65)       # concrete: rock plus bound water - good at both

RADIOACTIVE_EXTRA = [f'{NS}:monazite_sand']
FERROMAGNETIC_EXTRA = [f'{NS}:neodymium_magnet', f'{NS}:samarium_cobalt_magnet', f'{NS}:neodymium_blank', f'{NS}:samarium_cobalt_blank', f'{NS}:proof_mass']

LANG = {
    'message.theoreticalsciences.geiger2': 'Geiger: %s counts/s · %s · your dose %s mSv',
    'message.theoreticalsciences.seismo': 'Seismograph: shear waves %s ms after the pressure waves - %s m down: %s',
    'message.theoreticalsciences.seismo_none': 'Seismograph: nothing echoes within 64 m below.',
    'message.theoreticalsciences.seismo_cave': 'a cave',
    'puzzle.theoreticalsciences.posterior_used': 'A Posterior Tablet pays for the hint.',
}


# =================================================================================== WRITERS
def _write_research(g):
    w, D, q = g['w'], g['D'], g['q']
    for rid, t, desc, icon, x, y, ins, req, cost in RESEARCH:
        w(os.path.join(D, 'ts_research', rid + '.json'), {
            'title': t, 'description': desc, 'icon': q(icon), 'x': x, 'y': y, 'insight': ins,
            'requires': [q(r) for r in req], 'cost': [{'item': q(c), 'count': n} for c, n in cost],
            'time': 0 if ins < 70 else int(round(0.6 * ins ** 1.35 / 30.0)) * 30})
        g['LANG'][f'research.{NS}.{rid}'] = t
        g['LANG'][f'research.{NS}.{rid}.desc'] = desc
        w(os.path.join(D, 'advancement/research', rid + '.json'), {
            'criteria': {'unlocked': {'trigger': 'minecraft:impossible'}}, 'requirements': [['unlocked']]})
        r = (rid, t, desc, icon, x, y, ins, list(req), cost)
        g['DEFS'][rid] = r
        if all(e[0] != rid for e in g['RESEARCH']):
            g['RESEARCH'].append(r)


def _write_puzzles():
    p = os.path.join(HERE, 'puzzles2', 'group_r38.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    for name, typ, ins, fins, outs, fouts, fe, t, res, extra in RECIPES:
        g['mr']('r38_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res, **extra)
        p = os.path.join(g['REC'], typ, 'r38_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        g['w'](p, r)
    for name, fuel, magnet, fe, ticks in GENERATOR:
        g['mr']('r38_induction_' + name, IG, [(fuel, 1)], [], [], [], energy=fe, duration=ticks, research='electromagnetic_induction', catalysts=[magnet])
        p = os.path.join(g['REC'], IG, 'r38_induction_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        g['w'](p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r38_' + name, pat, key, res, n)
    for name, items, res, n in SHAPELESS:
        g['shapeless']('r38_' + name, items, res, n)


def _data_maps(g):
    D = g['D']
    g['w'](os.path.join(D, 'data_maps', 'item', 'radiation_source.json'),
           {'values': {k: {'isotope': i, 'strength': s} for k, (i, s) in ITEM_SOURCES.items()}})
    g['w'](os.path.join(D, 'data_maps', 'block', 'radiation_source.json'),
           {'values': {k: {'isotope': i, 'strength': s} for k, (i, s) in BLOCK_SOURCES.items()}})
    g['w'](os.path.join(D, 'data_maps', 'block', 'radiation_shielding.json'),
           {'values': {k: {'gamma': a, 'neutron': b} for k, (a, b) in SHIELDING.items()}})


def _extend_tag(g, kind, name, ids):
    p = os.path.join(g['D'], 'tags', kind, name + '.json')
    t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    for i in ids:
        if i not in t['values']:
            t['values'].append(i)
    g['w'](p, t)


def _tags(g):
    _extend_tag(g, 'item', 'radioactive', RADIOACTIVE_EXTRA)
    _extend_tag(g, 'item', 'ferromagnetic', FERROMAGNETIC_EXTRA)
    _extend_tag(g, 'block', 'magnetic', [])
    g['w'](os.path.join(g['RES'], 'data', 'c', 'tags', 'item', 'dusts', 'lead.json'), {'replace': False, 'values': [f'{NS}:radiogenic_lead']})
    g['w'](os.path.join(g['RES'], 'data', 'c', 'tags', 'item', 'plates', 'lead.json'), {'replace': False, 'values': [f'{NS}:lead_shielding_plate']})
    for kind in ('item', 'block'):
        g['w'](os.path.join(g['RES'], 'data', 'c', 'tags', kind, 'storage_blocks', 'lead.json'),
               {'replace': False, 'values': [f'{NS}:lead_shielding_block']})


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)
    _data_maps(g)


def apply_late(g):
    """After round 37 wrote its tag files."""
    _tags(g)


def finish(g):
    te = g['text_entry']
    te('nuclear', 'r38_radiation', 'Radiation & Shielding', 'lead_shielding_block', [
        'From 1.2, radiation in the mod follows the physics. Every source is an $(thing)isotope$() with its real half-life and its own kind of '
        'radiation, and what reaches you falls with the $(thing)square of the distance$(): a source two blocks away gives a quarter of the dose it gives '
        'at one.',
        '$(thing)Alpha$() particles (uranium, radium, radon) are stopped by a few centimetres of air or by skin: they harm only what you carry. '
        '$(thing)Beta$() electrons are stopped by any solid block. $(thing)Gamma$() rays pass through walls, but each block lets through only '
        'e^(−μx) of them. $(thing)Neutrons$() pass through metal and are slowed by water and anything rich in hydrogen.',
        'For shielding, each block counts as a 5 cm slab. Lead lets through about 6% of gamma rays, iron 11%, concrete 55%, rock 58%, water 70%, '
        'wood 87%. Neutrons are the other way round: water stops 70% of them, lead almost none. Pack makers can set any block\'s values in the data map '
        '$(thing)theoreticalsciences:radiation_shielding$().',
        'Every dimension has a $(thing)background$() rate: about 0.1 µSv/h in the Overworld (the real world\'s average), more in the Nether, more '
        'still in the End with no air above you, and none at all in the Quantum Foam. The $(item)Geiger Counter$() shows the rate where you stand in '
        'µSv/h and its count, which saturates near ten thousand a second as a real tube does.',
        'Your $(thing)dose$() is counted in millisieverts and fades by 1% a second as the body repairs itself. Doses here are scaled up, as everything '
        'in the game\'s time is: sickness begins at 100 mSv and grows worse at 400, 1,000 and 2,500.'],
        adv='radioactivity', sort=3, spot=['lead_shielding_plate', 'lead_shielding_block'])
    te('nuclear', 'r38_decay', 'Radon & Decay Chains', 'decay_chamber', [
        'Radium-226 decays into $(item)Radon-222$(), a gas with a half-life of 3.8 days. The $(item)Decay Chamber$() draws it off a piece of Radium '
        'Salt - which, with a 1,600-year half-life, is never used up.',
        'Inside the chamber radon decays on a real clock, sped up so a day passes in a minute: after 5.5 minutes half of any radon is gone, after 11 '
        'minutes three quarters. Its daughters run down to lead, and each litre that decays leaves one $(item)Radiogenic Lead$().',
        'A long-lived parent and a short-lived daughter settle into $(thing)secular equilibrium$(): the daughter is made as fast as it decays, so '
        'N₂ = N₁·T₂/T₁. A gram of radium holds only 17 micrograms of radon at any moment.',
        'Radon\'s daughters give off gamma rays, so a working chamber is a source. Wall it in: two blocks of lead stop 99.6% of them. Radiogenic Lead '
        'makes $(item)Lead Shielding Plates$() and Lead Shielding Blocks; lead from any other mod works too.'],
        adv='radiochemistry', sort=4, spot=['decay_chamber', 'radiogenic_lead'])
    te('geography', 'r38_seismology', 'Seismology', 'seismograph', [
        'An earthquake sends out two kinds of wave. $(thing)Pressure waves$() push and pull the rock along their path and travel fastest, about 6 km/s '
        'in the crust. $(thing)Shear waves$() shake it sideways and travel at about 3.5 km/s. The farther the source, the larger the gap between them: '
        'd = Δt·v_p·v_s/(v_p − v_s).',
        'The $(item)Seismograph$() must stand on solid ground. Each finished run times the echoes from below and reports the depth of the nearest ore '
        'or cave - within 64 m and three blocks to either side.'],
        adv='seismology', sort=3, spot=['seismograph', 'seismic_recorder_drum'])
    te('electromagnetism', 'r38_magnets', 'Permanent Magnets', 'neodymium_magnet', [
        'A permanent magnet is set by a field stronger than its $(thing)coercivity$(), and keeps what it was given. Ferrite needs about 0.35 T, '
        'neodymium-iron-boron 1.2 T, samarium-cobalt 2 T.',
        'The rare earths come from $(item)Monazite Sand$(), which the Spectrograph picks out of ordinary sand and separates into $(item)Rare Earth '
        'Oxide$(). Monazite carries a little thorium: it is weakly radioactive.',
        'The $(item)Magnetizer$() gives 0.6 T from its coil alone. An iron core multiplies a coil\'s field: each block of iron (or any block tagged '
        '$(thing)magnetic$()) touching the Magnetizer adds 0.35 T.'],
        adv='ferromagnetism', sort=3, spot=['magnetizer', 'neodymium_magnet', 'samarium_cobalt_magnet'])
    te('electromagnetism', 'r38_generator', 'The Induction Generator', 'induction_generator', [
        'Spin a coil in a magnet\'s field and it drives a current: ε = NBAω. With the same coil and load, the power goes as ε², so a magnet three '
        'times stronger gives about ten times the power.',
        'The $(item)Induction Generator$() burns fuel to spin its coil. Put a magnet in its second slot - a Ferrite Magnet gives 24 FE/t, '
        'Samarium-Cobalt 180, Neodymium 250 - and flowing water beside it, like a water wheel, adds half again for every side.'],
        adv='electromagnetic_induction', sort=3, spot=['induction_generator'])
    te('probability', 'r38_galton', 'The Galton Board', 'galton_board', [
        'Drop a ball through rows of pegs. At each it goes left or right with even odds, so after n rows it has gone right k times with probability '
        'C(n,k)/2ⁿ. Pile up enough balls and the binomial heap becomes a bell curve: $(thing)the central limit theorem$().',
        'The $(item)Galton Board$() sorts raw ore into ingots by where each piece lands after 12 rows: within one of the middle, one ingot (61%); '
        'two or three away, two (35%); four or more, three (4%). On average 1.43 ingots a piece.'],
        adv='combinatorics', sort=2, spot=['galton_board'])
    te('probability', 'r38_bayes', 'Belief & Evidence', 'bayesian_analyzer', [
        'A curve made of many small chances is the $(thing)normal distribution$(): f(x) = e^(−(x−μ)²/2σ²)/(σ√2π). 68% of it lies within one width of '
        'its centre, 95% within two.',
        'Bayes\' rule turns a belief into a better one: P(H|E) = P(E|H)·P(H)/P(E). A check that is 99% reliable, for something one person in a '
        'hundred has, is right only one time in six when it says yes - because most yeses are false alarms among the many who do not have it.',
        'The $(item)Bayesian Analyzer$() combines a $(item)Prior Tablet$() with evidence into a $(item)Posterior Tablet$(). Carry Posterior Tablets '
        'while solving research derivations: each one pays for a hint instead of Insight.'],
        adv='bayesian_inference', sort=3, spot=['bayesian_analyzer', 'prior_tablet', 'posterior_tablet'])
    te('astrophysics', 'r38_clocks', 'Clocks in a Gravity Well', 'atomic_clock', [
        'Everything falls the same way: a feather and a hammer land together on the Moon. That is the $(thing)equivalence principle$(), and a '
        '$(item)Proof Mass$() - shielded so that nothing but gravity touches it - follows free fall exactly.',
        'Clocks lower in a gravity well run slower: τ = t·√(1 − 2GM/rc²). On the Earth\'s surface the effect is a few parts in 10¹⁰; on a neutron star '
        'a clock loses twelve minutes an hour.',
        'The $(item)Atomic Clock$() shows it, scaled up so you can see it: it runs at full speed at the top of the world and at about three quarters at '
        'its floor. Carry one in your off hand and your own time stays in step - it clears Time Dilation.'],
        adv='gravitational_time_dilation', sort=3, spot=['atomic_clock', 'proof_mass', 'time_dilated_sample'])
