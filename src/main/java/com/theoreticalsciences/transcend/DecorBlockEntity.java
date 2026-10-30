package com.theoreticalsciences.transcend;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No data: it only exists so the client knows where the miniatures are. */
public class DecorBlockEntity extends BlockEntity {
    public DecorBlockEntity(BlockPos pos, BlockState state) {
        super(TranscendRegistry.DECOR_BE.get(), pos, state);
    }

    public DecorBlock.Kind kind() {
        return getBlockState().getBlock() instanceof DecorBlock d ? d.kind : DecorBlock.Kind.TESSERACT;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.DECOR.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.DECOR.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.DECOR.remove(this);
    }
}
