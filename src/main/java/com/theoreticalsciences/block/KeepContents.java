package com.theoreticalsciences.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Round 29: blocks that store things which cannot be dropped as items (fluids, energy, absorbed parts) keep them in
 * their own dropped item, like a shulker box. Placing that item again puts everything back.
 */
public final class KeepContents {
    /**
     * Called from a block's getDrops. {@code data} turns the block entity into the saved data to keep, or returns null
     * when there is nothing worth keeping (then the drops are left as they are).
     */
    public static <T extends BlockEntity> List<ItemStack> drops(Block block, List<ItemStack> drops, LootParams.Builder params,
                                                                 Class<T> type, Function<T, CompoundTag> data) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!type.isInstance(be)) return drops;
        CompoundTag tag = data.apply(type.cast(be));
        if (tag == null) return drops;
        List<ItemStack> out = new ArrayList<>(drops);
        ItemStack mine = null;
        for (ItemStack s : out) if (s.is(block.asItem())) mine = s;
        // even if the loot table gave nothing (explosion, wrong tool) the contents must not vanish
        if (mine == null) out.add(mine = new ItemStack(block));
        BlockItem.setBlockEntityData(mine, be.getType(), tag);
        return out;
    }

    /** Called from playerWillDestroy: creative breaking drops nothing, but a block holding contents still drops itself. */
    public static <T extends BlockEntity> void creative(Block block, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                         Class<T> type, Function<T, CompoundTag> data) {
        if (level.isClientSide || !player.isCreative()) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!type.isInstance(be)) return;
        CompoundTag tag = data.apply(type.cast(be));
        if (tag == null) return;
        ItemStack s = new ItemStack(block);
        BlockItem.setBlockEntityData(s, be.getType(), tag);
        ItemEntity e = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        e.setDefaultPickUpDelay();
        level.addFreshEntity(e);
    }

    private KeepContents() {}
}
