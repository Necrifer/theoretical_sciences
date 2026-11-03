"""
Round 57 (1.14.2): every recipe has to earn its place.

The balance pass (balance.py) prices every recipe the way the player pays: vanilla materials at 1 each (bar the five
truly tedious ones), the mod's materials at the cost of their cheapest route, energy, time, and the machine itself -
a multiblock with its whole structure. Any route on a dearer machine, or newer on an equal one, that is not
substantially cheaper per item than the route it competes with is given what it needs (more output, or less input), and
a machine route for a vanilla item that cannot beat simply gathering it is removed. The report is written to
tools/balance_report.txt.
"""
import os

import balance

NS = 'theoreticalsciences'
ITEMS, BLOCKS, FLUIDS = [], [], []
RESEARCH, PUZZLES = [], {}
KEY_ADD, MACHINE_TEXT, MACHINE_CAT, MACHINE_RESEARCH, TIER, MC, EXTRA = {}, {}, {}, {}, {}, {}, {}
LANG = {
    # 1.1.0: Interface Ports that import and export, and a Structure Assembler bound to an ME network
    'message.theoreticalsciences.port_mode': 'Interface Port: %s',
    'message.theoreticalsciences.port_mode.0': 'passive (pipes and cables move things)',
    'message.theoreticalsciences.port_mode.1': 'exports products into the blocks beside it',
    'message.theoreticalsciences.port_mode.2': 'imports ingredients from the blocks beside it',
    'message.theoreticalsciences.port_mode.3': 'imports ingredients and exports products',
    'block.theoreticalsciences.interface_port.lore': "Takes the place of a structure's block to let items, fluids and energy in and out (or to link an ME network). Sneak-use with an empty hand to make it pull ingredients from, or push products into, the blocks beside it.",
    'item.theoreticalsciences.structure_assembler.lore': 'Use on a multiblock controller to build every missing block from your inventory. Sneak-use to preview what is missing. Sneak-use on any block of an ME network to bind it: what you lack is then taken from the network.',
    'message.theoreticalsciences.assembler_bound': 'Structure Assembler bound to the ME network at %s',
    'message.theoreticalsciences.assembler_unbound': 'Structure Assembler unbound from its ME network',
    'message.theoreticalsciences.assembler_me_offline': 'The bound ME network cannot be reached (its block is unloaded or in another dimension, or the network has no power): using your inventory only.',
    'tooltip.theoreticalsciences.assembler_bound': 'ME network: %s in %s',
}
REMOVED = []


def apply(g):
    g['LANG'].update(LANG)


def apply_late(g):
    lines, removed = balance.fix(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'balance_report.txt'))
    REMOVED.extend(removed)
    # the removed routes leave the recipe list the codex and the progression check read
    names = {os.path.basename(f)[:-5] for f in removed}
    g['MACHINE_RECIPES'][:] = [r for r in g['MACHINE_RECIPES'] if r['name'] not in names]
    print('round57 balance:', len(lines) - 1, 'changes;', len(removed), 'removed;', lines[-1])


def finish(g):
    # a second pass over the recipes as they finally ship (later steps move recipes to the big table, expand and reuse)
    lines, removed = balance.fix(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'balance_report_final.txt'))
    REMOVED.extend(removed)
    print('round57 final balance:', len(lines) - 1, 'changes;', len(removed), 'removed;', lines[-1])
