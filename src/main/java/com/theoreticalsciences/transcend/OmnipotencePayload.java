package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHSounds;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** "Make me this": sent by the Omnipotence screen. The server checks the player really is at the Core. */
public record OmnipotencePayload(BlockPos core, String item, int count) implements CustomPacketPayload {
    public static final net.minecraft.tags.TagKey<Item> BEYOND = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            TheoreticalSciences.id("beyond_omnipotence"));
    public static final Type<OmnipotencePayload> TYPE = new Type<>(TheoreticalSciences.id("omnipotence"));
    public static final StreamCodec<FriendlyByteBuf, OmnipotencePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OmnipotencePayload::core, ByteBufCodecs.STRING_UTF8, OmnipotencePayload::item, ByteBufCodecs.VAR_INT, OmnipotencePayload::count,
            OmnipotencePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Round 28: the Core will not materialize anything of this mod. */
    public static boolean denied(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(TheoreticalSciences.MODID);
    }

    /**
     * Round 29: at most this many requests per player per second - a modded client could otherwise ask thousands of
     * times a second and flood the server with items. (Weak map: a player who leaves is forgotten automatically.)
     */
    private static final int MAX_PER_SECOND = 10;
    private static final java.util.Map<ServerPlayer, long[]> RATE = new java.util.WeakHashMap<>();

    private static boolean tooFast(ServerPlayer sp) {
        long now = System.nanoTime();
        long[] w = RATE.computeIfAbsent(sp, k -> new long[]{now, 0});    // {when this second began, requests in it}
        if (now - w[0] >= 1_000_000_000L) {
            w[0] = now;
            w[1] = 0;
        }
        return ++w[1] > MAX_PER_SECOND;
    }

    public static void handle(ServerPlayer sp, OmnipotencePayload p) {
        if (tooFast(sp)) return;
        if (!sp.blockPosition().closerThan(p.core(), 12)) return;
        boolean forged = ForgedCoreBlock.awake(sp.level(), p.core());     // round 27: an awake forged Core, anywhere
        if (!forged && (sp.level().dimension() != QHDimensions.TRANSCENDENTAL || !sp.level().getBlockState(p.core()).is(TranscendRegistry.CORE.get()))) return;
        ResourceLocation id = ResourceLocation.tryParse(p.item());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return;
        // round 28: nothing of this mod can be materialized - it is listed, and the Core refuses
        if (denied(item)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.core_access_denied", new ItemStack(item).getHoverName())
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
            return;
        }
        // beyond Omnipotence: the Sundering's and the Forge's materials and everything made from them
        if (new ItemStack(item).is(BEYOND)) return;
        if (CoreForge.forgeOnly(item)) return;            // creative items: only the rebuilt Core's Forge
        Omnipotence.Phase ph = Omnipotence.phase(sp.server);
        if (!forged && ph != Omnipotence.Phase.INTACT && ph != Omnipotence.Phase.REBORN) return;
        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.max(1, Math.min(p.count(), stack.getMaxStackSize())));
        if (!sp.getInventory().add(stack)) sp.drop(stack, false);
        sp.level().playSound(null, sp.blockPosition(), QHSounds.OMNIPOTENCE_GRANT.get(), SoundSource.PLAYERS, 0.6F, 0.9F + sp.getRandom().nextFloat() * 0.2F);
    }
}
