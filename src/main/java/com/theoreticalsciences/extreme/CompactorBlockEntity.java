package com.theoreticalsciences.extreme;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * Slot 0 = input (absorbed into a counter every tick, as fast as it arrives), slot 1 = output.
 * Once a material is started the compactor is locked to that recipe until the singularity pops out.
 */
public class CompactorBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler slots = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0;
        }
    };
    ResourceLocation target;     // recipe id currently being filled
    int absorbed;
    int required;

    /** For Jade: how much of the singularity being made has been fed in. */
    public int absorbed() {
        return absorbed;
    }

    public int required() {
        return required;
    }
    int resultItemId = -1;

    /** What pipes / AE2 see: insert into the input only, extract from the output only. */
    public final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int s) {
            return slots.getStackInSlot(s);
        }

        @Override
        public ItemStack insertItem(int s, ItemStack stack, boolean sim) {
            if (s != 0 || stack.isEmpty() || !accepts(stack)) return stack;
            // Straight into the counter, as much as the singularity still needs - not limited to one stack, so an AE2
            // pattern pushing all 4,096 (or 65,536) at once fits. Whatever is waiting in the input slot counts first.
            ItemStack waiting = slots.getStackInSlot(0);
            if (!waiting.isEmpty() && !ItemStack.isSameItemSameComponents(waiting, stack)) return stack;
            int need;
            CompactionRecipe r = null;
            ResourceLocation id = target;
            if (target != null) {
                need = required - absorbed;
            } else {
                var h = find(stack);
                if (h.isEmpty()) return stack;
                id = h.get().id();
                r = h.get().value();
                need = r.count();
            }
            need -= waiting.getCount();
            if (need <= 0) return stack;
            int take = Math.min(need, stack.getCount());
            if (!sim) {
                if (target == null) {
                    target = id;
                    required = r.count();
                    resultItemId = BuiltInRegistries.ITEM.getId(r.result().getItem());
                }
                absorbed += take;
                setChanged();
            }
            return take >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - take);
        }

        @Override
        public ItemStack extractItem(int s, int amount, boolean sim) {
            return s == 1 ? slots.extractItem(1, amount, sim) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int s) {
            return s == 0 ? Integer.MAX_VALUE : 64;
        }

        @Override
        public boolean isItemValid(int s, ItemStack stack) {
            return s == 0 && accepts(stack);
        }
    };

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> absorbed & 0xFFFF;
                case 1 -> absorbed >>> 16;
                case 2 -> required & 0xFFFF;
                case 3 -> required >>> 16;
                // the result item's number, in two 16-bit halves like the counts above (one half alone breaks past
                // 65,535 registered items, which big modpacks reach)
                case 4 -> (resultItemId + 1) & 0xFFFF;
                case 5 -> (resultItemId + 1) >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    public CompactorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtremeRegistry.COMPACTOR_BE.get(), pos, state);
    }

    Optional<RecipeHolder<CompactionRecipe>> find(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        var r = level.getRecipeManager().getRecipeFor(ExtremeRegistry.COMPACTION_TYPE.get(), new SingleRecipeInput(stack), level);
        if (r.isPresent()) return r;
        // the singularities packs add, and the automatic ingot ones (round 21)
        for (Singularities.Def d : Singularities.all(level.isClientSide)) {
            if (!d.ingredient().test(stack)) continue;
            ResourceLocation id = ResourceLocation.tryParse(d.id());
            if (id != null) return Optional.of(new RecipeHolder<>(id, new CompactionRecipe(d.ingredient(), d.count(), Singularities.stack(d))));
        }
        return Optional.empty();
    }

    /** The recipe being filled: a real one, or a pack / automatic singularity. */
    @javax.annotation.Nullable
    CompactionRecipe recipe(ResourceLocation id) {
        if (level == null || id == null) return null;
        var h = level.getRecipeManager().byKey(id);
        if (h.isPresent() && h.get().value() instanceof CompactionRecipe r) return r;
        Singularities.Def d = Singularities.byId(id.toString(), level.isClientSide);
        return d == null ? null : new CompactionRecipe(d.ingredient(), d.count(), Singularities.stack(d));
    }

    boolean accepts(ItemStack stack) {
        if (level == null) return false;
        if (target != null) {
            CompactionRecipe r = recipe(target);
            return r != null && r.ingredient().test(stack);
        }
        return find(stack).isPresent();
    }

    void serverTick() {
        if (level == null) return;
        CompactionRecipe recipe = null;
        if (target != null) {
            recipe = recipe(target);
            if (recipe == null) {
                // the singularity it was making is gone (pack removed, blacklisted...): keep what was absorbed and
                // wait - it carries on if it comes back. Only an empty one starts afresh.
                if (absorbed > 0) return;
                reset();
            }
        }
        ItemStack in = slots.getStackInSlot(0);
        if (!in.isEmpty()) {
            if (recipe == null) {
                var h = find(in);
                if (h.isPresent()) {
                    target = h.get().id();
                    recipe = h.get().value();
                    required = recipe.count();
                    resultItemId = BuiltInRegistries.ITEM.getId(recipe.result().getItem());
                }
            }
            if (recipe != null && recipe.ingredient().test(in) && absorbed < required) {
                int take = Math.min(in.getCount(), required - absorbed);
                slots.extractItem(0, take, false);
                absorbed += take;
                setChanged();
            }
        }
        if (recipe != null && absorbed >= required) {
            ItemStack out = recipe.result().copy();
            ItemStack cur = slots.getStackInSlot(1);
            boolean room = cur.isEmpty() || (ItemStack.isSameItemSameComponents(cur, out) && cur.getCount() + out.getCount() <= cur.getMaxStackSize());
            if (room) {      // (insertItem would refuse: the output slot is not "valid" for insertion from outside)
                slots.setStackInSlot(1, cur.isEmpty() ? out : cur.copyWithCount(cur.getCount() + out.getCount()));
                reset();
            }
        }
    }

    private void reset() {
        target = null;
        absorbed = 0;
        required = 0;
        resultItemId = -1;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences.singularity_compactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CompactorMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Slots", slots.serializeNBT(regs));
        if (target != null) tag.putString("Target", target.toString());
        tag.putInt("Absorbed", absorbed);
        tag.putInt("Required", required);
        tag.putInt("ResultId", resultItemId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        slots.deserializeNBT(regs, tag.getCompound("Slots"));
        target = tag.contains("Target") ? ResourceLocation.tryParse(tag.getString("Target")) : null;
        absorbed = tag.getInt("Absorbed");
        required = tag.getInt("Required");
        resultItemId = tag.contains("ResultId") ? tag.getInt("ResultId") : -1;
    }
}
