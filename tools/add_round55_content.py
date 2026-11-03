"""Round 55 content (1.14.0, Ensembles)."""
import json, os
import round55
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round55.ITEMS: upsert(C['items'], round55.ITEMS)
if round55.BLOCKS: upsert(C['blocks'], round55.BLOCKS)
if round55.FLUIDS: upsert(C['fluids'], round55.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round55 content ok')
