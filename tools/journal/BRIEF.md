# Quantum Codex rewrite brief

You are rewriting the in-game guide ("Quantum Codex") of a hard-sci-fi Minecraft mod, Theoretical Sciences, in the voice of a
scientist's research journal: the reader is following one researcher who discovers each concept, experiments, gets
things wrong, and records what finally works. First person, curious, precise, a little awed. Real physics/maths is explained
accurately but briefly (what the idea actually is in reality), then the in-game mechanics. Think: Spectrum's guidebook or
Thaumcraft's Thaumonomicon, but a physicist's lab notebook.

## Source
The current book is generated JSON at
`/home/claude/qh/src/main/resources/assets/theoreticalsciences/patchouli_books/codex/en_us/entries/<category>/<entry>.json`.
Each has `pages`; only pages of type `patchouli:spotlight` and `patchouli:text` are yours to rewrite (in their order of
appearance). Leave multiblock / crafting pages alone (don't mention them).

## HARD RULES (gameplay accuracy)
- Every mechanical fact in the original must survive: item/block/machine names, numbers, coordinates, research names,
  what is required, where things are found, effects, config names, commands. You may add physics context, never invent
  new mechanics, numbers, items or recipes. If unsure, keep the original wording of that fact.
- Keep item and machine names exactly as written in the original (they're real in-game names).

## Format (Patchouli text formatting)
- Allowed macros only: `$(item)Name$()` for items/blocks/machines, `$(thing)term$()` for concepts/mechanics/places,
  `$(fluid)Name$()` for fluids, `$(br2)` paragraph break, `$(li)` list bullet, `$(br)` line break. Nothing else
  (no $(l:...) links, no colour codes, no $(k), no markdown).
- Page size limits (they are physical book pages): spotlight page (the first page if the entry starts with a spotlight)
  <= 230 visible characters; text pages <= 420 visible characters (macros don't count). Split into more pages rather than
  exceeding. Aim for 1.3-2x the original length overall, not more.
- Open entries in-journal, e.g. "Day 41. ...", "Log, the Bulk, third descent: ...", "Note to self: ..." - vary it, don't
  date every page. Keep it readable: one clear idea per page, mechanics stated plainly ("The Ordinal Recursor takes...").
- An optional new title per entry is allowed (<= 24 chars) if it reads better in the journal voice; otherwise omit.

## Output
Write ONE JSON file per category: `/home/claude/qh/tools/journal/<category>.json`:
```
{ "<entry_id>": { "title": "optional", "texts": ["page 1 (replaces the spotlight text if the entry has one)", "page 2", ...] }, ... }
```
`texts[i]` replaces the i-th spotlight/text page; extra texts beyond the original count are inserted as new text pages
right after them. `texts` must have at least as many items as the original has spotlight+text pages. Include every entry
in your categories. Validate the file parses (python3 -c "import json;json.load(open(...))") and check the length limits
with a quick script (strip `$(...)` before counting). Report only: categories done, entry count, any fact you weren't sure
how to keep.
