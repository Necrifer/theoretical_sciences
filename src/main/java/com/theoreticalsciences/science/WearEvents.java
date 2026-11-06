package com.theoreticalsciences.science;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.2.0: a radioactive source's clock starts the first time it is seen (made, crafted, carried), and one taken out of a
 * Chronostatic Vault by a shift-click (which hands over the stack as it lies in the vault) starts running again once it
 * is in the inventory.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class WearEvents {
    static final int PERIOD = 40;

    /** Technetium-99m dropped on the ground decays there too. */
    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity ie) || ie.level().isClientSide || ie.tickCount % 20 != 0) return;
        ItemStack s = ie.getItem();
        Wear.Profile pr = Wear.profile(s);
        if (pr == null || !pr.exposed()) return;
        ItemStack c = s.copy();
        Wear.expose(c, 20);
        ie.setItem(c);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % PERIOD != 7 || p.isSpectator()) return;
        long now = p.level().getGameTime();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            Wear.Profile pr = Wear.profile(s);
            if (pr == null || !pr.source()) continue;
            Wear.Clock c = s.get(Wear.CLOCK.get());
            if (pr.exposed()) Wear.expose(s, PERIOD);             // Technetium-99m carried loose (not in a Lead Pig)
            else if (c == null) Wear.stamp(s, now);
            else if (c.frozen()) inv.setItem(i, Wear.release(s, now));
        }
        ItemStack carried = p.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            Wear.expose(carried, PERIOD);
            if (!(p.containerMenu instanceof net.minecraft.world.inventory.ChestMenu m && m.getContainer() instanceof ChronostaticVaultBlockEntity))
                Wear.stamp(carried, now);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        Wear.stamp(e.getCrafting(), e.getEntity().level().getGameTime());
    }

    private WearEvents() {}
}
