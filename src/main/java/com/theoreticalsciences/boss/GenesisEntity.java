package com.theoreticalsciences.boss;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The summoning of Alpha Omega, played out over fifteen seconds where the Omnilex was thrown:
 * <ol>
 *   <li>0-3 s: the Omnilex shatters into every material it was made from (the client draws them flying apart);</li>
 *   <li>3-6 s: the pieces fall back together into a higher-dimensional shape;</li>
 *   <li>6-14 s: it questions its maker's understanding and purpose;</li>
 *   <li>15 s: it takes them - and anyone standing close - into the Living Proof.</li>
 * </ol>
 */
public class GenesisEntity extends Entity {
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(GenesisEntity.class, EntityDataSerializers.INT);
    public static final int SHATTER = 60, ASSEMBLE = 120, END = 300;
    public UUID owner;

    public GenesisEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public int age() {
        return entityData.get(AGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel l = (ServerLevel) level();
        // the one who threw it has logged out just as the genesis completes: it waits for them, humming
        if (age() >= END - 1 && owner != null && l.getServer().getPlayerList().getPlayer(owner) == null) {
            if (tickCount % 20 == 0) l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), getX(), getY(), getZ(), 6, 0.5, 0.5, 0.5, 0.01);
            return;
        }
        int age = age() + 1;
        entityData.set(AGE, age);
        BlockPos at = blockPosition();
        if (age == 1) {
            l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.OMNILEX_COLLAPSE.get(), SoundSource.HOSTILE, 5F, 1.4F);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), getX(), getY(), getZ(), 2, 0, 0, 0, 0);
        }
        if (age == SHATTER) l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.HOSTILE, 4F, 0.5F);
        if (age == ASSEMBLE) l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.EMERGENCE.get(), SoundSource.HOSTILE, 4F, 0.7F);
        if (age > ASSEMBLE && age < END && (age - ASSEMBLE) % 34 == 0) {
            int line = (age - ASSEMBLE) / 34;
            if (line >= 1 && line <= 5)
                for (ServerPlayer p : witnesses(l, 64))
                    p.sendSystemMessage(Component.translatable("proof.theoreticalsciences.voice", Component.translatable("boss.theoreticalsciences.alpha_omega.genesis_" + line))
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.PULSE.get(), SoundSource.HOSTILE, 3F, 0.5F);
        }
        if (age > ASSEMBLE) {
            // the pull begins: everything nearby leans toward it
            for (ServerPlayer p : witnesses(l, 24)) {
                var d = position().subtract(p.position()).normalize().scale(0.015 * (age - ASSEMBLE) / 60.0);
                p.push(d.x, 0, d.z);
                p.hurtMarked = true;
            }
        }
        if (age >= END) {
            // Round 29: only the one who threw the Omnilex can be pulled in as the challenger, and never over a proof
            // they are already in. (A logged-out owner is waited for - see the top of tick().)
            ServerPlayer o = owner == null ? null : l.getServer().getPlayerList().getPlayer(owner);
            if (o == null || ProofArena.get(l.getServer()).active(o.getUUID())) {
                // the genesis fails quietly and the Omnilex falls back out where it was - nobody else's progress is touched
                net.minecraft.world.entity.item.ItemEntity back = new net.minecraft.world.entity.item.ItemEntity(l, getX(), getY(), getZ(),
                        new net.minecraft.world.item.ItemStack(com.theoreticalsciences.registry.QHItems.get("omnilex")));
                back.setNoGravity(true);           // the Foam has no floor to land on: it hangs where the genesis was
                back.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                back.setPickUpDelay(40);
                back.setUnlimitedLifetime();
                if (o != null) back.setThrower(o);
                l.addFreshEntity(back);
                discard();
                return;
            }
            List<ServerPlayer> others = witnesses(l, 24);
            others.remove(o);
            ProofArena.begin(o, others);
            discard();
        }
    }

    private List<ServerPlayer> witnesses(ServerLevel l, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : l.players()) if (!p.isSpectator() && p.distanceToSqr(this) < r * r) out.add(p);
        return out;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 192 * 192;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(AGE, t.getInt("Age"));
        if (t.hasUUID("Owner")) owner = t.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Age", age());
        if (owner != null) t.putUUID("Owner", owner);
    }
}
