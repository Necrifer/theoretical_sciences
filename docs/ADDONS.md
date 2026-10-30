# Making an addon for Theoretical Sciences

Addons are welcome. Theoretical Sciences is under the [MIT License](../LICENSE): if your addon includes any of its code
or assets, keep the copyright notice and licence text with them. Beyond that, a link back to Theoretical Sciences's page
and credit to Innight in your description and mod metadata are appreciated. The simplest setup is to ship your addon on
its own and list Theoretical Sciences as a dependency, so players get the mod from its [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/theoretical-sciences).

## Data-only addons

The simplest addon is a mod (or datapack) that only ships data. Everything in [DATAPACKS.md](DATAPACKS.md) works from
inside your own jar, under your own namespace: machine recipes, research entries, coil and emitter tiers for your
blocks, machine numbers, Forge recipes, singularities and tags. No code against Theoretical Sciences is needed.

## Code addons

Depend on the Theoretical Sciences jar at compile time and declare the dependency in your `neoforge.mods.toml`:

```toml
[[dependencies.youraddon]]
    modId = "theoreticalsciences"
    type = "required"
    versionRange = "[1.0.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

and credit it in your mod entry, e.g. `credits="Built on Theoretical Sciences by Innight"`.

### Events

Fired on `NeoForge.EVENT_BUS`, server side, from the package `com.theoreticalsciences.api.event`:

| Event | When | Gives |
|---|---|---|
| `MachineRecipeEvent.Start` (cancellable) | a machine is about to start a recipe | `getMachine()`, `getRecipeId()`, `getRecipe()` |
| `MachineRecipeEvent.Finish` | a machine finished a recipe | the same |
| `ResearchUnlockedEvent` | a player gained a research entry (terminal, command or script) | `getPlayer()`, `getResearch()` |
| `MultiblockFormedEvent` | a multiblock became complete, or broke | `getController()`, `isFormed()` |

```java
NeoForge.EVENT_BUS.addListener((MachineRecipeEvent.Start e) -> {
    if (e.getRecipeId().getPath().startsWith("kugelblitz/") && e.getMachine().getLevel().isRaining()) e.setCanceled(true);
});
```

### Research

`com.theoreticalsciences.research.ResearchService`:

```java
ResearchService.has(serverPlayer, ResourceLocation.parse("theoreticalsciences:quantum_entanglement"));
ResearchService.grant(serverPlayer, id);
ResearchService.revoke(serverPlayer, id);
ResearchService.insight(serverPlayer);
ResearchService.addInsight(serverPlayer, 100);
```

Only the `api` package and the methods named here are meant for addons. Other classes may change between versions.
If your addon needs something that isn't exposed yet, open an issue on the repository.
