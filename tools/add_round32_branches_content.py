"""Round 32's content (round32_branches)."""
import json, os
import round32_branches
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
if round32_branches.ITEMS: upsert(C['items'], round32_branches.ITEMS)
if round32_branches.BLOCKS: upsert(C['blocks'], round32_branches.BLOCKS)
if round32_branches.FLUIDS: upsert(C['fluids'], round32_branches.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round32_branches content ok')
