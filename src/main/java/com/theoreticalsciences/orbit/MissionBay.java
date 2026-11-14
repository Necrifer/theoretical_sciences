package com.theoreticalsciences.orbit;

import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.12.0: the Mission Bay, a berth for uncrewed craft aboard the Orbital Station. Slot 0 holds a Sample Return Probe,
 * slots 1 to 3 Transfer Stages, slots 4 to 12 what comes back. A mission flies out and back on its transfer orbit
 * ({@link MissionMath#missionTicks}); the probe and the stages it burned are spent, and the samples wait in the bay.
 */
public final class MissionBay {
    private MissionBay() {}

    public static final int PROBE = 0, STAGE0 = 1, STAGES = 3, OUT0 = 4, OUTS = 9;

    static boolean isProbe(ItemStack s) {
        return !s.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals("sample_return_probe")
                && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(com.theoreticalsciences.TheoreticalSciences.MODID);
    }

    // ------------------------------------------------------------------ the block
    public static class BayBlock extends BaseEntityBlock {
        public static final com.mojang.serialization.MapCodec<BayBlock> CODEC = simpleCodec(BayBlock::new);

        public BayBlock(Properties p) {
            super(p);
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState s) {
            return RenderShape.MODEL;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BayEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide || type != SpaceRegistry.BAY.get() ? null : (l, p, s, be) -> ((BayEntity) be).tick((ServerLevel) l);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (held.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!(level.getBlockEntity(pos) instanceof BayEntity bay)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!isProbe(held) && MissionMath.stageDv(held) <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            ItemStack before = held.copy();
            ItemStack left = held.copy();
            if (isProbe(held)) left = bay.automation.insertItem(PROBE, left, false);
            else for (int i = STAGE0; i < STAGE0 + STAGES && !left.isEmpty(); i++) left = bay.automation.insertItem(i, left, false);
            if (left.getCount() != before.getCount()) {
                player.setItemInHand(hand, left);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.bay_loaded", before.getHoverName()).withStyle(ChatFormatting.AQUA), true);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 1.2F);
            }
            return ItemInteractionResult.CONSUME;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof BayEntity bay)) return InteractionResult.CONSUME;
            // samples first; then, sneaking, unload what has not flown; then the planner
            boolean took = false;
            for (int i = OUT0; i < OUT0 + OUTS; i++) {
                ItemStack s = bay.items.getStackInSlot(i);
                if (s.isEmpty()) continue;
                bay.items.setStackInSlot(i, ItemStack.EMPTY);
                if (!sp.getInventory().add(s)) sp.drop(s, false);
                took = true;
            }
            if (took) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.bay_collected").withStyle(ChatFormatting.GREEN), true);
                return InteractionResult.CONSUME;
            }
            if (sp.isShiftKeyDown() && bay.mission == null) {
                for (int i = PROBE; i < STAGE0 + STAGES; i++) {
                    ItemStack s = bay.items.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    bay.items.setStackInSlot(i, ItemStack.EMPTY);
                    if (!sp.getInventory().add(s)) sp.drop(s, false);
                }
                return InteractionResult.CONSUME;
            }
            if (level.dimension() != QHDimensions.ORBITAL_STATION) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.bay_grounded").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            bay.owner = sp.getUUID();
            bay.setChanged();
            Planner.openBay(sp, bay);
            return InteractionResult.CONSUME;
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moved) {
            if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof BayEntity bay)
                for (int i = 0; i < bay.items.getSlots(); i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bay.items.getStackInSlot(i));
            super.onRemove(state, level, pos, now, moved);
        }
    }

    // ------------------------------------------------------------------ the block entity
    public static class BayEntity extends BlockEntity {
        final ItemStackHandler items = new ItemStackHandler(OUT0 + OUTS) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack s) {
                if (slot == PROBE) return isProbe(s);
                if (slot < OUT0) return MissionMath.stageDv(s) > 0;
                return true;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == PROBE ? 1 : slot < OUT0 ? 3 : 64;
            }
        };
        /** What pipes see: they may load probes and stages, and take only samples out. */
        public final IItemHandler automation = new IItemHandler() {
            public int getSlots() {
                return items.getSlots();
            }

            public ItemStack getStackInSlot(int s) {
                return items.getStackInSlot(s);
            }

            public ItemStack insertItem(int s, ItemStack st, boolean sim) {
                return s < OUT0 ? items.insertItem(s, st, sim) : st;
            }

            public ItemStack extractItem(int s, int n, boolean sim) {
                return s >= OUT0 ? items.extractItem(s, n, sim) : ItemStack.EMPTY;
            }

            public int getSlotLimit(int s) {
                return items.getSlotLimit(s);
            }

            public boolean isItemValid(int s, ItemStack st) {
                return s < OUT0 && items.isItemValid(s, st);
            }
        };
        @Nullable ResourceLocation mission;
        long returnAt;
        @Nullable UUID owner;

        public BayEntity(BlockPos pos, BlockState state) {
            super(SpaceRegistry.BAY.get(), pos, state);
        }

        public boolean hasProbe() {
            return isProbe(items.getStackInSlot(PROBE));
        }

        public List<ItemStack> stages() {
            List<ItemStack> out = new ArrayList<>();
            for (int i = STAGE0; i < STAGE0 + STAGES; i++) out.add(items.getStackInSlot(i));
            return out;
        }

        public int busyTicks() {
            return mission == null || level == null ? 0 : (int) Math.max(0, returnAt - level.getGameTime());
        }

        /** Spends the probe and the stages of the plan, and sends the probe. */
        void launch(Missions.Mission m, MissionMath.Plan plan, double tof) {
            items.extractItem(PROBE, 1, false);
            for (int slot : plan.slots()) items.extractItem(STAGE0 + slot, 1, false);
            mission = m.id();
            returnAt = level.getGameTime() + MissionMath.missionTicks(m.transferDays(), tof);
            setChanged();
            level.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.BLOCKS, 1.0F, 1.5F);
        }

        void tick(ServerLevel level) {
            if (mission == null || level.getGameTime() < returnAt || level.getGameTime() % 20 != 0) return;
            Missions.Mission m = Missions.INSTANCE.byId(mission);
            if (m != null)
                for (ItemStack s : m.samples()) {
                    ItemStack left = s.copy();
                    for (int i = OUT0; i < OUT0 + OUTS && !left.isEmpty(); i++) left = items.insertItem(i, left, false);
                    if (!left.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, left);
                }
            if (owner != null && level.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.bay_returned",
                        Component.translatable(m == null ? mission.toString() : m.name())).withStyle(ChatFormatting.GREEN), false);
            level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
            mission = null;
            setChanged();
        }

        @Override
        protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
            super.saveAdditional(t, r);
            t.put("Items", items.serializeNBT(r));
            if (mission != null) {
                t.putString("Mission", mission.toString());
                t.putLong("ReturnAt", returnAt);
            }
            if (owner != null) t.putUUID("Owner", owner);
        }

        @Override
        protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
            super.loadAdditional(t, r);
            if (t.contains("Items")) items.deserializeNBT(r, t.getCompound("Items"));
            mission = t.contains("Mission") ? ResourceLocation.tryParse(t.getString("Mission")) : null;
            returnAt = t.getLong("ReturnAt");
            owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
        }
    }
}
