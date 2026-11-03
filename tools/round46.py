"""
Round 46 (1.5.0, "Clear Sight"): Optics & Crystals - a branch of physics joined with chemistry.

Crystals from the Planetary Interior (and from the Overworld) are studied, grown pure and cut into optics for the
telescopes and beyond - the real chain of a lens:

* X-ray Diffractometer (crystallography): Bragg's law, nλ = 2d·sin θ. Set the copper tube above its K-edge and the
  detector angle to the crystal's own lattice spacing and the reflection appears: a Laue Pattern that records how the
  crystal is oriented, so it can be cut along the right planes. Every study gives Insight. Glass has no lattice - fused
  silica needs no pattern.
* Crystal Growth Furnace (single-crystal growth): a Czochralski puller with an iridium crucible. The melt's superheat
  sets the gradient at the growth front; pull rate over gradient (Voronkov's ratio) decides whether the boule grows free
  of defects, and slow pulling with good stirring leaves the impurities behind in the melt (Burton-Prim-Slichter).
* Optical Workshop (geometric optics): grinding and polishing. The surfaces' curvatures set the lens power by the
  lensmaker's equation, P = (n − 1)(C₁ − C₂); polishing time brings the surface error down below λ/4 (λ/8, λ/16 for
  correctors and gratings).

The optics feed the telescopes (a Schmidt corrector for wide surveys, an echelle grating for rich spectra, a fluorite
apochromat for planets), the Radio Telescope's cold receiver (sapphire windows cut from a boule) and, as lithography lenses
are really made of fluorite and fused silica, a second route to the Photonic Lens.
"""
import json
import math
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
# (colours follow what each thing is made of)
ITEMS = [
    _I('sapphire_boule', 'Sapphire Boule', 'crystal', '#3050c0', '#a0c0ff',
       'A single crystal of aluminium oxide pulled from the melt, every atom of it in one lattice. Cut, it gives windows clear from ultraviolet to '
       'infrared.'),
    _I('fluorite_boule', 'Fluorite Boule', 'crystal', '#402060', '#c080ff',
       'Calcium fluoride grown as one crystal. It bends colours apart less than any glass, so lenses of it bring every colour to one focus.'),
    _I('garnet_boule', 'Garnet Boule', 'crystal', '#801828', '#ff6070',
       'A synthetic garnet crystal, grown flawless. Garnets doped with rare earths are the heart of the most common solid-state lasers.'),
    _I('laue_pattern', 'Laue Pattern', 'plate', '#101418', '#e0e8f0',
       'A photographic plate exposed to X-rays scattered by a crystal: a pattern of spots that shows how its lattice planes lie. Cut along them.'),
    _I('fused_silica_lens', 'Fused Silica Lens', 'lens', '#c8d8e0', '#ffffff',
       'A lens of fused silica, ground and polished to a quarter of a wavelength. Glass has no lattice: it needs no orientation to cut.'),
    _I('fluorite_lens', 'Fluorite Lens', 'lens', '#402060', '#c080ff',
       'A lens cut from a fluorite boule along its lattice. Paired with glass it cancels the colour error glass alone cannot.'),
    _I('moissanite_lens', 'Moissanite Lens', 'lens', '#2a5060', '#e0ffff',
       'Silicon carbide bends light more than diamond does: a moissanite lens is strong for its thickness, and it fires rainbows.'),
    _I('schmidt_corrector', 'Schmidt Corrector Plate', 'lens', '#b8c8d8', '#ffffff',
       'A thin, almost flat plate of fused silica, figured to a gentle aspheric curve. In front of a spherical mirror it gives a sharp image across '
       'a field many degrees wide.'),
    _I('echelle_grating', 'Echelle Grating', 'plate', '#808890', '#e8f0ff',
       'A few dozen grooves to the millimetre ruled steep in an aluminised silica blank, used at high orders: it spreads a star\'s light into a '
       'spectrum a hundred times finer than a prism\'s.'),
    _I('apochromatic_objective', 'Apochromatic Objective', 'lens', '#6a5080', '#e0d8ff',
       'Fluorite between two lenses of fused silica: three colours brought to exactly one focus, and the rest within a hair of it.'),
]
BLOCKS = [
    # the X-ray Diffractometer
    _B('xray_cabinet', 'X-ray Cabinet', 'casing', '#4a5060', '#a0a8b8', light=0),
    _B('leaded_glass', 'Leaded Glass', 'fieldglass', '#606878', '#c8d0e0', kind='glass', light=0, hardness=2.0),
    _B('xray_tube', 'X-ray Tube', 'injector', '#8a5030', '#ffd080', light=6),
    _B('goniometer', 'Goniometer', 'dipole', '#6a6e72', '#e0e0e0', light=2),
    # the Crystal Growth Furnace
    _B('growth_furnace_casing', 'Growth Furnace Casing', 'magcasing', '#5a4a44', '#c08060', light=0),
    _B('iridium_crucible', 'Iridium Crucible', 'cryo', '#50504c', '#e0e0d8', light=8),
    _B('pull_shaft', 'Pull Shaft', 'beampipe', '#606468', '#c0c4c8', light=0),
    # the Optical Workshop
    _B('optical_bench', 'Optical Bench', 'casing', '#707478', '#b8bcc0', light=0),
    _B('lens_grinder', 'Lens Grinder', 'coilblock', '#6a6058', '#c8b8a0', light=0),
    _B('interferometer', 'Interferometer', 'rfcavity', '#405070', '#c0d8ff', light=4),
]
FLUIDS = []

KEY_ADD = {
    'ξ': ['xray_cabinet'], 'ο': ['xray_cabinet', 'interface_port'], 'ρ': ['leaded_glass'], 'χ': ['xray_tube'], 'γ': ['goniometer'],
    'ν': ['growth_furnace_casing'], 'ι': ['growth_furnace_casing', 'interface_port'], 'ζ': ['iridium_crucible'], 'υ': ['pull_shaft'],
    'Ξ': ['optical_bench'], 'Π': ['optical_bench', 'interface_port'], 'Γ': ['lens_grinder'], 'Φ': ['interferometer'],
}


# =================================================================================== SHAPES
def x_ray_diffractometer():
    """A lead cabinet with leaded-glass doors; inside, the X-ray tube on one side, the goniometer circle standing upright
    round the sample in the middle, and the detector arm on the other side."""
    s = shapes.Shape(13, 8, 7)
    s.box(0, 0, 0, 8, 6, 6, 'ξ', hollow=True)
    s.box(2, 7, 1, 6, 7, 5, 'ξ')                                        # the extraction hood
    s.box(10, 0, 1, 12, 4, 4, 'ξ')                                      # the high-voltage generator, beside the cabinet
    for x in (9, 10):
        s.put(x, 4, 3, 'ξ')                                             # its cable to the tube
    s.put(11, 5, 2, 'χ')                                                # the generator's own valve
    s.air_box(1, 1, 1, 7, 5, 5)
    for x in range(2, 7):
        for y in (2, 3, 4):
            s.put(x, y, 0, 'ρ')                                         # the doors
    for y in (2, 3, 4):
        s.put(0, y, 3, 'ρ'); s.put(8, y, 3, 'ρ')
    s.put(1, 3, 3, 'χ')                                                 # the tube
    for a in range(16):                                                 # the goniometer circle, upright, round the sample
        t = a * math.pi / 8
        s.put(round(4 + 2 * math.cos(t)), round(3 + 2 * math.sin(t)), 3, 'γ')
    s.put(4, 1, 3, 'γ')
    s.put(7, 3, 4, 'γ')                                                 # the detector arm
    s.center = (4, 3, 3)
    s.ports('ξ', 'ο', 4, y=0)
    s.v[(4, 1, 0)] = 'C'
    s.ctrl = (4, 1, 0)
    return s


def crystal_growth_furnace():
    """A Czochralski puller: a squat furnace chamber with a viewport, the crucible in a ring of heaters, and a tall
    tower above it down which the pull shaft lowers the seed and draws the boule up."""
    s = shapes.Shape(7, 14, 9)
    c = 3
    s.box(0, 0, 0, 6, 0, 6, 'ν')
    s.box(0, 1, 0, 6, 5, 6, 'ν', hollow=True)                           # the chamber
    s.air_box(1, 1, 1, 5, 4, 5)
    for x in (2, 3, 4):
        for y in (2, 3):
            s.put(x, y, 0, 'ρ')                                         # viewport
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)):
        s.put(c + dx, 1, c + dz, '#')                                   # heaters round the crucible
    s.put(c, 1, c, 'ζ')                                                 # the crucible
    s.box(2, 6, 2, 4, 12, 4, 'ν', hollow=True)                          # the pull tower
    for y in range(2, 14):
        s.put(c, y, c, 'υ' if y >= 3 else '.')                          # the pull shaft
    s.put(c, 13, c, 'ν')
    s.box(2, 13, 2, 4, 13, 4, 'ν')
    s.put(c, 13, c, 'υ')
    for x in (1, 5):
        s.box(x, 0, 8, x, 4, 8, 'υ')                                    # argon cylinders: the melt grows under inert gas
        s.put(x, 4, 7, 'υ')                                             # and their lines into the chamber
    s.center = (c, 2.2, c)
    s.ports('ν', 'ι', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


def optical_workshop():
    """A long stone optical bench: the grinding spindle and polishing laps at one end, the interferometer tower at the
    other, where every surface is tested against a reference flat."""
    s = shapes.Shape(13, 7, 5)
    s.box(0, 0, 0, 12, 0, 4, 'Ξ')
    s.box(0, 1, 1, 12, 1, 3, 'Ξ')                                       # the bench top
    for x in (0, 12):
        for z in (0, 4):
            s.put(x, 1, z, 'Ξ')                                         # its feet
    for x in (2, 4):
        s.put(x, 2, 2, 'Γ')                                             # grinding spindle and polishing lap
        s.put(x, 3, 2, 'Γ')
    s.column(10, 2, 2, 5, 'Ξ')                                          # the interferometer tower
    s.put(10, 6, 2, 'Φ')
    s.put(9, 5, 2, 'Φ')
    s.put(8, 2, 2, 'Φ')                                                 # the test stand
    s.center = (8.0, 3.2, 2.0)
    s.ports('Ξ', 'Π', 4, y=0)
    s.v[(6, 1, 0)] = 'C'
    s.ctrl = (6, 1, 0)
    return s


shapes.BUILDERS.update({'x_ray_diffractometer': x_ray_diffractometer, 'crystal_growth_furnace': crystal_growth_furnace,
                        'optical_workshop': optical_workshop})

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'x_ray_diffractometer': ('X-ray Diffractometer',
                             'X-rays scattered by a crystal\'s planes of atoms reinforce only at angles that fit the spacing between them. Find the '
                             'angle, and you know the lattice - and how to cut it.'),
    'crystal_growth_furnace': ('Crystal Growth Furnace',
                               'A seed is dipped into the melt and drawn slowly up, turning: the melt freezes onto it as one single crystal, and '
                               'the impurities stay behind.'),
    'optical_workshop': ('Optical Workshop',
                         'Grinding spindles, polishing laps and an interferometer: blanks become lenses, plates and gratings, true to a fraction '
                         'of a wavelength.'),
}
MACHINE_CAT = {k: 'optics' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'x_ray_diffractometer': 'crystallography', 'crystal_growth_furnace': 'crystal_growth_physics',
                    'optical_workshop': 'geometric_optics'}
TIER = {k: 2 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'x_ray_diffractometer': (['GTG', 'XSX', 'LCL'], {'G': 'goniometer', 'T': 'xray_tube', 'X': 'xray_cabinet', 'S': _S, 'L': 'leaded_glass',
                                                     'C': 'photographic_plate'}),
    'crystal_growth_furnace': (['PPP', 'FSF', 'CRC'], {'P': 'pull_shaft', 'F': 'growth_furnace_casing', 'S': _S, 'C': 'refractory_firebox',
                                                       'R': 'iridium_crucible'}),
    'optical_workshop': (['IGI', 'BSB', 'BBB'], {'I': 'interferometer', 'G': 'lens_grinder', 'B': 'optical_bench', 'S': _S}),
}
EXTRA = {
    'x_ray_diffractometer': 'Set the tube voltage above copper\'s K-edge (8.98 kV) so it gives its Kα line (λ = 1.5406 Å), then the detector angle '
                            '2θ so that nλ = 2d·sin θ for the crystal\'s lattice spacing d. The sample comes back with a $(item)Laue Pattern$() and '
                            'Insight. Keep the anode under 2.2 kW.',
    'crystal_growth_furnace': 'Heat the melt a little above its melting point, pull the seed and turn it. Pull rate over the thermal gradient must sit near '
                              'the critical ratio for a defect-free crystal; slow pulling and stirring leave the impurities in the melt.',
    'optical_workshop': 'Set the two surface curvatures: the lens power is P = (n − 1)(C₁ − C₂). Polish until the surface error is below what the '
                        'piece needs - a quarter of a wavelength for a lens, less for correctors and gratings. Crystals need a Laue Pattern to be '
                        'cut along their lattice; glass does not.',
}

# =================================================================================== PHYSICS
_r12.model('x_ray_diffractometer', 'X-ray Diffractometer',
           'X-rays scattered by successive planes of atoms reinforce only where the extra path between planes is a whole number of wavelengths: '
           'nλ = 2d·sin θ. A copper anode gives its Kα line, λ = 1.5406 Å - but only when the tube voltage can knock out a K-shell electron, above '
           '8.98 kV. The count rate grows with the current and steeply with the overvoltage.',
           'Thermal drift of the goniometer: the angle wanders by ±0.05°·d.',
           [_P('V', 'Tube voltage V', 'kV', 0, 60, 0.1, 30, 'The voltage across the X-ray tube. Copper\'s Kα line appears only above 8.98 kV.'),
            _P('I', 'Tube current I', 'mA', 0, 60, 0.1, 20, 'Electrons per second onto the anode. More current, more X-rays - and more heat in the anode.'),
            _P('tt', 'Detector angle 2θ', '°', 5, 150, 0.001, 40, 'The angle between the incoming beam and the detector.')],
           [_D('th', 'Bragg angle θ', '°', 'tt/2 + 0.05*d', 'θ = 2θ/2', 'The glancing angle on the lattice planes.'),
            _D('dm', 'Spacing that reflects', 'Å', '1.5406/(2*sin(th*0.0174533))', 'd = λ/(2 sin θ)', 'The lattice spacing this angle picks out.'),
            _D('err', 'Mismatch', '%', '100*abs(dm - dl)/dl', '|d − d_lattice|/d_lattice', 'How far that is from the crystal\'s own spacing.'),
            _D('Pa', 'Anode power', 'kW', 'V*I/1000', 'P = V·I', 'Heat the anode has to shed.'),
            _D('cnt', 'Count rate', 'kcps', 'I*pos(V - 8.98)^1.6/100', 'N ∝ I·(V − V_K)^1.6', 'Characteristic X-rays per second.')],
           [_C('kedge', 'stop', 'V - 8.98', 'Below copper\'s K-edge: no Kα line', 'V > 8.98 kV'),
            _C('anode', 'damage', '2.2 - Pa', 'The anode is melting (above 2.2 kW)', 'P < 2.2 kW')],
           rate='clamp(cnt/40, 0.2, 2)', rate_formula='rate = count rate / 40 kcps', rate_desc='More X-rays, a faster exposure.',
           heat='Pa', idle_heat='0', rvars=[_V('dl', 'Lattice spacing d (Å)', 2.551)], kappa=40, heat_capacity=40)

_r12.model('crystal_growth_furnace', 'Crystal Growth Furnace',
           'A seed touches the melt and is drawn up at speed v: the melt freezes onto it, releasing its latent heat into the crystal. The heater\'s '
           'superheat sets the temperature gradient G at the growth front, and v/G decides what the crystal keeps - too fast, and it traps vacancies; '
           'too slow, interstitials; near the critical ratio, neither (Voronkov). An impurity is taken in only k_eff as readily as it sits in the '
           'melt; pulling slowly and stirring hard brings k_eff down to its equilibrium value (Burton-Prim-Slichter).',
           'Convection in the melt: the superheat varies by ±2%·d.',
           [_P('Th', 'Heater temperature', 'K', 300, 3200, 1, 1500, 'Temperature of the heaters round the crucible.'),
            _P('v', 'Pull rate v', 'mm/h', 0.1, 100, 0.1, 10, 'How fast the seed and the growing crystal are drawn up.'),
            _P('w', 'Rotation ω', 'rpm', 0, 60, 0.1, 10, 'How fast the crystal turns. Stirring thins the layer of melt the impurities pile up in.')],
           [_D('dT', 'Superheat', 'K', '(Th - Tm)*(1 + 0.02*d)', 'ΔT = T − T_m', 'How far above its melting point the melt is held.'),
            _D('G', 'Gradient at the front', 'K/mm', '0.5 + dT/10', 'G ≈ G₀ + ΔT/L', 'How steeply the temperature falls across the growth front.'),
            _D('xi', 'Voronkov ratio v/G', 'mm²/(h·K)', 'v/G', 'ξ = v/G', 'Near 3 the crystal grows free of point defects.'),
            _D('bl', 'Boundary layer', 'mm', '1/sqrt(w + 1)', 'δ ∝ ω^(−1/2)', 'The melt layer at the front that rejected impurities pile up in.'),
            _D('ke', 'Effective segregation k_eff', '', 'k0/(k0 + (1 - k0)*exp(-(v/3600)*bl/0.01))', 'k_eff = k₀/(k₀ + (1 − k₀)e^(−vδ/D))',
               'How readily the crystal takes in the impurity, compared with the melt.'),
            _D('pur', 'Crystal purity', '%', '100*(1 - 0.02*ke)', 'purity = 1 − k_eff·c₀', 'The boule\'s purity, from a feed 98% pure.')],
           [_C('frozen', 'stop', 'dT - 1', 'The melt has frozen: the heaters are below its melting point', 'T > T_m'),
            _C('boil', 'wear', '150 - dT', 'The melt is so hot it attacks the crucible and boils off', 'ΔT < 150 K'),
            _C('crucible', 'damage', '2800 - Th', 'The iridium crucible softens above 2,800 K', 'T < 2800 K')],
           rate='clamp(v/8, 0.1, 2)', rate_formula='rate = v / 8 mm/h', rate_desc='The faster it is pulled, the sooner the boule is done.',
           heat='0.002*(Th - 300)', idle_heat='0',
           rvars=[_V('Tm', 'Melting point (K)', 2323), _V('k0', 'Equilibrium segregation k₀', 0.05)], kappa=40, heat_capacity=60)

_r12.model('optical_workshop', 'Optical Workshop',
           'A lens\'s power comes from its two surfaces: P = (n − 1)(C₁ − C₂), with C the curvature (one over the radius) of each, positive when it '
           'bulges toward the light. Grinding sets the curvatures; polishing then wears the surface error down, roughly exponentially with time. '
           'Below a quarter of a wavelength the image is as sharp as diffraction allows (the Rayleigh criterion).',
           'Temperature of the shop: the glass expands, and the power drifts by ±0.2%·d.',
           [_P('C1', 'Front curvature C₁', '1/m', -50, 50, 0.001, 1, 'One over the radius of the first surface.'),
            _P('C2', 'Back curvature C₂', '1/m', -50, 50, 0.001, -1, 'One over the radius of the second surface.'),
            _P('tp', 'Polishing time', 'min', 1, 600, 1, 60, 'How long the surface is polished. Longer is truer - and slower.')],
           [_D('Pw', 'Lens power', 'D', '(n - 1)*(C1 - C2)*(1 + 0.002*d)', 'P = (n − 1)(C₁ − C₂)', 'In dioptres: one over the focal length in metres.'),
            _D('f', 'Focal length', 'm', '1/max(abs(Pw), 0.000001)', 'f = 1/P', 'Where it brings parallel light to a point.'),
            _D('e', 'Surface error', 'nm', '600*exp(-tp/45)*(1 + 0.1*d) + 15', 'ε ≈ ε₀·e^(−t/τ)', 'How far the surface is from its ideal shape.'),
            _D('S', 'Strehl ratio', '', 'exp(-(6.2832*e/550)^2)', 'S = e^(−(2πε/λ)²)', 'Peak brightness of a star\'s image against a perfect lens.')],
           [],
           rate='clamp(2*60/(tp + 60), 0.2, 2)', rate_formula='rate = 2·60 / (t + 60 min)', rate_desc='Longer polishing is slower.',
           heat='0.05', idle_heat='0', rvars=[_V('n', 'Refractive index n', 1.458)], kappa=40, heat_capacity=40)

# lattice spacings (Å) of the strong reflections studied
_D_SPACING = {'sapphire_boule': 2.551, 'fluorite_boule': 3.155, 'garnet_boule': 2.584, 'moissanite': 2.513, 'minecraft:quartz': 3.343,
              'minecraft:calcite': 3.035}
_r12.WINDOWS['x_ray_diffractometer'] = {f'r46_study_{k.split(":")[-1]}': ({'dl': v}, [('err', 0, 0.4)]) for k, v in _D_SPACING.items()}
_GROW = {  # recipe: (Tm, k0, minimum purity %)
    'sapphire_from_crystal': (2323, 0.05, 99.85), 'sapphire_from_corundum': (2323, 0.05, 99.85), 'fluorite': (1691, 0.1, 99.75),
    'garnet': (1700, 0.08, 99.8),
}
_r12.WINDOWS['crystal_growth_furnace'] = {f'r46_grow_{k}': ({'Tm': t, 'k0': k0}, [('xi', 2.4, 3.6), ('pur', p, 100), ('dT', 5, 80)])
                                          for k, (t, k0, p) in _GROW.items()}
_CUT = {  # recipe: (n, power range, surface error max nm)
    'fused_silica_lens': (1.458, 0.9, 1.1, 137), 'fluorite_lens': (1.434, 0.9, 1.1, 137), 'moissanite_lens': (2.65, 4.5, 5.5, 137),
    'sapphire_window': (1.77, -0.02, 0.02, 69), 'schmidt_corrector': (1.458, -0.05, 0.05, 69), 'echelle_grating': (1.458, -0.02, 0.02, 34),
    'photonic_lens': (1.5, 9.5, 10.5, 34),
}
_r12.WINDOWS['optical_workshop'] = {f'r46_cut_{k}': ({'n': n}, [('Pw', lo, hi), ('e', 0, em)]) for k, (n, lo, hi, em) in _CUT.items()}
_r12.WINDOWS.setdefault('reflecting_telescope', {}).update({
    'r46_schmidt_survey': ({'mreq': 15}, [('mlim', 15, 99)]),
    'r46_echelle_spectra': ({'mreq': 11}, [('mlim', 11, 99)]),
    'r46_apochromat_planetary': ({'mreq': 0}, [('res', 0, 1.1)]),
})

# =================================================================================== RESEARCH
RESEARCH = [
    ('crystallography', 'Crystallography',
     'A crystal is one pattern of atoms repeated in every direction. X-rays are short enough to be scattered by its planes, and the angles at which '
     'they reinforce give the spacing between them. Unlocks the X-ray Diffractometer and the Laue Pattern.',
     'x_ray_diffractometer', 3, -9, 45, ['spectroscopy', 'chemical_bonding'], [('minecraft:amethyst_shard', 8), ('spectral_plate', 2)]),
    ('crystal_growth_physics', 'Crystal Growth',
     'Every transistor, laser and lens crystal starts as a seed dipped in its own melt. How fast it is drawn out decides what defects and '
     'impurities it keeps. Unlocks the Crystal Growth Furnace and its boules.',
     'crystal_growth_furnace', 4, -10, 60, ['crystallography'], [('laue_pattern', 4), ('minecraft:quartz', 16)]),
    ('geometric_optics', 'Geometric Optics',
     'A lens bends light by the difference in its two curvatures, times how much more slowly light runs in it. Grind the curvatures, polish '
     'the error away, and you have an instrument. Unlocks the Optical Workshop and its lenses, plates and gratings.',
     'optical_workshop', 6, -9, 70, ['crystallography', 'stellar_astrophysics'], [('fused_silica', 4), ('laue_pattern', 2)]),
]

PUZZLES = {
    'crystallography': dict(
        goal='Two neighbouring planes of atoms, a distance a apart, scatter X-rays of wavelength λ arriving at a glancing angle θ. The rays reinforce '
             'when the extra path between the planes is a whole number n of wavelengths. Write a.',
        name='Bragg reflection condition', lhs='a',
        form='n * λ / ( 2 * sin ( θ ) )',
        expr='n * L / ( 2 * sin ( t ) )',
        ask='Find d in ångströms for copper Kα (L = 1.5406 Å), first order (n = 1), reflecting at t = 0.3067 rad.',
        vars={'a': 'spacing between planes (Å)', 'n': 'order', 'λ': 'wavelength', 'θ': 'glancing angle', 'L': 'wavelength (Å)',
              't': 'glancing angle (rad)'},
        given={'n': 1, 'L': 1.5406, 't': 0.3067}, unit='Å',
        method='The ray scattered by the deeper plane travels 2d·sin θ further; the waves add up when that is a whole number of wavelengths.',
        distractors=['cos', '4', 'c']),
    'crystal_growth_physics': dict(
        goal='A crystal grows at its front only as fast as its latent heat L (per kilogram) can be carried away up the crystal, of density ρ, which '
             'conducts heat k watts per metre per kelvin down a gradient G. Write the growth speed v.',
        name='Stefan condition at the front', lhs='v',
        form='k * G / ( ρ * L )',
        expr='k * G / ( R * L ) * 3600000',
        ask='Find v in mm/h for silicon: k = 22 W/(m·K), G = 1500 K/m, R = 2330 kg/m³, L = 1.8e6 J/kg.',
        vars={'v': 'growth speed (mm/h)', 'k': 'thermal conductivity', 'G': 'gradient (K/m)', 'ρ': 'density', 'L': 'latent heat (J/kg)',
              'R': 'density (kg/m³)'},
        given={'k': 22, 'G': 1500, 'R': 2330, 'L': 1.8e6}, unit='mm/h',
        method='At the front the heat conducted away, k·G per square metre, must equal the latent heat set free, ρ·L·v.',
        distractors=['T', '2', '+']),
    'geometric_optics': dict(
        goal='A thin lens of refractive index n has two surfaces of radii R _ 1 and R _ 2 (positive when the centre of the curve lies beyond the '
             'surface). Write its power P, one over its focal length.',
        name='Lensmaker\'s equation', lhs='P',
        form='( n - 1 ) * ( 1 / R _ 1 - 1 / R _ 2 )',
        expr='( n - 1 ) * ( 1 / A - 1 / B )',
        ask='Find P in dioptres for a fused silica lens, n = 1.458, with A = 0.5 m and B = −0.5 m.',
        vars={'P': 'power (dioptres)', 'n': 'refractive index', 'R': 'radius of curvature', 'A': 'front radius (m)', 'B': 'back radius (m)'},
        given={'n': 1.458, 'A': 0.5, 'B': -0.5}, unit='D',
        method='Each surface bends the light by (n − 1)/R; for a thin lens the two bends simply add.',
        distractors=['2', 'π', 'f']),
}

# =================================================================================== RECIPES
XD, CP, OF, AP = 'xray_diffraction', 'crystal_pulling', 'optical_finishing', 'astrophotography'
RECIPES = [
    # (name, type, item inputs, item outputs, FE/t, ticks, research, insight, catalysts, chances)
    # (a grown boule is one flawless crystal: its spots are sharp, and one exposure gives several clean patterns)
    *[(f'study_{k.split(":")[-1]}', XD, [(k, 1), ('photographic_plate', 1)], [(k, 1), ('laue_pattern', 3 if k.endswith('_boule') else 2 if k == 'moissanite' else 1)],
       256, 300 if ':' in k else 200, 'crystallography', 3, [], None)
      for k in _D_SPACING],
    ('grow_sapphire_from_crystal', CP, [('corundum_crystal', 1)], [('sapphire_boule', 3)], 512, 1200, 'crystal_growth_physics', 0, [], None),
    ('grow_sapphire_from_corundum', CP, [('corundum', 2)], [('sapphire_boule', 3)], 512, 600, 'crystal_growth_physics', 0, [], None),
    ('grow_fluorite', CP, [('fluorspar', 4)], [('fluorite_boule', 1)], 384, 800, 'crystal_growth_physics', 0, [], None),
    ('grow_garnet', CP, [('garnet', 4), ('rare_earth_oxide', 1)], [('garnet_boule', 1)], 384, 800, 'crystal_growth_physics', 0, [], None),
    ('cut_fused_silica_lens', OF, [('fused_silica', 2)], [('fused_silica_lens', 1)], 128, 400, 'geometric_optics', 0, [], None),
    ('cut_fluorite_lens', OF, [('fluorite_boule', 1), ('laue_pattern', 1)], [('fluorite_lens', 2)], 128, 400, 'geometric_optics', 0, [], None),
    ('cut_moissanite_lens', OF, [('moissanite', 1), ('laue_pattern', 1)], [('moissanite_lens', 1)], 128, 400, 'geometric_optics', 0, [], None),
    ('cut_sapphire_window', OF, [('sapphire_boule', 1), ('laue_pattern', 1)], [('sapphire_window', 4)], 128, 400, 'geometric_optics', 0, [], None),
    ('cut_schmidt_corrector', OF, [('fused_silica', 4)], [('schmidt_corrector', 1)], 128, 600, 'geometric_optics', 0, [], None),
    ('cut_echelle_grating', OF, [('fused_silica', 2), ('minecraft:iron_ingot', 1)], [('echelle_grating', 1)], 128, 600, 'geometric_optics', 0, [],
     None),
    # deep-ultraviolet lithography lenses are fluorite and fused silica: the Photonic Lens, a second way
    ('cut_photonic_lens', OF, [('fluorite_boule', 4), ('fused_silica', 8), ('metamaterial_sheet', 6), ('laue_pattern', 1)], [('photonic_lens', 1)],
     1024, 1200, 'geometric_optics', 0, [], None),
    # the telescopes, better equipped
    ('schmidt_survey', AP, [('photographic_plate', 4)], [('star_field_plate', 2), ('deep_sky_plate', 1)], 128, 1200, 'geometric_optics', 8,
     ['schmidt_corrector'], None),
    ('echelle_spectra', AP, [('photographic_plate', 1), ('spectral_plate', 1)], [('stellar_spectrum_plate', 3)], 128, 800, 'geometric_optics', 6,
     ['echelle_grating'], None),
    ('apochromat_planetary', AP, [('photographic_plate', 1)], [('planetary_plate', 2)], 128, 400, 'geometric_optics', 5, ['apochromatic_objective'],
     None),
]
CRAFTING = [
    ('xray_cabinet', ['ILI', 'L L', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'lead_shielding_plate'}, 'xray_cabinet', 4),
    ('leaded_glass', ['GGG', 'GLG', 'GGG'], {'G': 'minecraft:glass', 'L': 'lead_shielding_plate'}, 'leaded_glass', 8),
    ('xray_tube', ['GCG', 'RIR', 'GCG'], {'G': 'minecraft:glass', 'C': 'minecraft:copper_ingot', 'R': 'minecraft:redstone',
                                         'I': 'minecraft:iron_ingot'}, 'xray_tube', 1),
    ('goniometer', ['III', 'ICI', 'III'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:comparator'}, 'goniometer', 4),
    ('growth_furnace_casing', ['IBI', 'B B', 'IBI'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:bricks'}, 'growth_furnace_casing', 4),
    # Earth's core, and the iron meteorites, hoard the iridium the crust lacks
    ('iridium_crucible', ['K K', 'K K', 'KKK'], {'K': 'core_iron'}, 'iridium_crucible', 1),
    ('pull_shaft', ['I', 'P', 'I'], {'I': 'minecraft:iron_ingot', 'P': 'minecraft:piston'}, 'pull_shaft', 2),
    ('optical_bench', ['SSS', 'I I'], {'S': 'minecraft:smooth_stone', 'I': 'minecraft:iron_ingot'}, 'optical_bench', 4),
    ('lens_grinder', [' G ', 'IRI', 'III'], {'G': 'minecraft:grindstone', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone'}, 'lens_grinder', 1),
    ('interferometer', ['MRM', 'GIG', 'MRM'], {'M': 'telescope_mirror_blank', 'R': 'minecraft:redstone', 'G': 'minecraft:glass_pane',
                                              'I': 'minecraft:iron_ingot'}, 'interferometer', 1),
    ('apochromatic_objective', ['S', 'F', 'S'], {'S': 'fused_silica_lens', 'F': 'fluorite_lens'}, 'apochromatic_objective', 1),
]

CATS = [('optics', 'Optics & Crystals', 'Crystals studied, grown pure and cut - and the lenses, plates and gratings made of them.', 'fluorite_lens', 23)]
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
    p = os.path.join(HERE, 'puzzles2', 'group_r46.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    REC, w = g['REC'], g['w']
    for name, typ, ins, outs, fe, t, res, insight, cats, ch in RECIPES:
        g['mr']('r46_' + name, typ, ins, [], outs, [], energy=fe, duration=t, research=res, catalysts=cats, chances=ch)
        p = os.path.join(REC, typ, 'r46_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        if insight: r['insight_per_run'] = insight
        w(p, r)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r46_' + name, pat, key, res, n)


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)


def apply_late(g):
    import round42
    round42._extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'),
                        [f'{NS}:{b["id"]}' for b in BLOCKS])


def finish(g):
    te = g['text_entry']
    te('optics', 'r46_overview', 'Optics & Crystals', 'fluorite_lens', [
        'Every lens is made the same way: study the crystal, grow it pure, cut it true. Three machines do it - the $(item)X-ray Diffractometer$(), '
        'the $(item)Crystal Growth Furnace$() and the $(item)Optical Workshop$() - and what they make goes into the telescopes and more.',
        'The crystals come from everywhere: quartz and calcite from the Overworld, fluorspar from calcite and geodes, and from the Planetary Interior '
        'corundum, garnet and moissanite. Glass - fused silica - has no crystal lattice at all, so it needs no study before it is cut.'],
        adv='crystallography', sort=1, spot=['fluorite_lens', 'sapphire_boule', 'laue_pattern'])
    te('optics', 'r46_diffraction', 'X-ray Diffraction', 'x_ray_diffractometer', [
        'X-rays are about as long as the gaps between atoms. Scattered by a crystal\'s planes, they reinforce only where the extra path between '
        'neighbouring planes is a whole number of wavelengths: $(thing)nλ = 2d·sin θ$().',
        'The $(item)X-ray Diffractometer$() has a copper tube: above 8.98 kV it gives copper\'s Kα line, λ = 1.5406 Å. Set the detector angle 2θ '
        'so that θ satisfies Bragg\'s law for the crystal\'s spacing - corundum 2.551 Å, fluorite 3.155, garnet 2.584, moissanite 2.513, quartz '
        '3.343, calcite 3.035 - and the reflection appears.',
        'The sample comes back, with a $(item)Laue Pattern$(): the crystal\'s orientation, which every cut along its lattice needs. Every study '
        'gives $(thing)Insight$(). The anode melts above 2.2 kW: more current, but not too much.'],
        adv='crystallography', sort=2, spot=['x_ray_diffractometer', 'laue_pattern', 'goniometer'])
    te('optics', 'r46_growth', 'Growing a Crystal', 'crystal_growth_furnace', [
        'In the $(item)Crystal Growth Furnace$() a seed crystal is dipped into the melt and drawn slowly up, turning - the Czochralski method. The '
        'melt freezes onto it as one crystal. Its $(item)Iridium Crucible$() is made of Iron-Nickel: a planet\'s core, and the iron meteorites, hoard '
        'the iridium the crust lacks.',
        'Hold the melt a little above its melting point: the superheat sets how steeply the temperature falls at the growth front, G. Then pull at '
        'v so that $(thing)v/G$() sits near 3 - Voronkov\'s critical ratio: faster and the crystal traps vacancies, slower and it traps '
        'interstitials.',
        'An impurity enters the crystal only k_eff as readily as it sits in the melt. Pull slowly and turn fast, and k_eff falls to its lowest: '
        'the impurities stay in the crucible. Optical boules must be 99.75-99.85% pure: close to the limit k₀ sets.',
        'Sapphire from corundum (best from a whole $(item)Corundum Crystal Mass$()), fluorite from fluorspar, and garnet from garnet with a rare '
        'earth - the dopant that makes garnet a laser crystal.'],
        adv='crystal_growth_physics', sort=3, spot=['crystal_growth_furnace', 'iridium_crucible', 'sapphire_boule', 'fluorite_boule'])
    te('optics', 'r46_workshop', 'Grinding and Polishing', 'optical_workshop', [
        'The $(item)Optical Workshop$() turns blanks into optics. A lens\'s power is set by its two surfaces: $(thing)P = (n − 1)(C₁ − C₂)$(), '
        'with C the curvature of each - one over its radius. A 1-dioptre lens focuses at a metre.',
        'Polishing wears the surface error down. Below a quarter of a wavelength the image is as sharp as diffraction allows; a Schmidt corrector '
        'needs an eighth, a grating a sixteenth. Longer polishing is truer, and slower.',
        'Fused silica becomes lenses, $(item)Schmidt Corrector Plates$() and $(item)Echelle Gratings$(). Crystals need a Laue Pattern: fluorite '
        'lenses, moissanite lenses (its index, 2.65, makes a strong lens thin) and $(item)Sapphire Windows$(), four from a boule. Fluorite between '
        'two silica lenses is an $(item)Apochromatic Objective$().',
        'Lithography lenses are really fluorite and fused silica: the workshop makes a $(item)Photonic Lens$() from them too.'],
        adv='geometric_optics', sort=4, spot=['optical_workshop', 'fused_silica_lens', 'schmidt_corrector', 'apochromatic_objective'])
    te('astrophysics', 'r46_better_telescopes', 'Better Telescopes', 'schmidt_corrector', [
        'Optics from the workshop make the Reflecting Telescope a better instrument. Each sits in it as a catalyst and is never used up.',
        'A $(item)Schmidt Corrector Plate$() turns it into a survey camera: four plates at once give two Star Field Plates and a Deep Sky Plate '
        '(magnitude 15). An $(item)Echelle Grating$() spreads each star\'s light so finely that one exposure gives three Stellar Spectrum Plates. '
        'An $(item)Apochromatic Objective$() brings every colour to one focus: two Planetary Plates at a time.'],
        adv='geometric_optics', sort=7, spot=['schmidt_corrector', 'echelle_grating', 'apochromatic_objective'])
