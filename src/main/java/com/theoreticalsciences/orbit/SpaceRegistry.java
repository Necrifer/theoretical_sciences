package com.theoreticalsciences.orbit;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/** 1.12.0, Patch C1 (getting there): the Transfer Stages, the Return Beacon and the Mission Bay's block entity. */
public final class SpaceRegistry {
    private SpaceRegistry() {}

    static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MissionBay.BayEntity>> BAY = BES.register("mission_bay",
            () -> BlockEntityType.Builder.of(MissionBay.BayEntity::new, QHBlocks.SIMPLE.get("mission_bay").get()).build(null));

    public static void register(IEventBus bus) {
        BES.register(bus);
        bus.addListener((RegisterCapabilitiesEvent e) ->
                e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BAY.get(), (be, side) -> be.automation));
    }

    // ------------------------------------------------------------------ the stages
    /** Δv (m/s) and specific impulse (s) of each stage: Δv = Isp·g₀·ln(m₀/m_f), with the mass ratios in the lore. */
    static final Map<String, double[]> STAGES = Map.of(
            "hydrolox_transfer_stage", new double[]{6100, 450},
            "methalox_transfer_stage", new double[]{4900, 360},
            "nuclear_transfer_stage", new double[]{9700, 900},
            "ion_transfer_stage", new double[]{20400, 3000},
            "antimatter_transfer_stage", new double[]{79600, 20000});

    public static class TransferStageItem extends com.theoreticalsciences.item.LoreItem {
        public double dv, isp;

        public TransferStageItem(Properties p) {
            super(p);
        }

        void resolve() {
            if (dv > 0) return;
            double[] v = STAGES.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath());
            if (v != null) {
                dv = v[0];
                isp = v[1];
            }
        }

        public double dv() {
            resolve();
            return dv;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            resolve();
            tip.add(Component.translatable("tooltip.theoreticalsciences.stage_dv", String.format(java.util.Locale.ROOT, "%.1f", dv / 1000),
                    String.format(java.util.Locale.ROOT, "%,.0f", isp)).withStyle(ChatFormatting.AQUA));
            super.appendHoverText(stack, ctx, tip, flag);
        }
    }

    // ------------------------------------------------------------------ the Return Beacon
    public static class ReturnBeaconBlock extends Block {
        public ReturnBeaconBlock(Properties p) {
            super(p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            if (level.isClientSide || !(by instanceof ServerPlayer sp)) return;
            Destinations.Destination d = Destinations.INSTANCE.forDimension(level.dimension());
            if (d == null || d.home()) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.beacon_nowhere").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            OrbitData.get(sp.server).chart(d.id());
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.beacon_charted").withStyle(ChatFormatting.AQUA), false);
        }
    }
}
