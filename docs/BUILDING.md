# Building Theoretical Sciences

## The jar

Requirements: **JDK 21** and an internet connection (for Gradle, NeoForge and the Maven dependencies).

1. Put the three compile-only jars listed in [`libs/README.md`](../libs/README.md) into `libs/`.
2. Run:

   ```
   ./gradlew build        (Windows: gradlew.bat build)
   ```

3. The jar is in `build/libs/theoreticalsciences-<version>.jar`.

`./gradlew runClient` starts a development client with the mod loaded.

The version number is `mod_version` in `gradle.properties`.

## Generated files

Much of `src/` is **generated** by the Python scripts in `tools/`, and the generated files are committed. You only
need the generators if you change content.

| Generated | From |
|---|---|
| `src/main/java/com/theoreticalsciences/registry/QHContentList.java` | `tools/content.json` (via `gen_java.py`) |
| models, blockstates, recipes, loot tables, tags, advancements, research, the Codex, the language file | `tools/gen_data.py` and the `tools/round*.py` modules it imports |
| item, block and entity textures | `tools/gen_textures.py`, `gen_entities.py`, `ore_textures.py` |
| sounds and the soundtrack | `tools/gen_sounds.py`, `gen_music.py` |

Requirements: Python 3.10 or newer with `pillow`, `numpy` and `scipy` (`pip install pillow numpy scipy`).
The generators also read vanilla Minecraft's own textures and models: set the environment variable
`TS_MC_CLIENT_JAR` to a Minecraft 1.21.1 client jar, e.g. `~/.minecraft/versions/1.21.1/1.21.1.jar`
(see `tools/mcpaths.py`).

To regenerate everything except textures, and check the result:

```
tools/regenerate.sh
```

It finishes with four checks, and all four must pass before a release:

* **progression** — every item, fluid and research can be reached in survival (also after the Shutdown Sequence);
* **validate** — every model, texture, recipe and lang reference resolves (`ALL OK`);
* **controllers** — every multiblock definition is consistent.

Textures are drawn by `gen_textures.py`, which takes a long time for the whole mod. To redraw only some items, use
`python3 tools/gen_textures_partial.py <item id> [<item id> ...]`.

## Where things live

| Folder | What |
|---|---|
| `src/main/java/com/theoreticalsciences/` | the mod: `machine/` (machines, tuning, physics), `research/`, `transcend/` (the Transcendental and the Omnipotence Core), `endgame/`, `world/` (dimensions), `client/` (screens, renderers, the Codex), `compat/` (JEI, EMI, Jade, KubeJS, AE2), `custom/` (the KubeJS / datapack folders) |
| `src/main/resources/assets/theoreticalsciences/multiblocks/` | structure definitions |
| `src/main/resources/data/theoreticalsciences/ts_physics/` | the multiblocks' physics models |
| `src/main/resources/data/theoreticalsciences/ts_research/` | the research tree |
| `docs/KUBEJS.md` | customisation guide for modpack developers |
