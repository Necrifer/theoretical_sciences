"""Round 55 content (1.14.0, Ensembles)."""
import json, os
import round56
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round56.ITEMS: upsert(C['items'], round56.ITEMS)
if round56.BLOCKS: upsert(C['blocks'], round56.BLOCKS)
if round56.FLUIDS: upsert(C['fluids'], round56.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round56 content ok')
