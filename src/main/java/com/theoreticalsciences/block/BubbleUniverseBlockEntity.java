package com.theoreticalsciences.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Carries no data; it exists so the client knows where to draw a bubble universe's galaxy. */
public class BubbleUniverseBlockEntity extends BlockEntity {
    public BubbleUniverseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.BUBBLES.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.BUBBLES.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.BUBBLES.remove(this);
    }
}
