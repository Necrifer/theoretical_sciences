package com.theoreticalsciences.science;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * 1.2.0: a Lead Pig - the shielded pot Technetium-99m is carried in. Technetium-99m decays only while it is out in the
 * open (carried loose, or on the ground); in here it waits, as it does in a machine, an ME network or a Chronostatic
 * Vault. Holds up to {@link #CAPACITY} vials and nothing else.
 * <p>
 * Right-click it with vials on the cursor to put them in, or right-click a stack of vials with it on the cursor;
 * right-click it with an empty cursor to take the vials out (onto the cursor), or right-click an empty slot with it.
 * Vials put in at different times share one age: the average of theirs, weighed by how many there are.
 */
public class LeadPigItem extends Item {
    public static final int CAPACITY = 64;

    public record Contents(int count, long age) {
        public static final Contents EMPTY = new Contents(0, 0);
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("count").forGetter(Contents::count),
                Codec.LONG.optionalFieldOf("age", 0L).forGetter(Contents::age)).apply(i, Contents::new));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Contents> STREAM = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Contents::count, ByteBufCodecs.VAR_LONG, Contents::age, Contents::new);
    }

    public LeadPigItem(Properties p) {
        super(p.stacksTo(1));
    }

    static Contents contents(ItemStack pig) {
        return pig.getOrDefault(Wear.PIG.get(), Contents.EMPTY);
    }

    static Item vial() {
        return BuiltInRegistries.ITEM.get(TheoreticalSciences.id("technetium_99m_vial"));
    }

    static boolean accepts(ItemStack s) {
        Wear.Profile p = Wear.profile(s);
        return p != null && p.exposed();
    }

    /** Puts as many of these vials in as fit; returns how many went in. */
    static int insert(ItemStack pig, ItemStack vials) {
        if (!accepts(vials)) return 0;
        Contents c = contents(pig);
        int n = Math.min(vials.getCount(), CAPACITY - c.count());
        if (n <= 0) return 0;
        long age = Wear.age(vials, 0);
        long merged = (c.age() * c.count() + age * n) / (c.count() + n);
        pig.set(Wear.PIG.get(), new Contents(c.count() + n, merged));
        vials.shrink(n);
        return n;
    }

    /** Takes up to {@code max} vials out, held at their age. */
    static ItemStack extract(ItemStack pig, int max) {
        Contents c = contents(pig);
        int n = Math.min(max, c.count());
        if (n <= 0) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(vial(), n);
        out.set(Wear.CLOCK.get(), new Wear.Clock(0, c.age(), true));
        if (c.count() - n == 0) pig.remove(Wear.PIG.get());
        else pig.set(Wear.PIG.get(), new Contents(c.count() - n, c.age()));
        return out;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pig, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack there = slot.getItem();
        if (there.isEmpty()) {
            ItemStack out = extract(pig, CAPACITY);
            if (out.isEmpty()) return false;
            ItemStack rest = slot.safeInsert(out);
            if (!rest.isEmpty()) insert(pig, rest);
        } else if (accepts(there)) {
            ItemStack taken = slot.safeTake(there.getCount(), CAPACITY - contents(pig).count(), player);
            insert(pig, taken);
            if (!taken.isEmpty()) slot.safeInsert(taken);
        } else return false;
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.6F);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pig, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess cursor) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty()) {
            ItemStack out = extract(pig, CAPACITY);
            if (out.isEmpty()) return false;
            cursor.set(out);
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.6F);
            return true;
        }
        if (!accepts(other)) return false;
        if (insert(pig, other) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.6F);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack pig) {
        return contents(pig).count() > 0;
    }

    @Override
    public int getBarWidth(ItemStack pig) {
        return Math.round(13F * contents(pig).count() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack pig) {
        return 0x80FFE0;
    }

    @Override
    public void appendHoverText(ItemStack pig, TooltipContext ctx, List<Component> out, TooltipFlag flag) {
        Contents c = contents(pig);
        if (c.count() <= 0) {
            out.add(Component.translatable("tooltip.theoreticalsciences.lead_pig.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack probe = extract(pig.copy(), 1);
        double a = Wear.activity(probe, 0);
        out.add(Component.translatable("tooltip.theoreticalsciences.lead_pig.holds", c.count(), CAPACITY,
                String.format(Locale.ROOT, "%.0f%%", a * 100)).withStyle(ChatFormatting.DARK_AQUA));
    }
}
