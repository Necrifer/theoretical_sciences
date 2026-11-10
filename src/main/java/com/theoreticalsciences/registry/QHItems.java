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

    /**
     * 1.3.8: what the Planetary Interior yields below its magma sea - the rocks, ores and what they drop - never burns:
     * not in Basaltic Magma, lava or fire (see FluidHazard for the magma).
     */
    public static final java.util.Set<String> FIREPROOF = java.util.Set.of(
            "magnetite", "ringwoodite", "bridgmanite", "peridotite", "core_iron", "core_magnetite_ore", "ringwoodite_ore", "bridgmanite_ore",
            "basalt_column", "crustal_rock", "oklo_rock", "oklo_uraninite_ore", "fission_fragment_ore", "fission_fragment_mix", "raw_pitchblende",
            "rich_iron_ore", "rich_copper_ore", "rich_gold_ore", "rich_iron_chunk", "rich_copper_chunk", "rich_gold_chunk",
            "corundum", "garnet", "peridot", "moissanite", "corundum_crystal", "garnet_crystal", "peridot_crystal", "moissanite_crystal",
            "geode_shell", "inner_core_boundary");

    static {
        for (QHContentList.ItemDef def : QHContentList.ITEMS) {
            Item.Properties p = new Item.Properties().stacksTo(def.stack()).rarity(rarityFor(def.id()));
            if (FIREPROOF.contains(def.id())) p.fireResistant();
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
                case "leadpig" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.science.LeadPigItem::new, p);       // 1.2.0
                case "resonant_fork" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.TuningForkItem::resonant, p);   // 1.2.0
                case "rate_table" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::rateTable, p);   // 1.2.0
                case "confidence_seal" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::confidenceSeal, p);
                case "error_code" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::errorCode, p);
                case "optimised_pathway" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::optimisedPathway, p);
                case "stability_margin" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::stabilityMargin, p);
                case "proof_certificate" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.ProofCertificateItem::new, p);   // 1.2.0
                case "sterile_pack" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.bio.CleaningItem::sterilePack, p);   // 1.2.0
                case "antibiotic" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.bio.CleaningItem::antibiotic, p);
                case "anomaly_ward" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.bio.VaccineItem::new, p);   // 1.2.0
                case "symbiont_lining" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::symbiontLining, p);
                case "synaptic_lattice" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::synapticLattice, p);   // 1.4.0
                case "wear_coating" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::wearCoating, p);
                case "visor" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.mind.LowLightVisorItem::new, p);
                case "synthesis" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.FieldSynthesis.SynthesisItem::new, p);   // 1.7.0
                case "survey_radar" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.archaeology.SurveyRadarItem::new, p.durability(256));   // 1.6.0
                case "faraday_mesh" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::faradayMesh, p);   // 1.5.0
                case "vestibular" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.medicine.MedicineItem::vestibular, p);
                case "anti_inflammatory" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.medicine.MedicineItem::antiInflammatory, p);
                case "probiotic_seal" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.UpgradeItem::probioticSeal, p);
                case "tuning_fork" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.TuningForkItem::new, p);   // 1.2.0
                case "calibration_card" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.machine.tuning.CalibrationCardItem::new, p);   // 1.2.0
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
                case "fold" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.FoldedMatter::new, p.rarity(Rarity.EPIC).fireResistant());   // 1.2.0
                case "eschatoncell" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.EschatonCellItem::new, p.rarity(Rarity.EPIC));
                // round 35: holds space flat around its bearer (no distortion near impossible structures)
                case "euclideananchor" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.endgame.EuclideanAnchorItem::new, p.rarity(Rarity.EPIC));
                // round 37 (1.1): the first instruments of the new sciences
                case "surveyor" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.science.ScienceTools.Surveyor::new, p);
                case "gaussmeter" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.science.ScienceTools.GaussMeter::new, p);
                case "geiger" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.science.ScienceTools.GeigerCounter::new, p);
                case "warpcore" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.frontier.SpacetimeTools.WarpBubbleCore::new, p.rarity(Rarity.RARE));
                case "anchoredwarp" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.frontier.SpacetimeTools.AnchoredWarpBubble::new, p.rarity(Rarity.RARE));
                case "lightsail" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.frontier.FrontierTravel.LightSailItem::new, p);
                case "coredrill" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.interior.CoreDrillItem::new, p);
                case "moissanitedrill" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.interior.MoissaniteDrillItem::new, p);
                case "dice" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.science.ScienceTools.Dice::new, p);
                // 1.2.0: Titan
                case "miniaturiser" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.inner.InnerTravel.MiniaturiserItem::new, p);   // 1.2.0
                case "cryobot" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.ocean.Cryobot::new, p);   // 1.2.0
                case "lander" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.titan.TitanTravel.CryogenicLanderItem::new, p);
                case "crew_capsule" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.orbit.OrbitTravel.CrewCapsuleItem::new, p);   // 1.3.0
                case "bathyscaphe" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.ocean.HadalTrench.BathyscapheItem::new, p);
                case "engineered_seed" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.green.PlantTraits.EngineeredSeedItem::new, p);
                case "ration" -> ITEMS.registerItem(def.id(), LoreItem::new, p.food(new net.minecraft.world.food.FoodProperties.Builder()
                        .nutrition(10).saturationModifier(1.2F).alwaysEdible()
                        .effect(() -> new net.minecraft.world.effect.MobEffectInstance(QHEffects.WELL_FED, 12000, 0), 1.0F).build()));
                case "rebreather" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.titan.TitanGear.RebreatherItem::new, p);
                case "coating" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.titan.TitanGear.CoatingItem::new, p);
                case "prussianblue" -> ITEMS.registerItem(def.id(), com.theoreticalsciences.science.ScienceTools.PrussianBlue::new, p);
                default -> ITEMS.registerItem(def.id(), LoreItem::new, p);
            };
            ALL.put(def.id(), holder);
        }
        QHBlocks.SIMPLE.forEach((id, block) -> BLOCK_ITEMS.put(id, FIREPROOF.contains(id)
                ? ITEMS.registerSimpleBlockItem(block, new Item.Properties().fireResistant()) : ITEMS.registerSimpleBlockItem(block)));
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
