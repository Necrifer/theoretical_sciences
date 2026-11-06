package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.2.0: Folded Matter. What the Dodecaract Excavator and the Omnipotent Edge gather (a whole cube of rock, a whole
 * field of the dead) is folded into a few of these instead of filling the inventory and spilling hundreds of items onto
 * the ground, which on a big job was enough to stall a server (the same answer as Avaritia's Matter Cluster). Each holds
 * up to {@link #CAPACITY} items of up to {@link #KINDS} kinds. Use it to unfold it into your inventory: what does not
 * fit stays folded, so nothing is ever dropped.
 */
public class FoldedMatter extends Item {
    public static final int CAPACITY = 16384, KINDS = 64;
    private static final String KEY = "Fold";

    public FoldedMatter(Properties p) {
        super(p);
    }

    // ------------------------------------------------------------------ contents
    /** One kind: the item (a stack of one, with its components) and how many. */
    public record Entry(ItemStack kind, int count) {}

    public static List<Entry> contents(ItemStack fold, HolderLookup.Provider regs) {
        List<Entry> out = new ArrayList<>();
        CustomData d = fold.get(DataComponents.CUSTOM_DATA);
        if (d == null) return out;
        ListTag l = d.copyTag().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompound(i);
            ItemStack k = ItemStack.parseOptional(regs, c.getCompound("Item"));
            int n = c.getInt("N");
            if (!k.isEmpty() && n > 0) out.add(new Entry(k, n));
        }
        return out;
    }

    static void write(ItemStack fold, List<Entry> es, HolderLookup.Provider regs) {
        ListTag l = new ListTag();
        for (Entry e : es) {
            if (e.count() <= 0) continue;
            CompoundTag c = new CompoundTag();
            c.put("Item", e.kind().copyWithCount(1).save(regs));
            c.putInt("N", e.count());
            l.add(c);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, fold, t -> t.put(KEY, l));
    }

    static int total(List<Entry> es) {
        long n = 0;
        for (Entry e : es) n += e.count();
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    // ------------------------------------------------------------------ gathering
    /** Drops gathered during one job (or one sweep of the Edge), handed over as folds at the end. */
    public static final class Gatherer {
        private final List<Entry> kinds = new ArrayList<>();
        private long items;

        public void add(ItemStack s) {
            if (s.isEmpty()) return;
            items += s.getCount();
            for (int i = 0; i < kinds.size(); i++) {
                Entry e = kinds.get(i);
                if (ItemStack.isSameItemSameComponents(e.kind(), s)) {
                    kinds.set(i, new Entry(e.kind(), (int) Math.min(Integer.MAX_VALUE, (long) e.count() + s.getCount())));
                    return;
                }
            }
            kinds.add(new Entry(s.copyWithCount(1), s.getCount()));
        }

        public boolean isEmpty() {
            return kinds.isEmpty();
        }

        public long items() {
            return items;
        }

        public int kinds() {
            return kinds.size();
        }

        /** Every gathered item, packed into folds (as few as will hold them). */
        public List<ItemStack> fold(Item foldItem, HolderLookup.Provider regs) {
            List<ItemStack> out = new ArrayList<>();
            List<Entry> cur = new ArrayList<>();
            int curN = 0;
            for (Entry e : kinds) {
                int left = e.count();
                while (left > 0) {
                    if (cur.size() >= KINDS || curN >= CAPACITY) {
                        out.add(pack(foldItem, cur, regs));
                        cur = new ArrayList<>();
                        curN = 0;
                    }
                    int take = Math.min(left, CAPACITY - curN);
                    cur.add(new Entry(e.kind(), take));
                    curN += take;
                    left -= take;
                }
            }
            if (!cur.isEmpty()) out.add(pack(foldItem, cur, regs));
            kinds.clear();
            items = 0;
            return out;
        }

        private static ItemStack pack(Item foldItem, List<Entry> es, HolderLookup.Provider regs) {
            ItemStack f = new ItemStack(foldItem);
            write(f, es, regs);
            return f;
        }

        /**
         * Hands the gathered items to the player: a few items of few kinds go straight into the inventory, as before;
         * anything more is folded. A fold that does not fit lands at the player's feet: one item on the ground, not
         * hundreds.
         */
        public void deliver(ServerPlayer sp) {
            if (isEmpty()) return;
            if (kinds.size() <= 4 && items <= 4L * 64) {
                List<Entry> rest = new ArrayList<>();
                for (Entry e : kinds) {
                    int left = e.count();
                    while (left > 0) {
                        ItemStack s = e.kind().copyWithCount(Math.min(left, e.kind().getMaxStackSize()));
                        int n = s.getCount();
                        sp.getInventory().add(s);
                        left -= n - s.getCount();
                        if (!s.isEmpty()) break;
                    }
                    if (left > 0) rest.add(new Entry(e.kind(), left));
                }
                kinds.clear();
                items = 0;
                for (Entry e : rest) {
                    kinds.add(e);
                    items += e.count();
                }
                if (isEmpty()) return;
            }
            for (ItemStack f : fold(item(), sp.registryAccess())) {
                if (!sp.getInventory().add(f)) sp.drop(f, false, false);
            }
        }

        /** No player to give it to (they left): the folds are left where the work was. */
        public void dropAt(ServerLevel level, Vec3 at) {
            if (isEmpty()) return;
            for (ItemStack f : fold(item(), level.registryAccess()))
                net.minecraft.world.Containers.dropItemStack(level, at.x, at.y, at.z, f);
        }
    }

    static Item item() {
        return com.theoreticalsciences.registry.QHItems.get("folded_matter");
    }

    // ------------------------------------------------------------------ the item
    /** Unfolds into the inventory, as much as fits; the rest stays folded. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack fold = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(fold, level.isClientSide);
        List<Entry> es = contents(fold, level.registryAccess());
        List<Entry> left = new ArrayList<>();
        int moved = 0;
        for (Entry e : es) {
            int n = e.count();
            while (n > 0) {
                ItemStack s = e.kind().copyWithCount(Math.min(n, e.kind().getMaxStackSize()));
                int want = s.getCount();
                sp.getInventory().add(s);
                int took = want - s.getCount();
                n -= took;
                moved += took;
                if (took == 0) break;
            }
            if (n > 0) left.add(new Entry(e.kind(), n));
        }
        if (left.isEmpty()) fold.shrink(1);
        else write(fold, left, level.registryAccess());
        if (moved > 0) level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
        player.displayClientMessage(left.isEmpty() ? Component.translatable("message.theoreticalsciences.folded_matter.unfolded", moved).withStyle(ChatFormatting.LIGHT_PURPLE)
                : Component.translatable("message.theoreticalsciences.folded_matter.full", moved, total(left)).withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.sidedSuccess(fold, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        HolderLookup.Provider regs = ctx.registries();
        if (regs == null) return;
        List<Entry> es = contents(stack, regs);
        tip.add(Component.translatable("tooltip.theoreticalsciences.folded_matter.holds", total(es), es.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
        es.sort((a, b) -> Integer.compare(b.count(), a.count()));
        for (int i = 0; i < Math.min(8, es.size()); i++)
            tip.add(Component.literal(" " + es.get(i).count() + " x ").append(es.get(i).kind().getHoverName()).withStyle(ChatFormatting.GRAY));
        if (es.size() > 8) tip.add(Component.translatable("tooltip.theoreticalsciences.folded_matter.more", es.size() - 8).withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("tooltip.theoreticalsciences.folded_matter.use").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isFoil(ItemStack s) {
        return false;
    }
}
