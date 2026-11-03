# Item art brief (round 11)

You are designing unique animated 3D pixel-art item textures for a Minecraft mod, "Theoretical Sciences" (theoretical
physics / maths / chemistry / biology sci-fi; think GregTech + Avaritia + Meatballcraft / Infinite Alchemy modpacks,
where every endgame material has its own look and animation that tells you what it *is*).

## The problem
Many items share one model and one animation (all ingots look the same; Omega Point, Theory of Everything and
Omniscience Catalyst use the same orb). Every item in your list must get its **own** model and/or animation that matches
its concept (name + lore). Two items in your list must never look alike. Singularities and maths-concept glyph tablets
are not in scope.

## How the art system works (read these files)
- `tools/render3d.py` - SDF ray-marcher: `Part(sdf_fn, material_index, pos=(x,y,z), rot=...)`, `Material(base_hex_or_rgb,
  accent, metal=, emissive=, pattern=)`, primitives `sd_sphere sd_box sd_octa sd_torus sd_cyl sd_capsule sd_hexprism sd_gem`,
  rotations `rotx roty rotz`. Look at the `Part` and `Material` classes for exact arguments.
- `tools/items3d.py` - existing builders (`b_ingot`, `b_gem`, `b_orb`, `b_device`, `b_torus`, `b_tube_curve`, `b_network`,
  `b_swarm`, ...), emissive patterns (`pat_bands pat_grid pat_spots pat_pulse_core pat_noise_shade pat_combine pat_all`) and
  2D overlay helpers (`put`, `sparkle`). A builder is `fn(c1, c2, t) -> (parts, mats, view_dict, overlay_or_None)`, t in
  [0,1) is the animation phase (the loop must be seamless: t=0 and t=1 identical). `view_dict` keys: view_pitch, view_yaw,
  zoom, extent. overlay is `fn(img)` drawing on the rendered RGBA float image (16x16 grid via `put`, image is 32x32).
- `tools/anim.py` - motions (`apply_motions`: spin spin_back rock tumble bob pulse heartbeat hue hue_soft breathe),
  overlays (`ov_glint ov_sparkles ov_orbit ov_embers ov_infall ov_rays ov_glitch ov_fractal ov_tracer`), and the
  story animations `NARRATIVES` (nar_black_hole, nar_big_bang ...) for the most special items.

## What to write
Create `tools/art11/g<N>.py` (N = your group number) containing:
```python
import math
import numpy as np
from items3d import *      # builders, patterns, Part, Material, sd_*, rot*, H, TAU ...
import anim

ITEMS = {'item_id': builder, ...}            # every id in your list (required)
RECIPES = {'item_id': (motions, overlays)}   # optional per item; default otherwise comes from its art category
NARRATIVES = {}                              # optional, for at most 3 truly special items (80-frame loop, rendered as-is)
FRAMES = {}                                  # optional loop length per item (default 40)
```
Builders may reuse/compose existing helpers, but each item's final look must be distinct: shape, proportions,
surface pattern, emissive behaviour, motion, overlay - at least two of these should differ from every other item of the
same kind. Ideas: an ingot of time-steel with a ticking clock-hand engraving and frozen dust motes; a tachyon ingot whose
glint runs backwards; a neutronium ingot so dense it bends a glint around it; plasma ingots that flicker; a dark-matter
ingot that is barely visible except for its outline; a negative-mass ingot that floats upward; an Omega Point that is a
converging light-cone; Theory of Everything as four force-rings fusing into one; Omniscience Catalyst as an eye made of
every colour. Use the item's colours c1 (base) and c2 (accent) as the palette basis.

Keep render cost sane: at most ~12 Parts per item, and avoid per-pixel Python loops in patterns (numpy only).

## Check your work (mandatory)
`cd /home/claude/qh/tools && python3 art_preview.py /tmp/claude-0/-home-claude/51dad36d-3125-5483-a3e3-ab1ab9d31062/scratchpad/art_g<N>_<k>.png id1 id2 ...`
renders 6 frames per item through the real pipeline (your group file is loaded automatically). Look at the PNG with the
Read tool. Iterate until every item reads clearly at 32x32 and differs from its neighbours. Preview in batches of ~10.
Also run `python3 -c "import anim, art11; import items3d; from art11 import g<N>"` to make sure it imports.

Do not edit any other file. Do not run the full texture generator. Report: the file written, anything you could not do.
