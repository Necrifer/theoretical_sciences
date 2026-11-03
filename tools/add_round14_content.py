"""Adds the round-14 containment caches to content.json."""
import json, os
import round14
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['blocks'], round14.BLOCKS)
json.dump(C, open(p, 'w'), indent=1)
print('round14 content ok:', len(C['blocks']), 'blocks')
