"""
1.5.0: Small Wonders. The smaller candidates of the expansion plan, each a concept with a console, a derivation and a
material chain: acoustic resonance and space weather (physics); chaos, knots, cellular automata, computational
complexity and quantum computing (mathematics and computer science); pharmacology and medical imaging (medicine).

Run:  python3 tools/round150.py
"""
import json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG

# ====================================================================== research
research('acoustic_resonance', 'Acoustic Resonance', 16, 24, 160, ['fourier_series', 'ocean_waves'], [('minecraft:amethyst_shard', 8), ('minecraft:iron_ingot', 4)], 200,
         "A bar rings at the frequencies whose half-waves fit its length exactly, f = n·v/2L. Drive it at one of them and the "
         "vibration builds on itself until the bar shakes apart along its weakest planes: in an ore, the grain boundaries "
         "between the metal and the rock. Unlocks the Resonance Chamber: more metal from raw ore, and tuned crystal shards.",
         'A note that shatters stone',
         {'goal': 'A bar of length L in which sound travels at speed v rings in standing waves with nodes at both ends. Write the '
                  'frequency f of its n-th mode.',
          'name': 'Standing waves in a bar', 'lhs': 'f', 'form': 'n * v / ( 2 * L )', 'expr': 'n * v / ( 2 * L )',
          'ask': 'Find f in hertz for mode n = @{n} of a bar L = @{L} m long, v = @{v} m/s.',
          'vars': {'f': 'frequency', 'n': 'mode number', 'v': 'speed of sound in the bar', 'L': 'length'},
          'given': {'n': 1.0, 'v': 6000.0, 'L': 0.05}, 'unit': 'Hz',
          'method': 'A whole number of half-wavelengths must fit: L = n·λ/2, and f = v/λ.',
          'distractors': ['*', '4', 'π'], 'vary': {'n': [1.0, 4.0, 1.0], 'L': [0.02, 0.2, 0.01]}},
         'resonance_chamber', 'fourier_series')
research('space_weather', 'Space Weather', 18, 24, 220, ['magnetohydrodynamics', 'acoustic_resonance'], [('minecraft:copper_ingot', 16), ('minecraft:compass', 1)], 240,
         "The Sun blows a wind of plasma at four hundred kilometres a second, and now and then hurls a cloud of it. The Earth's "
         "field stands it off where the wind's pressure balances the field's, ten Earth radii out; in a storm the boundary is "
         "pushed in, currents surge through the ground and every long conductor, and aurorae light the night. "
         "Geomagnetic storms now come every few days: consoles and multiblocks in the Overworld run at 70% while one lasts. "
         "Unlocks the Heliograph and the Faraday Mesh, which shields a machine from them.",
         'When the Sun reaches the ground',
         {'goal': "The solar wind (density ρ, speed v) presses on the Earth's dipole field, whose pressure falls as B₀²/(2μ₀)·(R/r)⁶. "
                  'Write the stand-off distance r of the magnetopause, in Earth radii R, where the two pressures balance.',
          'name': 'The Chapman–Ferraro stand-off', 'lhs': 'r', 'form': '( B0 ^ 2 / ( 2 * μ0 * ρ * v ^ 2 ) ) ^ ( 1 / 6 )',
          'expr': '( B0 ^ 2 / ( 2 * mu0 * rho * v ^ 2 ) ) ^ ( 1 / 6 )',
          'ask': 'Find r in Earth radii for B₀ = 3.1e-5 T, μ₀ = 1.2566e-6, ρ = @{rho} kg/m³ and v = @{v} m/s.',
          'vars': {'r': 'stand-off distance (Earth radii)', 'B0': 'equatorial surface field', 'μ0': 'permeability of free space',
                   'ρ': 'solar wind density', 'v': 'solar wind speed'},
          'given': {'B0': 0.000031, 'mu0': 0.0000012566, 'rho': 0.00000000000000000001, 'v': 400000.0}, 'unit': 'R',
          'method': 'Pressure balance: ρv² = B²/(2μ₀) with B = B₀(R/r)³; solve for r/R.',
          'distractors': ['3', '*', 'sqrt'], 'vary': {'v': [300000.0, 900000.0, 50000.0]}},
         'heliograph', 'magnetohydrodynamics')
T.RESEARCH['space_weather']['cost'] = [c for c in T.RESEARCH['space_weather']['cost'] if c['count'] > 0]
T.dump(os.path.join(T.D, 'ts_research', 'space_weather.json'), T.RESEARCH['space_weather'])
research('chaos_theory', 'Chaos Theory', 14, 26, 200, ['bifurcations'], [('minecraft:clock', 1), ('minecraft:redstone', 16)], 240,
         "Three simple equations of a convecting fluid (Lorenz) never settle and never repeat: two starts a hair apart "
         "diverge as e^(λt), so the weather cannot be foretold past a week. Yet chaos can be tamed: the strange attractor is "
         "threaded with unstable periodic orbits, and small, well-timed nudges hold the system on any one of them (Ott, "
         "Grebogi and Yorke). Unlocks the Chaos Controller and Strange Attractors.",
         'Order hidden in disorder',
         {'goal': 'Two trajectories start a distance d₀ apart and after a time t are d₁ apart. Write the Lyapunov exponent λ, the '
                  'rate at which they diverge.',
          'name': 'The Lyapunov exponent', 'lhs': 'λ', 'form': 'ln ( d1 / d0 ) / t', 'expr': 'ln ( d1 / d0 ) / t',
          'ask': 'Find λ for d₀ = @{d0}, d₁ = @{d1} after t = @{t} s.',
          'vars': {'λ': 'Lyapunov exponent', 'd0': 'initial separation', 'd1': 'later separation', 't': 'time'},
          'given': {'d0': 0.000001, 'd1': 0.01, 't': 10.0}, 'unit': '1/s',
          'method': 'd₁ = d₀·e^(λt): take the logarithm of both sides.',
          'distractors': ['exp', '*', 'd0'], 'vary': {'t': [5.0, 20.0, 1.0]}},
         'chaos_controller', 'bifurcations')
research('knot_theory', 'Knot Theory', 16, 26, 200, ['geometric_topology', 'chaos_theory'], [('minecraft:string', 32), ('polymer_sheet', 2)], 240,
         "A knot is a closed loop in space that cannot be untangled without cutting. Two drawings show the same knot if "
         "Reidemeister's three moves turn one into the other, and invariants that survive the moves tell knots apart: the "
         "crossing number, the writhe, the Jones polynomial. Chemists tie them into molecules on a metal-ion template, and "
         "knotted chains are far tougher than plain ones. Unlocks the Knot Loom: Molecular Knots and Knotted Polymer.",
         'A loop that cannot be undone',
         {'goal': 'A knot diagram has p positive and m negative crossings. Write its writhe w.',
          'name': 'The writhe', 'lhs': 'w', 'form': 'p - m', 'expr': 'p - m',
          'ask': 'Find w for a diagram with p = @{p} positive and m = @{m} negative crossings.',
          'vars': {'w': 'writhe', 'p': 'positive crossings', 'm': 'negative crossings'},
          'given': {'p': 5.0, 'm': 2.0}, 'unit': '',
          'method': 'Each crossing counts +1 or −1 by the right-hand rule; the writhe is their sum.',
          'distractors': ['+', '*', '2'], 'vary': {'p': [3.0, 9.0, 1.0], 'm': [0.0, 3.0, 1.0]}},
         'knot_loom', 'chaos_theory')
research('cellular_automata', 'Cellular Automata', 18, 26, 260, ['computability', 'turing_patterns', 'chaos_theory'], [('strange_attractor', 2), ('minecraft:redstone', 32)], 300,
         "A grid of cells, each on or off, each following one rule from its neighbours. Most rules die out or freeze or boil "
         "into noise; a few, at the edge between order and chaos (Langton's λ near 0.27), make gliders, guns and machines. "
         "Von Neumann showed such a grid can hold a constructor that builds a copy of anything, itself included. "
         "Unlocks the Automaton Lattice: Von Neumann Constructors, and Quine Replicators far more cheaply.",
         'A machine that builds itself',
         {'goal': "In Conway's Life a cell lives on if it has two or three live neighbours. In a random grid with a fraction ρ of "
                  'cells alive, the chance a cell has exactly k live neighbours of its eight is binomial. Write the chance P it has '
                  'exactly three.',
          'name': 'Neighbours in Life', 'lhs': 'P', 'form': '56 * ρ ^ 3 * ( 1 - ρ ) ^ 5', 'expr': '56 * rho ^ 3 * ( 1 - rho ) ^ 5',
          'ask': 'Find P for a grid with ρ = @{rho} of its cells alive.',
          'vars': {'P': 'chance of exactly three live neighbours', 'ρ': 'fraction alive'},
          'given': {'rho': 0.3}, 'unit': '',
          'method': 'C(8,3) = 56 ways to choose which three of the eight are alive, each with ρ³(1 − ρ)⁵.',
          'distractors': ['8', '+', '3'], 'vary': {'rho': [0.1, 0.5, 0.05]}},
         'automaton_lattice', 'chaos_theory')
research('complexity_theory', 'Complexity Theory', 20, 26, 260, ['computability', 'cryptography'], [('proof_certificate', 1), ('one_time_pad', 1)], 300,
         "Some problems are hard to solve but easy to check: given a certificate, a verifier can confirm a solution in "
         "polynomial time even when finding one may take exponential time (NP). Whether every such problem is also easy to "
         "solve (P = NP) is the great open question. A zero-knowledge proof convinces a verifier, round by round, that a "
         "certificate exists without showing it. Unlocks the Certificate Verifier: Zero-Knowledge Proofs and Grover Oracles.",
         'Hard to find, easy to check',
         {'goal': 'In each round of an interactive proof a cheating prover is caught with probability one half. Write the chance ε '
                  'that a cheat survives k rounds.',
          'name': 'Soundness of an interactive proof', 'lhs': 'ε', 'form': '( 1 / 2 ) ^ k', 'expr': '( 1 / 2 ) ^ k',
          'ask': 'Find ε after k = @{k} rounds.',
          'vars': {'ε': 'soundness error', 'k': 'rounds'},
          'given': {'k': 20.0}, 'unit': '',
          'method': 'The rounds are independent: the cheat must be lucky every time.',
          'distractors': ['*', '2', '-'], 'vary': {'k': [10.0, 40.0, 1.0]}},
         'certificate_verifier', 'cellular_automata')
research('quantum_computing', 'Quantum Computing', 22, 26, 420, ['complexity_theory', 'quantum_entanglement', 'error_correction'],
         [('qubit_die', 4), ('zero_knowledge_proof', 2)], 600,
         "A register of n qubits holds all 2ⁿ of its values at once; a quantum algorithm makes the wrong answers cancel. "
         "Grover's search finds one marked item among N in about (π/4)·√N steps, where a classical search needs N/2; but "
         "every gate must be faultless enough that the whole circuit keeps its coherence, so the qubits are cooled to a "
         "hundredth of a degree. Unlocks the Quantum Computer: each run shortens your current study by fifteen minutes.",
         'All the answers at once',
         {'goal': "Grover's search among N items needs about (π/4)·√N oracle calls. Write the speed-up S over a classical search, "
                  'which needs N/2 on average.',
          'name': "Grover's speed-up", 'lhs': 'S', 'form': '( N / 2 ) / ( π / 4 * sqrt ( N ) )', 'expr': '( N / 2 ) / ( π / 4 * sqrt ( N ) )',
          'ask': 'Find S for N = @{N} items.',
          'vars': {'S': 'speed-up', 'N': 'items searched'},
          'given': {'N': 1000000.0}, 'unit': '×',
          'method': 'Divide the classical expected count by the quantum one; it grows as √N.',
          'distractors': ['N', '*', 'ln'], 'vary': {'N': [10000.0, 100000000.0, 10000.0]}},
         'quantum_computer', 'complexity_theory')
research('pharmacology', 'Pharmacology', 14, 28, 180, ['pharmacokinetics', 'enzyme_kinetics'], [('ion_channel_membrane', 2), ('minecraft:glass_bottle', 4)], 240,
         "A drug's effect rises with its dose along Hill's curve, E = D^n/(EC₅₀^n + D^n): nothing, then a steep rise, then a "
         "plateau. Toxicity follows its own curve at a higher dose; between the two lies the therapeutic window. Dose again "
         "before the last has cleared, and the level climbs out of the window. Unlocks the Dose-Response Assay: the "
         "Vestibular Suppressant and the Anti-Inflammatory.",
         'The dose makes the poison',
         {'goal': "A drug acts on its receptors with a Hill coefficient n and a half-maximal dose EC₅₀. Write the fraction E of the "
                  'full effect at dose D.',
          'name': 'The Hill equation', 'lhs': 'E', 'form': 'D ^ n / ( EC50 ^ n + D ^ n )', 'expr': 'D ^ n / ( EC50 ^ n + D ^ n )',
          'ask': 'Find E for D = @{D} mg, EC₅₀ = @{EC50} mg and n = @{n}.',
          'vars': {'E': 'fractional effect', 'D': 'dose', 'EC50': 'half-maximal dose', 'n': 'Hill coefficient'},
          'given': {'D': 30.0, 'EC50': 10.0, 'n': 2.0}, 'unit': '',
          'method': 'n molecules bind at once: occupancy is D^n over the sum of D^n and the dissociation constant EC₅₀^n.',
          'distractors': ['-', '*', 'exp'], 'vary': {'D': [5.0, 60.0, 5.0]}},
         'dose_response_assay', 'pharmacokinetics')
research('medical_imaging', 'Medical Imaging', 16, 28, 240, ['nmr_spectroscopy', 'pharmacology'], [('fossil_fragment', 2), ('fused_silica_lens', 1)], 300,
         "Shine X-rays through a body from every angle and each beam's loss is the sum of what it passed through (the Radon "
         "transform). Turn the gantry and collect enough of those sums and the inside can be rebuilt slice by slice, by "
         "filtered back-projection: a tomogram. Unlocks the CT Scanner, which scans fossils and amber so the Ancient DNA "
         "Lab knows where to sample.",
         'Seeing inside without cutting',
         {'goal': 'An X-ray beam crosses a slab of thickness x with attenuation coefficient μ. Write the intensity I that comes out, '
                  'if I₀ went in.',
          'name': 'Beer–Lambert attenuation', 'lhs': 'I', 'form': 'I0 * exp ( - μ * x )', 'expr': 'I0 * exp ( - mu * x )',
          'ask': 'Find I for I₀ = @{I0}, μ = @{mu} per cm and x = @{x} cm.',
          'vars': {'I': 'transmitted intensity', 'I0': 'incident intensity', 'μ': 'attenuation coefficient', 'x': 'thickness'},
          'given': {'I0': 1000.0, 'mu': 0.2, 'x': 5.0}, 'unit': '',
          'method': 'Each thin layer dx removes the same fraction μ·dx of what reaches it: dI = −μI·dx.',
          'distractors': ['ln', '/', '+'], 'vary': {'x': [1.0, 20.0, 1.0]}},
         'ct_scanner', 'pharmacology')

# ====================================================================== consoles
RC = model([par('f', 'kHz', 1, 200, 0.5, 20), par('Lc', 'cm', 1, 20, 0.1, 5), par('Am', 'µm', 0.1, 20, 0.1, 2)],
           {'vs': 6.0},
           [der('fn', 'kHz', '50*vs/Lc', 'f₁ = v/2L'),
            der('det', '', 'abs(f - 50*vs/Lc)/(50*vs/Lc)*(1 + 0.02*d)', 'detuning'),
            der('R', '', '1/sqrt(1 + (100*abs(f - 50*vs/Lc)/(50*vs/Lc))^2)', 'response at Q = 50'),
            der('sh', 'µm', 'Am*50/sqrt(1 + (100*abs(f - 50*vs/Lc)/(50*vs/Lc))^2)', 'strain amplitude: drive × Q × response')],
           [con('crack', 'wear', '18 - Am', 'drive under 18 µm, or the transducer cracks'),
            con('pulverise', 'stop', '800 - Am*50/sqrt(1 + (100*abs(f - 50*vs/Lc)/(50*vs/Lc))^2)', 'under 800 µm: beyond, the ore turns to dust')],
           'clamp(f/40, 0.3, 1.5)', 'higher notes shatter faster')
machine('resonance_chamber', 'Resonance Chamber', "A transducer under a sample of raw ore, driven at the note the ore rings at: tuned "
        "exactly, the ore shakes apart along its grain boundaries and gives up more of its metal. Made by crafting.", 'wave', 2, RC,
        {'p.f': 'Drive frequency', 'p.f.desc': 'The note the transducer plays.', 'p.Lc': 'Sample length', 'p.Lc.desc': 'How long the ore sample is cut: it sets the note it rings at.',
         'p.Am': 'Drive amplitude', 'p.Am.desc': 'How hard the transducer drives. Over 18 µm it cracks.',
         'd.fn': 'Resonance', 'd.fn.desc': "The sample's own note.", 'd.det': 'Detuning', 'd.det.desc': 'How far off the drive is.',
         'd.R': 'Response', 'd.R.desc': 'How strongly the sample answers.', 'd.sh': 'Strain', 'd.sh.desc': 'The vibration in the sample.',
         'c.crack': 'The transducer is cracking', 'c.pulverise': 'The ore is turning to dust', 'v.vs': 'Speed of sound',
         'disturb': 'Temperature: the tuning drifts by ±2%·d.', 'rate.desc': 'Higher notes shatter faster.'},
        extreme('resonance_chamber', 1, P_CONSOLE, {'A': 'minecraft:iron_ingot', 'F': 'minecraft:amethyst_shard', 'L': 'minecraft:note_block', 'N': 'minecraft:jukebox'}))

HG = model([par('vw', 'km/s', 250, 1200, 5, 400), par('nw', 'cm⁻³', 1, 60, 0.5, 5), par('Bz', 'nT', -40, 15, 0.5, 0)],
           {},
           [der('Pd', 'nPa', '0.00000167*nw*vw^2*(1 + 0.02*d)', 'dynamic pressure ρv²'),
            der('r0', 'R', '(10.22 + 1.29*tanh(0.184*(Bz + 8.14)))*(0.00000167*nw*vw^2)^(-1/6.6)', 'magnetopause stand-off (Shue)'),
            der('Kp', '', 'clamp(vw*pos(-Bz)/1800, 0, 9)', 'storm index from v·Bs')],
           [con('extreme', 'wear', '60 - 0.00000167*nw*vw^2', 'pressure under 60 nPa (beyond the record storms)')],
           'clamp(vw/600, 0.3, 1.5)', 'faster winds, faster runs')
machine('heliograph', 'Heliograph', "A coronagraph and a magnetometer under one dome: it watches the Sun's wind and sets a storm "
        "against a machine's shielding to prove it. Made by crafting.", 'spectrum', 2, HG,
        {'p.vw': 'Wind speed', 'p.vw.desc': "The solar wind's speed in the test storm.", 'p.nw': 'Wind density', 'p.nw.desc': 'Protons per cubic centimetre.',
         'p.Bz': 'Field southward', 'p.Bz.desc': 'The wind\'s magnetic field: southward (negative) couples into the Earth\'s and drives storms.',
         'd.Pd': 'Pressure', 'd.Pd.desc': 'On the magnetosphere.', 'd.r0': 'Stand-off', 'd.r0.desc': 'How far out the magnetopause stands.',
         'd.Kp': 'Storm index', 'd.Kp.desc': 'Kp, nought to nine.', 'c.extreme': 'Beyond any storm on record',
         'disturb': 'Gusts: pressure varies by ±2%·d.', 'rate.desc': 'Faster winds, faster runs.'},
        extreme('heliograph', 1, P_CONSOLE, {'A': 'minecraft:copper_ingot', 'F': 'minecraft:compass', 'L': 'ferrite_magnet', 'N': 'minecraft:spyglass'}))

CH = model([par('rho', '', 0, 50, 0.1, 10), par('eps', '', 0, 1, 0.005, 0), par('tau', 'ms', 1, 100, 1, 50)],
           {},
           [der('lam', '1/s', '0.3*sqrt(pos(rho - 24.74))*(1 + 0.02*d)', 'Lyapunov exponent: chaos above ρ = 24.74'),
            der('ctrl', '', 'exp(-0.3*sqrt(pos(rho - 24.74))*tau/20)*clamp(eps/(0.03*sqrt(pos(rho - 24.74)) + 0.02), 0, 1)', 'held on the orbit (OGY)'),
            der('tl', 's', '1/(0.3*sqrt(pos(rho - 24.74)) + 0.001)', 'Lyapunov time')],
           [con('kick', 'wear', '0.8 - eps', 'nudges under 0.8, or they knock it off the attractor')],
           'clamp(30/tau, 0.2, 1.5)', 'nudging often takes effort')
machine('chaos_controller', 'Chaos Controller', "A convection cell with a heater, sensors and fast valves: it runs the Lorenz flow "
        "into chaos, then holds it on one orbit of its strange attractor with nudges. Made by crafting.", 'equilibrium', 2, CH,
        {'p.rho': 'Rayleigh number', 'p.rho.desc': 'How hard the cell is heated (ρ in Lorenz\'s equations). Above 24.74 it turns chaotic.',
         'p.eps': 'Nudge size', 'p.eps.desc': 'How big each correction is.', 'p.tau': 'Nudge interval', 'p.tau.desc': 'Time between corrections: faster than the chaos spreads.',
         'd.lam': 'Lyapunov exponent', 'd.lam.desc': 'How fast neighbouring paths part.', 'd.ctrl': 'Control', 'd.ctrl.desc': 'How firmly the orbit is held.',
         'd.tl': 'Lyapunov time', 'd.tl.desc': 'How long a forecast lasts.', 'c.kick': 'The nudges are knocking it off the attractor',
         'disturb': 'Noise: chaos grows by ±2%·d.', 'rate.desc': 'Frequent nudges are slow work.'},
        extreme('chaos_controller', 1, P_CONSOLE, {'A': 'minecraft:copper_ingot', 'F': 'minecraft:clock', 'L': 'minecraft:comparator', 'N': 'minecraft:observer'}))

KL = model([par('c', 'crossings', 3, 9, 1, 3), par('cm', 'mM', 0.1, 10, 0.1, 1), par('Tk', '°C', 0, 100, 1, 25)],
           {'kt': 3},
           [der('match', '', 'exp(-(c - kt)^2*4)', 'the template ties the knot asked for'),
            der('Y', '', 'exp(-(c - kt)^2*4)*cm/(cm + 0.5)*exp(-pos(Tk - 60)/20)*exp(-0.15*(c - 3))*(1 - 0.01*d)', 'yield of the knot'),
            der('w', '', 'c', 'writhe of the standard diagram')],
           [con('cold', 'stop', 'Tk - 10', 'above 10 °C, or the strands do not move'),
            con('tangle', 'wear', '8 - cm', 'under 8 mM: thicker, the strands tangle into oligomers')],
           'clamp((Tk + 20)/60, 0.3, 1.5)', 'warmer ties faster')
machine('knot_loom', 'Knot Loom', "A flask of molecular strands wound round copper-ion templates: each template holds its strands "
        "crossing in the pattern of one knot, until their ends are joined. Made by crafting.", 'chain', 2, KL,
        {'p.c': 'Crossings', 'p.c.desc': 'The knot the templates are set to tie.', 'p.cm': 'Template concentration', 'p.cm.desc': 'Copper ions in the flask.',
         'p.Tk': 'Temperature', 'p.Tk.desc': 'Warmer tie faster; above 60 °C knots slip apart.', 'd.match': 'Match', 'd.match.desc': 'Whether it ties the knot wanted.',
         'd.Y': 'Yield', 'd.Y.desc': 'Strands that close into the knot.', 'd.w': 'Writhe', 'd.w.desc': 'Of the diagram.', 'c.cold': 'Too cold: the strands are frozen',
         'c.tangle': 'The strands are tangling', 'v.kt': 'Knot wanted', 'disturb': 'Impurities: yield varies by ±1%·d.', 'rate.desc': 'Warmer ties faster.'},
        extreme('knot_loom', 1, P_CONSOLE, {'A': 'minecraft:copper_ingot', 'F': 'minecraft:string', 'L': 'polymer_sheet', 'N': 'minecraft:loom'}))

AL = model([par('lam', '', 0, 1, 0.005, 0.5), par('Ls', 'log', 2, 8, 0.1, 3), par('p0', '', 0, 1, 0.01, 0.5)],
           {},
           [der('Cx', '', 'exp(-((lam - 0.27)/0.06)^2)*(1 - 0.01*d)', "complexity: Langton's edge of chaos"),
            der('rep', '', 'exp(-((lam - 0.27)/0.06)^2)*clamp(Ls - 4, 0, 1)*(1 - abs(p0 - 0.3))', 'a constructor fits and copies itself'),
            der('cells', '', '10^Ls', 'cells in the lattice')],
           [con('heat', 'wear', '7.5 - Ls', 'under 10^7.5 cells: more overheats the lattice')],
           'clamp(6/Ls, 0.4, 1.5)', 'bigger lattices are slower')
machine('automaton_lattice', 'Automaton Lattice', "A sheet of a billion programmable cells, each obeying the same rule from its "
        "neighbours: set the rule at the edge of chaos and machines grow in it. Made by crafting.", 'chain', 3, AL,
        {'p.lam': "Langton's λ", 'p.lam.desc': 'The share of neighbourhoods that make a cell live. Near 0.27 is the edge of chaos.',
         'p.Ls': 'Lattice size', 'p.Ls.desc': 'Cells, on a log scale: a constructor needs 10⁵.', 'p.p0': 'Seed density', 'p.p0.desc': 'How full the starting grid is.',
         'd.Cx': 'Complexity', 'd.Cx.desc': 'How rich the patterns are.', 'd.rep': 'Replication', 'd.rep.desc': 'Whether a constructor can copy itself.',
         'd.cells': 'Cells', 'd.cells.desc': 'In the lattice.', 'c.heat': 'The lattice is overheating',
         'disturb': 'Bit flips: complexity varies by ±1%·d.', 'rate.desc': 'Bigger lattices are slower.'},
        extreme('automaton_lattice', 1, P_CONSOLE, {'A': 'minecraft:redstone_block', 'F': 'photonic_processor', 'L': 'minecraft:observer', 'N': 'strange_attractor'}))

CV = model([par('nP', '', 10, 1000, 5, 100), par('kr', 'rounds', 1, 100, 1, 5)],
           {},
           [der('lg', '', 'kr*0.30103*(1 - 0.005*d)', 'soundness: −log₁₀ of the cheat\'s chance 2^(−k)'),
            der('Tv', 's', 'nP^2*kr/1000000', 'verifier time: polynomial, n² a round'),
            der('Tf', 'log s', 'nP/50 - 6', 'finding it instead: exponential')],
           [con('slow', 'wear', '30 - nP^2*kr/1000000', 'verification under 30 s')],
           'clamp(1/(nP^2*kr/1000000 + 0.5), 0.2, 1.5)', 'quicker checks, faster runs')
machine('certificate_verifier', 'Certificate Verifier', "A verifier that challenges a prover over and over: a cheat survives each "
        "round only by luck, so enough rounds make a proof all but certain without the secret ever being shown. Made by crafting.",
        'cipher', 2, CV,
        {'p.nP': 'Problem size', 'p.nP.desc': 'How big the certificate is.', 'p.kr': 'Rounds', 'p.kr.desc': 'Challenges per proof: each halves a cheat\'s chance.',
         'd.lg': 'Soundness', 'd.lg.desc': 'Nines of certainty: 9 is one in a billion.', 'd.Tv': 'Verify time', 'd.Tv.desc': 'Seconds to check.',
         'd.Tf': 'Search time', 'd.Tf.desc': 'Log seconds to find a certificate by search instead.', 'c.slow': 'Verification is taking too long',
         'disturb': 'Noisy channel: soundness varies by ±0.5%·d.', 'rate.desc': 'Quicker checks, faster runs.'},
        extreme('certificate_verifier', 1, P_CONSOLE, {'A': 'minecraft:iron_ingot', 'F': 'proof_certificate', 'L': 'photonic_processor', 'N': 'one_time_pad'}))

DR = model([par('Ld', 'log mg', -1, 3, 0.02, 0), par('nh', '', 0.5, 4, 0.05, 1), par('tau', 'h', 1, 48, 0.5, 24)],
           {'EC': 10, 'TD': 1000, 'th': 8},
           [der('E', '', '(10^Ld)^nh/(EC^nh + (10^Ld)^nh)*(1 - 0.01*d)', 'Hill: effect at the peak'),
            der('Tx', '', '(10^Ld)^nh/(TD^nh + (10^Ld)^nh)', 'toxicity at the peak'),
            der('Et', '', '(10^Ld*exp(-0.693*tau/th))^nh/(EC^nh + (10^Ld*exp(-0.693*tau/th))^nh)', 'effect at the trough, before the next dose')],
           [con('poison', 'wear', '0.3 - (10^Ld)^nh/(TD^nh + (10^Ld)^nh)', 'toxicity under 30%')],
           'clamp(tau/12, 0.2, 1.5)', 'longer intervals, fewer doses to test')
machine('dose_response_assay', 'Dose-Response Assay', "Wells of cultured cells dosed in a ladder of strengths and watched for "
        "effect and harm: it finds the formulation and the dosing interval that keep a medicine in its window. Made by crafting.",
        'oxidation', 2, DR,
        {'p.Ld': 'Dose', 'p.Ld.desc': 'Milligrams, on a log scale: 1 is ten.', 'p.nh': 'Hill coefficient', 'p.nh.desc': 'How steep the formulation\'s curve is.',
         'p.tau': 'Interval', 'p.tau.desc': 'Hours between doses.', 'd.E': 'Effect', 'd.E.desc': 'At the peak.', 'd.Tx': 'Toxicity', 'd.Tx.desc': 'At the peak.',
         'd.Et': 'Trough effect', 'd.Et.desc': 'Just before the next dose.', 'c.poison': 'The dose is toxic', 'v.EC': 'EC₅₀', 'v.TD': 'TD₅₀', 'v.th': 'Half-life',
         'disturb': 'Patient variation: effect varies by ±1%·d.', 'rate.desc': 'Longer intervals, fewer doses to test.'},
        extreme('dose_response_assay', 1, P_CONSOLE, {'A': 'minecraft:glass', 'F': 'ion_channel_membrane', 'L': 'minecraft:brewing_stand', 'N': 'minecraft:cauldron'}))

QC = model([par('nq', 'qubits', 4, 100, 1, 10), par('Tm', 'mK', 5, 100, 1, 50), par('le', 'log', -9, -1, 0.05, -3)],
           {},
           [der('depth', 'gates', '0.785*2^(nq/2)', "Grover depth: (π/4)·√N"),
            der('F', '', 'exp(-0.785*2^(nq/2)*10^le*(1 + Tm/20))*(1 - 0.01*d)', 'circuit fidelity'),
            der('Sx', 'log', 'nq*0.1505', 'speed-up, log₁₀ √N')],
           [con('decohere', 'stop', '80 - Tm', 'under 80 mK, or the qubits decohere'),
            con('fridge', 'wear', 'Tm - 7', 'over 7 mK: colder strains the dilution stages')],
           'clamp(Sx/5, 0.2, 1.5)', 'bigger speed-ups, faster', heat='0.3 + nq/100', idle='0.1')
machine('quantum_computer', 'Quantum Computer', "The controller of a dilution refrigerator: a chandelier of gold-plated stages "
        "hanging in a cryostat, the qubits at its tip a hundredth of a degree above absolute zero. It runs Grover searches through "
        "the space of proofs for whatever its owner is studying. Made by crafting.", 'balance', 3, QC,
        {'p.nq': 'Qubits', 'p.nq.desc': 'How many: the search covers 2ⁿ possibilities.', 'p.Tm': 'Temperature', 'p.Tm.desc': 'Of the mixing chamber, in millikelvin.',
         'p.le': 'Gate error', 'p.le.desc': 'Log of the error per gate: error correction drives it down.', 'd.depth': 'Depth', 'd.depth.desc': 'Gates in the circuit.',
         'd.F': 'Fidelity', 'd.F.desc': 'Chance the whole circuit runs clean.', 'd.Sx': 'Speed-up', 'd.Sx.desc': 'Orders of magnitude over a classical search.',
         'c.decohere': 'The qubits are decohering', 'c.fridge': 'The dilution stages are straining',
         'disturb': 'Cosmic rays: fidelity varies by ±1%·d.', 'rate.desc': 'Bigger speed-ups, faster.'},
        extreme('quantum_computer', 1, P_CTRL, {'F': 'cryostat_shell', 'T': 'qubit_chandelier', 'M': 'dilution_stage', 'B': 'qubit_die'}), multiblock=True)

CT = model([par('Np', 'views', 10, 4000, 10, 200), par('Nd', 'pixels', 64, 2048, 16, 256), par('dose', 'mGy', 1, 100, 1, 10)],
           {},
           [der('K', '', 'clamp(Np/(1.5708*Nd), 0, 1)', 'angular sampling: π/2 views a pixel (Crowther)'),
            der('Qi', '', 'clamp(Np/(1.5708*Nd), 0, 1)*(1 - exp(-dose/10))*(1 - 0.01*d)', 'image quality'),
            der('res', 'mm', '500/Nd', 'resolution over a 50 cm bore')],
           [con('burn', 'wear', '80 - dose', 'under 80 mGy, or the specimen\'s DNA is damaged')],
           'clamp(1600/Np, 0.2, 1.5)', 'fewer views, faster scans', heat='0.3 + dose/200', idle='0.05')
machine('ct_scanner', 'CT Scanner', "The controller of a computed tomograph: a gantry of X-ray tube and detectors turning round a "
        "bore, rebuilding the inside of whatever lies on the couch slice by slice. Made by crafting.", 'equilibrium', 3, CT,
        {'p.Np': 'Views', 'p.Np.desc': 'Projections taken in one turn of the gantry.', 'p.Nd': 'Detector pixels', 'p.Nd.desc': 'Across the detector arc.',
         'p.dose': 'Dose', 'p.dose.desc': 'X-ray dose to the specimen.', 'd.K': 'Sampling', 'd.K.desc': 'Enough views for the pixels?', 'd.Qi': 'Image quality',
         'd.Qi.desc': 'Sharp and quiet.', 'd.res': 'Resolution', 'd.res.desc': 'Smallest detail seen.', 'c.burn': 'The dose is damaging the specimen',
         'disturb': 'Scatter: quality varies by ±1%·d.', 'rate.desc': 'Fewer views, faster scans.'},
        extreme('ct_scanner', 1, P_CTRL, {'F': 'gantry_ring', 'T': 'detector_array', 'M': 'scanner_couch', 'B': 'fused_silica_lens'}), multiblock=True)

# ====================================================================== casings and items
casing('dilution_stage', 'Dilution Stage', "A gold-plated copper plate of a dilution refrigerator, each one colder than the one above, "
       "joined by heat-exchange lines. Helium-3 dissolving into helium-4 below draws the heat out. Made by crafting.")
casing('qubit_chandelier', 'Qubit Chandelier', "The mixing chamber and the qubit chips under it, wired with superconducting lines, "
       "at ten millikelvin. Its readout resonators glow as they ring. Made by crafting.")
casing('cryostat_shell', 'Cryostat Shell', "A vacuum can of polished steel, layered with radiation shields inside, that holds the "
       "cold in. Made by crafting.")
casing('gantry_ring', 'Gantry Ring', "A segment of a CT scanner's rotating ring, slip-ringed so it can turn for ever, carrying the "
       "X-ray tube. Made by crafting.")
casing('detector_array', 'Detector Array', "Scintillator crystals on photodiodes, an arc of them facing the X-ray tube across the "
       "bore. They flash as the beam reaches them. Made by crafting.")
casing('scanner_couch', 'Scanner Couch', "The bed that slides the specimen through the bore, a slice at a time. Made by crafting.")

item('resonant_shard', 'Resonant Shard', "An amethyst shard cut to ring at one exact note, its faces gleaming: the heart of a "
     "transducer, and a clock crystal for anything that must keep time. Made in the Resonance Chamber.")
item('faraday_mesh', 'Faraday Mesh', "A cage of copper mesh and resonant filters round a machine's controls: the currents a "
     "geomagnetic storm induces flow round it. Fitted, the machine runs at full speed through storms. One upgrade per machine. "
     "Made in the Heliograph.")
item('strange_attractor', 'Strange Attractor', "A glass sphere in which a single orbit of the Lorenz attractor is held for ever, "
     "traced in light: chaos, tamed. A source of randomness that can be steered. Made in the Chaos Controller.")
item('molecular_knot', 'Molecular Knot', "A trefoil tied in a single molecule on a copper template, then freed: three crossings, and "
     "no way to undo them without breaking a bond. Made in the Knot Loom.")
item('knotted_polymer', 'Knotted Polymer', "A fibre of chains knotted through each other: pulled, the knots tighten and share the "
     "load, so it is tougher than aramid. Made in the Knot Loom.")
item('von_neumann_constructor', 'Von Neumann Constructor', "A pattern in a cellular lattice that reads a tape and builds whatever "
     "the tape describes, its own tape included: the first machine that can copy itself. Made in the Automaton Lattice.")
item('zero_knowledge_proof', 'Zero-Knowledge Proof', "A transcript of forty rounds of challenge and answer that convinces anyone a "
     "certificate exists, while giving away nothing of it. Made in the Certificate Verifier.")
item('grover_oracle', 'Grover Oracle', "A quantum circuit compiled from a zero-knowledge proof: it marks the right answer among all "
     "the possible ones, so a Grover search can find it. Made in the Certificate Verifier.")
item('vestibular_suppressant', 'Vestibular Suppressant', "A dose that quiets the balance organs: it cures nausea and Seismic "
     "Vertigo and keeps them off for three minutes. A second dose within two minutes is an overdose. Made in the Dose-Response Assay.")
item('anti_inflammatory', 'Anti-Inflammatory', "A dose that calms the body's alarm: it cures Inflammation and weakness and keeps "
     "Inflammation off for three minutes. A second dose within two minutes is an overdose. Made in the Dose-Response Assay.")
item('tomogram', 'Tomogram', "Slices of a fossil or a piece of amber, rebuilt from X-ray views: the Ancient DNA Lab reads them to "
     "drill only where the best-preserved tissue lies. Made in the CT Scanner.")
L['tooltip.%s.upgrade.faraday_mesh' % NS] = 'Upgrade: full speed through geomagnetic storms'
L['message.%s.storm_begins' % NS] = 'A geomagnetic storm has struck: consoles and multiblocks run at 70% while it lasts (unless fitted with a Faraday Mesh). Look for aurorae tonight.'
L['message.%s.storm_ends' % NS] = 'The geomagnetic storm has passed.'
L['message.%s.overdose' % NS] = 'Overdose: the last dose had not cleared.'
L['message.%s.dose_taken' % NS] = 'Dose taken. Wait %s minutes before the next.'

for pid, n, outer, inner in [('dilution_stage', 8, 'minecraft:gold_ingot', 'cryocooler_unit'), ('qubit_chandelier', 4, 'minecraft:gold_ingot', 'qubit_die'),
                             ('cryostat_shell', 16, 'minecraft:iron_ingot', 'passivated_plate'), ('gantry_ring', 8, 'minecraft:iron_ingot', 'rated_i_beam'),
                             ('detector_array', 4, 'minecraft:glass', 'resonant_shard'), ('scanner_couch', 8, 'minecraft:smooth_stone', 'polymer_sheet')]:
    dump(os.path.join(D, 'recipe/crafting', 'r150_%s.json' % pid), extreme(pid, n, P_PART, {'O': outer, 'I': inner}))

# ====================================================================== machine recipes
mrec('acoustic_resonance', 'r150_iron', [('minecraft:raw_iron', 1)], [], [('minecraft:iron_nugget', 14)], [], 64, 120, 'acoustic_resonance',
     {'vs': 6.0}, [('R', 0.9, 1.0), ('sh', 100, 800)], 'resonance_chamber')
mrec('acoustic_resonance', 'r150_gold', [('minecraft:raw_gold', 1)], [], [('minecraft:gold_nugget', 14)], [], 64, 120, 'acoustic_resonance',
     {'vs': 3.2}, [('R', 0.9, 1.0), ('sh', 100, 800)], 'resonance_chamber')
mrec('acoustic_resonance', 'r150_resonant_shard', [('minecraft:amethyst_shard', 2)], [], [('resonant_shard', 1)], [], 64, 200, 'acoustic_resonance',
     {'vs': 5.4}, [('R', 0.98, 1.0), ('sh', 20, 100)], 'resonance_chamber')
mrec('heliography', 'r150_faraday_mesh', [('minecraft:copper_ingot', 8), ('resonant_shard', 2), ('ferrite_magnet', 1)], [], [('faraday_mesh', 1)], [],
     256, 400, 'space_weather', None, [('Kp', 6, 9), ('r0', 3, 7.5)], 'heliograph')
mrec('chaos_control', 'r150_strange_attractor', [('minecraft:glass', 1), ('minecraft:glowstone_dust', 4), ('resonant_shard', 1)], [], [('strange_attractor', 1)], [],
     256, 300, 'chaos_theory', None, [('lam', 0.5, 10), ('ctrl', 0.9, 1)], 'chaos_controller')
mrec('knot_tying', 'r150_molecular_knot', [('polymer_sheet', 1), ('minecraft:copper_ingot', 1)], [('phosphate_buffer', 250)], [('molecular_knot', 2)], [],
     128, 300, 'knot_theory', {'kt': 3}, [('match', 0.95, 1), ('Y', 0.6, 1)], 'knot_loom')
mrec('knot_tying', 'r150_knotted_polymer', [('molecular_knot', 2), ('polymer_sheet', 2)], [('phosphate_buffer', 250)], [('knotted_polymer', 2)], [],
     256, 400, 'knot_theory', {'kt': 5}, [('match', 0.95, 1), ('Y', 0.5, 1)], 'knot_loom')
mrec('automaton_growth', 'r150_von_neumann_constructor', [('strange_attractor', 2), ('undecidable_kernel', 1)], [], [('von_neumann_constructor', 2)], [],
     4096, 600, 'cellular_automata', None, [('rep', 0.85, 1)], 'automaton_lattice')
mrec('automaton_growth', 'r150_quine_replicator', [('von_neumann_constructor', 1), ('nanite_swarm', 3)], [], [('quine_replicator', 4)], [],
     65536, 600, 'cellular_automata', None, [('rep', 0.9, 1)], 'automaton_lattice')
mrec('certificate_check', 'r150_zero_knowledge_proof', [('proof_certificate', 1), ('one_time_pad', 1)], [], [('zero_knowledge_proof', 4)], [],
     256, 300, 'complexity_theory', None, [('lg', 9, 40)], 'certificate_verifier')
mrec('certificate_check', 'r150_grover_oracle', [('zero_knowledge_proof', 2), ('qubit_die', 1), ('strange_attractor', 1)], [], [('grover_oracle', 1)], [],
     1024, 600, 'quantum_computing', None, [('lg', 12, 40)], 'certificate_verifier')
mrec('dose_response', 'r150_vestibular_suppressant', [('ion_channel_membrane', 1), ('minecraft:sugar', 2), ('minecraft:glass_bottle', 4)], [('phosphate_buffer', 250)],
     [('vestibular_suppressant', 4)], [], 128, 300, 'pharmacology', {'EC': 10, 'TD': 1000, 'th': 8}, [('E', 0.9, 1), ('Et', 0.5, 1), ('Tx', 0, 0.05)],
     'dose_response_assay')
mrec('dose_response', 'r150_anti_inflammatory', [('minecraft:birch_log', 1), ('minecraft:sugar', 2), ('minecraft:glass_bottle', 4)], [('phosphate_buffer', 250)],
     [('anti_inflammatory', 4)], [], 128, 300, 'pharmacology', {'EC': 30, 'TD': 2000, 'th': 4}, [('E', 0.9, 1), ('Et', 0.5, 1), ('Tx', 0, 0.05)],
     'dose_response_assay')
mrec('quantum_search', 'r150_grover_search', [('grover_oracle', 1)], [('superfluid_helium', 100)], [], [], 131072, 1200, 'quantum_computing', None,
     [('F', 0.9, 1), ('Sx', 5, 20)], 'quantum_computer')
mrec('tomography', 'r150_tomogram_fossil', [('fossil_fragment', 1)], [], [('tomogram', 1)], [], 2048, 400, 'medical_imaging', None,
     [('Qi', 0.85, 1), ('res', 0, 0.5)], 'ct_scanner')
mrec('tomography', 'r150_tomogram_amber', [('raw_amber', 1)], [], [('tomogram', 1)], [], 2048, 400, 'medical_imaging', None,
     [('Qi', 0.85, 1), ('res', 0, 0.5)], 'ct_scanner')
# newer, cheaper routes elsewhere: genomes guided by a tomogram; a tether of knotted polymer
for g, src, v in (('mammoth_genome', 'dated_bone', {'lam': 0.02, 'Gs': 3300000000.0, 'mol0': 3000000000.0, 'c0': 0.3}, ),
                  ('meganeura_genome', 'dated_amber', {'lam': 0.05, 'Gs': 1000000000.0, 'mol0': 1500000000.0, 'c0': 0.15})):
    win = 10.0 if g == 'mammoth_genome' else 4.0
    dump(os.path.join(D, 'recipe/ancient_dna', 'r150_%s_tomogram.json' % g),
         {'type': ts('ancient_dna'), 'item_inputs': [{'item': ts(src), 'count': 2}, {'item': ts('tomogram'), 'count': 1}],
          'item_outputs': [{'id': ts(g), 'count': 1}], 'energy': 512, 'duration': 600, 'research': ts('medical_imaging'), 'unscaled': True,
          'physics': {'vars': v, 'windows': [{'q': 'Cv', 'min': win, 'max': 1000000000.0}]}})
mrec('composite_layup', 'r150_tether_spool_knotted', [('nanotube_composite', 4), ('knotted_polymer', 1)], [], [('tether_spool', 1)], [],
     2048, 600, 'knot_theory', {'Ef': 1000, 'rf': 1.4, 'Em': 3.5, 'rm': 1.2}, [('S', 380, 2000), ('q', 0.95, 1.0)], 'composite_layup_press')

verify_recipes()

# ====================================================================== multiblocks
# --- Quantum Computer: a cylindrical cryostat, open at the front so the chandelier shows, the chandelier's stages
#     narrowing downwards to the glowing qubits; the qubits turn slowly as they are read
R = 4
W = 2 * R + 3
H = 14
g = grid(W, H, W)
c = W // 2
for y in range(H):
    for z in range(W):
        for x in range(W):
            dx, dz = x - c, z - c
            rr = math.hypot(dx, dz)
            if y == 0 or y == H - 1:
                if rr <= R + 0.5:
                    g[y][z][x] = 'P' if (y == 0 and rr > R - 0.5 and dz < 0 and abs(dx) <= 1) else 'S'
            elif R - 0.5 < rr <= R + 0.5:
                g[y][z][x] = 'G' if (dz < -1 and abs(dx) <= 1 and 2 <= y <= H - 3) else 'S'
for i, (y, r) in enumerate(((11, 3.0), (9, 2.4), (7, 1.8), (5, 1.2))):    # the stages, each smaller and colder
    for z in range(W):
        for x in range(W):
            if math.hypot(x - c, z - c) <= r:
                g[y][z][x] = 'M'
for y in range(5, H - 1):                                               # the central column
    if g[y][c][c] == ' ':
        g[y][c][c] = 'M'
for y in (2, 3, 4):                                                       # the qubit chandelier under the last stage
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if abs(dx) + abs(dz) <= (1 if y == 2 else 2):
                g[y][c + dz][c + dx] = 'Q'
cz = min(z for z in range(W) if g[1][z][c] != ' ')
g[1][cz][c] = 'C'
T.MB_COUNTS['quantum_computer'] = write_mb('quantum_computer', rows(g),
                                           {'S': ['cryostat_shell'], 'P': ['cryostat_shell'] + PORT, 'G': ['containment_field_glass'], 'M': ['dilution_stage'],
                                            'Q': ['qubit_chandelier']},
                                           extra={'effect_center': [c, 3, c], 'effect_scale': 2.0,
                                                  'moving': [{'id': 'qubits', 'chars': 'Q', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 3,
                                                              'motion': [{'type': 'spin', 'axis': 'up', 'deg_per_sec': 12}]}],
                                                  'fx': [{'type': 'glow', 'chars': 'QM', 'color': '#80E0FF', 'hot': '#FFFFFF'},
                                                         {'type': 'inflow', 'radius': 2.5, 'color': '#A0F0FF', 'waves': 2, 'period': 3.0}]})

# --- CT Scanner: a gantry ring standing round a bore, detectors on its far arc, a couch through it on a base; the ring
#     turns about the bore while it scans
Rg = 5
Wc = 2 * Rg + 3
Dp = 15
Hc = 2 * Rg + 3
g = grid(Wc, Hc, Dp)
cx, cy, cz0 = Wc // 2, Rg + 1, 6
for z in range(cz0, cz0 + 3):
    for y in range(Hc):
        for x in range(Wc):
            rr = math.hypot(x - cx, y - cy)
            if Rg - 1.5 < rr <= Rg + 0.5:
                ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
                g[y][z][x] = 'D' if (z == cz0 + 1 and 200 <= ang <= 340) else 'R'
for z in range(Dp):                                                       # the couch through the bore, on a base
    g[cy - 2][z][cx] = 'B'
    g[0][z][cx] = 'F'
    for x in (cx - 1, cx + 1):
        g[0][z][x] = 'F'
for z in (cz0 - 1, cz0 + 3):                                               # the gantry's feet
    for x in range(cx - 3, cx + 4):
        g[0][z][x] = 'P' if (z == cz0 - 1 and abs(x - cx) == 3) else 'F'
    for y in range(1, cy - Rg + 1 + 1):
        g[y][z][cx - 3] = 'F'
        g[y][z][cx + 3] = 'F'
g[1][0][cx] = 'C'
T.MB_COUNTS['ct_scanner'] = write_mb('ct_scanner', rows(g),
                                     {'R': ['gantry_ring'], 'D': ['detector_array'], 'B': ['scanner_couch'], 'F': ['cryostat_shell'],
                                      'P': ['cryostat_shell'] + PORT},
                                     extra={'effect_center': [cx, cy, cz0 + 1], 'effect_scale': 2.5,
                                            'moving': [{'id': 'gantry', 'chars': 'RD', 'split': 'none', 'pivot': [cx + 0.5, cy + 0.5, cz0 + 1.5], 'ramp_seconds': 2,
                                                        'motion': [{'type': 'spin', 'axis': 'back', 'deg_per_sec': 120}]}],
                                            'fx': [{'type': 'glow', 'chars': 'D', 'color': '#C0E0FF', 'hot': '#FFFFFF'},
                                                   {'type': 'beam', 'start_back': cz0 + 1, 'end_back': cz0 + 2, 'up': cy, 'width': 0.15, 'color': '#D0F0FF'}]})
print('multiblock blocks:', {k: T.MB_COUNTS[k] for k in ('quantum_computer', 'ct_scanner')})

# ====================================================================== tags
CAS = ['dilution_stage', 'qubit_chandelier', 'cryostat_shell', 'gantry_ring', 'detector_array', 'scanner_couch']
CON = ['resonance_chamber', 'heliograph', 'chaos_controller', 'knot_loom', 'automaton_lattice', 'certificate_verifier', 'dose_response_assay']
CTL = ['quantum_computer', 'ct_scanner']
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts(x) for x in CAS + CON + CTL])
tag_add(os.path.join(MC, 'tags/block/needs_iron_tool.json'), [ts(x) for x in CAS + CON + CTL])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'casings.json'), [ts(x) for x in CAS])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts(x) for x in CON + CTL])
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts(x) for x in CTL])

# ====================================================================== the Codex
E = {}
RS = {'acoustic_resonance': 'resonance_chamber', 'space_weather': 'heliograph', 'chaos_theory': 'chaos_controller', 'knot_theory': 'knot_loom',
      'cellular_automata': 'automaton_lattice', 'complexity_theory': 'certificate_verifier', 'quantum_computing': 'quantum_computer',
      'pharmacology': 'dose_response_assay', 'medical_imaging': 'ct_scanner'}
E['physics/acoustic_resonance'] = {'title': 'Acoustic Resonance', 'icon': ts('resonant_shard'), 'category': 'physics', 'research': 'acoustic_resonance', 'pages': [
    spot(['resonance_chamber', 'resonant_shard'], "f = n·v/2L. The $(thing)Resonance Chamber$() shakes raw iron and gold apart at their own notes: fourteen nuggets from each raw ore. It also tunes amethyst into $(item)Resonant Shards$().", 'Resonance Chamber')]}
E['physics/space_weather'] = {'title': 'Space Weather', 'icon': ts('faraday_mesh'), 'category': 'physics', 'research': 'space_weather', 'pages': [
    text("Every three to seven days a $(thing)geomagnetic storm$() strikes the Overworld for five minutes. It changes no block and harms no one, but every console and multiblock there runs at $(thing)70%$() while it lasts, and aurorae hang in the night sky."),
    spot(['heliograph', 'faraday_mesh'], "r = (B₀²/2μ₀ρv²)^(1/6). The $(thing)Heliograph$() proves a $(item)Faraday Mesh$() against a design storm; fitted as an upgrade, the machine runs at full speed through storms.", 'Heliograph')]}
E['mathematics/chaos_theory'] = {'title': 'Chaos Theory', 'icon': ts('strange_attractor'), 'category': 'mathematics', 'research': 'chaos_theory', 'pages': [
    spot(['chaos_controller', 'strange_attractor'], "λ = ln(d₁/d₀)/t. The $(thing)Chaos Controller$() drives the Lorenz flow into chaos and holds one orbit with small nudges: a $(item)Strange Attractor$().", 'Chaos Controller')]}
E['mathematics/knot_theory'] = {'title': 'Knot Theory', 'icon': ts('molecular_knot'), 'category': 'mathematics', 'research': 'knot_theory', 'pages': [
    spot(['knot_loom', 'molecular_knot', 'knotted_polymer'], "w = p − m. The $(thing)Knot Loom$() ties $(item)Molecular Knots$() (trefoils) and $(item)Knotted Polymer$() (pentafoils). A Tether Spool of knotted polymer costs less than one of aramid.", 'Knot Loom')]}
E['mathematics/cellular_automata'] = {'title': 'Cellular Automata', 'icon': ts('von_neumann_constructor'), 'category': 'mathematics', 'research': 'cellular_automata', 'pages': [
    spot(['automaton_lattice', 'von_neumann_constructor'], "P = 56ρ³(1 − ρ)⁵. The $(thing)Automaton Lattice$() grows $(item)Von Neumann Constructors$() at the edge of chaos, and with them $(item)Quine Replicators$() far more cheaply than the Nanofabricator.", 'Automaton Lattice')]}
E['mathematics/complexity_theory'] = {'title': 'Complexity Theory', 'icon': ts('zero_knowledge_proof'), 'category': 'mathematics', 'research': 'complexity_theory', 'pages': [
    spot(['certificate_verifier', 'zero_knowledge_proof', 'grover_oracle'], "ε = 2^(−k). The $(thing)Certificate Verifier$() makes $(item)Zero-Knowledge Proofs$(), and compiles them into $(item)Grover Oracles$() for the Quantum Computer.", 'Certificate Verifier')]}
E['mathematics/quantum_computing'] = {'title': 'Quantum Computing', 'icon': ts('qubit_chandelier'), 'category': 'mathematics', 'research': 'quantum_computing', 'pages': [
    spot(['quantum_computer', 'grover_oracle'], "S = (N/2)/((π/4)√N). The $(thing)Quantum Computer$() is %d blocks: a cryostat with a chandelier of dilution stages and its qubits at the tip. Each Grover Oracle it runs shortens its owner's current study by $(thing)fifteen minutes$(). It runs only while you are studying something." % T.MB_COUNTS['quantum_computer'], 'Quantum Computer'),
    {'type': 'multiblock', 'name': 'Quantum Computer', 'text': 'Interface Ports in the front of the floor ring.', 'multiblock': 'quantum_computer'}]}
E['biology/pharmacology'] = {'title': 'Pharmacology', 'icon': ts('vestibular_suppressant'), 'category': 'biology', 'research': 'pharmacology', 'pages': [
    spot(['dose_response_assay', 'vestibular_suppressant', 'anti_inflammatory'], "E = D^n/(EC₅₀^n + D^n). The $(thing)Dose-Response Assay$() finds a dose that works without harm and lasts until the next. Its medicines cure, then guard for three minutes; a second dose within two minutes is an overdose.", 'Dose-Response Assay')]}
E['biology/medical_imaging'] = {'title': 'Medical Imaging', 'icon': ts('tomogram'), 'category': 'biology', 'research': 'medical_imaging', 'pages': [
    spot(['ct_scanner', 'tomogram'], "I = I₀·e^(−μx). The $(thing)CT Scanner$() is %d blocks: a gantry ring that turns round a couch. It scans fossils and amber into $(item)Tomograms$(); with one, the Ancient DNA Lab needs half the dated samples for a genome." % T.MB_COUNTS['ct_scanner'], 'CT Scanner'),
    {'type': 'multiblock', 'name': 'CT Scanner', 'text': 'Interface Ports at the front foot of the gantry.', 'multiblock': 'ct_scanner'}]}
OPS = {
    'resonance_chamber': ("Every bar rings at its own notes. Cut the sample to a length, then play its note exactly: off by more than a hair and it barely answers.",
                          'Drive frequency, Sample length, Drive amplitude', 'temperature: the tuning drifts by ±2%·d.', 'over 18 µm the transducer cracks (wear); strain over 800 µm turns the ore to dust (stops).',
                          'higher notes faster.', ['f₁ = 50·v/L kHz', 'Response = 1/√(1 + (2Q·detuning)²), Q = 50', 'Strain = amplitude × Q × response']),
    'heliograph': ("A Faraday Mesh is proved against a severe storm: a fast wind with its field turned south, pushing the magnetopause in.",
                   'Wind speed, Wind density, Field southward', 'gusts: pressure varies by ±2%·d.', 'over 60 nPa is beyond any storm on record (wear).', 'faster winds faster.',
                   ['P = ρv²', 'r = (10.22 + 1.29·tanh(0.184(B_z + 8.14)))·P^(−1/6.6)', 'Kp ≈ v·B_s/1800']),
    'chaos_controller': ("Heat the cell past ρ = 24.74 for chaos, then nudge it often enough, and hard enough, to hold one orbit.",
                         'Rayleigh number, Nudge size, Nudge interval', 'noise: chaos grows by ±2%·d.', 'nudges over 0.8 knock it off the attractor (wear).', 'frequent nudges are slow.',
                         ['λ = 0.3·√(ρ − 24.74)', 'Control = e^(−λτ/20)·min(1, ε/(0.1λ + 0.02))']),
    'knot_loom': ("Set the templates to the knot wanted, enough of them to catch the strands but not so many they tangle, warm but not hot.",
                  'Crossings, Template concentration, Temperature', 'impurities: yield varies by ±1%·d.', 'under 10 °C nothing moves (stops); over 8 mM tangles (wear).',
                  'warmer faster.', ['Yield = match · c/(c + 0.5) · e^(−(T − 60)⁺/20) · e^(−0.15(n − 3))']),
    'automaton_lattice': ("Set the rule at the edge of chaos, make the lattice big enough to hold a constructor, and seed it near a third full.",
                          "Langton's λ, Lattice size, Seed density", 'bit flips: complexity varies by ±1%·d.', 'over 10^7.5 cells overheats (wear).', 'bigger lattices slower.',
                          ['Complexity = e^(−((λ − 0.27)/0.06)²)', 'Replication = complexity × min(1, log cells − 4) × (1 − |ρ − 0.3|)']),
    'certificate_verifier': ("Each round halves a cheat's chance; nine nines of certainty take thirty rounds. Keep the problem small enough to check quickly.",
                             'Problem size, Rounds', 'noise: soundness varies by ±0.5%·d.', 'checks over 30 s wear the verifier.', 'quicker checks faster.',
                             ['Soundness = k·log₁₀2', 'Verify time = n²k/10⁶ s']),
    'dose_response_assay': ("Find a dose high enough to work, low enough not to harm, and an interval short enough that it still works before the next.",
                            'Dose, Hill coefficient, Interval', 'patient variation: effect varies by ±1%·d.', 'toxicity over 30% wears the assay.', 'longer intervals faster.',
                            ['E = D^n/(EC₅₀^n + D^n)', 'Toxicity = D^n/(TD₅₀^n + D^n)', 'Trough: D·e^(−0.693τ/t½)']),
    'quantum_computer': ("More qubits search more, but the circuit grows as √N and every gate must be faultless enough for it all to stay coherent.",
                         'Qubits, Temperature, Gate error', 'cosmic rays: fidelity varies by ±1%·d.', 'over 80 mK the qubits decohere (stops); under 7 mK strains the fridge (wear).',
                         'bigger speed-ups faster.', ['Depth = (π/4)·2^(n/2)', 'Fidelity = e^(−depth·ε·(1 + T/20))']),
    'ct_scanner': ("Take enough views for the detector's pixels (π/2 per pixel), and enough dose for a quiet image, but not so much it damages the specimen.",
                   'Views, Detector pixels, Dose', 'scatter: quality varies by ±1%·d.', 'over 80 mGy damages the specimen (wear).', 'fewer views faster.',
                   ['Sampling = N_views/(π/2·N_pixels)', 'Quality = sampling × (1 − e^(−dose/10))', 'Resolution = 500/N_pixels mm']),
}
rmap = {v: k for k, v in RS.items()}
for mid, (intro, sets, dist, lim, rate, eqs) in OPS.items():
    E['operation/' + mid] = {'title': T.MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation', 'research': rmap[mid],
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {
    'physics': [{'entry': 'physics/acoustic_resonance', 'x': 3, 'y': 140, 'parents': []},
                {'entry': 'physics/space_weather', 'x': 5, 'y': 140, 'parents': ['physics/acoustic_resonance']}],
    'mathematics': [{'entry': 'mathematics/chaos_theory', 'x': 3, 'y': 83, 'parents': []},
                    {'entry': 'mathematics/knot_theory', 'x': 5, 'y': 83, 'parents': ['mathematics/chaos_theory']},
                    {'entry': 'mathematics/cellular_automata', 'x': 5, 'y': 85, 'parents': ['mathematics/chaos_theory']},
                    {'entry': 'mathematics/complexity_theory', 'x': 7, 'y': 85, 'parents': ['mathematics/cellular_automata']},
                    {'entry': 'mathematics/quantum_computing', 'x': 9, 'y': 85, 'parents': ['mathematics/complexity_theory']}],
    'biology': [{'entry': 'biology/pharmacology', 'x': 3, 'y': 55, 'parents': []},
                {'entry': 'biology/medical_imaging', 'x': 5, 'y': 55, 'parents': ['biology/pharmacology']}],
    'machines': [{'entry': 'operation/' + m, 'x': 28 + 2 * (i % 6), 'y': 12 + 2 * (i // 6), 'parents': []} for i, m in enumerate(OPS)],
}
codex(E, NODES)
mt = json.load(open(os.path.join(A, 'ts_codex/mathematics.json'), encoding='utf-8'))
ys = [n['y'] for n in mt['nodes'] if not n['entry'].startswith('mathematics/chaos') and n['entry'] not in
      ('mathematics/knot_theory', 'mathematics/cellular_automata', 'mathematics/complexity_theory', 'mathematics/quantum_computing')]
print('mathematics tab y range', min(ys), max(ys))
regions('mathematics', 'Patterns & Computation', 81.5, 86.5)
regions('biology', 'Medicine', 53.5, 56.5)
regions('physics', 'Sound & Space Weather', 138.5, 141.5)

finish()
print('research', len(T.RESEARCH), 'machines', len(T.MACHINES), 'recipes', len(T.RECIPES), 'lang', len(L))
