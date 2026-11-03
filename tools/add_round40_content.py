"""Round 40 content (1.3.0, Planetary Interior)."""
import json, os
import round40
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round40.ITEMS: upsert(C['items'], round40.ITEMS)
if round40.BLOCKS: upsert(C['blocks'], round40.BLOCKS)
if round40.FLUIDS: upsert(C['fluids'], round40.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round40 content ok')
