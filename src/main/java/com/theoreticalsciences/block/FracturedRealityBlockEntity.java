package com.theoreticalsciences.block;

import com.theoreticalsciences.registry.QHBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Remembers the block that was fractured, how far the fracture has spread, and when reality will heal. */
public class FracturedRealityBlockEntity extends BlockEntity {
    private BlockState original = Blocks.STONE.defaultBlockState();
    private int generation, maxGeneration = 2, age, lifetime = 12000, spreads, nextSpread = 200;

    public FracturedRealityBlockEntity(BlockPos pos, BlockState state) {
        super(QHBlockEntities.FRACTURED_REALITY.get(), pos, state);
    }

    public BlockState original() {
        return original;
    }

    void init(BlockState original, int generation, int maxGeneration) {
        this.original = original;
        this.generation = generation;
        this.maxGeneration = maxGeneration;
        this.lifetime = 9000 + (level != null ? level.random.nextInt(6000) : 0) - generation * 1200;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FracturedRealityBlockEntity be) {
        ServerLevel sl = (ServerLevel) level;
        be.age++;
        if (be.age >= be.lifetime) {                       // reality heals
            level.setBlock(pos, be.original, 3);
            return;
        }
        if (--be.nextSpread <= 0) {
            be.nextSpread = 300 + level.random.nextInt(600);
            if (be.generation < be.maxGeneration && be.spreads < 2) {
                Direction d = Direction.getRandom(level.random);
                BlockPos n = pos.relative(d);
                if (sl.isLoaded(n) && FracturedRealityBlock.fracturable(sl, n)) {      // (never into an unloaded chunk)
                    FracturedRealityBlock.fracture(sl, n, be.generation + 1, be.maxGeneration);
                    be.spreads++;
                    be.setChanged();
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.FRACTURES.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.theoreticalsciences.client.ClientHooks.FRACTURES.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Original", NbtUtils.writeBlockState(original));
        tag.putInt("Gen", generation);
        tag.putInt("MaxGen", maxGeneration);
        tag.putInt("Age", age);
        tag.putInt("Life", lifetime);
        tag.putInt("Spreads", spreads);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("Original")) original = NbtUtils.readBlockState(regs.lookupOrThrow(Registries.BLOCK), tag.getCompound("Original"));
        generation = tag.getInt("Gen");
        maxGeneration = tag.getInt("MaxGen");
        age = tag.getInt("Age");
        lifetime = tag.contains("Life") ? tag.getInt("Life") : 12000;
        spreads = tag.getInt("Spreads");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = new CompoundTag();
        tag.put("Original", NbtUtils.writeBlockState(original));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
