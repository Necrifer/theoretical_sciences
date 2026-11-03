"""
Round 20.

* Creative items of other mods: no longer in the Transcendental's reliquaries nor in the Core's MATERIALIZE list - only
  the rebuilt Core's Forge makes them (transcend/CoreForge.all()). The reliquaries now hold the controllers of the great
  endgame multiblocks.
* The Transcendent Cache is an Avaritia-style infinity chest (storage/InfinityCache*): 243 slots of up to 2^31-1 items,
  keeps its contents when broken; made on the Extreme Assembly Table.
* Interface Ports join AE2 networks (compat/ae2/Ae2Link): the multiblock draws ingredients from the network and stores
  its products there.
* Machine slots and tanks are as large as the game allows (2,147,483,647). The recipe generator's own caps are in
  recipe_tuning.json (item_cap, fluid_cap_multiblock, fluid_cap_single).
"""
import os

NS = 'theoreticalsciences'
BLOCKS_KIND = {'transcendent_cache': dict(kind='infinity_cache', hardness=10.0, tool='diamond')}

LANG = {
    'tooltip.theoreticalsciences.transcendent_cache': 'Holds 243 kinds of item, up to 2,147,483,647 of each. Keeps its contents when broken.',
    'tooltip.theoreticalsciences.transcendent_cache_holds': 'Holds %s kinds of item',
}


def content(C):
    for b in C['blocks']:
        if b['id'] in BLOCKS_KIND:
            b.update(BLOCKS_KIND[b['id']])


def apply(g):
    w, REC = g['w'], g['REC']
    g['LANG'].update(LANG)
    old = os.path.join(REC, 'crafting', 'transcendent_cache.json')
    if os.path.exists(old):
        os.remove(old)
    w(os.path.join(REC, 'extreme', 'transcendent_cache.json'), {
        'type': f'{NS}:extreme_shaped',
        'pattern': ['SSSOOOSSS',
                    'SHHOCOHHS',
                    'SHBBNBBHS',
                    'OOBXAXBOO',
                    'OCNAPANCO',
                    'OOBXAXBOO',
                    'SHBBNBBHS',
                    'SHHOCOHHS',
                    'SSSOOOSSS'],
        'key': {k: {'item': f'{NS}:{v}'} for k, v in {
            'S': 'transcendent_stone', 'O': 'omnium_block', 'H': 'hilbert_locker', 'C': 'bulk_cache', 'B': 'cosmic_neutronium_block',
            'N': 'aleph_one', 'X': 'observatory_cache', 'A': 'absolute_infinity', 'P': 'pocket_universe'}.items()},
        'result': {'id': f'{NS}:transcendent_cache', 'count': 1}})


def finish(g):
    te = g['text_entry']
    te('world', 'transcendent_cache', 'The Transcendent Cache', 'transcendent_cache', [
        'A cache the Transcendental keeps for itself - and the one you can build: 243 kinds of item, and up to $(thing)2,147,483,647$() of each. Whatever you put in joins the pile of the same item. Scroll to move through its 27 rows; a click or shift-click takes one normal stack at a time.',
        'Pipes put into it and take out of it (one stack at a time). Broken, it keeps everything inside. It is made on the $(item)Extreme Assembly Table$().'],
        sort=41, spot=['transcendent_cache'])
    te('basics', 'me_ports', 'Interface Ports and ME Networks', 'interface_port', [
        'With Applied Energistics 2 installed, an $(item)Interface Port$() of a formed multiblock connects to ME cables (it needs a channel). The multiblock then takes recipe ingredients - items and fluids - straight from the network, after whatever is in its own slots, and puts everything it makes into the network.',
        'Every machine slot and tank holds as much as the game can count (2,147,483,647).'],
        sort=7)
    te('endgame', 'creative_forge', 'Creative Things', 'eschaton_core', [
        'The creative items of other mods (infinite chests, tanks, energy cells...) are no longer simply handed out by the Core, nor kept in the Transcendental\'s reliquaries - those now hold the controllers of the great machines of the endgame.$(br2)Only the rebuilt Core\'s $(thing)FORGE$() can make creative items, each from Eschaton Cores, Omnipotent Ingots, Absolute Singularities and Transcendence Crystals - and each needs its own universe tuned.',
        'Pack makers: the Forge finds creative items by itself (a list of known ones, and any item whose id says "creative"). The item tag $(thing)theoreticalsciences:forge_only$() adds items the search misses - or late-endgame items to keep behind the Forge - and $(thing)theoreticalsciences:forge_exempt$() takes out ones it picked up by mistake.'],
        sort=32)
