package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Client -> server: the player clicked a tank in a machine screen while holding a fluid container on the cursor.
 * A full container is poured into an input tank; an empty one is filled from the clicked tank (input or output).
 */
public record TankClickPayload(int tank) implements CustomPacketPayload {
    public static final Type<TankClickPayload> TYPE = new Type<>(TheoreticalSciences.id("tank_click"));
    public static final StreamCodec<ByteBuf, TankClickPayload> CODEC = ByteBufCodecs.VAR_INT.map(TankClickPayload::new, TankClickPayload::tank);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPlayer player, int tankIndex) {
        if (!(player.containerMenu instanceof MachineMenu menu)) return;
        var be = menu.be;
        FluidTank tank = be.fluidCap.tank(tankIndex);       // recipe tanks, then a physics machine's coolant / spent coolant
        if (tank == null) return;
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        ItemStack one = carried.copyWithCount(1);
        boolean input = be.fluidCap.isInputTank(tankIndex);
        FluidActionResult r = FluidActionResult.FAILURE;
        if (input) r = FluidUtil.tryEmptyContainer(one, tank, Integer.MAX_VALUE, player, true);
        if (!r.isSuccess()) r = FluidUtil.tryFillContainer(one, tank, Integer.MAX_VALUE, player, true);
        if (!r.isSuccess()) return;
        ItemStack result = r.getResult();
        carried.shrink(1);
        if (carried.isEmpty()) menu.setCarried(result);
        else if (!player.getInventory().add(result)) player.drop(result, false);
        menu.broadcastChanges();
    }
}
