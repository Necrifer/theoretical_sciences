package com.theoreticalsciences.gr;

import com.theoreticalsciences.item.BraneShifterItem;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Takes you across an event horizon - into the black hole - and back out (which should be impossible). */
public class HorizonDiverItem extends BraneShifterItem {
    public HorizonDiverItem(Properties p) {
        super(p);
    }

    @Override
    protected ResourceKey<Level> destination() {
        return QHDimensions.BLACK_HOLE;
    }

    @Override
    protected String researchId() {
        return "black_hole_interior";
    }

    @Override
    protected String platformBlock() {
        return "infall_rock";
    }

    @Override
    protected String coreBlock() {
        return "einstein_rosen_bridge";
    }

    @Override
    protected void onArrive(ServerPlayer sp, ServerLevel target) {
        if (target.dimension() != QHDimensions.BLACK_HOLE) return;
        BridgeBuilder.ensureBuilt(target);
        ServerLevel wh = sp.server.getLevel(QHDimensions.WHITE_HOLE);
        if (wh != null) BridgeBuilder.ensureBuilt(wh);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.black_hole_arrival").withStyle(ChatFormatting.GOLD), false);
    }
}
