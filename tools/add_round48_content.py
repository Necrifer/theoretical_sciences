"""Round 48 content (1.7.0, Chance & Order)."""
import json, os
import round48
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round48.ITEMS: upsert(C['items'], round48.ITEMS)
if round48.BLOCKS: upsert(C['blocks'], round48.BLOCKS)
if round48.FLUIDS: upsert(C['fluids'], round48.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round48 content ok')
