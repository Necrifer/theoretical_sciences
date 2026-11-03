"""Adds round 15 (the Sundering and the Rekindling): items, blocks, and the Pleroma Reconstitutor in machines.json."""
import json, os
import round15
from add_complexity_content import upsert

here = os.path.dirname(os.path.abspath(__file__))
p = os.path.join(here, 'content.json')
C = json.load(open(p))
upsert(C['items'], round15.ITEMS)
upsert(C['blocks'], round15.BLOCKS)
json.dump(C, open(p, 'w'), indent=1)
print('round15 content ok:', len(C['items']), 'items', len(C['blocks']), 'blocks')
