"""
Round 52 (1.11.0, "Fields & Nuclei"): the rest of the 1.1 design's Electromagnetism and Nuclear Physics branches, and
the two astrophysics nodes that need nuclear physics first (Kilonovae, Gamma-Ray Bursts).

Electromagnetism: Electrodynamics (waveguides, Faraday cage mesh, mu-metal; the Hypervelocity Impact Forge - a railgun),
Magnetic Confinement (the Pulsed Magnet Lab), the Quantum Hall Effect (Hall bar chips, cut by the Laser Cutter), Magnetars
and Magnetic Monopoles.
Nuclear: Fusion Ignition (the Tokamak), Nuclear Astrophysics, Transuranic Synthesis (the Irradiation Chamber's
californium) and Nuclear Isomers (hafnium-178m2 and the Isomer Trigger); heavy water, deuterium, tritium, lithium-6;
spent fuel vitrified in the Decontamination Scrubber.
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
    # electromagnetism
    I('litz_wire', 'Litz Wire', 'coil', '#603010', '#ffa060',
      'Hundreds of thin copper strands, each insulated and woven so every one takes its turn at the outside: at high frequency current crowds '
      'to a wire\'s skin, and this gives it more skin.'),
    I('mu_metal_shield', 'Mu-Metal Shield', 'plate', '#506060', '#c0d8d8',
      'Four parts nickel to one of iron, annealed in hydrogen: so permeable that a magnetic field would rather run through it than past what '
      'it shields. Carried in the off hand, it keeps you from being Magnetized.'),
    I('faraday_cage_mesh', 'Faraday Cage Mesh', 'hexgrid', '#503018', '#e0a060',
      'A fine copper mesh. Charges in it rearrange until the field inside is zero. Carried in the off hand, it shields you from Decoherence.'),
    I('waveguide', 'Waveguide', 'probe', '#604020', '#ffc080',
      'A rectangular copper pipe that carries microwaves the way a pipe carries water - no wave longer than twice its width gets through.'),
    I('bitter_plate', 'Bitter Plate', 'plate', '#704020', '#ffb070',
      'A copper disc cut with a slit and pierced with cooling holes. Stacked into a helix, Bitter plates make the strongest steady magnets '
      'there are.'),
    I('rail_armature', 'Rail Armature', 'pellet', '#605040', '#e0c080',
      'A copper slug that bridges a railgun\'s two rails: the current through it and the field of the rails push it out at kilometres a second.'),
    I('lonsdaleite', 'Lonsdaleite', 'gem', '#404850', '#e0f0ff',
      'Hexagonal diamond, made only in the shock of an impact - harder, in places, than diamond itself.'),
    I('hall_bar_chip', 'Hall Bar Chip', 'chip', '#202830', '#80ffc0',
      'A strip of two-dimensional electrons at a kelvin and ten tesla. Its Hall resistance sits on plateaus at exactly h/(νe²): a resistance '
      'standard set by nature.'),
    I('plasma_thruster_nozzle', 'Plasma Thruster Nozzle', 'emitter', '#283048', '#80c0ff',
      'A magnetic nozzle of lonsdaleite and iron: plasma pushed out along its field lines leaves faster than any chemical exhaust.'),
    # nuclear
    I('lithium_ingot', 'Lithium Ingot', 'ingot', '#a0a0a8', '#f0f0f8',
      'The lightest metal, won from brine. Soft enough to cut with a knife; it floats on oil and burns crimson.', tags=['c:ingots/lithium']),
    I('lithium_6_ingot', 'Lithium-6 Ingot', 'ingot', '#a0b0a8', '#f0fff0',
      'Lithium enriched in its lighter isotope. One neutron splits it into helium and tritium - which is how a fusion blanket breeds its fuel.'),
    I('thorium_232_pellet', 'Thorium-232 Pellet', 'pellet', '#404040', '#c0c0b0',
      'Three times as common as uranium, and not fissile itself - but a neutron makes it uranium-233, which is.', tags=['c:pellets/thorium']),
    I('polonium_210_source', 'Polonium-210 Source', 'pellet', '#303828', '#c0ff80',
      'A speck of polonium giving off so many alpha particles that it is warm to the touch. Harmless at arm\'s length; deadly inside you.'),
    I('technetium_99m_vial', 'Technetium-99m Vial', 'contained', '#203040', '#80ffe0',
      'An excited nucleus that gives off one clean gamma ray and calms down within hours: the most used medical isotope in the world.'),
    I('californium_252_neutron_source', 'Californium-252 Neutron Source', 'pellet', '#302010', '#ffa040',
      'A microgram of californium splits by itself often enough to give off two million neutrons a second.'),
    I('spent_fuel_rod', 'Spent Fuel Rod', 'pellet', '#302818', '#a0ff40',
      'A fuel rod after three years in a reactor: still mostly uranium, but laced with fission products a million times more radioactive.'),
    I('nuclear_waste_glass', 'Nuclear Waste Glass', 'crystal', '#203020', '#80c060',
      'Fission products melted into borosilicate glass: it will hold them for the hundred thousand years they need.'),
    I('fusion_blanket_tile', 'Fusion Blanket Tile', 'plate', '#304030', '#c0ffc0',
      'Lithium-6 behind lead and graphite. It catches the fusion neutrons, turns their energy into heat, and breeds tritium as it does.'),
    I('hafnium_178m2_isomer', 'Hafnium-178m2 Isomer', 'crystal', '#303040', '#e0ff80',
      'A nucleus that holds 2.4 MeV in a high-spin state for thirty-one years: a gram of it stores as much as a tonne of TNT.'),
    I('hafnium_dust', 'Hafnium Dust', 'dots', '#404048', '#c0c0d0', 'What is left when an isomer gives up its energy: ordinary hafnium.'),
    # astrophysics
    I('kilonova_ash', 'Kilonova Ash', 'dots', '#302010', '#ffd060',
      'Gold, platinum and lanthanides made in a second by the rapid neutron capture of a neutron-star merger, and thrown out at a fifth of '
      'the speed of light.'),
    I('burst_afterglow_crystal', 'Burst Afterglow Crystal', 'crystal', '#202040', '#ffffff',
      'The fading afterglow of a gamma-ray burst - the brightest explosions there are, seen across the Universe.'),
]
BLOCKS = [
    B('tokamak_casing', 'Tokamak Casing', 'casing2', '#302830', '#c080b0'),
    B('vacuum_vessel', 'Vacuum Vessel', 'plating', '#404048', '#a0a0b0'),
    B('toroidal_field_coil', 'Toroidal Field Coil', 'coilblock', '#302040', '#c080ff', light=5),
    B('central_solenoid', 'Central Solenoid', 'coilblock', '#203040', '#80c0ff', light=5),
    B('pulsed_lab_casing', 'Pulsed Lab Casing', 'casing', '#303840', '#80b0e0'),
    B('bitter_plate_stack', 'Bitter Plate Stack', 'coilblock', '#603010', '#ffb070', light=4),
    B('rail_conductor', 'Rail Conductor', 'beampipe', '#603818', '#ffc080', light=3),
    B('armature_breech', 'Armature Breech', 'injector', '#404040', '#e0c080', light=2),
    B('impact_chamber', 'Impact Chamber', 'casing2', '#383838', '#ffe0a0'),
    B('burst_casing', 'Burst Observatory Casing', 'casing', '#283040', '#c0c0e0'),
    B('scintillator_panel', 'Scintillator Panel', 'detector', '#203050', '#a0c0ff', light=6),
]
FLUIDS = [
    FL('heavy_water', 'Heavy Water', '#304060', '#80a0e0', temp=300, density=1107, viscosity=1250, art='condensate'),
    FL('deuterium', 'Deuterium', '#304060', '#a0c0ff', gas=True, temp=300, density=1, viscosity=200, art='gas'),
    FL('tritium', 'Tritium', '#204030', '#a0ffc0', gas=True, temp=300, light=4, density=1, viscosity=200, art='gas'),
    FL('ferrofluid', 'Ferrofluid', '#101010', '#404050', temp=300, density=1300, viscosity=1500, art='exotic'),
    FL('liquid_sodium', 'Liquid Sodium', '#806020', '#ffe080', temp=600, light=3, density=927, viscosity=700, art='condensate'),
    FL('magnetized_plasma', 'Magnetized Plasma', '#402048', '#ff80ff', gas=True, temp=1_000_000, light=12, density=1, viscosity=200, art='plasma'),
]

KEY_ADD = {
    'ж': ['tokamak_casing'], 'з': ['tokamak_casing', 'interface_port'], 'и': ['vacuum_vessel'], 'к': ['toroidal_field_coil'], 'л': ['central_solenoid'],
    'м': ['pulsed_lab_casing'], 'н': ['pulsed_lab_casing', 'interface_port'], 'о': ['bitter_plate_stack'],
    'п': ['rail_conductor'], 'р': ['armature_breech'], 'с': ['impact_chamber'], 'т': ['impact_chamber', 'interface_port'],
    'у': ['burst_casing'], 'ф': ['burst_casing', 'interface_port'], 'х': ['scintillator_panel'],
}


# =================================================================================== SHAPES
def tokamak():
    """A torus: the vacuum vessel round its ring of plasma, twelve D-shaped toroidal field coils threaded over it, the
    central solenoid up the middle, all on a round footing. Windows in the vessel's crown to watch the plasma by."""
    s = shapes.Shape(15, 9, 15)
    c, R, yc = 7, 4.5, 4
    for x in range(15):
        for z in range(15):
            d = math.hypot(x - c, z - c)
            if 3.4 <= d <= 7.0:
                s.put(x, 0, z, 'ж')
            for y in range(1, 8):
                rr = math.hypot(d - R, y - yc)
                if 0.9 <= rr <= 1.75:
                    top = y > yc and abs(d - R) < 0.8
                    s.put(x, y, z, 'G' if top and (x + z) % 2 == 0 else 'и')
                elif rr < 0.9:
                    s.put(x, y, z, '.')
    for k in range(12):                                                   # the toroidal field coils
        a = k * math.pi / 6
        for t in range(24):
            th = t * math.pi / 12
            r = R + 2.3 * math.cos(th) * (1.15 if math.cos(th) > 0 else 0.85)
            y = yc + 2.6 * math.sin(th)
            x, z = c + math.cos(a) * r, c + math.sin(a) * r
            if 0 <= round(y) < 9: s.put(x, y, z, 'к')
    s.column(c, c, 0, 8, 'л')                                             # the central solenoid
    s.center = (7.0, 4.0, 7.0)
    s.ports('ж', 'з', 4, y=0)
    s.v[(c, 0, 0)] = 'C'
    s.ctrl = (c, 0, 0)
    return s


def pulsed_magnet_lab():
    """A room that grows with the coil: a floor, one storey per stack of Bitter plates in the middle (1 to 7), racks of
    capacitor banks standing outside both long walls to dump into it, windows in the end walls, and on the roof the coil's
    head with its cooling vents."""
    s = shapes.Shape(9, 4, 7)
    c, zc = 4, 3
    s.box(1, 0, 0, 7, 0, 6, 'м')
    for x in range(1, 8):
        for z in range(7):
            if x in (1, 7) or z in (0, 6):
                s.put(x, 1, z, 'G' if (x in (1, 7) and z in (2, 3, 4)) else 'м')
            else:
                s.put(x, 1, z, '.')
    for z in (1, 2, 4, 5):
        s.put(0, 1, z, 'Э')                                               # capacitor racks outside, a bank per storey
        s.put(8, 1, z, 'Э')
    s.put(c, 1, zc, 'о')                                                  # the coil
    s.box(1, 2, 0, 7, 2, 6, 'м')
    s.put(c, 3, zc, 'о')                                                  # its head through the roof
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        s.put(c + dx, 3, zc + dz, 'Э')
    s.repeat = (1, 1, 6)
    s.center = (4.0, 1.5, 3.0)
    s.ports('м', 'н', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


def hypervelocity_impact_forge():
    """A long line: the breech at one end, two copper rails down the bore with capacitor banks along both sides, clamps
    over the rails, and the impact chamber at the far end."""
    s = shapes.Shape(16, 4, 5)
    s.box(0, 0, 0, 12, 0, 4, 'с')
    for x in range(1, 13):
        s.put(x, 1, 1, 'п')
        s.put(x, 1, 3, 'п')
        s.put(x, 1, 2, '.')
        if x % 2 == 0:
            s.put(x, 1, 0, 'Э')
            s.put(x, 1, 4, 'Э')
            s.put(x, 2, 1, 'с')
            s.put(x, 2, 2, 'с')
            s.put(x, 2, 3, 'с')
        else:
            s.put(x, 2, 2, 'G')
    s.put(0, 1, 2, 'р')                                                   # the breech
    s.put(0, 1, 1, 'с'); s.put(0, 1, 3, 'с')
    s.box(13, 0, 0, 15, 3, 4, 'с', hollow=True)                           # the impact chamber
    s.put(13, 1, 2, '.')
    s.put(14, 2, 0, 'G'); s.put(14, 2, 4, 'G')
    s.center = (7.0, 1.5, 2.0)
    s.ports('с', 'т', 4, y=0)
    s.v[(6, 1, 0)] = 'C'
    s.ctrl = (6, 1, 0)
    return s


def burst_observatory():
    """An open frame: a mast with twelve scintillator panels on arms, each facing a different way so between them they see
    the whole sky (as a gamma-ray burst monitor does), and a coded-mask crown on top for the afterglow's position."""
    s = shapes.Shape(9, 10, 9)
    c = 4
    s.box(0, 0, 0, 8, 0, 8, 'у')
    s.column(c, c, 1, 8, 'у')
    dirs = [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)]
    for k, (dx, dz) in enumerate(dirs + dirs[:4]):
        y = 2 + (k % 6)
        r = 2 if k < 8 else 3
        for i in range(1, r):
            s.put(c + dx * i, y, c + dz * i, 'у')                         # the arm
        s.put(c + dx * r, y, c + dz * r, 'х')                             # the panel
    s.box(c - 1, 9, c - 1, c + 1, 9, c + 1, 'х')                          # the crown
    s.put(c, 9, c, 'G')
    s.center = (4.0, 6.0, 4.0)
    s.ports('у', 'ф', 4, y=0)
    s.v[(c, 0, 0)] = 'C'
    s.ctrl = (c, 0, 0)
    return s


shapes.BUILDERS.update({'tokamak': tokamak, 'pulsed_magnet_lab': pulsed_magnet_lab, 'hypervelocity_impact_forge': hypervelocity_impact_forge,
                        'burst_observatory': burst_observatory})

MACHINE_TEXT = {
    'tokamak': ('Tokamak', 'A ring of hydrogen plasma held off its walls by a twisted magnetic field, hot enough for deuterium and tritium to fuse.'),
    'pulsed_magnet_lab': ('Pulsed Magnet Lab', 'A coil of Bitter plates and a room of capacitors dumped into it: a field of up to a hundred tesla, '
                                               'for a few milliseconds.'),
    'hypervelocity_impact_forge': ('Hypervelocity Impact Forge', 'A railgun: an armature shot down two rails at kilometres a second into a '
                                                                 'target, where the shock makes what only impacts can.'),
    'burst_observatory': ('Burst Observatory', 'Twelve scintillators facing every way at once, waiting for the brightest explosions in the '
                                               'Universe.'),
    'laser_cutter': ('Laser Cutter', 'A focused beam that cuts copper, silicon and diamond to a micron.'),
    'isomer_trigger': ('Isomer Trigger', 'A gamma pulse knocks a hafnium-178m2 nucleus out of its long-lived state, and all its stored energy '
                                         'comes out at once.'),
    'decontamination_scrubber': ('Decontamination Scrubber', 'Melts radioactive waste into glass, and takes the uranium back out of spent fuel.'),
    'irradiation_chamber': ('Irradiation Chamber', 'Holds a target in the neutron flux of a source until nuclei catch neutrons and become '
                                                   'something else.'),
}
MACHINE_CAT = {'tokamak': 'nuclear', 'pulsed_magnet_lab': 'electromagnetism', 'hypervelocity_impact_forge': 'electromagnetism',
               'burst_observatory': 'astrophysics', 'laser_cutter': 'electromagnetism', 'isomer_trigger': 'nuclear',
               'decontamination_scrubber': 'nuclear', 'irradiation_chamber': 'nuclear'}
MACHINE_RESEARCH = {'tokamak': 'fusion_ignition', 'pulsed_magnet_lab': 'magnetic_confinement', 'hypervelocity_impact_forge': 'electrodynamics',
                    'burst_observatory': 'gamma_ray_bursts', 'laser_cutter': 'laser_physics', 'isomer_trigger': 'nuclear_isomers',
                    'decontamination_scrubber': 'radiochemistry', 'irradiation_chamber': 'nuclear_fission'}
TIER = {'tokamak': 3, 'pulsed_magnet_lab': 3, 'hypervelocity_impact_forge': 2, 'burst_observatory': 4, 'laser_cutter': 2, 'isomer_trigger': 4,
        'decontamination_scrubber': 2, 'irradiation_chamber': 2}
_S = 'spacetime_casing'
MC = {
    'tokamak': (['CVC', 'SHS', 'CVC'], {'C': 'toroidal_field_coil', 'V': 'vacuum_vessel', 'S': 'central_solenoid', 'H': _S}),
    'pulsed_magnet_lab': (['BEB', 'ESE', 'BEB'], {'B': 'bitter_plate_stack', 'E': 'capacitor_bank', 'S': _S}),
    'hypervelocity_impact_forge': (['RRR', 'ESA', 'III'], {'R': 'rail_conductor', 'E': 'capacitor_bank', 'S': _S, 'A': 'armature_breech',
                                                           'I': 'impact_chamber'}),
    'burst_observatory': (['PPP', 'PSP', 'CCC'], {'P': 'scintillator_panel', 'S': _S, 'C': 'burst_casing'}),
    'laser_cutter': (['ILI', 'RSR', 'III'], {'I': 'minecraft:iron_ingot', 'L': 'laser_rod', 'R': 'minecraft:redstone', 'S': _S}),
    'isomer_trigger': (['LCL', 'LSL', 'LLL'], {'L': 'lead_shielding_plate', 'C': 'cobalt_57_source', 'S': _S}),
    'decontamination_scrubber': (['GFG', 'LSL', 'GFG'], {'G': 'minecraft:glass', 'F': 'fused_silica', 'L': 'lead_shielding_plate', 'S': _S}),
    'irradiation_chamber': (['LGL', 'GSG', 'LRL'], {'L': 'lead_shielding_plate', 'G': 'graphite_moderator', 'S': _S, 'R': 'radium_salt'}),
}
EXTRA = {
    'tokamak': 'Burns Deuterium and Tritium (or Deuterium alone, far harder) for power, leaving warm helium. Set the plasma density, its '
               'temperature and the toroidal field. Ignition needs the Lawson triple product nTτ above 3×10²¹ keV·s/m³; the energy confinement '
               'time τ grows with the field. The plasma pressure must stay below 5% of the magnetic pressure (β), or it disrupts. The coils '
               'are superconducting: keep it below 20 K, and the field below 12.5 T.',
    'pulsed_magnet_lab': 'Set the turns, the current and how long the pulse lasts. The field is B = μ₀NI/L. Every storey adds copper and cooling '
                         'to the coil. All that current heats it (ΔT ∝ NI²t/size) - past 400 K it melts - and past 100 T the magnetic pressure '
                         'tears it apart.',
    'hypervelocity_impact_forge': 'Set the current and the armature\'s mass. The rails push with F = ½L′I²; the armature leaves at '
                                  'v = √(2FL/m) and hits with a pressure of about ½ρv². Each product forms only in its own pressure window. '
                                  'Each shot uses a Rail Armature.',
    'burst_observatory': 'Lower the trigger threshold and it sees more bursts - but noise sets it off falsely more often. Watch long enough to '
                         'be all but sure of catching one (90%), with few enough false triggers.',
    'laser_cutter': 'Cuts copper into waveguides, Bitter plates and armatures, silicon and gold into Hall bar chips, and lonsdaleite into '
                    'plasma thruster nozzles.',
    'isomer_trigger': 'Burns Hafnium-178m2 for a great deal of power. Needs a Cobalt-57 source in an input slot for the gamma pulse that '
                      'triggers it (never used up).',
    'decontamination_scrubber': 'Vitrifies fission products and spent fuel salt into Nuclear Waste Glass, and recovers the uranium from spent '
                                'fuel rods.',
    'irradiation_chamber': 'Needs a neutron source in an input slot (Radium Salt, or for the strongest flux Californium-252), never used up.',
}

_r12.model('tokamak', 'Tokamak',
           'Deuterium and tritium fuse when their ions are hot enough to get close: around 10-20 keV. A plasma of density n at temperature T '
           'loses its energy over a confinement time τ, which grows with the magnetic field holding it (here τ ∝ B²). It ignites when the '
           'Lawson triple product nTτ passes 3×10²¹ keV·s/m³. The plasma\'s pressure pushes on the field; past β = 5% of the magnetic '
           'pressure, it disrupts.',
           'Edge turbulence: the confinement time varies by ±5%·d.',
           [_P('n', 'Plasma density', '10²⁰ /m³', 0.1, 3, 0.01, 1, 'Ions in each cubic metre.'),
            _P('Ti', 'Ion temperature', 'keV', 1, 50, 0.01, 10, 'How hot the plasma is (1 keV is 11.6 million kelvin).'),
            _P('B', 'Toroidal field', 'T', 1, 13, 0.01, 5, 'The field holding the plasma off the wall.')],
           [_D('tau', 'Energy confinement time', 's', '0.06*B^2*(1 + 0.05*d)', 'τ ∝ B²', 'How long the plasma keeps its heat.'),
            _D('L3', 'Triple product', '10²¹ keV·s/m³', '0.1*n*Ti*tau', 'nTτ', 'Lawson\'s figure of merit. D-T ignites at 3.'),
            _D('beta', 'Plasma beta', '%', '8.04*n*Ti/B^2', 'β = 2μ₀nkT/B²', 'Plasma pressure against magnetic pressure.')],
           [_C('disrupt', 'damage', '5 - beta', 'The plasma has disrupted: its pressure beat the field', 'β < 5%'),
            _C('quench', 'stop', '12.5 - B', 'The superconducting coils have quenched (too much field)', 'B < 12.5 T'),
            _C('cryo', 'stop', '20 - T', 'The superconducting coils are too warm', 'T < 20 K')],
           rate='clamp(L3/Lref, 0, 2)', rate_formula='output = nTτ / nTτ_rated', rate_desc='Power follows the triple product.',
           heat='0.002*n*Ti', idle_heat='0.05', rvars=[_V('Lref', 'Rated triple product', 6)], kappa=60, heat_capacity=200)

_r12.model('pulsed_magnet_lab', 'Pulsed Magnet Lab',
           'A capacitor bank dumps a current I through a coil of N turns and length L: the field is B = μ₀NI/L. The coil\'s resistance turns '
           'the current into heat, ΔT ∝ NI²t over the pulse t; the magnetic pressure B²/2μ₀ pushes the coil outward. Every storey of the '
           'room is another stack of plates and its cooling: more copper to soak up the heat.',
           'Capacitor charge: the current varies by ±2%·d.',
           [_P('N', 'Turns', '', 10, 200, 1, 50, 'Turns of the Bitter-plate helix.'),
            _P('I', 'Peak current', 'kA', 1, 100, 0.01, 20, 'The current the capacitors dump.'),
            _P('t', 'Pulse length', 'ms', 1, 100, 0.01, 10, 'How long the field is held.')],
           [_D('Lc', 'Coil length', 'm', '0.1', 'L', 'The coil\'s length (10 cm).'),
            _D('Bf', 'Peak field', 'T', '0.0012566*N*I*(1 + 0.02*d)/Lc', 'B = μ₀NI/L', 'The field at the centre of the coil.'),
            _D('dT', 'Coil heating', 'K', '0.0004*N*I^2*t/size', 'ΔT ∝ NI²t', 'How much the pulse heats the copper.')],
           [_C('melt', 'damage', '400 - dT', 'The coil is melting', 'ΔT < 400 K'),
            _C('burst', 'damage', '100 - Bf', 'The magnetic pressure is tearing the coil apart', 'B < 100 T')],
           rate='clamp(4/(t + 1), 0.25, 2)', rate_formula='rate = 4/(t + 1 ms)', rate_desc='Shorter pulses recharge faster.',
           heat='0.0001*N*I^2', idle_heat='0', kappa=40, heat_capacity=60)

_r12.model('hypervelocity_impact_forge', 'Hypervelocity Impact Forge',
           'A current I flows up one rail, through the armature and back down the other. The rails\' field pushes the armature with '
           'F = ½L′I² (L′ = 0.5 µH/m) along their 10 m; it leaves at v = √(2FL/m). Hitting the target it drives a shock of pressure '
           'P ≈ ½ρv². Shock synthesis needs the right pressure: too low and nothing changes, too high and the target vaporises.',
           'Rail contact: the force varies by ±3%·d.',
           [_P('I', 'Current', 'MA', 0.05, 5, 0.001, 0.5, 'The current through the rails.'),
            _P('m', 'Armature mass', 'g', 10, 1000, 0.1, 100, 'How heavy the armature is.')],
           [_D('F', 'Rail force', 'MN', '0.25*I^2*(1 + 0.03*d)', 'F = ½L′I²', 'The push on the armature.'),
            _D('v', 'Muzzle speed', 'm/s', 'sqrt(2*F*1e6*10/(m/1000))', 'v = √(2FL/m)', 'How fast it leaves the rails.'),
            _D('P', 'Impact pressure', 'GPa', '4e-6*v^2', 'P ≈ ½ρv²', 'The shock pressure in the target (ρ = 8 g/cm³).')],
           [_C('erode', 'damage', '3 - I', 'The rails are eroding under the arc', 'I < 3 MA')],
           rate='clamp(0.8/(0.2 + I), 0.25, 2)', rate_formula='rate = 0.8/(0.2 + I)', rate_desc='Smaller currents recharge faster.',
           heat='0.5*I', idle_heat='0', kappa=40, heat_capacity=60)

_r12.model('burst_observatory', 'Burst Observatory',
           'Gamma-ray bursts come from anywhere in the sky, about once a day bright enough to catch. A lower trigger threshold Eth catches '
           'fainter ones (rate ∝ e^(−Eth/50 keV)) - and more noise. Over a watch of t hours the chance of catching at least one is '
           '1 − e^(−N), N the bursts expected; the false triggers grow as t·e^(−Eth/8 keV).',
           'Background: the noise rate varies by ±20%·d.',
           [_P('Eth', 'Trigger threshold', 'keV', 10, 200, 0.1, 50, 'The least energy a flash must deposit to count.'),
            _P('t', 'Watch', 'h', 1, 400, 0.1, 48, 'How long it watches before it reports.')],
           [_D('R', 'Burst rate', '/day', '1.5*exp(-Eth/50)', 'R ∝ e^(−Eth/50)', 'Bursts a day above the threshold.'),
            _D('Pdet', 'Detection chance', '', '1 - exp(-R*t/24)', '1 − e^(−N)', 'The chance of catching at least one.'),
            _D('FA', 'False triggers', '', 't/24*100*exp(-Eth/8)*(1 + 0.2*d)', 'FA ∝ t·e^(−Eth/8)', 'Noise flashes mistaken for bursts.')],
           [],
           rate='clamp(240/t, 0.25, 2)', rate_formula='rate = 240/t', rate_desc='Shorter watches report sooner.',
           heat='0', idle_heat='0', kappa=40, heat_capacity=40)

_r12.WINDOWS['tokamak'] = {
    'r52_dt_fusion': ({'Lref': 6}, [('L3', 3, 1e9)]),
    'r52_blanket_breeding': ({'Lref': 6}, [('L3', 3, 1e9)]),
    'r52_dd_fusion': ({'Lref': 20}, [('L3', 15, 1e9)]),
    'r52_magnetized_plasma': ({'Lref': 2}, [('L3', 1, 1e9)]),
}
_r12.WINDOWS['pulsed_magnet_lab'] = {
    'r52_high_field_magnets': ({}, [('Bf', 40, 99)]),
    'r52_monopole_search': ({}, [('Bf', 80, 99)]),
}
_r12.WINDOWS['hypervelocity_impact_forge'] = {
    'r52_lonsdaleite': ({}, [('P', 15, 40)]),
    'r52_shock_moissanite': ({}, [('P', 50, 90)]),
}
_r12.WINDOWS['burst_observatory'] = {'r52_burst': ({}, [('Pdet', 0.9, 1), ('FA', 0, 0.2)])}
_r12.WINDOWS.setdefault('reflecting_telescope', {}).update({'r52_kilonova_ash': ({'mreq': 14}, [('mlim', 14, 99)])})
_r12.WINDOWS.setdefault('gas_centrifuge_cascade', {}).update({'r52_enrich_lithium': ({'dm': 0.001, 'xF': 0.075}, [('xP', 0.12, 0.2)])})
_r12.WINDOWS.setdefault('cyclotron', {}).update({'r52_hafnium_isomer': ({'Eth': 6}, [('E', 6, 99)])})

# =================================================================================== RESEARCH
RESEARCH = kit.place([
    ('electrodynamics', 'Electrodynamics',
     'Maxwell\'s four equations: charges make electric fields, there are no magnetic charges, changing fields make each other - and so a '
     'wave of field can carry itself across empty space, at exactly the speed of light. Unlocks waveguides, Faraday cage mesh, mu-metal and '
     'the Hypervelocity Impact Forge.',
     'waveguide', None, None, 60, ['electromagnetic_induction', 'calculus'], [('electrical_steel_lamination', 4), ('ferrite_magnet', 2)]),
    ('magnetic_confinement', 'Magnetic Confinement',
     'A charged particle spirals round a field line and cannot leave it. Twist the lines into a torus and a plasma can be held off every '
     'wall. Unlocks the Pulsed Magnet Lab and the Tokamak\'s coils.',
     'bitter_plate', None, None, 130, ['magnetohydrodynamics', 'superconductivity'], [('neodymium_magnet', 4), ('samarium_cobalt_magnet', 2)]),
    ('quantum_hall_effect', 'Quantum Hall Effect',
     'Electrons in a sheet at low temperature and strong field fill Landau levels; the Hall resistance locks onto plateaus at h/(νe²), exact '
     'to a part in a billion - because it counts a topological invariant. Unlocks Hall Bar Chips.',
     'hall_bar_chip', None, None, 140, ['superconductivity', 'topology'], [('silicon_boule', 2), ('laue_pattern', 2)]),
    ('magnetars', 'Magnetars',
     'A star\'s core collapses to a neutron star and its field is squeezed with it, flux conserved: a thousand times stronger than any magnet '
     'made on Earth, a billion times. Starquakes crack its crust.',
     'magnetar_crust_fragment', None, None, 160, ['magnetic_confinement', 'compact_stars'],
     [('magnetar_crust_fragment', 4), ('pulsar_timing_crystal', 2)]),
    ('magnetic_monopoles', 'Magnetic Monopoles',
     'Grand unified theories say magnetic charges must exist, made in the first instant - and Dirac showed that one would explain why every '
     'electric charge is a whole number of e. The Pulsed Magnet Lab can search for them in magnetar crust.',
     'magnetic_monopole', None, None, 260, ['magnetars', 'grand_unification'], [('magnetar_crust_fragment', 8), ('cosmic_ray_shard', 8)]),
    ('fusion_ignition', 'Fusion Ignition',
     'Deuterium and tritium fuse into helium and a neutron, giving 17.6 MeV - if they are hot enough, dense enough, and held long enough. '
     'Unlocks the Tokamak, heavy water, deuterium and tritium breeding.',
     'fusion_blanket_tile', None, None, 140, ['nuclear_fission', 'plasma_metallurgy', 'magnetic_confinement'],
     [('lithium_ingot', 4), ('graphite_moderator', 2)]),
    ('nuclear_astrophysics', 'Nuclear Astrophysics',
     'Stars fuse by tunnelling: their protons are far too cold to touch, but quantum mechanics lets a few through the Coulomb barrier. The '
     'Gamow energy sets which ones.',
     'kilonova_ash', None, None, 150, ['fusion_ignition', 'nucleosynthesis'], [('stellar_spectrum_plate', 4), ('lithium_6_ingot', 2)]),
    ('transuranic_synthesis', 'Transuranic Synthesis',
     'Beyond uranium every element is made: a nucleus is fed neutrons until it beta-decays up the table - plutonium, americium, curium, '
     'californium. Unlocks Californium-252.',
     'californium_252_neutron_source', None, None, 160, ['nuclear_fission', 'accelerator_physics'],
     [('plutonium_239_pellet', 2), ('uranium_238_pellet', 4)]),
    ('nuclear_isomers', 'Nuclear Isomers',
     'Some nuclei can sit in an excited state for years, held there by their spin. Hafnium-178m2 holds 2.4 MeV per nucleus for thirty-one '
     'years. Unlocks the Isomer Trigger.',
     'hafnium_178m2_isomer', None, None, 230, ['transuranic_synthesis', 'island_of_stability'],
     [('californium_252_neutron_source', 1), ('rare_earth_oxide', 4)]),
    ('kilonovae', 'Kilonovae',
     'When two neutron stars merge, a few hundredths of a solar mass of neutron-rich matter is flung out, and in a second it captures '
     'neutrons up to gold and platinum. Its radioactive glow fades over days.',
     'kilonova_ash', None, None, 170, ['compact_stars', 'nuclear_astrophysics'], [('gw_record_bns', 2), ('deep_sky_plate', 2)]),
    ('gamma_ray_bursts', 'Gamma-Ray Bursts',
     'For a few seconds, the brightest light in the Universe: a jet from a newborn black hole at nearly the speed of light, beamed straight '
     'at us. Unlocks the Burst Observatory.',
     'burst_afterglow_crystal', None, None, 240, ['kilonovae', 'accretion_physics'], [('kilonova_ash', 2), ('quasar_jet_filament', 2)]),
])

PUZZLES = {
    'electrodynamics': dict(
        goal='Maxwell found that waves of electric and magnetic field cross empty space at a rate set only by its permeability μ0 and its '
             'permittivity ε0. Write that rate c.',
        name='Speed of light from the vacuum', lhs='c',
        form='1 / sqrt ( μ0 * ε0 )',
        expr='1 / ( m * e ) ^ 0.5 / 1000000',
        ask='Find c in units of 10⁶ m/s, with m = 1.2566e-6 H/m for μ0 and e = 8.854e-12 F/m for ε0.',
        vars={'c': 'wave speed (10⁶ m/s)', 'μ0': 'permeability of the vacuum', 'ε0': 'permittivity of the vacuum', 'm': 'μ0 (H/m)', 'e': 'ε0 (F/m)'},
        given={'m': 1.2566e-6, 'e': 8.854e-12}, unit='×10⁶ m/s',
        method='From the curl equations in empty space, each field obeys the wave equation with speed² = 1/(μ0 ε0).',
        distractors=['2', 'π', 'h']),
    'magnetic_confinement': dict(
        goal='A magnetic field B pushes outward on whatever carries the current that makes it. Write the pressure P it exerts, with the '
             'permeability of the vacuum μ0.',
        name='Maxwell stress', lhs='P',
        form='B ^ 2 / ( 2 * μ0 )',
        expr='B ^ 2 / ( 2 * m ) / 1000000',
        ask='Find P in megapascals for B = 13 T (m = 1.2566e-6 H/m for μ0).',
        vars={'P': 'pressure (MPa)', 'B': 'field (T)', 'μ0': 'permeability of the vacuum', 'm': 'μ0 (H/m)'},
        given={'B': 13, 'm': 1.2566e-6}, unit='MPa',
        method='The energy stored in a field is B²/2μ0 per volume; pressure is energy per volume.',
        distractors=['π', '4', 'c']),
    'quantum_hall_effect': dict(
        goal='With ν Landau levels filled, a sheet of electrons carries a Hall resistance R fixed only by the quantum of action h and the '
             'elementary charge q. Write R.',
        name='Von Klitzing constant', lhs='R',
        form='h / ( ν * q ^ 2 )',
        expr='h / ( n * q ^ 2 )',
        ask='Find R in ohms for the first plateau, n = 1 (h = 6.626e-34 J·s, q = 1.602e-19 C).',
        vars={'R': 'Hall resistance (Ω)', 'h': 'Planck constant', 'ν': 'filling factor', 'q': 'elementary charge (C)', 'n': 'filling factor'},
        given={'h': 6.626e-34, 'n': 1, 'q': 1.602e-19}, unit='Ω',
        method='Each filled level carries a conductance of exactly q²/h, a topological (Chern) number times the quantum of conductance.',
        distractors=['π', '2', 'B']),
    'magnetars': dict(
        goal='A core of radius R threaded by a field B collapses to radius r, keeping all the field lines through it. Write the field b it ends '
             'with.',
        name='Flux freezing', lhs='b',
        form='B * ( R / r ) ^ 2',
        expr='B * ( R / r ) ^ 2 / u',
        ask='Find b in units of 10⁹ T (u = 1e9) for B = 100 T, R = 7e8 m, r = 1e4 m.',
        vars={'b': 'final field (10⁹ T)', 'B': 'initial field (T)', 'R': 'initial radius (m)', 'r': 'final radius (m)', 'u': '10⁹ T'},
        given={'B': 100, 'R': 7e8, 'r': 1e4, 'u': 1e9}, unit='×10⁹ T',
        method='The flux Φ = Bπr² through the star is frozen into its conducting plasma, so B grows as 1/r².',
        distractors=['π', '3', 'G']),
    'magnetic_monopoles': dict(
        goal='If a single magnetic charge g exists anywhere, the reduced quantum of action ħ and the elementary charge q fix its smallest '
             'value. Write g.',
        name='Dirac quantisation', lhs='g',
        form='2 * π * ħ / q',
        expr='h / q * u',
        ask='Find g in units of 10⁻¹⁵ Wb (u = 1e15) with h = 6.626e-34 J·s and q = 1.602e-19 C.',
        vars={'g': 'magnetic charge (10⁻¹⁵ Wb)', 'π': 'pi', 'ħ': 'reduced Planck constant', 'h': 'Planck constant (2πħ)', 'q': 'elementary charge (C)',
              'u': '10¹⁵'},
        given={'h': 6.626e-34, 'q': 1.602e-19, 'u': 1e15}, unit='×10⁻¹⁵ Wb',
        method='An electron carried round the monopole must come back with the same phase: q·g/ħ must be a whole multiple of 2π.',
        distractors=['2', 'π', 'c']),
    'fusion_ignition': dict(
        goal='A plasma of n deuterium-tritium pairs per cubic metre reacts at the rate n²/4·σ; each reaction gives an energy E. Write the power '
             'P each cubic metre makes.',
        name='Fusion power density', lhs='P',
        form='n ^ 2 / 4 * σ * E',
        expr='n ^ 2 / 4 * s * E / u',
        ask='Find P in MW/m³ (u = 1e6) for n = 1e20 /m³, s = 3e-22 m³/s (the reactivity near 15 keV) and E = 2.82e-12 J (17.6 MeV).',
        vars={'P': 'power density (MW/m³)', 'n': 'ions per m³', 'σ': 'reactivity ⟨σv⟩', 's': 'reactivity (m³/s)', 'E': 'energy per reaction (J)',
              'u': '10⁶'},
        given={'n': 1e20, 's': 3e-22, 'E': 2.82e-12, 'u': 1e6}, unit='MW/m³',
        method='Half the ions are deuterium and half tritium: n/2 of each, so the rate per volume is (n/2)²⟨σv⟩.',
        distractors=['2', 'c', 'k']),
    'nuclear_astrophysics': dict(
        goal='Two nuclei of charges Z and z tunnel through their Coulomb barrier with an energy scale set by their reduced mass m, the speed '
             'of light c and the fine-structure constant α. Write that energy E.',
        name='Gamow energy', lhs='E',
        form='2 * m * c ^ 2 * ( π * α * Z * z ) ^ 2',
        expr='2 * m * c ^ 2 * ( 3.14159 * a * Z * z ) ^ 2 / k',
        ask='Find E in keV (k = 1.602e-16 J) for two protons: m = 8.36e-28 kg, c = 2.998e8 m/s, a = 0.0073, Z = 1, z = 1.',
        vars={'E': 'energy (keV)', 'm': 'reduced mass (kg)', 'c': 'speed of light', 'π': 'pi', 'α': 'fine-structure constant', 'a': 'α',
              'Z': 'charge number', 'z': 'charge number', 'k': '1 keV (J)'},
        given={'m': 8.36e-28, 'c': 2.998e8, 'a': 0.0073, 'Z': 1, 'z': 1, 'k': 1.602e-16}, unit='keV',
        method='The tunnelling probability goes as e^(−√(E_G/E)); E_G collects every constant in the WKB exponent.',
        distractors=['h', 'G', '4']),
    'transuranic_synthesis': dict(
        goal='N nuclei of a sample with half-life τ are left to wait a time t. Write how many n are left.',
        name='Radioactive decay law', lhs='n',
        form='N / 2 ^ ( t / τ )',
        expr='N / 2 ^ ( t / T )',
        ask='Find n for N = 1000 nuclei of californium-252 (T = 2.645 years) after t = 5.29 years.',
        vars={'n': 'nuclei left', 'N': 'nuclei at the start', 't': 'time waited (years)', 'τ': 'half-life', 'T': 'half-life (years)'},
        given={'N': 1000, 't': 5.29, 'T': 2.645}, unit='',
        method='Every half-life halves what is left: after t/τ of them, N is divided by 2 that many times.',
        distractors=['e', '3', 'λ']),
    'nuclear_isomers': dict(
        goal='Each nucleus of an isomer stores an energy E; a mole of them weighs M and holds N of them. Write the energy U each kilogram '
             'stores.',
        name='Spin-trap capacity', lhs='U',
        form='E * N / M',
        expr='E * N / M / u',
        ask='Find U in GJ/kg (u = 1e9) for hafnium-178m2: E = 3.919e-13 J (2.446 MeV), N = 6.022e23 /mol, M = 0.178 kg/mol.',
        vars={'U': 'energy per kg (GJ/kg)', 'E': 'energy per nucleus (J)', 'N': 'Avogadro constant', 'M': 'molar mass (kg/mol)', 'u': '10⁹'},
        given={'E': 3.919e-13, 'N': 6.022e23, 'M': 0.178, 'u': 1e9}, unit='GJ/kg',
        method='Nuclei per kilogram are N/M; each gives E.',
        distractors=['c', '2', 'h']),
    'kilonovae': dict(
        goal='Ejecta of mass M expanding at speed v stay opaque until their light can diffuse out through opacity κ. Write the time t its glow '
             'peaks, with the speed of light c.',
        name='Diffusion time of the ejecta', lhs='t',
        form='sqrt ( κ * M / ( 4 * π * v * c ) )',
        expr='( k * M / ( 12.5664 * v * c ) ) ^ 0.5 / 86400',
        ask='Find t in days for M = 9.945e28 kg (0.05 suns), v = 3e7 m/s, k = 1 m²/kg (lanthanide-rich), c = 2.998e8 m/s.',
        vars={'t': 'peak time (days)', 'κ': 'opacity', 'k': 'opacity (m²/kg)', 'M': 'ejecta mass (kg)', 'π': 'pi', 'v': 'speed (m/s)',
              'c': 'speed of light'},
        given={'k': 1, 'M': 9.945e28, 'v': 3e7, 'c': 2.998e8}, unit='days',
        method='Light escapes when the time to random-walk out (∝ κM/rc) equals the time since the blast (r/v): solve for t = r/v.',
        distractors=['G', '2', 'h']),
    'gamma_ray_bursts': dict(
        goal='A jet moving at speed v beams its light forward into a narrow cone. Write the half-angle θ of the cone, with the speed of light c.',
        name='Relativistic beaming angle', lhs='θ',
        form='sqrt ( 1 - ( v / c ) ^ 2 )',
        expr='( 1 - b ^ 2 ) ^ 0.5 * 57.2958',
        ask='Find θ in degrees for a jet with b = 0.9999944 (its speed as a fraction of c, a Lorentz factor of 300).',
        vars={'θ': 'half-angle (degrees)', 'v': 'jet speed', 'c': 'speed of light', 'b': 'v/c'},
        given={'b': 0.9999944}, unit='°',
        method='Aberration: light emitted at right angles in the jet\'s frame comes out at sin θ = 1/γ = √(1 − v²/c²) in ours.',
        distractors=['2', 'G', 'π']),
}

LC, IT, DS, IR, TK, PM, HF, BO = ('laser_cutting', 'isomer_triggering', 'decontamination', 'irradiation', 'fusion_power', 'pulsed_field',
                                  'shock_synthesis', 'burst_detection')
RECIPES = [
    # Laser Cutter
    ('waveguide', LC, [('minecraft:copper_ingot', 2)], [], [('waveguide', 2)], [], 256, 200, 'electrodynamics', 0),
    ('bitter_plate', LC, [('minecraft:copper_ingot', 2)], [], [('bitter_plate', 2)], [], 256, 200, 'magnetic_confinement', 0, ['minecraft:redstone']),
    ('rail_armature', LC, [('minecraft:copper_ingot', 1)], [], [('rail_armature', 2)], [], 256, 160, 'electrodynamics', 0, ['minecraft:iron_ingot']),
    ('hall_bar_chip', LC, [('silicon_boule', 1), ('minecraft:gold_ingot', 1)], [], [('hall_bar_chip', 4)], [], 512, 400, 'quantum_hall_effect', 0),
    ('plasma_thruster_nozzle', LC, [('lonsdaleite', 1), ('minecraft:iron_ingot', 2)], [], [('plasma_thruster_nozzle', 1)], [], 512, 400,
     'magnetohydrodynamics', 0),
    # Isomer Trigger (generator)
    ('hafnium_burst', IT, [('hafnium_178m2_isomer', 1)], [], [('hafnium_dust', 1)], [], 131072, 400, 'nuclear_isomers', 0, ['cobalt_57_source']),
    # Decontamination Scrubber
    ('vitrify_fragments', DS, [('fission_fragment_mix', 2), ('fused_silica', 1)], [], [('nuclear_waste_glass', 1)], [], 512, 400, 'radiochemistry', 0),
    ('vitrify_salt', DS, [('fused_silica', 1)], [('spent_fuel_salt', 1000)], [('nuclear_waste_glass', 2)], [], 512, 600, 'radiochemistry', 0),
    ('reprocess_rod', DS, [('spent_fuel_rod', 1), ('fused_silica', 1)], [], [('nuclear_waste_glass', 1), ('uranium_238_pellet', 1)], [], 1024, 600,
     'radiochemistry', 0),
    # Irradiation Chamber (the source sits in a slot as a catalyst)
    ('technetium', IR, [('uranium_235_pellet', 1)], [], [('technetium_99m_vial', 2), ('spent_fuel_rod', 1)], [], 512, 800, 'decay_chains', 0,
     ['radium_salt']),
    ('breed_tritium', IR, [('lithium_6_ingot', 1)], [], [], [('tritium', 250)], 512, 600, 'fusion_ignition', 0, ['radium_salt']),
    ('californium', IR, [('plutonium_239_pellet', 4)], [], [('californium_252_neutron_source', 1)], [], 2048, 4800, 'transuranic_synthesis', 4,
     ['radium_salt']),
    ('breed_plutonium', IR, [('uranium_238_pellet', 2)], [], [('plutonium_239_pellet', 1)], [], 1024, 1200, 'transuranic_synthesis', 0,
     ['californium_252_neutron_source']),
    # Tokamak (generator)
    ('dt_fusion', TK, [], [('deuterium', 100), ('tritium', 100)], [], [('warm_helium', 50)], 65536, 1200, 'fusion_ignition', 6),
    ('blanket_breeding', TK, [('fusion_blanket_tile', 1)], [('deuterium', 100), ('tritium', 100)], [], [('warm_helium', 50), ('tritium', 150)],
     65536, 1200, 'fusion_ignition', 6),
    ('dd_fusion', TK, [], [('deuterium', 200)], [], [('warm_helium', 50)], 32768, 1600, 'nuclear_astrophysics', 8),
    # Pulsed Magnet Lab
    ('high_field_magnets', PM, [('samarium_cobalt_blank', 4)], [], [('samarium_cobalt_magnet', 6)], [], 4096, 400, 'magnetic_confinement', 2,
     ['bitter_plate']),
    ('monopole_search', PM, [('magnetar_crust_fragment', 1), ('cosmic_ray_shard', 4)], [], [('magnetic_monopole', 1), ('magnetite', 2)], [],
     16384, 1600, 'magnetic_monopoles', 12, [], [0.08, 1.0]),
    # Hypervelocity Impact Forge
    ('lonsdaleite', HF, [('graphite_moderator', 1), ('rail_armature', 1)], [], [('lonsdaleite', 1)], [], 4096, 300, 'electrodynamics', 0),
    ('shock_moissanite', HF, [('minecraft:sand', 4), ('minecraft:coal', 4), ('rail_armature', 1)], [], [('moissanite', 1)], [], 4096, 400,
     'electrodynamics', 0),
    # Burst Observatory
    ('burst', BO, [('cosmic_ray_shard', 2), ('photographic_plate', 1)], [], [('burst_afterglow_crystal', 1)], [], 1024, 2400, 'gamma_ray_bursts', 15),
    # elsewhere
    ('kilonova_ash', 'astrophotography', [('photographic_plate', 1), ('gw_record_bns', 1)], [], [('kilonova_ash', 2)], [], 128, 1600, 'kilonovae', 8),
    ('polonium', 'radioactive_decay', [('radium_salt', 2)], [], [('polonium_210_source', 1)], [], 64, 1200, 'radiochemistry', 0),
    ('thorium', 'spectroscopy', [('monazite_sand', 2), ('spectral_plate', 1)], [], [('thorium_232_pellet', 1), ('rare_earth_oxide', 1)], [],
     20000, 300, 'radioactivity', 0),
    ('heavy_water', 'electrolysis', [], [('minecraft:water', 1000)], [], [('heavy_water', 10)], 2048, 400, 'isotope_separation', 0),
    ('deuterium', 'electrolysis', [], [('heavy_water', 100)], [], [('deuterium', 200)], 2048, 200, 'fusion_ignition', 0),
    ('lithium_from_brine', 'electrolysis', [], [('deep_brine', 1000)], [('lithium_ingot', 2)], [], 2048, 400, 'electrochemistry', 0),
    ('enrich_lithium', 'isotope_enrichment', [('lithium_ingot', 4)], [], [('lithium_6_ingot', 1)], [], 4096, 800, 'isotope_separation', 0),
    ('hafnium_isomer', 'cyclotron_irradiation', [('rare_earth_oxide', 2)], [], [('hafnium_178m2_isomer', 1)], [], 2048, 1600, 'nuclear_isomers', 6),
    ('ferrofluid', 'chemical_synthesis', [('magnetite', 2)], [('minecraft:water', 500)], [], [('ferrofluid', 500)], 1024, 400, 'ferromagnetism', 0),
    ('liquid_sodium', 'electrolysis', [('minecraft:kelp', 4)], [('deep_brine', 500)], [], [('liquid_sodium', 250)], 2048, 400, 'electrochemistry', 0),
    ('magnetized_plasma', TK, [], [('stellar_plasma', 100)], [], [('magnetized_plasma', 100)], 4096, 400, 'magnetic_confinement', 0),
]
SHAPED = [
    ('litz_wire', ['CSC', 'SCS', 'CSC'], {'C': 'minecraft:copper_ingot', 'S': 'minecraft:string'}, 'litz_wire', 4),
    ('mu_metal_shield', ['NNN', 'NEN', 'NNN'], {'N': 'core_iron', 'E': 'electrical_steel_lamination'}, 'mu_metal_shield', 1),
    ('faraday_cage_mesh', ['C C', ' C ', 'C C'], {'C': 'minecraft:copper_ingot'}, 'faraday_cage_mesh', 2),
    ('fusion_blanket_tile', ['LGL', 'GBG', 'LGL'], {'L': 'lead_shielding_plate', 'G': 'graphite_moderator', 'B': 'lithium_6_ingot'},
     'fusion_blanket_tile', 2),
    ('tokamak_casing', ['ILI', 'LSL', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'lead_shielding_plate', 'S': 'spacetime_casing'}, 'tokamak_casing', 4),
    ('vacuum_vessel', ['III', 'IGI', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'containment_field_glass'}, 'vacuum_vessel', 4),
    ('toroidal_field_coil', ['MLM', 'L L', 'MLM'], {'M': 'neodymium_magnet', 'L': 'litz_wire'}, 'toroidal_field_coil', 2),
    ('central_solenoid', ['LML', 'LML', 'LML'], {'M': 'neodymium_magnet', 'L': 'litz_wire'}, 'central_solenoid', 2),
    ('pulsed_lab_casing', ['III', 'IFI', 'III'], {'I': 'minecraft:iron_ingot', 'F': 'faraday_cage_mesh'}, 'pulsed_lab_casing', 4),
    ('bitter_plate_stack', ['B', 'B', 'B'], {'B': 'bitter_plate'}, 'bitter_plate_stack', 1),
    ('rail_conductor', ['CCC', 'III', 'CCC'], {'C': 'minecraft:copper_ingot', 'I': 'minecraft:iron_ingot'}, 'rail_conductor', 4),
    ('armature_breech', ['III', 'ARA', 'III'], {'I': 'minecraft:iron_ingot', 'A': 'rail_armature', 'R': 'minecraft:redstone_block'}, 'armature_breech', 1),
    ('impact_chamber', ['OIO', 'I I', 'OIO'], {'O': 'minecraft:obsidian', 'I': 'minecraft:iron_ingot'}, 'impact_chamber', 4),
    ('burst_casing', ['III', 'ISI', 'III'], {'I': 'minecraft:iron_ingot', 'S': 'spacetime_casing'}, 'burst_casing', 8),
    ('scintillator_panel', ['GCG', 'CPC', 'GCG'], {'G': 'minecraft:glass', 'C': 'cosmic_ray_shard', 'P': 'photographic_plate'}, 'scintillator_panel', 2),
]
SHAPELESS = []

ITEM_SOURCES = {M + 'thorium_232_pellet': ('th232', 0.0005), M + 'polonium_210_source': ('po210', 0.02), M + 'technetium_99m_vial': ('tc99m', 0.01),
                M + 'californium_252_neutron_source': ('cf252', 0.03), M + 'spent_fuel_rod': ('cs137', 0.3), M + 'nuclear_waste_glass': ('cs137', 0.002),
                M + 'hafnium_178m2_isomer': ('hf178m2', 0.01)}
LANG = {}


def apply(g):
    g['LANG'].update(LANG)
    kit.write_puzzles('r52', PUZZLES)
    kit.write_research(g, RESEARCH)
    kit.recipes(g, 'r52_', RECIPES)
    kit.crafting(g, 'r52_', SHAPED, SHAPELESS)


def apply_late(g):
    kit.pickaxe(g, BLOCKS)
    D = g['D']
    p = os.path.join(D, 'data_maps', 'item', 'radiation_source.json')
    t = json.load(open(p)) if os.path.exists(p) else {'values': {}}
    for k, (iso, s) in ITEM_SOURCES.items():
        t['values'][k] = {'isotope': iso, 'strength': s}
    g['w'](p, t)
    kit.extend_tag(g, os.path.join(D, 'tags', 'item', 'radioactive.json'), list(ITEM_SOURCES))
    kit.extend_tag(g, os.path.join(D, 'tags', 'item', 'strongly_radioactive.json'), [M + 'spent_fuel_rod', M + 'californium_252_neutron_source'])
    kit.extend_tag(g, os.path.join(D, 'tags', 'item', 'ferromagnetic.json'), [M + 'mu_metal_shield'])


def finish(g):
    te = g['text_entry']
    te('electromagnetism', 'r52_maxwell', 'Electrodynamics', 'waveguide', [
        'Maxwell\'s equations put electricity, magnetism and light into one theory: changing fields make each other, and a wave of field '
        'carries itself across empty space at $(thing)c = 1/√(μ₀ε₀)$().',
        'The $(item)Laser Cutter$() cuts copper into $(item)Waveguides$() (microwaves along a pipe), $(item)Bitter Plates$() and '
        '$(item)Rail Armatures$(). A $(item)Faraday Cage Mesh$() keeps fields out - carried in the off hand it shields you from Decoherence. '
        '$(item)Mu-Metal$(), nickel-iron so permeable the field runs through it instead of you, does the same for being Magnetized.',
        '$(item)Litz Wire$(), woven so every strand takes its turn at the surface, winds the coils of the Tokamak.'],
        adv='electrodynamics', sort=10, spot=['waveguide', 'faraday_cage_mesh', 'mu_metal_shield'])
    te('electromagnetism', 'r52_railgun', 'The Hypervelocity Impact Forge', 'hypervelocity_impact_forge', [
        'Two rails and an armature between them: current up one rail, across, down the other. The field between the rails pushes the armature '
        'out with $(thing)F = ½L′I²$() - a railgun.',
        'Set the current and the armature\'s mass. It leaves at $(thing)v = √(2FL/m)$() and strikes the target with a shock of about '
        '$(thing)½ρv²$(). Graphite shocked between 15 and 40 GPa becomes $(item)Lonsdaleite$(), hexagonal diamond, as in meteorite craters; '
        'sand and coal at 50-90 GPa fuse into moissanite. Each shot spends a Rail Armature. Above 3 MA the rails erode.'],
        adv='electrodynamics', sort=11, spot=['hypervelocity_impact_forge', 'lonsdaleite', 'rail_armature'])
    te('electromagnetism', 'r52_pulsed', 'The Pulsed Magnet Lab', 'pulsed_magnet_lab', [
        'Steady magnets top out near 45 tesla. For more, dump a room of capacitors into a coil for a few milliseconds: $(thing)B = μ₀NI/L$().',
        'The $(item)Pulsed Magnet Lab$() grows a storey for every stack of Bitter plates (1 to 7): more copper and cooling to soak up the '
        'pulse\'s heat (ΔT ∝ NI²t/storeys). Past 400 K the coil melts; past 100 T the magnetic pressure B²/2μ₀ tears it apart.',
        'At 40 T it magnetises samarium-cobalt in bulk. At 80 T, with magnetar crust and cosmic ray shards, it searches for '
        '$(item)Magnetic Monopoles$() - and now and then, finds one.'],
        adv='magnetic_confinement', sort=12, spot=['pulsed_magnet_lab', 'bitter_plate_stack', 'magnetic_monopole'])
    te('electromagnetism', 'r52_qhe', 'Hall Bars and Magnetars', 'hall_bar_chip', [
        'In a strong field and the cold, the Hall resistance of a sheet of electrons sits on plateaus at $(thing)h/(νe²)$() - 25 812.807 Ω '
        'for ν = 1, exact to a part in a billion, because it counts a topological number. The Laser Cutter cuts $(item)Hall Bar Chips$() from '
        'silicon and gold.',
        'A collapsing star keeps its magnetic flux: its field grows as $(thing)(R/r)²$(). A magnetar\'s is 10¹¹ tesla. Its crust, found in '
        'the Accretion Frontier\'s Pulsar Field, cracks in starquakes; Dirac showed a single magnetic charge would have to be a multiple of h/e.'],
        adv='quantum_hall_effect', sort=13, spot=['hall_bar_chip', 'magnetar_crust_fragment'])
    te('nuclear', 'r52_fuels', 'Heavy Water, Lithium and Tritium', 'lithium_6_ingot', [
        'Electrolysing water leaves the heavy molecules behind: a thousand millibuckets of $(fluid)Water$() give ten of $(fluid)Heavy Water$(), '
        'and electrolysing that gives $(fluid)Deuterium$(). $(item)Lithium$() is won from Deep Brine; the Gas Centrifuge Cascade enriches it '
        'to $(item)Lithium-6$().',
        'In the $(item)Irradiation Chamber$() - a neutron source in one slot, never used up - lithium-6 catches a neutron and splits into helium '
        'and $(fluid)Tritium$(). Uranium-235 fissions into $(item)Technetium-99m$() and a spent fuel rod; plutonium fed neutrons for long enough '
        'climbs to $(item)Californium-252$(), and californium\'s flux turns uranium-238 into plutonium-239.',
        'The $(item)Decontamination Scrubber$() melts fission products and spent salt into $(item)Nuclear Waste Glass$(), and takes the uranium '
        'back out of spent fuel rods. Radium decays, in the Decay Chamber, to $(item)Polonium-210$(); monazite sand gives $(item)Thorium-232$().'],
        adv='fusion_ignition', sort=20, spot=['lithium_6_ingot', 'technetium_99m_vial', 'californium_252_neutron_source'])
    te('nuclear', 'r52_tokamak', 'The Tokamak', 'tokamak', [
        'Deuterium and tritium fuse into helium-4 and a neutron, giving $(thing)17.6 MeV$(). They must be hot (10-20 keV, over a hundred '
        'million kelvin), dense, and held long enough: Lawson\'s $(thing)nTτ ≥ 3×10²¹ keV·s/m³$().',
        'The $(item)Tokamak$() holds the plasma in a torus of twisted field: twelve toroidal field coils, a central solenoid, a vacuum vessel. '
        'Set the density, temperature and field; confinement grows with the field squared. Keep β - plasma pressure against magnetic - under '
        '5%, or the plasma disrupts. The coils are superconducting: below 20 K, and below 12.5 T.',
        'A $(item)Fusion Blanket Tile$() of lithium-6 breeds more tritium than the reaction burns. Deuterium alone burns too - but needs five '
        'times the triple product.'],
        adv='fusion_ignition', sort=21, spot=['tokamak', 'fusion_blanket_tile', 'toroidal_field_coil'])
    te('nuclear', 'r52_isomers', 'Nuclear Isomers', 'isomer_trigger', [
        'Hafnium-178m2 holds 2.4 MeV in each nucleus, locked in by its spin, for thirty-one years: $(thing)1.3 TJ$() a kilogram. The Cyclotron '
        'makes it from rare earths (hafnium travels with them) above 6 MeV.',
        'The $(item)Isomer Trigger$() knocks it loose with a gamma pulse from a Cobalt-57 source, and all of it comes out at once - a great deal '
        'of power, and plain hafnium dust.'],
        adv='nuclear_isomers', sort=22, spot=['isomer_trigger', 'hafnium_178m2_isomer'])
    te('astrophysics', 'r52_bursts', 'Kilonovae and Gamma-Ray Bursts', 'burst_observatory', [
        'A neutron-star merger heard by the Laser Interferometer can be found in the sky: point the telescope with its record and photograph '
        'the $(thing)kilonova$() - the glow of freshly made gold and platinum, peaking after days: $(item)Kilonova Ash$().',
        'The same mergers, and the collapse of the heaviest stars, launch jets at Γ ≈ 300, beamed into a cone of 1/Γ: $(thing)gamma-ray bursts$(). '
        'The $(item)Burst Observatory$() watches the whole sky with twelve scintillators. A low threshold sees more bursts but more noise. Watch '
        'until you are 90% sure of a catch, with few false triggers: a $(item)Burst Afterglow Crystal$().'],
        adv='gamma_ray_bursts', sort=24, spot=['burst_observatory', 'burst_afterglow_crystal', 'kilonova_ash'])
