"""Round 46 content (1.5.0, Optics & Crystals)."""
import json, os
import round46
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round46.ITEMS: upsert(C['items'], round46.ITEMS)
if round46.BLOCKS: upsert(C['blocks'], round46.BLOCKS)
if round46.FLUIDS: upsert(C['fluids'], round46.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round46 content ok')
