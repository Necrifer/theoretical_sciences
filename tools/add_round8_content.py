"""Adds the round-8 expansion's items, blocks and fluids to content.json."""
import json, os
import round8
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], round8.ITEMS)
upsert(C['blocks'], round8.BLOCKS)
upsert(C['fluids'], round8.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round8 content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks', len(C['fluids']), 'fluids')
