package com.theoreticalsciences.home;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import javax.annotation.Nullable;
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
 *   "key": {"A": {"item": "...", "count": 4096}, "B": {"tag": "c:ingots/lithium", "count": 64}},
 *   "result": {"item": "...", "count": 4096},
 *   "needs_home": true }      // only for a player whose Recreating Home checklist is complete
 * </pre>
 */
public class EmpyreanRecipe implements Recipe<RecipeInput> {
    public static final int SIZE = 15;

    /**
     * What a slot asks for: one exact item, or (1.3.1) any item of a tag. Equal needs are counted together, so slots
     * naming the same item, or the same tag, are filled from one count.
     */
    public record Need(@Nullable Item item, @Nullable TagKey<Item> tag) {
        public static Need of(Item item) {
            return new Need(item, null);
        }

        public static Need of(TagKey<Item> tag) {
            return new Need(null, tag);
        }

        public boolean isTag() {
            return tag != null;
        }

        public boolean matches(Item it) {
            return tag == null ? it == item : it.builtInRegistryHolder().is(tag);
        }

        /** Every item it takes (one, for an item; the tag's items now, for a tag). */
        public List<Item> items() {
            if (tag == null) return List.of(item);
            List<Item> out = new ArrayList<>();
            BuiltInRegistries.ITEM.getTag(tag).ifPresent(set -> set.forEach(h -> out.add(h.value())));
            return out;
        }

        /** The item to draw for it (a tag's first item; a barrier for an empty tag). */
        public Item display() {
            if (tag == null) return item;
            return BuiltInRegistries.ITEM.getTag(tag).flatMap(set -> set.stream().findFirst()).map(net.minecraft.core.Holder::value).orElse(Items.BARRIER);
        }

        public Component name() {
            return tag == null ? item.getDescription() : Component.translatable("gui.theoreticalsciences.empyrean.any_tag", "#" + tag.location());
        }
    }

    /** A slot: what it takes and how many. Written as {"item": ..., "count": n} or {"tag": ..., "count": n}. */
    public record Slot(Need need, long count) {
        static final Codec<Slot> CODEC = RecordCodecBuilder.<Slot>create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(s -> java.util.Optional.ofNullable(s.need.item())),
                TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(s -> java.util.Optional.ofNullable(s.need.tag())),
                Codec.LONG.optionalFieldOf("count", 1L).forGetter(Slot::count)
        ).apply(i, (item, tag, count) -> new Slot(item.isPresent() ? Need.of(item.get()) : tag.map(Need::of).orElse(null), count)))
                .validate(s -> s.need == null ? DataResult.error(() -> "an Empyrean slot needs \"item\" or \"tag\"")
                        : s.count < 1 ? DataResult.error(() -> "an Empyrean slot's count must be at least 1") : DataResult.success(s));

        /** The item to draw (see Need.display). */
        public Item item() {
            return need.display();
        }
    }

    /** The product: one exact item and how many one craft makes. */
    public record Out(Item item, long count) {
        static final Codec<Out> CODEC = RecordCodecBuilder.<Out>create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Out::item),
                Codec.LONG.optionalFieldOf("count", 1L).forGetter(Out::count)).apply(i, Out::new))
                .validate(o -> o.count < 1 ? DataResult.error(() -> "an Empyrean result's count must be at least 1") : DataResult.success(o));
    }

    final List<String> pattern;
    final Map<String, Slot> key;
    public final Out result;
    public final boolean needsHome;
    /** Row-major, SIZE*SIZE long; null for an empty slot. */
    private final Slot[] cells = new Slot[SIZE * SIZE];
    private final Map<Need, Long> totals = new LinkedHashMap<>();

    public EmpyreanRecipe(List<String> pattern, Map<String, Slot> key, Out result, boolean needsHome) {
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
                totals.merge(s.need(), s.count(), Long::sum);
            }
        }
    }

    /** What stands in slot (x, y), or null. */
    public Slot cell(int x, int y) {
        return cells[y * SIZE + x];
    }

    /** Everything the recipe takes and how many of it in all (in the order the picture first shows them). */
    public Map<Need, Long> totals() {
        return totals;
    }

    /**
     * Which of this recipe's needs an item counts towards, or null: an exact item first, else the first tag (in picture
     * order) that has it. So an item named in its own slot is never used up by a tag slot instead.
     */
    @Nullable
    public Need needFor(Item item) {
        Need tagged = null;
        for (Need n : totals.keySet()) {
            if (!n.isTag()) {
                if (n.item() == item) return n;
            } else if (tagged == null && n.matches(item)) tagged = n;
        }
        return tagged;
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
                Out.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.BOOL.optionalFieldOf("needs_home", false).forGetter(r -> r.needsHome)
        ).apply(i, EmpyreanRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmpyreanRecipe> STREAM = StreamCodec.of((buf, r) -> {
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, r.pattern);
            buf.writeVarInt(r.key.size());
            r.key.forEach((k, v) -> {
                buf.writeUtf(k);
                buf.writeBoolean(v.need().isTag());
                buf.writeResourceLocation(v.need().isTag() ? v.need().tag().location() : BuiltInRegistries.ITEM.getKey(v.need().item()));
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
                boolean tag = buf.readBoolean();
                ResourceLocation id = buf.readResourceLocation();
                Need need = tag ? Need.of(TagKey.create(Registries.ITEM, id)) : Need.of(BuiltInRegistries.ITEM.get(id));
                k.put(c, new Slot(need, buf.readVarLong()));
            }
            ResourceLocation out = buf.readResourceLocation();
            Out res = new Out(BuiltInRegistries.ITEM.get(out), buf.readVarLong());
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
