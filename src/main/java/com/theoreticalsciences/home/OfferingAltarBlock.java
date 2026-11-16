package com.theoreticalsciences.home;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The Offering Altar (1.8.0): what is piped into it is given to the Core, on the checklist of the player who placed it.
 * Thirty-three million of anything cannot be carried to the Core by hand.
 */
public class OfferingAltarBlock extends BaseEntityBlock {
    public static final MapCodec<OfferingAltarBlock> CODEC = simpleCodec(OfferingAltarBlock::new);

    public OfferingAltarBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player p && level.getBlockEntity(pos) instanceof Entity be) be.setOwner(p.getUUID());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof Entity be) {
            if (be.owner == null) be.setOwner(sp.getUUID());
            if (!be.owner.equals(sp.getUUID())) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.offering_altar.not_yours").withStyle(ChatFormatting.RED), true);
            } else if (!HomeProgress.unlocked(sp.server, sp.getUUID())) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.offering_altar.silent").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            } else {
                long n = HomeProgress.submitInventory(sp);            // by hand too: everything carried that is still needed
                long[] g = HomeProgress.given(sp.server, sp.getUUID());
                int full = 0;
                for (long v : g) if (v >= HomeMaterials.TARGET) full++;
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.offering_altar.status", String.format(java.util.Locale.ROOT, "%,d", n), full, g.length)
                        .withStyle(ChatFormatting.GOLD), true);
                HomePayloads.announceIfComplete(sp);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static class Entity extends BlockEntity {
        @Nullable UUID owner;

        public Entity(BlockPos pos, BlockState state) {
            super(HomeRegistry.ALTAR_BE.get(), pos, state);
        }

        void setOwner(UUID id) {
            owner = id;
            setChanged();
        }

        private long wanted(ItemStack stack) {
            if (owner == null || level == null || level.getServer() == null || stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) return 0;
            int idx = HomeMaterials.index(stack.getItem());
            if (idx < 0 || !HomeProgress.unlocked(level.getServer(), owner)) return 0;
            return Math.max(0, HomeMaterials.TARGET - HomeProgress.given(level.getServer(), owner)[idx]);
        }

        /** Takes only the forty-two materials, and only as many as the owner's checklist still needs. Nothing comes out. */
        public final IItemHandler automation = new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                long n = Math.min(stack.getCount(), wanted(stack));
                if (n <= 0) return stack;
                if (!simulate) {
                    HomeProgress.give(level.getServer(), owner, HomeMaterials.index(stack.getItem()), n);
                    if (level.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer sp) HomePayloads.announceIfComplete(sp);
                }
                return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) n);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return wanted(stack) > 0;
            }
        };

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
            super.saveAdditional(tag, regs);
            if (owner != null) tag.putUUID("Owner", owner);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
            super.loadAdditional(tag, regs);
            owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        }
    }
}
