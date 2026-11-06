package com.theoreticalsciences.inner;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.interior.InteriorMobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 1.2.0: the Tumour Hydra, Inner Space's mini-boss, where the Anomaly storyline's Oncogenesis chapter is fought. Cut off a
 * head (every 30 damage cuts one) and the stump is open for five seconds: treat it and the head is gone for good; leave it
 * and two grow back, as a tumour regrows from what surgery leaves behind. A stump is treated by fire (cautery, as Iolaus
 * sealed the Lernaean Hydra's necks) or by striking it with a CAR-T Cell Culture or an Apoptosis Trigger (targeted therapy:
 * the cell is told to die). With no heads left it can be killed. Its bite carries Metastasis. Called with a Metastatic Spore
 * used in the Tumour. No block is changed.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class TumourHydra {
    public static final int START_HEADS = 3, MAX_HEADS = 9, STUMP_TICKS = 100, RISE_TICKS = 60;
    public static final float CUT = 30F;

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItemStack().getItem());
        if (!key.getNamespace().equals(TheoreticalSciences.MODID) || !key.getPath().equals("metastatic_spore")) return;
        if (!Inner.in(sp.level())) return;                 // elsewhere it is only an ingredient
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (!sp.level().getBiome(sp.blockPosition()).is(Inner.TUMOUR)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.hydra_not_here").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel sl = sp.serverLevel();
        if (!sl.getEntitiesOfClass(Hydra.class, sp.getBoundingBox().inflate(96)).isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.hydra_already").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Hydra h = InnerRegistry.TUMOUR_HYDRA.get().create(sl);
        if (h == null) return;
        // it needs open space to rise in: try round where the player is looking, and refuse if it is too cramped
        Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
        boolean placed = false;
        for (int tries = 0; tries < 12 && !placed; tries++) {
            double ang = (tries % 2 == 0 ? 1 : -1) * (tries / 2) * 0.5;
            Vec3 dir = look.yRot((float) ang);
            Vec3 c = sp.position().add(dir.scale(12));
            for (int dy = 3; dy >= -3 && !placed; dy--) {
                h.moveTo(c.x, Math.floor(sp.getY()) + dy, c.z, sp.getYRot() + 180, 0);
                if (roomFor(sl, h)) placed = true;
            }
        }
        if (!placed) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.hydra_cramped").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        sl.addFreshEntity(h);
        if (!sp.isCreative()) e.getItemStack().shrink(1);
        sl.playSound(null, sp.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.5F, 0.6F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.hydra_summoned").withStyle(ChatFormatting.DARK_RED), false);
    }

    /** Open air for the whole body (and a margin for its necks), standing on solid ground. */
    static boolean roomFor(ServerLevel l, Hydra h) {
        if (!l.noCollision(h, h.getBoundingBox().inflate(1.0, 0, 1.0).expandTowards(0, 1.5, 0)) || l.containsAnyLiquid(h.getBoundingBox())) return false;
        BlockPos below = BlockPos.containing(h.getX(), h.getY() - 0.5, h.getZ());
        return l.getBlockState(below).isFaceSturdy(l, below, net.minecraft.core.Direction.UP);
    }

    /** Striking an open stump with a targeted therapy seals it (the item is used up). */
    static boolean therapy(ItemStack s) {
        var k = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
        return k.getNamespace().equals(TheoreticalSciences.MODID) && (k.getPath().equals("car_t_culture") || k.getPath().equals("apoptosis_trigger"));
    }

    public static class Hydra extends InteriorMobs.Base {
        /** Which of its nine places round the body have a head (a bit each), and the place of the open stump (-1: none). */
        static final EntityDataAccessor<Integer> MASK = SynchedEntityData.defineId(Hydra.class, EntityDataSerializers.INT);
        static final EntityDataAccessor<Integer> STUMP_SLOT = SynchedEntityData.defineId(Hydra.class, EntityDataSerializers.INT);
        static final EntityDataAccessor<Integer> STUMP = SynchedEntityData.defineId(Hydra.class, EntityDataSerializers.INT);
        static final int SLOTS = MAX_HEADS, START_MASK = 0b001001001;
        final float[] cutAt = new float[SLOTS];
        static final EntityDataAccessor<Integer> RISE = SynchedEntityData.defineId(Hydra.class, EntityDataSerializers.INT);
        int clock;
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.theoreticalsciences.tumour_hydra"),
                BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);

        static final EntityDataAccessor<Integer> BITE = SynchedEntityData.defineId(Hydra.class, EntityDataSerializers.INT);
        /** Three hit boxes for each place: the head, and two along its neck (a blow anywhere on a neck is a blow to that head). */
        final HeadPart[] parts = new HeadPart[SLOTS * 3];

        public Hydra(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 100;
            for (int k = 0; k < parts.length; k++) parts[k] = new HeadPart(this, k / 3, k % 3);
            setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        }

        /**
         * Where head k of n rests (with its idle sway), in the model's own blocks: +z forward, y up. Returns the neck's base
         * (x, y, z), the head (x, y, z) and the neck's outward angle. The renderer draws it here and its hit box sits here.
         */
        public static float[] headLocal(int k, float t) {
            float a = k / (float) SLOTS * net.minecraft.util.Mth.TWO_PI + net.minecraft.util.Mth.PI * 0.5F;
            float ca = net.minecraft.util.Mth.cos(a), sa = net.minecraft.util.Mth.sin(a);
            float sway = 0.9F * net.minecraft.util.Mth.sin(t * 0.07F + k * 1.3F), lift = 0.6F * net.minecraft.util.Mth.sin(t * 0.06F + k * 0.9F);
            float bx = ca * 1.9F, bz = sa * 1.7F, by = 1.6F + (k % 2) * 0.6F;
            return new float[]{bx, by, bz, bx + ca * 3.0F + sa * sway, by + 2.2F + lift, bz + sa * 3.0F - ca * sway, a};
        }

        /** The head now biting (-1: none), and how far into its strike, 0 to 1 (both from the synced BITE). */
        public int bitingHead() {
            int b = entityData.get(BITE);
            return b < 0 || (tickCount - b % 100000 + 100000) % 100000 > 14 ? -1 : b / 100000;
        }

        public float biteProgress(float partial) {
            int b = entityData.get(BITE);
            if (b < 0) return 0;
            float age = (tickCount - b % 100000 + 100000) % 100000 + partial;
            return age > 14 ? 0 : age / 14F;
        }

        /** Model space to the world: the model faces +z and is turned by the body's heading. */
        Vec3 world(float lx, float ly, float lz) {
            double yaw = Math.toRadians(yBodyRot);
            return new Vec3(getX() + lx * Math.cos(yaw) - lz * Math.sin(yaw), getY() + ly, getZ() + lx * Math.sin(yaw) + lz * Math.cos(yaw));
        }

        /** A point along neck k, u from its base (0) to the head (1), in model space (the same curve the renderer draws). */
        public static float[] neckLocal(int k, float u, float t) {
            float[] g = headLocal(k, t);
            float a = g[6], mx = g[0] + net.minecraft.util.Mth.cos(a) * 2.4F, my = g[1] + 0.2F, mz = g[2] + net.minecraft.util.Mth.sin(a) * 2.4F;
            float iu = 1 - u;
            return new float[]{iu * iu * g[0] + 2 * iu * u * mx + u * u * g[3], iu * iu * g[1] + 2 * iu * u * my + u * u * g[4],
                    iu * iu * g[2] + 2 * iu * u * mz + u * u * g[5]};
        }

        /** Where head k is in the world now. */
        Vec3 headPos(int k) {
            float[] g = headLocal(k, tickCount);
            return world(g[3], g[4] + 0.3F, g[5]);
        }

        public boolean alive(int k) {
            return (entityData.get(MASK) >> k & 1) != 0;
        }

        /** The place of the open stump (-1 when none is open). */
        public int stumpSlot() {
            return stump() > 0 ? entityData.get(STUMP_SLOT) : -1;
        }

        @Override
        public boolean isMultipartEntity() {
            return true;
        }

        @Override
        public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
            return parts;
        }

        @Override
        public void setId(int id) {
            super.setId(id);
            for (int k = 0; k < parts.length; k++) parts[k].setId(id + k + 1);
        }

        void moveParts() {
            for (int k = 0; k < parts.length; k++) {
                HeadPart pt = parts[k];
                int slot = k / 3, seg = k % 3;
                Vec3 at;
                if (alive(slot)) {
                    if (seg == 0) at = headPos(slot);
                    else {
                        float[] q = neckLocal(slot, seg == 1 ? 0.68F : 0.36F, tickCount);
                        at = world(q[0], q[1], q[2]);
                    }
                } else if (slot == stumpSlot() && seg == 2) {
                    float[] q = neckLocal(slot, 0.72F, tickCount);         // the stump: the cut neck, right where the head came off
                    at = world(q[0], q[1] + 0.3F, q[2]);
                } else at = position().add(0, 2, 0);
                pt.xo = pt.getX();
                pt.yo = pt.getY();
                pt.zo = pt.getZ();
                pt.xOld = pt.getX();
                pt.yOld = pt.getY();
                pt.zOld = pt.getZ();
                pt.setPos(at.x, at.y - 0.9, at.z);
            }
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ARMOR, 6.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                    .add(Attributes.STEP_HEIGHT, 1.5);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(MASK, START_MASK);
            b.define(STUMP_SLOT, -1);
            b.define(STUMP, 0);
            b.define(RISE, 0);
            b.define(BITE, -1);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
            com.theoreticalsciences.entity.OwnAI.hunter(this);
            targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, InnerMobs.Macrophage.class, true));
            targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, InnerMobs.TCell.class, true));
        }

        public int heads() {
            return Integer.bitCount(entityData.get(MASK));
        }

        /** Ticks left on the open stump (0: none open). */
        public int stump() {
            return entityData.get(STUMP);
        }

        public boolean rising() {
            return entityData.get(RISE) < RISE_TICKS;
        }

        public float rise(float partial) {
            return Math.min(1F, (entityData.get(RISE) + (rising() ? partial : 0)) / RISE_TICKS);
        }

        void tell(String key, ChatFormatting col) {
            for (ServerPlayer p : bar.getPlayers()) p.displayClientMessage(Component.translatable(key).withStyle(col), true);
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            // a blow to the body: while it has heads, the mass soaks most of it and cuts nothing; with none left, it all lands
            return hurt(src, heads() > 0 ? amount * 0.25F : amount, -1);
        }

        /** A blow to head k or its neck (HeadPart): full damage, and it counts toward cutting that head off. */
        boolean hurtHead(int k, DamageSource src, float amount) {
            return hurt(src, amount, k);
        }

        boolean hurt(DamageSource src, float amount, int slot) {
            boolean head = slot >= 0;
            if (src.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) || src.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING)) return false;
            if (src.getEntity() == this || rising() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            if (level().isClientSide) return super.hurt(src, amount);
            // targeted therapy on an open stump: it is sealed
            if (stump() > 0 && src.getEntity() instanceof Player p && therapy(p.getMainHandItem())) {
                if (!p.isCreative()) p.getMainHandItem().shrink(1);
                seal();
            }
            int h = heads();
            // while it has a head left it cannot die: the cut takes a head, not its life
            float raw = amount;
            if (h > 0) amount = Math.max(0.01F, Math.min(amount, getHealth() - 1F));
            boolean hit = super.hurt(src, amount);
            if (hit && h > 0 && head && alive(slot)) {
                cutAt[slot] += raw;
                if (cutAt[slot] >= CUT && stump() == 0) {
                    // that head comes off, and its stump is left right there
                    cutAt[slot] = 0;
                    entityData.set(MASK, entityData.get(MASK) & ~(1 << slot));
                    entityData.set(STUMP_SLOT, slot);
                    entityData.set(STUMP, STUMP_TICKS);
                    Vec3 hp = headPos(slot);
                    ((ServerLevel) level()).sendParticles(InnerMobs.BLOOD, hp.x, hp.y, hp.z, 30, 0.5, 0.4, 0.5, 0.05);
                    level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5F, 0.6F);
                    tell("message.theoreticalsciences.hydra_cut", ChatFormatting.GOLD);
                }
            }
            return hit;
        }

        void seal() {
            entityData.set(STUMP, 0);
            ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.2, getZ(), 20, 0.4, 0.3, 0.4, 0.02);
            level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1F, 1F);
            tell("message.theoreticalsciences.hydra_sealed", ChatFormatting.GREEN);
        }

        @Override
        public boolean doHurtTarget(net.minecraft.world.entity.Entity t) {
            return false;                                       // the body does not fight: its heads do (aiStep)
        }

        @Override
        public void aiStep() {
            super.aiStep();
            moveParts();
            if (level().isClientSide) return;
            ServerLevel sl = (ServerLevel) level();
            if (rising()) {
                int r = entityData.get(RISE) + 1;
                entityData.set(RISE, r);
                setDeltaMovement(Vec3.ZERO);
                sl.sendParticles(InnerMobs.BLOOD, getX(), getY() + r / (double) RISE_TICKS * 2, getZ(), 4, 0.8, 0.3, 0.8, 0.02);
                return;
            }
            clock++;
            int s = stump();
            if (s > 0) {
                if (isOnFire()) seal();                         // cautery
                else if (s == 1) {
                    // untreated: two heads grow back where one was cut (the cut place and the nearest free one beside it)
                    int st = entityData.get(STUMP_SLOT), m = entityData.get(MASK);
                    if (st >= 0) {
                        m |= 1 << st;
                        for (int d = 1; d <= SLOTS / 2; d++) {
                            int a1 = (st + d) % SLOTS, a2 = (st - d + SLOTS) % SLOTS;
                            if ((m >> a1 & 1) == 0) { m |= 1 << a1; break; }
                            if ((m >> a2 & 1) == 0) { m |= 1 << a2; break; }
                        }
                        entityData.set(MASK, m);
                    }
                    heal(CUT);
                    sl.sendParticles(InnerMobs.BLOOD, getX(), getY() + 2.6, getZ(), 40, 0.6, 0.4, 0.6, 0.08);
                    level().playSound(null, blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 2F, 0.5F);
                    tell("message.theoreticalsciences.hydra_regrew", ChatFormatting.RED);
                    entityData.set(STUMP, 0);
                } else entityData.set(STUMP, s - 1);
            }
            LivingEntity tg = getTarget();
            // the body only carries the heads: it closes until its heads can reach (about five blocks off), and no nearer
            if (tg != null && tg.isAlive()) {
                double d = Math.sqrt(distanceToSqr(tg.getX(), getY(), tg.getZ()));
                if (d > 6.5) getNavigation().moveTo(tg, 1.0);
                else {
                    getNavigation().stop();
                    if (d < 3.5) {
                        Vec3 away = position().subtract(tg.position()).multiply(1, 0, 1).normalize().scale(0.12);
                        setDeltaMovement(getDeltaMovement().add(away));
                    }
                }
            }
            // the heads strike: every second, the head nearest something within reach lunges at it and bites
            if (clock % 20 == 10) {
                int n = SLOTS;
                java.util.List<LivingEntity> prey = sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(9), e ->
                        e != this && e.isAlive() && (e instanceof Player pl ? Inner.live(pl) : e == tg || e instanceof InnerMobs.Macrophage || e instanceof InnerMobs.TCell));
                boolean[] used = new boolean[Math.max(1, n)];
                for (LivingEntity v : prey) {
                    int best = -1;
                    double bd = 4.2 * 4.2;
                    for (int k = 0; k < n; k++) {
                        if (used[k] || !alive(k)) continue;
                        double dd = headPos(k).distanceToSqr(v.getX(), v.getY() + v.getBbHeight() * 0.5, v.getZ());
                        if (dd < bd) {
                            bd = dd;
                            best = k;
                        }
                    }
                    if (best < 0) continue;
                    used[best] = true;
                    entityData.set(BITE, best * 100000 + tickCount % 100000);
                    Vec3 h = headPos(best);
                    if (v.hurt(damageSources().mobAttack(this), 5F) && v instanceof ServerPlayer spp)
                        com.theoreticalsciences.bio.Immunity.infect(spp, com.theoreticalsciences.bio.Immunity.Infection.METASTASIS);
                    sl.sendParticles(InnerMobs.BLOOD, v.getX(), v.getY() + 1, v.getZ(), 8, 0.3, 0.3, 0.3, 0.03);
                    level().playSound(null, BlockPos.containing(h), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1F, 0.6F);
                }
            }
            // with many heads it spits: a spray of tumour cells at everyone it can see
            LivingEntity t = getTarget();
            if (heads() >= 5 && clock % 80 == 40 && t != null) {
                for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(16))) {
                    if (!Inner.live(p) || !hasLineOfSight(p)) continue;
                    Vec3 a = position().add(0, 2.5, 0), b = p.position().add(0, 1, 0);
                    for (int i = 0; i <= 8; i++) {
                        Vec3 q = a.lerp(b, i / 8.0);
                        sl.sendParticles(InnerMobs.BLOOD, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0);
                    }
                    p.hurt(damageSources().mobAttack(this), 3F);
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                }
            }
            bar.setName(Component.translatable("message.theoreticalsciences.hydra_bar", heads()));
            bar.setProgress(heads() == 0 ? getHealth() / getMaxHealth() : heads() / (float) MAX_HEADS);
            bar.setColor(stump() > 0 ? BossEvent.BossBarColor.YELLOW : heads() == 0 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PINK);
            if (clock % 20 == 0)
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) bar.addPlayer(p);
            for (ServerPlayer p : java.util.List.copyOf(bar.getPlayers()))
                if (!p.isAlive() || p.level() != level() || p.distanceTo(this) > 64) bar.removePlayer(p);
        }

        @Override
        public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
            return true;
        }

        @Override
        public boolean fireImmune() {
            return false;
        }

        @Override
        public void remove(RemovalReason r) {
            super.remove(r);
            bar.removeAllPlayers();
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putInt("HeadMask", entityData.get(MASK));
            tag.putInt("StumpSlot", entityData.get(STUMP_SLOT));
            tag.putInt("Rise", entityData.get(RISE));
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("HeadMask")) entityData.set(MASK, tag.getInt("HeadMask"));
            if (tag.contains("StumpSlot")) entityData.set(STUMP_SLOT, tag.getInt("StumpSlot"));
            entityData.set(RISE, tag.contains("Rise") ? tag.getInt("Rise") : RISE_TICKS);
        }
    }

    /**
     * One of the Hydra's heads: a hit box of its own (as the Ender Dragon's parts are), so it is the heads you strike.
     * A blow to a head counts toward cutting it off; a blow to the body barely touches it.
     */
    public static class HeadPart extends net.neoforged.neoforge.entity.PartEntity<Hydra> {
        final int slot, seg;
        static final net.minecraft.world.entity.EntityDimensions HEAD = net.minecraft.world.entity.EntityDimensions.scalable(2.0F, 1.8F),
                NECK = net.minecraft.world.entity.EntityDimensions.scalable(1.5F, 1.5F);

        HeadPart(Hydra parent, int slot, int seg) {
            super(parent);
            this.slot = slot;
            this.seg = seg;
            refreshDimensions();
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {}

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {}

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {}

        @Override
        public boolean isPickable() {
            Hydra h = getParent();
            return !h.rising() && (h.alive(slot) || seg == 2 && h.stumpSlot() == slot);
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            return isPickable() && !isInvulnerableTo(src) && getParent().hurtHead(slot, src, amount);
        }

        @Override
        public boolean is(net.minecraft.world.entity.Entity e) {
            return this == e || getParent() == e;
        }

        @Override
        public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
            return seg == 0 ? HEAD : NECK;
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public ItemStack getPickResult() {
            return getParent().getPickResult();
        }
    }

    private TumourHydra() {}
}
