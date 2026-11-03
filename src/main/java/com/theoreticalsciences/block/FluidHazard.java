package com.theoreticalsciences.block;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.item.ContainmentCells;
import com.theoreticalsciences.registry.QHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.List;
import java.util.Set;

/**
 * Round 37: what the mod's liquids do to whatever is in them - the same reasons a bucket refuses them.
 * <ul>
 *   <li><b>Hot</b> (500 K and up) burns like lava, harder the hotter it is, and destroys dropped items. Up to 2,000 K it
 *   is ordinary fire (fire resistance and fire-resistant items are safe); above that, stellar heat ignores both.</li>
 *   <li><b>Cold</b> (200 K and below) freezes like powder snow, faster and harder the colder it is. Leather armour keeps
 *   it out down to about 2 K; below that nothing does.</li>
 *   <li><b>Exotic</b> (everything else - false vacuum, liquid spacetime, the broths and reagents) twists whoever is in
 *   it with harmful effects, like the Ocean of Primordial Law but without its gifts. The truly strange ones hit harder.</li>
 * </ul>
 */
public final class FluidHazard {
    public enum Kind { HOT, COLD, EXOTIC }

    /** Stored at 0 K only because no temperature applies: they are strange, not cold. */
    static final Set<String> STRANGE = Set.of("false_vacuum", "pleroma", "liquid_spacetime", "exotic_matter", "primordial_law",
            "schrodinger_limit", "relativity_breakdown", "mod_essence");
    /** Exotic, and harder on whoever is in it. */
    static final Set<String> STRONG = Set.of("false_vacuum", "pleroma", "liquid_spacetime", "exotic_matter", "antiwater",
            "carborane_superacid", "xenoplasm", "dimensional_residue");
    /** Their blocks do their own thing (the Ocean of Primordial Law, the Quantum Foam's floor and ceiling). */
    static final Set<String> OWN = Set.of("primordial_law", "schrodinger_limit", "relativity_breakdown");

    public static final ResourceKey<DamageType> HEAT = ResourceKey.create(Registries.DAMAGE_TYPE, TheoreticalSciences.id("exotic_heat"));
    public static final ResourceKey<DamageType> STELLAR_HEAT = ResourceKey.create(Registries.DAMAGE_TYPE, TheoreticalSciences.id("stellar_heat"));

    private static final List<Holder<MobEffect>> DEBUFFS = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS,
            MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.HUNGER, MobEffects.DARKNESS, MobEffects.UNLUCK);
    private static final List<Holder<MobEffect>> STRONG_DEBUFFS = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS,
            MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.HUNGER, MobEffects.DARKNESS, MobEffects.UNLUCK, MobEffects.LEVITATION);

    public static Kind kind(QHFluids.Entry e) {
        int t = e.def.temperature();
        if (STRANGE.contains(e.def.id())) return Kind.EXOTIC;
        if (t >= 500) return Kind.HOT;
        if (t <= 200) return Kind.COLD;
        return Kind.EXOTIC;
    }

    /** 0 at 500 K, about 6.6 for a quark-gluon plasma. */
    static double heat(int kelvin) {
        return Math.log10(Math.max(kelvin, 500) / 500.0);
    }

    /** 0 at 200 K, 1 at 20 K, 2 at 2 K, about 2.6 at absolute zero. */
    static double chill(int kelvin) {
        return Math.log10(200.0 / Math.max(kelvin, 0.5));
    }

    /** Called by the liquid's block for every entity touching it. */
    static void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity.isSpectator()) return;
        FluidState fs = state.getFluidState();
        if (fs.isEmpty()) return;
        QHFluids.Entry e = ContainmentCells.entryFor(fs.getType());
        if (e == null || OWN.contains(e.def.id())) return;
        // once per tick (an entity can touch several fluid blocks), and only when really in it
        if (!pos.equals(entity.blockPosition()) || entity.getY() > pos.getY() + fs.getHeight(level, pos)) return;
        int t = e.def.temperature();
        // 1.3.3: the Planetary Interior's two liquids
        if (e.def.id().equals("basaltic_magma")) {
            magma(level, pos, entity);
            return;
        }
        if (e.def.id().equals("deep_brine")) {
            entity.igniteForSeconds(15);                                  // scalding, as lava is
            entity.hurt(level.damageSources().lava(), 4);
            return;
        }
        switch (kind(e)) {
            case HOT -> hot(level, pos, entity, t);
            case COLD -> cold(level, entity, t);
            case EXOTIC -> exotic(level, entity, STRONG.contains(e.def.id()));
        }
    }

    private static void hot(Level level, BlockPos pos, Entity entity, int kelvin) {
        double s = heat(kelvin);
        boolean stellar = kelvin > 2000;
        float dmg = (float) Math.min(20.0, 3.0 + 2.0 * s);
        DamageSource src = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(stellar ? STELLAR_HEAT : HEAT));
        if (entity instanceof ItemEntity item) {
            if (fireproof(item) && !stellar) return;      // fire resistance holds below 2,000 K
            if (item.hurt(src, dmg) && item.isRemoved())
                level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.VENT.get(), SoundSource.BLOCKS, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
            return;
        }
        entity.igniteForSeconds((float) Math.min(30.0, 8.0 + 3.0 * s));
        entity.hurt(src, dmg);
    }

    /** Fire-resistant items (netherite, the Interior's rocks and ores) survive every hot liquid, not only lava. */
    static boolean fireproof(ItemEntity ie) {
        return ie.getItem().has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT);
    }

    public static final ResourceKey<DamageType> MAGMA = ResourceKey.create(Registries.DAMAGE_TYPE, TheoreticalSciences.id("basaltic_magma"));
    /** A full set of armour in this tag keeps Basaltic Magma off (the armour itself comes in a later release). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> MAGMA_IMMUNE =
            net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE, TheoreticalSciences.id("magma_immune"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> MAGMA_PROOF =
            net.minecraft.tags.TagKey.create(Registries.ITEM, TheoreticalSciences.id("magma_proof"));

    /**
     * 1.3.3: Basaltic Magma melts what it touches - 10 a hit, two and a half times lava's - with a damage type that is not
     * fire, so Fire Resistance does nothing. Only a full set of magma-proof armour keeps it off.
     */
    private static void magma(Level level, BlockPos pos, Entity entity) {
        DamageSource src = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MAGMA));
        if (entity.getType().is(MAGMA_IMMUNE)) return;                   // 1.3.7: what lives in it (the Magma Crawler)
        if (entity instanceof ItemEntity ie && fireproof(ie)) return;     // 1.3.8: the Interior's own rock and ore come out of it whole
        if (entity instanceof LivingEntity le) {
            boolean proof = true;
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET})
                if (!le.getItemBySlot(slot).is(MAGMA_PROOF)) proof = false;
            if (proof) return;
        }
        entity.igniteForSeconds(20);
        entity.hurt(src, 10);
    }

    private static void cold(Level level, Entity entity, int kelvin) {
        double s = chill(kelvin);
        boolean deep = s >= 2.0;                       // ~2 K and colder: leather is no help
        if (!(entity.canFreeze() || deep && !entity.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES))) return;
        entity.clearFire();
        int gain = 3 + (int) Math.round(2.0 * s);      // vanilla thaws 2 a tick outside powder snow
        entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze() + 20, entity.getTicksFrozen() + gain));
        // on top of the frost vanilla already deals when fully frozen
        if (entity instanceof LivingEntity && entity.isFullyFrozen() && s > 0.5 && (level.getGameTime() + entity.getId()) % 20 == 0)
            entity.hurt(level.damageSources().freeze(), (float) (s * 1.5));
    }

    private static void exotic(Level level, Entity entity, boolean strong) {
        if (!(entity instanceof LivingEntity le)) return;
        if ((level.getGameTime() + entity.getId()) % (strong ? 40 : 100) != 0) return;
        List<Holder<MobEffect>> pool = strong ? STRONG_DEBUFFS : DEBUFFS;
        Holder<MobEffect> eff = pool.get(level.random.nextInt(pool.size()));
        int duration = eff == MobEffects.LEVITATION ? 40 : strong ? 160 : 100;
        le.addEffect(new MobEffectInstance(eff, duration, strong ? level.random.nextInt(2) : 0, false, true));
    }

    private FluidHazard() {}
}
