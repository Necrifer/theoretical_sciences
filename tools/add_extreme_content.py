"""Adds the Avaritia/GTNH-style mass-crafting items (material singularities, matrix, everything-items) to content.json."""
import json, os
import extreme
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], extreme.ITEMS)
json.dump(C, open(p, 'w'), indent=1)
print('extreme content ok:', len(C['items']), 'items')
