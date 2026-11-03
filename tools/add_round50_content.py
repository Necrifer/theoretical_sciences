"""Round 50 content (1.9.0, Unification)."""
import json, os
import round50
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round50.ITEMS: upsert(C['items'], round50.ITEMS)
if round50.BLOCKS: upsert(C['blocks'], round50.BLOCKS)
if round50.FLUIDS: upsert(C['fluids'], round50.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round50 content ok')
