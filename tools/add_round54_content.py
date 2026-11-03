"""Round 54 content (1.13.0, Horizons)."""
import json, os
import round54
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round54.ITEMS: upsert(C['items'], round54.ITEMS)
if round54.BLOCKS: upsert(C['blocks'], round54.BLOCKS)
if round54.FLUIDS: upsert(C['fluids'], round54.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round54 content ok')
