package com.theoreticalsciences.field;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The physical conditions a recipe needs around the machine's controller. Each bound is optional; the defaults mean
 * "anything goes". Values are read from {@link FieldEnvironment} - the dimension's own nature, overridden by any
 * Field Projector covering the spot.
 * <pre>
 * "environment": {
 *   "gravity_min": 100,     // in g (Earth = 1)
 *   "gravity_max": 0.1,     // micro-gravity
 *   "dimensions_min": 5,    // spatial dimensions
 *   "time_min": 10,         // how fast time runs here relative to normal
 *   "time_max": 0.1,
 *   "vacuum_min": 2,        // 0 ordinary, 1 Casimir-depleted, 2 false vacuum, 3 true vacuum
 *   "dimensions": ["theoreticalsciences:quantum_foam"],    // round 35: only in one of these dimensions (by dimension id)
 *   "biomes": ["minecraft:desert", "#c:is_cold"]      // round 35: only in one of these biomes (ids or #tags)
 * }
 * </pre>
 */
public record Environment(double gravityMin, double gravityMax, int dimensionsMin, double timeMin, double timeMax, int vacuumMin,
                          List<String> dimensions, List<String> biomes) {
    public static final Environment ANY = new Environment(0, Double.MAX_VALUE, 0, 0, Double.MAX_VALUE, 0, List.of(), List.of());

    public static final Codec<Environment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("gravity_min", 0.0).forGetter(Environment::gravityMin),
            Codec.DOUBLE.optionalFieldOf("gravity_max", Double.MAX_VALUE).forGetter(Environment::gravityMax),
            Codec.INT.optionalFieldOf("dimensions_min", 0).forGetter(Environment::dimensionsMin),
            Codec.DOUBLE.optionalFieldOf("time_min", 0.0).forGetter(Environment::timeMin),
            Codec.DOUBLE.optionalFieldOf("time_max", Double.MAX_VALUE).forGetter(Environment::timeMax),
            Codec.INT.optionalFieldOf("vacuum_min", 0).forGetter(Environment::vacuumMin),
            Codec.STRING.listOf().optionalFieldOf("dimensions", List.of()).forGetter(Environment::dimensions),
            Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(Environment::biomes)
    ).apply(i, Environment::new));

    public boolean isAny() {
        return equals(ANY);
    }

    public boolean test(FieldEnvironment.State s) {
        return s.gravity() >= gravityMin && s.gravity() <= gravityMax && s.dimensions() >= dimensionsMin
                && s.time() >= timeMin && s.time() <= timeMax && s.vacuum() >= vacuumMin;
    }

    /** Round 35: the place itself - which dimension (by its id) and which biome (by id or tag) the machine stands in. */
    public boolean testPlace(Level level, BlockPos pos) {
        if (!dimensions.isEmpty() && !dimensions.contains(level.dimension().location().toString())) return false;
        if (biomes.isEmpty()) return true;
        var biome = level.getBiome(pos);
        for (String b : biomes) {
            if (b.startsWith("#")) {
                ResourceLocation t = ResourceLocation.tryParse(b.substring(1));
                if (t != null && biome.is(TagKey.create(Registries.BIOME, t))) return true;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(b);
                if (id != null && biome.is(id)) return true;
            }
        }
        return false;
    }

    public boolean test(Level level, BlockPos pos) {
        return testPlace(level, pos) && test(FieldEnvironment.at(level, pos));
    }

    public void write(FriendlyByteBuf b) {
        b.writeDouble(gravityMin);
        b.writeDouble(gravityMax);
        b.writeVarInt(dimensionsMin);
        b.writeDouble(timeMin);
        b.writeDouble(timeMax);
        b.writeVarInt(vacuumMin);
        b.writeCollection(dimensions, FriendlyByteBuf::writeUtf);
        b.writeCollection(biomes, FriendlyByteBuf::writeUtf);
    }

    public static Environment read(FriendlyByteBuf b) {
        return new Environment(b.readDouble(), b.readDouble(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readVarInt(),
                b.readList(FriendlyByteBuf::readUtf), b.readList(FriendlyByteBuf::readUtf));
    }

    static String num(double d) {
        return d >= 1 ? String.format("%,.0f", d) : String.format("%.2f", d);
    }

    /** One line per requirement, for JEI and the machine screen. */
    public List<Component> describe() {
        List<Component> out = new ArrayList<>();
        if (gravityMin > 0) out.add(Component.translatable("env.theoreticalsciences.gravity_min", num(gravityMin)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (gravityMax < Double.MAX_VALUE) out.add(Component.translatable("env.theoreticalsciences.gravity_max", num(gravityMax)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (dimensionsMin > 3) out.add(Component.translatable("env.theoreticalsciences.dimensions_min", dimensionsMin).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (timeMin > 0) out.add(Component.translatable("env.theoreticalsciences.time_min", num(timeMin)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (timeMax < Double.MAX_VALUE) out.add(Component.translatable("env.theoreticalsciences.time_max", num(timeMax)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (vacuumMin > 0) out.add(Component.translatable("env.theoreticalsciences.vacuum_min", Component.translatable("env.theoreticalsciences.vacuum." + vacuumMin)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!dimensions.isEmpty())
            out.add(Component.translatable("env.theoreticalsciences.in_dimension", joined(dimensions, false)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!biomes.isEmpty())
            out.add(Component.translatable("env.theoreticalsciences.in_biome", joined(biomes, true)).withStyle(ChatFormatting.LIGHT_PURPLE));
        return out;
    }

    /** "the Quantum Foam or the Bulk" / "Desert or #c:is_cold". */
    private static Component joined(List<String> ids, boolean biome) {
        net.minecraft.network.chat.MutableComponent c = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) c.append(Component.translatable("env.theoreticalsciences.or"));
            String s = ids.get(i);
            ResourceLocation rl = ResourceLocation.tryParse(s.startsWith("#") ? s.substring(1) : s);
            if (rl == null || s.startsWith("#")) c.append(Component.literal(s));
            else if (biome) c.append(Component.translatableWithFallback("biome." + rl.getNamespace() + "." + rl.getPath(), rl.getPath()));
            else c.append(com.theoreticalsciences.machine.MachineType.dimensionName(rl));
        }
        return c;
    }
}
