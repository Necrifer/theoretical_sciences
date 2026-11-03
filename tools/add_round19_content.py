"""Adds round 19's Convergence Altar and pedestals to content.json."""
import json, os
import round19
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['blocks'], round19.BLOCKS)
json.dump(C, open(p, 'w'), indent=1)
print('round19 content ok:', len(C['blocks']), 'blocks')
