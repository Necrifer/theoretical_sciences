"""Round 42 content (1.3.5, Interior machines)."""
import json, os
import round42
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round42.ITEMS: upsert(C['items'], round42.ITEMS)
if round42.BLOCKS: upsert(C['blocks'], round42.BLOCKS)
if round42.FLUIDS: upsert(C['fluids'], round42.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round42 content ok')
