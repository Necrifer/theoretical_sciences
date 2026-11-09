package com.theoreticalsciences.archaeology;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.6.0 Archaeology: the Buried Tell, and the two suspicious soils it hides its finds in. Those are brushed like the
 * game's own suspicious sand, so they join the game's brushable block entity here.
 */
public final class ArchaeologyRegistry {
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<Feature<?>, BuriedTell> BURIED_TELL = FEATURES.register("buried_tell",
            () -> new BuriedTell(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
        bus.addListener((BlockEntityTypeAddBlocksEvent e) -> e.modify(BlockEntityType.BRUSHABLE_BLOCK,
                QHBlocks.get("suspicious_tell_soil"), QHBlocks.get("suspicious_ash_layer")));
    }

    private ArchaeologyRegistry() {}
}
