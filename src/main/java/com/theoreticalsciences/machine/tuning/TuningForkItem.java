package com.theoreticalsciences.machine.tuning;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.recipe.QHRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.2.0: the Tuning Fork. Used on a machine with a console (a multiblock's controller or a single-block console), it
 * opens that machine's calibration game (see {@link CalibrationGames}). Solved, the machine is set exactly at the
 * working point of the recipe it is running, as Laplace's Demon does in creative; then a Calibration Card can copy it.
 * Which game a machine plays, and how hard, is in data/theoreticalsciences/calibration/games.json (others: WAVE, tier 2).
 */
public class TuningForkItem extends LoreItem {
    /** The Resonant Tuning Fork: never wears out, and says how far a wrong answer is off. */
    private final boolean resonant;

    public TuningForkItem(Properties p) {
        this(p, false);
    }

    private TuningForkItem(Properties p, boolean resonant) {
        super(resonant ? p : p.durability(256));
        this.resonant = resonant;
    }

    public static TuningForkItem resonant(Properties p) {
        return new TuningForkItem(p, true);
    }

    private record Pending(BlockPos pos, int kind, long seed, int tier) {}

    private static final Map<UUID, Pending> PENDING = com.theoreticalsciences.world.SessionMaps.perPlayer(new java.util.concurrent.ConcurrentHashMap<>());   // 1.2.0: forgotten on logout
    private static Map<String, int[]> games;

    /** {game, tier} for a machine. */
    private static int[] gameFor(MinecraftServer server, String machine) {
        if (games == null) {
            games = new HashMap<>();
            try {
                var res = server.getResourceManager().getResource(TheoreticalSciences.id("calibration/games.json"));
                if (res.isPresent()) try (Reader r = res.get().openAsReader()) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    for (var e : o.entrySet()) {
                        JsonObject g = e.getValue().getAsJsonObject();
                        int kind = switch (g.get("game").getAsString()) {
                            case "sieve" -> CalibrationGames.SIEVE;
                            case "chain" -> CalibrationGames.CHAIN;
                            case "balance" -> CalibrationGames.BALANCE;
                            case "spectrum" -> CalibrationGames.SPECTRUM;
                            case "flow" -> CalibrationGames.FLOW;
                            case "build" -> CalibrationGames.BUILD;
                            case "oxidation" -> CalibrationGames.OXIDATION;
                            case "equilibrium" -> CalibrationGames.EQUILIBRIUM;
                            case "vertex" -> CalibrationGames.VERTEX;
                            case "symmetry" -> CalibrationGames.SYMMETRY;
                            case "cipher" -> CalibrationGames.CIPHER;
                            case "path" -> CalibrationGames.PATH;
                            case "tree" -> CalibrationGames.TREE;
                            case "nash" -> CalibrationGames.NASH;
                            default -> CalibrationGames.WAVE;
                        };
                        games.put(e.getKey(), new int[]{kind, g.has("tier") ? g.get("tier").getAsInt() : 0});
                    }
                }
            } catch (Exception ex) {
                TheoreticalSciences.LOGGER.warn("Could not read the calibration games: {}", ex.toString());
            }
        }
        return games.getOrDefault(machine, new int[]{CalibrationGames.WAVE, 0});
    }

    /** A machine's stage when the games file gives none: from how much energy it holds (100 kFE: 1 ... 1 GFE and up: 6). */
    private static int tierOf(ControllerBlockEntity c) {
        double e = Math.max(1, c.type.energyCapacity);
        return Math.max(1, Math.min(6, 1 + (int) Math.floor(Math.log10(e / 100_000.0) * 1.25)));
    }

    /** Whether the fork can calibrate this machine: a console's physics, or an older multiblock's tuning dial. */
    public static boolean tunable(ControllerBlockEntity c) {
        return c.hasPhysics() ? c.physics.model() != null : c.tuningProfile() != null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c) || !tunable(c)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.fork.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        int[] g = gameFor(level.getServer(), c.type.id());
        long seed = level.random.nextLong();
        int tier = Math.max(1, Math.min(6, g[1] > 0 ? g[1] : tierOf(c)));
        PENDING.put(sp.getUUID(), new Pending(c.getBlockPos(), g[0], seed, tier));
        PacketDistributor.sendToPlayer(sp, new Open(c.getBlockPos(), g[0], seed, tier | (resonant ? 16 : 0), c.type.color));
        level.playSound(null, c.getBlockPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.2F);
        return InteractionResult.CONSUME;
    }

    /** The player's answer: checked against the game the server gave them, then the machine is set. */
    public static void answer(ServerPlayer sp, Answer a) {
        Pending p = PENDING.get(sp.getUUID());
        if (p == null || !p.pos().equals(a.pos()) || p.seed() != a.seed()) return;
        if (!sp.blockPosition().closerThan(p.pos(), 12) || !(sp.level() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(p.pos()) instanceof ControllerBlockEntity c) || !tunable(c)) return;
        int[] ans = a.answer().stream().mapToInt(Integer::intValue).toArray();
        if (!CalibrationGames.check(p.kind(), p.seed(), p.tier(), ans)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.fork.wrong").withStyle(ChatFormatting.RED), true);
            level.playSound(null, p.pos(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6F, 0.6F);
            return;
        }
        ItemStack fork = sp.getMainHandItem().getItem() instanceof TuningForkItem ? sp.getMainHandItem()
                : sp.getOffhandItem().getItem() instanceof TuningForkItem ? sp.getOffhandItem() : ItemStack.EMPTY;
        if (fork.isEmpty()) return;          // 1.2.0: the fork must still be in hand (it wears with each tuning)
        PENDING.remove(sp.getUUID());
        if (!fork.isEmpty() && fork.isDamageableItem()) fork.hurtAndBreak(1, sp, sp.getMainHandItem() == fork ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        QHRecipe target = LaplaceDemonItem.pick(level, c, sp.getOffhandItem());
        boolean started = c.forkTune(level, target, res -> {
            if (!res.ok() && !res.coolant().isEmpty() && !res.coolant().equals(c.physics.coolant.getFluid().isEmpty() ? ""
                    : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(c.physics.coolant.getFluid().getFluid()).toString()))
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.fork.needs_coolant", String.format("%.0f", res.rate() * 100),
                        Component.translatable("fluid_type." + res.coolant().replace(':', '.'))).withStyle(ChatFormatting.GOLD), false);
            else sp.displayClientMessage(Component.translatable(res.ok() ? "message.theoreticalsciences.fork.tuned" : "message.theoreticalsciences.fork.partial",
                    String.format("%.0f", res.rate() * 100)).withStyle(res.ok() ? ChatFormatting.AQUA : ChatFormatting.GOLD), false);
            level.playSound(null, c.getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1F, 1.5F);
        });
        if (!started) sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_busy").withStyle(ChatFormatting.GRAY), true);
        else sp.displayClientMessage(Component.translatable("message.theoreticalsciences.fork.solved").withStyle(ChatFormatting.AQUA), true);
    }

    // ------------------------------------------------------------------ payloads
    /** Server -> client: open a calibration game. */
    /** tier: the stage (1..6), plus 16 when the fork gives hints; colour: the machine's accent. */
    public record Open(BlockPos pos, int kind, long seed, int tier, int colour) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(TheoreticalSciences.id("calibration_open"));
        public static final StreamCodec<ByteBuf, Open> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::pos, ByteBufCodecs.VAR_INT, Open::kind, ByteBufCodecs.VAR_LONG, Open::seed,
                ByteBufCodecs.VAR_INT, Open::tier, ByteBufCodecs.INT, Open::colour, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: the player's answer to a calibration game. */
    public record Answer(BlockPos pos, long seed, List<Integer> answer) implements CustomPacketPayload {
        public static final Type<Answer> TYPE = new Type<>(TheoreticalSciences.id("calibration_answer"));
        public static final StreamCodec<ByteBuf, Answer> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Answer::pos, ByteBufCodecs.VAR_LONG, Answer::seed,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), Answer::answer, Answer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
