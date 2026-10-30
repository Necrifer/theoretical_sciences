package com.theoreticalsciences.transcend;

import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the Transcendental the first time anyone arrives (it cannot be generated chunk by chunk: it is one object).
 * <ul>
 *   <li>A disc of transcendent stone 112 blocks across, inlaid with a golden-angle sunflower spiral.</li>
 *   <li>At the centre: <b>Omnipotency</b> - ringed walls, eight omnium pillars, a floating halo and, on its dais,
 *       the Omnipotence Core, which makes whatever is asked of it. None of it can be broken or built elsewhere.</li>
 *   <li>The Hall of Matter: a ring of pedestals carrying the most expensive blocks in the pack.</li>
 *   <li>Eight reliquaries holding every creative item of every mod installed, plus Avaritia's infinity gear.</li>
 *   <li>A rift back to the Overworld.</li>
 * </ul>
 */
public final class TranscendentalBuilder {
    public static final int Y = 100;
    public static final BlockPos CORE = new BlockPos(0, Y + 2, 0);
    public static final BlockPos ARRIVAL = new BlockPos(0, Y, 30);
    public static final BlockPos RIFT = new BlockPos(0, Y, 50);

    /** Explicit relics (anything not installed is skipped). Every other registered item with "creative" in its id is added at build time too. */
    static final String[] RELICS = {
            "ae2:creative_energy_cell", "ae2:creative_storage_cell", "applied_thaumaturgy:creative_essentia_cell",
            "mekanism:creative_bin", "mekanism:creative_chemical_tank", "mekanism:creative_energy_cube", "mekanism:creative_fluid_tank",
            "draconicevolution:creative_capacitor", "draconicevolution:creative_op_capacitor",
            "bigreactors:basic_turbinecreativesteamgenerator", "bigreactors:reinforced_reactorcreativewatergenerator", "bigreactors:reinforced_turbinecreativesteamgenerator",
            "immersiveengineering:capacitor_creative", "nuclearcraft:creative_particle_source", "nuclear_radiation:creative_rad_source",
            "gtceu:creative_chest", "gtceu:creative_computation_provider", "gtceu:creative_data_access_hatch", "gtceu:creative_energy", "gtceu:creative_tank",
            "oritech:creative_storage_block", "oritech:creative_tank_block", "enderio:creative_power",
            "rftoolspower:dimensionalcell_creative", "rftoolsutility:creative_screen",
            "excessive_utilities:creative_builders_wand", "excessive_utilities:creative_chest", "excessive_utilities:creative_destruction_wand",
            "excessive_utilities:creative_drum", "excessive_utilities:creative_energy_source", "excessive_utilities:creative_harvest",
            "excessive_utilities:creative_mill", "excessive_utilities:creative_spike", "excessive_utilities:creative_upgrade",
            "storagedrawers:creative_storage_upgrade", "storagedrawers:creative_vending_upgrade", "buildinggadgets2:construction_paste_container_creative",
            "botania:creative_pool", "botania:corporea_spark_creative", "ars_nouveau:creative_source_jar", "ars_nouveau:creative_spell_book",
            "primalmagick:creative_wand_gem_item", "primalmagick:grimoire_creative", "primalmagick:mana_singularity_creative",
            "spectrum:creative_ink_assortment", "spectrum:creative_particle_spawner",
            "thaumaturge:creative_flux_sponge", "thaumaturge:creative_node_placer", "thaumaturge:jar_creative",
            "mysticalagradditions:creative_essence", "mysticalagriculture:creative_soulium_dagger", "neoecoprototype:migration_drive_creative",
            "woot_revived:creative_power", "woot_revived:creative_tank",
            "avaritia_integration:creative_augment_base", "avaritia_integration:creative_compound", "avaritia_integration:creative_compressed_iron",
            "avaritia_integration:creative_integral_components", "avaritia_integration:creative_mechanism",
            // round 35: Create - the Worldshaper is creative-only without saying so in its name
            "create:creative_motor", "create:creative_fluid_tank", "create:creative_crate", "create:creative_blaze_cake", "create:handheld_worldshaper",
            // Avaritia's infinity gear - creative in all but name
            "avaritia:infinity_sword", "avaritia:infinity_pickaxe", "avaritia:infinity_axe", "avaritia:infinity_shovel", "avaritia:infinity_hoe",
            "avaritia:infinity_bow", "avaritia:infinity_crossbow", "avaritia:infinity_trident", "avaritia:infinity_mace", "avaritia:infinity_shield",
            "avaritia:infinity_helmet", "avaritia:infinity_chestplate", "avaritia:infinity_pants", "avaritia:infinity_boots", "avaritia:infinity_elytra",
            "avaritia:infinity_totem", "avaritia:infinity_umbrella", "avaritia:infinity_bucket", "avaritia:infinity_ring", "avaritia:infinity_clock",
            "avaritia:infinity_upgrade", "avaritia:eternal_singularity", "avaritia_expand:infinity_shears", "avaritia_expand:infinity_book",
            "avaritia_delight:infinity_knife", "avaritia_integration:infinity_capacitor", "avaritia_integration:infinity_me_storage_cell_big",
    };

    /** The most expensive blocks around: displayed on pedestals and used as the reliquaries' pillars. */
    /** The endgame multiblocks just before what lies beyond Omnipotence: one controller in each reliquary. */
    static final String[] CONTROLLERS = {"reality_compiler", "axiomatic_breach_engine", "omnilex_engine", "transcendent_plasma_forge",
            "eye_of_the_horizon", "concept_compressor", "abiogenesis_crucible", "vacuum_harvester"};

    static final String[] EXPENSIVE = {
            "avaritia:infinity", "avaritia:neutron", "avaritia:crystal_matrix", "avaritia:diamond_lattice_block", "avaritia:blaze_cube_block",
            "avaritia:star_fuel_block", "avaritia:refined_coal_block", "avaritia_delight:neutronium_hay_bale",
            "theoreticalsciences:omnium_block", "theoreticalsciences:transfinite_coil_block", "theoreticalsciences:cosmic_neutronium_block",
            "theoreticalsciences:spacetime_alloy_block", "theoreticalsciences:computronium_block", "theoreticalsciences:dilithium_block",
            "theoreticalsciences:void_lattice_block", "theoreticalsciences:chronosteel_block", "theoreticalsciences:neutronium_block",
            "theoreticalsciences:cosmic_neutronium_coil_block", "minecraft:netherite_block", "minecraft:beacon", "minecraft:diamond_block",
            "minecraft:emerald_block", "minecraft:gold_block", "minecraft:dragon_egg",
    };

    static final class Flag extends SavedData {
        boolean built;

        static Flag load(CompoundTag t, HolderLookup.Provider r) {
            Flag f = new Flag();
            f.built = t.getBoolean("Built");
            return f;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
            t.putBoolean("Built", built);
            return t;
        }
    }

    public static void ensureBuilt(ServerLevel level) {
        Flag f = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Flag::new, Flag::load, null), "theoreticalsciences_transcendental");
        // the heart of the Transcendental bends sight itself (also added to worlds built before round 14)
        com.theoreticalsciences.world.zones.StructureZones.mark(level, CORE, 22, com.theoreticalsciences.world.zones.StructureZones.Mode.TRANSCENDENT);
        if (f.built) return;
        build(level);
        f.built = true;
        f.setDirty();
    }

    /** The Transcendental was destroyed: the next ensureBuilt builds it afresh. */
    public static void forgetBuilt(ServerLevel level) {
        Flag f = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Flag::new, Flag::load, null), "theoreticalsciences_transcendental");
        f.built = false;
        f.setDirty();
    }

    private static Block qh(String id) {
        return QHBlocks.get(id);
    }

    private static void set(ServerLevel l, int x, int y, int z, Block b) {
        l.setBlock(new BlockPos(x, y, z), b.defaultBlockState(), 2);
    }

    static List<Block> expensiveBlocks() {
        List<Block> out = new ArrayList<>();
        for (String id : EXPENSIVE) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) out.add(BuiltInRegistries.BLOCK.get(rl));
        }
        return out;
    }

    /** The creative items of every installed mod: no longer handed out; only the rebuilt Core's Forge makes them. */
    public static List<Item> relics() {
        Set<Item> out = new LinkedHashSet<>();
        for (String id : RELICS) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) out.add(BuiltInRegistries.ITEM.get(rl));
        }
        // anything else any installed mod calls "creative"
        for (ResourceLocation rl : BuiltInRegistries.ITEM.keySet()) {
            String p = rl.getPath();
            if (p.contains("creative") && !p.contains("tab_icon") && !p.contains("spawn_egg")) out.add(BuiltInRegistries.ITEM.get(rl));
        }
        out.remove(Items.AIR);
        // never the creative-only tools (Laplace's Demon, the Oracle's Lemma) nor anything beyond Omnipotence
        out.removeIf(i -> new ItemStack(i).is(OmnipotencePayload.BEYOND));
        return new ArrayList<>(out);
    }

    private static void build(ServerLevel l) {
        Block stone = qh("transcendent_stone"), glass = qh("transcendent_glass"), omnium = qh("omnium_block"), casing = qh("omnipotence_casing");
        int y = Y - 1;
        // ---- the disc, with a golden-angle spiral of glass and an omnium rim
        for (int x = -56; x <= 56; x++)
            for (int z = -56; z <= 56; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > 56.5) continue;
                set(l, x, y, z, r > 54.5 ? omnium : stone);
                if (r > 40 && r < 54) set(l, x, y - 1, z, stone);
            }
        for (int i = 0; i < 700; i++) {
            double r = 2.0 * Math.sqrt(i), a = i * 2.39996323;
            if (r > 53) break;
            set(l, (int) Math.round(Math.cos(a) * r), y, (int) Math.round(Math.sin(a) * r), glass);
        }
        // a hanging keel beneath the disc so it does not look paper-thin
        for (int x = -40; x <= 40; x++)
            for (int z = -40; z <= 40; z++) {
                double r = Math.sqrt(x * x + z * z);
                int depth = (int) (18 * (1 - r / 40.0) * (1 - r / 40.0));
                for (int d = 1; d <= depth; d++) set(l, x, y - 1 - d, z, (d + x + z) % 7 == 0 ? glass : stone);
            }
        // ---- Omnipotency
        for (int x = -16; x <= 16; x++)
            for (int z = -16; z <= 16; z++) {
                double r = Math.sqrt(x * x + z * z);
                double ang = Math.toDegrees(Math.atan2(z, x));
                boolean door = Math.abs(((ang % 45) + 45) % 45 - 22.5) > 18.5;          // eight doorways
                if (r >= 14 && r < 15.5 && !door) for (int h = 0; h < 4; h++) set(l, x, Y + h, z, h == 3 ? omnium : casing);
                if (r >= 10 && r < 11.5 && !(Math.abs(x) <= 1 || Math.abs(z) <= 1)) for (int h = 0; h < 8; h++) set(l, x, Y + h, z, h % 3 == 2 ? glass : casing);
                if (r < 6.5) {
                    set(l, x, Y, z, casing);
                    if (r < 4.5) set(l, x, Y + 1, z, r < 2.5 ? glass : casing);
                }
            }
        l.setBlock(CORE, com.theoreticalsciences.transcend.TranscendRegistry.CORE.get().defaultBlockState(), 3);
        // eight omnium pillars with glass crowns
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + 45 * k);
            int px = (int) Math.round(Math.cos(a) * 18), pz = (int) Math.round(Math.sin(a) * 18);
            for (int h = 0; h < 28; h++)
                for (int dx = 0; dx <= 1; dx++)
                    for (int dz = 0; dz <= 1; dz++) set(l, px + dx, Y + h, pz + dz, h >= 25 ? glass : omnium);
        }
        // the halo: a floating ring of casing and glass high above the core, and a smaller tilted one inside it
        for (int i = 0; i < 360; i++) {
            double a = Math.toRadians(i);
            set(l, (int) Math.round(Math.cos(a) * 12), Y + 22, (int) Math.round(Math.sin(a) * 12), i % 15 == 0 ? glass : casing);
            double tx = Math.cos(a) * 7, ty = Math.sin(a) * 7 * 0.5, tz = Math.sin(a) * 7 * 0.866;
            set(l, (int) Math.round(tx), Y + 16 + (int) Math.round(ty), (int) Math.round(tz), glass);
        }
        // ---- the Hall of Matter: pedestals carrying every expensive block that exists here
        List<Block> rich = expensiveBlocks();
        for (int k = 0; k < rich.size(); k++) {
            double a = 2 * Math.PI * k / rich.size();
            int px = (int) Math.round(Math.cos(a) * 26), pz = (int) Math.round(Math.sin(a) * 26);
            set(l, px, Y, pz, omnium);
            set(l, px, Y + 1, pz, rich.get(k));
        }
        // ---- eight reliquaries, each holding the controller of one of the great machines of the endgame
        //      (the creative items are no longer here: only the rebuilt Core's Forge can make them)
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(45 * k);
            int cx = (int) Math.round(Math.cos(a) * 38), cz = (int) Math.round(Math.sin(a) * 38);
            Block pillar = rich.isEmpty() ? omnium : rich.get(k % rich.size());
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    set(l, cx + dx, Y - 1, cz + dz, (dx + dz) % 2 == 0 ? omnium : stone);
                    set(l, cx + dx, Y + 5, cz + dz, Math.abs(dx) == 3 || Math.abs(dz) == 3 ? omnium : glass);
                    if (Math.abs(dx) == 3 && Math.abs(dz) == 3) for (int h = 0; h < 5; h++) set(l, cx + dx, Y + h, cz + dz, pillar);
                }
            set(l, cx, Y, cz, casing);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("theoreticalsciences", CONTROLLERS[k % CONTROLLERS.length]);
            Block ctrl = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (ctrl != null && ctrl != Blocks.AIR) l.setBlock(new BlockPos(cx, Y + 1, cz), ctrl.defaultBlockState(), 2);
        }
        // ---- the way home, and the place everyone arrives
        for (int h = -1; h <= 3; h++)
            for (int dx = -2; dx <= 2; dx++) {
                boolean frame = h == -1 || h == 3 || Math.abs(dx) == 2;
                set(l, dx, Y + h, RIFT.getZ(), frame ? omnium : qh("return_rift"));
            }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) set(l, ARRIVAL.getX() + dx, Y - 1, ARRIVAL.getZ() + dz, glass);
        @SuppressWarnings("unused") BlockState unused = Blocks.AIR.defaultBlockState();
    }

    private TranscendentalBuilder() {}
}
