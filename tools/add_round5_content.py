"""Adds the round-5 expansion's items, blocks and fluids to content.json."""
import json, os
import round5
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], round5.ITEMS)
upsert(C['blocks'], round5.BLOCKS)
upsert(C['fluids'], round5.FLUIDS)
json.dump(C, open(p, 'w'), indent=1)
print('round5 content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks', len(C['fluids']), 'fluids')
