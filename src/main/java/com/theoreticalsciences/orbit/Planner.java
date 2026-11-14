package com.theoreticalsciences.orbit;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.12.0: the Mission Planner. The Transfer Console (crewed trips) and the Mission Bay (probes) open the same screen:
 * the destinations or targets, what each costs now and at its window, and a porkchop plot of departure day against
 * time of flight. The server sends what the screen shows ({@link Open}), and decides and charges when the player asks to
 * go ({@link Go}); the client never says what anything costs.
 */
public final class Planner {
    private Planner() {}

    public record Entry(String id, String name, double deltaV, double synodic, double penalty, double offset, boolean free, double transferDays) {
        static final StreamCodec<FriendlyByteBuf, Entry> CODEC = StreamCodec.of((b, e) -> {
            b.writeUtf(e.id);
            b.writeUtf(e.name);
            b.writeDouble(e.deltaV);
            b.writeDouble(e.synodic);
            b.writeDouble(e.penalty);
            b.writeDouble(e.offset);
            b.writeBoolean(e.free);
            b.writeDouble(e.transferDays);
        }, b -> new Entry(b.readUtf(), b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean(), b.readDouble()));
    }

    /** Server → client: open (or refresh) the planner. */
    public record Open(boolean bay, BlockPos pos, double day, double stageDv, boolean probe, int busyTicks, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(TheoreticalSciences.id("planner_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.of((b, o) -> {
            b.writeBoolean(o.bay);
            b.writeBlockPos(o.pos);
            b.writeDouble(o.day);
            b.writeDouble(o.stageDv);
            b.writeBoolean(o.probe);
            b.writeVarInt(o.busyTicks);
            b.writeVarInt(o.entries.size());
            for (Entry e : o.entries) Entry.CODEC.encode(b, e);
        }, b -> {
            boolean bay = b.readBoolean();
            BlockPos pos = b.readBlockPos();
            double day = b.readDouble(), dv = b.readDouble();
            boolean probe = b.readBoolean();
            int busy = b.readVarInt();
            int n = Math.min(64, b.readVarInt());
            List<Entry> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(Entry.CODEC.decode(b));
            return new Open(bay, pos, day, dv, probe, busy, list);
        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client → server: go to (or send a probe to) this destination, taking this fraction of the Hohmann time. */
    public record Go(boolean bay, BlockPos pos, String id, double tof) implements CustomPacketPayload {
        public static final Type<Go> TYPE = new Type<>(TheoreticalSciences.id("planner_go"));
        public static final StreamCodec<FriendlyByteBuf, Go> CODEC = StreamCodec.of((b, g) -> {
            b.writeBoolean(g.bay);
            b.writeBlockPos(g.pos);
            b.writeUtf(g.id);
            b.writeDouble(g.tof);
        }, b -> new Go(b.readBoolean(), b.readBlockPos(), b.readUtf(), b.readDouble()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static double today(ServerPlayer sp) {
        return MissionMath.day(sp.server.overworld().getGameTime());
    }

    // ------------------------------------------------------------------ opening
    public static void openConsole(ServerPlayer sp, BlockPos pos) {
        OrbitData data = OrbitData.get(sp.server);
        List<Entry> list = new ArrayList<>();
        for (Destinations.Destination d : Destinations.INSTANCE.offered(sp))
            list.add(new Entry(d.id().toString(), d.name(), d.deltaV(), d.synodic(), d.penalty(), d.offset(), d.home() || data.charted(d.id()), 0));
        PacketDistributor.sendToPlayer(sp, new Open(false, pos, today(sp), MissionMath.best(sp.getInventory().items), false, 0, list));
    }

    static void openBay(ServerPlayer sp, MissionBay.BayEntity bay) {
        List<Entry> list = new ArrayList<>();
        for (Missions.Mission m : Missions.INSTANCE.offered(sp))
            list.add(new Entry(m.id().toString(), m.name(), m.deltaV(), m.synodic(), m.penalty(), m.offset(), false, m.transferDays()));
        PacketDistributor.sendToPlayer(sp, new Open(true, bay.getBlockPos(), today(sp), MissionMath.best(bay.stages()), bay.hasProbe(), bay.busyTicks(), list));
    }

    // ------------------------------------------------------------------ going
    static String kms(double ms) {
        return String.format(Locale.ROOT, "%.1f", ms / 1000);
    }

    public static void handle(ServerPlayer sp, Go g) {
        if (sp.level().dimension() != QHDimensions.ORBITAL_STATION || !sp.level().isLoaded(g.pos()) || !sp.blockPosition().closerThan(g.pos(), 8)) return;
        ResourceLocation id = ResourceLocation.tryParse(g.id());
        if (id == null) return;
        double tof = Math.max(0.5, Math.min(2.5, g.tof()));
        double day = today(sp);
        if (g.bay()) {
            if (!(sp.level().getBlockEntity(g.pos()) instanceof MissionBay.BayEntity bay) || bay.mission != null || !bay.hasProbe()) return;
            Missions.Mission m = Missions.INSTANCE.byId(id);
            if (m == null || !m.offered(sp)) return;
            double cost = MissionMath.cost(m.deltaV(), m.synodic(), m.penalty(), m.offset(), day, tof);
            MissionMath.Plan plan = MissionMath.choose(bay.stages(), cost);
            if (plan == null) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.planner_short", kms(cost), kms(MissionMath.best(bay.stages())))
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            bay.owner = sp.getUUID();
            bay.launch(m, plan, tof);
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.bay_launched", Component.translatable(m.name()),
                    String.valueOf(Math.max(1, Math.round(MissionMath.missionTicks(m.transferDays(), tof) / 1200.0)))).withStyle(ChatFormatting.AQUA), false);
            return;
        }
        if (!(sp.level().getBlockState(g.pos()).getBlock() instanceof OrbitTravel.ConsoleBlock)) return;
        Destinations.Destination d = Destinations.INSTANCE.byId(id);
        if (d == null || !d.offered(sp)) return;
        if (!pay(sp, d, day)) return;
        OrbitTravel.depart(sp, d);
    }

    /** Charges a crewed trip from the player's stages (nothing for home or a charted route); false if they cannot pay. */
    static boolean pay(ServerPlayer sp, Destinations.Destination d, double day) {
        if (d.home() || OrbitData.get(sp.server).charted(d.id())) return true;
        double cost = MissionMath.cost(d.deltaV(), d.synodic(), d.penalty(), d.offset(), day, 1);
        List<ItemStack> inv = sp.getInventory().items;
        MissionMath.Plan plan = MissionMath.choose(inv, cost);
        if (plan == null) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.planner_short", kms(cost), kms(MissionMath.best(inv)))
                    .withStyle(ChatFormatting.RED), false);
            return false;
        }
        StringBuilder names = new StringBuilder();
        for (int slot : plan.slots()) {
            if (names.length() > 0) names.append(", ");
            names.append(inv.get(slot).getHoverName().getString());
        }
        for (int slot : plan.slots()) inv.get(slot).shrink(1);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.planner_paid", names.toString(), kms(plan.dv())).withStyle(ChatFormatting.GOLD), false);
        return true;
    }
}
