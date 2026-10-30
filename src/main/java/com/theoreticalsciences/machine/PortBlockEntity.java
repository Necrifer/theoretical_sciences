package com.theoreticalsciences.machine;

import com.theoreticalsciences.registry.QHBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

public class PortBlockEntity extends BlockEntity {
    @Nullable private BlockPos controllerPos;
    /** Its AE2 node (compat.ae2.Ae2Link) when AE2 is installed; null otherwise. */
    @Nullable private Object me;
    @Nullable private CompoundTag meSaved;

    public PortBlockEntity(BlockPos pos, BlockState state) {
        super(QHBlockEntities.PORT.get(), pos, state);
    }

    void setController(@Nullable BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    @Nullable
    public ControllerBlockEntity controller() {
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) return null;
        if (level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity c && c.isFormed() && c.ports().contains(worldPosition)) return c;
        return null;
    }

    @Nullable
    public Object meLink() {
        return me;
    }

    /** The ME network this port is cabled to, if it is online. */
    @Nullable
    public com.theoreticalsciences.compat.MEAccess network() {
        return me instanceof com.theoreticalsciences.compat.MEAccess a && a.online() ? a : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && me == null) me = com.theoreticalsciences.compat.MEBridge.attach(this, meSaved);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        com.theoreticalsciences.compat.MEBridge.detach(me);
        me = null;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        com.theoreticalsciences.compat.MEBridge.detach(me);
        me = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (controllerPos != null) tag.putLong("Controller", controllerPos.asLong());
        if (me != null) com.theoreticalsciences.compat.MEBridge.save(me, tag);
        else if (meSaved != null) tag.put("ME", meSaved);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
        meSaved = tag.contains("ME") ? tag.getCompound("ME") : null;
    }
}
