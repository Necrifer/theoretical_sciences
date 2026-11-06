package com.theoreticalsciences.titan;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.interior.InteriorGear;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.2.0: what Titan does to you, and nothing here changes or moves a block.
 * <ul>
 *   <li><b>Cold</b> - 94 K. You freeze as in powder snow, then take frost damage. Each piece of a Heated Pressure Suit
 *   keeps out a quarter of it; a full suit, all of it.</li>
 *   <li><b>Methane rain</b> - out under the sky in a storm, the rain soaks through the suit's aerogel and chills you as
 *   much again - unless the piece is treated with a Hydrophobic Coating (a quarter of the soaking per coated piece).</li>
 *   <li><b>No oxygen</b> - the air is nitrogen and methane: your breath runs out as if under water, unless you carry an
 *   Oxygen Rebreather with oxygen left (it spends a second of oxygen every second you are here).</li>
 *   <li><b>No fire</b> - nothing burns without oxygen: flint and steel and fire charges fail, and burning stops.</li>
 *   <li><b>Low gravity</b> - a seventh of Earth's: you fall slowly and land softly, and you jump higher. Liquid methane
 *   is too thin to swim in - you sink.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class TitanHazards {
    static final ResourceLocation GRAVITY = TheoreticalSciences.id("titan_gravity");
    /** Titan's gravity against Earth's is 0.14, but a suited explorer's legs were made for Earth: jumps are capped. */
    static final double GRAVITY_FACTOR = 0.30, JUMP_FACTOR = 0.75, FALL_FACTOR = 0.30;
    /** Frost added a tick by the open air, on top of what vanilla thaws (2 a tick); powder snow adds 1. */
    static final double COLD = 0.5;

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean ocean = p.level().dimension() == QHDimensions.TITAN_OCEAN && !p.isSpectator();
        boolean here = p.level().dimension() == QHDimensions.TITAN && !p.isSpectator();
        gravity(p, here || ocean);
        if (ocean && !p.isCreative()) {
            // 1.2.0: Titan's hidden ocean: still cold, and water to drown in, unless a rebreather keeps you breathing
            cold(p, p.serverLevel());
            ItemStack rb = TitanGear.rebreather(p);
            if (!rb.isEmpty() && p.isUnderWater()) {
                p.setAirSupply(p.getMaxAirSupply());
                if ((p.level().getGameTime() + p.getId()) % 20 == 0) rb.setDamageValue(Math.min(rb.getMaxDamage(), rb.getDamageValue() + 1));
            }
            return;
        }
        if (!here || p.isCreative()) return;
        ServerLevel level = p.serverLevel();
        if (p.isOnFire()) p.clearFire();                       // nothing burns without oxygen
        cold(p, level);
        breath(p, level);
        if ((p.tickCount + p.getId()) % 200 == 0) populate(p, level);
        // too thin to swim in: you sink
        var methane = com.theoreticalsciences.registry.QHFluids.ALL.get("liquid_methane");
        if (methane != null && p.isInFluidType(methane.type.value())) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, Math.min(v.y, v.y - 0.012), v.z);
            p.hurtMarked = true;
        }
    }

    static void gravity(ServerPlayer p, boolean on) {
        mod(p.getAttribute(Attributes.GRAVITY), on, GRAVITY_FACTOR - 1);
        mod(p.getAttribute(Attributes.JUMP_STRENGTH), on, JUMP_FACTOR - 1);
        mod(p.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), on, FALL_FACTOR - 1);
    }

    static void mod(AttributeInstance a, boolean on, double amount) {
        if (a == null) return;
        boolean has = a.hasModifier(GRAVITY);
        if (on && !has) a.addTransientModifier(new AttributeModifier(GRAVITY, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!on && has) a.removeModifier(GRAVITY);
    }

    /** How fast the cold gets in, in frost a tick (0 = not at all). */
    public static double coldRate(Player p, ServerLevel level) {
        int suit = InteriorGear.pieces(p, InteriorGear.Suit.HEATED);
        double rate = COLD * (1 - suit / 4.0);
        if (level.isRainingAt(p.blockPosition().above())) rate += COLD * (1 - TitanGear.coated(p) / 4.0);
        return rate;
    }

    static void cold(ServerPlayer p, ServerLevel level) {
        double rate = coldRate(p, level);
        if (rate <= 0) return;
        int add = 2 + (int) rate + (level.random.nextDouble() < rate - Math.floor(rate) ? 1 : 0);
        p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 20, p.getTicksFrozen() + add));
        if (p.isFullyFrozen() && (level.getGameTime() + p.getId()) % 20 == 0) {
            p.hurt(level.damageSources().freeze(), 2F);
            if ((level.getGameTime() + p.getId()) % 100 == 0)
                p.displayClientMessage(Component.translatable(InteriorGear.pieces(p, InteriorGear.Suit.HEATED) >= 4
                        ? "message.theoreticalsciences.titan_soaked" : "message.theoreticalsciences.titan_cold").withStyle(ChatFormatting.AQUA), true);
        }
    }

    static void breath(ServerPlayer p, ServerLevel level) {
        ItemStack rb = TitanGear.rebreather(p);
        if (!rb.isEmpty()) {
            p.setAirSupply(p.getMaxAirSupply());
            if ((level.getGameTime() + p.getId()) % 20 == 0) {
                rb.setDamageValue(Math.min(rb.getMaxDamage(), rb.getDamageValue() + 1));
                if (rb.getDamageValue() == rb.getMaxDamage() - 60)
                    p.displayClientMessage(Component.translatable("message.theoreticalsciences.rebreather_low").withStyle(ChatFormatting.YELLOW), true);
            }
            return;
        }
        int air = p.getAirSupply() - 5;                        // vanilla gives back 4 a tick out of water: a net loss of 1
        if (air <= -20) {
            air = 0;
            p.hurt(level.damageSources().drown(), 2F);
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.titan_anoxic").withStyle(ChatFormatting.RED), true);
        }
        p.setAirSupply(air);
    }

    /** Each region's own creatures (as its biome lists them), for the top-up below. */
    static final java.util.Map<String, String[]> LIFE = java.util.Map.of(
            "kraken_mare", new String[]{"methane_skater", "methane_skater", "tholin_drifter"},
            "shangri_la_dunes", new String[]{"tholin_drifter"},
            "xanadu_uplands", new String[]{"tholin_drifter", "tholin_drifter", "cryolava_slug"},
            "ontario_lacus_basin", new String[]{"catalyst_sprite", "catalyst_sprite", "methane_skater"},
            "sotra_cryovolcanic_field", new String[]{"cryolava_slug", "cryolava_slug", "catalyst_sprite"},
            "selk_crater", new String[]{"tholin_drifter", "catalyst_sprite", "cryolava_slug"});

    /**
     * Titan is mostly sea and dim haze, where vanilla's spawning finds little room: every ten seconds, if fewer than eight
     * of Titan's creatures are within 48 blocks of a player, one of the region's own comes in, 20 to 40 blocks away.
     */
    static void populate(ServerPlayer p, ServerLevel level) {
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        if (level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(48), TitanRegistry::isTitanCreature).size() >= 8) return;
        var r = level.random;
        double a = r.nextDouble() * Math.PI * 2, d = 20 + r.nextDouble() * 20;
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new net.minecraft.core.BlockPos(x, 0, z))) return;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        var key = level.getBiome(pos).unwrapKey();
        if (key.isEmpty()) return;
        String[] kinds = LIFE.get(key.get().location().getPath());
        if (kinds == null) return;
        String kind = kinds[r.nextInt(kinds.length)];
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(TheoreticalSciences.id(kind));
        boolean wet = !level.getFluidState(pos.below()).isEmpty();
        if (wet && !kind.equals("methane_skater") && !kind.equals("tholin_drifter") && !kind.equals("catalyst_sprite")) return;
        if (!wet && kind.equals("methane_skater") && r.nextInt(3) != 0) return;
        var mob = type.create(level);
        if (!(mob instanceof net.minecraft.world.entity.Mob m)) return;
        double lift = kind.equals("tholin_drifter") || kind.equals("catalyst_sprite") ? 2 + r.nextInt(4) : 0;
        m.moveTo(x + 0.5, y + lift, z + 0.5, r.nextFloat() * 360, 0);
        if (!level.noCollision(m)) return;
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        level.addFreshEntity(m);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().dimension() != QHDimensions.TITAN) return;
        ItemStack st = e.getItemStack();
        if (st.is(Items.FLINT_AND_STEEL) || st.is(Items.FIRE_CHARGE)) {
            e.setCanceled(true);
            if (e.getEntity() instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.titan_no_fire").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private TitanHazards() {}
}
