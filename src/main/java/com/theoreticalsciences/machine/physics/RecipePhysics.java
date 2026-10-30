package com.theoreticalsciences.machine.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a recipe asks of the machine's physics, QMD-style.
 * <pre>
 * "physics": {
 *   "vars":    {"m": 0.938, "q": 1},                         // the particle / plasma / feed this recipe works with
 *   "windows": [{"q": "E_k", "min": 2.0, "max": 3.5}]        // the machine must be operated inside these
 * }
 * </pre>
 */
public record RecipePhysics(Map<String, Double> vars, List<Window> windows) {
    public static final RecipePhysics NONE = new RecipePhysics(Map.of(), List.of());

    public record Window(String q, double min, double max) {
        public static final Codec<Window> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("q").forGetter(Window::q),
                Codec.DOUBLE.optionalFieldOf("min", -Double.MAX_VALUE).forGetter(Window::min),
                Codec.DOUBLE.optionalFieldOf("max", Double.MAX_VALUE).forGetter(Window::max)
        ).apply(i, Window::new));

        public boolean test(double v) {
            return v >= min && v <= max;
        }
    }

    public static final Codec<RecipePhysics> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("vars", Map.of()).forGetter(RecipePhysics::vars),
            Window.CODEC.listOf().optionalFieldOf("windows", List.of()).forGetter(RecipePhysics::windows)
    ).apply(i, RecipePhysics::new));

    public boolean isNone() {
        return vars.isEmpty() && windows.isEmpty();
    }

    public void write(FriendlyByteBuf b) {
        b.writeVarInt(vars.size());
        vars.forEach((k, v) -> {
            b.writeUtf(k);
            b.writeDouble(v);
        });
        b.writeVarInt(windows.size());
        for (Window w : windows) {
            b.writeUtf(w.q());
            b.writeDouble(w.min());
            b.writeDouble(w.max());
        }
    }

    public static RecipePhysics read(FriendlyByteBuf b) {
        int n = b.readVarInt();
        Map<String, Double> v = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) v.put(b.readUtf(), b.readDouble());
        int m = b.readVarInt();
        List<Window> w = new ArrayList<>();
        for (int i = 0; i < m; i++) w.add(new Window(b.readUtf(), b.readDouble(), b.readDouble()));
        return n == 0 && m == 0 ? NONE : new RecipePhysics(v, w);
    }
}
