"""
Round 29 - the release check.

* The first power: the Thermoelectric Generator burns ordinary furnace fuel (coal, charcoal, blaze rods, dried kelp),
  so the mod can be started with nothing but Minecraft installed. Before, every power source of the mod itself needed
  plates pressed with power - fine inside a big pack, a dead end on its own.
* (The glyphs of complex numbers and of bra-ket notation are known from the start - they no longer wait for drops of a containment failure - see
  tools/glyphs; chaincheck no longer hands those drops out for free.)
"""
MACHINE_TEXT = {'thermoelectric_generator': ('Thermoelectric Generator',
                                             'A furnace wrapped in thermocouples: the heat of ordinary fuel, turned straight into current by the Seebeck effect. Humble - and the first power any laboratory has.')}
MACHINE_CAT = {'thermoelectric_generator': 'basics'}
MACHINE_RESEARCH = {'thermoelectric_generator': None}
TIER = {'thermoelectric_generator': 1}
MC = {'thermoelectric_generator': (['ICI', 'RFR', 'IQI'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:copper_block', 'R': 'minecraft:redstone',
                                                          'F': 'minecraft:furnace', 'Q': 'cherenkov_quartz'})}
EXTRA = {'thermoelectric_generator': 'Burns $(item)coal$(), $(item)charcoal$(), $(item)coal blocks$(), $(item)blaze rods$() and $(item)dried kelp blocks$() for power - no research needed. Enough to run the Casimir Vacuum Press and build the Zero-Point Energy Extractor.'}

# fuel, FE per tick while burning, ticks
FUELS = [('minecraft:coal', 48, 1600), ('minecraft:charcoal', 48, 1600), ('minecraft:coal_block', 432, 1600),
         ('minecraft:blaze_rod', 96, 2400), ('minecraft:dried_kelp_block', 24, 4000)]


def apply(g):
    for item, fe, t in FUELS:
        g['mr']('burn_' + item.split(':')[1], 'thermoelectric', [(item, 1)], energy=fe, duration=t)


def finish(g):
    pass
