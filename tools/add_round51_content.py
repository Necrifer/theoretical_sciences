"""Round 51 content (1.10.0, The Accretion Frontier)."""
import json, os
import round51
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round51.ITEMS: upsert(C['items'], round51.ITEMS)
if round51.BLOCKS: upsert(C['blocks'], round51.BLOCKS)
if round51.FLUIDS: upsert(C['fluids'], round51.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round51 content ok')
