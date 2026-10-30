package com.theoreticalsciences.transcend;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Random;

/**
 * Round 27: an Omnipotence Core forged anew, outside the Transcendental. It arrives dormant and only wakes once it is
 * attuned to the laws of the place it stands in (every place - world, dimension and spot - has its own, hidden).
 * Moved (with the Hand of the Demiurge) it has to be attuned again.
 * <p>
 * Round 34c: attuning is no longer one dial but the hardest fine-tuning in the mod. Every branch studied on the way
 * has three parameters (21 in all), and each branch behaves differently:
 * <ul>
 *   <li><b>Physics</b> - three plain fields.</li>
 *   <li><b>Chemistry</b> - three reagent fractions that always add up to a whole: raising one lowers the others.</li>
 *   <li><b>Biology</b> - what is measured is what the three settle into (equilibrium, diversity, resilience), not the
 *       parameters themselves.</li>
 *   <li><b>Mathematics</b> - whole numbers; exact or nothing. Four laws (congruences and a prime), each a lamp.</li>
 *   <li><b>Origin</b> - three phases that wrap around; the first is absolute, the others count from the one before.</li>
 *   <li><b>Finality</b> - entropy creeps up while the Core is being handled: aim a little low.</li>
 *   <li><b>Abnormality</b> - while uncontained, the anomaly spreads: it nudges other branches' parameters and clouds
 *       every other reading (and the mathematics lamps). Contain it first.</li>
 * </ul>
 * And the branches lean on each other in a ring (Physics, Chemistry, Biology, Origin, Finality, Abnormality and back):
 * where one wants to be depends a little on the branch before it, so settling one moves the next one's target. The
 * screen only ever shows how well each branch as a whole is in tune, never which parameter is off.
 * <p>
 * Everything about the targets lives here, on the server; the client is only sent the (possibly clouded) harmonies.
 */
public class ForgedCoreBlockEntity extends BlockEntity {
    public static final int BRANCHES = 7, PER = 3, N = BRANCHES * PER;
    public static final int PHYSICS = 0, CHEMISTRY = 1, BIOLOGY = 2, MATHEMATICS = 3, ORIGIN = 4, FINALITY = 5, ABNORMALITY = 6;
    /** How in tune every branch must be to wake the Core (mathematics: all four laws). */
    public static final float IN_TUNE = 0.97F;
    /** The ring the branches lean on each other in (mathematics stands apart: exact laws do not lean). */
    private static final int[] RING = {PHYSICS, CHEMISTRY, BIOLOGY, ORIGIN, FINALITY, ABNORMALITY};
    /** How strongly a branch's target leans on the branch before it. */
    private static final double LEAN = 0.12;
    /** How sharply harmony falls with distance (in thousandths of a parameter's range). */
    private static final double SIGMA = 80;
    private static final int[] PRIMES = java.util.stream.IntStream.rangeClosed(2, 199).filter(ForgedCoreBlockEntity::prime).toArray();

    final int[] v = defaults();
    boolean active;
    /** Game time the Core was last handled (the screen polls while open); entropy and the anomaly only move then. */
    private long handledUntil;
    /** Client only: how far the waking animation is (0 dormant .. 1 awake). */
    public float wakeShown = -1, lastFrame;

    public ForgedCoreBlockEntity(BlockPos pos, BlockState state) {
        super(TranscendRegistry.FORGED_CORE_BE.get(), pos, state);
    }

    // ================================================================== parameters
    /** The highest value parameter i can take (all start at 0). */
    public static int max(int i) {
        int b = i / PER;
        return b == MATHEMATICS ? 199 : b == ORIGIN ? 3599 : 1000;
    }

    private static int[] defaults() {
        int[] d = new int[N];
        Arrays.fill(d, 500);
        d[CHEMISTRY * PER] = 334;
        d[CHEMISTRY * PER + 1] = 333;
        d[CHEMISTRY * PER + 2] = 333;
        for (int j = 0; j < PER; j++) {
            d[MATHEMATICS * PER + j] = 0;
            d[ORIGIN * PER + j] = 1800;
        }
        return d;
    }

    public int[] values() {
        return v.clone();
    }

    /** A player set parameter i to x (the screen). Chemistry keeps its whole; the rest just clamp. */
    public void set(int i, int x) {
        if (i < 0 || i >= N) return;
        x = i / PER == ORIGIN ? Math.floorMod(x, 3600) : Mth.clamp(x, 0, max(i));      // phases wrap round
        if (i / PER == CHEMISTRY) {
            int base = CHEMISTRY * PER, a = (i - base + 1) % PER + base, b = (i - base + 2) % PER + base;
            int rest = 1000 - x, was = v[a] + v[b];
            int na = was <= 0 ? rest / 2 : (int) Math.round(rest * (v[a] / (double) was));
            v[i] = x;
            v[a] = na;
            v[b] = rest - na;
        } else v[i] = x;
        knocked();
        changed();
    }

    // ================================================================== the place's hidden laws
    private record Laws(int[] phys, int[] chem, int[] bio, int[] math, int[] origin, int[] fin, int[] abn, double[] lean) {}

    private Laws laws;

    private Laws laws() {
        if (laws != null) return laws;
        long h = 0;
        if (level instanceof ServerLevel sl)
            h = sl.getSeed() * 0x9E3779B97F4A7C15L ^ sl.dimension().location().toString().hashCode() * 31L ^ worldPosition.asLong() * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        Random r = new Random(h);
        int[] phys = {80 + r.nextInt(841), 80 + r.nextInt(841), 80 + r.nextInt(841)};
        int c0 = 120 + r.nextInt(500), c1 = 120 + r.nextInt(Math.max(1, 880 - c0 - 120));
        int[] chem = {c0, c1, 1000 - c0 - c1};
        int[] bio = {250 + r.nextInt(501), 250 + r.nextInt(501), 250 + r.nextInt(501)};           // a hidden living state
        int[] math = {r.nextInt(7), 1 + r.nextInt(10), r.nextInt(13)};
        int[] origin = {r.nextInt(3600), r.nextInt(3600), r.nextInt(3600)};
        int[] fin = {150 + r.nextInt(701), 80 + r.nextInt(841), 80 + r.nextInt(841)};
        int[] abn = {80 + r.nextInt(841), 80 + r.nextInt(841), 80 + r.nextInt(841)};
        double[] lean = new double[BRANCHES * PER];
        for (int k = 0; k < lean.length; k++) lean[k] = (r.nextDouble() * 2 - 1) * LEAN;
        return laws = new Laws(phys, chem, bio, math, origin, fin, abn, lean);
    }

    /** Parameter j of a branch on a 0..1000 scale (for leaning on it). */
    private double norm(int branch, int j) {
        int x = v[branch * PER + j];
        return branch == ORIGIN ? x / 3.6 : x;
    }

    /** How far the branch before this one in the ring pushes this one's target for parameter j. */
    private double lean(int branch, int j) {
        int at = -1;
        for (int k = 0; k < RING.length; k++) if (RING[k] == branch) at = k;
        if (at < 0) return 0;
        int prev = RING[(at + RING.length - 1) % RING.length];
        return laws().lean[branch * PER + j] * (norm(prev, j) - 500);
    }

    private static double closeness(double dist) {
        return 1 / (1 + Math.pow(dist / SIGMA, 1.5));
    }

    private double plain(int branch, int[] target) {
        double s = 0;
        for (int j = 0; j < PER; j++) {
            double t = Mth.clamp(target[j] + lean(branch, j), 30, 970);
            double d = v[branch * PER + j] - t;
            s += d * d;
        }
        return closeness(Math.sqrt(s));
    }

    /** Which of mathematics' four laws hold (bits 0-3). */
    public int lamps() {
        int[] m = laws().math;
        int a = v[MATHEMATICS * PER], b = v[MATHEMATICS * PER + 1], n = v[MATHEMATICS * PER + 2];
        int bits = 0;
        if (a % 7 == m[0]) bits |= 1;
        if ((a * b) % 11 == m[1]) bits |= 2;
        if ((a + b + n) % 13 == m[2]) bits |= 4;
        if (prime(n)) bits |= 8;
        return bits;
    }

    /** How in tune one branch is (0..1), truthfully. */
    public float harmony(int branch) {
        Laws L = laws();
        return (float) switch (branch) {
            case PHYSICS -> plain(PHYSICS, L.phys);
            case FINALITY -> plain(FINALITY, L.fin);
            case ABNORMALITY -> plain(ABNORMALITY, L.abn);
            case CHEMISTRY -> {
                double[] t = new double[PER];
                double sum = 0;
                for (int j = 0; j < PER; j++) sum += t[j] = Math.max(20, L.chem[j] + lean(CHEMISTRY, j));
                double s = 0;
                for (int j = 0; j < PER; j++) {
                    double d = v[CHEMISTRY * PER + j] - t[j] * 1000 / sum;
                    s += d * d;
                }
                yield closeness(Math.sqrt(s));
            }
            case BIOLOGY -> {
                // the living state the place wants, leaned on by Chemistry, and what it settles into
                double[] w = new double[PER];
                for (int j = 0; j < PER; j++) w[j] = Mth.clamp(L.bio[j] + lean(BIOLOGY, j), 50, 950);
                double g = v[BIOLOGY * PER], m = v[BIOLOGY * PER + 1], s = v[BIOLOGY * PER + 2];
                // each measured quantity is mostly one parameter, pulled by the next one along
                double dE = (2 * g + m) / 3 - (2 * w[0] + w[1]) / 3;                 // equilibrium
                double dD = (2 * m + s) / 3 - (2 * w[1] + w[2]) / 3;                 // diversity
                double dR = (2 * s + g) / 3 - (2 * w[2] + w[0]) / 3;                 // resilience
                yield closeness(Math.sqrt(dE * dE + dD * dD + dR * dR) * 1.6);
            }
            case MATHEMATICS -> Integer.bitCount(lamps()) / 4.0;
            case ORIGIN -> {
                double s = 0, prevAngle = 0;
                for (int j = 0; j < PER; j++) {
                    double want = j == 0 ? L.origin[0] + lean(ORIGIN, 0) * 3.6 : L.origin[j] + prevAngle;
                    double d = Math.abs(((v[ORIGIN * PER + j] - want) % 3600 + 5400) % 3600 - 1800);   // wrapped, tenths of a degree
                    d = d / 3.6;                                                                         // to the 0..1000 scale
                    s += d * d;
                    prevAngle = v[ORIGIN * PER + j];
                }
                yield closeness(Math.sqrt(s));
            }
            default -> 0;
        };
    }

    public boolean inTune() {
        for (int b = 0; b < BRANCHES; b++) if (b == MATHEMATICS ? lamps() != 15 : harmony(b) < IN_TUNE) return false;
        return true;
    }

    /** The weakest branch's harmony (0..1). */
    public float match() {
        float m = 1;
        for (int b = 0; b < BRANCHES; b++) m = Math.min(m, harmony(b));
        return m;
    }

    // ================================================================== what the screen is told
    /** The state for the screen: values, harmonies as they read (clouded by an uncontained anomaly) and the lamps. */
    public CompoundTag screenState(boolean open) {
        CompoundTag t = new CompoundTag();
        t.putIntArray("v", v.clone());
        float abn = harmony(ABNORMALITY);
        float cloud = abn >= IN_TUNE ? 0 : (1 - abn) * 0.3F;
        Random r = level == null ? new Random() : new Random(level.getGameTime() / 10 * 31 + worldPosition.asLong());
        int[] shown = new int[BRANCHES];
        for (int b = 0; b < BRANCHES; b++) {
            float h = harmony(b);
            if (b != ABNORMALITY && b != MATHEMATICS && cloud > 0) h = Mth.clamp(h + (r.nextFloat() * 2 - 1) * cloud, 0, 1);
            shown[b] = Math.round(h * 1000);
        }
        int lamps = lamps();
        if (cloud > 0) for (int k = 0; k < 4; k++) if (r.nextFloat() < cloud * 1.6F) lamps ^= 1 << k;
        shown[MATHEMATICS] = Integer.bitCount(lamps) * 250;
        t.putIntArray("h", shown);
        t.putInt("lamps", lamps);
        t.putBoolean("clouded", cloud > 0);
        t.putBoolean("active", active);
        t.putBoolean("open", open);
        return t;
    }

    /** The screen is open on this Core (it polls): entropy and the anomaly move for the next two seconds. */
    public void handled() {
        if (level != null) handledUntil = level.getGameTime() + 40;
    }

    /** Server tick: while being handled and dormant, entropy creeps up and an uncontained anomaly spreads. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgedCoreBlockEntity be) {
        long now = level.getGameTime();
        // a Core from before round 34c (or loaded mid-change): make its look match whether it is awake
        if (state.hasProperty(ForgedCoreBlock.ACTIVE) && state.getValue(ForgedCoreBlock.ACTIVE) != be.active)
            level.setBlock(pos, state.setValue(ForgedCoreBlock.ACTIVE, be.active), 3);
        if (be.active || now > be.handledUntil) return;
        boolean moved = false;
        if (now % 10 == 0 && be.v[FINALITY * PER] < 1000) {
            be.v[FINALITY * PER]++;
            moved = true;
        }
        if (now % 20 == 0) {
            float abn = be.harmony(ABNORMALITY);
            if (abn < 0.9F) {
                Random r = new Random(now * 7 + pos.asLong());
                be.v[ABNORMALITY * PER] = Math.min(1000, be.v[ABNORMALITY * PER] + 2);       // the infection grows
                int[] spread = {PHYSICS, CHEMISTRY, BIOLOGY, ORIGIN, FINALITY};
                int b = spread[r.nextInt(spread.length)], i = b * PER + r.nextInt(PER);
                int push = Math.max(1, Math.round((1 - abn) * 6)) * (r.nextBoolean() ? 1 : -1) * (b == ORIGIN ? 4 : 1);
                if (b == CHEMISTRY) be.set(i, be.v[i] + push);
                else be.v[i] = b == ORIGIN ? Math.floorMod(be.v[i] + push, 3600) : Mth.clamp(be.v[i] + push, 0, max(i));
                moved = true;
            }
        }
        if (moved) be.setChanged();
    }

    private static boolean prime(int n) {
        if (n < 2) return false;
        for (int d = 2; d * d <= n; d++) if (n % d == 0) return false;
        return true;
    }

    // ================================================================== state
    public boolean active() {
        return active;
    }

    /** Knocked out of tune while awake: it goes dormant. */
    private void knocked() {
        if (active && !inTune()) active = false;
    }

    public void setActive(boolean on) {
        active = on;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            BlockState s = getBlockState();
            if (s.hasProperty(ForgedCoreBlock.ACTIVE) && s.getValue(ForgedCoreBlock.ACTIVE) != active && !level.isClientSide)
                level.setBlock(worldPosition, s.setValue(ForgedCoreBlock.ACTIVE, active), 3);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putIntArray("Attunement", v.clone());
        tag.putBoolean("Active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        int[] a = tag.getIntArray("Attunement");
        if (a.length == N) System.arraycopy(a, 0, v, 0, N);
        active = tag.getBoolean("Active");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Active", active);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
