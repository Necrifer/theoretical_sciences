"""Round 35's content (round35)."""
import json, os
import round35
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round35.ITEMS: upsert(C['items'], round35.ITEMS)
if round35.BLOCKS: upsert(C['blocks'], round35.BLOCKS)
if round35.FLUIDS: upsert(C['fluids'], round35.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round35 content ok')
