package com.theoreticalsciences.research;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Round 33: the first time a player joins a world, they get the Quantum Codex (config {@code giveCodexOnFirstJoin})
 * and a one-time chat message saying how to use it - use it to read, sneak-use it for the research tree, and the
 * look-up key. Remembered in the player's persistent data (kept through death), so it happens once per player per
 * world.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class FirstJoinGift {
    private static final String KEY = TheoreticalSciences.MODID + ":first_join_done";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        CompoundTag root = sp.getPersistentData();
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (kept.getBoolean(KEY)) return;
        kept.putBoolean(KEY, true);
        root.put(Player.PERSISTED_NBT_TAG, kept);
        boolean give = QHConfig.GIVE_CODEX_ON_FIRST_JOIN.get();
        boolean gave = false;
        if (give) {
            Item codex = QHItems.get("quantum_codex");
            if (codex != null && !sp.getInventory().contains(new ItemStack(codex))) {
                ItemStack st = new ItemStack(codex);
                if (!sp.getInventory().add(st)) sp.drop(st, false);
                gave = true;
            } else gave = codex != null;
        }
        sp.sendSystemMessage(Component.translatable(gave ? "message.theoreticalsciences.first_join" : "message.theoreticalsciences.first_join_craft")
                .withStyle(ChatFormatting.AQUA));
        sp.sendSystemMessage(Component.translatable("message.theoreticalsciences.first_join_lookup",
                Component.keybind("key.theoreticalsciences.codex_lookup").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
    }

    private FirstJoinGift() {}
}
