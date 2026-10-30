# Porting Theoretical Sciences to Minecraft 1.12.2 — what would have to be reworked

Status: **not started.** A port will be decided on later, depending on how popular the mod is.

This file records what a port involves, so the decision and the eventual work start from the facts. It was written
against version 1.0.0 (NeoForge 1.21.1, about 53,600 lines of Java in 360 files).

## Target

* **Minecraft 1.12.2 with Forge 14.23.5.2860**, not 1.12.1. Practically every 1.12 mod and modpack is on 1.12.2.
* Also test on **Cleanroom**: a maintained 1.12.2 loader on modern Java that aims to keep Forge mods working. Many
  current 1.12.2 packs use it.
* Build with **RetroFuturaGradle** (e.g. the CleanroomMC TemplateDevEnv template), which builds 1.12.2 mods with a
  Java 21 toolchain. Mod code still has to run on Java 8 for plain Forge.

## Summary

It is a **rewrite against a different game**, sharing the design, the art, the sounds and the content data. Nearly
every Java file touches an API that does not exist in 1.12 or works very differently. The content itself (textures,
sounds, music and the recipe, research and physics data in `src/main/resources`) is the largest reusable part.

## What has to be reworked

### Scripting and pack customisation (the biggest design change)

* **KubeJS → CraftTweaker** (ZenScript), and ideally **GroovyScript** as well. In 1.12 packs these are the standard;
  KubeJS's 1.12 version is long abandoned.
* That means a new integration layer, written from scratch:
  * a CraftTweaker `@ZenRegister` / `@ZenClass` handler for every recipe type (82 machine types, Extreme Table,
    Compactor, Convergence, Forge), with add and remove actions applied at the right load stage;
  * ZenScript-accessible events (recipe started/finished, research unlocked, multiblock formed);
  * the research and insight helpers.
* **Datapacks don't exist in 1.12.** Everything that is a data folder today has to be loaded another way, for
  example JSON files in `config/theoreticalsciences/`, plus CraftTweaker/GroovyScript methods for the same data:
  * machine recipes (`data/.../recipe/`, 883 files) — 1.12 has JSON crafting recipes, but not for custom machine
    types: those need the mod's own loader;
  * `ts_research`, `ts_physics`, `ts_glyphs`, `ts_singularities`, `ts_component_tiers`, `ts_forge`, `ts_machines`;
  * tags → the **Ore Dictionary** (`hidden_from_viewers`, `beyond_omnipotence`, `excavator_*`, `not_accelerated`,
    `observable`, `not_spawnable`, `creative_duplicator_parts`; for entities and blocks, config lists).
* `/reload` doesn't exist: Crafttweaker contains a reload command, but does not update recipe changes, textures or multiblock changes without a restart.

### Engine APIs (in roughly every file)

| Today (1.21.1) | 1.12.2 | Files affected (approx.) |
|---|---|---|
| Data components on items (`DataComponents`, `CustomData`) | Item NBT | 19 |
| Codecs / `MapCodec` / `StreamCodec` | Hand-written NBT and `ByteBuf` read/write | 76 / 36 |
| `CustomPacketPayload` networking | `SimpleNetworkWrapper` + `IMessage` | 36 |
| Java records, `switch` expressions, `var`, text blocks | Java 8 classes (unless Cleanroom-only on modern Java) | 77+ |
| `BlockEntity` and its tickers | `TileEntity` + `ITickable` | 99 |
| Block states without metadata (the "flattening") | 16 metadata values per block ID; variants as properties or separate blocks | all blocks |
| `ResourceKey<Level>` dimensions | Integer dimension IDs and `DimensionType` registration | 28 |
| Capabilities (FE, items, fluids) | Forge capabilities exist in 1.12 — closest match, mostly a rename | machines, ports |
| `FluidStack` / NeoForge fluids | Forge `FluidRegistry`, `UniversalBucket` | fluids, tanks, cells |
| Registries (`DeferredRegister`) | `RegistryEvent.Register` | registry/ |

### World generation and dimensions

* The ten dimensions are JSON data today (dimension types, noise settings, biomes, features). In 1.12 each needs a
  hand-written `WorldProvider`, `IChunkGenerator` and biome class.
* Ores and structures: `IWorldGenerator` instead of placed and configured features. The structure zones (spatial
  distortion) carry over in logic only.
* The Transcendental's built structure, the Living Proof's arena and the dimension gate are mostly logic — portable.

### Rendering and screens (`client/`, about a third of the code)

* `GuiGraphics` screens (40 files: the machine consoles, the Omnipotence terminal, the Codex, research, puzzles,
  attunement) → `GuiScreen` / `GuiContainer` with direct GL and `Tessellator` calls. The layouts and logic port; every
  draw call is rewritten.
* `RenderType` / `VertexConsumer` / shaders (24 files: gravitational lensing, structure distortion, procedural mob
  models, block entity renderers) → fixed-function GL, `BufferBuilder` and custom GLSL through Forge. The lensing and
  distortion effects are the hardest single pieces.
* Item and block models: the JSON model format is similar but older (no data-driven item model overrides of the same
  kind). The generated models need a 1.12 output mode.
* Animated textures work the same way (`.mcmeta`).

### Integrations

| Today | 1.12.2 |
|---|---|
| JEI | JEI exists for 1.12.2 — different, older API. All categories and the 3D multiblock preview are rewritten. |
| EMI | Doesn't exist. |
| KubeJS | → CraftTweaker / GroovyScript (above). |
| AE2 | → AE2 Unofficial Extended Life (AE2UEL), the maintained 1.12 fork. Different API. |
| Patchouli | Exists for 1.12.2. Not needed: the mod has its own Codex (1.0.0 no longer ships a Patchouli book). |
| Modonomicon | Doesn't exist, and 1.0.0 no longer uses it. |

### Content (`src/main/resources`)

* **Reusable as is:** item, block and entity textures, sounds and the music.
* **Needs converting to 1.12 formats:**
  * language files — 1.12 uses `.lang` (`key=value`), not JSON;
  * models, blockstates and loot tables; recipes in the new loader's format;
  * the Ore Dictionary instead of tags;
  * the Java content list (`registry/QHContentList.java`).
* Research, physics models and multiblock definitions are the mod's own JSON formats. They can be read by a 1.12
  port unchanged, as long as it ships its own loader for them.

## Suggested way to do it, if it happens

1. A separate repository or branch ("Theoretical Sciences Legacy").
2. Consider a reduced first release: the machines, research and the early and middle dimensions. Then the endgame.
3. Convert the content to 1.12 formats first. Then registries, items, blocks and fluids. Then machines,
   networking and screens. Then JEI and CraftTweaker. Then dimensions, the endgame and rendering effects.
4. Decide CraftTweaker versus GroovyScript first-class support early: it shapes how every data folder is loaded.

## References

* CleanroomMC / Cleanroom — https://github.com/CleanroomMC/Cleanroom
* CleanroomMC TemplateDevEnv (RetroFuturaGradle) — https://github.com/CleanroomMC/Contemplate
* GroovyScript — https://cleanroommc.com/groovy-script/
