"""
1.14.0 (Patch D1): the Sun and small bodies. Six concepts from the space document's section 6 (3 to 7, and 10):

  3. Sailing on light: a new single-block console, the Radiation Pressure Balance (F = (1 + R)·I·cos²θ/c, and the
     sail's temperature from what it absorbs); Light Sails and Solar Statites by a newer route, and the Laser Sail.
  4. The Sun's magnetism: the Heliograph gains a magnetograph (the Zeeman splitting Δλ = 4.67e-13·g·λ²·B, with an
     observing wavelength to choose); Sunspot Magnetograms; Coronal Loop Plasma from a probe to the corona.
  5. Magnetospheres: the Pulsed Magnet Lab gains a magnetic mirror's loss cone; Magnetopause Records from the
     Heliograph, Trapped Proton Flux from a probe to the Van Allen belts, and the Active Magnetic Shield module.
  6. Comets: a probe to a Jupiter-family comet brings Cometary Ice (heavy in deuterium, as Rosetta found) and Cometary
     Dust (with glycine, as Stardust found), which the Abiogenesis Crucible turns into amino acids.
  7. Asteroids by type: C-type and M-type asteroids to add to the S-type of 1.12.0, found first by the Reflecting
     Telescope (an Asteroid Ephemeris); M-type metal is rich in the platinum-group metals, real catalysts.
 10. The nebula's gases: the Bussard Ramscoop scoops Nebular Gas in the Frontier, and the Gas Centrifuge Cascade
     separates it (and comet ice, and Jupiter's air from a probe) into hydrogen, deuterium and a new fluid, helium-3,
     which the Tokamak burns with deuterium.

Run:  python3 tools/round1140.py
"""
import glob, json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
T.WORLD['tokamak'] = {'T': 10.0}      # its magnets cold
T.WORLD['reflecting_telescope'] = {'night': 1, 'sky': 1, 'moon': 0, 'rain': 0, 'alt': 100}   # a clear moonless night on a hill


def edit_model(mid, f):
    p = os.path.join(D, 'ts_physics', mid + '.json')
    m = json.load(open(p, encoding='utf-8'))
    if f(m) is not False:
        dump(p, m)


def has(m, kind, ident):
    return any(x['id'] == ident for x in m[kind])


# ====================================================================== research
research('radiation_pressure', 'Radiation Pressure', 26, 8, 300, ['satellite_orbits', 'electromagnetic_induction'],
         [('light_sail', 1), ('minecraft:glowstone_dust', 4)], 320,
         "Light carries momentum: a beam of power P pushes on what absorbs it with a force P/c, and twice that on a mirror, since "
         "the light bounces back. Sunlight at Earth pushes on a square metre with about the weight of a grain of sand, but in "
         "space nothing pushes back, so a sail light enough keeps gaining speed. Nichols and Hull measured it with a torsion "
         "balance in 1901; JAXA's IKAROS sailed to Venus on it in 2010. A sail pushed by a laser array instead of the Sun could "
         "reach a fifth of the speed of light, if it does not melt. Unlocks the Radiation Pressure Balance.",
         'The push of light',
         {'goal': 'Light of power P, carrying momentum P/c each second, falls straight onto a perfect mirror and bounces back. Write the '
                  'force F on the mirror.',
          'name': 'Light pressure', 'lhs': 'F', 'form': '2 * P / c', 'expr': '2 * P / c',
          'ask': 'Find F in newtons for P = @{P} W and c = 3e8 m/s.',
          'vars': {'F': 'force', 'P': 'power', 'c': 'speed of light'},
          'given': {'P': 1000.0, 'c': 3e8}, 'unit': 'N',
          'method': 'The momentum per second arriving is P/c; it leaves with the same, reversed, so the change is 2P/c.',
          'distractors': ['*', '+', 'sqrt'], 'vary': {'P': [10.0, 10000.0, 10.0]}},
         'radiation_pressure_balance', 'satellite_orbits')
research('solar_magnetism', 'The Sun\'s Magnetism', 28, 7, 320, ['space_weather', 'spectroscopy'],
         [('spectral_plate', 2), ('ferrite_magnet', 2)], 340,
         "Sunspots are where the Sun's magnetic field breaks through its surface, kilogauss strong, holding back the heat from "
         "below, so they are cooler and look dark. The field is measured from here by the Zeeman effect: in a magnetic field a "
         "spectral line splits, by an amount that grows with the field and with the square of the wavelength, which is why "
         "magnetographs often look in the infrared. Where the field lines tangle and snap, flares and coronal mass ejections "
         "begin. Adds a magnetograph to the Heliograph; unlocks a probe to the corona.",
         'Dark spots, strong fields',
         {'goal': 'A sunspot is at temperature Ts, its surroundings at T. By the Stefan-Boltzmann law the flux a surface gives off goes as '
                  'the fourth power of its temperature. Write the fraction f of the surroundings\' brightness the spot gives off.',
          'name': 'Spot contrast', 'lhs': 'f', 'form': '( Ts / T ) ^ 4', 'expr': '( Ts / T ) ^ 4',
          'ask': 'Find f for Ts = @{Ts} K and T = 5778 K.',
          'vars': {'f': 'brightness fraction', 'Ts': 'spot temperature', 'T': 'surface temperature'},
          'given': {'Ts': 4278.0, 'T': 5778.0}, 'unit': '',
          'method': 'Flux is σT⁴ for each; the σ cancels in the ratio.',
          'distractors': ['*', '2', 'sqrt'], 'vary': {'Ts': [3500.0, 5000.0, 10.0]}},
         'sunspot_magnetogram', 'space_weather')
research('magnetospheres', 'Magnetospheres', 28, 8, 340, ['solar_magnetism', 'magnetic_confinement'],
         [('ferrite_magnet', 4), ('survey_satellite', 1)], 360,
         "Earth's field holds off the solar wind: the boundary, the magnetopause, stands where the wind's pressure equals the "
         "field's, about ten Earth radii sunward. Inside it, charged particles spiral along the field lines and bounce between "
         "the poles, where the field grows stronger and turns them back: a magnetic mirror. That is what holds the Van Allen "
         "belts. Only particles moving nearly along the field, inside the loss cone, escape into the air as aurorae. Adds a "
         "magnetic mirror to the Pulsed Magnet Lab; unlocks a probe to the belts and the Active Magnetic Shield.",
         'A bottle made of field',
         {'goal': 'A particle spiralling along a field line keeps its magnetic moment, so the field B where it is and the strongest field '
                  'Bm it can turn back from fix the pitch angle α it must have to be trapped. Write sin α at the edge of the loss cone.',
          'name': 'Loss cone', 'lhs': 'sinα', 'form': 'sqrt ( B / Bm )', 'expr': 'sqrt ( B / Bm )',
          'ask': 'Find sin α for B = @{B} nT at the equator of a field line and Bm = 50000 nT at its foot.',
          'vars': {'sinα': 'sine of the loss-cone angle', 'B': 'field where it is', 'Bm': 'field at the mirror point'},
          'given': {'B': 500.0, 'Bm': 50000.0}, 'unit': '',
          'method': 'μ = mv⊥²/2B is kept and so is the speed: sin²α/B is the same everywhere, and at the mirror point α = 90°.',
          'distractors': ['*', '2', 'ln'], 'vary': {'B': [100.0, 5000.0, 50.0]}},
         'active_magnetic_shield', 'solar_magnetism')
research('asteroid_taxonomy', 'Asteroids by Type', 24, 10, 300, ['sample_return', 'telescope_optics'],
         [('asteroid_regolith', 2), ('spectral_plate', 2)], 320,
         "Asteroids sort by the light they reflect. C-types are dark and carbon-rich, with water locked in clays and organic "
         "molecules; S-types are stony and bright; M-types are metal, the cores of shattered small worlds, iron and nickel with "
         "the platinum-group metals. An asteroid's brightness alone does not give its size: a dark one must be bigger to look as "
         "bright. Unlocks C-type and M-type asteroid missions, found first with the Reflecting Telescope.",
         'Dark, stony, metal',
         {'goal': 'An asteroid of absolute magnitude H reflects a fraction p of the light that falls on it (its albedo). Write its '
                  'diameter D in km, using 1329 km, the diameter of a body of H = 0 and p = 1.',
          'name': 'Size from brightness', 'lhs': 'D', 'form': '1329 / sqrt ( p ) * 10 ^ ( - H / 5 )', 'expr': '1329 / sqrt ( p ) * 10 ^ ( - H / 5 )',
          'ask': 'Find D in km for H = @{H} and p = 0.05 (a C-type).',
          'vars': {'D': 'diameter', 'p': 'albedo', 'H': 'absolute magnitude'},
          'given': {'p': 0.05, 'H': 18.0}, 'unit': 'km',
          'method': 'Reflected light goes as p·D²; five magnitudes are a factor of a hundred in light, so D goes as 10^(−H/5)/√p.',
          'distractors': ['*', 'ln', '+'], 'vary': {'H': [10.0, 22.0, 0.1]}},
         'platinum_group_nugget', 'sample_return')
research('comets', 'Comets', 26, 10, 340, ['asteroid_taxonomy'],
         [('asteroid_regolith', 2), ('minecraft:packed_ice', 4)], 360,
         "Comets are the leftovers of the outer solar system: ice, dust and organic molecules, kept cold since the planets "
         "formed. Jupiter-family comets were thrown inward by Jupiter and come round every few years; long-period comets fall "
         "in from the Oort cloud, far out. One number from the orbit, Tisserand's parameter, tells which: between 2 and 3, "
         "Jupiter's family. Rosetta found comet 67P's water three times richer in deuterium than our oceans; Stardust found "
         "glycine, an amino acid, in comet Wild 2's dust. Unlocks a comet mission.",
         'Ice from the edge',
         {'goal': 'For an orbit of semi-major axis a and eccentricity e in Jupiter\'s plane, write Tisserand\'s parameter T with respect to '
                  'Jupiter, whose semi-major axis is aJ.',
          'name': 'Tisserand parameter', 'lhs': 'T', 'form': 'aJ / a + 2 * sqrt ( a / aJ * ( 1 - e ^ 2 ) )', 'expr': 'aJ / a + 2 * sqrt ( a / aJ * ( 1 - e ^ 2 ) )',
          'ask': 'Find T for aJ = 5.2 AU, a = @{a} AU and e = 0.64 (comet 67P).',
          'vars': {'T': 'Tisserand parameter', 'aJ': "Jupiter's semi-major axis", 'a': 'semi-major axis', 'e': 'eccentricity'},
          'given': {'aJ': 5.2, 'a': 3.46, 'e': 0.64}, 'unit': '',
          'method': 'In the frame turning with Jupiter, the Jacobi constant is kept through an encounter; far from Jupiter it reduces to this.',
          'distractors': ['-', '*', 'ln'], 'vary': {'a': [2.5, 30.0, 0.1]}},
         'cometary_dust', 'asteroid_taxonomy')
research('nebular_gases', 'The Nebula\'s Gases', 28, 10, 340, ['ramscoops', 'isotope_separation'],
         [('deuterium_cell', 1), ('warm_helium_cell', 1)], 360,
         "The gas between the stars is mostly hydrogen and helium, with a trace of everything else. Spun fast, a gas sorts "
         "itself by mass: heavier molecules drift outward, and a cascade of centrifuges multiplies the small difference each one "
         "makes. That is how deuterium is taken from hydrogen, and helium-3 from helium-4: helium-3, which fuses with deuterium "
         "into helium and a proton, with no neutrons to wear out the reactor. Jupiter's air holds it as the Sun was born. "
         "Unlocks scooping Nebular Gas, separating it, a probe into Jupiter's atmosphere, and D-He3 fusion.",
         'Sorted by spinning',
         {'goal': 'Two gases whose molar masses differ by Δm spin at peripheral speed v at temperature T. Write the separation factor α '
                  'of one centrifuge, with R the gas constant.',
          'name': 'Separation factor', 'lhs': 'α', 'form': 'exp ( Δm * v ^ 2 / ( 2 * R * T ) )', 'expr': 'exp ( dm * v ^ 2 / ( 2 * R * T ) )',
          'ask': 'Find α for helium-3 and helium-4 (Δm = 0.001 kg/mol) at v = @{v} m/s, T = 300 K, R = 8.314.',
          'vars': {'α': 'separation factor', 'Δm': 'difference in molar mass', 'v': 'peripheral speed', 'R': 'gas constant', 'T': 'temperature'},
          'given': {'dm': 0.001, 'v': 700.0, 'R': 8.314, 'T': 300.0}, 'unit': '',
          'method': 'In the spinning frame each gas settles as in gravity, with potential −½ω²r²: the Boltzmann factors differ by Δm.',
          'distractors': ['*', '-', 'sqrt'], 'vary': {'v': [300.0, 950.0, 10.0]}},
         'helium_3_cell', 'ramscoops')

# ====================================================================== 3. the Radiation Pressure Balance (a console)
RP = model([par('I', 'kW/m²', 0.1, 100, 0.1, 1.4), par('th', '°', 0, 80, 0.5, 0)],
           {'sig': 7.0, 'Rf': 0.9, 'Tm': 600, 'I0': 1.0},
           [der('F', 'N/m²', '(1 + Rf)*I*I0*1000*cos(th/57.296)^2/2.998e8', 'F = (1 + R)·I·cos²θ/c per square metre'),
            der('ac', 'mm/s²', '(1 + Rf)*I*I0*1000*cos(th/57.296)^2/2.998e8/(sig/1000)*1000*(1 + 0.01*d)', 'acceleration: force over the sail\'s mass per area'),
            der('Ts', 'K', '((1 - Rf)*I*I0*1000*cos(th/57.296)/(2*0.8*5.67e-8))^0.25', 'what it absorbs, radiated from both faces')],
           [con('melt', 'stop', 'Tm - ((1 - Rf)*I*I0*1000*cos(th/57.296)/(2*0.8*5.67e-8))^0.25', 'the sail under its melting point')],
           'clamp(0.3 + I/40, 0.3, 1.5)', 'a stronger beam finishes the test sooner')
machine('radiation_pressure_balance', 'Radiation Pressure Balance', "A torsion balance in a vacuum bell: a sample of sail film on a "
        "quartz fibre, a mirror to read its twist, and a lamp (or, for the strongest tests, a laser) shining on it. It measures "
        "the push of light on the film, and how hot the film gets, before a sail is cut from it.", 'chain', 3, RP,
        {'p.I': 'Light intensity', 'p.I.desc': 'What falls on the film (×1,000,000 for a laser test): sunlight at Earth is 1.4 kW/m².',
         'p.th': 'Angle of incidence', 'p.th.desc': 'Tilting the film away from the light cuts the push as cos²θ, and the heating as cos θ.',
         'd.F': 'Pressure', 'd.F.desc': 'The push of the light per square metre.', 'd.ac': 'Characteristic acceleration',
         'd.ac.desc': 'What a sail of this film would feel.', 'd.Ts': 'Film temperature', 'd.Ts.desc': 'What it absorbs, radiated away from both faces.',
         'c.melt': 'The film is melting', 'v.sig': 'Areal density (g/m²)', 'v.Rf': 'Reflectivity', 'v.Tm': 'Melting point', 'v.I0': 'Source scale',
         'disturb': 'Fibre drift: the reading wanders by 1%·d.', 'rate.desc': 'A stronger beam finishes sooner.'},
        extreme('radiation_pressure_balance', 1, P_CONSOLE, {'A': 'fused_silica_lens', 'F': 'silica_aerogel', 'L': 'laser_head', 'N': 'precision_gyroscope'}))

# ====================================================================== 4 and 5: the Heliograph and the Pulsed Magnet Lab gain modes
def helio(m):
    if has(m, 'params', 'lam'):
        return False
    m['params'].append(par('lam', 'Å', 4000, 16000, 10, 6173))
    m.setdefault('recipe_vars', {}).update({'gL': 0.0, 'Bs': 0.0})
    m['derived'].append(der('dl', 'Å', '4.67e-13*gL*lam^2*Bs*(1 + 0.02*d)', 'Zeeman splitting Δλ = 4.67e-13·g·λ²·B'))


edit_model('heliograph', helio)
L['physics.%s.heliograph.p.lam' % NS] = 'Observing wavelength'
L['physics.%s.heliograph.p.lam.desc' % NS] = 'The spectral line the magnetograph watches: the splitting grows as its square.'
L['physics.%s.heliograph.d.dl' % NS] = 'Zeeman splitting'
L['physics.%s.heliograph.d.dl.desc' % NS] = 'How far the field splits the line: it must be wide enough to see, narrow enough to stay in the passband.'
L['physics.%s.heliograph.v.gL' % NS] = 'Landé factor'
L['physics.%s.heliograph.v.Bs' % NS] = 'Field (gauss)'


def magnet(m):
    if has(m, 'derived', 'lc'):
        return False
    m.setdefault('recipe_vars', {}).update({'B0': 0.0})
    m['derived'].append(der('lc', '°', '57.296*atan(sqrt(clamp(B0/max(Bf, 0.01), 0, 0.999))/sqrt(1 - clamp(B0/max(Bf, 0.01), 0, 0.999)))',
                            'loss cone of a mirror from B0 to the peak field: sin²α = B0/Bf'))


edit_model('pulsed_magnet_lab', magnet)
L['physics.%s.pulsed_magnet_lab.d.lc' % NS] = 'Loss cone'
L['physics.%s.pulsed_magnet_lab.d.lc.desc' % NS] = 'Of a magnetic mirror from the field B0 to the peak field: what leaks through the ends.'
L['physics.%s.pulsed_magnet_lab.v.B0' % NS] = 'Mirror base field (T)'

# ====================================================================== items, blocks and a fluid
item('laser_sail', 'Laser Sail', "A sail four metres across and a gram in weight, a stack of dielectric layers that reflects all but "
     "a hundred-thousandth of the light that falls on it, with a chip of a probe at its centre. A ground laser array pushes it at "
     "ten thousand g. Load it in a Mission Bay as a stage: it gives a probe 200 km/s and needs no propellant (crews cannot ride "
     "it). Made in the Radiation Pressure Balance.")
item('sunspot_magnetogram', 'Sunspot Magnetogram', "A map of the Sun's surface field, white and black for the two polarities, the "
     "sunspot pairs standing out like knots. Read from the Zeeman splitting of a spectral line. Made in the Heliograph.")
item('magnetopause_record', 'Magnetopause Record', "A chart of where the solar wind's pressure meets the Earth's field, pushed in "
     "close when the wind gusts and the field turns south. Made in the Heliograph.")
item('coronal_loop_plasma', 'Coronal Loop Plasma', "Plasma at a million degrees from the arches of field above a sunspot, caught in "
     "a magnetic bottle by a probe that flew through the corona, as Parker Solar Probe does. "
     "Brought back by a Mission Bay's probe from the Sun's corona.")
item('trapped_proton_flux', 'Trapped Proton Flux', "A record of the protons held in the inner Van Allen belt, energy against pitch "
     "angle, from a probe that flew through it as the Van Allen Probes did. Brought back by a Mission Bay's probe from the "
     "radiation belts.")
item('cometary_ice', 'Cometary Ice', "Dirty snow from a comet's nucleus: water ice, carbon monoxide and dioxide, ammonia, and grit. "
     "Its water is three times richer in deuterium than the oceans'. Brought back by a Mission Bay's probe from a comet.")
item('cometary_dust', 'Cometary Dust', "Grains caught in aerogel as a probe flew through a comet's tail, as Stardust did. Among the "
     "silicates and the organic tar there is glycine, the simplest amino acid. Brought back by a Mission Bay's probe from a comet.")
item('carbonaceous_regolith', 'Carbonaceous Regolith', "Black, crumbling soil from a C-type asteroid, a tenth of it water held in "
     "clays, with carbon and organic molecules, as Hayabusa2 and OSIRIS-REx found on Ryugu and Bennu. Brought back by a Mission "
     "Bay's probe from a C-type asteroid.")
item('platinum_group_nugget', 'Platinum-Group Nugget', "A nugget of iron-nickel from an M-type asteroid's core, rich in platinum, "
     "rhodium, iridium and osmium, which sank with the iron when the parent body melted. Brought back by a Mission Bay's probe "
     "from an M-type asteroid.")
item('jovian_atmosphere_sample', 'Jovian Atmosphere Sample', "Gas from Jupiter's cloud tops, hydrogen and helium in the proportions "
     "the Sun was born with, helium-3 among them, as the Galileo probe measured on its way down. Brought back by a Mission Bay's "
     "probe from Jupiter.")
item('asteroid_ephemeris', 'Asteroid Ephemeris', "A table of where a small body will be, worked out from a few nights of positions "
     "on photographic plates. A Mission Bay needs one to send a probe to an asteroid or a comet. Made in the Reflecting Telescope.")
simple_block('active_magnetic_shield', 'Active Magnetic Shield', "A superconducting coil around a habitat, its field a small "
             "magnetosphere: storm protons spiral along it and are turned away, except those in the loss cone at its ends. Within "
             "eight blocks of it a storm's dose is a tenth. Works aboard the Orbital Station and in the Accretion Frontier. Made in "
             "the Pulsed Magnet Lab.", light=True)
L['fluid_type.%s.helium_3' % NS] = 'Helium-3'
L['block.%s.helium_3' % NS] = 'Helium-3'
L['block.%s.helium_3.lore' % NS] = ("Helium with one neutron instead of two: a few in every ten thousand helium atoms in the Sun's birth "
                                     "gas, and almost none on Earth. It fuses with deuterium into helium and a proton, with no neutrons "
                                     "to wear out the reactor. Made in the Gas Centrifuge Cascade.")
L['item.%s.helium_3_cell' % NS] = 'Helium-3 Gas Cell'
L['item.%s.helium_3_cell.lore' % NS] = L['block.%s.helium_3.lore' % NS] + ' (1,000 mB in a containment cell.)'

# ====================================================================== recipes: the Radiation Pressure Balance
mrec('photon_pressure', 'r1140_light_sail', [('polymer_sheet', 4), ('aluminium_ingot', 1)], [], [('light_sail', 1)], [], 1024, 400,
     'radiation_pressure', {'sig': 7.0, 'Rf': 0.9, 'Tm': 600, 'I0': 1.0}, [('ac', 1.0, 1.6)], 'radiation_pressure_balance')
mrec('photon_pressure', 'r1140_solar_statite', [('graphene_sheet', 2), ('aluminium_ingot', 1)], [], [('solar_statite', 2)], [], 2048, 400,
     'radiation_pressure', {'sig': 1.5, 'Rf': 0.92, 'Tm': 1200, 'I0': 1.0}, [('ac', 5.8, 6.1)], 'radiation_pressure_balance')
mrec('photon_pressure', 'r1140_laser_sail', [('graphene_sheet', 4), ('metamaterial_sheet', 2), ('quantum_dot', 4)], [], [('laser_sail', 1)], [],
     65536, 800, 'radiation_pressure', {'sig': 0.06, 'Rf': 0.99999, 'Tm': 1400, 'I0': 1e6}, [('ac', 5e7, 1e12)], 'radiation_pressure_balance')

# ====================================================================== recipes: the Heliograph's magnetograph
mrec('heliography', 'r1140_sunspot_magnetogram', [('spectral_plate', 2), ('ferrite_magnet', 1)], [], [('sunspot_magnetogram', 2)], [], 512, 400,
     'solar_magnetism', {'gL': 2.5, 'Bs': 2500.0}, [('dl', 0.08, 0.2)], 'heliograph')
mrec('heliography', 'r1140_magnetopause_record', [('spectral_plate', 1), ('sunspot_magnetogram', 1)], [], [('magnetopause_record', 2)], [], 512, 400,
     'magnetospheres', {}, [('r0', 6.0, 8.0), ('Kp', 3, 9)], 'heliograph')

# ====================================================================== recipes: the Active Magnetic Shield
mrec('pulsed_field', 'r1140_active_magnetic_shield', [('superconducting_coil', 8), ('magnetopause_record', 2), ('trapped_proton_flux', 2),
                                                       ('station_hull', 4)], [('superfluid_helium', 1000)], [('active_magnetic_shield', 1)], [],
     32768, 800, 'magnetospheres', {'B0': 2.0}, [('lc', 5, 15), ('Bf', 30, 99)], 'pulsed_magnet_lab')

# ====================================================================== recipes: telescopes, comets, asteroids
mrec('astrophotography', 'r1140_asteroid_ephemeris', [('spectral_plate', 1), ('minecraft:paper', 4)], [], [('asteroid_ephemeris', 4)], [], 256, 600,
     'asteroid_taxonomy', {'mreq': 17.0}, [('mlim', 17.0, 99.0)], 'reflecting_telescope')
mrec('abiogenesis', 'r1140_cometary_soup', [('cometary_dust', 1)], [('minecraft:water', 1000)], [], [('primordial_soup', 16000)], 65536, 600,
     'comets', {'L': 2, 'sg': 10}, [('Y', 0.5, 1.0)], 'abiogenesis_crucible')


def copy_recipe(rtype, src, rid, ins, outs, research_id, fins=None, fouts=None, fixed_vars=None):
    """A recipe in a machine with no console (or one whose physics we copy from a sibling)."""
    r = json.load(open(os.path.join(D, 'recipe', rtype, src + '.json'), encoding='utf-8'))
    o = {'type': r['type']}
    o['item_inputs'] = [{'item': ts(i) if ':' not in i else i, 'count': n} for i, n in ins]
    if fins:
        o['fluid_inputs'] = [{'fluid': ts(f) if ':' not in f else f, 'amount': a} for f, a in fins]
    if outs:
        o['item_outputs'] = [{'id': ts(i) if ':' not in i else i, 'count': n} for i, n in outs]
    if fouts:
        o['fluid_outputs'] = [{'id': ts(f), 'amount': a} for f, a in fouts]
    for k in ('energy', 'duration', 'unscaled'):
        if k in r:
            o[k] = r[k]
    o['research'] = ts(research_id)
    if 'physics' in r:
        o['physics'] = r['physics']
    if fixed_vars:
        o['physics'] = fixed_vars
    dump(os.path.join(D, 'recipe', rtype, rid + '.json'), o)


copy_recipe('chemical_synthesis', 'r59_platinum_rhodium_gauze_metal', 'r1140_asteroid_gauze', [('platinum_group_nugget', 1)],
            [('platinum_rhodium_gauze', 2)], 'asteroid_taxonomy')
copy_recipe('electrolysis', 'r1120_lunar_oxygen', 'r1140_carbonaceous_water', [('carbonaceous_regolith', 2)],
            [('organic_evaporite', 2)], 'asteroid_taxonomy', fouts=[('hydrogen', 1000)])
for f in glob.glob(os.path.join(D, 'recipe/electrolysis/r1140_carbonaceous_water.json')):
    r = json.load(open(f))
    r['item_inputs'] = [{'tag': ts('samples/carbonaceous_regolith'), 'count': 2}]
    dump(f, r)
copy_recipe('plasma_smelting', 'chronosteel', 'r1140_coronal_plasma', [('coronal_loop_plasma', 1)], [], 'solar_magnetism',
            fouts=[('stellar_plasma', 2000)])
dump(os.path.join(D, 'tags/item/samples/carbonaceous_regolith.json'), {'replace': False, 'values': [ts('carbonaceous_regolith')]})

# ====================================================================== 10: the nebula's gases
ramp = json.load(open(os.path.join(D, 'recipe/ram_scooping/r104_molecular_cloud.json'), encoding='utf-8'))
ramp['fluid_outputs'] = [{'id': ts('nebular_gas'), 'amount': 1000}]
ramp['research'] = ts('nebular_gases')
dump(os.path.join(D, 'recipe/ram_scooping/r1140_nebular_gas.json'), ramp)
CEN = 'gas_centrifuge_cascade'
mrec('isotope_enrichment', 'r1140_nebular_deuterium', [], [('nebular_gas', 2000)], [], [('hydrogen', 1400), ('deuterium', 200), ('carbon_monoxide', 50)],
     4096, 600, 'nebular_gases', {'dm': 0.001, 'xF': 0.05}, [('xP', 0.055, 0.19)], CEN)
mrec('isotope_enrichment', 'r1140_nebular_helium3', [], [('nebular_gas', 2000)], [], [('warm_helium', 400), ('helium_3', 100)],
     4096, 600, 'nebular_gases', {'dm': 0.001, 'xF': 0.05}, [('xP', 0.055, 0.19)], CEN)
mrec('isotope_enrichment', 'r1140_cometary_deuterium', [('cometary_ice', 4)], [], [], [('deuterium', 400), ('carbon_monoxide', 100)],
     2048, 400, 'comets', {'dm': 0.001, 'xF': 0.05}, [('xP', 0.055, 0.19)], CEN)
mrec('isotope_enrichment', 'r1140_jovian_helium3', [('jovian_atmosphere_sample', 2)], [], [], [('hydrogen', 1600), ('helium_3', 200)],
     4096, 600, 'nebular_gases', {'dm': 0.001, 'xF': 0.05}, [('xP', 0.055, 0.19)], CEN)
mrec('fusion_power', 'r1140_dhe3_fusion', [], [('deuterium', 100), ('helium_3', 100)], [], [('warm_helium', 50), ('hydrogen', 50)],
     131072, 1200, 'nebular_gases', {'Lref': 40.0}, [('L3', 40.0, 1e9)], 'tokamak')
verify_recipes()

# ====================================================================== missions (data)
NEEDS = [{'item': ts('asteroid_ephemeris'), 'count': 1}]
MISSIONS = {
    'c_type_asteroid': dict(name='A C-Type Asteroid', delta_v=5500, synodic_days=16, penalty=0.7, transfer_days=400, order=11, research='asteroid_taxonomy',
                            samples=[('carbonaceous_regolith', 8)], needs=NEEDS),
    'm_type_asteroid': dict(name='An M-Type Asteroid', delta_v=9000, synodic_days=15.6, penalty=0.8, transfer_days=1300, order=12, research='asteroid_taxonomy',
                            samples=[('platinum_group_nugget', 2), ('meteoric_iron', 8)], needs=NEEDS),
    'comet': dict(name='A Jupiter-Family Comet', delta_v=11000, synodic_days=14.6, penalty=0.7, transfer_days=2000, order=13, research='comets',
                  samples=[('cometary_ice', 8), ('cometary_dust', 4)], needs=NEEDS),
    'van_allen_belts': dict(name='The Radiation Belts', delta_v=2500, synodic_days=0, penalty=0, transfer_days=30, order=5, research='magnetospheres',
                            samples=[('trapped_proton_flux', 4)]),
    'solar_corona': dict(name="The Sun's Corona", delta_v=20000, synodic_days=0, penalty=0, transfer_days=100, order=70, research='solar_magnetism',
                         samples=[('coronal_loop_plasma', 2)]),
    'jupiter_atmosphere': dict(name="Jupiter's Atmosphere", delta_v=15000, synodic_days=13.1, penalty=0.8, transfer_days=2000, order=65, research='nebular_gases',
                               samples=[('jovian_atmosphere_sample', 4)]),
}
for mid, m in MISSIONS.items():
    o = {'name': 'mission.%s.%s' % (NS, mid), 'delta_v': m['delta_v'], 'synodic_days': m['synodic_days'], 'penalty': m['penalty'],
         'transfer_days': m['transfer_days'], 'order': m['order'], 'research': ts(m['research']),
         'samples': [{'item': ts(i), 'count': n} for i, n in m['samples']]}
    if 'needs' in m:
        o['needs'] = m['needs']
    dump(os.path.join(D, 'ts_missions', mid + '.json'), o)
    L['mission.%s.%s' % (NS, mid)] = m['name']

# ====================================================================== tags
tag_add(os.path.join(MC, 'tags/block/mineable/pickaxe.json'), [ts('active_magnetic_shield'), ts('radiation_pressure_balance')])
for kind in ('block', 'item'):
    tag_add(os.path.join(D, 'tags', kind, 'machines.json'), [ts('radiation_pressure_balance')])
tag_add(os.path.join(D, 'tags/block/radiation_shielding.json'), [ts('active_magnetic_shield')])

# ====================================================================== text the Java reads
L['message.%s.planner_needs' % NS] = 'This mission needs %s in your inventory.'
L['screen.%s.planner.needs' % NS] = 'Needs: %s'
L['tooltip.%s.laser_sail_dv' % NS] = 'Δv %s km/s (pushed by a ground laser; probes only)'

# ====================================================================== the Codex
E = {}
E['fluids/radiation_pressure'] = {'title': 'Radiation Pressure', 'icon': ts('laser_sail'), 'category': 'fluids', 'research': 'radiation_pressure', 'pages': [
    spot(['radiation_pressure_balance'], "F = (1 + R)·I·cos²θ/c. The $(thing)Radiation Pressure Balance$() measures the push of light on a film and how hot it gets. Tune the light and the angle until the film's acceleration is what the sail needs.", 'Radiation Pressure Balance'),
    spot(['light_sail', 'solar_statite', 'laser_sail'], "Light Sails (aluminised film, about 1.3 mm/s² at Earth's distance) and Solar Statites (light enough to hover: 5.9 mm/s², the Sun's pull at 1 AU) by a newer route; and the $(item)Laser Sail$(): a gram of dielectric mirror pushed at ten thousand g, if it absorbs little enough not to melt. In a Mission Bay it gives a probe 200 km/s with no propellant.", 'Sails')]}
E['fluids/solar_magnetism'] = {'title': "The Sun's Magnetism", 'icon': ts('sunspot_magnetogram'), 'category': 'fluids', 'research': 'solar_magnetism', 'pages': [
    text("f = (Ts/T)⁴. Sunspots are where the field breaks through, cooler and so darker. The $(thing)Heliograph$() gains a magnetograph: Δλ = 4.67e-13·g·λ²·B. Choose the observing wavelength so the split line is wide enough to see and narrow enough to stay in the passband."),
    spot(['sunspot_magnetogram', 'coronal_loop_plasma'], "Sunspot Magnetograms from the Heliograph. A Mission Bay's probe to the $(thing)corona$() (20 km/s, with gravity assists) brings back Coronal Loop Plasma, which the Stellar Plasma Crucible turns into stellar plasma.", 'Magnetograms')]}
E['fluids/magnetospheres'] = {'title': 'Magnetospheres', 'icon': ts('active_magnetic_shield'), 'category': 'fluids', 'research': 'magnetospheres', 'pages': [
    text("sin²α = B/Bm. A field bottles charged particles: they bounce between the poles, all but those inside the loss cone. The Heliograph charts the $(item)Magnetopause$(); a probe to the $(thing)radiation belts$() (2.5 km/s) brings back Trapped Proton Flux; the Pulsed Magnet Lab gains a magnetic mirror's loss cone."),
    spot(['active_magnetic_shield'], "The $(item)Active Magnetic Shield$(), a station module: within eight blocks of it a solar storm's dose is a tenth (and it adds to a Storm Shelter's). Aboard the station and in the Accretion Frontier.", 'Active Magnetic Shield')]}
E['fluids/asteroid_taxonomy'] = {'title': 'Asteroids by Type', 'icon': ts('platinum_group_nugget'), 'category': 'fluids', 'research': 'asteroid_taxonomy', 'pages': [
    text("D = 1329 km/√p · 10^(−H/5). Find your target first: the $(thing)Reflecting Telescope$() makes an $(item)Asteroid Ephemeris$() (a limiting magnitude of 17 or fainter). A Mission Bay needs one, in your inventory, for each asteroid or comet mission."),
    spot(['carbonaceous_regolith', 'platinum_group_nugget'], "C-type (5.5 km/s): Carbonaceous Regolith, which the Electrolytic Cell turns into hydrogen and organic residue. M-type (9 km/s): Platinum-Group Nuggets and Meteoric Iron; a nugget makes two Platinum-Rhodium Gauze in the Chemical Synthesis Reactor. (The S-type is the near-Earth asteroid of Sample Return.)", 'Types')]}
E['fluids/comets'] = {'title': 'Comets', 'icon': ts('cometary_ice'), 'category': 'fluids', 'research': 'comets', 'pages': [
    text("T = aJ/a + 2√((a/aJ)(1 − e²)). A Jupiter-family comet (11 km/s, needs an Asteroid Ephemeris) gives Cometary Ice and Cometary Dust."),
    spot(['cometary_ice', 'cometary_dust'], "The Gas Centrifuge Cascade takes deuterium from the ice (three times richer than the oceans'); the Abiogenesis Crucible steeps the dust's glycine and organics in water into Primordial Soup.", 'Samples')]}
E['fluids/nebular_gases'] = {'title': "The Nebula's Gases", 'icon': ts('helium_3_cell'), 'category': 'fluids', 'research': 'nebular_gases', 'pages': [
    text("α = exp(Δm·v²/2RT). The $(thing)Bussard Ramscoop$() scoops Nebular Gas high in the Accretion Frontier. The $(thing)Gas Centrifuge Cascade$() separates it into hydrogen, deuterium and carbon monoxide, or into helium and $(item)helium-3$(); a probe into $(thing)Jupiter's atmosphere$() (15 km/s) brings back more."),
    spot(['helium_3_cell'], "Helium-3 fuses with deuterium in the Tokamak into helium and a proton, with no neutrons: it needs a triple product of 40 or more, and gives twice D-T's power.", 'Helium-3')]}
OPS = {'radiation_pressure_balance': ("Shine light on the film until it feels the push the sail needs, without melting it.",
                                      'Light intensity, Angle of incidence', 'fibre drift: the reading wanders by 1%·d.',
                                      'over its melting point the film fails (stops).', 'a stronger beam, sooner.',
                                      ['F = (1 + R)·I·cos²θ/c', 'a = F/σ', 'T = ((1 − R)·I·cos θ/2εσ_SB)^¼'], 'radiation_pressure')}
for mid, (intro, sets, dist, lim, rate, eqs, rs) in OPS.items():
    E['operation/' + mid] = {'title': T.MACHINES[mid]['name'], 'icon': ts(mid), 'category': 'operation', 'research': rs,
                             'pages': op_pages(mid, intro, sets, dist, lim, rate, eqs)}
NODES = {'physics': [{'entry': 'fluids/radiation_pressure', 'x': 53, 'y': 87, 'parents': ['fluids/satellite_orbits']},
                     {'entry': 'fluids/solar_magnetism', 'x': 55, 'y': 85, 'parents': ['fluids/space_solar_power']},
                     {'entry': 'fluids/magnetospheres', 'x': 55, 'y': 87, 'parents': ['fluids/solar_magnetism']},
                     {'entry': 'fluids/asteroid_taxonomy', 'x': 49, 'y': 91, 'parents': ['fluids/sample_return']},
                     {'entry': 'fluids/comets', 'x': 51, 'y': 91, 'parents': ['fluids/asteroid_taxonomy']},
                     {'entry': 'fluids/nebular_gases', 'x': 53, 'y': 91, 'parents': ['fluids/comets']}],
         'machines': [{'entry': 'operation/radiation_pressure_balance', 'x': 34, 'y': 22, 'parents': []}]}
codex(E, NODES)

finish()
print('research', len(T.RESEARCH), 'machines', len(T.MACHINES), 'recipes', len(T.RECIPES))
