package com.theoreticalsciences.science;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.2.0: catalysts and instruments that wear.
 * <p>
 * Which items wear, and how, is the data map {@code theoreticalsciences:wear} (data/theoreticalsciences/data_maps/item/wear.json),
 * synced to clients so tooltips can show it:
 * <ul>
 *   <li><b>runs</b> (catalysts, lenses, magnets, crystals, chips): each recipe run the item serves as a catalyst for uses one
 *       run of its life ({@code life}); a run while the machine's physics shows a "wear" condition (too hot, saturating...)
 *       uses {@link #HOT_RUNS}. At the end of its life one item is worn out: a restorable one comes out of the machine
 *       marked worn (its restoration recipe makes it new), the rest are used up.</li>
 *   <li><b>source</b> (radioactive sources): activity falls by the half-life on the world's clock (the same scaled clock as
 *       the Decay Chamber), wherever the item is - except in a Chronostatic Vault, which holds its clock still. Below
 *       {@link #SPENT} of its first activity a source no longer works.</li>
 * </ul>
 * A worn or spent item never counts as a recipe's catalyst (see QHRecipe.matches).
 */
public final class Wear {
    public static final int HOT_RUNS = 10;
    /** 1.2.0: what a run on an unassayed feed costs a catalyst that can be poisoned (sulfur, lead or water on its sites). */
    public static final int POISON_RUNS = 5;
    public static final double SPENT = 1.0 / 16;
    /** Seconds of decay per real second: the Decay Chamber's clock (ScienceMachineBlockEntity.DECAY_SCALE). */
    public static final double DECAY_SCALE = 1440;
    /** Decay stamps are kept to the start of a game day, so sources made on the same day still stack. */
    public static final long STAMP_STEP = 24000;

    /** {@code poison}: an unassayed feed (see {@link #ASSAYED}) costs it {@link #POISON_RUNS} more runs. */
    public record Profile(String kind, int life, double halfLife, String restore, boolean poison) {
        public static final Codec<Profile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("kind").forGetter(Profile::kind),
                Codec.INT.optionalFieldOf("life", 0).forGetter(Profile::life),
                Codec.DOUBLE.optionalFieldOf("half_life", 0.0).forGetter(Profile::halfLife),
                Codec.STRING.optionalFieldOf("restore", "").forGetter(Profile::restore),
                Codec.BOOL.optionalFieldOf("poison", false).forGetter(Profile::poison)).apply(i, Profile::new));

        /** Decays on the world's clock (kind "source"), or only while exposed (kind "exposed": Technetium-99m). */
        public boolean source() {
            return "source".equals(kind) || exposed();
        }

        /** Decays only while carried loose by a player or lying on the ground; waits anywhere it is stored. */
        public boolean exposed() {
            return "exposed".equals(kind) || living();
        }

        /** 1.2.0: a living culture: it dies off as an exposed source decays, and once dead it is no use to any recipe. */
        public boolean living() {
            return "living".equals(kind);
        }
    }

    public static final DataMapType<Item, Profile> PROFILE = DataMapType.builder(TheoreticalSciences.id("wear"), Registries.ITEM, Profile.CODEC)
            .synced(Profile.CODEC, false).build();

    /** Runs used so far, and whether this one is worn out. */
    public record Used(int runs, boolean worn) {
        public static final Codec<Used> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("runs").forGetter(Used::runs),
                Codec.BOOL.optionalFieldOf("worn", false).forGetter(Used::worn)).apply(i, Used::new));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Used> STREAM = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Used::runs, ByteBufCodecs.BOOL, Used::worn, Used::new);
    }

    /** A source's decay clock: the game time it was made (stamp), or - in a Chronostatic Vault - the age it was held at. */
    public record Clock(long stamp, long frozenAge, boolean frozen) {
        public static final Codec<Clock> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("stamp").forGetter(Clock::stamp),
                Codec.LONG.optionalFieldOf("held_age", 0L).forGetter(Clock::frozenAge),
                Codec.BOOL.optionalFieldOf("held", false).forGetter(Clock::frozen)).apply(i, Clock::new));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Clock> STREAM = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Clock::stamp, ByteBufCodecs.VAR_LONG, Clock::frozenAge, ByteBufCodecs.BOOL, Clock::frozen, Clock::new);
    }

    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TheoreticalSciences.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Used>> USED = COMPONENTS.registerComponentType("wear",
            b -> b.persistent(Used.CODEC).networkSynchronized(Used.STREAM));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Clock>> CLOCK = COMPONENTS.registerComponentType("decay_clock",
            b -> b.persistent(Clock.CODEC).networkSynchronized(Clock.STREAM));

    /** Everything a machine recipe holds as a catalyst - what a machine's catalyst slot accepts (generated by tools/round58.py). */
    public static final net.minecraft.tags.TagKey<Item> CATALYSTS = net.minecraft.tags.TagKey.create(Registries.ITEM, TheoreticalSciences.id("catalysts"));

    public static boolean isCatalyst(ItemStack s) {
        return !s.isEmpty() && s.is(CATALYSTS);
    }

    /**
     * 1.2.0: the purity tag. A fluid assayed at the Titration Bench carries it; a feed in the
     * theoreticalsciences:assayable fluid tag that does not costs a poisonable catalyst {@link #POISON_RUNS} runs a run.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.util.Unit>> ASSAYED = COMPONENTS.registerComponentType("assayed",
            b -> b.persistent(net.minecraft.util.Unit.CODEC).networkSynchronized(StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)));
    /** 1.2.0 (Titan): a piece of armour treated with a Hydrophobic Coating - methane rain runs off it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.util.Unit>> COATED = COMPONENTS.registerComponentType("hydrophobic",
            b -> b.persistent(net.minecraft.util.Unit.CODEC).networkSynchronized(StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> ASSAYABLE =
            net.minecraft.tags.TagKey.create(Registries.FLUID, TheoreticalSciences.id("assayable"));

    /** True when every fluid these feeds draw that could have been assayed was. */
    public static boolean feedsPure(List<net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient> wanted, List<net.neoforged.neoforge.fluids.FluidStack> tanks) {
        for (var w : wanted)
            for (var f : tanks)
                if (!f.isEmpty() && w.ingredient().test(f) && f.getFluid().builtInRegistryHolder().is(ASSAYABLE) && !f.has(ASSAYED.get())) return false;
        return true;
    }

    /** What a Lead Pig holds (see {@link LeadPigItem}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LeadPigItem.Contents>> PIG = COMPONENTS.registerComponentType("lead_pig",
            b -> b.persistent(LeadPigItem.Contents.CODEC).networkSynchronized(LeadPigItem.Contents.STREAM));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(PROFILE));
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public static Profile profile(ItemStack s) {
        return s.isEmpty() ? null : s.getItem().builtInRegistryHolder().getData(PROFILE);
    }

    // ------------------------------------------------------------------ sources
    /** Game ticks since the source was made (held still in a vault). */
    public static long age(ItemStack s, long now) {
        Clock c = s.get(CLOCK.get());
        if (c == null) return 0;
        return c.frozen() ? c.frozenAge() : Math.max(0, now - c.stamp());
    }

    /** Fraction of its first activity left: 1 when new. */
    public static double activity(ItemStack s, long now) {
        Profile p = profile(s);
        if (p == null || !p.source()) return 1;
        double half = halfLife(s, p);
        if (half <= 0) return 1;
        double seconds = age(s, now) / 20.0 * DECAY_SCALE;
        return Math.pow(0.5, seconds / half);
    }

    /** Seconds: the profile's own, or else the half-life of the isotope the item's radiation comes from. */
    public static double halfLife(ItemStack s, Profile p) {
        if (p.halfLife() > 0) return p.halfLife();
        RadiationData.Source src = RadiationData.source(s);
        return src == null ? 0 : src.isotope().halfLifeSeconds;
    }

    /** Game ticks the stamps are kept to: a game day, or less for a source that would lose much of itself in a day. */
    static long step(ItemStack s, Profile p) {
        double half = halfLife(s, p);
        if (half <= 0) return STAMP_STEP;
        long halfTicks = (long) (half / DECAY_SCALE * 20);
        return Math.max(1, Math.min(STAMP_STEP, halfTicks / 32));
    }

    /** Starts a source's clock the first time it is seen (kept to the day, so a day's sources stack together). */
    public static void stamp(ItemStack s, long now) {
        Profile p = profile(s);
        if (p == null || !p.source() || s.has(CLOCK.get())) return;
        if (p.exposed()) {
            s.set(CLOCK.get(), new Clock(0, 0, true));
            return;
        }
        long st = step(s, p);
        s.set(CLOCK.get(), new Clock(now - Math.floorMod(now, st), 0, false));
    }

    /** Into a Chronostatic Vault: the clock is held at the age it has. */
    public static ItemStack hold(ItemStack s, long now) {
        Profile p = profile(s);
        if (p == null || !p.source() || s.isEmpty()) return s;
        Clock c = s.get(CLOCK.get());
        if (c != null && c.frozen()) return s;
        if (p.exposed()) {
            ItemStack out = s.copy();
            out.set(CLOCK.get(), new Clock(0, 0, true));
            return out;
        }
        long age = c == null ? 0 : Math.max(0, now - c.stamp());
        ItemStack out = s.copy();
        out.set(CLOCK.get(), new Clock(0, age - Math.floorMod(age, step(s, p)), true));
        return out;
    }

    /** Out of the vault: the clock runs again from the held age. */
    public static ItemStack release(ItemStack s, long now) {
        Clock c = s.get(CLOCK.get());
        Profile p = profile(s);
        if (c == null || !c.frozen() || p == null || p.exposed()) return s;
        ItemStack out = s.copy();
        long stamp = now - c.frozenAge();
        long st = step(s, p);
        out.set(CLOCK.get(), new Clock(stamp + Math.floorMod(-stamp, st), 0, false));
        return out;
    }

    /** An exposed source (Technetium-99m) out in the open for this many ticks: it ages by them. Changes the stack itself. */
    public static void expose(ItemStack s, long ticks) {
        Profile p = profile(s);
        if (p == null || !p.exposed()) return;
        Clock c = s.get(CLOCK.get());
        long age = c == null ? 0 : c.frozen() ? c.frozenAge() : 0;
        s.set(CLOCK.get(), new Clock(0, age + ticks, true));
    }

    // ------------------------------------------------------------------ catalysts and instruments
    /** Worn out (runs), or spent (sources): it no longer works as a catalyst. */
    public static boolean exhausted(ItemStack s, @Nullable Level level) {
        Profile p = profile(s);
        if (p == null) return false;
        if (p.source()) return level != null && activity(s, level.getGameTime()) < SPENT;
        Used u = s.get(USED.get());
        return u != null && u.worn();
    }

    /** The inputs as a recipe's catalysts see them: worn and spent items are not there. */
    public static List<ItemStack> usable(List<ItemStack> in, @Nullable Level level) {
        List<ItemStack> out = null;
        for (int i = 0; i < in.size(); i++) {
            if (exhausted(in.get(i), level)) {
                if (out == null) out = new ArrayList<>(in);
                out.set(i, ItemStack.EMPTY);
            }
        }
        return out == null ? in : out;
    }

    /**
     * After a run: every catalyst the recipe named uses a run of its life (or {@link #HOT_RUNS}). Returns the worn-out
     * items that came off (restorable ones, marked worn) for the machine to put out.
     */
    public static List<ItemStack> afterRun(List<Ingredient> catalysts, List<ItemStack> inputs, boolean hot, ServerLevel level) {
        return afterRun(catalysts, inputs, hot, true, level);
    }

    public static List<ItemStack> afterRun(List<Ingredient> catalysts, List<ItemStack> inputs, boolean hot, boolean pure, ServerLevel level) {
        return afterRun(catalysts, inputs, hot, pure, 1.0, false, level);
    }

    /** 1.2.0: catalysts that are alive (the Living Catalyst) heal a run of wear each run the recipe feeds them. */
    public static final java.util.Set<String> SELF_HEALING = java.util.Set.of("living_catalyst");

    public static boolean selfHealing(ItemStack s) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
        return id.getNamespace().equals(TheoreticalSciences.MODID) && SELF_HEALING.contains(id.getPath());
    }

    /**
     * 1.2.0: {@code scale} is the share of runs that count (a Symbiont Lining makes it 0.6: catalysts wear 40% slower);
     * {@code fed} is true when the recipe feeds the machine sugar or broth, which heals a self-healing catalyst instead.
     */
    public static List<ItemStack> afterRun(List<Ingredient> catalysts, List<ItemStack> inputs, boolean hot, boolean pure, double scale, boolean fed,
                                           ServerLevel level) {
        List<ItemStack> off = new ArrayList<>();
        for (Ingredient cat : catalysts) {
            for (ItemStack s : inputs) {
                if (s.isEmpty() || !cat.test(s) || exhausted(s, level)) continue;
                Profile p = profile(s);
                if (p == null || p.source() || p.life() <= 0) break;      // permanent (or decaying by itself): nothing to do
                Used u = s.getOrDefault(USED.get(), new Used(0, false));
                int add = (hot ? HOT_RUNS : 1) + (!pure && p.poison() ? POISON_RUNS : 0);
                if (scale < 1.0) {
                    int kept = 0;
                    for (int k = 0; k < add; k++) if (level.random.nextDouble() < scale) kept++;
                    add = kept;
                }
                if (fed && !hot && selfHealing(s)) add = -1;
                int runs = Math.max(0, u.runs() + add);
                if (runs < p.life()) {
                    s.set(USED.get(), new Used(runs, false));
                } else {
                    ItemStack worn = s.copyWithCount(1);
                    s.shrink(1);
                    if (!s.isEmpty()) s.remove(USED.get());       // the next one of the stack starts fresh
                    if (!p.restore().isEmpty()) {
                        worn.set(USED.get(), new Used(p.life(), true));
                        off.add(worn);
                    }
                }
                break;
            }
        }
        return off;
    }

    // ------------------------------------------------------------------ tooltip
    public static void tooltip(ItemStack s, @Nullable Level level, List<Component> out) {
        if (selfHealing(s)) out.add(Component.translatable("tooltip.theoreticalsciences.wear.heals").withStyle(net.minecraft.ChatFormatting.GREEN));
        Profile p = profile(s);
        if (p == null) return;
        if (p.source()) {
            double a = level == null ? 1 : activity(s, level.getGameTime());
            Clock c = s.get(CLOCK.get());
            if (p.living()) {
                out.add(Component.translatable(a < SPENT ? "tooltip.theoreticalsciences.wear.dead" : "tooltip.theoreticalsciences.wear.viability",
                        String.format(Locale.ROOT, "%.0f%%", a * 100)).withStyle(a < SPENT ? ChatFormatting.RED : ChatFormatting.GREEN));
                out.add(Component.translatable("tooltip.theoreticalsciences.wear.living").withStyle(ChatFormatting.DARK_AQUA));
                double h = halfLife(s, p) / DECAY_SCALE;
                if (h > 0) out.add(Component.translatable("tooltip.theoreticalsciences.wear.living_half", span(h)).withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            out.add(Component.translatable(a < SPENT ? "tooltip.theoreticalsciences.wear.spent" : "tooltip.theoreticalsciences.wear.activity",
                    String.format(Locale.ROOT, "%.0f%%", a * 100)).withStyle(a < SPENT ? ChatFormatting.RED : ChatFormatting.YELLOW));
            if (p.exposed()) out.add(Component.translatable("tooltip.theoreticalsciences.wear.exposed").withStyle(ChatFormatting.DARK_AQUA));
            else if (c != null && c.frozen()) out.add(Component.translatable("tooltip.theoreticalsciences.wear.held").withStyle(ChatFormatting.AQUA));
            double half = halfLife(s, p) / DECAY_SCALE;           // real seconds
            if (half > 0) out.add(Component.translatable("tooltip.theoreticalsciences.wear.half_life", span(half)).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (p.life() <= 0) return;
        Used u = s.get(USED.get());
        if (u != null && u.worn())
            out.add(Component.translatable("tooltip.theoreticalsciences.wear.worn").withStyle(ChatFormatting.RED));
        else
            out.add(Component.translatable("tooltip.theoreticalsciences.wear.life", p.life() - (u == null ? 0 : u.runs()), p.life())
                    .withStyle(ChatFormatting.GRAY));
        if (p.poison()) out.add(Component.translatable("tooltip.theoreticalsciences.wear.poison", POISON_RUNS).withStyle(ChatFormatting.DARK_GRAY));
        out.add(p.restore().isEmpty() ? Component.translatable("tooltip.theoreticalsciences.wear.used_up").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.theoreticalsciences.wear.restore",
                        Component.translatable("block." + TheoreticalSciences.MODID + "." + p.restore())).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** "15 s", "2.3 h", "405 days" - how long a real-time span is. */
    public static String span(double sec) {
        if (sec < 120) return String.format(Locale.ROOT, "%.0f s", sec);
        if (sec < 7200) return String.format(Locale.ROOT, "%.0f min", sec / 60);
        if (sec < 172800) return String.format(Locale.ROOT, "%.1f h", sec / 3600);
        if (sec < 3.156e7 * 2) return String.format(Locale.ROOT, "%.0f days", sec / 86400);
        return String.format(Locale.ROOT, "%.0f years", sec / 3.156e7);
    }

    /** Durability-style bar: how much life (or activity) is left. */
    public static float barFraction(ItemStack s, @Nullable Level level) {
        Profile p = profile(s);
        if (p == null) return -1;
        if (p.source()) return level == null || !s.has(CLOCK.get()) ? -1 : (float) Mth.clamp(activity(s, level.getGameTime()), 0, 1);
        Used u = s.get(USED.get());
        if (u == null || p.life() <= 0) return -1;
        return u.worn() ? 0 : Mth.clamp(1F - (float) u.runs() / p.life(), 0, 1);
    }

    private Wear() {}
}
