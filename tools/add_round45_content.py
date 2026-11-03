"""Round 45 content (1.4.0, Under the Stars)."""
import json, os
import round45
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round45.ITEMS: upsert(C['items'], round45.ITEMS)
if round45.BLOCKS: upsert(C['blocks'], round45.BLOCKS)
if round45.FLUIDS: upsert(C['fluids'], round45.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round45 content ok')
