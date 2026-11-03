"""Round 38 content (1.2, Tier II)."""
import json, os
import round38
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round38.ITEMS: upsert(C['items'], round38.ITEMS)
if round38.BLOCKS: upsert(C['blocks'], round38.BLOCKS)
if round38.FLUIDS: upsert(C['fluids'], round38.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round38 content ok')
