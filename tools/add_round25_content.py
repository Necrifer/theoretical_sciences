"""Round 24's items."""
import json, os
import round25
from add_complexity_content import upsert

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
upsert(C['items'], round25.ITEMS)
json.dump(C, open(p, 'w'), indent=1)
print('round25 content ok:', len(C['items']), 'items')
