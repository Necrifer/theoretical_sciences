"""Round 53 content (1.12.0, Earth & Spacetime)."""
import json, os
import round53
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round53.ITEMS: upsert(C['items'], round53.ITEMS)
if round53.BLOCKS: upsert(C['blocks'], round53.BLOCKS)
if round53.FLUIDS: upsert(C['fluids'], round53.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round53 content ok')
