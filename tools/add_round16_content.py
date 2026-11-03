"""Adds round 16's Engineering Components to content.json."""
import json, os
import round16
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], round16.ITEMS)
json.dump(C, open(p, 'w'), indent=1)
print('round16 content ok:', len(C['items']), 'items')
