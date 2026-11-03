# Codex lore brief (round 11)

Theoretical Sciences is a Minecraft mod about pushing physics past its edges: quantum mechanics, relativity, black holes,
string theory, transfinite mathematics, chemistry, biology, and finally the axioms of reality itself. Its guidebook
(the Codex) is written as **the player's own research notebook** - a scientist discovering each thing, in first
person, curious, precise, occasionally wry. Real science is used correctly where it exists; speculative things are
framed as extrapolations of real ideas.

Your input file `tools/lore11/_todo_<N>.json` maps ids to context: name, kind (item / block / machine / fluid),
current_text (often a single line), made_in + from (how it is made), research (the research entry that unlocks it,
"Title - description"), used_in, dropped_by (creatures), found_in_loot (structure chests), ore_in (host rock of an ore).

Write `tools/lore11/lore_<N>.json`: `{ "id": {"flavour": "...", "discovery": "..."}, ... }` for **every** id in your input.
- **flavour** (2-4 sentences, 200-420 characters): what it is and what it is like - physically, conceptually, how it
  behaves in the hand. Ground it in the real concept behind the name. No game mechanics, no numbers of items.
- **discovery** (1-3 sentences, 120-300 characters): a notebook entry on *how it was derived or found* - the idea, the
  experiment or the accident that led to it, tied to its research and its ingredients ("Feeding Casimir plates into...
  I noticed..."). For things found in the world (ores, creature drops, loot), how the player first came across it.
  For machines: the problem it was built to solve and the principle it uses. For decorative/structural blocks keep it
  short and practical.
- English, no Markdown, no quotation marks inside the text except apostrophes. Avoid repeating the name at the start
  of every flavour. Vary sentence openings across entries. Never invent recipes or mechanics that contradict the context.

Validate before finishing: `python3 -c "import json;d=json.load(open('tools/lore11/lore_<N>.json'));t=json.load(open('tools/lore11/_todo_<N>.json'));print(len(d),len(t),set(t)-set(d))"`
(run from /home/claude/qh) must print equal counts and an empty set. Do not edit any other file. Report only the file
written and any ids you were unsure about.
