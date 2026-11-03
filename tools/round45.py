"""
Round 45 (1.4.0, "Under the Stars"): Astrophysics, Tier II - instruments that need the real sky.

The 1.1 design left Astrophysics at Tier I (a mirror blank and the Spectrograph). This round gives it its working
instruments, each a multiblock with its own silhouette and a physics model that reads the world around it (1.4.0
built-ins: sky, night, sun, moon, rain, alt, rfi - see PhysicsModel):

* Reflecting Telescope: a drum and a dome with an open slit. Only on a clear night under open sky. Aperture and exposure
  set the faintest star it records (limiting magnitude); seeing - better the higher it stands - blurs it, and short
  exposures freeze the seeing (lucky imaging). The Moon brightens the sky.
* Solar Furnace: an Odeillo-style concentrator - heliostats, a parabolic mirror wall, a target on a tower at its focus.
  Only by day. The target's temperature follows radiative balance, T = (αCG/εσ)^(1/4).
* Radio Telescope: a dish on an alt-azimuth mount. Day or night, rain or not - but every machine running nearby adds
  noise. The radiometer equation, SNR = T_src·√(Bτ)/T_sys; a spectral line is diluted in too wide a band; a pulsar's
  pulse is smeared by dispersion across a wide one.

Observations teach: the telescopes' recipes give Insight each run. Meteorite craters (iron and stony) appear in the
Overworld; iron meteorites hold the iron-nickel of shattered planetary cores, and stony ones presolar silicon carbide -
stardust older than the Sun, and the first natural moissanite ever found.

Every recipe here is new (r45_ files) and frozen out of the older recipes' expansion, as 1.3.5's were.
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
# (colours follow what each thing is made of: glass and silver, quartz, corundum, copper, iron)
ITEMS = [
    _I('photographic_plate', 'Photographic Plate', 'plate', '#6a7078', '#d8e0e8',
       'A glass plate coated with a light-sensitive emulsion set in gelatin. Left open under the sky for an hour, it records stars far too '
       'faint for any eye.'),
    _I('star_field_plate', 'Star Field Plate', 'plate', '#101828', '#f0f4ff',
       'A developed plate of a patch of sky: thousands of stars, their positions measured to a fraction of an arcsecond.'),
    _I('stellar_spectrum_plate', 'Stellar Spectrum Plate', 'plate', '#281838', '#ffc080',
       'Starlight spread into a spectrum on the plate. The dark lines across it give each star\'s temperature and what it is made of.'),
    _I('deep_sky_plate', 'Deep Sky Plate', 'plate', '#100818', '#c080ff',
       'A long exposure on a dark, moonless night: faint smudges that are whole galaxies, millions of light-years away.'),
    _I('planetary_plate', 'Planetary Plate', 'plate', '#302010', '#ffd090',
       'The sharpest of a thousand short exposures of a planet, taken in the instants the air held still: belts, moons, a shadow.'),
    _I('hydrogen_line_map', 'Hydrogen Line Map', 'codex', '#102018', '#60ffa0',
       'A chart of the 21-centimetre glow of cold hydrogen across the Galaxy. The line\'s Doppler shift gives how fast each cloud moves.'),
    _I('pulsar_timing_record', 'Pulsar Timing Record', 'codex', '#101820', '#80e0ff',
       'Arrival times of a pulsar\'s pulses, steady to a millionth of a second over months: a clock spun up in a dead star.'),
    _I('fused_silica', 'Fused Silica', 'crystal', '#c8d8e0', '#ffffff',
       'Quartz melted in the Solar Furnace and cooled to a glass. It barely expands with heat, so a mirror made of it keeps its figure.'),
    _I('sapphire_window', 'Sapphire Window', 'lens', '#3050c0', '#a0c0ff',
       'A disc of synthetic sapphire, aluminium oxide grown from the melt: hard, clear from ultraviolet to infrared, and it stays clear '
       'when frozen.'),
    _I('heliostat_mirror', 'Heliostat Mirror', 'lens', '#a0b0c0', '#ffffff',
       'A flat silvered mirror on a two-axis mount that turns with the Sun, so its reflection stays on one spot all day.'),
    _I('cryogenic_receiver', 'Cryogenic Receiver', 'capacitor', '#8a5030', '#ffa060',
       'An amplifier cooled near absolute zero behind a sapphire window, in a copper feed. The colder it is, the less it hisses over the '
       'faint signal from the sky.'),
    _I('meteoric_iron', 'Meteoric Iron', 'chunk', '#3a3a40', '#b8b8c0',
       'Iron and nickel from the core of a planetesimal that shattered billions of years ago. Etched, it shows crossed plates that grew '
       'only a degree in a million years.'),
    _I('chondrite_fragment', 'Chondrite Fragment', 'chunk', '#5a4a3a', '#b09070',
       'Stone that never melted since the planets formed: grains of olivine and metal, and among them dust older than the Sun.'),
    _I('presolar_grains', 'Presolar Grains', 'dots', '#2a5060', '#e0ffff',
       'Silicon carbide stardust condensed in the winds of stars that died before the Sun was born. Moissan named the mineral moissanite '
       'when he found it in a meteorite.'),
]
BLOCKS = [
    # the Reflecting Telescope
    _B('observatory_wall', 'Observatory Wall', 'casing', '#c8ccd0', '#7080a0', light=0),
    _B('observatory_dome', 'Observatory Dome', 'tiles', '#e4e8ec', '#a0aec0', light=0),
    _B('telescope_tube', 'Telescope Tube', 'beampipe', '#202630', '#a0b8e0', light=0),
    _B('primary_mirror_cell', 'Primary Mirror Cell', 'rfcavity', '#30384a', '#e0ecff', light=4),
    _B('telescope_pier', 'Telescope Pier', 'coilblock', '#6a6e72', '#b0b4b8', light=0),
    # the Solar Furnace
    _B('heliostat', 'Heliostat', 'cryo', '#5a7088', '#f0f8ff', light=2),
    _B('concentrator_mirror', 'Concentrator Mirror', 'tiles', '#90a4b8', '#f8fcff', light=3),
    _B('solar_receiver', 'Solar Receiver', 'vent', '#3a1810', '#ffb040', light=10),
    # the Radio Telescope
    _B('radio_dish_panel', 'Radio Dish Panel', 'tiles', '#d0d4d4', '#8a9298', light=0),
    _B('dish_truss', 'Dish Truss', 'riftframe', '#5a6068', '#c0c8d0', light=0),
    _B('feed_horn', 'Feed Horn', 'injector', '#7a4a2a', '#ffa060', light=4),
    _B('alt_az_mount', 'Alt-Azimuth Mount', 'coilblock', '#4a4c50', '#c8a050', light=0),
    # meteorite craters (world blocks)
    _B('impact_breccia', 'Impact Breccia', 'bulkstone', '#4a4440', '#8a8078', kind='simple', light=0, hardness=2.0, tool='stone',
       lore='Rock shattered and welded back together in the instant of an impact. It lines a meteorite crater\'s bowl and rim.'),
    dict(id='iron_meteorite', name='Iron Meteorite', art='ore', base='impact_breccia', c1='#2a2a30', c2='#c0c0c8', kind='ore',
         drops='meteoric_iron', light=0, hardness=6.0, tool='stone',
         lore='The iron-nickel mass of a meteorite, half buried where it struck. Found in craters out in open country.'),
    dict(id='chondrite', name='Chondrite', art='ore', base='impact_breccia', c1='#3a3028', c2='#c0a070', kind='ore',
         drops='chondrite_fragment', light=0, hardness=4.0, tool='stone',
         lore='A stony meteorite: rock that has not melted since the planets formed. Found in craters out in open country.'),
]
FLUIDS = []

# multiblock key characters: the single ASCII symbols are nearly all taken, so these are Greek
KEY_ADD = {
    'κ': ['observatory_wall'], 'λ': ['observatory_wall', 'interface_port'], 'δ': ['observatory_dome'], 'τ': ['telescope_tube'],
    'μ': ['primary_mirror_cell'], 'π': ['telescope_pier'],
    'η': ['heliostat'], 'σ': ['concentrator_mirror'], 'θ': ['solar_receiver'],
    'ω': ['radio_dish_panel'], 'ψ': ['dish_truss'], 'φ': ['feed_horn'], 'α': ['alt_az_mount'], 'β': ['alt_az_mount', 'interface_port'],
}


# =================================================================================== SHAPES
def reflecting_telescope():
    """A round drum with a hemispherical dome, its slit open north to south; inside, the pier, the mirror cell and the
    tube pointing up through the slit."""
    s = shapes.Shape(11, 12, 11)
    c, R = 5, 5.4
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - c, z - c)
            if d <= R + 0.1:
                s.put(x, 0, z, 'κ')                                       # floor
                for y in range(1, 6):
                    if d >= R - 0.9:
                        s.put(x, y, z, 'κ')                               # the drum
                    else:
                        s.put(x, y, z, '.')
            for y in range(6, 12):
                h = (y - 6) * 1.05
                r_ = math.sqrt(max(0.0, R * R - h * h))
                if d <= r_ + 0.3:
                    shell = d >= r_ - 1.0 or y == 11
                    s.put(x, y, z, 'δ' if shell else '.')
    for y in range(6, 12):
        for z in range(11):
            if (c, y, z) in s.v and z >= 2:
                s.put(c, y, z, '.')                                       # the slit, open from the zenith down the south side
    s.column(c, c, 1, 2, 'π')                                             # pier
    s.put(c, 3, c, 'μ')                                                   # primary mirror
    for y in range(4, 9):
        s.put(c, y, c, 'τ')                                               # the tube, up into the slit
    s.center = (c, 10.0, c)
    s.ports('κ', 'λ', 4, y=0)
    s.v[(c, 1, 0)] = 'C'
    s.ctrl = (c, 1, 0)
    return s


def solar_furnace():
    """After the furnace at Odeillo: two terraces of heliostats at the front, the target on a tower in the middle, and
    behind it a parabolic mirror wall that brings their light to a point on the target."""
    s = shapes.Shape(15, 13, 12)
    cx, cy = 7, 7
    s.box(0, 0, 0, 14, 0, 11, 'κ')                                        # the platform
    for x in range(1, 14, 2):
        s.put(x, 1, 0, 'η')                                               # the low terrace
    s.box(0, 1, 1, 14, 1, 1, 'κ')
    for x in range(0, 15, 2):
        s.put(x, 2, 1, 'η')                                               # the high terrace
    s.column(cx, 3, 1, 6, 'κ')                                            # the target tower
    s.put(cx, 7, 3, 'θ')                                                  # the target, at the focus
    for x in range(15):
        for y in range(1, 13):
            r2 = (x - cx) ** 2 + (y - cy) ** 2
            if r2 > 7.4 ** 2:
                continue
            z = round(10 - r2 / 28)                                       # paraboloid, focal length 7 - its focus is the target
            s.put(x, y, z, 'σ')
            if x % 3 == 1 and y in (2, 5, 8, 11):
                for zz in range(z + 1, 12):
                    s.put(x, y, zz, 'κ')                                  # struts from the mirror to its backing frame
    for x in range(1, 15, 3):
        top = max(y for (xx, y, zz), ch in s.v.items() if xx == x and ch == 'σ')
        s.column(x, 11, 1, top, 'κ')                                      # the frame's posts
    s.center = (cx, cy, 3.7)
    s.ports('κ', 'λ', 4, y=0)
    s.v[(cx, 1, 0)] = 'C'
    s.ctrl = (cx, 1, 0)
    return s


def radio_telescope():
    """A dish on an alt-azimuth mount: a turntable and a pedestal, a paraboloid of panels on a backing truss, and a
    quadrupod holding the feed horn at the focus."""
    s = shapes.Shape(17, 13, 17)
    c = 8
    s.disc(c, c, 0, 3.6, 'α')                                             # turntable
    s.box(7, 1, 7, 9, 3, 9, 'α')                                          # pedestal
    s.put(c, 4, c, 'α')                                                   # elevation bearing
    f = 5.0
    for x in range(17):
        for z in range(17):
            r = math.hypot(x - c, z - c)
            if r > 8.2:
                continue
            y0 = 5 + r * r / (4 * f)
            for y in range(int(math.floor(y0)), int(math.floor(y0 + max(0.6, r / (2 * f)))) + 1):
                if y < 13:
                    s.put(x, y, z, 'ω')                                   # the dish surface, no gaps on its steep rim
    for dx, dz in ((4, 0), (-4, 0), (0, 4), (0, -4)):
        s.line((c, 4, c), (c + dx, 5 + 16 / (4 * f), c + dz), 'ψ')        # backing truss
    for dx, dz in ((5, 0), (-5, 0), (0, 5), (0, -5)):
        s.line((c + dx, round(6 + 25 / (4 * f)), c + dz), (c, 9, c), 'ψ')  # quadrupod legs to the focus
    s.put(c, 10, c, 'φ')                                                  # the feed horn at the focus
    s.put(c, 9, c, 'ψ')
    for y in (5, 6, 7, 8):
        if s.get(c, y, c) in (None, 'ω'):
            s.put(c, y, c, 'ω' if y == 5 else '.')
    s.center = (c, 9.6, c)
    s.ports('α', 'β', 4, y=0)
    s.v[(c, 1, 7)] = 'C'
    s.ctrl = (c, 1, 7)
    return s


shapes.BUILDERS.update({'reflecting_telescope': reflecting_telescope, 'solar_furnace': solar_furnace, 'radio_telescope': radio_telescope})

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'reflecting_telescope': ('Reflecting Telescope',
                             'A mirror under a dome, its slit open to the night. The bigger the mirror and the longer the exposure, the fainter '
                             'the stars it records - on a clear, dark night, under open sky.'),
    'solar_furnace': ('Solar Furnace',
                      'Heliostats turn with the Sun and feed a parabolic mirror wall, which brings their light to a point. Concentrated ten '
                      'thousand times, sunlight melts what no fuel flame can.'),
    'radio_telescope': ('Radio Telescope',
                        'A dish that listens to the sky: to cold hydrogen, to pulsars. It works by day and in rain, but every machine running '
                        'nearby is noise in its receiver.'),
}
MACHINE_CAT = {k: 'astrophysics' for k in MACHINE_TEXT}
MACHINE_RESEARCH = {'reflecting_telescope': 'telescope_optics', 'solar_furnace': 'stellar_astrophysics', 'radio_telescope': 'radio_astronomy'}
TIER = {k: 2 for k in MACHINE_TEXT}
_S = 'spacetime_casing'
MC = {
    'reflecting_telescope': (['DMD', 'TST', 'PWP'], {'D': 'observatory_dome', 'M': 'primary_mirror_cell', 'T': 'telescope_tube', 'S': _S,
                                                     'P': 'telescope_pier', 'W': 'observatory_wall'}),
    'solar_furnace': (['HCH', 'RSR', 'WWW'], {'H': 'heliostat', 'C': 'concentrator_mirror', 'R': 'solar_receiver', 'S': _S, 'W': 'observatory_wall'}),
    'radio_telescope': (['PFP', 'TST', 'MAM'], {'P': 'radio_dish_panel', 'F': 'feed_horn', 'T': 'dish_truss', 'S': _S, 'A': 'alt_az_mount',
                                                'M': 'minecraft:iron_block'}),
}
EXTRA = {
    'reflecting_telescope': 'Works only at night, with open sky above the slit and no rain. Set the aperture and the exposure: the limiting '
                            'magnitude rises with both, falls under a bright Moon, and suffers from the seeing - better the higher the '
                            'telescope stands. Very short exposures freeze the seeing for sharp planetary plates. Every plate gives Insight.',
    'solar_furnace': 'Works only by day, under open sky and without rain. Set the concentration and the shutter: the target heats to '
                     'T = (αCG/εσ)^(1/4), and must sit between the melting point and where the material boils off - or the crucible fails.',
    'radio_telescope': 'Works day and night, rain or shine, but needs a view of the sky - and every other machine running within 24 blocks '
                       'raises the system temperature. Tune to the line, narrow the band to it, and integrate long enough: SNR = '
                       'T·√(Bτ)/T_sys. For pulsars, too wide a band smears the pulse. Every map and record gives Insight.',
}

# =================================================================================== PHYSICS
_r12.model('reflecting_telescope', 'Reflecting Telescope',
           'A mirror of diameter D gathers light in proportion to D², so the faintest star it records - the limiting magnitude - rises by 5·log₁₀ D, '
           'and by 2.5·log₁₀ for every tenfold longer exposure. The air smears every star into a seeing disc; the mirror itself cannot do better '
           'than its diffraction limit, 1.22λ/D. A moonlit sky is brighter, and drowns the faintest stars.',
           'Turbulence: the seeing varies by ±15%·d.',
           [_P('D', 'Aperture D', 'm', 0.1, 2.4, 0.01, 1.0, 'How much of the primary mirror is used. More gathers more light and resolves finer.'),
            _P('t', 'Exposure t', 's', 0.001, 3600, 0.001, 60, 'How long the plate is open. Longer reaches fainter stars - and lets the air blur them.')],
           [_D('see', 'Seeing', '″', '2.5*exp(-pos(alt - 64)/120)*(1 + 0.15*d)', 'θ_see ≈ 2.5″·e^(−(h − 64)/120)',
               'How far the turbulent air smears a star. It improves the higher the telescope stands.'),
            _D('th', 'Diffraction limit', '″', '0.1384/D', 'θ = 1.22λ/D', 'The finest detail the mirror itself can split, for green light.'),
            _D('fr', 'Seeing frozen', '', '0.3 + 0.7*t/(t + 0.05)', 'lucky imaging', 'In exposures shorter than the air\'s coherence time '
                                                                              '(about 50 ms) the seeing is partly frozen.', show=False),
            _D('res', 'Resolution', '″', 'sqrt(th^2 + (see*fr)^2)', 'θ = √(θ_diff² + θ_see²)', 'The size of a star\'s image on the plate.'),
            _D('msky', 'Sky brightness', 'mag/□″', '21.6 - 3.2*moon - 8*(1 - night)', 'μ_sky', 'Darker skies have larger numbers. The Moon '
                                                                                                   'brightens it by up to 3 magnitudes.'),
            _D('mlim', 'Limiting magnitude', 'mag', '13.2 + 5*log10(D) + 1.25*log10(t) + 0.5*(msky - 21.6) - 2.5*log10(max(res, 0.05))',
               'm_lim ≈ m₀ + 5 log D + 1.25 log t + ½(μ − μ₀) − 2.5 log θ', 'The faintest star the plate records. Each magnitude is '
                                                                            '2.5 times fainter.')],
           [_C('dark', 'stop', 'night - 0.9', 'The sky is too bright: a telescope works only at night', 'night'),
            _C('sky', 'stop', 'sky - 0.5', 'Something blocks the sky above the slit', 'open sky'),
            _C('clouds', 'stop', '0.5 - rain', 'Overcast: no stars tonight', 'no rain')],
           rate='clamp((1 + 0.3*(mlim - mreq))/(1 + t/1200), 0.2, 2)', rate_formula='rate = (1 + 0.3·(m_lim − m_need)) / (1 + t/1200 s)',
           rate_desc='Deeper than the target needs is faster; very long exposures are slower.',
           heat='0.02', idle_heat='0', rvars=[_V('mreq', 'Faintest magnitude needed', 9)], kappa=40, heat_capacity=40)

_r12.model('solar_furnace', 'Solar Furnace',
           'Sunlight brings about 1000 W/m² at noon, less as the Sun sinks. Concentrated C times and let through a shutter of opening g, it heats '
           'the target until it radiates away what it absorbs: αgCG = εσT⁴. The target\'s temperature decides what melts - and what boils off.',
           'Thin cloud: the sunlight varies by ±3%·d.',
           [_P('C', 'Concentration C', '×', 1, 10000, 1, 1000, 'How many suns the heliostats and the mirror bring to the target.'),
            _P('g', 'Shutter opening g', '', 0, 1, 0.001, 0.5, 'The louvres in front of the target. Close them to hold the temperature down.')],
           [_D('G', 'Sunlight', 'W/m²', '1000*sun*(1 + 0.03*d)', 'G = 1000·sin(elevation)', 'Sunlight arriving now, by the Sun\'s height.'),
            _D('q', 'Flux on target', 'MW/m²', 'g*C*G/1e6', 'q = g·C·G', 'Power falling on each square metre of the target.'),
            _D('Tt', 'Target temperature', 'K', '(0.9*q*1e6/(0.85*5.67e-8) + T_amb^4)^0.25', 'T = (αq/εσ + T_amb⁴)^(1/4)',
               'Where the target radiates away what it absorbs.'),
            _D('mT', 'Above the melting point', 'K', 'Tt - Tm', 'T − T_m', 'Positive: the charge melts.')],
           [_C('sun', 'stop', 'sun - 0.15', 'The Sun is too low (or down)', 'Sun up'),
            _C('sky', 'stop', 'sky - 0.5', 'Something shades the mirrors', 'open sky'),
            _C('clouds', 'stop', '0.5 - rain', 'Overcast: no sunlight to concentrate', 'no rain'),
            _C('crucible', 'damage', '3200 - Tt', 'The crucible is melting (above 3,200 K)', 'T < 3200 K')],
           rate='clamp(1 + mT/400, 0.3, 2)', rate_formula='rate = 1 + (T − T_m)/400 K', rate_desc='The hotter above its melting point, the faster the '
                                                                                               'charge melts.',
           heat='min(10, q)', idle_heat='0', rvars=[_V('Tm', 'Melting point (K)', 1986)], kappa=40, heat_capacity=60)

_r12.model('radio_telescope', 'Radio Telescope',
           'A source of flux S raises the antenna temperature by T = S·A_eff/2k. The receiver hears it over the system temperature T_sys - its own '
           'noise, the Galaxy\'s background (strong at low frequency), rain, and the switching noise of every machine nearby. Integrating a band B '
           'for a time τ beats the noise down: SNR = T·√(Bτ)/T_sys. A spectral line narrower than the band is diluted; a pulsar\'s pulse is smeared '
           'by dispersion across the band, by 8.3 µs·DM·B/ν³.',
           'Receiver gain drift: ±5%·d.',
           [_P('nu', 'Frequency ν', 'MHz', 100, 10000, 0.1, 1420, 'Where the receiver is tuned. Cold hydrogen shines at 1420.4 MHz.'),
            _P('B', 'Bandwidth B', 'MHz', 0.001, 500, 0.001, 1, 'How wide a band is taken in. Wider collects more noise-beating samples, but '
                                                                  'dilutes a line and smears a pulse.'),
            _P('tau', 'Integration τ', 's', 1, 36000, 1, 600, 'How long each point is observed.')],
           [_D('Tsys', 'System temperature', 'K', '20 + 6*rfi + 12*rain + 60*(nu/408)^(-2.6)', 'T_sys = T_rx + T_RFI + T_rain + T_gal(ν)',
               'Noise the receiver hears from everything but the source.'),
            _D('Sf', 'Source flux at ν', 'Jy', 'S*(nu/1400)^al', 'S(ν) = S₁₄₀₀·(ν/1400)^α', 'The source\'s brightness at this frequency.'),
            _D('Ta', 'Antenna temperature', 'K', '0.107*Sf*(1 + 0.05*d)', 'T = S·A_eff/2k', 'What the source adds (A_eff ≈ 300 m²).'),
            _D('dil', 'Line filling', '', 'min(1, wl/B)', 'min(1, Δν_line/B)', 'The share of the band the line fills.', show=False),
            _D('inb', 'Line in band', 'MHz', 'B/2 - abs(nu - nl)', 'B/2 − |ν − ν_line|', 'Positive: the line falls inside the band.'),
            _D('SNR', 'Signal to noise', '', 'Ta*dil*sqrt(B*1e6*tau)/Tsys', 'SNR = T·√(Bτ)/T_sys', 'How far the source stands above the noise.'),
            _D('ts', 'Dispersion smearing', 'ms', '8.3e6*DM*B/nu^3', 'Δt = 8.3 µs·DM·B/ν³', 'How much a pulse is spread across the band.')],
           [_C('sky', 'stop', 'sky - 0.5', 'The dish has no view of the sky', 'open sky')],
           rate='clamp((0.6 + 0.04*(SNR - Sreq))/(1 + tau/7200), 0.2, 2)', rate_formula='rate = (0.6 + 0.04·(SNR − SNR_need)) / (1 + τ/2 h)',
           rate_desc='A clearer detection is faster; very long integrations are slower.',
           heat='0.05', idle_heat='0',
           rvars=[_V('S', 'Source flux at 1400 MHz (Jy)', 0.2), _V('al', 'Spectral index', 0), _V('wl', 'Line width (MHz)', 1e6),
                  _V('nl', 'Line frequency (MHz)', 1420.4), _V('DM', 'Dispersion measure (pc/cm³)', 0), _V('Sreq', 'SNR needed', 10)],
           kappa=40, heat_capacity=40)

_LINE = {'S': 0.2, 'al': 0, 'wl': 0.5, 'nl': 1420.4, 'DM': 0}
_r12.WINDOWS['reflecting_telescope'] = {
    'r45_star_field': ({'mreq': 9}, [('mlim', 9, 99)]),
    'r45_stellar_spectra': ({'mreq': 12}, [('mlim', 12, 99)]),
    'r45_deep_sky': ({'mreq': 16.5}, [('mlim', 16.5, 99)]),
    'r45_planetary': ({'mreq': 0}, [('res', 0, 1.1)]),
}
_r12.WINDOWS['solar_furnace'] = {
    'r45_fused_silica': ({'Tm': 1986}, [('Tt', 2000, 2450)]),
    'r45_fused_silica_sand': ({'Tm': 1986}, [('Tt', 2000, 2450)]),
    'r45_sapphire': ({'Tm': 2323}, [('Tt', 2330, 2700)]),
    'r45_sapphire_from_clay': ({'Tm': 2323}, [('Tt', 2330, 2700)]),
}
_r12.WINDOWS['radio_telescope'] = {
    'r45_hydrogen_line': (dict(_LINE, Sreq=10), [('SNR', 10, 1e12), ('inb', 0, 1e9)]),
    'r45_pulsar_timing': ({'S': 0.05, 'al': -1.6, 'wl': 1e6, 'nl': 1400, 'DM': 50, 'Sreq': 8}, [('SNR', 8, 1e12), ('ts', 0, 1)]),
    'r45_galactic_survey': (dict(_LINE, Sreq=30), [('SNR', 30, 1e12), ('inb', 0, 1e9)]),
    'r45_binary_pulsar': ({'S': 0.03, 'al': -1.6, 'wl': 1e6, 'nl': 1400, 'DM': 30, 'Sreq': 10}, [('SNR', 10, 1e12), ('ts', 0, 0.5)]),
}

# =================================================================================== RESEARCH
# (id, title, description, icon, x, y, insight, requires, cost)
RESEARCH = [
    ('telescope_optics', 'Telescope Optics',
     'A telescope is a light bucket: its mirror gathers as much more light than your eye as its area is larger, so it records stars hundreds of '
     'times fainter. What it cannot beat is the air above it. Unlocks the Reflecting Telescope and the Photographic Plate.',
     'reflecting_telescope', 4, -7, 40, ['spectroscopy'], [('telescope_mirror_blank', 4), ('spectral_plate', 4)]),
    ('meteoritics', 'Meteoritics',
     'Stones fall from the sky: iron ones from the broken cores of small planets, stony ones that never melted at all since the planets formed. '
     'Look for their craters. Unlocks the assay of meteorites.',
     'meteoric_iron', 2, -8, 25, ['observational_astronomy'], [('meteoric_iron', 2)]),
    ('stellar_astrophysics', 'Stellar Astrophysics',
     'A star\'s colour gives its surface temperature, and its brightness and temperature together give its size: a star radiates σT⁴ from every '
     'square metre. The Sun is the star close enough to use. Unlocks the Solar Furnace and deep-sky exposures.',
     'solar_furnace', 5, -8, 60, ['telescope_optics'], [('stellar_spectrum_plate', 4), ('star_field_plate', 4), ('planetary_plate', 2)]),
    ('radio_astronomy', 'Radio Astronomy',
     'Cold hydrogen, invisible to any telescope, glows at 21 centimetres; dead stars spinning hundreds of times a second pulse in radio. A dish and '
     'a cold receiver hear them through cloud and daylight. Unlocks the Radio Telescope and the Cryogenic Receiver.',
     'radio_telescope', 6, -7, 80, ['stellar_astrophysics', 'electromagnetic_induction', 'geometric_optics'], [('sapphire_window', 2), ('fused_silica', 4)]),
    ('galactic_dynamics', 'Galactic Dynamics',
     'Map how fast the Galaxy\'s hydrogen turns at each distance from its centre and the stars alone cannot hold it together: something unseen '
     'outweighs them several times over. Unlocks the long galactic survey.',
     'hydrogen_line_map', 7, -8, 110, ['radio_astronomy'], [('hydrogen_line_map', 4), ('pulsar_timing_record', 2), ('deep_sky_plate', 2)]),
]

PUZZLES = {
    'telescope_optics': dict(
        goal='A telescope collects starlight over a round mirror of diameter D; your eye collects it through a pupil of diameter p. Write G, how many '
             'times more light the telescope gathers.',
        name='Aperture gain', lhs='G',
        form='( D / p ) ^ 2',
        expr='( D / p ) ^ 2',
        ask='Find G for a 2.4 m mirror against a dark-adapted pupil of 7 mm (0.007 m).',
        vars={'G': 'light gathered, relative to the eye', 'D': 'mirror diameter (m)', 'p': 'pupil diameter (m)'},
        given={'D': 2.4, 'p': 0.007}, unit='×',
        method='Light collected is proportional to the collecting area, π(D/2)², so the ratio of two apertures is the square of the ratio of '
               'their diameters.',
        distractors=['π', '4', 'ln']),
    'meteoritics': dict(
        goal='A body of mass m strikes the ground at speed v. All of its motion becomes heat, shock and a crater. Write the energy E it brings.',
        name='Kinetic energy of an impactor', lhs='E',
        form='1 / 2 * m * v ^ 2',
        expr='0.5 * m * v ^ 2 / 1000000000000',
        ask='Find E in terajoules for the body that made Meteor Crater: m = 3e8 kg at v = 12800 m/s.',
        vars={'E': 'energy (TJ)', 'm': 'mass (kg)', 'v': 'speed (m/s)'},
        given={'m': 3e8, 'v': 12800}, unit='TJ',
        method='The work needed to bring a mass from rest to speed v is the integral of m·v dv, which is half of m v².',
        distractors=['g', 'h', '2']),
    'stellar_astrophysics': dict(
        goal='A star is a ball of radius R whose surface glows at temperature T. Every square metre of a perfect radiator gives off σT⁴. Write the '
             'star\'s total power L.',
        name='Stefan-Boltzmann law for a star', lhs='L',
        form='4 * π * R ^ 2 * σ * T ^ 4',
        expr='4 * 3.14159 * R ^ 2 * s * T ^ 4 / 100000000000000000000000000',
        ask='Find L for the Sun, in units of 10²⁶ W: R = 6.96e8 m, T = 5772 K, s = 5.67e-8 W/(m²·K⁴).',
        vars={'L': 'luminosity (10²⁶ W)', 'R': 'radius (m)', 'T': 'surface temperature (K)', 'σ': 'Stefan-Boltzmann constant', 's': 'σ (SI)',
              'π': 'pi'},
        given={'R': 6.96e8, 'T': 5772, 's': 5.67e-8}, unit='×10²⁶ W',
        method='Multiply the power per square metre by the area of the sphere, 4πR².',
        distractors=['c', 'ν', '3']),
    'radio_astronomy': dict(
        goal='A cloud of hydrogen emits a line at frequency ν _ 0. Moving away from us at speed v, it is received at a lower frequency ν. For speeds '
             'far below light\'s, write v.',
        name='Radial velocity from the line shift', lhs='v',
        form='c * ( ν _ 0 - ν ) / ν _ 0',
        expr='c * ( A - B ) / A',
        ask='Find v in km/s for the 21 cm line, A = 1420.406 MHz, received at B = 1420.000 MHz (c = 299792 km/s).',
        vars={'v': 'speed away from us (km/s)', 'c': 'speed of light (km/s)', 'ν': 'frequency', 'A': 'rest frequency (MHz)',
              'B': 'received frequency (MHz)'},
        given={'c': 299792, 'A': 1420.406, 'B': 1420.0}, unit='km/s',
        method='Each successive wave crest is emitted v/c of a wavelength further away, so the received frequency drops by the fraction v/c.',
        distractors=['λ', '2', '+']),
    'galactic_dynamics': dict(
        goal='A cloud of gas circles the centre of a galaxy at radius r, held by the gravity of the mass M inside its orbit. Write the speed v it '
             'must have to stay on a circle.',
        name='Circular orbital speed', lhs='v',
        form='sqrt ( G * M / r )',
        expr='( G * M / r ) ^ 0.5 / 1000',
        ask='Find v in km/s for the Sun\'s orbit: M = 1.8e41 kg within r = 2.5e20 m, G = 6.674e-11.',
        vars={'v': 'orbital speed (km/s)', 'G': 'gravitational constant', 'M': 'mass inside the orbit (kg)', 'r': 'orbit radius (m)'},
        given={'G': 6.674e-11, 'M': 1.8e41, 'r': 2.5e20}, unit='km/s',
        method='Gravity supplies exactly the centripetal force: G M m / r² = m v² / r.',
        distractors=['2', 'π', 'c']),
}

# =================================================================================== RECIPES
AP, SM, RO = 'astrophotography', 'solar_melting', 'radio_observation'
RECIPES = [
    # (name, type, item inputs, fluid inputs, item outputs, fluid outputs, FE/t, ticks, research, insight per run)
    ('star_field', AP, [('photographic_plate', 1)], [], [('star_field_plate', 1)], [], 128, 400, 'telescope_optics', 2),
    ('stellar_spectra', AP, [('photographic_plate', 1), ('spectral_plate', 1)], [], [('stellar_spectrum_plate', 1)], [], 128, 600,
     'telescope_optics', 3),
    ('planetary', AP, [('photographic_plate', 1)], [], [('planetary_plate', 1)], [], 128, 400, 'telescope_optics', 3),
    ('deep_sky', AP, [('photographic_plate', 2)], [], [('deep_sky_plate', 1)], [], 128, 1200, 'stellar_astrophysics', 6),
    # 1.1.0: the Solar Furnace is the bulk route - free sunlight, a costly mirror field and only by day - so it melts in large batches
    ('fused_silica', SM, [('minecraft:quartz', 4)], [], [('fused_silica', 4)], [], 64, 200, 'stellar_astrophysics', 0),
    ('fused_silica_sand', SM, [('minecraft:sand', 16)], [], [('fused_silica', 16)], [], 64, 400, 'stellar_astrophysics', 0),
    # 1.5.0: the furnace melts alumina into a boule (solar floating-zone growth); the Optical Workshop cuts the windows
    ('sapphire', SM, [('corundum', 1)], [], [('sapphire_boule', 1)], [], 64, 400, 'stellar_astrophysics', 0),
    ('sapphire_from_clay', SM, [('minecraft:clay_ball', 8)], [], [('sapphire_boule', 1)], [], 64, 600, 'stellar_astrophysics', 0),
    ('hydrogen_line', RO, [('minecraft:paper', 1)], [], [('hydrogen_line_map', 1)], [], 256, 600, 'radio_astronomy', 3),
    ('pulsar_timing', RO, [('minecraft:paper', 1)], [], [('pulsar_timing_record', 1)], [], 256, 800, 'radio_astronomy', 4),
    ('galactic_survey', RO, [('minecraft:paper', 4)], [], [('hydrogen_line_map', 6)], [], 512, 1600, 'galactic_dynamics', 12),
    # 1.4.0 integration: a binary pulsar's orbit decays exactly as general relativity says it should - timed against a clock that
    # has itself run slow in a gravity well
    ('binary_pulsar', RO, [('minecraft:paper', 2), ('time_dilated_sample', 1)], [], [('pulsar_timing_record', 3)], [], 384, 1200,
     'galactic_dynamics', 8),
]
SPECTRO = [
    # meteorites in the Spectrograph (their assay is Meteoritics')
    ('meteoric_iron_assay', [('meteoric_iron', 4), ('spectral_plate', 1)], [('core_iron', 2), ('peridot', 1)]),
    ('chondrite_assay', [('chondrite_fragment', 4), ('spectral_plate', 1)], [('presolar_grains', 1), ('minecraft:iron_nugget', 6)]),
    # 1.4.0 integration: schist is studded with the garnets that grew in it
    ('schist_assay', [('metamorphic_schist', 4), ('spectral_plate', 1)], [('garnet', 2), ('minecraft:quartz', 2)]),
]
SPECTRO_CHANCES = {'meteoric_iron_assay': [1.0, 0.25]}       # (a pallasite: olivine crystals set in the iron)
# 1.4.0 integration: telescope data as evidence in the Bayesian Analyzer
BAYES = [
    ('posterior_stellar', [('prior_tablet', 1), ('stellar_spectrum_plate', 1)], [('posterior_tablet', 2)]),
    ('posterior_star_field', [('prior_tablet', 1), ('star_field_plate', 2)], [('posterior_tablet', 1)]),
]
CRAFTING = [
    ('photographic_plate', ['GGG', 'BQB', 'GGG'], {'G': 'minecraft:glass_pane', 'B': 'minecraft:bone_meal', 'Q': 'minecraft:quartz'},
     'photographic_plate', 6),
    ('observatory_wall', ['WIW', 'ISI', 'WIW'], {'W': 'minecraft:white_concrete', 'I': 'minecraft:iron_ingot', 'S': 'minecraft:smooth_stone'},
     'observatory_wall', 4),
    ('observatory_dome', ['IWI', 'W W', 'IWI'], {'I': 'minecraft:iron_ingot', 'W': 'minecraft:white_concrete'}, 'observatory_dome', 8),
    ('telescope_tube', ['IBI', 'I I', 'IBI'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:black_concrete'}, 'telescope_tube', 4),
    ('primary_mirror_cell', ['IMI', 'MPM', 'IMI'], {'I': 'minecraft:iron_ingot', 'M': 'telescope_mirror_blank', 'P': 'minecraft:piston'},
     'primary_mirror_cell', 1),
    ('telescope_pier', ['SSS', ' S ', 'SSS'], {'S': 'minecraft:stone_bricks'}, 'telescope_pier', 4),
    ('heliostat_mirror', ['GGG', 'GIG', 'GGG'], {'G': 'minecraft:glass_pane', 'I': 'minecraft:iron_nugget'}, 'heliostat_mirror', 4),
    ('heliostat', ['MMM', ' I ', 'IRI'], {'M': 'heliostat_mirror', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone'}, 'heliostat', 2),
    ('concentrator_mirror', ['MMM', 'MIM', 'MMM'], {'M': 'heliostat_mirror', 'I': 'minecraft:iron_ingot'}, 'concentrator_mirror', 4),
    ('solar_receiver', ['OBO', 'BFB', 'OBO'], {'O': 'minecraft:obsidian', 'B': 'minecraft:bricks', 'F': 'minecraft:blast_furnace'},
     'solar_receiver', 1),
    ('radio_dish_panel', ['BBB', 'III'], {'B': 'minecraft:iron_bars', 'I': 'minecraft:iron_ingot'}, 'radio_dish_panel', 6),
    ('dish_truss', ['I I', ' I ', 'I I'], {'I': 'minecraft:iron_ingot'}, 'dish_truss', 6),
    ('cryogenic_receiver', ['SFS', 'CRC', 'SFS'], {'S': 'sapphire_window', 'F': 'fused_silica', 'C': 'minecraft:copper_ingot',
                                                   'R': 'minecraft:comparator'}, 'cryogenic_receiver', 1),
    ('feed_horn', ['CCC', 'CRC', 'C C'], {'C': 'minecraft:copper_ingot', 'R': 'cryogenic_receiver'}, 'feed_horn', 1),
    ('alt_az_mount', ['IPI', 'OBO', 'IPI'], {'I': 'minecraft:iron_ingot', 'P': 'minecraft:piston', 'O': 'minecraft:observer',
                                             'B': 'minecraft:iron_block'}, 'alt_az_mount', 2),
]
CRAFTING += [
    # garnet grit grinds a mirror faster and truer than sand: twice the blanks
    ('mirror_blank_garnet', ['NGN', 'GAG', 'NGN'], {'N': 'minecraft:iron_nugget', 'G': 'minecraft:glass', 'A': 'garnet'}, 'telescope_mirror_blank', 4),
    # olivine (forsterite) is a refractory: it lines furnaces as well as corundum
    ('refractory_firebox_olivine', ['KPK', 'PFP', 'KPK'], {'K': 'minecraft:bricks', 'P': 'peridot', 'F': 'minecraft:blast_furnace'},
     'refractory_firebox', 2),
]
SHAPELESS = [
    # the geoid was first mapped by comparing star positions with the plumb line: astrogeodesy
    ('geoid_model_astrogeodetic', ['star_field_plate', 'survey_map', 'survey_map', 'minecraft:clay_ball'], 'geoid_model', 2),
    # Moissan found silicon carbide in the Canyon Diablo meteorite; enough presolar grains make a crystal
    ('moissanite_from_presolar', ['presolar_grains'] * 8, 'moissanite', 1),
]
SMELT = [('meteoric_iron', 'minecraft:iron_ingot')]

LANG = {}


# =================================================================================== METEORITE CRATERS
def crater(kind):
    """A simple bowl crater, about 20 blocks across: a raised rim and a floor of impact breccia, the meteorite's
    remains in the middle and fragments thrown out across the bowl. Placed 4 blocks below the surface."""
    import round39
    t = round39.T(25, 9, 25, 45 if kind == 'iron' else 46)
    c, R, base = 12, 9.0, 4
    core = M + ('iron_meteorite' if kind == 'iron' else 'chondrite')
    for x in range(25):
        for z in range(25):
            d = math.hypot(x - c, z - c)
            if d <= R:
                floor = round(base * (d / R) ** 2)                        # a parabolic bowl, deepest in the middle
                for y in range(max(0, floor - 1), floor + 1):
                    t.set(x, y, z, M + 'impact_breccia')
                for y in range(floor + 1, 9):
                    t.set(x, y, z, 'air')
                if d > 2.5 and t.r.random() < 0.05:
                    t.set(x, floor, z, core)                              # fragments in the floor
            elif d <= R + 2.6:
                rim = round(2 * (1 - (d - R) / 2.6))                      # the raised rim, falling away outside
                for y in range(base - 1, base + rim + 1):
                    t.set(x, y, z, M + 'impact_breccia')
                for y in range(base + rim + 1, 9):
                    t.set(x, y, z, 'air')
    for x in range(c - 1, c + 2):
        for z in range(c - 1, c + 2):
            for y in (0, 1):
                if abs(x - c) + abs(z - c) + y <= 2:
                    t.set(x, y, z, core)                                  # the main mass, half buried
    return t


M = NS + ':'
SITES = {
    'meteorite_crater': (['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:desert', 'minecraft:savanna', 'minecraft:badlands',
                          'minecraft:snowy_plains', 'minecraft:taiga', 'minecraft:meadow'], 40, 14, 45045045),
}


def _structures(g):
    D, w = g['D'], g['w']
    for kind in ('iron', 'stony'):
        crater(kind).save(os.path.join(D, 'structure', f'meteorite_crater_{kind}.nbt'))
    for sid, (biomes, spacing, sep, salt) in SITES.items():
        w(os.path.join(D, 'tags', 'worldgen', 'biome', 'has_structure', sid + '.json'), {'replace': False, 'values': biomes})
        w(os.path.join(D, 'worldgen', 'template_pool', sid, 'start.json'), {
            'fallback': 'minecraft:empty',
            'elements': [{'weight': wt, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{sid}_{k}',
                                                    'projection': 'rigid', 'processors': 'minecraft:empty'}}
                         for k, wt in (('iron', 1), ('stony', 2))]})
        w(os.path.join(D, 'worldgen', 'structure', sid + '.json'), {
            'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{sid}', 'step': 'surface_structures', 'spawn_overrides': {},
            'terrain_adaptation': 'none', 'start_pool': f'{NS}:{sid}/start', 'size': 1, 'max_distance_from_center': 80,
            'use_expansion_hack': False, 'start_height': {'absolute': -4}, 'project_start_to_heightmap': 'WORLD_SURFACE_WG'})
        w(os.path.join(D, 'worldgen', 'structure_set', sid + '.json'), {
            'structures': [{'structure': f'{NS}:{sid}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': sep, 'salt': salt}})


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
    p = os.path.join(HERE, 'puzzles2', 'group_r45.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    import round42
    REC, w, q = g['REC'], g['w'], g['q']
    for name, typ, ins, fins, outs, fouts, fe, t, res, insight in RECIPES:
        g['mr']('r45_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res)
        p = os.path.join(REC, typ, 'r45_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        if insight: r['insight_per_run'] = insight
        w(p, r)
    for name, ins, outs in SPECTRO:
        res = 'metamorphism' if name == 'schist_assay' else 'meteoritics'
        g['mr']('r45_' + name, 'spectroscopy', ins, [], outs, [], energy=20000, duration=200, research=res, chances=SPECTRO_CHANCES.get(name))
        round42._unscaled(g, 'spectroscopy', 'r45_' + name)
    for name, ins, outs in BAYES:
        g['mr']('r45_' + name, 'bayesian_inference', ins, [], outs, [], energy=30000, duration=300, research='bayesian_inference')
        round42._unscaled(g, 'bayesian_inference', 'r45_' + name)
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r45_' + name, pat, key, res, n)
    for name, items, res, n in SHAPELESS:
        g['shapeless']('r45_' + name, items, res, n)
    for src, out in SMELT:
        for kind, t in (('smelting', 200), ('blasting', 100)):
            w(os.path.join(REC, 'crafting', f'r45_{src}_{kind}.json'), {
                'type': f'minecraft:{kind}', 'category': 'misc', 'ingredient': {'item': q(src)}, 'result': {'id': q(out), 'count': 1},
                'experience': 0.7, 'cookingtime': t})


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)
    _structures(g)


def apply_late(g):
    import round42
    round42._extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'),
                        [f'{NS}:{b["id"]}' for b in BLOCKS])


def _mapped_halo(g):
    """1.4.0 integration: the Halo Collector, told by a galactic survey where the dark matter is, gathers twice as much. Built from the
    Halo Collector's own dark matter recipe as it stands (after the older recipes' expansion), plus two Hydrogen Line Maps."""
    REC, w, q = g['REC'], g['w'], g['q']
    r = json.load(open(os.path.join(REC, 'halo_collection', 'dark_matter.json')))
    r = {k: v for k, v in r.items() if k not in ('expanded', 'scaled')}
    r['item_inputs'] = list(r.get('item_inputs', [])) + [{'item': q('hydrogen_line_map'), 'count': 2}]
    r['fluid_outputs'] = [dict(o, amount=o['amount'] * 2) for o in r['fluid_outputs']]
    r['research'] = q('galactic_dynamics')
    r['unscaled'] = True
    w(os.path.join(REC, 'halo_collection', 'r45_mapped_dark_matter.json'), r)


def finish(g):
    _mapped_halo(g)
    te = g['text_entry']
    te('astrophysics', 'r45_telescope', 'The Reflecting Telescope', 'reflecting_telescope', [
        'The $(item)Reflecting Telescope$() is a mirror under a dome. It works only at $(thing)night$(), with $(thing)open sky$() above its slit and '
        'no rain - and every plate it exposes teaches you something: each run gives $(thing)Insight$().',
        'Two settings decide what it records. The $(thing)aperture$() D gathers light in proportion to D², and the $(thing)exposure$() t adds up '
        'what arrives: the faintest star on the plate, the limiting magnitude, rises by 5·log D and 1.25·log t. Each magnitude is 2.5 times '
        'fainter. A full Moon brightens the sky by three magnitudes and drowns the faintest stars.',
        'The air smears every star into a $(thing)seeing$() disc of about 2.5″ at sea level - better the higher you build. The mirror cannot beat '
        'its own $(thing)diffraction limit$(), 1.22λ/D. But in exposures shorter than about 50 ms the turbulence stands still: take very short '
        'ones and keep the sharpest - $(thing)lucky imaging$() - for a $(item)Planetary Plate$().',
        'What it makes: $(item)Star Field Plates$() (magnitude 9), $(item)Stellar Spectrum Plates$() through a Spectral Plate (12), '
        '$(item)Planetary Plates$() (sharper than 1.1″), and, once you know stellar astrophysics, $(item)Deep Sky Plates$() of galaxies '
        '(16.5 - a big mirror, long exposures and a dark sky).',
        'The plates are evidence, too: the $(item)Bayesian Analyzer$() turns a Prior Tablet and a Stellar Spectrum Plate (or two Star Field Plates) '
        'into Posterior Tablets. And star positions against the plumb line are how the geoid was first mapped - a Star Field Plate and two Survey '
        'Maps make two $(item)Geoid Models$(). Mirror blanks ground with $(item)Garnet$() grit come out twice as many.'],
        adv='telescope_optics', sort=3, spot=['reflecting_telescope', 'photographic_plate', 'star_field_plate', 'deep_sky_plate'])
    te('astrophysics', 'r45_solar', 'The Solar Furnace', 'solar_furnace', [
        'After the furnace at Odeillo in the Pyrenees: $(item)Heliostats$() turn with the Sun and throw its light onto a parabolic wall of '
        '$(item)Concentrator Mirrors$(), which brings it to a point on the $(item)Solar Receiver$(). It works only by day, under open sky and '
        'without rain.',
        'Sunlight brings about 1,000 W/m² when the Sun is high - less as it sinks. Concentrated C times and let through the shutter, it heats the '
        'target until it radiates away what it takes in: αgCG = εσT⁴. At ten thousand suns that is well over 3,000 K - hotter than any flame. Too '
        'hot and the crucible itself melts (3,200 K); too cool and nothing does.',
        'Quartz or sand melts at 1,986 K into $(item)Fused Silica$(): a glass that barely expands with heat. Corundum - or, more slowly, the '
        'alumina in clay - melts at 2,323 K and freezes into a $(item)Sapphire Boule$(), for the Optical Workshop to cut into windows. Both go '
        'into the Radio Telescope\'s cold receiver.'],
        adv='stellar_astrophysics', sort=4, spot=['solar_furnace', 'heliostat', 'fused_silica', 'sapphire_window'])
    te('astrophysics', 'r45_radio', 'The Radio Telescope', 'radio_telescope', [
        'The $(item)Radio Telescope$() is a dish on an alt-azimuth mount with a $(item)Feed Horn$() at its focus. Radio passes through cloud and '
        'daylight: it works at any hour, rain or shine - but it needs a view of the sky, and every machine running within 24 blocks is $(thing)noise$() '
        'in its receiver.',
        'A source raises the antenna temperature by T = S·A/2k; the receiver hears it over the system temperature, T_sys. Taking in a band B for '
        'a time τ beats the noise down: $(thing)SNR = T·√(Bτ)/T_sys$(). The Galaxy itself roars at low frequencies, so T_sys climbs steeply below '
        'a few hundred MHz.',
        '$(thing)Hydrogen$() glows in a narrow line at 1420.4 MHz. Tune to it and keep the band narrow: a line half a megahertz wide is diluted in '
        'a wider one. $(thing)Pulsars$() are faint and broadband - take a wide band for the SNR, but not so wide that dispersion in the '
        'interstellar plasma, 8.3 µs·DM·B/ν³, smears the pulse beyond a millisecond. Higher frequencies smear less, but the pulsar is fainter there.',
        'Every map and timing record gives $(thing)Insight$(). Once you have mapped how fast the Galaxy turns, the long galactic survey '
        'gives a great deal more - and a $(thing)binary pulsar$() can be timed: its orbit shrinks exactly as general relativity predicts, measured '
        'against a $(item)Time-Dilated Sample$() as the reference clock.'],
        adv='radio_astronomy', sort=5, spot=['radio_telescope', 'feed_horn', 'hydrogen_line_map', 'pulsar_timing_record'])
    te('astrophysics', 'r45_galaxy', 'The Turning Galaxy', 'hydrogen_line_map', [
        'Measure the Doppler shift of the hydrogen line along the Galaxy and you have how fast its gas circles at each distance from the centre. '
        'If the stars were all its mass, the speed would fall off far out, as the planets\' does: v = √(GM/r).',
        'It does not. Far beyond the visible disc the gas still turns at over 200 km/s. Something unseen holds it: $(thing)dark matter$(), '
        'several times the mass of every star. The long galactic survey maps it - and gives a great deal of Insight.',
        'A $(item)Dark Matter Collector$() told where the halo is gathers twice as much: give it two $(item)Hydrogen Line Maps$() with its usual '
        'inputs.'],
        adv='galactic_dynamics', sort=6, spot=['hydrogen_line_map'])
    te('astrophysics', 'r45_meteorites', 'Meteorites', 'meteoric_iron', [
        'Sometimes a $(thing)crater$() lies in open country: a bowl of $(item)Impact Breccia$() with a raised rim, and in the middle, half '
        'buried, what made it. An $(item)Iron Meteorite$() is the core of a small planet that shattered billions of years ago; a '
        '$(item)Chondrite$() is stone that has not melted since the planets formed.',
        '$(item)Meteoric Iron$() smelts to iron. Assayed in the Spectrograph, four pieces give two blocks of the $(item)Iron-Nickel$() a '
        'planet\'s core is made of. Chondrites hold $(item)Presolar Grains$(): silicon carbide that condensed around stars that died before the '
        'Sun was born. Henri Moissan found it in the Canyon Diablo meteorite: eight grains make a $(item)Moissanite$() crystal.',
        'One iron meteorite in four is a $(thing)pallasite$(): green olivine crystals set in the metal, the boundary between a small planet\'s core '
        'and mantle. Its assay gives a $(item)Peridot$() - an olivine refractory that lines a Refractory Firebox as well as corundum does.'],
        adv='meteoritics', sort=2, spot=['meteoric_iron', 'chondrite_fragment', 'presolar_grains'])
