package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.item.*;
import com.theoreticalsciences.machine.MachineType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QHItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);

    public static final Map<String, DeferredItem<? extends Item>> ALL = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> BLOCK_ITEMS = new LinkedHashMap<>();

    static {
        for (QHContentList.ItemDef def : QHContentList.ITEMS) {
            Item.Properties p = new Item.Properties().stacksTo(def.stack()).rarity(rarityFor(def.id()));
            DeferredItem<? extends Item> holder = switch (def.special()) {
                case "analyzer" -> ITEMS.registerItem(def.id(), SpectralAnalyzerItem::new, p);
                case "codex" -> ITEMS.registerItem(def.id(), CodexItem::new, p);
                case "shifter" -> ITEMS.registerItem(def.id(), BraneShifterItem::new, p);
                case "compass" -> ITEMS.registerItem(def.id(), BraneShifterItem.PlatonicCompass::new, p);
                case "foamdiver" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.foam.PlanckDiverItem::new, p);
                case "horizondiver" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.gr.HorizonDiverItem::new, p);
                case "cantorkey" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.gr.CantorKeyItem::new, p);
                case "cptmirror" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.item.CptMirrorItem::new, p);
                case "oracle" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.item.OracleLemmaItem::new, p);
                case "laplace" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.LaplaceDemonItem::new, p.rarity(Rarity.EPIC));
                case "dynsing" -> ITEMS.registerItem(def.id(), pp -> new com.theoreticalsciences.extreme.DynamicSingularityItem(pp, false), p.rarity(Rarity.EPIC));
                case "dyncluster" -> ITEMS.registerItem(def.id(), pp -> new com.theoreticalsciences.extreme.DynamicSingularityItem(pp, true), p.rarity(Rarity.EPIC));
                case "akashic" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.item.AkashicRecordItem::new, p.rarity(Rarity.EPIC));
                case "specimen" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.extreme.SpecimenBundleItem::new, p.rarity(Rarity.RARE));
                case "stabiliser1" -> ITEMS.registerItem(def.id(), pp -> new com.theoreticalsciences.machine.tuning.StabiliserItem(pp, 1), p);
                case "stabiliser2" -> ITEMS.registerItem(def.id(), pp -> new com.theoreticalsciences.machine.tuning.StabiliserItem(pp, 2), p.rarity(Rarity.UNCOMMON));
                case "stabiliser3" -> ITEMS.registerItem(def.id(), pp -> new com.theoreticalsciences.machine.tuning.StabiliserItem(pp, 3), p.rarity(Rarity.RARE));
                case "stabiliser4" -> ITEMS.registerItem(def.id(), pp -> new com.theoreticalsciences.machine.tuning.StabiliserItem(pp, 4), p.rarity(Rarity.EPIC).fireResistant());
                case "simuniverse" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.transcend.SimulatedUniverseItem::new, p.rarity(Rarity.EPIC));
                case "censer" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.EntropyCenserItem::new, p.rarity(Rarity.EPIC).fireResistant());
                case "emptycell" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.item.ContainmentCells.EmptyCellItem::new, p);
                case "genesampler" -> ITEMS.registerItem(def.id(), GeneSamplerItem::new, p);
                case "tuner" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.logistics.EntanglementTunerItem::new, p);
                case "probe" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.foam.ResonanceProbeItem::new, p);
                case "axiomkey" -> ITEMS.registerItem(def.id(), AxiomKeyItem::new, p);
                case "assembler" -> ITEMS.registerItem(def.id(), StructureAssemblerItem::new, p);
                case "calibrator" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.FieldCalibratorItem::new, p);
                case "autocalibrator" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.FieldCalibratorItem::autonomous, p);
                case "singularity", "whitehole", "tesseract", "universe", "star" -> ITEMS.registerItem(def.id(), pp -> new SingularItem(pp, def.special()), p.fireResistant());
                case "edge" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.OmnipotentEdgeItem::new, p);
                case "scepter" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.SingularityScepterItem::new, p);
                case "armor_head", "armor_chest", "armor_legs", "armor_feet" -> ITEMS.registerItem(def.id(), pp -> com.theoreticalsciences.endgame.TranscendenceArmorItem.of(def.special(), pp), p);
                case "demiurgehand" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.DemiurgeHandItem::new, p.rarity(Rarity.EPIC));
                case "lawcodex" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.LawgiversCodexItem::new, p);
                // round 32: the Dodecaract Excavator and the Eschaton Cell (what every endgame item now runs on)
                case "excavator" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.DodecaractExcavatorItem::new, p.rarity(Rarity.EPIC));
                case "eschatoncell" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.EschatonCellItem::new, p.rarity(Rarity.EPIC));
                // round 35: holds space flat around its bearer (no distortion near impossible structures)
                case "euclideananchor" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.EuclideanAnchorItem::new, p.rarity(Rarity.EPIC));
                default -> ITEMS.registerItem(def.id(), LoreItem::new, p);
            };
            ALL.put(def.id(), holder);
        }
        QHBlocks.SIMPLE.forEach((id, block) -> BLOCK_ITEMS.put(id, ITEMS.registerSimpleBlockItem(block)));
        QHBlocks.MACHINES.forEach((type, block) -> BLOCK_ITEMS.put(type.id(),
                ITEMS.registerItem(type.id(), p -> new MachineBlockItem(block.get(), p, type),
                        new Item.Properties().rarity(type.isMultiblock() ? Rarity.EPIC : Rarity.RARE))));
        BLOCK_ITEMS.put("interface_port", ITEMS.registerSimpleBlockItem(QHBlocks.INTERFACE_PORT));
        BLOCK_ITEMS.put("research_terminal", ITEMS.registerSimpleBlockItem(QHBlocks.RESEARCH_TERMINAL, new Item.Properties().rarity(Rarity.RARE)));
    }

    private static Rarity rarityFor(String id) {
        return switch (id) {
            case "micro_singularity", "white_hole_fragment", "tesseract_core", "planck_shard", "brane_shifter", "calabi_yau_manifold",
                 "pocket_universe", "superintelligence_core", "stellar_core", "universe_seed", "multiverse_shard", "m_brane",
                 "aleph_null_shard", "godel_sentence", "banach_tarski_sphere", "russell_paradox_set", "absolute_infinity", "axiom_key",
                 "bubble_universe_sample", "tree_simulacrum", "ocean_simulacrum", "omnilex", "omega_point",
                 "mathematics_singularity", "standard_model_singularity", "spacetime_singularity", "stellar_singularity", "dimensional_singularity",
                 "temporal_singularity", "quantum_singularity", "cognitive_singularity", "exotic_matter_singularity", "theory_of_everything",
                 "omniscience_catalyst", "omnium_ingot", "omnium_singularity", "surreal_number", "topos", "m_theory_core", "grand_unified_core" -> Rarity.EPIC;
            case "antimatter_pellet", "strange_matter_pellet", "magnetic_monopole", "cosmic_string_segment", "kugelblitz_seed",
                 "alcubierre_coil", "penrose_spindle", "dark_matter_lens", "exotic_matter_crystal", "hawking_emitter" -> Rarity.RARE;
            default -> Rarity.UNCOMMON;
        };
    }

    public static Item get(String id) {
        DeferredItem<? extends Item> i = ALL.get(id);
        if (i != null) return i.get();
        return BLOCK_ITEMS.get(id).get();
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private QHItems() {}
}
