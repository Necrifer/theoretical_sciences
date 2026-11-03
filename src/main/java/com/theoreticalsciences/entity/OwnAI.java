package com.theoreticalsciences.entity;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Round 37 (1.1): the mod's creatures think for themselves. Several were built on vanilla mobs (a zombie, a spider, a vex) and
 * inherited their minds with the body: chasing villagers and turning them into zombie villagers, hunting iron golems,
 * calling reinforcements, spawning as babies or jockeys. Each now gets its own goals from here, and vanilla's golems leave
 * them alone - they are not part of the Overworld's quarrels.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class OwnAI {
    /** A plain hunter: walks, fights players (and whoever hurts it), looks around. Nothing vanilla-specific. */
    public static void melee(PathfinderMob mob, double speed, boolean leaps) {
        mob.goalSelector.removeAllGoals(g -> true);
        mob.targetSelector.removeAllGoals(g -> true);
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        if (leaps) mob.goalSelector.addGoal(2, new LeapAtTargetGoal(mob, 0.4F));
        mob.goalSelector.addGoal(3, new MeleeAttackGoal(mob, speed, false));
        mob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(6, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(7, new RandomLookAroundGoal(mob));
        hunter(mob);
    }

    /** Keeps the body's own movement goals but replaces who it hunts: players, and whoever attacks it. */
    public static void hunter(PathfinderMob mob) {
        mob.targetSelector.removeAllGoals(g -> true);
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
    }

    static boolean ours(LivingEntity e) {
        return e != null && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(TheoreticalSciences.MODID);
    }

    /** Iron and snow golems target anything hostile; the mod's creatures are not theirs to fight. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getEntity() instanceof AbstractGolem && ours(e.getNewAboutToBeSetTarget())) e.setCanceled(true);
    }

    private OwnAI() {}
}
