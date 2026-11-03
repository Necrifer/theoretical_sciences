"""Round 39 content (1.2.6, Field Sites)."""
import json, os
import round39
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round39.ITEMS: upsert(C['items'], round39.ITEMS)
if round39.BLOCKS: upsert(C['blocks'], round39.BLOCKS)
if round39.FLUIDS: upsert(C['fluids'], round39.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round39 content ok')
