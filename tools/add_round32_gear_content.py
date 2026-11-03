"""Round 32's content (round32_gear)."""
import json, os
import round32_gear
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round32_gear.ITEMS: upsert(C['items'], round32_gear.ITEMS)
if round32_gear.BLOCKS: upsert(C['blocks'], round32_gear.BLOCKS)
if round32_gear.FLUIDS: upsert(C['fluids'], round32_gear.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round32_gear content ok')
