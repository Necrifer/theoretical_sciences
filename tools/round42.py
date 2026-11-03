"""
Round 42 (1.3.5): the Planetary Interior's machines - five multiblocks, each with its own silhouette (the 1.2 design
rule) and a real physics model.

* Tectonic Press (geography): a heavy box - firebox below, a multi-anvil pressure vessel, a hydraulic gantry and ram
  above it, a chimney. The pressure-temperature phase diagram decides what forms: schist, diamond, ringwoodite,
  bridgmanite, graphite.
* Deep Drilling Rig (geography): an open derrick over a drill floor. Depth sets the rock it cores and the heat and
  pressure the bit meets; the mud has to sit between the pore pressure and the fracture pressure.
* Gas Centrifuge Cascade (nuclear): a tower, one layer per stage, built 1 to 14 stages tall - the first structure in
  the mod whose size is variable (see MultiblockDefinition "repeat"). Each stage multiplies the U-235 ratio.
* Molten Salt Reactor (nuclear, generator): a shielded vessel of graphite and fuel salt. Its reactivity has to be tuned
  to k = 1; a negative temperature coefficient sets the salt's temperature.
* MHD Generator (electromagnetism, generator): a long duct through C-frame magnets. A conducting fluid crossing the
  field drives current through the electrodes: p = σu²B²k(1 − k).

Every recipe here is new (r42_ files) and frozen out of the older recipes' expansion, as 1.2's were.
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


def _B(i, n, art, c1, c2, kind='casing', light=2, hardness=6.0, tool='iron', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=light, hardness=hardness, tool=tool)
    d.update(kw)
    return d


# =================================================================================== CONTENT
ITEMS = [
    _I('yellowcake', 'Yellowcake', 'dots', '#8a7010', '#ffe040',
       'Uranium oxide concentrate, roasted out of pitchblende: a coarse yellow-brown powder, still 99.3% uranium-238.'),
    _I('fluorspar', 'Fluorspar', 'gem', '#402060', '#c080ff',
       'Calcium fluoride, which grows in veins beside calcite. The fluorine that turns uranium into a gas comes from here.'),
    _I('uranium_235_pellet', 'Uranium-235 Pellet', 'pellet', '#304028', '#a0ff70',
       'Uranium enriched to a few percent of the fissile isotope: reactor fuel. The cascade will not go past 20%.'),
    _I('uranium_238_pellet', 'Uranium-238 Pellet', 'pellet', '#2a2c30', '#7a8090',
       'Depleted uranium: what is left when the U-235 has been taken out. Dense, barely radioactive - and fertile: in a reactor it becomes plutonium.'),
    _I('plutonium_239_pellet', 'Plutonium-239 Pellet', 'pellet', '#4a4030', '#ffb060',
       'Bred from uranium-238 in the reactor and taken out of its spent salt. Warm to the touch from its own alpha decay.'),
    _I('graphite_moderator', 'Graphite Moderator', 'plate', '#202022', '#606068',
       'Nuclear-grade graphite. Carbon slows fission neutrons down without swallowing them, so they can split the next nucleus.'),
    _I('boron_control_rod', 'Boron Control Rod', 'ingot', '#303438', '#c0d0e0',
       'Boron carbide in a steel sleeve. Boron-10 drinks neutrons: push the rod in and the chain reaction slows.'),
    _I('metamorphic_schist', 'Metamorphic Schist', 'chunk', '#4a4a50', '#c0c8d0',
       'Basalt pressed and baked until its minerals regrew as flat, glittering mica - with the odd red garnet among them.'),
]
BLOCKS = [
    _B('lithostatic_casing', 'Lithostatic Casing', 'magcasing', '#2a2624', '#ff9040', hardness=10.0, tool='diamond'),
    _B('refractory_firebox', 'Refractory Firebox', 'vent', '#3a1c14', '#ff6020', light=8),
    _B('press_ram', 'Press Ram', 'coilblock', '#404448', '#e0a060', hardness=10.0, tool='diamond'),
    _B('exhaust_stack', 'Exhaust Stack', 'vent', '#3a3a3a', '#b0b0b0', light=0),
    _B('derrick_frame', 'Derrick Frame', 'riftframe', '#3a2a1a', '#e0a040', light=0),
    _B('drill_string', 'Drill String', 'beampipe', '#303030', '#c0a080', light=0),
    _B('cascade_casing', 'Cascade Casing', 'casing2', '#1c2420', '#90ff60'),
    _B('centrifuge_rotor', 'Centrifuge Rotor', 'rfcavity', '#202830', '#80ffa0', light=4),
    _B('reactor_vessel_casing', 'Reactor Vessel Casing', 'magcasing', '#1a2420', '#60ff90', hardness=10.0, tool='diamond'),
    _B('graphite_moderator_block', 'Graphite Moderator Block', 'tiles', '#1a1a1c', '#505058', light=0),
    _B('control_rod_drive', 'Control Rod Drive', 'injector', '#283038', '#c0e0ff'),
    _B('salt_heat_exchanger', 'Salt Heat Exchanger', 'cryo', '#2a2018', '#ffb060'),
    _B('mhd_channel_casing', 'MHD Channel Casing', 'casing', '#1c2030', '#60a0ff'),
    _B('mhd_field_magnet', 'MHD Field Magnet', 'dipole', '#30343c', '#ff6060', light=3),
    _B('mhd_electrode', 'MHD Electrode', 'coilblock', '#302818', '#ffd060', light=4),
]
FLUIDS = [
    dict(id='uranium_hexafluoride', name='Uranium Hexafluoride', c1='#c0c0a0', c2='#ffffe8', gas=True, light=0, temp=340, density=-20,
         viscosity=200, art='gas'),
    dict(id='molten_salt_fuel', name='Molten Salt Fuel', c1='#105020', c2='#80ff90', gas=False, light=10, temp=900, density=2000,
         viscosity=2500, art='plasma'),
    dict(id='spent_fuel_salt', name='Spent Fuel Salt', c1='#303010', c2='#d0ff40', gas=False, light=8, temp=900, density=2100,
         viscosity=2600, art='plasma'),
]

# multiblock key characters (all one-character symbols the older shapes do not use)
KEY_ADD = {
    '!': ['lithostatic_casing'], '?': ['lithostatic_casing', 'interface_port'], '#': ['refractory_firebox'], ')': ['press_ram'],
    '(': ['exhaust_stack'], '%': ['derrick_frame'], '=': ['drill_string'],
    '~': ['cascade_casing'], '^': ['cascade_casing', 'interface_port'], '@': ['centrifuge_rotor'],
    '$': ['reactor_vessel_casing'], '/': ['reactor_vessel_casing', 'interface_port'], '<': ['graphite_moderator_block'],
    '>': ['lead_shielding_block'], '{': ['control_rod_drive'], '}': ['salt_heat_exchanger'],
    '-': ['mhd_channel_casing'], '|': ['mhd_channel_casing', 'interface_port'], ':': ['mhd_field_magnet'], ';': ['mhd_electrode'],
}


# =================================================================================== SHAPES
def tectonic_press():
    """A heavy box: a firebox course at the base (its doors open at the front), the multi-anvil pressure vessel above it,
    a four-post hydraulic gantry whose ram drives down into the vessel, and a chimney at the back corner."""
    s = shapes.Shape(9, 13, 9)
    c = 4
    s.box(0, 0, 0, 8, 0, 8, '!')                          # foundation
    s.box(0, 1, 0, 8, 1, 8, '!')
    s.box(2, 1, 0, 6, 1, 6, '#')                          # the firebox, its doors on the front face
    s.box(1, 2, 1, 7, 6, 7, '!', hollow=True)             # the pressure vessel
    s.air_box(2, 3, 2, 6, 5, 6)
    s.box(2, 2, 2, 6, 2, 6, '#')                          # the hearth under the anvils
    for y in range(6, 10):
        s.put(c, y, c, ')')                               # the ram, down through the vessel's roof
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        s.column(x, z, 7, 10, '!')                        # gantry posts
    for i in range(1, 8):
        s.put(i, 10, 1, '!'); s.put(i, 10, 7, '!')        # crosshead frame
        s.put(1, 10, i, '!'); s.put(7, 10, i, '!')
        s.put(i, 10, c, '!')                              # the beam the cylinder hangs from
    s.put(c, 11, c, ')')                                  # hydraulic cylinder
    s.column(8, 8, 1, 12, '(')                            # chimney
    s.put(8, 0, 8, '!')
    s.center = (c, 4, c)
    s.ports('!', '?', 4, y=0)
    s.v[(c, 3, 1)] = 'C'
    s.ctrl = (c, 3, 1)
    return s


def deep_drilling_rig():
    """An open derrick: four lattice legs rising from a drill floor and leaning in to a crown block, braced by two girts,
    the drill string hanging down the middle into the ground, and the driller's house at the front."""
    s = shapes.Shape(9, 16, 9)
    c = 4
    for x0, z0 in ((0, 0), (6, 0), (0, 6), (6, 6)):
        s.box(x0, 0, z0, x0 + 2, 0, z0 + 2, '!')          # substructure pads
    s.box(0, 1, 0, 8, 1, 8, '!')                          # the drill floor
    for (x0, z0), (x1, z1) in (((1, 1), (3, 3)), ((7, 1), (5, 3)), ((1, 7), (3, 5)), ((7, 7), (5, 5))):
        s.line((x0, 2, z0), (x1, 14, z1), '%')            # the legs lean in toward the crown
    for y in (6, 10):                                     # girts: square rings tying the legs together
        k = round(1 + 2 * (y - 2) / 12)
        for i in range(k, 9 - k):
            s.put(i, y, k, '%'); s.put(i, y, 8 - k, '%')
            s.put(k, y, i, '%'); s.put(8 - k, y, i, '%')
    s.box(3, 15, 3, 5, 15, 5, '%')                        # crown block
    for y in range(0, 15):
        s.put(c, y, c, '=')                               # the string, from the crown sheaves down into the hole
    s.box(0, 2, 0, 2, 3, 1, '!')                          # driller's house
    s.center = (c, 6, c)
    s.ports('!', '?', 4, y=1)
    s.v[(1, 2, 0)] = 'C'
    s.ctrl = (1, 2, 0)
    return s


def gas_centrifuge_cascade():
    """A tower, one layer per stage: four tall rotors behind windows, a header ring around them. Layer 2 is the stage;
    it may be stacked up to 13 more times (14 stages, 16 blocks high)."""
    s = shapes.Shape(7, 3, 7)
    c = 3
    s.box(0, 0, 0, 6, 0, 6, '~')                          # base
    s.box(0, 1, 0, 6, 1, 6, '~')                          # feed header
    for x in range(7):                                    # the stage: header ring, windows on each face, four rotors
        for z in range(7):
            if x in (0, 6) or z in (0, 6):
                s.put(x, 2, z, 'G' if (x in (2, 3, 4) or z in (2, 3, 4)) and not (x in (0, 6) and z in (0, 6)) else '~')
    for x in range(1, 6):
        for z in range(1, 6):
            s.put(x, 2, z, '.')
    for x, z in ((2, 2), (4, 2), (2, 4), (4, 4)):
        s.put(x, 2, z, '@')
    s.put(c, 2, c, '~')                                   # the product and tails headers between the rotors
    s.center = (c, 2, c)
    s.repeat = (2, 2, 13)                                 # the stage is the top layer, so a taller tower just adds layers on top
    s.ports('~', '^', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


def molten_salt_reactor():
    """A shielded vessel: a lead biological shield round a reactor vessel with a graphite core, a lid carrying the
    control-rod drives, a salt heat exchanger on the right and a stack at the back."""
    s = shapes.Shape(11, 11, 11)
    c = 5
    s.box(0, 0, 0, 10, 0, 10, '$')                        # base slab
    for y in range(1, 6):
        for x in range(1, 10):
            for z in range(1, 10):
                if x in (1, 9) or z in (1, 9):
                    s.put(x, y, z, '>')                   # lead shield
    for y in range(1, 6):
        s.disc(c, c, y, 2.6, '$', r_in=1.7)               # the vessel
        if 2 <= y <= 4:
            s.disc(c, c, y, 1.7, '<')                     # graphite core...
            s.put(c, y, c, '.')                           # ...round a fuel-salt channel
    s.disc(c, c, 1, 1.7, '$')
    s.disc(c, c, 5, 1.7, '$')
    s.box(1, 6, 1, 9, 6, 9, '$')                          # the lid
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        s.put(c + dx, 7, c + dz, '{')                     # control-rod drives
    for y in range(1, 5):
        for z in range(3, 8):
            s.put(10, y, z, '}')                          # heat exchanger
    s.column(1, 9, 7, 10, '(')                            # stack
    s.center = (c, 3, c)
    s.ports('$', '/', 4, y=0)
    s.v[(c, 2, 1)] = 'C'
    s.ctrl = (c, 2, 1)
    return s


def mhd_generator():
    """A long line: combustor at the back, a nozzle, the channel through three C-frame field magnets with electrodes on
    its side walls, a diffuser, and the inverter at the front."""
    s = shapes.Shape(7, 8, 15)
    c = 3
    s.box(1, 0, 0, 5, 0, 14, '-')                         # skid
    for z in range(11, 15):                               # combustor
        for x in range(1, 6):
            for y in range(1, 6):
                if x in (1, 5) or y in (1, 5) or z == 14:
                    s.put(x, y, z, '-')
    for z in (9, 10):                                     # nozzle
        for x in range(2, 5):
            for y in range(2, 5):
                if x in (2, 4) or y in (2, 4):
                    s.put(x, y, z, '-')
    for z in range(3, 9):                                 # the channel
        for x in range(1, 6):
            for y in range(1, 6):
                if (x in (1, 5) or y in (1, 5)) and z % 2 == 1:
                    s.put(x, y, z, ':')                   # three C-frame magnets: field up through the duct
        for x in range(2, 5):
            s.put(x, 2, z, '-'); s.put(x, 4, z, '-')
        s.put(2, 3, z, ';'); s.put(4, 3, z, ';')          # electrodes on the side walls
    for z in (1, 2):                                      # diffuser
        for x in range(2, 5):
            for y in range(2, 5):
                if x in (2, 4) or y in (2, 4):
                    s.put(x, y, z, '-')
    s.box(2, 1, 0, 4, 3, 0, '-')                          # inverter
    for z in range(1, 15):
        if (c, 3, z) not in s.v:
            s.put(c, 3, z, '.')                           # the bore stays open
    s.column(c, 12, 6, 7, '(')                            # exhaust
    s.center = (c, 3, 6)
    s.ports('-', '|', 4, y=0)
    s.v[(c, 2, 0)] = 'C'
    s.ctrl = (c, 2, 0)
    return s


shapes.BUILDERS.update({'tectonic_press': tectonic_press, 'deep_drilling_rig': deep_drilling_rig,
                        'gas_centrifuge_cascade': gas_centrifuge_cascade, 'molten_salt_reactor': molten_salt_reactor,
                        'mhd_generator': mhd_generator})

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'tectonic_press': ('Tectonic Press', 'A multi-anvil press over a firebox: it squeezes rock to the pressures of the deep mantle and bakes it to '
                                         'match. What crystallises depends on where on the phase diagram you hold it.'),
    'deep_drilling_rig': ('Deep Drilling Rig', 'A derrick and a string of drill pipe kilometres long. The deeper the bit, the hotter the rock, the '
                                               'higher the pressure in it - and the older and stranger the core it brings up.'),
    'gas_centrifuge_cascade': ('Gas Centrifuge Cascade', 'Rotors spinning faster than sound fling the heavier isotope outward. One stage '
                                                         'enriches a little; a tower of them, a lot. Build it taller for more stages: each new layer on top is one more.'),
    'molten_salt_reactor': ('Molten Salt Reactor', 'Uranium dissolved in molten fluoride salt flows through a core of graphite and goes '
                                                   'critical there. Tune it to exactly one neutron per neutron.'),
    'mhd_generator': ('MHD Generator', 'A conducting fluid driven through a magnetic field makes current with no moving parts: the flow itself is '
                                       'the armature.'),
}
MACHINE_CAT = {'tectonic_press': 'geography', 'deep_drilling_rig': 'geography', 'gas_centrifuge_cascade': 'nuclear',
               'molten_salt_reactor': 'nuclear', 'mhd_generator': 'electromagnetism'}
MACHINE_RESEARCH = {'tectonic_press': 'metamorphism', 'deep_drilling_rig': 'deep_drilling', 'gas_centrifuge_cascade': 'isotope_separation',
                    'molten_salt_reactor': 'nuclear_fission', 'mhd_generator': 'magnetohydrodynamics'}
TIER = {k: 2 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'tectonic_press': (['RPR', 'FSF', 'LLL'], {'R': 'press_ram', 'P': 'minecraft:piston', 'F': 'refractory_firebox', 'S': _S,
                                               'L': 'lithostatic_casing'}),
    'deep_drilling_rig': (['DMD', 'ESE', 'TTT'], {'D': 'derrick_frame', 'M': 'moissanite_drill', 'E': 'electrical_steel_lamination', 'S': _S,
                                                  'T': 'drill_string'}),
    'gas_centrifuge_cascade': (['RGR', 'CSC', 'RGR'], {'R': 'centrifuge_rotor', 'G': 'geiger_tube', 'C': 'cascade_casing', 'S': _S}),
    'molten_salt_reactor': (['VGV', 'BSB', 'VLV'], {'V': 'reactor_vessel_casing', 'G': 'graphite_moderator_block', 'B': 'boron_control_rod',
                                                    'S': _S, 'L': 'lead_shielding_block'}),
    'mhd_generator': (['MEM', 'CSC', 'MEM'], {'M': 'mhd_field_magnet', 'E': 'mhd_electrode', 'C': 'mhd_channel_casing', 'S': _S}),
}
EXTRA = {
    'tectonic_press': 'Set the anvil pressure and the furnace temperature. Below the graphite-diamond line carbon stays graphite; above it, with '
                      '$(item)Iron-Nickel$() as the solvent catalyst beside the press, it becomes diamond. Peridotite turns to '
                      '$(item)Ringwoodite$() above 17.5 GPa and to $(item)Bridgmanite$() above the post-spinel line near 23 GPa. Basalt becomes '
                      '$(item)Metamorphic Schist$() at a few kilobars.',
    'deep_drilling_rig': 'Choose a depth, the weight on the bit, its speed and the mud. The mud must press harder than the water in the rock '
                         '(or the well kicks) but not so hard it splits the rock (lost circulation). Shallow: sediment cores and brine; a few '
                         'kilometres: crustal rock with rich ore, or a pocket of magma; at the bottom of thin crust, peridotite.',
    'gas_centrifuge_cascade': 'A tower of stages: build the stage layer (four rotors behind windows) once, or stack it up to fourteen high - '
                              'every stage multiplies the U-235 ratio by β. It also makes uranium hexafluoride, fuel salt, and takes spent salt '
                              'apart again (fluoride volatility). Enrichment is interlocked at 20%.',
    'molten_salt_reactor': 'Burns $(item)Molten Salt Fuel$() for power and leaves $(item)Spent Fuel Salt$(). The fuel concentration and the '
                           'control rods set k; the salt settles at the temperature where k is exactly 1 - keep it between 732 K (frozen) '
                           'and 980 K (the vessel creeps). Its rejected heat goes into the coolant loop.',
    'mhd_generator': 'Burns a conducting fluid: $(item)Deep Brine$(), $(item)Basaltic Magma$() or $(item)Stellar Plasma$(). Power per volume is '
                     'σu²B²k(1 − k): more field and more speed give more, the load factor k is best at one half - unless the current density '
                     'would arc the electrodes away.',
}

# =================================================================================== PHYSICS
_r12.model('tectonic_press', 'Tectonic Press',
           'Each mineral is stable only in part of the pressure-temperature plane, and the lines between them follow the Clapeyron slope dP/dT = '
           'ΔS/ΔV. Hold the sample on the right side of the line and keep it hot enough to react: the rate follows Arrhenius, e^(−Eₐ/RT).',
           'Gasket creep: the sample sees P·(1 ± 2%·d).',
           [_P('P', 'Anvil pressure P', 'GPa', 0, 30, 0.01, 1, 'Pressure on the sample. 1 GPa is about 30 km down.'),
            _P('Tf', 'Furnace temperature', 'K', 300, 3200, 1, 1000, 'Temperature of the sample capsule.')],
           [_D('Pe', 'Sample pressure', 'GPa', 'P*(1 + 0.02*d)', 'P_s = P·(1 + 0.02·d)', 'The pressure the sample feels, after gasket creep.'),
            _D('z', 'Equivalent depth', 'km', 'Pe*1e9/(3300*9.81)/1000', 'z = P/(ρg)', 'How deep in the Earth this pressure is found.'),
            _D('Pdg', 'Graphite-diamond line', 'GPa', '1.94 + 0.0025*(Tf - 273)', 'P = 1.94 + 0.0025·T(°C)', 'Above it diamond is stable, below it graphite.'),
            _D('Prw', 'Ringwoodite line', 'GPa', '17.5 + 0.005*(Tf - 1873)', 'P = 17.5 + 0.005·(T − 1873)', 'Above it olivine has become ringwoodite.'),
            _D('Pps', 'Post-spinel line', 'GPa', '23.5 - 0.0025*(Tf - 1900)', 'P = 23.5 − 0.0025·(T − 1900)',
               'Above it ringwoodite breaks down to bridgmanite and periclase. Its slope is negative.'),
            _D('mD', 'Margin over the diamond line', 'GPa', 'Pe - Pdg', 'P − P_dg', 'Positive: diamond is stable.'),
            _D('mR', 'Margin over the ringwoodite line', 'GPa', 'Pe - Prw', 'P − P_rw', 'Positive: ringwoodite is stable.'),
            _D('mB', 'Margin over the post-spinel line', 'GPa', 'Pe - Pps', 'P − P_ps', 'Positive: bridgmanite is stable.'),
            _D('kin', 'Reaction rate', '', 'exp(Ea/8.314*(1/Tr - 1/Tf))', 'k/k_ref = e^(Eₐ/R·(1/T_ref − 1/T))',
               'How fast the sample reacts, relative to the recipe\'s reference temperature.')],
           [_C('anvil', 'wear', '26 - P', 'Tungsten-carbide anvils deforming above 26 GPa', 'P < 26 GPa'),
            _C('overheat', 'damage', '900 - T', 'The press frame is overheating', 'T < 900 K')],
           rate='clamp(kin, 0, 3)', rate_formula='rate = e^(Eₐ/R·(1/T_ref − 1/T))', rate_desc='Hotter samples react faster.',
           heat='0.02*(Tf - 300) + 0.5*P', idle_heat='0.5',
           rvars=[_V('Ea', 'Activation energy (J/mol)', 300000), _V('Tr', 'Reference temperature (K)', 1500)], kappa=40, heat_capacity=60)

_r12.model('deep_drilling_rig', 'Deep Drilling Rig',
           'Rock warms by about 25 K for every kilometre down, and the water in its pores is under pressure. The mud in the well must press back '
           'harder than that pore pressure, or the well kicks; but not so hard that it fractures the rock. Between the two, weight and speed on '
           'the bit decide how fast it cuts.',
           'Overpressured pockets: the pore pressure varies by ±3%·d.',
           [_P('h', 'Bit depth', 'km', 0, 12, 0.01, 1, 'How deep the bit is.'),
            _P('W', 'Weight on bit', 't', 0, 60, 0.1, 10, 'How much of the drill string\'s weight rests on the bit.'),
            _P('N', 'Rotary speed', 'rpm', 0, 250, 1, 60, 'How fast the string turns.'),
            _P('rm', 'Mud density', 'g/cm³', 1, 2.5, 0.01, 1.2, 'Density of the drilling mud that fills the well.')],
           [_D('Tz', 'Formation temperature', 'K', '288 + 25*h', 'T = T₀ + (dT/dz)·h', 'Temperature of the rock at the bit.'),
            _D('Pp', 'Pore pressure', 'MPa', '9.81*h*(1 + 0.04*h)*(1 + 0.03*d)', 'P_p ≈ ρ_w g h (overpressured below)',
               'Pressure of the water in the rock\'s pores.'),
            _D('Pm', 'Mud pressure', 'MPa', '9.81*rm*h', 'P_m = ρ_m g h', 'Pressure the mud column puts on the bottom of the well.'),
            _D('Pf', 'Fracture pressure', 'MPa', '9.81*h*(1.65 + 0.03*h)', 'P_f', 'Pressure at which the rock splits open.'),
            _D('ROP', 'Rate of penetration', 'm/h', '0.5*W*N^0.7*exp(-0.08*pos(Pm - Pp))/(1 + 0.3*h)', 'ROP ∝ W·N^0.7·e^(−c·(P_m − P_p))',
               'How fast the bit cuts. Overbalanced mud holds the chips down and slows it.'),
            _D('Tb', 'Bit temperature', 'K', 'Tz + 0.02*W*N', 'T_bit = T + friction', 'The cutters\' temperature: the rock\'s heat plus friction.')],
           [_C('kick', 'damage', 'Pm - Pp', 'Kick: formation fluid is flowing into the well', 'P_m ≥ P_p'),
            _C('losses', 'stop', 'Pf - Pm', 'Lost circulation: the mud is fracturing the rock', 'P_m < P_f'),
            _C('bit', 'damage', '700 - Tb', 'The diamond cutters are burning (above 700 K)', 'T_bit < 700 K'),
            _C('buckle', 'wear', '45 - W', 'The drill string is buckling under the weight', 'W < 45 t')],
           rate='clamp(ROP/30, 0, 3)', rate_formula='rate = ROP / 30 m/h', rate_desc='The faster it cuts, the faster cores come up.',
           heat='0.002*W*N + 0.5*rm', idle_heat='0.5', kappa=40, heat_capacity=60)

_r12.model('gas_centrifuge_cascade', 'Gas Centrifuge Cascade',
           'In a spinning rotor the heavier molecule crowds to the wall: the separation factor is α = e^(Δm v²/2RT). A stage passes on a fraction '
           'θ (the cut) of its feed, enriched by β = 1 + (α − 1)(1 − θ); n stages multiply the isotope ratio by βⁿ. Every layer of the tower is a '
           'stage.',
           'Rotor imbalance: the speed wanders by ±1%·d.',
           [_P('v', 'Rotor peripheral speed', 'm/s', 100, 1000, 1, 400, 'How fast the rotor wall moves. The separation grows with v², and so '
                                                                         'does the stress in the rotor.'),
            _P('th', 'Cut θ', '', 0.05, 0.95, 0.01, 0.5, 'The fraction of each stage\'s feed passed up as product. A small cut enriches more, '
                                                          'a large one is faster.')],
           [_D('ve', 'Rotor speed', 'm/s', 'v*(1 + 0.01*d)', 'v·(1 + 0.01·d)', 'The speed the rotors actually run at.'),
            _D('Tg', 'Gas temperature', 'K', 'T + 20', 'T_gas = T + 20 K', 'The gas runs a little warmer than the coolant.'),
            _D('al', 'Separation factor α', '', 'exp(dm*ve^2/(2*8.314*Tg))', 'α = e^(Δm v²/2RT)', 'How much richer in the light isotope the axis is '
                                                                                                  'than the wall.'),
            _D('be', 'Stage enrichment β', '', '1 + (al - 1)*(1 - th)', 'β = 1 + (α − 1)(1 − θ)', 'What one stage multiplies the isotope ratio by.'),
            _D('n', 'Stages', '', 'size', 'n', 'Stage layers built in the tower.'),
            _D('Rf', 'Feed ratio', '', 'xF/(1 - xF)', 'R_F = x_F/(1 − x_F)', 'U-235 to U-238 in the feed.', show=False),
            _D('xP', 'Product assay', '', 'Rf*be^n/(1 + Rf*be^n)', 'x_P = R_F βⁿ/(1 + R_F βⁿ)', 'Fraction of U-235 in the product.'),
            _D('sig', 'Rotor hoop stress', 'MPa', '0.0016*ve^2', 'σ = ρv²', 'Stress in the carbon-fibre rotor wall.')],
           [_C('burst', 'damage', '1500 - sig', 'Rotor stress above what carbon fibre safely holds', 'σ < 1500 MPa'),
            _C('resonance', 'wear', 'abs(ve - 450) - 25', 'The rotors are sitting on a flexural resonance', '|v − 450| > 25 m/s'),
            _C('safeguard', 'stop', '0.2 - xP', 'Safeguards interlock: product above 20% U-235', 'x_P < 20%'),
            _C('freeze', 'stop', 'Tg - 250', 'Uranium hexafluoride freezing onto the rotor walls', 'T_gas > 250 K')],
           rate='clamp(2*th*(0.7 + 0.05*n), 0, 3)', rate_formula='rate = 2θ·(0.7 + 0.05n)', rate_desc='A bigger cut, and a taller tower, pass more product.',
           heat='0.001*v*n + 0.5', idle_heat='0.3', rvars=[_V('dm', 'Mass difference Δm (kg/mol)', 0.003), _V('xF', 'Feed U-235 fraction', 0.0072)],
           kappa=40, heat_capacity=60)

_r12.model('molten_salt_reactor', 'Molten Salt Reactor',
           'Each fission frees neutrons; k = η f p ε of them cause the next fission, less what leaks out and what the control rods absorb. The salt '
           'expands as it warms and k falls by 4 pcm per kelvin, so the reactor settles at the temperature where k is exactly 1. Pumping the salt '
           'through the heat exchanger carries that heat to the turbine.',
           'Xenon and burn-up: reactivity drifts by ±0.2%·d.',
           [_P('c', 'Fuel concentration', 'mol%', 0, 5, 0.01, 1, 'How much uranium is dissolved in the salt.'),
            _P('r', 'Control rod insertion', '', 0, 1, 0.001, 0.5, 'How far the boron rods are pushed into the core.'),
            _P('Q', 'Salt flow', 'kg/s', 0, 5, 0.01, 2, 'How fast the salt is pumped through the core and the heat exchanger.')],
           [_D('f', 'Thermal utilisation f', '', 'c/(c + 0.6)', 'f = Σ_fuel/(Σ_fuel + Σ_other)', 'Fraction of slowed neutrons the fuel absorbs.'),
            _D('kinf', 'k∞', '', '2.07*f*0.92*1.02*(1 + 0.002*d)', 'k∞ = η f p ε', 'Neutrons per neutron in an endless core: η = 2.07, p = 0.92, '
                                                                                   'ε = 1.02.'),
            _D('k0', 'k at 900 K', '', 'kinf*0.8*(1 - 0.5*r)', 'k = k∞·P_NL·(1 − w·r)', 'With leakage (20%) and the rods (worth up to half).'),
            _D('rho', 'Reactivity', 'pcm', '(k0 - 1)/k0*1e5', 'ρ = (k − 1)/k', 'How far from critical, in per cent mille.'),
            _D('Ts', 'Salt temperature', 'K', '900 + (k0 - 1)/4e-5', 'T = 900 K + ρ/α_T', 'Where the temperature feedback brings k to exactly 1.'),
            _D('Pth', 'Thermal power', 'kW', '2.4*Q*pos(Ts - 820)', 'P = Q·c_p·(T − T_return)', 'Heat carried out of the core by the salt.'),
            _D('eta', 'Cycle efficiency', '', '0.65*pos(1 - T/Ts)', 'η = 0.65·(1 − T_cold/T)', 'How much of it the turbine turns into work.'),
            _D('Pe', 'Electric power', 'kW', 'eta*Pth', 'P_e = η·P', 'Power delivered.')],
           [_C('freeze', 'stop', 'Ts - 732', 'The fuel salt is freezing (below 732 K): the freeze plug has drained the core', 'T > 732 K'),
            _C('vessel', 'damage', '980 - Ts', 'Above 980 K the Hastelloy-N vessel creeps', 'T < 980 K'),
            _C('prompt', 'damage', '0.0065 - (k0 - 1)', 'Prompt critical: reactivity above the delayed-neutron fraction', 'k − 1 < 0.0065'),
            _C('flow', 'stop', 'Q - 0.05', 'No salt flow: the heat has nowhere to go', 'Q > 0')],
           rate='clamp(Pe/Pref, 0, 2)', rate_formula='output = P_e / P_rated', rate_desc='Power output follows the electric power.',
           heat='pos(Pth - Pe)/10', idle_heat='0.5', rvars=[_V('Pref', 'Rated power (kW)', 500)], kappa=60, heat_capacity=200)

_r12.model('mhd_generator', 'MHD Generator',
           'A fluid of conductivity σ crossing a field B at speed u sees an electric field uB. The load takes a fraction k of it; the current density '
           'j = σuB(1 − k) flows to the electrodes, and the power per volume is p = σu²B²k(1 − k).',
           'Magnet supply ripple: the field varies by ±1%·d.',
           [_P('B', 'Magnetic field B', 'T', 0, 2, 0.01, 1, 'Field across the channel. Iron-cored magnets saturate near 2 T.'),
            _P('u', 'Flow speed u', 'm/s', 0, 8000, 0.1, 10, 'How fast the fluid is driven through the channel.'),
            _P('k', 'Load factor k', '', 0, 1, 0.001, 0.5, 'The fraction of the open-circuit voltage the load takes.')],
           [_D('Be', 'Effective field', 'T', 'B*(1 + 0.01*d)', 'B·(1 + 0.01·d)', 'The field the fluid sees.'),
            _D('j', 'Current density', 'A/m²', 'sg*u*Be*(1 - k)', 'j = σuB(1 − k)', 'Current through the electrodes.'),
            _D('p', 'Power density', 'W/m³', 'sg*u^2*Be^2*k*(1 - k)', 'p = σu²B²k(1 − k)', 'Electrical power per cubic metre of channel.'),
            _D('Pe', 'Electric power', 'kW', 'p*4/1000', 'P = p·V', 'Power from the 4 m³ channel.'),
            _D('q', 'Dynamic pressure', 'kPa', '0.5*rh*u^2/1000', 'q = ½ρu²', 'What the pumps must drive the fluid against.')],
           [_C('arc', 'wear', '1e6 - j', 'Electrode arcing: current density above 100 A/cm²', 'j < 10⁶ A/m²'),
            _C('pumps', 'stop', '200 - q', 'The pumps cannot drive the flow that fast', 'q < 200 kPa'),
            _C('overheat', 'damage', '1300 - T', 'The channel is overheating', 'T < 1300 K')],
           rate='clamp(Pe/Pref, 0, 2)', rate_formula='output = P / P_rated', rate_desc='Power output follows the electric power.',
           heat='0.5 + min(200, 4*sg*u^2*Be^2*(1 - k)^2/1000)', idle_heat='0.3',
           rvars=[_V('sg', 'Conductivity σ (S/m)', 20), _V('rh', 'Density ρ (kg/m³)', 1200), _V('Pref', 'Rated power (kW)', 18)],
           kappa=40, heat_capacity=60)

_TP = {
    'schist': ({'Ea': 150000, 'Tr': 950}, [('Pe', 0.3, 1.2), ('Tf', 700, 1000)]),
    'diamond': ({'Ea': 400000, 'Tr': 2100}, [('mD', 0.5, 100), ('Tf', 1600, 2400)]),
    'ringwoodite': ({'Ea': 300000, 'Tr': 1900}, [('mR', 0, 100), ('mB', -100, -0.5), ('Tf', 1600, 2200)]),
    'bridgmanite': ({'Ea': 300000, 'Tr': 2300}, [('mB', 0.3, 100), ('Tf', 1800, 2600)]),
    'graphite': ({'Ea': 500000, 'Tr': 3000}, [('mD', -100, -1), ('Tf', 2600, 3200)]),
    'boron_control_rod': ({'Ea': 300000, 'Tr': 2600}, [('Pe', 0, 1), ('Tf', 2200, 3200)]),
}
_r12.WINDOWS['tectonic_press'] = {'r42_' + k: v for k, v in _TP.items()}
_DR = {'sediment': [('h', 0.5, 3)], 'geothermal': [('h', 3, 6), ('Tz', 360, 1000)], 'crustal': [('h', 4, 8)], 'moho': [('h', 9, 12)]}
_r12.WINDOWS['deep_drilling_rig'] = {'r42_drill_' + k: ({}, v) for k, v in _DR.items()}
_r12.WINDOWS['gas_centrifuge_cascade'] = {'r42_enrich_uranium': ({}, [('xP', 0.03, 0.2)])}
_r12.WINDOWS['molten_salt_reactor'] = {'r42_msr_fuel': ({'Pref': 500}, [])}
_r12.WINDOWS['mhd_generator'] = {
    'r42_mhd_brine': ({'sg': 20, 'rh': 1200, 'Pref': 18}, []),
    'r42_mhd_magma': ({'sg': 10, 'rh': 2700, 'Pref': 4}, []),
    'r42_mhd_plasma': ({'sg': 10000, 'rh': 0.01, 'Pref': 2e7}, []),
}

# =================================================================================== RESEARCH
RESEARCH = [
    ('metamorphism', 'Metamorphic Petrology',
     'Bury a rock and it changes without melting: its minerals regrow as the ones stable at the new pressure and temperature. Every boundary on '
     'that map has a slope set by how much the change gains in entropy and loses in volume. Unlocks the Tectonic Press.',
     'tectonic_press', 6, 16, 70, ['plate_tectonics'], [('peridotite', 8), ('basalt_column', 8)]),
    ('deep_drilling', 'Deep Drilling',
     'The deepest hole ever drilled went down 12 km and met rock at 180 °C, softer than expected and full of water. Drilling deep is a fight '
     'with heat and pressure. Unlocks the Deep Drilling Rig.',
     'deep_drilling_rig', 7, 16, 90, ['mantle_geodynamics', 'metamorphism'], [('moissanite', 2), ('sediment_core', 8)]),
    ('isotope_separation', 'Isotope Separation',
     'Isotopes of one element are chemically the same; only their mass differs, by about one per cent for uranium. Spin them fast enough and '
     'that is enough to sort them, a little at a time. Unlocks the Gas Centrifuge Cascade, Yellowcake and Fluorspar.',
     'gas_centrifuge_cascade', 6, 13, 60, ['radiochemistry', 'decay_chains'], [('raw_pitchblende', 8), ('radium_salt', 1)]),
    ('nuclear_fission', 'Nuclear Fission',
     'A slow neutron splits a uranium-235 nucleus into two lighter ones, freeing 200 MeV and two or three new neutrons. If on average exactly '
     'one of them splits another nucleus, the reaction keeps itself going. Unlocks the Molten Salt Reactor.',
     'molten_salt_reactor', 7, 13, 90, ['isotope_separation', 'nuclear_binding_energy'], [('uranium_235_pellet', 2), ('graphite_moderator', 4)]),
    ('magnetohydrodynamics', 'Magnetohydrodynamics',
     'A conducting fluid in a magnetic field carries the field with it and pushes current through it: the physics of the Sun\'s surface, of the '
     'geodynamo, and of a generator with no moving parts. Unlocks the MHD Generator.',
     'mhd_generator', 5, -5, 70, ['electromagnetic_induction', 'plasma_metallurgy'], [('neodymium_magnet', 4), ('electrical_steel_lamination', 4)]),
]

PUZZLES = {
    'metamorphism': dict(
        goal='Two minerals share a boundary on the pressure-temperature map. Turning the first into the second changes its entropy from S _ 1 to S _ 2 '
             'and its molar volume from V _ 1 to V _ 2. Write s, how many pascals the boundary climbs for each kelvin.',
        name='Clapeyron slope', lhs='s',
        form='( S _ 2 - S _ 1 ) / ( V _ 2 - V _ 1 )',
        expr='( B - A ) / ( Y - X )',
        ask='Find s in Pa/K for graphite into diamond: A = 5.74, B = 2.38 J/(mol·K); X = 5.30e-6, Y = 3.42e-6 m³/mol.',
        vars={'s': 'boundary steepness (Pa/K)', 'S': 'molar entropy', 'V': 'molar volume', 'A': 'entropy of graphite', 'B': 'entropy of diamond',
              'X': 'volume of graphite (m³/mol)', 'Y': 'volume of diamond (m³/mol)'},
        given={'A': 5.74, 'B': 2.38, 'X': 5.30e-6, 'Y': 3.42e-6}, unit='Pa/K',
        method='Along the boundary both phases have the same Gibbs energy, so their changes match: V₁dP − S₁dT = V₂dP − S₂dT.',
        distractors=['T', '+', 'ln']),
    'deep_drilling': dict(
        goal='Heat flows up out of the planet at q watts per square metre through rock that conducts k watts per metre per kelvin. The surface is at '
             'T _ 0. Write the temperature T a depth h down.',
        name='Geothermal gradient', lhs='T',
        form='T _ 0 + q * h / k',
        expr='A + q * h / k',
        ask='Find T in kelvin at the bottom of the deepest hole, h = 12000 m, with A = 288 K, q = 0.065 W/m² and k = 2.5 W/(m·K).',
        vars={'T': 'temperature (K)', 'q': 'heat flow (W/m²)', 'h': 'depth (m)', 'k': 'conductivity (W/m·K)', 'A': 'surface temperature (K)'},
        given={'A': 288, 'q': 0.065, 'h': 12000, 'k': 2.5}, unit='K',
        method='Fourier\'s law: the heat flow is the conductivity times how fast the temperature rises with depth.',
        distractors=['ln', '2', 'ρ']),
    'isotope_separation': dict(
        goal='In a spinning rotor a heavy and a light molecule of the same gas settle differently. Their molar masses differ by M, the wall moves at '
             'speed v and the gas is at temperature T. Write α, how much richer in the light one the gas at the axis is than at the wall.',
        name='Centrifuge separation factor', lhs='α',
        form='exp ( M * v ^ 2 / ( 2 * R * T ) )',
        expr='exp ( M * v ^ 2 / ( 2 * R * T ) )',
        ask='Find α for uranium hexafluoride: M = 0.003 kg/mol, v = 700 m/s, R = 8.314 J/(mol·K), T = 310 K.',
        vars={'α': 'enrichment between axis and wall', 'M': 'molar mass difference (kg/mol)', 'v': 'wall speed (m/s)', 'R': 'gas constant',
              'T': 'temperature (K)'},
        given={'M': 0.003, 'v': 700, 'R': 8.314, 'T': 310}, unit='',
        method='Each species follows the Boltzmann law in the rotating frame, n ∝ e^(mω²r²/2RT); divide the two at the wall and at the axis.',
        distractors=['ln', 'π', '4']),
    'nuclear_fission': dict(
        goal='A neutron from one splitting nucleus makes η new neutrons per neutron the fuel absorbs; a fraction f of slowed neutrons are absorbed by '
             'fuel, a fraction p survive slowing down, and fast splits add a factor ε. Write k, the neutrons in the next generation per neutron in this one.',
        name='Four-factor formula', lhs='k',
        form='η * f * p * ε',
        expr='n * f * p * e',
        ask='Find k for a graphite core: n = 2.07, f = 0.71, p = 0.75, e = 1.03.',
        vars={'k': 'multiplication', 'η': 'neutrons per absorption', 'f': 'thermal utilisation', 'p': 'resonance escape', 'ε': 'fast fission',
              'n': 'neutrons per absorption', 'e': 'fast fission'},
        given={'n': 2.07, 'f': 0.71, 'p': 0.75, 'e': 1.03}, unit='',
        method='Follow one generation round: each step keeps or multiplies the neutron count, so the factors multiply.',
        distractors=['+', 'λ', '2']),
    'magnetohydrodynamics': dict(
        goal='A fluid of conductivity σ flows at speed u across a field B between two electrodes; the outside circuit takes a fraction k of the '
             'open-circuit voltage. Write p, the electrical output per cubic metre.',
        name='Faraday generator', lhs='p',
        form='σ * u ^ 2 * B ^ 2 * k * ( 1 - k )',
        expr='σ * u ^ 2 * B ^ 2 * k * ( 1 - k )',
        ask='Find p in W/m³ for a seeded plasma: σ = 50 S/m, u = 1000 m/s, B = 5 T, k = 0.5.',
        vars={'p': 'output per volume (W/m³)', 'σ': 'conductivity (S/m)', 'u': 'flow speed (m/s)', 'B': 'magnetic field (T)', 'k': 'load factor'},
        given={'σ': 50, 'u': 1000, 'B': 5, 'k': 0.5}, unit='W/m³',
        method='The current density is j = σ(uB − E) with E = k·uB at the load; the power delivered is j·E.',
        distractors=['2', '+', 'ρ']),
}

# =================================================================================== RECIPES
TP, DR, GC, MS, MH = 'tectonic_pressing', 'deep_drilling', 'isotope_enrichment', 'molten_salt_fission', 'magnetohydrodynamic'
RECIPES = [
    # (name, type, item inputs, fluid inputs, item outputs, fluid outputs, energy, ticks, research, extra)
    ('schist', TP, [('basalt_column', 4)], [('minecraft:water', 250)], [('metamorphic_schist', 4), ('garnet', 1)], [], 2048, 400,
     'metamorphism', {'chances': [1.0, 0.25]}),
    ('diamond', TP, [('minecraft:coal_block', 2)], [], [('minecraft:diamond', 1)], [], 8192, 1200, 'metamorphism', {'catalysts': ['core_iron']}),
    ('ringwoodite', TP, [('peridotite', 4)], [('minecraft:water', 100)], [('ringwoodite', 1)], [], 6144, 800, 'metamorphism', {}),
    ('bridgmanite', TP, [('peridotite', 4)], [], [('bridgmanite', 1)], [], 8192, 1000, 'metamorphism', {}),
    ('graphite', TP, [('minecraft:coal_block', 1)], [], [('graphite_moderator', 4)], [], 4096, 600, 'metamorphism', {}),
    ('boron_control_rod', TP, [('graphite_moderator', 1), ('minecraft:iron_ingot', 2)], [('deep_brine', 1000)], [('boron_control_rod', 2)], [],
     4096, 600, 'metamorphism', {}),
    ('drill_sediment', DR, [], [('minecraft:water', 500)], [('sediment_core', 4)], [('deep_brine', 500)], 1024, 400, 'deep_drilling', {}),
    ('drill_geothermal', DR, [], [('minecraft:water', 500)], [], [('basaltic_magma', 1000)], 2048, 600, 'deep_drilling', {}),
    ('drill_crustal', DR, [], [('minecraft:water', 500)], [('crustal_rock', 8), ('rich_iron_chunk', 1), ('rich_copper_chunk', 1)], [], 2048, 600,
     'deep_drilling', {'chances': [1.0, 0.3, 0.2]}),
    ('drill_moho', DR, [], [('minecraft:water', 500)], [('peridotite', 4), ('garnet', 1)], [], 4096, 900, 'deep_drilling', {'chances': [1.0, 0.25]}),
    ('uf6_conversion', GC, [('yellowcake', 2), ('fluorspar', 3)], [], [], [('uranium_hexafluoride', 1000)], 1024, 300, 'isotope_separation', {}),
    ('enrich_uranium', GC, [], [('uranium_hexafluoride', 1000)], [('uranium_235_pellet', 1), ('uranium_238_pellet', 5)], [], 2048, 600,
     'isotope_separation', {}),
    ('fuel_salt', GC, [('uranium_235_pellet', 1), ('uranium_238_pellet', 2), ('fluorspar', 2)], [('deep_brine', 1000)], [],
     [('molten_salt_fuel', 1000)], 1024, 300, 'nuclear_fission', {}),
    ('mox_salt', GC, [('plutonium_239_pellet', 1), ('uranium_238_pellet', 2), ('fluorspar', 2)], [('deep_brine', 1000)], [],
     [('molten_salt_fuel', 1000)], 1024, 300, 'nuclear_fission', {}),
    ('fluoride_volatility', GC, [], [('spent_fuel_salt', 1000)], [('fission_fragment_mix', 2), ('plutonium_239_pellet', 1)],
     [('uranium_hexafluoride', 500)], 2048, 600, 'nuclear_fission', {'chances': [1.0, 0.5]}),
]
# generator fuels: (name, type, item in, fluid in, item out, fluid out, FE/t, ticks, research)
GENERATOR = [
    ('msr_fuel', MS, [], [('molten_salt_fuel', 1000)], [], [('spent_fuel_salt', 900)], 4096, 4800, 'nuclear_fission'),
    ('mhd_brine', MH, [], [('deep_brine', 1000)], [], [], 128, 1200, 'magnetohydrodynamics'),
    ('mhd_magma', MH, [], [('basaltic_magma', 1000)], [('minecraft:basalt', 4)], [], 512, 1200, 'magnetohydrodynamics'),
    ('mhd_plasma', MH, [], [('stellar_plasma', 1000)], [], [], 65536, 600, 'magnetohydrodynamics'),
]
CRAFTING = [
    ('lithostatic_casing', ['IBI', 'BOB', 'IBI'], {'I': 'minecraft:iron_ingot', 'B': 'basalt_column', 'O': 'minecraft:obsidian'},
     'lithostatic_casing', 4),
    ('refractory_firebox', ['KCK', 'CFC', 'KCK'], {'K': 'minecraft:bricks', 'C': 'corundum', 'F': 'minecraft:blast_furnace'}, 'refractory_firebox', 2),
    ('press_ram', ['IMI', 'IMI', 'IMI'], {'I': 'minecraft:iron_block', 'M': 'moissanite'}, 'press_ram', 2),
    ('exhaust_stack', ['B B', 'B B', 'B B'], {'B': 'minecraft:bricks'}, 'exhaust_stack', 4),
    ('derrick_frame', ['IBI', 'B B', 'IBI'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:iron_bars'}, 'derrick_frame', 8),
    ('drill_string', ['I I', 'I I', 'I I'], {'I': 'minecraft:iron_ingot'}, 'drill_string', 3),
    ('cascade_casing', ['ILI', 'LGL', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'lead_shielding_plate', 'G': 'minecraft:glass'}, 'cascade_casing', 4),
    ('centrifuge_rotor', [' N ', 'EIE', ' N '], {'N': 'neodymium_magnet', 'E': 'electrical_steel_lamination', 'I': 'minecraft:iron_block'},
     'centrifuge_rotor', 2),
    ('reactor_vessel_casing', ['ILI', 'LKL', 'ILI'], {'I': 'minecraft:iron_ingot', 'L': 'lead_shielding_plate', 'K': 'core_iron'},
     'reactor_vessel_casing', 4),
    ('graphite_moderator_block', ['GGG', 'GGG', 'GGG'], {'G': 'graphite_moderator'}, 'graphite_moderator_block', 1),
    ('control_rod_drive', ['IRI', 'PBP', 'IRI'], {'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone', 'P': 'minecraft:piston',
                                                  'B': 'boron_control_rod'}, 'control_rod_drive', 2),
    ('salt_heat_exchanger', ['CIC', 'IBI', 'CIC'], {'C': 'minecraft:copper_ingot', 'I': 'minecraft:iron_ingot', 'B': 'minecraft:bucket'},
     'salt_heat_exchanger', 2),
    ('mhd_channel_casing', ['ICI', 'C C', 'ICI'], {'I': 'minecraft:iron_ingot', 'C': 'corundum'}, 'mhd_channel_casing', 4),
    ('mhd_field_magnet', ['ENE', 'NIN', 'ENE'], {'E': 'electrical_steel_lamination', 'N': 'neodymium_magnet', 'I': 'minecraft:iron_block'},
     'mhd_field_magnet', 2),
    ('mhd_electrode', ['CCC', 'CMC', 'CCC'], {'C': 'minecraft:copper_ingot', 'M': 'moissanite'}, 'mhd_electrode', 4),
]
SHAPELESS = [
    ('graphite_from_block', ['graphite_moderator_block'], 'graphite_moderator', 9),
]
SPECTRO = [
    ('fluorspar_from_calcite', [('minecraft:calcite', 4), ('spectral_plate', 1)], [('fluorspar', 2)]),
    ('fluorspar_from_shell', [('geode_shell', 2), ('spectral_plate', 1)], [('fluorspar', 2)]),
]

# =================================================================================== RADIATION
ITEM_SOURCES = {
    f'{NS}:yellowcake': ('u238_chain', 0.03),
    f'{NS}:uranium_235_pellet': ('u235', 0.02),
    f'{NS}:uranium_238_pellet': ('u238', 0.005),
    f'{NS}:plutonium_239_pellet': ('pu239', 0.05),
    f'{NS}:boron_control_rod': ('co60', 0.0),
}
ITEM_SOURCES.pop(f'{NS}:boron_control_rod')
SHIELDING = {f'{NS}:graphite_moderator_block': (0.6, 1.2), f'{NS}:reactor_vessel_casing': (2.2, 0.3)}
RADIOACTIVE_EXTRA = [f'{NS}:yellowcake', f'{NS}:uranium_235_pellet', f'{NS}:uranium_238_pellet', f'{NS}:plutonium_239_pellet']

LANG = {
    'message.theoreticalsciences.structure_units': 'Stages built: %s (the tower takes up to %s).',
    'jei.theoreticalsciences.variable_height': 'Variable height: the shown stage layer can be stacked up to %s high.',
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
    p = os.path.join(HERE, 'puzzles2', 'group_r42.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _unscaled(g, typ, name):
    p = os.path.join(g['REC'], typ, name + '.json')
    r = json.load(open(p))
    r['unscaled'] = True
    g['w'](p, r)


def _recipes(g):
    for name, typ, ins, fins, outs, fouts, fe, t, res, extra in RECIPES:
        g['mr']('r42_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res, **extra)
        _unscaled(g, typ, 'r42_' + name)
    for name, typ, ins, fins, outs, fouts, fe, t, res in GENERATOR:
        g['mr']('r42_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res)
        _unscaled(g, typ, 'r42_' + name)
    for name, ins, outs in SPECTRO:
        g['mr']('r42_' + name, 'spectroscopy', ins, [], outs, [], energy=20000, duration=200, research='isotope_separation')
        _unscaled(g, 'spectroscopy', 'r42_' + name)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r42_' + name, pat, key, res, n)
    for name, items, res, n in SHAPELESS:
        g['shapeless']('r42_' + name, items, res, n)
    REC, w, q = g['REC'], g['w'], g['q']
    for kind, t in (('smelting', 200), ('blasting', 100)):
        w(os.path.join(REC, 'crafting', f'r42_yellowcake_{kind}.json'), {
            'type': f'minecraft:{kind}', 'category': 'misc', 'ingredient': {'item': q('raw_pitchblende')}, 'result': {'id': q('yellowcake'), 'count': 1},
            'experience': 0.5, 'cookingtime': t})


def _merge_map(g, kind, name, values):
    p = os.path.join(g['D'], 'data_maps', kind, name + '.json')
    t = json.load(open(p)) if os.path.exists(p) else {'values': {}}
    t['values'].update(values)
    g['w'](p, t)


def _extend_tag(g, p, ids):
    t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    for i in ids:
        if i not in t['values']:
            t['values'].append(i)
    g['w'](p, t)


def _data(g):
    _merge_map(g, 'item', 'radiation_source', {k: {'isotope': i, 'strength': s} for k, (i, s) in ITEM_SOURCES.items()})
    _merge_map(g, 'block', 'radiation_shielding', {k: {'gamma': a, 'neutron': b} for k, (a, b) in SHIELDING.items()})
    _extend_tag(g, os.path.join(g['D'], 'tags', 'item', 'radioactive.json'), RADIOACTIVE_EXTRA)
    RES = g['RES']
    _extend_tag(g, os.path.join(RES, 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'), [f'{NS}:{b["id"]}' for b in BLOCKS])
    _extend_tag(g, os.path.join(RES, 'data', 'c', 'tags', 'item', 'gems', 'fluorite.json'), [f'{NS}:fluorspar'])
    _extend_tag(g, os.path.join(RES, 'data', 'c', 'tags', 'item', 'ingots', 'graphite.json'), [f'{NS}:graphite_moderator'])


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)


def apply_late(g):
    _data(g)


def finish(g):
    te = g['text_entry']
    te('geography', 'r42_press', 'The Tectonic Press', 'tectonic_press', [
        'Rock buried deep recrystallises into whatever is stable down there. The $(item)Tectonic Press$() does it on purpose: a multi-anvil '
        'vessel over a firebox, under a hydraulic ram. You set two things - the anvil pressure and the furnace temperature - and the phase '
        'diagram does the rest.',
        'Each boundary between two minerals is a line on that diagram, and its slope is the $(thing)Clapeyron slope$(), dP/dT = ΔS/ΔV. '
        'Graphite turns to diamond above P = 1.94 + 0.0025·T(°C) GPa; olivine to ringwoodite above about 17.5 GPa; ringwoodite to bridgmanite '
        'above the post-spinel line near 23 GPa, which leans the other way.',
        'Being on the right side is not enough: the sample must be hot enough to react. Its rate follows Arrhenius - a few hundred kelvin '
        'make the difference between hours and seconds. Diamond also wants a solvent: keep a block of $(item)Iron-Nickel$() in the press, as '
        'real diamond presses use molten iron and nickel.',
        'At low pressure and 3,000 K the press turns coal into nuclear-grade $(item)Graphite$(), and with brine and iron into '
        '$(item)Boron Control Rods$(): boron carbide is made the same way.'],
        adv='metamorphism', sort=8, spot=['tectonic_press', 'metamorphic_schist', 'graphite_moderator'])
    te('geography', 'r42_drilling', 'Deep Drilling', 'deep_drilling_rig', [
        'The $(item)Deep Drilling Rig$() is a derrick: four leaning legs, a crown block, and a string of pipe hanging into the hole. Set the '
        'depth, the weight on the bit and its speed, and the density of the mud that fills the well.',
        'Below you the rock warms by about 25 K a kilometre, and the water in its pores is under pressure - overpressured, deeper down. The mud '
        'column must press harder than that water, or it flows into the well: a $(thing)kick$(). But too heavy a mud fractures the rock and '
        'vanishes into it: $(thing)lost circulation$(). Between the two is the window you drill in.',
        'What comes up depends on how deep you are: sediment cores and brine near the top, a pocket of $(fluid)Basaltic Magma$() where the rock is '
        'hot enough, crustal rock with rich ore, and at the bottom of thin crust, $(item)Peridotite$() from the mantle.'],
        adv='deep_drilling', sort=9, spot=['deep_drilling_rig', 'drill_string', 'derrick_frame'])
    te('nuclear', 'r42_enrichment', 'Enrichment', 'gas_centrifuge_cascade', [
        'Natural uranium is 0.72% uranium-235. A reactor needs a few per cent. Roast $(item)Raw Pitchblende$() into $(item)Yellowcake$(), '
        'combine it with $(item)Fluorspar$() (the Spectrograph finds it in calcite and in geode shells) and you have $(fluid)Uranium '
        'Hexafluoride$(), a gas.',
        'In a centrifuge spinning at hundreds of metres a second, the heavier U-238 crowds to the wall: the separation factor is '
        'α = e^(Δm v²/2RT). One stage barely helps. The $(item)Gas Centrifuge Cascade$() is a tower, one layer per stage: build the stage layer '
        'once, or stack it up to fourteen high, and every stage multiplies the ratio again.',
        'The $(thing)cut$() is the share of each stage\'s feed passed upward: small cuts enrich more, large ones run faster. Spin the rotors '
        'faster for more separation, but carbon fibre bursts above about 1,500 MPa, and there is a flexural resonance near 450 m/s. Enrichment '
        'stops at 20%: reactor fuel, never more.'],
        adv='isotope_separation', sort=5, spot=['gas_centrifuge_cascade', 'yellowcake', 'fluorspar', 'uranium_235_pellet'])
    te('nuclear', 'r42_msr', 'The Molten Salt Reactor', 'molten_salt_reactor', [
        'In a $(item)Molten Salt Reactor$() the fuel is liquid: uranium fluoride dissolved in a lithium salt, pumped through a core of graphite. '
        'Graphite slows the neutrons; the salt carries the heat out.',
        'A chain reaction holds steady when each fission leads to exactly one more: k = η f p ε, less what leaks out and what the boron '
        '$(thing)control rods$() absorb. Too little uranium and it will not go critical at all; too much and the rods must hold it back.',
        'Salt expands as it warms, so k falls with temperature: the reactor finds its own temperature, where k is exactly one. Keep that between '
        '732 K, where the salt freezes, and 980 K, where the vessel creeps. More fuel or fewer rods: hotter. More flow: more power at the same '
        'temperature.',
        'The spent salt still holds most of its uranium, plutonium bred from uranium-238, and fission products. The cascade takes it apart by '
        'fluoride volatility: back to hexafluoride, plutonium and $(item)Fission Fragment Mix$(). Plutonium makes mixed-oxide fuel salt.'],
        adv='nuclear_fission', sort=6, spot=['molten_salt_reactor', 'boron_control_rod', 'plutonium_239_pellet'])
    te('electromagnetism', 'r42_mhd', 'The MHD Generator', 'mhd_generator', [
        'Push a conducting fluid across a magnetic field and it becomes the armature of a generator: the field drives current across the flow, '
        'into $(item)MHD Electrodes$() on the channel walls. No turbine, no moving parts.',
        'Power per cubic metre is p = σu²B²k(1 − k): conductivity, speed squared, field squared. The load factor k is the share of the voltage '
        'the outside circuit takes; one half gives the most - unless the current density, j = σuB(1 − k), would arc the electrodes away.',
        'Brine conducts a little, magma a little less but the Interior has oceans of it, and $(fluid)Stellar Plasma$() conducts ten thousand '
        'times better. The pumps limit how fast a dense fluid can be driven.'],
        adv='magnetohydrodynamics', sort=4, spot=['mhd_generator', 'mhd_field_magnet', 'mhd_electrode'])
