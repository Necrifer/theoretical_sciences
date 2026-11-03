package com.theoreticalsciences.science;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Round 38 (1.2): radiation as data, so packs can add their own materials (data/&lt;ns&gt;/data_maps/...):
 * <ul>
 *   <li>{@code item/radiation_source.json} and {@code block/radiation_source.json}: what an item or block gives off -
 *   {@code {"isotope": "u238_chain", "strength": 0.01}}, strength being the dose rate in mSv/s one block away (for an
 *   item: carried, per item).</li>
 *   <li>{@code block/radiation_shielding.json}: how much a block stops, {@code {"gamma": 2.77, "neutron": 0.1}} - the
 *   exponent μx for one block, so a block lets through e^(−μx) of what reaches it.</li>
 * </ul>
 * Without an entry the item tags radioactive / strongly_radioactive and the block tag radioactive still count, and any
 * solid block shields a little.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RadiationData {
    public record Source(Isotope isotope, float strength) {
        public static final Codec<Source> CODEC = RecordCodecBuilder.create(i -> i.group(
                Isotope.CODEC.fieldOf("isotope").forGetter(Source::isotope),
                Codec.FLOAT.fieldOf("strength").forGetter(Source::strength)).apply(i, Source::new));
    }

    public record Shielding(float gamma, float neutron) {
        public static final Codec<Shielding> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("gamma").forGetter(Shielding::gamma),
                Codec.FLOAT.optionalFieldOf("neutron", 0.3F).forGetter(Shielding::neutron)).apply(i, Shielding::new));
    }

    public static final DataMapType<Item, Source> ITEM_SOURCE = DataMapType.builder(TheoreticalSciences.id("radiation_source"), Registries.ITEM, Source.CODEC)
            .synced(Source.CODEC, false).build();
    public static final DataMapType<Block, Source> BLOCK_SOURCE = DataMapType.builder(TheoreticalSciences.id("radiation_source"), Registries.BLOCK, Source.CODEC)
            .synced(Source.CODEC, false).build();
    public static final DataMapType<Block, Shielding> SHIELDING = DataMapType.builder(TheoreticalSciences.id("radiation_shielding"), Registries.BLOCK, Shielding.CODEC)
            .synced(Shielding.CODEC, false).build();

    /** Fallbacks for the tags. */
    static final Source WEAK = new Source(Isotope.U238_CHAIN, 0.02F), STRONG = new Source(Isotope.RA226, 0.25F), ORE = new Source(Isotope.U238_CHAIN, 0.004F);
    /** Any solid block without an entry: about a 5 cm slab of rock. */
    static final Shielding SOLID = new Shielding(0.45F, 0.35F), PARTIAL = new Shielding(0.15F, 0.12F), NONE = new Shielding(0, 0);

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent e) {
        e.register(ITEM_SOURCE);
        e.register(BLOCK_SOURCE);
        e.register(SHIELDING);
    }

    public static Source source(ItemStack s) {
        if (s.isEmpty()) return null;
        @SuppressWarnings("deprecation") Source d = s.getItem().builtInRegistryHolder().getData(ITEM_SOURCE);
        if (d != null) return d;
        if (s.is(Sciences.STRONGLY_RADIOACTIVE)) return STRONG;
        if (s.is(Sciences.RADIOACTIVE)) return WEAK;
        return null;
    }

    public static Source source(BlockState s) {
        @SuppressWarnings("deprecation") Source d = s.getBlock().builtInRegistryHolder().getData(BLOCK_SOURCE);
        if (d != null) return d;
        return s.is(Sciences.RADIOACTIVE_BLOCKS) ? ORE : null;
    }

    /** What one block stops; air and plants stop nothing. */
    public static Shielding shielding(BlockState s, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        if (s.isAir()) return NONE;
        @SuppressWarnings("deprecation") Shielding d = s.getBlock().builtInRegistryHolder().getData(SHIELDING);
        if (d != null) return d;
        if (!s.getFluidState().isEmpty() && s.getCollisionShape(level, pos).isEmpty())
            return s.getFluidState().is(net.minecraft.tags.FluidTags.WATER) ? new Shielding(0.35F, 1.2F) : PARTIAL;
        if (s.getCollisionShape(level, pos).isEmpty()) return NONE;
        return s.isCollisionShapeFullBlock(level, pos) ? SOLID : PARTIAL;
    }

    private RadiationData() {}
}
