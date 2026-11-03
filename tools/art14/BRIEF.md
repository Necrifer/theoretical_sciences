# Item art brief (round 14): transformation animations

Read `/home/claude/qh/tools/art11/BRIEF.md` first - same art system, same file format, same preview tool
(`python3 art_preview.py OUT.png ids...` also loads art14 groups, which override art11). Then this brief.

## What the player asked for
"Many of the ingots, and some items, still need more animation rework. Transcendent Metal Ingot is currently the best
design framework (it morphs ingot -> sphere -> octahedron -> twisted bar, see `transcendent_metal_ingot` in
`tools/art11/g2.py`). Items are not restricted to keeping their shape all the time: the item can fade in/out,
collapse/expand, grow into the item, etc." Reference style: Meatballcraft's showpiece materials (Halite Ingot,
Unidentified Ingot, Trinity Ingot, Ingot of Shadesmar, Black Hole Alloy Ingot, the Mythic/Ascended Fractals, Quantum
Shard, Core of the Mythic Shells): items whose animation is a little *story* - the object assembles itself from
particles, collapses to a point and bursts back, splits into fragments that orbit and re-merge, flickers between
states, shows an inner core through counter-rotating shells, cycles through colours with strong glow and outline
flashes. "This especially applies to the Omnium Ingot, which is less flashy than Transcendent Metal Ingot, one of its
ingredients" - Omnium must be the most spectacular ingot of all (it is made from every other material).

## Your job (group N)
Every id in `/home/claude/qh/tools/art14/_todo_N.json` (with name, colours and lore) gets a NEW builder in
`/home/claude/qh/tools/art14/gN.py` (same exports as art11 groups: ITEMS, RECIPES, NARRATIVES, FRAMES). Look at the item's
current art11 builder first (grep its id in `tools/art11/g*.py`) and keep its concept, but make the animation a
transformation:
- Use longer loops: `FRAMES = {id: 60..80}` for every item (the texture becomes an animated strip; keep it seamless).
- Each item's loop should have at least two phases, e.g. form -> collapse -> reform, or idle -> transform -> idle.
  Tools: interpolate SDFs (`w0*sd_a + w1*sd_b`), scale an SDF by `s` via `sd(p / s) * s` (s -> 0 makes it vanish to a point),
  split into Parts that move apart/orbit/return, fade with the overlay (multiply `img[..., 3]`) and afterimages,
  emissive flashes timed to the transformation, colour shifts through the material pattern, burst sparks in the overlay.
- The ingot silhouette should be present for a good part of the loop (players must still recognise an ingot), but it may
  dissolve, unfold or be reborn in between.
- Omnium Ingot: the showpiece. Suggested: every material it is made of passes through it (colour bands of each
  ingredient), it shatters into a ring of shards that orbit as a prismatic halo, collapses to a white point, and is reborn
  as a perfect ingot with a rainbow sheen and flare. Transcendent Metal Ingot (group 0 also) may be refined but keep its
  morphing idea.
- Keep render cost sane (<= 12 Parts, numpy-only patterns). The loop must be seamless (t=0 equals t=1).

## Check (mandatory)
Preview every item (`art_preview.py`), look at the PNGs, iterate until every frame reads at 32x32 and the transformation
is visible across the 6 preview frames. `python3 -c "import anim, art14; from art14 import gN"` must import.
Only write `tools/art14/gN.py` (and scratchpad previews prefixed gN_). Report what each item now does.
