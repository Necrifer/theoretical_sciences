"""Round 49 content (1.8.0, Fields)."""
import json, os
import round49
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round49.ITEMS: upsert(C['items'], round49.ITEMS)
if round49.BLOCKS: upsert(C['blocks'], round49.BLOCKS)
if round49.FLUIDS: upsert(C['fluids'], round49.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round49 content ok')
