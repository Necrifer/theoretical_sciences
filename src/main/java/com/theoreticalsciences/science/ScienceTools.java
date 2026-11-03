package com.theoreticalsciences.science;

import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Round 37 (1.1): the first instruments of the new sciences. All read the world; none changes it. */
public final class ScienceTools {
    private ScienceTools() {}

    static String compass(int dx, int dz) {
        if (dx == 0 && dz == 0) return "here";
        double a = Math.toDegrees(Math.atan2(dx, -dz));                 // 0 = north, clockwise
        String[] n = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return n[(int) Math.round(((a % 360) + 360) % 360 / 45.0) % 8];
    }

    // ------------------------------------------------------------------ Geodetic Surveyor
    /** Triangulates the ground around you: which ores lie within 24 blocks, how many, and where the nearest of each is. */
    public static class Surveyor extends LoreItem {
        static final int R = 24, H = 16;

        public Surveyor(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) survey(sl, sp);
            player.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }

        static void survey(ServerLevel level, ServerPlayer p) {
            BlockPos c = p.blockPosition();
            Map<Block, int[]> found = new LinkedHashMap<>();         // count, best distance², best dx, dy, dz
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int dx = -R; dx <= R; dx++)
                for (int dz = -R; dz <= R; dz++) {
                    if (!level.isLoaded(m.set(c.getX() + dx, c.getY(), c.getZ() + dz))) continue;
                    for (int dy = -H; dy <= H; dy++) {
                        m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                        BlockState s = level.getBlockState(m);
                        if (!s.is(Tags.Blocks.ORES)) continue;
                        int d2 = dx * dx + dy * dy + dz * dz;
                        int[] f = found.computeIfAbsent(s.getBlock(), k -> new int[]{0, Integer.MAX_VALUE, 0, 0, 0});
                        f[0]++;
                        if (d2 < f[1]) {
                            f[1] = d2;
                            f[2] = dx;
                            f[3] = dy;
                            f[4] = dz;
                        }
                    }
                }
            p.sendSystemMessage(Component.translatable("message.theoreticalsciences.survey_head", c.getY(),
                    level.getBiome(c).unwrapKey().map(k -> k.location().getPath().replace('_', ' ')).orElse("?")).withStyle(ChatFormatting.AQUA));
            if (found.isEmpty()) {
                p.sendSystemMessage(Component.translatable("message.theoreticalsciences.survey_none").withStyle(ChatFormatting.GRAY));
                return;
            }
            List<Map.Entry<Block, int[]>> list = new ArrayList<>(found.entrySet());
            list.sort((a, b) -> Integer.compare(a.getValue()[1], b.getValue()[1]));
            for (Map.Entry<Block, int[]> e : list.subList(0, Math.min(6, list.size()))) {
                int[] f = e.getValue();
                p.sendSystemMessage(Component.translatable("message.theoreticalsciences.survey_line", f[0], e.getKey().getName(),
                        String.format(Locale.ROOT, "%.0f", Math.sqrt(f[1])), compass(f[2], f[4]), f[3] >= 0 ? "+" + f[3] : String.valueOf(f[3]))
                        .withStyle(ChatFormatting.GRAY));
                level.sendParticles(p, com.theoreticalsciences.registry.QHParticles.PHOTON.get(), true,
                        c.getX() + f[2] + 0.5, c.getY() + f[3] + 0.5, c.getZ() + f[4] + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            }
            level.playSound(null, p.blockPosition(), QHSounds.UI_CLICK.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    // ------------------------------------------------------------------ Gauss Meter
    /** Reads the magnetic field where you stand: the planet's own, plus magnets (block tag theoreticalsciences:magnetic) nearby. */
    public static class GaussMeter extends LoreItem {
        public GaussMeter(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            if (!level.isClientSide) {
                double t = field(level, player);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.gauss", tesla(t)).withStyle(ChatFormatting.AQUA), true);
                level.playSound(null, player.blockPosition(), QHSounds.UI_CLICK.get(), SoundSource.PLAYERS, 0.4F, 1.8F);
            }
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }

        /** The field at the player, in tesla. */
        public static double field(Level level, Player p) {
            BlockPos c = p.blockPosition();
            double b;
            if (level.dimension() == Level.OVERWORLD) b = (25 + 40 * Math.abs(Math.sin(c.getZ() / 4000.0))) * 1e-6;  // a geodynamo: 25-65 µT
            else if (level.dimension() == Level.NETHER || level.dimension() == Level.END) b = 0;
            else b = 5e-6;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int dx = -6; dx <= 6; dx++)
                for (int dy = -6; dy <= 6; dy++)
                    for (int dz = -6; dz <= 6; dz++) {
                        if (!level.getBlockState(m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz)).is(Sciences.MAGNETIC_BLOCKS)) continue;
                        double r = Math.max(1.0, Math.sqrt(dx * dx + dy * dy + dz * dz));
                        b += 5e-3 / (r * r * r);                       // a dipole: 5 mT a block away, falling with r³
                    }
            var mag = p.getEffect(QHEffects.MAGNETIZED);
            if (mag != null) b += 1e-3 * (mag.getAmplifier() + 1);
            return b;
        }

        static String tesla(double t) {
            if (t >= 1) return String.format(Locale.ROOT, "%.2f T", t);
            if (t >= 1e-3) return String.format(Locale.ROOT, "%.2f mT", t * 1e3);
            return String.format(Locale.ROOT, "%.1f µT", t * 1e6);
        }
    }

    // ------------------------------------------------------------------ Geiger Counter
    /** Held in either hand, clicks for every decay it hears: radioactive items you carry, radioactive blocks within 4. */
    public static class GeigerCounter extends LoreItem {
        public GeigerCounter(Properties p) {
            super(p);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity e, int slot, boolean selected) {
            if (level.isClientSide || !(e instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 1) return;
            if (!selected && p.getOffhandItem() != stack) return;
            // round 38 (1.2): the reading the radiation service worked out this second - carried, around and background
            RadiationService.Reading r = RadiationService.reading(p);
            double rate = r.total(), cps = RadiationService.countsPerSecond(rate);
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.geiger2",
                    cps >= 1000 ? String.format(Locale.ROOT, "%.1fk", cps / 1000) : String.format(Locale.ROOT, "%.1f", cps),
                    RadiationService.rateText(rate), String.format(Locale.ROOT, "%.0f", RadiationService.dose(p)))
                    .withStyle(cps > 2000 ? ChatFormatting.RED : cps > 50 ? ChatFormatting.GOLD : ChatFormatting.GREEN), true);
            int clicks = (int) Math.min(8, Math.round(cps + p.getRandom().nextDouble() - 0.5));
            for (int i = 0; i < clicks; i++)
                level.playSound(null, p.blockPosition(), QHSounds.UI_CLICK.get(), SoundSource.PLAYERS, 0.25F, 1.9F + p.getRandom().nextFloat() * 0.2F);
        }
    }

    // ------------------------------------------------------------------ Dice Set
    /** Two fair dice. Roll them; probability starts here. */
    public static class Dice extends LoreItem {
        public Dice(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            if (!level.isClientSide) {
                int a = 1 + level.random.nextInt(6), b = 1 + level.random.nextInt(6);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.dice", a, b, a + b).withStyle(ChatFormatting.YELLOW), true);
                level.playSound(null, player.blockPosition(), QHSounds.UI_CLICK.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
            }
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }
    }

    // ------------------------------------------------------------------ Prussian Blue Tablet
    /** Binds radioactive caesium and thallium in the gut so they leave the body: most of the dose, and the sickness, go. */
    public static class PrussianBlue extends LoreItem {
        public PrussianBlue(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            if (RadiationService.dose(player) <= 0 && !player.hasEffect(QHEffects.RADIATION_SICKNESS)) return InteractionResultHolder.pass(held);
            if (!level.isClientSide) {
                RadiationService.setDose(player, RadiationService.dose(player) * 0.4);
                player.removeEffect(QHEffects.RADIATION_SICKNESS);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.prussian_blue").withStyle(ChatFormatting.BLUE), true);
                held.consume(1, player);
            }
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }
    }
}
