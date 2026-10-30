package com.theoreticalsciences.endgame;

import com.theoreticalsciences.registry.QHItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Carrying the Omnilex lets you fly: gravity becomes just another law you are allowed to ignore.
 * Flight is only taken away again if the Omnilex granted it, so creative mode and other mods' flight are left alone.
 */
public final class OmnilexAbilities {
    private static final String TAG = "theoreticalsciences_omnilex_flight";

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 10 != 0) return;
        Item omnilex = QHItems.get("omnilex");
        boolean has = sp.getInventory().contains(new net.minecraft.world.item.ItemStack(omnilex));
        CompoundTag data = sp.getPersistentData();
        boolean granted = data.getBoolean(TAG);
        if (has && !sp.getAbilities().mayfly) {
            sp.getAbilities().mayfly = true;
            sp.onUpdateAbilities();
            data.putBoolean(TAG, true);
        } else if (!has && granted) {
            data.remove(TAG);
            // still wearing the whole Raiment of Transcendence: it keeps the flight going
            if (Raiment.wearsFullSet(sp)) {
                data.putBoolean("theoreticalsciences_raiment_flight", true);
                return;
            }
            if (!sp.isCreative() && !sp.isSpectator()) {
                sp.getAbilities().mayfly = false;
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            }
        }
    }

    private OmnilexAbilities() {}
}
