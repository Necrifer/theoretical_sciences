package com.theoreticalsciences.home;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recipe of the Empyrean Assembly Table: a picture of up to 15 by 15 slots, each naming an item and how many of it
 * (far more than a stack), and a result that may also be far more than a stack.
 * <pre>
 * { "type": "theoreticalsciences:empyrean", "pattern": [15 rows of 15 characters, ' ' = empty],
 *   "key": {"A": {"item": "...", "count": 4096}}, "result": {"item": "...", "count": 4096},
 *   "needs_home": true }      // only for a player whose Recreating Home checklist is complete
 * </pre>
 */
public class EmpyreanRecipe implements Recipe<RecipeInput> {
    public static final int SIZE = 15;

    public record Slot(Item item, long count) {
        static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Slot::item),
                Codec.LONG.optionalFieldOf("count", 1L).forGetter(Slot::count)).apply(i, Slot::new));
    }

    final List<String> pattern;
    final Map<String, Slot> key;
    public final Slot result;
    public final boolean needsHome;
    /** Row-major, SIZE*SIZE long; null for an empty slot. */
    private final Slot[] cells = new Slot[SIZE * SIZE];
    private final Map<Item, Long> totals = new LinkedHashMap<>();

    public EmpyreanRecipe(List<String> pattern, Map<String, Slot> key, Slot result, boolean needsHome) {
        this.pattern = List.copyOf(pattern);
        this.key = Map.copyOf(key);
        this.result = result;
        this.needsHome = needsHome;
        for (int y = 0; y < Math.min(SIZE, pattern.size()); y++) {
            String row = pattern.get(y);
            for (int x = 0; x < Math.min(SIZE, row.length()); x++) {
                Slot s = row.charAt(x) == ' ' ? null : key.get(String.valueOf(row.charAt(x)));
                if (s == null) continue;
                cells[y * SIZE + x] = s;
                totals.merge(s.item(), s.count(), Long::sum);
            }
        }
    }

    /** What stands in slot (x, y), or null. */
    public Slot cell(int x, int y) {
        return cells[y * SIZE + x];
    }

    /** Every item the recipe takes and how many of it in all (in the order the picture first shows them). */
    public Map<Item, Long> totals() {
        return totals;
    }

    @Override
    public boolean matches(RecipeInput in, Level level) {
        return false;          // the table matches by what it holds, not by a crafting grid
    }

    @Override
    public ItemStack assemble(RecipeInput in, HolderLookup.Provider regs) {
        return getResultItem(regs).copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider regs) {
        return new ItemStack(result.item(), (int) Math.min(result.count(), result.item().getDefaultMaxStackSize()));
    }

    @Override
    public boolean isSpecial() {
        return true;           // not for the vanilla recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HomeRegistry.EMPYREAN_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HomeRegistry.EMPYREAN_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<EmpyreanRecipe> {
        private static final Codec<List<String>> PATTERN = Codec.STRING.listOf().flatXmap(l -> l.size() <= SIZE && l.stream().allMatch(s -> s.length() <= SIZE)
                ? DataResult.success(l) : DataResult.error(() -> "Empyrean patterns are at most 15x15"), DataResult::success);
        public static final MapCodec<EmpyreanRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                PATTERN.fieldOf("pattern").forGetter(r -> r.pattern),
                Codec.unboundedMap(Codec.STRING, Slot.CODEC).fieldOf("key").forGetter(r -> r.key),
                Slot.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.BOOL.optionalFieldOf("needs_home", false).forGetter(r -> r.needsHome)
        ).apply(i, EmpyreanRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmpyreanRecipe> STREAM = StreamCodec.of((buf, r) -> {
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, r.pattern);
            buf.writeVarInt(r.key.size());
            r.key.forEach((k, v) -> {
                buf.writeUtf(k);
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(v.item()));
                buf.writeVarLong(v.count());
            });
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(r.result.item()));
            buf.writeVarLong(r.result.count());
            buf.writeBoolean(r.needsHome);
        }, buf -> {
            List<String> p = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);
            int n = buf.readVarInt();
            Map<String, Slot> k = new java.util.HashMap<>();
            for (int j = 0; j < n; j++) {
                String c = buf.readUtf();
                ResourceLocation id = buf.readResourceLocation();
                k.put(c, new Slot(BuiltInRegistries.ITEM.get(id), buf.readVarLong()));
            }
            ResourceLocation out = buf.readResourceLocation();
            Slot res = new Slot(BuiltInRegistries.ITEM.get(out), buf.readVarLong());
            return new EmpyreanRecipe(new ArrayList<>(p), k, res, buf.readBoolean());
        });

        @Override
        public MapCodec<EmpyreanRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EmpyreanRecipe> streamCodec() {
            return STREAM;
        }
    }
}
