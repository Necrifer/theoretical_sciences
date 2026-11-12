package com.theoreticalsciences.home;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Empyrean Assembly Table: a 15 by 15 picture whose slots each take far more than a stack.
 *
 * <p>What it holds is counted per item, not per slot: slots that ask for the same item are filled from the one count,
 * in reading order. So there are no 225 stacks to keep in step between server and screen (which is what makes very
 * large crafting grids slow to open): the screen is sent the chosen recipe and a few numbers.
 */
public class EmpyreanTableBlockEntity extends BlockEntity implements MenuProvider {
    /** Room for this many crafts' worth of each ingredient, so pipes can run ahead of the crafting. */
    private static final int BUFFER_CRAFTS = 64;
    private static final long OUT_CAP = 1_000_000_000_000L;

    @Nullable private ResourceLocation recipeId;
    private final Map<Item, Long> held = new LinkedHashMap<>();
    private Item outItem = Items.AIR;
    private long outCount;
    private boolean auto;
    @Nullable private UUID owner;
    /** Bumped on every change: an open screen is sent the table again when it differs from what it last saw. */
    private int version;

    public EmpyreanTableBlockEntity(BlockPos pos, BlockState state) {
        super(HomeRegistry.TABLE_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ state
    public int version() {
        return version;
    }

    private void changed() {
        version++;
        setChanged();
    }

    public void setOwner(@Nullable UUID id) {
        owner = id;
        setChanged();
    }

    @Nullable
    public ResourceLocation recipeId() {
        return recipeId;
    }

    @Nullable
    public EmpyreanRecipe recipe() {
        if (recipeId == null || level == null) return null;
        return level.getRecipeManager().byKey(recipeId).map(RecipeHolder::value).filter(r -> r instanceof EmpyreanRecipe).map(r -> (EmpyreanRecipe) r).orElse(null);
    }

    public Map<Item, Long> held() {
        return held;
    }

    public Item outItem() {
        return outItem;
    }

    public long outCount() {
        return outCount;
    }

    public boolean auto() {
        return auto;
    }

    public void toggleAuto() {
        auto = !auto;
        changed();
    }

    /** Chooses a recipe. What the table holds stays in it (and can be taken back out) even if the new recipe has no use for it. */
    public void select(@Nullable ResourceLocation id) {
        if (level != null && id != null && level.getRecipeManager().byKey(id).filter(h -> h.value() instanceof EmpyreanRecipe).isEmpty()) return;
        recipeId = id;
        changed();
    }

    // ------------------------------------------------------------------ in and out
    /** How many more of this item the table will take now. */
    public long room(Item item) {
        EmpyreanRecipe r = recipe();
        if (r == null) return 0;
        Long need = r.totals().get(item);
        if (need == null) return 0;
        return Math.max(0, need * BUFFER_CRAFTS - held.getOrDefault(item, 0L));
    }

    /** Puts in as many as fit; returns how many went in. */
    public long put(Item item, long count) {
        long n = Math.min(count, room(item));
        if (n > 0) {
            held.merge(item, n, Long::sum);
            changed();
        }
        return n;
    }

    /** Takes back up to {@code count} of an ingredient. */
    public long takeBack(Item item, long count) {
        long have = held.getOrDefault(item, 0L);
        long n = Math.min(count, have);
        if (n > 0) {
            if (have == n) held.remove(item);
            else held.put(item, have - n);
            changed();
        }
        return n;
    }

    /** Takes up to {@code count} of the product. */
    public long takeOut(long count) {
        long n = Math.min(count, outCount);
        if (n > 0) {
            outCount -= n;
            if (outCount == 0) outItem = Items.AIR;
            changed();
        }
        return n;
    }

    /** Null if the chosen recipe can be made now, else why not (a translation key's last part). */
    @Nullable
    public String blocked() {
        EmpyreanRecipe r = recipe();
        if (r == null) return "no_recipe";
        if (r.needsHome && (level == null || level.getServer() == null || !HomeProgress.complete(level.getServer(), owner))) return "needs_home";
        if (outCount > 0 && outItem != r.result.item()) return "output_other";
        if (outCount + r.result.count() > OUT_CAP) return "output_full";
        for (Map.Entry<Item, Long> e : r.totals().entrySet())
            if (held.getOrDefault(e.getKey(), 0L) < e.getValue()) return "missing";
        return null;
    }

    /** Makes the recipe up to {@code times} times; returns how many were made. */
    public int craft(int times) {
        int made = 0;
        EmpyreanRecipe r = recipe();
        while (made < times && r != null && blocked() == null) {
            for (Map.Entry<Item, Long> e : r.totals().entrySet()) {
                long left = held.get(e.getKey()) - e.getValue();
                if (left == 0) held.remove(e.getKey());
                else held.put(e.getKey(), left);
            }
            outItem = r.result.item();
            outCount += r.result.count();
            made++;
        }
        if (made > 0) {
            changed();
            if (level instanceof ServerLevel sl)
                sl.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.7F);
        }
        return made;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, EmpyreanTableBlockEntity be) {
        if (be.auto && level.getGameTime() % 10 == 0) be.craft(64);
    }

    // ------------------------------------------------------------------ pipes and hoppers
    /**
     * For automation: one slot per ingredient of the chosen recipe (it shows up to a stack of what is held), then one
     * slot for the product. Anything inserted goes to its ingredient whichever slot it was offered to.
     */
    public final IItemHandler automation = new IItemHandler() {
        private List<Item> wanted() {
            EmpyreanRecipe r = recipe();
            return r == null ? List.of() : new ArrayList<>(r.totals().keySet());
        }

        @Override
        public int getSlots() {
            return wanted().size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            List<Item> w = wanted();
            if (slot == w.size()) return outCount > 0 ? new ItemStack(outItem, (int) Math.min(outCount, outItem.getDefaultMaxStackSize())) : ItemStack.EMPTY;
            if (slot < 0 || slot > w.size()) return ItemStack.EMPTY;
            long h = held.getOrDefault(w.get(slot), 0L);
            return h > 0 ? new ItemStack(w.get(slot), (int) Math.min(h, w.get(slot).getDefaultMaxStackSize())) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) return stack;      // plain items only: the table keeps counts, not stacks
            long n = Math.min(stack.getCount(), room(stack.getItem()));
            if (n <= 0) return stack;
            if (!simulate) put(stack.getItem(), n);
            return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) n);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != wanted().size() || outCount <= 0 || amount <= 0) return ItemStack.EMPTY;
            int n = (int) Math.min(Math.min(amount, outItem.getDefaultMaxStackSize()), outCount);
            ItemStack out = new ItemStack(outItem, n);
            if (!simulate) takeOut(n);
            return out;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getComponentsPatch().isEmpty() && room(stack.getItem()) > 0;
        }
    };

    // ------------------------------------------------------------------ menu
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences.empyrean_assembly_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EmpyreanTableMenu(id, inv, this);
    }

    /** Whether anything is in it (a table that is broken keeps its contents in the item, since they cannot be dropped). */
    public boolean holdsAnything() {
        return !held.isEmpty() || outCount > 0;
    }

    // ------------------------------------------------------------------ save
    public CompoundTag write(CompoundTag tag) {
        if (recipeId != null) tag.putString("Recipe", recipeId.toString());
        ListTag list = new ListTag();
        held.forEach((item, n) -> {
            CompoundTag t = new CompoundTag();
            t.putString("Item", BuiltInRegistries.ITEM.getKey(item).toString());
            t.putLong("Count", n);
            list.add(t);
        });
        tag.put("Held", list);
        if (outCount > 0) {
            tag.putString("Out", BuiltInRegistries.ITEM.getKey(outItem).toString());
            tag.putLong("OutCount", outCount);
        }
        tag.putBoolean("Auto", auto);
        if (owner != null) tag.putUUID("Owner", owner);
        return tag;
    }

    public void read(CompoundTag tag) {
        recipeId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        held.clear();
        for (Tag raw : tag.getList("Held", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            ResourceLocation id = ResourceLocation.tryParse(t.getString("Item"));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id) && t.getLong("Count") > 0) held.put(BuiltInRegistries.ITEM.get(id), t.getLong("Count"));
        }
        ResourceLocation out = tag.contains("Out") ? ResourceLocation.tryParse(tag.getString("Out")) : null;
        outItem = out != null && BuiltInRegistries.ITEM.containsKey(out) ? BuiltInRegistries.ITEM.get(out) : Items.AIR;
        outCount = outItem == Items.AIR ? 0 : tag.getLong("OutCount");
        auto = tag.getBoolean("Auto");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        version++;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        read(tag);
    }
}
