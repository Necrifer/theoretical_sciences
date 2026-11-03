"""Adds round 17's Chronal Accelerator to content.json."""
import json, os
import round17
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['blocks'], round17.BLOCKS)
json.dump(C, open(p, 'w'), indent=1)
print('round17 content ok:', len(C['blocks']), 'blocks')
