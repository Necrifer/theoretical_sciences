package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class QHTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheoreticalSciences.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.theoreticalsciences"))
            .icon(() -> new ItemStack(QHItems.get("tesseract_core")))
            .displayItems((params, out) -> {
                out.accept(QHItems.get("quantum_codex"));
                out.accept(QHItems.get("spectral_analyzer"));
                out.accept(QHItems.get("research_terminal"));
                QHItems.BLOCK_ITEMS.values().forEach(i -> {
                    if (i.get() != QHItems.get("research_terminal")) out.accept(i.get());
                });
                QHItems.ALL.values().forEach(i -> {
                    if (i.get() != QHItems.get("quantum_codex") && i.get() != QHItems.get("spectral_analyzer")) out.accept(i.get());
                });
                out.accept(com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_TABLE_ITEM.get());
                out.accept(com.theoreticalsciences.extreme.ExtremeRegistry.COMPACTOR_ITEM.get());
                out.accept(com.theoreticalsciences.extreme.ExtremeRegistry.AUTO_ASSEMBLER_ITEM.get());
                com.theoreticalsciences.transcend.TranscendRegistry.TAB.forEach(i -> out.accept(i.get()));
                QHFluids.ALL.values().forEach(f -> out.accept(f.bucket.get()));
                QHEntities.SPAWN_EGGS.forEach(e -> out.accept(e.get()));
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private QHTabs() {}
}
