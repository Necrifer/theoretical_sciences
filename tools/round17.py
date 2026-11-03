"""
Round 17.

* Machine input slots take up to 1024 of an item by hand too (they always could through pipes and hoppers).
* Ingredients from other mods are asked for in quantity (scaled with the machine's tier, like everything else), are
  drawn from the hard end of those mods (boss drops, top alloys, singularities, processors), and fluids from other
  mods are asked for as well ("optional_fluid_inputs"; used only if that fluid exists).
* The Chronal Accelerator: a field in which time runs up to 256x faster for every machine and growing thing inside.
  Ruinously expensive to build and to run - energy and Chronon Gas for every extra tick it hands out.
"""
import os

NS = 'theoreticalsciences'

BLOCKS = [dict(id='chronal_accelerator', name='Chronal Accelerator', art='emitterblock', c1='#1a0a2a', c2='#ffc040',
               kind='chrono_accel', light=3, hardness=25.0, tool='diamond')]

LANG = {
    'block.theoreticalsciences.chronal_accelerator': 'Chronal Accelerator',
    'tooltip.theoreticalsciences.chronal_accelerator': 'Time runs up to 256x faster for everything in its field. Every extra tick costs energy and Chronon Gas.',
    'message.theoreticalsciences.chronal': 'Chronal Accelerator: time x%s over %sx%sx%s blocks | %s FE stored, %s mB Chronon Gas | last tick cost %s FE',
}


def apply(g):
    mr, LANG_ = g['mr'], g['LANG']
    LANG_.update(LANG)
    mr('chronal_accelerator', 'causality_loop', [
        ('closed_timelike_curve', 4), ('temporal_singularity', 2), ('chronon_capacitor', 16), ('chronosteel_ingot', 64),
        ('tachyon_klystron', 4), ('causal_shield_lattice', 8), ('pulsar_timing_crystal', 32), ('chronoplastic_casing', 16), ('arrow_of_time_shard', 8), ('temporal_knot', 16)],
        [('chronon_gas', 64_000)], [('chronal_accelerator', 1)], [], 8_388_608, 2400, research='causality_violation')


def finish(g):
    w, text_entry = g['w'], g['text_entry']
    w(os.path.join(g['D'], 'tags', 'block', 'not_accelerated.json'), {'replace': False, 'values': []})
    text_entry('operation', 'chronal_accelerator', 'Chronal Accelerator', 'chronal_accelerator', [
        'The $(item)Chronal Accelerator$() wraps a cube of space in a closed timelike shell and lets time inside run faster - x2 up to x256. Every machine, furnace, block entity of any mod, and every growing thing (crops, saplings, ores of the Bulk) inside the cube runs that many times faster.',
        '$(thing)Right-click$() to change the speed, $(thing)sneak-right-click$() to change the size of the field (3x3x3 up to 9x9x9). A redstone signal stops it. It glows while it is working.',
        'It is ruinous to run. Every extra tick it gives one block entity costs $(thing)65,536 FE$(); every extra random tick to a growing block 16,384 FE; and every eight extra ticks one mB of $(item)Chronon Gas$(). A single machine at x256 eats about 16.7 million FE and 32 mB of gas per tick. When it runs short it simply does nothing that tick.',
        'Feed it energy and Chronon Gas from any side. Pack makers: the costs, the top speed and the largest field are in the common config; the block tag theoreticalsciences:not_accelerated lists blocks it will never touch. It never accelerates another accelerator, and the $(item)Chronal Field Projector$() (which only sets the rate of time for this mod\'s recipes) is a different thing.'],
        sort=40, spot=['chronal_accelerator'])
