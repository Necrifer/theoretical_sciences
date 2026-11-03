"""Round 47 content (1.6.0, Ripples)."""
import json, os
import round47
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round47.ITEMS: upsert(C['items'], round47.ITEMS)
if round47.BLOCKS: upsert(C['blocks'], round47.BLOCKS)
if round47.FLUIDS: upsert(C['fluids'], round47.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round47 content ok')
