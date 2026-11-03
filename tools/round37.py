"""
Round 37 (1.1, "Foundations"): the first tier of five new sciences - see the 1.1 expansion design.

* ASTROPHYSICS: Observational Astronomy, Spectroscopy (the Spectrograph).
* GEOGRAPHY: Cartography, Geodesy, Geomorphology (the Geodetic Surveyor).
* NUCLEAR SCIENCE: Radioactivity, Nuclear Binding Energy (pitchblende, radium, the Geiger Counter, Radiation Sickness).
* ELECTROMAGNETISM: Magnetostatics, Electromagnetic Induction, Ferromagnetism (magnetite, ferrite, the Gauss Meter).
* PROBABILITY: Probability Theory, Combinatorics (dice, quantum random numbers).

Plus the new effects (the Java side registers them), the item and block tags the sciences read, and the
NuclearRadiation bindings that let NuclearCraft packs see this mod's radioactive items.
The research JSON, puzzles (tools/puzzles2/group_r37.json), recipes and Codex chapters are all generated here.
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
    # ---- astrophysics
    _I('telescope_mirror_blank', 'Telescope Mirror Blank', 'lens', '#b8c4d8', '#ffffff',
       'A disc of glass ground to a paraboloid within a fraction of a wavelength, then silvered. Light from a whole star lands on one point.'),
    _I('spectral_plate', 'Spectral Plate', 'plate', '#302050', '#ff80d0',
       'Starlight spread into a rainbow and caught on glass. The dark lines in it say which elements the light passed through.'),
    # ---- geography
    _I('survey_map', 'Survey Map', 'codex', '#c8b080', '#40a060',
       'Heights, rivers and benchmarks, measured rather than guessed. Every line on it was walked.'),
    _I('theodolite_lens', 'Theodolite Lens', 'lens', '#a0b0a0', '#e0ffe0',
       'A sighting lens on a graduated circle: two angles and one measured baseline, and every distance follows.'),
    _I('geoid_model', 'Geoid Model', 'orb', '#2a8040', '#90d0ff',
       'The true shape of the planet: not a sphere, not quite an ellipsoid, but the surface the sea would take if it could flow everywhere.'),
    _I('sediment_core', 'Sediment Core', 'probe', '#806040', '#d0b080',
       'A tube of layered ground. Each band is a year, a flood or an ash fall, stacked in the order it happened.'),
    # ---- nuclear science
    _I('raw_pitchblende', 'Raw Pitchblende', 'chunk', '#2a2a24', '#b8c840',
       'Uraninite: heavy, black and pitch-glossy. Marie and Pierre Curie found two new elements in it by following its radioactivity.'),
    _I('radium_salt', 'Radium Salt', 'crystal', '#d8f0c8', '#80ffa0',
       'Ten milligrams of radium chloride, glowing faintly in the dark by its own decay. Handle it as little as you can.'),
    _I('geiger_tube', 'Geiger Tube', 'capacitor', '#606870', '#ffe080',
       'A thin wire in a tube of low-pressure gas at high voltage. One ionising particle and the whole tube avalanches into a click.'),
    _I('prussian_blue_tablet', 'Prussian Blue Tablet', 'pellet', '#103080', '#4060d0',
       'Iron hexacyanoferrate. Swallowed, it traps radioactive caesium and thallium in the gut so the body passes them instead of keeping them.',
       special='prussianblue', stack=16),
    # ---- electromagnetism
    _I('magnetite', 'Magnetite', 'crystal', '#202024', '#707084',
       'Fe₃O₄, the most magnetic mineral there is. Struck by lightning it becomes lodestone; ground up it becomes ferrite.'),
    _I('ferrite_magnet', 'Ferrite Magnet', 'plate', '#303034', '#d04040',
       'Iron oxide pressed with clay, fired, then magnetised: cheap, brittle and permanently magnetic.'),
    _I('electrical_steel_lamination', 'Electrical Steel Lamination', 'plate', '#707880', '#c8d0d8',
       'Thin sheets of silicon steel, insulated from each other, so a changing field cannot drive eddy currents through the core.'),
    # ---- probability
    _I('dice_set', 'Dice Set', 'dots', '#f0ece0', '#202020',
       'Two fair dice: thirty-six outcomes, eleven totals, and seven the most likely of them.', special='dice', stack=1),
    _I('random_number_chip', 'Quantum Random Number Chip', 'chip', '#103020', '#40ff80',
       'Single photons meet a half-silvered mirror: transmitted is 1, reflected is 0. Nothing in the universe knows the next bit in advance.'),
    # ---- the instruments
    _I('geodetic_surveyor', 'Geodetic Surveyor', 'analyzer', '#406040', '#a0ffb0',
       'Triangulates the ground around you and lists the ores within 24 blocks, with the nearest of each.', special='surveyor', stack=1),
    _I('gauss_meter', 'Gauss Meter', 'analyzer', '#403040', '#ff7070',
       'A Hall-effect probe: reads the magnetic field where you stand, from the planet\'s own to a magnet in your hand.', special='gaussmeter', stack=1),
    _I('geiger_counter', 'Geiger Counter', 'analyzer', '#404020', '#e0ff60',
       'Held in either hand, it clicks for every decay it hears - from what you carry and from the ground around you.', special='geiger', stack=1),
]
BLOCKS = [
    dict(id='pitchblende_ore', name='Pitchblende Ore', art='ore', base='stone', c1='#202018', c2='#b8c840', kind='ore', drops='raw_pitchblende',
         light=0, hardness=3.5, tool='iron'),
    dict(id='deepslate_pitchblende_ore', name='Deepslate Pitchblende Ore', art='ore', base='deepslate', c1='#202018', c2='#c8d850', kind='ore',
         drops='raw_pitchblende', light=0, hardness=5.0, tool='iron'),
]
FLUIDS = []

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'spectrograph': ('Spectrograph',
                     'A slit, a grating and a plate. Light comes in as one colour and leaves as a rainbow crossed with dark lines, and every '
                     'line is an element\'s fingerprint.'),
}
MACHINE_CAT = {'spectrograph': 'astrophysics'}
MACHINE_RESEARCH = {'spectrograph': 'spectroscopy'}
TIER = {'spectrograph': 1}
MC = {'spectrograph': (['ATA', 'QSQ', 'CNC'], {'A': 'minecraft:amethyst_shard', 'T': 'telescope_mirror_blank', 'Q': 'cherenkov_quartz',
                                               'S': 'spacetime_casing', 'C': 'minecraft:copper_ingot', 'N': 'neutronium_ingot'})}
EXTRA = {'spectrograph': 'Spreads light into $(item)Spectral Plates$(), and identifies the radium hidden in $(item)Raw Pitchblende$() by its lines.'}

CATS = [
    ('astrophysics', 'Astrophysics', 'Telescopes, spectra, and the sky as a resource.', 'telescope_mirror_blank', 18),
    ('geography', 'Earth & Mapping', 'Surveying, the shape of the planet and what the ground is made of.', 'geodetic_surveyor', 19),
    ('nuclear', 'Nuclear Science', 'Radioactivity, isotopes and the energy that holds nuclei together.', 'geiger_counter', 20),
    ('electromagnetism', 'Electromagnetism', 'Magnets, induction and the fields between them.', 'gauss_meter', 21),
    ('probability', 'Probability', 'Chance, counting and the laws that randomness still obeys.', 'dice_set', 22),
]

# =================================================================================== RESEARCH
# (id, title, description, icon, x (tier column), y, insight, requires, cost)
RESEARCH = [
    ('observational_astronomy', 'Observational Astronomy',
     'Everything known about the sky came through a lens. A bigger mirror gathers more light and splits finer detail - up to the limit that '
     'the wave nature of light itself sets. Unlocks the Telescope Mirror Blank.',
     'telescope_mirror_blank', 1, -7, 8, ['quantum_foundations'], [('cherenkov_quartz', 4)]),
    ('spectroscopy', 'Spectroscopy',
     'Split light by colour and its spectrum tells you what made it: how hot, and which elements, by the dark and bright lines each one '
     'leaves. Unlocks the Spectrograph and Spectral Plates.',
     'spectrograph', 3, -7, 20, ['observational_astronomy', 'chemical_bonding'], [('telescope_mirror_blank', 2)]),
    ('cartography', 'Cartography',
     'A map is a projection: a round planet drawn on a flat sheet, and every projection trades one truth for another. The first study of '
     'the Earth sciences. Unlocks the Survey Map.',
     'survey_map', 0, 15, 5, [], []),
    ('geodesy', 'Geodesy',
     'Measure one baseline and two angles and every other distance follows - that is how countries were mapped, triangle by triangle. '
     'Geodesy measures the planet itself. Unlocks the Theodolite Lens, the Geoid Model and the Geodetic Surveyor.',
     'theodolite_lens', 1, 15, 10, ['cartography', 'number_systems'], [('survey_map', 2)]),
    ('geomorphology', 'Geomorphology',
     'Landforms are a record: rivers carry grains, the grains settle, and the speed at which they settle sorts sand from silt from clay. '
     'Unlocks the Sediment Core.',
     'sediment_core', 1, 16, 10, ['cartography'], [('survey_map', 1)]),
    ('radioactivity', 'Radioactivity',
     'Some nuclei are unstable and fall apart on their own, at random, but at a rate fixed for each kind. Half of any sample is gone after '
     'its half-life, whatever you do to it. Unlocks Radium Salt, the Geiger Tube and Counter, and the Prussian Blue Tablet.',
     'radium_salt', 1, 12, 10, ['quantum_foundations'], [('raw_pitchblende', 4)]),
    ('nuclear_binding_energy', 'Nuclear Binding Energy',
     'A nucleus weighs less than the protons and neutrons in it. The missing mass is the energy that binds them - millions of times more '
     'per atom than any chemical bond. The way into fission and fusion.',
     'geiger_counter', 3, 13, 25, ['radioactivity', 'chemical_bonding'], [('radium_salt', 1)]),
    ('magnetostatics', 'Magnetostatics',
     'A steady current makes a magnetic field that circles the wire and weakens with distance from it. The first study of '
     'electromagnetism. Unlocks the Gauss Meter.',
     'gauss_meter', 1, -5, 8, ['quantum_foundations'], [('cherenkov_quartz', 4)]),
    ('electromagnetic_induction', 'Electromagnetic Induction',
     'A changing magnetic field drives a current. Spin a coil in a field and you have a generator; laminate the iron core and you stop it '
     'heating itself. Unlocks Magnetite and Electrical Steel Laminations.',
     'electrical_steel_lamination', 2, -6, 12, ['magnetostatics'], [('cherenkov_quartz', 8)]),
    ('ferromagnetism', 'Ferromagnetism',
     'In iron, the spins of neighbouring atoms line up of their own accord - until heat shakes them apart above a critical temperature. '
     'Unlocks the Ferrite Magnet.',
     'ferrite_magnet', 3, -5, 20, ['magnetostatics', 'chemical_bonding'], [('magnetite', 8)]),
    ('probability_theory', 'Probability Theory',
     'Chance has rules. Count the ways an outcome can happen, divide by all the ways anything can, and the result is exact even when '
     'every single trial is not. Unlocks the Dice Set.',
     'dice_set', 2, 10, 10, ['number_systems'], [('cherenkov_quartz', 4)]),
    ('combinatorics', 'Combinatorics',
     'Counting without listing: how many orders, how many choices, how fast the numbers grow - so fast that their logarithm is the only '
     'way to hold them. Unlocks the Quantum Random Number Chip.',
     'random_number_chip', 3, 10, 18, ['probability_theory'], [('dice_set', 1)]),
]

# =================================================================================== PUZZLES (tools/puzzles2/group_r37.json)
PUZZLES = {
    'observational_astronomy': dict(
        goal='Light from a distant star enters a round mirror of diameter D. Because light is a wave, the mirror cannot focus it to a point: it makes a '
             'small disc with rings, and two stars closer together than that disc cannot be told apart. Write the smallest angle θ the mirror resolves '
             'for light of wavelength λ.',
        name='Diffraction limit of a circular aperture', lhs='θ',
        form='1.22 * λ / D',
        expr='1.22 * λ / D * 0.206265',
        ask='Find θ in arcseconds for green light, λ in micrometres and D in metres.',
        vars={'θ': 'smallest resolvable angle', 'λ': 'wavelength (µm)', 'D': 'mirror diameter (m)'},
        given={'λ': 0.55, 'D': 2.4}, unit='arcsec',
        method='The first dark ring of the diffraction pattern of a circular opening sits where the first zero of the Bessel function J₁ falls; 1 radian is '
               '206,265 arcseconds.',
        distractors=['c', 'ν', '2']),
    'spectroscopy': dict(
        goal='A hot, dense body glows with a thermal spectrum whose peak moves to shorter wavelengths as it gets hotter, in inverse proportion to its '
             'temperature T. Write the wavelength at which it shines brightest, from the quantum of action h, the speed of light and the thermal energy scale.',
        name="Wien's displacement law (from Planck's spectrum)", lhs='λ',
        form='h * c / ( 4.965 * kB * T )',
        expr='h * c / ( 4.965 * kB * T ) * 1000000',
        ask='Find the peak wavelength in micrometres for a star with the Sun\'s surface temperature (h, c and k_B in SI units).',
        vars={'λ': 'wavelength of the peak (µm)', 'h': 'Planck constant (J·s)', 'c': 'speed of light (m/s)', 'kB': 'Boltzmann constant (J/K)',
              'T': 'temperature (K)'},
        given={'h': 6.626e-34, 'c': 2.998e8, 'kB': 1.381e-23, 'T': 5778}, unit='µm',
        method='Differentiate the thermal spectrum with respect to wavelength and set it to zero: the root is a fixed number times hc/(k_B T).',
        distractors=['σ', 'ν', '4']),
    'cartography': dict(
        goal='A map that keeps every angle true (so a compass course is a straight line) must stretch north-south distances more and more towards the '
             'poles. On a sphere of radius R, write how far up the map a point at latitude φ is drawn.',
        name='Conformal cylindrical projection', lhs='Y',
        form='R * ln ( tan ( π / 4 + φ / 2 ) )',
        expr='R * ln ( sin ( 0.785398 + φ / 2 ) / cos ( 0.785398 + φ / 2 ) )',
        ask='Find Y in kilometres for latitude 45° (0.7854 rad) on a planet of radius R km.',
        vars={'Y': 'distance up the map (km)', 'R': 'radius of the planet (km)', 'φ': 'latitude (rad)', 'π': 'pi'},
        given={'R': 6371, 'φ': 0.7854}, unit='km',
        method='Keeping angles means the north-south stretch at each latitude must equal the east-west one, 1/cos φ: integrate R/cos φ from the equator.',
        distractors=['sin', 'cos', 'θ']),
    'geodesy': dict(
        goal='Surveyors measure one side c of a triangle and the angles at both its ends; the third angle γ follows. Write the length a of the side opposite '
             'the angle α.',
        name='Triangulation by the sine rule', lhs='a',
        form='c * sin ( α ) / sin ( γ )',
        expr='c * sin ( α ) / sin ( γ )',
        ask='Find a in metres for a 1000 m baseline c, α = 0.9 rad and γ = 1.2 rad.',
        vars={'a': 'side opposite α (m)', 'c': 'measured baseline (m)', 'α': 'angle opposite a', 'γ': 'angle opposite the baseline'},
        given={'c': 1000, 'α': 0.9, 'γ': 1.2}, unit='m',
        method='Drop a height from one corner onto the opposite side: it equals each adjacent side times the sine of its angle, so a sin γ = c sin α.',
        distractors=['cos', 'tan', 'b']),
    'geomorphology': dict(
        goal='A small grain of density ρ_s sinks through still water of density ρ_f and viscosity μ. It soon stops speeding up: the drag of the slow flow '
             'around it balances its weight less its buoyancy. Write the speed v at which a sphere of radius r settles.',
        name='Settling speed of a small sphere', lhs='v',
        form='2 / 9 * ( ρ _ s - ρ _ f ) * g * r ^ 2 / μ',
        expr='2 / 9 * ( S - F ) * g * r ^ 2 / μ',
        ask='Find v in metres per second for a 0.1 mm quartz grain in water.',
        vars={'v': 'settling speed (m/s)', 'ρ': 'density', 's': 'of the solid grain', 'f': 'of the fluid', 'g': 'gravity (m/s²)', 'r': 'grain radius (m)',
              'μ': 'viscosity (Pa·s)', 'S': 'grain density (kg/m³)', 'F': 'water density (kg/m³)'},
        given={'S': 2650, 'F': 1000, 'g': 9.81, 'r': 0.0001, 'μ': 0.001}, unit='m/s',
        method='At low speed the drag on a sphere is 6πμrv; set it equal to the weight minus buoyancy, (4/3)πr³(ρ_s − ρ_f)g.',
        distractors=['ν', 'π', '6']),
    'radioactivity': dict(
        goal='Each unstable nucleus has the same small chance λ per second of decaying, whatever the others do. Starting with N _ 0 nuclei, write how many '
             'N are left after a time t.',
        name='Radioactive decay law', lhs='N',
        form='N _ 0 * exp ( - λ * t )',
        expr='M * exp ( - 0.693147 * t / T )',
        ask='A sample starts with M = 1000 radium nuclei; radium-226 has a half-life T of 1600 years. Find how many remain after t = 400 years.',
        vars={'N': 'nuclei left', 'λ': 'decay constant (per year)', 't': 'time (years)', 'M': 'nuclei at the start', 'T': 'half-life (years)'},
        given={'M': 1000, 't': 400, 'T': 1600}, unit='nuclei',
        method='dN/dt = −λN: the decay rate is proportional to what is left. The half-life is the time for exp(−λt) to reach 1/2, so λ = ln 2 / T.',
        distractors=['τ', 'ln', '2']),
    'nuclear_binding_energy': dict(
        goal='A nucleus of Z protons and A − Z neutrons weighs less than its parts did apart. Write the energy B that holds it together, from the proton '
             'mass m_p, the neutron mass m_n, the nucleus\'s mass M and the speed of light.',
        name='Nuclear binding energy (mass defect)', lhs='B',
        form='( Z * m _ p + ( A - Z ) * m _ n - M ) * c ^ 2',
        expr='( Z * P + ( A - Z ) * N - M ) * 931.494',
        ask='Find B in MeV for helium-4, with masses in atomic mass units (1 u·c² = 931.494 MeV).',
        vars={'B': 'binding energy (MeV)', 'Z': 'protons', 'A': 'mass number', 'm': 'mass', 'p': 'of a proton', 'n': 'of a neutron', 'M': 'mass of the nucleus (u)',
              'c': 'speed of light', 'P': 'proton mass (u)', 'N': 'neutron mass (u)'},
        given={'Z': 2, 'A': 4, 'P': 1.007276, 'N': 1.008665, 'M': 4.001506}, unit='MeV',
        method='Mass and energy are one: the mass that went missing when the nucleons came together left as energy, E = Δm c².',
        distractors=['e', 'h', '2']),
    'magnetostatics': dict(
        goal='A long straight wire carries a steady current I. Its magnetic field circles the wire; integrate the field around a circle of radius r and the '
             'total is fixed by the current through it. Write the field strength B at distance r.',
        name="Ampère's circuital law for a straight wire", lhs='B',
        form='μ _ 0 * I / ( 2 * π * r )',
        expr='0.2 * I / r',
        ask='Find B in microtesla 5 cm from a wire carrying 10 A.',
        vars={'B': 'magnetic field (µT)', 'μ': 'permeability', 'I': 'current (A)', 'r': 'distance from the wire (m)', 'π': 'pi'},
        given={'I': 10, 'r': 0.05}, unit='µT',
        method='Around any closed loop the field adds up to μ₀ times the current threading it; by symmetry B is the same all round the circle of length 2πr.',
        distractors=['r', '4', 'ε']),
    'electromagnetic_induction': dict(
        goal='A coil of N turns, each of area A, spins at angular speed ω in a steady field B. The flux through it rises and falls as the coil turns, and '
             'the changing flux drives a voltage. Write the voltage ε at time t.',
        name='EMF of a rotating coil', lhs='ε',
        form='N * B * A * ω * sin ( ω * t )',
        expr='N * B * A * ω',
        ask='Find the peak voltage in volts for 100 turns of 0.01 m² spinning at 50 turns a second (ω = 314.16 rad/s) in 0.5 T.',
        vars={'ε': 'voltage (V)', 'N': 'number of turns', 'B': 'field (T)', 'A': 'area of a turn (m²)', 'ω': 'angular speed (rad/s)', 't': 'time (s)'},
        given={'N': 100, 'B': 0.5, 'A': 0.01, 'ω': 314.16}, unit='V',
        method='The flux through the coil is NBA cos ωt; the voltage is minus its rate of change.',
        distractors=['cos', 'I', 'R']),
    'ferromagnetism': dict(
        goal='Above a critical temperature the spins in iron no longer line up by themselves, but an applied field still lines them up a little, more '
             'easily the closer the iron is to that temperature. Write the susceptibility χ, with C a constant of the material and T_c the critical '
             'temperature.',
        name='Curie-Weiss law', lhs='χ',
        form='C / ( T - T _ c )',
        expr='C / ( T - X )',
        ask='Find χ for iron at 1100 K, with C = 1.5 K and a critical temperature X of 1043 K.',
        vars={'χ': 'magnetic susceptibility', 'C': 'material constant (K)', 'T': 'temperature (K)', 'c': 'critical', 'X': 'critical temperature (K)'},
        given={'C': 1.5, 'T': 1100, 'X': 1043}, unit='',
        method='Mean-field theory: each spin feels the applied field plus a field proportional to the average magnetisation of its neighbours. Solve for '
               'the magnetisation per unit applied field.',
        distractors=['μ', 'B', '1']),
    'probability_theory': dict(
        goal='An experiment either succeeds, with probability p, or fails. It is repeated n times, independently. Write the probability P of exactly k '
             'successes.',
        name='Binomial distribution', lhs='P',
        form='binom ( n , k ) * p ^ k * ( 1 - p ) ^ ( n - k )',
        expr='120 * p ^ k * ( 1 - p ) ^ ( n - k )',
        ask='Find the probability of exactly 3 heads in 10 tosses of a fair coin (there are 120 ways to choose which 3).',
        vars={'P': 'probability', 'n': 'number of trials', 'k': 'number of successes', 'p': 'chance of success in one trial'},
        given={'n': 10, 'k': 3, 'p': 0.5}, unit='',
        method='Each particular sequence with k successes has probability p^k (1 − p)^(n − k); count how many such sequences there are.',
        distractors=['q', 'e', 'm']),
    'combinatorics': dict(
        goal='The number of ways to put n objects in order grows faster than any power of n. For large n its logarithm is very nearly a simple expression. '
             'Write it.',
        name='Large-n approximation of the factorial', lhs='ln ( n ! )',
        form='n * ln ( n ) - n',
        expr='n * ln ( n ) - n',
        ask='Find ln(100!) approximately.',
        vars={'n': 'number of objects'},
        given={'n': 100}, unit='',
        method='ln n! = ln 1 + ln 2 + … + ln n. For large n the sum is close to the integral of ln x from 1 to n.',
        distractors=['exp', 'π', '2']),
}

# =================================================================================== RECIPES
SP = 'spectroscopy'
RECIPES = [
    # (name, machine recipe type, item inputs, fluid inputs, item outputs, fluid outputs, energy, ticks, research)
    ('spectral_plate', SP, [('minecraft:glass_pane', 2), ('minecraft:amethyst_shard', 1)], [], [('spectral_plate', 2)], [], 20000, 200, 'spectroscopy'),
    ('radium_assay', SP, [('raw_pitchblende', 4), ('spectral_plate', 1)], [], [('radium_salt', 1), ('magnetite', 1)], [], 40000, 400, 'radioactivity'),
]
CRAFTING = [
    # shaped: (name, pattern, key, result, count)
    ('telescope_mirror_blank', ['NGN', 'GQG', 'NGN'], {'N': 'minecraft:iron_nugget', 'G': 'minecraft:glass', 'Q': 'minecraft:quartz'}, 'telescope_mirror_blank', 2),
    ('theodolite_lens', [' T ', 'CSC'], {'T': 'telescope_mirror_blank', 'C': 'minecraft:copper_ingot', 'S': 'minecraft:spyglass'}, 'theodolite_lens', 1),
    ('geoid_model', ['MKM', 'KSK', 'MKM'], {'M': 'survey_map', 'K': 'minecraft:clay_ball', 'S': 'minecraft:slime_ball'}, 'geoid_model', 1),
    ('geodetic_surveyor', [' T ', 'CSC', ' M '], {'T': 'theodolite_lens', 'C': 'minecraft:copper_ingot', 'S': 'minecraft:compass', 'M': 'survey_map'},
     'geodetic_surveyor', 1),
    ('geiger_tube', ['IGI', 'GCG', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'C': 'minecraft:copper_ingot'}, 'geiger_tube', 1),
    ('geiger_counter', [' T ', 'IRI', 'ICI'], {'T': 'geiger_tube', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone', 'C': 'minecraft:copper_ingot'},
     'geiger_counter', 1),
    ('ferrite_magnet', ['MKM', 'KRK', 'MKM'], {'M': 'magnetite', 'K': 'minecraft:clay_ball', 'R': 'minecraft:redstone'}, 'ferrite_magnet', 4),
    ('electrical_steel_lamination', ['III', 'QQQ', 'III'], {'I': 'minecraft:iron_ingot', 'Q': 'minecraft:quartz'}, 'electrical_steel_lamination', 6),
    ('gauss_meter', [' F ', 'CSC', ' R '], {'F': 'ferrite_magnet', 'C': 'minecraft:copper_ingot', 'S': 'minecraft:compass', 'R': 'minecraft:redstone'},
     'gauss_meter', 1),
    ('random_number_chip', ['DQD', 'RCR'], {'D': 'quantum_dot', 'Q': 'cherenkov_quartz', 'R': 'minecraft:redstone', 'C': 'minecraft:comparator'},
     'random_number_chip', 2),
]
SHAPELESS = [
    ('survey_map', ['minecraft:map', 'minecraft:compass', 'minecraft:feather', 'minecraft:ink_sac'], 'survey_map', 2),
    ('sediment_core', ['minecraft:sand', 'minecraft:gravel', 'minecraft:clay_ball', 'minecraft:dirt', 'minecraft:iron_ingot'], 'sediment_core', 2),
    ('magnetite_roast', ['minecraft:raw_iron', 'minecraft:raw_iron', 'minecraft:charcoal'], 'magnetite', 2),
    ('radium_salt_by_hand', ['raw_pitchblende', 'raw_pitchblende', 'raw_pitchblende', 'raw_pitchblende', 'raw_pitchblende', 'raw_pitchblende',
                             'raw_pitchblende', 'raw_pitchblende', 'minecraft:bone_meal'], 'radium_salt', 1),
    ('prussian_blue_tablet', ['minecraft:lapis_lazuli', 'minecraft:iron_nugget', 'minecraft:sugar'], 'prussian_blue_tablet', 2),
    ('dice_set', ['minecraft:bone', 'minecraft:bone', 'minecraft:black_dye'], 'dice_set', 1),
]

# =================================================================================== TAGS
FERROMAGNETIC = ['minecraft:iron_ingot', 'minecraft:iron_nugget', 'minecraft:iron_block', 'minecraft:raw_iron', 'minecraft:iron_helmet',
                 'minecraft:iron_chestplate', 'minecraft:iron_leggings', 'minecraft:iron_boots', 'minecraft:chainmail_helmet',
                 'minecraft:chainmail_chestplate', 'minecraft:chainmail_leggings', 'minecraft:chainmail_boots', 'minecraft:iron_sword',
                 'minecraft:iron_pickaxe', 'minecraft:iron_axe', 'minecraft:iron_shovel', 'minecraft:iron_hoe', 'minecraft:shears',
                 'minecraft:anvil', 'minecraft:compass', 'minecraft:lodestone', f'{NS}:magnetite', f'{NS}:ferrite_magnet',
                 f'{NS}:electrical_steel_lamination', f'{NS}:gauss_meter']
FERROMAGNETIC_TAGS = ['c:ingots/iron', 'c:nuggets/iron', 'c:storage_blocks/iron', 'c:ingots/steel', 'c:nuggets/steel', 'c:storage_blocks/steel',
                      'c:ingots/nickel', 'c:storage_blocks/nickel', 'c:ingots/cobalt', 'c:storage_blocks/cobalt', 'c:plates/iron', 'c:plates/steel',
                      'c:plates/nickel', 'c:dusts/iron', 'c:raw_materials/iron', 'c:raw_materials/nickel', 'c:raw_materials/cobalt']
RADIOACTIVE = [f'{NS}:raw_pitchblende', f'{NS}:pitchblende_ore', f'{NS}:deepslate_pitchblende_ore']
RADIOACTIVE_TAGS = ['c:raw_materials/uranium', 'c:ores/uranium', 'c:raw_materials/thorium', 'c:ores/thorium']
STRONG = [f'{NS}:radium_salt']
STRONG_TAGS = ['c:ingots/uranium', 'c:ingots/plutonium', 'c:ingots/thorium']
RADIOACTIVE_BLOCKS = [f'{NS}:pitchblende_ore', f'{NS}:deepslate_pitchblende_ore']
RADIOACTIVE_BLOCK_TAGS = ['c:ores/uranium', 'c:ores/thorium', 'c:storage_blocks/uranium']
MAGNETIC_BLOCKS = ['minecraft:lodestone']

# NuclearRadiation (NuclearCraft) bindings: what each radioactive item holds, as atoms of its isotopes
NR_BINDINGS = {
    'raw_pitchblende': {'nr:u_238': 1.0e22, 'nr:u_234': 5.5e17},
    'pitchblende_ore': {'nr:u_238': 2.0e22, 'nr:u_234': 1.1e18},
    'deepslate_pitchblende_ore': {'nr:u_238': 2.0e22, 'nr:u_234': 1.1e18},
    # radium-226 is in uranium-238's own decay chain; NuclearRadiation has no radium, so it is given as the U-234 that matches its activity
    'radium_salt': {'nr:u_234': 4.0e21},
}

LANG = {
    'effect.theoreticalsciences.radiation_sickness': 'Radiation Sickness',
    'effect.theoreticalsciences.lithostatic_pressure': 'Lithostatic Pressure',
    'effect.theoreticalsciences.seismic_vertigo': 'Seismic Vertigo',
    'effect.theoreticalsciences.magnetized': 'Magnetized',
    'effect.theoreticalsciences.frame_drag': 'Frame Drag',
    'effect.theoreticalsciences.information_loss': 'Information Loss',
    'effect.theoreticalsciences.variance': 'Variance',
    'message.theoreticalsciences.survey_head': 'Survey at elevation %s, %s:',
    'message.theoreticalsciences.survey_none': 'No ores within 24 blocks.',
    'message.theoreticalsciences.survey_line': '  %s × %s - nearest %s m %s, %s levels',
    'message.theoreticalsciences.gauss': 'Magnetic field: %s',
    'message.theoreticalsciences.geiger': 'Geiger: %s counts/s · your dose %s',
    'message.theoreticalsciences.dice': 'You roll %s and %s: %s',
    'commands.theoreticalsciences.radiation.reset': 'Cleared the radiation dose of %s (and in Mekanism and GregTech, where installed)',
    'commands.theoreticalsciences.radiation.get': '%s carries a radiation dose of %s',
    'commands.theoreticalsciences.radiation.set': 'Set the radiation dose of %s to %s',
    'message.theoreticalsciences.boundary_no_pour': 'A boundary of the Quantum Foam cannot be poured out - only a tank can hold it.',
    'message.theoreticalsciences.prussian_blue': 'The tablet binds the contamination. Most of the dose is gone.',
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
    p = os.path.join(HERE, 'puzzles2', 'group_r37.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    for name, typ, ins, fins, outs, fouts, fe, t, res in RECIPES:
        g['mr']('r37_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res)
        p = os.path.join(g['REC'], typ, 'r37_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True          # first-tier recipes stay at the size written here
        g['w'](p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r37_' + name, pat, key, res, n)
    for name, items, res, n in SHAPELESS:
        g['shapeless']('r37_' + name, items, res, n)
    # pitchblende smelts down for its uranium later (1.3); for now the ore itself drops raw pitchblende


def _tag_file(g, kind, name, ids, tags):
    vals = list(ids) + [{'id': '#' + t, 'required': False} for t in tags]
    g['w'](os.path.join(g['D'], 'tags', kind, name + '.json'), {'replace': False, 'values': vals})


def _tags(g):
    _tag_file(g, 'item', 'ferromagnetic', FERROMAGNETIC, FERROMAGNETIC_TAGS)
    _tag_file(g, 'item', 'radioactive', RADIOACTIVE, RADIOACTIVE_TAGS)
    _tag_file(g, 'item', 'strongly_radioactive', STRONG, STRONG_TAGS)
    _tag_file(g, 'block', 'radioactive', RADIOACTIVE_BLOCKS, RADIOACTIVE_BLOCK_TAGS)
    _tag_file(g, 'block', 'magnetic', MAGNETIC_BLOCKS, [])
    # the ores are ores to every other mod too
    for kind in ('item', 'block'):
        g['w'](os.path.join(g['RES'], 'data', 'c', 'tags', kind, 'ores', 'uranium.json'),
               {'replace': False, 'values': [f'{NS}:pitchblende_ore', f'{NS}:deepslate_pitchblende_ore']})
    g['w'](os.path.join(g['RES'], 'data', 'c', 'tags', 'item', 'raw_materials', 'uranium.json'), {'replace': False, 'values': [f'{NS}:raw_pitchblende']})


def _worldgen(g):
    g['ore_feature']('ore_pitchblende', {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}, 'pitchblende_ore', 4, 2, 0, 48)
    g['ore_feature']('ore_deepslate_pitchblende', {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'},
                     'deepslate_pitchblende_ore', 5, 3, -64, 0)
    g['w'](os.path.join(g['D'], 'neoforge/biome_modifier', 'overworld_pitchblende.json'),
           {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld',
            'features': [f'{NS}:ore_pitchblende', f'{NS}:ore_deepslate_pitchblende'], 'step': 'underground_ores'})


def _nuclear_radiation(g):
    for item, iso in NR_BINDINGS.items():
        g['w'](os.path.join(g['D'], 'nuclear_radiation', 'bindings', item + '.json'),
               {'type': 'item', 'target': f'{NS}:{item}', 'isotopes': {k: {'atoms': v} for k, v in iso.items()}})


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)
    _tags(g)
    _worldgen(g)
    _nuclear_radiation(g)


def finish(g):
    te = g['text_entry']
    te('astrophysics', 'r37_telescopes', 'Telescopes', 'telescope_mirror_blank', [
        'Astronomy begins with gathering light. A $(item)Telescope Mirror Blank$() - glass, ground and silvered - collects a star\'s light over its whole face '
        'and brings it to one point. The wider the mirror, the fainter the stars it shows.',
        'But no mirror focuses light to a true point. Light is a wave, and a round opening of diameter D spreads it into a disc about 1.22λ/D wide. Two stars '
        'closer than that blur into one: $(thing)the diffraction limit$(). Bigger mirrors are sharper as well as brighter.'],
        adv='observational_astronomy', sort=1, spot=['telescope_mirror_blank'])
    te('astrophysics', 'r37_spectrograph', 'The Spectrograph', 'spectrograph', [
        'Split light into its colours and it tells you what made it. A hot body glows brightest at a wavelength set by its temperature alone: '
        '$(thing)λ = b/T$(), with b = 2.898 mm·K. The Sun, at 5,778 K, peaks in green light.',
        'Atoms leave their own marks: dark lines where cool gas in front absorbs, bright lines where hot gas emits, each element at its own wavelengths. '
        'That is how helium was found on the Sun before anyone found it on Earth.',
        'The $(item)Spectrograph$() turns glass and amethyst into $(item)Spectral Plates$(). Fed $(item)Raw Pitchblende$() and a plate, it finds the radium '
        'hidden in the ore by its lines and separates it as $(item)Radium Salt$(), with a little magnetite left over.'],
        adv='spectroscopy', sort=2, spot=['spectrograph', 'spectral_plate'])
    te('geography', 'r37_mapping', 'Maps & Measurement', 'geodetic_surveyor', [
        'A map flattens a round planet, and no flat map keeps everything true. Keep every angle true - so a compass course is a straight line - and the map must '
        'stretch towards the poles: a point at latitude φ is drawn at R·ln tan(π/4 + φ/2).',
        '$(thing)Geodesy$() measures the planet itself. Measure one baseline, sight two angles with a $(item)Theodolite Lens$(), and the sine rule gives every '
        'other side of the triangle. Chain triangles together and a whole country is mapped.',
        'The $(item)Geodetic Surveyor$() does it for you: use it to list the ores within 24 blocks - how many of each, and how far and which way the nearest '
        'one lies. A burst of light marks each.'],
        adv='cartography', sort=1, spot=['survey_map', 'theodolite_lens', 'geodetic_surveyor'])
    te('geography', 'r37_ground', 'Reading the Ground', 'sediment_core', [
        'Landscapes are written by water. A river carries grains as fast as it flows, and drops them as it slows - the heaviest first. A small grain of radius '
        'r sinks at a steady speed v = (2/9)(ρ_s − ρ_f)g r²/μ: sand settles in minutes, clay in days.',
        'That is why the ground is layered. A $(item)Sediment Core$() is a tube of those layers, oldest at the bottom: floods, droughts and ash falls in the '
        'order they happened.'],
        adv='geomorphology', sort=2, spot=['sediment_core', 'geoid_model'])
    te('nuclear', 'r37_radioactivity', 'Radioactivity', 'radium_salt', [
        '$(item)Pitchblende Ore$() lies in stone and, more often, in deepslate. It is uranium ore, and it is radioactive: its nuclei fall apart on their own, at '
        'random, but at a fixed rate. Of N₀ nuclei, N = N₀·e^(−λt) remain - half of them after each half-life.',
        'Eight $(item)Raw Pitchblende$() and a little bone meal give one $(item)Radium Salt$() by hand; the $(item)Spectrograph$() does it from four. Radium is '
        'a hundred thousand times more active than the uranium it came from.',
        '$(thing)Radiation Sickness$(): every radioactive thing you carry adds to a dose (the item tags $(thing)radioactive$() and $(thing)strongly_radioactive$()). '
        'The dose fades slowly. Past a point it brings nausea, hunger and lower maximum health. A $(item)Prussian Blue Tablet$() flushes most of it out.',
        'The $(item)Geiger Counter$(), held in either hand, clicks for every decay it hears and shows your dose. Pack makers can turn Radiation Sickness off in '
        'the config. With Mekanism or GregTech installed the same dose also counts in their radiation; NuclearCraft\'s radiation reads this mod\'s items too.'],
        adv='radioactivity', sort=1, spot=['raw_pitchblende', 'radium_salt', 'geiger_counter'])
    te('nuclear', 'r37_binding', 'The Binding of Nuclei', 'geiger_counter', [
        'Weigh a helium nucleus and it is lighter than its two protons and two neutrons were apart, by about 0.03 atomic mass units. That missing mass is '
        'the $(thing)binding energy$(): B = Δm·c², 28.3 MeV for helium-4 - several million times what a chemical bond holds.',
        'Binding energy per nucleon rises from hydrogen to iron and falls after it. Split a heavy nucleus or fuse two light ones, and both ways climb towards '
        'iron and release energy. Fission and fusion are where this leads.'],
        adv='nuclear_binding_energy', sort=2)
    te('electromagnetism', 'r37_fields', 'Magnetic Fields', 'gauss_meter', [
        'A current makes a magnetic field. Around a long straight wire it circles the wire, and its strength falls with distance: B = μ₀I/(2πr). Ten amps give '
        '40 µT five centimetres away - about the strength of the planet\'s own field.',
        'The $(item)Gauss Meter$() reads the field where you stand: the planet\'s (25 to 65 µT in the Overworld, none in the Nether or the End) and any '
        'magnets near you (block tag $(thing)magnetic$(): lodestones, and more to come).'],
        adv='magnetostatics', sort=1, spot=['gauss_meter'])
    te('electromagnetism', 'r37_induction', 'Induction & Magnets', 'ferrite_magnet', [
        'A changing field drives a current: spin a coil of N turns in a field B and it gives ε = NBAω·sin ωt. Every generator works this way. The iron core that '
        'guides the field is built from thin $(item)Electrical Steel Laminations$(), so the changing field cannot stir currents inside it.',
        '$(item)Magnetite$() comes from raw iron roasted with charcoal, or from the Spectrograph\'s radium assay. Pressed with clay and redstone it makes '
        '$(item)Ferrite Magnets$(): iron oxide that stays magnetised.',
        'Iron is ferromagnetic because its atoms\' spins line up by themselves - until a critical temperature, 1043 K for iron, shakes them apart. Above it the '
        'susceptibility is χ = C/(T − T_c). The item tag $(thing)ferromagnetic$() lists what a field pulls on, for the Magnetized effect to come.'],
        adv='electromagnetic_induction', sort=2, spot=['magnetite', 'electrical_steel_lamination', 'ferrite_magnet'])
    te('probability', 'r37_chance', 'Chance & Counting', 'dice_set', [
        'Chance has laws. Repeat an experiment n times, each a success with probability p, and exactly k successes happen with probability '
        'C(n,k)·p^k·(1 − p)^(n−k). Three heads in ten tosses: 11.7%.',
        'Roll the $(item)Dice Set$() and add them: seven comes up a sixth of the time, two and twelve one time in thirty-six each.',
        '$(thing)Combinatorics$() counts without listing. There are n! ways to order n things, and for large n, ln n! ≈ n ln n − n. The '
        '$(item)Quantum Random Number Chip$() draws its bits from single photons at a half-silvered mirror - the only randomness nothing can predict.'],
        adv='probability_theory', sort=1, spot=['dice_set', 'random_number_chip'])
