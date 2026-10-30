package com.theoreticalsciences.boss;

import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * Alpha Omega: every concept, rule and dimension of the mod gathered into one thing - drawn as a five-dimensional
 * hypercube turning through itself. Once the Living Proof has melted away it fights like a creature:
 * <ul>
 *   <li>it hovers around its challenger and strikes with the laws it is made of (a collapse ring that decoheres, a
 *       tidal lance that drags you in and spaghettifies, a regress barrage, a false-vacuum bubble, gravity reversal);</li>
 *   <li>at 2/3 and 1/3 of its health it closes itself into a proof: invulnerable, it asks one last question at four
 *       pedestals in the arena. Answer right and the shield breaks, wounding it; answer wrong and it heals;</li>
 *   <li>at 75% and 40% it calls the creatures of its dimensions.</li>
 * </ul>
 * Its death is slow: rays of light pour out of it while the arena fractures, then everyone is thrown back into
 * Hyperdimensional Reality carrying the Proof of Everything.
 */
public class AlphaOmegaEntity extends Monster {
    private static final EntityDataAccessor<Boolean> SHIELD = SynchedEntityData.defineId(AlphaOmegaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(AlphaOmegaEntity.class, EntityDataSerializers.INT);
    public static final int DEATH_TICKS = 200;

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.theoreticalsciences.alpha_omega"), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);
    public UUID session;
    private int cooldown = 80, attackTime, proofsDone, summonsDone, question = -1, correct = -1, stun;
    private Vec3 attackAt;
    private final List<BlockPos> pedestals = new ArrayList<>();

    public AlphaOmegaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = false;
        xpReward = 500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1500).add(Attributes.ARMOR, 20).add(Attributes.ARMOR_TOUGHNESS, 12)
                .add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.FOLLOW_RANGE, 96).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SHIELD, false);
        b.define(ATTACK, 0);
    }

    public boolean shielded() {
        return entityData.get(SHIELD);
    }

    /** 0 = none, 1-5 = the attack being telegraphed (the renderer colours itself by it). */
    public int attack() {
        return entityData.get(ATTACK);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;      // the arena would summon it again every five seconds
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (shielded() || deathTime > 0) {
            if (src.getEntity() instanceof ServerPlayer p && level().getGameTime() % 10 == 0)
                p.displayClientMessage(Component.translatable("boss.theoreticalsciences.alpha_omega.shielded").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return false;
        }
        // only damage with nobody behind it (/kill, the void) may skip the cap. Anything a player or creature deals -
        // even "unblockable" weapons - is capped, so the two proofs can never be skipped.
        if (src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && src.getEntity() == null && src.getDirectEntity() == null)
            return super.hurt(src, amount);
        float capped = Math.min(amount, 60F);      // no single blow may end it
        // and no blow may carry it past the next proof (at 2/3 and 1/3 health): it stops just below the line
        float line = proofsDone == 0 ? 0.667F : proofsDone == 1 ? 0.333F : -1F;
        if (line > 0 && getHealth() > getMaxHealth() * line)
            capped = Math.min(capped, getHealth() - (getMaxHealth() * line - 1F));
        return super.hurt(src, Math.max(0F, capped));
    }

    // ------------------------------------------------------------------ behaviour
    ServerPlayer target() {
        ServerPlayer best = null;
        double bd = 96 * 96;
        for (ServerPlayer p : ((ServerLevel) level()).players()) {
            if (p.isSpectator() || p.isCreative() || !p.isAlive()) continue;
            double d = p.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0)
                level().addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), getX() + (random.nextDouble() - 0.5) * 4, getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 4, 0, 0.02, 0);
            return;
        }
        ServerLevel l = (ServerLevel) level();
        // summoned by a command, copied, or left over from a proof that has ended: it simply goes (no arena to break)
        if (orphan()) {
            discard();
            return;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        ServerPlayer p = target();
        if (p == null) {
            setDeltaMovement(getDeltaMovement().scale(0.8));
            return;
        }
        getLookControl().setLookAt(p, 30, 30);
        // hover in a slow circle above the challenger
        double ang = tickCount * 0.012;
        Vec3 want = new Vec3(p.getX() + Math.cos(ang) * 11, Math.max(ProofArena.Y + 3, p.getY() + 5), p.getZ() + Math.sin(ang) * 11);
        if (shielded()) want = new Vec3(want.x, ProofArena.Y + 9, want.z).lerp(position(), 0.97);
        Vec3 d = want.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(0.85).add(d.normalize().scale(Math.min(0.08, d.length() * 0.02))));
        if (stun > 0) {
            stun--;
            setDeltaMovement(getDeltaMovement().scale(0.5).add(0, -0.01, 0));
            return;
        }
        if (shielded()) {
            if (tickCount % 40 == 0) l.sendParticles(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), getX(), getY() + 2, getZ(), 40, 2, 2, 2, 0.5);
            return;
        }
        // proofs at 2/3 and 1/3, summons at 75% and 40%
        float f = getHealth() / getMaxHealth();
        if (proofsDone == 0 && f < 0.667F || proofsDone == 1 && f < 0.333F) {
            beginProof(l, p);
            return;
        }
        if (summonsDone == 0 && f < 0.75F || summonsDone == 1 && f < 0.40F) {
            summonsDone++;
            summon(l);
        }
        // melee if close
        if (distanceToSqr(p) < 9 && tickCount % 20 == 0) doHurtTarget(p);
        // attacks
        if (attack() == 0 && --cooldown <= 0) {
            int a = 1 + random.nextInt(5);
            entityData.set(ATTACK, a);
            attackTime = 0;
            attackAt = p.position();
            l.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.CHARGE.get(), SoundSource.HOSTILE, 3F, 0.6F + a * 0.15F);
        }
        if (attack() != 0) runAttack(l, p, attack(), attackTime++);
    }

    private void runAttack(ServerLevel l, ServerPlayer p, int a, int t) {
        switch (a) {
            case 1 -> {           // collapse ring: where you stood, the wavefunction collapses
                ring(l, attackAt, 3.0, 0.5F, 0.9F, 1F);
                if (t == 30) {
                    l.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), attackAt.x, attackAt.y + 1, attackAt.z, 1, 0, 0, 0, 0);
                    for (ServerPlayer q : near(l, attackAt, 3)) {
                        q.hurt(damageSources().indirectMagic(this, this), 10F);
                        q.addEffect(new MobEffectInstance(QHEffects.DECOHERENCE, 120, 1));
                    }
                    end(70);
                }
            }
            case 2 -> {           // tidal lance: dragged in along a geodesic
                if (t < 25) {
                    Vec3 pull = position().subtract(p.position()).normalize().scale(0.12);
                    p.push(pull.x, pull.y * 0.5, pull.z);
                    p.hurtMarked = true;
                    line(l, p.position().add(0, 1, 0), position().add(0, 2, 0));
                } else {
                    if (p.distanceToSqr(this) < 64) {
                        p.hurt(damageSources().indirectMagic(this, this), 8F);
                        p.addEffect(new MobEffectInstance(QHEffects.SPAGHETTIFICATION, 120, 1));
                    }
                    end(60);
                }
            }
            case 3 -> {           // regress barrage: three hits, each one deeper
                if (t % 12 == 0 && t <= 24 && hasLineOfSight(p)) {
                    line(l, position().add(0, 2, 0), p.position().add(0, 1, 0));
                    p.hurt(damageSources().indirectMagic(this, this), 4F);
                    MobEffectInstance cur = p.getEffect(QHEffects.INFINITE_REGRESS);
                    p.addEffect(new MobEffectInstance(QHEffects.INFINITE_REGRESS, 200, cur == null ? 0 : Math.min(5, cur.getAmplifier() + 1)));
                }
                if (t > 26) end(50);
            }
            case 4 -> {           // a bubble of true vacuum opens where you stood
                double r = Math.min(4.5, t * 0.12);
                ring(l, attackAt, r, 0.1F, 0.1F, 0.3F);
                if (t == 40) {
                    for (ServerPlayer q : near(l, attackAt, 4.5)) q.addEffect(new MobEffectInstance(QHEffects.VACUUM_DECAY, 160, 1));
                    end(60);
                }
            }
            default -> {          // gravity reversed - then restored, all at once
                if (t == 0)
                    for (ServerPlayer q : near(l, position(), 30)) q.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 3));
                if (t == 36) {
                    for (ServerPlayer q : near(l, position(), 30)) {
                        q.setDeltaMovement(q.getDeltaMovement().x, -2.0, q.getDeltaMovement().z);
                        q.hurtMarked = true;
                    }
                    end(70);
                }
            }
        }
    }

    private void end(int cd) {
        entityData.set(ATTACK, 0);
        cooldown = cd + random.nextInt(30) - (int) ((1 - getHealth() / getMaxHealth()) * 30);
    }

    private static List<ServerPlayer> near(ServerLevel l, Vec3 c, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : l.players()) if (!p.isCreative() && !p.isSpectator() && p.position().distanceToSqr(c) < r * r) out.add(p);
        return out;
    }

    private static void ring(ServerLevel l, Vec3 c, double r, float red, float g, float b) {
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12 + l.getGameTime() * 0.1;
            l.sendParticles(com.theoreticalsciences.registry.QHGlowOptions.of(red, g, b, 1.5F), c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static void line(ServerLevel l, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        int n = (int) (d.length() * 2);
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(d.scale(i / (double) Math.max(1, n)));
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private void summon(ServerLevel l) {
        l.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.EMERGENCE.get(), SoundSource.HOSTILE, 2F, 1.4F);
        String[] who = {"planck_mite", "planck_mite", "ordinal_hound", "ordinal_hound", "tidal_horror", "axiom_sentinel"};
        for (String id : who) {
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(com.theoreticalsciences.TheoreticalSciences.id(id));
            Entity m = type.create(l);
            if (m == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            m.moveTo(getX() + Math.cos(a) * 8, ProofArena.Y, getZ() + Math.sin(a) * 8, 0, 0);
            l.addFreshEntity(m);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        }
    }

    // ------------------------------------------------------------------ the two proofs
    private BlockPos arenaCenter() {
        ProofArena.Session s = session == null ? null : ProofArena.get(getServer()).of(session);
        return s != null ? s.arenaCenter() : BlockPos.containing(getX(), ProofArena.Y, getZ());
    }

    private void beginProof(ServerLevel l, ServerPlayer p) {
        proofsDone++;
        entityData.set(SHIELD, true);
        entityData.set(ATTACK, 0);
        List<ProofQuestions.Q> all = ProofQuestions.all();
        question = random.nextInt(all.size());
        correct = random.nextInt(4);
        ProofQuestions.Q q = all.get(question);
        BlockPos c = arenaCenter();
        pedestals.clear();
        List<String> wrong = new ArrayList<>(List.of(q.w1(), q.w2(), q.w3()));
        Collections.shuffle(wrong, new Random(random.nextLong()));
        int w = 0;
        String tag = ProofArena.TAG + "_boss";
        for (int i = 0; i < 4; i++) {
            BlockPos ped = c.offset(i == 0 ? 8 : i == 2 ? -8 : 0, 0, i == 1 ? 8 : i == 3 ? -8 : 0);
            l.setBlock(ped, ProofArena.b("answer_pedestal"), 3);
            pedestals.add(ped);
            String ans = i == correct ? q.right() : wrong.get(w++);
            ProofArena.text(l, Vec3.atCenterOf(ped).add(0, 1.4, 0), Component.literal((char) ('A' + i) + ". " + ans).withStyle(ChatFormatting.AQUA), 0.6F, 120, tag);
        }
        ProofArena.text(l, Vec3.atCenterOf(c).add(0, 5, 0), Component.literal(q.prompt()).withStyle(ChatFormatting.GOLD), 1.2F, 240, tag);
        l.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.HOSTILE, 3F, 0.5F);
        for (ServerPlayer pl : near(l, position(), 96))
            pl.sendSystemMessage(Component.translatable("proof.theoreticalsciences.voice", Component.translatable("boss.theoreticalsciences.alpha_omega.proof_" + proofsDone))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    public void answer(ServerPlayer p, BlockPos pos) {
        int idx = pedestals.indexOf(pos);
        if (idx < 0 || !shielded()) return;
        ServerLevel l = (ServerLevel) level();
        if (idx == correct) {
            entityData.set(SHIELD, false);
            stun = 80;
            super.hurt(damageSources().playerAttack(p), getMaxHealth() * 0.1F);
            l.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.HOSTILE, 4F, 0.5F);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), getX(), getY() + 2, getZ(), 3, 1, 1, 1, 0);
            clearProof(l);
        } else {
            heal(getMaxHealth() * 0.1F);
            p.hurt(damageSources().indirectMagic(this, this), 8F);
            p.addEffect(new MobEffectInstance(QHEffects.INFINITE_REGRESS, 200, 1));
            l.playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.EMERGENCE.get(), SoundSource.HOSTILE, 2F, 1.2F);
            p.displayClientMessage(Component.translatable("proof.theoreticalsciences.wrong").withStyle(ChatFormatting.RED), true);
            // a new question in the same places
            proofsDone--;
            clearProof(l);
            beginProof(l, p);
        }
    }

    private void clearProof(ServerLevel l) {
        for (BlockPos b : pedestals) l.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
        pedestals.clear();
        String tag = ProofArena.TAG + "_boss";
        for (Entity e : l.getEntities((Entity) null, getBoundingBox().inflate(48), e -> e.getTags().contains(tag))) e.discard();
    }

    /** The boss whose arena a pedestal stands in, if any. */
    public static AlphaOmegaEntity near(ServerLevel l, BlockPos pos) {
        List<AlphaOmegaEntity> list = l.getEntitiesOfClass(AlphaOmegaEntity.class, new AABB(pos).inflate(64));
        return list.isEmpty() ? null : list.get(0);
    }

    // ------------------------------------------------------------------ death: rays of light, a fracturing arena
    /**
     * Round 29: true when this Alpha Omega belongs to no running fight - outside the Living Proof, without a
     * challenger, or not the boss its challenger's session is waiting on.
     */
    private boolean orphan() {
        if (!(level() instanceof ServerLevel l) || l.dimension() != com.theoreticalsciences.world.QHDimensions.LIVING_PROOF || session == null) return true;
        ProofArena.Session s = ProofArena.get(l.getServer()).of(session);
        if (s == null || s.phase != ProofArena.Phase.BOSS) return true;
        return s.boss != null && !s.boss.equals(getUUID());
    }

    @Override
    protected void tickDeath() {
        if (deathTime == 0 && !level().isClientSide && orphan()) {      // no fracturing arena for a stray copy
            remove(RemovalReason.KILLED);
            return;
        }
        deathTime++;
        if (level() instanceof ServerLevel l) {
            if (deathTime == 1) {
                clearProof(l);
                l.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.EMERGENCE.get(), SoundSource.HOSTILE, 4F, 0.6F);
                for (Entity e : l.getEntities(this, getBoundingBox().inflate(64), e -> e instanceof Mob && !(e instanceof AlphaOmegaEntity))) e.discard();
            }
            setDeltaMovement(0, 0.02, 0);
            if (deathTime % 3 == 0) {
                // the arena fractures from the edge inward
                BlockPos c = arenaCenter();
                double r = ProofArena.ARENA_R * (1 - deathTime / (double) DEATH_TICKS) + 1;
                for (int i = 0; i < 40; i++) {
                    double a = random.nextDouble() * Math.PI * 2, rr = r + random.nextDouble() * 4;
                    BlockPos b = c.offset((int) (Math.cos(a) * rr), -1, (int) (Math.sin(a) * rr));
                    for (int y = -1; y < 8; y++) {
                        BlockPos q = new BlockPos(b.getX(), ProofArena.Y + y, b.getZ());
                        if (!l.getBlockState(q).isAir()) {
                            l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.1);
                        }
                    }
                }
                l.sendParticles(com.theoreticalsciences.registry.QHParticles.BURST.get(), getX() + (random.nextDouble() - 0.5) * 6, getY() + random.nextDouble() * 5, getZ() + (random.nextDouble() - 0.5) * 6, 1, 0, 0, 0, 0);
            }
            if (deathTime % 20 == 0) l.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.HOSTILE, 4F, 0.4F + deathTime / 400F);
            if (deathTime >= DEATH_TICKS) {
                if (session != null) ProofArena.conquer(l, session);
                remove(RemovalReason.KILLED);
            }
        }
    }

    // ------------------------------------------------------------------ save
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (session != null) t.putUUID("Session", session);
        t.putInt("Proofs", proofsDone);
        t.putInt("Summons", summonsDone);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("Session")) session = t.getUUID("Session");
        proofsDone = t.getInt("Proofs");
        summonsDone = t.getInt("Summons");
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 192 * 192;
    }
}
