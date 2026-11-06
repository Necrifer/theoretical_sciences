package com.theoreticalsciences.inner;

import com.theoreticalsciences.bio.Immunity;
import com.theoreticalsciences.entity.OwnAI;
import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.2.0: the creatures of Inner Space. Each acts out what its cell does in a body and thinks for itself; none changes a block.
 * <ul>
 *   <li><b>Red Cell</b> (the Bloodstream): a disc carried by the current. Climb on and it carries you too.</li>
 *   <li><b>Macrophage</b> (Tissue, the Bloodstream): slow, and it engulfs whatever it catches: a player is held inside and
 *   digested until they strike their way out; items and red cells it simply eats (and gives back the items when it dies).</li>
 *   <li><b>T Cell</b> (the Lymph Node): it patrols, and attacks anything infected, the player included, and the anomalies that
 *   infect. A player who is not infected it leaves alone, unless struck.</li>
 *   <li><b>Polymerase</b> (the Nucleus): it walks along, copying the strand it is on; harmless.</li>
 * </ul>
 */
public final class InnerMobs {
    static final DustParticleOptions STRAND = new DustParticleOptions(new Vector3f(0.6F, 0.9F, 1.0F), 0.8F);
    static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.75F, 0.1F, 0.12F), 1.0F);

    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), kind);
    }

    /** A gentle base: no vanilla habits, its own sounds, never hostile. */
    public abstract static class Gentle extends PathfinderMob {
        protected Gentle(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return dist > 96 * 96;
        }

        /** A cell weighs next to nothing in a body: a fall does not hurt it. */
        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }
    }

    // ================================================================== Red Cell
    public static class RedCell extends Gentle {
        public RedCell(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.15);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            Vec3 v = getDeltaMovement();
            if (Inner.inPlasma(this) || isInWater()) {
                // carried by the current, and riding up to the surface (a cell is a little lighter than plasma)
                Vec3 c = Inner.current(level(), getX(), getZ()).scale(isVehicle() ? 2.2 : 1.4);
                v = new Vec3(v.x * 0.8 + c.x, Math.min(0.08, v.y + 0.03), v.z * 0.8 + c.z);
                setDeltaMovement(v);
                setYRot((float) (Math.atan2(-c.x, c.z) * 180 / Math.PI));
            }
        }

        @Override
        public InteractionResult mobInteract(Player player, InteractionHand hand) {
            if (isVehicle() || player.isSecondaryUseActive()) return super.mobInteract(player, hand);
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        @Override
        public boolean canBeLeashed() {
            return false;
        }

    }

    // ================================================================== Macrophage
    public static class Macrophage extends InteriorMobs.Base {
        static final EntityDataAccessor<Boolean> FULL = SynchedEntityData.defineId(Macrophage.class, EntityDataSerializers.BOOLEAN);
        UUID prey;
        int hold;
        final List<ItemStack> eaten = new ArrayList<>();

        public Macrophage(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 8;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.ARMOR, 2.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.17).add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(FULL, false);
        }

        /** Holding something inside it (for the renderer: it swells round its meal). */
        public boolean full() {
            return entityData.get(FULL);
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, false);
            // a tumour is the macrophage's work too: it goes for the Hydra on sight
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, TumourHydra.Hydra.class, false));
        }

        /** A cell weighs next to nothing in a body: a fall does not hurt it. */
        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        /** Inner Space is lit like noon: unlike an Overworld monster, it does not wait for the dark. */
        @Override
        public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
            return true;
        }


        @Override
        public boolean doHurtTarget(Entity t) {
            boolean hit = super.doHurtTarget(t);
            if (hit && hold <= 0 && t instanceof Player p && Inner.live(p)) {
                prey = p.getUUID();
                hold = 80;
                entityData.set(FULL, true);
                level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1F, 0.6F);
                if (p instanceof net.minecraft.server.level.ServerPlayer sp)
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.engulfed")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return hit;
        }

        void release() {
            prey = null;
            hold = 0;
            entityData.set(FULL, !eaten.isEmpty());
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            // a meal that fights back is let go
            if (prey != null && src.getEntity() != null && prey.equals(src.getEntity().getUUID())) release();
            return super.hurt(src, amount);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (full() && random.nextInt(4) == 0) level().addParticle(ParticleTypes.BUBBLE_POP, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
                return;
            }
            if (hold > 0) {
                Player p = prey == null ? null : level().getPlayerByUUID(prey);
                if (p == null || !p.isAlive() || !Inner.live(p) || p.distanceToSqr(this) > 36) {
                    release();
                } else {
                    // held inside: drawn to its middle, slowed to nothing, and digested a little each second
                    Vec3 c = position().add(0, 0.2, 0);
                    p.setDeltaMovement(c.subtract(p.position()).scale(0.4));
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, false));
                    if (hold % 20 == 0) p.hurt(damageSources().mobAttack(this), 2F);
                    if (--hold <= 0) release();
                }
            }
            if (tickCount % 10 == 0) {
                // anything loose it touches it eats: items (kept, and given back when it dies) and red cells
                for (ItemEntity it : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.4))) {
                    if (eaten.size() >= 9) break;
                    eaten.add(it.getItem().copy());
                    it.discard();
                    entityData.set(FULL, true);
                }
                for (RedCell rc : level().getEntitiesOfClass(RedCell.class, getBoundingBox().inflate(0.3))) {
                    if (rc.isVehicle()) continue;
                    rc.hurt(damageSources().mobAttack(this), 100F);
                    heal(4F);
                }
            }
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean byPlayer) {
            super.dropCustomDeathLoot(level, src, byPlayer);
            for (ItemStack s : eaten) spawnAtLocation(s);
            eaten.clear();
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            ListTag l = new ListTag();
            for (ItemStack s : eaten) if (!s.isEmpty()) l.add(s.save(registryAccess()));
            tag.put("Eaten", l);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            eaten.clear();
            for (Tag t : tag.getList("Eaten", Tag.TAG_COMPOUND)) ItemStack.parse(registryAccess(), t).ifPresent(eaten::add);
            entityData.set(FULL, !eaten.isEmpty());
        }
    }

    // ================================================================== T Cell
    public static class TCell extends InteriorMobs.Base {
        public TCell(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 6;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        /** Inner Space is lit like noon: unlike an Overworld monster, it does not wait for the dark. */
        @Override
        public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
            return true;
        }

        /** A cell weighs next to nothing in a body: a fall does not hurt it. */
        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        /** What a T cell is for: anything carrying an infection, and the anomalies that spread them. */
        static boolean foreign(LivingEntity e) {
            if (e instanceof Player p) return Inner.live(p) && Immunity.infected(p);
            if (e instanceof TumourHydra.Hydra) return true;
            for (Immunity.Infection i : Immunity.Infection.values()) if (i.isSource(e.getType())) return true;
            return false;
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            targetSelector.addGoal(1, new HurtByTargetGoal(this, TCell.class));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, TCell::foreign));
        }

        /** A T cell is not fooled for long: it lets go of a player who is no longer infected and did not strike it. */
        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide || tickCount % 20 != 0) return;
            if (getTarget() instanceof Player p && !foreign(p) && getLastHurtByMob() != p) setTarget(null);
        }
    }

    // ================================================================== Polymerase
    /**
     * The Polymerase works the DNA. It looks for the nearest helix (its rungs are Nucleotide Clusters), walks to it, and
     * climbs it rung by rung, copying as it goes (a strand of light trails behind it); at the top it turns and works its way
     * back down. Knocked off, it climbs back on. Harmless.
     */
    public static class Polymerase extends Gentle {
        static final EntityDataAccessor<Boolean> ON_DNA = SynchedEntityData.defineId(Polymerase.class, EntityDataSerializers.BOOLEAN);
        BlockPos rung, seek;
        int dir = 1, look, copied;

        public Polymerase(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(ON_DNA, false);
        }

        /** On the helix and working (for the renderer: it clamps shut). */
        public boolean working() {
            return entityData.get(ON_DNA);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        static boolean isRung(Level l, BlockPos p) {
            return l.getBlockState(p).is(com.theoreticalsciences.registry.QHBlocks.get("nucleotide_cluster")) && l.getBlockState(p.above()).isAir();
        }

        /** The nearest exposed rung within r (the helix spires' rungs; buried ore has nothing above it). */
        BlockPos nearestRung(int r, int dyLo, int dyHi) {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-r, dyLo, -r), blockPosition().offset(r, dyHi, r)))
                if (isRung(level(), p)) {
                    double d = p.distSqr(blockPosition());
                    if (d < bd) {
                        bd = d;
                        best = p.immutable();
                    }
                }
            return best;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if ((working() || walkAnimation.speed() > 0.05F) && random.nextInt(2) == 0)
                    level().addParticle(STRAND, getX() - Math.sin(Math.toRadians(-yBodyRot)) * 0.6, getY() + 0.4,
                            getZ() - Math.cos(Math.toRadians(-yBodyRot)) * 0.6, 0, 0, 0);
                return;
            }
            if (rung != null) {
                // climbing: glide onto the next rung, one base pair at a time
                Vec3 to = Vec3.atBottomCenterOf(rung.above());
                Vec3 d = to.subtract(position());
                if (d.lengthSqr() > 0.04) {
                    setDeltaMovement(d.normalize().scale(Math.min(0.08, d.length())));
                    float yaw = (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
                    if (d.x * d.x + d.z * d.z > 1e-4) {
                        setYRot(yaw);
                        yBodyRot = yaw;
                    }
                } else {
                    setDeltaMovement(Vec3.ZERO);
                    copied++;
                    BlockPos next = null;
                    for (int tries = 0; tries < 2 && next == null; tries++) {
                        next = nextRung(rung, dir);
                        if (next == null) dir = -dir;                   // the end of the strand: turn and work back
                    }
                    if (next == null || !isRung(level(), rung)) detach();
                    else rung = next;
                }
                if (rung != null && !isRung(level(), rung)) detach();
                return;
            }
            // off the helix: find one, walk to it, and climb on when it is close
            if (--look <= 0) {
                look = 60;
                seek = nearestRung(14, -6, 10);
            }
            if (seek != null) {
                if (seek.distToCenterSqr(position()) < 9) attach(seek);
                else if (getNavigation().isDone()) getNavigation().moveTo(seek.getX() + 0.5, seek.getY() + 1, seek.getZ() + 0.5, 1.0);
            } else if (getNavigation().isDone() && random.nextInt(60) == 0) {
                Vec3 w = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(this, 10, 4);
                if (w != null) getNavigation().moveTo(w.x, w.y, w.z, 0.8);
            }
        }

        /** The rung one or two levels up (dir 1) or down (dir -1), close beside this one: the next base pair along the helix. */
        BlockPos nextRung(BlockPos from, int dir) {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (int dy = 1; dy <= 2; dy++)
                for (BlockPos p : BlockPos.betweenClosed(from.offset(-3, dir * dy, -3), from.offset(3, dir * dy, 3)))
                    if (isRung(level(), p)) {
                        double d = p.distSqr(from) + dy * 4;
                        if (d < bd) {
                            bd = d;
                            best = p.immutable();
                        }
                    }
            return best;
        }

        void attach(BlockPos r) {
            rung = r;
            seek = null;
            getNavigation().stop();
            setNoGravity(true);
            noPhysics = true;
            entityData.set(ON_DNA, true);
        }

        void detach() {
            rung = null;
            setNoGravity(false);
            noPhysics = false;
            entityData.set(ON_DNA, false);
            look = 0;
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            boolean hit = super.hurt(src, amount);
            if (hit && rung != null && src.getEntity() != null) detach();      // knocked off; it will climb back on
            return hit;
        }
    }

    private InnerMobs() {}
}
