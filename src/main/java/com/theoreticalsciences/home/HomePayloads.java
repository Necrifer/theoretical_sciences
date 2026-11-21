package com.theoreticalsciences.home;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Recreating Home's packets: the checklist (to the Core's screen) and the Empyrean Assembly Table's picture and clicks. */
public final class HomePayloads {
    private HomePayloads() {}

    // ------------------------------------------------------------------ the checklist
    /** Server -> client, with the Core's screen: whether the tab shows, and how much of each material has been given. */
    public record State(boolean unlocked, long[] given) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(TheoreticalSciences.id("home_state"));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of((buf, s) -> {
            buf.writeBoolean(s.unlocked);
            buf.writeLongArray(s.given);
        }, buf -> new State(buf.readBoolean(), buf.readLongArray()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void sendState(ServerPlayer sp) {
        boolean un = HomeProgress.unlocked(sp.server, sp.getUUID());
        PacketDistributor.sendToPlayer(sp, new State(un, un ? HomeProgress.given(sp.server, sp.getUUID()) : new long[0]));
        if (un && HomeProgress.greetOnce(sp.server, sp.getUUID()))
            for (int i = 1; i <= 4; i++)
                sp.sendSystemMessage(Component.translatable("message.theoreticalsciences.home.greeting." + i).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }

    /** Client -> server: give the Core everything carried that the checklist still needs. */
    public record Submit(BlockPos core) implements CustomPacketPayload {
        public static final Type<Submit> TYPE = new Type<>(TheoreticalSciences.id("home_submit"));
        public static final StreamCodec<FriendlyByteBuf, Submit> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Submit::core, Submit::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ServerPlayer sp, Submit p) {
            if (!sp.blockPosition().closerThan(p.core(), 12) || !com.theoreticalsciences.transcend.CorePayloads.inSession(sp, p.core())) return;
            if (!HomeProgress.unlocked(sp.server, sp.getUUID())) return;
            long n = HomeProgress.submitInventory(sp);
            if (n > 0) sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.2F);
            announceIfComplete(sp);
            sendState(sp);
        }
    }

    /** The Core speaks once when the last line fills. */
    public static void announceIfComplete(ServerPlayer sp) {
        if (HomeProgress.complete(sp.server, sp.getUUID()) && HomeProgress.finishOnce(sp.server, sp.getUUID())) {
            for (int i = 1; i <= 4; i++)
                sp.sendSystemMessage(Component.translatable("message.theoreticalsciences.home.complete." + i).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.6F);
        }
    }

    // ------------------------------------------------------------------ the table
    /** Server -> client: the table as it stands (its saved form), and why it cannot craft ("" if it can). */
    public record TableSync(BlockPos pos, CompoundTag tag, String blocked) implements CustomPacketPayload {
        public static final Type<TableSync> TYPE = new Type<>(TheoreticalSciences.id("empyrean_sync"));
        public static final StreamCodec<FriendlyByteBuf, TableSync> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TableSync::pos, ByteBufCodecs.COMPOUND_TAG, TableSync::tag, ByteBufCodecs.stringUtf8(64), TableSync::blocked, TableSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Client -> server: 0 choose recipe {@code arg}; 1 put in what the cursor carries; 2 take product (arg "all": into
     * the inventory, else a stack onto the cursor); 3 craft (arg "1" or "max"); 4 automatic on/off; 5 take back the
     * ingredient {@code arg} (an item id; ";all" after it: into the inventory).
     */
    public record TableAction(BlockPos pos, int action, String arg) implements CustomPacketPayload {
        public static final Type<TableAction> TYPE = new Type<>(TheoreticalSciences.id("empyrean_action"));
        public static final StreamCodec<FriendlyByteBuf, TableAction> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TableAction::pos, ByteBufCodecs.VAR_INT, TableAction::action, ByteBufCodecs.stringUtf8(256), TableAction::arg, TableAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ServerPlayer sp, TableAction a) {
            if (!(sp.containerMenu instanceof EmpyreanTableMenu menu) || !menu.be.getBlockPos().equals(a.pos()) || !menu.stillValid(sp)) return;
            EmpyreanTableBlockEntity be = menu.be;
            switch (a.action()) {
                case 0 -> be.select(a.arg().isEmpty() ? null : ResourceLocation.tryParse(a.arg()));
                case 1 -> {
                    ItemStack c = menu.getCarried();
                    if (!c.isEmpty() && c.getComponentsPatch().isEmpty()) {
                        long n = be.put(c.getItem(), c.getCount());
                        if (n > 0) {
                            c.shrink((int) n);
                            menu.setCarried(c);
                        }
                    }
                }
                case 2 -> give(sp, menu, be.outItem(), a.arg().equals("all"), be::takeOut, be.outCount());
                case 3 -> be.craft(a.arg().equals("max") ? 1024 : 1);
                case 4 -> be.toggleAuto();
                case 5 -> {
                    String[] parts = a.arg().split(";", 2);
                    ResourceLocation id = ResourceLocation.tryParse(parts[0]);
                    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
                    Item item = BuiltInRegistries.ITEM.get(id);
                    give(sp, menu, item, parts.length > 1, n -> be.takeBack(item, n), be.held().getOrDefault(item, 0L));
                }
                default -> {
                }
            }
            menu.broadcastChanges();
        }

        private interface Taker {
            long take(long n);
        }

        /** Onto the cursor (a stack at most), or into the inventory (as much as fits). */
        private static void give(ServerPlayer sp, EmpyreanTableMenu menu, Item item, boolean toInventory, Taker taker, long available) {
            if (available <= 0 || item == net.minecraft.world.item.Items.AIR) return;
            int max = item.getDefaultMaxStackSize();
            if (!toInventory) {
                ItemStack c = menu.getCarried();
                if (!c.isEmpty() && (c.getItem() != item || !c.getComponentsPatch().isEmpty() || c.getCount() >= max)) return;
                int room = max - c.getCount();
                int n = (int) taker.take(Math.min(room, available));
                if (n > 0) menu.setCarried(new ItemStack(item, c.getCount() + n));
                return;
            }
            var inv = sp.getInventory();
            for (int guard = 0; guard < 64 && available > 0; guard++) {
                int want = (int) Math.min(max, available);
                ItemStack st = new ItemStack(item, want);
                if (!inv.add(st)) {                          // (add() leaves in the stack what did not fit)
                    int fitted = want - st.getCount();
                    if (fitted > 0) taker.take(fitted);
                    break;
                }
                taker.take(want);
                available -= want;
            }
        }
    }
}
