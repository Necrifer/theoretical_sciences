package com.theoreticalsciences.endgame;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client side of the round 15 endgame. Only ever called on the physical client. */
public final class EndgameClient {
    /** The last laws the server told us about (for the Codex screen); null until the first answer arrives. */
    public static CodexStatePayload state;

    public static void setup() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> state = null);
        com.theoreticalsciences.client.render.GodformRenderer.register();
        // round 32: sneak and scroll with the Dodecaract Excavator to change its reach (instead of the hotbar slot)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown() || e.getScrollDeltaY() == 0) return;
            if (!(mc.player.getMainHandItem().getItem() instanceof DodecaractExcavatorItem)) return;
            e.setCanceled(true);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ExcavatorPayload(e.getScrollDeltaY() > 0 ? 1 : -1));
        });
    }

    /** Opens the Codex screen, remembering the creature the player is looking at (within 32 blocks), if any. */
    public static void openCodex(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) return;
        int target = -1;
        HitResult hit = SingularityScepterItem.aim(player, 32);
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le && le.isAlive()) target = le.getId();
        mc.setScreen(new com.theoreticalsciences.client.screen.LawgiverScreen(target));
    }

    public static void receiveState(CodexStatePayload p) {
        state = p;
    }

    private EndgameClient() {}
}
