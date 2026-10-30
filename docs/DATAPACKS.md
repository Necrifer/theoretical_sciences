# Customising Theoretical Sciences with datapacks

Theoretical Sciences is customisable through datapacks. A plain datapack can add, replace or
remove machine recipes, the research tree, structure parts, machine numbers, the Omnipotence Core's Forge recipes,
singularities and the tags the mod reads. No other mod is needed.

With no datapack, the mod behaves exactly as shipped. (Using KubeJS instead? The same folders and more are covered in
[KUBEJS.md](KUBEJS.md).)

---

## Contents

1. [Making a datapack](#1-making-a-datapack)
2. [Machine recipes](#2-machine-recipes)
3. [Extreme Assembly Table recipes](#3-extreme-assembly-table-recipes)
4. [Singularity Compactor recipes](#4-singularity-compactor-recipes)
5. [Convergence Altar recipes](#5-convergence-altar-recipes)
6. [Changing mod's recipes](#6-changing-mods-recipes)
7. [Machine properties (`ts_machines`)](#7-machine-properties-ts_machines)
8. [Coil and emitter tiers (`ts_component_tiers`)](#8-coil-and-emitter-tiers-ts_component_tiers)
9. [Machine physics (`ts_physics`)](#9-machine-physics-ts_physics)
10. [Omnipotence Core Forge recipes (`ts_forge`)](#10-omnipotence-core-forge-recipes-ts_forge)
11. [Research (`ts_research`)](#11-research-ts_research)
12. [Singularities (`ts_singularities`)](#12-singularities-ts_singularities)
13. [Tags](#13-tags)
14. [Names, tooltips and the Codex (resource packs)](#14-names-tooltips-and-the-codex-resource-packs)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Making a datapack

A datapack is a folder (or a zip of it) with a `pack.mcmeta` file and a `data` folder:

```
mypack/
├── pack.mcmeta
└── data/
    └── mypack/                       <- your namespace: any lower-case name
        ├── recipe/
        │   └── entangling/
        │       └── my_qubits.json
        ├── ts_machines/
        │   └── quark_gluon_collider.json
        └── ts_research/
            └── advanced_containment.json
```

`pack.mcmeta` for Minecraft 1.21.1:

```json
{ "pack": { "pack_format": 48, "description": "My Theoretical Sciences changes" } }
```

Where to put it:

* **One world:** `<world folder>/datapacks/mypack/`, then `/reload` (or reopen the world).
* **A modpack, every world:** use a mod that loads global datapacks (for example *Paxi* or *Global Packs*). KubeJS's
  `kubejs/data/` folder also works if KubeJS is installed.

Rules for every folder below:

* `/reload` applies changes, and they are sent to every player. JEI/EMI, the machine screens and the Codex update too.
* A broken file is skipped with a message in `logs/latest.log` (search for `Theoretical Sciences` or the file name). It
  never stops the world from loading.
* To **replace** one of the mod's own files, put yours at the same path in the `theoreticalsciences` namespace:
  `data/theoreticalsciences/<folder>/<same name>.json`. Anything else goes in your own namespace.

---

## 2. Machine recipes

Every machine uses one recipe format. The file goes in `data/<namespace>/recipe/<anything>.json`. Its `type` names
the machine's recipe type; the type is also the name of the machine's folder in the mod jar
(`data/theoreticalsciences/recipe/<type>/`). There are 82 types: `entangling`, `condensing`, `collider`,
`plasma_smelting`, `kugelblitz`, `vacuum_harvesting`, as examples.

```json
{
  "type": "theoreticalsciences:entangling",
  "item_inputs": [
    { "item": "minecraft:amethyst_shard", "count": 4 },
    { "tag": "c:gems/diamond", "count": 1 }
  ],
  "item_outputs": [
    { "id": "theoreticalsciences:qubit", "count": 2 }
  ],
  "energy": 512,
  "duration": 200,
  "research": "theoreticalsciences:quantum_entanglement"
}
```

A machine that takes fluids:

```json
{
  "type": "theoreticalsciences:condensing",
  "item_inputs":   [ { "item": "minecraft:snowball", "count": 1 } ],
  "fluid_inputs":  [ { "fluid": "minecraft:water", "amount": 1000 } ],
  "item_outputs":  [ { "id": "theoreticalsciences:quantum_dot", "count": 1 } ],
  "fluid_outputs": [ { "id": "theoreticalsciences:superfluid_helium", "amount": 500 } ],
  "energy": 2048,
  "duration": 300
}
```

All fields:

| Field | Default | Meaning |
|---|---|---|
| `item_inputs` | `[]` | `{ "item": id, "count": n }` or `{ "tag": tag, "count": n }`. |
| `fluid_inputs` | `[]` | `{ "fluid": id, "amount": mB }` or `{ "tag": fluid tag, "amount": mB }`. |
| `item_outputs` | `[]` | `{ "id": id, "count": n }`. |
| `fluid_outputs` | `[]` | `{ "id": fluid id, "amount": mB }`. |
| `energy` | 0 | FE per tick (up to 2,147,483,647). For the generators (`zero_point`, `penrose`, `dyson`, `annihilation`, `thermoelectric`) it is FE per tick **produced**. |
| `duration` | 200 | duration of recipe in Ticks (20 = one second). |
| `research` | none | Recipe pre-requisite research. |
| `catalysts` | `[]` | Non-consumable input item: `[{ "item": id }]`. |
| `chances` | all 1 | Chance for each item output, in order, accepts value of 0 to 1: `[1.0, 0.25]`. |
| `min_tier` | 0 | Lowest coil tier the multiblock must have (section 8). |
| `optional_inputs` | `[]` | Only used if other mods are installed: `[{ "item": "mekanism:alloy_atomic", "count": 4 }]`. |
| `optional_fluid_inputs` | `[]` | The same for fluids: `[{ "fluid": "mekanism:hydrogen", "amount": 1000 }]`. |
| `environment` | anything | Conditions around the controller (below). |
| `physics` | none | Physics windows the operator must hold (below). |
| `insight_per_run` | 0 | Research Insight given to the machine's owner per run. |

Each machine has a fixed number of item and fluid slots. A recipe with more inputs or outputs than the machine has
won't fit; compare with the mod's own recipes for that machine.

**`environment`** — the physical conditions where the machine stands. A dimension sets them, and a Field Projector
changes one of them within 16 blocks. Every bound is optional:

```json
"environment": {
  "gravity_min": 100,      "gravity_max": 0.1,
  "dimensions_min": 5,
  "time_min": 10,          "time_max": 0.1,
  "vacuum_min": 2,
  "dimensions": ["theoreticalsciences:the_bulk", "minecraft:the_end"],
  "biomes": ["minecraft:deep_dark", "#c:is_cold"]
}
```

* Gravity is in gravitation units, g (Earth = 1).
* `dimensions` is the number of spatial dimensions.
* Time is how fast time runs, relative to normal.
* Vacuum: 0 ordinary, 1 Casimir-depleted, 2 false vacuum, 3 true vacuum.
* `dimensions`: the recipe runs only in one of these dimensions (by id).
* `biomes`: only in one of these biomes at the controller's position (ids, or `#tags`).

Dimension checks use the **dimension id** (`theoreticalsciences:quantum_foam`), never the biome, so mods that change biomes
don't affect them. Use `biomes` when a recipe should depend on the biome. To change which dimensions a whole machine
works in, see `dimensions` in section 7.

**`physics`** — for multiblocks with a physics console. Each window names a quantity of that machine's model
(section 9) and the range it must be held in while the recipe runs. `vars` sets per-recipe values the model reads:

```json
"physics": {
  "vars": { "A": 1, "Z": 1 },
  "windows": [ { "q": "sNN", "min": 3600, "max": 4200 } ]
}
```

The easiest way to write either field is to copy it from one of the mod's recipes for the same machine.

---

## 3. Extreme Assembly Table recipes

Shaped, any grid up to 9×9:

```json
{
  "type": "theoreticalsciences:extreme_shaped",
  "pattern": [
    "AAA",
    "ABA",
    "AAA"
  ],
  "key": {
    "A": { "item": "minecraft:glass" },
    "B": { "item": "minecraft:nether_star" }
  },
  "result": { "id": "minecraft:beacon", "count": 1 }
}
```

Shapeless, up to 81 ingredients:

```json
{
  "type": "theoreticalsciences:extreme_shapeless",
  "ingredients": [
    { "item": "minecraft:phantom_membrane" },
    { "item": "minecraft:netherite_ingot" }
  ],
  "result": { "id": "minecraft:elytra", "count": 1 }
}
```

The table also accepts vanilla crafting-table recipe.

---

## 4. Singularity Compactor recipes

```json
{
  "type": "theoreticalsciences:compaction",
  "ingredient": { "item": "minecraft:coal_block" },
  "count": 1000,
  "result": { "id": "minecraft:diamond_block", "count": 1 }
}
```

`count` is how many of the ingredients required. The Compactor has no timer: it finishes as fast as it is fed. (For new
singularities, see section 12 instead.)

---

## 5. Convergence Altar recipes

```json
{
  "type": "theoreticalsciences:convergence",
  "core": { "item": "minecraft:emerald_block" },
  "pedestals": [
    { "item": "minecraft:gold_block" },
    { "item": "minecraft:gold_block" }
  ],
  "result": { "id": "minecraft:totem_of_undying", "count": 1 },
  "energy": 1000000,
  "duration": 200,
  "tier": 1,
  "research": "theoreticalsciences:synthetic_biology"
}
```

* `core` sits on the altar; `pedestals` are the items around it.
* `energy` here is the **total** FE for the whole ritual, not FE per tick.
* `duration` here is the time taken for the recipe to be processed, in ticks.
* `tier` is the lowest grade of pedestal needed: 1 Resonance, 2 Harmonic, 3 Absolute.
* `research` is the pre-requisite research for this recipe, optional.

---

## 6. Changing mod's recipes

Every recipe of the mod has an id: its path in the jar. `data/theoreticalsciences/recipe/collider/superpartner_shard.json`
is `theoreticalsciences:collider/superpartner_shard`. JEI and EMI show a recipe's id when advanced tooltips are on (F3+H).

* **Replace:** put your own file at the same path in the `theoreticalsciences` namespace,
  `data/theoreticalsciences/recipe/collider/superpartner_shard.json`. That will take priority over the mod's usual settings.
* **Remove:** put a file at that same path that switches itself off with NeoForge's condition:

  ```json
  { "neoforge:conditions": [ { "type": "neoforge:false" } ] }
  ```

  This works for every kind of recipe in the mod. For example, to remove an Extreme Assembly Table recipe, a Compactor
  recipe and a Convergence recipe, create these three files, each containing only the condition above:

  ```
  data/theoreticalsciences/recipe/extreme/cosmic_record_fragment.json
  data/theoreticalsciences/recipe/compaction/amethyst_singularity.json
  data/theoreticalsciences/recipe/convergence/breached_anomaly_engine.json
  ```

* **Change** one of them: write your full version at the same path. For example,
  `data/theoreticalsciences/recipe/compaction/amethyst_singularity.json`:

  ```json
  {
    "type": "theoreticalsciences:compaction",
    "ingredient": { "item": "minecraft:amethyst_block" },
    "count": 4096,
    "result": { "id": "theoreticalsciences:amethyst_singularity", "count": 1 }
  }
  ```

  Singularities from section 12 aren't recipe files: switch those off there.
  The below is only for Singularities added due to other mods.
* **Only when another mod is present:** add a condition to your own recipe:

  ```json
  "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "mekanism" } ]
  ```

---

## 7. Machine properties (`ts_machines`)

One file per machine: `data/<namespace>/ts_machines/<machine id>.json`. The machine id is its block id without
`theoreticalsciences:` (`quark_gluon_collider`, `quantum_entangler`, `stellar_plasma_crucible`, ...). Every field is
optional.

```json
{
  "energy_capacity": 500000000,
  "speed_multiplier": 2.0,
  "energy_multiplier": 1.5,
  "max_parallel": 8,
  "max_overclock": 4
}
```

| Field | Machines | Meaning (the mod's own value in brackets) |
|---|---|---|
| `energy_capacity` | all | The amount of FE the buffer holds. It still grows by itself if a recipe needs more per tick. |
| `max_energy_input` | consumers | FE/t accepted from cables (no limit). |
| `max_energy_output` | generators | FE/t pushed into cables (the machine's own rate). |
| `speed_multiplier` | all | Recipes take `duration / this` ticks (1). |
| `energy_multiplier` | all | Recipes draw `this ×` FE/t; generators produce `this ×` (1). |
| `max_parallel` | tuned multiblocks | Parallel recipes before the emitter tier multiplies it (1–8). Max 4096. |
| `max_overclock` | tuned multiblocks | Overclock levels; each is 2× speed for 4× power (0–3). Max 16. |
| `coil_speed_bonus` | multiblocks with coils | Extra speed per coil tier above 1 (0.25). |
| `integrity_wear_multiplier` | tuned multiblocks | How fast containment integrity wears (1). 0 = never. |
| `drift_multiplier` | tuned multiblocks | How strongly the ideal setting / physics disturbance drifts (1). 0 = never. |
| `dimensions` | all | The dimensions the machine works in, by id. `[]` = anywhere. (The Vacuum Fluctuation Harvester: `["theoreticalsciences:quantum_foam"]`; the Omnilex Engine: `["theoreticalsciences:hyperdimensional_reality"]`; every other machine: anywhere.) |

**Where a machine works:** `dimensions` applies to every recipe of the machine; a recipe's own `environment` can
narrow it further. Built anywhere else, the machine stands idle and says *Only works in …*, and its tooltip and
JEI/EMI page say so too. For example, `data/mypack/ts_machines/vacuum_harvester.json`:

```json
{ "dimensions": ["theoreticalsciences:quantum_foam", "theoreticalsciences:the_bulk"] }
```

lets the Harvester work in the Bulk as well, and `{ "dimensions": [] }` in `omnilex_engine.json` lets the Omnilex
Engine work anywhere. The Transcendental and the Living Proof can still only be entered the intended way, this is 
hardcoded behaviour.

Machines in the world pick up the new numbers within a tick of `/reload`. Lowering `energy_capacity` never deletes
stored energy, and lowering `max_parallel` or `max_overclock` lowers machines already set higher.

**Limits and extreme values.** Every number is clamped, so no combination of settings can crash the game:

| What | Limit |
|---|---|
| `speed_multiplier` | 0.0001 – 10,000 |
| `energy_multiplier` | 0 – 10,000 |
| `max_parallel` | 1 – 4,096. The emitter tier multiplies it, but a structure never runs more than **4,096 recipes at once**. |
| `max_overclock` | 0 – 16 (up to 65,536× speed for 4,294,967,296× power) |
| Fastest possible recipe | One finished cycle (× parallel) per tick. Extra speed beyond that is wasted, not an error. |
| Recipe `energy` | up to 2,147,483,647 FE/t. With overclock, parallel and multipliers the real draw may go higher; the buffer grows to match (up to about 4.6 × 10¹⁸ FE). A machine that can't be fed shows *No Power*. |
| Energy I/O | up to 2,147,483,647 FE per side per tick — the same as every FE mod |
| Item input slots | up to 2,147,483,647 items each |
| Item output | at most 99 per output entry in a recipe file (Minecraft's own limit); output slots stack to 64 |
| Fluid tanks | up to 2,147,483,647 mB each, inputs and outputs |

A recipe asking for more than a slot or tank can hold will never start.

---

## 8. Coil and emitter tiers (`ts_component_tiers`)

Many multiblocks have positions that take a tiered part, a **coil** or a **field emitter**. The mod's coils are tiers
1–5 and its emitters 1–3. A structure counts as the **lowest** tier among its parts of that family.

* **Coil tier:** recipes can require a minimum (`min_tier`). Each tier above 1 gives +25% speed. Machines with physics
  use the coil's superconductor limits.
* **Emitter tier:** multiplies the parallel recipes a tuned multiblock may run.

`data/<namespace>/ts_component_tiers/<name>.json` makes any block a part of a family:

```json
{
  "family": "coil",
  "tier": 6,
  "block": "othermod:dark_matter_coil",
  "critical_temperature": 16000,
  "critical_field": 20000,
  "speed_bonus": 1.5
}
```

| Field | Needed? | Meaning |
|---|---|---|
| `family` | yes | `"coil"` or `"emitter"`. |
| `tier` | yes | The tier it counts as: a new one (6, 7, ...) or an existing one. |
| `block` / `blocks` / `tag` | one of them | One block id, a list of ids, or a block tag (no `#`). |
| `critical_temperature` | no (required for coils) | Kelvin, for the physics models. |
| `critical_field` | no (required for coils) | Tesla, for the physics models. |
| `speed_bonus` | no | Speed at this tier = 1 + this. Default: +25% per tier above 1. |

Every position of that family, in every multiblock, then accepts the block at that tier; structure shapes are never
edited. The block itself must already exist: added by another mod, or by KubeJS (see [KUBEJS.md](KUBEJS.md)).

---

## 9. Machine physics (`ts_physics`)

The multiblocks' physics models are `data/theoreticalsciences/ts_physics/<machine id>.json`, plus `coolants.json`. To change
one, copy the whole file out of the mod jar (it is a zip) into your datapack at the **same path in the
`theoreticalsciences` namespace**, and edit the copy.

* `params` — the console's dials: `min`, `max`, `step`, `default`. Widening a range is the usual edit.
* `consts`, `derived`, `states` — named quantities and their equations (`expr`). `formula` is only display text.
* `constraints` — `expr` must stay above 0. Otherwise, by `kind`: `stop` (won't run), `wear` (wears 4× faster),
  `quench` (sudden failure) or `damage` (loses integrity every second).
* `rate` — speed (1 = normal), capped by `rate_max`. `heat` / `idle_heat` — heat into the coolant.
* `coolants.json`: `"<fluid id>": { "temperature": K, "capacity": kJ per mB, "returns": "<warm fluid id>" }`.

If you narrow a model so far that a recipe's `physics` window can't be reached, that recipe never runs. **Laplace's
Demon** (a creative item) finds a working setting if one exists.

**Adding and removing physics**

* Every multiblock already has a model (37 of them). To give one new dials, limits or quantities, edit its copy: add
  entries to `params`, `derived` or `constraints`. Recipes can then ask for windows on your new names.
* Single-block machines can't have physics; a model file for one is ignored. New machines need an addon
  ([ADDONS.md](ADDONS.md)).
* Remove a machine's physics with `data/theoreticalsciences/ts_physics/<machine id>.json` containing `{ "remove": true }`.
  The machine then runs without its console, on its plain tuning dial if it has one, and its recipes' `physics`
  windows are ignored.
* Remove a coolant: copy `coolants.json` and delete its entry.

**When a model is wrong** — mistakes never crash the game:

* A file that can't be read is skipped with an error naming it in `logs/latest.log`, and the machine keeps the mod's
  own model. This covers unknown names in an expression, a dial with `min` above `max`, a missing `default`, and
  `kappa`, `heat_capacity` or `timescale` of 0 or less.
* Physics that can't be satisfied is allowed: the machine shows *Unstable* or *Out of window* and doesn't run.
* Laplace's Demon on an impossible model searches for at most 15 seconds, in the background. Then it reports that it
  found no working setting and leaves the machine at the closest point. If the model fails while being searched, it
  says so in chat and the reason goes to the log.
* Values that blow up (division by zero, overflow) count as "the physics breaks down here". If a model fails in some
  unforeseen way while a machine runs, that machine stops and the log names the model.

---

## 10. Omnipotence Core Forge recipes (`ts_forge`)

The reborn Omnipotence Core's Forge crafts from the player's inventory. `data/<namespace>/ts_forge/<name>.json` adds a
recipe with the id `<namespace>:<name>`:

```json
{
  "result": { "item": "minecraft:dragon_egg", "count": 1 },
  "ingredients": [
    { "item": "theoreticalsciences:omnipotent_ingot", "count": 16 },
    { "item": "minecraft:dragon_breath", "count": 64 },
    { "item": "theoreticalsciences:rekindled_spark", "count": 0 },
    { "universe": "minecraft", "count": 1 }
  ]
}
```

* `count: 0` means the item is reusable.
* `{ "universe": "<mod id>" }` is a Simulated Universe of that mod.
* 1 to 32 ingredients; result count 1 to 64.
* A recipe naming a missing item, or a Simulated Universe of a mod that isn't installed, is skipped (the log will mention).
  A universe of any installed mod works, vanilla `minecraft` included: the Forge adds that universe's own recipe
  automatically.

**Replace** one of the mod's: `data/theoreticalsciences/ts_forge/<its name>.json`. The names are `omnipotent_ingot`,
`transcendence_crystal`, `absolute_singularity`, `eschaton_core`, `omnipotent_edge`, `singularity_scepter`,
`crown_of_transcendence`, `mantle_of_omnipotence`, `greaves_of_the_absolute`, `treads_of_infinity`, `ex_nihilo_engine`,
`entropy_censer`, `equilibrium_lattice`, `lawgivers_codex`, `resonant_shards`, `echo_replay`, `second_spark`,
`rekindled_heart`, `echo_lattice`, `omnipotence_seed`, `demiurge_hand`, `dodecaract_excavator`, `eschaton_cell`,
`forged_omnipotence_core` and `euclidean_anchor`. Generated ones are `simulate/<mod id>` and `creative/<item id>`.

**Remove** by id from any file:

```json
{ "remove": ["echo_lattice", "creative/mekanism:creative_bin"] }
```

or remove the recipe with the file's own id: `data/theoreticalsciences/ts_forge/echo_lattice.json` containing
`{ "remove": true }`.

**Which items get a generated recipe.** The Forge finds creative-only items by itself: a built-in list of known ones
(Avaritia's Infinity gear, Create's Worldshaper and others) plus any item whose id contains `creative`. Two item tags
change that list. Mod developers can ship them in their own mod's data the same way:

* `data/theoreticalsciences/tags/item/forge_only.json` adds items: each gets its own `creative/<item id>` recipe,
  and the Core's "make anything" screen refuses it. Use it for creative-only items the detection misses, or for
  late-endgame items you want behind the Forge. The item's other recipes are not removed.
* `data/theoreticalsciences/tags/item/forge_exempt.json` takes items off the list (a normal item that happens to
  have `creative` in its id).

```json
{ "replace": false, "values": ["somemod:ultimate_reactor", { "id": "othermod:void_core", "required": false }] }
```

Use `"required": false` for items of mods that may not be installed: a missing required entry makes Minecraft discard
the whole tag file.

A forged Core can never forge another Omnipotence Core or a Hand of the Demiurge, whatever the recipes say.

---

## 11. Research (`ts_research`)

`data/<namespace>/ts_research/<name>.json` is the research `<namespace>:<name>`. Use the `theoreticalsciences` namespace and
an existing name to replace one of the mod's entries.

```json
{
  "title": "Advanced Containment",
  "description": "Unlocks the pack's own containment machines.",
  "icon": "theoreticalsciences:quarantine_field_matrix",
  "requires": ["theoreticalsciences:containment_protocols"],
  "insight": 400,
  "cost": [ { "item": "theoreticalsciences:anomaly_specimen", "count": 4 } ],
  "time": 600,
  "x": 30,
  "y": 12
}
```

| Field | Default | Meaning |
|---|---|---|
| `title` | (required) | Name of the research. |
| `description` | `""` | Descriptive flavour text. |
| `icon` | `minecraft:book` | Item on the node. |
| `category` | `general` | The Chapter the node is under. |
| `requires` | `[]` | Pre-requisite research. |
| `insight` | 0 | Insight costs. |
| `cost` | `[]` | Items consumed: `{ "item": id, "count": n }` or `{ "tag": tag, "count": n }`. |
| `time` | 0 | Seconds of study before completion (0 = instant). |
| `x`, `y` | 0 | Position on the research map. |
| `hidden` | false | Hidden until every prerequisite is done. |
| `grant_only` | false | Can only be granted though commands |
| `puzzle` | none | A derivation puzzle. Copy the format from one of the mod's research files. |

Gate any recipe with `"research": "<namespace>:<name>"`.

---

## 12. Singularities (`ts_singularities`)

`data/<namespace>/ts_singularities/<name>.json` adds a singularity, made in the Singularity Compactor. The Omniscience
Catalyst then needs one of each.

```json
{
  "tag": "c:ingots/osmium",
  "count": 16384,
  "name": "Osmium Singularity",
  "core": "#30405a",
  "glow": "#a0c8ff",
  "enabled": true
}
```

* Use `item` instead of `tag` for a single item. Colours left out are taken from the item's texture.
* `enabled: false` switches the file off without deleting it.
* Every ingot of every other mod gets a singularity automatically. A file that covers an ingot takes the place of its
  automatic one. Automatic singularities are switched off in `config/theoreticalsciences-common.toml`
  (under `autoSingularities` and `autoSingularityBlacklist`).
* With more singularities than the Extreme Assembly Table can hold, they are condensed nine at a time into
  Singularity Clusters. Name them with `ts_singularities/_clusters.json`:
  `{ "names": { "1.0": "Ferrous Cluster" } }`.

---

## 13. Tags

Add to a tag with a file at `data/theoreticalsciences/tags/<type>/<tag name>.json`. Keep `"replace": false`, so you add to
the mod's list instead of wiping it:

```json
{
  "replace": false,
  "values": [ "othermod:debug_block" ]
}
```

| File | What it does |
|---|---|
| `tags/item/hidden_from_viewers.json` | Left out of JEI's and EMI's item lists. |
| `tags/item/beyond_omnipotence.json` (also `tags/fluid/`) | The Omnipotence Core can't materialize it, and creative duplicators refuse it. |
| `tags/item/forge_only.json` | Only the Omnipotence Core's Forge makes it, with its own generated recipe (see [section 10](#10-omnipotence-core-forge-recipes-ts_forge)). |
| `tags/item/forge_exempt.json` | Taken off the Forge-only list (a normal item with `creative` in its id). |
| `tags/item/creative_duplicator_parts.json` | Parts that turn storage into a duplicator; they're stopped from copying anything beyond Omnipotence. |
| `tags/block/excavator_ores.json` | Extra blocks the Dodecaract Excavator's Ore Extraction takes. |
| `tags/block/excavator_ignored.json` | Blocks the Excavator never breaks. |
| `tags/block/not_accelerated.json` | Blocks the Chronal Accelerator never speeds up. |
| `tags/block/observable.json`, `tags/entity_type/observable.json` | What the Spectral Analyzer can study. |
| `tags/entity_type/not_spawnable.json` | Never summoned by the Lawgiver's Codex. |

---

## 14. Names, tooltips and the Codex (resource packs)

Text and the Codex are client assets, so they belong in a **resource pack**, not a datapack.

* **Names, tooltips, messages:** `assets/theoreticalsciences/lang/en_us.json` with only the keys you change, e.g.
  `{ "item.theoreticalsciences.qubit": "Quantum Bit" }`. An item's flavour line is `<item key>.lore`.
* **The Quantum Codex** reads `assets/theoreticalsciences/ts_codex/`: `index.json`, `entries.json` and one map per tab. A
  file there replaces the mod's file of the same name, so start from the mod's copy.

---

## 15. Troubleshooting

* **Nothing changed:** check the path. Folder names must be exactly as written, directly under `data/<namespace>/`,
  and `recipe` is singular. Check `/datapack list` to see whether the pack is enabled, and `logs/latest.log` for a
  message naming your file.
* **A replacement has no effect:** it needs the `theoreticalsciences` namespace and the mod's exact path.
* **A recipe never runs:** JEI/EMI list its conditions — research, coil tier, environment, physics window, required
  dimension. The machine's screen says which one is missing.
