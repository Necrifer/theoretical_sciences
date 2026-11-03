"""Adds the round-10 expansion's items, blocks and fluids to content.json."""
import json, os
import round10
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], round10.ITEMS)
upsert(C['blocks'], round10.BLOCKS)
upsert(C['fluids'], round10.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round10 content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks', len(C['fluids']), 'fluids')
