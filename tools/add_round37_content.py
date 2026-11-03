"""Round 37 content (1.1, Foundations)."""
import json, os
import round37
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round37.ITEMS: upsert(C['items'], round37.ITEMS)
if round37.BLOCKS: upsert(C['blocks'], round37.BLOCKS)
if round37.FLUIDS: upsert(C['fluids'], round37.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round37 content ok')
