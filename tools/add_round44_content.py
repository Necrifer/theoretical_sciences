"""Round 44 content (1.3.8, radiation plating)."""
import json, os
import round44
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round44.ITEMS: upsert(C['items'], round44.ITEMS)
if round44.BLOCKS: upsert(C['blocks'], round44.BLOCKS)
if round44.FLUIDS: upsert(C['fluids'], round44.FLUIDS)
# 1.3.8: blocks whose Codex node had only their name
LORE_FILL = {
    'inner_core_boundary': 'The floor of the Planetary Interior: the edge of the solid inner core, 5,150 km down. Iron here is crystallised '
        'under 330 GPa and about 5,400 C - as hot as the Sun\'s surface, yet solid because the pressure will not let it melt. No tool breaks it.',
    'mantle_breach': 'A hole bored through bedrock by the Core Drill. Step into it and you fall to the Planetary Interior; on arrival a second '
        'breach opens in the floor beside you, and stepping into that one brings you back up. It cannot be broken or moved.',
    'antisoil': 'Soil of the Antiverse, made of antiprotons, antineutrons and positrons. It is stable only because everything around it is '
        'antimatter too; brought into contact with ordinary matter, a gram of it would release about 180 TJ.',
    'oklo_rock': 'Host rock of the Natural Reactor, modelled on Oklo in Gabon, where two billion years ago groundwater slowed neutrons enough for '
        'uranium ore to run as a fission reactor for hundreds of thousands of years. It is still slightly radioactive.',
    'lead_bricks': 'Bricks of lead shielding. Lead is dense and has many electrons, so it stops gamma rays well - about half of them every 1.3 cm - '
        'but it does little against neutrons. Shields as well as the Lead Shielding Block.',
    'lead_hazard_panel': 'Lead shielding striped yellow and black to mark a radiation area. It shields as well as the Lead Shielding Block.',
    'magnetite_bricks': 'Bricks of magnetite (Fe3O4), the most magnetic mineral in nature. Iron oxide is dense, so it also shields: it stops gamma rays about two thirds as well as lead, and neutrons three times better.',
    'ex_nihilo_engine': 'Creation from nothing, made a machine. It does not convert energy - it simply has it: it gives unlimited FE to every '
        'block beside it, and charges the items of every player within 16 blocks.',
}
for b in C['blocks']:
    if b['id'] in LORE_FILL:
        b['lore'] = LORE_FILL[b['id']]
json.dump(C, open(p, 'w'), indent=1)
print('round44 content ok')
