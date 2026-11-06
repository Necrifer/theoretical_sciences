package com.theoreticalsciences.inner;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.bio.Immunity;
import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1.2.0: what Inner Space does to you. Effects only: nothing here changes or moves a block.
 * <ul>
 *   <li><b>The Bloodstream</b>: in the plasma, the current carries you downstream (Inner.current).</li>
 *   <li><b>Tissue</b>: inflammation. Where a fight breaks out (you were hurt, or a creature near you was), the tissue
 *   swells: you slow, and the heat hurts.</li>
 *   <li><b>The Lymph Node</b>: tagged. A player carrying an infection glows here, and the T cells hunt them.</li>
 *   <li><b>The Nucleus</b>: mutation. Now and then one of your effects is swapped for its opposite.</li>
 *   <li><b>The Tumour</b>: the oncogenic field. Weakness that grows the longer you stay, and fades once you leave.</li>
 * </ul>
 * Creatures are kept few (at most ten near a player), as the Tessellate taught.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class InnerHazards {
    static final String ONCO = "ts_oncogenic";
    /** Each effect, and its opposite (the Nucleus swaps one for the other). */
    static final List<Holder<MobEffect>[]> PAIRS = new ArrayList<>();

    @SuppressWarnings("unchecked")
    static void pair(Holder<MobEffect> a, Holder<MobEffect> b) {
        PAIRS.add(new Holder[]{a, b});
    }

    static {
        pair(MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SLOWDOWN);
        pair(MobEffects.DIG_SPEED, MobEffects.DIG_SLOWDOWN);
        pair(MobEffects.DAMAGE_BOOST, MobEffects.WEAKNESS);
        pair(MobEffects.REGENERATION, MobEffects.POISON);
        pair(MobEffects.NIGHT_VISION, MobEffects.BLINDNESS);
        pair(MobEffects.LUCK, MobEffects.UNLUCK);
        pair(MobEffects.SATURATION, MobEffects.HUNGER);
        pair(MobEffects.DAMAGE_RESISTANCE, MobEffects.GLOWING);
    }

    /** What lives in each biome (Inner Space's creatures are spawned here, a few at a time, rather than by the world). */
    static final Map<String, String[]> LIFE = Map.of(
            "bloodstream", new String[]{"red_cell", "red_cell", "red_cell", "macrophage"},
            "tissue", new String[]{"macrophage", "macrophage", "red_cell"},
            "lymph_node", new String[]{"t_cell", "t_cell", "macrophage"},
            "nucleus", new String[]{"polymerase", "polymerase", "t_cell"},
            "tumour", new String[]{"macrophage", "t_cell"});

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean here = Inner.in(p.level()) && !p.isSpectator();
        if (!here) {
            if (p.tickCount % 20 == 0) fadeOncogenic(p, 3);
            return;
        }
        ServerLevel level = p.serverLevel();
        // the current: in the plasma, carried downstream
        if (Inner.inPlasma(p) && !p.isCreative() && p.getVehicle() == null) {
            Vec3 c = Inner.current(level, p.getX(), p.getZ());
            p.setDeltaMovement(p.getDeltaMovement().add(c.scale(0.25)));
            p.hurtMarked = true;
        }
        // life comes in round every player, creative or not, as the world's own spawning does
        if ((p.tickCount + p.getId()) % 160 == 0) populate(p, level);
        if (p.isCreative()) return;
        if (p.tickCount % 20 != 0) return;
        String b = Inner.biome(level, p.blockPosition());
        if (b.equals("tumour") && !p.getPersistentData().getBoolean("ts_tumour_told")) {
            p.getPersistentData().putBoolean("ts_tumour_told", true);
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.tumour_entered").withStyle(ChatFormatting.DARK_RED), false);
        }
        if (!b.equals("tumour")) fadeOncogenic(p, 2);
        switch (b) {
            case "tissue" -> inflammation(p, level);
            case "lymph_node" -> {
                if (Immunity.infected(p)) p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
            case "nucleus" -> {
                if (p.getRandom().nextInt(20) == 0) mutate(p);
            }
            case "tumour" -> oncogenic(p);
            default -> {}
        }
    }

    static void inflammation(ServerPlayer p, ServerLevel level) {
        boolean fight = p.hurtTime > 0 || p.tickCount - p.getLastHurtByMobTimestamp() < 60;
        if (!fight)
            for (LivingEntity m : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12)))
                if (m != p && m.hurtTime > 0) {
                    fight = true;
                    break;
                }
        if (!fight) return;
        if (!p.hasEffect(QHEffects.INFLAMMATION))
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.inflammation").withStyle(ChatFormatting.RED), true);
        p.addEffect(new MobEffectInstance(QHEffects.INFLAMMATION, 160, 0));
    }

    /** One of your effects becomes its opposite, with the same strength and time left. */
    static void mutate(ServerPlayer p) {
        List<MobEffectInstance> have = new ArrayList<>();
        for (MobEffectInstance m : p.getActiveEffects()) if (opposite(m.getEffect()) != null) have.add(m);
        if (have.isEmpty()) return;
        MobEffectInstance m = have.get(p.getRandom().nextInt(have.size()));
        Holder<MobEffect> to = opposite(m.getEffect());
        p.removeEffect(m.getEffect());
        p.addEffect(new MobEffectInstance(to, m.getDuration(), m.getAmplifier(), m.isAmbient(), m.isVisible()));
        p.displayClientMessage(Component.translatable("message.theoreticalsciences.mutation", m.getEffect().value().getDisplayName(),
                to.value().getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    static Holder<MobEffect> opposite(Holder<MobEffect> e) {
        for (Holder<MobEffect>[] pr : PAIRS) {
            if (pr[0].equals(e)) return pr[1];
            if (pr[1].equals(e)) return pr[0];
        }
        return null;
    }

    /** The oncogenic field: a second of exposure more; Weakness one level stronger for each minute of it (to V). */
    static void oncogenic(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        int s = d.getInt(ONCO) + 1;
        d.putInt(ONCO, s);
        if (s == 1) p.displayClientMessage(Component.translatable("message.theoreticalsciences.oncogenic").withStyle(ChatFormatting.DARK_RED), true);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, Math.min(4, s / 60), false, true));
    }

    static void fadeOncogenic(ServerPlayer p, int by) {
        CompoundTag d = p.getPersistentData();
        if (d.contains(ONCO)) {
            int s = d.getInt(ONCO) - by;
            if (s <= 0) d.remove(ONCO);
            else d.putInt(ONCO, s);
        }
    }

    static void populate(ServerPlayer p, ServerLevel level) {
        if (level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), InnerRegistry::isInnerCreature).size() >= 12) return;
        var r = level.random;
        double a = r.nextDouble() * Math.PI * 2, dist = 12 + r.nextDouble() * 16;
        int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        String[] kinds = LIFE.get(Inner.biome(level, pos));
        if (kinds == null) return;
        String kind = kinds[r.nextInt(kinds.length)];
        boolean wet = !level.getFluidState(pos.below()).isEmpty();
        if (kind.equals("red_cell") != wet && !kind.equals("macrophage")) return;     // red cells live in the plasma, the others on the ground
        boolean hostile = !kind.equals("red_cell") && !kind.equals("polymerase");
        if (hostile && level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(TheoreticalSciences.id(kind));
        if (!(type.create(level) instanceof Mob m)) return;
        m.moveTo(x + 0.5, wet ? y - 0.5 : y, z + 0.5, r.nextFloat() * 360, 0);
        if (!level.noCollision(m)) return;
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(m);
    }

    private InnerHazards() {}
}
