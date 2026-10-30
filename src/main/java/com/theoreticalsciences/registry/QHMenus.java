package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class QHMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TheoreticalSciences.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE = MENUS.register("machine",
            () -> IMenuTypeExtension.create(MachineMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<com.theoreticalsciences.storage.InfinityCacheMenu>> INFINITY_CACHE = MENUS.register("transcendent_cache",
            () -> IMenuTypeExtension.create(com.theoreticalsciences.storage.InfinityCacheMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<com.theoreticalsciences.dungeon.AnomalyEngineMenu>> ANOMALY_ENGINE = MENUS.register("anomaly_engine",
            () -> IMenuTypeExtension.create(com.theoreticalsciences.dungeon.AnomalyEngineMenu::fromNetwork));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private QHMenus() {}
}
