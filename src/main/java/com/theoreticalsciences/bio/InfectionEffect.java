package com.theoreticalsciences.bio;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/** 1.2.0: an infection. Milk does not wash it out; it runs its course, and an Anomaly Ward keeps the next one away. */
public class InfectionEffect extends MobEffect {
    public InfectionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
        cures.clear();
    }
}
