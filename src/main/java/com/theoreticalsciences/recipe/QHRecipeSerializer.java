package com.theoreticalsciences.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public class QHRecipeSerializer implements RecipeSerializer<QHRecipe> {
    /**
     * An ingredient from another mod (round 16): {"item": "mekanism:alloy_atomic", "count": 4}. Used only if that item
     * exists in this game - so the same recipe works with or without the other mod. Items only (tags are not yet bound
     * when recipes load).
     */
    public record OptionalInput(String item, int count) {
        public static final Codec<OptionalInput> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("item").forGetter(OptionalInput::item),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(OptionalInput::count)).apply(i, OptionalInput::new));
    }

    /** A fluid from another mod (round 17): {"fluid": "nuclearcraft:helium_3", "amount": 8000}; used only if that fluid exists. */
    public record OptionalFluid(String fluid, int amount) {
        public static final Codec<OptionalFluid> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("fluid").forGetter(OptionalFluid::fluid),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("amount", 1000).forGetter(OptionalFluid::amount)).apply(i, OptionalFluid::new));
    }

    /** The configured cost multiplier (config "recipeIngredientMultiplier"), applied once when recipes load. */
    private static com.theoreticalsciences.machine.MachineType machine(String typeId) {
        for (com.theoreticalsciences.machine.MachineType t : com.theoreticalsciences.machine.MachineType.values()) if (t.recipeType.equals(typeId)) return t;
        return null;
    }

    private static double costMultiplier(String typeId) {
        com.theoreticalsciences.machine.MachineType mt = machine(typeId);
        if (mt != null && mt.generator) return 1.0;           // fuel, not cost
        try {
            return com.theoreticalsciences.QHConfig.INGREDIENT_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    private static List<SizedIngredient> withOptional(String typeId, List<SizedIngredient> base, List<OptionalInput> opt) {
        double m = costMultiplier(typeId);
        java.util.ArrayList<SizedIngredient> out = new java.util.ArrayList<>();
        for (SizedIngredient s : base) out.add(m == 1.0 ? s : new SizedIngredient(s.ingredient(), scale(s.count(), m)));
        for (OptionalInput o : opt) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(o.item());
            if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) continue;     // that mod is not installed
            out.add(new SizedIngredient(net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)), scale(o.count(), m)));
        }
        return mergeItems(out);
    }

    /**
     * Round 22: the same ingredient listed twice (in a pack's recipe, or an optional input that is already there)
     * becomes one entry with the counts added - a slot holds any number, so it never needs two.
     */
    static List<SizedIngredient> mergeItems(List<SizedIngredient> in) {
        if (in.size() < 2) return in;
        java.util.LinkedHashMap<String, SizedIngredient> m = new java.util.LinkedHashMap<>();
        for (SizedIngredient s : in) {
            String k = net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, s.ingredient())
                    .result().map(Object::toString).orElse("#" + System.identityHashCode(s));
            SizedIngredient old = m.get(k);
            m.put(k, old == null ? s : new SizedIngredient(s.ingredient(), (int) Math.min(com.theoreticalsciences.machine.BigItemStackHandler.BIG_LIMIT, (long) old.count() + s.count())));
        }
        return m.size() == in.size() ? in : new java.util.ArrayList<>(m.values());
    }

    static List<SizedFluidIngredient> mergeFluids(List<SizedFluidIngredient> in) {
        if (in.size() < 2) return in;
        java.util.LinkedHashMap<String, SizedFluidIngredient> m = new java.util.LinkedHashMap<>();
        for (SizedFluidIngredient s : in) {
            String k = net.neoforged.neoforge.fluids.crafting.FluidIngredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, s.ingredient())
                    .result().map(Object::toString).orElse("#" + System.identityHashCode(s));
            SizedFluidIngredient old = m.get(k);
            m.put(k, old == null ? s : new SizedFluidIngredient(s.ingredient(), (int) Math.min(Integer.MAX_VALUE, (long) old.amount() + s.amount())));
        }
        return m.size() == in.size() ? in : new java.util.ArrayList<>(m.values());
    }

    private static List<SizedFluidIngredient> scaledFluids(String typeId, List<SizedFluidIngredient> base, List<OptionalFluid> opt) {
        double m = costMultiplier(typeId);
        if (m == 1.0 && opt.isEmpty()) return mergeFluids(base);
        java.util.ArrayList<SizedFluidIngredient> out = new java.util.ArrayList<>();
        com.theoreticalsciences.machine.MachineType mt = machine(typeId);
        int cap = mt == null ? 128_000 : mt.tankCapacity();     // never more than one of its tanks can hold
        int slots = mt == null ? Integer.MAX_VALUE : mt.fluidInputs;
        for (SizedFluidIngredient f : base) out.add(m == 1.0 ? f : new SizedFluidIngredient(f.ingredient(), (int) Math.min(cap, Math.max(1, Math.round(f.amount() * m)))));
        for (OptionalFluid o : opt) {
            if (out.size() >= slots) break;
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(o.fluid());
            if (id == null || !net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)) continue;     // that mod is not installed
            net.minecraft.world.level.material.Fluid fl = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
            if (fl == net.minecraft.world.level.material.Fluids.EMPTY) continue;
            out.add(SizedFluidIngredient.of(new FluidStack(fl, (int) Math.min(cap, Math.max(1, Math.round(o.amount() * m))))));
        }
        return mergeFluids(out);
    }

    private static int scale(int n, double m) {
        return (int) Math.min(com.theoreticalsciences.machine.BigItemStackHandler.BIG_LIMIT, Math.max(1, Math.round(n * m)));
    }

    private final String typeId;
    private final MapCodec<QHRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, QHRecipe> streamCodec;

    public QHRecipeSerializer(String typeId) {
        this.typeId = typeId;
        this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("item_inputs", List.of()).forGetter(r -> r.itemInputs),
                SizedFluidIngredient.FLAT_CODEC.listOf().optionalFieldOf("fluid_inputs", List.of()).forGetter(r -> r.fluidInputs),
                ItemStack.CODEC.listOf().optionalFieldOf("item_outputs", List.of()).forGetter(r -> r.itemOutputs),
                FluidStack.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of()).forGetter(r -> r.fluidOutputs),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy", 0).forGetter(r -> r.energy),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", 200).forGetter(r -> r.duration),
                Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
                net.minecraft.world.item.crafting.Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("catalysts", List.of()).forGetter(r -> r.catalysts),
                Codec.floatRange(0F, 1F).listOf().optionalFieldOf("chances", List.of()).forGetter(r -> r.chances),
                Codec.intRange(0, 1_000_000).optionalFieldOf("min_tier", 0)   // round 31: any tier (datapacks add tiers above 5)
                        .forGetter(r -> r.minTier),
                Codec.BOOL.optionalFieldOf("secret", false).forGetter(r -> r.secret),
                com.theoreticalsciences.field.Environment.CODEC.optionalFieldOf("environment", com.theoreticalsciences.field.Environment.ANY).forGetter(r -> r.environment),
                com.theoreticalsciences.machine.physics.RecipePhysics.CODEC.optionalFieldOf("physics", com.theoreticalsciences.machine.physics.RecipePhysics.NONE).forGetter(r -> r.physics),
                OptionalInput.CODEC.listOf().optionalFieldOf("optional_inputs", List.of()).forGetter(r -> List.of()),
                OptionalFluid.CODEC.listOf().optionalFieldOf("optional_fluid_inputs", List.of()).forGetter(r -> List.of()),
                Codec.intRange(0, 1_000_000).optionalFieldOf("insight_per_run", 0).forGetter(r -> r.insight)
        ).apply(i, (a, b, c, d, e, f, g, h, k, t, sec, env, ph, opt, optf, ins) -> {
            QHRecipe r = new QHRecipe(typeId, withOptional(typeId, a, opt), scaledFluids(typeId, b, optf), c, d, e, f, g, h, k, t);
            r.secret = sec;
            r.insight = ins;
            r.environment = env;
            r.physics = ph;
            return r;
        }));

        StreamCodec<RegistryFriendlyByteBuf, List<SizedIngredient>> items = SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list());
        StreamCodec<RegistryFriendlyByteBuf, List<SizedFluidIngredient>> fluids = SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list());
        StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> outItems = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
        StreamCodec<RegistryFriendlyByteBuf, List<FluidStack>> outFluids = FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list());
        StreamCodec<RegistryFriendlyByteBuf, List<net.minecraft.world.item.crafting.Ingredient>> cats =
                net.minecraft.world.item.crafting.Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list());
        this.streamCodec = StreamCodec.of((buf, r) -> {
            items.encode(buf, r.itemInputs);
            fluids.encode(buf, r.fluidInputs);
            outItems.encode(buf, r.itemOutputs);
            outFluids.encode(buf, r.fluidOutputs);
            buf.writeVarInt(r.energy);
            buf.writeVarInt(r.duration);
            buf.writeUtf(r.research);
            cats.encode(buf, r.catalysts);
            buf.writeVarInt(r.chances.size());
            for (float f : r.chances) buf.writeFloat(f);
            buf.writeVarInt(r.minTier);
            buf.writeBoolean(r.secret);
            buf.writeVarInt(r.insight);
            r.environment.write(buf);
            r.physics.write(buf);
        }, buf -> {
            List<SizedIngredient> a = items.decode(buf);
            List<SizedFluidIngredient> b = fluids.decode(buf);
            List<ItemStack> c = outItems.decode(buf);
            List<FluidStack> d = outFluids.decode(buf);
            int e = buf.readVarInt(), f = buf.readVarInt();
            String g = buf.readUtf();
            List<net.minecraft.world.item.crafting.Ingredient> h = cats.decode(buf);
            int n = buf.readVarInt();
            java.util.ArrayList<Float> k = new java.util.ArrayList<>();
            for (int j = 0; j < n; j++) k.add(buf.readFloat());
            int t = buf.readVarInt();
            QHRecipe r = new QHRecipe(typeId, a, b, c, d, e, f, g, h, k, t);
            r.secret = buf.readBoolean();
            r.insight = buf.readVarInt();
            r.environment = com.theoreticalsciences.field.Environment.read(buf);
            r.physics = com.theoreticalsciences.machine.physics.RecipePhysics.read(buf);
            return r;
        });
    }

    public String typeId() {
        return typeId;
    }

    @Override
    public MapCodec<QHRecipe> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, QHRecipe> streamCodec() {
        return streamCodec;
    }
}
