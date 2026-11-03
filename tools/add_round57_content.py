"""Round 55 content (1.14.0, Ensembles)."""
import json, os
import round57
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round57.ITEMS: upsert(C['items'], round57.ITEMS)
if round57.BLOCKS: upsert(C['blocks'], round57.BLOCKS)
if round57.FLUIDS: upsert(C['fluids'], round57.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round57 content ok')
