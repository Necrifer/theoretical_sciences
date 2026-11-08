# Animated multiblocks

1.2.0 adds two things that any multiblock can use from data alone, with no Java per machine:

- **Structure animations**: parts of the machine itself that move (rings that turn, arms that swing, pistons that
  slide). Each part is drawn by the renderer from real block models, so it reads as the machine's own casing moving.
- **Effects**: a star, a sphere of open space, small bodies orbiting, and beams. They are drawn from shaders and
  procedural meshes; nothing is copied from another mod, and no textures beyond the game's own are needed.

Both are client side only. Nothing is sent over the network: each machine is drawn from what the client already knows
(formed or not, and whether a recipe is running).

## Two rules

| | Shown | Moves |
|---|---|---|
| Structure animation | whenever the machine is **formed**, idle or not | only while a recipe **runs** |
| Effects | on the spec's `trigger` (default `working`) | while shown |

A part speeds up and slows down over its own `ramp_seconds`. Its angle is added up tick by tick from its speed, so
starting, stopping and starting again never makes it jump: when the machine stops, the part comes to rest where it is
and holds there. A gap of up to 12 ticks between two recipes is smoothed over, so nothing flickers between recipes.
When the structure breaks, its parts are no longer drawn.

## Machine space

Every position is in blocks from the centre of the controller, as **`[right, up, back]`**:

- **back** points away from the controller's front (into the machine);
- **right** is the machine's own right: standing behind the controller looking out of its front, it is on your right;
- **up** is up.

So a machine is written once and turns with its controller's facing. A block "cell" at `[r, u, b]` is the block whose
centre is there.

## Structure animation files

`assets/<namespace>/ts_animations/<path>.json`, referred to as `<namespace>:<path>`.

```json
{
  "parts": [
    {
      "id": "ring_1",
      "source": { "type": "blocks", "key": { "F": "ns:casing_a", "D": ["ns:coil_t1", "ns:coil_t2"] },
                  "origin": [-63, -9, 59], "layers": [["  FFD  ", "..."], ["..."]] },
      "pivot": [0, 0, 122],
      "parent": null,
      "motion": [{ "type": "spin", "axis": "up", "deg_per_sec": 14 }],
      "ramp_seconds": 4,
      "light": "world"
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `id` | the part's name, for `parent` |
| `source` | what the part is made of (below) |
| `pivot` | the point it turns about, machine space |
| `parent` | another part's `id`: this part is carried by the parent's movement (a gear on an arm, a ring in a ring). Parents may have parents. |
| `motion` | one movement or a list of them, applied in order (below) |
| `ramp_seconds` | how long it takes to reach full speed, and to stop (default 3) |
| `light` | `world` (default): lit by the world's light at its pivot, sampled each frame; `full`: always full bright |

### Sources

- **`blocks`**: real block models, in the same layer format as the mod's own multiblocks. `layers` go bottom to top
  (up); each layer is a list of rows going back; each row is a string of characters going right. `origin` is the cell
  of the first character of the first row of the first layer. `key` maps a character to a block: `"ns:block"`,
  `"ns:block[prop=value]"`, or a list of those. Space and any character not in the key are empty.
  - **Tier slots**: where the machine's pattern accepts several tiers of a block, list them; the **first** (lowest
    tier) is the one drawn. The mesh is baked once and shared by every machine using the animation, so it does not
    change with the tier actually built.
  - Faces between two blocks of the same part are left out when the neighbour is a full, solid block (as the game
    does for placed blocks), so a ring of thousands of blocks costs about what its outside does.
- **`mesh`**: a procedural shape on a block texture.
  - `"shape": "hoop"`: a flat ring round `axis` at `centre` (default: the pivot), from `inner_radius` to
    `outer_radius`, `height` thick; `segments` (default from its size) and `tile` (blocks per texture repeat, 1).
  - `"shape": "box"`: from `from` to `to` (machine space corners).
  - Both take `texture` (a block atlas sprite, such as `"minecraft:block/iron_block"`), `color` (`"#RRGGBB"` tint) and
    `translucent` (true to draw in the translucent pass).
- **`model`**: one block model, `block` (as in a key), at `position`, scaled by `scale`.

### Motions

Every motion has a clock: seconds of running, which only moves while the part is moving (so it pauses with the
machine).

| `type` | Fields | Does |
|---|---|---|
| `spin` | `axis`, `deg_per_sec` | turns steadily |
| `oscillate` | `mode` (`rotate` or `move`), `axis`, `amplitude` (degrees or blocks), `period_seconds`, `phase` (0 to 1) | swings or slides to and fro, a sine |
| `keyframes` | `mode`, `axis`, `keys` (`[[seconds, value], ...]`), `loop_seconds` (0: play once and hold), `ease` (`smooth` default, or `linear`) | follows the keys |

`axis` is `"right"`, `"up"`, `"back"` (or `"left"`, `"down"`, `"front"`, or any of them with a `-` in front), or a
vector `[x, y, z]` in machine space.

### Rendering

- Each part is baked once, the first time it is seen, into GPU buffers (one per block layer: solid, cutout mipped,
  cutout, translucent) on the block atlas, shared by every machine using the animation. Resource reload (F3+T) frees
  them all and they are baked again when next drawn.
- Moving parts cannot use the game's per-block lighting: each part takes one light value, sampled at its pivot.
- Parts are drawn only: they have no collision, no outline and cannot be clicked. Hatches, buses and anything else
  with a block entity must stay real, still blocks.
- Translucent parts are not sorted against each other (they are baked once).
- Each part is culled when its bounding sphere is out of view. Beyond `animationDistance` (client config, default 256
  blocks) a machine is not drawn; beyond `animationFreezeDistance` (default 160) its parts are drawn but hold still.

### Hiding the real blocks

Placed blocks cannot move, so the space a moving part occupies must not hold visible blocks. For now that is done in
the machine's pattern: the moving part's space is required to be **air** (option A of the design), so nothing is there
until the machine is formed and draws the part. Making the mod's own casing blocks invisible while formed (a blockstate)
is planned for when its own multiblocks are reworked.

## The mod's own multiblocks

Every multiblock of the mod moves while it runs. Two fields in `assets/theoreticalsciences/multiblocks/<id>.json` do it
(both written by `tools/animate_multiblocks.py`, which `tools/regenerate.sh` runs):

### `moving`: the structure's own blocks, moving

```json
"moving": [
  { "id": "toroidal_field_coil", "chars": "к", "split": "layers", "pivot": "centre", "layers": [1, 2, 3],
    "alternate": true, "ramp_seconds": 4, "motion": [{ "type": "spin", "axis": "up", "deg_per_sec": 18 }] }
]
```

| Field | Meaning |
|---|---|
| `chars` | the key characters whose blocks move |
| `split` | `none` (one part), `layers` (each layer of them its own part) or `components` (each connected piece its own part) |
| `pivot` | `centre` (the structure's effect centre), `self` (each part's own middle) or `[x, y, z]` in pattern coordinates |
| `layers` | only these pattern layers (of the smallest build; a taller build's repeated layers follow the layer they copy) |
| `alternate` | each further part turns (or swings) the other way |
| `stagger` | each further part runs this much later in its swing (0 to 1 of a period) |
| `parent` | another spec's `id`: carried by its first part's movement |
| `motion`, `ramp_seconds` | as in an animation file |

How it works, and why nothing has to be rebuilt:

- When the structure forms, the server marks the blocks of its moving parts with the casing's `moving` blockstate.
  The block is still there: it collides, it drops, it counts for the structure. It only draws nothing, lets light
  through and stops hiding the faces of the blocks beside it.
- The client reads those blocks from the world (so a tier slot shows the tier actually built), bakes them for that
  machine, and draws them moving while the machine runs, still while it is idle. When the structure breaks the blocks
  are unmarked and show as ordinary blocks again.
- Only casing blocks can be hidden like this; any other block in a moving slot (glass, a block from another mod)
  simply stays still.
- A machine already built and formed in a world picks this up on its first structure check after loading.
- The common config `movingMultiblockParts` (on) turns the hiding off: then every block stays as an ordinary still
  block. The client config `animatedMultiblocks` turns the drawing off.

### `animation`: an animation file

`"animation": "<namespace>:<path>"` draws an animation file (see above) while the structure is formed, moving while the
machine is active.

### `fx`: working effects from data

A multiblock can ask for effects drawn while it runs. Positions (`points`) are pattern coordinates, `chars` key
characters; colours are `"#RRGGBB"`.

| `type` | Fields | Draws |
|---|---|---|
| `glow` | `chars`, `color`, `hot` | those blocks heating up while it runs (about ten seconds to full heat), from `color` to `hot`, cooling slowly after |
| `inflow` | `radius`, `color`, `waves`, `period` | rings of energy closing in on the centre, sparks running in along twelve spokes |
| `inject` | `points`, `color`, `width` | beams from each point into the centre, pulses along them, a flare where they meet |
| `universe` | `radius`, `color` | starry space in a sphere round the centre, three turning spiral galaxies, a young star |
| `swirl` | `radius` | the running recipe's item ingredients swirling round the centre |
| `bubbles` | `points`, `color`, `rise`, `size`, `period` | bubble universes swelling at the points, breaking free, drifting up |
| `drips` | `points`, `color`, `floor` (or `fall`), `period` | drops gathering at the points and falling to `floor` (a pattern y) |
| `echo` | `scale`, `sky` | the Transcendental's rekindling, traced over and over `sky` blocks above the centre |
| `star` | `radius`, `color`, `at` | a star: a burning surface and a corona |
| `shell` | `radius`, `color`, `at` | a sphere of starry space, seen from inside or out |
| `orbiters` | `count`, `inner`, `outer`, `blocks`, `at` | up to 16 blocks orbiting between two radii |
| `beam` | `start_back`, `end_back`, `up`, `width`, `color` | a beam along the machine's depth, scrolling |

`at`, `up` and the backs are pattern coordinates (as the layers are written); without `at`, the machine's effect centre.

The late and endgame machines each have an animation of their own (`EndgameFx`), one type per machine:
`annihilation`, `causality`, `dyson`, `matrioshka`, `vacuum`, `breach`, `concept`, `eye`, `plasma_forge`, `big_bang`,
`inflation`, `mirror`, `entropy`, `distillery`, `abiogenesis`, `ward`, `resolution`, `compiler`. Their fields are
written by tools/round96_endgame.py with the structure they are built into. A machine with one of these does not also
draw its shared effect.

### Larger multiblocks (tools/round95_mega.py)

1.2.0 rebuilt every multiblock larger: the old shape scaled up (x3 for those at most 9 across, x2 for the rest), sheets
of blocks kept one block thick, with the effect centre and scale moved to match. The late and endgame machines are
instead built new, each to its own design (tools/round96_endgame.py), with block counts at least the old ones. A machine's old layout is appended to its `legacy` list.
`scaled_95` marks a layout already scaled, so running the tool again leaves it alone.

### The rules the tool follows

- **Rings**: only the machines built round a turning ring (the warp core's coil rings, the accretion disc's confinement
  ring) turn them, about the structure's vertical axis, each layer the other way from the one below. Only a layer that is truly
  round (the same after a quarter turn) and open enough to be seen turning (mostly air or glass beside it, not casing)
  is set turning, so nothing grinds through the casing.
- **Rotors, pumps, compressors, fans**: each small piece spins in place. Coils, lens arrays, field emitters and
  stabilisers hold still.
- **Rams, rods, drives, nozzles, beds**: slide to and fro (along their walls, never into them).
- **Mounts, dishes, telescopes, heliostats, valves**: slew to and fro.
- **By hand**: the Radio Telescope's dish slews and tilts on its mount; the Reflecting Telescope's dome turns and its
  tube (inside it) tilts.

## Trying it out

The client command `/tsanim` shows animations and effects where you stand, without building anything:

- `/tsanim list`: every animation loaded
- `/tsanim preview <id>`: an animation, as if a controller stood at your feet facing you (so "back" runs the way you look)
- `/tsanim run`, `/tsanim stop`: start and stop the previews, to watch the ramps
- `/tsanim clear`: remove the previews
- `/tsanim stats`: what the animated multiblocks cost: machines drawn, parts, quads, and milliseconds a frame

One example ships with the mod: `theoreticalsciences:examples/test_gimbal` (a block arm carrying a rocking ring carrying
a sliding block: a parent, a child and a grandchild).

The client config `animatedMultiblocks` turns all of it off.

## Performance

Each moving part is baked once into buffers on the graphics card and drawn from them where its controller's renderer
says (so it moves with anything that moves the controller's pose), culled on its own by distance (`animationDistance`)
and by view. One machine's parts are baked a frame, so a base full of them loads without a hitch. With Sable installed,
or the client config `movingPartsCompatibility` on, they are drawn through the game's own block buffers instead.

The JEI / EMI structure viewer does not use these buffers. A structure with 1,500 or more fixed blocks is drawn by
`client/preview/PreviewRenderer`: it is split into 16 × 16 × 16 sections, meshed on background threads and uploaded a
few sections a frame (at most 3 ms and 8 MB each frame), and sections outside the view are skipped. It always draws
from the graphics card, whatever `movingPartsCompatibility` is set to.
