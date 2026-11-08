"""
1.4.0: Mind and Language, and Engineering of the Large (the fifth and sixth branches of the expansion plan).

Writes every data and resource file of the two branches: research and its advancements, the consoles' physics, the
machine recipes, crafting recipes, multiblock layouts, blockstates and models, loot, tags, worldgen, the Codex pages and
the English text. Each console's physics is checked here before anything is written: every recipe's window must be
reachable with every limit kept (a random search over the dials), so no recipe can be impossible.

Run:  python3 tools/round140.py
"""
import json, math, os, random, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src/main/resources')
A = os.path.join(RES, 'assets/theoreticalsciences')
D = os.path.join(RES, 'data/theoreticalsciences')
MC = os.path.join(RES, 'data/minecraft')
NS = 'theoreticalsciences'
LANG = {}


def ts(x):
    return x if ':' in x else NS + ':' + x


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def edit_json(path, fn):
    raw = open(path, encoding='utf-8').read()
    d = json.loads(raw)
    fn(d)
    ind = 1 if raw.startswith('{\n "') or raw.startswith('[\n "') else 2
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=ind, ensure_ascii=('\\u' in raw[:200000]))


def tag_add(path, values):
    if not os.path.exists(path):
        dump(path, {'replace': False, 'values': []})

    def fn(d):
        for v in values:
            if v not in d['values']:
                d['values'].append(v)
    edit_json(path, fn)


# ====================================================================== physics check
FUNCS = {'sqrt': math.sqrt, 'cbrt': lambda x: math.copysign(abs(x) ** (1 / 3), x), 'ln': math.log, 'log10': math.log10, 'exp': math.exp,
         'sin': math.sin, 'cos': math.cos, 'tan': math.tan, 'atan': math.atan, 'tanh': math.tanh, 'abs': abs,
         'pos': lambda x: max(0.0, x), 'step': lambda x: 1.0 if x >= 0 else 0.0, 'min': min, 'max': max,
         'clamp': lambda x, a, b: max(a, min(b, x))}


def pyexpr(e):
    return e.replace('^', '**')


def evaluate(model, params, rvars, d=0.0):
    env = dict(FUNCS)
    env.update({'d': d, 'T': 300.0, 'T_amb': 300.0, 'on': 1.0, 'size': 1.0, 'sky': 1.0, 'night': 0.0, 'sun': 0.5, 'moon': 0.5, 'rain': 0.0,
                'alt': 64.0, 'rfi': 0.0, 'coil': 1.0, 'emitter': 1.0, 'oc': 0.0, 'par': 1.0, 'thr': 1.0, 'T_c': 300.0, 'sc_Tc': 0.0, 'sc_B0': 0.0})
    env.update(rvars)
    env.update(params)
    for c in model.get('consts', []):
        env[c['id']] = eval(pyexpr(c['expr']), {}, env)
    for x in model['derived']:
        env[x['id']] = eval(pyexpr(x['expr']), {}, env)
    ok = all(eval(pyexpr(c['expr']), {}, env) > 0 for c in model['constraints'])
    rate = eval(pyexpr(model['rate']), {}, env)
    return env, ok, rate


def check_reachable(machine, model, recipe):
    rv = dict(model.get('recipe_vars', {}))
    rv.update(recipe.get('physics', {}).get('vars', {}))
    wins = recipe.get('physics', {}).get('windows', [])
    rng = random.Random(1400)
    ps = model['params']
    best = None
    for i in range(60000):
        p = {}
        for q in ps:
            if i == 0:
                p[q['id']] = q['default']
            else:
                v = rng.uniform(q['min'], q['max'])
                p[q['id']] = round(v / q['step']) * q['step']
        try:
            env, ok, rate = evaluate(model, p, rv)
        except (ValueError, ZeroDivisionError, OverflowError):
            continue
        if not ok or rate <= 0:
            continue
        if all(w['min'] <= env[w['q']] <= w['max'] for w in wins):
            return p, {w['q']: env[w['q']] for w in wins}, rate
    raise SystemExit('UNREACHABLE: %s recipe %s windows %s' % (machine, recipe.get('_id'), wins))


# ====================================================================== content: research
RESEARCH = {}


def research(rid, title, x, y, insight, requires, cost, time, desc, short, puzzle, icon, parent):
    RESEARCH[rid] = dict(title=title, x=x, y=y, insight=insight, requires=[ts(r) for r in requires],
                         cost=[{'item': ts(i), 'count': n} for i, n in cost], time=time, description=desc, icon=ts(icon), puzzle=puzzle)
    LANG['research.%s.%s' % (NS, rid)] = title
    LANG['research.%s.%s.desc' % (NS, rid)] = desc
    LANG['advancements.%s.research.%s.short' % (NS, rid)] = short
    adv = {'display': {'icon': {'id': ts(icon)}, 'title': {'translate': 'research.%s.%s' % (NS, rid)},
                       'description': {'translate': 'advancements.%s.research.%s.short' % (NS, rid)}, 'frame': 'task', 'show_toast': True,
                       'announce_to_chat': False, 'hidden': False},
           'parent': ts('research/' + parent), 'criteria': {'unlocked': {'trigger': 'minecraft:impossible'}}, 'requirements': [['unlocked']]}
    dump(os.path.join(D, 'advancement/research', rid + '.json'), adv)
    dump(os.path.join(D, 'ts_research', rid + '.json'), RESEARCH[rid])


# ---------------------------------------------------------------- Mind and Language
research('action_potential', 'The Action Potential', 14, 19, 180, ['membrane_potential', 'neurobiology'], [('neural_organoid', 2), ('ion_channel_membrane', 4)], 240,
         "A neuron fires when sodium channels open faster than potassium ones can close the gap: the voltage runs away upwards, "
         "then recovers. Down a long axon the signal leaks out through the membrane and fades over a length λ = √(r_m/r_i); wrap the "
         "axon in myelin and the leak all but stops, so the spike jumps from gap to gap, a hundred times faster. "
         "Unlocks the Neural Culture Array and Myelinated Axons.",
         'The spike that runs down a nerve',
         {'goal': 'A long axon leaks its signal through its membrane (resistance r_m per length, outward) while the current runs on '
                  'along its core (resistance r_i per length). Write the length λ over which the voltage falls to 1/e.',
          'name': 'The cable equation', 'lhs': 'λ', 'form': 'sqrt ( r_m / r_i )', 'expr': 'sqrt ( rm / ri )',
          'ask': 'Find λ in metres for an axon with r_m = @{rm} Ω·m and r_i = @{ri} Ω/m.',
          'vars': {'λ': 'length constant', 'r_m': 'membrane resistance × length', 'r_i': 'axial resistance per length'},
          'given': {'rm': 4.0, 'ri': 64.0}, 'unit': 'm',
          'method': 'λ²·d²V/dx² = V: the leak through the wall balances the drop along the core where V falls by e each λ.',
          'distractors': ['r_m', '*', 'π'], 'vary': {'rm': [2.0, 9.0, 1.0]}},
         'neural_culture_array', 'neurobiology')
research('synaptic_plasticity', 'Synaptic Plasticity', 16, 19, 220, ['action_potential', 'artificial_intelligence'], [('myelinated_axon', 4), ('photonic_neuron', 2)], 300,
         "Cells that fire together wire together: a synapse grows stronger each time the cell before it and the cell after it are "
         "active at once, Δw = η·x·y. Left alone that runs away; real synapses also scale themselves back (Oja's rule), so a trained "
         "network keeps what it learnt without saturating. Unlocks the Synapse Trainer and the Synaptic Lattice upgrade.",
         'What fires together wires together',
         {'goal': 'A synapse of weight w joins a cell with activity x to a cell with activity y. Hebb: it strengthens in proportion to '
                  'both. Write the change Δw in one step, with learning rate η.',
          'name': "Hebb's rule", 'lhs': 'Δw', 'form': 'η * x * y', 'expr': 'eta * x * y',
          'ask': 'Find Δw for η = @{eta}, x = @{x} and y = @{y}.',
          'vars': {'Δw': 'change in synaptic weight', 'η': 'learning rate', 'x': 'presynaptic activity', 'y': 'postsynaptic activity'},
          'given': {'eta': 0.05, 'x': 0.8, 'y': 0.6}, 'unit': '',
          'method': 'The change needs both cells active: the product of the two activities, scaled by how fast the synapse learns.',
          'distractors': ['+', 'w', '2'], 'vary': {'x': [0.2, 1.0, 0.1], 'y': [0.2, 1.0, 0.1]}},
         'synapse_trainer', 'action_potential')
research('psychophysics', 'Psychophysics', 14, 21, 200, ['action_potential'], [('myelinated_axon', 2), ('eel_photophore', 2)], 240,
         "How bright a light seems grows not with its intensity but with the logarithm of it (Weber and Fechner): S = k·ln(I/I₀). "
         "Whether you notice a faint signal at all depends on how far it stands out of the noise (d′) and on where you set your "
         "threshold: low, and you see things that are not there; high, and you miss things that are. "
         "Unlocks the Psychophysics Booth, the Rhodopsin Retina and the Low-Light Visor.",
         'Seeing in the dark is a choice of threshold',
         {'goal': 'A light of intensity I against a threshold intensity I₀. Fechner: equal ratios of intensity feel like equal steps. '
                  'Write the sensation S, with a constant k.',
          'name': 'The Weber–Fechner law', 'lhs': 'S', 'form': 'k * ln ( I / I0 )', 'expr': 'k * ln ( I / I0 )',
          'ask': 'Find S for k = @{k}, I = @{I} and I₀ = @{I0} (in the same units).',
          'vars': {'S': 'perceived sensation', 'k': 'constant of the sense', 'I': 'stimulus intensity', 'I0': 'threshold intensity'},
          'given': {'k': 2.0, 'I': 80.0, 'I0': 5.0}, 'unit': '',
          'method': "Weber: the just-noticeable step dI is a fixed fraction of I. Add up dS = k·dI/I from I₀ to I.",
          'distractors': ['exp', '-', 'I'], 'vary': {'I': [20.0, 200.0, 10.0]}},
         'psychophysics_booth', 'action_potential')
research('linguistics', 'Linguistics', 16, 21, 220, ['psychophysics', 'information_theory'], [('glyph_tablet', 2), ('one_time_pad', 1)], 300,
         "In any language the commonest word turns up about twice as often as the second, three times as often as the third: "
         "Zipf's law. Count the symbols of an unknown script and their frequencies tell you its alphabet, then its words, then its "
         "grammar; how much each symbol says is its entropy, H = log₂N for N symbols used equally. "
         "Unlocks the Decipherment Desk: the glyph tablets of the Library Ruins can be read.",
         'The commonest word, twice as often as the next',
         {'goal': 'A script of N symbols, each as likely as any other. Write the information H each symbol carries, in bits.',
          'name': "Shannon's entropy of an alphabet", 'lhs': 'H', 'form': 'ln ( N ) / ln ( 2 )', 'expr': 'ln ( N ) / ln ( 2 )',
          'ask': 'Find H in bits for a script of N = @{N} symbols.',
          'vars': {'H': 'information per symbol', 'N': 'number of symbols'},
          'given': {'N': 24.0}, 'unit': 'bits',
          'method': 'H = −Σ p·log₂p with every p = 1/N: N terms of (1/N)·log₂N.',
          'distractors': ['N', '*', 'exp'], 'vary': {'N': [16.0, 64.0, 2.0]}},
         'decipherment_desk', 'psychophysics')
research('integrated_information', 'Integrated Information', 18, 20, 360, ['synaptic_plasticity', 'linguistics', 'superintelligence'],
         [('engram', 0), ('synaptic_lattice', 2), ('universal_grammar', 1)], 600,
         "A mind is more than its parts: a network is conscious, on one theory, to the degree that it is both integrated (every part "
         "affects every other) and differentiated (its parts do different things). Integrated information Φ is low for a crystal, "
         "where everything does the same, and low for a heap of separate parts; it peaks between. "
         "Unlocks the Connectome Array, Engrams, and a cheaper Superintelligence Core.",
         'More than the sum of its parts',
         {'goal': 'A toy network of n units, each joined to the rest with probability p. Its integration grows as 1 − e^(−n·p) and its '
                  'differentiation falls as e^(−p·n/10). Write the toy Φ as their product times n.',
          'name': 'A toy Φ', 'lhs': 'Φ', 'form': 'n * ( 1 - exp ( - n * p ) ) * exp ( - p * n / 10 )',
          'expr': 'n * ( 1 - exp ( - n * p ) ) * exp ( - p * n / 10 )',
          'ask': 'Find Φ for n = @{n} units joined with p = @{p}.',
          'vars': {'Φ': 'integrated information (toy)', 'n': 'units', 'p': 'connection probability'},
          'given': {'n': 40.0, 'p': 0.05}, 'unit': '',
          'method': 'Each factor alone is a monotone curve; their product is a hump: Φ is largest for a sparse but connected net.',
          'distractors': ['+', 'ln', '2'], 'vary': {'p': [0.02, 0.1, 0.01]}},
         'connectome_array', 'synaptic_plasticity')
# the integrated-information study costs no engram (one can only be made after it): drop the zero entry
RESEARCH['integrated_information']['cost'] = [c for c in RESEARCH['integrated_information']['cost'] if c['count'] > 0]
dump(os.path.join(D, 'ts_research', 'integrated_information.json'), RESEARCH['integrated_information'])

# ---------------------------------------------------------------- Engineering of the Large
research('structural_mechanics', 'Structural Mechanics', 19, 10, 160, ['constructions', 'ceramics'], [('minecraft:iron_ingot', 32), ('alumina_ceramic_plate', 2)], 200,
         "Steel stretches in proportion to the stress on it (Hooke, σ = Eε) until it yields; but a slender column fails long before "
         "that, by bowing sideways. Euler found the load at which it buckles: P = π²EI/(KL)². Stiffness against buckling comes from "
         "the shape of the section, its second moment I, which is why beams are I-shaped. "
         "Unlocks the Load Test Rig and Rated I-Beams.",
         'Why beams are I-shaped',
         {'goal': 'A slender column of length L, pinned at both ends, of a material of modulus E and a section of second moment I. '
                  'Write the load P at which it buckles.',
          'name': "Euler's buckling load", 'lhs': 'P', 'form': 'π ^ 2 * E * I / L ^ 2', 'expr': 'π ^ 2 * E * I / L ^ 2',
          'ask': 'Find P in newtons for a steel column: E = @{E} Pa, I = @{I} m⁴, L = @{L} m.',
          'vars': {'P': 'critical load', 'E': "Young's modulus", 'I': 'second moment of area', 'L': 'length'},
          'given': {'E': 200000000000.0, 'I': 0.00002, 'L': 6.0}, 'unit': 'N',
          'method': 'EI·y″ + P·y = 0 with y = 0 at both ends: the first mode, y = sin(πx/L), needs P = π²EI/L².',
          'distractors': ['2', '*', 'σ'], 'vary': {'L': [3.0, 10.0, 1.0]}},
         'load_test_rig', 'constructions')
research('composite_materials', 'Composite Materials', 21, 10, 200, ['structural_mechanics', 'nanotechnology'], [('rated_i_beam', 4), ('carbon_nanotube_bundle', 1)], 240,
         "Stiff fibres in a light matrix: along the fibres, the composite is as stiff as the mixture of the two by volume (the rule "
         "of mixtures, E = V_f·E_f + V_m·E_m), and far lighter than metal. Off the fibre direction it is weak, so plies are laid at "
         "angles; pack the fibres too tight and the matrix cannot wet them, and voids weaken it. "
         "Unlocks the Composite Layup Press: carbon-fibre and nanotube composites.",
         'Stiff fibres, light glue',
         {'goal': 'Fibres of modulus E_f fill a fraction V_f of a part; the rest is matrix of modulus E_m. Along the fibres both stretch '
                  'alike. Write the part\'s modulus E.',
          'name': 'The rule of mixtures', 'lhs': 'E', 'form': 'V_f * E_f + ( 1 - V_f ) * E_m', 'expr': 'Vf * Ef + ( 1 - Vf ) * Em',
          'ask': 'Find E in GPa for V_f = @{Vf}, E_f = @{Ef} GPa and E_m = @{Em} GPa.',
          'vars': {'E': 'composite modulus', 'V_f': 'fibre volume fraction', 'E_f': 'fibre modulus', 'E_m': 'matrix modulus'},
          'given': {'Vf': 0.6, 'Ef': 230.0, 'Em': 3.5}, 'unit': 'GPa',
          'method': 'Equal strain in both: the stresses add, each weighted by the share of the section it carries.',
          'distractors': ['/', 'V_m', '2'], 'vary': {'Vf': [0.3, 0.7, 0.05]}},
         'composite_layup_press', 'structural_mechanics')
research('tribology', 'Tribology', 19, 12, 180, ['structural_mechanics'], [('rated_i_beam', 2), ('graphene_sheet', 1)], 200,
         "Every machine wears where its parts slide. Archard: the volume worn away is V = k·W·L/H, the load times the distance slid "
         "over the hardness, with a wear coefficient k for the pair. A hard, slippery coating (diamond-like carbon) cuts both the "
         "friction and k by orders of magnitude. Unlocks the Tribometer and the Wear Coating upgrade.",
         'Hard and slippery lasts longest',
         {'goal': 'A part slides a distance L under a load W against a surface of hardness H; the pair has wear coefficient k. '
                  'Write the volume V worn away.',
          'name': "Archard's wear law", 'lhs': 'V', 'form': 'k * W * L / H', 'expr': 'k * W * L / H',
          'ask': 'Find V in m³ for k = @{k}, W = @{W} N, L = @{L} m and H = @{H} Pa.',
          'vars': {'V': 'volume worn', 'k': 'wear coefficient', 'W': 'load', 'L': 'distance slid', 'H': 'hardness'},
          'given': {'k': 0.0001, 'W': 100.0, 'L': 1000.0, 'H': 2000000000.0}, 'unit': 'm³',
          'method': 'Asperities touch over an area W/H; a fixed fraction k of the contacts tears off a lump as it slides.',
          'distractors': ['*', 'μ', '2'], 'vary': {'W': [50.0, 500.0, 50.0]}},
         'tribometer', 'structural_mechanics')
research('corrosion_science', 'Corrosion Science', 21, 12, 220, ['tribology', 'electrochemistry'], [('passivated_plate', 0), ('nickel_plated_iron', 4), ('minecraft:iron_ingot', 16)], 240,
         "Rust is a battery shorted out: iron gives up electrons where it dissolves, oxygen takes them where it does not. Whether "
         "iron corrodes, rusts over and stops (passivity) or cannot corrode at all (immunity) depends on its potential and the "
         "pH: Pourbaix's map. Chloride punches through the passive film, so sea water is worst. Hold a structure below its "
         "immunity line with a less noble metal, and that metal dissolves instead: a sacrificial anode. "
         "Unlocks the Corrosion Cell, Sacrificial Anodes and Passivated Plate.",
         'Rust is a battery shorted out',
         {'goal': 'Iron dissolves as Fe → Fe²⁺ + 2e⁻ (standard potential E⁰). Write its equilibrium potential E when the iron ions '
                  'in the water have concentration c (Nernst, at 25 °C, in volts).',
          'name': 'The Nernst equation for iron', 'lhs': 'E', 'form': 'E0 + 0.0296 * ln ( c ) / ln ( 10 )', 'expr': 'E0 + 0.0296 * ln ( c ) / ln ( 10 )',
          'ask': 'Find E in volts for E⁰ = −0.44 V and c = @{c} mol/L.',
          'vars': {'E': 'equilibrium potential', 'E0': 'standard potential', 'c': 'concentration of Fe²⁺'},
          'given': {'E0': -0.44, 'c': 0.000001}, 'unit': 'V',
          'method': 'E = E⁰ + (RT/nF)·ln c; with n = 2 at 25 °C, RT/2F·ln 10 = 0.0296 V per decade.',
          'distractors': ['*', '2', 'exp'], 'vary': {}},
         'corrosion_cell', 'tribology')
RESEARCH['corrosion_science']['cost'] = [c for c in RESEARCH['corrosion_science']['cost'] if c['count'] > 0]
dump(os.path.join(D, 'ts_research', 'corrosion_science.json'), RESEARCH['corrosion_science'])
research('space_elevator', 'The Space Elevator', 23, 9, 320, ['composite_materials', 'orbital_mechanics'], [('nanotube_composite', 8), ('crew_capsule', 1)], 480,
         "A cable from the equator to beyond geostationary orbit, held taut by the Earth's spin: climbers ride it up with no rocket "
         "at all. The cable must hold its own weight, so it tapers, thickest at the top; the taper ratio grows as exp(ρgR²/σr), "
         "which is hopeless for steel and possible only for nanotubes. "
         "Unlocks the Space Elevator: a second route to orbit, needing only power.",
         'A rope to orbit',
         {'goal': 'A tether of density ρ and strength σ hangs from geostationary orbit. Its taper ratio (cross-section at the top over '
                  'that at the ground) is exp of the characteristic stress ratio. With the planet\'s surface gravity g and radius R and '
                  'the geostationary radius r, write the taper ratio T.',
          'name': 'The tether taper', 'lhs': 'T', 'form': 'exp ( ρ * g * R ^ 2 / ( σ * r ) )', 'expr': 'exp ( rho * g * R ^ 2 / ( sigma * r ) )',
          'ask': 'Find T for a nanotube tether: ρ = @{rho} kg/m³, σ = @{sigma} Pa, g = 9.81 m/s², R = 6.371e6 m, r = 4.216e7 m.',
          'vars': {'T': 'taper ratio', 'ρ': 'tether density', 'σ': 'tether strength', 'g': 'surface gravity', 'R': 'planet radius',
                   'r': 'geostationary radius'},
          'given': {'rho': 1300.0, 'sigma': 50000000000.0, 'g': 9.81, 'R': 6371000.0, 'r': 42160000.0}, 'unit': '',
          'method': 'Constant stress σ along the tether: dA/A = (ρ/σ)·(net gravity minus centrifugal)·dr; integrate to the top.',
          'distractors': ['ln', '*', '2'], 'vary': {'sigma': [30000000000.0, 90000000000.0, 10000000000.0]}},
         'space_elevator', 'composite_materials')
research('rotating_habitats', 'Rotating Habitats', 25, 9, 420, ['space_elevator', 'ecology'], [('stable_ecosystem', 1), ('passivated_plate', 16)], 600,
         "Spin a cylinder and its floor presses on your feet as gravity does: a = ω²r. Make it big and it can spin slowly enough "
         "that turning your head does not make you sick (under 2 turns a minute); but the hoop stress in its hull, ρω²r², grows "
         "with the square of its size, and that sets the largest you can build. O'Neill's cylinders were eight kilometres "
         "across. Unlocks the O'Neill Cylinder.",
         'Gravity from spin',
         {'goal': 'A habitat of radius r spins at angular speed ω. Write the apparent gravity a on its floor.',
          'name': 'Spin gravity', 'lhs': 'a', 'form': 'ω ^ 2 * r', 'expr': 'w ^ 2 * r',
          'ask': 'Find a in m/s² for r = @{r} m spinning at ω = @{w} rad/s.',
          'vars': {'a': 'apparent gravity', 'ω': 'angular speed', 'r': 'radius'},
          'given': {'w': 0.1, 'r': 1000.0}, 'unit': 'm/s²',
          'method': 'The floor must push you round the circle: the centripetal acceleration v²/r = ω²r.',
          'distractors': ['/', 'g', '2'], 'vary': {'r': [500.0, 4000.0, 250.0]}},
         'oneill_cylinder', 'space_elevator')
research('stellar_engines', 'Stellar Engines', 25, 11, 640, ['rotating_habitats', 'dyson_sphere'], [('shkadov_mirror_film', 16), ('stellar_core', 1)], 900,
         "Move a star. A Shkadov thruster is a vast curved mirror hung on one side of a star, held up by the star's own light, "
         "reflecting it back: the light pushes one way only, so the star drifts the other, with thrust F = fL/c. A Caplan thruster "
         "lifts matter from the star and fires it as a jet. Unlocks the Stellar Engine.",
         'Moving a star',
         {'goal': 'A mirror covering a fraction f of the sky round a star of luminosity L sends that light back. Write the net thrust F '
                  'on the star, with c the speed of light.',
          'name': 'The Shkadov thrust', 'lhs': 'F', 'form': 'f * L / c', 'expr': 'f * L / c',
          'ask': 'Find F in newtons for f = @{f}, L = @{L} W and c = @{c} m/s.',
          'vars': {'F': 'thrust', 'f': 'fraction of the sky covered', 'L': 'luminosity', 'c': 'speed of light'},
          'given': {'f': 0.5, 'L': 3.828e26, 'c': 299792458.0}, 'unit': 'N',
          'method': 'Light carries momentum E/c. Unbalanced: the reflected fraction f pushes back on the star with f·L/c.',
          'distractors': ['2', '*', 'G'], 'vary': {'f': [0.1, 0.5, 0.05]}},
         'stellar_engine', 'rotating_habitats')


# ====================================================================== machines
MACHINES = {}   # id -> dict(kind, name, lore, physics, game, recipe(crafting))


def machine(mid, name, lore, game, tier, physics, plang, crafting, multiblock=False):
    MACHINES[mid] = dict(name=name, lore=lore, multiblock=multiblock)
    LANG['block.%s.%s' % (NS, mid)] = name
    LANG['block.%s.%s.lore' % (NS, mid)] = lore
    dump(os.path.join(D, 'ts_physics', mid + '.json'), physics)
    for k, v in plang.items():
        LANG['physics.%s.%s.%s' % (NS, mid, k)] = v
    LANG['physics.%s.%s.title' % (NS, mid)] = name

    def g(dct):
        dct[mid] = {'game': game, 'tier': tier}
    edit_json(os.path.join(D, 'calibration/games.json'), g)
    dump(os.path.join(D, 'recipe/crafting', mid + '.json'), crafting)
    # blockstate and models (machines face the player; on while running)
    var = {}
    for facing, yrot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for act in (False, True):
            m = {'model': '%s:block/machine/%s_%s' % (NS, mid, 'on' if act else 'off')}
            if yrot:
                m['y'] = yrot
            var['active=%s,facing=%s' % ('true' if act else 'false', facing)] = m
    dump(os.path.join(A, 'blockstates', mid + '.json'), {'variants': var})
    for st in ('on', 'off'):
        dump(os.path.join(A, 'models/block/machine', '%s_%s.json' % (mid, st)),
             {'parent': 'minecraft:block/orientable', 'textures': {'top': '%s:block/machine/%s_top' % (NS, mid),
                                                                     'front': '%s:block/machine/%s_front_%s' % (NS, mid, st),
                                                                     'side': '%s:block/machine/%s_side' % (NS, mid)}})
    dump(os.path.join(A, 'models/item', mid + '.json'), {'parent': '%s:block/machine/%s_on' % (NS, mid)})
    loot(mid)


def loot(bid):
    dump(os.path.join(D, 'loot_table/blocks', bid + '.json'),
         {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ts(bid)}],
                                                'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def extreme(result, count, pattern, key):
    return {'type': ts('extreme_shaped'), 'pattern': pattern, 'key': {k: {'item': ts(v)} for k, v in key.items()},
            'result': {'id': ts(result), 'count': count}}


P_CONSOLE = ['AAAFFFAAA', 'AAAFFFAAA', 'AAAFFFAAA', 'FFFLLLFFF', 'FFFLNLFFF', 'FFFLLLFFF', 'AAAFFFAAA', 'AAAFFFAAA', 'AAAFFFAAA']
P_PART = ['OOOOOOOOO', 'OOOOOOOOO', 'OOOOOOOOO', 'OOOIIIOOO', 'OOOIIIOOO', 'OOOIIIOOO', 'OOOOOOOOO', 'OOOOOOOOO', 'OOOOOOOOO']
P_CTRL = ['FFFTTTFFF', 'FFFTTTFFF', 'FFFTTTFFF', 'MMMBBBMMM', 'MMMBBBMMM', 'MMMBBBMMM', 'FFFTTTFFF', 'FFFTTTFFF', 'FFFTTTFFF']


def par(pid, unit, lo, hi, step, default):
    return {'id': pid, 'unit': unit, 'min': lo, 'max': hi, 'step': step, 'default': default}


def der(did, unit, expr, formula):
    return {'id': did, 'unit': unit, 'expr': expr, 'formula': formula}


def con(cid, kind, expr, formula):
    return {'id': cid, 'kind': kind, 'expr': expr, 'formula': formula}


def model(params, rvars, derived, constraints, rate, rate_formula, rate_max=1.5, heat='0', idle='0'):
    return {'params': params, 'recipe_vars': rvars, 'consts': [], 'states': [], 'derived': derived, 'constraints': constraints,
            'rate': rate, 'rate_formula': rate_formula, 'heat': heat, 'idle_heat': idle, 'quench_energy': '0', 'kappa': 40,
            'heat_capacity': 60, 'timescale': 1, 'rate_max': rate_max}


# ---------------------------------------------------------------- Mind and Language consoles
NCA = model([par('gNa', 'mS/cm²', 10, 200, 1, 60), par('Dax', 'µm', 0.5, 20, 0.1, 2), par('my', 'wraps', 0, 100, 1, 0)],
            {'vmin': 60},
            [der('mf', '', 'clamp(my/40, 0, 1)', 'myelination: full at 40 wraps'),
             der('lam', 'mm', '0.5*sqrt(Dax*(1 + my))', 'λ = √(r_m/r_i): wraps raise r_m, girth lowers r_i'),
             der('v', 'm/s', '(6*Dax*mf + sqrt(Dax)*(1 - mf))*sqrt(gNa/120)*(1 + 0.02*d)', 'v ≈ 6·D myelinated, √D bare'),
             der('fire', '', 'gNa/40', 'channel drive over threshold')],
            [con('threshold', 'stop', 'gNa - 40', 'g_Na > 40 mS/cm² to fire at all'),
             con('excite', 'wear', '160 - gNa', 'g_Na < 160: beyond, the cells die of overexcitation'),
             con('gratio', 'wear', '2.5*Dax - my*0.05', 'myelin no thicker than the axon allows')],
            'clamp(gNa/120, 0.25, 1.5)', 'more sodium channels, faster cultures')
machine('neural_culture_array', 'Neural Culture Array',
        "A multi-electrode dish of living neurons, fed and kept warm, with glial cells coaxed to wrap the axons in myelin. "
        "Made by crafting.", 'wave', 2, NCA,
        {'p.gNa': 'Sodium channels', 'p.gNa.desc': 'How many sodium channels the cells express: below 40 they never fire; above 160 they die of it.',
         'p.Dax': 'Axon diameter', 'p.Dax.desc': 'Thicker axons conduct faster, but take more myelin to wrap.',
         'p.my': 'Myelin wraps', 'p.my.desc': 'Layers of glial membrane round each axon. Forty wraps make a full sheath.',
         'd.mf': 'Myelination', 'd.mf.desc': 'How much of a full sheath the axons have.',
         'd.lam': 'Length constant', 'd.lam.desc': 'How far a signal travels before it fades to a third.',
         'd.v': 'Conduction speed', 'd.v.desc': 'How fast a spike runs down the axon.', 'd.fire': 'Drive', 'd.fire.desc': 'Sodium drive over the firing threshold.',
         'c.threshold': 'Too few channels: the cells cannot fire', 'c.excite': 'Overexcitation is killing the culture',
         'c.gratio': 'More myelin than the axon can hold', 'v.vmin': 'Speed wanted', 'disturb': 'The culture drifts: speed varies by ±2%·d.',
         'rate.desc': 'More sodium channels, faster runs.'},
        extreme('neural_culture_array', 1, P_CONSOLE, {'A': 'minecraft:glass', 'F': 'ion_channel_membrane', 'L': 'polymer_sheet', 'N': 'neural_organoid'}))

ST = model([par('eta', '', 0.001, 0.5, 0.001, 0.05), par('N', 'epochs', 1, 500, 1, 20), par('xa', '', 0.05, 1, 0.01, 0.5)],
           {},
           [der('dw', '', 'eta*xa^2', 'Δw = η·x·y per step (y ≈ x)'),
            der('w', '', '(1 - exp(-eta*N*xa^2))*(1 - 0.01*d)', 'w = 1 − e^(−η·x²·N): how much of the pattern is learnt'),
            der('ov', '', 'eta*xa^2/0.25', 'step size over the stable limit (Oja)')],
           [con('runaway', 'stop', '0.25 - eta*xa^2', 'η·x² < 0.25 or the weights run away'),
            con('fatigue', 'wear', '400 - N', 'under 400 epochs: overtraining wears the lattice')],
           'clamp(100/N, 0.2, 1.2)', 'more epochs, slower runs')
machine('synapse_trainer', 'Synapse Trainer',
        "Trains a lattice of living synapses on a pattern, epoch after epoch, until it has learnt it. Made by crafting.", 'chain', 2, ST,
        {'p.eta': 'Learning rate', 'p.eta.desc': 'How much each synapse changes per step. Too high and the weights run away.',
         'p.N': 'Epochs', 'p.N.desc': 'How many times the pattern is shown. More learns more, but takes longer and wears the lattice past 400.',
         'p.xa': 'Activity', 'p.xa.desc': 'How strongly the training pattern drives the cells.',
         'd.dw': 'Step', 'd.dw.desc': 'The change in weight each time the pattern is shown.',
         'd.w': 'Learnt', 'd.w.desc': 'How much of the pattern the lattice now holds.',
         'd.ov': 'Stability', 'd.ov.desc': 'The step over the largest stable step: keep it below one.',
         'c.runaway': 'The weights are running away', 'c.fatigue': 'Overtraining is wearing the lattice',
         'disturb': 'Noise in the pattern: what is learnt varies by ±1%·d.', 'rate.desc': 'More epochs, slower runs.'},
        extreme('synapse_trainer', 1, P_CONSOLE, {'A': 'minecraft:copper_ingot', 'F': 'photonic_neuron', 'L': 'neural_lattice', 'N': 'myelinated_axon'}))

PB = model([par('Lg', 'log', -2, 3, 0.05, 0), par('cc', 'σ', 0, 4, 0.05, 1), par('n', 'trials', 10, 1000, 10, 100)],
           {},
           [der('dp', '', '0.5*ln(10^Lg/0.01)*(1 - 0.02*d)', "d′ = k·ln(I/I₀): the Weber–Fechner sensation"),
            der('H', '', '0.5*(1 + tanh(0.8*(dp - cc)))', 'hits: the share of signals seen'),
            der('F', '', '0.5*(1 + tanh(-0.8*cc))', 'false alarms: noise taken for a signal'),
            der('se', '', 'sqrt(H*(1 - H)/n)', 'the uncertainty on the hit rate')],
           [con('glare', 'wear', '2.5 - Lg', 'I below 10^2.5: brighter bleaches the retina'),
            con('blind', 'stop', 'dp', 'the signal must be above threshold at all')],
           'clamp(200/n, 0.2, 1.5)', 'more trials, slower runs')
machine('psychophysics_booth', 'Psychophysics Booth',
        "A dark booth where a cultured retina is shown faint flashes, over and over, and its answers counted. Made by crafting.", 'spectrum', 2, PB,
        {'p.Lg': 'Flash intensity', 'p.Lg.desc': 'How bright the test flash is, on a log scale: 0 is a hundred times the threshold.',
         'p.cc': 'Criterion', 'p.cc.desc': 'Where the retina sets its threshold: low sees more, and imagines more.',
         'p.n': 'Trials', 'p.n.desc': 'Flashes per run: more give a surer count, but take longer.',
         'd.dp': "Sensitivity d′", 'd.dp.desc': 'How far the signal stands out of the noise.', 'd.H': 'Hit rate', 'd.H.desc': 'Signals seen.',
         'd.F': 'False alarms', 'd.F.desc': 'Flashes seen that were not there.', 'd.se': 'Uncertainty', 'd.se.desc': 'The error on the hit rate.',
         'c.glare': 'The flash is bleaching the retina', 'c.blind': 'The flash is below threshold',
         'disturb': 'Adaptation: sensitivity drifts by ±2%·d.', 'rate.desc': 'More trials, slower runs.'},
        extreme('psychophysics_booth', 1, P_CONSOLE, {'A': 'minecraft:black_wool', 'F': 'photonic_lens', 'L': 'myelinated_axon', 'N': 'eel_photophore'}))

DD = model([par('Nc', 'log', 1, 6, 0.1, 3), par('k', 'symbols', 2, 64, 1, 26), par('g', '', 1, 5, 1, 1)],
           {'Kt': 24},
           [der('q', '', 'exp(-((k - Kt)/(0.15*Kt))^2)', 'how well the alphabet guess fits the script'),
            der('cov', '', '1 - exp(-10^Nc/(20*k^g))', 'enough text to see every context of g symbols'),
            der('dec', '', 'q*cov*g/(g + 0.5)*(1 - 0.01*d)', 'deciphered: fit × coverage × context'),
            der('Hb', 'bits', 'ln(k)/ln(2)', 'H = log₂N bits per symbol')],
           [con('corpus', 'stop', 'Nc - 1.5', 'at least 30 tokens of text')],
           'clamp(6/(Nc + 2), 0.3, 1.2)', 'more text, slower runs')
machine('decipherment_desk', 'Decipherment Desk',
        "A reading desk wired to a frequency counter: lay an unknown script on it and it tallies every symbol, pair and triple. "
        "Made by crafting.", 'cipher', 2, DD,
        {'p.Nc': 'Corpus', 'p.Nc.desc': 'How much text is counted, on a log scale: 3 is a thousand symbols.',
         'p.k': 'Alphabet guess', 'p.k.desc': 'How many distinct symbols you take the script to have.',
         'p.g': 'Context', 'p.g.desc': 'How many symbols are counted together: longer contexts read more, but need far more text.',
         'd.q': 'Fit', 'd.q.desc': 'How well the alphabet guess matches the script.', 'd.cov': 'Coverage', 'd.cov.desc': 'Whether there is enough text for that context.',
         'd.dec': 'Deciphered', 'd.dec.desc': 'How much of the script is read.', 'd.Hb': 'Entropy', 'd.Hb.desc': 'Information per symbol, in bits.',
         'c.corpus': 'Too little text to count', 'v.Kt': 'True alphabet', 'disturb': 'Scribal errors: the reading varies by ±1%·d.',
         'rate.desc': 'More text, slower runs.'},
        extreme('decipherment_desk', 1, P_CONSOLE, {'A': 'minecraft:bookshelf', 'F': 'one_time_pad', 'L': 'myelinated_axon', 'N': 'photonic_processor'}))

CA = model([par('Ln', 'log', 3, 11, 0.1, 6), par('p', '', 0.001, 0.5, 0.001, 0.2), par('Q', '', 0, 1, 0.01, 0.2)],
           {},
           [der('fp', '', '(1 - exp(-50*p))*exp(-4*p)', 'integration × sparseness'),
            der('Phi', '', 'Ln*4*Q*(1 - Q)*fp*(1 - 0.01*d)', 'Φ ≈ log n · 4Q(1−Q) · f(p): integrated and differentiated'),
            der('Pw', 'MW', '10^(Ln - 9)*p*40', 'power to run the connections')],
           [con('power', 'wear', '20 - 10^(Ln - 9)*p*40', 'P < 20 MW'),
            con('seizure', 'damage', '0.3 - p*(1 - Q)', 'a dense, unmodular net seizes')],
           'clamp(Phi/6, 0.2, 1.5)', 'more Φ, faster mapping', heat='0.5 + 10^(Ln - 9)*p*4', idle='0.1')
machine('connectome_array', 'Connectome Array',
        "The controller of a dome of glial scaffold threaded with axon tracts: it maps every connection of a living network and "
        "can write one back. Made by crafting.", 'balance', 3, CA,
        {'p.Ln': 'Neurons', 'p.Ln.desc': 'How many neurons the network holds, on a log scale: 9 is a billion.',
         'p.p': 'Connectivity', 'p.p.desc': 'The chance any two are joined. Sparse but connected is best.',
         'p.Q': 'Modularity', 'p.Q.desc': 'How far the network is split into specialised modules. Half is best.',
         'd.fp': 'Integration', 'd.fp.desc': 'How well connected, without being a single mass.', 'd.Phi': 'Φ', 'd.Phi.desc': 'Integrated information (a toy measure).',
         'd.Pw': 'Power', 'd.Pw.desc': 'What the connections draw.', 'c.power': 'The array is drawing too much power',
         'c.seizure': 'The network is seizing', 'disturb': 'Noise: Φ varies by ±1%·d.', 'rate.desc': 'More Φ, faster mapping.'},
        extreme('connectome_array', 1, P_CTRL, {'F': 'glial_scaffold', 'T': 'synapse_relay', 'M': 'axon_bundle', 'B': 'neural_lattice'}),
        multiblock=True)

# ---------------------------------------------------------------- Engineering of the Large consoles
LT = model([par('h', 'mm', 50, 600, 5, 150), par('tf', 'mm', 4, 40, 1, 8), par('Lm', 'm', 1, 12, 0.5, 6)],
           {'Pk': 500, 'Eg': 200},
           [der('b', 'mm', 'h/2', 'flange width'),
            der('I', 'mm⁴', '(b*h^3 - (b - tf/2)*(h - 2*tf)^3)/12', 'second moment of the I-section'),
            der('Ar', 'mm²', '2*b*tf + (h - 2*tf)*tf/2', 'section area'),
            der('Pcr', 'kN', '9.8696*Eg*I/(1000*Lm)^2', 'P = π²EI/L²'),
            der('SF', '', 'Pcr/(Pk*(1 + 0.02*d))', 'safety factor against buckling'),
            der('sig', 'MPa', 'Pk*1000/Ar', 'direct stress σ = P/A'),
            der('kg', 'kg', '0.00785*Ar*Lm', 'mass of the beam')],
           [con('yield', 'stop', '250 - Pk*1000/Ar', 'σ < 250 MPa: steel yields'),
            con('heavy', 'wear', '400 - 0.00785*Ar*Lm', 'under 400 kg on the rig')],
           'clamp(150/(0.00785*Ar*Lm), 0.2, 1.5)', 'lighter beams test faster')
machine('load_test_rig', 'Load Test Rig',
        "A hydraulic frame that loads a beam end to end until it bows, and rates it for what it carried. Made by crafting.", 'equilibrium', 2, LT,
        {'p.h': 'Section depth', 'p.h.desc': 'How deep the I-section is. Deeper is far stiffer (I grows as the cube).',
         'p.tf': 'Flange thickness', 'p.tf.desc': 'How thick the flanges and web are.', 'p.Lm': 'Length', 'p.Lm.desc': 'How long the column is.',
         'd.b': 'Flange width', 'd.b.desc': 'Half the depth.', 'd.I': 'Second moment', 'd.I.desc': 'Stiffness of the shape against bending.',
         'd.Ar': 'Area', 'd.Ar.desc': 'Steel in the section.', 'd.Pcr': 'Buckling load', 'd.Pcr.desc': "Euler's load: where it bows.",
         'd.SF': 'Safety factor', 'd.SF.desc': 'Buckling load over the load it must carry. Rated beams are two to four.',
         'd.sig': 'Stress', 'd.sig.desc': 'Load over area.', 'd.kg': 'Mass', 'd.kg.desc': 'What the beam weighs.',
         'c.yield': 'The steel is yielding', 'c.heavy': 'The beam is too heavy for the rig', 'v.Pk': 'Rated load', 'v.Eg': 'Modulus',
         'disturb': 'Imperfection: the load varies by ±2%·d.', 'rate.desc': 'Lighter beams test faster.'},
        extreme('load_test_rig', 1, P_CONSOLE, {'A': 'minecraft:iron_ingot', 'F': 'minecraft:piston', 'L': 'electrical_steel_lamination', 'N': 'minecraft:anvil'}))

CL = model([par('Vf', '', 0.1, 0.8, 0.01, 0.4), par('th', '°', 0, 90, 1, 30), par('Tc', '°C', 80, 220, 1, 120)],
           {'Ef': 230, 'rf': 1.8, 'Em': 3.5, 'rm': 1.2},
           [der('E1', 'GPa', 'Vf*Ef + (1 - Vf)*Em', 'rule of mixtures'),
            der('Eth', 'GPa', 'E1*cos(th/57.3)^4 + Em*(1 - cos(th/57.3)^4)', 'stiffness off the fibre direction'),
            der('q', '', 'exp(-(pos(Vf - 0.62)/0.05)^2)*(1 - 0.01*d)', 'fibres packed past 62% leave voids'),
            der('rho', 'g/cm³', 'Vf*rf + (1 - Vf)*rm', 'density'),
            der('S', 'GPa·cm³/g', 'Eth*q/rho', 'specific stiffness')],
           [con('cure', 'stop', 'Tc - 110', 'cure above 110 °C'),
            con('burn', 'damage', '190 - Tc', 'under 190 °C: the resin chars')],
           'clamp((Tc - 100)/60, 0.2, 1.5)', 'hotter cures faster')
machine('composite_layup_press', 'Composite Layup Press',
        "Lays plies of fibre in resin at set angles and cures them under heat and pressure. Made by crafting.", 'flow', 2, CL,
        {'p.Vf': 'Fibre fraction', 'p.Vf.desc': 'Share of the part that is fibre. Past 62% the resin cannot wet it all.',
         'p.th': 'Ply angle', 'p.th.desc': 'Angle of the fibres to the load. Along them is stiff; across them, only the resin.',
         'p.Tc': 'Cure temperature', 'p.Tc.desc': 'Hotter cures faster; above 190 °C the resin chars.',
         'd.E1': 'Stiffness along', 'd.E1.desc': 'By the rule of mixtures.', 'd.Eth': 'Stiffness', 'd.Eth.desc': 'At the ply angle.',
         'd.q': 'Consolidation', 'd.q.desc': 'Free of voids.', 'd.rho': 'Density', 'd.rho.desc': 'Mass per volume.',
         'd.S': 'Specific stiffness', 'd.S.desc': 'Stiffness for its weight.', 'c.cure': 'The resin will not cure', 'c.burn': 'The resin is charring',
         'v.Ef': 'Fibre modulus', 'v.rf': 'Fibre density', 'v.Em': 'Resin modulus', 'v.rm': 'Resin density',
         'disturb': 'Ply wrinkles: consolidation varies by ±1%·d.', 'rate.desc': 'Hotter cures faster.'},
        extreme('composite_layup_press', 1, P_CONSOLE, {'A': 'minecraft:iron_ingot', 'F': 'minecraft:piston', 'L': 'polymer_sheet', 'N': 'carbon_nanotube_bundle'}))

TB = model([par('W', 'N', 10, 1000, 5, 100), par('vs', 'm/s', 0.1, 10, 0.1, 1), par('hc', 'µm', 0.1, 10, 0.1, 1)],
           {'Hg': 20, 'kw': 1},
           [der('Pf', 'W', 'W*vs', 'test duty: load × speed'),
            der('tau', 'h', '5556*hc*Hg/(20*kw*W*vs)*(1 - 0.02*d)', 'life: thickness over Archard wear rate kW·v/H'),
            der('q', 'W', '0.1*W*vs', 'friction heat μWv')],
           [con('duty', 'stop', 'W*vs - 200', 'test at the rated duty: W·v > 200 W'),
            con('flash', 'wear', '400 - 0.1*W*vs', 'friction under 400 W'),
            con('spall', 'wear', '8 - hc', 'coatings over 8 µm spall off')],
           'clamp(W*vs/400, 0.2, 1.5)', 'harder tests run faster')
machine('tribometer', 'Tribometer',
        "A pin pressed on a spinning disc: it measures friction and wear, and qualifies coatings for a machine's sliding parts. "
        "Made by crafting.", 'flow', 2, TB,
        {'p.W': 'Load', 'p.W.desc': 'How hard the pin presses.', 'p.vs': 'Sliding speed', 'p.vs.desc': 'How fast the disc turns under it.',
         'p.hc': 'Coating thickness', 'p.hc.desc': 'Thicker lasts longer, but past 8 µm it spalls.',
         'd.Pf': 'Duty', 'd.Pf.desc': 'Load times speed: the test must be at least the rated 200 W.',
         'd.tau': 'Life', 'd.tau.desc': 'Hours until the coating is worn through.', 'd.q': 'Friction heat', 'd.q.desc': 'Heat at the contact.',
         'c.duty': 'The test is too gentle to rate', 'c.flash': 'The contact is overheating', 'c.spall': 'The coating is spalling',
         'v.Hg': 'Hardness', 'v.kw': 'Wear coefficient', 'disturb': 'Debris: life varies by ±2%·d.', 'rate.desc': 'Harder tests run faster.'},
        extreme('tribometer', 1, P_CONSOLE, {'A': 'minecraft:iron_ingot', 'F': 'alumina_ceramic_plate', 'L': 'rated_i_beam', 'N': 'minecraft:grindstone'}))

CC = model([par('pH', '', 0, 14, 0.1, 7), par('Ev', 'V', -1.5, 1, 0.01, -0.3), par('Cl', 'log', -4, 0, 0.1, -2)],
           {},
           [der('imm', 'V', 'Ev + 0.62', 'over the immunity line (Fe/Fe²⁺ at 10⁻⁶)'),
            der('pas', '', 'step(pH - 9)*step(13 - pH)', 'inside the passive window'),
            der('Ep', 'V', '0.3 - 0.15*(Cl + 4)', 'pitting potential: chloride lowers it'),
            der('cr', 'mm/y', 'step(imm)*exp(min(imm, 0.6)/0.06)*(1 - pas*step(Ep - Ev))*0.001*(1 + 0.02*d)', 'corrosion rate (Tafel)'),
            der('clv', 'log', 'Cl', 'chloride in the test water')],
           [con('h2', 'wear', 'Ev + 1.2', 'above −1.2 V: hydrogen embrittles the steel')],
           'clamp(1 + Cl/8, 0.3, 1.2)', 'salty water tests slower')
machine('corrosion_cell', 'Corrosion Cell',
        "A potentiostat and a tank of test water: it holds a sample at a set potential and pH and measures how fast it dissolves. "
        "Made by crafting.", 'oxidation', 2, CC,
        {'p.pH': 'pH', 'p.pH.desc': 'Iron passivates between pH 9 and 13.', 'p.Ev': 'Potential', 'p.Ev.desc': 'The sample\'s potential against hydrogen: below −0.62 V iron cannot corrode.',
         'p.Cl': 'Chloride', 'p.Cl.desc': 'Salt in the water, on a log scale: 0 is sea water strength, −4 fresh water.',
         'd.imm': 'Over immunity', 'd.imm.desc': 'How far above the line where iron cannot dissolve.', 'd.pas': 'Passive', 'd.pas.desc': '1 in the passive window.',
         'd.Ep': 'Pitting potential', 'd.Ep.desc': 'Above this, chloride breaks the passive film.', 'd.cr': 'Corrosion rate',
         'd.cr.desc': 'Millimetres a year lost.', 'd.clv': 'Chloride', 'd.clv.desc': 'In the test water.',
         'c.h2': 'Hydrogen is embrittling the steel', 'disturb': 'Oxygen in the water: the rate varies by ±2%·d.', 'rate.desc': 'Salty water tests slower.'},
        extreme('corrosion_cell', 1, P_CONSOLE, {'A': 'minecraft:glass', 'F': 'nickel_plated_iron', 'L': 'aluminium_ingot', 'N': 'minecraft:cauldron'}))

SE = model([par('TR', '', 1, 20, 0.05, 1.5), par('vc', 'm/s', 10, 300, 1, 50), par('mt', 't', 1, 50, 0.5, 10)],
           {'Sp': 60},
           [der('Rq', '', 'exp(48.4/Sp)', 'taper the tether needs: exp(ρgR²/σr) = e^(48.4/specific strength)'),
            der('tm', '', 'TR/(Rq*(1 + 0.02*d))', 'taper margin'),
            der('Pc', 'MW', 'mt*9.81*vc/1000', 'climber power P = mgv')],
           [con('snap', 'stop', 'TR - exp(48.4/Sp)', 'taper over the needed ratio, or the tether snaps'),
            con('beam', 'damage', '50 - mt*9.81*vc/1000', 'climber under 50 MW of beamed power')],
           'clamp(vc/100, 0.1, 2)', 'faster climbers, faster lifts', rate_max=2, heat='0.2 + mt*9.81*vc/100000', idle='0.05')
machine('space_elevator', 'Space Elevator',
        "The anchor station of a tether that runs from the ground to beyond geostationary orbit: climbers carry capsules, "
        "laboratories and station modules up it on beamed power alone. Made by crafting.", 'balance', 3, SE,
        {'p.TR': 'Taper ratio', 'p.TR.desc': 'How much thicker the tether is at the top than at the ground. Too little and it snaps; much more is wasted mass.',
         'p.vc': 'Climber speed', 'p.vc.desc': 'Faster climbs finish sooner, but need more power.', 'p.mt': 'Payload', 'p.mt.desc': 'Tonnes per climber.',
         'd.Rq': 'Taper needed', 'd.Rq.desc': 'Set by the tether\'s specific strength.', 'd.tm': 'Taper margin', 'd.tm.desc': 'Taper over what is needed: 1 to 1.5.',
         'd.Pc': 'Climber power', 'd.Pc.desc': 'Beamed to the climber from the ground.', 'c.snap': 'The tether is snapping', 'c.beam': 'The power beam is overheating the climber',
         'v.Sp': 'Specific strength', 'disturb': 'Wind and tides: the load varies by ±2%·d.', 'rate.desc': 'Faster climbers, faster lifts.'},
        extreme('space_elevator', 1, P_CTRL, {'F': 'elevator_anchor', 'T': 'tether_ribbon', 'M': 'climber_car', 'B': 'crew_capsule'}), multiblock=True)

OC = model([par('rpm', 'rpm', 0.1, 4, 0.01, 1), par('Rm', 'm', 100, 8000, 10, 500), par('tw', 'm', 0.5, 10, 0.1, 2)],
           {'rhoh': 1600, 'sy': 1500},
           [der('w', 'rad/s', 'rpm*0.10472', 'ω'),
            der('a', 'm/s²', 'w^2*Rm*(1 + 0.01*d)', 'spin gravity a = ω²r'),
            der('vr', 'm/s', 'w*Rm', 'rim speed'),
            der('hs', 'MPa', 'rhoh*(w*Rm)^2/1000000*(1 + 200/tw)', 'hoop stress ρω²r², plus the air and soil on the hull')],
           [con('sick', 'wear', '2 - rpm', 'under 2 rpm, or the colonists are spin-sick'),
            con('hoop', 'stop', 'sy - rhoh*(w*Rm)^2/1000000*(1 + 200/tw)', 'hoop stress under the hull\'s strength')],
           'clamp(Rm/2000, 0.2, 1.5)', 'bigger habitats grow more', heat='0.3', idle='0.1')
machine('oneill_cylinder', "O'Neill Cylinder",
        "The controller of a spinning habitat: a cylinder of hull and window strips turning on its bearings, with land, air and "
        "water inside under spin gravity. Made by crafting.", 'equilibrium', 4, OC,
        {'p.rpm': 'Spin', 'p.rpm.desc': 'Turns a minute. Over 2 and the colonists are sick.', 'p.Rm': 'Radius', 'p.Rm.desc': 'Of the design the controller scales to.',
         'p.tw': 'Hull thickness', 'p.tw.desc': 'Thicker hulls carry the load of the air and soil.',
         'd.w': 'Angular speed', 'd.w.desc': 'Radians a second.', 'd.a': 'Spin gravity', 'd.a.desc': 'What the floor presses with: one gee is 9.8.',
         'd.vr': 'Rim speed', 'd.vr.desc': 'How fast the hull moves.', 'd.hs': 'Hoop stress', 'd.hs.desc': 'The pull in the hull.',
         'c.sick': 'The colonists are spin-sick', 'c.hoop': 'The hull is tearing', 'v.rhoh': 'Hull density', 'v.sy': 'Hull strength',
         'disturb': 'Wobble: gravity varies by ±1%·d.', 'rate.desc': 'Bigger habitats grow more.'},
        extreme('oneill_cylinder', 1, P_CTRL, {'F': 'habitat_hull', 'T': 'habitat_window', 'M': 'spin_bearing', 'B': 'stable_ecosystem'}), multiblock=True)

SEN = model([par('fm', '', 0, 0.5, 0.01, 0.1), par('sg', 'g/m²', 0.1, 5, 0.01, 1), par('jf', '', 0, 1, 0.01, 0)],
            {},
            [der('F', '', 'fm*(1 + jf)*(1 - 0.02*d)', 'thrust: mirror share f·L/c, doubled by the Caplan jets'),
             der('Tm', 'K', '394/sqrt(1 - fm)', 'mirror temperature'),
             der('sc', 'g/m²', '0.78', 'statite limit: L/(4πcGM)')],
            [con('fall', 'stop', '0.78 - sg', 'mirror lighter than 0.78 g/m², or it falls into the star'),
             con('melt', 'damage', '1500 - 394/sqrt(1 - fm)', 'mirror under 1500 K'),
             con('jets', 'wear', '0.9 - jf', 'jets under 90%: the star is stripped too fast')],
            'clamp(F*3, 0.1, 3)', 'more thrust, more power', rate_max=3, heat='2 + jf*4', idle='0.5')
machine('stellar_engine', 'Stellar Engine',
        "The controller of a Shkadov thruster and its Caplan jets: a curved mirror hung by starlight on one side of a captive "
        "star, and jets of the star's own matter. The star moves; the light it throws back powers everything else. Made by crafting.",
        'balance', 4, SEN,
        {'p.fm': 'Mirror coverage', 'p.fm.desc': 'Share of the star\'s sky the mirror covers.', 'p.sg': 'Mirror areal density',
         'p.sg.desc': 'Grams per square metre: heavier than 0.78 and light cannot hold it up.',
         'p.jf': 'Jet fraction', 'p.jf.desc': 'How hard the Caplan jets fire.', 'd.F': 'Thrust', 'd.F.desc': 'In units of the star\'s light over c.',
         'd.Tm': 'Mirror temperature', 'd.Tm.desc': 'Kelvin.', 'd.sc': 'Statite limit', 'd.sc.desc': 'The heaviest mirror light can hold up.',
         'c.fall': 'The mirror is falling into the star', 'c.melt': 'The mirror is melting', 'c.jets': 'The jets are stripping the star',
         'disturb': 'Flares: thrust varies by ±2%·d.', 'rate.desc': 'More thrust, more power.'},
        extreme('stellar_engine', 1, P_CTRL, {'F': 'shkadov_mirror', 'T': 'caplan_jet', 'M': 'dyson_shell_segment', 'B': 'stellar_core'}), multiblock=True)


# ====================================================================== casings, blocks and items
def simple_block(bid, name, lore, found=None, light=False):
    dump(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': '%s:block/%s' % (NS, bid)}}})
    dump(os.path.join(A, 'models/block', bid + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s' % (NS, bid)}})
    dump(os.path.join(A, 'models/item', bid + '.json'), {'parent': '%s:block/%s' % (NS, bid)})
    loot(bid)
    LANG['block.%s.%s' % (NS, bid)] = name
    LANG['block.%s.%s.lore' % (NS, bid)] = lore
    if found:
        LANG['block.%s.%s.found' % (NS, bid)] = found


def casing(cid, name, lore):
    dump(os.path.join(A, 'blockstates', cid + '.json'), {'variants': {
        'formed=false,moving=false': {'model': '%s:block/%s' % (NS, cid)}, 'formed=false,moving=true': {'model': '%s:block/%s' % (NS, cid)},
        'formed=true,moving=false': {'model': '%s:block/%s_formed' % (NS, cid)}, 'formed=true,moving=true': {'model': '%s:block/moving_hidden' % NS}}})
    for suf in ('', '_formed'):
        dump(os.path.join(A, 'models/block', cid + suf + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s%s' % (NS, cid, suf)}})
    dump(os.path.join(A, 'models/item', cid + '.json'), {'parent': '%s:block/%s' % (NS, cid)})
    loot(cid)
    LANG['block.%s.%s' % (NS, cid)] = name
    LANG['block.%s.%s.lore' % (NS, cid)] = lore


def item(iid, name, lore, found=None):
    dump(os.path.join(A, 'models/item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s' % (NS, iid)}})
    LANG['item.%s.%s' % (NS, iid)] = name
    LANG['item.%s.%s.lore' % (NS, iid)] = lore
    if found:
        LANG['item.%s.%s.found' % (NS, iid)] = found


casing('glial_scaffold', 'Glial Scaffold', "A lattice of polymer seeded with glial cells, the brain's own scaffolding: they hold neurons in place, feed them and insulate them. The Connectome Array's frame. Made by crafting.")
casing('axon_bundle', 'Axon Bundle', "A tract of myelinated axons laid side by side in a sleeve, white with myelin and faintly lit as signals run down it. The Connectome Array's wiring. Made by crafting.")
casing('synapse_relay', 'Synapse Relay', "A node where a thousand axons meet a thousand dendrites through trained synapses, pulsing as it passes signals on. The Connectome Array's junctions. Made by crafting.")
casing('elevator_anchor', 'Elevator Anchor', "Rated steel and passivated plate, sunk deep: the ground station that holds a space elevator's tether down against the pull of its counterweight. Made by crafting.")
casing('tether_ribbon', 'Tether Ribbon', "A ribbon of nanotube composite a metre wide and thinner than paper, the strongest thing for its weight ever made: the space elevator's cable. Made by crafting.")
casing('climber_car', 'Climber Car', "A cage of I-beams on caterpillar treads that grip the tether, with a photovoltaic dish beneath that drinks the power beamed up from the ground. Made by crafting.")
casing('habitat_hull', 'Habitat Hull', "Passivated steel laminated with carbon fibre, metres thick with soil and water on its inside face: it holds a spinning world together. Made by crafting.")
casing('habitat_window', 'Habitat Window', "A strip of fused-silica glass in a composite frame, running the habitat's length: mirrors outside throw sunlight in through it. Made by crafting.")
casing('spin_bearing', 'Spin Bearing', "A great coated bearing on which a habitat turns: it must run for centuries with no one able to stop the spin to service it. Made by crafting.")
casing('shkadov_mirror', 'Shkadov Mirror', "A panel of a mirror so thin that starlight holds it up: aluminium film on a polymer sail, lighter than 0.78 grams a square metre. Made by crafting.")
casing('caplan_jet', 'Caplan Jet', "A magnetic nozzle that lifts plasma off a star's surface and fires it as a jet: half the thrust of a Stellar Engine, and its power. Made by crafting.")

simple_block('grey_matter', 'Grey Matter', "Packed cell bodies and their dendrites: where a brain computes. Soft, warm and faintly lit by the signals in it.",
             "Found: in Inner Space (Neural Cortex), covering the ground.")
simple_block('white_matter', 'White Matter', "Bundles of myelinated axons: the cables a brain wires its grey matter together with. White with myelin.",
             "Found: in Inner Space (Neural Cortex), under the grey matter and in the neurons that grow there.")
simple_block('neuron_soma', 'Neuron Soma', "The body of a neuron, the size of a house at this scale, glowing as it fires: the light of the Neural Cortex.",
             "Found: in Inner Space (Neural Cortex), at the roots and tips of the giant neurons.")
simple_block('inscribed_glyph_stone', 'Inscribed Glyph Stone', "A stone panel cut with rows of the old glyph alphabet, still glowing faintly in its grooves. No one has read it in a long time.",
             "Found: in the Library Ruins of the Overworld's forests and taigas; made by crafting.")
simple_block('glyph_stone_bricks', 'Glyph Stone Bricks', "Dark dressed stone with a glyph stamped on every brick, as a library marks its walls.",
             "Found: in the Library Ruins of the Overworld's forests and taigas; made by crafting.")
simple_block('archive_shelf', 'Archive Shelf', "A shelf of stone tablets, each a page of a book no one could read until the Decipherment Desk.",
             "Found: in the Library Ruins of the Overworld's forests and taigas; made by crafting.")
# the anode: its charge shows as it corrodes
dump(os.path.join(A, 'blockstates', 'sacrificial_anode.json'), {'variants': {
    'charge=%d' % c: {'model': '%s:block/sacrificial_anode%s' % (NS, '' if c >= 3 else '_pitted' if c >= 1 else '_spent')} for c in range(5)}})
for suf in ('', '_pitted', '_spent'):
    dump(os.path.join(A, 'models/block', 'sacrificial_anode' + suf + '.json'),
         {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/sacrificial_anode%s' % (NS, suf)}})
dump(os.path.join(A, 'models/item', 'sacrificial_anode.json'), {'parent': '%s:block/sacrificial_anode' % NS})
dump(os.path.join(D, 'loot_table/blocks', 'sacrificial_anode.json'), {'type': 'minecraft:block', 'pools': [
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ts('sacrificial_anode'),
                              'conditions': [{'condition': 'minecraft:inverted', 'term': {'condition': 'minecraft:block_state_property', 'block': ts('sacrificial_anode'),
                                                                                         'properties': {'charge': '0'}}}]},
                             {'type': 'minecraft:item', 'name': ts('rust_flakes'),
                              'conditions': [{'condition': 'minecraft:block_state_property', 'block': ts('sacrificial_anode'), 'properties': {'charge': '0'}}],
                              'functions': [{'function': 'minecraft:set_count', 'count': 2}]}],
     'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
LANG['block.%s.sacrificial_anode' % NS] = 'Sacrificial Anode'
LANG['block.%s.sacrificial_anode.lore' % NS] = ("A block of aluminium-zinc alloy, less noble than iron, wired into a structure: it corrodes so the structure does not. "
                                               "Rust Mites within twelve blocks go for it before any machine, and eat it away in four bites; spent, it gives back only rust. "
                                               "Made in the Corrosion Cell.")

item('myelinated_axon', 'Myelinated Axon', "A nerve fibre wrapped in forty layers of glial membrane: the spike jumps from gap to gap down it, a hundred times faster than down a bare one. Made in the Neural Culture Array.")
item('synaptic_lattice', 'Synaptic Lattice', "A lattice of living synapses trained until it holds a pattern, and still learning: fitted to a machine, it learns the machine's work. +10% products, and precision falls off a third slower. One upgrade per machine. Made in the Synapse Trainer.")
item('rhodopsin_retina', 'Rhodopsin Retina', "A cultured sheet of rod cells, each able to count a single photon, its threshold trained to see in near darkness without imagining things. Made in the Psychophysics Booth.")
item('low_light_visor', 'Low-Light Visor', "A rhodopsin retina behind an image intensifier, fed by an anglerfish's bacteria, worn over the eyes: night vision while worn, and in the Hadal Trench it sees the deep as it is, clear and far. Made by crafting.")
item('glyph_tablet', 'Glyph Tablet', "A stone tablet cut with the old glyph alphabet, dense with text. Its symbols come up with the frequencies of a real language; it says something. The Decipherment Desk can find out what.",
     "Found: in Library Ruin caches, in the Overworld's forests, taigas and jungles.")
item('lexicon_fragment', 'Lexicon Fragment', "A list of glyphs and the words they stand for, worked out from how often each appears and what it appears next to. Made at the Decipherment Desk.")
item('universal_grammar', 'Universal Grammar', "The rules beneath the glyph language, and beneath every language: how words build sentences, written out as a machine could follow them. Made at the Decipherment Desk.")
item('engram', 'Engram', "A memory, mapped synapse by synapse out of a living network and written into a crystal lattice: the unit of a mind that can be copied. Made in the Connectome Array.")
item('dream_polyp', 'Dream Polyp', "A lobe of a Dreaming Colony, still faintly lit and twitching: a few thousand neurons that keep dreaming without the rest.",
     "Found: dropped by the Dreaming Colony (Inner Space, Neural Cortex).")
item('rated_i_beam', 'Rated I-Beam', "A steel I-beam load-tested to a safety factor between two and four against buckling: strong enough, and no heavier than it needs to be. Made on the Load Test Rig.")
item('carbon_fibre_composite', 'Carbon-Fibre Composite', "Plies of carbon fibre in cured epoxy, laid at the right angles: as stiff as steel at a fifth of the weight. Made in the Composite Layup Press.")
item('nanotube_composite', 'Nanotube Composite', "Aligned carbon nanotubes in a polymer matrix: stiffer for its weight than anything else that can be made in bulk. Made in the Composite Layup Press.")
item('wear_coating', 'Wear Coating', "A few micrometres of diamond-like carbon, hard and slippery, for every sliding part of a machine: fitted, its catalysts wear half as fast. One upgrade per machine. Made in the Tribometer.")
item('passivated_plate', 'Passivated Plate', "Steel plate with a phosphated surface that, held in its passive window, grows a film of oxide too thin to see and too tight for rust to get under. Made in the Corrosion Cell.")
item('rust_flakes', 'Rust Flakes', "Flakes of iron oxide, mostly haematite: what a Rust Mite sheds, or what is left of a spent anode. Rich in iron, and a passivating pigment.",
     "Found: dropped by the Rust Mite (Titan, the Antiverse, derelict orbital rings) and left by spent Sacrificial Anodes.")
item('tether_spool', 'Tether Spool', "A drum of nanotube ribbon wound on aramid, tested to the taper a space elevator needs. Made in the Composite Layup Press.")
item('shkadov_mirror_film', 'Shkadov Mirror Film', "Aluminium a few dozen atoms thick on a polymer sail: light enough that starlight alone holds it up. Made in the Composite Layup Press.")
for egg, lore in (('dreaming_colony', "A knot of a few hundred neurons grown into one floating mind, dreaming aloud. It learns how it is fought: the more often one kind of blow hurts it, the less that kind does (down to half), and it keeps further from fighters who close in and nearer to those who shoot. Every few seconds it pulses, dazing whoever is within seven blocks. Found in Inner Space's Neural Cortex."),
                  ('rust_mite', "An iron-eating pest with a shell of flaking rust. It feeds on the charge of machines within sixteen blocks, a bite a second; a live Sacrificial Anode nearby draws it off, and it eats that instead. It shies from players and bites only when struck. Found in Titan, the Antiverse and the derelict orbital rings.")):
    LANG['item.%s.%s_spawn_egg' % (NS, egg)] = {'dreaming_colony': 'Dreaming Colony', 'rust_mite': 'Rust Mite'}[egg] + ' Spawn Egg'
    LANG['item.%s.%s_spawn_egg.lore' % (NS, egg)] = lore
    LANG['entity.%s.%s.lore' % (NS, egg)] = lore
    dump(os.path.join(A, 'models/item', egg + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
LANG['entity.%s.dreaming_colony' % NS] = 'Dreaming Colony'
LANG['entity.%s.rust_mite' % NS] = 'Rust Mite'
LANG['tooltip.%s.upgrade.synaptic_lattice' % NS] = 'Upgrade: +10% products, precision falls off a third slower'
LANG['tooltip.%s.upgrade.wear_coating' % NS] = 'Upgrade: catalysts wear half as fast'
LANG['biome.%s.neural_cortex' % NS] = 'Neural Cortex'
LANG['message.%s.capsule_pad' % NS] = 'There is no formed Rocket Assembly or Space Elevator within 32 blocks to fly it.'

# crafting: parts and blocks
parts = [('glial_scaffold', 8, 'minecraft:iron_ingot', 'lipid_bilayer'), ('axon_bundle', 8, 'minecraft:copper_ingot', 'myelinated_axon'),
         ('synapse_relay', 4, 'minecraft:gold_ingot', 'photonic_neuron'), ('elevator_anchor', 16, 'minecraft:iron_ingot', 'rated_i_beam'),
         ('tether_ribbon', 16, 'carbon_fibre_composite', 'tether_spool'), ('climber_car', 4, 'rated_i_beam', 'solar_statite'),
         ('habitat_hull', 16, 'passivated_plate', 'carbon_fibre_composite'), ('habitat_window', 16, 'fused_silica', 'carbon_fibre_composite'),
         ('spin_bearing', 4, 'passivated_plate', 'wear_coating'), ('shkadov_mirror', 8, 'shkadov_mirror_film', 'solar_statite'),
         ('caplan_jet', 2, 'dyson_shell_segment', 'de_laval_nozzle'), ('glyph_stone_bricks', 16, 'minecraft:deepslate_bricks', 'glyph_tablet'),
         ('inscribed_glyph_stone', 8, 'glyph_stone_bricks', 'lexicon_fragment'), ('archive_shelf', 8, 'minecraft:smooth_stone', 'glyph_tablet')]
for pid, n, outer, inner in parts:
    dump(os.path.join(D, 'recipe/crafting', 'r140_%s.json' % pid), extreme(pid, n, P_PART, {'O': outer, 'I': inner}))
dump(os.path.join(D, 'recipe/crafting', 'r140_low_light_visor.json'),
     extreme('low_light_visor', 1, P_CONSOLE, {'A': 'pressure_hull_plate', 'F': 'anglerfish_lure', 'L': 'rhodopsin_retina', 'N': 'photonic_lens'}))


# ====================================================================== machine recipes (each checked against its physics)
RECIPES = []


def mrec(rtype, rid, ins, fins, outs, fouts, energy, dur, rsearch, vars_, windows, machine_id, extra=None):
    r = {'type': ts(rtype)}
    if ins:
        r['item_inputs'] = [{'item': ts(i), 'count': n} for i, n in ins]
    if fins:
        r['fluid_inputs'] = [{'fluid': ts(f), 'amount': n} for f, n in fins]
    if outs:
        r['item_outputs'] = [{'id': ts(i), 'count': n} for i, n in outs]
    if fouts:
        r['fluid_outputs'] = [{'id': ts(f), 'amount': n} for f, n in fouts]
    r['energy'] = energy
    r['duration'] = dur
    r['research'] = ts(rsearch)
    r['unscaled'] = True
    phys = {}
    if vars_:
        phys['vars'] = vars_
    if windows:
        phys['windows'] = [{'q': q, 'min': a, 'max': b} for q, a, b in windows]
    if phys:
        r['physics'] = phys
    if extra:
        r.update(extra)
    RECIPES.append((machine_id, rid, r))
    dump(os.path.join(D, 'recipe', rtype, rid + '.json'), r)


mrec('neural_culture', 'r140_myelinated_axon', [('neural_organoid', 1), ('lipid_bilayer', 2)], [('blood_plasma', 500)], [('myelinated_axon', 4)], [],
     256, 400, 'action_potential', {'vmin': 60}, [('v', 60, 150)], 'neural_culture_array')
mrec('synaptic_training', 'r140_synaptic_lattice', [('myelinated_axon', 8), ('neural_lattice', 1), ('photonic_neuron', 2)], [], [('synaptic_lattice', 1)], [],
     512, 600, 'synaptic_plasticity', None, [('w', 0.95, 1.0)], 'synapse_trainer')
mrec('psychophysics', 'r140_rhodopsin_retina', [('myelinated_axon', 2), ('eel_photophore', 2), ('fused_silica_lens', 1)], [], [('rhodopsin_retina', 2)], [],
     256, 400, 'psychophysics', None, [('H', 0.9, 1.0), ('F', 0.0, 0.05)], 'psychophysics_booth')
mrec('decipherment', 'r140_lexicon_fragment', [('glyph_tablet', 1), ('minecraft:paper', 4)], [], [('lexicon_fragment', 2)], [],
     128, 300, 'linguistics', {'Kt': 24}, [('dec', 0.7, 1.0)], 'decipherment_desk')
mrec('decipherment', 'r140_universal_grammar', [('lexicon_fragment', 8), ('one_time_pad', 1)], [], [('universal_grammar', 1)], [],
     256, 600, 'linguistics', {'Kt': 40}, [('dec', 0.75, 1.0)], 'decipherment_desk')
mrec('connectome_mapping', 'r140_engram', [('synaptic_lattice', 2), ('universal_grammar', 1), ('neural_organoid', 8)], [('synaptic_fluid', 1000)], [('engram', 4)], [],
     65536, 1200, 'integrated_information', None, [('Phi', 7.0, 20.0)], 'connectome_array')
mrec('load_testing', 'r140_rated_i_beam', [('minecraft:iron_ingot', 6)], [], [('rated_i_beam', 4)], [],
     128, 200, 'structural_mechanics', {'Pk': 500, 'Eg': 200}, [('SF', 2.0, 4.0)], 'load_test_rig')
mrec('composite_layup', 'r140_carbon_fibre_composite', [('minecraft:coal', 8), ('polymer_sheet', 2)], [], [('carbon_fibre_composite', 4)], [],
     256, 300, 'composite_materials', {'Ef': 230, 'rf': 1.8, 'Em': 3.5, 'rm': 1.2}, [('S', 80, 1000), ('q', 0.9, 1.0)], 'composite_layup_press')
mrec('composite_layup', 'r140_nanotube_composite', [('carbon_nanotube_bundle', 2), ('polymer_sheet', 1)], [], [('nanotube_composite', 2)], [],
     1024, 400, 'composite_materials', {'Ef': 1000, 'rf': 1.4, 'Em': 3.5, 'rm': 1.2}, [('S', 300, 2000), ('q', 0.9, 1.0)], 'composite_layup_press')
mrec('composite_layup', 'r140_tether_spool', [('nanotube_composite', 4), ('aramid_fibre', 2)], [], [('tether_spool', 1)], [],
     2048, 600, 'space_elevator', {'Ef': 1000, 'rf': 1.4, 'Em': 3.5, 'rm': 1.2}, [('S', 380, 2000), ('q', 0.95, 1.0)], 'composite_layup_press')
mrec('composite_layup', 'r140_shkadov_mirror_film', [('aluminium_ingot', 1), ('polymer_sheet', 4)], [], [('shkadov_mirror_film', 4)], [],
     512, 300, 'stellar_engines', {'Ef': 70, 'rf': 2.7, 'Em': 3.5, 'rm': 1.2}, [('S', 15, 1000), ('q', 0.95, 1.0)], 'composite_layup_press')
mrec('tribology', 'r140_wear_coating', [('graphene_sheet', 1), ('nickel_plated_iron', 1)], [('methane', 1000)], [('wear_coating', 1)], [],
     512, 400, 'tribology', {'Hg': 20, 'kw': 1}, [('tau', 100, 100000)], 'tribometer')
mrec('corrosion_testing', 'r140_sacrificial_anode', [('aluminium_ingot', 4), ('minecraft:iron_ingot', 1)], [('concentrated_brine', 500)], [('sacrificial_anode', 2)], [],
     128, 300, 'corrosion_science', None, [('cr', 0, 0.01), ('clv', -1, 0)], 'corrosion_cell')
mrec('corrosion_testing', 'r140_passivated_plate', [('minecraft:iron_ingot', 4), ('nickel_plated_iron', 1)], [('phosphate_buffer', 250)], [('passivated_plate', 4)], [],
     128, 300, 'corrosion_science', None, [('cr', 0, 0.01), ('imm', 0.62, 2)], 'corrosion_cell')
mrec('corrosion_testing', 'r140_passivated_plate_rust', [('minecraft:iron_ingot', 4), ('rust_flakes', 2)], [('phosphate_buffer', 250)], [('passivated_plate', 5)], [],
     128, 300, 'corrosion_science', None, [('cr', 0, 0.01), ('imm', 0.62, 2)], 'corrosion_cell')
mrec('elevator_lift', 'r140_crew_capsule', [('crew_capsule', 1)], [], [('fuelled_crew_capsule', 1)], [],
     65536, 600, 'space_elevator', {'Sp': 60}, [('tm', 1.0, 1.5)], 'space_elevator')
mrec('elevator_lift', 'r140_microgravity', [('microgravity_lab', 1)], [], [('microgravity_crystal', 12)], [],
     65536, 600, 'space_elevator', {'Sp': 60}, [('tm', 1.0, 1.5)], 'space_elevator')
mrec('elevator_lift', 'r140_station_module', [('station_module', 1)], [], [], [],
     65536, 600, 'space_elevator', {'Sp': 60}, [('tm', 1.0, 1.5)], 'space_elevator')
mrec('habitat_spin', 'r140_habitat_ecosystem', [('stable_ecosystem', 1), ('living_soil', 8)], [('minecraft:water', 8000)], [('stable_ecosystem', 6)], [],
     262144, 2400, 'rotating_habitats', {'rhoh': 1600, 'sy': 1500}, [('a', 9.3, 10.3)], 'oneill_cylinder')
mrec('habitat_spin', 'r140_habitat_rations', [('minecraft:wheat', 32), ('living_soil', 1)], [('minecraft:water', 4000)], [('field_ration', 24)], [],
     65536, 1200, 'rotating_habitats', {'rhoh': 1600, 'sy': 1500}, [('a', 9.3, 10.3)], 'oneill_cylinder')
mrec('stellar_thrust', 'r140_stellar_thrust', [('stellar_core', 1)], [('residual_starlight', 1000)], [], [('stellar_plasma', 4000)],
     16384, 72000, 'stellar_engines', None, [('F', 0.2, 2.0)], 'stellar_engine')
# the mind-upload route to the Superintelligence Core (newer, and far cheaper than the Matrioshka's old recipe)
mat = {'type': ts('matrioshka'), 'item_inputs': [{'item': ts('engram'), 'count': 16}, {'item': ts('computronium_ingot'), 'count': 32},
                                                  {'item': ts('neural_organoid'), 'count': 16}],
       'fluid_inputs': [{'fluid': ts('synaptic_fluid'), 'amount': 256000}], 'item_outputs': [{'id': ts('superintelligence_core'), 'count': 2}],
       'energy': 16777216, 'duration': 3600, 'research': ts('integrated_information'), 'physics': {'windows': [{'q': 'lgops', 'min': 49.3, 'max': 60.0}]}}
dump(os.path.join(D, 'recipe/matrioshka', 'r140_uploaded_minds.json'), mat)

# check every new console recipe's window can be reached
for mid, rid, r in RECIPES:
    m = json.load(open(os.path.join(D, 'ts_physics', mid + '.json'), encoding='utf-8'))
    r = dict(r)
    r['_id'] = rid
    p, got, rate = check_reachable(mid, m, r)
    print('  ok %-30s %-32s %s rate %.2f' % (mid, rid, {k: round(v, 3) for k, v in got.items()}, rate))


# ====================================================================== multiblocks
def write_mb(mid, layers, key, ctrl_char='C', extra=None):
    d = {'controller_block': ts(mid), 'controller': ctrl_char, 'layers': layers, 'key': {k: [ts(x) for x in v] for k, v in key.items()}, 'tiers': {}}
    if extra:
        d.update(extra)
    n = sum(row.count(ch) for layer in layers for row in layer for ch in key) + 1
    dump(os.path.join(A, 'multiblocks', mid + '.json'), d)
    return n


def grid(w, h, dpt):
    return [[[' '] * w for _ in range(dpt)] for _ in range(h)]


def rows(g):
    return [[''.join(r) for r in layer] for layer in g]


PORT = ['interface_port']
MB_COUNTS = {}

# --- Connectome Array: a brain-like dome of glial scaffold, axon tracts as meridians and an equator, relays where they
#     cross, viewing panes between, a turning ring of relays inside
R = 7
W = 2 * R + 1
g = grid(W, R + 2, W)
for y in range(R + 2):
    for z in range(W):
        for x in range(W):
            dx, dz = x - R, z - R
            if y == 0:
                if dx * dx + dz * dz <= R * R + 2:
                    edge = dx * dx + dz * dz > (R - 1) ** 2
                    g[y][z][x] = 'P' if edge and abs(dx) <= 1 and dz < 0 else 'F'
                continue
            rr = math.sqrt(dx * dx + (y - 0.5) ** 2 * 1.25 + dz * dz)
            if R - 1 < rr <= R + 0.2:
                mer = dx == 0 or dz == 0
                eq = y == 3
                g[y][z][x] = 'S' if mer and eq else ('A' if mer or eq else ('G' if (x + z + y) % 3 == 0 else 'F'))
# the turning ring inside
for a in range(24):
    t = a / 24 * math.tau
    x, z = int(round(R + math.cos(t) * 3)), int(round(R + math.sin(t) * 3))
    g[4][z][x] = 'R'
g[4][R][R] = 'S'
# controller: the front of the dome at y = 1
cz = min(z for z in range(W) if g[1][z][R] != ' ')
g[1][cz][R] = 'C'
MB_COUNTS['connectome_array'] = write_mb('connectome_array', rows(g),
                                         {'F': ['glial_scaffold'], 'P': ['glial_scaffold'] + PORT, 'A': ['axon_bundle'], 'S': ['synapse_relay'],
                                          'R': ['synapse_relay'], 'G': ['containment_field_glass']},
                                         extra={'effect_center': [R, 4.5, R], 'effect_scale': 3.0,
                                                'moving': [{'id': 'thought_ring', 'chars': 'R', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 3,
                                                            'motion': [{'type': 'spin', 'axis': 'up', 'deg_per_sec': 24}]}],
                                                'fx': [{'type': 'glow', 'chars': 'AS', 'color': '#B080E0', 'hot': '#FFE0FF'},
                                                       {'type': 'inflow', 'radius': 4.5, 'color': '#C090FF', 'waves': 3, 'period': 2.0}]})

# --- Space Elevator: an anchor platform with four buttresses, a 3 x 3 tether rising 48 blocks to a counterweight, and a
#     climber ring that rides up and down it
H = 52
Wb = 13
g = grid(Wb, H, Wb)
c = Wb // 2
for z in range(Wb):
    for x in range(Wb):
        dx, dz = x - c, z - c
        g[0][z][x] = 'P' if (abs(dx) == c or abs(dz) == c) and (abs(dx) <= 1 or abs(dz) <= 1) else 'F'
        if max(abs(dx), abs(dz)) <= 4:
            g[1][z][x] = 'F' if max(abs(dx), abs(dz)) == 4 else ' '
for y in range(1, 8):                                    # four buttresses leaning in to the tether
    k = 6 - min(y, 5)
    for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        g[y][c + sz * (k + 1)][c + sx * (k + 1)] = 'F'
for y in range(1, H - 3):
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            g[y][c + dz][c + dx] = 'T'
for y in range(H - 3, H):                               # the counterweight cap
    for dz in range(-2, 3):
        for dx in range(-2, 3):
            g[y][c + dz][c + dx] = 'F'
for y in (9, 10):                                         # the climber, a ring round the tether at rest
    for dz in range(-2, 3):
        for dx in range(-2, 3):
            if max(abs(dx), abs(dz)) == 2:
                g[y][c + dz][c + dx] = 'K'
g[1][0][c] = 'C'
g[1][1][c] = 'F'
MB_COUNTS['space_elevator'] = write_mb('space_elevator', rows(g),
                                       {'F': ['elevator_anchor'], 'P': ['elevator_anchor'] + PORT, 'T': ['tether_ribbon'], 'K': ['climber_car']},
                                       extra={'effect_center': [c, 24, c], 'effect_scale': 4.0,
                                              'moving': [{'id': 'climber', 'chars': 'K', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 2,
                                                          'motion': [{'type': 'oscillate', 'mode': 'move', 'axis': 'up', 'amplitude': 18, 'period_seconds': 24}]}],
                                              'fx': [{'type': 'glow', 'chars': 'TK', 'color': '#A0B0C0', 'hot': '#E0F0FF'}]})

# --- O'Neill Cylinder: a horizontal cylinder (radius 6, 23 long) of hull with three window strips, end caps on spin
#     bearings, raised on two cradles; the whole shell turns about its long axis
Rr = 6
Lc = 23
Wc = 2 * Rr + 5
Hc = 2 * Rr + 5
g = grid(Wc, Hc, Lc + 4)
cx, cy = Wc // 2, Rr + 3
for z in range(2, Lc + 2):
    for y in range(Hc):
        for x in range(Wc):
            dx, dy = x - cx, y - cy
            rr = math.sqrt(dx * dx + dy * dy)
            if z in (2, Lc + 1):
                if rr <= Rr + 0.3:
                    g[y][z][x] = 'B' if rr <= 1.5 else 'H'
            elif Rr - 0.7 < rr <= Rr + 0.3:
                ang = math.degrees(math.atan2(dy, dx)) % 120
                g[y][z][x] = 'W' if 50 <= ang <= 70 else 'H'
for z in (0, 1, Lc + 2, Lc + 3):                           # the cradles and the bearing hubs
    for y in range(cy + 1):
        g[y][z][cx] = 'B' if y >= cy - 1 else 'M'
    for x in range(cx - 3, cx + 4):
        g[0][z][x] = 'M'
for z in range(Lc + 4):                                     # a keel along the ground between the cradles
    g[0][z][cx] = g[0][z][cx] if g[0][z][cx] != ' ' else 'M'
g[1][0][cx] = 'C'
MB_COUNTS['oneill_cylinder'] = write_mb('oneill_cylinder', rows(g),
                                        {'H': ['habitat_hull'], 'W': ['habitat_window'], 'B': ['spin_bearing'], 'M': ['elevator_anchor'] + PORT},
                                        extra={'effect_center': [cx, cy, (Lc + 3) / 2], 'effect_scale': 6.0,
                                               'moving': [{'id': 'habitat', 'chars': 'HW', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 6,
                                                           'motion': [{'type': 'spin', 'axis': 'back', 'deg_per_sec': 6}]}],
                                               'fx': [{'type': 'glow', 'chars': 'W', 'color': '#A0D8C0', 'hot': '#F0FFE0'}]})

# --- Stellar Engine: a captive star at the centre, a deep spherical cap of Shkadov mirror on one side (the back), four
#     Caplan jets on the open side, held on a ring of Dyson segments
Rs = 12
Ws = 2 * Rs + 3
g = grid(Ws, Ws, Ws)
cs = Ws // 2
for y in range(Ws):
    for z in range(Ws):
        for x in range(Ws):
            dx, dy, dz = x - cs, y - cs, z - cs
            rr = math.sqrt(dx * dx + dy * dy + dz * dz)
            if Rs - 0.6 < rr <= Rs + 0.4 and dz >= 2:         # the mirror: the back half, past the star
                g[y][z][x] = 'S'
            elif abs(dz) <= 0 and Rs - 0.6 < math.sqrt(dx * dx + dy * dy) <= Rs + 0.4:
                g[y][z][x] = 'D'                                  # the support ring round the middle
for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):                 # Caplan jets on the open side
    for k in range(4):
        g[cs + dy * 6][cs - 5 - k][cs + dx * 6] = 'J'
g[cs][0][cs] = ' '
# a base platform under the ring, Interface Ports along its front, the controller standing on it
for z in range(cs - 3, cs + 4):
    for x in range(cs - 3, cs + 4):
        g[0][z][x] = 'P' if z == cs - 3 and abs(x - cs) <= 1 else 'D'
g[1][cs - 3][cs] = 'C'
MB_COUNTS['stellar_engine'] = write_mb('stellar_engine', rows(g),
                                       {'S': ['shkadov_mirror'], 'J': ['caplan_jet'], 'D': ['habitat_hull'],
                                        'P': ['habitat_hull'] + PORT},
                                       extra={'effect_center': [cs, cs, cs], 'effect_scale': 5.0,
                                              'moving': [{'id': 'mirror', 'chars': 'S', 'split': 'none', 'pivot': 'centre', 'ramp_seconds': 8,
                                                          'motion': [{'type': 'oscillate', 'mode': 'rotate', 'axis': 'up', 'amplitude': 4, 'period_seconds': 30}]}],
                                              'fx': [{'type': 'star', 'radius': 2.6, 'color': '#FFD070', 'at': [cs, cs, cs]},
                                                     {'type': 'glow', 'chars': 'J', 'color': '#FF9040', 'hot': '#FFF0C0'},
                                                     {'type': 'beam', 'start_back': cs - 8, 'end_back': -16, 'up': cs, 'width': 0.6, 'color': '#FFB060'}]})
print('multiblock blocks:', MB_COUNTS)


# ====================================================================== worldgen
WG = os.path.join(D, 'worldgen')
for f, chance, hm in (('neuron_arbor', None, 'MOTION_BLOCKING_NO_LEAVES'), ('library_ruin', 420, 'WORLD_SURFACE_WG'), ('derelict_ring', 18, None)):
    dump(os.path.join(WG, 'configured_feature', f + '.json'), {'type': ts(f), 'config': {}})
    pl = []
    if f == 'neuron_arbor':
        pl.append({'type': 'minecraft:count', 'count': 2})
    else:
        pl.append({'type': 'minecraft:rarity_filter', 'chance': chance})
    pl.append({'type': 'minecraft:in_square'})
    if hm:
        pl.append({'type': 'minecraft:heightmap', 'heightmap': hm})
    pl.append({'type': 'minecraft:biome'})
    dump(os.path.join(WG, 'placed_feature', f + '.json'), {'feature': ts(f), 'placement': pl})

# the Neural Cortex: a new region of Inner Space
tissue = json.load(open(os.path.join(WG, 'biome/tissue.json'), encoding='utf-8'))
cortex = json.loads(json.dumps(tissue))
cortex['effects']['sky_color'] = 0x8A70B0
cortex['effects']['fog_color'] = 0xC8B8E0
cortex['effects']['water_color'] = 0xD8C8F0
cortex['effects']['water_fog_color'] = 0x907CB0
cortex['effects']['particle'] = {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.0015}
cortex['features'] = [[] for _ in range(11)]
cortex['features'][4] = [ts('neuron_arbor')]
cortex['spawners'] = {'monster': [{'type': ts('dreaming_colony'), 'weight': 8, 'minCount': 1, 'maxCount': 1}],
                      'creature': [{'type': ts('red_cell'), 'weight': 2, 'minCount': 1, 'maxCount': 1}]}
dump(os.path.join(WG, 'biome/neural_cortex.json'), cortex)


def inner(d):
    bs = d['generator']['biome_source']['biomes']
    if any(b['biome'] == ts('neural_cortex') for b in bs):
        return
    for b in bs:
        if b['biome'] == ts('tissue') and b['parameters']['temperature'] == [0.15, 1.0]:
            b['parameters']['temperature'] = [0.15, 0.55]
            nb = json.loads(json.dumps(b))
            nb['biome'] = ts('neural_cortex')
            nb['parameters']['temperature'] = [0.55, 1.0]
            bs.append(nb)
            return
    raise SystemExit('tissue entry not found')


edit_json(os.path.join(D, 'dimension/inner_space.json'), inner)


def cortex_surface(d):
    seq = d['surface_rule']['sequence']
    if any('neural_cortex' in json.dumps(r) for r in seq):
        return
    grey = {'type': 'minecraft:block', 'result_state': {'Name': ts('grey_matter')}}
    white = {'type': 'minecraft:block', 'result_state': {'Name': ts('white_matter')}}
    rule = {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [ts('neural_cortex')]},
            'then_run': {'type': 'minecraft:sequence', 'sequence': [
                {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor', 'add_surface_depth': True,
                                                           'secondary_depth_range': 0}, 'then_run': grey},
                {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 6, 'surface_type': 'floor', 'add_surface_depth': True,
                                                           'secondary_depth_range': 0}, 'then_run': white}]}}
    seq.insert(1, rule)                                   # after the bone floor, before the other regions


edit_json(os.path.join(WG, 'noise_settings/inner_space.json'), cortex_surface)

dump(os.path.join(D, 'neoforge/biome_modifier/overworld_library_ruins.json'),
     {'type': 'neoforge:add_features', 'biomes': ['minecraft:forest', 'minecraft:birch_forest', 'minecraft:old_growth_birch_forest', 'minecraft:dark_forest',
                                                   'minecraft:taiga', 'minecraft:old_growth_pine_taiga', 'minecraft:old_growth_spruce_taiga', 'minecraft:jungle',
                                                   'minecraft:sparse_jungle', 'minecraft:snowy_taiga'],
      'features': ts('library_ruin'), 'step': 'surface_structures'})


def leo(d):
    feats = d['features']
    while len(feats) < 11:
        feats.append([])
    if ts('derelict_ring') not in feats[4]:
        feats[4].append(ts('derelict_ring'))


edit_json(os.path.join(WG, 'biome/low_earth_orbit.json'), leo)
# Rust Mites: Titan and the Antiverse
titan_biomes = [ts(b) for b in ('shangri_la_dunes', 'xanadu_uplands', 'selk_crater', 'kraken_mare', 'ontario_lacus_basin', 'sotra_cryovolcanic_field')]
anti = [ts('antiverse')]
dump(os.path.join(D, 'neoforge/biome_modifier/rust_mites.json'),
     {'type': 'neoforge:add_spawns', 'biomes': [b for b in titan_biomes + anti if os.path.exists(os.path.join(WG, 'biome', b.split(':')[1] + '.json'))],
      'spawners': [{'type': ts('rust_mite'), 'weight': 6, 'minCount': 1, 'maxCount': 3}]})

# loot: the caches and the creatures
dump(os.path.join(D, 'loot_table/chests/library_ruin.json'), {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 7}, 'entries': [
    {'type': 'minecraft:item', 'name': ts('glyph_tablet'), 'weight': 30, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}]},
    {'type': 'minecraft:item', 'name': ts('lexicon_fragment'), 'weight': 6},
    {'type': 'minecraft:item', 'name': ts('one_time_pad'), 'weight': 4},
    {'type': 'minecraft:item', 'name': ts('archive_shelf'), 'weight': 10, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}]},
    {'type': 'minecraft:item', 'name': ts('inscribed_glyph_stone'), 'weight': 8, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]},
    {'type': 'minecraft:item', 'name': ts('neural_organoid'), 'weight': 3}]}]})
dump(os.path.join(D, 'loot_table/chests/derelict_ring.json'), {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 7}, 'entries': [
    {'type': 'minecraft:item', 'name': ts('rated_i_beam'), 'weight': 20, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 6}}]},
    {'type': 'minecraft:item', 'name': ts('carbon_fibre_composite'), 'weight': 16, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}}]},
    {'type': 'minecraft:item', 'name': ts('passivated_plate'), 'weight': 14, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}}]},
    {'type': 'minecraft:item', 'name': ts('sacrificial_anode'), 'weight': 8},
    {'type': 'minecraft:item', 'name': ts('rust_flakes'), 'weight': 18, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 5}}]},
    {'type': 'minecraft:item', 'name': ts('field_ration'), 'weight': 10, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}]},
    {'type': 'minecraft:item', 'name': ts('wear_coating'), 'weight': 2}]}]})
dump(os.path.join(D, 'loot_table/entities/dreaming_colony.json'), {'type': 'minecraft:entity', 'pools': [{'rolls': 1, 'entries': [
    {'type': 'minecraft:item', 'name': ts('dream_polyp'), 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}},
                                                                         {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
                                                                          'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}]})
dump(os.path.join(D, 'loot_table/entities/rust_mite.json'), {'type': 'minecraft:entity', 'pools': [{'rolls': 1, 'entries': [
    {'type': 'minecraft:item', 'name': ts('rust_flakes'), 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}},
                                                                         {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
                                                                          'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}]})
# the dream polyp feeds the neural chain: a polyp is a quick neural organoid
mrec('neural_culture', 'r140_organoid_from_polyp', [('dream_polyp', 2)], [('blood_plasma', 250)], [('neural_organoid', 1)], [],
     256, 300, 'action_potential', {'vmin': 60}, [('v', 1, 150)], 'neural_culture_array')

# ====================================================================== tags
CASINGS = ['glial_scaffold', 'axon_bundle', 'synapse_relay', 'elevator_anchor', 'tether_ribbon', 'climber_car', 'habitat_hull', 'habitat_window',
           'spin_bearing', 'shkadov_mirror', 'caplan_jet']
CONSOLES = ['neural_culture_array', 'synapse_trainer', 'psychophysics_booth', 'decipherment_desk', 'load_test_rig', 'composite_layup_press', 'tribometer',
            'corrosion_cell']
CONTROLLERS = ['connectome_array', 'space_elevator', 'oneill_cylinder', 'stellar_engine']
STONE = ['inscribed_glyph_stone', 'glyph_stone_bricks', 'sacrificial_anode']
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts(x) for x in CASINGS + CONSOLES + CONTROLLERS + STONE])
tag_add(os.path.join(MC, 'tags/block/mineable/axe.json'), [ts('archive_shelf')])
tag_add(os.path.join(MC, 'tags/block/mineable/hoe.json'), [ts(x) for x in ('grey_matter', 'white_matter', 'neuron_soma')])
tag_add(os.path.join(MC, 'tags/block/needs_stone_tool.json'), [ts(x) for x in STONE])
tag_add(os.path.join(MC, 'tags/block/needs_iron_tool.json'), [ts(x) for x in CASINGS[:8] + CONSOLES + CONTROLLERS[:2]])
tag_add(os.path.join(MC, 'tags/block/needs_diamond_tool.json'), [ts(x) for x in CASINGS[8:] + CONTROLLERS[2:]])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'casings.json'), [ts(x) for x in CASINGS])
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts(x) for x in CONSOLES + CONTROLLERS])
    tag_add(os.path.join(D, 'tags', kind, 'multiblock_controllers.json'), [ts(x) for x in CONTROLLERS])
    tag_add(os.path.join(D, 'tags', kind, 'field/world.json'), [ts(x) for x in ('grey_matter', 'white_matter', 'neuron_soma')])
tag_add(os.path.join(D, 'tags/block/decorative_blocks.json'), [ts(x) for x in ('inscribed_glyph_stone', 'glyph_stone_bricks', 'archive_shelf')])
tag_add(os.path.join(D, 'tags/block/observable.json'), [ts(x) for x in ('grey_matter', 'white_matter', 'neuron_soma', 'inscribed_glyph_stone')])
tag_add(os.path.join(D, 'tags/item/hadal_vision.json'), [ts('low_light_visor')])


# ====================================================================== the Codex
def codex(entries, tab_nodes):
    def fe(E):
        for k, v in entries.items():
            E[k] = v
    edit_json(os.path.join(A, 'ts_codex/entries.json'), fe)
    for tab, nodes in tab_nodes.items():
        def ft(T, nodes=nodes):
            mine = {n['entry'] for n in nodes}
            T['nodes'] = [n for n in T['nodes'] if n['entry'] not in mine]
            taken = {(n['x'], n['y']) for n in T['nodes']}
            for n in nodes:
                n = dict(n)
                while (n['x'], n['y']) in taken:          # never on top of another node: step right
                    n['x'] += 2
                taken.add((n['x'], n['y']))
                T['nodes'].append(n)
        edit_json(os.path.join(A, 'ts_codex', tab + '.json'), ft)


def spot(items, text, title=None):
    p = {'type': 'spotlight', 'items': [ts(i) for i in items], 'text': text}
    if title:
        p['title'] = title
    return p


def text(t):
    return {'type': 'text', 'text': t}


def op_pages(mid, intro, sets, disturb, limits, rate, eqs):
    return [text(intro), text('$(thing)Set$(): %s.$(br2)$(thing)Disturbance$(): %s' % (sets, disturb)),
            text('$(thing)Limits$(): %s$(br2)$(thing)Rate$(): %s' % (limits, rate)), text('$(thing)Equations$()$(br)' + '$(br)'.join(eqs))]


E = {}
E['biology/action_potential'] = {'title': 'The Action Potential', 'icon': ts('myelinated_axon'), 'category': 'biology', 'research': 'action_potential', 'pages': [
    spot(['neural_culture_array'], "The spike runs on: λ = √(r_m/r_i). The $(thing)Neural Culture Array$() grows neurons in a dish and coaxes glial cells to wrap their axons in myelin, making $(item)Myelinated Axons$(). A culture catches what any culture catches: keep it clean.", 'Neural Culture Array'),
    spot(['myelinated_axon', 'dream_polyp'], "The lobes a $(item)Dream Polyp$() drops from a Dreaming Colony grow back into a Neural Organoid in the same array. JEI shows the recipes.")]}
E['biology/synaptic_plasticity'] = {'title': 'Synaptic Plasticity', 'icon': ts('synaptic_lattice'), 'category': 'biology', 'research': 'synaptic_plasticity', 'pages': [
    spot(['synapse_trainer', 'synaptic_lattice'], "Δw = η·x·y. The $(thing)Synapse Trainer$() shows a lattice of synapses a pattern until it has learnt it. The $(item)Synaptic Lattice$() is an upgrade: fitted to a machine, +10% products, and precision falls off a third slower. JEI shows the recipe.", 'Synapse Trainer')]}
E['biology/psychophysics'] = {'title': 'Psychophysics', 'icon': ts('low_light_visor'), 'category': 'biology', 'research': 'psychophysics', 'pages': [
    spot(['psychophysics_booth', 'rhodopsin_retina'], "S = k·ln(I/I₀). The $(thing)Psychophysics Booth$() trains a cultured retina to see faint flashes without seeing things that are not there: a $(item)Rhodopsin Retina$().", 'Psychophysics Booth'),
    spot(['low_light_visor'], "The $(item)Low-Light Visor$() (made by crafting) gives night vision while worn on the head; in the $(thing)Hadal Trench$() it sees the deep clearly, far into the water.")]}
E['biology/linguistics'] = {'title': 'Linguistics', 'icon': ts('glyph_tablet'), 'category': 'biology', 'research': 'linguistics', 'pages': [
    spot(['decipherment_desk', 'glyph_tablet'], "H = log₂N bits a symbol. $(item)Glyph Tablets$() are found in the caches of $(thing)Library Ruins$(), in the Overworld's forests, taigas and jungles. The $(thing)Decipherment Desk$() counts their symbols until they can be read.", 'Decipherment Desk'),
    spot(['lexicon_fragment', 'universal_grammar'], "A tablet read gives $(item)Lexicon Fragments$(); enough of them give the $(item)Universal Grammar$(), made at the Decipherment Desk. Guess the alphabet right and count enough text for the context you read in.")]}
E['biology/integrated_information'] = {'title': 'Integrated Information', 'icon': ts('engram'), 'category': 'biology', 'research': 'integrated_information', 'pages': [
    spot(['connectome_array', 'engram'], "Φ: integrated and differentiated. The $(thing)Connectome Array$() is a dome of %d blocks: glial scaffold threaded with axon tracts, relays where they cross, and a ring of relays turning inside. It maps a network into $(item)Engrams$()." % MB_COUNTS['connectome_array'], 'Connectome Array'),
    spot(['superintelligence_core'], "Sixteen Engrams carry enough of a mind for the $(thing)Matrioshka Brain$() to run as a $(item)Superintelligence Core$(), far more cheaply than growing one. JEI shows the recipe."),
    {'type': 'multiblock', 'name': 'Connectome Array', 'text': 'Rotate, zoom and view layers in $(thing)JEI$(). Interface Ports go in the front of the floor.', 'multiblock': 'connectome_array'}]}
E['engineering/structural_mechanics'] = {'title': 'Structural Mechanics', 'icon': ts('rated_i_beam'), 'category': 'physics', 'research': 'structural_mechanics', 'pages': [
    spot(['load_test_rig', 'rated_i_beam'], "P = π²EI/L². The $(thing)Load Test Rig$() loads a beam until it bows and rates it: a $(item)Rated I-Beam$() has a safety factor of two to four, no more, no less.", 'Load Test Rig')]}
E['engineering/composite_materials'] = {'title': 'Composite Materials', 'icon': ts('carbon_fibre_composite'), 'category': 'physics', 'research': 'composite_materials', 'pages': [
    spot(['composite_layup_press', 'carbon_fibre_composite', 'nanotube_composite'], "E = V_f·E_f + V_m·E_m. The $(thing)Composite Layup Press$() makes $(item)Carbon-Fibre$() and $(item)Nanotube Composites$(), and later the $(item)Tether Spool$() and $(item)Shkadov Mirror Film$(). Lay the plies along the load and do not pack the fibre past 62%.", 'Composite Layup Press')]}
E['engineering/tribology'] = {'title': 'Tribology', 'icon': ts('wear_coating'), 'category': 'physics', 'research': 'tribology', 'pages': [
    spot(['tribometer', 'wear_coating'], "V = k·W·L/H. The $(thing)Tribometer$() qualifies a diamond-like carbon $(item)Wear Coating$(): an upgrade that makes a machine's catalysts wear half as fast.", 'Tribometer')]}
E['engineering/corrosion_science'] = {'title': 'Corrosion Science', 'icon': ts('sacrificial_anode'), 'category': 'physics', 'research': 'corrosion_science', 'pages': [
    spot(['corrosion_cell', 'passivated_plate', 'sacrificial_anode'], "E = E⁰ + 0.0296·log c. The $(thing)Corrosion Cell$() holds a sample on Pourbaix's map: in the passive window it makes $(item)Passivated Plate$(); below the immunity line, in salt water, it qualifies $(item)Sacrificial Anodes$().", 'Corrosion Cell'),
    spot(['rust_flakes', 'rust_mite_spawn_egg'], "$(thing)Rust Mites$() eat the charge of machines. A live anode within twelve blocks draws them off: they eat it instead, four bites, and leave $(item)Rust Flakes$(). Flakes make passivated plate more cheaply.")]}
E['engineering/space_elevator'] = {'title': 'The Space Elevator', 'icon': ts('tether_ribbon'), 'category': 'physics', 'research': 'space_elevator', 'pages': [
    spot(['space_elevator', 'tether_ribbon', 'climber_car'], "T = e^(ρgR²/σr). The $(thing)Space Elevator$() is %d blocks: an anchor platform with four buttresses, a tether forty-eight blocks tall under a counterweight, and a climber that rides it. It takes Crew Capsules, Microgravity Labs and Station Modules to orbit on power alone." % MB_COUNTS['space_elevator'], 'Space Elevator'),
    {'type': 'multiblock', 'name': 'Space Elevator', 'text': 'Interface Ports in the middle of each side of the platform. A fuelled Crew Capsule can be flown from beside it, as from a Rocket Assembly.', 'multiblock': 'space_elevator'}]}
E['engineering/rotating_habitats'] = {'title': 'Rotating Habitats', 'icon': ts('habitat_window'), 'category': 'physics', 'research': 'rotating_habitats', 'pages': [
    spot(['oneill_cylinder', 'habitat_hull', 'habitat_window', 'spin_bearing'], "a = ω²r. The $(thing)O'Neill Cylinder$() is %d blocks: a hull with three window strips, turning on its bearings over two cradles. Seed it with a Stable Ecosystem and it grows six; it also feeds its colonists, in bulk." % MB_COUNTS['oneill_cylinder'], "O'Neill Cylinder"),
    {'type': 'multiblock', 'name': "O'Neill Cylinder", 'text': 'Interface Ports in the cradles and keel.', 'multiblock': 'oneill_cylinder'}]}
E['engineering/stellar_engines'] = {'title': 'Stellar Engines', 'icon': ts('shkadov_mirror'), 'category': 'physics', 'research': 'stellar_engines', 'pages': [
    spot(['stellar_engine', 'shkadov_mirror', 'caplan_jet'], "F = f·L/c. The $(thing)Stellar Engine$() is %d blocks: a mirror held up by a captive star's light on one side of it, Caplan jets on the other. It runs on Stellar Cores and gives four times a Dyson Array's power for each, with stellar plasma skimmed off by the jets." % MB_COUNTS['stellar_engine'], 'Stellar Engine'),
    {'type': 'multiblock', 'name': 'Stellar Engine', 'text': 'Interface Ports along the front of the platform under the ring.', 'multiblock': 'stellar_engine'}]}
E['world/node_dreaming_colony'] = {'title': 'Dreaming Colony', 'icon': ts('dreaming_colony_spawn_egg'), 'category': 'world', 'node': True, 'pages': [
    spot(['dreaming_colony_spawn_egg'], '$(thing)Dreaming Colony$(): ' + LANG['entity.%s.dreaming_colony.lore' % NS] + '$(br2)$(thing)Drops$(): Dream Polyp.')]}
E['world/node_rust_mite'] = {'title': 'Rust Mite', 'icon': ts('rust_mite_spawn_egg'), 'category': 'world', 'node': True, 'pages': [
    spot(['rust_mite_spawn_egg'], '$(thing)Rust Mite$(): ' + LANG['entity.%s.rust_mite.lore' % NS] + '$(br2)$(thing)Drops$(): Rust Flakes.')]}
E['world/library_ruin'] = {'title': 'Library Ruins and Derelict Rings', 'icon': ts('inscribed_glyph_stone'), 'category': 'world', 'pages': [
    spot(['inscribed_glyph_stone', 'archive_shelf', 'glyph_tablet'], "$(thing)Library Ruins$(): sunken halls of glyph stone in the Overworld's forests, taigas and jungles, their shelves still full of tablets in the old glyph alphabet, with a cache of what the readers left. Rare."),
    spot(['habitat_hull', 'rust_flakes'], "$(thing)Derelict Rings$(): arcs of old spinning habitats drifting in low Earth orbit, far from the station, overrun by Rust Mites. Their crews' caches are still aboard.")]}
E['world/neural_cortex'] = {'title': 'The Neural Cortex', 'icon': ts('neuron_soma'), 'category': 'world', 'research': 'inner_space', 'pages': [
    spot(['grey_matter', 'white_matter', 'neuron_soma'], "A sixth region of $(thing)Inner Space$(): a floor of $(item)Grey Matter$() over $(item)White Matter$(), where neurons grow as tall as trees, their cell bodies and synapses glowing. $(thing)Dreaming Colonies$() drift there.")]}
for mid, intro, sets, dist, lim, rate, eqs in (
        ('neural_culture_array', "A spike runs down an axon faster the thicker it is, and a hundred times faster once myelin wraps it. Too few sodium channels and the cells never fire; too many and they die of it.",
         'Sodium channels, Axon diameter, Myelin wraps', 'the culture drifts: speed varies by ±2%·d.', 'under 40 mS/cm² the cells cannot fire (stops); over 160 they die (wear); more myelin than the axon can hold wears it.',
         'more channels, faster.', ['Myelination: m = min(1, wraps/40)', 'Speed: v = (6D·m + √D·(1 − m))·√(g/120)', 'Length: λ = 0.5·√(D·(1 + wraps))']),
        ('synapse_trainer', "Each showing of the pattern strengthens the synapses it uses. Learn too fast and the weights run away; train too long and the lattice wears.",
         'Learning rate, Epochs, Activity', 'noise: what is learnt varies by ±1%·d.', 'η·x² over 0.25 runs away (stops); over 400 epochs wears.', 'more epochs, slower.',
         ['Step: Δw = η·x²', 'Learnt: w = 1 − e^(−η·x²·N)']),
        ('psychophysics_booth', "Brighter flashes stand further out of the noise. Set the criterion high to keep false alarms down, but high enough and you miss the flashes too.",
         'Flash intensity, Criterion, Trials', 'adaptation: sensitivity varies by ±2%·d.', 'too bright a flash bleaches the retina (wear).', 'more trials, slower.',
         ["Sensitivity: d′ = 0.5·ln(I/I₀)", "Hits: H = Φ(d′ − c)", 'False alarms: F = Φ(−c)']),
        ('decipherment_desk', "Guess the alphabet's size from the symbol counts, then read in context. Longer contexts read more but need far more text.",
         'Corpus, Alphabet guess, Context', 'scribal errors: the reading varies by ±1%·d.', 'under 30 symbols of text there is nothing to count (stops).', 'more text, slower.',
         ['Fit: e^(−((k − K)/0.15K)²)', 'Coverage: 1 − e^(−N/(20·k^g))', 'Deciphered: fit × coverage × g/(g + 0.5)']),
        ('connectome_array', "A mind is integrated and differentiated at once: connected enough that every part affects every other, split enough into modules that they do different things.",
         'Neurons (log), Connectivity, Modularity', 'noise: Φ varies by ±1%·d.', 'over 20 MW wears; a dense unmodular net seizes (damage).', 'more Φ, faster.',
         ['Integration: f = (1 − e^(−50p))·e^(−4p)', 'Φ = log n · 4Q(1 − Q) · f']),
        ('load_test_rig', "Deeper sections are stiffer by the cube of their depth; longer columns buckle at the inverse square of their length. Rate a beam between two and four: strong enough, and no heavier.",
         'Section depth, Flange thickness, Length', 'imperfection: the load varies by ±2%·d.', 'over 250 MPa the steel yields (stops); over 400 kg wears the rig.', 'lighter beams faster.',
         ['I = (b·h³ − (b − t/2)(h − 2t)³)/12', 'P = π²EI/L²', 'Safety factor = P/load']),
        ('composite_layup_press', "Along the fibres a composite is stiff; across them it is only resin. Fibres packed past 62% leave voids.",
         'Fibre fraction, Ply angle, Cure temperature', 'ply wrinkles: consolidation varies by ±1%·d.', 'under 110 °C the resin does not cure (stops); over 190 °C it chars (damage).', 'hotter cures faster.',
         ['E = V_f·E_f + (1 − V_f)·E_m', 'E(θ) = E·cos⁴θ + E_m·(1 − cos⁴θ)', 'Specific stiffness = E·q/ρ']),
        ('tribometer', "A coating lasts as long as its thickness over its wear rate. Test it at the duty it must stand, but not so hard the contact overheats.",
         'Load, Sliding speed, Coating thickness', 'debris: life varies by ±2%·d.', 'under 200 W the test is too gentle (stops); over 400 W of friction wears; past 8 µm the coating spalls.', 'harder tests faster.',
         ['Life: τ = 5556·h·H/(20·k·W·v) hours']),
        ('corrosion_cell', "Below −0.62 V iron cannot dissolve; between pH 9 and 13 it covers itself in oxide and stops; chloride breaks that film above the pitting potential.",
         'pH, Potential, Chloride', 'oxygen: the rate varies by ±2%·d.', 'below −1.2 V hydrogen embrittles the steel (wear).', 'salty water slower.',
         ['Immunity: E < −0.62 V', 'Pitting: E_p = 0.3 − 0.15·(log Cl + 4)', 'Rate: e^((E + 0.62)/0.06) where not immune or passive']),
        ('space_elevator', "The tether must taper by at least e^(48.4/specific strength), or it snaps; much more is wasted mass. Climbers ride on beamed power.",
         'Taper ratio, Climber speed, Payload', 'wind and tides: the load varies by ±2%·d.', 'a taper below what is needed snaps the tether (stops); over 50 MW of beam overheats the climber (damage).', 'faster climbers, faster lifts.',
         ['Taper needed: e^(48.4/S)', 'Margin: taper/needed', 'Power: P = m·g·v']),
        ('oneill_cylinder', "Spin it for one gee on its floor, slowly enough not to sicken its colonists, without tearing its hull.",
         'Spin, Radius, Hull thickness', 'wobble: gravity varies by ±1%·d.', 'over 2 rpm the colonists are sick (wear); hoop stress over the hull\'s strength tears it (stops).', 'bigger habitats grow more.',
         ['a = ω²r', 'Hoop stress = ρω²r²·(1 + 200/t)']),
        ('stellar_engine', "Cover more of the star's sky with mirror for more thrust, and fire the jets for more again; but a mirror too heavy falls in, and too close it melts.",
         'Mirror coverage, Mirror areal density, Jet fraction', 'flares: thrust varies by ±2%·d.', 'heavier than 0.78 g/m² the mirror falls (stops); over 1500 K it melts (damage); jets over 90% strip the star (wear).', 'more thrust, more power.',
         ['F = f·(1 + j)·L/c', 'T = 394/√(1 − f) K'])):
    E['operation/' + mid] = {'title': MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation',
                             'research': {'neural_culture_array': 'action_potential', 'synapse_trainer': 'synaptic_plasticity', 'psychophysics_booth': 'psychophysics',
                                          'decipherment_desk': 'linguistics', 'connectome_array': 'integrated_information', 'load_test_rig': 'structural_mechanics',
                                          'composite_layup_press': 'composite_materials', 'tribometer': 'tribology', 'corrosion_cell': 'corrosion_science',
                                          'space_elevator': 'space_elevator', 'oneill_cylinder': 'rotating_habitats', 'stellar_engine': 'stellar_engines'}[mid],
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {
    'biology': [{'entry': 'biology/action_potential', 'x': 3, 'y': 48, 'parents': []}, {'entry': 'biology/synaptic_plasticity', 'x': 5, 'y': 48, 'parents': ['biology/action_potential']},
                {'entry': 'biology/psychophysics', 'x': 5, 'y': 50, 'parents': ['biology/action_potential']}, {'entry': 'biology/linguistics', 'x': 7, 'y': 50, 'parents': ['biology/psychophysics']},
                {'entry': 'biology/integrated_information', 'x': 9, 'y': 49, 'parents': ['biology/synaptic_plasticity', 'biology/linguistics']}],
    'physics': [{'entry': 'engineering/structural_mechanics', 'x': 3, 'y': 133, 'parents': []},
                {'entry': 'engineering/composite_materials', 'x': 5, 'y': 133, 'parents': ['engineering/structural_mechanics']},
                {'entry': 'engineering/tribology', 'x': 5, 'y': 135, 'parents': ['engineering/structural_mechanics']},
                {'entry': 'engineering/corrosion_science', 'x': 7, 'y': 135, 'parents': ['engineering/tribology']},
                {'entry': 'engineering/space_elevator', 'x': 7, 'y': 133, 'parents': ['engineering/composite_materials']},
                {'entry': 'engineering/rotating_habitats', 'x': 9, 'y': 133, 'parents': ['engineering/space_elevator']},
                {'entry': 'engineering/stellar_engines', 'x': 11, 'y': 133, 'parents': ['engineering/rotating_habitats']}],
    'machines': [{'entry': 'operation/' + m, 'x': 28 + 2 * (i % 6), 'y': 6 + 2 * (i // 6), 'parents': []} for i, m in enumerate(
        ['neural_culture_array', 'synapse_trainer', 'psychophysics_booth', 'decipherment_desk', 'connectome_array', 'load_test_rig',
         'composite_layup_press', 'tribometer', 'corrosion_cell', 'space_elevator', 'oneill_cylinder', 'stellar_engine'])],
    'mobs': [{'entry': 'world/node_dreaming_colony', 'x': 67, 'y': 13, 'parents': [], 'node': True},
             {'entry': 'world/node_rust_mite', 'x': 69, 'y': 13, 'parents': [], 'node': True}],
    'dimensions': [{'entry': 'world/library_ruin', 'x': 127, 'y': 11, 'parents': []},
                   {'entry': 'world/neural_cortex', 'x': 85, 'y': 17, 'parents': ['world/r93_inner_space']}],
}
codex(E, NODES)


def regions(tab, label, y0, y1):
    def f(T):
        if any(r['label'] == label for r in T['regions']):
            return
        tier = max(r['tier'] for r in T['regions']) + 1
        T['regions'].append({'tier': tier, 'label': label, 'x0': -1.5, 'x1': 125.5, 'y0': y0, 'y1': y1})
    edit_json(os.path.join(A, 'ts_codex', tab + '.json'), f)


regions('biology', 'Mind & Language', 46.5, 52.5)
regions('physics', 'Engineering of the Large', 131.5, 137.5)


# ====================================================================== English
def lang(L):
    L.update(LANG)


edit_json(os.path.join(A, 'lang/en_us.json'), lang)
print('research', len(RESEARCH), 'machines', len(MACHINES), 'recipes', len(RECIPES) + 1, 'lang keys', len(LANG))
