"""Round 52 content (1.11.0, Fields & Nuclei)."""
import json, os
import round52
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round52.ITEMS: upsert(C['items'], round52.ITEMS)
if round52.BLOCKS: upsert(C['blocks'], round52.BLOCKS)
if round52.FLUIDS: upsert(C['fluids'], round52.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round52 content ok')
