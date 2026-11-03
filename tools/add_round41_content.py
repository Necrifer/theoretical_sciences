"""Round 41 content (1.3.3, Deep Riches)."""
import json, os
import round41
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round41.ITEMS: upsert(C['items'], round41.ITEMS)
if round41.BLOCKS: upsert(C['blocks'], round41.BLOCKS)
if round41.FLUIDS: upsert(C['fluids'], round41.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round41 content ok')
