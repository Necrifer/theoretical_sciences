package com.theoreticalsciences.home;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHSounds;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

/**
 * 1.9.0: who lives in Omnipotency. Five that are at peace (the Halo Lamb, the Cherub, the Choir Seraph, the Cloud Ray
 * and the Dweller) and one that keeps watch (the Threshold Warden), which stands as still as a statue until something
 * of the house is broken, an urn is opened, or one of the others is struck. Every one has its own mind: none is built
 * on a vanilla creature.
 */
public final class HomeMobs {
    private HomeMobs() {}

    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), kind);
    }

    static boolean isBlock(BlockState s, String... ids) {
        for (String id : ids) if (s.is(QHBlocks.get(id))) return true;
        return false;
    }

    /** The nearest block of the given kinds within a box round the mob, or null. Checked a few times a minute at most. */
    @Nullable
    static BlockPos nearest(PathfinderMob mob, int r, int up, String... ids) {
        BlockPos o = mob.blockPosition(), best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -up, -r), o.offset(r, up, r))) {
            if (!isBlock(mob.level().getBlockState(p), ids)) continue;
            double d = p.distSqr(o);
            if (d < bd) {
                bd = d;
                best = p.immutable();
            }
        }
        return best;
    }

    /** Everything here that is not the Warden. Striking one wakes the Wardens near. */
    public abstract static class Resident extends PathfinderMob {
        protected Resident(EntityType<? extends PathfinderMob> type, Level level) {
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
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public int getAmbientSoundInterval() {
            return 240;
        }
    }

    abstract static class Flier extends Resident {
        protected Flier(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            this.moveControl = new FlyingMoveControl(this, 20, true);
            this.setNoGravity(true);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanFloat(true);
            return nav;
        }

        @Override
        protected void checkFallDamage(double y, boolean g, BlockState s, BlockPos p) {}
    }

    /** Flies to a block of some kind, stays by it a while (doing whatever {@code work} does), and leaves. */
    static class VisitGoal extends Goal {
        private final PathfinderMob mob;
        private final String[] ids;
        private final double speed, hover;
        private final Runnable work;
        private BlockPos target;
        private int stay, cooldown;

        VisitGoal(PathfinderMob mob, double speed, double hover, Runnable work, String... ids) {
            this.mob = mob;
            this.speed = speed;
            this.hover = hover;
            this.work = work;
            this.ids = ids;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown-- > 0 || mob.getRandom().nextInt(60) != 0) return false;
            target = nearest(mob, 12, 6, ids);
            cooldown = 200;
            return target != null;
        }

        @Override
        public void start() {
            stay = 80 + mob.getRandom().nextInt(80);
        }

        @Override
        public boolean canContinueToUse() {
            return stay > 0 && target != null && isBlock(mob.level().getBlockState(target), ids);
        }

        @Override
        public void tick() {
            Vec3 at = Vec3.atCenterOf(target).add(0, hover, 0);
            if (mob.position().distanceToSqr(at) > 2.5) {
                mob.getNavigation().moveTo(at.x, at.y, at.z, speed);
            } else {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(Vec3.atCenterOf(target));
                stay--;
                if (stay % 10 == 0) work.run();
            }
        }

        @Override
        public void stop() {
            cooldown = 300 + mob.getRandom().nextInt(300);
            target = null;
        }
    }

    // ================================================================== Halo Lamb
    /** A small lamb with a fleece the colour of first light and a ring of it over its head. It grazes the Halo Grass. */
    public static class HaloLamb extends Resident {
        public int grazeAnim;

        public HaloLamb(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.22);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, new PanicGoal(this, 1.5));
            goalSelector.addGoal(3, new Goal() {             // grazing: it stops, lowers its head, and the grass is none the worse
                private int left;

                {
                    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
                }

                @Override
                public boolean canUse() {
                    return getRandom().nextInt(400) == 0 && isBlock(level().getBlockState(blockPosition().below()), "halo_grass");
                }

                @Override
                public void start() {
                    left = 40;
                    getNavigation().stop();
                    level().broadcastEntityEvent(HaloLamb.this, (byte) 10);
                }

                @Override
                public boolean canContinueToUse() {
                    return left > 0;
                }

                @Override
                public void tick() {
                    left--;
                }
            });
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        }

        @Override
        public void handleEntityEvent(byte id) {
            if (id == 10) grazeAnim = 40;
            else super.handleEntityEvent(id);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide && grazeAnim > 0) grazeAnim--;
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }
    }

    // ================================================================== Cherub
    /** A small winged keeper of the gardens. It tends the Glory Lanterns and the golden trees, and is curious about visitors. */
    public static class Cherub extends Flier {
        public Cherub(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.FLYING_SPEED, 0.5).add(Attributes.MOVEMENT_SPEED, 0.3)
                    .add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new PanicGoal(this, 1.7));
            goalSelector.addGoal(3, new VisitGoal(this, 1.0, 1.2, () -> {
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() - 0.3, getZ(), 3, 0.4, 0.3, 0.4, 0);
            }, "glory_lantern", "aureate_leaves"));
            goalSelector.addGoal(4, new Goal() {             // curiosity: comes to look at a visitor, circles once or twice, loses interest
                private Player who;
                private int left, cooldown;

                {
                    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
                }

                @Override
                public boolean canUse() {
                    if (cooldown-- > 0 || getRandom().nextInt(100) != 0) return false;
                    who = level().getNearestPlayer(Cherub.this, 12);
                    return who != null && !who.isSpectator();
                }

                @Override
                public void start() {
                    left = 120;
                }

                @Override
                public boolean canContinueToUse() {
                    return left-- > 0 && who.isAlive() && distanceToSqr(who) < 400;
                }

                @Override
                public void tick() {
                    double a = left * 0.08 + getId();
                    getNavigation().moveTo(who.getX() + Math.cos(a) * 3.2, who.getEyeY() - 0.6, who.getZ() + Math.sin(a) * 3.2, 1.1);
                    getLookControl().setLookAt(who);
                }

                @Override
                public void stop() {
                    cooldown = 600;
                    who = null;
                }
            });
            goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
            goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        }
    }

    // ================================================================== Choir Seraph
    /** Six wings and a voice. It keeps to the crystal spires, and now and then it sings; those who hear it are mended a little. */
    public static class ChoirSeraph extends Flier {
        public int singAnim;
        private int singCooldown = 200;

        public ChoirSeraph(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.FLYING_SPEED, 0.3).add(Attributes.MOVEMENT_SPEED, 0.2)
                    .add(Attributes.FOLLOW_RANGE, 20.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new PanicGoal(this, 1.5));
            goalSelector.addGoal(3, new VisitGoal(this, 0.8, 2.5, () -> {}, "choir_crystal_block", "choir_bell"));
            goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.6));
            goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10F));
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (--singCooldown > 0) return;
            singCooldown = 500 + random.nextInt(500);
            Player near = level().getNearestPlayer(this, 14);
            if (near == null) return;
            SoundEvent s = snd(this, "sing");
            if (s != null) playSound(s, 2.0F, 1.0F);
            level().broadcastEntityEvent(this, (byte) 11);
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(9)))
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, true));
        }

        @Override
        public void handleEntityEvent(byte id) {
            if (id == 11) singAnim = 90;
            else super.handleEntityEvent(id);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide && singAnim > 0) {
                singAnim--;
                if (singAnim % 6 == 0)
                    level().addParticle(ParticleTypes.NOTE, getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 1.6 + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 1.6,
                            random.nextInt(24) / 24.0, 0, 0);
            }
        }
    }

    // ================================================================== Cloud Ray
    /** A broad, slow ray that swims the Sea of Clouds in wide circles. It has never needed to be afraid of anything. */
    public static class CloudRay extends Resident {
        private Vec3 anchor;
        private double radius = 14, phase;
        private int flee;

        public CloudRay(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.2);
        }

        @Override
        protected void registerGoals() {}

        @Override
        protected void customServerAiStep() {
            if (anchor == null) {
                anchor = position();
                radius = 10 + random.nextDouble() * 10;
                phase = random.nextDouble() * Math.PI * 2;
            }
            double speed = flee > 0 ? 0.42 : 0.16;
            if (flee > 0) flee--;
            phase += speed / radius;
            Vec3 want = anchor.add(Math.cos(phase) * radius, Math.sin(phase * 0.37) * 3.0, Math.sin(phase) * radius);
            Vec3 d = want.subtract(position());
            if (d.lengthSqr() > 1e-4) setDeltaMovement(getDeltaMovement().lerp(d.normalize().scale(Math.min(speed, d.length())), 0.15));
            if (horizontalCollision || verticalCollision) {          // something in the way: circle somewhere else, a little higher
                anchor = anchor.add((random.nextDouble() - 0.5) * 8, 2, (random.nextDouble() - 0.5) * 8);
            }
            Vec3 v = getDeltaMovement();
            if (v.horizontalDistanceSqr() > 1e-5) {
                setYRot((float) (Math.atan2(v.z, v.x) * 180 / Math.PI) - 90F);
                yBodyRot = getYRot();
                yHeadRot = getYRot();
            }
        }

        @Override
        public void travel(Vec3 input) {
            if (isControlledByLocalInstance()) move(MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(false);
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            boolean r = super.hurt(src, amount);
            if (r && !level().isClientSide) {
                flee = 200;
                if (anchor != null) anchor = anchor.add(0, 6, 0);
            }
            return r;
        }

        @Override
        protected void checkFallDamage(double y, boolean g, BlockState s, BlockPos p) {}

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (anchor != null) {
                tag.putDouble("AnchorX", anchor.x);
                tag.putDouble("AnchorY", anchor.y);
                tag.putDouble("AnchorZ", anchor.z);
            }
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("AnchorX")) anchor = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        }
    }

    // ================================================================== Dweller
    /**
     * One of those who live in the house again. It keeps near its own door, tends what needs tending, greets a visitor
     * with a bow, and, spoken to, remembers something aloud.
     */
    public static class Dweller extends Resident {
        public static final int LINES = 18;
        public int bowAnim, workAnim;
        private int greetCooldown, talkCooldown, line = -1;

        public Dweller(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, new PanicGoal(this, 1.4));
            goalSelector.addGoal(2, new Goal() {             // the greeting
                private Player who;
                private int left;

                {
                    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
                }

                @Override
                public boolean canUse() {
                    if (greetCooldown > 0) return false;
                    who = level().getNearestPlayer(Dweller.this, 5);
                    return who != null && !who.isSpectator() && hasLineOfSight(who);
                }

                @Override
                public void start() {
                    left = 50;
                    getNavigation().stop();
                    level().broadcastEntityEvent(Dweller.this, (byte) 12);
                }

                @Override
                public boolean canContinueToUse() {
                    return left-- > 0 && who.isAlive();
                }

                @Override
                public void tick() {
                    getLookControl().setLookAt(who, 30F, 30F);
                }

                @Override
                public void stop() {
                    greetCooldown = 2400;
                }
            });
            goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.9));
            goalSelector.addGoal(4, new VisitGoal(this, 0.9, 0, () -> level().broadcastEntityEvent(this, (byte) 13), "glory_lantern", "choir_bell", "sealed_urn", "halo_glass"));
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        }

        @Nullable
        @Override
        public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
            restrictTo(blockPosition(), 20);
            setPersistenceRequired();
            return super.finalizeSpawn(level, difficulty, reason, data);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (greetCooldown > 0) greetCooldown--;
            if (talkCooldown > 0) talkCooldown--;
        }

        @Override
        protected InteractionResult mobInteract(Player player, InteractionHand hand) {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!level().isClientSide && talkCooldown == 0 && player instanceof ServerPlayer sp) {
                talkCooldown = 60;
                line = line < 0 ? Math.floorMod(getId() * 7, LINES) : (line + 1) % LINES;          // each begins somewhere of its own, then goes on in order
                sp.sendSystemMessage(Component.translatable("chat.type.text", getDisplayName(),
                        Component.translatable("entity.theoreticalsciences.dweller.line." + (line + 1))).withStyle(ChatFormatting.GOLD));
                SoundEvent s = snd(this, "ambient");
                if (s != null) playSound(s, 1F, 1F);
                level().broadcastEntityEvent(this, (byte) 12);
                getLookControl().setLookAt(player, 30F, 30F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        @Override
        public void handleEntityEvent(byte id) {
            if (id == 12) bowAnim = 30;
            else if (id == 13) workAnim = 12;
            else super.handleEntityEvent(id);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (bowAnim > 0) bowAnim--;
                if (workAnim > 0) workAnim--;
            }
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
            tag.putInt("Line", line);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            NbtUtils.readBlockPos(tag, "Home").ifPresent(p -> restrictTo(p, 20));
            line = tag.contains("Line") ? tag.getInt("Line") : -1;
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }
    }

    // ================================================================== Threshold Warden
    /**
     * The one that keeps watch. It kneels at its post, sword point down, and is stone to look at. Break something of
     * the house within its sight, open a sealed urn, or strike one of those who live here, and it stands. It fights
     * only whoever did that, only for as long as they keep at it, and then it goes back and kneels again.
     */
    public static class ThresholdWarden extends PathfinderMob {
        private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(ThresholdWarden.class, EntityDataSerializers.BOOLEAN);
        private BlockPos post;
        private float postYaw;
        private int anger;
        public int strikeAnim;

        public ThresholdWarden(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            setPersistenceRequired();
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 600.0).add(Attributes.ARMOR, 20.0).add(Attributes.ARMOR_TOUGHNESS, 12.0)
                    .add(Attributes.ATTACK_DAMAGE, 28.0).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.STEP_HEIGHT, 1.1);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(AWAKE, false);
        }

        public boolean awake() {
            return entityData.get(AWAKE);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
                @Override
                public boolean canUse() {
                    return awake() && super.canUse();
                }
            });
            goalSelector.addGoal(4, new Goal() {             // back to the post, and down on one knee
                {
                    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
                }

                @Override
                public boolean canUse() {
                    return awake() && getTarget() == null && post != null;
                }

                @Override
                public void tick() {
                    if (post.distToCenterSqr(position()) > 2.0) {
                        if (getNavigation().isDone()) getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.8);
                    } else {
                        getNavigation().stop();
                        setYRot(postYaw);
                        yBodyRot = postYaw;
                        yHeadRot = postYaw;
                        entityData.set(AWAKE, false);
                        heal(getMaxHealth());
                    }
                }
            });
        }

        @Nullable
        @Override
        public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
            post = blockPosition();
            postYaw = getYRot();
            return super.finalizeSpawn(level, difficulty, reason, data);
        }

        /** Someone did something it will not have. */
        public void provoke(Player player) {
            if (level().isClientSide || !isAlive() || player.isCreative() || player.isSpectator() || level().getDifficulty() == Difficulty.PEACEFUL) return;
            if (post == null) {
                post = blockPosition();
                postYaw = getYRot();
            }
            if (!awake()) {
                entityData.set(AWAKE, true);
                SoundEvent s = snd(this, "wake");
                if (s != null) playSound(s, 3.0F, 1.0F);
                if (player instanceof ServerPlayer sp)
                    sp.displayClientMessage(Component.translatable("message.theoreticalsciences.threshold_warden.wakes").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            }
            anger = 600;
            setTarget(player);
        }

        /** Wakes every Warden within {@code r} blocks that can be said to be guarding this spot. */
        public static void provokeNear(Level level, Vec3 at, Player player, double r) {
            if (level.isClientSide) return;
            for (ThresholdWarden w : level.getEntitiesOfClass(ThresholdWarden.class, new AABB(at, at).inflate(r))) w.provoke(player);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (!awake()) {
                getNavigation().stop();
                return;
            }
            LivingEntity t = getTarget();
            if (anger > 0) anger--;
            boolean done = t == null || !t.isAlive() || anger <= 0 || distanceToSqr(t) > 48 * 48 || level().getDifficulty() == Difficulty.PEACEFUL
                    || (t instanceof Player p && (p.isCreative() || p.isSpectator())) || (post != null && post.distToCenterSqr(position()) > 64 * 64);
            if (done && t != null) setTarget(null);
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            boolean r = super.hurt(src, amount);
            if (r && src.getEntity() instanceof Player p) provoke(p);
            return r;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean r = super.doHurtTarget(target);
            if (r) {
                level().broadcastEntityEvent(this, (byte) 14);
                SoundEvent s = snd(this, "strike");
                if (s != null) playSound(s, 2.0F, 1.0F);
                anger = Math.max(anger, 200);
            }
            return r;
        }

        @Override
        public void handleEntityEvent(byte id) {
            if (id == 14) strikeAnim = 12;
            else super.handleEntityEvent(id);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide && strikeAnim > 0) strikeAnim--;
            if (level().isClientSide && awake() && random.nextInt(3) == 0) {          // "like burning coals of fire... and out of the fire went forth lightning"
                double a = random.nextDouble() * Math.PI * 2, r = 0.5 + random.nextDouble() * 0.7;
                level().addParticle(random.nextInt(4) == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME, getX() + Math.cos(a) * r, getY() + 1.25 + (random.nextDouble() - 0.5) * 1.6,
                        getZ() + Math.sin(a) * r, 0, 0.02, 0);
            }
        }

        @Override
        public boolean isPushable() {
            return awake();
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return awake() ? snd(this, "ambient") : null;
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
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (post != null) tag.put("Post", NbtUtils.writeBlockPos(post));
            tag.putFloat("PostYaw", postYaw);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
            postYaw = tag.getFloat("PostYaw");
        }
    }

    // ================================================================== what wakes the Wardens
    /** The blocks the house is built of. Breaking one in a Warden's sight is noticed. */
    private static final Set<String> HALLOWED = Set.of("sealed_urn", "choir_bell", "halo_glass", "empyrean_pillar", "empyrean_marble_tiles", "seraph_gold_trim",
            "empyrean_marble_bricks", "glory_lantern", "remembrance_stone");

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || level.dimension() != QHDimensions.OMNIPOTENCY) return;
        Block b = e.getState().getBlock();
        String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
        if (HALLOWED.contains(id)) ThresholdWarden.provokeNear(level, Vec3.atCenterOf(e.getPos()), e.getPlayer(), 28);
    }

    public static void onHurt(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof Resident r && e.getSource().getEntity() instanceof Player p) ThresholdWarden.provokeNear(r.level(), r.position(), p, 32);
    }
}
