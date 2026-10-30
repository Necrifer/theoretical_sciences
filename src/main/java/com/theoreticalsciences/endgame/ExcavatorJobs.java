package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The Dodecaract Excavator's big jobs: a cube of up to 33x33x33 blocks is worked through a slice at a time, so a
 * whole mountain does not stall the server. Each player has at most one job; it stops if they log out, change
 * dimension, put the Excavator away or run out of charge.
 * <p>
 * Every block is checked like a block broken by hand: spawn protection, adventure mode, claim mods (they cancel the
 * {@link BlockEvent.BreakEvent} we post). Unbreakable blocks, blocks with contents (block entities) and fluids are
 * never touched.
 */
public final class ExcavatorJobs {
    /** Blocks looked at, and blocks actually taken, per job per tick. */
    static final int SCAN_PER_TICK = 6000, TAKE_PER_TICK = 96;
    /** Extra ores for Ore Extraction, and blocks it must never take (block tags, empty by default). */
    public static final TagKey<Block> EXTRA_ORES = BlockTags.create(TheoreticalSciences.id("excavator_ores"));
    public static final TagKey<Block> NEVER = BlockTags.create(TheoreticalSciences.id("excavator_ignored"));
    private static final Pattern ORE_NAME = Pattern.compile("(^|_)ore($|_)");

    private static final Map<UUID, Job> JOBS = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> ORE_CACHE = new ConcurrentHashMap<>();
    /** True while a job breaks blocks (so its own break events do not set off more Area mining). */
    private static boolean working;

    static final class Job {
        final ResourceKey<Level> dim;
        final BlockPos min;
        final int side;
        final boolean extract;
        final BlockPos skip;               // Area mode: the block the player broke themselves
        int cursor, taken, xp;
        Job(ResourceKey<Level> dim, BlockPos centre, int r, boolean extract, BlockPos skip) {
            this.dim = dim;
            this.min = centre.offset(-r, -r, -r);
            this.side = 2 * r + 1;
            this.extract = extract;
            this.skip = skip;
        }
        int total() {
            return side * side * side;
        }
    }

    static boolean busy(ServerPlayer sp) {
        return JOBS.containsKey(sp.getUUID());
    }

    static boolean working() {
        return working;
    }

    static void start(ServerPlayer sp, BlockPos centre, int r, boolean extract, BlockPos skip) {
        JOBS.put(sp.getUUID(), new Job(sp.level().dimension(), centre.immutable(), r, extract, skip));
    }

    /** Tags were reloaded: what counts as an ore may have changed. */
    public static void clearCache() {
        ORE_CACHE.clear();
    }

    public static void onServerStopped() {
        JOBS.clear();
        ORE_CACHE.clear();
    }

    /** An ore: tagged c:ores, in any minecraft:*_ores tag, in theoreticalsciences:excavator_ores, or named like one (..._ore, ore_...). */
    public static boolean isOre(BlockState st) {
        return ORE_CACHE.computeIfAbsent(st.getBlock(), b -> {
            BlockState d = b.defaultBlockState();
            if (d.is(NEVER)) return false;
            if (d.is(Tags.Blocks.ORES) || d.is(EXTRA_ORES)) return true;
            if (d.getTags().anyMatch(t -> t.location().getNamespace().equals("minecraft") && t.location().getPath().endsWith("_ores"))) return true;
            return ORE_NAME.matcher(BuiltInRegistries.BLOCK.getKey(b).getPath()).find();
        });
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (JOBS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Job>> it = JOBS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ServerPlayer sp = e.getServer().getPlayerList().getPlayer(en.getKey());
            Job job = en.getValue();
            ItemStack tool = sp == null ? ItemStack.EMPTY : sp.getMainHandItem();
            if (sp == null || !sp.isAlive() || sp.level().dimension() != job.dim || !(tool.getItem() instanceof DodecaractExcavatorItem)) {
                it.remove();
                continue;
            }
            boolean done;
            working = true;
            try {
                done = step(sp, sp.serverLevel(), tool, job);
            } finally {
                working = false;
            }
            if (done) {
                it.remove();
                finish(sp, job, tool);
            }
        }
    }

    /** One slice of a job. True when it is over (the whole cube done, or out of charge). */
    private static boolean step(ServerPlayer sp, ServerLevel level, ItemStack tool, Job job) {
        int took = 0, total = job.total(), s = job.side;
        int end = Math.min(total, job.cursor + SCAN_PER_TICK);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        while (job.cursor < end && took < TAKE_PER_TICK) {
            int i = job.cursor++;
            m.set(job.min.getX() + i % s, job.min.getY() + (i / s) % s, job.min.getZ() + i / (s * s));
            if (job.skip != null && job.skip.equals(m)) continue;
            if (level.isOutsideBuildHeight(m) || !level.isLoaded(m)) continue;
            BlockState st = level.getBlockState(m);
            if (st.isAir() || st.getBlock() instanceof LiquidBlock || st.hasBlockEntity() || st.is(NEVER)) continue;
            float hard = st.getDestroySpeed(level, m);
            if (hard < 0) continue;                                   // bedrock, barriers, the mod's unbreakable blocks
            if (job.extract && !isOre(st)) continue;
            // an ore the tool could not harvest would just be destroyed: leave it
            if (job.extract && !sp.getAbilities().instabuild && !st.canHarvestBlock(level, m, sp)) continue;
            int cost = job.extract ? EschatonCharge.EXCAVATOR_ORE : EschatonCharge.EXCAVATOR_BLOCK;
            if (!EschatonCharge.active(sp, tool) || (!EschatonCharge.free(sp) && EschatonCharge.get(tool) < cost)) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.excavator.out").withStyle(ChatFormatting.RED), false);
                return true;
            }
            BlockPos pos = m.immutable();
            if (!mayTake(sp, level, pos, st)) continue;
            take(sp, level, tool, pos, st, job);
            EschatonCharge.spend(sp, tool, cost);
            took++;
            job.taken++;
        }
        return job.cursor >= total;
    }

    /** Would the player be allowed to break this block by hand? */
    private static boolean mayTake(ServerPlayer sp, ServerLevel level, BlockPos pos, BlockState st) {
        if (!level.mayInteract(sp, pos) || sp.blockActionRestricted(level, pos, sp.gameMode.getGameModeForPlayer())) return false;
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, st, sp)).isCanceled();
    }

    private static void take(ServerPlayer sp, ServerLevel level, ItemStack tool, BlockPos pos, BlockState st, Job job) {
        // the drops, rolled with the tool itself (Fortune, Silk Touch), and only if it may harvest the block
        if (!sp.getAbilities().instabuild && st.canHarvestBlock(level, pos, sp)) {
            for (ItemStack d : Block.getDrops(st, level, pos, null, sp, tool)) give(sp, d);
            job.xp += EnchantmentHelper.processBlockExperience(level, tool, st.getExpDrop(level, pos, null, sp, tool));
        }
        BlockState after = job.extract ? hostFor(level, pos, st) : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, after, Block.UPDATE_ALL);
        sp.awardStat(Stats.BLOCK_MINED.get(st.getBlock()));
        if (job.extract) {
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            if (job.taken % 8 == 0) level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.02);
        } else if (job.taken % 4 == 0) {
            level.levelEvent(2001, pos, Block.getId(st));             // the usual breaking puff, for some of them
        }
    }

    /** Into the inventory; what does not fit lands at the player's feet. */
    static void give(ServerPlayer sp, ItemStack d) {
        if (d.isEmpty()) return;
        ItemStack c = d.copy();
        if (!sp.getInventory().add(c) || !c.isEmpty()) sp.drop(c, false, false);
    }

    /**
     * The stone an ore grew in: deepslate / nether / end ores by name; otherwise whatever natural rock most surrounds
     * it (so the mod's own dimension stones stay themselves); failing that, by dimension and depth.
     */
    static BlockState hostFor(ServerLevel level, BlockPos pos, BlockState ore) {
        String path = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
        if (path.startsWith("deepslate_") || path.contains("_deepslate_")) return Blocks.DEEPSLATE.defaultBlockState();
        if (path.startsWith("nether_") || path.contains("netherrack")) return Blocks.NETHERRACK.defaultBlockState();
        if (path.startsWith("end_") || path.contains("end_stone")) return Blocks.END_STONE.defaultBlockState();
        Map<Block, Integer> count = new HashMap<>();
        Block best = null;
        int bestN = 0;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n)) continue;
            BlockState ns = level.getBlockState(n);
            if (ns.isAir() || ns.hasBlockEntity() || !ns.getFluidState().isEmpty() || isOre(ns) || !ns.isCollisionShapeFullBlock(level, n)) continue;
            if (ns.getDestroySpeed(level, n) < 0 || ns.is(NEVER)) continue;
            int k = count.merge(ns.getBlock(), 1, Integer::sum);
            if (k > bestN) {
                bestN = k;
                best = ns.getBlock();
            }
        }
        if (best != null) return best.defaultBlockState();
        if (level.dimension() == Level.NETHER) return Blocks.NETHERRACK.defaultBlockState();
        if (level.dimension() == Level.END) return Blocks.END_STONE.defaultBlockState();
        return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    private static void finish(ServerPlayer sp, Job job, ItemStack tool) {
        if (job.xp > 0) sp.giveExperiencePoints(job.xp);
        if (job.extract) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.excavator.extracted", job.taken, job.total()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 0.8F, job.taken > 0 ? 1.2F : 0.6F);
            if (job.taken > 0) sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.PLAYERS, 1F, 0.8F);
        } else if (job.taken > 0) {
            sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.PLAYERS, 1F, 0.6F);
        }
    }

    private ExcavatorJobs() {}
}
