package com.theoreticalsciences.deeptime;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * 1.4.0: the creatures brought back from deep time in the Revival Vat. They are wild animals: they cannot be tamed,
 * bred, ridden or led, and they never spawn by themselves (they are extinct). A released one lives where it was let go,
 * and stays (it never despawns). Their minds are their own, not a vanilla animal's.
 */
public final class RevivedMobs {

    /**
     * The Woolly Mammoth (Mammuthus primigenius): six tonnes of Ice Age grazer. It wanders, stops to graze, and keeps
     * near its herd. It never starts a fight, but hurt one and the whole herd nearby turns on you, charging with its tusks.
     */
    public static class Mammoth extends PathfinderMob {
        /** Client side: ticks left of the grazing pose (sent by entity event 10). */
        public int grazeAnim;
        int stepParity;

        public Mammoth(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            this.xpReward = 10;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 90.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                    .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ATTACK_KNOCKBACK, 2.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                    .add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.1);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));      // the charge
            goalSelector.addGoal(4, new KeepWithHerd(this));
            goalSelector.addGoal(5, new Graze(this));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10F));
            goalSelector.addGoal(8, new RandomLookAroundGoal(this));
            targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());   // the herd defends its own
        }

        @Override
        public void handleEntityEvent(byte id) {
            if (id == 10) grazeAnim = 60;
            else super.handleEntityEvent(id);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (grazeAnim > 0) grazeAnim--;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) target.push(0, 0.35, 0);                                // tossed on the tusks
            return hit;
        }

        @Override
        protected void playStepSound(BlockPos pos, BlockState state) {
            if ((stepParity++ & 1) == 0) {
                SoundEvent s = QHSounds.mob("mammoth", "step");
                if (s != null) playSound(s, 0.6F, 0.9F + random.nextFloat() * 0.2F);
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return QHSounds.mob("mammoth", "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return QHSounds.mob("mammoth", "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return QHSounds.mob("mammoth", "death");
        }

        @Override
        public int getAmbientSoundInterval() {
            return 240;
        }

        @Override
        protected float getSoundVolume() {
            return 1.4F;
        }

        @Override
        public boolean removeWhenFarAway(double d) {
            return false;
        }
    }

    /** Wanders back towards the others when it has strayed more than 12 blocks from the nearest of them. */
    static class KeepWithHerd extends Goal {
        final Mammoth m;
        Mammoth other;

        KeepWithHerd(Mammoth m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (m.getRandom().nextInt(80) != 0) return false;
            List<Mammoth> near = m.level().getEntitiesOfClass(Mammoth.class, m.getBoundingBox().inflate(32), x -> x != m);
            other = null;
            double best = Double.MAX_VALUE;
            for (Mammoth x : near) {
                double d = x.distanceToSqr(m);
                if (d < best) { best = d; other = x; }
            }
            return other != null && best > 12 * 12;
        }

        @Override
        public boolean canContinueToUse() {
            return other != null && other.isAlive() && m.distanceToSqr(other) > 6 * 6 && !m.getNavigation().isDone();
        }

        @Override
        public void start() {
            m.getNavigation().moveTo(other, 0.8);
        }
    }

    /** Stands, lowers its head and crops the grass for three seconds (grass only; it leaves the ground as it was). */
    static class Graze extends Goal {
        final Mammoth m;
        int left;

        Graze(Mammoth m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (m.getRandom().nextInt(300) != 0) return false;
            BlockState below = m.level().getBlockState(m.blockPosition().below());
            return below.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || below.is(net.minecraft.tags.BlockTags.DIRT)
                    || m.level().getBlockState(m.blockPosition()).is(net.minecraft.tags.BlockTags.REPLACEABLE);
        }

        @Override
        public void start() {
            left = 60;
            m.getNavigation().stop();
            m.level().broadcastEntityEvent(m, (byte) 10);
        }

        @Override
        public boolean canContinueToUse() {
            return left > 0;
        }

        @Override
        public void tick() {
            if (--left == 20 && m.getHealth() < m.getMaxHealth()) m.heal(2.0F);
        }
    }

    /**
     * Meganeura monyi: a griffinfly of the Carboniferous, seventy centimetres from wingtip to wingtip, in the age when the
     * air held a third more oxygen. It patrols a beat round where it was let go: darts to a point, hovers, darts again,
     * its four wings beating out of step. Harmless to you; hurt it and it flees fast and high.
     */
    public static class Meganeura extends PathfinderMob {
        Vec3 home, aim;
        int retarget, flee;

        public Meganeura(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            this.xpReward = 3;
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.4)
                    .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            if (home == null) home = position();
            Vec3 v = getDeltaMovement().scale(0.8);
            if (--retarget <= 0 || aim == null || aim.distanceToSqr(position()) < 1) {
                aim = pick(flee > 0 ? 20 : 10);
                retarget = flee > 0 ? 20 : 30 + random.nextInt(50);
            }
            if (flee > 0) flee--;
            Vec3 d = aim.subtract(position());
            double dist = d.length();
            if (dist > 1.5) v = v.add(d.scale((flee > 0 ? 0.09 : 0.05) / dist));        // a dart
            else v = v.add((random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02);   // the hover
            setDeltaMovement(v);
            if (v.horizontalDistanceSqr() > 0.0025) {
                float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90F;
                setYRot(Mth.rotLerp(0.4F, getYRot(), yaw));
                yBodyRot = getYRot();
            }
        }

        /** A point in its beat: open air, two to eight blocks over the ground, within r of home. */
        Vec3 pick(int r) {
            Vec3 base = flee > 0 ? position() : home;
            for (int i = 0; i < 10; i++) {
                double x = base.x + (random.nextDouble() * 2 - 1) * r, z = base.z + (random.nextDouble() * 2 - 1) * r;
                int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
                double y = ground + 2 + random.nextDouble() * (flee > 0 ? 10 : 6);
                BlockPos p = BlockPos.containing(x, y, z);
                if (level().isLoaded(p) && level().getBlockState(p).isAir() && level().getBlockState(p.above()).isAir())
                    return new Vec3(x, y, z);
            }
            return position().add(0, 0.5, 0);
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            boolean r = super.hurt(src, amount);
            if (r && !level().isClientSide) { flee = 100; retarget = 0; }
            return r;
        }

        @Override
        public boolean causeFallDamage(float dist, float mult, DamageSource src) {
            return false;
        }

        @Override
        protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

        @Override
        protected SoundEvent getAmbientSound() {
            return QHSounds.mob("meganeura", "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return QHSounds.mob("meganeura", "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return QHSounds.mob("meganeura", "death");
        }

        @Override
        public int getAmbientSoundInterval() {
            return 100;
        }

        @Override
        public boolean removeWhenFarAway(double d) {
            return false;
        }

        @Override
        public void addAdditionalSaveData(CompoundTag t) {
            super.addAdditionalSaveData(t);
            if (home != null) { t.putDouble("HomeX", home.x); t.putDouble("HomeY", home.y); t.putDouble("HomeZ", home.z); }
        }

        @Override
        public void readAdditionalSaveData(CompoundTag t) {
            super.readAdditionalSaveData(t);
            if (t.contains("HomeX")) home = new Vec3(t.getDouble("HomeX"), t.getDouble("HomeY"), t.getDouble("HomeZ"));
        }
    }

    /** Lets a revived creature go: it appears on the clicked face, wild, and stays. */
    public static class CapsuleItem extends com.theoreticalsciences.item.LoreItem {
        final java.util.function.Supplier<? extends EntityType<? extends PathfinderMob>> type;

        public CapsuleItem(java.util.function.Supplier<? extends EntityType<? extends PathfinderMob>> type, Properties p) {
            super(p.stacksTo(16));
            this.type = type;
        }

        @Override
        public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof net.minecraft.server.level.ServerLevel sl)) return net.minecraft.world.InteractionResult.SUCCESS;
            BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
            PathfinderMob m = type.get().create(sl);
            if (m == null) return net.minecraft.world.InteractionResult.FAIL;
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sl.random.nextFloat() * 360, 0);
            if (!sl.noCollision(m)) return net.minecraft.world.InteractionResult.FAIL;
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
            m.setPersistenceRequired();
            sl.addFreshEntity(m);
            sl.playSound(null, at, net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.NEUTRAL, 0.8F, 0.8F);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, m.getX(), m.getY() + 0.5, m.getZ(), 20, 0.5, 0.5, 0.5, 0.02);
            if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
            return net.minecraft.world.InteractionResult.CONSUME;
        }
    }

    private RevivedMobs() {}
}
