package com.theoreticalsciences.dungeon;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.QHEnergyStorage;
import com.theoreticalsciences.machine.tuning.Materialisation;
import com.theoreticalsciences.machine.tuning.TuningProfile;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.registry.QHLogistics;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two lives.
 * <p>
 * <b>Found in ruins</b> (no owner): while a player is near, every 10-25 seconds it materialises one of its dimension's
 * creatures, as long as fewer than four are around it.
 * <p>
 * <b>Built</b> (round 21): the mod's mob spawner. A genome sample (from the Gene Sampler) in its template slot says
 * which creature; Morphogenic Serum is used up for each one; energy too. It works only while its four conditions are
 * tuned to that creature (see {@link SpawnTuning}), and only someone who has studied Synthetic Biology can open it.
 */
public class AnomalyEngineBlockEntity extends BlockEntity implements MenuProvider {
    private static final Map<ResourceKey<Level>, List<String>> MOBS = Map.of(
            QHDimensions.BULK, List.of("brane_strider", "moduli_crawler", "graviton_wisp"),
            QHDimensions.PLATONIC, List.of("fractal_wyrm", "axiom_sentinel"),
            QHDimensions.FOAM, List.of("planck_mite", "vacuum_drifter"),
            QHDimensions.TRANSFINITE, List.of("ordinal_hound"),
            QHDimensions.BLACK_HOLE, List.of("tidal_horror", "void_wraith"),
            QHDimensions.WHITE_HOLE, List.of("void_wraith", "paradox_echo"),
            QHDimensions.HYPERREAL, List.of("law_wraith"),
            QHDimensions.TRANSCENDENTAL, List.of("law_wraith", "paradox_echo"),
            QHDimensions.ANTIVERSE, List.of("annihilator", "positronium_skitterer"));
    private static final List<String> ELSEWHERE = List.of("void_wraith", "paradox_echo");
    public static final TagKey<EntityType<?>> FORBIDDEN = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, TheoreticalSciences.id("not_spawnable"));
    public static final ResourceLocation RESEARCH = TheoreticalSciences.id("synthetic_biology");
    public static final long ENERGY_PER_SPAWN = 16_000_000;
    public static final int INTERVAL = 100, MAX_AROUND = 8;

    private int cooldown = 100;
    @Nullable UUID owner;
    boolean built;
    final int[] knobs = new int[SpawnTuning.N];
    int status;               // 0 idle, 1 working, 2 no template, 3 not tuned, 4 no serum, 5 no power, 6 crowded, 7 forbidden
    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack s) {
            return slot == 0 ? species(s) != null : s.is(QHItems.get("morphogenic_serum"));
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public final QHEnergyStorage energy = new QHEnergyStorage(1_000_000_000L, Integer.MAX_VALUE, 0, this::setChanged);

    public AnomalyEngineBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.ANOMALY_ENGINE.get(), pos, state);
    }

    /** The creature a genome sample was taken from (Gene Sampler, round 21), or null. */
    @Nullable
    public static String species(ItemStack s) {
        if (s.isEmpty() || !(s.is(QHItems.get("genome_sample")) || s.is(QHItems.get("xenogenome_sample")))) return null;
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return t.contains("Species") ? t.getString("Species") : null;
    }

    public boolean isBuilt() {
        return built;
    }

    void setBuilt(@Nullable UUID by) {
        built = true;
        owner = by;
        setChanged();
        if (level != null && !level.isClientSide) {
            // cables asked before it was "built" and were told "nothing here": make them ask again
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    static TuningProfile.Hazard look(ResourceKey<Level> dim) {
        if (dim == QHDimensions.PLATONIC || dim == QHDimensions.HYPERREAL || dim == QHDimensions.TRANSCENDENTAL) return TuningProfile.Hazard.DIMENSIONAL;
        if (dim == QHDimensions.TRANSFINITE || dim == QHDimensions.WHITE_HOLE) return TuningProfile.Hazard.TEMPORAL;
        return TuningProfile.Hazard.SPATIAL;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AnomalyEngineBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.built) {
            be.tickBuilt(sl, pos);
            return;
        }
        if (--be.cooldown > 0) return;
        be.cooldown = 200 + sl.random.nextInt(300);
        if (sl.getDifficulty() == Difficulty.PEACEFUL) return;
        if (sl.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, p -> !p.isSpectator()) == null) return;
        AABB near = new AABB(pos).inflate(10);
        int around = sl.getEntitiesOfClass(Mob.class, near, m -> m.isAlive()
                && BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getNamespace().equals(TheoreticalSciences.MODID)).size();
        if (around >= 4) return;
        List<String> pool = MOBS.getOrDefault(sl.dimension(), ELSEWHERE);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(TheoreticalSciences.id(pool.get(sl.random.nextInt(pool.size()))));
        Vec3 at = spot(sl, pos);
        if (at == null) return;
        @SuppressWarnings("unchecked") EntityType<? extends Mob> mob = (EntityType<? extends Mob>) type;
        Materialisation.spawn(sl, at, look(sl.dimension()), mob, 0);
        sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 1F, 0.6F);
        be.setChanged();
    }

    private void tickBuilt(ServerLevel sl, BlockPos pos) {
        if (--cooldown > 0) return;
        cooldown = INTERVAL;
        String sp = species(items.getStackInSlot(0));
        if (sp == null) {
            setStatus(2);
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(sp)).orElse(null);
        if (type == null || type.is(FORBIDDEN) || type.is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES) || !type.canSummon() || !isMob(sl, type)) {
            setStatus(7);
            return;
        }
        if (!SpawnTuning.tuned(SpawnTuning.problem(sp), knobs)) {
            setStatus(3);
            return;
        }
        if (items.getStackInSlot(1).isEmpty()) {
            setStatus(4);
            return;
        }
        if (energy.stored() < ENERGY_PER_SPAWN) {
            setStatus(5);
            return;
        }
        AABB near = new AABB(pos).inflate(10);
        if (sl.getEntities(type, near, e -> e.isAlive()).size() >= MAX_AROUND
                || sl.getEntitiesOfClass(Mob.class, near.inflate(6), Mob::isAlive).size() >= MAX_AROUND * 3) {
            setStatus(6);
            return;
        }
        Vec3 at = spot(sl, pos);
        if (at == null) {
            setStatus(6);
            return;
        }
        boolean ok;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(TheoreticalSciences.MODID)) {
            @SuppressWarnings("unchecked") EntityType<? extends Mob> mob = (EntityType<? extends Mob>) type;
            Materialisation.spawn(sl, at, look(sl.dimension()), mob, 0);
            ok = true;
        } else {
            var e = type.spawn(sl, BlockPos.containing(at), MobSpawnType.SPAWNER);
            ok = e != null && e.isAddedToLevel();
            // like a vanilla spawner: not persistent, so strays that wander off despawn normally
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), at.x, at.y + 0.8, at.z, 40, 0.4, 0.6, 0.4, 0.1);
        }
        if (ok) {
            items.getStackInSlot(1).shrink(1);
            energy.consume(ENERGY_PER_SPAWN);
            sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 1F, 0.6F);
        }
        setStatus(ok ? 1 : 6);
    }

    private static final Map<EntityType<?>, Boolean> MOBS_ONLY = new java.util.concurrent.ConcurrentHashMap<>();

    /** Only living creatures (no boats, arrows, ...). Worked out once per type, not every few seconds. */
    private static boolean isMob(ServerLevel sl, EntityType<?> type) {
        return MOBS_ONLY.computeIfAbsent(type, t -> {
            var e = t.create(sl);
            boolean mob = e instanceof Mob;
            if (e != null) e.discard();
            return mob;
        });
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (built && level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = super.getUpdateTag(reg);
        t.putBoolean("Built", built);
        return t;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider reg) {
        built = tag.getBoolean("Built");
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider reg) {
        built = pkt.getTag().getBoolean("Built");
    }

    private void setStatus(int s) {
        if (status != s) {
            status = s;
            setChanged();
        }
    }

    /** A free standing spot a few blocks from the engine. */
    private static Vec3 spot(ServerLevel l, BlockPos pos) {
        for (int tries = 0; tries < 12; tries++) {
            BlockPos p = pos.offset(l.random.nextInt(9) - 4, l.random.nextInt(5) - 2, l.random.nextInt(9) - 4);
            if (!l.getBlockState(p.below()).getCollisionShape(l, p.below()).isEmpty()
                    && l.getBlockState(p).getCollisionShape(l, p).isEmpty() && l.getBlockState(p.above()).getCollisionShape(l, p.above()).isEmpty()
                    && l.getFluidState(p).isEmpty())
                return Vec3.atBottomCenterOf(p);
        }
        return null;
    }

    public void knob(int i, int delta) {
        if (i < 0 || i >= SpawnTuning.N) return;
        knobs[i] = Math.max(SpawnTuning.MIN, Math.min(SpawnTuning.MAX, knobs[i] + delta));
        setChanged();
    }

    /** Menu sync: 0-3 the knobs (offset by 1000 so they stay positive), 4 status, 5-6 energy (in units of 1,000 FE). */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            if (i < SpawnTuning.N) return knobs[i] + 1000;
            if (i == 4) return status;
            long e = energy.stored() / 1000;
            return i == 5 ? (int) (e & 0xFFFF) : (int) ((e >> 16) & 0xFFFF);
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return 7;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences.breached_anomaly_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AnomalyEngineMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("Cooldown", cooldown);
        tag.putBoolean("Built", built);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putIntArray("Knobs", knobs);
        tag.put("Items", items.serializeNBT(reg));
        tag.putLong("Energy", energy.stored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        cooldown = tag.getInt("Cooldown");
        built = tag.getBoolean("Built");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        int[] k = tag.getIntArray("Knobs");
        for (int i = 0; i < SpawnTuning.N && i < k.length; i++) knobs[i] = k[i];
        if (tag.contains("Items")) items.deserializeNBT(reg, tag.getCompound("Items"));
        energy.setRaw(tag.getLong("Energy"));
    }
}
