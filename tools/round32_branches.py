"""
Round 32 (round32_branches): two new branches of physics about time itself - where a universe comes from and how it
ends.

* ORIGIN (the beginning of a universe): the hot Big Bang (Friedmann), primordial nucleosynthesis, cosmic inflation,
  reheating, leptogenesis, the stability of the vacuum (what can stop a universe from ever starting), creation from
  nothing (quantum cosmology) - and, as the bridge to the other branch, conformal cyclic cosmology.
* FINALITY (the end of a universe): cosmic eschatology (the fate written in w and Ω), proton decay, the black hole era,
  the Big Rip, the Big Crunch and the Big Bounce, vacuum decay, heat death - and what a dead universe still does:
  Boltzmann fluctuations and Poincare recurrence.
* Two multiblocks: the Inflation Chamber (Origin) and the Entropy Horizon (Finality). Every new material has a machine
  route from renewable inputs; the end of one universe (the ember and the horizon of a heat death) is, through
  conformal cyclic cosmology, the seed of the next.

The research JSON, puzzles (tools/puzzles2/group_branches.json, written from PUZZLES here), recipes and Codex chapters
are all generated from this file.
"""
import json
import math
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))


# =================================================================================== CONTENT
def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    # ---- Origin
    _I('primordial_ash', 'Primordial Nucleosynthesis Ash', 'dots', '#d8c890', '#fff8e0',
       'What the first three minutes made: three parts hydrogen to one of helium-4, a pinch of deuterium and helium-3, a trace of lithium-7.'),
    _I('inflaton_condensate', 'Inflaton Condensate', 'orb', '#ff70d0', '#fff0ff',
       'A scalar field held at the top of its potential, rolling so slowly that space doubles many times before it moves.'),
    _I('baryon_asymmetry_crystal', 'Baryon Asymmetry Crystal', 'crystal', '#ffd060', '#fffff0',
       'One extra quark in every billion pairs. Everything that was not annihilated grew out of that one part in a billion.'),
    _I('metastable_vacuum_lattice', 'Metastable Vacuum Lattice', 'lattice', '#40d0a0', '#e0fff0',
       'The Higgs potential mapped out to the Planck scale: the valley we live in is not the deepest one. Held in a lattice of Higgs field capacitors.'),
    _I('no_boundary_seed', 'No-Boundary Seed', 'useed', '#e8e0ff', '#ffffff',
       'A closed universe tunnelled from nothing: round, without an edge, and without a first moment of time.'),
    _I('aeon_boundary_shard', 'Aeon Boundary Shard', 'shard', '#80e0ff', '#fff4d0',
       'Rescale an infinitely old, empty, massless universe and it looks like a Big Bang. A sliver of the seam between two aeons.'),
    # ---- Finality
    _I('proton_decay_residue', 'Proton Decay Residue', 'pellet', '#ff9060', '#fff0e0',
       'Positrons and neutral pions from protons that lived 10^34 years and then simply came apart.'),
    _I('evaporation_flash', 'Last Flash of a Black Hole', 'ghostorb', '#c0a0ff', '#ffffff',
       'The final second of an evaporating black hole: hotter and brighter the smaller it gets, until nothing is left but the light.'),
    _I('phantom_energy_cell', 'Phantom Energy Cell', 'warp', '#ff3060', '#ffd0e0',
       'Dark energy with w below -1. Its density grows as space expands, until it tears apart galaxies, stars, atoms - and then the cell.'),
    _I('bounce_core', 'Bounce Core', 'singularity', '#ffb040', '#ffffe0',
       'A universe crushed to the Planck density, where the quantum geometry of space turned the collapse around.'),
    _I('true_vacuum_seed', 'True Vacuum Seed', 'seed', '#20ff80', '#f0fff0',
       'A bubble of the deeper vacuum, held below its critical radius. Let it grow and it expands at the speed of light, forever.'),
    _I('heat_death_ember', 'Heat Death Ember', 'remnant', '#503040', '#ff9060',
       'The last warmth of a universe going cold: a little above the temperature of its own horizon, and falling.'),
    _I('de_sitter_horizon_shard', 'De Sitter Horizon Shard', 'shard', '#203060', '#a0c0ff',
       'A piece of the cosmic horizon of an empty, accelerating universe. It glows at 10^-30 K and holds 10^122 bits.'),
    _I('boltzmann_fluctuation', 'Boltzmann Fluctuation', 'ghostorb', '#80ffd0', '#ffffff',
       'Wait long enough in a universe at equilibrium and anything happens by chance. This is the anything.'),
]
BLOCKS = []
FLUIDS = [
    dict(id='reheating_plasma', name='Reheating Plasma', c1='#ff5020', c2='#fff6c0', gas=False, light=15, temp=1000000000, density=200,
         viscosity=300, art='plasma'),
    dict(id='dark_era_haze', name='Dark Era Haze', c1='#100818', c2='#504070', gas=True, light=1, temp=1, density=-200, viscosity=50,
         art='darkgas'),
]

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'inflation_chamber': ('Inflation Chamber',
                          'A sphere of containment glass around a pocket of false vacuum, wound with coils and lensed by gravitons. The inflaton '
                          'inside rolls down its potential slowly enough to inflate - and then it reheats, and a universe\'s worth of matter is made.'),
    'entropy_horizon': ('Entropy Horizon',
                        'A ring of event-horizon casing that runs a universe forward to its end: stars burn out, protons decay, black holes '
                        'evaporate, and the horizon glows at its own faint temperature over what is left.'),
}
MACHINE_CAT = {'inflation_chamber': 'strings', 'entropy_horizon': 'spacetime'}
MACHINE_RESEARCH = {'inflation_chamber': 'cosmic_inflation', 'entropy_horizon': 'heat_death'}
TIER = {'inflation_chamber': 5, 'entropy_horizon': 5}
MC = {
    'inflation_chamber': (['XPX', 'FSF', 'XLX'], {'X': 'cosmogenic_casing', 'P': 'quantum_processor', 'F': 'field_emitter',
                                                  'S': 'dark_energy_siphon', 'L': 'dark_matter_lens'}),
    'entropy_horizon': (['EPE', 'NSN', 'EHE'], {'E': 'event_horizon_casing', 'P': 'quantum_processor', 'N': 'negentropy_cell',
                                                'S': 'kugelblitz_chamber', 'H': 'hawking_absorber_plate'}),
}
EXTRA = {
    'inflation_chamber': 'Makes $(item)Inflaton Condensate$() from the $(fluid)Inflaton Field$() and $(fluid)False Vacuum$(), reheats it into $(fluid)Reheating Plasma$(), '
                         'grows $(item)No-Boundary Seeds$() - and, once you understand conformal cyclic cosmology, a new $(item)Universe Seed$() out of the end of an old universe.',
    'entropy_horizon': 'Runs matter forward to the end of time: $(item)Heat Death Embers$(), $(fluid)Dark Era Haze$(), $(item)De Sitter Horizon Shards$(), '
                       '$(item)Heat Death Remnants$() and, given long enough, a $(item)Boltzmann Fluctuation$().',
}


def inflation_chamber():
    """A glass sphere on a cosmogenic pedestal, a coil belt round its equator, graviton lenses at the poles."""
    s = shapes.Shape(9, 11, 9)
    c, cy = 4, 5
    s.disc(c, c, 0, 4, 'X')
    s.disc(c, c, 1, 2.2, 'X')
    s.sphere(c, cy, c, 3.6, 'G', shell=0.55, filter=lambda x, y, z: y >= 2)
    s.disc(c, c, cy, 4.0, 'K', r_in=3.1)
    for dx, dz in ((-3, 0), (3, 0), (0, -3), (0, 3)):
        s.put(c + dx, 1, c + dz, 's')                     # stabilisers hold the bubble on its pedestal
    s.put(c, 2, c, 'F')
    s.put(c, 9, c, 'l')
    s.disc(c, c, 10, 1.2, 'X')
    s.put(c, 10, c, 'F')
    s.sphere_air(c, cy, c, 3.0)
    s.put(c, 2, c, 'F')
    s.ports('X', 'Z', 4, y=0)
    s.controller(c, 0, 0)
    s.center = (c, cy, c)
    return s


def entropy_horizon():
    """A squat drum: event-horizon walls, a band of glass between two coil rings, Hawking collectors on four
    chronoplastic pillars, and an empty core where the last light of a universe fades."""
    s = shapes.Shape(9, 8, 9)
    c = 4
    s.disc(c, c, 0, 4, 'E')
    s.disc(c, c, 1, 3.6, 'E', r_in=2.6)
    s.disc(c, c, 2, 3.6, 'K', r_in=2.6)
    s.disc(c, c, 3, 3.6, 'G', r_in=2.6)
    s.disc(c, c, 4, 3.6, 'K', r_in=2.6)
    s.disc(c, c, 5, 3.6, 'E', r_in=2.6)
    s.disc(c, c, 6, 2.6, 'E')
    s.put(c, 6, c, 'F')
    for dx, dz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        s.column(c + dx, c + dz, 1, 5, 'O')
        s.put(c + dx, 6, c + dz, 'w')                     # Hawking collectors catch the horizon's glow
    for y in range(1, 6):
        s.disc(c, c, y, 2.2, '.')
    s.put(c, 1, c, 'z')                                   # a causal shield under the core
    s.ports('E', 'V', 4, y=0)
    s.controller(c, 0, 0)
    s.center = (c, 3, c)
    return s


shapes.BUILDERS['inflation_chamber'] = inflation_chamber
shapes.BUILDERS['entropy_horizon'] = entropy_horizon

_r12.model('inflation_chamber', 'Inflation Chamber',
           'A scalar field φ with potential V = ½m²φ² inflates while it rolls slowly: the slow-roll parameter ε = 2/φ² (in reduced Planck units) must stay below 1, '
           'and the number of e-folds N = (φ₀² − 2)/4 must reach about 60 to make a smooth, flat universe. Start too high, though, and quantum jumps of the field '
           'outrun its classical roll - inflation becomes eternal and the bubble never reheats.',
           'Quantum fluctuations kick the starting field by ±3%·d.',
           [_P('phi', 'Initial field φ₀', 'M_P', 0, 40, 0.1, 10, 'Where on its potential the inflaton starts. Higher means more e-folds.'),
            _P('Gam', 'Reheating coupling Γ', 'GeV', 0, 10, 0.01, 1, 'How quickly the inflaton decays into particles once it reaches the bottom.')],
           [_D('phe', 'Effective field', 'M_P', 'phi*(1 + 0.03*d)', 'φ = φ₀·(1 + 0.03·d)', 'The starting field the fluctuations leave you with.'),
            _D('eps', 'Slow-roll ε', '', '2/(phe^2 + 1e-9)', 'ε = 2/φ²', 'Inflation only happens while ε < 1.'),
            _D('Nf', 'e-folds N', '', 'pos(phe^2 - 2)/4', 'N = (φ₀² − 2)/4', 'How many times space grows by e. About 60 solve the horizon and flatness problems.'),
            _D('Trh', 'Reheating temperature', '10⁹ GeV', '0.5406*sqrt(Gam*2.435)', 'T_rh = (90/π²g*)^¼·√(Γ·M_P)', 'How hot the universe is when inflation ends.')],
           [_C('noinfl', 'stop', '1 - eps', 'No inflation: the field is too low on its potential (ε above 1)', 'ε < 1'),
            _C('short', 'stop', 'Nf - 50', 'Too few e-folds: the bubble comes out curved and lumpy', 'N ≥ 50'),
            _C('eternal', 'damage', '30 - phe', 'Eternal inflation: quantum jumps outrun the roll and the bubble never reheats', 'φ₀ < 30 M_P'),
            _C('cold', 'stop', 'Trh - 0.05', 'Reheating too weak: the universe stays cold and empty', 'T_rh > 5×10⁷ GeV'),
            _C('overheat', 'damage', '1500 - T', 'The containment sphere is overheating', 'T < 1500 K')],
           rate='clamp(Nf/60, 0, 2)*clamp(Trh, 0, 2)', rate_formula='rate = N/60 · T_rh', rate_desc='More e-folds and a hotter reheating make the work go faster.',
           heat='0.5 + 0.4*Trh', idle_heat='0.5', kappa=50, heat_capacity=60)
_r12.WINDOWS['inflation_chamber'] = {}

_r12.model('entropy_horizon', 'Entropy Horizon',
           'Matter is run forward through the eras of an expanding universe until it reaches the temperature of its own horizon. The horizon of a de Sitter universe '
           'of Hubble rate H glows at T = ħH/(2πk_B); everything left inside cools towards it, and the work goes as fast as the entropy still left to produce. '
           'Squeeze the horizon too small and it is hot enough to boil the containment.',
           'The expansion rate wanders by ±5%·d.',
           [_P('Hs', 'Horizon Hubble rate H', '10⁻¹⁸ s⁻¹', 0.1, 50, 0.1, 2.2, 'How fast the simulated space expands. A faster expansion means a smaller, hotter horizon.'),
            _P('age', 'Simulated age', 'log₁₀ years', 10, 120, 1, 40, 'How far the simulation is run. Protons decay near 10³⁴ years, black holes evaporate by 10¹⁰⁰.')],
           [_D('He', 'Effective Hubble rate', '10⁻¹⁸ s⁻¹', 'Hs*(1 + 0.05*d)', 'H = H₀·(1 + 0.05·d)', 'The expansion rate the drift leaves you with.'),
            _D('Tgh', 'Horizon temperature', '10⁻³⁰ K', '1.215*He', 'T = ħH/(2πk_B)', 'The faint thermal glow of the cosmic horizon.'),
            _D('Rh', 'Horizon radius', 'Gly', '13.6*2.2/(He + 1e-9)', 'R = c/H', 'How far away the horizon is.'),
            _D('Sleft', 'Entropy still to produce', '', 'clamp((age - 10)/90, 0, 1)', 'S/S_max ≈ (log t − 10)/90', 'How close the simulated universe is to maximum entropy.')],
           [_C('young', 'stop', 'age - 34', 'Too young: the stars are still burning and the protons have not decayed', 'age ≥ 10³⁴ years'),
            _C('hot', 'damage', '60 - Tgh', 'The horizon is too small and too hot: the containment is boiling', 'T < 6×10⁻²⁹ K'),
            _C('overheat', 'damage', '1200 - T', 'The horizon casing is overheating', 'T < 1200 K')],
           rate='clamp(Sleft*2, 0, 3)', rate_formula='rate = 2·S/S_max', rate_desc='The further towards maximum entropy, the faster the ember and the haze come off.',
           heat='0.3 + 0.02*Tgh', idle_heat='0.3', kappa=60, heat_capacity=60)
_r12.WINDOWS['entropy_horizon'] = {}

# =================================================================================== RESEARCH
# id, title, description, icon, x, y, insight, requires, cost
RESEARCH = [
    # ---------------------------------------------------------------- ORIGIN
    ('big_bang_cosmology', 'The Hot Big Bang',
     'Everything we see was once hotter and denser than the inside of any star, and has been expanding and cooling ever since. The Friedmann equations of general '
     'relativity say how fast: by what fills the universe and how it is curved. The first study of the Origin.',
     'cosmic_record_fragment', 9, -6, 110, ['general_relativity', 'nucleosynthesis'], [('metric_tensor', 4)]),
    ('primordial_nucleosynthesis', 'Primordial Nucleosynthesis',
     'For three minutes the whole universe was a fusion reactor. Neutrons froze out at one in six, decayed a little, and were swept into helium-4: a quarter of all '
     'matter, as measured. Unlocks Primordial Nucleosynthesis Ash.',
     'primordial_ash', 10, -7, 120, ['big_bang_cosmology'], [('white_dwarf_core', 4)]),
    ('cosmic_inflation', 'Cosmic Inflation',
     'Why is the universe so flat, so smooth, and the same in every direction - even regions that could never have touched? Because a scalar field, the inflaton, '
     'made space grow by e sixty times over in 10^-32 seconds. Unlocks the Inflation Chamber and Inflaton Condensate.',
     'inflaton_condensate', 10, -6, 160, ['big_bang_cosmology', 'dark_energy'], [('dark_matter_lens', 8)]),
    ('reheating', 'Reheating',
     'Inflation leaves space empty and cold. When the inflaton reaches the bottom of its potential it oscillates and decays, and its energy becomes a hot plasma of '
     'every particle there is: the hot Big Bang begins here. Unlocks Reheating Plasma.',
     'reheating_plasma_cell', 11, -6, 170, ['cosmic_inflation', 'particle_physics'], [('strange_matter_pellet', 4)]),
    ('leptogenesis', 'Leptogenesis',
     'Matter and antimatter should have annihilated perfectly. They did not: heavy neutrinos decayed out of equilibrium, unequally to leptons and antileptons, and '
     'sphalerons turned the excess into baryons - one in a billion. Unlocks the Baryon Asymmetry Crystal.',
     'baryon_asymmetry_crystal', 12, -7, 190, ['reheating', 'neutrino_physics'], [('sterile_neutrino', 8)]),
    ('vacuum_stability', 'Vacuum Stability',
     'What could stop a universe from starting at all? A vacuum energy a little larger and it would fly apart before stars formed; a Higgs coupling a little '
     'smaller and the vacuum would be unstable. Ours sits, by measurement, at the edge: metastable. Unlocks the Metastable Vacuum Lattice.',
     'metastable_vacuum_lattice', 11, -5, 180, ['cosmic_inflation', 'standard_model'], [('higgs_field_capacitor', 4)]),
    ('quantum_cosmology', 'Creation from Nothing',
     'Before inflation, before time: a closed universe can tunnel into existence from no space at all, or begin without a boundary, its time becoming space near the '
     'start like the South Pole of a sphere. Quantum cosmology writes the wave function of the whole universe. Unlocks the No-Boundary Seed.',
     'no_boundary_seed', 13, -6, 220, ['reheating', 'vacuum_stability', 'quantum_gravity'], [('spin_foam_lattice', 4)]),
    ('conformal_cyclic_cosmology', 'Conformal Cyclic Cosmology',
     'At the end of time only massless things remain, and massless things cannot tell an infinite distance from a finite one. Rescale the dead universe and its '
     'infinite future becomes the Big Bang of the next aeon. Where Finality ends, Origin begins: make a Universe Seed from the end of a universe.',
     'aeon_boundary_shard', 15, -7, 280, ['quantum_cosmology', 'heat_death', 'cosmogenesis'], [('de_sitter_horizon_shard', 2)]),
    # ---------------------------------------------------------------- FINALITY
    ('cosmic_eschatology', 'Cosmic Eschatology',
     'How does a universe end? Its fate is written in what fills it: matter decelerates the expansion, dark energy accelerates it, and the equation of state w '
     'decides whether it freezes, rips or collapses. The first study of Finality.',
     'cosmological_constant', 10, -8, 160, ['big_bang_cosmology', 'dark_energy'], [('dark_matter_lens', 4)]),
    ('proton_decay', 'Proton Decay',
     'Grand unified theories join quarks and leptons, so even a proton can change into a positron and a pion. Its lifetime is at least 10^34 years - and after '
     'the last star, the degenerate era ends when every proton is gone. Unlocks Proton Decay Residue.',
     'proton_decay_residue', 11, -9, 180, ['cosmic_eschatology', 'grand_unification'], [('superpartner_shard', 4)]),
    ('black_hole_era', 'The Black Hole Era',
     'After the protons, only black holes are left, slowly radiating. A black hole of a solar mass needs 10^67 years to evaporate, a supermassive one 10^100 - '
     'and then it ends in a flash. Unlocks the Last Flash of a Black Hole.',
     'evaporation_flash', 11, -8, 190, ['cosmic_eschatology', 'singularity_physics', 'holographic_principle'], [('hawking_emitter', 4)]),
    ('big_rip', 'The Big Rip',
     'If dark energy is phantom - w below -1 - its density grows as space expands. Clusters, then galaxies, then stars and planets, then atoms are torn apart, all '
     'in a finite time. Unlocks the Phantom Energy Cell.',
     'phantom_energy_cell', 12, -9, 190, ['cosmic_eschatology', 'mass_manipulation'], [('negative_mass_ingot', 4)]),
    ('big_crunch', 'The Big Crunch & the Big Bounce',
     'Enough matter, or dark energy that decays, and the expansion stops and reverses: everything falls back into a crunch. Quantum geometry may refuse the '
     'singularity - at the Planck density gravity repels, and the crunch bounces into a new expansion. Unlocks the Bounce Core.',
     'bounce_core', 12, -8, 200, ['cosmic_eschatology', 'compact_stars', 'quantum_gravity'], [('neutronium_block', 8)]),
    ('vacuum_decay', 'Vacuum Decay',
     'If our vacuum is only metastable, a bubble of the true vacuum may nucleate somewhere, someday. Its wall sweeps outwards at nearly the speed of light, and '
     'inside it the laws of physics are different. There would be no warning. Unlocks the True Vacuum Seed.',
     'true_vacuum_seed', 13, -9, 220, ['cosmic_eschatology', 'vacuum_stability'], [('metastable_vacuum_lattice', 2)]),
    ('heat_death', 'Heat Death',
     'The likeliest end of all: endless expansion to maximum entropy. Stars burn out, protons decay, black holes evaporate, and an empty de Sitter space is left, '
     'glowing at the temperature of its own horizon. Unlocks the Entropy Horizon.',
     'heat_death_ember', 13, -8, 240, ['proton_decay', 'black_hole_era'], [('evaporation_flash', 2)]),
    ('poincare_recurrence', 'Boltzmann Brains & Poincaré Recurrence',
     'A dead universe is not quite dead. In a finite system at equilibrium every state comes back, given time, and fluctuations out of equilibrium happen by pure '
     'chance - a star, a planet, a brain. Unlocks the Boltzmann Fluctuation.',
     'boltzmann_fluctuation', 14, -8, 260, ['heat_death', 'thermodynamics_of_information'], [('heat_death_ember', 4)]),
]
ORIGIN = [r[0] for r in RESEARCH[:8]]
FINALITY = [r[0] for r in RESEARCH[8:]]

# =================================================================================== PUZZLES (tools/puzzles2/group_branches.json)
PUZZLES = {
    'big_bang_cosmology': dict(
        goal='A homogeneous, isotropic universe is described by one scale factor a(t) that stretches every distance. Put such a space into general relativity: '
             'the square of the expansion rate H = (da/dt)/a is set by the density ρ of what fills it, by the curvature k of space, and by a vacuum energy Λ. '
             'Write the square of the expansion rate.',
        name='First Friedmann equation', lhs='H ^ 2',
        form='8 * π * G / 3 * ρ - k * c ^ 2 / a ^ 2 + Λ * c ^ 2 / 3',
        expr='100 * 3 * H ^ 2 / ( 8 * π * G )',
        ask='A flat universe (k = 0) without Λ has exactly the critical density ρ_c = 3H²/(8πG). With H in units of 10⁻¹⁸ s⁻¹ and G in 10⁻¹¹ SI units, '
            'find ρ_c in units of 10⁻²⁷ kg/m³ (about five protons per cubic metre).',
        vars={'H': 'Hubble expansion rate (10⁻¹⁸ s⁻¹)', 'G': 'gravitational constant (10⁻¹¹ m³ kg⁻¹ s⁻²)', 'ρ': 'mass density', 'k': 'spatial curvature (+1, 0, −1)',
              'c': 'speed of light', 'a': 'scale factor', 'Λ': 'cosmological constant'},
        given={'H': 2.2, 'G': 6.674}, unit='10⁻²⁷ kg/m³',
        method='Treat a comoving ball of dust: its kinetic energy per unit mass ½ȧ² minus the potential 4πGρa²/3 is constant, and that constant is the curvature. '
               'General relativity adds the vacuum term.',
        distractors=['p', 'ħ', '4']),
    'primordial_nucleosynthesis': dict(
        goal='One second after the beginning, neutrons and protons stop converting into each other when the weak rate falls below the expansion rate, at '
             'temperature T. Their ratio is frozen at the Boltzmann factor of their mass difference Q, and then free neutrons decay with lifetime τ for a time t '
             'until deuterium can survive. Nearly every remaining neutron ends up in helium-4. Write the mass fraction Y of helium.',
        name='Primordial helium abundance (neutron freeze-out)', lhs='Y',
        form='2 * exp ( - Q / ( kB * T ) - t / τ ) / ( 1 + exp ( - Q / ( kB * T ) - t / τ ) )',
        expr='2 * exp ( - Q / E - t / τ ) / ( 1 + exp ( - Q / E - t / τ ) )',
        ask='With Q = 1.293 MeV, freeze-out at k_BT = E, and the neutrons decaying for t seconds before nucleosynthesis, find Y.',
        vars={'Y': 'helium-4 mass fraction', 'Q': 'neutron-proton mass difference', 'kB': 'Boltzmann constant', 'T': 'freeze-out temperature',
              't': 'time until deuterium survives', 'τ': 'free neutron lifetime', 'E': 'freeze-out energy k_BT (MeV)'},
        given={'Q': 1.293, 'E': 0.75, 't': 180, 'τ': 880}, unit='',
        method='Each helium-4 nucleus holds two neutrons and two protons, so all the neutrons n make a helium mass fraction 2n/(n + p). Divide by p.',
        distractors=['ħ', 'ln', '4']),
    'cosmic_inflation': dict(
        goal='A scalar field φ with potential V(φ) dominates the universe and rolls so slowly that friction from the expansion balances the slope. Each step dφ '
             'then grows space by dN = H dt e-folds. Using H² = V/(3M_P²) and 3Hφ̇ = −dV/dφ, write the number of e-folds made while the field rolls from φ down '
             'to where it stops inflating, φ_e.',
        name='e-fold count of slow-roll inflation', lhs='N',
        form='1 / M _ P ^ 2 * int ( φ _ e , φ , V / d ( V , φ ) , φ )',
        expr='( φ ^ 2 - 2 ) / 4',
        ask='For V = ½m²φ² the integrand is φ/2, so N = (φ² − φ_e²)/(4M_P²). Inflation ends when ε = 1, at φ_e = √2 M_P. With M_P = 1, find N for a start at φ.',
        vars={'N': 'number of e-folds', 'M': 'reduced Planck mass', 'P': 'Planck', 'φ': 'inflaton field', 'e': 'end of inflation', 'V': 'inflaton potential'},
        given={'φ': 15}, unit='e-folds',
        method='Write dN = H dt = H dφ/φ̇ and replace φ̇ by the slow-roll equation; H² and V\' combine into V/V\'.',
        distractors=['H', 'exp', '3']),
    'reheating': dict(
        goal='At the end of inflation the inflaton decays at a rate λ, and its energy turns into a thermal bath of g relativistic species whose energy density is '
             'π²gT⁴/30. The decay completes when the expansion rate has fallen to the decay rate, with H² = ρ/(3M_P²). Write the temperature the universe '
             'is reheated to.',
        name='Reheating temperature', lhs='T',
        form='( 90 / ( π ^ 2 * g ) ) ^ ( 1 / 4 ) * sqrt ( λ * M _ P )',
        expr='( 90 / ( π ^ 2 * g ) ) ^ ( 1 / 4 ) * sqrt ( λ * M )',
        ask='With every Standard Model species in the bath (g = 106.75), a decay rate λ in GeV and M_P = M × 10¹⁸ GeV, find the reheating temperature in units of 10⁹ GeV.',
        vars={'T': 'reheating temperature', 'g': 'number of relativistic degrees of freedom', 'λ': 'inflaton decay rate', 'M': 'reduced Planck mass',
              'P': 'Planck'},
        given={'g': 106.75, 'λ': 1, 'M': 2.435}, unit='10⁹ GeV',
        method='Set H = λ, square it, replace H² by ρ/(3M_P²) and ρ by the radiation density, and solve for T.',
        distractors=['kB', '3', 'exp']),
    'leptogenesis': dict(
        goal='How rare are baryons? Count the photons: a black-body gas at temperature T has, per unit volume, an integral over energy of E² times the Bose-Einstein '
             'occupation 1/(e^{E/k_BT} − 1), times the density of states for two polarisations, 1/π² (units ħ = c = 1). Write the number density of photons.',
        name='Photon number density of black-body radiation (the baryon-to-photon ratio)', lhs='n _ γ',
        form='1 / π ^ 2 * int ( 0 , ∞ , E ^ 2 / ( exp ( E / ( kB * T ) ) - 1 ) , E )',
        expr='1000 * n / ( 20.29 * T ^ 3 )',
        ask='The integral gives n_γ = 2ζ(3)(k_BT)³/π² ≈ 20.29·T³ photons per cm³ (T in kelvin). Today T = 2.725 K. With n × 10⁻⁷ baryons per cm³, find the '
            'baryon-to-photon ratio η = n_B/n_γ in units of 10⁻¹⁰.',
        vars={'n': 'baryon number density (10⁻⁷ cm⁻³)', 'γ': 'photons', 'E': 'photon energy', 'kB': 'Boltzmann constant', 'T': 'temperature of the radiation'},
        given={'n': 2.5, 'T': 2.725}, unit='10⁻¹⁰',
        method='Integrate the occupation number over momentum space: d³p/(2π)³ times two polarisations is E²dE/π². The integral of x²/(eˣ − 1) is 2ζ(3).',
        distractors=['+', 'ħ', 'ln']),
    'vacuum_stability': dict(
        goal='The quartic coupling λ of the Higgs field is not a constant: it runs with the energy scale μ. The top quark, with Yukawa coupling y, dominates the '
             'running and drives λ down logarithmically from its value λ₀ at the top mass m. Write λ at scale μ in this top-dominated approximation.',
        name='Top-loop running of the scalar quartic coupling (renormalisation group)', lhs='λ',
        form='λ _ 0 - 3 * y ^ 4 / ( 8 * π ^ 2 ) * ln ( μ / m )',
        expr='m * exp ( 8 * π ^ 2 * λ / ( 3 * y ^ 4 ) ) / 1000',
        ask='The vacuum becomes unstable where λ(μ) = 0, at μ = m·exp(8π²λ₀/(3y⁴)). With λ₀ = λ, y and m (GeV) below, find that scale in TeV (with every other loop '
            'included it moves to about 10¹⁰ GeV - our vacuum is metastable).',
        vars={'λ': 'Higgs quartic coupling (at the top mass)', 'y': 'top Yukawa coupling', 'μ': 'renormalisation scale', 'm': 'top quark mass (GeV)'},
        given={'λ': 0.126, 'y': 0.94, 'm': 173}, unit='TeV',
        method='The top loop gives dλ/d ln μ = −3y⁴/(8π²). Hold y fixed and integrate from ln m to ln μ.',
        distractors=['exp', 'G', '+']),
    'quantum_cosmology': dict(
        goal='Quantise a closed universe with only one degree of freedom, its radius a, filled with vacuum energy Λ. The Hamiltonian constraint becomes a '
             'Schrödinger-like equation with zero energy for the wave function ψ(a) of the whole universe: a kinetic term from the curvature of ψ, minus a '
             'potential a² − (Λ/3)a⁴ that forms a barrier between a = 0 and the classical universe (units with the factor 3π/2G absorbed). Write it.',
        name='Wheeler–DeWitt equation (closed minisuperspace; tunnelling from nothing)', lhs='0',
        form='ħ ^ 2 * d2 ( ψ , a ) - ( a ^ 2 - Λ / 3 * a ^ 4 ) * ψ',
        expr='3 / ( 8 * G ^ 2 * ρ )',
        ask='Tunnelling through the barrier from a = 0 (from nothing) has probability P ∝ exp(−3/(8G²ρ_V)). With G = 1 and the vacuum energy density ρ in Planck '
            'units, find the exponent 3/(8G²ρ).',
        vars={'ħ': 'reduced Planck constant', 'ψ': 'wave function of the universe', 'a': 'scale factor (radius of the universe)', 'Λ': 'cosmological constant',
              'G': 'gravitational constant (Planck units)', 'ρ': 'vacuum energy density (Planck units)'},
        given={'G': 1, 'ρ': 0.01}, unit='',
        method='Write the Einstein-Hilbert action for a closed FRW universe, keep only a, form the Hamiltonian and replace the momentum by −iħ d/da. The WKB '
               'integral under the barrier gives the exponent.',
        distractors=['t', 'φ', '2']),
    'conformal_cyclic_cosmology': dict(
        goal='Light does not care about the size of space, only about its shape: it moves through conformal time, the integral of c dt divided by the scale factor '
             'a(t). In a universe that ends in pure vacuum energy, a(t) = a₀ e^{Ht} grows exponentially forever. Write how much conformal time is left from t₀ '
             'to the infinite future.',
        name='conformal time to the future boundary of de Sitter space', lhs='η',
        form='int ( t _ 0 , ∞ , c / ( a _ 0 * exp ( H * t ) ) , t )',
        expr='977.8 / ( a * H )',
        ask='The integral is finite: c e^{−Ht₀}/(a₀H). Measured from now (t₀ = 0, a₀ = a), and with the Hubble rate H in km/s/Mpc (1/H = 977.8/H Gyr), find the '
            'conformal time to the end of time, in Gyr.',
        vars={'η': 'conformal time left', 't': 'cosmic time', 'c': 'speed of light', 'a': 'scale factor', 'H': 'Hubble rate (km/s/Mpc)'},
        given={'a': 1, 'H': 67.7}, unit='Gyr',
        method='Change variable to u = e^{−Ht}; an exponential integrand gives an exponential back, and it vanishes at infinity.',
        distractors=['ρ', 'ln', '2']),
    'cosmic_eschatology': dict(
        goal='Whether the expansion of a universe speeds up or slows down depends on what fills it. Take the acceleration of the scale factor, d²a/dt², divided by '
             'the scale factor itself: ordinary matter and radiation of density ρ and pressure p pull it back, a vacuum energy Λ pushes it on. Write that relative acceleration.',
        name='Second Friedmann (acceleration) equation', lhs='d2 ( a , t ) / a',
        form='- 4 * π * G / 3 * ( ρ + 3 * p / c ^ 2 ) + Λ * c ^ 2 / 3',
        expr='Λ - Ω / 2',
        ask='In density parameters the deceleration parameter is q = Ω_m/2 − Ω_Λ (q < 0 means acceleration). With Ω_m = Ω and Ω_Λ = Λ below, find −q: how strongly '
            'our universe accelerates towards its end.',
        vars={'a': 'scale factor', 't': 'cosmic time', 'G': 'gravitational constant', 'ρ': 'density', 'p': 'pressure', 'c': 'speed of light',
              'Λ': 'cosmological constant (Ω_Λ in step 2)', 'Ω': 'matter density parameter'},
        given={'Λ': 0.685, 'Ω': 0.315}, unit='',
        method='Differentiate the first Friedmann equation in time and use energy conservation dρ/dt = −3H(ρ + p/c²).',
        distractors=['k', 'H', '2']),
    'proton_decay': dict(
        goal='In a theory that unifies the forces, a heavy boson of mass M_X can turn a quark into a lepton. A proton of mass m_p then decays by exchanging it; '
             'the rate is the square of the unified coupling α_G, times m_p⁵ by dimensional analysis, divided by M_X⁴, times a hadronic factor A. Write the '
             'proton lifetime (units ħ = c = 1).',
        name='lifetime of the proton in grand unification (dimension-six decay)', lhs='τ',
        form='1 / ( A * α _ G ^ 2 * m _ p ^ 5 / M _ X ^ 4 )',
        expr='208.56 * M ^ 4 / ( α ^ 2 * m ^ 5 )',
        ask='With A = 1, M_X = M × 10¹⁶ GeV, α_G = α and m_p in GeV (1 GeV⁻¹ = 6.58×10⁻²⁵ s), find τ in units of 10³⁰ years.',
        vars={'τ': 'proton lifetime', 'A': 'hadronic matrix-element factor', 'α': 'unified coupling', 'G': 'grand unified', 'm': 'proton mass (GeV)',
              'p': 'proton', 'M': 'X boson mass (10¹⁶ GeV)', 'X': 'X boson'},
        given={'M': 1, 'α': 0.025, 'm': 0.938}, unit='10³⁰ years',
        method='Like muon decay through a W: the amplitude carries 1/M_X², the rate its square, and m_p⁵ makes the dimensions of a rate.',
        distractors=['ħ', 'exp', 'c']),
    'black_hole_era': dict(
        goal='A black hole radiates as a black body at a temperature inversely proportional to its mass, so it loses mass at a rate dM/dt = −ħc⁴/(15360πG²M²) '
             'and gets brighter as it shrinks. Separate the variables and write the total time it needs to radiate away a mass M completely.',
        name='Hawking evaporation time', lhs='t',
        form='int ( 0 , M , 15360 * π * G ^ 2 * m ^ 2 / ( ħ * c ^ 4 ) , m )',
        expr='5120 * π * G ^ 2 * M ^ 3 / ( ħ * c ^ 4 ) / 3.156',
        ask='The integral is 5120πG²M³/(ħc⁴). For a primordial black hole of M × 10¹⁰ kg (G in 10⁻¹¹, ħ in 10⁻³⁴, c in 10⁸ SI units), find its lifetime in thousands of years.',
        vars={'t': 'evaporation time', 'M': 'initial mass (10¹⁰ kg)', 'm': 'mass while evaporating', 'G': 'gravitational constant', 'ħ': 'reduced Planck constant',
              'c': 'speed of light'},
        given={'M': 2, 'G': 6.674, 'ħ': 1.0546, 'c': 2.998}, unit='thousand years',
        method='dt = −dM/(dM/dt): put the mass-loss rate upside down and integrate from the full mass down to nothing.',
        distractors=['kB', 'T', '3']),
    'big_rip': dict(
        goal='Dark energy with equation of state w below −1 grows denser as space expands, so the scale factor reaches infinity at a finite time. In a flat '
             'universe of matter fraction Ω_m expanding at H₀ today, the remaining time depends on how far w lies below −1. Write the time left until '
             'everything is torn apart.',
        name='phantom-energy doomsday time (Big Rip)', lhs='t _ R',
        form='2 / ( 3 * abs ( 1 + w ) * H _ 0 * sqrt ( 1 - Ω _ m ) )',
        expr='2 * 977.8 / ( 3 * ( - 1 - w ) * H * sqrt ( 1 - Ω ) )',
        ask='With w, H₀ = H in km/s/Mpc (1/H₀ = 977.8/H Gyr) and Ω_m = Ω, find the time to the Rip in Gyr.',
        vars={'t': 'time', 'R': 'the Rip', 'w': 'dark energy equation of state', 'H': 'Hubble rate today (km/s/Mpc)', 'Ω': 'matter density parameter',
              'm': 'matter'},
        given={'w': -1.5, 'H': 70, 'Ω': 0.3}, unit='Gyr',
        method='Phantom energy dilutes as a^{−3(1+w)}, which grows. Once it dominates, H ∝ ρ^{1/2} blows up, and integrating da/(aH) gives a finite time.',
        distractors=['ρ', 'exp', '4']),
    'big_crunch': dict(
        goal='Quantum geometry modifies the expansion of the universe near the Planck scale: the square of the Hubble rate is the classical 8πGρ/3 multiplied '
             'by a factor that vanishes when the density ρ reaches a critical density, and the curvature k of a closed universe adds its usual term. Write '
             'the corrected expansion rate.',
        name='Effective Friedmann equation of loop quantum cosmology (the Big Bounce)', lhs='H ^ 2',
        form='8 * π * G / 3 * ρ * ( 1 - ρ / ρ _ c ) - k / a ^ 2',
        expr='sqrt ( 2 * π * G * ρ / 3 )',
        ask='At ρ = ρ_c the expansion stops and the collapse bounces. For k = 0 the expansion is fastest at ρ = ρ_c/2, where H² = 2πGρ_c/3. With G = 1 and '
            'ρ_c = ρ (Planck units), find the largest Hubble rate.',
        vars={'H': 'Hubble rate', 'G': 'gravitational constant', 'ρ': 'density (critical density in step 2)', 'c': 'critical (bounce)', 'k': 'spatial curvature',
              'a': 'scale factor'},
        given={'G': 1, 'ρ': 0.41}, unit='Planck units',
        method='The correction is quadratic in ρ: put x = ρ/ρ_c and find where x(1 − x) is largest.',
        distractors=['Λ', '2', 'exp']),
    'vacuum_decay': dict(
        goal='A spherical bubble of a lower-energy vacuum of radius R nucleates inside our vacuum. Its wall costs energy in proportion to its area, with tension σ; '
             'its interior releases energy in proportion to its volume, the energy-density difference ε. Write the energy of the bubble.',
        name='Thin-wall energy of a true-vacuum bubble', lhs='E',
        form='4 * π * R ^ 2 * σ - 4 / 3 * π * R ^ 3 * ε',
        expr='16 * π * σ ^ 3 / ( 3 * ε ^ 2 )',
        ask='Small bubbles collapse; above the critical radius R_c = 2σ/ε (where dE/dR = 0) they grow at the speed of light. Find the barrier height '
            'E(R_c) = 16πσ³/(3ε²) in natural units.',
        vars={'E': 'bubble energy', 'R': 'bubble radius', 'σ': 'wall tension', 'ε': 'vacuum energy difference'},
        given={'σ': 2, 'ε': 1.5}, unit='',
        method='Area 4πR², volume 4πR³/3. Set the derivative to zero to find the critical bubble.',
        distractors=['G', 'c', '2']),
    'heat_death': dict(
        goal='A universe that expands forever on vacuum energy approaches empty space with a cosmic horizon at distance c/H. That horizon has an entropy like a '
             'black hole\'s: Boltzmann\'s constant times its area A in Planck units, divided by four. Write the entropy of the horizon, with the area of a '
             'sphere of radius c/H written out.',
        name='de Sitter horizon entropy (the maximum entropy of a universe)', lhs='S',
        form='kB * c ^ 3 / ( 4 * G * ħ ) * 4 * π * ( c / H ) ^ 2',
        expr='π * c ^ 5 / ( G * ħ * H ^ 2 ) / 10',
        ask='That is S/k_B = πc⁵/(GħH²): the most entropy the universe can ever hold. With c in 10⁸, G in 10⁻¹¹, ħ in 10⁻³⁴ SI units and H in 10⁻¹⁸ s⁻¹, find '
            'S/k_B in units of 10¹²² (its horizon glows at ħH/(2πk_B) ≈ 3×10⁻³⁰ K).',
        vars={'S': 'horizon entropy', 'kB': 'Boltzmann constant', 'c': 'speed of light', 'G': 'gravitational constant', 'ħ': 'reduced Planck constant',
              'H': 'final Hubble rate'},
        given={'c': 2.998, 'G': 6.674, 'ħ': 1.0546, 'H': 2.2}, unit='10¹²²',
        method='The entropy of any horizon is k_B·A/(4ℓ_P²) with ℓ_P² = Għ/c³. The cosmic horizon is a sphere of radius c/H.',
        distractors=['T', 'ln', '2']),
    'poincare_recurrence': dict(
        goal='A small part of a universe at equilibrium, at temperature T, can fluctuate by chance into a state with a little more energy (by Δ_E) and a little '
             'less entropy (by Δ_S) than it should have. The probability falls off exponentially in the free energy the fluctuation needs, measured in units '
             'of k_BT. Write the probability.',
        name='Einstein–Boltzmann fluctuation formula', lhs='P',
        form='exp ( - ( Δ _ E - T * Δ _ S ) / ( kB * T ) )',
        expr='S / ln ( 10 )',
        ask='A brain-sized fluctuation that costs no energy but lowers the entropy by S × 10⁵⁰ k_B has 1/P = 10^X. Find X in units of 10⁵⁰. (A whole universe '
            'returns, by Poincaré recurrence, after about e^{10¹²²} years - far longer.)',
        vars={'P': 'probability', 'Δ': 'change', 'E': 'energy', 'S': 'entropy (10⁵⁰ k_B in step 2)', 'T': 'temperature', 'kB': 'Boltzmann constant'},
        given={'S': 1.2}, unit='10⁵⁰',
        method='Einstein\'s fluctuation formula: the probability of a state is e^{S/k_B}; for a subsystem in contact with a bath use the total entropy change, '
               'Δ_S − Δ_E/T.',
        distractors=['ħ', 'ln', '2']),
}

# =================================================================================== RECIPES
# (name, machine recipe type, item inputs, fluid inputs, item outputs, fluid outputs, energy, ticks, research, catalysts)
# Base amounts; costed at the machine's tier like round30_renew does, then left unscaled. Every input is renewable.
IC, EH = 'inflation', 'eschatology'
RECIPES = [
    # ---------------------------------------------------------------- ORIGIN
    ('bbn_from_quarks', 'nucleosynthesis', [('up_down_quarks', 4), ('neutronium_ingot', 2)], [('quark_gluon_plasma', 1000)],
     [('primordial_ash', 4)], [], 262144, 1200, 'primordial_nucleosynthesis', []),
    ('inflaton_condensation', IC, [('graviton_crystal', 1), ('dark_matter_lens', 1)], [('inflaton_field', 250), ('false_vacuum', 100)],
     [('inflaton_condensate', 2)], [], 16777216, 1200, 'cosmic_inflation', []),
    ('reheating', IC, [('inflaton_condensate', 1)], [('quark_gluon_plasma', 50)],
     [], [('reheating_plasma', 64000)], 16777216, 800, 'reheating', []),
    ('bbn_from_reheating', 'nucleosynthesis', [('neutronium_ingot', 1)], [('reheating_plasma', 1000)],
     [('primordial_ash', 8)], [], 262144, 1200, 'reheating', []),
    ('leptogenesis', 'collider', [('antimatter_pellet', 2), ('sterile_neutrino', 1)], [('reheating_plasma', 1000)],
     [('baryon_asymmetry_crystal', 1)], [], 262144, 800, 'leptogenesis', []),
    ('metastable_vacuum_mapping', 'mass_modulation', [('higgs_field_capacitor', 2), ('magnetic_monopole', 1)], [('false_vacuum', 250)],
     [('metastable_vacuum_lattice', 1)], [], 262144, 900, 'vacuum_stability', []),
    ('no_boundary_tunnelling', IC, [('spin_foam_lattice', 1), ('inflaton_condensate', 2), ('metastable_vacuum_lattice', 1)], [('reheating_plasma', 100)],
     [('no_boundary_seed', 1)], [], 33554432, 1600, 'quantum_cosmology', []),
    ('aeon_boundary', IC, [('heat_death_ember', 2), ('de_sitter_horizon_shard', 1)], [('dark_era_haze', 100)],
     [('aeon_boundary_shard', 1)], [], 33554432, 1600, 'conformal_cyclic_cosmology', []),
    ('aeon_rebirth', IC, [('aeon_boundary_shard', 1), ('no_boundary_seed', 1), ('inflaton_condensate', 2)], [('reheating_plasma', 100)],
     [('universe_seed', 2)], [], 67108864, 2400, 'conformal_cyclic_cosmology', []),
    ('bounce_rebirth', IC, [('bounce_core', 1), ('baryon_asymmetry_crystal', 1), ('inflaton_condensate', 2)], [('reheating_plasma', 100)],
     [('universe_seed', 1)], [], 67108864, 2400, 'conformal_cyclic_cosmology', []),
    # ---------------------------------------------------------------- FINALITY
    ('proton_decay_watch', 'neutrino_capture', [('cherenkov_quartz_block', 2), ('neutronium_block', 1)], [('minecraft:water', 4000)],
     [('proton_decay_residue', 2)], [], 16384, 600, 'proton_decay', []),
    ('black_hole_final_flash', 'kugelblitz', [('micro_singularity', 1), ('hawking_absorber_plate', 1)], [],
     [('evaporation_flash', 2)], [('hawking_radiation', 1000)], 524288, 1200, 'black_hole_era', []),
    ('phantom_energy', 'dark_energy_extraction', [('negative_mass_ingot', 1), ('exotic_matter_crystal', 1)], [('dark_energy', 100)],
     [('phantom_energy_cell', 1)], [], 2097152, 1600, 'big_rip', []),
    ('big_crunch', 'stellar_compression', [('spin_foam_lattice', 1), ('degenerate_matter_chunk', 4), ('neutronium_block', 1)], [],
     [('bounce_core', 1)], [], 1048576, 1400, 'big_crunch', []),
    ('true_vacuum_nucleation', 'mass_modulation', [('metastable_vacuum_lattice', 1), ('exotic_matter_crystal', 1)], [('false_vacuum', 500)],
     [('true_vacuum_seed', 1)], [], 262144, 900, 'vacuum_decay', []),
    ('degenerate_era', EH, [('proton_decay_residue', 2), ('evaporation_flash', 1)], [('superfluid_helium', 100)],
     [('heat_death_ember', 2)], [('dark_era_haze', 32000)], 16777216, 1600, 'heat_death', []),
    ('de_sitter_horizon', EH, [('heat_death_ember', 1), ('dark_matter_lens', 1)], [('dark_energy', 100)],
     [('de_sitter_horizon_shard', 1)], [], 16777216, 1600, 'heat_death', []),
    ('heat_death_remnant', EH, [('heat_death_ember', 4), ('de_sitter_horizon_shard', 1)], [('dark_era_haze', 100)],
     [('heat_death_remnant', 2)], [], 16777216, 2000, 'heat_death', []),
    ('boltzmann_fluctuation', EH, [('de_sitter_horizon_shard', 1), ('negentropy_cell', 2)], [('dark_era_haze', 250)],
     [('boltzmann_fluctuation', 1)], [], 33554432, 2400, 'poincare_recurrence', []),
    # ---------------------------------------------------------------- the branches feeding what came before
    ('boltzmann_brain_from_fluctuation', 'psionic_attunement', [('boltzmann_fluctuation', 1), ('neural_organoid', 1)], [('psionic_ether', 250)],
     [('boltzmann_brain', 2)], [], 524288, 1200, 'poincare_recurrence', []),
    ('lambda_from_horizon', 'dark_energy_extraction', [('de_sitter_horizon_shard', 1), ('phantom_energy_cell', 1)], [('dark_energy', 100)],
     [('cosmological_constant', 1)], [], 2097152, 1600, 'heat_death', []),
    ('pocket_universe_from_first_principles', 'universe_forging',
     [('universe_seed', 1), ('true_vacuum_seed', 1), ('baryon_asymmetry_crystal', 1), ('primordial_ash', 2), ('inflaton_condensate', 1)],
     [('reheating_plasma', 1000), ('inflaton_field', 1000)],
     [('pocket_universe', 1)], [], 33554432, 9600, 'cosmogenesis', ['spacetime_catalyst']),
    ('baryogenesis_from_asymmetry', 'mirror_synthesis', [('baryon_asymmetry_crystal', 2), ('antihelium_nucleus', 1)], [('reheating_plasma', 500)],
     [('baryogenesis_seed', 1)], [], 67108864, 2100, 'baryon_asymmetry', []),
]
RECIPE_RESEARCH_TYPES = {IC: 'inflation_chamber', EH: 'entropy_horizon'}

# later recipes that take the branches' materials as an additional input: (recipe path, item, count)
ADDITIONAL = [
    ('horizon_simulation/simulate_cosmology', 'primordial_ash', 4),
    ('omnilex_simulation/grow_bubble_universes', 'no_boundary_seed', 1),
]

LANG = {}


def _write_research(g):
    w, D, q = g['w'], g['D'], g['q']
    for rid, t, desc, icon, x, y, ins, req, cost in RESEARCH:
        w(os.path.join(D, 'ts_research', rid + '.json'), {
            'title': t, 'description': desc, 'icon': q(icon), 'x': x, 'y': y, 'insight': ins,
            'requires': [q(r) for r in req], 'cost': [{'item': q(c), 'count': n} for c, n in cost],
            # (round 5's study time, which ran before this round: 0.6·insight^1.35 seconds, in half minutes)
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
    p = os.path.join(HERE, 'puzzles2', 'group_branches.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    T = json.load(open(os.path.join(HERE, 'recipe_tuning.json')))
    cost = {int(k): v for k, v in T['cost_factor'].items()}
    fcost = {int(k): v for k, v in T.get('fluid_factor', T['cost_factor']).items()}
    dur = {int(k): v for k, v in T.get('duration_factor', {}).items()}
    item_cap = int(T.get('item_cap', 1024))
    by = {m['recipe']: m for m in g['M']}
    for name, typ, ins, fins, outs, fouts, fe, t, res, cats in RECIPES:
        m = by[typ]
        tier = g['TIER'].get(m['id'], 1)
        tank = int(T.get('fluid_cap_multiblock' if m['multiblock'] else 'fluid_cap_single', 2_048_000 if m['multiblock'] else 256_000))
        assert len(ins) <= m['inputs'] and len(fins) <= m['fluid_inputs'] and len(outs) <= m['outputs'] and len(fouts) <= m['fluid_outputs'], name
        assert len({i for i, _ in ins}) == len(ins) and len({f for f, _ in fins}) == len(fins), name
        ins_ = [(i, min(item_cap, n * cost.get(tier, 1))) for i, n in ins]
        fins_ = [(f, min(tank, n * fcost.get(tier, 1))) for f, n in fins]
        g['mr']('r32_' + name, typ, ins_, fins_, outs, fouts, energy=min(fe, 2147483647), duration=int(t * dur.get(tier, 1.0)), research=res,
                catalysts=tuple(cats))
        p = os.path.join(g['REC'], typ, 'r32_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        g['w'](p, r)


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)


def _additional(g):
    by = {m['recipe']: m for m in g['M']}
    for path, item, n in ADDITIONAL:
        f = os.path.join(g['REC'], path + '.json')
        if not os.path.exists(f):
            print('round32_branches: no recipe', path)
            continue
        r = json.load(open(f))
        ins = r.setdefault('item_inputs', [])
        if any(i.get('item') == g['q'](item) for i in ins):
            continue
        if len(ins) >= by[path.split('/')[0]]['inputs']:
            print('round32_branches: no free slot in', path)
            continue
        ins.append({'item': g['q'](item), 'count': n})
        g['w'](f, r)


def finish(g):
    _additional(g)
    te = g['text_entry']
    # ---------------------------------------------------------------- ORIGIN (Strings & the Multiverse)
    te('strings', 'origin_branch', 'Origin: How a Universe Begins', 'cosmic_record_fragment', [
        'Two branches of physics are about time itself: $(thing)Origin$(), how a universe begins, and $(thing)Finality$(), how it ends. Origin starts with $(thing)The Hot Big Bang$(): '
        'run the expansion backwards and everything was once hotter and denser than the core of a star.',
        'The $(thing)Friedmann equation$() says how fast space expands: H² = 8πGρ/3 − kc²/a² + Λc²/3. Matter and radiation slow it, curvature bends it, and the vacuum '
        'energy Λ - dark energy - takes over in the end. Exactly flat space has the critical density, about five protons per cubic metre.',
        '$(li)$(thing)Primordial Nucleosynthesis$(): for three minutes the universe fused hydrogen into helium. $(item)Primordial Nucleosynthesis Ash$() comes from quark-gluon '
        'plasma (or reheating plasma) in the $(item)Nucleosynthesis Chamber$().$(li)$(thing)Cosmic Inflation$() and $(thing)Reheating$(): see the next chapter.'
        '$(li)$(thing)Leptogenesis$(), $(thing)Vacuum Stability$(), $(thing)Creation from Nothing$() and $(thing)Conformal Cyclic Cosmology$() come after.'],
        adv='big_bang_cosmology', sort=40, spot=['primordial_ash'])
    te('strings', 'origin_inflation', 'Inflation & Reheating', 'inflaton_condensate', [
        'The Big Bang alone cannot explain why opposite sides of the sky have the same temperature to one part in 100,000: they were never in contact. $(thing)Inflation$() '
        'does: before the hot Big Bang, a scalar field - the inflaton - sat high on its potential and made space grow exponentially, sixty e-folds in 10⁻³² s.',
        'Build the $(item)Inflation Chamber$(). It condenses the $(fluid)Inflaton Field$() and $(fluid)False Vacuum$() into $(item)Inflaton Condensate$(). In its Operation console '
        'keep the slow-roll ε = 2/φ² below 1 and N = (φ₀² − 2)/4 above 50 - but start the field too high and inflation turns $(thing)eternal$(): quantum jumps outrun the roll, '
        'and the bubble never ends (in our universe, other bubbles still may).',
        '$(thing)Reheating$(): at the bottom of its potential the inflaton oscillates and decays. The chamber turns Condensate and a little quark-gluon plasma into $(fluid)Reheating '
        'Plasma$() - every particle there is, at 10⁹ GeV and more. That plasma is the raw stuff of every other Origin material.'],
        adv='cosmic_inflation', sort=41, spot=['inflation_chamber', 'inflaton_condensate'])
    te('strings', 'origin_matter', 'Why There Is Anything', 'baryon_asymmetry_crystal', [
        'Reheating makes matter and antimatter in equal amounts, and they should have annihilated into light. For every billion photons there is one baryon left over. Andrei '
        'Sakharov\'s conditions say what it takes: baryon number violation, C and CP violation, and a moment out of equilibrium.',
        '$(thing)Leptogenesis$() meets all three: heavy right-handed neutrinos decay a little more often into leptons than antileptons, and sphaleron transitions of the '
        'electroweak vacuum convert that excess into baryons. In a $(item)Quark-Gluon Collider$(), antimatter and sterile neutrinos in $(fluid)Reheating Plasma$() leave a '
        '$(item)Baryon Asymmetry Crystal$(). With the antimatter of the Antiverse, crystals become a $(item)Baryogenesis Seed$() in the $(item)Mirror Foundry$().'],
        adv='leptogenesis', sort=42, spot=['baryon_asymmetry_crystal'])
    te('strings', 'origin_nothing', 'What Could Stop a Universe', 'no_boundary_seed', [
        '$(thing)Vacuum Stability$(): a universe can fail before it starts. A vacuum energy larger by a factor of a hundred and no galaxy forms; the measured one is 10¹²⁰ times '
        'smaller than quantum theory\'s natural guess - the $(thing)cosmological constant problem$(). And the Higgs coupling runs downwards with energy until, near 10¹⁰ GeV, '
        'a deeper vacuum opens: ours is $(thing)metastable$(). The $(item)Higgs Field Modulator$() maps it into a $(item)Metastable Vacuum Lattice$().',
        '$(thing)Creation from Nothing$(): quantise a whole closed universe and its wave function obeys the Wheeler-DeWitt equation. Vilenkin\'s universe tunnels through a barrier '
        'from no space at all; Hartle and Hawking\'s has no boundary - near the start time turns into space, and asking what came before is like asking what is south of the '
        'South Pole.',
        'The $(item)Inflation Chamber$() folds spin foam, inflaton condensate and a metastable vacuum into a $(item)No-Boundary Seed$().'],
        adv='vacuum_stability', sort=43, spot=['metastable_vacuum_lattice', 'no_boundary_seed'])
    te('strings', 'origin_ccc', 'Conformal Cyclic Cosmology', 'aeon_boundary_shard', [
        'Where Finality ends, Origin begins. Roger Penrose noticed that at the end of a heat death only massless particles remain - and light has no clock: it cannot tell an '
        'infinitely large, cold universe from a tiny, hot one. Rescale the dead universe\'s infinite future and it is the Big Bang of the next $(thing)aeon$().',
        'In the $(item)Inflation Chamber$(): $(item)Heat Death Embers$(), a $(item)De Sitter Horizon Shard$() and $(fluid)Dark Era Haze$() make an $(item)Aeon Boundary Shard$(); the '
        'Shard with a $(item)No-Boundary Seed$() and inflaton condensate makes two $(item)Universe Seeds$(). A $(item)Bounce Core$() from a Big Crunch makes one too.',
        'Origin also feeds what came before it: the $(item)Universe Forge$() can make a $(item)Pocket Universe$() from first principles - a seed, a true-vacuum bubble, the '
        'asymmetry, the ash and the plasma - and the Eye of the Horizon\'s and the Omnilex\'s universes now want Primordial Ash and a No-Boundary Seed.'],
        adv='conformal_cyclic_cosmology', sort=44, spot=['aeon_boundary_shard', 'universe_seed'])
    # ---------------------------------------------------------------- FINALITY (Wormholes & Causality)
    te('spacetime', 'finality_branch', 'Finality: How a Universe Ends', 'cosmological_constant', [
        'The second branch of time is $(thing)Finality$(). $(thing)Cosmic Eschatology$() starts from the acceleration equation, ä/a = −4πG(ρ + 3p/c²)/3 + Λc²/3: matter and '
        'radiation decelerate the expansion, dark energy accelerates it - and ours is accelerating (q₀ ≈ −0.53).',
        'What happens next depends on dark energy\'s equation of state w = p/ρc²:$(li)w = −1 (a cosmological constant): expansion forever, to the $(thing)Heat Death$().'
        '$(li)w < −1 (phantom energy): the $(thing)Big Rip$().$(li)decaying dark energy or enough matter: the $(thing)Big Crunch$() - and perhaps a $(thing)Bounce$().'
        '$(li)at any moment, if the vacuum is metastable: $(thing)Vacuum Decay$().'],
        adv='cosmic_eschatology', sort=40, spot=['cosmological_constant'])
    te('spacetime', 'finality_eras', 'The Long Dark: Protons and Black Holes', 'evaporation_flash', [
        'If the universe expands forever, its history after the stars falls into eras. The last red dwarfs die after 10¹⁴ years; then the $(thing)Degenerate Era$() of white '
        'dwarfs, neutron stars and planets - until, if grand unification is right, $(thing)Proton Decay$() dissolves them: p → e⁺ + π⁰, with a lifetime above 10³⁴ years.',
        'Watch a tank of water long enough, like Super-Kamiokande: in the $(item)Neutrino Condenser$(), Cherenkov quartz and neutronium under 4,000 mB of water catch '
        '$(item)Proton Decay Residue$().',
        'Then the $(thing)Black Hole Era$(): only black holes remain, radiating at T = ħc³/(8πGMk_B). A solar-mass hole takes 10⁶⁷ years to evaporate, getting hotter as it '
        'shrinks, and ends in a flash. A $(item)Kugelblitz Chamber$() evaporates a micro singularity down to its $(item)Last Flash$().'],
        adv='proton_decay', sort=41, spot=['proton_decay_residue', 'evaporation_flash'])
    te('spacetime', 'finality_violent', 'Rip, Crunch, Bounce and Decay', 'phantom_energy_cell', [
        '$(thing)The Big Rip$(): phantom dark energy (w < −1) grows denser as space grows. With w = −1.5 the Rip comes 22 billion years from now: clusters go a billion years '
        'before, the Solar System three months before, atoms in the last instant. The $(item)Dark Energy Siphon$() bottles it in a $(item)Phantom Energy Cell$().',
        '$(thing)The Big Crunch$(): expansion reverses and everything falls together. In loop quantum cosmology the Friedmann equation gains a factor (1 − ρ/ρ_c): at the Planck '
        'density gravity repels and the collapse $(thing)bounces$(). The $(item)Neutron Star Forge$() crushes spin foam and degenerate matter into a $(item)Bounce Core$().',
        '$(thing)Vacuum Decay$(): a bubble of the true vacuum needs a radius 2σ/ε to grow; bigger, it expands at nearly the speed of light, and inside it chemistry is '
        'impossible. The $(item)Higgs Field Modulator$() holds a $(item)True Vacuum Seed$() just below the critical radius. Please keep it there.'],
        adv='big_rip', sort=42, spot=['phantom_energy_cell', 'bounce_core', 'true_vacuum_seed'])
    te('spacetime', 'finality_heat_death', 'Heat Death', 'heat_death_ember', [
        'The likeliest end: maximum entropy. After the black holes, an empty de Sitter space is left, with a horizon c/H away that glows at ħH/(2πk_B) ≈ 3×10⁻³⁰ K and holds '
        'S = πc⁵k_B/(GħH²) ≈ 2×10¹²² - the most entropy there can ever be. Nothing more can happen, because nothing can get colder than its surroundings.',
        'Build the $(item)Entropy Horizon$(). It runs matter forward to the end: proton decay residue and a black hole\'s last flash become $(item)Heat Death Embers$() and '
        '$(fluid)Dark Era Haze$() - photons, neutrinos and positronium atoms wider than today\'s observable universe. Embers under dark energy give $(item)De Sitter Horizon Shards$(), '
        'and embers with a shard give $(item)Heat Death Remnants$().',
        'In its Operation console the simulated age must pass 10³⁴ years (or the protons have not decayed); a faster expansion makes a smaller, hotter horizon - too hot, and it '
        'boils the containment. Horizon shards and phantom cells also make a $(item)Cosmological Constant$() in the Siphon.'],
        adv='heat_death', sort=43, spot=['entropy_horizon', 'heat_death_ember', 'de_sitter_horizon_shard'])
    te('spacetime', 'finality_after', 'After a Universe Dies', 'boltzmann_fluctuation', [
        'Is a dead universe dead forever? Statistical mechanics says no. In a finite system every state recurs (Poincaré recurrence) - for a de Sitter universe after about '
        'e^{10¹²²} years. Long before that, fluctuations happen by chance: P ∝ exp(−(Δ_E − TΔ_S)/k_BT). A single brain is far more likely than a whole world - the $(thing)Boltzmann '
        'brain$() problem.',
        'The $(item)Entropy Horizon$() waits for one: a horizon shard and negentropy in the haze give a $(item)Boltzmann Fluctuation$() - which a Psionic Resonator turns into two '
        '$(item)Boltzmann Brains$().',
        'And the other way out is $(thing)Conformal Cyclic Cosmology$() (in Strings & the Multiverse): the end of one aeon is the beginning of the next.'],
        adv='poincare_recurrence', sort=44, spot=['boltzmann_fluctuation'])
