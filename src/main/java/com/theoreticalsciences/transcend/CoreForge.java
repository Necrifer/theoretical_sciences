package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The reborn Core's Forge (round 15): crafting with Omnipotence. Ingredients come straight from the player's inventory.
 * The mod's own recipes are fixed here (shared by client and server); the Core's screen lists them.
 * Round 31: datapacks / KubeJS can add, replace and remove recipes (data/&lt;ns&gt;/ts_forge, see
 * {@link com.theoreticalsciences.custom.ForgeRecipes}); {@link #base()} is the merged list, synced to every client.
 */
public final class CoreForge {
    /**
     * One ingredient: an item and how many ({@code 0}: needed but not used up). {@code universe}, if set, means a
     * Simulated Universe of that mod (round 22).
     */
    public record Ing(String item, int count, @javax.annotation.Nullable String universe) {
        public Ing(String item, int count) {
            this(item, count, null);
        }

        public Item get() {
            ResourceLocation r = ResourceLocation.tryParse(item.contains(":") ? item : TheoreticalSciences.MODID + ":" + item);
            return r == null ? Items.AIR : BuiltInRegistries.ITEM.get(r);
        }

        /** What it looks like (with its universe), {@code n} of it. */
        public ItemStack stack(int n) {
            return universe != null ? SimulatedUniverseItem.of(universe, n) : new ItemStack(get(), n);
        }

        public boolean matches(ItemStack s) {
            return s.is(get()) && (universe == null || universe.equals(SimulatedUniverseItem.mod(s)));
        }
    }

    public record Recipe(String id, Ing out, List<Ing> in) {
        public ItemStack result() {
            return out.stack(out.count());
        }
    }

    private static Ing i(String id, int n) {
        return new Ing(id, n);
    }

    /**
     * Round 22: every recipe its own - no two share the same materials in the same amounts. Each piece of the last
     * gear is made of what it is for (the edge of cosmic strings and paradoxical spheres, the boots of warp coils and
     * negative mass, the codex of crystallised laws...).
     * Round 35: the Resolved Anomaly (the end of the Abnormality branch) goes into the Eschaton Core and the last
     * things of all - the Echo Lattice, the Seed, the Hand, the Lawgiver's Codex, the Equilibrium Lattice, the forged
     * Core - and the Euclidean Anchor.
     */
    public static final List<Recipe> RECIPES = List.of(
            // the last materials
            new Recipe("omnipotent_ingot", i("omnipotent_ingot", 2), List.of(i("apeiron_ingot", 32), i("omnipotence_dust", 32), i("pleroma_crystal", 8))),
            new Recipe("transcendence_crystal", i("transcendence_crystal", 1), List.of(i("pleroma_crystal", 32), i("transcendental_crystal", 16), i("logos_thread", 32))),
            new Recipe("absolute_singularity", i("absolute_singularity", 1), List.of(i("omnipotent_ingot", 32), i("micro_singularity", 32), i("absolute_infinity", 8),
                    i("transcendence_crystal", 8))),
            new Recipe("eschaton_core", i("eschaton_core", 1), List.of(i("absolute_singularity", 16), i("transcendence_crystal", 16), i("omnipotent_ingot", 64),
                    i("grand_unified_core", 8), i("resolved_anomaly", 4), i("rekindled_spark", 0))),
            // beyond Omnipotence
            new Recipe("omnipotent_edge", i("omnipotent_edge", 1), List.of(i("eschaton_core", 6), i("omnipotent_ingot", 144), i("cosmic_string_segment", 512),
                    i("banach_tarski_sphere", 64), i("logos_thread", 48))),
            new Recipe("singularity_scepter", i("singularity_scepter", 1), List.of(i("eschaton_core", 10), i("absolute_singularity", 32), i("micro_singularity", 96),
                    i("white_hole_fragment", 256), i("kugelblitz_seed", 64))),
            new Recipe("crown_of_transcendence", i("crown_of_transcendence", 1), List.of(i("eschaton_core", 5), i("transcendence_crystal", 40),
                    i("superintelligence_core", 64), i("omega_point", 16), i("thought_matrix", 128))),
            new Recipe("mantle_of_omnipotence", i("mantle_of_omnipotence", 1), List.of(i("eschaton_core", 9), i("omnipotent_ingot", 160),
                    i("degenerate_armour_plate", 512), i("reality_anchor", 64), i("stress_energy_tensor", 96))),
            new Recipe("greaves_of_the_absolute", i("greaves_of_the_absolute", 1), List.of(i("eschaton_core", 7), i("omnipotent_ingot", 120),
                    i("inertia_damper", 192), i("geodesic_anchor", 8), i("massless_alloy", 256))),
            new Recipe("treads_of_infinity", i("treads_of_infinity", 1), List.of(i("eschaton_core", 4), i("omnipotent_ingot", 88), i("alcubierre_coil", 128),
                    i("negative_mass_ingot", 256), i("closed_timelike_curve", 32))),
            new Recipe("ex_nihilo_engine", i("ex_nihilo_engine", 1), List.of(i("eschaton_core", 16), i("absolute_singularity", 24), i("pleroma_crystal", 192),
                    i("false_vacuum_membrane", 128), i("universe_seed", 256))),
            new Recipe("entropy_censer", i("entropy_censer", 1), List.of(i("eschaton_core", 8), i("transcendence_crystal", 28), i("omnicellular_genome", 64),
                    i("heat_death_remnant", 128), i("maxwell_demon_chip", 96))),
            // round 25: the only perfect stabiliser - a multiblock that never drifts, wears, quenches or faults
            new Recipe("equilibrium_lattice", i("equilibrium_lattice", 1), List.of(i("eschaton_core", 3), i("predictive_lattice_governor", 4),
                    i("omnipotent_ingot", 72), i("transcendence_crystal", 20), i("law_crystal", 256), i("resolved_anomaly", 8))),
            new Recipe("lawgivers_codex", i("lawgivers_codex", 1), List.of(i("eschaton_core", 12), i("omnilex", 1), i("logos_thread", 160), i("law_crystal", 512),
                    i("proof_of_everything", 16), i("resolved_anomaly", 16))),
            // round 27: what the Sundering and the Rekindling left behind is no longer a one-off. The reborn Core can
            // resonate more of it - and it is what the last things are made of, up to a new Omnipotence Core of your own.
            new Recipe("resonant_shards", i("omnipotence_shard", 2), List.of(i("omnipotent_ingot", 16), i("omnipotence_dust", 48), i("transcendence_crystal", 4),
                    i("rekindled_spark", 0))),
            new Recipe("echo_replay", i("transcendental_echo", 1), List.of(i("omnipotence_shard", 12), i("pleroma_crystal", 48), i("logos_thread", 48),
                    i("absolute_singularity", 2), i("rekindled_spark", 0))),
            new Recipe("second_spark", i("rekindled_spark", 1), List.of(i("transcendental_echo", 2), i("omnipotence_shard", 24), i("eschaton_core", 1),
                    i("pleroma_crystal", 96))),
            new Recipe("rekindled_heart", i("rekindled_heart", 1), List.of(i("rekindled_spark", 4), i("transcendental_echo", 2), i("eschaton_core", 2),
                    i("omnipotent_ingot", 64))),
            new Recipe("echo_lattice", i("echo_lattice", 1), List.of(i("transcendental_echo", 4), i("omnipotence_shard", 32), i("transcendence_crystal", 16),
                    i("law_crystal", 128), i("resolved_anomaly", 6))),
            new Recipe("omnipotence_seed", i("omnipotence_seed", 1), List.of(i("rekindled_heart", 2), i("echo_lattice", 2), i("omnipotence_shard", 64),
                    i("proof_of_everything", 8), i("absolute_singularity", 16), i("resolved_anomaly", 12))),
            new Recipe("demiurge_hand", i("demiurge_hand", 1), List.of(i("rekindled_heart", 1), i("echo_lattice", 1), i("omnipotent_ingot", 96),
                    i("transcendence_crystal", 32), i("proof_of_everything", 8), i("omnipotence_shard", 48), i("resolved_anomaly", 8))),
            // round 32: a pickaxe folded through twelve dimensions, and the Eschaton Cells every endgame item now runs on
            new Recipe("dodecaract_excavator", i("dodecaract_excavator", 1), List.of(i("eschaton_core", 6), i("tesseract_core", 128), i("dimensional_singularity", 24),
                    i("calabi_yau_manifold", 64), i("echo_lattice", 1), i("omnipotent_ingot", 112))),
            new Recipe("eschaton_cell", i("eschaton_cell", 4), List.of(i("eschaton_core", 1), i("omnipotent_ingot", 24), i("transcendence_crystal", 6),
                    i("absolute_singularity", 4), i("pleroma_crystal", 16), i("logos_thread", 16), i("omnipotence_shard", 8), i("omnipotence_dust", 32),
                    i("rekindled_spark", 0))),
            // the last thing in the mod: the Omnipotence Core itself, forged anew
            new Recipe("forged_omnipotence_core", i("forged_omnipotence_core", 1), List.of(i("omnipotence_seed", 4), i("eschaton_core", 16), i("ex_nihilo_engine", 1),
                    i("equilibrium_lattice", 1), i("lawgivers_codex", 1), i("omnipotent_ingot", 256), i("rekindled_spark", 8), i("resolved_anomaly", 32))),
            // round 35: holds space flat around its bearer - made from what the Anomaly branch resolved
            new Recipe("euclidean_anchor", i("euclidean_anchor", 1), List.of(i("eschaton_core", 2), i("resolved_anomaly", 24), i("geodesic_anchor", 16),
                    i("reality_anchor", 32), i("stress_energy_tensor", 32), i("quarantine_field_matrix", 16), i("omnipotent_ingot", 48))));

    /**
     * Round 31: the mod's own recipes with the datapacks' changes - replaced ones swapped in place, removed ones left
     * out, new ones after them (in file order). Exactly {@link #RECIPES} when no datapack touches the Forge.
     */
    public static List<Recipe> base() {
        var d = com.theoreticalsciences.custom.ForgeRecipes.data();
        if (d.empty()) return RECIPES;
        List<Recipe> out = new java.util.ArrayList<>();
        for (Recipe r : RECIPES) {
            if (d.removed(r.id())) continue;
            Recipe o = d.overrides().get(r.id());
            out.add(o != null ? o : r);
        }
        for (Recipe r : d.added()) if (!d.removed(r.id())) out.add(r);
        return List.copyOf(out);
    }

    private static volatile List<Recipe> all;
    private static volatile java.util.Set<Item> creative = java.util.Set.of();

    /** How a client reaches its recipes and registries (set by the client; the server uses its own). */
    public static volatile java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeManager> CLIENT_RECIPES;
    public static volatile java.util.function.Supplier<net.minecraft.core.HolderLookup.Provider> CLIENT_REGISTRIES;

    /** Tags or recipes were (re)loaded: the creative list is worked out again (it leaves out what lies beyond Omnipotence). */
    public static synchronized void invalidate() {
        all = null;
    }

    /**
     * Every Forge recipe: the fixed ones above, then, for each mod with creative items, the simulation of that mod's
     * universe (made from the mod's own most advanced things) and one recipe per creative item, each built from that
     * universe, the item's own best ordinary version and the material that fits what it does (round 22).
     */
    public static synchronized List<Recipe> all() {
        if (all == null) {
            List<Recipe> base = base();
            List<Recipe> out = new java.util.ArrayList<>(base);
            List<Item> cr = new java.util.ArrayList<>(TranscendentalBuilder.relics());
            cr.sort(java.util.Comparator.comparing(it -> BuiltInRegistries.ITEM.getKey(it).toString()));
            java.util.Set<Item> crSet = new java.util.HashSet<>(cr);
            java.util.Map<String, List<Item>> byMod = new java.util.TreeMap<>();
            for (Item it : cr) byMod.computeIfAbsent(BuiltInRegistries.ITEM.getKey(it).getNamespace(), k -> new java.util.ArrayList<>()).add(it);
            // round 35: a recipe (a pack's, usually) that asks for a Simulated Universe of an installed mod gets that
            // universe's recipe too, even when the mod has no creative items of its own (vanilla Minecraft, say)
            for (Recipe r : base)
                for (Ing g : r.in())
                    if (g.universe() != null && (g.universe().equals("minecraft") || net.neoforged.fml.ModList.get().isLoaded(g.universe())))
                        byMod.computeIfAbsent(g.universe(), k -> new java.util.ArrayList<>());
            Samples samples = Samples.of(byMod.keySet(), crSet);
            // every item of those mods with its name split into words, once (not the whole registry per creative item)
            java.util.Map<String, List<Object[]>> words = new java.util.HashMap<>();
            for (Item it : BuiltInRegistries.ITEM) {
                ResourceLocation k = BuiltInRegistries.ITEM.getKey(it);
                if (!byMod.containsKey(k.getNamespace()) || crSet.contains(it) || k.getPath().contains("creative")) continue;
                words.computeIfAbsent(k.getNamespace(), x -> new java.util.ArrayList<>())
                        .add(new Object[]{it, new java.util.HashSet<>(java.util.Arrays.asList(k.getPath().split("[_/.]"))), k.toString()});
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Recipe r : base) seen.add(signature(r.in()));
            for (var e : byMod.entrySet()) {
                String mod = e.getKey();
                out.add(unique(simulation(mod, samples.top(mod)), seen));
                for (Item it : e.getValue()) out.add(unique(creativeRecipe(mod, it, words.getOrDefault(mod, List.of())), seen));
            }
            creative = java.util.Set.copyOf(cr);
            // a datapack may also remove the generated ones ("simulate/<mod>", "creative/<item id>")
            var fd = com.theoreticalsciences.custom.ForgeRecipes.data();
            if (!fd.removed().isEmpty()) out.subList(base.size(), out.size()).removeIf(r -> fd.removed(r.id()));
            List<Recipe> done = List.copyOf(out);
            if (samples.complete) all = done;          // recipes not here yet (a client still joining): work it out again later
            return done;
        }
        return all;
    }

    // ------------------------------------------------------------------ round 22: simulated universes
    private static int h(String s, int salt) {
        int x = s.hashCode() * 0x9E3779B1 + salt * 0x85EBCA6B;
        x ^= x >>> 15;
        x *= 0x2C1B3C6D;
        x ^= x >>> 12;
        return x & 0x7FFFFFFF;
    }

    private static final String[] COSMOS = {"calabi_yau_manifold", "m_brane", "spin_foam_lattice", "multiverse_shard", "bubble_universe_sample",
            "cosmological_constant", "false_vacuum_membrane", "heat_death_remnant", "baryogenesis_seed", "hypersphere_fragment", "e8_lattice", "primordial_charter"};

    /** The Simulated Universe of one mod: the stuff a universe is made of, and its own most advanced things. */
    static Recipe simulation(String mod, List<Ing> samples) {
        List<Ing> in = new java.util.ArrayList<>();
        in.add(i("universe_seed", 48 + h(mod, 1) % 49));
        in.add(i("pocket_universe", 12 + h(mod, 2) % 21));
        in.add(i("omnipotent_ingot", 16 + h(mod, 3) % 33));
        in.add(i(COSMOS[h(mod, 4) % COSMOS.length], 16 + h(mod, 5) % 49));
        in.addAll(samples);
        in.add(i("rekindled_spark", 0));
        return new Recipe("simulate/" + mod, new Ing("simulated_universe", 1, mod), in);
    }

    /** Round 35: the best ordinary version of creative items whose names do not lead to it. */
    static final java.util.Map<String, String> TWIN = java.util.Map.of(
            "create:creative_motor", "create:steam_engine",
            "create:creative_crate", "create:item_vault",
            "create:creative_fluid_tank", "create:fluid_tank",
            "create:creative_blaze_cake", "create:blaze_cake",
            "create:handheld_worldshaper", "create:schematicannon");

    /** What a creative item is for decides the one material of this mod that goes into it. */
    static String theme(String path) {
        String p = path.toLowerCase(java.util.Locale.ROOT);
        // round 35: Create's - rotation is energy, the Worldshaper builds, the cake feeds a blaze for ever
        if (p.matches(".*(motor|engine|rotation|kinetic).*")) return "negentropy_cell";
        if (p.matches(".*(worldshaper|zapper|shaper).*")) return "programmable_matter";
        if (p.matches(".*(blaze|cake).*")) return "psionic_crystal";
        if (p.matches(".*(chest|bin|storage|drawer|vending|container|cache|crate|barrel|item).*")) return "pocket_universe";
        if (p.matches(".*(tank|drum|fluid|jar|pool|bucket|steam|water).*")) return "false_vacuum_membrane";
        if (p.matches(".*(energy|power|capacitor|battery|cube|generator|cell|op_).*")) return "negentropy_cell";
        if (p.matches(".*(wand|book|grimoire|spell|gem|ink|dagger).*")) return "law_crystal";
        if (p.matches(".*(mana|essence|essentia|source|flux|rad|particle|soul|node|spark).*")) return "psionic_crystal";
        if (p.matches(".*(computation|data|screen|hatch|drive|access).*")) return "superintelligence_core";
        if (p.matches(".*(harvest|mill|spike|upgrade|paste|builder|destruction).*")) return "programmable_matter";
        return "absolute_infinity";
    }

    private static final String[] TIERS = {"basic", "simple", "standard", "improved", "advanced", "reinforced", "hardened", "elite", "superior", "ultimate",
            "infinity", "final"};

    /** The best ordinary version of a creative item (mekanism:creative_bin -> mekanism:ultimate_bin), if there is one. */
    @javax.annotation.Nullable
    @SuppressWarnings("unchecked")
    static Item counterpart(Item creativeItem, List<Object[]> candidates) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(creativeItem);
        // round 35: hand-picked ordinary versions where the name alone does not say (Create)
        String pick = TWIN.get(key.toString());
        if (pick != null) {
            ResourceLocation rl = ResourceLocation.tryParse(pick);
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) return BuiltInRegistries.ITEM.get(rl);
        }
        List<String> want = new java.util.ArrayList<>();
        for (String t : key.getPath().split("[_/.]"))
            if (!t.isEmpty() && !t.contains("creative") && !t.equals("item") && !t.equals("block")) want.add(t);
        if (want.isEmpty()) return null;
        Item best = null;
        int bestScore = 0;
        String bestId = null;
        for (Object[] c : candidates) {
            Item it = (Item) c[0];
            java.util.Set<String> toks = (java.util.Set<String>) c[1];
            int match = 0;
            for (String t : want) if (toks.contains(t)) match++;
            if (match == 0 || !toks.contains(want.get(want.size() - 1))) continue;
            int tier = 0;
            for (int t = 0; t < TIERS.length; t++) if (toks.contains(TIERS[t])) tier = t + 1;
            int score = match * 100 + tier * 10 - (toks.size() - match);
            if (score > bestScore || (score == bestScore && best != null && ((String) c[2]).compareTo(bestId) < 0)) {
                best = it;
                bestId = (String) c[2];
                bestScore = score;
            }
        }
        return best;
    }

    static Recipe creativeRecipe(String mod, Item it, List<Object[]> candidates) {
        String id = BuiltInRegistries.ITEM.getKey(it).toString();
        List<Ing> in = new java.util.ArrayList<>();
        in.add(new Ing("simulated_universe", 1 + h(id, 1) % 4, mod));
        in.add(i("eschaton_core", 1 + h(id, 2) % 3));
        Item twin = counterpart(it, candidates);
        if (twin != null) {
            int max = new ItemStack(twin).getMaxStackSize();
            in.add(new Ing(BuiltInRegistries.ITEM.getKey(twin).toString(), max <= 1 ? 1 + h(id, 3) % 2 : Math.min(64, max)));
        }
        in.add(i(theme(BuiltInRegistries.ITEM.getKey(it).getPath()), 32 + h(id, 4) % 97));
        in.add(i("omnipotent_ingot", 24 + h(id, 5) % 73));
        in.add(i("rekindled_spark", 0));
        return new Recipe("creative/" + id, new Ing(id, 1), in);
    }

    private static String signature(List<Ing> in) {
        List<String> l = new java.util.ArrayList<>();
        for (Ing g : in) l.add(g.item() + "#" + g.universe() + "x" + g.count());
        java.util.Collections.sort(l);
        return String.join(",", l);
    }

    /** Never two recipes asking for exactly the same: nudge the Omnipotent Ingots until it differs. */
    private static Recipe unique(Recipe r, java.util.Set<String> seen) {
        List<Ing> in = new java.util.ArrayList<>(r.in());
        while (!seen.add(signature(in))) {
            for (int k = 0; k < in.size(); k++)
                if (in.get(k).item().equals("omnipotent_ingot")) {
                    in.set(k, i("omnipotent_ingot", in.get(k).count() + 1));
                    break;
                }
        }
        return new Recipe(r.id(), r.out(), List.copyOf(in));
    }

    /** Each mod's most advanced obtainable things: what its recipes make, rarest and most involved first. */
    static final class Samples {
        final java.util.Map<String, List<Ing>> byMod = new java.util.HashMap<>();
        boolean complete = true;
        static final int MAX = 12;

        static Samples of(java.util.Set<String> mods, java.util.Set<Item> relics) {
            Samples s = new Samples();
            if (mods.isEmpty()) return s;
            net.minecraft.world.item.crafting.RecipeManager rm = null;
            net.minecraft.core.HolderLookup.Provider regs = null;
            var srv = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            // each thread reads its own side's recipes (in singleplayer the screen and JEI must not touch the server's)
            if (srv != null && srv.isSameThread()) {
                rm = srv.getRecipeManager();
                regs = srv.registryAccess();
            } else if (CLIENT_RECIPES != null && CLIENT_REGISTRIES != null) {
                rm = CLIENT_RECIPES.get();
                regs = CLIENT_REGISTRIES.get();
            } else if (srv != null) {
                rm = srv.getRecipeManager();
                regs = srv.registryAccess();
            }
            if (rm == null || regs == null) {
                s.complete = false;
                return s;
            }
            java.util.Map<Item, Integer> involved = new java.util.HashMap<>();
            for (var holder : rm.getRecipes()) {
                ItemStack res;
                int n;
                try {
                    res = holder.value().getResultItem(regs);
                    n = holder.value().getIngredients().size();
                } catch (Throwable t) {
                    continue;
                }
                if (res == null || res.isEmpty()) continue;
                Item it = res.getItem();
                ResourceLocation k = BuiltInRegistries.ITEM.getKey(it);
                if (!mods.contains(k.getNamespace()) || relics.contains(it) || k.getPath().contains("creative")) continue;
                if (it instanceof net.minecraft.world.item.SpawnEggItem || new ItemStack(it).is(OmnipotencePayload.BEYOND)) continue;
                involved.merge(it, n, Math::max);
            }
            java.util.Map<String, List<Item>> per = new java.util.HashMap<>();
            for (Item it : involved.keySet()) per.computeIfAbsent(BuiltInRegistries.ITEM.getKey(it).getNamespace(), k -> new java.util.ArrayList<>()).add(it);
            for (var e : per.entrySet()) {
                List<Item> l = e.getValue();
                l.sort(java.util.Comparator.<Item>comparingInt(it -> -(new ItemStack(it).getRarity().ordinal() * 1000 + Math.min(81, involved.get(it)) * 10))
                        .thenComparing(it -> BuiltInRegistries.ITEM.getKey(it).toString()));
                List<Ing> ings = new java.util.ArrayList<>();
                for (Item it : l.subList(0, Math.min(MAX, l.size()))) {
                    ItemStack st = new ItemStack(it);
                    int base = switch (st.getRarity()) {
                        case EPIC -> 8;
                        case RARE -> 16;
                        case UNCOMMON -> 32;
                        default -> 64;
                    };
                    int max = st.getMaxStackSize();
                    ings.add(new Ing(BuiltInRegistries.ITEM.getKey(it).toString(), max <= 1 ? 1 : Math.min(max, base)));
                }
                s.byMod.put(e.getKey(), ings);
            }
            return s;
        }

        List<Ing> top(String mod) {
            return byMod.getOrDefault(mod, List.of());
        }
    }

    /** Creative items: only the Forge makes them. */
    public static boolean forgeOnly(Item it) {
        all();
        return creative.contains(it);
    }

    /** "rekindled_spark x0" in a recipe means: needed, but not used up (a catalyst). */
    public static int count(Player p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int k = 0; k < inv.getContainerSize(); k++) {
            ItemStack s = inv.getItem(k);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    public static int count(Player p, Ing g) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int k = 0; k < inv.getContainerSize(); k++) {
            ItemStack s = inv.getItem(k);
            if (g.matches(s)) n += s.getCount();
        }
        return n;
    }

    /**
     * Tried on a copy of the counts, in the same order the Forge takes them: if two ingredients could take from the
     * same stacks, each still has to find enough of its own.
     */
    public static boolean canMake(Player p, Recipe r) {
        Inventory inv = p.getInventory();
        int[] left = new int[inv.getContainerSize()];
        for (int k = 0; k < left.length; k++) left[k] = inv.getItem(k).getCount();
        for (Ing g : r.in()) {
            int need = Math.max(1, g.count());
            boolean keep = g.count() == 0;           // a catalyst: only has to be there
            for (int k = 0; k < left.length && need > 0; k++) {
                if (left[k] <= 0 || !g.matches(inv.getItem(k))) continue;
                int take = Math.min(need, left[k]);
                if (!keep) left[k] -= take;
                need -= take;
            }
            if (need > 0) return false;
        }
        return true;
    }

    public static Recipe byId(String id) {
        for (Recipe r : all()) if (r.id().equals(id)) return r;
        return null;
    }

    /** Server: forge once, if the simulated laws converge (the screen sends one request per click). */
    public static void forge(ServerPlayer sp, String id, int[] laws) {
        Recipe r = byId(id);
        if (r == null || !canMake(sp, r)) return;
        boolean drifts = ForgeDrift.drifts(r.id());
        int wait = drifts ? ForgeDrift.secondsLeft(sp.server, r.id()) : 0;
        if (wait > 0) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.forge_reseeding", wait / 60, String.format("%02d", wait % 60))
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            ForgeDrift.sync(sp);
            return;
        }
        if (!ForgeLaws.converged(ForgeLaws.problem(r.id(), drifts ? ForgeDrift.epoch(sp.server, r.id()) : 0), laws)) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.forge_diverges")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        Inventory inv = sp.getInventory();
        for (Ing g : r.in()) {
            int need = g.count();
            for (int k = 0; k < inv.getContainerSize() && need > 0; k++) {
                ItemStack s = inv.getItem(k);
                if (!g.matches(s)) continue;
                int take = Math.min(need, s.getCount());
                s.shrink(take);
                need -= take;
            }
        }
        inv.setChanged();
        ItemStack out = r.result();
        if (!inv.add(out)) sp.drop(out, false);
        if (drifts) {                     // round 36: that universe is spent - new laws, and the Core re-seeds
            ForgeDrift.forged(sp.server, r.id());
            ForgeDrift.sync(sp);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.forge_drifted")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.OMNIPOTENCE_GRANT.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
        if (sp.level() instanceof net.minecraft.server.level.ServerLevel sl)
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), sp.getX(), sp.getY() + 1.2, sp.getZ(), 40, 0.6, 0.8, 0.6, 0.08);
    }

    private CoreForge() {}
}
