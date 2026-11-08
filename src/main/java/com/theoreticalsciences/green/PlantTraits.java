package com.theoreticalsciences.green;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.4.0 (Green Kingdom): the traits a bred seed carries, kept on the item ({@code theoreticalsciences:plant_traits}):
 * which line it was selected for ({@code yield}, {@code hardiness} or {@code glow}) and how many rounds of selection it
 * has been through. Recipes match a generation with NeoForge's component ingredient, and make the next one with a fixed
 * component on the output, so breeding is ordinary data (see the Breeding Bench recipes); seeds of different traits do
 * not stack, and JEI and EMI show each as its own item.
 */
public final class PlantTraits {
    public record Traits(String line, int level) {
        public static final Codec<Traits> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("line").forGetter(Traits::line),
                Codec.INT.fieldOf("level").forGetter(Traits::level)).apply(i, Traits::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Traits> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Traits::line, ByteBufCodecs.VAR_INT, Traits::level, Traits::new);
    }

    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TheoreticalSciences.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Traits>> TRAITS = COMPONENTS.registerComponentType("plant_traits",
            b -> b.persistent(Traits.CODEC).networkSynchronized(Traits.STREAM));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    @Nullable
    public static Traits of(ItemStack s) {
        return s.get(TRAITS.get());
    }

    /** "yield/3" and so on, for JEI and EMI: one entry per line and level. */
    public static String idOf(ItemStack s) {
        Traits t = of(s);
        return t == null ? "" : t.line() + "/" + t.level();
    }

    static String roman(int n) {
        String[] r = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    /** An Engineered Seed: named for its line and generation, its tooltip saying what the line does. */
    public static class EngineeredSeedItem extends com.theoreticalsciences.item.LoreItem {
        public EngineeredSeedItem(Properties p) {
            super(p);
        }

        @Override
        public Component getName(ItemStack s) {
            Traits t = of(s);
            if (t == null) return super.getName(s);
            return Component.translatable("item.theoreticalsciences.engineered_seed.of",
                    Component.translatable("trait.theoreticalsciences." + t.line()), roman(t.level()));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, tip, flag);
            Traits t = of(stack);
            if (t != null)
                tip.add(Component.translatable("trait.theoreticalsciences." + t.line() + ".desc", t.level()).withStyle(ChatFormatting.GREEN));
        }
    }

    private PlantTraits() {}
}
