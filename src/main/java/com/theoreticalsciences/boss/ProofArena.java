package com.theoreticalsciences.boss;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * The Living Proof: the dimension that is the first half of the fight against Alpha Omega. It is built fresh for every
 * challenger as a line of eight sealed chambers - each one a law of nature or of mathematics, alive and hostile - and
 * an arena beyond them. A chamber's membrane only dissolves once its law is understood:
 * <ol start="0">
 *   <li><b>The Measurement Problem</b> - the exit moves whenever you look at it.</li>
 *   <li><b>Proper Time</b> - your clock only runs while you are at rest; the singularity strikes where you stand.</li>
 *   <li><b>The Diagonal</b> - write a number missing from the list on the wall (Cantor's diagonal argument).</li>
 *   <li><b>Hilbert's Hotel</b>, <b>the Platonic Forms</b>, <b>the Bulk</b>, <b>Foundations</b>, <b>the Codex</b> - five questions each,
 *       answered at the pedestals; a wrong answer makes the chamber bite.</li>
 * </ol>
 * When the last chamber yields the whole proof melts away from the far end and Alpha Omega descends into the arena.
 */
public class ProofArena extends SavedData {
    public static final String NAME = "theoreticalsciences_living_proof";
    public static final int CHAMBERS = 8, STRIDE = 24, Y = 64, Z = 0, ARENA_R = 24;
    public static final String TAG = "ts_proof";

    public enum Phase { TRIALS, MELT, BOSS, DONE, FAILED }

    public static final class Session {
        public UUID player;
        public int index;
        public int chamber;          // 0-7 trial chambers
        public int stage;            // questions answered in a row in a quiz chamber
        public int question = -1, correct = -1;
        public long grid;            // the diagonal chamber's list of numbers (36 bits)
        public Phase phase = Phase.TRIALS;
        public int meltX;            // how far the melt has advanced
        public UUID boss;
        public boolean returnOmnilex, announced;
        // transient
        int plate = -1, plateCooldown, properTime, offline;
        int ownerAway;               // round 29: ticks the challenger has been logged out (companions are sent home after 30 s)
        Vec3 lastPos;
        final java.util.Set<String> asked = new java.util.HashSet<>();     // questions already put to them in this chamber
        boolean drawn;      // the Diagonal's labels were (re)drawn by this server session
        long strikeAt = -1;
        Vec3 strike;

        public int x0() {
            return (index + 1) * 4096;
        }

        public int chamberX(int c) {
            return x0() + c * STRIDE;
        }

        public BlockPos arenaCenter() {
            return new BlockPos(chamberX(CHAMBERS) + 26, Y, Z);
        }

        public BlockPos entry() {
            return new BlockPos(x0() + 2, Y, Z);
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", player);
            t.putInt("Index", index);
            t.putInt("Chamber", chamber);
            t.putInt("Stage", stage);
            t.putInt("Question", question);
            t.putInt("Correct", correct);
            t.putLong("Grid", grid);
            t.putString("Phase", phase.name());
            t.putInt("Melt", meltX);
            if (boss != null) t.putUUID("Boss", boss);
            t.putBoolean("Return", returnOmnilex);
            t.putBoolean("Announced", announced);
            return t;
        }

        static Session load(CompoundTag t) {
            Session s = new Session();
            s.player = t.getUUID("Player");
            s.index = t.getInt("Index");
            s.chamber = t.getInt("Chamber");
            s.stage = t.getInt("Stage");
            s.question = t.getInt("Question");
            s.correct = t.getInt("Correct");
            s.grid = t.getLong("Grid");
            try {
                s.phase = Phase.valueOf(t.getString("Phase"));
            } catch (IllegalArgumentException e) {
                s.phase = Phase.FAILED;
            }
            s.meltX = t.getInt("Melt");
            if (t.hasUUID("Boss")) s.boss = t.getUUID("Boss");
            s.returnOmnilex = t.getBoolean("Return");
            s.announced = t.getBoolean("Announced");
            return s;
        }
    }

    final Map<UUID, Session> sessions = new HashMap<>();
    int next;
    public final Set<UUID> conquered = new HashSet<>();

    public static ProofArena get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ProofArena::new, ProofArena::load, null), NAME);
    }

    static ProofArena load(CompoundTag t, HolderLookup.Provider r) {
        ProofArena a = new ProofArena();
        a.next = t.getInt("Next");
        for (net.minecraft.nbt.Tag s : t.getList("Sessions", 10)) {
            Session ss = Session.load((CompoundTag) s);
            a.sessions.put(ss.player, ss);
        }
        for (net.minecraft.nbt.Tag c : t.getList("Conquered", 11)) a.conquered.add(net.minecraft.nbt.NbtUtils.loadUUID(c));
        return a;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        t.putInt("Next", next);
        ListTag l = new ListTag();
        sessions.values().forEach(s -> l.add(s.save()));
        t.put("Sessions", l);
        ListTag c = new ListTag();
        conquered.forEach(u -> c.add(net.minecraft.nbt.NbtUtils.createUUID(u)));
        t.put("Conquered", c);
        return t;
    }

    public Session of(UUID player) {
        return sessions.get(player);
    }

    public boolean active(UUID player) {
        Session s = sessions.get(player);
        return s != null && (s.phase == Phase.TRIALS || s.phase == Phase.MELT || s.phase == Phase.BOSS);
    }

    // ================================================================== beginning
    /** Called at the end of the genesis animation: build a proof for this challenger and pull them in. */
    public static void begin(ServerPlayer owner, List<ServerPlayer> companions) {
        MinecraftServer server = owner.server;
        ServerLevel lp = server.getLevel(QHDimensions.LIVING_PROOF);
        if (lp == null) return;
        ProofArena a = get(server);
        Session s = new Session();
        s.player = owner.getUUID();
        s.index = a.next++;
        s.returnOmnilex = true;          // until it is won, the Omnilex is only lent
        a.sessions.put(s.player, s);
        a.setDirty();
        build(lp, s);
        setupChamber(lp, s);
        BlockPos e = s.entry();
        List<ServerPlayer> all = new ArrayList<>(companions);
        if (!all.contains(owner)) all.add(0, owner);
        for (ServerPlayer p : all) {
            com.theoreticalsciences.world.DimensionGate.allow(p, QHDimensions.LIVING_PROOF);      // round 35: the intended way in
            p.changeDimension(new DimensionTransition(lp, Vec3.atBottomCenterOf(e), Vec3.ZERO, -90, 0, DimensionTransition.DO_NOTHING));
            com.theoreticalsciences.world.DimensionGate.revoke(p);
            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0, false, false));
            title(p, Component.translatable("proof.theoreticalsciences.enter_title").withStyle(ChatFormatting.GOLD),
                    Component.translatable("proof.theoreticalsciences.enter_sub").withStyle(ChatFormatting.GRAY));
        }
        lp.playSound(null, e, com.theoreticalsciences.registry.QHSounds.EMERGENCE.get(), SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    // ================================================================== construction
    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    static void build(ServerLevel l, Session s) {
        BlockState wall = b("proof_wall"), floor = b("proof_floor"), membrane = b("proof_membrane");
        int x0 = s.x0();
        for (int c = 0; c < CHAMBERS; c++) {
            int cx = s.chamberX(c);
            for (int x = cx - 1; x <= cx + 23; x++)
                for (int z = Z - 11; z <= Z + 11; z++) {
                    l.setBlock(new BlockPos(x, Y - 1, z), floor, 2);
                    l.setBlock(new BlockPos(x, Y + 12, z), wall, 2);
                    boolean edge = z == Z - 11 || z == Z + 11 || x == cx - 1 || x >= cx + 21;
                    if (edge)
                        for (int y = Y; y < Y + 12; y++) {
                            boolean door = (x >= cx + 21 || x == cx - 1 && c > 0) && Math.abs(z - Z) <= 1 && y <= Y + 2;
                            l.setBlock(new BlockPos(x, y, z), door ? membrane : wall, 2);
                        }
                }
            // a few veins of light in the ceiling - the proof is alive
            for (int i = 0; i < 6; i++) l.setBlock(new BlockPos(cx + 3 + i * 3, Y + 12, Z + (i % 2 == 0 ? -4 : 4)), b("proof_membrane"), 2);
        }
        // the arena: a disc, ringed by a wall, beyond the last membrane
        BlockPos ac = s.arenaCenter();
        for (int x = -ARENA_R - 1; x <= ARENA_R + 1; x++)
            for (int z = -ARENA_R - 1; z <= ARENA_R + 1; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= ARENA_R + 0.5) l.setBlock(ac.offset(x, -1, z), floor, 2);
                if (r > ARENA_R - 0.5 && r <= ARENA_R + 1.0)
                    for (int y = 0; y < 14; y++) l.setBlock(ac.offset(x, y, z), wall, 2);
            }
        // six pillars to hide behind
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3;
            int px = (int) Math.round(Math.cos(ang) * 14), pz = (int) Math.round(Math.sin(ang) * 14);
            for (int y = 0; y < 7; y++)
                for (int dx = 0; dx < 2; dx++)
                    for (int dz = 0; dz < 2; dz++) l.setBlock(ac.offset(px + dx, y, pz + dz), wall, 2);
        }
        // the walkway from the last chamber into the arena, with an opening in the ring wall
        int lastEnd = s.chamberX(CHAMBERS - 1) + 23;
        for (int x = lastEnd + 1; x <= ac.getX() - ARENA_R + 1; x++)
            for (int z = Z - 1; z <= Z + 1; z++) {
                l.setBlock(new BlockPos(x, Y - 1, z), floor, 2);
                for (int y = 0; y < 3; y++) l.setBlock(new BlockPos(x, Y + y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        l.setBlock(new BlockPos(x0 + 2, Y - 1, Z), b("proof_floor"), 2);
    }

    // ================================================================== chamber setup
    static final String[] KEYS = {"measurement", "proper_time", "diagonal", "hotel", "forms", "bulk", "foundations", "codex"};

    static List<ProofQuestions.Q> bank(int chamber) {
        return switch (chamber) {
            case 3 -> ProofQuestions.TRANSFINITE;
            case 4 -> ProofQuestions.FORMS;
            case 5 -> ProofQuestions.BULK;
            case 6 -> ProofQuestions.FOUNDATIONS;
            default -> ProofQuestions.CODEX;
        };
    }

    static String tag(Session s, int c) {
        return TAG + "_" + s.index + "_" + c;
    }

    static AABB chamberBox(Session s, int c) {
        int cx = s.chamberX(c);
        return new AABB(cx - 1, Y - 1, Z - 11, cx + 24, Y + 13, Z + 12);
    }

    static void clearDisplays(ServerLevel l, Session s, int c) {
        String tg = tag(s, c);
        for (Entity e : l.getEntities((Entity) null, chamberBox(s, c), e -> e.getTags().contains(tg))) e.discard();
    }

    static void setupChamber(ServerLevel l, Session s) {
        int c = s.chamber, cx = s.chamberX(c);
        clearDisplays(l, s, c);
        s.announced = false;
        s.stage = 0;
        s.asked.clear();
        String tg = tag(s, c);
        text(l, new Vec3(cx + 2.5, Y + 6, Z + 0.5), Component.translatable("proof.theoreticalsciences." + KEYS[c] + ".title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 2.0F, 200, tg);
        text(l, new Vec3(cx + 2.5, Y + 4.2, Z + 0.5), Component.translatable("proof.theoreticalsciences." + KEYS[c] + ".rule").withStyle(ChatFormatting.WHITE), 0.8F, 260, tg);
        switch (c) {
            case 0 -> {
                s.plate = -1;
                movePlate(l, s, null);
            }
            case 1 -> {
                s.properTime = 0;
                s.strikeAt = -1;
            }
            case 2 -> setupDiagonal(l, s, tg);
            default -> askQuestion(l, s);
        }
    }

    // ---------------------------------------------------------------- 0: the measurement problem
    static BlockPos[] plateSpots(Session s) {
        int cx = s.chamberX(0);
        return new BlockPos[]{new BlockPos(cx + 5, Y - 1, Z - 7), new BlockPos(cx + 9, Y - 1, Z + 8), new BlockPos(cx + 13, Y - 1, Z - 3),
                new BlockPos(cx + 17, Y - 1, Z + 6), new BlockPos(cx + 18, Y - 1, Z - 8), new BlockPos(cx + 11, Y - 1, Z + 1),
                new BlockPos(cx + 6, Y - 1, Z + 5)};
    }

    static void movePlate(ServerLevel l, Session s, ServerPlayer p) {
        BlockPos[] spots = plateSpots(s);
        if (s.plate >= 0) {
            l.setBlock(spots[s.plate], b("proof_floor"), 2);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), spots[s.plate].getX() + 0.5, Y + 0.2, spots[s.plate].getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.05);
        }
        int n;
        do n = l.random.nextInt(spots.length);
        while (n == s.plate || p != null && spots[n].distToCenterSqr(p.position()) < 25);
        s.plate = n;
        l.setBlock(spots[n], b("observer_plate"), 2);
        l.playSound(null, spots[n], com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.BLOCKS, 1F, 1.8F);
    }

    static void tickMeasurement(ServerLevel l, Session s, ServerPlayer p) {
        if (s.plate < 0) movePlate(l, s, p);
        BlockPos plate = plateSpots(s)[s.plate];
        Vec3 top = Vec3.atCenterOf(plate).add(0, 0.5, 0);
        if (p.onGround() && p.blockPosition().below().equals(plate)) {
            solve(l, s, p);
            return;
        }
        if (s.plateCooldown > 0) {
            s.plateCooldown--;
            return;
        }
        Vec3 to = top.subtract(p.getEyePosition());
        double dist = to.length();
        if (dist > 2.5 && to.normalize().dot(p.getLookAngle()) > Math.cos(Math.toRadians(22))) {
            movePlate(l, s, p);
            s.plateCooldown = 10;
            p.displayClientMessage(Component.translatable("proof.theoreticalsciences.measurement.observed").withStyle(ChatFormatting.AQUA), true);
        }
    }

    // ---------------------------------------------------------------- 1: proper time
    static final int PROPER_TARGET = 400;

    static void tickProperTime(ServerLevel l, Session s, ServerPlayer p) {
        // The server never sees a player's velocity (movement is decided on the client), so measure how far they
        // actually went since the last tick: any walking, jumping or being pushed stops the clock.
        Vec3 pos = p.position();
        double moved = s.lastPos == null ? 1 : pos.distanceToSqr(s.lastPos);
        s.lastPos = pos;
        boolean still = p.onGround() && moved < 1e-6;
        if (still) s.properTime++;
        else if (moved > 0.04) s.properTime = Math.max(0, s.properTime - (p.isSprinting() ? 3 : 1));
        if (l.getGameTime() % 5 == 0)
            p.displayClientMessage(Component.translatable("proof.theoreticalsciences.proper_time.clock", String.format("%.1f", s.properTime / 20.0),
                    String.format("%.0f", PROPER_TARGET / 20.0)).withStyle(still ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        long now = l.getGameTime();
        if (s.strikeAt < 0) {
            s.strikeAt = now + 70 + l.random.nextInt(30);
            s.strike = null;
        }
        long nowI = now;
        if (s.strike == null && nowI >= s.strikeAt - 30) {
            s.strike = p.position();
            l.playSound(null, BlockPos.containing(s.strike), com.theoreticalsciences.registry.QHSounds.CHARGE.get(), SoundSource.HOSTILE, 0.8F, 1.6F);
        }
        if (s.strike != null) {
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 + now * 0.2;
                l.sendParticles(com.theoreticalsciences.registry.QHGlowOptions.of(1F, 0.3F, 0.1F, 1.2F), s.strike.x + Math.cos(a) * 1.4, Y + 0.1, s.strike.z + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
            }
            if (nowI >= s.strikeAt) {
                l.sendParticles(com.theoreticalsciences.registry.QHParticles.SHOCKWAVE.get(), s.strike.x, Y + 1, s.strike.z, 1, 0, 0, 0, 0);
                if (p.position().distanceToSqr(s.strike.x, p.getY(), s.strike.z) < 1.6 * 1.6) {
                    p.hurt(p.damageSources().magic(), 6F);
                    p.addEffect(new MobEffectInstance(QHEffects.SPAGHETTIFICATION, 60, 0));
                    s.properTime = Math.max(0, s.properTime - 60);
                }
                s.strikeAt = -1;
            }
        }
        if (s.properTime >= PROPER_TARGET) solve(l, s, p);
    }

    // ---------------------------------------------------------------- 2: the diagonal
    static BlockPos lamp(Session s, int row, int col) {
        return new BlockPos(s.chamberX(2) + 7 + col, Y + 7 - row, Z - 10);
    }

    static BlockPos toggle(Session s, int col) {
        return new BlockPos(s.chamberX(2) + 7 + col, Y, Z - 6);
    }

    static BlockPos submit(Session s) {
        return new BlockPos(s.chamberX(2) + 15, Y, Z - 6);
    }

    static final String BIT = TAG + "_bit";

    static final String GRID = TAG + "_grid";

    /** A fresh list of six numbers - never the one just shown, so a wrong answer cannot simply be tried again. */
    static long newGrid(ServerLevel l, long old) {
        long g;
        do g = l.random.nextLong() & ((1L << 36) - 1);
        while (Long.bitCount(g) < 8 || diagonal(g) == diagonal(old));
        return g;
    }

    static int diagonal(long g) {
        int d = 0;
        for (int i = 0; i < 6; i++) d |= (int) ((g >> (i * 6 + i)) & 1) << i;
        return d;
    }

    static void setupDiagonal(ServerLevel l, Session s, String tg) {
        s.grid = newGrid(l, s.grid ^ 1);
        for (int c = 0; c < 6; c++) l.setBlock(toggle(s, c), b("axiom_toggle").setValue(BlockStateProperties.LIT, false), 2);
        l.setBlock(submit(s), b("answer_pedestal"), 2);
        drawDiagonal(l, s, tg);
    }

    /** The wall shows six numbers, one per row, as lamps with their digits written on them; nothing else is explained. */
    static void drawDiagonal(ServerLevel l, Session s, String tg) {
        s.drawn = true;
        drawLamps(l, s, tg);
        for (int c = 0; c < 6; c++) bit(l, s, c);
        textB(l, Vec3.atCenterOf(toggle(s, 0)).add(-1.6, 0.9, 0), Component.translatable("proof.theoreticalsciences.diagonal.yours").withStyle(ChatFormatting.YELLOW), 0.6F, 80, "vertical", tg);
        textB(l, Vec3.atCenterOf(submit(s)).add(0, 1.3, 0), Component.translatable("proof.theoreticalsciences.diagonal.submit").withStyle(ChatFormatting.AQUA), 0.7F, 120, "vertical", tg);
    }

    /** The six numbers on the wall. */
    static void drawLamps(ServerLevel l, Session s, String tg) {
        for (Entity e : l.getEntities((Entity) null, chamberBox(s, 2), e -> e.getTags().contains(GRID))) e.discard();
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 6; c++) {
                boolean one = (s.grid >> (r * 6 + c) & 1) == 1;
                l.setBlock(lamp(s, r, c), b("axiom_lamp").setValue(BlockStateProperties.LIT, one), 2);
                // every lamp shows its digit - and nothing more: finding the diagonal is the player's job
                textB(l, Vec3.atCenterOf(lamp(s, r, c)).add(0, -0.25, 0.75), Component.literal(one ? "1" : "0").withStyle(ChatFormatting.WHITE), 0.9F, 40, "vertical", tg, GRID);
            }
    }

    /** The digit shown over one of the player's switches. */
    static void bit(ServerLevel l, Session s, int c) {
        BlockPos t = toggle(s, c);
        for (Entity e : l.getEntities((Entity) null, new AABB(t).inflate(0.6, 2, 0.6), e -> e.getTags().contains(BIT))) e.discard();
        boolean on = l.getBlockState(t).getOptionalValue(BlockStateProperties.LIT).orElse(false);
        textB(l, Vec3.atCenterOf(t).add(0, 1.1, 0), Component.literal(on ? "1" : "0").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), 1.0F, 40, "vertical",
                tag(s, 2), BIT);
    }

    /** A switch was flipped: update the digit above it. */
    public static void onToggle(ServerLevel l, BlockPos pos) {
        for (Session s : get(l.getServer()).sessions.values()) {
            if (s.phase != Phase.TRIALS || s.chamber != 2) continue;
            for (int c = 0; c < 6; c++) if (toggle(s, c).equals(pos)) bit(l, s, c);
        }
    }

    static void checkDiagonal(ServerLevel l, Session s, ServerPlayer p) {
        List<Integer> same = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            boolean listed = (s.grid >> (i * 6 + i) & 1) == 1;
            boolean mine = l.getBlockState(toggle(s, i)).getOptionalValue(BlockStateProperties.LIT).orElse(false);
            if (listed == mine) same.add(i + 1);
        }
        if (same.isEmpty()) {
            solve(l, s, p);
            return;
        }
        // no hints about where it went wrong - only that it did; and the list is rewritten, so the answer changes
        punish(l, s, p);
        if (s.phase != Phase.TRIALS || s.chamber != 2) return;
        s.grid = newGrid(l, s.grid);
        get(l.getServer()).setDirty();
        drawLamps(l, s, tag(s, 2));
        for (int c = 0; c < 6; c++) {
            l.setBlock(toggle(s, c), b("axiom_toggle").setValue(BlockStateProperties.LIT, false), 2);
            bit(l, s, c);
        }
        l.playSound(null, submit(s), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1F, 0.5F);
        p.sendSystemMessage(Component.translatable("proof.theoreticalsciences.diagonal.wrong").withStyle(ChatFormatting.RED));
    }

    // ---------------------------------------------------------------- 3-7: questions
    static BlockPos pedestal(Session s, int slot) {
        return new BlockPos(s.chamberX(s.chamber) + 14, Y, Z - 6 + slot * 4);
    }

    /** Correct answers each question chamber wants before its membrane opens; a wrong answer costs two of them. */
    public static final int QUESTIONS = 5;

    static void askQuestion(ServerLevel l, Session s) {
        ProofQuestions.Q qq = ProofQuestions.pick(s.chamber, new Random(l.random.nextLong()), s.asked);
        s.correct = l.random.nextInt(4);
        String tg = tag(s, s.chamber) + "_q";
        int cx = s.chamberX(s.chamber);
        for (Entity e : l.getEntities((Entity) null, chamberBox(s, s.chamber), e -> e.getTags().contains(tg))) e.discard();
        text(l, new Vec3(cx + 18.5, Y + 5, Z + 0.5), Component.literal(qq.prompt()).withStyle(ChatFormatting.WHITE), 1.0F, 240, tg, tag(s, s.chamber));
        List<String> wrong = new ArrayList<>(List.of(qq.w1(), qq.w2(), qq.w3()));
        Collections.shuffle(wrong, new Random(l.random.nextLong()));
        int w = 0;
        for (int slot = 0; slot < 4; slot++) {
            BlockPos ped = pedestal(s, slot);
            l.setBlock(ped, b("answer_pedestal"), 2);
            String ans = slot == s.correct ? qq.right() : wrong.get(w++);
            text(l, Vec3.atCenterOf(ped).add(0, 1.4, 0), Component.literal((char) ('A' + slot) + ". " + ans).withStyle(ChatFormatting.AQUA), 0.55F, 110, tg, tag(s, s.chamber));
        }
        text(l, new Vec3(cx + 18.5, Y + 7.6, Z + 0.5), Component.translatable("proof.theoreticalsciences.question_n", s.stage, QUESTIONS).withStyle(ChatFormatting.GRAY), 0.7F, 120, tg, tag(s, s.chamber));
    }

    /** A pedestal was touched. */
    public static void onPedestal(ServerPlayer p, BlockPos pos) {
        ServerLevel l = p.serverLevel();
        ProofArena a = get(p.server);
        Session s = a.ownerOf(p, pos);
        if (s == null) {
            AlphaOmegaEntity boss = AlphaOmegaEntity.near(l, pos);
            if (boss != null) boss.answer(p, pos);
            return;
        }
        if (s.phase != Phase.TRIALS) return;
        if (s.chamber == 2 && pos.equals(submit(s))) {
            checkDiagonal(l, s, p);
            a.setDirty();
            return;
        }
        if (s.chamber < 3) return;
        for (int slot = 0; slot < 4; slot++) {
            if (!pedestal(s, slot).equals(pos)) continue;
            if (slot == s.correct) {
                l.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.CHIME.get(), SoundSource.BLOCKS, 2F, 1.2F);
                s.stage++;
                if (s.stage >= QUESTIONS) solve(l, s, p);
                else {
                    p.displayClientMessage(Component.translatable("proof.theoreticalsciences.right").withStyle(ChatFormatting.GREEN), true);
                    askQuestion(l, s);
                }
            } else {
                punish(l, s, p);
                s.stage = Math.max(0, s.stage - 2);
                askQuestion(l, s);
            }
            a.setDirty();
            return;
        }
    }

    /** Which session's chamber a position belongs to (the challenger's, or the one the player is a companion in). */
    Session ownerOf(ServerPlayer p, BlockPos pos) {
        for (Session s : sessions.values()) {
            if (s.phase != Phase.TRIALS) continue;
            if (chamberBox(s, s.chamber).contains(Vec3.atCenterOf(pos))) return s;
        }
        return null;
    }

    /**
     * Oracle's Lemma (creative only): the chamber the player stands in counts as solved. Returns false if they are not
     * in an unsolved trial chamber.
     */
    public static boolean oracleSolve(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel l) || !p.isCreative()) return false;
        Session s = get(l.getServer()).ownerOf(p, p.blockPosition());
        if (s == null) return false;
        solve(l, s, p);
        return true;
    }

    // ---------------------------------------------------------------- solving and failing
    static final String[] MINIONS = {"planck_mite", "tidal_horror", "ordinal_hound", "ordinal_hound", "axiom_sentinel", "vacuum_drifter", "planck_mite", "tidal_horror"};

    static void punish(ServerLevel l, Session s, ServerPlayer p) {
        p.displayClientMessage(Component.translatable("proof.theoreticalsciences.wrong").withStyle(ChatFormatting.RED), true);
        l.playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.EMERGENCE.get(), SoundSource.HOSTILE, 1F, 1.4F);
        p.hurt(p.damageSources().magic(), 6F);
        var effect = switch (s.chamber) {
            case 0, 6 -> QHEffects.DECOHERENCE;
            case 1, 5 -> QHEffects.SPAGHETTIFICATION;
            case 2, 3 -> QHEffects.INFINITE_REGRESS;
            case 4 -> QHEffects.IMPERFECTION;
            default -> QHEffects.VACUUM_DECAY;
        };
        p.addEffect(new MobEffectInstance(effect, 200, 0));
        // the chamber answers back with one of its own creatures
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(com.theoreticalsciences.TheoreticalSciences.id(MINIONS[s.chamber]));
        Entity m = type.create(l);
        if (m != null) {
            // somewhere open in the chamber, away from the switches, lamps and pedestals - never inside a block
            Vec3 at = null;
            for (int i = 0; i < 40 && at == null; i++) {
                double x = s.chamberX(s.chamber) + 2.5 + l.random.nextInt(17), z = Z - 8.5 + l.random.nextInt(18);
                m.moveTo(x, Y, z, l.random.nextFloat() * 360, 0);
                if (l.noCollision(m) && !l.getBlockState(BlockPos.containing(x, Y - 1, z)).isAir() && p.distanceToSqr(x, Y, z) > 9) at = new Vec3(x, Y, z);
            }
            if (at == null) m.moveTo(s.chamberX(s.chamber) + 5.5, Y, Z + 5.5, 0, 0);
            if (m instanceof net.minecraft.world.entity.Mob mob) {
                mob.setPersistenceRequired();
                mob.setTarget(p);
            }
            m.addTag(tag(s, s.chamber));
            l.addFreshEntity(m);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), m.getX(), m.getY() + 1, m.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        }
    }

    static void solve(ServerLevel l, Session s, ServerPlayer p) {
        int c = s.chamber, cx = s.chamberX(c);
        clearDisplays(l, s, c);
        for (Entity e : l.getEntities((Entity) null, chamberBox(s, c), e -> e instanceof net.minecraft.world.entity.Mob)) e.discard();
        for (int x = cx + 21; x <= cx + 23; x++)
            for (int z = Z - 1; z <= Z + 1; z++)
                for (int y = Y; y <= Y + 2; y++) {
                    l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), x + 0.5, y + 0.5, z + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
                }
        l.playSound(null, new BlockPos(cx + 21, Y, Z), com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 2F, 1.4F);
        for (ServerPlayer pl : players(l, s))
            title(pl, Component.translatable("proof.theoreticalsciences.yields").withStyle(ChatFormatting.AQUA),
                    Component.translatable("proof.theoreticalsciences." + KEYS[c] + ".lesson").withStyle(ChatFormatting.GRAY));
        s.chamber++;
        s.stage = 0;
        s.question = -1;
        get(l.getServer()).setDirty();
        if (s.chamber >= CHAMBERS) {
            s.phase = Phase.MELT;
            s.meltX = s.x0() - 1;
            for (ServerPlayer pl : players(l, s)) {
                title(pl, Component.translatable("proof.theoreticalsciences.qed").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable("proof.theoreticalsciences.qed_sub").withStyle(ChatFormatting.GRAY));
                BlockPos ac = s.arenaCenter();
                pl.teleportTo(ac.getX() - 16 + 0.5, Y, ac.getZ() + 0.5);
            }
        } else setupChamber(l, s);
    }

    static List<ServerPlayer> players(ServerLevel l, Session s) {
        List<ServerPlayer> out = new ArrayList<>();
        int x0 = s.x0() - 8, x1 = s.arenaCenter().getX() + ARENA_R + 8;
        for (ServerPlayer p : l.players()) if (p.getX() >= x0 && p.getX() <= x1 && Math.abs(p.getZ() - Z) < 64) out.add(p);
        return out;
    }

    // ================================================================== ticking
    public static void tick(ServerLevel l) {
        ProofArena a = get(l.getServer());
        for (Session s : a.sessions.values()) {
            if (s.phase == Phase.DONE || s.phase == Phase.FAILED) continue;
            ServerPlayer owner = l.getServer().getPlayerList().getPlayer(s.player);
            if (owner == null) {                       // logged out: everything waits
                if (s.ownerAway < Integer.MAX_VALUE) s.ownerAway++;
                continue;
            }
            s.ownerAway = 0;
            if (owner.level() != l) {
                if (++s.offline > 200) fail(l.getServer(), s, "left");
                continue;
            }
            s.offline = 0;
            switch (s.phase) {
                case TRIALS -> tickTrials(l, s, owner);
                case MELT -> tickMelt(l, s, a);
                case BOSS -> {
                    if (s.boss != null && l.getGameTime() % 100 == 0 && l.isLoaded(s.arenaCenter())
                            && l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(s.arenaCenter())) && l.getEntity(s.boss) == null) {
                        // the boss vanished without dying (chunk lost, killed by command): bring it back
                        spawnBoss(l, s);
                    }
                }
                default -> {}
            }
        }
    }

    static void tickTrials(ServerLevel l, Session s, ServerPlayer p) {
        int c = s.chamber, cx = s.chamberX(c);
        boolean inside = p.getX() >= cx && p.getX() < cx + 21 && Math.abs(p.getZ() - Z) < 11;
        if (l.getGameTime() % 60 == 0) l.playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.PULSE.get(), SoundSource.AMBIENT, 0.6F, 0.7F);
        if (!inside) return;
        if (!s.announced) {
            s.announced = true;
            title(p, Component.translatable("proof.theoreticalsciences." + KEYS[c] + ".title").withStyle(ChatFormatting.GOLD),
                    Component.translatable("proof.theoreticalsciences.chamber_n", c + 1, CHAMBERS).withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.translatable("proof.theoreticalsciences.voice", Component.translatable("proof.theoreticalsciences." + KEYS[c] + ".voice"))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
        if (c == 2 && !s.drawn) {
            // a proof saved before its wall was labelled (or reloaded): write everything on the wall again
            s.drawn = true;
            clearDisplays(l, s, 2);
            String tg = tag(s, 2);
            int dx = s.chamberX(2);
            text(l, new Vec3(dx + 2.5, Y + 6, Z + 0.5), Component.translatable("proof.theoreticalsciences.diagonal.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 2.0F, 200, tg);
            text(l, new Vec3(dx + 2.5, Y + 4.2, Z + 0.5), Component.translatable("proof.theoreticalsciences.diagonal.rule").withStyle(ChatFormatting.WHITE), 0.8F, 260, tg);
            drawDiagonal(l, s, tg);
        }
        switch (c) {
            case 0 -> tickMeasurement(l, s, p);
            case 1 -> tickProperTime(l, s, p);
            default -> {}
        }
    }

    static void tickMelt(ServerLevel l, Session s, ProofArena a) {
        // a wave from the first chamber to the arena: every block of the proof dissolves into light
        int end = s.chamberX(CHAMBERS - 1) + 24;
        for (int k = 0; k < 2 && s.meltX <= end; k++, s.meltX++) {
            for (int y = Y - 1; y <= Y + 12; y++)
                for (int z = Z - 11; z <= Z + 11; z++) {
                    BlockPos p = new BlockPos(s.meltX, y, z);
                    if (!l.getBlockState(p).isAir()) {
                        l.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        if (l.random.nextInt(10) == 0) l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            for (Entity e : l.getEntities((Entity) null, new AABB(s.meltX, Y - 2, Z - 12, s.meltX + 1, Y + 14, Z + 12), e -> !(e instanceof ServerPlayer)))
                e.discard();
        }
        if (s.meltX % 8 == 0) l.playSound(null, new BlockPos(s.meltX, Y + 6, Z), com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.BLOCKS, 3F, 0.5F);
        a.setDirty();
        if (s.meltX > end) {
            // the arena's wall melts last
            BlockPos ac = s.arenaCenter();
            for (int x = -ARENA_R - 1; x <= ARENA_R + 1; x++)
                for (int z = -ARENA_R - 1; z <= ARENA_R + 1; z++) {
                    double r = Math.sqrt(x * x + z * z);
                    if (r > ARENA_R - 0.5 && r <= ARENA_R + 1.0) for (int y = 0; y < 14; y++) l.setBlock(ac.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            s.phase = Phase.BOSS;
            spawnBoss(l, s);
        }
    }

    static void spawnBoss(ServerLevel l, Session s) {
        BlockPos ac = s.arenaCenter();
        AlphaOmegaEntity boss = com.theoreticalsciences.transcend.TranscendRegistry.ALPHA_OMEGA.get().create(l);
        if (boss == null) return;
        boss.moveTo(ac.getX() + 0.5, Y + 14, ac.getZ() + 0.5, 0, 0);
        boss.session = s.player;
        l.addFreshEntity(boss);
        s.boss = boss.getUUID();
        get(l.getServer()).setDirty();
        for (ServerPlayer p : players(l, s))
            title(p, Component.translatable("entity.theoreticalsciences.alpha_omega").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("proof.theoreticalsciences.boss_sub").withStyle(ChatFormatting.GRAY));
    }

    /** Alpha Omega is dead: everyone in the proof is thrown back into Hyperdimensional Reality, carrying the proof. */
    public static void conquer(ServerLevel l, UUID owner) {
        ProofArena a = get(l.getServer());
        Session s = a.sessions.get(owner);
        if (s == null || s.phase == Phase.DONE) return;
        s.phase = Phase.DONE;
        s.returnOmnilex = false;
        a.conquered.add(owner);
        a.setDirty();
        ServerLevel hr = l.getServer().getLevel(QHDimensions.HYPERREAL);
        List<ServerPlayer> ps = players(l, s);
        ServerPlayer op = l.getServer().getPlayerList().getPlayer(owner);
        if (op != null && !ps.contains(op)) ps.add(op);
        if (op == null) s.returnOmnilex = true;          // the winner is offline: their rewards wait for them
        for (ServerPlayer p : ps) {
            give(p, new ItemStack(QHItems.get("proof_of_everything")));
            if (p.getUUID().equals(owner)) give(p, new ItemStack(QHItems.get("omnilex")));
            com.theoreticalsciences.research.ResearchService.grant(p, com.theoreticalsciences.TheoreticalSciences.id("conquered_alpha_omega"));
            if (hr != null) {
                BlockPos land = com.theoreticalsciences.item.BraneShifterItem.findLanding(hr, BlockPos.ZERO.above(100), "bulk_stone", "bulk_crystal");
                p.changeDimension(new DimensionTransition(hr, Vec3.atBottomCenterOf(land), Vec3.ZERO, p.getYRot(), p.getXRot(), DimensionTransition.DO_NOTHING));
            }
            title(p, Component.translatable("proof.theoreticalsciences.won").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("proof.theoreticalsciences.won_sub").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void fail(MinecraftServer server, Session s, String why) {
        if (s.phase == Phase.DONE || s.phase == Phase.FAILED) return;
        s.phase = Phase.FAILED;
        ServerLevel lp = server.getLevel(QHDimensions.LIVING_PROOF);
        if (lp != null && s.boss != null) {
            Entity e = lp.getEntity(s.boss);
            if (e != null) e.discard();
        }
        // nobody may be left stranded in a proof that no longer has a way out
        if (lp != null)
            for (ServerPlayer q : players(lp, s))
                if (q.isAlive()) q.changeDimension(q.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING));
        get(server).setDirty();
        ServerPlayer p = server.getPlayerList().getPlayer(s.player);
        if (p != null) p.sendSystemMessage(Component.translatable("proof.theoreticalsciences.failed." + why).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    /** How long (ticks) a challenger may be logged out before the companions they left in the proof are sent home. */
    static final int OWNER_AWAY_LIMIT = 600;

    /**
     * Round 29: nobody may be stuck in the Living Proof. Called when a player logs in and every 5 seconds: a player in
     * the Living Proof who is not inside a running proof - or whose challenger has been logged out for over 30
     * seconds - is sent to their respawn point (bed / anchor / world spawn, the same safe spot dying would give).
     * The challenger themselves is never sent away from their own proof; creative and spectator players are left alone.
     */
    public static void rescueStranded(ServerPlayer p) {
        if (p.level().dimension() != QHDimensions.LIVING_PROOF || !p.isAlive() || p.isCreative() || p.isSpectator()) return;
        ProofArena a = get(p.server);
        Session in = null;
        for (Session s : a.sessions.values()) {
            int x0 = s.x0() - 8, x1 = s.arenaCenter().getX() + ARENA_R + 8;
            if (p.getX() >= x0 && p.getX() <= x1 && Math.abs(p.getZ() - Z) < 64) {
                in = s;
                break;
            }
        }
        boolean stranded = in == null || !a.active(in.player)
                || !in.player.equals(p.getUUID()) && p.server.getPlayerList().getPlayer(in.player) == null && in.ownerAway > OWNER_AWAY_LIMIT;
        if (!stranded) return;
        p.changeDimension(p.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING));
        p.sendSystemMessage(Component.translatable("proof.theoreticalsciences.stranded").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** Hands back the Omnilex once a failed challenger is out of the proof. */
    public static void returnLoans(ServerPlayer p) {
        ProofArena a = get(p.server);
        Session s = a.sessions.get(p.getUUID());
        if (s == null || !s.returnOmnilex || p.level().dimension() == QHDimensions.LIVING_PROOF || !p.isAlive()) return;
        if (s.phase == Phase.DONE) {
            s.returnOmnilex = false;
            a.setDirty();
            give(p, new ItemStack(QHItems.get("proof_of_everything")));
            give(p, new ItemStack(QHItems.get("omnilex")));
            com.theoreticalsciences.research.ResearchService.grant(p, com.theoreticalsciences.TheoreticalSciences.id("conquered_alpha_omega"));
            return;
        }
        if (s.phase != Phase.FAILED) return;
        s.returnOmnilex = false;
        a.setDirty();
        give(p, new ItemStack(QHItems.get("omnilex")));
        p.sendSystemMessage(Component.translatable("proof.theoreticalsciences.returned").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    // ================================================================== helpers
    static void title(ServerPlayer p, Component title, Component sub) {
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Spawns a text display (vanilla entity) - used for every rule, question and answer written into the proof. */
    public static Entity text(ServerLevel l, Vec3 at, Component c, float scale, int width, String... tags) {
        return textB(l, at, c, scale, width, "center", tags);
    }

    public static Entity textB(ServerLevel l, Vec3 at, Component c, float scale, int width, String billboard, String... tags) {
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:text_display");
        t.putString("text", Component.Serializer.toJson(c, l.registryAccess()));
        t.putString("billboard", billboard);
        t.putInt("line_width", width);
        t.putInt("background", 0xB0100818);
        CompoundTag br = new CompoundTag();
        br.putInt("block", 15);
        br.putInt("sky", 15);
        t.put("brightness", br);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, 0, 0));
        tr.put("scale", floats(scale, scale, scale));
        t.put("transformation", tr);
        ListTag tl = new ListTag();
        for (String s : tags) tl.add(StringTag.valueOf(s));
        t.put("Tags", tl);
        Entity e = EntityType.loadEntityRecursive(t, l, en -> {
            en.moveTo(at.x, at.y, at.z, 0, 0);
            return en;
        });
        if (e != null) l.addFreshEntity(e);
        return e;
    }

    static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) l.add(FloatTag.valueOf(f));
        return l;
    }
}
