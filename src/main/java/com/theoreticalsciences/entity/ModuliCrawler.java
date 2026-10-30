package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A scuttling knot of folded geometry from the Moduli Wastes - a little piece of hidden dimension that keeps
 * reshaping itself. They hunt in packs and climb anything.
 */
public class ModuliCrawler extends Spider {
    public ModuliCrawler(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.34);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.CRAWLER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.CRAWLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.CRAWLER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(QHSounds.CRAWLER_STEP.get(), 0.3F, 1.2F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.1F;
    }

    /** A spider's 1-in-100 skeleton rider has no business in the Bulk: whatever vanilla adds as a passenger is removed. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance diff,
                                                                     net.minecraft.world.entity.MobSpawnType type, @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, diff, type, data);
        for (net.minecraft.world.entity.Entity p : java.util.List.copyOf(getPassengers())) {
            p.stopRiding();
            p.discard();
        }
        return out;
    }
}
