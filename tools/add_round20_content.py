"""Round 20: the Transcendent Cache becomes an infinity chest."""
import json, os
import round20

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content.json')
C = json.load(open(p))
round20.content(C)
json.dump(C, open(p, 'w'), indent=1)
print('round20 content ok')
